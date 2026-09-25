# 3D Model Viewer — Android Multi-Model GLB Workspace

A high-performance Android application built with **Kotlin** and **Google Filament**. Supports simultaneous rendering of multiple independent GLB model instances, with performance profiled using 5 models concurrently on an interactive canvas. Features draggable and resizable containers, dynamic 3D-to-2D projected part labels extracted from binary glTF metadata (`extras.prop`), dual-mode touch gesture isolation, and demand-driven rendering optimized for resource-constrained Android hardware.

---

## 1. Project Overview & 3D Library Choice

**3D Model Viewer** demonstrates real-time, multi-model 3D rendering and interactive manipulation within a single Android Activity.

### Why Google Filament?
**Google Filament** (`com.google.android.filament`) was chosen for this project because of its lightweight real-time PBR rendering pipeline, official glTF/GLB support via `gltfio`, and low-level architectural flexibility. Crucially, Filament allows binding multiple isolated `Renderer`, `Scene`, `View`, `Camera`, and translucent `SwapChain` instances to a single shared native `Engine`, making it ideal for running multiple concurrent 3D viewports efficiently on memory-constrained mobile hardware.

### Key Features
* **100% Jetpack Compose UI**: Entire UI layer built declaratively with Jetpack Compose and Material 3, replacing legacy XML layouts and custom `ViewGroup`s.
* **Zero-Recomposition Gesture Performance**: Container translation and sizing update layout/draw phases directly via lambda modifiers (`Modifier.offset { ... }`), completely preventing recomposition of Filament `TextureView` viewports during touch gestures.
* **Shared Filament Engine**: Exactly one shared `Engine` instance across all active models to prevent duplicated native threads, shader pools, and context overhead.
* **5 Bundled GLB Models**: Pre-loaded assets covering mechanical, biological, and celestial domains (`Bulb.glb`, `Fiagena.glb`, `Lungs.glb`, `Microscope.glb`, `solarsystem.glb`).
* **Dynamic Binary GLB Metadata Parser**: Fast zero-copy parsing of glTF 2.0 chunk 0 (JSON) to discover nodes with `extras.prop` labels.
* **Real-Time 3D-to-2D Label Projection**: Leader lines and text badges accurately track 3D model anchors across camera orbit, zoom, container drag, and resize.
* **Strict Dual-Mode Gesture Separation**:
  * **Normal Mode**: 1-finger container dragging and 2-finger container pinch-resizing with canvas bounds clamping. 3D models remain immutable.
  * **Interaction Mode**: 1-finger 3D camera orbit and 2-finger 3D camera dolly zoom. Container size and position remain immutable.
* **Multi-Touch Re-Anchoring**: Zero-jump transitions on pointer down and pointer up events in Compose `pointerInput`.
* **Active Container Z-Ordering**: Touching any container brings it to the top via Compose `zIndex` elevation.
* **Demand-Driven Dirty Rendering**: Render loop rests at **0 idle render submissions/sec** when models are static, waking only for active motion or UI events with 3-frame settling.
* **Resource Stabilization**: Clean deallocation of all Filament entities, swapchains, renderers, and scenes upon model close.

---

## 2. Bundled GLB Models

All 5 bundled models reside in `app/src/main/assets/` and contain embedded `extras.prop` metadata:

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
├── MainActivity.kt                      # Single ComponentActivity hosting Compose root
├── engine
│   ├── FilamentManager.kt               # Shared Engine & UbershaderProvider singleton
│   └── Model3DRenderer.kt               # Per-model Filament Renderer, Camera, Scene & SwapChain
├── glb
│   ├── GlbMetadataParser.kt             # Binary GLB header & JSON chunk parser
│   └── GlbNodeMetadata.kt               # Parsed node index, name, label, and local translation
├── labels
│   ├── ProjectionUtils.kt               # 3D world-to-2D screen projection & frustum culling
│   ├── TrackedModelLabel.kt             # Pre-allocated entity tracking data structure
│   └── LabelOverlayCanvas.kt            # Zero-allocation Compose Canvas overlay (badges & lines)
└── ui
    ├── container
    │   ├── InteractionMode.kt           # NORMAL vs INTERACTION enum
    │   └── ModelContainerCard.kt        # Compose Card with header controls & gesture routing
    ├── dialog
    │   └── ModelPickerDialog.kt         # Material 3 AlertDialog for selecting GLB assets
    ├── screen
    │   └── ModelViewerScreen.kt         # Declarative multi-model canvas, top bar, & FAB
    ├── state
    │   └── ModelItemState.kt            # Observable container state holder (bounds, mode, zIndex)
    └── theme
        ├── Color.kt                     # Material 3 color definitions
        └── Theme.kt                     # Dark theme configuration for 3D workspace
