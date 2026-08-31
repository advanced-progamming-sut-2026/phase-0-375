# Music (BGM)

Audio files are **not** in git (~220 MB). After `git pull`, download them once on your machine.

---

## Automatic download

From the **project root** (where `build.gradle` lives):

```bash
python3 tools/download_ost.py
```

### Requirements

- **Python 3** (usually preinstalled on macOS/Linux)
- **curl** (the script uses it to fetch files from the CDN)
- Internet connection

### What it does

1. Reads the PvZ2 album track list from Khinsider.
2. Downloads **36 files** into `assets/music/` using the names `GameAudio` / `MusicTracks` expect (`.mp3` format).
3. Skips files that already exist and look valid (>100 KB).

Example output:

```text
get   Title Screen -> menu/title.mp3
skip  egypt/wave_first.mp3
...
Done: 36/36 tracks in assets/music
```

### Verify

Run the game and check:

| Where | Expected BGM |
|-------|----------------|
| Login / Register | `menu/title` |
| Main Hub | `menu/hub` |
| Adventure / Chapter map | `menu/world_map` |
| Greenhouse | `menu/zen_garden` |
| Plant Selection | `<chapter>/choose_seeds` |
| In-level (wave 1) | `<chapter>/wave_first` |
| In-level (middle waves) | `wave_mid` or `wave_mid_a` / `wave_mid_b` |
| In-level (final wave) | `<chapter>/wave_final` |
| Win | `<chapter>/victory`, then `<chapter>/reward` on the results screen |
| Lose | `<chapter>/defeat` |
| Gem / flower-pot loot | `menu/reward_sting` |

If it is silent, check volume in Settings or confirm files exist under `assets/music/`.

### Troubleshooting

| Problem | Fix |
|---------|-----|
| `python3: command not found` | Install Python 3 or try `python` instead |
| `curl failed` / timeout | Check internet or VPN; rerun the script |
| Corrupt or tiny file | Delete that `.mp3` and rerun the script |
| `ModuleNotFoundError` | Only the standard library is needed; no pip dependencies |

**Important:** Do not commit `.mp3` / `.ogg` files — they are listed in `.gitignore`.

---

## Source album

https://downloads.khinsider.com/game-soundtracks/album/plants-vs.-zombies-2

Supported formats: **OGG**, **MP3**, or **WAV** with the same base name (e.g. `menu/title.mp3`).

---

## In-game usage

Wiring lives in `MusicTracks` (file ids) and `GameplayMusic` (when to play during levels).

### Menus

| Track | Screen |
|-------|--------|
| `menu/title` | Login, Register |
| `menu/hub` | Main Hub |
| `menu/world_map` | Adventure, Chapter Levels |
| `menu/zen_garden` | Greenhouse |
| `<chapter>/choose_seeds` | Plant Selection; continues into gameplay until wave 1 starts |

### Gameplay (`GameplayScreen`)

| Event | Track |
|-------|-------|
| Wave 1 starts | `<chapter>/wave_first` |
| Middle waves | Egypt: `wave_mid`. Frostbite / Beach / Dark Ages: alternates `wave_mid_a` and `wave_mid_b` |
| Final wave | `<chapter>/wave_final` |
| Level won | `<chapter>/victory` (once), then `<chapter>/reward` (loops on win overlay) |
| Level lost | `<chapter>/defeat` |
| Gem or flower-pot collected | `menu/reward_sting` (coins are silent) |

Levels without scripted waves (Vase Breaker, I, Zombie, etc.) keep **choose seeds** until win or lose. Last Stand keeps choose seeds until **LET'S ROCK!**.

---

## Track map (Khinsider → local path)

### Menu / hub → `menu/`

| Local path | Khinsider track name |
|---|---|
| `menu/title` | Title Screen |
| `menu/hub` | Player's House |
| `menu/world_map` | World Map |
| `menu/zen_garden` | Zen Garden |
| `menu/reward_sting` | Here's your Reward! |

### Ancient Egypt → `egypt/`

| Local path | Khinsider track name |
|---|---|
| `egypt/choose_seeds` | Ancient Egypt (Choose Your Seeds) |
| `egypt/wave_first` | Ancient Egypt (First Wave) |
| `egypt/wave_mid` | Ancient Egypt (Mid Wave A - B) |
| `egypt/wave_final` | Ancient Egypt (Final Wave) |
| `egypt/victory` | Ancient Egypt (Victory!) |
| `egypt/defeat` | Ancient Egypt (The Zombies Ate Your Brains!) |
| `egypt/reward` | Ancient Egypt (Reward) |

### Frostbite Caves → `frostbite/`

| Local path | Khinsider track name |
|---|---|
| `frostbite/choose_seeds` | Frostbite Caves (Choose Your Seeds) |
| `frostbite/wave_first` | Frostbite Caves (First Wave) |
| `frostbite/wave_mid_a` | Frostbite Caves (Mid Wave A) |
| `frostbite/wave_mid_b` | Frostbite Caves (Mid Wave B) |
| `frostbite/wave_final` | Frostbite Caves (Final Wave) |
| `frostbite/victory` | Frostbite Caves (Victory!) |
| `frostbite/defeat` | Frostbite Caves (The Zombies Ate Your Brains!) |
| `frostbite/reward` | Frostbite Caves (Reward) |

### Big Wave Beach → `beach/`

| Local path | Khinsider track name |
|---|---|
| `beach/choose_seeds` | Big Wave Beach (Choose Your Seeds) |
| `beach/wave_first` | Big Wave Beach (First Wave) |
| `beach/wave_mid_a` | Big Wave Beach (Mid Wave A) |
| `beach/wave_mid_b` | Big Wave Beach (Mid Wave B) |
| `beach/wave_final` | Big Wave Beach (Final Wave) |
| `beach/victory` | Big Wave Beach (Victory!) |
| `beach/defeat` | Big Wave Beach (The Zombies Ate Your Brains!) |
| `beach/reward` | Big Wave Beach (Reward) |

### Dark Ages → `dark_ages/`

| Local path | Khinsider track name |
|---|---|
| `dark_ages/choose_seeds` | Dark Ages (Choose Your Seeds) |
| `dark_ages/wave_first` | Dark Ages (First Wave) |
| `dark_ages/wave_mid_a` | Dark Ages (Mid Wave A) |
| `dark_ages/wave_mid_b` | Dark Ages (Mid Wave B) |
| `dark_ages/wave_final` | Dark Ages (Final Wave) |
| `dark_ages/victory` | Dark Ages (Victory!) |
| `dark_ages/defeat` | Dark Ages (The Zombies Ate Your Brains!) |
| `dark_ages/reward` | Dark Ages (Reward) |

---

## Manual download (optional)

If the script fails, download from Khinsider and rename according to the table above:

```bash
mv "Ancient Egypt (First Wave).mp3" assets/music/egypt/wave_first.mp3
```

Convert to OGG (optional):

```bash
ffmpeg -i assets/music/egypt/wave_first.mp3 -c:a libvorbis -q:a 5 assets/music/egypt/wave_first.ogg
```
