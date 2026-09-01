package view.gui.screen.gameplay;

import model.game.core.GameModel;
import model.network.packet.chat.ReactionPacket;

/** Hide/dispose teardown for lawn collaborators. */
public final class GameplayLifecycle {
    private final GameplayContext ctx;

    public GameplayLifecycle(GameplayContext ctx) {
        this.ctx = ctx;
    }

    public void hide() {
        ctx.flow.forfeitMultiplayerMatchIfActive();
        if (ctx.multiplayerClient != null && ctx.reactionPacketHandler != null) {
            ctx.multiplayerClient.unregisterHandler(ReactionPacket.class, ctx.reactionPacketHandler);
            ctx.reactionPacketHandler = null;
        }
        if (ctx.localReactionBubble != null) {
            ctx.localReactionBubble.dispose();
        }
        if (ctx.remoteReactionBubble != null) {
            ctx.remoteReactionBubble.dispose();
        }
        if (ctx.plantfoodMode) {
            ctx.cursors.setPlantfoodMode(false);
        }
        if (ctx.shovelMode) {
            ctx.cursors.setShovelMode(false);
        }
        ctx.flow.flushPendingLoot();
        GameModel model = GameplayLevelQueries.model();
        if (model != null) {
            model.setGameEventListener(null);
        }
    }

    public void dispose() {
        disposeRenderers();
        disposeHudCaches();
        ctx.localReactionBubble = null;
        ctx.remoteReactionBubble = null;
        ctx.reactionPamClips = null;
        ctx.cursors.restoreOsCursor();
        if (ctx.hiddenCursor != null) {
            ctx.hiddenCursor.dispose();
            ctx.hiddenCursor = null;
        }
    }

    private void disposeRenderers() {
        if (ctx.entityOverlay != null) {
            ctx.entityOverlay.dispose();
        }
        if (ctx.zombossHpHud != null) {
            ctx.zombossHpHud.dispose();
            ctx.zombossHpHud = null;
        }
        if (ctx.rowColHighlight != null) {
            ctx.rowColHighlight.dispose();
        }
        if (ctx.necromancyTiles != null) {
            ctx.necromancyTiles.dispose();
        }
        if (ctx.lawnGridRenderer != null) {
            ctx.lawnGridRenderer.dispose();
            ctx.lawnGridRenderer = null;
        }
        if (ctx.deadLineRenderer != null) {
            ctx.deadLineRenderer.dispose();
        }
    }

    private void disposeHudCaches() {
        if (ctx.conveyorHud != null) {
            ctx.conveyorHud.dispose();
            ctx.conveyorHud = null;
        }
        if (ctx.sheetClips != null) {
            ctx.sheetClips.dispose();
            ctx.sheetClips = null;
        }
    }
}
