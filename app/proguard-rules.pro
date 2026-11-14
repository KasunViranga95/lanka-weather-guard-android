# R8 rules for release builds (isMinifyEnabled = true in app/build.gradle.kts).
# Libraries such as Retrofit, OkHttp, WorkManager, Glance and Compose ship their own rules;
# these cover what R8 can't see on its own.

# --- JSON with Moshi's reflection adapter (KotlinJsonAdapterFactory) ---
# Moshi reads property names and constructors through Kotlin metadata at run time, so the JSON
# classes must keep their names and members. The offline cache on the phone is stored with these
# same names, so a release build can read what an earlier version saved.
-keep class kotlin.Metadata { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keep class com.example.network.** { *; }
-keep class com.example.model.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# --- Readable crash reports ---
# Keep line numbers so a stack trace from the Play Console points at the right line
# (upload the mapping.txt from app/build/outputs/mapping/release with each release).
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# --- OkHttp 4.10 ---
# OkHttp can use these TLS providers when an app bundles them; this app doesn't (Android's own TLS
# is used), so the references are safe to ignore. Newer OkHttp versions ship this rule themselves.
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# --- WorkManager's database (background alert checks) ---
# WorkManager opens its Room database by creating WorkDatabase_Impl through reflection; without
# this R8 strips the constructor and the app crashes at start-up.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
