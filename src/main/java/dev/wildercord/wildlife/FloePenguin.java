package dev.wildercord.wildlife;

import dev.wildercord.cast.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
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
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * A floe penguin: a small round black-and-white penguin of the frozen shores, with a white ring round each eye, a stubby
 * beak and pink feet. On land it waddles, rocking side to side; in a hurry on ice or snow it flops onto its belly and
 * toboggans along, pushing with its feet, far faster than it can walk. In the water it is quick and sleek.
 * <p>Now and then it waddles down to open water, dives, and comes up with a fish crosswise in its beak. It eats it itself,
 * unless there is a hungry chick about, which it brings it to. At night the colony gathers into a tight huddle and dozes,
 * heads tucked in, and anyone standing in among them is kept warm: the frost thaws off them.
 * <p>Raw cod and salmon tempt it and breed it. Fed, it trusts you a while and waddles (or slides) after you, and its catch
 * is yours: it brings the fish to your feet.
 */
public class FloePenguin extends Animal {
	private static final EntityDataAccessor<Integer> CARRY = SynchedEntityData.defineId(FloePenguin.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> SLIDING = SynchedEntityData.defineId(FloePenguin.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> HUDDLING = SynchedEntityData.defineId(FloePenguin.class, EntityDataSerializers.BOOLEAN);

	/** Who fed it and when, and its last fishing trip. */
	private @Nullable UUID truster;
	private long fedAt = -1;
	private long fishedAt = -1;

	/** Client: how far down on its belly sliding, swimming, and tucked into the huddle, eased, this tick's and last. */
	public float slide, slideO, swim, swimO, huddle, huddleO;

	public FloePenguin(EntityType<? extends FloePenguin> type, Level level) {
		super(type, level);
		moveControl = new SmoothSwimmingMoveControl<>(this, 85, 10, 0.6F, 1.0F, false);
		setPathfindingMalus(PathType.WATER, 0);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.TEMPT_RANGE, 10);
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
			moveRelative(getSpeed() * 1.6F, input);
			move(MoverType.SELF, getDeltaMovement());
			setDeltaMovement(getDeltaMovement().scale(0.9));
		} else {
			super.travel(input);
		}
	}

