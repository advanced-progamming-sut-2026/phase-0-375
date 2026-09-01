package view.gui.ui;

import static view.gui.ui.CollectionEntryOverlay.*;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import controller.CollectionMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.collection.Collection;
import model.enums.PlantCategory;
import model.plant.definition.LevelUpgrade;
import model.plant.definition.Plant;
import model.user.User;
import model.zombie.definition.Zombie;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import view.gui.anim.PamClipCache;
import view.gui.anim.PamVisibility;
import view.gui.anim.SpritesheetClipCache;
import view.gui.anim.zombie.SunshineAnim;
import view.gui.anim.zombie.ZombieAnimAdapter;
import view.gui.anim.zombie.ZombotanyAnim;
import view.gui.assets.AlmanacArt;
import view.gui.assets.AlmanacZombieLabels;
import view.gui.assets.PamCatalog;
import view.gui.assets.PvzAssets;
import view.gui.audio.GameAudio;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Scaling;
import pvz.skin.BorderedTable;
import view.gui.assets.PlantSpritesheetCatalog;
import view.gui.assets.SeedPacketIds;
import view.gui.assets.ShopArt;
import view.gui.assets.UiRegions;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

final class CollectionBlueModalPanel extends WidgetGroup {
    private final BorderedTable frame = new BorderedTable();
    private final RoundedRegionImage blueFill;
    private final Table content;
    private final AtlasImageButton closeBtn;
    private final float modalW;
    private final float minInnerH;

    CollectionBlueModalPanel(TextureBank textures, Table content, float modalW, float minInnerH,
                   Runnable onClose) {
        this.content = content;
        this.modalW = modalW;
        this.minInnerH = minInnerH;
        blueFill = new RoundedRegionImage(blueFillRegion(textures), BLUE_CORNER);
        blueFill.setTouchable(Touchable.disabled);
        frame.setTouchable(Touchable.disabled);
        closeBtn = new AtlasImageButton(
            textures.region(CLOSE_UP),
            textures.region(CLOSE_DOWN),
            CLOSE_SIZE, onClose);
        addActor(frame);
        addActor(blueFill);
        addActor(content);
        addActor(closeBtn);
        layoutPanel();
    }

    @Override
    public float getPrefWidth() {
        return getWidth() > 0f ? getWidth() : modalW;
    }

    @Override
    public float getPrefHeight() {
        return getHeight() > 0f ? getHeight() : 400f;
    }

    void layoutPanel() {
        float frameT = BLUE_FRAME;
        float innerW = modalW - frameT * 2f;
        content.setSize(innerW, 0f);
        content.invalidate();
        content.validate();
        float innerH = Math.max(content.getPrefHeight(), minInnerH);
        float panelH = innerH + frameT * 2f;

        setSize(modalW, panelH);
        frame.setBounds(0f, 0f, modalW, panelH);
        blueFill.setBounds(frameT, frameT, innerW, innerH);
        content.setBounds(frameT, frameT, innerW, innerH);
        closeBtn.setSize(CLOSE_SIZE, CLOSE_SIZE);
        closeBtn.setPosition(
            modalW - CLOSE_SIZE - CLOSE_PAD,
            panelH - CLOSE_SIZE - CLOSE_PAD);
        if (hasParent()) {
            invalidateHierarchy();
        }
    }
}


// ── Plant modal card ───────────────────────────────────────────────────

final class CollectionPlantPage {
    private final Skin skin;
    private final TextureBank textures;
    private final CollectionMenuController controller = CollectionMenuController.getInstance();
    private final List<String> names;
    private final Runnable onChanged;
    private final Consumer<CommandResult<?>> onResult;
    private final boolean blueBg = PLANT_BLUE_BG;

    final Actor root;
    private final CollectionBlueModalPanel bluePanel;
    private final CollectionIdlePreview preview;
    private Label levelLabel;
    private ProgressBar seedBar;
    private Label seedCountLabel;
    private Image xpIcon;
    private TextButton upgradeBtn;
    private TextButton buyBtn;
    private Label title;
    private Label sunStat;
    private Label rechargeStat;
    private Label toughStat;
    private Label damageStat;
    private Label rangeStat;
    private Label specialStat;
    private FamilyBadge familyBadge;
    private Label familyLabel;
    private Label plantFoodLabel;
    private Label abilityLabel;
    private Label flavorLabel;
    private int index;

