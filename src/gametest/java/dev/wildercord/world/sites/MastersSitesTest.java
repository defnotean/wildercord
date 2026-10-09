package dev.wildercord.world.sites;

import dev.wildercord.cast.Runebound;
import dev.wildercord.content.RuneSealBlock;
import dev.wildercord.content.WildercordBlocks;
import dev.wildercord.world.sites.masters.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * Builds every Master hall in both of its schools (each turned a different way) in a real level and checks what a player
 * finds there: practice dummies, the lectern's notes on challenging the Master, the school's reward and supplies, the trial
 * that keeps the reward (a seal of the school's element, a Runebound guard, or a climb), and a way to walk in from outside
 * to the lectern and on to the reward once the trial is passed. Walking is checked on the blocks themselves: steps of one,
 * drops of up to three, ladders, and running jumps over a one-block gap.
 */
public final class MastersSitesTest implements FabricClientGameTest {
	/** site: local width, local depth, which trial. */
	private record Site(String id, int w, int d, Trial trial) {}
	private enum Trial { SEAL, GUARD, CLIMB }

	private static final List<Site> SITES = List.of(
		new Site("master_forge_dojo", 17, 19, Trial.SEAL), new Site("master_wind_gate", 15, 14, Trial.CLIMB),
		new Site("master_quarry_hall", 19, 19, Trial.GUARD), new Site("master_waterfall_shrine", 17, 17, Trial.CLIMB),
		new Site("master_root_temple", 17, 19, Trial.GUARD), new Site("master_sundial_court", 19, 21, Trial.SEAL),
		new Site("master_star_terrace", 17, 18, Trial.SEAL), new Site("master_resonance_chamber", 17, 17, Trial.SEAL));

	private static final Map<String, RuneSealBlock.Element> SEALS = Map.of("ember", RuneSealBlock.Element.FIRE, "crimson", RuneSealBlock.Element.BLOOD,
		"dune", RuneSealBlock.Element.EARTH, "hourglass", RuneSealBlock.Element.TIME, "starlit", RuneSealBlock.Element.ARCANE,
		"dawn", RuneSealBlock.Element.ARCANE, "hollow", RuneSealBlock.Element.VOID, "echo", RuneSealBlock.Element.WIND);

	private static final int BASE = 100;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("time set 6000");
			List<String> keys = new ArrayList<>();
			List<BlockPos> views = world.getServer().computeOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.CREATIVE);
				ServerLevel level = player.level();
				List<BlockPos> out = new ArrayList<>();
				Set<String> schoolsSeen = new HashSet<>();
				for (int i = 0; i < SITES.size(); i++) {
					Site site = SITES.get(i);
					for (int a = 0; a < 2; a++) {
						boolean alt = a == 1;
						Direction facing = Direction.from2DDataValue(i + a);
						int x = i * 64, z = a * 64;
						MasterSitePiece piece = switch (site.id()) {
							case "master_forge_dojo" -> new ForgeDojoPiece(x, BASE, z, facing, alt);
							case "master_wind_gate" -> new WindGatePiece(x, BASE, z, facing, alt);
							case "master_quarry_hall" -> new QuarryHallPiece(x, BASE, z, facing, alt);
							case "master_waterfall_shrine" -> new WaterfallShrinePiece(x, BASE, z, facing, alt);
							case "master_root_temple" -> new RootTemplePiece(x, BASE, z, facing, alt);
							case "master_sundial_court" -> new SundialCourtPiece(x, BASE, z, facing, alt);
							case "master_star_terrace" -> new StarTerracePiece(x, BASE, z, facing, alt);
							default -> new ResonanceChamberPiece(x, BASE, z, facing, alt, 30);
						};
						String school = piece.school();
						check(site.id().equals(piece.site()) && schoolsSeen.add(school), site.id() + " keeps a school of its own: " + school);
						int ground = piece instanceof ResonanceChamberPiece r ? r.surface() : 0;
						build(level, piece, site, ground);
						String where = site.id() + "/" + school;
						inspect(level, piece, site, school, ground, where);
						keys.add("book.wildercord.master_site." + school);
						keys.add("book.wildercord." + site.id() + ".trial");
						keys.add("advancements.wildercord.sites." + site.id() + ".title");
						if (!alt) {
							out.add(piece.localPosition(site.w() / 2, ground + 1, -4));
							out.add(piece.localPosition(site.w() / 2, ground + 1, site.d() / 2));
						}
					}
				}
				check(schoolsSeen.size() == 16, "the eight halls keep all sixteen schools between them");
				return out;
			});
			keys.add("book.wildercord.master_site.challenge");
			context.runOnClient(mc -> {
				for (String key : keys) check(Language.getInstance().has(key), "text for " + key);
			});
			// A look at each hall from its entrance, for review.
			context.runOnClient(mc -> mc.getWindow().setWindowed(1600, 900));
			for (int i = 0; i < SITES.size(); i++) {
				BlockPos at = views.get(2 * i), centre = views.get(2 * i + 1);
				world.getServer().runOnServer(server -> {
					var p = server.getPlayerList().getPlayers().getFirst();
					double dx = at.getX() - centre.getX(), dz = at.getZ() - centre.getZ(), len = Math.max(1, Math.sqrt(dx * dx + dz * dz));
					double x = at.getX() + 0.5 + dx / len * 10, y = at.getY() + 7, z = at.getZ() + 0.5 + dz / len * 10;
					float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)) + 180;
					p.teleportTo(p.level(), x, y, z, Set.<Relative>of(), yaw, 22, false);
				});
				context.waitTicks(15);
				context.takeScreenshot(TestScreenshotOptions.of(SITES.get(i).id()).disableCounterPrefix());
			}
		}
	}

	/** Lays a stone floor under and around the piece (local y ground-4..ground), then builds it. */
	private static void build(ServerLevel level, MasterSitePiece piece, Site site, int ground) {
		BoundingBox box = piece.getBoundingBox();
		for (int cx = (box.minX() - 8) >> 4; cx <= (box.maxX() + 8) >> 4; cx++)
			for (int cz = (box.minZ() - 8) >> 4; cz <= (box.maxZ() + 8) >> 4; cz++) level.getChunk(cx, cz);
		var stone = Blocks.STONE.defaultBlockState();
		for (int x = -4; x <= site.w() + 3; x++) for (int z = -6; z <= site.d() + 3; z++) for (int y = ground - 4; y <= ground; y++) {
			level.setBlock(piece.localPosition(x, y, z), stone, 2);
		}
		// Worldgen hands a piece the chunk's whole writable column, so pits and pools below its floor are placed too.
		var writable = new BoundingBox(box.minX(), level.getMinY(), box.minZ(), box.maxX(), level.getMaxY(), box.maxZ());
		piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(4242), writable,
			new ChunkPos(box.minX() >> 4, box.minZ() >> 4), BlockPos.ZERO);
	}

	private static void inspect(ServerLevel level, MasterSitePiece piece, Site site, String school, int ground, String where) {
		BoundingBox box = piece.getBoundingBox();
		BlockPos reward = null, lectern = null;
		boolean supplies = false;
		int dummies = 0, targets = 0, seals = 0;
		List<BlockPos> sealAt = new ArrayList<>();
		for (BlockPos at : BlockPos.betweenClosed(box.minX(), box.minY() - 4, box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			var state = level.getBlockState(at);
			if (level.getBlockEntity(at) instanceof RandomizableContainerBlockEntity chest && chest.getLootTable() != null) {
				if (chest.getLootTable().equals(MasterSites.reward(site.id(), school))) reward = at.immutable();
				if (chest.getLootTable().equals(MasterSites.supplies(site.id()))) supplies = true;
			}
			if (level.getBlockEntity(at) instanceof LecternBlockEntity l) {
				lectern = at.immutable();
				checkNotes(l, site, school, where);
			}
			if (state.is(Blocks.CARVED_PUMPKIN) && level.getBlockState(at.below()).is(Blocks.HAY_BLOCK)) dummies++;
			if (state.is(Blocks.TARGET)) targets++;
			if (state.is(WildercordBlocks.RUNE_SEAL)) {
				seals++;
				sealAt.add(at.immutable());
				check(state.getValue(RuneSealBlock.ELEMENT) == SEALS.get(school), where + ": the seal answers the school's element");
			}
		}
		check(reward != null, where + ": the reward chest holds the school's own table");
		check(supplies, where + ": a supply chest");
		check(lectern != null, where + ": a lectern with the school's notes");
		check(dummies >= 2 && targets >= 1, where + ": practice dummies and a target");
		BlockPos start = piece.localPosition(site.w() / 2, ground + 1, -3);
		check(standable(level, start), where + ": the walk starts on open ground outside");
		Set<BlockPos> reach = walk(level, start, box.inflatedBy(5));
		check(beside(reach, lectern), where + ": a player walks in from outside to the lectern");
		switch (site.trial()) {
			case SEAL -> {
				check(seals == 9, where + ": a three by three seal door, found " + seals);
				check(!beside(reach, reward), where + ": the reward is shut away until the seal opens");
				for (BlockPos at : sealAt) level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
				check(beside(walk(level, start, box.inflatedBy(5)), reward), where + ": with the seal open, the reward is a walk away");
			}
			case GUARD -> {
				check(seals == 0, where + ": no seal");
				EntityType<?> type = site.id().equals("master_quarry_hall") ? (school.equals("iron") ? EntityTypes.VINDICATOR : EntityTypes.HUSK) : EntityTypes.BOGGED;
				var guards = level.getEntitiesOfClass(Mob.class, AABB.of(box).inflate(1).expandTowards(0, -4, 0), m -> !Runebound.spellOf(m).isEmpty());
				check(guards.size() == 1 && guards.getFirst().getType() == type, where + ": one Runebound guard, found " + guards);
				check(beside(reach, reward), where + ": the reward lies in the sparring pit, a drop away");
				guards.forEach(Mob::discard);
			}
			case CLIMB -> {
				check(seals == 0, where + ": no seal");
				int[][] steps = piece instanceof WindGatePiece ? WindGatePiece.posts() : WaterfallShrinePiece.ledges();
				for (int[] s : steps) check(!level.getBlockState(piece.localPosition(s[0], s[2], s[1])).isAir(), where + ": a step at " + Arrays.toString(s));
				check(reward.getY() - box.minY() >= 12, where + ": the reward sits high, at the top of the climb");
				check(beside(reach, reward), where + ": the climb reaches the reward");
			}
		}
	}

	private static void checkNotes(LecternBlockEntity lectern, Site site, String school, String where) {
		var content = lectern.getBook().get(DataComponents.WRITTEN_BOOK_CONTENT);
		check(content != null && content.pages().size() == 3, where + ": the notes have three pages");
		List<String> pageKeys = new ArrayList<>();
		Object[] args = null;
		for (var page : content.pages()) {
			if (page.raw().getContents() instanceof TranslatableContents t) {
				pageKeys.add(t.getKey());
				if (t.getKey().equals("book.wildercord.master_site.challenge")) args = t.getArgs();
			}
		}
		check(pageKeys.equals(List.of("book.wildercord.master_site." + school, "book.wildercord.master_site.challenge", "book.wildercord." + site.id() + ".trial")),
			where + ": the notes' pages " + pageKeys);
		check(args != null && args.length == 2 && ("/master challenge " + school).equals(String.valueOf(args[1])), where + ": the notes name the challenge command");
	}

	// ------------------------------------------------------------------ walking

	private static boolean beside(Set<BlockPos> reach, BlockPos block) {
		if (block == null) return false;
		for (Direction d : Direction.Plane.HORIZONTAL) if (reach.contains(block.relative(d))) return true;
		return reach.contains(block.above());
	}

	/** Every place a player standing at {@code start} can walk, climb or jump to within {@code area}. */
	private static Set<BlockPos> walk(ServerLevel level, BlockPos start, BoundingBox area) {
		Set<BlockPos> seen = new HashSet<>(List.of(start));
		ArrayDeque<BlockPos> queue = new ArrayDeque<>(List.of(start));
		int[][] around = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
		while (!queue.isEmpty()) {
			BlockPos at = queue.poll();
			List<BlockPos> next = new ArrayList<>();
			boolean canJump = open(level, at.above(2)) || isLadder(level, at);
			for (int[] o : around) {
				boolean diagonal = o[0] != 0 && o[1] != 0;
				for (int dy = 1; dy >= -3; dy--) {
					if (dy == 1 && !canJump) continue;
					BlockPos to = at.offset(o[0], dy, o[1]);
					if (!standable(level, to)) continue;
					int top = Math.max(at.getY(), to.getY());
					if (!clear(level, at.getX() + o[0], at.getZ() + o[1], to.getY(), top + 1)) continue;
					if (diagonal && !clear(level, at.getX() + o[0], at.getZ(), top, top + 1) && !clear(level, at.getX(), at.getZ() + o[1], top, top + 1)) continue;
					next.add(to);
					break;
				}
				// A running jump over a one-block gap.
				if (!diagonal && canJump && clear(level, at.getX() + o[0], at.getZ() + o[1], at.getY(), at.getY() + 2)) {
					for (int dy = 1; dy >= -3; dy--) {
						BlockPos to = at.offset(2 * o[0], dy, 2 * o[1]);
						if (standable(level, to) && clear(level, to.getX(), to.getZ(), to.getY(), Math.max(at.getY(), to.getY()) + 1)) { next.add(to); break; }
					}
				}
			}
			if (isLadder(level, at) && open(level, at.above()) && open(level, at.above(2))) next.add(at.above());
			if (isLadder(level, at.below())) next.add(at.below());
			for (BlockPos to : next) if (area.isInside(to) && seen.add(to)) queue.add(to);
		}
		return seen;
	}

	private static boolean isLadder(ServerLevel level, BlockPos at) { return level.getBlockState(at).is(Blocks.LADDER); }

	/** Nothing to bump into: air, water, a carpet or a low slab, or a ladder to climb. */
	private static boolean open(ServerLevel level, BlockPos at) {
		var state = level.getBlockState(at);
		var shape = state.getCollisionShape(level, at);
		return shape.isEmpty() || shape.max(Direction.Axis.Y) <= 0.5 || state.is(Blocks.LADDER);
	}

	/** The column from y0 to y1 at (x, z) is open. */
	private static boolean clear(ServerLevel level, int x, int z, int y0, int y1) {
		for (int y = y0; y <= y1; y++) if (!open(level, new BlockPos(x, y, z))) return false;
		return true;
	}

	/** A player's feet fit here: room for feet and head, and something to stand on (not a fence top), or water or a ladder. */
	private static boolean standable(ServerLevel level, BlockPos at) {
		if (!open(level, at) || !open(level, at.above())) return false;
		if (isLadder(level, at) || !level.getFluidState(at).isEmpty()) return true;
		var below = level.getBlockState(at.below()).getCollisionShape(level, at.below());
		return !below.isEmpty() && below.max(Direction.Axis.Y) <= 1.0 && below.max(Direction.Axis.Y) > 0.5
			|| isLadder(level, at.below()) || !level.getFluidState(at.below()).isEmpty() && open(level, at.below());
	}

	private static void check(boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}
}
