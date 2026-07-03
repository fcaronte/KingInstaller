# KingInstaller X

KingInstaller X installs Android packages while letting you control the installer source and advanced install flags. It is mainly useful when an app needs to appear as installed from Google Play Store, including Android Auto workflows.

## Supported files

- APK
- APKS
- APKM
- XAPK
- ZIP bundles containing split APKs

## Install methods

KingInstaller X uses a simple flow:

1. Pick a package file.
2. Choose a method.
3. Choose advanced options if needed.
4. Install and check the status.

Methods:

- Normal Mode: Android's standard install route.
- OEM MODE: chooser route for Oppo, OnePlus, Realme and Nothing-style installers.

Authorization:

- No root: opens Android's install prompt.
- Shizuku/Sui: silent session install through Shizuku.
- Root: silent `pm install` route through `su`.

## Advanced options

Advanced options are real install/session flags where Android supports them. Unsupported options are disabled in no-root mode, and the status area reports selected options as set, requested, skipped or failed after install.

Available options include:

- Set install source package, defaulting to `com.android.vending`.
- Customize install reason and package source.
- Set target user or install for all users.
- Allow test APKs.
- Allow downgrade.
- Bypass low target SDK block.
- Grant requested permissions.
- Allow restricted permissions.
- Disable package verification.
- Disable ADB verifier.
- Enable rollback.
- Mark install as from ADB.
- Request update ownership.
- Bypass Play Protect-related checks where the selected route can request it.
- Private space/profile install flag on supported Android versions.

Some flags depend on Android version, ROM behavior, root policy or Shizuku permissions. If Android ignores a submitted flag, KingInstaller X cannot force it without deeper system privileges.

## Extra actions

- Open installed app after a successful install.
- Check the reported installer source.
- Open package-installer settings to clear default handlers.
- Request ignore battery optimization for stricter ROMs.

## GitHub release builds

The Android release workflow can be run manually from GitHub Actions. It builds debug and release APKs, uploads them as workflow artifacts, and creates or updates a GitHub release with the APK files and SHA256 sums.

## Android Auto notes

Installing as Google Play Store is only one Android Auto requirement. Some apps still need Android Auto developer settings, AA phenotype changes, MicroG-specific files, or an unlock module before they appear in Android Auto.

## Credits

- Original KingInstaller work by annexhack.
- Root installer flow by Rikj000.
- OEM MODE work by fcaronte.
- File path fixes by jen94.
- Advanced install ideas referenced from InstallerX Revived, InstallWithOptions and PackageInstaller.
