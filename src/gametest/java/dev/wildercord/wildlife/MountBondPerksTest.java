package dev.wildercord.wildlife;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * What a mount bond gives in the real game: each level's speed and health on the mount, the rider's real sprint key
 * dashing it only from the third level, a landing from a jump at the fifth striking a beast nearby but sparing the
 * rider's own pet, the bond and its perks kept through a save and reopen, and a mount bonded to someone else starting
 * again with a new rider.
 */
public final class MountBondPerksTest implements FabricClientGameTest {
	private static UUID stagId, pigId, wolfId;

	private static void check(boolean x, String why) {
		if (!x) throw new AssertionError(why);
	}

	private static ServerPlayer p(MinecraftServer s) {
		return s.getPlayerList().getPlayers().getFirst();
	}

	private static RidgebackStag stag(MinecraftServer s) {
		return (RidgebackStag) s.overworld().getEntity(stagId);
	}

	private static double bonus(RidgebackStag stag, String id) {
		var attribute = stag.getAttribute(id.equals("speed") ? Attributes.MOVEMENT_SPEED : Attributes.MAX_HEALTH);
		var modifier = attribute == null ? null : attribute.getModifier(Wildercord.id("mount_bond_" + id));
		return modifier == null ? 0 : modifier.amount();
	}

	/** Checks the stag carries exactly a {@code level} bond's speed and health. */
	private static void perks(RidgebackStag stag, int level, String when) {
		check(Math.abs(bonus(stag, "speed") - MountBondRules.speed(level)) < 1e-6, when + ": speed for bond " + level + ", had " + bonus(stag, "speed"));
		check(Math.abs(bonus(stag, "health") - MountBondRules.health(level)) < 1e-6, when + ": health for bond " + level + ", had " + bonus(stag, "health"));
	}

	/** Holds the real sprint key a few ticks with the stag standing still and returns how far it went along its heading. */
	private static double sprint(ClientGameTestContext c, TestServerContext server, KeyMapping key) {
		double from = server.computeOnServer(s -> stag(s).getZ());
		c.getInput().holdKey(key);
		c.waitTicks(5);
		c.getInput().releaseKey(key);
		c.waitTicks(10);
		return server.computeOnServer(s -> stag(s).getZ()) - from;
	}

	@Override
	public void runTest(ClientGameTestContext c) {
		TestWorldSave save;
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runCommand("time set day");
			w.getServer().runOnServer(s -> {
				var l = s.overworld();
				for (int x = -8; x <= 8; x++) for (int z = -8; z <= 40; z++) {
					l.setBlock(new BlockPos(x, 100, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
					for (int y = 101; y <= 110; y++) l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
				}
				p(s).setGameMode(GameType.SURVIVAL);
				p(s).teleportTo(l, 0.5, 101, 0.5, Set.<Relative>of(), 0, 10, false);
				RidgebackStag stag = MountContent.RIDGEBACK_STAG.create(l, EntitySpawnReason.COMMAND);
				stag.snapTo(0.5, 101, 1.5, 0, 0);
				l.addFreshEntity(stag);
				stag.tameWithName(p(s));
				stag.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
				stagId = stag.getUUID();
				check(p(s).startRiding(stag), "the owner climbs on");
				perks(stag, 0, "A fresh stag");

				// Level 2: quicker and hardier, but no dash yet.
				var bond = MountBonds.grow(l, p(s), stag, 600);
				check(bond.level() == 2, "600 points make bond 2, had " + bond.level());
				perks(stag, 2, "At bond 2");
				check(Math.abs(stag.getMaxHealth() - stag.getAttribute(Attributes.MAX_HEALTH).getBaseValue() - MountBondRules.health(2)) < 1e-6, "Bond 2 raises its max health");
			});
			c.waitTicks(5);
			TestServerContext server = w.getServer();
			KeyMapping sprintKey = c.computeOnClient(mc -> mc.options.keySprint);

			double still = sprint(c, server, sprintKey);
			check(Math.abs(still) < 0.5, "Below bond 3 the sprint key doesn't dash, moved " + still);

			w.getServer().runOnServer(s -> {
				check(MountBonds.grow(s.overworld(), p(s), stag(s), 800).level() == MountBondRules.DASH_LEVEL, "1400 points make bond 3");
				perks(stag(s), 3, "At bond 3");
			});
			c.waitTicks(3);
			double dashed = sprint(c, server, sprintKey);
			check(dashed > 1.0, "At bond 3 the real sprint key dashes the stag ahead, moved " + dashed);
			c.takeScreenshot(TestScreenshotOptions.of("mount_bond_dash").disableCounterPrefix());

			// Level 5: a landing from a jump strikes the pig, never the rider's own wolf.
			w.getServer().runOnServer(s -> {
				var l = s.overworld();
				check(MountBonds.grow(l, p(s), stag(s), 6000).level() == MountBondRules.STRIKE_LEVEL, "Enough points make bond 5");
				perks(stag(s), 5, "At bond 5");
				double x = stag(s).getX(), z = stag(s).getZ();
				var pig = EntityTypes.PIG.create(l, EntitySpawnReason.COMMAND);
				pig.snapTo(x + 2, 101, z, 0, 0);
				pig.setNoAi(true);
				l.addFreshEntity(pig);
				pigId = pig.getUUID();
				Wolf wolf = EntityTypes.WOLF.create(l, EntitySpawnReason.COMMAND);
				wolf.snapTo(x - 2, 101, z, 0, 0);
				wolf.setNoAi(true);
				wolf.tame(p(s));
				l.addFreshEntity(wolf);
				wolfId = wolf.getUUID();
				stag(s).teleportTo(l, x, 105, z, Set.<Relative>of(), 0, 0, false);
			});
			c.waitTicks(30);
			w.getServer().runOnServer(s -> {
				var l = s.overworld();
				check(p(s).getVehicle() == stag(s), "Still riding after the landing");
				var pig = (LivingEntity) l.getEntity(pigId);
				var wolf = (Wolf) l.getEntity(wolfId);
				check(pig != null && pig.getHealth() < pig.getMaxHealth(), "The landing strikes the pig beside it");
				check(wolf != null && wolf.getHealth() == wolf.getMaxHealth(), "and spares the rider's own wolf");
			});
			c.takeScreenshot(TestScreenshotOptions.of("mount_bond_strike").disableCounterPrefix());
			save = w.getWorldSave();
		}

		try (var w = save.open()) {
			c.waitTicks(40);
			w.getServer().runOnServer(s -> {
				RidgebackStag stag = stag(s);
				check(stag != null, "The stag is there after a reopen");
				check(MountBonds.bond(stag, p(s)).level() == MountBondRules.STRIKE_LEVEL, "The bond is kept through a save and reopen");
				perks(stag, 5, "After a reopen");

				// Bonded to someone else, the stag starts again with its new rider and loses the old bond's perks.
				stag.setAttached(MountBonds.BOND, new MountBonds.Bond(Optional.of(UUID.randomUUID()), 5000));
				check(MountBonds.bond(stag, p(s)).points() == 0, "Another's bond isn't this rider's");
				var fresh = MountBonds.grow(s.overworld(), p(s), stag, 10);
				check(fresh.points() == 10 && fresh.owner().orElseThrow().equals(p(s).getUUID()), "A new rider's bond starts from nothing");
				perks(stag, 0, "With the bond started over");
			});
		}
	}
}
