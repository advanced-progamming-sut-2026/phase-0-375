package view.gui.screen.gameplay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import model.app.App;
import model.game.core.GameModel;
import model.game.level.minigame.bowling.WallnutBowlingLevel;
import view.gui.anim.AnimScale;
import view.gui.anim.bowling.BowlingWalnutAnim;
import view.gui.lawn.LawnGridRenderer;
import view.gui.lawn.LawnRowColHighlight;
import view.gui.ui.PlantFoodBankHud;

/** Lawn terrain, highlights, entities, and plant/zombie previews. */
public final class GameplayWorldRenderer {
    private final GameplayContext ctx;

    public GameplayWorldRenderer(GameplayContext ctx) {
        this.ctx = ctx;
    }

    public void render(float delta) {
        drawTerrain(delta);
        drawGridAndLanes();
        drawHighlights();
        ctx.entityRenderer.draw(ctx.view.game.batch, App.getInstance().getCurrentGameModel(), delta);
        drawPreviews();
    }

    public void drawArmedCursors() {
        if (ctx.plantfoodMode) {
            if (ctx.plantfoodCursorRegion == null) {
                ctx.plantfoodCursorRegion = ctx.view.assets.textures.region(PlantFoodBankHud.CURSOR_ID);
            }
            drawHudCursor(ctx.plantfoodCursorRegion, GameplayContext.CURSOR_SIZE);
            return;
        }
        if (!ctx.shovelMode) {
            return;
        }
        if (ctx.shovelCursorRegion == null) {
            ctx.shovelCursorRegion = ctx.view.assets.textures.region(GameplayContext.SHOVEL_CURSOR_ID);
        }
        float width = ctx.shovelCursorRegion == null || ctx.shovelCursorRegion.getRegionWidth() <= 0
            ? GameplayContext.CURSOR_SIZE
            : ctx.shovelCursorRegion.getRegionWidth();
        drawHudCursor(ctx.shovelCursorRegion, width);
    }

    private void drawTerrain(float delta) {
        ctx.lawnBackground.draw(ctx.view.game.batch);
        if (ctx.waterUnderlayer != null) {
            ctx.waterUnderlayer.draw(ctx.view.game.batch, App.getInstance().getCurrentGameModel(), delta);
        }
        if (ctx.necromancyTiles != null) {
            ctx.necromancyTiles.draw(ctx.view.game.batch, ctx.lawnLayout,
                App.getInstance().getCurrentGameModel(), delta);
        }
        if (ctx.protectTileRenderer != null) {
            ctx.protectTileRenderer.draw(
                ctx.view.game.batch, ctx.lawnLayout, App.getInstance().getCurrentGameModel());
        }
    }

    private void drawGridAndLanes() {
        GameModel model = App.getInstance().getCurrentGameModel();
        if (model != null && model.isShowLawnGrid()) {
            if (ctx.lawnGridRenderer == null) {
                ctx.lawnGridRenderer = new LawnGridRenderer();
            }
            ctx.lawnGridRenderer.draw(ctx.view.game.batch, ctx.lawnLayout);
        }
        if (ctx.deadLineRenderer != null) {
            ctx.deadLineRenderer.draw(ctx.view.game.batch, ctx.lawnLayout, GameplayLevelQueries.deadLineColumn());
        }
        if (ctx.brainLaneRenderer != null) {
            ctx.brainLaneRenderer.draw(ctx.view.game.batch, ctx.lawnLayout, model);
        }
    }

    private void drawHighlights() {
        boolean dropHighlight = ctx.zombieDropMode && ctx.dropCol >= 0 && ctx.dropRow >= 0;
        boolean highlight = cellHighlightWanted();
        if (!highlight && !dropHighlight) {
            return;
        }
        if (ctx.rowColHighlight == null) {
            ctx.rowColHighlight = new LawnRowColHighlight();
        }
        if (highlight) {
            ctx.rowColHighlight.draw(ctx.view.game.batch, ctx.lawnLayout, ctx.hoverCol, ctx.hoverRow);
        }
        if (dropHighlight) {
            ctx.rowColHighlight.draw(ctx.view.game.batch, ctx.lawnLayout, ctx.dropCol, ctx.dropRow);
        }
    }

    private boolean cellHighlightWanted() {
        boolean highlight = (ctx.previewPlant != null || ctx.plantfoodMode || ctx.shovelMode || ctx.swapDragging)
            && ctx.hoverCol >= 0;
        if (highlight && ctx.bowlingMode && ctx.previewPlant != null
                && !GameplayLevelQueries.canBowlAt(ctx.hoverCol)) {
            return false;
        }
        if (highlight && ctx.useZombiePackets && !ctx.couchPlayMode && ctx.previewPlant != null
                && !GameplayLevelQueries.canPlaceIZombieAt(ctx.hoverCol)) {
            return false;
        }
        if (highlight && (ctx.multiplayerPlantSide || ctx.couchPlayMode) && ctx.previewPlant != null
                && !GameplayLevelQueries.canPlaceMultiplayerPlantAt(ctx.hoverCol)) {
            return false;
        }
        return highlight;
    }

    private void drawPreviews() {
        if (ctx.zombieDropMode && ctx.dropZombieName != null && ctx.dropCol >= 0) {
            float[] xy = ctx.lawnLayout.centerOf(ctx.dropRow, ctx.dropCol);
            ctx.entityRenderer.drawZombieIdle(
                ctx.view.game.batch, ctx.dropZombieName, xy[0], xy[1], ctx.previewTime,
                GameplayLevelQueries.currentChapter());
        }
        if (ctx.previewPlant == null) {
            return;
        }
        if (ctx.useZombiePackets && !ctx.couchPlayMode) {
            ctx.entityRenderer.drawZombieIdle(
                ctx.view.game.batch, ctx.previewPlant, ctx.worldTmp.x, ctx.worldTmp.y, ctx.previewTime,
                GameplayLevelQueries.currentChapter());
            return;
        }
        float scale = ctx.bowlingMode
            ? BowlingWalnutAnim.scale(WallnutBowlingLevel.parseWalnutType(ctx.previewPlant))
            : AnimScale.PLANT;
        ctx.entityRenderer.drawPlantIdle(
            ctx.view.game.batch, ctx.previewPlant, ctx.worldTmp.x, ctx.worldTmp.y, ctx.previewTime, scale);
    }

    private void drawHudCursor(TextureRegion region, float width) {
        if (region == null) {
            return;
        }
        ctx.cursorUnprojectTmp.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        ctx.view.uiViewport.unproject(ctx.cursorUnprojectTmp);
        float height = region.getRegionWidth() <= 0f
            ? width
            : width * (region.getRegionHeight() / (float) region.getRegionWidth());
        ctx.view.game.batch.setProjectionMatrix(ctx.view.uiCamera.combined);
        ctx.view.game.batch.begin();
        ctx.view.game.batch.draw(
            region,
            ctx.cursorUnprojectTmp.x - width * 0.5f,
            ctx.cursorUnprojectTmp.y - height * 0.5f,
            width, height);
        ctx.view.game.batch.end();
    }
}
