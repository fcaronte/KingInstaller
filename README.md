---

[🇺🇸 English](https://www.google.com/search?q=%23-english) | [🇮🇹 Italiano](https://www.google.com/search?q=%23-italiano)

---

# KingInstaller (English)

Install packages "as Google Play Store" to work around restrictions! Useful for Android Auto.

---

## 🚀 What is KingInstaller?

KingInstaller is a utility designed to install APK files in a way that tricks the system (and Android Auto) into thinking the application originated directly from the official Google Play Store, helping bypass specific app visibility restrictions.

---

## ✨ Features & Recent Updates

* **Hybrid Installation Architecture:** Combines direct intents, Shizuku remote `UserService` (supporting **Shizuku-Next**, **Standard Shizuku**, **ShizukuPlus**, and **Plus Drop-In**), and Root execution.
* **Smart Workflow:** Streamlined installation flow recommending the classic method first, falling back to Shizuku, and utilizing Root as the ultimate solution when needed.
* **Auto-Fixer:** Ensures the correct installer identity — the **Root** method sets it *at install time* by launching the system package installer as a privileged shell, while **Shizuku** repairs it right after install via the system binder.
* **Material Design 3 UI:** Modernized interface embracing clean layouts and Material 3 guidelines.
* **🌍 Multilingual Experience:** KingInstaller supports multiple languages and automatically adapts the interface to the user's preferred system language for a more intuitive experience.
* **App Diagnostic Checker:** Built-in tool to inspect how the system perceives an installed application.
* **Diagnostic Log Sharing:** Share a full diagnostic report (device, ROM, install source, logcat) via the share icon next to the title — handy for troubleshooting and support.
* **One-Tap Log Copy:** Tap the status/log box to copy its contents to the clipboard (in addition to the share icon) — makes it easy to paste the install diagnostics.
* **Android Auto Settings Shortcut:** Quick shortcut button to jump straight into AA settings.

---

## ⚠️ Compatibility & Recommended Installation Workflow

To get the best results across different devices and Android versions, follow this progressive approach:

### 1️⃣ Step 1: The Classic Method (Recommended First)

* **How to use:** Just select your APK and hit **Install** normally (without enabling any switches).
* On many stock devices and custom ROMs (especially Android 10–16), the system installer handles the spoofing flags automatically. Always try this first.

### 2️⃣ Step 2: The Shizuku Trick (If Step 1 fails)

* **When to use:** If the classic installation doesn't flag the app as coming from the Play Store or Android Auto rejects it.
* **Recommended Shizuku:** Use **[Shizuku-Next](https://github.com/rushiranpise/Shizuku-Next)** by [rushiranpise](https://github.com/rushiranpise). This fork makes daily use much easier because it can **start automatically even without Wi‑Fi**. Official Shizuku on unrooted devices usually depends on Wireless debugging, so it often needs a network connection after each reboot.
* **How to use:** Start **Shizuku-Next** (or another compatible Shizuku implementation such as Standard, ShizukuPlus, or Plus Drop-In), enable the **Shizuku Trick** switch in KingInstaller, and try again. KingInstaller will use a secure remote `UserService` via IPC binding to force the correct installer metadata.
* **Where Shizuku works:** On Pixel (including Android 17), Honor, Realme, RedMagic and many other devices the Shizuku trick works well — the install dialog honors the Play Store metadata directly.
* **Xiaomi / POCO / Redmi (MIUI/HyperOS):** these vendors either ignore the installer metadata at install time, or block a non-root shell from setting it afterwards (Android throws `SecurityException: Caller does not have same cert as new installer package com.android.vending`). On these devices, **Shizuku without root cannot set the Play Store installer** — **Root is the only reliable method** (or start Shizuku *with* root). KingInstaller detects this and suggests trying the Classic method first, then Root. *(Vivo has also been reported as problematic, but this is unconfirmed — it may still work.)*

### 3️⃣ Step 3: The Root Method (Ultimate Fallback)

* **When to use:** If both the classic method and Shizuku fail due to heavy vendor security restrictions (mandatory for Xiaomi / POCO / Redmi on MIUI/HyperOS; Vivo reported but unconfirmed).
* **How to use:** Enable the **Root Trick** switch and install. KingInstaller uses `su` to launch the system package installer **as a privileged shell**, so Play Store is recorded as the installer *and* the Package Installer stays the initiator — the same end result as the Shizuku method, but without Shizuku (ideal where the vendor blocks Shizuku, e.g. Xiaomi).

---

## 🚗 Android Auto Rules & Insights

Based on extensive testing:

* **The Golden Rule:** For an app to work in Android Auto, the **"Installed by"** field **MUST be the Play Store** (`com.android.vending`). On **Android Auto 14+** the **"Requested by"** field is enforced too and must be the **Package Installer** (never shell/empty). KingInstaller's Root and Shizuku methods set both correctly.
* **Experimental Tweaks:** Due to the wide variety of custom ROMs and security patches across manufacturers, certain installation flags or behavior may require a trial-and-error approach.

---

## ☕ Support my work

If KingInstaller helped you, consider supporting the project:
* **[Donate via PayPal](https://www.paypal.com/paypalme/FCaronte/4)**
* **[Buy Me a Coffee](http://buymeacoffee.com/fcaronte)**

---

# KingInstaller (Italiano)

Installa pacchetti "come Google Play Store" per aggirare le restrizioni! Utile per Android Auto.

---

## 🚀 Cos'è KingInstaller?

KingInstaller è un'utilità progettata per installare file APK ingannando il sistema (e Android Auto) facendogli credere che l'applicazione provenga direttamente dal Google Play Store ufficiale, aiutando ad aggirare le specifiche restrizioni di visibilità delle app.

---

## ✨ Funzionalità e Ulteriori Aggiornamenti (v2.5)

* **Architettura di Installazione Ibrida:** Combina intent diretti, `UserService` remoto di Shizuku (con supporto a **Shizuku-Next**, **Shizuku Standard**, **ShizukuPlus** e **Plus Drop-In**) ed esecuzione tramite Root.
* **Flusso Intelligente:** Processo di installazione ottimizzato che consiglia prima il metodo classico, passando a Shizuku in caso di problemi e riservando il Root come soluzione definitiva.
* **Auto-Fixer:** Garantisce l'identità corretta dell'installer — il metodo **Root** la imposta *durante* l'installazione lanciando il package installer di sistema come shell privilegiata, mentre **Shizuku** la corregge subito dopo l'installazione tramite il binder di sistema.
* **Interfaccia Material Design 3:** Design moderno basato su linee pulite e linee guida Material 3.
* **🌍 Esperienza Multilingua:** KingInstaller supporta ora più lingue e adatta automaticamente l'interfaccia alla lingua preferita del sistema per un'esperienza più intuitiva.
* **Diagnostica App Integrata:** Strumento integrato per verificare come il sistema percepisce l'applicazione installata.
* **Condivisione Log Diagnostico:** Condividi un report completo (dispositivo, ROM, origine installazione, logcat) tramite l'icona di condivisione accanto al titolo — utile per diagnosi e supporto.
* **Copia Log con un Tocco:** Tocca il riquadro di stato/log per copiarne il contenuto negli appunti (oltre all'icona di condivisione) — comodo per incollare la diagnostica dell'installazione.
* **Scorciatoia Impostazioni Android Auto:** Pulsante rapido per accedere direttamente alle impostazioni di AA.

---

## ⚠️ Compatibilità e Flusso di Installazione Consigliato

Per ottenere i risultati migliori su diversi dispositivi e versioni Android, segui questo approccio progressivo:

### 1️⃣ Passo 1: Il Metodo Classico (Consigliato per primo)

* **Come usarlo:** Seleziona semplicemente il tuo APK e premi **Installa** normalmente (senza attivare alcunché).
* Su molti dispositivi stock e custom ROM (specialmente Android 10–16), il programma di installazione gestisce i flag di spoofing automaticamente. Prova sempre prima questo.

### 2️⃣ Passo 2: Il Trucco Shizuku (Se il Passo 1 fallisce)

* **Quando usarlo:** Se l'installazione classica non contrassegna l'app come proveniente dal Play Store o se Android Auto la rifiuta.
* **Shizuku consigliato:** Usa **[Shizuku-Next](https://github.com/rushiranpise/Shizuku-Next)** di [rushiranpise](https://github.com/rushiranpise). Questo fork rende la vita più facile perché supporta **l'avvio automatico anche senza Wi‑Fi**. Lo Shizuku ufficiale, sui dispositivi non root, di solito dipende dal Wireless debugging e dopo ogni riavvio spesso richiede una connessione di rete.
* **Come usarlo:** Avvia **Shizuku-Next** (oppure un'altra implementazione compatibile come Standard, ShizukuPlus o Plus Drop-In), attiva l'interruttore **Shizuku** in KingInstaller e riprova. KingInstaller utilizzerà un `UserService` sicuro tramite IPC binding per forzare i metadati corretti dell'installer.
* **Dove Shizuku funziona:** Su Pixel (anche Android 17), Honor, Realme, RedMagic e molti altri dispositivi il trucco Shizuku funziona bene — il dialog di installazione onora direttamente i metadati del Play Store.
* **Xiaomi / POCO / Redmi (MIUI/HyperOS):** questi produttori ignorano i metadati dell'installer al momento dell'installazione, oppure impediscono a uno shell non-root di impostarli dopo (Android restituisce `SecurityException: Caller does not have same cert as new installer package com.android.vending`). Su questi dispositivi **Shizuku senza root non può impostare l'installer Play Store**: serve il **Root** (oppure Shizuku avviato *con* permessi di root). KingInstaller lo rileva e suggerisce di provare prima il metodo Classico e poi il Root. *(Anche Vivo è stato segnalato come problematico, ma non è confermato — potrebbe comunque funzionare.)*

### 3️⃣ Passo 3: Il Metodo Root (Soluzione di riserva estrema)

* **Quando usarlo:** Se sia il metodo classico che Shizuku falliscono per restrizioni del produttore (obbligatorio su Xiaomi / POCO / Redmi con MIUI/HyperOS; Vivo segnalato ma non confermato).
* **Come usarlo:** Attiva l'interruttore **Root** e installa. KingInstaller usa `su` per lanciare il package installer di sistema **come shell privilegiata**, così il Play Store viene registrato come installer *e* il Package Installer resta l'iniziatore — lo stesso risultato del metodo Shizuku, ma senza Shizuku (ideale dove il produttore blocca Shizuku, es. Xiaomi).

---

## 🚗 Regole e Consigli per Android Auto

Basato su test approfonditi:

* **La Regola d'Oro:** Affinché un'app funzioni in Android Auto, il campo **"Installed by"** (Installato da) **DEVE essere il Play Store** (`com.android.vending`). Su **Android Auto 14+** viene controllato **anche** il campo **"Requested by"** (Richiesto da), che deve essere il **Package Installer** (mai shell/vuoto). I metodi Root e Shizuku di KingInstaller impostano correttamente entrambi.
* **Modifiche Sperimentali:** A causa della grande varietà di custom ROM e patch di sicurezza tra i vari produttori, alcuni flag d'installazione potrebbero richiedere un approccio per tentativi ed errori.

---

## ☕ Supporta il mio lavoro

Se KingInstaller ti è stato utile, considera l'idea di supportare il progetto:
* **[Dona via PayPal](https://www.paypal.com/paypalme/FCaronte/4)**
* **[Offrimi un caffè (Buy Me a Coffee)](http://buymeacoffee.com/fcaronte)**

---

## 📝 Note e Limitazioni

* Assicurati di abilitare le **Origini Sconosciute** nelle impostazioni sviluppatore di Android Auto.
* **Dispositivi Xiaomi / POCO / Redmi (MIUI/HyperOS)** (e forse Vivo, segnalato ma non confermato): su questi dispositivi i metodi standard e Shizuku (senza root) non sono supportati dal sistema e falliscono regolarmente; è richiesto l'uso dei permessi di **Root**.
* Alcune app presentano restrizioni cablate. Prendi in considerazione l'utilizzo di moduli Xposed se KingInstaller da solo non dovesse bastare.
