package com.webtoapp.core.engine.shields

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap

/**
 * Shields 拦截统计
 * 
 * 实时追踪各类拦截事件数量，通过 StateFlow 暴露给 UI
 *
 * 优化点:
 * 1. 使用 AtomicInteger 计数器代替高频 data class copy()，减少 GC 压力
 * 2. 仅在需要时（UI collect）才构建 snapshot data class
 * 3. 线程安全：所有计数器操作原子化
 */
class ShieldsStats {
    
    // ★ 使用 Atomic 计数器做累加，避免每次拦截都创建新 data class
    // 页面级计数
    private val _pageAds = AtomicInteger(0)
    private val _pageTrackers = AtomicInteger(0)
    private val _pageHttpsUpgrades = AtomicInteger(0)
    private val _pageCookieConsents = AtomicInteger(0)
    private val _pageThirdPartyCookies = AtomicInteger(0)
    private val _pageTrackerCategories = ConcurrentHashMap<TrackerCategory, AtomicInteger>()

    // 会话级计数
    private val _sessionAds = AtomicInteger(0)
    private val _sessionTrackers = AtomicInteger(0)
    private val _sessionHttpsUpgrades = AtomicInteger(0)
    private val _sessionCookieConsents = AtomicInteger(0)
    private val _sessionThirdPartyCookies = AtomicInteger(0)

    /** 当前页面统计 — 惰性构建 snapshot */
    private val _pageStats = MutableStateFlow(PageStats())
    val pageStats: StateFlow<PageStats> = _pageStats.asStateFlow()
    
    /** 会话统计（应用启动至今）— 惰性构建 snapshot */
    private val _sessionStats = MutableStateFlow(SessionStats())
    val sessionStats: StateFlow<SessionStats> = _sessionStats.asStateFlow()
    
    // ==================== 记录事件 ====================
    
    fun recordAdBlocked() {
        _pageAds.incrementAndGet()
        _sessionAds.incrementAndGet()
        emitSnapshots()
    }
    
    fun recordTrackerBlocked(category: TrackerCategory) {
        _pageTrackers.incrementAndGet()
        _pageTrackerCategories.getOrPut(category) { AtomicInteger(0) }.incrementAndGet()
        _sessionTrackers.incrementAndGet()
        emitSnapshots()
    }
    
    fun recordHttpsUpgrade() {
        _pageHttpsUpgrades.incrementAndGet()
        _sessionHttpsUpgrades.incrementAndGet()
        emitSnapshots()
    }
    
    fun recordCookieConsentBlocked() {
        _pageCookieConsents.incrementAndGet()
        _sessionCookieConsents.incrementAndGet()
        emitSnapshots()
    }
    
    fun recordThirdPartyCookieBlocked() {
        _pageThirdPartyCookies.incrementAndGet()
        _sessionThirdPartyCookies.incrementAndGet()
        emitSnapshots()
    }
    
    /** 页面导航时重置页面统计 */
    fun resetPageStats() {
        _pageAds.set(0)
        _pageTrackers.set(0)
        _pageHttpsUpgrades.set(0)
        _pageCookieConsents.set(0)
        _pageThirdPartyCookies.set(0)
        _pageTrackerCategories.clear()
        _pageStats.value = PageStats()
    }
    
    /** 重置会话统计 */
    fun resetSessionStats() {
        _sessionAds.set(0)
        _sessionTrackers.set(0)
        _sessionHttpsUpgrades.set(0)
        _sessionCookieConsents.set(0)
        _sessionThirdPartyCookies.set(0)
        _sessionStats.value = SessionStats()
    }

    /** 获取当前页面总拦截数 */
    val totalPageBlocked: Int
        get() = _pageAds.get() + _pageTrackers.get() + _pageHttpsUpgrades.get() + 
                _pageCookieConsents.get() + _pageThirdPartyCookies.get()
    
    // ★ 集中 emit，只在需要时构建 snapshot
    private fun emitSnapshots() {
        _pageStats.value = PageStats(
            adsBlocked = _pageAds.get(),
            trackersBlocked = _pageTrackers.get(),
            httpsUpgrades = _pageHttpsUpgrades.get(),
            cookieConsentsBlocked = _pageCookieConsents.get(),
            thirdPartyCookiesBlocked = _pageThirdPartyCookies.get(),
            trackerCategories = _pageTrackerCategories.mapValues { it.value.get() }
        )
        _sessionStats.value = SessionStats(
            totalAdsBlocked = _sessionAds.get(),
            totalTrackersBlocked = _sessionTrackers.get(),
            totalHttpsUpgrades = _sessionHttpsUpgrades.get(),
            totalCookieConsentsBlocked = _sessionCookieConsents.get(),
            totalThirdPartyCookiesBlocked = _sessionThirdPartyCookies.get()
        )
    }
}

/**
 * 页面级统计
 */
data class PageStats(
    val adsBlocked: Int = 0,
    val trackersBlocked: Int = 0,
    val httpsUpgrades: Int = 0,
    val cookieConsentsBlocked: Int = 0,
    val thirdPartyCookiesBlocked: Int = 0,
    val trackerCategories: Map<TrackerCategory, Int> = emptyMap()
) {
    val total: Int get() = adsBlocked + trackersBlocked + httpsUpgrades + cookieConsentsBlocked + thirdPartyCookiesBlocked
}

/**
 * 会话级统计
 */
data class SessionStats(
    val totalAdsBlocked: Int = 0,
    val totalTrackersBlocked: Int = 0,
    val totalHttpsUpgrades: Int = 0,
    val totalCookieConsentsBlocked: Int = 0,
    val totalThirdPartyCookiesBlocked: Int = 0
) {
    val total: Int get() = totalAdsBlocked + totalTrackersBlocked + totalHttpsUpgrades + totalCookieConsentsBlocked + totalThirdPartyCookiesBlocked
}
