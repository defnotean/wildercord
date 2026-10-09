package dev.wildercord.wildlife;
import dev.wildercord.cast.*;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import java.util.*;
/** Actual client construction, amphibian routes, finite rests, wake conditions and saved habitat. */
public final class ReedRefugeTest implements FabricClientGameTest {
 private static final BlockPos ROOF=new BlockPos(3,101,3),DRY=new BlockPos(-4,102,-1);
 private static LanternNewt first,second,third,surfaceVisitor,ordinarySwimmer,routeVisitor,walker,stuckVisitor;private static UUID saved;private static long firstRest,secondRest,savedRest;
 @Override public void runTest(ClientGameTestContext c) {
  EcologyReturnProbeChecks.verify();
  NewtBlockedTimeoutProbeChecks.verify();
  TestWorldSave save;
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(30);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");w.getServer().runCommand("weather clear");
   w.getServer().runOnServer(s -> {
    var l=s.overworld();dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.wildlife.ReedRefugeTest\",\"seed\":\"{}\"}",l.getSeed());for(int x=-12;x<=12;x++)for(int z=-8;z<=12;z++) {l.setBlock(new BlockPos(x,100,z),Blocks.DIRT.defaultBlockState(),2);l.setBlock(new BlockPos(x,101,z),x>=-5 && x<=7 && z>=0 && z<=8?Blocks.WATER.defaultBlockState():Blocks.GRASS_BLOCK.defaultBlockState(),2);l.setBlock(new BlockPos(x,102,z),Blocks.AIR.defaultBlockState(),2);}
    for(var row:List.of(List.of(Items.BAMBOO,Items.BAMBOO,Items.BAMBOO,WetlandGarden.FLOSS,Items.STRING,Items.SEAGRASS,WetlandShelters.REFUGE_ITEM),List.of(Items.BOOK,WetlandGarden.FLOSS,Items.SEAGRASS,WetlandShelters.NOTES))) {
     var input=CraftingInput.of(3,2,java.util.stream.IntStream.range(0,6).mapToObj(i -> i<row.size()-1?new ItemStack(row.get(i)):ItemStack.EMPTY).toList());var r=s.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,l);check(r.isPresent() && r.get().value().assemble(input).is(row.getLast()),"Native refuge/notes recipe");
    }
    p(s).setGameMode(GameType.SURVIVAL);p(s).setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(WetlandShelters.REFUGE_ITEM,2));aim(s,new Vec3(3.5,101,3.5),3);
   });c.waitTicks(5);place(c,ROOF.below());c.waitTicks(5);
   w.getServer().runOnServer(s -> {check(s.overworld().getBlockState(ROOF).is(WetlandShelters.REFUGE) && s.overworld().getBlockState(ROOF).getValue(BlockStateProperties.WATERLOGGED) && p(s).getMainHandItem().getCount()==1,"Actual underwater placement retains water and consumes one roof");aim(s,Vec3.atCenterOf(DRY),2.5);});c.waitTicks(5);place(c,DRY.below());c.waitTicks(5);
   w.getServer().runOnServer(s -> {
    check(s.overworld().getBlockState(DRY).is(WetlandShelters.REFUGE) && !s.overworld().getBlockState(DRY).getValue(BlockStateProperties.WATERLOGGED) && p(s).getMainHandItem().isEmpty(),"Dry placement stays dry and consumes final roof");
    check(s.overworld().getBlockEntity(ROOF)==null,"Habitat has no ticking block entity");p(s).setGameMode(GameType.CREATIVE);p(s).setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);aim(s,new Vec3(3.5,101.3,3.5),3);
    s.overworld().setBlock(new BlockPos(2,101,3),Blocks.GLASS.defaultBlockState(),2);first=spawn(s,.5,3.5);
   });
   awaitRest(c,w,()->first,"Newt swims around a separate obstacle into a real waterlogged roof",true);
   w.getServer().runOnServer(s -> {check(first.getX()>3.2 && first.getZ()>3.2 && first.getZ()<3.8,"Settled body is actually beneath roof");firstRest=first.refugeReady();check(first.getHealth()==10 && first.pearlReady()==0 && pearls(s)==0,"Rest heals nothing, grants nothing and changes no pearl clock");second=spawn(s,.5,4.5);aim(s,first.position(),3);});c.waitTicks(16);int firstId=w.getServer().computeOnServer(srv -> first.getId());check(c.computeOnClient(mc -> ((LanternNewt)mc.level.getEntity(firstId)).rest>.9F),"Actual resting pose blends on synchronized client");shot(c,"reed_refuge_sleeping_newt");c.waitTicks(30);
   w.getServer().runOnServer(s -> {check(first.resting() && !second.resting(),"A roof holds one resting visitor: first="+first.resting()+" at "+first.position()+", second="+second.resting()+" at "+second.position());});c.waitTicks(110);
   w.getServer().runOnServer(s -> {check(!first.resting() && first.refugeReady()==firstRest && first.pearlReady()==0,"Rest ends naturally without renewing its deadline");first.setNoAi(true);first.teleportTo(-3,101.1,6);s.overworld().removeBlock(new BlockPos(2,101,3),false);});
   awaitRest(c,w,()->second,"Other newt can use the vacated roof");
   w.getServer().runOnServer(s -> {secondRest=second.refugeReady();p(s).setGameMode(GameType.SURVIVAL);p(s).setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SEAGRASS,3));aim(s,second.position(),2.5);});c.waitTicks(4);feed(c,w,()->second);c.waitTicks(5);
   w.getServer().runOnServer(s -> {check(!second.resting() && second.refugeReady()==secondRest && second.pearlReady()>s.overworld().getGameTime() && p(s).getMainHandItem().getCount()==2 && pearls(s)==1,"Actual feeding wakes animal, spends one plant, gives ordinary pearl and preserves rest");second.setNoAi(true);second.teleportTo(-3,101.1,5);p(s).setGameMode(GameType.CREATIVE);p(s).setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);third=spawn(s,.5,3.5);});
   w.getServer().runCommand("time set 18000");c.waitTicks(100);check(w.getServer().computeOnServer(s -> !third.resting() && third.refugeReady()==0),"Clear night is for active newts, not refuge rests");
   w.getServer().runCommand("weather rain");awaitRest(c,w,()->third,"Rainy night leads to refuge");w.getServer().runOnServer(s -> {savedRest=third.refugeReady();aim(s,third.position(),3);});c.waitTicks(16);shot(c,"reed_refuge_rainy_night");
   w.getServer().runOnServer(s -> {var plan=SpellCompiler.compile(List.of(Runes.TOUCH,Runes.TIDEBREATH));CastEngine.onHit(new Cast(p(s)),plan.root().groups.getFirst(),new Cast.Hit(List.of(third),third.position(),new Vec3(0,0,1),p(s).position(),third.blockPosition(),Direction.UP,false),null);});c.waitTicks(5);
   w.getServer().runOnServer(s -> {check(!third.resting() && third.response()>0 && third.refugeReady()==savedRest && third.pearlReady()==0,"Actual spell impact wakes without renewing rest or yielding resources");saved=third.getUUID();p(s).setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(WetlandShelters.NOTES));var book=p(s).getMainHandItem().get(DataComponents.WRITTEN_BOOK_CONTENT);check(book!=null && book.pages().size()==3,"Lore book has three native pages");});c.waitTicks(5);c.runOnClient(mc -> mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND));c.waitTicks(5);check(c.computeOnClient(mc -> mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.BookViewScreen),"Actual book use opens native reader");shot(c,"reed_refuge_field_notes");c.runOnClient(mc -> mc.gui.setScreen(null));save=w.getWorldSave();
  }
  try(var w=save.open()) {c.waitTicks(35);w.getServer().runOnServer(s -> {dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.wildlife.ReedRefugeTest#reopen\",\"seed\":\"{}\"}",s.overworld().getSeed());third=(LanternNewt)s.overworld().getEntity(saved);check(third!=null && third.refugeReady()==savedRest && !third.resting(),"Full restart preserves exact refuge deadline without a phantom rest pose");check(s.overworld().getBlockState(ROOF).getValue(BlockStateProperties.WATERLOGGED) && !s.overworld().getBlockState(DRY).getValue(BlockStateProperties.WATERLOGGED),"Wet and dry habitat placements survive restart");check(third.getHealth()==10 && third.pearlReady()==0,"Restart grants no shelter reward or heal");});c.waitTicks(50);check(w.getServer().computeOnServer(s -> !third.resting() && third.refugeReady()==savedRest),"Restart cannot bypass refuge rest");
   w.getServer().runOnServer(s -> {third=spawn(s,3.5,3.5);});awaitRest(c,w,()->third,"New visitor settles before danger check");
   w.getServer().runOnServer(s -> {savedRest=third.refugeReady();third.hurtServer(s.overworld(),s.overworld().damageSources().generic(),1);});c.waitTicks(5);
   check(w.getServer().computeOnServer(s -> !third.resting() && third.frightened() && third.refugeReady()==savedRest && third.getHealth()==9),"Damage wakes immediately, preserves rest and provides no healing");
   w.getServer().runOnServer(s -> {third.setNoAi(true);third.teleportTo(-3,101.1,4);third=spawn(s,3.5,3.5);});awaitRest(c,w,()->third,"Another visitor settles before roof removal");
   w.getServer().runOnServer(s -> {savedRest=third.refugeReady();s.overworld().destroyBlock(ROOF,true);});c.waitTicks(5);
   check(w.getServer().computeOnServer(s -> !third.resting() && third.refugeReady()==savedRest && s.overworld().getEntitiesOfClass(ItemEntity.class,new AABB(ROOF).inflate(2),e -> e.getItem().is(WetlandShelters.REFUGE_ITEM)).stream().mapToInt(e -> e.getItem().getCount()).sum()==1),"Breaking roof wakes visitor and drops exactly one roof");
   verifyNavigationCases(c,w);
  }
 }
 private static LanternNewt spawn(MinecraftServer s,double x,double z) {var n=WetlandContent.NEWT.create(s.overworld(),EntitySpawnReason.COMMAND);n.snapTo(x,101.1,z,0,0);n.getRandom().setSeed(314);s.overworld().addFreshEntity(n);return n;}
 private static void awaitRest(ClientGameTestContext c,TestSingleplayerContext w,java.util.function.Supplier<LanternNewt> target,String why) {
  awaitRest(c,w,target,why,false);
 }
 private static void awaitRest(ClientGameTestContext c,TestSingleplayerContext w,java.util.function.Supplier<LanternNewt> target,String why,boolean glassDetour) {
  var probe=w.getServer().computeOnServer(s->EcologyReturnProbe.begin(EcologyReturnProbe.REED,s.overworld(),target.get()));
  var witness=new RefugeRouteWitness();
  try{for(int i=0;i<100;i++) {c.waitTicks(5);if(w.getServer().computeOnServer(s -> {var n=target.get();witness.observe(n,glassDetour);return n.resting();})) {
    check(!glassDetour || witness.aquaticDetour && witness.bodyPassedGlass,"Native aquatic route and actual body both pass around the unchanged glass");
    check(!glassDetour || witness.descendingPitch && witness.descendingBody,"Actual refuge swimmer retains native downward pitch and descends before arrival");return;
   }}
   throw new AssertionError(w.getServer().computeOnServer(s -> why+": "+EcologyReturnProbe.body(target.get())));
  }finally{probe.close();}
 }

 /** Observes paths the real goal already requested; never creates or changes a route. */
 private static final class RefugeRouteWitness {
  boolean aquaticDetour,bodyPassedGlass,descendingPitch,descendingBody;
  void observe(LanternNewt n,boolean glassDetour) {
   check(n.level().noCollision(n,n.getBoundingBox()),"Swimming body never overlaps the real obstacle or refuge shape");
   var path=n.getNavigation().getPath();
   if(path!=null && path.getTarget().equals(ROOF)) {
    boolean around=false;
    for(int i=0;i<path.getNodeCount();i++) {var node=path.getNode(i);var at=new BlockPos(node.x,node.y,node.z);
     check(n.level().getFluidState(at).is(net.minecraft.tags.FluidTags.WATER),"Refuge's native path contains only aquatic nodes: "+EcologyReturnProbe.path(path));
     around|=node.z!=ROOF.getZ();
    }
    aquaticDetour|=around;
    descendingPitch|=n.getXRot()>5 && n.yya<0;
    descendingBody|=n.getDeltaMovement().y<-.003;
   }
   var box=n.getBoundingBox();
   if(glassDetour) {
    check(n.level().getBlockState(new BlockPos(2,101,3)).is(Blocks.GLASS),"Separate glass obstacle remains present throughout the detour");
    bodyPassedGlass|=n.getX()>2 && n.getX()<3 && (box.maxZ<=3 || box.minZ>=4);
   }
   if(n.resting()) {
    check(n.beneathRefuge(ROOF),"Rest begins only after physical arrival beneath the waterlogged roof");
    check(!((NewtPathNavigation)n.getNavigation()).followingRefuge() && n.getXRot()==0,"Ordinary pitch reset resumes after actual refuge arrival");
   }
  }
 }

 private static void verifyNavigationCases(ClientGameTestContext c,TestSingleplayerContext w) {
  w.getServer().runCommand("weather clear");w.getServer().runCommand("time set 6000");
  w.getServer().runOnServer(s -> {
   s.overworld().setBlock(ROOF,WetlandShelters.REFUGE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,true),2);
   surfaceVisitor=WetlandContent.NEWT.create(s.overworld(),EntitySpawnReason.COMMAND);surfaceVisitor.snapTo(.5,101.6,3.5,0,0);surfaceVisitor.getRandom().setSeed(314);s.overworld().addFreshEntity(surfaceVisitor);
  });c.waitTicks(1);
  w.getServer().runOnServer(s -> {
   check(surfaceVisitor.isInWater() && surfaceVisitor.getY()>=101.5,"Surface swimmer has wet feet but a dry rounded native start");
   var path=surfaceVisitor.getNavigation().createPath(ROOF,0);
   check(path!=null && path.canReach() && path.getNode(0).y==101,"Surface-height swimmer's native refuge search starts in its actual water cell");
   for(int i=0;i<path.getNodeCount();i++) {var node=path.getNode(i);check(s.overworld().getFluidState(new BlockPos(node.x,node.y,node.z)).is(net.minecraft.tags.FluidTags.WATER),"Surface route retains aquatic candidate filtering");}
   // Separate native control contract: this actor is discarded before the real
   // approach witness. No controller calls are injected into natural arrival.
   check(surfaceVisitor.getNavigation().moveTo(path,.7),"Native surface path is accepted for the look-control contract");
   surfaceVisitor.getNavigation().tick();
   check(((NewtPathNavigation)surfaceVisitor.getNavigation()).followingRefuge(),"Actual surface-refuge route owns swimming control");
   checkNativeSwimControl(surfaceVisitor,false,"active surface-refuge route");
   surfaceVisitor.setXRot(30);surfaceVisitor.getLookControl().setLookAt(surfaceVisitor.getX()+2,surfaceVisitor.getEyeY()+1,surfaceVisitor.getZ()+2,10,20);surfaceVisitor.getLookControl().tick();
   check(surfaceVisitor.getXRot()==30,"Active refuge navigation retains swimming pitch despite an old look target");
   surfaceVisitor.getNavigation().stop();checkOrdinaryLook(surfaceVisitor,"explicit refuge cancellation");checkNativeSwimControl(surfaceVisitor,true,"cancelled surface-refuge route");
   surfaceVisitor.discard();ordinarySwimmer=spawn(s,.5,3.5);
  });c.waitTicks(1);
  w.getServer().runOnServer(s -> {
   var ordinaryPath=ordinarySwimmer.getNavigation().createPath(new BlockPos(1,101,3),0);
   check(ordinarySwimmer.isInWater() && ordinaryPath!=null && ordinaryPath.canReach() && ordinarySwimmer.getNavigation().moveTo(ordinaryPath,.7),"Native ordinary-water route starts in the original pool");
   ordinarySwimmer.getNavigation().tick();
   check(!ordinarySwimmer.getNavigation().isDone() && !((NewtPathNavigation)ordinarySwimmer.getNavigation()).followingRefuge(),"Ordinary swimming remains outside refuge control");
   checkNativeSwimControl(ordinarySwimmer,true,"ordinary active swimming");
   ordinarySwimmer.discard();routeVisitor=spawn(s,.5,3.5);
  });
  boolean approaching=false;
  for(int i=0;i<500;i++) {c.waitTicks(1);if(w.getServer().computeOnServer(s -> {var path=routeVisitor.getNavigation().getPath();return path!=null && path.getTarget().equals(ROOF) && path.getNextNodeIndex()==path.getNodeCount()-1 && !routeVisitor.beneathRefuge(ROOF);})) {approaching=true;break;}}
  check(approaching,"Ordinary Shelter goal reaches a live final native waypoint before the removal check");
  w.getServer().runOnServer(s -> s.overworld().removeBlock(ROOF,false));
  for(int i=0;i<25;i++) {c.waitTicks(1);w.getServer().runOnServer(s -> {
   var path=routeVisitor.getNavigation().getPath();
   check(!routeVisitor.resting() && routeVisitor.refugeReady()==0 && (path==null || !path.getTarget().equals(ROOF)),"Removing a refuge during final approach stops that route without rewarding arrival");
  });}
  w.getServer().runOnServer(s -> {
   var dry=new BlockPos(-8,102,3);
   var reference=new net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation(routeVisitor,s.overworld());
   var configuration=queryConfiguration(reference);var maluses=queryMaluses(routeVisitor);
   check(configuration.equals(queryConfiguration(routeVisitor.getNavigation())),"Reference and actual dry queries begin with identical native search bounds and evaluator flags");
   dryRouteReceipt("before",routeVisitor,dry,reference,null,null);
   var expected=reference.createPath(dry,0);
   dryRouteReceipt("after_reference",routeVisitor,dry,reference,expected,null);
   check(configuration.equals(queryConfiguration(reference)) && configuration.equals(queryConfiguration(routeVisitor.getNavigation())) && maluses.equals(queryMaluses(routeVisitor)),"Reference query preserves the original search configuration and actor maluses");
   var actual=routeVisitor.getNavigation().createPath(dry,0);
   dryRouteReceipt("after_actual",routeVisitor,dry,reference,expected,actual);
   check(configuration.equals(queryConfiguration(reference)) && configuration.equals(queryConfiguration(routeVisitor.getNavigation())) && maluses.equals(queryMaluses(routeVisitor)),"Actual dry query preserves the same search configuration and actor maluses");
   // The bounded vanilla search also returns a partial route here. Compare its
   // complete result, then prove actual dry walking separately with TemptGoal.
   check(sameNativeRoute(expected,actual,dry),"After a refuge query, native and custom dry searches return identical target, nodes, costs, cursor and reachability, including partial paths");
   routeVisitor.setNoAi(true);checkOrdinaryLook(routeVisitor,"removed refuge");checkNativeSwimControl(routeVisitor,true,"removed refuge");
   walker=WetlandContent.NEWT.create(s.overworld(),EntitySpawnReason.COMMAND);walker.snapTo(-8.5,102,1.5,0,0);walker.getRandom().setSeed(314);s.overworld().addFreshEntity(walker);
   p(s).setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SEAGRASS));p(s).teleportTo(s.overworld(),-8.5,102,6.5,Set.<Relative>of(),180,0,false);
  });
  boolean tempted=false;
  for(int i=0;i<100;i++) {c.waitTicks(2);if(w.getServer().computeOnServer(s -> walker.getZ()>3.5 && !walker.isInWater() && walker.getGoalSelector().getAvailableGoals().stream().anyMatch(g -> g.isRunning() && g.getGoal() instanceof net.minecraft.world.entity.ai.goal.TemptGoal))) {tempted=true;break;}}
  check(tempted,"An ordinary TemptGoal still walks the newt across dry land using native amphibious navigation");
  w.getServer().runOnServer(s -> {walker.setNoAi(true);checkNativeSwimControl(walker,true,"actual dry-land TemptGoal");p(s).setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);});
  verifyStuckLookRestoration(c,w);
 }

 private static List<Object> queryConfiguration(net.minecraft.world.entity.ai.navigation.PathNavigation navigation) {
  var evaluator=navigation.getNodeEvaluator();var finder=observedField(navigation,"pathFinder");
  var followRange=((net.minecraft.world.entity.Mob)observedField(navigation,"mob")).getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
  return List.of(followRange,observedField(navigation,"requiredPathLength"),observedField(navigation,"maxVisitedNodesMultiplier"),observedField(finder,"maxVisitedNodes"),
   evaluator.canFloat(),evaluator.canPassDoors(),evaluator.canOpenDoors(),evaluator.canWalkOverFences(),observedField(evaluator,"prefersShallowSwimming"));
 }
 private static List<Float> queryMaluses(LanternNewt n) {
  return List.of(n.getPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER),n.getPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER_BORDER),n.getPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WALKABLE));
 }
 static boolean sameNativeRoute(net.minecraft.world.level.pathfinder.Path expected,net.minecraft.world.level.pathfinder.Path actual,BlockPos target) {
  if(expected==null || actual==null || expected.getNodeCount()==0 || !expected.getTarget().equals(target) || !actual.getTarget().equals(target)
    || expected.canReach()!=actual.canReach() || expected.isDone()!=actual.isDone() || expected.getNextNodeIndex()!=actual.getNextNodeIndex() || !actual.sameAs(expected))return false;
  for(int i=0;i<expected.getNodeCount();i++) {var e=expected.getNode(i);var a=actual.getNode(i);if(e.type!=a.type || Float.compare(e.costMalus,a.costMalus)!=0)return false;}
  return true;
 }

 /** Reads the two existing query results and their original configuration; never requests another path. */
 private static void dryRouteReceipt(String phase,LanternNewt n,BlockPos target,net.minecraft.world.entity.ai.navigation.PathNavigation reference,net.minecraft.world.level.pathfinder.Path expected,net.minecraft.world.level.pathfinder.Path actual) {
  try {
  var box=n.getBoundingBox();var rounded=BlockPos.containing(box.minX,box.minY+.5,box.minZ);var feet=BlockPos.containing(box.minX,box.minY,box.minZ);
  System.out.println("NEWT_DRY_ROUTE_QUERY phase="+phase+" "+EcologyReturnProbe.body(n)
   +" target="+target+" targetState="+n.level().getBlockState(target)+" targetFluid="+n.level().getFluidState(target)+" targetBelow="+n.level().getBlockState(target.below())
   +" targetLoaded="+n.level().hasChunkAt(target)+" roundedStart="+rounded+" roundedState="+n.level().getBlockState(rounded)+" roundedFluid="+n.level().getFluidState(rounded)
   +" feetStart="+feet+" feetState="+n.level().getBlockState(feet)+" feetFluid="+n.level().getFluidState(feet)
   +" followRange="+n.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE)
   +" waterMalus="+n.getPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER)+" borderMalus="+n.getPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER_BORDER)+" walkableMalus="+n.getPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WALKABLE)
   +" referenceNavigation="+navigationReceipt(reference)+" actualNavigation="+navigationReceipt(n.getNavigation())
   +" aquaticSearch="+observedField(n.getNavigation(),"aquaticSearch")+" refugeRoute="+observedField(n.getNavigation(),"refugeRoute")
   +" expected="+EcologyReturnProbe.path(expected)+" actual="+EcologyReturnProbe.path(actual)
   +" decisions={expectedPresent="+(expected!=null)+",expectedReachable="+(expected!=null&&expected.canReach())+",actualPresent="+(actual!=null)+",actualReachable="+(actual!=null&&actual.canReach())+",sameNodes="+(expected!=null&&actual!=null&&actual.sameAs(expected))+"}");
  }catch(Throwable failure){System.out.println("NEWT_DRY_ROUTE_QUERY phase="+phase+" observationError="+failure);}
 }
 private static String navigationReceipt(net.minecraft.world.entity.ai.navigation.PathNavigation navigation) {
  var evaluator=navigation.getNodeEvaluator();var finder=observedField(navigation,"pathFinder");
  return "{type="+navigation.getClass().getName()+",target="+navigation.getTargetPos()+",requiredPathLength="+observedField(navigation,"requiredPathLength")+",maxVisitedMultiplier="+observedField(navigation,"maxVisitedNodesMultiplier")+",maxVisitedNodes="+observedField(finder,"maxVisitedNodes")
   +",evaluator="+evaluator.getClass().getName()+",canFloat="+evaluator.canFloat()+",canPassDoors="+evaluator.canPassDoors()+",canOpenDoors="+evaluator.canOpenDoors()+",canWalkOverFences="+evaluator.canWalkOverFences()+",contextReleased="+(observedField(evaluator,"currentContext")==null)+",path="+EcologyReturnProbe.path(navigation.getPath())+"}";
 }
 private static Object observedField(Object target,String name) {
  for(Class<?> type=target.getClass();type!=null;type=type.getSuperclass())try{var field=type.getDeclaredField(name);field.setAccessible(true);return field.get(target);}catch(NoSuchFieldException ignored){}catch(ReflectiveOperationException failure){throw new AssertionError("Cannot read native query field "+name,failure);}
  throw new AssertionError("Missing native query field "+name);
 }

 /** Compares the real restored controller to vanilla on an actor retired from its movement case. */
 private static void checkOrdinaryLook(LanternNewt n,String after) {
  check(!((NewtPathNavigation)n.getNavigation()).followingRefuge(),"Refuge pitch ownership ends after "+after);
  var ordinary=new net.minecraft.world.entity.ai.control.LookControl(n);
  n.setXRot(30);ordinary.setLookAt(n.getX()+2,n.getEyeY()+1,n.getZ()+2,10,20);ordinary.tick();float expected=n.getXRot();
  n.setXRot(30);n.getLookControl().setLookAt(n.getX()+2,n.getEyeY()+1,n.getZ()+2,10,20);n.getLookControl().tick();
  check(n.getXRot()==expected,"Default pitch reset and target-looking behavior resume after "+after);
 }

 /** Compares one control tick only on disposable/retired actors, restoring all inputs afterward. */
 private static void checkNativeSwimControl(LanternNewt n,boolean buoyancy,String scenario) {
  var actual=n.getMoveControl();var reference=new net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl<>(n,85,10,.7F,.65F,buoyancy);
  if(actual.hasWanted())reference.setWantedPosition(actual.getWantedX(),actual.getWantedY(),actual.getWantedZ(),actual.getSpeedModifier());
  var initial=SwimControlState.capture(n);
  try {
   reference.tick();var expected=SwimControlState.capture(n);initial.restore(n);
   actual.tick();var observed=SwimControlState.capture(n);
   check(observed.equals(expected),"Swim control matches native buoyancy="+buoyancy+" for "+scenario+": expected="+expected+", actual="+observed);
  }finally{initial.restore(n);}
 }
 private record SwimControlState(Vec3 position,Vec3 velocity,float speed,float sideways,float vertical,float forward,float pitch,float yaw,float headYaw,float bodyYaw) {
  static SwimControlState capture(LanternNewt n) {return new SwimControlState(n.position(),n.getDeltaMovement(),n.getSpeed(),n.xxa,n.yya,n.zza,n.getXRot(),n.getYRot(),n.yHeadRot,n.yBodyRot);}
  void restore(LanternNewt n) {n.setDeltaMovement(velocity);n.setSpeed(speed);n.xxa=sideways;n.yya=vertical;n.zza=forward;n.setXRot(pitch);n.setYRot(yaw);n.yHeadRot=headYaw;n.yBodyRot=bodyYaw;}
 }

 private static boolean shelterRunning(LanternNewt n) {return n.getGoalSelector().getAvailableGoals().stream().anyMatch(g -> g.isRunning() && g.getGoal().getClass().getSimpleName().equals("Shelter"));}
 private static int shelterTicksLeft(LanternNewt n) {
  var goal=n.getGoalSelector().getAvailableGoals().stream().filter(g -> g.isRunning() && g.getGoal().getClass().getSimpleName().equals("Shelter")).findFirst().orElseThrow().getGoal();
  try{var field=goal.getClass().getDeclaredField("travelLeft");field.setAccessible(true);return field.getInt(goal);}
  catch(ReflectiveOperationException failure){throw new AssertionError("Cannot observe the actual Shelter journey counter",failure);}
 }
 private static void verifyStuckLookRestoration(ClientGameTestContext c,TestSingleplayerContext w) {
  var witness=new BlockedJourneyWitness();
  NewtBlockedTimeoutProbe.Session[] probe={null};
  try {
  w.getServer().runOnServer(s -> {s.overworld().setBlock(ROOF,WetlandShelters.REFUGE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,true),2);stuckVisitor=spawn(s,.5,3.5);});
  boolean started=false;
  for(int i=0;i<500;i++) {c.waitTicks(1);if(w.getServer().computeOnServer(s -> {
   if(!shelterRunning(stuckVisitor) || !((NewtPathNavigation)stuckVisitor.getNavigation()).followingRefuge())return false;
   probe[0]=NewtBlockedTimeoutProbe.begin(stuckVisitor,ROOF,()->pearls(s));return true;
  })) {started=true;break;}}
  check(started,"Ordinary Shelter starts before the separate blocked-journey case");
  var journey=w.getServer().computeOnServer(s -> {
   var at=stuckVisitor.blockPosition();
   // Enclose this later test actor after admission without overlapping its body.
   // The original glass-detour fixture and arrival proof have already completed.
   for(var d:Direction.Plane.HORIZONTAL) {
    witness.observe("before_glass_"+d,stuckVisitor);
    s.overworld().setBlock(at.relative(d),Blocks.GLASS.defaultBlockState(),2);
    witness.observe("after_glass_"+d,stuckVisitor);
   }
   witness.observe("before_glass_above",stuckVisitor);
   s.overworld().setBlock(at.above(),Blocks.GLASS.defaultBlockState(),2);
   witness.observe("after_glass_above",stuckVisitor);
   check(s.overworld().noCollision(stuckVisitor,stuckVisitor.getBoundingBox()),"Stuck case leaves the actual body clear inside its water cell");
   probe[0].arm(at);
   return new int[]{probe[0].originalTick,probe[0].originalRemaining};
  });
  boolean stuck=false,expired=false;
  for(int i=0;i<260;i++) {c.waitTicks(1);var state=w.getServer().computeOnServer(s -> {
   witness.observe("sample",stuckVisitor);
   var navigation=(NewtPathNavigation)stuckVisitor.getNavigation();
   if(navigation.isStuck())check(!navigation.followingRefuge() && stuckVisitor.getXRot()==0,"Native stuck termination immediately restores ordinary pitch reset");
   if(probe[0].timeoutCount()>0 && navigation.isDone())check(!navigation.followingRefuge() && stuckVisitor.getXRot()==0,"Native waypoint timeout restores ordinary pitch reset without requiring displacement-stuck");
   boolean originalExpired=probe[0].sample();
   return new int[]{navigation.isStuck()?1:0,originalExpired?0:1,stuckVisitor.tickCount-journey[0]};
  });stuck|=state[0]!=0;if(state[1]==0) {
   // Record the first stop, including an early water/availability exit, against
   // the actual remaining counter; selectors check every other tick.
   check(state[2]>=journey[1] && state[2]<=journey[1]+2,"Blocked Shelter first stops when its original journey counter expires: remaining="+journey[1]+", observed age="+state[2]);expired=true;break;
  }}
  witness.result(stuck,expired,journey);
  w.getServer().runOnServer(s -> probe[0].verify());
  w.getServer().runOnServer(s -> {check(!stuckVisitor.resting() && stuckVisitor.refugeReady()==0,"Blocked approach never counts as arrival");stuckVisitor.setNoAi(true);checkOrdinaryLook(stuckVisitor,"native blocked termination and journey expiry");checkNativeSwimControl(stuckVisitor,true,"native blocked termination and journey expiry");});
  }finally{if(probe[0]!=null)probe[0].close();witness.close();}
 }

 /** Local bounded reads of this actor's existing route and clocks; never requests a path. */
 private static final class BlockedJourneyWitness implements AutoCloseable {
  private static final int LIMIT=32;
  private final List<String> first=new ArrayList<>(),milestones=new ArrayList<>();
  private final ArrayDeque<String> last=new ArrayDeque<>();
  private int observed,changed,errors,sinkFailures;private String previous="",result="event=result reached=false";
  void observe(String phase,LanternNewt n) {
   try {
    var navigation=(NewtPathNavigation)n.getNavigation();var path=navigation.getPath();
    var shelter=n.getGoalSelector().getAvailableGoals().stream().filter(g -> g.getGoal().getClass().getSimpleName().equals("Shelter")).findFirst().orElseThrow();var goal=shelter.getGoal();
    var nativeTick=observedField(navigation,"tick");var stuckCheck=observedField(navigation,"lastStuckCheck");var recompute=observedField(navigation,"timeLastRecompute");
    String route=EcologyReturnProbe.path(path);
    String key=route+" "+navigation.isStuck()+" "+navigation.followingRefuge()+" "+shelter.isRunning()+" "+stuckCheck+" "+recompute;
    var move=n.getMoveControl();
    String row="event="+phase+" now="+n.level().getGameTime()+" tick="+n.tickCount+" actor="+n.getUUID()+" body="+n.position()+" box="+n.getBoundingBox()+" delta="+n.getDeltaMovement()
     +" ground="+n.onGround()+" water="+n.isInWater()+" horizontalCollision="+n.horizontalCollision+" verticalCollision="+n.verticalCollision+" resting="+n.resting()+" refugeReady="+n.refugeReady()+" pitch="+n.getXRot()
     +" movementInputs={x="+n.xxa+",y="+n.yya+",z="+n.zza+",speed="+n.getSpeed()+"} moveControl={operation="+observedField(move,"operation")+",wanted="+move.hasWanted()+",x="+move.getWantedX()+",y="+move.getWantedY()+",z="+move.getWantedZ()+",speed="+move.getSpeedModifier()+"}"
     +" navigationDone="+navigation.isDone()+" nativePath="+route+" routePartial="+(path!=null&&!path.canReach())
     +" isStuck="+navigation.isStuck()+" followingRefuge="+navigation.followingRefuge()+" refugeRoute="+observedField(navigation,"refugeRoute")
     +" shelterRunning="+shelter.isRunning()+" travelLeft="+observedField(goal,"travelLeft")+" settled="+observedField(goal,"settled")+" retries="+observedField(goal,"retries")+" roof="+observedField(goal,"roof")
     +" nativeClocks={tick="+nativeTick+",lastStuckCheck="+stuckCheck+",lastStuckCheckPos="+observedField(navigation,"lastStuckCheckPos")
     +",timeoutCachedNode="+observedField(navigation,"timeoutCachedNode")+",timeoutTimer="+observedField(navigation,"timeoutTimer")+",timeoutLimit="+observedField(navigation,"timeoutLimit")
     +",lastTimeoutCheck="+observedField(navigation,"lastTimeoutCheck")+",timeLastRecompute="+recompute+",hasDelayedRecomputation="+observedField(navigation,"hasDelayedRecomputation")+"}";
    if(!phase.equals("sample")||!key.equals(previous)){changed++;if(milestones.size()<LIMIT)milestones.add(row);}
    previous=key;observed++;if(first.size()<LIMIT)first.add(row);else{if(last.size()==LIMIT)last.removeFirst();last.addLast(row);}
   }catch(Throwable failure){errors++;if(milestones.size()<LIMIT)milestones.add("event="+phase+" observationError="+failure);}
  }
  void result(boolean stuck,boolean expired,int[] journey) {result="event=result reached=true stuck="+stuck+" expired="+expired+" enclosureTick="+journey[0]+" originalRemaining="+journey[1];}
  private void send(String row) {try{System.out.println("NEWT_BLOCKED_JOURNEY "+row);}catch(Throwable ignored){sinkFailures++;}}
  @Override public void close() {
   try{milestones.forEach(this::send);first.forEach(this::send);last.forEach(this::send);send(result);send("event=summary observed="+observed+" retained="+(first.size()+last.size())+" omitted="+Math.max(0,observed-first.size()-last.size())+" milestones="+changed+" retainedMilestones="+milestones.size()+" observationErrors="+errors+" sinkFailures="+sinkFailures);}
   finally{first.clear();last.clear();milestones.clear();}
  }
 }

 private static void feed(ClientGameTestContext c,TestSingleplayerContext w,java.util.function.Supplier<LanternNewt> n) {int id=w.getServer().computeOnServer(s -> n.get().getId());c.runOnClient(mc -> {var e=mc.level.getEntity(id);mc.gameMode.interact(mc.player,e,new EntityHitResult(e,e.getBoundingBox().getCenter()),InteractionHand.MAIN_HAND);});}
 private static void place(ClientGameTestContext c,BlockPos ground) {c.runOnClient(mc -> mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(ground),Direction.UP,ground,false)));}
 private static int pearls(MinecraftServer s) {return s.overworld().getEntitiesOfClass(ItemEntity.class,new AABB(-12,100,-8,12,106,12),e -> e.getItem().is(WetlandContent.DUSK_PEARL)).stream().mapToInt(e -> e.getItem().getCount()).sum()+java.util.stream.IntStream.range(0,p(s).getInventory().getContainerSize()).map(i -> p(s).getInventory().getItem(i).is(WetlandContent.DUSK_PEARL)?p(s).getInventory().getItem(i).getCount():0).sum();}
 private static ServerPlayer p(MinecraftServer s) {return s.getPlayerList().getPlayers().getFirst();}
 private static void aim(MinecraftServer s,Vec3 target,double distance) {var p=p(s);if(p.getAbilities().mayfly) {p.getAbilities().flying=true;p.onUpdateAbilities();}var at=target.add(0,.2,-distance);var d=target.add(0,.15,0).subtract(at.add(0,p.getEyeHeight(),0));p.teleportTo(s.overworld(),at.x,at.y,at.z,Set.<Relative>of(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())),false);}
 private static void shot(ClientGameTestContext c,String name) {c.runOnClient(mc -> {mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());}
 private static void check(boolean b,String why) {if(!b)throw new AssertionError(why);}
}
