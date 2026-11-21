package dev.wildercord.aura.world;

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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import dev.wildercord.world.sites.oathhold.OathholdLaw;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Oathbound Halberdier — Knight #3 (Way of the Storm-Glaive), a swift polearm sentinel armored in gunmetal-indigo
 * and cobalt plate with winged houndskull helm, flowing scarf, cyan-violet rune circuits, and a lightning Storm-Halberd.
 *
 * <p>All timings, whirl radii, hook-pull speeds, and 4-way thunder cross rules live in {@link OathboundHalberdierRules}.</p>
 */
public final class OathboundHalberdier extends PathfinderMob implements Enemy {
	private static final EntityDataAccessor<Integer> MOVE = SynchedEntityData.defineId(OathboundHalberdier.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN = SynchedEntityData.defineId(OathboundHalberdier.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Integer> TELL = SynchedEntityData.defineId(OathboundHalberdier.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> EMPOWERED = SynchedEntityData.defineId(OathboundHalberdier.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Float> PREVIEW_ELAPSED = SynchedEntityData.defineId(OathboundHalberdier.class, EntityDataSerializers.FLOAT);

	private static final int HEX_CYAN = 0x48F0FF;
	private static final int HEX_VIOLET = 0x9A68FF;
	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** Client-side PoseBlend interpolation state. */
	public int shownMove = -1, prevMove = -1, shownTell, prevTell;
	public float shownElapsed, prevElapsed, switchedAt = -100.0F;

	private final MoveDeck deck = new MoveDeck(OathboundHalberdierRules.MEMORY);
	private final long[] lastUsed = new long[8];
	private final Set<UUID> struck = new HashSet<>();

	private float aim;
	private int moveTicksLeft;
	private int gapTicks = 8;
	private boolean whirlHitFired;
	private boolean hookHitFired;
	private boolean crashHitFired;

	{
		Arrays.fill(lastUsed, -9999L);
	}

	public OathboundHalberdier(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		xpReward = OathboundHalberdierRules.XP_REWARD;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, OathboundHalberdierRules.MAX_HEALTH)
			.add(Attributes.ARMOR, OathboundHalberdierRules.ARMOR)
			.add(Attributes.ARMOR_TOUGHNESS, OathboundHalberdierRules.ARMOR_TOUGHNESS)
			.add(Attributes.KNOCKBACK_RESISTANCE, OathboundHalberdierRules.KNOCKBACK_RESISTANCE)
			.add(Attributes.MOVEMENT_SPEED, OathboundHalberdierRules.MOVEMENT_SPEED)
			.add(Attributes.ATTACK_DAMAGE, OathboundHalberdierRules.WHIRL_DAMAGE)
			.add(Attributes.FOLLOW_RANGE, OathboundHalberdierRules.FOLLOW_RANGE);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(MOVE, OathboundHalberdierRules.LOW_GLAIVE_STALK);
		builder.define(BEGAN, 0L);
		builder.define(TELL, 0);
		builder.define(EMPOWERED, false);
		builder.define(PREVIEW_ELAPSED, -1.0F);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.78));
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
	public void poseForPreview(int move, int tellTicks, float fixedElapsed) {
		entityData.set(MOVE, move);
		entityData.set(TELL, tellTicks);
		entityData.set(BEGAN, level().getGameTime() - (long) fixedElapsed);
		entityData.set(PREVIEW_ELAPSED, fixedElapsed);
		moveTicksLeft = Math.max(0, OathboundHalberdierRules.moveDuration(move, empowered()) - (int) fixedElapsed);
		aim = getYRot();
	}

	/** Starts a live combat move from AI or test harnesses. */
	public void startMove(ServerLevel level, int nextMove, @Nullable LivingEntity target) {
		boolean emp = empowered();
		int tell = OathboundHalberdierRules.tellTicks(nextMove, emp);
		int duration = nextMove == OathboundHalberdierRules.STAGGERED
			? OathboundHalberdierRules.STAGGER_TICKS
			: OathboundHalberdierRules.moveDuration(nextMove, emp);
		startMoveWithDuration(level, nextMove, tell, duration, target);
	}

	private void startMoveWithDuration(ServerLevel level, int nextMove, int tell, int duration, @Nullable LivingEntity target) {
		entityData.set(PREVIEW_ELAPSED, -1.0F);
		entityData.set(MOVE, nextMove);
		entityData.set(TELL, tell);
		entityData.set(BEGAN, level.getGameTime());
		moveTicksLeft = duration;
		whirlHitFired = false;
		hookHitFired = false;
		crashHitFired = false;
		struck.clear();
		getNavigation().stop();

		if (target != null) {
			aim = yawTo(target.position());
			face(aim);
		} else {
			aim = getYRot();
		}

		if (nextMove != OathboundHalberdierRules.LOW_GLAIVE_STALK && nextMove != OathboundHalberdierRules.STAGGERED) {
			lastUsed[nextMove] = level.getGameTime();
			deck.played(nextMove);
		}

		Vec3 chest = chestPos();
		switch (nextMove) {
			case OathboundHalberdierRules.TEMPEST_WHIRL ->
				Feels.sound(level, chest, "duelist_knight_windup", 1.15F, 1.18F);
			case OathboundHalberdierRules.SHADOWSTEP_THRUST -> {
				Feels.sound(level, chest, "duelist_knight_windup", 1.0F, 1.38F);
				Sigils.flash(level, chest, 0xFF48F0FF, 1.5F);
			}
			case OathboundHalberdierRules.HOOK_AND_REAP ->
				Feels.sound(level, chest, "duelist_knight_windup", 1.05F, 1.02F);
			case OathboundHalberdierRules.SKY_VAULT_CRASH -> {
				Feels.sound(level, chest, "duelist_knight_windup", 1.25F, 0.90F);
				Light.groundRing(level, position(), HEX_CYAN, 0.35, 2.6, 0.08, tell);
			}
			case OathboundHalberdierRules.STAGGERED ->
				sound(level, chest, SoundEvents.SHIELD_BREAK.value(), 1.25F, 0.92F);
			default -> {}
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);

		if (!empowered() && OathboundHalberdierRules.empowered(getHealth(), getMaxHealth())) {
			entityData.set(EMPOWERED, true);
			Feels.sound(level, chestPos(), "duelist_clash", 1.3F, 1.15F);
			Light.groundRing(level, position(), HEX_CYAN, 0.5, 4.6, 0.14, 16);
			Sigils.flash(level, chestPos(), 0xFF48F0FF, 2.1F);
		}

		LivingEntity target = getTarget();
		if (target != null && !target.isAlive()) {
			setTarget(null);
			target = null;
		}

		int move = move();
		if (move != OathboundHalberdierRules.LOW_GLAIVE_STALK) {
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
		if (dist > 3.5) {
			getNavigation().moveTo(target, 1.12);
			if (tickCount % 10 == 0 && onGround()) {
				Feels.sound(level, position(), "duelist_knight_step", 0.72F, 1.04F + random.nextFloat() * 0.12F);
			}
		} else {
			getNavigation().stop();
		}
		turnToward(yawTo(target.position()), 15.0F);

		if (gapTicks <= 0 && dist <= 8.4) {
			int next = chooseMove(level, dist);
			if (next > 0) {
				startMove(level, next, target);
			}
		}
	}

	private int chooseMove(ServerLevel level, double dist) {
		long now = level.getGameTime();
		boolean emp = empowered();
		int[] moves = OathboundHalberdierRules.COMBAT_MOVES;
		double[] weights = new double[moves.length];

		for (int i = 0; i < moves.length; i++) {
			int m = moves[i];
			if (now - lastUsed[m] < OathboundHalberdierRules.moveCooldown(m, emp)) {
				continue;
			}
			weights[i] = switch (m) {
				case OathboundHalberdierRules.TEMPEST_WHIRL -> dist <= 4.7 ? 2.0 : 0.0;
				case OathboundHalberdierRules.SHADOWSTEP_THRUST -> (dist >= 2.2 && dist <= 8.2) ? (emp ? 2.3 : 1.7) : 0.0;
				case OathboundHalberdierRules.HOOK_AND_REAP -> (dist >= 2.8 && dist <= 6.2) ? 1.55 : 0.0;
				case OathboundHalberdierRules.SKY_VAULT_CRASH -> dist <= 6.2 ? (emp ? 1.8 : 1.25) : 0.0;
				default -> 0.0;
			};
		}
		return deck.pick(moves, weights, random.nextDouble());
	}

	private void tickActiveMove(ServerLevel level, int move, @Nullable LivingEntity target) {
		long el = since();
		int tell = tell();
		boolean emp = empowered();
		double mult = emp ? OathboundHalberdierRules.EMPOWERED_DAMAGE_MULT : 1.0;

		switch (move) {
			case OathboundHalberdierRules.TEMPEST_WHIRL -> {
				still();
				face(aim);
				if (!whirlHitFired && el >= tell + 2) {
					whirlHitFired = true;
					Vec3 center = chestPos();
					Light.slash(level, center, UP, ahead(aim), HEX_CYAN, 4.4, Math.PI * 1.85, 0.24, 1, 12);
					Light.slash(level, center.add(0, 0.14, 0), UP, ahead(aim), HEX_VIOLET, 4.0, Math.PI * 1.65, 0.14, 1, 10);
					Feels.sound(level, center, "duelist_clash", 1.25F, 1.12F);
					for (LivingEntity foe : targetsIn(level, position(), OathboundHalberdierRules.WHIRL_RADIUS + 0.8)) {
						if (OathboundHalberdierRules.inWhirlRadius(foe.getX() - getX(), foe.getZ() - getZ())) {
							strikeWithKnockback(level, foe, OathboundHalberdierRules.WHIRL_DAMAGE * mult,
								OathboundHalberdierRules.WHIRL_KNOCKBACK, 0.14);
						}
					}
				}
			}
			case OathboundHalberdierRules.SHADOWSTEP_THRUST -> {
				if (el < tell) {
					still();
					if (target != null) {
						aim = Mth.rotLerp(0.35F, aim, yawTo(target.position()));
					}
					face(aim);
				} else if (el < tell + OathboundHalberdierRules.THRUST_DASH_TICKS) {
					face(aim);
					Vec3 dir = ahead(aim);
					double speed = OathboundHalberdierRules.THRUST_SPEED * (emp ? 1.12 : 1.0);
					setDeltaMovement(dir.x * speed, Math.min(0.0, getDeltaMovement().y), dir.z * speed);
					move(MoverType.SELF, getDeltaMovement());
					level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.1, getZ(), 6, 0.3, 0.4, 0.3, 0.05);
					for (LivingEntity foe : targetsIn(level, position().add(dir.scale(2.0)), OathboundHalberdierRules.THRUST_LENGTH)) {
						if (OathboundHalberdierRules.inThrustLine(aim, foe.getX() - getX(), foe.getZ() - getZ())
								&& struck.add(foe.getUUID())) {
							Feels.sound(level, foe.position(), "duelist_clash", 1.2F, 1.28F);
							strikeWithKnockback(level, foe, OathboundHalberdierRules.THRUST_DAMAGE * mult,
								OathboundHalberdierRules.THRUST_KNOCKBACK, 0.14);
						}
					}
				} else {
					still();
					face(aim);
				}
			}
			case OathboundHalberdierRules.HOOK_AND_REAP -> {
				still();
				if (target != null && el < tell) {
					aim = Mth.rotLerp(0.28F, aim, yawTo(target.position()));
				}
				face(aim);
				if (!hookHitFired && el >= tell) {
					hookHitFired = true;
					Vec3 dir = ahead(aim);
					Light.slash(level, chestPos().add(dir.scale(1.4)), UP, dir, HEX_CYAN, 3.2, 1.5, 0.18, 1, 10);
					Feels.sound(level, chestPos(), "duelist_clash", 1.1F, 0.92F);
					for (LivingEntity foe : targetsIn(level, position(), OathboundHalberdierRules.HOOK_LENGTH + 1.0)) {
						if (OathboundHalberdierRules.inHookLine(aim, foe.getX() - getX(), foe.getZ() - getZ())) {
							GuardianBite.land(level, this, foe, (float) (OathboundHalberdierRules.HOOK_DAMAGE * mult),
								OathboundHalberdierRules.PIERCE);
							Vec3 pull = position().add(dir.scale(1.2)).subtract(foe.position());
							Vec3 horiz = new Vec3(pull.x, 0.0, pull.z);
							if (horiz.lengthSqr() > 1.0e-4) {
								Vec3 vel = horiz.normalize().scale(OathboundHalberdierRules.HOOK_PULL_SPEED).add(0.0, 0.24, 0.0);
								foe.setDeltaMovement(vel);
								McEntities.syncVelocity(foe);
							}
							foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, OathboundHalberdierRules.HOOK_SLOW_TICKS, 1));
							Sigils.flash(level, foe.position().add(0, 1.0, 0), 0xFF48F0FF, 1.4F);
						}
					}
				}
			}
			case OathboundHalberdierRules.SKY_VAULT_CRASH -> {
				still();
				if (target != null && el < tell - 4) {
					aim = Mth.rotLerp(0.20F, aim, yawTo(target.position()));
				}
				face(aim);
				if (el == Math.max(2, tell / 2)) {
					Sigils.layer(level, position().add(0, 2.2, 0), UP, SigilOption.RING, HEX_VIOLET, 1.5F, 14, 1);
				}
				if (!crashHitFired && el >= tell) {
					crashHitFired = true;
					landThunderCrash(level, mult);
				}
			}
			case OathboundHalberdierRules.STAGGERED -> {
				still();
				face(aim);
			}
			default -> {}
		}

		moveTicksLeft--;
		if (moveTicksLeft <= 0) {
			entityData.set(MOVE, OathboundHalberdierRules.LOW_GLAIVE_STALK);
			entityData.set(TELL, 0);
			gapTicks = emp ? OathboundHalberdierRules.MOVE_GAP_EMPOWERED : OathboundHalberdierRules.MOVE_GAP_TICKS;
		}
	}

