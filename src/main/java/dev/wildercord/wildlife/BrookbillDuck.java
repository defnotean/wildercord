package dev.wildercord.wildlife;

import dev.wildercord.cast.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * A brookbill duck: a plump speckled brown duck of the rivers and ponds, with a blue patch on each wing, an orange bill and
 * orange feet. It floats high on the water and paddles about, and now and then up-ends, tail in the air, to dabble for
 * something on the bottom. Climbing out it shakes itself off; out on land in the rain it flaps its wings for the joy of it.
 * <p>Its ducklings, yellow balls of fluff with a brown stripe down the back, paddle after it in a single-file line, each
 * one keeping close behind the next. A duckling hatched by breeding imprints on the first face it sees, so whoever bred it
 * becomes its mother: it falls in behind them instead, the rest of its brood behind it, until it grows up. Feeding a
 * duckling seeds imprints it too. Seeds tempt and breed the grown ducks.
 */
public class BrookbillDuck extends Animal {
	private static final EntityDataAccessor<Boolean> DABBLING = SynchedEntityData.defineId(BrookbillDuck.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> FLAPPING = SynchedEntityData.defineId(BrookbillDuck.class, EntityDataSerializers.BOOLEAN);

	/** The player a duckling imprinted on, if any. */
	private @Nullable UUID imprint;
	/** Server: whoever it is following at the head of its line, refreshed now and then; whether it was afloat last tick. */
	private @Nullable Entity lead;
	private boolean wasAfloat;
	private int flapLeft;
	private long dabbledAt = -1;

	/** Client: afloat, up-ended and flapping, eased, this tick's and last. */
	public float swim, swimO, dabble, dabbleO, flap, flapO;

	public BrookbillDuck(EntityType<? extends BrookbillDuck> type, Level level) {
		super(type, level);
		setPathfindingMalus(PathType.WATER, 0);
		getNavigation().setCanFloat(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.TEMPT_RANGE, 10);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DABBLING, false);
		builder.define(FLAPPING, false);
	}

	public boolean dabbling() {
		return entityData.get(DABBLING);
	}

