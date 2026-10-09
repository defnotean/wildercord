# Affected client game tests

Re-run only the native client game test classes that a change can statically
reach, in one client launch:

```sh
./gradlew runClientGameTest -PaffectedSince=origin/main --console=plain
```

`affectedSince` is a client suite selector like `focusedSuite` and `ciSuite`; it
cannot be combined with them. It compiles `main`, `client` and `gametest` first,
then `tools/affected_tests.py` rewrites the processed `fabric-client-gametest`
entrypoints to the selected classes, in their registered order. The selection and
its reasons are printed before the client starts, and the launch log carries
`WILDERCORD_AFFECTED_SELECTION`. If nothing is selected the build fails with the
changed paths instead of launching an empty client.

Changed files are `git diff --name-only <ref>` (staged and unstaged) plus
untracked files. To preview without launching Minecraft:

```sh
python tools/affected_tests.py --since origin/main --explain
python tools/affected_tests.py --files src/main/java/dev/wildercord/aura/MastersStyleAnimation.java --explain
python tools/affected_tests.py --since HEAD --json
```

`--explain` prints one dependency path per selected test. `->` is a bytecode
reference from `jdeps`; `~>` is a source-only reference (javac inlines
compile-time constants, so the tool also adds over-approximate import/name
edges). The graph is cached in `build/affected-tests/graph.json` and rebuilt when
any class or source file changes.

## Rules

| Change | Selection |
| --- | --- |
| Java under `src/{main,client,gametest}/java` | Tests whose class closure reaches a class compiled from that file, including `$` nested and anonymous classes |
| Mod entrypoint or mixin class, or a class reached only from them | All |
| `src/*/resources/**` (assets, textures, models, lang, data), `fabric.mod.json`, mixin JSON, access wideners | All |
| `build.gradle`, `settings.gradle`, `gradle.properties`, Gradle wrapper, unclassified paths | All |
| `docs/`, `wiki/`, `gitbook/`, `tools/`, `.github/`, Markdown, `src/test/` | None |

Resource changes stay "all": no texture or model mapping is narrow enough to
trust. Most classes are also reachable from the mod entrypoints, so the tool
warns when a changed class can run through the game itself (renderers, events,
registries) for tests that never reference it. `-PaffectedStrict` (or
`--strict`) turns that warning into a full selection.

## Not a release gate

This is a fast iteration aid only. Static dependencies cannot see shared world or
player state, events, registry lookups, rendering dispatch, ordering or timing
between classes in one client. A class that passes alone can still fail in the
full roster. The full CI suites (`ciSuite` catalogs, the full-client shards and
the required Masters parts) remain the release gate; run the owning suite before
merging.
