plugins { id("com.android.application"); kotlin("android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
 namespace="dev.breda.tap"; compileSdk=35
 defaultConfig { applicationId="dev.breda.tap"; minSdk=26; targetSdk=35; versionCode=1; versionName="0.1.0" }
 buildFeatures { compose=true }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
}
kotlin { jvmToolchain(17) }
dependencies {
 implementation(project(":core")); implementation(project(":payment-sdk"))
 implementation(platform("androidx.compose:compose-bom:2025.04.01")); implementation("androidx.compose.material3:material3")
 implementation("androidx.activity:activity-compose:1.10.1"); implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1"); implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.1")
 implementation(platform("com.google.firebase:firebase-bom:33.12.0")); implementation("com.google.firebase:firebase-auth"); implementation("com.google.firebase:firebase-firestore")
}

dependencies {
 implementation("androidx.navigation:navigation-compose:2.8.9")
 implementation("com.airbnb.android:lottie-compose:6.6.6")
 implementation("androidx.compose.material:material-icons-extended")
 testImplementation("junit:junit:4.13.2")
}

dependencies { implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0") }