	public boolean flapping() {
		return entityData.get(FLAPPING);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new PanicGoal(this, 1.4));
		goalSelector.addGoal(2, new BreedGoal(this, 1.0));
		goalSelector.addGoal(3, new TemptGoal(this, 1.1, this::isFood, false));
		goalSelector.addGoal(4, new FollowLine());
		goalSelector.addGoal(5, new Dabble());
		goalSelector.addGoal(6, new RandomStrollGoal(this, 1.0));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(ItemTags.CHICKEN_FOOD);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			swimO = swim;
			dabbleO = dabble;
			flapO = flap;
			swim = WildlifeRules.approach(swim, isInWater() ? 1 : 0, 0.2F);
			dabble = WildlifeRules.approach(dabble, dabbling() ? 1 : 0, 0.15F);
			flap = WildlifeRules.approach(flap, flapping() ? 1 : 0, 0.3F);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		boolean afloat = isInWater();
		if (DuckRules.shakes(wasAfloat, afloat, onGround())) {
			flap();
			Fx.send(level, ParticleTypes.SPLASH, getX(), getY() + 0.4, getZ(), 10, 0.25, 0.15, 0.25, 0.1);
		} else if (flapLeft == 0 && !isBaby() && DuckRules.rejoices(level.isRainingAt(blockPosition().above()), onGround(), getRandom().nextDouble())) {
			flap();
			playSound(getAmbientSound(), 0.8F, getVoicePitch() * 1.1F);
		}
		wasAfloat = afloat;
		if (flapLeft > 0 && --flapLeft == 0) {
			entityData.set(FLAPPING, false);
		}
	}

	private void flap() {
		flapLeft = DuckRules.FLAP_TICKS;
		entityData.set(FLAPPING, true);
	}

	/** The player it imprinted on, if they are about. */
	private @Nullable Player imprinted() {
		if (imprint == null || !isBaby()) {
			return null;
		}
		Player player = level().getPlayerByUUID(imprint);
		return player != null && player.isAlive() && !player.isSpectator() && distanceTo(player) < 32 ? player : null;
	}

	/** Whoever heads its line: the player it imprinted on, else the nearest grown duck. */
	private @Nullable Entity findLead() {
		Player player = imprinted();
		if (player != null) {
			return player;
		}
		BrookbillDuck best = null;
		for (BrookbillDuck duck : level().getEntitiesOfClass(BrookbillDuck.class, getBoundingBox().inflate(DuckRules.MOTHER_RANGE),
			d -> !d.isBaby() && d.isAlive())) {
			if (best == null || distanceToSqr(duck) < distanceToSqr(best)) {
				best = duck;
			}
		}
		return best;
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (isFood(held) && DuckRules.imprints(isBaby(), true, imprint != null && player.getUUID().equals(imprint))) {
			if (!level().isClientSide()) {
				held.consume(1, player);
				imprint = player.getUUID();
				lead = player;
				level().broadcastEntityEvent(this, (byte) 18);
				playSound(SoundEvents.PARROT_AMBIENT, 0.6F, 1.8F);
			}
			return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
		}
		return super.mobInteract(player, hand);
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		if (!(getType().create(level, EntitySpawnReason.BREEDING) instanceof BrookbillDuck young)) {
			return null;
		}
		// The first face it sees: whoever bred it, if they stayed to watch, else anyone standing close.
		ServerPlayer breeder = getLoveCause();
		Player seen = breeder != null && distanceTo(breeder) <= DuckRules.IMPRINT_RANGE ? breeder
			: level.getNearestPlayer(this, DuckRules.IMPRINT_RANGE);
		if (seen != null && !seen.isSpectator()) {
			young.imprint = seen.getUUID();
		}
		return young;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hit = super.hurtServer(level, source, amount);
		if (hit) {
			entityData.set(DABBLING, false);
		}
		return hit;
	}

	@Override
	public void ageUp(int seconds, boolean forced) {
		super.ageUp(seconds, forced);
		if (!isBaby()) {
			imprint = null;
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (imprint != null) {
			output.putString("imprint", imprint.toString());
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		String id = input.getStringOr("imprint", "");
		try {
			imprint = id.isEmpty() ? null : UUID.fromString(id);
		} catch (IllegalArgumentException e) {
			imprint = null;
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.PARROT_AMBIENT;
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
		playSound(SoundEvents.CHICKEN_STEP.value(), 0.12F, 1.2F);
	}

	/** A deep quack from the grown ducks, a thin peep from the ducklings. */
	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * (isBaby() ? 1.5F : 0.6F);
	}

	@Override
	public int getAmbientSoundInterval() {
		return isBaby() ? 140 : 220;
	}

	/** A duckling paddles single file behind the one ahead of it in its brood, the first of them behind its mother. */
	private final class FollowLine extends Goal {
		private @Nullable Entity ahead;

		FollowLine() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (!isBaby() || (tickCount + getId()) % 10 != 0) {
				return false;
			}
			lead = findLead();
			ahead = ahead();
			return ahead != null && DuckRules.pace(distanceTo(ahead)) > 0;
		}

		@Override
		public boolean canContinueToUse() {
			return isBaby() && ahead != null && ahead.isAlive() && distanceTo(ahead) < DuckRules.MOTHER_RANGE * 1.5;
		}

		@Override
		public void stop() {
			ahead = null;
			getNavigation().stop();
		}

		@Override
		public void tick() {
			if (tickCount % 20 == 0) {
				lead = findLead();
				ahead = ahead();
				if (ahead == null) {
					return;
				}
			}
			getLookControl().setLookAt(ahead, 30, 30);
			double pace = DuckRules.pace(distanceTo(ahead));
			if (pace == 0) {
				getNavigation().stop();
			} else if (tickCount % 5 == 0) {
				getNavigation().moveTo(ahead, pace);
			}
		}

		/** Whoever is just ahead of it in the line. */
		private @Nullable Entity ahead() {
			if (lead == null) {
				return null;
			}
			Entity head = lead;
			List<BrookbillDuck> brood = level().getEntitiesOfClass(BrookbillDuck.class, head.getBoundingBox().inflate(DuckRules.MOTHER_RANGE),
				d -> d.isBaby() && d.isAlive() && d.lead == head);
			int[] ids = brood.stream().mapToInt(Entity::getId).sorted().toArray();
			int place = DuckRules.place(getId(), ids);
			if (place == 0 || place >= ids.length) {
				return head;
			}
			Entity before = level().getEntity(ids[place - 1]);
			return before != null ? before : head;
		}
	}

	/** Afloat and idle, it up-ends, tail in the air, and dabbles at the bottom a while. */
	private final class Dabble extends Goal {
		private int left;

		Dabble() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if ((tickCount + getId()) % 20 != 0 || getRandom().nextInt(3) != 0) {
				return false;
			}
			long now = level().getGameTime();
			return DuckRules.dabbles(!isBaby(), isInWater(), getNavigation().isDone(), dabbledAt < 0 ? -1 : now - dabbledAt);
		}

		@Override
		public boolean canContinueToUse() {
			return left > 0 && isInWater();
		}

		@Override
		public void start() {
			left = DuckRules.DABBLE_TICKS;
			getNavigation().stop();
			entityData.set(DABBLING, true);
			playSound(SoundEvents.GENERIC_SPLASH, 0.3F, 1.6F);
		}

		@Override
		public void stop() {
			entityData.set(DABBLING, false);
			dabbledAt = level().getGameTime();
			if (left <= 0) {
				heal(DuckRules.DABBLE_HEAL);
			}
		}

		@Override
		public void tick() {
			left--;
			if (left % 8 == 0 && level() instanceof ServerLevel level) {
				Fx.send(level, ParticleTypes.BUBBLE, getX(), getY() + 0.1, getZ(), 3, 0.2, 0.1, 0.2, 0.02);
			}
		}
	}
}
