package dev.wildercord.client.fx;

import dev.wildercord.cast.*;
import dev.wildercord.config.*;
import dev.wildercord.content.*;
import dev.wildercord.net.WildercordNetworking;
import dev.wildercord.player.*;
import dev.wildercord.spell.*;
import dev.wildercord.wildlife.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Review frames from real paid owner outcomes; supplied rune/habitat, no cosmetic packet injection. */
public final class RootCarryPresentationTest implements FabricClientGameTest {
 private static final BlockPos A=new BlockPos(0,101,0),B=new BlockPos(3,101,0);
 private record Receipt(long received,RootCarryFx.Event event){}
 public void runTest(ClientGameTestContext c){
  var quality=c.computeOnClient(mc->MagicQuality.own);boolean hidden=c.computeOnClient(mc->mc.gui.hud.isHidden());
  int[] window=c.computeOnClient(mc->new int[]{mc.getWindow().getWidth(),mc.getWindow().getHeight()});
  try {
   c.runOnClient(mc->{mc.getWindow().setWindowed(1920,1080);mc.resizeGui();if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();});
   for(var q:List.of(MagicQuality.Level.FULL,MagicQuality.Level.MINIMAL))try(var w=c.worldBuilder().create()){
    c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");w.getServer().runCommand("time set 6000");
    var original=w.getServer().computeOnServer(srv->Config.get());
    try {
     var rune=Runes.get("wildercord:root_carry").orElseThrow();
     w.getServer().runOnServer(srv->{current(copy(original,Map.of("manaRegenMultiplier",0.0)));var l=srv.overworld();var actor=p(srv);actor.setGameMode(GameType.SURVIVAL);actor.getInventory().clearContent();
      for(int x=-4;x<=8;x++)for(int z=-5;z<=5;z++){l.setBlock(new BlockPos(x,100,z),Blocks.STONE_BRICKS.defaultBlockState(),2);for(int y=101;y<=105;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}
      l.setBlock(A.below(),Blocks.DIRT.defaultBlockState(),2);l.setBlock(B.below(),Blocks.DIRT.defaultBlockState(),2);l.setBlock(A,EmberContent.FERN.defaultBlockState(),2);
      Spellbooks.setCord(actor,new ItemStack(WildercordItems.ECHO_CORD));Spellbooks.set(actor,Spellbooks.get(actor).withStarterGiven().learn(Runes.TOUCH.id()).learn(rune.id()));
      check(SpellCaster.edit(actor,0,List.of(Runes.TOUCH.id(),rune.id()))==null,"Actual editor admits supplied learned review spell");Spellbooks.setMana(actor,200);aim(actor,new Vec3(.5,101.15,.5));
     });c.waitTicks(5);c.runOnClient(mc->{mc.gui.setScreen(null);mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);MagicQuality.own=q;if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();mc.particleEngine.clearParticles();});
     int price=w.getServer().computeOnServer(srv->cost(p(srv),List.of(Runes.TOUCH,rune)));cast(c);
     var selected=receive(c,false);beat(c,selected,0,"root_carry_"+q.name().toLowerCase(Locale.ROOT)+"_paid_selection");
     w.getServer().runOnServer(srv->{check(Spellbooks.mana(p(srv))==200-price,"Selection is actual exact paid CastSpell admission");check(p(srv).getAttached(RootCarry.SELECT)!=null&&p(srv).getAttached(RootCarry.SELECT).at().equals(A),"Real selection retains exact source");check(srv.overworld().getBlockState(A).is(EmberContent.FERN)&&srv.overworld().getBlockState(B).isAir(),"Selection has not relocated the supplied root");});
     int wait=w.getServer().computeOnServer(srv->(int)Math.max(1,Spellbooks.readyAt(p(srv),0)-srv.overworld().getGameTime()+1));c.waitTicks(wait);
     w.getServer().runOnServer(srv->aim(p(srv),new Vec3(3.5,100.99,.5)));c.waitTicks(3);int secondPrice=w.getServer().computeOnServer(srv->cost(p(srv),List.of(Runes.TOUCH,rune)));c.runOnClient(mc->{mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);mc.particleEngine.clearParticles();});cast(c);
     var moved=receive(c,true);c.runOnClient(mc->look(mc,new Vec3(2,101.45,.5)));
     for(int b=0;b<3;b++)beat(c,moved,b,"root_carry_"+q.name().toLowerCase(Locale.ROOT)+"_paid_transfer_beat_"+b);
     w.getServer().runOnServer(srv->{check(Spellbooks.mana(p(srv))==200-price-secondPrice,"Actual selection and transfer each pay once");check(srv.overworld().getBlockState(A).isAir()&&srv.overworld().getBlockState(B).is(EmberContent.FERN),"Actual paid receiver corresponds to committed two-cell transfer");check(p(srv).getAttached(RootCarry.SELECT)==null&&p(srv).getAttachedOrElse(RootCarry.READY,0L)>srv.overworld().getGameTime(),"Actual outcome consumes selection and starts finite rest");});
     c.waitTicks(22);c.runOnClient(mc->check(particles(mc.particleEngine).stream().noneMatch(RootCarryPresentationTest::rootMaterial),"Actual authored particles retire after finite beats"));
     c.takeScreenshot("root_carry_"+q.name().toLowerCase(Locale.ROOT)+"_paid_settled");
    } finally {w.getServer().runOnServer(srv->current(original));}
   }
  } finally {c.runOnClient(mc->{MagicQuality.own=quality;if(mc.gui.hud.isHidden()!=hidden)mc.gui.hud.toggle();mc.getWindow().setWindowed(window[0],window[1]);mc.resizeGui();});}
 }
 private static Receipt receive(ClientGameTestContext c,boolean moved){
  c.waitFor(mc->receipt(mc,moved)!=null,120);return c.computeOnClient(mc->{var r=receipt(mc,moved);check(r!=null,"Actual owner packet still retained at receiver admission");return r;});
 }
 private static Receipt receipt(Minecraft mc,boolean moved){
  for(var pending:(List<?>)field(null,RootCarryClient.class,"ACTIVE")){
   var event=(RootCarryFx.Event)method(pending,"event");
   if(event.moved()==moved&&event.source().equals(mc.player.getUUID())&&event.from().equals(A)&&event.to().equals(moved?B:A))return new Receipt(((Number)method(pending,"received")).longValue(),event);
  }return null;
 }
 private static void beat(ClientGameTestContext c,Receipt receipt,int beat,String name){
  long wanted=beat*6L+3;c.waitFor(mc->((Number)field(null,RootCarryClient.class,"playbackTick")).longValue()-receipt.received()>=wanted,40);
  var captured=c.computeOnClient(mc->{long age=((Number)field(null,RootCarryClient.class,"playbackTick")).longValue()-receipt.received();check(age>=wanted&&age<=wanted+2,"Actual finite receiver beat captured without renewed clock: "+name+" age="+age);
   double t=receipt.event().moved()?beat/2.0:0;var at=new Vec3(.5+3*t,101.18+Math.sin(t*Math.PI)*.65,.5);
   var live=particles(mc.particleEngine).stream().filter(RootCarryPresentationTest::rootMaterial).filter(p->Math.abs(((Number)field(p,Particle.class,"x")).doubleValue()-at.x)<.3&&Math.abs(((Number)field(p,Particle.class,"z")).doubleValue()-at.z)<.3&&Math.abs(((Number)field(p,Particle.class,"y")).doubleValue()-at.y)<.3).toList();
   check(live.stream().anyMatch(p->p instanceof MaterialParticle),"Real admitted beat retains actual soil clod at authored path point");check(live.stream().anyMatch(p->p instanceof LifeParticle),"Real admitted beat retains actual roots at authored path point");
   mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);
   var drawn=pixels(mc,name);mc.particleEngine.clearParticles();var background=pixels(mc,name+"_background");
   return drawn.thenCombine(background,(a,b)->{check(a.length==b.length,"Matched native render dimensions");int changed=0;for(int i=0;i<a.length;i++){int x=a[i],y=b[i];int d=Math.abs((x>>16&255)-(y>>16&255))+Math.abs((x>>8&255)-(y>>8&255))+Math.abs((x&255)-(y&255));if(d>20)changed++;}check(changed>10,"Actual paid authored outcome changes visible native pixels: "+name+" changed="+changed);return (Void)null;});
  });c.waitFor(mc->captured.isDone());captured.join();
 }
 private static boolean rootMaterial(Particle p){return p.isAlive()&&((p instanceof MaterialParticle&&((Integer)field(p,MaterialParticle.class,"style"))==MaterialOption.STONE)||(p instanceof LifeParticle&&((LifeOption)field(p,LifeParticle.class,"material")).style()==LifeOption.VINE));}
 private static CompletableFuture<int[]> pixels(Minecraft mc,String name){var result=new CompletableFuture<int[]>();mc.gameRenderer.update(net.minecraft.client.DeltaTracker.ONE);mc.gameRenderer.extract(net.minecraft.client.DeltaTracker.ONE,true);mc.gameRenderer.render();com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder().submit();net.minecraft.client.Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try(image){var path=java.nio.file.Path.of("screenshots",name+".png");java.nio.file.Files.createDirectories(path.getParent());image.writeToFile(path);result.complete(image.getPixels());}catch(Throwable e){result.completeExceptionally(e);}});return result;}
 private static List<Particle> particles(ParticleEngine e){var out=new ArrayList<Particle>();for(var group:((Map<?,?>)field(e,ParticleEngine.class,"particles")).values())for(var p:(Queue<?>)field(group,ParticleGroup.class,"particles"))out.add((Particle)p);for(var p:(Queue<?>)field(e,ParticleEngine.class,"particlesToAdd"))out.add((Particle)p);return out;}
 private static Object field(Object o,Class<?> owner,String name){try{var f=owner.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static Object method(Object o,String name){try{var m=o.getClass().getDeclaredMethod(name);m.setAccessible(true);return m.invoke(o);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void look(Minecraft mc,Vec3 at){var d=at.subtract(mc.player.getEyePosition());mc.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())));}
 private static int cost(ServerPlayer p,List<RuneDef> rs){return Heart.manaCost(p,SpellCompiler.compile(rs),Heart.secretCost(p,rs)*Mastery.costFactor(p,rs));}
 private static void cast(ClientGameTestContext c){c.runOnClient(mc->ClientPlayNetworking.send(new WildercordNetworking.CastSpell(0)));}
 private static void aim(ServerPlayer p,Vec3 target){var eye=new Vec3(2,101+p.getEyeHeight(),2.5);var d=target.subtract(eye);p.teleportTo(p.level(),2,101,2.5,Set.<Relative>of(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())),false);}
 private static ServerPlayer p(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
 @SuppressWarnings("unchecked") private static <T>T copy(T record,Map<String,Object> changes){try{var ps=record.getClass().getRecordComponents();var types=new Class<?>[ps.length];var args=new Object[ps.length];for(int i=0;i<ps.length;i++){types[i]=ps[i].getType();args[i]=changes.getOrDefault(ps[i].getName(),ps[i].getAccessor().invoke(record));}return (T)record.getClass().getDeclaredConstructor(types).newInstance(args);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void current(WildercordConfig config){try{var f=Config.class.getDeclaredField("current");f.setAccessible(true);f.set(null,config);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
}
