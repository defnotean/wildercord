package dev.wildercord.world.dungeons;

import dev.wildercord.content.RuneSealBlock;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/** An octagonal mountain observatory: copper instruments, switchback stairs and a lightning crown. */
public class StormSpirePiece extends DungeonPiece {
	private final boolean modern;
	private static final BlockState STONE = Blocks.POLISHED_ANDESITE.defaultBlockState();
	private static final BlockState BRICK = Blocks.STONE_BRICKS.defaultBlockState();
	private static final BlockState COPPER = Blocks.CUT_COPPER.waxed().oxidized().defaultBlockState();
	private static final BlockState DARK = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
	private static final BlockState GLASS = Blocks.STAINED_GLASS.cyan().defaultBlockState();
	private static final BlockState LIGHT = Blocks.SEA_LANTERN.defaultBlockState();

	public StormSpirePiece(int x, int y, int z, Direction facing) {
		super(DungeonWorldgen.STORM_SPIRE_PIECE, x, y, z, 23, 43, 23, facing);
		modern=true;
	}

	public StormSpirePiece(CompoundTag tag) {
		super(DungeonWorldgen.STORM_SPIRE_PIECE, tag);
		modern=tag.getIntOr("StormLayout",1)>=2;
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext context) {
		Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
		ChunkPos chunk = context.chunkPos();
		StormSpirePiece piece = new StormSpirePiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), facing);
		BlockPos entrance = piece.getWorldPos(11, 0, 0);
		int ground = context.chunkGenerator().getFirstOccupiedHeight(entrance.getX(), entrance.getZ(), Heightmap.Types.WORLD_SURFACE_WG,
			context.heightAccessor(), context.randomState());
		if (ground - 1 + 43 > context.heightAccessor().getMaxY()) return Optional.empty();
		piece.move(0, ground - 1, 0);
		return Optional.of(new Structure.GenerationStub(new BlockPos(entrance.getX(), ground, entrance.getZ()), builder -> builder.addPiece(piece)));
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunkPos, BlockPos reference) {
		DungeonWards.remember(level, this);
		if (!modern) {legacy(level,bb);return;}
		// Clear only the authored tower volume. All decoration stays within the piece's bounds.
		for (int x=0;x<=22;x++) for (int z=0;z<=22;z++) {
			if (inside(x,z,11,19)) set(level,bb,noise(x,z)<20?Blocks.CRACKED_STONE_BRICKS.defaultBlockState():DARK,x,0,z);
			if (!inside(x,z,10,17)) continue;
			boolean edge=boundary(x,z,10,17);
			for (int y=1;y<=27;y++) {
				BlockState wall=noise(x,y,z)<14?Blocks.CRACKED_STONE_BRICKS.defaultBlockState():BRICK;
				if (y%7==0||y==27) wall=COPPER;
				set(level,bb,edge?wall:AIR,x,y,z);
			}
			for (int y:new int[]{7,14,21,27}) set(level,bb,edge?COPPER:STONE,x,y,z);
		}
		// Paired buttresses read as structural ribs, tapering towards the crown.
		for (int[] at:new int[][]{{1,5},{1,17},{21,5},{21,17},{5,1},{17,1},{5,21},{17,21}}) {
			for (int y=1;y<=27;y++) set(level,bb,y%7==0||y>=25?COPPER:DARK,at[0],y,at[1]);
		}
		for (int base:new int[]{1,8,15,22}) {
			for (int a:new int[]{7,8,9,13,14,15}) for (int y=base+1;y<=base+3;y++) {
				set(level,bb,GLASS,a,y,1);set(level,bb,GLASS,a,y,21);
				set(level,bb,GLASS,1,y,a);set(level,bb,GLASS,21,y,a);
			}
			for (int x:new int[]{3,19}) for (int z:new int[]{5,17}) {
				set(level,bb,DARK,x,base,z);set(level,bb,LIGHT,x,base+1,z);
			}
		}
		// Deep entrance arch and the first landing; no jump or scaffold is required to climb.
		fill(level,bb,9,1,1,13,4,1,AIR);
		for(int x:new int[]{8,14}) fill(level,bb,x,1,0,x,4,1,DARK);
		fill(level,bb,8,5,0,14,5,1,COPPER);set(level,bb,LIGHT,11,5,0);
		for (int flight=0;flight<3;flight++) {
			int base=flight*7,x=flight==1?7:4;
			for (int step=0;step<7;step++) {
				int z=flight==1?10-step:4+step,y=base+1+step;
				fill(level,bb,x,y+1,z,x+1,y+3,z,AIR);
				fill(level,bb,x,base+1,z,x+1,y,z,DARK);
				BlockState stair=Blocks.POLISHED_ANDESITE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,flight==1?Direction.SOUTH:Direction.NORTH);
				set(level,bb,stair,x,y,z);set(level,bb,stair,x+1,y,z);
			}
			int landing=flight==1?3:11;
			fill(level,bb,4,base+7,landing,8,base+7,landing,STONE);
			fill(level,bb,4,base+8,landing,8,base+10,landing,AIR);
		}
		// A glass-insulated conductor binds the three instrument rooms together.
		for(int y=1;y<=26;y++) set(level,bb,y%7==0?LIGHT:(y%2==0?COPPER:GLASS),11,y,11);
		for(int floor:new int[]{0,7,14}) {
			for(int x=10;x<=12;x++) for(int z=10;z<=12;z++) if(x!=11||z!=11) set(level,bb,COPPER,x,floor,z);
		}
		// Ground: archive benches. Middle: induction coils. Upper: a wind instrument gallery.
		fill(level,bb,15,1,7,18,1,7,Blocks.CHISELED_BOOKSHELF.defaultBlockState());
		fill(level,bb,15,1,8,18,1,8,Blocks.DARK_OAK_SLAB.defaultBlockState());
		for(int x:new int[]{15,17}) {fill(level,bb,x,8,7,x,10,7,COPPER);set(level,bb,LIGHT,x,11,7);}
		for(int z=5;z<=9;z++) {set(level,bb,COPPER,16,15,z);set(level,bb,Blocks.IRON_BARS.defaultBlockState(),16,16,z);}
		// An open instrument crown has eight ribs and nested copper rings, capped by a lightning mast.
		for(int y:new int[]{28,34}) for(int x=3;x<=19;x++) for(int z=3;z<=19;z++)
			if(boundary(x,z,8,13)) set(level,bb,COPPER,x,y,z);
		for(int[] at:new int[][]{{3,8},{3,14},{19,8},{19,14},{8,3},{14,3},{8,19},{14,19}}) {
			fill(level,bb,at[0],28,at[1],at[0],34,at[1],DARK);
			set(level,bb,LIGHT,at[0],35,at[1]);
		}
		for(int y=28;y<=40;y++) set(level,bb,y==36?LIGHT:COPPER,11,y,11);
		for(int x=5;x<=17;x++) {set(level,bb,COPPER,x,37,11);set(level,bb,COPPER,11,37,x);}
		for(int[] at:new int[][]{{5,11},{17,11},{11,5},{11,17}}) {
			set(level,bb,LIGHT,at[0],38,at[1]);set(level,bb,Blocks.LIGHTNING_ROD.weathering().unaffected().defaultBlockState(),at[0],39,at[1]);
		}
		set(level,bb,Blocks.LIGHTNING_ROD.weathering().unaffected().defaultBlockState(),11,41,11);
		// Each floor has a guarded side cache. The upper chamber is warded and opened with magic.
		chest(level, bb, 18, 8, 10, DungeonWorldgen.STORM_HALL, Direction.WEST, 201);
		chest(level, bb, 4, 15, 17, DungeonWorldgen.STORM_HALL, Direction.EAST, 202);
		fill(level, bb, 2, 22, 15, 20, 27, 15, BRICK);
		sealDoorAcross(level, bb, 15, 9, 13, 22, 25, RuneSealBlock.Element.STORM, RuneSealBlock.Element.WIND);
		chest(level, bb, 6, 22, 19, DungeonWorldgen.STORM_VAULT, Direction.EAST, 203);
		chest(level, bb, 17, 22, 19, DungeonWorldgen.STORM_VAULT, Direction.WEST, 204);
		altar(level, bb, dev.wildercord.content.dungeons.DungeonAltarBlock.Kind.STORM, 11, 22, 18);
		for (int x : new int[] {6, 11, 16}) set(level, bb, Blocks.LIGHTNING_ROD.weathering().unaffected().defaultBlockState(), x, 22, 17);
		guard(level, bb, EntityTypes.STRAY, 6, 1, 16, List.of(Runes.BOLT, Runes.SHOCK), false);
		guard(level, bb, EntityTypes.SKELETON, 16, 8, 5, List.of(Runes.ARC, Runes.WINDCUT), false);
		guard(level, bb, EntityTypes.STRAY, 6, 15, 6, List.of(Runes.ORB, Runes.THUNDERCLAP), true);
	}

	/** Old saved pieces finish in their original 29-block box, including previously ungenerated chunks. */
	private void legacy(WorldGenLevel level,BoundingBox bb) {
		room(level, bb, 1, 0, 1, 21, 27, 21, BRICK);
		fill(level, bb, 9, 1, 1, 13, 4, 1, AIR);
		for (int y : new int[] {7, 14, 21}) {
			fill(level, bb, 2, y, 2, 20, y, 20, STONE);
			fill(level, bb, 10, y, 10, 12, y, 12, AIR);
			for (int x : new int[] {4, 18}) {
				for (int z : new int[] {4, 18}) {
					set(level, bb, LIGHT, x, y, z);
				}
			}
		}
		// The central scaffold shaft can be climbed or descended from every floor.
		for (int y = 1; y <= 23; y++) {
			set(level, bb, Blocks.SCAFFOLDING.defaultBlockState(), 11, y, 11);
		}
		for (int y : new int[] {1, 8, 15, 22}) {
			for (int x : new int[] {3, 19}) {
				for (int z : new int[] {3, 19}) {
					fill(level, bb, x, y, z, x, y + 3, z, STONE);
					set(level, bb, LIGHT, x, y + 3, z);
				}
			}
		}
		// Each floor has a guarded side cache. The upper chamber is warded and opened with magic.
		chest(level, bb, 18, 8, 10, DungeonWorldgen.STORM_HALL, Direction.WEST, 201);
		chest(level, bb, 4, 15, 17, DungeonWorldgen.STORM_HALL, Direction.EAST, 202);
		fill(level, bb, 2, 22, 15, 20, 25, 15, BRICK);
		sealDoorAcross(level, bb, 15, 9, 13, 22, 25, RuneSealBlock.Element.STORM, RuneSealBlock.Element.WIND);
		chest(level, bb, 6, 22, 19, DungeonWorldgen.STORM_VAULT, Direction.EAST, 203);
		chest(level, bb, 17, 22, 19, DungeonWorldgen.STORM_VAULT, Direction.WEST, 204);
		altar(level, bb, dev.wildercord.content.dungeons.DungeonAltarBlock.Kind.STORM, 11, 22, 18);
		for (int x : new int[] {6, 11, 16}) set(level, bb, Blocks.LIGHTNING_ROD.weathering().unaffected().defaultBlockState(), x, 22, 17);
		for (int x : new int[] {5, 11, 17}) {
			set(level, bb, Blocks.LIGHTNING_ROD.weathering().unaffected().defaultBlockState(), x, 28, 11);
		}
		guard(level, bb, EntityTypes.STRAY, 6, 1, 16, List.of(Runes.BOLT, Runes.SHOCK), false);
		guard(level, bb, EntityTypes.SKELETON, 16, 8, 5, List.of(Runes.ARC, Runes.WINDCUT), false);
		guard(level, bb, EntityTypes.STRAY, 6, 15, 6, List.of(Runes.ORB, Runes.THUNDERCLAP), true);
	}

	@Override protected void addAdditionalSaveData(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext context,CompoundTag tag) {
		super.addAdditionalSaveData(context,tag);tag.putInt("StormLayout",modern?2:1);
	}

	private static boolean inside(int x,int z,int radius,int diagonal) {
		return Math.abs(x-11)<=radius&&Math.abs(z-11)<=radius&&Math.abs(x-11)+Math.abs(z-11)<=diagonal;
	}
	private static boolean boundary(int x,int z,int radius,int diagonal) {
		return inside(x,z,radius,diagonal)&&(!inside(x-1,z,radius,diagonal)||!inside(x+1,z,radius,diagonal)||!inside(x,z-1,radius,diagonal)||!inside(x,z+1,radius,diagonal));
	}

	@Override
	public List<BoundingBox> wardedBoxes() {
		return List.of(worldBox(2, 21, 15, 20, 27, 21));
	}
}
