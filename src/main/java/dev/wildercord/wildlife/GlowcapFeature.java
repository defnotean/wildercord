package dev.wildercord.wildlife;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.block.Block;
/** Twelve bounded columns across the finite underground band; no terrain replacement or spread. */
public record GlowcapFeature() implements Feature {
 public static final MapCodec<GlowcapFeature> CODEC=MapCodec.unit(new GlowcapFeature());
 public MapCodec<GlowcapFeature> codec() {return CODEC;}
 /** Natural decoration samples one column in each of twelve disjoint current-chunk strata.
  * It does not rely on a neighboring chunk having already acquired its lush floor and water.
  * Direct/manual placement deliberately keeps its supplied origin's local nine-block square.
  */
 static BlockPos column(boolean natural,RandomSource r,BlockPos origin,int index) {
  if(index<0 || index>=12)throw new IllegalArgumentException("Glowcap column outside finite patch");
  if(!natural)return new BlockPos(origin.getX()+r.nextInt(9)-4,47,origin.getZ()+r.nextInt(9)-4);
  int startX=(origin.getX()>>4)<<4,startZ=(origin.getZ()>>4)<<4;
  int row=index/4,lowZ=row*16/3,highZ=(row+1)*16/3;
  return new BlockPos(startX+(index%4)*4+r.nextInt(4),47,startZ+lowZ+r.nextInt(highZ-lowZ));
 }
 public boolean place(WorldGenLevel l,ChunkGenerator generator,RandomSource r,BlockPos origin) {
  int placed=0;boolean natural=l instanceof net.minecraft.server.level.WorldGenRegion;
  // Same upper bound as the local patch: twelve columns times112positions =1344candidates.
  for(int i=0;i<12;i++) {
   var top=column(natural,r,origin,i);
   for(int dy=0;dy<112;dy++) {
    var p=top.below(dy);if(p.getY()<l.getMinY() || !l.hasChunkAt(p))break;
    if(!l.isEmptyBlock(p) || !GlowcapBlock.footing(l,p) || !GlowcapBlock.moist(l,p) || !GlowcapBlock.structuralCover(l,p))continue;
    if(natural && !(l.getBiome(p).is(net.minecraft.world.level.biome.Biomes.LUSH_CAVES) || l.getBiome(p).is(net.minecraft.world.level.biome.Biomes.DRIPSTONE_CAVES)))continue;
    var state=FungalGarden.GLOWCAP.defaultBlockState();if(state.canSurvive(l,p)) {l.setBlock(p,state,Block.UPDATE_CLIENTS);placed++;break;}
   }
  }
  return placed>0;
 }
}
