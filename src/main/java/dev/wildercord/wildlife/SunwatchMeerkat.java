package dev.wildercord.wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
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
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A sunwatch meerkat: a slim little sandy-banded digger of the deserts and savannas, dark round the eyes, that lives in a mob. One
 * at a time goes up to the highest spot near and stands sentry, tall on its hind legs and leaning on its tail, its head turning
 * this way and that, while the rest dig with their noses down, sit up with their bellies to the morning sun, and at night all
 * go to sleep in a heap together. A stranger walking up in plain sight (or a monster) and the sentry, or whoever sees it first,
 * gives the alarm, and the whole mob dives down their holes; a while later their heads come up to peek, and if the stranger is
 * still there, down they go again. Creep up crouched and they don't see you until you are right there.
 * <p>Spider eyes or eggs tempt it and breed it. Feed one and it trusts you a while: it sits up to eat, trots along after you, and
 * never gives the alarm at you.
 */
public class SunwatchMeerkat extends Animal {
	/** What it is doing ({@link MeerkatRules#IDLE} and so on), and how long that has to go. */
	private static final EntityDataAccessor<Integer> POSE = SynchedEntityData.defineId(SunwatchMeerkat.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> LEFT = SynchedEntityData.defineId(SunwatchMeerkat.class, EntityDataSerializers.INT);

	private @Nullable UUID truster;
	private long fedAt = -1;
	private boolean wantsWatch;

	/** Client: how far it is up on watch, down its hole, sunning, digging, asleep, sat up eating, eased, this tick's and last. */
	public float stand, standO, down, downO, sun, sunO, dig, digO, sleep, sleepO, munch, munchO;

	public SunwatchMeerkat(EntityType<? extends SunwatchMeerkat> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.3);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(POSE, MeerkatRules.IDLE);
		builder.define(LEFT, 0);
	}

	public int pose() {
		return entityData.get(POSE);
	}

	public int poseLeft() {
		return entityData.get(LEFT);
	}

