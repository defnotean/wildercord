package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.AuraVfx;
import dev.wildercord.aura.BreathingMethod;
import dev.wildercord.aura.BreathingMethods;
import dev.wildercord.aura.Crescents;
import dev.wildercord.cast.AuraElements;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Fx;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Sigils;
import dev.wildercord.cast.SpellDefence;
import dev.wildercord.cast.SpellDefenceRules;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.config.Config;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.monster.MonsterMagic;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

/**
 * What a duelist and a fallen knight share: a swordsman who fights with aura. Each carries a breathing method (its colour
 * and element) and a stage, and fights with what that stage brings, mirroring a player's techniques with tells a player can
 * read:
 * <ul>
 *   <li><b>coated blows</b>: its melee lands a tenth harder, sparking in its colour;</li>
 *   <li><b>a guard</b>: raised, it halves blows and projectiles from in front; its first moments are a perfect guard (a
 *       parry's timing) that turns a blow whole and staggers whoever struck, and sends a slash back. An axe breaks it. Struck
 *       from behind, it's no guard at all;</li>
 *   <li><b>a slash</b>: the blade raised high (the tell) while the aim settles, fixed a moment before it flies, then a crescent
 *       of aura through the spell defences (see {@link Crescents}).</li>
 * </ul>
 * What it is doing is synced in one byte of pose flags, eased on the client for its model and the glow along its blade.
 */
public abstract class AuraFighter extends PathfinderMob implements Crescents.Guarding {
	/** Its blade raised high: a slash is coming. */
	public static final int WINDUP = 1;
	/** Its guard up. */
	public static final int GUARD = 2;
	/** Caught out (its blow turned, its guard broken): the moment to strike back. */
	public static final int STAGGER = 4;
	/** A bow, at a duel's end. */
	public static final int BOW = 8;
	/** On one knee: a duelist that yields. */
	public static final int YIELD = 16;
	/** Its blade drawn (a duelist's stays sheathed until a duel). */
	public static final int DRAWN = 32;
	/** Sitting by its campfire. */
	public static final int SIT = 64;
	/** A dash (a duelist's, from Form). */
	public static final int DASH = 128;
	private static final int FLAGS = 8;
	/** How fast each pose eases on the client per tick: a tell snaps in, a bow and a sit settle slowly. */
	private static final float[] EASE = {0.3F, 0.4F, 0.25F, 0.1F, 0.12F, 0.35F, 0.08F, 0.5F};

