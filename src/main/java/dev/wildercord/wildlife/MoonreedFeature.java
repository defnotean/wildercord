package dev.wildercord.wildlife;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.block.Block;
/** Twenty-four bounded attempts at a damp bank; never replaces an existing plant or water. */
public class MoonreedFeature extends Feature<NoneFeatureConfiguration> {
 public MoonreedFeature() {super(NoneFeatureConfiguration.CODEC);}
 @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {return place(context.level(),context.chunkGenerator(),context.random(),context.origin());}
 public boolean place(WorldGenLevel l,ChunkGenerator generator,RandomSource r,BlockPos origin) {
  int placed=0;
  for(int i=0;i<24;i++) {
   int x=origin.getX()+r.nextInt(9)-4,z=origin.getZ()+r.nextInt(9)-4;
   var at=new BlockPos(x,MoonreedBlock.surfaceHeight(l,x,z),z);
   var state=WetlandGarden.REED.defaultBlockState().setValue(MoonreedBlock.AGE,1);
   if(l.isEmptyBlock(at) && state.canSurvive(l,at) && MoonreedBlock.moist(l,at) && MoonreedBlock.openSky(l,at)) {l.setBlock(at,state,Block.UPDATE_CLIENTS);placed++;}
  }
  placed+=dev.wildercord.world.biome.ReedlandBanks.place(l,origin);
  return placed>0;
 }
}
