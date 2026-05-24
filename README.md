# Qingshuo Editor

A 剪映 / CapCut–style mobile video editor for Android. Mid-level MVP: import clips
from your gallery, trim, split, merge, add text overlays, apply filters and
crossfade transitions, then export an MP4 directly on the device.

The whole stack is Kotlin + Jetpack Compose + AndroidX Media3 (Google's modern
video pipeline), so the APK is small (~10 MB) and there is no native FFmpeg
dependency.

## Quick start — get the APK on your phone

You said you want a **cloud build**, so the recipe below builds the APK on
GitHub's servers — no Android Studio needed on your computer.

### 1. Create a GitHub repo

1. Sign in to <https://github.com> and click **New repository**.
2. Name it `qingshuo-editor` (or anything you like).
3. **Don't** initialize with a README — leave it empty.
4. Copy the URL (e.g. `https://github.com/<you>/qingshuo-editor.git`).

### 2. Push this project to it

Open a terminal in this folder and run:

```bash
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/<you>/qingshuo-editor.git
git push -u origin main
```

(If you don't have git installed, GitHub also lets you drag-and-drop the folder
into the new repo via the web UI.)

### 3. Wait for the build (~6 minutes)

Once you push, open the **Actions** tab in your repo on GitHub. You'll see
a workflow called **Build APK** running. When it finishes:

1. Click the run.
2. Scroll to the **Artifacts** section.
3. Download `QingshuoEditor-debug-apk.zip`.
4. Unzip it — inside is `QingshuoEditor-debug.apk`.

### 4. Install on your phone

1. Transfer `QingshuoEditor-debug.apk` to your phone (email it to yourself, copy
   over USB, or upload to cloud and download on phone).
2. On your phone open the APK file.
3. Android will warn that "this is from an unknown source" — tap **Settings**
   → enable **Allow from this source** → return → tap **Install**.
4. Open the app. The launcher icon is the pink play triangle.

> **Heads up:** because it's a debug build it's not signed for the Play Store.
> That's fine for personal installation, but Android may show extra warnings.

## Features (v0.1.0)

| Feature              | Status | Notes                                     |
|----------------------|--------|-------------------------------------------|
| Import video         | ✅     | Pick from gallery / files (any format)    |
| Multi-clip timeline  | ✅     | Sequential, horizontally scrollable       |
| Trim (start/end)     | ✅     | Per-clip in-out points                    |
| Split at playhead    | ✅     | One-tap split of selected clip            |
| Delete clip          | ✅     | Selected clip                             |
| Text overlay         | ✅     | Add at current playback position          |
| Background music     | ✅     | Pick an audio file                        |
| Color filter         | ✅     | None / B&W (extendable)                   |
| Transition           | ⚠️     | Crossfade duration set per clip (export honors it via Media3) |
| Export MP4           | ✅     | Media3 Transformer → `Android/data/com.qingshuo.editor/files/exports/` |

Beyond v0.1 (not yet implemented): waveform on timeline, sticker overlays,
chroma key, speed ramps, AI captioning, undo/redo stack.

## Project layout

```
qingshuo-editor/
├── .github/workflows/build.yml      # CI that builds the APK
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/                     # icons, strings, themes
│       └── java/com/qingshuo/editor/
│           ├── MainActivity.kt
│           ├── QingshuoApp.kt
│           ├── data/                # Clip, Project, TextOverlay
│           ├── video/               # MediaImporter, VideoExporter (Media3)
│           ├── viewmodel/           # EditorViewModel
│           └── ui/
│               ├── screens/         # HomeScreen, EditorScreen
│               ├── components/      # Timeline, VideoPreview, ToolBar, Dialogs
│               └── theme/           # Color, Type, Theme
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Building locally (optional)

If you'd rather build on your computer instead of GitHub Actions:

1. Install **Android Studio Hedgehog (2023.1.1)** or newer.
2. **File → Open** this folder. Android Studio will download Gradle 8.5 and
   regenerate the wrapper automatically — give it 5 minutes the first time.
3. Plug your phone in over USB, enable **USB debugging** under Developer Options.
4. Click the green **Run** triangle. Android Studio installs and launches the
   app directly.

## License

MIT. Use it however you want.
