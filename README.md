# ContaFácil (Android)

App local-first para gestionar clientes, productos, deudas y cobros.  
**Sin Firebase.** Google Sign-In (Credential Manager) + modo invitado offline con Room + respaldo opcional en Google Drive.

Package / applicationId: `com.benedy.deudas` (no cambiar — OAuth y actualizaciones).

## Cómo probar

### Con Google
1. Configura `WEB_CLIENT_ID` (tipo **Web**) en `local.properties` — ver `local.properties.example`
2. En Google Cloud Console: OAuth client Android con package `com.benedy.deudas` y SHA-1 del keystore de debug
3. **Habilita la Google Drive API** en el mismo Cloud project
4. En la pantalla de consentimiento OAuth, incluye el scope `.../auth/drive.appdata`
5. Si el proyecto OAuth está en **Testing**, solo usuarios de prueba pueden iniciar sesión
6. Abre la app → **Conectar con Google**
7. En **Ajustes → Respaldo Google Drive**:
   - **Respaldar en Google Drive**
   - **Restaurar desde Google Drive**

### Sin cuenta (invitado)
1. Abre la app → **Continuar sin cuenta**
2. En el **Home**: Ver clientes, Ver productos, Cobrar, Historial (agrega vía FAB en listas)
3. **Ajustes** tiene sesión, Salir y el bloque de Respaldo (requiere Google)

## Build

```bash
export ANDROID_HOME=/workspace/android-sdk
# local.properties debe tener sdk.dir y WEB_CLIENT_ID
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Datos y migraciones Room

- **Nunca** usar `fallbackToDestructiveMigration()` en APKs que los usuarios actualizan.
- Los cambios de esquema van con `Migration(from, to)`.
- Actualizar el APK in-place (mismo `applicationId` + firma) debe conservar datos.

## Arquitectura

Ver [ARCHITECTURE.md](ARCHITECTURE.md).
