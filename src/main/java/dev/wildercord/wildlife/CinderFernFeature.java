package dev.wildercord.wildlife;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Block;
/** Sixteen bounded surface probes, no existing plant or fluid replacement. */
public record CinderFernFeature() implements Feature {
 public static final MapCodec<CinderFernFeature> CODEC=MapCodec.unit(new CinderFernFeature());
 public MapCodec<CinderFernFeature> codec(){return CODEC;}
 public boolean place(WorldGenLevel l,ChunkGenerator generator,RandomSource r,BlockPos origin){if(!l.getLevel().dimension().equals(net.minecraft.world.level.Level.OVERWORLD))return false;int placed=0;
  for(int i=0;i<16;i++){int x=origin.getX()+r.nextInt(7)-3,z=origin.getZ()+r.nextInt(7)-3;var p=new BlockPos(x,l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z),z);if(!l.hasChunkAt(p)||l.isOutsideBuildHeight(p)||l instanceof net.minecraft.server.level.WorldGenRegion region&&!region.isWithinWriteZone(p))continue;var state=EmberContent.FERN.defaultBlockState().setValue(CinderFernBlock.AGE,r.nextInt(3));if(l.isEmptyBlock(p)&&l.getFluidState(p).isEmpty()&&state.canSurvive(l,p)&&l.setBlock(p,state,Block.UPDATE_CLIENTS))placed++;}return placed>0;
 }
}
