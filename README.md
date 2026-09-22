# 3D Model Viewer — Android Multi-Model GLB Workspace

A high-performance Android application built with **Kotlin** and **Google Filament** that renders up to 5 concurrent 3D GLB models on an infinite-style interactive canvas. Features draggable and resizable containers, dynamic 3D-to-2D projected part labels extracted from binary glTF metadata (`extras.prop`), dual-mode touch gesture isolation, and demand-driven rendering optimized for low-end Android hardware.

---

## 1. Project Overview & Task Objective

This project was developed as a Senior Android Developer technical screening task. The core objective is to achieve simultaneous, fluid rendering of at least 5 independent 3D GLB models within a single-activity architecture, maintaining $\ge 30\text{ FPS}$ interactive responsiveness and strict memory efficiency on low-end hardware ($\approx 2\text{--}3\text{ GB RAM}$).

### Key Features
* **Single Activity, Zero Fragments**: Exactly one `MainActivity` hosting native Android Views on a `FrameLayout` canvas.
* **Shared Filament Engine Architecture**: Exactly one `Engine` instance across all 5 models to minimize native graphics overhead and avoid duplicated contexts.
* **5 Bundled GLB Models**: Pre-loaded assets covering mechanical, biological, and celestial domains.
* **Dynamic Binary GLB Metadata Parser**: Fast zero-copy parsing of glTF 2.0 chunk 0 (JSON) to discover nodes with `extras.prop` labels.
* **Real-Time 3D-to-2D Label Projection**: Leader lines and text badges accurately track 3D model anchors across camera orbit, zoom, container drag, and container resize.
* **Strict Dual-Mode Gesture Separation**:
  * **Normal Mode**: 1-finger container dragging and 2-finger container pinch-resizing with canvas bounds clamping. 3D models remain immutable.
  * **Interaction Mode**: 1-finger 3D camera orbit and 2-finger 3D camera dolly zoom. Container size and position remain immutable.
* **Multi-Touch Re-Anchoring**: Zero-jump transitions on `ACTION_POINTER_DOWN` and `ACTION_POINTER_UP`.
* **Active Container Z-Ordering**: Touching any container brings it to the top (`translationZ` elevation + `bringToFront()`).
* **Demand-Driven Dirty Rendering**: Render loop rests at **0 idle render submissions/sec** when models are static, waking only for active motion or UI events with 3-frame settling.
* **Resource Stabilization**: Clean deallocation of all Filament entities, swapchains, renderers, and scenes upon model close.

---

## 2. Bundled GLB Models

All models reside in `app/src/main/assets/` and contain embedded `extras.prop` metadata:

| Model Asset | Display Name | Node Count / Domain | Key Labeled Parts (`extras.prop`) |
| :--- | :--- | :--- | :--- |
| `Bulb.glb` | **Light Bulb** | 6 labeled nodes | Filament, Glass Bulb, Support Wires, Glass Mount, Metal Base, Insulator |
| `Fiagena.glb` | **Fiagena** | 7 labeled nodes | Hook, Hook Filament junction, L ring, P ring, MS ring, filament, Helical structure |
| `Lungs.glb` | **Human Lungs** | 5 labeled nodes | Larynx, Trachea, Main bronchus, Bronchial tree, Lung |
| `Microscope.glb` | **Microscope** | 12 labeled nodes | Eyepiece, Body Tube, Revolving Nosepiece, Objective Lenses, Stage, Diaphragm, Illuminator, Base, etc. |
| `solarsystem.glb` | **Solar System** | 9 labeled nodes | Mercury, Venus, Earth, Mars, Jupiter, Saturn, Uranus, Neptune, Sun |

---

## 3. Architecture & Design

```
com.app.a3dmodelviewer
├── MainActivity.kt                  # Single Activity host & model container manager
├── engine
│   ├── FilamentManager.kt           # Shared Engine & UbershaderProvider singleton
│   └── Model3DRenderer.kt           # Per-model Filament Renderer, Camera, Scene & SwapChain
├── glb
│   ├── GlbMetadataParser.kt         # Binary GLB header & JSON chunk parser
│   └── GlbNodeMetadata.kt           # Parsed node index, name, label, and local translation
├── labels
│   ├── ProjectionUtils.kt           # 3D world-to-2D screen projection & frustum culling
│   ├── TrackedModelLabel.kt         # Pre-allocated entity tracking data structure
│   └── LabelOverlayView.kt          # Zero-allocation 2D Canvas overlay (badges & lines)
└── ui
    ├── container
    │   ├── InteractionMode.kt       # NORMAL vs INTERACTION enum
    │   └── ModelContainerView.kt    # Draggable/resizable card container with gesture routing
    └── dialog
        └── ModelPickerDialog.kt     # Material dialog for selecting bundled GLB assets
```

