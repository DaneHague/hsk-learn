# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.hsklearn.app.data.model.** { *; }

# kotlinx.serialization
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keepclassmembers @kotlinx.serialization.Serializable class com.hsklearn.app.data.model.** {
    *** Companion;
    *** $serializer;
    <fields>;
}
