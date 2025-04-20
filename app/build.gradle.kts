plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {

    namespace = "com.example.newearningapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.newearningapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_11
//        targetCompatibility = JavaVersion.VERSION_11
//    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Firebase BoM (Bill of Materials)
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.android.gms:play-services-ads:24.1.0")

    // Firebase dependencies
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation ("com.karumi:dexter:6.2.3")
    // CircleImageView
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    // AndroidX dependencies
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation ("androidx.core:core-ktx:1.12.0")
    implementation ("androidx.legacy:legacy-support-v4:1.0.0")

    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("com.facebook.android:audience-network-sdk:6.+")
    // Navigation components
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation(libs.activity)
    implementation(libs.firebase.storage)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation ("cn.pedant.sweetalert:library:1.3")
    implementation(libs.play.services.ads)
    implementation ("androidx.annotation:annotation:1.0.0")
    implementation ("com.facebook.android:audience-network-sdk:6.+")

    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}