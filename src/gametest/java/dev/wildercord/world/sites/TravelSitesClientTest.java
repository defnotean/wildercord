package dev.wildercord.world.sites;

import dev.wildercord.Wildercord;
import dev.wildercord.content.RuneItem;
import dev.wildercord.lore.LoreJournal;
import dev.wildercord.world.dungeons.DungeonPiece;
import dev.wildercord.world.sites.travel.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * The travel pack: every site built in every turn (in chunk-sized calls, as worldgen does), its chests stocked from its
 * own tables, its lecterns holding their books, its residents and wall maps in place, the way in walked in Survival,
 * and every site placed by its registered structure on natural terrain, where standing in it writes the lore journal.
 */
public final class TravelSitesClientTest implements FabricClientGameTest {
	/** chests, lecterns and doors (opened before walking in, as a player would) in local coordinates; inside is a local box. */
	private record Spec(String id, BiFunction<Integer, Direction, DungeonPiece> make, int[][] chests, int[][] lecterns, int[][] doors, int[] start, int[] inside) {}

	private static final int GROUND = 100;
	private static final int[][] NONE = {};

	private static final List<Spec> SPECS = List.of(
		new Spec(TravelSites.INN, (x, f) -> new WayfarerInnPiece(x, GROUND, 0, f), new int[][]{{16, 1, 16}}, new int[][]{{8, 1, 8}}, new int[][]{{11, 1, 6}},
			new int[]{11, 1, 0}, new int[]{7, 1, 7, 15, 1, 12}),
		new Spec(TravelSites.TOWER, (x, f) -> new WatchtowerPiece(x, GROUND, 0, f), new int[][]{{7, WatchtowerPiece.TOP + 1, 7}}, new int[][]{{3, WatchtowerPiece.TOP + 1, 7}},
			new int[][]{{5, 1, 2}}, new int[]{5, 1, 0}, new int[]{3, 1, 3, 7, 1, 6}),
		new Spec(TravelSites.BRIDGE, (x, f) -> new BrokenBridgePiece(x, GROUND, 0, f), new int[][]{{12, 1, 21}}, new int[][]{{12, 1, 19}}, NONE,
			new int[]{7, 1, 0}, new int[]{5, 2, 9, 9, 2, 11}),
		new Spec(TravelSites.LIBRARY, (x, f) -> new RuneLibraryPiece(x, GROUND, 0, f), NONE, new int[][]{{7, 1, 5}, {7, 1, 11}}, new int[][]{{7, 1, 1}},
			new int[]{7, 1, 0}, new int[]{3, 1, 2, 11, 1, 4}),
		new Spec(TravelSites.STONES, (x, f) -> new StandingStonesPiece(x, GROUND, 0, f), new int[][]{{StandingStonesPiece.GIFT_X, StandingStonesPiece.GIFT_Y, StandingStonesPiece.GIFT_Z}},
			new int[][]{{10, 1, 10}}, NONE, new int[]{10, 1, 0}, new int[]{7, 1, 4, 13, 1, 8}),
		new Spec(TravelSites.CARAVAN, (x, f) -> new CaravanCampPiece(x, GROUND, 0, f), new int[][]{{4, 3, 4}, {18, 3, 4}}, NONE, NONE,
			new int[]{13, 1, 0}, new int[]{9, 1, 3, 15, 1, 6}),
		new Spec(TravelSites.CARTOGRAPHER, (x, f) -> new CartographerHutPiece(x, GROUND, 0, f), new int[][]{{5, 1, 8}}, new int[][]{{7, 1, 5}}, new int[][]{{5, 1, 1}},
			new int[]{5, 1, 0}, new int[]{2, 1, 2, 8, 1, 4}),
		new Spec(TravelSites.VOWS, (x, f) -> new VowCirclePiece(x, GROUND, 0, f), new int[][]{{9, 1, 11}}, new int[][]{{9, 1, 9}}, NONE,
			new int[]{9, 1, 0}, new int[]{7, 1, 4, 11, 1, 7}));

