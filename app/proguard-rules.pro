# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/jim/Library/Android/sdk/tools/proguard/proguard-android.txt
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
# Samsung ruining all nice things
-keep class !android.support.v7.view.menu.**, !android.support.design.internal.NavigationMenu, !android.support.design.internal.NavigationMenuPresenter, !android.support.design.internal.NavigationSubMenu, android.support.** {*;}

-keep class com.crashlytics.** { *; }
-keep class com.crashlytics.android.**
-keepattributes SourceFile,LineNumberTable
#-keepattributes *Annotation*

#admob
-keep public class com.google.android.gms.ads.** {
   public *;
}

-keep public class com.google.ads.** {
   public *;
}

#facebook ad
#-keep public class com.facebook.ads.** {
#   public *;
#}

-keep public class com.flurry.android.ads.** {
   public *;
}

-keep public class com.inmobi.ads.** {
   public *;
}

-dontwarn com.facebook.**

-dontwarn com.squareup.okhttp.**
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keepresourcexmlelements manifest/application/meta-data@value=GlideModule
-dontwarn okio.**
-dontnote org.xbill.DNS.spi.DNSJavaNameServiceDescriptor
-dontwarn org.xbill.DNS.spi.DNSJavaNameServiceDescriptor


-keep class yyf.shadowsocks.**{*;}
-keep class yyf.shadowsocks.service.**{*;}
-keep class yyf.shadowsocks.jni.**{*;}
-keep class yyf.shadowsocks.utils.**{*;}


#For design support library
-keep class android.support.design.widget.** { *; }
-keep interface android.support.design.widget.** { *; }

#glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keepresourcexmlelements manifest/application/meta-data@value=GlideModule

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod