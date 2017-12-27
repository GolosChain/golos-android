# Add project specific ProGuard rules here.для до андроид 6,- прямоуглольная
# By default, the flags in this file are appended to flags specified
# in /Users/yuri/Library/Android/sdk/tools/proguard/proguard-android.txt
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
#  public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.MapActivity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep,includedescriptorclasses class eu.bittrade.libs.steemj.communication** { *; }
-dontoptimize
-keepparameternames
-keepclassmembers,allowoptimization enum * {
public static **[] values();
public static ** valueOf(java.lang.String);
}
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

-keepattributes *Annotation*


#-keep class org.jetbrains.** { *; }
-keep,includedescriptorclasses class javax.lang.model.type.** { *; }
-keep,includedescriptorclasses class io.golos.** { *; }
-keep,includedescriptorclasses class eu.bittrade.libs.steemj.apis.** { *; }
-keep,includedescriptorclasses class eu.bittrade.libs.steemj.base.models.** { *; }
-keep,includedescriptorclasses class kotlin.reflect.jvm.** { *; }

-keep,includedescriptorclasses class ru.noties.markwon.** { *; }

-keep,includedescriptorclasses class org.commonmark.** { *; }
-keepclassmembers class org.commonmark.internal.util.Html5Entities {
    <fields>;
}

-keep,includedescriptorclasses class java.beans.** { *; }

# support design
-dontwarn android.support.design.**
-keep class android.support.design.** { *; }
-keep interface android.support.design.** { *; }
-keep public class android.support.design.R$* { *; }

-keep class com.fasterxml.jackson.annotation.** { *; }
-keepattributes Signature,InnerClasses,*Annotation*
-keep,includedescriptorclasses  class org.glassfish.tyrus.** {  *; }
-keep,includedescriptorclasses  class org.glassfish.grizzly.** { *; }

-keep,includedescriptorclasses  class android.arch.lifecycle.** { *; }
-keep,includedescriptorclasses  class com.caverock.androidsvg.** { *; }
-keep,includedescriptorclasses  class javax.websocket.** { *; }

-keeppackagenames org.jsoup.nodes
-keep,includedescriptorclasses  class android.support.v7.util.** { *; }

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-dontwarn org.glassfish.**
-dontwarn com.caverock.androidsvg**
-dontwarn ru.noties.markwon**
-dontwarn org.bitcoinj.**
-dontwarn com.google.**
-dontwarn org.slf4j.**
-dontwarn okio.**
-dontwarn com.squareup**
-dontwarn com.fasterxml.jackson.databind.**
-dontwarn android.arch.util.paging.CountedDataSource
-dontwarn android.arch.persistence.room.paging.LimitOffsetDataSource
-dontwarn android.arch.persistence**
-dontwarn org.abego.treelayout**
-dontwarn org.antlr.v4.gui**
-dontwarn org.stringtemplate.v4.gui.**
-dontwarn Type_mirror_extKt*
-dontwarn org.antlr.runtime.tree.**
-dontwarn kotlin.reflect.jvm.**
-dontwarn org.htmlcreaner.**
