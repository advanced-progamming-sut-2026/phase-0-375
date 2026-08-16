package view.gui.assets;

import model.enums.BowlingBulbType;
import model.enums.PlantTags;
import model.plant.definition.Plant;
import model.projectile.BowlingBulb;
import model.projectile.FumeCloud;
import model.projectile.Projectile;

import java.util.Map;

/**
 * Shooter projectile PAM paths. Effect PAMs are not in {@link PamCatalog}
 * (it indexes plant/zombie packs only), so this table is the source of truth.
 */
public final class ProjectilePamPaths {
    private ProjectilePamPaths() {}

    public static final String PEA =
            "768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM";
    public static final String SNOW_PEA =
            "768/INITIAL/EFFECTS/T_SNOW_PEA/T_SNOW_PEA.PAM";
    public static final String FIRE_PEA =
            "768/INITIAL/EFFECTS/T_FIRE_PEA/T_FIRE_PEA.PAM";
    public static final String GOO_PEA =
            "768/INITIAL/EFFECTS/GOOPEASHOOTER_PROJECTILES/GOOPEASHOOTER_PROJECTILES.PAM";
    public static final String MEGA_GATLING =
            "768/INITIAL/EFFECTS/MEGAGATLING_PROJECTILE/MEGAGATLING_PROJECTILE.PAM";
    public static final String STARFRUIT =
            "768/INITIAL/EFFECTS/T_STARFRUIT_PROJECTILE/T_STARFRUIT_PROJECTILE.PAM";
    public static final String PUFF_SHROOM =
            "768/INITIAL/EFFECTS/T_PUFFSHROOM_PROJECTILE/T_PUFFSHROOM_PROJECTILE.PAM";
    public static final String CACTUS =
            "768/INITIAL/EFFECTS/T_CACTUS_PROJECTILE/T_CACTUS_PROJECTILE.PAM";
    public static final String FUME_SHROOM =
            "768/INITIAL/EFFECTS/FUMESHROOM_BUBBLES/FUMESHROOM_BUBBLES.PAM";
    public static final String ROTOBAGA =
            "768/FULL/EFFECTS/ROTORUTABAGA_PROJECTILE1/ROTORUTABAGA_PROJECTILE1.PAM";
    public static final String CITRON =
            "768/FULL/EFFECTS/CITRON_CITRUS_ORB/CITRON_CITRUS_ORB.PAM";
    public static final String SEA_SHROOM =
            "768/FULL/EFFECTS/SEASHROOM_PROJECTILE/SEASHROOM_PROJECTILE.PAM";
    public static final String BOWLING_CYAN =
            "768/FULL/EFFECTS/BOWLINGBULB_PROJECTILE1/BOWLINGBULB_PROJECTILE1.PAM";
    public static final String BOWLING_BLUE =
            "768/FULL/EFFECTS/BOWLINGBULB_PROJECTILE2/BOWLINGBULB_PROJECTILE2.PAM";
    public static final String BOWLING_ORANGE =
            "768/FULL/EFFECTS/BOWLINGBULB_PROJECTILE3/BOWLINGBULB_PROJECTILE3.PAM";

    /** Preferred clip names for effect PAMs (resolved against the loaded file). */
    public static final String[] CLIP_PREFERENCES = {
            "animation", "idle", "loop", "projectile"
    };

    private static final Map<String, String> BY_PLANT = Map.ofEntries(
            Map.entry("Peashooter", PEA),
            Map.entry("Repeater", PEA),
            Map.entry("Threepeater", PEA),
            Map.entry("Pea Pod", PEA),
            Map.entry("Split Pea", PEA),
            Map.entry("Snow Pea", SNOW_PEA),
            Map.entry("Rotobaga", ROTOBAGA),
            Map.entry("Citron", CITRON),
            Map.entry("Fire Peashooter", FIRE_PEA),
            Map.entry("Starfruit", STARFRUIT),
            Map.entry("Goo Peashooter", GOO_PEA),
            Map.entry("Mega Gatling Pea", MEGA_GATLING),
            Map.entry("Sea-shroom", SEA_SHROOM),
            Map.entry("Puff-shroom", PUFF_SHROOM),
            Map.entry("Cactus", CACTUS),
            Map.entry("Fume-shroom", FUME_SHROOM)
    );

    /**
     * @return PAM path for this projectile, or {@code null} if this renderer
     *         does not own its art.
     */
    public static String pathFor(Projectile projectile) {
        if (projectile == null) {
            return null;
        }
        if (projectile instanceof BowlingBulb bulb) {
            return pathForBulb(bulb.getType());
        }
        if (projectile instanceof FumeCloud) {
            return FUME_SHROOM;
        }
        Plant source = projectile.getSourcePlant();
        String name = source != null ? source.getName() : null;
        if (projectile.isFire() && isPeaFamily(source, name)) {
            return FIRE_PEA;
        }
        if (name != null) {
            String mapped = BY_PLANT.get(name);
            if (mapped != null) {
                return mapped;
            }
        }
        return null;
    }

    public static String pathForBulb(BowlingBulbType type) {
        if (type == null) {
            return BOWLING_CYAN;
        }
        return switch (type) {
            case CYAN -> BOWLING_CYAN;
            case BLUE -> BOWLING_BLUE;
            case ORANGE -> BOWLING_ORANGE;
        };
    }

    /**
     * Peas (including snow / goo / gatling) that Torchwood should restyle as
     * fire peas. Citron, Rotobaga, Starfruit, and shrooms keep their own PAM.
     */
    private static boolean isPeaFamily(Plant source, String name) {
        if (source != null && source.hasTag(PlantTags.PEA)) {
            return true;
        }
        if (name == null) {
            return false;
        }
        return "Goo Peashooter".equals(name) || "Mega Gatling Pea".equals(name);
    }
}
