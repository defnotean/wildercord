package dev.wildercord.cast;

import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Resonance;
import dev.wildercord.spell.ResonanceTwists;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.IntPredicate;

/**
 * What each twist of a world's resonances does (see {@link ResonanceTwists} for the catalogue, {@link TwistVfx} for how
 * they look). A twist rides one cast: {@link #ride} marks it when the resonance wakes, the twist's set piece goes off as
 * the spell leaves the caster's hands, and {@link #onHit} (called from {@link CastEngine#onHit}) lets it change what each
 * of the spell's hits does.
 *
 * <p>Every twist is a modest extra on top of the ordinary spell, never a second spell: its damage is a few points
 * (scaled by the caster's power like any spell's), it goes through {@link Effects#hurt} (so Shields, the spell defences,
 * PvP scaling and friendly fire all hold), it takes its creatures out of the cast's budget, it caps how often it goes off
 * in a cast, and it never moves a boss.</p>
 */
public final class TwistMagic {
	private TwistMagic() {}

	/** One cast a twist rides: the resonance, and what the twist has done so far in it. */
	static final class Ride {
		final Resonance resonance;
		final ResonanceTwists.Twist twist;
		final Cast cast;
		/** Where the caster's eyes were as they cast: where a spell flew from. */
		final Vec3 origin;
		final Set<UUID> touched = new HashSet<>();
		int procs;
		boolean landed;

		Ride(Resonance resonance, ResonanceTwists.Twist twist, Cast cast) {
			this.resonance = resonance;
			this.twist = twist;
			this.cast = cast;
			this.origin = cast.caster.getEyePosition();
		}

		ServerLevel level() {
			return cast.level;
		}

		LivingEntity caster() {
			return cast.caster;
		}
	}

	/** The casts a twist rides, by the cast's identity (shared by all its parts); gone once the cast is. */
	private static final Map<Object, Ride> RIDES = new WeakHashMap<>();

	/** Marks {@code cast} as {@code resonance}'s: its twist rides it. */
	static void ride(Cast cast, Resonance resonance) {
		ResonanceTwists.Twist twist = resonance.twistDef().orElse(null);
		if (twist == null) {
			return;
		}
		Ride ride = new Ride(resonance, twist, cast);
		RIDES.put(cast.identity(), ride);
		// The spell itself leaves the caster's hands 3 ticks after the press, once its circle has formed (see SpellCaster).
		Scheduler.later(3, () -> {
			if (cast.alive()) {
				release(ride);
			}
		});
	}

	/** Whether a twist rides {@code cast} (the tests ask). */
	public static boolean rides(Cast cast) {
		return RIDES.containsKey(cast.identity());
	}

	/**
	 * Forgets every ride (the server stopped). A ride holds its cast, and so the cast's identity it's kept by: the
	 * weak keys alone never let one go, and a stopped world's casts (and their levels) mustn't outlive it.
	 */
	static void clear() {
		RIDES.clear();
	}

	/** Every hit of every spell: a twist riding it may add to what the hit does. */
	public static void onHit(Cast cast, SpellPlan.Group group, Cast.Hit hit) {
		if (RIDES.isEmpty()) {
			return;
		}
		Ride ride = RIDES.get(cast.identity());
		if (ride != null && cast.alive()) {
			hit(ride, cast, hit);
		}
	}

	// ------------------------------------------------------------------ the set pieces, as the spell leaves

	private static void release(Ride ride) {
		switch (ride.twist.id()) {
			case "pale_steed" -> paleSteed(ride);
			case "slow_hour" -> slowHour(ride);
			case "paper_storm" -> paperStorm(ride);
			case "mirror_shards" -> mirrorShards(ride);
			case "aurora" -> aurora(ride);
			case "red_moon" -> TwistVfx.redMoon(ride.level(), ride.caster());
			case "lantern_flies" -> lanternFlies(ride);
			case "rime_steps" -> rimeSteps(ride);
			case "great_bell" -> greatBell(ride);
			default -> {
			}
		}
	}

	// ------------------------------------------------------------------ changing each hit

