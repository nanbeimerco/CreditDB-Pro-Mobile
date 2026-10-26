import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.creditdb.pro"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.creditdb.pro"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM & UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Image loading
    implementation(libs.coil.compose)

    // Haze (progressive blur)
    implementation(libs.haze)
    implementation(libs.haze.materials)

    // Kotlinx Serialization & Coroutines
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// 同梱用の DB（assets/creditdb.zip）は git に入れず、ビルド時に最新の Release から取得する。
// 取得し直すときは ./gradlew assembleDebug -PrefreshDb
val bundledDb = file("src/main/assets/creditdb.zip")
val refreshBundledDb = providers.gradleProperty("refreshDb").isPresent
val fetchBundledDb by tasks.registering {
    description = "最新の Release から同梱用の creditdb.zip をダウンロードする"
    outputs.file(bundledDb)
    onlyIf { refreshBundledDb || !bundledDb.exists() }
    outputs.upToDateWhen { !refreshBundledDb }  // -PrefreshDb のときは必ず取り直す
    doLast {
        val api = URI("https://api.github.com/repos/nanbeimerco/CreditDB-Pro-Mobile/releases?per_page=30").toURL()
        val json = api.openStream().use { it.readBytes().toString(Charsets.UTF_8) }
        // Release は新しい順に並ぶので、最初に見つかった creditdb.zip が最新
        val url = Regex(""""browser_download_url"\s*:\s*"([^"]+/creditdb\.zip)"""").find(json)?.groupValues?.get(1)
            ?: throw GradleException("creditdb.zip を含む Release が見つかりません")
        logger.lifecycle("同梱用 DB をダウンロード: $url")
        bundledDb.parentFile.mkdirs()
        val tmp = File(bundledDb.path + ".part")
        URI(url).toURL().openStream().use { input -> tmp.outputStream().use { input.copyTo(it) } }
        if (!tmp.renameTo(bundledDb)) { bundledDb.delete(); tmp.renameTo(bundledDb) }
    }
}
tasks.named("preBuild") { dependsOn(fetchBundledDb) }
