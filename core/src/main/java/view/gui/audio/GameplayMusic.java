package view.gui.audio;

import model.enums.Chapter;
import model.enums.LootPickupKind;
import model.enums.WaveManagerPhase;
import model.game.core.GameModel;
import model.game.wave.Wave;
import model.game.wave.WaveManager;
import model.item.LootPickup;

/**
 * BGM during {@link view.gui.screen.GameplayScreen}: wave beds, win/lose stings, loot reward.
 * Menu tracks (e.g. choose seeds) keep playing until the first active wave starts.
 */
public final class GameplayMusic {
    private int lastWaveIndex = -1;
    private int midWaveOrdinal;
    private boolean winRewardPlayed;

    public void reset() {
        lastWaveIndex = -1;
        midWaveOrdinal = 0;
        winRewardPlayed = false;
    }

    /**
     * @param wavesPaused {@code true} during Last Stand setup or similar (no wave BGM yet)
     */
    public void sync(GameModel model, Chapter chapter, boolean wavesPaused) {
        if (chapter == null || model == null || wavesPaused) {
            return;
        }
        WaveManager waves = model.getWaveManager();
        if (waves == null || waves.getTotalWaveCount() <= 0) {
            return;
        }
        if (waves.getPhase() != WaveManagerPhase.ACTIVE_WAVE) {
            return;
        }
        int index = waves.getCurrentWaveIndex();
        if (index == lastWaveIndex) {
            return;
        }
        lastWaveIndex = index;
        GameAudio.get().play(trackForWave(chapter, index, waves.getCurrentWave()));
    }

    public void playVictory(Chapter chapter) {
        if (chapter == null) {
            return;
        }
        GameAudio.get().play(MusicTracks.victory(chapter), false);
    }

    public void playDefeat(Chapter chapter) {
        if (chapter == null) {
            return;
        }
        GameAudio.get().playSfx(GameSfx.LOSE_GAME);
        GameAudio.get().play(MusicTracks.defeat(chapter), false);
    }

    /** Short sting for gem / flower-pot loot (not every coin). */
    public void playLootSting(LootPickup loot) {
        if (loot == null) {
            return;
        }
        LootPickupKind kind = loot.getKind();
        if (kind == LootPickupKind.COIN_GOLD || kind == LootPickupKind.COIN_SILVER) {
            return;
        }
        GameAudio.get().play(MusicTracks.REWARD_STING, false);
    }

    /** Piñata-style bed on the win screen (after victory sting). */
    public void playChapterRewardOnce(Chapter chapter) {
        if (winRewardPlayed || chapter == null) {
            return;
        }
        winRewardPlayed = true;
        GameAudio.get().play(MusicTracks.reward(chapter), true);
    }

    private MusicTracks trackForWave(Chapter chapter, int waveIndex, Wave wave) {
        if (waveIndex <= 0) {
            return MusicTracks.wave(chapter, MusicTracks.WavePhase.FIRST);
        }
        if (wave != null && wave.isFinalWave()) {
            return MusicTracks.wave(chapter, MusicTracks.WavePhase.FINAL);
        }
        midWaveOrdinal++;
        if (chapter == Chapter.ANCIENT_EGYPT) {
            return MusicTracks.wave(chapter, MusicTracks.WavePhase.MID);
        }
        return (midWaveOrdinal % 2 == 1)
            ? MusicTracks.wave(chapter, MusicTracks.WavePhase.MID)
            : MusicTracks.waveMidB(chapter);
    }
}
