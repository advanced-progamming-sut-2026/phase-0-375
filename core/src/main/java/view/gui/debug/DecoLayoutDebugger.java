package view.gui.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import pvz.libpvz.textures.TextureBank;
import view.gui.assets.ShopArt;
import view.gui.assets.WorldMapArt;
import view.gui.ui.DraggableDecoActor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Layout-debug helper: spawn draggable/scalable decoration actors and dump their
 * positions as paste-ready Java lines. Keep this class even after positions are
 * baked into the screen — useful for any future deco pass.
 *
 * <p>Controls (after clicking a deco to select it — it turns yellow):
 * <ul>
 *   <li>Drag — move</li>
 *   <li>Mouse wheel — scale selected (works even over the shop panel)</li>
 *   <li>{@code =}/{@code +} or {@code E} — bigger</li>
 *   <li>{@code -} or {@code Q} — smaller</li>
 * </ul>
 */
public final class DecoLayoutDebugger {
    private static final float SCROLL_ZOOM_IN = 1.10f;
    private static final float SCROLL_ZOOM_OUT = 0.90f;
    private static final float KEY_ZOOM_IN = 1.08f;
    private static final float KEY_ZOOM_OUT = 0.92f;

    public record Spec(String assetId, float x, float y, float scale) {}

    private final List<DraggableDecoActor> actors = new ArrayList<>();
    private final Map<String, String> printNames;
    private DraggableDecoActor selected;
    private InputListener stageControls;

    public DecoLayoutDebugger() {
        this.printNames = new HashMap<>();
        printNames.putAll(constantNames(ShopArt.class, "DECO_"));
        printNames.putAll(constantNames(WorldMapArt.class, "DECOR_"));
    }

    /** Adds every spec as a draggable actor on {@code stage}. Missing regions are skipped. */
    public void spawn(Stage stage, TextureBank textures, Spec[] specs) {
        actors.clear();
        selected = null;
        for (Spec spec : specs) {
            TextureRegion region = textures.region(spec.assetId());
            if (region == null) {
                Gdx.app.log("DecoLayoutDebugger", "missing region: " + spec.assetId());
                continue;
            }
            String printName = printNames.getOrDefault(spec.assetId(),
                "\"" + spec.assetId() + "\"");
            DraggableDecoActor actor = new DraggableDecoActor(
                spec.assetId(), printName, region, spec.x(), spec.y(), spec.scale());
            actor.setOnSelected(() -> select(actor));
            stage.addActor(actor);
            actors.add(actor);
        }
        installStageControls(stage);
    }

    /** Small Save button that prints the current layout to the log / stdout. */
    public TextButton addSaveButton(Stage stage, Skin skin, float x, float y) {
        return addSaveButton(stage, skin, x, y, "ChapterLevelsScreen.DECORATIONS");
    }

    public TextButton addSaveButton(Stage stage, Skin skin, float x, float y, String pasteTarget) {
        TextButton save = new TextButton("Save Deco Layout", skin, "brown");
        save.setSize(220f, 52f);
        save.setPosition(x, y);
        save.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                printLayout(pasteTarget);
            }
        });
        stage.addActor(save);
        return save;
    }

    /** Logs paste-ready {@code new Deco(id, x, y, scale)} lines (scale included). */
    public void printLayout() {
        printLayout("ShopScreen.DECORATIONS");
    }

    public void printLayout(String pasteTarget) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n// === Deco layout dump — paste into ").append(pasteTarget).append(" ===\n");
        sb.append("// format: new Deco(asset, x, y, scale)\n");
        sb.append("private static final Deco[] DECORATIONS = {\n");
        for (DraggableDecoActor actor : actors) {
            sb.append(actor.toDecoJavaLine()).append('\n');
        }
        sb.append("};\n// === end deco layout dump ===\n");
        String dump = sb.toString();
        Gdx.app.log("DecoLayoutDebugger", dump);
        System.out.print(dump);
    }

    public List<DraggableDecoActor> actors() {
        return actors;
    }

    private void select(DraggableDecoActor actor) {
        if (selected == actor) {
            return;
        }
        if (selected != null) {
            selected.setSelected(false);
        }
        selected = actor;
        if (selected != null) {
            selected.setSelected(true);
            Gdx.app.log("DecoLayoutDebugger",
                "selected " + selected.printName()
                    + " — scroll / Q-E / -+= to scale");
        }
    }

    /**
     * Stage-level scroll + keys. Actor-local scroll often never fires (ScrollPane /
     * missing mouseMoved), so selection-based controls are the reliable path.
     */
    private void installStageControls(Stage stage) {
        if (stageControls != null) {
            stage.removeListener(stageControls);
        }
        stageControls = new InputListener() {
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                DraggableDecoActor target = selected != null ? selected : decoUnder(stage, x, y);
                if (target == null) {
                    return false;
                }
                if (selected != target) {
                    select(target);
                }
                // amountY > 0 typically means scroll down → zoom out
                target.nudgeScale(amountY > 0f ? SCROLL_ZOOM_OUT : SCROLL_ZOOM_IN);
                event.stop();
                return true;
            }

            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (selected == null) {
                    return false;
                }
                switch (keycode) {
                    case Input.Keys.EQUALS:
                    case Input.Keys.PLUS:
                    case Input.Keys.E:
                    case Input.Keys.RIGHT_BRACKET:
                        selected.nudgeScale(KEY_ZOOM_IN);
                        return true;
                    case Input.Keys.MINUS:
                    case Input.Keys.Q:
                    case Input.Keys.LEFT_BRACKET:
                        selected.nudgeScale(KEY_ZOOM_OUT);
                        return true;
                    default:
                        return false;
                }
            }
        };
        // Capture so ScrollPane / other UI cannot eat wheel / keys first.
        stage.addCaptureListener(stageControls);
        stage.setKeyboardFocus(stage.getRoot());
    }

    private static DraggableDecoActor decoUnder(Stage stage, float stageX, float stageY) {
        Actor hit = stage.hit(stageX, stageY, true);
        while (hit != null) {
            if (hit instanceof DraggableDecoActor deco) {
                return deco;
            }
            hit = hit.getParent();
        }
        return null;
    }

    /** Maps region id → {@code Class.FIELD} name for pretty dumps. */
    private static Map<String, String> constantNames(Class<?> type, String prefix) {
        Map<String, String> map = new HashMap<>();
        for (Field field : type.getDeclaredFields()) {
            if (!String.class.equals(field.getType())) {
                continue;
            }
            String name = field.getName();
            if (!name.startsWith(prefix)) {
                continue;
            }
            try {
                String value = (String) field.get(null);
                if (value != null) {
                    map.put(value, type.getSimpleName() + "." + name);
                }
            } catch (IllegalAccessException ignored) {
                // skip inaccessible fields
            }
        }
        return map;
    }
}
