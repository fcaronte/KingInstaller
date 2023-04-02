# King Installer

Install packages "as Google Play Store", to work around restrictions!   
Useful for Android Auto.

## Installation
1. Download and install the latest King Installer [release](https://github.com/fcaronte/KingInstaller/releases)
2. Settings => Apps
    - Special app access => All files access => KingInstaller => Check `Allow access to manage all files`
    - See all apps => KingInstaller =>  Install unknown apps => Check `Allow from this source`

## Usage
1. If the `.apk` is already installed => Uninstall it first
2. KingInstaller
    - Click `Select file ...` => Select `.apk` to install
    - Optional, use maximum 1 check:
        - Check `Enable if you use Oppo, Realme or OnePlus phone`
        - Check `Enable if you use rooted (LineageOS) phone`
    - Click `Install as king`
        - Optional:
            - If checked `Enable if you use Oppo, Realme or OnePlus phone` => Select Default Package installer => Install
            - If checked `Enable if you use rooted (LineageOS) phone` => Grant Root access
        - Wait for text-field with `.apk` file-path to clear
3. Validate if package installed "as Google Play Store"

## Notes 
Make sure to enable `Unknown Sources` in Android Auto's Developer Settings.

King Installer **only** installs `.apk`s "as Google Play Store".   
So it overcomes only 1 of multiple restrictions put into place by Google.

You might still need to patch your `phenotype.db` with [AA-Tweaker](https://github.com/shmykelsa/AA-Tweaker) for the apps to show up in Android Auto.

If you're on MicroG, then you likely are still missing the actual `phenotype.db` + a `.pb` file,   
see this [fork of `aa4mg` ](https://github.com/Rikj000/AndroidAuto4MicroG) for that.

[Telegram](https://t.me/Android_auto_4pda) - Discuss the program in the chat room

[Video](https://www.yewtu.be/watch?v=X5UF9mYKrqc) - Example usage

Restriction applies only to Android 11 and above.

It works in 8 out of 10 cases.   
If it didn't work for you, here's a [video](https://www.yewtu.be/watch?v=ZiFnHxu-g4E) for you about an alternative variant.

## ChangeLog

**[Rikj000](https://github.com/Rikj000/KingInstaller)**
Added the root trick needed for (rooted) LineageOS phones.   
Updated documentation.

**[fcaronte](https://github.com/fcaronte/KingInstaller)**
Added the oppo trick needed for Oppo/Realme/OnePlus phones.   
Updated dependencies.

**jen94**
Corrected the paths to the files.

**[annexhack](https://gitlab.com/annexhack/king-installer)**
Initial releases
