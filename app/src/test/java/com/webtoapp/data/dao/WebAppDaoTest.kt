package com.webtoapp.data.dao

import android.content.Context
import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.webtoapp.data.database.AppDatabase
import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.WebApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class WebAppDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: WebAppDao

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.webAppDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `startup candidates only return web apps without icons`() = runTest {
        dao.insert(
            WebApp(name = "A", url = "http://a.com", appType = AppType.WEB)
        )
        dao.insert(
            WebApp(name = "B", url = "http://b.com", iconPath = "file:///icon.png", appType = AppType.WEB)
        )
        dao.insert(
            WebApp(name = "C", url = "https://c.com", appType = AppType.HTML)
        )

        val candidates = dao.getStartupCandidatesWithoutIcons(AppType.WEB, 10)

        assertThat(candidates).hasSize(1)
        assertThat(candidates.single().name).isEqualTo("A")
    }

    @Test
    fun `upgrade remote http urls only touches web apps`() = runTest {
        val webId = dao.insert(WebApp(name = "A", url = "http://a.com", appType = AppType.WEB))
        dao.insert(WebApp(name = "B", url = "http://b.com", appType = AppType.HTML))

        val updated = dao.upgradeRemoteHttpUrls(AppType.WEB, 123L)

        assertThat(updated).isEqualTo(1)
        assertThat(dao.getWebAppById(webId)?.url).isEqualTo("https://a.com")
    }

    @Test
    fun `getAllWebAppSummaries returns lightweight rows with flag columns`() = runTest {
        dao.insert(
            WebApp(
                name = "A",
                url = "https://a.com",
                appType = AppType.WEB,
                iconPath = "file:///a.png",
                categoryId = 7L,
                activationEnabled = true,
                adBlockEnabled = false,
                announcementEnabled = true,
            )
        )
        dao.insert(
            WebApp(
                name = "B",
                url = "",
                appType = AppType.HTML,
                activationEnabled = false,
                adBlockEnabled = true,
                announcementEnabled = false,
            )
        )

        val summaries = dao.getAllWebAppSummaries().first()

        assertThat(summaries).hasSize(2)
        val byName = summaries.associateBy { it.name }
        val a = byName.getValue("A")
        assertThat(a.url).isEqualTo("https://a.com")
        assertThat(a.iconPath).isEqualTo("file:///a.png")
        assertThat(a.appType).isEqualTo(AppType.WEB)
        assertThat(a.categoryId).isEqualTo(7L)
        assertThat(a.activationEnabled).isTrue()
        assertThat(a.adBlockEnabled).isFalse()
        assertThat(a.announcementEnabled).isTrue()

        val b = byName.getValue("B")
        assertThat(b.url).isEmpty()
        assertThat(b.iconPath).isNull()
        assertThat(b.appType).isEqualTo(AppType.HTML)
        assertThat(b.categoryId).isNull()
        assertThat(b.activationEnabled).isFalse()
        assertThat(b.adBlockEnabled).isTrue()
        assertThat(b.announcementEnabled).isFalse()
    }
}
