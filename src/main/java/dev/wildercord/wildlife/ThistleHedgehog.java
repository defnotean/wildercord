package dev.wildercord.wildlife;

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
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A thistle hedgehog: a small round forager of the meadows, plains and forests, brown-quilled with a cream face, a twitching
 * black nose and, more often than not, something caught in its quills (a thistle bloom, a leaf, a berry). It snuffles about
 * the grass and sneezes now and then. Run at it, or let something hostile come close, or hit it, and it curls into a ball of
 * quills; struck curled up it takes little and pricks back. After a while it peeks out, then uncurls.
 * <p>Sweet berries tempt it and breed it (its young are tiny hoglets). Feed one and it trusts you a while: hearts, it potters
 * after you, won't curl up at your running, and often brings you something it foraged.
 */
public class ThistleHedgehog extends Animal {
	/** Curled with this many ticks to go, 0 when it isn't. */
	private static final EntityDataAccessor<Integer> CURL = SynchedEntityData.defineId(ThistleHedgehog.class, EntityDataSerializers.INT);
	/** The entity event its sneeze is sent as. */
	private static final byte SNEEZE = 70;
	private static final Item[] GIFTS = {Items.SWEET_BERRIES, Items.BROWN_MUSHROOM, Items.RED_MUSHROOM, Items.DANDELION, Items.APPLE, Items.BEETROOT_SEEDS};

	/** Who fed it last, when, and when its gift comes (-1 for none). */
	private @Nullable UUID truster;
	private long fedAt = -1, giftAt = -1;

	/** Client: how far it is curled (1 tight, less while peeking out) and how far into a sneeze, eased, this tick's and last. */
	public float curl, curlO, sneeze, sneezeO;

