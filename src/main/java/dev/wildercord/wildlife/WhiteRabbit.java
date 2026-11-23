package dev.wildercord.wildlife;

import dev.wildercord.cast.Vfx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Forest-dwelling White Rabbit carrying a brass pocket watch. When an adventurer approaches,
 * it glances at its watch and leads them along a sparkling trail to a newly revealed Hollow
 * Ancient Tree before diving down into the dark portal hole inside its roots.
 */
public class WhiteRabbit extends Animal {
	public static final int STATE_WANDERING = 0;
	public static final int STATE_LEADING = 1;
	public static final int STATE_WAITING_LOOKBACK = 2;
	public static final int STATE_AT_HOLE = 3;

	private static final EntityDataAccessor<Integer> DATA_STATE =
		SynchedEntityData.defineId(WhiteRabbit.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_CHECKING_WATCH =
		SynchedEntityData.defineId(WhiteRabbit.class, EntityDataSerializers.BOOLEAN);

	private @Nullable BlockPos targetTreePos;
	private @Nullable BlockPos portalPos;
	private @Nullable UUID leadPlayerId;
	private int divePauseTicks;
	private boolean dived;
	private boolean announcedLead;
	private long lastWaitCueTick;
	private long lastTrailTick;

	/** Client-side eased airborne factor for bounding animation. */
	public float air;
	public float airO;

	public WhiteRabbit(EntityType<? extends WhiteRabbit> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes()
			.add(Attributes.MAX_HEALTH, 12.0)
			.add(Attributes.MOVEMENT_SPEED, 0.34)
			.add(Attributes.FOLLOW_RANGE, 24.0)
			.add(Attributes.STEP_HEIGHT, 1.1);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_STATE, STATE_WANDERING);
		builder.define(DATA_CHECKING_WATCH, false);
	}

	public int rabbitState() {
		return entityData.get(DATA_STATE);
	}

	public boolean isCheckingWatch() {
		return entityData.get(DATA_CHECKING_WATCH);
	}

	public @Nullable BlockPos portalPos() {
		return portalPos;
	}

