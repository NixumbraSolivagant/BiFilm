-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep AGSL shader raw resources
-keep class com.bifilm.app.R$* { *; }

# Keep CameraX
-keep class androidx.camera.** { *; }
