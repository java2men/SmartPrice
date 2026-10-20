import java.util.Calendar

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "ru.embtlab.smartprice"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "ru.embtlab.smartprice"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        val buildYear = Calendar.getInstance().get(Calendar.YEAR).toString()
        buildConfigField("String", "BUILD_YEAR", "\"$buildYear\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    flavorDimensions += "distribution"

    productFlavors {
        create("rustore") {
            dimension = "distribution"
            applicationId = "ru.embtlab.smartprice"

            // Ключи для сборки под RuStore
            buildConfigField("String", "MY_TRACKER_KEY", "\"68878970625945961690\"")
            buildConfigField("int", "VK_BANNER_SLOT_ID", "2093874")
            buildConfigField("Boolean", "ADS_ENABLED", "true")
        }

        create("google") {
            dimension = "distribution"
            applicationId = "ru.embtlab.smartprice.play"

            // Отдельные ключи из кабинетов, созданные для Google Play версии:
            buildConfigField("String", "MY_TRACKER_KEY", "\"25906024298703320363\"")
            buildConfigField("int", "VK_BANNER_SLOT_ID", "2094146") // ID слота для площадки Google Play
            buildConfigField("Boolean", "ADS_ENABLED", "true")
        }

        create("free") {
            dimension = "distribution"
            applicationId = "ru.embtlab.smartprice.dev"
            versionNameSuffix = "-dev"

            // В свободной сборке ключи пустые, реклама отключена
            buildConfigField("String", "MY_TRACKER_KEY", "\"\"")
            buildConfigField("int", "VK_BANNER_SLOT_ID", "0")
            buildConfigField("Boolean", "ADS_ENABLED", "false")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // ML Kit Text Recognition
    implementation(libs.google.mlkit.text.recognition)

    // Accompanist Permissions
    implementation(libs.accompanist.permissions)

    implementation(libs.androidx.core.splashscreen)

    // Реклама и аналитика VK
//    implementation(libs.vk.ads.sdk)
//    implementation(libs.mytracker.sdk)
//    implementation(libs.google.play.services.ads.identifier)

    // Зависимости для RuStore (через add)
    add("rustoreImplementation", libs.vk.ads.sdk)
    add("rustoreImplementation", libs.mytracker.sdk)
    add("rustoreImplementation", libs.google.play.services.ads.identifier)

    // Зависимости для Google Play (через add)
    add("googleImplementation", libs.vk.ads.sdk)
    add("googleImplementation", libs.mytracker.sdk)
    add("googleImplementation", libs.google.play.services.ads.identifier)

    // Для free ничего из этого не подключается!

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}