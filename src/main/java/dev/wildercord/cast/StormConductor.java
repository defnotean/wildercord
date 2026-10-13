package dev.wildercord.cast;

import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import dev.wildercord.content.dungeons.DungeonSounds;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Storm Conductor: a copper gyroscope rooted to its podium, conducting the storm with a baton, and the storm answers on the
 * beat. Its charge turns half of every blow aside. Ground it by standing beside the one of its three rods that sparks: it breaks
 * off whatever it was conducting and lies open to harm for a while, then the next rod takes up the spark.
 *
 * <p>The rods are shelter too, for a while: a bolt meant for anyone at a rod's foot is drawn down into the rod instead. So it
 * blows people off them, freezes them short of them, and from its second phase runs its chord between the rods that are not
 * sparking. Its ball lightning hunts one of its prey, and led into the sparking rod it is swallowed and grounds the Conductor
 * itself. It is winded after its held note and spent after it discharges; those are the times to hit it hard.</p>
 */
public final class StormConductor extends DungeonBoss {
	private static final int COLOR = 0x60CAFF, ACCENT = 0xFFCC72;
	/** How far out from its podium its arena runs. */
	private static final double ARENA = 14;
	private static final int BOLT = ElementFx.STORM.primary(), WHITE = ElementFx.STORM.secondary(), BLUE = ElementFx.STORM.accent();
	private static final int MINT = ElementFx.WIND.primary(), AIR = ElementFx.WIND.secondary();
	/** The beat of its baton, flicking one way and back, so the client can swing it in time. */
	private static final EntityDataAccessor<Float> DATA_BEAT = SynchedEntityData.defineId(StormConductor.class, EntityDataSerializers.FLOAT);

	private long groundedUntil, nextGround;
	private int rodIndex;
	private List<BlockPos> rods;
	/** When it last made each of its own moves, by id, so it waits a while before the same again. */
	private final long[] made = new long[StormConductorRules.MOVES + 1];
	/** Whoever this swing (and a second beat after it, and each wave of rolling thunder) has already caught: once each. */
	private final Set<UUID> struck = new HashSet<>(), struck2 = new HashSet<>();
	private final List<Set<UUID>> waves = new ArrayList<>();
	/** Where the move under way is aimed (locked a few ticks before it lands). */
	private float aim;
	/** Whether this move already follows straight on from another, and the opening it still owes after one that did. */
	private boolean chained;
	private int owed;
	/** Its marks on the floor (a downbeat's, or the squares of its counterpoint), and who each downbeat mark follows. */
	private final List<Vec3> marks = new ArrayList<>(), marks2 = new ArrayList<>();
	private final List<UUID> marked = new ArrayList<>();
	/** The lines of its chord, end to end. */
	private final List<Vec3[]> lines = new ArrayList<>();
	/** Its ball lightning, while it hunts. */
	private Vec3 ball;
	/** Where each of its prey stood when its held note last listened. */
	private final Map<UUID, Vec3> held = new HashMap<>();
	private float beat, beatO;

