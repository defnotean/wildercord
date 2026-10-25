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
  ruin(level,bb,copper,carved);
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
 private void ruin(WorldGenLevel level,BoundingBox bb,net.minecraft.world.level.block.state.BlockState copper,net.minecraft.world.level.block.state.BlockState carved) {
  var shaft=Blocks.QUARTZ_PILLAR.defaultBlockState();var bricks=Blocks.QUARTZ_BRICKS.defaultBlockState();
  var smooth=Blocks.SMOOTH_QUARTZ.defaultBlockState();var slab=Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState();
  // The floating plaza: a copper compass rose laid in the floor, its rim raised as a low parapet with a gap at the stair.
  for(int x=3;x<=21;x++)for(int z=3;z<=25;z++){double r=dist(x,z,12,14);
   if(r<=9.5&&(onSpoke(x,z,12,14,8,0.5)||Math.abs(r-9)<0.5||Math.abs(r-4)<0.5))set(level,bb,copper,x,0,z);
   else if(noise(x,z)<12)set(level,bb,bricks,x,0,z);}
  set(level,bb,carved,12,0,14);
  for(int x=2;x<=22;x++)for(int z:new int[]{2,26})if((x<11||x>13)&&noise(x,z)<80)set(level,bb,(x+z)%4==0?carved:slab,x,1,z);
  for(int z=3;z<=25;z++)for(int x:new int[]{2,22})if(noise(x,z)<80)set(level,bb,(x+z)%4==0?carved:slab,x,1,z);
  // The platform columns run down to the plaza, so both terraces stand on a colonnade rather than on nothing.
  for(int x:new int[]{8,16})for(int z:new int[]{2,8,19,26}){
   set(level,bb,carved,x,1,z);fill(level,bb,x,2,z,x,4,z,shaft);set(level,bb,carved,x,5,z);
   fill(level,bb,x,8,z,x,9,z,shaft);set(level,bb,shaft,x,11,z);}
  // Edge corbels under each terrace lip.
  for(int x=9;x<=15;x++)for(int z:new int[]{2,8,19,26})if(!(x==12&&z==2))set(level,bb,topSlab(Blocks.SMOOTH_QUARTZ_SLAB),x,5,z);
  // Broken columns along the plaza's flanks, each snapped at its own height; the tallest still carry a lamp.
  for(int x:new int[]{3,21})for(int z=4;z<=24;z+=5){int top=3+noise(x,z)%7;
   set(level,bb,carved,x,1,z);fill(level,bb,x,2,z,x,top,z,shaft);
   set(level,bb,top>=8?carved:slab,x,top+1,z);if(top>=8)set(level,bb,Blocks.END_ROD.defaultBlockState(),x,top+2,z);}
  // The vault terrace roofed as a stepped shrine: an entablature, three receding tiers, a crystal at the crown.
  for(int x=8;x<=16;x++)for(int z=19;z<=26;z++)if((x==8||x==16||z==19||z==26)&&!(x==12&&z==26))set(level,bb,(x==8||x==16)&&(z==19||z==26)?carved:bricks,x,13,z);
  fill(level,bb,9,14,20,15,14,25,smooth);fill(level,bb,10,15,21,14,15,24,bricks);fill(level,bb,11,16,22,13,16,23,carved);
  set(level,bb,Blocks.AMETHYST_BLOCK.defaultBlockState(),12,17,22);
  for(int x=9;x<=15;x++){set(level,bb,slab,x,14,19);set(level,bb,slab,x,14,26);}
  for(int z=20;z<=25;z++){set(level,bb,slab,8,14,z);set(level,bb,slab,16,14,z);}
  weather(level,bb,9,14,20,15,15,25,smooth,AIR,18);weather(level,bb,9,14,20,15,15,25,bricks,AIR,12);
  // Lamps hanging inside the shrine and under the broken entrance arch.
  hangingLantern(level,bb,12,13,22,2,true);
  for(int x:new int[]{10,14})hangingLantern(level,bb,x,11,2,1,true);
  // Crystal grown in the cracks of the plaza and on the terraces.
  scatter(level,bb,3,1,3,21,1,25,Blocks.AMETHYST_CLUSTER.defaultBlockState(),4);
  weather(level,bb,2,0,2,22,0,26,smooth,Blocks.CALCITE.defaultBlockState(),8);
 }
 @Override public List<BoundingBox> wardedBoxes(){return List.of(worldBox(8,6,19,16,14,26));}
}
