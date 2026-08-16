package view.gui.assets;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Definition-name → PAM catalog name aliases for plants.
 */
public final class PlantPamAliases {
    private PlantPamAliases() {}

    /** Unmodifiable map of game definition name → animations.json PAM name. */
    public static Map<String, String> all() {
        Map<String, String> m = new HashMap<>();
        m.put("Twin Sunflower", "SUNFLOWER_TWIN");
        m.put("Sun-shroom", "SUNSHROOM");
        m.put("Primal Sunflower", "PRIMAL_SUNFLOWER");
        m.put("Wall-nut", "WALLNUT");
        m.put("Fume-shroom", "FUMESHROOM");
        m.put("Ice-shroom", "ICESHROOM");
        m.put("Hypno-shroom", "HYPNOSHROOM");
        m.put("Magnet-shroom", "MAGNETSHROOM");
        m.put("Grave Buster", "GRAVEBUSTER");
        m.put("Potato Mine", "POTATOMINE");
        m.put("Kernel-pult", "KERNALPULT");
        m.put("Cabbage-pult", "CABBAGEPULT");
        m.put("Melon-pult", "MELONPULT");
        m.put("Pepper-pult", "PEPPERPULT");
        m.put("Winter Melon", "WINTERMELON");
        m.put("Bonk Choy", "BONKCHOY");
        m.put("Snapdragon", "SNAPDRAGON");
        m.put("Cold Snapdragon", "COLD_SNAPDRAGON");
        m.put("Iceberg Lettuce", "ICEBERGLETTUCE");
        m.put("Cat-tail", "CATTAIL");
        m.put("Lily Pad", "LILYPAD");
        m.put("Gold Bloom", "GOLDBLOOM");
        m.put("Hot Potato", "HOTPOTATO");
        m.put("Pierce-mint", "PEPPERMINT");
        m.put("Rotobaga", "ROTORUTABAGA");
        return Collections.unmodifiableMap(m);
    }
}
