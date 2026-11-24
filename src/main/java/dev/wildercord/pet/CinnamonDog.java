package dev.wildercord.pet;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.SwordMaster;
import dev.wildercord.cast.Targets;
import dev.wildercord.compat.McItemStackWithSlot;
import dev.wildercord.compat.McValueInput;
import dev.wildercord.compat.McValueOutput;
import dev.wildercord.duel.Duels;
import dev.wildercord.party.Parties;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * An immortal little dog bound to the configured owner from the moment she appears. She heals injured party members
 * with her licks, casts elemental magic against nearby foes, can grow permanently large and strong with her Grand Bow,
 * and can carry up to four riders with twin side chests through the sky when wearing her flying harness.
 */
public final class CinnamonDog extends TamableAnimal implements HasCustomInventoryScreen {
	private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> MOOD = net.minecraft.network.syncher.SynchedEntityData.defineId(CinnamonDog.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
	/** Recovery has its own walking pose; it must never reuse the sleeping/sitting pose. */
	public static final int SLEEPING = 1, PLAYING = 2, GREETING = 4, RINGING = 8, TONGUE = 16, BOW = 32, EXHAUSTED = 64, LICKING = 128,
		GRAND_BOW = 256, HARNESS = 512, FLYING = 1024, HIT_PAUSED = 2048;
	/** A lick: her tongue keeps coming out for most of it, longer and longer, then is slurped back in all at once. */
	public static final int LICK_TICKS = 70, LICK_GROW = 56;
	public static final double GROWTH_STEP = 0.5, MAX_GROWTH_SCALE = 3.0;
	public static final int GROWTH_TICKS = 1_200, RECOVERY_TICKS = 600, DAMAGE_IMMUNITY_TICKS = 10, DAMAGE_QUIET_TICKS = 200;
	public static final float DAMAGE_BUDGET = 20;
	/** What each growth step adds while she's big: bite, health, armour, footing and how much she takes before tiring. */
	public static final double BIG_ATTACK = 2, BIG_HEALTH = 5, BIG_ARMOR = 1.5, BIG_FOOTING = 0.1;
	public static final float BIG_BUDGET = 5;
	private static final float MAX_BUDGET = DAMAGE_BUDGET + BIG_BUDGET * (float) ((MAX_GROWTH_SCALE - 1) / GROWTH_STEP) + CinnamonRules.GRAND_BOW_DAMAGE_BUDGET_BONUS;
	/** Her spells: a short windup, then one every few seconds in a fight, and a mending beam when her owner or party is hurt. */
	public static final int CAST_WINDUP = 12, CAST_INTERVAL = CinnamonRules.NORMAL_CAST_INTERVAL_TICKS, HEAL_INTERVAL = 300;
	private static final List<List<dev.wildercord.spell.RuneDef>> SPELLS = List.of(
		List.of(dev.wildercord.spell.Runes.BOLT, dev.wildercord.spell.Runes.FIRE),
		List.of(dev.wildercord.spell.Runes.BOLT, dev.wildercord.spell.Runes.FROST),
		List.of(dev.wildercord.spell.Runes.BOLT, dev.wildercord.spell.Runes.SHOCK),
		List.of(dev.wildercord.spell.Runes.BOLT, dev.wildercord.spell.Runes.HARM));
	private static final List<dev.wildercord.spell.RuneDef> MENDING = List.of(dev.wildercord.spell.Runes.BEAM, dev.wildercord.spell.Runes.HEAL);
	private static final ResourceLocation GROWTH_MODIFIER = Wildercord.id("cinnamon_growth");
	private static final ResourceLocation STRENGTH_MODIFIER = Wildercord.id("cinnamon_strength");
	private static final ResourceLocation GRAND_BOW_MODIFIER = Wildercord.id("cinnamon_grand_bow");
	/** How long a jingle lasts, and when in it each of its three little dings sounds. */
	private static final int RING_TICKS = 12;
	private int settled, playTicks, greeting, ringTicks, tongueTicks, lickTicks, partyLickCooldown;
	/** Client side: how far into the lick she is, counted from when it showed up. */
	public int lickAge;
	private int castCooldown = 40, healCooldown, castWindup, spellIndex;
	private List<dev.wildercord.spell.RuneDef> casting;
	private LivingEntity castTarget;
	private int nextRing = 100 + getRandom().nextInt(300);
	private boolean bow, grandBow, harness, flying, loadingSaddlebags;
	private boolean savedSitting, resizing, feeding, countingDamage;
	private double growthScale = 1;
	private long growthUntil, recoveryUntil, damageImmuneUntil, hitPauseUntil, stateRevision;
	private float damage;
	private final SimpleContainer saddlebags = new SimpleContainer(CinnamonRules.SADDLEBAG_SLOTS) {
		@Override public void setChanged() {
			super.setChanged();
			if (!loadingSaddlebags && level() instanceof ServerLevel) changed();
		}
		@Override public boolean stillValid(Player player) { return canAccessChest(player); }
	};
	public int mood() { return entityData.get(MOOD); }
	@Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder); builder.define(MOOD,0);
	}
	public CinnamonDog(EntityType<? extends CinnamonDog> type, Level level) {
		super(type, level);
		setCustomName(Component.literal("Cinnamon"));
		setCustomNameVisible(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createLivingAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.27)
			.add(Attributes.FLYING_SPEED, CinnamonRules.NORMAL_FLIGHT_SPEED)
			.add(Attributes.ARMOR, 0).add(Attributes.ARMOR_TOUGHNESS, 0).add(Attributes.KNOCKBACK_RESISTANCE, 0)
			.add(Attributes.FOLLOW_RANGE, 32).add(Attributes.ATTACK_DAMAGE, 3);
	}

	void bind(ServerPlayer owner) {
		setTame(true, false);
		setOwnerUUID(owner.getUUID());
		setOrderedToSit(owner.getAttachedOrElse(CinnamonState.SITTING, false));
		bow = owner.getAttachedOrElse(CinnamonState.BOW, false);
		setCustomName(Component.literal("Cinnamon"));
		setCustomNameVisible(true);
		greeting = 60;
	}

	@Override
	public void tick() {
		// Do this before goal selection. A stale entity may be loaded before its owner or journal.
		if (level() instanceof ServerLevel server) {
			// Loaded companions still expire their fixed clocks while the owner is offline.
			updateRecoveryAndGrowth(server);
			if (!CinnamonCompanion.active(this) || isHitPaused()) {
				super.setTarget(null);
				getNavigation().stop();
			}
		}
		super.tick();
		if (!level().isClientSide() && CinnamonCompanion.active(this)) {
			if (partyLickCooldown > 0) partyLickCooldown--;
			if (isOrderedToSit() && playTicks == 0 && greeting == 0) settled++; else settled = 0;
			if (playTicks > 0) playTicks--;
			if (greeting > 0) greeting--;
			ring();
			if (level() instanceof ServerLevel server) {
				autonomousFlight(server);
				partyLick(server);
				magic(server);
			}
			// The tip of her tongue, for a few seconds about every minute and a half (asleep too).
			if (tongueTicks > 0) tongueTicks--;
			else if (getRandom().nextInt(1800) == 0) tongueTicks = 60 + getRandom().nextInt(80);
			lick();
			if (flying && !onGround() && tickCount % 4 == 0 && level() instanceof ServerLevel server) {
				server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.2 * getScale(), getZ(), 2, 0.25, 0.1, 0.25, 0.01);
			}
			if (onGround() && (!isVehicle() || getControllingPassenger() == null) && !shouldAutonomousFly()) {
				flying = false;
				if (isNoGravity()) setNoGravity(false);
			}
			int moodFlags = (!isExhausted() && settled >= 200 ? SLEEPING : 0) | (playTicks > 0 ? PLAYING : 0) | (greeting > 0 ? GREETING : 0)
				| (ringTicks > 0 ? RINGING : 0) | (tongueTicks > 0 ? TONGUE : 0) | (bow ? BOW : 0) | (isExhausted() ? EXHAUSTED : 0)
				| (lickTicks > 0 ? LICKING : 0) | (grandBow ? GRAND_BOW : 0) | (harness ? HARNESS : 0)
				| ((flying || (!onGround() && (isNoGravity() || isVehicle()))) ? FLYING : 0) | (isHitPaused() ? HIT_PAUSED : 0);
			entityData.set(MOOD, moodFlags);
		}
		if (level().isClientSide()) lickAge = (mood() & LICKING) != 0 ? lickAge + 1 : 0;
	}

	@Override public boolean isEffectiveAi() { return super.isEffectiveAi() && CinnamonCompanion.active(this) && !isHitPaused(); }

	/** Vanilla's teleport only checks a small footprint. The registry owns all companion moves. */
	@Override public void tryToTeleportToOwner() {
		if (!isVehicle()) CinnamonCompanion.recallFollowing(this);
	}

	@Override public boolean canUsePortal(boolean allowPassengers) { return false; }

	@Override public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
		if (harness || flying || grandBow) {
			resetFallDistance();
			return false;
		}
		return super.causeFallDamage(fallDistance, damageMultiplier, source);
	}

	private long clock() {
		return level() instanceof ServerLevel server ? server.getServer().overworld().getGameTime() : level().getGameTime();
	}

	public boolean isExhausted() { return level().isClientSide() ? (mood() & EXHAUSTED) != 0 : recoveryUntil > clock(); }
	public boolean isHitPaused() { return level().isClientSide() ? (mood() & HIT_PAUSED) != 0 : CinnamonRules.isHitPaused(clock(), hitPauseUntil); }
	public boolean isFlying() { return level().isClientSide() ? (mood() & FLYING) != 0 : flying; }
	public double growthScale() { return growthScale; }
	public double effectiveScale() { return CinnamonRules.effectiveScale(grandBow, growthScale, growthScale > 1); }
	/** Strength applies to both treat growth and her permanent Grand Bow. */
	public boolean isBig() { return grandBow || growthScale > 1; }
	private double growthSteps() { return Math.max(0, (growthScale - 1) / GROWTH_STEP); }
	/** How much damage tires her: more while she's big or wearing her Grand Bow. */
	public float damageBudget() {
		return DAMAGE_BUDGET + BIG_BUDGET * (float) growthSteps() + (grandBow ? CinnamonRules.GRAND_BOW_DAMAGE_BUDGET_BONUS : 0F);
	}
	public long growthExpiresAt() { return growthUntil; }
	public long recoveryExpiresAt() { return recoveryUntil; }
	public long damageImmuneUntil() { return damageImmuneUntil; }
	public long hitPauseUntil() { return hitPauseUntil; }
	public float accumulatedDamage() { return damage; }
	public boolean savedSitting() { return savedSitting; }
	public boolean wearingBow() { return bow; }
	public boolean wearingGrandBow() { return grandBow; }
	public boolean wearingHarness() { return harness; }
	public SimpleContainer saddlebags() { return saddlebags; }
	List<McItemStackWithSlot> snapshotSaddlebags() {
		List<McItemStackWithSlot> stored = new ArrayList<>();
		for (int i = 0; i < saddlebags.getContainerSize(); i++) {
			ItemStack stack = saddlebags.getItem(i);
			if (!stack.isEmpty()) stored.add(new McItemStackWithSlot(i, stack.copy()));
		}
		return stored;
	}
	long stateRevision() { return stateRevision; }

	private void changed() { stateRevision++; CinnamonCompanion.changed(this); }

	@Override public void setOrderedToSit(boolean sitting) {
		if (savedSitting != sitting) stateRevision++;
		savedSitting = sitting;
		super.setOrderedToSit(sitting && !isExhausted());
	}

	/** A successful whistle changes her saved preference, including while she is recovering. */
	void followOwner() {
		setOrderedToSit(false);
		setInSittingPose(false);
		settled = 0;
		if (getOwner() instanceof ServerPlayer owner) owner.setAttached(CinnamonState.SITTING, false);
		changed();
	}

	private void updateRecoveryAndGrowth(ServerLevel server) {
		long now = clock();
		if (recoveryUntil > 0 && now >= recoveryUntil) {
			recoveryUntil = 0;
			damage = 0;
			super.setOrderedToSit(savedSitting);
			changed();
			ownerMessage("recovered");
		} else if (isExhausted()) {
			super.setOrderedToSit(false);
			setInSittingPose(false);
			super.setTarget(null);
		}
		if (!isExhausted() && damage > 0 && now - damageImmuneUntil >= DAMAGE_QUIET_TICKS - DAMAGE_IMMUNITY_TICKS) {
			damage = 0;
			changed();
		}
		if (growthUntil > 0 && now >= growthUntil) resetGrowth();
		else if (!grandBow && growthScale > 1 && (!server.noCollision(this, getBoundingBox())
			|| CinnamonCompanion.active(this) && onGround() && tickCount % 10 == 0 && !CompanionLanding.safe(server, this, getBoundingBox()))) {
			// Shrinking applies only to temporary food growth; Grand Bow stays permanently large.
			applyGrowth(1);
			changed();
		}
		if (hitPauseUntil > 0 && now >= hitPauseUntil) {
			hitPauseUntil = 0;
			changed();
		}
		if (isHitPaused()) {
			casting = null;
			castTarget = null;
			castWindup = 0;
			lickTicks = 0;
			super.setTarget(null);
			getNavigation().stop();
		}
		LivingEntity target = getTarget();
		if (target != null && !canAttack(target)) { super.setTarget(null); getNavigation().stop(); }
	}

	private void ownerMessage(String key) {
		if (getOwner() instanceof ServerPlayer owner) owner.displayClientMessage(Component.translatable("message.wildercord.cinnamon." + key), true);
	}

	/** Preserve other mods' scale modifiers and preview the exact resulting physical size without mutating the entity. */
	private double scaleAt(double factor) {
		AttributeInstance current = getAttribute(Attributes.SCALE);
		if (current == null) return 1;
		AttributeInstance preview = new AttributeInstance(current.getAttribute(), ignored -> {});
		preview.replaceFrom(current);
		preview.removeModifier(GROWTH_MODIFIER);
		if (factor > 1) preview.addTransientModifier(new AttributeModifier(GROWTH_MODIFIER, factor - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		return preview.getValue();
	}

	private void applyGrowth(double factor) {
		growthScale = factor;
		double physical = CinnamonRules.effectiveScale(grandBow, growthScale, growthScale > 1);
		AttributeInstance scale = getAttribute(Attributes.SCALE);
		if (scale == null) return;
		resizing = true;
		try {
			scale.removeModifier(GROWTH_MODIFIER);
			if (physical > 1) scale.addTransientModifier(new AttributeModifier(GROWTH_MODIFIER, physical - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			refreshDimensions();
		} finally { resizing = false; }
		double steps = growthSteps();
		strengthen(Attributes.ATTACK_DAMAGE, BIG_ATTACK * steps, grandBow ? CinnamonRules.GRAND_BOW_ATTACK_DAMAGE_BONUS : 0);
		strengthen(Attributes.MAX_HEALTH, BIG_HEALTH * steps, grandBow ? CinnamonRules.GRAND_BOW_MAX_HEALTH_BONUS : 0);
		strengthen(Attributes.ARMOR, BIG_ARMOR * steps, grandBow ? CinnamonRules.GRAND_BOW_ARMOR_BONUS : 0);
		strengthen(Attributes.ARMOR_TOUGHNESS, 0, grandBow ? CinnamonRules.GRAND_BOW_ARMOR_TOUGHNESS_BONUS : 0);
		strengthen(Attributes.KNOCKBACK_RESISTANCE, BIG_FOOTING * steps, grandBow ? CinnamonRules.GRAND_BOW_KNOCKBACK_RESISTANCE_BONUS : 0);
		strengthen(Attributes.MOVEMENT_SPEED, 0, grandBow ? CinnamonRules.GRAND_BOW_SPEED_BONUS : 0);
		strengthen(Attributes.FLYING_SPEED, 0, grandBow ? (CinnamonRules.GRAND_BOW_FLIGHT_SPEED - CinnamonRules.NORMAL_FLIGHT_SPEED) : 0);
	}

	private void strengthen(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double growthAmount, double grandAmount) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance == null) return;
		instance.removeModifier(STRENGTH_MODIFIER);
		instance.removeModifier(GRAND_BOW_MODIFIER);
		if (growthAmount > 0) instance.addTransientModifier(new AttributeModifier(STRENGTH_MODIFIER, growthAmount, AttributeModifier.Operation.ADD_VALUE));
		if (grandAmount > 0) instance.addTransientModifier(new AttributeModifier(GRAND_BOW_MODIFIER, grandAmount, AttributeModifier.Operation.ADD_VALUE));
		if (attribute == Attributes.MAX_HEALTH && getHealth() > getMaxHealth()) setHealth(getMaxHealth());
	}

	@Override public boolean fudgePositionAfterSizeChange(EntityDimensions previous) {
		return !resizing && super.fudgePositionAfterSizeChange(previous);
	}

	void resetGrowth() {
		if (growthScale == 1 && growthUntil == 0) return;
		growthUntil = 0;
		applyGrowth(1);
		changed();
	}

	private InteractionResult feed(ServerPlayer player, InteractionHand hand, ItemStack held) {
		if (feeding) return InteractionResult.PASS;
		feeding = true;
		try { return feedOnce(player, hand, held); }
		finally { feeding = false; }
	}

	private InteractionResult feedOnce(ServerPlayer player, InteractionHand hand, ItemStack held) {
		long now = clock();
		if (growthUntil > 0 && now >= growthUntil) resetGrowth();
		if (grandBow || growthScale >= MAX_GROWTH_SCALE) {
			ownerMessage("growth_full");
			return InteractionResult.SUCCESS;
		}
		double next = Math.min(MAX_GROWTH_SCALE, growthScale + GROWTH_STEP);
		double physical = scaleAt(next);
		if (physical <= getScale()) { ownerMessage("growth_full"); return InteractionResult.SUCCESS; }
		AABB proposed = getDimensions(getPose()).scale((float) (physical / getScale())).makeBoundingBox(position());
		AABB before = getBoundingBox();
		var ownerPosition = player.position();
		float previousScale = getScale();
		long revision = stateRevision;
		boolean safe = CompanionLanding.safe((ServerLevel) player.level(), this, proposed);
		// Protection hooks can re-enter, transfer ownership, or replace the hand stack. Fail closed.
		if (!safe || !CinnamonCompanion.active(this) || !isOwnedBy(player) || player.level() != level()
			|| stateRevision != revision || !getBoundingBox().equals(before) || !player.position().equals(ownerPosition)
			|| getScale() != previousScale || scaleAt(next) != physical
			|| player.getItemInHand(hand) != held || held.isEmpty() || !held.is(ItemTags.WOLF_FOOD)) {
			player.displayClientMessage(Component.translatable("message.wildercord.cinnamon.growth_blocked"), true);
			return InteractionResult.SUCCESS;
		}
		if (growthUntil == 0) growthUntil = now + GROWTH_TICKS;
		applyGrowth(next);
		held.consume(1, player);
		settled = 0;
		changed();
		level().broadcastEntityEvent(this, (byte) 7);
		ownerMessage("grow");
		return InteractionResult.SUCCESS;
	}

	/** Her collar bell: three quick, high dings every 20 to 40 seconds, loud enough to find her by from a way off. */
	private void ring() {
		if (ringTicks > 0) {
			ringTicks--;
			int ding = RING_TICKS - 1 - ringTicks;
			if (ding % 3 == 0 && ding < 9) {
				level().playSound(null, getX(), getY() + 0.5, getZ(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL,
					net.minecraft.sounds.SoundSource.NEUTRAL, 1.6F - ding * 0.12F, 1.85F + getRandom().nextFloat() * 0.15F);
			}
			return;
		}
		if (--nextRing <= 0) {
			nextRing = 400 + getRandom().nextInt(400);
			if (CinnamonCompanion.bell()) ringTicks = RING_TICKS;
		}
	}

	/** One spell at a time: choose it, open its circle for {@link #CAST_WINDUP} ticks, then cast if it is still allowed. */
	private void magic(ServerLevel level) {
		if (healCooldown > 0) healCooldown--;
		LivingEntity owner = getOwner();
		if (isExhausted() || isHitPaused()) return;
		if (castWindup > 0) {
			if (--castWindup == 0) release(level, owner);
			return;
		}
		if (--castCooldown > 0) return;
		castCooldown = 20;
		if (!attackReady(owner)) return;
		double range = CinnamonRules.spellRange(grandBow);
		if (isBig() && healCooldown == 0) {
			LivingEntity injured = findMendingPatient(level, owner, range);
			if (injured != null) {
				begin(level, MENDING, injured);
				return;
			}
		}
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive() || !canAttack(target) || distanceTo(target) > range || !hasLineOfSight(target)) {
			target = findPassiveMagicTarget(level, owner, range);
		}
		if (target != null && target.isAlive() && canAttack(target) && distanceTo(target) <= range && hasLineOfSight(target)) {
			begin(level, SPELLS.get(spellIndex++ % SPELLS.size()), target);
		}
	}

	private LivingEntity findMendingPatient(ServerLevel level, LivingEntity owner, double range) {
		if (owner != null && owner.isAlive() && owner.getHealth() < owner.getMaxHealth() * 0.5F
			&& distanceTo(owner) <= range && hasLineOfSight(owner)) {
			return owner;
		}
		Player best = null;
		for (Player candidate : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(range),
			p -> p.isAlive() && !p.isSpectator() && p.getHealth() < p.getMaxHealth() * 0.5F
				&& samePartyPlayer(p) && distanceTo(p) <= range && hasLineOfSight(p))) {
			if (best == null || CinnamonRules.isMoreInjured(candidate.getHealth(), candidate.getMaxHealth(), best.getHealth(), best.getMaxHealth())) {
				best = candidate;
			}
		}
		return best;
	}

	private LivingEntity findPassiveMagicTarget(ServerLevel level, LivingEntity owner, double range) {
		LivingEntity best = null;
		double bestDist = Double.MAX_VALUE;
		for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(range),
			e -> (e instanceof Enemy || e instanceof Player) && e != this && !hasPassenger(e)
				&& canAttack(e) && distanceTo(e) <= range && hasLineOfSight(e))) {
			double dist = distanceToSqr(candidate);
			if (dist < bestDist) {
				bestDist = dist;
				best = candidate;
			}
		}
		return best;
	}

	private void begin(ServerLevel level, List<dev.wildercord.spell.RuneDef> spell, LivingEntity target) {
		casting = spell;
		castTarget = target;
		castWindup = CAST_WINDUP;
		dev.wildercord.cast.Runebound.aimAt(this, target);
		dev.wildercord.cast.Runebound.telegraph(level, this, spell, target, CAST_WINDUP);
	}

	/** The target, her owner and her own state are checked again: anything may have changed during the windup. */
	private void release(ServerLevel level, LivingEntity owner) {
		List<dev.wildercord.spell.RuneDef> spell = casting;
		LivingEntity target = castTarget;
		casting = null;
		castTarget = null;
		double range = CinnamonRules.spellRange(grandBow);
		if (spell == null || target == null || isExhausted() || isHitPaused() || !target.isAlive() || target.level() != level
			|| !attackReady(owner) || distanceTo(target) > range || !hasLineOfSight(target)) return;
		boolean mending = spell == MENDING;
		if (mending ? (!isBig() || !samePartyPlayer(target)) : !canAttack(target)) return;
		dev.wildercord.cast.Runebound.aimAt(this, target);
		dev.wildercord.cast.Runebound.cast(level, this, spell, CinnamonRules.spellPower(grandBow) + (growthScale - 1) * 0.25);
		if (mending) healCooldown = HEAL_INTERVAL;
		castCooldown = CinnamonRules.castIntervalTicks(grandBow);
	}

	private void partyLick(ServerLevel level) {
		if (isExhausted() || isHitPaused() || isOrderedToSit()) return;
		Player patient = findLickPatient(level);
		if (patient == null) return;
		double reach = CinnamonRules.LICK_HEAL_REACH * Math.max(1.0D, effectiveScale() * 0.65D);
		boolean close = hasPassenger(patient) || distanceTo(patient) <= reach;
		if (close && partyLickCooldown == 0) {
			getLookControl().setLookAt(patient, 30.0F, 30.0F);
			startLick();
			float heal = CinnamonRules.lickHealAmount(grandBow, isBig());
			patient.heal(heal);
			patient.addEffect(new MobEffectInstance(MobEffects.REGENERATION, CinnamonRules.LICK_REGEN_DURATION_TICKS, 0, true, true));
			level.sendParticles(ParticleTypes.HEART, patient.getX(), patient.getY() + patient.getBbHeight() * 0.6D, patient.getZ(), 8, 0.35D, 0.35D, 0.35D, 0.02D);
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.NEUTRAL, 0.7F, 1.35F);
			if (patient instanceof ServerPlayer sp) {
				sp.displayClientMessage(Component.translatable("message.wildercord.cinnamon.lick_heal"), true);
			}
			partyLickCooldown = CinnamonRules.LICK_HEAL_COOLDOWN_TICKS;
		} else if (!close && !isVehicle()) {
			getNavigation().moveTo(patient, 1.35D);
		}
	}

	private Player findLickPatient(ServerLevel level) {
		Player best = null;
		double bestDist = Double.MAX_VALUE;
		for (Player candidate : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(CinnamonRules.PARTY_HEAL_SEARCH_RADIUS),
			p -> p.isAlive() && !p.isSpectator() && samePartyPlayer(p)
				&& CinnamonRules.needsLickHeal(p.getHealth(), p.getMaxHealth()))) {
			if (best == null) {
				best = candidate;
				bestDist = distanceToSqr(candidate);
			} else if (CinnamonRules.isMoreInjured(candidate.getHealth(), candidate.getMaxHealth(), best.getHealth(), best.getMaxHealth())) {
				best = candidate;
				bestDist = distanceToSqr(candidate);
			} else if (!CinnamonRules.isMoreInjured(best.getHealth(), best.getMaxHealth(), candidate.getHealth(), candidate.getMaxHealth())
				&& distanceToSqr(candidate) < bestDist) {
				best = candidate;
				bestDist = distanceToSqr(candidate);
			}
		}
		return best;
	}

	private boolean shouldAutonomousFly() {
		if (isVehicle() || isOrderedToSit() || isExhausted() || isHitPaused() || !CinnamonCompanion.active(this)) return false;
		if (!(level() instanceof ServerLevel server)) return false;
		LivingEntity target = findLickPatient(server);
		if (target == null && getOwner() instanceof ServerPlayer owner && CinnamonCompanion.mayActWith(owner, this)) {
			target = owner;
		}
		if (target == null || target.level() != server || distanceToSqr(target) > 64.0D * 64.0D) return false;
		return (target.getY() - getY() > 2.2D) || (target instanceof Player p && (p.isFallFlying() || !p.onGround() && p.getY() > getY() + 1.8D));
	}

	private void autonomousFlight(ServerLevel level) {
		if (isVehicle()) return;
		if (isOrderedToSit() || isExhausted() || isHitPaused()) {
			if (flying) {
				setNoGravity(false);
				if (onGround()) flying = false;
			}
			return;
		}
		LivingEntity target = findLickPatient(level);
		if (target == null && getOwner() instanceof ServerPlayer owner && CinnamonCompanion.mayActWith(owner, this)) {
			target = owner;
		}
		if (target != null && target.level() == level() && distanceToSqr(target) <= 64.0D * 64.0D
			&& ((target.getY() - getY() > 2.2D) || (!target.onGround() && target.getY() > getY() + 1.8D) || (flying && !onGround()))) {
			flying = true;
			setNoGravity(true);
			resetFallDistance();
			Vec3 dest = target.position().add(0.0D, 0.35D, 0.0D);
			Vec3 delta = dest.subtract(position());
			double dist = delta.length();
			if (dist > 1.4D) {
				double speed = CinnamonRules.flightSpeed(grandBow) * 0.55D;
				setDeltaMovement(delta.scale(speed / dist));
				dev.wildercord.cast.Runebound.aimAt(this, target);
			} else {
				setDeltaMovement(getDeltaMovement().scale(0.6D));
			}
		} else if (flying) {
			if (onGround()) {
				flying = false;
				setNoGravity(false);
			} else {
				setNoGravity(true);
				Vec3 cur = getDeltaMovement();
				setDeltaMovement(cur.x * 0.85D, Math.max(-0.22D, cur.y - 0.03D), cur.z * 0.85D);
			}
		}
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3, true));
		goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.15, 8, 2));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.75));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 7));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		// Whoever lays a hand on her owner gets bitten (not while she's sitting, like any tame dog).
		targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
		// While she's big, whatever her owner fights, she joins in.
		targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal(this) {
			@Override public boolean canUse() { return isBig() && super.canUse(); }
			@Override public boolean canContinueToUse() { return isBig() && super.canContinueToUse(); }
		});
	}

	@Override
	public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
		if (!attackReady(owner) || target == owner || target instanceof Creeper || target instanceof Ghast
			|| target instanceof ArmorStand || target instanceof SwordMaster || duellingOwner(target) || hasPassenger(target)) {
			return false;
		}
		if (target instanceof TamableAnimal pet && pet.isTame() && pet.getOwner() == owner) {
			return false;
		}
		return !(target instanceof Player victim && owner instanceof Player master && !master.canHarmPlayer(victim))
			&& Targets.canHarm(owner, target);
	}

	private boolean attackReady(LivingEntity owner) {
		return CinnamonCompanion.active(this) && !isExhausted() && !isHitPaused() && !isOrderedToSit()
			&& owner instanceof ServerPlayer player && getOwner() == owner && owner.level() == level()
			&& CinnamonCompanion.mayActWith(player, this);
	}

	private static boolean duellingOwner(Entity target) {
		LivingEntity responsible = target instanceof Player player ? player
			: target instanceof OwnableEntity pet ? dev.wildercord.compat.McOwnable.rootOwner(pet) : null;
		return responsible instanceof Player player && Duels.inDuel(player);
	}

	@Override public boolean canAttack(LivingEntity target) {
		LivingEntity owner = getOwner();
		return owner != null && wantsToAttack(target, owner) && super.canAttack(target);
	}

	@Override public void setTarget( LivingEntity target) {
		super.setTarget(target == null || canAttack(target) ? target : null);
	}

	/** Final bite sink: a target admitted by an earlier AI tick can become protected in the meantime. */
	@Override public boolean doHurtTarget(Entity target) {
		return target instanceof LivingEntity living && target.level() == level() && canAttack(living)
			&& attackReady(getOwner()) && super.doHurtTarget(target);
	}

	private boolean samePartyPlayer(Entity entity) {
		return entity instanceof Player p && (isOwnedBy(p) || Parties.sameParty(this, p));
	}

	public boolean canAccessChest(Player player) {
		return isAlive() && !isRemoved() && harness && samePartyPlayer(player) && distanceToSqr(player) <= 64.0D;
	}

	private boolean ownerIsRiding() {
		for (Entity passenger : getPassengers()) {
			if (passenger instanceof Player player && isOwnedBy(player)) return true;
		}
		return false;
	}

	@Override
	public void openCustomInventoryScreen(Player player) {
		if (player instanceof ServerPlayer sp && canAccessChest(sp)) {
			sp.openMenu(new SimpleMenuProvider(
				(id, inv, p) -> ChestMenu.sixRows(id, inv, saddlebags),
				Component.translatable("container.wildercord.cinnamon_harness")));
		}
	}

	@Override
	public LivingEntity getControllingPassenger() {
		for (Entity passenger : getPassengers()) {
			if (passenger instanceof Player player && isOwnedBy(player)) return player;
		}
		return null;
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		if (!(passenger instanceof Player player) || isExhausted() || !samePartyPlayer(player)) return false;
		return CinnamonRules.canAcceptRider(harness, getPassengers().size(), isOwnedBy(player), ownerIsRiding());
	}

	@Override
	protected void addPassenger(Entity passenger) {
		super.addPassenger(passenger);
		if (savedSitting) {
			setOrderedToSit(false);
			setInSittingPose(false);
			if (getOwner() instanceof ServerPlayer owner) owner.setAttached(CinnamonState.SITTING, false);
			changed();
		}
	}

	@Override
	protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		if (!level().isClientSide() && !onGround() && passenger instanceof ServerPlayer sp) {
			sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, true));
		}
		if (!isVehicle() && onGround()) {
			flying = false;
			setNoGravity(false);
		}
	}

	private int seatIndexFor(Entity passenger) {
		if (passenger instanceof Player p && isOwnedBy(p)) return 0;
		int slot = 1;
		for (Entity other : getPassengers()) {
			if (other == passenger) return slot;
			if (!(other instanceof Player p && isOwnedBy(p))) slot++;
		}
		return Math.min(CinnamonRules.MAX_RIDERS - 1, slot);
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		int seatIndex = seatIndexFor(passenger);
		CinnamonRules.SeatOffset offset = CinnamonRules.seatOffset(seatIndex, Math.max(1, getPassengers().size()));
		float yScale = isInSittingPose() ? 0.65F : 1.0F;
		float s = getScale();
		return new Vec3(offset.side() * s, offset.up() * s * yScale, offset.forward() * s)
			.yRot(-this.yBodyRot * Mth.DEG_TO_RAD);
	}

	private boolean isClimbInput(Player driver) {
		return ((dev.wildercord.mixin.LivingEntityJumpingAccessor) driver).wildercord$isJumping()
			|| dev.wildercord.compat.McPlayerInput.of(driver).jump();
	}

	@Override
	protected Vec3 getRiddenInput(Player driver, Vec3 travelVector) {
		if (isHitPaused() || isExhausted()) return Vec3.ZERO;
		float strafe = driver.xxa;
		float rawForward = driver.zza;
		if (strafe == 0.0F && rawForward == 0.0F && driver instanceof ServerPlayer sp) {
			var input = dev.wildercord.compat.McPlayerInput.of(sp);
			rawForward = (input.forward() ? 1.0F : 0.0F) - (input.backward() ? 1.0F : 0.0F);
			strafe = (input.left() ? 1.0F : 0.0F) - (input.right() ? 1.0F : 0.0F);
		}
		float up = 0.0F;
		float forward = 0.0F;
		boolean climb = isClimbInput(driver);
		if (rawForward != 0.0F) {
			float pitchRad = driver.getXRot() * Mth.DEG_TO_RAD;
			float cos = Mth.cos(pitchRad);
			float sin = -Mth.sin(pitchRad);
			if (rawForward < 0.0F) {
				cos *= -0.5F;
				sin *= -0.5F;
			}
			forward = cos;
			if (flying || climb || sin > 0.25F) {
				up = sin;
			}
		}
		if (climb) {
			up += 0.6F;
		}
		return new Vec3(strafe, up, forward);
	}

	@Override
	protected void tickRidden(Player driver, Vec3 travelVector) {
		super.tickRidden(driver, travelVector);
		setRot(driver.getYRot(), driver.getXRot() * 0.5F);
		yHeadRot = yBodyRot = yRotO = getYRot();
		if (isHitPaused()) {
			if (flying) {
				setNoGravity(true);
				setDeltaMovement(getDeltaMovement().scale(0.5D));
			}
			return;
		}
		boolean climb = isClimbInput(driver);
		if (climb || travelVector.y > 0.12D || !onGround()) {
			flying = true;
		} else if (onGround() && !climb && travelVector.y <= 0.02D) {
			flying = false;
		}
		setNoGravity(flying);
		if (flying) resetFallDistance();
	}

	@Override
	protected float getRiddenSpeed(Player driver) {
		return (float) (flying ? CinnamonRules.flightSpeed(grandBow) : getAttributeValue(Attributes.MOVEMENT_SPEED));
	}

	@Override
	public void travel(Vec3 travelVector) {
		if (isHitPaused() && flying) {
			setDeltaMovement(getDeltaMovement().scale(0.5D));
			super.travel(Vec3.ZERO);
			return;
		}
		if (flying) {
			setDeltaMovement(travelVector.scale(0.02)); // 1.21.5 has no travelFlying yet
			return;
		}
		super.travel(travelVector);
	}

	private boolean equipGrandBow(Player player, ItemStack held) {
		double targetScale = CinnamonRules.effectiveScale(true, growthScale, growthScale > 1);
		double physical = scaleAt(targetScale);
		if (player instanceof ServerPlayer sp && physical > getScale()) {
			AABB proposed = getDimensions(getPose()).scale((float) (physical / Math.max(0.01D, getScale()))).makeBoundingBox(position());
			if (!CompanionLanding.safe((ServerLevel) sp.level(), this, proposed)) {
				player.displayClientMessage(Component.translatable("message.wildercord.cinnamon.growth_blocked"), true);
				return false;
			}
		}
		if (bow) {
			bow = false;
			player.setAttached(CinnamonState.BOW, false);
			ItemStack oldBow = new ItemStack(CinnamonContent.BOW);
			if (!player.getInventory().add(oldBow) && level() instanceof ServerLevel sl) spawnAtLocation(oldBow.getItem(), 1);
		}
		grandBow = true;
		settled = 0;
		applyGrowth(growthScale);
		setHealth(Math.min(getMaxHealth(), getHealth() + (float) CinnamonRules.GRAND_BOW_MAX_HEALTH_BONUS));
		held.consume(1, player);
		changed();
		level().broadcastEntityEvent(this, (byte) 7);
		level().playSound(null, getX(), getY(), getZ(), SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.NEUTRAL, 0.9F, 1.15F);
		player.displayClientMessage(Component.translatable("message.wildercord.cinnamon.grand_bow_on"), true);
		return true;
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!isOwnedBy(player)) {
			if (!level().isClientSide() && CinnamonCompanion.active(this) && harness && samePartyPlayer(player)) {
				if (player.isShiftKeyDown()) {
					openCustomInventoryScreen(player);
					return InteractionResult.SUCCESS;
				}
				if (canAddPassenger(player)) {
					player.startRiding(this);
					return InteractionResult.SUCCESS;
				}
			}
			if (!isTame() && !level().isClientSide()) player.displayClientMessage(Component.translatable("message.wildercord.cinnamon.owner"), true);
			return InteractionResult.PASS;
		}
		if (!level().isClientSide() && !CinnamonCompanion.active(this)) {
			return InteractionResult.PASS;
		}
		if (!level().isClientSide()) {
			ItemStack held = player.getItemInHand(hand);
			if (held.is(ItemTags.WOLF_FOOD) && player instanceof ServerPlayer owner) return feed(owner, hand, held);
			if (held.is(CinnamonContent.TOY)) {
				playTicks = 50; settled = 0;
				level().broadcastEntityEvent(this, (byte) 7);
				player.displayClientMessage(Component.translatable("message.wildercord.cinnamon.toy"), true);
				return InteractionResult.SUCCESS;
			}
			if (held.is(CinnamonContent.GRAND_BOW) && !grandBow) {
				equipGrandBow(player, held);
				return InteractionResult.SUCCESS;
			}
			if (held.is(CinnamonContent.BOW) && !bow && !grandBow) {
				wearBow(player, true);
				held.consume(1, player);
				player.displayClientMessage(Component.translatable("message.wildercord.cinnamon.bow_on"), true);
				return InteractionResult.SUCCESS;
			}
			if (held.is(CinnamonContent.HARNESS) && !harness) {
				harness = true;
				settled = 0;
				held.consume(1, player);
				changed();
				level().playSound(null, getX(), getY(), getZ(), SoundEvents.HORSE_SADDLE, SoundSource.NEUTRAL, 0.9F, 1.1F);
				player.displayClientMessage(Component.translatable("message.wildercord.cinnamon.harness_on"), true);
				return InteractionResult.SUCCESS;
			}
			if (held.is(Items.SHEARS)) {
				if (grandBow) {
					grandBow = false;
					applyGrowth(growthScale);
					ItemStack ribbon = new ItemStack(CinnamonContent.GRAND_BOW);
					if (!player.getInventory().add(ribbon)) spawnAtLocation(ribbon.getItem(), 1);
					changed();
					level().playSound(null, getX(), getY(), getZ(), SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 0.8F, 1.1F);
					player.displayClientMessage(Component.translatable("message.wildercord.cinnamon.grand_bow_off"), true);
					return InteractionResult.SUCCESS;
				}
				if (bow) {
					wearBow(player, false);
					ItemStack ribbon = new ItemStack(CinnamonContent.BOW);
					if (!player.getInventory().add(ribbon)) spawnAtLocation(ribbon.getItem(), 1);
					player.displayClientMessage(Component.translatable("message.wildercord.cinnamon.bow_off"), true);
					return InteractionResult.SUCCESS;
				}
				if (harness && !isVehicle()) {
					harness = false;
					if (!saddlebags.isEmpty()) {
						Containers.dropContents(level(), blockPosition(), saddlebags);
						saddlebags.clearContent();
					}
					ItemStack gear = new ItemStack(CinnamonContent.HARNESS);
					if (!player.getInventory().add(gear)) spawnAtLocation(gear.getItem(), 1);
					changed();
					level().playSound(null, getX(), getY(), getZ(), SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 0.8F, 1.0F);
					player.displayClientMessage(Component.translatable("message.wildercord.cinnamon.harness_off"), true);
					return InteractionResult.SUCCESS;
				}
			}
			if (player.isShiftKeyDown()) {
				if (harness) {
					openCustomInventoryScreen(player);
					return InteractionResult.SUCCESS;
				}
				playTicks = 25; settled = 0;
				level().broadcastEntityEvent(this, (byte) 7);
				if (lickTicks == 0 && !isExhausted()) {
					startLick();
					player.displayClientMessage(Component.translatable("message.wildercord.cinnamon.lick"), true);
					return InteractionResult.SUCCESS;
				}
				player.displayClientMessage(Component.translatable("message.wildercord.cinnamon.pet"), true);
				return InteractionResult.SUCCESS;
			}
			if (harness && !isExhausted() && canAddPassenger(player)) {
				player.startRiding(this);
				return InteractionResult.SUCCESS;
			}
			if (isExhausted()) {
				ownerMessage("rest_follow");
				return InteractionResult.SUCCESS;
			}
			setOrderedToSit(!isOrderedToSit());
			settled = 0;
			player.setAttached(CinnamonState.SITTING, isOrderedToSit());
			changed();
			player.displayClientMessage(Component.translatable(isOrderedToSit() ? "message.wildercord.cinnamon.sit" : "message.wildercord.cinnamon.follow"), true);
			getNavigation().stop();
		}
		return InteractionResult.SUCCESS;
	}

	/** She licks: out comes her tongue, and it just keeps growing. */
	public void startLick() {
		lickTicks = LICK_TICKS;
		settled = 0;
	}

	/** A lick under way: a wet little lap every few ticks while it grows, then one big slurp as it all comes back. Now and then she licks her owner unasked. */
	private void lick() {
		if (lickTicks > 0) {
			int age = LICK_TICKS - lickTicks--;
			if (age < LICK_GROW && age % 8 == 0) {
				level().playSound(null, getX(), getEyeY(), getZ(), net.minecraft.sounds.SoundEvents.SLIME_SQUISH_SMALL,
					net.minecraft.sounds.SoundSource.NEUTRAL, 0.5F, 1.6F + age * 0.01F);
			}
			if (age == LICK_GROW) {
				level().playSound(null, getX(), getEyeY(), getZ(), net.minecraft.sounds.SoundEvents.GENERIC_DRINK.value(),
					net.minecraft.sounds.SoundSource.NEUTRAL, 0.9F, 1.5F);
			}
			return;
		}
		if (getOwner() instanceof Player owner && !isExhausted() && !isOrderedToSit() && distanceToSqr(owner) < 6.25 && getRandom().nextInt(2400) == 0) startLick();
	}

	/** Sticks her tongue out for {@code ticks} and jingles her bell now (for the game tests' pictures). */
	public void showOff(int ticks) {
		tongueTicks = ticks;
		ringTicks = RING_TICKS;
	}

	/** Puts her bow on or takes it off, remembered with her owner. */
	private void wearBow(Player player, boolean on) {
		bow = on;
		settled = 0;
		player.setAttached(CinnamonState.BOW, on);
		changed();
		level().playSound(null, getX(), getY(), getZ(), on ? net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER.value() : net.minecraft.sounds.SoundEvents.SHEEP_SHEAR,
			net.minecraft.sounds.SoundSource.NEUTRAL, 0.8F, 1.3F);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		// Operators can deliberately remove her. Ordinary hazards never enter the health-loss path.
		if (source.is(DamageTypes.GENERIC_KILL)) return super.hurt( source,  amount);
		if (countingDamage) return false;
		countingDamage = true;
		try { return countDamage(((ServerLevel) level()), source, amount); }
		finally { countingDamage = false; }
	}

	private boolean countDamage(ServerLevel level, DamageSource source, float amount) {
		long now = clock();
		if (!CinnamonCompanion.active(this) || level != level() || !isAlive() || !Float.isFinite(amount) || amount <= 0
			|| isInvulnerableTo(level, source) || isExhausted() || now < damageImmuneUntil) return false;
		long revision = stateRevision;
		LivingEntity owner = getOwner();
		// An immortal override must still honor the same protection event as vanilla hurtServer.
		if (!ServerLivingEntityEvents.ALLOW_DAMAGE.invoker().allowDamage(this, source, amount)
			|| !CinnamonCompanion.active(this) || level != level() || getOwner() != owner || stateRevision != revision
			|| isExhausted() || clock() < damageImmuneUntil || !isAlive()) return false;
		if (now - damageImmuneUntil >= DAMAGE_QUIET_TICKS - DAMAGE_IMMUNITY_TICKS) damage = 0;
		damage = Math.min(damageBudget(), damage + amount);
		damageImmuneUntil = now + DAMAGE_IMMUNITY_TICKS;
		Entity attacker = source.getEntity();
		if (attacker == null) attacker = source.getDirectEntity();
		boolean self = attacker == this;
		boolean passenger = attacker != null && hasPassenger(attacker);
		boolean ally = attacker != null && (attacker == owner || Parties.sameParty(this, attacker) || (owner != null && Targets.isAlly(owner, attacker)));
		if (CinnamonRules.shouldPauseFromAttack(self, ally, passenger)) {
			hitPauseUntil = now + CinnamonRules.rollHitPauseTicks(getRandom().nextInt(CinnamonRules.HIT_PAUSE_MAX_TICKS - CinnamonRules.HIT_PAUSE_MIN_TICKS + 1));
			casting = null;
			castTarget = null;
			castWindup = 0;
			lickTicks = 0;
			super.setTarget(null);
			getNavigation().stop();
		}
		hurtDuration = hurtTime = 10;
		level.broadcastDamageEvent(this, source);
		if (damage >= damageBudget()) {
			recoveryUntil = now + RECOVERY_TICKS;
			super.setTarget(null);
			super.setOrderedToSit(false);
			setInSittingPose(false);
			getNavigation().stop();
			settled = playTicks = greeting = 0;
			entityData.set(MOOD, (mood() & ~SLEEPING) | EXHAUSTED | (grandBow ? GRAND_BOW : 0) | (harness ? HARNESS : 0));
			ownerMessage("exhausted");
		}
		changed();
		return true;
	}

	@Override
	public boolean isFood(ItemStack stack) { return stack.is(ItemTags.WOLF_FOOD); }

	@Override
	public boolean canMate(net.minecraft.world.entity.animal.Animal partner) { return false; }

	@Override
	public  AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) { return null; }

	@Override
	public boolean canBeLeashed() { return false; }

	@Override
	public boolean shouldBeSaved() { return super.shouldBeSaved(); }

	/** Canonical journal state wins over an older chunk snapshot. Deadlines never restart on recreation or recall. */
	void restoreState(boolean sitting, boolean wearingBow, boolean wearingGrandBow, boolean wearingHarness,
			double scale, long growthUntil, float damage, long recoveryUntil, long damageImmuneUntil,
			long hitPauseUntil, List<McItemStackWithSlot> bagItems) {
		long now = clock();
		savedSitting = sitting;
		bow = wearingBow && !wearingGrandBow;
		grandBow = wearingGrandBow;
		harness = wearingHarness;
		this.growthUntil = boundedDeadline(growthUntil, now, GROWTH_TICKS);
		this.recoveryUntil = boundedDeadline(recoveryUntil, now, RECOVERY_TICKS);
		this.damageImmuneUntil = Math.clamp(damageImmuneUntil, 0, now + DAMAGE_IMMUNITY_TICKS);
		this.hitPauseUntil = boundedDeadline(hitPauseUntil, now, CinnamonRules.HIT_PAUSE_MAX_TICKS);
		this.damage = Float.isFinite(damage) ? Math.clamp(damage, 0, MAX_BUDGET) : 0;
		if (this.recoveryUntil == 0 && (recoveryUntil > 0 || now - this.damageImmuneUntil >= DAMAGE_QUIET_TICKS - DAMAGE_IMMUNITY_TICKS)) this.damage = 0;
		applyGrowth(this.growthUntil > 0 && Double.isFinite(scale) ? Math.clamp(scale, 1, MAX_GROWTH_SCALE) : 1);
		loadingSaddlebags = true;
		try {
			saddlebags.clearContent();
			if (bagItems != null) {
				for (McItemStackWithSlot entry : bagItems) {
					if (entry != null && entry.isValidInContainer(saddlebags.getContainerSize())) {
						saddlebags.setItem(entry.slot(), entry.stack().copy());
					}
				}
			}
		} finally {
			loadingSaddlebags = false;
		}
		super.setOrderedToSit(savedSitting && !isExhausted());
		if (isExhausted()) { super.setTarget(null); setInSittingPose(false); }
		entityData.set(MOOD, (bow ? BOW : 0) | (grandBow ? GRAND_BOW : 0) | (harness ? HARNESS : 0)
			| (isExhausted() ? EXHAUSTED : 0) | (isHitPaused() ? HIT_PAUSED : 0));
		stateRevision++;
	}

	private static long boundedDeadline(long deadline, long now, int maximum) {
		return deadline <= now ? 0 : Math.min(deadline, now + maximum);
	}

	@Override public void addAdditionalSaveData(CompoundTag outputTag) { McValueOutput output = new McValueOutput(outputTag, (level() == null ? RegistryAccess.EMPTY : level().registryAccess()));
		super.addAdditionalSaveData(outputTag);
		output.putBoolean("CinnamonSitting", savedSitting);
		output.putBoolean("CinnamonBow", bow);
		output.putBoolean("CinnamonGrandBow", grandBow);
		output.putBoolean("CinnamonHarness", harness);
		output.putDouble("CinnamonScale", growthScale);
		output.putLong("CinnamonGrowthUntil", growthUntil);
		output.putFloat("CinnamonDamage", damage);
		output.putLong("CinnamonRecoveryUntil", recoveryUntil);
		output.putLong("CinnamonDamageImmuneUntil", damageImmuneUntil);
		output.putLong("CinnamonHitPauseUntil", hitPauseUntil);
		output.store("CinnamonSaddlebags", McItemStackWithSlot.CODEC.listOf(), snapshotSaddlebags());
	}

	@Override public void readAdditionalSaveData(CompoundTag inputTag) { McValueInput input = new McValueInput(inputTag, (level() == null ? RegistryAccess.EMPTY : level().registryAccess()));
		super.readAdditionalSaveData(inputTag);
		restoreState(
			input.getBooleanOr("CinnamonSitting", isOrderedToSit()),
			input.getBooleanOr("CinnamonBow", false),
			input.getBooleanOr("CinnamonGrandBow", false),
			input.getBooleanOr("CinnamonHarness", false),
			input.getDoubleOr("CinnamonScale", 1),
			input.getLongOr("CinnamonGrowthUntil", 0),
			input.getFloatOr("CinnamonDamage", 0),
			input.getLongOr("CinnamonRecoveryUntil", 0),
			input.getLongOr("CinnamonDamageImmuneUntil", 0),
			input.getLongOr("CinnamonHitPauseUntil", 0),
			input.read("CinnamonSaddlebags", McItemStackWithSlot.CODEC.listOf()).orElse(List.of()));
	}
}
