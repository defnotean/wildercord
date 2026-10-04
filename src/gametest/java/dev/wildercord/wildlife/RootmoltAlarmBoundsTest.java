package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Real AI warning and scheduled physical alarm; no pose, rest or attack-clock injection. */
public final class RootmoltAlarmBoundsTest implements FabricClientGameTest {
 private RootmoltStrider source;
 private final List<SporebackSnail> snails=new ArrayList<>();
 private long[] beforeThreat,beforeHidden;
 private List<UUID> ordered;
 public void runTest(ClientGameTestContext c){
  // Separate actual worlds provide fresh genuine warnings rather than forcing an attack deadline.
  try(var w=c.worldBuilder().create()){
   prepare(c,w.getServer(),32,false);
   awaitWarning(c,w.getServer());
   w.getServer().runOnServer(s->{
    ordered=snails.stream().sorted(Comparator.<SporebackSnail>comparingDouble(source::distanceToSqr).thenComparing(e->e.getUUID().toString())).map(e->e.getUUID()).toList();
    check(RootmoltContent.alarmCandidates(source).stream().map(e->e.getUUID()).toList().equals(ordered),"Actual WARNING has the complete thirty-two-receiver pool in deterministic order");
    snapshot();
   });
   awaitPulse(c,w.getServer());
   w.getServer().runOnServer(s->{
    for(int i=0;i<snails.size();i++)check(snails.get(i).pose()==2 && snails.get(i).threatReady()>beforeThreat[i] && snails.get(i).hiddenUntil()>beforeHidden[i],"The real scheduled warning physically hides every admitted receiver");
    snapshot();RootmoltContent.alarm(source);sameClocks("Repeated alarm cannot renew already hidden recipients");
   });
  }
  try(var w=c.worldBuilder().create()){
   prepare(c,w.getServer(),33,true);
   awaitWarning(c,w.getServer());
   w.getServer().runOnServer(s->{
    check(snails.subList(0,32).stream().allMatch(e->e.pose()==2) && snails.getLast().pose()!=2,"Thirty-two actual physical hides precede one eligible overflow snail");
    check(RootmoltContent.alarmCandidates(source).isEmpty(),"Hidden candidates cannot defeat the thirty-three-candidate refusal");snapshot();
   });
   awaitPulse(c,w.getServer());
   w.getServer().runOnServer(s->{
    sameClocks("The actual scheduled crowded warning changes no receiver clocks");
    check(snails.getLast().pose()!=2,"The eligible overflow receiver is not selected from a truncated prefix");
    // The first scheduled pulse occurs while this genuine twenty-eight-tick warning still has time.
    // This explicit authoritative call verifies immediate reopening, without fabricating a second pulse.
    check(source.pose()==RootmoltStrider.WARNING,"The original real warning still owns the reopening check");
    snails.getFirst().discard();var eligible=snails.getLast();
    check(RootmoltContent.alarmCandidates(source).equals(List.of(eligible)),"Removing one actual hidden overflow leaves a complete pool with its one eligible receiver");
    long oldThreat=eligible.threatReady(),oldHidden=eligible.hiddenUntil();
    RootmoltContent.alarm(source);
    check(eligible.pose()==2 && eligible.threatReady()>oldThreat && eligible.hiddenUntil()>oldHidden,"An authoritative alarm in the same genuine warning physically admits the reopened receiver");
    for(int i=1;i<32;i++)check(snails.get(i).threatReady()==beforeThreat[i] && snails.get(i).hiddenUntil()==beforeHidden[i],"Reopening does not renew hidden receiver deadlines");
   });
  }
 }
 private void prepare(ClientGameTestContext c,TestServerContext server,int count,boolean hiddenPrefix){
  snails.clear();c.waitTicks(25);server.runCommand("difficulty normal");server.runCommand("gamerule spawn_mobs false");
  server.runOnServer(s->{
   var l=s.overworld();var p=s.getPlayerList().getPlayers().getFirst();
   // Every eye ray remains inside this explicitly loaded central chunk, without chunk unload manipulation.
   var site=new BlockPos(8,30,8);l.getChunkAt(site);
   for(int x=3;x<=13;x++)for(int z=3;z<=13;z++){
    l.setBlock(new BlockPos(x,29,z),Blocks.MOSS_BLOCK.defaultBlockState(),2);
    for(int y=30;y<=32;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
    l.setBlock(new BlockPos(x,33,z),Blocks.STONE.defaultBlockState(),2);
   }
   p.setGameMode(GameType.SURVIVAL);p.setHealth(20);p.teleportTo(l,8.5,30,12.3,Set.<Relative>of(),180,0,false);
   source=RootmoltContent.STRIDER.create(l,EntitySpawnReason.COMMAND);check(source!=null,"Real Rootmolt created");source.snapTo(8.5,30,8.5,0,0);l.addFreshEntity(source);
   for(int i=0;i<count;i++){
    // Two separated rings keep actual collision boxes apart; the overflow sits on the opposite side of the player.
    double angle=(i%16)*Math.PI/8,radius=i<16?1.9:2.9;
    double x=i==32?8.5:8.5+Math.cos(angle)*radius,z=i==32?4.8:8.5+Math.sin(angle)*radius;
    var snail=SporebackContent.SNAIL.create(l,EntitySpawnReason.COMMAND);check(snail!=null,"Real Sporeback created");snail.setNoAi(true);snail.snapTo(x,30,z,0,0);l.addFreshEntity(snail);snails.add(snail);
    if(hiddenPrefix && i<32)check(snail.answerThreat() && snail.pose()==2,"Actual finite physical danger produces the hidden prefix");
   }
   source.setTarget(p);
  });
 }
 private void awaitWarning(ClientGameTestContext c,TestServerContext server){
  for(int i=0;i<20;i++){
   c.waitTicks(1);if(server.computeOnServer(s->source.pose()==RootmoltStrider.WARNING)){
    check(server.computeOnServer(s->source.isAlive() && source.getTarget()==s.getPlayerList().getPlayers().getFirst()),"Normal AI enters its actual committed warning against the Survival player");return;
   }
  }
  throw new AssertionError("Normal AI did not enter WARNING");
 }
 private void awaitPulse(ClientGameTestContext c,TestServerContext server){
  int next=server.computeOnServer(s->((source.tickCount/20)+1)*20);
  for(int i=0;i<22;i++){
   c.waitTicks(1);if(server.computeOnServer(s->source.tickCount>=next)){
    check(server.computeOnServer(s->source.pose()==RootmoltStrider.WARNING),"Actual scheduled pulse completed during the genuine warning window");return;
   }
  }
  throw new AssertionError("Native warning did not reach its scheduled twenty-tick alarm");
 }
 private void snapshot(){beforeThreat=snails.stream().mapToLong(SporebackSnail::threatReady).toArray();beforeHidden=snails.stream().mapToLong(SporebackSnail::hiddenUntil).toArray();}
 private void sameClocks(String why){for(int i=0;i<snails.size();i++)check(snails.get(i).threatReady()==beforeThreat[i] && snails.get(i).hiddenUntil()==beforeHidden[i],why);}
 private static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
}
