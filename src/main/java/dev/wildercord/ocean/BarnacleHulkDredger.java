package dev.wildercord.ocean;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * Barnacle-Hulk Dredger: towering corroded diving suit revenant overgrown with pink coral and barnacles,
 * wielding an 8-link chained Anchor-Flail for heavy seabed slams and target pulls.
 */
public class BarnacleHulkDredger extends Monster {
	private static final EntityDataAccessor<Integer> SLAM_TICKS = SynchedEntityData.defineId(BarnacleHulkDredger.class, EntityDataSerializers.INT);
	private int slamCooldown;

	public BarnacleHulkDredger(EntityType<? extends BarnacleHulkDredger> type, Level level) {
		super(type, level);
		this.xpReward = 22;
		this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.5F, 1.0F, false);
		setPathfindingMalus(PathType.WATER, 0.0F);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, OceanRules.BARNACLE_HULK_HP)
			.add(Attributes.ARMOR, OceanRules.BARNACLE_HULK_ARMOR)
			.add(Attributes.ATTACK_DAMAGE, OceanRules.BARNACLE_HULK_DAMAGE)
			.add(Attributes.MOVEMENT_SPEED, 0.24)
			.add(Attributes.FOLLOW_RANGE, 28.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.75)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SLAM_TICKS, 0);
	}

	public int getSlamTicks() {
		return entityData.get(SLAM_TICKS);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.05, true));
		goalSelector.addGoal(5, new RandomStrollGoal(this, 0.75, 120));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new AmphibiousPathNavigation(this, level);
	}

	/** 1.21.5 pins canBreatheUnderwater; a steady air supply keeps the tidefolk breathing. */
	@Override
	protected int decreaseAirSupply(int air) {
		return air;
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	public void travel(Vec3 input) {
		if (isEffectiveAi() && isInWater()) {
			moveRelative(getSpeed(), input);
			move(MoverType.SELF, getDeltaMovement());
			Vec3 motion = getDeltaMovement().scale(0.9);
			if (getNavigation().isDone() && !onGround()) {
				motion = motion.add(0.0, -0.025, 0.0);
			}
			setDeltaMovement(motion);
		} else {
			super.travel(input);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel server) {
		super.customServerAiStep(server);
		if (slamCooldown > 0) slamCooldown--;

		int slam = getSlamTicks();
		LivingEntity target = getTarget();
		if (slam > 0) {
			slam--;
			entityData.set(SLAM_TICKS, slam);
			if (slam == 1) {
				performAnchorSlam(server);
			}
			return;
		}
		if (target != null && target.isAlive() && slamCooldown <= 0
			&& distanceToSqr(target) <= 18.0 && distanceToSqr(target) >= 3.5 && hasLineOfSight(target)) {
			entityData.set(SLAM_TICKS, 16);
			slamCooldown = OceanRules.BARNACLE_HULK_SLAM_COOLDOWN;
			playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.1f, 0.65f);
		}
	}

	private void performAnchorSlam(ServerLevel server) {
		playSound(SoundEvents.ANVIL_LAND, 1.0f, 0.55f);
		server.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.5, getZ(), 24, 1.5, 0.4, 1.5, 0.15);
		for (Player p : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(OceanRules.BARNACLE_HULK_SLAM_RADIUS))) {
			if (!p.isAlive() || p.isSpectator()) continue;
			float dmg = 15.0f * OceanRules.abyssalDamageMultiplier(OceanItems.wornSuitPieces(p));
			p.hurt( server.damageSources().mobAttack(this),  dmg);
			double[] pull = OceanRules.computePullVector(p.getX(), p.getY(), p.getZ(), getX(), getY(), getZ(), 0.75);
			p.push(pull[0], 0.22, pull[2]);
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel server, Entity target) {
		boolean hit = super.doHurtTarget(server, target);
		if (hit && target instanceof LivingEntity living && OceanItems.wornSuitPieces(living) >= 4) {
			living.heal((float) (OceanRules.BARNACLE_HULK_DAMAGE * OceanRules.FULL_SUIT_ABYSSAL_DAMAGE_REDUCTION * 0.5f));
		}
		return hit;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.DROWNED_AMBIENT_WATER;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.IRON_GOLEM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.IRON_GOLEM_DEATH;
	}
}
