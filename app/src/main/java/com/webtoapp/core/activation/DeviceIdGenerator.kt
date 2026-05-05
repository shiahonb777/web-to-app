package com.webtoapp.core.activation

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.webtoapp.core.logging.AppLogger
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec







object DeviceIdGenerator {

    private const val TAG = "DeviceIdGenerator"
    private const val PREFS_NAME = "device_id_prefs"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_DEVICE_ID_HMAC = "device_id_hmac"
    private const val HMAC_SECRET = "WTA_DeviceId_Integrity_2024"






    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)


        val savedId = prefs.getString(KEY_DEVICE_ID, null)
        val savedHmac = prefs.getString(KEY_DEVICE_ID_HMAC, null)

        if (!savedId.isNullOrBlank() && !savedHmac.isNullOrBlank()) {

            val expectedHmac = computeHmac(savedId)
            if (constantTimeEquals(savedHmac, expectedHmac)) {
                return savedId
            }
            AppLogger.w(TAG, "Device ID integrity check failed — regenerating")
        }


        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )

        val deviceId = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {

            hashString(androidId)
        } else {

            UUID.randomUUID().toString().replace("-", "")
        }


        val hmac = computeHmac(deviceId)
        prefs.edit()
            .putString(KEY_DEVICE_ID, deviceId)
            .putString(KEY_DEVICE_ID_HMAC, hmac)
            .apply()

        AppLogger.i(TAG, "Device ID generated and persisted")
        return deviceId
    }




    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(32)
    }




    private fun computeHmac(data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(HMAC_SECRET.toByteArray(), "HmacSHA256")
        mac.init(keySpec)
        val hmacBytes = mac.doFinal(data.toByteArray())
        return hmacBytes.joinToString("") { "%02x".format(it) }
    }




    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}
