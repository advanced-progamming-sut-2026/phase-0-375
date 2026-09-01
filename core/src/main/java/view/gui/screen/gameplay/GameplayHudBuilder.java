package view.gui.screen.gameplay;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import controller.result.CommandResult;
import model.app.App;
import model.game.core.GameModel;
import model.game.level.special.PlantWhatYouGetLevel;
import view.gui.anim.PamClipCache;
import view.gui.ui.BeghouledMatchHud;
import view.gui.ui.CoinHud;
import view.gui.ui.ConveyorBeltHud;
import view.gui.ui.LootRewardPopup;
import view.gui.ui.LoveYourPlantsHud;
import view.gui.ui.MyopointAwardFeed;
import view.gui.ui.MyopointHud;
import view.gui.ui.PlantFoodBankHud;
import view.gui.ui.ReactionBubbleLayout;
import view.gui.ui.ReactionBubbleWidget;
import view.gui.ui.ReadySetPlantBanner;
import view.gui.ui.SeedPacketActor;
import view.gui.ui.SkinFonts;
import view.gui.ui.SunHud;
import view.gui.ui.TimedProgressHud;
import view.gui.ui.WaveAnnounceBanner;
import view.gui.ui.WaveProgressHud;
import view.gui.ui.ZombossHpHud;

/** Builds in-game HUD tables and wires packet / cheat / pause controls. */
public final class GameplayHudBuilder {
    private final GameplayContext ctx;

    public GameplayHudBuilder(GameplayContext ctx) {
        this.ctx = ctx;
    }

    public void build() {
        GameModel model = GameplayLevelQueries.model();
        addConveyor();
        addLeftColumn(model);
        addTopRight();
        addProgressTopBar(model);
        addMultiplayerBar(model);
        addCouchIzTop(model);
        addLootPopup();
        addPlantFoodBank(model);
        addBottomRight(model);
        addBanners(model);
        ctx.view.toast.toFront();
        ctx.packets.refresh();
    }

    private void addConveyor() {
        if (!ctx.conveyorMode) {
            return;
        }
        ctx.conveyorHud = new ConveyorBeltHud(ctx.view.assets, ctx.view.skin, ctx.packets.conveyorDrag());
        ctx.view.uiStage.addActor(ctx.conveyorHud);
        ctx.hudRoots.add(ctx.conveyorHud);
    }

    private void addLeftColumn(GameModel model) {
        Table left = newFillParent(Touchable.childrenOnly);
        left.top().left().pad(8f);
        ctx.packetColumn = new Table();
        boolean sunBank = SunHud.showFor(model);
        if (ctx.couchPlayMode) {
            addCouchLeft(model, left, sunBank);
        } else if (ctx.useZombiePackets) {
            addIZombieLeft(model, left, sunBank);
        } else if (!ctx.conveyorMode) {
            addStandardLeft(model, left, sunBank);
        } else {
            addConveyorLoveOnly(model, left);
        }
    }

    private void addCouchLeft(GameModel model, Table left, boolean sunBank) {
        if (sunBank) {
            ctx.sunHud = new SunHud(ctx.view.skin);
            ctx.sunHud.setAmount(model == null ? 0 : model.getPlantSun());
            Table sunRow = new Table();
            sunRow.add(ctx.sunHud).left();
            left.add(sunRow).left().padBottom(8f).row();
        }
        left.add(ctx.packetColumn).left().top();
        addHudRoot(left);
    }

    private void addIZombieLeft(GameModel model, Table left, boolean sunBank) {
        Table topRow = new Table();
        if (sunBank) {
            ctx.sunHud = new SunHud(ctx.view.skin);
            ctx.sunHud.setAmount(model == null ? 0 : model.getSunAmount());
            topRow.add(ctx.sunHud).left().padRight(10f);
        }
        topRow.add(ctx.packetColumn).left().top();
        left.add(topRow).left().top();
        addHudRoot(left);
    }

