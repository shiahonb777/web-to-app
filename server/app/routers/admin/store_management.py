"""Admin: Store management — manage apps and extension modules in the marketplace."""
import logging
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel
from sqlalchemy.orm import Session, joinedload
from sqlalchemy import desc, func, or_

from app.database import get_db
from app.dependencies import get_current_admin
from app.models.user import User
from app.models.module_store import StoreModule, ModuleComment
from app.schemas.common import ApiResponse
from app.routers.admin.helpers import audit

logger = logging.getLogger(__name__)
router = APIRouter()


# ── Pydantic schemas ──

class AdminUpdateStoreItem(BaseModel):
    """Admin fields that can be updated on any store item."""
    name: Optional[str] = None
    description: Optional[str] = None
    category: Optional[str] = None
    icon: Optional[str] = None
    tags: Optional[str] = None
    is_approved: Optional[bool] = None
    is_featured: Optional[bool] = None
    package_name: Optional[str] = None
    version_name: Optional[str] = None


# ── Dashboard stats ──

@router.get("/store/stats", response_model=ApiResponse)
def store_stats(
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    """Get marketplace overview statistics."""
    total_apps = db.query(func.count(StoreModule.id)).filter(
        StoreModule.module_type == "app"
    ).scalar() or 0

    total_extensions = db.query(func.count(StoreModule.id)).filter(
        StoreModule.module_type == "extension"
    ).scalar() or 0

    total_browser_ext = db.query(func.count(StoreModule.id)).filter(
        StoreModule.module_type == "browser_extension"
    ).scalar() or 0

    total_featured = db.query(func.count(StoreModule.id)).filter(
        StoreModule.is_featured == True
    ).scalar() or 0

    pending_count = db.query(func.count(StoreModule.id)).filter(
        StoreModule.is_approved == False
    ).scalar() or 0

    total_downloads = db.query(func.coalesce(func.sum(StoreModule.downloads), 0)).scalar() or 0

    return ApiResponse(data={
        "total_apps": total_apps,
        "total_extensions": total_extensions,
        "total_browser_extensions": total_browser_ext,
        "total_featured": total_featured,
        "pending_count": pending_count,
        "total_downloads": total_downloads,
        "total_items": total_apps + total_extensions + total_browser_ext,
    })


# ── List all store items (with search, filter, sort) ──

@router.get("/store/items", response_model=ApiResponse)
def list_store_items(
    module_type: Optional[str] = Query(None, description="Filter by type: app, extension, browser_extension"),
    status: Optional[str] = Query(None, description="Filter: approved, pending, featured"),
    search: Optional[str] = Query(None, description="Search by name, description, tags"),
    sort: str = Query("created_at", description="Sort field"),
    order: str = Query("desc", description="Sort order: asc/desc"),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    """List all store items with full admin filters."""
    query = db.query(StoreModule).options(joinedload(StoreModule.author))

    # Type filter
    if module_type and module_type in ("app", "extension", "browser_extension"):
        query = query.filter(StoreModule.module_type == module_type)

    # Status filter
    if status == "approved":
        query = query.filter(StoreModule.is_approved == True)
    elif status == "pending":
        query = query.filter(StoreModule.is_approved == False)
    elif status == "featured":
        query = query.filter(StoreModule.is_featured == True)

    # Search
    if search:
        like = f"%{search}%"
        query = query.filter(or_(
            StoreModule.name.ilike(like),
            StoreModule.description.ilike(like),
            StoreModule.tags.ilike(like),
        ))

    # Sort
    sort_col = {
        "created_at": StoreModule.created_at,
        "downloads": StoreModule.downloads,
        "rating": StoreModule.rating,
        "like_count": StoreModule.like_count,
        "view_count": StoreModule.view_count,
        "name": StoreModule.name,
    }.get(sort, StoreModule.created_at)
    from sqlalchemy import asc as sa_asc
    query = query.order_by(desc(sort_col) if order == "desc" else sa_asc(sort_col))

    total = query.count()
    items = query.offset((page - 1) * page_size).limit(page_size).all()

    return ApiResponse(data={
        "total": total,
        "page": page,
        "page_size": page_size,
        "total_pages": (total + page_size - 1) // page_size,
        "items": [_serialize_admin(m) for m in items],
    })


# ── Get single item detail ──

@router.get("/store/items/{item_id}", response_model=ApiResponse)
def get_store_item(
    item_id: int,
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    """Get full detail of a store item."""
    item = db.query(StoreModule).options(
        joinedload(StoreModule.author)
    ).filter(StoreModule.id == item_id).first()
    if not item:
        raise HTTPException(404, "Store item not found")
    return ApiResponse(data=_serialize_admin(item, full=True))


# ── Update item ──

@router.put("/store/items/{item_id}", response_model=ApiResponse)
def update_store_item(
    item_id: int,
    payload: AdminUpdateStoreItem,
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    """Admin update any store item's metadata, approval, or featured status."""
    item = db.query(StoreModule).filter(StoreModule.id == item_id).first()
    if not item:
        raise HTTPException(404, "Store item not found")

    changes = {}
    for field in ["name", "description", "category", "icon", "tags",
                   "is_approved", "is_featured", "package_name", "version_name"]:
        val = getattr(payload, field, None)
        if val is not None:
            old_val = getattr(item, field, None)
            setattr(item, field, val)
            changes[field] = {"old": old_val, "new": val}

    audit(db, admin.id, "update_store_item", "store_module", item_id, changes)
    db.commit()
    db.refresh(item)
    return ApiResponse(message="Store item updated", data=_serialize_admin(item))


# ── Delete item ──

@router.delete("/store/items/{item_id}", response_model=ApiResponse)
def delete_store_item(
    item_id: int,
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    """Admin delete any store item."""
    item = db.query(StoreModule).filter(StoreModule.id == item_id).first()
    if not item:
        raise HTTPException(404, "Store item not found")
    audit(db, admin.id, "delete_store_item", "store_module", item_id,
          {"name": item.name, "type": item.module_type})
    db.delete(item)
    db.commit()
    return ApiResponse(message="Store item deleted")


# ── Batch actions ──

class BatchActionRequest(BaseModel):
    ids: list[int]
    action: str  # approve, reject, feature, unfeature, delete


@router.post("/store/batch", response_model=ApiResponse)
def batch_store_action(
    payload: BatchActionRequest,
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    """Batch approve/reject/feature/unfeature/delete store items."""
    items = db.query(StoreModule).filter(StoreModule.id.in_(payload.ids)).all()
    if not items:
        raise HTTPException(404, "No items found")

    count = 0
    for item in items:
        if payload.action == "approve":
            item.is_approved = True
        elif payload.action == "reject":
            item.is_approved = False
        elif payload.action == "feature":
            item.is_featured = True
        elif payload.action == "unfeature":
            item.is_featured = False
        elif payload.action == "delete":
            db.delete(item)
        count += 1

    audit(db, admin.id, f"batch_{payload.action}", "store_module", None,
          {"ids": payload.ids, "count": count})
    db.commit()
    return ApiResponse(message=f"{count} items {payload.action}d")


# ── Serialization ──

def _serialize_admin(m: StoreModule, full: bool = False) -> dict:
    """Serialize store item for admin panel."""
    data = {
        "id": m.id,
        "module_type": m.module_type,
        "name": m.name,
        "icon": m.icon,
        "category": m.category,
        "tags": m.tags,
        "version_name": m.version_name,
        "version_code": m.version_code,
        "package_name": m.package_name,
        "downloads": m.downloads or 0,
        "view_count": m.view_count or 0,
        "like_count": m.like_count or 0,
        "rating": m.rating or 0.0,
        "rating_count": m.rating_count or 0,
        "comment_count": m.comment_count or 0,
        "is_approved": m.is_approved,
        "is_featured": m.is_featured,
        "author_name": m.author.username if m.author else "Unknown",
        "author_id": m.author_id,
        "created_at": m.created_at.isoformat() if m.created_at else None,
        "updated_at": m.updated_at.isoformat() if m.updated_at else None,
    }
    if full:
        data.update({
            "description": m.description,
            "screenshots": m.screenshots or [],
            "video_url": m.video_url,
            "apk_url_github": m.apk_url_github,
            "apk_url_gitee": m.apk_url_gitee,
            "contact_email": m.contact_email,
            "website_url": m.website_url,
            "privacy_policy_url": m.privacy_policy_url,
            "file_size": m.file_size or 0,
            "storage_url_github": m.storage_url_github,
            "storage_url_gitee": m.storage_url_gitee,
        })
    return data
