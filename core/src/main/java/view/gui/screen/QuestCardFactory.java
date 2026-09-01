package view.gui.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import model.quest.QuestReward;
import pvz.libpvz.textures.TextureBank;
import view.gui.PvzGdxGame;
import view.gui.anim.PamClipCache;
import view.gui.assets.QuestArt;
import view.gui.ui.PamEffectActor;
import view.gui.ui.RoundedRegionImage;
import view.gui.ui.SkinFonts;

final class QuestCardFactory {
    static final float CARD_GAP = 20f;
    static final float CARD_SIDE = 300f;
    private static final float CARD_RADIUS = 8f;
    private static final float CARD_BORDER = 3f;
    private static final float BODY_PAD_TOP = -8f;
    private static final float BODY_PAD_LEFT = 24f;
    private static final float BODY_PAD_BOTTOM = -10f;
    private static final float BODY_PAD_RIGHT = 26f;
    private static final float ICON_SIZE = 72f;
    private static final float ICON_FRAME = 4f;
    private static final float BTN_W = 150f;
    private static final float BTN_H = 52f;
    private static final float BTN_SHIFT_Y = 0f;
    private static final float DAILY_CLOCK_SIZE = 56f;
    private static final float DAILY_CLOCK_PAD = 4f;
    private static final float DAILY_CLOCK_SCALE = 0.3f;
    private static final float REWARD_COL_W = 100f;
    private static final float REWARD_ICON_COIN = 58f;
    private static final float REWARD_ICON_GEM = 50f;
    private static final float REWARD_ICON_SEED = 90f;
    private static final float REWARD_ICON_NEW_PLANT = 70f;
    private static final float TEXT_TITLE = 1.2f;
    private static final float TEXT_DESC = 1.2f;
    private static final float TEXT_PROGRESS = 1f;
    private static final float TEXT_REWARD = 1.5f;
    private static final float TEXT_BUTTON = 1f;
    private static final Color INK = new Color(0.12f, 0.10f, 0.12f, 1f);
    private static final Color CREAM = new Color(0.93f, 0.88f, 0.76f, 1f);
    private static final Color ICON_WELL = new Color(0.82f, 0.76f, 0.62f, 1f);
    private static final Color READY_BORDER = new Color(0.28f, 0.72f, 0.22f, 1f);
    private static final Color BAR_TEXT = new Color(1f, 1f, 1f, 1f);
    private static final Color REWARD_AMT = new Color(0.15f, 0.10f, 0.08f, 1f);

    private final Skin skin;
    private final TextureBank textures;
    private final Texture whitePixel;
    private final TextureRegion lawnColumnRegion;
    private final PvzGdxGame game;
    private final PamClipCache pamClips;

    QuestCardFactory(Skin skin, TextureBank textures, Texture whitePixel,
                     TextureRegion lawnColumnRegion, PvzGdxGame game, PamClipCache pamClips) {
        this.skin = skin;
        this.textures = textures;
        this.whitePixel = whitePixel;
        this.lawnColumnRegion = lawnColumnRegion;
        this.game = game;
        this.pamClips = pamClips;
    }

    Table questRow(String title, String description, String iconId,
                   int current, int target, boolean ready, boolean dailyClock,
                   QuestReward reward, int fallbackCoins, TextButton action) {
        float midW = midColumnWidth();
        Stack stack = new Stack();
        stack.add(roundedFill(ready ? READY_BORDER : CREAM, CARD_RADIUS));
        Table plate = new Table();
        plate.pad(CARD_BORDER);
        Stack plateStack = new Stack();
        plateStack.add(roundedFill(CREAM, Math.max(2f, CARD_RADIUS - 2f)));
        Table body = buildRowBody(title, description, iconId, current, target, ready,
                reward, fallbackCoins, action, midW);
        Table bodySlot = new Table();
        bodySlot.add(body).growX().expandY().center();
        plateStack.add(bodySlot);
        plate.add(plateStack).grow();
        stack.add(plate);
        addDailyClock(stack, dailyClock);
        Table root = new Table();
        root.add(stack).growX();
        return root;
    }

    private Table buildRowBody(String title, String description, String iconId,
                               int current, int target, boolean ready,
                               QuestReward reward, int fallbackCoins, TextButton action,
                               float midW) {
        Table body = new Table();
        body.pad(BODY_PAD_TOP, BODY_PAD_LEFT, BODY_PAD_BOTTOM, BODY_PAD_RIGHT);
        body.add(questIcon(iconId)).size(ICON_SIZE, ICON_SIZE).padRight(14f);
        body.add(midColumn(title, description, current, target, ready, midW))
                .growX().left().padRight(12f);
        body.add(rewardColumn(reward, fallbackCoins)).width(REWARD_COL_W).center().padRight(12f);
        SkinFonts.scaleButton(action, skin, "purple", TEXT_BUTTON);
        body.add(action).width(BTN_W).height(BTN_H).padTop(BTN_SHIFT_Y);
        return body;
    }

