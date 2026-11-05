import java.io.StringReader
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)

    // Google services Gradle plugin (reads google-services.json)
    id("com.google.gms.google-services")
}

// ---- Read the TMDB key from local.properties (never committed) or from an
// ---- environment variable (used by GitHub Actions) ----
val localText: String = providers.fileContents(
    rootProject.layout.projectDirectory.file("local.properties")
).asText.orElse("").get()
val localProps = Properties().apply { load(StringReader(localText)) }
val tmdbKey: String = localProps.getProperty("TMDB_API_KEY")
    ?: providers.environmentVariable("TMDB_API_KEY").orNull
    ?: ""

android {
    namespace = "com.example.cinepulse"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.cinepulse"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Exposed in code as BuildConfig.TMDB_API_KEY
        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbKey\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    buildFeatures {
        viewBinding = true   // type-safe access to XML views
        buildConfig = true   // needed for BuildConfig.TMDB_API_KEY
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    // ---- Firebase (BoM keeps all Firebase versions in sync) ----
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")        // register / login
    implementation("com.google.firebase:firebase-firestore-ktx")   // NoSQL database
    implementation("com.google.firebase:firebase-messaging-ktx")   // push notifications
    implementation("com.google.android.gms:play-services-auth:20.7.0") // Google SSO

    // ---- Networking + images ----
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // ---- UI ----
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // ---- Tests ----
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}