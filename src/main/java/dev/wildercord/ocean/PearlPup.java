package dev.wildercord.ocean;

import java.util.EnumSet;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * Pearl-Pup (Lantern Seal): tameable spotted amphibious pup carrying a glowing forehead pearl lure
 * that illuminates dark waters and sniffs out sunken treasure chests and barrels for its owner.
 */
public class PearlPup extends TamableAnimal {
	private static final EntityDataAccessor<Boolean> SNIFFING = SynchedEntityData.defineId(PearlPup.class, EntityDataSerializers.BOOLEAN);
	private int sniffTimer = 60;
	private int sniffHighlightTicks;
	private BlockPos detectedChest;

	public PearlPup(EntityType<? extends PearlPup> type, Level level) {
		super(type, level);
		this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.75F, 1.0F, false);
		setPathfindingMalus(PathType.WATER, 0.0F);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createLivingAttributes()
			.add(Attributes.MAX_HEALTH, OceanRules.PEARL_PUP_WILD_HP)
			.add(Attributes.MOVEMENT_SPEED, 0.28);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SNIFFING, false);
	}

	public boolean isSniffing() {
		return entityData.get(SNIFFING);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
		goalSelector.addGoal(2, new SwimFollowOwnerGoal(this, 1.15, 7.0F, 2.5F));
		goalSelector.addGoal(6, new RandomStrollGoal(this, 0.9, 60));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10.0F));
	}

	/** Amphibious-safe follow-owner goal (vanilla FollowOwnerGoal rejects AmphibiousPathNavigation). */
	private static final class SwimFollowOwnerGoal extends Goal {
		private final PearlPup pup;
		private final double speedModifier;
		private final float startDistance;
		private final float stopDistance;
		private int timeToRecalcPath;

		SwimFollowOwnerGoal(PearlPup pup, double speedModifier, float startDistance, float stopDistance) {
			this.pup = pup;
			this.speedModifier = speedModifier;
			this.startDistance = startDistance;
			this.stopDistance = stopDistance;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity owner = pup.getOwner();
			return owner != null && !owner.isSpectator() && !pup.isOrderedToSit()
				&& pup.distanceToSqr(owner) >= (double) (startDistance * startDistance);
		}

		@Override
		public boolean canContinueToUse() {
			LivingEntity owner = pup.getOwner();
			return owner != null && !pup.isOrderedToSit() && !pup.getNavigation().isDone()
				&& pup.distanceToSqr(owner) > (double) (stopDistance * stopDistance);
		}

		@Override
		public void start() {
			timeToRecalcPath = 0;
		}

		@Override
		public void stop() {
			pup.getNavigation().stop();
		}

		@Override
		public void tick() {
			LivingEntity owner = pup.getOwner();
			if (owner == null) return;
			pup.getLookControl().setLookAt(owner, 10.0F, pup.getMaxHeadXRot());
			if (--timeToRecalcPath <= 0) {
				timeToRecalcPath = adjustedTickDelay(10);
				if (pup.distanceToSqr(owner) >= 324.0 && !pup.isLeashed()) {
					pup.moveTo(owner.getX() + 1.5, owner.getY(), owner.getZ() + 1.5, pup.getYRot(), pup.getXRot());
				} else {
					pup.getNavigation().moveTo(owner, speedModifier);
				}
			}
		}
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
			setDeltaMovement(getDeltaMovement().scale(0.9));
		} else {
			super.travel(input);
		}
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(OceanItems.KELP_WRAPPED_SASHIMI);
	}

	@Override
	protected void applyTamingSideEffects() {
		super.applyTamingSideEffects();
		if (getAttribute(Attributes.MAX_HEALTH) != null) {
			getAttribute(Attributes.MAX_HEALTH).setBaseValue(isTame() ? OceanRules.PEARL_PUP_TAMED_HP : OceanRules.PEARL_PUP_WILD_HP);
			setHealth(getMaxHealth());
		}
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (!isTame() && isFood(held)) {
			if (!level().isClientSide()) {
				if (!player.getAbilities().instabuild) held.shrink(1);
				if (random.nextInt(2) == 0) {
					tame(player);
					setPersistenceRequired();
					navigation.stop();
					level().broadcastEntityEvent(this, (byte) 7);
				} else {
					level().broadcastEntityEvent(this, (byte) 6);
				}
			}
			return InteractionResult.SUCCESS;
		}
		if (isTame() && isOwnedBy(player)) {
			if (isFood(held) && getHealth() < getMaxHealth()) {
				if (!level().isClientSide()) {
					if (!player.getAbilities().instabuild) held.shrink(1);
					heal(6.0f);
					level().broadcastEntityEvent(this, (byte) 7);
				}
				return InteractionResult.SUCCESS;
			}
			if (!level().isClientSide()) {
				setOrderedToSit(!isOrderedToSit());
				navigation.stop();
			}
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	@Override
	protected void customServerAiStep(ServerLevel server) {
		super.customServerAiStep(server);
		if (!isTame() || isOrderedToSit()) {
			entityData.set(SNIFFING, false);
			return;
		}
		if (sniffHighlightTicks > 0) {
			sniffHighlightTicks--;
			entityData.set(SNIFFING, true);
			if (detectedChest != null && sniffHighlightTicks % 8 == 0) {
				Vec3 to = Vec3.atCenterOf(detectedChest).subtract(getEyePosition()).normalize();
				server.sendParticles(ParticleTypes.GLOW,
					getX() + to.x * 1.5, getEyeY() + to.y * 1.5, getZ() + to.z * 1.5,
					2, 0.1, 0.1, 0.1, 0.02);
			}
			if (sniffHighlightTicks == 0) entityData.set(SNIFFING, false);
			return;
		}
		if (--sniffTimer <= 0) {
			sniffTimer = OceanRules.PEARL_PUP_SNIFF_INTERVAL;
			detectedChest = findNearestTreasure(server);
			if (detectedChest != null) {
				sniffHighlightTicks = 60;
				playSound(SoundEvents.DOLPHIN_PLAY, 0.8f, 1.4f);
				navigation.moveTo(detectedChest.getX() + 0.5, detectedChest.getY() + 0.5, detectedChest.getZ() + 0.5, 1.15);
			}
		}
	}

	private BlockPos findNearestTreasure(ServerLevel server) {
		BlockPos origin = blockPosition();
		int rChunks = 2;
		BlockPos best = null;
		double bestDist = OceanRules.PEARL_PUP_SNIFF_RADIUS * OceanRules.PEARL_PUP_SNIFF_RADIUS;
		int cx0 = origin.getX() >> 4;
		int cz0 = origin.getZ() >> 4;
		for (int dx = -rChunks; dx <= rChunks; dx++) {
			for (int dz = -rChunks; dz <= rChunks; dz++) {
				if (!server.hasChunk(cx0 + dx, cz0 + dz)) continue;
				LevelChunk chunk = server.getChunk(cx0 + dx, cz0 + dz);
				for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
					BlockEntity be = entry.getValue();
					if (!(be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity)) continue;
					double d = entry.getKey().distSqr(origin);
					if (d < bestDist) {
						bestDist = d;
						best = entry.getKey();
					}
				}
			}
		}
		return best;
	}

	@Override
	public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		PearlPup pup = dev.wildercord.compat.McEntities.create(OceanEntities.PEARL_PUP, level, MobSpawnType.BREEDING);
		if (pup != null && isTame() && getOwner() instanceof Player owner) {
			pup.tame(owner);
		}
		return pup;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.DOLPHIN_AMBIENT;
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
