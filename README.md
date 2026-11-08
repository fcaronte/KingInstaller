---

[🇺🇸 English](https://www.google.com/search?q=%23-english) | [🇮🇹 Italiano](https://www.google.com/search?q=%23-italiano)

---

# KingInstaller (English)

Install packages "as Google Play Store" to work around restrictions! Useful for Android Auto.

---

## 🚀 What is KingInstaller?

KingInstaller is a utility designed to install APK files in a way that tricks the system (and Android Auto) into thinking the application originated directly from the official Google Play Store, helping bypass specific app visibility restrictions.

---

## ✨ Features & Recent Updates (v2.6)

* **Split APKs Support (.xapk, .apks, .apkm):** Native support for extracting, selecting, and installing multi-part APK bundles and OBB expansion files.
* **Smart Split Selection:** Automatically pre-selects device-compatible CPU architectures (ABIs), display densities (including all DPI resources like `mdpi`), and system language (`it`, `en`), deselecting unnecessary languages. Includes a one-tap "Select All / Deselect All" toggle.
* **Hybrid Installation Architecture:** Combines direct intents, Shizuku remote `UserService` (supporting **Shizuku-Next**, **Standard Shizuku**, **ShizukuPlus**, and **Plus Drop-In**), and Root execution.
* **Smart Workflow:** Streamlined installation flow recommending the classic method first, falling back to Shizuku, and utilizing Root as the ultimate solution when needed.
* **App List Reinstaller:** Reinstall single and split apps directly from the in-app package checker list.
* **In-App Direct Update:** Automatically downloads update APKs directly in-app and launches installation in a single tap.
* **Material Design 3 UI:** Modernized interface embracing clean layouts and Material 3 guidelines.
* **🌍 Multilingual Experience:** KingInstaller supports 11 languages and automatically adapts to the user's preferred system language.
* **App Diagnostic Checker:** Built-in tool to inspect how the system perceives an installed application.
* **Diagnostic Log Sharing & Export:** Share a full diagnostic report (device, ROM, install source, logcat) or automatically save a copy to the public `Downloads` folder for easy retrieval.
* **One-Tap Log Copy:** Tap the status/log box to copy its contents to the clipboard.
* **Android Auto Settings Shortcut:** Quick shortcut button to jump straight into AA settings.

---

## ⚠️ Compatibility & Recommended Installation Workflow

To get the best results across different devices and Android versions, follow this progressive approach:

### 1️⃣ Step 1: The Classic Method (Recommended First)

* **How to use:** Just select your APK and hit **Install** normally (without enabling any switches).
* On many stock devices and custom ROMs (especially Android 10–16), the system installer handles the spoofing flags automatically. Always try this first.

### 2️⃣ Step 2: The Shizuku Trick (If Step 1 fails)

* **When to use:** If the classic installation doesn't flag the app as coming from the Play Store or Android Auto rejects it.
* **Recommended Shizuku:** Use **[Shizuku-Next](https://github.com/rushiranpise/Shizuku-Next)** by [rushiranpise](https://github.com/rushiranpise). This fork makes daily use much easier because it can **start automatically even without Wi‑Fi**.
* **How to use:** Start **Shizuku-Next** (or another compatible Shizuku implementation such as Standard, ShizukuPlus, or Plus Drop-In), enable the **Shizuku Trick** switch in KingInstaller, and try again.
* **Where Shizuku works:** On Pixel (including Android 17), Honor, Realme, RedMagic, Xiaomi / POCO / Redmi (MIUI/HyperOS), and many other devices.
* **Xiaomi / POCO / Redmi (MIUI/HyperOS):** Confirmed working (e.g. POCO F6 on Android 16)! To use Shizuku successfully on Xiaomi/MIUI/HyperOS devices, you **MUST enable the following two settings in Developer Options**:
  1. **Install via USB**
  2. **USB debugging (Security settings)**
  *(As recommended by the Shizuku app itself).*

### 3️⃣ Step 3: The Root Method (Ultimate Fallback)

* **When to use:** If both the classic method and Shizuku fail due to heavy vendor security restrictions.
* **How to use:** Enable the **Root Trick** switch and install. KingInstaller uses `su` to launch the system package installer **as a privileged shell**, so Play Store is recorded as the installer *and* the Package Installer stays the initiator.

---

## 🚗 Android Auto Rules & Insights

Based on extensive testing:

* **The Golden Rule:** For an app to work in Android Auto, the **"Installed by"** field **MUST be the Play Store** (`com.android.vending`). On **Android Auto 14+** the **"Requested by"** field is enforced too and must be the **Package Installer** (never shell/empty). KingInstaller's Root and Shizuku methods set both correctly.

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

## ✨ Funzionalità e Ulteriori Aggiornamenti (v2.6)

* **Supporto File Split APK (.xapk, .apks, .apkm):** Supporto nativo per estrarre, selezionare e installare pacchetti multi-parte e file di espansione OBB.
* **Selezione Intelligente degli Split:** Pre-seleziona automaticamente l'architettura CPU compatibile (`ABI`), la densità dello schermo (`DENSITY`, inclusi gli split DPI come `mdpi`) e la lingua di sistema (`it`, `en`), deselezionando le lingue inutili. Include un pulsante "Seleziona tutto / Deseleziona tutto".
* **Architettura di Installazione Ibrida:** Combina intent diretti, `UserService` remoto di Shizuku (con supporto a **Shizuku-Next**, **Shizuku Standard**, **ShizukuPlus** e **Plus Drop-In**) ed esecuzione tramite Root.
* **Flusso Intelligente:** Processo di installazione ottimizzato che consiglia prima il metodo classico, passando a Shizuku o Root in caso di problemi.
* **Reinstallazione App dalla Lista:** Reinstalla app singole o split direttamente dalla schermata di verifica dell'installatore.
* **Aggiornamento Diretto In-App:** Scarica direttamente gli APK di aggiornamento da GitHub e avvia l'installazione in un solo tap.
* **Interfaccia Material Design 3:** Design moderno basato su linee pulite e linee guida Material 3.
* **🌍 Esperienza Multilingua:** KingInstaller supporta 11 lingue e adatta automaticamente l'interfaccia alla lingua preferita del sistema.
* **Diagnostica App Integrata:** Strumento integrato per verificare come il sistema percepisce l'applicazione installata.
* **Condivisione e Salvataggio Log Diagnostico:** Condividi un report completo o salvalo automaticamente nella cartella pubblica `Download` per un facile recupero.
* **Copia Log con un Tocco:** Tocca il riquadro di stato/log per copiarne il contenuto negli appunti.
* **Scorciatoia Impostazioni Android Auto:** Pulsante rapido per accedere direttamente alle impostazioni di AA.

---

## ⚠️ Compatibilità e Flusso di Installazione Consigliato

Per ottenere i risultati migliori su diversi dispositivi e versioni Android, segui questo approccio progressivo:

### 1️⃣ Passo 1: Il Metodo Classico (Consigliato per primo)

* **Come usarlo:** Seleziona semplicemente il tuo APK e premi **Installa** normalmente (senza attivare alcunché).
* Su molti dispositivi stock e custom ROM (specialmente Android 10–16), il programma di installazione gestisce i flag di spoofing automaticamente. Prova sempre prima questo.

### 2️⃣ Passo 2: Il Trucco Shizuku (Se il Passo 1 fallisce)

* **Quando usarlo:** Se l'installazione classica non contrassegna l'app come proveniente dal Play Store o se Android Auto la rifiuta.
* **Shizuku consigliato:** Usa **[Shizuku-Next](https://github.com/rushiranpise/Shizuku-Next)** di [rushiranpise](https://github.com/rushiranpise). Questo fork rende la vita più facile perché supporta **l'avvio automatico anche senza Wi‑Fi**.
* **Come usarlo:** Avvia **Shizuku-Next** (oppure un'altra implementazione compatibile come Standard, ShizukuPlus o Plus Drop-In), attiva l'interruttore **Shizuku** in KingInstaller e riprova.
* **Dove Shizuku funziona:** Su Pixel (anche Android 17), Honor, Realme, RedMagic, Xiaomi / POCO / Redmi (MIUI/HyperOS) e molti altri dispositivi.
* **Xiaomi / POCO / Redmi (MIUI/HyperOS):** Confermato e funzionante (es. POCO F6 con Android 16)! Per far funzionare Shizuku su dispositivi Xiaomi/MIUI/HyperOS **è NESSESSARIO abilitare queste due opzioni nelle Opzioni Sviluppatore**:
  1. **Installa tramite USB**
  2. **Debug USB (Impostazioni di sicurezza)**
  *(Come suggerito anche dall'app Shizuku stessa).*

### 3️⃣ Passo 3: Il Metodo Root (Soluzione di riserva estrema)

* **Quando usarlo:** Se sia il metodo classico che Shizuku falliscono per restrizioni estreme del produttore.
* **Come usarlo:** Attiva l'interruttore **Root** e installa. KingInstaller usa `su` per lanciare il package installer di sistema **come shell privilegiata**, così il Play Store viene registrato come installer *e* il Package Installer resta l'iniziatore.

---

## 🚗 Regole e Consigli per Android Auto

Basato su test approfonditi:

* **La Regola d'Oro:** Affinché un'app funzioni in Android Auto, il campo **"Installed by"** (Installato da) **DEVE essere il Play Store** (`com.android.vending`). Su **Android Auto 14+** viene controllato **anche** il campo **"Requested by"** (Richiesto da), che deve essere il **Package Installer** (mai shell/vuoto). I metodi Root e Shizuku di KingInstaller impostano correttamente entrambi.

---

## ☕ Supporta il mio lavoro

Se KingInstaller ti è stato utile, considera l'idea di supportare il progetto:
* **[Dona via PayPal](https://www.paypal.com/paypalme/FCaronte/4)**
* **[Offrimi un caffè (Buy Me a Coffee)](http://buymeacoffee.com/fcaronte)**
