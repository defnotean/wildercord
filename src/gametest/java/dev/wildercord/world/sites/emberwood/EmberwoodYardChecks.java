package dev.wildercord.world.sites.emberwood;

import dev.wildercord.Wildercord;
import dev.wildercord.client.lore.LoreJournalScreen;
import dev.wildercord.lore.LoreEntries;
import dev.wildercord.lore.LoreJournal;
import dev.wildercord.lore.LoreQuestRules;
import dev.wildercord.wildlife.CinderFernBlock;
import dev.wildercord.wildlife.EmberContent;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
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
 * Native client checks for Emberwood Charcoal Yard (BLD-04), Emberwood Kiln Keeper (NPC-04),
 * and the 3-chapter Eleven Winters at the Gate / Forge Records / Kiln Draft Lamp journal route (LOR-04).
 */
public final class EmberwoodYardChecks {
	private EmberwoodYardChecks() {}

	public static CharcoalYardPiece verifyLocator(ServerLevel level, BlockPos center) {
		var source = level.getChunkSource();
		var generator = source.getGenerator();
		var randomState = source.randomState();
		var yardHolder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(
			ResourceKey.create(Registries.STRUCTURE, Wildercord.id(EmberwoodSites.CHARCOAL_YARD)));
		var courtHolder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(
			ResourceKey.create(Registries.STRUCTURE, Wildercord.id(EmberwoodSites.KILN_COURT)));
		var sampler = randomState.createClimateSampler(SamplerContext.EMPTY_UNCACHED);
		CharcoalYardPiece foundYard = null;
		boolean foundCourt = false;
		for (int dx = -6; dx <= 6 && (foundYard == null || !foundCourt); dx++) {
			for (int dz = -6; dz <= 6 && (foundYard == null || !foundCourt); dz++) {
				var chunk = new ChunkPos((center.getX() >> 4) + dx, (center.getZ() >> 4) + dz);
				if (foundYard == null) {
					var start = yardHolder.value().generate(yardHolder, level.dimension(), level.registryAccess(), generator,
						generator.getBiomeSource(), sampler, randomState, level.getStructureTemplateManager(), level.getSeed(),
						chunk, 0, level, yardHolder.value().biomes()::contains);
					if (start.isValid()) {
						check(start.getPieces().size() == 1 && start.getPieces().getFirst() instanceof CharcoalYardPiece,
							"Registered Emberwood Charcoal Yard structure admits its authored piece");
						foundYard = (CharcoalYardPiece) start.getPieces().getFirst();
					}
				}
				if (!foundCourt) {
					var start = courtHolder.value().generate(courtHolder, level.dimension(), level.registryAccess(), generator,
						generator.getBiomeSource(), sampler, randomState, level.getStructureTemplateManager(), level.getSeed(),
						chunk, 0, level, courtHolder.value().biomes()::contains);
					if (start.isValid()) {
						check(start.getPieces().size() == 1 && start.getPieces().getFirst() instanceof KilnCourtPiece,
							"Registered Emberwood Kiln Court structure admits its authored piece");
						foundCourt = true;
					}
				}
			}
		}
		check(foundYard != null, "Emberwood Charcoal Yard locator finds valid woodland shelf terrain in the Emberwood survey");
		check(foundCourt, "Emberwood Kiln Court locator finds valid woodland terrace terrain in the Emberwood survey");
		return foundYard;
	}

