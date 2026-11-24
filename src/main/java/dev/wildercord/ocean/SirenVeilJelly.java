package dev.wildercord.ocean;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * Siren-Veil Jelly: translucent violet-indigo medusa concealing an emissive crystal core that
 * contracts and blasts a stinging voltaic shock-ring through dark waters.
 */
public class SirenVeilJelly extends Monster {
	private static final EntityDataAccessor<Integer> PULSE_TICKS = SynchedEntityData.defineId(SirenVeilJelly.class, EntityDataSerializers.INT);
	private int pulseCooldown;

	public SirenVeilJelly(EntityType<? extends SirenVeilJelly> type, Level level) {
		super(type, level);
		this.xpReward = 10;
		this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.8F, 1.0F, false);
		setPathfindingMalus(PathType.WATER, 0.0F);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, OceanRules.SIREN_JELLY_HP)
			.add(Attributes.ATTACK_DAMAGE, OceanRules.SIREN_JELLY_DAMAGE)
			.add(Attributes.MOVEMENT_SPEED, 0.28)
			.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(PULSE_TICKS, 0);
	}

	public int getPulseTicks() {
		return entityData.get(PULSE_TICKS);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
		goalSelector.addGoal(5, new RandomSwimmingGoal(this, 0.8, 40));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new WaterBoundPathNavigation(this, level);
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
			setDeltaMovement(getDeltaMovement().scale(0.92));
		} else {
			super.travel(input);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel server) {
		super.customServerAiStep(server);
		if (pulseCooldown > 0) pulseCooldown--;

		int pulse = getPulseTicks();
		if (pulse > 0) {
			pulse--;
			entityData.set(PULSE_TICKS, pulse);
			if (pulse == 1) {
				releaseShockRing(server);
			}
			return;
		}
		LivingEntity target = getTarget();
		if (target != null && target.isAlive() && pulseCooldown <= 0
			&& distanceToSqr(target) <= OceanRules.SIREN_JELLY_PULSE_RADIUS * OceanRules.SIREN_JELLY_PULSE_RADIUS) {
			entityData.set(PULSE_TICKS, 14);
			pulseCooldown = OceanRules.SIREN_JELLY_PULSE_COOLDOWN;
		}
	}

	private void releaseShockRing(ServerLevel server) {
		playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.3f, 1.45f);
		for (int i = 0; i < 16; i++) {
			double a = i * (Math.PI * 2.0 / 16.0);
			server.sendParticles(ParticleTypes.GLOW,
				getX() + Math.cos(a) * 2.4, getY() + 0.6, getZ() + Math.sin(a) * 2.4,
				1, 0.1, 0.1, 0.1, 0.02);
		}
		for (Player p : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(OceanRules.SIREN_JELLY_PULSE_RADIUS))) {
			if (!p.isAlive() || p.isSpectator()) continue;
			float dmg = 8.0f * OceanRules.abyssalDamageMultiplier(OceanItems.wornSuitPieces(p));
			p.hurt( server.damageSources().indirectMagic(this, this),  dmg);
			p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SQUID_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SQUID_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SQUID_DEATH;
	}
}
