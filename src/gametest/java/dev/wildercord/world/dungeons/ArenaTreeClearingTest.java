package dev.wildercord.world.dungeons;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
/** The biome's trees don't grow into a registered boss arena or hang over its edge, and the trees already standing in one are cut while the arena's own build stays. */
public final class ArenaTreeClearingTest implements FabricClientGameTest {
	private static void check(boolean x, String why) { if (!x) throw new AssertionError(why); }
	/** Runs {@code /place feature minecraft:oak} at {@code at}; whether an oak grew. */
	private static boolean oak(MinecraftServer s, BlockPos at) {
		boolean[] ok = {false};
		s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withCallback((success, value) -> ok[0] = success),
			"place feature minecraft:oak " + at.getX() + " " + at.getY() + " " + at.getZ());
		return ok[0];
	}
	private static void clear(ServerLevel l, int x0, int z0, int x1, int z1, int y0, int y1) {
		for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) for (int y = y0; y <= y1; y++) l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
	}
	private static List<BlockState> snapshot(ServerLevel l, BoundingBox b) {
		List<BlockState> states = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) states.add(l.getBlockState(pos));
		return states;
	}
	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runCommand("time set day");
			BoundingBox box = w.getServer().computeOnServer(s -> {
				var l = s.overworld();
				var piece = new AshenGladePiece(16, 101, 16, Direction.NORTH);
				var b = piece.getBoundingBox();
				for (int cx = (b.minX() - 24) >> 4; cx <= (b.maxX() + 24) >> 4; cx++) for (int cz = (b.minZ() - 8) >> 4; cz <= (b.maxZ() + 8) >> 4; cz++) l.getChunk(cx, cz);
				for (int x = b.minX() - 8; x <= b.maxX() + 24; x++) for (int z = b.minZ() - 8; z <= b.maxZ() + 8; z++) {
					l.setBlock(new BlockPos(x, 100, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
				}
				clear(l, b.minX() - 8, b.minZ() - 8, b.maxX() + 24, b.maxZ() + 8, 101, b.maxY() + 12);
				// Registered the way world generation registers it: the start in its chunk, a reference in every chunk it covers.
				var glade = l.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE, Wildercord.id("ashen_glade"))).value();
				var home = new ChunkPos(b.minX() >> 4, b.minZ() >> 4);
				l.getChunk(home.x(), home.z()).setStartForStructure(glade, new StructureStart(glade, home, 0, new PiecesContainer(List.of(piece))));
				for (int cx = b.minX() >> 4; cx <= b.maxX() >> 4; cx++) for (int cz = b.minZ() >> 4; cz <= b.maxZ() >> 4; cz++) l.getChunk(cx, cz).addReferenceForStructure(glade, home.pack());
				piece.postProcess(l, l.structureManager(), l.getChunkSource().getGenerator(), RandomSource.create(56), b, home, BlockPos.ZERO);
				check(ArenaTrees.blocksTree(l, BlockPos.containing(b.getCenter().getX(), 101, b.getCenter().getZ())), "The registered arena is found from its middle");
				return b;
			});
			int cx = box.getCenter().getX(), cz = box.getCenter().getZ();
			w.getServer().runOnServer(s -> {
				var l = s.overworld();
				// A clearing in the middle of the arena, open to the sky on grass, where an oak would otherwise grow.
				clear(l, cx - 3, cz - 3, cx + 3, cz + 3, 101, box.maxY() + 12);
				check(!oak(s, new BlockPos(cx, 101, cz)), "No oak grows in the middle of the arena");
				check(l.getBlockState(new BlockPos(cx, 102, cz)).isAir(), "Nothing of it was placed");
				check(!oak(s, new BlockPos(box.minX() - 2, 101, cz)), "No oak grows just outside, where its canopy would hang in");
				BlockPos far = new BlockPos(box.maxX() + ArenaTrees.OVERHANG + 8, 101, cz);
				check(oak(s, far), "An oak well clear of the arena grows as ever");
				check(l.getBlockState(far).is(Blocks.OAK_LOG), "Its trunk stands there");

				// The arena's own work: a post on grass under a placed (persistent) leaf. Then a tree that grew before the fix.
				// The piece lays its own floor, so the soil each trunk stands on is put back first.
				l.setBlock(new BlockPos(cx - 3, 100, cz - 3), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
				l.setBlock(new BlockPos(cx, 100, cz), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
				for (int y = 101; y <= 103; y++) l.setBlock(new BlockPos(cx - 3, y, cz - 3), Blocks.OAK_LOG.defaultBlockState(), 2);
				l.setBlock(new BlockPos(cx - 3, 104, cz - 3), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true), 2);
				var before = snapshot(l, box);
				for (int y = 101; y <= 105; y++) l.setBlock(new BlockPos(cx, y, cz), Blocks.BIRCH_LOG.defaultBlockState(), 2);
				for (int x = cx - 1; x <= cx + 1; x++) for (int z = cz - 1; z <= cz + 1; z++) for (int y = 105; y <= 106; y++) {
					if (!(x == cx && z == cz && y == 105)) l.setBlock(new BlockPos(x, y, z), Blocks.BIRCH_LEAVES.defaultBlockState(), 2);
				}
				check(ArenaTrees.wildLeaf(l.getBlockState(new BlockPos(cx, 106, cz))), "The grown tree's leaves are wild");
				int cut = ArenaTrees.sweep(l, box);
				check(cut == 5 + 17, "The sweep cuts the grown tree's five logs and seventeen leaves (" + cut + ")");
				check(l.getBlockState(new BlockPos(cx, 101, cz)).isAir() && l.getBlockState(new BlockPos(cx, 106, cz)).isAir(), "The grown tree is gone");
				check(snapshot(l, box).equals(before), "Every block of the arena's own build, its post and placed leaf included, is left as it was");
				check(ArenaTrees.sweep(l, box) == 0, "A second sweep finds nothing to cut");

				var p = s.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.CREATIVE);
				p.teleportTo(l, box.maxX() + ArenaTrees.OVERHANG + 22.5, 118, cz + 0.5, Set.<Relative>of(), 90, 32, false);
				p.getAbilities().flying = true;
				p.onUpdateAbilities();
			});
			c.waitTicks(40);
			w.getConnection().waitForChunksRender();
			c.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
			c.takeScreenshot(TestScreenshotOptions.of("arena_trees_kept_out").disableCounterPrefix());
		}
	}
}
