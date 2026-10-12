package dev.wildercord.wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A glowberry dormouse: a tiny round golden mouse of the lush caves with great round ears, big dark eyes and a long fluffy
 * tail. By day it sleeps curled in a ball on the moss with its tail wrapped round it, and nothing short of a blow or a berry
 * wakes it; by night it scurries about under the cave vines, and wherever a glow berry hangs low enough it stands up on its
 * hind legs and plucks it, and stuffs it in its cheeks, which swell and glow with what they hold (three at most). Left to
 * itself it eats them, one now and then.
 * <p>Run near it and it bolts; creep up and it lets you. Glow berries tempt and breed it. Fed, it trusts you a while,
 * scurries along after you, and now and then brings a berry out of its cheeks and leaves it at your feet.
 */
public class GlowberryDormouse extends Animal {
	private static final EntityDataAccessor<Integer> POUCH = SynchedEntityData.defineId(GlowberryDormouse.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> ASLEEP = SynchedEntityData.defineId(GlowberryDormouse.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> RISEN = SynchedEntityData.defineId(GlowberryDormouse.class, EntityDataSerializers.BOOLEAN);

	/** When something last woke it, who fed it last and when, when it last gave a berry, and when it next eats one of its own. */
	private long wokenAt = -1;
	private @Nullable UUID truster;
	private long fedAt = -1;
	private long gaveAt = -1;
	private long eatAt;

	/** Client: how far curled asleep, stood up, and how bright its cheeks, eased, this tick's and last. */
	public float sleep, sleepO, rise, riseO, glow, glowO;

	public GlowberryDormouse(EntityType<? extends GlowberryDormouse> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.28);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(POUCH, 0);
		builder.define(ASLEEP, false);
		builder.define(RISEN, false);
	}

	/** How many berries are in its cheeks. */
	public int pouch() {
		return entityData.get(POUCH);
	}

	public boolean asleep() {
		return entityData.get(ASLEEP);
	}

	public boolean risen() {
		return entityData.get(RISEN);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new Asleep());
		goalSelector.addGoal(2, new PanicGoal(this, 1.6));
		goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Player.class, 6.0F, 1.3, 1.6,
			e -> e instanceof Player p && DormouseRules.startles(p.isSprinting(), distanceTo(p), trusts(p), asleep())));
		goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Monster.class, (float) DormouseRules.ENEMY_RANGE, 1.3, 1.6, e -> !asleep()));
		goalSelector.addGoal(4, new BreedGoal(this, 1.0));
		goalSelector.addGoal(5, new TemptGoal(this, 1.1, this::isFood, false));
		goalSelector.addGoal(6, new FollowTruster());
		goalSelector.addGoal(7, new Forage());
		goalSelector.addGoal(8, new FollowParentGoal(this, 1.2));
		goalSelector.addGoal(9, new WaterAvoidingRandomStrollGoal(this, 0.9));
		goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 5.0F));
		goalSelector.addGoal(11, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.GLOW_BERRIES);
	}

	@Override
	public float getWalkTargetValue(BlockPos pos, LevelReader level) {
		// The dark of the caves doesn't put it off, only the ground.
		return level.getBlockState(pos.below()).is(DormouseContent.GROUND) ? 10 : 0;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			sleepO = sleep;
			riseO = rise;
			glowO = glow;
			sleep = WildlifeRules.approach(sleep, asleep() ? 1 : 0, 0.06F);
			rise = WildlifeRules.approach(rise, risen() ? 1 : 0, 0.2F);
			glow = WildlifeRules.approach(glow, DormouseRules.glow(pouch()), 0.05F);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		if (tickCount % 20 == 0) {
			boolean sleepy = DormouseRules.sleeps(!level.isDarkOutside(), wokenAt < 0 ? -1 : now - wokenAt)
				&& onGround() && !isInWater() && getTarget() == null && truster() == null;
			if (sleepy != asleep()) {
				entityData.set(ASLEEP, sleepy);
				entityData.set(RISEN, false);
				getNavigation().stop();
				playSound(SoundEvents.RABBIT_AMBIENT, 0.3F, sleepy ? 0.8F : 1.8F);
			}
		}
		Player friend = truster();
		if (friend != null && DormouseRules.gives(pouch(), distanceTo(friend), gaveAt < 0 ? -1 : now - gaveAt)) {
			give(level, friend, now);
		} else if (friend == null && pouch() > 0 && !asleep()) {
			if (eatAt == 0) {
				eatAt = now + DormouseRules.EAT_TICKS;
			} else if (now >= eatAt) {
				// A berry from its cheeks, nibbled.
				eatAt = now + DormouseRules.EAT_TICKS;
				entityData.set(POUCH, pouch() - 1);
				playSound(SoundEvents.GENERIC_EAT.value(), 0.3F, 1.8F);
				heal(1.0F);
			}
		}
	}

	/** A berry out of its cheeks, left at its friend's feet. */
	private void give(ServerLevel level, Player friend, long now) {
		gaveAt = now;
		entityData.set(POUCH, pouch() - 1);
		Vec3 at = position().lerp(friend.position(), 0.6);
		ItemEntity berry = new ItemEntity(level, at.x, at.y + 0.2, at.z, new ItemStack(Items.GLOW_BERRIES));
		berry.setDeltaMovement(0, 0.15, 0);
		berry.setDefaultPickUpDelay();
		level.addFreshEntity(berry);
		getLookControl().setLookAt(friend, 30, 30);
		playSound(SoundEvents.RABBIT_AMBIENT, 0.4F, 2.0F);
		level.broadcastEntityEvent(this, (byte) 18);
	}

	private boolean trusts(Player player) {
		return player.getUUID().equals(truster) && DormouseRules.trusts(level().getGameTime() - fedAt);
	}

	private @Nullable Player truster() {
		if (truster == null || !DormouseRules.trusts(level().getGameTime() - fedAt)) {
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
			playSound(SoundEvents.RABBIT_AMBIENT, 0.4F, 2.0F);
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
			playSound(SoundEvents.GENERIC_EAT.value(), 0.4F, 1.8F);
		}
		return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return getType().create(level, EntitySpawnReason.BREEDING) instanceof GlowberryDormouse young ? young : null;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Pouch", pouch());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(POUCH, Math.max(0, Math.min(DormouseRules.POUCH, input.getIntOr("Pouch", 0))));
	}

	// ------------------------------------------------------------------ sound: little squeaks and sniffs

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return asleep() ? null : SoundEvents.RABBIT_AMBIENT;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 200;
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
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 1.7F;
	}

	@Override
	protected float getSoundVolume() {
		return 0.4F;
	}

	/** Curled asleep: it doesn't move, look about or jump until it wakes. */
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

	/** To the foot of a cave vine with a berry hanging low enough, up on its hind legs, and the berry into its cheeks. */
	private final class Forage extends Goal {
		private @Nullable BlockPos vine;
		private @Nullable BlockPos stand;
		private int plucking;
		private int time;
		private long nextLook;

		Forage() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			if (!DormouseRules.forages(asleep(), pouch()) || isBaby() || level().getGameTime() < nextLook) {
				return false;
			}
			nextLook = level().getGameTime() + 40 + random.nextInt(60);
			return find();
		}

		@Override
		public boolean canContinueToUse() {
			return vine != null && stand != null && time < 240 && !asleep() && DormouseRules.forages(false, pouch()) && berry(level().getBlockState(vine));
		}

		@Override
		public void start() {
			plucking = 0;
			time = 0;
			getNavigation().moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 1.0);
		}

		@Override
		public void stop() {
			entityData.set(RISEN, false);
			vine = stand = null;
		}

		@Override
		public void tick() {
			time++;
			if (vine == null || stand == null) {
				return;
			}
			double dx = getX() - (stand.getX() + 0.5), dz = getZ() - (stand.getZ() + 0.5);
			boolean there = dx * dx + dz * dz < 0.6 && Math.abs(getY() - stand.getY()) < 0.6;
			if (!there) {
				entityData.set(RISEN, false);
				if (time % 20 == 0) {
					getNavigation().moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 1.0);
				}
				return;
			}
			getNavigation().stop();
			getLookControl().setLookAt(vine.getX() + 0.5, vine.getY() + 0.2, vine.getZ() + 0.5, 30, 60);
			entityData.set(RISEN, true);
			if (++plucking >= DormouseRules.PLUCK_TICKS && level() instanceof ServerLevel level) {
				BlockState state = level.getBlockState(vine);
				level.setBlock(vine, state.setValue(BlockStateProperties.BERRIES, false), 2);
				level.gameEvent(GameEvent.BLOCK_CHANGE, vine, GameEvent.Context.of(GlowberryDormouse.this, state));
				level.playSound(null, vine, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.NEUTRAL, 0.8F, 1.3F);
				entityData.set(POUCH, pouch() + 1);
				eatAt = Math.max(eatAt, level.getGameTime() + DormouseRules.EAT_TICKS);
				vine = null;
			}
		}

		private boolean berry(BlockState state) {
			return (state.is(Blocks.CAVE_VINES) || state.is(Blocks.CAVE_VINES_PLANT)) && state.getValue(BlockStateProperties.BERRIES);
		}

		/** The nearest berry it can stand under and reach. */
		private boolean find() {
			Level level = level();
			BlockPos home = blockPosition();
			double best = Double.MAX_VALUE;
			vine = stand = null;
			for (BlockPos pos : BlockPos.betweenClosed(home.offset(-DormouseRules.SEARCH, 0, -DormouseRules.SEARCH),
					home.offset(DormouseRules.SEARCH, DormouseRules.SEARCH_UP, DormouseRules.SEARCH))) {
				if (!berry(level.getBlockState(pos))) {
					continue;
				}
				// Down from under the berry to the ground: somewhere to stand, near enough below it.
				BlockPos under = pos.below();
				for (int drop = 0; drop <= DormouseRules.REACH + 1 && level.getBlockState(under).isAir(); drop++) {
					BlockPos floor = under.below();
					if (level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
						if (DormouseRules.reaches(pos.getY() - under.getY() - 1) || DormouseRules.reaches(pos.getY() - under.getY())) {
							double d = under.distSqr(home);
							if (d < best) {
								best = d;
								vine = pos.immutable();
								stand = under.immutable();
							}
						}
						break;
					}
					under = floor;
				}
			}
			return vine != null;
		}
	}

	/** Trusting someone, it scurries along after them. */
	private final class FollowTruster extends Goal {
		private @Nullable Player player;

		FollowTruster() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			player = truster();
			return player != null && !isBaby() && distanceTo(player) > DormouseRules.FOLLOW_FAR && distanceTo(player) < 20;
		}

		@Override
		public boolean canContinueToUse() {
			return player != null && truster() == player && distanceTo(player) > DormouseRules.FOLLOW_NEAR && distanceTo(player) < 20;
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
