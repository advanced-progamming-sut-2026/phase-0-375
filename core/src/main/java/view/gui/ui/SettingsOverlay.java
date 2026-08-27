package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import controller.SettingsMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.user.User;
import pvz.skin.BorderedTable;
import view.gui.audio.GameAudio;

import java.util.function.BiConsumer;

/**
 * Settings panel over the main hub (dim + fade).
 */
public final class SettingsOverlay {
    private static final float CARD_WIDTH = 720f;
    private static final float ROW_HEIGHT = 48f;
    /** Horizontal inner padding (left / right) of the settings card. */
    public static float CARD_PAD_X = 50f;
    /** Vertical inner padding (top / bottom) of the settings card. */
    public static float CARD_PAD_Y = 42f;
    private static final float FADE_IN = 0.20f;
    private static final float FADE_OUT = 0.17f;
    private static final Color DIM = new Color(0f, 0f, 0f, 0.55f);
    private static final Color INK = CollectionEntryOverlay.INK;
    private static final Color MUTED = CollectionEntryOverlay.MUTED;

    private static Texture pixel;

    private SettingsOverlay() {}

    public static Table create(Skin skin, BiConsumer<String, Boolean> toast, Runnable onClose,
                               Runnable onResourceBarRefresh) {
        App.getInstance().setCurrentMenu(MenuType.SETTINGS);
        GameAudio.get().syncFromUser();

        SettingsMenuController controller = SettingsMenuController.getInstance();
        Panel panel = new Panel(skin, controller, toast, onResourceBarRefresh);

        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setBackground(new TextureRegionDrawable(whitePixel()).tint(DIM));
        overlay.setTouchable(Touchable.enabled);

        ScrollPane scroll = new ScrollPane(panel.card, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);

        overlay.add(scroll).width(CARD_WIDTH).maxHeight(980f).pad(40f);

        TextButton back = panel.back;
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dismiss(overlay, onClose);
            }
        });

        fadeIn(overlay);
        UiMotion.fadeSlideIn(panel.card, 0.35f);
        panel.refreshFromUser();
        return overlay;
    }

    private static final class Panel {
        private final Skin skin;
        private final SettingsMenuController controller;
        private final BiConsumer<String, Boolean> toast;
        private final Runnable onResourceBarRefresh;
        private final BorderedTable card;
        private final TextButton back;

        private Label summary;
        private ButtonGroup<TextButton> difficultyGroup;
        private ButtonGroup<TextButton> speedGroup;
        private CheckBox lawnGrid;
        private CheckBox debugMode;
        private Slider musicSlider;
        private Slider sfxSlider;
        private Label musicValue;
        private Label sfxValue;
        private boolean syncing;

        Panel(Skin skin, SettingsMenuController controller, BiConsumer<String, Boolean> toast,
              Runnable onResourceBarRefresh) {
            this.skin = skin;
            this.controller = controller;
            this.toast = toast;
            this.onResourceBarRefresh = onResourceBarRefresh;
            syncing = true;

            card = new BorderedTable();
            card.pad(CARD_PAD_Y, CARD_PAD_X, CARD_PAD_Y, CARD_PAD_X);
            Label title = new Label("Settings", skin, "big");
            title.setColor(INK);
            card.add(title).padBottom(8f).row();
            summary = new Label("", skin, "secondary");
            summary.setColor(MUTED);
            summary.setWrap(true);
            card.add(summary).width(CARD_WIDTH - CARD_PAD_X * 2f - 24f).padBottom(18f).row();

            card.add(sectionLabel(skin, "Difficulty")).left().padBottom(6f).row();
            difficultyGroup = new ButtonGroup<>();
            difficultyGroup.setMaxCheckCount(1);
            difficultyGroup.setMinCheckCount(1);
            Table difficultyRow = new Table();
            for (int level = 1; level <= 5; level++) {
                TextButton button = choiceButton(skin, String.valueOf(level));
                final int chosen = level;
                button.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        if (syncing || !button.isChecked()) {
                            return;
                        }
                        restyleChoices(skin, difficultyGroup);
                        applyDifficulty(chosen);
                    }
                });
                difficultyGroup.add(button);
                difficultyRow.add(button).width(72f).height(ROW_HEIGHT).padRight(8f);
            }
            card.add(difficultyRow).left().padBottom(16f).row();

            card.add(sectionLabel(skin, "Game speed")).left().padBottom(6f).row();
            speedGroup = new ButtonGroup<>();
            speedGroup.setMaxCheckCount(1);
            speedGroup.setMinCheckCount(1);
            Table speedRow = new Table();
            for (int speed = 1; speed <= 3; speed++) {
                TextButton button = choiceButton(skin, speed + "x");
                final int chosen = speed;
                button.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        if (syncing || !button.isChecked()) {
                            return;
                        }
                        restyleChoices(skin, speedGroup);
                        applyGameSpeed(chosen);
                    }
                });
                speedGroup.add(button);
                speedRow.add(button).width(96f).height(ROW_HEIGHT).padRight(8f);
            }
            card.add(speedRow).left().padBottom(16f).row();

            card.add(sectionLabel(skin, "Display & debug")).left().padBottom(6f).row();
            lawnGrid = new CheckBox(" Show lawn grid (red lines in game)", skin);
            lawnGrid.getLabel().setColor(MUTED);
            lawnGrid.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (syncing) {
                        return;
                    }
                    applyLawnGrid(lawnGrid.isChecked());
                }
            });
            card.add(lawnGrid).left().padBottom(8f).row();

            debugMode = new CheckBox(" Debug mode (all levels / coins / gems / sun / plant food cheats)", skin);
            debugMode.getLabel().setColor(MUTED);
            debugMode.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (syncing) {
                        return;
                    }
                    applyDebugMode(debugMode.isChecked());
                }
            });
            card.add(debugMode).left().padBottom(16f).row();

            card.add(sectionLabel(skin, "Audio")).left().padBottom(6f).row();
            musicValue = new Label("100%", skin, "medium");
            musicValue.setColor(INK);
            musicSlider = volumeSlider(skin);
            musicSlider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (syncing) {
                        return;
                    }
                    float volume = musicSlider.getValue() / 100f;
                    musicValue.setText(Math.round(musicSlider.getValue()) + "%");
                    GameAudio.get().setMusicVolume(volume);
                }
            });
            musicSlider.addListener(new ClickListener() {
                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                    super.touchUp(event, x, y, pointer, button);
                    if (!syncing) {
                        applyMusicVolume(musicSlider.getValue() / 100f);
                    }
                }
            });
            card.add(volumeRow(skin, "Music", musicSlider, musicValue)).growX().padBottom(10f).row();

            sfxValue = new Label("100%", skin, "medium");
            sfxValue.setColor(INK);
            sfxSlider = volumeSlider(skin);
            sfxSlider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (syncing) {
                        return;
                    }
                    float volume = sfxSlider.getValue() / 100f;
                    sfxValue.setText(Math.round(sfxSlider.getValue()) + "%");
                    GameAudio.get().setSfxVolume(volume);
                }
            });
            sfxSlider.addListener(new ClickListener() {
                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                    super.touchUp(event, x, y, pointer, button);
                    if (!syncing) {
                        applySfxVolume(sfxSlider.getValue() / 100f);
                    }
                }
            });
            card.add(volumeRow(skin, "SFX", sfxSlider, sfxValue)).growX().padBottom(20f).row();

            back = new TextButton("Back", skin, "brown");
            UiMotion.bindPressScale(back);
            card.add(back).width(220f).height(56f);
        }

        void refreshFromUser() {
            CommandResult<User> result = controller.showSettings();
            if (!result.isSuccess() || result.getData() == null) {
                toast.accept(result.getMessage(), true);
                return;
            }
            User user = result.getData();
            summary.setText(result.getMessage());

            syncing = true;
            selectDifficulty(user.getDifficultyLevel());
            selectSpeed(user.getGameSpeed());
            lawnGrid.setChecked(user.isShowLawnGrid());
            debugMode.setChecked(user.isDebugMode());
            musicSlider.setValue(user.getMusicVolume() * 100f);
            sfxSlider.setValue(user.getSfxVolume() * 100f);
            musicValue.setText(Math.round(user.getMusicVolume() * 100f) + "%");
            sfxValue.setText(Math.round(user.getSfxVolume() * 100f) + "%");
            syncing = false;
        }

        private void selectDifficulty(int level) {
            int index = Math.max(0, Math.min(level, 5) - 1);
            if (index < difficultyGroup.getButtons().size) {
                String checkedText = difficultyGroup.getButtons().get(index).getText().toString();
                difficultyGroup.setChecked(checkedText);
            }
            restyleChoices(skin, difficultyGroup);
        }

        private void selectSpeed(int speed) {
            int index = Math.max(0, Math.min(speed, 3) - 1);
            if (index < speedGroup.getButtons().size) {
                String checkedText = speedGroup.getButtons().get(index).getText().toString();
                speedGroup.setChecked(checkedText);
            }
            restyleChoices(skin, speedGroup);
        }

        private void applyDifficulty(int level) {
            CommandResult<Void> r = controller.changeDifficulty(level);
            toast.accept(r.getMessage(), !r.isSuccess());
            if (r.isSuccess()) {
                refreshSummary();
            } else {
                refreshFromUser();
            }
        }

        private void applyGameSpeed(int speed) {
            CommandResult<Void> r = controller.changeGameSpeed(speed);
            toast.accept(r.getMessage(), !r.isSuccess());
            if (r.isSuccess()) {
                refreshSummary();
            } else {
                refreshFromUser();
            }
        }

        private void applyLawnGrid(boolean enabled) {
            CommandResult<Void> r = controller.setShowLawnGrid(enabled);
            toast.accept(r.getMessage(), !r.isSuccess());
            if (r.isSuccess()) {
                refreshSummary();
            } else {
                refreshFromUser();
            }
        }

        private void applyDebugMode(boolean enabled) {
            CommandResult<Void> r = controller.setDebugMode(enabled);
            toast.accept(r.getMessage(), !r.isSuccess());
            if (r.isSuccess()) {
                refreshSummary();
                if (onResourceBarRefresh != null) {
                    onResourceBarRefresh.run();
                }
            } else {
                refreshFromUser();
            }
        }

        private void applyMusicVolume(float volume) {
            CommandResult<Void> r = controller.setMusicVolume(volume);
            if (r.isSuccess()) {
                GameAudio.get().setMusicVolume(volume);
                refreshSummary();
            } else {
                toast.accept(r.getMessage(), true);
                refreshFromUser();
            }
        }

        private void applySfxVolume(float volume) {
            CommandResult<Void> r = controller.setSfxVolume(volume);
            if (r.isSuccess()) {
                GameAudio.get().setSfxVolume(volume);
                refreshSummary();
            } else {
                toast.accept(r.getMessage(), true);
                refreshFromUser();
            }
        }

        private void refreshSummary() {
            CommandResult<User> result = controller.showSettings();
            if (result.isSuccess()) {
                summary.setText(result.getMessage());
            }
        }
    }

    private static Label sectionLabel(Skin skin, String text) {
        Label label = new Label(text, skin, "medium");
        label.setColor(INK);
        return label;
    }

    private static TextButton choiceButton(Skin skin, String text) {
        TextButton button = new TextButton(text, skin, "purple");
        UiMotion.bindPressScale(button);
        return button;
    }

    private static void restyleChoices(Skin skin, ButtonGroup<TextButton> group) {
        for (TextButton button : group.getButtons()) {
            String style = button.isChecked() ? "green" : "purple";
            button.setStyle(skin.get(style, TextButton.TextButtonStyle.class));
        }
    }

    private static Slider volumeSlider(Skin skin) {
        return new Slider(0f, 100f, 1f, false, skin);
    }

    private static Table volumeRow(Skin skin, String title, Slider slider, Label value) {
        Table row = new Table();
        Label titleLabel = new Label(title, skin, "secondary");
        titleLabel.setColor(MUTED);
        row.add(titleLabel).width(90f).left();
        row.add(slider).growX().height(36f).padRight(12f);
        row.add(value).width(64f).right();
        return row;
    }

    private static void fadeIn(Table overlay) {
        overlay.getColor().a = 0f;
        overlay.addAction(Actions.fadeIn(FADE_IN));
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
