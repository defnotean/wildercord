package dev.wildercord.wildlife;

import static dev.wildercord.wildlife.TidewardNative.*;
import dev.wildercord.client.fx.TidewardClient;
import dev.wildercord.net.TidewardCue;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Real registered private cosmetic transport controls. Actual paid route success belongs to BankSurveyLineTest. */
public final class TidewardPendingReceiverTest implements FabricClientGameTest {
 private static final BlockPos GAP=new BlockPos(0,100,1);
 private static final List<BlockPos> CELLS=List.of(new BlockPos(0,100,0),GAP,new BlockPos(0,100,2));
 @Override public void runTest(ClientGameTestContext c){try(var settings=new TidewardNative(c)){for(int mode=0;mode<4;mode++)control(c,mode);}}
 private void control(ClientGameTestContext c,int mode){try(var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");
  w.getServer().runOnServer(s->{var l=s.overworld();floor(l);l.setBlock(GAP,Blocks.AIR.defaultBlockState(),2);var p=player(s);p.setGameMode(GameType.SURVIVAL);place(p,2.5,101,2.5);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TidewardSurvey.ITEM));p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();var nether=s.getLevel(Level.NETHER);nether.setBlock(new BlockPos(0,109,0),Blocks.STONE.defaultBlockState(),2);for(int y=110;y<=114;y++)nether.setBlock(new BlockPos(0,y,0),Blocks.AIR.defaultBlockState(),2);});
  c.waitTicks(8);
  check(c.computeOnClient(mc->mc.level.getBlockState(GAP).isAir()&&mc.player.getMainHandItem().is(TidewardSurvey.ITEM)),"Actual replicated missing footing and held source establish pending-admission conditions");
  boolean frozen=w.getServer().computeOnServer(s->s.tickRateManager().isFrozen());
  try{
   w.getServer().runOnServer(s->s.tickRateManager().setFrozen(true));c.waitTicks(3);
   long serverAge=w.getServer().computeOnServer(s->s.overworld().getGameTime()),clientAge=c.computeOnClient(mc->mc.level.getGameTime());
   long accepted=c.computeOnClient(mc->TidewardClient.routeAccepted()),bursts=c.computeOnClient(mc->TidewardClient.routeBursts()),pieces=c.computeOnClient(mc->TidewardClient.routePieces()),reads=c.computeOnClient(mc->TidewardClient.routeTerrainReads());
   w.getServer().runOnServer(s->{
    var p=player(s);var original=s.overworld();long nonce=original.getGameTime();TidewardCue.route(p,nonce,CELLS);
    // Same server task preserves actual packet ordering: cue first, then invalidation, then block replication.
    if(mode==1)TidewardCue.clear(p,nonce);
    else if(mode==2){p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();}
    else if(mode==3)p.teleportTo(s.getLevel(Level.NETHER),.5,110,.5,Set.<Relative>of(),0,0,false);
    if(mode!=0)original.setBlock(GAP,Blocks.STONE.defaultBlockState(),2);
   });
   c.waitTicks(mode==3?20:12);
   check(c.computeOnClient(mc->TidewardClient.routeTerrainReads())>reads,"Actual registered cue reached geometry admission before its pending control");
   check(c.computeOnClient(mc->TidewardClient.routeAccepted())==accepted&&c.computeOnClient(mc->TidewardClient.routeBursts())==bursts&&c.computeOnClient(mc->TidewardClient.routePieces())==pieces,"Pending control emits no prefix or phantom acceptance: mode="+mode);
   if(mode==0){
    check(c.computeOnClient(mc->TidewardClient.routeTerrainReads())-reads<=5,"Invalid pending route has at most five local-tick geometry passes under stalled server age");
    check(w.getServer().computeOnServer(s->s.overworld().getGameTime())==serverAge&&c.computeOnClient(mc->mc.level.getGameTime())==clientAge,"Actual server and client world ages remained frozen while pending expired locally");
    w.getServer().runOnServer(s->s.overworld().setBlock(GAP,Blocks.STONE.defaultBlockState(),2));
   }else if(mode==2){
    check(c.computeOnClient(mc->mc.player.getMainHandItem().isEmpty()),"Actual held-source removal replicated to the owner");
    w.getServer().runOnServer(s->{var p=player(s);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TidewardSurvey.ITEM));p.inventoryMenu.broadcastChanges();});
   }else if(mode==3){
    check(c.computeOnClient(mc->mc.level.dimension().equals(Level.NETHER)),"Actual owner departed into a distinct client world");
    w.getServer().runOnServer(s->player(s).teleportTo(s.overworld(),2.5,101,2.5,Set.<Relative>of(),0,0,false));
   }
   c.waitTicks(mode==3?20:8);
   check(c.computeOnClient(mc->mc.level.dimension().equals(Level.OVERWORLD)&&mc.level.getBlockState(GAP).is(Blocks.STONE)),"Actual restored support is now present in the owner world");
   check(c.computeOnClient(mc->TidewardClient.routeAccepted())==accepted&&c.computeOnClient(mc->TidewardClient.routeBursts())==bursts&&c.computeOnClient(mc->TidewardClient.routePieces())==pieces,"Restoring support/source/world cannot resurrect the retired pending cue: mode="+mode);
   w.getServer().runOnServer(s->{var p=player(s);check(p.getAttachedOrElse(TidewardSurvey.READY,0L)==0,"Cosmetic transport controls do not create a paid tool rest");if(p.getMainHandItem().is(TidewardSurvey.ITEM))check(p.getMainHandItem().getDamageValue()==0,"Cosmetic transport controls do not create tool wear");});
  }finally{w.getServer().runOnServer(s->s.tickRateManager().setFrozen(frozen));}
 }}
}
