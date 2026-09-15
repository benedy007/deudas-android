# Deudas (Android)

App local-first para gestionar clientes, productos, deudas y cobros.  
**Sin Firebase.** Google Sign-In (Credential Manager) + modo invitado offline con Room + respaldo opcional en Google Drive.

## Cómo probar

### Con Google
1. Configura `WEB_CLIENT_ID` (tipo **Web**) en `local.properties` — ver `local.properties.example`
2. En Google Cloud Console: OAuth client Android con package `com.benedy.deudas` y SHA-1 del keystore de debug
3. **Habilita la Google Drive API** en el mismo Cloud project (APIs & Services → Library → Google Drive API → Enable). Sin esto, el respaldo falla en cliente aunque el OAuth esté bien.
4. En la pantalla de consentimiento OAuth, incluye el scope `.../auth/drive.appdata` (o añádelo cuando AuthorizationClient lo pida la primera vez)
5. Si el proyecto OAuth está en **Testing**, solo usuarios de prueba pueden iniciar sesión
6. Abre la app → **Conectar con Google**
7. En **Home → Respaldo**:
   - **Respaldar en Google Drive** — exporta clientes (con direcciones), deudas, pagos y productos a JSON en la carpeta privada `appDataFolder`
   - **Restaurar desde Google Drive** — pide confirmación y reemplaza los datos locales

### Sin cuenta (invitado)
1. Abre la app → **Continuar sin cuenta**
2. En el **Home** verás botones grandes (Agregar cliente, Cobrar, etc.)
3. El bloque **Respaldo** explica que debes **Conectar con Google** primero
4. **Salir** vuelve al login (datos locales se conservan en el dispositivo)

## Build

```bash
export ANDROID_HOME=/workspace/android-sdk
# local.properties debe tener sdk.dir y WEB_CLIENT_ID
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

`WEB_CLIENT_ID` se inyecta en `BuildConfig` desde `local.properties` (no se sube a Git).

## Google Drive (cliente)

| Pieza | Detalle |
|--------|---------|
| Auth | Credential Manager (ID token) para sesión |
| Scopes Drive | `https://www.googleapis.com/auth/drive.appdata` vía `AuthorizationClient` (incremental, al tocar Respaldar/Restaurar) |
| Almacenamiento | Archivo `deudas-backup.json` en `appDataFolder` (no aparece en «Mi unidad») |
| Formato | JSON con `clients`, `debts`, `payments`, `products` (incluye campos de dirección) |
| Cloud Console | Drive API habilitada + OAuth Android + Web client ID |

Alternativa documentada (no usada ahora): carpeta visible «Deudas» con scope `drive.file`.

## Datos y migraciones Room

- **Nunca** usar `fallbackToDestructiveMigration()` en APKs que los usuarios actualizan: borra clientes/deudas al subir la versión de la DB.
- Los cambios de esquema van con `Migration(from, to)` (p. ej. `MIGRATION_1_2` añade columnas de dirección).
- Desinstalar/reinstalar sí borra datos locales (esperado). Actualizar el APK in-place (mismo `applicationId` + firma) debe conservarlos.
- Respaldo Drive: disponible para usuarios con Google (no invitados).

## Arquitectura

Ver [ARCHITECTURE.md](ARCHITECTURE.md).
