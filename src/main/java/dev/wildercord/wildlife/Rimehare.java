package dev.wildercord.wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
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
import org.jspecify.annotations.Nullable;

/**
 * A rimehare: a quick white hare of the snowy lands, long-eared and long-legged, frost glinting on its coat. It goes in
 * bounds, and every landing leaves a pair of frost prints that fade in a few seconds (drawn by each client). It bolts
 * from anyone who comes near, faster than you can run, unless they hold out sweet berries and don't run at it; two fed
 * berries raise a leveret.
 */
public class Rimehare extends Animal {
	private final RimehareBoundCooldown boundCooldown = new RimehareBoundCooldown();
	/** A grounded move-control tick must orient this bound before travel starts. */
	private boolean groundedMove;
	/** Client: in the air, and sitting up on alert, eased (this tick's and last tick's). */
	public float air, airO, alert, alertO;
	/** Client: the tick it last landed from a bound, and the last landing its frost prints were drawn for. */
	public int landedTick = -1, printedTick = -1;
	private boolean wasOnGround = true;

	public Rimehare(EntityType<? extends Rimehare> type, Level level) {
		super(type, level);
		moveControl = new BoundMoveControl(this);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(1, new PanicGoal(this, 2.6));
		goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<>(this,dev.wildercord.aura.world.Galeclaw.class,12,2.0,2.6,
			who -> boltsFrom(who)));
		goalSelector.addGoal(2, new BreedGoal(this, 0.9));
		goalSelector.addGoal(3, new TemptGoal(this, 1.0, this::isFood, true));
		goalSelector.addGoal(4, new FleeGoal(this, this::boltsFrom, WildlifeRules.HARE_NOTICE, 2.0, 2.6));
		goalSelector.addGoal(5, new FollowParentGoal(this, 1.2));
		goalSelector.addGoal(5, new WindreedForageGoal(this,() -> !isBaby(),feeding -> {}));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
	}

	/** Whether it bolts from this creature: any player near who isn't calmly holding out berries. */
	public boolean boltsFrom(LivingEntity who) {
		if(who instanceof dev.wildercord.aura.world.Galeclaw predator) return predator.isAlive() && predator.hungry();
		if (!(who instanceof Player player) || player.isSpectator()) {
			return false;
		}
		if(HighlandContent.quiet(player) && distanceTo(player)>=2)return false;
		boolean berries = isFood(player.getMainHandItem()) || isFood(player.getOffhandItem());
		return WildlifeRules.hareBolts(distanceTo(player), berries, player.isSprinting());
	}

	@Override
	public void aiStep() {
		boolean groundedBeforeTravel = onGround();
		groundedMove = false;
		super.aiStep();
		if (level().isClientSide()) {
			airO = air;
			alertO = alert;
			// (One standing still for a picture, with no mind of its own, is never in the air.)
			boolean aloft = !onGround() && !isNoAi();
			air = WildlifeRules.approach(air, aloft ? 1 : 0, aloft ? 0.3F : 0.5F);
			if (onGround() && !wasOnGround) {
				landedTick = tickCount;
			}
			wasOnGround = onGround();
			// It sits up when someone is near and it's standing still, ready to run.
			Player near = level().getNearestPlayer(this, 12);
			boolean still = getDeltaMovement().horizontalDistanceSqr() < 0.001;
			alert = WildlifeRules.approach(alert, near != null && !near.isSpectator() && still ? 1 : 0, 0.12F);
		}
		if (!level().isClientSide() && !isNoAi() && !isInWater()) {
			// Air time no longer spends the landing recovery. Ground navigation needs real ticks to
			// advance its waypoint and turn before the next takeoff, including after a long fall.
			boolean recovered = boundCooldown.tick(groundedBeforeTravel, onGround());
			if (recovered && groundedMove && getNavigation().isInProgress() && getSpeed() > 0
				&& getDeltaMovement().y <= 0) {
				jumpFromGround();
				boundCooldown.launched(3 + random.nextInt(4));
			}
		}
	}

	/** Keep vanilla path following and obstacle jumps, but never accelerate through an unfinished ground turn. */
	private static final class BoundMoveControl extends MoveControl<Rimehare> {
		private BoundMoveControl(Rimehare hare) {
			super(hare);
		}

		@Override
		public void tick() {
			if (mob.onGround() && !mob.isInWater() && !mob.isNoAi() && operation == Operation.MOVE_TO) {
				double dx = wantedX - mob.getX(), dz = wantedZ - mob.getZ();
				float heading = (float) (Mth.atan2(dz, dx) * (180 / Math.PI)) - 90;
				if (dx * dx + dz * dz > MIN_SPEED_SQR && Math.abs(Mth.wrapDegrees(heading - mob.getYRot())) > MAX_TURN) {
					mob.setYRot(rotlerp(mob.getYRot(), heading, MAX_TURN));
					mob.setSpeed(0);
					mob.setZza(0);
					mob.setXxa(0);
					return;
				}
				double requestedSpeed = speedModifier * mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
				if (mob.boundCooldown.shouldBrake(mob.getDeltaMovement().horizontalDistance(), requestedSpeed)) {
					operation = Operation.WAIT;
					// Shed excess momentum during recovery before a full-speed input could skip a
					// waypoint cell. Slower steps keep their requested movement input.
					mob.setSpeed(0);
					mob.setZza(0);
					mob.setXxa(0);
					return;
				}
				mob.groundedMove = true;
			}
			// This also retains the native JUMPING operation for steps, and FloatGoal in water.
			super.tick();
		}
	}

	/** Low, quick bounds rather than leaps. */
	@Override
	protected float getJumpPower() {
		return getJumpPower(0.85F);
	}

	// ------------------------------------------------------------------ breeding

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.SWEET_BERRIES);
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return Wildlife.RIMEHARE.create(level, EntitySpawnReason.BREEDING);
	}

	// ------------------------------------------------------------------ sound

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return Wildlife.sound("rimehare_squeak");
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return Wildlife.sound("rimehare_hurt");
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return Wildlife.sound("rimehare_hurt");
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		SoundEvent step = Wildlife.sound("rimehare_hop");
		if (step != null) {
			playSound(step, 0.25F, 1.0F);
		} else {
			super.playStepSound(pos, state);
		}
	}

	@Override
	public int getAmbientSoundInterval() {
		return 360;
	}

	@Override
	protected float getSoundVolume() {
		return 0.6F;
	}
}
