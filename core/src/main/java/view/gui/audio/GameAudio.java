package view.gui.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import model.app.App;
import model.user.User;
import controller.result.CommandResult;

import java.util.EnumMap;
import java.util.Map;

/**
 * Applies music/SFX volumes from the logged-in user's settings.
 * Loads BGM from {@code assets/music/} via {@link MusicTracks} (missing files = silent).
 */
public final class GameAudio {
    private static final GameAudio INSTANCE = new GameAudio();
    private static final String[] EXTENSIONS = {".ogg", ".mp3", ".wav"};

    private float musicVolume = 1f;
    private float sfxVolume = 1f;
    private Music currentMusic;
    private MusicTracks currentTrack;
    private final Map<MusicTracks, Music> cache = new EnumMap<>(MusicTracks.class);
    private final Map<GameSfx, Sound> sfxCache = new EnumMap<>(GameSfx.class);

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

    /** Plays a catalog track (looping). No-op if the file is missing. */
    public void play(MusicTracks track) {
        play(track, true);
    }

    public void play(MusicTracks track, boolean looping) {
        if (track == null) {
            stopMusic();
            return;
        }
        if (track == currentTrack && currentMusic != null && currentMusic.isPlaying()) {
            currentMusic.setLooping(looping);
            currentMusic.setVolume(musicVolume);
            return;
        }
        Music music = obtain(track);
        if (music == null) {
            return;
        }
        playMusic(music, looping);
        currentTrack = track;
    }

    public void playMusic(Music music, boolean looping) {
        if (currentMusic != null && currentMusic != music) {
            currentMusic.stop();
        }
        currentMusic = music;
        if (music == null) {
            currentTrack = null;
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
        currentTrack = null;
    }

    public MusicTracks currentTrack() {
        return currentTrack;
    }

    public long playSound(Sound sound) {
        if (sound == null || sfxVolume <= 0f) {
            return -1L;
        }
        return sound.play(sfxVolume);
    }

    /** Plays a catalog SFX (missing file = silent). */
    public void playSfx(GameSfx sfx) {
        if (sfx == null) {
            return;
        }
        playSound(obtainSfx(sfx));
    }

    public void playNavClick() {
        playSfx(GameSfx.NAV_CLICK);
    }

    public void playOverlayOpen() {
        playSfx(GameSfx.OVERLAY_OPEN);
    }

    /** Successful shop / collection purchase (same sting as overlay open). */
    public void playPurchaseSuccess() {
        playSfx(GameSfx.OVERLAY_OPEN);
    }

    public void feedbackPurchase(CommandResult<?> result) {
        if (result == null) {
            return;
        }
        if (result.isSuccess()) {
            playPurchaseSuccess();
        } else {
            playSfx(GameSfx.ERROR);
        }
    }

    public static void playPlantPlaceSfx(String plantName) {
        if (plantName == null || plantName.isBlank()) {
            return;
        }
        GameSfx sfx = GameSfx.PLANT;
        if ("Sea-shroom".equalsIgnoreCase(plantName)
                || "Tangle Kelp".equalsIgnoreCase(plantName)
                || "Lily Pad".equalsIgnoreCase(plantName)) {
            sfx = GameSfx.PLANT_ON_WATER;
        } else if ("Grave Buster".equalsIgnoreCase(plantName)) {
            sfx = GameSfx.GRAVE_BUSTER;
        }
        get().playSfx(sfx);
    }

    /** Disposes cached {@link Music} instances (call on app exit). */
    public void dispose() {
        stopMusic();
        for (Music music : cache.values()) {
            if (music != null) {
                music.dispose();
            }
        }
        cache.clear();
        for (Sound sound : sfxCache.values()) {
            if (sound != null) {
                sound.dispose();
            }
        }
        sfxCache.clear();
    }

    private Sound obtainSfx(GameSfx sfx) {
        Sound cached = sfxCache.get(sfx);
        if (cached != null) {
            return cached;
        }
        FileHandle file = resolveMusicFile(sfx.relativePath);
        if (file == null) {
            Gdx.app.debug("GameAudio", "Missing SFX: " + sfx.relativePath);
            return null;
        }
        try {
            Sound sound = Gdx.audio.newSound(file);
            sfxCache.put(sfx, sound);
            return sound;
        } catch (Exception e) {
            Gdx.app.error("GameAudio", "Failed to load " + file.path(), e);
            return null;
        }
    }

    private Music obtain(MusicTracks track) {
        Music cached = cache.get(track);
        if (cached != null) {
            return cached;
        }
        FileHandle file = resolveMusicFile(track.relativePath);
        if (file == null) {
            Gdx.app.debug("GameAudio", "Missing music: " + track.relativePath);
            return null;
        }
        try {
            Music music = Gdx.audio.newMusic(file);
            cache.put(track, music);
            return music;
        } catch (Exception e) {
            Gdx.app.error("GameAudio", "Failed to load " + file.path(), e);
            return null;
        }
    }

    /**
     * Resolves {@code assets/music/<relative>.(ogg|mp3|wav)}, trying {@code assets/} prefix
     * and bare working-dir paths (same pattern as other GUI media).
     */
    static FileHandle resolveMusicFile(String relativeWithoutExt) {
        if (relativeWithoutExt == null || relativeWithoutExt.isBlank()) {
            return null;
        }
        String base = relativeWithoutExt.replace('\\', '/');
        while (base.startsWith("/")) {
            base = base.substring(1);
        }
        for (String ext : EXTENSIONS) {
            FileHandle local = Gdx.files.local("assets/music/" + base + ext);
            if (local.exists()) {
                return local;
            }
            FileHandle bare = Gdx.files.local("music/" + base + ext);
            if (bare.exists()) {
                return bare;
            }
        }
        return null;
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
