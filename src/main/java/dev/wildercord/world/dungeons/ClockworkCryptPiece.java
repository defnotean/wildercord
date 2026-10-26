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
 public ClockworkCryptPiece(int x,int y,int z,Direction facing){super(DungeonWorldgen.CLOCKWORK_PIECE,x,y,z,27,26,35,facing);}
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
  // The vault floor is a clock face: a dark dial, twelve gold hour marks, a lamp at its hub (the trap's lane stays level).
  for(int x=7;x<=19;x++)for(int z=24;z<=33;z++){double d=dist(x,z,13,28);
   if(d>=3.5&&d<4.5)set(level,bb,onSpoke(x,z,13,28,12,.5)?gold:Blocks.POLISHED_DEEPSLATE.defaultBlockState(),x,0,z);
   else if(d<3.5)set(level,bb,Blocks.DEEPSLATE_TILES.defaultBlockState(),x,0,z);}
  set(level,bb,Blocks.SEA_LANTERN.defaultBlockState(),13,0,28);
  // Sarcophagi along the tomb walls, each with a candle long gone out.
  for(int z:new int[]{13,19}){fill(level,bb,2,1,z,4,1,z,Blocks.POLISHED_DEEPSLATE.defaultBlockState());fill(level,bb,22,1,z,24,1,z,Blocks.POLISHED_DEEPSLATE.defaultBlockState());
   set(level,bb,Blocks.CANDLE.defaultBlockState(),3,2,z);set(level,bb,Blocks.CANDLE.defaultBlockState(),23,2,z);}
  // A side gallery crosses behind the tombs in two layouts; all retain the central route.
  if(variant()!=0){fill(level,bb,4,0,20,22,0,22,gold);fill(level,bb,4,1,20,22,3,22,AIR);}
  if(variant()==2){fill(level,bb,7,0,25,9,0,31,gold);fill(level,bb,7,1,25,9,3,31,AIR);set(level,bb,Blocks.REDSTONE_LAMP.defaultBlockState(),8,4,28);}
  // A stair flight is an ordinary route out: time magic is useful, never mandatory.
  for(int z=0;z<=8;z++) {fill(level,bb,11,z,8-z,15,z,8-z,gold);fill(level,bb,11,z+1,8-z,15,11,8-z,AIR);}
  // Gold columns on dark plinths, crowned with chiseled deepslate under their lamps.
  var polished=Blocks.POLISHED_DEEPSLATE.defaultBlockState();var crown=Blocks.CHISELED_DEEPSLATE.defaultBlockState();var st=Blocks.DEEPSLATE_BRICK_STAIRS;
  for(int z:new int[]{12,22,29}) {for(int x:new int[]{7,19}){pillar(level,bb,x,z,1,6,polished,gold,crown);set(level,bb,Blocks.SEA_LANTERN.defaultBlockState(),x,7,z);}}
  dressing(level,bb,gold,polished,crown,st);
  mechanism(level,bb,DungeonBlocks.CLOCK,13,1,23);
  chest(level,bb,3,1,17,DungeonWorldgen.CLOCK_HALL,Direction.EAST,301);chest(level,bb,23,1,17,DungeonWorldgen.CLOCK_HALL,Direction.WEST,302);
  chest(level,bb,13,1,32,DungeonWorldgen.CLOCK_VAULT,Direction.NORTH,303);
  guard(level,bb,EntityTypes.HUSK,8,1,17,List.of(Runes.BOLT,Runes.COUNTDOWN),false);
  guard(level,bb,EntityTypes.SKELETON,18,1,29,List.of(Runes.ARC,Runes.AFTERSHOCK),true);
 }
 private void dressing(WorldGenLevel level,BoundingBox bb,net.minecraft.world.level.block.state.BlockState gold,net.minecraft.world.level.block.state.BlockState polished,net.minecraft.world.level.block.state.BlockState crown,net.minecraft.world.level.block.Block st) {
  // A stepped vault over the clock hall: two courses of corbels climbing in from each long wall, ribs across every few rows.
  for(int z=24;z<=33;z++){set(level,bb,stair(st,Direction.WEST,true),7,8,z);set(level,bb,stair(st,Direction.EAST,true),19,8,z);
   set(level,bb,stair(st,Direction.WEST,true),8,9,z);set(level,bb,stair(st,Direction.EAST,true),18,9,z);}
  for(int z:new int[]{25,28,31})fill(level,bb,9,9,z,17,9,z,polished);
  // Soul light over the dial and down the hall, and under the tomb ceilings.
  hangingLantern(level,bb,13,9,28,3,true);
  for(int z:new int[]{25,31})for(int x:new int[]{10,16})hangingLantern(level,bb,x,9,z,2,true);
  hangingLantern(level,bb,6,6,17,1,true);hangingLantern(level,bb,20,6,17,1,true);
  hangingLantern(level,bb,8,9,5,2,false);hangingLantern(level,bb,18,9,5,2,false);
  // The corridor mouths are framed: a dark lintel with a gold keystone and corbels under it.
  for(int z:new int[]{11,23}){fill(level,bb,9,5,z,17,5,z,polished);set(level,bb,gold,13,5,z);fill(level,bb,9,1,z,9,4,z,polished);fill(level,bb,17,1,z,17,4,z,polished);}
  fill(level,bb,13,6,11,13,9,11,Blocks.DEEPSLATE_BRICKS.defaultBlockState());
  for(int x=10;x<=16;x++){set(level,bb,stair(st,Direction.NORTH,true),x,5,10);set(level,bb,stair(st,Direction.SOUTH,true),x,5,24);}
  // Banners of the old order flank the hall's far doorway and the vault's chest.
  for(int x:new int[]{8,18})banner(level,bb,Blocks.WALL_BANNER.pick(net.minecraft.world.item.DyeColor.BLUE),x,4,10,Direction.SOUTH);
  for(int x:new int[]{11,15})banner(level,bb,Blocks.WALL_BANNER.pick(net.minecraft.world.item.DyeColor.BLUE),x,4,33,Direction.SOUTH);
  // Above ground: a portal of dark pilasters and a gold-keyed lintel at the stair's head, merlons along the hall's roof.
  fill(level,bb,10,8,0,10,11,0,polished);fill(level,bb,16,8,0,16,11,0,polished);fill(level,bb,11,11,0,15,11,0,polished);
  set(level,bb,gold,13,11,0);set(level,bb,crown,10,11,0);set(level,bb,crown,16,11,0);
  set(level,bb,stair(st,Direction.WEST,true),11,10,0);set(level,bb,stair(st,Direction.EAST,true),15,10,0);
  for(int x=6;x<=20;x++)for(int z=0;z<=12;z++){boolean rim=x==6||x==20||z==0||z==12;
   if(rim&&(x<9||x>17||z>0)&&(x+z)%2==0)set(level,bb,Blocks.DEEPSLATE_BRICKS.defaultBlockState(),x,11,z);}
  belfry(level,bb,gold,polished,crown);
  // Age: cracked bricks through every wall, webs in the tomb corners.
  weather(level,bb,0,0,0,26,25,34,Blocks.DEEPSLATE_BRICKS.defaultBlockState(),Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState(),16);
  for(int[] c:new int[][]{{2,12},{2,23},{24,12},{24,23}})set(level,bb,Blocks.COBWEB.defaultBlockState(),c[0],6,c[1]);
 }
 /** What stands on the mesa: a clock tower over the stair with a belfry and a hipped spire, obelisks at the hall's corners, and the vault's great roof. */
 private void belfry(WorldGenLevel level,BoundingBox bb,net.minecraft.world.level.block.state.BlockState gold,net.minecraft.world.level.block.state.BlockState polished,net.minecraft.world.level.block.state.BlockState crown) {
  var wall=Blocks.DEEPSLATE_BRICKS.defaultBlockState();var tiles=Blocks.DEEPSLATE_TILES.defaultBlockState();
  // The tower straddles the stair's far end, so the way down runs in under its face.
  room(level,bb,10,11,6,16,17,12,wall);
  for(int[] c:new int[][]{{10,6},{16,6},{10,12},{16,12}})pillar(level,bb,c[0],c[1],11,19,polished,polished,crown);
  for(int x=11;x<=15;x++)set(level,bb,stair(Blocks.DEEPSLATE_BRICK_STAIRS,Direction.NORTH,true),x,17,5);
  // The clock: a dark plate on the tower's face, gold hour marks at the quarters, end-rod hands from a carved hub.
  fill(level,bb,11,12,5,15,16,5,polished);
  for(int[] m:new int[][]{{13,16},{15,14},{13,12},{11,14}})set(level,bb,gold,m[0],m[1],5);
  set(level,bb,crown,13,14,5);
  set(level,bb,Blocks.END_ROD.defaultBlockState().setValue(net.minecraft.world.level.block.EndRodBlock.FACING,Direction.UP),13,15,5);
  set(level,bb,Blocks.END_ROD.defaultBlockState().setValue(net.minecraft.world.level.block.EndRodBlock.FACING,Direction.EAST),14,14,5);
  // An open belfry: the floor of the tower's top, a beam across, a bell hung from it, arches on every side.
  fill(level,bb,11,18,7,15,19,11,AIR);
  for(int x=11;x<=15;x++){set(level,bb,stair(Blocks.DEEPSLATE_BRICK_STAIRS,Direction.SOUTH,true),x,19,6);set(level,bb,stair(Blocks.DEEPSLATE_BRICK_STAIRS,Direction.NORTH,true),x,19,12);}
  for(int z=7;z<=11;z++){set(level,bb,stair(Blocks.DEEPSLATE_BRICK_STAIRS,Direction.EAST,true),10,19,z);set(level,bb,stair(Blocks.DEEPSLATE_BRICK_STAIRS,Direction.WEST,true),16,19,z);}
  fill(level,bb,11,19,9,15,19,9,polished);
  set(level,bb,Blocks.BELL.defaultBlockState().setValue(net.minecraft.world.level.block.BellBlock.ATTACHMENT,net.minecraft.world.level.block.state.properties.BellAttachType.CEILING),13,18,9);
  fill(level,bb,10,20,6,16,20,12,tiles);
  hipRoof(level,bb,10,6,16,12,21,Blocks.DEEPSLATE_TILE_STAIRS,tiles,crown);
  // Obelisks at the hall's four corners with soul fire on their tips.
  for(int[] c:new int[][]{{6,0},{20,0},{6,12},{20,12}}){pillar(level,bb,c[0],c[1],11,14,polished,wall,gold);set(level,bb,Blocks.SOUL_LANTERN.defaultBlockState(),c[0],15,c[1]);}
  // The vault's great hipped roof rises out of the rock behind.
  fill(level,bb,6,11,23,20,11,34,tiles);
  hipRoof(level,bb,7,24,19,33,12,Blocks.DEEPSLATE_TILE_STAIRS,tiles,crown);
  for(int[] c:new int[][]{{6,23},{20,23},{6,34},{20,34}}){pillar(level,bb,c[0],c[1],11,13,polished,wall,crown);set(level,bb,Blocks.SOUL_LANTERN.defaultBlockState(),c[0],14,c[1]);}
 }
 @Override public List<BoundingBox> wardedBoxes(){return List.of(worldBox(6,0,23,20,10,34));}
}
