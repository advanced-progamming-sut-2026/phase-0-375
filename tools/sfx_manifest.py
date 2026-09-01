"""Paths (under assets/music/) for SFX wired in GameSfx.java."""

from __future__ import annotations

# Keep in sync with core/src/main/java/view/gui/audio/GameSfx.java
# Paths are relative to assets/music/. Extensions must match on disk (.ogg or .mp3).
WIRED_SFX: tuple[str, ...] = (
    "SFXs/PVZ-click/Audio_Always_Loaded.048.ogg",
    "SFXs/PVZ-click/Audio_Always_Loaded.092.ogg",
    "SFXs/PVZ-collectsun/Audio_Always_Loaded.051.ogg",
    "SFXs/PVZ-error/Audio_Always_Loaded.344.ogg",
    "SFXs/PVZ-losegame/Audio_Always_Loaded.318.ogg",
    "SFXs/PVZ-pause/Audio_Always_Loaded.048.ogg",
    "SFXs/PVZ-plantaplant/Audio_Always_Loaded.204.ogg",
    "SFXs/PVZ-plantonwater/Audio_Always_Loaded.252.ogg",
    "SFXs/PVZ-gravebuster/Audio_Always_Loaded.381.ogg",
    "SFXs/PVZ-plantfood/Audio_Always_Loaded.011.ogg",
    "SFXs/PVZ-shovel/Audio_Always_Loaded.022.ogg",
    "SFXs/RiseaAndShineDrZarrabi.mp3",
    "SFXs/PVZ-zomboss/Audio_Always_Loaded.213.ogg",
)

DEFAULT_ZIP = "tools/sfx-bundle.zip"
