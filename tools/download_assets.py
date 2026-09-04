#!/usr/bin/env python3
"""Download, extract, and configure PvZ2 base assets into assets/remote/.

This tool acquires the base game assets required by libPVZ (PAM animations,
texture atlases, spritesheets, and RESOURCES.json), unzips them into assets/remote/,
and points `pvz.assets` in gradle.properties to the assets folder.

Default download URL:
    https://my.sharif.edu/s/FzjnEi8RPdx9dfX/download/pvz-assets.zip
"""

from __future__ import annotations

import argparse
import os
import re
import shutil
import sys
import time
import urllib.error
import urllib.request
import zipfile
from pathlib import Path

DEFAULT_URL = "https://my.sharif.edu/s/FzjnEi8RPdx9dfX/download/pvz-assets.zip"
DEFAULT_REL_DEST = Path("assets") / "remote"
EXPECTED_ENTRIES = ("RESOURCES.json", "resources.json")


def _find_candidate_local_zips() -> list[Path]:
    """Search common local paths where developers might already have pvz-assets.zip."""
    candidates = [
        Path.home() / "Desktop" / "pvz-assets.zip",
        Path.home() / "Downloads" / "pvz-assets.zip",
        Path.cwd() / "pvz-assets.zip",
        Path(__file__).resolve().parent / "pvz-assets.zip",
        # Common Windows locations
        Path(r"C:\Users\ahgha\Desktop\pvz-assets.zip"),
    ]
    seen: set[Path] = set()
    found: list[Path] = []
    for c in candidates:
        try:
            resolved = c.resolve()
        except Exception:
            continue
        if resolved not in seen and resolved.is_file():
            seen.add(resolved)
            found.append(resolved)
    return found


def _format_size(bytes_num: int | float) -> str:
    """Format byte counts into human-readable strings."""
    for unit in ("B", "KB", "MB", "GB"):
        if abs(bytes_num) < 1024.0:
            return f"{bytes_num:3.1f} {unit}"
        bytes_num /= 1024.0
    return f"{bytes_num:.1f} TB"


def download_file(url: str, dest: Path) -> None:
    """Download a file with a dynamic terminal progress indicator."""
    dest.parent.mkdir(parents=True, exist_ok=True)
    temp_dest = dest.with_suffix(".download.tmp")

    print(f"Connecting to: {url}")
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) PvZ2-Asset-Downloader"},
    )

    try:
        with urllib.request.urlopen(req) as response, open(temp_dest, "wb") as out_file:
            total_size = int(response.headers.get("Content-Length", 0))
            downloaded = 0
            chunk_size = 1024 * 512  # 512 KB chunks
            start_time = time.time()
            last_print_time = 0.0

            while True:
                chunk = response.read(chunk_size)
                if not chunk:
                    break
                out_file.write(chunk)
                downloaded += len(chunk)

                current_time = time.time()
                if current_time - last_print_time >= 0.25 or downloaded == total_size:
                    last_print_time = current_time
                    elapsed = max(current_time - start_time, 0.001)
                    speed = downloaded / elapsed
                    if total_size > 0:
                        pct = (downloaded / total_size) * 100
                        eta = (total_size - downloaded) / speed if speed > 0 else 0
                        status = (
                            f"\rDownloading: {pct:5.1f}% | {_format_size(downloaded)} / "
                            f"{_format_size(total_size)} | {_format_size(speed)}/s | ETA: {int(eta)}s"
                        )
                    else:
                        status = f"\rDownloading: {_format_size(downloaded)} | {_format_size(speed)}/s"
                    sys.stdout.write(status)
                    sys.stdout.flush()

        print()  # newline after progress
        if temp_dest.exists():
            if dest.exists():
                dest.unlink()
            temp_dest.rename(dest)
    except Exception:
        if temp_dest.exists():
            temp_dest.unlink()
        raise


def extract_assets(zip_path: Path, dest_dir: Path) -> None:
    """Safely extract archive into destination directory."""
    dest_dir.mkdir(parents=True, exist_ok=True)
    print(f"Extracting {zip_path.name} -> {dest_dir}/ ...")

    with zipfile.ZipFile(zip_path) as archive:
        entries = archive.infolist()
        total = len(entries)
        # Zip slip protection check
        for entry in entries:
            name = entry.filename
            if name.startswith(("/", "\\")) or ".." in Path(name).parts:
                raise RuntimeError(f"Unsafe zip entry detected: {name}")

        last_print = 0.0
        start_time = time.time()
        for idx, entry in enumerate(entries, start=1):
            archive.extract(entry, dest_dir)
            current_time = time.time()
            if current_time - last_print >= 0.25 or idx == total:
                last_print = current_time
                pct = (idx / total) * 100
                sys.stdout.write(f"\rExtracting files: {idx}/{total} ({pct:.1f}%)")
                sys.stdout.flush()

        print(f"\nExtracted {total} files in {time.time() - start_time:.2f}s")


