package dev.wildercord.cast;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.network.chat.Component;
import java.util.*;
/** Clearly labelled synchronous claim/pool admission fixtures; not a public paid delivery claim. */
public final class WatchweftAdmissionTest implements FabricClientGameTest {
 private boolean injecting;private int claims,nested;private String mode="idle";
 public void runTest(ClientGameTestContext c){var callback=new PlayerBlockBreakEvents.Before(){public boolean beforeBlockBreak(net.minecraft.world.level.Level l,net.minecraft.world.entity.player.Player actor,BlockPos at,net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.block.entity.BlockEntity be){
  if(!injecting||!(actor instanceof net.minecraft.server.level.ServerPlayer p)||!at.equals(new BlockPos(0,100,0)))return true;claims++;
  if(mode.equals("recursive")){nested+=Watchweft.arm(new Cast(p),CampConcordNative.hit())?1:0;}
  if(mode.equals("departure"))p.setPos(p.getX()+.2,p.getY(),p.getZ());return !mode.equals("deny");
 }};PlayerBlockBreakEvents.BEFORE.register(callback);
  try(var w=c.worldBuilder().create()){c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runOnServer(s->{CampConcordNative.arena(s);var p=CampConcordNative.player(s);
   for(String scenario:List.of("deny","departure","recursive")){mode=scenario;claims=0;nested=0;injecting=true;try{boolean result=Watchweft.arm(new Cast(p),CampConcordNative.hit());CampConcordNative.check(claims==1&&nested==0,"Whole-player lease prevents nested callback across different payments");CampConcordNative.check(result==scenario.equals("recursive"),"Claim denial/body departure refuses; harmless recursive callback permits one outer arm");}finally{injecting=false;}Watchweft.remove(p.getUUID(),false);p.removeAttached(Watchweft.READY);CampConcordNative.aim(p,new net.minecraft.world.phys.Vec3(.5,100.999,.5));}
   var benign=new ArrayList<Zombie>();for(int i=0;i<25;i++){var z=new Zombie(s.overworld());z.setCustomName(Component.literal("Raw benign prefix "+i));z.setNoAi(true);z.setPos(.5,101,4.5);s.overworld().addFreshEntity(z);benign.add(z);}CampConcordNative.check(!Watchweft.arm(new Cast(p),CampConcordNative.hit()),"Complete raw saturation refuses arming even when no entry targets owner");benign.forEach(Zombie::discard);CampConcordNative.check(Watchweft.arm(new Cast(p),CampConcordNative.hit()),"Actual complete pool recovery permits a fresh baseline");
  });w.getServer().runOnServer(s->Watchweft.remove(CampConcordNative.player(s).getUUID(),false));}
  finally{injecting=false;mode="idle";}
 }
}
