# Implementazione Dynamic Colors (Material You)

L'obiettivo è abilitare correttamente i colori dinamici di Android 12+ (Material You) in tutta l'applicazione, risolvendo il problema dei colori viola predefiniti.

## Modifiche Proposte

### [Componente Core]

Creazione di una classe `Application` personalizzata per inizializzare `DynamicColors` a livello globale. Questo garantisce che tutte le Activity (inclusa la principale al primo avvio) ricevano il tema dinamico.

#### [NEW] [KingInstallerApp.kt](file:///home/fraca/StudioProjects/KingInstaller/app/src/main/java/com/example/kinginstaller/KingInstallerApp.kt)
Creazione della classe che estende `Application` e chiama `DynamicColors.applyToActivitiesIfAvailable(this)`.

#### [MODIFY] [AndroidManifest.xml](file:///home/fraca/StudioProjects/KingInstaller/app/src/main/AndroidManifest.xml)
Registrazione della nuova classe `KingInstallerApp` nell'attributo `android:name` del tag `<application>`.

#### [MODIFY] [MainActivity.kt](file:///home/fraca/StudioProjects/KingInstaller/app/src/main/java/com/example/kinginstaller/MainActivity.kt)
Rimozione della chiamata ridondante a `DynamicColors.applyToActivitiesIfAvailable(this.application)`.

#### [MODIFY] [AppManagerActivity.kt](file:///home/fraca/StudioProjects/KingInstaller/app/src/main/java/com/example/kinginstaller/AppManagerActivity.kt)
Rimozione della chiamata ridondante a `DynamicColors.applyToActivitiesIfAvailable(application)`.

## Piano di Verifica

### Verifiche Manuali
- Avviare l'app su un dispositivo con Android 12 o superiore e verificare che i colori seguano lo sfondo del sistema.
- Verificare che sia la `MainActivity` che la `AppManagerActivity` mostrino i colori corretti.
