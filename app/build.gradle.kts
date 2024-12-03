
plugins {
    id("com.android.application")
    
}

android {
    namespace = "com.tcd.gamelauncher"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.tcd.gamelauncher"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        
        vectorDrawables { 
            useSupportLibrary = true
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildTypes {
      release{
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
    
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.appcompat:appcompat:1.6.1")
}
