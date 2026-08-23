-dontwarn de.robv.android.xposed.**
-dontwarn org.mozilla.javascript.**

-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.**
-dontwarn androidx.window.reflection.**

-dontwarn javax.management.**
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn java.rmi.**
-dontwarn javax.security.auth.**
-dontwarn org.apache.tomcat.jni.**
-dontwarn org.ietf.jgss.**

-keep enum * { *; }

-keep class com.android.tools.smali.dexlib2.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class androidx.compose.material.icons.** { *; }
-keep class androidx.compose.material3.R$* { *; }
-keep class androidx.compose.ui.R$* { *; }
-keep class androidx.navigation.** { *; }
-keep class me.rhunk.snapenhance.** { *; }
-keep class androidx.core.content.res.ResourcesCompat { *; }

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}