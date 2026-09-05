# Plants vs. Zombies 2 (PvZ 2)

[![Java](https://img.shields.io/badge/Java-25%20%2F%2021+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![libGDX](https://img.shields.io/badge/libGDX-1.14.2-E10098?style=for-the-badge&logo=libgdx&logoColor=white)](https://libgdx.com/)
[![Gradle](https://img.shields.io/badge/Gradle-9.6.1-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org/)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-4285F4?style=for-the-badge&logo=linux&logoColor=white)](#prerequisites)
[![Architecture](https://img.shields.io/badge/Architecture-Client--Server-00C7B7?style=for-the-badge)](#architecture)

An authentic, feature-rich desktop remake of PopCap's **Plants vs. Zombies 2**, featuring graphical (libGDX) and terminal (TUI) interfaces, animated plant and zombie models powered by `libPVZ` (PopCap Animation Module), multiple adventure worlds, a Zen Garden, quests and achievements, and a dedicated multi-threaded client-server networking backend for online multiplayer battles and leaderboards.

Developed as the term project for **Advanced Programming (Spring 2026 / 1404–1405)** at **Sharif University of Technology (SUT)**, Computer Engineering Department.  
Instructor: **Dr. Mohammad Amin Fazli**

---

## 👥 Authors & Team Members

| Name | Student ID |
| :--- | :---: |
| **Mohammad Rajaei** | `404105842` |
| **AmirHossein Qaraqani** | `404106225` |
| **Mohammad Matin Nameni** | `404106452` |

---

## 🏛️ System Architecture

The project is structured as a multi-module Gradle project following clean separation of concerns and the Model-View-Controller (MVC) pattern:

```text
PlantsVsZombies2/
├── core/         # Domain simulation models, game loop, lawn grid, combat logic,
│                 # controllers, scene2d GUI screens, network protocol DTOs, TUI
├── lwjgl3/       # Desktop client launcher using LWJGL3, windowing, and OpenGL
├── server/       # Dedicated multiplayer server handling user sessions, authentication,
│                 # synchronized game lobbies, leaderboards, and daily offers
├── assets/       # Internal textures, fonts, data JSONs, plus external music and libPVZ assets
└── tools/        # Python automation utilities for asset downloads, bundles, and setups
```

### High-Level Component Flow

```
   ┌────────────────────────────────────────────────────────┐
   │                Desktop Client (:lwjgl3)                │
   │               libGDX OpenGL Render Loop                │
   └───────────────────────────┬────────────────────────────┘
                               │ delegates to
   ┌───────────────────────────▼────────────────────────────┐
   │                   Core Engine (:core)                  │
   │  ┌─────────────────┐ ┌───────────────┐ ┌────────────┐  │
   │  │   Screens & UI  │ │ Simulation /  │ │   Audio    │  │
   │  │    (Scene2D)    │ │   Game Grid   │ │ (BGM/SFX)  │  │
   │  └────────┬────────┘ └───────┬───────┘ └────────────┘  │
   │           │                  │                         │
   │           │      TCP Packets │ (JSON / DTOs)           │
   └───────────┼──────────────────┼─────────────────────────┘
               │                  │
               │         Network Socket Connection
               │                  │
   ┌───────────▼──────────────────▼─────────────────────────┐
   │             Multiplayer Server (:server)               │
   │  ┌─────────────────┐ ┌───────────────┐ ┌────────────┐  │
   │  │ Session & Auth  │ │ Game Lobbies  │ │  Daily &   │  │
   │  │   Repository    │ │ & Matchmaking │ │Leaderboard │  │
   │  └─────────────────┘ └───────────────┘ └────────────┘  │
   └────────────────────────────────────────────────────────┘
```

---

## 📦 Prerequisites

Ensure you have the following installed on your system:

- **Java Development Kit (JDK)**: **Java 21 or Java 25** (Gradle toolchain will automatically resolve compatible JDKs via Foojay resolver).
- **Python**: **Python 3.9+** (used for downloading and installing binary game assets).
- **Git**: For version control.

---

## 🎨 Asset Setup (Required After Clone)

Large binary assets (graphics, animations, BGM, SFX) are git-ignored to keep repository sizes minimal and clone operations fast. Three dedicated Python automation scripts in `tools/` handle asset installation:

### 1. Base Game Assets (libPVZ PAM & Texture Atlases)

The high-resolution textures, PAM animation definitions, and `RESOURCES.json` catalog (~380 MB) are extracted into `assets/remote/`:

```bash
# Automatically download and extract into assets/remote/
python tools/download_assets.py
```

*Note: If you already have `pvz-assets.zip` on your machine (e.g. on your Desktop), the tool will detect it automatically, or you can specify it explicitly:*
```bash
python tools/download_assets.py --zip "/path/to/pvz-assets.zip"
```
*For detailed options and manual setup instructions, see [`assets/remote/README.md`](assets/remote/README.md).*

### 2. Background Music (OST / BGM)

Downloads all 36 original PvZ2 chapter and menu music tracks into `assets/music/`:

```bash
python tools/download_ost.py
```
*For track listings and playback details, see [`assets/music/README.md`](assets/music/README.md).*

### 3. Sound Effects (SFX)

Extracts the bundled sound effects archive into `assets/music/SFXs/`:

```bash
python tools/install_sfx.py
```
*For SFX manifests, see [`assets/music/SFXs/README.md`](assets/music/SFXs/README.md).*

---

## 🚀 Building & Running

Use the Gradle wrapper from the root of the project:

### 🎮 Running the Desktop GUI Client

Launch the graphical game client:

```bash
# Windows
.\gradlew.bat lwjgl3:run

# macOS / Linux
./gradlew lwjgl3:run
```

### 🌐 Running the Dedicated Multiplayer Server

Launch the dedicated server (binds to `0.0.0.0:8080` by default):

```bash
# Windows
.\gradlew.bat server:run

# macOS / Linux
./gradlew server:run
```

To run the server on a custom port:
```bash
./gradlew server:run -PserverPort=9090
```

### ⌨️ Running the Terminal UI (TUI)

Play the lightweight text/terminal interface (JLine 3):

```bash
# Windows
.\gradlew.bat core:runTui

# macOS / Linux
./gradlew core:runTui
```

### 🧪 Running Tests

Execute all unit and integration tests across all modules:

```bash
./gradlew test
```

### 📦 Building Production Executable JARs

Produce standalone runnable Fat JARs:

```bash
# Build desktop client and server JARs
./gradlew jar

# Client JAR: lwjgl3/build/libs/PlantsVsZombies2-1.0.0.jar
# Server JAR: server/build/libs/PlantsVsZombies2-server-1.0.0.jar
```

---

## ⚙️ Configuration (`gradle.properties`)

Key game and network settings can be adjusted in [`gradle.properties`](gradle.properties):

| Property | Default | Description |
| :--- | :---: | :--- |
| `pvz.assets` | `assets/remote` | Directory containing libPVZ atlases, animations, and `RESOURCES.json` |
| `pvz.resolution` | `768` | Base asset rendering resolution (`768` for high-res assets) |
| `pvz.server.host` | `127.0.0.1` | Multiplayer server bind host (`0.0.0.0` for LAN access) |
| `pvz.server.port` | `8080` | Multiplayer server TCP listening port |
| `pvz.client.host` | `127.0.0.1` | Server host address that game clients connect to |
| `pvz.client.port` | `8080` | Server port that game clients connect to |

---

## 🌟 Key Features

### 🌻 Adventure Mode & Eras
- **Ancient Egypt**: Classic lawn battles, tombstones, Ra Zombie, Camel Zombies, and Pharaoh Zombies.
- **Frostbite Caves**: Freezing winds, ice floes, warming plants, and sliding mechanics.
- **Big Wave Beach**: Tides, water lanes requiring Lily Pads, Snorkel Zombies, and Octopus attacks.
- **Dark Ages**: Nighttime gameplay without falling sun, Gravestones, Necromancy waves, and Jester Zombies.

### 🎮 Multiplayer & Minigames
- **Client-Server Architecture**: Dedicated server handling state synchronization, lobby management, and game rooms.
- **"I, Zombie" (من، زامبی)**: Play as the zombies against pre-planted defenses, strategically placing zombie units to reach the brains.
- **In-Game Reactions & Emotes**: Real-time communication during multiplayer matches.
- **Global Leaderboard**: Track highest scores, waves survived, and player rankings.

### 🏡 Metagame Systems
- **Greenhouse / Zen Garden**: Care for plants, water, fertilize, and harvest bonus coins and gems.
- **Almanac**: Detailed statistics, lore, damage profiles, and attack rates for all plants and zombies.
- **Quests & Achievements**: Daily objectives, progression challenges, and coin/gem rewards.
- **Daily Offers & Shop**: In-game storefront for unlocking plants, plant food upgrades, and perks.

---

## 🔍 Code Quality & Static Analysis

The project enforces clean code and style conventions via Checkstyle and PMD:

```bash
# Run Checkstyle inspections
./gradlew checkstyleMain

# Run PMD static code analysis
./gradlew pmdMain
```

Configuration files:
- Checkstyle: [`config/checkstyle/checkstyle.xml`](config/checkstyle/checkstyle.xml)
- PMD: [`config/pmd/pmd.xml`](config/pmd/pmd.xml)

---

## 📜 License & Credits

- Special thanks to **Arya Ghahremani** for recording and dubbing **Dr. Fazli**'s voice lines used in the game.
- Original *Plants vs. Zombies* game concepts, imagery, characters, and audio are trademarks and intellectual property of **Electronic Arts** and **PopCap Games**.
- Built with **[libGDX](https://libgdx.com/)**, **[libPVZ](https://github.com/pizpizi/libPVZ)**, **[pvz-skin](https://github.com/ahgharaghani/pvz-skin)**, and **[TenPatch](https://github.com/raeleus/TenPatch)**.
- Educational project developed for the Computer Engineering Department, Sharif University of Technology.
