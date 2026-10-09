package dev.wildercord.world.sites;

import dev.wildercord.content.WildercordBlocks;
import dev.wildercord.world.dungeons.DungeonPiece;
import dev.wildercord.world.dungeons.DungeonWards;
import dev.wildercord.world.sites.wilds.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * The eight wild sites, built in chunk-sized pieces on a real server (the dawn pavilion in all four facings): each
 * keeps its two chests, a rune seal inside its ward, its guards, its trap or puzzle, and never a shrieker. Then a
 * survival player walks in through three of the doors, and each site is photographed.
 */
public final class WildsSitesTest implements FabricClientGameTest {
	static final int FLOOR = 120;
	static final List<String> IDS = List.of("wilds_venom_ziggurat", "wilds_dune_temple", "wilds_rime_monastery", "wilds_iron_gatehouse",
		"wilds_dawn_pavilion", "wilds_echo_post", "wilds_ember_outpost", "wilds_void_lantern");

	public void runTest(ClientGameTestContext c) {
		try (var world = c.worldBuilder().create()) {
			c.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule fall_damage false");
			world.getServer().runCommand("time set 1000");
			world.getServer().runOnServer(s -> {
				var structures = s.registryAccess().lookupOrThrow(Registries.STRUCTURE);
				for (String id : IDS)
					check(structures.get(ResourceKey.create(Registries.STRUCTURE, Identifier.parse("wildercord:" + id))).isPresent(), id + " is a registered structure");
			});
			DungeonPiece[] built = world.getServer().computeOnServer(s -> {
				ServerLevel l = s.overworld();
				Direction[] f = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
				int x = 2000;
				List<DungeonPiece> out = new ArrayList<>();
				out.add(new VenomZigguratPiece(x, 0, f[0]));
				out.add(new DuneTemplePiece(x += 64, 0, f[1]));
				out.add(new RimeMonasteryPiece(x += 64, 0, f[2]));
				out.add(new IronGatehousePiece(x += 64, 0, f[3]));
				for (Direction d : f) out.add(new DawnPavilionPiece(x += 64, 0, d));
				out.add(new EchoPostPiece(x += 64, 0, f[1]));
				out.add(new EmberOutpostPiece(x += 64, 0, f[2]));
				out.add(new VoidLanternPiece(x += 64, 0, f[3]));
				int[] guards = {3, 3, 0, 3, 0, 0, 0, 0, 0, 3, 2};
				for (int i = 0; i < out.size(); i++) {
					var piece = out.get(i);
					piece.move(0, FLOOR - 6 - piece.getBoundingBox().minY(), 0);
					build(l, piece);
					var b = piece.getBoundingBox();
					String name = piece.getClass().getSimpleName() + " " + piece.getOrientation();
					int chests = 0, seals = 0;
					for (BlockPos p : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
						var state = l.getBlockState(p);
						if (state.is(Blocks.CHEST)) chests++;
						if (state.is(WildercordBlocks.RUNE_SEAL)) seals++;
						check(!state.is(Blocks.SCULK_SHRIEKER), name + " has no sculk shrieker");
					}
					check(chests == 2, name + " has a hall chest and a vault chest; found " + chests);
					check(seals >= 4, name + " has a rune seal door; found " + seals);
					var mobs = l.getEntitiesOfClass(Mob.class, new AABB(b.minX(), b.minY(), b.minZ(), b.maxX() + 1, b.maxY() + 1, b.maxZ() + 1));
					check(mobs.size() == guards[i], name + " has " + guards[i] + " guards; found " + mobs.size());
					// Guards are covered elsewhere; clear them so the walks below are only about the buildings.
					mobs.forEach(m -> m.discard());
				}
				// The ziggurat's dart corridor: wire across the tunnel, a loaded dispenser in each wall.
				var zig = out.get(0);
				for (int z : new int[]{19, 16}) {
					check(l.getBlockState(zig.localPosition(11, 1, z)).is(Blocks.TRIPWIRE), "dart wire at z " + z);
					check(l.getBlockState(zig.localPosition(9, 1, z)).is(Blocks.TRIPWIRE_HOOK) && l.getBlockState(zig.localPosition(13, 1, z)).is(Blocks.TRIPWIRE_HOOK), "dart hooks at z " + z);
					for (int x0 : new int[]{9, 13}) {
						var at = zig.localPosition(x0, 2, z);
						check(l.getBlockState(at).is(Blocks.DISPENSER) && l.getBlockEntity(at) instanceof DispenserBlockEntity, "dart dispenser at " + x0 + "," + z);
					}
				}
				check(l.getBlockState(zig.localPosition(11, 1, 22)).isAir() && l.getBlockState(zig.localPosition(11, 2, 22)).isAir(), "ziggurat back tunnel is open");
				// The dune temple's loose square, before it settles.
				var dune = out.get(1);
				for (int x0 = 9; x0 <= 11; x0++) for (int z0 = 9; z0 <= 11; z0++) {
					check(l.getBlockState(dune.localPosition(x0, 0, z0)).is(Blocks.SAND), "dune plug sand at " + x0 + "," + z0);
					check(l.getBlockState(dune.localPosition(x0, -1, z0)).is(Blocks.SAND), "dune plug under-layer at " + x0 + "," + z0);
				}
				check(l.getBlockState(dune.localPosition(5, -5, 14)).is(Blocks.SUSPICIOUS_SAND), "sanctum has sand to brush");
				check(l.getBlockState(dune.localPosition(5, -3, 10)).is(Blocks.LADDER), "sanctum ladder out");
				check(l.getBlockState(out.get(2).localPosition(18, 14, 17)).is(Blocks.BELL), "monastery bell hangs in the tower");
				var gate = out.get(3);
				check(l.getBlockState(gate.localPosition(3, 1, 1)).is(Blocks.IRON_DOOR), "gatehouse tower iron door");
				check(l.getBlockState(gate.localPosition(12, 3, 2)).is(Blocks.IRON_BARS), "gatehouse portcullis");
				for (int i = 4; i < 8; i++) {
					var dawn = out.get(i);
					var centre = dawn.localPosition(9, 1, 9);
					var b = dawn.getBoundingBox();
					BlockPos vault = null;
					for (BlockPos p : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()))
						if (l.getBlockState(p).is(Blocks.CHEST) && p.getX() - centre.getX() >= 7) vault = p.immutable();
					check(vault != null && Math.abs(vault.getZ() - centre.getZ()) <= 1, "dawn shrine chest is on the sunrise side, facing " + dawn.getOrientation());
					check(l.getBlockState(vault.above(3)).is(Blocks.REDSTONE_LAMP) && l.getBlockState(vault.above(4)).is(Blocks.DAYLIGHT_DETECTOR), "dawn shrine lamp and detector");
				}
				var echo = out.get(8);
				check(l.getBlockState(echo.localPosition(4, 1, 4)).is(Blocks.SCULK_SENSOR) && l.getBlockState(echo.localPosition(12, 1, 8)).is(Blocks.CALIBRATED_SCULK_SENSOR), "echo post sensors");
				check(l.getBlockState(out.get(9).localPosition(9, 17, 12)).is(Blocks.CAMPFIRE), "ember outpost tower fire");
				check(l.getBlockState(out.get(10).localPosition(7, 32, 7)).is(Blocks.PEARLESCENT_FROGLIGHT), "void lantern light");
				return out.toArray(DungeonPiece[]::new);
			});
			c.waitTicks(10);
			// Generated sand rests until disturbed. Dig one block of the loose square, as a player would.
			world.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				check(l.getBlockState(built[1].localPosition(9, 0, 9)).is(Blocks.SAND), "the loose square rests until dug");
				// Block ticks (falling sand) only run near a player.
				var p = s.getPlayerList().getPlayers().getFirst();
				var above = built[1].localPosition(10, 12, 2);
				p.setGameMode(GameType.CREATIVE);
				p.setNoGravity(true);
				p.teleportTo(l, above.getX() + .5, above.getY(), above.getZ() + .5, Set.<Relative>of(), 0, 0, false);
				l.setBlock(built[1].localPosition(10, 0, 10), Blocks.AIR.defaultBlockState(), 3);
			});
			c.waitTicks(60);
			world.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				var dune = built[1];
				for (int[] p : new int[][]{{9, 9}, {11, 9}, {9, 11}, {11, 11}}) {
					check(l.getBlockState(dune.localPosition(p[0], 0, p[1])).isAir() && l.getBlockState(dune.localPosition(p[0], -1, p[1])).isAir(), "the whole loose square drops into the sanctum at " + p[0] + "," + p[1]);
					check(l.getBlockState(dune.localPosition(p[0], -5, p[1])).is(Blocks.SAND), "the fallen sand lands on the sanctum floor at " + p[0] + "," + p[1]);
				}
				check(l.getBlockState(dune.localPosition(8, 0, 10)).is(Blocks.CHISELED_SANDSTONE), "the carved ring stays");
				for (var piece : built) {
					var b = piece.getBoundingBox();
					BlockPos seal = null;
					for (BlockPos p : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()))
						if (seal == null && l.getBlockState(p).is(WildercordBlocks.RUNE_SEAL)) seal = p.immutable();
					check(seal != null && DungeonWards.warded(l, seal), piece.getClass().getSimpleName() + " vault seal is warded");
				}
			});
			// Survival walks in through three doors: the monastery gate, the echo post and the void lantern.
			walk(c, world, built[2], 11, 0, 60, 8, "rime monastery gate");
			walk(c, world, built[8], 8, 0, 40, 4.5, "echo post door");
			walk(c, world, built[10], 7, 1, 40, 4, "void lantern door");
			world.getServer().runOnServer(s -> { var p = s.getPlayerList().getPlayers().getFirst(); p.setGameMode(GameType.CREATIVE); p.setNoGravity(true); });
			c.runOnClient(mc -> { mc.getWindow().setWindowed(1600, 900); if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle(); });
			String[] names = {"venom_ziggurat", "dune_temple", "rime_monastery", "iron_gatehouse", "dawn_pavilion", "echo_post", "ember_outpost", "void_lantern"};
			int[] shown = {0, 1, 2, 3, 4, 8, 9, 10};
			for (int i = 0; i < shown.length; i++) {
				var b = built[shown[i]].getBoundingBox();
				Vec3 mid = new Vec3((b.minX() + b.maxX()) / 2.0, FLOOR + 4, (b.minZ() + b.maxZ()) / 2.0);
				look(c, world, mid.add(-22, 16, -22), mid);
				world.getConnection().waitForChunksRender();
				shot(c, "wilds_" + names[i]);
			}
			c.runOnClient(mc -> { if (mc.gui.hud.isHidden()) mc.gui.hud.toggle(); });
		}
	}

	private static void build(ServerLevel l, DungeonPiece piece) {
		var b = piece.getBoundingBox();
		for (int x = b.minX() >> 4; x <= b.maxX() >> 4; x++) for (int z = b.minZ() >> 4; z <= b.maxZ() >> 4; z++) l.getChunk(x, z);
		// Chunk-sized calls, as natural generation does, to expose clipping errors.
		for (int x = b.minX() >> 4; x <= b.maxX() >> 4; x++) for (int z = b.minZ() >> 4; z <= b.maxZ() >> 4; z++) {
			var clip = new BoundingBox(x * 16, b.minY(), z * 16, x * 16 + 15, b.maxY(), z * 16 + 15);
			piece.postProcess(l, l.structureManager(), l.getChunkSource().getGenerator(), RandomSource.create(61), clip, new ChunkPos(x, z), BlockPos.ZERO);
		}
	}

	/** Walks forward (local +z) in survival from local (x, 1, z) and checks the player got at least {@code least} blocks in. */
	private static void walk(ClientGameTestContext c, TestSingleplayerContext w, DungeonPiece piece, int x, int z, int ticks, double least, String what) {
		var start = piece.localPosition(x, 1, z);
		var next = piece.localPosition(x, 1, z + 1);
		w.getServer().runOnServer(s -> {
			var p = s.getPlayerList().getPlayers().getFirst();
			p.setGameMode(GameType.SURVIVAL);
			p.setNoGravity(false);
			p.getFoodData().setFoodLevel(20);
			float yaw = Direction.getNearest(next.getX() - start.getX(), 0, next.getZ() - start.getZ(), null).toYRot();
			p.teleportTo(p.level(), start.getX() + .5, start.getY(), start.getZ() + .5, Set.<Relative>of(), yaw, 0, false);
			p.setDeltaMovement(Vec3.ZERO);
		});
		c.waitTicks(5);
		c.runOnClient(mc -> mc.options.keyUp.setDown(true));
		c.waitTicks(ticks);
		c.runOnClient(mc -> mc.options.keyUp.setDown(false));
		c.waitTicks(5);
		w.getServer().runOnServer(s -> {
			var p = s.getPlayerList().getPlayers().getFirst();
			double moved = p.position().distanceTo(Vec3.atBottomCenterOf(start));
			check(moved >= least && Math.abs(p.getY() - start.getY()) < 1.2, what + ": walked " + moved + " to " + p.position());
		});
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

	private static void check(boolean value, String label) { if (!value) throw new AssertionError(label); }
}
