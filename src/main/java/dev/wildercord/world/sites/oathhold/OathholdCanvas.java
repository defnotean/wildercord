package dev.wildercord.world.sites.oathhold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Chunk-clipped architectural drafting engine for Oathhold.
 *
 * <p>All coordinates {@code (lx, ly, lz)} are city-local relative to {@code (originX, baseY, originZ)} where
 * {@code +X} is East, {@code +Z} is South, {@code -Z} is North, and {@code ly = 0} is the main lower city deck.
 * Every fill and placement method clips tightly against {@link BoundingBox clip} so each chunk generates only
 * its own blocks in O(chunk) time.</p>
 */
public final class OathholdCanvas {
	public static final BlockState AIR = Blocks.AIR.defaultBlockState();
	public static final BlockState WATER = Blocks.WATER.defaultBlockState();
	public static final BlockState STONE_BRICKS = Blocks.STONE_BRICKS.defaultBlockState();
	public static final BlockState CRACKED_STONE_BRICKS = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
	public static final BlockState MOSSY_STONE_BRICKS = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
	public static final BlockState CHISELED_STONE_BRICKS = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
	public static final BlockState TUFF_BRICKS = Blocks.TUFF_BRICKS.defaultBlockState();
	public static final BlockState CHISELED_TUFF_BRICKS = Blocks.CHISELED_TUFF_BRICKS.defaultBlockState();
	public static final BlockState POLISHED_DEEPSLATE = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
	public static final BlockState DEEPSLATE_BRICKS = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
	public static final BlockState DEEPSLATE_TILES = Blocks.DEEPSLATE_TILES.defaultBlockState();
	public static final BlockState COBBLED_DEEPSLATE = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
	public static final BlockState CALCITE = Blocks.CALCITE.defaultBlockState();
	public static final BlockState POLISHED_ANDESITE = Blocks.POLISHED_ANDESITE.defaultBlockState();
	public static final BlockState ANDESITE = Blocks.ANDESITE.defaultBlockState();
	public static final BlockState COBBLESTONE = Blocks.COBBLESTONE.defaultBlockState();
	public static final BlockState BRICKS = Blocks.BRICKS.defaultBlockState();
	public static final BlockState DARK_OAK = Blocks.DARK_OAK_PLANKS.defaultBlockState();
	public static final BlockState DARK_OAK_LOG = Blocks.DARK_OAK_LOG.defaultBlockState();
	public static final BlockState SPRUCE = Blocks.SPRUCE_PLANKS.defaultBlockState();
	public static final BlockState SPRUCE_LOG = Blocks.SPRUCE_LOG.defaultBlockState();
	public static final BlockState GOLD = Blocks.GOLD_BLOCK.defaultBlockState();
	public static final BlockState RAW_GOLD = Blocks.RAW_GOLD_BLOCK.defaultBlockState();
	public static final BlockState GLOWSTONE = Blocks.GLOWSTONE.defaultBlockState();
	public static final BlockState OCHRE_LIGHT = Blocks.OCHRE_FROGLIGHT.defaultBlockState();
	public static final BlockState SEA_LANTERN = Blocks.SEA_LANTERN.defaultBlockState();
	public static final BlockState LANTERN = Blocks.LANTERN.defaultBlockState();
	public static final BlockState HANGING_LANTERN = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
	public static final BlockState HANGING_SOUL_LANTERN = Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
	public static final BlockState CHAIN = Blocks.IRON_CHAIN.defaultBlockState();
	public static final BlockState IRON_BARS = Blocks.IRON_BARS.defaultBlockState();
	public static final BlockState EMERALD_GREEN = Blocks.GREEN_CONCRETE.defaultBlockState();
	public static final BlockState EMERALD_TEAL = Blocks.CYAN_TERRACOTTA.defaultBlockState();
	public static final BlockState BANNER_GOLD = Blocks.YELLOW_TERRACOTTA.defaultBlockState();
	public static final BlockState COPPER_WEATHERED = Blocks.WAXED_WEATHERED_CUT_COPPER.defaultBlockState();
	public static final BlockState COPPER_OXIDIZED = Blocks.WAXED_OXIDIZED_COPPER.defaultBlockState();
	public static final BlockState COPPER_CUT_OXIDIZED = Blocks.WAXED_OXIDIZED_CUT_COPPER.defaultBlockState();

	private final WorldGenLevel level;
	private final BoundingBox clip;
	private final int originX;
	private final int baseY;
	private final int originZ;
	private final RandomSource random;
	private final int minLx, maxLx, minLz, maxLz;

	public OathholdCanvas(WorldGenLevel level, BoundingBox clip, int originX, int baseY, int originZ, RandomSource random) {
		this.level = level;
		this.clip = clip;
		this.originX = originX;
		this.baseY = baseY;
		this.originZ = originZ;
		this.random = random;
		this.minLx = clip.minX() - originX;
		this.maxLx = clip.maxX() - originX;
		this.minLz = clip.minZ() - originZ;
		this.maxLz = clip.maxZ() - originZ;
	}

