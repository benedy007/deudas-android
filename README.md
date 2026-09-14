# Deudas — Android

App Android en **Kotlin + Jetpack Compose + Material 3** para gestionar clientes y deudas.

**V1:** solo inicio de sesión con Google (Credential Manager / Google Identity).  
**Próximamente:** clientes, deudas, WhatsApp y respaldo en Google Drive (**sin Firebase**).

- **Package:** `com.benedy.deudas`
- **minSdk:** 26 · **compileSdk / targetSdk:** 35

---

## Abrir el proyecto en Android Studio

1. Instala [Android Studio](https://developer.android.com/studio) (Ladybug o más reciente recomendado).
2. **File → Open** y selecciona esta carpeta (`deudas-android`).
3. Espera a que Gradle sync termine (descargará el wrapper y dependencias).
4. Copia `local.properties.example` → `local.properties` y completa los valores (ver abajo).
5. Conecta un dispositivo/emulador y pulsa **Run**.

> Si Android Studio crea `local.properties` solo con `sdk.dir`, añade la línea `WEB_CLIENT_ID=...` tú mismo.

---

## Configurar Google Sign-In (obligatorio)

Hace falta un proyecto en [Google Cloud Console](https://console.cloud.google.com/).

### 1. Obtener el SHA-1 de depuración

En la máquina de desarrollo:

```bash
# macOS / Linux
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# Windows (PowerShell)
keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Copia el valor **SHA1** (formato `AA:BB:CC:...`).

Con Gradle (alternativa, tras el primer sync):

```bash
./gradlew signingReport
```

### 2. Crear clientes OAuth en Google Cloud

1. Crea o elige un proyecto en Cloud Console.
2. **APIs y servicios → Pantalla de consentimiento de OAuth** (tipo Externo o Interno; en prueba puedes añadir tu Gmail como usuario de prueba).
3. **APIs y servicios → Credenciales → Crear credenciales → ID de cliente de OAuth**:

#### A) Cliente **Android** (obligatorio para que el dispositivo confíe en la app)

| Campo | Valor |
|--------|--------|
| Tipo | Android |
| Nombre | Deudas Android |
| Nombre del paquete | `com.benedy.deudas` |
| Huella SHA-1 | la de tu `debug.keystore` (y más adelante la de release) |

#### B) Cliente **Aplicación web** (obligatorio como `serverClientId`)

| Campo | Valor |
|--------|--------|
| Tipo | Aplicación web |
| Nombre | Deudas Web (Credential Manager) |

Copia el **ID de cliente** del tipo **Web** (termina en `.apps.googleusercontent.com`).

> Credential Manager usa el **Web Client ID** como `serverClientId`, aunque la app sea Android. El cliente Android (paquete + SHA-1) también debe existir.

### 3. Poner el Web Client ID en `local.properties`

```properties
sdk.dir=/ruta/a/tu/Android/Sdk
WEB_CLIENT_ID=1234567890-xxxx.apps.googleusercontent.com
```

Plantilla: ver `local.properties.example`.  
**No subas** `local.properties` a Git (ya está en `.gitignore`).

Tras cambiar `WEB_CLIENT_ID`, haz **Sync / Rebuild** para regenerar `BuildConfig`.

---

## Ejecutar / generar APK

```bash
# Debug APK
./gradlew assembleDebug

# Instalar en dispositivo conectado
./gradlew installDebug
```

APK de debug: `app/build/outputs/apk/debug/app-debug.apk`

Para release necesitarás un keystore propio y el SHA-1 de release en el cliente OAuth Android.

---

## Estructura (resumen)

```
app/src/main/java/com/benedy/deudas/
  MainActivity.kt          # Única Activity
  DeudasApp.kt             # Application + AuthRepository
  data/auth/               # AuthRepository, UserSession
  ui/auth/                 # LoginScreen, AuthViewModel, AuthUiState
  ui/home/                 # HomeScreen (perfil + stubs)
  ui/navigation/           # NavGraph + auth gate
  ui/theme/                # Material 3
```

Más detalle: [ARCHITECTURE.md](ARCHITECTURE.md).

---

## Notas

- **Sin Firebase.** Auth = Google Identity Services + Credential Manager. Respaldo futuro = Google Drive API.
- No hay secretos reales en el repositorio; solo placeholders.
- Si el botón «Continuar con Google» falla: revisa SHA-1, package name, Web Client ID y que la cuenta esté en usuarios de prueba del consentimiento OAuth.
