package view.gui.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import model.app.App;
import model.enums.Chapter;
import model.enums.MenuType;
import model.game.core.GameModel;
import model.game.level.Level;
import pvz.skin.BorderedTable;
import view.gui.PvzGdxGame;
import view.gui.ui.ResourceBar;

/**
 * Temporary stand-in for {@link MenuType#IN_GAME} on chapters that still lack a lawn GUI.
 * Ancient Egypt, Frostbite Caves, Big Wave Beach, and Dark Ages use {@link GameplayScreen}.
 * Keeps menu routing / loadout state honest without drawing gameplay.
 */
public final class GameplayStubScreen extends AbstractMenuScreen {
    public GameplayStubScreen(PvzGdxGame game) {
        super(game);
    }

    @Override
    protected void buildUi() {
        App.getInstance().setCurrentMenu(MenuType.IN_GAME);
        addResourceBar();
        GameModel model = App.getInstance().getCurrentGameModel();
        Level level = model == null ? null : model.getCurrentLevel();
        BorderedTable card = stubCard(model, level);
        Table root = new Table();
        root.setFillParent(true);
        root.add(card).width(560f);
        stage.addActor(root);
    }

    private void addResourceBar() {
        Table top = new Table();
        top.setFillParent(true);
        top.top();
        top.add(new ResourceBar(skin, game.assets != null ? game.assets.textures : null))
                .expandX().right().pad(12f);
        stage.addActor(top);
    }

    private BorderedTable stubCard(GameModel model, Level level) {
        String detail = level == null
                ? "No level loaded"
                : level.getConfig().getChapter() + " · Level " + level.getConfig().getLevelId();
        String loadout = model == null || model.getSelectedPlants() == null
                || model.getSelectedPlants().isEmpty()
                ? "Loadout: (empty / level-controlled)"
                : "Loadout: " + String.join(", ", model.getSelectedPlants());
        BorderedTable card = new BorderedTable();
        card.pad(28f);
        card.add(new Label("Gameplay", skin, "big")).padBottom(8f).row();
        card.add(new Label(detail, skin, "medium")).padBottom(8f).row();
        card.add(new Label(loadout, skin, "secondary")).padBottom(12f).row();
        Label note = new Label(
                "In-game lawn UI is not implemented yet. Your level and plant selection "
                        + "are loaded in App — this screen only confirms the adventure flow.",
                skin, "secondary");
        note.setWrap(true);
        card.add(note).width(480f).padBottom(20f).row();
        card.add(backButton(level)).width(260f).height(56f);
        return card;
    }

    private TextButton backButton(Level level) {
        TextButton back = new TextButton("Back to levels", skin, "brown");
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Chapter chapter = level == null ? null : level.getConfig().getChapter();
                App.getInstance().setCurrentGameModel(null);
                App.getInstance().setCurrentGameLoop(null);
                App.getInstance().setCurrentMenu(MenuType.GAME);
                if (chapter != null) {
                    game.setScreen(new ChapterLevelsScreen(game, chapter));
                } else {
                    game.setScreen(new AdventureScreen(game));
                }
            }
        });
        return back;
    }
}
