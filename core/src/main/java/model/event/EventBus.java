package model.event;

public interface EventBus {
    public void dispatch(GameEvent event);
}
