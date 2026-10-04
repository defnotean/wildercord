package dev.wildercord.cast;
import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.client.fx.LifeParticle;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.particle.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import static dev.wildercord.cast.NextSignatureNative.*;
/** Paid owner source metadata reaches actual local quality at a different recipient's wake. */
public final class LifeOutcomeSourceTest implements FabricClientGameTest {
 private static Pillager sleeper;
 private static ServerPlayer actor;
 @Override public void runTest(ClientGameTestContext c){
  var observed=new ArrayList<LifeOwnerEvents.Event>();var oldOwn=c.computeOnClient(mc->MagicQuality.own);var oldOthers=c.computeOnClient(mc->MagicQuality.others);
  try(var w=c.worldBuilder().create()){
   c.waitTicks(30);UUID connected=c.computeOnClient(mc->mc.player.getUUID());w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule natural_health_regeneration false");
   w.getServer().runOnServer(s->{actor=s.getPlayerList().getPlayer(connected);check(actor!=null,"Actual connected client player owns paid source fixture");var p=actor;floor(p);LifeOwnerEvents.observe(observed::add);p.setHealth(4);cast(p,Runes.SELF,Runes.HEAL);});c.waitTicks(8);
   w.getServer().runOnServer(s->{var p=actor;check(p.getHealth()>4,"Paid Heal actually restores health");check(observed.stream().anyMatch(e->e.rune().equals("heal")&&e.delta()>0&&p.getUUID().equals(e.source())),"Paid Heal records actual source UUID");observed.clear();
    p.level().setBlock(new BlockPos(1,100,0),Blocks.FARMLAND.defaultBlockState(),3);p.level().setBlock(new BlockPos(1,101,0),Blocks.WHEAT.defaultBlockState(),3);cast(p,Runes.SELF,Runes.GROW);});c.waitTicks(10);
   w.getServer().runOnServer(s->{var p=actor;check(observed.stream().anyMatch(e->e.rune().equals("grow")&&e.recipient()==null&&p.getUUID().equals(e.source())),"Actual changed cell keeps caster even with no recipient UUID");
    sleeper=EntityTypes.PILLAGER.create(p.level(),EntitySpawnReason.COMMAND);check(sleeper!=null,"Actual hostile different recipient");sleeper.setNoAi(true);sleeper.setPos(.5,101,5.5);p.level().addFreshEntity(sleeper);});c.waitTicks(12);
   for(boolean minimal:new boolean[]{false,true}){
    c.runOnClient(mc->{MagicQuality.own=minimal?MagicQuality.Level.MINIMAL:MagicQuality.Level.FULL;MagicQuality.others=minimal?MagicQuality.Level.FULL:MagicQuality.Level.MINIMAL;mc.particleEngine.clearParticles();});
    w.getServer().runOnServer(s->{observed.clear();cast(actor,Runes.BOLT,Runes.DROWSE);});c.waitTicks(24);
    w.getServer().runOnServer(s->check(CraftedRunes.asleep(sleeper),"Actual paid Bolt admits sleep on a different body"));
    c.runOnClient(mc->mc.particleEngine.clearParticles());
    w.getServer().runOnServer(s->{observed.clear();sleeper.hurtServer(s.overworld(),s.overworld().damageSources().magic(),1);check(!CraftedRunes.asleep(sleeper),"Real damage wakes body");
     var p=actor;check(observed.stream().anyMatch(e->e.rune().equals("drowse")&&e.moment()==LifeOwnerEvents.Moment.END&&sleeper.getUUID().equals(e.recipient())&&!e.recipient().equals(p.getUUID())&&p.getUUID().equals(e.source())),"Lifecycle transition preserves original actual cast source; never recipient identity");});
    c.waitTicks(2);int actual=c.computeOnClient(mc->(int)particles(mc.particleEngine).stream().filter(p->p.isAlive()&&p instanceof LifeParticle).count());
    int[] expected={0};LifeOutcomes.draw(new LifeOutcomes.Observation("drowse",LifeOutcomes.Moment.END,Vec3.ZERO,null,1,0,0),minimal,(option,at)->expected[0]++);
    check(actual==expected[0],"Actual paid different-recipient wake reaches client with own "+(minimal?"Minimal":"Full")+" recipe: "+actual+"/"+expected[0]);c.waitTicks(12);
   }
  }finally{LifeOwnerEvents.clear();observed.clear();sleeper=null;actor=null;c.runOnClient(mc->{MagicQuality.own=oldOwn;MagicQuality.others=oldOthers;});}
 }
 private static List<Particle> particles(ParticleEngine e){var out=new ArrayList<Particle>();for(var group:((Map<?,?>)field(e,ParticleEngine.class,"particles")).values())for(var p:(Queue<?>)field(group,ParticleGroup.class,"particles"))out.add((Particle)p);for(var p:(Queue<?>)field(e,ParticleEngine.class,"particlesToAdd"))out.add((Particle)p);return out;}
 private static Object field(Object o,Class<?> type,String name){try{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
}
