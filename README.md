# JuntaVecinal

Aplicación Android en Kotlin y Jetpack Compose para reservar el salón de la junta vecinal. Usa Room para consultar la caché de reservas por cuenta y Retrofit para comunicarse con el Gateway de `backend-app`.

## Ejecutar en local

1. En la carpeta `backend-app`, ejecuta `docker compose up --build`. Espera a que los servicios se registren en Eureka.
2. Abre `JuntaVecinal-Frontend` en Android Studio, sincroniza Gradle y ejecuta la app en el emulador. El proyecto usa el SDK de Android 37 para compilar.
3. La app entra por `http://10.0.2.2:8080/api/`, que apunta al Gateway del PC. En Android 17, concede el permiso de red local cuando aparezca.
4. Entra con `22.222.222-2` y `vecino123`, o registra una cuenta ficticia. Desde **Reservas** puedes crear una solicitud, consultarla, cambiar la fecha y hora o cancelarla.

El calendario de Android deshabilita los feriados nacionales que informa Roison. La app también revisa las fechas escritas a mano al crear o reprogramar una reserva. Si el calendario no carga o no cubre el año elegido, Android pide reintentar o elegir otro año antes de guardar.

El Compose de `backend-app` publica MySQL en el puerto `3306` del PC. Si está ocupado, ajusta el puerto publicado del contenedor conservando `mysql-db:3306` dentro de Docker. La URL del Gateway se mantiene.

Finanzas aparece para Directiva, Tesorería y Secretaría. Directiva y Tesorería pueden registrar movimientos y abonar cuotas previamente asignadas. Las migraciones de `backend-app` crean las dos cuentas de autenticación de ejemplo con rol `VECINO`. Para demostrar Finanzas, prepara una cuenta de prueba con un rol autorizado en `auth-service` y vuelve a iniciar sesión después de cambiarlo.

## Rutas que usa Android

| Función | Método y ruta por el Gateway |
| --- | --- |
| Registro e inicio de sesión | `POST /api/auth/register`, `POST /api/auth/login` |
| Listar y crear reservas del salón | `GET /api/reserva/reservas/app?usuarioId={id}`, `POST /api/reserva/reservas/app` |
| Consultar el espacio, reprogramar y cancelar | `GET`, `PUT` y `DELETE /api/reserva/reservas/{id}` |
| Resumen financiero y movimientos por período | `GET /api/finanzas/reportes`, `GET /api/finanzas/movimientos` |
| Registrar un movimiento | `POST /api/finanzas/movimientos` |
| Consultar asignaciones pendientes y registrar un pago | `GET /api/finanzas/cuotas/asignaciones`, `POST /api/finanzas/pagos` |
| Feriados de Chile en Inicio y Reservas | `GET https://api.roison.cl/feriados.json` |

## Pruebas y compilación

Desde `JuntaVecinal-Frontend`, con el SDK de Android configurado, ejecuta lo siguiente.

```sh
./gradlew :app:assembleDebug :app:createDebugUnitTestCoverageReport :app:testDebugUnitTest
```

En Windows usa `gradlew.bat`. Las pruebas unitarias usan respuestas simuladas y se ejecutan con el Gateway apagado. El informe queda en `app/build/reports/coverage/test/debug/index.html` y el APK de depuración en `app/build/outputs/apk/debug/app-debug.apk`.
