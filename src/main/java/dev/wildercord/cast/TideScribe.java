package dev.wildercord.cast;

import dev.wildercord.content.SigilOption;
import dev.wildercord.content.dungeons.DungeonAltarBlock;
import dev.wildercord.content.dungeons.DungeonBlocks;
import dev.wildercord.content.dungeons.DungeonSounds;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * The Tide Scribe, keeper of the Drowned Scriptorium: a drowned sorcerer trailing ink, who floods its
 * arena and drains it again, over and over. Water conducts: a storm spell that lands in the flood
 * shocks everything standing in it, the Scribe included (hard, and it staggers), and whoever cast it
 * too if they're wading. Frost freezes the water where it lands into ice to stand on, and ice closing
 * round the Scribe strands it, open to harm. While the arena is flooded and it swims, the water wraps
 * it (half damage from anything but a shock); while it's dry, it's only a sorcerer. Its own storms
 * find you in the water just the same, so use the tide: get out of it, freeze it, or shock it.
 *
 * <p>Once it breaks free of the ice it can't be stranded again for a few seconds. The flood comes in
 * and goes out a ring at a time over a few ticks, quietly (no block updates but at its rim, so the
 * water never flows on its own), and every ebb clears all the ice and water from its pit, whoever
 * froze it and whenever (the ice isn't remembered across a restart, so it's found, not recalled).</p>
 */
public class TideScribe extends DungeonBoss {
	private static final int COLOR = 0x3A8CFF;
	private static final int ACCENT = 0x9AF0E8;
	/** The pit that floods: everything within this many blocks of the altar, two blocks deep. */
	public static final double PIT_RADIUS = 12.0;
	private static final int FILL_TICKS = 40;
	private static final int DRAIN_TICKS = 30;
	private static final int[] LOW_TICKS = {0, 240, 200, 160};
	private static final int[] HIGH_TICKS = {0, 300, 260, 220};
	private static final int STRAND_TICKS = 70;

	private static final List<List<RuneDef>> PHASE_1 = List.of(
		List.of(Runes.BOLT, Runes.SHOCK),
		List.of(Runes.WAVE, Runes.FROST),
		List.of(Runes.BOLT, Runes.BUBBLE));
	private static final List<List<RuneDef>> PHASE_2 = List.of(
		List.of(Runes.RAIN, Runes.SHOCK),
		List.of(Runes.RING, Runes.COLDSNAP),
		List.of(Runes.ORB, Runes.JOLT),
		List.of(Runes.BOLT, Runes.BUBBLE, Runes.DELAY, Runes.BOLT, Runes.SHOCK));
	private static final List<List<RuneDef>> PHASE_3 = List.of(
		List.of(Runes.DOMAIN, Runes.SHOCK),
		List.of(Runes.BOLT, Runes.LIGHTNING),
		List.of(Runes.WAVE, Runes.FREEZE),
		List.of(Runes.RAIN, Runes.JOLT, Runes.WIDEN));

	/** Every awake Scribe, so a spell anywhere can ask quickly whether it landed in a flooded arena. */
	private static final Set<TideScribe> AWAKE = Collections.newSetFromMap(new WeakHashMap<>());

	private enum Tide { LOW, RISING, HIGH, EBBING }

	private Tide tide = Tide.LOW;
	private long tideUntil;
	private int step;
	private List<BlockPos> cells;
	/** The pit's cells by layer (0, 1) and ring (0 at the core, out to {@link #PIT_RADIUS}). */
	private List<BlockPos>[][] rings;
	private final Set<BlockPos> iced = new HashSet<>();
	private long strandedUntil;
	/** Until when frost can't strand it again (it has just broken free). */
	private long strandImmuneUntil;
	private boolean shocking;
	private int conductions;
	/** Casts that already conducted or froze here, so a spell striking again and again does it once a second. */
	private final Map<Object, Long> handled = new HashMap<>();
	private final Map<UUID, Long> told = new HashMap<>();
	private double swim;

	/** How far under the water it has sunk (a dive), 0 to 1, for its pose. */
	private static final EntityDataAccessor<Float> DATA_DEPTH = SynchedEntityData.defineId(TideScribe.class, EntityDataSerializers.FLOAT);
	/** Which go of a move it is on (0 the first, 1 the second), so the second comes off the other hand. */
	private static final EntityDataAccessor<Float> DATA_GO = SynchedEntityData.defineId(TideScribe.class, EntityDataSerializers.FLOAT);
	private float depthPose, depthPoseO, goPose, goPoseO;
	/** When it last made each of its own moves, by id, so it waits a while before the same again. */
	private final long[] made = new long[TideScribeRules.MOVES + 1];
	/** Whoever this swing (and a second ring behind it) has already caught: once each. */
	private final Set<UUID> struck = new HashSet<>();
	private final Set<UUID> struck2 = new HashSet<>();
	/** Its aim for the move under way (locked a few ticks before the blow), where it began it, and where it means to be. */
	private float aim;
	private Vec3 from;
	private Vec3 goal;
	/** The words it wrote on the floor (and the second wave), and the spot its dive is hunting. */
	private final List<Vec3> glyphs = new ArrayList<>();
	private final List<Vec3> glyphs2 = new ArrayList<>();
	private Vec3 spot;
	/** Which go of the move it is on, and whether this move already follows straight on from another. */
	private int again;
	private boolean chained;

