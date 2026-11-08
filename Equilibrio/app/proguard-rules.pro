# Equilíbrio — regras de ofuscação

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclasseswithmembernames class * { @javax.inject.Inject <fields>; }
-keepclasseswithmembernames class * { @javax.inject.Inject <init>(...); }

# Room / SQLCipher
-keep class androidx.room.** { *; }
-keep class net.zetetic.database.** { *; }
-keep class net.sqlcipher.** { *; }
-dontwarn net.zetetic.database.**

# Kotlin serialization (DTOs do Gemini e Codec)
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.**
-keepclassmembers class com.equilibrio.core.data.mapper.** { *; }
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# TensorFlow Lite (modelo opcional em assets)
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# Google AI SDK
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# WorkManager + HiltWorker
-keep class * extends androidx.work.ListenableWorker { *; }
-keep @androidx.hilt.work.HiltWorker class * { *; }

# Reflection de enums usados em nomes persistidos
-keepclassmembers enum com.equilibrio.core.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Não preservar logs/linhas desnecessárias
-renamesourcefileattribute SourceFile
