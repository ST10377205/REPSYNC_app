plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    buildFeatures { buildConfig = true }
    namespace = "com.example.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        val uploadStore = providers.environmentVariable("REPSYNC_UPLOAD_STORE").orNull
        if (uploadStore != null) {
            create("upload") {
                storeFile = file(uploadStore)
                storePassword = providers.environmentVariable("REPSYNC_UPLOAD_PASSWORD").orNull
                keyAlias = "repsync-upload"
                keyPassword = providers.environmentVariable("REPSYNC_UPLOAD_PASSWORD").orNull
            }
        }
    }
    buildTypes {
        release {
            isDebuggable = false
            signingConfig = signingConfigs.findByName("upload")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

// A release must never silently fall back to an unsigned artifact or the debug key.
tasks.matching { it.name in listOf("packageRelease", "signReleaseBundle") }.configureEach {
    doFirst {
        check(!System.getenv("REPSYNC_UPLOAD_STORE").isNullOrBlank() &&
            !System.getenv("REPSYNC_UPLOAD_PASSWORD").isNullOrBlank()) {
            "Use tools/Build-Release.ps1 or provide REPSYNC_UPLOAD_STORE and REPSYNC_UPLOAD_PASSWORD."
        }
    }
}

dependencies {
    // Pinned to the existing Kotlin/Android toolchain; use the main Firebase module.
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Retrofit for REST API
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)

    // Password Encryption
    implementation(libs.jbcrypt)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
