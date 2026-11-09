# Keep JNI native methods
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.antigravity.echolink.audio.** { *; }
-dontwarn org.bouncycastle.**
