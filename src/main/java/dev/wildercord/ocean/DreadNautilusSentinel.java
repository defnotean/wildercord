package dev.wildercord.ocean;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
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
 * Dread-Nautilus Sentinel: ancient bronze-armored chambered sentinel patrolling abyssal ruins.
 * Rear/flank strikes deflect off its coiled shell; its recessed glowing eye charges a cyan beam.
 */
public class DreadNautilusSentinel extends Monster {
	private static final EntityDataAccessor<Integer> LASER_TICKS = SynchedEntityData.defineId(DreadNautilusSentinel.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(DreadNautilusSentinel.class, EntityDataSerializers.INT);
	private int cooldown;

	public DreadNautilusSentinel(EntityType<? extends DreadNautilusSentinel> type, Level level) {
		super(type, level);
		this.xpReward = 18;
		this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.65F, 1.0F, false);
		setPathfindingMalus(PathType.WATER, 0.0F);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, OceanRules.DREAD_NAUTILUS_HP)
			.add(Attributes.ARMOR, OceanRules.DREAD_NAUTILUS_ARMOR)
			.add(Attributes.ATTACK_DAMAGE, 7.0)
			.add(Attributes.MOVEMENT_SPEED, 0.23)
			.add(Attributes.FOLLOW_RANGE, 28.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.85);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(LASER_TICKS, 0);
		builder.define(TARGET_ID, -1);
	}

	public int getLaserTicks() {
		return entityData.get(LASER_TICKS);
	}

	public int getBeamTargetId() {
		return entityData.get(TARGET_ID);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(5, new RandomSwimmingGoal(this, 0.7, 50));
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
			setDeltaMovement(getDeltaMovement().scale(0.9));
		} else {
			super.travel(input);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel server, DamageSource source, float amount) {
		Entity attacker = source.getEntity();
		if (attacker != null && !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
			Vec3 toAttacker = attacker.position().subtract(position()).normalize();
			boolean fromFront = getLookAngle().dot(toAttacker) > 0.1;
			if (!fromFront) {
				amount *= OceanRules.nautilusShellDamageScale(false);
				playSound(SoundEvents.SHIELD_BLOCK.value(), 0.9f, 0.8f);
			}
		}
		return super.hurtServer(server, source, amount);
	}

	@Override
	protected void customServerAiStep(ServerLevel server) {
		super.customServerAiStep(server);
		if (cooldown > 0) cooldown--;

		LivingEntity target = getTarget();
		if (target == null || !target.isAlive() || distanceToSqr(target) > 22.0 * 22.0 || !hasLineOfSight(target)) {
			entityData.set(LASER_TICKS, 0);
			entityData.set(TARGET_ID, -1);
			return;
		}
		getLookControl().setLookAt(target, 30.0f, 30.0f);
		entityData.set(TARGET_ID, target.getId());

		if (cooldown > 0) return;

		int laser = getLaserTicks() + 1;
		entityData.set(LASER_TICKS, laser);

		int totalTicks = OceanRules.DREAD_NAUTILUS_CHARGE_TICKS + OceanRules.DREAD_NAUTILUS_FIRE_TICKS;
		if (laser == 1) {
			playSound(SoundEvents.GUARDIAN_ATTACK, 1.0f, 0.7f);
		} else if (laser > OceanRules.DREAD_NAUTILUS_CHARGE_TICKS) {
			if (laser % 5 == 0) {
				Vec3 from = getEyePosition();
				Vec3 to = target.getEyePosition();
				for (int step = 1; step <= 6; step++) {
					Vec3 p = from.lerp(to, step / 6.0);
					server.sendParticles(ParticleTypes.GLOW, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
				}
			}
			if (laser % 10 == 0) {
				float dmg = OceanRules.DREAD_NAUTILUS_LASER_DAMAGE * OceanRules.abyssalDamageMultiplier(OceanItems.wornSuitPieces(target));
				target.hurtServer(server, server.damageSources().indirectMagic(this, this), dmg);
			}
		}

		if (laser >= totalTicks) {
			entityData.set(LASER_TICKS, 0);
			cooldown = 40;
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ELDER_GUARDIAN_AMBIENT_LAND;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.GUARDIAN_HURT_LAND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ELDER_GUARDIAN_DEATH;
	}
}
