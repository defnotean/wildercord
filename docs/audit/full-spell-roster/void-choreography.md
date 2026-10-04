# Void material choreography — development acceptance

## Implemented scope

The canonical runtime Void roster has 36 effect runes. Each now has an explicit
preparation recipe and a velocity-oriented moving body. Eleven original material
styles have two sprite frames each: folds, jaws, teeth, cloth, haze, shards,
sculk, pressure, shells, remnants and running hounds. Shades uses the hound
silhouette; Phantom retains its separate humanoid remnant.

Preparation assembles visible material in front while the existing casting
circles remain behind the caster. Generic front orb/line scaffolding is suppressed
only for completely authored same-family projectile preparations. Cross-family
preparations can retain that scaffold. Uncovered moving identities retain their
legacy moving layer; malformed or foreign identities do not claim coverage.

Void material uses world light except its deliberately emissive sculk and pressure
styles. Reduced-flash alpha is respected. Preparation fragments last eight ticks;
projectile fragments last five. Constructor, map codec and network codec bound
size, lifetime, drift and spin and reject nonfinite input. Minimal quality keeps
the defining material and every required supporting ingredient.

## Roster and visual intentions

| Group | Individually authored identities |
| --- | --- |
| Gathering, pressure and navigation | Pull, Sonic Boom, Gravity Well, Grapple, Collect, Echolocate, Resonant Shriek |
| Folds and displacement | Blink, Zipper, Shadowstep, Banish, Warp, Portalfall, Warp Step, Riftcall |
| Cover, shells and concealment | Veil, Blind, Infinity, Anchor, Shulkershell, Hush, Eclipse |
| Teeth, curses and consumption | Wither, Blackspark, Hollow, Hex, Devour, Singularity, Starmaw, Umbra |
| Distinct mixed material | Blackflame (Fire), Warp (Wind), Entropy (Time), Devour (Blood), Malison (Arcane) |
| Creature and afterimage silhouettes | Shades (hounds), Phantom (humanoid remnants) |
| Breath | Dragon Breath (staggered haze and jaw) |

Some identities appear in two rows because their ingredients and function both
matter. Runtime exact-roster assertions, rather than this reading aid, establish
all 36 identities. Preparation and moving recipe traces differ for every rune
and evolve over time; that check does not prove complete gameplay identities.

## Focused native evidence

Final runs are retained under `artifacts/review/void-choreography/final/`.
These are Windows Minecraft client tests, not injected particle galleries.

| Suite | Result | What it establishes |
| --- | --- | --- |
| VoidRecipeTest | Passed, 31 s | Exact runtime roster; distinct changing recipes, bounded emission, Full/Minimal supporting ingredients; nonvacuous vertical/stationary emission |
| VoidFormationTest | Passed, 1 min 13 s | Native visible preparation, actual particle provider and original frames |
| VoidFlightTest | Passed, 2 min 9 s | All 36 paid Bolt launches in Full/Minimal; synchronized identity, motion, five-tick authored material, particle-clear recovery, removed-entity retirement; foreign innate refusal and owned Phantom acceptance |
| VoidWardDeliveryTest | Passed, 30 s | Paid Self Veil, Anchor, Infinity, Shulkershell and Phantom; caster-bound materials, no projectile; real invisibility, anchor, enemy slowing, shell status and exactly one afterimage |
| VoidMovementDeliveryTest | Passed, 38 s | Actual paid Blink safe arrival, Warp exchange, Warp Step delayed return and real crouch stay choice; actual Zipper wall protection refusal then allowed safe traversal |
| VoidWireBoundsTest | Passed, 21 s | Material network/map round trips and rejected invalid or resource-exhausting data |

195 original screenshots accompany these six runs. Ordinary preparation/flight
captures are supplemented by paired diagnostic close views: the real paid
projectile's retained production particles are re-admitted once to the native
engine at a fixed camera pose. Those close views are labelled diagnostics, not
ordinary player camera frames or proof of a natural impact.

## Remaining gates and limits

The alternate delivery gate passed in `variants-second/native.log` (52 s).
Twelve actual paid cases cover Arc Pull/Wither, Pierce Umbra, Frugal Collect,
covered Umbra+Shock and uncovered Umbra+Harm in Full and Minimal. Assertions
check real velocity/gravity, synchronized effect/style metadata, defining
Void styles, retained Storm material, coverage fallback, clear/restart and
retirement. A separate actual paid piercing Umbra damages two real Husks.
Sampled world lighting, deliberate sculk brightness, Reduced Flash and
nonvacuous vertical/stationary emission also pass.

The first variant run failed its diagnostic image comparison because it moved
the review camera and captured in one step. The corrected fixture settles the
actual camera for two ticks before capture. Production particle selection and
the pixel-difference thresholds are unchanged. Its 24 original diagnostic frames
are retained separately from ordinary player-camera captures.

Affected older-family variant regressions passed: Earth 49 s, Frost 33 s, Storm 33 s,
Wind 32 s and Life 31 s. Covered mixed Umbra cases remain covered, while separate
Harm cases establish an actual uncovered fallback. The Ward suite (33 s) and
Movement suite (38 s) also passed after prior camera/key state restoration was
added to their fixtures. Logs and original screenshots are in `regression/`.

This milestone does not claim all 36 impacts, persistent fields, summons, trap,
beam, rain or sound lifecycles, or actual multiplayer recipient acceptance.
The proposed post-bonus damage admission and permanent reflection provenance
seams are staged outside this milestone for the next counterplay additions;
their dedicated gameplay acceptance remains pending.

The full Linux gameplay gate failed at the prior milestone's FungalNurseryTest.
Its correction and full-descriptor rerun remain separately tracked. The public
0.9.1-alpha.1 release and installed server do not contain this development work.
