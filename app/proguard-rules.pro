# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line numbers for readable stack traces (Crashlytics / logcat).
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,InnerClasses,Signature,Exceptions,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Optional desktop/server integrations referenced by Apache POI, XMLBeans,
# Log4j, and PDFBox but not packaged or used on Android.
-dontwarn aQute.bnd.annotation.spi.ServiceConsumer
-dontwarn aQute.bnd.annotation.spi.ServiceProvider
-dontwarn com.gemalto.jp2.JP2Decoder
-dontwarn java.awt.**
-dontwarn javax.xml.stream.**
-dontwarn net.sf.saxon.**
-dontwarn org.apache.batik.**
-dontwarn org.osgi.framework.**

# ---------------------------------------------------------------------------
# kotlinx.serialization — R8 strips the generated $serializer companions
# without these, breaking decodeSingle() in release builds.
# ---------------------------------------------------------------------------

# kotlinx.serialization JSON plugin support (Companion.serializer() lookup).
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep generated serializers for every @Serializable type in the app.
-keep,includedescriptorclasses class com.aplicator.jobapplier.**$$serializer { *; }
-keepclassmembers class com.aplicator.jobapplier.** {
    *** Companion;
}
-keepclasseswithmembers class com.aplicator.jobapplier.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------------------------------------------------------------------------
# Supabase Kotlin SDK (kotlinx.serialization based) — keep DTOs + serializers.
# ---------------------------------------------------------------------------
-keep @kotlinx.serialization.Serializable class ** { *; }
-keep class io.github.jan.supabase.** { *; }
-keep class io.github.jan.supabase.**$* { *; }

# ---------------------------------------------------------------------------
# Ktor (Supabase HTTP engine).
# ---------------------------------------------------------------------------
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ---------------------------------------------------------------------------
# Hilt / Dagger (plugin injects its own rules; keep ViewModel factories safe).
# ---------------------------------------------------------------------------
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# ---------------------------------------------------------------------------
# Mixpanel
# ---------------------------------------------------------------------------
-keep class com.mixpanel.android.** { *; }
-dontwarn com.mixpanel.android.**