	public WorldGenLevel level() { return level; }
	public BoundingBox clip() { return clip; }
	public int originX() { return originX; }
	public int baseY() { return baseY; }
	public int originZ() { return originZ; }
	public RandomSource random() { return random; }
	public int clipMinLx() { return minLx; }
	public int clipMaxLx() { return maxLx; }
	public int clipMinLz() { return minLz; }
	public int clipMaxLz() { return maxLz; }
	public int wx(int lx) { return originX + lx; }
	public int wy(int ly) { return baseY + ly; }
	public int wz(int lz) { return originZ + lz; }

	public boolean intersects(int x0, int z0, int x1, int z1) {
		return x1 >= minLx && x0 <= maxLx && z1 >= minLz && z0 <= maxLz;
	}

	public boolean inClip(int lx, int ly, int lz) {
		return lx >= minLx && lx <= maxLx && lz >= minLz && lz <= maxLz && wy(ly) >= clip.minY() && wy(ly) <= clip.maxY();
	}

	public static int hash(int x, int y, int z) {
		int h = x * 73856093 ^ y * 19349663 ^ z * 83492791;
		h ^= h >>> 13;
		h *= 0x5bd1e995;
		h ^= h >>> 15;
		return Math.floorMod(h, 100);
	}

	public static BlockState stair(Block block, Direction facing, boolean top) {
		return block.defaultBlockState()
			.setValue(StairBlock.FACING, facing)
			.setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
	}

	public static BlockState slab(Block block, boolean top) {
		return block.defaultBlockState().setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
	}

