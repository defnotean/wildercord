# Porting Wildercord Across Minecraft Versions

Wildercord separates mod mechanics, data generation, and UI logic from version-specific Minecraft and Fabric APIs so that backporting or forward-porting touches four small, explicit surfaces instead of hundreds of gameplay and rendering files.

## 1. The Four Version-Specific Surfaces

| Surface | Path(s) | What varies per version |
|---|---|---|
| Build toolchain | `gradle.properties`, `build.gradle` | `minecraft_version`, `loader_version`, `loom_version`, `fabric_api_version`, dependencies |
| Datapack & guide generators | `tools/datapack_profile.py` | Loot modifier/condition schema, `exploration_map` `#` prefix, `worldgen/configured_feature` vs `worldgen/feature`, placement/spawner JSON fields, brewing path, component names, test profile |
| Compatibility seams | `src/main/java/dev/wildercord/compat/`<br>`src/client/java/dev/wildercord/client/compat/`<br>`src/gametest/java/dev/wildercord/gametest/compat/` | Entity hurt/swing/drop/registry helpers (`McEntities`, `CompatCubeMob`, `McBlocksAndItems`, `SimpleWorldFeature`, `McWorldgen`, `McLootAndTriggers`, `WildercordBrewing`), client rendering/input/culling seams (`RenderCompat`, `CullableMobRenderer`, `CullableEntityRenderer`, `FirstPersonHandView`), and gametest harness helpers (`GametestCompat`) |
| Mixin hooks | `src/*/java/**/mixin/` | Bytecode descriptors, `@At(target = ...)` signatures, `@Shadow` / `@Accessor` / `@Invoker` members, and version-specific hook targets |

Everything outside these four surfaces — `dev.wildercord.spell` (pure Java, zero Minecraft imports), spell casting, Aura combat mechanics, structures, entities, widgets, and tests — should stay identical across version branches.

## 2. Rules for Preserving Portability

1. **Never scatter version-divergent Minecraft calls outside `compat/`.** If a new feature hits an API that changed across versions (e.g. `Entity`, `LootContext`, `Structure`, `RenderPipeline`, `SubmitNodeCollector`, `PoseStack`), add a helper to the matching `compat` class rather than calling the version-specific API directly across gameplay or UI classes.
2. **Respect package and lifecycle invariants.**
   - All client compat classes must live under `dev.wildercord.client.compat` (enforced by `tools/audit_packages.py`).
   - Do not hold mutable static player/entity maps in compat classes (`SessionMapStopClearTest`).
   - `dev.wildercord.spell` must never import Minecraft classes.
3. **Always update generators rather than hand-editing resources.** Add schema switches to `tools/datapack_profile.py` (`DataPackProfile`), regenerate with `python tools/generate_assets.py`, and keep `DataFormatTest` aligned via `src/test/java/dev/wildercord/TestDataProfile.java`.
4. **Update documentation together with code.** Run `python tools/player_docs.py --check` and `python tools/check_generated_assets.py` before committing.

## 3. Pre-Flight Port Verification (`tools/verify_port.py`)

`tools/verify_port.py` statically checks Mixins and datapacks against the active Minecraft version's Loom JAR (using `javap -c -p -s`) before compiling or booting the game:

- **Mixins (across all `src/*/resources/*.mixins.json`):** verifies `@Mixin` targets exist in the MC JAR, injector `method` names and explicit descriptors match, `@At("INVOKE")` / `@At("FIELD")` / `@At("NEW")` targets exist and satisfy `expect` / `allow` instruction counts inside matched methods, `@Local(argsOnly = true)` parameters without `ordinal`/`index` are unambiguous in the target descriptor, and `@Shadow` / `@Accessor` / `@Invoker` members exist.
- **Datapacks:** verifies stale version directories (`worldgen/feature` vs `worldgen/configured_feature`, `brewing` vs `recipe/brewing`) are absent, checks `exploration_map` destination `#` rules match `DataPackProfile`, and validates `minecraft:*` registry IDs (placement modifiers, block predicates, loot functions, loot conditions, item data components, recipe serializers) against constants compiled into the MC JAR.

Run it directly on any machine with the Loom cache populated (it is also wired into `python tools/run_vps_checks.py build`):

```bash
python tools/verify_port.py
```

When running locally without a populated Loom cache, run `python tools/verify_port.py --allow-missing-jar` to run the structural datapack checks, or point `--mc-jar` at a merged Minecraft client JAR.

## 4. Step-by-Step Playbook for a New Target Version

1. **Create the version branch** from the closest maintained branch (`git checkout -b <target_version>`).
2. **Update `gradle.properties`** (`minecraft_version`, `loader_version`, `loom_version`, `fabric_api_version`).
3. **Add the version profile in `tools/datapack_profile.py` and `TestDataProfile.java`**, then regenerate datapacks and verify:
   ```bash
   python tools/generate_assets.py
   python tools/check_generated_assets.py
   ```
4. **Run static pre-flight verification** once Loom has cached the target Minecraft JAR:
   ```bash
   python tools/verify_port.py
   ```
5. **Adapt `compat/` and any Mixins flagged by `verify_port.py` or `javac`**, without re-splitting callers outside `compat/`.
6. **Verify on `pandora-vps` (`WIN-I2GBVRRD872`):**
   - `python tools/run_vps_checks.py build --head <sha> --computer WIN-I2GBVRRD872`
   - `python tools/build_profiles.py` and `python tools/audit_packages.py`
   - `python tools/run_vps_checks.py suite --suite release-smoke --head <sha> --computer WIN-I2GBVRRD872 --accepted-eula C:\wildercord\eula.txt` (plus per-area native suites for any rewritten mechanics or rendering paths).
