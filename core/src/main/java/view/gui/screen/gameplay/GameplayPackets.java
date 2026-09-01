package view.gui.screen.gameplay;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import model.game.core.GameModel;
import model.game.level.minigame.vasebreaker.PendingSeedPacket;
import view.gui.assets.SheetPacketPortraits;
import view.gui.ui.ConveyorBeltHud;
import view.gui.ui.SeedPacketActor;
import view.gui.ui.ZombieHotkeys;
import view.gui.ui.ZombiePacketActor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Seed / zombie packet column rebuild and afford/cooldown chrome. */
public final class GameplayPackets {
    private final GameplayContext ctx;

    public GameplayPackets(GameplayContext ctx) {
        this.ctx = ctx;
    }

    public void refresh() {
        if (ctx.conveyorMode && ctx.conveyorHud != null) {
            List<String> selected = hudPlantNames();
            ctx.shownPackets = new ArrayList<>(selected);
            ctx.conveyorHud.sync(selected);
            refreshChrome();
            return;
        }
        if (ctx.beghouledMode) {
            refreshBeghouledUpgrades();
            return;
        }
        if (ctx.useZombiePackets && !ctx.couchPlayMode) {
            refreshIZombiePackets();
            return;
        }
        rebuildPlantColumn();
    }

    public void refreshChrome() {
        if (ctx.conveyorMode && ctx.conveyorHud != null) {
            for (SeedPacketActor packet : ctx.conveyorHud.getPacketActors()) {
                packet.setDimmed(false);
            }
            return;
        }
        GameModel model = GameplayLevelQueries.model();
        int sun = model == null ? 0 : model.getSunAmount();
        int plantSun = ctx.couchPlayMode && model != null ? model.getPlantSun() : sun;
        if (dimBeghouled(sun) || dimZombiePackets(sun)) {
            return;
        }
        dimSeedPackets(model, plantSun);
    }

    public List<String> hudPlantNames() {
        if (ctx.beghouledMode) {
            return GameplayLevelQueries.beghouledUpgradeFromNames();
        }
        if (ctx.useZombiePackets && !ctx.couchPlayMode) {
            return GameplayLevelQueries.iZombieRosterNames();
        }
        if (ctx.vaseBreakerMode) {
            return vasePlantNames();
        }
        return GameplayLevelQueries.selectedPlants();
    }

    public void applySheetPortrait(SeedPacketActor packet, String plantName) {
        SheetPacketPortraits.applyIfNeeded(packet, plantName, ctx.view.assets, ctx.sheetClips);
    }

    private void rebuildPlantColumn() {
        List<String> selected = hudPlantNames();
        ctx.shownPackets = new ArrayList<>(selected);
        ctx.packetColumn.clearChildren();
        List<PendingSeedPacket> pending = ctx.vaseBreakerMode
            ? GameplayLevelQueries.pendingPackets() : List.of();
        int pendingIndex = 0;
        for (String name : selected) {
            ctx.packetColumn.add(createSeedPacket(name, pending, pendingIndex))
                .size(SeedPacketActor.PACKET_WIDTH, SeedPacketActor.PACKET_HEIGHT)
                .padBottom(6f).row();
            pendingIndex++;
        }
        if (ctx.couchPlayMode) {
            refreshIZombiePackets();
        } else {
            refreshChrome();
        }
    }

    private SeedPacketActor createSeedPacket(
            String name, List<PendingSeedPacket> pending, int pendingIndex) {
        SeedPacketActor packet = (ctx.bowlingMode || ctx.vaseBreakerMode)
            ? new SeedPacketActor(ctx.view.assets.textures, ctx.view.skin, name, 0, 1, false, false, false)
            : new SeedPacketActor(
                ctx.view.assets.textures, ctx.view.skin, name, GameplayPlantMeta.cost(name),
                GameplayPlantMeta.level(name), GameplayPlantMeta.boosted(name), false);
        applySheetPortrait(packet, name);
        if (ctx.vaseBreakerMode) {
            packet.enableExpiryTimer(ctx.view.skin);
            if (pendingIndex < pending.size()) {
                packet.setExpirySeconds(pending.get(pendingIndex).getTimeToExpiry());
            }
        }
        packet.onDragPlant(new PlantSeedDrag(ctx, name));
        return packet;
    }

    private void refreshIZombiePackets() {
        List<String> roster = GameplayLevelQueries.iZombieRosterNames();
        if (!ctx.couchPlayMode) {
            ctx.shownPackets = new ArrayList<>(roster);
        }
        Table host = ctx.zombiePacketColumn != null ? ctx.zombiePacketColumn : ctx.packetColumn;
        host.clearChildren();
        Map<String, Integer> costs = GameplayLevelQueries.iZombieRosterCosts();
        int index = 0;
        for (String name : roster) {
            host.add(createZombiePacket(name, costs.getOrDefault(name, 0), index))
                .size(ZombiePacketActor.PACKET_WIDTH, ZombiePacketActor.PACKET_HEIGHT)
                .padRight(6f);
            index++;
        }
        refreshChrome();
    }