	@Override
	public void runTest(ClientGameTestContext c) {
		check(SPECS.size() == TravelSites.IDS.size(), "the test covers every travel site");
		try (var world = c.worldBuilder().create()) {
			c.waitTicks(40);
			for (String rule : List.of("gamerule spawn_mobs false", "gamerule fall_damage false", "gamerule advance_time false", "time set 6000"))
				world.getServer().runCommand(rule);
			c.runOnClient(mc -> { mc.getWindow().setWindowed(1600, 900); mc.options.guiScale().set(2); mc.resizeGui(); });
			int[] runes = {0};
			for (int i = 0; i < SPECS.size(); i++) {
				Spec spec = SPECS.get(i);
				int x0 = 64 + i * 48;
				for (Direction facing : Direction.Plane.HORIZONTAL) {
					int x = x0 + facing.get2DDataValue() * 1024;
					DungeonPiece piece = world.getServer().computeOnServer(s -> build(s.overworld(), spec, x, facing, runes));
					// New entities only show once their forced chunks turn entity-ticking, a tick or two later.
					String missing = "";
					for (int wait = 0; wait < 20 && missing != null; wait++) {
						c.waitTicks(2);
						missing = world.getServer().computeOnServer(s -> residents(s.overworld(), spec.id(), piece, facing));
					}
					check(missing == null, missing);
					if (facing == Direction.from2DDataValue(i % 4)) {
						walkIn(c, world, piece, spec);
						// Standing in the site, registered as natural generation would, writes its Places entry in the lore journal.
						world.getServer().runOnServer(s -> register(s.overworld(), spec.id(), piece));
						c.waitTicks(LoreJournal.POLL_TICKS + 5);
						world.getServer().runOnServer(s -> check(LoreJournal.data(s.getPlayerList().getPlayers().getFirst()).has("place:" + spec.id()), spec.id() + " writes its journal entry"));
						overview(c, world, piece, spec.id());
					}
				}
			}
			check(runes[0] > 0, "the travel chests hand out the pack's runes");
			c.runOnClient(mc -> {
				var lang = net.minecraft.locale.Language.getInstance();
				TravelSites.BOOKS.forEach((stem, pages) -> { for (int p = 1; p <= pages; p++) check(lang.has("book.wildercord." + stem + "." + p), stem + " page " + p + " is translated"); });
				for (String id : TravelSites.IDS) check(lang.has("journal.wildercord.entry.place." + id), id + " has a journal entry");
				check(lang.has("book.wildercord.travel_vows.vow") && lang.has("book.wildercord.travel_riddle.end"), "the vow and riddle pages are translated");
			});
		}
		// The registered structures, placed by command on ordinary terrain.
		try (var natural = c.worldBuilder().setUseConsistentSettings(false).create()) {
			c.waitTicks(40);
			natural.getServer().runCommand("gamerule spawn_mobs false");
			natural.getServer().runCommand("time set 6000");
			BlockPos land = natural.getServer().computeOnServer(s -> {
				var source = s.overworld().getChunkSource();
				var b = source.getGenerator().getBiomeSource().findBiomeHorizontal(0, 80, 0, 6400, 32, v -> v.is(net.minecraft.world.level.biome.Biomes.PLAINS),
					RandomSource.create(2026), true, source.randomState());
				check(b != null, "the world has plains");
				return b.getFirst();
			});
			for (int i = 0; i < TravelSites.IDS.size(); i++) {
				String id = TravelSites.IDS.get(i);
				BlockPos found = null;
				// Sites only take fairly flat dry ground, so try a grid of spots near the plains until one fits.
				for (int attempt = 0; attempt < 36 && found == null; attempt++) {
					int x = land.getX() + i * 96 + (attempt % 6) * 32 - 80, z = land.getZ() + (attempt / 6) * 48 - 120;
					natural.getServer().runOnServer(s -> { for (int cx = (x >> 4) - 2; cx <= (x >> 4) + 3; cx++) for (int cz = (z >> 4) - 2; cz <= (z >> 4) + 3; cz++) s.overworld().getChunk(cx, cz); });
					natural.getServer().runCommand("place structure wildercord:" + id + " " + x + " 80 " + z);
					found = natural.getServer().computeOnServer(s -> {
						for (int cx = (x >> 4) - 2; cx <= (x >> 4) + 3; cx++) for (int cz = (z >> 4) - 2; cz <= (z >> 4) + 3; cz++)
							for (var be : s.overworld().getChunk(cx, cz).getBlockEntities().values())
								if (be instanceof ChestBlockEntity chest && chest.getLootTable() != null && chest.getLootTable().identifier().getPath().equals("chests/" + id))
									return be.getBlockPos();
						return null;
					});
				}
				check(found != null, id + " is placed by its registered structure on natural terrain");
				var at = found;
				if (i == 0 || i == 5) {
					natural.getServer().runOnServer(s -> s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.SPECTATOR));
					look(c, natural, new Vec3(at.getX() - 22, at.getY() + 14, at.getZ() - 22), Vec3.atCenterOf(at));
					natural.getConnection().waitForChunksRender();
					shot(c, id + "_natural_terrain");
				}
			}
		}
	}

	/** Builds one site on a grass platform, chunk by chunk, and checks what it should hold. */
	private static DungeonPiece build(ServerLevel l, Spec spec, int x0, Direction facing, int[] runes) {
		var piece = spec.make().apply(x0, facing);
		var b = piece.getBoundingBox();
		var server = l.getServer();
		// Load the chunks (and the platform's margin) first: the fill commands refuse unloaded ground.
		for (int x = (b.minX() - 3) >> 4; x <= (b.maxX() + 3) >> 4; x++) for (int z = (b.minZ() - 3) >> 4; z <= (b.maxZ() + 3) >> 4; z++) { l.setChunkForced(x, z, true); l.getChunk(x, z); }
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "fill %d %d %d %d %d %d dirt".formatted(b.minX() - 3, GROUND - 10, b.minZ() - 3, b.maxX() + 3, GROUND - 1, b.maxZ() + 3));
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "fill %d %d %d %d %d %d grass_block".formatted(b.minX() - 3, GROUND, b.minZ() - 3, b.maxX() + 3, GROUND, b.maxZ() + 3));
		// The chunk box spans the world's height, as natural generation's does, so footings below the piece are kept.
		for (int x = b.minX() >> 4; x <= b.maxX() >> 4; x++) for (int z = b.minZ() >> 4; z <= b.maxZ() >> 4; z++) {
			var clip = new BoundingBox(x * 16, l.getMinY() + 1, z * 16, x * 16 + 15, l.getMaxY(), z * 16 + 15);
			piece.postProcess(l, l.structureManager(), l.getChunkSource().getGenerator(), RandomSource.create(7), clip, new ChunkPos(x, z), BlockPos.ZERO);
		}
		String id = spec.id() + " facing " + facing;
		check(b.maxY() < l.getMaxY(), id + " fits under the sky");
		int[][] chests = spec.id().equals(TravelSites.LIBRARY) ? new int[][]{((RuneLibraryPiece) piece).nook()} : spec.chests();
		for (int[] at : chests) {
			var pos = piece.localPosition(at[0], at[1], at[2]);
			check(l.getBlockEntity(pos) instanceof ChestBlockEntity, id + " has its chest at " + List.of(at[0], at[1], at[2]));
			var chest = (ChestBlockEntity) l.getBlockEntity(pos);
			check(chest.getLootTable() != null && chest.getLootTable().identifier().getPath().equals("chests/" + spec.id()), id + " chest uses its own table");
			chest.unpackLootTable(null);
			check(!chest.isEmpty(), id + " chest table rolls goods");
			for (int slot = 0; slot < chest.getContainerSize(); slot++) if (RuneItem.runeOf(chest.getItem(slot)).isPresent()) runes[0]++;
		}
		for (int[] at : spec.lecterns()) {
			check(l.getBlockEntity(piece.localPosition(at[0], at[1], at[2])) instanceof LecternBlockEntity lectern && lectern.hasBook(), id + " lectern holds its book at " + List.of(at[0], at[1], at[2]));
		}
		for (int[] at : spec.doors()) {
			var pos = piece.localPosition(at[0], at[1], at[2]);
			var state = l.getBlockState(pos);
			check(state.getBlock() instanceof DoorBlock && l.getBlockState(pos.above()).getBlock() instanceof DoorBlock, id + " has its door");
			((DoorBlock) state.getBlock()).setOpen(null, l, state, pos, true);
		}
		switch (spec.id()) {
			case TravelSites.INN -> {
				check(is(l, piece, 11, 1, 16, Blocks.CAMPFIRE) && l.getBlockState(piece.localPosition(11, 1, 16)).getValue(BlockStateProperties.LIT), id + " hearth burns");
				check(l.getBlockState(piece.localPosition(16, 1, 8)).getBlock() instanceof net.minecraft.world.level.block.BedBlock, id + " has beds");
			}
			case TravelSites.TOWER -> {
				var fire = l.getBlockState(piece.localPosition(5, WatchtowerPiece.TOP + 1, 4));
				check(fire.is(Blocks.CAMPFIRE) && !fire.getValue(BlockStateProperties.LIT) && fire.getValue(BlockStateProperties.SIGNAL_FIRE), id + " signal fire waits unlit on hay");
				for (int y = 1; y <= WatchtowerPiece.TOP; y++) check(is(l, piece, 5, y, 7, Blocks.LADDER), id + " ladder reaches the top at " + y);
			}
			case TravelSites.BRIDGE -> {
				check(is(l, piece, 7, 1, BrokenBridgePiece.GAP0, Blocks.AIR) && is(l, piece, 7, 1, BrokenBridgePiece.GAP1, Blocks.AIR), id + " middle has fallen");
				check(is(l, piece, 7, 1, BrokenBridgePiece.GAP0 - 1, Blocks.AIR) == false, id + " deck stands before the gap");
				check(is(l, piece, 3, -3, 12, Blocks.WATER), id + " creek runs below");
				check(is(l, piece, 3, -2, 9, Blocks.LADDER) && is(l, piece, 11, -2, 15, Blocks.LADDER), id + " ladders climb out of the creek");
			}
			case TravelSites.LIBRARY -> {
				int[] n = ((RuneLibraryPiece) piece).nook();
				check(is(l, piece, n[0] == 1 ? 2 : 12, 1, n[2], Blocks.BOOKSHELF), id + " nook hides behind the riddle's shelf");
			}
			case TravelSites.STONES -> {
				check(is(l, piece, StandingStonesPiece.GIFT_X, 0, StandingStonesPiece.GIFT_Z, Blocks.COARSE_DIRT), id + " gift lies under coarse dirt");
				check(is(l, piece, 10, 7, 18, Blocks.CHISELED_STONE_BRICKS), id + " tallest stone stands inward");
			}
			case TravelSites.CARAVAN -> {
			}
			case TravelSites.CARTOGRAPHER -> {
				check(is(l, piece, 2, 1, 8, Blocks.CARTOGRAPHY_TABLE), id + " has its table");
			}
			case TravelSites.VOWS -> {
				check(l.getBlockEntity(piece.localPosition(9, 1, 9)) instanceof LecternBlockEntity lectern
					&& lectern.getBook().get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT).pages().size() == dev.wildercord.spell.CircleVows.ALL.size() + 2,
					id + " book has a page for every vow");
				int pillars = 0;
				for (int x = 0; x <= 18; x++) for (int z = 0; z <= 18; z++) if (is(l, piece, x, 1, z, Blocks.QUARTZ_PILLAR)) pillars++;
				check(pillars == dev.wildercord.spell.CircleVows.ALL.size(), id + " has a pillar per vow; found " + pillars);
			}
			default -> throw new AssertionError(spec.id());
		}
		return piece;
	}

	/** Records the piece as its structure's start, as natural generation does, so the journal can tell the player is in it. */
	private static void register(ServerLevel l, String site, DungeonPiece piece) {
		var structure = l.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(ResourceKey.create(Registries.STRUCTURE, Wildercord.id(site)));
		var b = piece.getBoundingBox();
		var origin = new ChunkPos(b.minX() >> 4, b.minZ() >> 4);
		l.getChunk(b.minX() >> 4, b.minZ() >> 4).setStartForStructure(structure, new StructureStart(structure, origin, 0, new PiecesContainer(List.of(piece))));
		for (int x = b.minX() >> 4; x <= b.maxX() >> 4; x++) for (int z = b.minZ() >> 4; z <= b.maxZ() >> 4; z++) l.getChunk(x, z).addReferenceForStructure(structure, origin.pack());
	}

	/** What the site still lacks of its residents and wall frames, or null once all are there. */
	private static String residents(ServerLevel l, String site, DungeonPiece piece, Direction facing) {
		var b = piece.getBoundingBox();
		var area = new AABB(b.minX(), b.minY(), b.minZ(), b.maxX() + 1, b.maxY() + 1, b.maxZ() + 1);
		String id = site + " facing " + facing;
		int cats = l.getEntitiesOfClass(Cat.class, area).size(), frames = l.getEntitiesOfClass(ItemFrame.class, area).size();
		var traders = l.getEntitiesOfClass(WanderingTrader.class, area);
		int llamas = l.getEntitiesOfClass(Llama.class, area).size();
		return switch (site) {
			case TravelSites.INN -> cats == 1 && frames == 2 ? null : id + " has its cat and a notice board with a map and a note; found " + cats + " cats, " + frames + " frames";
			case TravelSites.CARAVAN -> traders.size() == 1 && llamas == 2 && traders.getFirst().isPersistenceRequired() ? null
				: id + " has a trader who stays and two pack llamas; found " + traders.size() + " traders, " + llamas + " llamas";
			case TravelSites.CARTOGRAPHER -> frames == 5 ? null : id + " walls hold maps and a compass; found " + frames + " frames";
			default -> null;
		};
	}

	/** Walks in from the entrance in Survival, holding forward until the player stands inside. */
	private static void walkIn(ClientGameTestContext c, TestSingleplayerContext world, DungeonPiece piece, Spec spec) {
		int[] s = spec.start(), in = spec.inside();
		var from = piece.localPosition(s[0], s[1], s[2]);
		var ahead = piece.localPosition(s[0], s[1], s[2] + 1);
		float yaw = Direction.getNearest(ahead.getX() - from.getX(), 0, ahead.getZ() - from.getZ(), null).toYRot();
		var bb = BoundingBox.fromCorners(piece.localPosition(in[0], in[1], in[2]), piece.localPosition(in[3], in[4], in[5]));
		world.getServer().runOnServer(srv -> { var p = srv.getPlayerList().getPlayers().getFirst(); p.setGameMode(GameType.SURVIVAL); p.setNoGravity(false);
			p.teleportTo(p.level(), from.getX() + .5, from.getY(), from.getZ() + .5, Set.<Relative>of(), yaw, 0, false); p.setDeltaMovement(Vec3.ZERO); });
		c.waitTicks(10);
		c.runOnClient(mc -> mc.options.keyUp.setDown(true));
		boolean inside = false;
		for (int t = 0; t < 160 && !inside; t++) {
			c.waitTick();
			inside = world.getServer().computeOnServer(srv -> bb.isInside(srv.getPlayerList().getPlayers().getFirst().blockPosition()));
		}
		c.runOnClient(mc -> mc.options.keyUp.setDown(false));
		var at = world.getServer().computeOnServer(srv -> srv.getPlayerList().getPlayers().getFirst().position());
		check(inside, spec.id() + ": walking in from the entrance reaches the interior; stopped at " + at);
	}

	private static void overview(ClientGameTestContext c, TestSingleplayerContext world, DungeonPiece piece, String id) {
		var b = piece.getBoundingBox();
		world.getServer().runOnServer(s -> s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.SPECTATOR));
		var entrance = piece.localPosition(b.getXSpan() / 2, 0, -14);
		look(c, world, new Vec3(entrance.getX() + .5, GROUND + 16, entrance.getZ() + .5), Vec3.atCenterOf(piece.localPosition(b.getXSpan() / 2, 2, b.getZSpan() / 2)));
		world.getConnection().waitForChunksRender();
		shot(c, id + "_overview");
	}

	private static boolean is(ServerLevel l, DungeonPiece piece, int x, int y, int z, Block block) {
		return l.getBlockState(piece.localPosition(x, y, z)).is(block);
	}

	private static void shot(ClientGameTestContext c, String name) {
		c.runOnClient(mc -> { mc.gui.toastManager().clear(); mc.gui.hud.getChat().clearMessages(false); });
		c.waitTicks(5);
		c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static void look(ClientGameTestContext c, TestSingleplayerContext w, Vec3 at, Vec3 target) {
		var d = target.subtract(at);
		float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90), pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
		w.getServer().runOnServer(s -> { var p = s.getPlayerList().getPlayers().getFirst(); p.teleportTo(p.level(), at.x, at.y, at.z, Set.<Relative>of(), yaw, pitch, false); });
		c.waitTicks(15);
	}

	private static void check(boolean value, String label) {
		if (!value) throw new AssertionError(label);
	}
}
