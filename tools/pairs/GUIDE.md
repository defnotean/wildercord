# Writing pair fusions

Every two elemental runes fuse at the Fusion Altar into a pair rune of their own. Each pair is **written by hand**:
its own name, its own rules text, its own coded effect, and **its own animation, sound and feel**. Nothing is
generated from tables. If two pairs in your file would play the same way with different colours, rewrite one.

Read the reference first: `src/main/java/dev/wildercord/pairs/b000/Pairs000.java` (Thermal Shock, chill + fire).
Every pair should be at least that hand-made.

## Where a pair lives

One file per batch, e.g. `src/main/java/dev/wildercord/pairs/b001/Pairs001.java`:

```java
package dev.wildercord.pairs.b001;

public final class Pairs001 {
	private Pairs001() {}

	@Pair(a = "fire", b = "shock", name = "Arc Furnace", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "What the rune says it does, with its real numbers.")
	public static void fireShock(PairCast c) {
		...
	}

	private static void helper(PairCast c, LivingEntity t) { ... }   // private helpers are fine
}
```

The `@Pair` fields:
- `a`, `b`: the two rune paths, **alphabetical** (`a < b`).
- `name`: 1-32 characters, unique across every pair. Evocative, not "Fire Shock".
- `element`: one of the two runes' elements: the one the spell feels most like.
- `kind`: `HARMFUL` (hurts enemies), `HELPFUL` (helps allies), `MOVEMENT` (moves the caster), or `WORLD`.
- `traits`: which of `"power"`, `"duration"`, `"radius"` the code really scales by (`c.power`, `c.duration`, `c.radius`).
- `text`: 20-320 characters. The **true** numbers the code uses at power 1. Players read it on the rune.

Only edit your own file. Never touch the generated files (`PairIndex.java`, `PairSpecs.java`, `pairs/gen/`).
Don't add static mutable state, threads or new classes outside your file.

## Design: the two runes must both be in it

Read both runes' descriptions (given in your task). The pair must be **a new spell where both halves matter**:
a mechanic that neither rune has alone, an interaction, a sequence, a twist. Examples of the bar:
- chill + fire, *Thermal Shock*: a frost shell closes on the target, then fire inside cracks it, shards fly to neighbours.
- push + venom could be *Miasma Gust*: a cone of poisoned wind; whoever it throws into a wall takes the poison twice.
- heal + stoneskin could be *Mended Stone*: heals, and what heal overflows hardens into stone plates that crack off one by one.

Cover a range: some single-target, some area, some lingering zones, some chains, some delayed detonations, some
that move things, some that help allies. MOVEMENT pairs (blink, dash, leap, grapple...) move the **caster**.

## Balance (keep the 0.12 intent: no free power)

A pair costs about 1.5 times its dearer rune. So its total effect should be about 1.2-1.6 times the stronger rune's.
- Single target damage: roughly the stronger rune's damage + half the weaker's, times `c.power`. 4-12 typical. Never over 18.
- Area damage: 2-6 per enemy times `c.power`. Use `c.enemiesNear(at, r)`: it is budgeted. Radius 2-5 blocks times `c.radius`.
- Healing: 4-10 per ally. Shields (`absorb`) 2-8.
- Effects: amplifier 0-2 (Slowness III is amplifier 2), 2-10 seconds. Times `c.duration` if you list "duration".
- Damage over time: total across the whole time follows the same limits.
- Never move, hold or lift a boss: `c.push`/`knockFrom`/`pullTo`/`lift`/`blink` already refuse one. Don't bypass them.
- At most `PairCast.MAX_TARGETS` (8) primary targets: `PairCast.first(c.enemies(), PairCast.MAX_TARGETS)`.
- No block breaking or placing.

## Animation, sound and feel (required for every pair)

