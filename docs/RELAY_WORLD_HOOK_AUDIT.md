# Relay inherited world-hook inventory

This finite inventory follows the only accepted payloads, `Relay + Harm`, `Relay + Frost` and
`Relay + Shock`, through `WorldRules.of`, `Effects.afterEffect` and `WorldMagic.onSpell`.
It is source review, not native execution evidence.

All Relay post-effect hooks remain in the original effect's saved context until the existing
`finally` restores every changed field. The ordinary spell path retains its previous outer-context
boundary. Every guarded terrain write reserves the original paid block allowance through the
same `WorldMagic.edit` admission; claim callbacks are followed by another receipt/point check.

| Payload and reachable branch | Mutation or effect | Relay admission and budget |
| --- | --- | --- |
| Harm → SHIMMER → shimmer | Glowing on nearby invisible foes | Bounded collateral query; current original-body/party/visibility policy; impact-to-victim sightline; one paid creature debit before each status write; overflow refuses the query |
| Harm → shimmer | Bookshelf/enchanting-table particles and sound | Bounded existing scan and visual count; no entity status, control, conversion or block write |
| Frost → soak | Frozen ticks and Frozen reaction mark on the already-hit wet primary | Current victim admission at the mutation; the primary already used its creature allowance; mark also inherits the active Cast context |
| Frost → FREEZE → snuff/blowOut | Remove fire; extinguish campfire, candle or candle cake | Per-cell `edit` checks original lifetime, caster/focus visibility, total path, ward membership, build rights and shared block debit |
| Frost → freeze | Place frosted ice on unobstructed source-water surfaces | Same per-cell `edit`; existing freeze count, radius and occupancy cap |
| Frost → crust | Place temporary basalt over unobstructed source lava | Same per-cell `edit`; existing crust cap; records only accepted cells |
| Frost → thawLater/crustWarns/meltCrust | Scheduled thaw, warning magma and restoration of accepted terrain | Existing bounded, identity-checked world-result lifecycle; no new spell receipt or new target acquisition. Valid placed terrain is not rolled back merely because focus expires |
| Frost → watchBridge | Optional feat when the player later walks on accepted ice | Watches only accepted cells with existing expiry and actual movement condition; no additional damage/status/terrain write |
| Shock → CONDUCT → conduct | Damage to creatures sharing connected water | Existing bounded water traversal; bounded victim query; current admission, secondary sightline, shared creature and damage debit; actual water-arc origin; every nested callback remains in the same Cast context |
| Shock → strike → charge | Charge the already-hit uncharged creeper | Current victim admission immediately before the roll and conversion; skipped after conduction retires the receipt; no extra RNG draws |
| Shock → strike | Scrape copper oxidation and pulse lightning rods | Per-cell `edit` with shared block debit; existing target/block-count bounds; rod's later switch-off remains the accepted device response |

`WorldRules.wets` is false for all three payloads. Fire, wind, earth, life, void, time and blood
branches are unreachable under the original-row grammar. `frostWater` is an Aura entrypoint and
is not called by Relay. Shock does not call a real lightning projectile/entity.

The adjacent shared hooks were also traced:

- Mastery strike statuses recheck the original victim; chain uses bounded admitted collateral,
  its own incoming leg and the paid creature allowance. Residue sinks reached from direct or
  world/arena damage inherit the original Cast and paid block allowance.
- Tide conduction and Frost control use the same victim policy and creature allowance; native
  arena ice uses block admission/debit. An over-cap active arena set is refused before traversal.
- The initiating matching Rune Seal must pass point admission, including its own solid surface.
  Its accepted connected-door response remains the existing bounded device action, `MAX_DOOR=96`.
- Mirrors/parries retain original lifetime and payment; Reprieve records debt only after valid
  post-callback admission. Already accepted reduced debt remains the ordinary owed wound.
- TwistMagic is not armed by Relay placement. Mirrorfrost/Star-Eater copies of Relay plans are
  refused by the generic CastEngine route. A pre-existing Spellbrand burst belongs to its earlier,
  independently paid Brand cast.

Native source covers inherited world residue with a shielded primary, zero-block allowance,
context restoration, pinned allowed/retired creeper conversion, initiating seal visibility and
Harm shimmer's positive, exhausted-budget, cover and overflow cases. Earlier suites cover
Frost/water/Tide limits, callbacks, original-body retirement, reflections and Reprieve. Actual
server/client execution and final independent review remain separate acceptance gates.
