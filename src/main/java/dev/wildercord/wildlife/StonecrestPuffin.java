package dev.wildercord.wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A stonecrest puffin: a stout little seabird of the stony shores, black above and white below, with a white face, an orange-red
 * striped bill far too big for it and bright orange feet. It waddles about the rocks with its wings held out a little for
 * balance, stretches them with a quick flapping now and then, preens, and bill-taps with its mate, the two of them rattling
 * their bills together. It takes off in a whirring flurry for short hops out to the water and back, and comes down slow, its
 * wings a blur. Afloat it bobs on the swell and dives, flying down under the water with its wings, and two times in three it
 * pops back up with a row of little silver fish crosswise in its bill: those go to a puffling if there is one about, and
 * otherwise it eats them itself. At night it sleeps with its head tucked back, ashore or afloat.
 * <p>Raw cod or salmon tempts it and breeds it. Feed one and it trusts you a while: it waddles along after you, and the next fish
 * it catches it brings back and drops at your feet.
 */
public class StonecrestPuffin extends Animal {
	/** What it is doing ({@link PuffinRules#IDLE} and so on), how long that has to go, and whether it has fish in its bill. */
	private static final EntityDataAccessor<Integer> POSE = SynchedEntityData.defineId(StonecrestPuffin.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> LEFT = SynchedEntityData.defineId(StonecrestPuffin.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> FISH = SynchedEntityData.defineId(StonecrestPuffin.class, EntityDataSerializers.BOOLEAN);

	private @Nullable UUID truster;
	private long fedAt = -1;
	private boolean gifted;
	private int carryLeft;
	private @Nullable StonecrestPuffin mate;

	/**
	 * Client: afloat, diving, sleeping, preening, bill-tapping and eating, eased, this tick's and last; how hard its wings beat and
	 * where they are in the beat.
	 */
	public float swim, swimO, dive, diveO, sleep, sleepO, preen, preenO, tap, tapO, munch, munchO, flap, flapO, beat, beatO;

	public StonecrestPuffin(EntityType<? extends StonecrestPuffin> type, Level level) {
		super(type, level);
		// It is as happy afloat as ashore: it wanders out over the water as readily as over the rocks.
		setPathfindingMalus(PathType.WATER, 0.0F);
		getNavigation().setCanFloat(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.22);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(POSE, PuffinRules.IDLE);
		builder.define(LEFT, 0);
		builder.define(FISH, false);
	}

	public int pose() {
		return entityData.get(POSE);
	}

	public int poseLeft() {
		return entityData.get(LEFT);
	}

	/** Whether it has fish in its bill. */
	public boolean carrying() {
		return entityData.get(FISH);
	}

	private void carrying(boolean fish) {
		entityData.set(FISH, fish);
		carryLeft = fish ? PuffinRules.CARRY_TICKS : 0;
	}

	private void pose(int pose, int ticks) {
		entityData.set(POSE, pose);
		entityData.set(LEFT, ticks);
		if (pose != PuffinRules.IDLE && pose != PuffinRules.FLY) {
			getNavigation().stop();
		}
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new Posed());
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(2, new PanicGoal(this, 1.5));
		goalSelector.addGoal(3, new BreedGoal(this, 1.0));
		goalSelector.addGoal(4, new TemptGoal(this, 1.1, this::isFood, false));
		goalSelector.addGoal(5, new BringCatch());
		goalSelector.addGoal(6, new FollowTruster());
		goalSelector.addGoal(7, new FollowParentGoal(this, 1.2));
		goalSelector.addGoal(8, new RandomStrollGoal(this, 0.9));
		goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(10, new RandomLookAroundGoal(this));
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
			boolean afloat = isInWater();
			boolean aloft = !onGround() && !afloat;
			swimO = swim;
			swim = WildlifeRules.approach(swim, afloat && pose != PuffinRules.DIVE ? 1 : 0, 0.2F);
			diveO = dive;
			dive = WildlifeRules.approach(dive, pose == PuffinRules.DIVE && afloat ? 1 : 0, 0.12F);
			sleepO = sleep;
			sleep = WildlifeRules.approach(sleep, pose == PuffinRules.SLEEP ? 1 : 0, 0.06F);
			preenO = preen;
			preen = WildlifeRules.approach(preen, pose == PuffinRules.PREEN ? 1 : 0, 0.15F);
			tapO = tap;
			tap = WildlifeRules.approach(tap, pose == PuffinRules.TAP ? 1 : 0, 0.2F);
			munchO = munch;
			munch = WildlifeRules.approach(munch, pose == PuffinRules.EAT ? 1 : 0, 0.2F);
			// Its wings: a whirring blur aloft, a quick shake stretching them, slow strokes flying under the water.
			float want = aloft || pose == PuffinRules.STRETCH ? 1 : pose == PuffinRules.DIVE && afloat ? 0.7F : 0;
			flapO = flap;
			flap = WildlifeRules.approach(flap, want, 0.25F);
			beatO = beat;
			beat += aloft ? 1.5F : pose == PuffinRules.STRETCH ? 0.9F : 0.3F;
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		// Aloft, its whirring wings let it down slow, and it never lands hard.
		if (!onGround() && !isInWater()) {
			Vec3 v = getDeltaMovement();
			if (v.y < -PuffinRules.GLIDE) {
				setDeltaMovement(v.x, PuffinRules.glide(v.y), v.z);
			}
			resetFallDistance();
		}
		if (carrying() && carryLeft > 0) {
			carryLeft--;
		}
		int pose = pose(), left = poseLeft();
		if (pose != PuffinRules.IDLE) {
			if (pose == PuffinRules.DIVE && isInWater() && left % 4 == 0) {
				level.sendParticles(ParticleTypes.BUBBLE, getX(), getY() + 0.3, getZ(), 2, 0.1, 0.1, 0.1, 0.02);
			}
			if (pose == PuffinRules.TAP && left % 5 == 0) {
				playSound(SoundEvents.BAMBOO_WOOD_BUTTON_CLICK_ON, 0.35F, 1.6F + random.nextFloat() * 0.3F);
			}
			if (pose == PuffinRules.EAT && left % 10 == 0) {
				double yaw = getYRot() * Mth.DEG_TO_RAD;
				level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.COD), getX() - Math.sin(yaw) * 0.3, getY() + 0.5,
					getZ() + Math.cos(yaw) * 0.3, 3, 0.05, 0.05, 0.05, 0.02);
				playSound(SoundEvents.PARROT_EAT, 0.5F, 0.8F);
			}
			if (pose == PuffinRules.SLEEP && !PuffinRules.night(level.getOverworldClockTime()) && left > 20) {
				entityData.set(LEFT, left = 20);
			}
			if (pose == PuffinRules.FLY && left < PuffinRules.FLY_TICKS - 6 && (onGround() || isInWater())) {
				left = 1;
			}
			if (left <= 1) {
				pose(PuffinRules.IDLE, 0);
				finish(level, pose);
			} else {
				entityData.set(LEFT, left - 1);
			}
			return;
		}
		long time = level.getOverworldClockTime();
		boolean night = PuffinRules.night(time);
		if (isInWater()) {
			if (night && random.nextInt(PuffinRules.SLEEP_ODDS) == 0) {
				pose(PuffinRules.SLEEP, PuffinRules.SLEEP_TICKS + random.nextInt(300));
			} else if (!night && !carrying() && !isBaby() && random.nextInt(PuffinRules.DIVE_ODDS) == 0
				&& PuffinRules.deepEnough(depth(level))) {
				dive();
			} else if (!night && !carrying() && !isBaby() && random.nextInt(PuffinRules.FLY_ODDS * 2) == 0) {
				takeOff(level, false);
			}
			return;
		}
		if (!onGround()) {
			return;
		}
		if (night) {
			if (random.nextInt(PuffinRules.SLEEP_ODDS) == 0) {
				pose(PuffinRules.SLEEP, PuffinRules.SLEEP_TICKS + random.nextInt(300));
			}
			return;
		}
		if (carrying()) {
			if (carryLeft <= 0) {
				pose(PuffinRules.EAT, PuffinRules.EAT_TICKS);
			}
			return;
		}
		if (random.nextInt(PuffinRules.PREEN_ODDS) == 0) {
			pose(PuffinRules.PREEN, PuffinRules.PREEN_TICKS + random.nextInt(40));
		} else if (random.nextInt(PuffinRules.STRETCH_ODDS) == 0) {
			stretch();
		} else if (!isBaby() && random.nextInt(PuffinRules.TAP_ODDS) == 0) {
			StonecrestPuffin other = mateNear(level);
			if (other != null) {
				mate = other;
				other.mate = this;
				pose(PuffinRules.TAP, PuffinRules.TAP_TICKS);
				other.pose(PuffinRules.TAP, PuffinRules.TAP_TICKS);
			}
		} else if (!isBaby() && random.nextInt(PuffinRules.FLY_ODDS) == 0) {
			takeOff(level, true);
		}
	}

	/** What happens as each thing it does comes to an end. */
	private void finish(ServerLevel level, int pose) {
		if (pose == PuffinRules.DIVE) {
			// Back up to the surface with a splash, and (two times in three) a bill full of fish.
			setDeltaMovement(getDeltaMovement().x, 0.35, getDeltaMovement().z);
			if (isInWater() && PuffinRules.catches(random.nextInt(3))) {
				carrying(true);
			}
			level.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.4, getZ(), 12, 0.2, 0.1, 0.2, 0.1);
			playSound(SoundEvents.GENERIC_SPLASH, 0.3F, 1.6F);
		} else if (pose == PuffinRules.EAT) {
			carrying(false);
			heal(2);
		} else if (pose == PuffinRules.TAP) {
			mate = null;
		} else if (pose == PuffinRules.FLY && onGround()) {
			// Down on the rocks: it gives its wings a shake and folds them.
			stretch();
		}
	}

	private void stretch() {
		pose(PuffinRules.STRETCH, PuffinRules.STRETCH_TICKS);
		playSound(SoundEvents.PARROT_FLY, 0.4F, 1.3F);
	}

	private void dive() {
		pose(PuffinRules.DIVE, PuffinRules.DIVE_TICKS + random.nextInt(30));
		double yaw = getYRot() * Mth.DEG_TO_RAD;
		setDeltaMovement(-Math.sin(yaw) * 0.1, -0.25, Math.cos(yaw) * 0.1);
		playSound(SoundEvents.GENERIC_SPLASH, 0.25F, 1.8F);
	}

	/**
	 * A whirring hop: out to the water if it is ashore and the sea is near, back to the rocks if it is afloat, otherwise just a
	 * little way along.
	 */
	private void takeOff(ServerLevel level, boolean toSea) {
		BlockPos at = blockPosition();
		float yaw = getYRot();
		for (int i = 0; i < 16; i++) {
			int dx = random.nextInt(PuffinRules.SEA_REACH * 2 + 1) - PuffinRules.SEA_REACH;
			int dz = random.nextInt(PuffinRules.SEA_REACH * 2 + 1) - PuffinRules.SEA_REACH;
			if (dx * dx + dz * dz < 9) {
				continue;
			}
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX() + dx, at.getZ() + dz);
			boolean water = level.getFluidState(new BlockPos(at.getX() + dx, y - 1, at.getZ() + dz)).is(FluidTags.WATER);
			if (water == toSea && Math.abs(y - at.getY()) <= 4) {
				yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
				break;
			}
		}
		pose(PuffinRules.FLY, PuffinRules.FLY_TICKS);
		setYRot(yaw);
		setYBodyRot(yaw);
		setYHeadRot(yaw);
		double r = yaw * Mth.DEG_TO_RAD;
		setDeltaMovement(-Math.sin(r) * 0.42, isInWater() ? 0.7 : 0.55, Math.cos(r) * 0.42);
		playSound(SoundEvents.PARROT_FLY, 0.6F, 1.1F);
	}

	/** How deep the water is under it, up to a few blocks. */
	private int depth(ServerLevel level) {
		BlockPos at = blockPosition();
		int depth = 0;
		while (depth < 4 && level.getFluidState(at.below(depth)).is(FluidTags.WATER)) {
			depth++;
		}
		return depth;
	}

	/** Another grown puffin close by, ashore and with nothing else to do, to bill-tap with. */
	private @Nullable StonecrestPuffin mateNear(ServerLevel level) {
		StonecrestPuffin best = null;
		double near = PuffinRules.MATE_NEAR;
		for (StonecrestPuffin other : level.getEntitiesOfClass(StonecrestPuffin.class, getBoundingBox().inflate(PuffinRules.MATE_NEAR),
			p -> p != this && p.isAlive() && !p.isBaby() && p.onGround() && !p.isInWater() && p.pose() == PuffinRules.IDLE && !p.carrying())) {
			double d = distanceTo(other);
			if (d < near) {
				near = d;
				best = other;
			}
		}
		return best;
	}

	private @Nullable StonecrestPuffin puffling() {
		StonecrestPuffin best = null;
		double near = PuffinRules.BRING_FAR;
		for (StonecrestPuffin other : level().getEntitiesOfClass(StonecrestPuffin.class, getBoundingBox().inflate(PuffinRules.BRING_FAR),
			p -> p != this && p.isAlive() && p.isBaby())) {
			double d = distanceTo(other);
			if (d < near) {
				near = d;
				best = other;
			}
		}
		return best;
	}

	private @Nullable Player truster() {
		if (truster == null || !PuffinRules.trusts(level().getGameTime() - fedAt)) {
			return null;
		}
		Player player = level().getPlayerByUUID(truster);
		return player != null && player.isAlive() && !player.isSpectator() ? player : null;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hit = super.hurtServer(level, source, amount);
		if (hit && pose() != PuffinRules.IDLE && pose() != PuffinRules.FLY) {
			pose(PuffinRules.IDLE, 0);
		}
		return hit;
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (!isFood(held) || pose() == PuffinRules.DIVE || pose() == PuffinRules.FLY) {
			return super.mobInteract(player, hand);
		}
		InteractionResult result = super.mobInteract(player, hand);
		if (!level().isClientSide()) {
			if (!result.consumesAction()) {
				held.consume(1, player);
			}
			// Fed, it trusts you: hearts, and it stands there with the fish crosswise in its bill and gulps it down.
			truster = player.getUUID();
			fedAt = level().getGameTime();
			gifted = false;
			entityData.set(FISH, true);
			carryLeft = PuffinRules.CARRY_TICKS;
			pose(PuffinRules.EAT, PuffinRules.EAT_TICKS);
			level().broadcastEntityEvent(this, (byte) 18);
			playSound(SoundEvents.PARROT_EAT, 0.7F, 0.8F);
		}
		return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}

	// ------------------------------------------------------------------ breeding: a puffling

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return getType().create(level, EntitySpawnReason.BREEDING) instanceof StonecrestPuffin puffling ? puffling : null;
	}

	// ------------------------------------------------------------------ sound: a low purring growl

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		int pose = pose();
		return pose == PuffinRules.SLEEP || pose == PuffinRules.DIVE || carrying() ? null : SoundEvents.PARROT_AMBIENT;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.PARROT_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return SoundEvents.PARROT_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState block) {
		playSound(SoundEvents.CHICKEN_STEP.value(), 0.12F, 1.3F);
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.55F;
	}

	@Override
	protected float getSoundVolume() {
		return 0.45F;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 260;
	}

	/** Busy at something (diving, preening, bill-tapping, asleep, eating): it keeps to it. */
	private final class Posed extends Goal {
		Posed() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return pose() != PuffinRules.IDLE;
		}

		@Override
		public void start() {
			if (pose() != PuffinRules.FLY) {
				getNavigation().stop();
			}
		}

		@Override
		public void tick() {
			int pose = pose();
			if (pose == PuffinRules.FLY) {
				return;
			}
			if (pose == PuffinRules.DIVE) {
				// Flying under the water with its wings: down for the first half, wandering after the fish, then back up.
				if (isInWater()) {
					setYRot(getYRot() + (float) random.nextGaussian() * 3);
					setYBodyRot(getYRot());
					double yaw = getYRot() * Mth.DEG_TO_RAD;
					boolean down = poseLeft() > PuffinRules.DIVE_TICKS / 2;
					setDeltaMovement(getDeltaMovement().add(-Math.sin(yaw) * 0.012, down ? -0.02 : 0.015, Math.cos(yaw) * 0.012));
				}
				return;
			}
			setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
			// Afloat, it bobs on the surface whatever it's doing.
			if (isInWater() && getFluidHeight(FluidTags.WATER) > getFluidJumpThreshold()) {
				getJumpControl().jump();
			}
			if (pose == PuffinRules.TAP && mate != null) {
				getLookControl().setLookAt(mate, 30, 30);
				setYBodyRot(getYHeadRot());
			}
		}
	}

	/** With fish in its bill: to a puffling if there's one about, else to someone it trusts. */
	private final class BringCatch extends Goal {
		private @Nullable StonecrestPuffin chick;
		private @Nullable Player friend;
		private int ticks;

		BringCatch() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (!carrying() || pose() != PuffinRules.IDLE || tickCount % 10 != 0) {
				return false;
			}
			chick = puffling();
			friend = truster();
			int to = PuffinRules.catchFor(chick != null ? distanceTo(chick) : -1, friend != null ? distanceTo(friend) : -1, gifted);
			if (to == PuffinRules.FEED) {
				friend = null;
			} else if (to == PuffinRules.GIFT) {
				chick = null;
			} else {
				chick = null;
				friend = null;
			}
			return chick != null || friend != null;
		}

		@Override
		public boolean canContinueToUse() {
			return carrying() && pose() == PuffinRules.IDLE && ticks < 400
				&& (chick != null && chick.isAlive() || friend != null && friend.isAlive());
		}

		@Override
		public void start() {
			ticks = 0;
		}

		@Override
		public void stop() {
			chick = null;
			friend = null;
			getNavigation().stop();
		}

		@Override
		public void tick() {
			ticks++;
			var to = chick != null ? chick : friend;
			if (to == null) {
				return;
			}
			getLookControl().setLookAt(to, 30, 30);
			if (distanceTo(to) > PuffinRules.BRING_NEAR) {
				if (ticks % 10 == 1) {
					getNavigation().moveTo(to, 1.1);
				}
				return;
			}
			if (!(level() instanceof ServerLevel level)) {
				return;
			}
			if (chick != null) {
				// Fed to the puffling, which grows on it.
				if (chick.isBaby()) {
					chick.ageUp(AgeableMob.getSpeedUpSecondsWhenFeeding(-chick.getAge()), true);
				}
				chick.playSound(SoundEvents.PARROT_EAT, 0.5F, 1.6F);
			} else {
				// Dropped at your feet: a present.
				spawnAtLocation(level, new ItemStack(random.nextInt(4) == 0 ? Items.SALMON : Items.COD));
				gifted = true;
				level.broadcastEntityEvent(StonecrestPuffin.this, (byte) 18);
				playSound(SoundEvents.PARROT_AMBIENT, 0.6F, 0.7F);
			}
			carrying(false);
			ticks = 999;
		}
	}

	/** Trusting someone, it waddles along after them. */
	private final class FollowTruster extends Goal {
		private @Nullable Player player;

		FollowTruster() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			player = truster();
			return player != null && !isBaby() && distanceTo(player) > PuffinRules.FOLLOW_FAR && distanceTo(player) < 24;
		}

		@Override
		public boolean canContinueToUse() {
			return player != null && truster() == player && distanceTo(player) > PuffinRules.FOLLOW_NEAR && distanceTo(player) < 24;
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
				getNavigation().moveTo(player, 1.2);
			}
		}
	}
}
