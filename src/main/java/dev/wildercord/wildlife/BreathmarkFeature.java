package dev.wildercord.wildlife;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.block.Block;
/** One tiny authored field marker, never a dungeon or a source of repeatable site rewards. */
public record BreathmarkFeature() implements Feature {
 public static final MapCodec<BreathmarkFeature> CODEC=MapCodec.unit(new BreathmarkFeature());
 public MapCodec<BreathmarkFeature> codec() {return CODEC;}
 public boolean place(WorldGenLevel l,ChunkGenerator generator,RandomSource r,BlockPos origin) {
  int kind=r.nextInt(2);
  for(int i=0;i<12;i++) {
   int x=origin.getX()+r.nextInt(9)-4,z=origin.getZ()+r.nextInt(9)-4;
   for(int y=47;y>=Math.max(-64,l.getMinY());y--) {
   var p=new BlockPos(x,y,z);
   if(!l.hasChunkAt(p))break;
   if(!l.isEmptyBlock(p) || !GlowcapBlock.footing(l,p) || !GlowcapBlock.structuralCover(l,p))continue;
   l.setBlock(p,FungalGarden.BREATHMARK.defaultBlockState().setValue(BreathmarkBlock.KIND,kind),Block.UPDATE_CLIENTS);
   if(l.getBlockEntity(p) instanceof BreathmarkEntity e)e.awaken();else {l.removeBlock(p,false);return false;}
   return true;
   }
  }return false;
 }
}
