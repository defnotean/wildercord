package dev.wildercord.monster;

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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The Dripfang Lurker: a long pale cave crawler of the dripstone caves, its back a tapering stone-grey shell ending in a
 * spike, its many thin legs folded tight along it. It hangs from the cave roof, tail down, among the dripstone, and looks
 * just like one more stalactite.
 *
 * <p>Walk under it and it lets go, spike first, and whoever it lands on takes the full weight and is left slowed. Then it
 * scuttles after you and bites. Left alone a while it climbs back up to wait again. Strike or shoot one while it hangs and
 * it falls badly, landing on its back, legs waving, and blows land half again as hard till it rights itself. The only tell
 * is a faint clicking from above when someone is near, and now and then a twitch of a leg.</p>
 *
 * <p>Its pose flags: {@code GUARD} hanging, {@code ACTING} falling spike-first, {@code STUNNED} on its back,
 * {@code WINDUP} a twitch, {@code ALT} climbing.</p>
 */
public class DripfangLurker extends WildMonster {
	private enum Phase { ROAM, HANG, FALL, FLIPPED, CLIMB }

	private Phase phase = Phase.ROAM;
	private boolean placed;
	private boolean badFall;
	private long phaseUntil;
	private long twitchUntil;
	private long nextBite;
	private int idle;
	private int falling;
	private Vec3 hangAt = Vec3.ZERO;

