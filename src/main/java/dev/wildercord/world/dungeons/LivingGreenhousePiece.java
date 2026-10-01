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

/** Glass conservatory with a broken garden path and a one-time cleanse-versus-harvest choice. */
public final class LivingGreenhousePiece extends DungeonPiece {
 public LivingGreenhousePiece(int x,int y,int z,Direction f){super(DungeonWorldgen.GREENHOUSE_PIECE,x,y,z,29,13,37,f);}
 public LivingGreenhousePiece(CompoundTag tag){super(DungeonWorldgen.GREENHOUSE_PIECE,tag);}
 static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
  var chunk=c.chunkPos();var piece=new LivingGreenhousePiece(chunk.getMinBlockX(),0,chunk.getMinBlockZ(),Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));var entrance=piece.getWorldPos(14,0,0);
  int y=c.chunkGenerator().getFirstOccupiedHeight(entrance.getX(),entrance.getZ(),Heightmap.Types.WORLD_SURFACE_WG,c.heightAccessor(),c.randomState());piece.move(0,y-1,0);
  return Optional.of(new Structure.GenerationStub(new BlockPos(entrance.getX(),y,entrance.getZ()),b->b.addPiece(piece)));
 }
 @Override public void postProcess(WorldGenLevel level,StructureManager structures,ChunkGenerator generator,RandomSource random,BoundingBox bb,ChunkPos chunk,BlockPos reference) {
  DungeonWards.remember(level,this);var brick=Blocks.MOSSY_STONE_BRICKS.defaultBlockState();var moss=Blocks.MOSS_BLOCK.defaultBlockState();
  room(level,bb,1,0,1,27,10,35,Blocks.GLASS.defaultBlockState());fill(level,bb,1,0,1,27,0,35,moss);
  for(int x:new int[]{1,9,19,27})for(int z:new int[]{1,12,24,35})fill(level,bb,x,0,z,x,10,z,brick);
  fill(level,bb,12,1,1,16,4,1,AIR);
  if(variant()!=0){fill(level,bb,12,1,35,16,4,35,AIR);fill(level,bb,11,0,30,17,0,35,brick);}
  if(variant()==2){fill(level,bb,3,2,5,7,2,5,Blocks.SPRUCE_SLAB.defaultBlockState());fill(level,bb,21,2,29,25,2,29,Blocks.SPRUCE_SLAB.defaultBlockState());}
  // Safe water under the gap; a physical rim route permits harvesting without life magic.
  fill(level,bb,10,0,14,18,0,22,Blocks.WATER.defaultBlockState());fill(level,bb,10,-1,14,18,-1,22,brick);
  for(int x:new int[]{5,23})for(int z:new int[]{6,12,26,32}) {
   set(level,bb,Blocks.ROOTED_DIRT.defaultBlockState(),x,0,z);set(level,bb,Blocks.FLOWERING_AZALEA.defaultBlockState(),x,1,z);
   set(level,bb,Blocks.SHROOMLIGHT.defaultBlockState(),x,6,z);
  }
  mechanism(level,bb,DungeonBlocks.GARDEN,14,1,13);
  chest(level,bb,4,1,17,DungeonWorldgen.GARDEN_HALL,Direction.EAST,401);chest(level,bb,24,1,17,DungeonWorldgen.GARDEN_HALL,Direction.WEST,402);
  chest(level,bb,14,1,32,DungeonWorldgen.GARDEN_VAULT,Direction.NORTH,403);
  guard(level,bb,EntityTypes.BOGGED,7,1,10,List.of(Runes.BOLT,Runes.VENOM),false);
  guard(level,bb,EntityTypes.WITCH,21,1,27,List.of(Runes.ZONE,Runes.MIRE),true);
 }
 @Override public List<BoundingBox> wardedBoxes(){return List.of(worldBox(1,0,24,27,10,35));}
}
