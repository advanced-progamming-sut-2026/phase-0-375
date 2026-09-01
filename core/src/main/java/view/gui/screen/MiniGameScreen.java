package view.gui.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import controller.TravelLogMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.data.minigame.MiniGameDataEntry;
import model.enums.MenuType;
import view.gui.PvzGdxGame;
import view.gui.ui.IZombieMatchmakingOverlay;

import java.util.List;

/** Graphical mini-game selector reachable from the quests/travel-log page. */
public final class MiniGameScreen extends AbstractMenuScreen {
    private final TravelLogMenuController controller = TravelLogMenuController.getInstance();

    public MiniGameScreen(PvzGdxGame game) {
        super(game);
    }

    @Override
    protected void buildUi() {
        App.getInstance().setCurrentMenu(MenuType.TRAVEL_LOG);

        Table root = new Table();
        root.setFillParent(true);
        root.top().padTop(52f);

        Label title = new Label("Mini-Games", skin, "big");
        title.setColor(Color.WHITE);
        root.add(title).padBottom(24f).row();

        Table list = new Table();
        list.defaults().pad(8f);
        CommandResult<List<MiniGameDataEntry>> result = controller.showMiniGames();
        if (!result.isSuccess() || result.getData() == null || result.getData().isEmpty()) {
            list.add(new Label(result.getMessage(), skin, "medium")).row();
        } else {
            for (MiniGameDataEntry entry : result.getData()) {
                addEntry(list, entry);
            }
            addMultiplayerIZombieEntry(list);
            addCouchPlayIZombieEntry(list);
        }

        ScrollPane scroll = new ScrollPane(list, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        root.add(scroll).width(900f).height(700f).row();

        TextButton back = new TextButton("Back to Quests", skin, "brown");
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new QuestsScreen(game, QuestsScreen.Tab.MINI_GAMES));
            }
        });
        root.add(back).width(250f).height(56f).padTop(18f);
        stage.addActor(root);
    }

    @Override
    protected void onBack() {
        game.setScreen(new QuestsScreen(game, QuestsScreen.Tab.MINI_GAMES));
    }

    private void addEntry(Table list, MiniGameDataEntry entry) {
        String type = prettyType(entry.getMiniGameType());
        String label = type + "  •  Stage " + entry.getStage()
            + "  •  Difficulty " + entry.getDifficultyTier()
            + "  •  Reward " + entry.getCoinReward() + " coins";

        Label info = new Label(label, skin, "medium");
        info.setColor(Color.WHITE);
        info.setWrap(true);

        TextButton play = new TextButton("Play", skin, "purple");
        play.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                enter(entry);
            }
        });

        list.add(info).width(590f);
        list.add(play).width(150f).height(48f).row();
    }

    private void enter(MiniGameDataEntry entry) {
        CommandResult<Void> result = controller.enterMiniGame(
            entry.getMiniGameType(), entry.getStage());
        showToast(result.getMessage(), !result.isSuccess());
        if (result.isSuccess()) {
            game.setScreen(new LevelObjectivesScreen(game, null));
        }
    }

    private void addMultiplayerIZombieEntry(Table list) {
        Label info = new Label(
            "I Zombie  •  Multiplayer 1v1 — invite a friend or match with a random opponent.",
            skin, "medium");
        info.setColor(Color.WHITE);
        info.setWrap(true);

        TextButton play = new TextButton("Play", skin, "purple");
        play.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                stage.addActor(new IZombieMatchmakingOverlay(game, skin, null));
            }
        });

        list.add(info).width(590f);
        list.add(play).width(150f).height(48f).row();
    }

    private void addCouchPlayIZombieEntry(Table list) {
        Label info = new Label(
            "I Zombie  •  Couch Play — plants (mouse) vs zombies (keyboard) on one device.",
            skin, "medium");
        info.setColor(Color.WHITE);
        info.setWrap(true);

        TextButton play = new TextButton("Play", skin, "purple");
        play.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> result = MultiplayerMatchBootstrap.openCouchPlay(game);
                showToast(result.getMessage(), !result.isSuccess());
            }
        });

        list.add(info).width(590f);
        list.add(play).width(150f).height(48f).row();
    }

    private static String prettyType(String raw) {
        if (raw == null || raw.isBlank()) return "Mini-Game";
        String[] words = raw.replace('_', ' ').split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0)))
                .append(word.substring(1).toLowerCase());
        }
        return out.toString();
    }
}