    private Table midColumn(String title, String description, int current, int target,
                            boolean ready, float midW) {
        Label name = new Label(title == null ? "" : title, skin, "medium");
        name.setColor(INK);
        name.setWrap(true);
        SkinFonts.scaleLabel(name, skin, "medium", TEXT_TITLE);
        String descText = description == null ? "" : description.trim();
        Table mid = new Table();
        mid.add(name).width(midW).left().row();
        if (!descText.isEmpty()) {
            Label desc = new Label(descText, skin, "secondary");
            desc.setColor(new Color(0.35f, 0.32f, 0.30f, 1f));
            desc.setWrap(true);
            SkinFonts.scaleLabel(desc, skin, "secondary", TEXT_DESC);
            mid.add(desc).width(midW).left().padTop(2f).row();
        }
        mid.add(progressWrap(current, target, ready)).width(midW).padTop(6f);
        return mid;
    }

    private Table progressWrap(int current, int target, boolean ready) {
        ProgressBar bar = new ProgressBar(0f, Math.max(1, target), 1f, false,
                skin.get(ready || current >= target ? "xp_green" : "xp_yellow",
                        ProgressBar.ProgressBarStyle.class));
        bar.setAnimateDuration(0f);
        bar.setValue(Math.min(current, Math.max(1, target)));
        bar.setTouchable(Touchable.disabled);
        Label progressLabel = new Label(current + "/" + target, skin, "secondary");
        progressLabel.setColor(BAR_TEXT);
        progressLabel.setAlignment(Align.center);
        SkinFonts.scaleLabel(progressLabel, skin, "secondary", TEXT_PROGRESS);
        Table barWrap = new Table();
        barWrap.stack(bar, progressLabel).growX().height(22f * Math.max(1f, TEXT_PROGRESS));
        return barWrap;
    }

    private void addDailyClock(Stack stack, boolean dailyClock) {
        if (!dailyClock || pamClips == null || game.assets == null) {
            return;
        }
        Table corner = new Table();
        corner.setFillParent(true);
        corner.top().left();
        PamEffectActor clock = new PamEffectActor(
                game.assets.player, pamClips, QuestArt.PAM_DAILY_CLOCK, QuestArt.PAM_CLIP);
        clock.setEffectScale(DAILY_CLOCK_SCALE);
        corner.add(clock).size(DAILY_CLOCK_SIZE, DAILY_CLOCK_SIZE)
                .padTop(DAILY_CLOCK_PAD).padLeft(DAILY_CLOCK_PAD);
        stack.add(corner);
    }

    private static float midColumnWidth() {
        float w = AbstractMenuScreenUi.UI_WIDTH - 28f * 2f - CARD_SIDE * 2f;
        w -= CARD_BORDER * 2f;
        w -= BODY_PAD_LEFT + BODY_PAD_RIGHT;
        w -= ICON_SIZE + 14f;
        w -= REWARD_COL_W + 12f;
        w -= BTN_W;
        return Math.max(80f, w);
    }

