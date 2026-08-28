package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import model.app.App;
import model.enums.LevelType;
import model.enums.MiniGameType;
import model.game.core.GameModel;
import model.game.level.Level;
import model.game.level.LevelConfig;
import model.game.level.minigame.MiniGameLevel;
import model.game.level.minigame.beghouled.BeghouledLevel;
import model.game.level.minigame.bowling.WallnutBowlingLevel;
import model.game.level.minigame.izombie.IZombieLevel;
import model.game.level.minigame.vasebreaker.VaseBreakerLevel;
import model.game.level.minigame.zombotany.ZombotanyLevel;
import model.game.level.special.ScoreLevel;
import model.game.rule.GameRules;

import java.util.ArrayList;
import java.util.List;

/**
 * "Level Objectives" splash overlay shown at gameplay start.
 * Outer background: image_ui_if_bundle_reward1_bg (ten-patch).
 * Inner card:       image_ui_cards_store_store_bundle_card (ten-patch).
 * Each objective: an unchecked, non-interactive CheckBox.
 * Continue button: purple TextButton, centered on the outer frame's bottom edge.
 */
public final class LevelObjectivesOverlay {

    /** Yellow header band height on {@code store_bundle_card}. */
    private static final float HEADER_HEIGHT = 90f;
    private static final float BTN_W = 240f;
    private static final float BTN_H = 60f;
    private static final Color TITLE_WHITE = Color.WHITE;
    private static final Color TITLE_SHADOW = new Color(0f, 0f, 0f, 0.45f);
    private static final Color OBJECTIVE_COLOR = new Color(0.22f, 0.12f, 0.04f, 1f);

    private static Texture pixel;

    private LevelObjectivesOverlay() {}

