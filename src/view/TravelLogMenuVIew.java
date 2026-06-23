package view;

public class TravelLogMenuVIew extends AppMenuView {
    private static TravelLogMenuVIew instance = null;

    public static TravelLogMenuVIew getInstance() {
        if (instance == null)  instance = new TravelLogMenuVIew();
        return instance;
    }


    @Override
    public void processInput(String input) {}
}
