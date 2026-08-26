package model.game.score;

import model.game.core.GameModel;
import model.game.wave.WaveManager;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scores a run of the daily Myopoint game (see
 * {@link model.game.level.special.ScoreLevel}).
 *
 * <p>Every kill earns base points scaled by the victim's toughness, and five
 * bonus patterns pay extra:
 * <ul>
 *   <li><b>Multi-kill</b>: one source (e.g. a single projectile plus its
 *       splash) kills 2+ zombies - +{@value #MULTI_KILL_BONUS} per extra kill
 *       from the same source.</li>
 *   <li><b>Quick kill</b>: a zombie dies within
 *       {@value #QUICK_KILL_WINDOW_SECONDS} seconds of spawning -
 *       +{@value #QUICK_KILL_BONUS}.</li>
 *   <li><b>Simultaneous kills</b>: 2+ zombies die inside a small window of
 *       ticks ({@value #SIMULTANEOUS_WINDOW_TICKS} ticks) from different
 *       sources - +{@value #SIMULTANEOUS_BONUS} per zombie in the group.
 *       Kills whose source is unknown (explosions, mowers, ...) also qualify;
 *       only kills provably from the same source are excluded, because those
 *       are already paid as a multi-kill.</li>
 *   <li><b>Combo streak</b>: each kill at most {@value #COMBO_WINDOW_SECONDS}
 *       seconds after the previous one extends a streak and pays
 *       +{@value #COMBO_BONUS_STEP} x (streak - 1), capped at streak
 *       {@value #COMBO_MAX_STREAK}.</li>
 *   <li><b>Perfect wave</b>: a wave ends without losing a plant or spending a
 *       lawn mower - +{@value #PERFECT_WAVE_BONUS}.</li>
 * </ul>
 */
public class MyopointTracker {

    public static final int BASE_KILL_POINTS = 10;
    /** One extra base point per this much of the victim's base HP. */
    public static final int TOUGHNESS_HP_PER_POINT = 20;

    public static final float QUICK_KILL_WINDOW_SECONDS = 5f;
    public static final int QUICK_KILL_BONUS = 25;

    public static final long SIMULTANEOUS_WINDOW_TICKS = 3;
    public static final int SIMULTANEOUS_BONUS = 20;

    public static final int MULTI_KILL_BONUS = 30;

    public static final float COMBO_WINDOW_SECONDS = 5f;
    public static final int COMBO_BONUS_STEP = 5;
    public static final int COMBO_MAX_STREAK = 10;

    public static final int PERFECT_WAVE_BONUS = 100;

    private static final String KEY_KILLS = "Kills";
    private static final String KEY_MULTI = "Multi-kills";
    private static final String KEY_QUICK = "Quick kills";
    private static final String KEY_SIMULTANEOUS = "Simultaneous kills";
    private static final String KEY_COMBO = "Combo streaks";
    private static final String KEY_PERFECT = "Perfect waves";

    /** A single kill, kept briefly to detect simultaneous kills. */
    private static final class KillRecord {
        final long tick;
        final Object source;
        boolean simultaneousAwarded;

        KillRecord(long tick, Object source) {
            this.tick = tick;
            this.source = source;
        }
    }

    /** One scored award the GUI can toast without re-parsing the breakdown. */
    public record AwardEvent(String key, int points) {
        /** True for stylish bonuses (everything except the base kill award). */
        public boolean isBonus() {
            return !KEY_KILLS.equals(key);
        }
    }

    private int totalPoints;
    private final Map<String, Integer> breakdown = new LinkedHashMap<>();
    private final Deque<AwardEvent> pendingAwards = new ArrayDeque<>();

    private final Map<ZombieInstance, Float> spawnTimes = new HashMap<>();
    private final Deque<KillRecord> recentKills = new ArrayDeque<>();
    private final Map<Object, Integer> killsBySource = new HashMap<>();

    private float lastKillTime = Float.NEGATIVE_INFINITY;
    private int comboStreak;

    private int trackedWaveIndex = -1;
    private int plantsLostAtWaveStart;
    private boolean mowerUsedAtWaveStart;
    private boolean finished;

    public MyopointTracker() {
        breakdown.put(KEY_KILLS, 0);
        breakdown.put(KEY_MULTI, 0);
        breakdown.put(KEY_QUICK, 0);
        breakdown.put(KEY_SIMULTANEOUS, 0);
        breakdown.put(KEY_COMBO, 0);
        breakdown.put(KEY_PERFECT, 0);
    }

    /** Called by the game model whenever a zombie enters the field. */
    public void onZombieSpawned(ZombieInstance zombie, float timeSeconds) {
        if (zombie == null) return;
        spawnTimes.put(zombie, timeSeconds);
    }

    /** Called by the game model at the moment a zombie's death is finalised. */
    public void onZombieKilled(ZombieInstance zombie, float timeSeconds, long tick) {
        if (zombie == null || finished) return;

        awardBaseKill(zombie);
        awardQuickKillIfAny(zombie, timeSeconds);
        Object source = zombie.getLastDamageSource();
        awardMultiKillIfAny(source);
        awardSimultaneousKills(tick, source);
        awardComboStreak(timeSeconds);
    }

    /** Base points, scaled by the victim's toughness. */
    private void awardBaseKill(ZombieInstance zombie) {
        int toughnessBonus = 0;
        if (zombie.getDefinition() != null) {
            toughnessBonus = Math.max(0, zombie.getDefinition().getBaseHP() / TOUGHNESS_HP_PER_POINT);
        }
        award(KEY_KILLS, BASE_KILL_POINTS + toughnessBonus);
    }

    /** Quick kill: died shortly after spawning. */
    private void awardQuickKillIfAny(ZombieInstance zombie, float timeSeconds) {
        Float spawnTime = spawnTimes.remove(zombie);
        if (spawnTime != null && timeSeconds - spawnTime <= QUICK_KILL_WINDOW_SECONDS) {
            award(KEY_QUICK, QUICK_KILL_BONUS);
        }
    }

    /** Multi-kill: the same source killed more than one zombie. */
    private void awardMultiKillIfAny(Object source) {
        if (source == null) return;
        int kills = killsBySource.merge(source, 1, Integer::sum);
        if (kills >= 2) {
            award(KEY_MULTI, MULTI_KILL_BONUS);
        }
    }

    /**
     * Simultaneous kills: deaths within a small tick window whose sources
     * are not provably the same (same non-null source is a multi-kill).
     */
    private void awardSimultaneousKills(long tick, Object source) {
        while (!recentKills.isEmpty()
                && tick - recentKills.peekFirst().tick > SIMULTANEOUS_WINDOW_TICKS) {
            recentKills.removeFirst();
        }
        KillRecord current = new KillRecord(tick, source);
        boolean simultaneous = false;
        for (KillRecord past : recentKills) {
            if (past.source != null && source != null && past.source == source) continue;
            simultaneous = true;
            if (!past.simultaneousAwarded) {
                past.simultaneousAwarded = true;
                award(KEY_SIMULTANEOUS, SIMULTANEOUS_BONUS);
            }
        }
        if (simultaneous) {
            current.simultaneousAwarded = true;
            award(KEY_SIMULTANEOUS, SIMULTANEOUS_BONUS);
        }
        recentKills.addLast(current);
    }

    /** Combo streak: keep killing without a pause. */
    private void awardComboStreak(float timeSeconds) {
        if (timeSeconds - lastKillTime <= COMBO_WINDOW_SECONDS) {
            comboStreak++;
            int step = Math.min(comboStreak, COMBO_MAX_STREAK) - 1;
            award(KEY_COMBO, COMBO_BONUS_STEP * step);
        } else {
            comboStreak = 1;
        }
        lastKillTime = timeSeconds;
    }

    /** Called every tick by the score level to watch wave transitions. */
    public void tick(GameModel model, float deltaTime) {
        if (model == null || finished) return;
        WaveManager waveManager = model.getWaveManager();
        if (waveManager == null) return;

        int waveIndex = waveManager.getCurrentWaveIndex();
        if (trackedWaveIndex < 0) {
            trackedWaveIndex = waveIndex;
            resetWaveBaseline(model);
            return;
        }
        if (waveIndex != trackedWaveIndex) {
            scoreCompletedWave(model);
            trackedWaveIndex = waveIndex;
            resetWaveBaseline(model);
        }
    }

    /** Called once when the game ends; scores the final wave on a win. */
    public void onGameFinished(GameModel model, boolean won) {
        if (finished) return;
        if (won && model != null && trackedWaveIndex >= 0) {
            scoreCompletedWave(model);
        }
        finished = true;
    }

    private void resetWaveBaseline(GameModel model) {
        plantsLostAtWaveStart = model.getPlantsLost();
        mowerUsedAtWaveStart = model.isLawnMowerUsed();
    }

    private void scoreCompletedWave(GameModel model) {
        if (model.getPlantsLost() == plantsLostAtWaveStart
                && model.isLawnMowerUsed() == mowerUsedAtWaveStart) {
            award(KEY_PERFECT, PERFECT_WAVE_BONUS);
        }
    }

    private void award(String key, int points) {
        if (points <= 0) return;
        totalPoints += points;
        breakdown.merge(key, points, Integer::sum);
        pendingAwards.addLast(new AwardEvent(key, points));
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    /** Current combo streak length (1 after a lone kill, 0 before any kill). */
    public int getComboStreak() {
        return comboStreak;
    }

    public boolean isFinished() {
        return finished;
    }

    /**
     * Drains award events queued since the last call so the HUD can show
     * floating "+N" toasts without missing or double-counting awards.
     */
    public List<AwardEvent> drainAwardEvents() {
        if (pendingAwards.isEmpty()) {
            return List.of();
        }
        List<AwardEvent> out = new ArrayList<>(pendingAwards.size());
        while (!pendingAwards.isEmpty()) {
            out.add(pendingAwards.removeFirst());
        }
        return out;
    }

    /** Per-pattern point breakdown, in stable display order. */
    public Map<String, Integer> getBreakdown() {
        return new LinkedHashMap<>(breakdown);
    }

    /** Human-readable score summary used by 'show score' and the end screen. */
    public List<String> getSummaryLines() {
        List<String> lines = new ArrayList<>();
        lines.add("Myopoints: " + totalPoints);
        for (Map.Entry<String, Integer> entry : breakdown.entrySet()) {
            lines.add("  " + entry.getKey() + ": " + entry.getValue());
        }
        return lines;
    }

    /** Short label for a floating HUD toast (e.g. {@code Quick kill!}). */
    public static String toastLabel(String key) {
        if (key == null) return "Myopoint!";
        return switch (key) {
            case KEY_MULTI -> "Multi-kill!";
            case KEY_QUICK -> "Quick kill!";
            case KEY_SIMULTANEOUS -> "Simultaneous!";
            case KEY_COMBO -> "Combo!";
            case KEY_PERFECT -> "Perfect wave!";
            case KEY_KILLS -> "Kill!";
            default -> key + "!";
        };
    }
}
