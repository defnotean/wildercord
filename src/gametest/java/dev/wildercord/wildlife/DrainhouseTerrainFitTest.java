package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
/** Candidate acceptance for exact embedded geometry, not evidence of natural discovery. */
public final class DrainhouseTerrainFitTest implements FabricClientGameTest {
 private static final BlockPos C=new BlockPos(0,30,0),DECOR=C.offset(2,0,2),GAP=C.offset(-5,-1,-2);
 @Override public void runTest(ClientGameTestContext c){try(var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");
  w.getServer().runOnServer(s->{var l=s.overworld();
   reset(l);put(l,DECOR,Blocks.MOSS_CARPET);var rock=C.offset(2,1,1);check(DrainhouseFeature.plan(C).get(rock).isAir(),"Real rock witness occupies a required opening");put(l,rock,Blocks.DRIPSTONE_BLOCK);put(l,GAP,Blocks.AIR);put(l,GAP.below(),Blocks.AIR);
   var before=snapshot(l);check(DrainhouseFeature.room(l,C),"Existing dry mouth plus bounded rock/decoration/support admits");int[] changed={0};
   check(!DrainhouseFeature.build(l,C,(at,state)->{boolean wrote=l.setBlock(at,state,DrainhouseFeature.WRITE_FLAGS);return ++changed[0]!=80 && wrote;}),"Actual changed-write denial rolls back new support, carving and architecture");
   unchanged(l,before);check(changed[0]==80,"Denied transaction stops immediately");
   check(DrainhouseFeature.build(l,C,(at,state)->l.setBlock(at,state,DrainhouseFeature.WRITE_FLAGS)),"Actual embedded three-room architecture commits");
   exact(l,before,Set.of(GAP.below()));check(l.getBlockState(GAP.below()).is(Blocks.DEEPSLATE_BRICKS),"Extra support really fills the lower gap");
   check(l.getBlockState(DECOR).isAir() && l.getBlockState(rock).isAir(),"Actual carpet and ordinary rock are cleared");

   reset(l);put(l,DECOR,Blocks.IRON_ORE);refused(l,"ore_in_required_opening","Ore in an actual walkable opening refuses without removing it");
   reset(l);var floorOre=C.offset(2,-1,2);var platformOre=C.offset(5,0,2);put(l,floorOre,Blocks.IRON_ORE);put(l,platformOre,Blocks.DEEPSLATE_DIAMOND_ORE);
   check(DrainhouseFeature.plan(C).get(floorOre).isSolidRender() && DrainhouseFeature.plan(C).get(platformOre).isSolidRender(),"Both ore witnesses replace only full solid architecture");before=snapshot(l);
   check(DrainhouseFeature.room(l,C) && DrainhouseFeature.build(l,C,(at,state)->l.setBlock(at,state,DrainhouseFeature.WRITE_FLAGS)),"Full solid original ore can remain inside structure");exact(l,before,Set.of());check(l.getBlockState(floorOre).is(Blocks.IRON_ORE) && l.getBlockState(platformOre).is(Blocks.DEEPSLATE_DIAMOND_ORE),"Two actual original ore states survive exact commit");
   reset(l);put(l,C.offset(0,-1,-3),Blocks.IRON_ORE);refused(l,"ore_in_required_gutter_or_opening","Ore cannot become the actual water gutter");
   reset(l);l.setBlock(DECOR,FungalGarden.GLOWCAP.defaultBlockState().setValue(GlowcapBlock.AGE,1),DrainhouseFeature.WRITE_FLAGS);refused(l,"unsafe_obstruction","Prepared cultivated Glowcap refuses independent of replaceable tags");
   reset(l);put(l,DECOR,Blocks.CHEST);refused(l,"block_entity","Existing container refuses");
   reset(l);put(l,GAP,Blocks.WATER);refused(l,"unsafe_or_wet_floor","Wet foundation refuses");
   reset(l);put(l,C.above(),Blocks.BEDROCK);refused(l,"unbreakable","Unbreakable interior refuses");
   reset(l);put(l,GAP,Blocks.AIR);put(l,GAP.below(),Blocks.AIR);put(l,GAP.below(2),Blocks.AIR);refused(l,"missing_dry_support","Unsupported depth beyond dry support refuses");
   reset(l);for(var p:List.of(C.offset(-10,0,0),C.offset(-6,0,-4),C.offset(0,0,4),C.offset(10,0,0)))put(l,p,Blocks.STONE);
   refused(l,"no_existing_cave_mouth","No existing three-wide cave entrance refuses before any room carve");

   // Only the north approach is open. Its third block is moss, with real safe vines below it.
   var north=twoHighNorth(l);before=snapshot(l);
   check(DrainhouseFeature.dryMouthAt(l,north,1,0),"Actual three-wide two-high vine approach has unchanged dry clay feet");
   for(int dx=-1;dx<=1;dx++){var at=north.offset(dx,0,0);check(!l.getBlockState(at.above(2)).isAir(),"The third exterior headroom block remains actual solid moss");check(!l.getBlockCollisions(null,new net.minecraft.world.phys.AABB(at.getX()+.2,at.getY(),at.getZ()+.2,at.getX()+.8,at.getY()+1.8,at.getZ()+.8)).iterator().hasNext(),"Actual0.6x1.8 player body clears each two-high mouth column");}
   check(DrainhouseFeature.room(l,C) && DrainhouseFeature.build(l,C,(at,state)->l.setBlock(at,state,DrainhouseFeature.WRITE_FLAGS)),"Real two-high safe cave approach admits complete architecture");exact(l,before,Set.of());
   north=twoHighNorth(l);put(l,north.above(),Blocks.STONE);check(l.getBlockCollisions(null,new net.minecraft.world.phys.AABB(north.getX()+.2,north.getY(),north.getZ()+.2,north.getX()+.8,north.getY()+1.8,north.getZ()+.8)).iterator().hasNext(),"Actual stone blocks the player body");refused(l,"no_existing_cave_mouth","Blocked actual player headroom refuses");
   north=twoHighNorth(l);put(l,north.above(),Blocks.WATER);check(!l.getFluidState(north.above()).isEmpty(),"Refusal contains actual water");refused(l,"no_existing_cave_mouth","Wet actual headroom refuses");
   north=twoHighNorth(l);put(l,north,Blocks.FIRE);check(l.getBlockState(north).getCollisionShape(l,north).isEmpty(),"Actual damaging fire has no collision shape");refused(l,"no_existing_cave_mouth","Noncolliding fire cannot admit an unsafe mouth");
   north=twoHighNorth(l);l.setBlock(north,FungalGarden.GLOWCAP.defaultBlockState(),DrainhouseFeature.WRITE_FLAGS);check(l.getBlockState(north).is(FungalGarden.GLOWCAP) && l.getBlockState(north).getCollisionShape(l,north).isEmpty(),"Unknown-to-mouth custom bud is actual and noncolliding");refused(l,"no_existing_cave_mouth","Unknown noncolliding custom block cannot bypass the explicit safe mouth list");

   // The north doorway is the only actual outside approach. Existing west-door ore is infill.
   north=twoHighNorth(l);var unusedOre=C.offset(-9,0,-1);put(l,unusedOre,Blocks.COPPER_ORE);before=snapshot(l);
   check(DrainhouseFeature.room(l,C) && DrainhouseFeature.build(l,C,(at,state)->l.setBlock(at,state,DrainhouseFeature.WRITE_FLAGS)),"Actual unused west-door copper ore preserves while the real primary north entrance remains open");exact(l,before,Set.of());check(l.getBlockState(unusedOre).equals(before.get(unusedOre)),"Exact original copper ore state remains natural infill, without an ore generation write");
   for(int z=-4;z<=0;z++)for(int dx=-1;dx<=1;dx++){var at=C.offset(-6+dx,0,z);check(!l.getBlockCollisions(null,new net.minecraft.world.phys.AABB(at.getX()+.2,at.getY(),at.getZ()+.2,at.getX()+.8,at.getY()+1.8,at.getZ()+.8)).iterator().hasNext(),"Actual primary north entrance and arrival lane preserve three-wide player-body clearance");}
   north=twoHighNorth(l);put(l,C.offset(-6,1,-3),Blocks.COPPER_ORE);refused(l,"ore_in_required_opening","Ore in the only primary internal body doorway cannot be retained or removed");
   north=twoHighNorth(l);put(l,C.offset(-6,0,-2),Blocks.COPPER_ORE);refused(l,"ore_in_required_opening","Ore beyond the outer doorway in the actual arrival lane still refuses");

   // Exact observed roof material is admitted only for the actual authored rigid ceiling.
   reset(l);var gravelRoof=C.offset(-4,4,2);put(l,gravelRoof,Blocks.GRAVEL);before=snapshot(l);check(DrainhouseFeature.room(l,C),"Real dry solid breakable gravel ceiling admits a rigid station roof");
   int expectedChanges=0;for(var e:DrainhouseFeature.plan(C).entrySet())if(!before.get(e.getKey()).equals(e.getValue()))expectedChanges++;final int lastChange=expectedChanges;check(lastChange>80,"Late failure occurs after a substantial real architecture transaction");int[] roofWrites={0};
   check(!DrainhouseFeature.build(l,C,(at,state)->{boolean wrote=l.setBlock(at,state,DrainhouseFeature.WRITE_FLAGS);return ++roofWrites[0]!=lastChange && wrote;}),"Actual final changed-write denial rolls back the full architecture and gravel ceiling");check(roofWrites[0]==lastChange,"The denial occurs at the actual final changed blueprint write");unchanged(l,before);check(l.getBlockState(gravelRoof).equals(before.get(gravelRoof)),"Exact original gravel is restored by real rollback");
   check(DrainhouseFeature.build(l,C,(at,state)->l.setBlock(at,state,DrainhouseFeature.WRITE_FLAGS)),"The same actual gravel ceiling then accepts a complete real transaction");exact(l,before,Set.of());check(l.getBlockState(gravelRoof).equals(DrainhouseFeature.plan(C).get(gravelRoof)) && !l.getBlockState(gravelRoof).is(Blocks.GRAVEL),"Observed gravel is replaced with the exact rigid planned ceiling");
   reset(l);put(l,gravelRoof,Blocks.OAK_PLANKS);refused(l,"unsafe_roof","Unknown ordinary solid breakable roof material still refuses");
   reset(l);put(l,gravelRoof,Blocks.CHEST);refused(l,"block_entity","Roof block entities still refuse before mutation");
   reset(l);put(l,gravelRoof,Blocks.WATER);refused(l,"fluid","Actual ceiling fluid still refuses before mutation");
   reset(l);put(l,gravelRoof,Blocks.BEDROCK);refused(l,"unsafe_roof","Unbreakable ceiling still refuses despite being solid and dry");
   reset(l);put(l,DECOR,Blocks.GRAVEL);refused(l,"unsafe_obstruction","The roof-only gravel exception does not grant interior carving");
   reset(l);put(l,GAP,Blocks.GRAVEL);refused(l,"unsafe_or_wet_floor","The roof-only gravel exception does not grant new foundation material");

   reset(l);BlockPos northLiner=C.offset(0,-1,-4),bottomLiner=C.offset(0,-2,-3);put(l,northLiner,Blocks.CHEST);refused(l,"liner_block_entity","Actual outer gutter liner container refuses");
   reset(l);put(l,bottomLiner,Blocks.WATER);refused(l,"liner_fluid","Actual lower gutter liner fluid refuses");
   reset(l);put(l,northLiner,Blocks.BEDROCK);refused(l,"liner_unbreakable","Actual gutter liner bedrock refuses");
   reset(l);put(l,northLiner,Blocks.IRON_ORE);put(l,bottomLiner,Blocks.DEEPSLATE_DIAMOND_ORE);before=snapshot(l);
   check(DrainhouseFeature.build(l,C,(at,state)->l.setBlock(at,state,DrainhouseFeature.WRITE_FLAGS)),"Actual solid liner ores retain dry water containment");exact(l,before,Set.of());

   // Separate decoration and support budgets remain unchanged; prove both boundary sides.
   var feet=new ArrayList<BlockPos>();for(int x=-9;x<=9;x++)for(int z=-3;z<=3;z++)feet.add(C.offset(x,-1,z));
   reset(l);for(int i=0;i<32;i++){put(l,feet.get(i),Blocks.AIR);put(l,feet.get(i).below(),Blocks.AIR);}check(DrainhouseFeature.MAX_SUPPORTS==32 && DrainhouseFeature.room(l,C),"Thirty-two actual extra dry support cells admit");before=snapshot(l);var extra=new HashSet<BlockPos>();for(int i=0;i<32;i++)extra.add(feet.get(i).below());
   check(DrainhouseFeature.build(l,C,(at,state)->l.setBlock(at,state,DrainhouseFeature.WRITE_FLAGS)),"Thirty-two supports actually commit under918 total cells");exact(l,before,extra);for(var p:extra)check(l.getBlockState(p).is(Blocks.DEEPSLATE_BRICKS),"Each admitted dry support is physically placed");
   reset(l);for(int i=0;i<33;i++){put(l,feet.get(i),Blocks.AIR);put(l,feet.get(i).below(),Blocks.AIR);}refused(l,"support_budget","Thirty-third extra support refuses");
   var carpets=new ArrayList<BlockPos>();for(int x=-9;x<=9;x++)for(int z=-3;z<=3;z++)carpets.add(C.offset(x,0,z));
   reset(l);for(int i=0;i<96;i++)put(l,carpets.get(i),Blocks.MOSS_CARPET);check(DrainhouseFeature.MAX_DECORATION==96 && DrainhouseFeature.room(l,C),"Ninety-six actual supported decorations admit");before=snapshot(l);
   check(DrainhouseFeature.build(l,C,(at,state)->l.setBlock(at,state,DrainhouseFeature.WRITE_FLAGS)),"Ninety-six decoration treatment commits exact architecture");exact(l,before,Set.of());
   reset(l);for(int i=0;i<97;i++)put(l,carpets.get(i),Blocks.MOSS_CARPET);refused(l,"decoration_budget","Ninety-seventh decoration refuses");

   // The rigid vault has226 required AIR cells: even a fully embedded ordinary-rock room
   // must fit below the unchanged256 ceiling. A257th AIR carve is impossible for this template.
   var openings=new ArrayList<BlockPos>();for(var e:DrainhouseFeature.plan(C).entrySet())if(e.getValue().isAir())openings.add(e.getKey());
   check(DrainhouseFeature.MAX_CARVES==256 && openings.size()==226 && openings.size()<DrainhouseFeature.MAX_CARVES,"Complete real blueprint contains exactly226 required air carves under the unchanged256 cap");
   reset(l);for(var at:openings)put(l,at,Blocks.STONE);check(DrainhouseFeature.room(l,C),"Every actual required opening can be ordinary rock without exceeding the finite carve limit");before=snapshot(l);int allChanges=0;for(var e:DrainhouseFeature.plan(C).entrySet())if(!before.get(e.getKey()).equals(e.getValue()))allChanges++;final int finalWrite=allChanges;int[] vaultWrites={0};
   check(!DrainhouseFeature.build(l,C,(at,state)->{boolean wrote=l.setBlock(at,state,DrainhouseFeature.WRITE_FLAGS);return ++vaultWrites[0]!=finalWrite && wrote;}),"Late final-write denial restores all226 real rock openings and original vault volume");check(vaultWrites[0]==finalWrite && finalWrite<=918,"Actual full transaction reaches final changed write within918 calls");unchanged(l,before);for(var at:openings)check(l.getBlockState(at).is(Blocks.STONE),"Every original required-opening rock is restored by actual rollback");
   int[] count={0};check(DrainhouseFeature.build(l,C,(at,state)->{count[0]++;return l.setBlock(at,state,DrainhouseFeature.WRITE_FLAGS);}),"Complete actual226-rock excavation and rigid vault commit");check(count[0]<=918,"Real vault writer calls preserve original transaction bound");exact(l,before,Set.of());for(var at:openings)check(l.getBlockState(at).isAir(),"Each of the226 required ordinary rocks becomes its exact blueprint AIR state");
   for(int z=-3;z<=0;z++)for(int dx=-1;dx<=1;dx++){var at=C.offset(-6+dx,0,z);check(!l.getBlockCollisions(null,new net.minecraft.world.phys.AABB(at.getX()+.2,at.getY(),at.getZ()+.2,at.getX()+.8,at.getY()+1.8,at.getZ()+.8)).iterator().hasNext(),"Main room north arrival remains three-wide with actual1.8 standing clearance below the vault");}
   for(int x=5;x<=7;x++){var at=C.offset(x,1,0);check(l.getBlockState(at.below()).is(Blocks.CUT_COPPER.waxed().unaffected()) && !l.getBlockCollisions(null,new net.minecraft.world.phys.AABB(at.getX()+.2,at.getY(),at.getZ()+.2,at.getX()+.8,at.getY()+1.8,at.getZ()+.8)).iterator().hasNext(),"Actual raised copper alcove has a solid floor and full1.8 standing body clearance below rigid vault");check(!l.getBlockState(at.above(2)).isAir(),"Actual alcove ceiling is rigid atY3 rather than unexcavated AIR");}
   reset(l);var vaultOre=C.offset(6,3,0);put(l,vaultOre,Blocks.DEEPSLATE_DIAMOND_ORE);before=snapshot(l);check(DrainhouseFeature.build(l,C,(at,state)->l.setBlock(at,state,DrainhouseFeature.WRITE_FLAGS)),"Existing solid ore in the new rigid vault retains its original exact state");exact(l,before,Set.of());check(l.getBlockState(vaultOre).equals(before.get(vaultOre)),"The vault neither deletes nor produces original ore");
   reset(l);put(l,C.offset(6,2,0),Blocks.DEEPSLATE_DIAMOND_ORE);refused(l,"ore_in_required_opening","Raised alcove headroom remains a required ore-free interior opening");
   // Leave a real successful structure for asynchronous ordinary server fluid ticks.
   reset(l);for(int x=-2;x<=2;x++){put(l,C.offset(x,-2,-3),Blocks.AIR);put(l,C.offset(x,-1,-4),Blocks.AIR);}
   before=snapshot(l);check(DrainhouseFeature.build(l,C,(at,state)->l.setBlock(at,state,DrainhouseFeature.WRITE_FLAGS)),"Explicit liner seals an actual previously open-bottom/open-north gutter");exact(l,before,Set.of());
   for(int x=-2;x<=2;x++)l.scheduleTick(C.offset(x,-1,-3),net.minecraft.world.level.material.Fluids.WATER,1);
   // A separate genuinely unsealed control proves ordinary fluid simulation actually ran.
   var control=C.offset(24,0,24);l.getChunk(1,1);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=-2;y<=1;y++)put(l,control.offset(x,y,z),Blocks.AIR);put(l,control,Blocks.WATER);l.scheduleTick(control,net.minecraft.world.level.material.Fluids.WATER,1);
  });
  c.waitTicks(100);
  w.getServer().runOnServer(s->{var l=s.overworld();
   check(!l.getFluidState(C.offset(24,-1,24)).isEmpty(),"Unsealed control actually flows downward, proving fluid ticks executed");
   for(int x=-2;x<=2;x++){
    check(l.getBlockState(C.offset(x,-1,-3)).is(Blocks.WATER),"All five actual gutter sources remain after ordinary fluid ticks");
    check(l.getBlockState(C.offset(x,-2,-3)).is(Blocks.DEEPSLATE_BRICKS) && l.getBlockState(C.offset(x,-1,-4)).is(Blocks.DEEPSLATE_BRICKS),"Actual bottom and outer north liners remain closed");
   }
   for(int x=-10;x<=10;x++)for(int z=-4;z<=4;z++)for(int y=-3;y<=4;y++)if(!(y==-1 && z==-3 && x>=-2 && x<=2))check(l.getFluidState(C.offset(x,y,z)).isEmpty(),"After actual fluid ticks no water escapes into any witnessed interior, cave mouth or underfloor cell");
  });
 }}
 private static BlockPos twoHighNorth(ServerLevel l){
  reset(l);for(var at:List.of(C.offset(-10,0,0),C.offset(-6,0,-4),C.offset(0,0,4),C.offset(10,0,0)))put(l,at,Blocks.STONE);
  var north=C.offset(-6,0,-4);for(int dx=-1;dx<=1;dx++){var at=north.offset(dx,0,0);put(l,at.below(),Blocks.CLAY);put(l,at,Blocks.AIR);put(l,at.above(2),Blocks.MOSS_BLOCK);put(l,at.above(),Blocks.CAVE_VINES);}return north;
 }
 private static void put(ServerLevel l,BlockPos p,Block b){check(l.setBlock(p,b.defaultBlockState(),DrainhouseFeature.WRITE_FLAGS) || l.getBlockState(p).is(b),"Actual fixture block is installed");}
 private static void reset(ServerLevel l){for(int cx=-1;cx<=1;cx++)for(int cz=-1;cz<=1;cz++)l.getChunk(cx,cz);for(int x=-10;x<=10;x++)for(int z=-4;z<=4;z++)for(int y=-3;y<=4;y++)l.setBlock(C.offset(x,y,z),y<0?Blocks.MOSS_BLOCK.defaultBlockState():y==4?Blocks.GRANITE.defaultBlockState():Blocks.AIR.defaultBlockState(),DrainhouseFeature.WRITE_FLAGS);}
 private static Map<BlockPos,BlockState> snapshot(ServerLevel l){var states=new LinkedHashMap<BlockPos,BlockState>();for(int x=-10;x<=10;x++)for(int z=-4;z<=4;z++)for(int y=-3;y<=4;y++){var at=C.offset(x,y,z);states.put(at,l.getBlockState(at));}return states;}
 private static void unchanged(ServerLevel l,Map<BlockPos,BlockState> before){for(var e:before.entrySet())check(l.getBlockState(e.getKey()).equals(e.getValue()),"Refusal or rollback preserves every original footprint/support/mouth cell");for(var p:DrainhouseFeature.plan(C).keySet())check(!(l.getBlockEntity(p) instanceof DrainhouseMarkEntity),"No partial quest ledger remains");}
 private static void refused(ServerLevel l,String reason,String why){var before=snapshot(l);int[] writes={0};check(DrainhouseFeature.diagnose(l,C).equals(reason) && !DrainhouseFeature.room(l,C),why+" (actual refusal category)");check(!DrainhouseFeature.build(l,C,(at,state)->{writes[0]++;return l.setBlock(at,state,DrainhouseFeature.WRITE_FLAGS);}) && writes[0]==0,why+" (zero actual writer calls)");unchanged(l,before);}
 private static void exact(ServerLevel l,Map<BlockPos,BlockState> before,Set<BlockPos> supports){var plan=DrainhouseFeature.plan(C);check(plan.size()+supports.size()<=918,"Every planned cell plus extra support retains918 unique-write limit");for(var e:plan.entrySet()){var old=before.get(e.getKey());boolean preservedOre=old.is(Blocks.IRON_ORE)||old.is(Blocks.DEEPSLATE_DIAMOND_ORE)||old.is(Blocks.COPPER_ORE);check(l.getBlockState(e.getKey()).equals(preservedOre?old:e.getValue()),"Every actual full plan cell matches its state including original preserved ore");}for(var e:before.entrySet())if(!plan.containsKey(e.getKey()) && !supports.contains(e.getKey()))check(l.getBlockState(e.getKey()).equals(e.getValue()),"Unplanned external mouth and deep support remain unchanged");for(var at:List.of(C.offset(-6,0,1),C.offset(0,0,1),C.offset(6,1,1)))check(l.getBlockEntity(at) instanceof DrainhouseMarkEntity e && e.authentic(),"All three actual ledger entities authenticate only on full commit");}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
