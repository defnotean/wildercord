package dev.wildercord.world.sites;

import dev.wildercord.content.RuneItem;
import dev.wildercord.world.sites.farm.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * The farmstead pack: every site built in every turn (in chunk-sized calls, as worldgen does), its chests stocked from
 * its own tables, its bees, flock and herd in place, the windmill's well woken by its button, the way in walked in
 * Survival, and every site placed by its registered structure on natural terrain.
 */
public final class FarmSitesClientTest implements FabricClientGameTest {
	private record Spec(String id, BiFunction<Integer, Direction, FarmPiece> make, int ground, int[][] chests, int[] start, int[] inside) {}

	private static final List<Spec> SPECS = List.of(
		new Spec("farm_windmill", (x, f) -> new WindmillPiece(x, 0, f), 0, new int[][]{{10, 7, 12}}, new int[]{11, 1, 1}, new int[]{9, 1, 11, 13, 1, 14}),
		new Spec("farm_herbalist", (x, f) -> new HerbalistPiece(x, 0, f), 0, new int[][]{{4, 1, 7}, {11, 6, 7}}, new int[]{8, 1, 0}, new int[]{4, 1, 6, 12, 1, 12}),
		new Spec("farm_apiary", (x, f) -> new ApiaryPiece(x, 0, f), 0, new int[][]{{6, 3, 17}}, new int[]{9, 1, 0}, new int[]{5, 3, 13, 13, 3, 16}),
		new Spec("farm_orchard", (x, f) -> new OrchardPiece(x, 0, f), 0, new int[][]{{16, 1, 3}, {12, 0, 14}}, new int[]{12, 1, 0}, new int[]{10, 1, 10, 14, 1, 11}),
		new Spec("farm_shepherd", (x, f) -> new ShepherdPiece(x, 0, f), 0, new int[][]{{7, 1, 17}}, new int[]{5, 1, 0}, new int[]{3, 1, 13, 7, 1, 17}),
		new Spec("farm_mushroom_ring", (x, f) -> new MushroomRingPiece(x, 0, f), 4, new int[][]{{10, 1, 16}}, new int[]{10, 5, 0}, new int[]{5, 1, 5, 15, 1, 9}),
		new Spec("farm_granary", (x, f) -> new GranaryPiece(x, 0, f), 0, new int[][]{{16, 1, 13}, {16, 1, 1}}, new int[]{10, 1, 0}, new int[]{4, 1, 7, 16, 1, 21}),
		new Spec("farm_scarecrow", (x, f) -> new ScarecrowPiece(x, 0, f), 0, new int[][]{{20, 1, 20}}, new int[]{17, 1, 0}, new int[]{15, 1, 15, 20, 1, 20}));

	private static final int GROUND = 100;

