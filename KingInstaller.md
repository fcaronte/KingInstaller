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

### 1.4. ROM Cinesi (Xiaomi / MIUI / HyperOS) — Gli Extra dell'Intent Vengono Ignorati
- **Causa**: su MIUI/HyperOS gli intent di installazione sono dirottati verso `com.miui.packageinstaller`, che **ignora gli extra** (`EXTRA_INSTALLER_PACKAGE_NAME`, `REFERRER_NAME`, `ORIGINATING_PACKAGE`, ecc.). L'app viene quindi installata con "Installed by" errato. **Questo riguarda Xiaomi/MIUI/HyperOS, NON Samsung.**
- **Nota sui test (Samsung S24)**: su **One UI** il metodo **classico** funziona già (installa come `com.android.vending`), quindi Samsung non è un device da correggere. Nei test l'S24 è usato solo come banco di prova del **codice post-installazione**, forzando il path Shizuku (che lancia il dialog come shell → "Installed by" = `com.google.android.packageinstaller`). La validazione reale del fix va fatta su un device **Xiaomi**.
- **Fix affidabile**: impostare l'installer **dopo** l'installazione, con un comando diretto al package manager eseguito con privilegi (Shell via Shizuku `UserService`, oppure Root):
  `cmd package set-installer-package --user 0 <pkg> com.android.vending` (con fallback `pm set-installer <pkg> com.android.vending`).
- **Implementazione (via primaria = binder)**: `ShizukuUtils.setInstallerViaShizuku()` imposta "Installed by" chiamando direttamente `IPackageManager.setInstallerPackageName` tramite il binder di sistema wrappato da Shizuku (`setInstallerViaBinder`) — la **stessa tecnica dell'installazione** (`startInstallActivityAsShell`), **senza avviare un UserService**. ⚠️ Riesce solo se il chiamante è root/system: da shell semplice viene rifiutato (vedi LIMITE FONDAMENTALE sotto). `RootUtils.setInstallerViaRoot()` fa lo stesso via `su`. Il `packageAddedReceiver` in `MainActivity` lancia il fix in modo **reattivo**, solo se dopo `fixGraceMillis` l'app non è già `isAACompatible`; timeout del polling 6 s.
- **Fallback UserService**: se la via binder fallisce, si ripiega su `MyUserService` (AIDL → `set-installer` via shell), con warm-up (`prewarmUserService` chiamato in `installApk`) e binder in cache (`cachedService`). **NB**: su alcune One UI (es. l'S24 di test) il `UserService` **non si connette affatto** (bind silenzioso, senza eccezione né log).
- **⚠️ LIMITE FONDAMENTALE scoperto (test S24, One UI/Android 17)**: impostare "Installed by" = `com.android.vending` da **shell** (Shizuku senza root) è **bloccato dal sistema**: `SecurityException: Caller does not have same cert as new installer package com.android.vending`. Il check confronta la firma del chiamante con quella di Google (vending): lo shell (firma piattaforma) non combacia. `pm set-installer` usa lo **stesso** metodo IPC (`IPackageManager.setInstallerPackageName`) → identico blocco. Solo **root/system** aggira il check (bypass tipo `isCallerSystemOrRoot`). **Conclusione: il no-root Shizuku NON può impostare vending su questi device → serve Root** (oppure Shizuku avviato con permessi di root). Coerente con la storica indicazione "su Xiaomi serve Root". Il metodo **classico** su Samsung funziona perché è il package installer *di sistema* (Google, stessa firma) a onorare l'extra, non lo shell.
- **Limite del metodo classico puro**: senza privilegi (né Root né Shizuku) su Xiaomi NON è possibile correggere "Installed by" dopo che l'installer MIUI ha ignorato gli extra. Root/Shizuku restano necessari.
- **Nota su "Requested by"**: `set-installer` cambia solo "Installed by" (`installingPackageName`). "Requested by" (`initiatingPackageName`) resta quello del dialog (es. `com.miui.packageinstaller`, che contiene "packageinstaller" e quindi supera `isAACompatible`). Non è modificabile dopo l'installazione.

### 1.5. Invarianti UI Installer (NON regredire)
- Il **finto installer di King** (activity-alias `.OppoTrick`, schema `package`) deve essere **visibile solo** durante l'installazione in **modalità Oppo**. Fuori da quella modalità resta disabilitato.
- Durante "Installa con King" l'alias `.ApkHandler` viene **disabilitato** così King non compare nel chooser dell'installer di sistema (evita tap per errore). Gestito da `updateComponentStates(installing)`.
- Aprendo un APK **dall'esterno** (file manager / "condividi con"), King **deve** restare tra le app con cui aprire, caricando l'APK pronto all'installazione: l'alias `.ApkHandler` è abilitato a riposo e riabilitato in `onResume`.

### 1.6. Policy per Versione Android
- **Android 15+ (API 35)**: lo switch **Oppo/Realme/OnePlus** è nascosto (non più utile) e disattivato se residuo da sessioni precedenti.
- **Fino ad Android 14 (API 34)**: il suggerimento iniziale basato sul package installer rilevato (`isGooglePackageExist`) viene mostrato; su API ≥ 35 è soppresso perché l'euristica non è più attendibile.

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
  - Fix installer **post-installazione reattivo** per Xiaomi via Shizuku (oltre che Root): vedi §1.4.
  - **Policy per versione Android** (vedi §1.6): switch Oppo nascosto su Android 15+; suggerimento package installer solo fino ad Android 14.
  - **Feedback del fix installer**: durante la correzione post-installazione la UI mostra lo stato (arancione) e, in caso di fallimento, i valori reali di install-source ("Installed by / Requested by").
  - **Condivisione log diagnostico**: icona di condivisione accanto al titolo "Welcome to Installer!" (`shareLogButton`) genera e condivide un report (device, ROM MIUI/HyperOS via `getprop`, stato metodo/Shizuku/root, install-source dell'ultima app, logcat del processo) — `DiagnosticsUtils.kt`.

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
- `app/src/main/AndroidManifest.xml`: Configurazione `FileProvider`, filtri Intent `ACTION_VIEW` e `ACTION_SEND`, alias `.ApkHandler` e `.OppoTrick`.
- `ShizukuUtils.kt`: Installazione via Shizuku (dialog come shell) + `setInstallerViaShizuku()` (fix post-installazione via `UserService`).
- `RootUtils.kt`: Installazione via `su` + `setInstallerViaRoot()`.
- `MyUserService.kt` / `IMyUserService.aidl`: `UserService` remoto che esegue `set-installer` con privilegi Shell/Root.
- `MainActivity.kt`: Selezione metodo, `packageAddedReceiver` con polling reattivo + fix, `updateComponentStates()`, policy per versione Android.
- `KingInstaller.md`: Questo file di memoria continua per la persistenza del contesto e delle linee guida di sviluppo.
- `CLAUDE.md`: Istruzioni operative per le sessioni Claude (workflow, invarianti da non regredire).

---

## 5. Next Steps (Roadmap & TODO)
- [ ] Monitorare le modifiche delle API del PackageInstaller sulle future versioni di Android (Android 15+).
- [ ] Mantenere aggiornati i collegamenti consigliati per Shizuku Next e il supporto multi-lingua.
- [ ] Verificare costantemente che ogni refactoring rispetti la regola d'oro "No Shell + PlayStore".