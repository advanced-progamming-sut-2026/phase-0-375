#!/usr/bin/env python3
"""Pack and install game image/atlas assets for teammates.

Cross-platform (Windows / Linux / macOS). Requires Python 3.8+.

Typical workflow
----------------
Creator (has the assets locally)::

    python tools/asset_pack.py pack cattail --from assets -o cattail-assets.zip

Recipient (points at their extracted Base Assets / project assets root)::

    python tools/asset_pack.py install cattail-assets.zip --to /path/to/assets

Or install a named pack from tools/asset-packs/<name>/ after packing into
that folder::

    python tools/asset_pack.py pack cattail --from assets -o tools/asset-packs/cattail
    python tools/asset_pack.py install cattail --to /path/to/assets

A pack is either:
  * a zip / folder containing ``manifest.json`` + mirrored ``IMAGES/...`` (and
    optionally ``ATLASES/...``) paths, or
  * a folder/zip that already mirrors ``IMAGES/...`` with no manifest
    (everything under IMAGES/ATLASES is copied).

Asset roots are detected by looking for ``RESOURCES.json`` /
``resources.json``, ``animations.json``, and ``IMAGES``/``images`` or
``ATLASES``/``atlases``. Destination casing for ``IMAGES`` vs ``images``
follows whatever already exists on disk.
"""

from __future__ import annotations

import argparse
import fnmatch
import json
import shutil
import sys
import tempfile
import zipfile
from pathlib import Path
from typing import Iterable, Sequence

SCRIPT_DIR = Path(__file__).resolve().parent
PACKS_DIR = SCRIPT_DIR / "asset-packs"
ROOT_MARKERS = (
    "RESOURCES.json",
    "resources.json",
    "animations.json",
    "IMAGES",
    "images",
    "ATLASES",
    "atlases",
)
TOP_LEVEL_PREFIXES = ("IMAGES", "ATLASES", "images", "atlases")


# ---------------------------------------------------------------------------
# Path helpers
# ---------------------------------------------------------------------------

def die(message: str, code: int = 1) -> None:
    print(f"error: {message}", file=sys.stderr)
    raise SystemExit(code)


def normalize_rel(path: str | Path) -> str:
    return str(path).replace("\\", "/").strip("/")


def path_parts(rel: str) -> list[str]:
    return [p for p in normalize_rel(rel).split("/") if p and p != "."]


def looks_like_assets_root(path: Path) -> bool:
    if not path.is_dir():
        return False
    return any((path / name).exists() for name in ROOT_MARKERS)


def resolve_case_dir(parent: Path, *candidates: str) -> Path | None:
    """Return the first existing child among candidates (exact, then scan)."""
    for name in candidates:
        candidate = parent / name
        if candidate.exists():
            return candidate
    if not parent.is_dir():
        return None
    lower_map = {child.name.lower(): child for child in parent.iterdir()}
    for name in candidates:
        hit = lower_map.get(name.lower())
        if hit is not None:
            return hit
    return None


def images_dir_name(assets_root: Path) -> str:
    existing = resolve_case_dir(assets_root, "IMAGES", "images")
    return existing.name if existing is not None else "IMAGES"


def atlases_dir_name(assets_root: Path) -> str:
    existing = resolve_case_dir(assets_root, "ATLASES", "atlases")
    return existing.name if existing is not None else "ATLASES"


def remap_pack_rel_for_dest(rel: str, assets_root: Path) -> str:
    """Map pack-relative path onto the destination's IMAGES/ATLASES casing."""
    parts = path_parts(rel)
    if not parts:
        return rel
    head = parts[0].upper()
    if head == "IMAGES":
        parts[0] = images_dir_name(assets_root)
    elif head == "ATLASES":
        parts[0] = atlases_dir_name(assets_root)
    return "/".join(parts)


def find_source_entry(assets_root: Path, rel: str) -> Path | None:
    """Locate ``rel`` under assets_root, tolerating IMAGES/images casing."""
    parts = path_parts(rel)
    if not parts:
        return None
    head = parts[0].upper()
    if head == "IMAGES":
        base = resolve_case_dir(assets_root, "IMAGES", "images")
        rest = parts[1:]
    elif head == "ATLASES":
        base = resolve_case_dir(assets_root, "ATLASES", "atlases")
        rest = parts[1:]
    else:
        base = assets_root
        rest = parts
    if base is None:
        return None
    current = base
    for part in rest:
        nxt = current / part
        if nxt.exists():
            current = nxt
            continue
        if not current.is_dir():
            return None
        lower_map = {child.name.lower(): child for child in current.iterdir()}
        hit = lower_map.get(part.lower())
        if hit is None:
            return None
        current = hit
    return current


