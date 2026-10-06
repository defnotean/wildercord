package dev.wildercord.aura.world;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.AuraVfx;
import dev.wildercord.aura.BreathingMethods;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.feel.Feels;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * A wandering duelist: a sword master in a travelling cloak with a sheathed blade, who breathes one way (its colour and
 * element) and teaches it to whoever can beat it. It drifts about near villages and roads, or sits by a small campfire, and
 * harms nobody. Use it and it offers a duel; use it again to accept (see {@link DuelistDuels}).
 *
 * <p>In a duel it meets its challenger at their own stage and fights as that stage fights: coated blows always, a guard with
 * a perfect moment from Flow, the slash from Edge, and a dash from Form once Aura Step exists. Its tells are a player's
 * cues: the guard's ring, the blade raised high before a slash, a crouch before a dash. It never dies in a duel: brought low
 * it yields on one knee. Outside a duel nothing harms it (it turns every blow aside, and fire, falls and drowning don't touch
 * it), and after a while it moves on.</p>
 */
public class Duelist extends AuraFighter {
	/** Its challenger, while a duel is on (server only). */
	@Nullable UUID opponent;
	/** When it moves on, when it can take another challenge, and (once beaten) when it goes. */
	long stayUntil;
	long restUntil;
	long leaveAt = -1;
	/** Its campfire, if it made one. */
	@Nullable BlockPos camp;
	/** Whether to raise its guard next tick (it learned the rhythm of the last blow). */
	private boolean guardNext;
	/** The last crescent of its challenger's it decided whether to answer (once a crescent). */
	private dev.wildercord.aura.Crescents.Flight considered;
	/** A dash: when its crouch ends and the dash goes, when the dash is over, and its way. */
	private long dashAt;
	private long dashUntil;
	private long dashReadyAt;
	private Vec3 dashDir = Vec3.ZERO;

