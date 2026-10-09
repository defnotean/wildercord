package dev.wildercord.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * One dimension's record of its residues: where each is, what kind, when it was left and when it fades,
 * and whose magic left it. Indexed three ways so nothing ever has to look at every residue: by position
 * (is this block a residue?), by chunk (how many here? which wait for this chunk to load?) and by the
 * time each fades (which are due now?). The server asks only for the ones whose time has come.
 *
 * <p>Pure (no Minecraft types; positions pack the way the game packs them), so caps, ordering and the
 * decay schedule are unit-tested. {@code P} is whatever the runtime keeps alongside (the block that was
 * put down and the one it replaced).</p>
 */
public final class ResidueLedger<P> {
	/**
	 * One residue.
	 *
	 * @param kind       its {@link ResidueRules.Kind#path}
	 * @param placedAt   the game time it was left
	 * @param due        the game time it fades
	 * @param generation how many times removed from the spell that left it (a wildbloom's seedlings count up)
	 * @param owner      whose magic left it (a player's id), or "" for the world's own (a boss's)
	 */
	public record Entry<P>(int x, int y, int z, String kind, long placedAt, long due, int generation, String owner, P data) {
		public long pos() {
			return pack(x, y, z);
		}

		public long chunk() {
			return chunkOf(x, z);
		}

		public Entry<P> shifted(long ticks) {
			return new Entry<>(x, y, z, kind, placedAt - ticks, due - ticks, generation, owner, data);
		}
	}

	/** The limits a new residue must fit under. */
	public record Caps(int perChunk, int perArea, int areaRadius, int perDimension, int perOwner) {
		public static final Caps DEFAULT = new Caps(ResidueRules.PER_CHUNK, ResidueRules.PER_AREA, ResidueRules.AREA_RADIUS,
			ResidueRules.PER_DIMENSION, ResidueRules.PER_OWNER);
	}

	/** Why a residue wasn't let in (null when it was). */
	public enum Refusal { TAKEN, CHUNK_FULL, AREA_FULL, DIMENSION_FULL, OWNER_FULL }

	private final Map<Long, Entry<P>> byPos = new HashMap<>();
	private final Map<Long, Set<Long>> byChunk = new HashMap<>();
	/** Positions waiting for their time, by the time they fade. */
	private final TreeMap<Long, Set<Long>> schedule = new TreeMap<>();
	/** Positions past their time whose chunk wasn't loaded then, by chunk: they fade as it loads, never loaded for it. */
	private final Map<Long, Set<Long>> parked = new HashMap<>();
	private final Map<String, Integer> owners = new HashMap<>();

	// ------------------------------------------------------------------ positions, packed as the game packs them

	private static final int HORIZONTAL = 26;
	private static final int VERTICAL = 64 - 2 * HORIZONTAL;
	private static final long HORIZONTAL_MASK = (1L << HORIZONTAL) - 1L;
	private static final long VERTICAL_MASK = (1L << VERTICAL) - 1L;

	/** A block position as one number, the same number the game's {@code BlockPos.asLong} gives. */
	public static long pack(int x, int y, int z) {
		return (x & HORIZONTAL_MASK) << (VERTICAL + HORIZONTAL) | (y & VERTICAL_MASK) | (z & HORIZONTAL_MASK) << VERTICAL;
	}

	/** A chunk as one number, the same number the game's {@code ChunkPos.pack} gives. */
	public static long chunkOf(int blockX, int blockZ) {
		return (blockX >> 4) & 0xFFFFFFFFL | ((long) (blockZ >> 4) & 0xFFFFFFFFL) << 32;
	}

	// ------------------------------------------------------------------ asking

	public int size() {
		return byPos.size();
	}

	public boolean isEmpty() {
		return byPos.isEmpty();
	}

	public Entry<P> get(long pos) {
		return byPos.get(pos);
	}

	public Entry<P> get(int x, int y, int z) {
		return byPos.get(pack(x, y, z));
	}

	public int inChunk(long chunk) {
		Set<Long> here = byChunk.get(chunk);
		return here == null ? 0 : here.size();
	}

	public int owned(String owner) {
		return owner == null || owner.isEmpty() ? 0 : owners.getOrDefault(owner, 0);
	}

	/** How many residues lie within {@code radius} blocks of a spot (in every direction, as a cube). */
	public int near(int x, int y, int z, int radius) {
		int count = 0;
		for (int cx = (x - radius) >> 4; cx <= (x + radius) >> 4; cx++) {
			for (int cz = (z - radius) >> 4; cz <= (z + radius) >> 4; cz++) {
				Set<Long> here = byChunk.get(cx & 0xFFFFFFFFL | ((long) cz & 0xFFFFFFFFL) << 32);
				if (here == null) {
					continue;
				}
				for (long pos : here) {
					Entry<P> e = byPos.get(pos);
					if (Math.abs(e.x() - x) <= radius && Math.abs(e.y() - y) <= radius && Math.abs(e.z() - z) <= radius) {
						count++;
					}
				}
			}
		}
		return count;
	}

