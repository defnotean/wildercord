package dev.wildercord.monster;

import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Targets;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * The Bog Witch-Frog: a great warty frog of the swamps and mangroves, mossy green and bruise-purple, with a throat sac that
 * glows a sickly green when it fills. Its throat swells and gurgles (the tell), then it lobs a bubble of bog poison in a
 * high, slow arc: wherever it bursts, it poisons all round it. A bubble can be popped in the air by an arrow, a thrown
 * thing or a blade before it lands.
 *
 * <p>Its tongue snatches small creatures (chickens, rabbits, bats, silverfish, endermites, the smallest slimes) and it
 * swallows them whole, healing, and swelling its next bubble. At a player close by it opens its mouth wide (the tell) and
 * lashes its tongue out to drag them in; a raised shield turns it.</p>
 */
public class BogWitchFrog extends WildMonster {
	static final int SWELL = MonsterPressureRules.FROG_SWELL;
	static final int MOUTH = MonsterPressureRules.FROG_MOUTH;
	static final int GULP = MonsterPressureRules.FROG_GULP;
	static final double TONGUE_RANGE = MonsterPressureRules.FROG_TONGUE_RANGE;
	static final double BUBBLE_MIN = MonsterPressureRules.FROG_BUBBLE_MIN;
	static final double BUBBLE_MAX = MonsterPressureRules.FROG_BUBBLE_MAX;
	static final float TONGUE_DAMAGE = 2.0F;
	private static final int BOG = 0x8CD84A;
	private static final int TONGUE = 0xD86A8A;

	private enum Phase { IDLE, SWELL, MOUTH, GULP }

	private Phase phase = Phase.IDLE;
	private long phaseUntil;
	private long nextBubble;
	private long nextTongue;
	private long nextHunt;
	private Entity prey;
	/** Its last meal makes the next bubble fatter and stronger. */
	private boolean engorged;

