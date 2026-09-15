# Deudas (Android)

App local-first para gestionar clientes, productos, deudas y cobros.  
**Sin Firebase.** Google Sign-In (Credential Manager) + modo invitado offline con Room.

## Cómo probar

### Con Google
1. Configura `WEB_CLIENT_ID` (tipo **Web**) en `local.properties` — ver `local.properties.example`
2. En Google Cloud Console: OAuth client Android con package `com.benedy.deudas` y SHA-1 del keystore de debug
3. Si el proyecto OAuth está en **Testing**, solo usuarios de prueba pueden iniciar sesión
4. Abre la app → **Conectar con Google**

### Sin cuenta (invitado)
1. Abre la app → **Continuar sin cuenta**
2. En el **Home** verás botones grandes:
   - **Agregar cliente** — nombre + teléfono WhatsApp
   - **Cobrar** — elige cliente → agrega deuda / registra pago → comprobante → **Enviar por WhatsApp**
   - **Agregar producto** — catálogo para usar al crear deudas
   - **Ver clientes** / **Ver productos**
3. **Salir** vuelve al login (datos locales se conservan en el dispositivo)

## Build

```bash
export ANDROID_HOME=/workspace/android-sdk
# local.properties debe tener sdk.dir y WEB_CLIENT_ID
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

`WEB_CLIENT_ID` se inyecta en `BuildConfig` desde `local.properties` (no se sube a Git).

## Datos y migraciones Room

- **Nunca** usar `fallbackToDestructiveMigration()` en APKs que los usuarios actualizan: borra clientes/deudas al subir la versión de la DB.
- Los cambios de esquema van con `Migration(from, to)` (p. ej. `MIGRATION_1_2` añade columnas de dirección).
- Desinstalar/reinstalar sí borra datos locales (esperado). Actualizar el APK in-place (mismo `applicationId` + firma) debe conservarlos.
- Backup a Google Drive: todavía por venir.

## Arquitectura

Ver [ARCHITECTURE.md](ARCHITECTURE.md).
