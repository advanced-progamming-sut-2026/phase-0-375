package model.game.wave.spawn;

import model.enums.SpawnPatternType;
import model.game.wave.EntryRuntime;
import model.game.wave.Wave;
import model.game.wave.WaveZombieEntry;
import model.zombie.definition.Zombie;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase-2 budget-based spawn strategy.
 *
 * <p>Spawns zombies until the cumulative {@link Zombie#getWavePointCost()}
 * reaches the wave's budget. Each zombie in the pool is spawned at least once
 * (round-robin over the pool) before any zombie is spawned twice. Selection
 * afterwards is uniform random — weight is ignored, only wavePointCost
 * matters. Each spawn is placed in a randomly chosen lane.</p>
 */
public class BudgetSpawnStrategy extends SpawnStrategy {

    private static final float SPAWN_INTERVAL_SECONDS = 1.5f;

    private final SpawnPatternType type = SpawnPatternType.STREAM;
    private final int budget;

    private final List<Zombie> spawnOrder = new ArrayList<>();
    private int cursor = 0;
    private int spent = 0;
    private float nextSpawnAt;

    public BudgetSpawnStrategy(EntryRuntime runtime, Wave wave, int budget) {
        super(runtime, wave);
        this.budget = Math.max(0, budget);
    }

    @Override
    public SpawnPatternType getType() {
        return type;
    }

    /** Called by {@link Wave#startWave()} via the entry's pattern. Builds the
     *  spawn order: every pool member once (round-robin), then uniform random
     *  fills, until the budget is exhausted. */
    @Override
    public void tick(float deltaTime) {
        if (runtime.isExhausted()) return;

        // Lazy initialization on first tick: build spawn order.
        if (spawnOrder.isEmpty()) {
            buildSpawnOrder();
            nextSpawnAt = runtime.getFirstSpawnAt();
            if (nextSpawnAt <= 0f) nextSpawnAt = 0.001f;
        }

        float clock = wave.getWaveClock();
        while (cursor < spawnOrder.size() && clock >= nextSpawnAt) {
            Zombie z = spawnOrder.get(cursor);
            wave.spawnOne(runtime.getWaveZombieEntry());
            spent += z.getWavePointCost();
            cursor++;
            int lane = wave.getRng().nextInt(0, Math.max(0, wave.getRowCount() - 1));
            model.app.App.logToShell("Zombie " + z.getName()
                    + " spawned at wave " + wave.getWaveNumber()
                    + " in lane " + lane
                    + " which costed " + z.getWavePointCost() + ".");
            nextSpawnAt += SPAWN_INTERVAL_SECONDS;
        }
        if (cursor >= spawnOrder.size()) {
            runtime.setExhausted(true);
        }
    }

    /**
     * Builds the spawn order: one of each pool member first (so every zombie
     * appears at least once), then uniform-random picks (weight ignored)
     * until the cumulative wavePointCost reaches the budget.
     */
    private void buildSpawnOrder() {
        WaveZombieEntry entry = runtime.getWaveZombieEntry();
        List<model.game.wave.ZombieSpawnCandidate> pool = entry.getPool();
        if (pool == null || pool.isEmpty()) return;

        // Pass 1: every pool member once (cheapest first to maximize count).
        List<Zombie> poolDefs = new ArrayList<>();
        for (model.game.wave.ZombieSpawnCandidate c : pool) {
            if (c.getZombieDefinition() != null) poolDefs.add(c.getZombieDefinition());
        }
        poolDefs.sort(java.util.Comparator.comparingInt(Zombie::getWavePointCost));

        for (Zombie z : poolDefs) {
            if (z.getWavePointCost() <= 0) continue;
            if (spent + z.getWavePointCost() > budget && !spawnOrder.isEmpty()) {
                // Adding this would exceed budget; skip but keep going —
                // cheaper ones later might still fit.
                continue;
            }
            spawnOrder.add(z);
            spent += z.getWavePointCost();
        }

        // Pass 2: uniform-random fills until we can't fit the cheapest zombie.
        int minCost = poolDefs.stream()
                .mapToInt(Zombie::getWavePointCost)
                .filter(c -> c > 0)
                .min().orElse(Integer.MAX_VALUE);
        int safety = 1000;
        while (spent + minCost <= budget && safety-- > 0) {
            Zombie z = poolDefs.get(wave.getRng().nextInt(0, poolDefs.size() - 1));
            if (z.getWavePointCost() <= 0) continue;
            if (spent + z.getWavePointCost() > budget) continue;
            spawnOrder.add(z);
            spent += z.getWavePointCost();
        }
    }
}
