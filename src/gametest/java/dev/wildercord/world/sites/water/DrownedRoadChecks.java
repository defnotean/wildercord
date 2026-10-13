package dev.wildercord.world.sites.water;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.client.lore.LoreJournalScreen;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.lore.LoreJournal;
import dev.wildercord.lore.LoreQuestRules;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/** Continues the real Reedkeeper conversation in its world; authored structures, ordinary polls and a client lever packet. */
final class DrownedRoadChecks {
	private DrownedRoadChecks() {}

	static int[] run(ClientGameTestContext c, TestSingleplayerContext world, java.util.UUID keeper) {
		await(c, world, "drowned_road", LoreQuestRules.DONE);
		await(c, world, "second_count", LoreQuestRules.ACTIVE);
		int[] before = world.getServer().computeOnServer(s -> rewards(s.getPlayerList().getPlayers().getFirst()));
		var boathouse = new PierBoathousePiece(448, 90, 96, Direction.EAST);
		var lighthouse = new LighthousePiece(512, 90, 96, Direction.WEST);
		BlockPos lamp = lighthouse.lampPosition(), supply = lighthouse.localPosition(5, LighthousePiece.GALLERY + 1, 6);
		BlockPos stand = lighthouse.localPosition(6, LighthousePiece.GALLERY + 1, 4);
		world.getServer().runOnServer(s -> {
			build(s.overworld(), boathouse); build(s.overworld(), lighthouse);
			book(s.overworld(), boathouse.localPosition(1, PierBoathousePiece.DECK + 1, 24), 3, "Tideward boatkeepers");
			book(s.overworld(), lighthouse.localPosition(8, LighthousePiece.BASE + 1, 5), 4, "?");
			register(s.overworld(), boathouse, "water_pier_boathouse");
			visit(s.getPlayerList().getPlayers().getFirst(), boathouse.localPosition(5, PierBoathousePiece.DECK + 1, 21));
		});
		await(c, world, "second_count", LoreQuestRules.DONE);
		await(c, world, "last_boat_light", LoreQuestRules.ACTIVE);
		ReedkeeperLandingChecks.hear(c, world, keeper, "lighthouse");
		world.getServer().runOnServer(s -> {
			visit(s.getPlayerList().getPlayers().getFirst(), stand);
			s.overworld().setBlock(supply, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
		});
		c.waitTicks(45);
		world.getServer().runOnServer(s -> {
			check(s.overworld().getBlockState(lamp).getValue(RedstoneLampBlock.LIT), "The unregistered fixture really has a lit lamp");
			unseen(s.getPlayerList().getPlayers().getFirst(), "An unregistered copy cannot invent a coastal discovery");
			s.overworld().setBlock(supply, Blocks.AIR.defaultBlockState(), 3);
			register(s.overworld(), lighthouse, "water_lighthouse");
		});
		c.waitTicks(45);
		world.getServer().runOnServer(s -> {
			var p = s.getPlayerList().getPlayers().getFirst();
			check(LoreJournal.data(p).has("place:water_lighthouse"), "The real structure is discovered by the ordinary poll");
			check(!s.overworld().getBlockState(lamp).getValue(RedstoneLampBlock.LIT), "Its lamp really is dark");
			unseen(p, "Entering a dark lighthouse does not finish the last chapter");
			BlockPos other = lighthouse.localPosition(8, LighthousePiece.GALLERY + 1, 6);
			s.overworld().setBlock(other, Blocks.REDSTONE_LAMP.defaultBlockState(), 3);
			s.overworld().setBlock(other.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
		});
		c.waitTicks(45);
		world.getServer().runOnServer(s -> {
			check(s.overworld().getBlockState(lighthouse.localPosition(8, LighthousePiece.GALLERY + 1, 6)).getValue(RedstoneLampBlock.LIT), "The unrelated lamp is powered");
			unseen(s.getPlayerList().getPlayers().getFirst(), "A nearby replacement lamp is not the tower's lamp");
			s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.SPECTATOR);
			s.overworld().setBlock(supply, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
		});
		c.waitTicks(45);
		world.getServer().runOnServer(s -> {
			unseen(s.getPlayerList().getPlayers().getFirst(), "A spectator cannot earn the discovery");
			s.overworld().setBlock(supply, Blocks.AIR.defaultBlockState(), 3);
		});
		c.waitTicks(6);
		world.getServer().runOnServer(s -> visit(s.getPlayerList().getPlayers().getFirst(), lighthouse.localPosition(5, LighthousePiece.GALLERY + 1, 7)));
		c.waitTicks(8);
		int xp = world.getServer().computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().totalExperience);
		BlockPos lever = lighthouse.localPosition(6, LighthousePiece.GALLERY + 1, 7);
		c.runOnClient(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(lever), Direction.UP, lever, false)));
		await(c, world, "last_boat_light", LoreQuestRules.DONE);
		int[] after = world.getServer().computeOnServer(s -> {
			var p = s.getPlayerList().getPlayers().getFirst();
			check(s.overworld().getBlockState(lamp).getValue(RedstoneLampBlock.LIT), "The ordinary client use packet lights the authored lamp");
			check(p.totalExperience == xp + 40, "The final chapter pays its exact XP reward");
			int[] earned = rewards(p);
			check(earned[0] == before[0] + 1 && earned[1] == before[1] + 4, "The final two chapters pay their exact item rewards");
			for (int i = 0; i < 6; i++) LoreJournal.poll(p);
			check(java.util.Arrays.equals(earned, rewards(p)), "Repeated observation cannot repay the route");
			return earned;
		});
		c.runOnClient(mc -> {
			mc.gui.setScreen(new LoreJournalScreen());
			var screen = (LoreJournalScreen) mc.gui.screen(); screen.showTab("quests");
			for (String title : List.of("The Drowned Road", "The Second Count", "A Light for the Last Boat"))
				check(screen.text("quests").contains("Done: " + title), "Synced journal translates " + title);
		});
		c.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_END); c.waitTick();
		c.getInput().releaseKey(com.mojang.blaze3d.platform.InputConstants.KEY_END); c.waitTicks(3);
		c.takeScreenshot(TestScreenshotOptions.of("lore_drowned_road").disableCounterPrefix());
		c.runOnClient(mc -> mc.gui.setScreen(null));
		return after;
	}

	static void reopened(ServerPlayer p, int[] expected) {
		for (String id : List.of("drowned_road", "second_count", "last_boat_light"))
			check(LoreJournal.data(p).stage(id) == LoreQuestRules.DONE, "Saved coastal chapter remains done: " + id);
		check(LoreJournal.data(p).has(WaterSites.COAST_LIGHT_ENTRY), "The observed lamp survives reopening");
		for (int i = 0; i < 6; i++) LoreJournal.poll(p);
		check(java.util.Arrays.equals(expected, rewards(p)), "Reopening cannot repay a completed coastal route");
	}

	private static int[] rewards(ServerPlayer p) {
		return new int[] {p.getInventory().countItem(AuraWorld.AURA_SHARD), p.getInventory().countItem(WildercordItems.BLANK_RUNE)};
	}
	private static void unseen(ServerPlayer p, String message) {
		LoreJournal.poll(p);
		check(!LoreJournal.data(p).has(WaterSites.COAST_LIGHT_ENTRY) && LoreJournal.data(p).stage("last_boat_light") == LoreQuestRules.ACTIVE, message);
	}
	private static void await(ClientGameTestContext c, TestSingleplayerContext world, String quest, int stage) {
		world.getServer().waitFor(s -> LoreJournal.data(s.getPlayerList().getPlayers().getFirst()).stage(quest) == stage, 240);
		c.waitFor(mc -> LoreJournal.data(mc.player).stage(quest) == stage, 100);
	}
	private static void visit(ServerPlayer p, BlockPos at) {
		p.setGameMode(GameType.CREATIVE);
		p.teleportTo(p.level(), at.getX() + .5, at.getY(), at.getZ() + .5, Set.of(), 0, 0, false);
	}
	private static void book(ServerLevel level, BlockPos at, int pages, String author) {
		check(level.getBlockEntity(at) instanceof LecternBlockEntity, "The authored lectern exists at " + at);
		var content = ((LecternBlockEntity) level.getBlockEntity(at)).getBook().get(DataComponents.WRITTEN_BOOK_CONTENT);
		check(content != null && content.pages().size() == pages && content.author().equals(author), "Its complete attributed book survives chunk clipping");
	}
	private static void build(ServerLevel level, WaterPiece piece) {
		var box = piece.getBoundingBox();
		for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
			level.getChunk(x, z);
			piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(7),
				new BoundingBox(x * 16, level.getMinY() + 1, z * 16, x * 16 + 15, level.getMaxY(), z * 16 + 15), new ChunkPos(x, z), BlockPos.ZERO);
		}
	}
	private static void register(ServerLevel level, WaterPiece piece, String id) {
		var box = piece.getBoundingBox();
		var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(ResourceKey.create(Registries.STRUCTURE, Wildercord.id(id)));
		var origin = new ChunkPos(box.minX() >> 4, box.minZ() >> 4);
		level.getChunk(origin.x(), origin.z()).setStartForStructure(structure, new StructureStart(structure, origin, 0, new PiecesContainer(List.of(piece))));
		for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++)
			level.getChunk(x, z).addReferenceForStructure(structure, origin.pack());
	}
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
