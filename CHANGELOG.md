# Changelog

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
