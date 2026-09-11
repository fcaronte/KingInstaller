---

[🇺🇸 English](https://www.google.com/search?q=%23-english) | [🇮🇹 Italiano](https://www.google.com/search?q=%23-italiano)

---

# KingInstaller (English)

Install packages "as Google Play Store" to work around restrictions! Useful for Android Auto.

---

## 🚀 What is KingInstaller?

KingInstaller is a utility designed to install APK files in a way that tricks the system (and Android Auto) into thinking the application originated directly from the official Google Play Store, helping bypass specific app visibility restrictions.

---

## ✨ Features & Recent Updates (v1.9)

* **Hybrid Installation Architecture:** Combines direct intents, Shizuku remote `UserService` (supporting **Standard Shizuku**, **ShizukuPlus**, and **Plus Drop-In**), and Root execution.
* **Smart Workflow:** Streamlined installation flow recommending the classic method first, falling back to Shizuku, and utilizing Root as the ultimate solution when needed.
* **Auto-Fixer:** Automatically repairs the installer identity immediately after installation if Shizuku or Root is available.
* **Material Design 3 UI:** Modernized interface embracing clean layouts and Material 3 guidelines.
* **App Diagnostic Checker:** Built-in tool to inspect how the system perceives an installed application.
* **Android Auto Settings Shortcut:** Quick shortcut button to jump straight into AA settings.

---

## ⚠️ Compatibility & Recommended Installation Workflow

To get the best results across different devices and Android versions, follow this progressive approach:

### 1️⃣ Step 1: The Classic Method (Recommended First)

* **How to use:** Just select your APK and hit **Install** normally (without enabling any switches).
* On many stock devices and custom ROMs (especially Android 10–16), the system installer handles the spoofing flags automatically. Always try this first.

### 2️⃣ Step 2: The Shizuku Trick (If Step 1 fails)

* **When to use:** If the classic installation doesn't flag the app as coming from the Play Store or Android Auto rejects it.
* **How to use:** Start your preferred Shizuku implementation (**Standard**, **ShizukuPlus**, or **Plus Drop-In**), enable the **Shizuku Trick** switch in KingInstaller, and try again. KingInstaller will use a secure remote `UserService` via IPC binding to force the correct installer metadata.
* **Xiaomi / POCO / Redmi Note:** On recent Xiaomi, POCO, and Redmi devices running MIUI or HyperOS, deep vendor restrictions block standard installation hooks and Shizuku methods. On these devices, **Shizuku usually fails**, and **Root access is currently the only reliable method** to achieve successful installation spoofing.

### 3️⃣ Step 3: The Root Method (Ultimate Fallback)

* **When to use:** If both the classic method and Shizuku fail due to heavy vendor security restrictions (mandatory for Xiaomi / POCO / Redmi).
* **How to use:** Enable the **Root Trick** switch. This grants KingInstaller direct root-level execution via `su` to manipulate the package manager service directly.

---

## 🚗 Android Auto Rules & Insights

Based on extensive testing:

* **The Golden Rule:** For an app to work in Android Auto, the **"Requested by"** field should ideally be the Package Installer, while the **"Installed by"** field **MUST be the Play Store**.
* **Experimental Tweaks:** Due to the wide variety of custom ROMs and security patches across manufacturers, certain installation flags or behavior may require a trial-and-error approach.

---

## ☕ Support my work

If KingInstaller helped you, consider supporting the project:
**[Donate via PayPal](https://www.paypal.com/paypalme/FCaronte/2)**

---

# KingInstaller (Italiano)

Installa pacchetti "come Google Play Store" per aggirare le restrizioni! Utile per Android Auto.

---

## 🚀 Cos'è KingInstaller?

KingInstaller è un'utilità progettata per installare file APK ingannando il sistema (e Android Auto) facendogli credere che l'applicazione provenga direttamente dal Google Play Store ufficiale, aiutando ad aggirare le specifiche restrizioni di visibilità delle app.

---

## ✨ Funzionalità e Ulteriori Aggiornamenti (v1.9)

* **Architettura di Installazione Ibrida:** Combina intent diretti, `UserService` remoto di Shizuku (con supporto a **Shizuku Standard**, **ShizukuPlus** e **Plus Drop-In**) ed esecuzione tramite Root.
* **Flusso Intelligente:** Processo di installazione ottimizzato che consiglia prima il metodo classico, passando a Shizuku in caso di problemi e riservando il Root come soluzione definitiva.
* **Auto-Fixer:** Ripara automaticamente l'identità dell'installer subito dopo l'installazione se Shizuku o il Root sono disponibili.
* **Interfaccia Material Design 3:** Design moderno basato su linee pulite e linee guida Material 3.
* **Diagnostica App Integrata:** Strumento integrato per verificare come il sistema percepisce l'applicazione installata.
* **Scorciatoia Impostazioni Android Auto:** Pulsante rapido per accedere direttamente alle impostazioni di AA.

---

## ⚠️ Compatibilità e Flusso di Installazione Consigliato

Per ottenere i risultati migliori su diversi dispositivi e versioni Android, segui questo approccio progressivo:

### 1️⃣ Passo 1: Il Metodo Classico (Consigliato per primo)

* **Come usarlo:** Seleziona semplicemente il tuo APK e premi **Installa** normalmente (senza attivare alcunché).
* Su molti dispositivi stock e custom ROM (specialmente Android 10–16), il programma di installazione gestisce i flag di spoofing automaticamente. Prova sempre prima questo.

### 2️⃣ Passo 2: Il Trucco Shizuku (Se il Passo 1 fallisce)

* **Quando usarlo:** Se l'installazione classica non contrassegna l'app come proveniente dal Play Store o se Android Auto la rifiuta.
* **Come usarlo:** Avvia la tua implementazione di Shizuku preferita (**Standard**, **ShizukuPlus** o **Plus Drop-In**), attiva l'interruttore **Shizuku** in KingInstaller e riprova. KingInstaller utilizzerà un `UserService` sicuro tramite IPC binding per forzare i metadati corretti dell'installer.
* **Nota per Xiaomi / POCO / Redmi:** Sui dispositivi recenti della famiglia Xiaomi, POCO e Redmi con MIUI o HyperOS, le rigide personalizzazioni del produttore bloccano i metodi di installazione alternativi. Su questi dispositivi **il metodo Shizuku non funziona**, rendendo i **permessi di Root** l'unico metodo realmente funzionante.

### 3️⃣ Passo 3: Il Metodo Root (Soluzione di riserva estrema)

* **Quando usarlo:** Se sia il metodo classico che Shizuku falliscono (obbligatorio su dispositivi Xiaomi / POCO / Redmi).
* **Come usarlo:** Attiva l'interruttore **Root**. Questo concederà a KingInstaller l'esecuzione diretta con privilegi di root tramite `su` per manipolare direttamente il servizio di package manager.

---

## 🚗 Regole e Consigli per Android Auto

Basato su test approfonditi:

* **La Regola d'Oro:** Affinché un'app funzioni in Android Auto, il campo **"Requested by"** (Richiesto da) dovrebbe idealmente essere il Package Installer, mentre il campo **"Installed by"** (Installato da) **DEVE essere il Play Store**.
* **Modifiche Sperimentali:** A causa della grande varietà di custom ROM e patch di sicurezza tra i vari produttori, alcuni flag d'installazione potrebbero richiedere un approccio per tentativi ed errori.

---

## ☕ Supporta il mio lavoro

If KingInstaller ti è stato utile, considera l'idea di supportare il progetto:
**[Dona via PayPal](https://www.paypal.com/paypalme/FCaronte/2)**

---

## 📝 Note e Limitazioni

* Assicurati di abilitare le **Origini Sconosciute** nelle impostazioni sviluppatore di Android Auto.
* **Dispositivi Xiaomi / POCO / Redmi:** Su questi dispositivi (MIUI/HyperOS), i metodi standard e Shizuku non sono supportati dal sistema e falliscono regolarmente; è richiesto l'uso dei permessi di **Root**.
* Alcune app presentano restrizioni cablate. Prendi in considerazione l'utilizzo di moduli Xposed se KingInstaller da solo non dovesse bastare.