	/** On its belly it goes much faster than it walks. */
	@Override
	public void setSpeed(float speed) {
		super.setSpeed(PenguinRules.speed(speed, sliding()));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CARRY, PenguinRules.NOTHING);
		builder.define(SLIDING, false);
		builder.define(HUDDLING, false);
	}

	/** What it carries in its beak: {@link PenguinRules#NOTHING}, {@link PenguinRules#COD} or {@link PenguinRules#SALMON}. */
	public int carrying() {
		return entityData.get(CARRY);
	}

	public boolean sliding() {
		return entityData.get(SLIDING);
	}

	public boolean huddling() {
		return entityData.get(HUDDLING);
	}

	private void carry(int what) {
		entityData.set(CARRY, what);
	}

	private static Item fish(int what) {
		return what == PenguinRules.SALMON ? Items.SALMON : Items.COD;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new PanicGoal(this, 1.5));
		goalSelector.addGoal(2, new BreedGoal(this, 1.0));
		goalSelector.addGoal(3, new TemptGoal(this, 1.2, this::isFood, false));
		goalSelector.addGoal(4, new Fish());
		goalSelector.addGoal(5, new FollowTruster());
		goalSelector.addGoal(6, new FollowParentGoal(this, 1.2));
		goalSelector.addGoal(7, new Huddle());
		goalSelector.addGoal(8, new RandomStrollGoal(this, 1.0));
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
			slideO = slide;
			swimO = swim;
			huddleO = huddle;
			slide = WildlifeRules.approach(slide, sliding() ? 1 : 0, 0.2F);
			swim = WildlifeRules.approach(swim, isInWater() ? 1 : 0, 0.15F);
			huddle = WildlifeRules.approach(huddle, huddling() ? 1 : 0, 0.08F);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		double hurry = getMoveControl().hasWanted() ? getMoveControl().getSpeedModifier() : 0;
		boolean slides = PenguinRules.slides(onSlick(), onGround() && !isInWater(), hurry);
		if (slides != sliding()) {
			entityData.set(SLIDING, slides);
			if (slides) {
				playSound(SoundEvents.SNOW_HIT, 0.5F, 1.4F);
			}
		}
		if (slides && tickCount % 3 == 0) {
			Fx.send(level, ParticleTypes.SNOWFLAKE, getX(), getY() + 0.1, getZ(), 2, 0.15, 0.05, 0.15, 0.01);
		}
		if (huddling() && tickCount % 20 == 0) {
			// Anyone standing in among the huddle is kept warm.
			for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(1.2))) {
				player.setTicksFrozen(Math.max(0, player.getTicksFrozen() - 40));
			}
		}
	}

	/** Ice or snow underfoot. */
	private boolean onSlick() {
		BlockPos at = blockPosition();
		return slick(level().getBlockState(at)) || slick(level().getBlockState(at.below()));
	}

	private static boolean slick(BlockState state) {
		return state.is(BlockTags.ICE) || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW);
	}

	private @Nullable Player truster() {
		if (truster == null || level().getGameTime() - fedAt > PenguinRules.TRUST_TICKS) {
			return null;
		}
		Player player = level().getPlayerByUUID(truster);
		return player != null && player.isAlive() && !player.isSpectator() ? player : null;
	}

	/** A chick of its colony nearby that could do with a fish. */
	private @Nullable FloePenguin chick() {
		List<FloePenguin> chicks = level().getEntitiesOfClass(FloePenguin.class, getBoundingBox().inflate(PenguinRules.CHICK_RANGE),
			p -> p != this && p.isBaby() && p.isAlive());
		FloePenguin best = null;
		for (FloePenguin p : chicks) {
			if (best == null || distanceToSqr(p) < distanceToSqr(best)) {
				best = p;
			}
		}
		return best;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hit = super.hurtServer(level, source, amount);
		if (hit) {
			entityData.set(HUDDLING, false);
			if (carrying() != PenguinRules.NOTHING) {
				// It drops its fish in the fright.
				ItemEntity dropped = new ItemEntity(level, getX(), getY() + 0.4, getZ(), new ItemStack(fish(carrying())));
				dropped.setDefaultPickUpDelay();
				level.addFreshEntity(dropped);
				carry(PenguinRules.NOTHING);
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
			fishedAt = -1;
			entityData.set(HUDDLING, false);
			level().broadcastEntityEvent(this, (byte) 18);
			playSound(SoundEvents.PARROT_EAT, 0.8F, 0.9F);
		}
		return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return getType().create(level, EntitySpawnReason.BREEDING) instanceof FloePenguin young ? young : null;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return huddling() ? null : SoundEvents.PARROT_AMBIENT;
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
	protected void playStepSound(BlockPos pos, BlockState state) {
		if (!sliding()) {
			playSound(SoundEvents.CHICKEN_STEP.value(), 0.12F, 1.3F);
		}
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * (isBaby() ? 1.1F : 0.7F);
	}

	@Override
	public int getAmbientSoundInterval() {
		return 200;
	}

	/**
	 * A fishing trip: down to open water, a dive and a swim about, and back up with a fish in its beak, for its friend, a
	 * hungry chick, or itself.
	 */
	private final class Fish extends Goal {
		private @Nullable BlockPos water;
		private int swum;
		private int length;
		private int trip;

		Fish() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if ((tickCount + getId()) % 20 != 0 || huddling()) {
				return false;
			}
			long now = level().getGameTime();
			if (!PenguinRules.fishes(!isBaby(), true, carrying(), fishedAt < 0 ? -1 : now - fishedAt, truster() != null)) {
				return false;
			}
			water = find();
			return water != null;
		}

		@Override
		public boolean canContinueToUse() {
			return trip < PenguinRules.TRIP_TICKS && (water != null || carrying() != PenguinRules.NOTHING);
		}

		@Override
		public void start() {
			swum = 0;
			trip = 0;
			length = PenguinRules.fishLength(getRandom().nextDouble());
			if (water != null) {
				getNavigation().moveTo(water.getX() + 0.5, water.getY(), water.getZ() + 0.5, 1.2);
			}
		}

		@Override
		public void stop() {
			fishedAt = level().getGameTime();
			water = null;
			getNavigation().stop();
		}

		@Override
		public void tick() {
			if (!(level() instanceof ServerLevel level)) {
				return;
			}
			trip++;
			if (water != null) {
				fishing(level);
			} else {
				bringing(level);
			}
		}

		/** Off to the water, then diving about after a fish. */
		private void fishing(ServerLevel level) {
			if (!isInWater()) {
				if (trip % 20 == 0) {
					getNavigation().moveTo(water.getX() + 0.5, water.getY(), water.getZ() + 0.5, 1.2);
				}
				if (trip > PenguinRules.TRIP_TICKS / 2) {
					water = null;
					trip = PenguinRules.TRIP_TICKS;
				}
				return;
			}
			if (swum == 0) {
				playSound(SoundEvents.GENERIC_SPLASH, 0.5F, 1.5F);
			}
			swum++;
			if (swum % 25 == 1) {
				// Darting about under the surface after something.
				BlockPos to = blockPosition().offset(getRandom().nextInt(7) - 3, -getRandom().nextInt(3), getRandom().nextInt(7) - 3);
				if (level.getFluidState(to).is(FluidTags.WATER)) {
					getNavigation().moveTo(to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5, 1.3);
				}
			}
			if (swum % 8 == 0) {
				Fx.send(level, ParticleTypes.BUBBLE, getX(), getY() + 0.3, getZ(), 3, 0.2, 0.2, 0.2, 0.02);
			}
			if (swum >= length) {
				carry(PenguinRules.catchOf(getRandom().nextDouble()));
				water = null;
				trip = 0;
				Fx.send(level, ParticleTypes.SPLASH, getX(), getY() + 0.6, getZ(), 8, 0.3, 0.1, 0.3, 0.1);
				playSound(SoundEvents.FISH_SWIM, 0.6F, 1.2F);
			}
		}

		/** Back out with the fish: to its friend's feet, to a chick, or to a spot on dry land to eat it itself. */
		private void bringing(ServerLevel level) {
			Player friend = truster();
			FloePenguin chick = friend == null ? chick() : null;
			PenguinRules.Mouth mouth = PenguinRules.mouth(friend != null, chick != null);
			Vec3 to = switch (mouth) {
				case FRIEND -> friend.position();
				case CHICK -> chick.position();
				case SELF -> null;
			};
			if (to == null) {
				if (isInWater()) {
					if (trip % 20 == 1) {
						BlockPos shore = shore();
						if (shore != null) {
							getNavigation().moveTo(shore.getX() + 0.5, shore.getY(), shore.getZ() + 0.5, 1.2);
						}
					}
					return;
				}
				getNavigation().stop();
				eat(level);
				return;
			}
			if (position().distanceTo(to) > PenguinRules.HAND_RANGE) {
				if (trip % 10 == 0) {
					getNavigation().moveTo(to.x, to.y, to.z, mouth == PenguinRules.Mouth.FRIEND ? 1.3 : 1.15);
				}
				return;
			}
			getNavigation().stop();
			Item fish = fish(carrying());
			if (mouth == PenguinRules.Mouth.FRIEND) {
				getLookControl().setLookAt(friend, 30, 30);
				ItemEntity gift = new ItemEntity(level, friend.getX(), friend.getY() + 0.1, friend.getZ(), new ItemStack(fish));
				gift.setDefaultPickUpDelay();
				gift.setDeltaMovement(Vec3.ZERO);
				level.addFreshEntity(gift);
				Fx.send(level, ParticleTypes.HEART, getX(), getY() + 0.8, getZ(), 1, 0.1, 0.1, 0.1, 0);
				playSound(SoundEvents.PARROT_AMBIENT, 0.8F, 1.0F);
			} else {
				// Beak to beak, it feeds the chick, which grows a good deal on it.
				getLookControl().setLookAt(chick, 30, 30);
				chick.ageUp(AgeableMob.getSpeedUpSecondsWhenFeeding(-chick.getAge()), true);
				Fx.send(level, new ItemParticleOption(ParticleTypes.ITEM, fish), chick.getX(), chick.getY() + 0.5, chick.getZ(), 6, 0.1, 0.1, 0.1, 0.05);
				Fx.send(level, ParticleTypes.HEART, chick.getX(), chick.getY() + 0.6, chick.getZ(), 1, 0.1, 0.1, 0.1, 0);
				chick.playSound(SoundEvents.PARROT_EAT, 0.8F, 1.6F);
			}
			carry(PenguinRules.NOTHING);
		}

		/** Gulps the fish down whole. */
		private void eat(ServerLevel level) {
			Fx.send(level, new ItemParticleOption(ParticleTypes.ITEM, fish(carrying())), getX(), getY() + 0.7, getZ(), 6, 0.1, 0.1, 0.1, 0.05);
			playSound(SoundEvents.PARROT_EAT, 0.8F, 0.9F);
			heal(2);
			carry(PenguinRules.NOTHING);
		}

		/** Open water near it, with air over it to dive in from. */
		private @Nullable BlockPos find() {
			BlockPos here = blockPosition();
			int r = PenguinRules.FISH_RANGE;
			for (int i = 0; i < 24; i++) {
				BlockPos at = here.offset(getRandom().nextInt(2 * r + 1) - r, getRandom().nextInt(7) - 4, getRandom().nextInt(2 * r + 1) - r);
				if (level().getFluidState(at).is(FluidTags.WATER) && level().getFluidState(at.below()).is(FluidTags.WATER)
					&& level().getBlockState(at.above()).isAir()) {
					return at.immutable();
				}
			}
			return null;
		}

		/** Dry ground near it, to climb out onto. */
		private @Nullable BlockPos shore() {
			BlockPos here = blockPosition();
			for (int i = 0; i < 20; i++) {
				BlockPos at = here.offset(getRandom().nextInt(13) - 6, getRandom().nextInt(5) - 1, getRandom().nextInt(13) - 6);
				if (level().getBlockState(at.below()).isFaceSturdy(level(), at.below(), net.minecraft.core.Direction.UP)
					&& level().getBlockState(at).getFluidState().isEmpty() && level().getBlockState(at).getCollisionShape(level(), at).isEmpty()) {
					return at.immutable();
				}
			}
			return null;
		}
	}

	/** At night it shuffles in among the rest of its colony and dozes there, head tucked in, until morning. */
	private final class Huddle extends Goal {
		private @Nullable Vec3 heart;

		Huddle() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			if ((tickCount + getId()) % 40 != 0 || isInWater() || carrying() != PenguinRules.NOTHING) {
				return false;
			}
			List<FloePenguin> colony = colony();
			if (!PenguinRules.huddles(!level().isBrightOutside(), !isBaby(), colony.size(), truster() != null)) {
				return false;
			}
			heart = heartOf(colony);
			return true;
		}

		@Override
		public boolean canContinueToUse() {
			return !level().isBrightOutside() && truster() == null && carrying() == PenguinRules.NOTHING && !isInWater() && heart != null
				&& (getLastHurtByMob() == null || tickCount - getLastHurtByMobTimestamp() > 60);
		}

		@Override
		public void start() {
			getNavigation().moveTo(heart.x, heart.y, heart.z, 0.9);
		}

		@Override
		public void stop() {
			entityData.set(HUDDLING, false);
			heart = null;
		}

		@Override
		public void tick() {
			if (tickCount % 40 == 0) {
				List<FloePenguin> colony = colony();
				if (colony.isEmpty()) {
					heart = null;
					return;
				}
				heart = heartOf(colony);
			}
			boolean close = position().distanceTo(heart) < PenguinRules.HUDDLE_CLOSE || !level().getEntitiesOfClass(FloePenguin.class,
				getBoundingBox().inflate(0.35), p -> p != FloePenguin.this && !p.isBaby()).isEmpty() && position().distanceTo(heart) < PenguinRules.HUDDLE_CLOSE * 2;
			if (close) {
				getNavigation().stop();
				entityData.set(HUDDLING, true);
				setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
			} else {
				entityData.set(HUDDLING, false);
				if (tickCount % 20 == 0) {
					getNavigation().moveTo(heart.x, heart.y, heart.z, 0.9);
				}
			}
		}

		private List<FloePenguin> colony() {
			return level().getEntitiesOfClass(FloePenguin.class, getBoundingBox().inflate(PenguinRules.HUDDLE_RANGE),
				p -> p != FloePenguin.this && p.isAlive() && !p.isInWater());
		}

		/** The middle of the colony, itself included. */
		private Vec3 heartOf(List<FloePenguin> colony) {
			Vec3 sum = position();
			for (FloePenguin p : colony) {
				sum = sum.add(p.position());
			}
			return sum.scale(1.0 / (colony.size() + 1));
		}
	}

	/** Trusting someone, it waddles (or on the ice, slides) along after them. */
	private final class FollowTruster extends Goal {
		private @Nullable Player player;

		FollowTruster() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			player = truster();
			return player != null && !isBaby() && carrying() == PenguinRules.NOTHING && distanceTo(player) > PenguinRules.FOLLOW_FAR
				&& distanceTo(player) < 24;
		}

		@Override
		public boolean canContinueToUse() {
			return player != null && truster() == player && distanceTo(player) > PenguinRules.FOLLOW_NEAR && distanceTo(player) < 24;
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
