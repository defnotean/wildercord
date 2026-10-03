# Six field signature fusions: implementation and verification

## Scope and status

This milestone adds six exact signature recipes after commit `00cd3475`. The six are implemented and
passed the focused unit and native client suites described below. This report does not establish
publication, deployment, the completion of the wider living-world goal, or a fresh full-project build.
The parent agent owns those integration and release gates.

The previous 55 elemental recipes and 22 signatures remain, bringing the named recipe total to
**83**, including **28 signatures**. The additions have six carved inventory textures, twelve original
synthesized sound events (one preparation cue and one impact cue for each), and independent material
preparation, travel and impact arrangements. They use the existing material particle engine; they
are not six new particle engine types.

| Runtime ID | Exact altar pair | Element / effect kind | Base mana |
|---|---|---|---:|
| `springbed` | Basinfill + Grow | Frost / World | 14 |
| `cinder_sieve` | Ember + Collect | Fire / World | 12 |
| `ashen_mercy` | Fireward + Cleanse | Life / Helpful | 16 |
| `clockroot` | Root + Foresight | Earth / Harmful | 16 |
| `skylatch` | Levitate + Anchor | Wind / Helpful | 14 |
| `thresherwind` | Harvest + Windcut | Wind / World | 12 |

All six are tier III. Their effects use fixed resource and time caps rather than POWER or DURATION
scaling. Shapes and other ordinary spell costs still affect the displayed final price.

## Gameplay and safety

- **Springbed:** one Basinfill pour of at most sixteen enclosed cells per payment. After a successful
  pour, up to eight existing immature crops or moist Moonreed buds gain one stage. Growth requires
  hydration from the new pour and permission for both plant and support. It cannot create plants or
  skip Moonreed pollination. It retains Basinfill's shallow enclosure and Nether refusal rules.
- **Cinder Sieve:** simulates the entire inventory transaction before spending one coal or charcoal.
  It processes at most sixteen real furnace inputs from up to 24 nearby candidates, within four
  blocks of impact. All selected output must fit. Foreign ownership, pickup delays, obstruction and
  protected ground cause refusal; refused transactions preserve fuel and loose input. It grants no
  furnace XP. Pulses sharing the payment cannot buy another processing pass.
- **Ashen Mercy:** removes actual harmful effects and fire from eligible allies. Removed conditions
  supply two health each, capped at four health and missing health; a clean target receives no heal.
  Fire Resistance lasts five seconds. A payment can help eight distinct allied UUIDs once each,
  including repeated copies sharing that payment.
- **Clockroot:** remembers safe owned ground for four seconds. A hostile eligible target moving more
  than two horizontal blocks can return once, provided it remains within eight blocks and the
  original floor, route, loaded space and permissions still permit it. The mark is removed before
  a return attempt. Bosses, permanent immunity, Cinnamon, anchors, mounts, creative/spectator players
  and warded arenas are refused. The target's admission rest lasts eight seconds from admission.
- **Skylatch:** checks the space above an eligible ally and holds a small lift for four seconds while
  preserving horizontal motion. Crouching, unsafe space, invalid caster/target or the deadline ends
  the hold with a finite Slow Falling release. It grants no flight ability. Its own three-tick
  Levitation lease yields to an incoming stronger or longer Levitation effect without removing or
  shortening that effect. A payment admits at most eight allied UUIDs; admission rest is eight seconds.
- **Thresherwind:** three rows of three crops pass at separate beats. Each row checks the living
  caster, loaded blocks, permission and line of sight. It uses actual crop drops, spends a matching
  harvested seed for replanting, and collects only its own harvest. Overflow remains as loose output
  reserved for the caster. It does not vacuum arbitrary nearby drops or replant without a seed.

Payment state is shared by repeated casts and does not retain the payment object through its values.
Active mobile maps cap at 128 marks each, with finite expiry. Server stopping clears active maps and
payment state. Mobile actions recheck same-world eligibility and safe loaded space before movement;
line-of-sight sampling checks chunk availability before invoking the world clip.

## Authored presentation

Springbed pours three water threads into a branching seed root. Cinder Sieve closes hot basket ribs
around a dark intake. Ashen Mercy folds scorched leaf pieces toward living tissue through a small
ember seam. Clockroot pins a sand hourglass between unequal roots and reverses the travelling grains.
Skylatch holds open air brackets around a dark tether knot. Thresherwind shears stalk fibres across
three separate lanes with grain following the wind. Each has a separate successful-impact arrangement
and original deterministic synthesized preparation/impact sound.

