package model.zombie.armor;

import model.enums.ArmorType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArmorDamageLayerTest {

    @Test
    void bucketStagesFollowRemainingHpFractions() {
        Armor armor = new Armor(ArmorType.Bucket, 1100, true, true, true, false);
        armor.setDamageLayers(List.of(
                "zombie_armor_bucket_norm",
                "zombie_armor_bucket_damage_01",
                "zombie_armor_bucket_damage_02"));
        armor.setLayerThresholds(List.of(0.666f, 0.333f));

        armor.setCurrentHealth(1100);
        assertEquals(0, armor.getCurrentDamageLayer());
        armor.setCurrentHealth(733); // 733/1100 > 0.666
        assertEquals(0, armor.getCurrentDamageLayer());
        armor.setCurrentHealth(732);
        assertEquals(1, armor.getCurrentDamageLayer());
        armor.setCurrentHealth(367);
        assertEquals(1, armor.getCurrentDamageLayer());
        armor.setCurrentHealth(366);
        assertEquals(2, armor.getCurrentDamageLayer());
        armor.setCurrentHealth(0);
        assertEquals(2, armor.getCurrentDamageLayer());
        assertEquals("zombie_armor_bucket_damage_02", armor.popLayer());
    }
}
