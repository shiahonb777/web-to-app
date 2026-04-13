package com.webtoapp.core.apkbuilder

internal object ApkEntryPolicy {

    fun isRequiredNativeLib(libName: String, appType: String, engineType: String): Boolean {
        if (libName == "libcrypto_engine.so" || libName == "libc++_shared.so") {
            return true
        }

        if (libName == "libapk_optimizer.so" ||
            libName == "libcrypto_optimized.so" ||
            libName == "libperf_engine.so" ||
            libName == "libbrowser_kernel.so"
        ) {
            return false
        }

        if (libName == "libphp.so") {
            return appType in setOf("WORDPRESS", "PHP_APP")
        }
        if (libName == "libnode_bridge.so" || libName == "libnode.so") {
            return appType == "NODEJS_APP"
        }
        if (libName == "libpython3.so" || libName == "libmusl-linker.so") {
            return appType == "PYTHON_APP"
        }

        val geckoViewLibs = setOf(
            "libgkcodecs.so",
            "libminidump_analyzer.so",
            "libnss3.so",
            "libfreebl3.so",
            "libsoftokn3.so",
            "liblgpllibs.so"
        )
        if (libName in geckoViewLibs) {
            return engineType == "GECKOVIEW"
        }

        return true
    }

    fun isEditorOnlyAsset(entryName: String, appType: String, engineType: String): Boolean {
        if (entryName.startsWith("assets/template/")) return true
        if (entryName.startsWith("assets/sample_projects/")) return true
        if (entryName.startsWith("assets/ai/")) return true
        if (entryName == "assets/litellm_model_prices.json") return true
        if (entryName.startsWith("assets/extensions/")) return true
        if (entryName == "assets/omni.ja" && engineType != "GECKOVIEW") return true
        if (entryName == "assets/php_router_server.php" && appType !in setOf("WORDPRESS", "PHP_APP")) return true
        if (entryName.startsWith("assets/python_runtime/") && appType != "PYTHON_APP") return true
        if (entryName.startsWith("assets/go_runtime/") && appType != "GO_APP") return true
        if (entryName.startsWith("assets/docs/")) return true
        if (entryName.startsWith("assets/help/")) return true
        if (entryName.startsWith("assets/schemas/")) return true
        if (entryName == "assets/default_config.json") return true
        if (entryName.startsWith("assets/frontend_tools/") && appType != "FRONTEND") return true
        if (entryName.startsWith("assets/nodejs_runtime/") && appType != "NODEJS_APP") return true
        return false
    }

    fun isIconEntry(entryName: String): Boolean {
        if (ApkTemplate.ICON_PATHS.any { it.first == entryName } ||
            ApkTemplate.ROUND_ICON_PATHS.any { it.first == entryName }
        ) {
            return true
        }

        val iconPatterns = listOf("ic_launcher.png", "ic_launcher_round.png")
        return iconPatterns.any { pattern ->
            entryName.endsWith(pattern) && (entryName.contains("mipmap") || entryName.contains("drawable"))
        }
    }

    fun isAdaptiveIconEntry(entryName: String): Boolean {
        return entryName.contains("drawable") &&
            (entryName.contains("ic_launcher_foreground") || entryName.contains("ic_launcher_foreground_new")) &&
            (entryName.endsWith(".xml") || entryName.endsWith(".jpg") || entryName.endsWith(".png"))
    }

    fun isAdaptiveIconDefinition(entryName: String): Boolean {
        return entryName.startsWith("res/mipmap-anydpi") &&
            (entryName.contains("ic_launcher") || entryName.contains("ic_launcher_round")) &&
            entryName.endsWith(".xml")
    }
}
