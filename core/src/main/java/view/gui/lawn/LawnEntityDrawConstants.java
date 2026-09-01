package view.gui.lawn;

/** Shared PAM paths and part names for lawn entity drawing. */
final class LawnEntityDrawConstants {
    static final float NO_PHASE = -1f;
    static final float HIT_FLASH_SEC = 0.12f;
    static final int HIT_FLASH_CHUNK = 8;
    static final float CHEW_FLASH_COOLDOWN = 0.65f;
    static final float ARMOR_POP_FADE = 0.55f;
    static final float ARMOR_POP_HOP = 1.4f;
    static final float ARMOR_POP_GRAVITY = -9f;
    static final float ARMOR_POP_BACK_TILES = 0.2f;
    static final float HEAD_THROW_BACK_TILES = 0.2f;
    static final float HEAD_THROW_HOP_TILES = 0.45f;
    static final float POP_BOUNCE = 0.4f;

    static final String[] DEATH_PARTS_EGYPT = {
        "zombie_egypt_skull", "zombie_egypt_jaw",
        "zombie_egypt_arm_outer_lower", "zombie_egypt_hand_outer_01"};

    static final String[] DEATH_PARTS = {
        "zombie_skull", "zombie_jaw",
        "zombie_arm_outer_lower", "zombie_arms_outer_upper"};

    static final String ALLSTAR_PARTICLES = "_particles";
    static final String[] ALLSTAR_HEAD_PARTS = {
        "_particles", "particle_head", "particle_arm",
        "zombie_skull", "zombie_jaw", "allstar_head_helmet_particle",
        "zombie_arm_outer_lower", "zombie_arms_outer_upper"};

    static final String GARGANTUAR_HEAD = "Gargantuar_Head_Particle";
    static final String[] GARGANTUAR_HEAD_PARTS = {
        "Zombie_gargantuar_head", "Zombie_gargantuar_jaw",
        "Zombie_gargantuar_headBehind", "Zombie_gargantuar_head_Dress_Back"};

    static final String IMP_HEAD = "particle_head";
    static final String[] IMP_HEAD_PARTS = {
        "zombie_imp_skull", "zombie_imp_jaw", "_zombie_imp_head_top"};

    static final String[] ARCADE_PARTICLE_PARTS = {"particle_head", "particle_arm"};
    static final String[] ARCADE_HEAD_PARTS = {
        "particle_head", "particle_arm",
        "zombie_skull", "zombie_jaw",
        "zombie_arm_outer_lower", "zombie_arm_outer_upper", "zombie_arms_outer_upper",
        "zombie_hand_outer", "zombie_troglobite_hand_oute_push"};

    static final String[] HUNTER_HEAD_PARTS = {
        "particle_head", "particle_hand",
        "zombie_skull", "zombie_jaw",
        "zombie_arm_outer_lower", "zombie_arms_outer_upper"};

    static final String[] LOST_HAND_BODY_PARTS = {
        "particle_hand", "particle_arm", "particle_arm_01", "particle_arm_02",
        "zombie_arm_outer_lower", "zombie_arm_outer_upper", "zombie_arms_outer_upper",
        "zombie_hand_outer", "zombie_hand_outer_01", "zombie_hand_outer_02",
        "zombie_hand_outer_03",
        "zombie_troglobite_hand_oute_push", "zombie_troglobite_hand_outer",
        "zombie_troglobite_arm_outer_lower", "zombie_troglobite_arm_outer_upper",
        "zombie_egypt_arm_outer_lower", "zombie_egypt_arm_outer_upper",
        "zombie_egypt_arms_outer_upper", "zombie_egypt_hand_outer_01"};

    static final String[] ARM_PARTICLE_NAMES = {
        "particle_arm", "particle_arm_01", "particle_arm_02", "particle_hand"};

    static final String[] INK_BUTTER_PARTS = {
        "butter", "ink", "_butter", "_ink", "zombie_butter", "zombie_ink"};

    static final String[] HEAD_POP_HIDE = {
        "particle_arm", "particle_hand",
        "zombie_arm_outer_lower", "zombie_arms_outer_upper",
        "zombie_egypt_arm_outer_lower", "zombie_egypt_hand_outer_01",
        "zombie_jaw", "zombie_egypt_jaw"};

    static final String ARCADE_CABINET_PAM = "80S_ARCADE_CABINET";
    static final String PIANO_PAM = "PIANO";
    static final String[] PIANO_PARTICLE_PARTS = {
        "particle_jar_01", "particle_jar_02",
        "particle_key_01", "particle_key_02",
        "particle_note_01", "particle_note_02"};

    static final String JANE_ASH_PAM = "ZOMBIE_LOSTCITY_JANE_ASH";
    static final String BIG_ASH_PAM = "ZOMBIE_BIG_ASH";
    static final String GARGANTUAR_ASH_PAM = "ZOMBIE_GARGANTUAR_ASH";
    static final String IMP_ASH_PAM = "ZOMBIE_IMP_ASH";
    static final String ZOMBIE_ASH_PAM = "ZOMBIE_ASH";
    static final String CRYSTALSKULL_BEAM_PAM = "CRYSTALSKULL_BEAM";
    static final String SUN_PAM = "SUN";
    static final String SUN_BOMB_PAM = "SUN_BOMB";
    static final String PLANTFOOD_PICKUP_PAM = "PLANTFOOD_PICKUP";
    static final String COIN_GOLD_PAM = "COIN_GOLD";
    static final String COIN_SILVER_PAM = "COIN_SILVER";
    static final String COIN_DIAMOND_PAM = "COIN_DIAMOND";
    static final String FLOWER_POT_REGION =
        "IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_122X161";
    static final float FLOWER_POT_DRAW_H = 78f;
    static final float GLOW_BASE = 0.22f;
    static final float GLOW_PULSE = 0.28f;
    static final float GLOW_HZ = 0.55f;
    static final String PROSPECTOR_BLAST_PAM = "ZOMBIE_PROSPECTOR_BLAST_OFF";
    static final String[] PROSPECTOR_BLAST_CLIPS = {"animation", "animation2"};
    static final String CRYSTALSKULL_GLOW_PART = "zombie_egypt_ra_staff_whiteglow";
    static final String[] CRYSTALSKULL_SKULL_PARTS = {
        "zombie_egypt_ra_staff", CRYSTALSKULL_GLOW_PART, "zombie_skull"};
    static final String[] CRYSTALSKULL_BEAM_PARTS = {"laser_beam", "beam"};
    static final String ARCADE_HAND_PART = "zombie_troglobite_hand_oute_push";

    private LawnEntityDrawConstants() {}
}
