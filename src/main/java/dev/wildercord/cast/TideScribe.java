package dev.wildercord.cast;

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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
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

	public TideScribe(EntityType<? extends TideScribe> type, Level level) {
		super(type, level, BossEvent.BossBarColor.BLUE);
		setNoGravity(true);
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
		setState(EXPOSED, stranded());
		setState(GUARDED, flooded() && isInWater());
		// Swimming in wide, quick circles in the flood; drifting low and slow over the dry floor; held still when stranded.
		Vec3 centre = Vec3.atBottomCenterOf(home);
		if (stranded()) {
			setDeltaMovement(getDeltaMovement().scale(0.5));
		} else {
			boolean wet = flooded();
			swim += wet ? 0.03 : 0.012;
			double radius = wet ? 6.5 : 4.5;
			Vec3 goal = centre.add(Math.cos(swim) * radius, wet ? 0.4 : 1.4 + Math.sin(tickCount * 0.06) * 0.3, Math.sin(swim) * radius);
			Vec3 pull = goal.subtract(position()).scale(0.08);
			if (pull.length() > (wet ? 0.4 : 0.25)) {
				pull = pull.normalize().scale(wet ? 0.4 : 0.25);
			}
			setDeltaMovement(pull);
		}
		LivingEntity target = getTarget();
		if (target != null) {
			getLookControl().setLookAt(target, 30, 30);
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
	}

	@Override
	protected Component status() {
		if (stranded()) {
			return Component.translatable("boss.wildercord.tide_scribe_stranded").withColor(ACCENT);
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
