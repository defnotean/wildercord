package dev.wildercord.monster;

import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Sigils;
import dev.wildercord.cast.Targets;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Thunderwing Harpy: a hunter of the high peaks, storm-blue feathers streaked with lightning-yellow, a beaked crest and
 * talons like hooks. It circles high over its prey, out of a blade's reach; then it hangs a moment and shrieks, wings flung
 * wide (the tell), and dives. A dive that finds nothing ploughs into the ground, and the harpy lies stunned there a few
 * seconds: dodge, then punish. In a thunderstorm it calls lightning too, a ring of light marking where the bolt will fall.
 *
 * <p>Earth magic drags it out of the sky (any earth spell grounds it for a few seconds), and frost stiffens its feathers;
 * storm and wind are its own.</p>
 */
public class ThunderwingHarpy extends WildMonster {
	static final int SHRIEK = 16;
	static final int CALL = 26;
	static final int GROUNDED = 50;
	static final float DIVE_DAMAGE = 6.0F;
	static final float BOLT_DAMAGE = 5.0F;
	static final double DIVE_SPEED = 1.05;
	static final double BOLT_RADIUS = 1.8;
	private static final int STORM = 0xFFE650;
	private static final int STORM_BLUE = 0xA8C8FF;

	private enum Phase { IDLE, CIRCLE, SHRIEK, DIVE, CLIMB, CALL, GROUNDED }

	private Phase phase = Phase.IDLE;
	private long phaseUntil;
	private Vec3 goal;
	private Vec3 home;
	private Vec3 diveDir;
	private Vec3 mark;
	private double angle;
	private long nextCall;
	private int diveTicks;

