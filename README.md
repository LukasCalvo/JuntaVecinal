# JuntaVecinal

Aplicación Android para gestionar reservas de espacios de una junta vecinal. Permite registrar vecinos, iniciar sesión y crear/consultar reservas desde una interfaz desarrollada con Jetpack Compose.

> Estado: proyecto en desarrollo académico. La compilación no pudo verificarse en este entorno porque Gradle necesita descargar su distribución y el entorno de revisión no tiene acceso a `services.gradle.org`.

## Funcionalidades

- Registro de usuarios con nombre, RUT chileno, teléfono, región, comuna y contraseña.
- Validación y formato de RUT chileno.
- Inicio de sesión y manejo de sesión local.
- Registro y consulta de reservas.
- Validación de fechas, horas, cantidad de asistentes y datos del formulario.
- Prevención de reservas con horarios superpuestos para una misma fecha.
- Persistencia local con Room (SQLite).
- Notificaciones locales de reservas, sujetas al permiso de notificaciones de Android.

## Tecnologías

- Kotlin
- Android SDK
- Jetpack Compose y Material 3
- AndroidX ViewModel y Coroutines/Flow
- Room
- Gradle Kotlin DSL y Version Catalog (`gradle/libs.versions.toml`)
- KSP para generar el código de Room

## Requisitos

- Android Studio compatible con las versiones de Android Gradle Plugin y Gradle definidas en el proyecto.
- JDK compatible con la versión de Gradle configurada (revisa la configuración de Gradle JDK de Android Studio).
- Android SDK instalado, incluyendo la plataforma `compileSdk` configurada en `app/build.gradle.kts` (actualmente API 37).
- Un emulador o dispositivo Android con Android 8.0 (API 26) o superior.

## Cómo ejecutar

1. Descarga o clona el repositorio.
2. Abre la carpeta raíz `JuntaVecinal` desde Android Studio.
3. Espera a que termine la sincronización de Gradle y la descarga de dependencias.
4. Selecciona un emulador o conecta un dispositivo con depuración USB habilitada.
5. Ejecuta la configuración `app`.

6. ## Estructura del proyecto

```text
JuntaVecinal/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/juntavecinal/
│   │   │   ├── data/          # Configuración de Room
│   │   │   ├── model/
│   │   │   │   ├── dao/       # Consultas de base de datos
│   │   │   │   ├── entity/    # Entidades Usuario y Reserva
│   │   │   │   └── repository/# Acceso a datos y reglas de negocio
│   │   │   ├── ui/
│   │   │   │   ├── components/# Componentes reutilizables
│   │   │   │   ├── screens/   # Pantallas Compose
│   │   │   │   └── theme/     # Tema visual
│   │   │   ├── util/          # RUT, fechas y notificaciones
│   │   │   ├── viewmodel/     # Estado de pantalla y lógica de presentación
│   │   │   └── MainActivity.kt
│   │   ├── src/test/          # Pruebas unitarias
│   │   └── src/androidTest/   # Pruebas instrumentadas
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml     # Versiones y dependencias
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
