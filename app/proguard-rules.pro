-ignorewarnings
-optimizationpasses 10
-allowaccessmodification
#-keep public class de.** { !private *; }
-keep class android.** { !private *; }
-keepclassmembers class * {
    native <methods>;
}
-dontwarn **
-keepclasseswithmembers,allowshrinking class * {
    native <methods>;
}
-repackageclasses