def verify_extracted_assets(dest_dir: Path) -> bool:
    """Verify essential asset root markers exist."""
    found_resource_descriptor = any((dest_dir / r).is_file() for r in EXPECTED_ENTRIES)
    if not found_resource_descriptor:
        print(f"Warning: Neither RESOURCES.json nor resources.json found under {dest_dir}", file=sys.stderr)
        return False

    atlases_dir = dest_dir / "ATLASES"
    images_dir = dest_dir / "IMAGES"
    if not atlases_dir.is_dir() and not (dest_dir / "atlases").is_dir():
        print(f"Warning: ATLASES directory not found under {dest_dir}", file=sys.stderr)
        return False

    return True


def update_gradle_properties(project_root: Path, asset_prop_value: str = "assets/remote") -> bool:
    """Update pvz.assets in gradle.properties to point to asset_prop_value."""
    gradle_props = project_root / "gradle.properties"
    if not gradle_props.is_file():
        print(f"Notice: {gradle_props} not found; skipping properties update.")
        return False

    content = gradle_props.read_text(encoding="utf-8")
    asset_line = f"pvz.assets={asset_prop_value}"

    pattern = re.compile(r"^[ \t]*pvz\.assets[ \t]*=.*$", re.MULTILINE)
    if pattern.search(content):
        new_content = pattern.sub(asset_line, content)
    else:
        new_content = content.rstrip() + f"\n\n# Extracted PvZ2 Base Assets root for libPVZ\n{asset_line}\n"

    if new_content != content:
        gradle_props.write_text(new_content, encoding="utf-8")
        print(f"Updated {gradle_props.name}: {asset_line}")
    else:
        print(f"Already up-to-date in {gradle_props.name}: {asset_line}")
    return True


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Download and extract PvZ2 game assets into assets/remote/ and configure gradle.properties."
    )
    parser.add_argument(
        "--zip",
        type=str,
        default=None,
        help="Path to an existing pvz-assets.zip file (skips downloading if provided)",
    )
    parser.add_argument(
        "--url",
        type=str,
        default=DEFAULT_URL,
        help=f"Asset bundle download URL (default: {DEFAULT_URL})",
    )
    parser.add_argument(
        "--dest",
        type=str,
        default=None,
        help="Destination directory for extracted assets (default: assets/remote)",
    )
    parser.add_argument(
        "--download",
        "--force-download",
        action="store_true",
        dest="force_download",
        help="Force downloading from URL even if an existing local zip archive is found",
    )
    parser.add_argument(
        "--keep-zip",
        action="store_true",
        help="Keep downloaded zip file in the destination or temp folder instead of cleaning it up",
    )
    parser.add_argument(
        "--no-gradle-update",
        action="store_true",
        help="Skip updating pvz.assets in gradle.properties",
    )

    args = parser.parse_args()

    # Project root is parent of the tools/ directory
    project_root = Path(__file__).resolve().parents[1]
    dest_dir = Path(args.dest).resolve() if args.dest else (project_root / DEFAULT_REL_DEST)

    zip_file: Path | None = None

    if args.zip:
        specified = Path(args.zip).resolve()
        if specified.is_file():
            zip_file = specified
        else:
            print(f"Error: Specified zip file does not exist: {specified}", file=sys.stderr)
            return 1
    elif not args.force_download:
        candidates = _find_candidate_local_zips()
        if candidates:
            zip_file = candidates[0]
            print(f"Found existing local asset archive: {zip_file}")
            print("Tip: Pass --download to force re-downloading from remote server.")

    is_downloaded_temp = False
    if zip_file is None:
        target_download_path = project_root / "tools" / "pvz-assets.zip"
        print(f"No existing local zip selected. Downloading from:\n  {args.url}")
        try:
            download_file(args.url, target_download_path)
            zip_file = target_download_path
            is_downloaded_temp = True
        except Exception as e:
            print(f"\nDownload failed: {e}", file=sys.stderr)
            return 1

    try:
        extract_assets(zip_file, dest_dir)
    except Exception as e:
        print(f"\nExtraction failed: {e}", file=sys.stderr)
        return 1
    finally:
        if is_downloaded_temp and not args.keep_zip:
            try:
                if zip_file and zip_file.is_file():
                    zip_file.unlink()
                    print("Cleaned up temporary download archive.")
            except Exception:
                pass

    if not verify_extracted_assets(dest_dir):
        print("Warning: Asset validation completed with warnings. Check output above.", file=sys.stderr)
    else:
        print("Asset verification passed successfully.")

    if not args.no_gradle_update:
        # Determine relative property value if inside project root, otherwise use forward-slash path
        try:
            rel = dest_dir.relative_to(project_root).as_posix()
        except ValueError:
            rel = dest_dir.as_posix()
        update_gradle_properties(project_root, rel)

    print("\nAssets setup completed successfully!")
    print(f"Extracted directory : {dest_dir}")
    print(f"Configured in gradle: pvz.assets={dest_dir.relative_to(project_root).as_posix() if dest_dir.is_relative_to(project_root) else dest_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
