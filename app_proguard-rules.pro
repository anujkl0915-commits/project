# ReKaro ProGuard Rules

# Keep ML Kit models
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep CameraX
-keep class androidx.camera.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep data classes
-keep class com.rekaro.app.model.** { *; }