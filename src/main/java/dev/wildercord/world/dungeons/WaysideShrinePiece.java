package dev.wildercord.world.dungeons;

import dev.wildercord.config.Config;
import dev.wildercord.monster.MonsterContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import java.util.List;
import java.util.Optional;

/**
 * A wayside shrine: a little roofed shrine on a worn stone footing by the roadside, the kind travellers leave offerings at. Under
 * its gabled spruce roof a lantern hangs over a stone with its candles long burnt out, and two iron-bound coffers sit before it.
 * One of them is a Coffer Mimic. In one shrine in three both coffers are honest, and the mimic sits out on its own by the path
 * instead, where nobody would think to leave a chest. Laid out in its own coordinates (x across, y up, z inward from the path).
 *
 * <pre>
 *   z 0       the path's end, coarse dirt
 *   z 1-9     the footing, worn stone, ragged at its edges
 *   z 5-9     the shrine: posts at x 3 and 7, its roof from x 2 to 8, the stone at (5, 8)
 *   coffers   (4, 7) and (6, 7); the lone one by the path at (8, 2)
 * </pre>
 */
public final class WaysideShrinePiece extends DungeonPiece {
	public static final int WIDTH=11,HEIGHT=8,DEPTH=11;
	private static final BlockState POST=Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState();
	private static final BlockState PLANKS=Blocks.SPRUCE_PLANKS.defaultBlockState();
	private static final BlockState CANDLE=Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT,false);

	public WaysideShrinePiece(int x,int y,int z,Direction facing){super(DungeonWorldgen.WAYSIDE_SHRINE_PIECE,x,y,z,WIDTH,HEIGHT,DEPTH,facing);}
	public WaysideShrinePiece(CompoundTag tag){super(DungeonWorldgen.WAYSIDE_SHRINE_PIECE,tag);}

	/** Needs dry, near-level ground under the footing. */
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c){
		if(Config.get().monsters().spawnRate()<=0)return Optional.empty();
		var piece=new WaysideShrinePiece(c.chunkPos().getMinBlockX(),0,c.chunkPos().getMinBlockZ(),Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		var gen=c.chunkGenerator();int sea=gen.getSeaLevel();
		int high=Integer.MIN_VALUE,low=Integer.MAX_VALUE;
		for(int[] d:new int[][]{{5,5},{1,1},{9,1},{1,9},{9,9}}){
			var p=piece.localPosition(d[0],0,d[1]);
			int surface=gen.getFirstOccupiedHeight(p.getX(),p.getZ(),Heightmap.Types.WORLD_SURFACE_WG,c.heightAccessor(),c.randomState());
			int floor=gen.getFirstOccupiedHeight(p.getX(),p.getZ(),Heightmap.Types.OCEAN_FLOOR_WG,c.heightAccessor(),c.randomState());
			if(surface<=sea+1 || surface-floor>1)return Optional.empty();
			high=Math.max(high,floor);low=Math.min(low,floor);
		}
		if(high-low>2)return Optional.empty();
		int ground=(high+low+1)/2;
		piece.move(0,ground-1,0);
		var centre=piece.localPosition(5,1,5);
		return Optional.of(new Structure.GenerationStub(centre,b->b.addPiece(piece)));
	}

	@Override public void postProcess(WorldGenLevel level,StructureManager structures,ChunkGenerator generator,RandomSource random,
			BoundingBox bb,ChunkPos chunk,BlockPos reference){
		footing(level,bb);
		shrine(level,bb);
		// Which coffer is the mimic: the left, the right, or (one shrine in three) neither, and it waits out by the path.
		int v=variant();
		if(v!=0)chest(level,bb,4,1,7,DungeonWorldgen.WAYSIDE_SHRINE,Direction.SOUTH,1);
		if(v!=1)chest(level,bb,6,1,7,DungeonWorldgen.WAYSIDE_SHRINE,Direction.SOUTH,2);
		if(v==0)mimic(level,bb,4,7,5,0);
		else if(v==1)mimic(level,bb,6,7,5,0);
		else mimic(level,bb,8,2,5,2);
	}

	private void footing(WorldGenLevel level,BoundingBox bb){
		for(int x=0;x<WIDTH;x++)for(int z=0;z<DEPTH;z++){
			int edge=Math.min(Math.min(x-1,9-x),Math.min(z-1,9-z));
			boolean stone=edge>=1 || (edge==0 && noise(x,z)<55);
			boolean path=z==0 && x>=4 && x<=6;
			if(!stone && !path)continue;
			fill(level,bb,x,-3,z,x,-1,z,Blocks.DIRT.defaultBlockState());
			BlockState top=path?Blocks.COARSE_DIRT.defaultBlockState()
				:noise(x,z)<30?Blocks.MOSSY_COBBLESTONE.defaultBlockState()
				:noise(x,z)<45?Blocks.COBBLESTONE.defaultBlockState():Blocks.STONE_BRICKS.defaultBlockState();
			set(level,bb,top,x,0,z);
			fill(level,bb,x,1,z,x,HEIGHT-1,z,AIR);
		}
		// A worn track up from the path to the shrine.
		for(int z=1;z<=4;z++)set(level,bb,Blocks.GRAVEL.defaultBlockState(),5,0,z);
		// Candles long burnt out at the footing's front corners.
		set(level,bb,CANDLE.setValue(CandleBlock.CANDLES,2),2,1,2);
		set(level,bb,CANDLE,8,1,4);
	}

	private void shrine(WorldGenLevel level,BoundingBox bb){
		for(int x:new int[]{3,7})for(int z:new int[]{6,8})fill(level,bb,x,1,z,x,3,z,POST);
		// The gabled roof, its ridge running front to back.
		for(int z=5;z<=9;z++){
			set(level,bb,stair(Direction.EAST),2,4,z);
			set(level,bb,stair(Direction.WEST),8,4,z);
			fill(level,bb,3,4,z,7,4,z,PLANKS);
			set(level,bb,stair(Direction.EAST),3,5,z);
			set(level,bb,stair(Direction.WEST),7,5,z);
			fill(level,bb,4,5,z,6,5,z,PLANKS);
			set(level,bb,stair(Direction.EAST),4,6,z);
			set(level,bb,Blocks.SPRUCE_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE,SlabType.BOTTOM),5,6,z);
			set(level,bb,stair(Direction.WEST),6,6,z);
		}
		// The back wall, low, and the stone the offerings were left on.
		fill(level,bb,4,1,9,6,2,9,Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
		set(level,bb,Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),5,1,8);
		set(level,bb,CANDLE.setValue(CandleBlock.CANDLES,3),5,2,8);
		set(level,bb,Blocks.POTTED_POPPY.defaultBlockState(),4,3,9);
		set(level,bb,Blocks.POTTED_DANDELION.defaultBlockState(),6,3,9);
		set(level,bb,Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true),5,3,7);
	}

	private static BlockState stair(Direction facing){
		return Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,facing);
	}

	/** The mimic, shut and square, facing the spot ({@code tx}, {@code tz}) it watches. Spawned once, by the chunk its feet are in. */
	private void mimic(WorldGenLevel level,BoundingBox bb,int x,int z,int tx,int tz){
		var foot=getWorldPos(x,1,z);if(!bb.isInside(foot))return;
		var toward=getWorldPos(tx,1,tz);
		float yaw=(float)Math.toDegrees(Math.atan2(-(toward.getX()-foot.getX()),toward.getZ()-foot.getZ()));
		var mimic=MonsterContent.COFFER_MIMIC.create(level.getLevel(),EntitySpawnReason.STRUCTURE);if(mimic==null)return;
		mimic.setPersistenceRequired();
		mimic.snapTo(foot.getX()+.5,foot.getY(),foot.getZ()+.5,yaw,0);
		mimic.finalizeSpawn(level,level.getCurrentDifficultyAt(foot),EntitySpawnReason.STRUCTURE,null);
		mimic.settleHere(yaw);
		level.addFreshEntityWithPassengers(mimic);
	}

	@Override public List<BoundingBox> wardedBoxes(){return List.of();}
}
