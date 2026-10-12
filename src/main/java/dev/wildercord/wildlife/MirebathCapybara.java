package dev.wildercord.wildlife;

import dev.wildercord.cast.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * A mirebath capybara: a big, round, unhurried rodent of the swamps and jungles, russet-brown, with a blunt square head, tiny
 * ears and no tail to speak of. Every so often it ambles off to the water and settles in for a long soak, sunk to its chin
 * with its eyes shut. Anyone who soaks in the water beside it soaks up its calm and slowly heals.
 * <p>Hand it an apple while it bathes and it balances it on its head; while it wears it, the bath also washes away poison,
 * hunger and nausea. Getting out, it eats the apple.
 * <p>Melon slices and sugar cane tempt it and breed it. Its young climb up onto any grown one's back and ride there a while.
 */
public class MirebathCapybara extends Animal {
	private static final EntityDataAccessor<Boolean> BATHING = SynchedEntityData.defineId(MirebathCapybara.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> APPLE = SynchedEntityData.defineId(MirebathCapybara.class, EntityDataSerializers.BOOLEAN);

	/** When its bath began and ended and how long this one lasts, how long it's sat still in the water, and when it climbed up to ride. */
	private long bathAt = -1;
	private long bathEndedAt = -1;
	private int bathLength = CapybaraRules.BATH_MIN;
	private int still;
	private long rideAt = -1;

	/** Client: how far settled into its bath and how far its apple's up, eased, this tick's and last. */
	public float bathe, batheO, apple, appleO;

	public MirebathCapybara(EntityType<? extends MirebathCapybara> type, Level level) {
		super(type, level);
		setPathfindingMalus(PathType.WATER, 0);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.MOVEMENT_SPEED, 0.2);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(BATHING, false);
		builder.define(APPLE, false);
	}

	public boolean bathing() {
		return entityData.get(BATHING);
	}

