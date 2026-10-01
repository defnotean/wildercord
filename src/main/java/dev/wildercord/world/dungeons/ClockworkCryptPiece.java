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

/** A buried clock gallery with two optional side tombs and a controllable stored-attack trap. */
public final class ClockworkCryptPiece extends DungeonPiece {
 public ClockworkCryptPiece(int x,int y,int z,Direction facing){super(DungeonWorldgen.CLOCKWORK_PIECE,x,y,z,27,12,35,facing);}
 public ClockworkCryptPiece(CompoundTag tag){super(DungeonWorldgen.CLOCKWORK_PIECE,tag);}
 static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
  var chunk=c.chunkPos();var piece=new ClockworkCryptPiece(chunk.getMinBlockX(),0,chunk.getMinBlockZ(),Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
  BlockPos entrance=piece.getWorldPos(13,0,0);int y=c.chunkGenerator().getFirstOccupiedHeight(entrance.getX(),entrance.getZ(),Heightmap.Types.WORLD_SURFACE_WG,c.heightAccessor(),c.randomState());
  piece.move(0,y-9,0);return Optional.of(new Structure.GenerationStub(new BlockPos(entrance.getX(),y,entrance.getZ()),b->b.addPiece(piece)));
 }
 @Override public void postProcess(WorldGenLevel level,StructureManager structures,ChunkGenerator generator,RandomSource random,BoundingBox bb,ChunkPos chunk,BlockPos reference) {
  DungeonWards.remember(level,this);var wall=Blocks.DEEPSLATE_BRICKS.defaultBlockState();var gold=Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
  room(level,bb,6,0,0,20,10,12,wall);room(level,bb,1,0,11,12,7,24,wall);room(level,bb,14,0,11,25,7,24,wall);room(level,bb,6,0,23,20,10,34,wall);
  fill(level,bb,10,1,10,16,4,24,AIR);
  // A side gallery crosses behind the tombs in two layouts; all retain the central route.
  if(variant()!=0){fill(level,bb,4,0,20,22,0,22,gold);fill(level,bb,4,1,20,22,3,22,AIR);}
  if(variant()==2){fill(level,bb,7,0,25,9,0,31,gold);fill(level,bb,7,1,25,9,3,31,AIR);set(level,bb,Blocks.REDSTONE_LAMP.defaultBlockState(),8,4,28);}
  // A stair flight is an ordinary route out: time magic is useful, never mandatory.
  for(int z=0;z<=8;z++) {fill(level,bb,11,z,8-z,15,z,8-z,gold);fill(level,bb,11,z+1,8-z,15,11,8-z,AIR);}
  for(int z:new int[]{12,22,29}) {for(int x:new int[]{7,19}){fill(level,bb,x,1,z,x,6,z,gold);set(level,bb,Blocks.SEA_LANTERN.defaultBlockState(),x,7,z);}}
  mechanism(level,bb,DungeonBlocks.CLOCK,13,1,23);
  chest(level,bb,3,1,17,DungeonWorldgen.CLOCK_HALL,Direction.EAST,301);chest(level,bb,23,1,17,DungeonWorldgen.CLOCK_HALL,Direction.WEST,302);
  chest(level,bb,13,1,32,DungeonWorldgen.CLOCK_VAULT,Direction.NORTH,303);
  guard(level,bb,EntityTypes.HUSK,8,1,17,List.of(Runes.BOLT,Runes.COUNTDOWN),false);
  guard(level,bb,EntityTypes.SKELETON,18,1,29,List.of(Runes.ARC,Runes.AFTERSHOCK),true);
 }
 @Override public List<BoundingBox> wardedBoxes(){return List.of(worldBox(6,0,23,20,10,34));}
}
