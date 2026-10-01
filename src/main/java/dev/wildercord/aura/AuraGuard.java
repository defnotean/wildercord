package dev.wildercord.aura;

import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.ScreenFx;
import dev.wildercord.cast.Spirits;
import dev.wildercord.config.Config;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.spell.Parry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * Aura Guard (Flow): sneak and press the Aura key, and aura braces the blade. Held (while sneak is held, two seconds at most)
 * it halves blows and projectiles from in front, paying aura for what it takes off; at nothing it breaks, with backlash. Its
 * first moments are a perfect guard, timed as a parry is ({@link Parry}): a blow is turned aside whole and its attacker
 * staggered, a projectile flies back at whoever loosed it, and a spell is parried as a Shield raised at the last moment parries
 * one (negated, and answered: see {@code cast.Shields}).
 *
 * <p>Damage reaches the guard from {@code mixin.LivingEntityAuraMixin}, wrapped round {@code LivingEntity.hurtServer}.</p>
 */
public final class AuraGuard {
	private AuraGuard() {}

	/** The colour of a perfect guard: a parry's gold. */
	public static final int PERFECT_COLOR = 0xFFD54A;

	/** Raises the guard (the technique): Flow, an aura weapon in hand, rested, and its price paid. */
	public static boolean raise(ServerPlayer player) {
		long now = player.level().getGameTime();
		AuraAttachments.State state = Aura.state(player);
		if (!Aura.holdsWeapon(player)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.no_weapon").withColor(0xA89CC8));
			return false;
		}
		if (state.guarding(now) || now < state.guardUntil() + AuraRules.GUARD_REST) {
			return false;
		}
		// An aura-forged maul braces for less.
		AuraRules.Spend paid = Aura.spend(player, AuraRules.GUARD_RAISE_COST * dev.wildercord.aura.world.ForgedGear.guardCost(player), "guard");
		if (paid.backlash()) {
			// Spent past empty: the guard never forms.
			Aura.state(player, Aura.state(player).guard(-1, now));
			return false;
		}
		Aura.state(player, Aura.state(player).guard(now, now + AuraRules.GUARD_TICKS));
		Aura.sound(player, "aura_guard", 0.8F, 1.0F);
		AuraVfx.guard(player, Aura.color(player));
		player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
		return true;
	}

	/** Every tick: the guard drops when sneak is let go, the blade leaves the hand, or its time is up. */
	static void tick(ServerPlayer player, long now) {
		AuraAttachments.State state = Aura.state(player);
		if (state.guardRaised() < 0 || now > state.guardUntil()) {
			return;
		}
		if (!player.isShiftKeyDown() || !Aura.holdsWeapon(player) || !player.isAlive()) {
			drop(player, now);
		}
	}

	private static void drop(ServerPlayer player, long now) {
		Aura.state(player, Aura.state(player).guard(-1, now));
	}

	/** Whether {@code player}'s guard is up now. */
	public static boolean guarding(Player player) {
		return Aura.state(player).guarding(player.level().getGameTime());
	}

	/** Whether the guard is in its perfect moment now. */
	public static boolean perfectNow(Player player) {
		AuraAttachments.State state = Aura.state(player);
		long now = player.level().getGameTime();
		return state.guarding(now) && AuraRules.perfect(state.guardRaised(), now);
	}

