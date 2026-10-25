# CLAUDE.md — Istruzioni operative per KingInstaller

> Specifiche tecniche complete e ragionamenti: vedi **`KingInstaller.md`** (file di memoria del progetto).
> Questo file contiene solo il *come lavorare* e le invarianti da non rompere.

## Workflow (sempre)
- **Un commit per ogni fix diverso.** Niente commit "misti": ogni correzione/feature logicamente distinta ha il suo commit con messaggio chiaro (in italiano).
- **Aggiorna `KingInstaller.md`** quando cambi comportamento, architettura o aggiungi un fix: è la memoria viva del progetto.
- Compila prima di committare: `./gradlew :app:compileDebugKotlin --offline` (deve uscire con codice 0). Per le stringhe valida anche `:app:processDebugResources --offline`.
- Rispondi e commenta il codice in **italiano**.
- **App multilingue: ogni nuova stringa va tradotta in TUTTE le lingue.** File: `values/` (EN, default), `values-it`, `values-de`, `values-es`, `values-fr`, `values-ru`, `values-hu`, `values-hi`, `values-pt`, `values-ar`, `values-zh-rCN`. Lascia invariati i termini tecnici (Shizuku, Root, KingInstaller, logcat) ed escludi l'apostrofo in francese con `\'`.

## Vincoli di progetto (NON modificare)
- **Package name**: `com.example.kinginstaller` resta invariato (compatibilità firma/aggiornamenti). Idem `namespace` e firma di debug.
- **IPC Shizuku**: usare sempre `UserService` + Binder IPC (AIDL). Mai `Shizuku.newProcess()` o API remote deprecate (null su One UI/HyperOS).
- **Regola d'oro Android Auto**: "Installed by" (`installingPackageName`) **deve** essere `com.android.vending`; "Requested by" (`initiatingPackageName`) non deve mai essere `com.android.shell`. Vedi `KingInstaller.md` §1.

## Invarianti UI installer (NON regredire)
- Il **finto installer di King** (`.OppoTrick`, schema `package`) è visibile **solo** durante l'installazione in **modalità Oppo**.
- Durante "Installa con King", l'alias `.ApkHandler` è **disabilitato** (King non deve comparire nel chooser dell'installer di sistema). Gestito da `updateComponentStates(installing)`.
- Aprendo un APK **dall'esterno** (file manager / "condividi"), deve restare possibile **aprire con King** e caricare l'APK pronto: `.ApkHandler` abilitato a riposo e riabilitato in `onResume`.

## Policy per versione Android
- **Android 15+ (API 35)**: nascondi lo switch Oppo/Realme/OnePlus; non mostrare il suggerimento basato sul package installer rilevato.
- **Fino ad Android 14 (API 34)**: comportamento legacy (suggerimento `isGooglePackageExist` visibile).

## Fix Xiaomi / MIUI / HyperOS (riassunto)
MIUI ignora gli extra dell'intent, quindi la correzione dell'installer avviene **dopo** l'installazione con comando privilegiato (`set-installer` via Shizuku `UserService` o Root), lanciato **reattivamente** dal polling di `packageAddedReceiver` solo se l'app non è già `isAACompatible`. Dettagli in `KingInstaller.md` §1.4.
