package dev.wildercord.pet;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/** Disposable-save interrupted-journal and conflicting-marker checks, followed by proven-original recovery. */
public final class CinnamonIdentityChecks {
	private CinnamonIdentityChecks() {}
	public static void run(ClientGameTestContext context) {
		Path config = FabricLoader.getInstance().getConfigDir().resolve("wildercord-cinnamon.json");
		String previous = read(config);
		try {
			write(config, "{\"owner\":\"\"}");
			UUID[] id = new UUID[2]; Path[] journalFile = new Path[1]; TestWorldSave saved;
			try (var world = context.worldBuilder().create()) {
				context.waitTicks(25);
				world.getServer().runCommand("gamerule spawn_mobs false");
				world.getServer().runOnServer(server -> {
					var p = player(server); p.setGameMode(GameType.CREATIVE);
					for (BlockPos at : BlockPos.betweenClosed(1012, 199, -12, 1036, 199, 12)) p.level().setBlock(at, Blocks.STONE.defaultBlockState(), 3);
					for (BlockPos at : BlockPos.betweenClosed(1012, 200, -12, 1036, 204, 12)) p.level().setBlock(at, Blocks.AIR.defaultBlockState(), 3);
					p.teleportTo(p.level(), 1024.5, 200, .5, Set.<Relative>of(), 0, 0, false);
					check(p.getAttached(CinnamonState.IDENTITY) == null, "Fresh legacy fixture has no independent identity marker");
				});
				write(config, "{\"owner\":\"@singleplayer\",\"bell\":true}");
				await(context, world, s -> player(s).getAttached(CinnamonState.IDENTITY) != null, 150, "Initial registration writes the independent marker");
				world.getServer().runOnServer(server -> {
					var p = player(server); id[0] = p.getAttached(CinnamonState.IDENTITY);
					var dog = dog(server, id[0]); dog.setNoAi(true); dog.setOrderedToSit(true); CinnamonCompanion.changed(dog);
					journalFile[0] = Wildercord.id("cinnamon_companions.dat").resolveAgainst(server.getWorldPath(LevelResource.ROOT).resolve("data"));
				});
				write(config, "{\"owner\":\"00000000-0000-0000-0000-000000000042\",\"bell\":{}}");
				context.waitTicks(110);
				world.getServer().runOnServer(server -> {
					var p = player(server);
					check(CinnamonCompanion.configuredOwner(server) == p && CinnamonCompanion.bell(), "Malformed reload retains complete last valid owner and bell state");
					check(id[0].equals(p.getAttached(CinnamonState.IDENTITY)) && CinnamonCompanion.active(dog(server, id[0])), "Malformed config cannot rebind the saved identity");
				});
				write(config, "{\"owner\":\"@singleplayer\",\"bell\":false}");
				context.waitTicks(110);
				world.getServer().runOnServer(server -> check(!CinnamonCompanion.bell(), "A later complete valid config applies normally"));
				saved = world.getWorldSave();
			}
			byte[] originalJournal;
			try {
				check(Files.isRegularFile(journalFile[0]), "Actual fixture journal path exists after clean close");
				originalJournal = Files.readAllBytes(journalFile[0]);
				// Deliberately truncate only this disposable world's journal, simulating an interrupted compressed write.
				Files.write(journalFile[0], new byte[]{0x1f, (byte) 0x8b, 0x08});
			} catch (Exception e) { throw new AssertionError("Cannot prepare disposable interrupted-write fixture", e); }
			try (var world = saved.open()) {
				await(context, world, s -> s.overworld().getEntityInAnyDimension(id[0]) instanceof CinnamonDog, 150, "Intact entity data remains present despite truncated journal");
				world.getServer().runOnServer(server -> {
					var p = player(server); var dog = dog(server, id[0]);
					check(id[0].equals(p.getAttached(CinnamonState.IDENTITY)), "Independent marker survives a real interrupted journal reopen");
					check(CinnamonJournal.of(server).get(p.getUUID()) == null && dog.isAlive() && dog.isOwnedBy(p) && !CinnamonCompanion.active(dog),
						"Unknown owned saved body is quarantined without deletion, rebinding or new registration");
					CinnamonCompanion.whistle(p);
				});
				context.waitTicks(220);
				world.getServer().runOnServer(server -> {
					var p = player(server);
					check(CinnamonJournal.of(server).get(p.getUUID()) == null && dog(server, id[0]).isAlive() && CinnamonCompanion.recoveryTickets() == 0,
						"Missing authority cannot produce a copy or a speculative chunk ticket");
					check(p.level().getEntitiesOfClass(CinnamonDog.class, p.getBoundingBox().inflate(24)).size() == 1, "Only the original quarantined body exists");
				});
			}
			try { Files.write(journalFile[0], originalJournal); }
			catch (Exception e) { throw new AssertionError("Cannot restore exact fixture journal backup", e); }
			try (var world = saved.open()) {
				await(context, world, s -> s.overworld().getEntityInAnyDimension(id[0]) instanceof CinnamonDog d && CinnamonCompanion.active(d), 150,
					"Restoring matching proven journal reactivates the same original entity");
				world.getServer().runOnServer(server -> {
					var p = player(server); p.setAttached(CinnamonState.IDENTITY, new UUID(42, 42)); CinnamonCompanion.whistle(p);
					check(!CinnamonCompanion.active(dog(server, id[0])) && CinnamonJournal.of(server).get(p.getUUID()).entity().equals(id[0]),
						"Conflicting marker stops authority without rewriting either identity");
				});
			}
			try (var world = saved.open()) {
				await(context, world, s -> s.overworld().getEntityInAnyDimension(id[0]) instanceof CinnamonDog, 150, "Conflicting marker cannot delete original body on restart");
				world.getServer().runOnServer(server -> {
					var p = player(server); var dog = dog(server, id[0]);
					check(new UUID(42, 42).equals(p.getAttached(CinnamonState.IDENTITY)) && !CinnamonCompanion.active(dog), "Restart does not silently repair an unproven conflicting marker");
					check(CinnamonJournal.of(server).get(p.getUUID()).entity().equals(id[0]) && CinnamonCompanion.recoveryTickets() == 0, "No replacement identity or ticket while records disagree");
					p.setAttached(CinnamonState.IDENTITY, id[0]);
				});
				await(context, world, s -> CinnamonCompanion.active(dog(s, id[0])), 30, "Administrator's proven original marker restores the same body");
				world.getServer().runOnServer(server -> {
					var p = player(server); p.removeAttached(CinnamonState.IDENTITY);
					var intact = CinnamonContent.CINNAMON.create(p.level(), net.minecraft.world.entity.EntitySpawnReason.EVENT);
					check(intact != null, "Create intact owned-body conflict fixture");
					intact.bind(p); intact.setNoAi(true); intact.snapTo(1028.5, 200, 4.5, 0, 0); id[1] = intact.getUUID();
					check(p.level().addFreshEntity(intact) && !intact.isRemoved(), "Journal A with absent marker preserves differing owned body B on first load");
				});
				context.waitTicks(10);
				world.getServer().runOnServer(server -> {
					var p = player(server); var intact = dog(server, id[1]);
					check(id[0].equals(p.getAttached(CinnamonState.IDENTITY)), "Intact journal reconstructs only its own missing marker A");
					// Explicitly exercise the same load hook again after marker reconstruction; the next block also performs a real save reopen.
					net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.invoker().onLoad(intact, p.level());
					check(intact.isAlive() && !intact.isRemoved() && !CinnamonCompanion.active(intact), "Reconstructed marker A is not independent authority to delete owned body B");
				});
			}
			try (var world = saved.open()) {
				await(context, world, s -> s.overworld().getEntityInAnyDimension(id[1]) instanceof CinnamonDog, 150, "Conflicting owned body B survives actual saved-world reopen");
				world.getServer().runOnServer(server -> {
					var p = player(server); var intact = dog(server, id[1]);
					check(id[0].equals(p.getAttached(CinnamonState.IDENTITY)) && CinnamonJournal.of(server).get(p.getUUID()).entity().equals(id[0]), "Reopen retains marker and journal A without adopting B");
					check(intact.isAlive() && intact.isOwnedBy(p) && !CinnamonCompanion.active(intact), "Conflicting intact body remains preserved and inactive");
				});
			}
		} finally { restore(config, previous); }
	}
	private static ServerPlayer player(MinecraftServer s) { return s.getPlayerList().getPlayers().getFirst(); }
	private static CinnamonDog dog(MinecraftServer s, UUID id) {
		var entity = s.overworld().getEntityInAnyDimension(id);
		check(entity instanceof CinnamonDog, "Expected preserved Cinnamon UUID " + id);
		return (CinnamonDog) entity;
	}
	private static void await(ClientGameTestContext c, TestSingleplayerContext w, Predicate<MinecraftServer> condition, int ticks, String message) {
		for (int i = 0; i < ticks; i += 5) { if (w.getServer().computeOnServer(condition::test)) return; c.waitTicks(5); }
		check(w.getServer().computeOnServer(condition::test), message);
	}
	private static String read(Path path) { try { return Files.exists(path) ? Files.readString(path) : null; } catch (Exception e) { throw new AssertionError(e); } }
	private static void write(Path path, String text) { try { Files.createDirectories(path.getParent()); Files.writeString(path, text); } catch (Exception e) { throw new AssertionError(e); } }
	private static void restore(Path path, String text) { try { if (text == null) Files.deleteIfExists(path); else Files.writeString(path, text); } catch (Exception e) { throw new AssertionError(e); } }
	private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
