package view.tui;

import model.enums.GroundType;
import model.enums.PlacableLayer;
import model.game.core.GameModel;
import model.game.map.Cell;
import model.game.map.GameMap;
import model.item.Grave;
import model.item.Sun;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the live game map as colored terminal lines.
 */
final class MapView {

    private MapView() {}

    private static final AttributedStyle DIM = AttributedStyle.DEFAULT.faint();
    private static final AttributedStyle SUN_STYLE =
            AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);

    static List<AttributedString> render(GameModel model) {
        GameMap map = model.getMap();
        List<AttributedString> lines = new ArrayList<>();

        // Column indices.
        AttributedStringBuilder header = new AttributedStringBuilder();
        header.append("     ", DIM);
        for (int c = 0; c < map.getCols(); c++) {
            header.append(String.format("%-2d", c), DIM);
        }
        lines.add(header.toAttributedString());

        // Grid rows.
        for (int r = 0; r < map.getRows(); r++) {
            AttributedStringBuilder row = new AttributedStringBuilder();
            row.append(String.format("%3d  ", r), DIM);
            for (int c = 0; c < map.getCols(); c++) {
                char ch = cellChar(model, map, c, r);
                row.append(ch + " ", styleFor(ch));
            }
            lines.add(row.toAttributedString());
        }

        // Sun tokens on the ground.
        if (!model.getActiveSuns().isEmpty()) {
            AttributedStringBuilder suns = new AttributedStringBuilder();
            suns.append("Sun tokens: ", SUN_STYLE);
            for (Sun s : model.getActiveSuns()) {
                suns.append("(" + s.getX() + "," + s.getY() + ")=" + s.getValue() + "  ", SUN_STYLE);
            }
            lines.add(suns.toAttributedString());
        }

        lines.add(new AttributedString(
                "P plant  O overlay  B both  G ground  Z zombie  X zombie-on-plant"
                        + "  T grave  ~ water  _ tide  ^v slide  N necro  * ice  . empty",
                DIM));
        return lines;
    }

    /** Same layering rules as the controller's showMap(). */
    private static char cellChar(GameModel model, GameMap map, int c, int r) {
        Cell cell = map.getCell(c, r);
        if (cell == null) return '.';

        char ch = terrainChar(cell.getGroundType());

        // Graves sit on the field and hide the terrain under them.
        for (PlacableLayer layer : PlacableLayer.values()) {
            if (cell.getPlaceable(layer) instanceof Grave) {
                ch = 'T';
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
            case '~': return AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE);
            case '_': return AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN);
            case '^':
            case 'v': return AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
            case 'N': return AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA);
            case '*': return AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN).bold();
            default: return DIM;
        }
    }
}
