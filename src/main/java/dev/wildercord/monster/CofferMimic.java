package dev.wildercord.monster;

import dev.wildercord.cast.Targets;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * The Coffer Mimic: an iron-bound oak coffer that is nothing of the kind. Shut, it sits square to the world on its spot
 * and doesn't stir: no sound, no shove, no turning to look. But come within a few blocks and now and then its lid lifts a
 * hair and settles again with a faint creak, and a drop of drool runs out under it (the tell). Open it, strike it, or
 * stand right beside it too long, and the lid flies open on rows of teeth and it bites whoever is there.
 *
 * <p>Awake, it scuttles on four stubby legs. Up close it snaps its lid; a few blocks off it shoots its long tongue out and
 * hauls its prey in to be bitten; further off it crouches, its lid chattering, and leaps. A missed leap lands it on its
 * back, legs kicking, soft to blows. Hurt hard and fast, it clamps shut and turns most of what lands. With no one left to
 * chase, it walks back to its spot, squares itself up, and shuts.</p>
 *
 * <p>It is fond of books. Feed it one (shut or awake) and now and then it takes to whoever fed it: it never bites its owner,
 * follows them about, shutting up as a coffer beside them whenever they stop, and turns up at their side if left far
 * behind. Its owner can open it like a chest (it keeps what's put in it, and spills it all if it dies), stroke it by
 * sneaking up to it empty-handed, or tell it to stay put by sneaking up to it holding something else.</p>
 */
public class CofferMimic extends WildMonster implements OwnableEntity {
	private static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> DATA_OWNER =
		SynchedEntityData.defineId(CofferMimic.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);

	private enum Phase { DISGUISED, WAKE, HUNT, BITE, LASH, CROUCH, LUNGE, FLIPPED, CLAMP }

	private Phase phase = Phase.DISGUISED;
	private int phaseTicks;
	private @Nullable BlockPos home;
	private float homeYaw;
	private int lingered;
	private long nextTwitch;
	private long twitchUntil;
	private long nextBite;
	private long nextLash;
	private long nextLunge;
	private long nextClamp;
	private float recent;
	private long recentSince;
	private int idle;
	private int homing;
	private int repath;
	/** On the client: how shut it is, eased (1 a coffer, 0 awake). */
	private float shut = 1;
	private float shutO = 1;
	private boolean seen;
	private boolean staying;
	private boolean heeling;
	private int opened;
	/** A tamed one's belly: a chest's worth, kept with it and spilled when it dies. */
	private final SimpleContainer belly = new SimpleContainer(MimicRules.STORAGE_ROWS * 9) {
		@Override
		public boolean stillValid(Player player) {
			return MimicRules.opensFor(isAlive() && !isRemoved(), isOwnedBy(player), player.distanceTo(CofferMimic.this));
		}

		@Override
		public void startOpen(ContainerUser user) {
			if (opened++ == 0 && level() instanceof ServerLevel server) {
				play(server, SoundEvents.CHEST_OPEN, 0.6F, 1.0F);
			}
		}

		@Override
		public void stopOpen(ContainerUser user) {
			if (opened > 0 && --opened == 0 && level() instanceof ServerLevel server) {
				play(server, SoundEvents.CHEST_CLOSE, 0.6F, 1.0F);
			}
		}
	};

	public CofferMimic(EntityType<? extends CofferMimic> type, Level level) {
		super(type, level);
		this.xpReward = 12;
		setState(VEILED, true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 36.0)
			.add(Attributes.MOVEMENT_SPEED, 0.27)
			.add(Attributes.ATTACK_DAMAGE, 5.0)
			.add(Attributes.ARMOR, 4.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
			.add(Attributes.FOLLOW_RANGE, 24.0)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_OWNER, Optional.empty());
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (target, level) -> !disguised() && !tamed()));
	}

	// ------------------------------------------------------------------ owner

	@Override
	public @Nullable EntityReference<LivingEntity> getOwnerReference() {
		return entityData.get(DATA_OWNER).orElse(null);
	}

	/** Whether it has taken to someone. */
	public boolean tamed() {
		return getOwnerReference() != null;
	}

	public boolean isOwnedBy(Player player) {
		EntityReference<LivingEntity> owner = getOwnerReference();
		return owner != null && owner.getUUID().equals(player.getUUID());
	}

	/** Whether its owner has told it to stay put. */
	public boolean staying() {
		return staying;
	}

	/** Its belly (only its owner opens it). */
	public SimpleContainer belly() {
		return belly;
	}

	/** Takes to this player: it drops whatever it was after, and won't wander off or despawn. */
	public void tame(Player player) {
		entityData.set(DATA_OWNER, Optional.of(EntityReference.<LivingEntity>of(player.getUUID())));
		setTarget(null);
		setLastHurtByMob(null);
		setPersistenceRequired();
		staying = false;
	}

	/** Its owner, if they're in this world with it. */
	private @Nullable Player ownerHere(ServerLevel level) {
		EntityReference<LivingEntity> owner = getOwnerReference();
		return owner == null ? null : level.getPlayerByUUID(owner.getUUID());
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return !(target instanceof Player player && isOwnedBy(player)) && super.canAttack(target);
	}

	@Override
	public void checkDespawn() {
		// A tamed one (and all it holds) stays, even in peaceful.
		if (!tamed() || level().getDifficulty() != Difficulty.PEACEFUL) {
			super.checkDespawn();
		}
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.TOUCH, Runes.HARM), List.of(Runes.BOLT, Runes.WITHER));
	}

	/** Whether it's shut up as a coffer (both sides). */
	public boolean disguised() {
		return state(VEILED);
	}

	/** How shut it is on the client, eased: 1 a coffer, 0 awake. */
	public float shut(float partial) {
		return Mth.lerp(partial, shutO, shut);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			shutO = shut;
			float to = disguised() ? 1 : 0;
			if (!seen) {
				// Come into view shut, it's shut already: it doesn't close in front of anyone.
				seen = true;
				shut = to;
				shutO = to;
			} else {
				shut = Mth.approach(shut, to, to > shut ? 0.08F : 0.4F);
			}
		}
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		settleHere(getYRot());
		return data;
	}

	/** Makes wherever it stands its spot, squares itself up and shuts. */
	public void settleHere(float yaw) {
		home = blockPosition();
		homeYaw = MimicRules.snapYaw(yaw);
		setPos(home.getX() + 0.5, getY(), home.getZ() + 0.5);
		square();
		getNavigation().stop();
		setTarget(null);
		enter(Phase.DISGUISED);
		lingered = 0;
		idle = 0;
		homing = 0;
		setState(VEILED, true);
	}

	private void square() {
		setYRot(homeYaw);
		setYBodyRot(homeYaw);
		setYHeadRot(homeYaw);
		yRotO = homeYaw;
		yBodyRotO = homeYaw;
		yHeadRotO = homeYaw;
	}

	private void enter(Phase next) {
		phase = next;
		phaseTicks = 0;
	}

	// ------------------------------------------------------------------ shut

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		if (home == null) {
			home = blockPosition();
			homeYaw = MimicRules.snapYaw(getYRot());
		}
		phaseTicks++;
		LivingEntity target = getTarget();
		boolean live = target != null && target.isAlive() && Targets.canHarm(this, target);
		switch (phase) {
			case DISGUISED -> lieStill(level, now);
			case WAKE -> {
				getNavigation().stop();
				if (live) {
					face(target);
				}
				if (phaseTicks == MimicRules.WAKE_BITE_AT) {
					ambush(level);
				}
				if (phaseTicks >= MimicRules.WAKE_TICKS) {
					enter(Phase.HUNT);
				}
			}
			case HUNT -> hunt(level, target, live, now);
			case BITE -> {
				getNavigation().stop();
				if (live) {
					face(target);
				}
				if (phaseTicks == MimicRules.BITE_AT) {
					play(level, SoundEvents.EVOKER_FANGS_ATTACK, 0.9F, 1.3F);
					if (live && distanceTo(target) <= MimicRules.BITE_REACH + 0.6) {
						doHurtTarget(level, target);
					}
				}
				if (phaseTicks >= MimicRules.BITE_TICKS) {
					enter(Phase.HUNT);
				}
			}
			case LASH -> {
				getNavigation().stop();
				if (live) {
					face(target);
				}
				if (phaseTicks == MimicRules.LASH_AT && live && hasLineOfSight(target) && distanceTo(target) <= MimicRules.LASH_MAX + 1.0) {
					pull(level, target, now);
				}
				if (phaseTicks >= MimicRules.LASH_TICKS) {
					enter(Phase.HUNT);
				}
			}
			case CROUCH -> {
				getNavigation().stop();
				if (!live) {
					enter(Phase.HUNT);
				} else {
					face(target);
					if (phaseTicks % 4 == 0) {
						// The lid chattering: the tell before it leaps.
						play(level, SoundEvents.CHEST_CLOSE, 0.5F, 1.6F + random.nextFloat() * 0.3F);
					}
					if (phaseTicks >= MimicRules.LUNGE_CROUCH) {
						if (hasLineOfSight(target)) {
							leap(level, target);
						} else {
							enter(Phase.HUNT);
						}
					}
				}
			}
			case LUNGE -> {
				if (live && getBoundingBox().inflate(0.4).intersects(target.getBoundingBox())) {
					if (target.hurtServer(level, damageSources().mobAttack(this), MimicRules.LUNGE_DAMAGE)) {
						MonsterMagic.knock(target, position(), 0.7);
					}
					play(level, SoundEvents.EVOKER_FANGS_ATTACK, 1.0F, 1.0F);
					setDeltaMovement(getDeltaMovement().scale(0.2));
					enter(Phase.HUNT);
					nextBite = now + 10;
				} else if ((onGround() && phaseTicks > 4) || phaseTicks > 30) {
					// It missed: it comes down on its back, legs kicking, and for a while it's soft.
					enter(Phase.FLIPPED);
					play(level, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 0.8F, 0.8F);
					splinters(level, 10);
				}
			}
			case FLIPPED -> {
				getNavigation().stop();
				if (phaseTicks >= MimicRules.FLIPPED) {
					enter(Phase.HUNT);
					setDeltaMovement(getDeltaMovement().add(0, 0.3, 0));
					play(level, SoundEvents.CHEST_CLOSE, 0.8F, 0.7F);
				}
			}
			case CLAMP -> {
				getNavigation().stop();
				if (phaseTicks >= MimicRules.CLAMP_TICKS) {
					enter(Phase.HUNT);
					play(level, SoundEvents.CHEST_OPEN, 0.9F, 0.7F);
					// It comes out of it biting.
					nextBite = now;
				}
			}
		}
		setState(WINDUP, phase == Phase.CROUCH || (phase == Phase.DISGUISED && now < twitchUntil));
		setState(ACTING, phase == Phase.BITE || phase == Phase.LUNGE || (phase == Phase.WAKE && phaseTicks <= MimicRules.WAKE_BITE_AT + 2));
		setState(ALT, phase == Phase.LASH);
		setState(GUARD, phase == Phase.CLAMP);
		setState(STUNNED, phase == Phase.FLIPPED);
	}

	/** Shut: holds still and square, heals, and twitches now and then when someone's about. */
	private void lieStill(ServerLevel level, long now) {
		getNavigation().stop();
		setDeltaMovement(0, getDeltaMovement().y, 0);
		square();
		if (now % 20 == 0 && getHealth() < getMaxHealth()) {
			heal(1.0F);
		}
		if (tamed()) {
			// Its owner's own coffer: it only stirs to follow them.
			Player owner = ownerHere(level);
			if (owner != null && !owner.isSpectator() && MimicRules.heel(distanceTo(owner), staying, false) != MimicRules.Heel.STAY) {
				rouse(level);
			}
			return;
		}
		Player near = level.getNearestPlayer(getX(), getY(), getZ(), MimicRules.TWITCH_RANGE, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
		if (near == null) {
			lingered = 0;
			nextTwitch = Math.max(nextTwitch, now + 20);
			return;
		}
		double flat = Math.sqrt(Mth.square(near.getX() - getX()) + Mth.square(near.getZ() - getZ()));
		double dy = near.getY() - getY();
		lingered = flat <= MimicRules.WAKE_REACH && Math.abs(dy) <= 1.5 ? lingered + 1 : Math.max(0, lingered - 1);
		if (MimicRules.wakes(flat, dy, lingered)) {
			wake(level, near);
			return;
		}
		if (now >= nextTwitch) {
			twitchUntil = now + MimicRules.TWITCH_TICKS;
			nextTwitch = now + MimicRules.TWITCH_MIN + random.nextInt(MimicRules.TWITCH_SPREAD);
			play(level, SoundEvents.CHEST_OPEN, 0.12F, 1.7F);
			Vec3 front = getLookAngle().multiply(1, 0, 1).normalize().scale(0.46);
			level.sendParticles(ParticleTypes.FALLING_WATER, getX() + front.x, getY() + 0.55, getZ() + front.z, 1, 0.12, 0, 0.12, 0);
		}
	}

	/** The lid flies open: it's no coffer. */
	private void wake(ServerLevel level, @Nullable LivingEntity by) {
		setState(VEILED, false);
		enter(Phase.WAKE);
		lingered = 0;
		idle = 0;
		if (by != null && canAttack(by) && Targets.canHarm(this, by)) {
			setTarget(by);
		}
		play(level, SoundEvents.CHEST_OPEN, 1.0F, 0.55F);
		play(level, SoundEvents.RAVAGER_ROAR, 0.5F, 1.7F);
		splinters(level, 14);
	}

	/** A tamed one getting up to follow its owner: the lid lifts quietly, and no bite. */
	private void rouse(ServerLevel level) {
		setState(VEILED, false);
		enter(Phase.HUNT);
		idle = 0;
		heeling = true;
		play(level, SoundEvents.CHEST_OPEN, 0.4F, 1.2F);
	}

	/** A tamed one with no one to fight: it keeps by its owner, and left alone beside them, shuts. */
	private void heel(ServerLevel level) {
		Player owner = ownerHere(level);
		MimicRules.Heel heel = owner == null || owner.isSpectator() ? MimicRules.Heel.STAY : MimicRules.heel(distanceTo(owner), staying, heeling);
		switch (heel) {
			case BLINK -> {
				if (!blinkTo(level, owner)) {
					getNavigation().moveTo(owner, 1.2);
				}
			}
			case WALK -> {
				heeling = true;
				idle = 0;
				if (--repath <= 0) {
					getNavigation().moveTo(owner, 1.2);
					repath = 10;
				}
			}
			case STAY -> {
				heeling = false;
				getNavigation().stop();
				if (owner != null) {
					getLookControl().setLookAt(owner, 30, 30);
				}
				if (++idle >= MimicRules.SETTLE_BESIDE) {
					settleHere(owner == null ? getYRot() : (float) Math.toDegrees(Math.atan2(getX() - owner.getX(), owner.getZ() - getZ())));
				}
			}
		}
	}

	/** Turns up on open ground beside its owner, as a dog does; false if there's nowhere to stand. */
	private boolean blinkTo(ServerLevel level, Player owner) {
		BlockPos at = owner.blockPosition();
		for (int i = 0; i < 10; i++) {
			BlockPos to = at.offset(random.nextInt(5) - 2, random.nextInt(3) - 1, random.nextInt(5) - 2);
			if (Math.abs(to.getX() - at.getX()) < 2 && Math.abs(to.getZ() - at.getZ()) < 2) {
				continue;
			}
			if (level.getBlockState(to.below()).isFaceSturdy(level, to.below(), Direction.UP)
					&& level.noCollision(this, getBoundingBox().move(to.getX() + 0.5 - getX(), to.getY() - getY(), to.getZ() + 0.5 - getZ()))) {
				teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
				getNavigation().stop();
				return true;
			}
		}
		return false;
	}

	/** Just woken, it bites whoever is right there. */
	private void ambush(ServerLevel level) {
		play(level, SoundEvents.EVOKER_FANGS_ATTACK, 1.0F, 0.9F);
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(MimicRules.AMBUSH_REACH),
				e -> e != this && !(e instanceof CofferMimic) && (e instanceof Player || e == getTarget()) && canAttack(e) && Targets.canHarm(this, e))) {
			if (victim.hurtServer(level, damageSources().mobAttack(this), MimicRules.AMBUSH_DAMAGE)) {
				MonsterMagic.knock(victim, position(), 0.5);
			}
		}
	}

	// ------------------------------------------------------------------ awake

	private void hunt(ServerLevel level, @Nullable LivingEntity target, boolean live, long now) {
		if (!live) {
			if (tamed()) {
				heel(level);
				return;
			}
			// No one left to chase: in a while, it goes home and shuts.
			if (++idle >= MimicRules.SETTLE_AFTER) {
				goHome();
			}
			return;
		}
		idle = 0;
		homing = 0;
		double d = distanceTo(target);
		getLookControl().setLookAt(target, 30, 30);
		switch (MimicRules.choose(d, hasLineOfSight(target), now >= nextBite, now >= nextLash, now >= nextLunge)) {
			case BITE -> {
				enter(Phase.BITE);
				nextBite = now + MimicRules.BITE_COOLDOWN;
				play(level, SoundEvents.CHEST_OPEN, 0.7F, 1.3F);
			}
			case LASH -> {
				enter(Phase.LASH);
				nextLash = now + MimicRules.LASH_COOLDOWN;
				play(level, SoundEvents.FROG_TONGUE, 1.0F, 0.6F);
			}
			case LUNGE -> {
				enter(Phase.CROUCH);
				nextLunge = now + MimicRules.LUNGE_COOLDOWN;
			}
			case CHASE -> {
				if (--repath <= 0) {
					getNavigation().moveTo(target, 1.2);
					repath = 8;
				}
			}
		}
	}

	private void goHome() {
		if (home == null) {
			settleHere(getYRot());
			return;
		}
		homing++;
		double flat = Math.sqrt(Mth.square(home.getX() + 0.5 - getX()) + Mth.square(home.getZ() + 0.5 - getZ()));
		if (flat <= MimicRules.HOME_REACH && Math.abs(getY() - home.getY()) < 1.0) {
			settleHere(homeYaw);
		} else if (homing >= MimicRules.SETTLE_GIVE_UP) {
			// It can't get back: here will do.
			settleHere(getYRot());
		} else if (homing % 40 == 1 || getNavigation().isDone()) {
			getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.9);
		}
	}

	/** Its tongue shoots out and hauls its prey in, and its next bite is ready. */
	private void pull(ServerLevel level, LivingEntity target, long now) {
		target.hurtServer(level, damageSources().mobAttack(this), MimicRules.LASH_DAMAGE);
		Vec3 to = position().subtract(target.position());
		double flat = Math.sqrt(to.x * to.x + to.z * to.z);
		if (flat > 1.0E-3) {
			double power = 0.45 + 0.12 * flat;
			target.setDeltaMovement(0, 0, 0);
			MonsterMagic.shove(target, new Vec3(to.x / flat * power, 0.28, to.z / flat * power));
		}
		play(level, SoundEvents.SLIME_ATTACK, 0.8F, 0.7F);
		nextBite = now;
	}

	private void leap(ServerLevel level, LivingEntity target) {
		Vec3 d = target.position().subtract(position());
		double flat = Math.sqrt(d.x * d.x + d.z * d.z);
		double speed = Math.min(1.2, 0.15 * flat + 0.3);
		Vec3 dir = flat < 1.0E-3 ? getLookAngle() : new Vec3(d.x / flat, 0, d.z / flat);
		setDeltaMovement(dir.x * speed, 0.42 + 0.02 * flat + Math.max(0, d.y) * 0.08, dir.z * speed);
		needsSync = true;
		enter(Phase.LUNGE);
		play(level, SoundEvents.CHEST_OPEN, 1.0F, 0.8F);
	}

	private void splinters(ServerLevel level, int count) {
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.defaultBlockState()), getX(), getY() + 0.6, getZ(), count,
			0.3, 0.3, 0.3, 0.1);
	}

	private void play(ServerLevel level, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, getX(), getY(), getZ(), sound, SoundSource.HOSTILE, volume, pitch);
	}

	// ------------------------------------------------------------------ being struck, being opened

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean wasShut = disguised();
		float scaled = damage * MimicRules.damageScale(phase == Phase.CLAMP, phase == Phase.FLIPPED);
		boolean hurt = super.hurtServer(level, source, scaled);
		if (!hurt) {
			return false;
		}
		LivingEntity by = source.getEntity() instanceof LivingEntity l ? l : null;
		if (wasShut) {
			wake(level, by);
			return true;
		}
		long now = level.getGameTime();
		if (phase == Phase.CLAMP) {
			play(level, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.6F, 1.5F);
		}
		if (now - recentSince > MimicRules.CLAMP_WINDOW) {
			recent = 0;
			recentSince = now;
		}
		recent += scaled;
		if ((phase == Phase.HUNT || phase == Phase.BITE || phase == Phase.LASH) && now >= nextClamp && MimicRules.clamps(recent, getMaxHealth())) {
			enter(Phase.CLAMP);
			nextClamp = now + MimicRules.CLAMP_COOLDOWN;
			recent = 0;
			play(level, SoundEvents.CHEST_CLOSE, 1.0F, 0.6F);
		}
		return true;
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!(level() instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		ItemStack held = player.getItemInHand(hand);
		if (held.is(ItemTags.BOOKSHELF_BOOKS) && !tamed()) {
			feed(server, player, held);
			return InteractionResult.SUCCESS;
		}
		// Its owner's plain book mends it; at full health, or with any other book, it opens as usual.
		if (tamed() && isOwnedBy(player) && MimicRules.mends(held.is(Items.BOOK), player.isSecondaryUseActive(), getHealth(), getMaxHealth())) {
			feed(server, player, held);
			return InteractionResult.SUCCESS;
		}
		if (tamed()) {
			if (!isOwnedBy(player)) {
				// Someone else's: it stays shut on them.
				player.sendOverlayMessage(Component.translatable("message.wildercord.coffer_mimic.not_yours").withColor(0xC4BEB2));
				play(server, SoundEvents.CHEST_LOCKED, 0.6F, 1.2F);
				return InteractionResult.SUCCESS;
			}
			if (player.isSecondaryUseActive()) {
				if (held.isEmpty()) {
					pet(server, player);
				} else {
					staying = !staying;
					heeling = false;
					getNavigation().stop();
					player.sendOverlayMessage(Component.translatable(staying ? "message.wildercord.coffer_mimic.stay" : "message.wildercord.coffer_mimic.follow")
						.withColor(0xE2DCCC));
				}
				return InteractionResult.SUCCESS;
			}
			player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, belly), getDisplayName()));
			return InteractionResult.SUCCESS;
		}
		if (disguised()) {
			wake(server, player);
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	/** Eats a book: an untamed one may take to whoever fed it; its owner's book mends it a little. */
	private void feed(ServerLevel level, Player player, ItemStack book) {
		book.consume(1, player);
		play(level, SoundEvents.BOOK_PAGE_TURN, 1.0F, 0.7F);
		play(level, SoundEvents.GENERIC_EAT.value(), 0.7F, 0.8F);
		if (tamed()) {
			heal(MimicRules.BOOK_HEAL);
			level.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.0, getZ(), 2, 0.3, 0.2, 0.3, 0);
			return;
		}
		if (MimicRules.tames(random.nextFloat())) {
			tame(player);
			heal(MimicRules.BOOK_HEAL);
			level.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.0, getZ(), 7, 0.4, 0.3, 0.4, 0);
			player.sendOverlayMessage(Component.translatable("message.wildercord.coffer_mimic.tamed").withColor(0xE8B820));
		} else {
			level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.0, getZ(), 7, 0.4, 0.3, 0.4, 0.02);
		}
	}

	/** Its owner strokes it: its lid lifts and it purrs (as much as a coffer can). */
	private void pet(ServerLevel level, Player player) {
		twitchUntil = level.getGameTime() + MimicRules.TWITCH_TICKS * 2;
		level.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.0, getZ(), 3, 0.3, 0.2, 0.3, 0);
		play(level, SoundEvents.CAT_PURR_BABY.value(), 1.0F, 0.5F);
		play(level, SoundEvents.CHEST_OPEN, 0.3F, 1.6F);
		player.sendOverlayMessage(Component.translatable("message.wildercord.coffer_mimic.pet").withColor(0xE2DCCC));
	}

	@Override
	protected void dropEquipment(ServerLevel level) {
		super.dropEquipment(level);
		Containers.dropContents(level, this, belly);
	}

	@Override
	public boolean isPushable() {
		return !disguised() && super.isPushable();
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		EntityReference.store(getOwnerReference(), output, "Owner");
		output.putBoolean("Staying", staying);
		ValueOutput.TypedOutputList<ItemStackWithSlot> items = output.list("Belly", ItemStackWithSlot.CODEC);
		for (int i = 0; i < belly.getContainerSize(); i++) {
			ItemStack stack = belly.getItem(i);
			if (!stack.isEmpty()) {
				items.add(new ItemStackWithSlot(i, stack));
			}
		}
		output.putBoolean("Disguised", disguised());
		if (home != null) {
			output.putBoolean("HasHome", true);
			output.putInt("HomeX", home.getX());
			output.putInt("HomeY", home.getY());
			output.putInt("HomeZ", home.getZ());
			output.putFloat("HomeYaw", homeYaw);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(DATA_OWNER, Optional.ofNullable(EntityReference.read(input, "Owner")));
		staying = input.getBooleanOr("Staying", false);
		belly.clearContent();
		for (ItemStackWithSlot item : input.listOrEmpty("Belly", ItemStackWithSlot.CODEC)) {
			if (item.isValidInContainer(belly.getContainerSize())) {
				belly.setItem(item.slot(), item.stack());
			}
		}
		boolean shutUp = input.getBooleanOr("Disguised", true);
		setState(VEILED, shutUp);
		phase = shutUp ? Phase.DISGUISED : Phase.HUNT;
		if (input.getBooleanOr("HasHome", false)) {
			home = new BlockPos(input.getIntOr("HomeX", 0), input.getIntOr("HomeY", 0), input.getIntOr("HomeZ", 0));
			homeYaw = input.getFloatOr("HomeYaw", 0);
		}
	}

	// ------------------------------------------------------------------ sounds

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return disguised() ? null : SoundEvents.RAVAGER_AMBIENT;
	}

	@Override
	public float getVoicePitch() {
		return 1.6F + random.nextFloat() * 0.2F;
	}

	@Override
	protected float getSoundVolume() {
		return 0.5F;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState block) {
		playSound(SoundEvents.WOOD_STEP, 0.3F, 1.3F);
	}
}
