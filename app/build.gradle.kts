plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.eggyswarehouse.betterglucodash"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.eggyswarehouse.betterglucodash"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
        buildConfig = true
    }

    lint {
        abortOnError = true
        htmlReport = true
    }
}

kotlin {
    jvmToolchain(17)
}

ktlint {
    version.set("1.3.1")
    android.set(true)
    outputToConsole.set(true)
}

// ktlint-gradle 12.x + AGP 9 cannot auto-discover Kotlin source sets (AGP 9 applies
// kotlin-android internally without registering it in the plugin container). This task
// invokes ktlint-cli directly so that .kt sources are checked, not just .kts scripts.
val ktlintCli by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    ktlintCli("com.pinterest.ktlint:ktlint-cli:1.3.1")
}

tasks.register<JavaExec>("ktlintSourceCheck") {
    group = "verification"
    description = "Check Kotlin source files (.kt) with ktlint"
    classpath = ktlintCli
    mainClass.set("com.pinterest.ktlint.Main")
    args("src/**/*.kt", "--reporter=plain")
}

tasks.register<JavaExec>("ktlintSourceFormat") {
    group = "formatting"
    description = "Format Kotlin source files (.kt) with ktlint"
    classpath = ktlintCli
    mainClass.set("com.pinterest.ktlint.Main")
    args("-F", "src/**/*.kt", "--reporter=plain")
}

tasks.named("ktlintCheck") { dependsOn("ktlintSourceCheck") }
tasks.named("ktlintFormat") { dependsOn("ktlintSourceFormat") }

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    baseline = file("$rootDir/config/detekt/baseline.xml")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.text.google.fonts)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Network: Retrofit + OkHttp + Kotlinx Serialization
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp.logging)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.datastore.preferences)

    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
