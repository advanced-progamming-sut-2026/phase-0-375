package view.gui.anim.zombie;

import model.enums.ArmorType;
import model.enums.Chapter;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
import model.enums.ZombieState;
import model.zombie.armor.Armor;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewspaperAnimTest {

    @Test
    void hasIntactNewspaperFalseWhenArmorDestroyed() {
        ZombieInstance zombie = newspaperZombie();
        zombie.getArmors().get(0).setCurrentHealth(0);
        assertFalse(NewspaperAnim.hasIntactNewspaper(zombie));
    }

    @Test
    void hasIntactNewspaperFalseWhenArmorRemoved() {
        ZombieInstance zombie = newspaperZombie();
        zombie.getArmors().get(0).setCurrentHealth(0);
        zombie.removeDestroyedArmor();
        assertFalse(NewspaperAnim.hasIntactNewspaper(zombie));
    }

    @Test
    void hasIntactNewspaperTrueWhilePaperLives() {
        assertTrue(NewspaperAnim.hasIntactNewspaper(newspaperZombie()));
    }

    private static ZombieInstance newspaperZombie() {
        Zombie definition = new Zombie(
                "ZombieNewspaper", 190, 0.22f, 200f, ZombieSize.NORMAL,
                Chapter.ANCIENT_EGYPT, 700, 1, List.of(ArmorType.Newspaper), null, null,
                List.of(ZombieBehaviorType.ENRAGE));
        Armor paper = new Armor(ArmorType.Newspaper, 190, false, false, false, false);
        ZombieInstance zombie = new ZombieInstance(definition, List.of(paper), null);
        zombie.setState(ZombieState.WALKING);
        return zombie;
    }
}
