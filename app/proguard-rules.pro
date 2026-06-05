-keepattributes Signature
-keepattributes *Annotation*
-keep class com.banyadm.islam.data.** { *; }
-keep class retrofit2.** { *; }
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}
