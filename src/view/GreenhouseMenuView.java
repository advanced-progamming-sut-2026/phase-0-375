package view;

public class GreenhouseMenuView extends AppMenuView {
    private static GreenhouseMenuView instance = null;

    public static GreenhouseMenuView getInstance() {
        if (instance == null)  instance = new GreenhouseMenuView();
        return instance;
    }


    @Override
    public void processInput(String input) {}
}
