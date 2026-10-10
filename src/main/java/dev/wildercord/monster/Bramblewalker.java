package dev.wildercord.monster;

import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Targets;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * The Bramblewalker: a hunched thicket that walks the forests at night, all bark and thorn and creeping vine, with two
 * pale green lights deep in its tangle. It's slow and it hits hard up close, but its real threat is the lash: it rears
 * back (its vines glowing and drawing in) and whips a vine out along the ground at where you stood a moment before,
 * rooting whatever it catches for a breath. Step aside as it rears and the lash cracks on empty ground.
 *
 * <p>It's dry wood: fire hurts it half again as much, and a Bramblewalker set alight panics and runs, crackling, until
 * the flames go out and a little after. That's the counter: a fire spell, a flint and steel, a Fire Aspect blade.</p>
 */
public class Bramblewalker extends WildMonster {
	/** The tell: from rearing back to the crack of the vine. */
	static final int LASH_WINDUP = 16;
	/** The aim fixes this many ticks before the lash, so stepping aside late enough still works. */
	static final int LASH_LOCK = 5;
	static final double LASH_RANGE = 7.0;
	static final double LASH_MIN = 2.6;
	static final float LASH_DAMAGE = 3.0F;
	private static final int VINE = 0x4E9A3C;

	/** When the lash cracks (0: none coming). */
	private long lashAt;
	private long nextLash;
	private long actingUntil;
	private Vec3 lashAim;
	/** When it stops running from fire. */
	private long fleeUntil;