```

### Shared Filament Engine Lifecycle
Creating multiple `Engine` instances in Filament duplicates native background worker threads, shader compilation pools, and driver memory. 
* **Singleton `FilamentManager`**: Creates exactly **1 shared Engine** and **1 UbershaderProvider** on `MainActivity.onCreate()`.
* **Per-Model Render Components**: Each `Model3DRenderer` creates lightweight per-model instances on the shared engine:
  * 1 `Renderer`
  * 1 `Scene`
  * 1 `View` (configured for `BlendMode.TRANSLUCENT` with clear color `(0,0,0,0)`)
  * 1 `Camera` (spherical orbit framing around model bounding box)
  * 1 `SwapChain` (bound to the container's `TextureView` via `AndroidView`)
* **Clean Deallocation**: When a model is closed, its `Model3DRenderer.destroy()` releases the scene entities, asset, loaders, camera, view, scene, swapchain, surface, and renderer back to the shared engine.

---

## 4. Metadata Parsing & Dynamic 2D Label Projection

### Binary glTF 2.0 Parser (`GlbMetadataParser.kt`)
1. Validates GLB magic header (`0x46546C67`), version `2`, and total length.
2. Extracts Chunk 0 (`0x4E4F534A` / `JSON`) bytes without external heavy libraries.
3. Parses the `nodes` array for `extras.prop` strings and extracts `name` and local `translation` offsets.

### Projection Pipeline (`ProjectionUtils.kt` & `LabelOverlayCanvas.kt`)
1. **World Position Query**: Queries node world transform matrix from Filament's `TransformManager`.
2. **Matrix Projection**: Multiplies $P_{\text{world}} \to P_{\text{view}}$ ($4\times 4$ view matrix) $\to P_{\text{clip}}$ ($4\times 4$ projection matrix).
3. **Frustum & Near-Plane Culling**: Culls points behind the camera eye ($c_w \le 0.0001$), outside NDC depth ($[-1.05, 1.05]$), or outside viewport margins.
4. **Screen Mapping**: Converts NDC $[-1, 1]$ to Compose Canvas pixel coordinates with $Y$-axis inversion.
5. **Zero-Allocation 2D Overlay**: `LabelOverlayCanvas` draws leader lines, anchor rings, and rounded badge rectangles using pre-allocated `Paint`, `Rect`, and `RectF` buffers directly onto Compose `Canvas`.
6. **Conditional Execution**: When labels are toggled OFF, matrix math and projections are completely skipped.

---

## 5. Dual-Mode Gesture Separation & Touch Routing

Each container features a mode toggle button in its header bar:

### Normal Mode (Blue Header / Border)
* **1-Finger Drag**: Moves the container across the canvas. Re-anchors pointer coordinates on pointer down and pointer up to prevent jumping.
* **2-Finger Pinch**: Resizes the container (`minSizePx = 150dp`, `maxAllowed = 95% canvas`).
* **Bounds Clamping**: Constrains container translation within the visible canvas during drag and resize, ensuring all 3 header controls (Mode Toggle, Label Toggle, Close) remain accessible.
* **3D Content**: 3D camera remains completely static.

### Interaction Mode (Amber Header / Border)
* **1-Finger Drag**: Orbits the 3D camera around the model's visual center ($\Delta X \to \text{azimuth}$, $\Delta Y \to \text{elevation}$ clamped to $\pm 85^\circ$). Re-anchors baseline on pointer changes.
* **2-Finger Pinch**: Zooms the 3D camera distance ($d_{\text{new}} = d_{\text{current}} / \text{scaleFactor}$, clamped to $[0.6r, 8.0r]$).
* **Container Position & Size**: Container layout and translation remain completely immutable.

---

## 6. Performance Optimization & Measured Profiling

### Key Optimizations Applied
* **Shared Engine**: One native engine singleton avoids redundant threads and shader cache duplication across models.
* **Zero-Recomposition Gestures**: Drag and resize gestures use `Modifier.offset { ... }` lambda modifiers, bypassing Compose recomposition cycles entirely during active movement.
* **Demand-Driven Dirty Rendering**: Replaced continuous 60 Hz loops with dirty-frame scheduling (`requestRender(frames = 3)`). Static models rest at **0 idle render submissions/sec**.
* **Conditional Label Math**: Skipping matrix transformations when labels are hidden saves CPU cycles.
* **Zero Per-Frame Allocations**: Reusable matrix and drawing buffers in `Model3DRenderer`, `ProjectionUtils`, and `LabelOverlayCanvas` avoid runtime garbage collection.

### Test Device Specifications
* **Device**: Samsung Galaxy M01 (`SM-M015G`)
* **SoC / CPU**: Qualcomm Snapdragon 439 / 450 (`msm8937`, 8$\times$ Cortex-A53 @ 1.45 GHz)
* **GPU**: Qualcomm Adreno 505
* **RAM**: `2,911,608 kB` ($\approx \mathbf{2.77\text{ GB} \approx 3\text{ GB RAM}}$)
* **OS**: Android 12 (API level 31)

### Measured Frame Timing (`dumpsys gfxinfo`)
Profiled with 5 models actively loaded on screen during continuous 3D rotation interaction:

| Metric | Measured Value | Frame Rate Equivalent | Target Benchmark |
| :--- | :--- | :--- | :--- |
| **50th Percentile (Median Frame Time)** | **13 ms** | $\mathbf{\approx 76.9\text{ FPS}}$ | Exceeds $30\text{ FPS}$ target |
| **90th Percentile Frame Time** | **16 ms** | $\mathbf{\approx 62.5\text{ FPS}}$ | Exceeds $30\text{ FPS}$ target |
| **95th Percentile Frame Time** | **29 ms** | $\mathbf{\approx 34.5\text{ FPS}}$ | Exceeds $30\text{ FPS}$ target |
| **99th Percentile Frame Time** | **31 ms** | $\mathbf{\approx 32.3\text{ FPS}}$ | Exceeds $30\text{ FPS}$ target |
| **Janky Frames** | **4 / 97 (4.12%)** | N/A | Low jank rate ($< 5\%$) |
| **50th Percentile GPU Time** | **3 ms** | N/A | Adreno 505 load $\ll 16\text{ ms}$ |
| **90th Percentile GPU Time** | **4 ms** | N/A | Adreno 505 load $\ll 16\text{ ms}$ |

### Memory Footprint & Stabilization (`dumpsys meminfo`)
* **Sequential Model Loading**:
  * **0 Models (Cold Startup)**: $94.4\text{ MB Total PSS}$ (Native: 31.1 MB, Java: 9.4 MB, Graphics: 9.7 MB)
  * **1 Model (`Bulb.glb`)**: $162.1\text{ MB Total PSS}$ (Native: 57.1 MB, Java: 20.8 MB, Graphics: 38.4 MB)
  * **3 Models**: $231.1\text{ MB Total PSS}$ (Native: 77.5 MB, Java: 31.6 MB, Graphics: 76.2 MB)
  * **5 Models (Active Concurrency)**: $391.5\text{ MB Total PSS}$ (Native: 160.5 MB, Java: 32.5 MB, Graphics: 152.7 MB $\approx 14.1\%$ of device RAM)
* **3-Cycle Add $\to$ Close Stabilization**:
  * **Cycle 1**: 5 loaded ($393.5\text{ MB}$) $\to$ All closed: **140.09 MB**
  * **Cycle 2**: 5 loaded ($397.3\text{ MB}$) $\to$ All closed: **140.01 MB**
  * **Cycle 3**: 5 loaded ($389.5\text{ MB}$) $\to$ All closed: **140.52 MB**
  * **Result**: Post-close variance is approximately $< 0.5\text{ MB}$ across cycles. No significant memory growth was observed across three consecutive add $\to$ close cycles, indicating stable resource cleanup under the tested conditions.

---

## 7. Engineering Trade-offs

* **Shared Engine vs. Isolated Engines**: Using a single shared `Engine` instance with per-container `Renderer`/`Scene`/`View` saves significant native memory and eliminates duplicate worker threads, at the cost of managing explicit per-view resource lifecycles.
* **Camera Orbit vs. Model Transform Manipulation**: Orbiting the camera around the model bounding box keeps original glTF scene node transforms intact, ensuring straightforward world-to-screen label projection without introducing complex parent-child inverse kinematic matrix calculations.
* **Compose `AndroidView` wrapping `TextureView` vs. `SurfaceView`**: `TextureView` via `AndroidView` was selected over `SurfaceView` to enable alpha blending, translucent rendering, dynamic Compose `zIndex` ordering, and seamless clipping/elevation inside Compose Material 3 Cards.
* **Demand-Driven Settling Window**: Using a 3-frame settle on gesture completion ensures swapchain buffers receive the final visual state without requiring a continuously polling 60 Hz Choreographer loop.

---

## 8. Known Limitations

* **Device Profiling Scope**: Performance and frame-timing benchmarks were gathered on a Samsung Galaxy M01; metrics may vary on different GPU architectures and OEM driver implementations.
* **Label Layout**: The 2D label overlay clamps badges to container bounds but does not feature dynamic collision resolution when multiple labels cluster in dense areas.
* **Tested Model Scale**: Profiling specifically evaluated 5 concurrent models; adding significantly more simultaneous instances will scale graphics driver swapchain memory accordingly.

---

## 9. Future Improvements

* **Dynamic Label Collision Avoidance**: Implement force-directed placement or leader-line avoidance to prevent overlapping badges in dense viewing angles.
* **Automated Gesture & UI Instrumentation**: Add comprehensive multi-touch instrumented tests covering pinch and drag gesture interactions.
* **Model Geometry & Texture Caching**: Cache decoded binary GLB meshes to accelerate initial instantiation when loading duplicate models.
* **Expanded Camera Controls**: Add two-finger pan/translation and customizable field-of-view controls.

---

## 10. Build Configuration & Requirements

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
