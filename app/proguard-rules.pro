-dontwarn com.google.api.client.**
-dontwarn org.joda.time.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }

-keepattributes Signature,InnerClasses,Annotation,EnclosingMethod
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.** { *; }
-keep class com.example.webbox.WebSite { *; }
-keep class com.example.webbox.WebDavConfig { *; }

-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

-keepclassmembers class com.example.webbox.WebViewModel {
    public <init>(android.app.Application);
}
