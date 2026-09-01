#!/usr/bin/env python3
"""Extract the bundled SFX zip into assets/music/SFXs/. Local dev only."""

from __future__ import annotations

import argparse
import sys
import zipfile
from pathlib import Path

from sfx_manifest import DEFAULT_ZIP, WIRED_SFX


def main() -> int:
    parser = argparse.ArgumentParser(description="Install PvZ2 SFX from tools/sfx-bundle.zip")
    parser.add_argument(
        "--zip",
        default=DEFAULT_ZIP,
        help=f"Bundle zip path (default: {DEFAULT_ZIP})",
    )
    args = parser.parse_args()

    root = Path(__file__).resolve().parents[1]
    zip_path = root / args.zip if not Path(args.zip).is_absolute() else Path(args.zip)
    music_dir = root / "assets" / "music"

    if not zip_path.is_file():
        print(f"Missing bundle: {zip_path}", file=sys.stderr)
        print("Ask a teammate for tools/sfx-bundle.zip or rebuild with:", file=sys.stderr)
        print("  python3 tools/build_sfx_bundle.py", file=sys.stderr)
        return 1

    print(f"Extracting {zip_path} -> {music_dir}/")
    with zipfile.ZipFile(zip_path) as archive:
        bad = [name for name in archive.namelist() if name.startswith("/") or ".." in Path(name).parts]
        if bad:
            print("Unsafe zip entry:", bad[0], file=sys.stderr)
            return 1
        archive.extractall(music_dir)

    missing: list[str] = []
    for rel in WIRED_SFX:
        dest = music_dir / rel
        if not dest.is_file() or dest.stat().st_size < 100:
            missing.append(rel)

    if missing:
        print("Installed, but these wired files are still missing or too small:", file=sys.stderr)
        for path in missing:
            print(f"  - {path}", file=sys.stderr)
        return 1

    print(f"Done: {len(WIRED_SFX)} SFX files ready under assets/music/SFXs/")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