    CollectionPlantPage(Skin skin, PvzAssets assets, PamClipCache clips, TextureBank textures,
                      List<String> names, int start,
                      Runnable onChanged, Consumer<CommandResult<?>> onResult, Runnable onClose) {
        this.skin = skin;
        this.textures = textures;
        this.names = names;
        this.index = start;
        this.onChanged = onChanged;
        this.onResult = onResult;
        Color ink = panelInk(blueBg);
        Color muted = panelMuted(blueBg);
        Color flavor = panelFlavor(blueBg);
        Color pfGreen = panelPlantFoodGreen(blueBg);
        preview = new CollectionIdlePreview(assets, clips, PLANT_PAM_SCALE, PLANT_PAM_ANCHOR_Y);
        createLabels(ink, muted, flavor, pfGreen);
        bindPurchaseButtons();
        Table content = assembleCard(buildLeftColumn(), buildRightColumn());
        if (blueBg) {
            bluePanel = new CollectionBlueModalPanel(
                textures, content, PLANT_MODAL_W,
                PREVIEW_H + PLANT_CARD_PAD + plantPadBottom() + ACTIONS_PAD_TOP + BTN_H * 2f + 80f,
                onClose);
            root = bluePanel;
        } else {
            bluePanel = null;
            root = creamModalRoot(content, textures, onClose);
        }
        refresh();
    }

    private float plantPadBottom() {
        return blueBg ? PLANT_CARD_PAD_BOTTOM : PLANT_CARD_PAD;
    }

    private void createLabels(Color ink, Color muted, Color flavor, Color pfGreen) {
        levelLabel = inkLabel(skin, "medium", "", INK);
        levelLabel.setAlignment(Align.center);
        seedBar = new ProgressBar(0f, 1f, 1f, false,
            skin.get("xp_yellow", ProgressBar.ProgressBarStyle.class));
        seedBar.setAnimateDuration(0f);
        seedBar.setTouchable(Touchable.disabled);
        seedCountLabel = inkLabel(skin, "secondary", "", ink);
        SkinFonts.scaleLabel(seedCountLabel, skin, "secondary", SEED_BAR_FONT_SCALE);
        seedCountLabel.setAlignment(Align.center);
        xpIcon = new Image();
        xpIcon.setScaling(Scaling.fit);
        title = inkLabel(skin, "big", "", ink);
        title.setAlignment(Align.center);
        sunStat = inkLabel(skin, "medium", "", ink);
        rechargeStat = inkLabel(skin, "medium", "", ink);
        toughStat = inkLabel(skin, "medium", "", ink);
        damageStat = inkLabel(skin, "medium", "", ink);
        rangeStat = inkLabel(skin, "medium", "", ink);
        specialStat = inkLabel(skin, "medium", "", ink);
        familyLabel = inkLabel(skin, "medium", "", ink);
        plantFoodLabel = inkLabel(skin, "secondary", "", pfGreen);
        plantFoodLabel.setWrap(true);
        abilityLabel = inkLabel(skin, "secondary", "", muted);
        abilityLabel.setWrap(true);
        flavorLabel = inkLabel(skin, "secondary", "", flavor);
        flavorLabel.setWrap(true);
        familyBadge = new FamilyBadge(textures, STAT_ICON);
    }

    private void bindPurchaseButtons() {
        upgradeBtn = new TextButton("UPGRADE", skin, "purple");
        buyBtn = new TextButton("BUY", skin, "brown");
        SkinFonts.scaleButton(upgradeBtn, skin, "purple", UI_FONT_SCALE);
        SkinFonts.scaleButton(buyBtn, skin, "brown", UI_FONT_SCALE);
        upgradeBtn.addListener(purchaseListener(() -> controller.upgradePlant(currentName())));
        buyBtn.addListener(purchaseListener(() -> controller.purchasePlant(currentName())));
    }

