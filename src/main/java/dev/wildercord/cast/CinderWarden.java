package dev.wildercord.cast;

import static dev.wildercord.cast.CinderWardenRules.*;

import dev.wildercord.content.dungeons.DungeonSounds;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Cinder Warden, keeper of the Ember Sanctum: a hulking figure of magma plates and chains. Its
 * armour turns every plain blow and every single-element spell (a tenth of it gets through); only an
 * element reaction set off on it (any of them: Shatter, Conduct, Overload, Fracture...) cracks it open
 * for full damage, and leaves it open for a moment after. It teaches reactions by using them: its own
 * spells set them up on you (freeze, then burn; throw, then burn; pull, then blast), and its name
 * on the boss bar says which.
 * <p>
 * Between its spells it fights with its fists, its chains and its forge-heat (see {@link CinderWardenRules}): it hammers both
 * fists down on a ring before it and is left with them sunk in the floor; lets its chains out and drags them round in a ring to be
 * jumped; drags a seam of fire through the floor that bursts along its length; vents steam out of its back at anyone behind it;
 * casts a wrist chain to haul its prey in, or itself after them; spews slag over the hall onto where everyone stands; breathes fire
 * in a turning cone; and at the last draws all its heat in and bursts it across the hall, so only stone shelters you. Its moves
 * play into its armour: venting leaves it steaming (storm conducts), a missed haul pulled taut in its own chain (blast it), its
 * breath fanned open (burn it), a flare burnt out, cold and brittle (burn or break it); and a reaction that catches it winding
 * up rocks it back on its heels, the move lost.
 */
public class CinderWarden extends DungeonBoss {
	private static final int COLOR = 0xFF6A1E;
	private static final int ACCENT = 0xFFC040;
	/** How long a reaction leaves its armour cracked open (full damage from any spell). */
	private static final int CRACK_TICKS = 30;
	/** How far its chain reaches out (the haul's and the sweep's), for its pose. */
	private static final EntityDataAccessor<Float> DATA_REACH = SynchedEntityData.defineId(CinderWarden.class, EntityDataSerializers.FLOAT);
	/** Which way its chains go round in a sweep, 1 or -1, for its pose. */
	private static final EntityDataAccessor<Float> DATA_SPIN = SynchedEntityData.defineId(CinderWarden.class, EntityDataSerializers.FLOAT);

	private static final List<List<RuneDef>> PHASE_1 = List.of(
		List.of(Runes.BOLT, Runes.FROST, Runes.DELAY, Runes.BOLT, Runes.FIRE),
		List.of(Runes.PILLAR, Runes.FIRE),
		List.of(Runes.WAVE, Runes.EMBER));
	private static final List<List<RuneDef>> PHASE_2 = List.of(
		List.of(Runes.BOLT, Runes.DASH, Runes.DELAY, Runes.COMET, Runes.FIRE),
		List.of(Runes.RING, Runes.FROST, Runes.DELAY, Runes.RING, Runes.FIRE),
		List.of(Runes.RAIN, Runes.FIRE),
		List.of(Runes.CONE, Runes.FLASHFIRE));
	private static final List<List<RuneDef>> PHASE_3 = List.of(
		List.of(Runes.BOLT, Runes.PULL, Runes.DELAY, Runes.COMET, Runes.EXPLODE),
		List.of(Runes.BOLT, Runes.FROST, Runes.DELAY, Runes.BOLT, Runes.FIRE, Runes.AMPLIFY),
		List.of(Runes.ZONE, Runes.INFERNO),
		List.of(Runes.WAVE, Runes.FROST, Runes.DELAY, Runes.WAVE, Runes.FIRE));

	private long crackedUntil;
	/** When each player was last told why their blow did nothing, so it's said once in a while, not every hit. */
	private final Map<UUID, Long> told = new HashMap<>();
	/** Whoever its move under way has struck already, so no one is struck twice by one swing. */
	private final Set<UUID> struck = new HashSet<>();
	/** When its breath last caught each, so it burns them in beats, not every tick. */
	private final Map<UUID, Long> scorched = new HashMap<>();
	/** Walls of fire rolling out from its hammerfalls, slag in the air, and the pools it leaves burning. */
	private final List<Wave> waves = new ArrayList<>();
	private final List<Gob> gobs = new ArrayList<>();
	private final List<Pool> pools = new ArrayList<>();
	/** Where its move under way is aimed (degrees, locked a few ticks before the blow), and where its sweep's chain is now. */
	private float aim, spin;
	/** Which way its next sweep goes round. */
	private int spinWay = 1;
	/** Its haul: the hook where the chain bit, whoever it caught, how far the chain has run out, and how far it can before stone. */
	private Vec3 hook;
	private LivingEntity caught;
	private double cast, haulTo;
	/** The seams it drags, how far each runs before stone stops it. */
	private double[] seamYaw = new double[0];
	private double[] seamTo = new double[0];
	/** When it last flared, last reeled, and when its second volley of slag comes down. */
	private long lastFlare = -FLARE_EVERY, lastReel = -REEL_EVERY, volleyAt;
	/** Whether it has cast its lesson this phase (its first spell comes early). */
	private boolean taught;
	/** Where its prey stood a moment ago, and whether they've barely moved since. */
	private Vec3 lastSpot;
	private boolean idle;
	/** Its chain's reach, eased (client). */
	private float reach, reachO;

