package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import model.network.enums.ReactionType;
import model.network.packet.chat.ReactionPacket;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import view.gui.anim.PamClipCache;

import java.util.function.Consumer;

/**
 * Morphing SpeechBubble2 tenpatch for multiplayer reactions.
 * Local mode: icon → picker panel → sender preview → icon.
 * Remote mode: dot pop-in → preview hold → dot pop-out.
 */
public final class ReactionBubbleWidget extends Group {
    private enum State { ICON, PANEL, PREVIEW, HIDDEN }

    private static final float CELL_W = 118f;
    private static final float CELL_H = 72f;

    private final Skin skin;
    private final TextureBank textures;
    private final PamPlayer pamPlayer;
    private final PamClipCache pamClips;
    private final ReactionBubbleLayout.Corner corner;
    private final boolean local;
    private final Consumer<ReactionPresets.Preset> onPick;
    private final ReactionBubbleLayout.Metrics metrics;

    private final Table bubble;
    private Table contentRoot;
    private Stack contentStack;
    private Table gridLayer;
    private Table previewLayer;
    private Label hintLabel;
    private Label previewLabel;
    private Table dismissLayer;
    private Actor previewContent;
    private float previewBubbleW;
    private float previewBubbleH;
    private State state = State.HIDDEN;
    private float padY;

    public ReactionBubbleWidget(
            Skin skin,
            TextureBank textures,
            PamPlayer pamPlayer,
            PamClipCache pamClips,
            ReactionBubbleLayout.Corner corner,
            boolean local,
            Consumer<ReactionPresets.Preset> onPick
    ) {
        this.skin = skin;
        this.textures = textures;
        this.pamPlayer = pamPlayer;
        this.pamClips = pamClips;
        this.corner = corner;
        this.local = local;
        this.onPick = onPick;
        this.metrics = ReactionBubbleLayout.loadMetrics(skin);
        bubble = new Table();
        bubble.setTransform(true);
        bubble.setTouchable(local ? Touchable.enabled : Touchable.disabled);
        Drawable bg = ReactionBubbleLayout.bubbleBackground(skin, corner);
        if (bg != null) {
            bubble.setBackground(bg);
        }
        addActor(bubble);
        assembleContent();
        padForCorner();
        wireLocalClick();
        if (local) {
            resetToIcon(false);
            setVisible(true);
        } else {
            setVisible(false);
            state = State.HIDDEN;
        }
    }

    private void assembleContent() {
        contentRoot = new Table();
        bubble.add(contentRoot).grow();
        hintLabel = new Label("...", skin, "medium");
        hintLabel.setAlignment(Align.center);
        hintLabel.setColor(new Color(0.2f, 0.15f, 0.1f, 1f));
        gridLayer = buildGrid();
        previewLayer = new Table();
        previewLabel = createBorderedLabel("", 0.62f);
        previewLabel.setWrap(true);
        Table hintWrap = new Table();
        hintWrap.add(hintLabel).center().expand();
        previewLayer.add(previewLabel).grow().pad(18f, 28f, 28f, 28f);
        contentStack = new Stack();
        contentStack.add(hintWrap);
        contentStack.add(gridLayer);
        contentStack.add(previewLayer);
        showContent(State.ICON);
        contentRoot.add(contentStack).grow();
    }

    private void padForCorner() {
        float tailPad = Math.max(28f, metrics.nativeH() * 0.22f);
        float sidePad = 24f;
        if (ReactionBubbleLayout.isRightCorner(corner)) {
            contentRoot.pad(sidePad, sidePad, sidePad, tailPad);
        } else {
            contentRoot.pad(sidePad, sidePad, tailPad, sidePad);
        }
    }

