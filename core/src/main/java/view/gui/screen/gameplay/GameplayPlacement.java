package view.gui.screen.gameplay;

import controller.result.CommandResult;
import model.app.App;
import model.game.core.GameModel;
import model.game.level.minigame.vasebreaker.Vase;
import model.game.level.minigame.vasebreaker.VaseBreakerLevel;
import model.item.PlantFoodPickup;
import model.item.Sun;
import model.network.packet.game.CollectSunRequestPacket;
import model.network.packet.game.PlacePlantRequestPacket;
import model.network.packet.game.PlaceZombieRequestPacket;
import view.gui.anim.vase.VaseBreakerAnim;
import view.gui.audio.GameAudio;
import view.gui.audio.GameSfx;
import view.gui.ui.IZombieDropCursor;

/** Plant/zombie drops, world clicks, collectibles, and beghouled swaps. */
public final class GameplayPlacement {
    private final GameplayContext ctx;

    public GameplayPlacement(GameplayContext ctx) {
        this.ctx = ctx;
    }

    public void followPlantDrag(float stageX, float stageY) {
        stageToWorld(stageX, stageY);
        if (ctx.lawnLayout.worldToCell(ctx.worldTmp.x, ctx.worldTmp.y, ctx.cellTmp)) {
            ctx.hoverCol = ctx.cellTmp[0];
            ctx.hoverRow = ctx.cellTmp[1];
        } else {
            ctx.hoverCol = -1;
            ctx.hoverRow = -1;
        }
    }

    public void stageToWorld(float stageX, float stageY) {
        ctx.stageToScreen.set(stageX, stageY);
        ctx.view.uiStage.stageToScreenCoordinates(ctx.stageToScreen);
        ctx.view.worldViewport.unproject(ctx.worldTmp.set(ctx.stageToScreen.x, ctx.stageToScreen.y, 0f));
    }

    public void dropPlant(String plantName, float stageX, float stageY) {
        if (ctx.isPregame()) {
            return;
        }
        stageToWorld(stageX, stageY);
        if (!ctx.lawnLayout.worldToCell(ctx.worldTmp.x, ctx.worldTmp.y, ctx.cellTmp)) {
            return;
        }
        if (dropPlantNetwork(plantName)) {
            return;
        }
        if (ctx.couchPlayMode && !GameplayLevelQueries.canPlaceMultiplayerPlantAt(ctx.cellTmp[0])) {
            ctx.view.toast("Plants must be placed behind the red line.", true);
            return;
        }
        plantLocally(plantName);
    }

    public void dropZombie(String zombieName, float stageX, float stageY) {
        if (ctx.isPregame()) {
            return;
        }
        stageToWorld(stageX, stageY);
        if (!ctx.lawnLayout.worldToCell(ctx.worldTmp.x, ctx.worldTmp.y, ctx.cellTmp)) {
            return;
        }
        if (ctx.multiplayerMode) {
            dropZombieNetwork(zombieName);
            return;
        }
        CommandResult<Void> result = ctx.gameplay.placeZombie(zombieName, ctx.cellTmp[0], ctx.cellTmp[1]);
        ctx.view.toast(result.getMessage(), !result.isSuccess());
        ctx.logic.syncSunHuds(App.getInstance().getCurrentGameModel());
        ctx.packets.refreshChrome();
    }

    public boolean onWorldClick(float worldX, float worldY) {
        if (ctx.endSequenceActive) {
            return true;
        }
        if (ctx.plantfoodMode) {
            tryFeedPlantFood(worldX, worldY);
            ctx.cursors.setPlantfoodMode(false);
            return true;
        }
        if (ctx.shovelMode) {
            tryPluck(worldX, worldY);
            ctx.cursors.setShovelMode(false);
            return true;
        }
        if (ctx.vaseBreakerMode && tryBreakVase(worldX, worldY)) {
            return true;
        }
        return tryCollectPlantFood(worldX, worldY) || tryCollectSun(worldX, worldY);
    }

    public void onCellHover(int col, int row) {
        if (!ctx.plantfoodMode && !ctx.shovelMode) {
            return;
        }
        ctx.hoverCol = col;
        ctx.hoverRow = row;
    }

