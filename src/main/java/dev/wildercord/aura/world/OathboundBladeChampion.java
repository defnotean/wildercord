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
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
 * The Oathbound Blade-Champion — Knight #2 (Way of the Blade), a duel-master elite knight in fluted silver-and-gold
 * plate, crimson sash, glowing crimson T-visor, flowing crimson plume, split swallow-tail banner cape, and an ornate
 * crimson-rune Zweihänder greatsword.
 *
 * <p>All timings, parry angles, and fissure geometry live in {@link OathboundBladeChampionRules}.</p>
 */
public final class OathboundBladeChampion extends PathfinderMob implements Enemy {
	private static final EntityDataAccessor<Integer> MOVE = SynchedEntityData.defineId(OathboundBladeChampion.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN = SynchedEntityData.defineId(OathboundBladeChampion.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Integer> TELL = SynchedEntityData.defineId(OathboundBladeChampion.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> RIPOSTE_TRIGGERED = SynchedEntityData.defineId(OathboundBladeChampion.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Long> RIPOSTE_BEGAN = SynchedEntityData.defineId(OathboundBladeChampion.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Boolean> EMPOWERED = SynchedEntityData.defineId(OathboundBladeChampion.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Float> PREVIEW_ELAPSED = SynchedEntityData.defineId(OathboundBladeChampion.class, EntityDataSerializers.FLOAT);

	private static final int HEX_CRIMSON = 0xE63830;
	private static final int HEX_GOLD = 0xFFD048;
	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** Client-side PoseBlend interpolation state (same convention as OathboundBastion). */
	public int shownMove = -1, prevMove = -1, shownTell, prevTell;
	public float shownElapsed, prevElapsed, switchedAt = -100.0F;

	private final MoveDeck deck = new MoveDeck(OathboundBladeChampionRules.MEMORY);
	private final long[] lastUsed = new long[8];
	private final Set<UUID> struck = new HashSet<>();

	private float aim;
	private int moveTicksLeft;
	private int gapTicks = 9;
	private boolean crescentHitFired;
	private boolean pommelHitFired;
	private boolean guillotineHitFired;
	private boolean riposteCounterHitFired;

	{
		Arrays.fill(lastUsed, -9999L);
	}

	public OathboundBladeChampion(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		xpReward = OathboundBladeChampionRules.XP_REWARD;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, OathboundBladeChampionRules.MAX_HEALTH)
			.add(Attributes.ARMOR, OathboundBladeChampionRules.ARMOR)
			.add(Attributes.ARMOR_TOUGHNESS, OathboundBladeChampionRules.ARMOR_TOUGHNESS)
			.add(Attributes.KNOCKBACK_RESISTANCE, OathboundBladeChampionRules.KNOCKBACK_RESISTANCE)
			.add(Attributes.MOVEMENT_SPEED, OathboundBladeChampionRules.MOVEMENT_SPEED)
			.add(Attributes.ATTACK_DAMAGE, OathboundBladeChampionRules.CRESCENT_DAMAGE)
			.add(Attributes.FOLLOW_RANGE, OathboundBladeChampionRules.FOLLOW_RANGE);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(MOVE, OathboundBladeChampionRules.IDLE_STALK);
		builder.define(BEGAN, 0L);
		builder.define(TELL, 0);
		builder.define(RIPOSTE_TRIGGERED, false);
		builder.define(RIPOSTE_BEGAN, 0L);
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
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
	}

	public int move() {
		return entityData.get(MOVE);
	}

	public int tell() {
		return entityData.get(TELL);
	}

	public boolean riposteTriggered() {
		return entityData.get(RIPOSTE_TRIGGERED);
	}

	public float riposteCounterElapsed(float partialTick) {
		if (!riposteTriggered()) {
			return 0.0F;
		}
		return Math.max(0.0F, (float) (level().getGameTime() - entityData.get(RIPOSTE_BEGAN)) + partialTick);
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
	public void poseForPreview(int move, int tellTicks, float fixedElapsed, boolean counterTriggered) {
		entityData.set(MOVE, move);
		entityData.set(TELL, tellTicks);
		entityData.set(BEGAN, level().getGameTime() - (long) fixedElapsed);
		entityData.set(PREVIEW_ELAPSED, fixedElapsed);
		entityData.set(RIPOSTE_TRIGGERED, counterTriggered);
		entityData.set(RIPOSTE_BEGAN, level().getGameTime());
		moveTicksLeft = Math.max(0, OathboundBladeChampionRules.moveDuration(move, empowered()) - (int) fixedElapsed);
		aim = getYRot();
	}

	/** Starts a live combat move from AI or test harnesses. */
	public void startMove(ServerLevel level, int nextMove, @Nullable LivingEntity target) {
		boolean emp = empowered();
		int tell = OathboundBladeChampionRules.tellTicks(nextMove, emp);
		int duration = nextMove == OathboundBladeChampionRules.STAGGERED
			? OathboundBladeChampionRules.STAGGER_AXE_TICKS
			: OathboundBladeChampionRules.moveDuration(nextMove, emp);
		startMoveWithDuration(level, nextMove, tell, duration, target);
	}

	private void startMoveWithDuration(ServerLevel level, int nextMove, int tell, int duration, @Nullable LivingEntity target) {
		entityData.set(PREVIEW_ELAPSED, -1.0F);
		entityData.set(MOVE, nextMove);
		entityData.set(TELL, tell);
		entityData.set(BEGAN, level.getGameTime());
		entityData.set(RIPOSTE_TRIGGERED, false);
		entityData.set(RIPOSTE_BEGAN, 0L);
		moveTicksLeft = duration;
		crescentHitFired = false;
		pommelHitFired = false;
		guillotineHitFired = false;
		riposteCounterHitFired = false;
		struck.clear();
		getNavigation().stop();

		if (target != null) {
			aim = yawTo(target.position());
			face(aim);
		} else {
			aim = getYRot();
		}

		if (nextMove != OathboundBladeChampionRules.IDLE_STALK && nextMove != OathboundBladeChampionRules.STAGGERED) {
			lastUsed[nextMove] = level.getGameTime();
			deck.played(nextMove);
		}

		Vec3 chest = chestPos();
		switch (nextMove) {
			case OathboundBladeChampionRules.WRATHFUL_RIPOSTE -> {
				Feels.sound(level, chest, "duelist_clash", 0.9F, 1.25F);
				Sigils.layer(level, chest.add(ahead(aim).scale(0.8)), ahead(aim), SigilOption.RING, HEX_GOLD, 1.35F, tell + 8, 1);
			}
			case OathboundBladeChampionRules.CRESCENT_CLEAVE ->
				Feels.sound(level, chest, "duelist_knight_windup", 1.15F, 1.05F);
			case OathboundBladeChampionRules.POMMEL_BREAK ->
				Feels.sound(level, chest, "duelist_knight_windup", 1.05F, 1.25F);
			case OathboundBladeChampionRules.GUILLOTINE_SLAM -> {
				Feels.sound(level, chest, "duelist_knight_windup", 1.3F, 0.85F);
				Light.groundRing(level, position(), HEX_CRIMSON, 0.35, 2.2, 0.08, tell);
			}
			case OathboundBladeChampionRules.STAGGERED ->
				sound(level, chest, SoundEvents.SHIELD_BREAK.value(), 1.3F, 0.9F);
			default -> {}
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);

		if (!empowered() && OathboundBladeChampionRules.empowered(getHealth(), getMaxHealth())) {
			entityData.set(EMPOWERED, true);
			Feels.sound(level, chestPos(), "duelist_clash", 1.35F, 0.88F);
			Light.groundRing(level, position(), HEX_CRIMSON, 0.5, 4.5, 0.14, 18);
			Sigils.flash(level, chestPos(), 0xFFFF3830, 2.2F);
		}

		LivingEntity target = getTarget();
		if (target != null && !target.isAlive()) {
			setTarget(null);
			target = null;
		}

		int move = move();
		if (move != OathboundBladeChampionRules.IDLE_STALK) {
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
			getNavigation().moveTo(target, 1.08);
			if (tickCount % 11 == 0 && onGround()) {
				Feels.sound(level, position(), "duelist_knight_step", 0.75F, 0.96F + random.nextFloat() * 0.12F);
			}
		} else {
			getNavigation().stop();
		}
		turnToward(yawTo(target.position()), 14.0F);

		if (gapTicks <= 0 && dist <= 7.8) {
			int next = chooseMove(level, target, dist);
			if (next > 0) {
				startMove(level, next, target);
			}
		}
	}

	private int chooseMove(ServerLevel level, LivingEntity target, double dist) {
		long now = level.getGameTime();
		boolean emp = empowered();
		int[] moves = OathboundBladeChampionRules.COMBAT_MOVES;
		double[] weights = new double[moves.length];
		boolean targetBlocking = target instanceof Player p && p.isBlocking();

		for (int i = 0; i < moves.length; i++) {
			int m = moves[i];
			if (now - lastUsed[m] < OathboundBladeChampionRules.moveCooldown(m, emp)) {
				continue;
			}
			weights[i] = switch (m) {
				case OathboundBladeChampionRules.WRATHFUL_RIPOSTE -> dist <= 4.5 ? (emp ? 1.5 : 1.3) : 0.35;
				case OathboundBladeChampionRules.CRESCENT_CLEAVE -> dist <= 4.6 ? 2.1 : 0.0;
				case OathboundBladeChampionRules.POMMEL_BREAK -> (dist >= 1.8 && dist <= 7.4) ? (targetBlocking ? 3.2 : 1.55) : 0.0;
				case OathboundBladeChampionRules.GUILLOTINE_SLAM -> dist <= 6.2 ? (emp ? 1.85 : 1.25) : 0.0;
				default -> 0.0;
			};
		}
		return deck.pick(moves, weights, random.nextDouble());
	}

	private void tickActiveMove(ServerLevel level, int move, @Nullable LivingEntity target) {
		long el = since();
		int tell = tell();
		boolean emp = empowered();

		switch (move) {
			case OathboundBladeChampionRules.WRATHFUL_RIPOSTE -> {
				still();
				if (target != null && !riposteTriggered() && el <= tell + 12) {
					aim = Mth.rotLerp(0.28F, aim, yawTo(target.position()));
					face(aim);
				} else {
					face(aim);
				}
				if (riposteTriggered() && !riposteCounterHitFired) {
					long counterEl = level.getGameTime() - entityData.get(RIPOSTE_BEGAN);
					if (counterEl >= 3L) {
						riposteCounterHitFired = true;
						double mult = emp ? OathboundBladeChampionRules.EMPOWERED_DAMAGE_MULT : 1.0;
						Vec3 center = chestPos();
						Vec3 forward = ahead(aim);
						Light.slash(level, center, UP, forward, HEX_GOLD, 3.6, 2.4, 0.22, 1.0, 12);
						Light.slash(level, center.add(0, 0.12, 0), UP, forward, HEX_CRIMSON, 3.3, 2.2, 0.18, 1.0, 10);
						Feels.sound(level, center, "duelist_clash", 1.3F, 1.05F);
						for (LivingEntity foe : targetsIn(level, position(), OathboundBladeChampionRules.RIPOSTE_COUNTER_REACH + 1.0)) {
							if (OathboundBladeChampionRules.inCleaveArc(aim, foe.getX() - getX(), foe.getZ() - getZ(),
									OathboundBladeChampionRules.RIPOSTE_COUNTER_REACH, OathboundBladeChampionRules.RIPOSTE_COUNTER_HALF_ANGLE_DEG)) {
								strikeWithKnockback(level, foe, OathboundBladeChampionRules.RIPOSTE_COUNTER_DAMAGE * mult,
									OathboundBladeChampionRules.RIPOSTE_COUNTER_KNOCKBACK, 0.14);
							}
						}
					}
				}
			}
			case OathboundBladeChampionRules.CRESCENT_CLEAVE -> {
				still();
				if (target != null && el < tell) {
					aim = Mth.rotLerp(0.22F, aim, yawTo(target.position()));
				}
				face(aim);
				if (!crescentHitFired && el >= tell + 2) {
					crescentHitFired = true;
					double mult = emp ? OathboundBladeChampionRules.EMPOWERED_DAMAGE_MULT : 1.0;
					Vec3 center = chestPos().add(ahead(aim).scale(0.6));
					Light.slash(level, center, UP, ahead(aim), HEX_CRIMSON, 3.8, 2.6, 0.24, 1.0, 11);
					Light.slash(level, center.add(0, 0.08, 0), UP, ahead(aim), 0xFFFFFF, 3.4, 2.2, 0.10, 1.0, 8);
					Feels.sound(level, center, "duelist_clash", 1.15F, 0.95F);
					for (LivingEntity foe : targetsIn(level, position(), OathboundBladeChampionRules.CRESCENT_REACH + 1.0)) {
						if (OathboundBladeChampionRules.inCleaveArc(aim, foe.getX() - getX(), foe.getZ() - getZ(),
								OathboundBladeChampionRules.CRESCENT_REACH, OathboundBladeChampionRules.CRESCENT_HALF_ANGLE_DEG)) {
							strikeWithKnockback(level, foe, OathboundBladeChampionRules.CRESCENT_DAMAGE * mult,
								OathboundBladeChampionRules.CRESCENT_KNOCKBACK, 0.12);
						}
					}
				}
			}
			case OathboundBladeChampionRules.POMMEL_BREAK -> {
				if (el < tell) {
					still();
					if (target != null) {
						aim = Mth.rotLerp(0.32F, aim, yawTo(target.position()));
					}
					face(aim);
				} else if (el < tell + OathboundBladeChampionRules.POMMEL_LUNGE_TICKS) {
					face(aim);
					Vec3 dir = ahead(aim);
					double speed = OathboundBladeChampionRules.POMMEL_LUNGE_SPEED * (emp ? 1.12 : 1.0);
					setDeltaMovement(dir.x * speed, Math.min(0.0, getDeltaMovement().y), dir.z * speed);
					move(MoverType.SELF, getDeltaMovement());
					if (el % 2 == 0) {
						dust(level, position(), 3, 0.25);
					}
					if (!pommelHitFired) {
						Vec3 tip = chestPos().add(dir.scale(1.15));
						double mult = emp ? OathboundBladeChampionRules.EMPOWERED_DAMAGE_MULT : 1.0;
						for (LivingEntity foe : targetsIn(level, tip, OathboundBladeChampionRules.POMMEL_REACH)) {
							if (struck.add(foe.getUUID())) {
								pommelHitFired = true;
								boolean shattered = shatterShield(level, foe);
								strikeWithKnockback(level, foe, OathboundBladeChampionRules.POMMEL_DAMAGE * mult,
									OathboundBladeChampionRules.POMMEL_KNOCKBACK, 0.16);
								foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, OathboundBladeChampionRules.POMMEL_SLOW_TICKS, 1));
								Sigils.flash(level, foe.position().add(0, foe.getBbHeight() * 0.55, 0),
									shattered ? 0xFFFFD048 : 0xFFFF4838, shattered ? 1.8F : 1.2F);
								Feels.sound(level, foe.position(), shattered ? "aura_perfect_guard" : "aura_guard", 1.25F, 0.85F);
								break;
							}
						}
					}
				} else {
					still();
					face(aim);
				}
			}
			case OathboundBladeChampionRules.GUILLOTINE_SLAM -> {
				still();
				if (target != null && el < tell - 4) {
					aim = Mth.rotLerp(0.18F, aim, yawTo(target.position()));
				}
				face(aim);
				if (el == Math.max(2, tell / 2)) {
					Sigils.layer(level, position().add(0, 1.9, 0), ahead(aim), SigilOption.CIRCLE, HEX_CRIMSON, 1.4F, 14, -1);
				}
				if (!guillotineHitFired && el >= tell) {
					guillotineHitFired = true;
					landGuillotine(level);
				}
			}
			case OathboundBladeChampionRules.STAGGERED -> {
				still();
				face(aim);
			}
			default -> {}
		}

		moveTicksLeft--;
		if (moveTicksLeft <= 0) {
			entityData.set(MOVE, OathboundBladeChampionRules.IDLE_STALK);
			entityData.set(TELL, 0);
			entityData.set(RIPOSTE_TRIGGERED, false);
			gapTicks = emp ? OathboundBladeChampionRules.MOVE_GAP_EMPOWERED : OathboundBladeChampionRules.MOVE_GAP_TICKS;
		}
	}

	private void landGuillotine(ServerLevel level) {
		Vec3 forward = ahead(aim);
		Vec3 impact = position().add(forward.scale(2.1));
		boolean emp = empowered();

		Light.groundRing(level, impact, HEX_CRIMSON, 0.4, 3.8, 0.20, 16);
		Light.slash(level, position().add(0, 1.1, 0), new Vec3(-forward.z, 0, forward.x), forward, HEX_CRIMSON, 3.8, 2.2, 0.26, 1.0, 12);
		for (int step = 1; step <= 6; step++) {
			Vec3 pt = position().add(forward.scale(step * 1.05));
			dust(level, pt, 8, 0.45);
			level.sendParticles(ParticleTypes.FLAME, pt.x, pt.y + 0.18, pt.z, 4, 0.2, 0.08, 0.2, 0.02);
		}
		sound(level, impact, SoundEvents.MACE_SMASH_GROUND.value(), 1.4F, 0.85F);
		Feels.sound(level, impact, "duelist_clash", 1.35F, 0.78F);

		for (LivingEntity foe : targetsIn(level, position(), OathboundBladeChampionRules.GUILLOTINE_LINE_LENGTH + 1.5)) {
			double dx = foe.getX() - getX();
			double dz = foe.getZ() - getZ();
			if (OathboundBladeChampionRules.inGuillotineLine(aim, dx, dz)) {
				double dist = Math.hypot(dx, dz);
				double dmg = OathboundBladeChampionRules.guillotineDamage(dist, emp);
				strikeWithKnockback(level, foe, dmg, OathboundBladeChampionRules.GUILLOTINE_KNOCKBACK, 0.24);
			}
		}
	}

	private static boolean shatterShield(ServerLevel level, LivingEntity foe) {
		if (!(foe instanceof Player player)) {
			return false;
		}
		boolean blocking = player.isBlocking();
		player.resetAttackStrengthTicker();
		if (player.isUsingItem()) {
			player.stopUsingItem();
		}
		player.getCooldowns().addCooldown(new ItemStack(Items.SHIELD), OathboundBladeChampionRules.POMMEL_SHIELD_DISABLE_TICKS);
		if (blocking) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.2F, 0.9F);
			return true;
		}
		return false;
	}

	@Override
	public ProjectileDeflection deflection(Projectile projectile) {
		Entity owner = projectile.getOwner();
		double dx = owner != null ? owner.getX() - getX() : projectile.getX() - getX();
		double dz = owner != null ? owner.getZ() - getZ() : projectile.getZ() - getZ();
		if (OathboundBladeChampionRules.parryActive(move(), since(), tell(), riposteTriggered())
				&& OathboundBladeChampionRules.frontalParry(getYRot(), dx, dz)) {
			return McEntities.projectileDeflection((turned, by, rng) -> {
				Vec3 back = owner != null
					? owner.position().add(0.0, owner.getBbHeight() * 0.5, 0.0).subtract(turned.position())
					: ahead(getYRot()).add(0.0, 0.08, 0.0);
				if (back.lengthSqr() > 1.0e-5) {
					Vec3 vel = back.normalize().scale(Math.max(1.35, turned.getDeltaMovement().length()));
					turned.setDeltaMovement(vel);
					McEntities.syncVelocity(turned);
				}
				if (level() instanceof ServerLevel sl) {
					Fx.sound(sl, chestPos(), WildercordSounds.SHIELD_PARRY, 1.2F, 1.15F);
					Sigils.flash(sl, turned.position(), 0xFFFFD048, 1.3F);
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
		boolean triggered = riposteTriggered();

		if (src instanceof LivingEntity attacker && attacker.getMainHandItem().is(ItemTags.AXES)
				&& OathboundBladeChampionRules.axeBreaksParry(m, el, t, triggered, true, getYRot(), dx, dz)) {
			Sigils.flash(level, chestPos(), 0xFFFF6030, 1.8F);
			startMove(level, OathboundBladeChampionRules.STAGGERED, null);
			return super.hurtServer(level, source, amount * 1.15F);
		}

		boolean parried = OathboundBladeChampionRules.parriesAttack(m, el, t, triggered, unblockable, getYRot(), dx, dz);
		double mult = OathboundBladeChampionRules.damageMultiplier(m, el, t, triggered, unblockable, getYRot(), dx, dz);
		float scaled = (float) Math.max(parried ? 1.0 : 0.0, amount * mult);

		boolean applied = super.hurtServer(level, source, scaled);
		if (applied && parried) {
			Fx.sound(level, chestPos(), WildercordSounds.SHIELD_PARRY, 1.25F, 1.05F);
			Sigils.flash(level, chestPos().add(ahead(getYRot()).scale(0.85)), 0xFFFFD048, 1.6F);
			// Trigger immediate Wrathful Riposte counter-cleave!
			entityData.set(RIPOSTE_TRIGGERED, true);
			entityData.set(RIPOSTE_BEGAN, level.getGameTime());
			moveTicksLeft = Math.max(moveTicksLeft, OathboundBladeChampionRules.RIPOSTE_COUNTER_TICKS);
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
		GuardianBite.land(level, this, foe, (float) damage, OathboundBladeChampionRules.PIERCE);
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
			e.isAlive() && e != this && !(e instanceof OathboundBladeChampion)
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
		return 0.92F + random.nextFloat() * 0.08F;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput out) {
		super.addAdditionalSaveData(out);
		out.putBoolean("blade_champion_empowered", empowered());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		super.readAdditionalSaveData(in);
		boolean emp = in.getBooleanOr("blade_champion_empowered", false)
			|| OathboundBladeChampionRules.empowered(getHealth(), getMaxHealth());
		entityData.set(EMPOWERED, emp);
		entityData.set(MOVE, OathboundBladeChampionRules.IDLE_STALK);
		entityData.set(TELL, 0);
		entityData.set(RIPOSTE_TRIGGERED, false);
		entityData.set(PREVIEW_ELAPSED, -1.0F);
	}
}
