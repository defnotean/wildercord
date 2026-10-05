package dev.wildercord.cast;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.config.Config;
import dev.wildercord.content.MaterialOption;
import dev.wildercord.content.ResidueBlock;
import dev.wildercord.content.ResidueBlocks;
import dev.wildercord.spell.SpellPlan;
import dev.wildercord.world.ResidueLedger;
import dev.wildercord.world.ResidueRules;
import dev.wildercord.world.ResidueRules.Kind;
import dev.wildercord.world.ResidueRules.Placement;
import dev.wildercord.world.ResidueRules.Refusal;
import dev.wildercord.world.ResidueRules.Site;
import dev.wildercord.world.ResidueRules.Source;
import dev.wildercord.world.ResidueRules.Spot;
import dev.wildercord.world.dungeons.DungeonWards;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Residues at runtime: big magic leaves a mark of its element where it lands (see {@link ResidueRules} for the
 * rules and their numbers). This finds the ground for it, asks the same permission any spell's block change
 * does, puts the blocks down, and keeps a record of each with its dimension ({@link ResidueLedger}, saved), so
 * every one fades on time, after a restart too, and gives back the ground it took.
 *
 * <p>It never looks at every residue: each fades from a schedule ordered by time, a few at most per second,
 * and one whose chunk isn't loaded waits for that chunk to load (never loaded for it), as the terrain spells'
 * own blocks do ({@link TemporaryBlocks}). Ambience and behaviour are the blocks' own
 * ({@link dev.wildercord.content.ResidueBlock}).</p>
 *
 * <p>Other features leave residues through {@link #leave} (a mastery trait, say), and can ask what's here with
 * {@link #kindAt}.</p>
 */
public final class Residues {
	private Residues() {}

	/** Natural ground a residue may take or lie on: {@code #wildercord:residue_ground}. */
	public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, Wildercord.id("residue_ground"));

	/** How often (in ticks) the schedule is read. */
	private static final int SWEEP = 20;
	/** The most residues faded in one sweep: the rest wait for the next one. */
	private static final int PER_SWEEP = 64;

	/** What the record keeps beside each residue: the block put down and the block it took the place of. */
	public record Blocks(BlockState placed, BlockState replaced) {}

	/** One residue as it's saved. */
	private record Row(BlockPos pos, String kind, long placedAt, long due, int generation, String owner, BlockState placed, BlockState replaced) {
		static final Codec<Row> CODEC = RecordCodecBuilder.create(i -> i.group(
			BlockPos.CODEC.fieldOf("pos").forGetter(Row::pos),
			Codec.STRING.fieldOf("kind").forGetter(Row::kind),
			Codec.LONG.fieldOf("placed_at").forGetter(Row::placedAt),
			Codec.LONG.fieldOf("due").forGetter(Row::due),
			Codec.INT.optionalFieldOf("generation", 0).forGetter(Row::generation),
			Codec.STRING.optionalFieldOf("owner", "").forGetter(Row::owner),
			BlockState.CODEC.fieldOf("placed").forGetter(Row::placed),
			BlockState.CODEC.fieldOf("replaced").forGetter(Row::replaced)
		).apply(i, Row::new));
	}

	/** One dimension's residues, saved with it. */
	static final class Record extends SavedData {
		static final Codec<Record> CODEC = Row.CODEC.listOf().xmap(Record::new, Record::rows);
		static final SavedDataType<Record> TYPE = new SavedDataType<>(Wildercord.id("residues"), Record::new, CODEC, null);

		final ResidueLedger<Blocks> ledger = new ResidueLedger<>();

		Record() {
		}

		private Record(List<Row> rows) {
			for (Row row : rows) {
				BlockPos p = row.pos();
				ledger.add(new ResidueLedger.Entry<>(p.getX(), p.getY(), p.getZ(), row.kind(), row.placedAt(), row.due(), row.generation(), row.owner(),
					new Blocks(row.placed(), row.replaced())));
			}
		}

		private List<Row> rows() {
			List<Row> rows = new ArrayList<>();
			for (ResidueLedger.Entry<Blocks> e : ledger.all()) {
				rows.add(new Row(new BlockPos(e.x(), e.y(), e.z()), e.kind(), e.placedAt(), e.due(), e.generation(), e.owner(), e.data().placed(),
					e.data().replaced()));
			}
			return rows;
		}
	}

	/** When each caster last left a residue (game time), so a fight leaves marks rather than a carpet. */
	private static final Map<UUID, Long> RESTED = new HashMap<>();
	/** Accessibility wake-ups, in arrival order; incomplete full-chunk futures wait for a later sweep. */
	private static final Map<ResourceKey<Level>, Set<Long>> LOADED = new HashMap<>();
	/** Residue blocks that went some other way (an explosion, a piston, water) this tick: their records go at its end. */
	private static final Map<ResourceKey<Level>, Set<BlockPos>> GONE = new HashMap<>();
	/** The chance a reaction leaves a residue; tests may pin it (see {@link #reactionChance(double)}). */
	private static double reactionChance = ResidueRules.REACTION_CHANCE;

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Residues::tick);
		ServerChunkEvents.FULL_CHUNK_STATUS_CHANGE.register((level, chunk, oldStatus, newStatus) -> {
			// A resident chunk can become accessible again without another CHUNK_LOAD event.
			// Its full-chunk future may still be incomplete here: only queue, never read blocks.
			if (oldStatus != FullChunkStatus.INACCESSIBLE || !newStatus.isOrAfter(FullChunkStatus.FULL)) {
				return;
			}
			Record record = level.getDataStorage().get(Record.TYPE);
			long key = chunk.getPos().pack();
			if (record != null && record.ledger.inChunk(key) > 0) {
				// Include scheduled records: one may become due before this future completes.
				LOADED.computeIfAbsent(level.dimension(), k -> new LinkedHashSet<>()).add(key);
			}
		});
		// Harvested by a player: its reagent drops (the block's loot table), and the ground it took comes back.
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (world instanceof ServerLevel level && ResidueBlocks.is(state)) {
				harvested(level, pos, state, player instanceof ServerPlayer sp ? sp : null);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			RESTED.clear();
			LOADED.clear();
			GONE.clear();
			reactionChance = ResidueRules.REACTION_CHANCE;
		});
	}

	// ------------------------------------------------------------------ what leaves one

	/**
	 * Called after each effect a spell applies (see {@code Effects.apply}): a strong enough spell of a player's (or
	 * a boss's) leaves its element's residue where it landed, once a cast.
	 */
	static void onSpell(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit) {
		if (cast.passive || !Config.get().residues().enabled()) {
			return;
		}
		Optional<Kind> kind = ResidueRules.kindFor(node.effect.element());
		Source source = sourceOf(cast.caster);
		if (kind.isEmpty() || source == null) {
			return;
		}
		double strength = ResidueRules.strength(source, cast.weight(), Config.get().residues().minSpellCost(), cast.overcast());
		if (strength <= 0 || !rested(cast.caster, source, cast.level.getGameTime()) || !cast.once("residue")) {
			return;
		}
		BlockPos impact = hit.block() != null ? hit.block() : BlockPos.containing(hit.point());
		if (leave(cast.level, kind.get(), impact, strength, cast.caster, source) > 0) {
			RESTED.put(cast.caster.getUUID(), cast.level.getGameTime());
		}
	}

	/** Called as an element reaction goes off on {@code target}: now and then it leaves a small residue of {@code element} there. */
	static void reaction(Cast cast, String element, Entity target) {
		reaction(cast, element, target.position());
	}

	/** As {@link #reaction(Cast, String, Entity)}, for a reaction that goes off at a place (Implode). */
	static void reaction(Cast cast, String element, Vec3 at) {
		if (cast.passive || !Config.get().residues().enabled() || sourceOf(cast.caster) == null) {
			return;
		}
		Optional<Kind> kind = ResidueRules.kindFor(element);
		long now = cast.level.getGameTime();
		if (kind.isEmpty() || cast.level.getRandom().nextDouble() >= reactionChance || !rested(cast.caster, Source.REACTION, now)) {
			return;
		}
		if (leave(cast.level, kind.get(), BlockPos.containing(at), ResidueRules.REACTION_STRENGTH, cast.caster, Source.REACTION) > 0) {
			RESTED.put(cast.caster.getUUID(), now);
		}
	}

	/** A player's magic, or a boss's (the Riftcaller, the Archivist, a dungeon's master): nobody else's leaves residues. */
	private static @Nullable Source sourceOf(LivingEntity caster) {
		if (caster instanceof ServerPlayer) {
			return Source.SPELL;
		}
		if (caster instanceof DungeonBoss || caster instanceof Archivist || caster.entityTags().contains(dev.wildercord.cast.events.RiftSiege.RIFTCALLER_TAG)) {
			return Source.BOSS;
		}
		return null;
	}

	private static boolean rested(LivingEntity caster, Source source, long now) {
		Long last = RESTED.get(caster.getUUID());
		return last == null || now - last >= ResidueRules.rest(source) || last > now;
	}

	// ------------------------------------------------------------------ leaving one (the hook other features call)

	/**
	 * Leaves a residue of {@code element} at {@code at}: the hook for anything else that wants magic to mark the
	 * world (a mastery trait, an event). {@code strength} 1 is a spell at the threshold (one block), 3 the most
	 * (five blocks, lasting half again as long). {@code by} is whose magic it is: a player's (their permission
	 * and their share of the caps), or null for the world's own (as a boss's: it then follows
	 * {@code spells_edit_blocks} and the mob-griefing rule). Every rule holds: natural ground only, never in a
	 * ward, the caps. Returns how many blocks it left (0 for none, or with residues switched off).
	 */
	public static int leave(ServerLevel level, String element, Vec3 at, double strength, @Nullable LivingEntity by) {
		Optional<Kind> kind = ResidueRules.kindFor(element);
		if (kind.isEmpty() || strength < 1.0 || !Config.get().residues().enabled()) {
			return 0;
		}
		return leave(level, kind.get(), BlockPos.containing(at), Math.min(strength, ResidueRules.MAX_STRENGTH), by,
			by instanceof ServerPlayer || by == null ? Source.SPELL : Source.BOSS);
	}

	private static int leave(ServerLevel level, Kind kind, BlockPos impact, double strength, @Nullable LivingEntity by, Source source) {
		dev.wildercord.config.WildercordConfig.ResidueSettings cfg = Config.get().residues();
		if (!cfg.enabled()) {
			return 0;
		}
		Record record = level.getDataStorage().computeIfAbsent(Record.TYPE);
		ResidueLedger.Caps caps = new ResidueLedger.Caps(cfg.maxPerChunk(), ResidueRules.PER_AREA, ResidueRules.AREA_RADIUS, cfg.maxPerDimension(),
			ResidueRules.PER_OWNER);
		String owner = by instanceof ServerPlayer player ? player.getUUID().toString() : "";
		int wanted = ResidueRules.cells(strength);
		int reach = ResidueRules.spread(strength);
		long now = level.getGameTime();
		long due = now + ResidueRules.lifetime(kind, strength, cfg.lifetimeMultiplier());
		// The column it landed in first, then the rest nearest first (shuffled among equals, so marks scatter naturally).
		List<int[]> columns = new ArrayList<>();
		RandomSource random = level.getRandom();
		for (int dx = -reach; dx <= reach; dx++) {
			for (int dz = -reach; dz <= reach; dz++) {
				if (dx * dx + dz * dz <= reach * reach + 1) {
					columns.add(new int[] {dx, dz, dx * dx + dz * dz, random.nextInt(1000)});
				}
			}
		}
		columns.sort(Comparator.<int[]>comparingInt(c -> c[2]).thenComparingInt(c -> c[3]));
		int left = 0;
		for (int[] column : columns) {
			if (left >= wanted) {
				break;
			}
			BlockPos spot = groundFor(level, kind, impact.offset(column[0], 0, column[1]));
			if (spot == null || judge(level, kind, spot, by) != null || record.ledger.admit(spot.getX(), spot.getY(), spot.getZ(), owner, caps) != null) {
				continue;
			}
			put(level, record, kind, spot, now, due, 0, owner);
			left++;
		}
		if (left > 0) {
			answer(level, kind, Vec3.atCenterOf(impact));
		}
		return left;
	}

	/**
	 * Where in this column a residue of {@code kind} would go: the first solid thing from a little above down (the
	 * surface), or the space above it for one that lies on the ground; null if the column has no surface near.
	 */
	private static @Nullable BlockPos groundFor(ServerLevel level, Kind kind, BlockPos column) {
		for (int dy = 2; dy >= -4; dy--) {
			BlockPos p = column.offset(0, dy, 0);
			if (!level.isLoaded(p)) {
				return null;
			}
			if (spot(level, p, level.getBlockState(p)) == Spot.OPEN) {
				continue;
			}
			return kind.placement == Placement.COVER ? p : p.above();
		}
		return null;
	}

	/** Why a residue of {@code kind} can't go at {@code pos} for {@code by}, or null if it can (see {@link ResidueRules#judge}). */
	public static @Nullable Refusal judge(ServerLevel level, Kind kind, BlockPos pos, @Nullable LivingEntity by) {
		boolean loaded = level.isLoaded(pos) && level.isLoaded(pos.below()) && level.isLoaded(pos.above());
		if (!loaded) {
			return Refusal.UNLOADED;
		}
		BlockState here = level.getBlockState(pos);
		BlockPos below = pos.below();
		BlockState under = level.getBlockState(below);
		Record record = level.getDataStorage().get(Record.TYPE);
		boolean temporary = Effects.isTemporary(level, pos) || record != null && record.ledger.get(pos.asLong()) != null;
		Site site = new Site(spot(level, pos, here), spot(level, below, under), spot(level, pos.above(), level.getBlockState(pos.above())),
			under.isFaceSturdy(level, below, Direction.UP), temporary, level.getBlockEntity(pos) != null, true, false, true,
			level.getWorldBorder().isWithinBounds(pos) && !level.isOutsideBuildHeight(pos));
		Refusal refusal = ResidueRules.judge(kind.placement, site);
		if (refusal != null) {
			return refusal;
		}
		// Asked only now, for a block it could really take: the wards, then permission (claims hear the question as a break).
		if (DungeonWards.warded(level, pos)) {
			return Refusal.WARDED;
		}
		return permitted(level, pos, by) ? null : Refusal.PROTECTED;
	}

	/** What a block is to a residue: natural ground, open air (or something as easily replaced that grew there), or anything else. */
	private static Spot spot(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.is(GROUND) && !state.hasBlockEntity()) {
			return Spot.NATURAL;
		}
		if (state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty() && !state.hasBlockEntity() && !ResidueBlocks.is(state)) {
			return Spot.OPEN;
		}
		return Spot.OTHER;
	}

	/** Whether whoever's magic it is may change this block: a player as any spell of theirs may, the world's own by its rules. */
	private static boolean permitted(ServerLevel level, BlockPos pos, @Nullable LivingEntity by) {
		if (by instanceof ServerPlayer player) {
			return Casters.mayEdit(player, level, pos);
		}
		return Config.get().spellsEditBlocks() && level.getGameRules().get(GameRules.MOB_GRIEFING);
	}

	private static void put(ServerLevel level, Record record, Kind kind, BlockPos pos, long now, long due, int generation, String owner) {
		BlockState replaced = level.getBlockState(pos);
		BlockState placed = ResidueBlocks.of(kind).defaultBlockState();
		level.setBlock(pos, placed, Block.UPDATE_ALL);
		record.ledger.add(new ResidueLedger.Entry<>(pos.getX(), pos.getY(), pos.getZ(), kind.path, now, due, generation, owner, new Blocks(placed, replaced)));
		record.setDirty();
	}

	/** The world answering: a breath of the element where its residue settles, and its sound. */
	private static void answer(ServerLevel level, Kind kind, Vec3 at) {
		int style = switch (kind) {
			case SMOULDERING_ASH -> MaterialOption.EMBER;
			case EVERFROST -> MaterialOption.FROST;
			case FULGURITE -> MaterialOption.STORM;
			case LINGERING_EDDY -> MaterialOption.WIND;
			case RIVEN_STONE -> MaterialOption.STONE;
			case WILDBLOOM -> MaterialOption.PETAL;
			case VOID_SCAR -> MaterialOption.VOID;
			case STAR_GLYPH -> MaterialOption.ARCANE;
			case STILLED_SAND -> MaterialOption.TIME;
			case BLOODMOSS -> MaterialOption.BLOOD;
		};
		Fx.send(level, new MaterialOption(style, Vfx.theme(kind.element).primary(), 0.16F, 40), at.add(0, 0.6, 0), 10, 0.6, 0.02);
		dev.wildercord.cast.feel.Feels.sound(level, at, settleSound(kind), 0.7F, 1.0F);
	}

	/** Each residue's own quiet sound, played louder as it settles. */
	private static String settleSound(Kind kind) {
		return switch (kind) {
			case SMOULDERING_ASH -> "fire_smoulder";
			case EVERFROST -> "frost_glint";
			case FULGURITE -> "storm_fizz";
			case LINGERING_EDDY -> "wind_lingering_eddy";
			case RIVEN_STONE -> "earth_rumble";
			case WILDBLOOM -> "life_bloom";
			case VOID_SCAR -> "void_hum";
			case STAR_GLYPH -> "arcane_glyph";
			case STILLED_SAND -> "time_trickle";
			case BLOODMOSS -> "blood_pulse";
		};
	}

	// ------------------------------------------------------------------ what's here

	/** The residue at {@code pos}, if a residue's record says one stands there. */
	public static Optional<Kind> kindAt(ServerLevel level, BlockPos pos) {
		ResidueLedger.Entry<Blocks> entry = entry(level, pos);
		return entry == null ? Optional.empty() : Kind.byPath(entry.kind());
	}

	public static boolean isResidue(ServerLevel level, BlockPos pos) {
		return entry(level, pos) != null;
	}

	/** How many residues a dimension holds. */
	public static int count(ServerLevel level) {
		Record record = level.getDataStorage().get(Record.TYPE);
		return record == null ? 0 : record.ledger.size();
	}

	/** When the residue at {@code pos} fades (game time), or -1. */
	public static long dueAt(ServerLevel level, BlockPos pos) {
		ResidueLedger.Entry<Blocks> entry = entry(level, pos);
		return entry == null ? -1 : entry.due();
	}

	private static ResidueLedger.@Nullable Entry<Blocks> entry(ServerLevel level, BlockPos pos) {
		Record record = level.getDataStorage().get(Record.TYPE);
		return record == null ? null : record.ledger.get(pos.asLong());
	}

	/** The stage a void scar is at as it closes (0 to 3), from its record. */
	public static int stage(ServerLevel level, BlockPos pos) {
		ResidueLedger.Entry<Blocks> entry = entry(level, pos);
		return entry == null ? 0 : ResidueRules.stage(entry.placedAt(), entry.due(), level.getGameTime());
	}

	// ------------------------------------------------------------------ how they go

	/** A residue was harvested (broken by a player, or bottled): its record goes, and the ground it took comes back. */
	public static void harvested(ServerLevel level, BlockPos pos, BlockState state, @Nullable ServerPlayer by) {
		Record record = level.getDataStorage().get(Record.TYPE);
		ResidueLedger.Entry<Blocks> entry = record == null ? null : record.ledger.remove(pos.asLong());
		if (entry == null) {
			return;
		}
		record.setDirty();
		if (by != null) {
			Grimoire.feat(by, dev.wildercord.spell.Feats.RESIDUE);
		}
		Kind kind = ResidueBlocks.kindOf(state);
		if (kind != null && kind.placement == Placement.COVER && level.getBlockState(pos).isAir()) {
			level.setBlock(pos, entry.data().replaced(), Block.UPDATE_ALL);
		}
	}

	/** A residue block went some other way (an explosion, water, a piston): its record goes at the end of the tick. */
	public static void gone(ServerLevel level, BlockPos pos) {
		GONE.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.immutable());
	}

	/**
	 * A wildbloom's random tick: now and then it seeds a seedling on natural soil nearby, a few generations at
	 * most, with its owner's permission (only while they're on the server), never outliving it.
	 */
	public static void seed(ServerLevel level, BlockPos pos, RandomSource random) {
		Record record = level.getDataStorage().get(Record.TYPE);
		ResidueLedger.Entry<Blocks> parent = record == null ? null : record.ledger.get(pos.asLong());
		if (parent == null || parent.generation() >= ResidueRules.BLOOM_GENERATIONS || random.nextInt(4) != 0 || !Config.get().residues().enabled()) {
			return;
		}
		LivingEntity owner = null;
		if (!parent.owner().isEmpty()) {
			owner = level.getServer().getPlayerList().getPlayer(UUID.fromString(parent.owner()));
			if (owner == null) {
				return;
			}
		}
		int reach = ResidueRules.BLOOM_REACH;
		BlockPos spot = groundFor(level, Kind.WILDBLOOM, pos.offset(random.nextInt(reach * 2 + 1) - reach, 0, random.nextInt(reach * 2 + 1) - reach));
		if (spot == null || !level.getBlockState(spot.below()).is(BlockTags.SUBSTRATE_OVERWORLD) || judge(level, Kind.WILDBLOOM, spot, owner) != null) {
			return;
		}
		dev.wildercord.config.WildercordConfig.ResidueSettings cfg = Config.get().residues();
		ResidueLedger.Caps caps = new ResidueLedger.Caps(cfg.maxPerChunk(), ResidueRules.PER_AREA, ResidueRules.AREA_RADIUS, cfg.maxPerDimension(),
			ResidueRules.PER_OWNER);
		if (record.ledger.admit(spot.getX(), spot.getY(), spot.getZ(), parent.owner(), caps) != null) {
			return;
		}
		long now = level.getGameTime();
		long due = ResidueRules.seedlingDue(parent.due(), now, ResidueRules.lifetime(Kind.WILDBLOOM, 1.0, cfg.lifetimeMultiplier()));
		put(level, record, Kind.WILDBLOOM, spot, now, due, parent.generation() + 1, parent.owner());
		level.sendParticles(new MaterialOption(MaterialOption.PETAL, 0xFFB0E0, 0.1F, 40), spot.getX() + 0.5, spot.getY() + 0.4, spot.getZ() + 0.5, 6, 0.3, 0.2, 0.3,
			0.01);
	}

	private static void tick(MinecraftServer server) {
		if (!GONE.isEmpty()) {
			for (Map.Entry<ResourceKey<Level>, Set<BlockPos>> gone : GONE.entrySet()) {
				ServerLevel level = server.getLevel(gone.getKey());
				Record record = level == null ? null : level.getDataStorage().get(Record.TYPE);
				if (record == null) {
					continue;
				}
				for (BlockPos pos : gone.getValue()) {
					if (!ResidueBlocks.is(level.getBlockState(pos)) && record.ledger.remove(pos.asLong()) != null) {
						record.setDirty();
					}
				}
			}
			GONE.clear();
		}
		if (server.getTickCount() % SWEEP != 0) {
			return;
		}
		for (ServerLevel level : server.getAllLevels()) {
			Record record = level.getDataStorage().get(Record.TYPE);
			if (record == null || record.ledger.isEmpty()) {
				continue;
			}
			sweep(level, record);
		}
	}

	/** Fades every residue whose time has come in loaded ground (a few dozen at most); those in unloaded ground wait for it. */
	private static void sweep(ServerLevel level, Record record) {
		long now = level.getGameTime();
		int remaining = PER_SWEEP;
		Set<Long> loaded = LOADED.get(level.dimension());
		if (loaded != null) {
			int toCheck = Math.min(loaded.size(), PER_SWEEP);
			for (int checked = 0; checked < toCheck && !loaded.isEmpty() && remaining > 0; checked++) {
				// Remove before fading: block updates can enqueue more chunks without invalidating an iterator.
				Iterator<Long> chunks = loaded.iterator();
				long chunk = chunks.next();
				chunks.remove();
				if (record.ledger.inChunk(chunk) == 0) {
					continue;
				}
				if (!accessible(level, chunk)) {
					// Loading may still be pending, or it became inaccessible again. Keep the records.
					loaded.add(chunk);
					continue;
				}
				for (ResidueLedger.Entry<Blocks> entry : record.ledger.unpark(chunk, remaining)) {
					fade(level, record, entry);
					remaining--;
				}
				if (record.ledger.hasParked(chunk)) {
					loaded.add(chunk);
				}
			}
			if (loaded.isEmpty()) {
				LOADED.remove(level.dimension());
			}
		}
		if (remaining == 0 || record.ledger.nextDue() > now) {
			return;
		}
		for (ResidueLedger.Entry<Blocks> entry : record.ledger.takeDue(now, remaining)) {
			if (accessible(level, entry.chunk())) {
				fade(level, record, entry);
			} else {
				record.ledger.park(entry);
			}
		}
	}

	/** Checks the completed full-chunk future without loading or generating a chunk. */
	private static boolean accessible(ServerLevel level, long chunk) {
		return level.getChunkSource().getChunkNow((int) chunk, (int) (chunk >> 32)) != null;
	}

	/** One residue fades: the ground it took (or the air) comes back, if the residue is still what stands there. */
	private static void fade(ServerLevel level, Record record, ResidueLedger.Entry<Blocks> entry) {
		record.ledger.remove(entry.pos());
		record.setDirty();
		BlockPos pos = new BlockPos(entry.x(), entry.y(), entry.z());
		BlockState standing = level.getBlockState(pos);
		if (standing.is(entry.data().placed().getBlock())) {
			level.setBlock(pos, entry.data().replaced(), Block.UPDATE_ALL);
			Kind kind = ResidueBlocks.kindOf(standing);
			if (kind != null) {
				level.sendParticles(new MaterialOption(kind == Kind.VOID_SCAR ? MaterialOption.VOID : MaterialOption.VAPOUR, Vfx.theme(kind.element).primary(), 0.14F,
					30), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.2, 0.3, 0.01);
			}
		}
	}

	// ------------------------------------------------------------------ for tests and operators

	/** Moves every residue in a dimension {@code ticks} on, as if that much time had passed for them. */
	public static void fastForward(ServerLevel level, long ticks) {
		Record record = level.getDataStorage().get(Record.TYPE);
		if (record != null) {
			record.ledger.shift(ticks);
			record.setDirty();
		}
	}

	/** Forgets when each caster last left a residue, so the next strong spell leaves one at once. */
	public static void clearRests() {
		RESTED.clear();
	}

	/** Pins the chance a reaction leaves a residue (tests), or puts it back with a negative number. */
	public static void reactionChance(double chance) {
		reactionChance = chance < 0 ? ResidueRules.REACTION_CHANCE : chance;
	}

	/** Every residue position in a dimension (tests, debugging). */
	public static List<BlockPos> all(ServerLevel level) {
		Record record = level.getDataStorage().get(Record.TYPE);
		List<BlockPos> out = new ArrayList<>();
		if (record != null) {
			record.ledger.all().forEach(e -> out.add(new BlockPos(e.x(), e.y(), e.z())));
		}
		return out;
	}

	/** Whether {@code state} is any residue's block (a convenience for other features). */
	public static boolean isResidueBlock(BlockState state) {
		return state.getBlock() instanceof ResidueBlock;
	}
}