    private ChangeListener purchaseListener(java.util.function.Supplier<CommandResult<Void>> action) {
        return new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = action.get();
                GameAudio.get().feedbackPurchase(r);
                if (onResult != null) {
                    onResult.accept(r);
                }
                if (onChanged != null) {
                    onChanged.run();
                }
                refresh();
            }
        };
    }

    private Table buildLeftColumn() {
        Table left = new Table();
        Stack previewStack = new Stack();
        Image previewBg = regionImage(textures, AlmanacArt.STAT_BG);
        previewBg.setFillParent(true);
        previewStack.add(previewBg);
        previewStack.add(preview);
        Table levelPad = new Table();
        levelPad.bottom();
        levelPad.add(levelLabel).padBottom(LEVEL_LABEL_PAD_BOTTOM);
        previewStack.add(levelPad);
        left.add(previewStack).size(PREVIEW_W, PREVIEW_H).padBottom(PREVIEW_PAD_BOTTOM).row();
        left.add(seedStack())
            .width(SEED_BAR_W)
            .height(SEED_BAR_H * SEED_BAR_SCALE)
            .padBottom(SEED_BAR_PAD_BOTTOM)
            .row();
        Table actions = new Table();
        actions.add(upgradeBtn).width(BTN_W).height(BTN_H).padBottom(BTN_GAP).row();
        actions.add(buyBtn).width(BTN_W).height(BTN_H);
        left.add(actions).padTop(ACTIONS_PAD_TOP);
        left.padTop(LEFT_COL_PAD_TOP);
        return left;
    }

    private Stack seedStack() {
        Stack seedStack = new Stack();
        seedStack.add(scaledSeedBarSlot(seedBar, SEED_BAR_W));
        Table seedOverlay = new Table();
        seedOverlay.add(xpIcon).size(SEED_XP_ICON).padLeft(SEED_XP_ICON_PAD_LEFT).left();
        seedOverlay.add(seedCountLabel).expandX().center();
        seedStack.add(seedOverlay);
        return seedStack;
    }

    private Table buildRightColumn() {
        Table stats = new Table();
        stats.add(statRow(AlmanacArt.ICON_SUN, "SUN COST", sunStat))
            .growX().uniformX().padBottom(STAT_ROW_PAD_BOTTOM);
        stats.add(statRow(AlmanacArt.ICON_RECHARGE, "RECHARGE", rechargeStat))
            .growX().uniformX().padBottom(STAT_ROW_PAD_BOTTOM).row();
        stats.add(statRow(AlmanacArt.ICON_TOUGHNESS, "TOUGHNESS", toughStat))
            .growX().uniformX().padBottom(STAT_ROW_PAD_BOTTOM);
        stats.add(statRow(AlmanacArt.ICON_DAMAGE, "DAMAGE", damageStat))
            .growX().uniformX().padBottom(STAT_ROW_PAD_BOTTOM).row();
        stats.add(statRow(AlmanacArt.ICON_RANGE, "RANGE", rangeStat))
            .growX().uniformX().padBottom(STAT_ROW_PAD_BOTTOM);
        stats.add(statRow(AlmanacArt.ICON_SPECIAL, "SPECIAL", specialStat))
            .growX().uniformX().padBottom(STAT_ROW_PAD_BOTTOM).row();
        Table familyRow = new Table();
        familyRow.add(familyBadge).size(STAT_ICON).padRight(STAT_ICON_PAD_RIGHT);
        familyRow.add(familyLabel).left().growX();
        Table pfRow = new Table();
        pfRow.add(regionImage(textures, AlmanacArt.ICON_PLANT_FOOD))
            .size(STAT_ICON).padRight(STAT_ICON_PAD_RIGHT).top();
        pfRow.add(plantFoodLabel).growX().left();
        Table right = new Table();
        right.top();
        right.add(stats).growX().padBottom(STATS_PAD_BOTTOM).row();
        right.add(familyRow).left().padBottom(FAMILY_PAD_BOTTOM).row();
        right.add(pfRow).growX().padBottom(PLANT_FOOD_PAD_BOTTOM).row();
        right.add(abilityLabel).growX().padBottom(ABILITY_PAD_BOTTOM).row();
        right.add(flavorLabel).growX().row();
        return right;
    }

    private Table assembleCard(Table left, Table right) {
        Table body = new Table();
        body.add(left).width(LEFT_COL_W).top().padRight(BODY_COL_GAP);
        body.add(right).grow().top();
        AtlasImageButton prev = new AtlasImageButton(
            textures.region(AlmanacArt.NAV_PREV),
            textures.region(AlmanacArt.NAV_PREV_DOWN),
            NAV_W, NAV_H, this::prev);
        AtlasImageButton next = new AtlasImageButton(
            textures.region(AlmanacArt.NAV_NEXT),
            textures.region(AlmanacArt.NAV_NEXT_DOWN),
            NAV_W, NAV_H, this::next);
        Table middle = new Table();
        middle.add(prev).size(NAV_W, NAV_H).padRight(PLANT_NAV_GAP);
        middle.add(body).grow();
        middle.add(next).size(NAV_W, NAV_H).padLeft(PLANT_NAV_GAP);
        Table content = new Table();
        content.padLeft(PLANT_NAV_EDGE_PAD).padRight(PLANT_NAV_EDGE_PAD)
            .padBottom(plantPadBottom()).padTop(PLANT_TITLE_PAD_TOP);
        content.add(title).growX().center().padBottom(TITLE_PAD_BOTTOM).row();
        content.add(middle).growX();
        return content;
    }

    boolean handleNavKey(int keycode) {
        if (names.size() <= 1) {
            return false;
        }
        if (keycode == Input.Keys.LEFT || keycode == Input.Keys.A) {
            prev();
            return true;
        }
        if (keycode == Input.Keys.RIGHT || keycode == Input.Keys.D) {
            next();
            return true;
        }
        return false;
    }

    private Table statRow(String iconId, String caption, Label value) {
        Table row = new Table();
        row.add(regionImage(textures, iconId))
            .size(STAT_ICON)
            .padRight(STAT_ICON_PAD_RIGHT)
            .top();
        Table text = new Table();
        Label cap = inkLabel(skin, "secondary", caption, panelMuted(blueBg));
        text.add(cap).left().row();
        value.setWrap(true);
        value.setAlignment(Align.left);
        text.add(value).left().growX().padTop(STAT_VALUE_PAD_TOP);
        row.add(text).left().growX().top();
        return row;
    }

    private void setFamilyIcon(Plant plant) {
        familyBadge.setPlant(textures, plant);
    }

    private String currentName() {
        return names.get(index);
    }

    private void prev() {
        if (names.size() <= 1) {
            return;
        }
        GameAudio.get().playNavClick();
        index = (index - 1 + names.size()) % names.size();
        refresh();
    }

    private void next() {
        if (names.size() <= 1) {
            return;
        }
        GameAudio.get().playNavClick();
        index = (index + 1) % names.size();
        refresh();
    }

    private void refresh() {
        String name = currentName();
        Collection col = controller.currentCollection();
        Plant plant = col.getPlant(name);
        title.setText(name);
        preview.setPlant(name);
        boolean owned = col.ownsPlant(name);
        int level = owned ? col.getPlantLevel(name) : 1;
        levelLabel.setText(owned ? ("Level " + level) : "Locked");
        refreshSeedProgress(col, name, owned, level);
        refreshPurchaseButtons(col, name, owned, level);
        if (plant == null) {
            if (bluePanel != null) {
                bluePanel.layoutPanel();
            }
            return;
        }
        refreshPlantCopy(plant, col, owned, level);
        if (bluePanel != null) {
            bluePanel.layoutPanel();
        }
    }

    private void refreshSeedProgress(Collection col, String name, boolean owned, int level) {
        User user = App.getInstance().getCurrentUser();
        int have = user != null && user.getSeedPackets() != null
            ? user.getSeedPackets().getOrDefault(name, 0) : 0;
        boolean maxed = owned && level >= Collection.MAX_PLANT_LEVEL;
        int need = owned && !maxed ? Math.max(1, col.getUpgradeSeedCost(name)) : 1;
        boolean ready = owned && !maxed && have >= need;
        ProgressBar.ProgressBarStyle style = skin.get(
            ready ? "xp_green" : "xp_yellow", ProgressBar.ProgressBarStyle.class);
        seedBar.setStyle(style);
        seedBar.setRange(0f, need);
        seedBar.setValue(owned && !maxed ? Math.min(have, need) : 0f);
        if (!owned) {
            seedCountLabel.setText("");
        } else if (maxed) {
            seedCountLabel.setText("MAX");
        } else {
            seedCountLabel.setText(have + "/" + need);
        }
        xpIcon.setDrawable(xpDrawable(ready));
        xpIcon.setVisible(owned && !maxed);
    }

    private Drawable xpDrawable(boolean ready) {
        Drawable icon = skin.optional(
            ready ? "image_ui_generic_xp_progress_icon_green"
                : "image_ui_generic_xp_progress_icon_yellow",
            Drawable.class);
        if (icon == null) {
            icon = skin.optional(
                ready ? "image_ui_generic_xp_progress_icon_green_large"
                    : "image_ui_generic_xp_progress_icon_yellow_large",
                Drawable.class);
        }
        return icon;
    }

    private void refreshPurchaseButtons(Collection col, String name, boolean owned, int level) {
        User user = App.getInstance().getCurrentUser();
        int have = user != null && user.getSeedPackets() != null
            ? user.getSeedPackets().getOrDefault(name, 0) : 0;
        boolean maxed = owned && level >= Collection.MAX_PLANT_LEVEL;
        int need = owned && !maxed ? Math.max(1, col.getUpgradeSeedCost(name)) : 1;
        int purchaseCost = controller.purchaseCostCoins();
        int upgradeCoins = owned && !maxed ? col.getUpgradeCoinCost(name) : 0;
        int coins = user != null ? user.getCoins() : 0;
        boolean canBuy = !owned && coins >= purchaseCost;
        boolean canUpgrade = owned && !maxed && have >= need && coins >= upgradeCoins;
        upgradeBtn.setVisible(true);
        buyBtn.setVisible(true);
        upgradeBtn.setDisabled(!canUpgrade);
        buyBtn.setDisabled(!canBuy);
        buyBtn.setText("BUY (" + purchaseCost + ")");
        upgradeBtn.setColor(canUpgrade ? Color.WHITE : Color.GRAY);
        buyBtn.setColor(canBuy ? Color.WHITE : Color.GRAY);
    }

    private void refreshPlantCopy(Plant plant, Collection col, boolean owned, int level) {
        boolean maxed = owned && level >= Collection.MAX_PLANT_LEVEL;
        CollectionPlantStats now = statsAt(plant, owned ? level : 1);
        CollectionPlantStats next = owned && !maxed ? statsAt(plant, level + 1) : now;
        sunStat.setText(diff(now.cost(), next.cost(), owned && !maxed));
        rechargeStat.setText(formatFloat(now.recharge()));
        toughStat.setText(diff(now.hp(), next.hp(), owned && !maxed));
        damageStat.setText(diff(now.damage(), next.damage(), owned && !maxed));
        rangeStat.setText(rangeLabel(plant.getCategory()));
        specialStat.setText(specialLabel(plant));
        setFamilyIcon(plant);
        familyLabel.setText(prettyEnum(plant.getCategory().name()));
        plantFoodLabel.setText("Plant Food: " + plantFoodBlurb(plant));
        abilityLabel.setText(abilityBlurb(plant));
        flavorLabel.setText(flavorBlurb(plant, owned, level, maxed, col));
    }

    private static String diff(int now, int next, boolean showNext) {
        if (!showNext || now == next) {
            return String.valueOf(now);
        }
        return now + " > " + next;
    }

    private static String formatFloat(float v) {
        if (Math.abs(v - Math.rint(v)) < 0.05f) {
            return String.valueOf((int) Math.rint(v));
        }
        return String.format(Locale.US, "%.1f", v);
    }

    private static String rangeLabel(PlantCategory category) {
        if (category == null) {
            return "—";
        }
        return switch (category) {
            case LOBBER -> "Lobbed";
            case SHOOTER, STRIKE_THROUGH -> "Straight";
            case HOMING -> "Homing";
            case MELEE -> "Touch";
            case EXPLOSIVE -> "Tile / Area";
            case SUN_PRODUCER -> "—";
            case WALL_NUT -> "—";
            case MODIFIER -> "Aura";
            case MINT -> "Family";
        };
    }

    private static String specialLabel(Plant plant) {
        if (plant.getTags() != null && !plant.getTags().isEmpty()) {
            return prettyEnum(plant.getTags().get(0).name());
        }
        return prettyEnum(plant.getAbilityType().name());
    }

    private static String plantFoodBlurb(Plant plant) {
        return switch (plant.getPlantFoodType()) {
            case NONE -> "None.";
            case SPAWN_SUN_ITEMS -> "Drops a burst of sun.";
            case PROJECTILE_BURST -> "Fires a rapid projectile volley.";
            case RANDOM_HYPNOTIZE -> "Hypnotizes random zombies.";
            case KNOCKBACK_BLAST -> "Knocks back and damages nearby zombies.";
            case MAP_WIDE_FREEZE -> "Stuns / freezes zombies across the lawn.";
            case SPAWN_CLONES -> "Spawns temporary nearby clones.";
            case LOCAL_AOE_ATTACK -> "Hits all zombies around the plant.";
            case PULL_UNDERWATER -> "Pulls zombies in and finishes them.";
            case GRANT_PERMANENT_ARMOR -> "Grants lasting armor.";
            case ATTRACT_AND_HEAL -> "Pulls zombies in and fully heals.";
        };
    }

    private static String abilityBlurb(Plant plant) {
        String ability = prettyEnum(plant.getAbilityType().name());
        return plant.getName() + " — " + ability
            + " (value " + formatFloat(plant.getAbilityValue()) + ").";
    }

    private static String flavorBlurb(Plant plant, boolean owned, int level,
                                      boolean maxed, Collection col) {
        if (!owned) {
            return "Locked. Purchase to add this plant to your collection.";
        }
        if (maxed) {
            return "Already at max level.";
        }
        return "Next upgrade needs " + col.getUpgradeSeedCost(plant.getName())
            + " seed packets and " + col.getUpgradeCoinCost(plant.getName()) + " coins.";
    }

    private static String prettyEnum(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "—";
        }
        String[] parts = raw.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private static CollectionPlantStats statsAt(Plant plant, int level) {
        int hp = plant.getBaseHP();
        int damage = plant.getDamage();
        int cost = plant.getCost();
        float recharge = plant.getRechargeTime();
        if (level > 1 && plant.getLevels() != null) {
            for (LevelUpgrade up : plant.getLevels().cumulativeUpgrades(level).values()) {
                if (up == null) {
                    continue;
                }
                switch (up.getType()) {
                    case BUFF_HP -> hp += (int) up.getValue();
                    case BUFF_DAMAGE -> damage += (int) up.getValue();
                    case BUFF_COST -> cost = Math.max(0, cost + (int) up.getValue());
                    case BUFF_RECHARGE -> recharge = Math.max(0f, recharge + up.getValue());
                    default -> {
                    }
                }
            }
        }
        return new CollectionPlantStats(cost, hp, damage, recharge);
    }
}