	public TideScribe(EntityType<? extends TideScribe> type, Level level) {
		super(type, level, BossEvent.BossBarColor.BLUE);
		setNoGravity(true);
		Arrays.fill(made, Long.MIN_VALUE / 2);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 340.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.FOLLOW_RANGE, 40.0)
			.add(Attributes.ARMOR, 6.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	/** Wakes the Scribe over its core: the water in the conduit stirs, and it rises out of a pool of ink. */
	public static TideScribe rise(ServerLevel level, BlockPos altar) {
		TideScribe boss = place(level, DungeonEntities.TIDE_SCRIBE, altar, 1.2);
		if (boss == null) {
			return null;
		}
		Vec3 at = Vec3.atBottomCenterOf(altar).add(0, 1.2, 0);
		Sigils.ground(level, Vec3.atBottomCenterOf(altar).add(0, 0.05, 0), COLOR, ACCENT, 5.0F, 80);
		Vfx.emit(level, ParticleTypes.SQUID_INK, at, 40, 0.8, 0.05);
		Vfx.emit(level, ParticleTypes.BUBBLE_COLUMN_UP, at, 30, 1.2, 0.2);
		Vfx.radial(level, ParticleTypes.NAUTILUS, at.add(0, 1, 0), 40, 0.5);
		Fx.sound(level, at, DungeonSounds.BOSS_RISE, 2.0F, 1.0F);
		Fx.sound(level, at, SoundEvents.ELDER_GUARDIAN_CURSE, 0.5F, 1.4F);
		boss.tideUntil = level.getGameTime() + 100;
		boss.announce(level, "message.wildercord.tide_scribe_wakes", COLOR);
		return boss;
	}

	@Override
	protected List<List<RuneDef>> spells(int phase) {
		return phase == 1 ? PHASE_1 : phase == 2 ? PHASE_2 : PHASE_3;
	}

	@Override
	protected String feat() {
		return Feats.TIDE_SCRIBE;
	}

	@Override
	protected int color() {
		return COLOR;
	}

	@Override
	protected int accent() {
		return ACCENT;
	}

	@Override
	protected double leash() {
		return 16.0;
	}

	// ------------------------------------------------------------------ the tide

	/** Whether the arena holds water now (all of the way up, or most of it). */
	public boolean flooded() {
		return tide == Tide.HIGH || tide == Tide.RISING && step > FILL_TICKS / 2 || tide == Tide.EBBING && step < DRAIN_TICKS / 2;
	}

	/** Whether this Scribe's arena really is an arena: it floods only over its own core, never the open world. */
	private boolean hasArena(ServerLevel level) {
		BlockState altar = level.getBlockState(home);
		return altar.is(DungeonBlocks.ALTAR) && altar.getValue(DungeonAltarBlock.KIND) == DungeonAltarBlock.Kind.TIDE;
	}

	/** How many rings each layer of the pit pours (or drains) in, one a tick. */
	private static final int RINGS = (int) Math.floor(PIT_RADIUS) + 1;
	/** Block changes in the pit: seen by players, but no neighbour updates, so the water never flows or spreads. */
	private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SKIP_ON_PLACE;

	/** The cells that flood: two layers over the pit floor, within {@link #PIT_RADIUS} of the core. */
	private List<BlockPos> cells(ServerLevel level) {
		if (cells == null) {
			cells = new ArrayList<>();
			@SuppressWarnings("unchecked")
			List<BlockPos>[][] byRing = new List[2][RINGS];
			for (int layer = 0; layer < 2; layer++) {
				for (int ring = 0; ring < RINGS; ring++) {
					byRing[layer][ring] = new ArrayList<>();
				}
			}
			int r = (int) Math.ceil(PIT_RADIUS);
			for (int dy = 0; dy <= 1; dy++) {
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						if (dx * dx + dz * dz <= PIT_RADIUS * PIT_RADIUS) {
							BlockPos pos = home.offset(dx, dy, dz);
							cells.add(pos);
							byRing[dy][Math.min(RINGS - 1, (int) Math.floor(Math.sqrt(dx * dx + dz * dz)))].add(pos);
						}
					}
				}
			}
			rings = byRing;
		}
		return cells;
	}

	private void tickTide(ServerLevel level, long now) {
		if (!hasArena(level)) {
			return;
		}
		switch (tide) {
			case LOW -> {
				if (now >= tideUntil) {
					startRising(level);
				}
			}
			case RISING -> {
				fillStep(level, step++, FILL_TICKS, true);
				if (step >= FILL_TICKS) {
					tide = Tide.HIGH;
					tideUntil = now + HIGH_TICKS[phase];
				}
			}
			case HIGH -> {
				if (now % 5 == 0) {
					Vfx.emit(level, ParticleTypes.BUBBLE_POP, Vec3.atBottomCenterOf(home).add(0, 2, 0), 6, PIT_RADIUS * 0.5, 0.02);
				}
				if (now >= tideUntil) {
					tide = Tide.EBBING;
					step = 0;
					Fx.sound(level, Vec3.atCenterOf(home), DungeonSounds.TIDE_EBB, 2.0F, 1.0F);
					updateName(null);
				}
			}
			case EBBING -> {
				fillStep(level, step++, DRAIN_TICKS, false);
				if (step >= DRAIN_TICKS) {
					tide = Tide.LOW;
					tideUntil = now + LOW_TICKS[phase];
					iced.clear();
				}
			}
		}
	}

	private void startRising(ServerLevel level) {
		tide = Tide.RISING;
		step = 0;
		Vec3 c = Vec3.atCenterOf(home);
		Fx.sound(level, c, DungeonSounds.TIDE_RISE, 2.0F, 1.0F);
		for (ServerPlayer player : level.players()) {
			if (player.distanceTo(this) < 40) {
				player.sendOverlayMessage(Component.translatable("message.wildercord.tide_rises").withColor(ACCENT));
			}
		}
		updateName(null);
	}

	/**
	 * One tick of filling (or draining) the pit: a ring of one layer. The low layer rises first, from
	 * the core outward, then the high one; it ebbs from the top, from the rim inward. The blocks change
	 * quietly (see {@link #QUIET}), so water never flows into the gaps between rings (water between two
	 * sources would become a source itself, and the pit would never drain); once the last ring is
	 * done, the blocks round the pit's rim are told.
	 */
	private void fillStep(ServerLevel level, int index, int of, boolean fill) {
		cells(level);
		int half = of / 2;
		boolean second = index >= half;
		int layer = fill ? (second ? 1 : 0) : (second ? 0 : 1);
		int ring = second ? index - half : index;
		if (ring >= RINGS) {
			if (fill && level.getRandom().nextInt(3) == 0) {
				Vfx.emit(level, ParticleTypes.SPLASH, Vec3.atBottomCenterOf(home).add(0, 1.2 + layer, 0), 8, PIT_RADIUS * 0.5, 0.05);
			}
			if (second && ring == RINGS) {
				rimUpdates(level);
			}
			return;
		}
		// Rising, from the core out; ebbing, from the rim in.
		for (BlockPos pos : rings[layer][fill ? ring : RINGS - 1 - ring]) {
			setCell(level, pos, fill);
		}
	}

	/** The blocks round the pit's rim hear the flood came (or went), all at once, once the whole pit is done. */
	private void rimUpdates(ServerLevel level) {
		for (int layer = 0; layer < 2; layer++) {
			for (BlockPos pos : rings[layer][RINGS - 1]) {
				level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
			}
		}
	}

	private void setCell(ServerLevel level, BlockPos pos, boolean fill) {
		BlockState state = level.getBlockState(pos);
		if (fill) {
			// Only into open air, over something solid (or over water it just poured).
			BlockState below = level.getBlockState(pos.below());
			boolean open = state.isAir() || state.is(Blocks.WATER) && !state.getFluidState().isSource();
			if (open && (below.isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP) || below.getFluidState().is(FluidTags.WATER))) {
				level.setBlock(pos, Blocks.WATER.defaultBlockState(), QUIET);
				if (level.getRandom().nextInt(8) == 0) {
					Vfx.emit(level, ParticleTypes.SPLASH, Vec3.atCenterOf(pos).add(0, 0.5, 0), 3, 0.3, 0.05);
				}
			}
		} else if (state.getFluidState().is(FluidTags.WATER) && !state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)
				|| state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
			// All the water and ice in the pit goes, whoever froze it and whenever (even before a restart).
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET);
		}
	}

	/** For the tests and for its death: floods (or drains) the whole pit at once. */
	public void forceTide(boolean high) {
		if (!(level() instanceof ServerLevel level) || !hasArena(level)) {
			return;
		}
		cells(level);
		for (int layer = 0; layer < 2; layer++) {
			for (List<BlockPos> ring : rings[high ? layer : 1 - layer]) {
				for (BlockPos pos : ring) {
					setCell(level, pos, high);
				}
			}
		}
		rimUpdates(level);
		tide = high ? Tide.HIGH : Tide.LOW;
		step = high ? FILL_TICKS : DRAIN_TICKS;
		tideUntil = level.getGameTime() + (high ? HIGH_TICKS[phase] : LOW_TICKS[phase]);
		if (!high) {
			iced.clear();
		}
	}

	/** How many storm spells the water has carried here, and how much of it is ice (for the tests). */
	public int conductions() {
		return conductions;
	}

	public int icedCells() {
		return iced.size();
	}

	public boolean stranded() {
		return level().getGameTime() < strandedUntil;
	}

	// ------------------------------------------------------------------ spells landing in the water

	static boolean anyAwake() {
		return !AWAKE.isEmpty();
	}

	static void clearArenas() {
		AWAKE.clear();
	}

	/** A storm or frost effect landed somewhere: if it's in a flooded arena, the water carries it or freezes. */
	static void onSpell(Cast cast, Cast.Hit hit, String element) {
		if (cast.guardedImpact() && AWAKE.size() > Cast.MAX_ENTITIES) return;
		for (TideScribe scribe : new ArrayList<>(AWAKE)) {
			if (cast.guardedImpact() && !cast.alive()) return;
			if (scribe.isAlive() && scribe.level() == cast.level && scribe.home != null && scribe.flooded() && scribe.inPit(cast.level, hit)) {
				if (element.equals("storm")) {
					scribe.conduct(cast, hit.point());
				} else {
					scribe.freeze(cast, hit.point());
				}
			}
		}
	}

	/** Whether a hit landed in this arena's water: the point is in (or just over) the flood, or it struck something wading in it. */
	private boolean inPit(ServerLevel level, Cast.Hit hit) {
		Vec3 c = Vec3.atBottomCenterOf(home);
		Vec3 p = hit.point();
		double dx = p.x - c.x;
		double dz = p.z - c.z;
		if (dx * dx + dz * dz > (PIT_RADIUS + 0.5) * (PIT_RADIUS + 0.5) || p.y < home.getY() - 1 || p.y > home.getY() + 4) {
			return false;
		}
		BlockPos at = BlockPos.containing(p);
		if (level.getFluidState(at).is(FluidTags.WATER) || level.getFluidState(at.below()).is(FluidTags.WATER)) {
			return true;
		}
		for (Entity e : hit.entities()) {
			if (e.isInWater()) {
				return true;
			}
		}
		return false;
	}

	/** Once a second at most for each cast (a Zone pulsing in the water conducts each pulse, not each tick). */
	private boolean fresh(Cast cast, String what) {
		long now = level().getGameTime();
		handled.values().removeIf(until -> until < now);
		Object key = java.util.List.of(cast.identity(), what);
		if (handled.containsKey(key)) {
			return false;
		}
		handled.put(key, now + 20);
		return true;
	}

	/** Storm in the water: it runs through everything wading in the arena. */
	private void conduct(Cast cast, Vec3 at) {
		if (!fresh(cast, "storm") || !(level() instanceof ServerLevel level)) {
			return;
		}
		conductions++;
		double shock = 5 * cast.power;
		DamageSource source = level.damageSources().source(DamageTypes.LIGHTNING_BOLT, cast.caster);
		AABB arena = new AABB(Vec3.atBottomCenterOf(home), Vec3.atBottomCenterOf(home)).inflate(PIT_RADIUS + 0.5, 0, PIT_RADIUS + 0.5)
			.expandTowards(0, 3, 0).move(0, -0.5, 0);
		List<LivingEntity> wading = cast.guardedImpact() ? RelayCircles.collateral(cast, at, arena, e -> e.isInWater() && !e.isSpectator())
			: level.getEntitiesOfClass(LivingEntity.class, arena, e -> e.isAlive() && e.isInWater() && !e.isSpectator());
		for (LivingEntity t : wading) {
			if (!cast.admits(t) || cast.guardedImpact() && cast.takeEntities(1) < 1) continue;
			Vec3 tc = t.getBoundingBox().getCenter();
			ElementFx.bolt(level, at, tc, 0.04, 1, 2);
			if (t == this) {
				if (cast.caster == this) {
					continue;
				}
				// The Scribe is the water's own: the shock runs straight through it, and it reels.
				shocking = true;
				try {
					RelayCircles.from(cast, at, () -> Effects.hurt(cast, this, source, shock * 5));
				} finally {
					shocking = false;
				}
				if (!cast.admits(this)) continue;
				strandedUntil = Math.max(strandedUntil, level.getGameTime() + 40);
				interrupt();
				continue;
			}
			// Whoever cast it is shocked too if they're wading, but never their allies, pets or bystanders.
			if (t != cast.caster && !Targets.canHarm(cast.caster, t)) {
				continue;
			}
			RelayCircles.from(cast, at, () -> Effects.hurt(cast, t, source, shock));
		}
		// Sparks skitter across the whole surface.
		Vec3 c = Vec3.atBottomCenterOf(home).add(0, 2.0, 0);
		for (int i = 0; i < 24; i++) {
			double a = level.getRandom().nextDouble() * Math.PI * 2;
			double r = Math.sqrt(level.getRandom().nextDouble()) * PIT_RADIUS;
			Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, c.add(Math.cos(a) * r, 0, Math.sin(a) * r), 2, 0.2, 0.1);
		}
		Sigils.flash(level, at, ElementFx.STORM.secondary(), 3.0F);
		ElementFx.groundRing(level, c.subtract(0, 0.1, 0), ElementFx.STORM.primary(), 0.5, PIT_RADIUS, 0.2, 10);
		Fx.sound(level, at, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.2F, 1.4F);
		Fx.sound(level, at, dev.wildercord.content.WildercordSounds.impact("storm"), 1.4F, 1.0F);
		if (cast.caster instanceof ServerPlayer player) {
			long now = level.getGameTime();
			Long last = told.get(player.getUUID());
			if (last == null || now - last > 200) {
				told.put(player.getUUID(), now);
				player.sendOverlayMessage(Component.translatable("message.wildercord.tide_conducts").withColor(0xFFE650));
			}
		}
	}

	/** Frost in the water: the top of the flood freezes round where it landed, into ice to stand on. */
	private void freeze(Cast cast, Vec3 at) {
		if (!fresh(cast, "frost") || !(level() instanceof ServerLevel level)) {
			return;
		}
		int top = home.getY() + 1;
		int count = 0;
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				if (dx * dx + dz * dz > 7) {
					continue;
				}
				BlockPos pos = new BlockPos((int) Math.floor(at.x) + dx, top, (int) Math.floor(at.z) + dz);
				BlockState state = level.getBlockState(pos);
				if (state.is(Blocks.WATER) && pos.distSqr(home) <= (PIT_RADIUS + 1) * (PIT_RADIUS + 1)
					&& (!cast.guardedImpact() || cast.admitsBlock(pos) && cast.takeBlock())) {
					level.setBlock(pos, Blocks.ICE.defaultBlockState(), QUIET);
					iced.add(pos.immutable());
					count++;
				}
			}
		}
		if (count == 0) {
			return;
		}
		ElementFx.frostCreep(level, new Vec3(at.x, top + 1.0, at.z), 3.0, 30);
		Vfx.emit(level, ParticleTypes.SNOWFLAKE, new Vec3(at.x, top + 1.0, at.z), 20, 1.5, 0.02);
		Fx.sound(level, at, SoundEvents.GLASS_PLACE, 1.0F, 0.6F);
		// Ice closing round the Scribe strands it (unless it has only just broken free).
		long now = level.getGameTime();
		if (isInWater() && new Vec3(getX() - at.x, 0, getZ() - at.z).length() < 3.5 && now >= strandImmuneUntil
			&& (!cast.guardedImpact() || RelayCircles.admitsFrom(cast, at, this) && cast.takeEntities(1) > 0)) {
			strandedUntil = now + STRAND_TICKS;
			strandImmuneUntil = strandedUntil + BossRules.STRAND_IMMUNE_TICKS;
			interrupt();
			ElementFx.frostImpact(level, getBoundingBox().getCenter(), 1.5);
			announce(level, "message.wildercord.tide_scribe_stranded", ACCENT);
		}
	}

	// ------------------------------------------------------------------ its own moves

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_DEPTH, 0.0F);
		builder.define(DATA_GO, 0.0F);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			depthPoseO = depthPose;
			goPoseO = goPose;
			depthPose = Mth.approach(depthPose, entityData.get(DATA_DEPTH), 0.2F);
			goPose = Mth.approach(goPose, entityData.get(DATA_GO), 0.34F);
		}
	}

	/** 0: how far under the water it has sunk; 1: which go of a move it is on (the second off the other hand). */
	@Override
	public float extra(int i, float partial) {
		return switch (i) {
			case 0 -> Mth.lerp(partial, depthPoseO, depthPose);
			case 1 -> Mth.lerp(partial, goPoseO, goPose);
			default -> 0;
		};
	}

	@Override
	protected int ownMoves() {
		return TideScribeRules.MOVES;
	}

	@Override
	protected void weigh(ServerLevel level, LivingEntity target, double[] weights) {
		if (stranded() || home == null) {
			return;
		}
		long now = level.getGameTime();
		long[] since = new long[TideScribeRules.MOVES + 1];
		for (int m = 1; m <= TideScribeRules.MOVES; m++) {
			since[m] = now - made[m];
		}
		boolean behind = TideScribeRules.behind(yawTo(target.position()) - getYRot());
		boolean tideOut = hasArena(level) && tide == Tide.LOW && tideUntil - now > TideScribeRules.CALL_LEAST;
		double[] w = TideScribeRules.weights(phase, flooded(), tideOut, distanceTo(target), behind, target.getY() - home.getY(),
			target.isInWater(), since);
		for (int i = 0; i < TideScribeRules.PICKS.length; i++) {
			weights[TideScribeRules.PICKS[i]] = w[i];
		}
	}

	@Override
	protected double spellWeight(ServerLevel level, LivingEntity target, int index) {
		List<RuneDef> spell = spells(phase).get(index);
		boolean storm = spell.contains(Runes.SHOCK) || spell.contains(Runes.JOLT) || spell.contains(Runes.LIGHTNING);
		boolean frost = spell.contains(Runes.FROST) || spell.contains(Runes.COLDSNAP) || spell.contains(Runes.FREEZE);
		return TideScribeRules.spellWeight(storm, frost, flooded(), target.isInWater());
	}

	@Override
	protected void beginMove(ServerLevel level, int move, LivingEntity target) {
		chained = false;
		start(level, move, target);
	}

	/** Into {@code move} at {@code target} from its start: its aim taken, nothing yet caught, and its tell under way. */
	private void start(ServerLevel level, int move, LivingEntity target) {
		aim = yawTo(target.position());
		turnTo(aim);
		struck.clear();
		struck2.clear();
		glyphs.clear();
		glyphs2.clear();
		spot = null;
		again = 0;
		made[move] = level.getGameTime();
		from = position();
		goal = position();
		entityData.set(DATA_GO, 0.0F);
		pose(move, TideScribeRules.tell(move), TideScribeRules.length(move, phase));
	}

	/** Into the second go of the move under way: a fresh tell of {@code tell} ticks, {@code length} in all, off the other hand. */
	private void secondGo(int move, int tell, int length) {
		again = 1;
		struck.clear();
		struck2.clear();
		entityData.set(DATA_GO, 1.0F);
		pose(move, tell, length);
	}

	@Override
	protected void moveDone(ServerLevel level, int move, LivingEntity target) {
		entityData.set(DATA_GO, 0.0F);
		glyphs.clear();
		glyphs2.clear();
		switch (move) {
			case TideScribeRules.SCRIPT -> pose(TideScribeRules.BOWED, 0, TideScribeRules.length(TideScribeRules.BOWED, phase));
			case TideScribeRules.WHIRLPOOL -> {
				pose(TideScribeRules.SPUN, 0, TideScribeRules.length(TideScribeRules.SPUN, phase));
				Fx.sound(level, position(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.2F, 0.7F);
			}
			case TideScribeRules.DIVE -> {
				if (phase >= 3 && again == 0 && target != null && flooded()) {
					// Once more under, before it surfaces for good.
					spot = null;
					int tell = TideScribeRules.DIVE_TELL - 2;
					secondGo(TideScribeRules.DIVE, tell, tell + TideScribeRules.DIVE_HUNT + TideScribeRules.DIVE_BURST);
					Fx.sound(level, position(), SoundEvents.SQUID_SQUIRT, 1.4F, 0.6F);
				} else {
					spot = null;
					entityData.set(DATA_DEPTH, 0.0F);
					pose(TideScribeRules.SURFACED, 0, TideScribeRules.length(TideScribeRules.SURFACED, phase));
					Fx.sound(level, position(), SoundEvents.GENERIC_SPLASH, 1.4F, 0.6F);
				}
			}
			default -> {
				spot = null;
				entityData.set(DATA_DEPTH, 0.0F);
				goal = null;
				chainOn(level, move, target);
			}
		}
	}

	/** In its third phase, what it may follow {@code move} with in the same breath (once, never twice running). */
	private void chainOn(ServerLevel level, int move, LivingEntity target) {
		if (phase < 3 || chained || target == null || TideScribeRules.opening(move) || stranded()) {
			return;
		}
		int next = TideScribeRules.chain(move, flooded(), distanceTo(target), random.nextDouble());
		if (next <= 0 || level.getGameTime() - made[next] < TideScribeRules.cooldown(next, phase) / 2) {
			return;
		}
		deck.played(next);
		chained = true;
		start(level, next, target);
	}

	@Override
	protected void moveBroken(ServerLevel level, int move) {
		glyphs.clear();
		glyphs2.clear();
		spot = null;
		goal = null;
		again = 0;
		entityData.set(DATA_DEPTH, 0.0F);
		entityData.set(DATA_GO, 0.0F);
	}

	@Override
	protected int rest(ServerLevel level, int move) {
		return move < SPELL ? TideScribeRules.rest(move, phase) : super.rest(level, move);
	}

	/** Everyone in its arena it may harm. */
	private List<LivingEntity> prey(ServerLevel level) {
		Vec3 c = Vec3.atBottomCenterOf(home);
		AABB arena = new AABB(c, c).inflate(PIT_RADIUS + 4, 0, PIT_RADIUS + 4).expandTowards(0, 8, 0).move(0, -2, 0);
		List<LivingEntity> out = new ArrayList<>();
		for (Entity e : level.getEntities(this, arena, e -> e instanceof LivingEntity && e.isAlive() && Targets.canHarm(this, e))) {
			out.add((LivingEntity) e);
		}
		return out;
	}

	/** A blow of {@code base} (scaled as its spells are) landing on {@code t}, caught for this swing in {@code swing}: once each. */
	private boolean strike(ServerLevel level, LivingEntity t, double base, Set<UUID> swing) {
		if (!swing.add(t.getUUID())) {
			return false;
		}
		return t.hurtServer(level, level.damageSources().mobAttack(this), (float) (base * power()));
	}

	/** Flat along {@code yaw}. */
	private static Vec3 ahead(float yaw) {
		double r = Math.toRadians(yaw);
		return new Vec3(-Math.sin(r), 0, Math.cos(r));
	}

	private float yawTo(Vec3 at) {
		return (float) Math.toDegrees(Math.atan2(-(at.x - getX()), at.z - getZ()));
	}

	private void turnTo(float yaw) {
		setYRot(yaw);
		yBodyRot = yaw;
		yHeadRot = yaw;
	}

	/** Its aim follows {@code target} until {@code lock} ticks before the blow at {@code tell}, then holds. */
	private void track(int t, int tell, int lock, LivingEntity target) {
		if (target != null && t <= tell - lock) {
			aim = yawTo(target.position());
		}
		turnTo(aim);
	}

	/** The pit floor's height. */
	private double floorY() {
		return home.getY();
	}

	/** {@code at} brought down onto the pit floor, just over it. */
	private Vec3 onFloor(Vec3 at) {
		return new Vec3(at.x, floorY() + 0.06, at.z);
	}

	/** Where something {@code dx},{@code dz} off its pit's heart may be, kept within its roam. */
	private Vec3 kept(double x, double y, double z) {
		Vec3 c = Vec3.atBottomCenterOf(home);
		double k = TideScribeRules.keep(x - c.x, z - c.z);
		return new Vec3(c.x + (x - c.x) * k, y, c.z + (z - c.z) * k);
	}

	@Override
	protected void moveTick(ServerLevel level, int move, int t, LivingEntity target) {
		if (home == null) {
			return;
		}
		// The flood's own moves end when the pit drains under them: the current has nothing to run in.
		if ((move == TideScribeRules.WHIRLPOOL || move == TideScribeRules.RIPTIDE) && !flooded()) {
			breakMove(level);
			return;
		}
		if (move == TideScribeRules.DIVE && !flooded()) {
			spot = null;
			entityData.set(DATA_DEPTH, 0.0F);
			pose(TideScribeRules.SURFACED, 0, TideScribeRules.length(TideScribeRules.SURFACED, phase));
			return;
		}
		int tell = moveTell;
		switch (move) {
			case TideScribeRules.QUILLS -> quills(level, t, tell, target);
			case TideScribeRules.SCRIPT -> script(level, t, tell, target);
			case TideScribeRules.WHIRLPOOL -> whirlpool(level, t, tell, target);
			case TideScribeRules.RIPTIDE -> riptide(level, t, tell, target);
			case TideScribeRules.INK -> ink(level, t, tell, target);
			case TideScribeRules.PAGES -> pages(level, t, tell, target);
			case TideScribeRules.BREAKER -> breaker(level, t, tell, target);
			case TideScribeRules.DIVE -> dive(level, t, tell, target);
			case TideScribeRules.TIDE_CALL -> tideCall(level, t, tell, target);
			case TideScribeRules.BOWED -> bowed(level, t);
			case TideScribeRules.SPUN -> spun(level, t);
			case TideScribeRules.SURFACED -> surfaced(level, t);
			default -> {
			}
		}
	}

	/** The height its marks are drawn at: on the water's top while the pit is flooded, on the floor while it's dry. */
	private double surface() {
		return floorY() + (flooded() ? 2.02 : 0.1);
	}

	/** {@code at} at the height its marks are drawn at. */
	private Vec3 marked(Vec3 at) {
		return new Vec3(at.x, surface(), at.z);
	}

	/**
	 * The volley: it draws its quill back and the lanes the quills will fly down are drawn on the floor before it, following its
	 * prey until they hold still a moment before it throws. In its third phase it throws again from the other hand, down the gaps.
	 */
	private void quills(ServerLevel level, int t, int tell, LivingEntity target) {
		int n = TideScribeRules.QUILL_LANES[Math.min(3, phase)];
		double spread = TideScribeRules.QUILL_SPREAD[Math.min(3, phase)];
		double offset = again == 1 ? spread / 2 : 0;
		Vec3 hand = position().add(0, 1.2, 0);
		goal = position();
		if (t < tell) {
			track(t, tell, TideScribeRules.QUILL_LOCK, target);
			boolean locked = t > tell - TideScribeRules.QUILL_LOCK;
			if (t == 0) {
				Fx.sound(level, hand, SoundEvents.ENCHANTMENT_TABLE_USE, 1.2F, 1.5F);
				Fx.sound(level, hand, SoundEvents.BOOK_PAGE_TURN, 1.4F, 0.8F);
			}
			if (t == tell - TideScribeRules.QUILL_LOCK) {
				Fx.sound(level, hand, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, 1.6F, 1.6F);
			}
			if (t % 2 == 0 || locked) {
				for (int i = 0; i < n; i++) {
					Vec3 dir = ahead((float) (aim + TideScribeRules.lane(i, n, spread, offset)));
					Vec3 near = marked(position().add(dir.scale(TideScribeRules.QUILL_NEAR)));
					Vec3 far = marked(position().add(dir.scale(TideScribeRules.QUILL_REACH)));
					ElementFx.ray(level, near, far, locked ? ACCENT : COLOR, locked ? 0.07 : 0.035, 3);
				}
			}
			Vfx.emit(level, ParticleTypes.ENCHANT, hand.add(ahead(aim + 90).scale(-0.6)), 3, 0.2, 0.3);
			return;
		}
		if (t == tell) {
			for (int i = 0; i < n; i++) {
				Vec3 dir = ahead((float) (aim + TideScribeRules.lane(i, n, spread, offset)));
				ElementFx.ray(level, hand, hand.add(dir.scale(TideScribeRules.QUILL_REACH)).add(0, -0.4, 0), ACCENT, 0.12, 6);
				Vfx.fling(level, ParticleTypes.CRIT, hand.add(dir), dir, 1.6);
				Vfx.emit(level, ParticleTypes.SQUID_INK, hand.add(dir.scale(1.2)), 3, 0.1, 0.05);
			}
			for (LivingEntity p : prey(level)) {
				double dx = p.getX() - getX();
				double dz = p.getZ() - getZ();
				double h = p.getY() - floorY();
				for (int i = 0; i < n; i++) {
					Vec3 f = ahead((float) (aim + TideScribeRules.lane(i, n, spread, offset)));
					if (TideScribeRules.quilled(dx * f.x + dz * f.z, dx * -f.z + dz * f.x, h)) {
						if (strike(level, p, TideScribeRules.QUILL_DAMAGE, struck)) {
							Vfx.emit(level, ParticleTypes.SQUID_INK, p.getBoundingBox().getCenter(), 6, 0.2, 0.05);
						}
						break;
					}
				}
			}
			Fx.sound(level, hand, SoundEvents.TRIDENT_THROW, 1.4F, 1.4F);
			Fx.sound(level, hand, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.6F);
		}
		if (moveLeft == 1 && phase >= 3 && again == 0) {
			secondGo(TideScribeRules.QUILLS, TideScribeRules.QUILL_AGAIN, TideScribeRules.QUILL_AGAIN + 6);
		}
	}

	/**
	 * Words on the floor: bowed to its scroll, it writes, and a word goes down glowing under everyone (and about its prey) to
	 * burst a moment later. In its third phase a second wave goes down where they stood when the first burst.
	 */
	private void script(ServerLevel level, int t, int tell, LivingEntity target) {
		int fuse = TideScribeRules.SCRIPT_FUSE;
		int second = tell + fuse;
		goal = position();
		if (t < tell) {
			if (target != null) {
				aim = yawTo(target.position());
			}
			turnTo(aim);
			// The ink runs off its page and down to the floor as it writes.
			Vec3 page = position().add(ahead(aim).scale(0.8)).add(0, 1.2, 0);
			Vfx.emit(level, ParticleTypes.ENCHANT, page, 4, 0.3, 0.4);
			Vfx.emit(level, ParticleTypes.SQUID_INK, page.add(0, -0.4, 0), 1, 0.2, 0.02);
			if (t % 3 == 0) {
				Fx.sound(level, page, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, 1.0F, 1.2F + t * 0.03F);
			}
			if (t == 0) {
				Fx.sound(level, page, SoundEvents.BOOK_PAGE_TURN, 1.4F, 0.7F);
				ElementFx.flatSigil(level, marked(position()), SigilOption.CIRCLE, COLOR, 3.0, tell, 0.06);
			}
			return;
		}
		if (t == tell) {
			write(level, glyphs, target, TideScribeRules.GLYPH_SCATTER[Math.min(3, phase)], fuse);
		} else if (t < second) {
			fizz(level, glyphs, t - tell, fuse);
		} else if (t == second) {
			burstWords(level, glyphs, struck);
			glyphs.clear();
			if (phase >= 3) {
				write(level, glyphs2, target, 1, TideScribeRules.SCRIPT_AGAIN);
			}
		} else if (t < second + TideScribeRules.SCRIPT_AGAIN) {
			fizz(level, glyphs2, t - second, TideScribeRules.SCRIPT_AGAIN);
		} else if (t == second + TideScribeRules.SCRIPT_AGAIN) {
			burstWords(level, glyphs2, struck2);
			glyphs2.clear();
		}
	}

	/** Writes a word under everyone it may harm within reach (and {@code scatter} more about its prey), each to burst in {@code fuse} ticks. */
	private void write(ServerLevel level, List<Vec3> words, LivingEntity target, int scatter, int fuse) {
		words.clear();
		for (LivingEntity p : prey(level)) {
			if (p.distanceTo(this) <= 18 && p.getY() - floorY() < 4) {
				words.add(new Vec3(p.getX(), p.onGround() ? p.getY() + 0.02 : floorY() + 0.02, p.getZ()));
			}
		}
		if (target != null) {
			for (int i = 0; i < scatter; i++) {
				double a = random.nextDouble() * Math.PI * 2;
				double r = 1.8 + random.nextDouble() * (TideScribeRules.GLYPH_SPREAD - 1.8);
				words.add(onFloor(kept(target.getX() + Math.cos(a) * r, 0, target.getZ() + Math.sin(a) * r)));
			}
		}
		float size = (float) (TideScribeRules.GLYPH_RADIUS * 2);
		for (Vec3 g : words) {
			Sigils.target(level, g, COLOR, size, fuse);
			ElementFx.flatSigil(level, g, SigilOption.STAR, ACCENT, size * 0.7, fuse, 0.08);
			Vfx.emit(level, ParticleTypes.SQUID_INK, g.add(0, 0.2, 0), 6, 0.4, 0.02);
		}
		if (!words.isEmpty()) {
			Fx.sound(level, words.get(0), SoundEvents.ENCHANTMENT_TABLE_USE, 1.4F, 0.8F);
		}
	}

	/** Its words on the floor, {@code t} ticks into a {@code fuse}: brightening, and buzzing just before they burst. */
	private void fizz(ServerLevel level, List<Vec3> words, int t, int fuse) {
		boolean soon = t >= fuse - 5;
		for (Vec3 g : words) {
			if (soon || t % 3 == 0) {
				Vfx.emit(level, soon ? ParticleTypes.GLOW : ParticleTypes.ENCHANT, g.add(0, 0.3, 0), soon ? 3 : 2, TideScribeRules.GLYPH_RADIUS * 0.5, 0.05);
			}
		}
		if (t == fuse - 5 && !words.isEmpty()) {
			Fx.sound(level, words.get(0), SoundEvents.BOOK_PAGE_TURN, 1.6F, 1.8F);
		}
	}

	/** Its words burst: whoever stands on one is thrown up off it, once however many they stand on. */
	private void burstWords(ServerLevel level, List<Vec3> words, Set<UUID> swing) {
		List<LivingEntity> prey = prey(level);
		for (Vec3 g : words) {
			Sigils.flash(level, g.add(0, 0.6, 0), ACCENT, 2.5F);
			ElementFx.groundRing(level, g, ACCENT, 0.2, TideScribeRules.GLYPH_RADIUS, 0.2, 6);
			Vfx.emit(level, ParticleTypes.SQUID_INK, g.add(0, 0.6, 0), 14, 0.5, 0.15);
			Vfx.emit(level, ParticleTypes.END_ROD, g.add(0, 0.3, 0), 6, 0.3, 0.2);
			for (LivingEntity p : prey) {
				if (TideScribeRules.scribed(p.getX() - g.x, p.getZ() - g.z, p.getY() - g.y)
					&& strike(level, p, TideScribeRules.GLYPH_DAMAGE, swing)) {
					Effects.push(p, new Vec3(0, TideScribeRules.GLYPH_LIFT, 0));
				}
			}
		}
		if (!words.isEmpty()) {
			Fx.sound(level, words.get(0), SoundEvents.GLOW_INK_SAC_USE, 1.6F, 0.7F);
			Fx.sound(level, words.get(0), SoundEvents.SQUID_SQUIRT, 1.2F, 1.2F);
		}
	}

	/**
	 * The whirlpool: it sinks and starts to turn, the current marked closing in on the water; then it spins, dragging everyone
	 * wading toward it, its heart churning. In its third phase it ends by throwing everything near its heart back out.
	 */
	private void whirlpool(ServerLevel level, int t, int tell, LivingEntity target) {
		Vec3 top = marked(position());
		goal = new Vec3(getX(), floorY() + 0.6, getZ());
		if (t == 0) {
			aim = getYRot();
			Fx.sound(level, top, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.6F, 0.8F);
			ElementFx.groundRing(level, top.subtract(0, 0.08, 0), COLOR, TideScribeRules.WHIRL_RADIUS, TideScribeRules.WHIRL_CORE, 0.25, tell);
			Sigils.target(level, top, ACCENT, (float) (TideScribeRules.WHIRL_CORE * 2), tell + TideScribeRules.WHIRL_TICKS);
		}
		// Turning, slowly as it winds up and then fast.
		float turn = t < tell ? t * 1.5F : tell * 1.5F + (t - tell) * 22F;
		turnTo(aim + turn);
		if (t < tell) {
			double r = TideScribeRules.WHIRL_RADIUS * (1 - t / (double) tell);
			for (int i = 0; i < 3; i++) {
				double a = t * 0.4 + i * Math.PI * 2 / 3;
				Vfx.emit(level, ParticleTypes.BUBBLE, top.add(Math.cos(a) * r, -0.3, Math.sin(a) * r), 2, 0.1, 0.02);
			}
			if (t == tell - 6) {
				Fx.sound(level, top, SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 1.4F, 0.6F);
			}
			return;
		}
		int k = t - tell;
		// The current, spiralling in.
		for (int i = 0; i < 4; i++) {
			double frac = (k * 0.08 + i / 4.0) % 1.0;
			double r = TideScribeRules.WHIRL_RADIUS * (1 - frac);
			double a = k * 0.5 + frac * 6 + i * Math.PI / 2;
			Vfx.emit(level, ParticleTypes.BUBBLE, top.add(Math.cos(a) * r, -0.2, Math.sin(a) * r), 2, 0.1, 0.02);
		}
		Vfx.emit(level, ParticleTypes.CURRENT_DOWN, top.add(0, -0.4, 0), 4, TideScribeRules.WHIRL_CORE * 0.5, 0.05);
		if (k % 6 == 0) {
			ElementFx.swirl(level, top.add(0, -0.6, 0), TideScribeRules.WHIRL_RADIUS * 0.55, 1.2, 3, COLOR, ACCENT);
		}
		if (k % 20 == 0) {
			Fx.sound(level, top, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.4F, 0.9F);
		}
		List<LivingEntity> prey = prey(level);
		// Everyone wading is dragged round and in, a tug every few ticks (never anyone on the ice).
		if (k % 4 == 0) {
			for (LivingEntity p : prey) {
				if (!p.isInWater()) {
					continue;
				}
				Vec3 in = new Vec3(getX() - p.getX(), 0, getZ() - p.getZ());
				double d = in.length();
				double drag = TideScribeRules.drag(d, phase);
				if (drag <= 0) {
					continue;
				}
				Vec3 n = in.scale(1 / d);
				Vec3 round = new Vec3(-n.z, 0, n.x);
				Effects.push(p, n.add(round.scale(0.6)).normalize().scale(drag * 2.5).subtract(p.getDeltaMovement().scale(0.5).multiply(1, 0, 1)));
			}
		}
		// Its heart churns.
		if (k % TideScribeRules.WHIRL_EVERY == TideScribeRules.WHIRL_EVERY / 2) {
			struck.clear();
			ElementFx.groundRing(level, top.subtract(0, 0.08, 0), ACCENT, 0.3, TideScribeRules.WHIRL_CORE, 0.3, 6);
			Vfx.emit(level, ParticleTypes.BUBBLE_COLUMN_UP, top.add(0, -1.2, 0), 12, TideScribeRules.WHIRL_CORE * 0.5, 0.2);
			Fx.sound(level, top, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 1.4F, 0.6F);
			for (LivingEntity p : prey) {
				double d = Math.hypot(p.getX() - getX(), p.getZ() - getZ());
				if (TideScribeRules.churns(d, p.getY() - floorY())) {
					strike(level, p, TideScribeRules.WHIRL_DAMAGE, struck);
				}
			}
		}
		if (phase >= 3) {
			if (moveLeft == 10) {
				ElementFx.groundRing(level, top, ACCENT, TideScribeRules.WHIRL_FLING, TideScribeRules.WHIRL_FLING, 0.3, 10);
				Sigils.target(level, top, ACCENT, (float) (TideScribeRules.WHIRL_FLING * 2), 10);
				Fx.sound(level, top, SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 1.8F, 1.2F);
			}
			if (moveLeft == 1) {
				// Everything near its heart is thrown back out.
				ElementFx.groundRing(level, top, ACCENT, 0.5, TideScribeRules.WHIRL_FLING + 2, 0.4, 8);
				Vfx.radial(level, ParticleTypes.SPLASH, top, 30, 0.6);
				Fx.sound(level, top, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.8F, 0.6F);
				for (LivingEntity p : prey) {
					Vec3 out = new Vec3(p.getX() - getX(), 0, p.getZ() - getZ());
					double d = out.length();
					double h = p.getY() - floorY();
					if (d <= TideScribeRules.WHIRL_FLING && h > -1.5 && h < 3.5 && strike(level, p, TideScribeRules.WHIRL_FLING_DAMAGE, struck2)) {
						Vec3 n = d < 0.1 ? ahead(getYRot()) : out.scale(1 / d);
						Effects.push(p, n.scale(1.2).add(0, 0.5, 0));
					}
				}
			}
		}
	}

	/**
	 * The riptide: it coils back and the lane it will tear down is drawn on the water, following its prey until it holds a moment
	 * before it goes; then it races down it, sweeping whoever's in the lane along before it. In its third phase it turns at the
	 * end and tears back again.
	 */
	private void riptide(ServerLevel level, int t, int tell, LivingEntity target) {
		Vec3 dir = ahead(aim);
		if (t < tell) {
			track(t, tell, TideScribeRules.RIP_LOCK, target);
			dir = ahead(aim);
			goal = position();
			boolean locked = t > tell - TideScribeRules.RIP_LOCK;
			if (!locked) {
				from = position();
			}
			if (t % 2 == 0 || locked) {
				Vec3 side = new Vec3(-dir.z, 0, dir.x).scale(TideScribeRules.RIP_HALF);
				Vec3 a = marked(from);
				Vec3 b = marked(from.add(dir.scale(TideScribeRules.RIP_REACH)));
				ElementFx.ray(level, a.add(side), b.add(side), locked ? ACCENT : COLOR, locked ? 0.07 : 0.04, 3);
				ElementFx.ray(level, a.subtract(side), b.subtract(side), locked ? ACCENT : COLOR, locked ? 0.07 : 0.04, 3);
				ElementFx.ray(level, a, b, COLOR, 0.02, 3);
			}
			// Coiling back, the water boiling at its tail.
			Vfx.emit(level, ParticleTypes.BUBBLE, position().subtract(dir.scale(0.9)).add(0, 0.4, 0), 3, 0.3, 0.06);
			if (t == 0) {
				Fx.sound(level, position(), SoundEvents.GENERIC_SPLASH, 1.2F, 0.5F);
			}
			if (t == tell - TideScribeRules.RIP_LOCK) {
				Fx.sound(level, position(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 1.6F, 1.4F);
			}
			return;
		}
		int k = t - tell;
		if (k < TideScribeRules.RIP_TICKS) {
			double front = Math.min(TideScribeRules.RIP_REACH, (k + 1) * 1.5);
			goal = kept(from.x + dir.x * (front - 1.5), getY(), from.z + dir.z * (front - 1.5));
			Vec3 at = marked(from.add(dir.scale(front)));
			Vfx.emit(level, ParticleTypes.SPLASH, at, 14, 0.8, 0.2);
			Vfx.emit(level, ParticleTypes.BUBBLE, at.add(0, -0.4, 0), 6, 0.7, 0.1);
			Vfx.fling(level, ParticleTypes.CLOUD, at, dir.add(0, 0.3, 0), 0.3);
			if (k == 0) {
				Fx.sound(level, position(), SoundEvents.TRIDENT_RIPTIDE_1, 1.6F, 0.9F);
			}
			if (k % 3 == 0) {
				Fx.sound(level, at, SoundEvents.GENERIC_SPLASH, 1.0F, 1.2F);
			}
			for (LivingEntity p : prey(level)) {
				double dx = p.getX() - from.x;
				double dz = p.getZ() - from.z;
				double fwd = dx * dir.x + dz * dir.z;
				if (fwd <= front + 0.5 && TideScribeRules.swept(fwd, dx * -dir.z + dz * dir.x, p.getY() - floorY())
					&& strike(level, p, TideScribeRules.RIP_DAMAGE, struck)) {
					Effects.push(p, dir.scale(TideScribeRules.RIP_SWEEP).add(0, 0.15, 0));
				}
			}
		} else {
			goal = null;
		}
		if (moveLeft == 1 && phase >= 3 && again == 0 && target != null) {
			// It turns at the end of the lane and tears back.
			aim = yawTo(target.position());
			turnTo(aim);
			from = position();
			secondGo(TideScribeRules.RIPTIDE, TideScribeRules.RIP_AGAIN, TideScribeRules.RIP_AGAIN + TideScribeRules.RIP_TICKS + 4);
		}
	}

	/**
	 * The ink: its head swells dark and the cone it will spray is drawn before it, following its prey until it holds; then a cloud
	 * of ink bursts across it, blinding and slowing whoever it catches. Wider in its third phase. Its back is the safe side.
	 */
	private void ink(ServerLevel level, int t, int tell, LivingEntity target) {
		goal = position();
		boolean wide = phase >= 3;
		double angle = wide ? TideScribeRules.INK_ANGLE_WIDE : TideScribeRules.INK_ANGLE;
		Vec3 head = position().add(0, 1.6, 0);
		if (t < tell) {
			track(t, tell, TideScribeRules.INK_LOCK, target);
			boolean locked = t > tell - TideScribeRules.INK_LOCK;
			if (t % 3 == 0 || t == tell - TideScribeRules.INK_LOCK + 1) {
				Vec3 base = marked(position());
				Vec3 l = ahead((float) (aim - angle));
				Vec3 r = ahead((float) (aim + angle));
				int c = locked ? ACCENT : COLOR;
				ElementFx.ray(level, base.add(l), base.add(l.scale(TideScribeRules.INK_REACH)), c, 0.05, 3);
				ElementFx.ray(level, base.add(r), base.add(r.scale(TideScribeRules.INK_REACH)), c, 0.05, 3);
				ElementFx.slash(level, base, new Vec3(0, 1, 0), ahead(aim), c, TideScribeRules.INK_REACH, Math.toRadians(angle * 2), 0.15, 0, 3);
			}
			Vfx.emit(level, ParticleTypes.SQUID_INK, head, 2 + t / 4, 0.15 + t * 0.01, 0.01);
			if (t == 0) {
				Fx.sound(level, head, SoundEvents.SQUID_SQUIRT, 0.8F, 1.6F);
			}
			if (t == tell - TideScribeRules.INK_LOCK) {
				Fx.sound(level, head, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 1.6F, 0.5F);
			}
			return;
		}
		Vec3 f = ahead(aim);
		if (t == tell) {
			for (LivingEntity p : prey(level)) {
				double dx = p.getX() - getX();
				double dz = p.getZ() - getZ();
				if (TideScribeRules.inked(dx * f.x + dz * f.z, dx * -f.z + dz * f.x, p.getY() - getY(), wide)
					&& strike(level, p, TideScribeRules.INK_DAMAGE, struck)) {
					p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, TideScribeRules.INK_BLIND, 0, false, true), this);
					p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, TideScribeRules.INK_SLOW, 0, false, true), this);
				}
			}
			for (int i = -3; i <= 3; i++) {
				Vec3 d = ahead((float) (aim + angle * i / 3.0));
				Vfx.fling(level, ParticleTypes.SQUID_INK, head.add(d), d.add(0, -0.1, 0), 0.9);
				Vfx.fling(level, ParticleTypes.SQUID_INK, head.add(d), d, 0.6);
			}
			Fx.sound(level, head, SoundEvents.SQUID_SQUIRT, 1.8F, 0.6F);
			Fx.sound(level, head, SoundEvents.GLOW_INK_SAC_USE, 1.2F, 0.5F);
		}
		int k = t - tell;
		if (k > 0 && k < 8) {
			// The cloud hangs a moment over where it sprayed.
			for (int i = 0; i < 3; i++) {
				double a = aim + (random.nextDouble() * 2 - 1) * angle;
				double r = 1.5 + random.nextDouble() * (TideScribeRules.INK_REACH - 1.5);
				Vfx.emit(level, ParticleTypes.SQUID_INK, position().add(ahead((float) a).scale(r)).add(0, 1.0, 0), 4, 0.6, 0.02);
			}
		}
	}

	/**
	 * The pages: it tears its pages out and they wheel round it in a ring, marked on the floor, cutting whoever stands in the ring
	 * as they pass; inside the ring, close against it, is safe, and so is well outside. It drifts after its prey as they wheel.
	 * In its third phase the ring draws in and then flies out across the floor at the end (jump it).
	 */
	private void pages(ServerLevel level, int t, int tell, LivingEntity target) {
		Vec3 base = marked(position());
		if (t < tell) {
			goal = position();
			if (target != null) {
				turnTo(yawTo(target.position()));
			}
			if (t == 0) {
				ElementFx.groundRing(level, base, COLOR, TideScribeRules.PAGE_INNER, TideScribeRules.PAGE_INNER, 0.12, tell + 4);
				ElementFx.groundRing(level, base, COLOR, TideScribeRules.PAGE_OUTER, TideScribeRules.PAGE_OUTER, 0.12, tell + 4);
				Fx.sound(level, base, SoundEvents.BOOK_PAGE_TURN, 1.6F, 0.6F);
			}
			if (t % 3 == 0) {
				Fx.sound(level, position(), SoundEvents.BOOK_PAGE_TURN, 1.2F, 0.8F + t * 0.05F);
			}
			// Pages torn loose, fluttering out from its scroll.
			Vfx.emit(level, ParticleTypes.ENCHANT, position().add(0, 1.3, 0), 4, 0.6, 0.4);
			Vfx.emit(level, ParticleTypes.END_ROD, position().add(0, 1.3, 0), 1, 0.4, 0.05);
			return;
		}
		int k = t - tell;
		if (k < TideScribeRules.PAGES_TICKS) {
			// It drifts after its prey as the ring wheels.
			if (target != null) {
				Vec3 to = new Vec3(target.getX() - getX(), 0, target.getZ() - getZ());
				double d = to.length();
				goal = d > 0.5 ? kept(getX() + to.x / d * 0.6, getY(), getZ() + to.z / d * 0.6) : position();
			}
			double mid = (TideScribeRules.PAGE_INNER + TideScribeRules.PAGE_OUTER) / 2;
			for (int i = 0; i < 6; i++) {
				double a = k * 0.35 + i * Math.PI / 3;
				Vfx.emit(level, ParticleTypes.END_ROD, position().add(Math.cos(a) * mid, 1.0, Math.sin(a) * mid), 1, 0.4, 0.01);
				Vfx.emit(level, ParticleTypes.ENCHANT, position().add(Math.cos(a) * mid, 1.2, Math.sin(a) * mid), 2, 0.6, 0.05);
			}
			if (k % 10 == 0) {
				ElementFx.orbit(level, position().add(0, 1.0, 0), mid, 6, 10, COLOR, ACCENT);
				ElementFx.groundRing(level, base, COLOR, TideScribeRules.PAGE_INNER, TideScribeRules.PAGE_INNER, 0.1, 10);
				ElementFx.groundRing(level, base, COLOR, TideScribeRules.PAGE_OUTER, TideScribeRules.PAGE_OUTER, 0.1, 10);
			}
			boolean drawIn = phase >= 3 && k >= TideScribeRules.PAGES_TICKS - 10;
			if (k % TideScribeRules.PAGES_EVERY == TideScribeRules.PAGES_EVERY / 2 && !drawIn) {
				struck.clear();
				Fx.sound(level, position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 1.5F);
				for (LivingEntity p : prey(level)) {
					if (TideScribeRules.cut(Math.hypot(p.getX() - getX(), p.getZ() - getZ()), p.getY() - floorY())
						&& strike(level, p, TideScribeRules.PAGE_DAMAGE, struck)) {
						Vfx.emit(level, ParticleTypes.SWEEP_ATTACK, p.getBoundingBox().getCenter(), 1, 0, 0);
					}
				}
			}
			if (phase >= 3 && k == TideScribeRules.PAGES_TICKS - 10) {
				// The ring draws in tight and glows: about to fly.
				ElementFx.groundRing(level, base, ACCENT, TideScribeRules.PAGE_OUTER, TideScribeRules.PAGE_OUTER, 0.25, 10);
				ElementFx.groundRing(level, base, ACCENT, TideScribeRules.PAGE_OUTER, TideScribeRules.PAGE_FLY_REACH, 0.08, 10);
				Fx.sound(level, position(), SoundEvents.ENCHANTMENT_TABLE_USE, 1.6F, 1.6F);
			}
			return;
		}
		// Its third phase: the ring flies out across the floor.
		goal = position();
		int j = k - TideScribeRules.PAGES_TICKS;
		double r0 = TideScribeRules.PAGE_OUTER + j * TideScribeRules.PAGE_FLY_SPEED;
		double r1 = r0 + TideScribeRules.PAGE_FLY_SPEED;
		if (j == 0) {
			ElementFx.groundRing(level, base, ACCENT, TideScribeRules.PAGE_OUTER, TideScribeRules.PAGE_FLY_REACH, 0.35,
				TideScribeRules.rolls(TideScribeRules.PAGE_FLY_REACH - TideScribeRules.PAGE_OUTER, TideScribeRules.PAGE_FLY_SPEED));
			Fx.sound(level, position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.8F, 0.8F);
		}
		for (int i = 0; i < 8; i++) {
			double a = i * Math.PI / 4 + j * 0.2;
			Vfx.emit(level, ParticleTypes.END_ROD, base.add(Math.cos(a) * r1, 0.3, Math.sin(a) * r1), 1, 0.1, 0.01);
		}
		for (LivingEntity p : prey(level)) {
			if (TideScribeRules.crosses(Math.hypot(p.getX() - getX(), p.getZ() - getZ()), r0, r1, p.getY() - base.y)) {
				strike(level, p, TideScribeRules.PAGE_DAMAGE, struck2);
			}
		}
	}

	/**
	 * How high off the pit floor its waves roll: along the floor when the pit is dry, and along the water's top when it's flooded,
	 * so whoever wades on the bottom ducks under them and whoever swims or stands on the ice must jump them.
	 */
	private double waveBase() {
		return floorY() + (flooded() ? 1.6 : 0);
	}

	/**
	 * The breaker: it rises up out of the pit, water streaming off it, the reach of the wave marked faintly about it; then it
	 * drops and a ring of water rolls out from where it landed (jump it, or duck under it on the bottom of a flooded pit). In
	 * its third phase a second ring follows the first.
	 */
	private void breaker(ServerLevel level, int t, int tell, LivingEntity target) {
		if (t < tell) {
			goal = new Vec3(getX(), Math.min(floorY() + 4.5, from.y + 2.0 * t / tell), getZ());
			if (target != null) {
				turnTo(yawTo(target.position()));
			}
			Vec3 c = marked(position());
			if (t == 0) {
				Sigils.ground(level, c, COLOR, ACCENT, 3.0F, tell);
				ElementFx.groundRing(level, c, COLOR, TideScribeRules.BREAKER_REACH, TideScribeRules.BREAKER_REACH, 0.08, tell);
				Fx.sound(level, c, SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 1.6F, 0.5F);
			}
			ElementFx.drip(level, position().add(0, 1.0, 0), 0.6, 2);
			Vfx.emit(level, ParticleTypes.SPLASH, c, 6, 1.2, 0.1);
			if (t == tell - 5) {
				Fx.sound(level, c, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.2F, 1.6F);
			}
			return;
		}
		int k = t - tell;
		goal = new Vec3(getX(), floorY() + 0.4, getZ());
		if (k == 0) {
			spot = marked(position());
			ElementFx.groundRing(level, spot, ACCENT, 0.5, TideScribeRules.BREAKER_REACH, 0.5,
				TideScribeRules.rolls(TideScribeRules.BREAKER_REACH, TideScribeRules.BREAKER_SPEED));
			Vfx.radial(level, ParticleTypes.SPLASH, spot, 40, 0.8);
			Sigils.flash(level, spot.add(0, 0.4, 0), ACCENT, 3.0F);
			Fx.sound(level, spot, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 2.0F, 0.5F);
			Fx.sound(level, spot, SoundEvents.GENERIC_SPLASH, 1.6F, 0.7F);
		}
		if (spot == null) {
			spot = marked(position());
		}
		double base = waveBase();
		roll(level, spot, base, k, struck);
		if (phase >= 3) {
			int j = k - TideScribeRules.BREAKER_AGAIN;
			if (j == 0) {
				ElementFx.groundRing(level, spot, ACCENT, 0.5, TideScribeRules.BREAKER_REACH, 0.5,
					TideScribeRules.rolls(TideScribeRules.BREAKER_REACH, TideScribeRules.BREAKER_SPEED));
				Fx.sound(level, spot, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.8F, 0.7F);
			}
			if (j >= 0) {
				roll(level, spot, base, j, struck2);
			}
		}
	}

	/** One tick of a breaker's ring rolling out from {@code c}, {@code k} ticks after it began, at {@code base}. */
	private void roll(ServerLevel level, Vec3 c, double base, int k, Set<UUID> swing) {
		double r0 = k * TideScribeRules.BREAKER_SPEED;
		double r1 = r0 + TideScribeRules.BREAKER_SPEED;
		if (r0 > TideScribeRules.BREAKER_REACH) {
			return;
		}
		for (int i = 0; i < 14; i++) {
			double a = i * Math.PI * 2 / 14 + k * 0.1;
			Vfx.emit(level, ParticleTypes.SPLASH, c.add(Math.cos(a) * r1, 0.2, Math.sin(a) * r1), 2, 0.15, 0.1);
		}
		if (k % 5 == 0) {
			Fx.sound(level, c, SoundEvents.GENERIC_SPLASH, 0.8F, 1.0F + k * 0.02F);
		}
		for (LivingEntity p : prey(level)) {
			Vec3 out = new Vec3(p.getX() - c.x, 0, p.getZ() - c.z);
			double d = out.length();
			if (TideScribeRules.crosses(d, r0, r1, p.getY() - base) && strike(level, p, TideScribeRules.BREAKER_DAMAGE, swing)) {
				Vec3 n = d < 0.1 ? ahead(getYRot()) : out.scale(1 / d);
				Effects.push(p, n.scale(TideScribeRules.BREAKER_SHOVE).add(0, 0.3, 0));
			}
		}
	}

	/**
	 * The dive: it sinks under, leaving a stain of ink on the water, and a trail of bubbles hunts its prey across the pit floor;
	 * a moment before it bursts up, the trail stops and its spot is marked (get off the mark). Still in the water the whole
	 * time, so a storm spell shocks it out. In its third phase it goes under once more before it surfaces.
	 */
	private void dive(ServerLevel level, int t, int tell, LivingEntity target) {
		if (t < tell) {
			if (t == 0) {
				entityData.set(DATA_DEPTH, 1.0F);
				Fx.sound(level, position(), SoundEvents.SQUID_SQUIRT, 1.4F, 0.7F);
			}
			goal = new Vec3(getX(), floorY() + 0.2, getZ());
			Vec3 top = marked(position());
			Vfx.emit(level, ParticleTypes.SQUID_INK, top, 6, 0.8, 0.02);
			Vfx.emit(level, ParticleTypes.BUBBLE, position().add(0, 0.5, 0), 4, 0.4, 0.1);
			if (t == tell - 3) {
				Fx.sound(level, top, SoundEvents.PLAYER_SPLASH, 1.2F, 0.6F);
			}
			return;
		}
		int k = t - tell;
		int hunt = TideScribeRules.DIVE_HUNT;
		int lock = hunt - TideScribeRules.DIVE_LOCK;
		if (spot == null) {
			spot = new Vec3(getX(), floorY(), getZ());
		}
		if (k < hunt) {
			if (k < lock && target != null) {
				spot = kept(TideScribeRules.follow(spot.x, target.getX(), TideScribeRules.DIVE_FOLLOW), floorY(),
					TideScribeRules.follow(spot.z, target.getZ(), TideScribeRules.DIVE_FOLLOW));
			}
			goal = new Vec3(spot.x, floorY() + 0.2, spot.z);
			Vec3 top = marked(spot);
			Vfx.emit(level, ParticleTypes.BUBBLE_COLUMN_UP, new Vec3(spot.x, floorY() + 0.5, spot.z), 3, 0.3, 0.1);
			Vfx.emit(level, ParticleTypes.BUBBLE_POP, top, 3, 0.4, 0.02);
			if (k % 8 == 0) {
				Fx.sound(level, top, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 1.0F, 0.8F);
			}
			if (k == lock) {
				Sigils.target(level, top, ACCENT, (float) (TideScribeRules.DIVE_RADIUS * 2), TideScribeRules.DIVE_LOCK);
				ElementFx.groundRing(level, top, ACCENT, TideScribeRules.DIVE_RADIUS, TideScribeRules.DIVE_RADIUS, 0.2, TideScribeRules.DIVE_LOCK);
				Fx.sound(level, top, SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 1.6F, 1.2F);
			}
			if (k > lock && (k - lock) % 4 == 0) {
				Fx.sound(level, top, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 1.4F, 1.0F + (k - lock) * 0.05F);
			}
			return;
		}
		Vec3 top = marked(spot);
		if (k == hunt) {
			for (LivingEntity p : prey(level)) {
				if (TideScribeRules.burst(p.getX() - spot.x, p.getZ() - spot.z, p.getY() - floorY())
					&& strike(level, p, TideScribeRules.DIVE_DAMAGE, struck)) {
					Effects.push(p, new Vec3(0, TideScribeRules.DIVE_LIFT, 0));
				}
			}
			Vfx.emit(level, ParticleTypes.BUBBLE_COLUMN_UP, top, 30, 0.6, 0.6);
			Vfx.radial(level, ParticleTypes.SPLASH, top, 30, 0.6);
			Vfx.emit(level, ParticleTypes.SQUID_INK, top.add(0, 0.8, 0), 20, 0.6, 0.1);
			Sigils.flash(level, top.add(0, 0.6, 0), ACCENT, 3.0F);
			Fx.sound(level, top, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 2.0F, 0.8F);
			Fx.sound(level, top, SoundEvents.SQUID_SQUIRT, 1.4F, 0.5F);
			entityData.set(DATA_DEPTH, 0.0F);
		}
		goal = new Vec3(spot.x, floorY() + 2.6, spot.z);
	}

	/**
	 * The tide call: it rises over its pit's heart and calls, the rim marked; then the water rushes in as a ring closing from the
	 * rim on the heart (jump it), sweeping whoever it catches inward, and the tide starts to rise early.
	 */
	private void tideCall(ServerLevel level, int t, int tell, LivingEntity target) {
		Vec3 c = Vec3.atBottomCenterOf(home);
		Vec3 floor = new Vec3(c.x, surface(), c.z);
		goal = new Vec3(c.x, floorY() + 2.5, c.z);
		if (target != null) {
			turnTo(yawTo(target.position()));
		}
		if (t < tell) {
			if (t == 0) {
				ElementFx.groundRing(level, floor, ACCENT, TideScribeRules.CALL_FROM, TideScribeRules.CALL_FROM, 0.3, tell);
				Sigils.ground(level, floor, COLOR, ACCENT, 4.0F, tell);
				Fx.sound(level, floor, SoundEvents.CONDUIT_ACTIVATE, 1.6F, 0.8F);
			}
			// The water gathers at the rim.
			for (int i = 0; i < 4; i++) {
				double a = random.nextDouble() * Math.PI * 2;
				Vfx.emit(level, ParticleTypes.SPLASH,
					floor.add(Math.cos(a) * TideScribeRules.CALL_FROM, 0.2, Math.sin(a) * TideScribeRules.CALL_FROM), 4, 0.3, 0.1);
			}
			Vfx.emit(level, ParticleTypes.ENCHANT, position().add(0, 1.4, 0), 3, 0.5, 0.3);
			if (t == tell - 6) {
				Fx.sound(level, floor, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.6F, 0.6F);
			}
			return;
		}
		int k = t - tell;
		double r0 = TideScribeRules.CALL_FROM - k * TideScribeRules.CALL_SPEED;
		double r1 = r0 - TideScribeRules.CALL_SPEED;
		if (k == 0) {
			ElementFx.groundRing(level, floor, ACCENT, TideScribeRules.CALL_FROM, 0.5, 0.5,
				TideScribeRules.rolls(TideScribeRules.CALL_FROM, TideScribeRules.CALL_SPEED));
			Fx.sound(level, floor, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 2.0F, 0.5F);
		}
		if (r0 > 0) {
			double r = Math.max(0.3, r1);
			for (int i = 0; i < 16; i++) {
				double a = i * Math.PI / 8 - k * 0.08;
				Vfx.emit(level, ParticleTypes.SPLASH, floor.add(Math.cos(a) * r, 0.2, Math.sin(a) * r), 2, 0.15, 0.12);
			}
			if (k % 4 == 0) {
				Fx.sound(level, floor, SoundEvents.GENERIC_SPLASH, 1.0F, 0.8F + k * 0.02F);
			}
			double base = waveBase();
			for (LivingEntity p : prey(level)) {
				Vec3 in = new Vec3(c.x - p.getX(), 0, c.z - p.getZ());
				double d = in.length();
				if (TideScribeRules.crosses(d, r1, r0, p.getY() - base) && strike(level, p, TideScribeRules.CALL_DAMAGE, struck)) {
					Vec3 n = d < 0.1 ? Vec3.ZERO : in.scale(1 / d);
					Effects.push(p, n.scale(TideScribeRules.CALL_SWEEP).add(0, 0.2, 0));
				}
			}
		}
		if (moveLeft == 1 && hasArena(level) && tide == Tide.LOW) {
			startRising(level);
		}
	}

	/** Bowed over its writing after its words burst, low and still: the time to hit it. */
	private void bowed(ServerLevel level, int t) {
		goal = new Vec3(getX(), floorY() + 0.8, getZ());
		Vfx.emit(level, ParticleTypes.ENCHANT, position().add(ahead(getYRot()).scale(0.6)).add(0, 1.0, 0), 2, 0.4, 0.2);
		if (t % 8 == 0) {
			Fx.sound(level, position(), SoundEvents.VILLAGER_WORK_LIBRARIAN, 0.8F, 0.8F);
		}
	}

	/** Spun dizzy after its whirlpool, thrown up out of the water and slowing: the time to hit it. */
	private void spun(ServerLevel level, int t) {
		goal = new Vec3(getX(), floorY() + 3.2, getZ());
		turnTo(getYRot() + Math.max(2, 30 - t));
		ElementFx.drip(level, position().add(0, 1.2, 0), 0.5, 2);
		if (t % 4 == 0) {
			double a = t * 0.5;
			Vfx.emit(level, ParticleTypes.CRIT, position().add(Math.cos(a) * 0.6, 2.2, Math.sin(a) * 0.6), 2, 0.05, 0.02);
		}
		if (t % 10 == 0) {
			Fx.sound(level, position(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 0.8F, 0.6F);
		}
	}

	/** Surfaced after its dive, dazed and half out of the water: the time to hit it. */
	private void surfaced(ServerLevel level, int t) {
		goal = new Vec3(getX(), floorY() + 1.2, getZ());
		Vfx.emit(level, ParticleTypes.BUBBLE, position().add(0, 0.6, 0), 2, 0.5, 0.05);
		if (t % 3 == 0) {
			Vfx.emit(level, ParticleTypes.BUBBLE_POP, position().add(0, 1.4, 0), 2, 0.4, 0.02);
		}
		if (t % 10 == 0) {
			Fx.sound(level, position(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 0.8F, 0.7F);
		}
	}

	// ------------------------------------------------------------------ moving

	@Override
	protected void approach(ServerLevel level, LivingEntity target) {
	}

	@Override
	protected boolean mayCast(ServerLevel level, LivingEntity target) {
		return !stranded();
	}

	@Override
	protected void mechanic(ServerLevel level, long now) {
		AWAKE.add(this);
		tickTide(level, now);
		// Caught out after a move gone wide it is open, out of the water's wrap or not.
		boolean open = TideScribeRules.opening(move);
		setState(EXPOSED, stranded() || open);
		setState(GUARDED, flooded() && isInWater() && !open);
		Vec3 centre = Vec3.atBottomCenterOf(home);
		if (stranded()) {
			setDeltaMovement(getDeltaMovement().scale(0.5));
		} else if (move > 0 && move < SPELL) {
			// In a move of its own it goes where the move takes it: racing in a riptide, gliding under the ink, drifting otherwise.
			if (goal == null) {
				setDeltaMovement(getDeltaMovement().scale(0.5));
			} else {
				Vec3 to = kept(goal.x, goal.y, goal.z);
				Vec3 pull = to.subtract(position()).scale(0.35);
				double most = move == TideScribeRules.RIPTIDE ? 1.3 : move == TideScribeRules.DIVE ? 0.5 : 0.6;
				if (pull.length() > most) {
					pull = pull.normalize().scale(most);
				}
				setDeltaMovement(pull);
			}
		} else {
			// Swimming in wide, quick circles in the flood; drifting low and slow over the dry floor.
			boolean wet = flooded();
			swim += wet ? 0.03 : 0.012;
			double radius = wet ? 6.5 : 4.5;
			Vec3 circle = centre.add(Math.cos(swim) * radius, wet ? 0.4 : 1.4 + Math.sin(tickCount * 0.06) * 0.3, Math.sin(swim) * radius);
			Vec3 pull = circle.subtract(position()).scale(0.08);
			if (pull.length() > (wet ? 0.4 : 0.25)) {
				pull = pull.normalize().scale(wet ? 0.4 : 0.25);
			}
			setDeltaMovement(pull);
			LivingEntity target = getTarget();
			if (target != null) {
				getLookControl().setLookAt(target, 30, 30);
			}
		}
		if (now % 4 == 0) {
			Vfx.emit(level, isInWater() ? ParticleTypes.SQUID_INK : ParticleTypes.FALLING_WATER, position().add(0, 0.4, 0), 2, 0.4, 0.02);
		}
	}

	// ------------------------------------------------------------------ harm

	@Override
	protected float resist(ServerLevel level, DamageSource source, float damage) {
		if (shocking) {
			return damage;
		}
		if (stranded()) {
			return damage * 1.5F;
		}
		if (TideScribeRules.opening(move)) {
			// Bowed over its writing, spun dizzy or dazed on the surface, it takes more, and the water doesn't wrap it.
			return damage * (float) TideScribeRules.OPEN;
		}
		if (flooded() && isInWater()) {
			// The water wraps it: half of anything but a shock through the flood.
			Vfx.emit(level, ParticleTypes.SQUID_INK, getBoundingBox().getCenter(), 6, 0.4, 0.05);
			return damage * 0.5F;
		}
		return damage;
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	// ------------------------------------------------------------------ between phases: the tide called

	@Override
	protected void onPhase(ServerLevel level, int phase) {
		Vec3 c = position();
		Fx.sound(level, c, DungeonSounds.BOSS_PHASE, 2.0F, 1.0F);
		announce(level, "message.wildercord.tide_scribe_phase." + phase, COLOR);
		if (tide == Tide.LOW && hasArena(level)) {
			startRising(level);
		}
		for (int i = 0; i < 2; i++) {
			Mob guard = EntityTypes.DROWNED.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (guard == null) {
				continue;
			}
			double a = level.getRandom().nextDouble() * Math.PI * 2;
			Vec3 spot = Vec3.atBottomCenterOf(home).add(Math.cos(a) * 9, 0.2, Math.sin(a) * 9);
			guard.snapTo(spot.x, spot.y, spot.z, 0, 0);
			guard.finalizeSpawn(level, level.getCurrentDifficultyAt(guard.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
			Runebound.bind(guard, i == 0 ? List.of(Runes.BOLT, Runes.SHOCK) : List.of(Runes.TOUCH, Runes.FROST), phase == 3);
			level.addFreshEntity(guard);
			minion(guard);
			Vfx.emit(level, ParticleTypes.SQUID_INK, spot.add(0, 1, 0), 16, 0.4, 0.05);
			Sigils.ground(level, spot, COLOR, ACCENT, 1.2F, 30);
		}
	}

	@Override
	protected void shiftTick(ServerLevel level, int left) {
		Vec3 c = position().add(0, 1, 0);
		// A whirlpool of ink and bubbles round it as it calls the tide.
		double a = left * 0.35;
		for (int i = 0; i < 3; i++) {
			double b = a + Math.PI * 2 * i / 3;
			Vfx.emit(level, ParticleTypes.SQUID_INK, c.add(Math.cos(b) * 2.5, Math.sin(left * 0.2) * 0.5, Math.sin(b) * 2.5), 2, 0.1, 0.02);
			Vfx.emit(level, ParticleTypes.BUBBLE, c.add(Math.cos(b + 0.5) * 1.5, 0.5, Math.sin(b + 0.5) * 1.5), 2, 0.1, 0.05);
		}
		tickTide(level, level.getGameTime());
		// Its ink spirals up round it as it rises, the water boiling under it and splashing over its head.
		double h = (left % 20) / 20.0 * 3;
		double turn = left * 0.5;
		Vec3 base = position();
		Vfx.emit(level, ParticleTypes.SQUID_INK, base.add(Math.cos(turn) * 1.6, h, Math.sin(turn) * 1.6), 2, 0.05, 0.01);
		Vfx.emit(level, ParticleTypes.BUBBLE_COLUMN_UP, base.add(Math.cos(turn + Math.PI) * 1.6, h, Math.sin(turn + Math.PI) * 1.6), 2, 0.1, 0.05);
		Vfx.emit(level, ParticleTypes.SPLASH, base.add(0, 3.2, 0), 3, 0.5, 0.1);
		if (left % 10 == 0 && home != null) {
			// A star of script blazes out over the floor of its pit, and the pages of its scroll turn of themselves.
			Vec3 floor = new Vec3(home.getX() + 0.5, surface(), home.getZ() + 0.5);
			ElementFx.flatSigil(level, floor, SigilOption.STAR, ACCENT, 5.0, 12, 0.1);
			ElementFx.groundRing(level, floor, COLOR, 1.0, 7.0, 0.3, 12);
			Fx.sound(level, base, SoundEvents.BOOK_PAGE_TURN, 1.5F, 0.7F);
		}
	}

	@Override
	protected Component status() {
		if (stranded()) {
			return Component.translatable("boss.wildercord.tide_scribe_stranded").withColor(ACCENT);
		}
		String key = move > 0 && move < SPELL ? TideScribeRules.key(move) : null;
		if (key != null) {
			return Component.translatable("boss.wildercord.tide_scribe.move." + key).withColor(TideScribeRules.opening(move) ? ACCENT : COLOR);
		}
		return switch (tide) {
			case RISING -> Component.translatable("boss.wildercord.tide_rising").withColor(ACCENT);
			case HIGH -> Component.translatable("boss.wildercord.tide_high").withColor(COLOR);
			case EBBING -> Component.translatable("boss.wildercord.tide_ebbing").withColor(ACCENT);
			case LOW -> null;
		};
	}

	// ------------------------------------------------------------------ death

	@Override
	public void die(DamageSource source) {
		super.die(source);
		// The tide goes out for good.
		if (level() instanceof ServerLevel level && home != null && hasArena(level)) {
			forceTide(false);
			Fx.sound(level, Vec3.atCenterOf(home), DungeonSounds.TIDE_EBB, 2.0F, 0.8F);
		}
		// (The client runs die() too: the set of awake Scribes is the server's alone.)
		if (!level().isClientSide()) {
			AWAKE.remove(this);
		}
	}

	@Override
	public void onRemoval(RemovalReason reason) {
		if (!level().isClientSide()) {
			AWAKE.remove(this);
		}
		super.onRemoval(reason);
	}

	@Override
	protected void dying(ServerLevel level, int tick) {
		Vec3 c = getBoundingBox().getCenter();
		setDeltaMovement(0, -0.01, 0);
		if (tick == 1) {
			Fx.sound(level, c, DungeonSounds.TIDE_SCRIBE_DEATH, 2.0F, 1.0F);
		}
		if (tick % 2 == 0) {
			Vfx.emit(level, ParticleTypes.SQUID_INK, c, 4, 0.5, 0.03);
			Vfx.emit(level, ParticleTypes.BUBBLE_POP, c, 3, 0.5, 0.05);
		}
	}

	@Override
	protected void vanish(ServerLevel level) {
		Vec3 c = getBoundingBox().getCenter();
		Vfx.radial(level, ParticleTypes.SQUID_INK, c, 40, 0.2);
		Vfx.radial(level, new DustParticleOptions(ACCENT, 1.4F), c, 30, 0.3);
		Vfx.radial(level, ParticleTypes.NAUTILUS, c, 30, 0.5);
	}

	// ------------------------------------------------------------------ saved, sounds

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("tide", tide.name());
		output.putLong("tide_until", tideUntil);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		try {
			tide = Tide.valueOf(input.getStringOr("tide", "LOW"));
		} catch (IllegalArgumentException e) {
			tide = Tide.LOW;
		}
		// Mid-flood when it was saved: the pour (or the drain) picks up from the start, which is harmless.
		step = 0;
		// When the tide next turns, so a reload doesn't turn it at once; never further off than the longest wait (a clock that jumped).
		long now = level().getGameTime();
		tideUntil = Math.min(input.getLongOr("tide_until", now), now + Math.max(LOW_TICKS[1], HIGH_TICKS[1]));
	}

	/** The game time the tide next turns (low to rising, high to ebbing), for the tests. */
	public long tideTurnsAt() {
		return tideUntil;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return DungeonSounds.TIDE_SCRIBE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return DungeonSounds.TIDE_SCRIBE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return null;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 140;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		return false;
	}
}
