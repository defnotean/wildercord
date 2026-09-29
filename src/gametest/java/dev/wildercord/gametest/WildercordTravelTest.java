package dev.wildercord.gametest;

import dev.wildercord.client.WaypointHud;
import dev.wildercord.travel.Homes;
import dev.wildercord.travel.Spot;
import dev.wildercord.travel.Teleports;
import dev.wildercord.travel.TeleportRequests;
import dev.wildercord.travel.Travel;
import dev.wildercord.travel.TravelData;
import dev.wildercord.travel.TravelRules;
import dev.wildercord.travel.Warps;
import dev.wildercord.travel.Waypoints;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The travel commands in a real world, called through the same methods the commands call (with the
 * player treated as an operator or not, since the test's player is always one): a warmup broken by
 * moving and by being hurt, {@code /sethome} and {@code /home} waiting out its warmup and landing, the
 * cooldown after it, the homes limit, warps set, used and removed, {@code /back} both ways, teleport
 * request bookkeeping (a second player is a stand-in, so accepting is checked as far as it can be),
 * {@code /rtp} landing on safe ground, and a tracked waypoint reaching the client's HUD. Screenshots:
 * {@code travel_warmup} (the circle forming) and {@code travel_waypoint_hud} / {@code travel_waypoint_nether}.
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordTravelTest implements FabricClientGameTest {
	/** Two open spots on the flat ground, well apart: the home and where the player waits. */
	private static final int[] HOME = {8, 8};
	private static final int[] AWAY = {40, 8};

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				player.setGameMode(GameType.SURVIVAL);
				player.setAttached(Travel.DATA, TravelData.EMPTY);
				Teleports.resetCooldowns(player);
			});
			context.waitTicks(5);
			List<String> failures = new ArrayList<>();
			attempt(failures, "a warmup broken by moving", () -> brokenByMoving(context, world));
			attempt(failures, "a warmup broken by harm", () -> brokenByHarm(context, world));
			attempt(failures, "home after its warmup", () -> homeAfterWarmup(context, world));
			attempt(failures, "the homes limit", () -> homesLimit(world));
			attempt(failures, "warps", () -> warps(context, world));
			attempt(failures, "back", () -> back(context, world));
			attempt(failures, "teleport requests", () -> requests(world));
			attempt(failures, "random teleport", () -> randomTeleport(context, world));
			attempt(failures, "waypoints", () -> waypoints(context, world));
			if (!failures.isEmpty()) {
				throw new AssertionError("Travel went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	private static void attempt(List<String> failures, String what, Runnable test) {
		try {
			test.run();
		} catch (AssertionError | RuntimeException e) {
			failures.add(what + ": " + e.getMessage());
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void check(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError(what);
		}
	}

	/** The top of the ground at a column, where a player's feet go. */
	private static Vec3 ground(ServerLevel level, int[] xz) {
		level.getChunk(xz[0] >> 4, xz[1] >> 4);
		return new Vec3(xz[0] + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, xz[0], xz[1]), xz[1] + 0.5);
	}

	/** Puts the player at a column's ground, looking down a little (so a circle at their feet is in view). */
	private static void standAt(TestSingleplayerContext world, int[] xz) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel overworld = server.overworld();
			Vec3 at = ground(overworld, xz);
			player.teleportTo(overworld, at.x, at.y, at.z, Set.<Relative>of(), 0F, 50F, false);
		});
	}

	private static double distanceTo(TestSingleplayerContext world, int[] xz) {
		return world.getServer().computeOnServer(server -> player(server).position().distanceTo(ground(server.overworld(), xz)));
	}

	private static int warmupTicks() {
		return Travel.config().warmupSeconds() * TravelRules.TICKS_PER_SECOND;
	}

	// ------------------------------------------------------------------ warmups

	private static void brokenByMoving(ClientGameTestContext context, TestSingleplayerContext world) {
		standAt(world, HOME);
		context.waitTicks(5);
		int set = world.getServer().computeOnServer(server -> Homes.set(player(server), "camp"));
		check(set == 1, "/sethome camp should work");
		standAt(world, AWAY);
		context.waitTicks(5);
		boolean started = world.getServer().computeOnServer(server -> Homes.go(player(server), "camp", false) == 1 && Teleports.pending(player(server)));
		if (warmupTicks() == 0) {
			// A server with no warmup: nothing to break.
			return;
		}
		check(started, "/home camp should start a warmup for a player who isn't an operator");
		context.waitTicks(10);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.teleportTo(server.overworld(), player.getX() + 2, player.getY(), player.getZ(), Set.<Relative>of(), 0F, 50F, false);
		});
		context.waitTicks(3);
		check(!world.getServer().computeOnServer(server -> Teleports.pending(player(server))), "moving two blocks should cancel the warmup");
		context.waitTicks(warmupTicks() + 10);
		check(distanceTo(world, HOME) > 20, "a cancelled teleport shouldn't happen anyway");
	}

	private static void brokenByHarm(ClientGameTestContext context, TestSingleplayerContext world) {
		if (warmupTicks() == 0) {
			return;
		}
		standAt(world, AWAY);
		context.waitTicks(5);
		boolean started = world.getServer().computeOnServer(server -> Homes.go(player(server), "camp", false) == 1 && Teleports.pending(player(server)));
		check(started, "a cancelled warmup shouldn't start a cooldown, so /home should work again");
		context.waitTicks(10);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.hurtServer(player.level(), player.level().damageSources().generic(), 1.0F);
		});
		context.waitTicks(2);
		check(!world.getServer().computeOnServer(server -> Teleports.pending(player(server))), "being hurt should cancel the warmup");
		world.getServer().runOnServer(server -> player(server).setHealth(player(server).getMaxHealth()));
	}

	private static void homeAfterWarmup(ClientGameTestContext context, TestSingleplayerContext world) {
		standAt(world, AWAY);
		context.waitTicks(5);
		int began = world.getServer().computeOnServer(server -> Homes.go(player(server), "camp", false));
		check(began == 1, "/home camp should start");
		if (warmupTicks() > 0) {
			context.waitTicks(Math.min(20, warmupTicks() / 2));
			context.takeScreenshot("travel_warmup");
			check(distanceTo(world, HOME) > 20, "the teleport should wait out its warmup");
		}
		world.getServer().waitFor(server -> player(server).position().distanceTo(ground(server.overworld(), HOME)) < 1.0, warmupTicks() + 40);
		String problem = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			Spot back = Travel.data(player).back().orElse(null);
			if (back == null || back.pos().distanceTo(ground(server.overworld(), AWAY)) > 1.0) {
				return "/back should remember where the teleport left from (has " + back + ")";
			}
			if (Travel.config().cooldownSeconds() > 0 && Teleports.cooldown(player, Teleports.Kind.HOME) <= 0) {
				return "/home should be cooling down after it's used";
			}
			if (Travel.config().cooldownSeconds() > 0 && Homes.go(player, "camp", false) != 0) {
				return "/home shouldn't work again during its cooldown";
			}
			if (Homes.go(player, "camp", true) != 1) {
				return "an operator should skip the cooldown";
			}
			return null;
		});
		check(problem == null, problem);
	}

	private static void homesLimit(TestSingleplayerContext world) {
		String problem = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			int max = Travel.config().maxHomes();
			if (max < 1) {
				return null;
			}
			for (int i = Travel.data(player).homes().size(); i < max; i++) {
				if (Homes.set(player, "extra" + i) != 1) {
					return "home " + (i + 1) + " of " + max + " should be allowed";
				}
			}
			if (Homes.set(player, "onetoomany") != 0 || Travel.data(player).homes().size() != max) {
				return "a home past the limit of " + max + " should be refused";
			}
			if (Homes.set(player, "camp") != 1 || Travel.data(player).homes().size() != max) {
				return "setting a home again by the same name should move it, even at the limit";
			}
			if (Homes.set(player, "No Spaces!") != 0) {
				return "a name with spaces and punctuation should be refused";
			}
			for (String name : List.copyOf(Travel.data(player).homes().keySet())) {
				if (!name.equals("camp")) {
					Homes.delete(player, name);
				}
			}
			return Travel.data(player).homes().keySet().equals(Set.of("camp")) ? null : "/delhome should remove homes (left " + Travel.data(player).homes().keySet() + ")";
		});
		check(problem == null, problem);
	}

	// ------------------------------------------------------------------ warps and back

	private static void warps(ClientGameTestContext context, TestSingleplayerContext world) {
		standAt(world, HOME);
		context.waitTicks(5);
		check(world.getServer().computeOnServer(server -> Warps.set(player(server), "plaza")) == 1, "/setwarp plaza should work");
		check(world.getServer().computeOnServer(server -> Warps.of(server).get("plaza") != null), "the warp should be kept in the world's data");
		standAt(world, AWAY);
		context.waitTicks(5);
		check(world.getServer().computeOnServer(server -> Warps.go(player(server), "plaza", true)) == 1, "/warp plaza should work");
		check(distanceTo(world, HOME) < 1.0, "an operator's /warp should be instant, and land at the warp");
		check(world.getServer().computeOnServer(server -> Warps.delete(player(server), "plaza")) == 1, "/delwarp plaza should work");
		check(world.getServer().computeOnServer(server -> Warps.of(server).get("plaza") == null && Warps.go(player(server), "plaza", true) == 0),
			"a removed warp should be gone");
	}

	private static void back(ClientGameTestContext context, TestSingleplayerContext world) {
		// The last teleport (the warp) went from AWAY to HOME.
		check(world.getServer().computeOnServer(server -> Teleports.back(player(server), true)) == 1, "/back should work");
		check(distanceTo(world, AWAY) < 1.0, "/back should return to where the warp left from");
		context.waitTicks(2);
		check(world.getServer().computeOnServer(server -> Teleports.back(player(server), true)) == 1, "/back should work twice");
		check(distanceTo(world, HOME) < 1.0, "a second /back should undo the first");
	}

	// ------------------------------------------------------------------ requests

	private static void requests(TestSingleplayerContext world) {
		String problem = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = server.overworld();
			FakePlayer other = FakePlayer.get(level);
			TravelData before = Travel.data(other);
			try {
				other.setAttached(Travel.DATA, TravelData.EMPTY);
				if (TeleportRequests.ask(player, player, false) != 0) {
					return "a request to yourself should be refused";
				}
				if (TeleportRequests.ask(player, other, false) != 1 || TeleportRequests.waitingFor(other).size() != 1) {
					return "a request should wait for its answer";
				}
				if (TeleportRequests.ask(player, other, true) != 1 || TeleportRequests.waitingFor(other).size() != 1
						|| !TeleportRequests.waitingFor(other).getFirst().here()) {
					return "a second request to the same player should replace the first";
				}
				long later = server.getTickCount() + (long) Travel.config().tpaTimeoutSeconds() * TravelRules.TICKS_PER_SECOND;
				if (TeleportRequests.expire(server, later).size() != 1 || !TeleportRequests.waitingFor(other).isEmpty()) {
					return "a request should run out after the timeout";
				}
				TeleportRequests.ask(player, other, false);
				if (TeleportRequests.cancel(player) != 1 || !TeleportRequests.waitingFor(other).isEmpty()) {
					return "/tpcancel should take a request back";
				}
				TeleportRequests.ask(player, other, false);
				if (TeleportRequests.deny(other, player) != 1 || !TeleportRequests.waitingFor(other).isEmpty()) {
					return "/tpdeny should answer a request";
				}
				if (TeleportRequests.deny(other, player) != 0) {
					return "a request can only be answered once";
				}
				// Asked to come to the player: accepting starts the other one's teleport and uses up the request.
				TeleportRequests.ask(player, other, true);
				if (TeleportRequests.accept(other, null) != 1 || !TeleportRequests.waitingFor(other).isEmpty()) {
					return "/tpaccept should accept the newest request";
				}
				other.setAttached(Travel.DATA, TravelData.EMPTY.withRequestsOff(true));
				if (TeleportRequests.ask(player, other, false) != 0) {
					return "someone who switched requests off with /tptoggle shouldn't get any";
				}
				if (TeleportRequests.toggle(player) != 1 || !Travel.data(player).requestsOff() || TeleportRequests.toggle(player) != 1
						|| Travel.data(player).requestsOff()) {
					return "/tptoggle should switch requests off and on again";
				}
				return null;
			} finally {
				TeleportRequests.cancel(player);
				other.setAttached(Travel.DATA, before);
			}
		});
		check(problem == null, problem);
	}

	// ------------------------------------------------------------------ rtp and waypoints

	private static void randomTeleport(ClientGameTestContext context, TestSingleplayerContext world) {
		String problem = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			if (Teleports.randomly(player, true) != 1) {
				return "/rtp should find somewhere on flat ground";
			}
			if (player.level().dimension() != Level.OVERWORLD) {
				return "/rtp should land in the overworld";
			}
			ServerLevel level = player.level();
			BlockPos feet = player.blockPosition();
			BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.3, player.getZ());
			if (level.getBlockState(below).getCollisionShape(level, below).isEmpty() || level.getBlockState(below).is(BlockTags.LEAVES)) {
				return "/rtp should land on solid ground (landed on " + level.getBlockState(below) + ")";
			}
			if (!level.noCollision(player) || !level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) {
				return "/rtp shouldn't land inside blocks or water";
			}
			BlockPos spawn = server.getRespawnData().pos();
			double far = Math.hypot(player.getX() - spawn.getX(), player.getZ() - spawn.getZ());
			if (far > Travel.config().rtpRadius() + 2) {
				return "/rtp should stay within " + Travel.config().rtpRadius() + " blocks of spawn (went " + (int) far + ")";
			}
			return null;
		});
		check(problem == null, problem);
		context.waitTicks(20);
	}

	private static void waypoints(ClientGameTestContext context, TestSingleplayerContext world) {
		standAt(world, HOME);
		context.waitTicks(5);
		check(world.getServer().computeOnServer(server -> Waypoints.add(player(server), "camp", null, null)) == 1, "/waypoint add camp should work");
		check(world.getServer().computeOnServer(server -> Waypoints.track(player(server), "camp")) == 1, "/waypoint track camp should work");
		context.waitFor(mc -> WaypointHud.tracked() != null && WaypointHud.tracked().name().equals("camp"), 60);
		standAt(world, AWAY);
		context.waitTicks(25);
		context.takeScreenshot("travel_waypoint_hud");

		// One in the Nether, as a shared waypoint arrives: the HUD names the world instead of pointing.
		check(world.getServer().computeOnServer(server -> Waypoints.add(player(server), "fortress", new Vec3(120, 70, -40), server.getLevel(Level.NETHER))) == 1,
			"a waypoint in another world should be added");
		check(world.getServer().computeOnServer(server -> Waypoints.track(player(server), "fortress")) == 1, "tracking another waypoint should work");
		context.waitFor(mc -> WaypointHud.tracked() != null && WaypointHud.tracked().dimension().equals("minecraft:the_nether"), 60);
		context.waitTicks(5);
		context.takeScreenshot("travel_waypoint_nether");

		check(world.getServer().computeOnServer(server -> Waypoints.remove(player(server), "fortress")) == 1, "/waypoint remove should work");
		context.waitFor(mc -> WaypointHud.tracked() == null, 60);
		check(world.getServer().computeOnServer(server -> Travel.data(player(server)).tracked().isEmpty()), "removing the tracked waypoint should stop tracking it");
		check(world.getServer().computeOnServer(server -> Waypoints.track(player(server), "camp") == 1 && Waypoints.untrack(player(server)) == 1),
			"/waypoint untrack should work");
		context.waitFor(mc -> WaypointHud.tracked() == null, 60);
		check(world.getServer().computeOnServer(server -> Waypoints.remove(player(server), "camp") == 1 && Travel.data(player(server)).waypoints().isEmpty()),
			"waypoints should all be removable");
	}
}