	public Bramblewalker(EntityType<? extends Bramblewalker> type, Level level) {
		super(type, level);
		this.xpReward = 8;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 30.0)
			.add(Attributes.MOVEMENT_SPEED, 0.21)
			.add(Attributes.ATTACK_DAMAGE, 5.0)
			.add(Attributes.ARMOR, 4.0)
			.add(Attributes.FOLLOW_RANGE, 24.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new FleeFireGoal());
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false) {
			@Override
			public boolean canUse() {
				return lashAt == 0 && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return lashAt == 0 && super.canContinueToUse();
			}
		});
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.WAVE, Runes.ROOT), List.of(Runes.BOLT, Runes.VENOM), List.of(Runes.RING, Runes.ROOT));
	}

	/** Whether it's running from fire right now. */
	public boolean fleeing() {
		return isOnFire() || level().getGameTime() < fleeUntil;
	}

	/** Whether its lash is being wound up right now (the tell). */
	public boolean windingUp() {
		return lashAt > 0;
	}

	// ------------------------------------------------------------------ the lash

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		if (isOnFire()) {
			fleeUntil = now + MonsterRules.FLEE_AFTER_FIRE;
			if (now % 4 == 0) {
				smoulder(level);
			}
		}
		if (actingUntil > 0 && now >= actingUntil) {
			actingUntil = 0;
			setState(ACTING, false);
		}
		if (fleeing()) {
			// Burning, it forgets everything but running.
			if (lashAt > 0) {
				lashAt = 0;
				setState(WINDUP, false);
			}
			return;
		}
		LivingEntity target = getTarget();
		if (lashAt > 0) {
			getNavigation().stop();
			if (target != null && target.isAlive()) {
				face(target);
				if (lashAim == null && now >= lashAt - LASH_LOCK) {
					lashAim = target.getBoundingBox().getCenter();
				}
			}
			if (now % 3 == 0) {
				// The vines draw in, glowing.
				Vec3 c = position().add(0, getBbHeight() * 0.6, 0);
				Motes.glows(level, c, 2, 0.8, VINE, 0.12, 10, new Vec3(0, 0.02, 0), 0.02);
			}
			if (now >= lashAt) {
				lashAt = 0;
				setState(WINDUP, false);
				lash(level, target);
			}
			return;
		}
		if (target != null && target.isAlive() && now >= nextLash && Targets.canHarm(this, target) && hasLineOfSight(target)) {
			double d = distanceTo(target);
			if (d >= LASH_MIN && d <= LASH_RANGE) {
				windUp(level, now);
			}
		}
	}

	/** The tell: it rears back, its vines glowing and drawing in, and creaks. */
	public void windUp(ServerLevel level, long now) {
		lashAt = now + LASH_WINDUP;
		nextLash = now + 110 + getRandom().nextInt(50);
		lashAim = null;
		setState(WINDUP, true);
		getNavigation().stop();
		MonsterMagic.sound(level, position(), "monster_bramble_rear", 1.0F, 1.0F);
	}

	/** The vine whips out along a line at where the target was as the aim fixed, rooting what it catches. */
	private void lash(ServerLevel level, LivingEntity target) {
		Vec3 from = position().add(0, getBbHeight() * 0.55, 0);
		Vec3 aim = lashAim != null ? lashAim : target != null ? target.getBoundingBox().getCenter() : from.add(getLookAngle());
		lashAim = null;
		Vec3 dir = aim.subtract(from);
		if (dir.lengthSqr() < 1.0E-4) {
			return;
		}
		dir = dir.normalize();
		Vec3 end = from.add(dir.scale(LASH_RANGE));
		end = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getLocation();
		setState(ACTING, true);
		actingUntil = level.getGameTime() + 8;
		int ticks = MonsterRules.rootTicks(difficulty());
		AABB reach = new AABB(from, end).inflate(1.0);
		for (Entity e : level.getEntities(this, reach, e -> Targets.canHarm(this, e))) {
			LivingEntity t = (LivingEntity) e;
			if (distanceToSegment(t.getBoundingBox().getCenter(), from, end) > 0.6 + t.getBbWidth() * 0.5) {
				continue;
			}
			DamageSource source = damageSources().mobAttack(this);
			t.hurtServer(level, source, LASH_DAMAGE);
			MonsterMagic.root(level, t, ticks);
		}
		// The vine itself: a thread of green sap-light, leaves torn off along it.
		ElementFx.ray(level, from, end, VINE, 0.07, 8);
		double length = from.distanceTo(end);
		for (double s = 0.8; s < length; s += 0.9) {
			Vec3 at = from.add(dir.scale(s));
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState()), at.x, at.y, at.z, 2, 0.1, 0.1, 0.1, 0.02);
		}
		ElementFx.petals(level, end, 0.4, 4);
		MonsterMagic.sound(level, from.add(dir.scale(length * 0.5)), "monster_bramble_lash", 1.1F, 1.0F);
	}

	private static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
		Vec3 ab = b.subtract(a);
		double t = Math.max(0, Math.min(1, p.subtract(a).dot(ab) / Math.max(1.0E-6, ab.lengthSqr())));
		return p.distanceTo(a.add(ab.scale(t)));
	}

	/** Burning: it crackles, and smoke pours off it. */
	private void smoulder(ServerLevel level) {
		Vec3 c = position().add(0, getBbHeight() * 0.6, 0);
		Motes.smoke(level, c, 2, 0.4);
		ElementFx.embers(level, c, 0.5, 2);
	}

	// ------------------------------------------------------------------ fire

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		// Dry wood: fire that isn't a spell's (a spell's goes through its weakness to fire) burns it half again as hard.
		if (source.is(DamageTypeTags.IS_FIRE) && !MonsterMagic.spellLanding()) {
			damage *= 1.5F;
		}
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt && isOnFire() && lashAt > 0) {
			lashAt = 0;
			setState(WINDUP, false);
		}
		return hurt;
	}

	/** Runs from whoever is near while it burns, and a little after. */
	private final class FleeFireGoal extends Goal {
		private Vec3 to;

		FleeFireGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return fleeing() && pick();
		}

		@Override
		public boolean canContinueToUse() {
			return fleeing();
		}

		private boolean pick() {
			Entity threat = getTarget() != null ? getTarget() : getLastHurtByMob();
			if (threat == null) {
				threat = level().getNearestPlayer(Bramblewalker.this, 16);
			}
			Vec3 away = threat != null ? threat.position() : position().add(getLookAngle());
			to = DefaultRandomPos.getPosAway(Bramblewalker.this, 16, 6, away);
			return to != null;
		}

		@Override
		public void start() {
			getNavigation().moveTo(to.x, to.y, to.z, 1.6);
			if (level() instanceof ServerLevel level) {
				MonsterMagic.sound(level, position(), "monster_bramble_hurt", 1.0F, 1.3F);
			}
		}

		@Override
		public void tick() {
			if (getNavigation().isDone() && pick()) {
				getNavigation().moveTo(to.x, to.y, to.z, 1.6);
			}
		}

		@Override
		public void stop() {
			getNavigation().stop();
		}
	}

	// ------------------------------------------------------------------ sounds

	@Override
	protected SoundEvent getAmbientSound() {
		return MonsterMagic.kit("monster_bramble_ambient");
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return MonsterMagic.kit("monster_bramble_hurt");
	}

	@Override
	protected SoundEvent getDeathSound() {
		return MonsterMagic.kit("monster_bramble_death");
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState block) {
		playSound(SoundEvents.AZALEA_LEAVES_STEP, 0.5F, 0.8F);
		playSound(SoundEvents.WOOD_STEP, 0.35F, 0.6F);
	}

	/** 0.12: Frost where it's freezing, Ash where it's scorched. */
	@Override
	protected boolean hasVariants() {
		return true;
	}
}
