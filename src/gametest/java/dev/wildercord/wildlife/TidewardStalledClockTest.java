package dev.wildercord.wildlife;

import static dev.wildercord.wildlife.TidewardNative.*;
import dev.wildercord.client.fx.*;
import dev.wildercord.net.TidewardCue;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.util.List;

/** Real registered private cue under actual frozen server time; this is rendering, not paid acquisition proof. */
public final class TidewardStalledClockTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext c){
  try(var settings=new TidewardNative(c);var w=c.worldBuilder().create()){
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");
   w.getServer().runOnServer(s->{floor(s.overworld());player(s).setGameMode(GameType.SURVIVAL);place(player(s),2.5,101,2.5);player(s).setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TidewardSurvey.ITEM));});c.waitTicks(6);
   boolean frozen=w.getServer().computeOnServer(s->s.tickRateManager().isFrozen());
   try{
    w.getServer().runOnServer(s->s.tickRateManager().setFrozen(true));c.waitTicks(3);
    long serverTime=w.getServer().computeOnServer(s->s.overworld().getGameTime());long clientTime=c.computeOnClient(mc->mc.level.getGameTime());
    for(var quality:new MagicQuality.Level[]{MagicQuality.Level.FULL,MagicQuality.Level.MINIMAL}){
     c.runOnClient(mc->{MagicQuality.own=quality;MagicQuality.others=quality==MagicQuality.Level.FULL?MagicQuality.Level.MINIMAL:MagicQuality.Level.FULL;mc.particleEngine.clearParticles();});
     long bursts=c.computeOnClient(mc->TidewardClient.routeBursts()),pieces=c.computeOnClient(mc->TidewardClient.routePieces()),reads=c.computeOnClient(mc->TidewardClient.routeTerrainReads()),cancelled=c.computeOnClient(mc->TidewardClient.routeCancelled());
     w.getServer().runOnServer(s->TidewardCue.route(player(s),s.overworld().getGameTime(),List.of(new BlockPos(0,100,0),new BlockPos(0,100,1),new BlockPos(0,100,2),new BlockPos(0,100,3),new BlockPos(0,100,4))));
     c.waitTicks(115);
     check(w.getServer().computeOnServer(s->s.overworld().getGameTime())==serverTime&&c.computeOnClient(mc->mc.level.getGameTime())==clientTime,"Actual server and client world ages remained frozen during local playback");
     check(c.computeOnClient(mc->TidewardClient.routeBursts())-bursts==10,"Actual receiver emits exactly ten scheduled bursts despite frozen server age");
     check(c.computeOnClient(mc->TidewardClient.routePieces())-pieces==(quality==MagicQuality.Level.FULL?260:100),"Each genuine burst retains bounded26 Full or10 Minimal pieces");
     check(c.computeOnClient(mc->TidewardClient.routeTerrainReads())-reads<=101,"Complete route geometry is queried at most once per local playback tick");
     check(c.computeOnClient(mc->TidewardClient.routeCancelled())==cancelled+1,"Actual receiver retires the route after its finite local lifetime");
     long finished=c.computeOnClient(mc->TidewardClient.routeBursts());c.waitTicks(20);check(c.computeOnClient(mc->TidewardClient.routeBursts())==finished,"No stale world age resurrects expired playback");
    }
    long before=c.computeOnClient(mc->TidewardClient.routeBursts());w.getServer().runOnServer(s->TidewardCue.route(player(s),s.overworld().getGameTime(),List.of(new BlockPos(0,100,0),new BlockPos(0,100,1))));c.waitTicks(3);
    w.getServer().runOnServer(s->player(s).setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY));c.waitTicks(5);
    long stopped=c.computeOnClient(mc->TidewardClient.routeBursts());check(stopped==before+1,"Actual held-stack removal clears the admitted route before its next burst");c.waitTicks(20);check(c.computeOnClient(mc->TidewardClient.routeBursts())==stopped,"Removing the actual held tool cannot retain a stale route");
    w.getServer().runOnServer(s->{player(s).setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TidewardSurvey.ITEM));player(s).inventoryMenu.broadcastChanges();});c.waitTicks(5);
    w.getServer().runOnServer(s->TidewardCue.route(player(s),s.overworld().getGameTime(),List.of(new BlockPos(0,100,0),new BlockPos(0,100,1))));c.waitTicks(3);
    long worldBursts=c.computeOnClient(mc->TidewardClient.routeBursts());w.getServer().runOnServer(s->player(s).teleportTo(s.getLevel(net.minecraft.world.level.Level.NETHER),.5,110,.5,java.util.Set.<net.minecraft.world.entity.Relative>of(),0,0,false));c.waitTicks(15);
    check(c.computeOnClient(mc->mc.level.dimension().equals(net.minecraft.world.level.Level.NETHER)),"Actual player changed client world during frozen-clock playback");check(c.computeOnClient(mc->TidewardClient.routeBursts())==worldBursts,"Old-world route emits no further burst after genuine dimension departure");
   }finally{w.getServer().runOnServer(s->s.tickRateManager().setFrozen(frozen));}
  }
 }
}
