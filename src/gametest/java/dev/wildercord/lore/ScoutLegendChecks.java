package dev.wildercord.lore;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.aura.world.SleepingBladeEntity;
import dev.wildercord.aura.world.SleepingBlades;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.client.lore.LoreJournalScreen;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.world.dungeons.DungeonPiece;
import dev.wildercord.world.sites.travel.StandingStonesPiece;
import dev.wildercord.world.sites.travel.TravelSites;
import dev.wildercord.world.sites.travel.WatchtowerPiece;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/** The scout's connected lead uses real structure discovery and the blade stone's ordinary use callback. */
final class ScoutLegendChecks {
	private ScoutLegendChecks() {}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

	static void run(ClientGameTestContext c, TestSingleplayerContext world) {
		world.getServer().runCommand("gamerule spawn_mobs false");
		world.getServer().runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			Grimoire.unlock(player, "aura:battlefield_returned_step");
			LoreJournal.talk(player, "gale");
		});
		await(c, world, "last_cairn", LoreQuestRules.ACTIVE);
		check(world.getServer().computeOnServer(s -> LoreJournal.data(s.getPlayerList().getPlayers().getFirst()).stage("road_before_roads"))
			== LoreQuestRules.UNKNOWN, "The next chapter is hidden until the watchtower is found");

		world.getServer().runOnServer(s -> visit(s.getPlayerList().getPlayers().getFirst(),
			new WatchtowerPiece(64, 80, 64, Direction.NORTH), TravelSites.TOWER, 5, 5, 3));
		await(c, world, "last_cairn", LoreQuestRules.DONE);
		await(c, world, "road_before_roads", LoreQuestRules.ACTIVE);
		world.getServer().runOnServer(s -> visit(s.getPlayerList().getPlayers().getFirst(),
			new StandingStonesPiece(128, 80, 64, Direction.NORTH), TravelSites.STONES, 8, 8, 5));
		await(c, world, "road_before_roads", LoreQuestRules.DONE);
		await(c, world, "oath_at_rest", LoreQuestRules.ACTIVE);

		int[] before = world.getServer().computeOnServer(s -> {
			ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
			return new int[] {p.getInventory().countItem(AuraWorld.AURA_SHARD), p.getInventory().countItem(WildercordItems.BLANK_RUNE)};
		});
		world.getServer().runOnServer(s -> {
			ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
			BlockPos at = p.blockPosition().east(2);
			p.level().setBlock(at, SleepingBlades.STONE.defaultBlockState(), 3);
			SleepingBladeEntity stone = (SleepingBladeEntity) p.level().getBlockEntity(at);
			var hit = new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false);
			int books = p.getInventory().countItem(net.minecraft.world.item.Items.WRITTEN_BOOK);
			// A copied/decorative stone must not grant the real site's Grimoire history.
			UseBlockCallback.EVENT.invoker().interact(p, p.level(), InteractionHand.MAIN_HAND, hit);
			check(!dev.wildercord.player.Heart.discovered(p, "aura:sleeping_blade"), "A non-authentic stone cannot invent the legend");
			stone.awaken();
			UseBlockCallback.EVENT.invoker().interact(p, p.level(), InteractionHand.MAIN_HAND, hit);
			check(dev.wildercord.player.Heart.discovered(p, "aura:sleeping_blade"), "An authentic stone shares history before the player can draw");
			UseBlockCallback.EVENT.invoker().interact(p, p.level(), InteractionHand.MAIN_HAND, hit);
			check(p.getInventory().countItem(net.minecraft.world.item.Items.WRITTEN_BOOK) == books + 1, "The Last Oath is given once per visitor");
			check(stone.claimedBy() == null && !SleepingBlades.drawingAt(p.level(), at), "Reading never claims or draws the blade");
		});
		await(c, world, "oath_at_rest", LoreQuestRules.DONE);
		world.getServer().runOnServer(s -> {
			ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
			LoreQuestRules.Quest quest = LoreQuestRules.byId("oath_at_rest");
			for (int i = 0; i < 5; i++) LoreJournal.poll(p);
			check(p.getInventory().countItem(AuraWorld.AURA_SHARD) == before[0] + quest.shards(), "Final chapter pays shards once");
			check(p.getInventory().countItem(WildercordItems.BLANK_RUNE) == before[1] + quest.runes(), "Final chapter pays runes once");
		});
		c.runOnClient(mc -> {
			mc.gui.setScreen(new LoreJournalScreen());
			((LoreJournalScreen) mc.gui.screen()).showTab("quests");
			mc.gui.hud.getChat().clearMessages(false);
			mc.gui.toastManager().clear();
		});
		c.waitTicks(3);
		List<String> lines = c.computeOnClient(mc -> ((LoreJournalScreen) mc.gui.screen()).text("quests"));
		for (String title : List.of("The Scout's Last Cairn", "The Road Before Roads", "An Oath at Rest")) {
			check(lines.contains("Done: " + title), "Completed chapter is readable: " + title);
		}
		c.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_END);
		c.waitTick();
		c.getInput().releaseKey(com.mojang.blaze3d.platform.InputConstants.KEY_END);
		c.waitTicks(3);
		c.takeScreenshot(TestScreenshotOptions.of("lore_scout_last_cairn").disableCounterPrefix());
		c.runOnClient(mc -> mc.gui.setScreen(null));
	}

	private static void await(ClientGameTestContext c, TestSingleplayerContext world, String quest, int stage) {
		world.getServer().waitFor(s -> LoreJournal.data(s.getPlayerList().getPlayers().getFirst()).stage(quest) == stage, 240);
		c.waitFor(mc -> LoreJournal.data(mc.player).stage(quest) == stage, 100);
	}

	/** Build the real piece, retain its authored book, and register its structure start as generation does. */
	private static void visit(ServerPlayer player, DungeonPiece piece, String id, int localX, int localZ, int pages) {
		ServerLevel level = player.level();
		BoundingBox box = piece.getBoundingBox();
		boolean book = false;
		for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
			level.getChunk(x, z);
			piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(7),
				new BoundingBox(x * 16, level.getMinY() + 1, z * 16, x * 16 + 15, level.getMaxY(), z * 16 + 15), new ChunkPos(x, z), BlockPos.ZERO);
			for (var entity : level.getChunk(x, z).getBlockEntities().values()) {
				if (box.isInside(entity.getBlockPos()) && entity instanceof LecternBlockEntity lectern && lectern.hasBook()) {
					var content = lectern.getBook().get(DataComponents.WRITTEN_BOOK_CONTENT);
					if (content != null && content.pages().size() == pages) book = true;
				}
			}
		}
		check(book, id + " contains the new authored history page");
		var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
			.getValueOrThrow(ResourceKey.create(Registries.STRUCTURE, Wildercord.id(id)));
		ChunkPos origin = new ChunkPos(box.minX() >> 4, box.minZ() >> 4);
		level.getChunk(box.minX() >> 4, box.minZ() >> 4).setStartForStructure(structure, new StructureStart(structure, origin, 0, new PiecesContainer(List.of(piece))));
		for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++)
			level.getChunk(x, z).addReferenceForStructure(structure, origin.pack());
		BlockPos at = piece.localPosition(localX, 1, localZ);
		player.teleportTo(level, at.getX() + .5, at.getY(), at.getZ() + .5, Set.<Relative>of(), 0, 0, false);
	}
}
