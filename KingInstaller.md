# PROJECT MEMORY & TECHNICAL SPECIFICATIONS: KingInstaller

---

## 0. General Information & Package Settings
- **Project Name**: KingInstaller
- **Repository**: `fcaronte/KingInstaller`
- **Package Name (STRICT)**: `com.example.kinginstaller`
  > **Nota Importante**: Il package name legacy `com.example.kinginstaller` e la firma di debug originale devono essere rigorosamente mantenuti e NON modificati per preservare la compatibilità di aggiornamento ed evitare discrepanze nelle firme.
- **Primary Tech Stack**: Kotlin, Android SDK, Coroutines, Shizuku / Shizuku Next IPC, Root Execution.

---

## 1. Android Auto Requirements & Installation Golden Rule (CRITICAL)

Per fare in modo che le applicazioni di terze parti (es. **AABrowser**) vengano rilevate e mostrate correttamente nell'interfaccia di **Android Auto**, è necessario soddisfare tassativamente le seguenti condizioni di installazione del sistema:

### 1.1. Iniziatore dell'Installazione (Requested By / Initiating Package)
- **Requisito Minimo**: L'installazione DEVE essere avviata dal **Package Installer di sistema** (es. `com.google.android.packageinstaller` / `com.android.packageinstaller`) oppure direttamente dal **Google Play Store** (`com.android.vending`).
- **Divieto di Shell**: L'installazione **NON deve mai risultare avviata dalla Shell ADB** (`com.android.shell`).

### 1.2. Attributo di Origine (Installed By / Installer Package Name)
- **Requisito Obbligatorio**: L'installazione deve attribuire il pacchetto al Play Store:
  `InstallationUtils.VENDING_PKG` = `com.android.vending`.

### 1.3. Combinazione Errata (Blocco Android Auto)
- **`com.android.shell` + `com.android.vending`**: **FALLISCE**. Se il pacchetto risulta installato da Play Store ma avviato da Shell ADB, Android Auto rifiuta l'app e la nasconde dalla dashboard dell'auto.
- **`Package Installer` + `com.android.vending`**: **SUCCESSO**. L'app viene correttamente riconosciuta e mostrata in Android Auto.

---

## 2. Current Status & Milestone
- **Versione Attuale**: v2.3
- **Funzionalità Implementate**:
  - Architettura di installazione ibrida (Classico / Shizuku / Root).
  - Integrazione Shizuku / Shizuku Next / ShizukuPlus via Binder IPC.
  - Funzione di reinstallazione in-app per riparare le app sideloaded senza visibilità su Android Auto.
  - Supporto nativo per la selezione della lingua per-app (Android 13+ / API 33).
  - Diagnostic Checker integrato per verificare "Installed By" e "Requested By".
  - Gestione sicura di file di grandi dimensioni tramite URI `FileProvider` asincroni.

---

## 3. Key Architectural Decisions

1. **Integrazione Shizuku & IPC Binder**:
  - Raccomandata e prioritaria l'adozione di **Shizuku Next** (`rushiranpise/Shizuku-Next`) per la resilienza dell'auto-start senza connessione Wi-Fi al riavvio.
  - **Constraint IPC**: Mai utilizzare API remote deprecate o non-standard come `Shizuku.newProcess()`. Usare sempre `UserService` con Binder IPC per evitare referenze `IRemoteProcess` nulle su custom ROM (One UI, HyperOS, etc.).

2. **Gestione Asincrona dei Trasferimenti APK**:
  - Tutte le operazioni di lettura/trasferimento tramite `content://` URI e `FileProvider` sono eseguite tramite Kotlin Coroutines per prevenire blocchi UI e memory leak / OOM su file APK di grandi dimensioni.

---

## 4. Modified / Maintained Files Checklist
- `app/build.gradle`: Configurato con `applicationId "com.example.kinginstaller"` e la firma di debug predefinita.
- `app/src/main/AndroidManifest.xml`: Configurazione `FileProvider`, filtri Intent `ACTION_VIEW` e `ACTION_SEND`.
- `PROJECT_MEMORY.md`: Questo file di memoria continua per la persistenza del contesto e delle linee guida di sviluppo.

---

## 5. Next Steps (Roadmap & TODO)
- [ ] Monitorare le modifiche delle API del PackageInstaller sulle future versioni di Android (Android 15+).
- [ ] Mantenere aggiornati i collegamenti consigliati per Shizuku Next e il supporto multi-lingua.
- [ ] Verificare costantemente che ogni refactoring rispetti la regola d'oro "No Shell + PlayStore".