	@Override
	public void runTest(ClientGameTestContext c) {
		check(SPECS.size() == FarmSites.IDS.size(), "the test covers every farm site");
		try (var world = c.worldBuilder().create()) {
			c.waitTicks(40);
			for (String rule : List.of("gamerule spawn_mobs false", "gamerule fall_damage false", "gamerule advance_time false", "time set 6000"))
				world.getServer().runCommand(rule);
			c.runOnClient(mc -> { mc.getWindow().setWindowed(1600, 900); mc.options.guiScale().set(2); mc.resizeGui(); });
			int[] runes = {0};
			for (int i = 0; i < SPECS.size(); i++) {
				Spec spec = SPECS.get(i);
				Direction facing = Direction.from2DDataValue(i % 4);
				FarmPiece piece = world.getServer().computeOnServer(s -> build(s.overworld(), spec, facing, i0(spec), runes));
				walkIn(c, world, piece, spec);
				overview(c, world, piece, spec.id());
			}
			check(runes[0] > 0, "the farm chests hand out the pack's runes");
			c.runOnClient(mc -> {
				for (String key : List.of("farm_windmill", "farm_apiary", "farm_orchard", "farm_granary", "farm_scarecrow"))
					check(net.minecraft.locale.Language.getInstance().has("sign.wildercord." + key + ".1"), key + " sign text is translated");
				check(net.minecraft.locale.Language.getInstance().has("advancements.wildercord.world.farm_sites.title"), "the farm advancement is named");
			});
		}
		// The registered structures, placed by command on ordinary terrain.
		try (var natural = c.worldBuilder().setUseConsistentSettings(false).create()) {
			c.waitTicks(40);
			natural.getServer().runCommand("gamerule spawn_mobs false");
			BlockPos land = natural.getServer().computeOnServer(s -> {
				var source = s.overworld().getChunkSource();
				var b = source.getGenerator().getBiomeSource().findBiomeHorizontal(0, 80, 0, 6400, 32, v -> v.is(net.minecraft.world.level.biome.Biomes.PLAINS),
					RandomSource.create(2026), true, source.randomState());
				check(b != null, "the world has plains");
				return b.getFirst();
			});
			var used = new java.util.ArrayList<BlockPos>();
			for (int i = 0; i < FarmSites.IDS.size(); i++) {
				String id = FarmSites.IDS.get(i);
				BlockPos found = null;
				// The world's seed is random, so only spots that already pass the site's own checks (its biome, dry and
				// nearly flat ground) are offered to the command, away from the sites placed before it.
				List<BlockPos> spots = natural.getServer().computeOnServer(s -> flatSpots(s.overworld(), id, land, used));
				check(!spots.isEmpty(), id + " has flat, dry ground in its biome near the plains");
				for (int attempt = 0; attempt < spots.size() && found == null; attempt++) {
					int x = spots.get(attempt).getX(), z = spots.get(attempt).getZ();
					natural.getServer().runOnServer(s -> { for (int cx = (x >> 4) - 2; cx <= (x >> 4) + 3; cx++) for (int cz = (z >> 4) - 2; cz <= (z >> 4) + 3; cz++) s.overworld().getChunk(cx, cz); });
					natural.getServer().runCommand("place structure wildercord:" + id + " " + x + " 80 " + z);
					found = natural.getServer().computeOnServer(s -> {
						for (int cx = (x >> 4) - 2; cx <= (x >> 4) + 3; cx++) for (int cz = (z >> 4) - 2; cz <= (z >> 4) + 3; cz++)
							for (var be : s.overworld().getChunk(cx, cz).getBlockEntities().values())
								if (be instanceof ChestBlockEntity chest && chest.getLootTable() != null && chest.getLootTable().identifier().getPath().startsWith("chests/" + id))
									return be.getBlockPos();
						return null;
					});
				}
				check(found != null, id + " is placed by its registered structure on natural terrain");
				used.add(found);
				if (i == 0 || i == 5) {
					var at = found;
					natural.getServer().runOnServer(s -> { var p = s.getPlayerList().getPlayers().getFirst(); p.setGameMode(GameType.SPECTATOR); });
					look(c, natural, new Vec3(at.getX() - 22, at.getY() + 14, at.getZ() - 22), Vec3.atCenterOf(at));
					natural.getConnection().waitForChunksRender();
					shot(c, id + "_natural_terrain");
				}
			}
		}
	}

