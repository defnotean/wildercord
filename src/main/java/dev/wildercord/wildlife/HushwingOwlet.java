package dev.wildercord.wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
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
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * A hushwing owlet: a round pale-grey fledgling owl of the pale garden, all fluff, with two little ear tufts, a pale face
 * disc and great round amber eyes. It can't fly yet, only hop and flutter, falling soft. By day it sleeps where it stands,
 * eyes shut and fluffed up; by night it watches, turning its head right round to follow you.
 * <p>The creakings hate it. Whenever one stirs within its sight it fixes it with its great eyes and hoots, and a creaking it
 * stares at can't move, just as when you look at one yourself.
 * <p>Raw rabbit, chicken and cod tempt it and breed it. Fed, it trusts you a while, hops along after you, and at night hoots
 * out whatever is stalking you, so that it shows through the dark a few seconds.
 */
public class HushwingOwlet extends Animal {
	private static final EntityDataAccessor<Boolean> ASLEEP = SynchedEntityData.defineId(HushwingOwlet.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> STARING = SynchedEntityData.defineId(HushwingOwlet.class, EntityDataSerializers.BOOLEAN);

	/** What it stares down (server only); when something last woke it, who fed it last and when, when it last warned them, and its second hoot. */
	private @Nullable Creaking stare;
	private long wokenAt = -1;
	private @Nullable UUID truster;
	private long fedAt = -1;
	private long warnedAt = -1;
	private long hootAgain = -1;

	/** Client: how far asleep, staring and fluttering, eased, this tick's and last. */
	public float sleep, sleepO, staring, staringO, flutter, flutterO;

	public HushwingOwlet(EntityType<? extends HushwingOwlet> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.22);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ASLEEP, false);
		builder.define(STARING, false);
	}

	public boolean asleep() {
		return entityData.get(ASLEEP);
	}

	public boolean staring() {
		return entityData.get(STARING);
	}

	/** Whether any owlet near a creaking is staring it down (so it can't move). */
	public static boolean pinned(Creaking creaking) {
		return !creaking.level().getEntitiesOfClass(HushwingOwlet.class, creaking.getBoundingBox().inflate(OwletRules.STARE_RANGE),
			owlet -> owlet.stare == creaking).isEmpty();
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new Asleep());
		goalSelector.addGoal(2, new PanicGoal(this, 1.4));
		goalSelector.addGoal(3, new Stare());
		goalSelector.addGoal(4, new BreedGoal(this, 1.0));
		goalSelector.addGoal(5, new TemptGoal(this, 1.1, this::isFood, false));
		goalSelector.addGoal(6, new FollowTruster());
		goalSelector.addGoal(7, new FollowParentGoal(this, 1.1));
		goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(10, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.RABBIT) || stack.is(Items.CHICKEN) || stack.is(Items.COD);
	}

	/** An owl's neck: it turns its head right round over its back before its body follows. */
	@Override
	public int getMaxHeadYRot() {
		return 160;
	}

	@Override
	public int getHeadRotSpeed() {
		return 25;
	}

	@Override
	public float getWalkTargetValue(BlockPos pos, LevelReader level) {
		// The dark under the pale oaks doesn't put it off, only the ground.
		return level.getBlockState(pos.below()).is(OwletContent.GROUND) ? 10 : 0;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			sleepO = sleep;
			staringO = staring;
			flutterO = flutter;
			sleep = WildlifeRules.approach(sleep, asleep() ? 1 : 0, 0.06F);
			staring = WildlifeRules.approach(staring, staring() ? 1 : 0, 0.15F);
			flutter = WildlifeRules.approach(flutter, onGround() || isInWater() ? 0 : 1, 0.25F);
		}
	}

	/** It can't fly yet, but flutters: it falls soft, and never hard enough to hurt. */
	@Override
	public void aiStep() {
		super.aiStep();
		Vec3 motion = getDeltaMovement();
		if (!onGround() && !isInWater() && motion.y < 0) {
			setDeltaMovement(motion.x, OwletRules.flutter(motion.y), motion.z);
		}
	}

	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		if (tickCount % 20 == 0) {
			boolean sleepy = OwletRules.sleeps(!level.isDarkOutside(), wokenAt < 0 ? -1 : now - wokenAt)
				&& onGround() && !isInWater() && truster() == null && stare == null;
			if (sleepy != asleep()) {
				entityData.set(ASLEEP, sleepy);
				getNavigation().stop();
				if (!sleepy) {
					hoot(now);
				}
			}
		}
		if (tickCount % 10 == 0) {
			stare = watch(level);
		}
		entityData.set(STARING, stare != null);
		if (hootAgain >= 0 && now >= hootAgain) {
			hootAgain = -1;
			playSound(SoundEvents.NOTE_BLOCK_FLUTE.value(), 0.7F, 0.53F);
		}
		Player friend = truster();
		if (friend != null && tickCount % 20 == 5) {
			warn(friend, now);
		}
	}

	/** The nearest creaking it can see to stare down, if any. */
	private @Nullable Creaking watch(ServerLevel level) {
		if (asleep() || isBaby()) {
			return null;
		}
		Creaking best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Creaking creaking : level.getEntitiesOfClass(Creaking.class, getBoundingBox().inflate(OwletRules.STARE_RANGE), Creaking::isActive)) {
			double d = distanceTo(creaking);
			if (d < bestDistance && OwletRules.stares(false, d, hasLineOfSight(creaking))) {
				best = creaking;
				bestDistance = d;
			}
		}
		if (best != null && best != stare) {
			hoot(level.getGameTime());
		}
		return best;
	}

	/** Whatever is stalking its friend, hooted out so it shows through the dark a few seconds. */
	private void warn(Player friend, long now) {
		List<Mob> stalkers = level().getEntitiesOfClass(Mob.class, friend.getBoundingBox().inflate(OwletRules.WARN_RANGE),
			mob -> mob instanceof Enemy && mob.isAlive() && mob.getTarget() == friend);
		if (!OwletRules.warns(asleep(), stalkers.size(), warnedAt < 0 ? -1 : now - warnedAt)) {
			return;
		}
		warnedAt = now;
		for (Mob mob : stalkers) {
			mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, OwletRules.WARN_GLOW, 0, false, false), this);
		}
		getLookControl().setLookAt(stalkers.get(0), 60, 30);
		hoot(now);
	}

	/** Hoo-hoo: two low notes. */
	private void hoot(long now) {
		playSound(SoundEvents.NOTE_BLOCK_FLUTE.value(), 0.7F, 0.6F);
		hootAgain = now + 7;
	}

	private @Nullable Player truster() {
		if (truster == null || !OwletRules.trusts(level().getGameTime() - fedAt)) {
			return null;
		}
		Player player = level().getPlayerByUUID(truster);
		return player != null && player.isAlive() && !player.isSpectator() ? player : null;
	}

	/** Up, whatever the hour, for a while. */
	private void wake() {
		wokenAt = level().getGameTime();
		if (asleep()) {
			entityData.set(ASLEEP, false);
			playSound(SoundEvents.PARROT_HURT, 0.3F, 1.6F);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hit = super.hurtServer(level, source, amount);
		if (hit) {
			wake();
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
			// Fed, it wakes and trusts you.
			truster = player.getUUID();
			fedAt = level().getGameTime();
			wake();
			level().broadcastEntityEvent(this, (byte) 18);
			playSound(SoundEvents.GENERIC_EAT.value(), 0.4F, 1.6F);
		}
		return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return getType().create(level, EntitySpawnReason.BREEDING) instanceof HushwingOwlet young ? young : null;
	}

	// ------------------------------------------------------------------ sound: soft chirrs and, now and then, a hoot

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return asleep() ? null : SoundEvents.PARROT_FLY;
	}

	@Override
	public void playAmbientSound() {
		if (!asleep() && !level().isClientSide() && random.nextInt(3) == 0) {
			hoot(level().getGameTime());
			return;
		}
		super.playAmbientSound();
	}

	@Override
	public int getAmbientSoundInterval() {
		return 240;
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
		playSound(SoundEvents.PARROT_STEP, 0.1F, 1.4F);
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 1.3F;
	}

	@Override
	protected float getSoundVolume() {
		return 0.5F;
	}

	/** Asleep: it doesn't move, look about or jump until it wakes. */
	private final class Asleep extends Goal {
		Asleep() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return asleep();
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

	/** A creaking in sight: it stands stock still and fixes it with its eyes. */
	private final class Stare extends Goal {
		Stare() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return stare != null;
		}

		@Override
		public void start() {
			getNavigation().stop();
		}

		@Override
		public void tick() {
			Creaking creaking = stare;
			if (creaking != null) {
				getLookControl().setLookAt(creaking.getX(), creaking.getEyeY(), creaking.getZ(), 60, 60);
			}
		}
	}

	/** Trusting someone, it hops along after them. */
	private final class FollowTruster extends Goal {
		private @Nullable Player player;

		FollowTruster() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			player = truster();
			return player != null && !isBaby() && distanceTo(player) > OwletRules.FOLLOW_FAR && distanceTo(player) < 24;
		}

		@Override
		public boolean canContinueToUse() {
			return player != null && truster() == player && distanceTo(player) > OwletRules.FOLLOW_NEAR && distanceTo(player) < 24;
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
