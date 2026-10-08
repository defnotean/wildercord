package dev.wildercord.pet;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/** Invoked by the existing Cinnamon suite; uses real save close/reopen rather than a codec-only approximation. */
public final class CinnamonLifecycleChecks {
	private CinnamonLifecycleChecks() {}
	public static void run(ClientGameTestContext context) {
		Path config = FabricLoader.getInstance().getConfigDir().resolve("wildercord-cinnamon.json");
		String previous;
		try { previous = Files.exists(config) ? Files.readString(config) : null; }
		catch (Exception e) { throw new AssertionError(e); }
		try {
			write(config, "");
			UUID[] identity = new UUID[1];
			long[] deadlines = new long[3];
			TestWorldSave saved;
			try (var world = context.worldBuilder().create()) {
				context.waitTicks(25);
				world.getServer().runCommand("gamerule spawn_mobs false");
				world.getServer().runOnServer(server -> {
					var p = player(server); p.setGameMode(GameType.CREATIVE);
					floor(p, 1024); place(p, 1024);
					p.setAttached(CinnamonState.SITTING, true); p.setAttached(CinnamonState.BOW, true);
					check(CinnamonJournal.of(server).get(p.getUUID()) == null, "Old world has no Cinnamon journal yet");
				});
				write(config, "@singleplayer");
				await(context, world, s -> CinnamonJournal.of(s).get(player(s).getUUID()) != null, 150, "Initial migration creates exactly one saved identity");
				world.getServer().runOnServer(server -> {
					var p = player(server); var entry = CinnamonJournal.of(server).get(p.getUUID()); identity[0] = entry.entity();
					var dog = dog(server, identity[0]);
					check(dog.savedSitting() && dog.wearingBow() && dog.shouldBeSaved(), "Legacy owner sit and bow migrate to a persistent body");
					dog.setNoAi(true);
					p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CinnamonContent.WHISTLE));
					CinnamonContent.WHISTLE.use(p.level(), p, InteractionHand.MAIN_HAND);
					long first = CinnamonJournal.of(server).get(p.getUUID()).whistleUntil();
					p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CinnamonContent.WHISTLE));
					CinnamonContent.WHISTLE.use(p.level(), p, InteractionHand.MAIN_HAND);
					check(CinnamonJournal.of(server).get(p.getUUID()).whistleUntil() == first, "A second whistle copy shares the exact owner cooldown");
				});
				context.waitTicks(10);
				world.getServer().runOnServer(server -> {
					var p = player(server); var dog = dog(server, identity[0]);
					check(!dog.savedSitting() && dog.distanceToSqr(p) < 30, "Actual whistle recalls canonical dog and requests follow");
					long now = server.overworld().getGameTime();
					dog.restoreState(true, true, 2, now + CinnamonDog.GROWTH_TICKS, 20, now + CinnamonDog.RECOVERY_TICKS, now + 10);
					CinnamonCompanion.changed(dog);
					deadlines[0] = dog.growthExpiresAt(); deadlines[1] = dog.recoveryExpiresAt();
					deadlines[2] = CinnamonJournal.of(server).get(p.getUUID()).whistleUntil();
					check(dog.isExhausted() && !dog.isOrderedToSit(), "Saved resting preference does not immobilize recovery");
				});
				saved = world.getWorldSave();
			}
			check(CinnamonCompanion.recoveryTickets() == 0, "Actual server close leaves no retained recovery lease");
			try (var world = saved.open()) {
				context.waitTicks(25);
				await(context, world, s -> s.overworld().getEntityInAnyDimension(identity[0]) instanceof CinnamonDog, 250, "Actual restart loads the same saved UUID");
				world.getServer().runOnServer(server -> {
					var p = player(server); var dog = dog(server, identity[0]);
					check(dog.isOwnedBy(p) && dog.wearingBow() && dog.savedSitting(), "Owner, bow and requested sit survive actual saved-world restart");
					check(dog.growthExpiresAt() == deadlines[0] && dog.recoveryExpiresAt() == deadlines[1], "Restart preserves exact growth and recovery deadlines");
					check(CinnamonJournal.of(server).get(p.getUUID()).whistleUntil() == deadlines[2], "Restart cannot reset shared whistle cooldown");
					check(CinnamonCompanion.recoveryTickets() == 0, "Restart does not restore historical chunk tickets");
					check(dog.isExhausted() && !dog.isOrderedToSit(), "Recovery remains mobile after reopening");
					p.setGameMode(GameType.SURVIVAL); p.kill(p.level());
					check(!p.isAlive(), "Owner actually dies before the respawn identity test");
				});
				context.waitTicks(5);
				context.runOnClient(mc -> { mc.player.respawn(); mc.gui.setScreen(null); });
				context.waitTicks(15);
				world.getServer().runOnServer(server -> {
					var p = player(server); p.setGameMode(GameType.CREATIVE); place(p, 1024);
					var dog = dog(server, identity[0]);
					check(p.isAlive() && dog.isOwnedBy(p) && dog.recoveryExpiresAt() == deadlines[1], "Actual owner respawn retains dog identity, ownership and recovery deadline");
					var nether = server.getLevel(net.minecraft.world.level.Level.NETHER);
					check(nether != null, "Native dimension fixture has the Nether");
					floor(nether, 1024);
					p.teleportTo(nether, 1024.5, 200, .5, Set.<Relative>of(), 0, 0, false);
				});
				await(context, world, s -> s.overworld().getEntityInAnyDimension(identity[0]) instanceof CinnamonDog d && d.level() == player(s).level(), 100,
					"Owner dimension travel moves the original saved UUID instead of spawning a copy");
				world.getServer().runOnServer(server -> {
					var p = player(server); var dog = dog(server, identity[0]);
					check(dog.wearingBow() && dog.growthExpiresAt() == deadlines[0] && dog.recoveryExpiresAt() == deadlines[1], "Dimension transfer preserves equipment and exact timers");
					p.teleportTo(server.overworld(), 1024.5, 200, .5, Set.<Relative>of(), 0, 0, false);
				});
				await(context, world, s -> s.overworld().getEntityInAnyDimension(identity[0]) instanceof CinnamonDog d && d.level() == s.overworld(), 100,
					"Dimension round trip restores the same body identity to the original world");
				world.getServer().runOnServer(server -> {
					var p = player(server); var dog = dog(server, identity[0]);
					// A distant saved sit is deliberately unloaded, then recovered by the actual whistle.
					dog.restoreState(true, true, 1, 0, 0, 0, 0); CinnamonCompanion.changed(dog);
					floor(p, 1536); place(p, 1536);
				});
				await(context, world, s -> s.overworld().getEntityInAnyDimension(identity[0]) == null, 800, "Distant sitting body really unloads before recall");
				int whistleRemaining = world.getServer().computeOnServer(server ->
					(int) Math.max(0, deadlines[2] - server.overworld().getGameTime() + 1));
				if (whistleRemaining > 0) context.waitTicks(whistleRemaining);
				world.getServer().runOnServer(server -> {
					var p = player(server);
					check(CinnamonCompanion.recoveryTickets() == 0, "An intentionally parked dog does not repeatedly load chunks");
					p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CinnamonContent.WHISTLE));
					CinnamonContent.WHISTLE.use(p.level(), p, InteractionHand.MAIN_HAND);
				});
				await(context, world, s -> s.overworld().getEntityInAnyDimension(identity[0]) instanceof CinnamonDog d && d.distanceToSqr(player(s)) < 30, 230, "Bounded chunk recovery returns original unloaded UUID");
				world.getServer().runOnServer(server -> {
					var dog = dog(server, identity[0]);
					check(dog.wearingBow() && !dog.savedSitting(), "Recovered original retains equipment and honors whistle follow");
					check(CinnamonCompanion.recoveryTickets() == 0, "Successful recall removes its temporary ticket");
					dog.kill((net.minecraft.server.level.ServerLevel) dog.level());
					check(!dog.isAlive(), "Explicit administrator kill remains effective");
				});
				context.waitTicks(30);
				world.getServer().runOnServer(server -> {
					var p = player(server); var entry = CinnamonJournal.of(server).get(p.getUUID());
					check(entry.retired() && entry.entity().equals(identity[0]), "Verified administrator removal creates a durable tombstone");
					CinnamonCompanion.whistle(p);
					check(server.overworld().getEntityInAnyDimension(identity[0]) == null, "A whistle never recreates retired Cinnamon");
				});
			}
			try (var world = saved.open()) {
				context.waitTicks(30);
				world.getServer().runOnServer(server -> {
					var p = player(server); var entry = CinnamonJournal.of(server).get(p.getUUID());
					check(entry.retired() && server.overworld().getEntityInAnyDimension(identity[0]) == null, "Tombstone and absence survive actual restart");
					// An unknown registered body must remain unknown, never replaced after a bounded failed search.
					UUID missing = UUID.randomUUID(); identity[0] = missing;
					p.setAttached(CinnamonState.IDENTITY, missing);
					CinnamonJournal.of(server).put(p.getUUID(), new CinnamonJournal.Entry(missing, p.level().dimension().identifier().toString(),
						p.chunkPosition().x(), p.chunkPosition().z(), entry.care(), 0, false));
					CinnamonCompanion.whistle(p);
				});
				context.waitTicks(10);
				world.getServer().runOnServer(server -> check(CinnamonCompanion.recoveryTickets() == 1, "Missing registered body gets one bounded ticket"));
				context.waitTicks(210);
				world.getServer().runOnServer(server -> {
					var entry = CinnamonJournal.of(server).get(player(server).getUUID());
					check(entry.entity().equals(identity[0]) && server.overworld().getEntityInAnyDimension(identity[0]) == null,
						"Failed lookup neither changes identity nor manufactures a companion");
					check(CinnamonCompanion.recoveryTickets() == 0, "Timeout releases the actual ticket");
				});
				context.waitTicks(100);
				world.getServer().runOnServer(server -> check(CinnamonCompanion.recoveryTickets() == 0, "Failed automatic recovery does not permanently refresh itself"));
			}
		} finally {
			try { if (previous == null) Files.deleteIfExists(config); else Files.writeString(config, previous); }
			catch (Exception e) { throw new AssertionError(e); }
		}
	}
	private static ServerPlayer player(MinecraftServer server) { return server.getPlayerList().getPlayers().getFirst(); }
	private static CinnamonDog dog(MinecraftServer server, UUID id) {
		var entity = server.overworld().getEntityInAnyDimension(id);
		check(entity instanceof CinnamonDog, "Expected the original saved Cinnamon body: " + id);
		return (CinnamonDog) entity;
	}
	private static void floor(ServerPlayer player, int x) {
		floor(player.level(), x);
	}
	private static void floor(net.minecraft.server.level.ServerLevel level, int x) {
		for (BlockPos p : BlockPos.betweenClosed(x - 12, 199, -12, x + 12, 199, 12)) level.setBlock(p, Blocks.STONE.defaultBlockState(), 3);
		for (BlockPos p : BlockPos.betweenClosed(x - 12, 200, -12, x + 12, 204, 12)) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
	}
	private static void place(ServerPlayer player, int x) { player.teleportTo(player.level(), x + .5, 200, .5, Set.<Relative>of(), 0, 0, false); }
	private static void write(Path config, String owner) {
		try { Files.createDirectories(config.getParent()); Files.writeString(config, "{\"owner\":\"" + owner + "\"}\n"); }
		catch (Exception e) { throw new AssertionError(e); }
	}
	private static void await(ClientGameTestContext context, TestSingleplayerContext world, Predicate<MinecraftServer> condition, int ticks, String message) {
		for (int i = 0; i < ticks; i += 5) { if (world.getServer().computeOnServer(condition::test)) return; context.waitTicks(5); }
		check(world.getServer().computeOnServer(condition::test), message);
	}
	private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
