plugins {
    // Gunakan alias dari libs.versions.toml jika sudah terdefinisi di sana
    id("com.android.application") // Atau alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.dailytaskmanager"
    compileSdk = 35 // Pastikan SDK 35 terinstal atau sesuaikan ke versi yang stabil (misal 34)

    defaultConfig {
        applicationId = "com.example.dailytaskmanager"
        minSdk = 26
        targetSdk = 35 // Sesuaikan dengan compileSdk
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true // Diperlukan untuk Vector Drawables di SDK < 21 (walaupun minSdk 26)
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
        // Target Java 11 sudah bagus, tapi banyak library Android modern menargetkan 1.8 atau 17
        // Anda bisa biarkan 11 jika tidak ada masalah, atau coba ubah ke JavaVersion.VERSION_1_8 atau JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11" // Sesuaikan dengan compileOptions targetCompatibility
    }
    buildFeatures {
        compose = true // Ini mengaktifkan Compose
    }
    composeOptions {
        // Tentukan versi Kotlin Compiler Extension yang kompatibel dengan versi Compose Anda
        // Periksa tabel kompatibilitas di: https://developer.android.com/jetpack/androidx/releases/compose-kotlin
        // Contoh: Jika Anda menggunakan Compose BOM 2024.02.02, Anda mungkin memerlukan 1.5.10 atau lebih baru
        kotlinCompilerExtensionVersion = "1.5.10" // Ganti dengan versi yang sesuai
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}" // Umum untuk proyek Compose
        }
    }
}

dependencies {

    // Gunakan alias dari libs.versions.toml jika tersedia dan konsisten
    // Jika tidak menggunakan libs, ganti dengan string literal dan versi
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Import Compose BOM (Hanya sekali per konfigurasi: implementation, androidTestImplementation)
    implementation(platform(libs.androidx.compose.bom)) // <-- Cara yang benar menggunakan libs
    // implementation(platform("androidx.compose:compose-bom:2024.02.02")) // <-- Alternatif jika tidak pakai libs

    // Dependensi Compose (Versi akan diambil dari BOM)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // implementation("androidx.compose.ui:ui") <-- Alternatif jika tidak pakai libs
    // implementation("androidx.compose.ui:ui-graphics")
    // implementation("androidx.compose.ui:ui-tooling-preview")
    // implementation("androidx.compose.material3:material3")

    // ViewModel Compose Integration
    // Cek apakah ada alias di libs.versions.toml (misal: libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Jika tidak ada alias:
    // val lifecycleVersion = "2.7.0" // Definisikan di atas dependencies atau langsung
    // implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")


    // Icons (Extended) - Biasanya tidak ada di BOM default, perlu versi eksplisit atau alias libs
    implementation(libs.androidx.material.icons.extended) // <-- Jika ada alias di libs
    // implementation("androidx.compose.material:material-icons-extended:1.6.2") // <-- Ganti versi jika perlu

    // Lifecycle Runtime Compose (mungkin sudah tercakup oleh runtime-ktx atau activity-compose, tapi bisa ditambahkan jika perlu)
    // Cek alias di libs.versions.toml (misal: libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // Jika tidak ada alias:
    // implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion") // Gunakan lifecycleVersion yg sama


    // Testing Dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Import Compose BOM untuk Android Test (Hanya sekali)
    androidTestImplementation(platform(libs.androidx.compose.bom)) // <-- Cara yang benar

    // Compose UI Testing (Versi diambil dari BOM)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    // androidTestImplementation("androidx.compose.ui:ui-test-junit4") // <-- Alternatif jika tidak pakai libs

    // Debugging Dependencies (Versi diambil dari BOM)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    // debugImplementation("androidx.compose.ui:ui-tooling") // <-- Alternatif jika tidak pakai libs
    // debugImplementation("androidx.compose.ui:ui-test-manifest")



}