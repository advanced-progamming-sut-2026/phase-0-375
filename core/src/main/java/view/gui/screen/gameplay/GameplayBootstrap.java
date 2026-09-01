package view.gui.screen.gameplay;

import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import model.enums.Chapter;
import model.event.GameEvent;
import model.game.core.GameModel;
import view.gui.audio.GameAudio;
import view.gui.audio.GameplayCombatSfx;
import view.gui.audio.GameSfx;
import view.gui.anim.SpritesheetClipCache;
import view.gui.assets.AdventureHudRegions;
import view.gui.assets.ZombiePacketIds;
import view.gui.lawn.BrainLaneRenderer;
import view.gui.lawn.DeadLineRenderer;
import view.gui.lawn.DebugEntityOverlay;
import view.gui.lawn.LawnBackgroundRenderer;
import view.gui.lawn.LawnEntityRenderer;
import view.gui.lawn.LawnLayout;
import view.gui.lawn.NecromancyTileRenderer;
import view.gui.lawn.ProtectTileRenderer;
import view.gui.lawn.WaterUnderlayerRenderer;
import view.gui.screen.AbstractGameplayScreen;
import view.gui.ui.PauseMenuOverlay;
import view.gui.ui.PlantFoodBankHud;

import java.util.function.Consumer;

/** Loads lawn renderers, atlases, and world input for a gameplay session. */
public final class GameplayBootstrap {
    private GameplayBootstrap() {}

    public static void finish(
            GameplayContext ctx,
            Consumer<InputProcessor> setWorldInput,
            WorldClickFactory clickFactory
    ) {
        initWorld(ctx);
        loadAtlases(ctx);
        wireInput(ctx, setWorldInput, clickFactory);
        ctx.hudBuilder.build();
        GameModel model = GameplayLevelQueries.model();
        if (model != null) {
            model.setGameEventListener(evt -> {
                if (evt != null && evt.getType() == GameEvent.Type.PROJECTILE_FIRED
                        && GameplayCombatSfx.fireProjectileEnabled) {
                    GameAudio.get().playSfx(GameSfx.FIRE_PROJECTILE);
                }
            });
        }
        showRoleToast(ctx);
    }

    private static void initWorld(GameplayContext ctx) {
        ctx.gameplayMusic.reset();
        ctx.protectTileRenderer = new ProtectTileRenderer(ctx.view.assets.textures);
        if (ctx.saveOurSeedsMode) {
            ctx.protectTileRenderer.ensureLoaded();
        }
        Chapter chapter = GameplayLevelQueries.currentChapter();
        LawnBackgroundRenderer.Style lawnStyle = LawnBackgroundRenderer.Style.forChapter(chapter);
        ctx.lawnBackground = new LawnBackgroundRenderer(ctx.view.assets.textures, lawnStyle);
        ctx.lawnBackground.ensureLoaded();
        ctx.waterUnderlayer = chapter == Chapter.BIG_WAVE_BEACH
            ? new WaterUnderlayerRenderer(ctx.view.assets, ctx.lawnLayout) : null;
        if (chapter == Chapter.DARK_AGES) {
            ctx.necromancyTiles = new NecromancyTileRenderer();
        }
        initEntities(ctx, chapter);
    }

    private static void initEntities(GameplayContext ctx, Chapter chapter) {
        ctx.entityOverlay = new DebugEntityOverlay(ctx.lawnLayout, resolveFont(ctx.view.skin));
        ctx.entityRenderer = new LawnEntityRenderer(ctx.view.assets, ctx.lawnLayout, ctx.entityOverlay);
        ctx.entityRenderer.setScreenShake(ctx.view.screenShake);
        ctx.entityRenderer.resetMowers(chapter, GameplayLevelQueries.lawnMowersEnabled());
        if (ctx.vaseBreakerMode) {
            ctx.entityRenderer.preloadVases();
        }
        ctx.entityRenderer.preloadCraters();
        if (ctx.bowlingMode || ctx.iZombieMode || GameplayLevelQueries.deadLineColumn() >= 0) {
            ctx.deadLineRenderer = new DeadLineRenderer();
        }
        if (ctx.iZombieMode) {
            ctx.brainLaneRenderer = new BrainLaneRenderer(ctx.view.assets.textures);
            ctx.brainLaneRenderer.ensureLoaded();
        }
    }