	public @Nullable BlockPos targetTreePos() {
		return targetTreePos;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 1.6));
		this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.85));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			airO = air;
			boolean aloft = !onGround() && !isNoAi();
			air = air + ((aloft ? 1.0F : 0.0F) - air) * 0.35F;
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (dived || !isAlive()) {
			return;
		}
		long now = level.getGameTime();

		if (leadPlayerId == null || portalPos == null) {
			ServerPlayer watcher = nearestActivePlayer(level, WhiteRabbitRules.NOTICE_RANGE);
			if (watcher != null && getLastHurtByMob() == null) {
				BlockPos site = HollowTreeBuilder.findTreeSite(level, blockPosition(), level.getRandom());
				if (site != null) {
					leadToHollowTree(watcher, site, null);
				}
			}
			return;
		}

		ServerPlayer follower = resolveFollower(level);
		if (follower == null) {
			// Idle by the hole looking at its watch until someone returns.
			getNavigation().stop();
			entityData.set(DATA_STATE, STATE_WAITING_LOOKBACK);
			entityData.set(DATA_CHECKING_WATCH, true);
			return;
		}

		Vec3 targetCenter = Vec3.atBottomCenterOf(portalPos);
		double treeDist = position().distanceTo(targetCenter);
		double playerDist = distanceTo(follower);

		if (now - lastTrailTick >= 6L) {
			lastTrailTick = now;
			Vfx.emit(level, ParticleTypes.END_ROD, position().add(0, 0.35, 0), 1, 0.15, 0.01);
		}

		if (WhiteRabbitRules.shouldDiveIn(playerDist, treeDist)) {
			entityData.set(DATA_STATE, STATE_AT_HOLE);
			entityData.set(DATA_CHECKING_WATCH, true);
			getNavigation().stop();
			getLookControl().setLookAt(follower, 40.0F, 40.0F);
			divePauseTicks++;
			if (divePauseTicks >= 18) {
				performDiveIntoHole(level, targetCenter);
			}
			return;
		}

		divePauseTicks = 0;
		if (WhiteRabbitRules.shouldWaitForPlayer(playerDist, treeDist)) {
			entityData.set(DATA_STATE, STATE_WAITING_LOOKBACK);
			entityData.set(DATA_CHECKING_WATCH, true);
			getNavigation().stop();
			getLookControl().setLookAt(follower, 40.0F, 40.0F);
			if (now - lastWaitCueTick >= 50L) {
				lastWaitCueTick = now;
				playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 1.35F);
				Vfx.emit(level, ParticleTypes.ENCHANT, position().add(0, 0.5, 0), 6, 0.3, 0.04);
			}
			return;
		}

		entityData.set(DATA_STATE, STATE_LEADING);
		entityData.set(DATA_CHECKING_WATCH, false);
		getLookControl().setLookAt(targetCenter.x, targetCenter.y + 0.4, targetCenter.z, 30.0F, 30.0F);
		if (!getNavigation().isInProgress() || tickCount % 8 == 0) {
			getNavigation().moveTo(targetCenter.x, targetCenter.y, targetCenter.z, 1.35);
		}
		if (onGround() && !isInWater() && tickCount % 9 == 0) {
			jumpFromGround();
		}
	}

	/**
	 * Binds this rabbit to lead {@code player} to a freshly built Hollow Ancient Tree at
	 * {@code treeBase}, optionally pinning the portal's {@code forcedOutcome} (null for random roll).
	 */
	public BlockPos leadToHollowTree(ServerPlayer player, BlockPos treeBase,
			WhiteRabbitRules.@Nullable Outcome forcedOutcome) {
		if (!(level() instanceof ServerLevel level)) {
			return treeBase;
		}
		Direction towardRabbit = facingToward(treeBase, blockPosition());
		BlockPos portal = HollowTreeBuilder.placeHollowTree(level, treeBase, towardRabbit, forcedOutcome);
		this.targetTreePos = treeBase.immutable();
		this.portalPos = portal.immutable();
		this.leadPlayerId = player.getUUID();
		this.divePauseTicks = 0;
		this.entityData.set(DATA_STATE, STATE_LEADING);
		this.entityData.set(DATA_CHECKING_WATCH, false);
		setPersistenceRequired();

		if (!announcedLead) {
			announcedLead = true;
			playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.85F, 1.25F);
			playSound(SoundEvents.RABBIT_AMBIENT, 0.7F, 1.15F);
			player.displayClientMessage(
				Component.translatable("message.wildercord.white_rabbit.leading")
					.withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC), true);
		}
		return portal;
	}

	private void performDiveIntoHole(ServerLevel level, Vec3 holeCenter) {
		dived = true;
		Vec3 leap = holeCenter.subtract(position()).normalize().scale(0.45);
		setDeltaMovement(leap.x, 0.35, leap.z);
		Vfx.emit(level, ParticleTypes.PORTAL, holeCenter.add(0, 0.5, 0), 28, 0.35, 0.12);
		Vfx.emit(level, ParticleTypes.ENCHANT, holeCenter.add(0, 0.6, 0), 16, 0.4, 0.08);
		playSound(SoundEvents.ENDERMAN_TELEPORT, 0.85F, 1.4F);
		discard();
	}

	private static Direction facingToward(BlockPos from, BlockPos to) {
		int dx = to.getX() - from.getX();
		int dz = to.getZ() - from.getZ();
		if (Math.abs(dx) >= Math.abs(dz)) {
			return dx >= 0 ? Direction.EAST : Direction.WEST;
		}
		return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
	}

	private @Nullable ServerPlayer nearestActivePlayer(ServerLevel level, double radius) {
		ServerPlayer best = null;
		double bestSqr = radius * radius;
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator() || !player.isAlive()) {
				continue;
			}
			double d2 = player.distanceToSqr(this);
			if (d2 <= bestSqr) {
				bestSqr = d2;
				best = player;
			}
		}
		return best;
	}

	private @Nullable ServerPlayer resolveFollower(ServerLevel level) {
		if (leadPlayerId != null && level.getPlayerByUUID(leadPlayerId) instanceof ServerPlayer sp
			&& sp.isAlive() && !sp.isSpectator() && sp.distanceToSqr(this) <= 40.0 * 40.0) {
			return sp;
		}
		ServerPlayer near = nearestActivePlayer(level, 28.0);
		if (near != null) {
			leadPlayerId = near.getUUID();
		}
		return near;
	}

	@Override
	public void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (targetTreePos != null) output.store("TargetTree", BlockPos.CODEC, targetTreePos);
		if (portalPos != null) output.store("PortalPos", BlockPos.CODEC, portalPos);
		if (leadPlayerId != null) output.store("LeadPlayer", UUIDUtil.CODEC, leadPlayerId);
		output.putBoolean("Dived", dived);
		output.putBoolean("AnnouncedLead", announcedLead);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		targetTreePos = input.read("TargetTree", BlockPos.CODEC).orElse(null);
		portalPos = input.read("PortalPos", BlockPos.CODEC).orElse(null);
		leadPlayerId = input.read("LeadPlayer", UUIDUtil.CODEC).orElse(null);
		dived = input.getBooleanOr("Dived", false);
		announcedLead = input.getBooleanOr("AnnouncedLead", false);
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return false;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
		return null;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return SoundEvents.RABBIT_AMBIENT;
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
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return leadPlayerId == null && super.removeWhenFarAway(distanceToClosestPlayer);
	}
}