	/** Whether the guard covers this harm: a blow or a projectile, from in front. */
	static boolean covers(ServerPlayer player, DamageSource source) {
		Entity direct = source.getDirectEntity();
		if (direct == null || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.IS_EXPLOSION)) {
			return false;
		}
		boolean projectile = direct instanceof Projectile || source.is(DamageTypeTags.IS_PROJECTILE);
		boolean blow = direct instanceof LivingEntity && direct != player;
		if (!projectile && !blow) {
			return false;
		}
		return facing(player, direct.position());
	}

	/** Whether {@code from} is in front of the player (the guard's half). */
	static boolean facing(ServerPlayer player, Vec3 from) {
		Vec3 toward = from.subtract(player.position());
		Vec3 look = player.getViewVector(1.0F);
		return toward.horizontalDistanceSqr() < 1.0E-4 || look.x * toward.x + look.z * toward.z > 0;
	}

	/**
	 * A projectile reaching a player in their perfect guard's moment, from in front: turned back at whoever loosed it, a quarter
	 * faster, through vanilla's own deflection (as a breeze turns arrows), and the guard's from the next tick. Asked by
	 * {@code mixin.EntityAuraDeflectMixin} before the projectile hits; null when it isn't turned.
	 */
	public static net.minecraft.world.entity.projectile.ProjectileDeflection deflection(Entity entity, Projectile projectile) {
		if (!(entity instanceof ServerPlayer player) || !perfectNow(player) || !facing(player, projectile.position())) {
			return null;
		}
		long now = player.level().getGameTime();
		Aura.state(player, Aura.state(player).guard(now - AuraRules.PERFECT_TICKS - 1, Aura.state(player).guardUntil()));
		feedback(player);
		Grimoire.unlock(player, "aura:perfect_guard");
		Entity shooter = projectile.getOwner();
		double speed = Math.min(3.0, Math.max(0.6, projectile.getDeltaMovement().length()) * Parry.REFLECT_SPEED);
		Scheduler.later(1, () -> {
			if (!projectile.isRemoved()) {
				projectile.setOwner(player);
			}
		});
		return (turned, by, random, power) -> {
			Vec3 at = turned.position();
			Vec3 aim = shooter != null && shooter.isAlive() && shooter.level() == turned.level() && shooter.distanceTo(player) < Parry.COUNTER_RANGE
				? shooter.getBoundingBox().getCenter().subtract(at)
				: player.getViewVector(1.0F);
			if (aim.lengthSqr() < 1.0E-4) {
				aim = player.getViewVector(1.0F);
			}
			turned.setDeltaMovement(aim.normalize().scale(speed));
			turned.needsSync = true;
		};
	}

	/**
	 * What reaches a player through their guard: the whole blow when there's no guard (or it doesn't cover it), nothing when
	 * a perfect guard turns it (returns a negative number), half of it as far as aura pays when held.
	 */
	public static float incoming(LivingEntity target, DamageSource source, float damage) {
		if (!(target instanceof ServerPlayer player) || damage <= 0 || !guarding(player) || !covers(player, source)) {
			return damage;
		}
		long now = player.level().getGameTime();
		if (perfectNow(player)) {
			perfect(player, source);
			return -1;
		}
		double share = Config.get().aura().guardShare();
		// An aura-forged maul's guard pays less a point, so the same aura holds off more.
		double cost = dev.wildercord.aura.world.ForgedGear.guardCost(player);
		double absorbed = AuraRules.guardAbsorb(damage, Aura.aura(player) / Math.max(1.0E-3, cost), share);
		if (absorbed <= 0) {
			breaks(player, now);
			return damage;
		}
		Aura.spend(player, absorbed * AuraRules.GUARD_COST_PER_POINT * cost, "guard");
		AuraVfx.held(player, Aura.color(player), source);
		if (Aura.aura(player) <= 1.0E-3 && absorbed < damage * share - 1.0E-3) {
			// It took what it could and has nothing left: it breaks.
			breaks(player, now);
		}
		return (float) (damage - absorbed);
	}

	/** The guard gives way at nothing: it drops, with backlash. */
	private static void breaks(ServerPlayer player, long now) {
		drop(player, now);
		Aura.backlash(player);
	}

	/** A perfect guard: a blow turned aside whole and its attacker staggered, or a projectile sent back at whoever loosed it. */
	private static void perfect(ServerPlayer player, DamageSource source) {
		ServerLevel level = player.level();
		long now = level.getGameTime();
		// The perfect moment answers once.
		Aura.state(player, Aura.state(player).guard(now - AuraRules.PERFECT_TICKS - 1, Aura.state(player).guardUntil()));
		Entity direct = source.getDirectEntity();
		if (source.is(Aura.DAMAGE)) {
			// Aura off a blade (a slash, a spark): a crescent is sent back at whoever loosed it, as the guard's own. Nobody is
			// staggered from across a field.
			Crescents.reflect(player, Aura.color(player), e -> dev.wildercord.cast.Targets.canHarm(player, e), AuraSlash.cutter(player));
		} else if (direct instanceof Projectile projectile) {
			reflect(player, projectile);
		} else if (source.getEntity() instanceof LivingEntity attacker && attacker != player) {
			stagger(player, attacker);
		}
		feedback(player);
		// The first one goes into the Grimoire.
		Grimoire.unlock(player, "aura:perfect_guard");
	}

	/** The guard's flash, sound and words: a parry's gold. */
	static void feedback(ServerPlayer player) {
		// The swing straight after it is a counter, for sword strings.
		SwordStrings.cue(player, StringReader.Cue.GUARD);
		AuraVfx.perfect(player, Aura.color(player));
		Aura.sound(player, "aura_perfect_guard", 1.0F, 1.0F);
		dev.wildercord.cast.Fx.sound(player.level(), player.position(), WildercordSounds.SHIELD_PARRY, 0.8F, 1.2F);
		ScreenFx.kick(player, 0.35F);
		player.sendOverlayMessage(Component.translatable("message.wildercord.aura.perfect_guard").withColor(PERFECT_COLOR));
	}

	/** A staggered attacker: thrown back, slowed and weakened for a moment (a boss is only slowed, as every boss is). */
	static void stagger(ServerPlayer player, LivingEntity attacker) {
		Vec3 away = attacker.position().subtract(player.position());
		Vec3 flat = new Vec3(away.x, 0, away.z);
		if (flat.lengthSqr() > 1.0E-4) {
			attacker.knockback(0.9, -flat.x, -flat.z, player.damageSources().playerAttack(player), 0.0F);
			attacker.syncVelocity = true;
		}
		attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, AuraRules.STAGGER_TICKS, Spirits.isBoss(attacker) ? 0 : 2, false, true), player);
		if (!Spirits.isBoss(attacker)) {
			attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, AuraRules.STAGGER_TICKS, 0, false, true), player);
			if (attacker instanceof Mob mob) {
				mob.getNavigation().stop();
			}
			if (attacker instanceof Player other) {
				other.resetAttackStrengthTicker();
			}
		}
	}

	/**
	 * A projectile turned back: once vanilla has bounced it off (a hit that didn't hurt), it flies back at whoever loosed it,
	 * now the guard's, a quarter faster, as a parried bolt does.
	 */
	static void reflect(ServerPlayer player, Projectile projectile) {
		Entity shooter = projectile.getOwner();
		double speed = Math.max(0.6, projectile.getDeltaMovement().length()) * Parry.REFLECT_SPEED;
		Scheduler.later(1, () -> {
			if (projectile.isRemoved()) {
				return;
			}
			projectile.setOwner(player);
			Vec3 at = projectile.position();
			Vec3 aim = shooter != null && shooter.isAlive() && shooter.level() == projectile.level() && shooter.distanceTo(player) < Parry.COUNTER_RANGE
				? shooter.getBoundingBox().getCenter().subtract(at)
				: player.getViewVector(1.0F);
			if (aim.lengthSqr() < 1.0E-4) {
				aim = player.getViewVector(1.0F);
			}
			projectile.setDeltaMovement(aim.normalize().scale(Math.min(3.0, speed)));
			projectile.needsSync = true;
			projectile.syncVelocity = true;
		});
	}

	/**
	 * Whether a spell reaching {@code target} now meets a perfect aura guard: it's parried as a Shield raised at the last moment
	 * parries one ({@code cast.Shields} turns it). The perfect moment answers once.
	 */
	public static boolean parries(LivingEntity target) {
		if (!(target instanceof ServerPlayer player) || !perfectNow(player)) {
			return false;
		}
		long now = player.level().getGameTime();
		Aura.state(player, Aura.state(player).guard(now - AuraRules.PERFECT_TICKS - 1, Aura.state(player).guardUntil()));
		feedback(player);
		return true;
	}
}
