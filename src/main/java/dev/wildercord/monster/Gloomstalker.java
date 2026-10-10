package dev.wildercord.monster;

import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Targets;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Gloomstalker: a lean shadow panther of the dark forests and the deep caves. In the dark it's all but gone, a ripple
 * where the light bends, and only its two violet eyes hang in the gloom. It stalks, circling a few blocks off; then it
 * crouches (its eyes flaring, a low growl), pounces, and slinks away into the dark to try again. A missed pounce leaves it
 * sprawled for a moment.
 *
 * <p>Light undoes it. Within a torch's glow, or in day, it can't hide; up close it shows; a spell of light (fire, storm,
 * arcane or life) lays it bare for six seconds, any wound for two, and glowing (a Star Glyph, a spectral arrow, an arcane
 * familiar) shows it wherever it is. Its eyes are always the tell. Hidden or not, it's drawn by vanilla's own translucent
 * pass, so shader packs show it as they show any spectator's ghost.</p>
 */
public class Gloomstalker extends WildMonster {
	static final int CROUCH = MonsterPressureRules.GLOOM_CROUCH;
	static final float POUNCE_DAMAGE = 6.0F;
	static final int MISSED = MonsterPressureRules.GLOOM_MISSED;
	private static final int VIOLET = 0xB45AF0;

	private enum Phase { STALK, CROUCH, POUNCE, SPRAWL, RETREAT }

	private Phase phase = Phase.STALK;
	private long phaseUntil;
	private long revealedUntil;
	private double circle;
	private Vec3 retreatTo;
	private int pounceTicks;
	private long nextSwipe;
	private long nextPounce;
	/** When each player was last told it was revealed, so it's said once in a while. */
	private final Map<UUID, Long> told = new HashMap<>();