	public Duelist(EntityType<? extends Duelist> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		setPersistenceRequired();
		setDropChance(EquipmentSlot.MAINHAND, 0.0F);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return net.minecraft.world.entity.Mob.createMobAttributes()
			.add(Attributes.ATTACK_KNOCKBACK, 0.4)
			.add(Attributes.MAX_HEALTH, AuraWorldRules.duelistHealth(AuraRules.GLOW))
			.add(Attributes.MOVEMENT_SPEED, 0.32)
			.add(Attributes.ATTACK_DAMAGE, 1.0)
			.add(Attributes.ARMOR, 4.0)
			.add(Attributes.FOLLOW_RANGE, 32.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true) {
			@Override
			public boolean canUse() {
				return fighting() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return fighting() && super.canContinueToUse();
			}

			@Override
			protected int getAttackInterval() {
				return adjustedTickDelay(AuraWorldRules.duelistAttackTicks(stage()));
			}
		});
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5) {
			@Override
			public boolean canUse() {
				return tournament==null && opponent == null && leaveAt < 0 && !state(SIT) && super.canUse();
			}
		});
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	/** Whether it may strike now: a duel's fight is on, and it isn't winding up, guarding, dashing or caught out. */
	private boolean fighting() {
		return opponent != null && DuelistDuels.fighting(this) && slashAt == 0 && !guarding() && !staggered() && dashAt == 0 && dashUntil == 0;
	}

	/** Its campfire, if it made one. */
	public @Nullable BlockPos camp() {
		return camp;
	}

	/** When it takes another challenge (game time). */
	public long restingUntil() {
		return restUntil;
	}

	public boolean inDuel() {
		return opponent != null;
	}

	BlockPos tournament;
	int tournamentSlot;
	void gather(BlockPos board,int slot,long until){tournament=board.immutable();tournamentSlot=slot;stayUntil=until;leaveAt=-1;restUntil=0;}

	/** Whether it's on its way out (beaten, bowing, about to go). */
	public boolean leaving() {
		return leaveAt >= 0;
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData group) {
		setMethod(BreathingMethods.BUILT_IN.get(getRandom().nextInt(BreathingMethods.BUILT_IN.size())));
		setStage(AuraRules.GLOW);
		stayUntil = level.getLevel().getGameTime() + AuraWorldRules.DUELIST_STAY;
		return super.finalizeSpawn(level, difficulty, reason, group);
	}

	// ------------------------------------------------------------------ talking

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		if (player instanceof ServerPlayer server) {
			if(tournament!=null) {
				if(server.level().hasChunkAt(tournament) && server.level().getBlockEntity(tournament) instanceof TournamentBoardEntity board)board.describe(server);
			} else if (server.isShiftKeyDown()) {
				if (!dev.wildercord.aura.MasterFormLessons.offer(server, this)) SwordMaster.introduce(server, this);
			} else {
				DuelistDuels.use(server, this);
				if (!inDuel() && !leaving() && SwordMaster.readyForTrial(server)) {
					server.sendSystemMessage(Component.translatable("message.wildercord.master.teacher_hint"));
					if (dev.wildercord.aura.MasterForms.eligibleLesson(server)) server.sendSystemMessage(Component.translatable("message.wildercord.wall_turn.teacher_hint"));
				}
			}
		}
		return InteractionResult.SUCCESS;
	}

	// ------------------------------------------------------------------ a duel

	/** The duel begins: it draws, takes the stage it will fight at, and turns to its challenger. */
	void begin(ServerPlayer player, int stage) {
		opponent = player.getUUID();
		setStage(stage);
		AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.setBaseValue(AuraWorldRules.duelistHealth(stage));
		}
		setHealth(getMaxHealth());
		setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(stage >= AuraRules.FORM ? Items.NETHERITE_SWORD : stage >= AuraRules.EDGE ? Items.DIAMOND_SWORD : Items.IRON_SWORD));
		coat();
		setState(SIT, false);
		setState(DRAWN, true);
		setState(BOW, false);
		setState(YIELD, false);
		setTarget(player);
		faceTarget(player);
		getNavigation().stop();
		guardRaised = -1;
		guardReadyAt = 0;
		nextSlashAt = level().getGameTime() + AuraWorldRules.duelistSlashCooldown(stage) / 2;
		dashReadyAt = level().getGameTime() + AuraWorldRules.DASH_COOLDOWN / 2;
		if (level() instanceof ServerLevel level) {
			Feels.sound(level, position().add(0, 1, 0), "duelist_challenge", 1.0F, 1.0F);
		}
	}

	/** The duel is over: it sheathes, mends, and bows. */
	void end(boolean beaten) {
		opponent = null;
		setTarget(null);
		dropGuard();
		slashAt = 0;
		slashAim = null;
		dashAt = 0;
		dashUntil = 0;
		setState(WINDUP, false);
		setState(DASH, false);
		setState(DRAWN, false);
		setState(YIELD, false);
		setState(BOW, true);
		setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		removeAllEffects();
		clearFire();
		setHealth(getMaxHealth());
		getNavigation().stop();
		long now = level().getGameTime();
		if (beaten) {
			leaveAt = now + AuraWorldRules.BOW_TICKS + 60;
		} else {
			restUntil = now + AuraWorldRules.REST_TICKS;
		}
		if (level() instanceof ServerLevel level) {
			Feels.sound(level, position().add(0, 1, 0), "duelist_sheathe", 0.9F, 1.0F);
			Feels.sound(level, position().add(0, 1, 0), "duelist_bow", 0.8F, 1.0F);
		}
	}

	/** It yields: down on one knee, its blade lowered. */
	void kneel() {
		setState(YIELD, true);
		dropGuard();
		slashAt = 0;
		setState(WINDUP, false);
		getNavigation().stop();
		if (level() instanceof ServerLevel level) {
			Feels.sound(level, position().add(0, 1, 0), "duelist_yield", 1.0F, 1.0F);
		}
	}

	// ------------------------------------------------------------------ harm

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		// The void and /kill take anything.
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, damage);
		}
		if (!DuelistDuels.counts(this, source)) {
			// Outside its duel nothing touches it: a blow is turned aside, the rest it walks through.
			if (source.getEntity() instanceof ServerPlayer player && source.getDirectEntity() == player) {
				turnAside(level, player);
			}
			if (isOnFire()) {
				clearFire();
			}
			return false;
		}
		float through = guarded(level, source, damage);
		if (through < 0) {
			return false;
		}
		if (AuraWorldRules.yields(getHealth(), through, getMaxHealth())) {
			setHealth((float) AuraWorldRules.yieldHealth(getMaxHealth()));
			hurtTime = 10;
			DuelistDuels.yielded(this);
			return true;
		}
		boolean hurt = super.hurtServer(level, source, through);
		if (hurt && !guarding() && getRandom().nextDouble() < AuraWorldRules.duelistGuardChance(stage())) {
			guardNext = true;
		}
		return hurt;
	}

	/** A blow outside a duel: turned aside with a flick of the sheathed blade. */
	private void turnAside(ServerLevel level, ServerPlayer player) {
		Vec3 at = position().add(0, 1.1, 0).add(player.position().subtract(position()).normalize().scale(0.6));
		Light.ring(level, at, player.position().subtract(position()).multiply(1, 0, 1).normalize(), 0xFFD54A, 0.1, 1.2, 0.05, 7);
		Feels.sound(level, at, "aura_guard", 0.6F, 1.2F);
		say(player, Component.translatable("message.wildercord.duelist.turns_aside", getDisplayName()).withColor(0xE8D8B0));
	}

	@Override
	protected void onStaggered(LivingEntity attacker) {
		DuelistDuels.harmed(this, attacker, net.minecraft.world.effect.MobEffects.SLOWNESS, net.minecraft.world.effect.MobEffects.WEAKNESS);
	}

	@Override
	public boolean isPushable() {
		return opponent != null && super.isPushable();
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	// ------------------------------------------------------------------ its slash, and what it may harm

	@Override
	protected double slashDamage() {
		return getAttributeValue(Attributes.ATTACK_DAMAGE) / (1 + AuraRules.COAT_BONUS) * AuraWorldRules.duelistSlashFactor(stage());
	}

	@Override
	protected double slashSpeed() {
		return AuraWorldRules.DUELIST_SLASH_SPEED;
	}

	@Override
	protected double slashRange() {
		return AuraWorldRules.DUELIST_SLASH_RANGE;
	}

	@Override
	protected double slashWidth() {
		return AuraWorldRules.DUELIST_SLASH_WIDTH;
	}

	/** Only its challenger: no third party is ever cut. */
	@Override
	protected Predicate<Entity> mayCut() {
		UUID foe = opponent;
		return e -> foe != null && e.getUUID().equals(foe);
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return opponent != null && target.getUUID().equals(opponent) && super.canAttack(target);
	}

	// ------------------------------------------------------------------ each tick

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		tickGuard(now);
		tickStagger(now);
		if (leaving()) {
			leave(level, now);
			return;
		}
		if (opponent == null) {
			if(tournament!=null && now%40==0){
				if(!level.hasChunkAt(tournament))return;
				if(!(level.getBlockEntity(tournament) instanceof TournamentBoardEntity board) || !board.hosts(getUUID())){vanish(level);return;}
				BlockPos home=board.waiting(tournamentSlot);
				if(blockPosition().distSqr(home)>4)getNavigation().moveTo(home.getX()+.5,home.getY(),home.getZ()+.5,.6);
			}
			idle(level, now);
			return;
		}
		tickSlash(level, now);
		LivingEntity target = getTarget();
		if (target == null || !DuelistDuels.fighting(this) || state(YIELD)) {
			getNavigation().stop();
			if (target != null) {
				faceTarget(target);
			}
			return;
		}
		if (tickDash(level, now, target) || slashAt > 0 || staggered()) {
			return;
		}
		int stage = stage();
		double d = distanceTo(target);
		if (guardNext && stage >= AuraRules.FLOW) {
			guardNext = false;
			raiseGuard();
		}
		if (guarding()) {
			faceTarget(target);
			getNavigation().stop();
			if (d > 5) {
				dropGuard();
			}
			return;
		}
		// A crescent of its challenger's coming at it, its own slash ready: now and then it answers with one of its own, and the two clash.
		if (stage >= AuraRules.EDGE && now >= nextSlashAt && answer(target)) {
			return;
		}
		if (stage >= AuraRules.EDGE && now >= nextSlashAt && d > 5.5 && d < AuraWorldRules.DUELIST_SLASH_RANGE - 2 && hasLineOfSight(target)) {
			windUp(target, AuraWorldRules.duelistSlashWindup(stage), AuraWorldRules.duelistSlashCooldown(stage));
			return;
		}
		if (stage >= AuraRules.FORM && AuraApi.technique("step").isPresent() && now >= dashReadyAt && d > 4.5 && d < 10 && hasLineOfSight(target)) {
			crouch(level, now, target);
			return;
		}
		// Close, between blows: now and then it braces instead of striking, so a rushed swing meets its perfect moment.
		if (stage >= AuraRules.FLOW && d < 3.5 && now % 10 == 0 && getRandom().nextDouble() < 0.18) {
			raiseGuard();
		}
	}

	/**
	 * Whether it answers a crescent of {@code target}'s coming at it with a slash of its own (its blade snapping up, a moment's tell), so the two
	 * meet in the air: decided once a crescent, by its stage's chance.
	 */
	private boolean answer(LivingEntity target) {
		Vec3 chest = position().add(0, 1.2, 0);
		for (dev.wildercord.aura.Crescents.Flight f : dev.wildercord.aura.Crescents.inFlight()) {
			if (f.caster() != target || f.done() || f.held() || f == considered) {
				continue;
			}
			Vec3 to = chest.subtract(f.front());
			double d = to.length();
			if (d < AuraWorldRules.ANSWER_NEAR || d > AuraWorldRules.ANSWER_FAR || f.aim().dot(to.scale(1 / d)) < AuraWorldRules.ANSWER_ONCOMING) {
				continue;
			}
			considered = f;
			if (getRandom().nextDouble() >= AuraWorldRules.duelistAnswerChance(stage())) {
				return false;
			}
			windUp(target, AuraWorldRules.ANSWER_WINDUP, AuraWorldRules.duelistSlashCooldown(stage()));
			return true;
		}
		return false;
	}

	/** Outside a duel: it sits by its fire when nobody is close, and moves on once its time is up. */
	private void idle(ServerLevel level, long now) {
		if (stayUntil > 0 && now >= stayUntil) {
			vanish(level);
			return;
		}
		boolean byFire = camp != null && level.getBlockState(camp).is(Blocks.CAMPFIRE) && blockPosition().distSqr(camp) < 9;
		Player near = level.getNearestPlayer(this, 4.5);
		boolean sit = byFire && near == null && getNavigation().isDone() && now >= restUntil - AuraWorldRules.REST_TICKS + 40;
		setState(SIT, sit);
		if (state(BOW) && now >= restUntil - AuraWorldRules.REST_TICKS + AuraWorldRules.BOW_TICKS) {
			setState(BOW, false);
		}
		if (camp != null && !byFire && getNavigation().isDone() && now % 100 == 0 && blockPosition().distSqr(camp) > 36) {
			// It keeps near its fire.
			getNavigation().moveTo(camp.getX() + 1.5, camp.getY(), camp.getZ() + 0.5, 0.6);
		}
	}

	/** Beaten: it bows to its victor, then walks a few steps away and is gone. */
	private void leave(ServerLevel level, long now) {
		long bowEnds = leaveAt - 60;
		if (now < bowEnds) {
			setState(BOW, true);
			getNavigation().stop();
			return;
		}
		setState(BOW, false);
		if (getNavigation().isDone()) {
			Vec3 away = position().add(getLookAngle().scale(-6)).add(getRandom().nextGaussian() * 3, 0, getRandom().nextGaussian() * 3);
			getNavigation().moveTo(away.x, away.y, away.z, 0.6);
		}
		if (now >= leaveAt) {
			vanish(level);
		}
	}

	/** It goes: a swirl of its aura, and nothing left behind (its campfire goes too). */
	public void vanish(ServerLevel level) {
		Vec3 heart = position().add(0, 1.0, 0);
		dev.wildercord.aura.AuraFx.groundScar(level, position(), 2.0, 60, 0);
		Light.ray(level, position(), position().add(0, 3.5, 0), auraColor(), 0.3, 12);
		Motes.burst(level, heart, 18, AuraVfx.hot(auraColor(), 0.25), 0.1, 26, 0.18);
		Feels.sound(level, heart, "aura_breath", 0.8F, Feels.step(4));
		if (camp != null && level.isLoaded(camp) && level.getBlockState(camp).is(Blocks.CAMPFIRE)) {
			level.setBlockAndUpdate(camp, Blocks.AIR.defaultBlockState());
		}
		DuelistDuels.gone(this);
		discard();
	}

	// ------------------------------------------------------------------ the dash (from Form, once Aura Step exists)

	/** The dash's tell: a low crouch, then it goes. */
	private void crouch(ServerLevel level, long now, LivingEntity target) {
		dashAt = now + AuraWorldRules.DASH_WINDUP;
		dashReadyAt = now + AuraWorldRules.DASH_COOLDOWN;
		Vec3 to = target.position().subtract(position()).multiply(1, 0, 1);
		dashDir = to.lengthSqr() < 1.0E-4 ? flatLook() : to.normalize();
		setState(DASH, true);
		getNavigation().stop();
		Feels.sound(level, position(), "aura_breath", 0.7F, Feels.step(1));
	}

	/** The dash under way; returns whether it's busy with it this tick. */
	private boolean tickDash(ServerLevel level, long now, LivingEntity target) {
		if (dashAt > 0) {
			faceTarget(target);
			if (now < dashAt) {
				return true;
			}
			dashAt = 0;
			dashUntil = now + 4;
			Motes.glows(level, position().add(0, 1, 0), 6, 0.4, AuraVfx.hot(auraColor(), 0.3), 0.1, 14, Vec3.ZERO, 0.01);
		}
		if (dashUntil > 0) {
			double speed = AuraWorldRules.DASH_DISTANCE / 4.0;
			setDeltaMovement(dashDir.x * speed, Math.max(0.05, getDeltaMovement().y), dashDir.z * speed);
			needsSync = true;
			Motes.glows(level, position().add(0, 1, 0), 2, 0.3, auraColor(), 0.09, 10, Vec3.ZERO, 0.01);
			if (now >= dashUntil) {
				dashUntil = 0;
				setState(DASH, false);
			}
			return true;
		}
		return false;
	}

	// ------------------------------------------------------------------ saved

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putLong("stay_until", stayUntil);
		output.putLong("rest_until", restUntil);
		if(tournament!=null){output.putLong("tournament",tournament.asLong());output.putInt("tournament_slot",tournamentSlot);}
		if (camp != null) {
			output.putLong("camp", camp.asLong());
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		stayUntil = input.getLongOr("stay_until", 0L);
		restUntil = input.getLongOr("rest_until", 0L);
		long hosted=input.getLongOr("tournament",Long.MIN_VALUE);tournament=hosted==Long.MIN_VALUE?null:BlockPos.of(hosted);tournamentSlot=Math.clamp(input.getIntOr("tournament_slot",0),0,2);
		long campAt = input.getLongOr("camp", Long.MIN_VALUE);
		camp = campAt == Long.MIN_VALUE ? null : BlockPos.of(campAt);
		// A duel never survives a reload: whatever it was doing, it's done.
		setState(DRAWN, false);
		setState(WINDUP, false);
		setState(GUARD, false);
		setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
	}

	// ------------------------------------------------------------------ sounds

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.PLAYER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.PLAYER_DEATH;
	}
}
