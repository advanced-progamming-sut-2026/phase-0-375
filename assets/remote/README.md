# Remote Base Assets (`assets/remote/`)

This directory contains the large binary assets required by the game engine and [libPVZ](https://github.com/pizpizi/libPVZ) runtime (texture atlases, PopCap Animation Module / PAM definitions, spritesheets, and resource catalogs).

> **Note**: Due to size considerations (~380 MB compressed, ~470 MB uncompressed across 3,775+ files), binary assets in this directory are excluded from Git version control. Only this `README.md` file is tracked in the repository.

---

## Directory Structure

When properly extracted, `assets/remote/` should contain the following layout:

```text
assets/remote/
├── ATLASES/              # Texture atlas pages (e.g., ALWAYSLOADED_768_00.PNG, etc.)
├── IMAGES/               # High-resolution standalone images and spritesheets
├── animations.json       # Compiled PAM animation metadata and clip registries
├── RESOURCES.json        # Main libPVZ resource descriptor mapping sprites, animations, and pools
└── README.md             # This installation guide
```

---

## Installation Methods

### Method 1: Automated Setup via Python Tool (Recommended)

From the **project root directory**, run:

```bash
# On Windows / PowerShell
python tools/download_assets.py

# On macOS / Linux
python3 tools/download_assets.py
```

#### What the script does:
1. Checks for an existing local copy of `pvz-assets.zip` (on Desktop, Downloads, or working directory).
2. If no local copy is found, downloads the archive directly from the Sharif storage server:
   `https://my.sharif.edu/s/FzjnEi8RPdx9dfX/download/pvz-assets.zip`
3. Extracts all contents directly into `assets/remote/`.
4. Verifies the integrity of `RESOURCES.json`, `animations.json`, `ATLASES/`, and `IMAGES/`.
5. Automatically sets `pvz.assets=assets/remote` in `gradle.properties`.

---

### Method 2: Using an Existing Local Zip Archive

If you already downloaded `pvz-assets.zip` previously, pass the `--zip` parameter to skip downloading:

```bash
# Example with Desktop path
python tools/download_assets.py --zip "C:\Users\ahgha\Desktop\pvz-assets.zip"

# Or relative path
python tools/download_assets.py --zip pvz-assets.zip
```

#### Available CLI Options:
| Flag | Description | Default |
|------|-------------|---------|
| `--zip PATH` | Use a local zip file instead of downloading | Auto-detect |
| `--download` / `--force-download` | Force remote download even if local zip is found | False |
| `--dest DIR` | Target extract destination directory | `assets/remote` |
| `--url URL` | Custom download URL | Sharif storage URL |
| `--keep-zip` | Retain downloaded archive file | False |
| `--no-gradle-update` | Skip modifying `gradle.properties` | False |

---

### Method 3: Manual Installation

1. Download the archive directly in your browser:
   [https://my.sharif.edu/s/FzjnEi8RPdx9dfX/download/pvz-assets.zip](https://my.sharif.edu/s/FzjnEi8RPdx9dfX/download/pvz-assets.zip)
2. Open the downloaded `pvz-assets.zip` file.
3. Extract all folders and files directly into `assets/remote/` such that `RESOURCES.json` is located at `assets/remote/RESOURCES.json` (not inside a nested subfolder).
4. Verify that `gradle.properties` contains:
   ```properties
   pvz.assets=assets/remote
   pvz.resolution=768
   ```

---

## Verification & Testing

To verify the setup, run the desktop client or check asset resolution:

```bash
# Launch the desktop client
./gradlew lwjgl3:run
```

If assets are loaded correctly, you will see the PopCap / PvZ2 splash screens, main menu buttons, animated plant and zombie avatars, and high-resolution textures.

---

## Troubleshooting

### Error: `IllegalStateException: No RESOURCES.json / resources.json under pvz.assets`
- Cause: The zip archive was extracted into an extra nested folder (for example, `assets/remote/pvz-assets/RESOURCES.json`).
- Fix: Move the contents of the nested folder up into `assets/remote/` directly so that `assets/remote/RESOURCES.json` exists.

### Error: `pvz.assets path does not exist or is not a directory`
- Cause: `gradle.properties` points to a path that does not exist on your computer.
- Fix: Set `pvz.assets=assets/remote` in `gradle.properties` or provide an absolute path to the directory where the assets were extracted.
