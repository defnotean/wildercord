package dev.wildercord.world.dungeons;

import dev.wildercord.cast.Runebound;
import dev.wildercord.content.RuneSealBlock;
import dev.wildercord.content.WildercordBlocks;
import dev.wildercord.content.dungeons.DungeonAltarBlock;
import dev.wildercord.content.dungeons.DungeonBlocks;
import dev.wildercord.spell.RuneDef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;

/**
 * What the dimension dungeons' pieces share with the Archive's: each is one piece laid out in its
 * own coordinates (x across, y up, z inward from the entrance) and turned to face a random way, and
 * every choice is a pure function of the spot, so each chunk builds its part the same way whatever
 * order chunks generate in. In a piece's own frame NORTH points inward (+z), SOUTH back out toward
 * the entrance, EAST along +x and WEST along -x.
 */
public abstract class DungeonPiece extends ScatteredFeaturePiece implements WardedPiece {
	protected static final BlockState AIR = Blocks.AIR.defaultBlockState();
	/** Stable across chunk order and reloads, including rotated layouts. */
	public int variant(){return Math.floorMod(getBoundingBox().minX()*31+getBoundingBox().minZ()*17,3);}
	/** Immutable authoring/debug position in the piece's rotated local frame. */
	public BlockPos localPosition(int x,int y,int z){return getWorldPos(x,y,z).immutable();}

	protected DungeonPiece(StructurePieceType type, int x, int y, int z, int width, int height, int depth, Direction facing) {
		super(type, x, y, z, width, height, depth, facing);
	}

	protected DungeonPiece(StructurePieceType type, CompoundTag tag) {
		super(type, tag);
	}

	@Override
	protected void addAdditionalSaveData(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext context, CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
	}

	/** A box given in the piece's own coordinates, turned and placed as the piece is: in the world's. */
	protected BoundingBox worldBox(int x0, int y0, int z0, int x1, int y1, int z1) {
		return BoundingBox.fromCorners(new net.minecraft.core.Vec3i(getWorldX(x0, z0), getWorldY(y0), getWorldZ(x0, z0)),
			new net.minecraft.core.Vec3i(getWorldX(x1, z1), getWorldY(y1), getWorldZ(x1, z1)));
	}

	// ------------------------------------------------------------------ blocks

	protected void fill(WorldGenLevel level, BoundingBox bb, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
		generateBox(level, bb, x0, y0, z0, x1, y1, z1, state, state, false);
	}

	/** A hollow box: {@code wall} on every face (floor and ceiling too), air inside. */
	protected void room(WorldGenLevel level, BoundingBox bb, int x0, int y0, int z0, int x1, int y1, int z1, BlockState wall) {
		generateBox(level, bb, x0, y0, z0, x1, y1, z1, wall, AIR, false);
	}

	@Override
	protected void placeBlock(WorldGenLevel level, BlockState state, int x, int y, int z, BoundingBox bb) {
		BlockPos at = getWorldPos(x, y, z);
		if (!bb.isInside(at)) return;
		if (!state.hasBlockEntity() && level.getBlockState(at).hasBlockEntity()) {
			level.getBlockEntity(at);
			level.getChunk(at).removeBlockEntity(at);
		}
		super.placeBlock(level, state, x, y, z, bb);
		if (state.hasBlockEntity()) level.getBlockEntity(at);
	}

	protected void set(WorldGenLevel level, BoundingBox bb, BlockState state, int x, int y, int z) {
		placeBlock(level, state, x, y, z, bb);
	}

	/**
	 * Clears trees over the piece's footprint, from local y 1 to 30 above its top: leaves, logs, vines and mangrove roots
	 * become air, so a forest site stands in its own clearing. Sites that call this generate after vegetation.
	 */
	protected void clearCanopy(WorldGenLevel level, BoundingBox bb) {
		clearCanopy(level, bb, 1);
	}

