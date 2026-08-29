package view.gui.lawn;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import model.enums.LevelType;
import model.game.core.GameModel;
import model.game.level.Level;
import model.game.level.LevelConfig;
import model.game.level.ProtectedPlantTile;
import model.game.level.special.SaveOurSeedsLevel;
import model.game.map.Point;
import pvz.libpvz.textures.TextureBank;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws {@value #PROTECT_TILE_IMAGE_ID} over the tiles where protected seeds/plants
 * are placed in "Protect Your Seeds" / Save Our Seeds levels.
 */
public final class ProtectTileRenderer {
    public static final String ATLAS_GROUP = "ProtectThePlantChallengeModule_768";
    public static final String ATLAS_PAGE = "ATLASIMAGE_ATLAS_PROTECTTHEPLANTCHALLENGEMODULE_768_00";
    public static final String PROTECT_TILE_IMAGE_ID = "IMAGE_BACKGROUNDS_PROTECT_TILE_PROTECT_TILE_112X125";

    private final TextureBank textures;
    private boolean atlasRequested;

    public ProtectTileRenderer(TextureBank textures) {
        this.textures = textures;
    }

    public void ensureLoaded() {
        if (textures != null && !atlasRequested) {
            textures.loadSync(ATLAS_GROUP);
            textures.loadSync(ATLAS_PAGE);
            atlasRequested = true;
        }
    }

    public static boolean isProtectLevel(Level level) {
        if (level == null) {
            return false;
        }
        if (level instanceof SaveOurSeedsLevel) {
            return true;
        }
        LevelConfig config = level.getConfig();
        if (config == null) {
            return false;
        }
        if (config.getLevelType() == LevelType.SAVE_OUR_SEEDS) {
            return true;
        }
        return (config.getProtectedPlants() != null && !config.getProtectedPlants().isEmpty())
                || (config.getProtectedPlantPositions() != null && !config.getProtectedPlantPositions().isEmpty());
    }

    public static List<Point> getProtectedTilePositions(Level level) {
        if (level == null || level.getConfig() == null) {
            return List.of();
        }
        LevelConfig config = level.getConfig();
        List<ProtectedPlantTile> protectedPlants = config.getProtectedPlants();
        if (protectedPlants != null && !protectedPlants.isEmpty()) {
            List<Point> points = new ArrayList<>(protectedPlants.size());
            for (ProtectedPlantTile tile : protectedPlants) {
                if (tile != null && tile.getPosition() != null) {
                    points.add(tile.getPosition());
                }
            }
            return points;
        }
        List<Point> positions = config.getProtectedPlantPositions();
        if (positions != null && !positions.isEmpty()) {
            return positions;
        }
        return List.of();
    }

    public void draw(Batch batch, LawnLayout layout, GameModel model) {
        if (batch == null || layout == null || textures == null || model == null) {
            return;
        }
        Level level = model.getCurrentLevel();
        if (!isProtectLevel(level)) {
            return;
        }
        List<Point> positions = getProtectedTilePositions(level);
        if (positions.isEmpty()) {
            return;
        }
        ensureLoaded();
        TextureRegion region = textures.region(PROTECT_TILE_IMAGE_ID);
        if (region == null) {
            return;
        }
        float w = region.getRegionWidth();
        float h = region.getRegionHeight();
        if (w <= 0f) {
            w = layout.cellWidth();
        }
        if (h <= 0f) {
            h = layout.cellHeight();
        }
        for (Point pos : positions) {
            if (pos == null) {
                continue;
            }
            int col = pos.getX();
            int row = pos.getY();
            if (col < 0 || col >= layout.cols() || row < 0 || row >= layout.rows()) {
                continue;
            }
            float[] xy = layout.centerOf(row, col);
            float drawX = xy[0] - w * 0.5f;
            float drawY = xy[1] - h * 0.5f;
            batch.draw(region, drawX, drawY, w, h);
        }
    }
}
