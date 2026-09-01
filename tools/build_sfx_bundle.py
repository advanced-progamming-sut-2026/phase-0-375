#!/usr/bin/env python3
"""Pack wired SFX from assets/music/SFXs/ into tools/sfx-bundle.zip (maintainers)."""

from __future__ import annotations

import sys
import zipfile
from pathlib import Path

from sfx_manifest import DEFAULT_ZIP, WIRED_SFX


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    music_dir = root / "assets" / "music"
    zip_path = root / DEFAULT_ZIP

    missing: list[str] = []
    for rel in WIRED_SFX:
        if not (music_dir / rel).is_file():
            missing.append(rel)
    if missing:
        print("Cannot build bundle — missing source files:", file=sys.stderr)
        for path in missing:
            print(f"  - assets/music/{path}", file=sys.stderr)
        return 1

    zip_path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for rel in WIRED_SFX:
            src = music_dir / rel
            archive.write(src, rel)
            print(f"  add  {rel}")

    size_kb = zip_path.stat().st_size / 1024
    print(f"\nWrote {zip_path} ({size_kb:.1f} KB, {len(WIRED_SFX)} files)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
