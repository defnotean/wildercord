package dev.wildercord.wildlife;

import dev.wildercord.cast.Fx;
import dev.wildercord.spell.ClimateRules;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A blossom tanuki: a plump little raccoon-dog of the cherry groves, grey-brown with a dark mask, a round belly and a big
 * bushy ringed tail. Run at it, or let a wolf, fox or monster near, and it slaps a cherry leaf on its head and, in a puff of
 * petals, turns into a little blossom bush; it stays one until a while after the danger has gone, then pops back out. Hurt
 * it as a bush and it bursts out and bolts.
 * <p>Under a full moon the tanukis sit up and drum their bellies, pom-pom, pom-pom, all night.
 * <p>Sweet berries and apples tempt it and breed it. Fed, it trusts you a while and trots after you, and if you crouch beside
 * it a moment it hops up and puts its leaf on your head: you turn unseen for ten seconds.
 */
public class BlossomTanuki extends Animal {
	/** Its form: itself, itself with a leaf on its head about to turn, or a bush. */
	public static final int SELF = 0, LEAF = 1, BUSH = 2;

	private static final EntityDataAccessor<Integer> FORM = SynchedEntityData.defineId(BlossomTanuki.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DRUMMING = SynchedEntityData.defineId(BlossomTanuki.class, EntityDataSerializers.BOOLEAN);

	/** When its form last changed and when it last turned back, who fed it and when, how long they've crouched by it, and its last trick and beat. */
	private long formAt = -1;
	private long turnedBackAt = -1;
	private @Nullable UUID truster;
	private long fedAt = -1;
	private int crouched;
	private long trickAt = -1;
	private int beat;

	/** Client: how far turned to a bush, how far sat up drumming, and the leaf on its head, eased, this tick's and last. */
	public float bush, bushO, drum, drumO, leaf, leafO;

	public BlossomTanuki(EntityType<? extends BlossomTanuki> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.24);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(FORM, SELF);
		builder.define(DRUMMING, false);
	}

	public int form() {
		return entityData.get(FORM);
	}

	public boolean drumming() {
		return entityData.get(DRUMMING);
	}

	private boolean hidden() {
		return form() != SELF;
	}