	/** {@link #clearCanopy(WorldGenLevel, BoundingBox)} from local {@code y0} up, so roots under water stay put. */
	protected void clearCanopy(WorldGenLevel level, BoundingBox bb, int y0) {
		int m = margin(), w = boundingBox.getXSpan() - m, d = boundingBox.getZSpan() - m, top = boundingBox.getYSpan() + 30;
		for (int x = -m; x < w; x++) {
			for (int z = -m; z < d; z++) {
				for (int y = y0; y <= top; y++) {
					BlockState state = getBlock(level, x, y, z, bb);
					if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(Blocks.VINE) || state.is(Blocks.MANGROVE_ROOTS)
							|| state.is(Blocks.MUDDY_MANGROVE_ROOTS) || state.is(Blocks.COCOA) || state.is(Blocks.BEE_NEST)) {
						set(level, bb, AIR, x, y, z);
					}
				}
			}
		}
	}

	/** Open ground the box keeps on every side of the site; local (0, 0) stays the site's own corner. */
	protected int margin() {
		return 0;
	}

	@Override protected int getWorldX(int x, int z) { return super.getWorldX(x + margin(), z + margin()); }
	@Override protected int getWorldZ(int x, int z) { return super.getWorldZ(x + margin(), z + margin()); }

	/** A deterministic 0-99 for a spot, so the same column always gets the same variation. */
	protected static int noise(int x, int z) {
		int h = x * 73856093 ^ z * 19349663;
		h ^= h >>> 13;
		h *= 0x5bd1e995;
		h ^= h >>> 15;
		return Math.floorMod(h, 100);
	}

	protected static int noise(int x, int y, int z) {
		return noise(x * 31 + y * 7, z * 13 - y);
	}

	// ------------------------------------------------------------------ doors, chests, guards, the altar

	/**
	 * A door of Rune Seals across the piece at {@code z}, {@code x0}-{@code x1} wide and {@code y0}-{@code y1}
	 * high, checkered with two elements.
	 */
	protected void sealDoorAcross(WorldGenLevel level, BoundingBox bb, int z, int x0, int x1, int y0, int y1, RuneSealBlock.Element a,
			RuneSealBlock.Element b) {
		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				set(level, bb, seal((x + y) % 2 == 0 ? a : b), x, y, z);
			}
		}
	}

	/** The same, set in a wall that runs along z (a door in a side wall, at {@code x}). */
	protected void sealDoorAlong(WorldGenLevel level, BoundingBox bb, int x, int z0, int z1, int y0, int y1, RuneSealBlock.Element a,
			RuneSealBlock.Element b) {
		for (int z = z0; z <= z1; z++) {
			for (int y = y0; y <= y1; y++) {
				set(level, bb, seal((z + y) % 2 == 0 ? a : b), x, y, z);
			}
		}
	}

	private static BlockState seal(RuneSealBlock.Element element) {
		return WildercordBlocks.RUNE_SEAL.defaultBlockState().setValue(RuneSealBlock.ELEMENT, element);
	}

	/** A chest facing {@code facing} (in the piece's frame), filled from {@code loot}. */
	protected void chest(WorldGenLevel level, BoundingBox bb, int x, int y, int z, ResourceKey<LootTable> loot, Direction facing, long salt) {
		RandomSource random = RandomSource.create(boundingBox.minX() * 31L + boundingBox.minZ() * 17L + salt);
		BlockState state = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing);
		// Turned with the piece, as every other block is (a chest given a state isn't, otherwise).
		createChest(level, bb, random, getWorldPos(x, y, z), loot, state.mirror(getMirror()).rotate(getRotation()));
	}

	/** The altar its boss rises from. */
	protected void altar(WorldGenLevel level, BoundingBox bb, DungeonAltarBlock.Kind kind, int x, int y, int z) {
		set(level, bb, DungeonBlocks.ALTAR.defaultBlockState().setValue(DungeonAltarBlock.KIND, kind), x, y, z);
	}
	/** Mechanisms face the next local z coordinate; derive the world direction once, without a second state rotation. */
	protected net.minecraft.core.Direction inward(int x,int z) {
		BlockPos from=getWorldPos(x,1,z),to=getWorldPos(x,1,z+1);
		return to.getX()>from.getX()?Direction.EAST:to.getX()<from.getX()?Direction.WEST:to.getZ()>from.getZ()?Direction.SOUTH:Direction.NORTH;
	}
	protected void mechanism(WorldGenLevel level,BoundingBox bb,dev.wildercord.content.dungeons.ExpeditionMechanism block,int x,int y,int z) {
		BlockPos at=getWorldPos(x,y,z);if(bb.isInside(at))level.setBlock(at,block.defaultBlockState().setValue(dev.wildercord.content.dungeons.ExpeditionMechanism.FACING,inward(x,z)),2);
	}

	/** A Runebound guard carrying {@code spell}, placed once: by the chunk its feet are in. */
	protected void guard(WorldGenLevel level, BoundingBox bb, EntityType<? extends Mob> type, int x, int y, int z, List<RuneDef> spell, boolean adept) {
		BlockPos pos = getWorldPos(x, y, z);
		if (!bb.isInside(pos)) {
			return;
		}
		Mob mob = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (mob == null) {
			return;
		}
		mob.setPersistenceRequired();
		mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
		Runebound.bindAtGeneration(mob, spell, adept);
		level.addFreshEntityWithPassengers(mob);
	}

	/** A plain (not Runebound) guard, placed the same way. */
	protected void sentry(WorldGenLevel level, BoundingBox bb, EntityType<? extends Mob> type, int x, int y, int z) {
		BlockPos pos = getWorldPos(x, y, z);
		if (!bb.isInside(pos)) {
			return;
		}
		Mob mob = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (mob == null) {
			return;
		}
		mob.setPersistenceRequired();
		mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
		level.addFreshEntityWithPassengers(mob);
	}

	// ------------------------------------------------------------------ architecture

	/**
	 * A weighted mix of blocks picked per spot, so a wall reads as worn masonry instead of one flat texture. Built from
	 * pairs: {@code Palette.of(stoneBricks, 6, mossy, 2, cracked, 1)}.
	 */
	protected record Palette(BlockState[] blocks, int[] weights, int total) {
		public static Palette of(Object... pairs) {
			BlockState[] blocks = new BlockState[pairs.length / 2];
			int[] weights = new int[blocks.length];
			int total = 0;
			for (int i = 0; i < blocks.length; i++) {
				Object b = pairs[i * 2];
				blocks[i] = b instanceof net.minecraft.world.level.block.Block block ? block.defaultBlockState() : (BlockState) b;
				weights[i] = (Integer) pairs[i * 2 + 1];
				total += weights[i];
			}
			return new Palette(blocks, weights, total);
		}

		public BlockState at(int x, int y, int z) {
			int n = noise(x, y, z) * total / 100;
			for (int i = 0; i < blocks.length; i++) {
				if ((n -= weights[i]) < 0) return blocks[i];
			}
			return blocks[blocks.length - 1];
		}
	}

	/** A solid box of a palette. */
	protected void fill(WorldGenLevel level, BoundingBox bb, int x0, int y0, int z0, int x1, int y1, int z1, Palette p) {
		for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) set(level, bb, p.at(x, y, z), x, y, z);
	}

	/** The four walls of a box in a palette, floor and ceiling left alone. */
	protected void walls(WorldGenLevel level, BoundingBox bb, int x0, int y0, int z0, int x1, int y1, int z1, Palette p) {
		fill(level, bb, x0, y0, z0, x1, y1, z0, p);
		fill(level, bb, x0, y0, z1, x1, y1, z1, p);
		fill(level, bb, x0, y0, z0 + 1, x0, y1, z1 - 1, p);
		fill(level, bb, x1, y0, z0 + 1, x1, y1, z1 - 1, p);
	}

	/** A local-frame z step for a direction: NORTH is inward, +z. */
	protected static int dz(Direction d) {
		return -d.getStepZ();
	}

	/** A stair in the piece's frame climbing toward {@code facing}; {@code top} hangs it upside down (eaves, corbels, sills). */
	protected static BlockState stair(net.minecraft.world.level.block.Block stairs, Direction facing, boolean top) {
		return stairs.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, facing)
			.setValue(net.minecraft.world.level.block.StairBlock.HALF, top ? net.minecraft.world.level.block.state.properties.Half.TOP
				: net.minecraft.world.level.block.state.properties.Half.BOTTOM);
	}

	/** A top slab, for ledges and string courses. */
	protected static BlockState topSlab(net.minecraft.world.level.block.Block slab) {
		return slab.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP);
	}

	/** A column with a plinth and a capital: {@code base} at y0, {@code cap} at y1, {@code shaft} between. */
	protected void pillar(WorldGenLevel level, BoundingBox bb, int x, int z, int y0, int y1, BlockState base, BlockState shaft, BlockState cap) {
		set(level, bb, base, x, y0, z);
		fill(level, bb, x, y0 + 1, z, x, y1 - 1, z, shaft);
		set(level, bb, cap, x, y1, z);
	}

	/**
	 * A stepped buttress against a wall at (x, z), standing out toward {@code out}: full height {@code h} at the wall, half
	 * height one step out, a quarter two steps out, each step capped by a stair sloping back to the wall.
	 */
	protected void buttress(WorldGenLevel level, BoundingBox bb, int x, int z, int y0, int h, Direction out, BlockState body,
			net.minecraft.world.level.block.Block stairs) {
		int[] heights = {h, Math.max(1, h / 2), Math.max(1, h / 4)};
		for (int i = 0; i < 3; i++) {
			int sx = x + out.getStepX() * i, sz = z + dz(out) * i;
			fill(level, bb, sx, y0, sz, sx, y0 + heights[i] - 1, sz, body);
			if (i > 0) set(level, bb, stair(stairs, out.getOpposite(), false), sx, y0 + heights[i], sz);
		}
	}

	/**
	 * A hipped roof over x0..x1 by z0..z1, overhanging one block: rings of stairs climbing inward from {@code y}, outer
	 * corners turned, the inside of each ring filled with {@code under} so nothing sees through, {@code cap} along the top.
	 */
	protected void hipRoof(WorldGenLevel level, BoundingBox bb, int x0, int z0, int x1, int z1, int y,
			net.minecraft.world.level.block.Block stairs, BlockState under, BlockState cap) {
		x0--; z0--; x1++; z1++;
		for (int k = 0; ; k++) {
			int a = x0 + k, b = x1 - k, c = z0 + k, d = z1 - k, yy = y + k;
			if (a > b || c > d) break;
			if (a == b || c == d) { fill(level, bb, a, yy, c, b, yy, d, cap); break; }
			for (int x = a + 1; x < b; x++) {
				set(level, bb, stair(stairs, Direction.NORTH, false), x, yy, c);
				set(level, bb, stair(stairs, Direction.SOUTH, false), x, yy, d);
			}
			for (int z = c + 1; z < d; z++) {
				set(level, bb, stair(stairs, Direction.EAST, false), a, yy, z);
				set(level, bb, stair(stairs, Direction.WEST, false), b, yy, z);
			}
			var outer = net.minecraft.world.level.block.StairBlock.SHAPE;
			set(level, bb, stair(stairs, Direction.NORTH, false).setValue(outer, net.minecraft.world.level.block.state.properties.StairsShape.OUTER_RIGHT), a, yy, c);
			set(level, bb, stair(stairs, Direction.NORTH, false).setValue(outer, net.minecraft.world.level.block.state.properties.StairsShape.OUTER_LEFT), b, yy, c);
			set(level, bb, stair(stairs, Direction.SOUTH, false).setValue(outer, net.minecraft.world.level.block.state.properties.StairsShape.OUTER_LEFT), a, yy, d);
			set(level, bb, stair(stairs, Direction.SOUTH, false).setValue(outer, net.minecraft.world.level.block.state.properties.StairsShape.OUTER_RIGHT), b, yy, d);
			if (k > 0 && b - a > 1 && d - c > 1) fill(level, bb, a + 1, yy, c + 1, b - 1, yy, d - 1, under);
		}
	}

	/**
	 * A steep gable roof over x0..x1 by z0..z1 whose eaves overhang a block and whose ends overhang a block too, with a row
	 * of upside-down stairs under each eave. The ridge runs along x when {@code alongX}, along z otherwise; the gable ends
	 * are filled from {@code gableFill}. Returns the ridge's height.
	 */
	protected int gableRoof(WorldGenLevel level, BoundingBox bb, int x0, int z0, int x1, int z1, int y, boolean alongX,
			net.minecraft.world.level.block.Block stairs, BlockState ridge, Palette gableFill) {
		int lo = alongX ? z0 - 1 : x0 - 1, hi = alongX ? z1 + 1 : x1 + 1;
		int e0 = alongX ? x0 - 1 : z0 - 1, e1 = alongX ? x1 + 1 : z1 + 1;
		Direction up = alongX ? Direction.NORTH : Direction.EAST;
		int top = y;
		for (int k = 0; lo + k <= hi - k; k++) {
			int s = lo + k, n = hi - k, yy = y + k;
			top = yy;
			for (int e = e0; e <= e1; e++) {
				if (s == n) {
					put(level, bb, ridge, alongX, e, yy, s);
				} else {
					put(level, bb, stair(stairs, up, false), alongX, e, yy, s);
					put(level, bb, stair(stairs, up.getOpposite(), false), alongX, e, yy, n);
					if (k == 0) {
						put(level, bb, stair(stairs, up, true), alongX, e, yy - 1, s);
						put(level, bb, stair(stairs, up.getOpposite(), true), alongX, e, yy - 1, n);
					}
				}
			}
			for (int m = s + 1; m < n; m++) {
				int[] ends = {alongX ? x0 : z0, alongX ? x1 : z1};
				for (int e : ends) put(level, bb, alongX ? gableFill.at(e, yy, m) : gableFill.at(m, yy, e), alongX, e, yy, m);
			}
			if (s >= n) break;
		}
		return top;
	}

	/** Sets a block given along-the-ridge {@code e} and across-the-ridge {@code m} coordinates. */
	private void put(WorldGenLevel level, BoundingBox bb, BlockState state, boolean alongX, int e, int y, int m) {
		if (alongX) set(level, bb, state, e, y, m); else set(level, bb, state, m, y, e);
	}

	/** A square spire: a hipped cone of stairs from (cx±r, cz±r) at {@code y}, ending in {@code tip} one block over its peak. */
	protected void spire(WorldGenLevel level, BoundingBox bb, int cx, int cz, int r, int y, net.minecraft.world.level.block.Block stairs,
			BlockState under, BlockState peak, BlockState tip) {
		for (int k = 0; k < r; k++) {
			// Two courses per ring, for a needle rather than a tent.
			for (int j = 0; j < 2; j++) {
				int yy = y + k * 2 + j, s = r - k;
				hipRing(level, bb, cx - s, cz - s, cx + s, cz + s, yy, stairs, under, j == 0);
			}
		}
		set(level, bb, peak, cx, y + r * 2, cz);
		set(level, bb, tip, cx, y + r * 2 + 1, cz);
	}

	private void hipRing(WorldGenLevel level, BoundingBox bb, int a, int c, int b, int d, int y, net.minecraft.world.level.block.Block stairs,
			BlockState under, boolean solid) {
		for (int x = a; x <= b; x++) for (int z = c; z <= d; z++) {
			boolean edge = x == a || x == b || z == c || z == d;
			if (!edge) { set(level, bb, under, x, y, z); continue; }
			if (solid) { set(level, bb, under, x, y, z); continue; }
			Direction f = z == c ? Direction.NORTH : z == d ? Direction.SOUTH : x == a ? Direction.EAST : Direction.WEST;
			set(level, bb, stair(stairs, f, false), x, y, z);
		}
	}

	/** Merlons on every other block of a rectangle's rim at {@code y}, the corners always raised. */
	protected void crenellate(WorldGenLevel level, BoundingBox bb, int x0, int z0, int x1, int z1, int y, BlockState merlon) {
		for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
			boolean rim = x == x0 || x == x1 || z == z0 || z == z1;
			boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
			if (rim && (corner || (x + z) % 2 == 0)) set(level, bb, merlon, x, y, z);
		}
	}

	/** A lantern hanging under (x, y, z) on {@code chain} blocks of chain. */
	protected void hangingLantern(WorldGenLevel level, BoundingBox bb, int x, int y, int z, int chain, boolean soul) {
		for (int i = 0; i < chain; i++) set(level, bb, Blocks.IRON_CHAIN.defaultBlockState(), x, y - i, z);
		set(level, bb, (soul ? Blocks.SOUL_LANTERN : Blocks.LANTERN).defaultBlockState()
			.setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), x, y - chain, z);
	}

	/** A local-frame direction turned into its true world direction for this piece's orientation. */
	protected Direction worldDir(int x, int y, int z, Direction facing) {
		BlockPos from = getWorldPos(x, y, z), to = getWorldPos(x + facing.getStepX(), y, z + dz(facing));
		return to.getX() > from.getX() ? Direction.EAST : to.getX() < from.getX() ? Direction.WEST
			: to.getZ() > from.getZ() ? Direction.SOUTH : Direction.NORTH;
	}

	/** A wall banner at (x, y, z) hung on the block behind it, facing {@code facing} in the piece's frame. */
	protected void banner(WorldGenLevel level, BoundingBox bb, net.minecraft.world.level.block.Block wallBanner, int x, int y, int z, Direction facing) {
		BlockPos at = getWorldPos(x, y, z);
		if (!bb.isInside(at)) return;
		level.setBlock(at, wallBanner.defaultBlockState().setValue(net.minecraft.world.level.block.WallBannerBlock.FACING, worldDir(x, y, z, facing)), 2);
		level.getBlockEntity(at);
	}

	/** A wall torch or wall lantern-like block on the wall behind (x, y, z), facing {@code facing}. */
	protected void sconce(WorldGenLevel level, BoundingBox bb, int x, int y, int z, Direction facing, boolean soul) {
		BlockPos at = getWorldPos(x, y, z);
		if (!bb.isInside(at)) return;
		level.setBlock(at, (soul ? Blocks.SOUL_WALL_TORCH : Blocks.WALL_TORCH).defaultBlockState()
			.setValue(net.minecraft.world.level.block.WallTorchBlock.FACING, worldDir(x, y, z, facing)), 2);
	}

	/** Swaps some of the {@code from} blocks in a box for {@code to} ({@code chance} in 100), for moss, cracks and wear. */
	protected void weather(WorldGenLevel level, BoundingBox bb, int x0, int y0, int z0, int x1, int y1, int z1, BlockState from, BlockState to, int chance) {
		for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) {
			if (noise(x + 101, y, z - 37) < chance && getBlock(level, x, y, z, bb).is(from.getBlock())) set(level, bb, to, x, y, z);
		}
	}

	/** Puts {@code what} on some open tops in a box ({@code chance} in 100): air with a solid block beneath. */
	protected void scatter(WorldGenLevel level, BoundingBox bb, int x0, int y0, int z0, int x1, int y1, int z1, BlockState what, int chance) {
		for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) {
			if (noise(x - 59, y, z + 83) >= chance) continue;
			if (getBlock(level, x, y, z, bb).isAir() && getBlock(level, x, y - 1, z, bb).isSolid()) set(level, bb, what, x, y, z);
		}
	}

	/** The distance from a column to a circle's centre. */
	protected static double dist(int x, int z, int cx, int cz) {
		return Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
	}

	/** Whether a column lies on one of {@code spokes} straight lines out from a centre (a star map's rays, a forge's spokes). */
	protected static boolean onSpoke(int x, int z, int cx, int cz, int spokes, double width) {
		double a = Math.atan2(z - cz, x - cx);
		double r = dist(x, z, cx, cz);
		double step = Math.PI * 2 / spokes;
		double off = Math.abs(Math.IEEEremainder(a, step));
		return r * Math.sin(Math.min(off, Math.PI / 2)) < width;
	}
}
