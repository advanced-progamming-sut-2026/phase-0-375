package view.gui.screen.gameplay;

import model.app.App;
import model.enums.GameState;
import model.game.core.GameModel;
import model.game.core.PvZGameLoop;
import model.game.level.minigame.izombie.IZombieLevel;
import model.game.level.special.PlantWhatYouGetLevel;
import model.game.level.special.ScoreLevel;
import model.game.level.special.ZombossLevel;
import model.game.score.MyopointTracker;
import model.item.LootPickup;
import model.network.packet.game.GameStateSnapshotPacket;
import view.gui.audio.GameAudio;
import view.gui.audio.GameSfx;

import java.util.ArrayList;
import java.util.List;

/** Per-frame sim, snapshot apply, and HUD sync. */
public final class GameplayLogic {
    private final GameplayContext ctx;

    public GameplayLogic(GameplayContext ctx) {
        this.ctx = ctx;
    }

    public void update(float delta) {
        if (ctx.pauseMenuOpen || ctx.invitePauseActive) {
            return;
        }
        if (ctx.endSequenceActive) {
            tickEndSequence(delta);
            return;
        }
        GameModel model = GameplayLevelQueries.model();
        tickPregameAndSim(delta, model);
        ctx.flow.maybeStartEndSequence(model);
        if (ctx.endSequenceActive) {
            tickEndJustStarted(delta);
            return;
        }
        syncHud(delta, model);
    }

    public void syncSunHuds(GameModel model) {
        if (model == null) {
            return;
        }
        if (ctx.sunHud != null) {
            ctx.sunHud.setAmount(ctx.couchPlayMode ? model.getPlantSun() : model.getSunAmount());
        }
        if (ctx.zombieSunHud != null) {
            ctx.zombieSunHud.setAmount(model.getSunAmount());
        }
    }

    private void tickEndSequence(float delta) {
        ctx.entityRenderer.tickEndLevel(delta);
        if (ctx.loseOverlay != null && ctx.entityRenderer.isLoseFadeDone()) {
            ctx.loseOverlay.play();
        }
        playWinOverlaysIfReady();
    }

    private void tickEndJustStarted(float delta) {
        ctx.entityRenderer.tickEndLevel(delta);
        playWinOverlaysIfReady();
    }

    private void playWinOverlaysIfReady() {
        if (ctx.winOverlay != null && ctx.entityRenderer.isWinFadeDone()) {
            ctx.winOverlay.play();
            ctx.gameplayMusic.playChapterRewardOnce(GameplayLevelQueries.currentChapter());
        }
        if (ctx.myopointResultsOverlay != null
            && (ctx.entityRenderer.isLoseFadeDone() || ctx.entityRenderer.isWinFadeDone())) {
            ctx.myopointResultsOverlay.play();
            if (ctx.entityRenderer.isWinFadeDone()) {
                ctx.gameplayMusic.playChapterRewardOnce(GameplayLevelQueries.currentChapter());
            }
        }
    }

    private void tickPregameAndSim(float delta, GameModel model) {
        boolean pregame = ctx.isPregame();
        if (ctx.wasPregame && !pregame && model != null
                && model.getCurrentLevel() instanceof ZombossLevel zombossLevel) {
            if (zombossLevel.ensureBossSpawned()) {
                GameAudio.get().playSfx(GameSfx.ZOMBOSS_SPAWN);
            }
        }
        ctx.wasPregame = pregame;
        if (ctx.multiplayerMode) {
            applyMultiplayerSnapshot(model);
            tickPresentation(delta, model);
        } else if (pregame) {
            ctx.entityRenderer.tickMowerIntro(delta);
        } else {
            tickLocalSim(delta, model);
        }
    }

    private void tickPresentation(float delta, GameModel model) {
        PvZGameLoop loop = App.getInstance().getCurrentGameLoop();
        if (loop != null && model != null && model.getState() == GameState.RUNNING) {
            loop.updatePresentation(delta);
        }
    }

    private void tickLocalSim(float delta, GameModel model) {
        PvZGameLoop loop = App.getInstance().getCurrentGameLoop();
        if (loop != null && loop.getGameState() == GameState.RUNNING) {
            loop.update(delta);
        }
        if (model != null) {
            ctx.entityRenderer.tickMowers(model, delta);
        }
    }

    private void applyMultiplayerSnapshot(GameModel model) {
        if (ctx.snapshotApplier == null || ctx.multiplayerClient == null || model == null) {
            return;
        }
        GameStateSnapshotPacket snap = ctx.multiplayerClient.getLatestSnapshot();
        if (snap == null) {
            return;
        }
        ctx.snapshotApplier.apply(model, snap, ctx.multiplayerRole);
        ctx.snapshotApplier.drainPresentationAttacks(App.getInstance().getCurrentGameLoop());
        syncMatchTimer(snap);
        if (snap.isGameOver() && !ctx.endSequenceActive) {
            boolean won = ctx.multiplayerRole != null
                    && ctx.multiplayerRole.name().equalsIgnoreCase(snap.getWinnerRole());
            model.setGameState(won ? GameState.WON : GameState.LOST);
        }
    }

