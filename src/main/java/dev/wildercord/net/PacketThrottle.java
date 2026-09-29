package dev.wildercord.net;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A small allowance per player for packets that rewrite the spellbook (select, edit, rename, a
 * passive's switch): a burst of {@code burst}, refilled by one every {@code ticksPerPacket} ticks.
 * Far more than any hand on a keyboard or a mouse wheel sends; a modified client flooding them is
 * simply ignored past it. Pure logic: the server's tick count is passed in.
 */
public final class PacketThrottle {
	private final int burst;
	private final int ticksPerPacket;
	private final Map<UUID, Bucket> buckets = new HashMap<>();

	private static final class Bucket {
		double tokens;
		long at;

		Bucket(double tokens, long at) {
			this.tokens = tokens;
			this.at = at;
		}
	}

	public PacketThrottle(int burst, int ticksPerPacket) {
		this.burst = Math.max(1, burst);
		this.ticksPerPacket = Math.max(1, ticksPerPacket);
	}

	/**
	 * The allowance for casting packets (a cast, or a charge started or let go): a burst of 20, then 20 a
	 * second. Each reads the spell afresh, so a flood is worth stopping, but no hand comes near it: a Rapid
	 * spell is back in a quarter of a second, and there are only a handful of direct-cast keys.
	 */
	public static PacketThrottle casting() {
		return new PacketThrottle(20, 1);
	}

	/** Whether {@code player} may send one more now (and counts it if so). */
	public boolean allow(UUID player, long tick) {
		Bucket bucket = buckets.computeIfAbsent(player, k -> new Bucket(burst, tick));
		// A clock that went backwards (another world, a restart) just starts the bucket over.
		long elapsed = tick - bucket.at;
		bucket.tokens = elapsed < 0 ? burst : Math.min(burst, bucket.tokens + elapsed / (double) ticksPerPacket);
		bucket.at = tick;
		if (bucket.tokens < 1) {
			return false;
		}
		bucket.tokens -= 1;
		return true;
	}

	public void forget(UUID player) {
		buckets.remove(player);
	}

	public void clear() {
		buckets.clear();
	}
}
