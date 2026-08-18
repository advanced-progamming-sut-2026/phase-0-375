---
name: libgdx-gui
description: >-
  Implements libGDX GUIs for PlantsVsZombies2 using Scene2D for chrome/HUD and
  libPVZ (PamPlayer/TextureBank) for PAM animations and atlas art. Use when
  building or editing screens, stages, actors, menus, HUD, seed packets, lawn
  rendering, libGDX setup, Scene2D UI, SpriteBatch drawing, or anything involving
  libPVZ, PAM clips, TextureBank, ClipRef, or authentic PvZ2 assets.
---

# libGDX GUI (PlantsVsZombies2 + libPVZ)

Build graphical UIs on top of the existing `model` / `controller` layers. Do **not**
reimplement game rules in actors or screens. Controllers remain the only mutators of
`App` / `GameModel` / `PvZGameLoop` and continue returning `CommandResult`.

Upstream library: [pizpizi/libPVZ](https://github.com/pizpizi/libPVZ) (JitPack:
`com.github.pizpizi:libPVZ:v0.1.6`, LibGDX 1.12.1+).

This skill is self-contained. Do not open other local repos for GUI patterns.

## When this skill applies

- New or changed libGDX screens, Scene2D widgets, HUD, seed chooser, shop chrome
- Rendering plants/zombies/projectiles with PAM animations via libPVZ
- Wiring Gradle/desktop bootstrap for GDX + libPVZ
- Mapping `MenuType` navigation to `Screen` transitions

## Architecture (mandatory)

```
model.*        ← pure domain (unchanged)
controller.*   ← CommandResult APIs (reuse from TUI)
view.*         ← existing TUI (leave alone unless asked)
view.gui.*     ← NEW: Screens, Stage, Actors, Pam renderers
```

| Concern | Where it lives |
|---|---|
| Game rules, waves, planting validity | `model` + existing controllers |
| Input → domain actions | Call `*Controller` methods; map `CommandResult` to UI feedback |
| Layout, buttons, labels, tables | Scene2D (`Stage`, `Table`, `TextButton`, …) |
| Animated plants/zombies/FX | libPVZ `PamPlayer` + `ClipRef` into a `SpriteBatch` |
| Static UI icons / atlas regions | `TextureBank.region(...)` / `atlas(...)` |
| Menu routing | `App.getCurrentMenu()` ↔ `Game`/`Screen` stack |

**Rules**

1. Never put planting, wave, or win/lose logic inside an `Actor`.
2. Prefer extending/adapting existing controllers over duplicating them.
3. Keep TUI views working unless the user asks to replace them.
4. Package all GDX code under `view.gui` — not inside `model`.

## Structural defaults

Use these as the default GUI shape for this project:

| Pattern | Do this |
|---|---|
| Thin `Game` | Own shared assets; boot a loading screen; screens own batch/skin unless shared |
| Loading gate | Don’t enter gameplay until PAM/UI preload finishes (`loadAsync` + progress) |
| Dual viewports | World: `FitViewport` + camera + batch. UI: `ExtendViewport`/`FitViewport` + `Stage` |
| Logic / draw split | `updateLogic(delta)` vs `renderGraphics()`; clamp delta (~`1/30`) |
| Draw order | World batch (PAM) → optional batch HUD → `uiStage` (interactive) |
| Overlays | Pause/shop/dialogs as `Table`/`Group` on `uiStage` |
| Model → art | Adapter: entity state → PAM path + clip + visibility map → `ClipRef` draw |
| Asset registry | Central `PamPaths` / region IDs keyed by definition ids from JSON data |
| Navigation | Controllers drive `setScreen`; always sync `App.setCurrentMenu(MenuType)` |
| Shared chrome | One `Skin` + menu style helpers (fonts/colors); don’t recreate per screen |

**Avoid for entity art:** TextureAtlas `Animation<>` pipelines, TMX/tiled rooms, or
post-FX stacks unless the user asks. Character animation is libPVZ.

