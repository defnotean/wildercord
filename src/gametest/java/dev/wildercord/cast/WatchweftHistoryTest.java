package dev.wildercord.cast;
import dev.wildercord.client.fx.CampConcordClient;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import java.util.*;
/** Controlled actual body/pool admission fixtures; primary PaidTest separately proves vanilla AI traversal. */
public final class WatchweftHistoryTest implements FabricClientGameTest {
 private boolean injectCrowd,departDuringClaim;private int protectionCalls;
 private boolean armAtBoundary,crossAtBoundary,removedAtBoundary;private long crossAt,callbackTick=-1;private UUID watchedOwner;
 private Zombie subject;private final List<Zombie> crowd=new ArrayList<>();
 public void runTest(ClientGameTestContext c){net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((l,p,at,state,be)->{if(injectCrowd&&at.equals(new BlockPos(0,100,0))&&l instanceof net.minecraft.server.level.ServerLevel level){injectCrowd=false;protectionCalls++;callbackTick=level.getServer().overworld().getGameTime();for(int i=0;i<25;i++){var z=zombie(level,4.6);level.addFreshEntity(z);crowd.add(z);}if(departDuringClaim&&p instanceof net.minecraft.server.level.ServerPlayer actual)actual.teleportTo(level.getServer().getLevel(net.minecraft.world.level.Level.NETHER),.5,101,.5,Set.<net.minecraft.world.entity.Relative>of(),0,0,false);}return true;});
 net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s->{
  long now=s.overworld().getGameTime();
  if(armAtBoundary&&now%40==0){var p=CampConcordNative.player(s);watchedOwner=p.getUUID();CampConcordNative.check(Watchweft.arm(new Cast(p),CampConcordNative.hit()),"Natural clock boundary arms actual complete observation");armAtBoundary=false;crossAt=now+40;crossAtBoundary=true;}
  if(crossAtBoundary&&now==crossAt-1){subject.setPos(.5,101,2.4);injectCrowd=true;departDuringClaim=true;crossAtBoundary=false;}
  if(callbackTick==now&&departDuringClaim)removedAtBoundary=!Watchweft.active(watchedOwner);
 });for(String scenario:List.of("initial_inside","saturation_gap","wall","callback_crowd","callback_world_crowd"))try(var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set midnight");long before=c.computeOnClient(mc->CampConcordClient.accepted(CampConcordFx.WARN));long idleBefore=c.computeOnClient(mc->CampConcordClient.accepted(CampConcordFx.IDLE));long obscuredBefore=c.computeOnClient(mc->CampConcordClient.accepted(CampConcordFx.OBSCURED));
  w.getServer().runOnServer(s->{CampConcordNative.arena(s);var p=CampConcordNative.player(s);subject=zombie(s.overworld(),scenario.equals("initial_inside")?2.4:4.5);subject.setTarget(p);s.overworld().addFreshEntity(subject);if(scenario.equals("callback_world_crowd")){protectionCalls=0;callbackTick=-1;removedAtBoundary=false;armAtBoundary=true;}else CampConcordNative.check(Watchweft.arm(new Cast(p),CampConcordNative.hit()),"Complete controlled initial baseline admitted");});
  if(scenario.equals("callback_world_crowd")){
   w.getServer().waitFor(s->protectionCalls==1,200);c.waitTicks(4);
   w.getServer().runOnServer(s->CampConcordNative.check(callbackTick%40==0&&removedAtBoundary&&!Watchweft.active(watchedOwner),"Actual dimension departure on natural IDLE boundary cancels watch within the same END tick"));
   // ARM is sent after the boundary tick's runtime sampler; no idle is emitted before the departure at the next boundary.
   CampConcordNative.check(c.computeOnClient(mc->CampConcordClient.accepted(CampConcordFx.WARN)==before&&CampConcordClient.accepted(CampConcordFx.OBSCURED)==obscuredBefore&&CampConcordClient.accepted(CampConcordFx.IDLE)==idleBefore),"No stale warning, obscured or idle geometry follows callback dimension departure");
   w.getServer().runOnServer(s->{subject.discard();crowd.forEach(Zombie::discard);crowd.clear();});continue;
  }
  c.waitTicks(22);
  CampConcordNative.check(c.computeOnClient(mc->CampConcordClient.accepted(CampConcordFx.WARN)==before),"Initially present or outside subject does not fabricate a crossing");
  if(scenario.equals("saturation_gap")){
   w.getServer().runOnServer(s->{for(int i=0;i<25;i++){var z=zombie(s.overworld(),4.6);s.overworld().addFreshEntity(z);crowd.add(z);}subject.setPos(.5,101,2.4);});c.waitTicks(22);w.getServer().runOnServer(s->{crowd.forEach(Zombie::discard);crowd.clear();});c.waitTicks(22);
   CampConcordNative.check(c.computeOnClient(mc->CampConcordClient.accepted(CampConcordFx.WARN)==before),"Raw benign saturation erases history; recovering inside must rebaseline rather than infer a crossing");
  }
  if(scenario.equals("callback_crowd")||scenario.equals("callback_world_crowd")){w.getServer().runOnServer(s->{injectCrowd=true;departDuringClaim=scenario.equals("callback_world_crowd");protectionCalls=0;subject.setPos(.5,101,2.4);});c.waitTicks(22);w.getServer().runOnServer(s->CampConcordNative.check(protectionCalls==1&&Watchweft.active(CampConcordNative.player(s).getUUID())!=scenario.equals("callback_world_crowd"),"One real warning protection callback filled the raw pool, preserving only the finite watch"));CampConcordNative.check(c.computeOnClient(mc->CampConcordClient.accepted(CampConcordFx.WARN)==before),"Fresh post-callback raw crowd admission refuses stale warning");w.getServer().runOnServer(s->{crowd.forEach(Zombie::discard);crowd.clear();});c.waitTicks(22);CampConcordNative.check(c.computeOnClient(mc->CampConcordClient.accepted(CampConcordFx.WARN)==before),"Removing callback crowd while subject is inside must rebaseline, not invent a missed crossing");}
  if(scenario.equals("callback_world_crowd"))CampConcordNative.check(c.computeOnClient(mc->CampConcordClient.accepted(CampConcordFx.OBSCURED)==obscuredBefore),"Departed warning owner never receives old-world obscured geometry in its new dimension");
  if(scenario.equals("wall")){w.getServer().runOnServer(s->{for(int x=-2;x<=3;x++)for(int y=101;y<=104;y++)s.overworld().setBlock(new BlockPos(x,y,1),Blocks.STONE.defaultBlockState(),2);subject.setPos(.5,101,2.4);});c.waitTicks(22);CampConcordNative.check(c.computeOnClient(mc->CampConcordClient.accepted(CampConcordFx.WARN)==before)&&w.getServer().computeOnServer(s->Watchweft.active(CampConcordNative.player(s).getUUID())),"Actual loaded opaque wall prevents hidden warning; source watch remains finite");}
  else if(!scenario.equals("callback_world_crowd")){
   w.getServer().runOnServer(s->subject.setPos(.5,101,4.5));c.waitTicks(22);w.getServer().runOnServer(s->subject.setPos(.5,101,2.4));c.waitTicks(22);
   CampConcordNative.check(c.computeOnClient(mc->CampConcordClient.accepted(CampConcordFx.WARN)==before+1),"Fresh complete observed outside then inside crossing warns exactly once");
  }
  w.getServer().runOnServer(s->{subject.discard();Watchweft.remove(CampConcordNative.player(s).getUUID(),false);});
 }finally{injectCrowd=false;departDuringClaim=false;armAtBoundary=false;crossAtBoundary=false;callbackTick=-1;crowd.clear();}}
 private static Zombie zombie(net.minecraft.server.level.ServerLevel l,double z){var e=new Zombie(l);e.setCustomName(Component.literal("Controlled crossing admission body"));e.setNoAi(true);e.setPos(.5,101,z);return e;}
}
