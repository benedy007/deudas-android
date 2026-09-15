# Deudas (Android)

App local-first para gestionar clientes, productos, deudas y cobros.  
**Sin Firebase.** Modo invitado funciona 100% offline con Room.

## Cómo probar (modo invitado)

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
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Arquitectura

Ver [ARCHITECTURE.md](ARCHITECTURE.md).
