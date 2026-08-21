package view.gui.ui;

import com.badlogic.gdx.math.Rectangle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlantChooserPanelTest {
    @Test
    void coverFitCropsWiderImageToBox() {
        Rectangle dest = new Rectangle();
        PlantChooserPanel.coverDest(100f, 100f, 200f, 100f, dest);
        assertEquals(-50f, dest.x, 0.01f);
        assertEquals(0f, dest.y, 0.01f);
        assertEquals(200f, dest.width, 0.01f);
        assertEquals(100f, dest.height, 0.01f);
    }

    @Test
    void chooserCardSkipsMissingAtlas() {
        assertNull(PlantChooserPanel.chooserCard(null));
    }
}