    /** Creates the overlay and returns it ready to add to uiStage. */
    public static Table create(Skin skin, LevelConfig config, Runnable onContinue) {
        List<String> objectives = objectivesFor(App.getInstance().getCurrentGameModel(), config);

        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setTouchable(Touchable.enabled);
        overlay.setBackground(new TextureRegionDrawable(whitePixel()).tint(new Color(0f, 0f, 0f, 0.55f)));

        Table outer = new Table();
        Drawable outerBg = UiDrawables.tenPatch(skin, "image_ui_if_bundle_reward1_bg");
        if (outerBg != null) {
            outer.setBackground(outerBg);
        } else {
            outer.setBackground(new TextureRegionDrawable(whitePixel()).tint(new Color(0.55f, 0.35f, 0.12f, 1f)));
        }

        Table card = new Table();
        Drawable cardBg = UiDrawables.tenPatch(skin, "image_ui_cards_store_store_bundle_card");
        if (cardBg != null) {
            card.setBackground(cardBg);
        } else {
            card.setBackground(new TextureRegionDrawable(whitePixel()).tint(new Color(0.96f, 0.92f, 0.78f, 1f)));
        }

        Table header = new Table();
        header.add(titleLabel(skin)).expand().center();
        card.add(header).height(HEADER_HEIGHT).growX().row();

        Table body = new Table();
        body.pad(12f, 28f, 28f, 28f);
        body.defaults().left().growX();
        for (String text : objectives) {
            CheckBox cb = new CheckBox(" " + text, skin);
            cb.setChecked(false);
            cb.setTouchable(Touchable.disabled);
            SkinFonts.scaleCheckBox(cb, skin, "default", 1.1f);
            cb.getLabel().setColor(OBJECTIVE_COLOR);
            body.add(cb).padBottom(10f).row();
        }
        card.add(body).grow().row();

        outer.add(card).pad(32f, 40f, 36f, 40f).minWidth(480f);

        TextButton continueBtn = new TextButton("CONTINUE", skin, "purple");
        continueBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                overlay.remove();
                if (onContinue != null) {
                    onContinue.run();
                }
            }
        });

        // Button centered on the outer frame's bottom edge (half over, half below).
        Table wrap = new Table();
        wrap.add(outer).row();
        wrap.add(continueBtn).width(BTN_W).height(BTN_H).padTop(-BTN_H * 0.5f);

        overlay.add(wrap).pad(40f);
        return overlay;
    }

    private static Actor titleLabel(Skin skin) {
        BitmapFont font = SkinFonts.outlined(skin, "big");
        Label.LabelStyle style = new Label.LabelStyle(font, TITLE_WHITE);

        Label shadow = new Label("Level Objectives", style);
        shadow.setColor(TITLE_SHADOW);
        shadow.setAlignment(Align.center);

        Label title = new Label("Level Objectives", style);
        title.setColor(TITLE_WHITE);
        title.setAlignment(Align.center);

        Stack stack = new Stack();
        Table shadowPad = new Table();
        shadowPad.add(shadow).padLeft(3f).padBottom(3f);
        stack.add(shadowPad);
        stack.add(title);
        return stack;
    }

    /** Same objective lines shown on the start splash and the pause menu. */
    public static List<String> objectivesFor(LevelConfig config) {
        return objectivesFor(null, config);
    }

    public static List<String> objectivesFor(GameModel model, LevelConfig config) {
        List<String> scoreGame = scoreGameObjectives(model);
        if (scoreGame != null) {
            return scoreGame;
        }
        List<String> bowling = bowlingObjectives(model);
        if (bowling != null) {
            return bowling;
        }
        List<String> vaseBreaker = vaseBreakerObjectives(model);
        if (vaseBreaker != null) {
            return vaseBreaker;
        }
        List<String> beghouled = beghouledObjectives(model);
        if (beghouled != null) {
            return beghouled;
        }
        List<String> iZombie = iZombieObjectives(model);
        if (iZombie != null) {
            return iZombie;
        }
        List<String> zombotany = zombotanyObjectives(model);
        if (zombotany != null) {
            return zombotany;
        }
        List<String> out = new ArrayList<>();
        if (config == null) {
            out.add("Survive the zombie attack!");
            return out;
        }
        LevelType type = config.getLevelType();
        GameRules rules = config.getRules();

        if (type == LevelType.TIMED_WAR && rules != null) {
            int kills = rules.getTimedWarTargetKills();
            float limit = rules.getTimedWarLimit();
            if (kills > 0 && limit > 0) {
                out.add("Defeat " + kills + " zombies in " + formatTime(limit));
            } else if (kills > 0) {
                out.add("Defeat " + kills + " zombies");
            }
        } else if (type == LevelType.LOVE_YOUR_PLANTS && rules != null && rules.getMaxPlantDeaths() >= 0) {
            out.add("Lose no more than " + rules.getMaxPlantDeaths() + " plant" +
                    (rules.getMaxPlantDeaths() == 1 ? "" : "s"));
        } else if (type == LevelType.SAVE_OUR_SEEDS) {
            out.add("Protect the pre-placed plants");
        } else if (type == LevelType.DEAD_LINE) {
            out.add("Don't let zombies cross the Dead Line");
        } else if (type == LevelType.CONVEYOR_BELT) {
            out.add("Use the plants delivered by the conveyor belt");
        } else if (type == LevelType.NIGHT_OPS) {
            out.add("Survive without sun from the sky");
        } else if (type == LevelType.PLANT_WHAT_YOU_GET) {
            out.add("Plant everything you are given");
        } else if (type == LevelType.LOCKED_PLANTS) {
            out.add("Defeat all zombies with the given plants");
        }

        if (out.isEmpty()) {
            out.add("Survive the zombie attack!");
        }
        return out;
    }

    private static List<String> scoreGameObjectives(GameModel model) {
        if (model == null || !(model.getCurrentLevel() instanceof ScoreLevel)) {
            return null;
        }
        return List.of(
                "Survive today's five Myopoint waves",
                "Earn points for stylish kills and combos",
                "Bonus for multi-kills, quick kills, and perfect waves",
                "Your best score appears on the leaderboard");
    }

    private static List<String> bowlingObjectives(GameModel model) {
        if (model == null) {
            return null;
        }
        Level level = model.getCurrentLevel();
        boolean bowling = level instanceof WallnutBowlingLevel
                || (level instanceof MiniGameLevel mini
                && mini.getMiniGameType() == MiniGameType.WALLNUT_BOWLING);
        if (!bowling) {
            return null;
        }
        return List.of(
                "Roll Wall-nuts into zombies from the conveyor belt",
                "Only launch left of the red line",
                "Survive every zombie wave");
    }

    private static List<String> vaseBreakerObjectives(GameModel model) {
        if (model == null) {
            return null;
        }
        Level level = model.getCurrentLevel();
        boolean vase = level instanceof VaseBreakerLevel
                || (level instanceof MiniGameLevel mini
                && mini.getMiniGameType() == MiniGameType.VASE_BREAKER);
        if (!vase) {
            return null;
        }
        return List.of(
                "Break every vase on the lawn",
                "Plant free seed packets before they expire",
                "Don't let zombies eat your brains");
    }

    private static List<String> beghouledObjectives(GameModel model) {
        if (model == null) {
            return null;
        }
        Level level = model.getCurrentLevel();
        boolean beghouled = level instanceof BeghouledLevel
                || (level instanceof MiniGameLevel mini
                && mini.getMiniGameType() == MiniGameType.BEGHOULED);
        if (!beghouled) {
            return null;
        }
        int target = 0;
        if (level instanceof BeghouledLevel bg) {
            target = bg.getSettings().getMatchTarget();
        }
        String matches = target > 0
                ? "Make " + target + " matches of 3+ plants"
                : "Make matches of 3+ plants";
        return List.of(
                matches,
                "Drag plants to swap with neighbors",
                "Spend sun to upgrade plants on the board",
                "Don't let zombies reach your house");
    }

    private static List<String> iZombieObjectives(GameModel model) {
        if (model == null) {
            return null;
        }
        Level level = model.getCurrentLevel();
        boolean iZombie = level instanceof IZombieLevel
                || (level instanceof MiniGameLevel mini
                && mini.getMiniGameType() == MiniGameType.I_ZOMBIE);
        if (!iZombie) {
            return null;
        }
        return List.of(
                "Spend sun to place zombies right of the red line",
                "Eat every brain on the left side of the lawn",
                "Collect sun from the glowing zombies on the right");
    }

    private static List<String> zombotanyObjectives(GameModel model) {
        if (model == null) {
            return null;
        }
        Level level = model.getCurrentLevel();
        boolean zombotany = level instanceof ZombotanyLevel
                || (level instanceof MiniGameLevel mini
                && mini.getMiniGameType() == MiniGameType.ZOMBOTANY);
        if (!zombotany) {
            return null;
        }
        return List.of(
                "Survive waves of plant-headed zombies",
                "Peashooter zombies fire peas at your plants",
                "Don't let zombies reach your house");
    }

    private static String formatTime(float seconds) {
        int s = Math.round(seconds);
        if (s < 60) {
            return s + " second" + (s == 1 ? "" : "s");
        }
        int m = s / 60;
        int rem = s % 60;
        return rem == 0
            ? m + " minute" + (m == 1 ? "" : "s")
            : m + "m " + rem + "s";
    }

    private static Texture whitePixel() {
        if (pixel == null) {
            Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pm.setColor(Color.WHITE);
            pm.fill();
            pixel = new Texture(pm);
            pm.dispose();
        }
        return pixel;
    }
}