    public void tryBeghouledUpgrade(String fromType) {
        if (ctx.isPregame() || ctx.endSequenceActive) {
            return;
        }
        CommandResult<Void> result = ctx.gameplay.upgradePlant(fromType);
        ctx.view.purchase(result);
        GameModel model = GameplayLevelQueries.model();
        if (ctx.sunHud != null && model != null) {
            ctx.sunHud.setAmount(model.getSunAmount());
        }
        if (ctx.beghouledMatchHud != null) {
            ctx.beghouledMatchHud.sync(model);
        }
        ctx.packets.refreshChrome();
    }

    public void tryBeghouledSwap(int fromCol, int fromRow, int toCol, int toRow) {
        if (fromCol == toCol && fromRow == toRow) {
            return;
        }
        String direction = swapDirection(fromCol, fromRow, toCol, toRow);
        if (direction == null) {
            ctx.view.toast("Swap with an adjacent plant.", true);
            return;
        }
        CommandResult<Void> result = ctx.gameplay.swapPlant(fromCol, fromRow, direction);
        ctx.view.toast(result.getMessage(), !result.isSuccess());
        GameModel model = GameplayLevelQueries.model();
        if (ctx.sunHud != null && model != null) {
            ctx.sunHud.setAmount(model.getSunAmount());
        }
        if (ctx.beghouledMatchHud != null) {
            ctx.beghouledMatchHud.sync(model);
        }
        ctx.packets.refreshChrome();
    }

    public void enterZombieDrop(String zombieName) {
        if (zombieName == null) {
            return;
        }
        boolean wasDropping = ctx.zombieDropMode;
        ctx.zombieDropMode = true;
        ctx.dropZombieName = zombieName;
        if (ctx.previewPlant == null) {
            ctx.previewTime = 0f;
        }
        ctx.entityRenderer.preloadZombieIdle(zombieName, GameplayLevelQueries.currentChapter());
        if (!wasDropping || ctx.dropCol < 0) {
            placeDropOrigin();
        }
        ctx.packets.refreshChrome();
    }

    public void nudgeDropCursor(int dCol, int dRow) {
        var level = GameplayLevelQueries.currentLevel();
        if (!(level instanceof model.game.level.minigame.izombie.IZombieLevel iZombie)
                || level.getConfig() == null) {
            return;
        }
        int[] cell = {ctx.dropCol, ctx.dropRow};
        IZombieDropCursor.nudge(cell, dCol, dRow,
                iZombie.redLineColumn(),
                level.getConfig().getColumns() - 1,
                level.getConfig().getRows());
        ctx.dropCol = cell[0];
        ctx.dropRow = cell[1];
    }

    public void confirmZombieDrop() {
        if (!ctx.zombieDropMode || ctx.dropZombieName == null || ctx.dropCol < 0 || ctx.isPregame()) {
            return;
        }
        CommandResult<Void> result = ctx.gameplay.placeZombie(ctx.dropZombieName, ctx.dropCol, ctx.dropRow);
        ctx.view.toast(result.getMessage(), !result.isSuccess());
        ctx.logic.syncSunHuds(App.getInstance().getCurrentGameModel());
        ctx.packets.refreshChrome();
    }

    public void cancelZombieDrop() {
        if (!ctx.zombieDropMode && ctx.dropCol < 0) {
            return;
        }
        ctx.zombieDropMode = false;
        ctx.dropZombieName = null;
        ctx.dropCol = -1;
        ctx.dropRow = -1;
        ctx.packets.refreshChrome();
    }

    private boolean dropPlantNetwork(String plantName) {
        if (!ctx.multiplayerMode) {
            return false;
        }
        if (!ctx.multiplayerPlantSide) {
            ctx.view.toast("Only the plant player can plant.", true);
            return true;
        }
        if (!GameplayLevelQueries.canPlaceMultiplayerPlantAt(ctx.cellTmp[0])) {
            ctx.view.toast("Plants must be placed behind the red line.", true);
            return true;
        }
        if (ctx.multiplayerClient != null && ctx.multiplayerClient.isConnected()) {
            ctx.multiplayerClient.sendPacket(
                new PlacePlantRequestPacket(plantName, ctx.cellTmp[1], ctx.cellTmp[0]));
        }
        ctx.packets.refreshChrome();
        return true;
    }

