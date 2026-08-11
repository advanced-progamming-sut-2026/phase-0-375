package view.gui.audio;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import model.app.App;
import model.user.User;

/**
 * Applies music/SFX volumes from the logged-in user's settings.
 * Sounds and music tracks register here so volume changes take effect live.
 */
public final class GameAudio {
    private static final GameAudio INSTANCE = new GameAudio();

    private float musicVolume = 1f;
    private float sfxVolume = 1f;
    private Music currentMusic;

    private GameAudio() {}

    public static GameAudio get() {
        return INSTANCE;
    }

    /** Pulls volumes from the current user (or defaults). */
    public void syncFromUser() {
        User user = App.getInstance().getCurrentUser();
        if (user == null) {
            setMusicVolume(1f);
            setSfxVolume(1f);
            return;
        }
        setMusicVolume(user.getMusicVolume());
        setSfxVolume(user.getSfxVolume());
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public float getSfxVolume() {
        return sfxVolume;
    }

    public void setMusicVolume(float volume) {
        musicVolume = clamp01(volume);
        if (currentMusic != null) {
            currentMusic.setVolume(musicVolume);
        }
    }

    public void setSfxVolume(float volume) {
        sfxVolume = clamp01(volume);
    }

    public void playMusic(Music music, boolean looping) {
        if (currentMusic != null && currentMusic != music) {
            currentMusic.stop();
        }
        currentMusic = music;
        if (music == null) {
            return;
        }
        music.setLooping(looping);
        music.setVolume(musicVolume);
        if (!music.isPlaying()) {
            music.play();
        }
    }

    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
        }
    }

    public long playSound(Sound sound) {
        if (sound == null || sfxVolume <= 0f) {
            return -1L;
        }
        return sound.play(sfxVolume);
    }

    private static float clamp01(float value) {
        if (value < 0f) {
            return 0f;
        }
        if (value > 1f) {
            return 1f;
        }
        return value;
    }
}
