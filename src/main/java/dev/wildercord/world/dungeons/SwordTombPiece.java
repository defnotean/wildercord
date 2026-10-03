package dev.wildercord.world.dungeons;

import dev.wildercord.aura.world.*;
import dev.wildercord.config.Config;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import java.util.*;

/** A stair into a buried blade gallery, two intent thresholds and a spacious physical duel chamber. */
public final class SwordTombPiece extends DungeonPiece {
	public SwordTombPiece(int x,int y,int z,Direction facing){super(DungeonWorldgen.SWORD_TOMB_PIECE,x,y,z,41,18,55,facing);}
	public SwordTombPiece(CompoundTag tag){super(DungeonWorldgen.SWORD_TOMB_PIECE,tag);}
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c){
		if(!Config.get().auraWorld().swordTombs())return Optional.empty();
		var piece=new SwordTombPiece(c.chunkPos().getMinBlockX(),0,c.chunkPos().getMinBlockZ(),Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		var entrance=piece.localPosition(20,0,2);
		int surface=c.chunkGenerator().getFirstOccupiedHeight(entrance.getX(),entrance.getZ(),Heightmap.Types.WORLD_SURFACE_WG,c.heightAccessor(),c.randomState());
		int floor=c.chunkGenerator().getFirstOccupiedHeight(entrance.getX(),entrance.getZ(),Heightmap.Types.OCEAN_FLOOR_WG,c.heightAccessor(),c.randomState());
		if(surface<c.chunkGenerator().getSeaLevel() || surface-floor>1)return Optional.empty();
		piece.move(0,surface-12,0);return Optional.of(new Structure.GenerationStub(entrance.atY(surface),b->b.addPiece(piece)));
	}
	@Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox bb,ChunkPos chunk,BlockPos ref){
		DungeonWards.remember(level,this);var wall=Blocks.DEEPSLATE_TILES.defaultBlockState();var carved=Blocks.CHISELED_STONE_BRICKS.defaultBlockState();var trim=Blocks.POLISHED_ANDESITE.defaultBlockState();
		room(level,bb,10,0,0,30,12,16,wall);room(level,bb,6,0,15,34,7,25,wall);
		room(level,bb,4,0,24,36,10,47,wall);room(level,bb,10,0,46,30,7,54,wall);
		fill(level,bb,19,1,15,21,3,25,AIR);fill(level,bb,19,1,46,21,3,48,AIR);
		// Twelve grounded steps join the open entrance to the gallery; every step has walking headroom.
		for(int step=0;step<=12;step++){
			int z=2+step,y=12-step;fill(level,bb,18,0,z,22,y,z,trim);fill(level,bb,18,y+1,z,22,17,z,AIR);
		}
		// The roofed vestibule is marked at the surface by a broken arch and two carved blade stones.
		for(int x:new int[]{16,24}){fill(level,bb,x,12,1,x,15,1,carved);set(level,bb,Blocks.STONE_BRICK_SLAB.defaultBlockState(),x,16,1);}
		fill(level,bb,17,15,1,23,15,1,carved);fill(level,bb,18,13,0,22,14,1,AIR);
		// A grass barrow covers the vestibule; keep the descending stair open to the sky.
		for(int x=10;x<=30;x++)for(int z=0;z<=16;z++){
			if(x>=18 && x<=22 && z<=14)continue;
			double slope=(x-20)*(x-20)/100.0+(z-8)*(z-8)/64.0;
			int top=13+(int)Math.max(0,3*(1-slope));
			fill(level,bb,x,12,z,x,top-1,z,Blocks.DIRT.defaultBlockState());set(level,bb,Blocks.GRASS_BLOCK.defaultBlockState(),x,top,z);
		}
		// Each gallery bay has a grave plinth, steel blade and modest lamp, arranged away from the central route.
		for(int x:new int[]{9,31})for(int z:new int[]{18,22}){
			fill(level,bb,x-1,1,z-1,x+1,1,z+1,Blocks.POLISHED_DEEPSLATE.defaultBlockState());
			set(level,bb,Blocks.IRON_BARS.defaultBlockState(),x,2,z);set(level,bb,Blocks.LANTERN.defaultBlockState(),x,3,z);
		}
		for(int z:new int[]{27,34,43})for(int x:new int[]{6,34}){
			fill(level,bb,x,1,z,x,7,z,carved);set(level,bb,Blocks.LANTERN.defaultBlockState(),x,8,z);
		}
		for(int x:new int[]{14,26})for(int z:new int[]{30,40}){
			fill(level,bb,x,7,z,x,9,z,Blocks.IRON_CHAIN.defaultBlockState());
			set(level,bb,Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING,true),x,6,z);
		}
		// Low pendants light the actual duel and the keeper's mask, rather than only the perimeter.
		for(int x:new int[]{18,22}){
			fill(level,bb,x,5,36,x,9,36,Blocks.IRON_CHAIN.defaultBlockState());
			set(level,bb,Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING,true),x,4,36);
		}
		// Broken burial terraces provide flanking cover without choking the sweep/thrust lanes.
		for(int x:new int[]{8,29}){fill(level,bb,x,0,29,x+3,0,42,trim);for(int z:new int[]{30,40})fill(level,bb,x,1,z,x+2,1,z+1,Blocks.STONE_BRICK_SLAB.defaultBlockState());}
		fill(level,bb,14,0,28,26,0,44,Blocks.SMOOTH_STONE.defaultBlockState());
		if(variant()==1){fill(level,bb,7,1,18,12,2,20,AIR);fill(level,bb,7,0,18,12,0,20,Blocks.MOSS_BLOCK.defaultBlockState());}
		if(variant()==2){fill(level,bb,28,1,48,32,3,50,AIR);fill(level,bb,28,0,48,32,0,50,trim);}
		gate(level,bb,15,2);gate(level,bb,24,3);
		chest(level,bb,7,1,23,DungeonWorldgen.TOMB_HALL,Direction.EAST,931);
		chest(level,bb,29,1,51,DungeonWorldgen.TOMB_HALL,Direction.WEST,932);
		var altar=localPosition(20,1,44);
		if(bb.isInside(altar)){
			level.setBlock(altar,SwordTombs.RELIQUARY.defaultBlockState().setValue(TombReliquary.FACING,inward(20,44)),2);
			if(level.getBlockEntity(altar) instanceof TombReliquaryEntity entity)entity.awaken();
		}
		fill(level,bb,17,1,50,23,1,52,carved);set(level,bb,Blocks.IRON_BARS.defaultBlockState(),20,2,51);
	}
	private void gate(WorldGenLevel level,BoundingBox bb,int z,int stage){for(int x=19;x<=21;x++)for(int y=1;y<=3;y++){
		var p=localPosition(x,y,z);if(bb.isInside(p))level.setBlock(p,SwordTombs.GATE.defaultBlockState().setValue(IntentGate.STAGE,stage).setValue(IntentGate.FACING,inward(x,z)),2);
	}}
	@Override public List<BoundingBox> wardedBoxes(){return List.of(worldBox(6,0,15,34,7,25),worldBox(4,0,24,36,10,47),worldBox(10,0,46,30,7,54));}
}
