val coreOnly = providers.gradleProperty("xingxia.coreOnly").orNull == "true"

pluginManagement {
    repositories {
        if (providers.gradleProperty("xingxia.coreOnly").orNull != "true") google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (!coreOnly) google()
        mavenCentral()
    }
}

rootProject.name = "XingXia"

include(":core")

// 只编译、测试纯 Kotlin 的 :core 模块（没有 Android SDK 的环境里使用）：
// ./gradlew -Pxingxia.coreOnly=true :core:test
if (!coreOnly) {
    include(":app")
}
