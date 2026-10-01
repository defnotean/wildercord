# Local performance measurements

## Fixed practice scene

Hardware reported by the local game: AMD Ryzen 7 7800X3D, NVIDIA GeForce RTX 5080. The scenario uses 24 moving dummies, one caster repeatedly releasing Firestorm beams, a 1600 x 900 window, VSync disabled and a 120 FPS limit. Minecraft AFK throttling was disabled for the corrected comparison.

| Preset | Frames | Median interval | p95 | p99 | Idle-throttled frames |
|---|---:|---:|---:|---:|---:|
| Performance | 3590 | 8.33 ms | 9.91 ms | 10.63 ms | 0 |
| Balanced | 3598 | 8.33 ms | 9.66 ms | 10.15 ms | 0 |
| Cinematic | 3599 | 8.33 ms | 9.58 ms | 9.97 ms | 0 |

Evidence: logs/performance-profiles-unthrottled.log. The scene is too light and the frame limit too dominant to establish a meaningful speed advantage between presets. These are presentation intervals, not GPU timestamps. This is not an eight-client network benchmark.

### Repeat after the original-material expansion

The same fixed scene was repeated during `logs/expanded-complete-suite.log`, with the new material particles and casting changes compiled. Each profile again had zero AFK-throttled frames, zero limited formations and zero limited decorative deliveries.

| Preset | Frames | Median | p95 | p99 | Server tick median / p95 |
|---|---:|---:|---:|---:|---:|
| Performance | 3576 | 8.33 ms | 9.08 ms | 9.43 ms | 0.47 / 1.31 ms |
| Balanced | 3599 | 8.33 ms | 8.92 ms | 9.19 ms | 0.41 / 1.05 ms |
| Cinematic | 3599 | 8.33 ms | 8.96 ms | 9.23 ms | 0.45 / 0.99 ms |

The compiled-plan cache recorded 96 hits and one miss across the three runs. Server tick statistics use the latest 512 samples. These capped local measurements do not establish a preset speed ranking or an uncapped before/after improvement; variation between runs should not be attributed to the changes without a controlled benchmark. They also do not measure 512 active physical cells or real networked clients.

## Java Flight Recorder inspection

A separate 60-second recording during the integrated gameplay suite captured 1037 execution samples and 3828 allocation samples. This mixed scene includes gameplay checks, visual output and world transitions. It is useful for locating work, not a controlled before/after performance claim. The recording stayed local in artifacts/profiling/integrated-gameplay.jfr.

### Most frequent sampled executing methods

| Method | Samples |
|---|---:|
| `net.minecraft.world.level.LevelReader.getNoiseBiome` | 56 |
| `net.minecraft.client.FramerateLimiter.limitDisplayFPS` | 51 |
| `net.minecraft.world.level.chunk.PalettedContainer.get` | 36 |
| `it.unimi.dsi.fastutil.Arrays.quickSort` | 34 |
| `com.mojang.blaze3d.vertex.BufferBuilder.setUv` | 28 |
| `net.minecraft.world.level.levelgen.Aquifer$NoiseBasedAquifer.computeSubstance` | 26 |
| `net.minecraft.client.Screenshot.lambda$takeScreenshot$1` | 26 |
| `java.util.HashMap.getNode` | 23 |
| `net.minecraft.world.level.levelgen.synth.GradientNoise.permute` | 20 |
| `net.minecraft.world.level.chunk.PalettedContainer.getAndSet` | 18 |

### WilderCord methods present in sampled call stacks

Inclusive counts can overlap within one execution sample. They do not measure exclusive CPU time.

| Method | Inclusive samples |
|---|---:|
| `dev.wildercord.client.fx.LightParticle.quad` | 12 |
| `dev.wildercord.client.fx.LightParticle.inPlane` | 11 |
| `dev.wildercord.client.fx.SpellCircleParticle.extract` | 9 |
| `dev.wildercord.client.fx.LightParticle.circle` | 9 |
| `dev.wildercord.client.fx.SpellCircleParticle.piece` | 8 |
| `dev.wildercord.client.fx.SpellCircleParticle.ring` | 6 |
| `dev.wildercord.client.fx.LightParticle.ring` | 6 |
| `dev.wildercord.client.fx.SigilGroup.extractRenderState` | 5 |
| `dev.wildercord.client.SpellHud$$Lambda.0x0000000095f70ef8.extractRenderState` | 3 |
| `dev.wildercord.client.SpellHud.extract` | 3 |

### Largest sampled allocation classes

JFR allocation weights estimate bytes represented by a sample; they are not retained heap sizes.

| Class | Weighted MiB |
|---|---:|
| `[Ljava/lang/Object;` | 13785.1 |
| `[I` | 1387.3 |
| `net/minecraft/world/level/levelgen/densityfunction/DensityBufferPool` | 1150.3 |
| `java/util/Collections$UnmodifiableMap` | 1103.9 |
| `java/util/ImmutableCollections$ListItr` | 818.2 |
| `com/google/common/base/Suppliers$NonSerializableMemoizingSupplier` | 817.2 |
| `java/util/stream/ReferencePipeline$Head` | 798.4 |
| `net/minecraft/core/BlockPos` | 651.3 |
| `it/unimi/dsi/fastutil/objects/Reference2ObjectArrayMap$EntrySet$2` | 391.4 |
| `net/minecraft/world/level/ChunkPos` | 366.0 |

### First WilderCord allocation frame

| Method | Weighted MiB |
|---|---:|
| `dev.wildercord.client.SpellHud.extract` | 6.6 |
| `dev.wildercord.client.fx.ScreenEffects.drawTint` | 4.9 |
| `dev.wildercord.client.SpellHud.hexagon` | 4.0 |
| `dev.wildercord.content.RuneItem.familyLine` | 2.8 |
| `dev.wildercord.client.ElementGlyphs.draw` | 2.5 |
| `dev.wildercord.client.render.CordLayer.submit` | 2.0 |
| `dev.wildercord.content.RuneItem.stack` | 2.0 |
| `dev.wildercord.client.fx.LightParticle.arc` | 1.6 |
| `dev.wildercord.gear.GearSlots.equipped` | 1.6 |
| `dev.wildercord.cast.ArmorResponses.lambda$init$1` | 1.0 |

## Repeat capture

Use tools/capture_profile.ps1 with the Minecraft Java process id and a 10-600 second duration. The script starts a local JFR profile and returns immediately so the selected scenario can be exercised. Keep camera, spell load, caster count, draw distance and shader configuration constant between captures.

The native shader suite separately passed with the repository-pinned Iris/Sodium pair and its supplied test pack. Remote CI, arbitrary third-party shader packs, low-end hardware, natural exploration over many seeds and a real 2/4/8-client dedicated-server session remain separate verification tasks.