	public BogWitchFrog(EntityType<? extends BogWitchFrog> type, Level level) {
		super(type, level);
		this.xpReward = 10;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 32.0)
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			.add(Attributes.ATTACK_DAMAGE, 5.0)
			.add(Attributes.ARMOR, 2.0)
			.add(Attributes.FOLLOW_RANGE, 24.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new ApproachGoal());
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false) {
			@Override
			public boolean canUse() {
				return phase == Phase.IDLE && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return phase == Phase.IDLE && super.canContinueToUse();
			}
		});
		goalSelector.addGoal(5, new RandomStrollGoal(this, 0.7));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.ZONE, Runes.VENOM), List.of(Runes.BOLT, Runes.BLEED), List.of(Runes.ORB, Runes.VENOM));
	}

	public boolean swelling() {
		return phase == Phase.SWELL;
	}

	public boolean engorged() {
		return engorged;
	}

	// ------------------------------------------------------------------ its bubble and its tongue

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		LivingEntity target = getTarget();
		boolean hunting = target != null && target.isAlive() && Targets.canHarm(this, target);
		switch (phase) {
			case SWELL -> {
				getNavigation().stop();
				if (target != null) {
					face(target);
				}
				if (now % 3 == 0) {
					Vec3 throat = throat();
					Motes.glows(level, throat, 1, 0.15, BOG, 0.12, 12, new Vec3(0, 0.02, 0), 0.01);
				}
				if (now >= phaseUntil) {
					enter(Phase.IDLE, now, 0);
					if (hunting && MonsterPressureRules.frogCanBubble(distanceTo(target), hasLineOfSight(target))) {
						spit(level, target);
					}
				}
			}
			case MOUTH -> {
				getNavigation().stop();
				Entity at = prey != null ? prey : target;
				if (at != null) {
					getLookControl().setLookAt(at, 60, 60);
					if (at instanceof LivingEntity living) {
						face(living);
					}
				}
				if (now >= phaseUntil) {
					tongue(level, at, now);
				}
			}
			case GULP -> {
				getNavigation().stop();
				if (now >= phaseUntil) {
					enter(Phase.IDLE, now, 0);
				}
			}
			default -> {
				if (hunting && MonsterPressureRules.frogTongueFirst(difficulty(), distanceTo(target),
					hasLineOfSight(target), now >= nextTongue)) {
					prey = null;
					openMouth(level, now);
					break;
				}
				if (hunting && now >= nextBubble && hasLineOfSight(target)) {
					double d = distanceTo(target);
					if (d >= BUBBLE_MIN && d <= BUBBLE_MAX) {
						swell(level, now);
						break;
					}
				}
				if (hunting && now >= nextTongue && hasLineOfSight(target)) {
					double d = distanceTo(target);
					if (d >= MonsterPressureRules.FROG_TONGUE_MIN && d <= TONGUE_RANGE) {
						prey = null;
						openMouth(level, now);
						break;
					}
				}
				if (now >= nextHunt) {
					nextHunt = now + 40;
					Entity snack = findPrey(level);
					if (snack != null) {
						prey = snack;
						openMouth(level, now);
					}
				}
			}
		}
		setState(WINDUP, phase == Phase.SWELL);
		setState(ALT, phase == Phase.MOUTH);
		setState(GUARD, phase == Phase.GULP);
	}

	private void enter(Phase next, long now, int ticks) {
		phase = next;
		phaseUntil = now + ticks;
	}

	/** Where its throat sac is, in the world. */
	private Vec3 throat() {
		Vec3 look = Vec3.directionFromRotation(0, yBodyRot);
		return position().add(look.scale(getBbWidth() * 0.45)).add(0, getBbHeight() * 0.35, 0);
	}

	/** The tell for a bubble: its throat sac swells, glowing, and gurgles. */
	public void swell(ServerLevel level, long now) {
		enter(Phase.SWELL, now, SWELL);
		MonsterPressureRules.Delay interval = MonsterPressureRules.frogBubble(difficulty());
		nextBubble = now + interval.sample(getRandom().nextInt(interval.spread()));
		getNavigation().stop();
		MonsterMagic.sound(level, position(), "monster_frog_swell", 1.0F, 1.0F);
	}

	/** Lobs a bubble of bog poison in a high arc at where the target stands. */
	private void spit(ServerLevel level, LivingEntity target) {
		BogBubble bubble = new BogBubble(MonsterContent.BOG_BUBBLE, level);
		Vec3 from = position().add(0, getBbHeight() * 0.85, 0).add(Vec3.directionFromRotation(0, yBodyRot).scale(getBbWidth() * 0.5));
		bubble.setOwner(this);
		bubble.setEngorged(engorged);
		bubble.setPos(from.x, from.y, from.z);
		Vec3 to = target.position().add(0, 0.2, 0);
		Vec3 d = to.subtract(from);
		int flight = MonsterRules.bubbleFlight(Math.sqrt(d.x * d.x + d.z * d.z));
		double[] v = MonsterRules.lob(d.x, d.y, d.z, MonsterRules.BUBBLE_GRAVITY, flight);
		bubble.setDeltaMovement(v[0], v[1], v[2]);
		level.addFreshEntity(bubble);
		engorged = false;
		MonsterMagic.sound(level, from, "monster_frog_spit", 1.0F, 1.0F);
	}

	/** The tell for the tongue: its mouth opens wide. */
	private void openMouth(ServerLevel level, long now) {
		enter(Phase.MOUTH, now, prey != null ? 6 : MOUTH);
		getNavigation().stop();
		if (prey == null) {
			nextTongue = now + 140 + getRandom().nextInt(60);
		}
		MonsterMagic.sound(level, position(), "monster_frog_croak", 0.8F, 1.4F);
	}

	private void tongue(ServerLevel level, Entity at, long now) {
		Vec3 mouth = position().add(0, getBbHeight() * 0.55, 0).add(Vec3.directionFromRotation(0, yBodyRot).scale(getBbWidth() * 0.5));
		if (at == null || !at.isAlive() || at.distanceTo(this) > TONGUE_RANGE + 1 || !hasLineOfSight(at)
			|| at != prey && !Targets.canHarm(this, at)) {
			enter(Phase.IDLE, now, 0);
			prey = null;
			return;
		}
		Vec3 end = at.getBoundingBox().getCenter();
		ElementFx.ray(level, mouth, end, TONGUE, 0.1, 5);
		if (at == prey) {
			swallow(level, prey, now);
			return;
		}
		enter(Phase.IDLE, now, 0);
		if (at instanceof Player player && player.isBlocking()) {
			// A raised shield turns the tongue.
			playSound(SoundEvents.SHIELD_BLOCK.value(), 1.0F, 1.0F);
			return;
		}
		if (at instanceof LivingEntity living && living.hurtServer(level, damageSources().mobAttack(this), TONGUE_DAMAGE)) {
			Vec3 pull = mouth.subtract(living.position());
			double length = pull.length();
			if (length > 1.0E-3) {
				pull = pull.scale(Math.min(1.1, length * 0.22) / length);
				MonsterMagic.shove(living, new Vec3(pull.x, 0.25, pull.z));
			}
		}
		MonsterMagic.sound(level, end, "monster_frog_tongue", 1.0F, 1.0F);
	}

	/** Something small enough to swallow, within its tongue's reach and in sight: nobody's pet, nothing named. */
	private Entity findPrey(ServerLevel level) {
		Entity best = null;
		for (Entity e : level.getEntities(this, getBoundingBox().inflate(TONGUE_RANGE), e -> e instanceof Mob mob && small(mob))) {
			if (!hasLineOfSight(e) || (best != null && e.distanceToSqr(this) >= best.distanceToSqr(this))) {
				continue;
			}
			best = e;
		}
		return best;
	}

	/** Whether a creature is a frog's snack: small, wild and nameless. */
	public static boolean small(Mob mob) {
		if (mob.hasCustomName() || mob.isPersistenceRequired() || mob instanceof OwnableEntity owned && owned.getOwnerReference() != null) {
			return false;
		}
		if (mob instanceof AbstractCubeMob cube) {
			return cube.isTiny() && !(cube instanceof ManaOoze);
		}
		return mob instanceof Chicken || mob instanceof Rabbit || mob instanceof Bat || mob instanceof Silverfish || mob instanceof Endermite;
	}

	/** Swallowed whole: the snack is gone, the frog heals, and its next bubble is fatter. */
	public void swallow(ServerLevel level, Entity snack, long now) {
		Vec3 at = snack.getBoundingBox().getCenter();
		snack.discard();
		heal(6.0F);
		engorged = true;
		prey = null;
		enter(Phase.GULP, now, GULP);
		level.sendParticles(ParticleTypes.ITEM_SLIME, at.x, at.y, at.z, 6, 0.2, 0.2, 0.2, 0.05);
		MonsterMagic.sound(level, position(), "monster_frog_gulp", 1.0F, 1.0F);
	}

	/** Ground navigation around cover or back into bubble range, with bounded retries for unreachable targets. */
	private final class ApproachGoal extends Goal {
		private long nextRepath;

		ApproachGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = getTarget();
			return phase == Phase.IDLE && target != null && target.isAlive() && Targets.canHarm(BogWitchFrog.this, target)
				&& MonsterPressureRules.frogApproach(difficulty(), distanceTo(target), hasLineOfSight(target));
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void stop() {
			getNavigation().stop();
		}

		@Override
		public void tick() {
			LivingEntity target = getTarget();
			if (target == null) return;
			getLookControl().setLookAt(target, 30, 30);
			long now = level().getGameTime();
			if (now >= nextRepath) {
				boolean routed = getNavigation().moveTo(target, MonsterPressureRules.frogChaseSpeed(difficulty()));
				nextRepath = now + MonsterPressureRules.repathDelay(
					routed && getNavigation().getPath() != null && getNavigation().getPath().canReach());
			}
		}
	}

	// ------------------------------------------------------------------ sounds

	@Override
	protected SoundEvent getAmbientSound() {
		return MonsterMagic.kit("monster_frog_ambient");
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return MonsterMagic.kit("monster_frog_hurt");
	}

	@Override
	protected SoundEvent getDeathSound() {
		return MonsterMagic.kit("monster_frog_death");
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState block) {
		playSound(SoundEvents.FROG_STEP, 0.6F, 0.6F);
	}
}