// ── Zombie modal card ──────────────────────────────────────────────────

final class CollectionZombiePage {
    private final CollectionMenuController controller = CollectionMenuController.getInstance();
    private final TextureBank textures;
    private final List<String> names;
    private final boolean blueBg = ZOMBIE_BLUE_BG;
    private int index;

    final Actor root;
    private final CollectionBlueModalPanel bluePanel;
    private final CollectionIdlePreview preview;
    private Label title;
    private Label toughStat;
    private Label speedStat;
    private Label descLabel;
    private Label flavorLabel;

    CollectionZombiePage(Skin skin, PvzAssets assets, PamClipCache clips, TextureBank textures,
               List<String> names, int start, Runnable onClose) {
        this.textures = textures;
        this.names = names;
        this.index = start;
        Color ink = panelInk(blueBg);
        Color flavor = panelFlavor(blueBg);
        preview = new CollectionIdlePreview(assets, clips, ZOMBIE_PAM_SCALE, ZOMBIE_PAM_ANCHOR_Y);
        createZombieLabels(skin, ink, flavor);
        Table content = assembleZombieCard(textures, buildZombieLeft(), buildZombieRight(skin, textures));
        if (blueBg) {
            float padBottom = ZOMBIE_CARD_PAD_BOTTOM;
            bluePanel = new CollectionBlueModalPanel(
                textures, content, ZOMBIE_MODAL_W,
                ZOMBIE_PREVIEW_H + ZOMBIE_CARD_PAD + padBottom + ZOMBIE_RIGHT_COL_PAD_TOP + 40f,
                onClose);
            root = bluePanel;
        } else {
            bluePanel = null;
            root = creamModalRoot(content, textures, onClose);
        }
        refresh();
    }

