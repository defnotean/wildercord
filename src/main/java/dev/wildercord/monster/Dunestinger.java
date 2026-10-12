package dev.wildercord.monster;

import dev.wildercord.cast.Targets;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The Dunestinger: a sand scorpion of the deserts and badlands, the colour of the dunes, as broad as a sheep and low to
 * the ground, with two heavy pincers and a jointed tail curled up over its back to a dark, venomous sting. They keep in
 * little packs, and wait buried, only their eye stalks and the tip of a sting above the sand.
 *
 * <p>Walk too close and one feels you through the sand (running feet carry much further; a crouching step hardly at all)
 * and bursts up, and the rest of its pack bursts up round it. Close in it snaps its pincers; given a moment it draws its
 * tail back over itself and drives the sting down, and the sting poisons. Hurt badly on sand, it goes back under, out of
 * reach, and comes up again right beside whoever it was fighting. With nothing left to hunt, it buries itself again to
 * wait. The sun does not trouble it.</p>
 *
 * <p>Its pose flags: {@code VEILED} buried, {@code WINDUP} the sting drawn back, {@code ACTING} the sting driven down,
 * {@code GUARD} a pincer snap, {@code ALT} gone under mid-fight.</p>
 */
public class Dunestinger extends WildMonster {
	private long nextPinch;
	private long pinchUntil;
	private long nextSting;
	private long stingAt = -1;
	private long stingUntil;
	private long nextBurrow;
	private long burrowUntil = -1;
	private int idle;
	private int repath;

