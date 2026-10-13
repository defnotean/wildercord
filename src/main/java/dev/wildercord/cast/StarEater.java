package dev.wildercord.cast;

import dev.wildercord.content.SigilOption;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.content.dungeons.DungeonSounds;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.EffectKind;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static dev.wildercord.cast.StarEaterRules.*;

/**
 * The Star-Eater, keeper of the Astral Observatory: a floating knot of void with an eye at its heart
 * and star shards circling it. While its shard shield is up it turns every spell back on whoever
 * cast it: the Shield mechanics in reverse. It's a real Shield (see {@link Shields}), so a spell that
 * costs more mana than the shield is strong breaks it, the way it would break yours; so does a star
 * shard it throws, knocked back into it (parried) with a blade, a bolt or a Shield of your own.
 * Broken, it sags low and lies open for a few seconds, shedding shards about the floor, then grows its
 * shield again, stronger each phase.
 * <p>
 * Behind its shield it throws its shards in volleys and spears, drops out of the dark onto its prey and
 * lies stunned on the floor after, sends rings skimming out over the floor, stares a burning line after
 * whoever it watches, cages them in its shards, rains shards down from the glass and swallows the light
 * to drag everything in. Broken open it whips its tendrils about itself and calls home the shards it shed,
 * each one that gets back growing its shield sooner. Its moves are in {@link StarEaterRules}.
 */
public class StarEater extends DungeonBoss {
	private static final int COLOR = 0xB45AF0;
	private static final int ACCENT = 0xE8D8FF;
	private static final int GOLD = 0xFFD060;
	/** Its shield's strength (the mana a spell must cost to break it), by phase. */
	private static final float[] STRENGTH = {0, 20, 28, 36};
	/** How much of its shield a parried shard knocks off. */
	private static final float SHARD_CRACK = 16;
	private static final int SHIELD_TICKS = 20 * 60 * 20;
	private static final List<String> SHIELD_RUNES = List.of(Runes.SHIELD.id(), Runes.STARFALL.id(), Runes.INFINITY.id());