    static String miniGameIconId(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return QuestArt.ICON_ZOMBIE;
        }
        String key = rawType.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        return switch (key) {
            case "VASE_BREAKER", "VASEBREAKER" -> QuestArt.ICON_VASE_BREAKER;
            case "BEGHOULED" -> QuestArt.ICON_BEGHOULED;
            case "WALLNUT_BOWLING", "WALL_NUT_BOWLING", "BOWLING" -> QuestArt.ICON_BOWLING;
            case "I_ZOMBIE", "IZOMBIE" -> QuestArt.ICON_GARGANTUAR;
            case "ZOMBOTANY" -> QuestArt.ICON_ZOMBIE;
            default -> QuestArt.ICON_ZOMBIE;
        };
    }

    private Actor rewardColumn(QuestReward reward, int fallbackCoins) {
        Table col = new Table();
        boolean any = addRewardChips(col, reward);
        if (!any && fallbackCoins > 0) {
            col.add(rewardChip(QuestArt.COIN_ICON, fallbackCoins, REWARD_ICON_COIN, null)).row();
            any = true;
        }
        if (!any) {
            Label none = new Label("—", skin, "secondary");
            none.setColor(REWARD_AMT);
            none.setAlignment(Align.center);
            col.add(none);
        }
        return col;
    }

    private boolean addRewardChips(Table col, QuestReward reward) {
        boolean any = false;
        if (reward != null && reward.getCoinAmount() > 0) {
            col.add(rewardChip(QuestArt.COIN_ICON, reward.getCoinAmount(), REWARD_ICON_COIN, null))
                    .padBottom(4f).row();
            any = true;
        }
        if (reward != null && reward.getGemAmount() > 0) {
            col.add(rewardChip(QuestArt.GEM_ICON, reward.getGemAmount(), REWARD_ICON_GEM, null))
                    .padBottom(4f).row();
            any = true;
        }
        if (reward != null && reward.getInventoryItem() != null && !reward.getInventoryItem().isBlank()
                && reward.getInventoryItemAmount() > 0) {
            col.add(rewardChip(QuestArt.REWARD_SEED_PACKET, reward.getInventoryItemAmount(),
                    REWARD_ICON_SEED, null)).padBottom(4f).row();
            any = true;
        }
        if (reward != null && reward.getUnlockableName() != null && !reward.getUnlockableName().isBlank()) {
            col.add(rewardChip(QuestArt.REWARD_NEW_PLANT, 0, REWARD_ICON_NEW_PLANT, "New plant"))
                    .padBottom(4f).row();
            any = true;
        }
        return any;
    }

    private Table rewardChip(String iconId, int amount, float iconSize, String caption) {
        Table chip = new Table();
        TextureRegion region = textures == null ? null : textures.region(iconId);
        if (region != null) {
            Image icon = new Image(new TextureRegionDrawable(region));
            icon.setScaling(Scaling.fit);
            chip.add(icon).size(iconSize, iconSize).row();
        }
        if (amount > 0) {
            Label amt = new Label("x" + amount, skin, "secondary");
            amt.setColor(REWARD_AMT);
            amt.setAlignment(Align.center);
            SkinFonts.scaleLabel(amt, skin, "secondary", TEXT_REWARD);
            chip.add(amt);
        } else if (caption != null && !caption.isBlank()) {
            Label label = new Label(caption, skin, "secondary");
            label.setColor(REWARD_AMT);
            label.setAlignment(Align.center);
            SkinFonts.scaleLabel(label, skin, "secondary", TEXT_REWARD * 0.85f);
            chip.add(label);
        }
        return chip;
    }

    private Actor questIcon(String iconId) {
        Stack well = new Stack();
        well.add(roundedFill(ICON_WELL, 6f));
        Table pad = new Table();
        pad.pad(ICON_FRAME);
        String id = iconId == null || iconId.isBlank() ? QuestArt.ICON_ZOMBIE : iconId;
        if (QuestArt.ICON_LAWN_CROSS.equals(id)) {
            pad.add(lawnCross()).grow();
        } else {
            addResolvedIcon(pad, id);
        }
        well.add(pad);
        return well;
    }

    private Stack lawnCross() {
        Stack cross = new Stack();
        TextureRegion row = textures == null ? null : textures.region(QuestArt.ICON_LAWN_ROW);
        if (row != null) {
            Image rowImg = new Image(new TextureRegionDrawable(row));
            rowImg.setScaling(Scaling.fit);
            cross.add(rowImg);
        }
        if (lawnColumnRegion != null) {
            Image colImg = new Image(new TextureRegionDrawable(lawnColumnRegion));
            colImg.setScaling(Scaling.fit);
            cross.add(colImg);
        }
        return cross;
    }

    private void addResolvedIcon(Table pad, String id) {
        TextureRegion iconRegion = resolveIconRegion(id);
        if (iconRegion != null) {
            Image icon = new Image(new TextureRegionDrawable(iconRegion));
            icon.setScaling(Scaling.fit);
            pad.add(icon).grow();
        } else {
            Label fallback = new Label("?", skin, "medium");
            fallback.setColor(INK);
            fallback.setAlignment(Align.center);
            pad.add(fallback).grow();
        }
    }

    private TextureRegion resolveIconRegion(String id) {
        if (QuestArt.ICON_LAWN_COLUMN.equals(id)) {
            return lawnColumnRegion;
        }
        return textures == null ? null : textures.region(id);
    }

    private RoundedRegionImage roundedFill(Color color, float radius) {
        RoundedRegionImage image = new RoundedRegionImage(new TextureRegion(whitePixel), radius);
        image.setColor(color);
        image.setTouchable(Touchable.disabled);
        return image;
    }
}
