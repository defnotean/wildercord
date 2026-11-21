package dev.wildercord.client.fx;

import dev.wildercord.cast.LifeOutcomes;
import dev.wildercord.content.LifeOption;
import dev.wildercord.content.MaterialOption;
import dev.wildercord.net.LifeOutcomePayload;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.particle.*;
import net.minecraft.client.CameraType;
import dev.wildercord.client.compat.QuadParticleRenderState;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;

/** Real clientbound quality/channel acceptance, separate from paid owner gameplay suites. */
public final class LifeOutcomeQualityTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext c){
  var oldOwn=c.computeOnClient(mc->MagicQuality.own);var oldOthers=c.computeOnClient(mc->MagicQuality.others);
  var oldCamera=c.computeOnClient(mc->mc.options.getCameraType());
  try(var w=c.worldBuilder().create()){
   c.waitTicks(40);c.runOnClient(mc->{mc.setCameraEntity(mc.player);mc.options.setCameraType(CameraType.FIRST_PERSON);});w.getServer().runCommand("gamerule doMobSpawning false");
   w.getServer().runCommand("fill -10 100 -10 10 100 10 stone");
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);
    var known=new LifeOutcomePayload(p.getUUID(),s.overworld().dimension().location().toString(),"heal",LifeOutcomes.Moment.APPLY,new Vec3(.5,102,4.5),null,1,2,s.overworld().getGameTime(),new Vec3(0,0,-1),.38);
    var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),s.registryAccess());
    try{LifeOutcomePayload.CODEC.encode(buffer,known);check(known.equals(LifeOutcomePayload.CODEC.decode(buffer)),"Wire preserves actual source and observed values");}finally{buffer.release();}
    check(LifeOutcomePayload.own(p.getUUID(),p.getUUID())&&!LifeOutcomePayload.own(null,p.getUUID())&&!LifeOutcomePayload.own(new UUID(4,2),p.getUUID()),"Unknown/foreign source never treated as recipient-owned");
    reject(()->new LifeOutcomePayload(null,"minecraft:overworld","heal",LifeOutcomes.Moment.APPLY,new Vec3(Double.NaN,0,0),null,1,2,1));
    reject(()->new LifeOutcomePayload(null,"minecraft:overworld","heal",LifeOutcomes.Moment.APPLY,Vec3.ZERO,new Vec3(65,0,0),1,2,1));
    reject(()->new LifeOutcomePayload(null,"minecraft:overworld","heal",LifeOutcomes.Moment.REFUSED,Vec3.ZERO,null,1,2,1));
    reject(()->new LifeOutcomePayload(null,"minecraft:overworld","heal",LifeOutcomes.Moment.APPLY,Vec3.ZERO,null,7,2,1));
    reject(()->new LifeOutcomePayload(null,"minecraft:overworld","heal",LifeOutcomes.Moment.APPLY,Vec3.ZERO,null,1,2,1,Vec3.ZERO,.38));
    reject(()->new LifeOutcomePayload(null,"minecraft:overworld","heal",LifeOutcomes.Moment.APPLY,Vec3.ZERO,null,1,2,1,new Vec3(0,0,1),4.01));
   });c.waitTicks(8);
   // Opposing own/others settings make a recipient-as-source mistake observable.
   for(boolean own:new boolean[]{true,false}){
    c.runOnClient(mc->{MagicQuality.own=MagicQuality.Level.MINIMAL;MagicQuality.others=MagicQuality.Level.FULL;dev.wildercord.gametest.compat.GametestCompat.clearParticles(mc.particleEngine);});
    w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();ServerPlayNetworking.send(p,new LifeOutcomePayload(own?p.getUUID():new UUID(4,2),s.overworld().dimension().location().toString(),"heal",LifeOutcomes.Moment.APPLY,p.position().add(0,1,4),null,1,2,s.overworld().getGameTime(),new Vec3(0,0,-1),.38));});
    c.waitTicks(2);int actual=c.computeOnClient(mc->(int)particles(mc.particleEngine).stream().filter(p->p.isAlive()&&p instanceof LifeParticle).count());
    int[] expected={0};LifeOutcomes.draw(new LifeOutcomes.Observation("heal",LifeOutcomes.Moment.APPLY,Vec3.ZERO,null,1,2,0),own,(option,at)->expected[0]++);
    check(actual==expected[0],"Actual registered receiver selects "+(own?"own Minimal":"others Full")+" material count: "+actual+"/"+expected[0]);
    c.runOnClient(mc->{Vec3 anchor=mc.player.position().add(0,1,4),normal=new Vec3(0,0,-1);for(var p:particles(mc.particleEngine))if(p.isAlive()&&p instanceof LifeParticle){var at=new Vec3((double)field(p,Particle.class,"x"),(double)field(p,Particle.class,"y"),(double)field(p,Particle.class,"z"));check(at.subtract(anchor).dot(normal)>.34,"Actual packet renderer preserves rotated outward surface instead of legacy center");}});
   }
   c.runOnClient(mc->{MagicQuality.others=MagicQuality.Level.MINIMAL;dev.wildercord.gametest.compat.GametestCompat.clearParticles(mc.particleEngine);});
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();ServerPlayNetworking.send(p,new LifeOutcomePayload(null,s.overworld().dimension().location().toString(),"pulse_ferry",LifeOutcomes.Moment.PULSE,p.position().add(0,1,4),null,2,2,s.overworld().getGameTime()));});
   c.waitTicks(2);c.runOnClient(mc->{var live=particles(mc.particleEngine).stream().filter(Particle::isAlive).toList();check(live.stream().anyMatch(p->p instanceof LifeParticle),"Actual Minimal packet retains Life body");check(live.stream().anyMatch(p->p instanceof MaterialParticle&&((Integer)field(p,MaterialParticle.class,"style"))==MaterialOption.TIME),"Actual Minimal packet retains Time support");});
   c.runOnClient(mc->dev.wildercord.gametest.compat.GametestCompat.clearParticles(mc.particleEngine));
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();ServerPlayNetworking.send(p,new LifeOutcomePayload(p.getUUID(),"minecraft:the_nether","heal",LifeOutcomes.Moment.APPLY,p.position().add(0,1,4),null,1,2,s.overworld().getGameTime(),new Vec3(0,0,-1),.38));});
   c.waitTicks(2);c.runOnClient(mc->check(particles(mc.particleEngine).stream().noneMatch(p->p.isAlive()&&p instanceof LifeParticle),"Wrong-dimension packet produces no outcome"));
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();ServerPlayNetworking.send(p,new LifeOutcomePayload(p.getUUID(),s.overworld().dimension().location().toString(),"heal",LifeOutcomes.Moment.APPLY,p.getEyePosition(),null,1,2,s.overworld().getGameTime()));});
   c.waitTicks(2);c.runOnClient(mc->check(particles(mc.particleEngine).stream().noneMatch(p->p.isAlive()&&p instanceof LifeParticle),"First-person eye clearance remains enforced for every recipe point"));
   c.runOnClient(mc->{mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);dev.wildercord.gametest.compat.GametestCompat.clearParticles(mc.particleEngine);});c.waitTicks(2);
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();ServerPlayNetworking.send(p,new LifeOutcomePayload(new UUID(4,2),s.overworld().dimension().location().toString(),"heal",LifeOutcomes.Moment.APPLY,p.getEyePosition(),null,1,2,s.overworld().getGameTime()));});
   c.waitTicks(2);c.runOnClient(mc->check(particles(mc.particleEngine).stream().noneMatch(p->p.isAlive()&&p instanceof LifeParticle),"Foreign source retains eye clearance even in local third person"));
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();ServerPlayNetworking.send(p,new LifeOutcomePayload(p.getUUID(),s.overworld().dimension().location().toString(),"second_wind",LifeOutcomes.Moment.APPLY,p.getBoundingBox().getCenter(),null,1,0,s.overworld().getGameTime(),new Vec3(0,0,1),.38));});
   c.waitTicks(2);c.runOnClient(mc->{
    var live=particles(mc.particleEngine).stream().filter(p->p.isAlive()&&(p instanceof LifeParticle||p instanceof MaterialParticle)).toList();
    check(live.size()==3&&live.stream().anyMatch(p->p instanceof MaterialParticle),"Diagnostic owner packet keeps both Life and Time recipe ingredients in native third person");
    for(var p:live){var state=new CountingState();((dev.wildercord.client.compat.McQuadParticle)p).extract(state,mc.gameRenderer.getMainCamera(),1);check(!state.isEmpty(),"Actual native third-person extracts original recipe ingredient");}
    mc.options.setCameraType(CameraType.FIRST_PERSON);
    for(var p:live){var state=new CountingState();((dev.wildercord.client.compat.McQuadParticle)p).extract(state,mc.gameRenderer.getMainCamera(),1);check(state.isEmpty(),"Immediate first-person switch suppresses surviving Life and Time support alike");check(p.getLifetime()==(p instanceof LifeParticle?10:8),"Mode switches retain original recipe lifetimes");}
   });
  }finally{c.runOnClient(mc->{MagicQuality.own=oldOwn;MagicQuality.others=oldOthers;mc.options.setCameraType(oldCamera);});}
 }
 private static List<Particle> particles(ParticleEngine e){var out=new ArrayList<Particle>();for(var q:((Map<?,?>)field(e,ParticleEngine.class,"particles")).values())for(var p:(Queue<?>)q)out.add((Particle)p);for(var p:(Queue<?>)field(e,ParticleEngine.class,"particlesToAdd"))out.add((Particle)p);return out;}
 /** 1.21.11 keeps the quad list private; count the submissions instead. */
private static final class CountingState extends QuadParticleRenderState {
	int quads;
	@Override public void add(dev.wildercord.client.compat.McQuadParticle.Layer layer,
			float x, float y, float z, float xRot, float yRot, float zRot, float wRot,
			float scale, float u0, float u1, float v0, float v1, int color, int lightCoords) {
		if ((color >>> 24) != 0 && scale > 0F) quads++;
	}
	boolean isEmpty() { return quads == 0; }
}

private static Object field(Object o,Class<?> type,String name){try{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void reject(Runnable r){boolean refused=false;try{r.run();}catch(IllegalArgumentException expected){refused=true;}check(refused,"Malformed cosmetic payload rejected");}
 private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
}
