package dev.wildercord.cast;

import dev.wildercord.cast.feel.*;
import dev.wildercord.client.fx.*;
import dev.wildercord.spell.*;
import dev.wildercord.wildlife.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.particle.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Genuine Root Carry mutation wire plus actual whole-group/mixed fallback dispatch. Helper calls are not paid cast proof. */
public final class RootCarryOutcomeOwnershipTest implements FabricClientGameTest {
 private static final BlockPos FROM=new BlockPos(0,101,0),TO=new BlockPos(3,101,0);
 @Override public void runTest(ClientGameTestContext c){
  var observed=new ArrayList<LifeOwnerEvents.Event>();var old=c.computeOnClient(mc->MagicQuality.own);
  try(var w=c.worldBuilder().create()){
   c.waitTicks(30);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.overworld(),2,101,2.5,Set.<Relative>of(),0,0,false);
    for(int x=-4;x<=8;x++)for(int z=-5;z<=5;z++){s.overworld().setBlock(new BlockPos(x,100,z),Blocks.DIRT.defaultBlockState(),2);for(int y=101;y<=104;y++)s.overworld().setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}
    s.overworld().setBlock(FROM,EmberContent.FERN.defaultBlockState(),2);LifeOwnerEvents.observe(observed::add);
    var root=Signatures.get("wildercord:root_carry");check(root!=null&&root.ownsOutcomeBody()&&!root.replaces(Phase.IMPACT)&&!root.replaces(Phase.AFTERMATH),"Dedicated outcome ownership preserves uncovered mixed phase defaults");
   });c.waitTicks(8);c.runOnClient(mc->{mc.particleEngine.clearParticles();MagicQuality.own=MagicQuality.Level.MINIMAL;});
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(RootCarry.apply(new Cast(p),hit(FROM))==RootCarry.Result.SELECTED,"Real young root selection admits its dedicated owner wire");check(p.getAttached(RootCarry.SELECT)!=null&&s.overworld().getBlockState(FROM).is(EmberContent.FERN)&&s.overworld().getBlockState(TO).isAir(),"Selection observes unchanged actual source and empty destination");check(observed.isEmpty(),"Root selection invents no Life healing/growth observation");});c.waitTicks(2);
   c.runOnClient(mc->{check(livingMaterials(mc)>=2,"Actual registered dedicated selection receiver emits own-Minimal soil/root material");});c.waitTicks(14);c.runOnClient(mc->mc.particleEngine.clearParticles());
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(RootCarry.apply(new Cast(p),hit(TO.below()))==RootCarry.Result.MOVED,"Second genuine owner operation moves existing root");check(s.overworld().getBlockState(FROM).isAir()&&s.overworld().getBlockState(TO).is(EmberContent.FERN)&&p.getAttached(RootCarry.SELECT)==null,"Actual two-cell mutation retains exactly one root");check(observed.isEmpty(),"Actual move has dedicated outcome ownership without false heal/grow delta");});c.waitTicks(2);
   c.runOnClient(mc->check(livingMaterials(mc)>=2,"Actual admitted movement reaches dedicated source-qualified cosmetic receiver"));c.waitTicks(18);c.runOnClient(mc->mc.particleEngine.clearParticles());
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(RootCarry.apply(new Cast(p),hit(TO))==RootCarry.Result.REFUSED,"Real persistent transfer rest refuses another operation");});c.waitTicks(2);c.runOnClient(mc->check(livingMaterials(mc)==0,"Refused owner creates no successful dedicated material"));
   var root=Runes.get("wildercord:root_carry").orElseThrow();var owned=feel(List.of(Runes.BOLT,root));var mixed=feel(List.of(Runes.BOLT,root,Runes.HARM));
   check(Signatures.authoredOutcomes(owned)&&!Signatures.authoredOutcomes(mixed)&&mixed.band()!=Band.S,"Exact dedicated ownership refuses uncovered mixed group and enables real aftermath size branch");
   var at=new Vec3(.5,102,5.5);long alone=w.getServer().computeOnServer(s->{VisualMetrics.reset();Feels.impact(s.overworld(),at,Vfx.theme("life").with(owned),1);return packets();});check(alone==0,"Wholly dedicated collision cannot fabricate successful root body");c.waitTicks(12);
   long generic=w.getServer().computeOnServer(s->{VisualMetrics.reset();Vfx.impactDefault(s.overworld(),at,Vfx.theme("life").with(mixed),1,true);return packets();});check(generic>0,"Actual direct generic impact positive control emits native packets");c.waitTicks(16);c.runOnClient(mc->mc.particleEngine.clearParticles());
   long uncovered=w.getServer().computeOnServer(s->{VisualMetrics.reset();Feels.impact(s.overworld(),at,Vfx.theme("life").with(mixed),1);return packets();});check(uncovered>generic,"Actual uncovered mixed dispatch retains impact plus additional default Life aftermath packets");c.waitTicks(2);
   c.runOnClient(mc->check(!particles(mc.particleEngine).isEmpty(),"Mixed uncovered fallback actually reaches native client renderer"));
  }finally{LifeOwnerEvents.clear();observed.clear();c.runOnClient(mc->MagicQuality.own=old);}
 }
 private static Feel feel(List<RuneDef> runes){return Feel.of(SpellCompiler.compile(runes).root().groups.getFirst(),24,0);}
 private static Cast.Hit hit(BlockPos at){return new Cast.Hit(List.of(),Vec3.atCenterOf(at),new Vec3(0,-1,0),Vec3.ZERO,at,Direction.UP,false);}
 private static long packets(){return ((Number)field(null,VisualMetrics.class,"particles")).longValue();}
 private static int livingMaterials(net.minecraft.client.Minecraft mc){return (int)particles(mc.particleEngine).stream().filter(p->p.isAlive()&&(p instanceof LifeParticle||p instanceof MaterialParticle)).count();}
 private static List<Particle> particles(ParticleEngine engine){var out=new ArrayList<Particle>();for(var group:((Map<?,?>)field(engine,ParticleEngine.class,"particles")).values())for(var p:(Queue<?>)field(group,ParticleGroup.class,"particles"))out.add((Particle)p);for(var p:(Queue<?>)field(engine,ParticleEngine.class,"particlesToAdd"))out.add((Particle)p);return out;}
 private static Object field(Object value,Class<?> type,String name){try{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(value);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
}