# ---------------------------------------------------------------------------
# Manifest / exclude
# ---------------------------------------------------------------------------

def load_manifest(path: Path) -> dict:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        die(f"cannot read manifest {path}: {exc}")
    if not isinstance(data, dict) or "entries" not in data:
        die(f"manifest {path} must be a JSON object with an 'entries' array")
    return data


def is_excluded(rel_from_entry: str, patterns: Sequence[str]) -> bool:
    name = Path(rel_from_entry).name
    norm = normalize_rel(rel_from_entry)
    for pattern in patterns:
        pat = pattern.replace("\\", "/")
        if fnmatch.fnmatch(name, pat) or fnmatch.fnmatch(norm, pat):
            return True
        if "/" not in pat.rstrip("/") and fnmatch.fnmatch(name, pat):
            return True
    return False


def iter_files_under(root: Path) -> Iterable[Path]:
    for path in sorted(root.rglob("*")):
        if path.is_file():
            yield path


def collect_from_manifest(assets_root: Path, manifest: dict) -> list[tuple[str, Path]]:
    """Return list of (pack_relative_posix_path, absolute_source_file)."""
    collected: list[tuple[str, Path]] = []
    seen: set[str] = set()
    for entry in manifest.get("entries", []):
        if isinstance(entry, str):
            entry = {"path": entry}
        if not isinstance(entry, dict) or "path" not in entry:
            die(f"invalid manifest entry: {entry!r}")
        rel = normalize_rel(entry["path"])
        excludes = [str(x) for x in entry.get("exclude", [])]
        source = find_source_entry(assets_root, rel)
        if source is None:
            die(f"missing source entry '{rel}' under {assets_root}")
        if source.is_file():
            pack_rel = rel
            if pack_rel.upper().startswith("IMAGES/"):
                pack_rel = "IMAGES/" + pack_rel[7:]
            elif pack_rel.upper().startswith("ATLASES/"):
                pack_rel = "ATLASES/" + pack_rel[8:]
            if not is_excluded(Path(pack_rel).name, excludes) and pack_rel not in seen:
                collected.append((pack_rel, source))
                seen.add(pack_rel)
            continue
        if not source.is_dir():
            die(f"entry '{rel}' is neither a file nor a directory: {source}")

        # Canonicalize pack prefix to IMAGES/ or ATLASES/
        parts = path_parts(rel)
        if parts and parts[0].upper() == "IMAGES":
            pack_prefix = "IMAGES/" + "/".join(parts[1:])
        elif parts and parts[0].upper() == "ATLASES":
            pack_prefix = "ATLASES/" + "/".join(parts[1:])
        else:
            pack_prefix = "/".join(parts)

        for file_path in iter_files_under(source):
            inner = normalize_rel(file_path.relative_to(source))
            if is_excluded(inner, excludes):
                continue
            pack_rel = f"{pack_prefix}/{inner}" if pack_prefix else inner
            if pack_rel in seen:
                continue
            collected.append((pack_rel, file_path))
            seen.add(pack_rel)
    return collected


def collect_mirrored_tree(pack_root: Path) -> list[tuple[str, Path]]:
    """Collect IMAGES/ATLASES trees already present under pack_root."""
    collected: list[tuple[str, Path]] = []
    seen_roots: set[Path] = set()
    for canon, candidates in (
        ("IMAGES", ("IMAGES", "images")),
        ("ATLASES", ("ATLASES", "atlases")),
    ):
        base = resolve_case_dir(pack_root, *candidates)
        if base is None or not base.is_dir():
            continue
        resolved = base.resolve()
        if resolved in seen_roots:
            continue
        seen_roots.add(resolved)
        for file_path in iter_files_under(base):
            inner = normalize_rel(file_path.relative_to(base))
            collected.append((f"{canon}/{inner}", file_path))
    return collected

# ---------------------------------------------------------------------------
# Pack I/O (zip / folder)
# ---------------------------------------------------------------------------

def write_pack_folder(
    out_dir: Path,
    files: Sequence[tuple[str, Path]],
    manifest: dict | None,
) -> None:
    out_dir.mkdir(parents=True, exist_ok=True)
    if manifest is not None:
        (out_dir / "manifest.json").write_text(
            json.dumps(manifest, indent=2) + "\n",
            encoding="utf-8",
        )
    for pack_rel, src in files:
        dest = out_dir.joinpath(*path_parts(pack_rel))
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dest)


