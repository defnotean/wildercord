# Wildlife spawn authority — 2026-10-04

## Correction

Lantern Newt, Reedback Crab, Rootmolt Strider and Cinder Bailiff now refuse CHUNK_GENERATION before configuration, RNG, habitat and live population queries. NATURAL requires an actual ServerLevel on its owning server thread. Manual command/egg reason bypass remains available. Runtime chance, biome pools, habitat and population caps are unchanged.

Their prior registered predicates escaped the generation accessor through getLevel() into live entity enumeration. Cinder also read mature fern habitat through the live level. Actual regional generation access is not an owning-thread live level; the corrected policy uses runtime natural spawning only. This does not assert a witnessed concurrency crash.

Vanilla original animal generation reads CREATURE pools, making Newt and Crab relevant to that ordinary path. Rootmolt and Bailiff are MONSTER; their exposed branch was unsafe if called, but ordinary vanilla original-generation execution was not demonstrated.

## Acceptance

WildlifeSpawnThreadTest passes33s in the actual native client. It captures a real ServerLevel on the server, asserts the native test thread is not the owning thread, and checks16 runtime/generation refusals per registered type. It performs no off-thread world/entity query. Actual command/egg predicate bypass and server-thread generation refusal remain strict. RootmoltPlacementTest passes25s with eligible runtime positives, configuration/chance/cap/collision/light controls and generation refusal.

WetlandNewtTest passes51s, ReedbackCrabTest passes48s and EmberPaidPacketsTest passes29s. Their actual registered runtime positives, habitat/config/cap controls and paid Bailiff paths remain intact. No creature/item/ecosystem credit is added by this correction. Natural population frequency remains separate survey work.

## Other reviewed policies

Baseline WildlifeSpawns and SporebackContent query the actual regional accessor. Mapped WorldGenRegion entity queries return empty lists; this does not escape into live entity storage, but cannot enforce a generation population cap. That separate policy issue remains open. Aura's independent bounded-query candidate is under its own source review and native acceptance.

Final build passes12s with1000 unit tests and zero failures/errors/skips. Checked review JAR SHA256 **631d87fbd798132e25a1a445e330f1eafb7533610c1287fa315cad60d1c9189f**,5399 exact processed resources and1814 exact production/client classes; ZIP CRC, duplicate and class checks pass. Native instrumentation is excluded. Embedded version remains0.9.1-alpha.1+mc26.3; public release/server activation is separate.
