# Preserve stack trace line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class androidx.room.** { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static <fields>;
    public abstract <methods>;
}

# ── Kotlin / Coroutines ───────────────────────────────────────────────────────
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keep class kotlin.coroutines.Continuation

# ── Compose ───────────────────────────────────────────────────────────────────
# R8 ships with built-in Compose rules since AGP 7.x; these cover edge cases.
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ── App model classes used by Room ───────────────────────────────────────────
-keep class app.krafted.fruitslicer.data.** { *; }

# ── Enum classes (FruitType etc.) ─────────────────────────────────────────────
-keepclassmembers enum app.krafted.fruitslicer.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
