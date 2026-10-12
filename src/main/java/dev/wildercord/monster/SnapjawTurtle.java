package dev.wildercord.monster;

import dev.wildercord.cast.Targets;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The Snapjaw Turtle: a great old snapping turtle of the rivers, broad as a doorway, its shell ridged in three rows of
 * spikes and furred with green weed, a great hooked beak, and on its tongue a little pink worm of flesh that wriggles.
 *
 * <p>It lies still on the riverbed with its jaws open and its lure wriggling, and waits. Whatever comes close enough it
 * snaps at, quicker than you'd think, and if its jaws close they hold on: you're held fast and worried every second or so,
 * till it lets go, or you hurt it enough that it does, or you're pulled free. A snap that misses leaves it a moment with its
 * jaws shut on water. Its shell turns most of a blow from above or behind; strike it face on, at its jaws. Once roused it
 * comes after you, slowly, on land or in the water, and badly hurt it pulls into its shell a while, where little can touch
 * it. Out of the water with nothing to fight, it heads back in.</p>
 *
 * <p>Its pose flags: {@code VEILED} lurking with its lure out, {@code WINDUP} the twitch before a snap, {@code ACTING} the
 * snap or a bite, {@code ALT} holding on, {@code STUNNED} a missed snap, {@code GUARD} pulled into its shell.</p>
 */
public class SnapjawTurtle extends WildMonster {
	private long snapAt = -1;
	private long nextSnap;
	private long nextBite;
	private long actingUntil;
	private long stunnedUntil;
	private long withdrawnUntil = -1;
	private long nextWithdraw;
	private long roused;
	private int dry;
	private int repath;
	private @Nullable LivingEntity held;
	private int heldTicks;
	private float harm;

