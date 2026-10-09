package dev.wildercord.world.sites;

import dev.wildercord.Wildercord;
import dev.wildercord.content.WildercordBlocks;
import dev.wildercord.lore.LoreJournal;
import dev.wildercord.world.dungeons.DungeonPiece;
import dev.wildercord.world.dungeons.DungeonWards;
import dev.wildercord.world.sites.mine.*;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * The mining sites, built as natural generation builds them (one chunk at a time) in every facing: their loot chests, their
 * keepers, their lava and water held in place, the vault's seals and ward. Then real walks in Survival down the collapsed
 * delve's stair, through the hillside mine's tunnel and up the crystal lab's ladder; the journal and the advancement on
 * entering; and three sites placed by command on normal terrain.
 */
public final class MineSitesClientTest implements FabricClientGameTest {
	static final int VAULT_HEIGHT = 38;

	record Expect(String site, Map<String, Integer> loot, int mobs) {}

	static final List<Expect> EXPECT = List.of(
		new Expect("mine_hillside_mine", Map.of("mine_hillside_mine", 2, "mine_hillside_mine_tally", 1), 0),
		new Expect("mine_forge_hall", Map.of("mine_forge_hall", 2, "mine_forge_hall_masterwork", 1), 0),
		new Expect("mine_crystal_lab", Map.of("mine_crystal_lab", 2), 0),
		new Expect("mine_collapsed_delve", Map.of("mine_collapsed_delve", 1, "mine_collapsed_delve_cache", 1), 2),
		new Expect("mine_basalt_foundry", Map.of("mine_basalt_foundry", 2), 3),
		new Expect("mine_deep_vault", Map.of("mine_deep_vault", 2, "mine_deep_vault_antechamber", 1), 2),
		new Expect("mine_prospector_camp", Map.of("mine_prospector_camp", 2), 0),
		new Expect("mine_miners_rest", Map.of("mine_miners_rest", 1), 0));

	static DungeonPiece make(String site, int x, int y, int z, Direction facing) {
		return switch (site) {
			case "mine_hillside_mine" -> new HillsideMinePiece(x, y, z, facing);
			case "mine_forge_hall" -> new ForgeHallPiece(x, y, z, facing);
			case "mine_crystal_lab" -> new CrystalLabPiece(x, y - 30, z, facing);
			case "mine_collapsed_delve" -> new CollapsedDelvePiece(x, y, z, facing);
			case "mine_basalt_foundry" -> new BasaltFoundryPiece(x, y, z, facing);
			case "mine_deep_vault" -> new DeepVaultPiece(x, -5, z, VAULT_HEIGHT, facing);
			case "mine_prospector_camp" -> new ProspectorCampPiece(x, y, z, facing);
			case "mine_miners_rest" -> new MinersRestPiece(x, y, z, facing);
			default -> throw new IllegalArgumentException(site);
		};
	}