	private static void hit(Ride ride, Cast cast, Cast.Hit hit) {
		switch (ride.twist.id()) {
			case "glass_rain" -> glassRain(ride, cast, hit);
			case "kindly_flame" -> kindlyFlame(ride, hit);
			case "winter_blossom" -> winterBlossom(ride, hit);
			case "thorn_crown" -> thornCrown(ride, hit);
			case "soul_lanterns" -> soulLanterns(ride, hit);
			case "turning_tide" -> turningTide(ride, cast, hit);
			case "storm_crown" -> stormCrown(ride, hit);
			case "red_moon" -> redMoon(ride, hit);
			case "tolling_hour" -> tollingHour(ride, hit);
			default -> {
				if (firstLanding(ride, hit)) {
					switch (ride.twist.id()) {
						case "light_birds" -> lightBirds(ride, hit);
						case "upfall" -> upfall(ride, cast, hit);
						case "second_voice" -> secondVoice(ride, hit);
						case "star_wake" -> starWake(ride, hit);
						case "standing_stones" -> standingStones(ride, cast, hit);
						case "sun_seal" -> sunSeal(ride, hit);
						case "falling_blades" -> fallingBlades(ride, cast, hit);
						case "eddy" -> eddy(ride, hit);
						case "crackling_wake" -> cracklingWake(ride, cast, hit);
						case "thicket" -> thicket(ride, cast, hit);
						default -> {
						}
					}
				}
			}
		}
	}

	/** True the first time the spell lands somewhere (anywhere but on its own caster). */
	private static boolean firstLanding(Ride ride, Cast.Hit hit) {
		if (ride.landed || hit.self()) {
			return false;
		}
		ride.landed = true;
		return true;
	}

