# Hilt
-keep public class * extends android.app.Service
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.view.View
-keep class com.google.dagger.hilt.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, EnclosingMethod, Signature
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep class kotlinx.serialization.** { *; }

# Keep your DTOs (Data Transfer Objects)
-keep class pinak.sppunotify.data.remote.** { *; }
-keep class pinak.sppunotify.data.local.** { *; }
