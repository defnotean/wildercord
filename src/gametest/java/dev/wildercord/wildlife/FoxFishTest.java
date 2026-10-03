package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;
public final class FoxFishTest implements FabricClientGameTest {
 private static Fox fox;private static UUID saved;private static double initial;
 @Override public void runTest(ClientGameTestContext c) {
  TestWorldSave save;
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(30);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");
   w.getServer().runOnServer(s -> {
    var l=s.overworld();var p=s.getPlayerList().getPlayers().getFirst();
    for(int x=-22;x<23;x++)for(int z=-10;z<20;z++) {l.setBlock(new BlockPos(x,100,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);l.setBlock(new BlockPos(x,101,z),Blocks.AIR.defaultBlockState(),2);}
    p.setGameMode(GameType.SURVIVAL);p.teleportTo(l,0,101,0,Set.<Relative>of(),0,0,false);
    for(var food:List.of(Items.COD,Items.SALMON,Items.COOKED_COD,Items.COOKED_SALMON,Items.TROPICAL_FISH)) {
     var f=EntityTypes.FOX.create(l,EntitySpawnReason.COMMAND);f.snapTo(2,101,3,0,0);f.setNoAi(true);l.addFreshEntity(f);f.getRandom().setSeed(112);
     var cf=Wildlife.CINDERFOX.create(l,EntitySpawnReason.COMMAND);cf.snapTo(4,101,3,0,0);cf.setNoAi(true);l.addFreshEntity(cf);
     check(cf.tames(new ItemStack(food)),"Cinderfox accepts "+food);
     for(int i=0;i<40 && f.getAttached(FoxCompanions.BOND)==null;i++) {p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(food,2));FoxCompanions.use(f,p,InteractionHand.MAIN_HAND);check(p.getMainHandItem().getCount()==1,"One fish per survival attempt");}
     check(f.getAttached(FoxCompanions.BOND)!=null,"Vanilla fox tames with "+food);
     for(int i=0;i<40 && !cf.isTame();i++) {p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(food,2));cf.mobInteract(p,InteractionHand.MAIN_HAND);check(p.getMainHandItem().getCount()==1,"Cinderfox spends one fish");}
     check(cf.isTame() && cf.isOwnedBy(p) && cf.isOrderedToSit(),"Cinderfox owner and sit");f.discard();cf.discard();
    }
    fox=EntityTypes.FOX.create(l,EntitySpawnReason.COMMAND);fox.snapTo(0,101,2,180,0);fox.setNoAi(true);l.addFreshEntity(fox);fox.getRandom().setSeed(112);
    p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.PUFFERFISH,2));check(!FoxCompanions.use(fox,p,InteractionHand.MAIN_HAND).consumesAction() && fox.getAttached(FoxCompanions.BOND)==null && p.getMainHandItem().getCount()==2,"Pufferfish cannot tame");
    p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COD,40));
   });
   c.waitTicks(6);
   for(int i=0;i<40 && w.getServer().computeOnServer(s -> fox.getAttached(FoxCompanions.BOND)==null);i++) {click(c,w);c.waitTicks(3);}
   check(w.getServer().computeOnServer(s -> fox.getAttached(FoxCompanions.BOND)!=null && fox.isSitting()),"Actual client fish packet tames and sits");
   c.waitTicks(4);check(c.computeOnClient(mc -> mc.level.getEntity(fox.getId()).getAttached(FoxCompanions.BOND)!=null),"Bond syncs to client");
   w.getServer().runOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();var b=fox.getAttached(FoxCompanions.BOND);fox.setAttached(FoxCompanions.BOND,new FoxCompanions.Bond(UUID.randomUUID(),true));p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);FoxCompanions.use(fox,p,InteractionHand.MAIN_HAND);check(fox.getAttached(FoxCompanions.BOND).sitting(),"Nonowner cannot toggle");p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SALMON,2));FoxCompanions.use(fox,p,InteractionHand.MAIN_HAND);check(p.getMainHandItem().getCount()==2 && !fox.getAttached(FoxCompanions.BOND).owner().equals(p.getUUID()),"Nonowner cannot steal with fish");fox.setAttached(FoxCompanions.BOND,b);fox.setHealth(5);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COOKED_COD,2));FoxCompanions.use(fox,p,InteractionHand.MAIN_HAND);check(fox.getHealth()==8 && p.getMainHandItem().getCount()==1,"Owner fish heals");p.setGameMode(GameType.CREATIVE);fox.setHealth(5);FoxCompanions.use(fox,p,InteractionHand.MAIN_HAND);check(p.getMainHandItem().getCount()==1,"Creative feeding retains fish");p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);});
   c.waitTicks(4);click(c,w);c.waitTicks(4);
   w.getServer().runOnServer(s -> {check(!fox.getAttached(FoxCompanions.BOND).sitting(),"Actual empty hand stands fox");fox.setNoAi(false);fox.teleportTo(-14,101,2);s.getPlayerList().getPlayers().getFirst().teleportTo(s.overworld(),0,101,2,Set.<Relative>of(),90,15,false);initial=fox.getX();});
   c.waitTicks(140);w.getServer().runOnServer(s -> {check(fox.getX()>initial+5,"Actual owner following moves fox: "+fox.position());fox.teleportTo(-2,101,2);fox.setDeltaMovement(Vec3.ZERO);});c.waitTicks(4);click(c,w);c.waitTicks(12);
   w.getServer().runOnServer(s -> {check(fox.getAttached(FoxCompanions.BOND).sitting() && fox.isSitting(),"Owner can sit an active fox");saved=fox.getUUID();});
   c.takeScreenshot(TestScreenshotOptions.of("fish_tamed_fox_sitting").disableCounterPrefix());save=w.getWorldSave();
  }
  try(var w=save.open()) {c.waitTicks(35);w.getServer().runOnServer(s -> {fox=(Fox)s.overworld().getEntity(saved);check(fox!=null && fox.getAttached(FoxCompanions.BOND).owner().equals(s.getPlayerList().getPlayers().getFirst().getUUID()) && fox.getAttached(FoxCompanions.BOND).sitting() && fox.isSitting(),"Full restart retains owner and ordered sit");});}
 }
 private static void click(ClientGameTestContext c,TestSingleplayerContext w) {int id=w.getServer().computeOnServer(s -> fox.getId());c.runOnClient(mc -> {var e=mc.level.getEntity(id);mc.gameMode.interact(mc.player,e,new EntityHitResult(e,e.getBoundingBox().getCenter()),InteractionHand.MAIN_HAND);});}
 private static void check(boolean b,String why) {if(!b)throw new AssertionError(why);}
}