	public ThistleHedgehog(EntityType<? extends ThistleHedgehog> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.22);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CURL, 0);
	}

	public int curlLeft() {
		return entityData.get(CURL);
	}

	public boolean curled() {
		return curlLeft() > 0;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new Curled());
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(3, new BreedGoal(this, 1.0));
		goalSelector.addGoal(4, new TemptGoal(this, 1.0, this::isFood, false));
		goalSelector.addGoal(5, new FollowTruster());
		goalSelector.addGoal(6, new FollowParentGoal(this, 1.1));
		goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(9, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.SWEET_BERRIES);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			curlO = curl;
			int left = curlLeft();
			curl = WildlifeRules.approach(curl, left <= 0 ? 0 : HedgehogRules.peeking(left) ? 0.55F : 1, left > 0 && !HedgehogRules.peeking(left) ? 0.3F : 0.1F);
			sneezeO = sneeze;
			sneeze = Math.max(0, sneeze - 0.12F);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		int left = curlLeft();
		if (left > 0) {
			entityData.set(CURL, left - 1);
			if (left == 1) {
				playSound(SoundEvents.ARMADILLO_UNROLL_FINISH, 0.6F, 1.5F);
			}
		} else if (tickCount % 5 == 0 && startled(level)) {
			curlUp();
		} else if (random.nextInt(HedgehogRules.SNEEZE_ODDS) == 0) {
			level.broadcastEntityEvent(this, SNEEZE);
			playSound(SoundEvents.PANDA_SNEEZE, 0.5F, 1.9F + random.nextFloat() * 0.2F);
		}
		gift(level, now);
	}

	/** Someone running close by (who it doesn't trust), or something hostile right beside it. */
	private boolean startled(ServerLevel level) {
		for (Player player : level.players()) {
			if (!player.isSpectator() && HedgehogRules.startles(player.isSprinting(), distanceTo(player), trusts(player))) {
				return true;
			}
		}
		return !level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(2.5), e -> e instanceof Enemy && e.isAlive()).isEmpty();
	}

	private void curlUp() {
		if (!curled()) {
			playSound(SoundEvents.ARMADILLO_ROLL, 0.7F, 1.5F);
		}
		entityData.set(CURL, HedgehogRules.CURL_TICKS);
		getNavigation().stop();
	}

	private boolean trusts(Player player) {
		return player.getUUID().equals(truster) && HedgehogRules.trusts(level().getGameTime() - fedAt);
	}

	private @Nullable Player truster() {
		if (truster == null || !HedgehogRules.trusts(level().getGameTime() - fedAt)) {
			return null;
		}
		Player player = level().getPlayerByUUID(truster);
		return player != null && player.isAlive() && !player.isSpectator() ? player : null;
	}

	/** Its gift, when it's due: handed over once it's beside whoever fed it, and given up if the trust runs out first. */
	private void gift(ServerLevel level, long now) {
		if (giftAt < 0 || now < giftAt || curled()) {
			return;
		}
		Player player = truster();
		if (player == null) {
			giftAt = -1;
			return;
		}
		if (distanceTo(player) > HedgehogRules.GIFT_REACH) {
			return;
		}
		giftAt = -1;
		if (random.nextDouble() >= HedgehogRules.GIFT_CHANCE) {
			return;
		}
		spawnAtLocation(level, new ItemStack(GIFTS[HedgehogRules.gift(random.nextDouble())]));
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.5, getZ(), 6, 0.3, 0.2, 0.3, 0);
		playSound(SoundEvents.FOX_SNIFF, 0.8F, 1.7F);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean curled = curled();
		// Struck curled up, by something close enough to touch it: the quills prick back.
		if (curled && source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker && attacker != this) {
			attacker.hurtServer(level, damageSources().thorns(this), HedgehogRules.PRICK);
			playSound(SoundEvents.THORNS_HIT, 0.6F, 1.4F);
		}
		boolean hit = super.hurtServer(level, source, HedgehogRules.blunted(amount, curled));
		if (hit && isAlive()) {
			curlUp();
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
			// Fed, it trusts you: it uncurls, and hearts.
			truster = player.getUUID();
			fedAt = level().getGameTime();
			giftAt = fedAt + HedgehogRules.giftDelay(random.nextDouble());
			entityData.set(CURL, 0);
			level().broadcastEntityEvent(this, (byte) 18);
			playSound(SoundEvents.FOX_EAT, 0.7F, 1.6F);
		}
		return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == SNEEZE) {
			sneeze = 1;
			sneezeO = 1;
			double yaw = Math.toRadians(getYRot());
			level().addParticle(ParticleTypes.SNEEZE, getX() - Math.sin(yaw) * 0.5, getY() + 0.2, getZ() + Math.cos(yaw) * 0.5, 0, 0, 0);
			return;
		}
		super.handleEntityEvent(id);
	}

	// ------------------------------------------------------------------ breeding: a hoglet

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return getType().create(level, EntitySpawnReason.BREEDING) instanceof ThistleHedgehog hoglet ? hoglet : null;
	}

	// ------------------------------------------------------------------ sound: snuffles, squeaks

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return curled() ? null : SoundEvents.FOX_SNIFF;
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
	public float getVoicePitch() {
		return super.getVoicePitch() * 1.5F;
	}

	@Override
	protected float getSoundVolume() {
		return 0.5F;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 200;
	}

	/** Curled up: it doesn't move, look about or jump until it uncurls. */
	private final class Curled extends Goal {
		Curled() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return curled();
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

	/** Trusting someone, it potters along after them. */
	private final class FollowTruster extends Goal {
		private @Nullable Player player;

		FollowTruster() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			player = truster();
			return player != null && !isBaby() && distanceTo(player) > HedgehogRules.FOLLOW_FAR && distanceTo(player) < 24;
		}

		@Override
		public boolean canContinueToUse() {
			return player != null && truster() == player && distanceTo(player) > HedgehogRules.FOLLOW_NEAR && distanceTo(player) < 24;
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
				getNavigation().moveTo(player, 1.15);
			}
		}
	}
}