    private void plantLocally(String plantName) {
        boolean hadBoost = GameplayPlantMeta.boosted(plantName);
        CommandResult<Void> result = ctx.gameplay.plant(plantName, ctx.cellTmp[0], ctx.cellTmp[1]);
        if (result.isSuccess()) {
            GameAudio.playPlantPlaceSfx(plantName);
            if (hadBoost) {
                GameAudio.get().playSfx(GameSfx.PLANT_FOOD);
            }
        }
        ctx.view.toast(result.getMessage(), !result.isSuccess());
        ctx.logic.syncSunHuds(App.getInstance().getCurrentGameModel());
        if (ctx.conveyorMode || ctx.bowlingMode || ctx.vaseBreakerMode) {
            ctx.packets.refresh();
        } else {
            ctx.packets.refreshChrome();
        }
    }

    private void dropZombieNetwork(String zombieName) {
        if (ctx.multiplayerPlantSide) {
            ctx.view.toast("Only the zombie player can spawn zombies.", true);
            return;
        }
        if (!GameplayLevelQueries.canPlaceIZombieAt(ctx.cellTmp[0])) {
            ctx.view.toast("Zombies must be spawned at/right of the red line.", true);
            return;
        }
        if (ctx.multiplayerClient != null && ctx.multiplayerClient.isConnected()) {
            ctx.multiplayerClient.sendPacket(
                new PlaceZombieRequestPacket(zombieName, ctx.cellTmp[1], ctx.cellTmp[0]));
        }
        ctx.packets.refreshChrome();
    }

    private boolean tryBreakVase(float worldX, float worldY) {
        if (ctx.isPregame() || !ctx.lawnLayout.worldToCell(worldX, worldY, ctx.cellTmp)) {
            return false;
        }
        var level = GameplayLevelQueries.currentLevel();
        if (!(level instanceof VaseBreakerLevel vaseLevel)) {
            return false;
        }
        int col = ctx.cellTmp[0];
        int row = ctx.cellTmp[1];
        Vase vase = vaseLevel.vaseAt(col, row);
        if (vase == null) {
            return false;
        }
        String pam = VaseBreakerAnim.pamPath(vase);
        CommandResult<Void> result = ctx.gameplay.breakVase(col, row);
        ctx.view.toast(result.getMessage(), !result.isSuccess());
        if (result.isSuccess()) {
            ctx.entityRenderer.playVaseBreak(pam, col, row);
            ctx.packets.refresh();
        }
        return true;
    }

    private void tryFeedPlantFood(float worldX, float worldY) {
        if (!ctx.lawnLayout.worldToCell(worldX, worldY, ctx.cellTmp)) {
            return;
        }
        CommandResult<Void> result = ctx.gameplay.feed(ctx.cellTmp[0], ctx.cellTmp[1]);
        if (result.isSuccess()) {
            GameAudio.get().playSfx(GameSfx.PLANT_FOOD);
        }
        ctx.view.toast(result.getMessage(), !result.isSuccess());
    }

    private void tryPluck(float worldX, float worldY) {
        if (!ctx.lawnLayout.worldToCell(worldX, worldY, ctx.cellTmp)) {
            return;
        }
        CommandResult<Void> result = ctx.gameplay.pluck(ctx.cellTmp[0], ctx.cellTmp[1]);
        if (result.isSuccess()) {
            GameAudio.get().playSfx(GameSfx.SHOVEL);
        }
        ctx.view.toast(result.getMessage(), !result.isSuccess());
    }

    private boolean tryCollectPlantFood(float worldX, float worldY) {
        GameModel model = GameplayLevelQueries.model();
        if (model == null) {
            return false;
        }
        PlantFoodPickup food = ctx.entityRenderer.pickPlantFood(model, worldX, worldY);
        if (food == null) {
            return false;
        }
        CommandResult<Void> result = ctx.gameplay.collectPlantFood(food);
        if (!result.isSuccess()) {
            return false;
        }
        startPlantFoodFlight(model, food);
        return true;
    }