	/** Glass Rain: each strike shatters as glass over what's beneath it (ten strikes a cast at most). */
	private static void glassRain(Ride ride, Cast cast, Cast.Hit hit) {
		if (hit.self() || ride.procs >= 10) {
			return;
		}
		ride.procs++;
		TwistVfx.glass(ride.level(), hit.point());
		for (LivingEntity foe : foes(cast, hit.point(), 1.8)) {
			if (cast.takeEntities(1) == 1) {
				hurt(cast, foe, "", magic(cast), 2.0);
				foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 0, false, true));
			}
		}
	}

	/** Kindly Flame: allies near where it lands are mended, once each a cast. */
	private static void kindlyFlame(Ride ride, Cast.Hit hit) {
		for (LivingEntity ally : friends(ride.cast, hit.point(), 4.0)) {
			if (ride.procs < 8 && ride.touched.add(ally.getUUID())) {
				ride.procs++;
				ally.heal((float) (2.5 * ride.cast.power));
				TwistVfx.kindly(ride.level(), ally);
			}
		}
	}

	/** Birds of Light: five birds burst out of where it first strikes, each pecking one of the nearest foes. */
	private static void lightBirds(Ride ride, Cast.Hit hit) {
		Vec3 at = hit.point();
		int color = ride.resonance.color();
		TwistVfx.birdsTakeOff(ride.level(), at, color);
		List<LivingEntity> foes = foes(ride.cast, at, 9.0);
		for (int i = 0; i < 5; i++) {
			int ticks = 10 + i * 2;
			if (foes.isEmpty()) {
				TwistVfx.bird(ride.level(), at, at.add(ElementFx.flatDir(i * 1.25).scale(4)).add(0, 5, 0), color, ticks);
				continue;
			}
			LivingEntity prey = foes.get(i % foes.size());
			TwistVfx.bird(ride.level(), at, prey.getBoundingBox().getCenter(), color, ticks);
			Scheduler.later(ticks, () -> {
				if (ride.cast.alive() && prey.isAlive() && ride.cast.takeEntities(1) == 1) {
					hurt(ride.cast, prey, "", magic(ride.cast), 2.0);
					TwistVfx.peck(ride.level(), prey.getBoundingBox().getCenter(), color);
				}
			});
		}
	}

	/** Winter Blossom: flowers bloom where the frost lands (three patches a cast), and allies among them mend. */
	private static void winterBlossom(Ride ride, Cast.Hit hit) {
		if (hit.self() || ride.procs >= 3) {
			return;
		}
		ride.procs++;
		TwistVfx.blossom(ride.level(), hit.point());
		for (LivingEntity ally : friends(ride.cast, hit.point(), 3.0)) {
			ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, true));
		}
	}

	/** Upfall: foes near where it lands fall upward, hang, and come crashing down. */
	private static void upfall(Ride ride, Cast cast, Cast.Hit hit) {
		Vec3 at = hit.point();
		TwistVfx.upfallOmen(ride.level(), at, 4.5);
		List<LivingEntity> lifted = new ArrayList<>();
		for (LivingEntity foe : foes(cast, at, 4.5)) {
			if (!Spirits.isBoss(foe) && !VoidTime.anchored(foe) && cast.takeEntities(1) == 1) {
				lifted.add(foe);
				Statuses.airborne(foe, 40);
			}
		}
		every(ride.cast, 1, 24, tick -> {
			for (LivingEntity foe : lifted) {
				if (!foe.isAlive()) {
					continue;
				}
				if (tick < 14) {
					// Falling up: gently at first, then faster, as if the sky had become the ground.
					Vec3 v = foe.getDeltaMovement();
					move(foe, new Vec3(v.x * 0.6, 0.12 + tick * 0.014, v.z * 0.6));
					if (tick % 3 == 0) {
						TwistVfx.floating(ride.level(), foe);
					}
				} else if (tick < 23) {
					move(foe, new Vec3(0, 0.0, 0));
				} else {
					move(foe, new Vec3(0, -1.5, 0));
					TwistVfx.slam(ride.level(), foe);
					hurt(ride.cast, foe, "", magic(ride.cast), 3.0);
				}
			}
			return true;
		});
	}

	/** Second Voice: a second after it lands, the whole spell sounds again from there, at half strength. */
	private static void secondVoice(Ride ride, Cast.Hit hit) {
		Vec3 at = hit.point().add(0, 0.6, 0);
		Vec3 dir = hit.dir();
		Scheduler.later(20, () -> {
			if (!ride.cast.alive() || ride.cast.info.root() == null) {
				return;
			}
			TwistVfx.secondVoice(ride.level(), hit.point(), ride.cast.info.spell(), ride.resonance.color());
			// A copy for the same payment, with a budget of its own; it carries no twist, so it never echoes again.
			Cast echo = ride.cast.again(0.5);
			CastEngine.runSegment(echo, ride.cast.info.root(), new Cast.Trigger(at, dir, null, null, null));
		});
	}

	/** Star Wake: three small stars fall around where it lands. */
	private static void starWake(Ride ride, Cast.Hit hit) {
		Vec3 at = hit.point();
		for (int k = 0; k < 3; k++) {
			int star = k;
			double a = ride.level().getRandom().nextDouble() * Math.PI * 2;
			double d = 0.8 + ride.level().getRandom().nextDouble() * 1.7;
			Vec3 where = TwistVfx.groundAt(ride.level(), at.add(Math.cos(a) * d, 0, Math.sin(a) * d));
			Scheduler.later(6 + 5 * k, () -> {
				if (!ride.cast.alive()) {
					return;
				}
				TwistVfx.star(ride.level(), where, star);
				for (LivingEntity foe : foes(ride.cast, where, 1.6)) {
					if (ride.cast.takeEntities(1) == 1) {
						hurt(ride.cast, foe, "", magic(ride.cast), 2.5);
					}
				}
			});
		}
	}

	/** Crown of Thorns: each foe struck (six a cast) is crowned with thorns that bite three times and slow it. */
	private static void thornCrown(Ride ride, Cast.Hit hit) {
		for (LivingEntity foe : struck(ride, hit)) {
			if (ride.procs >= 6 || !ride.touched.add(foe.getUUID())) {
				continue;
			}
			ride.procs++;
			foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0, false, true));
			TwistVfx.thorns(ride.level(), foe, true);
			every(ride.cast, 10, 6, tick -> {
				if (!foe.isAlive()) {
					return false;
				}
				TwistVfx.thorns(ride.level(), foe, false);
				if (tick % 2 == 1) {
					Effects.lingering(() -> hurt(ride.cast, foe, "", magic(ride.cast), 1.0));
				}
				return true;
			});
		}
	}

	/** Soul Lanterns: a foe the spell ends gives up a lantern that drifts to the caster and mends them (three a cast). */
	private static void soulLanterns(Ride ride, Cast.Hit hit) {
		List<LivingEntity> struck = struck(ride, hit);
		if (struck.isEmpty()) {
			return;
		}
		Scheduler.later(2, () -> {
			for (LivingEntity foe : struck) {
				if (ride.procs >= 3 || foe.isAlive() && !foe.isDeadOrDying() || !ride.touched.add(foe.getUUID())) {
					continue;
				}
				ride.procs++;
				TwistVfx.lantern(ride.level(), foe.getBoundingBox().getCenter(), ride.caster().getBoundingBox().getCenter());
				Scheduler.later(24, () -> {
					if (ride.caster().isAlive()) {
						ride.caster().heal((float) (2.0 * ride.cast.power));
						Vfx.heal(ride.level(), ride.caster());
					}
				});
			}
		});
	}

	/** Turning Tide: a ring of seawater bursts out where it lands (twice a cast), soaking and shoving foes, putting out friends. */
	private static void turningTide(Ride ride, Cast cast, Cast.Hit hit) {
		if (hit.self() || ride.procs >= 2) {
			return;
		}
		ride.procs++;
		Vec3 at = hit.point();
		TwistVfx.tide(ride.level(), at, 4.0);
		for (LivingEntity foe : foes(cast, at, 4.0)) {
			if (cast.takeEntities(1) == 1) {
				Reactions.mark(foe, Reactions.Mark.SOAKED);
				if (!Spirits.isBoss(foe)) {
					push(foe, away(at, foe).scale(1.1).add(0, 0.25, 0));
				}
			}
		}
		for (LivingEntity ally : friends(cast, at, 4.0)) {
			ally.clearFire();
		}
	}

	/** Standing Stones: a ring of stones heaves up where it lands, battering and tossing the foes inside. */
	private static void standingStones(Ride ride, Cast cast, Cast.Hit hit) {
		Vec3 at = hit.point();
		TwistVfx.stones(ride.level(), at, 2.6);
		for (LivingEntity foe : foes(cast, at, 3.2)) {
			if (cast.takeEntities(1) == 1) {
				hurt(cast, foe, "", magic(cast), 3.0);
				if (!Spirits.isBoss(foe)) {
					push(foe, new Vec3(0, 0.45, 0));
				}
			}
		}
	}

	/** Storm Crown: each foe struck (four a cast) is followed by a little cloud that zaps it three times. */
	private static void stormCrown(Ride ride, Cast.Hit hit) {
		for (LivingEntity foe : struck(ride, hit)) {
			if (ride.procs >= 4 || !ride.touched.add(foe.getUUID())) {
				continue;
			}
			ride.procs++;
			every(ride.cast, 5, 12, tick -> {
				if (!foe.isAlive()) {
					return false;
				}
				TwistVfx.cloud(ride.level(), foe);
				if (tick % 4 == 3) {
					int zap = tick / 4;
					TwistVfx.zap(ride.level(), foe, zap);
					Effects.lingering(() -> hurt(ride.cast, foe, "storm", ride.level().damageSources().source(DamageTypes.LIGHTNING_BOLT, ride.caster()), 1.5));
				}
				return true;
			});
		}
	}

	/** Red Moon: each foe struck (six a cast) bleeds a drop of light back to the caster. */
	private static void redMoon(Ride ride, Cast.Hit hit) {
		for (LivingEntity foe : struck(ride, hit)) {
			if (ride.procs >= 6 || !ride.touched.add(foe.getUUID())) {
				continue;
			}
			ride.procs++;
			TwistVfx.drop(ride.level(), foe.getBoundingBox().getCenter(), ride.caster().getBoundingBox().getCenter());
			Scheduler.later(12, () -> {
				if (ride.caster().isAlive()) {
					ride.caster().heal((float) (1.0 * ride.cast.power));
				}
			});
		}
	}

	/** Sun Seal: a seal of the sun burns where it lands for three seconds, scorching the foes on it. */
	private static void sunSeal(Ride ride, Cast.Hit hit) {
		Vec3 at = hit.point();
		double radius = 2.5;
		TwistVfx.sunSeal(ride.level(), at, radius);
		every(ride.cast, 20, 3, tick -> {
			TwistVfx.sunPulse(ride.level(), at, radius);
			Cast pulse = ride.cast.pulse();
			for (LivingEntity foe : foes(pulse, at, radius)) {
				if (pulse.takeEntities(1) == 1) {
					foe.igniteForSeconds(2);
					Effects.lingering(() -> hurt(pulse, foe, "fire", pulse.level.damageSources().source(DamageTypes.IN_FIRE, pulse.caster), 1.0));
				}
			}
			return true;
		});
	}

	/** Falling Blades: blades hang over the three nearest foes and fall on them. */
	private static void fallingBlades(Ride ride, Cast cast, Cast.Hit hit) {
		List<LivingEntity> foes = foes(cast, hit.point(), 6.0);
		for (int k = 0; k < Math.min(3, foes.size()); k++) {
			LivingEntity foe = foes.get(k);
			if (cast.takeEntities(1) != 1) {
				break;
			}
			int blade = k;
			Vec3 over = foe.position().add(0, foe.getBbHeight() + 2.5, 0);
			TwistVfx.bladeHangs(ride.level(), over);
			Scheduler.later(12 + 4 * k, () -> {
				if (ride.cast.alive() && foe.isAlive()) {
					TwistVfx.bladeFalls(ride.level(), over, foe, blade);
					hurt(ride.cast, foe, "", magic(ride.cast), 3.0);
					Reactions.mark(foe, Reactions.Mark.BLEEDING);
				}
			});
		}
	}

	/** Whirling Eddy: a whirlwind spins up where it lands, drags foes round for two seconds, then flings them out. */
	private static void eddy(Ride ride, Cast.Hit hit) {
		Vec3 centre = hit.point();
		double radius = 3.5;
		TwistVfx.eddy(ride.level(), centre, radius, true);
		every(ride.cast, 2, 20, tick -> {
			if (tick % 4 == 0) {
				TwistVfx.eddy(ride.level(), centre, radius, false);
			}
			boolean last = tick == 19;
			Cast pulse = last ? ride.cast.pulse() : null;
			for (LivingEntity foe : foes(ride.cast, centre, radius)) {
				if (Spirits.isBoss(foe) || VoidTime.anchored(foe)) {
					continue;
				}
				Vec3 in = new Vec3(centre.x - foe.getX(), 0, centre.z - foe.getZ());
				if (last) {
					push(foe, in.lengthSqr() < 1.0E-4 ? new Vec3(0, 0.6, 0) : in.normalize().scale(-1.0).add(0, 0.35, 0));
					if (pulse.takeEntities(1) == 1) {
						hurt(pulse, foe, "", magic(pulse), 2.0);
					}
				} else if (in.lengthSqr() > 1.0E-4) {
					Vec3 round = new Vec3(-in.z, 0, in.x).normalize().scale(0.16).add(in.normalize().scale(0.05)).add(0, 0.04, 0);
					push(foe, round);
				}
			}
			return true;
		});
	}

	/** Tolling Hour: a clock opens over each foe struck (four a cast); three seconds later it tolls, and the foe takes a blow. */
	private static void tollingHour(Ride ride, Cast.Hit hit) {
		for (LivingEntity foe : struck(ride, hit)) {
			if (ride.procs >= 4 || !ride.touched.add(foe.getUUID())) {
				continue;
			}
			ride.procs++;
			every(ride.cast, 20, 4, tick -> {
				if (!foe.isAlive()) {
					return false;
				}
				if (tick < 3) {
					TwistVfx.clockOver(ride.level(), foe);
				} else {
					TwistVfx.toll(ride.level(), foe);
					hurt(ride.cast, foe, "", magic(ride.cast), 3.0);
				}
				return true;
			});
		}
	}

	/** Crackling Wake: sparks run back along the way it flew, bursting on foes beside the path. */
	private static void cracklingWake(Ride ride, Cast cast, Cast.Hit hit) {
		Vec3 end = hit.point();
		Vec3 path = end.subtract(ride.origin);
		double length = path.length();
		if (length < 1.0) {
			return;
		}
		Vec3 back = path.normalize().scale(-1);
		for (int k = 0; k < 4; k++) {
			int burst = k;
			Vec3 at = end.add(back.scale(length * (0.2 + 0.2 * k)));
			Scheduler.later(1 + 3 * k, () -> {
				if (!ride.cast.alive()) {
					return;
				}
				TwistVfx.crackle(ride.level(), at, burst);
				for (LivingEntity foe : foes(ride.cast, at, 1.6)) {
					if (ride.touched.add(foe.getUUID()) && ride.cast.takeEntities(1) == 1) {
						hurt(ride.cast, foe, "", ride.level().damageSources().source(DamageTypes.LIGHTNING_BOLT, ride.caster()), 2.0);
					}
				}
			});
		}
	}

	/** Sudden Thicket: a thicket bursts up where it lands and holds the foes in it fast for a moment. */
	private static void thicket(Ride ride, Cast cast, Cast.Hit hit) {
		Vec3 at = hit.point();
		TwistVfx.thicket(ride.level(), at, 3.0);
		for (LivingEntity foe : foes(cast, at, 3.0)) {
			if (cast.takeEntities(1) != 1) {
				continue;
			}
			boolean boss = Spirits.isBoss(foe);
			int hold = foe instanceof Player ? 15 : 30;
			foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, hold, boss ? 1 : 9, false, true));
			if (!boss && !VoidTime.anchored(foe)) {
				move(foe, new Vec3(0, Math.min(0, foe.getDeltaMovement().y), 0));
			}
			hurt(cast, foe, "", magic(cast), 1.0);
		}
	}

	// ------------------------------------------------------------------ set pieces

	/** Pale Steed: a spectral horse rises under the caster and carries them for half a minute. */
	private static void paleSteed(Ride ride) {
		ServerLevel level = ride.level();
		if (!(ride.caster() instanceof ServerPlayer player) || player.isPassenger() || player.isSpectator()) {
			TwistVfx.almost(level, ride.caster(), ride.resonance.color());
			return;
		}
		SkeletonHorse horse = EntityTypes.SKELETON_HORSE.create(level, EntitySpawnReason.TRIGGERED);
		if (horse == null) {
			return;
		}
		horse.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
		horse.setTamed(true);
		horse.setOwner(player);
		horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
		horse.setPermanentlyInvulnerable(true);
		horse.setPersistenceRequired();
		var speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.setBaseValue(0.32);
		}
		var jump = horse.getAttribute(Attributes.JUMP_STRENGTH);
		if (jump != null) {
			jump.setBaseValue(0.9);
		}
		// Saved with an end time, as a summoned wolf is, so a restart can never leave it behind for good (see Spirits).
		horse.setAttached(WildercordAttachments.SPIRIT_UNTIL, level.getGameTime() + 600);
		level.addFreshEntity(horse);
		player.startRiding(horse, true, true);
		TwistVfx.steedRises(level, horse);
		int[] empty = {0};
		every(null, 5, 120, tick -> {
			if (horse.isRemoved()) {
				return false;
			}
			boolean ridden = horse.hasPassenger(player);
			empty[0] = ridden ? 0 : empty[0] + 1;
			if (tick == 119 || empty[0] >= 3 || !player.isAlive()) {
				horse.ejectPassengers();
				TwistVfx.steedFades(level, horse);
				horse.discard();
				return false;
			}
			TwistVfx.steedTrail(level, horse);
			return true;
		});
	}

	/** Slow Hour: for six seconds, arrows and thrown things near the caster crawl through the air. */
	private static void slowHour(Ride ride) {
		TwistVfx.slowHour(ride.level(), ride.caster(), true);
		every(ride.cast, 1, 120, tick -> {
			LivingEntity caster = ride.caster();
			for (Projectile shot : ride.level().getEntitiesOfClass(Projectile.class, caster.getBoundingBox().inflate(8.0), p -> !friendly(caster, p))) {
				Vec3 v = shot.getDeltaMovement();
				if (v.lengthSqr() > 0.02) {
					shot.setDeltaMovement(v.scale(0.72));
					shot.needsSync = true;
				}
				if (tick % 4 == 0) {
					TwistVfx.slowed(ride.level(), shot);
				}
			}
			if (tick % 20 == 19) {
				TwistVfx.slowHour(ride.level(), caster, false);
			}
			return true;
		});
	}

	/** Paper Storm: three whirls of pages round the caster, cutting and lighting up the foes they catch. */
	private static void paperStorm(Ride ride) {
		every(ride.cast, 20, 3, tick -> {
			TwistVfx.pages(ride.level(), ride.caster(), tick);
			Cast pulse = ride.cast.pulse();
			for (LivingEntity foe : foes(pulse, ride.caster().position().add(0, 1, 0), 5.0)) {
				if (pulse.takeEntities(1) == 1) {
					hurt(pulse, foe, "", magic(pulse), 1.5);
					foe.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, false));
					TwistVfx.pageCut(ride.level(), foe);
				}
			}
			return true;
		});
	}

	/** Mirror Shards: three shards circle the caster for ten seconds; each strikes the next foe that comes close. */
	private static void mirrorShards(Ride ride) {
		int[] left = {3};
		every(ride.cast, 2, 100, tick -> {
			LivingEntity caster = ride.caster();
			TwistVfx.shards(ride.level(), caster, left[0], tick);
			if (tick % 3 == 0) {
				List<LivingEntity> near = foes(ride.cast, caster.getBoundingBox().getCenter(), 3.5);
				if (!near.isEmpty()) {
					LivingEntity foe = near.getFirst();
					Cast pulse = ride.cast.pulse();
					if (pulse.takeEntities(1) == 1) {
						left[0]--;
						TwistVfx.shardStrike(ride.level(), caster.getBoundingBox().getCenter(), foe.getBoundingBox().getCenter(), 3 - left[0]);
						hurt(pulse, foe, "", magic(pulse), 3.0);
						if (!Spirits.isBoss(foe)) {
							push(foe, away(caster.position(), foe).scale(0.6).add(0, 0.2, 0));
						}
					}
				}
			}
			return left[0] > 0;
		});
	}

	/** Aurora: ribbons of colour unroll overhead, and the caster and allies beneath are wrapped in a little protection. */
	private static void aurora(Ride ride) {
		for (int band = 0; band < 3; band++) {
			int b = band;
			Scheduler.later(1 + band * 6, () -> {
				if (ride.cast.alive()) {
					TwistVfx.aurora(ride.level(), ride.caster(), b);
				}
			});
		}
		for (LivingEntity ally : friends(ride.cast, ride.caster().position(), 8.0)) {
			ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0, false, true));
			// Never more than two hearts of it, and never less than what they had.
			ally.setAbsorptionAmount(Math.max(ally.getAbsorptionAmount(), 4.0F));
			TwistVfx.shimmer(ride.level(), ally);
		}
	}

	/** Lantern Flies: fireflies spill out; allies see in the dark, and hidden foes are lit. */
	private static void lanternFlies(Ride ride) {
		for (int wave = 0; wave < 3; wave++) {
			int w = wave;
			Scheduler.later(1 + wave * 10, () -> {
				if (ride.cast.alive()) {
					TwistVfx.fireflies(ride.level(), ride.caster(), w);
				}
			});
		}
		Vec3 at = ride.caster().position();
		for (LivingEntity ally : friends(ride.cast, at, 8.0)) {
			ally.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, true));
		}
		for (LivingEntity foe : foes(ride.cast, at, 12.0)) {
			foe.removeEffect(MobEffects.INVISIBILITY);
			foe.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0, false, false));
		}
	}

	/** Rime Steps: for six seconds the caster's footsteps leave rime flowers, and foes who tread on them are slowed. */
	private static void rimeSteps(Ride ride) {
		List<Vec3> flowers = new ArrayList<>();
		List<Integer> until = new ArrayList<>();
		Vec3[] last = {null};
		every(ride.cast, 1, 220, tick -> {
			LivingEntity caster = ride.caster();
			if (tick < 120 && tick % 4 == 0 && caster.onGround() && (last[0] == null || last[0].distanceToSqr(caster.position()) > 0.64)
					&& flowers.size() < 24) {
				last[0] = caster.position();
				flowers.add(caster.position());
				until.add(tick + 100);
				TwistVfx.rime(ride.level(), caster.position(), flowers.size() == 1);
			}
			if (tick % 5 == 0) {
				for (int i = 0; i < flowers.size(); i++) {
					if (until.get(i) < tick) {
						continue;
					}
					for (LivingEntity foe : foes(ride.cast, flowers.get(i), 1.1)) {
						foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, false, true));
					}
				}
			}
			return true;
		});
	}

	/** Great Bell: a bell rings out over the caster; foes nearby reel, allies are quickened. */
	private static void greatBell(Ride ride) {
		LivingEntity caster = ride.caster();
		TwistVfx.bell(ride.level(), caster);
		for (LivingEntity foe : foes(ride.cast, caster.position(), 6.0)) {
			foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0, false, true));
			foe.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true));
			if (!Spirits.isBoss(foe)) {
				push(foe, away(caster.position(), foe).scale(0.4).add(0, 0.15, 0));
			}
		}
		for (LivingEntity ally : friends(ride.cast, caster.position(), 6.0)) {
			ally.addEffect(new MobEffectInstance(MobEffects.SPEED, 80, 0, false, true));
		}
	}

	// ------------------------------------------------------------------ helpers

	/**
	 * Runs {@code step} every {@code period} ticks, {@code count} times, booking each step as the last one runs (the
	 * Scheduler walks every waiting task each tick). It stops early when {@code step} answers false, or when {@code cast}
	 * (if given) is no longer alive.
	 */
	private static void every(Cast cast, int period, int count, IntPredicate step) {
		book(cast, period, count, 0, step);
	}

	private static void book(Cast cast, int period, int count, int tick, IntPredicate step) {
		Scheduler.later(tick == 0 ? 1 : period, () -> {
			if (cast != null && !cast.alive()) {
				return;
			}
			if (step.test(tick) && tick + 1 < count) {
				book(cast, period, count, tick + 1, step);
			}
		});
	}

	/** Foes within {@code radius} of {@code at}, nearest first. */
	static List<LivingEntity> foes(Cast cast, Vec3 at, double radius) {
		List<LivingEntity> foes = new ArrayList<>(cast.level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
			e -> e.isAlive() && e.distanceToSqr(at) <= radius * radius && Targets.canHarm(cast.caster, e)));
		foes.sort(Comparator.comparingDouble(e -> e.distanceToSqr(at)));
		return foes;
	}

	/** The caster and their allies within {@code radius} of {@code at}. */
	static List<LivingEntity> friends(Cast cast, Vec3 at, double radius) {
		List<LivingEntity> friends = new ArrayList<>(cast.level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
			e -> e != cast.caster && e.distanceToSqr(at) <= radius * radius && Targets.canHelp(cast.caster, e)));
		if (cast.caster.isAlive() && cast.caster.distanceToSqr(at) <= radius * radius) {
			friends.add(0, cast.caster);
		}
		return friends;
	}

	/** The living foes a hit struck. */
	private static List<LivingEntity> struck(Ride ride, Cast.Hit hit) {
		List<LivingEntity> struck = new ArrayList<>();
		for (Entity e : hit.entities()) {
			if (e instanceof LivingEntity living && living.isAlive() && Targets.canHarm(ride.caster(), living)) {
				struck.add(living);
			}
		}
		return struck;
	}

	/** Whether a projectile is the caster's own, or a friend's: the Slow Hour lets those fly. */
	private static boolean friendly(LivingEntity caster, Projectile shot) {
		Entity owner = shot.getOwner();
		return owner == caster || owner != null && Targets.canHelp(caster, owner);
	}

	/** Damage from a twist: through the one door every spell's damage goes through, as its own element (or none). */
	private static void hurt(Cast cast, LivingEntity target, String element, DamageSource source, double amount) {
		Effects.asElement(element, () -> Effects.hurt(cast, target, source, amount * cast.power));
	}

	private static DamageSource magic(Cast cast) {
		return cast.level.damageSources().indirectMagic(cast.caster, cast.caster);
	}

	/** A level direction from {@code from} out to {@code target}. */
	private static Vec3 away(Vec3 from, Entity target) {
		Vec3 d = new Vec3(target.getX() - from.x, 0, target.getZ() - from.z);
		return d.lengthSqr() < 1.0E-4 ? ElementFx.flatDir(target.getRandom().nextDouble() * Math.PI * 2) : d.normalize();
	}

	/** A shove, as a spell's push is (an anchored creature isn't moved, knockback resistance counts). */
	private static void push(LivingEntity target, Vec3 impulse) {
		Effects.push(target, impulse);
	}

	/** Sets a creature's motion outright, telling a player's client (it moves itself). */
	private static void move(LivingEntity target, Vec3 velocity) {
		if (VoidTime.anchored(target)) {
			return;
		}
		target.setDeltaMovement(velocity);
		target.needsSync = true;
		if (target instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
	}
}
