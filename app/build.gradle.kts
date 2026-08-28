import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.ksp)
}

android {

  namespace = "com.wanderwildwood.cycle"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.wanderwildwood.cycle"
    // The Kompakt runs 28; nothing here needs anything newer.
    minSdk = 28
    targetSdk = 36
    versionCode = 2
    versionName = "0.1.1"
  }

  // The real keystore in signing/ signs every build type, so the very first install
  // is already on the key it will keep. A debug-signed build now would mean an
  // uninstall later to escape INSTALL_FAILED_UPDATE_INCOMPATIBLE, and an uninstall
  // takes the whole record with it. It is gitignored and nothing is checked in as a
  // fallback, so a clone builds unsigned until you point this at a keystore of your own.
  val signingPropertiesFile = rootProject.file("signing/signing.properties")
  val realSigningConfig = if (signingPropertiesFile.isFile) {
    val signingProperties = Properties().apply {
      signingPropertiesFile.inputStream().use(::load)
    }
    signingConfigs.create("real") {
      storeFile = rootProject.file("signing/signing.keystore")
      storePassword = signingProperties.getProperty("STORE_PASSWORD")
      keyAlias = signingProperties.getProperty("KEY_ALIAS")
      keyPassword = signingProperties.getProperty("KEY_PASSWORD")
      // v3 has to be in the first APK installed or it is no use: rotating a key
      // later is only possible if the installed one already carries a v3 block.
      // Both are asked for, but the APK comes out with only a v3 block -- at
      // minSdk 28 every device that can install it reads v3, so apksigner drops
      // the redundant v2 one. That is correct; do not "fix" the missing v2.
      // `apksigner verify --min-sdk-version 28` is the check that matters.
      enableV2Signing = true
      enableV3Signing = true
    }
  } else {
    null
  }

  buildTypes {
    getByName("debug") {
      realSigningConfig?.let { signingConfig = it }
    }
    getByName("release") {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
      realSigningConfig?.let { signingConfig = it }
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlin {
    compilerOptions {
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  sourceSets {
    named("main") { kotlin.srcDir("src/main/kotlin") }
    named("test") { kotlin.srcDir("src/test/kotlin") }
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime)
  implementation(libs.androidx.activity.compose)

  implementation(platform(libs.compose.bom))
  implementation(libs.compose.ui)
  implementation(libs.compose.material3)
  implementation(libs.compose.ui.tooling.preview)
  debugImplementation(libs.compose.ui.tooling)

  implementation(libs.room.runtime)
  implementation(libs.room.ktx)
  ksp(libs.room.compiler)

  testImplementation(libs.junit)
}
