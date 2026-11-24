# Wildercord Bedrock & Console Crossplay Guide (Java 26.3 + Bedrock / Xbox / PlayStation / Switch)

Wildercord supports full Java + Bedrock crossplay on the same Fabric `26.3` server using a dual-side translation architecture:

1. **Bedrock Resource Pack & Add-on (`tools/bedrock_export.py`)**
   - `artifacts/bedrock/wildercord-bedrock-rp.mcpack` (`~16 MB`, auto-pushed by Geyser to PC, Mobile, Xbox, PlayStation, and Switch players — no manual console install needed).
   - `artifacts/bedrock/wildercord-bedrock.mcaddon` (Resource Pack + Behavior Pack bundle for Bedrock players who also want to install/preview locally).
2. **Fabric Server-Side Bedrock Bridge (`dev.wildercord.bedrock`)**
   - Bypasses Fabric Registry Sync disconnects for Floodgate/Geyser sessions while keeping Java Registry Sync enforced for Java clients.
   - Rewrites outbound packets (items, per-rune `wildercord:rune_*` models, blocks, chunk palettes, entities, custom codecs/particles, block entities, and menus) strictly for Bedrock recipients so Geyser never disconnects on unknown modded IDs.
   - Provides controller- and touch-friendly **Cumulus Forms** (`/wc`) plus chest menu bridges.
3. **Wildercord Geyser Translator Extension (`bedrock-translator/geyser-extension`)**
   - Registers Wildercord's `317` items, `758` runes, and solid `note_block` block overrides in Geyser matching `wildercord-bedrock-rp.mcpack`.

---

## 1. Generating / Updating the Bedrock Pack & Extension

From `D:\Wildercord-knights`:

```bash
python tools/bedrock_export.py
python -m unittest tools/test_bedrock_export.py -v
```

This regenerates:
- `artifacts/bedrock/wildercord-bedrock-rp.mcpack`
- `artifacts/bedrock/wildercord-bedrock.mcaddon`
- `artifacts/bedrock/wildercord-geyser-mappings.json`
- `src/main/resources/wildercord-bedrock-mappings.json`

Then build both the main Fabric mod JAR and the Geyser extension JAR on your build host (with Java 25 for Loom and Java 21+ for the extension):

```bash
./gradlew test build
gradle -p bedrock-translator/geyser-extension jar
```

---

## 2. Server Setup (Fabric 26.3)

Install into your Fabric `26.3` server's `mods/` folder:
- `wildercord-0.16.0-alpha+mc26.3.jar`
- `fabric-api` (`>= 0.161.0+26.3`)
- `Geyser-Fabric` (`2.12.1-b1308` or matching `26.3` build)
- `Floodgate-Fabric` (`2.2.7-b69` or matching `26.3` build)

Configure Geyser (`config/Geyser-Fabric/`):
1. Copy `wildercord-geyser-extension.jar` into `config/Geyser-Fabric/extensions/`.
2. Copy `wildercord-bedrock-rp.mcpack` into `config/Geyser-Fabric/packs/`.
3. In `config/Geyser-Fabric/config.yml`:
   - Set `remote.auth-type: floodgate` (keeps `online-mode=true` on the Java server so both Java premium players and console/Xbox Live players authenticate cleanly).
   - Set `gameplay.enable-custom-content: true`.
   - Set `bedrock.transport: both` (enables both classic **RakNet** UDP on `bedrock.port: 19132` for consoles/BedrockConnect DNS AND **NetherNet** WebRTC on `bedrock.webrtc-port: 19133`).
4. Forward **UDP ports `19132` and `19133`** on your firewall/router (or add a **UDP** tunnel on Playit.gg pointing to `127.0.0.1:19132` for Bedrock alongside your TCP `25565` tunnel for Java).

---

## 3. How Console Players Join (Xbox, PlayStation & Nintendo Switch)

Consoles do not have a built-in **Add Server** button. Use either method below — neither modifies the console and both auto-download the Wildercord Resource Pack upon joining.

### Option A: FriendConnect / MCXboxBroadcast (Recommended — Zero Console Network Edits)
1. Install Geyser's **MCXboxBroadcast** extension alongside `wildercord-geyser-extension.jar` in `config/Geyser-Fabric/extensions/`, and sign in with a dedicated Microsoft account added as a friend by your console players.
2. Console players open Minecraft → **Friends** tab → join the server directly with one click (works on Xbox, PS4/PS5, Nintendo Switch, iOS, Android, and Windows).

### Option B: BedrockConnect DNS Method
1. On **Xbox / PlayStation / Switch**, open Internet Settings → **Manual DNS**:
   - **Primary DNS:** Public BedrockConnect DNS IP (check https://github.com/Pugmatt/BedrockConnect for the currently active DNS IPs)
   - **Secondary DNS:** `1.1.1.1` (Cloudflare) or `8.8.8.8` (Google)
2. Launch Minecraft → open **Servers** → join any Featured Server (such as The Hive).
3. In the BedrockConnect server list menu that opens, choose **Connect to a Server**, enter your server's IP/domain and Bedrock **UDP port `19132`**, accept the Resource Pack prompt, and join.

> **Troubleshooting console Resource Pack downloads:** The pack is sized at ~16 MB specifically to stay under console download thresholds. Ensure you place **`wildercord-bedrock-rp.mcpack`** (NOT `.mcaddon`) inside `config/Geyser-Fabric/packs/`.

---

## 4. Controls & Features for Bedrock / Console Players

- **Cast Active Spell:** Hold a Cord in your main hand and press **Use / Right-Click** (`LT` / `L2` / `ZL` on controller), or run `/wc cast`.
- **Open Wildercord Magic Hub:** **Sneak + Use** while holding a Cord, or run `/wc`. From the controller-friendly form menu you can:
  - Cast or switch between all 5 spell slots
  - Weave learned runes into any spell slot
  - Open the **9-slot Cord / Gear / Backpack equipment chest bridge** (`/wc equip`)
  - Open your worn **Backpack** (`/wc backpack`)
  - View and accept **Oathhold Guild Request Board** contracts (`/wc board`)
  - Inspect Heart Circle, Condensed Mana, Grimoire count, and Aura breakthrough status
- **Vanilla & Translated Gameplay:**
  - Java and Bedrock players share the exact same world, dungeons, quests, guilds, duels, bosses, mana, and loot.
  - Custom Wildercord items, runes, weapons, armor, meals, and solid blocks render with Wildercord's custom textures and sound effects via `wildercord-bedrock-rp.mcpack`.

### Known Bedrock Translation Adaptations
- **Custom client screens** (`CordScreen`, `RuneCatalogScreen`, etc.) open via `/wc` native **Cumulus Forms** and chest menu bridges instead of modded Java widgets.
- **Fusion Altar** opens via Bedrock's 5-slot **Hopper** layout (`3` rune slots, `1` catalyst slot, `1` output slot).
- **Custom 3D modded entities** (Shrine Cats, Thalassor, Sword Masters, wildlife) use mapped vanilla entity geometry models (`cat`, `fox`, `vindicator`, `dolphin`, `iron_golem`, etc.) while keeping their full server-side AI, stats, loot, and names.
- **Custom client shader particles** map to closest vanilla particles (`enchant`, `end_rod`, `flame`, `cherry_leaves`, etc.).