	public Dunestinger(EntityType<? extends Dunestinger> type, Level level) {
		super(type, level);
		this.xpReward = 7;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 18.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.ATTACK_DAMAGE, DunestingerRules.PINCH_DAMAGE)
			.add(Attributes.ARMOR, 4.0)
			.add(Attributes.FOLLOW_RANGE, 24.0)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7) {
			@Override
			public boolean canUse() {
				return !buried() && !burrowing() && getTarget() == null && super.canUse();
			}
		});
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		// Buried, it feels for footsteps instead (see below); up, it hunts like anything else.
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
			(target, level) -> !buried()));
	}

	@Override
	protected boolean packHunter() {
		return true;
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.TOUCH, Runes.VENOM), List.of(Runes.BOLT, Runes.VENOM));
	}

	/** Whether it's lying buried in wait. */
	public boolean buried() {
		return state(VEILED);
	}

	/** Whether it's gone under mid-fight. */
	public boolean burrowing() {
		return state(ALT);
	}

	/** Buries it (or brings it up) at once, for spawns and the tests. */
	public void setBuried(boolean buried) {
		setState(VEILED, buried);
	}

	private boolean onSand() {
		return onGround() && level().getBlockState(getBlockPosBelowThatAffectsMyMovement()).is(BlockTags.SAND);
	}

	// ------------------------------------------------------------------ the sand

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		LivingEntity target = getTarget();
		boolean live = target != null && target.isAlive() && Targets.canHarm(this, target);

		if (buried()) {
			hold();
			if (live) {
				burst(level, target);
			} else {
				Player feet = feel(level);
				if (feet != null) {
					burst(level, feet);
				}
			}
			return;
		}

		if (burrowing()) {
			hold();
			if (random.nextInt(3) == 0) {
				sand(level, position(), 3, 0.15);
			}
			if (now >= burrowUntil) {
				resurface(level, live ? target : null, now);
			}
			return;
		}

		setState(GUARD, now < pinchUntil);
		setState(ACTING, now < stingUntil);
		if (stingAt >= 0) {
			setState(WINDUP, true);
			if (live) {
				face(target);
				getNavigation().stop();
			}
			if (now >= stingAt) {
				sting(level, live ? target : null, now);
			}
			return;
		}
		setState(WINDUP, false);

		if (live) {
			idle = 0;
			if (DunestingerRules.burrows(getHealth(), getMaxHealth(), onSand(), now, nextBurrow)) {
				burrow(level, now);
				return;
			}
			hunt(level, target, now);
		} else if (onSand() && ++idle >= DunestingerRules.REBURY_TICKS) {
			idle = 0;
			bury(level);
		}
	}

	/** Lying still under the sand: no walking. */
	private void hold() {
		getNavigation().stop();
		setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
	}

	/** Whoever is near enough, by how they're moving, to be felt through the sand. */
	private @Nullable Player feel(ServerLevel level) {
		Player nearest = null;
		double best = Double.MAX_VALUE;
		for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(DunestingerRules.SPRINT_WAKE_RANGE))) {
			if (player.isSpectator() || player.isCreative() || !player.isAlive()) {
				continue;
			}
			double distance = player.distanceTo(this);
			if (DunestingerRules.wakes(distance, player.isSprinting(), player.isShiftKeyDown()) && distance < best) {
				nearest = player;
				best = distance;
			}
		}
		return nearest;
	}

	/** Up out of the sand in a spray of it, and the rest of the pack with it. */
	private void burst(ServerLevel level, LivingEntity target) {
		setState(VEILED, false);
		idle = 0;
		setDeltaMovement(getDeltaMovement().add(0, 0.3, 0));
		sand(level, position(), 24, 0.25);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SAND_BREAK, SoundSource.HOSTILE, 1.2F, 0.7F);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 0.8F, 1.4F);
		face(target);
		// It wakes ready to snap but not yet to sting: there's a moment to back off.
		nextSting = level.getGameTime() + DunestingerRules.STING_COOLDOWN / 2;
		setTarget(target);
		for (Dunestinger kin : level.getEntitiesOfClass(Dunestinger.class, getBoundingBox().inflate(DunestingerRules.PACK_RANGE),
				other -> other != this && other.isAlive() && other.buried())) {
			kin.setTarget(target);
		}
	}

	/** Back under to wait. */
	private void bury(ServerLevel level) {
		getNavigation().stop();
		setState(VEILED, true);
		sand(level, position(), 12, 0.1);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SAND_PLACE, SoundSource.HOSTILE, 0.8F, 0.8F);
	}

	/** Comes on, and snaps or stings when close. */
	private void hunt(ServerLevel level, LivingEntity target, long now) {
		double distance = distanceTo(target);
		if (--repath <= 0 || getNavigation().isDone()) {
			repath = 6;
			getNavigation().moveTo(target, 1.25);
		}
		getLookControl().setLookAt(target, 30, 30);
		switch (DunestingerRules.attack(distance, now, nextPinch, nextSting)) {
			case 2 -> {
				// The tail drawn back over its back: a tell, and a moment to get clear.
				stingAt = now + DunestingerRules.STING_WINDUP;
				nextSting = now + DunestingerRules.STING_COOLDOWN;
				face(target);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 0.9F, 1.7F);
			}
			case 1 -> {
				face(target);
				nextPinch = now + DunestingerRules.PINCH_COOLDOWN;
				pinchUntil = now + DunestingerRules.PINCH_TICKS;
				doHurtTarget(level, target);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.ARMADILLO_HURT_REDUCED, SoundSource.HOSTILE, 0.9F, 1.6F);
			}
			default -> { }
		}
	}

	/** The sting driven down: it poisons whatever is still within reach. */
	private void sting(ServerLevel level, @Nullable LivingEntity target, long now) {
		stingAt = -1;
		stingUntil = now + DunestingerRules.STING_TICKS;
		setState(WINDUP, false);
		setState(ACTING, true);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEE_STING, SoundSource.HOSTILE, 1.0F, 0.6F);
		if (target != null && distanceTo(target) <= DunestingerRules.STING_REACH + 0.6
				&& target.hurtServer(level, damageSources().mobAttack(this), DunestingerRules.STING_DAMAGE)) {
			target.addEffect(new MobEffectInstance(MobEffects.POISON, DunestingerRules.POISON_TICKS, DunestingerRules.POISON_LEVEL), this);
		}
	}

	/** Hurt: under the sand, where nothing can reach it, for a moment. */
	private void burrow(ServerLevel level, long now) {
		burrowUntil = now + DunestingerRules.BURROW_TICKS;
		nextBurrow = burrowUntil + DunestingerRules.BURROW_COOLDOWN;
		stingAt = -1;
		setState(WINDUP, false);
		setState(ALT, true);
		hold();
		sand(level, position(), 20, 0.15);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SAND_BREAK, SoundSource.HOSTILE, 1.0F, 0.6F);
	}

	/** Up again, right beside whoever it was fighting if it can find room there. */
	private void resurface(ServerLevel level, @Nullable LivingEntity target, long now) {
		burrowUntil = -1;
		if (target != null) {
			Vec3 spot = beside(level, target);
			if (spot != null) {
				teleportTo(spot.x, spot.y, spot.z);
				face(target);
			}
			nextPinch = now;
		}
		setState(ALT, false);
		setDeltaMovement(0, 0.3, 0);
		sand(level, position(), 24, 0.25);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SAND_BREAK, SoundSource.HOSTILE, 1.2F, 0.7F);
	}

	/** A place to come up beside someone: open, on firm ground, a block or two away. */
	private @Nullable Vec3 beside(ServerLevel level, LivingEntity target) {
		for (int i = 0; i < 12; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double r = 1.5 + random.nextDouble();
			double x = target.getX() + Math.cos(angle) * r;
			double z = target.getZ() + Math.sin(angle) * r;
			for (int dy : new int[] {0, 1, -1, 2, -2}) {
				BlockPos pos = BlockPos.containing(x, target.getY() + dy, z);
				BlockPos below = pos.below();
				if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
					continue;
				}
				Vec3 spot = new Vec3(x, pos.getY(), z);
				AABB box = getDimensions(getPose()).makeBoundingBox(spot);
				if (level.noCollision(this, box) && !level.containsAnyLiquid(box)) {
					return spot;
				}
			}
		}
		return null;
	}

	/** Sand thrown up, of whatever it's lying in. */
	private void sand(ServerLevel level, Vec3 at, int count, double speed) {
		BlockState ground = level.getBlockState(BlockPos.containing(at.x, at.y - 0.2, at.z));
		if (ground.isAir()) {
			ground = Blocks.SAND.defaultBlockState();
		}
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.2, at.z, count, 0.5, 0.2, 0.5, speed);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		// Gone under mid-fight, nothing reaches it.
		if (burrowing() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return false;
		}
		return super.hurtServer(level, source, damage);
	}

	@Override
	public boolean isPushable() {
		return !buried() && !burrowing() && super.isPushable();
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		// Born on sand, it's already lying in wait.
		if (level.getBlockState(blockPosition().below()).is(BlockTags.SAND)) {
			setState(VEILED, true);
		}
		return data;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Buried", buried());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setState(VEILED, input.getBooleanOr("Buried", false));
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return buried() ? null : SoundEvents.SILVERFISH_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SPIDER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SPIDER_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState block) {
		playSound(SoundEvents.SPIDER_STEP, 0.3F, 1.3F);
	}
}
