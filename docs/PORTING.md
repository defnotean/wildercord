# Porting Wildercord Across Minecraft Versions

Wildercord separates mod mechanics, data generation, and UI logic from version-specific Minecraft and Fabric APIs so that backporting or forward-porting touches four small, explicit surfaces instead of hundreds of gameplay and rendering files.

## 1. The Four Version-Specific Surfaces

| Surface | Path(s) | What varies per version |
|---|---|---|
| Build & descriptors | `gradle.properties`, `build.gradle`, `profiles/dependencies.lock.json`, `src/*/resources/fabric.mod.json`, `src/*/resources/*.mixins.json` | `minecraft_version`, `loader_version`, `loom_version`, `java_version`, `fabric_api_version`, Sodium/Iris Modrinth lock pins, Mixin `compatibilityLevel`, and (for `< 26.1`) Loom remap plugin/configurations |
| Datapack & guide generators | `tools/datapack_profile.py`, `src/test/java/dev/wildercord/TestDataProfile.java`, generated `src/main/resources/data/` | Loot modifier/condition schema, `exploration_map` `#` prefix, `worldgen/configured_feature` vs `worldgen/feature`, placement/spawner JSON fields, brewing path, component names, test profile |
| Compatibility seams | `src/main/java/dev/wildercord/compat/`<br>`src/client/java/dev/wildercord/client/compat/`<br>`src/gametest/java/dev/wildercord/gametest/compat/` | Entity hurt/swing/drop/registry helpers (`McEntities`, `CompatCubeMob`, `McBlocksAndItems`, `SimpleWorldFeature`, `McWorldgen`, `McLootAndTriggers`, `WildercordBrewing`), client rendering/input/culling seams (`RenderCompat`, `CullableMobRenderer`, `CullableEntityRenderer`, `FirstPersonHandView`), and gametest harness helpers (`GametestCompat`) |
| Mixin hooks | `src/*/java/**/mixin/`, `FirstPersonAvatarState.java`, `MastersHandMotionState.java` | Bytecode descriptors, `@At(target = ...)` signatures, `@Shadow` / `@Accessor` / `@Invoker` members, and version-specific hook targets |

Everything outside these four surfaces — `dev.wildercord.spell` (pure Java, zero Minecraft imports), spell casting, Aura combat mechanics, structures, entities, widgets, and tests — should stay identical across version branches for `26.1+`.

## 2. Non-Negotiable Portability Rules

1. **Never scatter version-divergent Minecraft calls outside `compat/`.** If a Minecraft or Fabric API signature changes in another version, adapt or add a method on the matching `compat/` class (and keep `COMPAT_CONTRACT` in `tools/verify_port.py` satisfied) rather than branching or editing callers directly.
2. **Audit your branch diff continuously.** Run:
   ```bash
   python tools/port_helper.py diff-surface --base 26.2
   ```
   Every modified Java/resource file must belong to one of the four surfaces (`diff-surface` exits `1` on out-of-surface edits unless `--allow-identifier-migration` is explicitly supplied for `< 26.1` `ResourceLocation` migrations).
3. **Respect package and lifecycle invariants.**
   - Client compat classes live strictly under `dev.wildercord.client.compat` (enforced by `tools/audit_packages.py`).
   - Do not hold mutable static fields or entity/player map caches (`WeakHashMap`, `IdentityHashMap`, `ConcurrentHashMap`, `HashMap`) in core `compat/` classes (`SessionMapsClearTest` and `verify_port.py` reject them; use Mixin duck interfaces instead).
   - `dev.wildercord.spell` must never import Minecraft classes.
4. **Regenerate resources instead of hand-editing JSONs.** Adjust schema rules in `tools/datapack_profile.py` and `TestDataProfile.java`, then run `python tools/generate_assets.py`.
5. **Never touch the live server or dirty VPS state.** All Gradle builds and native tests run in `C:\wildercord-ci\repo` on `WIN-I2GBVRRD872` (`pandora-vps`). Never modify `C:\wildercord`, never kill arbitrary `java.exe` processes, and always clean up `build\run` and temporary `.mrpack` files after verification.

## 3. Turnkey Porting Workflow (Command-by-Command)

