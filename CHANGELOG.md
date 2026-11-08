# Changelog

## v2.6

### 🇺🇸 English

**New**
- **Support for XAPK, APKS, and APKM files** 📦 — open, extract, and install split APK bundles (.xapk, .apks, .apkm) and OBB expansion files across all methods (Classic, Shizuku, Root), with smart pre-selection of the right ABI / screen density / language and a one-tap "Select all". Split apps can also be reinstalled from the in-app installer checker. *Note: split apps can't be made Android Auto–visible (a multi-APK bundle can't get the Play Store "Requested by" source); a dialog explains this, with a "don't show again" option.*
- **Direct in-app update** 🔄 — KingInstaller downloads its own update APK and launches the install in one tap.
- **XLauncher shortcut** 🚗 — a new "Universal AA unlock (XLauncher)" entry in the ⋮ menu; and if the Root method can't make an app Android Auto–visible, KingInstaller suggests XLauncher Unlocked (an Xposed/LSPosed module that unlocks every AA app regardless of installer).

**Improvements**
- **Android 13+ robustness** — several fixes so the system installer opens reliably on Android 13+ (dynamic binder signature resolution, synchronous Shizuku UserService connection with a safe fallback, Background-Activity-Launch workaround, correct installer resolution). Shizuku-Next compatibility.
- After a non-compatible install on a **rooted device**, the **Root method** is now offered too (not only Shizuku/Classic), and the Classic-failed prompt appears on rooted devices even without Shizuku.
- Update/reinstall of an already-installed app now shows the result feedback (ACTION_PACKAGE_REPLACED).
- The diagnostic log is also auto-saved to the public Downloads folder.
- Docs updated with the Xiaomi/POCO/HyperOS Developer-Options steps required for Shizuku.

**Removed / Fixed**
- Removed the **post-install installer fix**: on modern Android the `set-installer` is blocked by a signature check for every uid, so it never worked — the correct installer is set at install time instead.
- Removed the unneeded MANAGE_EXTERNAL_STORAGE prompt (content:// URIs + the system file picker are used instead) and trimmed superfluous install-intent extras (fewer conflicts on custom ROMs like MIUI).
- Fixed an EPIPE (broken pipe) error during root split installs and a Shizuku UserService crash (ClassCastException).

### 🇮🇹 Italiano

**Novità**
- **Supporto per file XAPK, APKS e APKM** 📦 — apri, estrai e installa pacchetti split (.xapk, .apks, .apkm) e file di espansione OBB su tutti i metodi (Classico, Shizuku, Root), con pre-selezione intelligente di ABI / densità schermo / lingua e un pulsante "Seleziona tutto". Le app split si possono anche reinstallare dalla schermata di verifica installatore. *Nota: le app split non possono essere rese visibili in Android Auto (un bundle multi-APK non può ottenere l'origine "Requested by" del Play Store); un dialogo lo spiega, con opzione "non mostrare più".*
- **Aggiornamento diretto in-app** 🔄 — KingInstaller scarica l'APK di aggiornamento e avvia l'installazione con un tocco.
- **Scorciatoia XLauncher** 🚗 — nuova voce "Sblocco universale AA (XLauncher)" nel menu ⋮; e se il metodo Root non riesce a rendere un'app visibile in Android Auto, KingInstaller propone XLauncher Unlocked (modulo Xposed/LSPosed che sblocca tutte le app AA a prescindere dall'installer).

**Miglioramenti**
- **Robustezza Android 13+** — vari fix perché l'installer di sistema si apra in modo affidabile su Android 13+ (risoluzione dinamica delle firme del binder, connessione sincrona del UserService Shizuku con fallback sicuro, aggiramento del Background Activity Launch, corretta risoluzione dell'installer). Compatibilità con Shizuku-Next.
- Dopo un'installazione non compatibile su **device rootato**, ora viene proposto anche il **Metodo Root** (non solo Shizuku/Classico), e il prompt del Classico-fallito compare sui device rootati anche senza Shizuku.
- L'aggiornamento/reinstallazione di un'app già presente ora mostra il feedback d'esito (ACTION_PACKAGE_REPLACED).
- Il log diagnostico viene salvato automaticamente anche nella cartella pubblica Download.
- Documentazione aggiornata con i passi delle Opzioni Sviluppatore Xiaomi/POCO/HyperOS necessari per Shizuku.

**Rimosso / Risolto**
- Rimosso il **fix installer post-installazione**: su Android moderno il `set-installer` è bloccato da un controllo di firma per ogni uid, quindi non funzionava — l'installer corretto viene impostato al momento dell'installazione.
- Rimosso il prompt inutile per MANAGE_EXTERNAL_STORAGE (si usano URI content:// + il file picker di sistema) e ripuliti gli extra superflui degli intent d'installazione (meno conflitti su ROM custom come MIUI).
- Risolto un errore EPIPE (broken pipe) nelle installazioni split via root e un crash del UserService Shizuku (ClassCastException).

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