def write_pack_zip(
    out_zip: Path,
    files: Sequence[tuple[str, Path]],
    manifest: dict | None,
) -> None:
    out_zip.parent.mkdir(parents=True, exist_ok=True)
    if out_zip.exists():
        out_zip.unlink()
    with zipfile.ZipFile(out_zip, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        if manifest is not None:
            zf.writestr(
                "manifest.json",
                json.dumps(manifest, indent=2) + "\n",
            )
        for pack_rel, src in files:
            zf.write(src, arcname=normalize_rel(pack_rel))


def open_pack_as_dir(pack: Path) -> tuple[Path, Path | None]:
    """Return (pack_dir, temp_dir_to_cleanup_or_None)."""
    if pack.is_dir():
        return pack, None
    if pack.is_file() and zipfile.is_zipfile(pack):
        tmp = Path(tempfile.mkdtemp(prefix="asset_pack_"))
        with zipfile.ZipFile(pack, "r") as zf:
            zf.extractall(tmp)
        return tmp, tmp
    die(f"pack must be a directory or zip file: {pack}")


def resolve_named_pack(name: str) -> Path:
    candidate = PACKS_DIR / name
    if candidate.is_dir():
        return candidate
    zip_candidate = PACKS_DIR / f"{name}.zip"
    if zip_candidate.is_file():
        return zip_candidate
    # Also allow bare path-like names relative to cwd
    path = Path(name)
    if path.exists():
        return path
    die(
        f"unknown pack '{name}'. Expected {candidate} or a path/zip. "
        f"Available: {', '.join(list_pack_names()) or '(none)'}"
    )


def list_pack_names() -> list[str]:
    if not PACKS_DIR.is_dir():
        return []
    names: list[str] = []
    for child in sorted(PACKS_DIR.iterdir()):
        if child.is_dir() and (child / "manifest.json").is_file():
            names.append(child.name)
        elif child.suffix.lower() == ".zip":
            names.append(child.stem)
    return names


# ---------------------------------------------------------------------------
# Commands
# ---------------------------------------------------------------------------

def cmd_list(_args: argparse.Namespace) -> None:
    names = list_pack_names()
    if not names:
        print(f"No packs found under {PACKS_DIR}")
        return
    for name in names:
        manifest_path = PACKS_DIR / name / "manifest.json"
        if manifest_path.is_file():
            manifest = load_manifest(manifest_path)
            desc = manifest.get("description") or ""
            ver = manifest.get("version") or ""
            extra = f"  ({ver})" if ver else ""
            print(f"{name}{extra}" + (f" — {desc}" if desc else ""))
        else:
            print(name)


def cmd_pack(args: argparse.Namespace) -> None:
    pack_name = args.pack
    assets_root = Path(args.source).expanduser().resolve()
    if not looks_like_assets_root(assets_root):
        die(
            f"{assets_root} does not look like an assets root "
            f"(need RESOURCES.json / IMAGES / ATLASES / animations.json)"
        )

    manifest_path = PACKS_DIR / pack_name / "manifest.json"
    if not manifest_path.is_file():
        die(f"missing manifest: {manifest_path}")
    manifest = load_manifest(manifest_path)

    files = collect_from_manifest(assets_root, manifest)
    if not files:
        die("pack would be empty — check manifest paths / excludes")

    out = Path(args.output).expanduser() if args.output else Path(f"{pack_name}-assets.zip")
    out = out.resolve()

    print(f"Packing '{pack_name}' from {assets_root}")
    for pack_rel, src in files:
        print(f"  + {pack_rel}  <- {src}")

    if args.dry_run:
        print(f"dry-run: would write {len(files)} file(s) to {out}")
        return

    if out.suffix.lower() == ".zip" or str(out).lower().endswith(".zip"):
        write_pack_zip(out, files, manifest)
    else:
        write_pack_folder(out, files, manifest)

    print(f"Wrote {len(files)} file(s) -> {out}")


def cmd_install(args: argparse.Namespace) -> None:
    pack_arg = args.pack
    dest_root = Path(args.dest).expanduser().resolve()
    if not looks_like_assets_root(dest_root):
        die(
            f"{dest_root} does not look like an assets root "
            f"(need RESOURCES.json / IMAGES / ATLASES / animations.json)"
        )

    pack_path = Path(pack_arg).expanduser()
    if not pack_path.exists():
        pack_path = resolve_named_pack(pack_arg)
    else:
        pack_path = pack_path.resolve()

    pack_dir, tmp = open_pack_as_dir(pack_path)
    try:
        manifest_file = pack_dir / "manifest.json"
        if manifest_file.is_file():
            manifest = load_manifest(manifest_file)
            # Prefer files already embedded in the pack; fall back to collecting
            # via destination? No — install from pack payload only.
            embedded = collect_mirrored_tree(pack_dir)
            if embedded:
                # Apply exclude rules from manifest when present
                exclude_by_prefix: list[tuple[str, list[str]]] = []
                for entry in manifest.get("entries", []):
                    if isinstance(entry, str):
                        entry = {"path": entry}
                    prefix = normalize_rel(entry.get("path", ""))
                    # canonicalize prefix head
                    parts = path_parts(prefix)
                    if parts and parts[0].lower() == "images":
                        prefix = "IMAGES/" + "/".join(parts[1:])
                    elif parts and parts[0].lower() == "atlases":
                        prefix = "ATLASES/" + "/".join(parts[1:])
                    exclude_by_prefix.append((prefix.rstrip("/"), [str(x) for x in entry.get("exclude", [])]))

                files: list[tuple[str, Path]] = []
                for pack_rel, src in embedded:
                    skip = False
                    for prefix, excludes in exclude_by_prefix:
                        if pack_rel == prefix or pack_rel.startswith(prefix + "/"):
                            inner = pack_rel[len(prefix) :].lstrip("/")
                            if excludes and is_excluded(inner or Path(pack_rel).name, excludes):
                                skip = True
                                break
                    if not skip:
                        files.append((pack_rel, src))
            else:
                # Manifest-only pack: gather from --from if provided
                if not args.source:
                    die(
                        f"pack {pack_path} has a manifest but no IMAGES/ATLASES "
                        f"payload. Re-pack with 'pack', or pass --from <assets>."
                    )
                source_root = Path(args.source).expanduser().resolve()
                files = collect_from_manifest(source_root, manifest)
        else:
            files = collect_mirrored_tree(pack_dir)
            if not files:
                die(
                    f"no IMAGES/ATLASES content found in {pack_path}. "
                    "Add a mirrored tree or a manifest.json."
                )

        planned: list[tuple[Path, Path]] = []
        for pack_rel, src in files:
            dest_rel = remap_pack_rel_for_dest(pack_rel, dest_root)
            dest = dest_root.joinpath(*path_parts(dest_rel))
            planned.append((src, dest))

        print(f"Installing into {dest_root}")
        for src, dest in planned:
            status = "overwrite" if dest.exists() else "create"
            print(f"  [{status}] {dest.relative_to(dest_root)}  <- {src.name}")

        if args.dry_run:
            print(f"dry-run: would install {len(planned)} file(s)")
            return

        for src, dest in planned:
            if dest.exists() and not args.force:
                # Default: overwrite image assets; --force is reserved for clarity
                # but we always replace to keep installs idempotent.
                pass
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(src, dest)

        print(f"Installed {len(planned)} file(s).")
    finally:
        if tmp is not None:
            shutil.rmtree(tmp, ignore_errors=True)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Pack / install Plants vs Zombies image assets for teammates.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    sub = parser.add_subparsers(dest="command", required=True)

    p_list = sub.add_parser("list", help="List known packs under tools/asset-packs/")
    p_list.set_defaults(func=cmd_list)

    p_pack = sub.add_parser(
        "pack",
        help="Build a distributable zip/folder from a named manifest + local assets",
    )
    p_pack.add_argument("pack", help="Pack name (folder under tools/asset-packs/)")
    p_pack.add_argument(
        "--from",
        dest="source",
        required=True,
        help="Source assets root (folder with IMAGES/, RESOURCES.json, …)",
    )
    p_pack.add_argument(
        "-o",
        "--output",
        help="Output .zip path or directory (default: <pack>-assets.zip)",
    )
    p_pack.add_argument(
        "-n",
        "--dry-run",
        action="store_true",
        help="Show what would be packed without writing",
    )
    p_pack.set_defaults(func=cmd_pack)

    p_install = sub.add_parser(
        "install",
        help="Install a pack (name, folder, or zip) into an assets root",
    )
    p_install.add_argument(
        "pack",
        help="Pack name, zip path, or folder containing IMAGES/… (+ optional manifest)",
    )
    p_install.add_argument(
        "--to",
        dest="dest",
        required=True,
        help="Destination assets root (recipient's IMAGES/RESOURCES.json folder)",
    )
    p_install.add_argument(
        "--from",
        dest="source",
        help="Only for manifest-only packs: gather files from this assets root",
    )
    p_install.add_argument(
        "-n",
        "--dry-run",
        action="store_true",
        help="Show what would be installed without writing",
    )
    p_install.add_argument(
        "-f",
        "--force",
        action="store_true",
        help="Overwrite existing files (default already overwrites; kept for clarity)",
    )
    p_install.set_defaults(func=cmd_install)

    return parser


def main(argv: Sequence[str] | None = None) -> None:
    parser = build_parser()
    args = parser.parse_args(argv)
    args.func(args)


if __name__ == "__main__":
    main()
