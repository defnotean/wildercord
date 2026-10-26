package dev.wildercord.gear;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Casting gear in the real game's loot: the Archive's vault and library, a stronghold library and an
 * ancient city each roll their own pieces through {@code /loot} (the vault alone holds greater staffs,
 * the ancient city only foci), and a boss's staff roll gives one of its elements or nothing.
 */
public final class GearLootTest implements FabricClientGameTest {
	private static final int X = 0, Y = 200, Z = 0;

	@Override
	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(35);
			TestServerContext server = w.getServer();
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("fill " + (X - 4) + " " + (Y - 1) + " " + (Z - 4) + " " + (X + 4) + " " + (Y - 1) + " " + (Z + 4) + " stone");
			server.runCommand("tp @p " + X + " " + Y + " " + (Z - 5) + " 0 20");

			// 45 in 100: in 120 rolls the vault gives a piece with near certainty, greater staffs among them.
			List<GearDef> vault = roll(c, server, "wildercord:chests/archive_vault", 120);
			check(!vault.isEmpty(), "The Archive vault yields casting gear");
			check(vault.stream().anyMatch(d -> d.kind() == GearDef.GearKind.GREATER_STAFF), "The vault holds greater staffs: " + vault);
			check(vault.stream().noneMatch(d -> d.kind() == GearDef.GearKind.STAFF), "Plain staffs are crafted, never found");
			c.takeScreenshot(TestScreenshotOptions.of("gear_loot_vault").disableCounterPrefix());

			List<GearDef> library = roll(c, server, "wildercord:chests/archive_library", 120);
			check(!library.isEmpty(), "The Archive library yields casting gear");
			check(library.stream().noneMatch(GearLootTest::staff), "The library holds the tome and foci, no staffs: " + library);

			// 10 in 100: no gear in 240 rolls would happen about once in ten billion.
			List<GearDef> stronghold = roll(c, server, "minecraft:chests/stronghold_library", 240);
			check(!stronghold.isEmpty(), "A stronghold library yields casting gear");
			check(stronghold.stream().noneMatch(GearLootTest::staff), "Stronghold libraries hold the tome and foci: " + stronghold);

			List<GearDef> city = roll(c, server, "minecraft:chests/ancient_city", 240);
			check(!city.isEmpty(), "An ancient city yields casting gear");
			check(city.stream().allMatch(d -> d.kind() == GearDef.GearKind.FOCUS), "Ancient cities hold foci only: " + city);

			server.runOnServer(s -> {
				RandomSource random = RandomSource.create(7);
				for (int i = 0; i < 20; i++) {
					var staff = GearLoot.bossStaff(random, 100, List.of("frost", "life"));
					check(staff.isPresent() && staff.get().getItem() instanceof CastingGearItem gear && gear.def.kind() == GearDef.GearKind.GREATER_STAFF
						&& List.of("frost", "life").contains(gear.def.element()), "A sure boss roll gives a greater staff of one of its elements");
					check(GearLoot.bossStaff(random, 0, List.of("frost")).isEmpty(), "A boss roll with no chance gives nothing");
				}
			});
		}
	}

	private static boolean staff(GearDef def) {
		return def.kind() == GearDef.GearKind.STAFF || def.kind() == GearDef.GearKind.GREATER_STAFF;
	}

	/** Spawns {@code table} {@code times} over the platform and gathers the casting gear dropped, clearing every item after. */
	private static List<GearDef> roll(ClientGameTestContext c, TestServerContext server, String table, int times) {
		server.runCommand("kill @e[type=item]");
		for (int i = 0; i < times; i++) {
			server.runCommand("loot spawn " + X + " " + Y + " " + Z + " loot " + table);
		}
		c.waitTicks(10);
		List<GearDef> found = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			List<GearDef> gear = new ArrayList<>();
			for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(X - 16, Y - 16, Z - 16, X + 16, Y + 16, Z + 16))) {
				if (item.getItem().getItem() instanceof CastingGearItem piece) {
					for (int n = 0; n < item.getItem().getCount(); n++) {
						gear.add(piece.def);
					}
				}
			}
			return gear;
		});
		return found;
	}

	private static void check(boolean yes, String why) {
		if (!yes) {
			throw new AssertionError(why);
		}
	}
}
