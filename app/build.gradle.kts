// موديول التطبيق

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// ————————————————————————————— توقيع نسخة الإصدار (Release) —————————————————————————————
// المصدر بالترتيب: متغيرات البيئة (GitHub Secrets) ثم ملف keystore.properties في جذر المشروع.
// إن لم يتوفر أي منهما يُبنى الـ APK غير موقّع (لن يمنع ذلك بقية البناء).
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun signingValue(envName: String, propertyName: String): String? =
    System.getenv(envName)?.takeIf { it.isNotBlank() } ?: keystoreProperties.getProperty(propertyName)

val releaseStoreFile = System.getenv("RELEASE_STORE_FILE")?.takeIf { it.isNotBlank() }
    ?.let { rootProject.file(it) }
    ?: rootProject.file("release-keystore.p12")
val releaseStorePassword = signingValue("RELEASE_STORE_PASSWORD", "storePassword")
val releaseKeyAlias = signingValue("RELEASE_KEY_ALIAS", "keyAlias")
val releaseKeyPassword = signingValue("RELEASE_KEY_PASSWORD", "keyPassword") ?: releaseStorePassword
val canSignRelease = releaseStoreFile.exists() && releaseStorePassword != null && releaseKeyAlias != null

// وضع البناء السريع: ./gradlew :app:assembleRelease -Pmywifi.minify=false
// يتخطى R8 وتقليص الموارد (أسرع بمراحل، لكن ملف APK أكبر) — مفيد للبناءات التجريبية.
val minifyRequested = (findProperty("mywifi.minify") as String?)?.toBoolean() ?: true

android {
    namespace = "com.mywifiadata.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.mywifiadata.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"
    }

    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                storeType = "PKCS12"
            }
        } else {
            logger.lifecycle("تنبيه: لا توجد بيانات توقيع، سيُبنى APK الإصدار غير موقّع.")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = minifyRequested
            isShrinkResources = minifyRequested
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    // إنتاج APK مستقل لكل ABI شائع على أجهزة أندرويد، إضافة إلى APK شامل للجميع.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // فحص اللينت غير مطلوب في بيئة CI: تعطيله (خصوصًا lintVitalRelease الذي يعمل تلقائيًا
    // مع assembleRelease) يوفّر عشرات الثواني في كل بناء.
    lint {
        abortOnError = false
        checkReleaseBuilds = false
        checkDependencies = false
        disable += setOf("MissingTranslation", "ExtraTranslation")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.graphics.shapes)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
