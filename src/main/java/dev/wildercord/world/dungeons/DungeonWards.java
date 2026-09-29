package dev.wildercord.world.dungeons;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import dev.wildercord.cast.TemporaryBlocks;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The dungeons' boss arenas and vaults are warded: nobody tunnels, blasts or spells their way in from outside, so the
 * fight and its reward are reached through the dungeon. The halls on the way may still be dug into. Inside a ward,
 * what isn't part of the dungeon can still go: something a player put there (a torch, a block to stand on), a spell's
 * temporary block, or anything easily replaced (grass, water). Players in creative mode aren't stopped.
 *
 * <p>The warded rooms come from the dungeon pieces themselves ({@link WardedPiece}): each piece writes its rooms down as
 * it's built (so a dungeon put down with {@code /place} is warded too), and the world's own record of which structures
 * stand where covers dungeons built before the ward existed.</p>
 */
public final class DungeonWards extends SavedData {
	/** Blocks players placed inside a ward, by position: these they may break again. */
	private final LongSet placed = new LongOpenHashSet();
	/** The warded rooms of the dungeons built in this world, as they were built. */
	private final List<BoundingBox> rooms = new ArrayList<>();

	private static final Codec<DungeonWards> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
		Codec.LONG.listOf().optionalFieldOf("placed", List.of()).forGetter(w -> new ArrayList<>(w.placed)),
		BoundingBox.CODEC.listOf().optionalFieldOf("rooms", List.of()).forGetter(w -> w.rooms)
	).apply(i, DungeonWards::new));
	private static final SavedDataType<DungeonWards> TYPE = new SavedDataType<>(Wildercord.id("dungeon_wards"), DungeonWards::new, CODEC, null);

	/**
	 * Rooms written down by pieces being built, not yet in their world's record. Pieces are built wherever chunks
	 * generate (not always on the server's thread), so they're handed over here and filed on the next tick.
	 */
	private static final java.util.Queue<Map.Entry<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, List<BoundingBox>>> BUILT =
		new java.util.concurrent.ConcurrentLinkedQueue<>();

	/** When each player was last told a wall is warded, so digging at one doesn't flood their screen. */
	private static final Map<UUID, Long> TOLD = new HashMap<>();

	public DungeonWards() {
	}

	private DungeonWards(List<Long> placed, List<BoundingBox> rooms) {
		this.placed.addAll(placed);
		this.rooms.addAll(rooms);
	}

	/** A warded piece being built (in some chunk): its rooms go into its world's record. */
	public static void remember(net.minecraft.world.level.WorldGenLevel level, WardedPiece piece) {
		BUILT.add(Map.entry(level.getLevel().dimension(), piece.wardedBoxes()));
	}

	public static void init() {
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			if (!(world instanceof ServerLevel level) || player.isCreative() || state.canBeReplaced() || !warded(level, pos)) {
				return true;
			}
			if (TemporaryBlocks.recorded(level, pos) || forgetPlaced(level, pos)) {
				return true;
			}
			refuse(level, player, pos);
			return false;
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (Map.Entry<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, List<BoundingBox>> built; (built = BUILT.poll()) != null; ) {
				ServerLevel level = server.getLevel(built.getKey());
				if (level == null) {
					continue;
				}
				DungeonWards wards = level.getDataStorage().computeIfAbsent(TYPE);
				for (BoundingBox room : built.getValue()) {
					if (!wards.rooms.contains(room)) {
						wards.rooms.add(room);
						wards.setDirty();
					}
				}
			}
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			TOLD.clear();
			BUILT.clear();
		});
	}

	/** Whether {@code pos} is inside a dungeon's warded room (its boss arena or vault). */
	public static boolean warded(ServerLevel level, BlockPos pos) {
		DungeonWards wards = level.getDataStorage().get(TYPE);
		if (wards != null) {
			for (BoundingBox room : wards.rooms) {
				if (room.isInside(pos)) {
					return true;
				}
			}
		}
		for (BoundingBox box : boxesAt(level, pos)) {
			if (box.isInside(pos)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Every warded room of the dungeons whose pieces reach the chunk at {@code pos}, or the chunks within
	 * {@code chunkRadius} of it (an explosion asks once for the whole blast).
	 */
	public static List<BoundingBox> boxesNear(ServerLevel level, BlockPos pos, int chunkRadius) {
		List<BoundingBox> boxes = new ArrayList<>();
		DungeonWards wards = level.getDataStorage().get(TYPE);
		if (wards != null) {
			BoundingBox area = new BoundingBox(pos).inflatedBy(16 * chunkRadius + 16);
			for (BoundingBox room : wards.rooms) {
				if (room.intersects(area)) {
					boxes.add(room);
				}
			}
		}
		ChunkPos centre = ChunkPos.containing(pos);
		for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
			for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
				BlockPos at = new BlockPos(((centre.x() + dx) << 4) + 8, pos.getY(), ((centre.z() + dz) << 4) + 8);
				if (!level.hasChunkAt(at)) {
					continue;
				}
				for (BoundingBox box : boxesAt(level, at)) {
					if (!boxes.contains(box)) {
						boxes.add(box);
					}
				}
			}
		}
		return boxes;
	}

	private static List<BoundingBox> boxesAt(ServerLevel level, BlockPos pos) {
		List<BoundingBox> boxes = new ArrayList<>();
		for (Structure structure : level.structureManager().getAllStructuresAt(pos).keySet()) {
			StructureStart start = level.structureManager().getStructureAt(pos, structure);
			if (!start.isValid()) {
				// Only whole-structure starts that reach this column are worth asking about.
				start = findStart(level, pos, structure);
				if (start == null) {
					continue;
				}
			}
			for (StructurePiece piece : start.getPieces()) {
				if (piece instanceof WardedPiece warded) {
					boxes.addAll(warded.wardedBoxes());
				}
			}
		}
		return boxes;
	}

	/** The start of {@code structure} whose bounds reach {@code pos}'s column, even if {@code pos} is above or below it. */
	private static StructureStart findStart(ServerLevel level, BlockPos pos, Structure structure) {
		ChunkPos chunk = ChunkPos.containing(pos);
		for (StructureStart start : level.structureManager().startsForStructure(chunk.x(), chunk.z(), structure)) {
			BoundingBox bb = start.getBoundingBox();
			if (start.isValid() && pos.getX() >= bb.minX() && pos.getX() <= bb.maxX() && pos.getZ() >= bb.minZ() && pos.getZ() <= bb.maxZ()) {
				return start;
			}
		}
		return null;
	}

	/** A player put a block down at {@code pos}: inside a ward, remember it's theirs to break again. */
	public static void placedBy(ServerLevel level, Player player, BlockPos pos) {
		if (player == null || player.isCreative() || !warded(level, pos)) {
			return;
		}
		DungeonWards wards = level.getDataStorage().computeIfAbsent(TYPE);
		if (wards.placed.add(pos.asLong())) {
			wards.setDirty();
		}
	}

	/** Whether a player put the block at {@code pos} down (and forgets it: it's being broken). */
	public static boolean forgetPlaced(ServerLevel level, BlockPos pos) {
		DungeonWards wards = level.getDataStorage().get(TYPE);
		if (wards != null && wards.placed.remove(pos.asLong())) {
			wards.setDirty();
			return true;
		}
		return false;
	}

	/** Whether a player put the block at {@code pos} down (without forgetting it). */
	public static boolean placedHere(ServerLevel level, BlockPos pos) {
		DungeonWards wards = level.getDataStorage().get(TYPE);
		return wards != null && wards.placed.contains(pos.asLong());
	}

	private static void refuse(ServerLevel level, Player player, BlockPos pos) {
		level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.35, 0.35, 0.35, 0.4);
		long now = level.getGameTime();
		Long last = TOLD.get(player.getUUID());
		if (last != null && now - last < 30 && now >= last) {
			return;
		}
		TOLD.put(player.getUUID(), now);
		level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.6F);
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendOverlayMessage(Component.translatableWithFallback("message.wildercord.warded",
				"These walls are warded: the only way in is through the dungeon").withColor(0xB48CFF));
		}
	}
}
