import view.AppMenuView;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Plants vs. Zombies 2 ===");
        System.out.println("Welcome! Type 'register -u ...' to begin, or 'menu enter login' to sign in.");
        System.out.println();

        AppMenuView appView = AppMenuView.getInstance();
        appView.run();
    }
}