	/** Its pose's figures, synced: its eye's tilt (degrees, down), its eye's charge, how limp it hangs, how many shards have left it, and the dark round it. */
	private static final EntityDataAccessor<Float> DATA_PITCH = SynchedEntityData.defineId(StarEater.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_EYE = SynchedEntityData.defineId(StarEater.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_LIMP = SynchedEntityData.defineId(StarEater.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_LOOSED = SynchedEntityData.defineId(StarEater.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_DARK = SynchedEntityData.defineId(StarEater.class, EntityDataSerializers.FLOAT);
	private static final int FIGURES = 5;

	private static final List<List<RuneDef>> PHASE_1 = List.of(
		List.of(Runes.SPARK, Runes.HARM, Runes.VOLLEY_MOD),
		List.of(Runes.ORB, Runes.PULL),
		List.of(Runes.BOLT, Runes.BLIND));
	private static final List<List<RuneDef>> PHASE_2 = List.of(
		List.of(Runes.COMET, Runes.BLACKFLAME),
		List.of(Runes.RAIN, Runes.HARM, Runes.WIDEN),
		List.of(Runes.BEAM, Runes.BANISH),
		List.of(Runes.BOLT, Runes.HEX, Runes.DELAY, Runes.LANCE, Runes.HARM));
	private static final List<List<RuneDef>> PHASE_3 = List.of(
		List.of(Runes.ZONE, Runes.STARFALL),
		List.of(Runes.LANCE, Runes.HARM, Runes.AMPLIFY),
		List.of(Runes.BOLT, Runes.PULL, Runes.DELAY, Runes.COMET, Runes.HARM),
		List.of(Runes.DOMAIN, Runes.HEX, Runes.HARM));

	/** A star shard loose from its orbit: thrown at a foe, flying straight, circling in a cage, falling from the glass, or called home. */
	private static final class Shard {
		/** Homing in on a foe; flying dead straight through whoever is in the way; circling in a cage; falling; called home. */
		static final int HOMING = 0, STRAIGHT = 1, CAGED = 2, FALLING = 3, CALLED = 4;
		final int kind;
		final Set<UUID> struck;
		Vec3 pos;
		Vec3 motion = Vec3.ZERO;
		LivingEntity target;
		Player parriedBy;
		int age;
		/** What it does, as a share of a shard. */
		double damage = 1;
		/** The spear: knocked back, it breaks the shield outright. */
		boolean heavy;
		/** Falling: ticks it hangs glinting before it drops, and the ground it falls to. */
		int hang;
		Vec3 ground;
		/** Caged: where round the cage it circles. */
		double angle;

		Shard(int kind, Vec3 pos, Set<UUID> struck) {
			this.kind = kind;
			this.pos = pos;
			this.struck = struck;
		}
	}

	/** A ring rolling out across the floor from where it began: low (jump it) or high (crouch under it). */
	private static final class Ring {
		final Vec3 centre;
		final long start;
		final double speed;
		final double reach;
		final boolean high;
		final double damage;
		final Set<UUID> struck = new HashSet<>();

		Ring(Vec3 centre, long start, double speed, double reach, boolean high, double damage) {
			this.centre = centre;
			this.start = start;
			this.speed = speed;
			this.reach = reach;
			this.high = high;
			this.damage = damage;
		}
	}

	private final List<Shard> shards = new ArrayList<>();
	/** Shards it shed when its shield broke, lying on the floor until trodden to dust or called home. */
	private final List<Vec3> strewn = new ArrayList<>();
	/** Where its starfall will land. */
	private final List<Vec3> marks = new ArrayList<>();
	private final List<Ring> rings = new ArrayList<>();
	/** Who its blow under way has already caught. */
	private final Set<UUID> struck = new HashSet<>();
	private final long[] lastMade = new long[OWN + 1];
	private long openUntil;
	private double orbit;
	private int reflections;
	private int parries;
	private final Map<UUID, Long> told = new HashMap<>();
	/** Whether the move under way follows another in the same breath; whether its plunge has bounced already. */
	private boolean chained;
	private boolean bounced;
	/** Where its move under way is aimed: where it will land, the cage's heart, the spear's line. */
	private Vec3 mark = Vec3.ZERO;
	private Vec3 aim = Vec3.ZERO;
	/** Its stare's line, in degrees. */
	private double gazeYaw;
	private double gazePitch;
	/** Its whip: where round it began, and which way it goes. */
	private double lashFrom;
	private int lashWay = 1;
	private final float[] shown = new float[FIGURES];
	private final float[] shownO = new float[FIGURES];
	/** How far its shards have wheeled round it: quicker with its shield up, quicker yet between phases and as it swallows the light. */
	private float spin;
	private float spinO;

	public StarEater(EntityType<? extends StarEater> type, Level level) {
		super(type, level, BossEvent.BossBarColor.PURPLE);
		setNoGravity(true);
		java.util.Arrays.fill(lastMade, -100000L);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 320.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.FOLLOW_RANGE, 48.0)
			.add(Attributes.ARMOR, 6.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	/** Wakes the Star-Eater over its star-altar: the stars in the floor go dark, and it opens its eye. */
	public static StarEater rise(ServerLevel level, BlockPos altar) {
		StarEater boss = place(level, DungeonEntities.STAR_EATER, altar, 3.0);
		if (boss == null) {
			return null;
		}
		Vec3 at = Vec3.atBottomCenterOf(altar).add(0, 3.5, 0);
		Sigils.ground(level, Vec3.atBottomCenterOf(altar).add(0, 1.02, 0), COLOR, ACCENT, 6.0F, 80);
		ElementFx.implode(level, at, 6.0, 12);
		ElementFx.starSeal(level, at, new Vec3(0, 1, 0), 3.0, 60);
		Vfx.radial(level, ParticleTypes.END_ROD, at, 50, 0.3);
		Fx.sound(level, at, DungeonSounds.BOSS_RISE, 2.0F, 1.0F);
		Fx.sound(level, at, SoundEvents.END_PORTAL_SPAWN, 0.6F, 1.4F);
		boss.raiseShield(level);
		boss.announce(level, "message.wildercord.star_eater_wakes", COLOR);
		return boss;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_PITCH, 0.0F);
		builder.define(DATA_EYE, 0.0F);
		builder.define(DATA_LIMP, 0.0F);
		builder.define(DATA_LOOSED, 0.0F);
		builder.define(DATA_DARK, 0.0F);
	}

	@Override
	protected List<List<RuneDef>> spells(int phase) {
		return phase == 1 ? PHASE_1 : phase == 2 ? PHASE_2 : PHASE_3;
	}

	@Override
	protected String feat() {
		return Feats.STAR_EATER;
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
	protected double castRange() {
		return 32.0;
	}

	@Override
	protected double leash() {
		return 20.0;
	}

	// ------------------------------------------------------------------ its shard shield

	/** Whether its shield is up right now. */
	public boolean shielded() {
		return Shields.strength(this) > 0;
	}

	/** Its shield's strength: the mana a spell must cost to break it. */
	public float shieldStrength() {
		return Shields.strength(this);
	}

	/** How many spells it has turned back, and how many of its shards have been parried (for the tests). */
	public int reflections() {
		return reflections;
	}

	public int parries() {
		return parries;
	}

	private void raiseShield(ServerLevel level) {
		Shields.give(this, STRENGTH[phase], SHIELD_TICKS, SHIELD_RUNES);
		setState(GUARDED, true);
		setState(EXPOSED, false);
		Vec3 c = heart();
		// Whatever it shed and nobody trod to dust flies home into the new shield.
		for (Vec3 s : strewn) {
			ElementFx.ray(level, s.add(0, 0.2, 0), c, ACCENT, 0.05, 6);
			Vfx.emit(level, ParticleTypes.END_ROD, s.add(0, 0.2, 0), 4, 0.1, 0.05);
		}
		strewn.clear();
		ElementFx.orbit(level, c, 2.2, 8, 30, COLOR, ACCENT);
		Sigils.layer(level, c, new Vec3(0, 1, 0), SigilOption.RING, ACCENT, 2.4F, 30, 0.2F);
		Fx.sound(level, c, WildercordSounds.SHIELD_UP, 1.4F, 0.8F);
	}

	/**
	 * A spell its shield stopped (see {@link Shields}): the shield holds, and the spell comes back out
	 * of it, a moment later, at whoever cast it.
	 */
	void reflect(Cast cast, float strength) {
		if (!(level() instanceof ServerLevel level) || !isAlive() || cast.caster == this) {
			return;
		}
		// The block spent the Shield: its shard shield doesn't spend. It holds at the strength it had,
		// so shards parried into it before still count.
		Shields.give(this, strength, SHIELD_TICKS, SHIELD_RUNES);
		LivingEntity back = cast.caster;
		List<RuneDef> spell = reflected(cast.info.spell());
		reflections++;
		Vec3 c = getBoundingBox().getCenter();
		Fx.sound(level, c, DungeonSounds.STAR_EATER_REFLECT, 1.4F, 1.0F);
		Vec3 toward = back.getEyePosition().subtract(c);
		Sigils.circle(level, c.add(toward.normalize().scale(1.6)), toward, ACCENT, COLOR, 1.6F, 16);
		if (back instanceof ServerPlayer player) {
			long now = level.getGameTime();
			Long last = told.get(player.getUUID());
			if (last == null || now - last > 200) {
				told.put(player.getUUID(), now);
				player.sendOverlayMessage(Component.translatable("message.wildercord.star_eater_reflects", Math.round(STRENGTH[phase])).withColor(ACCENT));
			}
		}
		Scheduler.later(6, () -> {
			if (isAlive() && back.isAlive() && back.level() == level && shifting == 0) {
				Runebound.aimAt(this, back);
				Runebound.cast(level, this, spell, power());
			}
		});
	}

	/** The harmful part of a spell, to throw back: its shapes, modifiers, links and harmful effects. */
	static List<RuneDef> reflected(List<RuneDef> spell) {
		List<RuneDef> out = new ArrayList<>();
		boolean harms = false;
		for (RuneDef rune : spell) {
			if (rune == Runes.IMBUE) {
				// What follows an Imbue is stored, not cast: nothing of it comes back.
				break;
			}
			if (rune.family() == RuneFamily.EFFECT) {
				if (rune.kind() != EffectKind.HARMFUL) {
					continue;
				}
				harms = true;
			}
			if (out.isEmpty() && (rune == Runes.SELF || rune == Runes.TOUCH)) {
				// Its own body or its reach mean nothing thrown back from across the room: it flies.
				out.add(Runes.BOLT);
				continue;
			}
			out.add(rune);
		}
		if (!harms || out.isEmpty() || out.getFirst().family() != RuneFamily.SHAPE) {
			return List.of(Runes.BOLT, Runes.HARM);
		}
		return out;
	}

	private void shieldBroken(ServerLevel level, long now) {
		setState(GUARDED, false);
		setState(EXPOSED, true);
		openUntil = now + OPEN_TICKS[phase];
		interrupt();
		Vec3 c = heart();
		Vfx.radial(level, ParticleTypes.END_ROD, c, 40, 0.45);
		ElementFx.shards(level, c, 2.5, 16);
		Sigils.flash(level, c, ACCENT, 3.0F);
		Fx.sound(level, c, DungeonSounds.STAR_EATER_HURT, 1.6F, 0.8F);
		// Its shards fall out of orbit and scatter over the floor round it, still glowing: it will want them back.
		strewn.clear();
		int count = StarEaterRules.shed(phase);
		double start = random.nextDouble() * Math.PI * 2;
		Vec3 centre = Vec3.atBottomCenterOf(home);
		for (int i = 0; i < count; i++) {
			double a = start + i * Math.PI * 2 / count + (random.nextDouble() - 0.5) * 0.5;
			double r = 3.5 + random.nextDouble() * 4.0;
			double dx = getX() + Math.cos(a) * r - centre.x, dz = getZ() + Math.sin(a) * r - centre.z;
			double k = StarEaterRules.within(dx, dz, 10.5);
			Vec3 spot = CastEngine.ground(level, new Vec3(centre.x + dx * k, floor() + 3, centre.z + dz * k));
			strewn.add(spot);
			ElementFx.ray(level, c, spot.add(0, 0.2, 0), ACCENT, 0.06, 6);
			ElementFx.flatSigil(level, spot, SigilOption.STAR, ACCENT, 0.9, 30, 0.1);
		}
		Fx.sound(level, c, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.4F, 0.7F);
		for (ServerPlayer player : level.players()) {
			if (player.distanceTo(this) < 40) {
				player.sendOverlayMessage(Component.translatable("message.wildercord.star_eater_open").withColor(ACCENT));
			}
		}
	}

	// ------------------------------------------------------------------ where it floats, and what it strikes

	/** The floor of its arena. */
	private double floor() {
		return home.getY();
	}

	/** Its eye, at the heart of it. */
	private Vec3 heart() {
		return getBoundingBox().getCenter();
	}

	/** The floor over its altar. */
	private Vec3 centre() {
		return Vec3.atBottomCenterOf(home);
	}

	/** What {@code share} of a shard does here. */
	private float hurtBy(ServerLevel level, double share) {
		return (float) (share * SHARD * Runebound.power(level));
	}

	private List<LivingEntity> foesNear(ServerLevel level, Vec3 at, double radius) {
		List<LivingEntity> out = new ArrayList<>();
		for (Entity e : level.getEntities(this, new AABB(at, at).inflate(radius), e -> e instanceof LivingEntity && Targets.canHarm(this, e))) {
			out.add((LivingEntity) e);
		}
		return out;
	}

	/** Strikes {@code foe} for {@code share} of a shard, once for each blow: whoever is in {@code hit} already is spared. */
	private boolean strike(ServerLevel level, LivingEntity foe, double share, Set<UUID> hit) {
		if (hit != null && !hit.add(foe.getUUID())) {
			return false;
		}
		SpellDefence.hurt(level, foe, level.damageSources().indirectMagic(this, this), hurtBy(level, share));
		return true;
	}

	/** Shoves {@code foe} away from {@code from} across the floor, and up a little. */
	private void fling(LivingEntity foe, Vec3 from, double out, double up) {
		Vec3 away = new Vec3(foe.getX() - from.x, 0, foe.getZ() - from.z);
		away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
		Effects.push(foe, new Vec3(away.x * out, up, away.z * out));
	}

	/** The way it faces at {@code yaw} degrees, across the floor. */
	private static Vec3 facing(double yaw) {
		double r = Math.toRadians(yaw);
		return new Vec3(-Math.sin(r), 0, Math.cos(r));
	}

	/** Its stare's line at {@code yaw} and {@code pitch} degrees (down is positive). */
	private static Vec3 gazeLine(double yaw, double pitch) {
		double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
		return new Vec3(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
	}

	/** Turns its eye toward {@code at}, no more than {@code most} degrees round this tick, and tilts it up or down to meet it. */
	private void face(Vec3 at, double most) {
		Vec3 c = heart();
		double dx = at.x - c.x, dy = at.y - c.y, dz = at.z - c.z;
		double flat = Math.sqrt(dx * dx + dz * dz);
		if (flat > 1.0E-3) {
			turnTo((float) StarEaterRules.turn(getYRot(), Math.toDegrees(Math.atan2(-dx, dz)), most));
		}
		tilt(-Math.toDegrees(Math.atan2(dy, Math.max(flat, 0.05))));
	}

	/** Sets its facing to {@code yaw} degrees, body and eye together. */
	private void turnTo(float yaw) {
		setYRot(yaw);
		yBodyRot = yaw;
		yHeadRot = yaw;
		Vec3 c = heart(), f = facing(yaw);
		getLookControl().setLookAt(c.x + f.x * 4.0, c.y, c.z + f.z * 4.0);
	}

	/** Tilts its eye {@code pitch} degrees down (up, below nothing), for its pose. */
	private void tilt(double pitch) {
		figure(DATA_PITCH, Math.round(Mth.clamp(pitch, -80, 80) * 2) / 2.0F);
	}

	/** Floats toward {@code goal} (its feet), easing in by {@code ease} of the way a tick and no faster than {@code fastest}, kept over its arena. */
	private void drift(Vec3 goal, double ease, double fastest) {
		Vec3 c = centre();
		double dx = goal.x - c.x, dz = goal.z - c.z;
		double k = StarEaterRules.within(dx, dz, PLUNGE_MOST);
		Vec3 to = new Vec3(c.x + dx * k, Mth.clamp(goal.y, floor(), floor() + 10), c.z + dz * k);
		Vec3 pull = to.subtract(position()).scale(ease);
		if (pull.length() > fastest) {
			pull = pull.normalize().scale(fastest);
		}
		setDeltaMovement(pull);
	}

	private void figure(EntityDataAccessor<Float> data, float value) {
		entityData.set(data, value);
	}

	private void restFigures() {
		figure(DATA_EYE, 0);
		figure(DATA_LIMP, state(EXPOSED) ? 0.5F : 0);
		figure(DATA_LOOSED, state(EXPOSED) ? 1 : 0);
		figure(DATA_DARK, 0);
	}

	/** Where {@code at} comes down on the floor below it. */
	private Vec3 ground(ServerLevel level, Vec3 at) {
		return CastEngine.ground(level, new Vec3(at.x, Math.max(at.y, floor()) + 1.5, at.z));
	}

	/** {@code at} kept within {@code most} of its altar, across the floor. */
	private Vec3 kept(Vec3 at, double most) {
		Vec3 c = centre();
		double dx = at.x - c.x, dz = at.z - c.z, k = StarEaterRules.within(dx, dz, most);
		return new Vec3(c.x + dx * k, at.y, c.z + dz * k);
	}

	// ------------------------------------------------------------------ each tick

	@Override
	protected void approach(ServerLevel level, LivingEntity target) {
	}

	@Override
	protected boolean mayCast(ServerLevel level, LivingEntity target) {
		return !state(EXPOSED);
	}

	@Override
	protected void mechanic(ServerLevel level, long now) {
		if (move == 0) {
			// Drifting in a slow circle high over the altar; sagging low while its shield is down.
			boolean open = state(EXPOSED);
			orbit += open ? 0.004 : 0.012;
			double height = open ? 2.2 : 5.0 + Math.sin(tickCount * 0.05) * 0.6;
			drift(centre().add(Math.cos(orbit) * 4.5, height, Math.sin(orbit) * 4.5), 0.06, 0.35);
			LivingEntity target = getTarget();
			if (target != null && target.isAlive()) {
				face(target.getEyePosition(), 10);
			}
			restFigures();
		}
		if (now % 6 == 0) {
			Vfx.emit(level, ParticleTypes.REVERSE_PORTAL, heart(), 2, 0.6, 0.02);
		}
		// Its shield: broken (by a spell that cost more, or a parried shard), then grown back; never while it's whipping
		// about or calling its shards, which it does only broken open.
		boolean busyOpen = move > 0 && StarEaterRules.exposedOnly(move);
		if (state(GUARDED) && !shielded()) {
			shieldBroken(level, now);
		} else if (state(EXPOSED) && now >= openUntil && !busyOpen || !state(GUARDED) && !state(EXPOSED)) {
			// Grown back; or never raised since it was loaded (its pose isn't saved, only its shield).
			raiseShield(level);
		}
		tickStrewn(level);
		tickRings(level, now);
		tickShards(level);
	}

	/** The shards it shed, glinting on the floor: a foot set on one treads it to dust, and it never gets that one back. */
	private void tickStrewn(ServerLevel level) {
		Iterator<Vec3> it = strewn.iterator();
		while (it.hasNext()) {
			Vec3 s = it.next();
			boolean trodden = false;
			for (Player player : level.getEntitiesOfClass(Player.class, new AABB(s, s).inflate(1.5))) {
				double dx = player.getX() - s.x, dz = player.getZ() - s.z, dy = player.getY() - s.y;
				if (player.isAlive() && !player.isSpectator() && dx * dx + dz * dz <= TREAD * TREAD && dy > -0.5 && dy < 1.0) {
					trodden = true;
					break;
				}
			}
			if (trodden) {
				ElementFx.shards(level, s.add(0, 0.2, 0), 0.8, 8);
				Vfx.emit(level, ParticleTypes.END_ROD, s.add(0, 0.2, 0), 6, 0.2, 0.05);
				Fx.sound(level, s, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 1.5F);
				it.remove();
				continue;
			}
			if (tickCount % 4 == 0) {
				Vfx.emit(level, ParticleTypes.END_ROD, s.add(0, 0.3, 0), 1, 0.1, 0.02);
			}
			if (tickCount % 10 == 0) {
				ElementFx.flatSigil(level, s, SigilOption.STAR, ACCENT, 0.7, 12, 0.1);
			}
		}
	}

	/** Its rings rolling out over the floor: a low one under anyone who stays on their feet, a high one into anyone standing tall. */
	private void tickRings(ServerLevel level, long now) {
		Iterator<Ring> it = rings.iterator();
		while (it.hasNext()) {
			Ring ring = it.next();
			long el = now - ring.start;
			double from = ring.speed * (el - 1), to = ring.speed * el;
			if (from > ring.reach) {
				it.remove();
				continue;
			}
			if (el % 2 == 1) {
				double out = Math.min(ring.reach, to + ring.speed * 2);
				if (ring.high) {
					ElementFx.ring(level, ring.centre.add(0, 2.05, 0), new Vec3(0, 1, 0), ACCENT, to, out, 0.45, 3);
					ElementFx.ring(level, ring.centre.add(0, 1.7, 0), new Vec3(0, 1, 0), COLOR, to, out, 0.2, 3);
				} else {
					ElementFx.ring(level, ring.centre.add(0, 0.45, 0), new Vec3(0, 1, 0), COLOR, to, out, 0.45, 3);
					ElementFx.groundRing(level, ring.centre, ACCENT, to, out, 0.3, 3);
				}
			}
			if (to <= 0) {
				continue;
			}
			for (LivingEntity foe : foesNear(level, ring.centre.add(0, 1, 0), to + 1.5)) {
				if (ring.struck.contains(foe.getUUID())) {
					continue;
				}
				double dx = foe.getX() - ring.centre.x, dz = foe.getZ() - ring.centre.z;
				double feet = foe.getY() - ring.centre.y;
				if (StarEaterRules.skims(ring.high, Math.sqrt(dx * dx + dz * dz), from, to, feet, feet + foe.getBbHeight())) {
					strike(level, foe, ring.damage, ring.struck);
					fling(foe, ring.centre, 0.6, 0.25);
					Vfx.emit(level, ParticleTypes.END_ROD, foe.position().add(0, ring.high ? 1.6 : 0.3, 0), 8, 0.2, 0.08);
				}
			}
		}
	}

	// ------------------------------------------------------------------ its moves

	@Override
	protected int ownMoves() {
		return OWN;
	}

	@Override
	protected void weigh(ServerLevel level, LivingEntity target, double[] weights) {
		long now = level.getGameTime();
		long[] since = new long[OWN + 1];
		for (int m = 0; m <= OWN; m++) {
			since[m] = now - lastMade[m];
		}
		double dx = target.getX() - getX(), dz = target.getZ() - getZ();
		double[] w = StarEaterRules.weights(phase, state(GUARDED), Math.sqrt(dx * dx + dz * dz), target.getY() - floor(),
			hasLineOfSight(target), strewn.size(), since, deck.last());
		System.arraycopy(w, 0, weights, 0, Math.min(w.length, weights.length));
	}

	@Override
	protected double spellWeight(ServerLevel level, LivingEntity target, int index) {
		return SPELL_WEIGHT;
	}

	@Override
	protected void beginMove(ServerLevel level, int move, LivingEntity target) {
		start(level, move, target, false);
	}

	/** Starts {@code m} at {@code target}, {@code pair} if it follows another move in the same breath. */
	private void start(ServerLevel level, int m, LivingEntity target, boolean pair) {
		lastMade[m] = level.getGameTime();
		struck.clear();
		chained = pair;
		marks.clear();
		if (m == PLUNGE) {
			bounced = false;
		}
		Vec3 at = target != null ? target.position() : centre();
		int tell = StarEaterRules.tell(m, pair);
		switch (m) {
			case LANCE -> aim = target != null ? target.getBoundingBox().getCenter() : heart().add(facing(getYRot()).scale(8));
			case PLUNGE -> mark = ground(level, kept(at, PLUNGE_MOST));
			case GAZE -> {
				gazeYaw = getYRot();
				gazePitch = entityData.get(DATA_PITCH);
			}
			case CAGE -> mark = ground(level, kept(at, 11));
			case STARFALL -> {
				// Besides one on each of its prey, a few fall scattered about the floor wherever they like.
				for (int i = 0; i < STARFALL_SCATTER; i++) {
					double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * STARFALL_SPREAD;
					marks.add(ground(level, kept(at.add(Math.cos(a) * r, 0, Math.sin(a) * r), 11)));
				}
			}
			case LASH -> {
				lashFrom = getYRot();
				lashWay = random.nextBoolean() ? 1 : -1;
			}
			default -> {
			}
		}
		pose(m, tell, length(m, tell));
	}

	/** How long {@code m} runs in all, its tell {@code tell} first. */
	private int length(int m, int tell) {
		return switch (m) {
			case VOLLEY -> tell + VOLLEY_EVERY * (StarEaterRules.volley(phase) - 1) + 1 + VOLLEY_RECOVER;
			case LANCE -> tell + 1 + LANCE_RECOVER + (phase >= 3 ? LANCE_RETELL + 1 : 0);
			case PLUNGE -> tell + PLUNGE_FALL + 1;
			case NOVA -> tell + NOVA_GAP * (StarEaterRules.rings(phase).length - 1) + 1 + NOVA_RECOVER;
			case GAZE -> tell + GAZE_TICKS + GAZE_RECOVER;
			case CAGE -> tell + StarEaterRules.cageShut(phase) + 1 + CAGE_RECOVER;
			case STARFALL -> tell + STARFALL_HANG + (int) Math.ceil(STARFALL_HEIGHT / STARFALL_SPEED) + 2 + STARFALL_RECOVER;
			case HORIZON -> tell + HORIZON_PULL + 1 + HORIZON_RECOVER;
			case GATHER -> tell + GATHER_FLIGHT + 1;
			case LASH -> tell + LASH_SPIN + 1 + (phase >= 3 ? LASH_RETELL + LASH_SPIN + 1 : 0) + LASH_RECOVER;
			default -> tell + 1;
		};
	}

	@Override
	protected void moveDone(ServerLevel level, int m, LivingEntity target) {
		if (m == PLUNGE) {
			if (phase >= 3 && !bounced && target != null && state(GUARDED)) {
				// In its last phase it springs straight back up off the floor and comes down again on wherever you ran.
				bounced = true;
				struck.clear();
				mark = ground(level, kept(target.position(), PLUNGE_MOST));
				pose(PLUNGE, PLUNGE_RETELL, PLUNGE_RETELL + PLUNGE_FALL + 1);
				return;
			}
			pose(GROUNDED, 6, StarEaterRules.grounded(phase));
			return;
		}
		if (phase >= 3 && !chained && target != null && state(GUARDED) && m != GROUNDED) {
			double dx = target.getX() - getX(), dz = target.getZ() - getZ();
			int next = StarEaterRules.chain(m, phase, Math.sqrt(dx * dx + dz * dz), random.nextDouble());
			if (next > 0 && StarEaterRules.unlocked(next, phase)) {
				deck.played(next);
				start(level, next, target, true);
			}
		}
	}

	@Override
	protected int rest(ServerLevel level, int m) {
		return chained ? CHAIN_REST + random.nextInt(8) : REST[phase] + random.nextInt(16);
	}

	@Override
	protected void moveBroken(ServerLevel level, int m) {
		shards.removeIf(s -> s.parriedBy == null && (s.kind == Shard.CAGED || s.kind == Shard.FALLING || s.kind == Shard.CALLED));
		marks.clear();
		rings.clear();
		restFigures();
	}

	@Override
	protected void moveTick(ServerLevel level, int m, int t, LivingEntity target) {
		switch (m) {
			case VOLLEY -> volley(level, t, target);
			case LANCE -> lance(level, t, target);
			case PLUNGE -> plunge(level, t, target);
			case GROUNDED -> grounded(level, t);
			case NOVA -> nova(level, t);
			case GAZE -> gaze(level, t, target);
			case CAGE -> cage(level, t, target);
			case STARFALL -> starfall(level, t);
			case HORIZON -> horizon(level, t);
			case GATHER -> gather(level, t);
			case LASH -> lash(level, t, target);
			default -> {
			}
		}
	}

	/** Holds where it floats. */
	private void hold() {
		setDeltaMovement(getDeltaMovement().scale(0.5));
	}

	/** Its shards fanned out before its eye, then loosed one after another, each homing in on whoever it watches. */
	private void volley(ServerLevel level, int t, LivingEntity target) {
		int tell = moveTell, n = StarEaterRules.volley(phase);
		hold();
		if (target != null) {
			face(target.getEyePosition(), 8);
		}
		Vec3 c = heart(), f = gazeLine(getYRot(), entityData.get(DATA_PITCH));
		if (t == 0) {
			Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4F, 1.2F);
		}
		if (t < tell) {
			figure(DATA_EYE, (t + 1) / (float) tell * 0.6F);
			if (t % 3 == 0) {
				// The fan it is about to loose, glinting out before it, one line for each shard.
				for (int k = 0; k < n; k++) {
					Vec3 dir = f.yRot((float) ((k - (n - 1) / 2.0) * 0.3));
					ElementFx.ray(level, c.add(dir.scale(1.2)), c.add(dir.scale(3.2)), ACCENT, 0.03, 3);
					Vfx.emit(level, ParticleTypes.END_ROD, c.add(dir.scale(1.4)), 1, 0.05, 0.01);
				}
			}
			return;
		}
		int k = (t - tell) / VOLLEY_EVERY;
		if ((t - tell) % VOLLEY_EVERY == 0 && k < n) {
			Vec3 dir = f.yRot((float) ((k - (n - 1) / 2.0) * 0.3));
			Shard s = new Shard(Shard.HOMING, c.add(dir.scale(1.4)), new HashSet<>());
			s.motion = dir.scale(VOLLEY_SPEED);
			s.target = target;
			s.damage = VOLLEY_DAMAGE;
			shards.add(s);
			figure(DATA_LOOSED, (k + 1) / (float) n);
			Fx.sound(level, s.pos, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2F, 1.2F + 0.1F * k);
		}
		if (k >= n) {
			figure(DATA_EYE, Math.max(0, entityData.get(DATA_EYE) - 0.1F));
		}
	}

	/** Its shards folded into one spear before its eye, a glint along its aim, then thrown dead straight. */
	private void lance(ServerLevel level, int t, LivingEntity target) {
		int tell = moveTell, second = tell + LANCE_RETELL;
		boolean twice = phase >= 3;
		hold();
		Vec3 c = heart();
		// Its aim follows its prey until a moment before each throw, then holds.
		boolean aiming = t < tell - LANCE_LOCK || twice && t > tell && t < second - LANCE_LOCK;
		if (aiming && target != null) {
			aim = target.getBoundingBox().getCenter();
		}
		face(aim, aiming ? 12 : 0.5);
		if (t == 0 || twice && t == tell + 1) {
			Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_CHIME, 1.6F, 0.5F);
		}
		if (t == tell - LANCE_LOCK || twice && t == second - LANCE_LOCK) {
			Fx.sound(level, c, SoundEvents.LODESTONE_COMPASS_LOCK, 1.6F, 1.2F);
			Sigils.flash(level, c.add(aim.subtract(c).normalize().scale(1.6)), ACCENT, 1.0F);
		}
		boolean telling = t < tell || twice && t > tell && t < second;
		if (telling) {
			float wound = t < tell ? (t + 1) / (float) tell : (t - tell) / (float) LANCE_RETELL;
			figure(DATA_LOOSED, wound);
			figure(DATA_EYE, wound * 0.5F);
			if (t % 2 == 0) {
				// A thread of light along its aim: where the spear will fly, stopped by whatever stands in the way.
				Vec3 dir = aim.subtract(c).normalize();
				Vec3 end = c.add(dir.scale(GAZE_REACH));
				BlockHitResult hit = level.clip(new ClipContext(c, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
				if (hit.getType() != HitResult.Type.MISS) {
					end = hit.getLocation();
				}
				boolean locked = !aiming;
				ElementFx.ray(level, c.add(dir.scale(1.6)), end, locked ? ACCENT : COLOR, locked ? 0.06 : 0.025, 3);
			}
			return;
		}
		if (t == tell || twice && t == second) {
			Vec3 dir = aim.subtract(c).normalize();
			Shard s = new Shard(Shard.STRAIGHT, c.add(dir.scale(1.6)), new HashSet<>());
			s.motion = dir.scale(LANCE_SPEED);
			s.damage = LANCE_DAMAGE;
			s.heavy = true;
			shards.add(s);
			figure(DATA_LOOSED, 0);
			figure(DATA_EYE, 0);
			Fx.sound(level, s.pos, SoundEvents.TRIDENT_THROW, 1.8F, 0.7F);
			Vfx.emit(level, ParticleTypes.END_ROD, s.pos, 10, 0.2, 0.15);
			// Thrown, it lurches back from the throw.
			setDeltaMovement(dir.scale(-0.3));
		}
	}

	/** It rises high, a mark following its prey below, and drops onto the mark like a falling star. */
	private void plunge(ServerLevel level, int t, LivingEntity target) {
		int tell = moveTell, lock = tell - PLUNGE_LOCK;
		Vec3 c = heart();
		if (t < tell) {
			if (t < lock && target != null) {
				mark = ground(level, kept(target.position(), PLUNGE_MOST));
			}
			drift(new Vec3(mark.x, floor() + PLUNGE_HEIGHT, mark.z), 0.25, 0.9);
			tilt(70);
			figure(DATA_EYE, (t + 1) / (float) tell);
			figure(DATA_LOOSED, 0);
			if (t == 0) {
				Fx.sound(level, c, SoundEvents.PHANTOM_FLAP, 1.6F, 0.6F);
			}
			if (t == lock) {
				Fx.sound(level, mark, SoundEvents.LODESTONE_COMPASS_LOCK, 1.6F, 0.9F);
				Sigils.target(level, mark.add(0, 0.05, 0), ACCENT, (float) PLUNGE_RADIUS, PLUNGE_LOCK + PLUNGE_FALL + 2);
			}
			if (t % 2 == 0) {
				// Where it will land: a ring the width of its landing, pale once it stops following.
				ElementFx.groundRing(level, mark, t >= lock ? ACCENT : COLOR, PLUNGE_RADIUS - 0.2, PLUNGE_RADIUS, 0.25, 3);
				Vfx.emit(level, ParticleTypes.END_ROD, c.add(0, -0.8, 0), 2, 0.4, 0.02);
			}
			return;
		}
		int fall = tell + PLUNGE_FALL;
		if (t < fall) {
			if (t == tell) {
				Fx.sound(level, c, SoundEvents.PHANTOM_SWOOP, 1.8F, 0.8F);
			}
			setDeltaMovement(mark.subtract(position()).scale(1.0 / (fall - t)));
			Vfx.emit(level, ParticleTypes.END_ROD, c, 4, 0.3, 0.02);
			ElementFx.ray(level, c, c.add(0, 2.5, 0), ACCENT, 0.2, 3);
			return;
		}
		if (t == fall) {
			setDeltaMovement(Vec3.ZERO);
			for (LivingEntity foe : foesNear(level, mark.add(0, 1, 0), PLUNGE_RADIUS + 1.5)) {
				if (StarEaterRules.lands(foe.getX() - mark.x, foe.getZ() - mark.z, foe.getY() - mark.y) && strike(level, foe, PLUNGE_DAMAGE, struck)) {
					fling(foe, mark, 1.0, 0.45);
				}
			}
			ElementFx.voidImpact(level, mark.add(0, 0.5, 0), 2.0);
			ElementFx.crack(level, mark, PLUNGE_RADIUS, 40);
			net.minecraft.world.level.block.state.BlockState under = ElementFx.groundBlock(level, mark);
			Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, under), mark.add(0, 0.2, 0), 30, 1.2, 0.2);
			ElementFx.stoneShards(level, mark.add(0, 0.2, 0), under, 12, 0.4);
			Fx.sound(level, mark, SoundEvents.MACE_SMASH_GROUND_HEAVY, 2.0F, 0.7F);
			if (phase >= 2) {
				// The landing sends a ring rolling out over the floor: jump it.
				Ring ring = new Ring(mark, level.getGameTime(), WAVE_SPEED, WAVE_REACH, false, WAVE_DAMAGE);
				ring.struck.addAll(struck);
				rings.add(ring);
			}
		}
	}

	/** Fallen to the floor after a plunge, its shards strewn about it, limp: blades reach it through its shield. */
	private void grounded(ServerLevel level, int t) {
		int left = moveLeft;
		Vec3 c = heart();
		if (left > 10) {
			setDeltaMovement(0, position().y > floor() + 0.1 ? -0.2 : 0, 0);
			figure(DATA_LIMP, 1);
			figure(DATA_LOOSED, 1);
			figure(DATA_EYE, 0);
			tilt(30);
			if (t % 8 == 0) {
				Vfx.emit(level, ParticleTypes.END_ROD, c, 3, 0.8, 0.02);
			}
			return;
		}
		// Gathering itself to rise again.
		if (left == 10) {
			Fx.sound(level, c, SoundEvents.PHANTOM_FLAP, 1.4F, 0.8F);
			Vfx.radial(level, ParticleTypes.END_ROD, c, 12, 0.15);
		}
		setDeltaMovement(0, 0.15, 0);
		figure(DATA_LIMP, left / 10.0F);
		figure(DATA_LOOSED, left / 10.0F);
		tilt(left * 3.0);
	}

	/** Sunk low with its shards spun flat about it, it sends rings skimming out over the floor, low or high, each with its own chime. */
	private void nova(ServerLevel level, int t) {
		int tell = moveTell;
		boolean[] order = StarEaterRules.rings(phase);
		drift(new Vec3(getX(), floor() + 1, getZ()), 0.2, 0.5);
		tilt(10);
		Vec3 c = heart(), base = new Vec3(getX(), floor(), getZ());
		if (t == 0) {
			Fx.sound(level, c, SoundEvents.CONDUIT_ACTIVATE, 1.6F, 1.0F);
		}
		figure(DATA_LOOSED, Math.min(1, (t + 1) / (float) tell));
		for (int k = 0; k < order.length; k++) {
			int at = tell + k * NOVA_GAP;
			boolean high = order[k];
			if (t >= at - 8 && t < at) {
				if (t == at - 8) {
					// A low chime for a ring to jump, a high one for a ring to crouch under.
					Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_CHIME, 1.8F, high ? 1.6F : 0.6F);
				}
				if (t % 2 == 0) {
					double h = high ? 2.05 : 0.45;
					ElementFx.ring(level, base.add(0, h, 0), new Vec3(0, 1, 0), high ? ACCENT : COLOR, 1.6, 2.0, 0.3, 3);
					Vfx.emit(level, ParticleTypes.END_ROD, base.add(0, h, 0), 2, 1.2, 0.01);
				}
			}
			if (t == at) {
				rings.add(new Ring(base, level.getGameTime(), NOVA_SPEED[phase], NOVA_REACH, high, NOVA_DAMAGE));
				Fx.sound(level, c, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.6F, high ? 1.3F : 0.8F);
				figure(DATA_EYE, 0.6F);
			}
		}
		if (t > tell) {
			figure(DATA_EYE, Math.max(0, entityData.get(DATA_EYE) - 0.08F));
		}
	}

	/** Its eye opened wide and a thread of light run to its prey, then a burning stare that turns after them. */
	private void gaze(ServerLevel level, int t, LivingEntity target) {
		int tell = moveTell;
		hold();
		Vec3 c = heart();
		if (t < tell) {
			if (t < tell - GAZE_LOCK && target != null) {
				face(target.getBoundingBox().getCenter(), 6);
				gazeYaw = getYRot();
				gazePitch = entityData.get(DATA_PITCH);
			}
			if (t == 0) {
				Fx.sound(level, c, SoundEvents.WARDEN_SONIC_CHARGE, 1.4F, 1.4F);
			}
			figure(DATA_EYE, (t + 1) / (float) tell);
			if (t % 2 == 0) {
				boolean locked = t >= tell - GAZE_LOCK;
				ElementFx.ray(level, c, gazeEnd(level, c), locked ? ACCENT : COLOR, locked ? 0.04 : 0.02, 3);
			}
			return;
		}
		int el = t - tell;
		if (el >= GAZE_TICKS) {
			figure(DATA_EYE, Math.max(0, entityData.get(DATA_EYE) - 0.1F));
			return;
		}
		if (target != null && target.isAlive()) {
			// The stare turns after its prey, but no faster than it can: outrun it, or put a pillar between you.
			Vec3 to = target.getBoundingBox().getCenter().subtract(c);
			double flat = Math.sqrt(to.x * to.x + to.z * to.z);
			double turn = StarEaterRules.gazeTurn(phase);
			gazeYaw = StarEaterRules.turn(gazeYaw, Math.toDegrees(Math.atan2(-to.x, to.z)), turn);
			gazePitch = StarEaterRules.turn(gazePitch, -Math.toDegrees(Math.atan2(to.y, Math.max(flat, 0.05))), turn);
		}
		turnTo((float) gazeYaw);
		tilt(gazePitch);
		figure(DATA_EYE, 1);
		Vec3 end = gazeEnd(level, c);
		if (el == 0) {
			Fx.sound(level, c, SoundEvents.BEACON_ACTIVATE, 1.8F, 1.6F);
		}
		if (el == GAZE_TICKS - 1) {
			Fx.sound(level, c, SoundEvents.BEACON_DEACTIVATE, 1.8F, 1.4F);
		}
		if (el % 2 == 0) {
			ElementFx.ray(level, c, end, ACCENT, 0.25, 3);
			ElementFx.ray(level, c, end, COLOR, 0.1, 3);
			Vfx.emit(level, ParticleTypes.END_ROD, end, 3, 0.2, 0.05);
		}
		if (el % GAZE_EVERY == GAZE_EVERY - 2) {
			Sigils.flash(level, c.add(gazeLine(gazeYaw, gazePitch).scale(1.2)), ACCENT, 1.2F);
		}
		if (el % GAZE_EVERY == 0) {
			Vec3 line = gazeLine(gazeYaw, gazePitch);
			double reach = end.distanceTo(c);
			double half = StarEaterRules.gazeHalf(phase);
			Set<UUID> seared = new HashSet<>();
			for (LivingEntity foe : foesNear(level, c.add(line.scale(reach / 2)), reach / 2 + 2)) {
				double along = Double.MAX_VALUE, across = Double.MAX_VALUE;
				for (Vec3 p : new Vec3[] {foe.position().add(0, 0.3, 0), foe.getBoundingBox().getCenter(), foe.getEyePosition()}) {
					Vec3 d = p.subtract(c);
					double a = d.dot(line);
					double x = d.subtract(line.scale(a)).length();
					if (x < across) {
						across = x;
						along = a;
					}
				}
				across = Math.max(0, across - foe.getBbWidth() / 2);
				if (StarEaterRules.burns(along, across, reach, half) && strike(level, foe, GAZE_DAMAGE, seared)) {
					foe.igniteForSeconds(2);
					Vfx.emit(level, ParticleTypes.END_ROD, foe.getBoundingBox().getCenter(), 6, 0.2, 0.05);
				}
			}
		}
	}

	/** Where its stare ends: its full reach, or the first wall or pillar in the way. */
	private Vec3 gazeEnd(ServerLevel level, Vec3 c) {
		Vec3 end = c.add(gazeLine(gazeYaw, gazePitch).scale(GAZE_REACH));
		BlockHitResult hit = level.clip(new ClipContext(c, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
		return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
	}

	/** Its shards leave it to circle its prey and close in on where they stood. */
	private void cage(ServerLevel level, int t, LivingEntity target) {
		int tell = moveTell, n = StarEaterRules.cage(phase), shut = StarEaterRules.cageShut(phase);
		hold();
		Vec3 c = heart();
		if (t < tell) {
			if (t < tell - CAGE_LOCK && target != null) {
				mark = ground(level, kept(target.position(), 11));
			}
			face(mark.add(0, 1, 0), 8);
			if (t == 0) {
				Fx.sound(level, c, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.4F, 1.3F);
			}
			figure(DATA_LOOSED, (t + 1) / (float) tell);
			if (t % 2 == 0) {
				ElementFx.groundRing(level, mark, COLOR, CAGE_RADIUS - 0.2, CAGE_RADIUS, 0.2, 3);
				for (int i = 0; i < n; i++) {
					double a = i * Math.PI * 2 / n;
					Vfx.emit(level, ParticleTypes.END_ROD, mark.add(Math.cos(a) * CAGE_RADIUS, 1.0, Math.sin(a) * CAGE_RADIUS), 1, 0.05, 0.0);
				}
			}
			if (t == tell - CAGE_LOCK) {
				Fx.sound(level, mark, SoundEvents.LODESTONE_COMPASS_LOCK, 1.6F, 1.1F);
				for (int i = 0; i < n; i++) {
					double a = i * Math.PI * 2 / n;
					ElementFx.ray(level, c, mark.add(Math.cos(a) * CAGE_RADIUS, 1.0, Math.sin(a) * CAGE_RADIUS), ACCENT, 0.04, 4);
				}
			}
			return;
		}
		if (t == tell) {
			double start = random.nextDouble() * Math.PI * 2;
			for (int i = 0; i < n; i++) {
				Shard s = new Shard(Shard.CAGED, mark, struck);
				s.ground = mark;
				s.hang = shut;
				s.angle = start + i * Math.PI * 2 / n;
				s.damage = CAGE_DAMAGE;
				s.pos = mark.add(Math.cos(s.angle) * CAGE_RADIUS, 1.0, Math.sin(s.angle) * CAGE_RADIUS);
				shards.add(s);
			}
			Fx.sound(level, mark, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.4F, 1.0F);
		}
		int el = t - tell;
		if (el < shut) {
			if (el % 6 == 0) {
				Fx.sound(level, mark, SoundEvents.TRIDENT_RETURN, 1.2F, 1.0F + el / (float) shut);
			}
			if (el % 2 == 0) {
				double r = StarEaterRules.closing(el, shut);
				ElementFx.groundRing(level, mark, ACCENT, Math.max(0, r - 0.15), r, 0.2, 3);
			}
			return;
		}
		if (el == shut) {
			// Shut: whoever is still inside is caught in the burst.
			for (LivingEntity foe : foesNear(level, mark.add(0, 1, 0), 2.5)) {
				double dx = foe.getX() - mark.x, dz = foe.getZ() - mark.z, dy = foe.getY() - mark.y;
				if (dx * dx + dz * dz <= 1.5 * 1.5 && dy > -1 && dy < 2.5) {
					strike(level, foe, CAGE_DAMAGE, struck);
				}
			}
			ElementFx.shatterRing(level, mark.add(0, 1, 0), 1.5);
			Vfx.radial(level, ParticleTypes.END_ROD, mark.add(0, 1, 0), 16, 0.25);
			Fx.sound(level, mark, SoundEvents.GLASS_BREAK, 1.6F, 0.9F);
			figure(DATA_LOOSED, 0);
		}
	}

	/** Spikes raised and the stars in the glass flaring, shards fall straight down onto marks round each of its prey. */
	private void starfall(ServerLevel level, int t) {
		int tell = moveTell;
		Vec3 mid = centre();
		drift(mid.add(0, 6, 0), 0.1, 0.4);
		tilt(-60);
		Vec3 c = heart();
		if (t < tell) {
			if (t < tell - STARFALL_LOCK) {
				// A mark under each of its prey, moving with them until the marks settle; the scattered ones stay put.
				while (marks.size() > STARFALL_SCATTER) {
					marks.removeLast();
				}
				int added = 0;
				for (LivingEntity foe : foesNear(level, mid.add(0, 2, 0), 16)) {
					if (added++ >= 4) {
						break;
					}
					marks.add(ground(level, kept(foe.position(), 11)));
				}
			}
			if (t == 0) {
				Fx.sound(level, c, SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.8F, 0.8F);
			}
			if (t == tell - STARFALL_LOCK) {
				Fx.sound(level, c, SoundEvents.LODESTONE_COMPASS_LOCK, 1.4F, 1.4F);
			}
			figure(DATA_EYE, (t + 1) / (float) tell * 0.7F);
			figure(DATA_LOOSED, (t + 1) / (float) tell);
			if (t % 2 == 0) {
				boolean settled = t >= tell - STARFALL_LOCK;
				for (Vec3 at : marks) {
					ElementFx.groundRing(level, at, settled ? ACCENT : COLOR, STARFALL_RADIUS - 0.15, STARFALL_RADIUS, 0.2, 3);
					Vfx.emit(level, ParticleTypes.END_ROD, at.add(0, STARFALL_HEIGHT, 0), 1, 0.15, 0.0);
				}
			}
			if (t % 4 == 0) {
				Vfx.emit(level, ParticleTypes.END_ROD, c.add(0, 1.2, 0), 3, 0.5, 0.08);
			}
			return;
		}
		if (t == tell) {
			for (Vec3 at : marks) {
				Shard s = new Shard(Shard.FALLING, at.add(0, STARFALL_HEIGHT, 0), struck);
				s.ground = at;
				s.hang = STARFALL_HANG;
				s.damage = STARFALL_DAMAGE;
				shards.add(s);
				ElementFx.ray(level, c, s.pos, ACCENT, 0.04, 4);
			}
			Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6F, 1.6F);
		}
		figure(DATA_EYE, Math.max(0, entityData.get(DATA_EYE) - 0.05F));
	}

	/** Its eye gone black, it swallows the light and drags everything toward a grinding ring of shards under it, then caves in. */
	private void horizon(ServerLevel level, int t) {
		int tell = moveTell;
		drift(centre().add(0, 2.5, 0), 0.15, 0.5);
		tilt(25);
		Vec3 c = heart(), base = new Vec3(getX(), floor(), getZ());
		if (t < tell) {
			figure(DATA_DARK, (t + 1) / (float) tell);
			figure(DATA_EYE, 0);
			if (t == 0 || t == tell / 2) {
				Fx.sound(level, c, SoundEvents.WARDEN_HEARTBEAT, 2.0F, 0.6F);
			}
			if (t % 4 == 0) {
				ElementFx.implode(level, c, 3.0, 6);
			}
			if (t % 2 == 0) {
				ElementFx.groundRing(level, base, COLOR, HORIZON_RADIUS - 0.3, HORIZON_RADIUS, 0.2, 3);
				ElementFx.groundRing(level, base, ACCENT, GRIND_RADIUS - 0.2, GRIND_RADIUS, 0.25, 3);
			}
			return;
		}
		int el = t - tell;
		if (el < HORIZON_PULL) {
			figure(DATA_DARK, 1);
			Set<UUID> grind = el % GRIND_EVERY == 0 ? new HashSet<>() : null;
			for (LivingEntity foe : foesNear(level, base.add(0, 1, 0), HORIZON_RADIUS + 1)) {
				double dx = base.x - foe.getX(), dz = base.z - foe.getZ();
				double d = Math.sqrt(dx * dx + dz * dz), h = foe.getY() - floor();
				double drag = StarEaterRules.drag(d);
				if (drag > 0 && h < 6) {
					Effects.push(foe, new Vec3(dx / d * drag, 0, dz / d * drag));
				}
				if (grind != null && StarEaterRules.grinds(d, h)) {
					strike(level, foe, GRIND_DAMAGE, grind);
				}
			}
			if (el % 10 == 0) {
				Fx.sound(level, c, SoundEvents.WARDEN_HEARTBEAT, 2.0F, 0.6F + el / (float) HORIZON_PULL * 0.4F);
			}
			if (el % 3 == 0) {
				ElementFx.implode(level, c, 4.0, 6);
				Vfx.emit(level, ParticleTypes.REVERSE_PORTAL, base.add(0, 0.5, 0), 6, 3.0, 0.02);
			}
			if (el % 2 == 0) {
				ElementFx.groundRing(level, base, ACCENT, GRIND_RADIUS - 0.3, GRIND_RADIUS, 0.35, 3);
				ElementFx.flatSigil(level, base, SigilOption.CRACKED, COLOR, GRIND_RADIUS, 3, 0.4);
				boolean warn = el >= HORIZON_PULL - 10;
				ElementFx.groundRing(level, base, warn ? ACCENT : COLOR, COLLAPSE_RADIUS - 0.2, COLLAPSE_RADIUS, warn ? 0.35 : 0.15, 3);
			}
			if (el == HORIZON_PULL - 10) {
				// Swelling to cave in: get clear of the inner ring.
				Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0F, 0.5F);
			}
			return;
		}
		if (el == HORIZON_PULL) {
			for (LivingEntity foe : foesNear(level, base.add(0, 1, 0), COLLAPSE_RADIUS + 1.5)) {
				double dx = foe.getX() - base.x, dz = foe.getZ() - base.z;
				if (StarEaterRules.collapses(Math.sqrt(dx * dx + dz * dz), foe.getY() - floor()) && strike(level, foe, COLLAPSE_DAMAGE, struck)) {
					fling(foe, base, 1.2, 0.5);
				}
			}
			ElementFx.blackCore(level, c, 2.0, 10);
			ElementFx.ring(level, base.add(0, 0.6, 0), new Vec3(0, 1, 0), ACCENT, 0.5, COLLAPSE_RADIUS + 1, 0.6, 8);
			Vfx.radial(level, ParticleTypes.END_ROD, c, 40, 0.45);
			Fx.sound(level, c, SoundEvents.GENERIC_EXPLODE.value(), 1.6F, 0.6F);
			Fx.sound(level, c, SoundEvents.WARDEN_SONIC_BOOM, 1.4F, 0.8F);
		}
		figure(DATA_DARK, Math.max(0, entityData.get(DATA_DARK) - 0.08F));
	}

	/** Broken open, it reaches for the shards it shed and calls them all home in straight lines. */
	private void gather(ServerLevel level, int t) {
		int tell = moveTell;
		hold();
		Vec3 c = heart();
		if (t < tell) {
			figure(DATA_EYE, (t + 1) / (float) tell * 0.5F);
			if (t == 0) {
				Fx.sound(level, c, SoundEvents.BEACON_POWER_SELECT, 1.6F, 1.4F);
			}
			if (t % 3 == 0) {
				// A thread to each shard it will call: the line it will fly home along.
				for (Vec3 s : strewn) {
					ElementFx.ray(level, s.add(0, 0.3, 0), c, ACCENT, 0.03, 3);
					Vfx.emit(level, ParticleTypes.END_ROD, s.add(0, 0.3, 0), 2, 0.1, 0.05);
				}
			}
			return;
		}
		if (t == tell) {
			for (Vec3 s : strewn) {
				Shard shard = new Shard(Shard.CALLED, s.add(0, 0.3, 0), struck);
				shard.damage = GATHER_DAMAGE;
				shard.motion = c.subtract(shard.pos).normalize().scale(GATHER_SPEED);
				shards.add(shard);
			}
			strewn.clear();
			Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6F, 1.8F);
		}
		figure(DATA_EYE, Math.max(0, entityData.get(DATA_EYE) - 0.03F));
	}

	/** Broken open, its tendrils curled up and back, then whipped all the way round it; after, they hang limp. */
	private void lash(ServerLevel level, int t, LivingEntity target) {
		int tell = moveTell, second = tell + LASH_SPIN + 1 + LASH_RETELL;
		boolean twice = phase >= 3;
		drift(new Vec3(getX(), floor() + 1, getZ()), 0.2, 0.4);
		Vec3 c = heart(), base = new Vec3(getX(), floor(), getZ());
		boolean telling = t < tell || twice && t > tell + LASH_SPIN && t < second;
		if (twice && t == tell + LASH_SPIN + 1) {
			// It curls them back the other way for a second whip.
			struck.clear();
			lashFrom = getYRot();
			lashWay = -lashWay;
		}
		if (telling) {
			if (target != null) {
				face(target.getBoundingBox().getCenter(), 6);
				lashFrom = getYRot();
			}
			if (t == 0 || t == tell + LASH_SPIN + 1) {
				Fx.sound(level, c, SoundEvents.BREEZE_INHALE, 1.6F, 0.7F);
			}
			figure(DATA_LIMP, 0);
			figure(DATA_EYE, 0.3F);
			if (t % 2 == 0) {
				ElementFx.groundRing(level, base, COLOR, LASH_REACH - 0.2, LASH_REACH, 0.25, 3);
			}
			return;
		}
		int s = t < second || !twice ? tell : second;
		int el = t - s + 1;
		if (el >= 1 && el <= LASH_SPIN) {
			for (LivingEntity foe : foesNear(level, base.add(0, 1, 0), LASH_REACH + 1)) {
				double dx = foe.getX() - base.x, dz = foe.getZ() - base.z;
				double offset = (Math.toDegrees(Math.atan2(-dx, dz)) - lashFrom) * lashWay;
				if (StarEaterRules.swept(offset, el, LASH_SPIN) && StarEaterRules.lashes(Math.sqrt(dx * dx + dz * dz), foe.getY() - floor())
						&& strike(level, foe, LASH_DAMAGE, struck)) {
					fling(foe, base, 0.9, 0.35);
				}
			}
			double yaw = lashFrom + lashWay * el * 360.0 / LASH_SPIN;
			turnTo((float) yaw);
			ElementFx.slash(level, base.add(0, 1.1, 0), new Vec3(0, 1, 0), facing(yaw), el % 2 == 0 ? ACCENT : COLOR, LASH_REACH * 0.8, 1.7, 0.35, 2, 5);
			if (el == 1) {
				Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_SWEEP, 1.8F, 0.6F);
			}
			return;
		}
		if (t >= limpFrom()) {
			figure(DATA_LIMP, 1);
			figure(DATA_EYE, 0);
			if (t == limpFrom()) {
				Vfx.emit(level, ParticleTypes.END_ROD, c, 6, 0.6, 0.02);
			}
		}
	}

	/** When its tendrils go limp after its whip. */
	private int limpFrom() {
		return phase >= 3 ? moveTell + LASH_SPIN + 1 + LASH_RETELL + LASH_SPIN : moveTell + LASH_SPIN;
	}

	/** Whether its tendrils hang limp after a whip, open to blows. */
	private boolean lashLimp() {
		return move == LASH && moveSince >= limpFrom();
	}

	// ------------------------------------------------------------------ its shards in flight

	private void tickShards(ServerLevel level) {
		if (shards.isEmpty()) {
			return;
		}
		List<Shard> spent = new ArrayList<>();
		for (Shard s : new ArrayList<>(shards)) {
			if (shards.contains(s) && fly(level, s)) {
				spent.add(s);
			}
		}
		shards.removeAll(spent);
	}

	/** One tick of a shard's flight; whether it is spent. */
	private boolean fly(ServerLevel level, Shard s) {
		s.age++;
		if (s.age > 160) {
			Vfx.emit(level, ParticleTypes.END_ROD, s.pos, 6, 0.2, 0.05);
			return true;
		}
		Vec3 c = heart();
		Vec3 from = s.pos;
		if (s.parriedBy != null) {
			// Knocked back: it flies home, fast, and breaks on the shield (or strikes, with no shield to break).
			Vec3 to = c.subtract(s.pos);
			if (to.length() < 1.3) {
				shardReturns(level, s);
				return true;
			}
			s.pos = s.pos.add(to.normalize().scale(Math.min(0.8, to.length())));
			ElementFx.ray(level, from, s.pos, GOLD, s.heavy ? 0.14 : 0.07, 3);
			Vfx.emit(level, ParticleTypes.CRIT, s.pos, 1, 0.05, 0.0);
			return false;
		}
		switch (s.kind) {
			case Shard.HOMING -> {
				if (s.target != null && s.target.isAlive()) {
					Vec3 to = s.target.getBoundingBox().getCenter().subtract(s.pos);
					s.motion = s.motion.add(to.normalize().scale(0.04)).normalize().scale(VOLLEY_SPEED);
				}
				s.pos = s.pos.add(s.motion);
			}
			case Shard.STRAIGHT -> s.pos = s.pos.add(s.motion);
			case Shard.CAGED -> {
				if (s.age >= s.hang) {
					return true;
				}
				s.angle += 0.09;
				double r = StarEaterRules.closing(s.age, s.hang);
				s.pos = s.ground.add(Math.cos(s.angle) * r, 1.0, Math.sin(s.angle) * r);
			}
			case Shard.FALLING -> {
				if (s.hang > 0) {
					// Hanging glinting under the glass over its mark, a moment before it drops.
					s.hang--;
					if (s.age % 2 == 0) {
						ElementFx.ray(level, s.pos, s.pos.add(0, -1.2, 0), ACCENT, 0.05, 3);
						Vfx.emit(level, ParticleTypes.END_ROD, s.pos, 2, 0.1, 0.02);
					}
					return false;
				}
				s.pos = new Vec3(s.pos.x, Math.max(s.ground.y, s.pos.y - STARFALL_SPEED), s.pos.z);
				ElementFx.ray(level, from, s.pos, ACCENT, 0.1, 3);
				if (s.pos.y <= s.ground.y + 0.2) {
					starLands(level, s);
					return true;
				}
				return false;
			}
			case Shard.CALLED -> {
				Vec3 to = c.subtract(s.pos);
				if (to.length() < 1.3) {
					// Home: each one back grows its shield sooner.
					openUntil -= StarEaterRules.hasten(phase);
					Vfx.emit(level, ParticleTypes.END_ROD, c, 6, 0.4, 0.05);
					Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F, 1.6F);
					return true;
				}
				if (s.age > GATHER_FLIGHT + 10) {
					return true;
				}
				s.motion = to.normalize().scale(Math.min(GATHER_SPEED, to.length()));
				s.pos = s.pos.add(s.motion);
			}
			default -> {
			}
		}
		if (s.kind == Shard.HOMING || s.kind == Shard.STRAIGHT) {
			BlockHitResult wall = level.clip(new ClipContext(from, s.pos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
			if (wall.getType() != HitResult.Type.MISS) {
				Vec3 at = wall.getLocation();
				Vfx.emit(level, ParticleTypes.END_ROD, at, s.heavy ? 16 : 8, 0.2, 0.06);
				Fx.sound(level, at, SoundEvents.AMETHYST_CLUSTER_BREAK, s.heavy ? 1.4F : 0.8F, s.heavy ? 0.9F : 1.6F);
				if (s.heavy) {
					ElementFx.shards(level, at, 1.2, 8);
				}
				return true;
			}
		}
		if (s.heavy) {
			ElementFx.ray(level, from, s.pos, COLOR, 0.16, 3);
			ElementFx.ray(level, from, s.pos, ACCENT, 0.07, 3);
			Vfx.emit(level, new DustParticleOptions(ACCENT, 1.4F), s.pos, 2, 0.08, 0.0);
		} else {
			ElementFx.ray(level, from, s.pos, ACCENT, 0.06, 3);
			Vfx.emit(level, new DustParticleOptions(COLOR, 1.0F), s.pos, 1, 0.05, 0.0);
		}
		if (parried(level, s)) {
			return false;
		}
		double grow = s.heavy ? 0.5 : 0.3;
		for (LivingEntity foe : foesNear(level, s.pos, 2.5)) {
			AABB box = foe.getBoundingBox().inflate(grow);
			if (s.struck.contains(foe.getUUID()) || !box.contains(s.pos) && box.clip(from, s.pos).isEmpty()) {
				continue;
			}
			if (foe instanceof Player player && Shields.strength(player) > 0) {
				// A Shield of their own turns the shard back where it came from.
				parry(level, s, player);
				return false;
			}
			strike(level, foe, s.damage, s.struck);
			Vfx.radial(level, ParticleTypes.END_ROD, s.pos, 12, 0.2);
			Fx.sound(level, s.pos, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 1.2F);
			if (s.kind == Shard.HOMING) {
				return true;
			}
		}
		return false;
	}

	/** A falling shard strikes its mark, and whoever is still standing on it. */
	private void starLands(ServerLevel level, Shard s) {
		Vec3 at = s.ground;
		for (LivingEntity foe : foesNear(level, at.add(0, 1, 0), STARFALL_RADIUS + 1.5)) {
			if (StarEaterRules.fallsOn(foe.getX() - at.x, foe.getZ() - at.z, foe.getY() - at.y)) {
				strike(level, foe, s.damage, s.struck);
			}
		}
		ElementFx.shards(level, at.add(0, 0.2, 0), 1.0, 8);
		Vfx.radial(level, ParticleTypes.END_ROD, at.add(0, 0.3, 0), 10, 0.15);
		ElementFx.groundRing(level, at, ACCENT, 0.2, STARFALL_RADIUS, 0.3, 5);
		Fx.sound(level, at, SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F, 1.4F);
		Fx.sound(level, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 1.0F);
	}

	/** Whether someone knocks this shard back: a swing of a blade (or a fist) just as it arrives, or a bolt of their own. */
	private boolean parried(ServerLevel level, Shard shard) {
		for (Player player : level.getEntitiesOfClass(Player.class, new AABB(shard.pos, shard.pos).inflate(3.0))) {
			if (!player.isAlive() || player.isSpectator() || !player.isSwinging() || player.getSwingAnimation(1.0F) > 0.6F) {
				continue;
			}
			Vec3 toShard = shard.pos.subtract(player.getEyePosition());
			if (toShard.length() > 3.2 || player.getLookAngle().dot(toShard.normalize()) < 0.3) {
				continue;
			}
			parry(level, shard, player);
			return true;
		}
		for (RuneBolt bolt : level.getEntitiesOfClass(RuneBolt.class, new AABB(shard.pos, shard.pos).inflate(1.3))) {
			if (bolt.getOwner() instanceof Player player) {
				bolt.discard();
				parry(level, shard, player);
				return true;
			}
		}
		return false;
	}

	/** A shard knocked back: it turns, with a bright ring, and flies home. */
	void parry(ServerLevel level, Shard shard, Player by) {
		shard.parriedBy = by;
		shard.age = 0;
		parries++;
		Sigils.flash(level, shard.pos, ACCENT, shard.heavy ? 2.0F : 1.4F);
		Vfx.emit(level, ParticleTypes.CRIT, shard.pos, shard.heavy ? 18 : 10, 0.2, 0.3);
		Fx.sound(level, shard.pos, DungeonSounds.SHARD_PARRY, 1.4F, shard.heavy ? 0.7F : 1.0F);
	}

	/** A knocked-back shard home: it cracks the shield (the spear breaks it outright), or strikes it with no shield to stop it. */
	private void shardReturns(ServerLevel level, Shard shard) {
		Vec3 c = heart();
		float strength = Shields.strength(this);
		Vfx.radial(level, ParticleTypes.END_ROD, shard.pos, 18, 0.3);
		if (strength > 0) {
			float left = shard.heavy ? 0 : strength - SHARD_CRACK;
			if (left <= 0) {
				// Broken by its own shard: noticed next tick, like a spell breaking it.
				removeAttached(WildercordAttachments.SPELL_SHIELD);
				ElementFx.shards(level, c, 2.0, 12);
				Fx.sound(level, c, WildercordSounds.SHIELD_BREAK, 1.6F, 0.9F);
			} else {
				Shields.give(this, left, SHIELD_TICKS, SHIELD_RUNES);
				Sigils.flash(level, c, ACCENT, 2.0F);
				Fx.sound(level, c, WildercordSounds.SHIELD_BLOCK, 1.4F, 0.8F);
			}
		} else if (shard.parriedBy != null) {
			hurtServer(level, level.damageSources().indirectMagic(shard.parriedBy, shard.parriedBy), shard.heavy ? 15 : 10);
		}
	}

	/** A few shards thrown at {@code target} at once, one more for each phase. */
	private void loose(ServerLevel level, LivingEntity target) {
		Vec3 c = heart();
		Vec3 toward = target.getEyePosition().subtract(c).normalize();
		for (int i = 0; i < phase; i++) {
			double spread = (i - (phase - 1) / 2.0) * 0.35;
			Vec3 motion = toward.yRot((float) spread).scale(VOLLEY_SPEED);
			Shard shard = new Shard(Shard.HOMING, c.add(motion.normalize().scale(1.4)), new HashSet<>());
			shard.motion = motion;
			shard.target = target;
			shards.add(shard);
		}
		Fx.sound(level, c, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2F, 1.4F);
	}

	/** For the tests: a shard thrown at {@code target}, and whether any of its shards is on its way back. */
	public void throwShard(LivingEntity target) {
		if (level() instanceof ServerLevel level) {
			loose(level, target);
		}
	}

	public boolean shardReturning() {
		return shards.stream().anyMatch(s -> s.parriedBy != null);
	}

	public int shardsInFlight() {
		return shards.size();
	}

	/** For the tests: the first shard in flight is knocked back by {@code player}, as a parry would. */
	public boolean parryFirst(Player player) {
		if (!(level() instanceof ServerLevel level) || shards.isEmpty()) {
			return false;
		}
		parry(level, shards.getFirst(), player);
		return true;
	}

	// ------------------------------------------------------------------ harm

	@Override
	protected float resist(ServerLevel level, DamageSource source, float damage) {
		if (!Dungeons.spellLanding() && shielded() && move != GROUNDED) {
			// Arrows and blades glance off the circling shards; not while it lies grounded with its shards strewn about it.
			Vec3 c = heart();
			Vfx.emit(level, ParticleTypes.END_ROD, c, 6, 0.6, 0.05);
			Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_HIT, 1.0F, 1.4F);
			return 0;
		}
		float hurt = state(EXPOSED) ? damage * 1.25F : damage;
		// Its tendrils hanging limp after a whip, it can't so much as flinch away.
		return lashLimp() ? hurt * 1.5F : hurt;
	}

	@Override
	protected Component status() {
		if (move > 0) {
			boolean opening = move == GROUNDED || lashLimp();
			return Component.translatable("boss.wildercord.star_eater.move." + StarEaterRules.key(move)).withColor(opening ? GOLD : ACCENT);
		}
		if (state(EXPOSED)) {
			return Component.translatable("boss.wildercord.star_eater_open").withColor(GOLD);
		}
		float strength = Shields.strength(this);
		return strength > 0 ? Component.translatable("boss.wildercord.star_eater_shielded", Math.round(strength)).withColor(ACCENT) : null;
	}

	// ------------------------------------------------------------------ between phases: an eclipse

	@Override
	protected void onPhase(ServerLevel level, int phase) {
		Vec3 c = heart();
		// Everything it had loose falls still and fades as it gathers itself.
		for (Shard s : shards) {
			Vfx.emit(level, ParticleTypes.END_ROD, s.pos, 6, 0.2, 0.05);
		}
		shards.clear();
		rings.clear();
		marks.clear();
		Fx.sound(level, c, DungeonSounds.BOSS_PHASE, 2.0F, 1.0F);
		ElementFx.blackCore(level, c, 1.5, SHIFT_TICKS);
		announce(level, "message.wildercord.star_eater_phase." + phase, COLOR);
		for (int i = 0; i < 2; i++) {
			Mob guard = (i == 0 ? EntityTypes.STRAY : EntityTypes.SKELETON).create(level, EntitySpawnReason.MOB_SUMMONED);
			if (guard == null) {
				continue;
			}
			double a = level.getRandom().nextDouble() * Math.PI * 2;
			Vec3 spot = CastEngine.ground(level, centre().add(Math.cos(a) * 7, 2, Math.sin(a) * 7));
			guard.snapTo(spot.x, spot.y, spot.z, 0, 0);
			guard.finalizeSpawn(level, level.getCurrentDifficultyAt(guard.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
			Runebound.bind(guard, i == 0 ? List.of(Runes.SPARK, Runes.HARM) : List.of(Runes.BOLT, Runes.BLIND), phase == 3);
			level.addFreshEntity(guard);
			minion(guard);
			Vfx.radial(level, ParticleTypes.REVERSE_PORTAL, spot.add(0, 1, 0), 24, 0.3);
			Sigils.ground(level, spot, COLOR, ACCENT, 1.2F, 30);
		}
	}

	@Override
	protected void shiftTick(ServerLevel level, int left) {
		// It rises to the middle of its dome with its eye blazing and its shards flung out wide, the dark folding in round it.
		drift(centre().add(0, 4.5, 0), 0.08, 0.3);
		tilt(0);
		Vec3 c = heart();
		if (left > 5) {
			figure(DATA_EYE, 1);
			figure(DATA_LIMP, 0);
			figure(DATA_LOOSED, 0);
			figure(DATA_DARK, Math.min(1, (SHIFT_TICKS - left) / 30.0F));
		}
		if (left > 10) {
			// Everything nearby is drawn in toward its eye.
			for (Entity e : level.getEntities(this, getBoundingBox().inflate(12.0), e -> Targets.canHarm(this, e))) {
				Vec3 in = c.subtract(e.position());
				if (in.length() > 2.5) {
					Effects.push((LivingEntity) e, in.normalize().scale(0.05));
				}
			}
			if (left % 4 == 0) {
				ElementFx.implode(level, c, 5.0, 6);
			}
			if (left % 6 == 0) {
				ElementFx.orbit(level, c, 3.0, 6, 8, COLOR, ACCENT);
				Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4F, 0.5F + (SHIFT_TICKS - left) / (float) SHIFT_TICKS);
			}
		}
		if (left == 5) {
			// Then a new, stronger shield bursts open and flings it all away.
			for (Entity e : level.getEntities(this, getBoundingBox().inflate(6.0), e -> Targets.canHarm(this, e))) {
				LivingEntity t = (LivingEntity) e;
				Vec3 out = t.position().subtract(c);
				Vec3 flat = new Vec3(out.x, 0, out.z).lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : new Vec3(out.x, 0, out.z).normalize();
				SpellDefence.hurt(level, t, level.damageSources().indirectMagic(this, this), (float) (5 * Runebound.power(level)));
				Effects.push(t, new Vec3(flat.x * 1.5, 0.4, flat.z * 1.5));
			}
			Vfx.radial(level, ParticleTypes.END_ROD, c, 60, 0.5);
			ElementFx.starSeal(level, c, new Vec3(0, 1, 0), 3.5, 20);
			ElementFx.ring(level, c, new Vec3(0, 1, 0), ACCENT, 1.0, 7.0, 0.5, 8);
			figure(DATA_DARK, 0);
			figure(DATA_LOOSED, 0);
			figure(DATA_EYE, 0.5F);
			raiseShield(level);
		}
	}

	// ------------------------------------------------------------------ death

	@Override
	public void die(DamageSource source) {
		shards.clear();
		strewn.clear();
		rings.clear();
		marks.clear();
		super.die(source);
	}

	@Override
	protected void dying(ServerLevel level, int tick) {
		Vec3 c = heart();
		setDeltaMovement(0, -0.01, 0);
		figure(DATA_LIMP, Math.min(1, tick / 20.0F));
		figure(DATA_EYE, Math.max(0, 1 - tick / (float) DEATH_TICKS));
		figure(DATA_DARK, Math.min(1, tick / (float) DEATH_TICKS));
		if (tick == 1) {
			Fx.sound(level, c, DungeonSounds.STAR_EATER_DEATH, 2.0F, 1.0F);
		}
		if (tick % 3 == 0) {
			ElementFx.implode(level, c, 3.0 * (1 - tick / (double) DEATH_TICKS) + 0.5, 6);
			Vfx.emit(level, ParticleTypes.REVERSE_PORTAL, c, 6, 0.4, 0.02);
		}
	}

	@Override
	protected void vanish(ServerLevel level) {
		Vec3 c = heart();
		ElementFx.starSeal(level, c, new Vec3(0, 1, 0), 4.0, 40);
		Vfx.radial(level, ParticleTypes.END_ROD, c, 80, 0.6);
		Sigils.flash(level, c, ACCENT, 5.0F);
	}

	// ------------------------------------------------------------------ its pose (client)

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			float[] goal = {entityData.get(DATA_PITCH), entityData.get(DATA_EYE), entityData.get(DATA_LIMP), entityData.get(DATA_LOOSED),
				entityData.get(DATA_DARK)};
			for (int i = 0; i < FIGURES; i++) {
				shownO[i] = shown[i];
				shown[i] += (goal[i] - shown[i]) * (i == 0 ? 0.35F : 0.3F);
			}
			spinO = spin;
			spin += 0.02F + (state(GUARDED) ? 0.07F : 0) + (state(SHIFTING) ? 0.2F : 0) + shown[4] * 0.35F;
			if (spin > 2000) {
				spin -= Mth.TWO_PI * 300;
				spinO -= Mth.TWO_PI * 300;
			}
		}
	}

	/**
	 * Its eye's tilt (degrees, down), its eye's charge, how limp it hangs, how many shards have left it, the dark round it, and how far
	 * its shards have wheeled round it (radians).
	 */
	@Override
	public float extra(int i, float partial) {
		if (i == FIGURES) {
			return Mth.lerp(partial, spinO, spin);
		}
		return i >= 0 && i < FIGURES ? Mth.lerp(partial, shownO[i], shown[i]) : 0;
	}

	// ------------------------------------------------------------------ sounds and such

	@Override
	protected SoundEvent getAmbientSound() {
		return DungeonSounds.STAR_EATER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return DungeonSounds.STAR_EATER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return null;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 160;
	}

	@Override
	public boolean fireImmune() {
		return true;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		return false;
	}

	@Override
	protected void returnHome(ServerLevel level) {
		super.returnHome(level);
		teleportTo(getX(), home.getY() + 5, getZ());
	}
}
