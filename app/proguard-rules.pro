# ============================================================
# DNote — ProGuard / R8 Rules
# ============================================================

# --- Crash report: preserve file names & line numbers -------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Kotlin ----------------------------------------------------
# Kotlin metadata is needed for reflection-based libraries
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions

# Keep Kotlin companion objects and top-level functions used by R8
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontnote kotlin.**

# --- Kotlin Coroutines -----------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# --- Kotlin Serialization --------------------------------------
# Keep serializable classes and their generated serializers
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep app-specific @Serializable classes (used for Navigation3 routes)
-keep,includedescriptorclasses class id.project.df.dnote.**$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class id.project.df.dnote.** {
    *** Companion;
    static ** serializer();
}

# --- Room ------------------------------------------------------
# Room bundles its own consumer rules but we add explicit keeps
# to protect entities and DAOs from obfuscation
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# Protect app entity & DAO packages explicitly
-keep class id.project.df.dnote.feature.note.data.local.entity.** { *; }
-keep class id.project.df.dnote.feature.note.data.local.dao.** { *; }

# --- Hilt / Dagger ---------------------------------------------
# Hilt bundles its own consumer rules; nothing extra needed.
# Keep generated Hilt components to avoid build issues on edge cases.
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager { *; }
-dontwarn dagger.**
-dontnote dagger.**

# --- Domain / Data models --------------------------------------
# Keep domain models so they're not stripped by aggressive shrinking
-keep class id.project.df.dnote.feature.note.domain.model.** { *; }

# --- Coil ------------------------------------------------------
# Coil bundles its own consumer rules; OkHttp is handled below.
-dontwarn coil.**

# --- OkHttp ----------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# --- Jetpack Compose -------------------------------------------
# Compose tooling is already excluded via debugImplementation.
# No extra rules needed for release.

# --- Navigation3 (experimental) --------------------------------
# NavKey implementations need to survive shrinking/obfuscation
# because they are serialized as navigation destinations.
-keep class * implements androidx.navigation3.runtime.NavKey { *; }
