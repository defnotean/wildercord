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
 * and what the Cord's spell enchantments add. Both sides use it, so the HUD, the Cord screen
 * and the server always show and charge the same numbers.
 */
public final class Heart {
	private Heart() {}

	public static final ResourceKey<Enchantment> POTENCY = ResourceKey.create(Registries.ENCHANTMENT, Wildercord.id("potency"));
	public static final ResourceKey<Enchantment> CELERITY = ResourceKey.create(Registries.ENCHANTMENT, Wildercord.id("celerity"));
	public static final ResourceKey<Enchantment> THRIFT = ResourceKey.create(Registries.ENCHANTMENT, Wildercord.id("thrift"));
	public static final ResourceKey<Enchantment> PERSISTENCE = ResourceKey.create(Registries.ENCHANTMENT, Wildercord.id("persistence"));

	public static int circles(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.CIRCLES, 0);
	}

	/**
	 * Circles that are working right now: overcasting cracks the outermost ones for a while,
	 * and a cracked circle gives nothing (mana, regeneration, power, perks or passive slots).
	 */
	public static int active(Player player) {
		return Math.max(0, circles(player) - cracked(player));
	}

	public static int cracked(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE).active(player.level().getGameTime());
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

	public static java.util.Map<String, Integer> elementCasts(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.ELEMENT_CASTS, java.util.Map.of());
	}

	/** The element this player's magic leans toward, or "". */
	public static String leaning(Player player) {
		return dev.wildercord.spell.Leaning.of(elementCasts(player));
	}

	public static int condensed(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.CONDENSED, 0);
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
			// Knots are spells, not runes: they don't count toward knowing runes.
			case RUNES -> (int) Spellbooks.get(player).learned().stream().filter(id -> !dev.wildercord.spell.Knots.isKnot(id)).count();
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
		return next <= Circles.MAX && Spellbooks.tier(player) != null
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
		return new Bonuses(
			Circles.power(Mana.enchantLevel(player, POTENCY), circles, overflow),
			Circles.duration(Mana.enchantLevel(player, PERSISTENCE)),
			Circles.cost(Mana.enchantLevel(player, THRIFT), circles),
			Circles.cooldown(Mana.enchantLevel(player, CELERITY), circles));
	}

	public static int manaCost(Player player, SpellCompiler.Compiled compiled) {
		return (int) Math.ceil(compiled.cost() * bonuses(player).cost() - 1e-9);
	}

	public static int healthCost(Player player, SpellCompiler.Compiled compiled) {
		return dev.wildercord.spell.SpellNumbers.healthCost(compiled.cost() * bonuses(player).cost());
	}

	public static int cooldownTicks(Player player, SpellCompiler.Compiled compiled) {
		return (int) Math.max(5, Math.round(compiled.cooldownTicks() * bonuses(player).cooldown()));
	}

	/** Mana per second to keep a passive running. */
	public static float upkeep(Player player, SpellCompiler.Compiled compiled) {
		return (float) dev.wildercord.spell.Passives.upkeep(compiled.cost() * bonuses(player).cost());
	}
}
