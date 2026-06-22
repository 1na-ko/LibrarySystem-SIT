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
    compileSdk = 35

    defaultConfig {
        applicationId = "com.library.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 后端 API 基地址（优先从 local.properties 读取，不入 git；否则使用生产环境默认值）
        // ────────────────────────────────────────────────────────────────────
        // P0-02 HTTP→HTTPS 半改造（2026-06-19）：
        //   ▸ api.use.https=true 时默认走 HTTPS（端口 8443）；待后端证书就绪后开启
        //   ▸ api.use.https=false（默认） 时仍走 HTTP（端口 8080），与现网部署一致
        //   ▸ 也可显式 api.base.url=... 完全覆盖默认 BASE_URL
        //   ▸ Release 构建会触发 verifyReleaseHttps task 强制要求 HTTPS（防回退）
        // 本地开发：在 local.properties 中设置 api.base.url=http://10.0.2.2:8080/api/v1/ 覆盖
        // ────────────────────────────────────────────────────────────────────
        val localProps = Properties()
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) localProps.load(localFile.inputStream())

        val useHttps = localProps.getProperty("api.use.https", "false").toBoolean()
        val defaultBaseUrl = if (useHttps) {
            "https://101.132.24.73:8443/api/v1/"
        } else {
            "http://101.132.24.73:8080/api/v1/"
        }
        val baseUrl = localProps.getProperty("api.base.url", defaultBaseUrl)
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
        buildConfigField("boolean", "USE_HTTPS", useHttps.toString())

        // Mock 模式（默认 false 连真实后端；设为 true 使用本地模拟数据；P0-05 仅 Debug 构建生效）
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

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // ---- AndroidX 核心 ----
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
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
    // Mockito — P0-01 TokenManager 降级单元测试 / P2-04 ViewModel & Repository 测试
    testImplementation("org.mockito:mockito-core:5.10.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    // P2-04：LiveData 同步执行（InstantTaskExecutorRule）+ Repository 网络层 Mock
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

// =============================================================================
// P0-02 CI 协议检查 — Release 构建强制 HTTPS
// =============================================================================
//   ▸ 在 assembleRelease / bundleRelease 之前自动校验 BASE_URL 协议
//   ▸ 一旦有人误把 release 包指向 http:// 会立即 fail，避免明文流量回潮
//   ▸ Debug 构建不受约束（保留本地真机/模拟器调试便利）
val verifyReleaseHttps by tasks.registering {
    group = "verification"
    description = "Ensure release BASE_URL uses HTTPS (P0-02 防 HTTP 回退)"
    doLast {
        val localProps = Properties()
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) localProps.load(localFile.inputStream())

        val useHttps = localProps.getProperty("api.use.https", "false").toBoolean()
        val defaultBaseUrl = if (useHttps) {
            "https://101.132.24.73:8443/api/v1/"
        } else {
            "http://101.132.24.73:8080/api/v1/"
        }
        val baseUrl = localProps.getProperty("api.base.url", defaultBaseUrl)

        if (!baseUrl.startsWith("https://")) {
            throw GradleException(
                "Release 构建要求 HTTPS BASE_URL，当前为：$baseUrl\n" +
                "请在 local.properties 中设置 api.use.https=true（或显式指定 https:// 前缀的 api.base.url），" +
                "或先完成生产环境 HTTPS 证书部署后再构建 Release。"
            )
        }
        println("✓ verifyReleaseHttps 通过：BASE_URL=$baseUrl")
    }
}

tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
    dependsOn(verifyReleaseHttps)
}