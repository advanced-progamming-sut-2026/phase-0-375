package view.gui.ui;

/**
 * Keyboard letters bound to I, Zombie roster cards in couch play (A, S, D, …).
 */
public final class ZombieHotkeys {
    static final String KEYS = "ASDFGHJKL";

    private ZombieHotkeys() {}

    public static char letterAt(int index) {
        if (index < 0 || index >= KEYS.length()) {
            return 0;
        }
        return KEYS.charAt(index);
    }

    public static int indexOf(char letter) {
        return KEYS.indexOf(Character.toUpperCase(letter));
    }

    public static int size() {
        return KEYS.length();
    }
}
