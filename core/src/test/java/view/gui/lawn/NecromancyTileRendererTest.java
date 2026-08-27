package view.gui.lawn;

import model.enums.GroundType;
import model.game.map.Cell;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NecromancyTileRendererTest {

    @Test
    void onlyNecromancyGroundDraws() {
        Cell necro = new Cell(0, 0);
        necro.setGroundType(GroundType.NECROMANCY);
        Cell plain = new Cell(1, 0);
        plain.setGroundType(GroundType.NORMAL);
        assertTrue(NecromancyTileRenderer.draws(necro));
        assertFalse(NecromancyTileRenderer.draws(plain));
        assertFalse(NecromancyTileRenderer.draws(null));
    }

    @Test
    void pulseStaysInBreathingRange() {
        float lo = NecromancyTileRenderer.pulse(0f);
        float hi = NecromancyTileRenderer.pulse((float) (Math.PI / (2.2 * 2)));
        assertTrue(lo >= 0.8f && lo <= 1.05f);
        assertTrue(hi >= 0.8f && hi <= 1.05f);
    }
}