	/** Up to twelve chunk corners within about 1500 blocks whose 32-block square is dry, within three blocks of level and in the site's biomes. */
	private static List<BlockPos> flatSpots(ServerLevel l, String id, BlockPos near, List<BlockPos> used) {
		var structure = l.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
			.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("wildercord", id));
		check(structure != null, id + " is a registered structure");
		var generator = l.getChunkSource().getGenerator();
		var state = l.getChunkSource().randomState();
		var out = new java.util.ArrayList<BlockPos>();
		for (int ring = 0; ring <= 30 && out.size() < 12; ring++) for (int dx = -ring; dx <= ring && out.size() < 12; dx++) for (int dz = -ring; dz <= ring && out.size() < 12; dz++) {
			if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
			int x = ((near.getX() >> 4) + dx * 3) << 4, z = ((near.getZ() >> 4) + dz * 3) << 4;
			if (used.stream().anyMatch(u -> Math.abs(u.getX() - x) < 96 && Math.abs(u.getZ() - z) < 96)) continue;
			int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
			boolean dry = true;
			for (int ox = 0; ox <= 32 && dry; ox += 8) for (int oz = 0; oz <= 32 && dry; oz += 8) {
				int top = generator.getBaseHeight(x + ox, z + oz, net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, l, state);
				dry = top == generator.getBaseHeight(x + ox, z + oz, net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, l, state);
				lo = Math.min(lo, top); hi = Math.max(hi, top);
			}
			if (!dry || hi - lo > 3) continue;
			if (!structure.biomes().contains(l.getBiome(new BlockPos(x + 16, hi, z + 16)))) continue;
			out.add(new BlockPos(x, hi, z));
		}
		return out;
	}

	private static int i0(Spec spec) {
		return SPECS.indexOf(spec) * 48;
	}

	/** Builds one site on a dirt platform, chunk by chunk, and checks what it should hold. */
	private static FarmPiece build(ServerLevel l, Spec spec, Direction facing, int x0, int[] runes) {
		var piece = spec.make().apply(x0, facing);
		piece.move(0, GROUND - spec.ground(), 0);
		var b = piece.getBoundingBox();
		var server = l.getServer();
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "fill %d %d %d %d %d %d dirt".formatted(b.minX() - 3, GROUND - 10, b.minZ() - 3, b.maxX() + 3, GROUND - 1, b.maxZ() + 3));
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "fill %d %d %d %d %d %d grass_block".formatted(b.minX() - 3, GROUND, b.minZ() - 3, b.maxX() + 3, GROUND, b.maxZ() + 3));
		for (int x = b.minX() >> 4; x <= b.maxX() >> 4; x++) for (int z = b.minZ() >> 4; z <= b.maxZ() >> 4; z++) l.getChunk(x, z);
		for (int x = b.minX() >> 4; x <= b.maxX() >> 4; x++) for (int z = b.minZ() >> 4; z <= b.maxZ() >> 4; z++) {
			var clip = new BoundingBox(x * 16, b.minY(), z * 16, x * 16 + 15, b.maxY(), z * 16 + 15);
			piece.postProcess(l, l.structureManager(), l.getChunkSource().getGenerator(), RandomSource.create(7), clip, new ChunkPos(x, z), BlockPos.ZERO);
		}
		String id = spec.id() + " facing " + facing;
		check(b.maxY() < l.getMaxY(), id + " fits under the sky");
		for (int[] at : spec.chests()) {
			var pos = piece.localPosition(at[0], at[1], at[2]);
			check(l.getBlockEntity(pos) instanceof ChestBlockEntity, id + " has its chest at " + List.of(at[0], at[1], at[2]));
			var chest = (ChestBlockEntity) l.getBlockEntity(pos);
			check(chest.getLootTable() != null && chest.getLootTable().identifier().getPath().startsWith("chests/farm_"), id + " chest uses a farm table");
			chest.unpackLootTable(null);
			check(!chest.isEmpty(), id + " chest table rolls goods");
			for (int slot = 0; slot < chest.getContainerSize(); slot++) if (RuneItem.runeOf(chest.getItem(slot)).isPresent()) runes[0]++;
		}
		var area = new AABB(b.minX(), b.minY(), b.minZ(), b.maxX() + 1, b.maxY() + 1, b.maxZ() + 1);
		switch (spec.id()) {
			case "farm_windmill" -> {
				check(is(l, piece, 12, 15, 8, Blocks.WOOL.white()), id + " sails turn above the door");
				check(l.getBlockState(piece.localPosition(3, 0, 10)).getValue(BlockStateProperties.MOISTURE) == 7, id + " west field is wet");
				check(l.getBlockState(piece.localPosition(16, 0, 7)).getValue(BlockStateProperties.MOISTURE) == 0, id + " east field starts dry");
				var button = piece.localPosition(18, 1, 9);
				check(l.getBlockState(button).getBlock() instanceof ButtonBlock, id + " well has its button");
				((ButtonBlock) l.getBlockState(button).getBlock()).press(l.getBlockState(button), l, button, null);
			}
			case "farm_herbalist" -> {
				check(is(l, piece, 5, 1, 12, Blocks.BREWING_STAND) && is(l, piece, 4, 1, 12, Blocks.WATER_CAULDRON), id + " bench has a stand and water");
				check(is(l, piece, 12, 3, 9, Blocks.LADDER), id + " loft ladder");
			}
			case "farm_apiary" -> {
				check(l.getBlockEntity(piece.localPosition(3, 2, 8)) instanceof BeehiveBlockEntity hive && hive.getOccupantCount() == 2, id + " hives hold their bees");
				check(l.getBlockEntity(piece.localPosition(9, 5, 17)) instanceof BeehiveBlockEntity hive && hive.getOccupantCount() == 3, id + " smoked hive is full");
				check(l.getBlockState(piece.localPosition(9, 3, 17)).getValue(BlockStateProperties.SIGNAL_FIRE), id + " smoker fire burns on hay");
			}
			case "farm_orchard" -> {
				check(is(l, piece, 12, 1, 14, Blocks.ROOTED_DIRT), id + " cache lies under roots");
				check(l.getBlockEntity(piece.localPosition(12, 1, 10)) instanceof SignBlockEntity sign && sign.isWaxed()
					&& sign.getText(SignTextSlot.FRONT).getMessages(false).getFirst().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
					&& t.getKey().equals("sign.wildercord.farm_orchard.1"), id + " sign tells where to dig");
			}
			case "farm_shepherd" -> {
				check(l.getEntitiesOfClass(Sheep.class, area).size() == 3, id + " pen holds three sheep");
				check(!l.getBlockState(piece.localPosition(14, 1, 4)).getValue(BlockStateProperties.OPEN), id + " gate starts shut");
			}
			case "farm_mushroom_ring" -> {
				check(is(l, piece, 10, 3, 10, Blocks.MUSHROOM_STEM) && is(l, piece, 10, 6, 10, Blocks.RED_MUSHROOM_BLOCK), id + " giant cap");
				check(is(l, piece, 10, 2, 2, Blocks.DARK_OAK_STAIRS), id + " steps lead down");
			}
			case "farm_granary" -> {
				check(l.getEntitiesOfClass(Cow.class, area).size() == 2, id + " stall holds two cows");
				check(is(l, piece, 14, 2, 3, Blocks.BELL) && is(l, piece, 16, 2, 3, Blocks.CAKE), id + " shrine bell and cake");
			}
			case "farm_scarecrow" -> check(is(l, piece, 4, 3, 5, Blocks.CARVED_PUMPKIN) && is(l, piece, 17, 1, 14, Blocks.AIR), id + " scarecrows and an open door");
			default -> throw new AssertionError(spec.id());
		}
		return piece;
	}

	/** Walks in from the entrance in Survival, holding forward until the player stands inside. */
	private static void walkIn(ClientGameTestContext c, TestSingleplayerContext world, FarmPiece piece, Spec spec) {
		int[] s = spec.start(), in = spec.inside();
		var from = piece.localPosition(s[0], s[1], s[2]);
		var ahead = piece.localPosition(s[0], s[1], s[2] + 1);
		float yaw = Direction.getNearest(ahead.getX() - from.getX(), 0, ahead.getZ() - from.getZ(), null).toYRot();
		var a = piece.localPosition(in[0], in[1], in[2]);
		var bb = BoundingBox.fromCorners(a, piece.localPosition(in[3], in[4], in[5]));
		world.getServer().runOnServer(srv -> { var p = srv.getPlayerList().getPlayers().getFirst(); p.setGameMode(GameType.SURVIVAL); p.setNoGravity(false);
			p.teleportTo(p.level(), from.getX() + .5, from.getY(), from.getZ() + .5, Set.<Relative>of(), yaw, 0, false); p.setDeltaMovement(Vec3.ZERO); });
		c.waitTicks(10);
		if (spec.id().equals("farm_windmill")) world.getServer().runOnServer(srv -> check(srv.overworld().getBlockState(piece.localPosition(18, 0, 10)).is(Blocks.WATER), "the windmill's button wakes the well"));
		c.runOnClient(mc -> mc.options.keyUp.setDown(true));
		boolean inside = false;
		for (int t = 0; t < 140 && !inside; t++) {
			c.waitTick();
			inside = world.getServer().computeOnServer(srv -> bb.isInside(srv.getPlayerList().getPlayers().getFirst().blockPosition()));
		}
		c.runOnClient(mc -> mc.options.keyUp.setDown(false));
		var at = world.getServer().computeOnServer(srv -> srv.getPlayerList().getPlayers().getFirst().position());
		check(inside, spec.id() + ": walking in from the entrance reaches the interior; stopped at " + at);
	}

	private static void overview(ClientGameTestContext c, TestSingleplayerContext world, FarmPiece piece, String id) {
		var b = piece.getBoundingBox();
		world.getServer().runOnServer(s -> { var p = s.getPlayerList().getPlayers().getFirst(); p.setGameMode(GameType.SPECTATOR); });
		var entrance = piece.localPosition(b.getXSpan() / 2, 0, -14);
		look(c, world, new Vec3(entrance.getX() + .5, b.minY() + 16, entrance.getZ() + .5), Vec3.atCenterOf(b.getCenter()));
		world.getConnection().waitForChunksRender();
		shot(c, id + "_overview");
	}

	private static boolean is(ServerLevel l, FarmPiece piece, int x, int y, int z, Block block) {
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
