# Pinchazos

App Android sencilla para registrar los pinchazos de insulina: cuándo y cuánta dosis. Todo en local en el móvil, sin cuentas ni Internet.

- **Registrar**: último pinchazo (verde si fue hace menos de 3 h), total de hoy, selector de dosis de 0 a 12 u en pasos de 0,5 y hora ("Ahora" o cambiada).
- **Informes**: periodo (Hoy / 7 días / 30 días / Todo), totales y media diaria, reparto por momento del día, gráfico "Cuándo y cuánto" e historial por días para corregir o borrar registros.
- **Datos**: exportar e importar CSV, y copia automática de Android (Auto Backup).

Kotlin + Jetpack Compose (Material 3), Room, DataStore. minSdk 26.

## Compilar

Necesita un JDK 17+ (sirve el que trae el SDK de Android):

```bash
export JAVA_HOME="/c/Program Files/Android/openjdk/jdk-21.0.8"
./gradlew assembleDebug        # APK en app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # tests unitarios
```

`local.properties` (no versionado) debe apuntar al SDK: `sdk.dir=C\:/Users/<usuario>/AppData/Local/Android/Sdk`.

## Instalar en el móvil

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

O copiar el `.apk` al móvil, abrirlo y permitir "Instalar apps desconocidas".
