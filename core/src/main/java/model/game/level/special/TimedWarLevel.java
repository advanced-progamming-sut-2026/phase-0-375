package model.game.level.special;

import model.app.App;
import model.game.core.GameModel;
import model.game.level.LevelConfig;
import model.game.level.RegularLevel;
import model.game.rule.TimedWarEndGameCondition;

/**
 * Timed War: kill the target number of zombies before the time limit runs
 * out. Losing happens when time expires first (or a zombie reaches the
 * house, as always).
 *
 * <p>Progress can decay if zombies are not killed within each specified time lapse.
 *
 * <p>The win/loss rule itself lives in {@link TimedWarEndGameCondition},
 * driven by {@code timedWarTargetKills} and {@code timedWarLimit} in the
 * level rules.
 */
public class TimedWarLevel extends RegularLevel {

    public static final float DEFAULT_DECAY_INTERVAL = 5.0f;

    private int effectiveKills;
    private int lastKnownModelKills;
    private float timeSinceLastKill;
    private float decayInterval = DEFAULT_DECAY_INTERVAL;

    public TimedWarLevel(LevelConfig config) {
        super(config);
        config.setEndGameCondition(new TimedWarEndGameCondition(this));
    }

    @Override
    public boolean canStart() {
        return super.canStart()
                && getConfig().getRules().getTimedWarTargetKills() > 0
                && getConfig().getRules().getTimedWarLimit() > 0;
    }

    @Override
    public void onStart() {
        super.onStart();
        effectiveKills = 0;
        lastKnownModelKills = 0;
        timeSinceLastKill = 0f;
        if (getConfig() != null && getConfig().getRules() != null && getConfig().getRules().getTimedWarDecayInterval() > 0) {
            decayInterval = getConfig().getRules().getTimedWarDecayInterval();
        }
    }

    @Override
    public void tick(float deltaTime) {
        super.tick(deltaTime);
        GameModel model = App.getInstance().getCurrentGameModel();
        if (model != null) {
            int modelKills = model.getZombiesKilled();
            int newKills = modelKills - lastKnownModelKills;
            if (newKills > 0) {
                effectiveKills += newKills;
                lastKnownModelKills = modelKills;
                timeSinceLastKill = 0f;
            } else {
                timeSinceLastKill += deltaTime;
                if (timeSinceLastKill >= decayInterval) {
                    if (effectiveKills > 0) {
                        effectiveKills--;
                    }
                    timeSinceLastKill = 0f;
                }
            }
        } else {
            timeSinceLastKill += deltaTime;
            if (timeSinceLastKill >= decayInterval) {
                if (effectiveKills > 0) {
                    effectiveKills--;
                }
                timeSinceLastKill = 0f;
            }
        }
    }

    /** Manually record a kill (useful in testing or scripted events). */
    public void recordKill() {
        effectiveKills++;
        timeSinceLastKill = 0f;
    }

    public int getEffectiveKills() {
        return effectiveKills;
    }

    public void setEffectiveKills(int effectiveKills) {
        this.effectiveKills = Math.max(0, effectiveKills);
    }

    public int getTargetKills() {
        return getConfig() != null && getConfig().getRules() != null
                ? getConfig().getRules().getTimedWarTargetKills()
                : 0;
    }

    public float getProgress01() {
        int target = getTargetKills();
        if (target <= 0) return 0f;
        return Math.min(1f, Math.max(0f, (float) effectiveKills / target));
    }

    public float getDecayInterval() {
        return decayInterval;
    }

    public void setDecayInterval(float decayInterval) {
        this.decayInterval = Math.max(0.1f, decayInterval);
    }

    public float getTimeSinceLastKill() {
        return timeSinceLastKill;
    }
}

