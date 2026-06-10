package model.item.equippable;

/**
 * Interface for items that a zombie carries and uses as equipment
 */
public interface Equippable {
    /**
     * Called when the zombie activates this item's effect.
     */
    void activate();

    /**
     * Called when the equipped item is destroyed.
     * The zombie should transition to a post-item state
     */
    void onDestroyed();

    /**
     * @return whether this item modifies the zombie's movement speed
     */
    boolean modifiesSpeed();

    /**
     * @return the speed multiplier applied while this item is equipped
     * (e.g. 0.5 = half speed while carrying sarcophagus)
     */
    float getSpeedModifier();

    /**
     * @return whether this item is currently active/equipped
     */
    boolean isActive();

    /**
     * @param active whether the item is active
     */
    void setActive(boolean active);

    /**
     * @return a description of what this equippable does
     */
    String getDescription();
}