### Shared Filament Engine Lifecycle
Creating multiple `Engine` instances in Filament duplicates native background worker threads, shader compilation pools, and driver memory. 
* **Singleton `FilamentManager`**: Creates exactly **1 shared Engine** and **1 UbershaderProvider** on `MainActivity.onCreate()`.
* **Per-Model Render Components**: Each `Model3DRenderer` creates lightweight per-model instances on the shared engine:
  * 1 `Renderer`
  * 1 `Scene`
  * 1 `View` (configured for `BlendMode.TRANSLUCENT` with clear color `(0,0,0,0)`)
  * 1 `Camera` (spherical orbit framing around model bounding box)
  * 1 `SwapChain` (bound to the container's `TextureView`)
* **Clean Deallocation**: When a model is closed, its `Model3DRenderer.destroy()` releases the scene entities, asset, loaders, camera, view, scene, swapchain, surface, and renderer back to the shared engine.

---

## 4. Metadata Parsing & Dynamic 2D Label Projection

### Binary glTF 2.0 Parser (`GlbMetadataParser.kt`)
1. Validates GLB magic header (`0x46546C67`), version `2`, and total length.
2. Extracts Chunk 0 (`0x4E4F534A` / `JSON`) bytes without external heavy libraries.
3. Parses the `nodes` array for `extras.prop` strings and extracts `name` and local `translation` offsets.

### Projection Pipeline (`ProjectionUtils.kt` & `LabelOverlayView.kt`)
1. **World Position Query**: Queries node world transform matrix from Filament's `TransformManager`.
2. **Matrix Projection**: Multiplies $P_{\text{world}} \to P_{\text{view}}$ ($4\times 4$ view matrix) $\to P_{\text{clip}}$ ($4\times 4$ projection matrix).
3. **Frustum & Near-Plane Culling**: Culls points behind the camera eye ($c_w \le 0.0001$), outside NDC depth ($[-1.05, 1.05]$), or outside viewport margins.
4. **Screen Mapping**: Converts NDC $[-1, 1]$ to Android View pixel coordinates with $Y$-axis inversion.
5. **Zero-Allocation 2D Overlay**: `LabelOverlayView` draws leader lines, anchor rings, and rounded badge rectangles using pre-allocated `Paint`, `Rect`, and `RectF` buffers.
6. **Conditional Execution**: When labels are toggled OFF, matrix math and projections are completely skipped.

---

## 5. Dual-Mode Gesture Separation & Touch Routing

Each container features a mode toggle button in its header bar:

### Normal Mode (Blue Header / Border)
* **1-Finger Drag**: Moves the container across the canvas. Re-anchors pointer coordinates on `ACTION_POINTER_DOWN` and `ACTION_POINTER_UP` to prevent jumping.
* **2-Finger Pinch**: Resizes the container (`minSizePx = 150dp`, `maxAllowed = 95% canvas`).
* **Bounds Clamping**: Constrains container translation within the visible canvas during drag and resize, ensuring all 3 header controls (Mode Toggle, Label Toggle, Close) remain accessible.
* **3D Content**: 3D camera remains completely static.

### Interaction Mode (Amber Header / Border)
* **1-Finger Drag**: Orbits the 3D camera around the model's visual center ($\Delta X \to \text{azimuth}$, $\Delta Y \to \text{elevation}$ clamped to $\pm 85^\circ$). Re-anchors baseline on pointer changes.
* **2-Finger Pinch**: Zooms the 3D camera distance ($d_{\text{new}} = d_{\text{current}} / \text{scaleFactor}$, clamped to $[0.6r, 8.0r]$).
* **Container Position & Size**: Container layout and translation remain completely immutable.

---

## 6. Performance Optimization & Measured Profiling

### Demand-Driven Dirty Rendering
Rather than executing continuous 60 Hz draw loops on all 5 containers simultaneously, `Model3DRenderer` employs dirty-frame scheduling:
* **Settling Counter**: `requestRender(frames = 3)` schedules 3 frames upon touch, resize, load, or toggle to ensure double/triple-buffered swapchains display the final frame accurately.
* **Idle State**: Render loop sleeps at **0 idle render submissions/sec** when models are static.

### Test Device Specifications
* **Device**: Samsung Galaxy M01s (`SM-M015G`)
* **SoC / CPU**: Qualcomm Snapdragon 439 / 450 (`msm8937`, 8$\times$ Cortex-A53 @ 1.45 GHz)
* **GPU**: Qualcomm Adreno 505
* **RAM**: `2,911,608 kB` ($\approx \mathbf{2.77\text{ GB} \approx 3\text{ GB RAM}}$)
* **OS**: Android 12 (API level 31)

### Measured Frame Timing (`dumpsys gfxinfo`)
Profiled with all 5 models actively loaded on screen during continuous 3D rotation interaction:

| Metric | Measured Value | Frame Rate Equivalent | Target Requirement |
| :--- | :--- | :--- | :--- |
| **50th Percentile (Median Frame Time)** | **13 ms** | $\mathbf{\approx 76.9\text{ FPS}}$ | $\ge 30\text{ FPS}$ (**PASSED**) |
| **90th Percentile Frame Time** | **16 ms** | $\mathbf{\approx 62.5\text{ FPS}}$ | $\ge 30\text{ FPS}$ (**PASSED**) |
| **95th Percentile Frame Time** | **29 ms** | $\mathbf{\approx 34.5\text{ FPS}}$ | $\ge 30\text{ FPS}$ (**PASSED**) |
| **99th Percentile Frame Time** | **31 ms** | $\mathbf{\approx 32.3\text{ FPS}}$ | $\ge 30\text{ FPS}$ (**PASSED**) |
| **Janky Frames** | **4 / 97 (4.12%)** | N/A | $< 10\%$ (**PASSED**) |
| **50th Percentile GPU Time** | **3 ms** | N/A | Adreno 505 load $\ll 16\text{ ms}$ |
| **90th Percentile GPU Time** | **4 ms** | N/A | Adreno 505 load $\ll 16\text{ ms}$ |

### Memory Footprint & Stabilization (`dumpsys meminfo`)
* **Sequential Model Loading**:
  * **0 Models (Cold Startup)**: $94.4\text{ MB Total PSS}$ (Native: 31.1 MB, Java: 9.4 MB, Graphics: 9.7 MB)
  * **1 Model (`Bulb.glb`)**: $162.1\text{ MB Total PSS}$ (Native: 57.1 MB, Java: 20.8 MB, Graphics: 38.4 MB)
  * **3 Models**: $231.1\text{ MB Total PSS}$ (Native: 77.5 MB, Java: 31.6 MB, Graphics: 76.2 MB)
  * **5 Models (All Active)**: $391.5\text{ MB Total PSS}$ (Native: 160.5 MB, Java: 32.5 MB, Graphics: 152.7 MB $\approx 14.1\%$ of device RAM)
* **3-Cycle Add $\to$ Close Stabilization**:
  * **Cycle 1**: 5 loaded ($393.5\text{ MB}$) $\to$ All closed: **140.09 MB**
  * **Cycle 2**: 5 loaded ($397.3\text{ MB}$) $\to$ All closed: **140.01 MB**
  * **Cycle 3**: 5 loaded ($389.5\text{ MB}$) $\to$ All closed: **140.52 MB**
  * **Result**: Post-close variance is approximately $< 0.5\text{ MB}$ across cycles. No significant memory growth was observed across three consecutive add $\to$ close cycles, indicating stable resource cleanup under the tested conditions.

---

## 7. Build Configuration & Requirements

* **Minimum SDK**: `minSdk = 24` (Android 7.0 Nougat)
* **Target SDK**: `targetSdk = 37`
* **Compile SDK**: `compileSdk = 37`
* **Kotlin**: JVM target 11 (`JavaVersion.VERSION_11`)
* **Filament Version**: `1.56.0` (`filament-android`, `gltfio-android`, `filament-utils-android`)

### How to Build & Run
```bash
# Clone the repository
git clone https://github.com/.../3DModelViewer.git
cd 3DModelViewer

# Run unit tests
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug
```

---

## 8. Final Verification Status

* [x] **Phase 1**: Single model Filament rendering with transparent background.
* [x] **Phase 2**: GLB binary metadata parser & real-time 3D-to-2D label projection.
* [x] **Phase 3**: Multi-model container canvas, Normal mode gestures, bounds clamping, z-ordering.
* [x] **Phase 4**: Interaction mode 3D camera orbit/zoom with dual-mode touch isolation.
* [x] **Phase 5**: Demand-driven dirty rendering optimization & physical device profiling.
* [x] **Phase 6**: Code polish, string resource extraction, unit test expansion, and documentation.
