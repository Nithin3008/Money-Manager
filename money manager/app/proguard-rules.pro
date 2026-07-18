# Gson serializes the backup payload and Room entities via reflection; R8 must not
# strip or rename their fields or export/import would silently corrupt.
-keep class com.moneymanager.app.model.AppBackupData { *; }
-keep class com.moneymanager.app.data.** { *; }
-keepattributes Signature, *Annotation*

# Gson TypeToken generic signatures.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
