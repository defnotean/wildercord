package dev.wildercord.cast;

import dev.wildercord.content.dungeons.DungeonSounds;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Cinder Warden, keeper of the Ember Sanctum: a hulking figure of magma plates and chains. Its
 * armour turns every plain blow and every single-element spell (a tenth of it gets through); only an
 * element reaction set off on it (any of them: Shatter, Conduct, Overload, Fracture...) cracks it open
 * for full damage, and leaves it open for a moment after. It teaches reactions by using them: its own
 * spells set them up on you (freeze, then burn; throw, then burn; pull, then blast), and its name
 * on the boss bar says which. Up close it brings a fist down in a ring of fire.
 */
public class CinderWarden extends DungeonBoss {
	private static final int COLOR = 0xFF6A1E;
	private static final int ACCENT = 0xFFC040;
	/** How long a reaction leaves its armour cracked open (full damage from any spell). */
	private static final int CRACK_TICKS = 30;
	private static final int SLAM_WINDUP = 16;
	private static final double SLAM_RADIUS = 3.8;

	private static final List<List<RuneDef>> PHASE_1 = List.of(
		List.of(Runes.BOLT, Runes.FROST, Runes.DELAY, Runes.BOLT, Runes.FIRE),
		List.of(Runes.PILLAR, Runes.FIRE),
		List.of(Runes.WAVE, Runes.EMBER));
	private static final List<List<RuneDef>> PHASE_2 = List.of(
		List.of(Runes.BOLT, Runes.DASH, Runes.DELAY, Runes.COMET, Runes.FIRE),
		List.of(Runes.RING, Runes.FROST, Runes.DELAY, Runes.RING, Runes.FIRE),
		List.of(Runes.RAIN, Runes.FIRE),
		List.of(Runes.CONE, Runes.FLASHFIRE));
	private static final List<List<RuneDef>> PHASE_3 = List.of(
		List.of(Runes.BOLT, Runes.PULL, Runes.DELAY, Runes.COMET, Runes.EXPLODE),
		List.of(Runes.BOLT, Runes.FROST, Runes.DELAY, Runes.BOLT, Runes.FIRE, Runes.AMPLIFY),
		List.of(Runes.ZONE, Runes.INFERNO),
		List.of(Runes.WAVE, Runes.FROST, Runes.DELAY, Runes.WAVE, Runes.FIRE));

	private long crackedUntil;
	private long slamAt;
	private long nextSlam;
	/** When each player was last told why their blow did nothing, so it's said once in a while, not every hit. */
	private final Map<UUID, Long> told = new HashMap<>();

