"""Admin analytics: multi-dimensional trend data API."""
import logging
from datetime import timedelta, date
from app.utils.time import utcnow
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from sqlalchemy import func, case, extract, and_

from app.database import get_db
from app.dependencies import get_current_admin
from app.models.user import User, LoginLog
from app.models.project import Project
from app.models.module_store import StoreModule
from app.schemas.common import ApiResponse

logger = logging.getLogger(__name__)
router = APIRouter()


def _fill_daily(rows_dict: dict, days: int, now) -> list:
    """Fill missing days with 0."""
    result = []
    for i in range(days):
        d = (now - timedelta(days=days - 1 - i)).date()
        day_str = str(d)
        result.append({"date": day_str, "label": d.strftime("%m-%d"), "value": rows_dict.get(day_str, 0)})
    return result


def _fill_monthly(rows_dict: dict, months: int, now) -> list:
    """Fill missing months with 0."""
    result = []
    d = now.date().replace(day=1)
    for _ in range(months):
        key = d.strftime("%Y-%m")
        result.append({"date": key, "label": d.strftime("%Y-%m"), "value": rows_dict.get(key, 0)})
        # Go back one month
        if d.month == 1:
            d = d.replace(year=d.year - 1, month=12)
        else:
            d = d.replace(month=d.month - 1)
    result.reverse()
    return result


def _fill_yearly(rows_dict: dict, now) -> list:
    """Build yearly data from earliest year to current."""
    if not rows_dict:
        return [{"date": str(now.year), "label": str(now.year), "value": 0}]
    min_year = min(int(k) for k in rows_dict.keys())
    result = []
    for y in range(min_year, now.year + 1):
        result.append({"date": str(y), "label": str(y), "value": rows_dict.get(str(y), 0)})
    return result


