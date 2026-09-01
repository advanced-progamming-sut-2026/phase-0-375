package model.game.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Per-plant seed-packet recharge timers for one level. */
final class SeedCooldownBank {

    private final Map<String, Float> remaining = new HashMap<>();
    private boolean disabled;

    boolean isReady(String plantName) {
        if (disabled) {
            return true;
        }
        Float left = remaining.get(plantName);
        return left == null || left <= 0f;
    }

    float remaining(String plantName) {
        if (disabled) {
            return 0f;
        }
        Float left = remaining.get(plantName);
        return left == null ? 0f : left;
    }

    void start(String plantName, float seconds) {
        if (disabled || seconds <= 0f) {
            return;
        }
        remaining.put(plantName, seconds);
    }

    void disable() {
        disabled = true;
        remaining.clear();
    }

    boolean isDisabled() {
        return disabled;
    }

    Map<String, Float> snapshot() {
        return new HashMap<>(remaining);
    }

    void restore(Map<String, Float> cooldowns, boolean disabledFlag) {
        remaining.clear();
        if (cooldowns != null) {
            remaining.putAll(cooldowns);
        }
        disabled = disabledFlag;
    }

    void syncFromNetwork(Map<String, Float> cooldowns) {
        remaining.clear();
        if (cooldowns == null) {
            return;
        }
        for (Map.Entry<String, Float> entry : cooldowns.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0f) {
                continue;
            }
            remaining.put(entry.getKey(), entry.getValue());
        }
    }

    void tick(float deltaTime) {
        if (remaining.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<String, Float>> it = remaining.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Float> e = it.next();
            float left = e.getValue() - deltaTime;
            if (left <= 0f) {
                it.remove();
            } else {
                e.setValue(left);
            }
        }
    }
}