	public ThunderwingHarpy(EntityType<? extends ThunderwingHarpy> type, Level level) {
		super(type, level);
		this.xpReward = 10;
		setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 22.0)
			.add(Attributes.FLYING_SPEED, 0.6)
			.add(Attributes.MOVEMENT_SPEED, 0.25)
			.add(Attributes.ATTACK_DAMAGE, 4.0)
			.add(Attributes.ARMOR, 1.0)
			.add(Attributes.FOLLOW_RANGE, 40.0);
	}

	@Override
	protected void registerGoals() {
		// It flies by its own steering (below), never by paths: only who it hunts is a goal.
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.BOLT, Runes.SHOCK), List.of(Runes.BOLT, Runes.JOLT), List.of(Runes.CRESCENT, Runes.WINDCUT));
	}

	/** Whether it's in the air (not lying stunned on the ground). */
	public boolean flying() {
		return phase != Phase.GROUNDED;
	}

	/** Whether it's shrieking: the tell before a dive. */
	public boolean shrieking() {
		return phase == Phase.SHRIEK;
	}

	/** Whether it's diving right now. */
	public boolean diving() {
		return phase == Phase.DIVE;
	}

	/** Whether it's lying stunned (after a missed dive, or dragged down by earth). */
	public boolean grounded() {
		return phase == Phase.GROUNDED;
	}

	/** Where the lightning it's calling will fall, or null. */
	public Vec3 lightningMark() {
		return phase == Phase.CALL ? mark : null;
	}

	@Override
	public void travel(Vec3 input) {
		if (flying()) {
			travelFlying(input, 0.02F);
		} else {
			super.travel(input);
		}
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
	}

	@Override
	public boolean onClimbable() {
		return false;
	}

	// ------------------------------------------------------------------ the hunt

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		if (home == null) {
			home = position();
		}
		LivingEntity target = getTarget();
		boolean hunting = target != null && target.isAlive() && Targets.canHarm(this, target);
		switch (phase) {
			case GROUNDED -> {
				setNoGravity(false);
				if (now >= phaseUntil) {
					// Up again in a gust.
					setNoGravity(true);
					setDeltaMovement(getDeltaMovement().add(0, 0.6, 0));
					enter(Phase.CLIMB, now, 18);
					Motes.clouds(level, position(), 5, 0.6, 0xD8E8F0, 0.9, 20, new Vec3(0, 0.03, 0), 0.03, 0.35);
				}
			}
			case IDLE -> {
				if (hunting) {
					enter(Phase.CIRCLE, now, 60 + getRandom().nextInt(40));
					angle = Math.atan2(getZ() - target.getZ(), getX() - target.getX());
				} else {
					if (goal == null || now >= phaseUntil || goal.distanceToSqr(position()) < 2) {
						goal = wanderPoint(level);
						phaseUntil = now + 100 + getRandom().nextInt(80);
					}
					steer(goal, 0.22);
				}
			}
			case CIRCLE -> {
				if (!hunting) {
					enter(Phase.IDLE, now, 0);
					break;
				}
				angle += 0.045;
				goal = target.position().add(Math.cos(angle) * 7.0, 8.0 + Math.sin(now * 0.05) * 1.2, Math.sin(angle) * 7.0);
				steer(goal, 0.45);
				if (now >= phaseUntil && hasLineOfSight(target)) {
					if (level.isThundering() && now >= nextCall && level.canSeeSky(target.blockPosition())) {
						call(level, target, now);
					} else {
						enter(Phase.SHRIEK, now, SHRIEK);
						MonsterMagic.sound(level, position(), "monster_harpy_shriek", 2.2F, 1.0F);
					}
				}
			}
			case SHRIEK -> {
				setDeltaMovement(getDeltaMovement().scale(0.75));
				if (target != null) {
					face(target);
				}
				if (now % 3 == 0) {
					ElementFx.sparks(level, position().add(0, getBbHeight() * 0.6, 0), 3, 0.15);
				}
				if (now >= phaseUntil) {
					if (!hunting) {
						enter(Phase.IDLE, now, 0);
					} else {
						dive(level, target, now);
					}
				}
			}
			case DIVE -> {
				diveTicks++;
				setDeltaMovement(diveDir.scale(DIVE_SPEED));
				if (diveTicks % 2 == 0) {
					Motes.glows(level, position().add(0, getBbHeight() * 0.5, 0), 1, 0.2, STORM_BLUE, 0.14, 8, Vec3.ZERO, 0.0);
				}
				if (target != null && target.isAlive() && getBoundingBox().inflate(0.3).intersects(target.getBoundingBox())) {
					strike(level, target, now);
				} else if (diveTicks > 2 && (horizontalCollision || verticalCollision || onGround())) {
					crash(level, now);
				} else if (diveTicks > 28) {
					enter(Phase.CLIMB, now, 20);
				}
			}
			case CLIMB -> {
				Vec3 up = position().add(getDeltaMovement().multiply(4, 0, 4)).add(0, 5, 0);
				steer(up, 0.5);
				if (now >= phaseUntil) {
					enter(hunting ? Phase.CIRCLE : Phase.IDLE, now, 50 + getRandom().nextInt(40));
				}
			}
			case CALL -> {
				setDeltaMovement(getDeltaMovement().scale(0.75));
				if (target != null) {
					face(target);
				}
				if (now % 4 == 0 && mark != null) {
					ElementFx.arc(level, position().add(0, getBbHeight() * 0.9, 0), position().add(getRandom().nextGaussian() * 0.8, getBbHeight() + 1.2,
						getRandom().nextGaussian() * 0.8), STORM, 0.05, 1, false, 4);
				}
				if (now >= phaseUntil) {
					lightning(level, now);
					enter(hunting ? Phase.CIRCLE : Phase.IDLE, now, 60 + getRandom().nextInt(40));
				}
			}
		}
		setState(WINDUP, phase == Phase.SHRIEK);
		setState(ACTING, phase == Phase.DIVE);
		setState(STUNNED, phase == Phase.GROUNDED);
		setState(ALT, phase == Phase.CALL);
		if (phase != Phase.SHRIEK && phase != Phase.CALL && phase != Phase.GROUNDED) {
			Vec3 v = getDeltaMovement();
			if (v.x * v.x + v.z * v.z > 1.0E-3) {
				float yaw = (float) Math.toDegrees(Math.atan2(-v.x, v.z));
				setYRot(yaw);
				setYBodyRot(yaw);
				setYHeadRot(yaw);
			}
		}
	}

	private void enter(Phase next, long now, int ticks) {
		phase = next;
		phaseUntil = now + ticks;
	}

	/** Eases toward {@code at} at up to {@code speed} blocks a tick, slowing as it arrives. */
	private void steer(Vec3 at, double speed) {
		Vec3 want = at.subtract(position());
		double length = want.length();
		want = length < 0.4 ? Vec3.ZERO : want.scale(speed * Math.min(1, length / 3.0) / length);
		Vec3 v = getDeltaMovement();
		setDeltaMovement(v.add(want.subtract(v).scale(0.18)));
	}

	private Vec3 wanderPoint(ServerLevel level) {
		double x = home.x + (getRandom().nextDouble() - 0.5) * 24;
		double z = home.z + (getRandom().nextDouble() - 0.5) * 24;
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(x, 0, z).getX(), BlockPos.containing(x, 0, z).getZ());
		return new Vec3(x, Math.max(ground + 5 + getRandom().nextDouble() * 6, level.getMinY() + 4), z);
	}

	/** Folds its wings and stoops at where the target is (and where it's heading), in a straight line. */
	private void dive(ServerLevel level, LivingEntity target, long now) {
		Vec3 aim = target.getBoundingBox().getCenter().add(target.getDeltaMovement().multiply(4, 0, 4));
		Vec3 d = aim.subtract(position().add(0, getBbHeight() * 0.5, 0));
		diveDir = d.lengthSqr() < 1.0E-4 ? new Vec3(0, -1, 0) : d.normalize();
		diveTicks = 0;
		enter(Phase.DIVE, now, 40);
		MonsterMagic.sound(level, position(), "monster_harpy_dive", 1.4F, 1.0F);
	}

	private void strike(ServerLevel level, LivingEntity target, long now) {
		if (target.hurtServer(level, damageSources().mobAttack(this), DIVE_DAMAGE)) {
			MonsterMagic.knock(target, position(), 0.8);
			ElementFx.slash(level, target.getBoundingBox().getCenter(), new Vec3(0, 1, 0), diveDir, STORM_BLUE, 0.9, 1.4, 0.1, 3, 8);
		}
		ElementFx.sparks(level, target.getBoundingBox().getCenter(), 6, 0.2);
		setDeltaMovement(diveDir.multiply(0.3, 0, 0.3).add(0, 0.5, 0));
		enter(Phase.CLIMB, now, 22);
	}

	/** A dive that met the ground instead of its prey: it tumbles and lies stunned. */
	private void crash(ServerLevel level, long now) {
		ground(level, GROUNDED);
		ElementFx.earthImpact(level, position(), 0.6);
		MonsterMagic.sound(level, position(), "monster_harpy_hurt", 1.0F, 0.8F);
	}

	/** Brought down for {@code ticks}: it can't fly, and lies open to blades. */
	public void ground(ServerLevel level, int ticks) {
		long now = level.getGameTime();
		int left = phase == Phase.GROUNDED ? (int) Math.max(0, phaseUntil - now) : 0;
		enter(Phase.GROUNDED, now, Math.max(ticks, left));
		setNoGravity(false);
		setDeltaMovement(getDeltaMovement().multiply(0.2, 0, 0.2).add(0, -0.3, 0));
		setState(STUNNED, true);
		Motes.clouds(level, position(), 4, 0.5, 0xB4A48A, 0.8, 20, new Vec3(0, 0.02, 0), 0.02, 0.35);
	}

	/** In a storm: it raises its wings and marks the ground under its prey; the bolt falls there when the call ends. */
	private void call(ServerLevel level, LivingEntity target, long now) {
		nextCall = now + 160 + getRandom().nextInt(80);
		BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, target.blockPosition());
		mark = new Vec3(target.getX(), Math.min(target.getY(), ground.getY()), target.getZ());
		enter(Phase.CALL, now, CALL);
		Sigils.target(level, mark, STORM, (float) BOLT_RADIUS, CALL + 4);
		MonsterMagic.sound(level, position(), "monster_harpy_shriek", 1.6F, 1.25F);
	}

	private void lightning(ServerLevel level, long now) {
		if (mark == null) {
			return;
		}
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			// The flash and the thunder only: the harm is the harpy's own, through every spell defence.
			bolt.setVisualOnly(true);
			bolt.snapTo(mark.x, mark.y, mark.z);
			level.addFreshEntity(bolt);
		}
		AABB area = new AABB(mark, mark).inflate(BOLT_RADIUS, 2.5, BOLT_RADIUS);
		for (Entity e : level.getEntities(this, area, e -> Targets.canHarm(this, e))) {
			if (e.position().distanceTo(mark) <= BOLT_RADIUS + 0.5) {
				MonsterMagic.hurt(level, this, (LivingEntity) e, BOLT_DAMAGE);
			}
		}
		ElementFx.stormImpact(level, mark.add(0, 0.2, 0), 1.2);
		mark = null;
	}

	// ------------------------------------------------------------------ sounds

	@Override
	protected SoundEvent getAmbientSound() {
		return MonsterMagic.kit("monster_harpy_ambient");
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return MonsterMagic.kit("monster_harpy_hurt");
	}

	@Override
	protected SoundEvent getDeathSound() {
		return MonsterMagic.kit("monster_harpy_death");
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState block) {
	}

	/** For the tests: starts the shriek at once, as if its circling were over. */
	public void shriekNow(ServerLevel level) {
		enter(Phase.SHRIEK, level.getGameTime(), SHRIEK);
		MonsterMagic.sound(level, position(), "monster_harpy_shriek", 2.2F, 1.0F);
	}

	/** For the tests: starts a lightning call at once, storm or not. */
	public void callNow(ServerLevel level, LivingEntity target) {
		call(level, target, level.getGameTime());
	}
}
