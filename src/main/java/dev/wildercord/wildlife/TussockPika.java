package dev.wildercord.wildlife;

import dev.wildercord.cast.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A tussock pika: a tiny round grey-brown pika of the meadows and high stony slopes, all fluff, with big round ears, no
 * tail to speak of and a busy little face. All summer long it goes haying: it scurries off, nibbles through a stalk of grass
 * and carries it home crosswise in its mouth to add to its pile. On a sunny day it climbs onto bare rock and basks there,
 * flattened out with its eyes shut, until something disturbs it.
 * <p>Wheat tempts it and breeds it. Fed, it trusts you a while and scampers after you, and its haying turns to flowers: it
 * nibbles one off and brings it to your feet.
 */
public class TussockPika extends Animal {
	private static final EntityDataAccessor<Integer> CARRY = SynchedEntityData.defineId(TussockPika.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> BASKING = SynchedEntityData.defineId(TussockPika.class, EntityDataSerializers.BOOLEAN);

	/** Where its haypile is, the flower it carries for its friend, who fed it and when, and its last trip and bask. */
	private @Nullable BlockPos home;
	private Item flower = Items.AIR;
	private @Nullable UUID truster;
	private long fedAt = -1;
	private long hayedAt = -1;
	private long baskedAt = -1;

	/** Client: how far flattened out basking, eased, this tick's and last. */
	public float bask, baskO;

	public TussockPika(EntityType<? extends TussockPika> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.3);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CARRY, PikaRules.NOTHING);
		builder.define(BASKING, false);
	}

	/** What it carries in its mouth: {@link PikaRules#NOTHING}, {@link PikaRules#GRASS} or {@link PikaRules#FLOWER}. */
	public int carrying() {
		return entityData.get(CARRY);
	}

	public boolean basking() {
		return entityData.get(BASKING);
	}

	private void carry(int what) {
		entityData.set(CARRY, what);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new Bask());
		goalSelector.addGoal(2, new PanicGoal(this, 1.6));
		goalSelector.addGoal(3, new BreedGoal(this, 1.0));
		goalSelector.addGoal(4, new TemptGoal(this, 1.15, this::isFood, false));
		goalSelector.addGoal(5, new Hay());
		goalSelector.addGoal(6, new FollowTruster());
		goalSelector.addGoal(7, new FollowParentGoal(this, 1.15));
		goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.9));
		goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(10, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.WHEAT);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			baskO = bask;
			bask = WildlifeRules.approach(bask, basking() ? 1 : 0, 0.1F);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (home == null) {
			home = blockPosition();
		}
	}

	/** Bare rock under it, open sky over it. */
	private boolean onSunnyRock() {
		BlockState under = level().getBlockState(blockPosition().below());
		return (under.is(BlockTags.BASE_STONE_OVERWORLD) || under.is(Blocks.CALCITE) || under.is(Blocks.COBBLESTONE))
			&& level().canSeeSky(blockPosition());
	}

	/** Something near enough to rouse it from its rock: a monster, or someone not creeping, other than its friend. */
	private boolean disturbed() {
		Player friend = truster();
		for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(6), e -> e != this && e.isAlive())) {
			if (e instanceof Enemy) {
				return true;
			}
			if (e instanceof Player player && player != friend && !player.isSpectator() && !player.isCrouching() && distanceTo(player) < 3.5) {
				return true;
			}
		}
		return false;
	}

	private @Nullable Player truster() {
		if (truster == null || !PikaRules.trusts(level().getGameTime() - fedAt)) {
			return null;
		}
		Player player = level().getPlayerByUUID(truster);
		return player != null && player.isAlive() && !player.isSpectator() ? player : null;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hit = super.hurtServer(level, source, amount);
		if (hit) {
			entityData.set(BASKING, false);
			if (carrying() != PikaRules.NOTHING) {
				carry(PikaRules.NOTHING);
				flower = Items.AIR;
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
			truster = player.getUUID();
			fedAt = level().getGameTime();
			hayedAt = -1;
			entityData.set(BASKING, false);
			level().broadcastEntityEvent(this, (byte) 18);
			playSound(SoundEvents.RABBIT_AMBIENT, 0.7F, 1.9F);
		}
		return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return getType().create(level, EntitySpawnReason.BREEDING) instanceof TussockPika young ? young : null;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return basking() ? null : SoundEvents.RABBIT_AMBIENT;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.RABBIT_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return SoundEvents.RABBIT_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.RABBIT_JUMP, 0.05F, 1.8F);
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 1.7F;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 160;
	}

	/** Flattened out on a sunny rock, eyes shut, until it has had its fill or something comes near. */
	private final class Bask extends Goal {
		private long since;
		private int length;

		Bask() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			if ((tickCount + getId()) % 20 != 0 || isBaby() || !onGround() || carrying() != PikaRules.NOTHING || truster() != null) {
				return false;
			}
			long now = level().getGameTime();
			return PikaRules.basks(level().isBrightOutside(), getNavigation().isDone(), onSunnyRock(), baskedAt < 0 ? -1 : now - baskedAt)
				&& !disturbed();
		}

		@Override
		public boolean canContinueToUse() {
			boolean disturbed = (tickCount % 10 == 0 && disturbed()) || getLastHurtByMob() != null && tickCount - getLastHurtByMobTimestamp() < 40;
			return basking() && PikaRules.staysBasking(level().isBrightOutside(), disturbed, since, length);
		}

		@Override
		public void start() {
			since = 0;
			length = PikaRules.baskLength(getRandom().nextDouble());
			getNavigation().stop();
			entityData.set(BASKING, true);
		}

		@Override
		public void tick() {
			since++;
			setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
		}

		@Override
		public void stop() {
			entityData.set(BASKING, false);
			baskedAt = level().getGameTime();
		}
	}

	/**
	 * A haying trip: off to a stalk of grass (or for a friend, a flower), nibble it through, and carry it home to the pile, or
	 * to its friend's feet.
	 */
	private final class Hay extends Goal {
		private @Nullable BlockPos plant;
		private int nibbled;
		private int trip;

		Hay() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if ((tickCount + getId()) % 20 != 0 || !(level() instanceof ServerLevel level) || !level.getGameRules().get(GameRules.MOB_GRIEFING)) {
				return false;
			}
			boolean friend = truster() != null;
			if (!PikaRules.hays(!isBaby(), !basking(), carrying(), hayedAt < 0 ? -1 : level.getGameTime() - hayedAt, friend)) {
				return false;
			}
			plant = find(friend);
			return plant != null;
		}

		@Override
		public boolean canContinueToUse() {
			return trip < PikaRules.TRIP_TICKS && (plant != null || carrying() != PikaRules.NOTHING);
		}

		@Override
		public void start() {
			nibbled = 0;
			trip = 0;
			if (plant != null) {
				getNavigation().moveTo(plant.getX() + 0.5, plant.getY(), plant.getZ() + 0.5, 1.1);
			}
		}

		@Override
		public void stop() {
			hayedAt = level().getGameTime();
			plant = null;
			if (carrying() == PikaRules.FLOWER) {
				// Its friend went off: it eats the flower itself.
				carry(PikaRules.NOTHING);
				flower = Items.AIR;
			}
			getNavigation().stop();
		}

		@Override
		public void tick() {
			if (!(level() instanceof ServerLevel level)) {
				return;
			}
			trip++;
			if (plant != null) {
				cutting(level);
			} else {
				bringing(level);
			}
		}

		/** Off to the plant, then nibbling it through. */
		private void cutting(ServerLevel level) {
			BlockState state = level.getBlockState(plant);
			boolean friend = truster() != null;
			if (!PikaRules.cuts(grass(state), state.is(BlockTags.SMALL_FLOWERS), friend)) {
				plant = null;
				return;
			}
			Vec3 at = Vec3.atBottomCenterOf(plant);
			getLookControl().setLookAt(at.x, at.y + 0.2, at.z);
			if (position().distanceToSqr(at) > 1.2) {
				if (trip % 20 == 0) {
					getNavigation().moveTo(at.x, at.y, at.z, 1.1);
				}
				nibbled = 0;
				return;
			}
			getNavigation().stop();
			if (++nibbled % 6 == 0) {
				Fx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y + 0.3, at.z, 2, 0.1, 0.1, 0.1, 0.02);
				playSound(SoundEvents.GRASS_HIT, 0.25F, 1.6F);
			}
			if (PikaRules.cut(nibbled)) {
				boolean bloom = state.is(BlockTags.SMALL_FLOWERS);
				flower = bloom ? state.getBlock().asItem() : Items.AIR;
				level.destroyBlock(plant, false, TussockPika.this);
				carry(PikaRules.carries(bloom));
				plant = null;
				playSound(SoundEvents.GRASS_BREAK, 0.4F, 1.5F);
			}
		}

		/** Carrying it off: a flower to its friend's feet, grass home to the pile. */
		private void bringing(ServerLevel level) {
			Player friend = truster();
			Vec3 to;
			if (carrying() == PikaRules.FLOWER) {
				if (friend == null) {
					trip = PikaRules.TRIP_TICKS;
					return;
				}
				to = friend.position();
			} else {
				to = Vec3.atBottomCenterOf(home == null ? blockPosition() : home);
			}
			if (position().distanceTo(to) > PikaRules.DROP_RANGE) {
				if (trip % 10 == 0) {
					getNavigation().moveTo(to.x, to.y, to.z, friend != null ? 1.3 : 1.1);
				}
				return;
			}
			getNavigation().stop();
			if (carrying() == PikaRules.FLOWER && friend != null) {
				getLookControl().setLookAt(friend, 30, 30);
				ItemEntity gift = new ItemEntity(level, friend.getX(), friend.getY() + 0.1, friend.getZ(), new ItemStack(flower));
				gift.setDefaultPickUpDelay();
				gift.setDeltaMovement(Vec3.ZERO);
				level.addFreshEntity(gift);
				Fx.send(level, ParticleTypes.HEART, getX(), getY() + 0.5, getZ(), 1, 0.1, 0.1, 0.1, 0);
				playSound(SoundEvents.RABBIT_AMBIENT, 0.8F, 2.0F);
			} else {
				Fx.send(level, ParticleTypes.COMPOSTER, getX(), getY() + 0.2, getZ(), 6, 0.25, 0.1, 0.25, 0);
				playSound(SoundEvents.GRASS_PLACE, 0.4F, 1.5F);
			}
			carry(PikaRules.NOTHING);
			flower = Items.AIR;
		}

		/** Somewhere near, a plant it may cut and can reach. */
		private @Nullable BlockPos find(boolean friend) {
			BlockPos here = blockPosition();
			int r = PikaRules.HAY_RANGE;
			BlockPos grassAt = null;
			for (int i = 0; i < 24; i++) {
				BlockPos at = here.offset(getRandom().nextInt(2 * r + 1) - r, getRandom().nextInt(5) - 2, getRandom().nextInt(2 * r + 1) - r);
				BlockState state = level().getBlockState(at);
				boolean bloom = state.is(BlockTags.SMALL_FLOWERS);
				if (!PikaRules.cuts(grass(state), bloom, friend)) {
					continue;
				}
				if (bloom) {
					// For a friend, a flower over any grass.
					return at.immutable();
				}
				if (grassAt == null) {
					grassAt = at.immutable();
				}
			}
			return friend ? null : grassAt;
		}

		private static boolean grass(BlockState state) {
			return state.is(Blocks.SHORT_GRASS) || state.is(Blocks.FERN);
		}
	}

	/** Trusting someone, it scampers along after them. */
	private final class FollowTruster extends Goal {
		private @Nullable Player player;

		FollowTruster() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			player = truster();
			return player != null && !isBaby() && carrying() == PikaRules.NOTHING && distanceTo(player) > PikaRules.FOLLOW_FAR
				&& distanceTo(player) < 24;
		}

		@Override
		public boolean canContinueToUse() {
			return player != null && truster() == player && distanceTo(player) > PikaRules.FOLLOW_NEAR && distanceTo(player) < 24;
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
				getNavigation().moveTo(player, 1.3);
			}
		}
	}
}