    private ZombiePacketActor createZombiePacket(String name, int cost, int index) {
        ZombiePacketActor packet = new ZombiePacketActor(ctx.view.assets.textures, ctx.view.skin, name, cost);
        if (ctx.couchPlayMode) {
            packet.setPickable(false);
            char letter = ZombieHotkeys.letterAt(index);
            if (letter != 0) {
                packet.setHotkey(ctx.view.skin, letter);
            }
        } else {
            packet.onDragZombie(new ZombieSeedDrag(ctx, name));
        }
        return packet;
    }

    private void refreshBeghouledUpgrades() {
        ctx.shownPackets = new ArrayList<>(GameplayLevelQueries.beghouledUpgradeFromNames());
        ctx.packetColumn.clearChildren();
        var level = GameplayLevelQueries.currentLevel();
        if (!(level instanceof model.game.level.minigame.beghouled.BeghouledLevel beghouled)) {
            return;
        }
        for (var rule : beghouled.getSettings().getUpgrades()) {
            String from = rule.getFrom();
            SeedPacketActor packet = new SeedPacketActor(
                ctx.view.assets.textures, ctx.view.skin, from, rule.getCost(), 1, false, false, true);
            applySheetPortrait(packet, from);
            packet.onClick(() -> ctx.placement.tryBeghouledUpgrade(from));
            ctx.packetColumn.add(packet)
                .size(SeedPacketActor.PACKET_WIDTH, SeedPacketActor.PACKET_HEIGHT)
                .padBottom(6f).row();
        }
        refreshChrome();
    }

    private boolean dimBeghouled(int sun) {
        if (!ctx.beghouledMode) {
            return false;
        }
        Map<String, Integer> costs = GameplayLevelQueries.beghouledUpgradeCosts(
            GameplayLevelQueries.currentLevel());
        for (com.badlogic.gdx.scenes.scene2d.Actor actor : ctx.packetColumn.getChildren()) {
            if (!(actor instanceof SeedPacketActor packet) || packet.plantName() == null) {
                continue;
            }
            Integer cost = costs.get(packet.plantName());
            packet.setDimmed(cost != null && cost > sun);
        }
        return true;
    }

    private boolean dimZombiePackets(int sun) {
        if (!ctx.useZombiePackets) {
            return false;
        }
        Map<String, Integer> costs = GameplayLevelQueries.iZombieRosterCosts();
        Table host = ctx.zombiePacketColumn != null ? ctx.zombiePacketColumn : ctx.packetColumn;
        for (com.badlogic.gdx.scenes.scene2d.Actor actor : host.getChildren()) {
            if (!(actor instanceof ZombiePacketActor packet) || packet.zombieName() == null) {
                continue;
            }
            Integer cost = costs.get(packet.zombieName());
            packet.setDimmed(cost != null && cost > sun);
            packet.setSelected(ctx.zombieDropMode && packet.zombieName().equals(ctx.dropZombieName));
        }
        return !ctx.couchPlayMode;
    }

    private void dimSeedPackets(GameModel model, int plantSun) {
        if (ctx.multiplayerMode && ctx.multiplayerPlantSide) {
            dimMultiplayerPlants(model, plantSun);
            return;
        }
        List<PendingSeedPacket> pending = ctx.vaseBreakerMode
            ? GameplayLevelQueries.pendingPackets() : List.of();
        int pendingIndex = 0;
        for (com.badlogic.gdx.scenes.scene2d.Actor actor : ctx.packetColumn.getChildren()) {
            if (!(actor instanceof SeedPacketActor packet) || packet.plantName() == null) {
                continue;
            }
            pendingIndex = dimOneSeed(model, plantSun, packet, pending, pendingIndex);
        }
    }

    private void dimMultiplayerPlants(GameModel model, int plantSun) {
        for (com.badlogic.gdx.scenes.scene2d.Actor actor : ctx.packetColumn.getChildren()) {
            if (!(actor instanceof SeedPacketActor packet) || packet.plantName() == null) {
                continue;
            }
            String name = packet.plantName();
            boolean afford = GameplayPlantMeta.cost(name) <= plantSun;
            boolean ready = model == null || model.isSeedReady(name);
            packet.setDimmed(!afford || !ready);
        }
    }

    private int dimOneSeed(
            GameModel model, int plantSun, SeedPacketActor packet,
            List<PendingSeedPacket> pending, int pendingIndex) {
        String name = packet.plantName();
        if (ctx.bowlingMode || ctx.vaseBreakerMode) {
            packet.setDimmed(false);
            if (ctx.vaseBreakerMode && pendingIndex < pending.size()) {
                packet.setExpirySeconds(pending.get(pendingIndex).getTimeToExpiry());
            }
            return pendingIndex + 1;
        }
        boolean ready = model == null || model.isSeedReady(name);
        packet.setDimmed(!ready || GameplayPlantMeta.cost(name) > plantSun);
        packet.setCooldownFraction(GameplayPlantMeta.seedCooldownFraction(model, name));
        return pendingIndex;
    }

    private static List<String> vasePlantNames() {
        List<String> names = new ArrayList<>();
        for (PendingSeedPacket packet : GameplayLevelQueries.pendingPackets()) {
            if (packet.getPlant() != null && packet.getPlant().getName() != null) {
                names.add(packet.getPlant().getName());
            }
        }
        return names;
    }

    public ConveyorBeltHud.DragCallback conveyorDrag() {
        return new ConveyorPlantDrag(ctx);
    }
}