    private void addStandardLeft(GameModel model, Table left, boolean sunBank) {
        if (sunBank) {
            addSunBankRow(model, left);
        } else if (LoveYourPlantsHud.showFor(model)) {
            addLoveRow(model, left);
        }
        left.add(ctx.packetColumn).left().top();
        addHudRoot(left);
    }

    private void addConveyorLoveOnly(GameModel model, Table left) {
        if (!LoveYourPlantsHud.showFor(model)) {
            return;
        }
        addLoveRow(model, left);
        addHudRoot(left);
    }

    private void addSunBankRow(GameModel model, Table left) {
        ctx.sunHud = new SunHud(ctx.view.skin);
        ctx.sunHud.setAmount(model == null ? 0 : model.getSunAmount());
        Table sunRow = new Table();
        sunRow.add(ctx.sunHud).left();
        if (LoveYourPlantsHud.showFor(model)) {
            ctx.loveYourPlantsHud = new LoveYourPlantsHud(ctx.view.skin, ctx.view.assets.textures);
            ctx.loveYourPlantsHud.sync(model);
            sunRow.add(ctx.loveYourPlantsHud).left().padLeft(8f);
        }
        addLetsRockIfNeeded(model, sunRow);
        left.add(sunRow).left().padBottom(8f).row();
    }

    private void addLoveRow(GameModel model, Table left) {
        ctx.loveYourPlantsHud = new LoveYourPlantsHud(ctx.view.skin, ctx.view.assets.textures);
        ctx.loveYourPlantsHud.sync(model);
        Table loveRow = new Table();
        loveRow.add(ctx.loveYourPlantsHud).left();
        left.add(loveRow).left().padBottom(8f).row();
    }