	private void setForm(int form) {
		entityData.set(FORM, form);
		formAt = level().getGameTime();
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new Still());
		goalSelector.addGoal(2, new PanicGoal(this, 1.5));
		goalSelector.addGoal(3, new BreedGoal(this, 1.0));
		goalSelector.addGoal(4, new TemptGoal(this, 1.1, this::isFood, false));
		goalSelector.addGoal(5, new FollowTruster());
		goalSelector.addGoal(6, new FollowParentGoal(this, 1.1));
		goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(9, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.SWEET_BERRIES) || stack.is(Items.APPLE);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			bushO = bush;
			drumO = drum;
			leafO = leaf;
			bush = WildlifeRules.approach(bush, form() == BUSH ? 1 : 0, 0.34F);
			drum = WildlifeRules.approach(drum, drumming() ? 1 : 0, 0.12F);
			leaf = WildlifeRules.approach(leaf, form() == LEAF ? 1 : 0, 0.3F);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		switch (form()) {
			case LEAF -> {
				if (now - formAt >= TanukiRules.LEAF_TICKS) {
					turn(level, BUSH);
				}
			}
			case BUSH -> {
				if (tickCount % 10 == 0 && !TanukiRules.staysBush(now - formAt, threat(level, TanukiRules.THREAT_RANGE) != null)) {
					turn(level, SELF);
					turnedBackAt = now;
				}
			}
			default -> {
				if (tickCount % 5 == 0 && !isBaby() && onGround()) {
					Entity threat = threat(level, TanukiRules.STARTLE_RANGE);
					if (threat != null && TanukiRules.startled(distanceTo(threat), true, turnedBackAt < 0 ? -1 : now - turnedBackAt)) {
						startle(threat);
					}
				}
			}
		}
		drum(level, now);
		Player friend = truster();
		if (friend != null && !hidden()) {
			crouched = friend.isCrouching() && distanceTo(friend) <= TanukiRules.TRICK_RANGE ? crouched + 1 : 0;
			if (TanukiRules.tricks(true, crouched, distanceTo(friend), trickAt < 0 ? -1 : now - trickAt)) {
				trick(level, friend, now);
			}
		} else {
			crouched = 0;
		}
	}

	/** The nearest thing near enough that frightens it: someone running (not its friend), a wolf, a fox, or a monster. */
	private @Nullable Entity threat(ServerLevel level, double range) {
		Player friend = truster();
		Entity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(range), e -> e != this && e.isAlive())) {
			boolean frightening = e instanceof Player player
				? player != friend && player.isSprinting() && !player.isSpectator()
				: e instanceof Enemy || e instanceof net.minecraft.world.entity.animal.wolf.Wolf || e instanceof net.minecraft.world.entity.animal.fox.Fox;
			double d = distanceTo(e);
			if (frightening && d <= range && d < bestDistance) {
				best = e;
				bestDistance = d;
			}
		}
		return best;
	}

	/** A leaf on its head, a freeze, and then the bush. */
	private void startle(Entity threat) {
		setForm(LEAF);
		entityData.set(DRUMMING, false);
		getNavigation().stop();
		getLookControl().setLookAt(threat, 60, 30);
		playSound(SoundEvents.FOX_SNIFF, 0.6F, 1.5F);
	}

	private void turn(ServerLevel level, int form) {
		setForm(form);
		Fx.send(level, ParticleTypes.CHERRY_LEAVES, getX(), getY() + 0.4, getZ(), 16, 0.35, 0.3, 0.35, 0.02);
		Fx.send(level, ParticleTypes.POOF, getX(), getY() + 0.3, getZ(), 6, 0.25, 0.2, 0.25, 0.01);
		playSound(SoundEvents.CHERRY_LEAVES_BREAK, 0.8F, form == BUSH ? 1.3F : 1.0F);
	}

	/** Under the full moon, sat up, drumming its belly. */
	private void drum(ServerLevel level, long now) {
		boolean night = level.isDarkOutside() && !level.dimensionType().hasFixedTime();
		boolean should = !isBaby() && onGround() && !isInWater() && getNavigation().isDone() && truster() == null
			&& TanukiRules.drums(night, ClimateRules.moonPhase(level.getOverworldClockTime()), hidden());
		if (should != drumming()) {
			entityData.set(DRUMMING, should);
		}
		if (should && (now + getId()) % TanukiRules.BEAT_TICKS == 0) {
			playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 0.5F, TanukiRules.beatPitch(beat++));
		}
	}

	/** It hops up and puts its leaf on its friend's head. */
	private void trick(ServerLevel level, Player friend, long now) {
		trickAt = now;
		crouched = 0;
		friend.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, TanukiRules.TRICK_TICKS, 0, false, true), this);
		Vec3 head = friend.getEyePosition().add(0, 0.4, 0);
		Fx.send(level, ParticleTypes.CHERRY_LEAVES, head.x, head.y, head.z, 20, 0.3, 0.3, 0.3, 0.02);
		Fx.send(level, ParticleTypes.POOF, friend.getX(), friend.getY() + 1, friend.getZ(), 10, 0.3, 0.5, 0.3, 0.01);
		playSound(SoundEvents.CHERRY_LEAVES_BREAK, 0.8F, 1.4F);
		setDeltaMovement(getDeltaMovement().add(0, 0.35, 0));
	}

	private @Nullable Player truster() {
		if (truster == null || !TanukiRules.trusts(level().getGameTime() - fedAt)) {
			return null;
		}
		Player player = level().getPlayerByUUID(truster);
		return player != null && player.isAlive() && !player.isSpectator() ? player : null;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hit = super.hurtServer(level, source, amount);
		if (hit) {
			entityData.set(DRUMMING, false);
			if (form() != SELF) {
				// Found out: it bursts from the bush and bolts.
				turn(level, SELF);
				turnedBackAt = level.getGameTime();
			}
		}
		return hit;
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (!isFood(held) || hidden()) {
			return super.mobInteract(player, hand);
		}
		InteractionResult result = super.mobInteract(player, hand);
		if (!level().isClientSide()) {
			if (!result.consumesAction()) {
				held.consume(1, player);
			}
			truster = player.getUUID();
			fedAt = level().getGameTime();
			level().broadcastEntityEvent(this, (byte) 18);
			playSound(SoundEvents.FOX_EAT, 0.6F, 1.3F);
		}
		return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return getType().create(level, EntitySpawnReason.BREEDING) instanceof BlossomTanuki young ? young : null;
	}

	@Override
	public boolean isPushable() {
		return !hidden() && super.isPushable();
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return hidden() || drumming() ? null : SoundEvents.FOX_AMBIENT;
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
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.FOX_SNIFF, 0.05F, 1.6F);
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 1.15F;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 200;
	}

	/** A leaf on its head, a bush, or drumming: it holds still. */
	private final class Still extends Goal {
		Still() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return hidden() || drumming();
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

	/** Trusting someone, it trots along after them. */
	private final class FollowTruster extends Goal {
		private @Nullable Player player;

		FollowTruster() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			player = truster();
			return player != null && !isBaby() && distanceTo(player) > TanukiRules.FOLLOW_FAR && distanceTo(player) < 24;
		}

		@Override
		public boolean canContinueToUse() {
			return player != null && truster() == player && distanceTo(player) > TanukiRules.FOLLOW_NEAR && distanceTo(player) < 24;
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
