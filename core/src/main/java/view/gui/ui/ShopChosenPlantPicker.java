package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.BorderedTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Shop overlay: pick an unlocked plant for {@code SEED_PACKET_CHOSEN}.
 * Grid tiles are {@link SeedPacketActor} (same size/portrait placement as gameplay).
 */
public final class ShopChosenPlantPicker {
    private static final int GRID_COLS = 5;
    private static final float PACKET_W = SeedPacketActor.PACKET_WIDTH;
    private static final float PACKET_H = SeedPacketActor.PACKET_HEIGHT;
    private static final float MODAL_W = 920f;
    private static final float GRID_H = 420f;
    private static final float FADE_IN = 0.2f;
    private static final float FADE_OUT = 0.15f;

    private static Texture pixel;

    private ShopChosenPlantPicker() {}

    /**
     * @param plants unlocked plant names (any order; sorted for display)
     * @param onPick called with the chosen plant name; overlay is removed first
     */
    public static Table open(Stage stage, Skin skin, TextureBank textures,
                             Collection<String> plants, Consumer<String> onPick) {
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setBackground(new TextureRegionDrawable(whitePixel()).tint(new Color(0f, 0f, 0f, 0.55f)));
        overlay.setTouchable(Touchable.enabled);

        BorderedTable card = new BorderedTable();
        Label heading = new Label("Choose a plant", skin, "big");
        heading.setColor(Color.BLACK);
        heading.setAlignment(Align.center);
        card.add(heading).padBottom(12f).row();

        Label hint = new Label("10 seed packets for the plant you pick", skin, "secondary");
        hint.setColor(Color.WHITE);
        hint.setAlignment(Align.center);
        card.add(hint).padBottom(16f).row();

        Table grid = new Table();
        List<String> sorted = new ArrayList<>(plants);
        sorted.sort(Comparator.comparing(String::toLowerCase));

        if (sorted.isEmpty()) {
            Label empty = new Label("No unlocked plants yet.", skin, "medium");
            empty.setColor(Color.LIGHT_GRAY);
            card.add(empty).padBottom(16f).row();
        } else {
            int col = 0;
            for (String plantName : sorted) {
                Actor tile = packetTile(textures, skin, plantName, name ->
                    dismiss(overlay, () -> {
                        if (onPick != null) {
                            onPick.accept(name);
                        }
                    }));
                grid.add(tile).size(PACKET_W + 8f, PACKET_H + 8f).pad(6f);
                col++;
                if (col >= GRID_COLS) {
                    grid.row();
                    col = 0;
                }
            }
            ScrollPane scroll = new ScrollPane(grid, skin);
            scroll.setFadeScrollBars(false);
            scroll.setScrollingDisabled(true, false);
            scroll.setOverscroll(false, false);
            card.add(scroll).width(MODAL_W - 80f).height(GRID_H).padBottom(16f).row();
        }

        TextButton close = new TextButton("Cancel", skin, "brown");
        close.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dismiss(overlay, null);
            }
        });
        card.add(close).width(180f).height(56f);

        overlay.add(card).width(MODAL_W).pad(24f);
        overlay.getColor().a = 0f;
        overlay.addAction(Actions.fadeIn(FADE_IN));
        stage.addActor(overlay);
        return overlay;
    }

    private static void dismiss(Table overlay, Runnable after) {
        overlay.setTouchable(Touchable.disabled);
        overlay.clearActions();
        overlay.addAction(Actions.sequence(
            Actions.fadeOut(FADE_OUT),
            Actions.run(() -> {
                overlay.remove();
                if (after != null) {
                    after.run();
                }
            })
        ));
    }

    private static Actor packetTile(TextureBank textures, Skin skin, String plantName,
                                    Consumer<String> onPick) {
        Table hit = new Table();
        // Same READY frame + native portrait placement as collection / plant selection.
        SeedPacketActor packet = new SeedPacketActor(
            textures, skin, plantName, 0, 1, false, false, false);
        packet.onClick(() -> onPick.accept(plantName));
        hit.add(packet).size(PACKET_W, PACKET_H);
        return hit;
    }

    private static Texture whitePixel() {
        if (pixel == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            pixel = new Texture(pixmap);
            pixmap.dispose();
        }
        return pixel;
    }
}
