# Ktor and Kotlin Multiplatform use Kotlin file classes during client setup.
# Keep their JVM names and inheritance relationships stable for R8 output.
-keep class kotlin.** { *; }
-keep class io.ktor.** { *; }
-dontwarn java.lang.management.**

# ONNX Runtime exposes Java classes through JNI.
-keep class ai.onnxruntime.** { *; }
