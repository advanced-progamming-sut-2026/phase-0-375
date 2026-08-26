package view.gui.assets;

import model.enums.ArmorType;
import model.zombie.definition.Zombie;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** PvZ2-style almanac labels for zombie toughness / speed codes. */
public final class AlmanacZombieLabels {
    private static final Map<String, String> TOUGHNESS = Map.ofEntries(
        Map.entry("toughness1", "Average"),
        Map.entry("toughness2", "Solid"),
        Map.entry("toughness3", "Protected"),
        Map.entry("toughness4", "Dense"),
        Map.entry("toughness5", "Hardened"),
        Map.entry("toughness6", "Machined"),
        Map.entry("toughness7", "Great")
    );

    private static final Map<String, String> SPEED = Map.ofEntries(
        Map.entry("speed0", "Stiff"),
        Map.entry("speed1", "Creeper"),
        Map.entry("speed2", "Basic"),
        Map.entry("speed3", "Speedy"),
        Map.entry("speed4", "Flighty"),
        Map.entry("speed5", "Hungry")
    );

    private AlmanacZombieLabels() {}

    public static String toughnessLabel(Zombie zombie) {
        String code = almanacCode(zombie, "almanacToughness");
        if (code != null) {
            return TOUGHNESS.getOrDefault(code.toLowerCase(Locale.ROOT), prettyCode(code));
        }
        return fallbackToughness(zombie);
    }

    public static String speedLabel(Zombie zombie) {
        String code = almanacCode(zombie, "almanacSpeed");
        if (code != null) {
            return SPEED.getOrDefault(code.toLowerCase(Locale.ROOT), prettyCode(code));
        }
        return fallbackSpeed(zombie);
    }

    public static String description(Zombie zombie) {
        List<ArmorType> armor = zombie.getArmorTypes();
        if (armor != null) {
            for (ArmorType type : armor) {
                if (type == ArmorType.Cone) {
                    return "His traffic cone headpiece makes him twice as tough as normal zombies.";
                }
                if (type == ArmorType.Bucket) {
                    return "His bucket greatly increases his damage resistance.";
                }
            }
        }
        if (zombie.getName() != null && zombie.getName().toLowerCase(Locale.ROOT).contains("gargantuar")) {
            return "Massive and extremely tough. Watch for the thrown Imp.";
        }
        return "A shambling menace hungry for brains.";
    }

    public static String flavor(Zombie zombie) {
        String name = zombie.getName();
        if (name == null || name.isBlank()) {
            return "";
        }
        if (name.toLowerCase(Locale.ROOT).contains("cone")) {
            return "After a wild night, Conehead Zombie woke up holding a mysterious receipt "
                + "for a cone and industrial strength adhesive.";
        }
        return "";
    }

    private static String almanacCode(Zombie zombie, String key) {
        Object raw = zombie.getBehaviorProp(key);
        return raw != null ? raw.toString() : null;
    }

    private static String fallbackToughness(Zombie zombie) {
        int hp = zombie.getBaseHP();
        if (hp >= 600) {
            return "Great";
        }
        if (hp >= 350) {
            return "Hardened";
        }
        if (hp >= 220) {
            return "Protected";
        }
        if (hp >= 150) {
            return "Solid";
        }
        return "Average";
    }

    private static String fallbackSpeed(Zombie zombie) {
        float speed = zombie.getSpeed();
        if (speed <= 0.01f) {
            return "Stiff";
        }
        if (speed < 0.12f) {
            return "Creeper";
        }
        if (speed < 0.2f) {
            return "Basic";
        }
        if (speed < 0.28f) {
            return "Speedy";
        }
        return "Flighty";
    }

    private static String prettyCode(String code) {
        if (code == null || code.isBlank()) {
            return "—";
        }
        return code.substring(0, 1).toUpperCase(Locale.ROOT) + code.substring(1);
    }
}
