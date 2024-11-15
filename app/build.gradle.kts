plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.moodly"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.moodly"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true  // View Binding 활성화
        compose = true      // Compose 활성화
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1" // Compose 버전에 맞게 설정
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Compose 라이브러리 추가
    implementation("androidx.activity:activity-compose:1.5.1")
    implementation("androidx.compose.ui:ui:1.2.0")
    implementation("androidx.compose.material:material:1.2.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.2.0")
    implementation("androidx.navigation:navigation-compose:2.5.1")

    // 테스트 라이브러리
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
