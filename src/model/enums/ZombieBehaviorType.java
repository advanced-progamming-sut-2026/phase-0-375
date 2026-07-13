package model.enums;

public enum ZombieBehaviorType {
    SHOOT,          // fires projectiles at plants (Hunter, Tomb Raiser)
    STEAL_SUN,      // steals sun from the player's reserve (Ra Zombie)
    JUGGLE,         // catches and reflects direct projectiles (Jester/Juggler)
    DEFLECT_LOBBER, // blocks/deflects LOBBED projectiles (Parasol Zombie)
    SWIM,           // moves through water lanes (Snorkel, Fast Swimmer)
    FLY,            // flies over plants/obstacles (Dodo)
    SUMMON,         // spawns grid items or units (Tomb Raiser, Weasel Hoarder)
    BUFF,           // enhances nearby zombies (Dark King)
    TRANSFORM,      // transforms plants into other things (Wizard -> sheep)
    FISH,           // hooks and pulls plants (Fisherman)
    THROW_IMP,      // throws an Imp at low health (Gargantuar)
    SMASH,          // instantly destroys plants on contact (Gargantuar)
    JUMP,           // Jumps over game entities (Prospector)
    PUSH,           // Push items on the ground (Arcade, Pianist)
    ENRAGE,         // Speeds up and eats faster when its armor is destroyed
    PIANO_SWAP,     // Periodically swaps rows of nearby zombies while playing
    BARREL_ROLLER,  // Spawns 2 imps when its barrel pushable is destroyed
}
