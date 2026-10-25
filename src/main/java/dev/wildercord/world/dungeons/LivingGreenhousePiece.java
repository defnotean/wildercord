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
 public LivingGreenhousePiece(int x,int y,int z,Direction f){super(DungeonWorldgen.GREENHOUSE_PIECE,x,y,z,29,18,37,f);}
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
  shell(level,bb);
  fill(level,bb,12,1,1,16,4,1,AIR);
  if(variant()!=0){fill(level,bb,12,1,35,16,4,35,AIR);fill(level,bb,11,0,30,17,0,35,brick);}
  // Raised beds along the side walls, a paved walk down the middle, a flush stone rim round the pool.
  for(int[] r:new int[][]{{2,3},{25,26}})for(int[] zz:new int[][]{{3,10},{26,33}}){
   fill(level,bb,r[0],1,zz[0],r[1],1,zz[1],Blocks.ROOTED_DIRT.defaultBlockState());
   for(int x=r[0];x<=r[1];x++)for(int z=zz[0];z<=zz[1];z++){int n=noise(x,z);
    set(level,bb,n<30?Blocks.FERN.defaultBlockState():n<50?Blocks.AZALEA.defaultBlockState():n<65?Blocks.ALLIUM.defaultBlockState():n<80?Blocks.LILY_OF_THE_VALLEY.defaultBlockState():Blocks.SHORT_GRASS.defaultBlockState(),x,2,z);}
   fill(level,bb,r[0],1,zz[0]-1,r[1],1,zz[0]-1,Blocks.MOSSY_COBBLESTONE.defaultBlockState());fill(level,bb,r[0],1,zz[1]+1,r[1],1,zz[1]+1,Blocks.MOSSY_COBBLESTONE.defaultBlockState());}
  var walk=Palette.of(Blocks.STONE_BRICKS,5,Blocks.MOSSY_STONE_BRICKS,3,Blocks.CRACKED_STONE_BRICKS,2);
  fill(level,bb,13,0,2,15,0,12,walk);fill(level,bb,13,0,24,15,0,29,walk);
  for(int x=9;x<=19;x++)for(int z=13;z<=23;z++)if(x==9||x==19||z==13||z==23)set(level,bb,walk.at(x,0,z),x,0,z);
  if(variant()==2){fill(level,bb,3,2,5,7,2,5,Blocks.SPRUCE_SLAB.defaultBlockState());fill(level,bb,21,2,29,25,2,29,Blocks.SPRUCE_SLAB.defaultBlockState());}
  // Safe water under the gap; a physical rim route permits harvesting without life magic.
  fill(level,bb,10,0,14,18,0,22,Blocks.WATER.defaultBlockState());fill(level,bb,10,-1,14,18,-1,22,brick);
  for(int x:new int[]{5,23})for(int z:new int[]{6,12,26,32}) {
   set(level,bb,Blocks.ROOTED_DIRT.defaultBlockState(),x,0,z);set(level,bb,Blocks.FLOWERING_AZALEA.defaultBlockState(),x,1,z);
   set(level,bb,Blocks.SHROOMLIGHT.defaultBlockState(),x,6,z);
  }
  // The lamps hang on chains from the glass vault; lily pads on the pool, moss creeping over the floor.
  for(int x:new int[]{5,23})for(int z:new int[]{6,12,26,32})fill(level,bb,x,7,z,x,13,z,Blocks.IRON_CHAIN.defaultBlockState());
  hangingLantern(level,bb,14,15,7,4,false);hangingLantern(level,bb,14,15,29,4,false);hangingLantern(level,bb,14,15,18,5,false);
  for(int x=10;x<=18;x++)for(int z=14;z<=22;z++)if(x!=14&&noise(x,z)<14)set(level,bb,Blocks.LILY_PAD.defaultBlockState(),x,1,z);
  scatter(level,bb,2,1,2,26,1,34,Blocks.MOSS_CARPET.defaultBlockState(),12);
  mechanism(level,bb,DungeonBlocks.GARDEN,14,1,13);
  chest(level,bb,4,1,17,DungeonWorldgen.GARDEN_HALL,Direction.EAST,401);chest(level,bb,24,1,17,DungeonWorldgen.GARDEN_HALL,Direction.WEST,402);
  chest(level,bb,14,1,32,DungeonWorldgen.GARDEN_VAULT,Direction.NORTH,403);
  guard(level,bb,EntityTypes.BOGGED,7,1,10,List.of(Runes.BOLT,Runes.VENOM),false);
  guard(level,bb,EntityTypes.WITCH,21,1,27,List.of(Runes.ZONE,Runes.MIRE),true);
 }
 /** The roof's height over column {@code x}: a glass barrel vault from the eaves at y 10 to its crown at y 16. */
 private static int crown(int x){double d=Math.abs(x-14)/13.5;return 10+(int)Math.round(6*Math.sqrt(Math.max(0,1-d*d)));}
 private void shell(WorldGenLevel level,BoundingBox bb) {
  var glass=Blocks.GLASS.defaultBlockState();var brick=Palette.of(Blocks.MOSSY_STONE_BRICKS,5,Blocks.STONE_BRICKS,3,Blocks.CRACKED_STONE_BRICKS,1);
  var st=Blocks.STONE_BRICK_STAIRS;
  // A stone plinth two courses high under the glass, and a band of stone at mid height.
  walls(level,bb,1,0,1,27,1,35,brick);walls(level,bb,1,6,1,27,6,35,brick);
  // The barrel vault: glass between stone ribs over every bay line, the gable ends glazed under a stone arch.
  fill(level,bb,2,10,2,26,10,34,AIR);
  for(int x=1;x<=27;x++){int top=crown(x),dx=Math.abs(x-14),lo=dx>=13?10:Math.min(top,crown(x<14?x-1:x+1));
   for(int z=1;z<=35;z++){boolean rib=z==1||z==12||z==18||z==24||z==35;
    if(z==1||z==35)fill(level,bb,x,10,z,x,top,z,glass);
    for(int y=lo;y<=top;y++)set(level,bb,rib?brick.at(x,y,z):glass,x,y,z);}}
  // The inner columns climb to the vault; the corner piers end in pinnacles with lamps.
  for(int x:new int[]{9,19})for(int z:new int[]{12,24})fill(level,bb,x,10,z,x,crown(x)-1,z,brick);
  for(int x:new int[]{1,27})for(int z:new int[]{1,35}){fill(level,bb,x,10,z,x,12,z,brick);set(level,bb,Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),x,13,z);set(level,bb,Blocks.LANTERN.defaultBlockState(),x,14,z);}
  for(int z:new int[]{1,35})set(level,bb,Blocks.LANTERN.defaultBlockState(),14,17,z);
  // A cornice of upturned stairs at the eaves and a skirting at the foot, in the margin round the glass.
  for(int z=2;z<=34;z++){set(level,bb,stair(st,Direction.EAST,true),0,10,z);set(level,bb,stair(st,Direction.WEST,true),28,10,z);
   set(level,bb,stair(st,Direction.EAST,false),0,0,z);set(level,bb,stair(st,Direction.WEST,false),28,0,z);}
  for(int x=2;x<=26;x++)if(x<11||x>17){set(level,bb,stair(st,Direction.NORTH,false),x,0,0);set(level,bb,stair(st,Direction.SOUTH,false),x,0,36);}
  // The entrance portico: two columns, an entablature and a stone pediment over the door.
  for(int x:new int[]{11,17})pillar(level,bb,x,0,0,4,Blocks.STONE_BRICKS.defaultBlockState(),Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState(),Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
  fill(level,bb,12,0,0,16,0,0,brick);fill(level,bb,11,5,0,17,5,1,brick);
  for(int k=0;k<3;k++)for(int z=0;z<=1;z++){set(level,bb,stair(st,Direction.EAST,false),11+k,6+k,z);set(level,bb,stair(st,Direction.WEST,false),17-k,6+k,z);
   fill(level,bb,12+k,6+k,z,16-k,6+k,z,brick);}
  set(level,bb,Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),14,9,0);
  set(level,bb,stair(st,Direction.WEST,true),12,4,0);set(level,bb,stair(st,Direction.EAST,true),16,4,0);
  hangingLantern(level,bb,14,4,0,0,false);
  // Neglect: a few panes of the vault fallen in, azalea pressing against the glass.
  weather(level,bb,2,11,2,26,16,34,glass,AIR,5);
  weather(level,bb,1,2,1,27,9,35,glass,Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT,true),4);
 }
 @Override public List<BoundingBox> wardedBoxes(){return List.of(worldBox(1,0,24,27,10,35));}
}
