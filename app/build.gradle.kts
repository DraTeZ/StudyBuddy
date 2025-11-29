
import java.util.Properties

plugins {
    alias(libs.plugins.android.application) version "8.4.1"
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

// 2. LÓGICA DE CARGA: Define e inicializa el objeto Properties en el scope global
val properties = Properties()
val propertiesFile = project.rootProject.file("secrets.properties")

if (propertiesFile.exists()) {
    properties.load(propertiesFile.inputStream())
} else {
    // Es buena práctica añadir un mensaje de advertencia si no se encuentra
    println("ADVERTENCIA: El archivo secrets.properties no fue encontrado en la raíz del proyecto.")
}


android {
    namespace = "com.example.studybuddy"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.studybuddy"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 3. USO: Ahora la variable 'properties' está cargada y lista.
        buildConfigField(
            type = "String",
            name = "GEMINI_API_KEY",
            value = properties.getProperty("GEMINI_API_KEY", "")
        )
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}


dependencies {
    // 1. Definimos la "lista de materiales" (BOM) para que gestione las versiones de Compose
    val composeBomVersion = "2024.04.01"
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    androidTestImplementation(platform("androidx.compose:compose-bom:$composeBomVersion"))

    // 2. Dependencias principales con versiones manuales y estables
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")

    // 3. Dependencias de Lifecycle (controlamos la versión para evitar errores)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // 4. Dependencias de Compose (SIN versión, la BOM se encarga)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended") // Versión controlada por la BOM

    // 5. Dependencias de Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    //implementation(libs.firebase.ai)

    // Si estás usando la clave API en lugar de Firebase AI,
    // asegúrate de añadir la dependencia del SDK de Google AI
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    implementation("com.google.android.gms:play-services-ads:23.0.0")

    // 6. Dependencias para Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.foundation:foundation")


}