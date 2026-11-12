import java.util.Properties
import java.util.regex.Pattern

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.ksp)
  alias(libs.plugins.aboutlibraries.android)
}

android {

  namespace = "com.wanderwildwood.cycle"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.wanderwildwood.cycle"
    // The Kompakt runs 28; nothing here needs anything newer.
    minSdk = 28
    targetSdk = 36
    versionCode = 36
    versionName = "0.3.12"
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
        // Release APKs carry arm64-v8a only: every device this app is for is arm64. Debug builds keep the emulator's.
        ndk { abiFilters += listOf("arm64-v8a") }
      // The one thing that differs between a release built here and the one GitHub
      // publishes: AGP stamps the git revision into META-INF, and the build box works
      // from an rsync with no .git, so it writes NO_SUPPORTED_VCS_FOUND where the CI
      // runner writes the commit. Off, so the two have identical contents.
      vcsInfo {
        include = false
      }

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
  implementation(libs.mmd)
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

// The Licenses page lists what the build actually packs: this plugin walks each variant's runtime
// dependencies and writes res/raw/aboutlibraries.json (one entry per artifact, with its POM's
// license). What Gradle cannot see (fonts, data sets, native code, desugaring) and license texts
// of our own choosing are in config/libraries and config/licenses. Offline: the build fetches
// nothing; the full texts come from res/raw and config/licenses. The app reads the file with
// shared-licences' LicencesJson.kt (no AboutLibraries runtime library).
aboutLibraries {
  offlineMode.set(true)
  collect {
    configPath.set(rootProject.file("config"))
    fetchRemoteLicense.set(false)
    fetchRemoteFunding.set(false)
    includePlatform.set(false)
  }
  export {
    excludeFields.addAll("funding")
  }
  library {
    mergePlatformArtifacts.set(true)
    // Only in debug builds (debugImplementation ui-tooling brings these); the page lists the release APK.
    exclusionPatterns.addAll(
      listOf(
        "androidx.compose.ui:ui-tooling", "androidx.compose.ui:ui-tooling-data",
        "androidx.compose.material:material",
      ).map { Pattern.compile("^" + Regex.escape(it) + "$") },
    )
  }
}

// The build fails when a bundled library has no license, or a license with no full text to show
// (neither its own text from config/licenses nor a family text in res/raw). Mudita review, 4 Nov 2026.
val licenceRawTexts: List<String> = file("src/main/res/raw").listFiles().orEmpty().map { it.name.lowercase() }
tasks.matching { it.name.startsWith("prepareLibraryDefinitions") }.configureEach {
  val raw = licenceRawTexts
  inputs.property("licenceRawTexts", raw) // so the check reruns when a text is added or removed
  doLast {
    fun family(n: String): String? {
      val l = n.lowercase()
      return when {
        "classpath" in l -> "classpath"; "lgpl" in l || "lesser general" in l -> "lgpl"
        "gpl" in l || "general public licen" in l -> "gpl"; "unicode" in l -> "unicode"; "apache" in l -> "apache"
        "mpl" in l || "mozilla" in l -> "mpl"; "mit" in l.split(Regex("[^a-z]+")) -> "mit"; "bsd" in l -> "bsd"
        "ofl" in l || "open font" in l -> "ofl"; "cc-by" in l || "cc by" in l || "cc0" in l || "public domain" in l -> "none"
        else -> null
      }
    }
    val bad = mutableListOf<String>()
    outputs.files.asFileTree.matching { include("**/aboutlibraries.json") }.forEach { f ->
      @Suppress("UNCHECKED_CAST")
      val root = groovy.json.JsonSlurper().parse(f) as Map<String, Any?>
      val lics = (root["licenses"] as? Map<String, Map<String, Any?>>).orEmpty()
      (root["libraries"] as? List<Map<String, Any?>>).orEmpty().forEach { lib ->
        val keys = (lib["licenses"] as? List<String>).orEmpty()
        if (keys.isEmpty()) bad += "${lib["uniqueId"]}: no license"
        keys.forEach { k ->
          val e = lics[k].orEmpty()
          if ((e["content"] as? String).isNullOrBlank()) {
            val fam = family((e["spdxId"] ?: e["name"] ?: k).toString())
            if (fam == null || (fam != "none" && raw.none { fam in it }))
              bad += "${lib["uniqueId"]}: no text for ${e["name"] ?: k}"
          }
        }
      }
    }
    if (bad.isNotEmpty()) throw GradleException("Licenses page: add the text in config/licenses or res/raw: " + bad.distinct().joinToString("; "))
  }
}
