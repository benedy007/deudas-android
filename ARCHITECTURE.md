# Arquitectura — Deudas (Android)

## V1.4.1 (actual)

- **Eliminar pagos (LIFO):** solo el último pago (o grupo waterfall) del cliente se puede borrar; más antiguos: botón oculto; si se intenta: «Solo puedes eliminar el último pago de este cliente».

## V1.4.0

```
ui/
  auth/          Login + AuthViewModel
  home/          Home hub + dashboard (totales) + Respaldo
  backup/        BackupViewModel (Drive authorize + upload/download)
  clients/       Lista (todos / solo con deuda) + Agregar/Editar + foto
  clientdetail/  Detalle / Cobrar + recordatorio WA + estado de cuenta + notas cobranza
  debt/          Agregar deuda (+ aviso límite de crédito)
  payment/       Registrar pago (waterfall)
  receipt/       Comprobante + WhatsApp
  settings/      Ajustes compañía
  history/       Historial de pagos + reenvío + eliminar
  products/      Lista + Agregar producto
  navigation/    NavGraph con auth gate
  theme/
data/
  auth/          AuthRepository + UserSession (Credential Manager)
  backup/        DriveAuthHelper + DriveBackupRepository + BackupPayload (v2)
  local/         Room (entities, daos, DeudasDatabase + MIGRATION_1_2 … MIGRATION_5_6)
  repository/    DebtCrmRepository (export/import, dashboard, statement)
```

- **UI:** Jetpack Compose + Material 3 + Navigation Compose
- **Dashboard:** total por cobrar, cobrado del mes, clientes en mora
- **Cobrar:** picker solo clientes con saldo > 0; Ver clientes muestra todos
- **Foto cliente:** cámara/galería → filesDir/client_photos + photoPath
- **Crédito / saldo inicial:** límite con aviso; saldo inicial crea deuda al crear cliente
- **Cobranza:** notas + fecha prometida; recordatorio WhatsApp; estado de cuenta (imagen)
- **Auth / Drive / pagos waterfall / cuotas / ajustes:** igual que v1.3.x
- **Migraciones:** explícitas, sin `fallbackToDestructiveMigration`

## Principios

1. Sin Firebase — auth Google Identity + Drive API directa
2. Local-first — Room es la fuente de verdad; Drive es copia de seguridad
3. Secrets fuera de Git — `WEB_CLIENT_ID` solo en `local.properties`
4. Un Activity — toda la UI en Compose Navigation
5. Migraciones explícitas — nunca borrar datos en actualizaciones de APK