    private static void loadAtlases(GameplayContext ctx) {
        if (ctx.useZombiePackets) {
            ctx.view.assets.textures.loadSync(ZombiePacketIds.ATLAS_GROUP);
            ctx.view.assets.textures.loadSync(ZombiePacketIds.ATLAS_PAGE);
        }
        ctx.view.assets.textures.loadSync("UI_SeedPackets_768");
        ctx.view.assets.textures.loadSync("ATLASIMAGE_ATLAS_UI_SEEDPACKETS_768_00");
        if (ctx.view.assets.root != null) {
            ctx.sheetClips = new SpritesheetClipCache(ctx.view.assets.root);
        }
        loadHudAtlases(ctx);
    }

    private static void loadHudAtlases(GameplayContext ctx) {
        ctx.view.assets.textures.loadSync(PlantFoodBankHud.ATLAS_GROUP);
        ctx.view.assets.textures.loadSync(PlantFoodBankHud.ATLAS_PAGE_0);
        ctx.view.assets.textures.loadSync(PlantFoodBankHud.ATLAS_PAGE_1);
        ctx.view.assets.textures.loadSync(AdventureHudRegions.ATLAS_WORLD_MAP);
        ctx.view.assets.textures.loadSync(AdventureHudRegions.ATLAS_ALWAYS_LOADED);
        ctx.view.assets.textures.loadSync("ZENGARDENGROUP_768");
        ctx.view.assets.textures.loadSync("ATLASIMAGE_ATLAS_ZENGARDENGROUP_768_00");
        ctx.view.assets.textures.loadSync(PauseMenuOverlay.ATLAS_GROUP);
        ctx.view.assets.textures.loadSync(PauseMenuOverlay.ATLAS_PAGE);
    }

    private static void wireInput(
            GameplayContext ctx,
            Consumer<InputProcessor> setWorldInput,
            WorldClickFactory clickFactory
    ) {
        if (ctx.beghouledMode) {
            setWorldInput.accept(new BeghouledWorldInput(ctx));
            return;
        }
        InputProcessor click = clickFactory.create(
            ctx.lawnLayout, ctx.placement::onWorldClick, ctx.placement::onCellHover);
        if (ctx.couchPlayMode) {
            setWorldInput.accept(new InputMultiplexer(new CouchZombieKeyInput(ctx), click));
        } else {
            setWorldInput.accept(click);
        }
    }

    private static void showRoleToast(GameplayContext ctx) {
        if (ctx.multiplayerMode) {
            String roleLabel = ctx.multiplayerPlantSide ? "Plant" : "Zombie";
            String vs = ctx.multiplayerOpponent != null ? ctx.multiplayerOpponent : "Opponent";
            ctx.view.toast(roleLabel + " vs " + vs, false);
        } else if (ctx.couchPlayMode) {
            ctx.view.toast("Couch play: mouse plants, A–L + arrows spawn zombies.", false);
        }
    }

    private static BitmapFont resolveFont(Skin skin) {
        try {
            Label.LabelStyle style = skin.get("medium", Label.LabelStyle.class);
            if (style != null && style.font != null) {
                return style.font;
            }
        } catch (Exception ignored) {
            // fall through
        }
        return skin.get(BitmapFont.class);
    }

    @FunctionalInterface
    public interface WorldClickFactory {
        InputProcessor create(
            LawnLayout layout,
            AbstractGameplayScreen.WorldClickListener click,
            AbstractGameplayScreen.CellHoverListener hover);
    }
}