@router.get("/analytics/trends", response_model=ApiResponse)
def analytics_trends(
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
    scope: str = Query("daily", regex="^(daily|monthly|yearly|all)$"),
):
    """
    Returns trend data for the admin analytics dashboard.

    scope: daily (last 30 days), monthly (last 12 months), yearly, all (from start)
    """
    now = utcnow()
    data = {}

    if scope == "daily":
        days = 30
        since = now - timedelta(days=days)

        # ── Active users trend (daily) ──
        active_rows = db.query(
            func.date(LoginLog.created_at).label("day"),
            func.count(func.distinct(LoginLog.user_id)).label("cnt"),
        ).filter(
            LoginLog.created_at >= since, LoginLog.success == True
        ).group_by(func.date(LoginLog.created_at)).all()
        active_dict = {str(r.day): r.cnt for r in active_rows}
        data["active"] = _fill_daily(active_dict, days, now)

        # ── Pro subscribers trend (daily) ──
        pro_rows = db.query(
            func.date(User.pro_since).label("day"),
            func.count(case((User.pro_plan.in_(["pro_monthly", "pro_yearly", "pro_lifetime", "lifetime"]), 1))).label("pro_cnt"),
            func.count(case((User.pro_plan.in_(["ultra_monthly", "ultra_yearly", "ultra_lifetime"]), 1))).label("ultra_cnt"),
        ).filter(
            User.pro_since >= since, User.is_pro == True
        ).group_by(func.date(User.pro_since)).all()
        pro_dict = {str(r.day): r.pro_cnt for r in pro_rows}
        ultra_dict = {str(r.day): r.ultra_cnt for r in pro_rows}
        data["pro_subscribers"] = _fill_daily(pro_dict, days, now)
        data["ultra_subscribers"] = _fill_daily(ultra_dict, days, now)

        # ── Store modules trend (daily) ──
        app_rows = db.query(
            func.date(StoreModule.created_at).label("day"),
            func.count(case((StoreModule.module_type == "app", 1))).label("app_cnt"),
            func.count(case((StoreModule.module_type != "app", 1))).label("mod_cnt"),
        ).filter(
            StoreModule.created_at >= since, StoreModule.is_approved == True
        ).group_by(func.date(StoreModule.created_at)).all()
        app_dict = {str(r.day): r.app_cnt for r in app_rows}
        mod_dict = {str(r.day): r.mod_cnt for r in app_rows}
        data["store_apps"] = _fill_daily(app_dict, days, now)
        data["store_modules"] = _fill_daily(mod_dict, days, now)

    elif scope == "monthly":
        months = 12
        since = (now - timedelta(days=365)).replace(day=1)

        # ── Active users trend (monthly) ──
        active_rows = db.query(
            func.date_format(LoginLog.created_at, '%Y-%m').label("month"),
            func.count(func.distinct(LoginLog.user_id)).label("cnt"),
        ).filter(
            LoginLog.created_at >= since, LoginLog.success == True
        ).group_by(func.date_format(LoginLog.created_at, '%Y-%m')).all()
        active_dict = {r.month: r.cnt for r in active_rows}
        data["active"] = _fill_monthly(active_dict, months, now)

        # ── Pro subscribers trend (monthly) ──
        pro_rows = db.query(
            func.date_format(User.pro_since, '%Y-%m').label("month"),
            func.count(case((User.pro_plan.in_(["pro_monthly", "pro_yearly", "pro_lifetime", "lifetime"]), 1))).label("pro_cnt"),
            func.count(case((User.pro_plan.in_(["ultra_monthly", "ultra_yearly", "ultra_lifetime"]), 1))).label("ultra_cnt"),
        ).filter(
            User.pro_since >= since, User.is_pro == True
        ).group_by(func.date_format(User.pro_since, '%Y-%m')).all()
        pro_dict = {r.month: r.pro_cnt for r in pro_rows}
        ultra_dict = {r.month: r.ultra_cnt for r in pro_rows}
        data["pro_subscribers"] = _fill_monthly(pro_dict, months, now)
        data["ultra_subscribers"] = _fill_monthly(ultra_dict, months, now)

        # ── Store modules trend (monthly) ──
        app_rows = db.query(
            func.date_format(StoreModule.created_at, '%Y-%m').label("month"),
            func.count(case((StoreModule.module_type == "app", 1))).label("app_cnt"),
            func.count(case((StoreModule.module_type != "app", 1))).label("mod_cnt"),
        ).filter(
            StoreModule.created_at >= since, StoreModule.is_approved == True
        ).group_by(func.date_format(StoreModule.created_at, '%Y-%m')).all()
        app_dict = {r.month: r.app_cnt for r in app_rows}
        mod_dict = {r.month: r.mod_cnt for r in app_rows}
        data["store_apps"] = _fill_monthly(app_dict, months, now)
        data["store_modules"] = _fill_monthly(mod_dict, months, now)

    elif scope == "yearly":
        # ── Active users trend (yearly) ──
        active_rows = db.query(
            extract("year", LoginLog.created_at).label("yr"),
            func.count(func.distinct(LoginLog.user_id)).label("cnt"),
        ).filter(LoginLog.success == True).group_by(extract("year", LoginLog.created_at)).all()
        active_dict = {str(int(r.yr)): r.cnt for r in active_rows}
        data["active"] = _fill_yearly(active_dict, now)

        # ── Pro subscribers trend (yearly) ──
        pro_rows = db.query(
            extract("year", User.pro_since).label("yr"),
            func.count(case((User.pro_plan.in_(["pro_monthly", "pro_yearly", "pro_lifetime", "lifetime"]), 1))).label("pro_cnt"),
            func.count(case((User.pro_plan.in_(["ultra_monthly", "ultra_yearly", "ultra_lifetime"]), 1))).label("ultra_cnt"),
        ).filter(User.is_pro == True, User.pro_since.isnot(None)).group_by(extract("year", User.pro_since)).all()
        pro_dict = {str(int(r.yr)): r.pro_cnt for r in pro_rows}
        ultra_dict = {str(int(r.yr)): r.ultra_cnt for r in pro_rows}
        data["pro_subscribers"] = _fill_yearly(pro_dict, now)
        data["ultra_subscribers"] = _fill_yearly(ultra_dict, now)

        # ── Store modules trend (yearly) ──
        app_rows = db.query(
            extract("year", StoreModule.created_at).label("yr"),
            func.count(case((StoreModule.module_type == "app", 1))).label("app_cnt"),
            func.count(case((StoreModule.module_type != "app", 1))).label("mod_cnt"),
        ).filter(StoreModule.is_approved == True).group_by(extract("year", StoreModule.created_at)).all()
        app_dict = {str(int(r.yr)): r.app_cnt for r in app_rows}
        mod_dict = {str(int(r.yr)): r.mod_cnt for r in app_rows}
        data["store_apps"] = _fill_yearly(app_dict, now)
        data["store_modules"] = _fill_yearly(mod_dict, now)

    else:  # scope == "all" — cumulative from start ──
        # ── Cumulative active users (monthly granularity, all time) ──
        active_rows = db.query(
            func.date_format(LoginLog.created_at, '%Y-%m').label("month"),
            func.count(func.distinct(LoginLog.user_id)).label("cnt"),
        ).filter(LoginLog.success == True).group_by(
            func.date_format(LoginLog.created_at, '%Y-%m')
        ).order_by(func.date_format(LoginLog.created_at, '%Y-%m')).all()
        data["active"] = [{"date": r.month, "label": r.month, "value": r.cnt} for r in active_rows]

        # ── Cumulative Pro / Ultra subscribers ──
        pro_rows = db.query(
            func.date_format(User.pro_since, '%Y-%m').label("month"),
            func.count(case((User.pro_plan.in_(["pro_monthly", "pro_yearly", "pro_lifetime", "lifetime"]), 1))).label("pro_cnt"),
            func.count(case((User.pro_plan.in_(["ultra_monthly", "ultra_yearly", "ultra_lifetime"]), 1))).label("ultra_cnt"),
        ).filter(
            User.is_pro == True, User.pro_since.isnot(None)
        ).group_by(func.date_format(User.pro_since, '%Y-%m')).order_by(
            func.date_format(User.pro_since, '%Y-%m')
        ).all()
        data["pro_subscribers"] = [{"date": r.month, "label": r.month, "value": r.pro_cnt} for r in pro_rows]
        data["ultra_subscribers"] = [{"date": r.month, "label": r.month, "value": r.ultra_cnt} for r in pro_rows]

        # ── Cumulative store items ──
        app_rows = db.query(
            func.date_format(StoreModule.created_at, '%Y-%m').label("month"),
            func.count(case((StoreModule.module_type == "app", 1))).label("app_cnt"),
            func.count(case((StoreModule.module_type != "app", 1))).label("mod_cnt"),
        ).filter(StoreModule.is_approved == True).group_by(
            func.date_format(StoreModule.created_at, '%Y-%m')
        ).order_by(func.date_format(StoreModule.created_at, '%Y-%m')).all()
        data["store_apps"] = [{"date": r.month, "label": r.month, "value": r.app_cnt} for r in app_rows]
        data["store_modules"] = [{"date": r.month, "label": r.month, "value": r.mod_cnt} for r in app_rows]

    # ── Current totals for summary cards ──
    totals = db.query(
        func.count(User.id).label("total_users"),
        func.count(case((User.pro_plan.in_(["pro_monthly", "pro_yearly", "pro_lifetime", "lifetime"]), 1))).label("pro_count"),
        func.count(case((User.pro_plan.in_(["ultra_monthly", "ultra_yearly", "ultra_lifetime"]), 1))).label("ultra_count"),
    ).filter(User.is_pro == True).first()

    store_totals = db.query(
        func.count(case((StoreModule.module_type == "app", 1))).label("apps"),
        func.count(case((StoreModule.module_type != "app", 1))).label("modules"),
    ).filter(StoreModule.is_approved == True).first()

    data["summary"] = {
        "pro_total": totals.pro_count or 0,
        "ultra_total": totals.ultra_count or 0,
        "store_apps_total": store_totals.apps or 0,
        "store_modules_total": store_totals.modules or 0,
    }

    return ApiResponse(data=data)