The first review exposed generic shape and secondary material layers around pure field effects.
The corrected Canvas integration uses the authored field body for groups whose effects are all
covered. The final native suite asserts that pure Cinder Sieve and Springbed preparation emits no
nearby generic LightParticle layer. An actual paid Cinder Sieve + uncovered Umbra cast verifies that
mixed groups still retain their fallback. Ashen Mercy uses the independently authored Life body.

The carved rune textures now reside at the paths actually referenced by their item models. Six
unused outputs from the initial incorrect path were removed by the parent agent. The final native
inventory capture shows the actual model textures, not standalone generator previews.

## Terminal verification

The following focused command completed successfully in **51 seconds**:

```powershell
./gradlew test --tests dev.wildercord.cast.FieldFusionRulesTest --tests dev.wildercord.spell.FusionTest --tests dev.wildercord.spell.RuneChoreographyTest runClientGameTest -PfocusedSuite=dev.wildercord.cast.FieldFusionTest
```

It passed **30 unit cases**: four FieldFusionRules, 23 Fusion, and three RuneChoreography cases.
The choreography gate covers the expanded 304 castable scripts. The native gameplay suite exercises
all six paid Survival casts, six real server altar transactions, resource refusal and shared-payment
limits, ownership and target eligibility, claim veto, boss/permanent-immunity refusal, unsafe remembered
floor refusal, player crouch release, external Levitation takeover, eight-target Mercy admission,
and state cleanup across a full native world reopen.

The following native presentation command completed successfully in **52 seconds**:

```powershell
./gradlew runClientGameTest -PfocusedSuite=dev.wildercord.client.fx.FieldFusionPresentationTest
```

It exercises six actual client Fuse packets with exact resulting runes and ingredient consumption,
the real inventory models, all six paid Bolt preparations and flights under Full and Minimal quality,
independent recipe fingerprints and motion, rear-circle placement, the pure-body cleanup and mixed
fallback case. HUD, window dimensions and quality preferences are restored in the suite's cleanup.

**Authoritative final evidence:** `artifacts/review/field-signature-fusions/final/` contains
`gameplay-and-unit.log`, `presentation.log` and **20 PNGs**: six altar screens, one inventory screen,
six preparations, six flights and one mixed fallback. Earlier evidence outside `final/` predates the
external-lease or Canvas corrections and must not be cited as final acceptance.

The illustrated player chapter is `wiki/magic/field-signatures.md`; its three preserved native images
are under `wiki/assets/field-signatures/`.

## Performance bounds and verification limits

The resource work has explicit limits: sixteen pour cells, eight growth changes, sixteen furnace
inputs from 24 candidates with a 36-slot inventory simulation, nine crop positions across three
scheduled rows, eight distinct targets per payment, and 128 active marks per mobile effect.
Line-of-sight samples are bounded to 256 over at most 128 blocks. Material emissions continue through
the existing effect budget. These are implementation bounds, not newly measured frame-time claims.

The focused native cases do not individually reproduce every death/logout/dimension/border/unloaded
chunk permutation. Those paths have explicit runtime guards, but this report does not convert source
inspection into native coverage. Thresherwind's paid native fixture verifies actual wheat collection
and seed-based replanting; it does not claim a separate native test for every crop's loot table.

Fire, Frost, Earth and Wind exact-roster suite expectations were expanded to include the new effects;
their entire family suites were not rerun as part of these two focused gates. The dedicated new-effect
presentation suite verifies the new ingredient and formation contracts. The Life agent owns the
separate 29-effect Life acceptance run, and the parent owns final generation/build/release checks.

## Integration map

- Runtime: `FieldFusionRules`, `FieldFusions`, `FieldFusionFx`, `FieldFusionFeels`; six definitions in
  Runes/Fusions and the Effects dispatch; item ownership accessor used by Cinder Sieve.
- Presentation: `FieldFusionForms`, Fire/Frost/Earth/Wind family delegates and FlightBodies coverage.
  The Life agent authored Ashen Mercy in LifeForms and the Life flight roster. The parent integrated
  Canvas field-body selection and generic-layer cleanup.
- Assets: `tools/fieldfusion_art.py`, `tools/feel/fieldfusion.py`, its manifest, six rune textures,
  twelve generated OGG cues, and six unique choreography entries.
- Tests: FieldFusionRulesTest, FieldFusionTest, FieldFusionPresentationTest and expanded existing
  fusion/choreography/family roster expectations.
- Common wiring: the parent integrated runtime initialization, asset and sound generator registration,
  and native test entrypoints. Wiki navigation/export remains a parent integration step.

No broad modpack completion, new publication or server deployment is claimed by this milestone audit.
