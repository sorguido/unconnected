import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val signingRoot = providers.environmentVariable("UNCONNECTED_SIGNING_ROOT").orNull?.let { rootProject.file(it) }
val unsigned = providers.gradleProperty("unsigned").isPresent
val signingFile = signingRoot?.resolve("development/signing.properties")
val signing = Properties().apply {
    if (!unsigned && signingFile?.isFile == true) signingFile.inputStream().use { load(it) }
}
val signingReady = !unsigned && signingRoot != null &&
    listOf("storeFile", "storePassword", "keyAlias", "keyPassword").all { !signing.getProperty(it).isNullOrBlank() }

android {
    namespace = "it.sorguido.unconnected"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "it.sorguido.unconnected"
        minSdk = 29
        targetSdk = 36
        versionCode = 4
        versionName = "1.0.0"
    }
    signingConfigs {
        if (signingReady) create("originalRelease") {
            val configured = File(signing.getProperty("storeFile"))
            storeFile = if (configured.isAbsolute) configured else signingRoot!!.resolve(configured.path)
            storePassword = signing.getProperty("storePassword")
            keyAlias = signing.getProperty("keyAlias")
            keyPassword = signing.getProperty("keyPassword")
            storeType = "PKCS12"
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-debug" }
        release {
            isMinifyEnabled = false
            if (signingReady) signingConfig = signingConfigs.getByName("originalRelease")
        }
    }
    buildFeatures { compose = true }
    bundle { language { enableSplit = false } }
    dependenciesInfo { includeInApk = false; includeInBundle = false }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint { abortOnError = true; checkReleaseBuilds = true }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencyLocking { lockAllConfigurations() }

val requireOriginalSigning = tasks.register("requireOriginalSigning") {
    doLast {
        check(unsigned || signingReady) {
            "HUMAN_REQUIRED: original Chromecast signing configuration unavailable. Set UNCONNECTED_SIGNING_ROOT, or use -Punsigned only for an explicitly unsigned non-release artifact."
        }
    }
}
tasks.configureEach {
    if (name == "preReleaseBuild") dependsOn(requireOriginalSigning)
}

dependencies {
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui:1.7.8")
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("com.googlecode.libphonenumber:libphonenumber:8.13.55")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