	public static void run(ClientGameTestContext context) {
		net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave save;
		UUID identity;
		BlockPos home;
		boolean hidden = context.computeOnClient(mc -> mc.gui.hud.isHidden());
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(25);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule random_tick_speed 0");
			world.getServer().runCommand("time set 6000");
			CharcoalYardPiece[] pieces = world.getServer().computeOnServer(server -> {
				var level = server.overworld();
				var built = new CharcoalYardPiece[4];
				int n = 0;
				for (var facing : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
					var piece = new CharcoalYardPiece(96 + n * 64, 90, 96, facing);
					var box = piece.getBoundingBox();
					server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
						"forceload add " + box.minX() + " " + box.minZ() + " " + box.maxX() + " " + box.maxZ());
					for (int x = box.minX() - 2; x <= box.maxX() + 2; x++) for (int z = box.minZ() - 2; z <= box.maxZ() + 2; z++) {
						for (int y = 90; y <= box.maxY() + 1; y++) level.setBlock(new BlockPos(x, y, z),
							y <= 91 ? Blocks.DIRT.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
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
					check(tag.getBooleanOr("KilnKeeperPlaced", false), "Resident placement flag is written to CharcoalYardPiece");
					place(level, new CharcoalYardPiece(tag), n % 2 == 0);
					check(residents(level, piece).size() == 1, "Restored generation cannot duplicate the resident in " + piece.getOrientation());
				}
				register(level, EmberwoodSites.CHARCOAL_YARD, pieces[0]);
			});
			home = pieces[0].localPosition(9, CharcoalYardPiece.DECK + 1, 11);
			BlockPos fernPos = pieces[0].localPosition(3, CharcoalYardPiece.DECK + 1, 7);
			identity = world.getServer().computeOnServer(server -> {
				var resident = residents(server.overworld(), pieces[0]).getFirst();
				var player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(server.overworld(), fernPos.getX() - 0.5, fernPos.getY(), fernPos.getZ() + 0.5, Set.of(), -90, 25, false);
				LoreJournal.poll(player);
				check(LoreJournal.data(player).has("place:" + EmberwoodSites.CHARCOAL_YARD),
					"Entering authentic Charcoal Yard records place:emberwood_charcoal_yard");
				var state = server.overworld().getBlockState(fernPos);
				check(state.is(EmberContent.FERN) && state.getValue(CinderFernBlock.AGE) == 2 && state.getValue(CinderFernBlock.COOLED),
					"Charcoal Yard ash bed contains mature cooled Cinder Ferns ready for harvest");
				return resident.getUUID();
			});
			context.waitTicks(8);
			int fernsBefore = world.getServer().computeOnServer(server -> fernCount(server.getPlayerList().getPlayers().getFirst(), fernPos));
			context.runOnClient(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
				new BlockHitResult(new Vec3(fernPos.getX() + 0.5, fernPos.getY() + 0.15, fernPos.getZ() + 0.5), Direction.UP, fernPos, false)));
			context.waitTicks(6);
			world.getServer().runOnServer(server -> {
				var state = server.overworld().getBlockState(fernPos);
				check(state.is(EmberContent.FERN) && state.getValue(CinderFernBlock.AGE) == 0 && state.getValue(CinderFernBlock.COOLED),
					"Harvesting a mature cooled ash-bed Cinder Fern resets it to stage 0 while preserving its cooled root");
				check(fernCount(server.getPlayerList().getPlayers().getFirst(), fernPos) == fernsBefore + 1,
					"Harvesting a mature cooled Cinder Fern yields a Cinder Fern item");
			});
			context.waitTicks(12);
			context.getInput().holdKey(options -> options.keyShift);
			context.waitTicks(4);
			for (String line : KilnKeeperResident.LINES) {
				interact(context, world, identity);
				world.getServer().runOnServer(server -> check(LoreJournal.data(server.getPlayerList().getPlayers().getFirst()).has("said:kiln_keeper." + line),
					"Real sneak-use records " + line));
				int before = world.getServer().computeOnServer(server -> LoreJournal.data(server.getPlayerList().getPlayers().getFirst()).entries().size());
				interact(context, world, identity);
				world.getServer().runOnServer(server -> check(LoreJournal.data(server.getPlayerList().getPlayers().getFirst()).entries().size() == before,
					"Repeated packet respects the conversation pause"));
				context.waitTicks(KilnKeeperResident.TALK_COOLDOWN);
			}
			context.getInput().releaseKey(options -> options.keyShift);
			context.waitTicks(3);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				LoreJournal.poll(p);
				check(LoreJournal.data(p).stage("eleven_winters") == LoreQuestRules.DONE, "Hearing the 4th Kiln Keeper story completes eleven_winters");
				LoreJournal.poll(p);
				check(LoreJournal.data(p).stage("forge_records") == LoreQuestRules.ACTIVE, "Next poll opens forge_records");
			});
			hear(context, world, identity, "forge_ledger");
			interact(context, world, identity);
			context.waitFor(mc -> mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.MerchantScreen, 100);
			context.runOnClient(mc -> {
				check(LoreJournal.data(mc.player).has(KilnKeeperResident.ENTRY), "Owner receives Kiln Keeper identity");
				check(LoreEntries.tab(KilnKeeperResident.ENTRY).equals("people"), "Kiln Keeper appears among people");
				check(LoreJournalScreen.describe("said:kiln_keeper.firebreaks").getString().contains("Emberwood Kiln Keeper"),
					"Saved dialogue resolves its speaker");
				mc.player.closeContainer();
			});
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				LoreJournal.record(p, "place:mine_forge_hall", true);
				LoreJournal.poll(p);
				check(LoreJournal.data(p).stage("forge_records") == LoreQuestRules.DONE, "Mountain Forge Hall visit completes forge_records");
				LoreJournal.poll(p);
				check(LoreJournal.data(p).stage("kiln_draft_lamp") == LoreQuestRules.ACTIVE, "Next poll opens kiln_draft_lamp");
				check(!LoreJournal.data(p).has(EmberwoodSites.DRAFT_MARK_ENTRY), "Unlit draft lamp does not complete kiln_draft_lamp");
			});
			hear(context, world, identity, "kiln_draft");
			BlockPos lever = pieces[0].leverPos();
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				p.teleportTo(server.overworld(), lever.getX() + 0.5, lever.getY() - 3, lever.getZ() + 1.5, Set.of(), 180, -35, false);
			});
			context.waitTicks(6);
			context.runOnClient(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(lever), Direction.SOUTH, lever, false)));
			context.waitTicks(6);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(server.overworld().getBlockState(pieces[0].lampPos()).getValue(RedstoneLampBlock.LIT), "Flipping the draft lever lights the kiln draft lamp");
				LoreJournal.poll(p);
				check(LoreJournal.data(p).has(EmberwoodSites.DRAFT_MARK_ENTRY), "Standing beside the lit draft lamp records learned:emberwood_draft");
				check(LoreJournal.data(p).stage("kiln_draft_lamp") == LoreQuestRules.DONE, "Lighting the draft lamp completes kiln_draft_lamp");
			});
			hear(context, world, identity, "draft_return");
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				var target = Vec3.atCenterOf(pieces[0].localPosition(10, CharcoalYardPiece.DECK + 2, 10));
				var eye = Vec3.atCenterOf(pieces[0].localPosition(10, CharcoalYardPiece.DECK + 10, 26));
				var delta = target.subtract(eye);
				player.setGameMode(GameType.SPECTATOR);
				player.teleportTo(server.overworld(), eye.x, eye.y, eye.z, Set.of(), (float) Math.toDegrees(Math.atan2(-delta.x, delta.z)),
					(float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())), false);
			});
			context.waitTicks(20);
			world.getConnection().waitForChunksRender();
			context.runOnClient(mc -> { mc.gui.toastManager().clear(); mc.gui.hud.getChat().clearMessages(false); if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle(); });
			context.takeScreenshot(TestScreenshotOptions.of("emberwood_charcoal_yard").disableCounterPrefix());
			save = world.getWorldSave();
		} finally {
			context.getInput().releaseKey(options -> options.keyShift);
			context.runOnClient(mc -> { if (mc.gui.hud.isHidden() != hidden) mc.gui.hud.toggle(); });
		}
		try (var world = save.open()) {
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				level.getChunkAt(home);
				check(level.getEntity(identity) instanceof Villager resident && resident.entityTags().contains(KilnKeeperResident.TAG),
					"Original Kiln Keeper UUID and role survive reopening");
				var data = LoreJournal.data(server.getPlayerList().getPlayers().getFirst());
				check(data.has(KilnKeeperResident.ENTRY) && data.has("said:kiln_keeper.kiln_widow"), "Kiln Keeper and stories persist");
				check(data.has("said:kiln_keeper.forge_ledger") && data.has("said:kiln_keeper.kiln_draft") && data.has("said:kiln_keeper.draft_return"),
					"Progress-aware return conversations survive reopening");
				check(data.stage("eleven_winters") == LoreQuestRules.DONE && data.stage("forge_records") == LoreQuestRules.DONE
					&& data.stage("kiln_draft_lamp") == LoreQuestRules.DONE, "Completed 3-chapter Emberwood Charcoal Yard route persists across reopening");
			});
		}
	}

	public static void hear(ClientGameTestContext context, TestSingleplayerContext world, UUID uuid, String line) {
		context.waitTicks(KilnKeeperResident.TALK_COOLDOWN);
		context.getInput().holdKey(options -> options.keyShift);
		context.waitTicks(4);
		try {
			interact(context, world, uuid);
			world.getServer().runOnServer(server -> check(LoreJournal.data(server.getPlayerList().getPlayers().getFirst()).has("said:kiln_keeper." + line),
				"Progress-aware conversation records " + line));
			context.waitFor(mc -> LoreJournal.data(mc.player).has("said:kiln_keeper." + line), 100);
			context.runOnClient(mc -> {
				String text = LoreJournalScreen.describe("said:kiln_keeper." + line).getString();
				check(text.contains("Emberwood Kiln Keeper") && !text.contains("dialogue.wildercord."),
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
			resident.teleportTo(server.overworld(), 105.5, 93.0, 105.5, Set.of(), 0, 0, false);
			var player = server.getPlayerList().getPlayers().getFirst();
			player.clearFire();
			player.setHealth(player.getMaxHealth());
			player.teleportTo(server.overworld(), 105.5, 93.0, 107.5, Set.of(), 180, 0, false);
			return resident.getId();
		});
		context.waitTicks(3);
		context.waitFor(mc -> mc.level.getEntity(id) != null && mc.level.getEntity(id).getUUID().equals(uuid), 100);
		context.runOnClient(mc -> {
			var entity = mc.level.getEntity(id);
			check(entity != null && entity.getUUID().equals(uuid), "Interaction targets the actual tracked Kiln Keeper");
			mc.gameMode.interact(mc.player, entity, new EntityHitResult(entity, entity.getBoundingBox().getCenter()), InteractionHand.MAIN_HAND);
		});
		context.waitTicks(5);
	}

	private static int fernCount(ServerPlayer player, BlockPos fernPos) {
		int count = player.getInventory().countItem(EmberContent.FERN_ITEM);
		return count + player.level().getEntitiesOfClass(ItemEntity.class, new AABB(fernPos).inflate(5),
			entity -> entity.isAlive() && entity.getItem().is(EmberContent.FERN_ITEM)).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static void place(ServerLevel level, CharcoalYardPiece piece, boolean reverse) {
		var box = piece.getBoundingBox();
		var chunks = new ArrayList<ChunkPos>();
		for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) chunks.add(new ChunkPos(x, z));
		if (reverse) Collections.reverse(chunks);
		for (var chunk : chunks) piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(41),
			new BoundingBox(chunk.getMinBlockX(), box.minY(), chunk.getMinBlockZ(), chunk.getMaxBlockX(), box.maxY(), chunk.getMaxBlockZ()), chunk, BlockPos.ZERO);
	}

	public static void register(ServerLevel level, String siteId, EmberwoodPiece piece) {
		var box = piece.getBoundingBox();
		var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(ResourceKey.create(Registries.STRUCTURE, Wildercord.id(siteId)));
		var origin = new ChunkPos(box.minX() >> 4, box.minZ() >> 4);
		level.getChunk(origin.x(), origin.z()).setStartForStructure(structure, new StructureStart(structure, origin, 0, new PiecesContainer(List.of(piece))));
		for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++)
			level.getChunk(x, z).addReferenceForStructure(structure, origin.pack());
	}

	private static List<Villager> residents(ServerLevel level, CharcoalYardPiece piece) {
		var b = piece.getBoundingBox();
		return level.getEntitiesOfClass(Villager.class, new AABB(b.minX(), b.minY(), b.minZ(), b.maxX() + 1, b.maxY() + 1, b.maxZ() + 1).inflate(8),
			v -> v.entityTags().contains(KilnKeeperResident.TAG));
	}

	private static void verify(ServerLevel level, CharcoalYardPiece piece) {
		int deck = CharcoalYardPiece.DECK;
		check(residents(level, piece).size() == 1, "Exactly one Kiln Keeper belongs to each yard at " + piece.getBoundingBox());
		var resident = residents(level, piece).getFirst();
		check(resident.getVillagerData().profession().is(VillagerProfession.WEAPONSMITH) && !resident.getOffers().isEmpty(), "The keeper has real weaponsmith trades");
		var block = level.getBlockEntity(piece.localPosition(12, deck + 1, 9));
		check(block instanceof LecternBlockEntity, "The yard contains its lectern");
		var book = ((LecternBlockEntity) block).getBook().get(DataComponents.WRITTEN_BOOK_CONTENT);
		check(book != null && book.pages().size() == 4 && book.author().equals("Ashwin, Kiln Keeper"), "The attributed four-page Eleven Winters at the Gate book is on the lectern");
		for (int z : new int[]{6, 8, 10, 11}) {
			var at = piece.localPosition(4, deck + 1, z);
			check(level.getBlockState(at).is(EmberContent.FERN) && level.getBlockState(at).getValue(CinderFernBlock.AGE) >= 1
				&& level.getBlockState(at).getValue(CinderFernBlock.COOLED),
				"Every ash-bed Cinder Fern plant is established and cooled in " + piece.getOrientation());
		}
	}

	private static void check(boolean ok, String msg) {
		if (!ok) throw new AssertionError(msg);
	}
}
