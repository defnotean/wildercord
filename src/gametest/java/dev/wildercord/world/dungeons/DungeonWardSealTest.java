package dev.wildercord.world.dungeons;

import dev.wildercord.world.ArchivePiece;
import dev.wildercord.world.sites.masters.ForgeDojoPiece;
import dev.wildercord.world.sites.masters.ResonanceChamberPiece;
import dev.wildercord.world.sites.masters.StarTerracePiece;
import dev.wildercord.world.sites.masters.SundialCourtPiece;
import dev.wildercord.world.sites.mine.DeepVaultPiece;
import dev.wildercord.world.sites.wilds.DawnPavilionPiece;
import dev.wildercord.world.sites.wilds.DuneTemplePiece;
import dev.wildercord.world.sites.wilds.EchoPostPiece;
import dev.wildercord.world.sites.wilds.EmberOutpostPiece;
import dev.wildercord.world.sites.wilds.IronGatehousePiece;
import dev.wildercord.world.sites.wilds.RimeMonasteryPiece;
import dev.wildercord.world.sites.wilds.VenomZigguratPiece;
import dev.wildercord.world.sites.wilds.VoidLanternPiece;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * Nothing to dig through. Each dungeon below is built in the open, so every face of its warded room is exposed,
 * and then asked two things: that a survival player may not break anything the ward covers, and that digging
 * reaches no corner of the room that walking does not already let them into. A room whose walls fall outside its
 * own ward fails the second; a ward that lets one of its own blocks go fails the first.
 */
public final class DungeonWardSealTest implements FabricClientGameTest {
	/** How far around a room the dig is tried. */
	private static final int DIG = 6;
	/** How deep into the room a dug path has to get before it counts as a break in. */
	private static final int CORE = 2;

	private static final List<String> NAMES = List.of("rootbound maze", "sword tomb", "clockwork crypt", "bell chapel",
		"storm spire", "deep vault", "archive", "astral observatory", "ashen glade", "barrow field", "glacier den",
		"living greenhouse", "mojito grove", "moving sky ruin", "serpent grove", "silk hollow", "forge dojo",
		"resonance chamber", "star terrace", "sundial court", "iron gatehouse", "venom ziggurat", "dune temple",
		"echo post", "ember outpost", "rime monastery", "void lantern", "drowned scriptorium", "ember sanctum",
		"dawn pavilion", "cat sanctuary", "lantern bathhouse");
	private static final List<IntFunction<StructurePiece>> PIECES = List.of(
		x -> new RootboundMazePiece(x, 120, 0, Direction.NORTH),
		x -> new SwordTombPiece(x, 120, 0, Direction.NORTH),
		x -> new ClockworkCryptPiece(x, 120, 0, Direction.NORTH),
		x -> new BellChapelPiece(x, 120, 0, Direction.NORTH),
		x -> new StormSpirePiece(x, 120, 0, Direction.NORTH),
		x -> new DeepVaultPiece(x, 120, 0, 12, Direction.NORTH),
		x -> new ArchivePiece(x, 120, 0, Direction.NORTH),
		x -> new AstralObservatoryPiece(x, 120, 0, Direction.NORTH),
		x -> new AshenGladePiece(x, 120, 0, Direction.NORTH),
		x -> new BarrowFieldPiece(x, 120, 0, Direction.NORTH),
		x -> new GlacierDenPiece(x, 120, 0, Direction.NORTH, 0),
		x -> new LivingGreenhousePiece(x, 120, 0, Direction.NORTH),
		x -> new MojitoGrovePiece(x, 120, 0, Direction.NORTH),
		x -> new MovingSkyRuinPiece(x, 120, 0, Direction.NORTH),
		x -> new SerpentGrovePiece(x, 120, 0, Direction.NORTH, 0),
		x -> new SilkHollowPiece(x, 120, 0, Direction.NORTH, 0),
		x -> new ForgeDojoPiece(x, 120, 0, Direction.NORTH, false),
		x -> new ResonanceChamberPiece(x, 120, 0, Direction.NORTH, false, 22),
		x -> new StarTerracePiece(x, 120, 0, Direction.NORTH, false),
		x -> new SundialCourtPiece(x, 120, 0, Direction.NORTH, false),
		x -> new IronGatehousePiece(x, 0, Direction.NORTH),
		x -> new VenomZigguratPiece(x, 0, Direction.NORTH),
		x -> new DuneTemplePiece(x, 0, Direction.NORTH),
		x -> new EchoPostPiece(x, 0, Direction.NORTH),
		x -> new EmberOutpostPiece(x, 0, Direction.NORTH),
		x -> new RimeMonasteryPiece(x, 0, Direction.NORTH),
		x -> new VoidLanternPiece(x, 0, Direction.NORTH),
		x -> new DrownedScriptoriumPiece(x, 120, 0, Direction.NORTH),
		x -> new EmberSanctumPiece(x, 120, 0, Direction.NORTH),
		x -> new DawnPavilionPiece(x, 0, Direction.NORTH),
		x -> new CatSanctuaryPiece(x, 120, 0, Direction.NORTH),
		x -> new LanternBathhousePiece(x, 120, 0, Direction.NORTH));

