# Examples — libGDX GUI patterns

Illustrative patterns for PlantsVsZombies2. Adapt package names to `view.gui.*`.
Reuse existing controllers; do not invent parallel game APIs. See
[screen-architecture.md](screen-architecture.md) for dual-viewport / loading /
overlay structure.

## 1. Main menu screen (Scene2D → controller)

```java
public class MainMenuScreen implements Screen {
    private final PvzGdxGame game;
    private final Stage stage;
    private final Label status;

    public MainMenuScreen(PvzGdxGame game) {
        this.game = game;
        stage = new Stage(new FitViewport(1280, 720), game.batch);
        Gdx.input.setInputProcessor(stage);

        status = new Label("", game.skin);
        TextButton play = new TextButton("Adventure", game.skin);
        TextButton logout = new TextButton("Logout", game.skin);

        play.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = GameMenuController.getInstance()
                        /* or the appropriate enter-game flow */.menuEnter("game");
                status.setText(r.getMessage());
                if (r.isSuccess()) {
                    App.getInstance().setCurrentMenu(MenuType.GAME);
                    game.setScreen(new GameHubScreen(game));
                }
            }
        });

        logout.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = MainMenuController.getInstance().logout();
                status.setText(r.getMessage());
                if (r.isSuccess()) game.setScreen(new LoginScreen(game));
            }
        });

        Table root = new Table();
        root.setFillParent(true);
        root.add(play).pad(8).row();
        root.add(logout).pad(8).row();
        root.add(status).padTop(16);
        stage.addActor(root);
    }

    @Override public void render(float delta) {
        ScreenUtils.clear(0.08f, 0.1f, 0.08f, 1f);
        game.assets.textures.update();
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
    }

    @Override public void dispose() { stage.dispose(); }
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}
```

Notes:

- Call the real controller methods already used by TUI views.
- Mirror `CommandResult` messaging with labels/dialogs instead of `System.out`.

## 2. Lawn entity renderer (libPVZ)

```java
public final class PamEntityRenderer {
    private final PamPlayer player;
    private final Map<String, ClipRef> clips = new HashMap<>();

    public PamEntityRenderer(PamPlayer player) { this.player = player; }

    public void preload(String pam, String... clipNames) {
        player.loadSync(pam);
        for (String c : clipNames) {
            ClipRef ref = player.getClip(pam, c);
            if (ref != null) clips.put(pam + "#" + c, ref);
        }
    }

    public void draw(Batch batch, String pam, String clip,
                     float stateTime, float x, float y, boolean loop,
                     Map<String, Boolean> visibility) {
        ClipRef ref = clips.get(pam + "#" + clip);
        if (ref == null) {
            // Still loading: kick async and skip frame
            player.loadAsync(pam, null);
            return;
        }
        if (visibility == null) {
            player.draw(batch, ref, stateTime, x, y, loop);
        } else {
            player.draw(batch, ref, stateTime, x, y, loop, visibility);
        }
    }
}
```

Wire from gameplay screen:

```java
// once when level starts
renderer.preload(PamPaths.forZombie("egypt_basic"), "idle", "walk", "eat", "die");

// each frame inside batch.begin/end
for (ZombieInstance z : model.getZombies()) {
    float[] xy = lawnLayout.centerOf(z.getRow(), z.getCol(), z.getProgress());
    String clip = mapBehaviorToClip(z); // walk/eat/die from model state
    renderer.draw(batch, PamPaths.forZombie(z.getDefinitionId()),
            clip, z.getAnimTime(), xy[0], xy[1], true,
            visibilityFor(z));
}
```

Keep `mapBehaviorToClip` and `visibilityFor` as pure view adapters over model fields.

## 3. Seed packet actor (atlas region + click → controller)

```java
public class SeedPacketActor extends Actor {
    private final TextureRegion icon;
    private final String plantId;
    private final PlantingService planting; // or GameplayMenuController facade

    public SeedPacketActor(TextureBank bank, String imageId, String plantId,
                           PlantingService planting) {
        this.icon = bank.region(imageId);
        this.plantId = plantId;
        this.planting = planting;
        setSize(80, 100);
        addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent e, float x, float y, int p, int b) {
                // Select packet in UI state only; planting happens on lawn click
                fire(new PacketSelectedEvent(plantId));
                return true;
            }
        });
    }

    @Override public void draw(Batch batch, float parentAlpha) {
        if (icon == null) return;
        Color c = getColor();
        batch.setColor(c.r, c.g, c.b, c.a * parentAlpha);
        batch.draw(icon, getX(), getY(), getWidth(), getHeight());
        batch.setColor(Color.WHITE);
    }
}
```

Lawn click handler (screen-level, not inside PamPlayer):

```java
CommandResult<?> result = planting.plant(selectedPlantId, row, col);
hud.show(result);
```

## 4. Loading screen with async PAM

```java
player.loadAsync(pamPath, () -> loadedCount.incrementAndGet());
// render():
textures.update();
float progress = loadedCount.get() / (float) total;
// when complete → game.setScreen(new GameplayScreen(...))
```

Never call massive `loadSync` stacks inside `GameplayScreen.render`.

## 5. Input multiplexer (HUD over lawn)

```java
InputMultiplexer mux = new InputMultiplexer();
mux.addProcessor(stage);           // UI first
mux.addProcessor(lawnInput);       // row/col picking second
Gdx.input.setInputProcessor(mux);
```

## 6. Gradle dual entry (TUI + GUI)

Keep `application.mainClass` for TUI. Add a second JavaExec task:

```groovy
tasks.register('runGui', JavaExec) {
    group = 'application'
    mainClass = 'view.gui.DesktopLauncher'
    classpath = sourceSets.main.runtimeClasspath
    workingDir = rootDir
    systemProperty 'pvz.assets', System.getProperty('pvz.assets', '')
    jvmArgs = ['--enable-native-access=ALL-UNNAMED']
}
```
