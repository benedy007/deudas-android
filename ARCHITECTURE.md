# Arquitectura — Deudas (Android)

## V1.3.5 (actual)

```
ui/
  auth/          Login + AuthViewModel
  home/          Home hub (acciones + sección Respaldo)
  backup/        BackupViewModel (Drive authorize + upload/download)
  clients/       Lista + Agregar cliente
  clientdetail/  Detalle / Cobrar (deudas + pagos)
  debt/          Agregar deuda (manual o desde producto)
  payment/       Registrar pago
  receipt/       Comprobante + WhatsApp
  settings/      Ajustes compañía (nombre, teléfono, pie de recibo)
  history/       Historial de pagos + reenvío
  products/      Lista + Agregar producto
  navigation/    NavGraph con auth gate
  theme/
data/
  auth/          AuthRepository + UserSession (Credential Manager)
  backup/        DriveAuthHelper + DriveBackupRepository + BackupPayload
  local/         Room (entities, daos, DeudasDatabase + MIGRATION_1_2 + MIGRATION_2_3)
  repository/    DebtCrmRepository (export/import backup)
```

- **UI:** Jetpack Compose + Material 3 + Navigation Compose
- **Home:** hub de acciones + Respaldo (Drive) para sesión Google
- **Auth:** Google Sign-In (Credential Manager + WEB_CLIENT_ID) + modo invitado
- **Drive:** AuthorizationClient pide `drive.appdata` al respaldar/restaurar; REST API v3
- **Persistencia:** Room local (offline). Sin Firebase. Sin `fallbackToDestructiveMigration`
- **WhatsApp:** texto vía wa.me; imagen vía system share sheet (elige WhatsApp)
- **Pagos:** waterfall sin sobrepago; historial/comprobante permiten eliminar pago (revierte saldo)
- **Ajustes:** DataStore (nombre/teléfono/comentario de compañía en recibos)
- **UI:** Material 3 con elevación, esquinas grandes, gradientes suaves (ScreenGradient / ElevatedActionCard)

## Principios

1. Sin Firebase — auth Google Identity + Drive API directa
2. Local-first — Room es la fuente de verdad; Drive es copia de seguridad
3. Secrets fuera de Git — `WEB_CLIENT_ID` solo en `local.properties`
4. Un Activity — toda la UI en Compose Navigation
5. Migraciones explícitas — nunca borrar datos en actualizaciones de APK
