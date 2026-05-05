package com.webtoapp.core.engine.shields

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap











class ShieldsStats {



    private val _pageAds = AtomicInteger(0)
    private val _pageTrackers = AtomicInteger(0)
    private val _pageHttpsUpgrades = AtomicInteger(0)
    private val _pageCookieConsents = AtomicInteger(0)
    private val _pageThirdPartyCookies = AtomicInteger(0)
    private val _pageTrackerCategories = ConcurrentHashMap<TrackerCategory, AtomicInteger>()


    private val _sessionAds = AtomicInteger(0)
    private val _sessionTrackers = AtomicInteger(0)
    private val _sessionHttpsUpgrades = AtomicInteger(0)
    private val _sessionCookieConsents = AtomicInteger(0)
    private val _sessionThirdPartyCookies = AtomicInteger(0)


    private val _pageStats = MutableStateFlow(PageStats())
    val pageStats: StateFlow<PageStats> = _pageStats.asStateFlow()


    private val _sessionStats = MutableStateFlow(SessionStats())
    val sessionStats: StateFlow<SessionStats> = _sessionStats.asStateFlow()



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


    fun resetPageStats() {
        _pageAds.set(0)
        _pageTrackers.set(0)
        _pageHttpsUpgrades.set(0)
        _pageCookieConsents.set(0)
        _pageThirdPartyCookies.set(0)
        _pageTrackerCategories.clear()
        _pageStats.value = PageStats()
    }


    fun resetSessionStats() {
        _sessionAds.set(0)
        _sessionTrackers.set(0)
        _sessionHttpsUpgrades.set(0)
        _sessionCookieConsents.set(0)
        _sessionThirdPartyCookies.set(0)
        _sessionStats.value = SessionStats()
    }


    val totalPageBlocked: Int
        get() = _pageAds.get() + _pageTrackers.get() + _pageHttpsUpgrades.get() +
                _pageCookieConsents.get() + _pageThirdPartyCookies.get()


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




data class SessionStats(
    val totalAdsBlocked: Int = 0,
    val totalTrackersBlocked: Int = 0,
    val totalHttpsUpgrades: Int = 0,
    val totalCookieConsentsBlocked: Int = 0,
    val totalThirdPartyCookiesBlocked: Int = 0
) {
    val total: Int get() = totalAdsBlocked + totalTrackersBlocked + totalHttpsUpgrades + totalCookieConsentsBlocked + totalThirdPartyCookiesBlocked
}
