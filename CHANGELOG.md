# Changelog

## v2.6

### 🇺🇸 English

**New**
- **Support for XAPK, APKS, and APKM files** 📦 — KingInstaller can now open, extract, and install split APK bundles (.xapk, .apks, .apkm) and expansion files (.obb) across all installation methods (Classic, Shizuku, Root) without affecting single APK installations.

### 🇮🇹 Italiano

**Novità**
- **Supporto per file XAPK, APKS e APKM** 📦 — KingInstaller ora può aprire, estrarre e installare pacchetti split (.xapk, .apks, .apkm) e file di espansione (.obb) su tutti i metodi di installazione (Classico, Shizuku, Root), mantenendo inalterata l'installazione dei singoli file APK.

## v2.5

### 🇺🇸 English

**New**
- **One-tap log copy** 📋 — tap the status/log box to copy its contents to the clipboard (besides the share icon), so you can paste the install diagnostics easily.

**Fixes & Improvements**
- **Root method reworked.** It now installs by launching the system package installer **as a privileged shell** (via `su`), so **"Installed by" = Play Store** *and* **"Requested by" = Package Installer** are set in a single step — the same technique as the Shizuku method, but without Shizuku. Ideal where the vendor blocks Shizuku (e.g. Xiaomi, which uses the Google installer but prevents Shizuku from starting).
- **Why the old Root approach couldn't work:** the post-install `set-installer` is rejected for **every uid** by a signature check (`Caller does not have same cert as new installer package com.android.vending`; root gets `Unknown calling UID: 0`), so the installer must be set **at install time**.
- **Android Auto 14+ also checks "Requested by"** (not only "Installed by"); this is why `pm install -i` alone isn't enough and the system installer must do the install.
- **Cleaner result screen:** on failure it shows a clear message plus the real **Installed by / Requested by** values; verbose diagnostics (`su`/`am` output) now go only to the shared log.

### 🇮🇹 Italiano

**Novità**
- **Copia log con un tocco** 📋 — tocca il riquadro di stato/log per copiarne il contenuto negli appunti (oltre all'icona di condivisione), così incolli al volo la diagnostica dell'installazione.

**Correzioni e miglioramenti**
- **Metodo Root rifatto.** Ora installa lanciando il package installer di sistema **come shell privilegiata** (via `su`), così **"Installed by" = Play Store** *e* **"Requested by" = Package Installer** vengono impostati in un colpo solo — la stessa tecnica del metodo Shizuku, ma senza Shizuku. Ideale dove il produttore blocca Shizuku (es. Xiaomi, che usa l'installer Google ma impedisce l'avvio di Shizuku).
- **Perché il vecchio Root non poteva funzionare:** il `set-installer` post-installazione è rifiutato per **ogni uid** da un controllo di firma (`Caller does not have same cert as new installer package com.android.vending`; da root `Unknown calling UID: 0`), quindi l'installer va impostato **al momento dell'installazione**.
- **Android Auto 14+ controlla anche "Requested by"** (non solo "Installed by"): per questo `pm install -i` da solo non basta ed è il package installer di sistema a dover eseguire l'installazione.
- **Schermata di esito più pulita:** in caso di fallimento mostra un messaggio chiaro più i valori reali di **Installed by / Requested by**; i dettagli verbosi (output di `su`/`am`) finiscono solo nel log condiviso.

## v2.4

### 🇺🇸 English

**New**
- **Share installation logs** 📋 — a new share icon next to the title generates a full diagnostic report (device, ROM, install source, logcat) you can send for support. This helps gather data from unsupported phones. Note: on devices where the installer spoofing doesn't work, it will likely be impossible to fix without **Root**.
- **Suggested fallback** — when the Shizuku method can't make an app Android Auto–compatible, KingInstaller now suggests trying the **Classic** method first, then **Root**.
- **Clearer feedback** — during the post-install installer fix the UI shows the status, and on failure it shows the real "Installed by / Requested by" values.

**Improvements**
- More reliable post-install installer fix via the system binder (works where the OS allows it; otherwise Root is required).
- The old Oppo/Realme/OnePlus switch is hidden on Android 15+.
- The package-installer hint on the home screen is shown only up to Android 14.
- Updated README: where Shizuku works (Pixel, Honor, Realme, RedMagic…) vs where Root is required (Xiaomi/POCO/Redmi on MIUI/HyperOS; Vivo reported but unconfirmed).

### 🇮🇹 Italiano

**Novità**
- **Condivisione dei log di installazione** 📋 — una nuova icona di condivisione accanto al titolo genera un report diagnostico completo (dispositivo, ROM, origine installazione, logcat) da inviare per il supporto. Serve a raccogliere dati dagli smartphone non supportati. Nota: sui dispositivi dove lo spoofing dell'installer non funziona, sarà probabilmente impossibile sistemarlo senza **Root**.
- **Suggerimento di fallback** — quando il metodo Shizuku non riesce a rendere un'app compatibile con Android Auto, KingInstaller suggerisce di provare prima il metodo **Classico** e poi il **Root**.
- **Feedback più chiaro** — durante la correzione dell'installer la UI mostra lo stato e, in caso di errore, i valori reali di "Installed by / Requested by".

**Miglioramenti**
- Correzione dell'installer post-installazione più affidabile tramite il binder di sistema (dove il sistema lo consente; altrimenti serve Root).
- Lo switch legacy Oppo/Realme/OnePlus è nascosto su Android 15+.
- Il suggerimento sul package installer in home è mostrato solo fino ad Android 14.
- README aggiornato: dove Shizuku funziona (Pixel, Honor, Realme, RedMagic…) e dove serve Root (Xiaomi/POCO/Redmi con MIUI/HyperOS; Vivo segnalato ma non confermato).
