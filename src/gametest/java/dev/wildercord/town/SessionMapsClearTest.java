package dev.wildercord.town;

import dev.wildercord.aura.arts.MethodsAFlavours;
import dev.wildercord.aura.world.WeaversSilk;
import dev.wildercord.cast.PracticeRoom;
import dev.wildercord.cast.Spirits;
import dev.wildercord.cooking.CampPotBlock;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/**
 * The camp pot's chosen recipe, the bounty board's shown contract, a Method sunder's end time, the practice room's entry
 * cooldowns, Weaver's Silk's last-grounded ticks and the spirit wolf packs are per-session state.
 * Closing a world must forget them so the next world starts clean.
 */
public final class SessionMapsClearTest implements FabricClientGameTest {
	private static final UUID SOMEONE = new UUID(0x1234L, 0x9abcL);
	private static final Object[][] MAPS = {
		{CampPotBlock.class, "CHOSEN"}, {BountyBoardBlock.class, "SHOWN"}, {MethodsAFlavours.class, "SUNDER_ENDS"},
		{PracticeRoom.class, "ENTERED"}, {WeaversSilk.class, "GROUNDED"}, {Spirits.class, "PACKS"},
	};

	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(20);
			w.getServer().runOnServer(s -> {
				Map<UUID, String> chosen = field(CampPotBlock.class, "CHOSEN");
				Map<UUID, long[]> shown = field(BountyBoardBlock.class, "SHOWN");
				Map<UUID, Long> sunder = field(MethodsAFlavours.class, "SUNDER_ENDS");
				chosen.put(SOMEONE, "stew");
				shown.put(SOMEONE, new long[] {0L, 1_000_000L});
				sunder.put(SOMEONE, 1_000_000L);
				Map<UUID, Long> entered = field(PracticeRoom.class, "ENTERED");
				entered.put(SOMEONE, 1_000_000L);
				Map<UUID, Integer> grounded = field(WeaversSilk.class, "GROUNDED");
				grounded.put(SOMEONE, 90_000);
				Map<UUID, Object> packs = field(Spirits.class, "PACKS");
				packs.put(SOMEONE, new ArrayList<>());
			});
		}
		for (var entry : MAPS) {
			Class<?> owner = (Class<?>) entry[0];
			String name = (String) entry[1];
			Map<UUID, Object> map = field(owner, name);
			if (!map.isEmpty()) throw new AssertionError(owner.getSimpleName() + "." + name + " kept entries after the world closed: " + map.keySet());
		}
	}

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
