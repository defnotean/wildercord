package dev.wildercord.world.sites.masters;

import dev.wildercord.content.RuneSealBlock;
import dev.wildercord.world.dungeons.DungeonPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * A Master hall or shrine: one piece serving two related breathing schools, the school it keeps a pure function of where it
 * stands (saved, so a reload keeps it). Local y=0 is the ground; every hall has practice dummies, a lectern telling how to
 * challenge its school's Master, and a reward chest of that school's pages, scrolls and runes behind a small trial.
 */
public abstract class MasterSitePiece extends DungeonPiece {
	/** Which of the two schools ({@link #schools()}) this hall keeps. */
	protected final boolean alt;

	protected MasterSitePiece(StructurePieceType type, int x, int y, int z, int w, int h, int d, Direction facing, boolean alt) {
		super(type, x, y, z, w, h, d, facing);
		this.alt = alt;
	}

	protected MasterSitePiece(StructurePieceType type, CompoundTag tag) {
		super(type, tag);
		alt = tag.getBooleanOr("MasterAlt", false);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
		tag.putBoolean("MasterAlt", alt);
	}

	/** The site id, e.g. {@code master_forge_dojo}. */
	public abstract String site();

	/** The two schools (breathing method ids) this site may keep. */
	public abstract List<String> schools();

	public boolean alt() { return alt; }

	public String school() { return schools().get(alt ? 1 : 0); }

	/** The reward chest's table: the kept school's own. */
	public net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> rewardLoot() {
		return MasterSites.reward(site(), school());
	}

	/** Which school a chunk's hall keeps: stable, and both appear across a world. */
	static boolean altFor(ChunkPos chunk) {
		return Math.floorMod(chunk.x() * 7 + chunk.z() * 13 + (chunk.x() >> 2), 2) == 1;
	}

	// ------------------------------------------------------------------ locating

	@FunctionalInterface
	interface Factory { MasterSitePiece make(int x, int y, int z, Direction facing, boolean alt); }

	/**
	 * A hall on dry, fairly level ground: the centre and four corners within {@code slope} blocks, none under water.
	 * The piece's y=0 lands on the centre's top block.
	 */
	static Optional<Structure.GenerationStub> onGround(Structure.GenerationContext c, Factory factory, int slope) {
		ChunkPos chunk = c.chunkPos();
		var piece = factory.make(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), Direction.Plane.HORIZONTAL.getRandomDirection(c.random()), altFor(chunk));
		var box = piece.getBoundingBox();
		int w = box.getXSpan(), d = box.getZSpan();
		var centre = new BlockPos(box.minX() + w / 2, 0, box.minZ() + d / 2);
		int surface = height(c, centre, Heightmap.Types.WORLD_SURFACE_WG);
		if (surface <= c.chunkGenerator().getSeaLevel() || surface != height(c, centre, Heightmap.Types.OCEAN_FLOOR_WG)) return Optional.empty();
		for (int[] k : new int[][]{{box.minX() + 1, box.minZ() + 1}, {box.maxX() - 1, box.minZ() + 1}, {box.minX() + 1, box.maxZ() - 1}, {box.maxX() - 1, box.maxZ() - 1}}) {
			var at = new BlockPos(k[0], 0, k[1]);
			int h = height(c, at, Heightmap.Types.WORLD_SURFACE_WG);
			if (Math.abs(h - surface) > slope || h != height(c, at, Heightmap.Types.OCEAN_FLOOR_WG)) return Optional.empty();
		}
		if (surface + box.getYSpan() > c.heightAccessor().getMaxY()) return Optional.empty();
		piece.move(0, surface, 0);
		return Optional.of(new Structure.GenerationStub(centre.atY(surface), b -> b.addPiece(piece)));
	}

	static int height(Structure.GenerationContext c, BlockPos p, Heightmap.Types h) {
		return c.chunkGenerator().getFirstOccupiedHeight(p.getX(), p.getZ(), h, c.heightAccessor(), c.randomState());
	}

	// ------------------------------------------------------------------ building

	/** A floor at y=0 over x0..x1, z0..z1, footed down to solid ground, with air cleared to {@code top}. */
	protected void pad(WorldGenLevel level, BoundingBox bb, int x0, int z0, int x1, int z1, int top, BlockState floor, BlockState footing) {
		for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
			fillColumnDown(level, footing, x, -1, z, bb);
			set(level, bb, floor, x, 0, z);
			fill(level, bb, x, 1, z, x, top, z, AIR);
		}
	}

	/** A straw dummy (post, hay body, carved head) facing the entrance. */
	protected void dummy(WorldGenLevel level, BoundingBox bb, int x, int y, int z, BlockState post) {
		set(level, bb, post, x, y, z);
		set(level, bb, Blocks.HAY_BLOCK.defaultBlockState(), x, y + 1, z);
		set(level, bb, Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, Direction.SOUTH), x, y + 2, z);
	}

	/** An archery target on a post. */
	protected void target(WorldGenLevel level, BoundingBox bb, int x, int y, int z, BlockState post) {
		set(level, bb, post, x, y, z);
		set(level, bb, Blocks.TARGET.defaultBlockState(), x, y + 1, z);
	}

	/** The hall's lectern, facing the entrance, holding its school's notes. */
	protected void lectern(WorldGenLevel level, BoundingBox bb, int x, int y, int z) {
		set(level, bb, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.SOUTH).setValue(LecternBlock.HAS_BOOK, true), x, y, z);
		BlockPos at = getWorldPos(x, y, z);
		if (bb.isInside(at) && level.getBlockEntity(at) instanceof LecternBlockEntity lectern) lectern.setBook(notes(site(), school()));
	}

	/** The school's notes: its story, how to face its Master, and this hall's trial. */
	public static ItemStack notes(String site, String school) {
		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		String name = name(school);
		book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Notes of the " + name + " School"), "The " + name + " School", 0,
			List.of(Filterable.passThrough(Component.translatable("book.wildercord.master_site." + school)),
				Filterable.passThrough(Component.translatable("book.wildercord.master_site.challenge", name, "/master challenge " + school)),
				Filterable.passThrough(Component.translatable("book.wildercord." + site + ".trial"))), true));
		return book;
	}

	static String name(String school) {
		return school.substring(0, 1).toUpperCase(Locale.ROOT) + school.substring(1);
	}

	/** The Rune Seal element a school's door answers to. */
	static RuneSealBlock.Element element(String school) {
		return switch (school) {
			case "ember" -> RuneSealBlock.Element.FIRE;
			case "crimson" -> RuneSealBlock.Element.BLOOD;
			case "rime", "tide" -> RuneSealBlock.Element.FROST;
			case "thunder" -> RuneSealBlock.Element.STORM;
			case "gale", "echo" -> RuneSealBlock.Element.WIND;
			case "stone", "iron", "dune" -> RuneSealBlock.Element.EARTH;
			case "verdant", "venom" -> RuneSealBlock.Element.LIFE;
			case "hollow" -> RuneSealBlock.Element.VOID;
			case "hourglass" -> RuneSealBlock.Element.TIME;
			default -> RuneSealBlock.Element.ARCANE;
		};
	}

	protected static BlockState s(net.minecraft.world.level.block.Block block) { return block.defaultBlockState(); }
}
