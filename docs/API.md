# The add-on API

Another mod can add runes to Wildercord, react to spells, and read a player's magic through the
`dev.wildercord.api` package. This page walks through it; the javadoc in the package has every detail.

- [Setting up](#setting-up)
- [Runes](#runes)
- [Effects](#effects)
- [Shapes](#shapes)
- [Modifiers](#modifiers)
- [Links](#links)
- [Categories](#categories)
- [Element reactions](#element-reactions)
- [Events](#events)
- [Reading a player's magic](#reading-a-players-magic)
- [Names, icons and recipes](#names-icons-and-recipes)
- [Rules and guarantees](#rules-and-guarantees)
- [A complete example](#a-complete-example)

## Setting up

Depend on Wildercord, then implement `WildercordAddon` and list it under the `wildercord` entrypoint
in your `fabric.mod.json`:

```json
"entrypoints": {
  "wildercord": ["com.example.magic.ExampleAddon"]
},
"depends": {
  "wildercord": "*"
}
```

```java
public class ExampleAddon implements WildercordAddon {
	@Override
	public void onWildercordInit(WildercordApi api) {
		// register runes, categories, reactions and listeners here
	}
}
```

Wildercord calls `onWildercordInit` once, on both the client and the server, at the end of its own
start-up. Register everything there: the client needs the same runes to show them, so everything you
register must be the same on both sides. `WildercordApi.get()` returns the same API object anywhere
else.

## Runes

Every rune is a `RuneDef` (see [ARCHITECTURE.md](ARCHITECTURE.md#runes-are-data)): an id, a name, a
family, a tier (which Cord can hold it), a cost, traits (what modifiers may change on it) and a
description. Build one with the API and register it:

| Start with | Makes | Its behaviour |
|---|---|---|
| `api.effect(id, name, element, kind)` | an effect: what happens to whatever the shape hit | `.onApply(ctx -> ...)` |
| `api.shape(id, name)` | a shape: where the spell goes | `.onDeliver(ctx -> ...)` |
| `api.modifier(id, name)` | a modifier: numbers on the rune to its left | `.numbers(power, duration, radius)` |
| `api.link(id, name)` | a link: when the rest fires | `.onLink(ctx -> ...)` |

Then `.tier(1-4)`, `.cost(mana)`, `.multiplier(x)` (shapes: on their effects' cost; modifiers: on what
they attach to), `.traits(Trait.POWER, ...)`, `.needs(Trait.X)` (modifiers), `.category(name)`,
`.description(text)`, and finally `.register()`, which checks the rune and returns its `RuneDef`.

Ids must be in your own namespace (`example:frostbite`, never `wildercord:...`), lower case. A bad
rune throws `IllegalArgumentException` from `register()`, so mistakes show up at start-up.

Your runes work with every other rune, built-in or from other add-ons, because they all speak the same
packet: shapes find targets, effects act on them, modifiers change numbers, links schedule the rest.
Effects automatically get `Trait.FRUGAL`, shapes `Trait.COOLDOWN`, just like built-in ones.

## Effects

```java
api.effect("example:drench", "Drench", "frost", EffectKind.HARMFUL)
	.tier(1).cost(5).traits(Trait.DURATION)
	.description("Soaks targets for 6 seconds and slows them.")
	.onApply(ctx -> {
		for (LivingEntity target : ctx.harmed()) {
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, (int) Math.round(120 * ctx.duration()), 0));
		}
	})
	.register();
```

The `EffectContext` has:

- `harmed()` (fair game: never the caster, their team or pets, and already past any Shield) and
  `helped()` (the caster and allies). The **kind** decides which list matters: `HARMFUL`, `HELPFUL`,
  `WORLD` (blocks and points), `MOVEMENT` (moves the caster).
- `point()`, `direction()`, `block()`, `face()`, `self()`: what the shape hit.
- `power()` and `duration()`: multipliers from its modifiers, the shape, Heart Circles, Cord
  enchantments, the charge, rhythm, the caster's affinity with its element and casting gear. Multiply your numbers by them.
- `hurt(target, source, amount)`: **always deal damage through this.** It skips invulnerability frames,
  applies Execute, element reactions, Unison and PvP scaling, meets Shields and counts spell kills.
  `magic()` is a ready-made damage source.
- `mayEdit(pos)`: **check it before changing any block.** It's false when the server has turned block
  editing off, when the caster isn't a player allowed to build there (claims and spawn protection are
  asked), or when the cast's block budget is spent. A true answer uses one block of the budget.
- `later(ticks, task)` and `alive()`: schedule follow-ups, and stop them once the caster leaves, dies or
  the cast is cut short.
- `count(modifier)`: how many of a modifier are attached, for your own modifiers.

The caster may be a monster (Runebound and bosses cast runes too), so never assume a `ServerPlayer`.

## Shapes

```java
api.shape("example:halo", "Halo").tier(2).cost(5).multiplier(1.8).traits(Trait.RADIUS)
	.onDeliver(ctx -> {
		Vec3 centre = ctx.caster().position().add(0, 1, 0);
		List<LivingEntity> near = ctx.near(centre, 3.0 * ctx.radius());
		near.remove(ctx.caster());
		ctx.hit(near, centre, null, null);
	})
	.register();
```

`ShapeContext.hit(entities, point, block, face)` applies the group's effects to what you hit and fires any
On Hit or On Kill after it. `origin()`, `direction()` and `fromCaster()` say where the group starts (after
a link, from whatever set it off); `aimPoint()` is where the caster looks; `radius()` and `copies()`
read Widen, Focus and Split. A shape that strikes again and again should take `ctx.pulse()` for each
strike (a fresh creature and block budget), create it **before** waiting, and check `alive()` when the
strike comes.

## Modifiers

```java
api.modifier("example:brutal", "Brutal").tier(2).multiplier(1.5).needs(Trait.POWER).numbers(1.8, 1.0, 1.0).register();
```

A modifier attaches to the closest rune on its left with the trait it `needs`. `multiplier` scales the
cost of what it attaches to, and `numbers(power, duration, radius)` scale that rune's numbers. The Cord
screen's readout and the cast both use them. For anything more, read `ctx.count(yourModifier)` in your own
effects and shapes.

## Links

```java
api.link("example:at_dusk", "At Dusk").tier(2).cost(2)
	.onLink(ctx -> ctx.later(40, () -> {
		if (ctx.alive()) {
			ctx.fireAt(ctx.caster().getEyePosition(), ctx.caster().getLookAngle(), ctx.caster());
		}
	}))
	.register();
```

The link's behaviour runs once the groups before it have gone off. Call `fire()` (from where the segment
started) or `fireAt(position, direction, entity)` now, later, several times or never. What follows the
link starts with the implicit shape *Target*, so effects with no shape of their own land on the entity or
point you fire at. Its readout header is the link's name.

## Categories

The Codex groups runes by category within each family (Damage, Control, Support...). Use a built-in one
(`RuneCategories`), or add your own with `api.registerCategory(RuneFamily.EFFECT, "weather")` (or just
name it in `.category(...)`). Its label is the lang key `category.wildercord.<family>.<category>`, e.g.
`category.wildercord.effect.weather`, in your own `en_us.json`.

## Element reactions

```java
api.registerReaction("example:steam", "fire", (caster, target) -> {
	if (!api.hasMark(target, "example:soaked")) {
		return 1.0;
	}
	api.clearMark(target, "example:soaked");
	return 1.4;
});
```

Every point of spell damage of the reaction's element asks it for a damage multiplier. Leave and read
marks with `api.mark(entity, key, ticks)`, `api.hasMark` and `api.clearMark` (use your own namespace for
keys). Registering the same reaction id again replaces it.

## Events

All on the server thread, in `WildercordEvents`:

| Event | When | Can stop it |
|---|---|---|
| `BEFORE_CAST` | a player's spell passed every check, before anything is spent (once per cast: after an overcast's confirming second press; `cost` is 0 for a free recast or a Blood Price spell) | return false |
| `AFTER_CAST` | a player's spell was paid for and is on its way | |
| `SPELL_HIT` | a shape hit something, just before its effects apply (players' and monsters' spells) | |
| `SPELL_BLOCKED` | a Shield stopped a spell (a parry too) | |
| `IMBUE_RELEASED` | an imbued weapon, tool, armour, arrow or glyph let its spell go | |

```java
WildercordEvents.AFTER_CAST.register((player, spell, runes, spent) -> { ... });
```

`spell` is the slot: 0-3 the Cord's spells, 4 the Tome of the Fifth Page's.

## Reading a player's magic

`api.mana(player)`, `api.maxMana(player)`, `api.knownRunes(player)`, `api.knows(player, runeId)`,
`api.spells(player)` (rune ids per slot, silent runes included), `api.selectedSpell(player)` and
`api.wearsCord(player)`. They work on both sides; a client only knows its own player's.

## Names, icons and recipes

- **Names and descriptions** come from the rune (`name`, `description`), and can be translated with
  the lang keys `rune.<namespace>.<path>` and `rune.<namespace>.<path>.desc`.
- **Items**: every rune is the one `wildercord:rune` item with a component naming the rune, so there is
  nothing to register. `RuneItem.stack(rune)` makes one (for your recipes, loot or creative tab).
- **Icons**: an add-on rune shows the Silent Rune icon for now; the rune item's model picks icons for
  built-in runes only.
- **Recipes and loot**: ordinary datapack recipes and loot tables, with a `wildercord:rune` component on
  the `wildercord:rune` result.

## Rules and guarantees

- **Server-authoritative.** Everything a behaviour does runs on the server. Casts, edits and
  costs are checked there.
- **Budgets hold.** Hits go through the cast's creature budget (64 by default, the server's
  `casting.max_creatures_per_cast`), blocks through its block budget, and links through its depth limit.
- **Missing add-ons are safe.** If your mod is removed, its runes stay in players' spellbooks as Silent
  Runes, skipped when casting, and wake up again when it's back. Keep your ids stable.
- **The API only grows.** Within 1.x, public types and methods in `dev.wildercord.api` (and
  `RuneDef`, `Trait`, `EffectKind`, `RuneFamily`) are only added to, never removed or renamed.
  Everything else in Wildercord is internal.

## A complete example

`src/test/java/dev/wildercord/api/ExampleAddon.java` is a whole add-on: an effect that soaks its
targets, a reaction that makes fire burst into steam on soaked creatures, a modifier, a shape that
strikes twice, a link that waits, a category, and an event listener. `ApiRegistrationTest` registers it
and checks that its runes read and cost like built-in ones.