	private static final EntityDataAccessor<Byte> DATA_POSE = SynchedEntityData.defineId(AuraFighter.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> DATA_METHOD = SynchedEntityData.defineId(AuraFighter.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> DATA_STAGE = SynchedEntityData.defineId(AuraFighter.class, EntityDataSerializers.BYTE);
	private static final Identifier COAT = Wildercord.id("aura_coat");

	private final float[] pose = new float[FLAGS];
	private final float[] poseO = new float[FLAGS];

	/** When its guard went up (its perfect moment runs from here), when it drops, and when another can rise. */
	protected long guardRaised = -1;
	protected long guardUntil = -1;
	protected long guardReadyAt;
	/** When the slash being wound up leaves the blade (0: none), when the next may, its target and its fixed aim. */
	protected long slashAt;
	protected long nextSlashAt;
	protected LivingEntity slashTarget;
	protected Vec3 slashAim;
	/** When it recovers from a stagger. */
	protected long staggerUntil;

	protected AuraFighter(EntityType<? extends AuraFighter> type, Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_POSE, (byte) 0);
		builder.define(DATA_METHOD, (byte) 0);
		builder.define(DATA_STAGE, (byte) AuraRules.GLOW);
	}

	// ------------------------------------------------------------------ what it is

	/** Its breathing method (one of the ten built in). */
	public BreathingMethod method() {
		int i = entityData.get(DATA_METHOD);
		return BreathingMethods.BUILT_IN.get(Math.floorMod(i, BreathingMethods.BUILT_IN.size()));
	}

	public void setMethod(BreathingMethod method) {
		int i = BreathingMethods.BUILT_IN.indexOf(method);
		entityData.set(DATA_METHOD, (byte) Math.max(0, i));
	}

	/** Its stage of aura (Glow and up). */
	public int stage() {
		return AuraRules.clampStage(Math.max(AuraRules.GLOW, entityData.get(DATA_STAGE)));
	}

	public void setStage(int stage) {
		entityData.set(DATA_STAGE, (byte) AuraRules.clampStage(Math.max(AuraRules.GLOW, stage)));
	}

	/** Its aura's colour now (0xRRGGBB). */
	public int auraColor() {
		return method().color(stage());
	}

	public String element() {
		return method().element();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("method", method().id());
		output.putInt("aura_stage", stage());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setMethod(BreathingMethods.byId(input.getStringOr("method", "")).orElse(BreathingMethods.EMBER));
		setStage(input.getIntOr("aura_stage", AuraRules.GLOW));
	}

	// ------------------------------------------------------------------ poses

	/** Turns one of the pose flags on or off (server side; clients see it a moment later). */
	protected void setState(int flag, boolean on) {
		byte state = entityData.get(DATA_POSE);
		byte next = (byte) (on ? state | flag : state & ~flag);
		if (next != state) {
			entityData.set(DATA_POSE, next);
		}
	}

	/** Holds one without AI in a pose, its flags exactly these (the tests' and the guide's pictures). */
	public void holdPose(int flags) {
		entityData.set(DATA_POSE, (byte) flags);
	}

	/** Whether a pose flag is on, on either side. */
	public boolean state(int flag) {
		return (entityData.get(DATA_POSE) & flag) != 0;
	}

	/** A pose on the client, eased: 0 off, 1 fully on. */
	public float pose(int flag, float partial) {
		int i = Integer.numberOfTrailingZeros(flag);
		return Mth.lerp(partial, poseO[i], pose[i]);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			for (int i = 0; i < FLAGS; i++) {
				poseO[i] = pose[i];
				pose[i] = Mth.approach(pose[i], state(1 << i) ? 1 : 0, EASE[i]);
			}
		}
	}

	// ------------------------------------------------------------------ coated blows