	@Override
	public void runTest(ClientGameTestContext c) {
		Map<String, DungeonPiece> north = new HashMap<>();
		try (var world = c.worldBuilder().create()) {
			c.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule fall_damage false");
			world.getServer().runCommand("time set 6000");
			// Frozen while building, so keepers stay put until their chunks are fully loaded and they can be counted.
			world.getServer().runCommand("tick freeze");
			List<DungeonPiece> all = new ArrayList<>();
			List<Runnable> keeperChecks = new ArrayList<>();
			world.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.CREATIVE);
				for (int i = 0; i < EXPECT.size(); i++) {
					var expect = EXPECT.get(i);
					for (Direction facing : Direction.Plane.HORIZONTAL) {
						var piece = make(expect.site(), 200 + i * 96, 100, facing.get2DDataValue() * 96, facing);
						build(l, piece, 400 + i);
						all.add(piece);
						if (facing == Direction.NORTH) north.put(expect.site(), piece);
						check(loot(l, piece).equals(expect.loot()), expect.site() + " " + facing + ": loot containers " + loot(l, piece));
						keeperChecks.add(() -> {
							var mobs = mobs(l, piece);
							check(mobs.size() == expect.mobs(), expect.site() + " " + facing + ": keepers " + mobs.stream().map(m -> m.getType().toShortString()).toList());
							mobs.forEach(m -> m.setNoAi(true));
						});
						var saved = piece.createTag(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(l));
						check(new ReloadCheck(expect.site(), saved).box().equals(piece.getBoundingBox()), expect.site() + ": piece survives saving");
					}
				}
				// The vault: seals at its door, a ward over it, a ladder all the way up the shaft.
				for (DungeonPiece vault : all.stream().filter(p -> p instanceof DeepVaultPiece).toList()) {
					for (int x = 7; x <= 9; x++) for (int y = 1; y <= 3; y++) check(l.getBlockState(vault.localPosition(x, y, 13)).is(WildercordBlocks.RUNE_SEAL), "vault door is sealed");
					check(l.getBlockState(vault.localPosition(6, 2, 13)).is(Blocks.MAGMA_BLOCK) && l.getBlockState(vault.localPosition(10, 2, 13)).is(Blocks.MUD_BRICKS), "the door's hint blocks");
					// Built wards are recorded at the end of a server tick, so these wait with the keeper counts.
					keeperChecks.add(() -> {
						check(DungeonWards.warded(l, vault.localPosition(8, 2, 20)), "the vault is warded");
						check(!DungeonWards.warded(l, vault.localPosition(8, 2, 8)), "the antechamber is not warded");
					});
					for (int y = 1; y < VAULT_HEIGHT - 8; y++) check(l.getBlockState(vault.localPosition(8, y, 5)).is(Blocks.LADDER), "vault ladder at local y " + y);
					check(l.getBlockState(vault.localPosition(8, VAULT_HEIGHT - 8, 5)).is(Blocks.SPRUCE_TRAPDOOR), "vault shaft capped by a trapdoor");
				}
				for (DungeonPiece lab : all.stream().filter(p -> p instanceof CrystalLabPiece).toList()) {
					for (int y = 7; y <= 32; y++) check(l.getBlockState(lab.localPosition(10, y, 10)).is(Blocks.LADDER), "lab ladder at local y " + y);
				}
			});
			// Keepers are counted once their forced chunks have settled and their entities are visible.
			c.waitTicks(40);
			world.getServer().runOnServer(s -> keeperChecks.forEach(Runnable::run));
			world.getServer().runCommand("tick unfreeze");
			// Fluids sit still: nothing flowing anywhere in any of the sites after a while.
			c.waitTicks(60);
			world.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				for (DungeonPiece piece : all) {
					var b = piece.getBoundingBox();
					int sources = 0;
					for (BlockPos at : BlockPos.betweenClosed(b.minX() - 1, b.minY() - 1, b.minZ() - 1, b.maxX() + 1, b.maxY(), b.maxZ() + 1)) {
						var fluid = l.getFluidState(at);
						if (fluid.isEmpty()) continue;
						check(fluid.isSource(), "fluid spreads at " + at.immutable() + " in " + piece.getClass().getSimpleName());
						sources++;
					}
					if (piece instanceof BasaltFoundryPiece || piece instanceof ProspectorCampPiece) check(sources > 0, "trough or sluice holds its fluid");
				}
			});
			// Walk down the collapsed delve's stair into the antechamber, in Survival.
			var delve = north.get("mine_collapsed_delve");
			walk(c, world, delve.localPosition(8, 14, 0), delve.localPosition(8, 14, 1), 90);
			world.getServer().runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().getFirst();
				var floor = delve.localPosition(8, 4, 15);
				check(Math.abs(p.getY() - floor.getY()) < 0.6, "the stair reaches the antechamber floor; at " + p.position());
				check(p.position().distanceTo(Vec3.atBottomCenterOf(floor)) < 5, "walked into the antechamber; at " + p.position());
			});
			// Walk the hillside mine's yard and tunnel, with the site registered as natural generation would: journal and advancement.
			var hill = north.get("mine_hillside_mine");
			world.getServer().runOnServer(s -> register(s.overworld(), "mine_hillside_mine", hill));
			// About 30 blocks at walking pace (0.22 a tick): the end wall stops the walk there.
			walk(c, world, hill.localPosition(7, 5, 1), hill.localPosition(7, 5, 2), 170);
			c.waitTicks(50);
			world.getServer().runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().getFirst();
				check(p.position().distanceTo(Vec3.atBottomCenterOf(hill.localPosition(7, 5, 30))) < 3, "walked to the tunnel's end; at " + p.position());
				check(LoreJournal.data(p).has("place:mine_hillside_mine"), "entering the mine writes it in the journal");
				var adv = s.getAdvancements().get(Wildercord.id("sites/mine_hillside_mine"));
				check(adv != null && p.getAdvancements().getOrStartProgress(adv).isDone(), "entering the mine grants its advancement");
			});
			// Climb out of the crystal lab by its ladder.
			var lab = north.get("mine_crystal_lab");
			climb(c, world, lab.localPosition(10, 7, 10), lab.localPosition(10, 7, 11), 260);
			world.getServer().runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().getFirst();
				check(p.getY() >= lab.localPosition(10, 30, 10).getY(), "climbed the lab's ladder; at " + p.position());
			});
			// A look at the builds.
			world.getServer().runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.CREATIVE);
				p.setNoGravity(true);
			});
			c.runOnClient(mc -> { if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle(); });
			var forge = north.get("mine_forge_hall").getBoundingBox();
			look(c, world, new Vec3(forge.minX() - 14, forge.maxY() + 8, forge.minZ() - 14), Vec3.atCenterOf(forge.getCenter()));
			shot(c, "mine_forge_hall_overview");
			var camp = north.get("mine_prospector_camp").getBoundingBox();
			look(c, world, new Vec3(camp.minX() - 12, camp.maxY() + 6, camp.minZ() - 12), Vec3.atCenterOf(camp.getCenter()));
			shot(c, "mine_prospector_camp_overview");
			c.runOnClient(mc -> { if (mc.gui.hud.isHidden()) mc.gui.hud.toggle(); });
		}
		// On normal terrain, by command: the camp, the vault and the lab each find a spot and place whole.
		try (var natural = c.worldBuilder().setUseConsistentSettings(false).create()) {
			c.waitTicks(40);
			natural.getServer().runCommand("gamerule spawn_mobs false");
			for (String site : List.of("mine_prospector_camp", "mine_deep_vault", "mine_crystal_lab")) {
				boolean found = false;
				for (int i = 0; i < 12 && !found; i++) {
					int x = 2048 + (i % 4) * 160, z = 2048 + (i / 4) * 160;
					natural.getServer().runOnServer(s -> { for (int cx = (x >> 4) - 3; cx <= (x >> 4) + 3; cx++) for (int cz = (z >> 4) - 3; cz <= (z >> 4) + 3; cz++) s.overworld().getChunk(cx, cz); });
					natural.getServer().runCommand("place structure wildercord:" + site + " " + x + " 64 " + z);
					found = natural.getServer().computeOnServer(s -> {
						int chests = 0;
						for (int cx = (x >> 4) - 3; cx <= (x >> 4) + 3; cx++) for (int cz = (z >> 4) - 3; cz <= (z >> 4) + 3; cz++) {
							for (var be : s.overworld().getChunk(cx, cz).getBlockEntities().values()) {
								if (be instanceof RandomizableContainerBlockEntity box && box.getLootTable() != null && box.getLootTable().identifier().getPath().startsWith("chests/" + site)) chests++;
							}
						}
						return chests >= 2;
					});
				}
				check(found, site + " places on normal terrain");
			}
		}
	}

	/** Reads a saved piece back, through the piece type's own loader. */
	record ReloadCheck(String site, net.minecraft.nbt.CompoundTag tag) {
		BoundingBox box() {
			return switch (site) {
				case "mine_hillside_mine" -> new HillsideMinePiece(tag).getBoundingBox();
				case "mine_forge_hall" -> new ForgeHallPiece(tag).getBoundingBox();
				case "mine_crystal_lab" -> new CrystalLabPiece(tag).getBoundingBox();
				case "mine_collapsed_delve" -> new CollapsedDelvePiece(tag).getBoundingBox();
				case "mine_basalt_foundry" -> new BasaltFoundryPiece(tag).getBoundingBox();
				case "mine_deep_vault" -> new DeepVaultPiece(tag).getBoundingBox();
				case "mine_prospector_camp" -> new ProspectorCampPiece(tag).getBoundingBox();
				default -> new MinersRestPiece(tag).getBoundingBox();
			};
		}
	}

	/** Builds a piece one chunk at a time, with the full-height chunk box natural generation hands it. */
	static void build(ServerLevel l, DungeonPiece piece, long seed) {
		var b = piece.getBoundingBox();
		// Forced, so the chunks stay loaded with their entities visible while later chunks load.
		for (int x = b.minX() >> 4; x <= b.maxX() >> 4; x++) for (int z = b.minZ() >> 4; z <= b.maxZ() >> 4; z++) l.setChunkForced(x, z, true);
		for (int x = b.minX() >> 4; x <= b.maxX() >> 4; x++) for (int z = b.minZ() >> 4; z <= b.maxZ() >> 4; z++) l.getChunk(x, z);
		for (int x = b.minX() >> 4; x <= b.maxX() >> 4; x++) for (int z = b.minZ() >> 4; z <= b.maxZ() >> 4; z++) {
			var clip = new BoundingBox(x * 16, l.getMinY(), z * 16, x * 16 + 15, l.getMaxY(), z * 16 + 15);
			piece.postProcess(l, l.structureManager(), l.getChunkSource().getGenerator(), RandomSource.create(seed), clip, new ChunkPos(x, z), BlockPos.ZERO);
		}
	}

	/** The loot containers inside a piece, by table. */
	static Map<String, Integer> loot(ServerLevel l, DungeonPiece piece) {
		var b = piece.getBoundingBox();
		Map<String, Integer> found = new HashMap<>();
		for (int x = b.minX() >> 4; x <= b.maxX() >> 4; x++) for (int z = b.minZ() >> 4; z <= b.maxZ() >> 4; z++) {
			for (var be : l.getChunk(x, z).getBlockEntities().values()) {
				if (!b.isInside(be.getBlockPos()) || !(be instanceof RandomizableContainerBlockEntity box) || box.getLootTable() == null) continue;
				found.merge(box.getLootTable().identifier().getPath().substring("chests/".length()), 1, Integer::sum);
			}
		}
		return found;
	}

	static List<Mob> mobs(ServerLevel l, DungeonPiece piece) {
		var b = piece.getBoundingBox();
		return l.getEntitiesOfClass(Mob.class, new AABB(b.minX(), b.minY(), b.minZ(), b.maxX() + 1, b.maxY() + 1, b.maxZ() + 1));
	}

	/** Records the piece as a generated structure start, as natural generation does, so the site is found by position. */
	static void register(ServerLevel l, String site, DungeonPiece piece) {
		var structure = l.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(ResourceKey.create(Registries.STRUCTURE, Wildercord.id(site)));
		var b = piece.getBoundingBox();
		var origin = new ChunkPos(b.minX() >> 4, b.minZ() >> 4);
		l.getChunk(b.minX() >> 4, b.minZ() >> 4).setStartForStructure(structure, new StructureStart(structure, origin, 0, new PiecesContainer(List.of(piece))));
		for (int x = b.minX() >> 4; x <= b.maxX() >> 4; x++) for (int z = b.minZ() >> 4; z <= b.maxZ() >> 4; z++) l.getChunk(x, z).addReferenceForStructure(structure, origin.pack());
	}

	private static void place(TestSingleplayerContext w, BlockPos at, BlockPos toward) {
		w.getServer().runOnServer(s -> {
			var p = s.getPlayerList().getPlayers().getFirst();
			p.setGameMode(GameType.SURVIVAL);
			p.setNoGravity(false);
			p.getFoodData().setFoodLevel(20);
			float yaw = Direction.getNearest(toward.getX() - at.getX(), 0, toward.getZ() - at.getZ(), null).toYRot();
			p.teleportTo(p.level(), at.getX() + .5, at.getY(), at.getZ() + .5, Set.<Relative>of(), yaw, 0, false);
			p.setDeltaMovement(Vec3.ZERO);
		});
	}

	private static void walk(ClientGameTestContext c, TestSingleplayerContext w, BlockPos at, BlockPos toward, int ticks) {
		place(w, at, toward);
		c.waitTicks(5);
		c.runOnClient(mc -> mc.options.keyUp.setDown(true));
		c.waitTicks(ticks);
		c.runOnClient(mc -> mc.options.keyUp.setDown(false));
		c.waitTicks(5);
	}

	/** Holds forward into a ladder, and jump to start the climb. */
	private static void climb(ClientGameTestContext c, TestSingleplayerContext w, BlockPos at, BlockPos toward, int ticks) {
		place(w, at, toward);
		c.waitTicks(5);
		c.runOnClient(mc -> { mc.options.keyUp.setDown(true); mc.options.keyJump.setDown(true); });
		c.waitTicks(ticks);
		c.runOnClient(mc -> { mc.options.keyUp.setDown(false); mc.options.keyJump.setDown(false); });
		c.waitTicks(5);
	}

	private static void shot(ClientGameTestContext c, String name) {
		c.runOnClient(mc -> { mc.gui.toastManager().clear(); mc.gui.hud.getChat().clearMessages(false); });
		c.waitTicks(5);
		c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static void look(ClientGameTestContext c, TestSingleplayerContext w, Vec3 at, Vec3 target) {
		var d = target.subtract(at);
		float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90), pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
		w.getServer().runOnServer(s -> {
			var p = s.getPlayerList().getPlayers().getFirst();
			p.teleportTo(p.level(), at.x, at.y, at.z, Set.<Relative>of(), yaw, pitch, false);
		});
		c.waitTicks(15);
	}

	private static void check(boolean value, String label) {
		if (!value) throw new AssertionError(label);
	}
}
