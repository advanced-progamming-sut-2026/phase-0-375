package view.gui.screen.gameplay;

import view.gui.ui.ZombiePacketActor;

final class ZombieSeedDrag implements ZombiePacketActor.DragZombie {
    private final GameplayContext ctx;
    private final String zombieName;

    ZombieSeedDrag(GameplayContext ctx, String zombieName) {
        this.ctx = ctx;
        this.zombieName = zombieName;
    }

    @Override
    public void dragStart(ZombiePacketActor packet) {
        ctx.previewPlant = zombieName;
        ctx.previewTime = 0f;
        ctx.entityRenderer.preloadZombieIdle(zombieName, GameplayLevelQueries.currentChapter());
        ctx.stageToScreen.set(packet.getWidth() * 0.5f, packet.getHeight() * 0.5f);
        packet.localToStageCoordinates(ctx.stageToScreen);
        ctx.placement.followPlantDrag(ctx.stageToScreen.x, ctx.stageToScreen.y);
    }

    @Override
    public void drag(ZombiePacketActor packet, float stageX, float stageY) {
        ctx.placement.followPlantDrag(stageX, stageY);
    }

    @Override
    public void dragEnd(ZombiePacketActor packet, float stageX, float stageY) {
        ctx.placement.dropZombie(zombieName, stageX, stageY);
        ctx.previewPlant = null;
        ctx.hoverCol = -1;
        ctx.hoverRow = -1;
    }
}
