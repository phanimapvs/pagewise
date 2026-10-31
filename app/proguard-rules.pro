# Backup JSON models are serialized by name; keep their generated serializers.
-keepclassmembers @kotlinx.serialization.Serializable class com.pagewise.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.pagewise.app.**$$serializer { *; }

# WorkManager instantiates workers reflectively by class name.
-keep class com.pagewise.app.service.*Worker { <init>(...); }
