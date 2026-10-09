# WilderCord visual profiles

Copy the selected profile's `config/wildercord-visuals.json` into your Minecraft instance's `config` directory while the game is closed. These settings can also be applied from **Magic visuals** using its assignable key in Controls. Changing them affects decoration, flashing and camera motion; gameplay timing and hostile warnings are preserved.

* **Performance:** balanced personal formations, minimal other formations, reduced flashing and no camera shake.
* **Balanced:** balanced formations for both groups, ordinary flashing and camera shake.
* **Cinematic:** full formations for both groups, ordinary flashing and camera shake.

The folders can be used as configuration presets or packaged into importable Modrinth launcher packs. After `gradlew build`, run:

```text
python tools/build_profiles.py --jar build/libs/wildercord-0.11.2-alpha+mc26.3.jar
python tools/audit_packages.py
```

The three `.mrpack` files appear in `artifacts/profiles`. Import one into a fresh instance using a launcher that supports the Modrinth pack format, and select Java 25. Each includes the local release jar, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3 and Sodium 0.9.2. Cinematic additionally includes Iris 1.11.6. External mods are downloaded by the launcher from the pinned official Modrinth URLs with SHA-1/SHA-512 verification; their jars are not redistributed in the archive. `dependencies.lock.json` records the exact published files and required dependency versions.

The normal build also produces `wildercord-distributable` (only the runtime JAR) and `wildercord-packaging-report` CI artifacts, alongside the existing full JAR and profile artifacts. The stdlib audit checks archive integrity and bounded sizes, metadata/version/dependencies, entrypoint and mixin classes, all main/client resource and compiled-class bytes, the MIT notice, each profile's locked manifest, and the embedded mod hash. It rejects unexpected files and known development/credential paths; this is not a general secret-content scan. The JSON report records hashes, sizes and checkout provenance, and reports whether the raw JAR fits 32 MiB. The downloaded CI artifact ZIP has separate overhead and is not measured by that comparison.

This is packaging verification only. It does not establish gameplay, native rendering, multiplayer, world-upgrade safety, successful launcher import, or the safety/availability of remote dependency bytes. Existing gameplay gates remain separate. Archive checks never extract files or download dependencies; ZIP64, encrypted, unsupported-compression and oversized archives fail closed.

Render distance starts at 10 chunks for Performance and 12 for the other profiles, with simulation distance 6 and a 120 FPS cap. These are configurable starting points. No custom JVM heap size is imposed without measuring live memory. No third-party shader pack is bundled or automatically enabled; Cinematic supports selecting one through Iris. The pinned Iris/Sodium pair passed the local shader fixture, which does not prove compatibility with every shader pack. Pack archive contents and dependency consistency are checked by the builder; launcher import itself still requires a fresh-instance check.

Use the settings screen's thirty-second benchmark in the same practice scene for each preset. It records presentation intervals in `logs/wildercord-frame-benchmarks.txt`. `/wildercord visualstats` reports packet, cache and server tick counters. Frame intervals are not GPU timings or CPU allocation measurements. The `PerformanceProfilesTest` uses 24 moving dummies, Firestorm beams, 1600×900 and a 120 FPS cap; compare local results rather than assuming any preset guarantees a particular frame rate.
