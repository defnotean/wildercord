package dev.wildercord.pet;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.SwordMaster;
import dev.wildercord.cast.Targets;
import dev.wildercord.duel.Duels;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * An immortal little dog bound to the configured owner from the moment she appears. She goes after anyone who hurts her
 * owner, her collar bell jingles every so often so you can find her by ear, now and then she pokes the tip of her tongue
 * out, and she can wear a bow. Fed until she's big, she grows stronger and remembers a little magic: she joins her
 * owner's fights, throws fire, frost, lightning and arcane bolts at their enemies, and mends her owner when they're badly hurt.
 */
public final class CinnamonDog extends TamableAnimal {
	private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> MOOD = net.minecraft.network.syncher.SynchedEntityData.defineId(CinnamonDog.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
	/** Recovery has its own walking pose; it must never reuse the sleeping/sitting pose. */
	public static final int SLEEPING = 1, PLAYING = 2, GREETING = 4, RINGING = 8, TONGUE = 16, BOW = 32, EXHAUSTED = 64;
	public static final double GROWTH_STEP = 0.5, MAX_GROWTH_SCALE = 3.0;
	public static final int GROWTH_TICKS = 1_200, RECOVERY_TICKS = 600, DAMAGE_IMMUNITY_TICKS = 10, DAMAGE_QUIET_TICKS = 200;
	public static final float DAMAGE_BUDGET = 20;
	/** What each growth step adds while she's big: bite, health, armour, footing and how much she takes before tiring. */
	public static final double BIG_ATTACK = 2, BIG_HEALTH = 5, BIG_ARMOR = 1.5, BIG_FOOTING = 0.1;
	public static final float BIG_BUDGET = 5;
	private static final float MAX_BUDGET = DAMAGE_BUDGET + BIG_BUDGET * (float) ((MAX_GROWTH_SCALE - 1) / GROWTH_STEP);
	/** Her spells: a short windup, then one every few seconds in a fight, and a mending beam when her owner is hurt. */
	public static final int CAST_WINDUP = 12, CAST_INTERVAL = 80, HEAL_INTERVAL = 300;
	private static final double SPELL_RANGE = 20, SPELL_POWER = 1.0;
	private static final java.util.List<java.util.List<dev.wildercord.spell.RuneDef>> SPELLS = java.util.List.of(
		java.util.List.of(dev.wildercord.spell.Runes.BOLT, dev.wildercord.spell.Runes.FIRE),
		java.util.List.of(dev.wildercord.spell.Runes.BOLT, dev.wildercord.spell.Runes.FROST),
		java.util.List.of(dev.wildercord.spell.Runes.BOLT, dev.wildercord.spell.Runes.SHOCK),
		java.util.List.of(dev.wildercord.spell.Runes.BOLT, dev.wildercord.spell.Runes.HARM));
	private static final java.util.List<dev.wildercord.spell.RuneDef> MENDING = java.util.List.of(dev.wildercord.spell.Runes.BEAM, dev.wildercord.spell.Runes.HEAL);
	private static final Identifier GROWTH_MODIFIER = Wildercord.id("cinnamon_growth");
	private static final Identifier STRENGTH_MODIFIER = Wildercord.id("cinnamon_strength");
	/** How long a jingle lasts, and when in it each of its three little dings sounds. */
	private static final int RING_TICKS = 12;
	private int settled, playTicks, greeting, ringTicks, tongueTicks;
	private int castCooldown = 40, healCooldown, castWindup, spellIndex;
	private java.util.@Nullable List<dev.wildercord.spell.RuneDef> casting;
	private @Nullable LivingEntity castTarget;
	private int nextRing = 100 + getRandom().nextInt(300);
	private boolean bow;
	private boolean savedSitting, resizing, feeding, countingDamage;
	private double growthScale = 1;
	private long growthUntil, recoveryUntil, damageImmuneUntil, stateRevision;
	private float damage;
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
		return createAnimalAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.27)
			.add(Attributes.FOLLOW_RANGE, 32).add(Attributes.ATTACK_DAMAGE, 3);
	}

	void bind(ServerPlayer owner) {
		setTame(true, false);
		setOwner(owner);
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
			if (!CinnamonCompanion.active(this)) { super.setTarget(null); getNavigation().stop(); }
		}
		super.tick();
		if (!level().isClientSide() && CinnamonCompanion.active(this)) {
			if (isOrderedToSit() && playTicks == 0 && greeting == 0) settled++; else settled = 0;
			if (playTicks > 0) playTicks--;
			if (greeting > 0) greeting--;
			ring();
			if (level() instanceof ServerLevel server) magic(server);
			// The tip of her tongue, for a few seconds about every minute and a half (asleep too).
			if (tongueTicks > 0) tongueTicks--;
			else if (getRandom().nextInt(1800) == 0) tongueTicks = 60 + getRandom().nextInt(80);
			entityData.set(MOOD, (!isExhausted() && settled >= 200 ? SLEEPING : 0) | (playTicks > 0 ? PLAYING : 0) | (greeting > 0 ? GREETING : 0)
				| (ringTicks > 0 ? RINGING : 0) | (tongueTicks > 0 ? TONGUE : 0) | (bow ? BOW : 0) | (isExhausted() ? EXHAUSTED : 0));
		}
	}

	@Override public boolean isEffectiveAi() { return super.isEffectiveAi() && CinnamonCompanion.active(this); }

	/** Vanilla's teleport only checks a small footprint. The registry owns all companion moves. */
	@Override public void tryToTeleportToOwner() { CinnamonCompanion.recallFollowing(this); }

	@Override public boolean canUsePortal(boolean allowPassengers) { return false; }

	private long clock() {
		return level() instanceof ServerLevel server ? server.getServer().overworld().getGameTime() : level().getGameTime();
	}

	public boolean isExhausted() { return level().isClientSide() ? (mood() & EXHAUSTED) != 0 : recoveryUntil > clock(); }
	public double growthScale() { return growthScale; }
	/** Strength and magic belong to her grown form only. */
	public boolean isBig() { return growthScale > 1; }
	private double growthSteps() { return Math.max(0, (growthScale - 1) / GROWTH_STEP); }
	/** How much damage tires her: more while she's big. */
	public float damageBudget() { return DAMAGE_BUDGET + BIG_BUDGET * (float) growthSteps(); }
	public long growthExpiresAt() { return growthUntil; }
	public long recoveryExpiresAt() { return recoveryUntil; }
	public long damageImmuneUntil() { return damageImmuneUntil; }
	public float accumulatedDamage() { return damage; }
	public boolean savedSitting() { return savedSitting; }
	public boolean wearingBow() { return bow; }
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
		else if (growthScale > 1 && (!server.noCollision(this, getBoundingBox())
			|| CinnamonCompanion.active(this) && onGround() && tickCount % 10 == 0 && !CompanionLanding.safe(server, this, getBoundingBox()))) {
			// Shrinking is always safe. Never push her through a ceiling, ledge, or protected boundary.
			// Keep the window: moving under a ceiling must not let the next meal restart sixty seconds.
			applyGrowth(1);
			changed();
		}
		LivingEntity target = getTarget();
		if (target != null && !canAttack(target)) { super.setTarget(null); getNavigation().stop(); }
	}

	private void ownerMessage(String key) {
		if (getOwner() instanceof ServerPlayer owner) owner.sendOverlayMessage(Component.translatable("message.wildercord.cinnamon." + key));
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
		AttributeInstance scale = getAttribute(Attributes.SCALE);
		if (scale == null) return;
		resizing = true;
		try {
			scale.removeModifier(GROWTH_MODIFIER);
			if (factor > 1) scale.addTransientModifier(new AttributeModifier(GROWTH_MODIFIER, factor - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			refreshDimensions();
		} finally { resizing = false; }
		double steps = growthSteps();
		strengthen(Attributes.ATTACK_DAMAGE, BIG_ATTACK * steps);
		strengthen(Attributes.MAX_HEALTH, BIG_HEALTH * steps);
		strengthen(Attributes.ARMOR, BIG_ARMOR * steps);
		strengthen(Attributes.KNOCKBACK_RESISTANCE, BIG_FOOTING * steps);
		if (!isBig()) { casting = null; castTarget = null; castWindup = 0; }
	}

	private void strengthen(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double amount) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance == null) return;
		instance.removeModifier(STRENGTH_MODIFIER);
		if (amount > 0) instance.addTransientModifier(new AttributeModifier(STRENGTH_MODIFIER, amount, AttributeModifier.Operation.ADD_VALUE));
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
		if (growthScale >= MAX_GROWTH_SCALE) {
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
		boolean safe = CompanionLanding.safe(player.level(), this, proposed);
		// Protection hooks can re-enter, transfer ownership, or replace the hand stack. Fail closed.
		if (!safe || !CinnamonCompanion.active(this) || !isOwnedBy(player) || player.level() != level()
			|| stateRevision != revision || !getBoundingBox().equals(before) || !player.position().equals(ownerPosition)
			|| getScale() != previousScale || scaleAt(next) != physical
			|| player.getItemInHand(hand) != held || held.isEmpty() || !held.is(ItemTags.WOLF_FOOD)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.cinnamon.growth_blocked"));
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
		if (!isBig()) return;
		if (castWindup > 0) {
			if (--castWindup == 0) release(level, owner);
			return;
		}
		if (--castCooldown > 0) return;
		castCooldown = 20;
		if (!attackReady(owner)) return;
		if (healCooldown == 0 && owner.getHealth() < owner.getMaxHealth() * 0.5F && distanceTo(owner) <= SPELL_RANGE && hasLineOfSight(owner)) {
			begin(level, MENDING, owner);
			return;
		}
		LivingEntity target = getTarget();
		if (target != null && target.isAlive() && canAttack(target) && distanceTo(target) <= SPELL_RANGE && hasLineOfSight(target)) {
			begin(level, SPELLS.get(spellIndex++ % SPELLS.size()), target);
		}
	}

	private void begin(ServerLevel level, java.util.List<dev.wildercord.spell.RuneDef> spell, LivingEntity target) {
		casting = spell;
		castTarget = target;
		castWindup = CAST_WINDUP;
		dev.wildercord.cast.Runebound.aimAt(this, target);
		dev.wildercord.cast.Runebound.telegraph(level, this, spell, target, CAST_WINDUP);
	}

	/** The target, her owner and her own state are checked again: anything may have changed during the windup. */
	private void release(ServerLevel level, @Nullable LivingEntity owner) {
		java.util.List<dev.wildercord.spell.RuneDef> spell = casting;
		LivingEntity target = castTarget;
		casting = null;
		castTarget = null;
		if (spell == null || target == null || !isBig() || !target.isAlive() || target.level() != level || !attackReady(owner)
			|| distanceTo(target) > SPELL_RANGE || !hasLineOfSight(target)) return;
		boolean mending = spell == MENDING;
		if (mending ? target != owner : !canAttack(target)) return;
		dev.wildercord.cast.Runebound.aimAt(this, target);
		dev.wildercord.cast.Runebound.cast(level, this, spell, SPELL_POWER + (growthScale - 1) * 0.25);
		if (mending) healCooldown = HEAL_INTERVAL;
		castCooldown = CAST_INTERVAL;
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
			|| target instanceof ArmorStand || target instanceof SwordMaster || duellingOwner(target)) {
			return false;
		}
		if (target instanceof TamableAnimal pet && pet.isTame() && pet.getOwner() == owner) {
			return false;
		}
		return !(target instanceof Player victim && owner instanceof Player master && !master.canHarmPlayer(victim))
			&& Targets.canHarm(owner, target) && attackReady(owner);
	}

	private boolean attackReady(LivingEntity owner) {
		return CinnamonCompanion.active(this) && !isExhausted() && !isOrderedToSit()
			&& owner instanceof ServerPlayer player && getOwner() == owner && owner.level() == level()
			&& CinnamonCompanion.mayRecall(player);
	}

	private static boolean duellingOwner(Entity target) {
		LivingEntity responsible = target instanceof Player player ? player
			: target instanceof OwnableEntity pet ? pet.getRootOwner() : null;
		return responsible instanceof Player player && Duels.inDuel(player);
	}

	@Override public boolean canAttack(LivingEntity target) {
		LivingEntity owner = getOwner();
		return owner != null && wantsToAttack(target, owner) && super.canAttack(target);
	}

	@Override public void setTarget(@Nullable LivingEntity target) {
		super.setTarget(target == null || canAttack(target) ? target : null);
	}

	/** Final bite sink: a target admitted by an earlier AI tick can become protected in the meantime. */
	@Override public boolean doHurtTarget(ServerLevel level, Entity target) {
		return target instanceof LivingEntity living && target.level() == level && canAttack(living)
			&& attackReady(getOwner()) && super.doHurtTarget(level, target);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!isOwnedBy(player) || !level().isClientSide() && !CinnamonCompanion.active(this)) {
			if (!isTame() && !level().isClientSide()) player.sendOverlayMessage(Component.translatable("message.wildercord.cinnamon.owner"));
			return InteractionResult.PASS;
		}
		if (!level().isClientSide()) {
			ItemStack held = player.getItemInHand(hand);
			if (held.is(ItemTags.WOLF_FOOD) && player instanceof ServerPlayer owner) return feed(owner, hand, held);
			if (player.getItemInHand(hand).is(CinnamonContent.TOY)) {
				playTicks = 50; settled = 0;
				level().broadcastEntityEvent(this, (byte) 7);
				player.sendOverlayMessage(Component.translatable("message.wildercord.cinnamon.toy"));
				return InteractionResult.SUCCESS;
			}
			if (held.is(CinnamonContent.BOW) && !bow) {
				wearBow(player, true);
				held.consume(1, player);
				player.sendOverlayMessage(Component.translatable("message.wildercord.cinnamon.bow_on"));
				return InteractionResult.SUCCESS;
			}
			if (held.is(Items.SHEARS) && bow) {
				wearBow(player, false);
				ItemStack ribbon = new ItemStack(CinnamonContent.BOW);
				if (!player.getInventory().add(ribbon)) spawnAtLocation((ServerLevel) level(), ribbon);
				player.sendOverlayMessage(Component.translatable("message.wildercord.cinnamon.bow_off"));
				return InteractionResult.SUCCESS;
			}
			if (player.isShiftKeyDown()) {
				playTicks = 25; settled = 0;
				level().broadcastEntityEvent(this, (byte) 7);
				player.sendOverlayMessage(Component.translatable("message.wildercord.cinnamon.pet"));
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
			player.sendOverlayMessage(Component.translatable(isOrderedToSit() ? "message.wildercord.cinnamon.sit" : "message.wildercord.cinnamon.follow"));
			getNavigation().stop();
		}
		return InteractionResult.SUCCESS;
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
		level().playSound(null, getX(), getY(), getZ(), on ? net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER.value() : net.minecraft.sounds.SoundEvents.SHEARS_SNIP,
			net.minecraft.sounds.SoundSource.NEUTRAL, 0.8F, 1.3F);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		// Operators can deliberately remove her. Ordinary hazards never enter the health-loss path.
		if (source.is(DamageTypes.GENERIC_KILL)) return super.hurtServer(level, source, amount);
		if (countingDamage) return false;
		countingDamage = true;
		try { return countDamage(level, source, amount); }
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
		hurtDuration = hurtTime = 10;
		level.broadcastDamageEvent(this, source);
		if (damage >= damageBudget()) {
			recoveryUntil = now + RECOVERY_TICKS;
			super.setTarget(null);
			super.setOrderedToSit(false);
			setInSittingPose(false);
			getNavigation().stop();
			settled = playTicks = greeting = 0;
			entityData.set(MOOD, (mood() & ~SLEEPING) | EXHAUSTED);
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
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) { return null; }

	@Override
	public boolean canBeLeashed() { return false; }

	@Override
	public boolean shouldBeSaved() { return super.shouldBeSaved(); }

	/** Canonical journal state wins over an older chunk snapshot. Deadlines never restart on recreation or recall. */
	void restoreState(boolean sitting, boolean wearingBow, double scale, long growthUntil, float damage,
			long recoveryUntil, long damageImmuneUntil) {
		long now = clock();
		savedSitting = sitting;
		bow = wearingBow;
		this.growthUntil = boundedDeadline(growthUntil, now, GROWTH_TICKS);
		this.recoveryUntil = boundedDeadline(recoveryUntil, now, RECOVERY_TICKS);
		this.damageImmuneUntil = Math.clamp(damageImmuneUntil, 0, now + DAMAGE_IMMUNITY_TICKS);
		this.damage = Float.isFinite(damage) ? Math.clamp(damage, 0, MAX_BUDGET) : 0;
		if (this.recoveryUntil == 0 && (recoveryUntil > 0 || now - this.damageImmuneUntil >= DAMAGE_QUIET_TICKS - DAMAGE_IMMUNITY_TICKS)) this.damage = 0;
		applyGrowth(this.growthUntil > 0 && Double.isFinite(scale) ? Math.clamp(scale, 1, MAX_GROWTH_SCALE) : 1);
		super.setOrderedToSit(savedSitting && !isExhausted());
		if (isExhausted()) { super.setTarget(null); setInSittingPose(false); }
		entityData.set(MOOD, (bow ? BOW : 0) | (isExhausted() ? EXHAUSTED : 0));
		stateRevision++;
	}

	private static long boundedDeadline(long deadline, long now, int maximum) {
		return deadline <= now ? 0 : Math.min(deadline, now + maximum);
	}

	@Override protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("CinnamonSitting", savedSitting);
		output.putBoolean("CinnamonBow", bow);
		output.putDouble("CinnamonScale", growthScale);
		output.putLong("CinnamonGrowthUntil", growthUntil);
		output.putFloat("CinnamonDamage", damage);
		output.putLong("CinnamonRecoveryUntil", recoveryUntil);
		output.putLong("CinnamonDamageImmuneUntil", damageImmuneUntil);
	}

	@Override protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		restoreState(input.getBooleanOr("CinnamonSitting", isOrderedToSit()), input.getBooleanOr("CinnamonBow", false),
			input.getDoubleOr("CinnamonScale", 1), input.getLongOr("CinnamonGrowthUntil", 0), input.getFloatOr("CinnamonDamage", 0),
			input.getLongOr("CinnamonRecoveryUntil", 0), input.getLongOr("CinnamonDamageImmuneUntil", 0));
	}
}
