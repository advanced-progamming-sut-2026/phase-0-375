package view.gui.screen.gameplay;

import view.gui.ui.ConveyorBeltHud;
import view.gui.ui.SeedPacketActor;

final class ConveyorPlantDrag implements ConveyorBeltHud.DragCallback {
    private final GameplayContext ctx;

    ConveyorPlantDrag(GameplayContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void onDragStart(SeedPacketActor packet, String plantName) {
        ctx.previewPlant = plantName;
        ctx.previewTime = 0f;
        ctx.entityRenderer.preloadPlantIdle(plantName);
        ctx.stageToScreen.set(packet.getWidth() * 0.5f, packet.getHeight() * 0.5f);
        packet.localToStageCoordinates(ctx.stageToScreen);
        ctx.placement.followPlantDrag(ctx.stageToScreen.x, ctx.stageToScreen.y);
    }

    @Override
    public void onDrag(SeedPacketActor packet, String plantName, float stageX, float stageY) {
        ctx.placement.followPlantDrag(stageX, stageY);
    }

    @Override
    public void onDragEnd(SeedPacketActor packet, String plantName, float stageX, float stageY) {
        ctx.placement.dropPlant(plantName, stageX, stageY);
        ctx.previewPlant = null;
        ctx.hoverCol = -1;
        ctx.hoverRow = -1;
    }
}
