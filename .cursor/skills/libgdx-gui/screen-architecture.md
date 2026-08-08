# Screen architecture

Self-contained GUI structure for this project. No external local repos required.

## Thin Game + loading gate

`Game` owns shared `PvzAssets` (and optionally a shared `Skin`). Boot into a
loading screen that:

1. Creates `TextureBank` + `PamPlayer`
2. Queues UI skins/fonts and level PAM paths (`loadAsync`)
3. Each frame: `textures.update()` + progress UI
4. On complete: `setScreen(firstMenu)`

Screens own their `SpriteBatch` unless several already share one from `Game`.

## AbstractScreen shape

```java
public abstract class AbstractScreen implements Screen {
    protected final PvzGdxGame game;
    protected OrthographicCamera worldCamera;
    protected OrthographicCamera uiCamera;
    protected Viewport worldViewport; // FitViewport
    protected Viewport uiViewport;    // ExtendViewport or FitViewport
    protected Stage uiStage;

    private static final float MAX_DELTA = 1f / 30f;

    protected AbstractScreen(PvzGdxGame game) {
        this.game = game;
        worldCamera = new OrthographicCamera();
        worldViewport = new FitViewport(1280, 720, worldCamera);
        uiCamera = new OrthographicCamera();
        uiViewport = new ExtendViewport(1280, 720, uiCamera);
        uiStage = new Stage(uiViewport);
    }

    @Override public void show() {
        Gdx.input.setInputProcessor(uiStage); // or InputMultiplexer
    }

    @Override public void render(float delta) {
        if (delta > MAX_DELTA) delta = MAX_DELTA;
        if (game.assets != null) game.assets.textures.update();
        updateLogic(delta);
        renderGraphics();
    }

    protected abstract void updateLogic(float delta);
    protected abstract void renderGraphics();

    @Override public void resize(int w, int h) {
        worldViewport.update(w, h);
        uiViewport.update(w, h, true);
    }

    @Override public void dispose() { uiStage.dispose(); }
}
```

Pick virtual resolution to match target art (1280×720 is a fine default; change
once, consistently).

## Menu screens

- Full-bleed background (`Image` fill-parent or batch draw)
- `Table` for buttons/forms
- Shared style helper for fonts/colors registered on one `Skin`
- Listeners → existing controllers → `CommandResult` → label/dialog
- On success: `App.setCurrentMenu(...)` then `game.setScreen(...)`

## Gameplay layering

1. **World batch** — lawn background, plants/zombies/projectiles via `PamPlayer`
2. **Batch HUD (optional)** — non-interactive meters / sun count art
3. **`uiStage`** — seed packets, pause, shop sheets, modal dialogs

Interactive controls belong on the Stage so hit-testing stays reliable.

## Model → visual adapter

Keep a small view-only class (not in `model`):

```text
PlantInstance / ZombieInstance
  → definitionId → PamPaths.pam(...)
  → behavior/HP/armor → clip name ("idle"|"walk"|"eat"|…)
  → status flags → visibility Map
  → LawnLayout.center(row, col, progress) → (x, y)
  → PamPlayer.draw(batch, clipRef, t, x, y, loop, vis)
```

Cache `ClipRef` after preload. Never put domain mutations here.

## Feature asset modules

Group path constants and preload lists:

- `PamPaths` — definition id → PAM relative path + default clips
- `UiRegions` — `IMAGE_*` ids for seed packets / icons via `TextureBank.region`
- Per-level preload list invoked from the loading screen or `show()` of gameplay

Backed by libPVZ, not a parallel TextureAtlas animation system for characters.

## Overlays

| Kind | Implementation |
|---|---|
| Clickable (pause, shop, confirm) | `Table` / `Group` on `uiStage`, `setVisible` |
| Transient toast | Stage actor or short-lived batch draw |
| Drag ghost (planting) | Absolute-positioned Actor or batch sprite; planting commit still via controller |

## Modules

Single Gradle module + desktop launcher is enough initially. Split `core` /
`lwjgl3` only when packaging needs it.