	public SnapjawTurtle(EntityType<? extends SnapjawTurtle> type, Level level) {
		super(type, level);
		this.xpReward = 10;
		moveControl = new SmoothSwimmingMoveControl<>(this, 85, 10, 0.6F, 1.0F, false);
		setPathfindingMalus(PathType.WATER, 0);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 40.0)
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			.add(Attributes.ATTACK_DAMAGE, 7.0)
			.add(Attributes.ARMOR, 6.0)
			.add(Attributes.FOLLOW_RANGE, 16.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(6, new RandomStrollGoal(this, 0.8, 240) {
			@Override
			public boolean canUse() {
				return getTarget() == null && held == null && super.canUse();
			}
		});
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new AmphibiousPathNavigation(this, level);
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	/** In the water it walks the bottom and swims slowly; lurking, it settles onto the riverbed. */
	@Override
	public void travel(Vec3 input) {
		if (isEffectiveAi() && isInWater()) {
			moveRelative(getSpeed(), input);
			move(MoverType.SELF, getDeltaMovement());
			Vec3 motion = getDeltaMovement().scale(0.9);
			if (getNavigation().isDone() && !onGround()) {
				motion = motion.add(0, -0.02, 0);
			}
			setDeltaMovement(motion);
		} else {
			super.travel(input);
		}
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.TOUCH, Runes.VENOM), List.of(Runes.SELF, Runes.FROST));
	}

	private boolean withdrawn(long now) {
		return withdrawnUntil >= 0 && now < withdrawnUntil;
	}

	// ------------------------------------------------------------------ the lurk and the snap

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		setState(ACTING, now < actingUntil);
		setState(STUNNED, now < stunnedUntil);

		if (withdrawn(now)) {
			setState(GUARD, true);
			setState(VEILED, false);
			setState(WINDUP, false);
			let(now);
			snapAt = -1;
			getNavigation().stop();
			return;
		}
		if (withdrawnUntil >= 0) {
			withdrawnUntil = -1;
			setState(GUARD, false);
			playSound(SoundEvents.TURTLE_AMBIENT_LAND, 1.0F, 0.6F);
		}

		if (held != null) {
			hold(level, now);
			return;
		}
		setState(ALT, false);
		if (now < stunnedUntil) {
			getNavigation().stop();
			return;
		}

		LivingEntity target = getTarget();
		boolean hunting = target != null && target.isAlive() && Targets.canHarm(this, target);
		if (!hunting) {
			snapAt = -1;
			setState(WINDUP, false);
			lurk(level);
			return;
		}
		double distance = distanceTo(target);
		getLookControl().setLookAt(target, 30, 30);

		if (snapAt >= 0) {
			face(target);
			if (now >= snapAt) {
				snap(level, target, now);
			}
			return;
		}
		boolean angry = now < roused;
		if (SnapjawRules.snaps(distance, now, nextSnap, false) && hasLineOfSight(target)) {
			snapAt = now + SnapjawRules.SNAP_WINDUP;
			setState(WINDUP, true);
			setState(VEILED, false);
			getNavigation().stop();
			return;
		}
		if (!angry) {
			// Not yet roused: it lies still, turning a little to keep its lure towards you.
			setState(VEILED, true);
			getNavigation().stop();
			if (distance < 8) {
				face(target);
			}
			return;
		}
		setState(VEILED, false);
		if (distance <= SnapjawRules.BITE_REACH && now >= nextBite) {
			nextBite = now + SnapjawRules.BITE_COOLDOWN;
			face(target);
			doHurtTarget(level, target);
			actingUntil = now + 6;
			setState(ACTING, true);
			playSound(SoundEvents.EVOKER_FANGS_ATTACK, 0.8F, 1.4F);
		}
		if (--repath <= 0 || getNavigation().isDone()) {
			repath = 10;
			getNavigation().moveTo(target, 1.0);
		}
	}

	/** Nothing to fight: in the water it lies still with its lure out; on land it heads for the water. */
	private void lurk(ServerLevel level) {
		if (isInWater()) {
			dry = 0;
			setState(VEILED, getNavigation().isDone());
			return;
		}
		setState(VEILED, false);
		if (++dry >= SnapjawRules.DRY_TICKS && dry % 20 == 0) {
			BlockPos water = nearWater(level);
			if (water != null) {
				getNavigation().moveTo(water.getX() + 0.5, water.getY(), water.getZ() + 0.5, 1.0);
			}
		}
	}

	private @Nullable BlockPos nearWater(ServerLevel level) {
		BlockPos at = blockPosition();
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (BlockPos pos : BlockPos.betweenClosed(at.offset(-8, -3, -8), at.offset(8, 2, 8))) {
			if (level.getFluidState(pos).is(FluidTags.WATER)) {
				double d = pos.distSqr(at);
				if (d < bestDistance) {
					bestDistance = d;
					best = pos.immutable();
				}
			}
		}
		return best;
	}

	/** The snap: a lunge and its jaws shut, on you if you're still there, else on water. */
	private void snap(ServerLevel level, LivingEntity target, long now) {
		snapAt = -1;
		nextSnap = now + SnapjawRules.SNAP_COOLDOWN;
		nextBite = now + SnapjawRules.BITE_COOLDOWN;
		roused = now + 400;
		actingUntil = now + 8;
		setState(WINDUP, false);
		setState(ACTING, true);
		Vec3 toward = target.position().subtract(position()).multiply(1, 0, 1);
		if (toward.lengthSqr() > 1.0E-4) {
			setDeltaMovement(getDeltaMovement().add(toward.normalize().scale(0.45)));
		}
		playSound(SoundEvents.EVOKER_FANGS_ATTACK, 1.2F, 1.2F);
		if (distanceTo(target) <= SnapjawRules.SNAP_REACH + 0.6 && doHurtTarget(level, target)) {
			held = target;
			heldTicks = 0;
			harm = 0;
			setState(ALT, true);
			playSound(SoundEvents.TURTLE_HURT, 1.0F, 0.5F);
		} else {
			stunnedUntil = now + 20;
			setState(STUNNED, true);
			level.sendParticles(ParticleTypes.BUBBLE, getX(), getY() + 0.4, getZ(), 10, 0.4, 0.2, 0.4, 0.05);
		}
	}

	/** Holding on: its prey held fast, worried now and then, till it lets go. */
	private void hold(ServerLevel level, long now) {
		LivingEntity prey = held;
		setState(ALT, true);
		setState(VEILED, false);
		getNavigation().stop();
		heldTicks++;
		if (prey == null || !prey.isAlive() || !Targets.canHarm(this, prey) || SnapjawRules.letsGo(heldTicks, harm, distanceTo(prey))) {
			let(now);
			return;
		}
		face(prey);
		prey.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, 6, false, false));
		// Its jaws drag its prey in towards it a little.
		Vec3 jaws = position().add(getLookAngle().multiply(1.2, 0, 1.2));
		Vec3 pull = jaws.subtract(prey.position()).multiply(1, 0, 1);
		if (pull.lengthSqr() > 0.25) {
			prey.push(pull.normalize().scale(0.08));
			prey.needsSync = true;
		}
		if (SnapjawRules.worries(heldTicks)) {
			prey.hurtServer(level, damageSources().mobAttack(this), SnapjawRules.WORRY_DAMAGE);
			actingUntil = now + 5;
			setState(ACTING, true);
			playSound(SoundEvents.EVOKER_FANGS_ATTACK, 0.6F, 0.8F);
		}
	}

	/** Its jaws open and whatever it held goes free. */
	private void let(long now) {
		if (held != null) {
			held = null;
			nextSnap = now + SnapjawRules.SNAP_COOLDOWN;
			setState(ALT, false);
		}
	}

	// ------------------------------------------------------------------ its shell

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		long now = level.getGameTime();
		Vec3 from = source.getSourcePosition();
		boolean shell = false;
		if (from != null && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
			Vec3 flat = from.subtract(position()).multiply(1, 0, 1);
			double dot = 1;
			if (flat.lengthSqr() > 1.0E-4) {
				float yaw = yBodyRot * Mth.DEG_TO_RAD;
				Vec3 facing = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
				dot = flat.normalize().dot(facing);
			}
			shell = SnapjawRules.onShell(from.y - (getY() + getBbHeight()), dot);
		}
		boolean in = withdrawn(now);
		float dealt = SnapjawRules.damage(damage, shell, in);
		if (shell || in) {
			playSound(SoundEvents.SHIELD_BLOCK.value(), 0.8F, 0.7F);
		}
		boolean hurt = super.hurtServer(level, source, dealt);
		if (hurt && isAlive()) {
			roused = now + 400;
			if (held != null) {
				harm += dealt;
			}
			if (!in && SnapjawRules.withdraws(getHealth(), getMaxHealth(), now, nextWithdraw)) {
				withdrawnUntil = now + SnapjawRules.WITHDRAW_TICKS;
				nextWithdraw = now + SnapjawRules.WITHDRAW_COOLDOWN;
				setState(GUARD, true);
				let(now);
				playSound(SoundEvents.TURTLE_SHAMBLE, 1.0F, 0.5F);
			}
		}
		return hurt;
	}

	// ------------------------------------------------------------------ sounds

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return isInWater() ? null : SoundEvents.TURTLE_AMBIENT_LAND;
	}

	@Override
	public float getVoicePitch() {
		return 0.5F + random.nextFloat() * 0.1F;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.TURTLE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.TURTLE_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState block) {
		playSound(SoundEvents.TURTLE_SHAMBLE, 0.4F, 0.6F);
	}
}
