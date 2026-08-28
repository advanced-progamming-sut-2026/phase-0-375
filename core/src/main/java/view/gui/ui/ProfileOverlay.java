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
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import controller.ProfileMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.user.User;
import pvz.skin.BorderedTable;

import java.util.function.BiConsumer;

/**
 * Profile panel over the main hub (dim + fade).
 */
public final class ProfileOverlay {
    private static final float FIELD_WIDTH = 400f;

    // ── Profile action buttons (tune in place) ─────────────────────────────
    public static float EDIT_BTN_W = 200f;
    public static float EDIT_BTN_H = 45;
    public static float BACK_BTN_W = 200f;
    public static float BACK_BTN_H = 50f;
    /** Label scale on purple edit buttons. */
    public static float EDIT_BTN_FONT_SCALE = 0.9f;
    /** Label scale on the brown Back button. */
    public static float BACK_BTN_FONT_SCALE = 0.95f;

    public static float CARD_PAD = 40f;
    public static float INFO_PAD = 12f;
    private static final float FADE_IN = 0.11f;
    private static final float FADE_OUT = 0.07f;
    private static final Color DIM = new Color(0f, 0f, 0f, 0.55f);
    private static final Color INK = CollectionEntryOverlay.INK;
    private static final Color MUTED = CollectionEntryOverlay.MUTED;

    private static Texture pixel;

    private ProfileOverlay() {}

