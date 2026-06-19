# =============================================================================
# 图书馆智能管理系统 — ProGuard 混淆规则
# =============================================================================

# ---- 通用属性保留 ----
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ---- Retrofit + OkHttp ----
-keep class com.library.android.model.** { *; }
-keep class com.library.android.network.** { *; }
-keepclassmembernames,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**

# ---- Gson 序列化字段保留 ----
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation @interface com.google.gson.annotations.SerializedName

# ---- Glide ----
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl

# ---- Hilt / Dagger ----
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.AndroidEntryPoint <methods>;
}

# ---- RxJava3 ----
-dontwarn java.util.concurrent.Flow*
-keep class io.reactivex.rxjava3.** { *; }

# ---- Kotlin Metadata（Hilt/Coroutines 反射用，即便项目本身是 Java 也需保留） ----
-keep class kotlin.Metadata { *; }

# ---- MPAndroidChart 保留绘制类（被反射访问） ----
-keep class com.github.mikephil.charting.** { *; }

# ---- ZXing 条码扫描 ----
-keep class com.journeyapps.barcodescanner.** { *; }
-keep class com.google.zxing.** { *; }
