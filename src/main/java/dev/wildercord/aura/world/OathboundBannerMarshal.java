package dev.wildercord.aura.world;

import dev.wildercord.cast.Fx;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.MoveDeck;
import dev.wildercord.cast.Sigils;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.compat.McEntities;
import dev.wildercord.content.SigilOption;
import dev.wildercord.content.WildercordSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import dev.wildercord.world.sites.oathhold.OathholdLaw;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Oathbound Banner-Marshal — Knight #4 (Way of the Sovereign Banner), an ivory-white and burnished-gold commander
 * wearing an emerald surcoat, golden laurel wreath, winged pauldrons, glowing chest Sun-Core, Sovereign War-Banner,
 * Sovereign Sunblade, and octagonal Solar Aegis Shield.
 *
 * <p>All timings, command aura radius, shield arc, and judgment pillar falloff live in {@link OathboundBannerMarshalRules}.</p>
 */
public final class OathboundBannerMarshal extends PathfinderMob implements Enemy {
	private static final EntityDataAccessor<Integer> MOVE = SynchedEntityData.defineId(OathboundBannerMarshal.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN = SynchedEntityData.defineId(OathboundBannerMarshal.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Integer> TELL = SynchedEntityData.defineId(OathboundBannerMarshal.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> GUARD_COUNTERED = SynchedEntityData.defineId(OathboundBannerMarshal.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Long> GUARD_COUNTER_BEGAN = SynchedEntityData.defineId(OathboundBannerMarshal.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Boolean> EMPOWERED = SynchedEntityData.defineId(OathboundBannerMarshal.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Float> PREVIEW_ELAPSED = SynchedEntityData.defineId(OathboundBannerMarshal.class, EntityDataSerializers.FLOAT);

	private static final int HEX_SOLAR = 0xFFE066;
	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** Client-side PoseBlend interpolation state. */
	public int shownMove = -1, prevMove = -1, shownTell, prevTell;
	public float shownElapsed, prevElapsed, switchedAt = -100.0F;

	private final MoveDeck deck = new MoveDeck(OathboundBannerMarshalRules.MEMORY);
	private final long[] lastUsed = new long[32];
	private final Set<UUID> struck = new HashSet<>();

	private float aim;
	private int moveTicksLeft;
	private int gapTicks = 10;
	private boolean decreeFired;
	private boolean cleaveHitFired;
	private boolean judgmentHitFired;
	private boolean counterHitFired;
	private boolean techHitFired;
	private int techHitCount;

	{
		Arrays.fill(lastUsed, -9999L);
	}

	public OathboundBannerMarshal(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		xpReward = OathboundBannerMarshalRules.XP_REWARD;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, OathboundBannerMarshalRules.MAX_HEALTH)
			.add(Attributes.ARMOR, OathboundBannerMarshalRules.ARMOR)
			.add(Attributes.ARMOR_TOUGHNESS, OathboundBannerMarshalRules.ARMOR_TOUGHNESS)
			.add(Attributes.KNOCKBACK_RESISTANCE, OathboundBannerMarshalRules.KNOCKBACK_RESISTANCE)
			.add(Attributes.MOVEMENT_SPEED, OathboundBannerMarshalRules.MOVEMENT_SPEED)
			.add(Attributes.ATTACK_DAMAGE, OathboundBannerMarshalRules.CLEAVE_DAMAGE)
			.add(Attributes.FOLLOW_RANGE, OathboundBannerMarshalRules.FOLLOW_RANGE);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(MOVE, OathboundBannerMarshalRules.SOVEREIGN_MARCH);
		builder.define(BEGAN, 0L);
		builder.define(TELL, 0);
		builder.define(GUARD_COUNTERED, false);
		builder.define(GUARD_COUNTER_BEGAN, 0L);
		builder.define(EMPOWERED, false);
		builder.define(PREVIEW_ELAPSED, -1.0F);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.72));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new OathholdLaw.LawEnforcementTargetGoal(this));
		targetSelector.addGoal(3, new OathholdLaw.CityMonsterDefenseGoal(this));
	}

	public int move() {
		return entityData.get(MOVE);
	}

	public int tell() {
		return entityData.get(TELL);
	}

	public boolean guardCountered() {
		return entityData.get(GUARD_COUNTERED);
	}

	public float guardCounterElapsed(float partialTick) {
		if (!guardCountered()) {
			return 0.0F;
		}
		return Math.max(0.0F, (float) (level().getGameTime() - entityData.get(GUARD_COUNTER_BEGAN)) + partialTick);
	}

	public boolean empowered() {
		return entityData.get(EMPOWERED);
	}

	public float elapsed(float partialTick) {
		float preview = entityData.get(PREVIEW_ELAPSED);
		if (preview >= 0.0F) {
			return preview;
		}
		return (float) (level().getGameTime() - entityData.get(BEGAN)) + partialTick;
	}

	private long since() {
		return Math.max(0L, level().getGameTime() - entityData.get(BEGAN));
	}

	/** Holds an exact animation keyframe pose for studio/gametest screenshots while {@code NoAI} is enabled. */
	public void poseForPreview(int move, int tellTicks, float fixedElapsed, boolean guardCounter) {
		entityData.set(MOVE, move);
		entityData.set(TELL, tellTicks);
		entityData.set(BEGAN, level().getGameTime() - (long) fixedElapsed);
		entityData.set(PREVIEW_ELAPSED, fixedElapsed);
		entityData.set(GUARD_COUNTERED, guardCounter);
		entityData.set(GUARD_COUNTER_BEGAN, level().getGameTime());
		moveTicksLeft = Math.max(0, OathboundBannerMarshalRules.moveDuration(move, empowered()) - (int) fixedElapsed);
		aim = getYRot();
	}

	/** Starts a live combat move from AI or test harnesses. */
	public void startMove(ServerLevel level, int nextMove, LivingEntity target) {
		boolean emp = empowered();
		int tell = OathboundBannerMarshalRules.tellTicks(nextMove, emp);
		int duration = nextMove == OathboundBannerMarshalRules.STAGGERED
			? OathboundBannerMarshalRules.STAGGER_AXE_TICKS
			: OathboundBannerMarshalRules.moveDuration(nextMove, emp);
		startMoveWithDuration(level, nextMove, tell, duration, target);
	}

	private void startMoveWithDuration(ServerLevel level, int nextMove, int tell, int duration, LivingEntity target) {
		entityData.set(PREVIEW_ELAPSED, -1.0F);
		entityData.set(MOVE, nextMove);
		entityData.set(TELL, tell);
		entityData.set(BEGAN, level.getGameTime());
		entityData.set(GUARD_COUNTERED, false);
		entityData.set(GUARD_COUNTER_BEGAN, 0L);
		moveTicksLeft = duration;
		decreeFired = false;
		cleaveHitFired = false;
		judgmentHitFired = false;
		counterHitFired = false;
		techHitFired = false;
		techHitCount = 0;
		struck.clear();
		getNavigation().stop();

		if (target != null) {
			aim = yawTo(target.position());
			face(aim);
		} else {
			aim = getYRot();
		}

		if (nextMove != OathboundBannerMarshalRules.SOVEREIGN_MARCH && nextMove != OathboundBannerMarshalRules.STAGGERED) {
			lastUsed[nextMove] = level.getGameTime();
			deck.played(nextMove);
		}

		Vec3 chest = chestPos();
		switch (nextMove) {
			case OathboundBannerMarshalRules.DECREE_OF_THE_STANDARD -> {
				Feels.sound(level, chest, "aura_bell_warden_shell", 1.25F, 1.15F);
				Sigils.layer(level, position().add(0, 0.08, 0), UP, SigilOption.RING, HEX_SOLAR, 3.8F, tell + 12, 1);
			}
			case OathboundBannerMarshalRules.SOLAR_AEGIS_GUARD -> {
				Feels.sound(level, chest, "aura_guard", 1.15F, 0.95F);
				sound(level, chest, SoundEvents.SHIELD_BLOCK.value(), 1.0F, 0.85F);
			}
			case OathboundBannerMarshalRules.RADIANT_SUNBLADE_CLEAVE ->
				Feels.sound(level, chest, "duelist_knight_windup", 1.1F, 1.05F);
			case OathboundBannerMarshalRules.SOVEREIGN_JUDGMENT -> {
				Feels.sound(level, chest, "duelist_knight_windup", 1.3F, 0.88F);
				Light.groundRing(level, position(), HEX_SOLAR, 0.5, OathboundBannerMarshalRules.JUDGMENT_OUTER_RADIUS, 0.10, tell);
			}
			case OathboundBannerMarshalRules.STAGGERED ->
				sound(level, chest, SoundEvents.SHIELD_BREAK.value(), 1.3F, 0.85F);
			default -> {
				OathboundTechnique tech = OathboundBannerMarshalRules.technique(nextMove);
				if (tech != null) {
					Feels.sound(level, chest, "aura_bell_warden_shell", 1.1F, 1.1F);
				}
			}
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);

		if (!empowered() && OathboundBannerMarshalRules.empowered(getHealth(), getMaxHealth())) {
			entityData.set(EMPOWERED, true);
			Feels.sound(level, chestPos(), "duelist_clash", 1.3F, 0.95F);
			Light.groundRing(level, position(), HEX_SOLAR, 0.5, 4.8, 0.14, 18);
			Sigils.flash(level, chestPos(), 0xFFFFE066, 2.2F);
		}

		LivingEntity target = getTarget();
		if (target != null && !target.isAlive()) {
			setTarget(null);
			target = null;
		}

		int move = move();
		if (move != OathboundBannerMarshalRules.SOVEREIGN_MARCH) {
			tickActiveMove(level, move, target);
			return;
		}

		if (gapTicks > 0) {
			gapTicks--;
		}
		if (target == null) {
			return;
		}

		double dist = distanceTo(target);
		if (dist > 3.2) {
			getNavigation().moveTo(target, 1.04);
			if (tickCount % 12 == 0 && onGround()) {
				Feels.sound(level, position(), "duelist_knight_step", 0.80F, 0.86F + random.nextFloat() * 0.1F);
			}
		} else {
			getNavigation().stop();
		}
		turnToward(yawTo(target.position()), 12.0F);

		if (gapTicks <= 0 && dist <= 9.0) {
			int next = chooseMove(level, dist);
			if (next > 0) {
				startMove(level, next, target);
			}
		}
	}

	private int chooseMove(ServerLevel level, double dist) {
		long now = level.getGameTime();
		boolean emp = empowered();
		int[] moves = OathboundBannerMarshalRules.COMBAT_MOVES;
		double[] weights = new double[moves.length];
		boolean woundedOrAllies = getHealth() < getMaxHealth() * 0.85F || !nearbyAlliedKnights(level).isEmpty();

		for (int i = 0; i < moves.length; i++) {
			int m = moves[i];
			if (now - lastUsed[m] < OathboundBannerMarshalRules.moveCooldown(m, emp)) {
				continue;
			}
			weights[i] = switch (m) {
				case OathboundBannerMarshalRules.DECREE_OF_THE_STANDARD -> woundedOrAllies ? 1.4 : 0.45;
				case OathboundBannerMarshalRules.SOLAR_AEGIS_GUARD -> dist <= 4.5 ? 1.55 : 0.4;
				case OathboundBannerMarshalRules.RADIANT_SUNBLADE_CLEAVE -> dist <= 4.3 ? 2.1 : 0.0;
				case OathboundBannerMarshalRules.SOVEREIGN_JUDGMENT -> dist <= 5.8 ? (emp ? 1.85 : 1.35) : 0.0;
				default -> {
					OathboundTechnique tech = OathboundBannerMarshalRules.technique(m);
					if (tech == null || (tech.empoweredOnly() && !emp) || !tech.inRange(dist)) {
						yield 0.0;
					}
					yield 1.4 + random.nextDouble() * 0.5;
				}
			};
		}
		return deck.pick(moves, weights, random.nextDouble());
	}

	private List<LivingEntity> nearbyAlliedKnights(ServerLevel level) {
		AABB box = new AABB(position(), position()).inflate(OathboundBannerMarshalRules.DECREE_RADIUS);
		return level.getEntitiesOfClass(LivingEntity.class, box, e ->
			e.isAlive()
				&& (e == this
					|| e instanceof FallenKnight
					|| e instanceof OathboundBastion
					|| e instanceof OathboundBladeChampion
					|| e instanceof OathboundHalberdier
					|| e instanceof OathboundBannerMarshal)
				&& OathboundBannerMarshalRules.inDecreeAura(e.getX() - getX(), e.getZ() - getZ()));
	}

	private void tickActiveMove(ServerLevel level, int move, LivingEntity target) {
		long el = since();
		int tell = tell();
		boolean emp = empowered();
		double mult = emp ? OathboundBannerMarshalRules.EMPOWERED_DAMAGE_MULT : 1.0;

		switch (move) {
			case OathboundBannerMarshalRules.DECREE_OF_THE_STANDARD -> {
				still();
				face(aim);
				if (!decreeFired && el >= tell) {
					decreeFired = true;
					Light.groundRing(level, position(), HEX_SOLAR, 0.6, 5.2, 0.22, 18);
					Light.ray(level, position().add(0, 0.5, 0), position().add(0, 7.5, 0), HEX_SOLAR, 0.35, 16);
					Feels.sound(level, chestPos(), "duelist_clash", 1.3F, 0.90F);
					for (LivingEntity ally : nearbyAlliedKnights(level)) {
						ally.heal(OathboundBannerMarshalRules.DECREE_HEAL);
						ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, OathboundBannerMarshalRules.DECREE_BUFF_TICKS, 0));
						ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, OathboundBannerMarshalRules.DECREE_BUFF_TICKS, 0));
						Sigils.flash(level, ally.position().add(0, ally.getBbHeight() * 0.6, 0), 0xFFFFE066, 1.4F);
					}
				}
			}
			case OathboundBannerMarshalRules.SOLAR_AEGIS_GUARD -> {
				still();
				if (target != null && !guardCountered() && el <= tell + 12) {
					aim = Mth.rotLerp(0.25F, aim, yawTo(target.position()));
					face(aim);
				} else {
					face(aim);
				}
				if (guardCountered() && !counterHitFired) {
					long cEl = level.getGameTime() - entityData.get(GUARD_COUNTER_BEGAN);
					if (cEl >= 3L) {
						counterHitFired = true;
						Vec3 center = chestPos();
						Light.slash(level, center, UP, ahead(aim), HEX_SOLAR, 3.5, 2.0, 0.22, 1, 11);
						Feels.sound(level, center, "duelist_clash", 1.25F, 1.02F);
						for (LivingEntity foe : targetsIn(level, position(), OathboundBannerMarshalRules.COUNTER_REACH + 1.0)) {
							if (OathboundBannerMarshalRules.inSunbladeArc(aim, foe.getX() - getX(), foe.getZ() - getZ(),
									OathboundBannerMarshalRules.COUNTER_REACH, OathboundBannerMarshalRules.COUNTER_HALF_ANGLE_DEG)) {
								strikeWithKnockback(level, foe, OathboundBannerMarshalRules.COUNTER_DAMAGE * mult,
									OathboundBannerMarshalRules.COUNTER_KNOCKBACK, 0.14);
							}
						}
					}
				}
			}
			case OathboundBannerMarshalRules.RADIANT_SUNBLADE_CLEAVE -> {
				still();
				if (target != null && el < tell) {
					aim = Mth.rotLerp(0.22F, aim, yawTo(target.position()));
				}
				face(aim);
				if (!cleaveHitFired && el >= tell + 2) {
					cleaveHitFired = true;
					Vec3 center = chestPos().add(ahead(aim).scale(0.5));
					Light.slash(level, center, UP, ahead(aim), HEX_SOLAR, 3.7, 2.3, 0.22, 1, 11);
					Feels.sound(level, center, "duelist_clash", 1.15F, 0.98F);
					for (LivingEntity foe : targetsIn(level, position(), OathboundBannerMarshalRules.CLEAVE_REACH + 1.0)) {
						if (OathboundBannerMarshalRules.inSunbladeArc(aim, foe.getX() - getX(), foe.getZ() - getZ(),
								OathboundBannerMarshalRules.CLEAVE_REACH, OathboundBannerMarshalRules.CLEAVE_HALF_ANGLE_DEG)) {
							strikeWithKnockback(level, foe, OathboundBannerMarshalRules.CLEAVE_DAMAGE * mult,
								OathboundBannerMarshalRules.CLEAVE_KNOCKBACK, 0.12);
						}
					}
				}
			}
			case OathboundBannerMarshalRules.SOVEREIGN_JUDGMENT -> {
				still();
				if (target != null && el < tell - 4) {
					aim = Mth.rotLerp(0.18F, aim, yawTo(target.position()));
				}
				face(aim);
				if (el == Math.max(2, tell / 2)) {
					Sigils.layer(level, position().add(0, 0.08, 0), UP, SigilOption.CIRCLE, HEX_SOLAR, 3.6F, 14, 1);
				}
				if (!judgmentHitFired && el >= tell) {
					judgmentHitFired = true;
					landJudgment(level, emp);
				}
			}
			case OathboundBannerMarshalRules.STAGGERED -> {
				still();
				face(aim);
			}
			default -> {
				if (OathboundBannerMarshalRules.technique(move) != null) {
					tickTechnique(level, move, target);
					return;
				}
			}
		}

		moveTicksLeft--;
		if (moveTicksLeft <= 0) {
			entityData.set(MOVE, OathboundBannerMarshalRules.SOVEREIGN_MARCH);
			entityData.set(TELL, 0);
			entityData.set(GUARD_COUNTERED, false);
			gapTicks = emp ? OathboundBannerMarshalRules.MOVE_GAP_EMPOWERED : OathboundBannerMarshalRules.MOVE_GAP_TICKS;
		}
	}

	private void tickTechnique(ServerLevel level, int move, LivingEntity target) {
		OathboundTechnique tech = OathboundBannerMarshalRules.technique(move);
		if (tech == null) {
			return;
		}
		long el = since();
		int tell = tell();
		boolean emp = empowered();
		if (el < tell) {
			still();
			if (target != null && el <= tell - 3) {
				aim = Mth.rotLerp(0.20F, aim, yawTo(target.position()));
			}
			face(aim);
			if (el % 4 == 0) {
				OathboundCombat.windupFx(level, this, tech, aim, el);
			}
			moveTicksLeft--;
			return;
		}
		face(aim);
		long active = el - tell;
		OathboundCombat.motion(this, tech, aim, active);
		if (tech.motion() == OathboundTechnique.Motion.PLANTED) {
			still();
		}
		if (!techHitFired && active >= tech.hitDelay()) {
			techHitFired = true;
			techHitCount = 1;
			OathboundCombat.strike(level, this, tech, aim, struck, emp, OathboundBannerMarshalRules.PIERCE);
			OathboundCombat.strikeFx(level, this, tech, aim);
			if (move == OathboundBannerMarshalRules.RALLYING_CRY
					|| move == OathboundBannerMarshalRules.BANNER_PLANT
					|| move == OathboundBannerMarshalRules.SOLAR_FLARE) {
				for (LivingEntity ally : nearbyAlliedKnights(level)) {
					ally.heal(6.0F);
					ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 100, 0, false, true), this);
					ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 0, false, true), this);
					Sigils.flash(level, ally.position().add(0, ally.getBbHeight() * 0.6, 0), 0xFFFFE066, 1.2F);
				}
			}
		} else if (tech.shape() == OathboundTechnique.Shape.MULTI_HIT && techHitCount < tech.hits()
				&& active >= tech.hitDelay() + techHitCount * 3L) {
			techHitCount++;
			OathboundCombat.strike(level, this, tech, aim, struck, emp, OathboundBannerMarshalRules.PIERCE);
		}
		if (tech.motion() == OathboundTechnique.Motion.DASH && active % 2 == 0) {
			dust(level, position(), 3, 0.25);
		}
		moveTicksLeft--;
		if (moveTicksLeft <= 0) {
			entityData.set(MOVE, OathboundBannerMarshalRules.SOVEREIGN_MARCH);
			entityData.set(TELL, 0);
			entityData.set(GUARD_COUNTERED, false);
			gapTicks = emp ? OathboundBannerMarshalRules.MOVE_GAP_EMPOWERED : OathboundBannerMarshalRules.MOVE_GAP_TICKS;
		}
	}

	private void landJudgment(ServerLevel level, boolean emp) {
		double rOuter = OathboundBannerMarshalRules.JUDGMENT_OUTER_RADIUS;
		Light.groundRing(level, position(), HEX_SOLAR, 0.5, rOuter, 0.24, 18);
		for (int i = 0; i < 6; i++) {
			double ang = i * (Math.PI / 3.0);
			Vec3 pillar = position().add(Math.cos(ang) * 3.4, 0.0, Math.sin(ang) * 3.4);
			Light.ray(level, pillar.add(0, 6.5, 0), pillar, HEX_SOLAR, 0.26, 14);
			dust(level, pillar, 5, 0.35);
			level.sendParticles(ParticleTypes.END_ROD, pillar.x, pillar.y + 0.8, pillar.z, 4, 0.15, 0.5, 0.15, 0.02);
		}
		sound(level, position(), SoundEvents.MACE_SMASH_GROUND_HEAVY, 1.35F, 0.92F);
		Feels.sound(level, position(), "duelist_clash", 1.35F, 0.84F);

		for (LivingEntity foe : targetsIn(level, position(), rOuter + 1.0)) {
			double dist = Math.hypot(foe.getX() - getX(), foe.getZ() - getZ());
			double dmg = OathboundBannerMarshalRules.judgmentDamage(dist, emp);
			if (dmg > 0.0) {
				strikeWithKnockback(level, foe, dmg, OathboundBannerMarshalRules.JUDGMENT_KNOCKBACK, 0.26);
			}
		}
	}

	@Override
	public ProjectileDeflection deflection(Projectile projectile) {
		Entity owner = projectile.getOwner();
		double dx = owner != null ? owner.getX() - getX() : projectile.getX() - getX();
		double dz = owner != null ? owner.getZ() - getZ() : projectile.getZ() - getZ();
		if (OathboundBannerMarshalRules.guardActive(move(), since(), tell(), guardCountered())
				&& OathboundBannerMarshalRules.blocksFrontal(getYRot(), dx, dz)) {
			return McEntities.projectileDeflection((turned, by, rng) -> {
				Vec3 back = owner != null
					? owner.position().add(0.0, owner.getBbHeight() * 0.5, 0.0).subtract(turned.position())
					: ahead(getYRot()).add(0.0, 0.08, 0.0);
				if (back.lengthSqr() > 1.0e-5) {
					Vec3 vel = back.normalize().scale(Math.max(1.3, turned.getDeltaMovement().length()));
					turned.setDeltaMovement(vel);
					McEntities.syncVelocity(turned);
				}
				if (level() instanceof ServerLevel sl) {
					Fx.sound(sl, chestPos(), WildercordSounds.SHIELD_PARRY, 1.2F, 1.05F);
					Sigils.flash(sl, turned.position(), 0xFFFFE066, 1.3F);
				}
			});
		}
		return super.deflection(projectile);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (amount <= 0.0F) {
			return false;
		}
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, amount);
		}

		boolean unblockable = source.is(DamageTypeTags.BYPASSES_SHIELD) || source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC);
		Entity src = source.getEntity();
		double dx = src != null ? src.getX() - getX() : -Math.sin(Math.toRadians(getYRot()));
		double dz = src != null ? src.getZ() - getZ() : Math.cos(Math.toRadians(getYRot()));
		long el = since();
		int m = move();
		int t = tell();
		boolean countered = guardCountered();

		if (src instanceof LivingEntity attacker && attacker.getMainHandItem().is(ItemTags.AXES)
				&& OathboundBannerMarshalRules.axeBreaksGuard(m, el, t, countered, true, getYRot(), dx, dz)) {
			Sigils.flash(level, chestPos(), 0xFFFFD048, 1.8F);
			startMove(level, OathboundBannerMarshalRules.STAGGERED, null);
			return super.hurtServer(level, source, amount * 1.15F);
		}

		boolean guarded = !unblockable && OathboundBannerMarshalRules.guardActive(m, el, t, countered)
			&& OathboundBannerMarshalRules.blocksFrontal(getYRot(), dx, dz);
		double mult = OathboundBannerMarshalRules.damageMultiplier(m, el, t, countered, unblockable, getYRot(), dx, dz);
		float scaled = (float) Math.max(guarded ? 1.0 : 0.0, amount * mult);
		boolean applied = super.hurtServer(level, source, scaled);

		if (applied && guarded) {
			Fx.sound(level, chestPos(), WildercordSounds.SHIELD_PARRY, 1.2F, 1.0F);
			Sigils.flash(level, chestPos().add(ahead(getYRot()).scale(0.85)), 0xFFFFE066, 1.6F);
			entityData.set(GUARD_COUNTERED, true);
			entityData.set(GUARD_COUNTER_BEGAN, level.getGameTime());
			moveTicksLeft = Math.max(moveTicksLeft, OathboundBannerMarshalRules.GUARD_COUNTER_TICKS);
			if (src instanceof LivingEntity attacker) {
				aim = yawTo(attacker.position());
				face(aim);
			}
		}
		return applied;
	}

	private boolean strikeWithKnockback(ServerLevel level, LivingEntity foe, double damage, double shove, double lift) {
		double xRatio = -(foe.getX() - getX());
		double zRatio = -(foe.getZ() - getZ());
		double d = Math.hypot(xRatio, zRatio);
		if (d < 1.0e-4) {
			Vec3 f = ahead(aim);
			xRatio = -f.x;
			zRatio = -f.z;
		} else {
			xRatio /= d;
			zRatio /= d;
		}
		GuardianBite.land(level, this, foe, (float) damage, OathboundBannerMarshalRules.PIERCE);
		foe.knockback(shove, xRatio, zRatio);
		if (lift > 0.0) {
			foe.setDeltaMovement(foe.getDeltaMovement().add(0.0, lift, 0.0));
		}
		McEntities.syncVelocity(foe);
		return true;
	}

	private List<LivingEntity> targetsIn(ServerLevel level, Vec3 center, double radius) {
		AABB box = new AABB(center, center).inflate(radius, 3.0, radius);
		LivingEntity currentTarget = getTarget();
		return level.getEntitiesOfClass(LivingEntity.class, box, e ->
			e.isAlive() && e != this && !(e instanceof OathboundBannerMarshal)
				&& (e instanceof Player p && !p.isCreative() && !p.isSpectator()
					|| e instanceof IronGolem
					|| (currentTarget != null && e == currentTarget)));
	}

	private Vec3 chestPos() {
		return position().add(0.0, getBbHeight() * 0.56, 0.0);
	}

	private void still() {
		getNavigation().stop();
		Vec3 v = getDeltaMovement();
		setDeltaMovement(0.0, Math.min(0.0, v.y), 0.0);
	}

	private void dust(ServerLevel level, Vec3 at, int count, double spread) {
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floorState(level, at)),
			at.x, at.y + 0.1, at.z, count, spread, 0.1, spread, 0.15);
	}

	private BlockState floorState(ServerLevel level, Vec3 at) {
		BlockState state = level.getBlockState(BlockPos.containing(at.x, at.y - 0.5, at.z));
		return state.isAir() ? Blocks.STONE_BRICKS.defaultBlockState() : state;
	}

	private void sound(ServerLevel level, Vec3 at, SoundEvent event, float vol, float pitch) {
		level.playSound(null, at.x, at.y, at.z, event, SoundSource.HOSTILE, vol, pitch);
	}

	private Vec3 ahead(float yaw) {
		double rad = Math.toRadians(yaw);
		return new Vec3(-Math.sin(rad), 0.0, Math.cos(rad));
	}

	private float yawTo(Vec3 at) {
		return (float) Math.toDegrees(Math.atan2(-(at.x - getX()), at.z - getZ()));
	}

	private void face(float yaw) {
		setYRot(yaw);
		yBodyRot = yaw;
		yHeadRot = yaw;
	}

	private void turnToward(float targetYaw, float maxStep) {
		face(getYRot() + Mth.clamp(Mth.wrapDegrees(targetYaw - getYRot()), -maxStep, maxStep));
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return WildercordSounds.kit("duelist_knight_ambient");
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return WildercordSounds.kit("duelist_knight_hurt");
	}

	@Override
	protected SoundEvent getDeathSound() {
		return WildercordSounds.kit("duelist_knight_death");
	}

	@Override
	public float getVoicePitch() {
		return 0.84F + random.nextFloat() * 0.08F;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput out) {
		super.addAdditionalSaveData(out);
		out.putBoolean("marshal_empowered", empowered());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		super.readAdditionalSaveData(in);
		boolean emp = in.getBooleanOr("marshal_empowered", false)
			|| OathboundBannerMarshalRules.empowered(getHealth(), getMaxHealth());
		entityData.set(EMPOWERED, emp);
		entityData.set(MOVE, OathboundBannerMarshalRules.SOVEREIGN_MARCH);
		entityData.set(TELL, 0);
		entityData.set(GUARD_COUNTERED, false);
		entityData.set(PREVIEW_ELAPSED, -1.0F);
	}
}
