package dev.wildercord.wildlife;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
/** Three distinct rooms in a pre-existing covered cave, with one contained damp garden and authentic ledgers. */
public record DrainhouseFeature() implements Feature {
 public static final MapCodec<DrainhouseFeature> CODEC=MapCodec.unit(new DrainhouseFeature());
 public MapCodec<DrainhouseFeature> codec(){return CODEC;}
 /** Nullable test-only observer. Production retains no worlds, histories or counters. */
 @FunctionalInterface interface Observer {void event(WorldGenLevel level,String event,int index,BlockPos origin,BlockPos cell,BlockState state,String reason);}
 static volatile Observer observer;
 private static void observe(WorldGenLevel l,String event,int index,BlockPos origin,BlockPos cell,BlockState state,String reason){var current=observer;if(current!=null)current.event(l,event,index,origin,cell,state,reason);}

 private static boolean writable(WorldGenLevel l,BlockPos p){return !l.isOutsideBuildHeight(p) && (!(l instanceof net.minecraft.server.level.WorldGenRegion region) || region.isWithinWriteZone(p));}
 private static boolean foundation(WorldGenLevel l,BlockPos p){var state=l.getBlockState(p);return state.isSolidRender() && state.getDestroySpeed(l,p)>=0 && !state.hasBlockEntity() && state.getFluidState().isEmpty();}
 /** Narrow three-room embedded station; guarded ordinary terrain only, with real existing cave access. */
 static final int MAX_CARVES=256,MAX_DECORATION=96,MAX_SUPPORTS=32,MAX_CLEARS=MAX_DECORATION;
 private static final Set<Block> ORES=Set.of(Blocks.COAL_ORE,Blocks.DEEPSLATE_COAL_ORE,Blocks.COPPER_ORE,Blocks.DEEPSLATE_COPPER_ORE,Blocks.IRON_ORE,Blocks.DEEPSLATE_IRON_ORE,Blocks.GOLD_ORE,Blocks.DEEPSLATE_GOLD_ORE,Blocks.LAPIS_ORE,Blocks.DEEPSLATE_LAPIS_ORE,Blocks.REDSTONE_ORE,Blocks.DEEPSLATE_REDSTONE_ORE,Blocks.DIAMOND_ORE,Blocks.DEEPSLATE_DIAMOND_ORE,Blocks.EMERALD_ORE,Blocks.DEEPSLATE_EMERALD_ORE);
 private static final net.minecraft.tags.TagKey<Block> EXTRA_ORES=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,net.minecraft.resources.Identifier.parse("c:ores"));
 private static boolean ore(BlockState s){return ORES.contains(s.getBlock()) || s.is(EXTRA_ORES);}
 private static boolean ordinaryRock(BlockState s){return !ore(s) && (s.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD) || s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.CLAY) || s.is(Blocks.DRIPSTONE_BLOCK));}
 private static boolean naturalFoundation(WorldGenLevel l,BlockPos p){return foundation(l,p) && (ordinaryRock(l.getBlockState(p)) || ore(l.getBlockState(p)));}
 private static boolean loadedWritable(WorldGenLevel l,BlockPos p){return l.hasChunkAt(p) && writable(l,p);}
 private static boolean decoration(BlockState s){if(s.is(FungalGarden.GLOWCAP))return s.getValue(GlowcapBlock.AGE)==0;return s.is(Blocks.MOSS_CARPET) || s.is(Blocks.CAVE_VINES) || s.is(Blocks.CAVE_VINES_PLANT) || s.is(Blocks.HANGING_ROOTS) || s.canBeReplaced() || s.is(net.minecraft.tags.BlockTags.REPLACEABLE_BY_TREES);}
 private record Fit(LinkedHashMap<BlockPos,BlockState> writes,String reason){}
 private static Fit refuse(WorldGenLevel l,BlockPos origin,BlockPos cell,BlockState state,String reason){observe(l,"refusal",-1,origin,cell,state,reason);return new Fit(null,reason);}
 /** Fixture diagnostics return only a fixed refusal category; no world/position retention. */
 static String diagnose(WorldGenLevel l,BlockPos p){return analyze(l,p).reason();}
 private static final Set<Block> HARMLESS_MOUTH_PLANTS=Set.of(Blocks.SHORT_GRASS,Blocks.TALL_GRASS,Blocks.FERN,Blocks.LARGE_FERN,Blocks.GLOW_LICHEN,Blocks.VINE,Blocks.CAVE_VINES,Blocks.CAVE_VINES_PLANT,Blocks.HANGING_ROOTS);
 /** Known safe, collision-free headroom only. Unknown empty-shape blocks, fire and cultivated caps refuse. */
 private static boolean mouthAir(WorldGenLevel l,BlockPos p){
  if(!loadedWritable(l,p))return false;var state=l.getBlockState(p);
  return !state.hasBlockEntity() && state.getFluidState().isEmpty() && (state.isAir() || HARMLESS_MOUTH_PLANTS.contains(state.getBlock())) && state.getCollisionShape(l,p).isEmpty();
 }
 /** A three-wide actual two-block-high approach with unchanged dry natural support admission. */
 static boolean dryMouthAt(WorldGenLevel l,BlockPos p,int acrossX,int acrossZ){
  if(Math.abs(acrossX)+Math.abs(acrossZ)!=1)throw new IllegalArgumentException("Cave mouth requires one horizontal width axis");
  for(int d=-1;d<=1;d++){
   var at=p.offset(d*acrossX,0,d*acrossZ);
   for(int y=0;y<2;y++)if(!mouthAir(l,at.above(y)))return false;
   var foot=at.below();if(!loadedWritable(l,foot))return false;
   if(!naturalFoundation(l,foot)){
    var lower=foot.below();if(!l.isEmptyBlock(foot) || !l.getFluidState(foot).isEmpty() || !loadedWritable(l,lower) || !naturalFoundation(l,lower))return false;
   }
  }
  return true;
 }
 private static final int[][] MOUTHS={{-10,0,0,1},{-6,-4,1,0},{0,4,1,0},{10,0,0,1}};
 private static boolean caveMouth(WorldGenLevel l,BlockPos p){for(var m:MOUTHS)if(dryMouthAt(l,p.offset(m[0],0,m[1]),m[2],m[3]))return true;return false;}
 /** Select one real dry approach with an ore-free internal three-wide two-high doorway. */
 private static int primaryMouth(WorldGenLevel l,BlockPos p){
  for(int i=0;i<MOUTHS.length;i++){var m=MOUTHS[i];if(!dryMouthAt(l,p.offset(m[0],0,m[1]),m[2],m[3]))continue;
   boolean clear=true;int x=m[0]-Integer.signum(m[0])*(m[2]==0?1:0),z=m[1]-Integer.signum(m[1])*(m[3]==0?1:0);
   for(int d=-1;d<=1 && clear;d++)for(int y=0;y<2;y++){var at=p.offset(x+d*m[2],y,z+d*m[3]);if(!loadedWritable(l,at) || ore(l.getBlockState(at))){clear=false;break;}}
   if(clear)return i;
  }return -1;
 }
 /** Only unused outer door AIR cells, never any interior passage or functional block. */
 private static boolean unusedDoor(BlockPos p,BlockPos at,int primary){
  int y=at.getY()-p.getY();if(y<0 || y>2)return false;int x=at.getX()-p.getX(),z=at.getZ()-p.getZ();
  for(int i=0;i<MOUTHS.length;i++){if(i==primary)continue;var m=MOUTHS[i];int dx=m[0]-Integer.signum(m[0])*(m[2]==0?1:0),dz=m[1]-Integer.signum(m[1])*(m[3]==0?1:0);
   if(m[2]==0?x==dx && Math.abs(z-dz)<=1:z==dz && Math.abs(x-dx)<=1)return true;
  }return false;
 }
 private static Fit analyze(WorldGenLevel l,BlockPos p){
  if(!l.getLevel().dimension().equals(net.minecraft.world.level.Level.OVERWORLD))return refuse(l,p,p,null,"dimension");
  if(!caveMouth(l,p))return refuse(l,p,p,null,"no_existing_cave_mouth");
  int primary=primaryMouth(l,p);if(primary<0)return refuse(l,p,p,null,"ore_in_required_opening");
  var blueprint=plan(p);var writes=new LinkedHashMap<BlockPos,BlockState>();int carved=0,decor=0,supports=0;
  for(int x=-9;x<=9;x++)for(int z=-3;z<=3;z++){
   var foot=p.offset(x,-1,z);if(!loadedWritable(l,foot))return refuse(l,p,foot,null,"unloaded_floor");
   if(!naturalFoundation(l,foot)){
    if(!l.isEmptyBlock(foot) || !l.getFluidState(foot).isEmpty())return refuse(l,p,foot,l.getBlockState(foot),"unsafe_or_wet_floor");
    var lower=foot.below();if(!loadedWritable(l,lower))return refuse(l,p,lower,null,"unloaded_support");
    if(!naturalFoundation(l,lower)){
     var base=lower.below();if(!l.isEmptyBlock(lower) || !l.getFluidState(lower).isEmpty() || !loadedWritable(l,base) || !naturalFoundation(l,base))return refuse(l,p,lower,l.getBlockState(lower),"missing_dry_support");
     if(++supports>MAX_SUPPORTS)return refuse(l,p,lower,l.getBlockState(lower),"support_budget");writes.put(lower.immutable(),Blocks.DEEPSLATE_BRICKS.defaultBlockState());
    }
   }
   for(int y=0;y<=4;y++){
    var cell=p.offset(x,y,z);if(!loadedWritable(l,cell))return refuse(l,p,cell,null,"unloaded_interior");BlockState old=l.getBlockState(cell),desired=blueprint.get(cell);
    if(old.hasBlockEntity())return refuse(l,p,cell,old,"block_entity");if(!old.getFluidState().isEmpty())return refuse(l,p,cell,old,"fluid");
    if(ore(old)){
     if(desired!=null && !desired.hasBlockEntity() && foundation(l,cell) && (desired.isSolidRender() || desired.isAir() && unusedDoor(p,cell,primary))){writes.put(cell.immutable(),old);continue;}
     return refuse(l,p,cell,old,"ore_in_required_opening");
    }
    if(old.isAir())continue;
    // Actual dry breakable solid gravel may become the rigid roof, only at the133 planned ceiling cells.
    if(y==4){if(!naturalFoundation(l,cell) && !(old.is(Blocks.GRAVEL) && foundation(l,cell)))return refuse(l,p,cell,old,"unsafe_roof");continue;}
    if(old.getDestroySpeed(l,cell)<0)return refuse(l,p,cell,old,"unbreakable");
    if(ordinaryRock(old)){if(desired.isAir() && ++carved>MAX_CARVES)return refuse(l,p,cell,old,"carve_budget");}
    else if(decoration(old)){if(++decor>MAX_DECORATION)return refuse(l,p,cell,old,"decoration_budget");}
    else return refuse(l,p,cell,old,"unsafe_obstruction");
   }
  }
  // The gutter sits at the north floor edge: explicit outer side and bottom lining are mandatory.
  // These ten cells participate in the same exact original-state transaction, never an untracked write.
  for(int x=-2;x<=2;x++)for(var liner:List.of(p.offset(x,-1,-4),p.offset(x,-2,-3))){
   if(!loadedWritable(l,liner))return refuse(l,p,liner,null,"unloaded_gutter_liner");
   var old=l.getBlockState(liner);if(old.hasBlockEntity())return refuse(l,p,liner,old,"liner_block_entity");
   if(!old.getFluidState().isEmpty())return refuse(l,p,liner,old,"liner_fluid");
   if(old.getDestroySpeed(l,liner)<0)return refuse(l,p,liner,old,"liner_unbreakable");
   if(!old.isAir() && !ordinaryRock(old) && !ore(old))return refuse(l,p,liner,old,"unsafe_gutter_liner");
  }
  writes.putAll(blueprint);
  // Preserve existing solid ore in solid architecture or unused exterior doorway infill only.
  for(var e:writes.entrySet()){var old=l.getBlockState(e.getKey());if(ore(old)){if(!foundation(l,e.getKey()) || !(e.getValue().isSolidRender() || e.getValue().isAir() && unusedDoor(p,e.getKey(),primary)) || e.getValue().hasBlockEntity() || !e.getValue().getFluidState().isEmpty())return refuse(l,p,e.getKey(),old,"ore_in_required_gutter_or_opening");e.setValue(old);}}
  if(writes.size()>918)return refuse(l,p,p,null,"write_budget");return new Fit(writes,"accepted");
 }
 private static LinkedHashMap<BlockPos,BlockState> fit(WorldGenLevel l,BlockPos p){return analyze(l,p).writes();}
 static boolean room(WorldGenLevel l,BlockPos p){return fit(l,p)!=null;}

 public boolean place(WorldGenLevel l,ChunkGenerator generator,RandomSource random,BlockPos origin){
  if(!l.getLevel().dimension().equals(net.minecraft.world.level.Level.OVERWORLD))return false;
  observe(l,"place",-1,origin,null,null,"");int checked=0;
  for(int col=0;col<12;col++){
   var column=GlowcapFeature.column(l instanceof net.minecraft.server.level.WorldGenRegion,random,origin,col);observe(l,"column",col,origin,column,null,"");
   for(int y=47;y>=Math.max(-64,l.getMinY());y--){
    var p=new BlockPos(column.getX(),y,column.getZ());if(!l.hasChunkAt(p))break;
    if(!l.isEmptyBlock(p) || !GlowcapBlock.footing(l,p) || !GlowcapBlock.structuralCover(l,p))continue;
    var biome=l.getBiome(p);if(l instanceof net.minecraft.server.level.WorldGenRegion && !biome.is(net.minecraft.world.level.biome.Biomes.LUSH_CAVES) && !biome.is(net.minecraft.world.level.biome.Biomes.DRIPSTONE_CAVES))continue;
    observe(l,"candidate",col,origin,p,null,"original-center");
    if(checked>=8){observe(l,"budget",col,origin,p,null,"original-center");return false;}
    checked++;observe(l,"fit",col,origin,p,null,"original-center");
    if(room(l,p)){boolean result=build(l,p);observe(l,"commit",col,origin,p,null,result?"accepted":"refused");return result;}
    if(checked>=8){observe(l,"budget",col,origin,p,null,"before-north-alignment");return false;}
    // Only the observed north-door orientation: prove the sampled real approach, then align at sameY.
    if(!dryMouthAt(l,p,1,0))continue;
    var center=p.offset(6,0,4);observe(l,"candidate",col,origin,center,null,"canonical-north");
    checked++;observe(l,"fit",col,origin,center,null,"canonical-north");
    if(room(l,center)){boolean result=build(l,center);observe(l,"commit",col,origin,center,null,result?"accepted":"refused");return result;}
   }
  }
  return false;
 }
 static final int WRITE_FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS|Block.UPDATE_SKIP_ON_PLACE;
 @FunctionalInterface interface PlacementWriter {boolean set(BlockPos at,BlockState state);}
 private static void put(Map<BlockPos,BlockState> plan,BlockPos at,Block block){plan.put(at.immutable(),block.defaultBlockState());}
 static LinkedHashMap<BlockPos,BlockState> plan(BlockPos p){var plan=new LinkedHashMap<BlockPos,BlockState>();
  for(int x=-9;x<=9;x++)for(int z=-3;z<=3;z++){
   for(int y=0;y<4;y++)put(plan,p.offset(x,y,z),Blocks.AIR);
   put(plan,p.offset(x,-1,z),(x+z)%4==0?Blocks.MOSSY_STONE_BRICKS:Blocks.DEEPSLATE_BRICKS);
   boolean edge=x==-9 || x==9 || Math.abs(z)==3;
   for(int y=0;y<4;y++)if(edge && !((x==-9 && Math.abs(z)<=1 && y<=2) || (z==-3 && x>=-7 && x<=-5 && y<=2) || (z==3 && Math.abs(x)<=1 && y<=2) || (x==9 && Math.abs(z)<=1 && y<=2)))put(plan,p.offset(x,y,z),y==3?Blocks.CHISELED_DEEPSLATE:((x+z+y)%5==0?Blocks.CRACKED_DEEPSLATE_BRICKS:Blocks.DEEPSLATE_BRICKS));
   put(plan,p.offset(x,4,z),Math.abs(z)==0 && x%3==0?Blocks.COPPER_GRATE.waxed().oxidized():Blocks.DEEPSLATE_TILES);
  }
  for(int wall:new int[]{-3,3})for(int z=-2;z<=2;z++)for(int y=0;y<=3;y++)if(Math.abs(z)>1 || y==3)put(plan,p.offset(wall,y,z),y==3?Blocks.CHISELED_STONE_BRICKS:Blocks.MOSSY_STONE_BRICKS);
  // Ribbed threshold, low gutter and a raised restoration alcove give each room a different silhouette.
  for(int x=-8;x<=8;x+=4)for(int z:new int[]{-2,2})for(int y=0;y<=3;y++)put(plan,p.offset(x,y,z),Blocks.STRIPPED_DARK_OAK_LOG);
  // A rigid ribbed vault avoids excavating unused fourth-layer volume. Main rooms retain
  // three-high standing room; the raised alcove retains two-high space above its copper floor.
  // Replace only existing blueprint AIR: existing ribs/partition caps retain their authored material.
  for(int x=-8;x<=8;x++)for(int z=-2;z<=2;z++){var ceiling=p.offset(x,3,z);if(plan.get(ceiling).isAir())put(plan,ceiling,x%3==0?Blocks.CHISELED_DEEPSLATE:Blocks.DEEPSLATE_TILES);}
  for(int x=-2;x<=2;x++){put(plan,p.offset(x,-1,-4),Blocks.DEEPSLATE_BRICKS);put(plan,p.offset(x,-2,-3),Blocks.DEEPSLATE_BRICKS);put(plan,p.offset(x,-1,-3),Blocks.WATER);put(plan,p.offset(x,-1,-2),Blocks.MOSS_BLOCK);put(plan,p.offset(x,0,-3),Blocks.CUT_COPPER.waxed().oxidized());}
  for(int x:new int[]{-1,1})plan.put(p.offset(x,0,-2),FungalGarden.GLOWCAP.defaultBlockState().setValue(GlowcapBlock.AGE,0));
  for(int x=5;x<=7;x++)for(int z=-2;z<=2;z++)put(plan,p.offset(x,0,z),Blocks.CUT_COPPER.waxed().unaffected());
  for(int z=-2;z<=2;z++)plan.put(p.offset(4,0,z),Blocks.CUT_COPPER_SLAB.waxed().unaffected().defaultBlockState().setValue(SlabBlock.TYPE,SlabType.BOTTOM));
  plan.put(p.offset(-6,0,1),DrainhouseContent.MARK.defaultBlockState().setValue(DrainhouseMark.KIND,0));
  plan.put(p.offset(0,0,1),DrainhouseContent.MARK.defaultBlockState().setValue(DrainhouseMark.KIND,1));
  plan.put(p.offset(6,1,1),DrainhouseContent.MARK.defaultBlockState().setValue(DrainhouseMark.KIND,2));
  return plan;
 }
 private static boolean build(WorldGenLevel l,BlockPos p){return commit(l,p,(at,state) -> l.setBlock(at,state,WRITE_FLAGS));}
 /** Package-local writer seam permits one-shot failure evidence; rollback always uses the real world writer. */
 static boolean build(WorldGenLevel l,BlockPos p,PlacementWriter writer){return room(l,p) && commit(l,p,writer);}
 private static boolean commit(WorldGenLevel l,BlockPos p,PlacementWriter writer){
  var plan=fit(l,p);if(plan==null)return false;if(plan.size()>918)throw new IllegalStateException("Drainhouse footprint budget exceeded");
  var originals=new LinkedHashMap<BlockPos,BlockState>();
  for(var at:plan.keySet()){if(!l.hasChunkAt(at) || !writable(l,at) || l.getBlockState(at).hasBlockEntity() || !l.getFluidState(at).isEmpty())return false;originals.put(at,l.getBlockState(at));}
  var touched=new ArrayList<BlockPos>();boolean accepted=false;
  try {
   for(var entry:plan.entrySet()){
    var at=entry.getKey();var desired=entry.getValue();if(originals.get(at).equals(desired))continue;
    // Record before calling: a custom writer can mutate and then report failure.
    touched.add(at);if(!writer.set(at,desired) || !l.getBlockState(at).equals(desired))return false;
   }
   var ledgers=new ArrayList<DrainhouseMarkEntity>(3);
   for(var at:new BlockPos[]{p.offset(-6,0,1),p.offset(0,0,1),p.offset(6,1,1)}){
    if(!(l.getBlockEntity(at) instanceof DrainhouseMarkEntity e) || e.authentic())return false;ledgers.add(e);
   }
   // No usable quest provenance exists until the complete structure and every ledger are verified.
   for(var e:ledgers)e.awaken();accepted=true;return true;
  }finally{
   if(!accepted){
    RuntimeException failed=null;
    for(int i=touched.size()-1;i>=0;i--){var at=touched.get(i);var original=originals.get(at);try{
     if(!l.getBlockState(at).equals(original))l.setBlock(at,original,WRITE_FLAGS);
     if(!l.getBlockState(at).equals(original) || l.getBlockEntity(at)!=null)throw new IllegalStateException("Drainhouse rollback refused at "+at);
    }catch(RuntimeException failure){if(failed==null)failed=failure;else failed.addSuppressed(failure);}}
    if(failed!=null)throw failed;
   }
  }
 }
}
