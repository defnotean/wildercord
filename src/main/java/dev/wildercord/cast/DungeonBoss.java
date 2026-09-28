package dev.wildercord.cast;

import dev.wildercord.content.RuneItem;
import dev.wildercord.content.dungeons.DungeonAltarBlockEntity;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellNames;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * What the dimension dungeons' bosses share, after the Archivist: a Cord of spells that changes
 * with each of three phases (at two thirds and one third of its health), every spell named on the
 * boss bar and telegraphed with its own circle, a pause between phases while it gathers itself
 * (untouchable), a keeper's leash to its arena, a slow death, and its loot: a Tier IV rune each
 * player who fought it doesn't know yet (in code, handed straight to them) and the rest from its
 * loot table ({@code wildercord:entities/<id>}, given to its killer, or left on its altar). No blow
 * carries it past the start of its next phase, so a burst of damage can't skip one. Its boss bar
 * shows to whoever is in its arena (within {@link #BAR_RANGE} of its altar), not to everyone who
 * can see it. Each boss adds one mechanic that teaches something about magic: see {@link CinderWarden}.
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
	/** Its boss bar shows to players this near its altar. */
	public static final double BAR_RANGE = 48.0;
	/** Players this near its altar when it falls, who fought it, get their rune. */
	private static final double REWARD_RANGE = 96.0;

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
	/** Every player who has hurt it (saved), for their share of its loot. */
	private final Set<UUID> fighters = new LinkedHashSet<>();
	/** The keepers it called up (saved), sent away when it falls. */
	private final List<UUID> minions = new ArrayList<>();

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
		if (now % 10 == 0) {
			updateViewers(level);
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
		if (shiftIfDue(level)) {
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

	/** Into its next phase, if its health says so: the pause begins at once. True if it did. */
	private boolean shiftIfDue(ServerLevel level) {
		int wanted = BossRules.phaseFor(getHealth(), getMaxHealth());
		if (wanted <= phase || shifting > 0 || isDeadOrDying()) {
			return false;
		}
		// One phase at a time: a blow is held at the start of the next (a command setting its health is not).
		phase++;
		shifting = SHIFT_TICKS;
		castAt = 0;
		casting = null;
		spellIndex = 0;
		setState(CASTING, false);
		setState(SHIFTING, true);
		onPhase(level, phase);
		updateName(null);
		return true;
	}

	/** Its boss bar: shown to everyone in its arena, taken from anyone who has left it (or its world). */
	private void updateViewers(ServerLevel level) {
		Vec3 heart = home != null ? Vec3.atCenterOf(home) : position();
		for (ServerPlayer player : new ArrayList<>(bossEvent.getPlayers())) {
			if (player.isRemoved() || player.level() != level || player.position().distanceTo(heart) > BAR_RANGE + 8) {
				bossEvent.removePlayer(player);
			}
		}
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceTo(heart) <= BAR_RANGE) {
				bossEvent.addPlayer(player);
			}
		}
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
		if (source.getEntity() instanceof ServerPlayer player) {
			fighters.add(player.getUUID());
		}
		boolean hurt = super.hurtServer(level, source, taken);
		// A blow that reached its next phase starts it now, before anything else can land.
		shiftIfDue(level);
		return hurt;
	}

	/** Held at the start of its next phase: no one blow (or burst of them) takes it further. */
	@Override
	protected void actuallyHurt(ServerLevel level, DamageSource source, float damage) {
		float before = getHealth();
		super.actuallyHurt(level, source, damage);
		if (!source.is(DamageTypes.GENERIC_KILL) && !source.is(DamageTypes.FELL_OUT_OF_WORLD) && shifting == 0) {
			float capped = BossRules.capped(phase, getMaxHealth(), before, getHealth());
			if (capped != getHealth()) {
				setHealth(capped);
			}
		}
	}

	/** Registers a keeper it called up, so it goes when the boss falls. */
	protected void minion(Mob mob) {
		minions.add(mob.getUUID());
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
		// Its keepers go with it.
		for (UUID id : minions) {
			if (level.getEntity(id) instanceof Mob mob && mob.isAlive()) {
				Vfx.radial(level, net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, mob.position().add(0, mob.getBbHeight() / 2, 0), 12, 0.1);
				mob.discard();
			}
		}
		minions.clear();
		// Its altar goes quiet for good.
		if (home != null && level.getBlockEntity(home) instanceof DungeonAltarBlockEntity altar) {
			altar.slain();
		}
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

	/** Its loot table's drops (trophies and all) go to its killer, or onto its altar if there's nobody to take them. */
	@Override
	protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled) {
		Optional<ResourceKey<LootTable>> table = getLootTable();
		if (table.isEmpty()) {
			return;
		}
		ServerPlayer killer = killer(level, source);
		dropFromLootTable(level, source, playerKilled, table.get(), stack -> give(level, killer, stack));
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		// A Tier IV rune for each player who fought it (one they don't know yet, if there is one), in hand;
		// the rest comes from its loot table.
		List<ServerPlayer> earned = new ArrayList<>();
		Vec3 heart = home != null ? Vec3.atCenterOf(home) : position();
		for (UUID id : fighters) {
			if (level.getEntity(id) instanceof ServerPlayer player && player.isAlive() && player.position().distanceTo(heart) <= REWARD_RANGE) {
				earned.add(player);
			}
		}
		ServerPlayer killer = killer(level, source);
		if (killer != null && !earned.contains(killer)) {
			earned.add(killer);
		}
		if (earned.isEmpty()) {
			give(level, null, RuneItem.stack(fourthFor(level, null)));
		}
		for (ServerPlayer player : earned) {
			give(level, player, RuneItem.stack(fourthFor(level, player)));
		}
		// Exclusive runes: this boss's own rune (from the location-exclusive rune set) is meant to drop here too; its
		// loot table's "exclusive runes" pool carries it, so nothing extra is dropped in code for now.
		ExperienceOrb.award(level, lootSpot(), 220);
	}

	/** A common Tier IV rune {@code player} doesn't know yet (any, if they know them all). */
	private static RuneDef fourthFor(ServerLevel level, ServerPlayer player) {
		List<RuneDef> fourth = new ArrayList<>();
		for (RuneDef rune : Runes.all()) {
			if (rune.tier() == 4 && Runes.common(rune) && (player == null || !Spellbooks.knows(player, rune.id()))) {
				fourth.add(rune);
			}
		}
		if (fourth.isEmpty()) {
			Runes.all().stream().filter(r -> r.tier() == 4 && Runes.common(r)).forEach(fourth::add);
		}
		return fourth.isEmpty() ? Runes.BOLT : fourth.get(level.getRandom().nextInt(fourth.size()));
	}

	/** Who struck the last blow (or, for a death by burning and the like, who hurt it last), if they're still here. */
	private ServerPlayer killer(ServerLevel level, DamageSource source) {
		ServerPlayer killer = source.getEntity() instanceof ServerPlayer p ? p : getLastHurtByPlayer() instanceof ServerPlayer q ? q : null;
		return killer != null && killer.isAlive() && killer.level() == level ? killer : null;
	}

	/** Into {@code player}'s pack; what doesn't fit (or has nobody to take it) is left on its altar. */
	private void give(ServerLevel level, ServerPlayer player, ItemStack stack) {
		if (player != null) {
			player.getInventory().add(stack);
		}
		if (!stack.isEmpty()) {
			drop(level, stack);
		}
	}

	/** Where its loot lands: on its altar, clear of the lava and the drops its arena may hold. */
	protected Vec3 lootSpot() {
		return home != null ? Vec3.atBottomCenterOf(home).add(0, 1.2, 0) : position().add(0, 1, 0);
	}

	protected void drop(ServerLevel level, ItemStack stack) {
		Vec3 at = lootSpot();
		ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack);
		item.setDeltaMovement(level.getRandom().nextGaussian() * 0.02, 0.2, level.getRandom().nextGaussian() * 0.02);
		// Nothing in the arena may take it: no fire, no lava, no blast.
		item.setPermanentlyInvulnerable(true);
		item.setUnlimitedLifetime();
		item.setGlowingTag(true);
		level.addFreshEntity(item);
	}

	// ------------------------------------------------------------------ seen, saved

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossEvent.removePlayer(player);
	}

	@Override
	public void remove(RemovalReason reason) {
		bossEvent.removeAllPlayers();
		super.remove(reason);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (home != null) {
			output.store("home", BlockPos.CODEC, home);
		}
		output.putInt("phase", phase);
		output.store("fighters", UUIDUtil.CODEC.listOf(), List.copyOf(fighters));
		output.store("minions", UUIDUtil.CODEC.listOf(), List.copyOf(minions));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		home = input.read("home", BlockPos.CODEC).orElse(null);
		phase = input.getIntOr("phase", 1);
		fighters.clear();
		input.read("fighters", UUIDUtil.CODEC.listOf()).ifPresent(fighters::addAll);
		minions.clear();
		input.read("minions", UUIDUtil.CODEC.listOf()).ifPresent(minions::addAll);
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
