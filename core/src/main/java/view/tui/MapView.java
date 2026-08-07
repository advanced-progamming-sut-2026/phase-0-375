package view.tui;

import model.enums.GroundType;
import model.enums.PlacableLayer;
import model.game.core.GameModel;
import model.game.map.Cell;
import model.game.map.GameMap;
import model.game.level.Level;
import model.game.level.LevelConfig;
import model.game.level.minigame.bowling.WallnutBowlingLevel;
import model.game.level.minigame.izombie.IZombieLevel;
import model.game.level.minigame.vasebreaker.Vase;
import model.game.level.minigame.vasebreaker.VaseBreakerLevel;
import model.game.level.minigame.beghouled.BeghouledLevel;
import model.game.level.special.ConveyorBeltLevel;
import model.item.Grave;
import model.item.Sun;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders the live game map as colored terminal lines.
 */
final class MapView {

    private MapView() {}

    private static final AttributedStyle DIM = AttributedStyle.DEFAULT.faint();
    private static final AttributedStyle SUN_STYLE =
            AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);

    /**
     * Sensible emoji for each Beghouled board plant and its upgraded form.
     * Keyed by the plant definition name, lower-cased.
     */
    private static final Map<String, String> BEGHOULED_PLANT_EMOJI = new HashMap<>();
    static {
        BEGHOULED_PLANT_EMOJI.put("peashooter", "\uD83D\uDFE2");        // 🟢 single pea
        BEGHOULED_PLANT_EMOJI.put("repeater", "\uD83D\uDD3B");          // 🔻 double-shot upgrade
        BEGHOULED_PLANT_EMOJI.put("mega gatling pea", "\uD83D\uDD25");  // 🔥 quad-shot heavy upgrade
        BEGHOULED_PLANT_EMOJI.put("snow pea", "\u2744\uFE0F");          // ❄️ frozen pea
        BEGHOULED_PLANT_EMOJI.put("wall-nut", "\uD83E\uDD5C");          // 🥜 defensive nut
        BEGHOULED_PLANT_EMOJI.put("tall-nut", "\uD83D\uDDFC");          // 🗼 taller defensive nut
        BEGHOULED_PLANT_EMOJI.put("puff-shroom", "\uD83C\uDF44");       // 🍄 short-range mushroom
        BEGHOULED_PLANT_EMOJI.put("fume-shroom", "\uD83D\uDCA8");       // 💨 gas-cloud upgrade
        BEGHOULED_PLANT_EMOJI.put("cabbage-pult", "\uD83E\uDD6C");      // 🥬 lobbed cabbage
        BEGHOULED_PLANT_EMOJI.put("melon-pult", "\uD83C\uDF49");        // 🍉 lobbed melon upgrade
        BEGHOULED_PLANT_EMOJI.put("winter melon", "\u26C4");            // ⛄ frozen melon upgrade
        BEGHOULED_PLANT_EMOJI.put("bonk choy", "\uD83E\uDD4A");         // 🥊 melee puncher
    }

    /** Fallback emoji for a Beghouled plant with no specific mapping above. */
    private static final String BEGHOULED_DEFAULT_EMOJI = "\uD83C\uDF31"; // 🌱

    static List<AttributedString> render(GameModel model) {
        GameMap map = model.getMap();
        List<AttributedString> lines = new ArrayList<>();
        Level level = model.getCurrentLevel();
        LevelConfig config = level != null ? level.getConfig() : null;
        int tideLimit = config != null ? config.getTideLimitColumn() : -1;

        appendHeaderAndGrid(lines, model, map, level, tideLimit);
        appendInfoLines(lines, model, level, tideLimit);

        String legend = "P plant  O overlay  B both  G ground  Z zombie  X zombie-on-plant"
                 + "  T grave  $ sun-grave  + pf-grave  V vase  ~ water  _ tide  ^v slide  N necro  * ice  . empty";
        if (level instanceof BeghouledLevel) {
            legend += "  (plants shown as emoji: 🟢 Peashooter 🔻 Repeater 🔥 Mega Gatling Pea "
                    + "❄️ Snow Pea 🥜 Wall-nut 🗼 Tall-nut 🍄 Puff-shroom 💨 Fume-shroom "
                    + "🥬 Cabbage-pult 🍉 Melon-pult ⛄ Winter Melon 🥊 Bonk Choy)";
        }
        lines.add(new AttributedString(legend, DIM));
        return lines;
    }

    /** Builds the column-index header row and the colored cell grid beneath it. */
    private static void appendHeaderAndGrid(
            List<AttributedString> lines, GameModel model, GameMap map, Level level, int tideLimit) {
        // Column indices with optional tide limit markers.
        AttributedStringBuilder header = new AttributedStringBuilder();
        header.append("     ", DIM);
        for (int c = 0; c < map.getCols(); c++) {
            if (tideLimit > 0 && c >= map.getCols() - tideLimit) {
                header.append("| ", AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE));
            } else {
                header.append(String.format("%-2d", c), DIM);
            }
        }
        lines.add(header.toAttributedString());

        // Grid rows.
        for (int r = 0; r < map.getRows(); r++) {
            AttributedStringBuilder row = new AttributedStringBuilder();
            row.append(String.format("%3d  ", r), DIM);
            for (int c = 0; c < map.getCols(); c++) {
                // I, Zombie red line: show | at the column just before the red line
                if (level instanceof IZombieLevel iZombie && c == iZombie.redLineColumn()) {
                    row.append("| ", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold());
                    continue;
                }
                char ch = cellChar(model, map, c, r);
                // Vase Breaker: show V for unbroken vases
                if (level instanceof VaseBreakerLevel vbLevel && ch == '.') {
                    Vase vase = vbLevel.vaseAt(c, r);
                    if (vase != null) {
                        ch = switch (vase.getContentType()) {
                            case ZOMBIE -> 'Y';
                            case SEED_PACKET -> 'S';
                            case GIANT_VASE -> 'W';
                            default -> 'V'; // EMPTY
                        };
                    }
                }
                // Beghouled: show a plant-specific emoji instead of the generic P/O/B/G letter.
                if (level instanceof BeghouledLevel && (ch == 'P' || ch == 'O' || ch == 'B' || ch == 'G')) {
                    String emoji = beghouledEmoji(map, c, r);
                    if (emoji != null) {
                        row.append(emoji, styleFor(ch));
                        continue;
                    }
                }
                row.append(ch + " ", styleFor(ch));
            }
            // I, Zombie: if red line is at the last column, no separator drawn
            lines.add(row.toAttributedString());
        }
    }

    /** Builds the belt / red-line / tide-limit / sun-token annotation lines below the grid. */
    private static void appendInfoLines(
            List<AttributedString> lines, GameModel model, Level level, int tideLimit) {
        List<String> beltPlants = model.getSelectedPlants();
        if ((level instanceof ConveyorBeltLevel || level instanceof WallnutBowlingLevel)
                && beltPlants != null && !beltPlants.isEmpty()) {
            AttributedStringBuilder belt = new AttributedStringBuilder();
            belt.append("Belt: ", AttributedStyle.DEFAULT.bold());
            for (int i = 0; i < beltPlants.size(); i++) {
                if (i > 0) belt.append(", ");
                // Highlight the first (front-of-belt) item
                if (i == 0) {
                    belt.append("[" + beltPlants.get(i) + "]",
                            AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN).bold());
                } else {
                    belt.append(beltPlants.get(i));
                }
            }
            lines.add(belt.toAttributedString());
        }

        if (level instanceof IZombieLevel iZombie) {
            AttributedStringBuilder izInfo = new AttributedStringBuilder();
            izInfo.append("Red line at column ", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold());
            izInfo.append(String.valueOf(iZombie.redLineColumn()),
                    AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold());
            izInfo.append(" | zombies placed right of ", DIM);
            izInfo.append("|", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold());
            lines.add(izInfo.toAttributedString());
        }

        if (tideLimit > 0) {
            AttributedStringBuilder tide = new AttributedStringBuilder();
            tide.append("Tide limit: rightmost ",
                    AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE));
            tide.append(String.valueOf(tideLimit),
                    AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE).bold());
            tide.append(" column(s) may flood (marked with |)",
                    AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE));
            lines.add(tide.toAttributedString());
        }

        if (!model.getActiveSuns().isEmpty()) {
            AttributedStringBuilder suns = new AttributedStringBuilder();
            suns.append("Sun tokens: ", SUN_STYLE);
            for (Sun s : model.getActiveSuns()) {
                suns.append("(" + s.getX() + "," + s.getY() + ")=" + s.getValue() + "  ", SUN_STYLE);
            }
            lines.add(suns.toAttributedString());
        }
    }

    /** Same layering rules as the controller's showMap(). */
    private static char cellChar(GameModel model, GameMap map, int c, int r) {
        Cell cell = map.getCell(c, r);
        if (cell == null) return '.';

        char ch = terrainChar(cell.getGroundType());

        // Graves sit on the field and hide the terrain under them.
        for (PlacableLayer layer : PlacableLayer.values()) {
            if (cell.getPlaceable(layer) instanceof Grave grave) {
                ch = switch (grave.getType()) {
                    case SUN -> '$';
                    case PLANT_FOOD -> '+';
                    default -> 'T';
                };
                break;
            }
        }

        boolean hasGround = cell.getPlaceable(PlacableLayer.GROUND) instanceof PlantInstance;
        boolean hasMain = cell.getPlaceable(PlacableLayer.MAIN) instanceof PlantInstance;
        boolean hasOverlay = cell.getPlaceable(PlacableLayer.OVERLAY) instanceof PlantInstance;
        if (hasMain && hasOverlay) ch = 'B';
        else if (hasMain) ch = 'P';
        else if (hasOverlay) ch = 'O';
        else if (hasGround) ch = 'G';

        for (ZombieInstance z : model.getZombies()) {
            var gp = z.getGridPosition();
            if (gp != null && gp.getX() == c && gp.getY() == r) {
                ch = (ch == 'P' || ch == 'O' || ch == 'B') ? 'X' : 'Z';
                break;
            }
        }
        return ch;
    }

    /**
     * Resolves the emoji for the plant occupying (c, r) on a Beghouled
     * board, checked in MAIN then GROUND layer order. Returns null if the
     * cell holds no plant (so the caller falls back to the plain letter).
     */
    private static String beghouledEmoji(GameMap map, int c, int r) {
        Cell cell = map.getCell(c, r);
        if (cell == null) return null;
        PlantInstance plant = null;
        if (cell.getPlaceable(PlacableLayer.MAIN) instanceof PlantInstance main) {
            plant = main;
        } else if (cell.getPlaceable(PlacableLayer.GROUND) instanceof PlantInstance ground) {
            plant = ground;
        }
        if (plant == null || plant.getDefinition() == null || plant.getDefinition().getName() == null) {
            return null;
        }
        String name = plant.getDefinition().getName().toLowerCase();
        return BEGHOULED_PLANT_EMOJI.getOrDefault(name, BEGHOULED_DEFAULT_EMOJI) + " ";
    }

    private static char terrainChar(GroundType ground) {
        if (ground == null) return '.';
        switch (ground) {
            case WATER: return '~';
            case LOW_TIDE: return '_';
            case SLIDE_UP: return '^';
            case SLIDE_DOWN: return 'v';
            case NECROMANCY: return 'N';
            case ICE: return '*';
            default: return '.';
        }
    }

    private static AttributedStyle styleFor(char ch) {
        switch (ch) {
            case 'P':
            case 'G': return AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
            case 'O':
            case 'B': return AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN).bold();
            case 'Z': return AttributedStyle.DEFAULT.foreground(AttributedStyle.RED);
            case 'X': return AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold();
            case 'T': return AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE).bold();
            case '$': return AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW).bold();
            case '+': return AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA).bold();
            case '~': return AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE);
            case '_': return AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN);
            case '^':
            case 'v': return AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
            case 'N': return AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA);
            case '*': return AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN).bold();
            case 'V': return AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW).bold();
            case 'Y': return AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold();
            case 'S': return AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN).bold();
            case 'W': return AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA).bold();
            default: return DIM;
        }
    }
}
