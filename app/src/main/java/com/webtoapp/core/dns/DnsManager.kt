package com.webtoapp.core.dns

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.DnsConfig
import com.webtoapp.data.model.DnsProvider
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap















class DnsManager(private val context: Context) {

    companion object {
        private const val TAG = "DnsManager"


        fun isDohSupported(): Boolean {
            return try {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to check DoH support: ${e.message}")
                false
            }
        }
    }

    private val client = OkHttpClient.Builder().build()


    private val dnsCache = ConcurrentHashMap<String, List<InetAddress>>()


    @Volatile
    private var currentConfig: DnsConfig? = null


    private var networkCallback: ConnectivityManager.NetworkCallback? = null






    fun applyDnsConfig(config: DnsConfig) {
        val previousConfig = currentConfig
        currentConfig = config

        if (config.provider == "SYSTEM" || config.effectiveDohUrl.isBlank()) {
            clearDnsConfig()
            return
        }

        AppLogger.d(TAG, "Applying DoH config: provider=${config.provider}, mode=${config.dohMode}")


        if (config.bypassSystemDns) {
            setupBypassSystemDns()
        } else if (previousConfig?.bypassSystemDns == true) {
            clearBypassSystemDns()
        }
    }




    fun clearDnsConfig() {
        currentConfig = null
        dnsCache.clear()
        clearBypassSystemDns()
        AppLogger.d(TAG, "DNS config cleared, using system default")
    }




    fun getCurrentDohUrl(): String? {
        val config = currentConfig ?: return null
        return config.effectiveDohUrl.takeIf { it.isNotBlank() }
    }




    fun getCurrentConfig(): DnsConfig? = currentConfig







    fun resolveWithDoh(hostname: String): List<InetAddress> {
        val config = currentConfig ?: throw UnknownHostException("No DoH config set")
        val dohUrl = config.effectiveDohUrl

        if (dohUrl.isBlank()) {
            throw UnknownHostException("DoH URL is blank")
        }


        dnsCache[hostname]?.let { return it }

        AppLogger.d(TAG, "Resolving $hostname via DoH: $dohUrl")

        val dohDns = DnsOverHttps.Builder()
            .client(client)
            .url(dohUrl.toHttpUrlOrNull() ?: throw UnknownHostException("Invalid DoH URL: $dohUrl"))
            .includeIPv6(true)
            .build()

        val result = try {
            dohDns.lookup(hostname)
        } catch (e: Exception) {
            AppLogger.e(TAG, "DoH resolution failed for $hostname", e)
            if (config.dohMode == "strict") {
                throw UnknownHostException("DoH resolution failed in strict mode: ${e.message}")
            }

            Dns.SYSTEM.lookup(hostname)
        }

        if (result.isNotEmpty()) {
            dnsCache[hostname] = result
        }
        return result
    }





    fun createDohOkHttpClient(): OkHttpClient {
        val config = currentConfig ?: return OkHttpClient.Builder().build()
        val dohUrl = config.effectiveDohUrl

        if (dohUrl.isBlank() || config.provider == "SYSTEM") {
            return OkHttpClient.Builder().build()
        }

        val dohDns = DnsOverHttps.Builder()
            .client(client)
            .url(dohUrl.toHttpUrlOrNull() ?: return OkHttpClient.Builder().build())
            .includeIPv6(true)
            .build()

        return OkHttpClient.Builder()
            .dns(dohDns)
            .build()
    }





    private fun setupBypassSystemDns() {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager


            clearBypassSystemDns()




            AppLogger.d(TAG, "Bypass system DNS enabled — all DNS queries will use DoH")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to setup bypass system DNS", e)
        }
    }




    private fun clearBypassSystemDns() {
        networkCallback?.let {
            try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                cm.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to unregister network callback: ${e.message}")
            }
            networkCallback = null
        }
    }




    fun clearCache() {
        dnsCache.clear()
    }
}
