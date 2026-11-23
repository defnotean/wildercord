package dev.wildercord.ocean;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Lumi-Seahorse: glowing aquamarine reef-coronet underwater steed. Can be tamed, saddled, and ridden
 * in full 3D yaw+pitch swimming with jump-activated Fin-Burst propulsion.
 */
public class LumiSeahorse extends AbstractHorse {
	private static final EntityDataAccessor<Integer> DASH_TICKS = SynchedEntityData.defineId(LumiSeahorse.class, EntityDataSerializers.INT);
	private int dashCooldown;

	public LumiSeahorse(EntityType<? extends LumiSeahorse> type, Level level) {
		super(type, level);
		this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.85F, 1.0F, false);
		setPathfindingMalus(PathType.WATER, 0.0F);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createBaseHorseAttributes()
			.add(Attributes.MAX_HEALTH, OceanRules.LUMI_SEAHORSE_HP)
			.add(Attributes.MOVEMENT_SPEED, OceanRules.LUMI_SEAHORSE_SWIM_SPEED)
			.add(Attributes.JUMP_STRENGTH, 0.85);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DASH_TICKS, 0);
	}

	public int getDashTicks() {
		return entityData.get(DASH_TICKS);
	}

	@Override
	protected void randomizeAttributes(RandomSource random) {
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(OceanRules.LUMI_SEAHORSE_HP + random.nextInt(10));
		getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(OceanRules.LUMI_SEAHORSE_SWIM_SPEED + random.nextDouble() * 0.05);
		getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(0.8 + random.nextDouble() * 0.2);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(4, new RandomSwimmingGoal(this, 0.9, 30) {
			@Override
			public boolean canUse() {
				return !isVehicle() && super.canUse();
			}
		});
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
	public boolean dismountsUnderwater() {
		return false;
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.KELP) || stack.is(Items.SEAGRASS) || stack.is(Items.COD) || stack.is(Items.SEA_PICKLE);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		boolean inventory = !isBaby() && isTamed() && player.isSecondaryUseActive();
		if (!isVehicle() && !inventory) {
			ItemStack stack = player.getItemInHand(hand);
			if (!stack.isEmpty() && isFood(stack)) {
				return fedFood(player, stack);
			}
		}
		return super.mobInteract(player, hand);
	}

	@Override
	protected void tickRidden(Player rider, Vec3 input) {
		super.tickRidden(rider, input);
		setRot(rider.getYRot(), rider.getXRot() * 0.5f);
		yBodyRot = yHeadRot = getYRot();
	}

	@Override
	public void travel(Vec3 input) {
		if (isAlive() && isVehicle() && getControllingPassenger() instanceof Player rider && isInWater()) {
			float strafe = rider.xxa * 0.5f;
			float forward = rider.zza;
			double vertical = 0.0;
			if (Math.abs(forward) > 0.05f) {
				vertical = -Math.sin(Math.toRadians(rider.getXRot())) * forward * 0.35;
			}
			setSpeed((float) getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.35f);
			moveRelative(getSpeed(), new Vec3(strafe, vertical, forward));
			move(MoverType.SELF, getDeltaMovement());
			setDeltaMovement(getDeltaMovement().scale(0.88));
			return;
		}
		if (isEffectiveAi() && isInWater()) {
			moveRelative(getSpeed(), input);
			move(MoverType.SELF, getDeltaMovement());
			setDeltaMovement(getDeltaMovement().scale(0.9));
		} else {
			super.travel(input);
		}
	}

	@Override
	public void onPlayerJump(int jumpPower) {
		if (isInWater() && dashCooldown <= 0 && getControllingPassenger() instanceof Player rider) {
			double[] v = OceanRules.seahorseDashVelocity(rider.getYRot(), rider.getXRot());
			setDeltaMovement(v[0], v[1], v[2]);
			needsSync = true;
			dashCooldown = OceanRules.LUMI_SEAHORSE_DASH_COOLDOWN;
			if (!level().isClientSide()) {
				entityData.set(DASH_TICKS, 14);
				playSound(SoundEvents.DOLPHIN_SPLASH, 1.0f, 1.3f);
				if (level() instanceof ServerLevel server) {
					server.sendParticles(ParticleTypes.GLOW, getX(), getY() + 0.6, getZ(), 14, 0.3, 0.3, 0.3, 0.05);
				}
			}
			return;
		}
		super.onPlayerJump(jumpPower);
	}

	@Override
	protected void customServerAiStep(ServerLevel server) {
		super.customServerAiStep(server);
		if (dashCooldown > 0) dashCooldown--;
		int dt = getDashTicks();
		if (dt > 0) entityData.set(DASH_TICKS, dt - 1);
	}

	@Override
	public boolean canMate(Animal other) {
		return other != this && other instanceof LumiSeahorse mate && canParent() && mate.canParent();
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		LumiSeahorse foal = OceanEntities.LUMI_SEAHORSE.create(level, EntitySpawnReason.BREEDING);
		if (foal != null) {
			setOffspringAttributes(partner, foal);
		}
		return foal;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.DOLPHIN_AMBIENT_WATER;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.DOLPHIN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.DOLPHIN_DEATH;
	}
}
