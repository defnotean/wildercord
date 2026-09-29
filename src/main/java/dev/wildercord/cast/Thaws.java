package dev.wildercord.cast;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Frosted ice a frost spell froze, waiting to thaw back into water (see {@link WorldMagic}). Vanilla
 * frosted ice melts by itself only in the light, so every block a spell freezes is written down here,
 * by chunk, and saved with its dimension: it thaws when its time comes, or, if its chunk isn't loaded
 * then (or the server stopped in between), as soon as the chunk is loaded again. Frozen water always
 * comes back.
 */
public final class Thaws extends SavedData {
	/** One block of frosted ice and the game time it thaws. */
	record Pending(BlockPos pos, long due) {
		static final Codec<Pending> CODEC = RecordCodecBuilder.create(i -> i.group(
			BlockPos.CODEC.fieldOf("pos").forGetter(Pending::pos),
			Codec.LONG.fieldOf("due").forGetter(Pending::due)
		).apply(i, Pending::new));
	}

	static final Codec<Thaws> CODEC = Pending.CODEC.listOf().xmap(Thaws::new, Thaws::all);
	static final SavedDataType<Thaws> TYPE = new SavedDataType<>(Wildercord.id("frost_thaws"), Thaws::new, CODEC, null);

	/** How often (in ticks) the waiting ice is looked over. */
	private static final int SWEEP = 20;

	/** The waiting ice, by chunk (see {@link #chunk}). */
	private final Map<Long, List<Pending>> byChunk = new HashMap<>();

	public Thaws() {
	}

	private Thaws(List<Pending> pending) {
		pending.forEach(this::add);
	}

	private List<Pending> all() {
		List<Pending> out = new ArrayList<>();
		byChunk.values().forEach(out::addAll);
		return out;
	}

	private void add(Pending pending) {
		byChunk.computeIfAbsent(chunk(pending.pos()), k -> new ArrayList<>()).add(pending);
	}

	/** The chunk a block is in, as one number. */
	static long chunk(BlockPos pos) {
		return ((long) (pos.getX() >> 4) << 32) | ((pos.getZ() >> 4) & 0xFFFFFFFFL);
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Thaws::tick);
	}

	/** Whether the block at {@code pos} is written down to thaw (for the tests). */
	public static boolean waiting(ServerLevel level, BlockPos pos) {
		Thaws thaws = level.getDataStorage().get(TYPE);
		List<Pending> inChunk = thaws == null ? null : thaws.byChunk.get(chunk(pos));
		return inChunk != null && inChunk.stream().anyMatch(p -> p.pos().equals(pos));
	}

	/** Writes down {@code ice} (just frozen) to thaw at game time {@code due}. */
	static void schedule(ServerLevel level, List<BlockPos> ice, long due) {
		if (ice.isEmpty()) {
			return;
		}
		Thaws thaws = level.getDataStorage().computeIfAbsent(TYPE);
		for (BlockPos pos : ice) {
			thaws.add(new Pending(pos.immutable(), due));
		}
		thaws.setDirty();
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % SWEEP != 0) {
			return;
		}
		for (ServerLevel level : server.getAllLevels()) {
			Thaws thaws = level.getDataStorage().get(TYPE);
			if (thaws != null && !thaws.byChunk.isEmpty()) {
				thaws.sweep(level);
			}
		}
	}

	/** Thaws every block whose time has come, in the chunks that are loaded; the rest wait. */
	private void sweep(ServerLevel level) {
		long now = level.getGameTime();
		boolean changed = false;
		Iterator<Map.Entry<Long, List<Pending>>> chunks = byChunk.entrySet().iterator();
		while (chunks.hasNext()) {
			Map.Entry<Long, List<Pending>> chunk = chunks.next();
			Iterator<Pending> it = chunk.getValue().iterator();
			while (it.hasNext()) {
				Pending pending = it.next();
				if (pending.due() > now || !level.isLoaded(pending.pos())) {
					continue;
				}
				if (level.getBlockState(pending.pos()).is(Blocks.FROSTED_ICE)) {
					level.setBlockAndUpdate(pending.pos(), Blocks.WATER.defaultBlockState());
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
