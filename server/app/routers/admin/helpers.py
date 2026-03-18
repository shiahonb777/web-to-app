"""Shared helpers for admin sub-modules."""
import logging
from typing import Optional
from fastapi import Request
from sqlalchemy.orm import Session
from app.models.audit_log import AdminAuditLog

logger = logging.getLogger(__name__)


def audit(
    db: Session,
    admin_id: int,
    action: str,
    target_type: str = None,
    target_id: int = None,
    details: dict = None,
    ip: str = None,
    request: Optional[Request] = None,
):
    """
    Record admin action for audit trail.
    
    If 'request' is provided, automatically extracts the client IP.
    """
    # Auto-extract IP from request if available
    if ip is None and request is not None:
        ip = (
            request.headers.get("CF-Connecting-IP")
            or request.headers.get("X-Forwarded-For", "").split(",")[0].strip()
            or (request.client.host if request.client else None)
        )

    if db is not None:
        db.add(AdminAuditLog(
            admin_id=admin_id, action=action,
            target_type=target_type, target_id=target_id,
            details=details, ip_address=ip,
        ))

    logger.info(
        "ADMIN AUDIT: admin=%d action=%s target=%s/%s ip=%s",
        admin_id, action, target_type, target_id, ip,
    )

