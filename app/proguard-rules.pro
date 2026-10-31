# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep data models used in JSON serialization/deserialization
-keep class com.example.model.** { *; }
-keepclassmembers class com.example.model.** { *; }

# Keep MySQL / MariaDB JDBC Driver and Reflection classes
-keep class com.mysql.jdbc.** { *; }
-keep class com.mysql.cj.** { *; }
-keep class org.gjt.mm.mysql.** { *; }
-dontwarn java.sql.**
-dontwarn javax.sql.**
-dontwarn com.mysql.**
-keep interface java.sql.** { *; }
-keep interface javax.sql.** { *; }

# Keep Firebase Auth, Firestore, and App Check classes
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep Jetpack Credential Manager and Google ID
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn androidx.credentials.**

# Keep Coroutines and Coil
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn coil.**

