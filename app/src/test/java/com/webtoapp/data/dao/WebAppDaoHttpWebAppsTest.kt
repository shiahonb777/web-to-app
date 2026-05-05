package com.webtoapp.data.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.webtoapp.data.database.AppDatabase
import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.WebApp
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class WebAppDaoHttpWebAppsTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: WebAppDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.webAppDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `http web apps only returns web apps with http urls`() = runTest {
        dao.insert(WebApp(name = "A", url = "http://a.com", appType = AppType.WEB))
        dao.insert(WebApp(name = "B", url = "https://b.com", appType = AppType.WEB))
        dao.insert(WebApp(name = "C", url = "http://c.com", appType = AppType.HTML))

        val apps = dao.getHttpWebApps().first()

        assertThat(apps).hasSize(1)
        assertThat(apps.single().name).isEqualTo("A")
    }

    @Test
    fun `http web apps still match uppercase http scheme`() = runTest {
        dao.insert(WebApp(name = "A", url = "HTTP://a.com", appType = AppType.WEB))

        val apps = dao.getHttpWebApps().first()

        assertThat(apps).hasSize(1)
        assertThat(apps.single().name).isEqualTo("A")
    }
}
