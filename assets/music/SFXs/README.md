# Sound effects (SFX)

SFX binaries (`.ogg` / `.mp3` under this folder) are **gitignored**. After `git pull`, install once:

```bash
python3 tools/install_sfx.py
```

This extracts `tools/sfx-bundle.zip` (13 files, ~230 KB) into `assets/music/SFXs/`.

**Format:** use Ogg **Vorbis** or MP3. Opus-in-Ogg will not play in libGDX (e.g. level-1 voice is shipped as `.mp3`).

## Rebuild the bundle (maintainers)

When wired SFX files change locally (list in `tools/sfx_manifest.py`, must match `GameSfx.java`):

```bash
python3 tools/build_sfx_bundle.py
git add tools/sfx-bundle.zip
```

## Wired files (`GameSfx.java`)

| Path | Used for |
|------|----------|
| `PVZ-click/...048.ogg` | Menu navigation, tabs, carousel |
| `PVZ-click/...092.ogg` | Overlay open, successful purchase |
| `PVZ-collectsun/...051.ogg` | Collect sun |
| `PVZ-error/...344.ogg` | Error toast / failed purchase |
| `PVZ-losegame/...318.ogg` | Level lost |
| `PVZ-pause/...048.ogg` | Pause menu |
| `PVZ-plantaplant/...204.ogg` | Plant on lawn (default) |
| `PVZ-plantonwater/...252.ogg` | Sea-shroom, Tangle Kelp, Lily Pad |
| `PVZ-gravebuster/...381.ogg` | Grave Buster |
| `PVZ-plantfood/...011.ogg` | Plant Food / boosted plant |
| `PVZ-shovel/...022.ogg` | Shovel |
| `RiseaAndShineDrZarrabi.mp3` | Level 1 NPC intro — delay: `NpcDialogueOverlay.RISE_AND_SHINE_VOICE_DELAY_SEC` |
| `PVZ-zomboss/...213.ogg` | Zomboss spawn laugh |
