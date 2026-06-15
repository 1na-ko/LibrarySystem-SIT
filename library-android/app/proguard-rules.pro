# =============================================================================
# 图书馆智能管理系统 — ProGuard 混淆规则
# =============================================================================

# ---- Retrofit ----
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.library.android.model.** { *; }
-keep class com.library.android.network.** { *; }

# ---- Gson ----
-keep class com.google.gson.** { *; }
-keepattributes EnclosingMethod

# ---- Glide ----
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl

# ---- Room ----
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
