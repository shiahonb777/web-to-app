pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Mozilla Maven 仓库 — GeckoView (Firefox 内核)
        maven { url = uri("https://maven.mozilla.org/maven2") }
    }
}

rootProject.name = "WebToApp"
include(":app")