	public Gloomstalker(EntityType<? extends Gloomstalker> type, Level level) {
		super(type, level);
		this.xpReward = 10;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 24.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.ATTACK_DAMAGE, 4.0)
			.add(Attributes.ARMOR, 2.0)
			.add(Attributes.FOLLOW_RANGE, 32.0)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new HuntGoal());
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.BOLT, Runes.BLIND), List.of(Runes.BOLT, Runes.WITHER), List.of(Runes.TOUCH, Runes.HARM));
	}

	// ------------------------------------------------------------------ the veil

	/** Whether it's hidden in the dark right now (both sides). */
	public boolean veiled() {
		return state(VEILED);
	}

	/** Whether it's crouched to pounce (the tell: eyes flaring, a low growl). */
	public boolean crouching() {
		return phase == Phase.CROUCH;
	}

	/** Whether it's in the air, mid-pounce. */
	public boolean pouncing() {
		return phase == Phase.POUNCE;
	}

	/** Lays it bare for {@code ticks}, whatever the light, with a burst of violet and a hiss. */
	public void reveal(ServerLevel level, int ticks, LivingEntity by) {
		boolean wasHidden = veiled();
		revealedUntil = Math.max(revealedUntil, level.getGameTime() + ticks);
		setState(VEILED, false);
		if (wasHidden) {
			Vec3 c = getBoundingBox().getCenter();
			Motes.burst(level, c, 14, VIOLET, 0.18, 22, 0.12);
			ElementFx.ring(level, c, new Vec3(0, 1, 0), VIOLET, 0.2, 1.4, 0.12, 10);
			MonsterMagic.sound(level, c, "monster_gloom_reveal", 1.0F, 1.0F);
			if (by instanceof ServerPlayer player) {
				Long last = told.get(player.getUUID());
				if (last == null || level.getGameTime() - last > 200) {
					told.put(player.getUUID(), level.getGameTime());
					MonsterMagic.tell(player, Component.translatable("message.wildercord.gloomstalker_revealed").withColor(0xE0B0FF));
				}
			}
		}
	}

	private void updateVeil(ServerLevel level, long now) {
		if (now % 4 != 0) {
			return;
		}
		int light = level.getMaxLocalRawBrightness(BlockPos.containing(getEyePosition()));
		boolean glowing = hasEffect(MobEffects.GLOWING) || isCurrentlyGlowing();
		Player near = level.getNearestPlayer(this, 8.0);
		double nearest = near == null || near.isSpectator() ? Double.MAX_VALUE : near.distanceTo(this);
		boolean acting = phase == Phase.POUNCE || phase == Phase.SPRAWL;
		setState(VEILED, MonsterRules.veiled(light, glowing, now < revealedUntil, nearest, acting));
	}

	// ------------------------------------------------------------------ the hunt

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		updateVeil(level, now);
		LivingEntity target = getTarget();
		switch (phase) {
			case CROUCH -> {
				getNavigation().stop();
				if (target == null || !target.isAlive()) {
					resumeStalk(now);
				} else {
					face(target);
					if (now >= phaseUntil) {
						if (Targets.canHarm(this, target) && MonsterPressureRules.gloomCanRelease(distanceTo(target),
							target.getY() - getY(), hasLineOfSight(target))) {
							pounce(level, target, now);
						} else {
							// Cover and a high ledge beat the tell; reacquire by walking, never by teleporting.
							resumeStalk(now);
						}
					}
				}
			}
			case POUNCE -> {
				pounceTicks++;
				if (target != null && target.isAlive() && Targets.canHarm(this, target) && hasLineOfSight(target)
					&& getBoundingBox().inflate(0.35).intersects(target.getBoundingBox())) {
					strike(level, target, now);
				} else if ((onGround() && pounceTicks > 4) || pounceTicks > 30) {
					// It missed: it lands sprawled, and for a moment it's open.
					enter(Phase.SPRAWL, now, MISSED);
					setState(STUNNED, true);
					MonsterMagic.sound(level, position(), "monster_gloom_hurt", 0.7F, 0.8F);
				}
			}
			case SPRAWL -> {
				getNavigation().stop();
				if (now >= phaseUntil) {
					setState(STUNNED, false);
					startRetreat(level, target, now);
				}
			}
			default -> {
			}
		}
		setState(ACTING, phase == Phase.POUNCE);
		setState(WINDUP, phase == Phase.CROUCH);
		setState(ALT, phase == Phase.RETREAT);
	}

	private void enter(Phase next, long now, int ticks) {
		phase = next;
		phaseUntil = now + ticks;
	}

	private void resumeStalk(long now) {
		enter(Phase.STALK, now, 0);
		nextPounce = now + MonsterPressureRules.GLOOM_CANCEL_RETRY;
	}

	private int delay(MonsterPressureRules.Delay interval) {
		return interval.sample(getRandom().nextInt(interval.spread()));
	}

	/** Leaps at where the target is, low and fast. */
	private void pounce(ServerLevel level, LivingEntity target, long now) {
		Vec3 d = target.position().subtract(position());
		double flat = Math.sqrt(d.x * d.x + d.z * d.z);
		double speed = Math.min(1.25, 0.16 * flat + 0.3);
		Vec3 dir = flat < 1.0E-3 ? getLookAngle() : new Vec3(d.x / flat, 0, d.z / flat);
		setDeltaMovement(dir.x * speed, 0.36 + 0.025 * flat + Math.max(0, d.y) * 0.08, dir.z * speed);
		needsSync = true;
		enter(Phase.POUNCE, now, 40);
		pounceTicks = 0;
		Motes.clouds(level, position().add(0, 0.2, 0), 4, 0.4, 0x2A1E36, 0.8, 18, new Vec3(0, 0.01, 0), 0.02, 0.4);
		MonsterMagic.sound(level, position(), "monster_gloom_pounce", 1.0F, 1.0F);
	}

	private void strike(ServerLevel level, LivingEntity target, long now) {
		if (target.hurtServer(level, damageSources().mobAttack(this), POUNCE_DAMAGE)) {
			MonsterMagic.knock(target, position(), 0.6);
			ElementFx.slash(level, target.getBoundingBox().getCenter(), new Vec3(0, 1, 0), getLookAngle(), ElementFx.dark(VIOLET), 0.9, 1.6, 0.12, 4, 10);
		}
		setDeltaMovement(getDeltaMovement().scale(0.2));
		startRetreat(level, target, now);
	}

	/** Slinks away into the darkest spot it can find nearby, to hide and try again. */
	private void startRetreat(ServerLevel level, LivingEntity target, long now) {
		enter(Phase.RETREAT, now, delay(MonsterPressureRules.gloomRetreat(difficulty())));
		retreatTo = null;
		Vec3 away = target != null ? target.position() : position().add(getLookAngle());
		int darkest = Integer.MAX_VALUE;
		for (int i = 0; i < MonsterPressureRules.GLOOM_RETREAT_CANDIDATES; i++) {
			Vec3 to = DefaultRandomPos.getPosAway(this, 14, MonsterPressureRules.GLOOM_RETREAT_HEIGHT, away);
			if (to == null) {
				continue;
			}
			int light = level.getMaxLocalRawBrightness(BlockPos.containing(to));
			if (light < darkest) {
				darkest = light;
				retreatTo = to;
			}
		}
		if (retreatTo != null) {
			getNavigation().moveTo(retreatTo.x, retreatTo.y, retreatTo.z, MonsterPressureRules.GLOOM_RETREAT_SPEED);
		}
	}

	/** Stalks, crouches, pounces, slinks away: the whole hunt is one goal, so nothing else moves it meanwhile. */
	private final class HuntGoal extends Goal {
		private int repath;
		private int stalkTime;

		HuntGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = getTarget();
			return target != null && target.isAlive() && Targets.canHarm(Gloomstalker.this, target);
		}

		@Override
		public boolean canContinueToUse() {
			return canUse() || phase == Phase.POUNCE || phase == Phase.SPRAWL;
		}

		@Override
		public void start() {
			stalkTime = delay(MonsterPressureRules.gloomStalk(difficulty(), false));
			repath = 0;
			circle = getRandom().nextDouble() * Math.PI * 2;
		}

		@Override
		public void stop() {
			if (phase == Phase.CROUCH || phase == Phase.RETREAT) {
				phase = Phase.STALK;
			}
			getNavigation().stop();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity target = getTarget();
			if (target == null || !target.isAlive() || !Targets.canHarm(Gloomstalker.this, target)
				|| !(level() instanceof ServerLevel level)) {
				return;
			}
			long now = level.getGameTime();
			if (phase == Phase.RETREAT) {
				if (now >= phaseUntil || getNavigation().isDone()) {
					phase = Phase.STALK;
					stalkTime = delay(MonsterPressureRules.gloomStalk(difficulty(), true));
				}
				return;
			}
			if (phase != Phase.STALK) {
				return;
			}
			double d = distanceTo(target);
			getLookControl().setLookAt(target, 30, 30);
			// Cornered or caught close: a quick swipe.
			if (d < MonsterPressureRules.GLOOM_SWIPE_RANGE && now >= nextSwipe && hasLineOfSight(target)) {
				nextSwipe = now + MonsterPressureRules.GLOOM_SWIPE_INTERVAL;
				doHurtTarget(level, target);
			}
			boolean sight = hasLineOfSight(target);
			if (--stalkTime <= 0 && now >= nextPounce
				&& MonsterPressureRules.gloomCanCrouch(d, target.getY() - getY(), sight)) {
				enter(Phase.CROUCH, now, CROUCH);
				getNavigation().stop();
				MonsterMagic.sound(level, position(), "monster_gloom_growl", 1.0F, 1.0F);
				return;
			}
			if (--repath > 0) {
				return;
			}
			boolean routed;
			if (MonsterPressureRules.gloomChase(difficulty(), d, target.getY() - getY(), sight)) {
				routed = getNavigation().moveTo(target, MonsterPressureRules.gloomChaseSpeed(difficulty()));
			} else {
				// Circle only while a pounce is a useful next move; do not orbit forever outside its reach.
				circle += MonsterPressureRules.GLOOM_CIRCLE_STEP;
				double r = MonsterPressureRules.GLOOM_CIRCLE_RADIUS;
				Vec3 at = target.position().add(Math.cos(circle) * r, 0, Math.sin(circle) * r);
				routed = getNavigation().moveTo(at.x, at.y, at.z, MonsterPressureRules.GLOOM_CIRCLE_SPEED);
			}
			repath = MonsterPressureRules.repathDelay(routed && getNavigation().getPath() != null && getNavigation().getPath().canReach());
		}
	}

	// ------------------------------------------------------------------ being struck

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt) {
			LivingEntity by = source.getEntity() instanceof LivingEntity l ? l : null;
			boolean light = MonsterMagic.spellLanding() && MonsterRules.revealing(MonsterMagic.spellElement());
			reveal(level, light ? MonsterRules.SPELL_REVEAL : MonsterRules.HURT_REVEAL, by);
			if (phase == Phase.CROUCH) {
				// Struck as it crouches: it breaks off and slinks away.
				startRetreat(level, by, level.getGameTime());
			}
		}
		return hurt;
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
		// It lands on its feet, as cats do.
		return super.causeFallDamage(fallDistance - 4, multiplier, source);
	}

	// ------------------------------------------------------------------ sounds

	@Override
	protected SoundEvent getAmbientSound() {
		return MonsterMagic.kit("monster_gloom_ambient");
	}

	@Override
	protected float getSoundVolume() {
		// A purr in the dark: close enough to hear is close enough to worry.
		return 0.6F;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return MonsterMagic.kit("monster_gloom_hurt");
	}

	@Override
	protected SoundEvent getDeathSound() {
		return MonsterMagic.kit("monster_gloom_death");
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState block) {
		// Soft pads: barely a sound.
		playSound(SoundEvents.WOOL_STEP, 0.08F, 1.4F);
	}

	/** 0.12: Frost where it's freezing, Ash where it's scorched. */
	@Override
	protected boolean hasVariants() {
		return true;
	}

	/** 0.12: Gloomstalkers hunt in packs, some led by an Alpha. */
	@Override
	protected boolean packHunter() {
		return true;
	}
}
