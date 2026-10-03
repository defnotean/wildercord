package dev.wildercord.client.fx;

import dev.wildercord.cast.SpellCaster;
import dev.wildercord.content.LightOption;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.util.List;
import java.util.Set;

/** Paid native preparation: authored bodies replace the generic Bolt orb and ray. */
public final class CleanProjectileFormationTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext c) {
  var previous=c.computeOnClient(mc -> MagicQuality.own);
  var camera=c.computeOnClient(mc -> mc.options.getCameraType());
  boolean hidden=c.computeOnClient(mc -> mc.gui.hud.isHidden());
  int width=c.computeOnClient(mc -> mc.getWindow().getScreenWidth());
  int height=c.computeOnClient(mc -> mc.getWindow().getScreenHeight());
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(40);
   w.getServer().runCommand("gamerule spawn_mobs false");
   w.getServer().runCommand("time set 6000");w.getServer().runCommand("weather clear");
   w.getServer().runCommand("fill -16 100 -12 16 100 20 polished_deepslate");
   w.getServer().runCommand("fill -12 101 8 12 109 8 gray_concrete");
   w.getServer().runOnServer(s -> {
    var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);
    p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);
    Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));
    var book=Spellbooks.get(p).withStarterGiven();for(var r:Runes.all())book=book.learn(r.id());Spellbooks.set(p,book);
   });c.waitTicks(15);
   c.runOnClient(mc -> {mc.getWindow().setWindowed(1280,720);mc.resizeGui();mc.options.setCameraType(CameraType.FIRST_PERSON);if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();mc.gui.toastManager().clear();});
   var background=c.computeOnClient(mc -> LifeFlightTest.snapshot(mc,"clean_body_background"));c.waitFor(mc -> background.isDone());background.join();
   for(var quality:List.of(MagicQuality.Level.FULL,MagicQuality.Level.MINIMAL)) {
    c.runOnClient(mc -> MagicQuality.own=quality);
    for(String effect:List.of("ember","chill","jolt","mixed")) {
     c.waitTicks(14);c.runOnClient(mc -> mc.particleEngine.clearParticles());
     w.getServer().runOnServer(s -> {
      var p=s.getPlayerList().getPlayers().getFirst();
      var ids=effect.equals("mixed")?List.of(Runes.BOLT.id(),Runes.EMBER.id(),Runes.UMBRA.id()):List.of(Runes.BOLT.id(),"wildercord:"+effect);
      SpellCaster.edit(p,0,ids);Spellbooks.setReadyAt(p,0,0);Spellbooks.setMana(p,100);
      float before=Spellbooks.mana(p);SpellCaster.cast(p,0);check(Spellbooks.mana(p)<before,"Actual Survival cast pays mana: "+effect);
     });c.waitTicks(4);
     var capture=c.computeOnClient(mc -> {
      int material=0,circles=0,lights=0,orbs=0,rays=0,arcs=0;var lightTrace=new java.util.ArrayList<String>();
      for(var particle:LifeFlightTest.particles(mc.particleEngine)) {
       if(!particle.isAlive())continue;
       if(particle instanceof SpellCircleParticle){circles++;check(LifeFlightTest.at(particle).subtract(mc.player.getEyePosition()).dot(mc.player.getLookAngle())<0,"Rear glyph: "+effect);}
       if(particle instanceof MaterialParticle){material++;check(LifeFlightTest.at(particle).distanceTo(mc.player.getEyePosition().add(0,-.65,3.2))<1.65,"Front authored assembly: "+effect);}
       // Rear glyph strokes are a separate assembly; inspect the front body volume.
       if(particle instanceof LightParticle && LifeFlightTest.at(particle).distanceTo(mc.player.getEyePosition().add(0,-.65,3.2))<1.65){lightTrace.add(LifeFlightTest.at(particle)+" kind="+LifeFlightTest.field(particle,LightParticle.class,"kind")+" life="+LifeFlightTest.field(particle,net.minecraft.client.particle.Particle.class,"lifetime"));lights++;int kind=((Number)LifeFlightTest.field(particle,LightParticle.class,"kind")).intValue();if(kind==LightOption.ORB)orbs++;if(kind==LightOption.RAY)rays++;if(kind==LightOption.ARC)arcs++;}
      }
      check(material>0 && circles>0,"Authored material and rear glyph retained: "+effect+" / "+quality);
      if(effect.equals("mixed"))check(orbs>0 && rays>0,"Uncovered Ember + Umbra retains generic Bolt fallback");
      else {
       check(orbs==0 && rays==0,"Pure authored Bolt has no generic orb or ray: "+effect+" "+lightTrace);
       if(effect.equals("jolt"))check(arcs>0,"Jolt retains its independently authored conducting arc");
       else check(lights==0,"Ember and Chill retain only their authored material assembly");
      }
      return quality==MagicQuality.Level.FULL?LifeFlightTest.snapshot(mc,"clean_body_"+effect):java.util.concurrent.CompletableFuture.<Void>completedFuture(null);
     });c.waitFor(mc -> capture.isDone());capture.join();
    }
   }
  }finally{c.runOnClient(mc -> {
   MagicQuality.own=previous;mc.options.setCameraType(camera);
   if(mc.gui.hud.isHidden()!=hidden)mc.gui.hud.toggle();
   mc.getWindow().setWindowed(width,height);mc.resizeGui();
  });}
 }
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