	private void pose(int pose, int ticks) {
		entityData.set(POSE, pose);
		entityData.set(LEFT, ticks);
		if (pose != MeerkatRules.IDLE) {
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
		goalSelector.addGoal(5, new Huddle());
		goalSelector.addGoal(6, new Lookout());
		goalSelector.addGoal(7, new FollowTruster());
		goalSelector.addGoal(8, new FollowParentGoal(this, 1.2));
		goalSelector.addGoal(9, new WaterAvoidingRandomStrollGoal(this, 0.9));
		goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(11, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.SPIDER_EYE) || stack.is(Items.EGG);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			int pose = pose();
			standO = stand;
			stand = WildlifeRules.approach(stand, pose == MeerkatRules.WATCH ? 1 : 0, 0.18F);
			downO = down;
			down = WildlifeRules.approach(down, pose == MeerkatRules.HIDE ? 1 - 0.3F * MeerkatRules.peek(poseLeft()) : 0, 0.25F);
			sunO = sun;
			sun = WildlifeRules.approach(sun, pose == MeerkatRules.SUN ? 1 : 0, 0.08F);
			digO = dig;
			dig = WildlifeRules.approach(dig, pose == MeerkatRules.DIG ? 1 : 0, 0.2F);
			sleepO = sleep;
			sleep = WildlifeRules.approach(sleep, pose == MeerkatRules.SLEEP ? 1 : 0, 0.06F);
			munchO = munch;
			munch = WildlifeRules.approach(munch, pose == MeerkatRules.EAT ? 1 : 0, 0.15F);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		int pose = pose(), left = poseLeft();
		if (pose == MeerkatRules.HIDE) {
			// Halfway through peeking it has a good look round: still there, and it ducks straight back down.
			if (left == MeerkatRules.PEEK_TICKS / 2 && threat(level, false) != null) {
				entityData.set(LEFT, MeerkatRules.HIDE_TICKS);
				playSound(SoundEvents.FOX_AMBIENT, 0.4F, 2.0F);
				return;
			}
		} else if (tickCount % (pose == MeerkatRules.WATCH ? 5 : 10) == 0 && !isInWater()) {
			Entity threat = threat(level, pose == MeerkatRules.WATCH);
			if (threat != null) {
				alarm(level, threat);
				return;
			}
		}
		if (pose != MeerkatRules.IDLE) {
			if (pose == MeerkatRules.SLEEP && !MeerkatRules.night(level.getOverworldClockTime()) && left > 20) {
				entityData.set(LEFT, left = 20);
			}
			if (pose == MeerkatRules.DIG && left % 8 == 0) {
				scrape(level);
			}
			if (pose == MeerkatRules.EAT && left % 12 == 0) {
				munch(level);
			}
			if (pose == MeerkatRules.WATCH && left % 80 == 40) {
				// The sentry's soft little peep: all's well.
				playSound(SoundEvents.FOX_AMBIENT, 0.25F, 2.1F + random.nextFloat() * 0.1F);
			}
			if (left <= 1) {
				pose(MeerkatRules.IDLE, 0);
			} else {
				entityData.set(LEFT, left - 1);
			}
			return;
		}
		if (isInWater() || !onGround()) {
			return;
		}
		long time = level.getOverworldClockTime();
		if (MeerkatRules.night(time)) {
			return;
		}
		if (MeerkatRules.morning(time) && random.nextInt(MeerkatRules.SUN_ODDS) == 0) {
			pose(MeerkatRules.SUN, MeerkatRules.SUN_TICKS + random.nextInt(100));
		} else if (random.nextInt(MeerkatRules.DIG_ODDS) == 0) {
			pose(MeerkatRules.DIG, MeerkatRules.DIG_TICKS + random.nextInt(40));
		} else if (!isBaby() && !wantsWatch && random.nextInt(MeerkatRules.WATCH_ODDS) == 0 && MeerkatRules.takesWatch(nearestWatcher(level))) {
			wantsWatch = true;
		}
	}

	/** A stranger it doesn't trust, or a monster, near enough to see; or nobody. */
	private @Nullable Entity threat(ServerLevel level, boolean watching) {
		for (Player player : level.players()) {
			if (!player.isSpectator() && player.isAlive()
				&& MeerkatRules.threatens(distanceTo(player), watching, player.isCrouching(), trusts(player))) {
				return player;
			}
		}
		double sight = watching ? MeerkatRules.WATCH_SIGHT : MeerkatRules.GROUND_SIGHT;
		for (Monster monster : level.getEntitiesOfClass(Monster.class, getBoundingBox().inflate(sight), Entity::isAlive)) {
			if (MeerkatRules.threatens(distanceTo(monster), watching, false, false)) {
				return monster;
			}
		}
		return null;
	}

	/** The alarm: a sharp bark, and every meerkat within earshot dives down its hole. */
	private void alarm(ServerLevel level, Entity threat) {
		getLookControl().setLookAt(threat, 30, 30);
		playSound(SoundEvents.FOX_SCREECH, 0.5F, 2.0F);
		for (SunwatchMeerkat other : level.getEntitiesOfClass(SunwatchMeerkat.class, getBoundingBox().inflate(MeerkatRules.ALARM_RANGE),
			m -> m.isAlive() && !m.isInWater() && m.pose() != MeerkatRules.HIDE)) {
			other.dive(level);
		}
	}

	private void dive(ServerLevel level) {
		wantsWatch = false;
		pose(MeerkatRules.HIDE, MeerkatRules.HIDE_TICKS + random.nextInt(30));
		BlockState ground = level.getBlockState(blockPosition().below());
		if (!ground.isAir()) {
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), getX(), getY() + 0.1, getZ(), 10, 0.2, 0.1, 0.2, 0.05);
			playSound(ground.getSoundType().getBreakSound(), 0.5F, 1.4F);
		}
	}

	/** A scrape at the ground with its forepaws: a spray of whatever it is digging in. */
	private void scrape(ServerLevel level) {
		BlockState ground = level.getBlockState(blockPosition().below());
		if (ground.isAir()) {
			return;
		}
		double yaw = getYRot() * Mth.DEG_TO_RAD;
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), getX() - Math.sin(yaw) * 0.3, getY() + 0.05,
			getZ() + Math.cos(yaw) * 0.3, 4, 0.1, 0.05, 0.1, 0.08);
		playSound(ground.getSoundType().getHitSound(), 0.35F, 1.3F);
		if (random.nextInt(6) == 0) {
			// Something to eat down there: a grub, gone in a gulp.
			heal(1);
		}
	}

	/** A mouthful: crumbs and a chew. */
	private void munch(ServerLevel level) {
		double yaw = getYRot() * Mth.DEG_TO_RAD;
		level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.SPIDER_EYE), getX() - Math.sin(yaw) * 0.25, getY() + 0.55,
			getZ() + Math.cos(yaw) * 0.25, 3, 0.06, 0.05, 0.06, 0.02);
		playSound(SoundEvents.FOX_EAT, 0.4F, 1.8F);
	}

	private double nearestWatcher(ServerLevel level) {
		double best = Double.MAX_VALUE;
		for (SunwatchMeerkat other : level.getEntitiesOfClass(SunwatchMeerkat.class, getBoundingBox().inflate(MeerkatRules.WATCH_SPACING),
			m -> m != this && (m.pose() == MeerkatRules.WATCH || m.wantsWatch))) {
			best = Math.min(best, distanceTo(other));
		}
		return best;
	}

	private boolean trusts(Player player) {
		return player.getUUID().equals(truster) && MeerkatRules.trusts(level().getGameTime() - fedAt);
	}

	private @Nullable Player truster() {
		if (truster == null || !MeerkatRules.trusts(level().getGameTime() - fedAt)) {
			return null;
		}
		Player player = level().getPlayerByUUID(truster);
		return player != null && player.isAlive() && !player.isSpectator() ? player : null;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		// Right down its hole, there is nothing to hit.
		if (pose() == MeerkatRules.HIDE && poseLeft() > MeerkatRules.PEEK_TICKS && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return false;
		}
		boolean hit = super.hurtServer(level, source, amount);
		if (hit) {
			wantsWatch = false;
			if (pose() != MeerkatRules.IDLE) {
				pose(MeerkatRules.IDLE, 0);
			}
			// Its yelp sends the rest of the mob down their holes.
			Entity attacker = source.getEntity();
			if (attacker != null) {
				alarm(level, attacker);
				if (pose() == MeerkatRules.HIDE) {
					pose(MeerkatRules.IDLE, 0);
				}
			}
		}
		return hit;
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (!isFood(held) || pose() == MeerkatRules.HIDE) {
			return super.mobInteract(player, hand);
		}
		InteractionResult result = super.mobInteract(player, hand);
		if (!level().isClientSide()) {
			if (!result.consumesAction()) {
				held.consume(1, player);
			}
			// Fed, it trusts you: hearts, and it sits right up to eat what you gave it.
			truster = player.getUUID();
			fedAt = level().getGameTime();
			wantsWatch = false;
			pose(MeerkatRules.EAT, MeerkatRules.EAT_TICKS);
			level().broadcastEntityEvent(this, (byte) 18);
			playSound(SoundEvents.FOX_EAT, 0.7F, 1.8F);
		}
		return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}

	// ------------------------------------------------------------------ breeding: a pup

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return getType().create(level, EntitySpawnReason.BREEDING) instanceof SunwatchMeerkat pup ? pup : null;
	}

	// ------------------------------------------------------------------ sound: chirps and peeps

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		int pose = pose();
		return pose == MeerkatRules.SLEEP || pose == MeerkatRules.HIDE ? null : SoundEvents.FOX_AMBIENT;
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
		return super.getVoicePitch() * 1.9F;
	}

	@Override
	protected float getSoundVolume() {
		return 0.4F;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 220;
	}

	/** On watch, down its hole, sunning, digging, asleep or eating: it holds still where it is. */
	private final class Posed extends Goal {
		Posed() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return pose() != MeerkatRules.IDLE;
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

	/** Its turn on watch: up to the highest spot near, and it stands sentry there. */
	private final class Lookout extends Goal {
		private @Nullable BlockPos spot;
		private int ticks;

		Lookout() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return wantsWatch && pose() == MeerkatRules.IDLE;
		}

		@Override
		public boolean canContinueToUse() {
			return wantsWatch && pose() == MeerkatRules.IDLE && ticks < 160;
		}

		@Override
		public void start() {
			ticks = 0;
			BlockPos at = blockPosition();
			int reach = MeerkatRules.LOOKOUT_REACH, top = Integer.MIN_VALUE;
			spot = at;
			for (int dx = -reach; dx <= reach; dx++) {
				for (int dz = -reach; dz <= reach; dz++) {
					int y = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX() + dx, at.getZ() + dz);
					BlockPos pos = new BlockPos(at.getX() + dx, y, at.getZ() + dz);
					if (y > top && y <= at.getY() + 3 && level().getFluidState(pos.below()).isEmpty()) {
						top = y;
						spot = pos;
					}
				}
			}
		}

		@Override
		public void stop() {
			if (wantsWatch && ticks >= 160 && pose() == MeerkatRules.IDLE) {
				// Up there (or as near as it got): it takes its watch.
				pose(MeerkatRules.WATCH, MeerkatRules.WATCH_TICKS + random.nextInt(100));
			}
			wantsWatch = false;
			spot = null;
		}

		@Override
		public void tick() {
			if (spot == null) {
				wantsWatch = false;
				return;
			}
			ticks++;
			if (distanceToSqr(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5) < 1.2) {
				ticks = 999;
			} else if (ticks % 10 == 1) {
				getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 1.1);
			}
		}
	}

	/** At night it trots over to the others and they all go to sleep in a heap. */
	private final class Huddle extends Goal {
		private @Nullable SunwatchMeerkat near;
		private int ticks;

		Huddle() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (pose() != MeerkatRules.IDLE || tickCount % 20 != 0 || !onGround() || isInWater()
				|| !MeerkatRules.night(level().getOverworldClockTime()) || truster() != null) {
				return false;
			}
			// The nearest of the others, one already asleep if there is one.
			near = null;
			double best = Double.MAX_VALUE;
			for (SunwatchMeerkat other : level().getEntitiesOfClass(SunwatchMeerkat.class, getBoundingBox().inflate(MeerkatRules.HUDDLE_FAR),
				m -> m != SunwatchMeerkat.this && m.isAlive() && !m.isInWater())) {
				double d = distanceTo(other) - (other.pose() == MeerkatRules.SLEEP ? 4 : 0);
				if (d < best) {
					best = d;
					near = other;
				}
			}
			if (near == null || distanceTo(near) <= MeerkatRules.HUDDLE_NEAR) {
				pose(MeerkatRules.SLEEP, MeerkatRules.SLEEP_TICKS + random.nextInt(300));
				return false;
			}
			return true;
		}

		@Override
		public boolean canContinueToUse() {
			return near != null && near.isAlive() && pose() == MeerkatRules.IDLE && ticks < 200;
		}

		@Override
		public void start() {
			ticks = 0;
		}

		@Override
		public void stop() {
			if (pose() == MeerkatRules.IDLE && MeerkatRules.night(level().getOverworldClockTime())) {
				pose(MeerkatRules.SLEEP, MeerkatRules.SLEEP_TICKS + random.nextInt(300));
			}
			near = null;
			getNavigation().stop();
		}

		@Override
		public void tick() {
			if (near == null) {
				return;
			}
			ticks++;
			getLookControl().setLookAt(near, 30, 30);
			if (distanceTo(near) <= MeerkatRules.HUDDLE_NEAR + 0.3) {
				ticks = 999;
			} else if (ticks % 10 == 1) {
				getNavigation().moveTo(near, 1.0);
			}
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
			return player != null && !isBaby() && distanceTo(player) > MeerkatRules.FOLLOW_FAR && distanceTo(player) < 24;
		}

		@Override
		public boolean canContinueToUse() {
			return player != null && truster() == player && distanceTo(player) > MeerkatRules.FOLLOW_NEAR && distanceTo(player) < 24;
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
