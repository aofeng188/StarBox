plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// 文案 JSON 直接复用 iOS 工程里的文件，两端只维护一份
sourceSets {
    main {
        resources.srcDir("../../Shared/Content")
        resources.srcDir("../../XingXia/Resources")
        resources.exclude("**/*.swift")
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation(kotlin("test"))
    testImplementation("com.ibm.icu:icu4j:76.1")
}

tasks.test {
    useJUnitPlatform()
}
