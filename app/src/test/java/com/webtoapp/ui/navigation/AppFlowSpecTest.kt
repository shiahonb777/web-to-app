package com.webtoapp.ui.navigation

import com.webtoapp.data.model.AppType
import org.junit.Assert.assertEquals
import org.junit.Test

class AppFlowSpecTest {

    @Test
    fun `web flow spec honors create edit preview routes`() {
        val spec = AppFlowSpec.from(AppType.WEB)
        assertEquals(Routes.CREATE_APP, spec.createRoute)
        assertEquals(Routes.editApp(42), spec.editRoute(42))
        assertEquals(Routes.preview(42), spec.previewRoute(42))
    }

    @Test
    fun `runtime flows cover node php python go routes`() {
        val nodeSpec = AppFlowSpec.from(AppType.NODEJS_APP)
        assertEquals(Routes.CREATE_NODEJS_APP, nodeSpec.createRoute)
        assertEquals(Routes.editNodeJsApp(99), nodeSpec.editRoute(99))

        val phpSpec = AppFlowSpec.from(AppType.PHP_APP)
        assertEquals(Routes.CREATE_PHP_APP, phpSpec.createRoute)
        assertEquals(Routes.editPhpApp(10), phpSpec.editRoute(10))

        val pythonSpec = AppFlowSpec.from(AppType.PYTHON_APP)
        assertEquals(Routes.CREATE_PYTHON_APP, pythonSpec.createRoute)
        assertEquals(Routes.editPythonApp(7), pythonSpec.editRoute(7))

        val goSpec = AppFlowSpec.from(AppType.GO_APP)
        assertEquals(Routes.CREATE_GO_APP, goSpec.createRoute)
        assertEquals(Routes.editGoApp(88), goSpec.editRoute(88))
    }
}
