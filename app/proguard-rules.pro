# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.leanote.next.** {
    *** Companion;
}
-keepclasseswithmembers class com.leanote.next.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.leanote.next.data.remote.dto.**$$serializer { *; }
-keepclassmembers class com.leanote.next.data.remote.dto.** { *; }

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature, Exceptions

# Room
-keep class * extends androidx.room.RoomDatabase

# Markwon 可选解码器（SVG / GIF 未引入依赖，仅告警忽略）
-dontwarn com.caverock.androidsvg.**
-dontwarn pl.droidsonroids.gif.**
-dontwarn io.noties.markwon.image.svg.**
-dontwarn io.noties.markwon.image.gif.**
