package dev.wildercord.ocean;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * Coral-Spine Mako: high-speed open-ocean predator with glowing coral dorsal ridges that
 * telegraphs with a jaw snap before darting forward in a torpedo lunge.
 */
public class CoralSpineMako extends Monster {
	private static final EntityDataAccessor<Integer> LUNGE_TICKS = SynchedEntityData.defineId(CoralSpineMako.class, EntityDataSerializers.INT);
	private int lungeCooldown;

	public CoralSpineMako(EntityType<? extends CoralSpineMako> type, Level level) {
		super(type, level);
		this.xpReward = 14;
		this.moveControl = new SmoothSwimmingMoveControl<>(this, 85, 10, 0.9F, 1.0F, false);
		setPathfindingMalus(PathType.WATER, 0.0F);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, OceanRules.CORAL_MAKO_HP)
			.add(Attributes.ATTACK_DAMAGE, OceanRules.CORAL_MAKO_DAMAGE)
			.add(Attributes.MOVEMENT_SPEED, 0.34)
			.add(Attributes.FOLLOW_RANGE, 30.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(LUNGE_TICKS, 0);
	}

	public int getLungeTicks() {
		return entityData.get(LUNGE_TICKS);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
		goalSelector.addGoal(5, new RandomSwimmingGoal(this, 1.0, 30));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0f));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new WaterBoundPathNavigation(this, level);
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
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
			setDeltaMovement(getDeltaMovement().scale(0.92));
		} else {
			super.travel(input);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel server) {
		super.customServerAiStep(server);
		if (lungeCooldown > 0) lungeCooldown--;

		int lunge = getLungeTicks();
		if (lunge > 0) {
			entityData.set(LUNGE_TICKS, lunge - 1);
			if (isInWater()) {
				server.sendParticles(ParticleTypes.BUBBLE, getX(), getY() + 0.3, getZ(), 2, 0.2, 0.1, 0.2, 0.02);
			}
			return;
		}
		LivingEntity target = getTarget();
		if (target != null && target.isAlive() && isInWater() && lungeCooldown <= 0
			&& distanceToSqr(target) >= 9.0 && distanceToSqr(target) <= 144.0 && hasLineOfSight(target)) {
			Vec3 dir = target.getEyePosition().subtract(getEyePosition()).normalize();
			setDeltaMovement(dir.scale(OceanRules.CORAL_MAKO_LUNGE_SPEED));
			entityData.set(LUNGE_TICKS, 14);
			lungeCooldown = OceanRules.CORAL_MAKO_LUNGE_COOLDOWN;
			playSound(SoundEvents.DOLPHIN_SPLASH, 0.9f, 0.8f);
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.GUARDIAN_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.GUARDIAN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.GUARDIAN_DEATH;
	}
}