    private void addLetsRockIfNeeded(GameModel model, Table sunRow) {
        if (model == null || !(model.getCurrentLevel() instanceof PlantWhatYouGetLevel lastStand)
                || !lastStand.isSetupPhase()) {
            return;
        }
        TextButton letsRock = new TextButton("LET'S ROCK!", ctx.view.skin, "purple");
        SkinFonts.scaleButton(letsRock, ctx.view.skin, "purple", 0.95f);
        letsRock.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onLetsRock(lastStand, letsRock, model);
            }
        });
        sunRow.add(letsRock).height(46f).padLeft(8f);
    }

    private void onLetsRock(PlantWhatYouGetLevel lastStand, TextButton letsRock, GameModel model) {
        lastStand.startWaves();
        letsRock.remove();
        if (ctx.waveProgress != null) {
            ctx.waveProgress.setVisible(true);
            ctx.waveProgress.sync(model);
        }
        if (ctx.readySetPlant != null) {
            ctx.readySetPlant.play();
        }
    }

    private void addTopRight() {
        Table topRight = newFillParent(Touchable.childrenOnly);
        topRight.top().right().pad(12f);
        ctx.coinHud = new CoinHud(ctx.view.skin, ctx.view.assets.textures);
        ctx.coinHud.setAmount(ctx.currentTotalCoins());
        ctx.pauseButton = new ImageButton(ctx.view.skin, "ingame_pause");
        ctx.pauseButton.setProgrammaticChangeEvents(false);
        ctx.pauseButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ctx.flow.openPauseMenu();
            }
        });
        Table coinRow = new Table();
        coinRow.add(ctx.coinHud).padRight(8f);
        coinRow.add(ctx.pauseButton).size(GameplayContext.PAUSE_BTN_SIZE);
        topRight.add(coinRow).right();
        addHudRoot(topRight);
    }

    private void addProgressTopBar(GameModel model) {
        if (!(TimedProgressHud.showFor(model) || WaveProgressHud.showFor(model)
                || BeghouledMatchHud.showFor(model) || MyopointHud.showFor(model)
                || ZombossHpHud.showFor(model))) {
            return;
        }
        Table topBar = newFillParent(Touchable.childrenOnly);
        float topPad = ZombossHpHud.showFor(model) ? 28f : 8f;
        topBar.top().padTop(topPad);
        topBar.add().width(160f);
        Table midLeft = new Table();
        midLeft.setTouchable(Touchable.childrenOnly);
        if (TimedProgressHud.showFor(model)) {
            ctx.timedProgressHud = new TimedProgressHud(ctx.view.skin, ctx.view.assets.textures);
            ctx.timedProgressHud.sync(model);
            midLeft.add(ctx.timedProgressHud).center();
        }
        topBar.add(midLeft).expandX().center();
        addCenterHud(model, topBar);
        topBar.add().expandX();
        topBar.add().width(160f).row();
        addMyopoints(model, topBar);
        addHudRoot(topBar);
    }

    private void addCenterHud(GameModel model, Table topBar) {
        Table centerHud = new Table();
        centerHud.setTouchable(Touchable.childrenOnly);
        if (ZombossHpHud.showFor(model)) {
            ctx.zombossHpHud = new ZombossHpHud(ctx.view.skin, ctx.view.assets.player);
            centerHud.add(ctx.zombossHpHud).center().padTop(8f);
        } else if (WaveProgressHud.showFor(model)) {
            addWaveProgress(model, centerHud);
        } else if (BeghouledMatchHud.showFor(model)) {
            ctx.beghouledMatchHud = new BeghouledMatchHud(ctx.view.skin);
            ctx.beghouledMatchHud.sync(model);
            centerHud.add(ctx.beghouledMatchHud).center();
        }
        topBar.add(centerHud).center();
    }

    private void addWaveProgress(GameModel model, Table centerHud) {
        ctx.waveProgress = new WaveProgressHud(ctx.view.skin, ctx.view.assets.textures);
        boolean inSetup = model != null && model.getCurrentLevel() instanceof PlantWhatYouGetLevel lastStand
                && lastStand.isSetupPhase();
        ctx.waveProgress.setVisible(!inSetup);
        centerHud.add(ctx.waveProgress).center();
    }

    private void addMyopoints(GameModel model, Table topBar) {
        if (!MyopointHud.showFor(model)) {
            return;
        }
        ctx.myopointHud = new MyopointHud(ctx.view.skin);
        ctx.myopointHud.sync(model);
        topBar.add(ctx.myopointHud).colspan(5).top().padTop(4f).row();
        ctx.myopointAwardFeed = new MyopointAwardFeed(ctx.view.skin);
        topBar.add(ctx.myopointAwardFeed).colspan(5).top().padTop(6f);
    }

    private void addMultiplayerBar(GameModel model) {
        if (!ctx.multiplayerMode) {
            return;
        }
        Table mpTopBar = newFillParent(Touchable.childrenOnly);
        mpTopBar.top().padTop(8f);
        ctx.multiplayerMatchTimer = new TimedProgressHud(ctx.view.skin, ctx.view.assets.textures);
        ctx.multiplayerMatchTimer.setBarWidth(WaveProgressHud.BAR_W);
        if (ctx.multiplayerPlantSide) {
            mpTopBar.add().width(160f);
            mpTopBar.add(ctx.multiplayerMatchTimer).center().expandX();
            mpTopBar.add().width(160f);
        } else {
            float leftPad = 8f + (ctx.sunHud != null ? SunHud.WIDTH + 10f : 0f)
                    + SeedPacketActor.PACKET_WIDTH + 8f;
            mpTopBar.add().width(leftPad);
            mpTopBar.add(ctx.multiplayerMatchTimer).center().expandX();
            mpTopBar.add().width(160f);
        }
        addHudRoot(mpTopBar);
        buildReactionBubbles(model);
    }

    private void addCouchIzTop(GameModel model) {
        if (!ctx.couchPlayMode) {
            return;
        }
        Table izTop = newFillParent(Touchable.childrenOnly);
        izTop.top().padTop(8f);
        ctx.zombiePacketColumn = new Table();
        ctx.zombieSunHud = new SunHud(ctx.view.skin);
        ctx.zombieSunHud.setAmount(model == null ? 0 : model.getSunAmount());
        ctx.multiplayerMatchTimer = new TimedProgressHud(ctx.view.skin, ctx.view.assets.textures);
        ctx.multiplayerMatchTimer.setBarWidth(WaveProgressHud.BAR_W);
        izTop.add().width(160f);
        izTop.add(ctx.zombiePacketColumn).left().padRight(8f);
        izTop.add(ctx.zombieSunHud).left().padRight(10f);
        izTop.add(ctx.multiplayerMatchTimer).center().expandX();
        izTop.add().width(160f);
        addHudRoot(izTop);
    }

    private void addLootPopup() {
        ctx.lootRewardPopup = new LootRewardPopup(ctx.view.skin);
        Table rewardAnchor = new Table();
        rewardAnchor.setFillParent(true);
        rewardAnchor.top().padTop(72f);
        rewardAnchor.add(ctx.lootRewardPopup).top();
        addHudRoot(rewardAnchor);
    }

    private void addPlantFoodBank(GameModel model) {
        if (!GameplayLevelQueries.plantFoodHudEnabled(model)) {
            return;
        }
        ctx.plantFoodBank = new PlantFoodBankHud(ctx.view.skin, ctx.view.assets.textures);
        ctx.plantFoodBank.onPlantFoodButton(() -> ctx.cursors.setPlantfoodMode(!ctx.plantfoodMode));
        Table bottomLeft = newFillParent(Touchable.childrenOnly);
        float leftPad = ctx.conveyorMode ? (ConveyorBeltHud.TOTAL_WIDTH + 8f) : 8f;
        bottomLeft.bottom().left().padLeft(leftPad).padBottom(8f);
        bottomLeft.add(ctx.plantFoodBank).left().bottom();
        addHudRoot(bottomLeft);
    }

    private void addBottomRight(GameModel model) {
        boolean debug = model != null && model.isDebugMode();
        boolean shovel = GameplayLevelQueries.shovelEnabled(model);
        if (!debug && !shovel) {
            return;
        }
        Table bottomRight = newFillParent(Touchable.childrenOnly);
        bottomRight.bottom().right().pad(8f);
        if (debug) {
            addCheatRow(bottomRight);
        }
        if (shovel) {
            addShovelButton(bottomRight);
        }
        addHudRoot(bottomRight);
    }

    private void addCheatRow(Table bottomRight) {
        TextButton addSun = cheatButton("+100 sun", () -> {
            CommandResult<Void> result = ctx.gameplay.cheatAddSuns(100);
            ctx.view.toast(result.getMessage(), !result.isSuccess());
            if (ctx.sunHud != null && result.isSuccess()) {
                ctx.sunHud.setAmount(App.getInstance().getCurrentGameModel().getSunAmount());
            }
            ctx.packets.refreshChrome();
        });
        TextButton addPlantFood = cheatButton("+1 plant food", () -> {
            CommandResult<Void> result = ctx.gameplay.cheatAddPlantFood();
            ctx.view.toast(result.getMessage(), !result.isSuccess());
            if (ctx.plantFoodBank != null && result.isSuccess()) {
                ctx.plantFoodBank.setCount(App.getInstance().getCurrentGameModel().getPlantFoodCount());
            }
            ctx.packets.refreshChrome();
        });
        TextButton nuke = cheatButton("Nuke", () -> {
            CommandResult<Void> result = ctx.gameplay.releaseNuke();
            ctx.view.toast(result.getMessage(), !result.isSuccess());
        });
        Table cheats = new Table();
        cheats.add(addSun).width(160f).height(44f).padRight(8f);
        cheats.add(addPlantFood).width(180f).height(44f).padRight(8f);
        cheats.add(nuke).width(100f).height(44f);
        bottomRight.add(cheats).right().padBottom(8f).row();
    }

    private void addShovelButton(Table bottomRight) {
        ctx.shovelButton = new ImageButton(ctx.view.skin, "ingame_shovel");
        ctx.shovelButton.setProgrammaticChangeEvents(false);
        ctx.shovelButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ctx.cursors.setShovelMode(!ctx.shovelMode);
            }
        });
        bottomRight.add(ctx.shovelButton).size(GameplayContext.SHOVEL_SIZE).right();
    }

    private void addBanners(GameModel model) {
        ctx.readySetPlant = new ReadySetPlantBanner(ctx.view.skin);
        ctx.view.uiStage.addActor(ctx.readySetPlant);
        boolean inSetup = model != null && model.getCurrentLevel() instanceof PlantWhatYouGetLevel lastStand
                && lastStand.isSetupPhase();
        boolean resumedMidLevel = model != null && model.getElapsedSeconds() > 0.5f;
        if (!inSetup && !resumedMidLevel) {
            ctx.readySetPlant.play();
        }
        ctx.waveAnnounce = new WaveAnnounceBanner(ctx.view.skin);
        ctx.view.uiStage.addActor(ctx.waveAnnounce);
    }

    private void buildReactionBubbles(GameModel model) {
        if (!ctx.multiplayerMode || ReactionBubbleLayout.bubbleBackground(ctx.view.skin) == null) {
            return;
        }
        ctx.reactionPamClips = new PamClipCache(ctx.view.assets.player);
        var localCorner = ctx.multiplayerPlantSide
                ? ReactionBubbleLayout.Corner.BOTTOM_LEFT : ReactionBubbleLayout.Corner.BOTTOM_RIGHT;
        var remoteCorner = ctx.multiplayerPlantSide
                ? ReactionBubbleLayout.Corner.BOTTOM_RIGHT : ReactionBubbleLayout.Corner.BOTTOM_LEFT;
        ctx.localReactionBubble = new ReactionBubbleWidget(
            ctx.view.skin, ctx.view.assets.textures, ctx.view.assets.player, ctx.reactionPamClips,
            localCorner, true, ctx.flow::sendReaction);
        float localPadY = ctx.multiplayerPlantSide && GameplayLevelQueries.plantFoodHudEnabled(model)
                ? ReactionBubbleLayout.PLANT_PAD_Y : ReactionBubbleLayout.PAD_Y;
        ctx.localReactionBubble.setPadY(localPadY);
        addRemoteBubble(remoteCorner);
    }

    private void addRemoteBubble(ReactionBubbleLayout.Corner remoteCorner) {
        ctx.remoteReactionBubble = new ReactionBubbleWidget(
            ctx.view.skin, ctx.view.assets.textures, ctx.view.assets.player, ctx.reactionPamClips,
            remoteCorner, false, null);
        ctx.remoteReactionBubble.setPadY(ReactionBubbleLayout.PAD_Y);
        ctx.view.uiStage.addActor(ctx.remoteReactionBubble);
        ctx.view.uiStage.addActor(ctx.localReactionBubble);
        ctx.hudRoots.add(ctx.remoteReactionBubble);
        ctx.hudRoots.add(ctx.localReactionBubble);
        ctx.localReactionBubble.relayout(ctx.view.uiStage.getWidth());
        ctx.remoteReactionBubble.relayout(ctx.view.uiStage.getWidth());
        ctx.localReactionBubble.toFront();
    }

    private TextButton cheatButton(String label, Runnable action) {
        TextButton button = new TextButton(label, ctx.view.skin, "brown");
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
        return button;
    }

    private void addHudRoot(Table table) {
        ctx.view.uiStage.addActor(table);
        ctx.hudRoots.add(table);
    }

    private static Table newFillParent(Touchable touchable) {
        Table table = new Table();
        table.setFillParent(true);
        table.setTouchable(touchable);
        return table;
    }
}
