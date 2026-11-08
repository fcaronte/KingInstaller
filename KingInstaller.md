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

### 1.4. ROM Cinesi (Xiaomi / MIUI / HyperOS / POCO / Redmi)
- **Configurazione Shizuku per Xiaomi/HyperOS/MIUI**: Confermato e testato (es. POCO F6 con Android 16). Per far funzionare il metodo Shizuku su dispositivi Xiaomi/HyperOS/MIUI **è necessario abilitare due opzioni nelle Opzioni Sviluppatore**:
  1. **Installa tramite USB** ("Install via USB")
  2. **Debug USB (Impostazioni di sicurezza)** ("USB debugging - Security settings")
  *(Requisito richiesto dall'app Shizuku stessa per consentire le operazioni di installazione e gestione pacchetti).*
- **Fallback UserService**: se la via binder diretta fallisce, si ripiega su `MyUserService` (AIDL → `set-installer` via shell), con warm-up (`prewarmUserService` chiamato in `installApk`) e binder in cache (`cachedService`).
- **Nota su "Requested by"**: `set-installer` cambia solo "Installed by" (`installingPackageName`). "Requested by" (`initiatingPackageName`) resta quello del dialog (es. `com.miui.packageinstaller`, che contiene "packageinstaller" e quindi supera `isAACompatible`).

### 1.5. Invarianti UI Installer (NON regredire)
- Il **finto installer di King** (activity-alias `.OppoTrick`, schema `package`) deve essere **visibile solo** durante l'installazione in **modalità Oppo**. Fuori da quella modalità resta disabilitato.
- Durante "Installa con King" l'alias `.ApkHandler` viene **disabilitato** così King non compare nel chooser dell'installer di sistema (evita tap per errore). Gestito da `updateComponentStates(installing)`.
- Aprendo un APK **dall'esterno** (file manager / "condividi con"), King **deve** restare tra le app con cui aprire, caricando l'APK pronto all'installazione: l'alias `.ApkHandler` è abilitato a riposo e riabilitato in `onResume`.

### 1.6. Policy per Versione Android
- **Android 15+ (API 35)**: lo switch **Oppo/Realme/OnePlus** è nascosto (non più utile) e disattivato se residuo da sessioni precedenti.
- **Fino ad Android 14 (API 34)**: il suggerimento iniziale basato sul package installer rilevato (`isGooglePackageExist`) viene mostrato; su API ≥ 35 è soppresso perché l'euristica non è più attendibile.

### 1.7. Panorama Reale dei Metodi e le 2 Forzature di King (Classico vs Shizuku tra A13, A15, A16, A17)
- **Le 2 Forzature Indispensabili di King E l'Obbligo di Passare dall'Installer Stock**:
  - Le sole 2 forzature da sole non bastano: per far comparire e funzionare qualsiasi app di terze parti su Android Auto (anche quelle non conformi ai criteri di Google), il campo **`Requested by` (initiatingPackageName)** DEVE essere tassativamente il **Package Installer di sistema** (o Play Store), e **MAI** la shell.
  - **Per questo motivo KingInstaller DEVE passare sempre dall'installatore pacchetti stock** (aprendo il relativo dialog a schermo) e **NON** può usare installazioni silenziose o dirette via shell/adb (come `pm install`), che registrerebbero inevitabilmente `Requested by: com.android.shell` causando il blocco categorico da parte di Android Auto.
  - Le **2 opzioni forzate** passate all'installatore stock sono:
    1. `Intent.EXTRA_INSTALLER_PACKAGE_NAME` = `com.android.vending` (attribuisce "Installed by" al Google Play Store).
    2. `Intent.EXTRA_NOT_UNKNOWN_SOURCE` = `true` (attesta al sistema che l'app NON proviene da origini sconosciute/untrusted).

- **Dove il Metodo Classico FUNZIONA BENISSIMO (Samsung A16/A17 e Pixel A16)**:
  - Su **Samsung con Android 16 e 17 (One UI)** e sui **Pixel con Android 16**, il **Metodo Classico** funziona alla grande: il Package Installer di sistema gestisce e onora direttamente le due forzature di King passate dall'Activity, consentendo di ottenere sia `Requested by: Package Installer` sia `Installed by: Play Store`.

- **Dove Shizuku ECCELLE (A15, A16, A17) e perché FALLISCE sui Samsung**:
  - Su **quasi tutti i dispositivi con Android 15, 16 e 17** (Pixel, Xiaomi HyperOS/MIUI, Honor, OnePlus, Realme, Motorola, ecc.), il **Metodo Shizuku** funziona perfettamente perché la Shell ADB (UID 2000) possiede i privilegi per avviare l'installazione imponendo le forzature senza blocchi.
  - **Nei Samsung invece Shizuku FALLISCE**: l'architettura di sicurezza di Samsung One UI (Knox e `SecPackageInstaller`) blocca o sterilizza le forzature richieste da King se l'intent o il comando proviene dalla Shell (`com.android.shell`). Per questo motivo sui Samsung con A16/A17 bisogna usare il **Metodo Classico**.

- **Il caso anomalo di Android 13 (perché Shizuku non si avviava)**:
  - Su **Android 13 stock con Google Package Installer**:
    - Con **Shizuku**: `InstallStart` verifica che il chiamante dichiari `REQUEST_INSTALL_PACKAGES`. La Shell (UID 2000) non lo dichiara a manifest, e su A13 `InstallStart` termina immediatamente con `finish()` e l'errore `Requesting uid 2000 needs to declare permission android.permission.REQUEST_INSTALL_PACKAGES`, causando la mancata apertura di qualsiasi interfaccia a schermo ("clicco installa e non si avvia nulla").
    - Con il **Classico**: su alcune build AOSP/Google A13 aggiornate con la patch CVE-2023-21017 (`A-236687884`), `InstallStart` rimuove l'extra `The given installer package name com.android.vending is invalid. Remove it.` declassando l'installer a `com.google.android.packageinstaller`.
    - Su questi specifici firmware A13 in cui sia Shizuku che il Classico sono limitati, la via risolutiva definitiva è il **Metodo Root**.

---

## 2. Current Status & Milestone
- **Versione Attuale**: v2.6 (versionCode 13)
- **Funzionalità Implementate**:
  - Architettura di installazione ibrida (Classico / Shizuku / Root).
  - Integrazione Shizuku / Shizuku Next / ShizukuPlus / Shizuku Next v14.0.33-next via Binder IPC e query manifest estese (`moe.shizuku.manager`, `rikka.shizuku`, `moe.shizuku.manager.dev`, `moe.shizuku.privileged.api`).
  - Funzione di reinstallazione in-app per riparare le app sideloaded senza visibilità su Android Auto.
  - Supporto nativo per la selezione della lingua per-app (Android 13+ / API 33).
  - Diagnostic Checker integrato per verificare "Installed By" e "Requested By".
  - **Supporto nativo per file split APK (`.xapk`, `.apks`, `.apkm`, `.zip`)**:
    - Estrazione asincrona e gestione dei file di espansione OBB (`/sdcard/Android/obb/<packageName>/`).
    - Ordinamento automatico con posizionamento dell'APK base all'indice 0.
    - **Dialogo di selezione interattivo con SplitType**: classificazione automatica e pre-selezione basata sull'architettura CPU del dispositivo (`ABI_COMPATIBLE`), sulla densità dello schermo (`DENSITY_COMPATIBLE`, inclusi tutti gli split DPI come `mdpi`), e sulla lingua di sistema (`LANG_COMPATIBLE` per la lingua corrente e inglese).
    - Pulsante `"Seleziona tutto"` / `"Deseleziona tutto"` all'interno di un layout dedicato (`dialog_split_selection.xml`) per prevenire problemi di spazio o wrapping in tutte le 11 lingue.
  - **Reinstallazione app split da lista app (`AppManagerActivity`)**: estrazione automatica di base + split installati, compattazione in archivio `.apks` e passaggio al motore KingInstaller.
  - **Download e installazione aggiornamenti in-app (`UpdateChecker`)**: download diretto dell'APK di aggiornamento da GitHub e avvio istantaneo dell'installazione al termine.
  - **Pulizia permessi Android 13+**: rimozione del prompt obsoleto `MANAGE_EXTERNAL_STORAGE` all'avvio (gestione tramite URI `content://` e file picker di sistema).
  - **Fix BAL (Background Activity Launch) su Android 13+**: utilizzo di `PendingIntent.getActivity` verso `MainActivity` per le sessioni split, consentendo l'apertura garantita del package installer su Android 13, 14, 15 e 17.

---

## 3. Key Architectural Decisions

1. **Integrazione Shizuku & IPC Binder**:
  - Raccomandata e prioritaria l'adozione di **Shizuku Next** (`rushiranpise/Shizuku-Next`) per la resilienza dell'auto-start senza connessione Wi-Fi al riavvio.
  - **Constraint IPC**: Mai utilizzare API remote deprecate o non-standard come `Shizuku.newProcess()`. Usare sempre `UserService` con Binder IPC per evitare referenze `IRemoteProcess` nulle su custom ROM (One UI, HyperOS, etc.).

2. **Gestione Asincrona dei Trasferimenti APK**:
  - Tutte le operazioni di lettura/trasferimento tramite `content://` URI e `FileProvider` sono eseguite tramite Kotlin Coroutines per prevenire blocchi UI e memory leak / OOM su file APK di grandi dimensioni.

3. **Gestione Pacchetti Multi-Parti (XAPK / APKS / APKM)**:
  - `PackageArchiveUtils`: estrazione ZIP in directory temporanee `split_temp_*`, riordinamento automatico posizionando l'APK base all'indice 0, copia automatica dei file OBB in `/sdcard/Android/obb/<packageName>/`. Se l'archivio contiene 1 solo APK, il flusso degrada in modo trasparente sul metodo a singolo APK.

---

## 4. Modified / Maintained Files Checklist
- `app/build.gradle`: Configurato con `applicationId "com.example.kinginstaller"` e la firma di debug predefinita, versione `v2.6` (versionCode 13).
- `app/src/main/AndroidManifest.xml`: Configurazione `FileProvider`, filtri Intent `ACTION_VIEW` e `ACTION_SEND` (incluse estensioni `.xapk`, `.apks`, `.apkm`), alias `.ApkHandler` e `.OppoTrick`, query manifest estese per Shizuku/Shizuku Next.
- `PackageArchiveUtils.kt`: Utility per l'analisi ed estrazione di archivi `.xapk`, `.apks`, `.apkm`, gestione OBB e classificazione `SplitType`.
- `InstallationUtils.kt`: Copia file con supporto estensioni split, pulizia cartelle temporanee, installazione `PackageInstaller.Session` con `PendingIntent.getActivity` per il metodo Classico (fix BAL Android 13+).
- `ShizukuUtils.kt`: Installazione via Shizuku + `setInstallerViaShizuku()` via `UserService` + `installSplitApks()`.
- `RootUtils.kt`: Esecuzione comandi `su` (`runSuWithCmd`), `setInstallerViaRoot()`, `launchInstallerAsShell()` e `installSplitApksAsShell()` con passaggio diretto del percorso assoluto del file (evitando `EPIPE`).
- `MyUserService.kt` / `IMyUserService.aidl`: `UserService` remoto che esegue `set-installer` con privilegi Shell/Root.
- `MainActivity.kt`: Selezione metodo, `packageAddedReceiver` con verifica AA, policy versione Android, gestione estrazione/installazione split vs singolo APK e dialogo personalizzato di selezione split.
- `AppManagerActivity.kt`: Verifica e reinstallazione di app singole e split (tramite compattazione `.apks`).
- `UpdateChecker.kt`: Controllo aggiornamenti GitHub, download diretto APK e avvio installazione immediato.
- `KingInstaller.md` & `CHANGELOG.md` & `CLAUDE.md`: Memoria di progetto, changelog e linee guida operative.