    private void createZombieLabels(Skin skin, Color ink, Color flavor) {
        title = inkLabel(skin, "big", "", ink);
        title.setAlignment(Align.center);
        toughStat = inkLabel(skin, "medium", "", ink);
        speedStat = inkLabel(skin, "medium", "", ink);
        descLabel = inkLabel(skin, "secondary", "", ink);
        descLabel.setWrap(true);
        descLabel.setAlignment(Align.topLeft);
        flavorLabel = inkLabel(skin, "secondary", "", flavor);
        flavorLabel.setWrap(true);
        flavorLabel.setAlignment(Align.topLeft);
    }

    private Table buildZombieLeft() {
        Stack previewStack = new Stack();
        Image previewBg = regionImage(textures, AlmanacArt.STAT_BG);
        previewBg.setFillParent(true);
        previewStack.add(previewBg);
        previewStack.add(preview);
        Table left = new Table();
        left.top();
        left.padTop(ZOMBIE_LEFT_COL_PAD_TOP);
        left.add(previewStack)
            .size(ZOMBIE_PREVIEW_W, ZOMBIE_PREVIEW_H)
            .padTop(ZOMBIE_PREVIEW_PAD_TOP);
        return left;
    }

    private Table buildZombieRight(Skin skin, TextureBank textures) {
        Table stats = new Table();
        stats.add(zombieStatRow(skin, textures, AlmanacArt.ICON_ZOMBIE_TOUGHNESS,
            "TOUGHNESS", toughStat))
            .growX().uniformX().padBottom(ZOMBIE_STAT_ROW_PAD_BOTTOM);
        stats.add(zombieStatRow(skin, textures, AlmanacArt.ICON_ZOMBIE_SPEED,
            "SPEED", speedStat))
            .growX().uniformX().padBottom(ZOMBIE_STAT_ROW_PAD_BOTTOM).row();
        Table right = new Table();
        right.top().left();
        right.padTop(ZOMBIE_RIGHT_COL_PAD_TOP);
        right.add(stats).growX().padBottom(ZOMBIE_STATS_PAD_BOTTOM).row();
        right.add(descLabel).growX().padBottom(ZOMBIE_DESC_PAD_BOTTOM).row();
        right.add(flavorLabel).growX().row();
        return right;
    }

