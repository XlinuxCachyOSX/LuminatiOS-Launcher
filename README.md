# LuminatiOS Launcher

Launcher Android personalizzato, realizzato come fork moderno dello stile di **LuminatiOS**.

## Funzionalità

- **Home screen** con griglia scorrevole (4 colonne) delle app installate che dichiarano `MAIN` + `CATEGORY_LAUNCHER`.
- **Hotseat** in basso con le app preferite dell'utente (persistite in `SharedPreferences`).
- **Drawer** ("Tutte le app") con la griglia completa delle app installate.
- **Interazione**: tap su un'icona avvia l'app; long-press aggiunge/rimuove l'app dal hotseat.
- **UI immersive**: status bar e navigazione bar nascoste (transizione con swipe).

## Aggiunte recenti

- **Quick Settings tile** personalizzato (`LuminatiOSTileService`) con azione `QS_TILE`,
  permissione `BIND_QUICK_SETTINGS_TILE`, meta-data `@xml/luminati_tile` e stato `ACTIVE`.
- **Widget** (`LuminatiOSWidgetProvider`) con layout `@layout/luminati_widget`, aggiornamento via
  `AppWidgetManager.updateAppWidget` e click (`PendingIntent` → `MainActivity`).

## Smooth

- Animazione di entrata (fade + scale) per le icone della workspace (`AppGridAdapter`).
- Animazione fade-in per le icone dell'hotseat (`MainActivity`).

## Build

```bash
./gradlew assembleDebug
```

L'APK prodotto si trova in `app/build/outputs/apk/debug/app-debug.apk`.

## Installazione

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

L'activity dichiarata con filtro `MAIN` + `CATEGORY_LAUNCHER` viene automaticamente
riconosciuta dal launcher manager del sistema come launcher dell'utente corrente.

## Tecnologie

- Android (compileSdk 37, targetSdk 37, minSdk 26)
- Kotlin
- GridView per la griglia di app
- AndroidX Core (WindowInsets)
- BroadcastReceiver per le conferme PIN (`CONFIRM_PIN_SHORTCUT` / `CONFIRM_PIN_APPWIDGET`)
