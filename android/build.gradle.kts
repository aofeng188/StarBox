// 插件放在根 buildscript 的同一个 classloader 里，Kotlin 插件才能找到 Android 插件。
// coreOnly 模式下不加载 Android 插件，方便在没有 Android SDK 的机器上跑 :core 的单元测试。
buildscript {
    val coreOnly = (findProperty("xingxia.coreOnly") as String?) == "true"
    val kotlinVersion = "2.1.0"
    repositories {
        if (!coreOnly) google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        if (!coreOnly) {
            classpath("com.android.tools.build:gradle:8.7.3")
            classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:$kotlinVersion")
        }
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
    }
}
