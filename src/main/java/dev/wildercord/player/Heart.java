package dev.wildercord.player;

import dev.wildercord.Wildercord;
import dev.wildercord.content.CordTier;
import dev.wildercord.spell.Circles;
import dev.wildercord.spell.SpellCompiler;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

/**
 * A player's heart: how many circles it holds, how much mana has condensed toward the next,
 * what the Cord's spell enchantments add, and the player's affinities with the elements. Both sides use it, so the HUD, the Cord screen
 * and the server always show and charge the same numbers.
 */
public final class Heart {
	private Heart() {}

	public static final ResourceKey<Enchantment> POTENCY = ResourceKey.create(Registries.ENCHANTMENT, Wildercord.id("potency"));
	public static final ResourceKey<Enchantment> CELERITY = ResourceKey.create(Registries.ENCHANTMENT, Wildercord.id("celerity"));
	public static final ResourceKey<Enchantment> THRIFT = ResourceKey.create(Registries.ENCHANTMENT, Wildercord.id("thrift"));
	public static final ResourceKey<Enchantment> PERSISTENCE = ResourceKey.create(Registries.ENCHANTMENT, Wildercord.id("persistence"));

	public static int circles(Player player) {
		return Circles.count(player.getAttachedOrElse(WildercordAttachments.CIRCLES, 0));
	}

	/**
	 * Circles that are working right now: overcasting cracks the outermost ones for a while,
	 * and a cracked circle gives nothing (mana, regeneration, power, perks or passive slots).
	 */
	public static int active(Player player) {
		return Math.max(0, circles(player) - cracked(player));
	}

	public static int cracked(Player player) {
		return Math.clamp(player.getAttachedOrElse(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE).active(player.level().getGameTime()), 0, circles(player));
	}

