package com.webtoapp.core.extension

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExtensionStorageSyncTest {

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ExtensionStorageSync.initialize(context)
        ExtensionStorageSync.clearAll()
    }

    @After
    fun tearDown() {
        ExtensionStorageSync.clearAll()
    }

    @Test
    fun `stores are isolated by area and extension id`() {
        ExtensionStorageSync.set("ext-a", "k", "\"local-a\"", ExtensionStorageSync.Area.LOCAL)
        ExtensionStorageSync.set("ext-a", "k", "\"sync-a\"", ExtensionStorageSync.Area.SYNC)
        ExtensionStorageSync.set("ext-a", "k", "\"session-a\"", ExtensionStorageSync.Area.SESSION)
        ExtensionStorageSync.set("ext-b", "k", "\"local-b\"", ExtensionStorageSync.Area.LOCAL)

        assertThat(ExtensionStorageSync.get("ext-a", "k", ExtensionStorageSync.Area.LOCAL)).isEqualTo("\"local-a\"")
        assertThat(ExtensionStorageSync.get("ext-a", "k", ExtensionStorageSync.Area.SYNC)).isEqualTo("\"sync-a\"")
        assertThat(ExtensionStorageSync.get("ext-a", "k", ExtensionStorageSync.Area.SESSION)).isEqualTo("\"session-a\"")
        assertThat(ExtensionStorageSync.get("ext-b", "k", ExtensionStorageSync.Area.LOCAL)).isEqualTo("\"local-b\"")
        assertThat(ExtensionStorageSync.get("ext-b", "k", ExtensionStorageSync.Area.SYNC)).isEmpty()
    }

    @Test
    fun `getAll and clear apply within one area only`() {
        ExtensionStorageSync.set("ext-a", "one", "1", ExtensionStorageSync.Area.LOCAL)
        ExtensionStorageSync.set("ext-a", "two", "2", ExtensionStorageSync.Area.LOCAL)
        ExtensionStorageSync.set("ext-a", "syncOnly", "3", ExtensionStorageSync.Area.SYNC)

        val localJson = JSONObject(ExtensionStorageSync.getAll("ext-a", ExtensionStorageSync.Area.LOCAL))
        assertThat(localJson.getString("one")).isEqualTo("1")
        assertThat(localJson.getString("two")).isEqualTo("2")
        assertThat(localJson.has("syncOnly")).isFalse()

        ExtensionStorageSync.clear("ext-a", ExtensionStorageSync.Area.LOCAL)

        assertThat(ExtensionStorageSync.get("ext-a", "one", ExtensionStorageSync.Area.LOCAL)).isEmpty()
        assertThat(ExtensionStorageSync.get("ext-a", "syncOnly", ExtensionStorageSync.Area.SYNC)).isEqualTo("3")
    }
}
