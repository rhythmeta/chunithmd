# Ktor and Kotlin Multiplatform use Kotlin file classes during client setup.
# Keep their JVM names and inheritance relationships stable for R8 output.
-keep class kotlin.** { *; }
-keep class io.ktor.** { *; }
-dontwarn java.lang.management.**

# ONNX Runtime exposes Java classes through JNI.
-keep class ai.onnxruntime.** { *; }

# MIUIX 0.9.4 was built against material-color-utilities 5.0.1, where DynamicScheme lived in
# com.materialkolor.scheme. materialkolor 6.0.0 moved it to com.materialkolor.dynamiccolor, so
# MIUIX's own palette generator (ThemeControllerKt.colorsFromSeed) no longer links against the
# version this app builds with. The app deliberately never uses that generator: ChunithmdTheme
# creates every Colors instance itself and only ever passes MIUIX's plain (non-Monet) color scheme
# modes plus explicit colors, so the stale references are unreachable at runtime. Silence R8 so the
# missing classes cannot fail the release build; drop this once MIUIX ships a release built against
# material-color-utilities 6.x (upstream main already migrated).
-dontwarn com.materialkolor.scheme.DynamicScheme**
