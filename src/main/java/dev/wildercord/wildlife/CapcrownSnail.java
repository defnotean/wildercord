package dev.wildercord.wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A capcrown snail: a plump grey-violet snail of the mushroom fields whose shell has grown a mushroom cap, red and white-spotted
 * or broad and brown. It glides slowly over the mycelium with its eye stalks swaying, stops to nibble, and sheds a drift of
 * spores off its cap now and then; in the rain it gets about quicker. Run at it, let something hostile near, strike it or poke
 * it in the eye and it draws into its shell with a puff of spores, where blows hardly touch it; after a while its eyes come out
 * first to look, then the rest of it.
 * <p>Shears take its cap (two to four mushrooms of its colour) and leave the shell bare; the cap grows back in a few minutes,
 * sooner if you feed it. Mushrooms tempt and breed it; its young carry only little nubs where a cap will be. Fed, it trusts
 * you a while and glides after you, as fast as a snail can.
 */
public class CapcrownSnail extends Animal implements Shearable {
	/** Which cap it grows: red or brown. */
	public static final int RED = 0, BROWN = 1;
	private static final EntityDataAccessor<Integer> COLOUR = SynchedEntityData.defineId(CapcrownSnail.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> CAPPED = SynchedEntityData.defineId(CapcrownSnail.class, EntityDataSerializers.BOOLEAN);
	/** In its shell with this many ticks to go, 0 when it isn't. */
	private static final EntityDataAccessor<Integer> HIDE = SynchedEntityData.defineId(CapcrownSnail.class, EntityDataSerializers.INT);
	private static final double SPEED = 0.12;

	/** When a shorn cap grows back (-1 for not shorn), and who fed it last and when. */
	private long regrowAt = -1;
	private @Nullable UUID truster;
	private long fedAt = -1;

	/** Client: how far it is drawn in (1 all the way, less while peeking), eased, this tick's and last. */
	public float hide, hideO;

	public CapcrownSnail(EntityType<? extends CapcrownSnail> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, SPEED).add(Attributes.ARMOR, 2.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(COLOUR, RED);
		builder.define(CAPPED, true);
		builder.define(HIDE, 0);
	}

	public int colour() {
		return entityData.get(COLOUR);
	}

	public boolean capped() {
		return entityData.get(CAPPED);
	}

	public int hideLeft() {
		return entityData.get(HIDE);
	}

	public boolean hidden() {
		return hideLeft() > 0;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new Hidden());
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(3, new BreedGoal(this, 1.0));
		goalSelector.addGoal(4, new TemptGoal(this, 1.2, this::isFood, false));
		goalSelector.addGoal(5, new FollowTruster());
		goalSelector.addGoal(6, new FollowParentGoal(this, 1.1));
		goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 5.0F));
		goalSelector.addGoal(9, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.RED_MUSHROOM) || stack.is(Items.BROWN_MUSHROOM);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			hideO = hide;
			int left = hideLeft();
			hide = WildlifeRules.approach(hide, left <= 0 ? 0 : SnailRules.peeking(left) ? 0.6F : 1, left > 0 && !SnailRules.peeking(left) ? 0.3F : 0.06F);
			// A drift of spores off its cap now and then.
			if (capped() && !isBaby() && random.nextInt(30) == 0) {
				level().addParticle(ParticleTypes.MYCELIUM, getRandomX(0.4), getY() + 0.75, getRandomZ(0.4), 0, 0, 0);
			}
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		int left = hideLeft();
		if (left > 0) {
			entityData.set(HIDE, left - 1);
			if (left == 1) {
				playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.5F, 1.4F);
			}
		} else if (tickCount % 5 == 0 && startled(level)) {
			hideAway(level, SnailRules.HIDE_TICKS);
		}
		if (!capped() && regrowAt >= 0 && now >= regrowAt) {
			regrowAt = -1;
			entityData.set(CAPPED, true);
			level.sendParticles(ParticleTypes.MYCELIUM, getX(), getY() + 0.8, getZ(), 14, 0.3, 0.15, 0.3, 0.02);
			playSound(SoundEvents.FUNGUS_PLACE, 0.6F, 1.3F);
		}
		if (tickCount % 20 == 0) {
			getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(level.isRainingAt(blockPosition()) ? SPEED * SnailRules.RAIN_SPEED : SPEED);
		}
	}

	/** Someone running close by (who it doesn't trust), or something hostile right beside it. */
	private boolean startled(ServerLevel level) {
		for (Player player : level.players()) {
			if (!player.isSpectator() && SnailRules.startles(player.isSprinting(), distanceTo(player), trusts(player))) {
				return true;
			}
		}
		return !level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SnailRules.ENEMY_RANGE), e -> e instanceof Enemy && e.isAlive()).isEmpty();
	}

	/** Into its shell with a puff of spores, for {@code ticks} (or longer, if it's already in). */
	private void hideAway(ServerLevel level, int ticks) {
		if (!hidden()) {
			playSound(SoundEvents.SLIME_SQUISH, 0.5F, 1.7F);
			level.sendParticles(ParticleTypes.MYCELIUM, getX(), getY() + 0.5, getZ(), 10, 0.3, 0.2, 0.3, 0.03);
		}
		entityData.set(HIDE, Math.max(hideLeft(), ticks));
		getNavigation().stop();
	}

	private boolean trusts(Player player) {
		return player.getUUID().equals(truster) && SnailRules.trusts(level().getGameTime() - fedAt);
	}

	private @Nullable Player truster() {
		if (truster == null || !SnailRules.trusts(level().getGameTime() - fedAt)) {
			return null;
		}
		Player player = level().getPlayerByUUID(truster);
		return player != null && player.isAlive() && !player.isSpectator() ? player : null;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hit = super.hurtServer(level, source, SnailRules.blunted(amount, hidden()));
		if (hit && isAlive()) {
			hideAway(level, SnailRules.HIDE_TICKS);
		}
		return hit;
	}

	// ------------------------------------------------------------------ shearing: its cap, which grows back

	@Override
	public boolean readyForShearing() {
		return isAlive() && !isBaby() && capped();
	}

	@Override
	public void shear(ServerLevel level, SoundSource source, ItemStack tool) {
		level.playSound(null, this, SoundEvents.MOOSHROOM_SHEAR, source, 1.0F, 1.2F);
		entityData.set(CAPPED, false);
		regrowAt = level.getGameTime() + SnailRules.REGROW_TICKS;
		int count = SnailRules.yield(random.nextDouble());
		spawnAtLocation(level, new ItemStack(colour() == BROWN ? Items.BROWN_MUSHROOM : Items.RED_MUSHROOM, count));
		level.sendParticles(ParticleTypes.MYCELIUM, getX(), getY() + 0.8, getZ(), 16, 0.3, 0.2, 0.3, 0.04);
		hideAway(level, SnailRules.POKE_TICKS);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (held.is(Items.SHEARS) && readyForShearing()) {
			if (level() instanceof ServerLevel level) {
				shear(level, SoundSource.PLAYERS, held);
				gameEvent(GameEvent.SHEAR, player);
				held.hurtAndBreak(1, player, hand);
			}
			return InteractionResult.SUCCESS;
		}
		// A poke in the eye: it pulls in, briefly.
		if (held.isEmpty() && !hidden()) {
			if (level() instanceof ServerLevel level) {
				hideAway(level, SnailRules.POKE_TICKS);
			}
			return InteractionResult.SUCCESS;
		}
		if (!isFood(held)) {
			return super.mobInteract(player, hand);
		}
		InteractionResult result = super.mobInteract(player, hand);
		if (!level().isClientSide()) {
			if (!result.consumesAction()) {
				held.consume(1, player);
			}
			// Fed, it trusts you: it comes out, hearts, and a shorn cap comes back sooner.
			truster = player.getUUID();
			fedAt = level().getGameTime();
			if (regrowAt >= 0) {
				regrowAt = SnailRules.hastened(regrowAt, fedAt);
			}
			entityData.set(HIDE, 0);
			level().broadcastEntityEvent(this, (byte) 18);
			playSound(SoundEvents.GENERIC_EAT.value(), 0.5F, 1.5F);
		}
		return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}

	// ------------------------------------------------------------------ colour, breeding and saving

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData group) {
		entityData.set(COLOUR, random.nextInt(3) == 0 ? BROWN : RED);
		return super.finalizeSpawn(level, difficulty, reason, group);
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		if (getType().create(level, EntitySpawnReason.BREEDING) instanceof CapcrownSnail young) {
			int colour = partner instanceof CapcrownSnail other && random.nextBoolean() ? other.colour() : colour();
			young.entityData.set(COLOUR, colour);
			return young;
		}
		return null;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Cap", colour());
		output.putBoolean("Capped", capped());
		output.putLong("Regrow", regrowAt);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(COLOUR, input.getIntOr("Cap", RED) == BROWN ? BROWN : RED);
		entityData.set(CAPPED, input.getBooleanOr("Capped", true));
		regrowAt = input.getLongOr("Regrow", -1);
	}

	// ------------------------------------------------------------------ sound: soft squelches

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SLIME_HURT_SMALL;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return SoundEvents.SLIME_DEATH_SMALL;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		if (random.nextInt(3) == 0) {
			playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.08F, 1.6F);
		}
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 1.4F;
	}

	@Override
	protected float getSoundVolume() {
		return 0.5F;
	}

	/** In its shell: it doesn't move, look about or jump until it comes out. */
	private final class Hidden extends Goal {
		Hidden() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return hidden();
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

	/** Trusting someone, it glides along after them. */
	private final class FollowTruster extends Goal {
		private @Nullable Player player;

		FollowTruster() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			player = truster();
			return player != null && !isBaby() && distanceTo(player) > SnailRules.FOLLOW_FAR && distanceTo(player) < 20;
		}

		@Override
		public boolean canContinueToUse() {
			return player != null && truster() == player && distanceTo(player) > SnailRules.FOLLOW_NEAR && distanceTo(player) < 20;
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
