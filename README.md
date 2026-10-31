# LuminatiOS Launcher

Launcher Android personalizzato, realizzato come fork moderno dello stile di **LuminatiOS**.

## Funzionalità

- **Home screen** con `RecyclerView` + `GridLayoutManager` (4 colonne) scorrevole e fluida.
- **Wallpaper di sistema** come sfondo della home.
- **Hotseat** con app preferite persistenti (long-press per aggiungere/rimuovere).
- **Drawer** ("Tutte le app") con griglia completa + **ricerca live**.
- **Smooth**: `DefaultItemAnimator`, `DiffUtil`, `setHasStableIds`, cache icone (`LruCache`).
- **UI immersive**: status bar e navigazione trasparenti, wallpaper dietro le barre.
- **Gesti**: swipe verso l'alto per aprire il drawer.
- **Query manifest** per visibilità app su Android 11+.

## Build

```bash
./gradlew assembleDebug
```

L'APK prodotto si trova in `app/build/outputs/apk/debug/app-debug.apk` (e anche `LuminatiOS-Launcher.apk` alla radice).

## Installazione

Dal telefono: scarica l'APK → **Installa** (Auto Blocker disattivato su Samsung).

Link diretto:
```
https://github.com/XlinuxCachyOSX/LuminatiOS-Launcher/raw/main/LuminatiOS-Launcher.apk
```

## Tecnologie

- Android (compileSdk 37, targetSdk 37, minSdk 26)
- Kotlin
- RecyclerView + GridLayoutManager + DiffUtil
- AndroidX Core (WindowInsets)
- BroadcastReceiver per le conferme PIN
- Quick Settings tile + Widget

## Aggiunte

- **Quick Settings tile** personalizzato (`LuminatiOSTileService`)
- **Widget** (`LuminatiOSWidgetProvider`)
- **Animazioni smooth** (fade + scale su entrata, fade-in hotseat)