	public StormConductor(EntityType<? extends StormConductor> type, Level level) {
		super(type, level, BossEvent.BossBarColor.BLUE);
		Arrays.fill(made, Long.MIN_VALUE / 2);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMonsterAttributes().add(Attributes.MAX_HEALTH, 240).add(Attributes.MOVEMENT_SPEED, .24)
			.add(Attributes.FOLLOW_RANGE, 32).add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.ATTACK_DAMAGE, 7);
	}

	public static StormConductor rise(ServerLevel level, BlockPos altar) {
		StormConductor boss = place(level, DungeonEntities.STORM_CONDUCTOR, altar, 1);
		if (boss != null) boss.announce(level, "message.wildercord.storm_conductor_wakes", COLOR);
		return boss;
	}

	/** Its spells fill the gaps between its own moves: a few, and more wanted by prey that keep far off. */
	@Override protected List<List<RuneDef>> spells(int phase) {
		return switch (phase) {
			case 1 -> List.of(List.of(Runes.ARC, Runes.SHOCK));
			case 2 -> List.of(List.of(Runes.RAIN, Runes.SHOCK), List.of(Runes.PRISM, Runes.WINDCUT));
			default -> List.of(List.of(Runes.BOLT, Runes.LIGHTNING), List.of(Runes.LANCE, Runes.WINDCUT));
		};
	}

	@Override protected String feat() { return "storm_conductor"; }
	@Override protected int color() { return COLOR; }
	@Override protected int accent() { return ACCENT; }
	/** Rooted to its podium: it only turns to follow its prey. */
	@Override protected void approach(ServerLevel level, LivingEntity target) { getNavigation().stop(); getLookControl().setLookAt(target, 30, 30); }

	// ------------------------------------------------------------------ its baton

	@Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(DATA_BEAT, 0.0F); }

	@Override public void tick() {
		super.tick();
		if (level().isClientSide()) { beatO = beat; beat = Mth.approach(beat, entityData.get(DATA_BEAT), 0.5F); }
	}

	/** 0: the beat of its baton, swinging 0 to 1 and back on each beat. */
	@Override public float extra(int i, float partial) { return i == 0 ? Mth.lerp(partial, beatO, beat) : 0; }

	/** Its baton flicks over to the other side, on the beat. */
	private void beat() { entityData.set(DATA_BEAT, entityData.get(DATA_BEAT) > 0.5F ? 0.0F : 1.0F); }

	// ------------------------------------------------------------------ its own moves

	@Override protected int ownMoves() { return StormConductorRules.MOVES; }

	@Override protected void weigh(ServerLevel level, LivingEntity target, double[] weights) {
		if (home == null) return;
		long now = level.getGameTime();
		long[] since = new long[StormConductorRules.MOVES + 1];
		for (int m = 1; m <= StormConductorRules.MOVES; m++) since[m] = now - made[m];
		BlockPos on = activeRod();
		boolean nearRod = !rods.isEmpty() && flat(target.position(), Vec3.atBottomCenterOf(on)) <= StormConductorRules.NEAR_ROD;
		boolean sheltered = false;
		for (BlockPos p : standing()) sheltered |= flat(target.position(), Vec3.atBottomCenterOf(p)) <= StormConductorRules.ROD_DRAW + 1;
		boolean behind = StormConductorRules.behind(yawTo(target.position()) - getYRot());
		double[] w = StormConductorRules.weights(phase, distanceTo(target), nearRod, sheltered, behind, quiet().size(), since);
		for (int i = 0; i < StormConductorRules.PICKS.length; i++) weights[StormConductorRules.PICKS[i]] = w[i];
	}

	@Override protected double spellWeight(ServerLevel level, LivingEntity target, int index) { return StormConductorRules.spellWeight(distanceTo(target)); }

	@Override protected void beginMove(ServerLevel level, int move, LivingEntity target) { chained = false; owed = 0; start(level, move, target); }

	/** Into {@code move} at {@code target} from its start: its aim taken, nothing yet caught, and its tell under way. */
	private void start(ServerLevel level, int move, LivingEntity target) {
		aim = yawTo(target.position());
		turnTo(aim);
		struck.clear(); struck2.clear(); waves.clear(); marks.clear(); marks2.clear(); marked.clear(); lines.clear(); held.clear();
		ball = null;
		made[move] = level.getGameTime();
		pose(move, StormConductorRules.tell(move), StormConductorRules.length(move, phase));
		begun(level, move, target);
	}

	@Override protected void moveDone(ServerLevel level, int move, LivingEntity target) {
		marks.clear(); marks2.clear(); marked.clear(); lines.clear(); held.clear(); ball = null;
		switch (move) {
			// Its held note leaves it winded, unless in its third phase it brings the baton down on everyone it held still first.
			case StormConductorRules.FERMATA -> { if (chainOn(level, move, target)) owed = StormConductorRules.WINDED; else opening(level, StormConductorRules.WINDED); }
			case StormConductorRules.DISCHARGE -> opening(level, StormConductorRules.SPENT);
			case StormConductorRules.WINDED, StormConductorRules.SPENT -> { }
			default -> {
				if (owed > 0) { int o = owed; owed = 0; opening(level, o); }
				else chainOn(level, move, target);
			}
		}
	}

	/** In its third phase, what it may follow {@code move} with in the same breath (once, never twice running). True if it did. */
	private boolean chainOn(ServerLevel level, int move, LivingEntity target) {
		if (phase < 3 || chained || target == null || StormConductorRules.opening(move)) return false;
		int next = StormConductorRules.chain(move, random.nextDouble());
		if (next <= 0 || !StormConductorRules.open(next, phase, quiet().size())
			|| level.getGameTime() - made[next] < StormConductorRules.cooldown(next, phase) / 2) return false;
		deck.played(next);
		chained = true;
		start(level, next, target);
		return true;
	}

	/** Left open after its held note or its discharge, its baton drooping and its core dark. */
	private void opening(ServerLevel level, int move) {
		pose(move, 0, StormConductorRules.length(move, phase));
		Vec3 core = position().add(0, 1.3, 0);
		Feels.sound(level, core, "storm_fizz", 1.4F, 0.6F);
		Feels.sound(level, core, "storm_sizzle", 1.0F, 0.5F);
		Motes.smoke(level, core, 14, 0.6);
		Vfx.radial(level, ParticleTypes.ELECTRIC_SPARK, core, 18, 0.25);
		Sigils.ground(level, podium(), ACCENT, COLOR, 2.5F, 30);
	}

	@Override protected void moveBroken(ServerLevel level, int move) {
		marks.clear(); marks2.clear(); marked.clear(); lines.clear(); held.clear(); waves.clear();
		ball = null; owed = 0;
		entityData.set(DATA_BEAT, 0.0F);
	}

	@Override protected int rest(ServerLevel level, int move) { return move < SPELL ? StormConductorRules.rest(move, phase) : super.rest(level, move); }

	/** What each move sets out before its tell begins. */
	private void begun(ServerLevel level, int move, LivingEntity target) {
		Vec3 c = podium();
		switch (move) {
			case StormConductorRules.DOWNBEAT -> { markPrey(level); Feels.sound(level, tip(), "storm_hum", 1.3F, 0.7F); }
			case StormConductorRules.CRESCENDO -> Feels.sound(level, c, "storm_whine", 1.2F, 0.5F);
			case StormConductorRules.STACCATO -> Feels.sound(level, tip(), "storm_pip", 1.2F, 0.8F);
			case StormConductorRules.FERMATA -> { Feels.sound(level, tip(), "storm_hum", 1.6F, 0.5F); Feels.sound(level, c, "note_effect", 1.4F, 0.5F); }
			case StormConductorRules.GALE -> Feels.sound(level, position(), "wind_rise", 1.4F, 0.8F);
			case StormConductorRules.DISCHARGE -> Feels.sound(level, position(), "storm_crackle_wake", 1.5F, 0.8F);
			case StormConductorRules.BALL -> Feels.sound(level, tip(), "storm_magnet", 1.3F, 0.8F);
			case StormConductorRules.CHORD -> {
				List<BlockPos> q = quiet();
				if (q.size() >= 2) {
					Vec3 a = Vec3.atBottomCenterOf(q.get(0)), b = Vec3.atBottomCenterOf(q.get(1)), me = new Vec3(c.x, a.y, c.z);
					lines.add(new Vec3[] {a, b}); lines.add(new Vec3[] {a, me}); lines.add(new Vec3[] {b, me});
				}
			}
			case StormConductorRules.ROLL -> Feels.sound(level, c, "tell_rumble", 1.6F, 0.6F);
			case StormConductorRules.COUNTERPOINT -> {
				// Its squares, laid out across whatever floor there is round its podium.
				int n = (int) Math.ceil(StormConductorRules.COUNTER_REACH / StormConductorRules.COUNTER_TILE);
				for (int i = -n; i <= n; i++) for (int j = -n; j <= n; j++) {
					if (!StormConductorRules.reaches(i, j)) continue;
					Vec3 at = new Vec3(c.x + StormConductorRules.centre(i), c.y, c.z + StormConductorRules.centre(j));
					BlockPos p = BlockPos.containing(at);
					if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty() || level.getBlockState(p.below()).getCollisionShape(level, p.below()).isEmpty()) continue;
					(StormConductorRules.half(i, j) == 0 ? marks : marks2).add(at);
				}
				Feels.sound(level, c, "note_shape", 1.4F, 0.7F);
			}
			default -> { }
		}
	}

	@Override protected void moveTick(ServerLevel level, int move, int t, LivingEntity target) {
		if (home == null) return;
		switch (move) {
			case StormConductorRules.DOWNBEAT -> downbeat(level, t, target);
			case StormConductorRules.CRESCENDO -> crescendo(level, t);
			case StormConductorRules.STACCATO -> staccato(level, t, target);
			case StormConductorRules.FERMATA -> fermata(level, t);
			case StormConductorRules.GALE -> gale(level, t, target);
			case StormConductorRules.DISCHARGE -> discharge(level, t);
			case StormConductorRules.BALL -> ball(level, t, target);
			case StormConductorRules.CHORD -> chord(level, t);
			case StormConductorRules.ROLL -> roll(level, t);
			case StormConductorRules.COUNTERPOINT -> counterpoint(level, t);
			case StormConductorRules.WINDED, StormConductorRules.SPENT -> slumped(level, move, t);
			default -> { }
		}
	}

	// ------------------------------------------------------------------ the downbeat

	/**
	 * Its baton raised high behind it while a mark follows each of its prey, then holds; brought down, and a bolt falls onto every
	 * mark. A mark at a rod's foot sends its bolt into the rod. In its third phase a second beat follows where they then stand.
	 */
	private void downbeat(ServerLevel level, int t, LivingEntity target) {
		int tell = StormConductorRules.DOWN_TELL, lock = StormConductorRules.DOWN_LOCK, again = StormConductorRules.DOWN_AGAIN;
		track(t, tell, lock, target);
		if (t < tell) { gather(level, t, tell); follow(level, t, tell - lock); }
		else if (t == tell) {
			beat(); fall(level, struck);
			if (phase >= 3) { markPrey(level); Feels.sound(level, tip(), "tell_zap", 1.2F, 1.2F); }
		}
		else if (phase >= 3 && t < tell + again) { gather(level, t - tell, again); follow(level, t - tell, again - lock); }
		else if (phase >= 3 && t == tell + again) { beat(); fall(level, struck2); }
	}

	/** A mark under each of its prey. */
	private void markPrey(ServerLevel level) {
		marks.clear(); marked.clear();
		for (LivingEntity e : prey(level)) { marks.add(e.position()); marked.add(e.getUUID()); }
	}

	/** Its marks following their prey until {@code until} ticks in, then holding where they are, flashing as they hold. */
	private void follow(ServerLevel level, int k, int until) {
		Map<UUID, LivingEntity> by = new HashMap<>();
		for (LivingEntity e : prey(level)) by.put(e.getUUID(), e);
		for (int i = 0; i < marks.size(); i++) {
			LivingEntity e = by.get(marked.get(i));
			if (k <= until && e != null) marks.set(i, e.position());
			Vec3 m = marks.get(i);
			boolean drawn = drawnBy(level, m) != null;
			if (k == until) {
				Sigils.flash(level, m.add(0, 0.2, 0), WHITE, 1.6F);
				Feels.sound(level, m, "tell_tick", 1.0F, 1.4F);
				if (drawn) ElementFx.arc(level, m.add(0, 0.2, 0), rodTop(drawnBy(level, m)), BLUE, 0.06, 1, false, 6);
			}
			if (k % 2 == 0) {
				Sigils.target(level, m, k >= until ? WHITE : drawn ? BLUE : BOLT, (float) StormConductorRules.DOWN_RADIUS, 3);
				Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, m.add(0, 0.1, 0), 1, StormConductorRules.DOWN_RADIUS * 0.5, 0.01);
			}
		}
	}

	/** The storm gathering at the tip of its baton, louder as it nears the beat. */
	private void gather(ServerLevel level, int k, int tell) {
		Vec3 top = tip();
		if (k % 2 == 0) Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, top, 2 + k / 3, 0.25, 0.02);
		if (k % 4 == 0) Motes.glows(level, top, 2, 0.2, WHITE, 0.15, 6, Vec3.ZERO, 0.02);
		if (k == tell - 6) Feels.sound(level, top, "storm_whine", 1.0F, 1.4F);
	}

	/** Its baton brought down: a bolt onto every mark (or into the rod that draws it), striking whoever stands on one. */
	private void fall(ServerLevel level, Set<UUID> swing) {
		List<LivingEntity> prey = prey(level);
		boolean any = false;
		for (Vec3 m : marks) {
			BlockPos rod = drawnBy(level, m);
			if (rod != null) {
				Vec3 top = rodTop(rod);
				ElementFx.bolt(level, top.add(0, 9, 0), top, 0.3, 2, 2);
				ElementFx.sparks(level, top, 12, 0.4);
				Feels.sound(level, top, "storm_zap", 1.3F, 0.8F);
				continue;
			}
			ElementFx.bolt(level, m.add(0, 10, 0), m.add(0, 0.1, 0), 0.35, 2, 2);
			Vfx.lightning(level, m);
			any = true;
			for (LivingEntity e : prey) {
				Vec3 d = e.position().subtract(m);
				if (StormConductorRules.marked(d.x, d.z, d.y)) strike(level, e, StormConductorRules.DOWN_DAMAGE, swing);
			}
		}
		if (any) Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, tip(), 12, 0.3, 0.2);
	}

	// ------------------------------------------------------------------ the crescendo

	/** Its baton circling faster and faster over its head; then a low ring of lightning rolls out along the floor. Jump it. */
	private void crescendo(ServerLevel level, int t) {
		int tell = StormConductorRules.CRESC_TELL;
		Vec3 c = podium();
		int rolls = StormConductorRules.rolls(StormConductorRules.CRESC_REACH, StormConductorRules.CRESC_SPEED);
		if (t < tell) {
			if (t % 4 == 0) {
				ElementFx.groundRing(level, c, BLUE, 0.5, 1.5 + t * 0.12, 0.2, 5);
				Feels.sound(level, c, "storm_pip", 0.9F, 0.6F + t * 0.06F);
			}
			double a = t * t * 0.03;
			Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, tip().add(Math.cos(a) * 0.8, 0, Math.sin(a) * 0.8), 2, 0.05, 0.01);
			if (t == tell - 4) Feels.sound(level, c, "tell_rumble", 1.2F, 1.2F);
			return;
		}
		ring(level, t - tell, rolls, struck, t == tell);
		if (phase >= 3) {
			int k = t - tell - StormConductorRules.CRESC_AGAIN;
			if (k >= 0) ring(level, k, rolls, struck2, k == 0);
			else if (k > -6 && k % 2 == 0) ElementFx.groundRing(level, c, BLUE, 0.5, 2.0, 0.2, 4);
		}
	}

	/** One ring of its crescendo, {@code k} ticks out from its podium. */
	private void ring(ServerLevel level, int k, int rolls, Set<UUID> swing, boolean first) {
		if (k >= rolls) return;
		Vec3 c = podium();
		if (first) {
			beat();
			ElementFx.groundRing(level, c.add(0, 0.05, 0), BOLT, 0.3, StormConductorRules.CRESC_REACH, 0.4, rolls);
			ElementFx.groundRing(level, c.add(0, 0.1, 0), WHITE, 0.2, StormConductorRules.CRESC_REACH, 0.15, rolls);
			Feels.sound(level, c, "storm_crack", 1.4F, 1.3F);
			Feels.sound(level, c, "storm_roll", 1.0F, 1.4F);
		}
		double from = k * StormConductorRules.CRESC_SPEED, to = from + StormConductorRules.CRESC_SPEED;
		if (k % 2 == 0) for (int i = 0; i < 8; i++) {
			double a = i * Math.PI / 4 + k * 0.2;
			Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, c.add(Math.cos(a) * to, 0.3, Math.sin(a) * to), 1, 0.1, 0.02);
		}
		for (LivingEntity e : prey(level)) {
			double d = flat(e.position(), c);
			if (StormConductorRules.crosses(d, from, to, e.getY() - c.y) && strike(level, e, StormConductorRules.CRESC_DAMAGE, swing))
				push(e, e.position().subtract(c), StormConductorRules.CRESC_SHOVE, 0.3);
		}
	}

	// ------------------------------------------------------------------ the staccato

	/** Quick flicks of its baton while a line of sparks is marked toward its prey; then the sparks pop along it, near to far, on the beat. */
	private void staccato(ServerLevel level, int t, LivingEntity target) {
		int tell = StormConductorRules.STAC_TELL, lock = StormConductorRules.STAC_LOCK, n = StormConductorRules.lines(phase);
		track(t, tell, lock, target);
		Vec3 c = podium();
		if (t < tell) {
			if (t % 3 == 0) { beat(); Feels.sound(level, tip(), "storm_tick", 0.9F, 1.2F + t * 0.03F); }
			if (t % 2 == 0) for (int l = 0; l < n; l++) {
				Vec3 dir = ahead(aim + (float) StormConductorRules.lane(l, n, StormConductorRules.STAC_SPREAD, 0));
				Vec3 end = c.add(dir.scale(StormConductorRules.spark(StormConductorRules.STAC_COUNT - 1)));
				ElementFx.ray(level, c.add(dir.scale(StormConductorRules.spark(0))).add(0, 0.08, 0), end.add(0, 0.08, 0), t >= tell - lock ? WHITE : BOLT, 0.06, 3);
				for (int i = 0; i < StormConductorRules.STAC_COUNT; i += 3)
					Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, c.add(dir.scale(StormConductorRules.spark(i))).add(0, 0.15, 0), 1, 0.05, 0.0);
			}
			if (t == tell - lock) Feels.sound(level, c, "tell_tick", 1.0F, 1.5F);
			return;
		}
		int i = StormConductorRules.popping(t - tell);
		if (i < 0) return;
		beat();
		List<LivingEntity> prey = prey(level);
		for (int l = 0; l < n; l++) {
			Vec3 at = c.add(ahead(aim + (float) StormConductorRules.lane(l, n, StormConductorRules.STAC_SPREAD, 0)).scale(StormConductorRules.spark(i)));
			ElementFx.bolt(level, at.add(0, 2.5, 0), at.add(0, 0.1, 0), 0.12, 0, 1);
			ElementFx.sparks(level, at.add(0, 0.2, 0), 6, 0.3);
			Sigils.flash(level, at.add(0, 0.4, 0), WHITE, 1.2F);
			for (LivingEntity e : prey) {
				Vec3 d = e.position().subtract(at);
				if (StormConductorRules.sparked(d.x, d.z, e.getY() - c.y)) strike(level, e, StormConductorRules.STAC_DAMAGE, struck);
			}
		}
		Feels.sound(level, c.add(ahead(aim).scale(StormConductorRules.spark(i))), "storm_pip", 1.2F, 0.8F + i * 0.08F);
	}

	// ------------------------------------------------------------------ the held note

	/** Its baton held up, still, while the hall hums: lightning finds whoever moves while the note is held. Then it is winded. */
	private void fermata(ServerLevel level, int t) {
		int tell = StormConductorRules.FERM_TELL, hold = StormConductorRules.FERM_HOLD;
		Vec3 c = podium();
		List<LivingEntity> prey = prey(level);
		if (t < tell) {
			if (t % 3 == 0) for (LivingEntity e : prey) Sigils.target(level, e.position(), BLUE, 0.8F, 4);
			if (t % 4 == 0) ElementFx.groundRing(level, c, BLUE, ARENA, ARENA - 2, 0.2, 5);
			Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, tip(), 2, 0.15, 0.01);
			if (t == tell - 4) Feels.sound(level, c, "tell_toll", 1.4F, 0.8F);
			return;
		}
		int k = t - tell;
		if (k == 0) {
			Feels.sound(level, c, "storm_hour", 1.6F, 0.7F);
			Sigils.flash(level, tip(), WHITE, 2.4F);
			for (LivingEntity e : prey) held.put(e.getUUID(), e.position());
		}
		if (k > hold) return;
		if (k % 4 == 0) Feels.sound(level, c, "storm_hum", 1.2F, 0.6F);
		if (k % 2 == 0) {
			for (int i = 0; i < 4; i++) {
				double a = random.nextDouble() * Math.PI * 2, r = 2 + random.nextDouble() * (ARENA - 4);
				Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, c.add(Math.cos(a) * r, 1 + random.nextDouble() * 3, Math.sin(a) * r), 1, 0.1, 0.0);
			}
			for (LivingEntity e : prey) if (!struck.contains(e.getUUID())) ElementFx.flatSigil(level, e.position(), SigilOption.RING, BLUE, 0.7, 3, 0.0);
		}
		if (k == 0 || k % StormConductorRules.FERM_EVERY != 0) return;
		for (LivingEntity e : prey) {
			Vec3 was = held.put(e.getUUID(), e.position());
			if (was == null || StormConductorRules.still(flat(e.position(), was))) continue;
			if (strike(level, e, StormConductorRules.FERM_DAMAGE, struck)) {
				ElementFx.bolt(level, e.position().add(0, 9, 0), e.position(), 0.3, 2, 2);
				Vfx.lightning(level, e.position());
			}
		}
	}

	// ------------------------------------------------------------------ the gale

	/** Its baton swept back while the air is drawn in toward it across a wedge of floor; then swung across, and a gale blows the wedge clear. */
	private void gale(ServerLevel level, int t, LivingEntity target) {
		int tell = StormConductorRules.GALE_TELL, lock = StormConductorRules.GALE_LOCK;
		boolean wide = phase >= 3;
		double angle = wide ? StormConductorRules.GALE_ANGLE_WIDE : StormConductorRules.GALE_ANGLE, reach = StormConductorRules.GALE_REACH;
		track(t, tell, lock, target);
		Vec3 c = podium();
		if (t < tell) {
			if (t % 3 == 0) {
				int color = t >= tell - lock ? AIR : MINT;
				for (int s = -1; s <= 1; s += 2) ElementFx.ray(level, c.add(0, 0.08, 0), c.add(ahead(aim + (float) (s * angle)).scale(reach)).add(0, 0.08, 0), color, 0.08, 4);
				for (int i = -2; i < 2; i++) {
					Vec3 a = c.add(ahead(aim + (float) (i * angle / 2)).scale(reach)), b = c.add(ahead(aim + (float) ((i + 1) * angle / 2)).scale(reach));
					ElementFx.ray(level, a.add(0, 0.08, 0), b.add(0, 0.08, 0), color, 0.05, 4);
				}
				ElementFx.swirl(level, position(), 1.6, 3.2, 2, MINT, AIR);
			}
			// The air drawn in along the floor toward it, from all across the wedge.
			double a = aim + (random.nextDouble() * 2 - 1) * angle, r = 3 + random.nextDouble() * (reach - 3);
			Vec3 from = c.add(ahead((float) a).scale(r)).add(0, 0.4 + random.nextDouble(), 0);
			Motes.clouds(level, from, 1, 0.2, AIR, 0.5, 8, c.subtract(from).normalize().scale(0.25), 0.02, 0.4);
			if (t == 0) Feels.sound(level, c, "wind_eddy", 1.2F, 0.7F);
			if (t == tell - lock) { Sigils.flash(level, tip(), AIR, 1.6F); Feels.sound(level, c, "tell_tick", 1.0F, 1.3F); }
			if (t == tell - 5) Feels.sound(level, c, "wind_whirl", 1.3F, 1.1F);
			return;
		}
		if (t == tell) {
			beat();
			Feels.sound(level, c, "wind_gale", 1.6F, 0.9F);
			Feels.sound(level, c, "wind_crash", 1.2F, 1.2F);
			ElementFx.gustRing(level, c, 3.0);
			ElementFx.windImpact(level, position().add(ahead(aim).scale(1.6)).add(0, 1.2, 0), 1.4);
			for (int i = 0; i < 9; i++) {
				Vec3 dir = ahead(aim + (float) ((i / 4.0 - 1) * angle));
				Motes.clouds(level, c.add(dir.scale(1.5)).add(0, 0.6, 0), 2, 0.3, AIR, 0.7, 10, dir.scale(0.8), 0.05, 0.5);
				Vfx.fling(level, ParticleTypes.SMALL_GUST, c.add(dir.scale(2)).add(0, 0.8, 0), dir, 0.6);
			}
			Vec3 f = ahead(aim);
			for (LivingEntity e : prey(level)) {
				Vec3 d = e.position().subtract(c);
				double forward = d.x * f.x + d.z * f.z, side = d.x * -f.z + d.z * f.x;
				if (!StormConductorRules.blown(forward, side, e.getY() - c.y, wide) || !strike(level, e, StormConductorRules.GALE_DAMAGE, struck)) continue;
				boolean low = e.isShiftKeyDown();
				push(e, new Vec3(d.x, 0, d.z).normalize().add(f).scale(0.5), StormConductorRules.shove(low), low ? 0 : 0.25);
			}
		}
		else if (t < tell + 4) {
			for (int i = 0; i < 3; i++) {
				Vec3 dir = ahead(aim + (float) ((random.nextDouble() * 2 - 1) * angle));
				Motes.clouds(level, c.add(dir.scale(2 + (t - tell) * 2)).add(0, 0.6, 0), 1, 0.3, AIR, 0.6, 8, dir.scale(0.6), 0.05, 0.4);
			}
		}
	}

	// ------------------------------------------------------------------ the discharge

	/** Its baton pointed straight down and its cage spun up, crackling ever louder round its feet; then it lets it all go at once. Get away. */
	private void discharge(ServerLevel level, int t) {
		int tell = StormConductorRules.DIS_TELL;
		boolean wide = phase >= 3;
		double radius = wide ? StormConductorRules.DIS_RADIUS_WIDE : StormConductorRules.DIS_RADIUS;
		Vec3 c = podium();
		if (t < tell) {
			if (t % 3 == 0) {
				ElementFx.groundRing(level, c.add(0, 0.05, 0), t >= tell - 4 ? WHITE : BOLT, radius - 0.2, radius, 0.15, 4);
				Feels.sound(level, c, "storm_whine", 0.9F, 0.8F + t * 0.06F);
			}
			for (int i = 0; i < 2 + t / 4; i++) {
				double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * radius;
				Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, c.add(Math.cos(a) * r, 0.15, Math.sin(a) * r), 1, 0.05, 0.05);
			}
			if (t % 4 == 1) ElementFx.arc(level, position().add(0, 1.2, 0), c.add(random.nextDouble() * 2 - 1, 0.1, random.nextDouble() * 2 - 1), BLUE, 0.05, 1, false, 4);
			if (t == tell - 4) Feels.sound(level, c, "tell_crack", 1.4F, 1.0F);
			return;
		}
		if (t != tell) return;
		beat();
		ElementFx.stormImpact(level, position().add(0, 0.8, 0), wide ? 1.6 : 1.3);
		ElementFx.groundRing(level, c.add(0, 0.05, 0), WHITE, 0.5, radius + 0.5, 0.4, 6);
		ElementFx.groundRing(level, c.add(0, 0.1, 0), BOLT, 0.3, radius, 0.2, 8);
		for (int i = 0; i < 10; i++) {
			double a = i * Math.PI / 5;
			ElementFx.arc(level, position().add(0, 1.0, 0), c.add(Math.cos(a) * radius, 0.1, Math.sin(a) * radius), WHITE, 0.07, 2, true, 6);
		}
		Vfx.thunderclap(level, position().add(0, 1, 0), radius, false);
		Feels.sound(level, c, "storm_boom", 1.6F, 1.1F);
		for (LivingEntity e : prey(level)) {
			if (StormConductorRules.discharged(flat(e.position(), c), e.getY() - c.y, wide) && strike(level, e, StormConductorRules.DIS_DAMAGE, struck))
				push(e, e.position().subtract(c), StormConductorRules.DIS_SHOVE, 0.35);
		}
	}

	// ------------------------------------------------------------------ the ball lightning

	/**
	 * A ball of lightning swelling at the tip of its baton, then let go to drift after one of its prey, slower than a walk; it bursts on
	 * whoever it touches, or where it is when its time runs out. Led into the sparking rod it is swallowed, and grounds the Conductor.
	 */
	private void ball(ServerLevel level, int t, LivingEntity target) {
		int tell = StormConductorRules.BALL_TELL, life = StormConductorRules.BALL_LIFE, length = StormConductorRules.length(StormConductorRules.BALL, phase);
		Vec3 c = podium();
		LivingEntity hunted = target != null && target.isAlive() ? target : nearest(level, ball != null ? ball : position());
		if (t < tell) {
			ElementFx.orb(level, tip(), BOLT, 0.15 + t * 0.03, 3);
			Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, tip(), 2, 0.2, 0.03);
			if (t % 3 == 0 && hunted != null) Sigils.target(level, hunted.position(), BOLT, 0.8F, 4);
			if (t % 5 == 0) Feels.sound(level, tip(), "storm_hum", 0.9F, 0.8F + t * 0.04F);
			if (t == tell - 4) Feels.sound(level, tip(), "tell_zap", 1.1F, 0.9F);
			return;
		}
		if (t == tell) { ball = tip(); beat(); Feels.sound(level, ball, "storm_pluck", 1.3F, 0.7F); }
		if (ball == null) return;
		boolean stopped = t >= tell + life;
		if (!stopped && hunted != null) {
			double step = phase >= 3 ? StormConductorRules.BALL_FOLLOW_FAST : StormConductorRules.BALL_FOLLOW;
			double dx = hunted.getX() - ball.x, dz = hunted.getZ() - ball.z, f = StormConductorRules.pace(dx, dz, step);
			double y = ball.y + (c.y + StormConductorRules.BALL_HEIGHT - ball.y) * 0.15;
			Vec3 next = new Vec3(ball.x + dx * f, y, ball.z + dz * f);
			double away = flat(next, c);
			if (away > ARENA) next = new Vec3(c.x + (next.x - c.x) * ARENA / away, next.y, c.z + (next.z - c.z) * ARENA / away);
			ball = next;
		}
		double size = stopped ? 0.6 + (t - tell - life) * 0.05 : 0.6;
		ElementFx.orb(level, ball, stopped && t % 2 == 0 ? WHITE : BOLT, size, 3);
		if (t % 2 == 0) Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, ball, 2, 0.3, 0.03);
		if (t % 6 == 0) ElementFx.arc(level, ball, ball.add(random.nextDouble() * 2 - 1, -ball.y + c.y + 0.05, random.nextDouble() * 2 - 1), BLUE, 0.04, 0, false, 3);
		if (t % 10 == 0) Feels.sound(level, ball, "storm_sizzle", 0.9F, 1.1F);
		// The sparking rod swallows it whole, and the Conductor with it.
		BlockPos rod = activeRod();
		if (level.getBlockState(rod).getBlock() instanceof LightningRodBlock) {
			Vec3 r = Vec3.atBottomCenterOf(rod);
			if (StormConductorRules.swallowed(ball.x - r.x, ball.z - r.z) && Math.abs(ball.y - r.y) < 3) {
				ElementFx.bolt(level, ball, rodTop(rod), 0.3, 2, 2);
				ElementFx.sparks(level, rodTop(rod), 16, 0.5);
				Feels.sound(level, rodTop(rod), "storm_clamp", 1.5F, 0.8F);
				ball = null;
				if (!ground(level)) moveLeft = 1;
				return;
			}
		}
		if (stopped) {
			if (t == tell + life) Feels.sound(level, ball, "tell_zap", 1.3F, 1.3F);
			if ((t - tell - life) % 3 == 0) ElementFx.groundRing(level, new Vec3(ball.x, c.y + 0.05, ball.z), WHITE, StormConductorRules.BALL_POP - 0.2, StormConductorRules.BALL_POP, 0.15, 4);
			if (t >= length - 1) burst(level);
			return;
		}
		for (LivingEntity e : prey(level)) {
			if (StormConductorRules.touches(ball.x - e.getX(), ball.y - e.getY(), ball.z - e.getZ())) { burst(level); moveLeft = 1; return; }
		}
	}

	/** Its ball lightning bursting where it is. */
	private void burst(ServerLevel level) {
		if (ball == null) return;
		ElementFx.stormImpact(level, ball, 1.2);
		Vfx.lightning(level, ball.add(0, -StormConductorRules.BALL_HEIGHT, 0));
		Feels.sound(level, ball, "storm_crack", 1.5F, 1.0F);
		for (LivingEntity e : prey(level))
			if (StormConductorRules.popped(ball.x - e.getX(), ball.y - e.getY(), ball.z - e.getZ())) strike(level, e, StormConductorRules.BALL_DAMAGE, struck);
		ball = null;
	}

	// ------------------------------------------------------------------ the chord

	/** Its baton pointed at one quiet rod, then the other, while lines are drawn between them and back to it; then lightning runs along them. */
	private void chord(ServerLevel level, int t) {
		int tell = StormConductorRules.CHORD_TELL, ticks = StormConductorRules.CHORD_TICKS, again = StormConductorRules.CHORD_AGAIN;
		if (lines.isEmpty()) { if (t == 0) moveLeft = 1; return; }
		Vec3 c = podium();
		if (t < tell) {
			Vec3 at = lines.get(0)[t < tell / 2 ? 0 : 1];
			turnTo(yawTo(at));
			if (t == 0) Feels.sound(level, c, "note_link", 1.3F, 0.8F);
			if (t == tell / 2) Feels.sound(level, c, "note_link", 1.3F, 1.2F);
			if (t == tell - 4) Feels.sound(level, c, "tell_zap", 1.2F, 1.1F);
			if (t % 3 == 0) drawLines(level, t >= tell - 4 ? WHITE : BLUE);
			if (t % 4 == 0) for (Vec3[] l : lines) Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, l[0].add(0, 1.0, 0), 3, 0.15, 0.02);
			return;
		}
		pulse(level, t - tell, ticks, struck);
		if (phase >= 3) {
			int k = t - tell - again;
			if (k >= 0) pulse(level, k, ticks, struck2);
			else if (k >= -(again - ticks) && k % 2 == 0) drawLines(level, k >= -3 ? WHITE : BLUE);
		}
	}

	private void drawLines(ServerLevel level, int color) {
		for (Vec3[] l : lines) ElementFx.ray(level, l[0].add(0, 0.1, 0), l[1].add(0, 0.1, 0), color, 0.07, 4);
	}

	/** One pulse of its chord, {@code k} ticks in: lightning running along every line, catching whoever stands on one, once. */
	private void pulse(ServerLevel level, int k, int ticks, Set<UUID> swing) {
		if (k < 0 || k >= ticks) return;
		if (k == 0) {
			beat();
			Feels.sound(level, podium(), "storm_zap", 1.5F, 0.9F);
			Feels.sound(level, podium(), "note_effect", 1.2F, 1.4F);
			for (Vec3[] l : lines) ElementFx.sparks(level, l[0].add(0, 1, 0), 10, 0.4);
		}
		if (k % 2 == 0) for (Vec3[] l : lines) ElementFx.arc(level, l[0].add(0, 0.6, 0), l[1].add(0, 0.6, 0), k == 0 ? WHITE : BOLT, 0.1, 2, false, 3);
		double floor = podium().y;
		for (LivingEntity e : prey(level)) for (Vec3[] l : lines) {
			double d = StormConductorRules.segment(e.getX(), e.getZ(), l[0].x, l[0].z, l[1].x, l[1].z);
			if (StormConductorRules.chorded(d, e.getY() - floor)) { strike(level, e, StormConductorRules.CHORD_DAMAGE, swing); break; }
		}
	}

	// ------------------------------------------------------------------ the rolling thunder

	/** Its baton drawn back and beating slow while the walls rumble; then rings of thunder roll in from the walls, one by one. Jump each. */
	private void roll(ServerLevel level, int t) {
		int tell = StormConductorRules.ROLL_TELL, gap = StormConductorRules.ROLL_GAP, n = StormConductorRules.waves(phase);
		double from = StormConductorRules.ROLL_FROM, speed = StormConductorRules.ROLL_SPEED;
		Vec3 c = podium();
		while (waves.size() < n) waves.add(new HashSet<>());
		if (t < tell) {
			if (t % 3 == 0) ElementFx.groundRing(level, c.add(0, 0.05, 0), t >= tell - 4 ? WHITE : BLUE, from, from - 0.4, 0.2, 4);
			if (t % 2 == 0) {
				double a = random.nextDouble() * Math.PI * 2;
				Motes.clouds(level, c.add(Math.cos(a) * from, 1.5 + random.nextDouble() * 2, Math.sin(a) * from), 1, 0.5, 0x3A4A66, 1.2, 14, Vec3.ZERO, 0.02, 0.6);
			}
			if (t % 5 == 0) beat();
			if (t == tell - 4) Feels.sound(level, c, "storm_roll", 1.4F, 0.7F);
			return;
		}
		List<LivingEntity> prey = prey(level);
		for (int w = 0; w < n; w++) {
			int k = t - tell - w * gap;
			if (k < 0) continue;
			double r0 = from - k * speed, r1 = r0 - speed;
			if (r1 < 1.0) continue;
			if (k == 0) {
				beat();
				ElementFx.groundRing(level, c.add(0, 0.05, 0), BOLT, from, 1.0, 0.4, StormConductorRules.rolls(from - 1.0, speed));
				ElementFx.groundRing(level, c.add(0, 0.1, 0), WHITE, from, 1.0, 0.15, StormConductorRules.rolls(from - 1.0, speed));
				Feels.sound(level, c, "storm_roll", 1.5F, 0.9F + w * 0.1F);
				Feels.sound(level, c, "storm_crack", 1.0F, 0.7F);
			}
			if (k % 2 == 0) for (int i = 0; i < 10; i++) {
				double a = i * Math.PI / 5 + k * 0.1;
				Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, c.add(Math.cos(a) * r1, 0.3, Math.sin(a) * r1), 1, 0.1, 0.02);
			}
			for (LivingEntity e : prey)
				if (StormConductorRules.crosses(flat(e.position(), c), r0, r1, e.getY() - c.y)) strike(level, e, StormConductorRules.ROLL_DAMAGE, waves.get(w));
		}
	}

	// ------------------------------------------------------------------ the counterpoint

	/**
	 * Its baton beating side to side while half the floor round it is marked out in squares; then lightning falls on every marked square,
	 * and straight away the other half is marked and struck. Stand on an unmarked square, then step across.
	 */
	private void counterpoint(ServerLevel level, int t) {
		int tell = StormConductorRules.COUNTER_TELL, again = StormConductorRules.COUNTER_AGAIN;
		if (t < tell) { tiles(level, t, tell, marks); return; }
		if (t == tell) { fallOn(level, marks, 0, struck); Feels.sound(level, podium(), "note_shape", 1.3F, 1.2F); }
		if (t < tell + again) tiles(level, t - tell, again, marks2);
		else if (t == tell + again) fallOn(level, marks2, 1, struck2);
	}

	/** Its squares marked out, brightening as the beat nears. */
	private void tiles(ServerLevel level, int k, int tell, List<Vec3> set) {
		if (k % 3 == 0) {
			beat();
			int color = k >= tell - 4 ? WHITE : BOLT;
			for (Vec3 at : set) ElementFx.flatSigil(level, at, SigilOption.CIRCLE, drawnBy(level, at) != null ? BLUE : color, 0.85, 4, 0.0);
			Feels.sound(level, podium(), "storm_tick", 0.9F, 1.0F + k * 0.04F);
		}
		if (k % 2 == 0) for (int i = 0; i < 4 && !set.isEmpty(); i++)
			Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, set.get(random.nextInt(set.size())).add(0, 0.15, 0), 1, 0.4, 0.0);
		if (k == tell - 4) Feels.sound(level, podium(), "tell_zap", 1.1F, 1.2F);
	}

	/** Lightning onto every square of half {@code half}, catching whoever stands on one (unless a rod draws it off them). */
	private void fallOn(ServerLevel level, List<Vec3> set, int half, Set<UUID> swing) {
		beat();
		Vec3 c = podium();
		for (Vec3 at : set) {
			ElementFx.flatSigil(level, at, SigilOption.GLOW, WHITE, 0.95, 6, 0.0);
			Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, at.add(0, 0.2, 0), 3, 0.5, 0.05);
		}
		for (int i = 0; i < 6 && !set.isEmpty(); i++) {
			Vec3 at = set.get(random.nextInt(set.size()));
			ElementFx.bolt(level, at.add(0, 8, 0), at.add(0, 0.1, 0), 0.25, 1, 1);
		}
		Vfx.thunderclap(level, c, 8, false);
		Feels.sound(level, c, "storm_crack", 1.5F, 0.9F + half * 0.2F);
		for (LivingEntity e : prey(level)) {
			double dx = e.getX() - c.x, dz = e.getZ() - c.z;
			int i = StormConductorRules.tile(dx), j = StormConductorRules.tile(dz);
			Vec3 square = new Vec3(c.x + StormConductorRules.centre(i), c.y, c.z + StormConductorRules.centre(j));
			if (!set.contains(square) || !StormConductorRules.counter(dx, dz, half, e.getY() - c.y)) continue;
			BlockPos rod = drawnBy(level, e.position());
			if (rod != null) { ElementFx.bolt(level, rodTop(rod).add(0, 8, 0), rodTop(rod), 0.25, 1, 1); continue; }
			ElementFx.bolt(level, e.position().add(0, 8, 0), e.position(), 0.3, 2, 1);
			strike(level, e, StormConductorRules.COUNTER_DAMAGE, swing);
		}
	}

	// ------------------------------------------------------------------ its openings

	/** Winded or spent: drooping on its podium, fizzling. */
	private void slumped(ServerLevel level, int move, int t) {
		Vec3 core = position().add(0, move == StormConductorRules.WINDED ? 0.9 : 1.3, 0);
		if (t % 3 == 0) Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, core, 2, 0.5, 0.05);
		if (t % 6 == 0) Motes.smoke(level, core.add(0, 0.4, 0), 3, 0.4);
		if (t % 12 == 0) Feels.sound(level, core, move == StormConductorRules.SPENT ? "storm_fizz" : "storm_sizzle", 0.9F, 0.6F);
	}

	// ------------------------------------------------------------------ its arena

	/** Its podium's foot: the floor its prey stand on, under the middle of it. */
	private Vec3 podium() { return Vec3.atBottomCenterOf(home); }

	/** Everyone in its arena it may harm. */
	private List<LivingEntity> prey(ServerLevel level) {
		if (home == null) return List.of();
		Vec3 c = podium();
		AABB arena = new AABB(c, c).inflate(ARENA, 0, ARENA).expandTowards(0, 8, 0).move(0, -2, 0);
		List<LivingEntity> out = new ArrayList<>();
		for (Entity e : level.getEntities(this, arena, e -> e instanceof LivingEntity && e.isAlive() && Targets.canHarm(this, e))) out.add((LivingEntity) e);
		return out;
	}

	/** The one of its prey nearest {@code at}, if any. */
	private LivingEntity nearest(ServerLevel level, Vec3 at) {
		LivingEntity best = null;
		for (LivingEntity e : prey(level)) if (best == null || e.distanceToSqr(at) < best.distanceToSqr(at)) best = e;
		return best;
	}

	/** A blow of {@code base} (scaled as its spells are) landing on {@code t}, caught for this swing in {@code swing}: once each. */
	private boolean strike(ServerLevel level, LivingEntity t, double base, Set<UUID> swing) {
		if (!swing.add(t.getUUID())) return false;
		return t.hurtServer(level, level.damageSources().mobAttack(this), (float) (base * power()));
	}

	/** {@code t} thrown {@code strength} along the floor the way of {@code dir}, and {@code up} into the air. */
	private static void push(LivingEntity t, Vec3 dir, double strength, double up) {
		Vec3 flat = new Vec3(dir.x, 0, dir.z);
		flat = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(strength);
		Effects.push(t, flat.add(0, up, 0));
	}

	/** Flat along {@code yaw}. */
	private static Vec3 ahead(float yaw) {
		double r = Math.toRadians(yaw);
		return new Vec3(-Math.sin(r), 0, Math.cos(r));
	}

	private float yawTo(Vec3 at) { return (float) Math.toDegrees(Math.atan2(-(at.x - getX()), at.z - getZ())); }

	private void turnTo(float yaw) { setYRot(yaw); yBodyRot = yaw; yHeadRot = yaw; }

	/** Its aim follows {@code target} until {@code lock} ticks before the blow at {@code tell}, then holds. */
	private void track(int t, int tell, int lock, LivingEntity target) {
		if (target != null && t <= tell - lock) aim = yawTo(target.position());
		turnTo(aim);
	}

	/** How far apart {@code a} and {@code b} lie across the floor. */
	private static double flat(Vec3 a, Vec3 b) { double dx = a.x - b.x, dz = a.z - b.z; return Math.sqrt(dx * dx + dz * dz); }

	/** The tip of its baton, raised over its cage. */
	private Vec3 tip() { return position().add(0, 2.4, 0).add(ahead(getYRot()).scale(0.3)); }

	/** The top of a rod, where its lightning lands. */
	private static Vec3 rodTop(BlockPos rod) { return Vec3.atBottomCenterOf(rod).add(0, 1, 0); }

	/** Its rods still standing. */
	private List<BlockPos> standing() {
		activeRod();
		List<BlockPos> out = new ArrayList<>();
		if (rods != null) for (BlockPos p : rods) if (level().getBlockState(p).getBlock() instanceof LightningRodBlock) out.add(p);
		return out;
	}

	/** Its rods still standing that are not sparking. */
	private List<BlockPos> quiet() {
		BlockPos on = activeRod();
		List<BlockPos> out = standing();
		out.remove(on);
		return out;
	}

	/** The rod (if any) that draws down a bolt meant for {@code at}. */
	private BlockPos drawnBy(ServerLevel level, Vec3 at) {
		for (BlockPos p : standing()) {
			Vec3 r = Vec3.atBottomCenterOf(p);
			if (StormConductorRules.drawn(at.x - r.x, at.z - r.z) && Math.abs(at.y - r.y) < 3) return p;
		}
		return null;
	}

	// ------------------------------------------------------------------ its rods

	public BlockPos activeRod() {
		if(home()==null) return blockPosition();
		if(rods==null || rods.isEmpty()&&level().getGameTime()%100==0 || rods!=null&&rods.stream().anyMatch(p->level().hasChunkAt(p)&&!(level().getBlockState(p).getBlock() instanceof LightningRodBlock))) {
			var found=new java.util.ArrayList<BlockPos>();
			for(BlockPos p:BlockPos.betweenClosed(home().offset(-6,0,-6),home().offset(6,0,6)))
				if(level().hasChunkAt(p) && level().getBlockState(p).getBlock() instanceof LightningRodBlock) found.add(p.immutable());
			found.sort(java.util.Comparator.<BlockPos>comparingInt(p -> p.getX()).thenComparingInt(p -> p.getZ()));
			rods=List.copyOf(found);
		}
		return rods.isEmpty()?home():rods.get(rodIndex%rods.size());
	}

	@Override protected void mechanic(ServerLevel level, long now) {
		boolean open = now < groundedUntil || StormConductorRules.opening(move);
		setState(EXPOSED, open); setState(GUARDED, !open);
		if (now < nextGround || home() == null || now % 10 != 0) return;
		BlockPos rod = activeRod();
		// The worldgen rods and manually placed rods obey exactly the same encounter rule.
		if (!(level.getBlockState(rod).getBlock() instanceof LightningRodBlock)) return;
		Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, Vec3.atCenterOf(rod).add(0, .8, 0), 6, .2, .01);
		for (var player : level.players()) if (player.isAlive() && !player.isSpectator() && player.position().distanceToSqr(Vec3.atCenterOf(rod)) < 4) { ground(level); break; }
	}

	public boolean ground(ServerLevel level) {
		long now = level.getGameTime();
		if (!isAlive() || state(SHIFTING) || now < nextGround) return false;
		groundedUntil = now + 100; nextGround = now + 160; rodIndex = (rodIndex + 1) % 3;
		interrupt(); readyAt = now + 40; setState(EXPOSED, true); setState(GUARDED, false);
		Sigils.ground(level, position(), color(), accent(), 3, 40);
		announce(level, "message.wildercord.storm_grounded", accent()); return true;
	}

	/** Charged, half of every blow turns aside; grounded, winded or spent, none does. */
	@Override protected float resist(ServerLevel level, DamageSource source, float damage) { return level.getGameTime() < groundedUntil || StormConductorRules.opening(move) ? damage : damage * .5F; }

	@Override protected void onPhase(ServerLevel level, int phase) {
		groundedUntil = 0; nextGround = 0; rodIndex = phase % 3;
		entityData.set(DATA_BEAT, 0.0F);
		Fx.sound(level, position(), DungeonSounds.BOSS_PHASE, 2.0F, 1.0F);
		announce(level, "message.wildercord.storm_conductor_phase", COLOR);
	}

	/** Its baton raised straight up while the storm gathers over it, lightning falling onto the tip, until it all comes down at once. */
	@Override protected void shiftTick(ServerLevel level, int left) {
		Vec3 top = position().add(0, 3.6, 0);
		double a = left * 0.4;
		for (int i = 0; i < 3; i++) {
			double b = a + Math.PI * 2 * i / 3, h = (left % 20) / 20.0 * 3.5;
			Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, position().add(Math.cos(b) * 1.8, h, Math.sin(b) * 1.8), 1, 0.05, 0.01);
		}
		Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, top, 3, 0.2, 0.05);
		if (left % 4 == 0) Motes.clouds(level, position().add(0, 7, 0), 3, 3.0, 0x3A4A66, 2.0, 30, new Vec3(0, -0.01, 0), 0.03, 0.8);
		if (left % 10 == 0 && home != null) {
			Vec3 floor = podium();
			ElementFx.flatSigil(level, floor, SigilOption.STAR, ACCENT, 5.0, 12, 0.1);
			ElementFx.groundRing(level, floor, COLOR, 1.0, 7.0, 0.3, 12);
			ElementFx.bolt(level, top.add(0, 10, 0), top, 0.3, 2, 2);
			Feels.sound(level, top, "storm_crack", 1.4F, 0.8F + (60 - left) * 0.01F);
		}
		if (left == 0) {
			ElementFx.stormImpact(level, position().add(0, 1.2, 0), 1.8);
			Feels.sound(level, position(), "storm_boom", 2.0F, 0.8F);
		}
	}

	@Override protected Component status() {
		String key = move > 0 && move < SPELL ? StormConductorRules.key(move) : null;
		if (key != null) return Component.translatable("boss.wildercord.storm_conductor.move." + key).withColor(StormConductorRules.opening(move) ? ACCENT : COLOR);
		return Component.translatable(level().getGameTime() < groundedUntil ? "boss.wildercord.storm_grounded" : "boss.wildercord.storm_charged");
	}

	@Override protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); out.putInt("rod_index",rodIndex); out.putLong("grounded_until",groundedUntil); out.putLong("next_ground",nextGround); }
	@Override protected void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); rodIndex=Math.floorMod(in.getIntOr("rod_index",0),3); groundedUntil=in.getLongOr("grounded_until",0); nextGround=in.getLongOr("next_ground",0); }
}
