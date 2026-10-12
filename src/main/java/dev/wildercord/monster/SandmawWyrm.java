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
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The Sandmaw Wyrm: a great segmented worm of the deserts and badlands, the first of the biome elites (see
 * {@link EliteTier}), sand-hided and ring-mouthed. It lies under the sand and feels footsteps from far off, though barely
 * a sneak.
 *
 * <p>Under the sand it can't be struck; a furrow of sand trails it as it swims after its prey. Beneath them, the ground
 * rumbles (the tell: step off it) and it bursts up, flinging all about into the air. Up, it rears back and slams its head
 * down along a line before it (step aside), and its head sticks in the sand a moment, wide open, blows landing half again
 * as hard; or, at whoever keeps their distance, it rears with its mouth flared and spits a gout of sand down a cone,
 * blinding and slowing. Up a while, or hurt enough, it dives back under, and the hunt begins again.</p>
 *
 * <p>Its pose flags: {@code VEILED} under the sand, {@code WINDUP} the rumble below or the rearing above, {@code GUARD}
 * bursting up, {@code ACTING} slamming down or spitting, {@code ALT} with the windup or the act, the spit,
 * {@code STUNNED} its head stuck.</p>
 */
public class SandmawWyrm extends EliteMonster {
	private enum Phase { BURROW, RUMBLE, ERUPT, UP, SLAM_WINDUP, SLAM, BURIED, SPIT_WINDUP, SPIT, DIVE }

	private static final int ERUPT_TICKS = 10, SLAM_TICKS = 6, SPIT_TICKS = 8, DIVE_TICKS = 16;

	private Phase phase = Phase.BURROW;
	private long phaseUntil;
	private long nextMove;
	private int down;
	private int up;
	private double hurtSinceRise;
	private Vec3 slamDir = Vec3.ZERO;

