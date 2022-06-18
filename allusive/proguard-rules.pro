# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in G:\AS_SDK\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
#noinspection ShrinkerUnresolvedReference
-keepattributes Signature
-keepclassmembers class com.afterroot.allusive2.model.** { *; }
-keepclassmembers class com.afollestad.materialdialogs.** { *; }
-keep class com.afterroot.** { *; }
-keep class androidx.navigation.fragment.NavHostFragment

# We only need to keep ComposeView
-keep public class androidx.compose.ui.platform.ComposeView {
    public <init>(android.content.Context, android.util.AttributeSet);
}

# For enumeration classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
