package dev.wildercord.wildlife;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Set;

/**
 * A bonded skyray in the real game: using the bridle calls one down under its owner and starts the call's cooldown, the
 * held forward key flies it along the rider's look (climbing when they look up), sneaking off in the air leaves the
 * rider slow-falling, a riderless skyray lingers and then goes back to the sky, a new call sends any other away, its
 * owner is kept through a save and reopen, its rider can't hurt it, and its death puts the bridle on a long cooldown.
 */
public final class SkyrayFlightTest implements FabricClientGameTest {
	private static void check(boolean x, String why) {
		if (!x) throw new AssertionError(why);
	}

	private static ServerPlayer p(MinecraftServer s) {
		return s.getPlayerList().getPlayers().getFirst();
	}

	private static List<BondedSkyray> rays(MinecraftServer s) {
		return s.overworld().getEntitiesOfClass(BondedSkyray.class, new AABB(p(s).blockPosition()).inflate(256), r -> p(s).getUUID().equals(r.owner()));
	}

	private static BondedSkyray ray(MinecraftServer s) {
		var all = rays(s);
		return all.isEmpty() ? null : all.getFirst();
	}

	/** Uses the bridle in the player's hand through the real client. */
	private static void callDown(ClientGameTestContext c) {
		c.runOnClient(mc -> mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND));
		c.waitTicks(5);
	}

	@Override
	public void runTest(ClientGameTestContext c) {
		TestWorldSave save;
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			TestServerContext server = w.getServer();
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("time set day");
			server.runOnServer(s -> {
				var l = s.overworld();
				for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
					l.setBlock(new BlockPos(x, 100, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
					for (int y = 101; y <= 106; y++) l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
				}
				p(s).setGameMode(GameType.SURVIVAL);
				p(s).teleportTo(l, 0.5, 101, 0.5, Set.<Relative>of(), 180, -30, false);
				p(s).setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MountContent.SKYRAY_BRIDLE));
			});
			c.waitTicks(5);

			// The bridle calls the skyray down under its owner and starts the call's cooldown.
			callDown(c);
			server.runOnServer(s -> {
				check(rays(s).size() == 1, "The bridle calls one skyray, had " + rays(s).size());
				check(p(s).getVehicle() == ray(s), "The caller is put on it");
				check(p(s).getCooldowns().isOnCooldown(p(s).getMainHandItem()), "The call starts the bridle's cooldown");
			});

			// The forward key flies it along the rider's look: north, and up while they look up.
			var from = server.computeOnServer(s -> ray(s).position());
			KeyMapping forward = c.computeOnClient(mc -> mc.options.keyUp);
			c.getInput().holdKey(forward);
			c.waitTicks(40);
			c.takeScreenshot(TestScreenshotOptions.of("skyray_flight").disableCounterPrefix());
			c.getInput().releaseKey(forward);
			server.runOnServer(s -> {
				var to = ray(s).position();
				check(from.z - to.z > 4, "It flies north along the look, moved " + (from.z - to.z));
				check(to.y - from.y > 2, "Looking up it climbs, rose " + (to.y - from.y));
				check(Math.abs(to.x - from.x) < 2, "It keeps to the rider's heading, drifted " + (to.x - from.x));
			});

			// Sneaking off in the air leaves the rider slow-falling and the skyray riderless.
			KeyMapping sneak = c.computeOnClient(mc -> mc.options.keyShift);
			c.getInput().holdKey(sneak);
			c.waitTicks(3);
			c.getInput().releaseKey(sneak);
			c.waitTicks(3);
			server.runOnServer(s -> {
				check(!p(s).isPassenger(), "Sneaking steps off");
				check(p(s).hasEffect(MobEffects.SLOW_FALLING), "Stepping off in the air brings slow falling");
				check(ray(s) != null && !ray(s).isVehicle(), "The skyray stays a while without a rider");
				// Back to the pad: it flew out over unknown ground, far below it.
				p(s).teleportTo(s.overworld(), 0.5, 101, 0.5, Set.<Relative>of(), 180, 0, false);
			});

			// Riderless it lingers, then goes back to the sky.
			c.waitTicks(SkyMountRules.LINGER_TICKS - 40);
			server.runOnServer(s -> check(ray(s) != null, "It lingers for its owner"));
			c.waitTicks(60);
			server.runOnServer(s -> check(rays(s).isEmpty(), "Left unridden it goes back to the sky"));

			// A new call, then another from the ground: one skyray at a time.
			server.runOnServer(s -> p(s).teleportTo(s.overworld(), 0.5, 101, 0.5, Set.<Relative>of(), 180, 0, false));
			c.waitTicks(10);
			callDown(c);
			server.runOnServer(s -> {
				check(p(s).getVehicle() == ray(s), "Once the cooldown is over it can be called again");
				p(s).stopRiding();
				p(s).teleportTo(s.overworld(), 0.5, 101, 0.5, Set.<Relative>of(), 180, 0, false);
			});
			// The cooldown runs out well before the riderless one would leave on its own.
			c.waitTicks(SkyMountRules.CALL_COOLDOWN + 5);
			var first = server.computeOnServer(s -> ray(s).getUUID());
			callDown(c);
			server.runOnServer(s -> {
				check(rays(s).size() == 1 && !ray(s).getUUID().equals(first), "A new call sends the other skyray away");
				p(s).stopRiding();
			});
			c.waitTicks(5);
			save = w.getWorldSave();
		}

		try (var w = save.open()) {
			c.waitTicks(30);
			w.getServer().runOnServer(s -> {
				var l = s.overworld();
				BondedSkyray ray = ray(s);
				check(ray != null, "The skyray and its owner are kept through a save and reopen");

				// Its rider can't hurt it; anything else can, and its death grounds the bridle.
				p(s).startRiding(ray);
				float health = ray.getHealth();
				check(!ray.hurtServer(l, p(s).damageSources().playerAttack(p(s)), 5) && ray.getHealth() == health, "Its rider can't hurt it");
				p(s).stopRiding();
				check(!ray.hurtServer(l, p(s).damageSources().playerAttack(p(s)), 5), "Nor can its owner on foot");
				check(ray.hurtServer(l, l.damageSources().magic(), 5) && ray.getHealth() < health, "Other harm hurts it");
				ray.hurtServer(l, l.damageSources().fellOutOfWorld(), 1000);
				check(!ray.isAlive(), "It dies");
				var bridle = new ItemStack(MountContent.SKYRAY_BRIDLE);
				check(p(s).getCooldowns().isOnCooldown(bridle) && p(s).getCooldowns().getCooldownPercent(bridle, 0) > 0.99F,
					"Its death puts the bridle on the long fallen cooldown");
			});
		}
	}
}
