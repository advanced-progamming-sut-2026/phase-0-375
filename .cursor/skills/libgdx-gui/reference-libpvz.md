# libPVZ reference

Source of truth: [https://github.com/pizpizi/libPVZ](https://github.com/pizpizi/libPVZ).
Current documented release tag: `v0.1.6`. Requires Java 8+ (project uses newer OK),
LibGDX **1.12.1+**, Gradle 7+.

Package root: `pvz.libpvz`.

## Gradle

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

ext { gdxVersion = '1.12.1' }

dependencies {
    implementation "com.badlogicgames.gdx:gdx:$gdxVersion"
    implementation "com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion"
    implementation "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop"
    implementation 'com.github.pizpizi:libPVZ:v0.1.6'
}
```

Desktop main class should launch `Lwjgl3Application` with this project's `Game`
implementation. Keep the existing CLI `Main` entry for TUI unless replacing it.

Optional JVM props (match libPVZ demo):

| Property | Purpose |
|---|---|
| `pvz.assets` | Absolute path to extracted Base Assets root |
| `pvz.resolution` | Atlas resolution prefix, e.g. `768` or `1200` |
| `pvz.pam` | Demo-only default PAM path |

## Asset directory layout

Root folder passed to `TextureBank` / `PamPlayer`:

```
<assets>/
  resources.json          # or RESOURCES.json (decoded RTON)
  ATLASES/                # or atlases/
  pam/                    # or IMAGES/ (PamPlayer falls back)
    .../**/*.PAM
```

libPVZ does **not** ship PopCap/EA assets. Provide legally owned extracted files.

## Core types

### `TextureBank`

```java
TextureBank textures = new TextureBank("768", assetsFolder);
textures.update();                    // every frame
TextureRegion r = textures.region("IMAGE_ZOMBIE_EGYPT_BASIC_HEAD");
TextureRegion a = textures.atlas("ATLAS_ZOMBIE_EGYPT_BASIC");
textures.loadSync(atlasId);
textures.loadAsync(atlasId, onDone);
textures.unloadAtlas(atlasId);
textures.dispose();
```

- Resolves regions from `resources.json` for the given resolution prefix.
- Owns an internal `AssetManager`; **must** call `update()` in the render loop.

### `PamPlayer`

```java
PamPlayer player = new PamPlayer(textures, assetsFolder);
```

Looks for `assetsFolder/pam`, else `assetsFolder/IMAGES`.

| Method | Use |
|---|---|
| `loadSync(pam)` | Loading screens / setup |
| `loadAsync(pam, cb)` | Background preload |
| `getClip(pam, clip)` | O(1) handle after load (`null` if not ready) |
| `draw(batch, pam, clip, time, x, y, loop)` | Convenient; string lookup |
| `draw(batch, clipRef, time, x, y, loop)` | **Preferred in render** |
| `draw(batch, clipRef, time, x, y, scaleX, scaleY, loop)` | Scaled about `(x, y)`; see `AnimScale` |
| `draw(..., visibilityMap)` | Armor / status parts |
| `draw(batch, clipRef, time, x, y, scaleX, scaleY, loop, visibilityMap)` | Scale + armor together |
| `drawPart(...)` | Single named part whitelist |
| `clips(pam)` | List clip names (sync load) |
| `bounds(pam[, clip])` | Canvas / clip bounds (sync) |
| `partBounds(clipRef \| pam+clip, time, part)` | Where one part sits on that frame |
| `partBoundsByFrame(clipRef, part)` | Same, every frame in one pass — bake curves at load |
| `clipDurationSeconds(pam, clip)` | Timing |

`partBounds` covers the part and its descendants (what `drawPart` renders), in canvas
units with the origin at the canvas centre and Y down — multiply by your draw scale and
add the draw position. Null means not loaded yet or the part draws nothing there; a null
part name is not supported.

One `PamPlayer` draws many entities concurrently (stateless draw path).

### `ClipRef`

```java
player.loadSync(pamPath);
ClipRef walk = player.getClip(pamPath, "walk");
// walk.duration — seconds
player.draw(batch, walk, stateTime, x, y, true);
```

Rebuild refs after ensuring the PAM is baked; treat null as “still loading”.

## Visibility maps

By default, parts flagged as armor / custom / ground_swatch / ink / butter stay
hidden. Override with:

```java
Map<String, Boolean> vis = new HashMap<>();
vis.put("zombie_armor_bucket_norm", true);
vis.put("_zombie_egypt_armor2_states", true);
vis.put("butter", true);
player.draw(batch, clip, t, x, y, true, vis);
```

Drive these from model state (armor type, frozen, buttered) — do not hardcode per
frame without consulting entity state.

## Coordinates & tint

- PAM native space is Y-down; `PamPlayer` applies Y flip and centers on canvas
  width/height at `(x, y)`.
- Batch color multiplies part colors when not pure white.
- Prefer world centers from a shared lawn layout helper.

## Finding assets

Use [pvz-asset-browser](https://github.com/pizpizi/pvz-asset-browser) to discover:

- PAM relative paths (e.g. `768/INITIAL/ZOMBIE/.../ZOMBIE_EGYPT_BASIC.PAM`)
- Clip names (`idle`, `walk`, `eat`, …)
- Image resource IDs for `TextureBank.region`

Centralize mappings from this project's definition names (`plants.json`,
`zombies.json`) → PAM path + default clips in one registry class.

## Demo smoke test

Upstream: `./gradlew runDemo` with:

```properties
systemProp.pvz.assets=/path/to/Base Assets
systemProp.pvz.pam=768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM
```

Useful to validate asset paths before wiring the full game GUI.
