"""Admin: User management — CRUD, detail, project management."""
import logging
from datetime import datetime
from app.utils.time import utcnow
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from sqlalchemy import func, desc

from app.database import get_db
from app.dependencies import get_current_admin
from app.models.user import User, UserDevice, LoginLog
from app.models.activation_code import ProTransaction
from app.models.project import Project, ProjectActivationCode, ProjectVersion
from app.schemas.common import ApiResponse, PaginatedResponse
from app.schemas.admin import AdminUpdateUserRequest
from app.routers.admin.helpers import audit

logger = logging.getLogger(__name__)
router = APIRouter()


@router.get("/users", response_model=PaginatedResponse)
def list_users(
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    search: str = Query(None),
    plan_filter: str = Query(None),
    status_filter: str = Query(None),
    sort_by: str = Query("created_at"),
    sort_order: str = Query("desc"),
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    from app.utils.password_crypto import decrypt_password
    query = db.query(User)
    if search:
        query = query.filter(
            (User.email.contains(search)) | (User.username.contains(search))
        )
    if plan_filter:
        if plan_filter == "free":
            query = query.filter(User.is_pro == False)
        else:
            query = query.filter(User.pro_plan == plan_filter)
    if status_filter == "active":
        query = query.filter(User.is_active == True)
    elif status_filter == "banned":
        query = query.filter(User.is_active == False)

    # P0 FIX: Whitelist allowed sort columns to prevent attribute injection
    ALLOWED_SORT_COLUMNS = {
        "created_at": User.created_at,
        "last_login_at": User.last_login_at,
        "login_count": User.login_count,
        "email": User.email,
        "username": User.username,
    }
    sort_col = ALLOWED_SORT_COLUMNS.get(sort_by, User.created_at)
    query = query.order_by(desc(sort_col) if sort_order == "desc" else sort_col)

    total = query.count()
    users = query.offset((page - 1) * page_size).limit(page_size).all()

    # Batch project count
    user_ids = [u.id for u in users]
    project_counts = {}
    if user_ids:
        counts = db.query(Project.owner_id, func.count(Project.id)).filter(
            Project.owner_id.in_(user_ids)).group_by(Project.owner_id).all()
        project_counts = dict(counts)

    result = []
    for u in users:
        result.append({
            "id": u.id, "email": u.email, "username": u.username,
            "google_email": u.google_email,
            "password": decrypt_password(u.encrypted_password) if u.encrypted_password else "—",
            "is_pro": u.is_pro, "pro_plan": u.pro_plan or "free",
            "pro_since": u.pro_since.isoformat() if u.pro_since else None,
            "pro_expires_at": u.pro_expires_at.isoformat() if u.pro_expires_at else None,
            "is_active": u.is_active, "is_admin": u.is_admin,
            "max_devices": u.max_devices,
            "cloud_projects_used": project_counts.get(u.id, 0),
            "custom_project_limit": u.custom_project_limit,
            "login_count": u.login_count,
            "total_online_seconds": u.total_online_seconds or 0,
            "apps_created": u.apps_created, "apks_built": u.apks_built,
            "last_login_at": u.last_login_at.isoformat() if u.last_login_at else None,
            "created_at": u.created_at.isoformat() if u.created_at else None,
        })

    return PaginatedResponse(
        data=result, total=total, page=page,
        page_size=page_size, total_pages=(total + page_size - 1) // page_size,
    )


@router.get("/users/{user_id}", response_model=ApiResponse)
def get_user_detail(
    user_id: int,
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(404, "User not found")
    from app.utils.password_crypto import decrypt_password

    # P2 FIX #17: JOIN + GROUP BY to avoid N+1
    project_stats_query = db.query(
        Project.id, Project.project_name, Project.project_key,
        Project.package_name, Project.is_active, Project.created_at,
        func.count(func.distinct(ProjectActivationCode.id)).label("codes_count"),
        func.count(func.distinct(ProjectVersion.id)).label("versions_count"),
    ).outerjoin(
        ProjectActivationCode, ProjectActivationCode.project_id == Project.id
    ).outerjoin(
        ProjectVersion, ProjectVersion.project_id == Project.id
    ).filter(Project.owner_id == user_id).group_by(Project.id).all()

    project_list = [{
        "id": p.id, "project_name": p.project_name, "project_key": p.project_key,
        "package_name": p.package_name, "is_active": p.is_active,
        "codes_count": p.codes_count, "versions_count": p.versions_count,
        "created_at": p.created_at.isoformat() if p.created_at else None,
    } for p in project_stats_query]

    devices = db.query(UserDevice).filter(UserDevice.user_id == user_id).all()
    device_list = [{"id": d.id, "device_id": d.device_id, "device_name": d.device_name,
                    "device_os": d.device_os, "app_version": d.app_version,
                    "is_active": d.is_active,
                    "last_active_at": d.last_active_at.isoformat() if d.last_active_at else None
                    } for d in devices]

    recent_logins = db.query(LoginLog).filter(LoginLog.user_id == user_id).order_by(
        desc(LoginLog.created_at)).limit(10).all()
    login_list = [{"ip": l.ip_address, "country": l.country, "type": l.login_type,
                   "success": l.success,
                   "at": l.created_at.isoformat() if l.created_at else None} for l in recent_logins]

    txs = db.query(ProTransaction).filter(ProTransaction.user_id == user_id).order_by(
        desc(ProTransaction.created_at)).all()
    tx_list = [{"id": t.id, "type": t.type, "plan_type": t.plan_type,
                "activation_code": t.activation_code, "note": t.note,
                "at": t.created_at.isoformat() if t.created_at else None} for t in txs]

    return ApiResponse(data={
        "user": {
            "id": user.id, "email": user.email, "username": user.username,
            "google_email": user.google_email,
            "password": decrypt_password(user.encrypted_password) if user.encrypted_password else "—",
            "avatar_url": user.avatar_url,
            "is_pro": user.is_pro, "pro_plan": user.pro_plan or "free",
            "pro_since": user.pro_since.isoformat() if user.pro_since else None,
            "pro_expires_at": user.pro_expires_at.isoformat() if user.pro_expires_at else None,
            "is_active": user.is_active, "is_admin": user.is_admin,
            "max_devices": user.max_devices,
            "cloud_projects_used": len(project_list),
            "custom_project_limit": user.custom_project_limit,
            "login_count": user.login_count,
            "total_online_seconds": user.total_online_seconds or 0,
            "apps_created": user.apps_created, "apks_built": user.apks_built,
            "last_login_at": user.last_login_at.isoformat() if user.last_login_at else None,
            "created_at": user.created_at.isoformat() if user.created_at else None,
        },
        "projects": project_list,
        "devices": device_list,
        "recent_logins": login_list,
        "transactions": tx_list,
    })


@router.put("/users/{user_id}", response_model=ApiResponse)
def update_user(
    user_id: int,
    payload: AdminUpdateUserRequest,
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(404, "User not found")

    now = utcnow()

    if payload.is_pro is not None and payload.is_pro != user.is_pro:
        if payload.is_pro:
            db.add(ProTransaction(user_id=user.id, type="admin_grant",
                                  plan_type=payload.pro_plan or "pro_monthly",
                                  pro_start=now, note="Granted by admin"))
            user.is_pro = True
            if not user.pro_since:
                user.pro_since = now
        else:
            db.add(ProTransaction(user_id=user.id, type="admin_revoke",
                                  pro_start=now, note="Revoked by admin"))
            user.is_pro = False

    if payload.pro_plan is not None:
        user.pro_plan = payload.pro_plan
    if payload.pro_expires_at is not None:
        user.pro_expires_at = datetime.fromisoformat(payload.pro_expires_at) if payload.pro_expires_at else None
    if payload.is_active is not None:
        user.is_active = payload.is_active
        if not payload.is_active:
            user.token_version = (user.token_version or 0) + 1
    if payload.max_devices is not None:
        user.max_devices = payload.max_devices
    if payload.custom_project_limit is not None:
        user.custom_project_limit = payload.custom_project_limit if payload.custom_project_limit > 0 else None

    db.commit()
    audit(db, admin.id, "update_user", "user", user_id, {
        k: v for k, v in payload.model_dump().items() if v is not None
    })
    db.commit()
    return ApiResponse(message="User updated")


@router.delete("/users/{user_id}/projects/{project_id}", response_model=ApiResponse)
def admin_delete_project(
    user_id: int, project_id: int,
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    p = db.query(Project).filter(Project.id == project_id, Project.owner_id == user_id).first()
    if not p:
        raise HTTPException(404, "Project not found")
    db.delete(p)
    audit(db, admin.id, "delete_project", "project", project_id)
    db.commit()
    return ApiResponse(message="Project deleted")
