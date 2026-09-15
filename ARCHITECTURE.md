# Arquitectura — Deudas (Android)

## V1.1 (actual)

```
ui/
  auth/          Login + AuthViewModel
  home/          Home hub (acciones grandes)
  clients/       Lista + Agregar cliente
  clientdetail/  Detalle / Cobrar (deudas + pagos)
  debt/          Agregar deuda (manual o desde producto)
  payment/       Registrar pago
  receipt/       Comprobante + WhatsApp
  products/      Lista + Agregar producto
  navigation/    NavGraph con auth gate
  theme/
data/
  auth/          AuthRepository + UserSession
  local/         Room (entities, daos, DeudasDatabase)
  repository/    DebtCrmRepository
```

- **UI:** Jetpack Compose + Material 3 + Navigation Compose
- **Home:** hub de acciones — Agregar cliente, Cobrar, Agregar producto, Ver clientes, Ver productos
- **Cobrar:** siempre empieza eligiendo cliente
- **Auth:** Google Sign-In (Credential Manager + WEB_CLIENT_ID) + modo invitado
- **Persistencia:** Room local (offline). Sin Firebase
- **WhatsApp:** wa.me deep links / share
- **Drive:** stub («próximamente»)

## Principios

1. Sin Firebase — auth Google Identity + Drive API directa (futuro)
2. Local-first — Room es la fuente de verdad
3. Secrets fuera de Git — `WEB_CLIENT_ID` solo en `local.properties`
4. Un Activity — toda la UI en Compose Navigation
