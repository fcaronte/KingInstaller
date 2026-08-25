---

# KingInstaller

Install packages "as Google Play Store" to work around restrictions! Useful for Android Auto.

---

## 🚀 What is KingInstaller?

KingInstaller is a utility designed to install APK files in a way that tricks the system (and Android Auto) into thinking the application originated directly from the official Google Play Store, helping bypass specific app visibility restrictions.

---

## 📸 Screenshots

<p align="center">
  <img src="assets/1.png" width="200" />
  <img src="assets/2.png" width="200" />
  <img src="assets/3.png" width="200" />
  <img src="assets/4.png" width="200" />
</p>

---

## ✨ Features & Recent Updates

* **Material Design 3 UI:** Modernized interface embracing clean layouts and Material 3 guidelines.
* **Shizuku (ADB) Support:** Integrated native Shizuku execution to leverage ADB privileges without requiring traditional root overhead.
* **App Diagnostic Checker:** Built-in tool to inspect how the system perceives an installed application, giving an immediate estimation of whether it will appear and function inside Android Auto.
* **Android Auto Settings Shortcut:** Quick shortcut button to jump straight into the system's Android Auto settings page.
* **Direct APK Opening:** Open APK files directly with KingInstaller (via file managers or shares) for a faster installation flow.
* **Improved Root Detection:** Enhanced accuracy for root permission detection across different devices.
* **Package Installer Reinstallation Menu:** Re-added utility option to reinstall the stock Package Installer *(Note: while some users believe this helps with unlocking, my testing shows no noticeable change)*.

---

## ⚠️ Compatibility & Android Auto Requirements (Current Status)

### 📊 System Compatibility

* **Android 16 (A16):** Currently confirmed to work properly on Samsung and Pixel devices running A16.
* **Android 17 (A17):** Out-of-the-box installation **does not work** on Pixel devices running A17. *(Note: It generally only works if the app was already installed and running on A16 and then preserved/carried over during an OS upgrade to A17 without reinstalling or updating it).*

### 🚗 Android Auto Rules & Insights

Based on extensive testing (using my companion project **[AABrowser](https://github.com/fcaronte/AABrowser)**, tested successfully on my personal Samsung Galaxy S24 Ultra and a friend's Pixel 7 running Android 16):

* **The Golden Rule:** For an app to successfully show up and function in Android Auto, the **"Requested by"** field must trace back to the Play Store or the Package Installer, while the **"Installed by"** field **MUST be exclusively the Play Store**.
* **The ADB Trap:** If an app is tracked as requested by **`com.android.shell`** (such as standard ADB commands or manual scripts), **it will fail to work in Android Auto**, even if it technically looks like it was installed from the Play Store.
* Shizuku allows injecting ADB commands for testing purposes, but **unfortunately, no pure ADB-driven installation method can permanently bypass Android Auto's strict installer checks.**

---

## 🛠️ Usage

1. Download and install the latest KingInstaller release.
2. Grant necessary file access and installation permissions when prompted.
3. Select your `.apk` file (or open it directly from your file manager with KingInstaller).
4. Follow the on-screen installation steps depending on your device configuration (Root/Shizuku/Package Installer method).

---

## 📝 Notes & Limitations

* Make sure to enable **Unknown Sources** in Android Auto's Developer Settings.
* KingInstaller only alters how the package source is registered; some apps may still require additional patches (like modifying `phenotype.db` via tools like AA-Tweaker) to show up on the car display.
* Some apps carry hardcoded restrictions enforced by Android Auto itself. To unlock those, consider using Xposed modules designed for layout unlocking (such as *Android Auto - XLauncher Unlocked*).
