package dev.wildercord.aura.world;

import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.BreathingMethods;
import dev.wildercord.aura.CastHitRules;
import dev.wildercord.aura.Stance;
import dev.wildercord.aura.Crescents;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.RuneBolt;
import dev.wildercord.cast.Statuses;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * An explicitly challenged endgame swordsman, separate from the travelling teachers. Only players who opt in can be
 * targeted. Three schools use different committed patterns; every strike has a tell, a locked aim and an exposed recovery.
 * Encounters are deliberately temporary: quitting, leaving the arena or restarting never leaves a hostile boss in a town.
 */
public final class SwordMaster extends AuraFighter implements Enemy {
	private static final EntityDataAccessor<Integer> DATA_ATTACK = SynchedEntityData.defineId(SwordMaster.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> DATA_ATTACK_BEGIN = SynchedEntityData.defineId(SwordMaster.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Float> DATA_ATTACK_AIM_PITCH = SynchedEntityData.defineId(SwordMaster.class, EntityDataSerializers.FLOAT);
	private static final Set<SwordMaster> ACTIVE = new LinkedHashSet<>();
	private record Introduction(UUID teacher, long until) {}
	private static final Map<UUID, Introduction> INTRODUCTIONS = new HashMap<>();
	private final Set<UUID> participants = new LinkedHashSet<>();
	private final Map<UUID, Long> invitations = new HashMap<>();
	private final ServerBossEvent bar = new ServerBossEvent(getUUID(), getType().getDescription(),
		BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
	private BlockPos home;
	private UUID challenger, waitingFor;
	private long waitingUntil;
	private long begins, expires, recoverUntil, attackAt, cutReadyAt, redirectReadyAt, dodgeReadyAt, dodgeUntil, breathingUntil, approachStarted;
	private int cuts;
	private double aura = MastersRules.AURA_MAX;
	private Vec3 dodgeDirection = Vec3.ZERO;
	private int discipline, sequence, partySize = 1, quiet;
	private boolean started, guardNext;
	private MastersRules.Move attack;
	private Vec3 lockedAim, lockedOrigin;
	private EmberAfterburn afterburn;
	private MasterPursuit pursuit;
	private long pursuitReadyAt;
	private GaleReprise reprise;
	private long repriseReadyAt;

	public SwordMaster(EntityType<? extends SwordMaster> type, Level level) {
		super(type, level);
		xpReward = 0; // Repeatable practice must not become a free experience farm.
		setPersistenceRequired();
		setDropChance(EquipmentSlot.MAINHAND, 0);
		setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
		setStage(AuraRules.SOVEREIGN);
		setDiscipline(MastersRules.EMBER);
		setState(DRAWN, true);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ATTACK, 0);
		builder.define(DATA_ATTACK_BEGIN, 0L);
		builder.define(DATA_ATTACK_AIM_PITCH, 0F);
	}

	/** 0 is idle; otherwise the move's ordinal plus one. IDs 1–4 remain the original four attacks. Server-authored. */
	public int attackAnimation() { return entityData.get(DATA_ATTACK); }

	/** Accepted crescent elevation in degrees (positive down), held through its entire recovery. */
	public float attackAimPitch() { return entityData.get(DATA_ATTACK_AIM_PITCH); }

	public float attackElapsed(float partial) {
		return attackAnimation() == 0 ? 0 : Math.max(0, level().getGameTime() - entityData.get(DATA_ATTACK_BEGIN) + partial);
	}

	public int attackTellTicks() {
		int move = attackAnimation() - 1;
		return move < 0 || move >= MastersRules.Move.values().length ? 0 : MastersRules.Move.values()[move].tell;
	}

	/** The hit occupies the first recovery tick; visual recovery is the remaining ticks. */
	public int attackActiveTicks() { return attackAnimation() == 0 ? 0 : 1; }

	public int attackRecoveryTicks() {
		int move = attackAnimation() - 1;
		return move < 0 || move >= MastersRules.Move.values().length ? 0 : MastersRules.Move.values()[move].recovery - 1;
	}

	public static void init() {
		MasterVictories.init();
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> { ACTIVE.clear(); INTRODUCTIONS.clear(); });
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, MastersRules.HEALTH)
			.add(Attributes.ARMOR, MastersRules.ARMOUR).add(Attributes.MOVEMENT_SPEED, 0.28)
			.add(Attributes.ATTACK_DAMAGE, 8).add(Attributes.FOLLOW_RANGE, MastersRules.ARENA_RADIUS)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.65);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		// No vanilla MeleeAttackGoal: it would bypass the visible commitment and recovery.
	}

	public void setDiscipline(int value) {
		discipline = MastersRules.discipline(value);
		setMethod(switch (discipline) {
			case MastersRules.GALE -> BreathingMethods.GALE;
			case MastersRules.STONE -> BreathingMethods.STONE;
			default -> BreathingMethods.EMBER;
		});
	}

	/** Starts a bounded trial on safe, loaded ground without changing blocks or enrolling bystanders. */
	public static int challenge(ServerPlayer player, int discipline) {
		return spawn(player, discipline, true);
	}

	/** The Survival teacher offers a separate, temporary master; accepting this invitation never starts combat. */
	public static int introduce(ServerPlayer player, Duelist teacher) {
		if (!mayEnter(player) || teacher.inDuel() || teacher.leaving() || teacher.tournament != null) return refuse(player, "teacher_busy");
		if (!readyForTrial(player)) return refuse(player, "teacher_unready");
		long now = player.level().getGameTime();
		INTRODUCTIONS.entrySet().removeIf(entry -> entry.getValue().until() < now);
		Introduction previous = INTRODUCTIONS.remove(player.getUUID());
		if (previous == null || !previous.teacher().equals(teacher.getUUID())) {
			if (INTRODUCTIONS.size() < 64) INTRODUCTIONS.put(player.getUUID(), new Introduction(teacher.getUUID(), now + MastersRules.INTRODUCTION_TICKS));
			player.sendSystemMessage(Component.translatable("message.wildercord.master.teacher_offer"));
			return 0;
		}
		int school = teacher.method().equals(BreathingMethods.GALE) ? MastersRules.GALE
			: teacher.method().equals(BreathingMethods.STONE) ? MastersRules.STONE : MastersRules.EMBER;
		return spawn(player, school, false);
	}

	public static boolean readyForTrial(ServerPlayer player) {
		return dev.wildercord.aura.Aura.stage(player) >= AuraRules.FORM || dev.wildercord.player.Heart.active(player) >= MastersRules.ENTRY_CIRCLE;
	}

	private static int spawn(ServerPlayer player, int discipline, boolean enroll) {
		ServerLevel level = player.level();
		ACTIVE.removeIf(Entity::isRemoved);
		if (!mayEnter(player) || level.getDifficulty() == Difficulty.PEACEFUL) {
			return refuse(player, "unavailable");
		}
		if (!MastersRules.canAdmitEncounter(ACTIVE.size(), false) || ACTIVE.stream().anyMatch(master -> master.participants.contains(player.getUUID())
			|| master.level() == level && master.distanceToSqr(player) < 64 * 64)) {
			return refuse(player, "occupied");
		}
		SwordMaster master = AuraWorld.SWORD_MASTER.create(level, EntitySpawnReason.TRIGGERED);
		if (master == null) {
			return refuse(player, "obstructed");
		}
		Vec3 start = player.position();
		for (int i = 0; i < 8; i++) {
			double angle = i * Math.PI / 4;
			BlockPos feet = BlockPos.containing(start.add(Math.cos(angle) * 8, 0, Math.sin(angle) * 8));
			if (!level.hasChunkAt(feet) || !level.getWorldBorder().isWithinBounds(feet.getX(), feet.getZ())) {
				continue;
			}
			if (!level.getBlockState(feet.below()).isSolidRender() || level.getBlockState(feet.below()).is(BlockTags.FIRE)
				|| !level.getFluidState(feet).isEmpty()) {
				continue;
			}
			master.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0, 0);
			if (!level.noCollision(master, master.getBoundingBox()) || !master.hasLineOfSight(player)) {
				continue;
			}
			master.setDiscipline(discipline);
			if (enroll) {
				master.prepare(player);
			} else {
				master.waitingFor = player.getUUID();
				master.waitingUntil = level.getGameTime() + MastersRules.WAITING_TICKS;
				ACTIVE.add(master);
			}
			if (!level.addFreshEntity(master)) {
				ACTIVE.remove(master);
				return refuse(player, "obstructed");
			}
			player.sendSystemMessage(Component.translatable(enroll ? "message.wildercord.master.challenge" : "message.wildercord.master.teacher_arrived", master.method().id()));
			return 1;
		}
		return refuse(player, "obstructed");
	}

	/** Explicit consent for another player, only during the thirty-second preparation. */
	public static int join(ServerPlayer player) {
		if (!mayEnter(player) || ACTIVE.stream().anyMatch(master -> master.participants.contains(player.getUUID()))) {
			return refuse(player, "unavailable");
		}
		SwordMaster master = ACTIVE.stream().filter(m -> !m.isRemoved() && m.lobbyOpen() && m.level() == player.level()
			&& m.home != null && player.distanceToSqr(Vec3.atCenterOf(m.home)) <= MastersRules.ARENA_RADIUS * MastersRules.ARENA_RADIUS)
			.min(Comparator.comparingDouble(m -> m.distanceToSqr(player))).orElse(null);
		if (master == null || master.participants.size() >= MastersRules.MAX_PARTICIPANTS) {
			return refuse(player, "no_trial");
		}
		master.participants.add(player.getUUID());
		player.sendSystemMessage(Component.translatable("message.wildercord.master.joined"));
		return 1;
	}

	/** The original challenger may close the lobby early after everyone has opted in. */
	public static int ready(ServerPlayer player) {
		SwordMaster master = ACTIVE.stream().filter(m -> !m.isRemoved() && m.lobbyOpen() && m.level() == player.level()
			&& player.getUUID().equals(m.challenger) && m.participant(player)).findFirst().orElse(null);
		if (master == null) return refuse(player, "no_trial");
		master.begins = player.level().getGameTime();
		player.sendSystemMessage(Component.translatable("message.wildercord.master.ready"));
		return 1;
	}

	private static boolean mayEnter(ServerPlayer player) {
		return player.isAlive() && !player.isSpectator() && !player.isCreative()
			&& !DuelistDuels.inDuel(player) && !dev.wildercord.duel.Duels.inDuel(player) && !dev.wildercord.aura.Spars.sparring(player);
	}

	private static int refuse(ServerPlayer player, String reason) {
		player.sendSystemMessage(Component.translatable("message.wildercord.master." + reason));
		return 0;
	}

	private void prepare(ServerPlayer challenger) {
		this.challenger = challenger.getUUID();
		waitingFor = null;
		waitingUntil = 0;
		home = challenger.blockPosition().immutable();
		begins = level().getGameTime() + MastersRules.PREPARE_TICKS;
		expires = begins + MastersRules.ENCOUNTER_TICKS;
		participants.add(challenger.getUUID());
		ACTIVE.add(this);
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
		if (!(player instanceof ServerPlayer server)) return InteractionResult.SUCCESS;
		if (home == null && waitingFor != null && !waitingFor.equals(player.getUUID())) {
			refuse(server, "reserved");
			return InteractionResult.SUCCESS;
		}
		if (started || home != null && !lobbyOpen() || participants.contains(player.getUUID())) {
			refuse(server, started || home != null && !lobbyOpen() ? "in_progress" : "waiting");
			return InteractionResult.SUCCESS;
		}
		if (!mayEnter(server) || level().getDifficulty() == Difficulty.PEACEFUL) {
			refuse(server, "unavailable");
			return InteractionResult.FAIL;
		}
		long now = level().getGameTime();
		invitations.entrySet().removeIf(entry -> entry.getValue() < now);
		Long invitation = invitations.remove(player.getUUID());
		if (invitation == null) {
			if (invitations.size() < 16) invitations.put(player.getUUID(), now + 200);
			server.sendSystemMessage(Component.translatable("message.wildercord.master.dialogue", method().id()));
			return InteractionResult.SUCCESS;
		}
		ACTIVE.removeIf(Entity::isRemoved);
		if (ACTIVE.stream().anyMatch(m -> m != this && m.participants.contains(player.getUUID()))
			|| home == null && !MastersRules.canAdmitEncounter(ACTIVE.size(), ACTIVE.contains(this))) {
			refuse(server, "occupied");
			return InteractionResult.FAIL;
		}
		if (home == null) {
			prepare(server);
			server.sendSystemMessage(Component.translatable("message.wildercord.master.challenge", method().id()));
		} else if (participants.size() < MastersRules.MAX_PARTICIPANTS
			&& server.distanceToSqr(Vec3.atCenterOf(home)) <= MastersRules.ARENA_RADIUS * MastersRules.ARENA_RADIUS) {
			participants.add(player.getUUID());
			server.sendSystemMessage(Component.translatable("message.wildercord.master.joined"));
		} else {
			refuse(server, "no_trial");
		}
		return InteractionResult.SUCCESS;
	}

	/** The party size is captured once; leaving or dying never lowers the remaining challenge. */
	public double postureMultiplier() {
		return MastersRules.postureMultiplier(partySize);
	}

	public boolean canHarmParticipant(Entity entity) {
		return started && isAlive() && !isRemoved() && entity instanceof LivingEntity living && participant(living);
	}

	/** Incoming attacks and control effects share the same enrollment boundary, including a challenger's summons. */
	public boolean acceptsHarmFrom(Entity entity) {
		ServerPlayer owner = MasterVictories.owner(entity);
		return started && isAlive() && !isRemoved() && owner != null && participant(owner);
	}

	/** Active trials reject outside or unattributed influence; self-authored mechanics and enrolled sources remain valid. */
	public boolean acceptsInfluence(Entity source) {
		return !started || source == this || acceptsHarmFrom(source);
	}

	@Override
	public boolean addEffect(MobEffectInstance effect, Entity source) {
		Entity responsible = source != null ? source : dev.wildercord.cast.Effects.applying();
		if (!acceptsInfluence(responsible)) return false;
		return super.addEffect(effect, source);
	}

	@Override
	public void heal(float amount) {
		// Vanilla instant/regeneration healing has no owner argument. Unattributed healing cannot alter an active trial.
		if (acceptsInfluence(dev.wildercord.cast.Effects.applying())) super.heal(amount);
	}

	/** Read-only encounter information for UI and contract tests. */
	public boolean started() { return started; }
	public int challengerCount() { return started ? partySize : participants.size(); }
	public Set<UUID> challengers() { return Set.copyOf(participants); }
	public double auraRemaining() { return aura; }

	boolean afterburnPending() { return afterburn != null; }
	boolean pursuitPending() { return pursuit != null; }
	boolean reprisePending() { return reprise != null; }

	boolean canBeginReprise(long now) {
		return canMaintainAfterburn() && !isNoAi() && attack == null && now >= recoverUntil
			&& now >= dodgeUntil && !guarding();
	}

	boolean canMaintainReprise() { return canMaintainAfterburn() && !isNoAi() && attack == MastersRules.Move.CROSSWIND_REPRISE; }

	boolean canMaintainPursuit() { return canMaintainAfterburn() && attack == MastersRules.Move.PURSUIT_BREAK; }

	boolean pursuitInsideArena(net.minecraft.world.phys.AABB body) {
		return home != null && MasterPursuitRules.insideArena(body.minX, body.maxX, body.minZ, body.maxZ,
			home.getX() + .5, home.getZ() + .5, MastersRules.ARENA_RADIUS)
			&& Math.abs(body.minY - home.getY()) <= 1;
	}

	/** A wake may never outlive its master, arena or last eligible challenger, even when ticked independently. */
	boolean canMaintainAfterburn() {
		return started && isAlive() && !isRemoved() && home != null && level() instanceof ServerLevel level
			&& level.getGameTime() < expires && level.getDifficulty() != Difficulty.PEACEFUL && !staggered() && !Stance.opened(this)
			&& distanceToSqr(Vec3.atCenterOf(home)) <= MastersRules.ARENA_RADIUS * MastersRules.ARENA_RADIUS
			&& Math.abs(getY() - home.getY()) <= 8
			&& participants.stream().anyMatch(id -> level.getPlayerByUUID(id) instanceof LivingEntity living && participant(living));
	}

	private boolean lobbyOpen() {
		return home != null && !started && level().getGameTime() < begins;
	}

	private boolean participant(LivingEntity entity) {
		return entity instanceof ServerPlayer player && participants.contains(player.getUUID()) && mayEnter(player)
			&& player.level() == level() && home != null
			&& player.distanceToSqr(Vec3.atCenterOf(home)) <= MastersRules.ARENA_RADIUS * MastersRules.ARENA_RADIUS;
	}

	/** Pausing AI cancels its current warning instead of allowing an unwarned release on the resume tick. */
	@Override
	public void setNoAi(boolean noAi) {
		if (noAi && !isNoAi()) cancelAttack();
		super.setNoAi(noAi);
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return started && participant(target) && super.canAttack(target);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (!isAlive()) return;
		long now = level.getGameTime();
		if (attackAnimation() != 0 && now >= entityData.get(DATA_ATTACK_BEGIN) + attackTellTicks() + attackActiveTicks() + attackRecoveryTicks()) {
			entityData.set(DATA_ATTACK, 0);
			entityData.set(DATA_ATTACK_AIM_PITCH, 0F);
		}
		if (home == null) {
			if (waitingUntil > 0 && now >= waitingUntil) discard();
			return;
		}
		participants.removeIf(id -> !(level.getPlayerByUUID(id) instanceof ServerPlayer player) || !participant(player));
		// Braking belongs before every interruption/abandonment early return, including losing the current target.
		if (state(DASH) && (now >= dodgeUntil || participants.isEmpty() || staggered() || Stance.opened(this)
			|| getTarget() == null || !participant(getTarget()))) stopDodge(now);
		if (now >= expires || level.getDifficulty() == Difficulty.PEACEFUL
			|| distanceToSqr(Vec3.atCenterOf(home)) > MastersRules.ARENA_RADIUS * MastersRules.ARENA_RADIUS
			|| Math.abs(getY() - home.getY()) > 8) {
			discard();
			return;
		}
		if (now % 10 == 0) {
			for (ServerPlayer player : new ArrayList<>(bar.getPlayers())) {
				if (!participant(player)) bar.removePlayer(player);
			}
			for (ServerPlayer player : level.players()) {
				if (participant(player)) bar.addPlayer(player);
			}
			bar.setProgress(getHealth() / getMaxHealth());
			Component activity = !started
				? Component.translatable("boss.wildercord.master.prepare", participants.size(), MastersRules.MAX_PARTICIPANTS, Math.max(0, (begins - now + 19) / 20))
				: Component.translatable("boss.wildercord.master." + (attack != null ? pursuit != null && pursuit.strikeWarned() ? "pursuit_strike" : reprise != null && reprise.replyWarned() ? "crosswind_reply" : attack.name().toLowerCase(java.util.Locale.ROOT) : afterburn != null ? "afterburn"
					: staggered() || Stance.opened(this) ? "broken" : now < breathingUntil ? "breathing" : now < recoverUntil ? "recover" : guarding() ? "guard" : "ready"));
			bar.setName(Component.translatable("boss.wildercord.master.status", method().id(), activity));
		}
		if (participants.isEmpty()) {
			cancelAttack();
			getNavigation().stop();
			if (++quiet >= MastersRules.ABANDON_TICKS) discard();
			return;
		}
		quiet = 0;
		if (!started) {
			getNavigation().stop();
			if (now < begins) return;
			removeAllEffects(); // Staging buffs cannot be smuggled into the locked encounter.
			started = true;
			partySize = MastersRules.participants(participants.size());
			getAttribute(Attributes.MAX_HEALTH).setBaseValue(MastersRules.health(partySize));
			setHealth(getMaxHealth());
			if (discipline == MastersRules.EMBER) for (ServerPlayer player : level.players()) {
				if (participant(player)) player.sendSystemMessage(Component.translatable("message.wildercord.master.ember_lesson"));
			}
			if (discipline == MastersRules.GALE) for (ServerPlayer player : level.players()) {
				if (participant(player)) player.sendSystemMessage(Component.translatable("message.wildercord.master.gale_lesson"));
			}
			for (ServerPlayer player : level.players()) if (participant(player)) {
				player.sendSystemMessage(Component.translatable("message.wildercord.master.pursuit_lesson")
					.append(" ").append(Component.translatable("message.wildercord.master.pursuit_" + method().id())));
			}
			recoverUntil = now + 20;
		}
		tickGuard(now);
		tickStagger(now);
		if (staggered() || Stance.opened(this)) {
			cancelAttack();
			dropGuard();
			getNavigation().stop();
			recoverUntil = Math.max(recoverUntil, now + 10);
			return;
		}
		if (afterburn != null && !afterburn.tick(now)) afterburn = null;
		LivingEntity target = getTarget();
		if (target == null || !participant(target)) {
			target = level.players().stream().filter(this::participant).min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
			setTarget(target);
		}
		if (target == null) return;
		if (attack != null) {
			tickAttack(level, now, target);
			return;
		}
		if (now < dodgeUntil) {
			getNavigation().stop();
			// Ground or obstacles may have changed since the initial probe.
			if (safeMotion(level, dodgeDirection.scale(0.65))) {
				setDeltaMovement(dodgeDirection.scale(0.65).add(0, getDeltaMovement().y, 0));
			} else {
				dodgeUntil = now;
				setDeltaMovement(0, getDeltaMovement().y, 0);
			}
			return;
		}
		if (state(DASH)) {
			// Braking ends the measured, collision-checked step; residual momentum may not carry it past its safe path.
			setDeltaMovement(0, getDeltaMovement().y, 0);
			setState(DASH, false);
		}
		if (now < recoverUntil) {
			getNavigation().stop();
			if (now < breathingUntil) aura = Math.min(MastersRules.AURA_MAX, aura + MastersRules.AURA_MAX / MastersRules.BREATH_TICKS);
			return;
		}
		if (aura < MastersRules.ATTACK_COST + MastersRules.GUARD_COST) {
			dropGuard();
			recoverUntil = breathingUntil = now + MastersRules.BREATH_TICKS;
			Feels.sound(level, position(), "aura_breath", 1, 0.8F);
			return;
		}
		if (guarding()) {
			getNavigation().stop();
			// A committed guard does not spin to negate flanking.
			cutBolt(level, now);
			return;
		}
		// Look for an admitted casting opening only while entirely free. Never retarget an accepted dash.
		for (ServerPlayer caster : level.players().stream().filter(this::participant)
			.sorted(Comparator.comparingDouble((ServerPlayer player) -> distanceToSqr(player)).thenComparing(Entity::getUUID)).toList()) {
			MasterPursuit opening = MasterPursuit.prepare(this, caster, discipline, aura, now, pursuitReadyAt);
			if (opening != null) {
				guardNext = false;
				setTarget(caster);
				beginAttack(level, caster, MastersRules.Move.PURSUIT_BREAK, now);
				pursuit = opening;
				pursuitReadyAt = now + MasterPursuitRules.school(discipline).cooldown();
				pursuit.tick(now);
				return;
			}
		}
		if (guardNext) {
			guardNext = false;
			faceTarget(target);
			if (raiseGuard()) { aura -= MastersRules.GUARD_COST; return; }
		}
		if (target instanceof ServerPlayer player) {
			GaleReprise opening = GaleReprise.prepare(this, player, discipline, sequence, aura, now, repriseReadyAt);
			if (opening != null) {
				beginAttack(level, player, MastersRules.Move.CROSSWIND_REPRISE, now);
				reprise = opening;
				repriseReadyAt = now + GaleRepriseRules.COOLDOWN;
				reprise.tick(now);
				return;
			}
		}
		if (evadeThreat(level, now)) return;
		double targetHeight = target.getBoundingBox().getCenter().y - slashOrigin().y;
		if (Math.abs(targetHeight) <= 2.5 && distanceTo(target) > 6 && sequence % 3 != 0) {
			if (approachStarted == 0) approachStarted = now;
			if (now - approachStarted < 40) {
				getNavigation().moveTo(target, discipline == MastersRules.GALE ? 1.3 : 1.1);
				return;
			}
		}
		if (!hasLineOfSight(target)) {
			getNavigation().moveTo(target, discipline == MastersRules.GALE ? 1.25 : 1.0);
			return;
		}
		MastersRules.Move next = MastersRules.needsCrescent(distanceTo(target), targetHeight) ? MastersRules.Move.CRESCENT
			: target.hasAttached(WildercordAttachments.CHARGE) && distanceTo(target) <= 4 ? MastersRules.Move.BREAK_CAST
			: EmberWakeRules.next(discipline, sequence, MastersRules.phase(getHealth(), getMaxHealth()), distanceTo(target)) ? MastersRules.Move.CINDER_WAKE
			: MastersRules.move(discipline, sequence, MastersRules.phase(getHealth(), getMaxHealth()), distanceTo(target));
		beginAttack(level, target, next, now);
	}

	private void beginAttack(ServerLevel level, LivingEntity target, MastersRules.Move move, long now) {
		dropGuard();
		attack = move;
		entityData.set(DATA_ATTACK, move.ordinal() + 1);
		entityData.set(DATA_ATTACK_BEGIN, now);
		approachStarted = 0;
		setDeltaMovement(0, getDeltaMovement().y, 0);
		aura -= move == MastersRules.Move.CINDER_WAKE ? EmberWakeRules.COST
			: move == MastersRules.Move.PURSUIT_BREAK ? MasterPursuitRules.school(discipline).cost()
			: move == MastersRules.Move.CROSSWIND_REPRISE ? GaleRepriseRules.COST : MastersRules.ATTACK_COST;
		attackAt = now + move.tell;
		lockedAim = null;
		lockedOrigin = null;
		faceTarget(target);
		if (move == MastersRules.Move.CINDER_WAKE) {
			// The whole two-beat shape is committed before its first warning, not re-aimed at the second beat.
			lockedAim = flatLook();
			lockedOrigin = position();
			EmberAfterburn.warnCut(this, lockedOrigin, lockedAim, move.tell);
		}
		syncAnimationPitch(move == MastersRules.Move.CRESCENT ? target.getBoundingBox().getCenter().subtract(slashOrigin()) : Vec3.ZERO);
		setState(WINDUP, true);
		getNavigation().stop();
		Feels.sound(level, position(), "duelist_knight_windup", 1, move == MastersRules.Move.BREAK_CAST ? 1.3F : 0.9F);
		AuraFx.banner(this, Component.translatable("boss.wildercord.master." + move.name().toLowerCase(java.util.Locale.ROOT)),
			Component.empty(), auraColor(), AuraFxRules.BannerKind.ART);
	}

	private void tickAttack(ServerLevel level, long now, LivingEntity target) {
		getNavigation().stop();
		if (attack == MastersRules.Move.CROSSWIND_REPRISE) {
			GaleReprise running = reprise;
			if (running != null && running.tick(now)) return;
			if (attack != MastersRules.Move.CROSSWIND_REPRISE) return;
			boolean released = running != null && running.released();
			if (!released) cancelAttack();
			else {
				reprise = null;
				attack = null;
				setState(WINDUP, false);
				guardNext = MastersRules.guardAfter(discipline, ++sequence);
				retargetBetweenAttacks(level);
			}
			setDeltaMovement(0, getDeltaMovement().y, 0);
			recoverUntil = now + GaleRepriseRules.RECOVERY;
			guardReadyAt = Math.max(guardReadyAt, recoverUntil);
			return;
		}
		if (attack == MastersRules.Move.PURSUIT_BREAK) {
			MasterPursuit running = pursuit;
			if (running != null && running.tick(now)) return;
			if (attack != MastersRules.Move.PURSUIT_BREAK) return; // A damage callback may already cancel it.
			boolean released = running != null && running.released();
			if (!released) cancelAttack();
			else {
				pursuit = null;
				attack = null;
				setState(WINDUP, false);
				guardNext = MastersRules.guardAfter(discipline, ++sequence);
				retargetBetweenAttacks(level);
			}
			setDeltaMovement(0, getDeltaMovement().y, 0);
			recoverUntil = now + MasterPursuitRules.RECOVERY;
			guardReadyAt = Math.max(guardReadyAt, recoverUntil);
			return;
		}
		if (attack == MastersRules.Move.CINDER_WAKE && now < attackAt && (attackAt - now) % EmberWakeRules.WARNING_REFRESH == 0) {
			EmberAfterburn.warnCut(this, lockedOrigin, lockedAim, (int) (attackAt - now));
		}
		if (lockedAim == null && now < attackAt - MastersRules.AIM_LOCK) {
			faceTarget(target);
			if (attack == MastersRules.Move.CRESCENT) syncAnimationPitch(target.getBoundingBox().getCenter().subtract(slashOrigin()));
		}
		if (lockedAim == null && now >= attackAt - MastersRules.AIM_LOCK) {
			Vec3 toward = target.getBoundingBox().getCenter().subtract(slashOrigin());
			lockedAim = attack == MastersRules.Move.CRESCENT && toward.lengthSqr() > 1.0E-6 ? toward.normalize() : flatLook();
			syncAnimationPitch(lockedAim);
			lockedOrigin = position();
			Vec3 feet = position().add(0, 0.1, 0);
			if (attack == MastersRules.Move.CRESCENT) {
				for (double angle : MastersRules.volleyAngles(partySize)) {
					Light.ray(level, slashOrigin(), slashOrigin().add(rotate(lockedAim, angle).scale(Math.min(MastersRules.PROJECTILE_RANGE, toward.length()))), auraColor(), 0.08, MastersRules.AIM_LOCK + 2);
				}
			} else {
				Light.ray(level, feet, feet.add(lockedAim.scale((attack == MastersRules.Move.SWEEP || attack == MastersRules.Move.CINDER_WAKE) ? 4 : 6)), auraColor(), 0.08, MastersRules.AIM_LOCK + 2);
			}
		}
		if (now < attackAt) return;
		if (attack == MastersRules.Move.CINDER_WAKE && now != attackAt) {
			cancelAttack();
			recoverUntil = now + EmberWakeRules.RECOVERY;
			return;
		}
		MastersRules.Move released = attack;
		Vec3 aim = lockedAim == null ? flatLook() : lockedAim;
		Vec3 origin = released == MastersRules.Move.CINDER_WAKE && lockedOrigin != null ? lockedOrigin : position();
		setState(WINDUP, false);
		swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
		if (released == MastersRules.Move.CRESCENT) {
			Feels.sound(level, position(), "aura_slash", 1, 0.9F);
			AuraFx.trail(this, AuraFxRules.Stroke.SWEEP, false, auraColor(), stage(), 1.2F);
			Set<UUID> struck = new HashSet<>();
			for (double angle : MastersRules.volleyAngles(partySize)) {
				Crescents.launch(this, slashOrigin(), rotate(aim, angle), auraColor(), slashDamage(), 1.0, slashSpeed(), slashRange(), slashWidth(),
					AuraRules.SLASH_TARGETS, false, mayCut(), (flight, foe) -> {
						// Extra lanes add positioning pressure, never overlapping damage on the same player.
						return struck.add(foe.getUUID()) ? projected(foe, flight.damage()) : 0;
					});
			}
		} else {
			Vec3 side = new Vec3(-aim.z, 0, aim.x);
			AuraFx.trail(this, (released == MastersRules.Move.SWEEP || released == MastersRules.Move.CINDER_WAKE) ? AuraFxRules.Stroke.SWEEP : AuraFxRules.Stroke.THRUST, false, auraColor(), stage(), 1.2F);
			Feels.sound(level, position(), "aura_slash", 1, 0.9F);
			for (ServerPlayer player : level.players()) {
				if (!participant(player)) continue;
				Vec3 delta = player.position().subtract(origin);
				if (MastersRules.hits(released, delta.dot(aim), delta.dot(side), delta.y)
					&& (released == MastersRules.Move.CINDER_WAKE ? EmberAfterburn.clear(level, this, origin.add(0, .9, 0), player.getBoundingBox().getCenter()) : hasLineOfSight(player))) {
					if (released == MastersRules.Move.BREAK_CAST) {
						var charge = player.getAttached(WildercordAttachments.CHARGE);
						MasterHitReceipt.Result hit = MasterHitReceipt.measure(this, player,
							() -> projected(player, MastersRules.damage(partySize, discipline, released)));
						if (CastHitRules.response(hit.damaging(), charge,
							player.getAttached(WildercordAttachments.CHARGE)) == CastHitRules.Response.INTERRUPT)
							Statuses.interrupt(player); // Shared immunity prevents repeated masters from locking out a caster.
					} else projected(player, MastersRules.damage(partySize, discipline, released));
				}
			}
		}
		if (released == MastersRules.Move.CINDER_WAKE && canMaintainAfterburn()) {
			afterburn = new EmberAfterburn(this, origin, aim, partySize, now);
			afterburn.tick(now);
			Feels.sound(level, origin, "duelist_knight_windup", 1, 1.35F);
			AuraFx.banner(this, Component.translatable("boss.wildercord.master.afterburn"),
				Component.translatable("message.wildercord.master.afterburn_hint"), auraColor(), AuraFxRules.BannerKind.ART);
			for (ServerPlayer player : level.players()) if (participant(player)) {
				player.sendOverlayMessage(Component.translatable("message.wildercord.master.afterburn_hint"));
			}
		}
		attack = null;
		lockedAim = null;
		lockedOrigin = null;
		recoverUntil = now + released.recovery;
		guardReadyAt = Math.max(guardReadyAt, recoverUntil);
		guardNext = MastersRules.guardAfter(discipline, ++sequence);
		retargetBetweenAttacks(level);
	}

	/** Retarget only between committed attacks, giving every enrolled player readable pressure. */
	private void retargetBetweenAttacks(ServerLevel level) {
		if (partySize > 1) {
			var available = level.players().stream().filter(this::participant).sorted(Comparator.comparing(Entity::getUUID)).toList();
			if (!available.isEmpty()) setTarget(available.get(Math.floorMod(sequence, available.size())));
		}
	}

	private static Vec3 rotate(Vec3 direction, double degrees) {
		double radians = Math.toRadians(degrees);
		return new Vec3(direction.x * Math.cos(radians) - direction.z * Math.sin(radians), direction.y,
			direction.x * Math.sin(radians) + direction.z * Math.cos(radians));
	}

	private void cutBolt(ServerLevel level, long now) {
		if (now >= cutReadyAt) { cuts = 0; cutReadyAt = now + MastersRules.CUT_REST; }
		if (cuts >= MastersRules.cutBudget(partySize) || aura < MastersRules.CUT_COST) return;
		for (RuneBolt bolt : level.getEntitiesOfClass(RuneBolt.class, getBoundingBox().inflate(MastersRules.CUT_RANGE))) {
			if (!(bolt.getOwner() instanceof LivingEntity owner) || !participant(owner)) continue;
			Vec3 toward = bolt.position().subtract(getBoundingBox().getCenter());
			double distance = toward.length();
			double front = flatLook().dot(toward.normalize());
			double incoming = bolt.getDeltaMovement().normalize().dot(toward.normalize().scale(-1));
			if (!MastersRules.canCut(now, guardRaised, 0, distance, front, incoming)) continue;
			boolean redirected = now >= redirectReadyAt && aura >= MastersRules.REDIRECT_COST && bolt.swordRedirect(this);
			if (redirected || bolt.swordCut(this)) {
				cuts++;
				aura -= redirected ? MastersRules.REDIRECT_COST : MastersRules.CUT_COST;
				if (redirected) redirectReadyAt = now + MastersRules.REDIRECT_REST;
				AuraFx.trail(this, AuraFxRules.Stroke.SWEEP, false, auraColor(), stage(), 0.8F);
				Feels.sound(level, position(), "aura_perfect_guard", 1, redirected ? 0.8F : 1.2F);
				if (cuts >= MastersRules.cutBudget(partySize) || aura < MastersRules.CUT_COST) break;
			}
		}
	}

	/** React only while uncommitted. Physics moves the body; every proposed step is checked for walls and unsafe ground. */
	private boolean evadeThreat(ServerLevel level, long now) {
		if (now < dodgeReadyAt || aura < MastersRules.DODGE_COST) return false;
		for (RuneBolt bolt : level.getEntitiesOfClass(RuneBolt.class, getBoundingBox().inflate(8))) {
			if (!(bolt.getOwner() instanceof LivingEntity owner) || !participant(owner) || !bolt.hostileSpellTo(this)) continue;
			Vec3 toward = getBoundingBox().getCenter().subtract(bolt.position());
			if (bolt.getDeltaMovement().normalize().dot(toward.normalize()) < 0.9 || !hasLineOfSight(bolt)) continue;
			Vec3 flight = bolt.getDeltaMovement().multiply(1, 0, 1).normalize();
			if (flight.lengthSqr() < 0.1) flight = flatLook(); // A vertical bolt still has a safe sideways answer.
			for (int sign : new int[] {sequence % 2 == 0 ? 1 : -1, sequence % 2 == 0 ? -1 : 1}) {
				Vec3 side = new Vec3(-flight.z * sign, 0, flight.x * sign);
				if (!safeStep(level, side)) continue;
				dodgeDirection = side;
				dodgeUntil = now + MastersRules.DODGE_TICKS;
				dodgeReadyAt = now + MastersRules.DODGE_REST;
				recoverUntil = dodgeUntil + 12;
				aura -= MastersRules.DODGE_COST;
				setState(DASH, true);
				setDeltaMovement(side.scale(0.65).add(0, getDeltaMovement().y, 0));
				Feels.sound(level, position(), "aura_step", 1, 0.9F);
				return true;
			}
		}
		return false;
	}

	private boolean safeStep(ServerLevel level, Vec3 direction) {
		if (!onGround()) return false;
		for (int i = 1; i <= 12; i++) {
			if (!safeMotion(level, direction.scale(i * 0.25))) return false;
		}
		return true;
	}

	private boolean safeMotion(ServerLevel level, Vec3 offset) {
		Vec3 next = position().add(offset);
		BlockPos feet = BlockPos.containing(next);
		return home != null && level.hasChunkAt(feet) && level.getWorldBorder().isWithinBounds(next.x, next.z)
			&& next.distanceToSqr(Vec3.atCenterOf(home)) <= MastersRules.ARENA_RADIUS * MastersRules.ARENA_RADIUS
			&& level.noCollision(this, getBoundingBox().expandTowards(offset))
			&& level.getFluidState(feet).isEmpty() && level.getFluidState(feet.below()).isEmpty()
			&& level.getBlockState(feet.below()).isSolidRender() && !level.getBlockState(feet).is(BlockTags.FIRE)
			&& !level.getBlockState(feet.below()).is(BlockTags.FIRE);
	}

	private void stopDodge(long now) {
		dodgeUntil = now;
		setDeltaMovement(0, getDeltaMovement().y, 0);
		setState(DASH, false);
	}

	private void cancelAttack() {
		if (reprise != null) {
			setDeltaMovement(0, getDeltaMovement().y, 0);
			recoverUntil = Math.max(recoverUntil, level().getGameTime() + GaleRepriseRules.RECOVERY);
			guardReadyAt = Math.max(guardReadyAt, recoverUntil);
		}
		reprise = null;
		if (pursuit != null) {
			setDeltaMovement(0, getDeltaMovement().y, 0);
			recoverUntil = Math.max(recoverUntil, level().getGameTime() + MasterPursuitRules.RECOVERY);
			guardReadyAt = Math.max(guardReadyAt, recoverUntil);
		}
		pursuit = null;
		entityData.set(DATA_ATTACK, 0);
		entityData.set(DATA_ATTACK_AIM_PITCH, 0F);
		if (state(DASH)) stopDodge(level().getGameTime());
		attack = null;
		lockedAim = null;
		lockedOrigin = null;
		afterburn = null;
		setState(WINDUP, false);
	}

	/** Animation reads this synchronized direction; it never tracks a target independently after aim lock. */
	private void syncAnimationPitch(Vec3 direction) {
		float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)));
		entityData.set(DATA_ATTACK_AIM_PITCH, Float.isFinite(pitch) ? pitch : 0F);
	}

	/** Slow effects still hinder footwork, but cannot become an indefinitely renewable full-body stagger. */
	@Override
	public boolean staggered() {
		return level().getGameTime() < staggerUntil || Stance.opened(this);
	}

	/** Shared Statuses.interrupt owns the per-target immunity; this is the master's genuinely interruptible window. */
	public boolean interruptWindup() {
		long now = level().getGameTime();
		if (!started || !isAlive() || !acceptsInfluence(dev.wildercord.cast.Effects.applying())
			|| !MastersRules.interruptible(attack != null, now, attackAt)) return false;
		cancelAttack();
		dropGuard();
		guardNext = false;
		staggerUntil = now + 12;
		recoverUntil = Math.max(recoverUntil, now + 20);
		getNavigation().stop();
		return true;
	}

	@Override
	public float projected(LivingEntity target, double damage) {
		return started && participant(target) ? super.projected(target, damage) : 0;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurtServer(level, source, damage);
		if (!acceptsHarmFrom(source.getEntity())) return false;
		float through = guarded(level, source, damage);
		if (through <= 0) return false;
		return super.hurtServer(level, source, through);
	}

	@Override
	protected void guardBroken(ServerLevel level, Player by) {
		super.guardBroken(level, by);
		cancelAttack();
		guardNext = false;
		recoverUntil = Math.max(recoverUntil, level.getGameTime() + 30);
	}

	@Override protected double slashDamage() { return MastersRules.damage(partySize, discipline, MastersRules.Move.CRESCENT); }
	@Override protected double slashSpeed() { return discipline == MastersRules.GALE ? 1.5 : 1.25; }
	@Override protected double slashRange() { return MastersRules.PROJECTILE_RANGE; }
	@Override protected double slashWidth() { return 2.5; }
	@Override protected Predicate<Entity> mayCut() { return entity -> entity instanceof LivingEntity living && participant(living); }
	@Override public boolean shouldBeSaved() { return false; }
	@Override public boolean removeWhenFarAway(double distance) { return false; }

	@Override
	public void die(DamageSource source) {
		cancelAttack();
		if (level() instanceof ServerLevel level) {
			ServerPlayer finisher = MasterVictories.owner(source.getEntity());
			if (finisher == null) finisher = MasterVictories.owner(source.getDirectEntity());
			boolean legitimate = MasterVictoryRules.legitimate(started, !isNoAi(), source.is(DamageTypeTags.BYPASSES_INVULNERABILITY),
				finisher != null && participant(finisher));
			Set<UUID> present = new HashSet<>();
			for (ServerPlayer player : level.players()) if (participant(player)) present.add(player.getUUID());
			Set<UUID> credited = legitimate ? MasterVictoryRules.credit(participants, present) : Set.of();
			for (ServerPlayer player : level.players()) {
				if (!participant(player)) continue;
				player.sendSystemMessage(Component.translatable("message.wildercord.master.victory", method().id()));
				if (credited.contains(player.getUUID())) MasterVictories.award(player, discipline);
			}
		}
		super.die(source);
		bar.removeAllPlayers();
		ACTIVE.remove(this);
	}

	@Override
	public void remove(RemovalReason reason) {
		cancelAttack();
		bar.removeAllPlayers();
		ACTIVE.remove(this);
		super.remove(reason);
	}
}
