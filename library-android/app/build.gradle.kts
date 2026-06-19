// =============================================================================
// 图书馆智能管理系统 — Android 应用模块构建配置
// =============================================================================
import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.library.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.library.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 后端 API 基地址（优先从 local.properties 读取，不入 git；否则使用生产环境默认值）
        // 默认值：生产服务器（http://101.132.24.73:8080/api/v1/）
        // 本地开发：在 local.properties 中设置 api.base.url=http://10.0.2.2:8080/api/v1/ 覆盖（emulator 映射本机）
        val localProps = Properties()
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) localProps.load(localFile.inputStream())
        val baseUrl = localProps.getProperty("api.base.url", "http://101.132.24.73:8080/api/v1/")
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
        // Mock 模式（默认 false 连真实后端；设为 true 使用本地模拟数据）
        val mockEnabled = localProps.getProperty("mock.enabled", "false")
        buildConfigField("boolean", "MOCK_ENABLED", mockEnabled)
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // ---- AndroidX 核心 ----
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // ---- 导航 ----
    implementation("androidx.navigation:navigation-fragment:2.7.6")
    implementation("androidx.navigation:navigation-ui:2.7.6")

    // ---- 下拉刷新 ----
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // ---- 生命周期 & ViewModel ----
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")

    // ---- 网络请求: Retrofit + OkHttp ----
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:adapter-rxjava3:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ---- JSON 解析 ----
    implementation("com.google.code.gson:gson:2.10.1")

    // ---- 图片加载: Glide ----
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // ---- 依赖注入: Hilt ----
    implementation("com.google.dagger:hilt-android:2.50")
    annotationProcessor("com.google.dagger:hilt-compiler:2.50")

    // ---- 安全加密: EncryptedSharedPreferences ----
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ---- 异步: RxJava 3 ----
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")

    // ---- 图表: MPAndroidChart ----
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // ---- 条码扫描: ZXing ----
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // ---- 测试 ----
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}