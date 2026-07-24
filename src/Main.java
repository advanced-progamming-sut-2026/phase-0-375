import view.AppMenuView;
import view.tui.TuiShell;

public class Main {
    public static void main(String[] args) {
        boolean plain = false;
        for (String arg : args) {
            if ("--plain".equalsIgnoreCase(arg)) {
                plain = true;
                break;
            }
        }

        // Start the TUI unless --plain was passed or no real terminal is
        // attached (piped input, IDE console, tests) - then keep classic CLI.
        boolean tui = !plain && TuiShell.tryStart();

        if (tui) {
            TuiShell shell = TuiShell.getActive();
            shell.log("=== Plants vs. Zombies 2 ===");
            shell.log("Welcome! Type 'register -u ...' to begin, or 'menu enter login' to sign in.");
        } else {
            System.out.println("=== Plants vs. Zombies 2 ===");
            System.out.println("Welcome! Type 'register -u ...' to begin, or 'menu enter login' to sign in.");
            System.out.println();
        }

        try {
            AppMenuView.getInstance().run();
        } finally {
            TuiShell.stop();
        }
    }
}