	private void landThunderCrash(ServerLevel level, double mult) {
		Vec3 center = position().add(0, 0.15, 0);
		Vec3 fwd = ahead(aim);
		Vec3 right = new Vec3(-fwd.z, 0, fwd.x);
		double len = OathboundHalberdierRules.CRASH_CROSS_LENGTH;

		Light.groundRing(level, position(), HEX_CYAN, 0.4, 3.6, 0.18, 16);
		Light.ray(level, center, center.add(fwd.scale(len)), HEX_CYAN, 0.28, 12);
		Light.ray(level, center, center.subtract(fwd.scale(len)), HEX_CYAN, 0.28, 12);
		Light.ray(level, center, center.add(right.scale(len)), HEX_VIOLET, 0.28, 12);
		Light.ray(level, center, center.subtract(right.scale(len)), HEX_VIOLET, 0.28, 12);

		for (Vec3 arm : List.of(fwd, fwd.scale(-1.0), right, right.scale(-1.0))) {
			for (int step = 1; step <= 5; step++) {
				Vec3 pt = position().add(arm.scale(step * 1.1));
				dust(level, pt, 5, 0.35);
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pt.x, pt.y + 0.25, pt.z, 6, 0.2, 0.12, 0.2, 0.04);
			}
		}
		sound(level, position(), SoundEvents.MACE_SMASH_GROUND, 1.35F, 1.05F);
		sound(level, position(), SoundEvents.TRIDENT_THUNDER.value(), 1.25F, 1.10F);

