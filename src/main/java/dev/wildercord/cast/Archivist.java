package dev.wildercord.cast;

import dev.wildercord.content.ArchiveLecternBlockEntity;
import dev.wildercord.content.RuneItem;
import dev.wildercord.content.SigilOption;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.Secrets;
import dev.wildercord.spell.SpellNames;
import dev.wildercord.spell.SpellSigil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Archivist: keeper of the Archive and its Tier IV runes. It fights with a Cord like any
 * caster: every spell it's about to cast is named on its boss bar and written out in its circle.
 * Between its spells it plays tricks of its own from its tome (see {@link ArchivistRules}): a volley
 * of books, a line of ink, a ring of script read aloud, a clap of its tome, a wiping, chains of
 * script, a bookmark it steps through and a hush that strikes down anyone who walks. Each has a tell
 * of its own, a way out of it, and its name on the boss bar while it plays; three leave it open after.
 * Twice it rewrites its Cord (at two thirds and one third of its health): a new phase, new
 * spells and tricks, and allies summoned from the stacks. Like the dimension bosses, no blow carries
 * it past the start of its next phase, so every phase is fought. In the last phase it plays its
 * tricks in pairs, opens a Domain of its own (answer it with yours) and knows at least one secret spell.
 */
public class Archivist extends SpellcasterIllager {
	private static final int TELEGRAPH = 28;
	private static final int REWRITE_TICKS = 50;
	private static final int DOMAIN_SECRET = -1;
	/** Its boss bar shows to players this near the Archive's heart, like the dimension bosses'. */
	private static final double BAR_RANGE = DungeonBoss.BAR_RANGE;

