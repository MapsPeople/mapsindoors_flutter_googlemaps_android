-dontwarn com.mapbox.maps.plugin.lifecycle.LifecycleUtils

-dontwarn org.xmlpull.v1.**
-dontwarn org.kxml2.io.**
-dontwarn android.content.res.**
-dontwarn org.slf4j.impl.StaticLoggerBinder

-keep class org.xmlpull.** { *; }
-keepclassmembers class org.xmlpull.** { *; }

-keep class com.mapsindoors.log.model.** { *; }
-keep class com.mapsindoors.log.enums.** { *; }