    public static Table create(Skin skin, BiConsumer<String, Boolean> toast, Runnable onClose,
                               Runnable onResourceBarRefresh) {
        App.getInstance().setCurrentMenu(MenuType.PROFILE);

        ProfileMenuController controller = ProfileMenuController.getInstance();
        Panel panel = new Panel(skin, controller, toast, onResourceBarRefresh);

        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setName(view.gui.screen.AbstractMenuScreen.OVERLAY_NAME);
        overlay.setBackground(new TextureRegionDrawable(whitePixel()).tint(DIM));
        overlay.setTouchable(Touchable.enabled);
        Runnable closer = () -> dismiss(overlay, onClose);
        overlay.setUserObject(closer);

        ScrollPane scroll = new ScrollPane(panel.card, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);

        overlay.add(scroll).width(560f).maxHeight(980f).pad(40f);

        panel.back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                closer.run();
            }
        });

        panel.bindStage(overlay);
        fadeIn(overlay);
        UiMotion.fadeSlideIn(panel.card, 0.35f);
        panel.refreshInfo();
        return overlay;
    }

    private static final class Panel {
        private final Skin skin;
        private final ProfileMenuController controller;
        private final BiConsumer<String, Boolean> toast;
        private final Runnable onResourceBarRefresh;
        private final BorderedTable card;
        private final TextButton back;

        private Table overlayRef;
        private Label usernameValue;
        private Label nicknameValue;
        private Label emailValue;
        private Label gamesValue;
        private Label coinsValue;
        private Label gemsValue;
        private Label levelsValue;
        private Label myopointValue;

        Panel(Skin skin, ProfileMenuController controller, BiConsumer<String, Boolean> toast,
              Runnable onResourceBarRefresh) {
            this.skin = skin;
            this.controller = controller;
            this.toast = toast;
            this.onResourceBarRefresh = onResourceBarRefresh;

            card = new BorderedTable();
            card.pad(CARD_PAD);
            Label title = new Label("Profile", skin, "big");
            title.setColor(INK);
            card.add(title).padBottom(20f).row();

            Table info = new Table();
            info.pad(INFO_PAD);
            usernameValue = addInfoRow(info, "Username");
            nicknameValue = addInfoRow(info, "Nickname");
            emailValue = addInfoRow(info, "Email");
            gamesValue = addInfoRow(info, "Games played");
            coinsValue = addInfoRow(info, "Coins");
            gemsValue = addInfoRow(info, "Gems");
            levelsValue = addInfoRow(info, "Levels completed");
            myopointValue = addInfoRow(info, "Highest myopoint");
            card.add(info).growX().padBottom(20f).row();

            card.add(editButton("Change username", this::openChangeUsername)).width(EDIT_BTN_W).height(EDIT_BTN_H)
                    .padBottom(8f).row();
            card.add(editButton("Change nickname", this::openChangeNickname)).width(EDIT_BTN_W).height(EDIT_BTN_H)
                    .padBottom(8f).row();
            card.add(editButton("Change email", this::openChangeEmail)).width(EDIT_BTN_W).height(EDIT_BTN_H)
                    .padBottom(8f).row();
            card.add(editButton("Change password", this::openChangePassword)).width(EDIT_BTN_W).height(EDIT_BTN_H)
                    .padBottom(16f).row();

            back = styledButton("Back", "brown", BACK_BTN_FONT_SCALE);
            card.add(back).width(BACK_BTN_W).height(BACK_BTN_H);
        }

        void bindStage(Table overlay) {
            this.overlayRef = overlay;
        }

        private Stage stage() {
            return overlayRef == null ? null : overlayRef.getStage();
        }

        private Label addInfoRow(Table info, String label) {
            Label key = new Label(label + ":", skin, "secondary");
            key.setColor(MUTED);
            Label value = new Label("—", skin, "medium");
            value.setColor(INK);
            info.add(key).left().padRight(20f).padBottom(10f);
            info.add(value).left().expandX().padBottom(10f).row();
            return value;
        }

        private TextButton editButton(String text, Runnable action) {
            TextButton button = styledButton(text, "purple", EDIT_BTN_FONT_SCALE);
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    action.run();
                }
            });
            return button;
        }

        private TextButton styledButton(String text, String style, float fontScale) {
            TextButton button = new TextButton(text, skin, style);
            SkinFonts.scaleButton(button, skin, style, fontScale);
            UiMotion.bindPressScale(button);
            return button;
        }

        private void refreshInfo() {
            CommandResult<User> result = controller.showInfo();
            if (!result.isSuccess() || result.getData() == null) {
                toast.accept(result.getMessage(), true);
                return;
            }
            User user = result.getData();
            usernameValue.setText(nullSafe(user.getUsername()));
            nicknameValue.setText(nullSafe(user.getNickname()));
            emailValue.setText(nullSafe(user.getEmail()));
            gamesValue.setText(String.valueOf(user.getGamesPlayed()));
            coinsValue.setText(String.valueOf(user.getCoins()));
            gemsValue.setText(String.valueOf(user.getGems()));
            int levelsCompleted = 0;
            if (user.getChapterProgress() != null) {
                levelsCompleted = user.getChapterProgress().values().stream().mapToInt(Integer::intValue).sum();
            }
            levelsValue.setText(String.valueOf(levelsCompleted));
            myopointValue.setText(String.valueOf(user.getHighestMyopoint()));
            if (onResourceBarRefresh != null) {
                onResourceBarRefresh.run();
            }
        }

        private void openChangeUsername() {
            Stage stage = stage();
            if (stage == null) {
                return;
            }
            Table body = new Table();
            TextField field = new TextField("", skin);
            field.setMessageText("New username");
            body.add(field).width(FIELD_WIDTH).height(48f).padBottom(12f).row();
            TextButton save = new TextButton("Save", skin);
            body.add(save).width(200f).height(52f);

            Table modal = ModalCard.create(skin, "Change username", body, null);
            save.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    CommandResult<Void> r = controller.changeUsername(field.getText());
                    toast.accept(r.getMessage(), !r.isSuccess());
                    if (r.isSuccess()) {
                        modal.remove();
                        refreshInfo();
                    }
                }
            });
            stage.addActor(modal);
            bringToastFront(stage);
        }

        private void openChangeNickname() {
            Stage stage = stage();
            if (stage == null) {
                return;
            }
            Table body = new Table();
            TextField field = new TextField("", skin);
            field.setMessageText("New nickname");
            body.add(field).width(FIELD_WIDTH).height(48f).padBottom(12f).row();
            TextButton save = new TextButton("Save", skin);
            body.add(save).width(200f).height(52f);

            Table modal = ModalCard.create(skin, "Change nickname", body, null);
            save.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    CommandResult<Void> r = controller.changeNickname(field.getText());
                    toast.accept(r.getMessage(), !r.isSuccess());
                    if (r.isSuccess()) {
                        modal.remove();
                        refreshInfo();
                    }
                }
            });
            stage.addActor(modal);
            bringToastFront(stage);
        }

        private void openChangeEmail() {
            Stage stage = stage();
            if (stage == null) {
                return;
            }
            Table body = new Table();
            TextField field = new TextField("", skin);
            field.setMessageText("New email");
            body.add(field).width(FIELD_WIDTH).height(48f).padBottom(12f).row();
            TextButton save = new TextButton("Save", skin);
            body.add(save).width(200f).height(52f);

            Table modal = ModalCard.create(skin, "Change email", body, null);
            save.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    CommandResult<Void> r = controller.changeEmail(field.getText());
                    toast.accept(r.getMessage(), !r.isSuccess());
                    if (r.isSuccess()) {
                        modal.remove();
                        refreshInfo();
                    }
                }
            });
            stage.addActor(modal);
            bringToastFront(stage);
        }

        private void openChangePassword() {
            Stage stage = stage();
            if (stage == null) {
                return;
            }
            Table body = new Table();
            TextField oldPassword = new TextField("", skin);
            oldPassword.setMessageText("Current password");
            oldPassword.setPasswordMode(true);
            oldPassword.setPasswordCharacter('*');
            TextField newPassword = new TextField("", skin);
            newPassword.setMessageText("New password");
            newPassword.setPasswordMode(true);
            newPassword.setPasswordCharacter('*');
            body.add(oldPassword).width(FIELD_WIDTH).height(48f).padBottom(8f).row();
            body.add(newPassword).width(FIELD_WIDTH).height(48f).padBottom(12f).row();
            TextButton save = new TextButton("Save", skin);
            body.add(save).width(200f).height(52f);

            Table modal = ModalCard.create(skin, "Change password", body, null);
            save.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    CommandResult<Void> r = controller.changePassword(
                            newPassword.getText(), oldPassword.getText());
                    toast.accept(r.getMessage(), !r.isSuccess());
                    if (r.isSuccess()) {
                        modal.remove();
                    }
                }
            });
            stage.addActor(modal);
            bringToastFront(stage);
        }

        private static void bringToastFront(Stage stage) {
            for (Actor actor : stage.getRoot().getChildren()) {
                if (actor instanceof ToastBanner toast) {
                    toast.toFront();
                    return;
                }
            }
        }

        private static String nullSafe(String value) {
            return value == null || value.isEmpty() ? "—" : value;
        }
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