	private static final int VIOLET = 0xB8A0FF;
	private static final int GOLD = 0xF5C46A;
	private static final int PALE = 0xE8DCFF;
	private static final int PAGE_COLOR = 0xF8EDCC;
	private static final int INK = 0x4A3470;
	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** Paper, for the pages torn from its tome. */
	private static final ItemParticleOption PAGE = new ItemParticleOption(ParticleTypes.ITEM, Items.PAPER);
	/** Books, for its volleys. */
	private static final ItemParticleOption BOOK = new ItemParticleOption(ParticleTypes.ITEM, Items.BOOK);
	/** Splinters of shelf, where a book lands. */
	private static final BlockParticleOption SHELF = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BOOKSHELF.defaultBlockState());
	private static final DustParticleOptions INK_DUST = new DustParticleOptions(INK, 1.3F);
	private static final DustParticleOptions GOLD_DUST = new DustParticleOptions(GOLD, 1.1F);
	private static final DustParticleOptions VIOLET_DUST = new DustParticleOptions(VIOLET, 1.1F);
	private static final DustParticleOptions PALE_DUST = new DustParticleOptions(PALE, 0.9F);

	/** The trick it is playing (see {@link ArchivistRules}), its tell, and when it began: synced, for its poses. */
	private static final EntityDataAccessor<Integer> DATA_MOVE = SynchedEntityData.defineId(Archivist.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_MOVE_TELL = SynchedEntityData.defineId(Archivist.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> DATA_MOVE_BEGAN = SynchedEntityData.defineId(Archivist.class, EntityDataSerializers.LONG);

	private static final List<List<RuneDef>> PHASE_1 = List.of(
		List.of(Runes.BOLT, Runes.FROST, Runes.SPLIT_MOD),
		List.of(Runes.RAIN, Runes.SHOCK),
		List.of(Runes.ORB, Runes.HARM));
	private static final List<List<RuneDef>> PHASE_2 = List.of(
		List.of(Runes.CRESCENT, Runes.FIRE, Runes.VOLLEY_MOD),
		List.of(Runes.ZONE, Runes.VENOM, Runes.WIDEN),
		List.of(Runes.BLITZ, Runes.HARM, Runes.AMPLIFY),
		List.of(Runes.MINE, Runes.CHILL, Runes.SPLIT_MOD));
	private static final List<List<RuneDef>> PHASE_3 = List.of(
		List.of(Runes.DOMAIN, Runes.HARM, Runes.CHILL),
		List.of(Runes.BARRAGE, Runes.DISMANTLE),
		List.of(Runes.BEAM, Runes.SONIC_BOOM),
		List.of(Runes.BOLT, Runes.FROST, Runes.FROST, Runes.SHOCK));

	private final ServerBossEvent bossEvent = new ServerBossEvent(getUUID(), Component.translatable("entity.wildercord.archivist"),
		BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
	private BlockPos home;
	private int phase = 1;
	private int rewriting;
	private long readyAt;
	private long castAt;
	private int casting = -2;
	private LivingEntity castTarget;
	private long nextBlink;
	/** The allies it called from the stacks (saved), sent away when it falls. */
	private final List<UUID> minions = new ArrayList<>();
	/** How far its arms and tome are into the casting and rewriting poses, 0 to 1 (worked out on the client only). */
	private float castPose;
	private float castPoseO;
	private float rewritePose;
	private float rewritePoseO;

	/** Its tricks and its phase's spells, drawn so that none comes twice running. */
	private final MoveDeck deck = new MoveDeck(ArchivistRules.MEMORY);
	/** The trick under way (server side), how long its tell is, how far in, how long left, and a count that changes with each. */
	private int move;
	private int moveTell;
	private int moveSince;
	private int moveLeft;
	private int moveEpoch;
	private boolean moveChained;
	/** How many tricks it has run together since it last chose freely. */
	private int chainCount;
	private LivingEntity moveTarget;
	/** Where an aimed trick points across the floor, until its aim locks. */
	private Vec3 aim = new Vec3(0, 0, 1);
	/** Where its bookmark lies. */
	private Vec3 mark;
	/** Who the trick under way has already struck, so none is struck twice by one. */
	private final Set<UUID> struck = new HashSet<>();
	/** Where each foe stood when its hush fell. */
	private final Map<UUID, Vec3> stood = new HashMap<>();
	/** When it last made each trick and the secret (by id), for their rests. */
	private final long[] lastMade = new long[ArchivistRules.RESTS];
	private boolean wasLocked;
	private Component shownName;
	/** What its tricks leave behind them: books about to land, rings rolling out, lines of ink running. */
	private final List<Volley> volleys = new ArrayList<>();
	private final List<Wave> waves = new ArrayList<>();
	private final List<Lane> lanes = new ArrayList<>();
	/** When the second volley of its last Cord is laid (0 for none). */
	private long volleyAgainAt;
	/** How far into each trick's pose, 0 to 1 (worked out on the client only). */
	private final float[] moveBlend = new float[ArchivistRules.MOVES];
	private final float[] moveBlendO = new float[ArchivistRules.MOVES];

	/** Marks where books are about to fall, and who they have hit. */
	private record Volley(List<Vec3> marks, long landAt, Set<UUID> struck) {
	}

	/** A ring of script rolling out along the floor. */
	private static final class Wave {
		private final Vec3 centre;
		private final long began;
		private final double speed;
		private final Set<UUID> struck = new HashSet<>();
		private double reached;

		private Wave(Vec3 centre, long began, double speed) {
			this.centre = centre;
			this.began = began;
			this.speed = speed;
		}
	}

	/** A line of ink cut out across the floor; the lines of one slash share who they have hit. */
	private record Lane(Vec3 from, Vec3 dir, long began, Set<UUID> struck) {
	}

	public Archivist(EntityType<? extends Archivist> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.bossEvent.setDarkenScreen(true);
		Arrays.fill(lastMade, -100_000L);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 400.0)
			.add(Attributes.MOVEMENT_SPEED, 0.42)
			.add(Attributes.FOLLOW_RANGE, 48.0)
			.add(Attributes.ARMOR, 8.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_MOVE, 0);
		builder.define(DATA_MOVE_TELL, 0);
		builder.define(DATA_MOVE_BEGAN, 0L);
	}

	/** Wakes the Archivist above its lectern (null if it couldn't be made). */
	public static Archivist rise(ServerLevel level, BlockPos lectern) {
		Archivist boss = WildercordEntities.ARCHIVIST.create(level, EntitySpawnReason.TRIGGERED);
		if (boss == null) {
			return null;
		}
		Vec3 at = Vec3.atBottomCenterOf(lectern).add(0, 1.2, 0);
		boss.snapTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360, 0);
		boss.home = lectern.above();
		boss.setPersistenceRequired();
		boss.readyAt = level.getGameTime() + 60;
		boss.nextBlink = level.getGameTime() + 200;
		level.addFreshEntity(boss);
		Sigils.ground(level, Vec3.atBottomCenterOf(lectern).add(0, 1, 0), 0xB8A0FF, 0xF5C46A, 5.0F, 80);
		Vfx.radial(level, ParticleTypes.ENCHANT, at.add(0, 1, 0), 60, 0.8);
		Vfx.radial(level, new DustParticleOptions(0xB8A0FF, 1.4F), at.add(0, 1, 0), 40, 0.4);
		Fx.sound(level, at, SoundEvents.EVOKER_PREPARE_SUMMON, 1.5F, 0.5F);
		Fx.sound(level, at, SoundEvents.BOOK_PAGE_TURN, 1.5F, 0.6F);
		Fx.sound(level, at, SoundEvents.WITHER_SPAWN, 0.4F, 1.6F);
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceTo(at) < 48) {
				player.sendSystemMessage(Component.translatable("message.wildercord.archivist_wakes").withColor(0xB8A0FF).withStyle(ChatFormatting.ITALIC));
			}
		}
		return boss;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(8, new RandomStrollGoal(this, 0.6));
		this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 16.0F, 1.0F));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	private List<List<RuneDef>> spells() {
		return phase == 1 ? PHASE_1 : phase == 2 ? PHASE_2 : PHASE_3;
	}

	private double power() {
		return Runebound.power((ServerLevel) level()) * 1.1;
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
		// What its tricks left behind (books about to land, rings rolling, lines of ink) goes on whatever it does next.
		tickHazards(level, now);
		// Rewriting its Cord: floats, untouchable, pages whirling.
		if (rewriting > 0) {
			rewriting--;
			getNavigation().stop();
			setDeltaMovement(0, 0.02, 0);
			Vec3 c = position().add(0, 1.4, 0);
			for (int i = 0; i < 6; i++) {
				double a = rewriting * 0.3 + Math.PI * 2 * i / 6;
				Vfx.emit(level, ParticleTypes.ENCHANT, c.add(Math.cos(a) * 1.6, Math.sin(rewriting * 0.2 + i) * 0.5, Math.sin(a) * 1.6), 2, 0.05, 0.1);
			}
			// Torn pages still whirl about it while the new Cord is written.
			if (rewriting % 4 == 0) {
				double a = rewriting * 0.45;
				Vfx.emit(level, PAGE, c.add(Math.cos(a) * 1.4, 0.4 + Math.sin(rewriting * 0.3) * 0.4, Math.sin(a) * 1.4), 2, 0.1, 0.06);
			}
			// The new lines are written into the floor about it ring by ring, and a column of gold climbs out of it.
			if (rewriting % 10 == 0) {
				ElementFx.groundRing(level, position(), GOLD, 0.5, 6.0, 0.25, 16);
				Fx.sound(level, c, SoundEvents.ENCHANTMENT_TABLE_USE, 1.2F, 0.6F + (REWRITE_TICKS - rewriting) * 0.01F);
			}
			Vfx.emit(level, GOLD_DUST, position().add(0, 0.2 + (REWRITE_TICKS - rewriting) * 0.06, 0), 2, 0.25, 0.02);
			if (rewriting == 0) {
				setIsCastingSpell(IllagerSpell.NONE);
				readyAt = now + 20;
				// Written: the new Cord bursts out of it in pages and light.
				Vfx.radial(level, PAGE, c, 30, 0.45);
				Vfx.radial(level, VIOLET_DUST, c, 40, 0.5);
				ElementFx.groundRing(level, position(), VIOLET, 0.5, 9.0, 0.4, 14);
				Sigils.flash(level, c, GOLD, 3.0F);
				Fx.sound(level, c, SoundEvents.BOOK_PUT, 1.5F, 0.5F);
				Fx.sound(level, c, SoundEvents.BELL_RESONATE, 1.0F, 0.8F);
				updateName(null);
			}
			return;
		}
		// A blow starts its rewriting at once (see hurtServer); a command setting its health still gets one for each phase.
		if (shiftIfDue(level)) {
			return;
		}
		// A seal on its casting breaks off whatever it was readying (though not an opening it stands spent in).
		boolean locked = CastLock.locked(this);
		if (locked && !wasLocked) {
			if (castAt > 0) {
				castAt = 0;
				setIsCastingSpell(IllagerSpell.NONE);
				updateName(null);
			}
			if (move > 0 && !ArchivistRules.open(move)) {
				breakMove(now);
			}
			readyAt = Math.max(readyAt, now + 30);
		}
		wasLocked = locked;
		LivingEntity target = getTarget();
		// Never strays far from the Archive's heart.
		if (home != null && position().distanceTo(Vec3.atCenterOf(home)) > 22) {
			breakMove(now);
			blink(level, Vec3.atBottomCenterOf(home).add(level.getRandom().nextGaussian() * 4, 0, level.getRandom().nextGaussian() * 4));
		}
		if (move > 0) {
			tickMove(level, now);
			return;
		}
		if (target != null && now >= nextBlink && castAt == 0) {
			nextBlink = now + 160 + level.getRandom().nextInt(120);
			if (distanceTo(target) < 6 || level.getRandom().nextBoolean()) {
				Vec3 away = position().subtract(target.position());
				Vec3 dir = new Vec3(away.x, 0, away.z).lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : new Vec3(away.x, 0, away.z).normalize();
				blink(level, target.position().add(dir.yRot((float) (level.getRandom().nextGaussian() * 0.8)).scale(10)));
			}
		}
		if (castAt > 0) {
			getNavigation().stop();
			if (castTarget != null && castTarget.isAlive()) {
				Runebound.aimAt(this, castTarget);
			}
			if (now >= castAt) {
				castAt = 0;
				int made = casting == DOMAIN_SECRET ? ArchivistRules.SUNFALL : ArchivistRules.SPELL;
				fire(level);
				readyAt = now + ArchivistRules.pause(made, phase, level.getRandom().nextDouble());
				updateName(null);
			}
			return;
		}
		if (target == null || !target.isAlive() || now < readyAt) {
			if (target != null && distanceTo(target) > 16) {
				getNavigation().moveTo(target, 0.9);
			}
			return;
		}
		// Choose what comes next: one of its tricks, one of its phase's spells, or in the last phase a secret.
		double dx = target.getX() - getX();
		double dz = target.getZ() - getZ();
		double distance = Math.sqrt(dx * dx + dz * dz);
		double height = target.getY() - getY();
		boolean sight = hasLineOfSight(target);
		boolean behind = ArchivistRules.behind(yBodyRot, Math.toDegrees(Math.atan2(-dx, dz)));
		int crowd = foes(level, position(), ArchivistRules.CROWD).size();
		long[] since = new long[ArchivistRules.RESTS];
		for (int i = 0; i < since.length; i++) {
			since[i] = now - lastMade[i];
		}
		List<List<RuneDef>> spells = spells();
		int choice = ArchivistRules.choose(deck, phase, spells.size(), distance, behind, height, sight, crowd, locked, since,
			level.getRandom().nextDouble());
		if (choice < 0) {
			readyAt = now + 10;
			if (distance > 8 || !sight) {
				getNavigation().moveTo(target, 0.9);
			}
			return;
		}
		deck.played(choice);
		if (choice < ArchivistRules.MOVES) {
			chainCount = 0;
			beginMove(level, choice, target, false, now);
			return;
		}
		castTarget = target;
		castAt = now + TELEGRAPH;
		spellCastingTickCount = TELEGRAPH + 6;
		setIsCastingSpell(IllagerSpell.FANGS);
		getNavigation().stop();
		if (choice == ArchivistRules.SUNFALL) {
			lastMade[ArchivistRules.SUNFALL] = now;
			casting = DOMAIN_SECRET;
			Sigils.target(level, CastEngine.ground(level, target.position().add(0, 1, 0)), 0xFF8A30, 7.0F, TELEGRAPH + 40);
			updateName(Secrets.SUNFALL.name());
			// Its circle spells Sunfall out, rune by rune, for anyone watching closely enough to learn it.
			List<RuneDef> sunfall = Secrets.SUNFALL.runes();
			Vec3 look = target.getBoundingBox().getCenter().subtract(getEyePosition()).normalize();
			Sigils.spell(level, Runebound.hands(this, look, 0.55F), look, sunfall, Secrets.SUNFALL.color(), 0.55F, TELEGRAPH + 4);
			Fx.sound(level, position(), SoundEvents.EVOKER_PREPARE_ATTACK, 1.2F, 0.6F);
			return;
		}
		casting = choice - ArchivistRules.SPELL;
		List<RuneDef> spell = spells.get(casting);
		updateName(SpellNames.auto(spell));
		Runebound.telegraph(level, this, spell, target, TELEGRAPH);
	}

	private void fire(ServerLevel level) {
		LivingEntity target = castTarget;
		setIsCastingSpell(IllagerSpell.NONE);
		if (target == null || !target.isAlive() || target.level() != level) {
			return;
		}
		Runebound.aimAt(this, target);
		if (casting == DOMAIN_SECRET) {
			// A secret spell: everyone who sees it learns its riddle.
			Cast cast = new Cast(this, 1, new Heart.Bonuses(power() * 0.75, 1, 1, 1), false, null, Cast.Info.NONE).weigh(400);
			SecretSpells.cast(cast, Secrets.SUNFALL);
			for (ServerPlayer player : level.players()) {
				if (player.distanceTo(this) < 40) {
					Grimoire.unlock(player, "hint:" + Secrets.SUNFALL.id());
				}
			}
			return;
		}
		List<List<RuneDef>> spells = spells();
		if (casting >= 0 && casting < spells.size()) {
			Runebound.cast(level, this, spells.get(casting), power());
		}
	}

	/** Into its next phase, if its health says so: one phase at a time, the rewriting beginning at once. True if it did. */
	private boolean shiftIfDue(ServerLevel level) {
		if (rewriting > 0 || isDeadOrDying() || BossRules.phaseFor(getHealth(), getMaxHealth()) <= phase) {
			return false;
		}
		phase++;
		rewrite(level);
		return true;
	}

	private void rewrite(ServerLevel level) {
		rewriting = REWRITE_TICKS;
		castAt = 0;
		setIsCastingSpell(IllagerSpell.WOLOLO);
		spellCastingTickCount = REWRITE_TICKS;
		// Whatever it was playing breaks off, and what its tricks left behind fades: the new Cord starts clean.
		deck.clear();
		endMove();
		chainCount = 0;
		clearHazards();
		Vec3 c = position();
		tearPages(level);
		Sigils.ground(level, c.add(0, 0.05, 0), 0xB8A0FF, 0xF5C46A, 4.0F, REWRITE_TICKS + 10);
		Sigils.layer(level, c.add(0, 2.6, 0), new Vec3(0, 1, 0), SigilOption.RING, 0xF5C46A, 2.5F, REWRITE_TICKS + 10, 0.12F);
		Vfx.radial(level, ParticleTypes.ENCHANT, c.add(0, 1.4, 0), 80, 0.9);
		// The room's light is drawn in to it, and a beam of gold stands over it while it writes.
		ElementFx.groundRing(level, c, VIOLET, 11.0, 0.5, 0.4, 20);
		ElementFx.ray(level, c.add(0, 0.2, 0), c.add(0, 9, 0), GOLD, 0.35, REWRITE_TICKS);
		Fx.sound(level, c, SoundEvents.BOOK_PAGE_TURN, 1.5F, 0.5F);
		Fx.sound(level, c, SoundEvents.EVOKER_PREPARE_WOLOLO, 1.5F, 0.6F);
		Fx.sound(level, c, SoundEvents.BELL_RESONATE, 1.2F, 0.5F);
		for (ServerPlayer player : level.players()) {
			if (player.distanceTo(this) < 48) {
				player.sendSystemMessage(Component.translatable("message.wildercord.archivist_rewrites." + phase).withColor(0xB8A0FF).withStyle(ChatFormatting.ITALIC));
			}
		}
		// Allies from the stacks: Runebound, woken to defend the Archive.
		for (int i = 0; i < 2; i++) {
			Mob guard = (i == 0 ? EntityTypes.SKELETON : EntityTypes.PILLAGER).create(level, EntitySpawnReason.MOB_SUMMONED);
			if (guard == null) {
				continue;
			}
			double a = level.getRandom().nextDouble() * Math.PI * 2;
			Vec3 spot = CastEngine.ground(level, c.add(Math.cos(a) * 5, 2, Math.sin(a) * 5));
			guard.snapTo(spot.x, spot.y, spot.z, 0, 0);
			guard.finalizeSpawn(level, level.getCurrentDifficultyAt(guard.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
			// Bound before it enters the world, so the Archive's own roll doesn't bind it a second time.
			Runebound.bind(guard, phase == 3);
			level.addFreshEntity(guard);
			minions.add(guard.getUUID());
			Sigils.ground(level, spot, 0x9A6AD0, 0xE8E0FF, 1.2F, 30);
			Vfx.emit(level, ParticleTypes.SOUL, spot.add(0, 1, 0), 12, 0.3, 0.03);
		}
		updateName(null);
	}

	// ------------------------------------------------------------------ its tricks

	/** Starts trick {@code m} on {@code target}: its tell first, its name on the boss bar while it plays. */
	private void beginMove(ServerLevel level, int m, LivingEntity target, boolean chained, long now) {
		moveTarget = target;
		moveChained = chained;
		struck.clear();
		stood.clear();
		mark = null;
		aim = toward(target);
		face(aim);
		hold();
		int tell = ArchivistRules.tell(m, chained);
		pose(m, tell, tell + ArchivistRules.after(m), now);
		lastMade[m] = now;
	}

	/** Into pose {@code m} (a trick or an opening) for {@code length} ticks, the first {@code tell} of them its tell. */
	private void pose(int m, int tell, int length, long now) {
		moveEpoch++;
		move = m;
		moveTell = tell;
		moveSince = 0;
		moveLeft = length;
		entityData.set(DATA_MOVE_TELL, tell);
		entityData.set(DATA_MOVE_BEGAN, now);
		entityData.set(DATA_MOVE, m);
		updateName(null);
	}

	/** Done with whatever trick or opening was under way. */
	private void endMove() {
		moveEpoch++;
		move = 0;
		moveTell = 0;
		moveSince = 0;
		moveLeft = 0;
		moveChained = false;
		moveTarget = null;
		mark = null;
		struck.clear();
		stood.clear();
		entityData.set(DATA_MOVE, 0);
		updateName(null);
	}

	/** Breaks off the trick under way (a seal, a blink home): it gathers itself a moment before choosing again. */
	private void breakMove(long now) {
		if (move > 0) {
			endMove();
			readyAt = Math.max(readyAt, now + 20);
		}
		chainCount = 0;
	}

	/** A tick of the trick or opening under way; it stands its ground for the whole of it. */
	private void tickMove(ServerLevel level, long now) {
		int epoch = moveEpoch;
		hold();
		moveTick(level, now);
		if (epoch != moveEpoch || move == 0) {
			return;
		}
		moveSince++;
		moveLeft--;
		if (moveLeft <= 0) {
			moveDone(level, now);
		}
	}

	/** The trick under way has run its course: into its opening, straight on into another (its last Cord), or a rest. */
	private void moveDone(ServerLevel level, long now) {
		int done = move;
		int opening = ArchivistRules.opening(done);
		if (opening != ArchivistRules.NONE) {
			struck.clear();
			pose(opening, 0, ArchivistRules.openFor(opening), now);
			openingBegins(level, opening);
			return;
		}
		LivingEntity target = moveTarget != null && moveTarget.isAlive() && moveTarget.level() == level ? moveTarget : getTarget();
		if (phase >= 3 && chainCount < ArchivistRules.CHAIN_MOST && target != null && target.isAlive() && !ArchivistRules.open(done)) {
			double dx = target.getX() - getX();
			double dz = target.getZ() - getZ();
			int next = ArchivistRules.chain(done, Math.sqrt(dx * dx + dz * dz), hasLineOfSight(target), level.getRandom().nextDouble());
			if (next > 0 && next != done && ArchivistRules.unlocked(next, phase) && now - lastMade[next] >= ArchivistRules.rest(next, phase) / 2) {
				chainCount++;
				deck.played(next);
				beginMove(level, next, target, true, now);
				return;
			}
		}
		boolean pair = chainCount > 0;
		endMove();
		chainCount = 0;
		readyAt = now + ArchivistRules.pause(done, phase, level.getRandom().nextDouble()) + (pair ? ArchivistRules.CHAIN_REST : 0);
	}

	/** What the trick under way does this tick, {@code moveSince} ticks in. */
	private void moveTick(ServerLevel level, long now) {
		int t = moveSince;
		if (!ArchivistRules.open(move) && t <= moveTell) {
			// It turns after its foe through the tell; an aimed trick stops turning a few ticks before it lands.
			LivingEntity target = liveTarget(level);
			int lock = ArchivistRules.lock(move);
			if (target != null && (lock == 0 || t < moveTell - lock)) {
				aim = toward(target);
			}
			face(aim);
		}
		switch (move) {
			case ArchivistRules.TOMES -> tomes(level, now, t);
			case ArchivistRules.QUILL -> quill(level, now, t);
			case ArchivistRules.RECITE -> recite(level, now, t);
			case ArchivistRules.SNAP -> snap(level, t);
			case ArchivistRules.ERASE -> erase(level, t);
			case ArchivistRules.BIND -> bind(level, t);
			case ArchivistRules.BOOKMARK -> bookmark(level, t);
			case ArchivistRules.HUSH -> hush(level, t);
			case ArchivistRules.SHUT, ArchivistRules.BLANK, ArchivistRules.LEAFING -> spent(level, t);
			default -> {
			}
		}
	}

	/** The foe the trick under way was aimed at, while it is still alive and here. */
	private LivingEntity liveTarget(ServerLevel level) {
		return moveTarget != null && moveTarget.isAlive() && moveTarget.level() == level ? moveTarget : null;
	}

	/** Books lift from the stacks and wheel about it as it raises its arms, then fly up and out, and marks open where they will land. */
	private void tomes(ServerLevel level, long now, int t) {
		Vec3 c = position();
		if (t == 0) {
			Vfx.emit(level, SHELF, c.add(0, 0.6, 0), 24, 0.9, 0.15);
			Fx.sound(level, c, SoundEvents.CHISELED_BOOKSHELF_PICKUP, 1.3F, 0.7F);
		}
		if (t < moveTell) {
			// The books wheel faster and climb higher as the tell runs on.
			for (int i = 0; i < 3; i++) {
				double a = t * 0.45 + Math.PI * 2 * i / 3;
				Vfx.emit(level, BOOK, c.add(Math.cos(a) * 1.8, 0.6 + t * 0.12, Math.sin(a) * 1.8), 1, 0.02, 0);
			}
			if (t % 4 == 0) {
				Fx.sound(level, c, SoundEvents.BOOK_PAGE_TURN, 1.0F, 0.8F + t * 0.04F);
			}
		}
		if (t == moveTell) {
			Vfx.radial(level, BOOK, c.add(0, 2.4, 0), 18, 0.5);
			Vfx.radial(level, VIOLET_DUST, c.add(0, 2.4, 0), 16, 0.4);
			Fx.sound(level, c, SoundEvents.ILLUSIONER_CAST_SPELL, 1.3F, 1.1F);
			layVolley(level, now, ArchivistRules.scatter(phase));
			if (phase >= 3) {
				volleyAgainAt = now + ArchivistRules.TOMES_AGAIN;
			}
		}
	}

	/** Its quill drawn back while lines of ink run across the floor after its foe, gold once its aim settles; then slashed out. */
	private void quill(ServerLevel level, long now, int t) {
		int lines = ArchivistRules.quillLines(phase);
		int lockAt = moveTell - ArchivistRules.QUILL_LOCK;
		Vec3 feet = position();
		if (t == 0) {
			Fx.sound(level, feet, SoundEvents.SQUID_SQUIRT, 1.0F, 0.6F);
		}
		if (t == lockAt) {
			Fx.sound(level, feet, SoundEvents.LODESTONE_COMPASS_LOCK, 1.4F, 1.2F);
		}
		if (t < moveTell && t % 2 == 0) {
			DustParticleOptions dust = t >= lockAt ? GOLD_DUST : INK_DUST;
			for (int i = 0; i < lines; i++) {
				Vec3 dir = quillDir(i, lines);
				for (double d = 1; d <= ArchivistRules.QUILL_REACH; d += 0.8) {
					Vfx.emit(level, dust, feet.add(dir.scale(d)).add(0, 0.1, 0), 1, 0.05, 0);
				}
			}
		}
		if (t == moveTell) {
			// The lines of one slash share who they have struck, so no one is cut twice by it.
			Set<UUID> shared = new HashSet<>();
			for (int i = 0; i < lines; i++) {
				Vec3 dir = quillDir(i, lines);
				lanes.add(new Lane(feet, dir, now, shared));
				ElementFx.ray(level, feet.add(dir.scale(0.6)).add(0, 0.15, 0), feet.add(dir.scale(ArchivistRules.QUILL_REACH)).add(0, 0.15, 0),
					GOLD, 0.25, 10);
			}
			Vfx.emit(level, ParticleTypes.SQUID_INK, tomeAt(), 10, 0.2, 0.05);
			Fx.sound(level, feet, SoundEvents.PLAYER_ATTACK_SWEEP, 1.3F, 0.7F);
			Fx.sound(level, feet, SoundEvents.EVOKER_FANGS_ATTACK, 1.0F, 1.3F);
		}
	}

	/** Which way line {@code i} of {@code lines} runs: straight along its aim, or fanned either side of it. */
	private Vec3 quillDir(int i, int lines) {
		double off = lines == 1 ? 0 : (i - (lines - 1) / 2.0) * ArchivistRules.QUILL_SPREAD;
		return aim.yRot((float) Math.toRadians(off));
	}

	/** Its tome raised high over its head while the words gather in to it; then it reads aloud and a ring of script rolls out. */
	private void recite(ServerLevel level, long now, int t) {
		Vec3 feet = position();
		Vec3 tome = feet.add(0, 3.0, 0);
		if (t == 0) {
			Fx.sound(level, tome, SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1.2F, 0.8F);
		}
		if (t < moveTell) {
			Vfx.emit(level, ParticleTypes.ENCHANT, tome, 6, 1.4, 0.6);
			if (t % 2 == 0) {
				// A ring of script drawn in toward its feet: where the one it reads will roll out from.
				Vfx.ring(level, PALE_DUST, feet.add(0, 0.15, 0), 0.6 + 4.0 * (1 - t / (double) moveTell), 18);
			}
		}
		if (t == moveTell) {
			waves.add(new Wave(feet, now, ArchivistRules.RECITE_SPEED));
			ElementFx.groundRing(level, feet, VIOLET, 0.3, ArchivistRules.RECITE_REACH, 0.5, (int) (ArchivistRules.RECITE_REACH / ArchivistRules.RECITE_SPEED));
			Vfx.radial(level, PAGE, tome, 14, 0.3);
			Fx.sound(level, tome, SoundEvents.BELL_RESONATE, 1.5F, 1.2F);
			Fx.sound(level, tome, SoundEvents.ILLUSIONER_CAST_SPELL, 1.2F, 0.7F);
		}
		if (phase >= 3 && t == moveTell + ArchivistRules.RECITE_AGAIN) {
			// Its last Cord reads a second line, faster.
			waves.add(new Wave(feet, now, ArchivistRules.RECITE_SPEED_AGAIN));
			ElementFx.groundRing(level, feet, GOLD, 0.3, ArchivistRules.RECITE_REACH, 0.5,
				(int) (ArchivistRules.RECITE_REACH / ArchivistRules.RECITE_SPEED_AGAIN));
			Fx.sound(level, tome, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5F, 1.4F);
		}
	}

	/** Its tome thrust out and opened wide over a circle marked about it, the light drawn into the pages; then clapped shut. */
	private void snap(ServerLevel level, int t) {
		Vec3 feet = position();
		Vec3 tome = tomeAt();
		if (t == 0) {
			Sigils.target(level, feet, VIOLET, (float) ArchivistRules.SNAP_RADIUS, moveTell + 2);
			Fx.sound(level, tome, SoundEvents.BOOK_PAGE_TURN, 1.4F, 0.5F);
		}
		if (t < moveTell) {
			if (t % 3 == 0) {
				Vfx.ring(level, VIOLET_DUST, feet.add(0, 0.12, 0), ArchivistRules.SNAP_RADIUS, 28);
			}
			Vfx.emit(level, ParticleTypes.ENCHANT, tome, 4, 1.2, 0.4);
			// In its last Cord the open pages pull at anyone near.
			if (phase >= 3 && t % 4 == 0) {
				pull(level);
			}
		}
		if (t == moveTell) {
			Fx.sound(level, tome, SoundEvents.BOOK_PUT, 2.0F, 0.5F);
			Fx.sound(level, feet, SoundEvents.GENERIC_EXPLODE, 0.6F, 1.6F);
			Sigils.flash(level, tome, PALE, 2.5F);
			ElementFx.groundRing(level, feet, PALE, 0.5, ArchivistRules.SNAP_RADIUS + 0.5, 0.4, 8);
			Vfx.radial(level, PAGE, tome, 20, 0.4);
			for (ServerPlayer p : foes(level, feet, ArchivistRules.SNAP_RADIUS + 1)) {
				double dx = p.getX() - getX();
				double dz = p.getZ() - getZ();
				if (!ArchivistRules.snapHits(Math.sqrt(dx * dx + dz * dz), p.getY() - getY()) || !struck.add(p.getUUID())) {
					continue;
				}
				strike(level, p, ArchivistRules.SNAP_DAMAGE);
				Vec3 away = flat(new Vec3(dx, 0, dz), aim);
				Effects.push(p, away.scale(ArchivistRules.SNAP_PUSH).add(0, ArchivistRules.SNAP_LIFT, 0));
			}
		}
	}

	/** Its hand drawn across its body over a pale cone chalked on the floor before it; then it sweeps the hand out and wipes the cone. */
	private void erase(ServerLevel level, int t) {
		Vec3 feet = position();
		int lockAt = moveTell - ArchivistRules.ERASE_LOCK;
		double half = phase >= 3 ? ArchivistRules.ERASE_HALF_LATE : ArchivistRules.ERASE_HALF;
		if (t == 0) {
			Fx.sound(level, feet, SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 1.2F, 0.9F);
		}
		if (t == lockAt) {
			Fx.sound(level, feet, SoundEvents.LODESTONE_COMPASS_LOCK, 1.4F, 0.8F);
		}
		if (t < moveTell && t % 2 == 0) {
			DustParticleOptions dust = t >= lockAt ? GOLD_DUST : PALE_DUST;
			// The cone's two edges and its far rim, chalked on the floor.
			for (int s = -1; s <= 1; s += 2) {
				Vec3 edge = aim.yRot((float) Math.toRadians(half * s));
				for (double d = ArchivistRules.ERASE_NEAR; d <= ArchivistRules.ERASE_REACH; d += 0.9) {
					Vfx.emit(level, dust, feet.add(edge.scale(d)).add(0, 0.1, 0), 1, 0.03, 0);
				}
			}
			for (double a = -half; a <= half; a += 6) {
				Vec3 rim = aim.yRot((float) Math.toRadians(a)).scale(ArchivistRules.ERASE_REACH);
				Vfx.emit(level, dust, feet.add(rim).add(0, 0.1, 0), 1, 0.03, 0);
			}
		}
		if (t == moveTell) {
			ElementFx.slash(level, feet.add(0, 0.4, 0), UP, aim, PALE, 3.0, Math.toRadians(half * 2), 0.4, 1, 8);
			ElementFx.slash(level, feet.add(0, 0.4, 0), UP, aim, PAGE_COLOR, 6.5, Math.toRadians(half * 2), 0.5, 1, 10);
			for (double a = -half; a <= half; a += 10) {
				Vec3 dir = aim.yRot((float) Math.toRadians(a));
				Vfx.fling(level, PALE_DUST, feet.add(dir.scale(2)).add(0, 0.4, 0), dir, 0.6);
			}
			Fx.sound(level, feet, SoundEvents.BEACON_DEACTIVATE, 1.4F, 1.4F);
			Fx.sound(level, feet, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.6F);
			Vec3 side = new Vec3(-aim.z, 0, aim.x);
			for (ServerPlayer p : foes(level, feet, ArchivistRules.ERASE_REACH + 1)) {
				Vec3 rel = p.position().subtract(feet);
				double forward = rel.x * aim.x + rel.z * aim.z;
				double across = rel.x * side.x + rel.z * side.z;
				if (!ArchivistRules.cone(forward, across, rel.y, phase) || !struck.add(p.getUUID())) {
					continue;
				}
				strike(level, p, ArchivistRules.ERASE_DAMAGE);
				wipe(level, p);
			}
		}
	}

	/** Chains of script run from its hands to everyone it can see; when it pulls them taut, whoever it still sees is bound. */
	private void bind(ServerLevel level, int t) {
		Vec3 hands = position().add(0, 1.3, 0);
		List<ServerPlayer> seen = new ArrayList<>();
		for (ServerPlayer p : foes(level, position(), ArchivistRules.BIND_RANGE)) {
			if (hasLineOfSight(p)) {
				seen.add(p);
			}
		}
		if (t < moveTell) {
			if (t % 6 == 0) {
				Fx.sound(level, hands, SoundEvents.CHAIN_PLACE, 1.2F, 0.6F + t * 0.03F);
			}
			if (t % 2 == 0) {
				// The links run out to each foe it sees, and turn gold as it is about to pull.
				DustParticleOptions dust = t >= moveTell - 4 ? GOLD_DUST : VIOLET_DUST;
				for (ServerPlayer p : seen) {
					Vec3 to = p.getBoundingBox().getCenter();
					Vec3 d = to.subtract(hands);
					double length = d.length();
					double reach = Math.min(length, length * (t + 2) / (double) Math.max(1, moveTell - 4));
					for (double s = 0.6; s < reach; s += 0.7) {
						Vfx.emit(level, dust, hands.add(d.scale(s / length)), 1, 0.02, 0);
					}
				}
			}
		}
		if (t == moveTell) {
			Fx.sound(level, hands, SoundEvents.CHAIN_HIT, 1.5F, 0.6F);
			Fx.sound(level, hands, SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, 0.8F);
			for (ServerPlayer p : seen) {
				if (!struck.add(p.getUUID())) {
					continue;
				}
				Vec3 to = p.getBoundingBox().getCenter();
				ElementFx.ray(level, hands, to, GOLD, 0.12, 6);
				Vfx.emit(level, GOLD_DUST, to, 10, 0.3, 0.02);
				strike(level, p, ArchivistRules.BIND_DAMAGE);
				p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ArchivistRules.BIND_SLOW, 1), this);
				// In its last Cord the chains drag the bound in toward it.
				if (phase >= 3) {
					Vec3 in = flat(position().subtract(p.position()), aim.reverse());
					Effects.push(p, in.scale(ArchivistRules.BIND_DRAW).add(0, 0.2, 0));
				}
			}
		}
	}

	/** Its hand raised straight up while a gold mark opens under its foe and creeps after them; the mark stops, and it steps onto it. */
	private void bookmark(ServerLevel level, int t) {
		int lockAt = moveTell - ArchivistRules.BOOKMARK_LOCK;
		LivingEntity target = liveTarget(level);
		if (t == 0) {
			mark = target != null ? spot(level, target.getX(), target.getZ(), target.getY())
				: spot(level, getX() + aim.x * 6, getZ() + aim.z * 6, getY());
			Fx.sound(level, mark, SoundEvents.BOOK_PAGE_TURN, 1.2F, 1.4F);
			Fx.sound(level, position(), SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F, 1.2F);
		}
		if (mark == null) {
			return;
		}
		// The mark creeps after its foe until it stops: anyone who keeps moving outruns it.
		if (t > 0 && t < lockAt && target != null) {
			double[] to = ArchivistRules.creep(mark.x, mark.z, target.getX(), target.getZ());
			mark = spot(level, to[0], to[1], Math.max(mark.y, target.getY()));
		}
		if (t < moveTell && t % 2 == 0) {
			Vfx.ring(level, t >= lockAt ? GOLD_DUST : PALE_DUST, mark.add(0, 0.12, 0), ArchivistRules.BOOKMARK_RADIUS, 20);
		}
		if (t == lockAt) {
			Sigils.target(level, mark, GOLD, (float) ArchivistRules.BOOKMARK_RADIUS, ArchivistRules.BOOKMARK_LOCK + 4);
			Fx.sound(level, mark, SoundEvents.LODESTONE_COMPASS_LOCK, 1.4F, 1.0F);
		}
		if (t >= lockAt && t < moveTell) {
			Vfx.emit(level, GOLD_DUST, mark.add(0, 0.2 + (t - lockAt) * 0.3, 0), 3, 0.3, 0.01);
		}
		if (t == moveTell) {
			Vec3 from = position();
			Vfx.radial(level, ParticleTypes.REVERSE_PORTAL, from.add(0, 1.2, 0), 24, 0.3);
			Vfx.radial(level, PAGE, from.add(0, 1.2, 0), 12, 0.3);
			// It steps through the page onto the mark, if there is room for it there; the burst lands either way.
			if (level.noCollision(this, getDimensions(getPose()).makeBoundingBox(mark))) {
				teleportTo(mark.x, mark.y, mark.z);
			}
			Sigils.flash(level, mark.add(0, 0.5, 0), GOLD, 3.0F);
			ElementFx.groundRing(level, mark, GOLD, 0.3, ArchivistRules.BOOKMARK_RADIUS + 1.2, 0.4, 8);
			Vfx.radial(level, GOLD_DUST, mark.add(0, 0.5, 0), 30, 0.4);
			Fx.sound(level, mark, SoundEvents.ENDER_EYE_DEATH, 1.4F, 0.8F);
			Fx.sound(level, mark, SoundEvents.BOOK_PUT, 1.6F, 0.6F);
			for (ServerPlayer p : foes(level, mark, ArchivistRules.BOOKMARK_RADIUS + 1.5)) {
				double dx = p.getX() - mark.x;
				double dz = p.getZ() - mark.z;
				if (!ArchivistRules.inMark(dx, dz, p.getY() - mark.y, ArchivistRules.BOOKMARK_RADIUS) || !struck.add(p.getUUID())) {
					continue;
				}
				strike(level, p, ArchivistRules.BOOKMARK_DAMAGE);
				Effects.push(p, flat(new Vec3(dx, 0, dz), aim).scale(0.4).add(0, ArchivistRules.BOOKMARK_LIFT, 0));
			}
		}
	}

	/** A finger raised to its hood and a seal over everyone near; when the seal closes it calls for quiet, and strikes down whoever walks. */
	private void hush(ServerLevel level, int t) {
		Vec3 c = position();
		if (t == 0) {
			for (ServerPlayer p : foes(level, c, ArchivistRules.HUSH_RANGE)) {
				Sigils.layer(level, p.position().add(0, 2.6, 0), UP, SigilOption.RING, PALE, 0.9F, moveTell + 2, 0.2F);
			}
			ElementFx.groundRing(level, c, PALE, ArchivistRules.HUSH_RANGE, 1.0, 0.3, moveTell);
			Fx.sound(level, c, SoundEvents.BEACON_POWER_SELECT, 1.2F, 0.6F);
		}
		if (t < moveTell && t % 4 == 0) {
			for (ServerPlayer p : foes(level, c, ArchivistRules.HUSH_RANGE)) {
				Vfx.emit(level, PALE_DUST, p.position().add(0, 2.4, 0), 3, 0.2, 0);
			}
		}
		if (t == moveTell) {
			Fx.sound(level, c, SoundEvents.BELL_BLOCK, 1.5F, 0.5F);
			Sigils.flash(level, tomeAt(), PALE, 2.0F);
			for (ServerPlayer p : foes(level, c, ArchivistRules.HUSH_RANGE)) {
				stood.put(p.getUUID(), p.position());
				Sigils.target(level, p.position(), PALE, 1.0F, ArchivistRules.HUSH_HOLD);
			}
		}
		if (t > moveTell) {
			for (ServerPlayer p : foes(level, c, ArchivistRules.HUSH_RANGE + 4)) {
				Vec3 was = stood.get(p.getUUID());
				if (was == null || struck.contains(p.getUUID())) {
					continue;
				}
				// Anyone knocked about by something else isn't counted as walking: where they land is where they stand.
				if (p.hurtTime > 0) {
					stood.put(p.getUUID(), p.position());
					continue;
				}
				if (t % 5 == 0) {
					Vfx.emit(level, PALE_DUST, p.position().add(0, 2.4, 0), 2, 0.15, 0);
				}
				if (ArchivistRules.moved(p.getX() - was.x, p.getZ() - was.z)) {
					struck.add(p.getUUID());
					strike(level, p, ArchivistRules.HUSH_DAMAGE);
					ElementFx.ray(level, tomeAt(), p.getBoundingBox().getCenter(), PALE, 0.18, 6);
					Fx.sound(level, p.position(), SoundEvents.RESPAWN_ANCHOR_DEPLETE, 1.2F, 1.4F);
				}
			}
		}
	}

	/** It falls open: the moment one of its openings begins. */
	private void openingBegins(ServerLevel level, int state) {
		Vec3 tome = tomeAt();
		switch (state) {
			case ArchivistRules.SHUT -> {
				Fx.sound(level, tome, SoundEvents.CHISELED_BOOKSHELF_INSERT, 1.2F, 0.6F);
				Vfx.emit(level, INK_DUST, tome, 10, 0.2, 0.01);
			}
			case ArchivistRules.BLANK -> {
				Fx.sound(level, tome, SoundEvents.BEACON_DEACTIVATE, 1.0F, 0.6F);
				Vfx.radial(level, PALE_DUST, position().add(0, 1.2, 0), 24, 0.25);
			}
			case ArchivistRules.LEAFING -> {
				Fx.sound(level, tome, SoundEvents.BOOK_PAGE_TURN, 1.2F, 1.5F);
				Vfx.emit(level, PAGE, tome, 8, 0.2, 0.08);
			}
			default -> {
			}
		}
	}

	/** Standing spent in one of its openings: a tome sagging shut, pages wiped pale, or leafing for its place. */
	private void spent(ServerLevel level, int t) {
		Vec3 tome = tomeAt();
		if (move == ArchivistRules.SHUT && t % 6 == 0) {
			Vfx.emit(level, INK_DUST, tome.add(0, -0.2, 0), 2, 0.15, 0);
		} else if (move == ArchivistRules.BLANK && t % 3 == 0) {
			Vfx.emit(level, PALE_DUST, position().add(0, 0.4 + level.getRandom().nextDouble() * 1.6, 0), 2, 0.35, 0);
		} else if (move == ArchivistRules.LEAFING && t % 3 == 0) {
			Fx.sound(level, tome, SoundEvents.BOOK_PAGE_TURN, 0.6F, 1.2F + level.getRandom().nextFloat() * 0.4F);
			Vfx.emit(level, PAGE, tome, 1, 0.1, 0.05);
		}
	}

	// ------------------------------------------------------------------ what its tricks leave behind

	/** Books about to land, rings of script rolling out and lines of ink running, each striking anyone once. */
	private void tickHazards(ServerLevel level, long now) {
		if (volleyAgainAt > 0 && now >= volleyAgainAt) {
			volleyAgainAt = 0;
			layVolley(level, now, Math.max(1, ArchivistRules.scatter(phase) - 2));
		}
		// Copies, so that anything a blow sets off (a rewrite clearing them, say) can't upset the walk through them.
		for (Volley volley : new ArrayList<>(volleys)) {
			long left = volley.landAt() - now;
			if (left > 0) {
				for (Vec3 m : volley.marks()) {
					if (left % 3 == 0) {
						Vfx.ring(level, VIOLET_DUST, m.add(0, 0.12, 0), ArchivistRules.TOMES_RADIUS, 12);
					}
					// The books come into sight overhead just before they land.
					if (left <= 6) {
						Vfx.emit(level, BOOK, m.add(0, 1.0 + left * 0.8, 0), 1, 0.1, 0);
					}
				}
				continue;
			}
			volleys.remove(volley);
			int landed = 0;
			for (Vec3 m : volley.marks()) {
				Vfx.emit(level, SHELF, m.add(0, 0.3, 0), 14, 0.5, 0.15);
				Vfx.emit(level, BOOK, m.add(0, 0.4, 0), 4, 0.4, 0.1);
				ElementFx.groundRing(level, m, VIOLET, 0.2, ArchivistRules.TOMES_RADIUS + 0.4, 0.3, 6);
				if (landed < 3) {
					Fx.sound(level, m, SoundEvents.BOOK_PUT, 1.4F, 0.6F + 0.1F * landed);
					Fx.sound(level, m, SoundEvents.CHISELED_BOOKSHELF_FALL, 1.0F, 0.8F);
				}
				landed++;
				for (ServerPlayer p : foes(level, m, ArchivistRules.TOMES_RADIUS + 1.5)) {
					if (!ArchivistRules.inMark(p.getX() - m.x, p.getZ() - m.z, p.getY() - m.y, ArchivistRules.TOMES_RADIUS)
						|| !volley.struck().add(p.getUUID())) {
						continue;
					}
					strike(level, p, ArchivistRules.TOMES_DAMAGE);
					Effects.push(p, new Vec3(0, ArchivistRules.TOMES_LIFT, 0));
				}
			}
		}
		for (Wave wave : new ArrayList<>(waves)) {
			double from = wave.reached;
			double to = ArchivistRules.wave((int) (now - wave.began), wave.speed);
			wave.reached = to;
			int points = Mth.clamp((int) (to * 3), 10, 40);
			Vfx.ring(level, VIOLET_DUST, wave.centre.add(0, 0.15, 0), to, points);
			Vfx.ring(level, PALE_DUST, wave.centre.add(0, 0.55, 0), to, points);
			for (ServerPlayer p : foes(level, wave.centre, to + 1.5)) {
				double dx = p.getX() - wave.centre.x;
				double dz = p.getZ() - wave.centre.z;
				if (!ArchivistRules.crosses(Math.sqrt(dx * dx + dz * dz), from, to, p.getY() - wave.centre.y) || !wave.struck.add(p.getUUID())) {
					continue;
				}
				strike(level, p, ArchivistRules.RECITE_DAMAGE);
				Effects.push(p, flat(new Vec3(dx, 0, dz), aim).scale(0.6).add(0, 0.25, 0));
			}
			if (to >= ArchivistRules.RECITE_REACH) {
				waves.remove(wave);
			}
		}
		for (Lane lane : new ArrayList<>(lanes)) {
			int elapsed = (int) (now - lane.began());
			double front = ArchivistRules.quillFront(elapsed);
			Vec3 dir = lane.dir();
			Vec3 side = new Vec3(-dir.z, 0, dir.x);
			for (ServerPlayer p : foes(level, lane.from(), front + 1.5)) {
				Vec3 rel = p.position().subtract(lane.from());
				double forward = rel.x * dir.x + rel.z * dir.z;
				double across = rel.x * side.x + rel.z * side.z;
				if (!ArchivistRules.lane(forward, across, rel.y, 0.5, front) || !lane.struck().add(p.getUUID())) {
					continue;
				}
				strike(level, p, ArchivistRules.QUILL_DAMAGE);
				Effects.push(p, dir.scale(0.4).add(0, 0.15, 0));
			}
			if (elapsed >= ArchivistRules.QUILL_GROW) {
				lanes.remove(lane);
			}
		}
	}

	private void clearHazards() {
		volleys.clear();
		waves.clear();
		lanes.clear();
		volleyAgainAt = 0;
	}

	/** Marks where books will fall: one under each of the nearest foes, and {@code scatter} more about them. */
	private void layVolley(ServerLevel level, long now, int scatter) {
		List<ServerPlayer> near = foes(level, position(), 24);
		if (near.isEmpty()) {
			return;
		}
		near.sort((a, b) -> Double.compare(distanceToSqr(a), distanceToSqr(b)));
		int marked = Math.min(4, near.size());
		List<Vec3> marks = new ArrayList<>();
		for (int i = 0; i < marked; i++) {
			ServerPlayer p = near.get(i);
			marks.add(spot(level, p.getX(), p.getZ(), p.getY()));
		}
		for (int i = 0; i < scatter; i++) {
			ServerPlayer p = near.get(level.getRandom().nextInt(marked));
			double a = level.getRandom().nextDouble() * Math.PI * 2;
			double r = ArchivistRules.TOMES_NEAR + level.getRandom().nextDouble() * (ArchivistRules.TOMES_FAR - ArchivistRules.TOMES_NEAR);
			marks.add(spot(level, p.getX() + Math.cos(a) * r, p.getZ() + Math.sin(a) * r, p.getY()));
		}
		for (Vec3 m : marks) {
			Sigils.target(level, m, VIOLET, (float) ArchivistRules.TOMES_RADIUS, ArchivistRules.TOMES_FALL + 4);
		}
		Fx.sound(level, position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F, 0.8F);
		volleys.add(new Volley(marks, now + ArchivistRules.TOMES_FALL, new HashSet<>()));
	}

	// ------------------------------------------------------------------ helpers

	/** Everyone it may harm within {@code range} of {@code at} across the floor. */
	private List<ServerPlayer> foes(ServerLevel level, Vec3 at, double range) {
		List<ServerPlayer> found = new ArrayList<>();
		AABB box = new AABB(at.x - range, at.y - 6, at.z - range, at.x + range, at.y + 6, at.z + range);
		for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, box)) {
			if (!p.isAlive() || p.isSpectator() || p.isCreative() || !Targets.canHarm(this, p)) {
				continue;
			}
			double dx = p.getX() - at.x;
			double dz = p.getZ() - at.z;
			if (dx * dx + dz * dz <= range * range) {
				found.add(p);
			}
		}
		return found;
	}

	/** A blow from one of its tricks, as strong as its spells. */
	private void strike(ServerLevel level, LivingEntity victim, double damage) {
		Effects.readyToHurt(victim);
		SpellDefence.hurt(level, victim, level.damageSources().indirectMagic(this, this), (float) (damage * power()));
	}

	/** Turns it to face {@code dir} across the floor, head and body. */
	private void face(Vec3 dir) {
		float yaw = (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90;
		setYRot(yaw);
		yHeadRot = yaw;
		yBodyRot = yaw;
		getLookControl().setLookAt(getX() + dir.x * 4, getEyeY(), getZ() + dir.z * 4);
	}

	/** Stands its ground. */
	private void hold() {
		getNavigation().stop();
		getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0);
	}

	/** The way to {@code target} across the floor (straight ahead, if there is none). */
	private Vec3 toward(LivingEntity target) {
		return target == null ? forward() : flat(target.position().subtract(position()), forward());
	}

	private Vec3 forward() {
		float yaw = yBodyRot * Mth.DEG_TO_RAD;
		return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
	}

	/** {@code v} laid flat and made one block long, or {@code fallback} if it has no length across the floor. */
	private static Vec3 flat(Vec3 v, Vec3 fallback) {
		Vec3 f = new Vec3(v.x, 0, v.z);
		return f.lengthSqr() < 1.0E-4 ? fallback : f.normalize();
	}

	/** A spot on the floor at {@code x}, {@code z}, brought within the arena and dropped from just above {@code y}. */
	private Vec3 spot(ServerLevel level, double x, double z, double y) {
		Vec3 heart = home != null ? Vec3.atBottomCenterOf(home) : position();
		double[] in = ArchivistRules.within(x - heart.x, z - heart.z);
		return CastEngine.ground(level, new Vec3(heart.x + in[0], y + 1, heart.z + in[1]));
	}

	/** Where its tome is, held before it. */
	private Vec3 tomeAt() {
		float yaw = yBodyRot * Mth.DEG_TO_RAD;
		return position().add(-Mth.sin(yaw) * 0.62, 1.45, Mth.cos(yaw) * 0.62);
	}

	/** Its open pages pull at anyone near (though not anyone already at its feet). */
	private void pull(ServerLevel level) {
		for (ServerPlayer p : foes(level, position(), ArchivistRules.SNAP_PULL_RANGE)) {
			double dx = getX() - p.getX();
			double dz = getZ() - p.getZ();
			double d = Math.sqrt(dx * dx + dz * dz);
			if (d > 1.5) {
				Effects.push(p, new Vec3(dx / d, 0, dz / d).scale(ArchivistRules.SNAP_PULL * 4));
				Vfx.emit(level, ParticleTypes.ENCHANT, p.position().add(0, 1, 0), 3, 0.3, 0.1);
			}
		}
	}

	/** Wipes the longest of {@code player}'s blessings away (never a shield of absorption, nor one that never ends). */
	private void wipe(ServerLevel level, ServerPlayer player) {
		MobEffectInstance longest = null;
		for (MobEffectInstance effect : player.getActiveEffects()) {
			if (effect.isInfiniteDuration() || effect.is(MobEffects.ABSORPTION)
				|| effect.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL) {
				continue;
			}
			if (longest == null || effect.getDuration() > longest.getDuration()) {
				longest = effect;
			}
		}
		if (longest == null) {
			return;
		}
		player.removeEffect(longest.getEffect());
		Vfx.emit(level, PALE_DUST, player.position().add(0, 1, 0), 16, 0.4, 0.05);
		Fx.sound(level, player.position(), SoundEvents.GLASS_BREAK, 0.8F, 1.6F);
	}

	/** The rewrite tears its tome apart: pages burst from the book and the ring around it, with a flurry of pale light. */
	private void tearPages(ServerLevel level) {
		float yaw = yBodyRot * Mth.DEG_TO_RAD;
		Vec3 tome = position().add(-Mth.sin(yaw) * 0.62, 1.45, Mth.cos(yaw) * 0.62);
		Vfx.emit(level, PAGE, tome, 36, 0.25, 0.32);
		for (int i = 0; i < 12; i++) {
			double a = Math.PI * 2 * i / 12;
			Vfx.emit(level, PAGE, position().add(Math.cos(a) * 1.0, 1.9, Math.sin(a) * 1.0), 2, 0.15, 0.2);
		}
		Vfx.radial(level, new DustParticleOptions(0xF8EDCC, 0.9F), tome, 30, 0.35);
		Vfx.radial(level, new DustParticleOptions(0xE8DCFF, 0.7F), tome, 24, 0.5);
		Sigils.flash(level, tome, 0xF5C46A, 2.2F);
		Fx.sound(level, tome, SoundEvents.BOOK_PUT, 1.2F, 0.6F);
		Fx.sound(level, tome, SoundEvents.CHISELED_BOOKSHELF_PICKUP_ENCHANTED, 1.0F, 0.7F);
	}

	/** Its boss bar: shown to everyone in the Archive's arena, taken from anyone who has left it (or its world). */
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

	private void blink(ServerLevel level, Vec3 to) {
		Vec3 spot = CastEngine.ground(level, to.add(0, 3, 0));
		if (!level.noCollision(this, getDimensions(getPose()).makeBoundingBox(spot))) {
			return;
		}
		Vec3 from = position();
		Vfx.radial(level, ParticleTypes.REVERSE_PORTAL, from.add(0, 1.2, 0), 30, 0.3);
		teleportTo(spot.x, spot.y, spot.z);
		Vfx.radial(level, ParticleTypes.PORTAL, spot.add(0, 1.2, 0), 30, 0.4);
		Sigils.send(level, SigilOption.flat(SigilOption.CIRCLE, 0xB8A0FF, 1.2F, 20, 0.15F), spot.add(0, 0.06, 0));
		Fx.sound(level, spot, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.7F);
	}

	/** Its boss bar names the spell it is writing, its rewriting, or the trick it is at and what that leaves it open to. */
	private void updateName(String spell) {
		Component name = Component.translatable("entity.wildercord.archivist");
		if (spell != null) {
			name = Component.translatable("boss.wildercord.archivist_casting", name, Component.literal(spell).withColor(GOLD));
		} else if (rewriting > 0) {
			name = Component.translatable("boss.wildercord.archivist_rewriting", name);
		} else if (move > 0 && move < ArchivistRules.MOVES) {
			Component doing = Component.translatable("boss.wildercord.archivist.move." + ArchivistRules.key(move)).withColor(GOLD);
			name = Component.translatable("boss.wildercord.status", name, doing);
		}
		if (!name.equals(shownName)) {
			shownName = name;
			bossEvent.setName(name);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			castPoseO = castPose;
			rewritePoseO = rewritePose;
			boolean rewrite = isRewriting();
			int current = currentMove();
			castPose = Mth.approach(castPose, isCastingSpell() && !rewrite ? 1 : 0, 0.2F);
			rewritePose = Mth.approach(rewritePose, rewrite ? 1 : 0, 0.12F);
			for (int i = 0; i < ArchivistRules.MOVES; i++) {
				moveBlendO[i] = moveBlend[i];
				moveBlend[i] = Mth.approach(moveBlend[i], i == current && !rewrite ? 1 : 0, 0.25F);
			}
		}
	}

	/** Rewriting its Cord (known on both sides: the spell being cast is synced). */
	public boolean isRewriting() {
		return getCurrentSpell() == IllagerSpell.WOLOLO;
	}

	/** How far into the casting pose (arms raised, tome lifted), 0 to 1; for the renderer. */
	public float castPose(float partial) {
		return Mth.lerp(partial, castPoseO, castPose);
	}

	/** How far into the rewriting pose (arms thrown wide, pages flung out), 0 to 1; for the renderer. */
	public float rewritePose(float partial) {
		return Mth.lerp(partial, rewritePoseO, rewritePose);
	}

	/** The trick it is at (see {@link ArchivistRules}), or none; known on both sides. */
	public int currentMove() {
		return entityData.get(DATA_MOVE);
	}

	/** How long the trick under way is told before it is thrown, in ticks. */
	public int moveTell() {
		return entityData.get(DATA_MOVE_TELL);
	}

	/** How long the trick under way has been going, in ticks. */
	public float moveSince(float partial) {
		long began = entityData.get(DATA_MOVE_BEGAN);
		return Math.max(0, (float) (level().getGameTime() - began) + partial);
	}

	/** How far into the pose for trick {@code move}, 0 to 1, eased in and out; for the renderer. */
	public float moveBlend(int move, float partial) {
		if (move <= 0 || move >= ArchivistRules.MOVES) {
			return 0;
		}
		return Mth.lerp(partial, moveBlendO[move], moveBlend[move]);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		// A command's kill and the void always go through, even while it rewrites.
		if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
			return super.hurtServer(level, source, damage);
		}
		if (rewriting > 0 || source.is(DamageTypes.FALL) || source.getEntity() instanceof Mob && !(source.getEntity() instanceof Player)) {
			return false;
		}
		// Caught shut, wiped blank or leafing for its place, it takes more from every blow.
		boolean hurt = super.hurtServer(level, source, damage * (float) ArchivistRules.taken(move));
		// A blow that reached its next phase starts the rewriting now, before anything else can land.
		shiftIfDue(level);
		return hurt;
	}

	/**
	 * Held at the start of its next phase, like the dimension bosses (see {@link BossRules}): no one
	 * blow, or burst of them, carries it past a phase or kills it before its last.
	 */
	@Override
	protected void actuallyHurt(ServerLevel level, DamageSource source, float damage) {
		float before = getHealth();
		super.actuallyHurt(level, source, damage);
		if (!source.is(DamageTypes.GENERIC_KILL) && !source.is(DamageTypes.FELL_OUT_OF_WORLD) && rewriting == 0) {
			float capped = BossRules.capped(phase, getMaxHealth(), before, getHealth());
			if (capped != getHealth()) {
				setHealth(capped);
			}
		}
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		// Whatever it was at stops, and nothing it laid is left to fall.
		endMove();
		clearHazards();
		Vec3 c = position().add(0, 1.2, 0);
		Sigils.ground(level, position(), 0xF5C46A, 0xFFFFFF, 6.0F, 60);
		Vfx.radial(level, ParticleTypes.ENCHANT, c, 120, 1.0);
		Vfx.radial(level, new DustParticleOptions(0xF5C46A, 1.6F), c, 50, 0.5);
		Fx.sound(level, c, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 0.8F);
		Fx.sound(level, c, SoundEvents.BOOK_PUT, 1.5F, 0.5F);
		// (The 7th Circle's boss breakthrough is given to everyone near by HeartCircles, as for every boss.)
		for (ServerPlayer player : level.players()) {
			if (player.distanceTo(this) <= 64 && !player.isSpectator()) {
				Grimoire.feat(player, Feats.ARCHIVIST);
			}
		}
		// Its lectern goes quiet for good.
		if (home != null && level.getBlockEntity(home.below()) instanceof ArchiveLecternBlockEntity lectern) {
			lectern.slain();
		}
		// The allies it called go with it.
		for (UUID id : minions) {
			if (level.getEntity(id) instanceof Mob mob && mob.isAlive()) {
				Motes.clouds(level, mob.position().add(0, mob.getBbHeight() / 2, 0), 5, 0.3, Motes.SMOKE, 1.0, 30, new Vec3(0, 0.03, 0), 0.08, 0.45);
				mob.discard();
			}
		}
		minions.clear();
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		// A Tier IV rune the killer doesn't know yet, if there is one.
		List<RuneDef> fourth = new ArrayList<>();
		ServerPlayer killer = source.getEntity() instanceof ServerPlayer p ? p : null;
		for (RuneDef rune : Runes.all()) {
			if (rune.tier() == 4 && Runes.common(rune) && (killer == null || !Spellbooks.knows(killer, rune.id()))) {
				fourth.add(rune);
			}
		}
		if (fourth.isEmpty()) {
			Runes.all().stream().filter(r -> r.tier() == 4 && Runes.common(r)).forEach(fourth::add);
		}
		drop(level, RuneItem.stack(fourth.get(level.getRandom().nextInt(fourth.size()))));
		drop(level, new ItemStack(WildercordItems.TORN_PAGE, 2));
		drop(level, new ItemStack(WildercordItems.MANA_CRYSTAL, 3));
		drop(level, new ItemStack(dev.wildercord.aura.world.AuraWorld.AURA_SHARD, 2));
		var methods = dev.wildercord.aura.BreathingMethods.BUILT_IN;
		if (!methods.isEmpty()) {
			var method = methods.get(level.getRandom().nextInt(methods.size()));
			drop(level, dev.wildercord.aura.world.ManualPageItem.of(dev.wildercord.aura.world.AuraWorld.MANUAL_PAGE, method.id()));
		}
		// Now and then a greater staff.
		dev.wildercord.gear.GearLoot.bossStaff(level.getRandom(), dev.wildercord.gear.GearLoot.ARCHIVIST_CHANCE, dev.wildercord.gear.GearDef.ELEMENTS)
			.ifPresent(staff -> drop(level, staff));
		ExperienceOrb.award(level, position(), 250);
	}

	private void drop(ServerLevel level, ItemStack stack) {
		ItemEntity item = new ItemEntity(level, getX(), getY() + 1, getZ(), stack);
		item.setDeltaMovement(level.getRandom().nextGaussian() * 0.1, 0.35, level.getRandom().nextGaussian() * 0.1);
		level.addFreshEntity(item);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossEvent.removePlayer(player);
	}

	/** Its bar goes from every screen however it leaves: killed, discarded, unloaded with its chunk or taken to another world. */
	@Override
	public void onRemoval(RemovalReason reason) {
		bossEvent.removeAllPlayers();
		super.onRemoval(reason);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	/** Never led off in a boat or a minecart: it keeps to the Archive. */
	@Override
	protected boolean canRide(Entity vehicle) {
		return false;
	}

	/** Never through a portal: one that left would be missing from the Archive, and its lectern would wake another. */
	@Override
	public boolean canUsePortal(boolean ignorePassenger) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (home != null) {
			output.store("home", BlockPos.CODEC, home);
		}
		output.putInt("phase", phase);
		output.store("minions", UUIDUtil.CODEC.listOf(), List.copyOf(minions));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		home = input.read("home", BlockPos.CODEC).orElse(null);
		phase = input.getIntOr("phase", 1);
		minions.clear();
		input.read("minions", UUIDUtil.CODEC.listOf()).ifPresent(minions::addAll);
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	public boolean fireImmune() {
		return true;
	}

	@Override
	public boolean canJoinRaid() {
		return false;
	}

	@Override
	public void applyRaidBuffs(ServerLevel level, int wave, boolean isCaptain) {
	}

	@Override
	public SoundEvent getCelebrateSound() {
		return SoundEvents.EVOKER_CELEBRATE;
	}

	@Override
	protected SoundEvent getCastingSoundEvent() {
		return SoundEvents.EVOKER_CAST_SPELL;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.EVOKER_AMBIENT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.EVOKER_DEATH;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.EVOKER_HURT;
	}
}