Each pair has **its own choreography over time**, not one burst. At least two beats with `c.every(...)` or
`c.later(...)`: a wind-up/travel, then a payoff (and maybe an aftermath). Pick:
- its own palette (hex colours) that blends both runes' feel: `PairCast.dust(rgb, size)`, `PairCast.shift(from, to, size)`;
- shapes that tell the story: `ring`, `wave`, `line`, `zigzag`, `arc`, `spiral`, `helix`, `sphere`, `column`, `disc`, `star`, `particles`, `mote`;
- vanilla particles where they fit (`ParticleTypes.FLAME`, `SOUL_FIRE_FLAME`, `ELECTRIC_SPARK`, `SNOWFLAKE`, `ITEM_SNOWBALL`, `CLOUD`, `GUST`, `SWEEP_ATTACK`, `CRIT`, `ENCHANTED_HIT`, `DAMAGE_INDICATOR`, `HEART`, `HAPPY_VILLAGER`, `SPORE_BLOSSOM_AIR`, `REVERSE_PORTAL`, `PORTAL`, `WITCH`, `SQUID_INK`, `SCULK_SOUL`, `SOUL`, `END_ROD`, `WAX_ON`, `WAX_OFF`, `ASH`, `WHITE_ASH`, `LAVA`, `SMOKE`, `CAMPFIRE_COSY_SMOKE`, `BUBBLE`, `SPLASH`, `DRIPPING_WATER`, `FALLING_WATER`, `TOTEM_OF_UNDYING`, `ENCHANT`, `GLOW`, `NOTE`, `SONIC_BOOM` (once, it's huge), `EXPLOSION`, `FLASH` needs a colour so avoid);
- two or three `c.sound(SoundEvents.X, at, volume, pitch)`: different ones per beat, tuned with pitch;
- screen feel matched to weight: `c.shake(at, 0.1-0.5F, r)`, `c.punch(0.1-0.4F)` for the caster, `c.tint(at, r, rgb, ticks)` for a flash; `c.bolt(at)` for a visual lightning strike.

Keep particle counts sane: under ~60 points per call, under ~300 a tick per pair.

Inside `every`/`later`, re-check targets with `c.still(list)` or `c.here(t)`: they may have died or left.

## The PairCast API (`dev.wildercord.cast.PairCast`)

Fields: `cast, level, caster, node, hit, power, duration, radius, amplify`.
Constants: `MAX_IN_AREA = 16`, `MAX_TARGETS = 8`.

Who: `enemies()`, `allies()`, `firstEnemy()`, `firstAlly()` (may be null), `static first(list, n)`, `point()`
(where it landed), `origin()` (where it was cast from), `dir()` (its flight direction), `self()`,
`enemiesNear(Vec3 at, double r)`, `alliesNear(at, r)`, `nearestEnemy(at, r, LivingEntity not)` (may be null),
`here(t)`, `movable(t)`, `static mid(Entity)` (body centre), `still(Collection)`.

Damage (amounts in half-hearts): `hurt(t, amount)` magic, `burn` fire, `freeze` cold, `shock` lightning,
`wither`, `strike` physical; `explode(at, r, blastPower)` (no block damage).
Statuses: `effect(t, MobEffects.X, seconds, amplifier)`, `ignite(t, seconds)`, `chill(t, seconds)`,
`mark(t, Reactions.Mark.X)` (FROZEN WINDSWEPT PULLED SOAKED RESONANT WET CRACKED SHADOWED BLEEDING AIRBORNE EXPOSED IONISED),
`heal(t, amount)`, `absorb(t, amount, seconds)`, `douse(t)`, `ticks(seconds)`.
Movement: `push(t, Vec3 impulse)`, `knockFrom(t, from, strength, up)`, `pullTo(t, to, strength)`, `lift(t, up)`,
`blink(e, Vec3 to)` (returns whether it fit), `ground(at)`.
Time: `later(ticks, Runnable)`, `every(everyTicks, times, frame -> {...})` (frame 0 runs now), `lingering(Runnable)`,
`once(key)`, `random()`.
Visuals: `static dust(rgb, size)`, `static shift(fromRgb, toRgb, size)`, `particles(p, at, count, spread, speed)`,
`mote(p, at, velocity)`, `ring(p, at, r, points, turn)`, `wave(p, at, points, speed)`, `line(p, a, b, perBlock)`,
`zigzag(p, a, b, jag, perBlock)`, `arc(p, a, b, height, points)`, `spiral(p, at, r, height, turns, points)`,
`helix(p, q, at, r, height, turns, points)`, `sphere(p, at, r, points)`, `column(p, at, r, height, points)`,
`disc(p, at, r, points)`, `star(p, at, rays, length, turn)`.
Sound and screen: `sound(SoundEvents.X, at, volume, pitch)` (SoundEvent or Holder), `shake(at, strength, r)`,
`punch(strength)`, `tint(at, r, rgb, ticks)`, `bolt(at)`.

Minecraft here uses **Mojang names for Minecraft 26.3**: `MobEffects.SLOWNESS`, `MobEffects.WEAKNESS`, `MobEffects.RESISTANCE`,
`MobEffects.REGENERATION`, `MobEffects.SPEED`, `MobEffects.JUMP_BOOST`, `MobEffects.GLOWING`, `MobEffects.LEVITATION`,
`MobEffects.SLOW_FALLING`, `MobEffects.BLINDNESS`, `MobEffects.DARKNESS`, `MobEffects.POISON`, `MobEffects.WITHER`,
`MobEffects.MINING_FATIGUE`, `MobEffects.STRENGTH`, `MobEffects.ABSORPTION`, `MobEffects.FIRE_RESISTANCE`, `MobEffects.NAUSEA`.
If unsure a name exists, grep the code: `grep -rhoE "SoundEvents\.[A-Z_]+" src/main/java | sort -u`.

## Check your own work (required, until it passes)

```bash
cd /c/Users/Demon/OneDrive/Desktop/wc-magic && bash tools/pairs/check.sh src/main/java/dev/wildercord/pairs/b001/Pairs001.java
```

It validates every `@Pair` (names, order, elements, text length, clashes with other files) and compiles your file
on its own. Fix everything it reports. **Do not run gradle** (other writers share this checkout) and don't commit:
the lead integrates, builds and runs the in-game test that casts every pair.

Then read your file once more as a player would: does each rule text say exactly what the code does, at power 1?
