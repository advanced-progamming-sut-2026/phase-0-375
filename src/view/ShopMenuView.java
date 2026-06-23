package view;

import model.shop.Shop;

public class ShopMenuView extends AppMenuView {
    private static ShopMenuView instance = null;

    public static ShopMenuView getInstance() {
        if (instance == null)  instance = new ShopMenuView();
        return instance;
    }


    @Override
    public void processInput(String input) {}
}
