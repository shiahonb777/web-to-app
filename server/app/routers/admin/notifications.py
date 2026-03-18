"""Admin: Push notifications and in-app messaging."""
import logging
from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import get_current_admin
from app.models.user import User
from app.schemas.common import ApiResponse
from app.routers.admin.helpers import audit

logger = logging.getLogger(__name__)
router = APIRouter()


class GlobalPushRequest(BaseModel):
    title: str = Field(..., min_length=1, max_length=200)
    body: str = Field(..., min_length=1, max_length=1000)
    topic: str = Field("all_users", description="FCM topic to send to")
    data: dict = Field(default_factory=dict, description="Optional data payload")


class UserPushRequest(BaseModel):
    user_id: int
    title: str = Field(..., min_length=1, max_length=200)
    body: str = Field(..., min_length=1, max_length=1000)


@router.post("/push/global", response_model=ApiResponse)
async def send_global_push(
    req: GlobalPushRequest,
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    """Send a push notification to ALL users via FCM topic."""
    try:
        from app.services.fcm_service import fcm_service
        if not fcm_service.is_available:
            raise HTTPException(503, "FCM service is not configured on the server")

        result = await fcm_service.send_to_topic(
            topic=req.topic, title=req.title,
            body=req.body, data={**req.data, "type": "global_push"},
        )

        from app.models.notification import PushHistory
        db.add(PushHistory(
            project_id=None, sender_id=admin.id,
            title=req.title, body=req.body, topic=req.topic,
            message_id=result if result else None,
            status="sent" if result else "failed",
        ))

        audit(db, admin.id, "global_push", details={
            "title": req.title, "topic": req.topic, "message_id": result,
        })
        db.commit()

        if result:
            return ApiResponse(
                message=f"Global push sent to topic '{req.topic}'",
                data={"message_id": result, "topic": req.topic},
            )
        else:
            raise HTTPException(500, "Failed to send push notification")

    except ImportError:
        raise HTTPException(503, "FCM module not available")
    except HTTPException:
        raise
    except Exception as e:
        logger.error("Global push failed: %s", e, exc_info=True)
        raise HTTPException(500, f"Push failed: {str(e)}")


@router.post("/push/user", response_model=ApiResponse)
def send_user_notification(
    req: UserPushRequest,
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    """Send an in-app notification to a specific user."""
    target = db.query(User).filter(User.id == req.user_id, User.is_active == True).first()
    if not target:
        raise HTTPException(404, "User not found")

    from app.models.notification import Notification
    db.add(Notification(
        user_id=req.user_id, type="system",
        title=req.title, content=req.body, actor_id=admin.id,
    ))
    audit(db, admin.id, "send_notification", "user", req.user_id,
          details={"title": req.title})
    db.commit()
    return ApiResponse(message=f"Notification sent to user {target.username}")
