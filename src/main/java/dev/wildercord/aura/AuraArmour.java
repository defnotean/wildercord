package dev.wildercord.aura;

import dev.wildercord.config.Config;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Aura armour (Form, always on): while a player holds aura enough ({@link AuraRules#ARMOUR_MIN}), it takes a share of what
 * reaches them (blows, arrows, spells, fire, blasts), paying aura for each point it takes; it never spends below that floor,
 * so it fades rather than breaks. A faint shell of aura is seen round them while it's up, flaring where it takes a blow.
 * Falling, drowning, starving and suffocating are the body's own troubles: it takes none of those.
 *
 * <p>Damage reaches it from {@code mixin.LivingEntityAuraMixin}, after Aura Guard has had its say; it's paid for only when the
 * blow really lands (a hit turned away by a moment of invulnerability costs nothing).</p>
 */
public final class AuraArmour {
	private AuraArmour() {}

	/** The shell flares at most this often (ticks), however fast the blows come. */
	private static final int FLARE_REST = 4;

	/** Whether aura armour is up for {@code player}: Form, aura working, and aura enough. */
	public static boolean up(Player player) {
		return Aura.enabled(player) && Aura.stage(player) >= AuraRules.FORM && Aura.aura(player) >= AuraRules.ARMOUR_MIN - 1.0E-4;
	}

	/** Whether the armour takes any of this harm. */
	static boolean covers(DamageSource source) {
		return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.is(DamageTypeTags.IS_FALL) && !source.is(DamageTypeTags.IS_DROWNING)
			&& !source.is(DamageTypes.STARVE) && !source.is(DamageTypes.IN_WALL) && !source.is(DamageTypes.CRAMMING)
			&& !source.is(DamageTypes.OUTSIDE_BORDER) && !source.is(DamageTypes.FELL_OUT_OF_WORLD) && !source.is(DamageTypes.GENERIC_KILL);
	}

	/** How much of {@code damage} the armour would take off (nothing is spent yet: see {@link #paid}). */
	public static float share(LivingEntity target, DamageSource source, float damage) {
		if (!(target instanceof ServerPlayer player) || damage <= 0 || !up(player) || !covers(source)) {
			return 0;
		}
		// The Way of the Bulwark at Form: sturdier, taking more for less.
		return (float) AuraRules.armourAbsorb(damage, Aura.aura(player), WayEffects.armourShare(player, Config.get().aura().heights().armourShare()),
			WayEffects.armourCost(player));
	}

	/** The blow landed with {@code absorbed} taken off it: the armour pays, and its shell flares. */
	public static void paid(LivingEntity target, DamageSource source, float absorbed) {
		if (!(target instanceof ServerPlayer player) || absorbed <= 0) {
			return;
		}
		Aura.spend(player, absorbed * AuraRules.ARMOUR_COST_PER_POINT * WayEffects.armourCost(player), "armour");
		long now = player.level().getGameTime();
		AuraPresence.Look look = AuraPresence.look(player);
		if (now - look.shellStruckAt() >= FLARE_REST) {
			AuraPresence.look(player, look.struck(now));
			AuraVfx.shellStruck(player, Aura.color(player), source);
			Aura.sound(player, "aura_armour", 0.55F, 0.9F + player.getRandom().nextFloat() * 0.2F);
		}
	}
}