## Bootstrap checklist

```
- [ ] Add LibGDX desktop deps + JitPack libPVZ (see reference-libpvz.md)
- [ ] Desktop launcher → Game; boot via LoadingScreen (or create assets then first menu)
- [ ] Shared PvzAssets (TextureBank + PamPlayer) created once, disposed once
- [ ] AbstractScreen (or equivalent): dual viewport + uiStage; clamp delta
- [ ] Screen per MenuType (or group related menus); keep App.MenuType in sync
- [ ] Gameplay: world batch (PAM) → HUD → Stage overlays
- [ ] Every render: textures.update() then draw
- [ ] dispose(): Stage, Skin, batch, fonts, PvzAssets
```

### Minimal Game shell

```java
public class PvzGdxGame extends Game {
    public PvzAssets assets; // TextureBank + PamPlayer; set during load

    @Override public void create() {
        setScreen(new LoadingScreen(this)); // queues PAM/UI; then MainMenuScreen
    }

    @Override public void render() {
        if (assets != null) assets.textures.update(); // REQUIRED every frame
        super.render();
    }

    @Override public void dispose() {
        if (screen != null) screen.dispose();
        if (assets != null) assets.dispose();
        super.dispose();
    }
}
```

### Shared assets holder

```java
public final class PvzAssets implements Disposable {
    public final TextureBank textures;
    public final PamPlayer player;
    public final String resolution; // e.g. "768"

    public static PvzAssets createDefault() {
        FileHandle root = resolveAssetsRoot(); // -Dpvz.assets or internal
        String res = System.getProperty("pvz.resolution", "768");
        TextureBank textures = new TextureBank(res, root);
        return new PvzAssets(textures, new PamPlayer(textures, root), res);
    }

    @Override public void dispose() { textures.dispose(); }
}
```

Asset folder must contain `resources.json` (decoded RESOURCES.RTON), `ATLASES/` (or
`atlases/`), and PAM files under `pam/` or `IMAGES/`. No game assets ship with libPVZ.

## Screen pattern

1. **World** — `FitViewport` + `OrthographicCamera` + `SpriteBatch` for lawn / PAM.
2. **UI** — `ExtendViewport` or `FitViewport` + `Stage` for menus, seed bar, dialogs.
3. **Split** — `updateLogic(delta)` (controllers, sim tick) vs `renderGraphics()` (draw only).
4. **Input** — `InputMultiplexer(stage, worldInput)` so HUD eats clicks first; pure menus
   may use stage-only input.
5. **Navigation** — call existing `*Controller` methods, then `App.setCurrentMenu` +
   `game.setScreen(...)` together.

```java
private static final float MAX_DELTA = 1f / 30f;

@Override public void render(float delta) {
    if (delta > MAX_DELTA) delta = MAX_DELTA;
    game.assets.textures.update();
    updateLogic(delta);
    renderGraphics();
}

void renderGraphics() {
    ScreenUtils.clear(0.1f, 0.12f, 0.1f, 1f);
    batch.setProjectionMatrix(worldCamera.combined);
    batch.begin();
    lawnRenderer.draw(batch, /* stateTimes */);
    batch.end();

    uiStage.act(Gdx.graphics.getDeltaTime());
    uiStage.draw(); // seed bar, pause overlay, dialogs
}
```

Map menus roughly as:

| `MenuType` | Screen responsibility |
|---|---|
| REGISTER / LOGIN | Auth forms → `RegisterMenuController` / `LoginMenuController` |
| MAIN | Hub → `MainMenuController` |
| GAME / TRAVEL_LOG / SHOP / … | Feature menus → matching controllers |
| PLANT_SELECTION | Seed loadout UI |
| IN_GAME | Lawn + HUD; drive `GameplayMenuController` / services |

## Scene2D best practices

