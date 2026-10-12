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
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A cinnabar panda: a small red panda of the cherry groves, bamboo and old-growth forests, rust-red above and black below, with a
 * white-masked face, white-rimmed ears and a great ringed tail. A stranger coming near makes it rear up on its hind legs with its
 * forepaws spread wide, to look as big as it can. It washes its face with its paws, sniffs out ripe sweet berry bushes and sits up
 * to eat, and through the bright middle of the day it curls up and naps with its tail over its face. Two near each other now and
 * then tumble over and over together; cubs do it on their own.
 * <p>Sweet berries (or bamboo) tempt it and breed it. Feed one and it trusts you a while: hearts, it sits up to eat what you gave it,
 * it trots along after you and no longer rears up at you.
 */
public class CinnabarPanda extends Animal {
	/** What it is doing ({@link RedPandaRules#IDLE} and so on), and how long that has to go. */
	private static final EntityDataAccessor<Integer> POSE = SynchedEntityData.defineId(CinnabarPanda.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> LEFT = SynchedEntityData.defineId(CinnabarPanda.class, EntityDataSerializers.INT);

	private @Nullable UUID truster;
	private long fedAt = -1, ateAt = -RedPandaRules.FORAGE_REST, rearAgain;

	/** Client: how far it is reared up, washing, curled asleep, sat up eating, tumbling, eased, this tick's and last. */
	public float stand, standO, wash, washO, curl, curlO, munch, munchO, tumble, tumbleO;

	public CinnabarPanda(EntityType<? extends CinnabarPanda> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.25);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(POSE, RedPandaRules.IDLE);
		builder.define(LEFT, 0);
	}

	public int pose() {
		return entityData.get(POSE);
	}

	public int poseLeft() {
		return entityData.get(LEFT);
	}

