# Arquitectura — ContaFácil (Android)

## V1.5.0 (actual)

- **Nombre:** ContaFácil (package/applicationId sigue `com.benedy.deudas`).
- **Home limpio:** solo Ver clientes, Ver productos, Cobrar, Historial. Sin «¿Qué deseas hacer?», sin Agregar cliente/producto en hub, sin email ni Salir ni Respaldo.
- **Resumen:** Total por cobrar, Cobrado del mes, **Cobrado en el día** (timezone local del dispositivo).
- **Ajustes:** compañía + sesión (email/invitado) + Salir + Respaldo Drive.
- **Lista clientes:** avatar + nombre + **total adeudado** + WhatsApp; verde (abono reciente) / rojo (mora).

## V1.4.3

- LazyColumn keys únicos para notas/deudas.

## V1.4.2

- Lista de clientes compacta + colores de estado (abono reciente gana sobre mora).

## V1.4.1

- Eliminar pagos (LIFO).

## V1.4.0

```
ui/
  auth/          Login + AuthViewModel
  home/          Home hub + dashboard
  backup/        BackupViewModel (Drive authorize + upload/download)
  clients/       Lista + Agregar/Editar + foto
  clientdetail/  Detalle / Cobrar + cobranza
  debt/          Agregar deuda
  payment/       Registrar pago (waterfall)
  receipt/       Comprobante + WhatsApp
  settings/      Ajustes compañía + sesión + Drive
  history/       Historial de pagos
  products/      Lista + Agregar producto
  navigation/    NavGraph con auth gate
data/
  auth/          AuthRepository + UserSession
  backup/        DriveAuthHelper + DriveBackupRepository
  local/         Room (migraciones explícitas)
  repository/    DebtCrmRepository
```

- **Migraciones:** explícitas, sin `fallbackToDestructiveMigration`

## Principios

1. Sin Firebase — auth Google Identity + Drive API directa
2. Local-first — Room es la fuente de verdad; Drive es copia de seguridad
3. Secrets fuera de Git — `WEB_CLIENT_ID` solo en `local.properties`
4. Un Activity — toda la UI en Compose Navigation
5. Migraciones explícitas — nunca borrar datos en actualizaciones de APK