- Build layouts with `Table`; avoid absolute `setPosition` except for drag ghosts.
- One shared `Skin`; do not create fonts per screen.
- Prefer `TextButton` / `ImageButton` listeners that call controllers, then show
  `CommandResult` via `Label` / `Dialog` / toast actor.
- Use `ChangeListener` for buttons; dispose listeners with the screen.
- For seed packets / inventory tiles: custom `Actor` that draws a `TextureRegion`
  (from `TextureBank`) and optional small PAM preview — still no domain logic.
- Keep `stage.getViewport().update(width, height, true)` in `resize`.
- Interactive HUD (seed bar) on Stage; passive meters may stay on the batch.

## libPVZ rendering best practices

Full API notes: [reference-libpvz.md](reference-libpvz.md). Patterns: [examples.md](examples.md).

**Hot path**

1. `loadSync` or `loadAsync` during loading / screen show — not inside tight draw loops beyond the library’s own lazy async trigger.
2. Cache `ClipRef` per entity type + clip (`idle`, `walk`, `eat`, …).
3. Draw with `player.draw(batch, clipRef, stateTime, x, y, loop[, visibilityMap])`.
4. Call `textures.update()` every frame (`Game.render` is fine).
5. One shared `PamPlayer` draws many entities (stateless).

**Visibility maps** — armor, butter, ink, ground swatches are hidden by default; set
part names `true` in a `Map<String, Boolean>` when the model says that state is active.

**Coordinates** — PAM is Y-down; `PamPlayer` flips Y and centers on canvas. Pass world
centers in libGDX space. Convert lawn `row/col` → world `x/y` in one place
(`LawnLayout`), not scattered in actors.

**Discovery** — use [pvz-asset-browser](https://github.com/pizpizi/pvz-asset-browser)
to find PAM paths, clip names, and `IMAGE_*` region IDs. Store stable path constants
in `view.gui.assets.PamPaths` (or similar), keyed by plant/zombie definition ids from
existing JSON data. Map model state → clip + visibility in a dedicated view adapter.

## Performance & lifecycle

- Prefer `ClipRef` over string PAM/clip in `render`.
- Preload PAMs for the active level on a loading screen (`loadAsync` + progress).
- `TextureBank.unloadAtlas` when leaving a chapter if memory is tight.
- Dispose Stage/Skin/batch/assets; do not dispose `TextureBank` while screens still draw.
- Avoid allocating `HashMap` visibility maps per frame — reuse per entity or pool.
- Prefer injectable `PvzAssets` over a mutable global singleton when practical.
- `core` + `lwjgl3` Gradle modules are optional; single-module GUI is fine initially.

## Anti-patterns

- Calling `GameplayMenuController` from deep inside `PamPlayer` draw code
- `new TextureBank` / `new PamPlayer` per zombie instance
- Skipping `textures.update()` (async loads never finish)
- Blocking the GL thread with bulk `loadSync` during gameplay
- Embedding raw asset paths in controllers/model
- Replacing Jackson data loaders or TUI packages as part of a GUI task
- Desyncing `App.getCurrentMenu()` from the active `Screen`
- Using TextureAtlas entity animations or TMX for lawn entities instead of libPVZ + grid model

## Workflow for a new GUI feature

1. Identify the existing controller method(s) and `CommandResult` shape.
2. Add or extend a `Screen` under `view.gui` (prefer `AbstractScreen` base once it exists).
3. Build Scene2D chrome; wire listeners → controller → UI feedback.
4. If entities need art: resolve PAM/`ClipRef` via asset map; draw in world batch.
5. Hook `MenuType` + `setScreen` together.
6. Verify dispose paths and that TUI still compiles.

## Additional resources

- [reference-libpvz.md](reference-libpvz.md) — install, API, asset layout, visibility
- [examples.md](examples.md) — menu screen, lawn entity renderer, seed-packet actor
- [screen-architecture.md](screen-architecture.md) — AbstractScreen / loading / overlays detail
