package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.BelowkeeperTestSupport.*;
import dev.wildercord.cast.*;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;
/** Actual authored feature, living growth/gather/harvest/counter, earned payment/craft/equip and full restart. */
public final class DrainhouseAcquisitionTest implements FabricClientGameTest {
 private final SporebackSnail[] snails=new SporebackSnail[6];private final BlockPos[] plants=new BlockPos[6],marks=new BlockPos[3];private RootmoltStrider root;
 public void runTest(ClientGameTestContext c){
  TestWorldSave save;UUID creature;BlockPos rootHome;long mealReady,repairReady,bellReady;
  try(var w=c.worldBuilder().create()){
   c.waitTicks(25);w.getServer().runCommand("difficulty normal");w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");w.getServer().runCommand("time set 18000");
   w.getServer().runOnServer(s -> {
    var l=s.overworld();p(s).setGameMode(GameType.SURVIVAL);p(s).addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,4000,0,false,false,false));
    for(int x=-18;x<=18;x++)for(int z=-12;z<=12;z++){l.setBlock(new BlockPos(x,29,z),Blocks.MOSS_BLOCK.defaultBlockState(),2);for(int y=30;y<=33;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);l.setBlock(new BlockPos(x,34,z),Blocks.STONE.defaultBlockState(),2);}
    check(new DrainhouseFeature().place(l,l.getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(716843),new BlockPos(0,30,0)),"Actual registered authored feature builds all three rooms");
    for(var at:BlockPos.betweenClosed(new BlockPos(-18,30,-12),new BlockPos(18,32,12)))if(l.getBlockState(at).is(DrainhouseContent.MARK)){int kind=l.getBlockState(at).getValue(DrainhouseMark.KIND);marks[kind]=at.immutable();check(((DrainhouseMarkEntity)l.getBlockEntity(at)).authentic(),"Only actual feature markers have provenance");}
    for(var at:marks)check(at!=null,"Every distinct authentic ledger is generated");
    for(int i=0;i<6;i++){plants[i]=new BlockPos(40+i*12,30,0);var center=plants[i];for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){l.setBlock(center.offset(x,-1,z),Blocks.MOSS_BLOCK.defaultBlockState(),2);for(int y=0;y<=2;y++)l.setBlock(center.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);l.setBlock(center.offset(x,3,z),Blocks.STONE.defaultBlockState(),2);}l.setBlock(center.below(2).east(),Blocks.STONE.defaultBlockState(),2);l.setBlock(center.below().east(),Blocks.WATER.defaultBlockState(),2);l.setBlock(center,FungalGarden.GLOWCAP.defaultBlockState(),2);snails[i]=SporebackContent.SNAIL.create(l,EntitySpawnReason.COMMAND);snails[i].setPersistenceRequired();snails[i].snapTo(center.getX()-1.5,30,.5,0,0);l.addFreshEntity(snails[i]);}
    move(p(s),42,30,3);
   });
   for(int i=0;i<6;i++){final int cell=i;w.getServer().runOnServer(s -> move(p(s),plants[cell].getX()+3.5,30,3.5));await(c,w,s -> GlowcapBlock.conditions(s.overworld(),plants[cell]),"Actual newly placed covered habitat finishes light propagation before Life preparation");w.getServer().runOnServer(s -> prepare(s,plants[cell]));await(c,w,s -> s.overworld().getBlockState(plants[cell]).getValue(GlowcapBlock.AGE)==2 && snails[cell].dew(),"Real uninterrupted snail visits prepare actual earned reserves and mature plants");}
   for(int i=0;i<5;i++){gather(c,w,snails[i]);harvest(c,w,plants[i]);}
   w.getServer().runOnServer(s -> {check(p(s).getInventory().countItem(SporebackContent.DEW)==5 && p(s).getInventory().countItem(FungalGarden.GILLS)==5,"Five actual emitted dew and gill pickups fund restoration, equipment and real paid repair/refusal");root=RootmoltContent.STRIDER.create(s.overworld(),EntitySpawnReason.COMMAND);root.setPersistenceRequired();root.snapTo(plants[5].getX()-1.5,30,.5,0,0);s.overworld().addFreshEntity(root);move(p(s),plants[5].getX()+3.5,30,2.5);});
   try{await(c,w,s -> FungalInvestigation.knows(p(s),"field:rootmolt_meal"),"Survival player actually observes the living competitor's mature-cap consumption");}catch(AssertionError failure){w.getServer().runOnServer(s -> dev.wildercord.Wildercord.LOGGER.info("DRAINHOUSE_MEAL_FAIL root={} pose={} alive={} removed={} target={} mealReady={} cap={} player={} distance={} observerLOS={} rootCapLOS={} now={}",root.position(),root.pose(),root.isAlive(),root.isRemoved(),root.getTarget(),root.mealReady(),s.overworld().getBlockState(plants[5]),p(s).position(),root.distanceToSqr(p(s)),root.hasLineOfSight(p(s)),s.overworld().clip(new net.minecraft.world.level.ClipContext(root.getEyePosition(),Vec3.atCenterOf(plants[5]),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,root)),s.overworld().getGameTime()));throw failure;}
   w.getServer().runOnServer(s -> {check(s.overworld().getBlockState(plants[5]).getValue(GlowcapBlock.AGE)==0 && root.mealReady()>s.overworld().getGameTime(),"Real meal spends only its sixth cap and saves rest");move(p(s),root.getX()+2,30,root.getZ());});await(c,w,s -> root.pose()==RootmoltStrider.WARNING,"Real territory produces a readable warning");
   int id=root.getId();c.runOnClient(mc -> mc.gameMode.attack(mc.player,mc.level.getEntity(id)));c.waitTicks(5);w.getServer().runOnServer(s -> check(FungalInvestigation.knows(p(s),"field:rootmolt_counter"),"Actual native physical counter records the earned fact"));
   // The books must be read in their authored order; replicas cannot grant any journal.
   var replica=new BlockPos(-15,30,9);w.getServer().runOnServer(s -> s.overworld().setBlock(replica,DrainhouseContent.MARK.defaultBlockState(),2));click(c,w,replica,ItemStack.EMPTY);w.getServer().runOnServer(s -> check(!FungalInvestigation.knows(p(s),DrainhouseInvestigation.THRESHOLD),"Ordinary replica lacks authentic clue provenance"));
   click(c,w,marks[1],ItemStack.EMPTY);click(c,w,marks[2],ItemStack.EMPTY);
   w.getServer().runOnServer(s -> check(!FungalInvestigation.knows(p(s),DrainhouseInvestigation.GARDEN) && !FungalInvestigation.knows(p(s),DrainhouseInvestigation.ALCOVE) && p(s).getInventory().countItem(DrainhouseContent.GARDEN_NOTES)==0 && p(s).getInventory().countItem(DrainhouseContent.ALCOVE_NOTES)==0,"Actual out-of-order ledgers cannot bypass the earned threshold clue"));
   for(int i=0;i<3;i++){click(c,w,marks[i],ItemStack.EMPTY);click(c,w,marks[i],ItemStack.EMPTY);}
   w.getServer().runOnServer(s -> check(p(s).getInventory().countItem(DrainhouseContent.ENTRANCE_NOTES)==1 && p(s).getInventory().countItem(DrainhouseContent.GARDEN_NOTES)==1 && p(s).getInventory().countItem(DrainhouseContent.ALCOVE_NOTES)==1,"Three distinct authored clue texts are each awarded once"));
   readBook(c,w,DrainhouseContent.ENTRANCE_NOTES,"drainhouse_threshold_original");readBook(c,w,DrainhouseContent.GARDEN_NOTES,"drainhouse_garden_original");readBook(c,w,DrainhouseContent.ALCOVE_NOTES,"drainhouse_alcove_original");
   // Copper and leather are ordinary vanilla fixture supplies; all fungal inputs came from real pickups.
   w.getServer().runOnServer(s -> {p(s).getInventory().placeItemBackInInventory(new ItemStack(Items.COPPER_INGOT,7),net.minecraft.util.Prediction.SERVER_ONLY);check(!FungalInvestigation.knows(p(s),DrainhouseInvestigation.DONE) && p(s).getInventory().countItem(BelowkeeperEquipment.BELL)==0,"The closed prepayment save contains earned investigation and materials but no reward");});
   creature=root.getUUID();rootHome=root.blockPosition();save=w.getWorldSave();
  }
  // Copy only after ordinary full shutdown, never a live region/player file or fabricated journal.
  TestWorldSave earnedBranch=copyEarnedWorld(c,save);
  try(var w=save.open()){
   c.waitTicks(25);w.getServer().runOnServer(s -> {s.overworld().getChunkAt(rootHome);move(p(s),plants[5].getX()+3.5,30,2.5);});
   await(c,w,s -> s.overworld().getEntity(creature) instanceof RootmoltStrider,"The original earned competitor loads normally after the prepayment save");
   w.getServer().runOnServer(s -> root=(RootmoltStrider)s.overworld().getEntity(creature));
   callbackRefusals(c,w);
   click(c,w,marks[2],w.getServer().computeOnServer(s -> take(p(s),SporebackContent.DEW,2)));
   w.getServer().runOnServer(s -> {check(FungalInvestigation.knows(p(s),DrainhouseInvestigation.DONE) && s.overworld().getBlockState(marks[2]).getValue(DrainhouseMark.RESTORED),"Actual earned restoration changes the authentic return desk");check(p(s).getInventory().countItem(BelowkeeperEquipment.BELL)==1 && p(s).getInventory().countItem(SporebackContent.DEW)==3 && p(s).getInventory().countItem(FungalGarden.GILLS)==3 && p(s).getInventory().countItem(Items.COPPER_INGOT)==3,"Finite relic is paid by exactly two dew/two gills/four copper");check(!DrainhouseInvestigation.restore(p(s),marks[2]),"A second admitted restoration cannot repeat rewards");});
   craft(c,w,w.getServer().computeOnServer(s -> List.of(new ItemStack(Items.LEATHER_BOOTS),take(p(s),Items.COPPER_INGOT,1),take(p(s),FungalGarden.GILLS,1),take(p(s),SporebackContent.DEW,1))),BelowkeeperEquipment.GREAVES);
   w.getServer().runOnServer(s -> hand(p(s),take(p(s),BelowkeeperEquipment.GREAVES,1)));c.waitTicks(5);c.runOnClient(mc -> mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND));c.waitTicks(5);w.getServer().runOnServer(s -> check(p(s).getItemBySlot(EquipmentSlot.FEET).is(BelowkeeperEquipment.GREAVES),"Actually crafted equipment occupies the real feet slot"));
   // Use the genuinely acquired relic against the same territory, then spend genuinely acquired repair materials.
   ringEarned(c,w);
   click(c,w,marks[2],w.getServer().computeOnServer(s -> take(p(s),BelowkeeperEquipment.BELL,1)));
   w.getServer().runOnServer(s -> {check(p(s).getMainHandItem().getDamageValue()==0,"Restored authentic desk repairs real relic wear from an actual committed use");check(p(s).getInventory().countItem(SporebackContent.DEW)==1 && p(s).getInventory().countItem(FungalGarden.GILLS)==1 && p(s).getInventory().countItem(Items.COPPER_INGOT)==1,"Repair consumes exactly one genuinely gathered dew, harvested gill and copper");});
   repairReady=w.getServer().computeOnServer(s -> p(s).getAttachedOrElse(DrainhouseContent.REPAIR_READY,0L));
   check(repairReady>w.getServer().computeOnServer(s -> s.overworld().getGameTime()),"Real paid repair establishes its saved finite rest");
   // Wait ordinary elapsed time safely at the desk; no clock rewrites or source resets.
   c.waitTicks(BelowkeeperEquipment.BELL_REST+5);ringEarned(c,w);
   click(c,w,marks[2],w.getServer().computeOnServer(s -> take(p(s),BelowkeeperEquipment.BELL,1)));
   w.getServer().runOnServer(s -> {check(p(s).getMainHandItem().getDamageValue()==1,"Rest refuses a second genuinely worn bell even when all real payment materials remain");check(p(s).getAttachedOrElse(DrainhouseContent.REPAIR_READY,0L)==repairReady && p(s).getInventory().countItem(SporebackContent.DEW)==1 && p(s).getInventory().countItem(FungalGarden.GILLS)==1 && p(s).getInventory().countItem(Items.COPPER_INGOT)==1,"Repair refusal neither renews rest nor debits earned materials");});
   bellReady=w.getServer().computeOnServer(s -> p(s).getAttachedOrElse(BelowkeeperEquipment.BELL_READY,0L));
   c.takeScreenshot(TestScreenshotOptions.of("drainhouse_paid_restoration_and_equipment").disableCounterPrefix());mealReady=root.mealReady();save=w.getWorldSave();
  }
  try(var w=save.open()){c.waitTicks(25);w.getServer().runOnServer(s -> s.overworld().getChunkAt(rootHome));await(c,w,s -> s.overworld().getEntity(creature) instanceof RootmoltStrider,"Saved earned competitor loads before exact deadline verification");w.getServer().runOnServer(s -> {var survivor=(RootmoltStrider)s.overworld().getEntity(creature);check(survivor!=null && survivor.mealReady()==mealReady,"The same living competitor retains its actual meal deadline through restart");check(FungalInvestigation.knows(p(s),DrainhouseInvestigation.DONE) && p(s).getInventory().countItem(BelowkeeperEquipment.BELL)==1,"Full restart retains once-only paid acquisition");for(var mark:marks)check(((DrainhouseMarkEntity)s.overworld().getBlockEntity(mark)).authentic(),"Authentic clue provenance survives full restart");check(s.overworld().getBlockState(marks[2]).getValue(DrainhouseMark.RESTORED) && p(s).getItemBySlot(EquipmentSlot.FEET).is(BelowkeeperEquipment.GREAVES),"Real restored desk and equipped crafted boots survive restart");check(!DrainhouseInvestigation.restore(p(s),marks[2]) && p(s).getInventory().countItem(BelowkeeperEquipment.BELL)==1,"Reload cannot repeat the relic reward");check(p(s).getAttachedOrElse(DrainhouseContent.REPAIR_READY,0L)==repairReady && p(s).getAttachedOrElse(BelowkeeperEquipment.BELL_READY,0L)==bellReady,"Actual paid repair and bell deadlines persist exactly through restart");check(!DrainhouseInvestigation.repair(p(s),marks[2],p(s).getMainHandItem()) && p(s).getMainHandItem().getDamageValue()==1 && p(s).getInventory().countItem(SporebackContent.DEW)==1,"Restart retains paid-repair refusal and real saved wear/materials");});}
  nestedEarnedRestore(c,earnedBranch);
 }
 private void callbackRefusals(ClientGameTestContext c,TestSingleplayerContext w){
  click(c,w,marks[2],ItemStack.EMPTY);
  w.getServer().runOnServer(s->{var player=p(s);var l=s.overworld();var at=marks[2];
   unpaid(player);
   player.setGameMode(GameType.SPECTATOR);check(!DrainhouseInvestigation.restore(player,at),"Spectator cannot spend an otherwise fully earned restoration");unpaid(player);
   player.setGameMode(GameType.CREATIVE);check(!DrainhouseInvestigation.restore(player,at),"Creative mode cannot spend an otherwise fully earned restoration");unpaid(player);player.setGameMode(GameType.SURVIVAL);
   check(!DrainhouseInvestigation.restore(player,at,(level,pos,state)->{check(level.setBlock(pos,state,3),"Actual false-return callback changes the real desk");return false;}),"False-after-write restoration refuses payment and reward");
   unpaid(player);check(l.getBlockState(at).getValue(DrainhouseMark.RESTORED),"Refused callback's cosmetic world write is real");
   check(l.setBlock(at,l.getBlockState(at).setValue(DrainhouseMark.RESTORED,false),3),"Fixture resets only cosmetic desk state for another injected writer case");
   var home=player.position();
   check(!DrainhouseInvestigation.restore(player,at,(level,pos,state)->{boolean wrote=level.setBlock(pos,state,3);move(player,40,30,3);return wrote;}),"Actual callback teleport cancels post-write admission before debit or reward");
   unpaid(player);move(player,home.x,home.y,home.z);
   check(l.setBlock(at,l.getBlockState(at).setValue(DrainhouseMark.RESTORED,false),3) && l.getBlockEntity(at) instanceof DrainhouseMarkEntity mark && mark.authentic(),"Ordinary first-success packet retains original authentic provenance and unrestored desk");
  });
 }
 private static void unpaid(net.minecraft.server.level.ServerPlayer player){check(!FungalInvestigation.knows(player,DrainhouseInvestigation.DONE) && player.getInventory().countItem(BelowkeeperEquipment.BELL)==0 && player.getInventory().countItem(SporebackContent.DEW)==5 && player.getInventory().countItem(FungalGarden.GILLS)==5 && player.getInventory().countItem(Items.COPPER_INGOT)==7,"Refused writer leaves every genuinely earned material and the once-only reward unspent");}
 private void nestedEarnedRestore(ClientGameTestContext c,TestWorldSave branch){
  try(var w=branch.open()){
   c.waitTicks(25);click(c,w,marks[2],ItemStack.EMPTY);
   w.getServer().runOnServer(s->{var player=p(s);unpaid(player);boolean[] inner={false};int[] writers={0};
    boolean outer=DrainhouseInvestigation.restore(player,marks[2],(level,pos,state)->{
     writers[0]++;check(level.setBlock(pos,state,3),"Reentrant fixture actually restores its earned branch desk");
     inner[0]=DrainhouseInvestigation.restore(player,pos);return true;
    });
    check(inner[0] && !outer && writers[0]==1,"Real nested ordinary restoration succeeds once and stale outer continuation refuses");paidBranch(player);
    check(!DrainhouseInvestigation.restore(player,marks[2]),"Earned reentrant branch refuses repeated rewards");paidBranch(player);
   });
  }
  try(var w=branch.open()){c.waitTicks(15);w.getServer().runOnServer(s->{paidBranch(p(s));check(!DrainhouseInvestigation.restore(p(s),marks[2]),"Full branch reopen preserves once-only reentrant acquisition");paidBranch(p(s));});}
 }
 private static void paidBranch(net.minecraft.server.level.ServerPlayer player){check(FungalInvestigation.knows(player,DrainhouseInvestigation.DONE) && player.getInventory().countItem(BelowkeeperEquipment.BELL)==1 && player.getInventory().countItem(SporebackContent.DEW)==3 && player.getInventory().countItem(FungalGarden.GILLS)==3 && player.getInventory().countItem(Items.COPPER_INGOT)==3,"Reentrant actual earned branch retains exactly one reward and one material payment");}
 private static TestWorldSave copyEarnedWorld(ClientGameTestContext c,TestWorldSave saved){
  try{
   var source=saved.getSaveDirectory().toAbsolutePath().normalize();var target=java.nio.file.Files.createTempDirectory(source.getParent(),"drainhouse-earned-");int copied=0;
   try(var entries=java.nio.file.Files.walk(source)){
    for(var at:entries.toList()){
     var relative=source.relativize(at);if(relative.toString().equals("session.lock"))continue;var destination=target.resolve(relative);
     if(java.nio.file.Files.isDirectory(at))java.nio.file.Files.createDirectories(destination);
     else {java.nio.file.Files.copy(at,destination);check(java.nio.file.Files.mismatch(at,destination)==-1,"Closed earned branch file is byte-identical to the original saved journey");copied++;}
    }
   }
   check(copied>0,"Closed earned snapshot contains actual saved world and player files");
   dev.wildercord.Wildercord.LOGGER.info("DRAINHOUSE_EARNED_BRANCH source={} branch={} byteIdenticalFiles={}; copied after full shutdown, no progress or inventory injection",source,target,copied);
   // This installed native-test implementation has a verified public (context, Path) constructor.
   return new net.fabricmc.fabric.impl.client.gametest.world.TestWorldSaveImpl(c,target);
  }catch(java.io.IOException failure){throw new AssertionError("Copying the fully closed earned prepayment branch",failure);}
 }
 private void ringEarned(ClientGameTestContext c,TestSingleplayerContext w){boolean use=c.computeOnClient(mc -> mc.options.keyUse.isDown());try {w.getServer().runOnServer(s -> {if(!p(s).getMainHandItem().is(BelowkeeperEquipment.BELL))hand(p(s),take(p(s),BelowkeeperEquipment.BELL,1));move(p(s),root.getX()+2,root.getY(),root.getZ());});await(c,w,s -> root.pose()==RootmoltStrider.WARNING,"The same earned competitor gives a real territorial bell warning");int before=w.getServer().computeOnServer(s -> p(s).getMainHandItem().getDamageValue());c.runOnClient(mc -> {mc.options.keyUse.setDown(true);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);});c.waitTicks(24);w.getServer().runOnServer(s -> check(p(s).getMainHandItem().getDamageValue()==before+1 && root.pose()==RootmoltStrider.RECOVERING && !p(s).hasEffect(RootmoltContent.TETHER),"Actual full-second use spends earned bell wear and interrupts its living source"));}finally{c.runOnClient(mc -> mc.options.keyUse.setDown(use));}}
 private static void prepare(MinecraftServer s,BlockPos at){var plan=SpellCompiler.compile(List.of(Runes.TOUCH,Runes.HEAL));CastEngine.onHit(new Cast(p(s)),plan.root().groups.getFirst(),new Cast.Hit(List.of(),Vec3.atCenterOf(at),new Vec3(0,0,1),p(s).position(),at,Direction.UP,false),null);check(s.overworld().getBlockState(at).getValue(GlowcapBlock.AGE)==1,"Actual Life impact hook prepares a bud, not a mature crop or reward");}
}
