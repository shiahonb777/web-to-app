"""Admin: Audit log, cleanup tasks, data exports."""
import csv
import io
import logging
from fastapi import APIRouter, Depends, HTTPException, Query, Request
from fastapi.responses import StreamingResponse
from sqlalchemy.orm import Session
from sqlalchemy import desc

from app.database import get_db
from app.dependencies import get_current_admin
from app.models.user import User
from app.models.project import Project
from app.models.audit_log import AdminAuditLog
from app.schemas.common import ApiResponse
from app.routers.admin.helpers import audit

logger = logging.getLogger(__name__)
router = APIRouter()


# ─── Audit Log ───

@router.get("/audit-log", response_model=ApiResponse)
def get_audit_log(
    page: int = Query(1, ge=1),
    page_size: int = Query(50, ge=1, le=100),
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    query = db.query(AdminAuditLog).order_by(desc(AdminAuditLog.created_at))
    total = query.count()
    logs = query.offset((page - 1) * page_size).limit(page_size).all()
    return ApiResponse(data={
        "total": total, "page": page, "page_size": page_size,
        "logs": [
            {
                "id": l.id, "admin_id": l.admin_id,
                "action": l.action, "target_type": l.target_type,
                "target_id": l.target_id, "details": l.details,
                "created_at": l.created_at.isoformat() if l.created_at else None,
            }
            for l in logs
        ],
    })


# ─── Cleanup Tasks ───

@router.post("/cleanup/view-logs", response_model=ApiResponse)
def trigger_view_log_cleanup(
    days: int = Query(7, ge=1, le=90),
    admin: User = Depends(get_current_admin),
):
    from app.tasks.cleanup import cleanup_old_view_logs
    deleted = cleanup_old_view_logs(days=days)
    audit(None, admin.id, "cleanup_view_logs", details={"days": days, "deleted": deleted})
    return ApiResponse(message=f"Cleaned up {deleted} old view logs")


@router.post("/cleanup/all", response_model=ApiResponse)
def trigger_full_cleanup(admin: User = Depends(get_current_admin)):
    from app.tasks.cleanup import run_all_cleanup
    run_all_cleanup()
    audit(None, admin.id, "cleanup_all")
    return ApiResponse(message="All cleanup tasks completed")


# ─── Data Exports ───
from app.utils.rate_limiter import RateLimiter
_export_limiter = RateLimiter(max_requests=1, window_seconds=300)  # 1 per 5 min

# P0 FIX: Max export rows to prevent memory exhaustion
MAX_EXPORT_ROWS = 10000

@router.get("/export/users")
def export_users(
    request: Request,
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
    limit: int = Query(MAX_EXPORT_ROWS, ge=1, le=MAX_EXPORT_ROWS),
    offset: int = Query(0, ge=0),
):
    """Export user data as CSV. Paginated to prevent memory exhaustion."""
    _export_limiter.check(request, key_suffix="export")
    users = db.query(User).order_by(User.id).offset(offset).limit(limit).all()
    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow([
        "ID", "Email", "Username", "Display Name",
        "Is Pro", "Pro Plan", "Pro Since", "Pro Expires",
        "Follower Count", "Following Count", "Published Modules",
        "Apps Created", "APKs Built", "Max Devices",
        "Is Active", "Is Admin", "Last Login", "Login Count", "Created At",
    ])
    for u in users:
        writer.writerow([
            u.id, u.email, u.username, u.display_name or "",
            u.is_pro, u.pro_plan or "free",
            u.pro_since.isoformat() if u.pro_since else "",
            u.pro_expires_at.isoformat() if u.pro_expires_at else "",
            u.follower_count or 0, u.following_count or 0,
            u.published_modules_count or 0,
            u.apps_created or 0, u.apks_built or 0, u.max_devices or 2,
            u.is_active, u.is_admin,
            u.last_login_at.isoformat() if u.last_login_at else "",
            u.login_count or 0,
            u.created_at.isoformat() if u.created_at else "",
        ])
    audit(None, admin.id, "export_users", details={"count": len(users), "offset": offset})
    output.seek(0)
    return StreamingResponse(
        iter([output.getvalue()]),
        media_type="text/csv",
        headers={"Content-Disposition": "attachment; filename=users_export.csv"},
    )


@router.get("/export/projects")
def export_projects(
    request: Request,
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
    limit: int = Query(MAX_EXPORT_ROWS, ge=1, le=MAX_EXPORT_ROWS),
    offset: int = Query(0, ge=0),
):
    """Export project data as CSV. Paginated to prevent memory exhaustion."""
    _export_limiter.check(request, key_suffix="export_projects")
    projects = db.query(Project).order_by(Project.id).offset(offset).limit(limit).all()
    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow([
        "ID", "Name", "Project Key", "Owner ID",
        "Description", "Is Active", "Package Name",
        "Manifest Version", "Created At",
    ])
    for p in projects:
        writer.writerow([
            p.id, p.name, p.project_key, p.owner_id,
            (p.description or "")[:100], p.is_active, p.package_name or "",
            p.manifest_version or 0,
            p.created_at.isoformat() if p.created_at else "",
        ])
    audit(None, admin.id, "export_projects", details={"count": len(projects), "offset": offset})
    output.seek(0)
    return StreamingResponse(
        iter([output.getvalue()]),
        media_type="text/csv",
        headers={"Content-Disposition": "attachment; filename=projects_export.csv"},
    )


# ═══════════════════════════════════════════
#  SYSTEM HEALTH & MAINTENANCE
# ═══════════════════════════════════════════

@router.get("/health", response_model=ApiResponse)
def system_health_check(
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    """Comprehensive system health check."""
    import os
    import time
    import psutil

    # Uptime
    boot_time = psutil.boot_time()
    uptime_seconds = int(time.time() - boot_time)
    days, remainder = divmod(uptime_seconds, 86400)
    hours, remainder = divmod(remainder, 3600)
    minutes, _ = divmod(remainder, 60)
    uptime_str = f"{days}天 {hours}时 {minutes}分"

    # Memory
    mem = psutil.virtual_memory()
    mem_str = f"{mem.used // (1024*1024)}MB / {mem.total // (1024*1024)}MB ({mem.percent}%)"

    # CPU
    cpu_percent = psutil.cpu_percent(interval=0.5)

    # Disk
    disk = psutil.disk_usage('/')
    disk_str = f"{disk.used // (1024*1024*1024)}GB / {disk.total // (1024*1024*1024)}GB ({disk.percent}%)"

    # DB check
    db_ok = True
    try:
        from sqlalchemy import text as sa_text
        db.execute(sa_text("SELECT 1"))
    except Exception:
        db_ok = False

    # DB pool info
    from app.database import engine
    pool = engine.pool
    pool_status = {
        "pool_size": pool.size() if hasattr(pool, 'size') else '-',
        "checked_in": pool.checkedin() if hasattr(pool, 'checkedin') else '-',
        "checked_out": pool.checkedout() if hasattr(pool, 'checkedout') else '-',
    }

    return ApiResponse(data={
        "status": "healthy" if db_ok else "degraded",
        "uptime": uptime_str,
        "db_connections": f"{pool_status['checked_out']}/{pool_status['pool_size']}",
        "memory_usage": mem_str,
        "cpu_usage": f"{cpu_percent}%",
        "disk_usage": disk_str,
        "database": "connected" if db_ok else "error",
        "python_pid": os.getpid(),
    })


@router.get("/stats", response_model=ApiResponse)
def system_stats(
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    """System-wide statistics."""
    from sqlalchemy import func
    from app.models.activation_code import ActivationCode
    from app.models.module_store import StoreModule
    from app.models.announcement import Announcement

    total_users = db.query(func.count(User.id)).scalar() or 0
    active_users = db.query(func.count(User.id)).filter(User.is_active == True).scalar() or 0
    pro_users = db.query(func.count(User.id)).filter(User.is_pro == True).scalar() or 0
    admin_users = db.query(func.count(User.id)).filter(User.is_admin == True).scalar() or 0
    total_projects = db.query(func.count(Project.id)).scalar() or 0
    total_codes = db.query(func.count(ActivationCode.id)).scalar() or 0
    used_codes = db.query(func.count(ActivationCode.id)).filter(ActivationCode.status == "used").scalar() or 0
    total_modules = db.query(func.count(StoreModule.id)).scalar() or 0
    total_announcements = db.query(func.count(Announcement.id)).scalar() or 0
    total_audit_logs = db.query(func.count(AdminAuditLog.id)).scalar() or 0

    return ApiResponse(data={
        "总用户数": total_users,
        "活跃用户": active_users,
        "Pro 用户": pro_users,
        "管理员": admin_users,
        "云项目": total_projects,
        "激活码总数": total_codes,
        "已使用激活码": used_codes,
        "社区模块": total_modules,
        "公告数": total_announcements,
        "审计日志": total_audit_logs,
    })


@router.post("/clear-cache", response_model=ApiResponse)
def clear_cache(
    admin: User = Depends(get_current_admin),
):
    """Clear all in-memory caches (rate limiters, intelligence caches)."""
    cleared = []
    try:
        from app.utils.rate_limiter import auth_limiter, register_limiter, api_limiter
        auth_limiter._buckets.clear()
        register_limiter._buckets.clear()
        api_limiter._buckets.clear()
        cleared.append("rate_limiters")
    except Exception:
        pass

    try:
        from app.services.intelligence.threat_scorer import threat_scorer
        threat_scorer._scores.clear()
        cleared.append("threat_scores")
    except Exception:
        pass

    try:
        from app.services.intelligence.circuit_breaker import circuit_manager
        for name in list(circuit_manager._breakers.keys()):
            circuit_manager._breakers[name].reset()
        cleared.append("circuit_breakers")
    except Exception:
        pass

    audit(None, admin.id, "clear_cache", details={"cleared": cleared})
    return ApiResponse(
        message=f"已清除 {len(cleared)} 类缓存: {', '.join(cleared) or '无'}",
        data={"cleared": cleared},
    )


@router.post("/backup", response_model=ApiResponse)
def create_db_backup(
    admin: User = Depends(get_current_admin),
):
    """Create a MySQL database backup using mysqldump."""
    import subprocess
    import os
    from app.config import get_settings

    settings = get_settings()
    db_url = settings.DATABASE_URL

    # Parse database URL to extract credentials
    # Format: mysql+pymysql://user:pass@host:port/dbname
    try:
        from urllib.parse import urlparse
        parsed = urlparse(db_url.replace("mysql+pymysql://", "mysql://"))
        db_user = parsed.username
        db_pass = parsed.password
        db_host = parsed.hostname or "127.0.0.1"
        db_port = parsed.port or 3306
        db_name = parsed.path.lstrip("/")
    except Exception as e:
        return ApiResponse(success=False, message=f"无法解析数据库连接: {str(e)}")

    # Create backup directory
    backup_dir = "/www/wwwroot/webtoapp-api/backups"
    os.makedirs(backup_dir, exist_ok=True)

    from app.utils.time import utcnow
    timestamp = utcnow().strftime("%Y%m%d_%H%M%S")
    backup_file = os.path.join(backup_dir, f"{db_name}_{timestamp}.sql.gz")

    try:
        cmd = (
            f"mysqldump -u {db_user} -p'{db_pass}' -h {db_host} -P {db_port} "
            f"--single-transaction --quick {db_name} | gzip > {backup_file}"
        )
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=120)
        if result.returncode != 0:
            return ApiResponse(success=False, message=f"备份失败: {result.stderr[:200]}")

        file_size = os.path.getsize(backup_file)
        size_str = f"{file_size / (1024*1024):.1f}MB" if file_size > 1024*1024 else f"{file_size // 1024}KB"

        audit(None, admin.id, "db_backup", details={
            "file": backup_file, "size": size_str,
        })
        return ApiResponse(
            message=f"备份完成: {size_str}",
            data={"file": backup_file, "size": size_str, "timestamp": timestamp},
        )
    except subprocess.TimeoutExpired:
        return ApiResponse(success=False, message="备份超时 (>120s)")
    except Exception as e:
        return ApiResponse(success=False, message=f"备份异常: {str(e)}")


BACKUP_DIR = "/www/wwwroot/webtoapp-api/backups"


@router.get("/backups", response_model=ApiResponse)
def list_backups(
    admin: User = Depends(get_current_admin),
):
    """List all database backup files."""
    import os
    from datetime import datetime

    if not os.path.exists(BACKUP_DIR):
        return ApiResponse(data={"backups": [], "total_size": "0KB"})

    backups = []
    total_bytes = 0
    for fname in sorted(os.listdir(BACKUP_DIR), reverse=True):
        if not fname.endswith((".sql.gz", ".sql", ".gz")):
            continue
        fpath = os.path.join(BACKUP_DIR, fname)
        if not os.path.isfile(fpath):
            continue
        stat = os.stat(fpath)
        size = stat.st_size
        total_bytes += size
        size_str = f"{size / (1024*1024):.1f}MB" if size > 1024*1024 else f"{size // 1024}KB"
        created = datetime.fromtimestamp(stat.st_mtime).strftime("%Y-%m-%d %H:%M:%S")
        backups.append({
            "filename": fname,
            "size": size_str,
            "size_bytes": size,
            "created_at": created,
        })

    total_str = f"{total_bytes / (1024*1024):.1f}MB" if total_bytes > 1024*1024 else f"{total_bytes // 1024}KB"
    return ApiResponse(data={
        "backups": backups,
        "count": len(backups),
        "total_size": total_str,
    })


@router.get("/backups/{filename}", response_model=None)
def download_backup(
    filename: str,
    request: Request,
    token: str = Query(None, description="JWT token for browser download"),
    db: Session = Depends(get_db),
):
    """Download a backup file. Accepts token via query param for browser download."""
    import os
    from fastapi.responses import FileResponse
    from app.utils.security import decode_token

    # Auth: try query param token first, then header
    user = None
    if token:
        payload = decode_token(token)
        if payload and payload.get("sub"):
            user = db.query(User).filter(User.id == int(payload["sub"])).first()
    if not user:
        auth_header = request.headers.get("Authorization", "")
        if auth_header.startswith("Bearer "):
            payload = decode_token(auth_header[7:])
            if payload and payload.get("sub"):
                user = db.query(User).filter(User.id == int(payload["sub"])).first()
    if not user or not user.is_admin:
        raise HTTPException(401, "Admin authentication required")

    # Prevent path traversal
    safe_name = os.path.basename(filename)
    fpath = os.path.join(BACKUP_DIR, safe_name)
    if not os.path.isfile(fpath):
        raise HTTPException(404, "Backup file not found")

    return FileResponse(
        fpath,
        media_type="application/gzip",
        filename=safe_name,
    )


@router.delete("/backups/{filename}", response_model=ApiResponse)
def delete_backup(
    filename: str,
    admin: User = Depends(get_current_admin),
):
    """Delete a backup file."""
    import os

    safe_name = os.path.basename(filename)
    fpath = os.path.join(BACKUP_DIR, safe_name)
    if not os.path.isfile(fpath):
        raise HTTPException(404, "Backup file not found")

    size = os.path.getsize(fpath)
    os.remove(fpath)
    audit(None, admin.id, "delete_backup", details={"file": safe_name, "size": size})
    return ApiResponse(message=f"已删除备份: {safe_name}")

# ═══════════════════════════════════════════
#  INTELLIGENCE DASHBOARD
# ═══════════════════════════════════════════

# (overview endpoint consolidated at bottom with advanced modules)


@router.get("/intelligence/threats")
def intelligence_threats(
    n: int = Query(50, ge=1, le=200),
    admin: User = Depends(get_current_admin),
):
    """Detailed threat actor list."""
    from app.services.intelligence.threat_scorer import threat_scorer
    return ApiResponse(data=threat_scorer.get_top_threats(n))


@router.get("/intelligence/circuits")
def intelligence_circuits(admin: User = Depends(get_current_admin)):
    """Circuit breaker status for all downstream services."""
    from app.services.intelligence.circuit_breaker import circuit_manager
    return ApiResponse(data=circuit_manager.get_all_status())


@router.get("/intelligence/abuse")
def intelligence_abuse(
    n: int = Query(50, ge=1, le=200),
    admin: User = Depends(get_current_admin),
):
    """Suspicious community engagement activity."""
    from app.services.intelligence.abuse_detector import abuse_detector
    return ApiResponse(data=abuse_detector.get_suspicious_modules(n))


@router.get("/intelligence/advanced")
def intelligence_advanced(admin: User = Depends(get_current_admin)):
    """
    Advanced intelligence dashboard — honeypots, fingerprints,
    Markov model, backpressure, IP reputation, impossible travel.
    """
    from app.services.intelligence.advanced import adv_intel
    return ApiResponse(data=adv_intel.get_full_status())


@router.get("/intelligence/overview")
def intelligence_overview_full(admin: User = Depends(get_current_admin)):
    """
    Combined intelligence dashboard — all 9 sub-systems in one response.
    """
    from app.services.intelligence.threat_scorer import threat_scorer
    from app.services.intelligence.circuit_breaker import circuit_manager
    from app.services.intelligence.abuse_detector import abuse_detector
    from app.services.intelligence.advanced import adv_intel

    top_threats = threat_scorer.get_top_threats(10)

    return ApiResponse(data={
        "threats": {
            "top_actors": top_threats,
            "critical_count": sum(1 for t in top_threats if t["risk_level"] == "CRITICAL"),
            "high_count": sum(1 for t in top_threats if t["risk_level"] == "HIGH"),
        },
        "circuits": circuit_manager.get_all_status(),
        "abuse": {
            "suspicious_modules": abuse_detector.get_suspicious_modules(10),
        },
        "advanced": adv_intel.get_full_status(),
    })