	public CinderWarden(EntityType<? extends CinderWarden> type, Level level) {
		super(type, level, BossEvent.BossBarColor.RED);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 300.0)
			.add(Attributes.MOVEMENT_SPEED, 0.23)
			.add(Attributes.FOLLOW_RANGE, 40.0)
			.add(Attributes.ARMOR, 4.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.ATTACK_DAMAGE, 10.0);
	}

	/** Wakes the Warden at its forge-altar: the altar spits fire and it climbs out of the coals. */
	public static CinderWarden rise(ServerLevel level, BlockPos altar) {
		CinderWarden boss = place(level, DungeonEntities.CINDER_WARDEN, altar, 1.0);
		if (boss == null) {
			return null;
		}
		Vec3 at = Vec3.atBottomCenterOf(altar).add(0, 1, 0);
		Sigils.ground(level, at, COLOR, ACCENT, 5.0F, 80);
		ElementFx.flameBurst(level, at.add(0, 1.2, 0), 2.5, 16);
		ElementFx.heatFlare(level, at.add(0, 1.5, 0), 2.5);
		Vfx.emit(level, ParticleTypes.LAVA, at.add(0, 1, 0), 30, 1.2, 0.3);
		Motes.clouds(level, at.add(0, 2, 0), 10, 1.0, Motes.SMOKE, 2.0, 50, new Vec3(0, 0.04, 0), 0.04, 0.45);
		Fx.sound(level, at, DungeonSounds.BOSS_RISE, 2.0F, 1.0F);
		Fx.sound(level, at, SoundEvents.BLAZE_SHOOT, 1.2F, 0.5F);
		boss.announce(level, "message.wildercord.cinder_warden_wakes", COLOR);
		return boss;
	}

	@Override
	protected List<List<RuneDef>> spells(int phase) {
		return phase == 1 ? PHASE_1 : phase == 2 ? PHASE_2 : PHASE_3;
	}

	@Override
	protected String feat() {
		return Feats.CINDER_WARDEN;
	}

	@Override
	protected int color() {
		return COLOR;
	}

	@Override
	protected int accent() {
		return ACCENT;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_REACH, 0.0F);
		builder.define(DATA_SPIN, 1.0F);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			reachO = reach;
			reach = Mth.approach(reach, entityData.get(DATA_REACH), 3.5F);
		}
	}

	/** 0: how far its chain is let out, in blocks; 1: which way its sweep goes round. */
	@Override
	public float extra(int i, float partial) {
		return i == 0 ? Mth.lerp(partial, reachO, reach) : i == 1 ? entityData.get(DATA_SPIN) : 0;
	}

	// ------------------------------------------------------------------ moving

	@Override
	protected void approach(ServerLevel level, LivingEntity target) {
		if (distanceTo(target) > 3.0) {
			getNavigation().moveTo(target, 1.0);
		} else {
			getNavigation().stop();
			getLookControl().setLookAt(target, 30, 30);
		}
	}

	/**
	 * Lava and fire are home: nothing sets it alight, not even for the rest of a tick, so no spell's fire
	 * on it can feed Overload, Elapse or Unweave (it isn't fire immune, or fire spells would read as immune).
	 */
	@Override
	public void setRemainingFireTicks(int ticks) {
		super.setRemainingFireTicks(Math.min(0, ticks));
	}

	@Override
	protected void mechanic(ServerLevel level, long now) {
		// Lava and fire are home: it never burns.
		clearFire();
		setState(EXPOSED, now < crackedUntil);
		if (castAt > 0) {
			taught = true;
		}
		Vec3 c = position().add(0, getBbHeight() * 0.6, 0);
		if (now % 8 == 0) {
			Vfx.emit(level, ParticleTypes.FALLING_LAVA, c, 1, 0.5, 0.0);
			Vfx.emit(level, ParticleTypes.SMALL_FLAME, c, 2, 0.5, 0.01);
		}
		// Left open by its own moves, and it shows: steaming, fanned, pulled taut in its chain, cold and brittle.
		if (now % 4 == 0) {
			if (Reactions.has(this, Reactions.Mark.WET)) {
				Motes.clouds(level, c.add(0, 0.6, 0), 2, 0.6, Motes.STEAM, 0.9, 30, new Vec3(0, 0.06, 0), 0.02, 0.4);
			}
			if (Reactions.has(this, Reactions.Mark.WINDSWEPT)) {
				ElementFx.embers(level, c, 0.9, 3);
				Vfx.emit(level, ParticleTypes.SMALL_GUST, c, 1, 0.6, 0.0);
			}
			if (Reactions.has(this, Reactions.Mark.FROZEN)) {
				Vfx.emit(level, ParticleTypes.SNOWFLAKE, c, 3, 0.7, 0.01);
				Motes.glows(level, c, 2, 0.7, 0xBFE8FF, 0.1, 24, new Vec3(0, -0.01, 0), 0.01);
			}
			if (Reactions.has(this, Reactions.Mark.PULLED)) {
				Motes.glows(level, c.add(0, -0.4, 0), 3, 0.8, 0xC8C4C0, 0.08, 20, Vec3.ZERO, 0.01);
			}
		}
		// Whoever stands still a while, it spews slag on.
		LivingEntity target = getTarget();
		if (now % 40 == 0 && target != null) {
			idle = lastSpot != null && target.position().distanceTo(lastSpot) < 2.0;
			lastSpot = target.position();
		}
		if (volleyAt > 0 && now >= volleyAt) {
			volleyAt = 0;
			volley(level, true);
		}
		waves(level);
		gobs(level, now);
		pools(level, now);
	}

	// ------------------------------------------------------------------ what it leaves about: fire walls, slag, burning pools

	/** A wall of fire rolling out along the floor from where its fists came down. */
	private static final class Wave {
		final Vec3 from;
		final double speed, reach;
		final Set<UUID> struck = new HashSet<>();
		double radius = 0.6;

		Wave(Vec3 from, double speed, double reach) {
			this.from = from;
			this.speed = speed;
			this.reach = reach;
		}
	}

	/** A gob of slag in the air from {@code from}, landing on {@code at} at {@code lands}, lobbed {@code arc} high. */
	private record Gob(Vec3 from, Vec3 at, long lands, double arc) {
	}

	/** Slag burning on the floor until {@code until}. */
	private record Pool(Vec3 at, long until) {
	}

	private void waves(ServerLevel level) {
		for (int w = waves.size() - 1; w >= 0; w--) {
			Wave wave = waves.get(w);
			double was = wave.radius;
			wave.radius = Math.min(wave.reach, wave.radius + wave.speed);
			int points = (int) Math.max(8, wave.radius * 3);
			for (int i = 0; i < points; i++) {
				double a = Math.PI * 2 * i / points + level.getRandom().nextDouble() * 0.3;
				Vec3 at = wave.from.add(Math.cos(a) * wave.radius, 0.15, Math.sin(a) * wave.radius);
				Vfx.emit(level, ParticleTypes.FLAME, at, 1, 0.1, 0.02);
				if (i % 3 == 0) {
					Vfx.emit(level, ParticleTypes.SMALL_FLAME, at.add(0, 0.35, 0), 1, 0.1, 0.01);
				}
			}
			for (LivingEntity t : foes(level, wave.from, wave.radius + 1.0)) {
				double d = Math.hypot(t.getX() - wave.from.x, t.getZ() - wave.from.z);
				if (crosses(d, was, wave.radius, t.getY() - wave.from.y) && wave.struck.add(t.getUUID())) {
					hurt(level, t, WAVE_DAMAGE, 2);
					throwFrom(t, wave.from, 0.5, 0.35);
				}
			}
			if (wave.radius >= wave.reach) {
				waves.remove(w);
			}
		}
	}

	private void gobs(ServerLevel level, long now) {
		for (int g = gobs.size() - 1; g >= 0; g--) {
			Gob gob = gobs.get(g);
			double k = 1 - (gob.lands() - now) / (double) SLAG_FALL;
			if (now < gob.lands()) {
				// Arcing over the hall (or dropping out of the dark above), trailing smoke.
				double kk = Math.max(0, Math.min(1, k));
				Vec3 at = gob.from().lerp(gob.at(), kk).add(0, gob.arc() * 4 * kk * (1 - kk), 0);
				Vfx.emit(level, ParticleTypes.LAVA, at, 1, 0.1, 0.0);
				Vfx.emit(level, ParticleTypes.FLAME, at, 2, 0.15, 0.01);
				if (now % 2 == 0) {
					Motes.smoke(level, at, 1, 0.1);
				}
				continue;
			}
			gobs.remove(g);
			Vec3 at = gob.at();
			for (LivingEntity t : foes(level, at, SLAG_RADIUS + 0.5)) {
				double d = Math.hypot(t.getX() - at.x, t.getZ() - at.z);
				double above = t.getY() - at.y;
				if (d <= SLAG_RADIUS + 0.3 && above > -1 && above < 2.5) {
					hurt(level, t, SLAG_DAMAGE, 3);
					throwFrom(t, at, 0.35, 0.3);
				}
			}
			ElementFx.fireImpact(level, at.add(0, 0.3, 0), 1.1);
			Vfx.emit(level, ParticleTypes.LAVA, at.add(0, 0.2, 0), 8, 0.6, 0.25);
			Fx.sound(level, at, SoundEvents.LAVA_POP, 1.2F, 0.6F);
			Fx.sound(level, at, SoundEvents.FIRECHARGE_USE, 0.6F, 0.7F);
			pools.add(new Pool(at, now + POOL_TICKS));
		}
	}

	private void pools(ServerLevel level, long now) {
		for (int p = pools.size() - 1; p >= 0; p--) {
			Pool pool = pools.get(p);
			if (now >= pool.until()) {
				Motes.smoke(level, pool.at().add(0, 0.2, 0), 3, 0.5);
				pools.remove(p);
				continue;
			}
			Vec3 at = pool.at();
			if (now % 3 == 0) {
				Vfx.emit(level, ParticleTypes.FLAME, at.add(0, 0.1, 0), 2, POOL_RADIUS * 0.5, 0.01);
				Motes.glows(level, at.add(0, 0.1, 0), 2, POOL_RADIUS * 0.6, COLOR, 0.12, 16, new Vec3(0, 0.02, 0), 0.01);
			}
			if (now % 12 == 0) {
				ElementFx.groundRing(level, at, COLOR, POOL_RADIUS - 0.2, POOL_RADIUS, 0.12, 12);
				Vfx.emit(level, ParticleTypes.LAVA, at.add(0, 0.1, 0), 1, 0.3, 0.1);
			}
			if ((pool.until() - now) % POOL_EVERY == 0) {
				for (LivingEntity t : foes(level, at, POOL_RADIUS + 0.5)) {
					if (Math.hypot(t.getX() - at.x, t.getZ() - at.z) <= POOL_RADIUS + 0.3 && Math.abs(t.getY() - at.y) < 1.0) {
						t.hurtServer(level, level.damageSources().hotFloor(), (float) (POOL_DAMAGE * Runebound.power(level)));
						t.igniteForSeconds(2);
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------ how it reckons where things are

	/** Those it may harm within {@code radius} of {@code at}. */
	private List<LivingEntity> foes(ServerLevel level, Vec3 at, double radius) {
		return level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(radius, 4, radius),
			e -> e != this && e.isAlive() && Targets.canHarm(this, e));
	}

	/** Harm {@code t} for {@code damage} (scaled by difficulty), setting them alight {@code burn} seconds. */
	private void hurt(ServerLevel level, LivingEntity t, double damage, int burn) {
		t.hurtServer(level, level.damageSources().mobAttack(this), (float) (damage * Runebound.power(level)));
		if (burn > 0) {
			t.igniteForSeconds(burn);
		}
	}

	/** Strikes {@code t} once in its move under way, at most. True if it did. */
	private boolean strike(ServerLevel level, LivingEntity t, double damage, int burn) {
		if (!struck.add(t.getUUID())) {
			return false;
		}
		hurt(level, t, damage, burn);
		return true;
	}

	/** Throws {@code t} away from {@code from}, {@code out} along the floor and {@code up}. */
	private static void throwFrom(LivingEntity t, Vec3 from, double out, double up) {
		Vec3 away = new Vec3(t.getX() - from.x, 0, t.getZ() - from.z);
		away = away.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : away.normalize();
		Effects.push(t, new Vec3(away.x * out, up, away.z * out));
	}

	/** The way {@code yaw} degrees faces, flat. */
	private static Vec3 ahead(double yaw) {
		double r = Math.toRadians(yaw);
		return new Vec3(-Math.sin(r), 0, Math.cos(r));
	}

	/** The yaw from its feet to {@code at}. */
	private float yawTo(Vec3 at) {
		return (float) Math.toDegrees(Math.atan2(-(at.x - getX()), at.z - getZ()));
	}

	private void face(float yaw) {
		setYRot(yaw);
		yBodyRot = yaw;
		yHeadRot = yaw;
	}

	/** Turns toward {@code yaw} by at most {@code most} degrees. */
	private void turn(float yaw, float most) {
		face(getYRot() + Mth.clamp(Mth.wrapDegrees(yaw - getYRot()), -most, most));
	}

	/** How far {@code at} lies along {@code yaw} from {@code from}, and how far across. */
	private static double[] local(Vec3 from, double yaw, Vec3 at) {
		Vec3 f = ahead(yaw);
		double dx = at.x - from.x, dz = at.z - from.z;
		return new double[] {dx * f.x + dz * f.z, dx * -f.z + dz * f.x};
	}

	/** Whether stone stands between {@code from} and {@code t}'s eyes. */
	private boolean sheltered(ServerLevel level, Vec3 from, LivingEntity t) {
		return level.clip(new ClipContext(from, t.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType()
			!= HitResult.Type.MISS;
	}

	/** How far along {@code yaw} from its feet the floor runs clear before stone, out to {@code most}. */
	private double clear(ServerLevel level, double yaw, double most) {
		Vec3 from = position().add(0, 0.6, 0);
		HitResult hit = level.clip(new ClipContext(from, from.add(ahead(yaw).scale(most)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		return hit.getType() == HitResult.Type.MISS ? most : Math.max(1.5, hit.getLocation().distanceTo(from) - 0.3);
	}

	private void setReach(double blocks) {
		entityData.set(DATA_REACH, (float) blocks);
	}

	// ------------------------------------------------------------------ its moves: what it wants, and how each begins

	@Override
	protected int ownMoves() {
		return PICKS.length;
	}

	@Override
	protected void weigh(ServerLevel level, LivingEntity target, double[] weights) {
		double d = Math.hypot(target.getX() - getX(), target.getZ() - getZ());
		double angle = wrap(yawTo(target.position()) - getYRot());
		int crowd = 0;
		for (LivingEntity e : foes(level, position(), CROWD)) {
			if (e.distanceTo(this) <= CROWD) {
				crowd++;
			}
		}
		double[] w = weights(phase, d, angle, target.getY() - getY(), crowd, idle, level.getGameTime() - lastFlare);
		for (int m : PICKS) {
			weights[m] = w[m];
		}
	}

	@Override
	protected double spellWeight(ServerLevel level, LivingEntity target, int index) {
		return CinderWardenRules.spellWeight(taught, distanceTo(target));
	}

	@Override
	protected void beginMove(ServerLevel level, int move, LivingEntity target) {
		getNavigation().stop();
		start(level, move, target, false);
	}

	/** Into {@code move} at {@code target}, run on from another if {@code chained}: its tell, its length, and the first sign of it. */
	private void start(ServerLevel level, int move, LivingEntity target, boolean chained) {
		struck.clear();
		scorched.clear();
		hook = null;
		caught = null;
		cast = 0;
		aim = target != null ? yawTo(target.position()) : getYRot();
		spin = getYRot();
		int tell = tell(move, chained);
		int length = switch (move) {
			case HAMMERFALL -> tell + 3;
			case SWEEP -> tell + SWEEP_SPIN + (phase >= 2 ? SWEEP_RETELL + SWEEP_SPIN : 0) + SWEEP_RECOVER;
			case SEAM -> tell + SEAM_RUN + SEAM_RECOVER;
			case VENT -> tell + VENT_BLAST + VENT_RECOVER;
			case HAUL -> tell + HAUL_CAST + HAUL_PULL_MOST + 2;
			case SLAG -> tell + 4 + SLAG_RECOVER;
			case BELLOWS -> tell + BREATH_TICKS + BELLOWS_RECOVER;
			case FLARE -> tell + FLARE_GATHER + 2;
			default -> 20;
		};
		pose(move, tell, length);
		Vec3 feet = position();
		Vec3 chest = feet.add(0, getBbHeight() * 0.6, 0);
		switch (move) {
			case HAMMERFALL -> {
				// Both fists clasped and raised, fire running up its chains.
				setState(SLAMMING, true);
				Fx.sound(level, feet, SoundEvents.RAVAGER_ROAR, 0.7F, 1.3F);
				Fx.sound(level, feet, SoundEvents.CHAIN_PLACE, 1.0F, 0.6F);
			}
			case SWEEP -> {
				spinWay = -spinWay;
				entityData.set(DATA_SPIN, (float) spinWay);
				Fx.sound(level, feet, SoundEvents.CHAIN_PLACE, 1.2F, 0.5F);
				Fx.sound(level, feet, SoundEvents.RAVAGER_STEP, 1.0F, 0.6F);
			}
			case SEAM -> {
				Fx.sound(level, feet, SoundEvents.ANVIL_PLACE, 0.6F, 0.5F);
				Fx.sound(level, feet, SoundEvents.BLAZE_BURN, 0.8F, 0.6F);
			}
			case VENT -> {
				// It goes still and its back plates lift, hissing.
				aim = getYRot();
				Fx.sound(level, chest, SoundEvents.LAVA_EXTINGUISH, 1.0F, 0.5F);
				Fx.sound(level, chest, SoundEvents.FIRE_EXTINGUISH, 0.8F, 0.6F);
			}
			case HAUL -> Fx.sound(level, chest, SoundEvents.CHAIN_PLACE, 1.0F, 0.9F);
			case SLAG -> {
				Fx.sound(level, chest, SoundEvents.LAVA_POP, 1.2F, 0.5F);
				Fx.sound(level, chest, SoundEvents.HOGLIN_ANGRY, 0.7F, 0.5F);
			}
			case BELLOWS -> Fx.sound(level, chest, SoundEvents.BREEZE_INHALE, 1.4F, 0.5F);
			case FLARE -> {
				lastFlare = level.getGameTime();
				aim = getYRot();
				Fx.sound(level, chest, SoundEvents.WARDEN_ROAR, 1.4F, 0.8F);
				Fx.sound(level, chest, SoundEvents.BLAZE_AMBIENT, 1.2F, 0.5F);
				ElementFx.groundRing(level, feet.add(0, 0.05, 0), ACCENT, FLARE_RADIUS - 0.3, FLARE_RADIUS, 0.3, tell + FLARE_GATHER);
			}
			default -> {
			}
		}
	}

	/**
	 * Through its tell it turns after its prey, then holds its aim {@code lock} ticks before the blow, and holds it: the last moment
	 * to step out of what it has marked.
	 */
	private void track(LivingEntity target, int t, int lock) {
		if (target != null && t < moveTell - lock) {
			aim = yawTo(target.position());
			turn(aim, TURN);
		} else {
			face(aim);
		}
	}

	/** Whether it has just locked its aim this tick. */
	private boolean locks(int t, int lock) {
		return t == moveTell - lock;
	}

	@Override
	protected void moveTick(ServerLevel level, int move, int t, LivingEntity target) {
		switch (move) {
			case HAMMERFALL -> hammerfall(level, t, target);
			case SWEEP -> sweep(level, t);
			case SEAM -> seam(level, t, target);
			case VENT -> vent(level, t);
			case HAUL -> haul(level, t, target);
			case SLAG -> slag(level, t);
			case BELLOWS -> bellows(level, t, target);
			case FLARE -> flare(level, t);
			case STUCK -> stuck(level, t);
			case TANGLED -> tangled(level, t);
			case SPENT -> spent(level, t);
			case REEL -> reel(level, t);
			default -> {
			}
		}
	}

	// ------------------------------------------------------------------ the hammerfall: both fists down on a ring before it

	private void hammerfall(ServerLevel level, int t, LivingEntity target) {
		track(target, t, HAMMER_LOCK);
		Vec3 feet = position();
		Vec3 centre = feet.add(ahead(aim).scale(HAMMER_AHEAD));
		if (t < moveTell) {
			Vec3 fists = feet.add(0, getBbHeight() + 0.6, 0);
			Vfx.emit(level, ParticleTypes.FLAME, fists, 3, 0.3, 0.02);
			if (t % 3 == 0) {
				ElementFx.embers(level, fists, 0.4, 2);
			}
			if (t < moveTell - HAMMER_LOCK && t % 4 == 0) {
				ElementFx.groundRing(level, centre.add(0, 0.05, 0), COLOR, HAMMER_RADIUS - 0.3, HAMMER_RADIUS, 0.12, 5);
			}
			if (locks(t, HAMMER_LOCK)) {
				Sigils.ground(level, centre.add(0, 0.06, 0), COLOR, ACCENT, (float) HAMMER_RADIUS, HAMMER_LOCK + 6);
				ElementFx.groundRing(level, centre.add(0, 0.05, 0), ACCENT, HAMMER_RADIUS - 0.2, HAMMER_RADIUS, 0.2, HAMMER_LOCK + 2);
				Fx.sound(level, feet, SoundEvents.CHAIN_HIT, 1.0F, 0.6F);
			}
			return;
		}
		if (t == moveTell) {
			setState(SLAMMING, false);
			for (LivingEntity e : foes(level, centre, HAMMER_RADIUS + 1)) {
				double[] at = local(feet, aim, e.position());
				if (hammerHits(at[0], at[1], e.getY() - feet.y) && strike(level, e, HAMMER_DAMAGE, 3)) {
					throwFrom(e, centre, 1.1, 0.45);
				}
			}
			ElementFx.flameBurst(level, centre.add(0, 0.4, 0), HAMMER_RADIUS, 18);
			ElementFx.groundRing(level, centre.add(0, 0.05, 0), COLOR, 0.5, HAMMER_RADIUS + 0.8, 0.25, 10);
			ElementFx.crack(level, centre, HAMMER_RADIUS, 40);
			Vfx.emit(level, ParticleTypes.LAVA, centre.add(0, 0.3, 0), 16, 1.5, 0.3);
			Fx.sound(level, centre, DungeonSounds.WARDEN_SLAM, 1.6F, 1.0F);
			Fx.sound(level, centre, SoundEvents.MACE_SMASH_GROUND_HEAVY, 1.2F, 0.7F);
			// From the second phase the blow sends a wall of fire rolling out along the floor: jump it.
			if (phase >= 2) {
				Wave wave = new Wave(centre, waveSpeed(phase), waveReach(phase));
				wave.struck.addAll(struck);
				waves.add(wave);
				ElementFx.groundRing(level, centre.add(0, 0.05, 0), COLOR, 0.6, waveReach(phase), 0.35,
					(int) Math.ceil(waveReach(phase) / waveSpeed(phase)));
				Fx.sound(level, centre, SoundEvents.BLAZE_SHOOT, 1.4F, 0.5F);
			}
			return;
		}
		Motes.smoke(level, centre.add(0, 0.3, 0), 2, 0.8);
	}

	// ------------------------------------------------------------------ the sweep: its chains let out and dragged round in a ring

	private void sweep(ServerLevel level, int t) {
		double outer = sweepReach(phase);
		int first = moveTell + SWEEP_SPIN;
		int second = phase >= 2 ? first + SWEEP_RETELL : -1;
		int end = second > 0 ? second + SWEEP_SPIN : first;
		Vec3 feet = position();
		if (t < moveTell) {
			// Letting its chains out, scraping, until they lie the length of the ring.
			face(aim);
			setReach(outer * (t + 1) / moveTell);
			band(level, outer, t);
			return;
		}
		boolean turning = t < first || second > 0 && t >= second && t < end;
		if (turning) {
			if (t == moveTell) {
				spin = aim;
			}
			if (t == moveTell || t == second) {
				Fx.sound(level, feet, SoundEvents.RAVAGER_ATTACK, 1.2F, 0.6F);
				Fx.sound(level, feet, SoundEvents.CHAIN_BREAK, 0.8F, 0.6F);
			}
			float was = spin;
			spin += 36 * spinWay;
			face(spin);
			Vec3 dir = ahead(spin);
			ElementFx.ray(level, feet.add(dir.scale(SWEEP_INNER)).add(0, 0.4, 0), feet.add(dir.scale(outer)).add(0, 0.4, 0), 0x8A8480, 0.12, 3);
			Vec3 tip = feet.add(dir.scale(outer)).add(0, 0.2, 0);
			Vfx.emit(level, ParticleTypes.FLAME, tip, 3, 0.2, 0.03);
			ElementFx.sparks(level, tip, 3, 0.25);
			if (t % 2 == 0) {
				Fx.sound(level, tip, SoundEvents.CHAIN_HIT, 0.9F, 0.7F + level.getRandom().nextFloat() * 0.3F);
			}
			for (LivingEntity e : foes(level, feet, outer + 1)) {
				double d = Math.hypot(e.getX() - feet.x, e.getZ() - feet.z);
				double above = e.getY() - feet.y;
				if (inRing(d, SWEEP_INNER, outer + 0.3) && above < SWEEP_HEIGHT && above > -1 && swept(yawTo(e.position()), was, spin)
						&& strike(level, e, SWEEP_DAMAGE, 2)) {
					throwFrom(e, feet, 0.9, 0.35);
				}
			}
			return;
		}
		if (t < end) {
			// Between turns: the chains drag to a stop and it turns them back the other way.
			if (t == first) {
				struck.clear();
				spinWay = -spinWay;
				entityData.set(DATA_SPIN, (float) spinWay);
				aim = spin;
			}
			face(spin);
			band(level, outer, t - first);
			return;
		}
		setReach(outer * Math.max(0, 1 - (t - end + 1) / (double) SWEEP_RECOVER));
		face(spin);
		ElementFx.sparks(level, feet.add(ahead(spin + 90).scale(Math.max(1, entityData.get(DATA_REACH)))).add(0, 0.1, 0), 1, 0.1);
	}

	/** The ring its chains will scrape, marked on the floor out to their reach and in to its feet, the chains rasping as they're let out. */
	private void band(ServerLevel level, double outer, int t) {
		Vec3 feet = position().add(0, 0.05, 0);
		if (t % 4 == 0) {
			ElementFx.groundRing(level, feet, ACCENT, outer - 0.25, outer, 0.18, 5);
			ElementFx.groundRing(level, feet, COLOR, SWEEP_INNER - 0.1, SWEEP_INNER + 0.1, 0.12, 5);
		}
		double out = Math.max(1, entityData.get(DATA_REACH));
		for (int side = -1; side <= 1; side += 2) {
			ElementFx.sparks(level, position().add(ahead(aim + 90 * side).scale(out)).add(0, 0.1, 0), 2, 0.15);
		}
		if (t % 3 == 0) {
			Fx.sound(level, position(), SoundEvents.CHAIN_HIT, 0.6F, 0.5F + level.getRandom().nextFloat() * 0.2F);
		}
	}

	// ------------------------------------------------------------------ the seam: a gauntlet dragged through the floor, bursting along its length

	private void seam(ServerLevel level, int t, LivingEntity target) {
		track(target, t, SEAM_LOCK);
		Vec3 feet = position();
		int tell = moveTell;
		if (t < tell) {
			Vec3 hand = feet.add(ahead(aim + 40).scale(1.2)).add(0, 0.2, 0);
			ElementFx.sparks(level, hand, 3, 0.2);
			Vfx.emit(level, ParticleTypes.LAVA, hand, 1, 0.2, 0.1);
			if (t % 4 == 0) {
				Fx.sound(level, hand, SoundEvents.BASALT_PLACE, 0.8F, 0.5F);
			}
			if (t < tell - SEAM_LOCK && t % 4 == 0) {
				lane(level, aim, SEAM_FROM, clear(level, aim, SEAM_TO), SEAM_HALF, COLOR, 4);
			}
			if (locks(t, SEAM_LOCK)) {
				double[] offs = seams(phase);
				seamYaw = new double[offs.length];
				seamTo = new double[offs.length];
				for (int i = 0; i < offs.length; i++) {
					seamYaw[i] = aim + offs[i];
					seamTo[i] = clear(level, seamYaw[i], SEAM_TO);
					lane(level, seamYaw[i], SEAM_FROM, seamTo[i], SEAM_HALF, ACCENT, SEAM_LOCK + SEAM_RUN + 2);
				}
				Fx.sound(level, hand, SoundEvents.ANVIL_LAND, 0.7F, 0.5F);
			}
			return;
		}
		int k = t - tell;
		if (k < SEAM_RUN) {
			double from = SEAM_FROM + k * SEAM_SPEED;
			for (int i = 0; i < seamYaw.length; i++) {
				double to = Math.min(SEAM_FROM + (k + 1) * SEAM_SPEED, seamTo[i]);
				if (from > seamTo[i]) {
					continue;
				}
				Vec3 dir = ahead(seamYaw[i]);
				for (double s = from; s <= to; s += 1.0) {
					Vec3 spot = feet.add(dir.scale(s));
					ElementFx.flames(level, spot, 0.5, SEAM_HEIGHT, 5);
					Vfx.emit(level, ParticleTypes.LAVA, spot.add(0, 0.2, 0), 1, 0.3, 0.15);
				}
				Vec3 front = feet.add(dir.scale(to));
				Fx.sound(level, front, SoundEvents.BLAZE_SHOOT, 0.8F, 0.6F + k * 0.05F);
				for (LivingEntity e : foes(level, feet, SEAM_TO + 2)) {
					double[] at = local(feet, seamYaw[i], e.position());
					double above = e.getY() - feet.y;
					if (inLane(at[0], at[1], from - 0.6, to + 0.6, SEAM_HALF) && above > -1 && above < SEAM_HEIGHT
							&& strike(level, e, SEAM_DAMAGE, 3)) {
						Effects.push(e, new Vec3(0, 0.65, 0));
					}
				}
			}
			return;
		}
		// The seams smoulder; its gauntlet cools in the floor.
		if (t % 3 == 0) {
			for (int i = 0; i < seamYaw.length; i++) {
				Motes.smoke(level, feet.add(ahead(seamYaw[i]).scale(SEAM_FROM + level.getRandom().nextDouble() * (seamTo[i] - SEAM_FROM))), 1, 0.3);
			}
		}
	}

	// ------------------------------------------------------------------ the vent: steam blasted out of its back

	private void vent(ServerLevel level, int t) {
		face(aim);
		Vec3 feet = position();
		Vec3 back = feet.add(ahead(aim + 180).scale(0.8)).add(0, getBbHeight() * 0.7, 0);
		double reach = ventReach(phase);
		double half = phase >= 2 ? VENT_HALF_LATER : VENT_HALF;
		int tell = moveTell;
		if (t < tell) {
			Motes.clouds(level, back, 1, 0.3, Motes.STEAM, 0.6, 16, new Vec3(0, 0.05, 0), 0.02, 0.4);
			if (t % 4 == 0) {
				cone(level, aim + 180, reach, half, COLOR, 5);
				Fx.sound(level, back, SoundEvents.LAVA_EXTINGUISH, 0.6F, 0.8F + t * 0.04F);
			}
			return;
		}
		if (t < tell + VENT_BLAST) {
			if (t == tell) {
				Fx.sound(level, back, SoundEvents.FIRE_EXTINGUISH, 1.6F, 0.5F);
				Fx.sound(level, back, SoundEvents.BREEZE_WIND_CHARGE_BURST, 1.0F, 0.6F);
			}
			for (int i = 0; i < 6; i++) {
				Vec3 dir = ahead(aim + 180 + (level.getRandom().nextDouble() * 2 - 1) * half);
				Vec3 at = feet.add(dir.scale(1 + level.getRandom().nextDouble() * reach * 0.7)).add(0, 0.8, 0);
				Motes.clouds(level, at, 1, 0.4, Motes.STEAM, 1.2, 20, dir.scale(0.25).add(0, 0.03, 0), 0.03, 0.5);
			}
			for (LivingEntity e : foes(level, feet, reach + 1)) {
				double[] at = local(feet, aim, e.position());
				double above = e.getY() - feet.y;
				if (vented(at[0], at[1], phase) && above > -1 && above < 3 && strike(level, e, VENT_DAMAGE, 0)) {
					throwFrom(e, feet, 1.0, 0.3);
				}
			}
			if (t == tell + VENT_BLAST - 1) {
				// Its steam spent, it stands wet with it: storm conducts.
				Reactions.mark(this, Reactions.Mark.WET, phase >= 2 ? STEAM_TICKS_LATER : STEAM_TICKS);
			}
			return;
		}
		if (t % 3 == 0) {
			Motes.clouds(level, back, 1, 0.4, Motes.STEAM, 0.8, 24, new Vec3(0, 0.06, 0), 0.02, 0.4);
		}
	}

	/** A cone on the floor from its feet, {@code yaw} on, {@code reach} long and {@code half} degrees each way: its edges and its rim. */
	private void cone(ServerLevel level, double yaw, double reach, double half, int color, int lifetime) {
		Vec3 feet = position().add(0, 0.1, 0);
		for (int side = -1; side <= 1; side += 2) {
			ElementFx.ray(level, feet, feet.add(ahead(yaw + half * side).scale(reach)), color, 0.12, lifetime);
		}
		int steps = (int) Math.max(3, half / 12);
		Vec3 last = feet.add(ahead(yaw - half).scale(reach));
		for (int i = 1; i <= steps * 2; i++) {
			Vec3 next = feet.add(ahead(yaw - half + half * i / steps).scale(reach));
			ElementFx.ray(level, last, next, color, 0.1, lifetime);
			last = next;
		}
	}

	/** A lane on the floor from its feet, {@code yaw} on, from {@code from} to {@code to} out and {@code half} wide each way. */
	private void lane(ServerLevel level, double yaw, double from, double to, double half, int color, int lifetime) {
		Vec3 feet = position().add(0, 0.1, 0);
		Vec3 f = ahead(yaw);
		Vec3 s = ahead(yaw + 90).scale(half);
		Vec3 a = feet.add(f.scale(from));
		Vec3 b = feet.add(f.scale(to));
		ElementFx.ray(level, a.add(s), b.add(s), color, 0.12, lifetime);
		ElementFx.ray(level, a.subtract(s), b.subtract(s), color, 0.12, lifetime);
		ElementFx.ray(level, b.add(s), b.subtract(s), color, 0.12, lifetime);
	}

	// ------------------------------------------------------------------ the haul: a wrist chain cast to drag its prey in, or itself after them

	private void haul(ServerLevel level, int t, LivingEntity target) {
		track(target, t, HAUL_LOCK);
		Vec3 feet = position();
		Vec3 hand = feet.add(ahead(aim - 35).scale(0.9)).add(0, getBbHeight() * 0.75, 0);
		int tell = moveTell;
		if (t < tell) {
			// The chain whirled overhead, faster and faster.
			setReach(1.5);
			ElementFx.swirl(level, hand, 0.9, 0.2, 1, 0x8A8480, COLOR);
			if (t % 3 == 0) {
				Fx.sound(level, hand, SoundEvents.CHAIN_PLACE, 0.8F, 0.6F + t * 0.05F);
			}
			if (t < tell - HAUL_LOCK && t % 4 == 0) {
				lane(level, aim, HAUL_FROM, clear(level, aim, HAUL_REACH), HAUL_HALF, COLOR, 4);
			}
			if (locks(t, HAUL_LOCK)) {
				haulTo = clear(level, aim, HAUL_REACH);
				lane(level, aim, HAUL_FROM, haulTo, HAUL_HALF, ACCENT, HAUL_LOCK + HAUL_CAST + 2);
				Fx.sound(level, hand, SoundEvents.CHAIN_HIT, 1.0F, 0.5F);
			}
			return;
		}
		int k = t - tell;
		if (k < HAUL_CAST) {
			// Thrown out down its lane; the first it finds there, it bites.
			if (k == 0) {
				Fx.sound(level, hand, SoundEvents.TRIDENT_THROW, 1.2F, 0.5F);
			}
			double was = cast;
			cast = caught != null ? cast : HAUL_FROM + (haulTo - HAUL_FROM) * (k + 1) / HAUL_CAST;
			setReach(cast);
			Vec3 tip = feet.add(ahead(aim).scale(cast)).add(0, 1.0, 0);
			ElementFx.ray(level, hand, tip, 0x8A8480, 0.12, 3);
			ElementFx.sparks(level, tip, 2, 0.15);
			if (caught == null) {
				LivingEntity first = null;
				double nearest = Double.MAX_VALUE;
				for (LivingEntity e : foes(level, feet, HAUL_REACH + 1)) {
					double[] at = local(feet, aim, e.position());
					double above = e.getY() - feet.y;
					if (inLane(at[0], at[1], HAUL_FROM, cast + 0.5, HAUL_HALF) && above > -1 && above < HAUL_HEIGHT && at[0] < nearest) {
						first = e;
						nearest = at[0];
					}
				}
				if (first != null) {
					caught = first;
					cast = nearest;
					Vec3 in = new Vec3(feet.x - first.getX(), 0, feet.z - first.getZ());
					in = in.lengthSqr() < 1.0E-4 ? Vec3.ZERO : in.normalize();
					Effects.push(first, in.scale(tug(nearest)).add(0, HAUL_LIFT, 0));
					ElementFx.sparks(level, first.position().add(0, 1, 0), 8, 0.3);
					Fx.sound(level, first.position(), SoundEvents.CHAIN_HIT, 1.4F, 0.6F);
					Fx.sound(level, feet, SoundEvents.RAVAGER_ATTACK, 1.0F, 0.7F);
				}
			}
			if (caught == null && k == HAUL_CAST - 1) {
				// It bit nothing living: it sinks the hook in the floor, no further from its arena's heart than it may go.
				Vec3 heart = Vec3.atBottomCenterOf(home);
				double reach = haulTo;
				Vec3 spot = feet.add(ahead(aim).scale(reach));
				while (reach > HAUL_FROM && Math.hypot(spot.x - heart.x, spot.z - heart.z) > HAUL_LEASH) {
					reach -= 0.5;
					spot = feet.add(ahead(aim).scale(reach));
				}
				hook = CastEngine.ground(level, spot.add(0, 1, 0));
				ElementFx.sparks(level, hook.add(0, 0.2, 0), 10, 0.3);
				ElementFx.crack(level, hook, 1.0, 30);
				Fx.sound(level, hook, SoundEvents.ANVIL_LAND, 0.8F, 0.6F);
			}
			return;
		}
		if (caught != null) {
			// Hauled in to its feet, and struck as they arrive.
			double d = caught.isAlive() ? caught.distanceTo(this) : 0;
			setReach(Math.max(0, Math.min(cast, d)));
			if (caught.isAlive() && !struck.contains(caught.getUUID())) {
				ElementFx.ray(level, hand, caught.position().add(0, 1.0, 0), 0x8A8480, 0.12, 2);
				double[] at = local(feet, aim, caught.position());
				if ((at[0] > -0.5 && Math.hypot(at[0], at[1]) <= HAUL_STRIKE || k >= HAUL_CAST + HAUL_DRAG) && strike(level, caught, HAUL_DAMAGE, 2)) {
					throwFrom(caught, feet, 0.8, 0.4);
					ElementFx.fireImpact(level, caught.position().add(0, 1, 0), 0.8);
					Fx.sound(level, feet, DungeonSounds.WARDEN_SLAM, 1.0F, 1.3F);
					setReach(0);
				}
			} else {
				setReach(0);
			}
			return;
		}
		if (hook == null) {
			setReach(0);
			return;
		}
		// It hauls itself after its hook, bowling over whoever stands in the way.
		Vec3 to = new Vec3(hook.x - getX(), 0, hook.z - getZ());
		double left = to.length();
		setReach(left);
		if (left <= 1.2) {
			goTangled(level);
			return;
		}
		Vec3 dir = to.scale(1 / left);
		setDeltaMovement(dir.x * HAUL_PULL, getDeltaMovement().y, dir.z * HAUL_PULL);
		face(aim);
		ElementFx.ray(level, hand, hook.add(0, 0.2, 0), 0x8A8480, 0.12, 2);
		ElementFx.sparks(level, feet.add(0, 0.1, 0), 3, 0.2);
		if (k % 3 == 0) {
			Fx.sound(level, feet, SoundEvents.RAVAGER_STEP, 1.0F, 0.6F);
		}
		for (LivingEntity e : foes(level, feet, HAUL_BOWL + 1)) {
			if (e.distanceTo(this) <= HAUL_BOWL + getBbWidth() / 2 && strike(level, e, HAUL_BOWL_DAMAGE, 2)) {
				throwFrom(e, feet, 0.9, 0.4);
				Fx.sound(level, e.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.6F);
			}
		}
	}

	/** Its haul missed, and it's caught up in its own chain, pulled taut: blast or repel it. */
	private void goTangled(ServerLevel level) {
		setDeltaMovement(Vec3.ZERO);
		setReach(0);
		hook = null;
		pose(TANGLED, 0, TANGLED_TICKS);
		Reactions.mark(this, Reactions.Mark.PULLED, PULLED_TICKS);
		Vec3 c = position().add(0, getBbHeight() * 0.5, 0);
		ElementFx.sparks(level, c, 12, 0.3);
		Fx.sound(level, c, SoundEvents.CHAIN_BREAK, 1.2F, 0.5F);
		Fx.sound(level, c, SoundEvents.RAVAGER_STUNNED, 0.8F, 0.7F);
	}

	// ------------------------------------------------------------------ the slag: molten gobs spewed over the hall onto where everyone stands

	private void slag(ServerLevel level, int t) {
		face(aim);
		Vec3 mouth = mouth();
		if (t < moveTell) {
			Vfx.emit(level, ParticleTypes.LAVA, mouth, 1, 0.2, 0.05);
			Vfx.emit(level, ParticleTypes.FALLING_LAVA, mouth, 1, 0.2, 0.0);
			if (t % 3 == 0) {
				Fx.sound(level, mouth, SoundEvents.LAVA_POP, 1.0F, 0.6F + t * 0.03F);
			}
			return;
		}
		if (t == moveTell) {
			volley(level, false);
			Vfx.emit(level, ParticleTypes.LAVA, mouth, 14, 0.4, 0.4);
			ElementFx.flameBurst(level, mouth, 0.8, 10);
			Fx.sound(level, mouth, SoundEvents.BLAZE_SHOOT, 1.4F, 0.5F);
			Fx.sound(level, mouth, SoundEvents.FIRECHARGE_USE, 1.0F, 0.6F);
			// In its last phase a second lot it flung high comes down out of the dark after the first.
			if (phase >= 3) {
				volleyAt = level.getGameTime() + SLAG_FALL;
			}
			return;
		}
		Motes.smoke(level, mouth, 1, 0.2);
	}

	/** Gobs of slag onto everyone about it and round them, marked where they'll land; {@code second}: dropping from overhead. */
	private void volley(ServerLevel level, boolean second) {
		long now = level.getGameTime();
		Vec3 mouth = mouth();
		int count = 0;
		for (LivingEntity e : foes(level, position(), SLAG_RANGE)) {
			if (count++ >= 4) {
				break;
			}
			List<Vec3> spots = new ArrayList<>();
			spots.add(CastEngine.ground(level, e.position().add(0, 0.5, 0)));
			for (int i = 0; i < SLAG_SCATTER; i++) {
				double a = level.getRandom().nextDouble() * Math.PI * 2;
				double r = 1.5 + level.getRandom().nextDouble() * (SLAG_SPREAD - 1.5);
				spots.add(CastEngine.ground(level, e.position().add(Math.cos(a) * r, 1.0, Math.sin(a) * r)));
			}
			for (Vec3 spot : spots) {
				Sigils.target(level, spot.add(0, 0.05, 0), COLOR, (float) SLAG_RADIUS, SLAG_FALL + 2);
				gobs.add(second ? new Gob(spot.add(0, 12, 0), spot, now + SLAG_FALL, 0) : new Gob(mouth, spot, now + SLAG_FALL, 7));
			}
		}
		if (second) {
			Fx.sound(level, position(), SoundEvents.LAVA_AMBIENT, 1.4F, 0.5F);
		}
	}

	private Vec3 mouth() {
		return position().add(ahead(getYRot()).scale(0.6)).add(0, getBbHeight() * 0.85, 0);
	}

	// ------------------------------------------------------------------ the bellows: a long breath of fire in a slowly turning cone

	private void bellows(ServerLevel level, int t, LivingEntity target) {
		Vec3 feet = position();
		Vec3 mouth = mouth();
		int tell = moveTell;
		if (t < tell) {
			track(target, t, BREATH_LOCK);
			// Its chest swells, drawing the hall's heat in at its mouth.
			for (int i = 0; i < 3; i++) {
				double a = level.getRandom().nextDouble() * Math.PI * 2;
				Vec3 from = mouth.add(Math.cos(a) * 2.2, level.getRandom().nextDouble() * 1.4 - 0.7, Math.sin(a) * 2.2);
				Motes.glows(level, from, 1, 0.1, COLOR, 0.1, 10, mouth.subtract(from).scale(0.1), 0.0);
			}
			if (t == tell / 2) {
				Fx.sound(level, mouth, SoundEvents.BREEZE_INHALE, 1.2F, 0.7F);
			}
			if (t < tell - BREATH_LOCK && t % 4 == 0) {
				cone(level, aim, BREATH_REACH, BREATH_HALF, COLOR, 4);
			}
			if (locks(t, BREATH_LOCK)) {
				cone(level, aim, BREATH_REACH, BREATH_HALF, ACCENT, BREATH_LOCK + 2);
				Fx.sound(level, mouth, SoundEvents.BLAZE_AMBIENT, 1.2F, 0.5F);
			}
			return;
		}
		int k = t - tell;
		if (k < BREATH_TICKS) {
			// It breathes, turning after its prey, but slowly: run round it, or put stone between you.
			if (target != null && target.isAlive()) {
				turn(yawTo(target.position()), (float) breathTurn(phase));
			}
			aim = getYRot();
			for (int i = 0; i < 5; i++) {
				Vec3 dir = ahead(aim + (level.getRandom().nextDouble() * 2 - 1) * BREATH_HALF).add(0, -0.12, 0);
				Vfx.fling(level, ParticleTypes.FLAME, mouth, dir, 0.45 + level.getRandom().nextDouble() * 0.35);
			}
			if (k % 2 == 0) {
				Vfx.fling(level, ParticleTypes.LARGE_SMOKE, mouth, ahead(aim).add(0, 0.1, 0), 0.3);
			}
			if (k % 6 == 0) {
				Fx.sound(level, mouth, SoundEvents.BLAZE_SHOOT, 1.0F, 0.5F + level.getRandom().nextFloat() * 0.2F);
			}
			if (k % 4 == 0) {
				cone(level, aim, BREATH_REACH, BREATH_HALF, COLOR, 4);
			}
			long now = level.getGameTime();
			for (LivingEntity e : foes(level, feet, BREATH_REACH + 1)) {
				double[] at = local(feet, aim, e.position());
				double above = e.getY() - feet.y;
				if (inCone(at[0], at[1], BREATH_REACH, BREATH_HALF) && above > -1 && above < 3 && !sheltered(level, mouth, e)
						&& now - scorched.getOrDefault(e.getUUID(), -BREATH_EVERY * 2L) >= BREATH_EVERY) {
					scorched.put(e.getUUID(), now);
					hurt(level, e, BREATH_DAMAGE, 3);
				}
			}
			if (k == BREATH_TICKS - 1) {
				// Out of breath, its plates hang fanned open: burn it.
				Reactions.mark(this, Reactions.Mark.WINDSWEPT, FANNED_TICKS);
				Fx.sound(level, mouth, SoundEvents.BREEZE_DEFLECT, 1.0F, 0.5F);
			}
			return;
		}
		face(aim);
		Motes.smoke(level, mouth, 1, 0.15);
		if (k == BREATH_TICKS) {
			Fx.sound(level, mouth, SoundEvents.FIRE_EXTINGUISH, 0.8F, 0.6F);
		}
	}

	// ------------------------------------------------------------------ the flare: all its heat drawn in, and burst across the hall

	private void flare(ServerLevel level, int t) {
		face(aim);
		Vec3 feet = position();
		Vec3 chest = feet.add(0, getBbHeight() * 0.6, 0);
		int tell = moveTell;
		if (t < tell) {
			// Flames run in across the floor to it from all sides.
			for (int i = 0; i < 4; i++) {
				double a = level.getRandom().nextDouble() * Math.PI * 2;
				Vec3 from = feet.add(Math.cos(a) * 6, 0.2, Math.sin(a) * 6);
				Vfx.fling(level, ParticleTypes.FLAME, from, chest.subtract(from).normalize(), 0.35);
			}
			if (t % 5 == 0) {
				Fx.sound(level, chest, SoundEvents.BLAZE_BURN, 1.2F, 0.5F + t * 0.03F);
			}
			return;
		}
		int k = t - tell;
		if (k < FLARE_GATHER) {
			if (k == 0) {
				Fx.sound(level, chest, SoundEvents.WARDEN_SONIC_CHARGE, 2.0F, 0.6F);
				// The ring closing in counts down the burst; only stone between you and it shelters you.
				ElementFx.groundRing(level, feet.add(0, 0.05, 0), COLOR, FLARE_RADIUS, 1.0, 0.3, FLARE_GATHER);
			}
			if (k % 2 == 0) {
				ElementFx.heatFlare(level, chest, 0.5 + 2.5 * k / FLARE_GATHER);
			}
			ElementFx.embers(level, chest, 1.2, 3);
			if (k % 5 == 0) {
				Fx.sound(level, chest, SoundEvents.WARDEN_HEARTBEAT, 2.0F, 0.6F + k * 0.03F);
			}
			return;
		}
		if (k == FLARE_GATHER) {
			for (LivingEntity e : foes(level, feet, FLARE_RADIUS)) {
				double d = Math.hypot(e.getX() - feet.x, e.getZ() - feet.z);
				if (flared(d, sheltered(level, chest, e)) && strike(level, e, FLARE_DAMAGE, 4)) {
					throwFrom(e, feet, FLARE_PUSH, 0.4);
				}
			}
			ElementFx.flameBurst(level, chest, 6, 40);
			ElementFx.heatFlare(level, chest, 6);
			ElementFx.ring(level, chest, new Vec3(0, 1, 0), ACCENT, 1, FLARE_RADIUS, 0.6, 12);
			ElementFx.groundRing(level, feet.add(0, 0.05, 0), COLOR, 1, FLARE_RADIUS, 0.4, 10);
			Vfx.emit(level, ParticleTypes.LAVA, chest, 30, 2.0, 0.5);
			Fx.sound(level, chest, SoundEvents.GENERIC_EXPLODE, 2.0F, 0.6F);
			Fx.sound(level, chest, SoundEvents.WARDEN_SONIC_BOOM, 2.0F, 0.6F);
		}
	}

	// ------------------------------------------------------------------ left open: fists sunk, tangled, spent, reeling

	private void stuck(ServerLevel level, int t) {
		Vec3 fists = position().add(ahead(getYRot()).scale(HAMMER_AHEAD)).add(0, 0.2, 0);
		if (t == 0) {
			Fx.sound(level, fists, SoundEvents.RAVAGER_STUNNED, 1.0F, 0.6F);
		}
		if (t % 3 == 0) {
			Motes.smoke(level, fists, 1, 0.4);
			ElementFx.sparks(level, fists, 2, 0.15);
		}
		if (t == STUCK_TICKS - 8) {
			// It heaves its fists out of the floor.
			ElementFx.crack(level, fists, 1.5, 20);
			Vfx.emit(level, ParticleTypes.LAVA, fists, 6, 0.6, 0.2);
			Fx.sound(level, fists, SoundEvents.ANVIL_PLACE, 0.8F, 0.5F);
		}
	}

	private void tangled(ServerLevel level, int t) {
		Vec3 c = position().add(0, getBbHeight() * 0.5, 0);
		if (t % 2 == 0) {
			Motes.glows(level, c, 2, 0.9, 0xC8C4C0, 0.08, 14, Vec3.ZERO, 0.02);
		}
		if (t % 6 == 0) {
			Fx.sound(level, c, SoundEvents.CHAIN_HIT, 0.8F, 0.6F + level.getRandom().nextFloat() * 0.3F);
		}
		if (t == TANGLED_TICKS - 1) {
			ElementFx.sparks(level, c, 8, 0.3);
			Fx.sound(level, c, SoundEvents.CHAIN_BREAK, 1.0F, 0.7F);
		}
	}

	private void spent(ServerLevel level, int t) {
		Vec3 c = position().add(0, getBbHeight() * 0.6, 0);
		if (t == 0) {
			Fx.sound(level, c, SoundEvents.FIRE_EXTINGUISH, 1.4F, 0.5F);
			Motes.clouds(level, c, 6, 0.8, Motes.SMOKE, 1.4, 40, new Vec3(0, 0.05, 0), 0.03, 0.45);
		}
		if (t % 2 == 0) {
			Motes.smoke(level, c, 1, 0.6);
		}
		if (t % 10 == 0) {
			Fx.sound(level, c, SoundEvents.LAVA_EXTINGUISH, 0.6F, 0.6F);
		}
	}

	private void reel(ServerLevel level, int t) {
		Vec3 c = position().add(0, getBbHeight() * 0.6, 0);
		if (t == 0) {
			Vec3 back = ahead(getYRot() + 180);
			setDeltaMovement(back.x * 0.4, 0.1, back.z * 0.4);
			Fx.sound(level, c, SoundEvents.WARDEN_HURT, 1.4F, 0.6F);
			Fx.sound(level, c, SoundEvents.RAVAGER_STUNNED, 1.0F, 0.6F);
		}
		if (t % 3 == 0) {
			Vfx.emit(level, ParticleTypes.LAVA, c, 2, 0.5, 0.15);
			ElementFx.sparks(level, c, 3, 0.2);
		}
	}

	// ------------------------------------------------------------------ how its moves end

	@Override
	protected void moveDone(ServerLevel level, int move, LivingEntity target) {
		switch (move) {
			// Its fists stay sunk in the floor a moment: its back is bare.
			case HAMMERFALL -> pose(STUCK, 0, STUCK_TICKS);
			case FLARE -> {
				// Burnt out, it stands cold and brittle a while: burn it, or break it.
				pose(SPENT, 0, SPENT_TICKS);
				Reactions.mark(this, Reactions.Mark.FROZEN, BRITTLE_TICKS);
			}
			case HAUL -> {
				if (caught == null) {
					goTangled(level);
				} else {
					runOn(level, move, target);
				}
			}
			default -> runOn(level, move, target);
		}
		if (this.move == 0) {
			setReach(0);
		}
	}

	/** In its last phase, a move may run straight on into another with a shorter tell. */
	private void runOn(ServerLevel level, int move, LivingEntity target) {
		if (target == null || move > PICKS.length) {
			return;
		}
		int next = chain(move, phase, Math.hypot(target.getX() - getX(), target.getZ() - getZ()), caught != null, random.nextDouble());
		if (next > 0) {
			deck.played(next);
			start(level, next, target, true);
		}
	}

	@Override
	protected void moveBroken(ServerLevel level, int move) {
		setReach(0);
		setState(SLAMMING, false);
		hook = null;
		caught = null;
		cast = 0;
		struck.clear();
	}

	@Override
	protected int rest(ServerLevel level, int move) {
		return CinderWardenRules.rest(phase, move, random.nextDouble());
	}

	/** Whether it's winding up a move (a flare until it bursts): a reaction now rocks it back on its heels, the move lost. */
	private boolean windingUp() {
		if (move == FLARE) {
			return moveSince < moveTell + FLARE_GATHER;
		}
		return move >= HAMMERFALL && move <= FLARE && moveSince < moveTell;
	}

	// ------------------------------------------------------------------ its armour: only reactions break through

	@Override
	protected float resist(ServerLevel level, DamageSource source, float damage) {
		long now = level.getGameTime();
		if (Dungeons.spellLanding()) {
			if (Reactions.reactedWithin(this, 1)) {
				// A reaction set off on it: its plates crack open, and stay open a moment.
				crackedUntil = now + CRACK_TICKS;
				setState(EXPOSED, true);
				crackFx(level);
				// Caught winding up, it's rocked back on its heels and loses the move (not again for a while).
				if (windingUp() && now - lastReel >= REEL_EVERY) {
					lastReel = now;
					breakMove(level);
					pose(REEL, 0, REEL_TICKS);
				}
				return damage;
			}
			if (now < crackedUntil) {
				return damage;
			}
			glance(level, source, now);
			return damage * 0.1F;
		}
		// Fire, lava and burning are nothing to it; anything else is turned by its armour.
		if (!source.is(DamageTypeTags.IS_FIRE)) {
			glance(level, source, now);
		}
		return 0;
	}

	/** A blow or a spell turned by its armour: a dead clang, sparks, and (now and then) a word of why. */
	private void glance(ServerLevel level, DamageSource source, long now) {
		Vec3 c = position().add(0, getBbHeight() * 0.6, 0);
		Vfx.emit(level, ParticleTypes.CRIT, c, 8, 0.6, 0.2);
		Vfx.emit(level, ParticleTypes.SMOKE, c, 4, 0.5, 0.02);
		Fx.sound(level, c, DungeonSounds.WARDEN_IMMUNE, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
		if (source.getEntity() instanceof ServerPlayer player) {
			Long last = told.get(player.getUUID());
			if (last == null || now - last > 200) {
				told.put(player.getUUID(), now);
				player.sendOverlayMessage(Component.translatable("message.wildercord.cinder_warden_immune").withColor(ACCENT));
			}
		}
	}

	/** Cracked open: magma bursts from between its plates. */
	private void crackFx(ServerLevel level) {
		Vec3 c = position().add(0, getBbHeight() * 0.55, 0);
		Sigils.flash(level, c, ACCENT, 2.6F);
		ElementFx.fireImpact(level, c, 1.4);
		Vfx.emit(level, ParticleTypes.LAVA, c, 14, 0.7, 0.3);
		Vfx.radial(level, new DustParticleOptions(0xFFB040, 1.3F), c, 20, 0.35);
		Fx.sound(level, c, DungeonSounds.WARDEN_HURT, 1.2F, 1.0F);
		Fx.sound(level, c, SoundEvents.ANVIL_LAND, 0.5F, 0.6F);
	}

	// ------------------------------------------------------------------ between phases: stoking its forge

	@Override
	protected void onPhase(ServerLevel level, int phase) {
		taught = false;
		waves.clear();
		gobs.clear();
		pools.clear();
		volleyAt = 0;
		Vec3 c = position();
		Sigils.ground(level, c.add(0, 0.05, 0), COLOR, ACCENT, 6.0F, SHIFT_TICKS + 10);
		Fx.sound(level, c, DungeonSounds.BOSS_PHASE, 2.0F, 1.0F);
		announce(level, "message.wildercord.cinder_warden_phase." + phase, COLOR);
		// Two of its keepers climb out of the forge: Runebound, carrying fire (Adepts the second time).
		for (int i = 0; i < 2; i++) {
			Mob guard = EntityTypes.WITHER_SKELETON.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (guard == null) {
				continue;
			}
			double a = level.getRandom().nextDouble() * Math.PI * 2;
			Vec3 spot = CastEngine.ground(level, c.add(Math.cos(a) * 6, 2, Math.sin(a) * 6));
			guard.snapTo(spot.x, spot.y, spot.z, 0, 0);
			guard.finalizeSpawn(level, level.getCurrentDifficultyAt(guard.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
			Runebound.bind(guard, i == 0 ? List.of(Runes.BOLT, Runes.FIRE) : List.of(Runes.ARC, Runes.FROST), phase == 3);
			level.addFreshEntity(guard);
			minion(guard);
			ElementFx.flameBurst(level, spot.add(0, 1, 0), 1.2, 8);
			Sigils.ground(level, spot, COLOR, ACCENT, 1.2F, 30);
		}
	}

	@Override
	protected void shiftTick(ServerLevel level, int left) {
		Vec3 c = position();
		// Fire climbs its chains and spirals up round it as it stokes its core, then it brings both fists down and the heat throws
		// everyone back.
		if (left % 3 == 0) {
			ElementFx.flames(level, c, 1.0, getBbHeight(), 3);
			Vfx.emit(level, ParticleTypes.LAVA, c.add(0, 1.5, 0), 3, 0.6, 0.2);
		}
		if (left % 4 == 0) {
			ElementFx.swirl(level, c, 1.6, getBbHeight() + 1, 2, COLOR, ACCENT);
		}
		ElementFx.embers(level, c.add(0, getBbHeight(), 0), 0.8, 2);
		if (left % 10 == 0 && left > 10) {
			ElementFx.groundRing(level, c.add(0, 0.05, 0), COLOR, SHIFT_REACH, 1.0, 0.2, 10);
		}
		if (left == 50) {
			Fx.sound(level, c, SoundEvents.BLAZE_AMBIENT, 1.6F, 0.5F);
		}
		if (left == 30) {
			Fx.sound(level, c, SoundEvents.BLAZE_SHOOT, 1.6F, 0.5F);
		}
		if (left == 20) {
			setState(SLAMMING, true);
		}
		if (left == 4) {
			setState(SLAMMING, false);
			double power = Runebound.power(level);
			for (Entity e : level.getEntities(this, getBoundingBox().inflate(SHIFT_REACH), e -> Targets.canHarm(this, e))) {
				LivingEntity t = (LivingEntity) e;
				Vec3 away = t.position().subtract(c);
				Vec3 push = new Vec3(away.x, 0, away.z).lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : new Vec3(away.x, 0, away.z).normalize();
				t.hurtServer(level, level.damageSources().mobAttack(this), (float) (SHIFT_DAMAGE * power));
				Effects.push(t, new Vec3(push.x * 1.6, 0.5, push.z * 1.6));
			}
			ElementFx.groundRing(level, c.add(0, 0.05, 0), COLOR, 0.5, SHIFT_REACH + 0.5, 0.3, 14);
			ElementFx.flameBurst(level, c.add(0, 0.5, 0), 4.0, 24);
			Fx.sound(level, c, DungeonSounds.WARDEN_SLAM, 2.0F, 0.7F);
		}
	}

	/** What it's doing on the boss bar: its move, and what it has left itself open to. */
	@Override
	protected Component status() {
		Component doing = move > 0 ? Component.translatable("boss.wildercord.cinder_warden.move." + key(move)).withColor(COLOR) : null;
		Component open = state(EXPOSED) ? Component.translatable("boss.wildercord.cinder_warden_cracked").withColor(ACCENT)
			: Reactions.has(this, Reactions.Mark.WET) ? window("steaming")
			: Reactions.has(this, Reactions.Mark.PULLED) ? window("pulled")
			: Reactions.has(this, Reactions.Mark.WINDSWEPT) ? window("fanned")
			: Reactions.has(this, Reactions.Mark.FROZEN) ? window("brittle")
			: null;
		if (doing != null && open != null) {
			return Component.translatable("boss.wildercord.status", doing, open);
		}
		return doing != null ? doing : open;
	}

	private static Component window(String key) {
		return Component.translatable("boss.wildercord.cinder_warden.window." + key).withColor(ACCENT);
	}

	// ------------------------------------------------------------------ death

	@Override
	protected void dying(ServerLevel level, int tick) {
		Vec3 c = position().add(0, getBbHeight() * 0.5 * (1 - tick / (float) DEATH_TICKS), 0);
		if (tick % 2 == 0) {
			Motes.smoke(level, c, 2, 0.6);
			Vfx.emit(level, ParticleTypes.LAVA, c, tick < DEATH_TICKS / 2 ? 2 : 0, 0.5, 0.2);
		}
		if (tick == 1) {
			Fx.sound(level, c, DungeonSounds.WARDEN_DEATH, 2.0F, 1.0F);
		}
		if (tick % 10 == 0) {
			Fx.sound(level, c, SoundEvents.LAVA_EXTINGUISH, 0.8F, 0.6F + tick / 100F);
		}
	}

	@Override
	protected void vanish(ServerLevel level) {
		Vec3 c = position().add(0, 0.6, 0);
		Motes.clouds(level, c, 14, 0.7, 0x5A5250, 1.6, 45, new Vec3(0, 0.035, 0), 0.1, 0.5);
		Motes.burst(level, c, 16, 0xFF6A2A, 0.14, 24, 0.25);
		ElementFx.embers(level, c, 1.5, 30);
	}

	// ------------------------------------------------------------------ sounds

	@Override
	protected SoundEvent getAmbientSound() {
		return DungeonSounds.WARDEN_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return DungeonSounds.WARDEN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return null;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 140;
	}

	/** For the tests: whether a reaction has its armour cracked open right now. */
	public boolean cracked() {
		return level().getGameTime() < crackedUntil;
	}

	/** Its casting cycle, for the tests: the spells of each phase, in order. */
	public static List<List<RuneDef>> cycle(int phase) {
		return phase == 1 ? PHASE_1 : phase == 2 ? PHASE_2 : PHASE_3;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		return false;
	}
}
