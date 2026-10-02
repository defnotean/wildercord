package dev.wildercord.aura;

import dev.wildercord.api.AuraApi;
import dev.wildercord.cast.Spirits;
import dev.wildercord.player.WildercordAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * What a bonded blade's trait does, where it does it (the numbers are {@link BladeRules}'). Each answers only for a swordsman holding
 * their own blade, Awakened or more for them, with traits on: {@link BondedBlades#heldTrait} and {@link BondedBlades#heldStrength}
 * (half again at Soulforged). Everything that touches a player is held: a damage bonus is a bonus inside {@code AuraCombat}'s
 * spell-defence cap and PvP scale (and half of it, or none at all), stance against a player is half and inside its caps, and nothing
 * here ignores armour, kills through a totem, or stacks with itself.
 *
 * <p>Read on both sides where the client predicts (an art's price and rest, the slash's price on the HUD): the blade's trait travels
 * on the blade, so the swordsman's own client knows it.</p>
 */
public final class BladeTraits {
	private BladeTraits() {}

	/** Each swordsman's Riposte: the next coated blow after a perfect guard, until when. */
	private static final Map<UUID, Long> RIPOSTE = new HashMap<>();

	private static boolean has(Player player, String trait) {
		return trait.equals(BondedBlades.heldTrait(player));
	}

	private static double strength(Player player) {
		return BondedBlades.heldStrength(player);
	}

	/** A foe counts as mighty for Mountainfeller: a boss or a Runebound one (never a player). */
	public static boolean mighty(LivingEntity target) {
		return !(target instanceof Player) && (Spirits.isBoss(target) || target.hasAttached(WildercordAttachments.RUNEBOUND));
	}

	// ------------------------------------------------------------------ prices and rests (both sides)

	/** What an art costs {@code player}, times this (Well-Worn Verse on its favourite art, Inkbound Steel on a technique). */
	public static double price(Player player, AuraApi.StringArt art) {
		String trait = BondedBlades.heldTrait(player);
		if (trait.isEmpty() || art == null) {
			return 1.0;
		}
		if (trait.equals(BladeRules.WELL_WORN)) {
			BladeBond b = BondedBlades.held(player);
			return b != null && art.id().equals(b.growth().traitArt()) ? BladeRules.scaled(BladeRules.WELL_WORN_PRICE, strength(player)) : 1.0;
		}
		if (trait.equals(BladeRules.INKBOUND) && TechniqueRules.slotOf(art.id()) >= 0) {
			return BladeRules.scaled(BladeRules.INKBOUND_PRICE, strength(player));
		}
		return 1.0;
	}

	/** How long an art rests for {@code player}, times this (Well-Worn Verse on its favourite art). */
	public static double rest(Player player, AuraApi.StringArt art) {
		if (art == null || !has(player, BladeRules.WELL_WORN)) {
			return 1.0;
		}
		BladeBond b = BondedBlades.held(player);
		return b != null && art.id().equals(b.growth().traitArt()) ? BladeRules.scaled(BladeRules.WELL_WORN_REST, strength(player)) : 1.0;
	}

	/** An art's rest for {@code player}, in ticks. */
	public static int restTicks(Player player, AuraApi.StringArt art) {
		return (int) Math.round(art.cooldownTicks() * rest(player, art));
	}

	/** Aura Step's price for {@code player}, times this (Wind Step). */
	public static double stepPrice(Player player) {
		return has(player, BladeRules.WIND_STEP) ? BladeRules.scaled(BladeRules.WIND_STEP_PRICE, strength(player)) : 1.0;
	}

	/** Aura Step's rest for {@code player}, times this (Wind Step). */
	public static double stepRest(Player player) {
		return has(player, BladeRules.WIND_STEP) ? BladeRules.scaled(BladeRules.WIND_STEP_REST, strength(player)) : 1.0;
	}

	/** Aura Slash's price for {@code player}, times this (Long Crescent). */
	public static double slashPrice(Player player) {
		return has(player, BladeRules.LONG_CRESCENT) ? BladeRules.scaled(BladeRules.LONG_CRESCENT_PRICE, strength(player)) : 1.0;
	}

	/** How far Aura Slash flies for {@code player}, times this (Long Crescent). */
	public static double slashReach(Player player) {
		return has(player, BladeRules.LONG_CRESCENT) ? BladeRules.scaled(BladeRules.LONG_CRESCENT_REACH, strength(player)) : 1.0;
	}

	/** How fast {@code player}'s techniques rank, times this (Inkbound Steel). */
	public static double techniqueXp(Player player) {
		return has(player, BladeRules.INKBOUND) ? BladeRules.scaled(BladeRules.INKBOUND_XP, strength(player)) : 1.0;
	}

	/** How long the awakening rests before the next, times this (Second Blaze). */
	public static double awakeningRest(Player player) {
		return has(player, BladeRules.SECOND_BLAZE) ? BladeRules.scaled(BladeRules.SECOND_BLAZE_REST, strength(player)) : 1.0;
	}

	/** How long the swordsman is spent after an awakening, times this (Second Blaze). */
	public static double awakeningSpent(Player player) {
		return has(player, BladeRules.SECOND_BLAZE) ? BladeRules.scaled(BladeRules.SECOND_BLAZE_SPENT, strength(player)) : 1.0;
	}

	// ------------------------------------------------------------------ in a fight (server)

	/** A perfect guard of {@code player}'s: Riposte readies the next coated blow. */
	static void guarded(ServerPlayer player) {
		if (has(player, BladeRules.RIPOSTE)) {
			RIPOSTE.put(player.getUUID(), player.level().getGameTime() + BladeRules.RIPOSTE_TICKS);
		}
	}

	/** Whether {@code player}'s Riposte waits on their next coated blow (the game tests ask). */
	public static boolean riposteReady(ServerPlayer player) {
		Long until = RIPOSTE.get(player.getUUID());
		return until != null && player.level().getGameTime() <= until;
	}

	/**
	 * What a coated blow of {@code player}'s on {@code target} is times, by their trait (a bonus like the coat's: {@code AuraCombat.coat}
	 * holds it to the spell-defence cap and the PvP scale against a player). Riposte is spent by the blow it answers (half against a
	 * player); Mountainfeller and Gravewarden never touch a player.
	 */
	public static double coat(ServerPlayer player, LivingEntity target) {
		String trait = BondedBlades.heldTrait(player);
		if (trait.isEmpty()) {
			return 1.0;
		}
		double s = strength(player);
		switch (trait) {
			case BladeRules.RIPOSTE -> {
				Long until = RIPOSTE.remove(player.getUUID());
				if (until != null && player.level().getGameTime() <= until) {
					return BladeRules.scaled(BladeRules.RIPOSTE_DAMAGE, target instanceof Player ? s * 0.5 : s);
				}
				return 1.0;
			}
			case BladeRules.MOUNTAINFELLER -> {
				return mighty(target) ? BladeRules.scaled(BladeRules.MOUNTAINFELLER_DAMAGE, s) : 1.0;
			}
			case BladeRules.GRAVEWARDEN -> {
				return !(target instanceof Player) && target.is(EntityTypeTags.UNDEAD) ? BladeRules.scaled(BladeRules.GRAVEWARDEN_DAMAGE, s) : 1.0;
			}
			default -> {
				return 1.0;
			}
		}
	}

	/** What an art's strike of {@code player}'s on {@code target} is times (Mountainfeller, never on a player). */
	public static double art(ServerPlayer player, LivingEntity target) {
		return has(player, BladeRules.MOUNTAINFELLER) && mighty(target) ? BladeRules.scaled(BladeRules.MOUNTAINFELLER_DAMAGE, strength(player)) : 1.0;
	}

	/**
	 * Stance worn (an {@code onStance} hook): Sundering Steel wears every blow, art and slash harder (half that on a player), Mountainfeller
	 * wears a boss's or a Runebound foe's harder. A perfect guard's or a held guard's wear is the guard's own, not the blade's.
	 */
	static double stance(ServerPlayer attacker, LivingEntity target, double wear, StanceRules.Source source) {
		if (source == StanceRules.Source.GUARD || source == StanceRules.Source.GUARDED) {
			return wear;
		}
		String trait = BondedBlades.heldTrait(attacker);
		if (trait.isEmpty()) {
			return wear;
		}
		double s = strength(attacker);
		if (trait.equals(BladeRules.SUNDERING_STEEL)) {
			return wear * BladeRules.scaled(BladeRules.SUNDERING_WEAR, target instanceof Player ? s * 0.5 : s);
		}
		if (trait.equals(BladeRules.MOUNTAINFELLER) && mighty(target)) {
			return wear * BladeRules.scaled(BladeRules.MOUNTAINFELLER_STANCE, s);
		}
		return wear;
	}

	/** The aura a finisher gives back to {@code player}, times this (Closing Stroke). */
	public static double finisherAura(ServerPlayer player) {
		return has(player, BladeRules.CLOSING_STROKE) ? BladeRules.scaled(BladeRules.CLOSING_AURA, strength(player)) : 1.0;
	}

	/** A finisher of {@code attacker}'s landed: Closing Stroke's momentum, Rallying Steel's for allied swordsmen near. */
	static void finished(ServerPlayer attacker, LivingEntity target) {
		String trait = BondedBlades.heldTrait(attacker);
		if (trait.isEmpty() || Momentum.practice(attacker, target)) {
			return;
		}
		double s = strength(attacker);
		if (trait.equals(BladeRules.CLOSING_STROKE)) {
			AuraApi.addMomentum(attacker, BladeRules.CLOSING_MOMENTUM * s, "blade");
		} else if (trait.equals(BladeRules.RALLYING_STEEL)) {
			double range = BladeRules.RALLYING_RANGE;
			for (ServerPlayer ally : attacker.level().getEntitiesOfClass(ServerPlayer.class, attacker.getBoundingBox().inflate(range),
					p -> p != attacker && p.isAlive() && !p.isSpectator() && p.distanceToSqr(attacker) <= range * range && WayBanner.ally(attacker, p))) {
				// "banner": what an ally is given is never shared on again.
				AuraApi.addMomentum(ally, BladeRules.RALLYING_MOMENTUM * s, "banner");
			}
		}
	}

	/**
	 * What a coated blow's aura is times for {@code player} (an {@code onGain} hook on "hit"): their blade's tier (Named and Soulforged
	 * draw more), and Moonwake by night.
	 */
	public static double hitGain(ServerPlayer player) {
		if (!BondedBlades.on(player)) {
			return 1.0;
		}
		double gain = BladeRules.hitGain(BondedBlades.heldTier(player));
		if (has(player, BladeRules.MOONWAKE) && player.level().dimension() == Level.OVERWORLD && !player.level().isBrightOutside()) {
			gain *= BladeRules.scaled(BladeRules.MOONWAKE_GAIN, strength(player));
		}
		return gain;
	}

	/** Harm about to reach {@code self} (Last Light: below a third of their health, a foe's harm lands lighter). */
	public static float harm(LivingEntity self, DamageSource source, float amount) {
		if (!(self instanceof ServerPlayer player) || amount <= 0 || source.getEntity() == null || source.getEntity() == self) {
			return amount;
		}
		if (player.getHealth() >= player.getMaxHealth() * BladeRules.LAST_LIGHT_BELOW || !has(player, BladeRules.LAST_LIGHT)) {
			return amount;
		}
		return (float) (amount * BladeRules.scaled(BladeRules.LAST_LIGHT_HARM, strength(player)));
	}

	static void forget(UUID id) {
		RIPOSTE.remove(id);
	}

	static void clear() {
		RIPOSTE.clear();
	}
}