	public CinderWarden(EntityType<? extends CinderWarden> type, Level level) {
		super(type, level, BossEvent.BossBarColor.RED);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 300.0)
			.add(Attributes.MOVEMENT_SPEED, 0.23)
			.add(Attributes.FOLLOW_RANGE, 40.0)
			.add(Attributes.ARMOR, 4.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.ATTACK_DAMAGE, 10.0);
	}

	/** Wakes the Warden at its forge-altar: the altar spits fire and it climbs out of the coals. */
	public static CinderWarden rise(ServerLevel level, BlockPos altar) {
		CinderWarden boss = place(level, DungeonEntities.CINDER_WARDEN, altar, 1.0);
		if (boss == null) {
			return null;
		}
		Vec3 at = Vec3.atBottomCenterOf(altar).add(0, 1, 0);
		Sigils.ground(level, at, COLOR, ACCENT, 5.0F, 80);
		ElementFx.flameBurst(level, at.add(0, 1.2, 0), 2.5, 16);
		ElementFx.heatFlare(level, at.add(0, 1.5, 0), 2.5);
		Vfx.emit(level, ParticleTypes.LAVA, at.add(0, 1, 0), 30, 1.2, 0.3);
		Motes.clouds(level, at.add(0, 2, 0), 10, 1.0, Motes.SMOKE, 2.0, 50, new Vec3(0, 0.04, 0), 0.04, 0.45);
		Fx.sound(level, at, DungeonSounds.BOSS_RISE, 2.0F, 1.0F);
		Fx.sound(level, at, SoundEvents.BLAZE_SHOOT, 1.2F, 0.5F);
		boss.announce(level, "message.wildercord.cinder_warden_wakes", COLOR);
		return boss;
	}

	@Override
	protected List<List<RuneDef>> spells(int phase) {
		return phase == 1 ? PHASE_1 : phase == 2 ? PHASE_2 : PHASE_3;
	}

	@Override
	protected String feat() {
		return Feats.CINDER_WARDEN;
	}

	@Override
	protected int color() {
		return COLOR;
	}

	@Override
	protected int accent() {
		return ACCENT;
	}

	// ------------------------------------------------------------------ moving and striking

	@Override
	protected void approach(ServerLevel level, LivingEntity target) {
		if (slamAt > 0) {
			getNavigation().stop();
		} else if (distanceTo(target) > 3.0) {
			getNavigation().moveTo(target, 1.0);
		} else {
			getNavigation().stop();
			getLookControl().setLookAt(target, 30, 30);
		}
	}

	/**
	 * Lava and fire are home: nothing sets it alight, not even for the rest of a tick, so no spell's fire
	 * on it can feed Overload, Elapse or Unweave (it isn't fire immune, or fire spells would read as immune).
	 */
	@Override
	public void setRemainingFireTicks(int ticks) {
		super.setRemainingFireTicks(Math.min(0, ticks));
	}

	@Override
	protected boolean mayCast(ServerLevel level, LivingEntity target) {
		return slamAt == 0;
	}

	@Override
	protected void mechanic(ServerLevel level, long now) {
		// Lava and fire are home: it never burns.
		clearFire();
		setState(EXPOSED, now < crackedUntil);
		if (now % 8 == 0) {
			Vec3 c = position().add(0, getBbHeight() * 0.6, 0);
			Vfx.emit(level, ParticleTypes.FALLING_LAVA, c, 1, 0.5, 0.0);
			Vfx.emit(level, ParticleTypes.SMALL_FLAME, c, 2, 0.5, 0.01);
		}
		LivingEntity target = getTarget();
		// Its fist: a ring of fire on the floor around it, then the blow.
		if (slamAt > 0) {
			if (now >= slamAt) {
				slamAt = 0;
				setState(SLAMMING, false);
				slam(level);
			}
			return;
		}
		if (castAt == 0 && target != null && target.isAlive() && now >= nextSlam && distanceTo(target) < 4.5) {
			slamAt = now + SLAM_WINDUP;
			nextSlam = now + 70;
			setState(SLAMMING, true);
			getNavigation().stop();
			Sigils.ground(level, position().add(0, 0.06, 0), COLOR, ACCENT, (float) SLAM_RADIUS, SLAM_WINDUP + 6);
			Fx.sound(level, position(), SoundEvents.RAVAGER_ROAR, 0.6F, 1.4F);
		}
	}

	private void slam(ServerLevel level) {
		Vec3 feet = position();
		double power = Runebound.power(level);
		DamageSource source = level.damageSources().mobAttack(this);
		for (Entity e : level.getEntities(this, new AABB(feet, feet).inflate(SLAM_RADIUS, 2.5, SLAM_RADIUS), e -> Targets.canHarm(this, e))) {
			LivingEntity t = (LivingEntity) e;
			if (t.position().distanceTo(feet) > SLAM_RADIUS + 0.5) {
				continue;
			}
			t.hurtServer(level, source, (float) (12 * power));
			t.igniteForSeconds(3);
			Vec3 away = t.position().subtract(feet);
			Vec3 push = new Vec3(away.x, 0, away.z).lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : new Vec3(away.x, 0, away.z).normalize();
			Effects.push(t, new Vec3(push.x * 1.1, 0.45, push.z * 1.1));
		}
		ElementFx.flameBurst(level, feet.add(0, 0.4, 0), SLAM_RADIUS, 18);
		ElementFx.groundRing(level, feet.add(0, 0.05, 0), COLOR, 0.5, SLAM_RADIUS + 0.8, 0.25, 10);
		ElementFx.crack(level, feet, SLAM_RADIUS, 40);
		Vfx.emit(level, ParticleTypes.LAVA, feet.add(0, 0.3, 0), 16, 1.5, 0.3);
		Fx.sound(level, feet, DungeonSounds.WARDEN_SLAM, 1.6F, 1.0F);
	}

	// ------------------------------------------------------------------ its armour: only reactions break through

	@Override
	protected float resist(ServerLevel level, DamageSource source, float damage) {
		long now = level.getGameTime();
		if (Dungeons.spellLanding()) {
			if (Reactions.reactedWithin(this, 1)) {
				// A reaction set off on it: its plates crack open, and stay open a moment.
				crackedUntil = now + CRACK_TICKS;
				setState(EXPOSED, true);
				crackFx(level);
				return damage;
			}
			if (now < crackedUntil) {
				return damage;
			}
			glance(level, source, now);
			return damage * 0.1F;
		}
		// Fire, lava and burning are nothing to it; anything else is turned by its armour.
		if (!source.is(DamageTypeTags.IS_FIRE)) {
			glance(level, source, now);
		}
		return 0;
	}

	/** A blow or a spell turned by its armour: a dead clang, sparks, and (now and then) a word of why. */
	private void glance(ServerLevel level, DamageSource source, long now) {
		Vec3 c = position().add(0, getBbHeight() * 0.6, 0);
		Vfx.emit(level, ParticleTypes.CRIT, c, 8, 0.6, 0.2);
		Vfx.emit(level, ParticleTypes.SMOKE, c, 4, 0.5, 0.02);
		Fx.sound(level, c, DungeonSounds.WARDEN_IMMUNE, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
		if (source.getEntity() instanceof ServerPlayer player) {
			Long last = told.get(player.getUUID());
			if (last == null || now - last > 200) {
				told.put(player.getUUID(), now);
				player.sendOverlayMessage(Component.translatable("message.wildercord.cinder_warden_immune").withColor(ACCENT));
			}
		}
	}

	/** Cracked open: magma bursts from between its plates. */
	private void crackFx(ServerLevel level) {
		Vec3 c = position().add(0, getBbHeight() * 0.55, 0);
		Sigils.flash(level, c, ACCENT, 2.6F);
		ElementFx.fireImpact(level, c, 1.4);
		Vfx.emit(level, ParticleTypes.LAVA, c, 14, 0.7, 0.3);
		Vfx.radial(level, new DustParticleOptions(0xFFB040, 1.3F), c, 20, 0.35);
		Fx.sound(level, c, DungeonSounds.WARDEN_HURT, 1.2F, 1.0F);
		Fx.sound(level, c, SoundEvents.ANVIL_LAND, 0.5F, 0.6F);
	}

	// ------------------------------------------------------------------ between phases: stoking its forge

	@Override
	protected void onPhase(ServerLevel level, int phase) {
		Vec3 c = position();
		Sigils.ground(level, c.add(0, 0.05, 0), COLOR, ACCENT, 6.0F, SHIFT_TICKS + 10);
		Fx.sound(level, c, DungeonSounds.BOSS_PHASE, 2.0F, 1.0F);
		announce(level, "message.wildercord.cinder_warden_phase." + phase, COLOR);
		// Two of its keepers climb out of the forge: Runebound, carrying fire (Adepts the second time).
		for (int i = 0; i < 2; i++) {
			Mob guard = EntityTypes.WITHER_SKELETON.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (guard == null) {
				continue;
			}
			double a = level.getRandom().nextDouble() * Math.PI * 2;
			Vec3 spot = CastEngine.ground(level, c.add(Math.cos(a) * 6, 2, Math.sin(a) * 6));
			guard.snapTo(spot.x, spot.y, spot.z, 0, 0);
			guard.finalizeSpawn(level, level.getCurrentDifficultyAt(guard.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
			Runebound.bind(guard, i == 0 ? List.of(Runes.BOLT, Runes.FIRE) : List.of(Runes.ARC, Runes.FROST), phase == 3);
			level.addFreshEntity(guard);
			minion(guard);
			ElementFx.flameBurst(level, spot.add(0, 1, 0), 1.2, 8);
			Sigils.ground(level, spot, COLOR, ACCENT, 1.2F, 30);
		}
	}

	@Override
	protected void shiftTick(ServerLevel level, int left) {
		Vec3 c = position();
		// Fire climbs its chains, then it brings both fists down and the heat throws everyone back.
		if (left % 3 == 0) {
			ElementFx.flames(level, c, 1.0, getBbHeight(), 3);
			Vfx.emit(level, ParticleTypes.LAVA, c.add(0, 1.5, 0), 3, 0.6, 0.2);
		}
		if (left == 20) {
			setState(SLAMMING, true);
		}
		if (left == 4) {
			setState(SLAMMING, false);
			double power = Runebound.power(level);
			for (Entity e : level.getEntities(this, getBoundingBox().inflate(8.0), e -> Targets.canHarm(this, e))) {
				LivingEntity t = (LivingEntity) e;
				Vec3 away = t.position().subtract(c);
				Vec3 push = new Vec3(away.x, 0, away.z).lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : new Vec3(away.x, 0, away.z).normalize();
				t.hurtServer(level, level.damageSources().mobAttack(this), (float) (5 * power));
				Effects.push(t, new Vec3(push.x * 1.6, 0.5, push.z * 1.6));
			}
			ElementFx.groundRing(level, c.add(0, 0.05, 0), COLOR, 0.5, 8.5, 0.3, 14);
			ElementFx.flameBurst(level, c.add(0, 0.5, 0), 4.0, 24);
			Fx.sound(level, c, DungeonSounds.WARDEN_SLAM, 2.0F, 0.7F);
		}
	}

	@Override
	protected Component status() {
		return state(EXPOSED) ? Component.translatable("boss.wildercord.cinder_warden_cracked").withColor(ACCENT) : null;
	}

	// ------------------------------------------------------------------ death

	@Override
	protected void dying(ServerLevel level, int tick) {
		Vec3 c = position().add(0, getBbHeight() * 0.5 * (1 - tick / (float) DEATH_TICKS), 0);
		if (tick % 2 == 0) {
			Motes.smoke(level, c, 2, 0.6);
			Vfx.emit(level, ParticleTypes.LAVA, c, tick < DEATH_TICKS / 2 ? 2 : 0, 0.5, 0.2);
		}
		if (tick == 1) {
			Fx.sound(level, c, DungeonSounds.WARDEN_DEATH, 2.0F, 1.0F);
		}
		if (tick % 10 == 0) {
			Fx.sound(level, c, SoundEvents.LAVA_EXTINGUISH, 0.8F, 0.6F + tick / 100F);
		}
	}

	@Override
	protected void vanish(ServerLevel level) {
		Vec3 c = position().add(0, 0.6, 0);
		Motes.clouds(level, c, 14, 0.7, 0x5A5250, 1.6, 45, new Vec3(0, 0.035, 0), 0.1, 0.5);
		Motes.burst(level, c, 16, 0xFF6A2A, 0.14, 24, 0.25);
		ElementFx.embers(level, c, 1.5, 30);
	}

	// ------------------------------------------------------------------ sounds

	@Override
	protected SoundEvent getAmbientSound() {
		return DungeonSounds.WARDEN_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return DungeonSounds.WARDEN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return null;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 140;
	}

	/** For the tests: whether a reaction has its armour cracked open right now. */
	public boolean cracked() {
		return level().getGameTime() < crackedUntil;
	}

	/** Its casting cycle, for the tests: the spells of each phase, in order. */
	public static List<List<RuneDef>> cycle(int phase) {
		return phase == 1 ? PHASE_1 : phase == 2 ? PHASE_2 : PHASE_3;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		return false;
	}
}
