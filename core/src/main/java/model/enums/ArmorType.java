package model.enums;

public enum ArmorType {
    Cone("ConeDefault"),
    Bucket("BucketDefault"),
    Brick("BrickDefault"),
    ShoulderArmor("ShoulderArmorDefault"),
    Crown("CrownDefault"),
    Newspaper("NewspaperDefault");

    private String primaryAlias;

    ArmorType(String primaryAlias) {
        this.primaryAlias = primaryAlias;
    }

    public String getPrimaryAlias() {
        return primaryAlias;
    }
}
