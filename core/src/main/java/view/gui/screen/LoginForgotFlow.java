package view.gui.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import controller.LoginMenuController;
import controller.result.CommandResult;
import view.gui.ui.CollectionEntryOverlay;
import view.gui.ui.SkinFonts;

/** Multi-step forgot-password body wired by {@link LoginScreen}. */
final class LoginForgotFlow {
    private final Skin skin;
    private final LoginMenuController controller;
    private final Table body = new Table();
    private final TextField userField;
    private final TextField emailField;
    private final Label questionHeading;
    private final Label resetPrompt;
    private final TextField resetField;
    private final TextButton resetAction;
    private int resetStep;

    LoginForgotFlow(Skin skin, LoginMenuController controller, String username) {
        this.skin = skin;
        this.controller = controller;
        userField = new TextField(username, skin);
        userField.setMessageText("Username");
        emailField = new TextField("", skin);
        emailField.setMessageText("Email");
        questionHeading = new Label("Security question:", skin, "medium");
        questionHeading.setColor(CollectionEntryOverlay.MUTED);
        resetPrompt = new Label("Enter username and email to begin reset.", skin);
        resetPrompt.setWrap(true);
        resetPrompt.setColor(CollectionEntryOverlay.INK);
        resetField = new TextField("", skin);
        resetAction = new TextButton("Continue", skin, "purple");
        showIdentityStep();
    }

    Table body() {
        return body;
    }

    TextButton actionButton() {
        return resetAction;
    }

    void reset() {
        resetStep = 0;
        controller.clearPendingForgotState();
    }

    ChangeListener listener(Table overlay, java.util.function.BiConsumer<String, Boolean> toast) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                handleStep(overlay, toast);
            }
        };
    }

    private void handleStep(Table overlay, java.util.function.BiConsumer<String, Boolean> toast) {
        if (resetStep == 0) {
            advanceFromIdentity(toast);
        } else if (resetStep == 1) {
            advanceFromAnswer(overlay, toast);
        } else {
            finishReset(overlay, toast);
        }
    }

    private void advanceFromIdentity(java.util.function.BiConsumer<String, Boolean> toast) {
        CommandResult<Void> r = controller.forgetPassword(userField.getText(), emailField.getText());
        toast.accept(r.getMessage(), !r.isSuccess());
        if (!r.isSuccess()) {
            return;
        }
        resetStep = 1;
        resetPrompt.setText(r.getMessage());
        SkinFonts.scaleLabel(resetPrompt, skin, "secondary", 1.28f);
        resetField.setPasswordMode(false);
        resetField.setText("");
        resetField.setMessageText("Your answer");
        resetAction.setText("Submit answer");
        showAnswerStep();
    }

    private void advanceFromAnswer(Table overlay, java.util.function.BiConsumer<String, Boolean> toast) {
        CommandResult<Void> r = controller.answer(resetField.getText());
        toast.accept(r.getMessage(), !r.isSuccess());
        if (!r.isSuccess()) {
            overlay.remove();
            resetStep = 0;
            return;
        }
        resetStep = 2;
        resetPrompt.setText(r.getMessage());
        SkinFonts.scaleLabel(resetPrompt, skin, "secondary", 1f);
        resetField.setText("");
        resetField.setPasswordMode(true);
        resetField.setPasswordCharacter('*');
        resetField.setMessageText("New password");
        resetAction.setText("Update password");
        showPasswordStep();
    }

    private void finishReset(Table overlay, java.util.function.BiConsumer<String, Boolean> toast) {
        CommandResult<Void> r = controller.resetPassword(resetField.getText());
        toast.accept(r.getMessage(), !r.isSuccess());
        if (r.isSuccess()) {
            overlay.remove();
            resetStep = 0;
        }
    }

    private void showIdentityStep() {
        body.clearChildren();
        SkinFonts.scaleLabel(resetPrompt, skin, "secondary", 1f);
        body.add(resetPrompt).width(400f).left().padBottom(12f).row();
        body.add(userField).width(400f).height(48f).padBottom(8f).row();
        body.add(emailField).width(400f).height(48f).padBottom(8f).row();
        body.add(resetAction).width(220f).height(56f);
        body.invalidateHierarchy();
    }

    private void showAnswerStep() {
        body.clearChildren();
        body.add(questionHeading).width(400f).left().padBottom(6f).row();
        body.add(resetPrompt).width(400f).left().padBottom(14f).row();
        body.add(resetField).width(400f).height(48f).padBottom(12f).row();
        body.add(resetAction).width(220f).height(56f);
        body.invalidateHierarchy();
    }

    private void showPasswordStep() {
        body.clearChildren();
        body.add(resetPrompt).width(400f).left().padBottom(14f).row();
        body.add(resetField).width(400f).height(48f).padBottom(12f).row();
        body.add(resetAction).width(220f).height(56f);
        body.invalidateHierarchy();
    }
}