	public DripfangLurker(EntityType<? extends DripfangLurker> type, Level level) {
		super(type, level);
		this.xpReward = 8;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 26.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.ATTACK_DAMAGE, 4.0)
			.add(Attributes.ARMOR, 6.0)
			.add(Attributes.FOLLOW_RANGE, 20.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8) {
			@Override
			public boolean canUse() {
				return getTarget() == null && phase == Phase.ROAM && super.canUse();
			}
		});
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8) {
			@Override
			public boolean canUse() {
				return phase == Phase.ROAM && super.canUse();
			}
		});
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
			@Override
			public boolean canUse() {
				// Hanging, it waits for prey to come under it rather than going after it.
				return phase != Phase.HANG && phase != Phase.CLIMB && super.canUse();
			}
		});
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.TOUCH, Runes.VENOM), List.of(Runes.TOUCH, Runes.PUSH));
	}

	/** Whether it hangs from the roof, still as stone. */
	public boolean hanging() {
		return phase == Phase.HANG;
	}

	// ------------------------------------------------------------------ the wait

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		LivingEntity target = getTarget();
		boolean hunting = target != null && target.isAlive() && Targets.canHarm(this, target);
		if (!placed) {
			// Born (or loaded) where it can, it goes straight up to the roof.
			placed = true;
			if (!hunting && phase == Phase.ROAM) {
				BlockPos roof = roof(level);
				if (roof != null) {
					hang(roof);
				}
			}
		}

		switch (phase) {
			case HANG -> waiting(level, now);
			case FALL -> fall(level, now);
			case FLIPPED -> {
				getNavigation().stop();
				if (now >= phaseUntil) {
					phase = Phase.ROAM;
					playSound(SoundEvents.SPIDER_STEP, 1.0F, 0.6F);
				}
			}
			case CLIMB -> climb(level, hunting);
			default -> roam(level, target, hunting, now);
		}
		if (phase != Phase.HANG && phase != Phase.CLIMB && isNoGravity()) {
			setNoGravity(false);
		}
		setState(GUARD, phase == Phase.HANG);
		setState(ACTING, phase == Phase.FALL && !badFall);
		setState(STUNNED, phase == Phase.FLIPPED || phase == Phase.FALL && badFall);
		setState(WINDUP, now < twitchUntil);
		setState(ALT, phase == Phase.CLIMB);
	}

	/** Still on the roof, watching what passes under. */
	private void waiting(ServerLevel level, long now) {
		getNavigation().stop();
		setNoGravity(true);
		setDeltaMovement(Vec3.ZERO);
		if (position().distanceToSqr(hangAt) > 0.01) {
			setPos(hangAt);
		}
		if ((tickCount + getId()) % 4 != 0) {
			return;
		}
		boolean near = false;
		for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(DripfangRules.TWITCH_RANGE, DripfangRules.DROP_DEPTH, DripfangRules.TWITCH_RANGE),
			p -> p.isAlive() && Targets.canHarm(this, p))) {
			double sideways = Math.sqrt(Math.pow(player.getX() - getX(), 2) + Math.pow(player.getZ() - getZ(), 2));
			double below = getY() - player.getY();
			if (DripfangRules.drops(sideways, below) && hasLineOfSight(player)) {
				setTarget(player);
				let(level, false);
				return;
			}
			near |= DripfangRules.twitches(distanceTo(player));
		}
		if (near && now >= twitchUntil && random.nextInt(12) == 0) {
			twitchUntil = now + 10;
			playSound(SoundEvents.SPIDER_STEP, 0.35F, 1.7F);
			level.sendParticles(ParticleTypes.DRIPPING_DRIPSTONE_WATER, getX(), getY() - 0.4, getZ(), 1, 0.05, 0, 0.05, 0);
		}
	}

	/** Lets go of the roof: spike first onto its prey, or badly, struck from it. */
	private void let(ServerLevel level, boolean badly) {
		phase = Phase.FALL;
		badFall = badly;
		falling = 0;
		setNoGravity(false);
		setDeltaMovement(0, -0.3, 0);
		playSound(SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, 1.0F, 0.7F);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DRIPSTONE_BLOCK.defaultBlockState()),
			getX(), getY() + getBbHeight(), getZ(), 8, 0.3, 0.05, 0.3, 0.05);
	}

	/** Falling, spike first; it strikes whatever it comes down on, or the ground. */
	private void fall(ServerLevel level, long now) {
		getNavigation().stop();
		falling++;
		Vec3 v = getDeltaMovement();
		setDeltaMovement(v.x * 0.5, Math.max(-1.6, v.y - 0.12), v.z * 0.5);
		boolean onPrey = !badFall && !level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.1),
			e -> e != this && e.isAlive() && Targets.canHarm(this, e)).isEmpty();
		if (onGround() || onPrey || falling > 100) {
			land(level, now);
		}
	}

	private void land(ServerLevel level, long now) {
		BlockState stone = Blocks.DRIPSTONE_BLOCK.defaultBlockState();
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, stone), getX(), getY() + 0.1, getZ(), 18, 0.5, 0.1, 0.5, 0.15);
		playSound(SoundEvents.POINTED_DRIPSTONE_LAND, 1.6F, 0.7F);
		setDeltaMovement(getDeltaMovement().multiply(0.2, 0, 0.2));
		if (badFall) {
			phase = Phase.FLIPPED;
			phaseUntil = now + DripfangRules.FLIPPED_TICKS;
			playSound(SoundEvents.SPIDER_HURT, 1.0F, 0.6F);
			return;
		}
		for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(DripfangRules.DROP_RADIUS, 1.0, DripfangRules.DROP_RADIUS),
			e -> e != this && e.isAlive() && !(e instanceof DripfangLurker) && Targets.canHarm(this, e))) {
			double distance = Math.sqrt(Math.pow(hit.getX() - getX(), 2) + Math.pow(hit.getZ() - getZ(), 2));
			float damage = DripfangRules.landing(distance);
			if (damage > 0 && hit.hurtServer(level, damageSources().mobAttack(this), damage)) {
				hit.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, DripfangRules.SLOW_TICKS, 1, false, true), this);
				playSound(SoundEvents.POINTED_DRIPSTONE_BREAK, 1.2F, 0.8F);
			}
		}
		phase = Phase.ROAM;
		idle = 0;
		nextBite = now + 10;
	}

	/** On the ground: after its prey, biting, or idling till it climbs back up. */
	private void roam(ServerLevel level, @Nullable LivingEntity target, boolean hunting, long now) {
		if (!hunting) {
			idle++;
			if (idle % 20 == 0 && DripfangRules.reclimbs(idle, false, true)) {
				BlockPos roof = roof(level);
				if (roof != null) {
					phase = Phase.CLIMB;
					hangAt = new Vec3(getX(), roof.getY() - getBbHeight(), getZ());
					getNavigation().stop();
					playSound(SoundEvents.SPIDER_STEP, 0.8F, 0.8F);
				}
			}
			return;
		}
		idle = 0;
		getLookControl().setLookAt(target, 30, 30);
		if (DripfangRules.bites(distanceTo(target), now, nextBite)) {
			nextBite = now + DripfangRules.BITE_COOLDOWN;
			face(target);
			doHurtTarget(level, target);
			playSound(SoundEvents.SPIDER_AMBIENT, 1.0F, 1.4F);
		}
		if (getNavigation().isDone() || tickCount % 10 == 0) {
			getNavigation().moveTo(target, 1.15);
		}
	}

	/** Back up to the roof, slowly. */
	private void climb(ServerLevel level, boolean hunting) {
		getNavigation().stop();
		if (hunting) {
			phase = Phase.ROAM;
			setNoGravity(false);
			return;
		}
		setNoGravity(true);
		setDeltaMovement(0, DripfangRules.CLIMB_SPEED, 0);
		if (tickCount % 6 == 0) {
			playSound(SoundEvents.SPIDER_STEP, 0.4F, 1.2F);
		}
		if (getY() >= hangAt.y - 0.05 || verticalCollision && getDeltaMovement().y <= 0) {
			hangAt = new Vec3(getX(), Math.min(getY(), hangAt.y), getZ());
			phase = Phase.HANG;
			idle = 0;
		}
	}

	private void hang(BlockPos roof) {
		hangAt = new Vec3(getX(), roof.getY() - getBbHeight(), getZ());
		setPos(hangAt);
		setNoGravity(true);
		phase = Phase.HANG;
	}

	/** A roof above it high enough over the floor to hang from, or nothing. */
	private @Nullable BlockPos roof(ServerLevel level) {
		BlockPos at = blockPosition();
		BlockPos roof = null;
		for (int i = 1; i <= DripfangRules.CEILING_REACH + 2; i++) {
			BlockPos p = at.above(i);
			if (solid(level, p)) {
				roof = p;
				break;
			}
			if (!level.getFluidState(p).isEmpty()) {
				return null;
			}
		}
		if (roof == null) {
			return null;
		}
		for (int i = 0; i <= DripfangRules.CEILING_REACH + 2; i++) {
			BlockPos p = roof.below(i + 1);
			if (solid(level, p)) {
				return DripfangRules.hangs(i) ? roof : null;
			}
		}
		return null;
	}

	private static boolean solid(ServerLevel level, BlockPos pos) {
		return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.IS_FALL)) {
			return false;
		}
		boolean flipped = phase == Phase.FLIPPED;
		boolean hit = super.hurtServer(level, source, DripfangRules.hurt(damage, flipped));
		if (hit && (phase == Phase.HANG || phase == Phase.CLIMB)) {
			let(level, true);
		}
		return hit;
	}

	@Override
	public boolean isPushable() {
		return phase != Phase.HANG && super.isPushable();
	}

	// ------------------------------------------------------------------ sounds

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return phase == Phase.ROAM ? SoundEvents.SILVERFISH_AMBIENT : null;
	}

	@Override
	public float getVoicePitch() {
		return 0.5F + random.nextFloat() * 0.1F;
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
