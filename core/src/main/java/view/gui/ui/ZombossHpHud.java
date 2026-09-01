package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import model.enums.ZombieBehaviorType;
import model.game.core.GameModel;
import model.game.level.special.ZombossLevel;
import model.zombie.behavior.zomboss.ZombossBehavior;
import model.zombie.behavior.zomboss.ZombossPhase;
import model.zombie.instance.ZombieInstance;
import pvz.libpvz.pam.PamPlayer;
import view.gui.anim.PamClipCache;
import view.gui.assets.EffectPamPaths;

/**
 * Three-segment Zomboss HP meter with narration-icon portrait, shown instead of
 * the wave progress bar.
 */
public final class ZombossHpHud extends WidgetGroup implements Disposable {
    private static final float BAR_W = 320f;
    private static final float BAR_H = 36f;
    private static final float PAD = 3f;
    private static final float ICON_SIZE = 56f;
    private static final float ICON_GAP = 4f;
    private static final float GROUP_PAD_X = 10f;
    private static final float GROUP_PAD_Y = 10f;
    private static final float ICON_SCALE = 0.20f;
    private static final Color TRACK = new Color(0.12f, 0.06f, 0.06f, 0.92f);
    private static final Color FILL = new Color(0.82f, 0.18f, 0.14f, 1f);
    private static final Color STUN_FILL = new Color(0.95f, 0.75f, 0.2f, 1f);
    private static final Color DIVIDER = new Color(0.08f, 0.04f, 0.04f, 0.95f);
    private static final Color BACKDROP = new Color(0.08f, 0.08f, 0.1f, 0.72f);

    private final float groupW;
    private final float groupH;
    private Texture pixel;
    private Image fill;
    private final Image[] dividers = new Image[2];
    private final PamClipCache clips;
    private final float barX;
    private final float barY;
    private final float innerW;
    private final float innerH;

    public ZombossHpHud(Skin skin) {
        this(skin, null);
    }

    public ZombossHpHud(Skin skin, PamPlayer player) {
        clips = player != null ? new PamClipCache(player) : null;
        float contentW = ICON_SIZE + ICON_GAP + BAR_W;
        float contentH = Math.max(ICON_SIZE, BAR_H);
        groupW = contentW + GROUP_PAD_X * 2f;
        groupH = contentH + GROUP_PAD_Y * 2f;
        setSize(groupW, groupH);
        setTouchable(Touchable.disabled);
        TextureRegionDrawable white = whiteDrawable();
        addBackdrop(white);
        float contentX = GROUP_PAD_X;
        float contentY = GROUP_PAD_Y;
        barX = contentX + ICON_SIZE + ICON_GAP;
        barY = contentY + (contentH - BAR_H) * 0.5f;
        innerW = BAR_W - PAD * 2f;
        innerH = BAR_H - PAD * 2f;
        addIcon(player, contentX, contentY, contentH);
        addBar(white);
    }

    private TextureRegionDrawable whiteDrawable() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();
        return new TextureRegionDrawable(new TextureRegion(pixel));
    }

    private void addBackdrop(TextureRegionDrawable white) {
        Image backdrop = new Image(white);
        backdrop.setColor(BACKDROP);
        backdrop.setSize(groupW, groupH);
        backdrop.setPosition(0f, 0f);
        backdrop.setTouchable(Touchable.disabled);
        addActor(backdrop);
    }

    private void addIcon(PamPlayer player, float contentX, float contentY, float contentH) {
        if (player == null || clips == null) {
            return;
        }
        PamEffectActor icon = new PamEffectActor(
                player, clips,
                EffectPamPaths.NARRATIONICONS_ZOMBOSS,
                EffectPamPaths.NARRATIONICONS_ZOMBOSS_IDLE);
        icon.setSize(ICON_SIZE, ICON_SIZE);
        icon.setPosition(contentX, contentY + (contentH - ICON_SIZE) * 0.5f);
        icon.setEffectScale(ICON_SCALE);
        icon.setLooping(true);
        icon.setColor(Color.WHITE);
        icon.setTouchable(Touchable.disabled);
        addActor(icon);
    }

    private void addBar(TextureRegionDrawable white) {
        Image track = new Image(white);
        track.setColor(TRACK);
        track.setSize(BAR_W, BAR_H);
        track.setPosition(barX, barY);
        track.setTouchable(Touchable.disabled);
        addActor(track);
        fill = new Image(white);
        fill.setColor(FILL);
        fill.setSize(innerW, innerH);
        fill.setPosition(barX + PAD, barY + PAD);
        fill.setTouchable(Touchable.disabled);
        addActor(fill);
        for (int i = 0; i < dividers.length; i++) {
            Image d = new Image(white);
            d.setColor(DIVIDER);
            d.setSize(3f, innerH);
            d.setPosition(barX, barY + PAD);
            d.setTouchable(Touchable.disabled);
            dividers[i] = d;
            addActor(d);
        }
    }

    public void sync(GameModel model) {
        if (!showFor(model)) {
            setVisible(false);
            return;
        }
        setVisible(true);
        ZombieInstance boss = model.findZomboss();
        float progress = 1f;
        boolean stunned = false;
        int phases = ZombossBehavior.PHASE_COUNT;
        if (boss != null) {
            ZombossBehavior behavior = (ZombossBehavior) boss.getBehavior(ZombieBehaviorType.ZOMBOSS);
            if (behavior != null) {
                progress = behavior.healthProgress01(boss);
                stunned = behavior.getPhase() == ZombossPhase.STUNNED;
                phases = behavior.getPhaseCount();
            } else if (boss.getDefinition() != null) {
                progress = boss.getCurrentHP() / (float) Math.max(1, boss.getCurrentHP());
            }
        }
        fill.setWidth(Math.max(0f, innerW * progress));
        fill.setHeight(innerH);
        fill.setPosition(barX + PAD, barY + PAD);
        fill.setColor(stunned ? STUN_FILL : FILL);

        for (int i = 0; i < dividers.length; i++) {
            float x = barX + PAD + innerW * ((i + 1f) / phases);
            dividers[i].setPosition(x - 1.5f, barY + PAD);
            dividers[i].setVisible(true);
        }
    }

    public static boolean showFor(GameModel model) {
        return model != null && model.getCurrentLevel() instanceof ZombossLevel;
    }

    @Override
    public float getPrefWidth() {
        return groupW;
    }

    @Override
    public float getPrefHeight() {
        return groupH;
    }

    @Override
    public float getMinWidth() {
        return groupW;
    }

    @Override
    public float getMinHeight() {
        return groupH;
    }

    @Override
    public void dispose() {
        if (pixel != null) {
            pixel.dispose();
        }
    }
}