	/** Its blows are coated: a tenth harder, as a player's at Glow. Kept as a modifier, so its weapon's own damage counts under it. */
	protected void coat() {
		AttributeInstance attack = getAttribute(Attributes.ATTACK_DAMAGE);
		if (attack != null && attack.getModifier(COAT) == null) {
			attack.addOrUpdateTransientModifier(new AttributeModifier(COAT, AuraRules.COAT_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		coat();
		boolean hit = super.doHurtTarget(level, target);
		if (hit) {
			Vec3 at = target.getBoundingBox().getCenter();
			Motes.burst(level, at, 4, AuraVfx.hot(auraColor(), 0.3), 0.07, 12, 0.08);
			Fx.send(level, ParticleTypes.CRIT, at, 4, 0.2, 0.15);
		}
		return hit;
	}

	// ------------------------------------------------------------------ the guard

	public boolean guarding() {
		return guardRaised >= 0 && level().getGameTime() <= guardUntil;
	}

	/** Whether its guard is in its perfect moment now. */
	public boolean perfectNow() {
		return guarding() && AuraWorldRules.perfect(guardRaised, level().getGameTime());
	}

	/** Raises its guard (the tell is the pose, a ring of its light braced in front, and the guard's hum). */
	public boolean raiseGuard() {
		long now = level().getGameTime();
		if (guarding() || now < guardReadyAt || slashAt > 0 || now < staggerUntil) {
			return false;
		}
		guardRaised = now;
		guardUntil = now + AuraWorldRules.MOB_GUARD_TICKS;
		setState(GUARD, true);
		getNavigation().stop();
		if (level() instanceof ServerLevel level) {
			Vec3 look = flatLook();
			Vec3 at = position().add(0, 1.1, 0).add(look.scale(0.8));
			Light.ring(level, at, look, auraColor(), 0.2, 0.9, 0.06, 8);
			Light.ring(level, at, look, AuraVfx.hot(auraColor(), 0.5), 0.1, 0.6, 0.03, 6);
			Feels.sound(level, position().add(0, 1, 0), "aura_guard", 0.8F, 0.95F);
		}
		swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
		return true;
	}

	/** Lets its guard drop (and rest before the next). */
	public void dropGuard() {
		if (guardRaised >= 0) {
			guardRaised = -1;
			guardUntil = -1;
			guardReadyAt = level().getGameTime() + AuraWorldRules.MOB_GUARD_REST;
		}
		setState(GUARD, false);
	}

	/** Its guard's tick: it drops on time. */
	protected void tickGuard(long now) {
		if (guardRaised >= 0 && now > guardUntil) {
			dropGuard();
		}
	}

	/** The way it faces, flat. */
	protected Vec3 flatLook() {
		Vec3 look = getViewVector(1.0F);
		Vec3 flat = new Vec3(look.x, 0, look.z);
		return flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
	}

	/** Whether {@code from} is in front of it (its guard's half). */
	protected boolean facing(Vec3 from) {
		Vec3 toward = from.subtract(position());
		Vec3 look = flatLook();
		return toward.horizontalDistanceSqr() < 1.0E-4 || look.x * toward.x + look.z * toward.z > 0;
	}

	@Override
	public boolean catches(Crescents.Flight flight) {
		return guarding() && facing(flight.caster().position());
	}

	/**
	 * What reaches it through its guard: the whole blow (no guard, from behind, or what a guard can't meet), {@code -1} for a
	 * perfect guard (nothing lands), and otherwise half. An axe's blow breaks it instead.
	 */
	protected float guarded(ServerLevel level, DamageSource source, float damage) {
		if (!guarding() || damage <= 0 || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.IS_EXPLOSION)) {
			return damage;
		}
		Entity direct = source.getDirectEntity();
		Vec3 from = direct != null ? direct.position() : source.getSourcePosition();
		if (from == null || !facing(from)) {
			return damage;
		}
		if (source.getEntity() instanceof Player player && direct == player && player.getMainHandItem().is(ItemTags.AXES)) {
			guardBroken(level, player);
			return damage;
		}
		if (perfectNow()) {
			perfect(level, source);
			return -1;
		}
		Vec3 look = flatLook();
		Vec3 at = position().add(0, 1.1, 0).add(look.scale(0.8));
		Light.ring(level, at, look, auraColor(), 0.1, 1.1, 0.05, 6);
		Fx.send(level, ParticleTypes.CRIT, at, 4, 0.2, 0.15);
		return (float) AuraWorldRules.throughGuard(damage);
	}

	/** A perfect guard: a blow turned whole and its striker staggered, a projectile bounced off, a slash sent back. */
	protected void perfect(ServerLevel level, DamageSource source) {
		long now = level.getGameTime();
		// The perfect moment answers once.
		guardRaised = now - AuraWorldRules.MOB_PERFECT_TICKS - 1;
		if (source.is(Aura.DAMAGE)) {
			Crescents.reflect(this, auraColor(), mayCut(), cutter());
		} else if (!(source.getDirectEntity() instanceof Projectile) && source.getEntity() instanceof LivingEntity attacker && attacker != this) {
			stagger(attacker);
		}
		Vec3 look = flatLook();
		Vec3 at = position().add(0, 1.1, 0).add(look.scale(0.8));
		Sigils.flash(level, at, 0xFF000000 | 0xFFD54A, 1.8F);
		Light.ring(level, at, look, 0xFFD54A, 0.2, 2.2, 0.08, 9);
		Light.ring(level, at, look, auraColor(), 0.1, 1.5, 0.05, 7);
		Fx.send(level, ParticleTypes.WAX_OFF, at, 12, 0.3, 0.4);
		Feels.sound(level, at, "aura_perfect_guard", 1.0F, 1.0F);
		Fx.sound(level, position(), WildercordSounds.SHIELD_PARRY, 0.8F, 1.2F);
	}

	/** Whoever struck into its perfect guard: thrown back, slowed and weakened a moment, their swing spent. */
	protected void stagger(LivingEntity attacker) {
		MonsterMagic.knock(attacker, position(), 0.7);
		attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, AuraWorldRules.MOB_STAGGER_TICKS, 1, false, true), this);
		attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, AuraWorldRules.MOB_STAGGER_TICKS, 0, false, true), this);
		if (attacker instanceof Player player) {
			player.resetAttackStrengthTicker();
			MonsterMagic.tell(player, Component.translatable("message.wildercord.aura_world.turned", getDisplayName()).withColor(0xFFD54A));
		}
		onStaggered(attacker);
	}

	/** Its perfect guard staggered {@code attacker} (slowed and weakened): a duelist notes it, to take off when its duel ends. */
	protected void onStaggered(LivingEntity attacker) {}

	/** An axe met its guard: it breaks, and the guard is open a while (a knight's is dazed; see the subclasses). */
	protected void guardBroken(ServerLevel level, Player by) {
		dropGuard();
		guardReadyAt = level.getGameTime() + AuraWorldRules.KNIGHT_BROKEN_TICKS;
		staggerUntil = level.getGameTime() + AuraWorldRules.KNIGHT_BROKEN_TICKS / 2;
		setState(STAGGER, true);
		Vec3 at = position().add(0, 1.1, 0).add(flatLook().scale(0.7));
		Fx.send(level, ParticleTypes.CRIT, at, 10, 0.35, 0.3);
		Motes.burst(level, at, 8, auraColor(), 0.09, 16, 0.18);
		Fx.sound(level, at, net.minecraft.sounds.SoundEvents.SHIELD_BREAK.value(), 1.0F, 0.9F);
		MonsterMagic.tell(by, Component.translatable("message.wildercord.aura_world.guard_broken", getDisplayName()).withColor(0xE8D8B0));
	}

	// ------------------------------------------------------------------ the slash

	/** Its slash: what it deals, how fast, far and wide it flies. */
	protected abstract double slashDamage();

	protected abstract double slashSpeed();

	protected abstract double slashRange();

	protected abstract double slashWidth();

	/** What its crescents (and the ones its guard sends back) may harm. */
	protected abstract Predicate<Entity> mayCut();

	/** What its crescent does to a creature it reaches: projected aura through the spell defences. */
	protected Crescents.Cut cutter() {
		return (flight, target) -> projected(target, flight.damage());
	}

	/** Raises its blade for a slash at {@code target}, landing in {@code windup} ticks (the tell). */
	public void windUp(LivingEntity target, int windup, int cooldown) {
		long now = level().getGameTime();
		slashAt = now + windup;
		nextSlashAt = now + windup + cooldown;
		slashTarget = target;
		slashAim = null;
		dropGuard();
		setState(WINDUP, true);
		getNavigation().stop();
		if (level() instanceof ServerLevel level) {
			windupSound(level);
			Motes.glows(level, position().add(0, 2.2, 0), 4, 0.25, AuraVfx.hot(auraColor(), 0.3), 0.08, 14, new Vec3(0, 0.02, 0), 0.01);
		}
	}

	/** The sound of its tell. */
	protected void windupSound(ServerLevel level) {
		Feels.sound(level, position().add(0, 1.5, 0), "aura_breath", 0.9F, Feels.step(3));
	}

	/** The slash under way: face the target, fix the aim a moment before (and show the line it will fly), then loose it. */
	protected void tickSlash(ServerLevel level, long now) {
		if (slashAt <= 0) {
			return;
		}
		getNavigation().stop();
		LivingEntity target = slashTarget;
		if (target != null && target.isAlive()) {
			faceTarget(target);
			if (slashAim == null && now >= slashAt - AuraWorldRules.SLASH_LOCK) {
				slashAim = target.getBoundingBox().getCenter().subtract(slashOrigin());
				Vec3 flat = new Vec3(slashAim.x, 0, slashAim.z);
				if (flat.lengthSqr() > 1.0E-4) {
					// The line it will fly, along the ground: step off it.
					Vec3 dir = flat.normalize();
					Vec3 feet = position().add(0, 0.08, 0);
					Light.ray(level, feet.add(dir.scale(1.2)), feet.add(dir.scale(Math.min(slashRange(), 9))), AuraVfx.hot(auraColor(), 0.2), 0.05,
						AuraWorldRules.SLASH_LOCK + 2);
				}
			}
		}
		if (now % 3 == 0) {
			Motes.glows(level, position().add(0, 2.1, 0), 2, 0.3, AuraVfx.hot(auraColor(), 0.4), 0.08, 10, new Vec3(0, 0.02, 0), 0.01);
		}
		if (now >= slashAt) {
			slashAt = 0;
			setState(WINDUP, false);
			release(level);
		}
	}

	/** Where its crescent leaves from: chest height, in front. */
	protected Vec3 slashOrigin() {
		return getEyePosition().subtract(0, 0.45, 0);
	}

	/** The crescent leaves the blade. */
	protected void release(ServerLevel level) {
		Vec3 origin = slashOrigin();
		Vec3 aim = slashAim != null ? slashAim : getViewVector(1.0F);
		slashAim = null;
		Vec3 flat = new Vec3(aim.x, aim.y * 0.6, aim.z);
		if (flat.lengthSqr() < 1.0E-4) {
			flat = flatLook();
		}
		flat = flat.normalize();
		swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
		Feels.sound(level, origin, "aura_slash", 1.0F, 0.92F);
		AuraVfx.slashStart(level, origin, flat, auraColor());
		Crescents.launch(this, origin, flat, auraColor(), slashDamage(), 1.0, slashSpeed(), slashRange(), slashWidth(), AuraRules.SLASH_TARGETS, false,
			mayCut(), cutter());
		afterSlash(level);
	}

	/** After its slash leaves (a knight is open a moment). */
	protected void afterSlash(ServerLevel level) {}

	/** Turns to face a creature (before an aimed attack goes off, so it flies true). */
	protected void faceTarget(LivingEntity target) {
		double dx = target.getX() - getX();
		double dz = target.getZ() - getZ();
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		setYRot(yaw);
		setYHeadRot(yaw);
		setYBodyRot(yaw);
		getLookControl().setLookAt(target, 60, 60);
	}

	/**
	 * Aura off its blade landing on {@code target}: its element as a spell of that element would meet it, held to the
	 * spell-defence cap against a player, through the spell defences (armour, Warding, the spellguard). Returns what it took.
	 */
	public float projected(LivingEntity target, double damage) {
		if (!(level() instanceof ServerLevel level)) {
			return 0;
		}
		DamageSource source = level.damageSources().source(Aura.DAMAGE, this, this);
		double bonus = AuraElements.bonus(this, target, source, element());
		double amount = target instanceof Player ? damage * SpellDefenceRules.capBonus(bonus, Config.get().defence().maxBonus()) : damage * bonus;
		float before = target.getHealth();
		Effects.readyToHurt(target);
		SpellDefence.hurt(level, target, source, (float) amount);
		return Math.max(0, before - Math.max(0, target.getHealth()));
	}

	/** Whether it's caught out right now (staggered or its guard broken). */
	public boolean staggered() {
		return level().getGameTime() < staggerUntil || hasEffect(MobEffects.SLOWNESS) && getEffect(MobEffects.SLOWNESS).getAmplifier() >= 2;
	}

	/** Keeps its stagger pose current. */
	protected void tickStagger(long now) {
		boolean caught = staggered();
		if (caught && slashAt > 0) {
			// Caught out mid-tell: the slash is lost.
			slashAt = 0;
			slashAim = null;
			setState(WINDUP, false);
		}
		setState(STAGGER, caught);
	}

	/** A line above a player's hotbar, from it. */
	protected static void say(ServerPlayer player, Component line) {
		player.sendOverlayMessage(line);
	}
}
