package dev.wildercord.cast;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Blocks a spell put up for a while (a Rampart's wall, a Span's glass, Light's light, frost's crust on
 * lava) and what each replaced, written down by chunk and saved with its dimension, as {@link Thaws} does
 * for frozen water.
 * The spells take their blocks down on time themselves, and every one still standing when the server
 * stops; this is for what they can't reach: a block whose chunk wasn't loaded when its time came
 * (never loaded just to take it down) and anything left by a server that stopped without warning (a
 * crash, a kill). Each goes back as soon as it's past its time and its chunk is loaded, but only if
 * the spell's block is still there: whatever replaced it since is left alone. One that held back lava or
 * water (frost's crust on lava) and is gone early, not by its spell (blown up), gives the fluid back at
 * once. However any of them goes, it drops nothing (see {@link #holds}).
 */
public final class TemporaryBlocks extends SavedData {
	/** One block a spell put up: the block it is, what it replaced, and the game time it goes. */
	record Placed(BlockPos pos, BlockState placed, BlockState replaced, long due) {
		static final Codec<Placed> CODEC = RecordCodecBuilder.create(i -> i.group(
			BlockPos.CODEC.fieldOf("pos").forGetter(Placed::pos),
			BlockState.CODEC.fieldOf("placed").forGetter(Placed::placed),
			BlockState.CODEC.fieldOf("replaced").forGetter(Placed::replaced),
			Codec.LONG.fieldOf("due").forGetter(Placed::due)
		).apply(i, Placed::new));
	}

	static final Codec<TemporaryBlocks> CODEC = Placed.CODEC.listOf().xmap(TemporaryBlocks::new, TemporaryBlocks::all);
	static final SavedDataType<TemporaryBlocks> TYPE = new SavedDataType<>(Wildercord.id("temporary_blocks"), TemporaryBlocks::new, CODEC, null);

	/** How often (in ticks) the written-down blocks are looked over. */
	private static final int SWEEP = 20;
	/** How long past its time a block waits for its spell to take it down before this does. */
	private static final int GRACE = 40;

	/** The blocks, by chunk (see {@link Thaws#chunk}). */
	private final Map<Long, List<Placed>> byChunk = new HashMap<>();

	public TemporaryBlocks() {
	}

	private TemporaryBlocks(List<Placed> placed) {
		placed.forEach(this::add);
	}

	private List<Placed> all() {
		List<Placed> out = new ArrayList<>();
		byChunk.values().forEach(out::addAll);
		return out;
	}

	private void add(Placed placed) {
		byChunk.computeIfAbsent(Thaws.chunk(placed.pos()), k -> new ArrayList<>()).add(placed);
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(TemporaryBlocks::tick);
	}

	/** Whether the block at {@code pos} is written down as a spell's, to go (for the tests). */
	public static boolean recorded(ServerLevel level, BlockPos pos) {
		TemporaryBlocks blocks = level.getDataStorage().get(TYPE);
		List<Placed> inChunk = blocks == null ? null : blocks.byChunk.get(Thaws.chunk(pos));
		return inChunk != null && inChunk.stream().anyMatch(p -> p.pos().equals(pos));
	}

	/**
	 * Whether {@code state} at {@code pos} is a spell's block, only there for a while: however it goes (an
	 * explosion, a piston, a player, a Wither), it drops nothing, so a Rampart is never a packed mud farm.
	 */
	public static boolean holds(ServerLevel level, BlockPos pos, BlockState state) {
		return find(level, pos, state.getBlock()) != null;
	}

	/**
	 * How a spell's {@code block} at {@code pos} is written down (what it replaced, and when it goes), or null
	 * if no spell's block of that kind is. Saved, so it still knows after a restart (a lava crust broken then
	 * drops nothing).
	 */
	static Placed find(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.Block block) {
		TemporaryBlocks blocks = level.getDataStorage().get(TYPE);
		List<Placed> inChunk = blocks == null ? null : blocks.byChunk.get(Thaws.chunk(pos));
		if (inChunk == null) {
			return null;
		}
		for (Placed placed : inChunk) {
			if (placed.pos().equals(pos) && placed.placed().is(block)) {
				return placed;
			}
		}
		return null;
	}

	/** Writes down a block a spell just put at {@code pos} in place of {@code replaced}, to go at game time {@code due}. */
	public static void put(ServerLevel level, BlockPos pos, BlockState placed, BlockState replaced, long due) {
		TemporaryBlocks blocks = level.getDataStorage().computeIfAbsent(TYPE);
		BlockPos at = pos.immutable();
		blocks.drop(at);
		blocks.add(new Placed(at, placed, replaced, due));
		blocks.setDirty();
	}

	/** Its spell took the block at {@code pos} down (or it was broken): nothing is left to do for it. */
	static void remove(ServerLevel level, BlockPos pos) {
		TemporaryBlocks blocks = level.getDataStorage().get(TYPE);
		if (blocks != null && blocks.drop(pos)) {
			blocks.setDirty();
		}
	}

	private boolean drop(BlockPos pos) {
		List<Placed> inChunk = byChunk.get(Thaws.chunk(pos));
		if (inChunk == null || !inChunk.removeIf(p -> p.pos().equals(pos))) {
			return false;
		}
		if (inChunk.isEmpty()) {
			byChunk.remove(Thaws.chunk(pos));
		}
		return true;
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % SWEEP != 0) {
			return;
		}
		for (ServerLevel level : server.getAllLevels()) {
			TemporaryBlocks blocks = level.getDataStorage().get(TYPE);
			if (blocks != null && !blocks.byChunk.isEmpty()) {
				blocks.sweep(level);
			}
		}
	}

	/** Takes down every block well past its time, in the chunks that are loaded; the rest wait. */
	private void sweep(ServerLevel level) {
		long now = level.getGameTime();
		boolean changed = false;
		Iterator<Map.Entry<Long, List<Placed>>> chunks = byChunk.entrySet().iterator();
		while (chunks.hasNext()) {
			Map.Entry<Long, List<Placed>> chunk = chunks.next();
			Iterator<Placed> it = chunk.getValue().iterator();
			while (it.hasNext()) {
				Placed placed = it.next();
				if (!level.isLoaded(placed.pos())) {
					continue;
				}
				BlockState standing = level.getBlockState(placed.pos());
				if (placed.due() + GRACE > now) {
					// Gone early, and not by its spell (an explosion, say): the lava or water it held back comes back
					// into the gap at once, rather than never.
					if (!standing.equals(placed.placed()) && standing.canBeReplaced() && !placed.replaced().getFluidState().isEmpty()) {
						level.setBlockAndUpdate(placed.pos(), placed.replaced());
						it.remove();
						changed = true;
					}
					continue;
				}
				if (standing.equals(placed.placed())) {
					level.setBlockAndUpdate(placed.pos(), placed.replaced());
				}
				it.remove();
				changed = true;
			}
			if (chunk.getValue().isEmpty()) {
				chunks.remove();
			}
		}
		if (changed) {
			setDirty();
		}
	}
}
