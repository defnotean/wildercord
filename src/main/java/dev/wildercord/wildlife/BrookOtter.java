package dev.wildercord.wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.UUID;

/**
 * A brook otter: a sleek brown otter of the rivers and swamps, cream at the throat, with a thick tapering tail. It swims fast and
 * low, and now and then rolls onto its back to float at the surface, paws folded on its chest. Floating, it brings up a shell and
 * knocks it on a stone on its belly; two floating otters drift together, side by side, and hold paws so they don't float apart; a
 * pup climbs onto a floating parent's belly and rides there. It hunts cod and salmon and floats on its back to eat them. Ashore it
 * sometimes sits up and juggles a pebble from paw to paw.
 * <p>Raw cod or salmon tempts it and breeds it. Feed one and it trusts you a while: it follows you about, and now and then drops a
 * little gift at your feet.
 */
public class BrookOtter extends Animal {
	/** What it is doing ({@link OtterRules#IDLE} and so on), how long that has to go, what it holds, and which paw it holds out. */
	private static final EntityDataAccessor<Integer> POSE = SynchedEntityData.defineId(BrookOtter.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> LEFT = SynchedEntityData.defineId(BrookOtter.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> HELD = SynchedEntityData.defineId(BrookOtter.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> RIGHT = SynchedEntityData.defineId(BrookOtter.class, EntityDataSerializers.BOOLEAN);

	private @Nullable UUID truster;
	private long fedAt = -1, huntedAt = -OtterRules.HUNT_REST, giftedAt;
	/** Holding paws: the other otter, and whether this one leads (the other drifts at its side). */
	private @Nullable BrookOtter partner;
	private boolean leads;

	/** Client: how far it lies on its back, sits up, swims, knocks or eats, and holds out a paw, eased, this tick's and last. */
	public float back, backO, sit, sitO, swim, swimO, crack, crackO, hold, holdO;

	public BrookOtter(EntityType<? extends BrookOtter> type, Level level) {
		super(type, level);
		moveControl = new SmoothSwimmingMoveControl<>(this, 85, 10, 0.55F, 1.0F, false);
		setPathfindingMalus(PathType.WATER, 0);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.TEMPT_RANGE, 10);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new AmphibiousPathNavigation(this, level);
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	/** In the water it swims, not wades: the swimming control's push, without the drag a land animal feels. */
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
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(POSE, OtterRules.IDLE);
		builder.define(LEFT, 0);
		builder.define(HELD, OtterRules.SHELL);
		builder.define(RIGHT, true);
	}

	public int pose() {
		return entityData.get(POSE);
	}

	public int poseLeft() {
		return entityData.get(LEFT);
	}

	public int held() {
		return entityData.get(HELD);
	}

	/** Holding paws: whether the other otter floats at its right (so it reaches with its right paw). */
	public boolean reachesRight() {
		return entityData.get(RIGHT);
	}

	/** Lying on its back at the surface: floating, knocking a shell or eating, or holding paws. */
	public boolean floating() {
		int pose = pose();
		return (pose == OtterRules.FLOAT || pose == OtterRules.CRACK || pose == OtterRules.HOLD) && isInWater();
	}

	private void pose(int pose, int ticks) {
		entityData.set(POSE, pose);
		entityData.set(LEFT, ticks);
		if (pose != OtterRules.IDLE) {
			getNavigation().stop();
		}
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new Posed());
		goalSelector.addGoal(1, new PanicGoal(this, 1.5));
		goalSelector.addGoal(2, new BreedGoal(this, 1.0));
		goalSelector.addGoal(3, new TemptGoal(this, 1.1, this::isFood, false));
		goalSelector.addGoal(4, new Hunt());
		goalSelector.addGoal(5, new RideParent());
		goalSelector.addGoal(6, new FollowTruster());
		goalSelector.addGoal(7, new FollowParentGoal(this, 1.2));
		goalSelector.addGoal(8, new ToWater());
		goalSelector.addGoal(9, new RandomSwimmingGoal(this, 1.0, 30));
		goalSelector.addGoal(10, new RandomStrollGoal(this, 1.0));
		goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(12, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.COD) || stack.is(Items.SALMON);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			int pose = pose();
			boolean lying = floating() || isPassenger();
			backO = back;
			back = WildlifeRules.approach(back, lying ? 1 : 0, 0.12F);
			sitO = sit;
			sit = WildlifeRules.approach(sit, !lying && (pose == OtterRules.JUGGLE || pose == OtterRules.CRACK) ? 1 : 0, 0.15F);
			swimO = swim;
			swim = WildlifeRules.approach(swim, isInWater() && !lying ? 1 : 0, 0.15F);
			crackO = crack;
			crack = WildlifeRules.approach(crack, pose == OtterRules.CRACK ? 1 : 0, 0.2F);
			holdO = hold;
			hold = WildlifeRules.approach(hold, pose == OtterRules.HOLD ? 1 : 0, 0.1F);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		if (isPassenger()) {
			// A pup rides until it has grown, or now and then slips off for a swim of its own.
			if (!isBaby() || random.nextInt(900) == 0) {
				stopRiding();
			}
			return;
		}
		if (isVehicle() && !floating()) {
			ejectPassengers();
		}
		gift(level, now);
		int pose = pose(), left = poseLeft();
		if (pose != OtterRules.IDLE) {
			if (pose == OtterRules.CRACK) {
				knock(level, left);
			} else if (pose == OtterRules.JUGGLE && left % 12 == 0) {
				playSound(SoundEvents.STONE_HIT, 0.2F, 1.9F);
			} else if (pose == OtterRules.FLOAT && tickCount % 20 == 0) {
				seekHand(level);
				if (pose() == OtterRules.FLOAT && random.nextInt(OtterRules.CRACK_ODDS / 20) == 0) {
					entityData.set(HELD, OtterRules.SHELL);
					pose(OtterRules.CRACK, OtterRules.CRACK_TICKS);
					return;
				}
			}
			if (pose() == OtterRules.HOLD && !stillHolding()) {
				letGo();
				return;
			}
			// Floating ends as soon as it is out of the water; a meal or a shell done afloat, it floats on a while.
			boolean afloat = pose == OtterRules.FLOAT || pose == OtterRules.HOLD || pose == OtterRules.CRACK && held() == OtterRules.SHELL;
			if (left <= 1 || afloat && !isInWater()) {
				if (pose == OtterRules.HOLD) {
					letGo();
				} else if (pose == OtterRules.CRACK && left <= 1 && isInWater()) {
					pose(OtterRules.FLOAT, OtterRules.FLOAT_TICKS / 3);
				} else {
					pose(OtterRules.IDLE, 0);
				}
			} else {
				entityData.set(LEFT, left - 1);
			}
			return;
		}
		if (!getNavigation().isDone()) {
			return;
		}
		if (OtterRules.floats(isInWater(), pose) && random.nextInt(OtterRules.FLOAT_ODDS) == 0) {
			pose(OtterRules.FLOAT, OtterRules.FLOAT_TICKS / 2 + random.nextInt(OtterRules.FLOAT_TICKS));
			playSound(SoundEvents.GENERIC_SPLASH, 0.3F, 1.5F);
		} else if (!isInWater() && onGround() && !isBaby() && random.nextInt(OtterRules.JUGGLE_ODDS) == 0) {
			pose(OtterRules.JUGGLE, OtterRules.JUGGLE_TICKS);
		}
	}

	/** Knocking a shell on the stone on its belly, or eating what it caught: a knock and a splash at each beat, or a munch. */
	private void knock(ServerLevel level, int left) {
		if (held() == OtterRules.SHELL) {
			if (OtterRules.knocks(left)) {
				playSound(SoundEvents.STONE_HIT, 0.45F, 1.5F + random.nextFloat() * 0.3F);
				level.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.5, getZ(), 3, 0.15, 0.05, 0.15, 0.05);
			}
			if (left == 8) {
				// Cracked: it slurps what was inside.
				playSound(SoundEvents.FOX_EAT, 0.6F, 1.5F);
			}
		} else if (left % 15 == 0) {
			playSound(SoundEvents.FOX_EAT, 0.6F, 1.4F);
			level.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.5, getZ(), 2, 0.1, 0.05, 0.1, 0.03);
		}
	}

	// ------------------------------------------------------------------ holding paws

	/** Floating alone, it looks for another otter floating alone nearby to drift over to and take by the paw. */
	private void seekHand(ServerLevel level) {
		if (partner != null || isBaby()) {
			return;
		}
		BrookOtter other = level.getEntitiesOfClass(BrookOtter.class, getBoundingBox().inflate(OtterRules.HOLD_RANGE),
				o -> o != this && o.partner == null && o.isInWater() && OtterRules.holds(pose(), o.pose(), o.isBaby(), distanceTo(o)))
			.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
		if (other == null) {
			return;
		}
		boolean right = OtterRules.partnerOnRight(other.position().subtract(position()).dot(right(getYRot())));
		partner = other;
		leads = true;
		other.partner = this;
		other.leads = false;
		entityData.set(RIGHT, right);
		other.entityData.set(RIGHT, !right);
		pose(OtterRules.HOLD, OtterRules.HOLD_TICKS);
		other.pose(OtterRules.HOLD, OtterRules.HOLD_TICKS);
		playSound(SoundEvents.FOX_AMBIENT, 0.4F, 1.8F);
	}

	private boolean stillHolding() {
		BrookOtter other = partner;
		return other != null && other.isAlive() && other.partner == this && other.pose() == OtterRules.HOLD && isInWater()
			&& distanceTo(other) < OtterRules.HOLD_RANGE * 1.5;
	}

	/** They let go, and float on a little while, each alone. */
	private void letGo() {
		BrookOtter other = partner;
		partner = null;
		if (other != null && other.partner == this) {
			other.partner = null;
			if (other.pose() == OtterRules.HOLD) {
				other.pose(other.isInWater() ? OtterRules.FLOAT : OtterRules.IDLE, 60);
			}
		}
		if (pose() == OtterRules.HOLD) {
			pose(isInWater() ? OtterRules.FLOAT : OtterRules.IDLE, 60);
		}
	}

	/** Its right, for a yaw in degrees. */
	private static Vec3 right(float yaw) {
		float r = yaw * Mth.DEG_TO_RAD;
		return new Vec3(-Mth.cos(r), 0, -Mth.sin(r));
	}

	/** Two otters holding paws shouldn't shove each other apart. */
	@Override
	protected void doPush(Entity entity) {
		if (entity == partner) {
			return;
		}
		super.doPush(entity);
	}

	// ------------------------------------------------------------------ a pup riding on its belly

	@Override
	protected boolean canAddPassenger(Entity entity) {
		return entity instanceof BrookOtter pup && pup.isBaby() && getPassengers().isEmpty();
	}

	/** The pup rides; it doesn't steer. */
	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return null;
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		return new Vec3(0, dimensions.height() * 0.72, 0);
	}

	@Override
	protected void positionRider(Entity passenger, Entity.MoveFunction move) {
		super.positionRider(passenger, move);
		passenger.setYRot(getYRot());
		if (passenger instanceof LivingEntity living) {
			living.yBodyRot = yBodyRot;
			living.setYHeadRot(yBodyRot);
		}
	}

	// ------------------------------------------------------------------ trust and gifts

	private @Nullable Player truster() {
		if (truster == null || !OtterRules.trusts(level().getGameTime() - fedAt)) {
			return null;
		}
		Player player = level().getPlayerByUUID(truster);
		return player != null && player.isAlive() && !player.isSpectator() ? player : null;
	}

	/** Trusting someone and near them, now and then it drops a little something it found at their feet. */
	private void gift(ServerLevel level, long now) {
		if (tickCount % 40 != 0 || isBaby()) {
			return;
		}
		Player player = truster();
		if (player == null || !OtterRules.gifts(true, distanceTo(player), now - giftedAt) || random.nextInt(3) != 0) {
			return;
		}
		giftedAt = now;
		ItemStack gift = switch (OtterRules.gift(random.nextInt(100))) {
			case 0 -> new ItemStack(Items.CLAY_BALL, 1 + random.nextInt(2));
			case 1 -> new ItemStack(Items.KELP);
			case 2 -> new ItemStack(Items.FLINT);
			default -> new ItemStack(Items.GOLD_NUGGET);
		};
		getLookControl().setLookAt(player, 30, 30);
		spawnAtLocation(level, gift);
		playSound(SoundEvents.FOX_SPIT, 0.6F, 1.6F);
		level.broadcastEntityEvent(this, (byte) 18);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hit = super.hurtServer(level, source, amount);
		if (hit) {
			letGo();
			ejectPassengers();
			if (pose() != OtterRules.IDLE) {
				pose(OtterRules.IDLE, 0);
			}
		}
		return hit;
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (!isFood(held)) {
			return super.mobInteract(player, hand);
		}
		InteractionResult result = super.mobInteract(player, hand);
		if (!level().isClientSide()) {
			if (!result.consumesAction()) {
				held.consume(1, player);
			}
			// Fed, it trusts you: hearts, and a happy chirp; its first gift comes a little sooner.
			truster = player.getUUID();
			fedAt = level().getGameTime();
			giftedAt = fedAt - OtterRules.GIFT_REST / 2;
			level().broadcastEntityEvent(this, (byte) 18);
			playSound(SoundEvents.FOX_EAT, 0.7F, 1.5F);
		}
		return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}

	// ------------------------------------------------------------------ breeding: a pup

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return getType().create(level, EntitySpawnReason.BREEDING) instanceof BrookOtter pup ? pup : null;
	}

	// ------------------------------------------------------------------ sound: chirps and squeaks

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return SoundEvents.FOX_AMBIENT;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.FOX_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return SoundEvents.FOX_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 1.45F;
	}

	@Override
	protected float getSoundVolume() {
		return 0.5F;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 200;
	}

	/**
	 * Floating, knocking, holding paws or juggling: it keeps to where it is. Afloat it bobs at the surface, its heading held still;
	 * the one led drifts to its partner's side, turned the same way.
	 */
	private final class Posed extends Goal {
		private float yaw;

		Posed() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return pose() != OtterRules.IDLE;
		}

		@Override
		public void start() {
			getNavigation().stop();
			yaw = getYRot();
		}

		@Override
		public void tick() {
			Vec3 v = getDeltaMovement();
			BrookOtter other = partner;
			if (other != null && !leads && pose() == OtterRules.HOLD) {
				// At its partner's side, turned the same way.
				yaw = other.getYRot();
				Vec3 to = other.position().add(right(yaw).scale((other.reachesRight() ? 1 : -1) * OtterRules.HOLD_GAP)).subtract(position());
				v = new Vec3(to.x * 0.3, v.y, to.z * 0.3);
			} else {
				// Drifting a little, slowly turning.
				yaw += Mth.sin(tickCount * 0.013F + getId()) * 0.15F;
				v = new Vec3(v.x * 0.6, v.y, v.z * 0.6);
			}
			setYRot(yaw);
			yBodyRot = yaw;
			setYHeadRot(yaw);
			if (isInWater()) {
				// Bob at the surface, belly up.
				double depth = getFluidHeight(FluidTags.WATER);
				v = new Vec3(v.x, Mth.clamp((depth - 0.4) * 0.12, -0.04, 0.05) + Mth.sin(tickCount * 0.12F) * 0.002, v.z);
			} else {
				v = new Vec3(0, v.y, 0);
			}
			setDeltaMovement(v);
		}
	}

	/** A hunt: it darts after a cod or a salmon, catches it, then floats on its back to eat it. */
	private final class Hunt extends Goal {
		private @Nullable AbstractFish fish;
		private int ticks;

		Hunt() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (pose() != OtterRules.IDLE || tickCount % 20 != 0 || isPassenger() || !(level() instanceof ServerLevel level)
				|| !OtterRules.hunts(isBaby(), 0, level.getGameTime() - huntedAt)) {
				return false;
			}
			fish = level.getEntitiesOfClass(AbstractFish.class, getBoundingBox().inflate(OtterRules.HUNT_RANGE),
					f -> (f instanceof Cod || f instanceof Salmon) && f.isAlive())
				.stream().min(Comparator.comparingDouble(BrookOtter.this::distanceToSqr)).orElse(null);
			return fish != null && OtterRules.hunts(isBaby(), distanceTo(fish), level.getGameTime() - huntedAt);
		}

		@Override
		public boolean canContinueToUse() {
			return fish != null && fish.isAlive() && ticks < 300 && distanceTo(fish) < OtterRules.HUNT_RANGE * 1.5 && pose() == OtterRules.IDLE;
		}

		@Override
		public void start() {
			ticks = 0;
		}

		@Override
		public void stop() {
			huntedAt = level().getGameTime();
			fish = null;
			getNavigation().stop();
		}

		@Override
		public void tick() {
			if (fish == null || !(level() instanceof ServerLevel level)) {
				return;
			}
			ticks++;
			getLookControl().setLookAt(fish, 30, 30);
			if (distanceTo(fish) < 1.3) {
				fish.hurtServer(level, damageSources().mobAttack(BrookOtter.this), OtterRules.BITE);
				playSound(SoundEvents.FOX_BITE, 0.7F, 1.4F);
				level.sendParticles(ParticleTypes.BUBBLE, fish.getX(), fish.getY(), fish.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
				fish = null;
				entityData.set(HELD, OtterRules.FISH);
				pose(OtterRules.CRACK, OtterRules.CRACK_TICKS);
			} else if (ticks % 5 == 1) {
				getNavigation().moveTo(fish, 1.6);
			}
		}
	}

	/** A pup in the water climbs onto a floating parent's belly to ride. */
	private final class RideParent extends Goal {
		@Override
		public boolean canUse() {
			if (!isBaby() || isPassenger() || tickCount % 20 != 0 || !isInWater()) {
				return false;
			}
			BrookOtter parent = level().getEntitiesOfClass(BrookOtter.class, getBoundingBox().inflate(2.5),
				o -> !o.isBaby() && o.floating() && !o.isVehicle()).stream().findFirst().orElse(null);
			if (parent != null) {
				startRiding(parent);
				if (isPassenger()) {
					playSound(SoundEvents.GENERIC_SPLASH, 0.2F, 1.8F);
				}
			}
			return false;
		}
	}

	/** Ashore a while, it makes for the water. */
	private final class ToWater extends Goal {
		private @Nullable BlockPos water;

		ToWater() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			if (isInWater() || pose() != OtterRules.IDLE || random.nextInt(240) != 0) {
				return false;
			}
			BlockPos here = blockPosition();
			for (int i = 0; i < 40; i++) {
				BlockPos at = here.offset(random.nextInt(17) - 8, random.nextInt(5) - 3, random.nextInt(17) - 8);
				if (level().getFluidState(at).is(FluidTags.WATER) && level().getBlockState(at.above()).isAir()) {
					water = at;
					return true;
				}
			}
			return false;
		}

		@Override
		public void start() {
			if (water != null) {
				getNavigation().moveTo(water.getX() + 0.5, water.getY(), water.getZ() + 0.5, 1.0);
			}
		}

		@Override
		public boolean canContinueToUse() {
			return !isInWater() && !getNavigation().isDone();
		}

		@Override
		public void stop() {
			water = null;
		}
	}

	/** Trusting someone, it follows them about. */
	private final class FollowTruster extends Goal {
		private @Nullable Player player;

		FollowTruster() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			player = truster();
			return player != null && !isBaby() && pose() == OtterRules.IDLE && distanceTo(player) > OtterRules.FOLLOW_FAR && distanceTo(player) < 24;
		}

		@Override
		public boolean canContinueToUse() {
			return player != null && truster() == player && pose() == OtterRules.IDLE && distanceTo(player) > OtterRules.FOLLOW_NEAR
				&& distanceTo(player) < 24;
		}

		@Override
		public void stop() {
			player = null;
			getNavigation().stop();
		}

		@Override
		public void tick() {
			if (player == null) {
				return;
			}
			getLookControl().setLookAt(player, 30, 30);
			if (tickCount % 10 == 0) {
				getNavigation().moveTo(player, 1.25);
			}
		}
	}
}