	@Override public void runTest(ClientGameTestContext c) {
		try (var world = c.worldBuilder().create()) {
			c.waitTicks(40);
			world.getServer().runCommand("gamerule doMobSpawning false");
			world.getServer().runCommand("difficulty peaceful");
			world.getServer().runCommand("time set 6000");
			List<Room> rooms = world.getServer().computeOnServer(s -> {
				var l = s.overworld();
				List<Room> built = new ArrayList<>();
				for (int i = 0; i < PIECES.size(); i++) {
					String name = NAMES.get(i);
					try {
						var piece = PIECES.get(i).apply(i * 160);
						BoundingBox b = piece.getBoundingBox();
						for (int cx = b.minX() >> 4; cx <= b.maxX() >> 4; cx++) {
							for (int cz = b.minZ() >> 4; cz <= b.maxZ() >> 4; cz++) l.getChunk(cx, cz);
						}
						piece.postProcess(l, l.structureManager(), l.getChunkSource().getGenerator(),
							RandomSource.create(1000 + i), b, new ChunkPos(b.minX() >> 4, b.minZ() >> 4), BlockPos.ZERO);
						for (var box : ((WardedPiece) piece).wardedBoxes()) built.add(new Room(name, box));
						if (((WardedPiece) piece).wardedBoxes().isEmpty()) built.add(new Room(name, null));
					} catch (Throwable t) {
						built.add(new Room(name + " (" + t + ")", null));
					}
				}
				return built;
			});
			// The rooms a piece wrote down as it was built are filed into the world's record on the next tick.
			c.waitTicks(10);
			List<String> holes = world.getServer().computeOnServer(s -> {
				var l = s.overworld();
				var p = s.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.SURVIVAL);
				List<String> found = new ArrayList<>();
				Map<String, List<BoundingBox>> byPiece = new java.util.LinkedHashMap<>();
				for (Room room : rooms) byPiece.computeIfAbsent(room.name(), k -> new ArrayList<>()).add(room.box());
				for (var entry : byPiece.entrySet()) {
					List<BoundingBox> boxes = entry.getValue();
					if (boxes.size() == 1 && boxes.get(0) == null) found.add(entry.getKey() + ": no warded room to guard");
					else audit(l, p, entry.getKey(), boxes, found);
				}
				return found;
			});
			check(holes.isEmpty(), "Warded rooms a survival player can break into (" + holes.size() + "):\n"
				+ String.join("\n", holes.subList(0, Math.min(80, holes.size()))));
		}
	}

	/** One dungeon: what its wards cover must not break, and no dug way may lead where an open way does not. */
	private static void audit(ServerLevel l, ServerPlayer p, String name, List<BoundingBox> boxes, List<String> holes) {
		for (BoundingBox box : boxes) room(l, p, name, box, holes);
		ownTheirBlock(l, p, name, boxes.get(0), holes);
		wardStopsAtTheWall(l, p, name, boxes, holes);
	}

	/** One warded room of a dungeon. */
	private static void room(ServerLevel l, ServerPlayer p, String name, BoundingBox box, List<String> holes) {
		BoundingBox region = box.inflatedBy(DIG);
		for (int cx = region.minX() >> 4; cx <= region.maxX() >> 4; cx++) {
			for (int cz = region.minZ() >> 4; cz <= region.maxZ() >> 4; cz++) l.getChunk(cx, cz);
		}
		Map<Long, Boolean> asked = new HashMap<>();
		Map<Long, Long> walking = reach(l, p, region, asked, false);
		Map<Long, Long> digging = reach(l, p, region, asked, true);
		int own = 0;
		for (BlockPos at : span(box)) {
			BlockState st = l.getBlockState(at);
			if (st.isAir() || st.liquid()) continue;
			own++;
			if (st.getDestroySpeed(l, at) >= 0 && DungeonWards.admits(l, p, at, st, false)) {
				holes.add(name + " lets " + block(l, st) + " at " + at + " break inside its ward");
			}
		}
		if (own == 0) holes.add(name + " leaves nothing of its own inside the warded room to guard");
		BoundingBox core = box.inflatedBy(-CORE);
		for (BlockPos at : span(box)) {
			if (!core.isInside(at) || !standable(l.getBlockState(at))) continue;
			long here = at.asLong();
			if (digging.containsKey(here) && !walking.containsKey(here)) {
				holes.add(name + " can be dug into at " + at + " through " + broken(l, digging, here));
			}
		}
	}

	/** A block the player put inside the ward is theirs to take back; an unrecorded waterlogged block stays sealed. */
	private static void ownTheirBlock(ServerLevel l, ServerPlayer p, String name, BoundingBox box, List<String> holes) {
		for (BlockPos at : span(box)) {
			if (!standable(l.getBlockState(at))) continue;
			l.setBlock(at, Blocks.DIRT.defaultBlockState(), 3, 3);
			DungeonWards.placedBy(l, p, at);
			if (!DungeonWards.admits(l, p, at, l.getBlockState(at), true)) {
				holes.add(name + " will not let the player take the block they set down themselves");
			}
			BlockState waterlogged = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
			l.setBlock(at, waterlogged, 3, 3);
			if (DungeonWards.admits(l, p, at, l.getBlockState(at), false)) {
				holes.add(name + " lets an unrecorded waterlogged block break inside its ward");
			}
			l.setBlock(at, Blocks.AIR.defaultBlockState(), 3, 3);
			return;
		}
	}

	/** The ward guards its rooms, not the country around them. */
	private static void wardStopsAtTheWall(ServerLevel l, ServerPlayer p, String name, List<BoundingBox> boxes, List<String> holes) {
		BoundingBox union = null;
		for (BoundingBox box : boxes) union = union == null ? box : union.encapsulate(box);
		BoundingBox region = union.inflatedBy(DIG);
		for (int cx = region.minX() >> 4; cx <= region.maxX() >> 4; cx++) {
			for (int cz = region.minZ() >> 4; cz <= region.maxZ() >> 4; cz++) l.getChunk(cx, cz);
		}
		// Anywhere on the ground around the whole building that no ward of this dungeon reaches.
		BlockPos out = null;
		for (BlockPos at : span(region)) {
			if (!(at.getX() == region.minX() || at.getX() == region.maxX() || at.getZ() == region.minZ() || at.getZ() == region.maxZ())) continue;
			if (boxes.stream().anyMatch(b -> b.isInside(at))) continue;
			out = at.immutable();
			break;
		}
		if (out == null) return;
		l.setBlock(out, Blocks.STONE.defaultBlockState(), 3, 3);
		if (!DungeonWards.admits(l, p, out, l.getBlockState(out), true)) {
			holes.add(name + " wards the ground at " + out + ", outside every room it declares");
		}
		l.setBlock(out, Blocks.AIR.defaultBlockState(), 3, 3);
	}

	/**
	 * Where can a player get to, starting from open ground around the room? Walking takes them through air and
	 * water only; digging takes them through anything the ward lets them break as well.
	 */
	private static Map<Long, Long> reach(ServerLevel l, ServerPlayer p, BoundingBox region, Map<Long, Boolean> asked, boolean digging) {
		Map<Long, Long> from = new HashMap<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		for (BlockPos at : span(region)) {
			if (!(at.getX() == region.minX() || at.getX() == region.maxX() || at.getY() == region.minY()
				|| at.getY() == region.maxY() || at.getZ() == region.minZ() || at.getZ() == region.maxZ())
				|| !standable(l.getBlockState(at))) continue;
			from.put(at.asLong(), null);
			queue.add(at.immutable());
		}
		while (!queue.isEmpty()) {
			BlockPos at = queue.poll();
			for (Direction d : Direction.values()) {
				BlockPos to = at.relative(d);
				if (!region.isInside(to)) continue;
				long here = to.asLong();
				if (from.containsKey(here)) continue;
				BlockState st = l.getBlockState(to);
				if (!standable(st) && !(digging && mayTake(l, p, to, st, asked))) continue;
				from.put(here, at.asLong());
				queue.add(to.immutable());
			}
		}
		return from;
	}

	private static boolean mayTake(ServerLevel l, ServerPlayer p, BlockPos pos, BlockState st, Map<Long, Boolean> asked) {
		if (st.getDestroySpeed(l, pos) < 0) return false;
		return asked.computeIfAbsent(pos.asLong(), k -> DungeonWards.admits(l, p, pos, st, false));
	}

	/** The blocks a dug path had to come through, nearest first. */
	private static String broken(ServerLevel l, Map<Long, Long> from, long here) {
		List<String> path = new ArrayList<>();
		Long at = here;
		while (at != null && path.size() < 3) {
			BlockPos pos = BlockPos.of(at);
			BlockState st = l.getBlockState(pos);
			if (!standable(st)) path.add(block(l, st) + " at " + pos);
			at = from.get(at);
		}
		return path.isEmpty() ? "an open way" : String.join(", ", path);
	}

	private static boolean standable(BlockState st) {
		return st.canBeReplaced() || st.liquid();
	}

	private static Iterable<BlockPos> span(BoundingBox box) {
		return BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
	}

	private static String block(ServerLevel l, BlockState st) {
		return String.valueOf(l.registryAccess().lookupOrThrow(Registries.BLOCK).getKey(st.getBlock()));
	}

	/** One warded room of one dungeon, as it was built. */
	private record Room(String name, BoundingBox box) { }

	private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
}
