package dev.wildercord.aura;

import net.minecraft.server.level.ServerPlayer;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/** Movement ownership for built-in dashes and explicit add-on admission. Ordinary walking takes no lease. */
public final class MasterFormMovement {
	private MasterFormMovement() {}
	private static final Map<ServerPlayer, Long> OWNERS = new IdentityHashMap<>();
	private static final CopyOnWriteArrayList<Predicate<ServerPlayer>> BLOCKERS = new CopyOnWriteArrayList<>();
	/** Add-ons with sustained movement must register their ownership; failures conservatively refuse the form. */
	public static void registerBlocker(Predicate<ServerPlayer> blocker) { BLOCKERS.add(java.util.Objects.requireNonNull(blocker)); }
	public static void begin(ServerPlayer player, int ticks) {
		MasterForms.cancel(player);
		OWNERS.merge(player, MasterForms.now(player) + Math.max(1, ticks), Math::max);
	}
	static boolean occupied(ServerPlayer player) {
		long now = MasterForms.now(player);
		OWNERS.entrySet().removeIf(entry -> entry.getKey().isRemoved() || entry.getValue() <= now);
		if (OWNERS.containsKey(player)) return true;
		for (Predicate<ServerPlayer> blocker : BLOCKERS) {
			try { if (blocker.test(player)) return true; }
			catch (RuntimeException failure) { return true; }
		}
		// A predicate can start an existing movement lease while reporting its own owner as absent.
		return OWNERS.getOrDefault(player, 0L) > now;
	}
	static void forget(ServerPlayer player) { OWNERS.remove(player); }
	static void clear() { OWNERS.clear(); }
}
