# Release builds are shrunk and obfuscated by R8. Retrofit, OkHttp, kotlinx.serialization, Coil and
# Compose ship their own consumer rules; the rules below cover what this app adds on top.

# kotlinx.serialization: keep the generated serializers of the app's @Serializable models
-keepclassmembers @kotlinx.serialization.Serializable class com.prijilevschi.library.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class com.prijilevschi.library.**
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit reads the generic signatures of the API interface (suspend functions return types)
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowobfuscation interface com.prijilevschi.library.data.LibraryApi
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