	public static BlockState doubleSlab(Block block) {
		return block.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE);
	}

	public void set(int lx, int ly, int lz, BlockState state) {
		int wx = originX + lx;
		int wy = baseY + ly;
		int wz = originZ + lz;
		if (wx < clip.minX() || wx > clip.maxX() || wz < clip.minZ() || wz > clip.maxZ() || wy < clip.minY() || wy > clip.maxY()) {
			return;
		}
		BlockPos at = new BlockPos(wx, wy, wz);
		if (!state.hasBlockEntity() && level.getBlockState(at).hasBlockEntity()) {
			level.getChunk(at).removeBlockEntity(at);
		}
		level.setBlock(at, state, 2);
	}

	public BlockState get(int lx, int ly, int lz) {
		int wx = originX + lx;
		int wy = baseY + ly;
		int wz = originZ + lz;
		if (wx < clip.minX() || wx > clip.maxX() || wz < clip.minZ() || wz > clip.maxZ() || wy < clip.minY() || wy > clip.maxY()) {
			return AIR;
		}
		return level.getBlockState(new BlockPos(wx, wy, wz));
	}

	public void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
		int cx0 = Math.max(Math.min(x0, x1), minLx);
		int cx1 = Math.min(Math.max(x0, x1), maxLx);
		int cz0 = Math.max(Math.min(z0, z1), minLz);
		int cz1 = Math.min(Math.max(z0, z1), maxLz);
		if (cx0 > cx1 || cz0 > cz1) return;
		int cy0 = Math.max(Math.min(y0, y1), clip.minY() - baseY);
		int cy1 = Math.min(Math.max(y0, y1), clip.maxY() - baseY);
		for (int x = cx0; x <= cx1; x++) {
			for (int z = cz0; z <= cz1; z++) {
				for (int y = cy0; y <= cy1; y++) {
					set(x, y, z, state);
				}
			}
		}
	}

	public void clear(int x0, int y0, int z0, int x1, int y1, int z1) {
		fill(x0, y0, z0, x1, y1, z1, AIR);
	}

	/** Clears only non-air blocks in a vertical column inside the clip box using a mutable BlockPos. */
	public void clearNonAirColumn(int lx, int y0, int y1, int lz) {
		int wx = originX + lx;
		int wz = originZ + lz;
		if (wx < clip.minX() || wx > clip.maxX() || wz < clip.minZ() || wz > clip.maxZ()) {
			return;
		}
		int wy0 = Math.max(clip.minY(), baseY + Math.min(y0, y1));
		int wy1 = Math.min(clip.maxY(), baseY + Math.max(y0, y1));
		if (wy0 > wy1) return;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(wx, wy0, wz);
		for (int wy = wy0; wy <= wy1; wy++) {
			pos.setY(wy);
			BlockState cur = level.getBlockState(pos);
			if (!cur.isAir()) {
				if (cur.hasBlockEntity()) {
					level.getChunk(pos).removeBlockEntity(pos);
				}
				level.setBlock(pos, AIR, 2);
			}
		}
	}

	/** Rich weathered castle masonry blending stone bricks, tuff bricks, cracked stone, andesites, and moss. */
	public void fillWeatheredStone(int x0, int y0, int z0, int x1, int y1, int z1) {
		int cx0 = Math.max(Math.min(x0, x1), minLx), cx1 = Math.min(Math.max(x0, x1), maxLx);
		int cz0 = Math.max(Math.min(z0, z1), minLz), cz1 = Math.min(Math.max(z0, z1), maxLz);
		if (cx0 > cx1 || cz0 > cz1) return;
		int cy0 = Math.max(Math.min(y0, y1), clip.minY() - baseY);
		int cy1 = Math.min(Math.max(y0, y1), clip.maxY() - baseY);
		for (int x = cx0; x <= cx1; x++) {
			for (int z = cz0; z <= cz1; z++) {
				for (int y = cy0; y <= cy1; y++) {
					int n = hash(x, y, z);
					BlockState st = n < 54 ? STONE_BRICKS : n < 72 ? TUFF_BRICKS : n < 87 ? CRACKED_STONE_BRICKS : n < 95 ? ANDESITE : MOSSY_STONE_BRICKS;
					set(x, y, z, st);
				}
			}
		}
	}

	/** Heavy dark deepslate fortress base masonry matching lower walls in concept art. */
	public void fillDeepslateBase(int x0, int y0, int z0, int x1, int y1, int z1) {
		int cx0 = Math.max(Math.min(x0, x1), minLx), cx1 = Math.min(Math.max(x0, x1), maxLx);
		int cz0 = Math.max(Math.min(z0, z1), minLz), cz1 = Math.min(Math.max(z0, z1), maxLz);
		if (cx0 > cx1 || cz0 > cz1) return;
		int cy0 = Math.max(Math.min(y0, y1), clip.minY() - baseY);
		int cy1 = Math.min(Math.max(y0, y1), clip.maxY() - baseY);
		for (int x = cx0; x <= cx1; x++) {
			for (int z = cz0; z <= cz1; z++) {
				for (int y = cy0; y <= cy1; y++) {
					int n = hash(x, y, z);
					BlockState st = n < 50 ? DEEPSLATE_BRICKS : n < 82 ? DEEPSLATE_TILES : COBBLED_DEEPSLATE;
					set(x, y, z, st);
				}
			}
		}
	}

	/** Warm patterned cobblestone, stone brick, and tuff-brick street paving. */
	public void fillPaving(int x0, int ly, int z0, int x1, int z1) {
		int cx0 = Math.max(Math.min(x0, x1), minLx), cx1 = Math.min(Math.max(x0, x1), maxLx);
		int cz0 = Math.max(Math.min(z0, z1), minLz), cz1 = Math.min(Math.max(z0, z1), maxLz);
		if (cx0 > cx1 || cz0 > cz1) return;
		for (int x = cx0; x <= cx1; x++) {
			for (int z = cz0; z <= cz1; z++) {
				int n = hash(x, ly, z);
				BlockState st = ((Math.floorMod(x, 6) == 0 || Math.floorMod(z, 6) == 0))
					? POLISHED_ANDESITE
					: (n < 44 ? STONE_BRICKS : n < 70 ? COBBLESTONE : n < 88 ? TUFF_BRICKS : CRACKED_STONE_BRICKS);
				set(x, ly, z, st);
			}
		}
	}

	/** Pointed Gothic arch tunnel/wall opening running along the Z axis centered at {@code cx}. */
	public void gothicArchZ(int cx, int y0, int z0, int z1, int halfSpan, int straightH, int archH, BlockState frame, BlockState interior) {
		if (!intersects(cx - halfSpan - 1, z0, cx + halfSpan + 1, z1)) return;
		for (int dy = 0; dy <= straightH + archH; dy++) {
			int w = dy <= straightH
				? halfSpan
				: Math.max(0, (int) Math.round(halfSpan * Math.sqrt(Math.max(0.0, 1.0 - Math.pow((dy - straightH) / (double) Math.max(1, archH), 1.7)))));
			for (int z = z0; z <= z1; z++) {
				set(cx - w - 1, y0 + dy, z, frame);
				set(cx + w + 1, y0 + dy, z, frame);
				for (int dx = -w; dx <= w; dx++) {
					set(cx + dx, y0 + dy, z, interior);
				}
			}
		}
	}

	/** Pointed Gothic arch opening running along the X axis centered at {@code cz}. */
	public void gothicArchX(int x0, int x1, int y0, int cz, int halfSpan, int straightH, int archH, BlockState frame, BlockState interior) {
		if (!intersects(x0, cz - halfSpan - 1, x1, cz + halfSpan + 1)) return;
		for (int dy = 0; dy <= straightH + archH; dy++) {
			int w = dy <= straightH
				? halfSpan
				: Math.max(0, (int) Math.round(halfSpan * Math.sqrt(Math.max(0.0, 1.0 - Math.pow((dy - straightH) / (double) Math.max(1, archH), 1.7)))));
			for (int x = x0; x <= x1; x++) {
				set(x, y0 + dy, cz - w - 1, frame);
				set(x, y0 + dy, cz + w + 1, frame);
				for (int dz = -w; dz <= w; dz++) {
					set(x, y0 + dy, cz + dz, interior);
				}
			}
		}
	}

	/** Circular gothic stained-glass rose window facing along Z at {@code (cx, cy, z)}. */
	public void roseWindowZ(int cx, int cy, int z, int radius, BlockState glassA, BlockState glassB, BlockState frame) {
		if (!intersects(cx - radius - 1, z, cx + radius + 1, z)) return;
		for (int dx = -radius - 1; dx <= radius + 1; dx++) {
			for (int dy = -radius - 1; dy <= radius + 1; dy++) {
				double d = Math.sqrt(dx * dx + dy * dy);
				if (d > radius + 1.1) continue;
				if (d >= radius - 0.3) {
					set(cx + dx, cy + dy, z, frame);
				} else if (d < 1.8) {
					set(cx + dx, cy + dy, z, GLOWSTONE);
				} else if (Math.abs(dx) == 0 || Math.abs(dy) == 0 || Math.abs(Math.abs(dx) - Math.abs(dy)) <= 0 && d > 2.5) {
					set(cx + dx, cy + dy, z, (dx == 0 || dy == 0) ? frame : glassB);
				} else {
					set(cx + dx, cy + dy, z, ((dx + dy) & 1) == 0 ? glassA : glassB);
				}
			}
		}
	}

	/** Circular gothic stained-glass rose window with an open daylight backing plane at {@code clearZ}. */
	public void roseWindowZOpen(int cx, int cy, int glassZ, int clearZ, int radius,
			BlockState glassA, BlockState glassB, BlockState frame) {
		int minZ = Math.min(glassZ, clearZ), maxZ = Math.max(glassZ, clearZ);
		if (!intersects(cx - radius - 1, minZ, cx + radius + 1, maxZ)) return;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -radius; dy <= radius; dy++) {
				if (Math.sqrt(dx * dx + dy * dy) < radius - 0.3) {
					set(cx + dx, cy + dy, clearZ, AIR);
				}
			}
		}
		roseWindowZ(cx, cy, glassZ, radius, glassA, glassB, frame);
	}

	/** Steep pitched gable roof whose ridge runs along Z over {@code [x0..x1] x [z0..z1]}. */
	public void pitchedRoofZ(int x0, int y0, int z0, int x1, int z1, Block stairBlock, Block slabBlock, BlockState gableWall, BlockState trim) {
		if (!intersects(x0 - 1, z0 - 1, x1 + 1, z1 + 1)) return;
		int half = (x1 - x0) / 2;
		int midX = (x0 + x1) / 2;
		boolean timberedGable = gableWall.equals(CALCITE) || gableWall.is(Blocks.SMOOTH_SANDSTONE);
		for (int k = 0; k <= half + 1; k++) {
			int lx = x0 + k - 1;
			int rx = x1 - k + 1;
			int yy = y0 + k;
			if (lx > rx) break;
			if (lx == rx) {
				fill(lx, yy, z0 - 1, rx, yy, z1 + 1, slab(slabBlock, false));
				break;
			}
			fill(lx, yy, z0 - 1, lx, yy, z1 + 1, stair(stairBlock, Direction.EAST, false));
			fill(rx, yy, z0 - 1, rx, yy, z1 + 1, stair(stairBlock, Direction.WEST, false));
			if (lx + 1 <= rx - 1 && k > 0) {
				if (timberedGable) {
					boolean collar = (k % 3) == 0;
					for (int x = Math.max(lx + 1, minLx); x <= Math.min(rx - 1, maxLx); x++) {
						boolean stud = collar || x == midX || Math.floorMod(Math.abs(x - midX), 4) == 0;
						boolean atticWin = half >= 4 && x == midX && (k == 2 || k == 3);
						BlockState fillBlock = atticWin ? Blocks.GLASS_PANE.defaultBlockState() : (stud ? DARK_OAK_LOG : gableWall);
						set(x, yy, z0, fillBlock);
						set(x, yy, z1, fillBlock);
					}
				} else {
					fill(lx + 1, yy, z0, rx - 1, yy, z0, gableWall);
					fill(lx + 1, yy, z1, rx - 1, yy, z1, gableWall);
				}
			}
			if (k > 0) {
				set(lx, yy, z0 - 1, trim);
				set(rx, yy, z0 - 1, trim);
				set(lx, yy, z1 + 1, trim);
				set(rx, yy, z1 + 1, trim);
			}
		}
	}

	/** Steep pitched gable roof whose ridge runs along X over {@code [x0..x1] x [z0..z1]}. */
	public void pitchedRoofX(int x0, int y0, int z0, int x1, int z1, Block stairBlock, Block slabBlock, BlockState gableWall, BlockState trim) {
		if (!intersects(x0 - 1, z0 - 1, x1 + 1, z1 + 1)) return;
		int half = (z1 - z0) / 2;
		int midZ = (z0 + z1) / 2;
		boolean timberedGable = gableWall.equals(CALCITE) || gableWall.is(Blocks.SMOOTH_SANDSTONE);
		for (int k = 0; k <= half + 1; k++) {
			int lz = z0 + k - 1;
			int rz = z1 - k + 1;
			int yy = y0 + k;
			if (lz > rz) break;
			if (lz == rz) {
				fill(x0 - 1, yy, lz, x1 + 1, yy, rz, slab(slabBlock, false));
				break;
			}
			fill(x0 - 1, yy, lz, x1 + 1, yy, lz, stair(stairBlock, Direction.SOUTH, false));
			fill(x0 - 1, yy, rz, x1 + 1, yy, rz, stair(stairBlock, Direction.NORTH, false));
			if (lz + 1 <= rz - 1 && k > 0) {
				if (timberedGable) {
					boolean collar = (k % 3) == 0;
					for (int z = Math.max(lz + 1, minLz); z <= Math.min(rz - 1, maxLz); z++) {
						boolean stud = collar || z == midZ || Math.floorMod(Math.abs(z - midZ), 4) == 0;
						boolean atticWin = half >= 4 && z == midZ && (k == 2 || k == 3);
						BlockState fillBlock = atticWin ? Blocks.GLASS_PANE.defaultBlockState() : (stud ? DARK_OAK_LOG : gableWall);
						set(x0, yy, z, fillBlock);
						set(x1, yy, z, fillBlock);
					}
				} else {
					fill(x0, yy, lz + 1, x0, yy, rz - 1, gableWall);
					fill(x1, yy, lz + 1, x1, yy, rz - 1, gableWall);
				}
			}
			if (k > 0) {
				set(x0 - 1, yy, lz, trim);
				set(x0 - 1, yy, rz, trim);
				set(x1 + 1, yy, lz, trim);
				set(x1 + 1, yy, rz, trim);
			}
		}
	}

	/** Monumental ribbed dome with octagonal drum, architectural ribs, and tall golden finial spire. */
	public void ribbedDome(int cx, int y0, int cz, int radius, int height, BlockState shell, BlockState rib, BlockState finial) {
		if (!intersects(cx - radius - 1, cz - radius - 1, cx + radius + 1, cz + radius + 1)) return;
		for (int dy = 0; dy <= height; dy++) {
			double t = dy / (double) Math.max(1, height);
			double rAt = radius * Math.cos(t * Math.PI * 0.48);
			int rInt = (int) Math.ceil(rAt) + 1;
			for (int dx = -rInt; dx <= rInt; dx++) {
				for (int dz = -rInt; dz <= rInt; dz++) {
					double dist = Math.sqrt(dx * dx + dz * dz);
					if (dist <= rAt + 0.6 && dist >= Math.max(0.0, rAt - 1.3)) {
						boolean isRib = Math.abs(dx) <= 1 || Math.abs(dz) <= 1 || Math.abs(Math.abs(dx) - Math.abs(dz)) <= 1;
						set(cx + dx, y0 + dy, cz + dz, isRib ? rib : shell);
					}
				}
			}
		}
		for (int sy = 1; sy <= Math.max(4, radius / 3 + 2); sy++) {
			set(cx, y0 + height + sy, cz, finial);
		}
	}

	/** Towering octagonal/chamfered stone bastion watchtower with optional conical spire or crenellated crown. */
	public void bastionTower(int cx, int y0, int cz, int radius, int shaftH, int spireH, BlockState roofBlock, boolean greenBanners) {
		if (!intersects(cx - radius - 3, cz - radius - 3, cx + radius + 3, cz + radius + 3)) return;
		int maxR = radius + 2;
		int x0 = Math.max(-maxR, minLx - cx), x1 = Math.min(maxR, maxLx - cx);
		int z0 = Math.max(-maxR, minLz - cz), z1 = Math.min(maxR, maxLz - cz);
		for (int dx = x0; dx <= x1; dx++) {
			for (int dz = z0; dz <= z1; dz++) {
				int manhattan = Math.abs(dx) + Math.abs(dz);
				int cheb = Math.max(Math.abs(dx), Math.abs(dz));
				boolean inTower = cheb <= radius && manhattan <= radius + Math.max(2, radius * 2 / 3);
				if (!inTower) continue;
				boolean isWall = cheb == radius || manhattan == radius + Math.max(2, radius * 2 / 3);

				fillDeepslateBase(cx + dx, y0 - 6, cz + dz, cx + dx, y0 + Math.min(4, shaftH / 4), cz + dz);
				for (int dy = Math.min(5, shaftH / 4 + 1); dy <= shaftH; dy++) {
					if (isWall) {
						boolean arrowSlit = (dy >= 7 && dy <= shaftH - 6 && (dy % 6 == 0 || dy % 6 == 1) && (dx == 0 || dz == 0));
						set(cx + dx, y0 + dy, cz + dz, arrowSlit ? IRON_BARS : (hash(cx + dx, y0 + dy, cz + dz) < 70 ? STONE_BRICKS : TUFF_BRICKS));
					} else if (dy == 0 || dy == shaftH - 5 || dy == shaftH) {
						set(cx + dx, y0 + dy, cz + dz, dy == 0 ? STONE_BRICKS : DARK_OAK);
					} else {
						set(cx + dx, y0 + dy, cz + dz, AIR);
					}
				}
			}
		}

		// Corbeled overhanging parapet rim & lanterns
		int topY = y0 + shaftH;
		for (int dx = x0; dx <= x1; dx++) {
			for (int dz = z0; dz <= z1; dz++) {
				int r = radius + 1;
				int cheb = Math.max(Math.abs(dx), Math.abs(dz));
				int manhattan = Math.abs(dx) + Math.abs(dz);
				if (cheb <= r && manhattan <= r + Math.max(2, r * 2 / 3)) {
					boolean rim = cheb == r || manhattan == r + Math.max(2, r * 2 / 3);
					if (rim) {
						set(cx + dx, topY, cz + dz, TUFF_BRICKS);
						if (((dx + dz) & 1) == 0) {
							set(cx + dx, topY + 1, cz + dz, STONE_BRICKS);
							set(cx + dx, topY + 2, cz + dz, slab(Blocks.DEEPSLATE_TILE_SLAB, false));
						}
					}
				}
			}
		}

		// Warm hanging lanterns at cardinal parapet faces & arrow-slit sconces
		set(cx, topY - 3, cz + radius + 1, HANGING_LANTERN);
		set(cx, topY - 3, cz - radius - 1, HANGING_LANTERN);
		set(cx + radius + 1, topY - 3, cz, HANGING_LANTERN);
		set(cx - radius - 1, topY - 3, cz, HANGING_LANTERN);

		// Long emerald & gold heraldic banners draping down the tower walls (matching gatehouse & wall concept renders)
		if (greenBanners && shaftH >= 14) {
			wallBannerStrip(cx, topY - 4, cz + radius + 1, Math.min(10, shaftH - 7), Direction.SOUTH);
			wallBannerStrip(cx, topY - 4, cz - radius - 1, Math.min(10, shaftH - 7), Direction.NORTH);
		}

		// Steep dark conical slate spire (when spireH > 0) OR rooftop emerald-gold pennant flagpole
		if (spireH > 0) {
			for (int dy = 1; dy <= spireH; dy++) {
				double frac = 1.0 - (dy / (double) spireH);
				int sr = Math.max(0, (int) Math.round(radius * frac));
				for (int dx = -sr; dx <= sr; dx++) {
					for (int dz = -sr; dz <= sr; dz++) {
						if (Math.abs(dx) + Math.abs(dz) <= sr + Math.max(1, sr / 2)) {
							if (dx >= minLx - cx && dx <= maxLx - cx && dz >= minLz - cz && dz <= maxLz - cz) {
								if (sr == 0 || Math.max(Math.abs(dx), Math.abs(dz)) == sr || Math.abs(dx) + Math.abs(dz) >= sr + Math.max(1, sr / 2) - 1) {
									set(cx + dx, topY + dy, cz + dz, roofBlock);
								}
							}
						}
					}
				}
			}
			for (int fy = 1; fy <= 5; fy++) {
				set(cx, topY + spireH + fy, cz, fy >= 4 ? GOLD : IRON_BARS);
			}
			flagPennant(cx, topY + spireH + 5, cz);
		} else {
			for (int fy = 1; fy <= 7; fy++) {
				set(cx, topY + fy, cz, Blocks.OAK_FENCE.defaultBlockState());
			}
			flagPennant(cx, topY + 7, cz);
		}
	}

	/** Architectural emerald-teal & gold heraldic wall banner hanging downward from {@code yTop}. */
	public void wallBannerStrip(int lx, int yTop, int lz, int len, Direction facing) {
		if (!intersects(lx - 1, lz - 1, lx + 1, lz + 1)) return;
		for (int dy = 0; dy < len; dy++) {
			boolean centerLine = dy >= 2 && dy <= len - 3;
			set(lx, yTop - dy, lz, (dy == len - 1 || (centerLine && dy % 3 == 0)) ? BANNER_GOLD : EMERALD_GREEN);
			if (facing == Direction.NORTH || facing == Direction.SOUTH) {
				set(lx - 1, yTop - dy, lz, EMERALD_TEAL);
				set(lx + 1, yTop - dy, lz, EMERALD_TEAL);
			} else {
				set(lx, yTop - dy, lz - 1, EMERALD_TEAL);
				set(lx, yTop - dy, lz + 1, EMERALD_TEAL);
			}
		}
		// Crown with real Minecraft wall banner at top center
		set(lx, yTop, lz, Blocks.GREEN_WALL_BANNER.defaultBlockState().setValue(WallBannerBlock.FACING, facing));
	}

	/** Emerald-and-gold fluttering pennant beside a spire/flagpole at {@code (cx, fy, cz)}. */
	public void flagPennant(int cx, int fy, int cz) {
		for (int fx = 1; fx <= 5; fx++) {
			set(cx + fx, fy, cz, fx <= 2 ? GOLD : EMERALD_GREEN);
			set(cx + fx, fy - 1, cz, fx == 3 ? BANNER_GOLD : EMERALD_TEAL);
			if (fx <= 3) set(cx + fx, fy - 2, cz, EMERALD_GREEN);
		}
	}

	/** Giant suspended iron-chain & dark-oak lantern chandelier (matches Great Hall interior concept). */
	public void chandelier(int cx, int ceilingY, int dropLen, int cz) {
		if (!intersects(cx - 3, cz - 3, cx + 3, cz + 3)) return;
		for (int dy = 0; dy < dropLen; dy++) {
			set(cx, ceilingY - dy, cz, CHAIN);
		}
		int rimY = ceilingY - dropLen;
		fill(cx - 2, rimY, cz, cx + 2, rimY, cz, DARK_OAK);
		fill(cx, rimY, cz - 2, cx, rimY, cz + 2, DARK_OAK);
		set(cx, rimY, cz, GOLD);
		for (int[] off : new int[][]{{-2, -1}, {2, -1}, {-2, 1}, {2, 1}, {-1, -2}, {1, -2}, {-1, 2}, {1, 2}}) {
			set(cx + off[0], rimY - 1, cz + off[1], HANGING_LANTERN);
		}
	}

	/** Ornate medieval street lamp post. */
	public void streetLamp(int lx, int y, int lz) {
		if (!intersects(lx - 1, lz - 1, lx + 1, lz + 1)) return;
		set(lx, y, lz, COBBLED_DEEPSLATE);
		set(lx, y + 1, lz, Blocks.STONE_BRICK_WALL.defaultBlockState());
		set(lx, y + 2, lz, Blocks.STONE_BRICK_WALL.defaultBlockState());
		set(lx, y + 3, lz, Blocks.DARK_OAK_FENCE.defaultBlockState());
		set(lx, y + 4, lz, LANTERN);
	}

	/** Loot chest placed deterministically when this tile/chunk builds. */
	public void chest(int lx, int ly, int lz, Direction facing, ResourceKey<LootTable> loot, long salt) {
		if (!inClip(lx, ly, lz)) return;
		BlockPos pos = new BlockPos(wx(lx), wy(ly), wz(lz));
		if (level.getBlockEntity(pos) instanceof ChestBlockEntity) return;
		set(lx, ly, lz, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
		if (level.getBlockEntity(pos) instanceof ChestBlockEntity c) {
			c.setLootTable(loot, pos.asLong() ^ salt);
		}
	}

	/** Lectern holding a multi-page city chronicle book. */
	public void lecternBook(int lx, int ly, int lz, Direction facing, String title, String author, List<String> pageTexts) {
		if (!inClip(lx, ly, lz)) return;
		BlockPos pos = new BlockPos(wx(lx), wy(ly), wz(lz));
		if (level.getBlockEntity(pos) instanceof LecternBlockEntity) return;
		set(lx, ly, lz, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, facing).setValue(LecternBlock.HAS_BOOK, true));
		if (level.getBlockEntity(pos) instanceof LecternBlockEntity lectern) {
			List<Filterable<Component>> pages = new ArrayList<>(pageTexts.size());
			for (String s : pageTexts) pages.add(Filterable.passThrough(Component.literal(s)));
			ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
			stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(title), author, 0, pages, true));
			lectern.setBook(stack);
		}
	}

	/** Places a bed spanning from {@code (fx, ly, fz)} toward {@code headDir}. */
	public void bed(int fx, int ly, int fz, DyeColor color, Direction headDir) {
		Block bed = dev.wildercord.compat.McBlocksAndItems.dyedBlock("bed", color);
		BlockState state = (bed == null ? Blocks.RED_BED : bed).defaultBlockState();
		set(fx, ly, fz, state.setValue(BedBlock.FACING, headDir).setValue(BedBlock.PART, BedPart.FOOT));
		set(fx + headDir.getStepX(), ly, fz + headDir.getStepZ(),
			state.setValue(BedBlock.FACING, headDir).setValue(BedBlock.PART, BedPart.HEAD));
	}

	/** Places a two-high wooden door facing {@code facing}. */
	public void door(int lx, int ly, int lz, Block doorBlock, Direction facing) {
		set(lx, ly, lz, doorBlock.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		set(lx, ly + 1, lz, doorBlock.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
	}

	/** Custom stylized cherry blossom or oak tree for Acropolis gardens and city courtyards. */
	public void tree(int lx, int y, int lz, boolean cherry) {
		if (!intersects(lx - 3, lz - 3, lx + 3, lz + 3)) return;
		BlockState log = cherry ? Blocks.CHERRY_LOG.defaultBlockState() : Blocks.OAK_LOG.defaultBlockState();
		BlockState leaves = (cherry ? Blocks.CHERRY_LEAVES : Blocks.OAK_LEAVES)
			.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		for (int dy = 1; dy <= 4; dy++) set(lx, y + dy, lz, log);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = 3; dy <= 5; dy++) {
					int r = Math.abs(dx) + Math.abs(dz) + Math.abs(dy - 4);
					if (r <= 3) set(lx + dx, y + dy, lz + dz, leaves);
				}
			}
		}
		if (cherry) {
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					if ((dx != 0 || dz != 0) && hash(lx + dx, y, lz + dz) < 28 && get(lx + dx, y + 1, lz + dz).isAir()) {
						set(lx + dx, y + 1, lz + dz, Blocks.PINK_PETALS.defaultBlockState());
					}
				}
			}
		}
	}

	/** Places an unbreakable Oathhold Adventurers' Guild Request Board at local {@code (lx, ly, lz)}. */
	public void guildBoard(int lx, int ly, int lz, Direction facing) {
		if (!inClip(lx, ly, lz)) return;
		set(lx, ly, lz, OathholdContent.GUILD_REQUEST_BOARD.defaultBlockState().setValue(GuildRequestBoardBlock.FACING, facing));
	}

	/**
	 * Finds a safe, non-fluid 2-block-high air standing spot near {@code (lx, ly, lz)} inside {@code clip}
	 * so spawned guards and citizens never suffocate inside walls, counters, or canals.
	 */
	public BlockPos findSafeStandPos(int lx, int ly, int lz) {
		if (isSafeStandColumn(lx, ly, lz)) {
			return new BlockPos(wx(lx), wy(ly), wz(lz));
		}
		int[] dyOrder = {0, 1, -1, 2, -2};
		for (int r = 1; r <= 2; r++) {
			for (int dy : dyOrder) {
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
						int cx = lx + dx, cy = ly + dy, cz = lz + dz;
						if (isSafeStandColumn(cx, cy, cz)) {
							return new BlockPos(wx(cx), wy(cy), wz(cz));
						}
					}
				}
			}
		}
		return null;
	}

	private boolean isSafeStandColumn(int lx, int ly, int lz) {
		if (!inClip(lx, ly, lz) || !inClip(lx, ly - 1, lz) || !inClip(lx, ly + 1, lz)) return false;
		BlockPos feet = new BlockPos(wx(lx), wy(ly), wz(lz));
		BlockPos head = feet.above();
		BlockPos floor = feet.below();
		BlockState feetState = level.getBlockState(feet);
		BlockState headState = level.getBlockState(head);
		BlockState floorState = level.getBlockState(floor);
		return (feetState.isAir() || feetState.canBeReplaced())
			&& (headState.isAir() || headState.canBeReplaced())
			&& !floorState.isAir()
			&& level.getFluidState(feet).isEmpty()
			&& level.getFluidState(floor).isEmpty();
	}

	/** Spawns one named {@link OathholdCitizen} inside the clip if safe standing clearance exists. */
	public OathholdCitizen spawnCitizen(int lx, int ly, int lz, OathholdCitizenRules.Archetype archetype,
			int tetherRadius, float yaw) {
		if (!inClip(lx, ly, lz)) return null;
		BlockPos pos = findSafeStandPos(lx, ly, lz);
		if (pos == null) return null;
		OathholdCitizen citizen = OathholdContent.CITIZEN.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (citizen == null) return null;
		citizen.configure(archetype, lx, ly, lz, pos, tetherRadius);
		citizen.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
		citizen.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
		level.addFreshEntityWithPassengers(citizen);
		return citizen;
	}

	/** Spawns one structure-resident entity inside the chunk if its block falls in {@code clip} and has clearance. */
	public <T extends Mob> T spawnMob(int lx, int ly, int lz, EntityType<T> type, float yaw, boolean persistent) {
		if (!inClip(lx, ly, lz)) return null;
		BlockPos pos = findSafeStandPos(lx, ly, lz);
		if (pos == null) {
			pos = new BlockPos(wx(lx), wy(ly), wz(lz));
		}
		T mob = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (mob == null) return null;
		if (persistent) mob.setPersistenceRequired();
		mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
		level.addFreshEntityWithPassengers(mob);
		return mob;
	}
}