### Step A — Branch & Pin Dependencies
Create the target branch from the closest maintained version branch (e.g. branch `26.1.2` or `26.1` from `26.2`, never from `main` unless porting upward past `26.3`):

```bash
git checkout -b 26.1.2 26.2
python tools/port_helper.py sync-deps --mc 26.1.2 --write
```

`sync-deps --write` queries Fabric Meta and Modrinth for `fabric-api`, `sodium`, and `iris` (selecting a mutually compatible triple where Iris's `required` Sodium version matches), computes `java_version` (`25` for `26.1+`, `21` for `< 26.1`), and updates all four aligned locations atomically:
1. `gradle.properties`
2. `src/main/resources/fabric.mod.json`
3. All `compatibilityLevel` fields in `src/*/resources/*.mixins.json`
4. `profiles/dependencies.lock.json`

Verify alignment and profile rules locally:

```bash
python tools/port_helper.py status
python tools/verify_port.py --allow-missing-jar
```

### Step B — Regenerate Datapacks, Check Docs & Run Python Unit Tests
Update `tools/datapack_profile.py` and `src/test/java/dev/wildercord/TestDataProfile.java` only if the new Minecraft version introduces schema differences not already handled by `DataPackProfile.before_26_3`. Then regenerate assets and check everything locally:

```bash
python tools/generate_assets.py
python tools/check_generated_assets.py
python tools/player_docs.py --check
python -m unittest discover -s tools -p "test_*.py"
```

### Step C — Warm Loom Cache & Run Static Bytecode Pre-Flight on `pandora-vps`
Push your branch (or fetch it into `C:\wildercord-ci\repo`) and populate the target Minecraft Loom JAR + compile outputs on `WIN-I2GBVRRD872`:

```powershell
# Clean inherited JVM/test env vars before every VPS Gradle or runner invocation
Remove-Item Env:JAVA_TOOL_OPTIONS,Env:JDK_JAVA_OPTIONS,Env:_JAVA_OPTIONS,Env:GRADLE_OPTS,Env:JAVA_OPTS -ErrorAction SilentlyContinue
Get-ChildItem Env:WILDERCORD_* | ForEach-Object { Remove-Item "Env:$($_.Name)" }

$env:JAVA_HOME = 'C:\java\jdk-25.0.4.1+1'          # Use jdk-21.0.13+9 for <= 1.21.x
$env:GRADLE_USER_HOME = 'C:\wildercord-ci\gradle'
$env:PATH = "C:\wildercord-ci\venv\Scripts;$env:JAVA_HOME\bin;$env:PATH"
$env:PYTHONUTF8 = '1'

.\gradlew.bat compileJava compileClientJava compileTestJava compileGametestJava --no-daemon --max-workers=2 --console=plain
python tools/port_helper.py status
python tools/verify_port.py
```

`tools/verify_port.py` now checks **both**:
- Every Minecraft-targeted Mixin (`@Mixin` targets, `@Shadow` / `@Accessor` / `@Invoker`, injector `method` names/descriptors, `@At("INVOKE"/"FIELD"/"NEW")` targets and counts, `@Local(argsOnly = true)` ambiguity, and `minecraft:*` registry IDs in datapacks), AND
- All mod-targeted `@Mixin`s (`dev/wildercord/*`) once `build/classes/java/{main,client,gametest}` is present (catching mismatched `remap = false` method/target signatures statically before booting a native client), AND
- Core `compat/` contract completeness (`verify_compat_contract`) and descriptor alignment (`verify_descriptor_alignment`).

When `verify_port.py` flags a renamed or moved class/method/field, it prints `candidates:` directly in the error line.

### Step D — Inspect Vanilla Classes Without Fighting `javap` or PowerShell Quoting
Use `tools/port_helper.py` whenever a Vanilla class moves package or renames a member:

```bash
# Find relocated classes (e.g. EnderMan vs Enderman, RenderPipeline, RenderSetup)
python tools/port_helper.py search-class Enderman

# Dump methods, fields, and exact JVM descriptors matching a substring
python tools/port_helper.py inspect LivingEntity --member swing

# Include javap -c bytecode instructions and superclass/interface hierarchy when checking @At targets
python tools/port_helper.py inspect Gui --member render --code --supertypes
```

Adapt only files inside `compat/` and `mixin/` (plus Mixin duck interfaces if needed), then verify surface isolation:

```bash
python tools/port_helper.py diff-surface --base 26.2
```

### Step E — Full VPS Verification (`build`, `audit_packages.py`, `release-smoke`)
Commit cleanly (`defnotean <iangaleonofficial@gmail.com>`, zero AI attribution), push to your working branch, checkout the exact SHA in `C:\wildercord-ci\repo`, and run all VPS gates inside a clean environment:

```powershell
# Ensure clean environment (no WILDERCORD_* or JAVA_TOOL_OPTIONS overrides)
Remove-Item Env:JAVA_TOOL_OPTIONS,Env:JDK_JAVA_OPTIONS,Env:_JAVA_OPTIONS,Env:GRADLE_OPTS,Env:JAVA_OPTS -ErrorAction SilentlyContinue
Get-ChildItem Env:WILDERCORD_* | ForEach-Object { Remove-Item "Env:$($_.Name)" }

$env:JAVA_HOME = 'C:\java\jdk-25.0.4.1+1'          # Use jdk-21.0.13+9 for <= 1.21.x
$env:GRADLE_USER_HOME = 'C:\wildercord-ci\gradle'
$env:PATH = "C:\wildercord-ci\venv\Scripts;$env:JAVA_HOME\bin;$env:PATH"
$env:PYTHONUTF8 = '1'

$sha = (git rev-parse HEAD).Trim()

try {
    python tools/run_vps_checks.py build --head $sha --computer WIN-I2GBVRRD872
    if ($LASTEXITCODE -ne 0) { throw "VPS build gate failed" }

    $jar = Get-ChildItem build\libs\wildercord-*.jar |
        Where-Object { $_.Name -notmatch '-sources\.jar$|-dev\.jar$' } |
        Select-Object -First 1 -ExpandProperty FullName
    python tools/build_profiles.py --jar $jar
    if ($LASTEXITCODE -ne 0) { throw "Profile build failed" }

    python tools/audit_packages.py --jar $jar
    if ($LASTEXITCODE -ne 0) { throw "Package audit failed" }

    python tools/run_vps_checks.py suite --suite release-smoke --batch 1 --head $sha --computer WIN-I2GBVRRD872 --accepted-eula C:\wildercord\eula.txt
    if ($LASTEXITCODE -ne 0) { throw "release-smoke failed" }
}
finally {
    Remove-Item -Recurse -Force build\run -ErrorAction SilentlyContinue
    Remove-Item -Force artifacts\profiles\*.mrpack -ErrorAction SilentlyContinue
}
```

Note `--batch 1` is **required** in `suite` mode (`run_vps_checks.py` exits with code `2` if omitted). `build_profiles.py` automatically deletes stale `wildercord-*.mrpack` files from other Minecraft versions before writing the current preset packs.

---

## 4. Version Cliff Reference (`26.2 -> 26.1.x -> 1.21.x`)

### Porting `26.2 -> 26.1.x` (`26.1.2` / `26.1`)
- **JDK:** Java `25` (`C:\java\jdk-25.0.4.1+1`), Mixin `JAVA_25`.
- **Gradle / Loom:** Stays on unobfuscated `net.fabricmc.fabric-loom` (`loom_version=1.18.2`), zero remapping. Modrinth provides releases for `26.1.2` (`fabric-api`, `sodium`, `iris`).
- **Datapacks:** `DataPackProfile.for_version("26.1.2")` automatically normalizes `"26.1.2"` -> `"26.1"` with `before_26_3 = True` (`worldgen/configured_feature`, legacy `functions`/`conditions` loot format, unprefixed `exploration_map` destination, `block_state_property`, `minCount`/`maxCount`, `brewing/` mixes). If any `26.1` vs `26.2` datapack difference appears, gate it on `profile.before_26_2` rather than `is_26_2`.
- **Code:** Focus almost exclusively on `compat/` and `mixin/` (rendering pipeline/render-type setup, particle layers, first-person hand state, any minor entity/worldgen method differences).

### Porting `26.1.x -> 1.21.x` (`1.21.11` and older — Obfuscated Boundary)
Minecraft `< 26.1` crosses the obfuscation & Java boundary and requires explicit build-script and resource-location adaptation:
1. **JDK & Mixin compatibility level:** Java `21` (`C:\Users\VPS\agent-workspace\tools\jdk21\jdk-21.0.13+9` on `WIN-I2GBVRRD872`), `java_version=21`, and `"compatibilityLevel": "JAVA_21"` (set automatically by `port_helper.py sync-deps --write`).
2. **`build.gradle` (Loom remap switch):**
   - Replace plugin `id 'net.fabricmc.fabric-loom'` with `id 'net.fabricmc.fabric-loom-remap'` (or compatible remapping Loom plugin version).
   - Add `mappings loom.officialMojangMappings()` inside `dependencies {}`.
   - Switch Fabric Loader / Fabric API / Sodium / Iris dependency configurations from `implementation` / `compileOnly` / `runtimeOnly` back to `modImplementation` / `modCompileOnly` / `modRuntimeOnly` so Loom remaps them against `intermediary`.
3. **Vanilla `Identifier` -> `ResourceLocation`:** Before `26.1`, Mojang mappings used `net.minecraft.resources.ResourceLocation` instead of `Identifier` (and Fabric API used pre-unobfuscation names such as `ItemGroupEvents`, `FabricItemGroup`, `KeyBindingHelper`, `WorldRenderEvents`, `EntityModelLayerRegistry`, `ParticleFactoryRegistry`, `FuelRegistry`, `PointOfInterestHelper`, `PayloadTypeRegistry.playC2S`/`playS2C`). Pass `--allow-identifier-migration` to `port_helper.py diff-surface` once on `1.21.x` for mechanical identifier renames while keeping gameplay logic identical.
4. **Re-check `tools/audit_packages.py` on `< 26.1`:** Remapped `1.21.x` release JARs carry `fabric-mapping-namespace: intermediary` and generated Mixin refmaps (`*-refmap.json`), so `check_manifest` / `check_jar` on a `< 26.1` branch must accept `intermediary` and the remapped refmap JSON entries.
5. **Re-check `javap` / Mixin `@At(remap)` behavior:** In obfuscated Loom named dev JARs (`minecraft-merged*-named.jar`), `verify_port.py` validates Mojang-named dev targets (`remap = false` still applies only to non-MC classes or Fabric interface-injected methods), while Mixin AP generates refmaps at build time.

---

## 5. Common Errors & Fast Fixes

| Symptom | Cause | Exact fix |
|---|---|---|
| `VPS batch needs exact commit, positive run ID and attempt` (exit `2`) | Missing `--batch` in `suite` mode | Pass `--batch 1` to `run_vps_checks.py suite --suite release-smoke ...` |
| `Inherited test filters or JVM injections are not accepted` | Shell inherited `WILDERCORD_*` or `JAVA_TOOL_OPTIONS` / `GRADLE_OPTS` | Run the `Remove-Item Env:...` snippet from Section 3 Step C before invoking `run_vps_checks.py` |
| `Missing or unexpected launcher profiles` in `audit_packages.py` | Leftover `.mrpack` archives from a previous MC version in `artifacts/profiles/` | Run `python tools/build_profiles.py --jar <jar>` (now auto-cleans stale `wildercord-*.mrpack`) |
| `Profile runtime pins disagree with Gradle properties` | `gradle.properties` updated manually without syncing `profiles/dependencies.lock.json` | Run `python tools/port_helper.py sync-deps --mc <ver> --write` |
| `Stale feature directory present` / `Stale brewing directory present` | Switched branches without rerunning `generate_assets.py` | Run `python tools/generate_assets.py` |
| `@Mixin target class/method/field missing` in `verify_port.py` | Vanilla class moved or member renamed across versions | Inspect the `candidates:` printed by `verify_port.py`, or run `port_helper.py search-class` / `inspect` |
| `OUT_OF_SURFACE` from `port_helper.py diff-surface` | Edited a gameplay/UI/gametest file directly instead of updating `compat/` | Move the version-specific call into `src/*/java/**/compat/` and restore the caller |