	public static List<String> grimoire(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.GRIMOIRE, List.of());
	}

	public static boolean discovered(Player player, String key) {
		return grimoire(player).contains(key);
	}

	public static String innate(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.INNATE, "");
	}

	public static int runeboundSlain(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.RUNEBOUND_SLAIN, 0);
	}

	/** Casts per element as counted before affinities existed (read once, to give a caster's affinities a start). */
	public static java.util.Map<String, Integer> elementCasts(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.ELEMENT_CASTS, java.util.Map.of());
	}

	/** This player's affinity points with each element (see {@link dev.wildercord.spell.PlayerAffinity}). */
	public static java.util.Map<String, Integer> affinity(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.AFFINITY, java.util.Map.of());
	}

	/** This player's affinity level with {@code element}, 0 (none yet) to 5. */
	public static int affinityLevel(Player player, String element) {
		return dev.wildercord.spell.PlayerAffinity.level(affinity(player).getOrDefault(element, 0));
	}

	/** The element this player's magic leans toward (their deepest affinity, clearly ahead), or "" (always, with affinities off). */
	public static String leaning(Player player) {
		return dev.wildercord.config.Config.playerAffinity(player) ? dev.wildercord.spell.Leaning.of(affinity(player)) : "";
	}

	public static int condensed(Player player) {
		return Math.max(0, player.getAttachedOrElse(WildercordAttachments.CONDENSED, 0));
	}

	public static boolean bossSlain(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.BOSS_SLAIN, false);
	}

	public static int spellKills(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.SPELL_KILLS, 0);
	}

	/** How far along a requirement is (for the tooltip), capped at what it needs. */
	public static int progress(Player player, Circles.Requirement requirement) {
		CordTier tier = Spellbooks.tier(player);
		int have = switch (requirement.need()) {
			// Knots are spells, not runes, and a missing add-on's runes are silent: neither counts (as for the advancements).
			case RUNES -> dev.wildercord.spell.Runes.countKnown(Spellbooks.get(player).learned());
			case CORD -> tier == null ? -1 : tier.ordinal();
			case KILLS -> spellKills(player);
			case BOSS -> bossSlain(player) ? 1 : 0;
			case REACTIONS -> dev.wildercord.spell.Feats.count(grimoire(player), "reaction:");
			case RUNEBOUND -> runeboundSlain(player);
			case SECRETS -> dev.wildercord.spell.Feats.count(grimoire(player), "secret:");
			case FEAT -> discovered(player, "feat:" + requirement.feat()) ? 1 : 0;
		};
		return Math.min(have, requirement.amount());
	}

	public static boolean met(Player player, Circles.Requirement requirement) {
		return progress(player, requirement) >= requirement.amount();
	}

	/** Whether every breakthrough for circle {@code n} has been earned. */
	public static boolean breakthroughMet(Player player, int n) {
		if (n < 1 || n > Circles.MAX) return false;
		for (Circles.Requirement requirement : Circles.requirements(n)) {
			if (!met(player, requirement)) {
				return false;
			}
		}
		return true;
	}

	/** Ready to form the next circle: a Cord is worn, enough mana has condensed, and the breakthrough is earned. */
	public static boolean ready(Player player) {
		int next = circles(player) + 1;
		return player.isAlive() && !player.isSpectator() && next <= Circles.MAX && Spellbooks.tier(player) != null
			&& condensed(player) >= Circles.condenseNeeded(next) && breakthroughMet(player, next);
	}

	// ------------------------------------------------------------------ spell bonuses

	/** How a player's circles and Cord enchantments change their spells. */
	public record Bonuses(double power, double duration, double cost, double cooldown) {
		public static final Bonuses NONE = new Bonuses(1, 1, 1, 1);

		public Bonuses withPower(double power) {
			return new Bonuses(power, duration, cost, cooldown);
		}
	}

	public static Bonuses bonuses(Player player) {
		return bonuses(player, false);
	}

	/** @param overflow the spell is cast at full mana (7th Circle: Overflow) */
	public static Bonuses bonuses(Player player, boolean overflow) {
		int circles = active(player);
		dev.wildercord.spell.CircleVows.Effect vows = vowEffect(player);
		return dev.wildercord.content.RelicCharmItem.apply(player,new Bonuses(
			Circles.power(Mana.enchantLevel(player, POTENCY), circles, overflow) * vows.power(),
			Circles.duration(Mana.enchantLevel(player, PERSISTENCE)) * vows.duration(),
			Circles.cost(Mana.enchantLevel(player, THRIFT), circles) * vows.cost(),
			Circles.cooldown(Mana.enchantLevel(player, CELERITY), circles) * vows.cooldown()));
	}

	/** The saved Circle Vows (old saves: none). */
	public static int vows(Player player) {
		return dev.wildercord.spell.CircleVows.clean(player.getAttachedOrElse(WildercordAttachments.CIRCLE_VOWS, 0));
	}

	/** What the vows of this heart's active circles add up to, with its Heart Path; a cracked circle's vow is silent. */
	public static dev.wildercord.spell.CircleVows.Effect vowEffect(Player player) {
		int active = active(player);
		return dev.wildercord.spell.CircleVows.effect(vows(player), active).plus(dev.wildercord.spell.HeartPaths.effect(path(player), active))
			.plus(dev.wildercord.spell.AscensionRules.effect(ascension(player), active));
	}

	/** Ascensions formed past the Twentieth Circle (old saves: none). */
	public static int ascension(Player player) {
		return dev.wildercord.spell.AscensionRules.clamp(player.getAttachedOrElse(WildercordAttachments.ASCENSION, 0));
	}

	/** Ready to form the next Ascension: all twenty circles unbroken, a Cord worn, and enough mana condensed. */
	public static boolean ascensionReady(Player player) {
		return player.isAlive() && !player.isSpectator() && Spellbooks.tier(player) != null
			&& dev.wildercord.spell.AscensionRules.ready(ascension(player), circles(player), active(player), condensed(player));
	}

	/** The saved Heart Path number (old saves: none). */
	public static int path(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.HEART_PATH, 0);
	}

	/** The Heart Path that speaks for this heart now, or null. */
	public static dev.wildercord.spell.HeartPaths.Path activePath(Player player) {
		return dev.wildercord.spell.HeartPaths.active(path(player), active(player));
	}

	/** Whether a spell cast with {@code mana} counts as cast at full mana (the Path of the Storm wakes Overflow sooner). */
	public static boolean overflowing(Player player, float mana) {
		return dev.wildercord.spell.HeartPaths.overflowing(path(player), active(player), mana, Mana.max(player));
	}

	public static int manaCost(Player player, SpellCompiler.Compiled compiled) {
		return manaCost(player, compiled, 1.0);
	}

	/** @param factor one more factor on the price, e.g. a secret spell's power */
	public static int manaCost(Player player, SpellCompiler.Compiled compiled, double factor) {
		double raw = rawCost(player, compiled) * factor;
		int paid = roundCost(raw);
		// A discount (a staff, Thrift, a mana storm, a heart perk) always saves at least one mana, even
		// where rounding up would swallow it (10% off 9 is still 9 rounded up).
		double undiscounted = compiled.cost() * serverCost(player) * factor;
		int full = roundCost(undiscounted);
		if (raw < undiscounted - 1e-9 && paid >= full && full > 1) {
			paid = full - 1;
		}
		return paid;
	}

	public static int healthCost(Player player, SpellCompiler.Compiled compiled) {
		return healthCost(player, compiled, 1.0);
	}

	/** Blood Price: the same mana price (every factor the same), paid in health. */
	public static int healthCost(Player player, SpellCompiler.Compiled compiled, double factor) {
		return dev.wildercord.spell.SpellNumbers.healthCost(rawCost(player, compiled) * factor);
	}

	/**
	 * A spell's price before rounding. Under a mana storm spells cost less (see cast.events.ManaStorm), and on a
	 * ley crossing (see cast.Climate); casting gear and the server's config are their own factors. Rounded once,
	 * at the very end.
	 */
	private static double rawCost(Player player, SpellCompiler.Compiled compiled) {
		return compiled.cost() * bonuses(player).cost() * dev.wildercord.cast.events.ManaStorm.costFactor(player)
			* gearCost(player, compiled) * serverCost(player) * affinityCost(player, compiled) * dev.wildercord.cast.Climate.costFactor(player)
			* focusedCost(player);
	}

	/** A Focused meal: spells cost less while it lasts. */
	private static double focusedCost(Player player) {
		net.minecraft.world.effect.MobEffectInstance effect = player.getEffect(dev.wildercord.content.WildercordEffects.FOCUSED);
		return dev.wildercord.cooking.CookingRules.focusedCost(effect == null ? 0 : effect.getAmplifier() + 1);
	}

	/**
	 * An affinity at level V: the share of the spell that element's effects make up costs 10% less (a spell
	 * of that element alone, 10% less). Read from the synced attachment, so the readout and HUD match.
	 */
	private static double affinityCost(Player player, SpellCompiler.Compiled compiled) {
		java.util.Map<String, Integer> points = affinity(player);
		if (!dev.wildercord.spell.PlayerAffinity.anyMastered(points) || !dev.wildercord.config.Config.playerAffinity(player)) {
			return 1.0;
		}
		return dev.wildercord.spell.PlayerAffinity.costFactor(SpellCompiler.elementShares(compiled.root()), points);
	}

	/** Rounds a price up to whole mana (a hair's float error never adds one). */
	public static int roundCost(double cost) {
		return (int) Math.ceil(cost - 1e-9);
	}

	/** Casting gear in hand (a staff of the spell's element, a Focus of Thrift): its own factor on a spell's cost. */
	private static double gearCost(Player player, SpellCompiler.Compiled compiled) {
		return dev.wildercord.gear.Gear.costFactor(player, compiled.root());
	}

	/** The server's mana.cost_multiplier (sent to clients, so the readout matches). */
	private static double serverCost(Player player) {
		return dev.wildercord.config.Config.costMultiplier(player);
	}

	public static int cooldownTicks(Player player, SpellCompiler.Compiled compiled) {
		return cooldownTicks(player, compiled, 1.0);
	}

	/** @param factor one more factor on the cooldown, e.g. a found secret spell's ({@link #secretCooldown}) */
	public static int cooldownTicks(Player player, SpellCompiler.Compiled compiled, double factor) {
		if (compiled.root().groups.stream().anyMatch(g -> g.shape.is(dev.wildercord.spell.RelayRules.ID))) return dev.wildercord.spell.RelayRules.REST_TICKS;
        if (compiled.root().groups.stream().anyMatch(g -> g.shape.is(dev.wildercord.spell.ReweaveRules.ID))) return dev.wildercord.spell.ReweaveRules.REST_TICKS;
		if (compiled.root().groups.stream().anyMatch(g -> g.effects.stream().anyMatch(e -> e.effect.is(dev.wildercord.spell.ExciseRules.ID)))) return dev.wildercord.spell.ExciseRules.REST_TICKS;
		for (var lesson : dev.wildercord.spell.LessonPackRules.ALL)
			if (compiled.root().groups.stream().anyMatch(g -> g.effects.stream().anyMatch(e -> e.effect.is(lesson.id)))) return lesson.restTicks;
		return (int) Math.max(5, Math.round(compiled.cooldownTicks() * bonuses(player).cooldown() * factor));
	}

	// ------------------------------------------------------------------ secret spells

	/**
	 * The secret spell {@code runes} spell out, if this player has found it. Only a found secret is
	 * named, priced and timed as one, here and on the server alike: until then everything shows the
	 * ordinary spell (anything else would give it away), and the first cast, which finds it, costs that.
	 */
	public static java.util.Optional<dev.wildercord.spell.Secrets.Secret> foundSecret(Player player, List<dev.wildercord.spell.RuneDef> runes) {
		return dev.wildercord.spell.Secrets.match(runes).filter(secret -> discovered(player, secret.key()));
	}

	/** The factor on a spell's price for this player: a found secret's power, a found resonance's {@link dev.wildercord.spell.Resonance#COST}, otherwise 1. */
	public static double secretCost(Player player, List<dev.wildercord.spell.RuneDef> runes) {
		return foundSecret(player, runes).map(dev.wildercord.spell.Secrets.Secret::power)
			.orElseGet(() -> foundResonance(player, runes).isPresent() ? dev.wildercord.spell.Resonance.COST : 1.0);
	}

	/**
	 * The factor on a spell's cooldown for this player: {@link dev.wildercord.spell.Secrets#COOLDOWN} for a found secret,
	 * {@link dev.wildercord.spell.Resonance#COOLDOWN} for a found resonance, otherwise 1.
	 */
	public static double secretCooldown(Player player, List<dev.wildercord.spell.RuneDef> runes) {
		if (foundSecret(player, runes).isPresent()) {
			return dev.wildercord.spell.Secrets.COOLDOWN;
		}
		return foundResonance(player, runes).isPresent() ? dev.wildercord.spell.Resonance.COOLDOWN : 1.0;
	}

	// ------------------------------------------------------------------ this world's resonances

	/**
	 * The resonance of this world {@code runes} spell out, if this player has found it: read from what the server told
	 * them of the world ({@code world_lore}), the same on both sides. As with a secret, until it's found everything
	 * shows (and the finding cast charges) the ordinary spell, and a resonance they haven't found isn't known here at all.
	 */
	public static java.util.Optional<dev.wildercord.spell.ResonanceLore.View> foundResonance(Player player, List<dev.wildercord.spell.RuneDef> runes) {
		if (runes.size() < 3 || runes.size() > 4) {
			return java.util.Optional.empty();
		}
		List<dev.wildercord.spell.ResonanceLore.View> known = player.getAttachedOrElse(WildercordAttachments.WORLD_LORE, WildercordAttachments.WorldLore.NONE)
			.resonances();
		if (known.isEmpty()) {
			return java.util.Optional.empty();
		}
		List<String> ids = runes.stream().map(dev.wildercord.spell.RuneDef::id).toList();
		for (dev.wildercord.spell.ResonanceLore.View view : known) {
			if (view.matchesIds(ids)) {
				return java.util.Optional.of(view);
			}
		}
		return java.util.Optional.empty();
	}

	/** Mana per second to keep a passive running. */
	public static float upkeep(Player player, SpellCompiler.Compiled compiled) {
		return (float) dev.wildercord.spell.Passives.upkeep(compiled.cost() * bonuses(player).cost() * serverCost(player));
	}
}
