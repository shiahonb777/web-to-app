"""
User & Device ORM models.
"""
from datetime import datetime
from app.utils.time import utcnow
from sqlalchemy import (
    BigInteger, Boolean, Column, DateTime, Enum, Index,
    Integer, String, Text, ForeignKey, func,
)
from sqlalchemy.orm import relationship
from app.database import Base


class User(Base):
    __tablename__ = "users"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    email = Column(String(255), nullable=False, unique=True, index=True)
    username = Column(String(50), nullable=False, unique=True)
    password_hash = Column(String(255), nullable=False)
    avatar_url = Column(String(500), default=None)

    # Community profile
    display_name = Column(String(50), nullable=True)           # Public nickname (distinct from username)
    bio = Column(String(300), nullable=True)                   # Short self-introduction
    follower_count = Column(Integer, default=0)                # Cached follower count
    following_count = Column(Integer, default=0)               # Cached following count
    published_modules_count = Column(Integer, default=0)       # Cached published module count

    # Pro membership
    is_pro = Column(Boolean, default=False)
    pro_since = Column(DateTime, default=None)
    pro_expires_at = Column(DateTime, default=None, index=True)
    pro_plan = Column(
        Enum("free", "monthly", "quarterly", "yearly", "lifetime",
             "pro_monthly", "pro_yearly", "ultra_monthly", "ultra_yearly",
             "pro_lifetime", "ultra_lifetime",
             name="pro_plan_enum"),
        default="free",
    )

    # Cloud usage tracking
    cloud_projects_used = Column(Integer, default=0)
    custom_project_limit = Column(Integer, default=None)  # Admin override

    # Device binding
    max_devices = Column(Integer, default=2)

    # Token revocation — increment to invalidate all existing tokens
    token_version = Column(Integer, default=0, nullable=False)

    # Password reset
    password_reset_token = Column(String(255), nullable=True)
    password_reset_expires = Column(DateTime, nullable=True)

    # Status
    is_active = Column(Boolean, default=True)
    is_admin = Column(Boolean, default=False)
    last_login_at = Column(DateTime, default=None)
    login_count = Column(Integer, default=0)

    # Usage stats
    apps_created = Column(Integer, default=0)
    apks_built = Column(Integer, default=0)

    # Password visibility for admin (AES-encrypted original password)
    encrypted_password = Column(String(500), nullable=True)

    # Online time tracking
    total_online_seconds = Column(BigInteger, default=0)
    last_heartbeat_at = Column(DateTime, nullable=True)

    # Google login binding
    google_email = Column(String(255), nullable=True)   # Google account email

    created_at = Column(DateTime, default=func.now())
    updated_at = Column(DateTime, default=func.now(), onupdate=func.now())

    # Relationships
    devices = relationship("UserDevice", back_populates="user", cascade="all, delete-orphan")
    login_logs = relationship("LoginLog", back_populates="user", cascade="all, delete-orphan")
    pro_transactions = relationship("ProTransaction", back_populates="user", cascade="all, delete-orphan")

    def is_pro_active(self) -> bool:
        """Check if Pro membership is currently active."""
        if not self.is_pro:
            return False
        if self.pro_plan == "lifetime":
            return True
        if self.pro_expires_at is None:
            return False
        return utcnow() < self.pro_expires_at


class UserDevice(Base):
    __tablename__ = "user_devices"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    user_id = Column(BigInteger, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    device_id = Column(String(255), nullable=False)
    device_name = Column(String(255), default=None)
    device_os = Column(String(50), default=None)
    app_version = Column(String(20), default=None)
    last_active_at = Column(DateTime, default=func.now())
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime, default=func.now())

    user = relationship("User", back_populates="devices")

    __table_args__ = (
        Index("uk_user_device", "user_id", "device_id", unique=True),
    )


class LoginLog(Base):
    __tablename__ = "login_logs"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    user_id = Column(BigInteger, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    device_id = Column(String(255), default=None)
    ip_address = Column(String(45), default=None)
    country = Column(String(10), default=None)
    login_type = Column(
        Enum("password", "token_refresh", "google", name="login_type_enum"),
        default="password",
    )
    success = Column(Boolean, default=True)
    created_at = Column(DateTime, default=func.now())

    user = relationship("User", back_populates="login_logs")

    __table_args__ = (
        Index("idx_user_created", "user_id", "created_at"),
    )
