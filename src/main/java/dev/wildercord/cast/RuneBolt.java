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
	private int color;
	private Vfx.Theme theme;
	private final List<Entity> alreadyHit = new ArrayList<>();

	public RuneBolt(EntityType<? extends RuneBolt> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	public static void launch(Cast cast, SpellPlan.Group group, SpellPlan.Link anchored, Vec3 origin, Vec3 dir) {
		launch(cast, group, anchored, origin, dir, false);
	}

	/** Launches a bolt, or with {@code arc} a lobbed one that falls and splashes where it lands. */
	public static void launch(Cast cast, SpellPlan.Group group, SpellPlan.Link anchored, Vec3 origin, Vec3 dir, boolean arc) {
		UUID owner = cast.caster.getUUID();
		if (LIVE.getOrDefault(owner, 0) >= MAX_LIVE_PER_PLAYER) {
			return;
		}
		RuneBolt bolt = new RuneBolt(WildercordEntities.RUNE_BOLT, cast.level);
		bolt.cast = cast.child();
		bolt.group = group;
		bolt.anchored = anchored;
		bolt.pierceLeft = SpellNumbers.pierce(group);
		bolt.bouncesLeft = SpellNumbers.bounces(group);
		bolt.homing = SpellNumbers.homing(group);
		bolt.arc = arc;
		bolt.speed = arc ? SpellNumbers.arcSpeed(group) : SpellNumbers.boltSpeed(group);
		bolt.lifeLeft = arc ? 120 : (int) Math.ceil(RANGE / bolt.speed) + 4;
		bolt.color = CastEngine.colorOf(group);
		bolt.theme = Vfx.theme(group);
		bolt.getEntityData().set(DATA_COLOR, bolt.theme.primary());
		bolt.getEntityData().set(DATA_SECONDARY, bolt.theme.secondary());
		bolt.setOwner(cast.caster);
		bolt.setPos(origin);
		bolt.setDeltaMovement(dir.normalize().scale(bolt.speed));
		LIVE.merge(owner, 1, Integer::sum);
		cast.level.addFreshEntity(bolt);
		Fx.sound(cast.level, origin, bolt.theme.cast(), 0.5F, 1.0F);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_COLOR, 0xFFFFFF);
		builder.define(DATA_SECONDARY, 0xFFFFFF);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel server)) {
			return;
		}
		if (cast == null || !cast.alive() || --lifeLeft <= 0) {
			fizzle();
			return;
		}
		if (homing) {
			steer();
		}
		if (arc) {
			setDeltaMovement(getDeltaMovement().add(0, -0.055, 0));
		}
		Vec3 motion = getDeltaMovement();
		Vec3 from = position();
		Vec3 to = from.add(motion);
		BlockHitResult block = server.clip(new net.minecraft.world.level.ClipContext(from, to,
			net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this));
		Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
		if (collide(server, from, end)) {
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
			default -> null;
		};
	}

	private void hitEntity(EntityHitResult result) {
		Entity target = result.getEntity();
		alreadyHit.add(target);
		Vec3 dir = getDeltaMovement().normalize();
		Vfx.impact((ServerLevel) level(), result.getLocation(), theme, 1.0);
		CastEngine.onHit(cast, group, new Cast.Hit(List.of(target), result.getLocation(), dir, cast.caster.position(), null, null, false), anchored);
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

	private void trail(ServerLevel server, Vec3 from, Vec3 to) {
		// Not right in front of the caster's eyes, where a first-person view sees it as a smear.
		Entity owner = getOwner();
		if (owner != null && to.distanceToSqr(owner.getEyePosition()) < 2.25) {
			return;
		}
		// The comet itself is drawn by each client; here just a few of the element's motes.
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