    private Table assembleZombieCard(TextureBank textures, Table left, Table right) {
        Table body = new Table();
        body.add(left).width(ZOMBIE_LEFT_COL_W).top().padRight(BODY_COL_GAP);
        body.add(right).grow().top().left();
        AtlasImageButton prev = new AtlasImageButton(
            textures.region(AlmanacArt.NAV_PREV),
            textures.region(AlmanacArt.NAV_PREV_DOWN),
            NAV_W, NAV_H, this::prev);
        AtlasImageButton next = new AtlasImageButton(
            textures.region(AlmanacArt.NAV_NEXT),
            textures.region(AlmanacArt.NAV_NEXT_DOWN),
            NAV_W, NAV_H, this::next);
        Table middle = new Table();
        middle.add(prev).size(NAV_W, NAV_H).padRight(NAV_PAD);
        middle.add(body).grow().padLeft(BODY_SIDE_PAD).padRight(BODY_SIDE_PAD);
        middle.add(next).size(NAV_W, NAV_H).padLeft(NAV_PAD);
        Table content = new Table();
        float padBottom = blueBg ? ZOMBIE_CARD_PAD_BOTTOM : ZOMBIE_CARD_PAD;
        content.pad(ZOMBIE_CARD_PAD).padBottom(padBottom);
        content.add(title).growX().center().padBottom(ZOMBIE_TITLE_PAD_BOTTOM).row();
        content.add(middle).growX();
        return content;
    }

