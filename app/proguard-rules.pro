# Add project specific ProGuard rules here.

# ── Stack traces ──────────────────────────────────────────────────────────────
# Preserve line numbers in crash stack traces for easier debugging.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Retrofit ──────────────────────────────────────────────────────────────────
# Keep Retrofit @HTTP annotation methods (GET, POST, etc.) from being stripped.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ── OkHttp / Okio ─────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# ── Kotlinx Serialization ─────────────────────────────────────────────────────
# The kotlinx.serialization gradle plugin auto-generates keep rules for all
# @Serializable classes at compile time — no manual rules needed here.

# ── Kotlin ────────────────────────────────────────────────────────────────────
-dontwarn kotlin.**
-keepattributes Signature
-keepattributes *Annotation*