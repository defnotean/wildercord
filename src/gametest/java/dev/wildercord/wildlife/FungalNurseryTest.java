package dev.wildercord.wildlife;
import dev.wildercord.cast.*;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.*;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import java.util.*;
/** Actual client investigation, native crafting result pickups, physical fertilization and full restart. */
public final class FungalNurseryTest implements FabricClientGameTest {
 private static SporebackSnail snail;private static BlockPos rootMark,airMark,plant=new BlockPos(4,30,0),roof=new BlockPos(4,31,2);private static UUID saved;private static long filterRest,nurseryRest,forageRest;
 private static volatile boolean traceGather;
 private static boolean traceInstalled;
 public static boolean tracingGather(){return traceGather;}
 private static void installGatherTrace() {
  if(traceInstalled)return;traceInstalled=true;
  net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player,level,hand,entity,hit) -> {
   if(traceGather && entity==snail && level instanceof ServerLevel l)
    dev.wildercord.Wildercord.LOGGER.info("FUNGAL_GATHER_RECEIVED now={} player={} shift={} hand={} held={} alive={} spectator={} worldSame={} distance={} visible={} snailAlive={} snailRemoved={} pose={} dew={} hidden={} gather={} selected={}",l.getGameTime(),player.position(),player.isShiftKeyDown(),hand,player.getItemInHand(hand),player.isAlive(),player.isSpectator(),player.level()==snail.level(),player.distanceToSqr(snail),player.hasLineOfSight(snail),snail.isAlive(),snail.isRemoved(),snail.pose(),snail.dew(),snail.hiddenUntil(),snail.gatherReady(),player.getInventory().getSelectedSlot());
   return InteractionResult.PASS;
  });
 }
 @Override public void runTest(ClientGameTestContext c) {
  boolean priorShift=c.computeOnClient(mc -> mc.options.keyShift.isDown());
  try {
  TestWorldSave save;
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(30);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");w.getServer().runCommand("time set 18000");
   w.getServer().runOnServer(s -> {var l=s.overworld();for(int x=-20;x<=20;x++)for(int z=-12;z<=12;z++) {l.setBlock(new BlockPos(x,29,z),Blocks.MOSS_BLOCK.defaultBlockState(),2);for(int y=30;y<=32;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);l.setBlock(new BlockPos(x,33,z),Blocks.STONE.defaultBlockState(),2);}l.setBlock(new BlockPos(-2,30,0),Blocks.BROWN_MUSHROOM.defaultBlockState(),2);l.setBlock(plant.below().east(),Blocks.WATER.defaultBlockState(),2);p(s).setGameMode(GameType.SURVIVAL);p(s).addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,3000));move(s,-4.5,-2.5);
    for(int i=0;i<20 && (rootMark==null || airMark==null);i++) {var origin=new BlockPos(i<10?-12:12,30,(i%4)*2-4);new BreathmarkFeature().place(l,l.getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(1300L+i*99194853094755497L),origin);for(var at:BlockPos.betweenClosed(origin.offset(-4,-6,-4),origin.offset(4,6,4)))if(l.getBlockState(at).is(FungalGarden.BREATHMARK)) {int k=l.getBlockState(at).getValue(BreathmarkBlock.KIND);if(k==0 && rootMark==null)rootMark=at.immutable();if(k==1 && airMark==null)airMark=at.immutable();}}
    for(int x=0;x<=7;x++)for(int z=-2;z<=4;z++)if(x==0 || x==7 || z==-2 || z==4)l.setBlock(new BlockPos(x,30,z),Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState(),3);
    check(rootMark!=null && airMark!=null,"Real registered feature creates both provenance-bearing clue kinds");check(((BreathmarkEntity)l.getBlockEntity(rootMark)).authentic(),"Generated clue provenance");snail=SporebackContent.SNAIL.create(l,EntitySpawnReason.COMMAND);snail.snapTo(-3.5,30,.5,0,0);l.addFreshEntity(snail);});
   clickBlock(c,w,rootMark,ItemStack.EMPTY);check(w.getServer().computeOnServer(s -> !FungalInvestigation.knows(p(s),FungalInvestigation.ROOT)),"Clue before living observation does not advance chain");
   await(c,w,()->snail.dew(),"Snail physically browses first mushroom");gatherVisitor(c,w);check(w.getServer().computeOnServer(s -> FungalInvestigation.knows(p(s),FungalInvestigation.FIRST)),"Actual observation starts investigation");
   var replica=new BlockPos(-6,30,5);w.getServer().runOnServer(s -> s.overworld().setBlock(replica,FungalGarden.BREATHMARK.defaultBlockState(),2));clickBlock(c,w,replica,ItemStack.EMPTY);check(w.getServer().computeOnServer(s -> !FungalInvestigation.knows(p(s),FungalInvestigation.ROOT)),"Placed replica cannot mint clue book/progress");
   clickBlock(c,w,rootMark,ItemStack.EMPTY);clickBlock(c,w,rootMark,ItemStack.EMPTY);clickBlock(c,w,airMark,ItemStack.EMPTY);w.getServer().runOnServer(s -> {check(p(s).getInventory().countItem(FungalGarden.ROOT_NOTES)==1 && p(s).getInventory().countItem(FungalGarden.AIR_NOTES)==1,"Distinct clues grant each original journal once");check(FungalInvestigation.next(p(s)).equals("harvest"),"Contextual next step names harvest");});
   readBook(c,w,FungalGarden.ROOT_NOTES,"fungal_root_field_notes");
   // An actual player planting packet, supported damp/dark conditions and native Life preparation.
   w.getServer().runOnServer(s -> {s.overworld().setBlock(plant,Blocks.AIR.defaultBlockState(),2);});clickBlock(c,w,plant.below(),new ItemStack(FungalGarden.CUTTING));c.waitTicks(5);
   w.getServer().runOnServer(s -> {check(s.overworld().getBlockState(plant).is(FungalGarden.GLOWCAP),"Native cutting placement");check(GlowcapBlock.conditions(s.overworld(),plant),"Actual planted damp covered habitat");s.overworld().setBlock(plant.below().east(),Blocks.AIR.defaultBlockState(),2);impact(s,plant,Runes.HEAL);check(s.overworld().getBlockState(plant).getValue(GlowcapBlock.AGE)==0 && !GlowcapBlock.conditions(s.overworld(),plant),"Life cannot prepare a dry planting");s.overworld().setBlock(plant.below().east(),Blocks.WATER.defaultBlockState(),2);impact(s,plant,Runes.GROW);for(var at:BlockPos.betweenClosed(plant.offset(-1,0,-1),plant.offset(1,0,1)))dev.wildercord.Wildercord.LOGGER.info("FUNGAL_GROW_GEOMETRY "+at+" state="+s.overworld().getBlockState(at)+" collision="+s.overworld().getBlockState(at).getCollisionShape(s.overworld(),at).toAabbs());check(s.overworld().getBlockState(plant).getValue(GlowcapBlock.AGE)==1,"Native Life prepares bud");impact(s,plant,Runes.HEAL);check(s.overworld().getBlockState(plant).getValue(GlowcapBlock.AGE)==1,"Repeated Life never replaces snail fertilization");s.overworld().setBlock(new BlockPos(-2,30,0),Blocks.AIR.defaultBlockState(),2);snail.discard();snail=SporebackContent.SNAIL.create(s.overworld(),EntitySpawnReason.COMMAND);snail.snapTo(1.5,30,.5,0,0);s.overworld().addFreshEntity(snail);move(s,7.5,-2.5);});
   await(c,w,()->snail.level().getBlockState(plant).getValue(GlowcapBlock.AGE)==2,"A real uninterrupted snail approach matures cap");w.getServer().runOnServer(s -> {check(snail.dew(),"Visit produces only the usual saved dew reserve");forageRest=snail.forageReady();view(s,Vec3.atCenterOf(plant).add(0,.2,0),plant.getX()+1.8,plant.getZ()-1.5);hand(p(s),ItemStack.EMPTY);});c.waitTicks(8);shot(c,"fungal_glowcap_snail_visit");
   w.getServer().runOnServer(s -> p(s).setGameMode(GameType.ADVENTURE));clickBlock(c,w,plant,ItemStack.EMPTY);check(w.getServer().computeOnServer(s -> s.overworld().getBlockState(plant).getValue(GlowcapBlock.AGE)==2 && !FungalInvestigation.knows(p(s),FungalInvestigation.HARVEST)),"Adventure/permission refusal preserves mature plant and progress");w.getServer().runOnServer(s -> p(s).setGameMode(GameType.SURVIVAL));clickBlock(c,w,plant,ItemStack.EMPTY);c.waitTicks(8);check(w.getServer().computeOnServer(s -> FungalInvestigation.knows(p(s),FungalInvestigation.HARVEST) && s.overworld().getBlockState(plant).getValue(GlowcapBlock.AGE)==0 && snail.forageReady()==forageRest),"Actual mature harvest resets plant without shortening visitor rest");
   // Pick up and spend actual harvested gills; only ordinary vanilla supplies are fixture-provided.
   w.getServer().runOnServer(s -> {move(s,plant.getX()+.5,plant.getZ()+.5);hand(p(s),ItemStack.EMPTY);});c.waitTicks(20);check(w.getServer().computeOnServer(s -> p(s).getInventory().countItem(FungalGarden.GILLS)==1),"Real harvested gill drop reaches Survival inventory");
   craft(c,w,w.getServer().computeOnServer(s -> List.of(new ItemStack(Items.STICK),new ItemStack(Items.STICK),new ItemStack(Items.PAPER),ingredient(p(s),FungalGarden.GILLS))),FungalGarden.NURSERY_ITEM);check(w.getServer().computeOnServer(s -> p(s).getInventory().countItem(FungalGarden.GILLS)==0),"First native craft spends the earned gill");
   pruneNurseryApproach(c,w); // Actual Survival garden work; keep the same visitor and real rests.
   w.getServer().runOnServer(s -> s.overworld().setBlock(roof.below(),Blocks.STONE.defaultBlockState(),2));clickEarned(c,w,roof.below(),FungalGarden.NURSERY_ITEM);w.getServer().runOnServer(s -> {check(s.overworld().getBlockState(roof).is(FungalGarden.NURSERY),"Native canopy placement above temporary support");check(s.overworld().getBlockState(roof).getCollisionShape(s.overworld(),roof).bounds().minY==-1,"Real canopy corner collision reaches walking floor");s.overworld().setBlock(roof.below(),Blocks.AIR.defaultBlockState(),2);});check(w.getServer().computeOnServer(s -> FungalInvestigation.knows(p(s),FungalInvestigation.ROOF)),"Own real nursery placement advances chain");
   // Clear the same visitor's reserve, prepare the perennial again and wait its real saved rest.
   await(c,w,()->snail.pose()==2 && snail.nurseryReady()>0 && snail.blockPosition().equals(roof.below()),"Visitor physically enters grounded Nursery before its first finite rest");
   await(c,w,()->snail.pose()!=2,"Nursery visitor opens after its finite first rest");
   gatherVisitor(c,w);w.getServer().runOnServer(s -> {check(!snail.dew(),"Actual admitted native gather spends the same visitor reserve");check(snail.forageReady()==forageRest,"Gathering the same visitor preserves its exact real forage rest");impact(s,plant,Runes.HEAL);});waitSavedForage(c,w);
   await(c,w,()->snail.level().getBlockState(plant).getValue(GlowcapBlock.AGE)==2,"Same living visitor completes second real rested browse");w.getServer().runOnServer(s -> {check(snail.forageReady()>forageRest,"Second cycle spends a new finite forage rest");forageRest=snail.forageReady();dev.wildercord.Wildercord.LOGGER.info("FUNGAL_REPEAT_BROWSE now={} actualForageReady={} nurseryReady={} pose={} position={}",s.overworld().getGameTime(),forageRest,snail.nurseryReady(),snail.pose(),snail.position());});await(c,w,()->snail.pose()==2 && snail.nurseryReady()>0 && snail.blockPosition().equals(roof.below()),"Rested repeat visitor finds actual nursery");w.getServer().runOnServer(s -> {nurseryRest=snail.nurseryReady();check(nurseryRest>s.overworld().getGameTime() && nurseryRest<=s.overworld().getGameTime()+1200,"The observed real repeat nursery admission establishes its independent unrenewed future rest");dev.wildercord.Wildercord.LOGGER.info("FUNGAL_REPEAT_NURSERY now={} nurseryReady={} forageReady={} pose={} position={}",s.overworld().getGameTime(),nurseryRest,snail.forageReady(),snail.pose(),snail.position());view(s,Vec3.atCenterOf(roof).add(0,-.7,0),6,-.8);});c.waitTicks(14);shot(c,"fungal_open_nursery_rest");clickBlock(c,w,plant,ItemStack.EMPTY);w.getServer().runOnServer(s -> {move(s,plant.getX()+.5,plant.getZ()+.5);hand(p(s),ItemStack.EMPTY);});c.waitTicks(20);check(w.getServer().computeOnServer(s -> p(s).getInventory().countItem(FungalGarden.GILLS)==1),"Second actual mature harvest reaches inventory");
   craft(c,w,w.getServer().computeOnServer(s -> List.of(ingredient(p(s),SporebackContent.DEW),ingredient(p(s),FungalGarden.GILLS),new ItemStack(Items.LEATHER),new ItemStack(Items.COPPER_INGOT))),FungalGarden.BREATHER);check(w.getServer().computeOnServer(s -> FungalInvestigation.next(p(s)).equals("return")),"Real crafted filter admits return step");
   clickEarned(c,w,roof,FungalGarden.BREATHER);clickEarned(c,w,roof,FungalGarden.BREATHER);w.getServer().runOnServer(s -> {check(FungalInvestigation.knows(p(s),FungalInvestigation.DONE),"Actual nursery return completes connected investigation");check(p(s).getInventory().countItem(FungalGarden.CONCLUSION)==1 && p(s).getInventory().countItem(dev.wildercord.content.WildercordItems.BLANK_RUNE)==3,"Completion reward exactly once");move(s,6.5,-1.5);});
   w.getServer().runOnServer(s -> check(snail.forageReady()==forageRest && snail.nurseryReady()==nurseryRest,"Nursery does not shorten forage rest or renew its independent admission clock"));
   // Offhand item packet, finite wear/tradeoff and no protection from a new poison application.
   w.getServer().runOnServer(s -> {hand(p(s),ItemStack.EMPTY);p(s).setItemInHand(InteractionHand.OFF_HAND,take(p(s),FungalGarden.BREATHER));p(s).addEffect(new MobEffectInstance(MobEffects.POISON,400));});c.waitTicks(5);c.runOnClient(mc -> mc.gameMode.useItem(mc.player,InteractionHand.OFF_HAND));c.waitTicks(5);
   w.getServer().runOnServer(s -> {check(!p(s).hasEffect(MobEffects.POISON) && p(s).hasEffect(MobEffects.SLOWNESS) && p(s).getOffhandItem().getDamageValue()==1,"Actual filter use clears current Poison with wear and heavy steps");filterRest=p(s).getAttachedOrElse(FungalGarden.BREATHER_READY,0L);p(s).addEffect(new MobEffectInstance(MobEffects.POISON,400));});c.runOnClient(mc -> mc.gameMode.useItem(mc.player,InteractionHand.OFF_HAND));c.waitTicks(5);w.getServer().runOnServer(s -> {check(p(s).hasEffect(MobEffects.POISON) && p(s).getOffhandItem().getDamageValue()==1 && p(s).getAttachedOrElse(FungalGarden.BREATHER_READY,0L)==filterRest,"New poison remains during shared rest, no repeat wear or renewal");p(s).removeEffect(MobEffects.POISON);saved=snail.getUUID();});readBook(c,w,FungalGarden.CONCLUSION,"fungal_three_breathmarks_conclusion");save=w.getWorldSave();
  }
  try(var w=save.open()) {c.waitTicks(35);w.getServer().runOnServer(s -> {snail=(SporebackSnail)s.overworld().getEntity(saved);check(snail!=null && snail.nurseryReady()==nurseryRest && snail.forageReady()==forageRest,"Full restart retains exact independent visitor rests");check(((BreathmarkEntity)s.overworld().getBlockEntity(rootMark)).authentic(),"Full restart preserves generated clue provenance");check(FungalInvestigation.knows(p(s),FungalInvestigation.DONE) && p(s).getAttachedOrElse(FungalGarden.BREATHER_READY,0L)==filterRest,"Full restart preserves claimed chain/filter deadline");check(p(s).getCooldowns().isOnCooldown(new ItemStack(FungalGarden.BREATHER)),"Join restores cooldown indicator for another copy");});clickBlock(c,w,rootMark,ItemStack.EMPTY);clickEarned(c,w,roof,FungalGarden.BREATHER);check(w.getServer().computeOnServer(s -> p(s).getInventory().countItem(FungalGarden.CONCLUSION)==1 && p(s).getInventory().countItem(dev.wildercord.content.WildercordItems.BLANK_RUNE)==3),"Restart/other sites cannot renew completion rewards");
   c.waitTicks(210);w.getServer().runOnServer(s -> {var tool=take(p(s),FungalGarden.BREATHER);tool.setDamageValue(31);hand(p(s),ItemStack.EMPTY);p(s).setItemInHand(InteractionHand.OFF_HAND,tool);p(s).addEffect(new MobEffectInstance(MobEffects.POISON,400));});c.waitTicks(5);c.runOnClient(mc -> mc.gameMode.useItem(mc.player,InteractionHand.OFF_HAND));c.waitTicks(5);check(w.getServer().computeOnServer(s -> p(s).getOffhandItem().isEmpty() && !p(s).hasEffect(MobEffects.POISON) && p(s).getAttachedOrElse(FungalGarden.BREATHER_READY,0L)>s.overworld().getGameTime()),"Final native filter use breaks finite equipment yet preserves shared rest");
  }
  } finally {c.runOnClient(mc -> mc.options.keyShift.setDown(priorShift));}
 }
 /** Grow may place colliding azalea beside the garden; physically prune that supplied approach before erecting the canopy. */
 private static void pruneNurseryApproach(ClientGameTestContext c,TestSingleplayerContext w){
  UUID visitor=w.getServer().computeOnServer(s -> snail.getUUID());
  Map<Integer,ItemStack> earnedNursery=w.getServer().computeOnServer(s -> {
   var held=new HashMap<Integer,ItemStack>();for(int slot=0;slot<36;slot++){var stack=p(s).getInventory().getItem(slot);if(stack.is(FungalGarden.NURSERY_ITEM))held.put(slot,stack);}
   check(held.size()==1 && held.values().iterator().next().getCount()==1,"Pruning begins with the one actual crafted Nursery stack");return Map.copyOf(held);
  });
  long rest=w.getServer().computeOnServer(s -> snail.forageReady()),nursery=w.getServer().computeOnServer(s -> snail.nurseryReady());
  List<BlockPos> shrubs=w.getServer().computeOnServer(s -> {
   var l=s.overworld();var result=new ArrayList<BlockPos>();
   for(var at:BlockPos.betweenClosed(plant.offset(-1,0,-1),plant.offset(1,0,1))) {
    var state=l.getBlockState(at);
    if((state.is(Blocks.AZALEA)||state.is(Blocks.FLOWERING_AZALEA))&&!state.getCollisionShape(l,at).isEmpty())result.add(at.immutable());
   }
   dev.wildercord.Wildercord.LOGGER.info("FUNGAL_PRUNE_BEFORE body={} box={} ground={} nurseryFoot={} footCollision={} nurseryAirCollisionFree={} shrubs={} forageReady={} nurseryReady={}",snail.position(),snail.getBoundingBox(),snail.onGround(),l.getBlockState(roof.below()),l.getBlockState(roof.below()).getCollisionShape(l,roof.below()).toAabbs(),l.noCollision(snail,new AABB(roof.below()).deflate(.15)),result,snail.forageReady(),snail.nurseryReady());return List.copyOf(result);
  });
  for(BlockPos at:shrubs){
   w.getServer().runOnServer(s -> {check(p(s).gameMode.getGameModeForPlayer()==GameType.SURVIVAL,"Garden pruning uses the actual Survival player");move(s,at.getX()+.5,at.getZ()-1.5);});c.waitTicks(3);
   c.runOnClient(mc -> {mc.gui.setScreen(null);mc.gameMode.startDestroyBlock(at,Direction.NORTH);});
   for(int tick=0;tick<20 && !w.getServer().computeOnServer(s -> s.overworld().getBlockState(at).isAir());tick++){c.runOnClient(mc -> mc.gameMode.continueDestroyBlock(at,Direction.NORTH));c.waitTicks(1);}
   c.runOnClient(mc -> mc.gameMode.stopDestroyBlock());
   check(w.getServer().computeOnServer(s -> s.overworld().getBlockState(at).isAir()),"Actual native Survival mining removes only a Grow-produced colliding azalea");
  }
  w.getServer().runOnServer(s -> {for(var entry:earnedNursery.entrySet()){check(p(s).getInventory().getItem(entry.getKey())==entry.getValue()&&entry.getValue().getCount()==1,"Native pruning preserves the exact actual crafted Nursery in its original inventory slot");}check(snail.getUUID().equals(visitor)&&snail.isAlive()&&snail.forageReady()==rest&&snail.nurseryReady()==nursery&&snail.dew(),"Real garden pruning preserves the exact living visitor, reserve and independent deadlines");check(s.overworld().getBlockState(plant).is(FungalGarden.GLOWCAP)&&s.overworld().getBlockState(plant).getValue(GlowcapBlock.AGE)==0,"Pruning preserves the actually harvested perennial");dev.wildercord.Wildercord.LOGGER.info("FUNGAL_PRUNE_AFTER body={} ground={} forageReady={} nurseryReady={} removedShrubs={}",snail.position(),snail.onGround(),snail.forageReady(),snail.nurseryReady(),shrubs.size());});
 }
 private static void craft(ClientGameTestContext c,TestSingleplayerContext w,List<ItemStack> ingredients,Item expected) {
  w.getServer().runOnServer(s -> {for(int i=0;i<4;i++)p(s).inventoryMenu.getSlot(i+1).set(ingredients.get(i));p(s).inventoryMenu.broadcastChanges();});c.waitTicks(5);check(w.getServer().computeOnServer(s -> p(s).inventoryMenu.getSlot(0).getItem().is(expected)),"Native crafting result is "+expected);c.runOnClient(mc -> mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId,0,0,ContainerInput.QUICK_MOVE,mc.player));c.waitTicks(5);check(w.getServer().computeOnServer(s -> p(s).getInventory().countItem(expected)>0 && p(s).inventoryMenu.getSlot(1).getItem().isEmpty()),"Actual result pickup consumes inputs");
 }
 private static void readBook(ClientGameTestContext c,TestSingleplayerContext w,Item book,String shot) {w.getServer().runOnServer(s -> {int index=-1;for(int i=0;i<36;i++)if(p(s).getInventory().getItem(i).is(book)){index=i;break;}check(index>=0,"Earned book exists");var stack=p(s).getInventory().getItem(index).copy();p(s).getInventory().setItem(index,ItemStack.EMPTY);hand(p(s),stack);});c.waitTicks(5);c.runOnClient(mc -> mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND));c.waitTicks(5);check(c.computeOnClient(mc -> mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.BookViewScreen),"Native earned book opens");shot(c,shot);c.runOnClient(mc -> mc.gui.setScreen(null));}
 private static ItemStack ingredient(ServerPlayer p,Item item) {for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(item)){var stack=p.getInventory().getItem(i);var one=stack.split(1);if(stack.isEmpty())p.getInventory().setItem(i,ItemStack.EMPTY);return one;}throw new AssertionError("Earned ingredient missing: "+item);}
 private static ItemStack take(ServerPlayer p,Item item) {for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(item)){var stack=p.getInventory().getItem(i);p.getInventory().setItem(i,ItemStack.EMPTY);return stack;}if(p.getOffhandItem().is(item)){var stack=p.getOffhandItem();p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);return stack;}throw new AssertionError("Actual earned item missing: "+item);}
 private static void clickEarned(ClientGameTestContext c,TestSingleplayerContext w,BlockPos at,Item item) {w.getServer().runOnServer(s -> {var stack=take(p(s),item);move(s,at.getX()+.5,at.getZ()-2);hand(p(s),stack);});c.waitTicks(5);c.runOnClient(mc -> mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at).add(0,.5,0),Direction.UP,at,false)));c.waitTicks(5);}
 private static void hand(ServerPlayer p,ItemStack stack) {var old=p.getMainHandItem();if(!old.isEmpty()) {int empty=-1;for(int i=9;i<36;i++)if(p.getInventory().getItem(i).isEmpty()){empty=i;break;}check(empty>=0,"Fixture can retain existing earned inventory");p.getInventory().setItem(empty,old.copy());}p.setItemInHand(InteractionHand.MAIN_HAND,stack);}
 private static void clickBlock(ClientGameTestContext c,TestSingleplayerContext w,BlockPos at,ItemStack held) {w.getServer().runOnServer(s -> {move(s,at.getX()+.5,at.getZ()-2);hand(p(s),held);});c.waitTicks(5);c.runOnClient(mc -> mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at).add(0,.5,0),Direction.UP,at,false)));c.waitTicks(5);}
 private static void impact(MinecraftServer s,BlockPos at,RuneDef effect) {var plan=SpellCompiler.compile(List.of(Runes.TOUCH,effect));CastEngine.onHit(new Cast(p(s)),plan.root().groups.getFirst(),new Cast.Hit(List.of(),Vec3.atCenterOf(at),new Vec3(0,0,1),p(s).position(),at,Direction.UP,false),null);}
 // Approach an actual open side of the grounded canopy; diagonal offsets can intersect a support.
 private static void gatherVisitor(ClientGameTestContext c,TestSingleplayerContext w) {
  installGatherTrace();
  boolean shift=c.computeOnClient(mc -> mc.options.keyShift.isDown());
  int dewBefore=w.getServer().computeOnServer(s -> p(s).getInventory().countItem(SporebackContent.DEW));
  Set<UUID> existingDrops=w.getServer().computeOnServer(s -> s.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,snail.getBoundingBox().inflate(4),e -> e.getItem().is(SporebackContent.DEW)).stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet()));
  try {
   traceGather=true;
   w.getServer().runOnServer(s -> hand(p(s),ItemStack.EMPTY));c.waitTicks(5);c.runOnClient(mc -> mc.options.keyShift.setDown(true));c.waitTicks(5);
   for(int attempt=0;attempt<4;attempt++) {
    w.getServer().runOnServer(s -> {
     var player=p(s);var l=s.overworld();boolean clear=false;
     for(double[] offset:new double[][]{{.9,0},{-.9,0},{0,.9},{0,-.9},{1.4,-1.4},{-1.4,1.4},{1.4,1.4},{-1.4,-1.4}}) {
      double x=snail.getX()+offset[0],z=snail.getZ()+offset[1],y=snail.getY();
      var footing=BlockPos.containing(x,y-.1,z);if(l.getBlockState(footing).getCollisionShape(l,footing).isEmpty())continue;
      player.teleportTo(l,x,y,z,Set.<Relative>of(),0,30,false);
      if(l.noCollision(player,player.getBoundingBox()) && player.hasLineOfSight(snail) && player.distanceToSqr(snail)<=9) {clear=true;break;}
     }
     check(clear,"A real collision-free covered-garden approach has line of sight to the visitor");
     dev.wildercord.Wildercord.LOGGER.info("FUNGAL_GATHER admission player="+player.position()+" snail="+snail.position()+" shift="+player.isShiftKeyDown()+" visible="+player.hasLineOfSight(snail)+" pose="+snail.pose()+" dew="+snail.dew()+" gather="+snail.gatherReady()+" forage="+snail.forageReady()+" now="+l.getGameTime());
    });
    c.waitTicks(5);interact(c,w);
    if(w.getServer().computeOnServer(s -> !snail.dew())) {collectEarnedDew(c,w,dewBefore,existingDrops);return;}
   }
   throw new AssertionError("Four real native crouch interactions did not spend the visitor reserve; inspect FUNGAL_GATHER admissions");
  }finally {traceGather=false;c.runOnClient(mc -> mc.options.keyShift.setDown(shift));}
 }
 // Follow the actual emitted item using client movement. A random drop may land beyond a stationary pickup box.
 private static void collectEarnedDew(ClientGameTestContext c,TestSingleplayerContext w,int before,Set<UUID> oldDrops) {
  boolean up=c.computeOnClient(mc -> mc.options.keyUp.isDown()),left=c.computeOnClient(mc -> mc.options.keyLeft.isDown()),right=c.computeOnClient(mc -> mc.options.keyRight.isDown());
  try {
   if(w.getServer().computeOnServer(s -> p(s).getInventory().countItem(SporebackContent.DEW)>before))return;
   UUID drop=w.getServer().computeOnServer(s -> {
    if(p(s).getInventory().countItem(SporebackContent.DEW)>before)return null;
    var found=s.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,snail.getBoundingBox().inflate(4),e -> e.getItem().is(SporebackContent.DEW) && !oldDrops.contains(e.getUUID()));
    check(found.size()==1,"One actual newly emitted dew drop remains to be physically gathered");return found.getFirst().getUUID();
   });
   if(drop==null)return; // Earned auto-pickup was observed atomically with lookup.
   Vec3 previous=w.getServer().computeOnServer(s -> p(s).position());int sideways=0,detours=0;
   for(int tick=0;tick<90;tick++) {
    if(w.getServer().computeOnServer(s -> p(s).getInventory().countItem(SporebackContent.DEW)>before))return;
    Vec3 target=w.getServer().computeOnServer(s -> {
     if(p(s).getInventory().countItem(SporebackContent.DEW)>before)return null;
     var e=s.overworld().getEntity(drop);check(e instanceof net.minecraft.world.entity.item.ItemEntity,"Tracked earned drop remains until real inventory pickup");return e.position();
    });
    if(target==null)return; // Position and earned inventory are one authoritative observation.
    if(tick%15==0) {
     Vec3 current=w.getServer().computeOnServer(s -> p(s).position());
     if(tick>0 && current.distanceToSqr(previous)<.0036 && current.distanceToSqr(target)>1 && detours<3){sideways=6;detours++;}
     previous=current;
     boolean pickedUp=w.getServer().computeOnServer(s -> {
      if(p(s).getInventory().countItem(SporebackContent.DEW)>before)return true;
      var found=s.overworld().getEntity(drop);check(found instanceof net.minecraft.world.entity.item.ItemEntity,"Tracked drop remains while earned inventory has not increased");
      var item=(net.minecraft.world.entity.item.ItemEntity)found;var access=(dev.wildercord.mixin.ItemEntityAccessor)item;
      dev.wildercord.Wildercord.LOGGER.info("FUNGAL_PICKUP player="+p(s).position()+" item="+item.position()+" velocity="+item.getDeltaMovement()+" age="+item.getAge()+" delay="+item.hasPickUpDelay()+" target="+access.wildercord$target()+" thrower="+(access.wildercord$thrower()==null?"none":access.wildercord$thrower().getUUID())+" collisionFree="+s.overworld().noCollision(p(s),p(s).getBoundingBox())+" inventory="+p(s).getInventory().countItem(SporebackContent.DEW));return false;
     });
     if(pickedUp)return;
    }
    boolean side=sideways-->0,toLeft=(detours%2)==1;
    c.runOnClient(mc -> {var d=target.subtract(mc.player.position());mc.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));mc.player.setXRot(35);mc.options.keyUp.setDown(true);mc.options.keyLeft.setDown(side && toLeft);mc.options.keyRight.setDown(side && !toLeft);});c.waitTicks(2);
   }
   check(w.getServer().computeOnServer(s -> p(s).getInventory().countItem(SporebackContent.DEW)>before),"Native movement must physically acquire the tracked earned dew; inspect FUNGAL_PICKUP geometry and ownership");
  } finally {c.runOnClient(mc -> {mc.options.keyUp.setDown(up);mc.options.keyLeft.setDown(left);mc.options.keyRight.setDown(right);});}
 }
 private static void interact(ClientGameTestContext c,TestSingleplayerContext w) {int id=w.getServer().computeOnServer(s -> snail.getId());c.runOnClient(mc -> {var e=mc.level.getEntity(id);check(e!=null && e.getUUID().equals(snail.getUUID()),"Native gather targets the actual tracked visitor");dev.wildercord.Wildercord.LOGGER.info("FUNGAL_GATHER_SEND player={} visitor={} shift={} keyShift={} held={} selected={} clientMode={}",mc.player.position(),e.position(),mc.player.isShiftKeyDown(),mc.options.keyShift.isDown(),mc.player.getMainHandItem(),mc.player.getInventory().getSelectedSlot(),mc.gameMode.getPlayerMode());var result=mc.gameMode.interact(mc.player,e,new EntityHitResult(e,e.getBoundingBox().getCenter()),InteractionHand.MAIN_HAND);dev.wildercord.Wildercord.LOGGER.info("FUNGAL_GATHER_CLIENT_RESULT {}",result);});c.waitTicks(5);w.getServer().runOnServer(s -> dev.wildercord.Wildercord.LOGGER.info("FUNGAL_GATHER_AFTER now={} player={} shift={} held={} pose={} dew={} hidden={} gather={}",s.overworld().getGameTime(),p(s).position(),p(s).isShiftKeyDown(),p(s).getMainHandItem(),snail.pose(),snail.dew(),snail.hiddenUntil(),snail.gatherReady()));}
 /** Observe the saved real deadline rather than blindly skipping a short later nursery visit. */
 private static void waitSavedForage(ClientGameTestContext c,TestSingleplayerContext w){
  w.getServer().runOnServer(s -> dev.wildercord.Wildercord.LOGGER.info("FUNGAL_REPEAT_WAIT_START now={} forageReady={} nurseryReady={} dew={} pose={}",s.overworld().getGameTime(),forageRest,snail.nurseryReady(),snail.dew(),snail.pose()));
  // At most the same1200-tick forage rest, not a new or extended ecological clock.
  for(int i=0;i<=240;i++){
   boolean reached=w.getServer().computeOnServer(s -> {long now=s.overworld().getGameTime();if(now>=forageRest)return true;check(snail.forageReady()==forageRest,"Before its saved deadline the visitor preserves the exact actual forage rest");check(!snail.dew() && s.overworld().getBlockState(plant).getValue(GlowcapBlock.AGE)==1,"Before its exact real rest expires the gathered visitor cannot mint dew or mature the prepared cap");return false;});
   if(reached){w.getServer().runOnServer(s -> dev.wildercord.Wildercord.LOGGER.info("FUNGAL_REPEAT_WAIT_REACHED now={} forageReady={} nurseryReady={} dew={} pose={}",s.overworld().getGameTime(),forageRest,snail.nurseryReady(),snail.dew(),snail.pose()));return;}
   if(i<240)c.waitTicks(5);
  }
  throw new AssertionError("The existing saved forage deadline must be reached within its unchanged1200-tick maximum");
 }
 private static void await(ClientGameTestContext c,TestSingleplayerContext w,java.util.function.BooleanSupplier yes,String why) {for(int i=0;i<150;i++) {c.waitTicks(5);if(w.getServer().computeOnServer(s -> yes.getAsBoolean()))return;}w.getServer().runOnServer(s -> dev.wildercord.Wildercord.LOGGER.info("FUNGAL_AWAIT "+why+" snail="+snail.position()+" pose="+snail.pose()+" dew="+snail.dew()+" forage="+snail.forageReady()+" now="+s.overworld().getGameTime()+" nurseryReady="+snail.nurseryReady()+" hiddenUntil="+snail.hiddenUntil()+" nurseryBlock="+s.overworld().getBlockState(roof)+" cap="+s.overworld().getBlockState(plant)+" foot="+s.overworld().getBlockState(snail.blockPosition().below())+" path="+(snail.getNavigation().getPath()==null?"none":snail.getNavigation().getPath().getEndNode())+" clip="+s.overworld().clip(new net.minecraft.world.level.ClipContext(snail.getEyePosition(),Vec3.atCenterOf(plant),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,snail))));throw new AssertionError(why);}
 private static ServerPlayer p(MinecraftServer s) {return s.getPlayerList().getPlayers().getFirst();}
 private static void move(MinecraftServer s,double x,double z) {p(s).teleportTo(s.overworld(),x,30,z,Set.<Relative>of(),0,30,false);}
 private static void view(MinecraftServer s,Vec3 focus,double x,double z) {var eye=new Vec3(x,30+p(s).getEyeHeight(),z);var d=focus.subtract(eye);p(s).teleportTo(s.overworld(),x,30,z,Set.<Relative>of(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())),false);}
 private static void shot(ClientGameTestContext c,String n) {c.runOnClient(mc -> {mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.takeScreenshot(TestScreenshotOptions.of(n).disableCounterPrefix());}
 private static void check(boolean b,String why) {if(!b)throw new AssertionError(why);}
}
