package dev.wildercord.world.sites.belowkeeper;

import dev.wildercord.Wildercord;
import dev.wildercord.client.lore.LoreJournalScreen;
import dev.wildercord.lore.LoreEntries;
import dev.wildercord.lore.LoreJournal;
import dev.wildercord.lore.LoreQuestRules;
import dev.wildercord.wildlife.FungalGarden;
import dev.wildercord.wildlife.GlowcapBlock;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * Native client checks for Belowkeeper Listening Station (BLD-02), Belowkeeper Listener (NPC-02),
 * and the 3-chapter What the Empty Bell Kept / Pressure Records / Resonance Shutter journal route (LOR-02).
 */
public final class BelowkeeperStationChecks {
	private BelowkeeperStationChecks() {}

	public static ListeningStationPiece verifyLocator(ServerLevel level, BlockPos center) {
		var source = level.getChunkSource();
		var generator = source.getGenerator();
		var randomState = source.randomState();
		var stationHolder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(
			ResourceKey.create(Registries.STRUCTURE, Wildercord.id(BelowkeeperSites.LISTENING_STATION)));
		var sanctumHolder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(
			ResourceKey.create(Registries.STRUCTURE, Wildercord.id(BelowkeeperSites.BELL_SANCTUM)));
		var sampler = randomState.createClimateSampler(SamplerContext.EMPTY_UNCACHED);
		ListeningStationPiece foundStation = null;
		boolean foundSanctum = false;
		for (int dx = -8; dx <= 8 && (foundStation == null || !foundSanctum); dx++) {
			for (int dz = -8; dz <= 8 && (foundStation == null || !foundSanctum); dz++) {
				var chunk = new ChunkPos((center.getX() >> 4) + dx, (center.getZ() >> 4) + dz);
				if (foundStation == null) {
					var start = stationHolder.value().generate(stationHolder, level.dimension(), level.registryAccess(), generator,
						generator.getBiomeSource(), sampler, randomState, level.getStructureTemplateManager(), level.getSeed(),
						chunk, 0, level, stationHolder.value().biomes()::contains);
					if (start.isValid()) {
						check(start.getPieces().size() == 1 && start.getPieces().getFirst() instanceof ListeningStationPiece,
							"Registered Belowkeeper Listening Station structure admits its authored piece");
						foundStation = (ListeningStationPiece) start.getPieces().getFirst();
					}
				}
				if (!foundSanctum) {
					var start = sanctumHolder.value().generate(sanctumHolder, level.dimension(), level.registryAccess(), generator,
						generator.getBiomeSource(), sampler, randomState, level.getStructureTemplateManager(), level.getSeed(),
						chunk, 0, level, sanctumHolder.value().biomes()::contains);
					if (start.isValid()) {
						check(start.getPieces().size() == 1 && start.getPieces().getFirst() instanceof BellSanctumPiece,
							"Registered Belowkeeper Bell Sanctum structure admits its authored piece");
						foundSanctum = true;
					}
				}
			}
		}
		check(foundStation != null, "Belowkeeper Listening Station locator finds valid underground vault terrain in the Belowkeeper Hollows survey");
		check(foundSanctum, "Belowkeeper Bell Sanctum locator finds valid underground resonance court terrain in the Belowkeeper Hollows survey");
		return foundStation;
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
			ListeningStationPiece[] pieces = world.getServer().computeOnServer(server -> {
				var level = server.overworld();
				var built = new ListeningStationPiece[4];
				int n = 0;
				for (var facing : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
					var piece = new ListeningStationPiece(96 + n * 64, 24, 96, facing);
					var box = piece.getBoundingBox();
					server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
						"forceload add " + box.minX() + " " + box.minZ() + " " + box.maxX() + " " + box.maxZ());
					for (int x = box.minX() - 2; x <= box.maxX() + 2; x++) for (int z = box.minZ() - 2; z <= box.maxZ() + 2; z++) {
						for (int y = 20; y <= box.maxY() + 2; y++) level.setBlock(new BlockPos(x, y, z),
							Blocks.DEEPSLATE.defaultBlockState(), 2);
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
					check(tag.getBooleanOr("ListenerPlaced", false), "Resident placement flag is written to ListeningStationPiece");
					place(level, new ListeningStationPiece(tag), n % 2 == 0);
					check(residents(level, piece).size() == 1, "Restored generation cannot duplicate the resident in " + piece.getOrientation());
				}
				register(level, BelowkeeperSites.LISTENING_STATION, pieces[0]);
			});
			home = pieces[0].localPosition(10, ListeningStationPiece.DECK + 1, 10);
			BlockPos capPos = pieces[0].localPosition(5, ListeningStationPiece.DECK + 1, 8);
			identity = world.getServer().computeOnServer(server -> {
				var resident = residents(server.overworld(), pieces[0]).getFirst();
				var player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(server.overworld(), capPos.getX() + 1.5, capPos.getY(), capPos.getZ() + 0.5, Set.of(), 90, 25, false);
				LoreJournal.poll(player);
				check(LoreJournal.data(player).has("place:" + BelowkeeperSites.LISTENING_STATION),
					"Entering authentic Listening Station records place:belowkeeper_listening_station");
				check(server.overworld().getBlockState(capPos).getValue(GlowcapBlock.AGE) == 2,
					"Listening Station Glowcap basin contains mature caps");
				return resident.getUUID();
			});
			context.waitTicks(8);
			int gillsBefore = world.getServer().computeOnServer(server -> gillCount(server.getPlayerList().getPlayers().getFirst(), capPos));
			context.runOnClient(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(capPos), Direction.UP, capPos, false)));
			context.waitTicks(6);
			world.getServer().runOnServer(server -> {
				var state = server.overworld().getBlockState(capPos);
				check(state.is(FungalGarden.GLOWCAP) && state.getValue(GlowcapBlock.AGE) == 0,
					"Harvesting a mature basin Glowcap resets it to bud stage while preserving the root");
				check(gillCount(server.getPlayerList().getPlayers().getFirst(), capPos) == gillsBefore + 1,
					"Harvesting a mature Glowcap yields Dried Glowcap Gills");
			});
			context.waitTicks(12);
			context.getInput().holdKey(options -> options.keyShift);
			context.waitTicks(4);
			for (String line : BelowkeeperResident.LINES) {
				interact(context, world, identity);
				world.getServer().runOnServer(server -> check(LoreJournal.data(server.getPlayerList().getPlayers().getFirst()).has("said:belowkeeper_listener." + line),
					"Real sneak-use records " + line));
				int before = world.getServer().computeOnServer(server -> LoreJournal.data(server.getPlayerList().getPlayers().getFirst()).entries().size());
				interact(context, world, identity);
				world.getServer().runOnServer(server -> check(LoreJournal.data(server.getPlayerList().getPlayers().getFirst()).entries().size() == before,
					"Repeated packet respects the conversation pause"));
				context.waitTicks(BelowkeeperResident.TALK_COOLDOWN);
			}
			context.getInput().releaseKey(options -> options.keyShift);
			context.waitTicks(3);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				LoreJournal.poll(p);
				check(LoreJournal.data(p).stage("empty_bell_kept") == LoreQuestRules.DONE, "Hearing the 4th Belowkeeper Listener story completes empty_bell_kept");
				LoreJournal.poll(p);
				check(LoreJournal.data(p).stage("pressure_records") == LoreQuestRules.ACTIVE, "Next poll opens pressure_records");
			});
			hear(context, world, identity, "deep_records");
			interact(context, world, identity);
			context.waitFor(mc -> mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.MerchantScreen, 100);
			context.runOnClient(mc -> {
				check(LoreJournal.data(mc.player).has(BelowkeeperResident.ENTRY), "Owner receives Belowkeeper Listener identity");
				check(LoreEntries.tab(BelowkeeperResident.ENTRY).equals("people"), "Belowkeeper Listener appears among people");
				check(LoreJournalScreen.describe("said:belowkeeper_listener.pressure").getString().contains("Belowkeeper Listener"),
					"Saved dialogue resolves its speaker");
				mc.player.closeContainer();
			});
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				LoreJournal.record(p, "place:mine_deep_vault", true);
				LoreJournal.poll(p);
				check(LoreJournal.data(p).stage("pressure_records") == LoreQuestRules.DONE, "Deep vault visit completes pressure_records");
				LoreJournal.poll(p);
				check(LoreJournal.data(p).stage("resonance_shutter") == LoreQuestRules.ACTIVE, "Next poll opens resonance_shutter");
				check(!LoreJournal.data(p).has(BelowkeeperSites.SHUTTER_MARK_ENTRY), "Unlit shutter lamp does not complete resonance_shutter");
			});
			hear(context, world, identity, "station_gauge");
			BlockPos lever = pieces[0].leverPos();
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				p.teleportTo(server.overworld(), lever.getX() + 0.5, lever.getY() - 2, lever.getZ() + 1.5, Set.of(), 180, -20, false);
			});
			context.waitTicks(6);
			context.runOnClient(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(lever), Direction.SOUTH, lever, false)));
			context.waitTicks(6);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(server.overworld().getBlockState(pieces[0].lampPos()).getValue(RedstoneLampBlock.LIT), "Flipping the shutter lever lights the acoustic shutter lamp");
				LoreJournal.poll(p);
				check(LoreJournal.data(p).has(BelowkeeperSites.SHUTTER_MARK_ENTRY), "Standing beside the lit shutter lamp records learned:belowkeeper_shutter");
				check(LoreJournal.data(p).stage("resonance_shutter") == LoreQuestRules.DONE, "Lighting the shutter lamp completes resonance_shutter");
			});
			hear(context, world, identity, "gauge_return");
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0, false, false, false));
				var target = Vec3.atCenterOf(pieces[0].localPosition(10, ListeningStationPiece.DECK + 2, 9));
				var eye = Vec3.atCenterOf(pieces[0].localPosition(10, ListeningStationPiece.DECK + 3, 18));
				var delta = target.subtract(eye);
				player.setGameMode(GameType.SPECTATOR);
				player.teleportTo(server.overworld(), eye.x, eye.y, eye.z, Set.of(), (float) Math.toDegrees(Math.atan2(-delta.x, delta.z)),
					(float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())), false);
			});
			context.waitTicks(20);
			world.getConnection().waitForChunksRender();
			context.runOnClient(mc -> { mc.gui.toastManager().clear(); mc.gui.hud.getChat().clearMessages(false); if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle(); });
			context.takeScreenshot(TestScreenshotOptions.of("belowkeeper_listening_station").disableCounterPrefix());
			save = world.getWorldSave();
		} finally {
			context.getInput().releaseKey(options -> options.keyShift);
			context.runOnClient(mc -> { if (mc.gui.hud.isHidden() != hidden) mc.gui.hud.toggle(); });
		}
		try (var world = save.open()) {
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				level.getChunkAt(home);
				check(level.getEntity(identity) instanceof Villager resident && resident.entityTags().contains(BelowkeeperResident.TAG),
					"Original Belowkeeper Listener UUID and role survive reopening");
				var data = LoreJournal.data(server.getPlayerList().getPlayers().getFirst());
				check(data.has(BelowkeeperResident.ENTRY) && data.has("said:belowkeeper_listener.hollow_bell"), "Belowkeeper Listener and stories persist");
				check(data.has("said:belowkeeper_listener.deep_records") && data.has("said:belowkeeper_listener.station_gauge") && data.has("said:belowkeeper_listener.gauge_return"),
					"Progress-aware return conversations survive reopening");
				check(data.stage("empty_bell_kept") == LoreQuestRules.DONE && data.stage("pressure_records") == LoreQuestRules.DONE
					&& data.stage("resonance_shutter") == LoreQuestRules.DONE, "Completed 3-chapter Belowkeeper route persists across reopening");
			});
		}
	}

	public static void hear(ClientGameTestContext context, TestSingleplayerContext world, UUID uuid, String line) {
		context.waitTicks(BelowkeeperResident.TALK_COOLDOWN);
		context.getInput().holdKey(options -> options.keyShift);
		context.waitTicks(4);
		try {
			interact(context, world, uuid);
			world.getServer().runOnServer(server -> check(LoreJournal.data(server.getPlayerList().getPlayers().getFirst()).has("said:belowkeeper_listener." + line),
				"Progress-aware conversation records " + line));
			context.waitFor(mc -> LoreJournal.data(mc.player).has("said:belowkeeper_listener." + line), 100);
			context.runOnClient(mc -> {
				String text = LoreJournalScreen.describe("said:belowkeeper_listener." + line).getString();
				check(text.contains("Belowkeeper Listener") && !text.contains("dialogue.wildercord."),
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
			check(entity != null && entity.getUUID().equals(uuid), "Interaction targets the actual tracked Belowkeeper Listener");
			mc.gameMode.interact(mc.player, entity, new EntityHitResult(entity, entity.getBoundingBox().getCenter()), InteractionHand.MAIN_HAND);
		});
		context.waitTicks(5);
	}

	private static int gillCount(ServerPlayer player, BlockPos capPos) {
		int count = player.getInventory().countItem(FungalGarden.GILLS);
		return count + player.level().getEntitiesOfClass(ItemEntity.class, new AABB(capPos).inflate(5),
			entity -> entity.isAlive() && entity.getItem().is(FungalGarden.GILLS)).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static void place(ServerLevel level, ListeningStationPiece piece, boolean reverse) {
		var box = piece.getBoundingBox();
		var chunks = new ArrayList<ChunkPos>();
		for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) chunks.add(new ChunkPos(x, z));
		if (reverse) Collections.reverse(chunks);
		for (var chunk : chunks) piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(41),
			new BoundingBox(chunk.getMinBlockX(), box.minY(), chunk.getMinBlockZ(), chunk.getMaxBlockX(), box.maxY(), chunk.getMaxBlockZ()), chunk, BlockPos.ZERO);
	}

	public static void register(ServerLevel level, String siteId, BelowkeeperPiece piece) {
		var box = piece.getBoundingBox();
		var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(ResourceKey.create(Registries.STRUCTURE, Wildercord.id(siteId)));
		var origin = new ChunkPos(box.minX() >> 4, box.minZ() >> 4);
		level.getChunk(origin.x(), origin.z()).setStartForStructure(structure, new StructureStart(structure, origin, 0, new PiecesContainer(List.of(piece))));
		for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++)
			level.getChunk(x, z).addReferenceForStructure(structure, origin.pack());
	}

	private static List<Villager> residents(ServerLevel level, ListeningStationPiece piece) {
		var b = piece.getBoundingBox();
		return level.getEntitiesOfClass(Villager.class, new AABB(b.minX(), b.minY(), b.minZ(), b.maxX() + 1, b.maxY() + 1, b.maxZ() + 1).inflate(8),
			v -> v.entityTags().contains(BelowkeeperResident.TAG));
	}

	private static void verify(ServerLevel level, ListeningStationPiece piece) {
		int deck = ListeningStationPiece.DECK;
		check(residents(level, piece).size() == 1, "Exactly one Belowkeeper Listener belongs to each station at " + piece.getBoundingBox());
		var resident = residents(level, piece).getFirst();
		check(resident.getVillagerData().profession().is(VillagerProfession.LIBRARIAN) && !resident.getOffers().isEmpty(), "The listener has real librarian trades");
		var block = level.getBlockEntity(piece.localPosition(15, deck + 1, 9));
		check(block instanceof LecternBlockEntity, "The station contains its lectern");
		var book = ((LecternBlockEntity) block).getBook().get(DataComponents.WRITTEN_BOOK_CONTENT);
		check(book != null && book.pages().size() == 4 && book.author().equals("Mara, Belowkeeper"), "The attributed four-page What the Empty Bell Kept book is on the lectern");
		for (int z : new int[]{7, 9, 11, 12}) {
			var at = piece.localPosition(4, deck + 1, z);
			check(level.getBlockState(at).is(FungalGarden.GLOWCAP) && level.getBlockState(at).getValue(GlowcapBlock.AGE) >= 1,
				"Every west-basin Glowcap plant is established in " + piece.getOrientation());
		}
	}

	private static void check(boolean ok, String msg) {
		if (!ok) throw new AssertionError(msg);
	}
}