    boolean handleNavKey(int keycode) {
        if (names.size() <= 1) {
            return false;
        }
        if (keycode == Input.Keys.LEFT || keycode == Input.Keys.A) {
            prev();
            return true;
        }
        if (keycode == Input.Keys.RIGHT || keycode == Input.Keys.D) {
            next();
            return true;
        }
        return false;
    }

    private void prev() {
        if (names.size() <= 1) {
            return;
        }
        GameAudio.get().playNavClick();
        index = (index - 1 + names.size()) % names.size();
        refresh();
    }

    private void next() {
        if (names.size() <= 1) {
            return;
        }
        GameAudio.get().playNavClick();
        index = (index + 1) % names.size();
        refresh();
    }

    private void refresh() {
        String name = names.get(index);
        Collection col = controller.currentCollection();
        boolean discovered = col.ownsZombie(name);
        Zombie zombie = col.getZombie(name);

        title.setText(discovered ? prettyZombieTitle(name) : "???");
        if (!discovered || zombie == null) {
            preview.clearEntity();
            toughStat.setText("—");
            speedStat.setText("—");
            descLabel.setText("You have not seen this zombie in battle yet.");
            flavorLabel.setText("");
            flavorLabel.setVisible(false);
            if (bluePanel != null) {
                bluePanel.layoutPanel();
            }
            return;
        }

        preview.setZombie(name);
        toughStat.setText(AlmanacZombieLabels.toughnessLabel(zombie));
        speedStat.setText(AlmanacZombieLabels.speedLabel(zombie));
        descLabel.setText(AlmanacZombieLabels.description(zombie));
        String flavor = AlmanacZombieLabels.flavor(zombie);
        flavorLabel.setText(flavor);
        flavorLabel.setVisible(flavor != null && !flavor.isBlank());
        if (bluePanel != null) {
            bluePanel.layoutPanel();
        }
    }

    private Table zombieStatRow(Skin skin, TextureBank textures, String iconId,
                                String caption, Label value) {
        Table row = new Table();
        row.add(regionImage(textures, iconId))
            .size(STAT_ICON)
            .padRight(STAT_ICON_PAD_RIGHT)
            .top();
        Table text = new Table();
        Label cap = inkLabel(skin, "secondary", caption, panelMuted(blueBg));
        text.add(cap).left().row();
        value.setWrap(true);
        value.setAlignment(Align.left);
        text.add(value).left().growX().padTop(STAT_VALUE_PAD_TOP);
        row.add(text).left().growX().top();
        return row;
    }

    private static String prettyZombieTitle(String id) {
        if (id == null) {
            return "";
        }
        return switch (id) {
            case "ZombieDefault" -> "Basic Zombie";
            case "ZombieArmor1" -> "Conehead Zombie";
            case "ZombieArmor2" -> "Buckethead Zombie";
            case "ZombieArmor4" -> "Brickhead Zombie";
            default -> id.startsWith("Zombie") ? id.substring("Zombie".length()) + " Zombie" : id;
        };
    }
}


record CollectionPlantStats(int cost, int hp, int damage, float recharge) {}

final class CollectionIdlePreview extends Actor {
    private final PamPlayer player;
    private final PamCatalog catalog;
    private final PlantSpritesheetCatalog sheets;
    private final PamClipCache clips;
    private final SpritesheetClipCache sheetClips;
    private final float drawScale;
    private final float anchorY;
    private String pamPath;
    private String clipName;
    private PlantSpritesheetCatalog.ClipSpec sheetSpec;
    private Map<String, Boolean> visibility;
    private String plantPamPath;
    private String plantClipName;
    private float sheetOffsetY;
    private float sheetScaleMul = 1f;
    private float time;

    CollectionIdlePreview(PvzAssets assets, PamClipCache clips, float drawScale, float anchorY) {
        this.player = assets.player;
        this.catalog = assets.pamCatalog;
        this.sheets = assets.plantSheets;
        this.clips = clips;
        this.sheetClips = new SpritesheetClipCache(assets.root);
        this.drawScale = drawScale;
        this.anchorY = anchorY;
    }

    void setPlant(String name) {
        pamPath = null;
        clipName = null;
        sheetSpec = null;
        visibility = null;
        plantPamPath = null;
        plantClipName = null;
        sheetOffsetY = 0f;
        sheetScaleMul = 1f;
        time = 0f;
        if (sheets != null && sheets.hasSheets(name)) {
            // Full sheet loop for almanac preview (e.g. Cat-tail attack = all frames).
            sheetSpec = sheets.resolveClip(name, "attack");
            if (sheetSpec == null) {
                sheetSpec = sheets.anyClip(name);
            }
            if (sheetSpec != null) {
                if ("Cat-tail".equalsIgnoreCase(name)) {
                    sheetOffsetY = CATTAIL_PREVIEW_OFFSET_Y;
                    sheetScaleMul = CATTAIL_PREVIEW_SCALE;
                }
                return;
            }
        }
        PamCatalog.PamEntry entry = catalog.forPlant(name);
        if (entry == null) {
            return;
        }
        pamPath = entry.path();
        clipName = catalog.resolveClip(entry, "idle", "idle2", "idle1", "loop");
        if ("Magnet-shroom".equalsIgnoreCase(name)) {
            visibility = PamVisibility.hide(MAGNET_ITEM_PART);
        }
    }

