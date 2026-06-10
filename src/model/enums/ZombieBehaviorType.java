package model.enums;

public enum ZombieBehaviorType {
    SHOOT,          // fires projectiles at plants (Hunter, Tomb Raiser)
    STEAL_SUN,      // steals sun from the player's reserve (Ra Zombie)
    JUGGLE,         // catches and reflects projectiles (Juggler)
    SWIM,           // moves through water lanes (Snorkel, Fast Swimmer)
    FLY,            // flies over plants/obstacles (Dodo)
    SUMMON,         // spawns grid items or units (Tomb Raiser, Weasel Hoarder)
    BUFF,           // enhances nearby zombies (Dark King)
    TRANSFORM,      // transforms plants into other things (Wizard → sheep)
    FISH,           // hooks and pulls plants (Fisherman)
    THROW_IMP,      // throws an Imp at low health (Gargantuar)
    SMASH,          // instantly destroys plants on contact (Gargantuar)
    DISARM,         // attracts/steals metallic items (Magnet-shroom reverse)
    HYPNOTIZE,      // resists or interacts with hypno effects
    DISGUISE        // appearance-based behavior (Camel segments)
}
