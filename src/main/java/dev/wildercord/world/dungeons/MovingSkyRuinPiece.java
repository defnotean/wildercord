package dev.wildercord.world.dungeons;
import dev.wildercord.content.dungeons.*;
import dev.wildercord.spell.Runes;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import java.util.*;

/** A visibly suspended ruin with shifting stepping stones, scaffold access and a recovery terrace. */
public final class MovingSkyRuinPiece extends DungeonPiece {
 public MovingSkyRuinPiece(int x,int y,int z,Direction f){super(DungeonWorldgen.SKY_RUIN_PIECE,x,y,z,25,18,29,f);}
 public MovingSkyRuinPiece(CompoundTag tag){super(DungeonWorldgen.SKY_RUIN_PIECE,tag);}
 static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
  var chunk=c.chunkPos();var piece=new MovingSkyRuinPiece(chunk.getMinBlockX(),0,chunk.getMinBlockZ(),Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));var entrance=piece.getWorldPos(12,0,1);
  int y=c.chunkGenerator().getFirstOccupiedHeight(entrance.getX(),entrance.getZ(),Heightmap.Types.WORLD_SURFACE_WG,c.heightAccessor(),c.randomState());piece.move(0,Math.min(c.heightAccessor().getMaxY()-20,y+8),0);
  return Optional.of(new Structure.GenerationStub(new BlockPos(entrance.getX(),piece.getBoundingBox().minY(),entrance.getZ()),b->b.addPiece(piece)));
 }
 @Override public void postProcess(WorldGenLevel level,StructureManager structures,ChunkGenerator generator,RandomSource random,BoundingBox bb,ChunkPos chunk,BlockPos reference) {
  DungeonWards.remember(level,this);var stone=Blocks.SMOOTH_QUARTZ.defaultBlockState();
  fill(level,bb,2,0,2,22,0,26,stone);fill(level,bb,2,1,2,22,17,26,AIR);
  fill(level,bb,8,6,2,16,6,8,stone);fill(level,bb,8,6,19,16,6,26,stone);
  for(int x:new int[]{8,16})for(int z:new int[]{2,8,19,26}){fill(level,bb,x,7,z,x,12,z,stone);set(level,bb,Blocks.SEA_LANTERN.defaultBlockState(),x,13,z);}
  // Weathered copper borders, carved column bands and broken high arches give the ruin a silhouette.
  var copper=Blocks.CUT_COPPER.waxed().oxidized().defaultBlockState();
  var carved=Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
  for(int x=2;x<=22;x++){set(level,bb,copper,x,0,2);set(level,bb,copper,x,0,26);}
  for(int z=3;z<26;z++){set(level,bb,copper,2,0,z);set(level,bb,copper,22,0,z);}
  for(int x:new int[]{8,16})for(int z:new int[]{2,8,19,26}){
   set(level,bb,carved,x,7,z);set(level,bb,carved,x,10,z);set(level,bb,copper,x,12,z);
  }
  for(int z:new int[]{2,26})for(int x=9;x<=15;x++) {
   if(x==12&&z==2)continue; // A collapsed section keeps the entrance crown visibly asymmetric.
   set(level,bb,Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState(),x,12,z);
  }
  for(int z:new int[]{3,7,20,25})for(int x=9;x<=15;x++)if(x!=12)set(level,bb,copper,x,6,z);
  for(int x:new int[]{10,14}){fill(level,bb,x,7,26,x,9,26,Blocks.QUARTZ_BRICKS.defaultBlockState());set(level,bb,carved,x,10,26);}
  set(level,bb,Blocks.AMETHYST_BLOCK.defaultBlockState(),12,13,26);
  for(int y=1;y<=7;y++)set(level,bb,Blocks.SCAFFOLDING.defaultBlockState(),12,y,3);
  // Optional outer route uses physical scaffolding, while the central moving puzzle stays available.
  if(variant()!=0){for(int z=8;z<=19;z++)set(level,bb,Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState(),5,6,z);for(int y=1;y<=7;y++)set(level,bb,Blocks.SCAFFOLDING.defaultBlockState(),5,y,8);fill(level,bb,5,6,19,8,6,19,stone);}
  if(variant()==2){fill(level,bb,19,6,8,19,6,19,Blocks.CUT_COPPER.waxed().oxidized().defaultBlockState());fill(level,bb,16,6,19,19,6,19,stone);for(int y=1;y<=7;y++)set(level,bb,Blocks.SCAFFOLDING.defaultBlockState(),19,y,8);}
  // The anchor is y7. Its collision stones lie at y6, with empty landing positions beside them.
  mechanism(level,bb,DungeonBlocks.SKY,12,7,7);
  for(int distance:new int[]{3,6,9}) {
   BlockPos stonePos=getWorldPos(12,7,7).relative(inward(12,7),distance).relative(inward(12,7).getClockWise(),-1).below();
   if(bb.isInside(stonePos))level.setBlock(stonePos,Blocks.IRON_BLOCK.defaultBlockState(),2);
  }
  chest(level,bb,9,7,5,DungeonWorldgen.SKY_HALL,Direction.EAST,501);chest(level,bb,12,7,24,DungeonWorldgen.SKY_VAULT,Direction.NORTH,502);
  guard(level,bb,EntityTypes.STRAY,14,7,23,List.of(Runes.ARC,Runes.WINDCUT),true);
 }
 @Override public List<BoundingBox> wardedBoxes(){return List.of(worldBox(8,6,19,16,14,26));}
}