	private void pose(int pose, int ticks) {
		entityData.set(POSE, pose);
		entityData.set(LEFT, ticks);
		if (pose != RedPandaRules.IDLE) {
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
		goalSelector.addGoal(5, new Forage());
		goalSelector.addGoal(6, new FollowTruster());
		goalSelector.addGoal(7, new FollowParentGoal(this, 1.15));
		goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.9));
		goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(10, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.SWEET_BERRIES) || stack.is(Items.BAMBOO);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			int pose = pose();
			standO = stand;
			stand = WildlifeRules.approach(stand, pose == RedPandaRules.STAND ? 1 : 0, 0.2F);
			washO = wash;
			wash = WildlifeRules.approach(wash, pose == RedPandaRules.WASH ? 1 : 0, 0.15F);
			curlO = curl;
			curl = WildlifeRules.approach(curl, pose == RedPandaRules.SLEEP ? 1 : 0, 0.06F);
			munchO = munch;
			munch = WildlifeRules.approach(munch, pose == RedPandaRules.EAT ? 1 : 0, 0.15F);
			tumbleO = tumble;
			tumble = WildlifeRules.approach(tumble, pose == RedPandaRules.TUMBLE ? 1 : 0, 0.2F);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		int pose = pose(), left = poseLeft();
		// A stranger near: it rears up, even out of a nap if they come right up to it.
		if ((pose == RedPandaRules.IDLE || pose == RedPandaRules.SLEEP) && tickCount % 5 == 0 && now >= rearAgain && stranger(level, pose == RedPandaRules.SLEEP)) {
			rear(now);
			return;
		}
		if (pose != RedPandaRules.IDLE) {
			if (pose == RedPandaRules.SLEEP && !RedPandaRules.drowsy(level.getOverworldClockTime()) && left > 20) {
				entityData.set(LEFT, left = 20);
			}
			if (pose == RedPandaRules.EAT && left % 12 == 0) {
				munch(level);
			}
			if (pose == RedPandaRules.TUMBLE && left % (RedPandaRules.ROLL_TICKS + RedPandaRules.ROLL_PAUSE) == 0) {
				playSound(SoundEvents.PANDA_AMBIENT, 0.4F, 1.9F + random.nextFloat() * 0.2F);
			}
			if (left <= 1) {
				pose(RedPandaRules.IDLE, 0);
			} else {
				entityData.set(LEFT, left - 1);
			}
			return;
		}
		if (isInWater()) {
			return;
		}
		if (RedPandaRules.drowsy(level.getOverworldClockTime()) && random.nextInt(RedPandaRules.SLEEP_ODDS) == 0 && onGround()) {
			pose(RedPandaRules.SLEEP, RedPandaRules.SLEEP_TICKS + random.nextInt(300));
		} else if (random.nextInt(RedPandaRules.WASH_ODDS) == 0) {
			pose(RedPandaRules.WASH, RedPandaRules.WASH_TICKS);
		} else if (random.nextInt(isBaby() ? RedPandaRules.PLAY_ODDS / 3 : RedPandaRules.PLAY_ODDS) == 0) {
			play(level);
		}
	}

	/** Someone it doesn't trust near enough to rear up at. */
	private boolean stranger(ServerLevel level, boolean asleep) {
		for (Player player : level.players()) {
			if (!player.isSpectator() && !player.isCreative() && RedPandaRules.rears(distanceTo(player), trusts(player), asleep)) {
				getLookControl().setLookAt(player, 30, 30);
				return true;
			}
		}
		return false;
	}

	private void rear(long now) {
		pose(RedPandaRules.STAND, RedPandaRules.STAND_TICKS);
		rearAgain = now + RedPandaRules.STAND_TICKS * 4L;
		playSound(SoundEvents.PANDA_AGGRESSIVE_AMBIENT, 0.5F, 1.8F);
	}

	/** A tumble: with another near and idle if there is one (both of them rolling over together, face to face), a cub on its own. */
	private void play(ServerLevel level) {
		CinnabarPanda friend = null;
		for (CinnabarPanda other : level.getEntitiesOfClass(CinnabarPanda.class, getBoundingBox().inflate(RedPandaRules.PLAY_RANGE),
			other -> other != this && other.isAlive() && !other.isInWater())) {
			if (RedPandaRules.tumbles(distanceTo(other), other.pose() == RedPandaRules.IDLE)) {
				friend = other;
				break;
			}
		}
		if (friend == null && !isBaby()) {
			return;
		}
		pose(RedPandaRules.TUMBLE, RedPandaRules.TUMBLE_TICKS);
		if (friend != null) {
			friend.pose(RedPandaRules.TUMBLE, RedPandaRules.TUMBLE_TICKS);
			face(friend);
			friend.face(this);
		}
		playSound(SoundEvents.PANDA_AMBIENT, 0.6F, 2.0F);
	}

	private void face(CinnabarPanda other) {
		float yaw = (float) (Mth.atan2(other.getZ() - getZ(), other.getX() - getX()) * Mth.RAD_TO_DEG) - 90;
		setYRot(yaw);
		setYBodyRot(yaw);
		setYHeadRot(yaw);
	}

	/** A mouthful: crumbs of berry and a chew. */
	private void munch(ServerLevel level) {
		double yaw = getYRot() * Mth.DEG_TO_RAD;
		level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.SWEET_BERRIES), getX() - Math.sin(yaw) * 0.3, getY() + 0.6,
			getZ() + Math.cos(yaw) * 0.3, 3, 0.08, 0.05, 0.08, 0.02);
		playSound(SoundEvents.PANDA_EAT, 0.4F, 1.8F);
	}

	private boolean trusts(Player player) {
		return player.getUUID().equals(truster) && RedPandaRules.trusts(level().getGameTime() - fedAt);
	}

	private @Nullable Player truster() {
		if (truster == null || !RedPandaRules.trusts(level().getGameTime() - fedAt)) {
			return null;
		}
		Player player = level().getPlayerByUUID(truster);
		return player != null && player.isAlive() && !player.isSpectator() ? player : null;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hit = super.hurtServer(level, source, amount);
		if (hit && pose() != RedPandaRules.IDLE) {
			pose(RedPandaRules.IDLE, 0);
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
			// Fed, it trusts you: hearts, and it sits right down to eat what you gave it.
			truster = player.getUUID();
			fedAt = ateAt = level().getGameTime();
			pose(RedPandaRules.EAT, RedPandaRules.EAT_TICKS);
			level().broadcastEntityEvent(this, (byte) 18);
			playSound(SoundEvents.PANDA_EAT, 0.7F, 1.7F);
		}
		return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}

	// ------------------------------------------------------------------ breeding: a cub

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return getType().create(level, EntitySpawnReason.BREEDING) instanceof CinnabarPanda cub ? cub : null;
	}

	// ------------------------------------------------------------------ sound: twitters and huffs

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return pose() == RedPandaRules.SLEEP ? null : SoundEvents.PANDA_AMBIENT;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.PANDA_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return SoundEvents.PANDA_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 1.8F;
	}

	@Override
	protected float getSoundVolume() {
		return 0.5F;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 260;
	}

	/** Rearing up, washing, napping, eating or tumbling: it holds still where it is. */
	private final class Posed extends Goal {
		Posed() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return pose() != RedPandaRules.IDLE;
		}

		@Override
		public void start() {
			getNavigation().stop();
		}

		@Override
		public void tick() {
			setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
		}
	}

	/** It sniffs out a ripe sweet berry bush, trots over, strips it and sits up to eat. */
	private final class Forage extends Goal {
		private @Nullable BlockPos bush;
		private int ticks;

		Forage() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (pose() != RedPandaRules.IDLE || tickCount % 40 != 0 || !RedPandaRules.forages(level().getGameTime() - ateAt)) {
				return false;
			}
			int reach = (int) RedPandaRules.FORAGE_RANGE;
			bush = null;
			double best = Double.MAX_VALUE;
			BlockPos at = blockPosition();
			for (BlockPos pos : BlockPos.betweenClosed(at.offset(-reach, -3, -reach), at.offset(reach, 3, reach))) {
				double d = pos.distSqr(at);
				if (d < best && ripe(level().getBlockState(pos))) {
					best = d;
					bush = pos.immutable();
				}
			}
			if (bush == null) {
				// Nothing ripe about: try again a little later rather than sniffing every couple of seconds.
				ateAt = level().getGameTime() - RedPandaRules.FORAGE_REST + 200;
			}
			return bush != null;
		}

		@Override
		public boolean canContinueToUse() {
			return bush != null && ticks < 300 && pose() == RedPandaRules.IDLE && ripe(level().getBlockState(bush));
		}

		@Override
		public void start() {
			ticks = 0;
		}

		@Override
		public void stop() {
			bush = null;
			getNavigation().stop();
		}

		@Override
		public void tick() {
			if (bush == null || !(level() instanceof ServerLevel level)) {
				return;
			}
			ticks++;
			getLookControl().setLookAt(bush.getX() + 0.5, bush.getY() + 0.5, bush.getZ() + 0.5);
			if (distanceToSqr(bush.getX() + 0.5, bush.getY(), bush.getZ() + 0.5) < 2.6) {
				BlockState state = level.getBlockState(bush);
				level.setBlock(bush, state.setValue(SweetBerryBushBlock.AGE, 1), 2);
				level.playSound(null, bush, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, getSoundSource(), 0.8F, 1.2F);
				ateAt = level.getGameTime();
				heal(2);
				pose(RedPandaRules.EAT, RedPandaRules.EAT_TICKS);
				bush = null;
			} else if (ticks % 10 == 1) {
				getNavigation().moveTo(bush.getX() + 0.5, bush.getY(), bush.getZ() + 0.5, 1.0);
			}
		}

		private static boolean ripe(BlockState state) {
			return state.is(Blocks.SWEET_BERRY_BUSH) && state.getValue(SweetBerryBushBlock.AGE) >= 2;
		}
	}

	/** Trusting someone, it trots along after them. */
	private final class FollowTruster extends Goal {
		private @Nullable Player player;

		FollowTruster() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			player = truster();
			return player != null && !isBaby() && distanceTo(player) > RedPandaRules.FOLLOW_FAR && distanceTo(player) < 24;
		}

		@Override
		public boolean canContinueToUse() {
			return player != null && truster() == player && distanceTo(player) > RedPandaRules.FOLLOW_NEAR && distanceTo(player) < 24;
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