    private void startPlantFoodFlight(GameModel model, PlantFoodPickup food) {
        ctx.entityRenderer.writePlantFoodDrawPos(food, ctx.sunPosTmp);
        float destX = ctx.sunPosTmp[0];
        float destY = ctx.sunPosTmp[1];
        if (ctx.plantFoodBank != null) {
            ctx.plantFoodBank.logoCenter(ctx.logoTmp);
            destX = ctx.logoTmp.x;
            destY = ctx.logoTmp.y;
            ctx.plantFoodBank.setCount(model.getPlantFoodCount());
        }
        ctx.entityRenderer.startPlantFoodCollect(food, ctx.sunPosTmp[0], ctx.sunPosTmp[1], destX, destY);
    }

    private boolean tryCollectSun(float worldX, float worldY) {
        GameModel model = GameplayLevelQueries.model();
        if (model == null || (ctx.multiplayerMode && !ctx.multiplayerPlantSide)) {
            return false;
        }
        Sun sun = ctx.entityRenderer.pickSun(model, worldX, worldY);
        if (sun == null) {
            return false;
        }
        ctx.entityRenderer.writeSunDrawPos(sun, ctx.sunPosTmp);
        if (ctx.multiplayerMode) {
            collectSunNetwork(sun);
            return true;
        }
        return collectSunLocal(model, sun);
    }

    private void collectSunNetwork(Sun sun) {
        if (ctx.multiplayerClient != null && ctx.multiplayerClient.isConnected()) {
            ctx.multiplayerClient.sendPacket(new CollectSunRequestPacket(sun.getX(), sun.getY()));
        }
        float destX = ctx.sunPosTmp[0];
        float destY = ctx.sunPosTmp[1];
        if (ctx.sunHud != null) {
            ctx.sunHud.logoCenter(ctx.logoTmp);
            destX = ctx.logoTmp.x;
            destY = ctx.logoTmp.y;
        }
        ctx.entityRenderer.startSunCollect(sun, ctx.sunPosTmp[0], ctx.sunPosTmp[1], destX, destY);
    }

    private boolean collectSunLocal(GameModel model, Sun sun) {
        CommandResult<Void> result = ctx.gameplay.collectSun(sun);
        if (!result.isSuccess()) {
            return false;
        }
        GameAudio.get().playSfx(GameSfx.COLLECT_SUN);
        float destX = ctx.sunPosTmp[0];
        float destY = ctx.sunPosTmp[1];
        if (ctx.sunHud != null) {
            ctx.sunHud.logoCenter(ctx.logoTmp);
            destX = ctx.logoTmp.x;
            destY = ctx.logoTmp.y;
            ctx.logic.syncSunHuds(model);
        }
        ctx.entityRenderer.startSunCollect(sun, ctx.sunPosTmp[0], ctx.sunPosTmp[1], destX, destY);
        ctx.packets.refreshChrome();
        return true;
    }

    private void placeDropOrigin() {
        var iZombie = GameplayLevelQueries.currentLevel()
            instanceof model.game.level.minigame.izombie.IZombieLevel level ? level : null;
        int red = iZombie != null ? iZombie.redLineColumn() : 0;
        int[] cell = {ctx.dropCol, ctx.dropRow};
        IZombieDropCursor.origin(cell, red);
        ctx.dropCol = cell[0];
        ctx.dropRow = cell[1];
    }

    private static String swapDirection(int fromCol, int fromRow, int toCol, int toRow) {
        int dc = toCol - fromCol;
        int dr = toRow - fromRow;
        if (dc == 1 && dr == 0) {
            return "right";
        }
        if (dc == -1 && dr == 0) {
            return "left";
        }
        if (dc == 0 && dr == 1) {
            return "down";
        }
        if (dc == 0 && dr == -1) {
            return "up";
        }
        return null;
    }
}
