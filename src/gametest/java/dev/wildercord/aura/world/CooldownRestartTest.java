package dev.wildercord.aura.world;

import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/**
 * Bell Shard, Rimescale and Cinder Antler remember the server tick of their last trigger. Tick counts restart with the
 * server, so closing a world must forget those ticks or the next world's cooldown looks stuck.
 */
public final class CooldownRestartTest implements FabricClientGameTest {
	private static final UUID SOMEONE = new UUID(0x1234L, 0x5678L);

	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(20);
			w.getServer().runOnServer(s -> {
				for (var map : maps()) map.put(SOMEONE, 1_000_000L);
				sprint().put(SOMEONE, 7);
			});
		}
		for (var map : maps()) if (!map.isEmpty()) throw new AssertionError("a cooldown map kept its ticks after the world closed: " + map);
		if (!sprint().isEmpty()) throw new AssertionError("Cinder Antler kept its sprint counts after the world closed");
	}

	private static Map<UUID, Long>[] maps() {
		@SuppressWarnings("unchecked")
		Map<UUID, Long>[] maps = new Map[] {field(BellShard.class, "LAST"), field(Rimescale.class, "LAST"), field(CinderAntler.class, "LAST")};
		return maps;
	}

	private static Map<UUID, Integer> sprint() { return field(CinderAntler.class, "SPRINT"); }

	@SuppressWarnings("unchecked")
	private static <V> Map<UUID, V> field(Class<?> owner, String name) {
		try {
			var f = owner.getDeclaredField(name);
			f.setAccessible(true);
			return (Map<UUID, V>) f.get(null);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(owner.getSimpleName() + "." + name, e);
		}
	}
}