		boolean emp = empowered();
		for (LivingEntity foe : targetsIn(level, position(), len + 1.5)) {
			double dx = foe.getX() - getX();
			double dz = foe.getZ() - getZ();
			if (OathboundHalberdierRules.inThunderCross(aim, dx, dz)) {
				double dmg = OathboundHalberdierRules.crashDamage(Math.hypot(dx, dz), emp);
				strikeWithKnockback(level, foe, dmg, OathboundHalberdierRules.CRASH_KNOCKBACK, 0.26);
			}
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (amount <= 0.0F) {
			return false;
		}
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, amount);
		}
		Entity src = source.getEntity();
		int m = move();
		int t = tell();
		long el = since();
		if (src instanceof LivingEntity attacker && attacker.getMainHandItem().is(ItemTags.AXES)
				&& OathboundHalberdierRules.axeInterruptsWindup(m, el, t, true)) {
			Sigils.flash(level, chestPos(), 0xFF48F0FF, 1.6F);
			startMove(level, OathboundHalberdierRules.STAGGERED, null);
			return super.hurtServer(level, source, amount * 1.15F);
		}
		double mult = OathboundHalberdierRules.damageMultiplier(m, el, t);
		return super.hurtServer(level, source, (float) (amount * mult));
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
		GuardianBite.land(level, this, foe, (float) damage, OathboundHalberdierRules.PIERCE);
		foe.knockback(shove, xRatio, zRatio, level.damageSources().mobAttack(this), 0.0F);
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
			e.isAlive() && e != this && !(e instanceof OathboundHalberdier)
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
		return 1.02F + random.nextFloat() * 0.08F;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput out) {
		super.addAdditionalSaveData(out);
		out.putBoolean("halberdier_empowered", empowered());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		super.readAdditionalSaveData(in);
		boolean emp = in.getBooleanOr("halberdier_empowered", false)
			|| OathboundHalberdierRules.empowered(getHealth(), getMaxHealth());
		entityData.set(EMPOWERED, emp);
		entityData.set(MOVE, OathboundHalberdierRules.LOW_GLAIVE_STALK);
		entityData.set(TELL, 0);
		entityData.set(PREVIEW_ELAPSED, -1.0F);
	}
}