    private void wireLocalClick() {
        if (!local) {
            return;
        }
        bubble.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (state == State.ICON) {
                    openPanel();
                }
            }
        });
    }

    public boolean isAvailable() {
        return ReactionBubbleLayout.bubbleBackground(skin, corner) != null;
    }

    public void setPadY(float padY) {
        this.padY = padY;
    }

    public void relayout(float stageW) {
        float layoutW = bubble.getWidth();
        ReactionBubbleLayout.applyTailOrigin(bubble, corner, layoutW);
        float x = ReactionBubbleLayout.tailAnchorX(corner, stageW, layoutW);
        setPosition(x, padY);
    }

    public void showIncoming(ReactionPacket packet) {
        if (packet == null || packet.getReactionType() == ReactionType.SURRENDER) {
            return;
        }
        ReactionPresets.Preset preset = ReactionPresets.byContentId(packet.getContent());
        if (preset == null) {
            preset = new ReactionPresets.Preset(
                    packet.getReactionType(),
                    packet.getContent(),
                    packet.getContent() != null ? packet.getContent() : "?",
                    null, null, null);
        }
        showIncoming(preset);
    }

    public void showIncoming(ReactionPresets.Preset preset) {
        if (preset == null) {
            return;
        }
        clearActions();
        bubble.clearActions();
        setVisible(true);
        state = State.PREVIEW;
        setPreviewContent(preset, ReactionPresets.hasPamSequence(preset) ? this::popOutRemote : null);
        showContent(State.PREVIEW);
        layoutPreviewBounds(false);

        bubble.setScale(0f, 0f);
        bubble.getColor().a = 1f;
        ReactionBubbleLayout.applyTailOrigin(bubble, corner, bubble.getWidth());

        float stageW = getStage() != null ? getStage().getWidth() : 1302f;
        relayout(stageW);

        bubble.addAction(popInAction());

        if (!ReactionPresets.hasPamSequence(preset)) {
            addAction(Actions.sequence(
                    Actions.delay(ReactionBubbleLayout.POP_SEC + ReactionBubbleLayout.PREVIEW_HOLD_SEC),
                    Actions.run(this::popOutRemote)));
        } else {
            addAction(Actions.delay(ReactionBubbleLayout.POP_SEC,
                    Actions.run(() -> relayout(stageW))));
        }
    }

    public void playSendPreview(ReactionPresets.Preset preset, Runnable sendAction) {
        if (!local || preset == null) {
            return;
        }
        removeDismissLayer();
        state = State.PREVIEW;
        showContent(State.PREVIEW);
        if (ReactionPresets.hasPamSequence(preset)) {
            setPreviewContent(preset, this::resetToIconAnimated);
        } else {
            setPreviewContent(preset, null);
        }
        if (sendAction != null) {
            sendAction.run();
        }

        bubble.clearActions();
        bubble.addAction(Actions.sizeTo(
                previewBubbleW,
                previewBubbleH,
                ReactionBubbleLayout.PREVIEW_SEC,
                Interpolation.sineOut));

        if (!ReactionPresets.hasPamSequence(preset)) {
            addAction(Actions.sequence(
                    Actions.delay(ReactionBubbleLayout.PREVIEW_SEC + ReactionBubbleLayout.PREVIEW_HOLD_SEC),
                    Actions.run(this::resetToIconAnimated)));
        }
        addAction(Actions.delay(ReactionBubbleLayout.PREVIEW_SEC,
                Actions.run(() -> relayout(getStage() != null ? getStage().getWidth() : 1302f))));
    }

    private void popOutRemote() {
        bubble.clearActions();
        bubble.addAction(popOutAction());
        addAction(Actions.sequence(
                Actions.delay(ReactionBubbleLayout.POP_SEC),
                Actions.run(() -> {
                    state = State.HIDDEN;
                    setVisible(false);
                    clearPreviewContent();
                })));
    }

    private void openPanel() {
        if (state != State.ICON) {
            return;
        }
        state = State.PANEL;
        clearPreviewContent();
        showContent(State.PANEL);
        fadeGridIn();
        showDismissLayer();

        bubble.clearActions();
        bubble.addAction(Actions.parallel(
                Actions.sizeTo(
                        ReactionBubbleLayout.PANEL_W,
                        ReactionBubbleLayout.PANEL_H,
                        ReactionBubbleLayout.OPEN_SEC,
                        Interpolation.sineOut),
                Actions.scaleTo(
                        1f,
                        1f,
                        ReactionBubbleLayout.OPEN_SEC,
                        Interpolation.sineOut)));
        relayout(getStage() != null ? getStage().getWidth() : 1302f);
    }

    private void closePanel() {
        if (state != State.PANEL) {
            return;
        }
        removeDismissLayer();
        resetToIconAnimated();
    }

    private void resetToIconAnimated() {
        state = State.ICON;
        clearPreviewContent();
        showContent(State.ICON);

        bubble.clearActions();
        bubble.addAction(Actions.parallel(
                Actions.sizeTo(
                        metrics.nativeW(),
                        metrics.nativeH(),
                        ReactionBubbleLayout.PREVIEW_SEC,
                        Interpolation.sineOut),
                Actions.scaleTo(
                        ReactionBubbleLayout.ICON_SCALE,
                        ReactionBubbleLayout.ICON_SCALE,
                        ReactionBubbleLayout.PREVIEW_SEC,
                        Interpolation.sineOut)));
        addAction(Actions.delay(ReactionBubbleLayout.PREVIEW_SEC,
                Actions.run(() -> relayout(getStage() != null ? getStage().getWidth() : 1302f))));
    }

    private void resetToIcon(boolean relayoutNow) {
        state = State.ICON;
        clearPreviewContent();
        showContent(State.ICON);
        bubble.setSize(metrics.nativeW(), metrics.nativeH());
        bubble.setScale(ReactionBubbleLayout.ICON_SCALE, ReactionBubbleLayout.ICON_SCALE);
        ReactionBubbleLayout.applyTailOrigin(bubble, corner, metrics.nativeW());
        setSize(bubble.getWidth(), bubble.getHeight());
        if (relayoutNow) {
            relayout(getStage() != null ? getStage().getWidth() : 1302f);
        }
    }

    private void layoutPreviewBounds(boolean iconScale) {
        bubble.setSize(previewBubbleW, previewBubbleH);
        bubble.setScale(iconScale ? ReactionBubbleLayout.ICON_SCALE : 1f, 1f);
        ReactionBubbleLayout.applyTailOrigin(bubble, corner, previewBubbleW);
        setSize(bubble.getWidth(), bubble.getHeight());
    }

    private Table buildGrid() {
        Table grid = new Table();
        grid.defaults().pad(3f).size(CELL_W, CELL_H).center();
        int col = 0;
        for (ReactionPresets.Preset preset : ReactionPresets.ALL) {
            grid.add(buildPresetCell(preset));
            col++;
            if (col % 3 == 0) {
                grid.row();
            }
        }
        return grid;
    }

    private Table buildPresetCell(ReactionPresets.Preset preset) {
        float innerW = CELL_W - 8f;
        float innerH = CELL_H - 8f;
        Table cell = new Table();
        cell.setTouchable(Touchable.enabled);
        cell.add(buildPresetVisual(preset, innerW, innerH, true)).size(innerW, innerH).center();
        cell.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (onPick != null) {
                    onPick.accept(preset);
                }
            }
        });
        return cell;
    }

    private PamSequenceEffectActor createPamSequence(
            ReactionPresets.Preset preset, float scale, boolean loopPam, boolean preview) {
        String[] clips = ReactionPresets.pamClipSequence(preset);
        float offsetY = preview ? ReactionPresets.pamPreviewOffsetY(preset) : ReactionPresets.pamDrawOffsetY(preset);
        PamSequenceEffectActor seq = new PamSequenceEffectActor(pamPlayer, pamClips, preset.pamPath(), clips)
                .setEffectScale(scale)
                .setOffsetY(offsetY)
                .setLoopSequence(loopPam);
        if (preview) {
            seq.setDrawAnchorY(ReactionPresets.pamPreviewAnchorY(preset));
        }
        return seq;
    }

    private Actor buildPresetVisual(ReactionPresets.Preset preset, float w, float h, boolean loopPam) {
        if (ReactionPresets.hasPamSequence(preset) && pamPlayer != null && pamClips != null) {
            float scale = loopPam ? ReactionPresets.pamPanelScale(preset) : ReactionPresets.pamPreviewScale(preset);
            return createPamSequence(preset, scale, loopPam, !loopPam);
        }
        if (ReactionPresets.hasPam(preset) && pamPlayer != null && pamClips != null) {
            float scale = ReactionPresets.isSticker(preset) ? 0.42f : 0.55f;
            return new PamEffectActor(pamPlayer, pamClips, preset.pamPath(), preset.pamClip())
                    .setEffectScale(scale)
                    .setLooping(true);
        }
        if (ReactionPresets.hasImage(preset)) {
            Drawable drawable = UiDrawables.tryNamed(skin, preset.imageId());
            if (drawable == null && textures != null) {
                TextureRegion region = textures.region(preset.imageId().toUpperCase());
                if (region != null) {
                    drawable = new TextureRegionDrawable(region);
                }
            }
            if (drawable != null) {
                Image image = new Image(drawable);
                image.setScaling(Scaling.fit);
                return image;
            }
        }
        float textScale = preset.label().length() > 16 ? 0.48f : 0.58f;
        Label label = createBorderedLabel(preset.label(), textScale);
        Container<Label> box = new Container<>(label);
        box.fill();
        box.width(w);
        box.height(h);
        return box;
    }

    private Label createBorderedLabel(String text, float scale) {
        Label label = new Label(text, skin);
        label.setAlignment(Align.center);
        label.setWrap(true);
        BitmapFont font = SkinFonts.getScaled(skin, "medium_outline", scale);
        if (font != null) {
            label.setStyle(new Label.LabelStyle(font, Color.WHITE));
        }
        return label;
    }

    private void showContent(State target) {
        for (Actor child : contentStack.getChildren()) {
            child.setVisible(false);
        }
        switch (target) {
            case ICON -> contentStack.getChild(0).setVisible(true);
            case PANEL -> contentStack.getChild(1).setVisible(true);
            case PREVIEW -> contentStack.getChild(2).setVisible(true);
            default -> { }
        }
    }

    private void fadeGridIn() {
        for (Actor cell : gridLayer.getChildren()) {
            cell.getColor().a = 0f;
            cell.addAction(Actions.fadeIn(0.18f));
        }
    }

    private void setPreviewContent(ReactionPresets.Preset preset, Runnable onPamComplete) {
        clearPreviewContent();
        previewLabel.setVisible(false);

        float contentW = ReactionPresets.previewContentWidth(preset);
        float contentH = ReactionPresets.previewContentHeight(preset);

        if (ReactionPresets.isText(preset)) {
            previewLabel.setVisible(true);
            BitmapFont font = SkinFonts.getScaled(skin, "medium_outline", 0.62f);
            if (font != null) {
                previewLabel.setStyle(new Label.LabelStyle(font, Color.WHITE));
            }
            previewLabel.setText(preset.label());
            previewLabel.setWrap(true);
            previewLabel.setWidth(contentW);
            previewLabel.pack();
            contentW = Math.max(contentW, previewLabel.getPrefWidth());
            contentH = Math.max(contentH, previewLabel.getPrefHeight());
            previewLayer.clearChildren();
            previewLayer.add(previewLabel).width(contentW).center();
        } else if (ReactionPresets.hasPamSequence(preset) && pamPlayer != null && pamClips != null) {
            PamSequenceEffectActor seq = createPamSequence(
                    preset, ReactionPresets.pamPreviewScale(preset), false, true);
            if (onPamComplete != null) {
                seq.onComplete(onPamComplete);
            }
            previewContent = seq;
            previewLayer.clearChildren();
            previewLayer.center();
            previewLayer.add(previewContent).size(contentW, contentH).center();
        } else {
            previewContent = buildPresetVisual(preset, contentW, contentH, false);
            previewLayer.clearChildren();
            previewLayer.add(previewContent).size(contentW, contentH).center();
        }

        ReactionBubbleLayout.PreviewSize size = ReactionBubbleLayout.previewBubbleSize(metrics, contentW, contentH);
        previewBubbleW = size.width();
        previewBubbleH = size.height();
    }

    private void clearPreviewContent() {
        if (previewContent != null) {
            previewContent.remove();
            previewContent = null;
        }
        previewLayer.clearChildren();
        previewLayer.center();
        previewLayer.add(previewLabel).center();
        previewBubbleW = metrics.nativeW();
        previewBubbleH = metrics.nativeH();
    }

    private com.badlogic.gdx.scenes.scene2d.Action popInAction() {
        return Actions.scaleTo(1f, 1f, ReactionBubbleLayout.POP_SEC, Interpolation.sineOut);
    }

    private com.badlogic.gdx.scenes.scene2d.Action popOutAction() {
        return Actions.scaleTo(0f, 0f, ReactionBubbleLayout.POP_SEC, Interpolation.sineIn);
    }

    private void showDismissLayer() {
        if (getStage() == null) {
            return;
        }
        removeDismissLayer();
        dismissLayer = new Table();
        dismissLayer.setFillParent(true);
        dismissLayer.setTouchable(Touchable.enabled);
        dismissLayer.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closePanel();
            }
        });
        getStage().addActor(dismissLayer);
        dismissLayer.toBack();
        toFront();
    }

    private void removeDismissLayer() {
        if (dismissLayer != null) {
            dismissLayer.remove();
            dismissLayer = null;
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (getStage() != null && isVisible() && state != State.HIDDEN) {
            relayout(getStage().getWidth());
        }
    }

    public void dispose() {
        removeDismissLayer();
        clearPreviewContent();
    }
}