	/** Whether a new residue at this spot, left by {@code owner}, fits under the caps. */
	public Refusal admit(int x, int y, int z, String owner, Caps caps) {
		if (byPos.containsKey(pack(x, y, z))) {
			return Refusal.TAKEN;
		}
		if (byPos.size() >= caps.perDimension()) {
			return Refusal.DIMENSION_FULL;
		}
		if (inChunk(chunkOf(x, z)) >= caps.perChunk()) {
			return Refusal.CHUNK_FULL;
		}
		if (owned(owner) >= caps.perOwner()) {
			return Refusal.OWNER_FULL;
		}
		if (near(x, y, z, caps.areaRadius()) >= caps.perArea()) {
			return Refusal.AREA_FULL;
		}
		return null;
	}

	/** Every residue, in a fixed order (for saving). */
	public List<Entry<P>> all() {
		List<Entry<P>> out = new ArrayList<>(byPos.values());
		out.sort(Comparator.comparingLong(Entry::pos));
		return out;
	}

	/** The soonest time any residue still waiting is due (Long.MAX_VALUE when none waits). */
	public long nextDue() {
		return schedule.isEmpty() ? Long.MAX_VALUE : schedule.firstKey();
	}

	public boolean hasParked(long chunk) {
		return parked.containsKey(chunk);
	}

	/** The chunks some residue past its time is waiting on. */
	public Set<Long> parkedChunks() {
		return parked.keySet();
	}

	// ------------------------------------------------------------------ changing

	/** Records a residue (replacing any already recorded at its spot). */
	public void add(Entry<P> entry) {
		long pos = entry.pos();
		remove(pos);
		byPos.put(pos, entry);
		byChunk.computeIfAbsent(entry.chunk(), k -> new LinkedHashSet<>()).add(pos);
		schedule.computeIfAbsent(entry.due(), k -> new LinkedHashSet<>()).add(pos);
		if (!entry.owner().isEmpty()) {
			owners.merge(entry.owner(), 1, Integer::sum);
		}
	}

	/** Forgets the residue at {@code pos} (it faded, was harvested, or is gone), returning it, or null if there was none. */
	public Entry<P> remove(long pos) {
		Entry<P> entry = byPos.remove(pos);
		if (entry == null) {
			return null;
		}
		drop(byChunk, entry.chunk(), pos);
		drop(schedule, entry.due(), pos);
		drop(parked, entry.chunk(), pos);
		if (!entry.owner().isEmpty()) {
			owners.computeIfPresent(entry.owner(), (k, n) -> n <= 1 ? null : n - 1);
		}
		return entry;
	}

	/**
	 * Takes up to {@code max} residues whose time has come, soonest first. They stay recorded: the caller
	 * fades each one (and {@link #remove}s it) or, its chunk not being loaded, {@link #park}s it.
	 */
	public List<Entry<P>> takeDue(long now, int max) {
		List<Entry<P>> out = new ArrayList<>();
		Iterator<Map.Entry<Long, Set<Long>>> it = schedule.headMap(now, true).entrySet().iterator();
		while (it.hasNext() && out.size() < max) {
			Set<Long> positions = it.next().getValue();
			Iterator<Long> each = positions.iterator();
			while (each.hasNext() && out.size() < max) {
				out.add(byPos.get(each.next()));
				each.remove();
			}
			if (positions.isEmpty()) {
				it.remove();
			}
		}
		return out;
	}

	/** A residue past its time whose chunk isn't loaded: it waits for that chunk. */
	public void park(Entry<P> entry) {
		if (byPos.get(entry.pos()) == entry) {
			drop(schedule, entry.due(), entry.pos());
			parked.computeIfAbsent(entry.chunk(), k -> new LinkedHashSet<>()).add(entry.pos());
		}
	}

	/** The residues that were waiting on a chunk now loaded (still recorded, for the caller to fade). */
	public List<Entry<P>> unpark(long chunk) {
		return unpark(chunk, Integer.MAX_VALUE);
	}

	/** Takes at most {@code max} waiting residues; the rest remain parked for a later sweep. */
	public List<Entry<P>> unpark(long chunk, int max) {
		if (max < 0) {
			throw new IllegalArgumentException("max must not be negative");
		}
		Set<Long> waiting = parked.get(chunk);
		List<Entry<P>> out = new ArrayList<>();
		if (waiting != null) {
			Iterator<Long> it = waiting.iterator();
			while (it.hasNext() && out.size() < max) {
				long pos = it.next();
				it.remove();
				Entry<P> entry = byPos.get(pos);
				if (entry != null) {
					out.add(entry);
				}
			}
			if (waiting.isEmpty()) {
				parked.remove(chunk);
			}
		}
		return out;
	}

	/**
	 * Moves every residue's clock {@code ticks} on, as if that much time had passed for them (their times
	 * left and faded both earlier). For tests, and for an operator who wants a world tidied sooner.
	 */
	public void shift(long ticks) {
		List<Entry<P>> every = new ArrayList<>(byPos.values());
		Set<Long> wasParked = new java.util.HashSet<>();
		parked.values().forEach(wasParked::addAll);
		byPos.clear();
		byChunk.clear();
		schedule.clear();
		parked.clear();
		owners.clear();
		for (Entry<P> entry : every) {
			Entry<P> moved = entry.shifted(ticks);
			add(moved);
			if (wasParked.contains(moved.pos())) {
				park(moved);
			}
		}
	}

	private static void drop(Map<Long, Set<Long>> index, long key, long pos) {
		Set<Long> set = index.get(key);
		if (set != null && set.remove(pos) && set.isEmpty()) {
			index.remove(key);
		}
	}
}
