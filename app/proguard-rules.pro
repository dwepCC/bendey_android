# Add project specific ProGuard rules here.

# isMinifyEnabled pasó a true el 2026-09-13 (Play Console reportaba "Optimización: Baja",
# ofuscación 1% — nunca se había activado R8 en este proyecto, ver build.gradle.kts). Retrofit,
# OkHttp, Hilt y Compose traen sus propias consumer-rules en el AAR, así que no hace falta nada
# especial para ellos. Lo de acá abajo es SOLO para lo que esos consumer-rules no cubren.

# kotlinx.serialization: reglas oficiales recomendadas por el proyecto (github.com/Kotlin/
# kotlinx.serialization/blob/master/rules/common.pro) — sin esto R8 puede eliminar el
# companion/serializer generado en modo full, rompiendo la (de)serialización JSON en runtime sin
# avisar en tiempo de compilación (no lanza excepción de R8, el modelo simplemente llega vacío).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    *** Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <1>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.bendey.restaurant.**$$serializer { *; }
-keepclassmembers class com.bendey.restaurant.** {
    *** Companion;
}
-keepclasseswithmembers class com.bendey.restaurant.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# fastexcel-reader (org.dhatim) trae aalto-xml + stax2-api, que asumen el StAX del JDK
# (javax.xml.stream.*) — Android nunca lo incluyó, ni siquiera hay un stax-api de respaldo entre
# las dependencias. R8 lo detecta como "missing class" al analizar el programa completo; sin esta
# regla, el build de release falla directamente (missing_rules.txt). No es un problema de R8: si
# el código que usa esas clases realmente se ejecuta en el importador de Excel (core/data/imports/
# RestaurantProductExcelImporter.kt), va a fallar en runtime con NoClassDefFoundError igual con
# minify apagado — hay que probar esa pantalla en un dispositivo real antes de confiar en que
# funciona (importar/exportar catálogo de productos por Excel).
-dontwarn javax.xml.stream.**
-dontwarn org.codehaus.stax2.**
-dontwarn com.fasterxml.aalto.**