    void setZombie(String name) {
        pamPath = null;
        clipName = null;
        sheetSpec = null;
        visibility = null;
        plantPamPath = null;
        plantClipName = null;
        sheetOffsetY = 0f;
        sheetScaleMul = 1f;
        time = 0f;
        if (sheets != null && sheets.hasSheets(name)) {
            sheetSpec = sheets.resolveClip(name, "idle", "walk");
            if (sheetSpec == null) {
                sheetSpec = sheets.anyClip(name);
            }
            if (sheetSpec != null) {
                if (SunshineAnim.isSunshineName(name)) {
                    sheetOffsetY = SUNSHINE_PREVIEW_OFFSET_Y;
                    sheetScaleMul = SUNSHINE_PREVIEW_SCALE;
                }
                return;
            }
        }
        PamCatalog.PamEntry entry = catalog.forZombie(name);
        if (entry == null) {
            return;
        }
        pamPath = entry.path();
        clipName = catalog.resolveClip(entry, "idle", "walk", "idle2", "loop");
        visibility = ZombieAnimAdapter.almanacArmorVisibility(name, entry);
        if (ZombotanyAnim.isPlantHeadName(name)) {
            visibility = ZombotanyAnim.headHiddenVisibility(visibility);
            String plantName = ZombotanyAnim.plantDefinitionName(name);
            PamCatalog.PamEntry plant = plantName == null ? null : catalog.forPlant(plantName);
            if (plant != null) {
                plantPamPath = plant.path();
                plantClipName = catalog.resolveClip(plant, "idle", "idle2", "idle1", "loop");
            }
        }
    }

    void clearEntity() {
        pamPath = null;
        clipName = null;
        sheetSpec = null;
        visibility = null;
        plantPamPath = null;
        plantClipName = null;
        sheetOffsetY = 0f;
        sheetScaleMul = 1f;
    }

    @Override public void act(float delta) {
        super.act(delta);
        time += Math.max(0f, delta);
    }

    @Override public void draw(Batch batch, float parentAlpha) {
        float a = batch.getColor().a * parentAlpha * getColor().a;
        batch.setColor(batch.getColor().r, batch.getColor().g, batch.getColor().b, a);
        float cx = getX() + getWidth() * 0.5f;
        float cy = getY() + getHeight() * anchorY + sheetOffsetY;
        if (sheetSpec != null) {
            SpritesheetClipCache.SheetAnim sheet = sheetClips.getOrLoad(sheetSpec);
            if (sheet != null && sheet.animation() != null) {
                TextureRegion frame = sheet.animation().getKeyFrame(time, true);
                if (frame != null) {
                    float scale = drawScale * 0.85f * sheetScaleMul;
                    float w = frame.getRegionWidth() * scale;
                    float h = frame.getRegionHeight() * scale;
                    batch.draw(frame, cx - w * 0.5f, cy, w, h);
                }
                return;
            }
        }
        if (pamPath == null || clipName == null) {
            return;
        }
        ClipRef ref = clips.getOrLoad(pamPath, clipName);
        if (ref == null) {
            return;
        }
        if (visibility != null) {
            player.draw(batch, ref, time, cx, cy, drawScale, drawScale, true, visibility);
        } else {
            player.draw(batch, ref, time, cx, cy, drawScale, drawScale, true);
        }
        drawZombotanyPlantHead(batch, ref, cx, cy, drawScale);
    }

    private void drawZombotanyPlantHead(Batch batch, ClipRef bodyRef, float bodyX, float bodyY,
                                        float bodyScale) {
        if (plantPamPath == null || plantClipName == null) {
            return;
        }
        ClipRef plantRef = clips.getOrLoad(plantPamPath, plantClipName);
        if (plantRef == null) {
            return;
        }
        com.badlogic.gdx.math.Rectangle skull = null;
        for (String part : ZombotanyAnim.SKULL_PARTS) {
            skull = player.partBounds(bodyRef, time, part);
            if (skull != null) {
                break;
            }
        }
        float[] xy = ZombotanyAnim.headWorldCenter(
                skull, false, bodyX, bodyY, bodyScale, getHeight() * 0.2f);
        float headScale = bodyScale * ZombotanyAnim.HEAD_SCALE;
        player.draw(batch, plantRef, time, xy[0], xy[1], -headScale, headScale, true);
    }
}
