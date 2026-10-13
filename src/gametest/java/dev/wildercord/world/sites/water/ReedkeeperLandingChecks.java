package dev.wildercord.world.sites.water;

import dev.wildercord.client.lore.LoreJournalScreen;
import dev.wildercord.lore.LoreEntries;
import dev.wildercord.lore.LoreJournal;
import dev.wildercord.wildlife.MoonreedBlock;
import dev.wildercord.wildlife.WetlandGarden;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Four chunk-clipped orientations, real conversation packets, and saved resident identity. */
public final class ReedkeeperLandingChecks {
	private ReedkeeperLandingChecks() {}

	/** Exercise the registered locator on the existing terrain census without widening it or forcing a biome. */
	public static ReedkeeperLandingPiece verifyLocator(ServerLevel level, BlockPos center) {
		var source = level.getChunkSource();
		var generator = source.getGenerator();
		var randomState = source.randomState();
		var holder = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(
			net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE, dev.wildercord.Wildercord.id("water_reedkeeper_landing")));
		var sampler = randomState.createClimateSampler(net.minecraft.world.level.levelgen.densityfunction.SamplerContext.EMPTY_UNCACHED);
		for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
			var chunk = new ChunkPos((center.getX() >> 4) + dx, (center.getZ() >> 4) + dz);
			var start = holder.value().generate(holder, level.dimension(), level.registryAccess(), generator, generator.getBiomeSource(), sampler,
				randomState, level.getStructureTemplateManager(), level.getSeed(), chunk, 0, level, holder.value().biomes()::contains);
			if (!start.isValid()) continue;
			check(start.getPieces().size() == 1 && start.getPieces().getFirst() instanceof ReedkeeperLandingPiece, "The registered regional structure admits its authored landing");
			dev.wildercord.Wildercord.LOGGER.info("REEDKEEPER_LOCATOR seed={}, chunk={}, bounds={}", level.getSeed(), chunk, start.getBoundingBox());
			return (ReedkeeperLandingPiece) start.getPieces().getFirst();
		}
		throw new AssertionError("No valid Reedkeeper landing in the original 49-chunk natural Reedlands census");
	}

	/** The real locator chooses the spot; only the authored structure is placed, with no terrain fixture. */
	public static void captureNatural(ClientGameTestContext context, TestSingleplayerContext world, BlockPos center) {
		world.getServer().runCommand("time set 6000");
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			var piece = verifyLocator(level, center);
			var box = piece.getBoundingBox();
			for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) level.getChunk(x, z);
			place(level, piece, false);
			var player = server.getPlayerList().getPlayers().getFirst();
			var target = Vec3.atCenterOf(piece.localPosition(9, 11, 9));
			var eye = Vec3.atCenterOf(piece.localPosition(26, 25, -16));
			var delta = target.subtract(eye);
			player.setGameMode(GameType.SPECTATOR);
			player.teleportTo(level, eye.x, eye.y, eye.z, Set.of(), (float) Math.toDegrees(Math.atan2(-delta.x, delta.z)),
				(float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())), false);
		});
		context.waitTicks(25);
		world.getConnection().waitForChunksRender();
		boolean hidden = context.computeOnClient(mc -> mc.gui.hud.isHidden());
		try {
			context.runOnClient(mc -> { mc.gui.toastManager().clear(); mc.gui.hud.getChat().clearMessages(false); if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle(); });
			context.takeScreenshot(TestScreenshotOptions.of("reedkeeper_landing_terrain").disableCounterPrefix());
		} finally {
			context.runOnClient(mc -> { if (mc.gui.hud.isHidden() != hidden) mc.gui.hud.toggle(); });
		}
	}
	public static void run(ClientGameTestContext context) {
		net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave save;
		UUID identity;
		BlockPos home;
		boolean hidden = context.computeOnClient(mc -> mc.gui.hud.isHidden());
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(25);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("time set 6000");
			ReedkeeperLandingPiece[] pieces = world.getServer().computeOnServer(server -> {
				var level = server.overworld();
				var built = new ReedkeeperLandingPiece[4];
				int n = 0;
				for (var facing : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
					var piece = new ReedkeeperLandingPiece(96 + n * 64, 90, 96, facing);
					var box = piece.getBoundingBox();
					server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
						"forceload add " + box.minX() + " " + box.minZ() + " " + box.maxX() + " " + box.maxZ());
					for (int x = box.minX() - 2; x <= box.maxX() + 2; x++) for (int z = box.minZ() - 2; z <= box.maxZ() + 2; z++) {
						for (int y = 90; y <= box.maxY() + 1; y++) level.setBlock(new BlockPos(x, y, z),
							y == 90 ? Blocks.MUD.defaultBlockState() : y <= 96 ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
					}
					built[n++] = piece;
				}
				return built;
			});
			// Forced terrain must reach entity-ticking status before synchronous resident inspection.
			context.waitTicks(20);
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				for (int n = 0; n < pieces.length; n++) {
					var piece = pieces[n];
					place(level, piece, n % 2 == 1);
					verify(level, piece);
					// A replay of every chunk on a restored piece must not mint a second resident.
					var tag = piece.createTag(StructurePieceSerializationContext.fromLevel(level));
					check(tag.getBooleanOr("ReedkeeperPlaced", false), "Resident success is written to the piece");
					place(level, new ReedkeeperLandingPiece(tag), n % 2 == 0);
					check(residents(level, piece).size() == 1, "Restored generation cannot duplicate the resident in " + piece.getOrientation());
				}
			});
			home = pieces[0].localPosition(7, ReedkeeperLandingPiece.DECK + 1, 11);
			identity = world.getServer().computeOnServer(server -> {
				var resident = residents(server.overworld(), pieces[0]).getFirst();
				var player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.CREATIVE);
				player.teleportTo(server.overworld(), resident.getX(), resident.getY(), resident.getZ() - 2, Set.of(), 0, 0, false);
				return resident.getUUID();
			});
			context.waitTicks(20);
			context.getInput().holdKey(options -> options.keyShift);
			context.waitTicks(4);
			for (String line : List.of("greet", "garden", "crossing", "ferryman")) {
				interact(context, world, identity);
				world.getServer().runOnServer(server -> check(LoreJournal.data(server.getPlayerList().getPlayers().getFirst()).has("said:reedkeeper." + line), "Real sneak-use records " + line));
				int before = world.getServer().computeOnServer(server -> LoreJournal.data(server.getPlayerList().getPlayers().getFirst()).entries().size());
				interact(context, world, identity);
				world.getServer().runOnServer(server -> check(LoreJournal.data(server.getPlayerList().getPlayers().getFirst()).entries().size() == before, "Repeated packet respects the conversation pause"));
				context.waitTicks(ReedkeeperResident.TALK_COOLDOWN);
			}
			context.getInput().releaseKey(options -> options.keyShift);
			context.waitTicks(3);
			interact(context, world, identity);
			context.waitFor(mc -> mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.MerchantScreen, 100);
			context.runOnClient(mc -> {
				check(LoreJournal.data(mc.player).has(ReedkeeperResident.ENTRY), "Owner receives the resident identity");
				check(LoreEntries.tab(ReedkeeperResident.ENTRY).equals("people"), "Resident appears among people");
				check(LoreJournalScreen.describe("said:reedkeeper.crossing").getString().contains("Tideward Reedkeeper"), "Saved dialogue resolves its actual speaker");
				mc.player.closeContainer();
			});
			world.getServer().runOnServer(server -> {
				for (var piece : pieces) verify(server.overworld(), piece);
				var player = server.getPlayerList().getPlayers().getFirst();
				var target = Vec3.atCenterOf(pieces[0].localPosition(9, 11, 9));
				var eye = Vec3.atCenterOf(pieces[0].localPosition(-10, 25, -12));
				var delta = target.subtract(eye);
				player.setGameMode(GameType.SPECTATOR);
				player.teleportTo(server.overworld(), eye.x, eye.y, eye.z, Set.of(), (float) Math.toDegrees(Math.atan2(-delta.x, delta.z)),
					(float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())), false);
			});
			context.waitTicks(20);
			world.getConnection().waitForChunksRender();
			context.runOnClient(mc -> { mc.gui.toastManager().clear(); mc.gui.hud.getChat().clearMessages(false); if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle(); });
			context.takeScreenshot(TestScreenshotOptions.of("reedkeeper_landing").disableCounterPrefix());
			save = world.getWorldSave();
		} finally {
			context.getInput().releaseKey(options -> options.keyShift);
			context.runOnClient(mc -> { if (mc.gui.hud.isHidden() != hidden) mc.gui.hud.toggle(); });
		}
		try (var world = save.open()) {
			world.getServer().runOnServer(server -> {
				var level = server.overworld(); level.getChunkAt(home);
				check(level.getEntity(identity) instanceof Villager resident && resident.entityTags().contains(ReedkeeperResident.TAG), "Original resident UUID and role survive reopening");
				var data = LoreJournal.data(server.getPlayerList().getPlayers().getFirst());
				check(data.has(ReedkeeperResident.ENTRY) && data.has("said:reedkeeper.ferryman"), "The person and all heard stories persist");
			});
		}
	}

	private static void interact(ClientGameTestContext context, TestSingleplayerContext world, UUID uuid) {
		int id = world.getServer().computeOnServer(server -> {
			var resident = server.overworld().getEntity(uuid);
			server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(), resident.getX(), resident.getY(), resident.getZ() - 2, Set.of(), 0, 0, false);
			return resident.getId();
		});
		context.waitTicks(3);
		context.runOnClient(mc -> {
			var entity = mc.level.getEntity(id);
			check(entity != null && entity.getUUID().equals(uuid), "Interaction targets the actual tracked resident");
			mc.gameMode.interact(mc.player, entity, new EntityHitResult(entity, entity.getBoundingBox().getCenter()), InteractionHand.MAIN_HAND);
		});
		context.waitTicks(5);
	}

	private static void place(ServerLevel level, ReedkeeperLandingPiece piece, boolean reverse) {
		var box = piece.getBoundingBox();
		var chunks = new ArrayList<ChunkPos>();
		for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) chunks.add(new ChunkPos(x, z));
		if (reverse) Collections.reverse(chunks);
		for (var chunk : chunks) piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(41),
			new BoundingBox(chunk.getMinBlockX(), box.minY(), chunk.getMinBlockZ(), chunk.getMaxBlockX(), box.maxY(), chunk.getMaxBlockZ()), chunk, BlockPos.ZERO);
	}

	private static List<Villager> residents(ServerLevel level, ReedkeeperLandingPiece piece) {
		var b = piece.getBoundingBox();
		return level.getEntitiesOfClass(Villager.class, new AABB(b.minX(), b.minY(), b.minZ(), b.maxX() + 1, b.maxY() + 1, b.maxZ() + 1).inflate(8), v -> v.entityTags().contains(ReedkeeperResident.TAG));
	}

	private static void verify(ServerLevel level, ReedkeeperLandingPiece piece) {
		int deck = ReedkeeperLandingPiece.DECK;
		check(residents(level, piece).size() == 1, "Exactly one resident belongs to each landing at " + piece.getBoundingBox() + "; found " + residents(level, piece).size());
		var resident = residents(level, piece).getFirst();
		check(resident.getVillagerData().profession().is(VillagerProfession.FISHERMAN) && !resident.getOffers().isEmpty(), "The keeper has real fishing trades");
		check(level.getBlockState(piece.localPosition(7, deck + 1, 8)).isAir() && level.getBlockState(piece.localPosition(7, deck + 2, 8)).isAir(), "The survey-house entrance stays open");
		var block = level.getBlockEntity(piece.localPosition(5, deck + 1, 14));
		check(block instanceof LecternBlockEntity, "The house contains its lectern");
		var book = ((LecternBlockEntity) block).getBook().get(DataComponents.WRITTEN_BOOK_CONTENT);
		check(book != null && book.pages().size() == 4 && book.author().equals("Tideward surveyors"), "The attributed four-page crossing legend is on the lectern");
		for (int x : new int[] {13, 15}) for (int z : new int[] {11, 13}) {
			var at = piece.localPosition(x, deck + 1, z);
			check(level.getBlockState(at).is(WetlandGarden.REED) && level.getBlockState(at).canSurvive(level, at)
				&& MoonreedBlock.moist(level, at) && MoonreedBlock.openSky(level, at), "Every cultivated bud has real soil, water and sky in " + piece.getOrientation());
		}
	}

	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
