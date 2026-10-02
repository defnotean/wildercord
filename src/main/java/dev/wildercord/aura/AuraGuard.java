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

	/** Whether the guard is in its perfect moment now (half again as long on the Way of the Bulwark). */
	public static boolean perfectNow(Player player) {
		AuraAttachments.State state = Aura.state(player);
		long now = player.level().getGameTime();
		return state.guarding(now) && AuraRules.perfect(state.guardRaised(), now, WayEffects.perfectWindow(player));
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
		return faces(player, direct.position());
	}

	/** Whether {@code from} is in front of the player (the guard's half). */
	public static boolean facing(ServerPlayer player, Vec3 from) {
		Vec3 toward = from.subtract(player.position());
		Vec3 look = player.getViewVector(1.0F);
		return toward.horizontalDistanceSqr() < 1.0E-4 || look.x * toward.x + look.z * toward.z > 0;
	}

	/** Whether the player's guard covers harm coming from {@code from}: in front, or from every side on the Way of the Bulwark. */
	public static boolean faces(ServerPlayer player, Vec3 from) {
		return facing(player, from) || WayEffects.coversAll(player);
	}

	/**
	 * A projectile reaching a player in their perfect guard's moment, from in front: turned back at whoever loosed it, a quarter
	 * faster, through vanilla's own deflection (as a breeze turns arrows), and the guard's from the next tick. Asked by
	 * {@code mixin.EntityAuraDeflectMixin} before the projectile hits; null when it isn't turned.
	 */
	public static net.minecraft.world.entity.projectile.ProjectileDeflection deflection(Entity entity, Projectile projectile) {
		if (!(entity instanceof ServerPlayer player)) {
			return null;
		}
		if (!perfectNow(player) || !faces(player, projectile.position())) {
			// A held guard on the Way of the Bulwark turns shots back too (for a little aura each).
			if (guarding(player) && faces(player, projectile.position()) && WayEffects.turnsShot(player)) {
				return turnBack(player, projectile, projectile.getOwner());
			}
			// No perfect guard: an art's ward may still turn it (Glacier Mirror's ice, the Eye of the Storm's wind).
			return dev.wildercord.aura.arts.ArtWards.deflection(player, projectile);
		}
		long now = player.level().getGameTime();
		Aura.state(player, Aura.state(player).guard(now - WayEffects.perfectWindow(player) - 1, Aura.state(player).guardUntil()));
		Entity shooter = projectile.getOwner();
		caught(player, shooter instanceof LivingEntity living ? living : null, 0);
		feedback(player);
		Momentum.guarded(player, false);
		BondedBlades.guarded(player, shooter instanceof LivingEntity living ? living : null);
		Grimoire.unlock(player, "aura:perfect_guard");
		return turnBack(player, projectile, shooter);
	}

	/** A shot turned back at whoever loosed it, a quarter faster, as the guard's own from the next tick. */
	private static net.minecraft.world.entity.projectile.ProjectileDeflection turnBack(ServerPlayer player, Projectile projectile, Entity shooter) {
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
			perfect(player, source, damage);
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
		// In a duel the guard takes the wear itself: pressure on a held guard breaks it in the end.
		if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker) {
			Stance.guarded(player, attacker, absorbed);
		}
		// The Way of the Bulwark: the held guard throws a share back at its striker, and staggers a creature that struck it.
		WayEffects.held(player, source, absorbed);
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
	private static void perfect(ServerPlayer player, DamageSource source, float damage) {
		ServerLevel level = player.level();
		long now = level.getGameTime();
		// The perfect moment answers once.
		Aura.state(player, Aura.state(player).guard(now - WayEffects.perfectWindow(player) - 1, Aura.state(player).guardUntil()));
		// What it caught, for the counter that may follow (a Third Art answers the one who struck, and some throw the blow back).
		caught(player, source.getEntity() instanceof LivingEntity attacker && attacker != player ? attacker : null, damage);
		Entity direct = source.getDirectEntity();
		LivingEntity slipFrom = null;
		Vec3 slipTo = null;
		if (source.is(Aura.DAMAGE)) {
			// Aura off a blade (a slash, a spark): a crescent is sent back at whoever loosed it, as the guard's own. Nobody is
			// staggered from across a field.
			Crescents.reflect(player, Aura.color(player), e -> dev.wildercord.cast.Targets.canHarm(player, e), AuraSlash.cutter(player));
		} else if (direct instanceof Projectile projectile) {
			reflect(player, projectile);
		} else if (source.getEntity() instanceof LivingEntity attacker && attacker != player) {
			// The Way of the Shadowstep slips behind the one who struck: it staggers where it stands then (thrown back, it would be
			// thrown into the swordsman behind it).
			slipTo = WayEffects.slipSpot(player, attacker);
			stagger(player, attacker, slipTo == null);
			// A blow turned aside whole breaks into its striker's stance.
			Stance.guardBreak(player, attacker);
			slipFrom = attacker;
		}
		feedback(player);
		if (slipTo != null) {
			// The swordsman slips behind the one who struck, for the counter to fall on its back.
			WayEffects.slip(player, slipFrom, slipTo);
		}
		Momentum.guarded(player, direct instanceof LivingEntity && !source.is(Aura.DAMAGE));
		// The bonded blade remembers it (and a Riposte readies the next blow).
		BondedBlades.guarded(player, source.getEntity() instanceof LivingEntity attacker && attacker != player ? attacker : null);
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

	/**
	 * What a perfect guard last caught: who struck (null for a spell or a projectile with no shooter near), how hard the blow was
	 * (0 for a projectile, whose harm isn't known until it lands), and when.
	 */
	public record Caught(LivingEntity attacker, float damage, long at) {}

	private static final java.util.Map<java.util.UUID, Caught> CAUGHT = new java.util.HashMap<>();

	private static void caught(ServerPlayer player, LivingEntity attacker, float damage) {
		CAUGHT.put(player.getUUID(), new Caught(attacker, Math.max(0, damage), player.level().getGameTime()));
	}

	/**
	 * What {@code player}'s last perfect guard caught, if it was within a counter's moment (and a little for the network): for a
	 * Third Art, which answers the one who struck. Null otherwise.
	 */
	public static Caught caught(ServerPlayer player) {
		Caught caught = CAUGHT.get(player.getUUID());
		if (caught == null || player.level().getGameTime() - caught.at() > StringRules.COUNTER_TICKS + StringRules.SEEN_SLACK) {
			return null;
		}
		return caught;
	}

	static void forget(java.util.UUID id) {
		CAUGHT.remove(id);
	}

	static void clear() {
		CAUGHT.clear();
	}

	/** A staggered attacker: thrown back, slowed and weakened for a moment (a boss is only slowed, as every boss is). */
	public static void stagger(ServerPlayer player, LivingEntity attacker) {
		stagger(player, attacker, true);
	}

	/** The same, thrown back only if {@code knock} (not when the guard slips behind it: the Way of the Shadowstep). */
	public static void stagger(ServerPlayer player, LivingEntity attacker, boolean knock) {
		Vec3 away = attacker.position().subtract(player.position());
		Vec3 flat = new Vec3(away.x, 0, away.z);
		if (knock && flat.lengthSqr() > 1.0E-4) {
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
		Aura.state(player, Aura.state(player).guard(now - WayEffects.perfectWindow(player) - 1, Aura.state(player).guardUntil()));
		caught(player, null, 0);
		feedback(player);
		Momentum.guarded(player, false);
		return true;
	}
}
