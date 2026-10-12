package dev.wildercord.wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A snowcap stoat: a long, slender little hunter of the cold country, white as the snow in winter places and brown above a cream
 * belly elsewhere, the tip of its tail black either way. It bounds about, and now and then (or when someone it doesn't know comes
 * near) it stands up tall on its hind legs to look about. It hunts rabbits: it creeps up, then breaks into a war dance of
 * twisting hops that holds the rabbit spellbound, then darts in. Sometimes it dances for no reason at all. On snow it dives in
 * headfirst, tunnels a little way and pops out somewhere else; struck on snow, that is how it gets away.
 * <p>Eggs (or raw chicken) tempt it and breed it. Feed one and it trusts you a while: hearts, it bounds along after you and no
 * longer stands up to stare at you.
 */
public class SnowcapStoat extends Animal {
	/** What it is doing ({@link StoatRules#IDLE} and so on), how long that has to go, and its coat. */
	private static final EntityDataAccessor<Integer> POSE = SynchedEntityData.defineId(SnowcapStoat.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> LEFT = SynchedEntityData.defineId(SnowcapStoat.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> WINTER = SynchedEntityData.defineId(SnowcapStoat.class, EntityDataSerializers.BOOLEAN);

	private @Nullable UUID truster;
	private long fedAt = -1, huntedAt = -StoatRules.HUNT_REST, standAgain;
	private boolean hunting;

	/** Client: how far it stands up, how far into its dance, how far down in the snow, eased, this tick's and last. */
	public float stand, standO, dance, danceO, dive, diveO;

	public SnowcapStoat(EntityType<? extends SnowcapStoat> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.3);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(POSE, StoatRules.IDLE);
		builder.define(LEFT, 0);
		builder.define(WINTER, true);
	}

	public int pose() {
		return entityData.get(POSE);
	}

	public int poseLeft() {
		return entityData.get(LEFT);
	}

	public boolean winter() {
		return entityData.get(WINTER);
	}

	private boolean buried() {
		return pose() == StoatRules.DIVE && StoatRules.buried(poseLeft());
	}

	private void pose(int pose, int ticks) {
		entityData.set(POSE, pose);
		entityData.set(LEFT, ticks);
		if (pose != StoatRules.IDLE) {
			getNavigation().stop();
		}
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new Posed());
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(2, new PanicGoal(this, 1.6));
		goalSelector.addGoal(3, new BreedGoal(this, 1.0));
		goalSelector.addGoal(4, new TemptGoal(this, 1.1, this::isFood, false));
		goalSelector.addGoal(5, new Hunt());
		goalSelector.addGoal(6, new FollowTruster());
		goalSelector.addGoal(7, new FollowParentGoal(this, 1.2));
		goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.9));
		goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(10, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.EGG) || stack.is(Items.CHICKEN);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			int pose = pose();
			standO = stand;
			stand = WildlifeRules.approach(stand, pose == StoatRules.STAND ? 1 : 0, 0.18F);
			danceO = dance;
			dance = WildlifeRules.approach(dance, pose == StoatRules.DANCE ? 1 : 0, 0.2F);
			diveO = dive;
			dive = WildlifeRules.approach(dive, pose == StoatRules.DIVE && StoatRules.buried(poseLeft()) ? 1 : 0, 0.22F);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		if (tickCount % 100 == 1) {
			BlockPos at = blockPosition();
			entityData.set(WINTER, StoatRules.winterCoat(level.getBiome(at).value().coldEnoughToSnow(at, level.getSeaLevel())));
		}
		int pose = pose(), left = poseLeft();
		if (pose != StoatRules.IDLE) {
			if (pose == StoatRules.DIVE) {
				dig(level, left);
			}
			if (left <= 1) {
				pose(StoatRules.IDLE, 0);
			} else {
				entityData.set(LEFT, left - 1);
			}
			return;
		}
		if (hunting || isBaby() && random.nextInt(3) != 0) {
			return;
		}
		if (tickCount % 5 == 0 && now >= standAgain && looker(level) != null) {
			stand(now);
		} else if (random.nextInt(StoatRules.STAND_ODDS) == 0) {
			stand(now);
		} else if (random.nextInt(StoatRules.PLAY_ODDS) == 0) {
			dance();
		} else if (random.nextInt(StoatRules.DIVE_ODDS) == 0 && StoatRules.dives(isBaby(), onSnow(blockPosition()))) {
			pose(StoatRules.DIVE, StoatRules.DIVE_TICKS);
		}
	}

	/** Someone it doesn't trust near enough to stand up and look at. */
	private @Nullable Player looker(ServerLevel level) {
		for (Player player : level.players()) {
			if (!player.isSpectator() && StoatRules.perks(distanceTo(player), trusts(player))) {
				return player;
			}
		}
		return null;
	}

	private void stand(long now) {
		pose(StoatRules.STAND, StoatRules.STAND_TICKS);
		standAgain = now + StoatRules.STAND_TICKS * 4L;
		playSound(SoundEvents.FOX_SNIFF, 0.5F, 1.8F);
	}

	private void dance() {
		pose(StoatRules.DANCE, StoatRules.DANCE_TICKS);
		playSound(SoundEvents.FOX_AGGRO, 0.6F, 1.9F);
	}

	/** Through a dive: in headfirst, under the snow to somewhere a little way off, then out again, snow flying each time. */
	private void dig(ServerLevel level, int left) {
		if (left == StoatRules.DIVE_TICKS - 6 || left == 8) {
			puff(level);
			playSound(left == 8 ? SoundEvents.POWDER_SNOW_BREAK : SoundEvents.SNOW_BREAK, 0.8F, 1.3F);
		}
		if (left == StoatRules.DIVE_SHIFT) {
			for (int i = 0; i < 12; i++) {
				BlockPos to = blockPosition().offset(random.nextInt(StoatRules.DIVE_REACH * 2 + 1) - StoatRules.DIVE_REACH, random.nextInt(3) - 1,
					random.nextInt(StoatRules.DIVE_REACH * 2 + 1) - StoatRules.DIVE_REACH);
				double x = to.getX() + 0.5, z = to.getZ() + 0.5;
				if (onSnow(to) && level.noCollision(this, getBoundingBox().move(x - getX(), to.getY() - getY(), z - getZ()))) {
					teleportTo(x, to.getY(), z);
					break;
				}
			}
		} else if (StoatRules.buried(left) && left % 4 == 0) {
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNOW_BLOCK.defaultBlockState()), getX(), getY() + 0.1, getZ(), 2, 0.15, 0.02, 0.15, 0.02);
		}
	}

	private void puff(ServerLevel level) {
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNOW_BLOCK.defaultBlockState()), getX(), getY() + 0.2, getZ(), 18, 0.3, 0.2, 0.3, 0.1);
		level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.3, getZ(), 6, 0.3, 0.2, 0.3, 0.03);
	}

	/** Whether the ground here is snow it can dive into. */
	private boolean onSnow(BlockPos pos) {
		BlockState at = level().getBlockState(pos), below = level().getBlockState(pos.below());
		return at.is(Blocks.SNOW) || at.is(Blocks.POWDER_SNOW) || at.isAir() && (below.is(Blocks.SNOW_BLOCK) || below.is(Blocks.POWDER_SNOW));
	}

	private boolean trusts(Player player) {
		return player.getUUID().equals(truster) && StoatRules.trusts(level().getGameTime() - fedAt);
	}

	private @Nullable Player truster() {
		if (truster == null || !StoatRules.trusts(level().getGameTime() - fedAt)) {
			return null;
		}
		Player player = level().getPlayerByUUID(truster);
		return player != null && player.isAlive() && !player.isSpectator() ? player : null;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		// Under the snow, nothing reaches it.
		if (buried() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return false;
		}
		boolean hit = super.hurtServer(level, source, amount);
		// Struck on snow, it's gone: in headfirst and out somewhere else.
		if (hit && isAlive() && pose() != StoatRules.DIVE && StoatRules.dives(isBaby(), onSnow(blockPosition()))) {
			hunting = false;
			pose(StoatRules.DIVE, StoatRules.DIVE_TICKS);
		} else if (hit && pose() != StoatRules.DIVE) {
			pose(StoatRules.IDLE, 0);
		}
		return hit;
	}

	@Override
	public boolean isPushable() {
		return !buried() && super.isPushable();
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
			// Fed, it trusts you: hearts, and a happy little hop or two.
			truster = player.getUUID();
			fedAt = level().getGameTime();
			if (pose() == StoatRules.STAND) {
				pose(StoatRules.IDLE, 0);
			}
			level().broadcastEntityEvent(this, (byte) 18);
			playSound(SoundEvents.FOX_EAT, 0.7F, 1.7F);
		}
		return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}

	// ------------------------------------------------------------------ breeding: a kit

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return getType().create(level, EntitySpawnReason.BREEDING) instanceof SnowcapStoat kit ? kit : null;
	}

	// ------------------------------------------------------------------ sound: chitters and squeaks

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return buried() ? null : SoundEvents.FOX_AMBIENT;
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
		return super.getVoicePitch() * 1.6F;
	}

	@Override
	protected float getSoundVolume() {
		return 0.5F;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 240;
	}

	/** Standing up, in the snow, or dancing for the fun of it: it holds still where it is (the hunt's dance is the hunt's own). */
	private final class Posed extends Goal {
		Posed() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return pose() != StoatRules.IDLE && !hunting;
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

	/**
	 * A hunt: it creeps up on a rabbit, dances at it (the rabbit held spellbound, quite still, for as long as the dance lasts), then
	 * darts in and bites. Gives up if the rabbit gets too far away or it takes too long.
	 */
	private final class Hunt extends Goal {
		private @Nullable Rabbit rabbit;
		private int ticks, danceEnd;
		private boolean danced;

		Hunt() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (pose() != StoatRules.IDLE || tickCount % 20 != 0 || !(level() instanceof ServerLevel level)) {
				return false;
			}
			if (StoatRules.hunts(isBaby(), 0, level.getGameTime() - huntedAt)) {
				rabbit = level.getNearestEntity(Rabbit.class, net.minecraft.world.entity.ai.targeting.TargetingConditions.forCombat().range(StoatRules.HUNT_RANGE),
					SnowcapStoat.this, getX(), getY(), getZ(), getBoundingBox().inflate(StoatRules.HUNT_RANGE, 4, StoatRules.HUNT_RANGE));
			} else {
				rabbit = null;
			}
			return rabbit != null && StoatRules.hunts(isBaby(), distanceTo(rabbit), level.getGameTime() - huntedAt);
		}

		@Override
		public boolean canContinueToUse() {
			return rabbit != null && rabbit.isAlive() && ticks < 400 && distanceTo(rabbit) < StoatRules.HUNT_RANGE * 1.5 && pose() != StoatRules.DIVE;
		}

		@Override
		public void start() {
			hunting = true;
			ticks = 0;
			danceEnd = -1;
			danced = false;
		}

		@Override
		public void stop() {
			hunting = false;
			huntedAt = level().getGameTime();
			if (pose() == StoatRules.DANCE) {
				pose(StoatRules.IDLE, 0);
			}
			rabbit = null;
			getNavigation().stop();
		}

		@Override
		public void tick() {
			if (rabbit == null || !(level() instanceof ServerLevel level)) {
				return;
			}
			ticks++;
			getLookControl().setLookAt(rabbit, 30, 30);
			double distance = distanceTo(rabbit);
			if (!danced) {
				if (danceEnd >= 0) {
					// Spellbound: the rabbit stands quite still and watches.
					rabbit.getNavigation().stop();
					rabbit.setDeltaMovement(rabbit.getDeltaMovement().multiply(0, 1, 0));
					rabbit.getLookControl().setLookAt(SnowcapStoat.this, 30, 30);
					setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
					if (ticks >= danceEnd || pose() != StoatRules.DANCE) {
						danced = true;
						pose(StoatRules.IDLE, 0);
					}
				} else if (distance <= StoatRules.DANCE_RANGE) {
					dance();
					danceEnd = ticks + StoatRules.DANCE_TICKS - 2;
				} else if (ticks % 10 == 1) {
					getNavigation().moveTo(rabbit, 0.8);
				}
				return;
			}
			// The dart and the bite.
			if (distance < 1.3) {
				rabbit.hurtServer(level, damageSources().mobAttack(SnowcapStoat.this), StoatRules.BITE);
				playSound(SoundEvents.FOX_BITE, 0.7F, 1.6F);
				rabbit = null;
			} else if (ticks % 4 == 0) {
				getNavigation().moveTo(rabbit, 1.7);
			}
		}
	}

	/** Trusting someone, it bounds along after them. */
	private final class FollowTruster extends Goal {
		private @Nullable Player player;

		FollowTruster() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			player = truster();
			return player != null && !isBaby() && distanceTo(player) > StoatRules.FOLLOW_FAR && distanceTo(player) < 24;
		}

		@Override
		public boolean canContinueToUse() {
			return player != null && truster() == player && distanceTo(player) > StoatRules.FOLLOW_NEAR && distanceTo(player) < 24;
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
