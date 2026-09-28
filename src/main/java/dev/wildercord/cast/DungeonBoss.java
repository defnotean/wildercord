package dev.wildercord.cast;

import dev.wildercord.content.RuneItem;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellNames;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * What the dimension dungeons' bosses share, after the Archivist: a Cord of spells that changes
 * with each of three phases (at two thirds and one third of its health), every spell named on the
 * boss bar and telegraphed with its own circle, a pause between phases while it gathers itself
 * (untouchable), a keeper's leash to its arena, a slow death, and its loot: a Tier IV rune its
 * killer doesn't know yet (in code) and the rest from its loot table
 * ({@code wildercord:entities/<id>}). Each boss adds one mechanic that teaches something about
 * magic: see {@link CinderWarden}.
 *
 * <p>The client only needs a few bits of its state for poses and glows; they're synced in one byte
 * ({@link #CASTING} and friends) and eased on the client like the Archivist's.</p>
 */
public abstract class DungeonBoss extends Monster {
	/** Telegraphing a spell. */
	public static final int CASTING = 1;
	/** Between phases: gathering itself, untouchable. */
	public static final int SHIFTING = 2;
	/** Its signature defence is up (the Star-Eater's shard shield). */
	public static final int GUARDED = 4;
	/** Open to harm: shield broken, stranded, or cracked open. */
	public static final int EXPOSED = 8;
	/** A heavy melee blow on its way. */
	public static final int SLAMMING = 16;

	private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(DungeonBoss.class, EntityDataSerializers.BYTE);

	protected static final int TELEGRAPH = 28;
	protected static final int SHIFT_TICKS = 60;
	/** How long it takes to die. */
	public static final int DEATH_TICKS = 60;

	protected final ServerBossEvent bossEvent;
	protected BlockPos home;
	protected int phase = 1;
	protected int shifting;
	protected long readyAt;
	protected long castAt;
	protected List<RuneDef> casting;
	protected LivingEntity castTarget;
	private int spellIndex;
	private Component shownName;

	private float castPose;
	private float castPoseO;
	private float shiftPose;
	private float shiftPoseO;
	private float guardPose;
	private float guardPoseO;
	private float exposedPose;
	private float exposedPoseO;
	private float slamPose;
	private float slamPoseO;

	protected DungeonBoss(EntityType<? extends DungeonBoss> type, Level level, BossEvent.BossBarColor color) {
		super(type, level);
		this.xpReward = 0;
		this.bossEvent = new ServerBossEvent(getUUID(), getType().getDescription(), color, BossEvent.BossBarOverlay.NOTCHED_10);
		this.bossEvent.setDarkenScreen(true);
		setPersistenceRequired();
	}

	// ------------------------------------------------------------------ what each boss says about itself

	/** The spells it casts in a phase (1 to 3), in turn. */
	protected abstract List<List<RuneDef>> spells(int phase);

	/** The Grimoire feat everyone near earns when it falls. */
	protected abstract String feat();

	/** Its colour, for its circles and flashes. */
	protected abstract int color();

	/** A second colour, for the rims of its circles. */
	protected abstract int accent();

	/** The flourish (and any allies) as it enters {@code phase}; it's untouchable for {@link #SHIFT_TICKS}. */
	protected abstract void onPhase(ServerLevel level, int phase);

	/** Each tick of the pause between phases. */
	protected void shiftTick(ServerLevel level, int left) {
	}

	/** Its signature mechanic, every tick it's awake and not between phases. */
	protected void mechanic(ServerLevel level, long now) {
	}

	/** How it moves toward (or around) its target when it isn't casting. */
	protected abstract void approach(ServerLevel level, LivingEntity target);

	/** Whether it may start a new spell now (the Warden won't while it's winding up a blow). */
	protected boolean mayCast(ServerLevel level, LivingEntity target) {
		return true;
	}

	/** How far off it will cast from. */
	protected double castRange() {
		return 26.0;
	}

	/** How far from its arena's heart it may go before it's pulled back. */
	protected double leash() {
		return 22.0;
	}

	/** Ticks between the end of one spell and the start of the next. */
	protected int pause(ServerLevel level) {
		return (phase == 3 ? 40 : phase == 2 ? 50 : 60) + level.getRandom().nextInt(30);
	}

	/**
	 * The damage it actually takes from {@code source}, or 0 to shrug it off. The base passes it on
	 * unchanged; bosses with a signature defence decide here.
	 */
	protected float resist(ServerLevel level, DamageSource source, float damage) {
		return damage;
	}

	/** What it's doing, for the boss bar when it isn't casting (null: just its name). */
	protected Component status() {
		return null;
	}

	/** Death throes, each tick of {@link #DEATH_TICKS}. */
	protected void dying(ServerLevel level, int tick) {
	}

	/** Its final moment, when it's gone. */
	protected void vanish(ServerLevel level) {
	}

	/** A runes-to-the-power spell's strength: Runebound's by difficulty, a little over. */
	protected double power() {
		return Runebound.power((ServerLevel) level()) * 1.1;
	}

	public int phase() {
		return phase;
	}

	public BlockPos home() {
		return home;
	}

	// ------------------------------------------------------------------ waking

	/** Sets it up over its altar: its home, its first pause, then into the world. */
	protected static <T extends DungeonBoss> T place(ServerLevel level, EntityType<T> type, BlockPos altar, double lift) {
		T boss = type.create(level, EntitySpawnReason.TRIGGERED);
		if (boss == null) {
			return null;
		}
		Vec3 at = Vec3.atBottomCenterOf(altar).add(0, lift, 0);
		boss.snapTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360, 0);
		boss.home = altar.immutable();
		boss.readyAt = level.getGameTime() + 60;
		level.addFreshEntity(boss);
		return boss;
	}

	/** Tells everyone near. */
	protected void announce(ServerLevel level, String key, int color) {
		for (ServerPlayer player : level.players()) {
			if (player.distanceTo(this) < 48) {
				player.sendSystemMessage(Component.translatable(key).withColor(color).withStyle(ChatFormatting.ITALIC));
			}
		}
	}

	// ------------------------------------------------------------------ the fight

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 24.0F, 1.0F));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_STATE, (byte) 0);
	}

	public boolean state(int flag) {
		return (entityData.get(DATA_STATE) & flag) != 0;
	}

	protected void setState(int flag, boolean on) {
		byte now = entityData.get(DATA_STATE);
		byte wanted = (byte) (on ? now | flag : now & ~flag);
		if (wanted != now) {
			entityData.set(DATA_STATE, wanted);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		bossEvent.setProgress(getHealth() / getMaxHealth());
		if (home == null) {
			home = blockPosition();
		}
		if (shifting > 0) {
			shifting--;
			getNavigation().stop();
			shiftTick(level, shifting);
			if (shifting == 0) {
				setState(SHIFTING, false);
				readyAt = now + 20;
			}
			updateName(null);
			return;
		}
		int wanted = getHealth() > getMaxHealth() * 2 / 3 ? 1 : getHealth() > getMaxHealth() / 3 ? 2 : 3;
		if (wanted > phase) {
			phase = wanted;
			shifting = SHIFT_TICKS;
			castAt = 0;
			casting = null;
			spellIndex = 0;
			setState(CASTING, false);
			setState(SHIFTING, true);
			onPhase(level, phase);
			updateName(null);
			return;
		}
		if (position().distanceTo(Vec3.atCenterOf(home)) > leash()) {
			returnHome(level);
		}
		mechanic(level, now);
		LivingEntity target = getTarget();
		if (castAt > 0) {
			getNavigation().stop();
			if (castTarget != null && castTarget.isAlive()) {
				Runebound.aimAt(this, castTarget);
			}
			if (now >= castAt) {
				castAt = 0;
				setState(CASTING, false);
				fire(level);
				readyAt = now + pause(level);
				updateName(null);
			}
			return;
		}
		if (target == null || !target.isAlive()) {
			updateName(null);
			return;
		}
		approach(level, target);
		if (now < readyAt || !hasLineOfSight(target) || distanceTo(target) > castRange() || !mayCast(level, target)) {
			updateName(null);
			return;
		}
		List<List<RuneDef>> spells = spells(phase);
		casting = spells.get(spellIndex++ % spells.size());
		castTarget = target;
		castAt = now + TELEGRAPH;
		setState(CASTING, true);
		updateName(SpellNames.auto(casting));
		Runebound.telegraph(level, this, casting, target, TELEGRAPH);
	}

	private void fire(ServerLevel level) {
		LivingEntity target = castTarget;
		List<RuneDef> spell = casting;
		casting = null;
		if (spell == null || target == null || !target.isAlive() || target.level() != level) {
			return;
		}
		Runebound.aimAt(this, target);
		Runebound.cast(level, this, spell, power());
	}

	/** Breaks off a spell it was telegraphing (stunned, stranded, its shield shattered). */
	protected void interrupt() {
		castAt = 0;
		casting = null;
		setState(CASTING, false);
		updateName(null);
	}

	/** Pulled back to its arena's heart when it strays (or is knocked) too far. */
	protected void returnHome(ServerLevel level) {
		Vec3 back = Vec3.atBottomCenterOf(home).add(level.getRandom().nextGaussian() * 3, 1, level.getRandom().nextGaussian() * 3);
		Vfx.radial(level, net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, position().add(0, getBbHeight() / 2, 0), 24, 0.3);
		teleportTo(back.x, back.y, back.z);
		setDeltaMovement(Vec3.ZERO);
		Sigils.ground(level, Vec3.atBottomCenterOf(home).add(0, 1.05, 0), color(), accent(), 2.0F, 30);
	}

	protected void updateName(String spell) {
		Component name = getType().getDescription();
		if (spell != null) {
			name = Component.translatable("boss.wildercord.casting", name, Component.literal(spell).withColor(accent()));
		} else if (shifting > 0) {
			name = Component.translatable("boss.wildercord.shifting." + typeName(), name);
		} else {
			Component status = status();
			if (status != null) {
				name = Component.translatable("boss.wildercord.status", name, status);
			}
		}
		if (!name.equals(shownName)) {
			shownName = name;
			bossEvent.setName(name);
		}
	}

	/** Its id's path: {@code cinder_warden} and so on, for its text. */
	protected String typeName() {
		return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath();
	}

	// ------------------------------------------------------------------ harm

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (shifting > 0 || source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_WALL)
				|| source.getEntity() instanceof Mob && !(source.getEntity() instanceof Player) && source.getEntity() != this) {
			return false;
		}
		if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
			return super.hurtServer(level, source, damage);
		}
		float taken = resist(level, source, damage);
		if (taken <= 0) {
			return false;
		}
		return super.hurtServer(level, source, taken);
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	// ------------------------------------------------------------------ death

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		bossEvent.setProgress(0);
		Vec3 c = position().add(0, getBbHeight() / 2, 0);
		Sigils.ground(level, position().add(0, 0.05, 0), color(), 0xFFFFFF, 6.0F, DEATH_TICKS + 20);
		Fx.sound(level, c, net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 0.8F);
		for (ServerPlayer player : level.players()) {
			if (player.distanceTo(this) <= 64) {
				Grimoire.feat(player, feat());
			}
		}
	}

	@Override
	protected void tickDeath() {
		deathTime++;
		if (level() instanceof ServerLevel level) {
			dying(level, deathTime);
			if (deathTime >= DEATH_TICKS && !isRemoved()) {
				vanish(level);
				level.broadcastEntityEvent(this, (byte) 60);
				remove(Entity.RemovalReason.KILLED);
			}
		}
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		// A Tier IV rune the killer doesn't know yet, if there is one (the rest comes from its loot table).
		ServerPlayer killer = source.getEntity() instanceof ServerPlayer p ? p : null;
		List<RuneDef> fourth = new ArrayList<>();
		for (RuneDef rune : Runes.all()) {
			if (rune.tier() == 4 && Runes.common(rune) && (killer == null || !Spellbooks.knows(killer, rune.id()))) {
				fourth.add(rune);
			}
		}
		if (fourth.isEmpty()) {
			Runes.all().stream().filter(r -> r.tier() == 4 && Runes.common(r)).forEach(fourth::add);
		}
		if (!fourth.isEmpty()) {
			drop(level, RuneItem.stack(fourth.get(level.getRandom().nextInt(fourth.size()))));
		}
		// Exclusive runes: this boss's own rune (from the location-exclusive rune set) is meant to drop here too; its
		// loot table's "exclusive runes" pool carries it, so nothing extra is dropped in code for now.
		ExperienceOrb.award(level, position(), 220);
	}

	protected void drop(ServerLevel level, ItemStack stack) {
		ItemEntity item = new ItemEntity(level, getX(), getY() + 1, getZ(), stack);
		item.setDeltaMovement(level.getRandom().nextGaussian() * 0.1, 0.35, level.getRandom().nextGaussian() * 0.1);
		level.addFreshEntity(item);
	}

	// ------------------------------------------------------------------ seen, saved

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossEvent.removePlayer(player);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (home != null) {
			output.store("home", BlockPos.CODEC, home);
		}
		output.putInt("phase", phase);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		home = input.read("home", BlockPos.CODEC).orElse(null);
		phase = input.getIntOr("phase", 1);
	}

	// ------------------------------------------------------------------ poses (client)

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			castPoseO = castPose;
			shiftPoseO = shiftPose;
			guardPoseO = guardPose;
			exposedPoseO = exposedPose;
			slamPoseO = slamPose;
			castPose = Mth.approach(castPose, state(CASTING) && !state(SHIFTING) ? 1 : 0, 0.2F);
			shiftPose = Mth.approach(shiftPose, state(SHIFTING) ? 1 : 0, 0.12F);
			guardPose = Mth.approach(guardPose, state(GUARDED) ? 1 : 0, 0.08F);
			exposedPose = Mth.approach(exposedPose, state(EXPOSED) ? 1 : 0, 0.15F);
			slamPose = Mth.approach(slamPose, state(SLAMMING) ? 1 : 0, 0.25F);
		}
	}

	/** Casting: arms raised, its circle blazing. 0 to 1. */
	public float castPose(float partial) {
		return Mth.lerp(partial, castPoseO, castPose);
	}

	/** Between phases. 0 to 1. */
	public float shiftPose(float partial) {
		return Mth.lerp(partial, shiftPoseO, shiftPose);
	}

	/** Its signature defence up. 0 to 1. */
	public float guardPose(float partial) {
		return Mth.lerp(partial, guardPoseO, guardPose);
	}

	/** Open to harm. 0 to 1. */
	public float exposedPose(float partial) {
		return Mth.lerp(partial, exposedPoseO, exposedPose);
	}

	/** Winding up a heavy blow. 0 to 1. */
	public float slamPose(float partial) {
		return Mth.lerp(partial, slamPoseO, slamPose);
	}
}
