# Arquitectura — Deudas (Android)

## V1 (actual)

```
ui/
  auth/     LoginScreen + AuthViewModel + AuthUiState
  home/     HomeScreen (perfil + stubs)
  navigation/  NavGraph con auth gate
  theme/    Material 3
data/
  auth/     AuthRepository + UserSession (Credential Manager / Google Identity)
```

- **UI:** Jetpack Compose + Material 3 + Navigation Compose
- **Auth:** Google Sign-In vía Credential Manager (`androidx.credentials` + `googleid`)
- **Sesión:** SharedPreferences locales (sin Firebase)
- **Sin backend propio** en V1

## Próximas capas (planificadas)

### Room (clientes / deudas)

```
data/
  local/
    DeudasDatabase.kt
    dao/ ClientDao, DebtDao
    entity/ ClientEntity, DebtEntity
  repository/
    ClientRepository, DebtRepository
```

- Base local-first: la app funciona offline
- Entidades: Cliente, Deuda, (opcional) pagos / notas
- ViewModels por feature (Clientes, Deudas, Detalle)

### Google Drive (respaldo — NO Firebase)

```
data/
  drive/
    DriveBackupRepository.kt
    BackupSerializer.kt   // JSON o protobuf del dump Room
```

- OAuth con la misma cuenta Google (scopes Drive App Data o carpeta dedicada)
- Exportar / importar snapshot de la BD Room
- UI: botón real en Home (hoy es stub «próximamente»)
- Sin Cloud Firestore / Realtime Database / Firebase Auth

### WhatsApp (futuro)

- Intents / deep links para recordar deudas a clientes
- Sin SDK oficial requerido en el diseño inicial

## Principios

1. **Sin Firebase** — auth Google Identity + Drive API directa
2. **Local-first** — Room es la fuente de verdad; Drive es respaldo
3. **Secrets fuera de Git** — `WEB_CLIENT_ID` solo en `local.properties`
4. **Un Activity** — toda la UI en Compose Navigation
