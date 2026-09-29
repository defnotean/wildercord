package dev.wildercord.cast;

import dev.wildercord.content.RuneItem;
import dev.wildercord.content.SigilOption;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.Secrets;
import dev.wildercord.spell.SpellNames;
import dev.wildercord.spell.SpellSigil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The Archivist: keeper of the Archive and its Tier IV runes. It fights with a Cord like any
 * caster: every spell it's about to cast is named on its boss bar and written out in its circle.
 * Twice it rewrites its Cord (at two thirds and one third of its health): a new phase, new
 * spells, and allies summoned from the stacks. Like the dimension bosses, no blow carries it past
 * the start of its next phase, so every phase is fought. In the last phase it opens a Domain of its
 * own (answer it with yours) and it knows at least one secret spell.
 */
public class Archivist extends SpellcasterIllager {
	private static final int TELEGRAPH = 28;
	private static final int REWRITE_TICKS = 50;
	private static final int DOMAIN_SECRET = -1;
	/** Its boss bar shows to players this near the Archive's heart, like the dimension bosses'. */
	private static final double BAR_RANGE = DungeonBoss.BAR_RANGE;

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
	private int spellIndex;
	/** How far its arms and tome are into the casting and rewriting poses, 0 to 1 (worked out on the client only). */
	private float castPose;
	private float castPoseO;
	private float rewritePose;
	private float rewritePoseO;

	public Archivist(EntityType<? extends Archivist> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.bossEvent.setDarkenScreen(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 400.0)
			.add(Attributes.MOVEMENT_SPEED, 0.42)
			.add(Attributes.FOLLOW_RANGE, 48.0)
			.add(Attributes.ARMOR, 8.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
	}

	/** Wakes the Archivist above its lectern. */
	public static void rise(ServerLevel level, BlockPos lectern) {
		Archivist boss = WildercordEntities.ARCHIVIST.create(level, EntitySpawnReason.TRIGGERED);
		if (boss == null) {
			return;
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
			if (rewriting == 0) {
				setIsCastingSpell(IllagerSpell.NONE);
				readyAt = now + 20;
			}
			return;
		}
		// A blow starts its rewriting at once (see hurtServer); a command setting its health still gets one for each phase.
		if (shiftIfDue(level)) {
			return;
		}
		LivingEntity target = getTarget();
		// Never strays far from the Archive's heart.
		if (home != null && position().distanceTo(Vec3.atCenterOf(home)) > 22) {
			blink(level, Vec3.atBottomCenterOf(home).add(level.getRandom().nextGaussian() * 4, 0, level.getRandom().nextGaussian() * 4));
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
				fire(level);
				readyAt = now + (phase == 3 ? 40 : 55) + level.getRandom().nextInt(30);
				updateName(null);
			}
			return;
		}
		if (target == null || !target.isAlive() || now < readyAt || !hasLineOfSight(target)) {
			if (target != null && distanceTo(target) > 16) {
				getNavigation().moveTo(target, 0.9);
			}
			return;
		}
		// Choose the next spell, in turn; in the last phase, sometimes a secret.
		castTarget = target;
		castAt = now + TELEGRAPH;
		spellCastingTickCount = TELEGRAPH + 6;
		setIsCastingSpell(IllagerSpell.FANGS);
		if (phase == 3 && level.getRandom().nextInt(4) == 0) {
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
		List<List<RuneDef>> spells = spells();
		casting = spellIndex++ % spells.size();
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
		spellIndex = 0;
		Vec3 c = position();
		tearPages(level);
		Sigils.ground(level, c.add(0, 0.05, 0), 0xB8A0FF, 0xF5C46A, 4.0F, REWRITE_TICKS + 10);
		Sigils.layer(level, c.add(0, 2.6, 0), new Vec3(0, 1, 0), SigilOption.RING, 0xF5C46A, 2.5F, REWRITE_TICKS + 10, 0.12F);
		Vfx.radial(level, ParticleTypes.ENCHANT, c.add(0, 1.4, 0), 80, 0.9);
		Fx.sound(level, c, SoundEvents.BOOK_PAGE_TURN, 1.5F, 0.5F);
		Fx.sound(level, c, SoundEvents.EVOKER_PREPARE_WOLOLO, 1.5F, 0.6F);
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
			Sigils.ground(level, spot, 0x9A6AD0, 0xE8E0FF, 1.2F, 30);
			Vfx.emit(level, ParticleTypes.SOUL, spot.add(0, 1, 0), 12, 0.3, 0.03);
		}
		updateName(null);
	}

	/** Paper, for the pages torn from its tome. */
	private static final ItemParticleOption PAGE = new ItemParticleOption(ParticleTypes.ITEM, Items.PAPER);

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

	private void updateName(String spell) {
		Component name = Component.translatable("entity.wildercord.archivist");
		if (spell != null) {
			name = Component.translatable("boss.wildercord.archivist_casting", name, Component.literal(spell).withColor(0xF5C46A));
		} else if (rewriting > 0) {
			name = Component.translatable("boss.wildercord.archivist_rewriting", name);
		}
		bossEvent.setName(name);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			castPoseO = castPose;
			rewritePoseO = rewritePose;
			boolean rewrite = isRewriting();
			castPose = Mth.approach(castPose, isCastingSpell() && !rewrite ? 1 : 0, 0.2F);
			rewritePose = Mth.approach(rewritePose, rewrite ? 1 : 0, 0.12F);
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

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		// A command's kill and the void always go through, even while it rewrites.
		if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
			return super.hurtServer(level, source, damage);
		}
		if (rewriting > 0 || source.is(DamageTypes.FALL) || source.getEntity() instanceof Mob && !(source.getEntity() instanceof Player)) {
			return false;
		}
		boolean hurt = super.hurtServer(level, source, damage);
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
		Vec3 c = position().add(0, 1.2, 0);
		Sigils.ground(level, position(), 0xF5C46A, 0xFFFFFF, 6.0F, 60);
		Vfx.radial(level, ParticleTypes.ENCHANT, c, 120, 1.0);
		Vfx.radial(level, new DustParticleOptions(0xF5C46A, 1.6F), c, 50, 0.5);
		Fx.sound(level, c, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 0.8F);
		Fx.sound(level, c, SoundEvents.BOOK_PUT, 1.5F, 0.5F);
		for (ServerPlayer player : level.players()) {
			if (player.distanceTo(this) <= 64) {
				Grimoire.feat(player, Feats.ARCHIVIST);
				if (!Heart.bossSlain(player)) {
					player.setAttached(WildercordAttachments.BOSS_SLAIN, true);
					player.sendSystemMessage(Component.translatable("message.wildercord.boss_breakthrough").withStyle(ChatFormatting.GOLD));
				}
			}
		}
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
		// Now and then a greater staff.
		dev.wildercord.gear.GearLoot.bossStaff(level.getRandom(), dev.wildercord.gear.GearLoot.ARCHIVIST_CHANCE, dev.wildercord.gear.GearDef.ELEMENTS)
			.ifPresent(staff -> drop(level, staff));
		ExperienceOrb.award(level, position(), 200);
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

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
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
