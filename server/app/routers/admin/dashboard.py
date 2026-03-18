"""Admin dashboard: stats, trends, metrics."""
import logging
from datetime import timedelta
from app.utils.time import utcnow
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from sqlalchemy import func, case

from app.database import get_db
from app.dependencies import get_current_admin
from app.models.user import User, LoginLog
from app.models.activation_code import ActivationCode
from app.models.project import Project
from app.schemas.common import ApiResponse

logger = logging.getLogger(__name__)
router = APIRouter()


@router.get("/dashboard", response_model=ApiResponse)
def dashboard(admin: User = Depends(get_current_admin), db: Session = Depends(get_db)):
    now = utcnow()
    today_start = now.replace(hour=0, minute=0, second=0, microsecond=0)

    # P2 FIX #14: Merged aggregate queries
    user_stats = db.query(
        func.count(User.id).label("total"),
        func.count(case((User.created_at >= today_start, 1))).label("new_today"),
        func.count(case((User.is_pro == True, 1))).label("pro"),
        func.count(case((User.pro_plan.in_(["ultra_monthly", "ultra_yearly", "ultra_lifetime"]), 1))).label("ultra"),
    ).first()
    total_users = user_stats.total or 0
    new_today = user_stats.new_today or 0
    pro_users = user_stats.pro or 0
    ultra_users = user_stats.ultra or 0

    active_today = db.query(func.count(func.distinct(LoginLog.user_id))).filter(
        LoginLog.created_at >= today_start, LoginLog.success == True).scalar() or 0

    project_stats = db.query(
        func.count(Project.id).label("total"),
        func.count(case((Project.is_active == True, 1))).label("active"),
    ).first()
    total_projects = project_stats.total or 0
    active_projects = project_stats.active or 0

    code_stats = db.query(
        func.count(ActivationCode.id).label("total"),
        func.count(case((ActivationCode.status == "unused", 1))).label("unused"),
    ).first()
    total_codes = code_stats.total or 0
    unused_codes = code_stats.unused or 0

    # Login trend — single aggregation
    since = now - timedelta(days=7)
    trend_rows = db.query(
        func.date(LoginLog.created_at).label("day"),
        func.count(func.distinct(LoginLog.user_id)).label("cnt"),
    ).filter(
        LoginLog.created_at >= since, LoginLog.success == True
    ).group_by(func.date(LoginLog.created_at)).all()
    trend_dict = {str(r.day): r.cnt for r in trend_rows}
    login_trend = []
    for i in range(7):
        day = (now - timedelta(days=6 - i)).replace(hour=0, minute=0, second=0, microsecond=0)
        day_str = day.strftime("%Y-%m-%d")
        login_trend.append({"date": day.strftime("%m-%d"), "count": trend_dict.get(day_str, 0)})

    # Revenue estimate
    plan_counts = db.query(User.pro_plan, func.count(User.id)).filter(
        User.is_pro == True).group_by(User.pro_plan).all()
    plan_pricing = {
        "pro_monthly": 3.0, "pro_yearly": 28.80, "pro_lifetime": 0,
        "ultra_monthly": 9.0, "ultra_yearly": 86.40, "ultra_lifetime": 0,
        "lifetime": 0,
    }

    def plan_mrr_factor(plan_name, count):
        if not plan_name:
            return 0
        price = plan_pricing.get(plan_name, 0)
        if "monthly" in plan_name:
            return price * count
        elif "quarterly" in plan_name:
            return price * count / 3.0
        elif "yearly" in plan_name:
            return price * count / 12.0
        return 0

    mrr = sum(plan_mrr_factor(p, c) for p, c in plan_counts)

    return ApiResponse(data={
        "total_users": total_users,
        "new_users_today": new_today,
        "pro_users": pro_users,
        "ultra_users": ultra_users,
        "pro_rate": round(pro_users / total_users * 100, 1) if total_users > 0 else 0,
        "active_today": active_today,
        "total_codes": total_codes,
        "unused_codes": unused_codes,
        "total_projects": total_projects,
        "active_projects": active_projects,
        "mrr": round(mrr, 2),
        "login_trend": login_trend,
    })
