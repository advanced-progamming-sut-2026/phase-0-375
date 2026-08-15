package model.zombie.behavior;

import model.enums.ArmorType;
import model.enums.Chapter;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
import model.enums.ZombieState;
import model.zombie.armor.Armor;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewspaperEnrageTest {

    private static final float TICK = 0.1f;
    private static final float SPEED = 0.22f;
    private static final float ENRAGED_SCALE = 4f;

    @Test
    void paperBreakPlaysDefeatThenEnrages() {
        ZombieInstance zombie = newspaper();
        EnrageBehavior enrage = (EnrageBehavior) zombie.getBehavior(ZombieBehaviorType.ENRAGE);
        BehaviorContext context = stubContext();

        enrage.execute(zombie, context, TICK);
        assertFalse(enrage.isEnraged());
        assertFalse(enrage.isDefeating());
        assertEquals(SPEED, zombie.getCurrentSpeed(), 1e-4f);
        assertEquals(1f, enrage.getEatDamageScale(), 1e-4f);

        zombie.getArmors().get(0).setCurrentHealth(0);
        enrage.execute(zombie, context, TICK);
        assertTrue(enrage.isDefeating());
        assertFalse(enrage.isEnraged());
        assertEquals(ZombieState.SPECIAL_ACTION, zombie.getState());
        assertEquals(SPEED, zombie.getCurrentSpeed(), 1e-4f);
        assertEquals(1f, enrage.getEatDamageScale(), 1e-4f);

        runFor(enrage, zombie, context, EnrageBehavior.NEWSPAPER_DEFEAT_DURATION);
        assertFalse(enrage.isDefeating());
        assertTrue(enrage.isEnraged());
        assertEquals(ZombieState.WALKING, zombie.getState());
        assertEquals(SPEED * ENRAGED_SCALE, zombie.getCurrentSpeed(), 1e-4f);
        assertEquals(ENRAGED_SCALE, enrage.getEatDamageScale(), 1e-4f);
    }

    @Test
    void noNewspaperNeverGasp() {
        Zombie definition = new Zombie(
                "ZombieDefault", 190, SPEED, 200f, ZombieSize.NORMAL,
                Chapter.ANCIENT_EGYPT, 100, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.ENRAGE));
        ZombieInstance basic = new ZombieInstance(definition);
        basic.setState(ZombieState.WALKING);
        EnrageBehavior enrage = (EnrageBehavior) basic.getBehavior(ZombieBehaviorType.ENRAGE);
        enrage.execute(basic, stubContext(), TICK);
        assertFalse(enrage.isDefeating());
        assertTrue(enrage.isEnraged());
        assertEquals(SPEED * EnrageBehavior.DEFAULT_ENRAGED_SPEED_SCALE,
                basic.getCurrentSpeed(), 1e-4f);
    }

    private static void runFor(EnrageBehavior enrage, ZombieInstance zombie,
                               BehaviorContext context, float seconds) {
        for (float t = 0f; t < seconds; t += TICK) {
            enrage.execute(zombie, context, TICK);
        }
    }

    private static ZombieInstance newspaper() {
        Zombie definition = new Zombie(
                "ZombieNewspaper", 190, SPEED, 200f, ZombieSize.NORMAL,
                Chapter.ANCIENT_EGYPT, 700, 1, List.of(ArmorType.Newspaper), null, null,
                List.of(ZombieBehaviorType.ENRAGE));
        definition.putBehaviorProp("EnragedSpeedScale", ENRAGED_SCALE);
        definition.putBehaviorProp("EnragedDamageScale", ENRAGED_SCALE);
        Armor paper = new Armor(ArmorType.Newspaper, 190, false, false, false, false);
        paper.setDamageLayers(List.of(
                "_zombie_newspaper", "_zombie_newspaper_dmg1", "_zombie_newspaper_dmg2"));
        paper.setLayerThresholds(List.of(0.666f, 0.333f));
        ZombieInstance zombie = new ZombieInstance(definition, List.of(paper), null);
        zombie.setState(ZombieState.WALKING);
        return zombie;
    }

    private static BehaviorContext stubContext() {
        return (BehaviorContext) Proxy.newProxyInstance(
                BehaviorContext.class.getClassLoader(),
                new Class<?>[]{BehaviorContext.class},
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
