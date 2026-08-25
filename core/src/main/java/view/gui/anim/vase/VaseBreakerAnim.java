package view.gui.anim.vase;

import model.enums.VaseContent;
import model.game.level.minigame.vasebreaker.Vase;

public final class VaseBreakerAnim {
    public static final String BROWN =
            "768/FULL/VASEBREAKER/VASE_BROWN/VASE_BROWN.PAM";
    public static final String GREEN =
            "768/FULL/VASEBREAKER/VASE_GREEN/VASE_GREEN.PAM";
    public static final String GARGANTUAR_VASE =
            "768/FULL/VASEBREAKER/VASE_GARGANTUAR/VASE_GARGANTUAR.PAM";
    public static final String GARGANTUAR_ZOMBIE =
            "768/FULL/ZOMBIE/VASE_GARGANTUAR/VASE_GARGANTUAR.PAM";

    public static final String CLIP_DROP = "drop";
    public static final String CLIP_IDLE = "idle";
    public static final String CLIP_BREAK = "break";

    /** Seconds of the drop intro before looping idle. */
    public static final float DROP_SECONDS = 0.8f;

    private VaseBreakerAnim() {}

    public static String pamPath(VaseContent content) {
        if (content == null) {
            return BROWN;
        }
        return switch (content) {
            case SEED_PACKET -> GREEN;
            case GIANT_VASE -> GARGANTUAR_VASE;
            case EMPTY, ZOMBIE -> BROWN;
        };
    }

    public static String pamPath(Vase vase) {
        return vase == null ? BROWN : pamPath(vase.getContentType());
    }

    public static String[] allVasePams() {
        return new String[] {BROWN, GREEN, GARGANTUAR_VASE};
    }
}
