#!/usr/bin/env python3
"""Download the 36 PvZ2 BGM tracks used by the game into assets/music/.

Covers menu/hub screens and all four chapters (choose seeds, waves, win/lose,
chapter reward, reward sting). See assets/music/README.md for when each file plays.
Local dev only — audio binaries are gitignored.
"""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

ALBUM_URL = "https://downloads.khinsider.com/game-soundtracks/album/plants-vs.-zombies-2"
B36 = "0123456789abcdefghijklmnopqrstuvwxyz"

# Khinsider display title -> relative path without extension (see assets/music/README.md)
# 36 tracks: 5 menu + 4 chapters × (choose + 4–5 wave beds + victory + defeat + reward)
TRACK_MAP: dict[str, str] = {
    "Title Screen": "menu/title",
    "Player's House": "menu/hub",
    "World Map": "menu/world_map",
    "Zen Garden": "menu/zen_garden",
    "Here's your Reward!": "menu/reward_sting",
    "Ancient Egypt (Choose Your Seeds)": "egypt/choose_seeds",
    "Ancient Egypt (First Wave)": "egypt/wave_first",
    "Ancient Egypt (Mid Wave A - B)": "egypt/wave_mid",
    "Ancient Egypt (Final Wave)": "egypt/wave_final",
    "Ancient Egypt (Victory!)": "egypt/victory",
    "Ancient Egypt (The Zombies Ate Your Brains!)": "egypt/defeat",
    "Ancient Egypt (Reward)": "egypt/reward",
    "Frostbite Caves (Choose Your Seeds)": "frostbite/choose_seeds",
    "Frostbite Caves (First Wave)": "frostbite/wave_first",
    "Frostbite Caves (Mid Wave A)": "frostbite/wave_mid_a",
    "Frostbite Caves (Mid Wave B)": "frostbite/wave_mid_b",
    "Frostbite Caves (Final Wave)": "frostbite/wave_final",
    "Frostbite Caves (Victory!)": "frostbite/victory",
    "Frostbite Caves (The Zombies Ate Your Brains!)": "frostbite/defeat",
    "Frostbite Caves (Reward)": "frostbite/reward",
    "Big Wave Beach (Choose Your Seeds)": "beach/choose_seeds",
    "Big Wave Beach (First Wave)": "beach/wave_first",
    "Big Wave Beach (Mid Wave A)": "beach/wave_mid_a",
    "Big Wave Beach (Mid Wave B)": "beach/wave_mid_b",
    "Big Wave Beach (Final Wave)": "beach/wave_final",
    "Big Wave Beach (Victory!)": "beach/victory",
    "Big Wave Beach (The Zombies Ate Your Brains!)": "beach/defeat",
    "Big Wave Beach (Reward)": "beach/reward",
    "Dark Ages (Choose Your Seeds)": "dark_ages/choose_seeds",
    "Dark Ages (First Wave)": "dark_ages/wave_first",
    "Dark Ages (Mid Wave A)": "dark_ages/wave_mid_a",
    "Dark Ages (Mid Wave B)": "dark_ages/wave_mid_b",
    "Dark Ages (Final Wave)": "dark_ages/wave_final",
    "Dark Ages (Victory!)": "dark_ages/victory",
    "Dark Ages (The Zombies Ate Your Brains!)": "dark_ages/defeat",
    "Dark Ages (Reward)": "dark_ages/reward",
}


def _token(c: int, base: int) -> str:
    d = c % base
    tail = chr(d + 29) if d > 35 else B36[d]
    return _token(c // base, base) + tail if c >= base else tail


def _unpack_packer(packed: str, base: int, count: int, keywords: list[str]) -> str:
    out = packed
    c = count
    while c:
        c -= 1
        if keywords[c]:
            out = re.sub(r"\b" + re.escape(_token(c, base)) + r"\b", keywords[c], out)
    return out


def _fetch_album_html() -> str:
    result = subprocess.run(
        [
            "curl",
            "-fsSL",
            "-H",
            "User-Agent: Mozilla/5.0",
            ALBUM_URL,
        ],
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or "Failed to fetch album page")
    return result.stdout


def _parse_tracks(html: str) -> dict[str, str]:
    start = html.index("eval(function(p,a,c,k,e,d)")
    depth = 0
    end = start
    for i, ch in enumerate(html[start:], start):
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                end = i + 1
                break
    eval_call = html[start:end]
    pos = eval_call.find("return p}('") + len("return p}('")
    rest = eval_call[pos:]
    m = re.search(
        r"',\s*(\d+),\s*(\d+),\s*'(.+)'\s*\.split\('\|'\),\s*0,\s*\{\}\)\s*\)\s*$",
        rest,
        re.DOTALL,
    )
    if not m:
        raise RuntimeError("Could not parse Khinsider packer payload")
    packed = rest[: m.start()]
    base, count = int(m.group(1)), int(m.group(2))
    keywords = m.group(3).split("|")
    unpacked = _unpack_packer(packed, base, count, keywords)

    tracks: dict[str, str] = {}
    for name, file_path in re.findall(
        r'\{"track":\d+,"name":"((?:\\.|[^"\\])*)","length":"[^"]*","songid":"[^"]*","file":"((?:\\.|[^"\\])*)"\}',
        unpacked,
    ):
        clean_name = bytes(name, "utf-8").decode("unicode_escape")
        clean_file = bytes(file_path, "utf-8").decode("unicode_escape")
        tracks.setdefault(clean_name, clean_file)
    return tracks


def _download(url: str, dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    result = subprocess.run(
        [
            "curl",
            "-fsSL",
            "--retry",
            "5",
            "--retry-delay",
            "2",
            "-H",
            "User-Agent: Mozilla/5.0",
            "-H",
            f"Referer: {ALBUM_URL}",
            "-o",
            str(dest),
            url,
        ],
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or f"curl failed for {url}")
    if dest.stat().st_size < 100_000:
        dest.unlink(missing_ok=True)
        raise RuntimeError(f"Download too small ({dest.stat().st_size} bytes): {url}")


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    music_dir = root / "assets" / "music"

    print(f"Fetching album page: {ALBUM_URL}")
    html = _fetch_album_html()
    available = _parse_tracks(html)
    print(f"Parsed {len(available)} CDN tracks")

    ok = 0
    missing: list[str] = []
    for title, rel in TRACK_MAP.items():
        file_path = available.get(title)
        if not file_path:
            missing.append(title)
            continue
        url = "https://" + file_path
        dest = music_dir / f"{rel}.mp3"
        if dest.exists() and dest.stat().st_size > 100_000:
            print(f"skip  {rel}.mp3")
            ok += 1
            continue
        print(f"get   {title} -> {rel}.mp3")
        try:
            _download(url, dest)
            ok += 1
        except Exception as e:
            print(f"FAIL  {title}: {e}", file=sys.stderr)
            missing.append(title)

    print(f"\nDone: {ok}/{len(TRACK_MAP)} tracks in {music_dir}")
    if missing:
        print("Missing or failed:", file=sys.stderr)
        for t in missing:
            print(f"  - {t}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
