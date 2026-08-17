package view.gui.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import controller.SettingsMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.user.User;
import pvz.skin.BorderedTable;
import view.gui.PvzGdxGame;
import view.gui.audio.GameAudio;
import view.gui.ui.ResourceBar;
import view.gui.ui.UiMotion;

/**
 * Settings menu: difficulty, game speed, lawn grid, debug mode,
 * plus music/SFX volume.
 */
public final class SettingsScreen extends AbstractMenuScreen {
    private static final float CARD_WIDTH = 640f;
    private static final float ROW_HEIGHT = 48f;

    private final SettingsMenuController controller = SettingsMenuController.getInstance();

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

    public SettingsScreen(PvzGdxGame game) {
        super(game);
    }

    @Override
    protected void buildUi() {
        App.getInstance().setCurrentMenu(MenuType.SETTINGS);
        GameAudio.get().syncFromUser();
        syncing = true;

        Table top = new Table();
        top.setFillParent(true);
        top.top();
        top.add(new ResourceBar(skin)).expandX().right().pad(12f);
        stage.addActor(top);

        BorderedTable card = new BorderedTable();
        card.pad(28f);
        card.add(new Label("Settings", skin, "big")).padBottom(8f).row();
        summary = new Label("", skin, "secondary");
        summary.setWrap(true);
        card.add(summary).width(CARD_WIDTH - 56f).padBottom(18f).row();

        card.add(sectionLabel("Difficulty")).left().padBottom(6f).row();
        difficultyGroup = new ButtonGroup<>();
        difficultyGroup.setMaxCheckCount(1);
        difficultyGroup.setMinCheckCount(1);
        Table difficultyRow = new Table();
        for (int level = 1; level <= 5; level++) {
            TextButton button = choiceButton(String.valueOf(level));
            final int chosen = level;
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (syncing || !button.isChecked()) {
                        return;
                    }
                    restyleChoices(difficultyGroup);
                    applyDifficulty(chosen);
                }
            });
            difficultyGroup.add(button);
            difficultyRow.add(button).width(72f).height(ROW_HEIGHT).padRight(8f);
        }
        card.add(difficultyRow).left().padBottom(16f).row();

        card.add(sectionLabel("Game speed")).left().padBottom(6f).row();
        speedGroup = new ButtonGroup<>();
        speedGroup.setMaxCheckCount(1);
        speedGroup.setMinCheckCount(1);
        Table speedRow = new Table();
        for (int speed = 1; speed <= 3; speed++) {
            TextButton button = choiceButton(speed + "x");
            final int chosen = speed;
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (syncing || !button.isChecked()) {
                        return;
                    }
                    restyleChoices(speedGroup);
                    applyGameSpeed(chosen);
                }
            });
            speedGroup.add(button);
            speedRow.add(button).width(96f).height(ROW_HEIGHT).padRight(8f);
        }
        card.add(speedRow).left().padBottom(16f).row();

        card.add(sectionLabel("Display & debug")).left().padBottom(6f).row();
        lawnGrid = new CheckBox(" Show lawn grid (red lines in game)", skin);
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

        card.add(sectionLabel("Audio")).left().padBottom(6f).row();
        musicValue = new Label("100%", skin, "medium");
        musicSlider = volumeSlider();
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
        card.add(volumeRow("Music", musicSlider, musicValue)).growX().padBottom(10f).row();

        sfxValue = new Label("100%", skin, "medium");
        sfxSlider = volumeSlider();
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
        card.add(volumeRow("SFX", sfxSlider, sfxValue)).growX().padBottom(20f).row();

        TextButton back = new TextButton("Back", skin, "brown");
        UiMotion.bindPressScale(back);
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = controller.menuExit();
                showToast(r.getMessage(), !r.isSuccess());
                if (r.isSuccess()) {
                    game.setScreen(new MainHubScreen(game));
                }
            }
        });
        card.add(back).width(220f).height(56f);

        ScrollPane scroll = new ScrollPane(card, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);

        Table root = new Table();
        root.setFillParent(true);
        root.add(scroll).width(CARD_WIDTH).maxHeight(UI_HEIGHT - 64f);
        stage.addActor(root);

        UiMotion.fadeSlideIn(card, 0.35f);
        refreshFromUser();
    }

    private Label sectionLabel(String text) {
        return new Label(text, skin, "medium");
    }

    private TextButton choiceButton(String text) {
        TextButton button = new TextButton(text, skin, "purple");
        UiMotion.bindPressScale(button);
        return button;
    }

    private void restyleChoices(ButtonGroup<TextButton> group) {
        for (TextButton button : group.getButtons()) {
            String style = button.isChecked() ? "green" : "purple";
            button.setStyle(skin.get(style, TextButton.TextButtonStyle.class));
        }
    }

    private Slider volumeSlider() {
        return new Slider(0f, 100f, 1f, false, skin);
    }

    private Table volumeRow(String title, Slider slider, Label value) {
        Table row = new Table();
        row.add(new Label(title, skin, "secondary")).width(90f).left();
        row.add(slider).growX().height(36f).padRight(12f);
        row.add(value).width(64f).right();
        return row;
    }

    private void refreshFromUser() {
        CommandResult<User> result = controller.showSettings();
        if (!result.isSuccess() || result.getData() == null) {
            showToast(result.getMessage(), true);
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
        restyleChoices(difficultyGroup);
    }

    private void selectSpeed(int speed) {
        int index = Math.max(0, Math.min(speed, 3) - 1);
        if (index < speedGroup.getButtons().size) {
            String checkedText = speedGroup.getButtons().get(index).getText().toString();
            speedGroup.setChecked(checkedText);
        }
        restyleChoices(speedGroup);
    }

    private void applyDifficulty(int level) {
        CommandResult<Void> r = controller.changeDifficulty(level);
        showToast(r.getMessage(), !r.isSuccess());
        if (r.isSuccess()) {
            refreshSummary();
        } else {
            refreshFromUser();
        }
    }

    private void applyGameSpeed(int speed) {
        CommandResult<Void> r = controller.changeGameSpeed(speed);
        showToast(r.getMessage(), !r.isSuccess());
        if (r.isSuccess()) {
            refreshSummary();
        } else {
            refreshFromUser();
        }
    }

    private void applyLawnGrid(boolean enabled) {
        CommandResult<Void> r = controller.setShowLawnGrid(enabled);
        showToast(r.getMessage(), !r.isSuccess());
        if (r.isSuccess()) {
            refreshSummary();
        } else {
            refreshFromUser();
        }
    }

    private void applyDebugMode(boolean enabled) {
        CommandResult<Void> r = controller.setDebugMode(enabled);
        showToast(r.getMessage(), !r.isSuccess());
        if (r.isSuccess()) {
            refreshSummary();
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
            showToast(r.getMessage(), true);
            refreshFromUser();
        }
    }

    private void applySfxVolume(float volume) {
        CommandResult<Void> r = controller.setSfxVolume(volume);
        if (r.isSuccess()) {
            GameAudio.get().setSfxVolume(volume);
            refreshSummary();
        } else {
            showToast(r.getMessage(), true);
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