    private void syncMatchTimer(GameStateSnapshotPacket snap) {
        if (ctx.multiplayerMatchTimer != null) {
            float duration = snap.getMatchDuration() > 0f ? snap.getMatchDuration() : 180f;
            ctx.multiplayerMatchTimer.syncMatchTimer(snap.getTimeRemaining(), duration);
        }
    }

    private void syncHud(float delta, GameModel model) {
        announceWave(model);
        boolean inLastStandSetup = model != null
            && model.getCurrentLevel() instanceof PlantWhatYouGetLevel lastStand
            && lastStand.isSetupPhase();
        ctx.gameplayMusic.sync(model, GameplayLevelQueries.currentChapter(), inLastStandSetup);
        syncMeters(model, inLastStandSetup);
        syncMyopointAwards(model);
        syncSunHuds(model);
        syncCouchTimer(model);
        if (ctx.plantFoodBank != null && model != null) {
            ctx.plantFoodBank.setCount(model.getPlantFoodCount());
        }
        autoCollectLoot(model);
        if (ctx.previewPlant != null || ctx.dropZombieName != null) {
            ctx.previewTime += delta;
        }
        refreshPacketsIfNeeded();
    }

    private void announceWave(GameModel model) {
        if (model != null && ctx.waveAnnounce != null && !ctx.waveAnnounce.isPlaying()) {
            String waveText = model.consumeWaveAnnouncement();
            if (waveText != null) {
                ctx.waveAnnounce.show(waveText);
            }
        }
    }

    private void syncMeters(GameModel model, boolean inLastStandSetup) {
        if (ctx.waveProgress != null) {
            ctx.waveProgress.setVisible(!inLastStandSetup);
            if (!inLastStandSetup) {
                ctx.waveProgress.sync(model);
            }
        }
        if (ctx.timedProgressHud != null) {
            ctx.timedProgressHud.sync(model);
        }
        if (ctx.zombossHpHud != null) {
            ctx.zombossHpHud.sync(model);
        }
        if (ctx.beghouledMatchHud != null) {
            ctx.beghouledMatchHud.sync(model);
        }
        if (ctx.loveYourPlantsHud != null) {
            ctx.loveYourPlantsHud.sync(model);
        }
        if (ctx.myopointHud != null) {
            ctx.myopointHud.sync(model);
        }
    }

    private void syncCouchTimer(GameModel model) {
        if (ctx.couchPlayMode && ctx.multiplayerMatchTimer != null && model != null) {
            float duration = IZombieLevel.VERSUS_MATCH_SECONDS;
            ctx.multiplayerMatchTimer.syncMatchTimer(duration - model.getElapsedSeconds(), duration);
        }
    }

    private void refreshPacketsIfNeeded() {
        if (!ctx.packets.hudPlantNames().equals(ctx.shownPackets)) {
            ctx.packets.refresh();
        } else {
            ctx.packets.refreshChrome();
        }
    }

    private void syncMyopointAwards(GameModel model) {
        if (ctx.myopointAwardFeed == null || model == null) {
            return;
        }
        MyopointTracker tracker = null;
        if (model.getCurrentLevel() instanceof ScoreLevel scoreLevel) {
            tracker = scoreLevel.getTracker();
        } else if (model.getMyopointTracker() != null) {
            tracker = model.getMyopointTracker();
        }
        if (tracker != null) {
            ctx.myopointAwardFeed.push(tracker.drainAwardEvents());
        }
    }

    private void autoCollectLoot(GameModel model) {
        if (model == null || ctx.coinHud == null) {
            return;
        }
        List<LootPickup> pending = model.getActiveLootPickups();
        if (pending == null || pending.isEmpty()) {
            return;
        }
        for (LootPickup loot : new ArrayList<>(pending)) {
            flyLoot(model, loot);
        }
    }

    private void flyLoot(GameModel model, LootPickup loot) {
        model.removeLootPickup(loot);
        ctx.entityRenderer.writeLootDrawPos(loot, ctx.sunPosTmp);
        float x0 = ctx.sunPosTmp[0];
        float y0 = ctx.sunPosTmp[1];
        ctx.coinHud.logoCenter(ctx.logoTmp);
        ctx.entityRenderer.startLootCollect(
            loot, x0, y0, ctx.logoTmp.x, ctx.logoTmp.y,
            () -> {
                model.applyLootPickup(loot);
                ctx.coinHud.setAmount(ctx.currentTotalCoins());
                if (ctx.lootRewardPopup != null) {
                    ctx.lootRewardPopup.show(loot);
                }
                ctx.gameplayMusic.playLootSting(loot);
            });
    }
}
