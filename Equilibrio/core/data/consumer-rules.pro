# SQLCipher mantém o esquema e as consultas ofuscados no artefato.
-keep class net.zetetic.database.** { *; }
-keep class net.sqlcipher.** { *; }

# TensorFlow Lite (opcional)
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keepclassmembers class com.equilibrio.core.data.mapper.** { *; }
