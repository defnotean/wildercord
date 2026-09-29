package dev.wildercord.cast;

import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Bolt shape: a gravity-free spell projectile. Each client draws it as a glowing comet with a
 * trail (following the entity's smoothed position, from the colours synced here); the server adds
 * only a few motes. It carries its spell in memory only and is never saved, so a bolt left over
 * after a restart simply vanishes.
 */
public class RuneBolt extends Projectile {
	public static final int MAX_LIVE_PER_PLAYER = 24;
	/** The bolt's colours, for the comet each client draws. */
	public static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(RuneBolt.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_SECONDARY = SynchedEntityData.defineId(RuneBolt.class, EntityDataSerializers.INT);
	/** What the bolt's modifiers make it look like, for the comet each client draws (see {@link #style}). */
	public static final EntityDataAccessor<Integer> DATA_STYLE = SynchedEntityData.defineId(RuneBolt.class, EntityDataSerializers.INT);
	/** Style bits: how much stronger it is (0 to 3: Amplify, Overcharge), thin (Frugal), a needle (Pierce), seeking (Homing). */
	public static final int STYLE_POWER = 0x3, STYLE_FRUGAL = 0x4, STYLE_PIERCE = 0x8, STYLE_HOMING = 0x10;
	private static final double RANGE = 48.0;
	private static final Map<UUID, Integer> LIVE = new ConcurrentHashMap<>();

	/** When the server stops: bolts saved in flight never report their removal. */
	static void clearLive() {
		LIVE.clear();
	}

	private Cast cast;
	private SpellPlan.Group group;
	private SpellPlan.Link anchored;
	private int pierceLeft;
	private int bouncesLeft;
	private boolean homing;
	private double speed;
	private boolean arc;
	private int lifeLeft;
	/** How much further a (straight, not lobbed) bolt may fly: {@link #RANGE} blocks in all, however fast it goes. */
	private double travelLeft;
	private int color;
	private Vfx.Theme theme;
	private final List<Entity> alreadyHit = new ArrayList<>();
	/** A parried bolt flies back at the one who cast it, and steers after them. */
	private LivingEntity quarry;

	public RuneBolt(EntityType<? extends RuneBolt> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	public static void launch(Cast cast, SpellPlan.Group group, SpellPlan.Link anchored, Vec3 origin, Vec3 dir) {
		launch(cast, group, anchored, origin, dir, false);
	}

	/**
	 * A spell a Shield parried, flying back at {@code quarry} (its caster) as {@code turned}'s: a bolt
	 * of the same shape's effects, a little faster, steering after them.
	 */
	static void reflect(Cast turned, SpellPlan.Group group, SpellPlan.Link anchored, Vec3 origin, LivingEntity quarry) {
		Vec3 aim = quarry.getBoundingBox().getCenter().subtract(origin);
		RuneBolt bolt = launch(turned, group, anchored, origin, aim.lengthSqr() < 1.0E-4 ? turned.caster.getLookAngle() : aim, false);
		if (bolt != null) {
			bolt.turnBack(quarry);
		}
	}

	/** Launches a bolt, or with {@code arc} a lobbed one that falls and splashes where it lands. */
	public static RuneBolt launch(Cast cast, SpellPlan.Group group, SpellPlan.Link anchored, Vec3 origin, Vec3 dir, boolean arc) {
		UUID owner = cast.caster.getUUID();
		if (LIVE.getOrDefault(owner, 0) >= MAX_LIVE_PER_PLAYER) {
			return null;
		}
		RuneBolt bolt = new RuneBolt(WildercordEntities.RUNE_BOLT, cast.level);
		// The same cast, not a child: a bolt is part of its shape, not a link, so it takes none of the cast's link depth.
		bolt.cast = cast;
		bolt.group = group;
		bolt.anchored = anchored;
		bolt.pierceLeft = SpellNumbers.pierce(group);
		bolt.bouncesLeft = SpellNumbers.bounces(group);
		bolt.homing = SpellNumbers.homing(group);
		bolt.arc = arc;
		bolt.speed = arc ? SpellNumbers.arcSpeed(group) : SpellNumbers.boltSpeed(group);
		bolt.lifeLeft = arc ? 120 : (int) Math.ceil(RANGE / bolt.speed) + 4;
		bolt.travelLeft = RANGE;
		bolt.color = CastEngine.colorOf(group);
		bolt.theme = cast.theme(group);
		bolt.getEntityData().set(DATA_COLOR, bolt.theme.primary());
		bolt.getEntityData().set(DATA_SECONDARY, bolt.theme.secondary());
		bolt.getEntityData().set(DATA_STYLE, style(group));
		bolt.setOwner(cast.caster);
		bolt.setPos(origin);
		bolt.setDeltaMovement(dir.normalize().scale(bolt.speed));
		LIVE.merge(owner, 1, Integer::sum);
		cast.level.addFreshEntity(bolt);
		if (cast.depth > 0) {
			// Fired by a link: the cast circle's cast sound is long past. A bolt from the hand has just had it.
			Fx.sound(cast.level, origin, bolt.theme.cast(), 0.5F, 1.0F);
		}
		return bolt;
	}

	/** The comet's style from the group's modifiers: a heavier core for Amplify and Overcharge, a thin one for Frugal, a needle, a corkscrew. */
	static int style(SpellPlan.Group group) {
		int power = 0;
		boolean frugal = false;
		for (SpellPlan.EffectNode e : group.effects) {
			power = Math.max(power, e.count(dev.wildercord.spell.Runes.AMPLIFY) + 2 * e.count(dev.wildercord.spell.Runes.OVERCHARGE_MOD));
			frugal |= e.count(dev.wildercord.spell.Runes.FRUGAL_MOD) > 0;
		}
		int style = Math.min(3, power);
		if (frugal) {
			style |= STYLE_FRUGAL;
		}
		if (SpellNumbers.pierce(group) > 0) {
			style |= STYLE_PIERCE;
		}
		if (SpellNumbers.homing(group)) {
			style |= STYLE_HOMING;
		}
		return style;
	}

	/** Parried: the bolt turns round where it met the Shield and flies back at its caster, now {@code defender}'s. */
	private void reflect(LivingEntity defender, Vec3 at) {
		LivingEntity back = cast.caster;
		LIVE.computeIfPresent(back.getUUID(), (k, n) -> n <= 1 ? null : n - 1);
		LIVE.merge(defender.getUUID(), 1, Integer::sum);
		cast = cast.reflected(defender);
		setOwner(defender);
		setPos(at);
		Vec3 aim = back.getBoundingBox().getCenter().subtract(at);
		setDeltaMovement((aim.lengthSqr() < 1.0E-4 ? getDeltaMovement().scale(-1) : aim).normalize().scale(speed));
		turnBack(back);
	}

	private void turnBack(LivingEntity back) {
		quarry = back;
		alreadyHit.clear();
		alreadyHit.add(cast.caster);
		arc = false;
		bouncesLeft = 0;
		speed = Math.min(3.0, speed * dev.wildercord.spell.Parry.REFLECT_SPEED);
		setDeltaMovement(getDeltaMovement().normalize().scale(speed));
		lifeLeft = (int) Math.ceil(RANGE / speed) + 4;
		travelLeft = RANGE;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_COLOR, 0xFFFFFF);
		builder.define(DATA_SECONDARY, 0xFFFFFF);
		builder.define(DATA_STYLE, 0);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel server)) {
			return;
		}
		if (cast == null || !cast.alive() || --lifeLeft <= 0 || !arc && travelLeft <= 1.0E-3) {
			fizzle();
			return;
		}
		if (quarry != null) {
			hunt();
		} else if (homing) {
			steer();
		}
		if (arc) {
			setDeltaMovement(getDeltaMovement().add(0, -0.055, 0));
		}
		Vec3 motion = getDeltaMovement();
		if (!arc) {
			// Up to 48 blocks, as it says: a quickened bolt's spare ticks used to carry it on well past that.
			double step = motion.length();
			if (step > travelLeft) {
				motion = motion.scale(travelLeft / step);
			}
			travelLeft -= Math.min(step, travelLeft);
		}
		Vec3 from = position();
		Vec3 to = from.add(motion);
		BlockHitResult block = server.clip(new net.minecraft.world.level.ClipContext(from, to,
			net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this));
		Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
		if (collide(server, from, end)) {
			return;
		}
		Shields.Interception shield = Shields.intercept(cast, from, end);
		if (shield != null && !alreadyHit.contains(shield.target()) && Shields.harmful(group, anchored) && Shields.parries(cast, shield.target())) {
			// Raised at the last moment, the Shield turns it: back it goes, at whoever cast it.
			Shields.parry(cast, shield.target(), from, false);
			reflect(shield.target(), shield.at());
			return;
		}
		if (shield != null && !alreadyHit.contains(shield.target())) {
			// It strikes the Shield's circle: stopped there, or through it and into what it was aimed at.
			setPos(shield.at());
			hitEntity(new EntityHitResult(shield.target(), shield.at()));
			if (!isRemoved() && Shields.blocked(cast, shield.target())) {
				fizzle();
			}
			return;
		}
		EntityHitResult entity = ProjectileUtil.getEntityHitResult(server, this, from, end,
			getBoundingBox().expandTowards(motion).inflate(1.0), this::canHitEntity);
		if (entity != null) {
			hitEntity(entity);
			if (isRemoved()) {
				return;
			}
		} else if (block.getType() != HitResult.Type.MISS) {
			if (bouncesLeft > 0) {
				bouncesLeft--;
				Vec3 normal = Vec3.atLowerCornerOf(block.getDirection().getUnitVec3i());
				Vec3 reflected = motion.subtract(normal.scale(2 * motion.dot(normal)));
				setPos(block.getLocation().add(normal.scale(0.05)));
				setDeltaMovement(reflected);
				Vfx.impact(server, block.getLocation(), theme, 0.4);
				return;
			}
			setPos(block.getLocation());
			Vfx.impact(server, block.getLocation(), theme, arc ? 1.4 : 1.0);
			// An Arc splashes: it hits everything within a couple of blocks of where it lands.
			List<Entity> splash = arc ? CastEngine.inRadius(cast, block.getLocation(), 2.0) : List.of();
			CastEngine.onHit(cast, group, new Cast.Hit(splash, block.getLocation(), motion.normalize(), block.getLocation(),
				block.getBlockPos(), block.getDirection(), false), anchored);
			fizzle();
			return;
		}
		setPos(end);
		trail(server, from, end);
	}

	/**
	 * Spell collision: a bolt that meets an enemy caster's bolt in the air. Both burst; different
	 * elements burst harder, and a reacting pair (fire and frost, storm and frost, fire and wind,
	 * void and arcane) sets off a small reaction around the point.
	 */
	private boolean collide(ServerLevel server, Vec3 from, Vec3 to) {
		for (Entity e : server.getEntities(this, new net.minecraft.world.phys.AABB(from, to).inflate(0.9), e -> e instanceof RuneBolt)) {
			RuneBolt other = (RuneBolt) e;
			if (other.cast == null || other.cast.caster == cast.caster || !Targets.canHarm(cast.caster, other.cast.caster)) {
				continue;
			}
			Vec3 at = position().add(other.position()).scale(0.5);
			String mine = element();
			String theirs = other.element();
			String reaction = collisionReaction(mine, theirs);
			LivingEntity player = cast.caster instanceof net.minecraft.server.level.ServerPlayer ? cast.caster : other.cast.caster;
			Cast owner = player == cast.caster ? cast : other.cast;
			double radius = reaction != null ? 4.0 : 2.5;
			double damage = reaction != null ? 8 : mine.equals(theirs) ? 0 : 5;
			Sigils.flash(server, at, 0xFF000000 | color, 1.3F);
			Vfx.radial(server, new net.minecraft.core.particles.DustParticleOptions(color, 1.3F), at, 16, 0.3);
			Vfx.radial(server, new net.minecraft.core.particles.DustParticleOptions(other.color, 1.3F), at, 16, 0.3);
			Vfx.radial(server, ParticleTypes.ELECTRIC_SPARK, at, 12, 0.4);
			Fx.sound(server, at, dev.wildercord.content.WildercordSounds.SHIELD_BREAK, 1.2F, 1.0F);
			Fx.sound(server, at, SoundEvents.GENERIC_EXPLODE, 0.5F, 1.6F);
			if (damage > 0) {
				for (Entity victim : CastEngine.inRadius(owner, at, radius)) {
					if (Targets.canHarm(player, victim)) {
						Effects.hurt(owner, (LivingEntity) victim, server.damageSources().indirectMagic(player, player), damage * owner.power);
					}
				}
			}
			if (reaction != null) {
				Vfx.shockwave(server, at.subtract(0, 0.5, 0), radius, Vfx.theme(mine), 5);
				Reactions.callout(owner, reaction, color);
			}
			Reactions.callout(owner, "collision", 0xFFF0C0);
			Grimoire.feat(player, dev.wildercord.spell.Feats.COLLISION);
			other.fizzle();
			fizzle();
			return true;
		}
		return false;
	}

	private String element() {
		return group == null || group.effects.isEmpty() ? "" : group.effects.getFirst().effect.element();
	}

	/** The reaction two elements set off when their bolts meet, or null. Same elements never react. */
	static String collisionReaction(String a, String b) {
		if (a.equals(b)) {
			return null;
		}
		String pair = a.compareTo(b) < 0 ? a + "+" + b : b + "+" + a;
		return switch (pair) {
			case "fire+frost" -> "shatter";
			case "frost+storm" -> "conduct";
			case "fire+wind" -> "wildfire";
			case "arcane+void" -> "implode";
			case "fire+storm" -> "overload";
			case "earth+frost" -> "fracture";
			case "life+void" -> "blight";
			case "blood+wind" -> "rupture";
			case "fire+time" -> "elapse";
			default -> null;
		};
	}

	private void hitEntity(EntityHitResult result) {
		Entity target = result.getEntity();
		alreadyHit.add(target);
		Vec3 dir = getDeltaMovement().normalize();
		Vfx.impact((ServerLevel) level(), result.getLocation(), theme, arc ? 1.4 : 1.0);
		List<Entity> hits = List.of(target);
		if (arc) {
			// An Arc bursts where it lands, on a creature as on the ground: everything within a couple of blocks.
			List<Entity> splash = new ArrayList<>(hits);
			for (Entity e : CastEngine.inRadius(cast, result.getLocation(), 2.0)) {
				if (e != target) {
					splash.add(e);
				}
			}
			hits = splash;
		}
		CastEngine.onHit(cast, group, new Cast.Hit(hits, result.getLocation(), dir, cast.caster.position(), null, null, false), anchored);
		CastEngine.chain(cast, group, anchored, target, theme);
		if (pierceLeft-- <= 0) {
			fizzle();
		}
	}

	private void steer() {
		Entity target = level().getEntities(this, getBoundingBox().inflate(12.0),
				e -> Targets.canHarm(cast.caster, e) && !alreadyHit.contains(e))
			.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(this))).orElse(null);
		if (target != null) {
			Vec3 want = target.getBoundingBox().getCenter().subtract(position()).normalize();
			Vec3 now = getDeltaMovement().normalize();
			setDeltaMovement(now.lerp(want, 0.25).normalize().scale(speed));
		}
	}

	/** A parried bolt closes on its caster: it turns hard toward them, wherever they run (until they're gone). */
	private void hunt() {
		if (!quarry.isAlive() || quarry.isRemoved() || quarry.level() != level()) {
			quarry = null;
			return;
		}
		Vec3 want = quarry.getBoundingBox().getCenter().subtract(position()).normalize();
		Vec3 now = getDeltaMovement().normalize();
		setDeltaMovement(now.lerp(want, 0.4).normalize().scale(speed));
	}

	private void trail(ServerLevel server, Vec3 from, Vec3 to) {
		// Not right in front of the caster's eyes, where a first-person view sees it as a smear.
		Entity owner = getOwner();
		if (owner != null && to.distanceToSqr(owner.getEyePosition()) < 2.25) {
			return;
		}
		// The comet itself is drawn by each client; here just a few of the element's motes (and a signature's own trail).
		boolean own = dev.wildercord.cast.feel.Feels.travel(server, theme, from, to, tickCount);
		if (!own) {
			return;
		}
		if (tickCount % 2 == 0) {
			Vfx.emit(server, theme.mote(), to, 1, 0.05, 0.01);
		}
	}

	private void fizzle() {
		if (!isRemoved()) {
			discard();
		}
	}

	@Override
	public void onRemoval(RemovalReason reason) {
		// Every removal path (discard, chunk unload, dimension change) ends up here once.
		if (cast != null) {
			LIVE.computeIfPresent(cast.caster.getUUID(), (k, n) -> n <= 1 ? null : n - 1);
			cast = null;
		}
		super.onRemoval(reason);
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return entity instanceof LivingEntity && entity.isAlive() && cast != null && entity != cast.caster && !alreadyHit.contains(entity);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}
}
