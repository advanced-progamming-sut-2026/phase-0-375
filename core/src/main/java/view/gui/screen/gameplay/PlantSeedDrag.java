package view.gui.screen.gameplay;

import view.gui.ui.SeedPacketActor;

final class PlantSeedDrag implements SeedPacketActor.DragPlant {
    private final GameplayContext ctx;
    private final String plantName;

    PlantSeedDrag(GameplayContext ctx, String plantName) {
        this.ctx = ctx;
        this.plantName = plantName;
    }

    @Override
    public void dragStart(SeedPacketActor packet) {
        ctx.previewPlant = plantName;
        if (ctx.dropZombieName == null) {
            ctx.previewTime = 0f;
        }
        ctx.entityRenderer.preloadPlantIdle(plantName);
        ctx.stageToScreen.set(packet.getWidth() * 0.5f, packet.getHeight() * 0.5f);
        packet.localToStageCoordinates(ctx.stageToScreen);
        ctx.placement.followPlantDrag(ctx.stageToScreen.x, ctx.stageToScreen.y);
    }

    @Override
    public void drag(SeedPacketActor packet, float stageX, float stageY) {
        ctx.placement.followPlantDrag(stageX, stageY);
    }

    @Override
    public void dragEnd(SeedPacketActor packet, float stageX, float stageY) {
        ctx.placement.dropPlant(plantName, stageX, stageY);
        ctx.previewPlant = null;
        ctx.hoverCol = -1;
        ctx.hoverRow = -1;
    }
}