	public SandmawWyrm(EntityType<? extends SandmawWyrm> type, Level level) {
		super(type, level, BossEvent.BossBarColor.YELLOW);
		this.xpReward = 30;
		setState(VEILED, true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		EliteTier tier = SandmawRules.TIER;
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, tier.health)
			.add(Attributes.ARMOR, tier.armour)
			.add(Attributes.ARMOR_TOUGHNESS, tier.toughness)
			.add(Attributes.MOVEMENT_SPEED, 0.26)
			.add(Attributes.ATTACK_DAMAGE, tier.hit(6))
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	public EliteTier tier() {
		return SandmawRules.TIER;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.RING, Runes.TREMOR), List.of(Runes.BOLT, Runes.BLIND), List.of(Runes.TOUCH, Runes.PUSH));
	}

	/** Whether it is under the sand right now, out of reach. */
	public boolean burrowed() {
		return phase == Phase.BURROW || phase == Phase.RUMBLE || phase == Phase.DIVE && down > 0;
	}

	// ------------------------------------------------------------------ the hunt

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		LivingEntity target = getTarget();
		boolean hunting = target != null && target.isAlive() && Targets.canHarm(this, target)
			&& distanceTo(target) < SandmawRules.FEEL_RANGE * 2;
		if (!hunting && target != null) {
			setTarget(null);
			target = null;
		}
		switch (phase) {
			case BURROW -> burrow(level, target, now);
			case RUMBLE -> {
				getNavigation().stop();
				setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
				sand(level, 6, 0.8, 0.05);
				if (tickCount % 4 == 0) {
					playSound(groundSound(level), 1.2F, 0.5F);
				}
				if (now >= phaseUntil) {
					erupt(level, now);
				}
			}
			case ERUPT -> {
				getNavigation().stop();
				if (now >= phaseUntil) {
					phase = Phase.UP;
				}
			}
			case UP -> surfaced(level, target, now);
			case SLAM_WINDUP -> {
				getNavigation().stop();
				if (now >= phaseUntil) {
					slam(level, now);
				}
			}
			case SLAM -> {
				getNavigation().stop();
				if (now >= phaseUntil) {
					phase = Phase.BURIED;
					phaseUntil = now + SandmawRules.BURIED_TICKS;
				}
			}
			case BURIED -> {
				getNavigation().stop();
				if (tickCount % 5 == 0) {
					sand(level, 3, 0.5, 0.02);
				}
				if (now >= phaseUntil) {
					phase = Phase.UP;
				}
			}
			case SPIT_WINDUP -> {
				getNavigation().stop();
				if (target != null) {
					face(target);
				}
				if (now >= phaseUntil) {
					spit(level, target, now);
				}
			}
			case SPIT -> {
				getNavigation().stop();
				if (now >= phaseUntil) {
					phase = Phase.UP;
				}
			}
			case DIVE -> {
				getNavigation().stop();
				down++;
				sand(level, 4, 0.6, 0.04);
				if (now >= phaseUntil) {
					phase = Phase.BURROW;
					down = 0;
				}
			}
		}
		if (phase == Phase.UP || phase == Phase.SLAM_WINDUP || phase == Phase.SLAM || phase == Phase.BURIED
			|| phase == Phase.SPIT_WINDUP || phase == Phase.SPIT) {
			up++;
		}
		setState(VEILED, burrowed() || phase == Phase.DIVE);
		setState(WINDUP, phase == Phase.RUMBLE || phase == Phase.SLAM_WINDUP || phase == Phase.SPIT_WINDUP);
		setState(GUARD, phase == Phase.ERUPT);
		setState(ACTING, phase == Phase.SLAM || phase == Phase.SPIT);
		setState(ALT, phase == Phase.SPIT_WINDUP || phase == Phase.SPIT);
		setState(STUNNED, phase == Phase.BURIED);
	}

	/** Under the sand: feeling for footsteps, then swimming after them, a furrow of sand in its wake, to rise beneath. */
	private void burrow(ServerLevel level, @Nullable LivingEntity target, long now) {
		down++;
		if (target == null) {
			getNavigation().stop();
			setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
			if (tickCount % 10 == 0) {
				feel(level);
			}
			if (tickCount % 40 == 0) {
				sand(level, 2, 0.4, 0.02);
			}
			return;
		}
		double dx = target.getX() - getX();
		double dz = target.getZ() - getZ();
		double across = Math.sqrt(dx * dx + dz * dz);
		if (SandmawRules.rises(across, down)) {
			phase = Phase.RUMBLE;
			phaseUntil = now + SandmawRules.RUMBLE_TICKS;
			getNavigation().stop();
			return;
		}
		if (getNavigation().isDone() || tickCount % 8 == 0) {
			getNavigation().moveTo(target, 1.4);
		}
		if (tickCount % 2 == 0) {
			sand(level, 2, 0.3, 0.03);
		}
	}

	/** Whoever walks near enough, unless sneaking, it feels and hunts. */
	private void feel(ServerLevel level) {
		Player nearest = null;
		double best = Double.MAX_VALUE;
		for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(SandmawRules.FEEL_RANGE),
			p -> p.isAlive() && p.onGround() && Targets.canHarm(this, p))) {
			double distance = distanceTo(player);
			if (distance < best && SandmawRules.feels(distance, player.isShiftKeyDown())) {
				nearest = player;
				best = distance;
			}
		}
		if (nearest != null) {
			setTarget(nearest);
			down = 0;
		}
	}

	/** Bursting up out of the sand: all about it struck and flung. */
	private void erupt(ServerLevel level, long now) {
		phase = Phase.ERUPT;
		phaseUntil = now + ERUPT_TICKS;
		up = 0;
		hurtSinceRise = 0;
		nextMove = now + SandmawRules.MOVE_COOLDOWN;
		sand(level, 40, 1.2, 0.25);
		playSound(SoundEvents.RAVAGER_ROAR, 1.6F, 0.6F);
		playSound(groundSound(level), 1.6F, 0.6F);
		float damage = tier().hit(SandmawRules.ERUPT_DAMAGE);
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SandmawRules.ERUPT_RADIUS, 1, SandmawRules.ERUPT_RADIUS),
			e -> e != this && e.isAlive() && Targets.canHarm(this, e))) {
			Vec3 to = victim.position().subtract(position());
			if (to.horizontalDistance() > SandmawRules.ERUPT_RADIUS + victim.getBbWidth() / 2 + getBbWidth() / 2) {
				continue;
			}
			if (victim.hurtServer(level, damageSources().mobAttack(this), damage)) {
				Vec3 out = to.horizontalDistanceSqr() < 1e-4 ? Vec3.ZERO : new Vec3(to.x, 0, to.z).normalize().scale(0.4);
				victim.setDeltaMovement(victim.getDeltaMovement().add(out.x, SandmawRules.ERUPT_LAUNCH, out.z));
				victim.needsSync = true;
			}
		}
	}

	/** Up: slamming whoever is near, spitting at whoever keeps off, diving after whoever has fled, or once up long enough. */
	private void surfaced(ServerLevel level, @Nullable LivingEntity target, long now) {
		getNavigation().stop();
		setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
		if (target == null) {
			if (up > 60) {
				dive(now);
			}
			return;
		}
		getLookControl().setLookAt(target, 30, 30);
		if (SandmawRules.dives(up, hurtSinceRise, getMaxHealth())) {
			dive(now);
			return;
		}
		switch (SandmawRules.choose(distanceTo(target), now, nextMove)) {
			case SLAM -> {
				face(target);
				Vec3 look = getLookAngle();
				slamDir = new Vec3(look.x, 0, look.z).normalize();
				phase = Phase.SLAM_WINDUP;
				phaseUntil = now + SandmawRules.SLAM_WINDUP;
				playSound(SoundEvents.RAVAGER_AMBIENT, 1.4F, 0.5F);
			}
			case SPIT -> {
				face(target);
				phase = Phase.SPIT_WINDUP;
				phaseUntil = now + SandmawRules.SPIT_WINDUP;
				playSound(SoundEvents.RAVAGER_AMBIENT, 1.2F, 0.8F);
			}
			case DIVE -> dive(now);
			case NONE -> { }
		}
	}

	/** Its head slammed down along the line before it, sticking in the sand. */
	private void slam(ServerLevel level, long now) {
		phase = Phase.SLAM;
		phaseUntil = now + SLAM_TICKS;
		nextMove = now + SLAM_TICKS + SandmawRules.BURIED_TICKS + SandmawRules.MOVE_COOLDOWN;
		playSound(SoundEvents.RAVAGER_ATTACK, 1.6F, 0.6F);
		playSound(groundSound(level), 1.6F, 0.7F);
		Vec3 side = new Vec3(-slamDir.z, 0, slamDir.x);
		BlockParticleOption dust = new BlockParticleOption(ParticleTypes.BLOCK, ground(level));
		for (double s = 1; s <= SandmawRules.SLAM_LENGTH; s += 0.75) {
			Vec3 p = position().add(slamDir.scale(s));
			level.sendParticles(dust, p.x, p.y + 0.2, p.z, 5, 0.4, 0.1, 0.4, 0.15);
		}
		float damage = tier().hit(SandmawRules.SLAM_DAMAGE);
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SandmawRules.SLAM_LENGTH, 2, SandmawRules.SLAM_LENGTH),
			e -> e != this && e.isAlive() && Targets.canHarm(this, e))) {
			Vec3 to = victim.position().subtract(position());
			if (SandmawRules.inLine(to.dot(slamDir), to.dot(side)) && Math.abs(to.y) < 2.5) {
				victim.hurtServer(level, damageSources().mobAttack(this), damage);
			}
		}
	}

	/** A gout of sand spat down a cone before it: stinging, blinding and slowing. */
	private void spit(ServerLevel level, @Nullable LivingEntity target, long now) {
		phase = Phase.SPIT;
		phaseUntil = now + SPIT_TICKS;
		nextMove = now + SPIT_TICKS + SandmawRules.MOVE_COOLDOWN;
		playSound(SoundEvents.LLAMA_SPIT, 1.6F, 0.5F);
		Vec3 look = getLookAngle();
		Vec3 mouth = position().add(0, getBbHeight() * 0.85, 0);
		BlockParticleOption dust = new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState());
		for (double s = 1; s <= SandmawRules.SPIT_RANGE; s += 1) {
			Vec3 p = mouth.add(look.scale(s));
			double spread = 0.15 + s * 0.08;
			level.sendParticles(dust, p.x, p.y, p.z, 6, spread, spread, spread, 0.02);
		}
		float damage = tier().hit(SandmawRules.SPIT_DAMAGE);
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SandmawRules.SPIT_RANGE),
			e -> e != this && e.isAlive() && Targets.canHarm(this, e))) {
			Vec3 to = victim.getEyePosition().subtract(mouth);
			double distance = to.length();
			double dot = distance < 1e-4 ? 1 : to.normalize().dot(look);
			if (SandmawRules.inSpit(distance, dot) && hasLineOfSight(victim)
				&& victim.hurtServer(level, damageSources().mobAttack(this), damage)) {
				victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), this);
				victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), this);
			}
		}
	}

	/** Back under the sand. */
	private void dive(long now) {
		phase = Phase.DIVE;
		phaseUntil = now + DIVE_TICKS;
		down = 0;
		up = 0;
		playSound(SoundEvents.RAVAGER_STEP, 1.4F, 0.5F);
	}

	// ------------------------------------------------------------------ sand

	private BlockState ground(ServerLevel level) {
		BlockState state = level.getBlockState(blockPosition().below());
		return state.isAir() ? Blocks.SAND.defaultBlockState() : state;
	}

	private SoundEvent groundSound(ServerLevel level) {
		return ground(level).getSoundType().getBreakSound();
	}

	private void sand(ServerLevel level, int count, double spread, double speed) {
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground(level)), getX(), getY() + 0.1, getZ(), count, spread, 0.1, spread, speed);
	}

	// ------------------------------------------------------------------ out of reach under the sand

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean buried = phase == Phase.BURIED;
		float dealt = SandmawRules.hurt(damage, burrowed(), buried);
		if (dealt <= 0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return false;
		}
		float before = getHealth();
		boolean hit = super.hurtServer(level, source, dealt > 0 ? dealt : damage);
		if (hit) {
			hurtSinceRise += Math.max(0, before - getHealth());
			if (getTarget() == null && source.getEntity() instanceof LivingEntity attacker && Targets.canHarm(this, attacker)) {
				setTarget(attacker);
			}
		}
		return hit;
	}

	@Override
	public boolean isPickable() {
		return !burrowed() && super.isPickable();
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return false;
	}

	// ------------------------------------------------------------------ sounds

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return burrowed() ? null : SoundEvents.RAVAGER_AMBIENT;
	}

	@Override
	public float getVoicePitch() {
		return 0.5F + random.nextFloat() * 0.1F;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.RAVAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.RAVAGER_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState block) {
	}
}
