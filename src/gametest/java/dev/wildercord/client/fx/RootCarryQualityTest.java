package dev.wildercord.client.fx;

import dev.wildercord.wildlife.RootCarryFx;
import dev.wildercord.content.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Relative;
import java.util.*;

/** Actual registered client channel/renderer acceptance; paid owner behavior has a separate suite. */
public final class RootCarryQualityTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext c){var own=c.computeOnClient(mc->MagicQuality.own);var others=c.computeOnClient(mc->MagicQuality.others);
  try(var w=c.worldBuilder().create()){
   c.waitTicks(35);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("fill -6 100 -6 8 100 10 dirt");
   w.getServer().runOnServer(s->s.getPlayerList().getPlayers().getFirst().teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false));c.waitTicks(5);
   for(boolean mine:new boolean[]{true,false}){
    c.waitTicks(20);c.runOnClient(mc->{MagicQuality.own=MagicQuality.Level.MINIMAL;MagicQuality.others=MagicQuality.Level.FULL;mc.particleEngine.clearParticles();});
    w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var event=new RootCarryFx.Event(mine?p.getUUID():new UUID(4,2),s.overworld().dimension().identifier().toString(),new BlockPos(0,101,4),new BlockPos(0,101,4),false,s.overworld().getGameTime());
     var b=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),s.registryAccess());try{RootCarryFx.Event.CODEC.encode(b,event);check(event.equals(RootCarryFx.Event.CODEC.decode(b)),"Actual wire roundtrip retains source, exact cells and selection kind");}finally{b.release();}ServerPlayNetworking.send(p,event);
    });c.waitTicks(2);
    c.runOnClient(mc->{var live=particles(mc.particleEngine).stream().filter(Particle::isAlive).filter(p->p instanceof LifeParticle||p instanceof MaterialParticle).toList();check(live.size()==(mine?2:6),"Actual receiver uses source "+(mine?"own Minimal":"others Full")+" count instead of recipient identity: "+live.size());check(live.stream().anyMatch(p->p instanceof LifeParticle&&((LifeOption)field(p,LifeParticle.class,"material")).style()==LifeOption.VINE),"Actual quality keeps fine-root material");check(live.stream().anyMatch(p->p instanceof MaterialParticle&&((Integer)field(p,MaterialParticle.class,"style"))==MaterialOption.STONE),"Actual quality keeps soil-clod material");});
    c.waitTicks(20);c.runOnClient(mc->check(particles(mc.particleEngine).stream().noneMatch(p->p.isAlive()&&(p instanceof LifeParticle||p instanceof MaterialParticle)),"Selection playback and particles retire instead of renewing"));
   }
   c.runOnClient(mc->{MagicQuality.own=MagicQuality.Level.MINIMAL;mc.particleEngine.clearParticles();});
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();ServerPlayNetworking.send(p,new RootCarryFx.Event(p.getUUID(),s.overworld().dimension().identifier().toString(),new BlockPos(0,101,4),new BlockPos(3,101,4),true,s.overworld().getGameTime()));});c.waitTicks(8);
   c.runOnClient(mc->check(particles(mc.particleEngine).stream().filter(Particle::isAlive).filter(p->p instanceof MaterialParticle&&((Integer)field(p,MaterialParticle.class,"style"))==MaterialOption.STONE).anyMatch(p->{double x=(Double)field(p,Particle.class,"x"),y=(Double)field(p,Particle.class,"y");return x>1.8&&x<2.2&&y>101.6;}),"Actual Minimal transfer receiver lifts the clod through its finite midpoint"));
   c.waitTicks(22);c.runOnClient(mc->check(particles(mc.particleEngine).stream().noneMatch(p->p.isAlive()&&(p instanceof LifeParticle||p instanceof MaterialParticle)),"All three actual transfer beats expire"));
   for(boolean wrongDimension:new boolean[]{true,false}){
    c.runOnClient(mc->mc.particleEngine.clearParticles());w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();ServerPlayNetworking.send(p,new RootCarryFx.Event(p.getUUID(),wrongDimension?"minecraft:the_nether":s.overworld().dimension().identifier().toString(),new BlockPos(0,101,4),new BlockPos(0,101,4),false,wrongDimension?s.overworld().getGameTime():Math.max(0,s.overworld().getGameTime()-50)));});c.waitTicks(2);c.runOnClient(mc->check(particles(mc.particleEngine).stream().noneMatch(p->p.isAlive()&&(p instanceof LifeParticle||p instanceof MaterialParticle)),"Wrong dimension or stale registered packet produces no material"));
   }
  }finally{c.runOnClient(mc->{MagicQuality.own=own;MagicQuality.others=others;mc.particleEngine.clearParticles();});}
 }
 private static List<Particle> particles(ParticleEngine e){var out=new ArrayList<Particle>();for(var group:((Map<?,?>)field(e,ParticleEngine.class,"particles")).values())for(var p:(Queue<?>)field(group,ParticleGroup.class,"particles"))out.add((Particle)p);for(var p:(Queue<?>)field(e,ParticleEngine.class,"particlesToAdd"))out.add((Particle)p);return out;}
 private static Object field(Object o,Class<?> owner,String name){try{var f=owner.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
}