	public boolean wearsApple() {
		return entityData.get(APPLE);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new Soak());
		goalSelector.addGoal(2, new PanicGoal(this, 1.4));
		goalSelector.addGoal(3, new BreedGoal(this, 1.0));
		goalSelector.addGoal(4, new TemptGoal(this, 1.1, this::isFood, false));
		goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
		goalSelector.addGoal(6, new GoBathe());
		goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(9, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.MELON_SLICE) || stack.is(Items.SUGAR_CANE);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			batheO = bathe;
			appleO = apple;
			bathe = WildlifeRules.approach(bathe, bathing() ? 1 : 0, 0.06F);
			apple = WildlifeRules.approach(apple, wearsApple() ? 1 : 0, 0.25F);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		if (isBaby() || isPassenger()) {
			ride(level, now);
		}
		boolean inWater = isInWater();
		if (bathing()) {
			if (!CapybaraRules.staysBathing(inWater, now - bathAt, bathLength)) {
				endBath(level, now);
			} else if (tickCount % CapybaraRules.SOAK_EVERY == 0) {
				soak(level);
			}
			return;
		}
		boolean resting = inWater && getNavigation().isDone() && getDeltaMovement().horizontalDistanceSqr() < 0.004;
		still = resting ? still + 1 : 0;
		if (!isPassenger() && CapybaraRules.settles(inWater, still, bathEndedAt < 0 ? -1 : now - bathEndedAt)) {
			bathAt = now;
			bathLength = CapybaraRules.bathLength(getRandom().nextDouble());
			entityData.set(BATHING, true);
			getNavigation().stop();
			playSound(SoundEvents.GENERIC_SPLASH, 0.4F, 0.8F);
			Fx.send(level, ParticleTypes.SPLASH, getX(), getY() + 0.5, getZ(), 14, 0.4, 0.1, 0.4, 0.1);
		}
	}

	/** Out of the bath, and the apple, if it had one, eaten. */
	private void endBath(ServerLevel level, long now) {
		entityData.set(BATHING, false);
		bathEndedAt = now;
		still = 0;
		if (wearsApple()) {
			entityData.set(APPLE, false);
			heal(2);
			playSound(SoundEvents.GENERIC_EAT.value(), 0.6F, 1.1F);
			Fx.send(level, ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.8, getZ(), 6, 0.3, 0.2, 0.3, 0.01);
		}
	}

	/** Its calm reaches whoever's in the water beside it; with its apple up, the water washes them clean, too. */
	private void soak(ServerLevel level) {
		boolean cleansing = wearsApple();
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(CapybaraRules.SOAK_RANGE),
				e -> e != this && e.isAlive() && (e instanceof Player || e instanceof MirebathCapybara))) {
			if (e instanceof Player player && player.isSpectator()) {
				continue;
			}
			if (!CapybaraRules.soaks(true, e.isInWater(), distanceTo(e))) {
				continue;
			}
			e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, CapybaraRules.SOAK_TICKS, 0, true, true), this);
			if (cleansing) {
				e.removeEffect(MobEffects.POISON);
				e.removeEffect(MobEffects.HUNGER);
				e.removeEffect(MobEffects.NAUSEA);
			}
		}
		if (getRandom().nextInt(3) == 0) {
			Fx.send(level, ParticleTypes.BUBBLE_POP, getX(), getY() + 0.6, getZ(), 3, 0.4, 0.05, 0.4, 0.01);
		}
	}

	/** A youngster climbs up onto a grown one's back, and rides there a while. */
	private void ride(ServerLevel level, long now) {
		if (getVehicle() instanceof MirebathCapybara carrier) {
			if (CapybaraRules.hopsDown(isBaby(), rideAt < 0 ? CapybaraRules.RIDE_TICKS : now - rideAt, carrier.hurtTime > 0)) {
				stopRiding();
				rideAt = -1;
			}
			return;
		}
		if (isPassenger() || tickCount % 20 != 0 || getRandom().nextInt(6) != 0) {
			return;
		}
		for (MirebathCapybara carrier : level.getEntitiesOfClass(MirebathCapybara.class, getBoundingBox().inflate(CapybaraRules.CLIMB_RANGE),
				c -> c != this && !c.isBaby() && c.isAlive())) {
			if (CapybaraRules.climbs(isBaby(), isPassenger(), !carrier.isVehicle(), distanceTo(carrier)) && startRiding(carrier)) {
				rideAt = now;
				playSound(SoundEvents.ARMADILLO_LAND, 0.5F, 1.6F);
				return;
			}
		}
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return !isVehicle() && passenger instanceof MirebathCapybara young && young.isBaby();
	}

	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return null;
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		return new Vec3(0, dimensions.height() - (bathing() ? 0.2 : 0.06) * scale, 0.05 * scale)
			.yRot(-getYRot() * ((float) Math.PI / 180F));
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hit = super.hurtServer(level, source, amount);
		if (hit) {
			if (bathing()) {
				endBath(level, level.getGameTime());
			}
			ejectPassengers();
		}
		return hit;
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (held.is(Items.APPLE) && bathing() && !wearsApple() && !isBaby()) {
			if (level() instanceof ServerLevel level) {
				held.consume(1, player);
				entityData.set(APPLE, true);
				playSound(SoundEvents.ARMADILLO_PEEK, 0.6F, 1.2F);
				Fx.send(level, ParticleTypes.HEART, getX(), getY() + 1.0, getZ(), 2, 0.2, 0.1, 0.2, 0.01);
			}
			return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
		}
		return super.mobInteract(player, hand);
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return getType().create(level, EntitySpawnReason.BREEDING) instanceof MirebathCapybara young ? young : null;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return SoundEvents.ARMADILLO_AMBIENT;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ARMADILLO_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return SoundEvents.ARMADILLO_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.ARMADILLO_STEP, 0.12F, 0.8F);
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 1.25F;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 320;
	}

	/** In its bath it holds still, only turning its head now and then. */
	private final class Soak extends Goal {
		Soak() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return bathing();
		}

		@Override
		public void start() {
			getNavigation().stop();
		}

		@Override
		public void tick() {
			setDeltaMovement(getDeltaMovement().multiply(0.2, 1, 0.2));
		}
	}

	/** Dry a while, it ambles off to the nearest water it can find for a soak. */
	private final class GoBathe extends Goal {
		GoBathe() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			if (isBaby() || isPassenger() || isInWater() || bathing() || getRandom().nextInt(reducedTickDelay(80)) != 0) {
				return false;
			}
			if (!CapybaraRules.wantsBath(bathEndedAt < 0 ? -1 : level().getGameTime() - bathEndedAt)) {
				return false;
			}
			BlockPos water = findWater();
			return water != null && getNavigation().moveTo(water.getX() + 0.5, water.getY(), water.getZ() + 0.5, 0.9);
		}

		@Override
		public boolean canContinueToUse() {
			return !getNavigation().isDone() && !isInWater();
		}

		@Override
		public void stop() {
			if (isInWater()) {
				getNavigation().stop();
			}
		}

		private @Nullable BlockPos findWater() {
			BlockPos home = blockPosition();
			int r = CapybaraRules.WATER_SEARCH;
			for (int i = 0; i < 24; i++) {
				BlockPos pos = home.offset(getRandom().nextInt(2 * r + 1) - r, getRandom().nextInt(6) - 3, getRandom().nextInt(2 * r + 1) - r);
				if (level().getFluidState(pos).is(FluidTags.WATER) && level().getBlockState(pos.above()).isAir()) {
					return pos;
				}
			}
			return null;
		}
	}
}
