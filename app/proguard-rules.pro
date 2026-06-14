# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# ─── Stack traces legíveis ───────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ─── Kotlin Metadata (necessário para reflection interna do Kotlin) ───────────
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**

# ─── Room — apenas o necessário ──────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.**

# ─── App — Entities e Models (Room usa reflection nestes) ────────────────────
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class pt.ipt.dama.muscleup.data.local.** {
    <init>(...);
    <fields>;
}
-keepclassmembers class pt.ipt.dama.muscleup.model.** {
    <init>(...);
    <fields>;
}

# ─── Coil ────────────────────────────────────────────────────────────────────
-dontwarn coil.**

# ─── Navigation e Lifecycle ──────────────────────────────────────────────────
-dontwarn androidx.navigation.**
-dontwarn androidx.lifecycle.**
-dontwarn androidx.compose.**
