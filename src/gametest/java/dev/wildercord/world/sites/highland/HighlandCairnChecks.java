package dev.wildercord.world.sites.highland;

import dev.wildercord.Wildercord;
import dev.wildercord.client.lore.LoreJournalScreen;
import dev.wildercord.lore.LoreEntries;
import dev.wildercord.lore.LoreJournal;
import dev.wildercord.lore.LoreQuestRules;
import dev.wildercord.wildlife.HighlandContent;
import dev.wildercord.wildlife.WindreedBlock;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Native client checks for Highland Cairn Lodge (BLD-03), Highland Cairn Keeper (NPC-03),
 * and the 3-chapter Paired Cairns / Ridge Watch / Lamp on the Ridge journal route (LOR-03).
 */
public final class HighlandCairnChecks {
	private HighlandCairnChecks() {}

	public static CairnLodgePiece verifyLocator(ServerLevel level, BlockPos center) {
		var source = level.getChunkSource();
		var generator = source.getGenerator();
		var randomState = source.randomState();
		var lodgeHolder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(
			ResourceKey.create(Registries.STRUCTURE, Wildercord.id(HighlandSites.CAIRN_LODGE)));
		var ringHolder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(
			ResourceKey.create(Registries.STRUCTURE, Wildercord.id(HighlandSites.SURVEY_RING)));
		var sampler = randomState.createClimateSampler(SamplerContext.EMPTY_UNCACHED);
		CairnLodgePiece foundLodge = null;
		boolean foundRing = false;
		for (int dx = -6; dx <= 6 && (foundLodge == null || !foundRing); dx++) {
			for (int dz = -6; dz <= 6 && (foundLodge == null || !foundRing); dz++) {
				var chunk = new ChunkPos((center.getX() >> 4) + dx, (center.getZ() >> 4) + dz);
				if (foundLodge == null) {
					var start = lodgeHolder.value().generate(lodgeHolder, level.dimension(), level.registryAccess(), generator,
						generator.getBiomeSource(), sampler, randomState, level.getStructureTemplateManager(), level.getSeed(),
						chunk, 0, level, lodgeHolder.value().biomes()::contains);
					if (start.isValid()) {
						check(start.getPieces().size() == 1 && start.getPieces().getFirst() instanceof CairnLodgePiece,
							"Registered Highland Cairn Lodge structure admits its authored piece");
						foundLodge = (CairnLodgePiece) start.getPieces().getFirst();
					}
				}
				if (!foundRing) {
					var start = ringHolder.value().generate(ringHolder, level.dimension(), level.registryAccess(), generator,
						generator.getBiomeSource(), sampler, randomState, level.getStructureTemplateManager(), level.getSeed(),
						chunk, 0, level, ringHolder.value().biomes()::contains);
					if (start.isValid()) {
						check(start.getPieces().size() == 1 && start.getPieces().getFirst() instanceof SurveyRingPiece,
							"Registered Highland Survey Ring structure admits its authored piece");
						foundRing = true;
					}
				}
			}
		}
		check(foundLodge != null, "Highland Cairn Lodge locator finds valid highland shelf terrain in the Windscar survey");
		check(foundRing, "Highland Survey Ring locator finds valid highland terrace terrain in the Windscar survey");
		return foundLodge;
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
			CairnLodgePiece[] pieces = world.getServer().computeOnServer(server -> {
				var level = server.overworld();
				var built = new CairnLodgePiece[4];
				int n = 0;
				for (var facing : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
					var piece = new CairnLodgePiece(96 + n * 64, 90, 96, facing);
					var box = piece.getBoundingBox();
					server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
						"forceload add " + box.minX() + " " + box.minZ() + " " + box.maxX() + " " + box.maxZ());
					for (int x = box.minX() - 2; x <= box.maxX() + 2; x++) for (int z = box.minZ() - 2; z <= box.maxZ() + 2; z++) {
						for (int y = 90; y <= box.maxY() + 1; y++) level.setBlock(new BlockPos(x, y, z),
							y <= 91 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
					}
					built[n++] = piece;
				}
				return built;
			});
			context.waitTicks(20);
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				for (int n = 0; n < pieces.length; n++) {
					var piece = pieces[n];
					place(level, piece, n % 2 == 1);
					verify(level, piece);
					var tag = piece.createTag(StructurePieceSerializationContext.fromLevel(level));
					check(tag.getBooleanOr("CairnKeeperPlaced", false), "Resident placement flag is written to CairnLodgePiece");
					place(level, new CairnLodgePiece(tag), n % 2 == 0);
					check(residents(level, piece).size() == 1, "Restored generation cannot duplicate the resident in " + piece.getOrientation());
				}
				register(level, HighlandSites.CAIRN_LODGE, pieces[0]);
			});
			home = pieces[0].localPosition(9, CairnLodgePiece.DECK + 1, 11);
			identity = world.getServer().computeOnServer(server -> {
				var resident = residents(server.overworld(), pieces[0]).getFirst();
				var player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(server.overworld(), resident.getX(), resident.getY(), resident.getZ() + 2, Set.of(), 180, 0, false);
				LoreJournal.poll(player);
				check(LoreJournal.data(player).has("place:" + HighlandSites.CAIRN_LODGE), "Entering authentic Cairn Lodge records place:highland_cairn_lodge");
				return resident.getUUID();
			});
			context.waitTicks(20);
			context.getInput().holdKey(options -> options.keyShift);
			context.waitTicks(4);
			for (String line : CairnKeeperResident.LINES) {
				interact(context, world, identity);
				world.getServer().runOnServer(server -> check(LoreJournal.data(server.getPlayerList().getPlayers().getFirst()).has("said:cairn_keeper." + line),
					"Real sneak-use records " + line));
				int before = world.getServer().computeOnServer(server -> LoreJournal.data(server.getPlayerList().getPlayers().getFirst()).entries().size());
				interact(context, world, identity);
				world.getServer().runOnServer(server -> check(LoreJournal.data(server.getPlayerList().getPlayers().getFirst()).entries().size() == before,
					"Repeated packet respects the conversation pause"));
				context.waitTicks(CairnKeeperResident.TALK_COOLDOWN);
			}
			context.getInput().releaseKey(options -> options.keyShift);
			context.waitTicks(3);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				LoreJournal.poll(p);
				check(LoreJournal.data(p).stage("paired_cairns") == LoreQuestRules.DONE, "Hearing the 4th Cairn Keeper story completes paired_cairns");
				LoreJournal.poll(p);
				check(LoreJournal.data(p).stage("ridge_orders") == LoreQuestRules.ACTIVE, "Next poll opens ridge_orders");
			});
			hear(context, world, identity, "watch_orders");
			interact(context, world, identity);
			context.waitFor(mc -> mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.MerchantScreen, 100);
			context.runOnClient(mc -> {
				check(LoreJournal.data(mc.player).has(CairnKeeperResident.ENTRY), "Owner receives Cairn Keeper identity");
				check(LoreEntries.tab(CairnKeeperResident.ENTRY).equals("people"), "Cairn Keeper appears among people");
				check(LoreJournalScreen.describe("said:cairn_keeper.cairns").getString().contains("Highland Cairn Keeper"),
					"Saved dialogue resolves its speaker");
				mc.player.closeContainer();
			});
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				LoreJournal.record(p, "place:travel_watchtower", true);
				LoreJournal.poll(p);
				check(LoreJournal.data(p).stage("ridge_orders") == LoreQuestRules.DONE, "Watchtower visit completes ridge_orders");
				LoreJournal.poll(p);
				check(LoreJournal.data(p).stage("cairn_beacon") == LoreQuestRules.ACTIVE, "Next poll opens cairn_beacon");
				check(!LoreJournal.data(p).has(HighlandSites.WIND_MARK_ENTRY), "Unlit cairn lamp does not complete cairn_beacon");
			});
			hear(context, world, identity, "cairn_light");
			// Position player beside the eastern cairn lever and flip it via real client interaction.
			BlockPos lever = pieces[0].leverPos();
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				p.teleportTo(server.overworld(), lever.getX() + 0.5, lever.getY() - 1, lever.getZ() + 1.5, Set.of(), 180, -15, false);
			});
			context.waitTicks(6);
			context.runOnClient(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(lever), Direction.SOUTH, lever, false)));
			context.waitTicks(6);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(server.overworld().getBlockState(pieces[0].lampPos()).getValue(RedstoneLampBlock.LIT), "Flipping the cairn lever lights the eastern cairn lamp");
				LoreJournal.poll(p);
				check(LoreJournal.data(p).has(HighlandSites.WIND_MARK_ENTRY), "Standing beside the lit cairn lamp records learned:scout_wind_mark");
				check(LoreJournal.data(p).stage("cairn_beacon") == LoreQuestRules.DONE, "Lighting the cairn lamp completes cairn_beacon");
			});
			hear(context, world, identity, "light_return");
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				var target = Vec3.atCenterOf(pieces[0].localPosition(10, CairnLodgePiece.DECK + 2, 10));
				var eye = Vec3.atCenterOf(pieces[0].localPosition(10, CairnLodgePiece.DECK + 10, 26));
				var delta = target.subtract(eye);
				player.setGameMode(GameType.SPECTATOR);
				player.teleportTo(server.overworld(), eye.x, eye.y, eye.z, Set.of(), (float) Math.toDegrees(Math.atan2(-delta.x, delta.z)),
					(float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())), false);
			});
			context.waitTicks(20);
			world.getConnection().waitForChunksRender();
			context.runOnClient(mc -> { mc.gui.toastManager().clear(); mc.gui.hud.getChat().clearMessages(false); if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle(); });
			context.takeScreenshot(TestScreenshotOptions.of("highland_cairn_lodge").disableCounterPrefix());
			save = world.getWorldSave();
		} finally {
			context.getInput().releaseKey(options -> options.keyShift);
			context.runOnClient(mc -> { if (mc.gui.hud.isHidden() != hidden) mc.gui.hud.toggle(); });
		}
		try (var world = save.open()) {
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				level.getChunkAt(home);
				check(level.getEntity(identity) instanceof Villager resident && resident.entityTags().contains(CairnKeeperResident.TAG),
					"Original Cairn Keeper UUID and role survive reopening");
				var data = LoreJournal.data(server.getPlayerList().getPlayers().getFirst());
				check(data.has(CairnKeeperResident.ENTRY) && data.has("said:cairn_keeper.cartographer"), "Cairn Keeper and stories persist");
				check(data.has("said:cairn_keeper.watch_orders") && data.has("said:cairn_keeper.cairn_light") && data.has("said:cairn_keeper.light_return"),
					"Progress-aware return conversations survive reopening");
				check(data.stage("paired_cairns") == LoreQuestRules.DONE && data.stage("ridge_orders") == LoreQuestRules.DONE
					&& data.stage("cairn_beacon") == LoreQuestRules.DONE, "Completed 3-chapter Highland Cairn route persists across reopening");
			});
		}
	}

	public static void hear(ClientGameTestContext context, TestSingleplayerContext world, UUID uuid, String line) {
		context.waitTicks(CairnKeeperResident.TALK_COOLDOWN);
		context.getInput().holdKey(options -> options.keyShift);
		context.waitTicks(4);
		try {
			interact(context, world, uuid);
			world.getServer().runOnServer(server -> check(LoreJournal.data(server.getPlayerList().getPlayers().getFirst()).has("said:cairn_keeper." + line),
				"Progress-aware conversation records " + line));
			context.waitFor(mc -> LoreJournal.data(mc.player).has("said:cairn_keeper." + line), 100);
			context.runOnClient(mc -> {
				String text = LoreJournalScreen.describe("said:cairn_keeper." + line).getString();
				check(text.contains("Highland Cairn Keeper") && !text.contains("dialogue.wildercord."),
					"Saved return dialogue resolves its speaker and complete translated text");
			});
			int entries = world.getServer().computeOnServer(server -> LoreJournal.data(server.getPlayerList().getPlayers().getFirst()).entries().size());
			interact(context, world, uuid);
			world.getServer().runOnServer(server -> check(LoreJournal.data(server.getPlayerList().getPlayers().getFirst()).entries().size() == entries,
				"Return conversation respects the packet cooldown"));
		} finally {
			context.getInput().releaseKey(options -> options.keyShift);
			context.waitTicks(3);
		}
	}

	private static void interact(ClientGameTestContext context, TestSingleplayerContext world, UUID uuid) {
		int id = world.getServer().computeOnServer(server -> {
			var resident = server.overworld().getEntity(uuid);
			server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(), resident.getX(), resident.getY(), resident.getZ() + 2, Set.of(), 180, 0, false);
			return resident.getId();
		});
		context.waitTicks(3);
		context.waitFor(mc -> mc.level.getEntity(id) != null && mc.level.getEntity(id).getUUID().equals(uuid), 100);
		context.runOnClient(mc -> {
			var entity = mc.level.getEntity(id);
			check(entity != null && entity.getUUID().equals(uuid), "Interaction targets the actual tracked Cairn Keeper");
			mc.gameMode.interact(mc.player, entity, new EntityHitResult(entity, entity.getBoundingBox().getCenter()), InteractionHand.MAIN_HAND);
		});
		context.waitTicks(5);
	}

	private static void place(ServerLevel level, CairnLodgePiece piece, boolean reverse) {
		var box = piece.getBoundingBox();
		var chunks = new ArrayList<ChunkPos>();
		for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) chunks.add(new ChunkPos(x, z));
		if (reverse) Collections.reverse(chunks);
		for (var chunk : chunks) piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(41),
			new BoundingBox(chunk.getMinBlockX(), box.minY(), chunk.getMinBlockZ(), chunk.getMaxBlockX(), box.maxY(), chunk.getMaxBlockZ()), chunk, BlockPos.ZERO);
	}

	public static void register(ServerLevel level, String siteId, HighlandPiece piece) {
		var box = piece.getBoundingBox();
		var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(ResourceKey.create(Registries.STRUCTURE, Wildercord.id(siteId)));
		var origin = new ChunkPos(box.minX() >> 4, box.minZ() >> 4);
		level.getChunk(origin.x(), origin.z()).setStartForStructure(structure, new StructureStart(structure, origin, 0, new PiecesContainer(List.of(piece))));
		for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++)
			level.getChunk(x, z).addReferenceForStructure(structure, origin.pack());
	}

	private static List<Villager> residents(ServerLevel level, CairnLodgePiece piece) {
		var b = piece.getBoundingBox();
		return level.getEntitiesOfClass(Villager.class, new AABB(b.minX(), b.minY(), b.minZ(), b.maxX() + 1, b.maxY() + 1, b.maxZ() + 1).inflate(8),
			v -> v.entityTags().contains(CairnKeeperResident.TAG));
	}

	private static void verify(ServerLevel level, CairnLodgePiece piece) {
		int deck = CairnLodgePiece.DECK;
		check(residents(level, piece).size() == 1, "Exactly one Cairn Keeper belongs to each lodge at " + piece.getBoundingBox());
		var resident = residents(level, piece).getFirst();
		check(resident.getVillagerData().profession().is(VillagerProfession.CARTOGRAPHER) && !resident.getOffers().isEmpty(), "The keeper has real cartographer trades");
		var block = level.getBlockEntity(piece.localPosition(12, deck + 1, 9));
		check(block instanceof LecternBlockEntity, "The lodge contains its lectern");
		var book = ((LecternBlockEntity) block).getBook().get(DataComponents.WRITTEN_BOOK_CONTENT);
		check(book != null && book.pages().size() == 4 && book.author().equals("Marchkeeper Survey"), "The attributed four-page Paired Cairns book is on the lectern");
		for (int z : new int[]{6, 8, 10, 11}) {
			var at = piece.localPosition(4, deck + 1, z);
			check(level.getBlockState(at).is(HighlandContent.REED) && level.getBlockState(at).getValue(WindreedBlock.AGE) >= 1,
				"Every leeward Windreed plant is established in " + piece.getOrientation());
		}
	}

	private static void check(boolean ok, String msg) {
		if (!ok) throw new AssertionError(msg);
	}
}
