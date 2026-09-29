package dev.wildercord.cast;

import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Fused effects of life and blood, made only at the Fusion Altar (see {@code spell.Fusions}); {@link FusedEffects}
 * hands each of them here. Their look is in {@link FusedLifeVfx}. Numbers match the rune descriptions in
 * {@code Runes}.
 *
 * <p>Three of them stay on a creature and answer what happens to it: Soulbond shares the wounds of two bound
 * souls, Second Wind cheats one death, Lifebloom heals over time and bursts when it fades. Their state is
 * kept here by UUID, holds the creature itself (so a respawned player is a new body, never still bound), and
 * goes on expiry, death, logout and server stop. None of it is saved.</p>
 */
final class FusedLife {
	private FusedLife() {}

	/** At most this many targets get a lingering or spreading part of their own, as in {@link FusedEffects}. */
	private static final int MAX_TARGETS = 8;
	/** Bonespur: spurs under at most this many enemies. */
	private static final int SPURS = 4;
	/** Soulbond: the bond breaks beyond this many blocks. */
	private static final double BOND_RANGE = 16.0;

	/** Registers anything these effects listen for (damage, deaths, ticks); called once at startup. */
	static void init() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(FusedLife::allowDamage);
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> afterDamage(entity));
		ServerLivingEntityEvents.ALLOW_DEATH.register(FusedLife::allowDeath);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.player));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			BONDS.clear();
			WINDS.clear();
			SPENT.clear();
			BLOOMS.clear();
			MISTS.clear();
			MIST_TOUCHED.clear();
			BLEEDING.clear();
			splitting = false;
		});
	}

	/** Does {@code node}'s effect if it's one of these, and says whether it was. */
	static boolean apply(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, List<LivingEntity> helped, List<LivingEntity> harmed,
			double power, double duration, int amplify) {
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		switch (node.effect.path()) {
			case "zephyr" -> zephyr(cast, hit, helped, 4.0 * SpellNumbers.effectRadius(node), Effects.ticks(10, duration), amplify);
			case "crimson_mist" -> crimsonMist(cast, hit, 3.0 * SpellNumbers.effectRadius(node), power, Effects.ticks(5, duration));
			case "soulbond" -> soulbond(cast, hit, helped, Effects.ticks(10, duration));
			case "second_wind" -> {
				for (LivingEntity t : first(helped)) {
					secondWind(cast, t, Effects.ticks(20, duration), power, duration);
				}
			}
			case "transfusion" -> transfusion(cast, helped, power);
			case "lifebloom" -> {
				double radius = 3.0 * SpellNumbers.effectRadius(node);
				for (int i = 0; i < helped.size(); i++) {
					lifebloom(cast, helped.get(i), power, Effects.ticks(5, duration), radius, i < MAX_TARGETS);
				}
			}
			case "bonespur" -> bonespur(cast, hit, 4.0 * SpellNumbers.effectRadius(node), power, Effects.ticks(3, duration));
			case "sanguine_rite" -> {
				if (harmed.isEmpty()) {
					// Nothing to strike, nothing asked: the rite takes blood only when it has a victim.
					return true;
				}
				if (!payBlood(caster, 3)) {
					FusedLifeVfx.sanguineRefused(level, caster);
					Casters.tell(caster, Component.translatableWithFallback("message.wildercord.sanguine_rite_weak",
						"The rite needs more blood than you can spare").withColor(0xFF6474));
					return true;
				}
				FusedLifeVfx.sanguineSigil(level, caster);
				// Every victim is struck; only the first few are drawn a lance of their own.
				for (int i = 0; i < harmed.size(); i++) {
					LivingEntity t = harmed.get(i);
					if (i < MAX_TARGETS) {
						FusedLifeVfx.sanguineLance(level, caster, t);
					}
					// Indirect magic: straight through armour.
					Effects.hurt(cast, t, level.damageSources().indirectMagic(caster, caster), 12 * power);
				}
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	private static List<LivingEntity> first(List<LivingEntity> targets) {
		return targets.size() <= MAX_TARGETS ? targets : targets.subList(0, MAX_TARGETS);
	}

	/** You and every ally within {@code radius} of the point (by the middle of its body), nearest first, at most {@link #MAX_TARGETS}. */
	private static List<LivingEntity> alliesAround(Cast cast, Vec3 point, double radius) {
		return around(cast, point, radius, e -> Targets.canHelp(cast.caster, e), MAX_TARGETS);
	}

	/** Every enemy within {@code radius} of the point (by the middle of its body), nearest first, at most {@code cap}. */
	private static List<LivingEntity> enemiesAround(Cast cast, Vec3 point, double radius, int cap) {
		return around(cast, point, radius, e -> Targets.canHarm(cast.caster, e), cap);
	}

	private static List<LivingEntity> around(Cast cast, Vec3 point, double radius, java.util.function.Predicate<Entity> test, int cap) {
		List<LivingEntity> out = new ArrayList<>();
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(point, point).inflate(radius + 1), e -> e instanceof LivingEntity && test.test(e))) {
			if (e.getBoundingBox().getCenter().distanceTo(point) <= radius + e.getBbWidth() / 2) {
				out.add((LivingEntity) e);
			}
		}
		out.sort(Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().distanceToSqr(point)));
		return out.size() <= cap ? out : new ArrayList<>(out.subList(0, cap));
	}

	/** Where an effect centred on the spell lands: at the caster's feet for Self, otherwise the point it hit. */
	private static Vec3 centreOf(Cast cast, Cast.Hit hit) {
		return hit.self() ? cast.caster.position() : hit.point();
	}

	// ------------------------------------------------------------------ Zephyr

	/** Zephyr: a warm breeze. Every ally around the point runs, leaps and mends a little better. */
	private static void zephyr(Cast cast, Cast.Hit hit, List<LivingEntity> helped, double radius, int ticks, int amplify) {
		ServerLevel level = cast.level;
		Vec3 point = centreOf(cast, hit);
		Set<LivingEntity> blessed = new LinkedHashSet<>(first(helped));
		blessed.addAll(alliesAround(cast, point, radius));
		// Like Surge: Amplify adds a level (rank III counts as one); charge and circles make it last longer, never stronger.
		int level2 = Math.min(2, Math.max(0, amplify));
		FusedLifeVfx.zephyr(level, CastEngine.ground(level, point.add(0, 0.5, 0)), radius);
		int n = 0;
		for (LivingEntity t : blessed) {
			if (n++ >= MAX_TARGETS) {
				break;
			}
			// Clear air: the breeze also blows away what blinds, sickens or slows (a hold stays).
			t.removeEffect(MobEffects.BLINDNESS);
			t.removeEffect(MobEffects.DARKNESS);
			t.removeEffect(MobEffects.NAUSEA);
			MobEffectInstance slow = t.getEffect(MobEffects.SLOWNESS);
			if (slow != null && slow.getAmplifier() < 6) {
				t.removeEffect(MobEffects.SLOWNESS);
			}
			t.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, level2, false, true));
			t.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, ticks, level2, false, true));
			t.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, level2, false, true));
			FusedLifeVfx.zephyrTouch(level, t);
		}
	}

	// ------------------------------------------------------------------ Crimson Mist

	/** One red mist lying where a spell landed. */
	private static final class Mist {
		Cast cast;
		final Vec3 centre;
		final double radius;
		double power;
		long until;
		long nextPulse;
		boolean over;

		Mist(Cast cast, Vec3 centre, double radius, double power, long now, int ticks) {
			this.cast = cast;
			this.centre = centre;
			this.radius = radius;
			this.power = power;
			this.until = now + ticks;
			this.nextPulse = now;
		}
	}

	private static final List<Mist> MISTS = new ArrayList<>();
	/** Who a mist last touched (a target, by whose mist), so overlapping mists of one caster touch it once a second, not once per mist. */
	private record Touch(UUID target, UUID caster) {}

	private static final Map<Touch, Long> MIST_TOUCHED = new HashMap<>();

	/**
	 * Crimson Mist: a red haze 3 blocks round where it lands. Once a second enemies in it bleed and allies in
	 * it mend. Cast again on the same spot, the mist thickens and lasts longer instead of doubling.
	 */
	private static void crimsonMist(Cast cast, Cast.Hit hit, double radius, double power, int ticks) {
		ServerLevel level = cast.level;
		Vec3 centre = CastEngine.ground(level, centreOf(cast, hit).add(0, 0.5, 0));
		long now = level.getGameTime();
		MISTS.removeIf(m -> m.over);
		for (Mist m : MISTS) {
			if (m.cast.caster == cast.caster && m.cast.level == level && m.centre.distanceToSqr(centre) < 2.25) {
				m.until = Math.max(m.until, now + ticks);
				m.power = Math.max(m.power, power);
				m.cast = cast;
				FusedLifeVfx.crimsonMistOpen(level, m.centre, m.radius);
				return;
			}
		}
		Mist mist = new Mist(cast, centre, radius, power, now, ticks);
		MISTS.add(mist);
		FusedLifeVfx.crimsonMistOpen(level, centre, radius);
		int[] age = {0};
		Runnable[] next = new Runnable[1];
		next[0] = Effects.carryContext(() -> {
			long time = level.getGameTime();
			if (mist.over || !mist.cast.alive() || time >= mist.until) {
				mist.over = true;
				MISTS.remove(mist);
				FusedLifeVfx.crimsonMistClose(level, mist.centre, mist.radius);
				return;
			}
			FusedLifeVfx.crimsonMist(level, mist.centre, mist.radius, age[0]);
			if (time >= mist.nextPulse) {
				mist.nextPulse = time + 20;
				Effects.lingering(() -> mistPulse(mist, time));
			}
			age[0] += 5;
			Scheduler.later(5, next[0]);
		});
		Scheduler.later(1, next[0]);
	}

	/** One second of the mist: enemies in it bleed, allies in it heal half a heart. */
	private static void mistPulse(Mist mist, long now) {
		Cast cast = mist.cast;
		ServerLevel level = cast.level;
		FusedLifeVfx.crimsonMistPulse(level, mist.centre, mist.radius);
		if (MIST_TOUCHED.size() > 128) {
			MIST_TOUCHED.values().removeIf(t -> now - t > 40);
		}
		AABB box = new AABB(mist.centre.x - mist.radius, mist.centre.y - 1, mist.centre.z - mist.radius,
			mist.centre.x + mist.radius, mist.centre.y + 3, mist.centre.z + mist.radius);
		int touched = 0;
		for (Entity e : level.getEntities((Entity) null, box, e -> e instanceof LivingEntity living && living.isAlive())) {
			if (touched >= 16) {
				break;
			}
			double dx = e.getX() - mist.centre.x;
			double dz = e.getZ() - mist.centre.z;
			if (dx * dx + dz * dz > mist.radius * mist.radius) {
				continue;
			}
			LivingEntity t = (LivingEntity) e;
			boolean enemy = Targets.canHarm(cast.caster, t);
			if (!enemy && !Targets.canHelp(cast.caster, t)) {
				continue;
			}
			Touch key = new Touch(t.getUUID(), cast.caster.getUUID());
			Long last = MIST_TOUCHED.get(key);
			if (last != null && now - last < 18) {
				continue;
			}
			MIST_TOUCHED.put(key, now);
			touched++;
			if (enemy) {
				FusedLifeVfx.crimsonMistBleed(level, t);
				// Bleeding while it stands in the mist: wind damage on it sets off Rupture.
				Reactions.mark(t, Reactions.Mark.BLEEDING, 30);
				Effects.hurt(cast, t, level.damageSources().indirectMagic(cast.caster, cast.caster), 1 * mist.power);
			} else if (t.getHealth() < t.getMaxHealth()) {
				t.heal((float) (1 * mist.power));
				FusedLifeVfx.crimsonMistMend(level, t);
			}
		}
	}

	// ------------------------------------------------------------------ Soulbond

	/** Two souls bound: the caster ({@code a}) and an ally ({@code b}). */
	private static final class Bond {
		final Cast cast;
		final ServerLevel level;
		final LivingEntity a;
		final LivingEntity b;
		long until;
		boolean over;
		/** A hit on either, being taken right now: its health and absorption before it (NaN when none). */
		final float[] health = {Float.NaN, Float.NaN};
		final float[] absorption = {0, 0};
		/** When the last share was drawn, so a fast run of small hits (a fire, say) doesn't flood the air with it. */
		long shownAt;

		Bond(Cast cast, LivingEntity a, LivingEntity b, long until) {
			this.cast = cast;
			this.level = cast.level;
			this.a = a;
			this.b = b;
			this.until = until;
		}

		int side(LivingEntity e) {
			return e == a ? 0 : e == b ? 1 : -1;
		}

		LivingEntity other(int side) {
			return side == 0 ? b : a;
		}

		/**
		 * Whether the bond still holds: in time, both bodies still in the world (the very ones bound) and within
		 * range. Says nothing about health: a hit that is killing one of them still reaches the bond.
		 */
		boolean holds(long now) {
			return !over && now <= until && !a.isRemoved() && !b.isRemoved() && a.level() == level && b.level() == level
				&& a.distanceToSqr(b) <= BOND_RANGE * BOND_RANGE;
		}
	}

	private static final Map<UUID, Bond> BONDS = new HashMap<>();
	/** Set while a share is being passed on, so the half passed on is never split again. */
	private static boolean splitting;

	/**
	 * Whether {@code e} can carry its half of a wound: alive, and not a player in creative or spectator (a
	 * bond with someone who can't be hurt would only ever halve the other's wounds).
	 */
	private static boolean canShare(LivingEntity e) {
		return e.isAlive() && !e.isRemoved() && !e.isInvulnerable() && !(e instanceof Player p && (p.isCreative() || p.isSpectator()));
	}

	/**
	 * Soulbond: binds the caster and an ally for a while. Every wound either takes, after their own armour, is
	 * shared half and half. On Self it reaches for the nearest ally within range; with none, it does nothing.
	 */
	private static void soulbond(Cast cast, Cast.Hit hit, List<LivingEntity> helped, int ticks) {
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		if (!canShare(caster)) {
			// Bound to someone who can't be hurt, the other would only ever take half of every wound.
			FusedLifeVfx.soulbondAlone(level, caster);
			Casters.tell(caster, Component.translatableWithFallback("message.wildercord.soulbond_unhurt",
				"A soul that can't be hurt can't share wounds").withColor(0xE678DC));
			return;
		}
		Bond current = BONDS.get(caster.getUUID());
		LivingEntity partner = null;
		if (current != null && current.side(caster) >= 0) {
			LivingEntity bound = current.other(current.side(caster));
			if (helped.contains(bound)) {
				// Cast again on the one it already holds: the bond is renewed, not doubled.
				partner = bound;
			}
		}
		if (partner == null) {
			partner = helped.stream()
				.filter(e -> e != caster && bindable(e))
				.min(Comparator.comparingDouble(e -> e.distanceToSqr(caster)))
				.orElse(null);
		}
		if (partner == null && hit.self()) {
			partner = level.getEntities(caster, caster.getBoundingBox().inflate(BOND_RANGE),
					e -> e instanceof LivingEntity living && Targets.canHelp(caster, e) && bindable(living)).stream()
				.map(e -> (LivingEntity) e)
				.filter(e -> e.distanceToSqr(caster) <= BOND_RANGE * BOND_RANGE)
				.min(Comparator.comparingDouble(e -> e.distanceToSqr(caster)))
				.orElse(null);
		}
		if (partner == null) {
			FusedLifeVfx.soulbondAlone(level, caster);
			Casters.tell(caster, Component.translatableWithFallback("message.wildercord.soulbond_alone",
				"Soulbond needs an ally to bind you to").withColor(0xE678DC));
			return;
		}
		if (partner.distanceToSqr(caster) > BOND_RANGE * BOND_RANGE) {
			FusedLifeVfx.soulbondAlone(level, caster);
			Casters.tell(caster, Component.translatableWithFallback("message.wildercord.soulbond_far",
				"Too far away to bind").withColor(0xE678DC));
			return;
		}
		long now = level.getGameTime();
		if (current != null && current.side(caster) >= 0 && current.other(current.side(caster)) == partner && current.holds(now)) {
			current.until = Math.max(current.until, now + ticks);
			FusedLifeVfx.soulbondBind(level, caster, partner);
			return;
		}
		// One bond each: a new one takes the place of any other either of them was in.
		unbind(caster, true);
		unbind(partner, true);
		Bond bond = new Bond(cast, caster, partner, now + ticks);
		BONDS.put(caster.getUUID(), bond);
		BONDS.put(partner.getUUID(), bond);
		FusedLifeVfx.soulbondBind(level, caster, partner);
		int[] age = {0};
		Runnable[] next = new Runnable[1];
		next[0] = () -> {
			if (bond.over) {
				return;
			}
			long time = level.getGameTime();
			if (!bond.a.isAlive() || !bond.b.isAlive() || !bond.cast.alive() || bond.a.level() != level || bond.b.level() != level) {
				// One of them died, left or went to another world: it just ends.
				end(bond, 0);
				return;
			}
			if (time > bond.until) {
				end(bond, 2);
				return;
			}
			if (!bond.holds(time)) {
				// Stretched too far: it snaps.
				end(bond, 1);
				return;
			}
			if (age[0] % 10 == 0) {
				FusedLifeVfx.soulbondTether(level, bond.a, bond.b, age[0], false);
			}
			age[0] += 5;
			Scheduler.later(5, next[0]);
		};
		Scheduler.later(5, next[0]);
	}

	/** Not a boss, and able to carry a share. */
	private static boolean bindable(LivingEntity e) {
		return !Spirits.isBoss(e) && canShare(e);
	}

	/** Ends a bond. {@code how}: 0 quietly, 1 snapped (too far apart), 2 faded (its time ran out). */
	private static void end(Bond bond, int how) {
		if (bond.over) {
			return;
		}
		bond.over = true;
		BONDS.remove(bond.a.getUUID(), bond);
		BONDS.remove(bond.b.getUUID(), bond);
		if (how == 1) {
			FusedLifeVfx.soulbondSnap(bond.level, bond.a, bond.b);
		} else if (how == 2) {
			FusedLifeVfx.soulbondFade(bond.level, bond.a, bond.b);
		}
	}

	/** Ends whatever bond {@code e} is in. */
	private static void unbind(LivingEntity e, boolean snap) {
		Bond bond = BONDS.get(e.getUUID());
		if (bond != null) {
			end(bond, snap && bond.a.level() == bond.b.level() ? 1 : 0);
		}
	}

	/**
	 * Before a hit on a bound creature: remembers its health, so the wound can be shared once it's known. A hit
	 * inside the hurt cooldown lands (or doesn't) on its own; the void and /kill are never shared.
	 */
	private static boolean allowDamage(LivingEntity e, DamageSource source, float amount) {
		if (BONDS.isEmpty()) {
			return true;
		}
		Bond bond = BONDS.get(e.getUUID());
		if (bond == null) {
			return true;
		}
		int side = bond.side(e);
		if (side < 0) {
			return true;
		}
		bond.health[side] = Float.NaN;
		long now = bond.level.getGameTime();
		if (now > bond.until + 40) {
			// Long over and somehow never ended (its ticking lost): let it go now.
			end(bond, 0);
			return true;
		}
		if (splitting || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !bond.holds(now)) {
			return true;
		}
		if (e.damageCooldownTime > 10 && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
			return true;
		}
		if (!canShare(bond.other(side))) {
			return true;
		}
		bond.health[side] = e.getHealth();
		bond.absorption[side] = e.getAbsorptionAmount();
		return true;
	}

	/** After a hit the bound creature lived through: it gets half of the wound back, and the other takes that half. */
	private static void afterDamage(LivingEntity e) {
		if (BONDS.isEmpty() || splitting) {
			return;
		}
		Bond bond = BONDS.get(e.getUUID());
		int side = bond == null ? -1 : bond.side(e);
		if (side < 0 || Float.isNaN(bond.health[side])) {
			return;
		}
		float health = bond.health[side];
		float absorption = bond.absorption[side];
		bond.health[side] = Float.NaN;
		LivingEntity other = bond.other(side);
		if (!e.isAlive() || !bond.holds(bond.level.getGameTime()) || !canShare(other)) {
			return;
		}
		float lostHealth = Math.max(0, health - e.getHealth());
		float lostAbsorption = Math.max(0, absorption - e.getAbsorptionAmount());
		float wound = lostHealth + lostAbsorption;
		if (wound < 0.01F) {
			return;
		}
		// Half of it back where it came off (health to health, absorption to absorption); the other half goes across.
		e.setAbsorptionAmount(e.getAbsorptionAmount() + lostAbsorption / 2);
		e.setHealth(e.getHealth() + lostHealth / 2);
		pass(bond, e, other, wound / 2, false);
	}

	/**
	 * A killing blow on a bound creature: the wound it would have taken is shared all the same. If its half
	 * alone doesn't kill it, it lives; otherwise it dies, and the other still takes the same half, so a bond
	 * never makes a death that would have come anyway disappear (two bound souls both struck down at once
	 * both fall).
	 */
	private static boolean soulbondDeath(LivingEntity e, DamageSource source, float amount) {
		if (BONDS.isEmpty() || splitting) {
			return false;
		}
		Bond bond = BONDS.get(e.getUUID());
		int side = bond == null ? -1 : bond.side(e);
		if (side < 0 || Float.isNaN(bond.health[side])) {
			return false;
		}
		float before = bond.health[side] + bond.absorption[side];
		bond.health[side] = Float.NaN;
		LivingEntity other = bond.other(side);
		if (!bond.holds(bond.level.getGameTime()) || !canShare(other)) {
			return false;
		}
		// Health stops at nothing, so the whole wound is worked out as the hit went through: at least all it had.
		float wound = Math.max(before, afterDefences(bond.level, e, source, amount));
		float half = wound / 2;
		float left = before - half;
		pass(bond, e, other, half, left > 0);
		if (left <= 0) {
			return false;
		}
		float health = Math.min(e.getMaxHealth(), left);
		e.setAbsorptionAmount(left - health);
		e.setHealth(health);
		return true;
	}

	/**
	 * What {@code amount} comes to after the creature's armour, Resistance and protective enchantments, as the
	 * game works it out when it takes a hit.
	 */
	private static float afterDefences(ServerLevel level, LivingEntity e, DamageSource source, float amount) {
		float damage = amount;
		if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
			damage = CombatRules.getDamageAfterAbsorb(e, damage, source, e.getArmorValue(), (float) e.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
		}
		if (source.is(DamageTypeTags.BYPASSES_EFFECTS)) {
			return damage;
		}
		MobEffectInstance resistance = e.getEffect(MobEffects.RESISTANCE);
		if (resistance != null && !source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
			damage = Math.max(0, damage * (25 - (resistance.getAmplifier() + 1) * 5) / 25.0F);
		}
		if (damage > 0 && !source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
			float protection = EnchantmentHelper.getDamageProtection(level, e, source);
			if (protection > 0) {
				damage = CombatRules.getDamageAfterMagicAbsorb(damage, protection);
			}
		}
		return damage;
	}

	/**
	 * {@code share} of a wound crossing the bond from {@code from} to {@code to}. It's already past
	 * {@code from}'s defences, so it comes straight off {@code to}'s absorption and health (like Blood Price:
	 * no armour, no hurt cooldown, nothing that would let the next real hit on it slip through). One that
	 * would kill goes through the damage code instead, so that death goes as deaths do and a totem (or a
	 * Second Wind) can still answer it.
	 */
	private static void pass(Bond bond, LivingEntity from, LivingEntity to, float share, boolean saved) {
		ServerLevel level = bond.level;
		splitting = true;
		try {
			float pool = to.getHealth() + to.getAbsorptionAmount();
			if (share < pool - 0.01F) {
				float fromAbsorption = Math.min(to.getAbsorptionAmount(), share);
				to.setAbsorptionAmount(to.getAbsorptionAmount() - fromAbsorption);
				to.setHealth(to.getHealth() - (share - fromAbsorption));
				// The flinch of a hit, without one.
				level.broadcastDamageEvent(to, level.damageSources().magic());
			} else {
				Effects.readyToHurt(to);
				// Enough to get through Resistance IV and full protection both: this share is a death.
				to.hurtServer(level, level.damageSources().magic(), (pool + share) * 25.0F);
			}
		} finally {
			splitting = false;
		}
		long now = level.getGameTime();
		if (saved || now - bond.shownAt >= 5) {
			bond.shownAt = now;
			FusedLifeVfx.soulbondShare(level, from, to, saved);
		}
	}

	// ------------------------------------------------------------------ Second Wind

	/** A Second Wind waiting on a creature. */
	private static final class Wind {
		final LivingEntity who;
		final Cast cast;
		long until;
		double power;
		double duration;
		boolean over;

		Wind(LivingEntity who, Cast cast, long until, double power, double duration) {
			this.who = who;
			this.cast = cast;
			this.until = until;
			this.power = power;
			this.duration = duration;
		}
	}

	private static final Map<UUID, Wind> WINDS = new HashMap<>();
	/**
	 * When a Second Wind last saved each creature (game time), for the lockout ({@link FusedLifeRules#lockedOut}).
	 * Kept through logout and death, so neither clears it; old entries are let go as new ones come.
	 */
	private static final Map<UUID, Long> SPENT = new HashMap<>();

	/**
	 * Second Wind: for a while, the first blow that would kill the ally leaves it standing instead. Cast again
	 * on a ward not yet spent, it only lasts longer (never two saves). Once one has saved a creature, a new one
	 * won't take on it for a minute, so recasting can't chain it into near-immortality.
	 */
	private static void secondWind(Cast cast, LivingEntity t, int ticks, double power, double duration) {
		ServerLevel level = cast.level;
		if (Spirits.isBoss(t)) {
			// A boss's death isn't to be cheated.
			return;
		}
		long now = level.getGameTime();
		Long saved = SPENT.get(t.getUUID());
		if (saved != null && FusedLifeRules.lockedOut(saved, now)) {
			FusedLifeVfx.secondWindSpent(level, t);
			if (cast.once("second_wind_spent")) {
				Casters.tell(cast.caster, Component.translatableWithFallback("message.wildercord.second_wind_spent",
					"Second Wind has saved them already: again in %s s", FusedLifeRules.lockoutSecondsLeft(saved, now)).withColor(0xF2D98A));
			}
			return;
		}
		Wind old = WINDS.get(t.getUUID());
		if (old != null && old.who == t && !old.over) {
			old.until = Math.max(old.until, now + ticks);
			old.power = Math.max(old.power, power);
			old.duration = Math.max(old.duration, duration);
			FusedLifeVfx.secondWind(level, t);
			return;
		}
		Wind wind = new Wind(t, cast, now + ticks, power, duration);
		WINDS.put(t.getUUID(), wind);
		FusedLifeVfx.secondWind(level, t);
		int[] age = {0};
		Runnable[] next = new Runnable[1];
		next[0] = () -> {
			if (wind.over) {
				return;
			}
			long time = level.getGameTime();
			if (!t.isAlive() || t.isRemoved() || t.level() != level || !wind.cast.alive() || time > wind.until) {
				wind.over = true;
				WINDS.remove(t.getUUID(), wind);
				if (time > wind.until && t.isAlive() && !t.isRemoved()) {
					FusedLifeVfx.secondWindFade(level, t);
				}
				return;
			}
			age[0] += 20;
			if (age[0] % 40 == 0) {
				FusedLifeVfx.secondWindMark(level, t, age[0]);
			}
			Scheduler.later(20, next[0]);
		};
		Scheduler.later(20, next[0]);
	}

	/** The killing blow on someone with a Second Wind: 4 health left, and Regeneration II. The void and /kill aren't blows. */
	private static boolean secondWindDeath(LivingEntity e, DamageSource source) {
		if (WINDS.isEmpty()) {
			return false;
		}
		Wind wind = WINDS.get(e.getUUID());
		if (wind == null || wind.who != e || wind.over || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
				|| !(e.level() instanceof ServerLevel level)) {
			return false;
		}
		wind.over = true;
		WINDS.remove(e.getUUID(), wind);
		long now = level.getGameTime();
		if (now > wind.until || DeathsDoor.resting(e) > 0) {
			return false;
		}
		DeathsDoor.saved(e, false);
		if (SPENT.size() > 64) {
			SPENT.values().removeIf(saved -> !FusedLifeRules.lockedOut(saved, now));
		}
		SPENT.put(e.getUUID(), now);
		e.setHealth(FusedLifeRules.secondWindHealth(wind.power, e.getMaxHealth()));
		e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Effects.ticks(4, wind.duration), 1, false, true));
		// The way out: a burst of speed and a gust that clears the enemies round them (a creature gets out, it doesn't stand and fight: that is Reversal).
		e.addEffect(new MobEffectInstance(MobEffects.SPEED, Effects.ticks(4, wind.duration), 1, false, true));
		for (Entity near : level.getEntities(e, e.getBoundingBox().inflate(3.0), x -> Targets.canHarm(wind.cast.caster, x) && x instanceof LivingEntity)) {
			if (!Spirits.isBoss(near) && near.distanceTo(e) <= 3.0) {
				Effects.push((LivingEntity) near, Effects.horizontal(near.position().subtract(e.position()), e.getLookAngle()).scale(1.6).add(0, 0.35, 0));
			}
		}
		FusedLifeVfx.secondWindSaved(level, e);
		if (e instanceof ServerPlayer player) {
			player.sendOverlayMessage(Component.translatableWithFallback("message.wildercord.second_wind", "Second Wind!").withColor(0x6EDC64));
		}
		return true;
	}

	/** Deaths first meet a Soulbond (a shared wound may be survivable), then a Second Wind. */
	private static boolean allowDeath(LivingEntity e, DamageSource source, float amount) {
		if (BONDS.isEmpty() && WINDS.isEmpty()) {
			return true;
		}
		return !soulbondDeath(e, source, amount) && !secondWindDeath(e, source);
	}

	// ------------------------------------------------------------------ Transfusion

	/**
	 * Transfusion: the caster bleeds some of their own health into each ally, which heals twice what was given.
	 * They give no more than the ally needs, never go below 2 themselves, and onto themselves it does nothing.
	 * The price comes straight off their health, like Blood Price: it's not a hit, so nothing answers it.
	 */
	private static void transfusion(Cast cast, List<LivingEntity> helped, double power) {
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		boolean free = caster instanceof Player player && player.isCreative();
		List<LivingEntity> allies = first(helped.stream().filter(e -> e != caster).toList());
		if (allies.isEmpty()) {
			FusedLifeVfx.transfusionRefused(level, caster);
			Casters.tell(caster, Component.translatableWithFallback("message.wildercord.transfusion_self",
				"Transfusion needs an ally to give to").withColor(0xFF6474));
			return;
		}
		boolean gave = false;
		boolean weak = false;
		for (LivingEntity t : allies) {
			if (!free && caster.getHealth() - FusedLifeRules.TRANSFUSION_FLOOR < 0.25) {
				weak = true;
			}
			double give = FusedLifeRules.transfusionGift(power, caster.getHealth(), t.getMaxHealth() - t.getHealth(), free);
			if (give <= 0) {
				continue;
			}
			if (!free) {
				caster.setHealth((float) (caster.getHealth() - give));
			}
			t.heal((float) (2 * give));
			FusedLifeVfx.transfusion(level, caster, t, give);
			gave = true;
		}
		if (!gave) {
			FusedLifeVfx.transfusionRefused(level, caster);
			Casters.tell(caster, weak
				? Component.translatableWithFallback("message.wildercord.transfusion_weak", "Too little blood left to give").withColor(0xFF6474)
				: Component.translatableWithFallback("message.wildercord.transfusion_whole", "They're already whole").withColor(0xFF6474));
		}
	}

	// ------------------------------------------------------------------ Lifebloom

	/** A Lifebloom opening on a creature. */
	private static final class Bloom {
		final LivingEntity who;
		Cast cast;
		/** Seconds of it still to come: one heal each, and the burst with the last. */
		int beats;
		double power;
		double radius;
		boolean over;

		Bloom(LivingEntity who, Cast cast, int beats, double power, double radius) {
			this.who = who;
			this.cast = cast;
			this.beats = beats;
			this.power = power;
			this.radius = radius;
		}
	}

	private static final Map<UUID, Bloom> BLOOMS = new HashMap<>();

	/**
	 * Lifebloom: heals at once, then a little every second; when it fades it bursts, healing every ally
	 * around. Cast again on a blooming ally, it heals at once and the bloom starts over (one burst, not two).
	 * Every ally is healed at once; only if {@code blooms} (the first few of a crowd) does the bloom open.
	 */
	private static void lifebloom(Cast cast, LivingEntity t, double power, int ticks, double radius, boolean blooms) {
		ServerLevel level = cast.level;
		t.heal((float) (4 * power));
		if (!blooms) {
			return;
		}
		FusedLifeVfx.lifebloomOpen(level, t);
		// Counted in whole seconds, so it's always exactly that many heals, however the ticks fall.
		int beats = Math.max(1, (int) Math.round(ticks / 20.0));
		Bloom old = BLOOMS.get(t.getUUID());
		if (old != null && old.who == t && !old.over) {
			old.beats = beats;
			old.power = power;
			old.radius = radius;
			old.cast = cast;
			return;
		}
		Bloom bloom = new Bloom(t, cast, beats, power, radius);
		BLOOMS.put(t.getUUID(), bloom);
		int[] beat = {0};
		Runnable[] next = new Runnable[1];
		next[0] = Effects.carryContext(() -> {
			if (bloom.over) {
				return;
			}
			if (!bloom.cast.alive() || !t.isAlive() || t.isRemoved() || t.level() != level) {
				bloom.over = true;
				BLOOMS.remove(t.getUUID(), bloom);
				return;
			}
			t.heal((float) (1 * bloom.power));
			FusedLifeVfx.lifebloomPulse(level, t, beat[0]++);
			if (--bloom.beats <= 0) {
				bloom.over = true;
				BLOOMS.remove(t.getUUID(), bloom);
				burst(bloom);
				return;
			}
			Scheduler.later(20, next[0]);
		});
		Scheduler.later(20, next[0]);
	}

	/** The bloom fading: it bursts, healing every ally of its caster around it for 3. */
	private static void burst(Bloom bloom) {
		Cast cast = bloom.cast;
		ServerLevel level = cast.level;
		Vec3 at = bloom.who.getBoundingBox().getCenter();
		FusedLifeVfx.lifebloomBurst(level, bloom.who, bloom.radius);
		for (LivingEntity ally : alliesAround(cast, at, bloom.radius)) {
			ally.heal((float) (3 * bloom.power));
			FusedLifeVfx.lifebloomMended(level, ally);
		}
	}

	// ------------------------------------------------------------------ Bonespur

	/**
	 * Bonespur: spurs of bone burst out of the ground under the enemies nearest the point, and the wounds
	 * they leave bleed for a while.
	 */
	private static void bonespur(Cast cast, Cast.Hit hit, double radius, double power, int bleedTicks) {
		ServerLevel level = cast.level;
		Vec3 point = centreOf(cast, hit);
		Vec3 ground = CastEngine.ground(level, point.add(0, 0.5, 0));
		List<LivingEntity> targets = enemiesAround(cast, point, radius, SPURS);
		FusedLifeVfx.bonespurField(level, ground, radius, targets.isEmpty());
		for (int i = 0; i < targets.size(); i++) {
			LivingEntity t = targets.get(i);
			FusedLifeVfx.bonespurWarn(level, t);
			// The nearest first, the rest a beat apart: the ground breaks open under each in turn.
			Scheduler.later(4 + i * 2, Effects.carryContext(() -> {
				if (!cast.alive() || !t.isAlive() || t.level() != level) {
					return;
				}
				FusedLifeVfx.bonespur(level, t);
				Effects.hurt(cast, t, strike(cast), 5 * power);
				if (t.isAlive()) {
					bleed(cast, t, power, bleedTicks);
				}
			}));
		}
	}

	/** A spur of bone: a blow from the ground, turned by armour like any other. */
	private static DamageSource strike(Cast cast) {
		return cast.caster instanceof Player player ? cast.level.damageSources().playerAttack(player) : cast.level.damageSources().mobAttack(cast.caster);
	}

	/** Who's bleeding, and the bleed that's theirs now: a fresh wound takes over from an old one instead of stacking. */
	private static final Map<UUID, Object> BLEEDING = new HashMap<>();

	/**
	 * A bleed: 1 damage a second for {@code ticks} (the same drops as the Bleed rune's wounds). Like Bleed it's
	 * magic, straight through armour; it's lingering damage, which a Shield can block but not parry.
	 */
	private static void bleed(Cast cast, LivingEntity t, double power, int ticks) {
		Object token = new Object();
		BLEEDING.put(t.getUUID(), token);
		// Bleeding as long as it runs: wind damage on it sets off Rupture.
		Reactions.mark(t, Reactions.Mark.BLEEDING, ticks + 10);
		ServerLevel level = cast.level;
		DamageSource source = level.damageSources().indirectMagic(cast.caster, cast.caster);
		int wounds = Math.max(1, (int) Math.round(ticks / 20.0));
		for (int i = 1; i <= wounds; i++) {
			boolean last = i == wounds;
			Scheduler.later(i * 20, Effects.carryContext(() -> {
				if (BLEEDING.get(t.getUUID()) != token) {
					return;
				}
				if (last || !cast.alive() || !t.isAlive() || t.level() != level) {
					BLEEDING.remove(t.getUUID(), token);
				}
				if (!cast.alive() || !t.isAlive() || t.level() != level) {
					return;
				}
				ExpansionVfx.bleed(level, t, false);
				Effects.lingering(() -> Effects.hurt(cast, t, source, 1 * power));
			}));
		}
	}

	// ------------------------------------------------------------------ Sanguine Rite

	/**
	 * Takes {@code cost} health from the caster, straight off (like Blood Price: not a hit, so it's never PvP,
	 * never shared by a Soulbond and never sets off the caster's own on-hurt magic). Refused, taking nothing,
	 * when it would leave them nothing. Creative players pay nothing.
	 */
	private static boolean payBlood(LivingEntity caster, float cost) {
		if (caster instanceof Player player && player.isCreative()) {
			return true;
		}
		if (!FusedLifeRules.canPayBlood(caster.getHealth(), cost)) {
			return false;
		}
		caster.setHealth(caster.getHealth() - cost);
		return true;
	}

	// ------------------------------------------------------------------ cleanup

	/** A player leaving: whatever of theirs is running here ends now. */
	private static void forget(ServerPlayer player) {
		UUID id = player.getUUID();
		Bond bond = BONDS.get(id);
		if (bond != null) {
			end(bond, 0);
		}
		Wind wind = WINDS.remove(id);
		if (wind != null) {
			wind.over = true;
		}
		Bloom bloom = BLOOMS.remove(id);
		if (bloom != null) {
			bloom.over = true;
		}
		BLEEDING.remove(id);
		for (Iterator<Mist> it = MISTS.iterator(); it.hasNext(); ) {
			Mist mist = it.next();
			if (mist.cast.caster == player) {
				mist.over = true;
				it.remove();
			}
		}
	}
}
