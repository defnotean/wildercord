package dev.wildercord.client.fx;

import dev.wildercord.cast.*;
import dev.wildercord.net.FormationPayload;
import dev.wildercord.spell.*;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.content.WildercordItems;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Decoded production group packets, actual delivery aim resolver and paid mixed-group Cord casting. */
public final class GroupFormationTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext c) {
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(40);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule advance_time false");w.getServer().runCommand("time set 6000");
   w.getServer().runCommand("fill -32 100 -16 32 100 32 polished_deepslate");
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,10,false);});c.waitTicks(15);
   c.runOnClient(mc->{mc.getWindow().setWindowed(1280,720);mc.resizeGui();mc.options.setCameraType(CameraType.FIRST_PERSON);});
   for(var shape:List.of(Runes.BURST,Runes.NOVA,Runes.RING,Runes.ZONE,Runes.RAIN,Runes.WALL,Runes.PILLAR,Runes.MINE,Runes.TOTEM,Runes.VORTEX)) {
    clear(c);var expected=new AtomicReference<Vec3>();var runes=List.of(shape,Runes.FIRE);
    w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var compiled=SpellCompiler.compile(runes);var cast=new Cast(p,1,dev.wildercord.player.Heart.Bonuses.NONE,false,null,new Cast.Info(compiled.root(),runes.size(),"",runes));expected.set(aim(cast));FormationVfx.send(cast,runes);});c.waitTicks(2);
    c.runOnClient(mc->{var packets=events();check(packets.size()==1,"One group packet for "+shape.path());var event=packets.getFirst();var canvas=new SpellFormations.Canvas(mc.level,mc.player,event,MagicQuality.Level.FULL);
     if(event.placement()==FormationPayload.CASTER)check(canvas.focus.distanceTo(mc.player.position().add(0,.7,0))<.001,"Caster-centered "+shape.path());
     else {check(event.placement()==FormationPayload.AIMED,"Aimed "+shape.path());check(canvas.focus.distanceTo(expected.get().add(0,shape==Runes.RAIN?12:.12,0))<.01,"Client formation matches actual server delivery aim for "+shape.path());}
     check(event.circle() && event.glyphs().contains(shape.id()),"Rear glyph keeps spell identity");
    });
    c.waitTicks(2);
    if(shape==Runes.ZONE || shape==Runes.RAIN)c.takeScreenshot(TestScreenshotOptions.of("group_formation_"+shape.path()).withSize(1280,720).disableCounterPrefix());
    if(shape==Runes.BURST){c.runOnClient(mc->mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));c.takeScreenshot(TestScreenshotOptions.of("group_formation_burst").withSize(1280,720).disableCounterPrefix());c.runOnClient(mc->mc.options.setCameraType(CameraType.FIRST_PERSON));}
   }
   clear(c);
   var delayed=List.of(Runes.BOLT,Runes.EMBER,Runes.DELAY,Runes.SELF,Runes.FIREWARD);
   send(w,delayed);c.waitTicks(2);c.runOnClient(mc->{var packets=events();check(packets.size()==1,"Initial segment excludes delayed group");check(packets.getFirst().runes().contains(Runes.EMBER.id()) && !packets.getFirst().runes().contains(Runes.FIREWARD.id()),"Delayed effect does not form with first group");check(packets.getFirst().glyphs().contains(Runes.FIREWARD.id()),"Complete spell remains in rear glyph");});
   clear(c);send(w,List.of(Runes.DELAY,Runes.BOLT,Runes.FIRE));c.waitTicks(2);c.runOnClient(mc->{var packets=events();check(packets.size()==1 && packets.getFirst().placement()==FormationPayload.CIRCLE_ONLY && packets.getFirst().runes().isEmpty(),"Delay-only root prepares rear glyph without future assembly");});
   linked(c,w);
   clear(c);
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var book=Spellbooks.get(p).withStarterGiven();for(var r:Runes.all())book=book.learn(r.id());Spellbooks.set(p,book);
    SpellCaster.edit(p,0,List.of(Runes.BOLT.id(),Runes.EMBER.id(),Runes.SELF.id(),Runes.FIREWARD.id()));Spellbooks.setReadyAt(p,0,0);Spellbooks.setMana(p,100);float before=Spellbooks.mana(p);SpellCaster.cast(p,0);check(Spellbooks.mana(p)<before,"Paid mixed-group cast");
   });c.waitTicks(2);c.runOnClient(mc->{var packets=events();check(packets.size()==2,"Actual paid cast emits both initial groups");check(packets.stream().filter(FormationPayload::circle).count()==1,"Exactly one rear glyph for the cast");
    var bolt=packets.stream().filter(p->p.shape().equals("bolt")).findFirst().orElseThrow();var self=packets.stream().filter(p->p.shape().equals("self")).findFirst().orElseThrow();check(bolt.runes().contains(Runes.EMBER.id())&&!bolt.runes().contains(Runes.FIREWARD.id()),"Bolt effect isolation");check(self.runes().contains(Runes.FIREWARD.id())&&!self.runes().contains(Runes.EMBER.id()),"Self effect isolation");check(self.placement()==FormationPayload.CASTER,"Self follows its own delivery");
   });c.waitTicks(2);c.runOnClient(mc->{mc.gui.hud.getChat().clearMessages(false);mc.gui.toastManager().clear();});c.takeScreenshot(TestScreenshotOptions.of("group_formation_paid_bolt_self").withSize(1280,720).disableCounterPrefix());
  }
 }
 private static void linked(ClientGameTestContext c,TestSingleplayerContext w) {
  // A real paid delayed Cord: the branch must stay absent until its scheduled segment executes.
  clear(c);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var book=Spellbooks.get(p).withStarterGiven();for(var r:Runes.all())book=book.learn(r.id());Spellbooks.set(p,book);
   SpellCaster.edit(p,0,List.of(Runes.DELAY.id(),Runes.SELF.id(),Runes.FIREWARD.id()));Spellbooks.setReadyAt(p,0,0);Spellbooks.setMana(p,100);float before=Spellbooks.mana(p);SpellCaster.cast(p,0);check(Spellbooks.mana(p)<before,"Delayed Cord pays once at initial cast");
  });c.waitTicks(3);c.runOnClient(mc->check(events().stream().noneMatch(p->p.runes().contains(Runes.FIREWARD.id())),"No premature delayed assembly"));
  c.waitFor(mc->events().stream().anyMatch(p->p.runes().contains(Runes.FIREWARD.id())));
  c.runOnClient(mc->{var e=events().stream().filter(p->p.runes().contains(Runes.FIREWARD.id())).findFirst().orElseThrow();check(!e.circle() && e.glyphs().isEmpty(),"Delayed stage does not recreate rear glyph");check(e.placement()==FormationPayload.CASTER,"Delayed self remains attached");});
  c.waitTicks(3);c.runOnClient(mc->{mc.gui.hud.getChat().clearMessages(false);mc.gui.toastManager().clear();mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);});c.takeScreenshot(TestScreenshotOptions.of("linked_delayed_fireward").withSize(1280,720).disableCounterPrefix());c.runOnClient(mc->mc.options.setCameraType(CameraType.FIRST_PERSON));
  clear(c);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var foe=net.minecraft.world.entity.EntityTypes.HUSK.create(s.overworld(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);foe.setNoAi(true);foe.snapTo(.5,101,8.5,180,0);s.overworld().addFreshEntity(foe);
   SpellCaster.edit(p,0,List.of(Runes.BOLT.id(),Runes.EMBER.id(),Runes.ON_HIT.id(),Runes.BURST.id(),Runes.FIRE.id()));Spellbooks.setReadyAt(p,0,0);Spellbooks.setMana(p,100);SpellCaster.cast(p,0);
  });c.waitFor(mc->events().stream().anyMatch(p->p.shape().equals("burst")));
  c.runOnClient(mc->{var event=events().stream().filter(p->p.shape().equals("burst")).findFirst().orElseThrow();check(event.placement()==FormationPayload.FIXED && !event.circle(),"Actual projectile impact starts linked Burst");check(event.anchor().distanceTo(new Vec3(.5,102,8.5))<2,"Actual impact assembly stays at foe");});c.waitTicks(3);c.runOnClient(mc->{mc.gui.hud.getChat().clearMessages(false);mc.gui.toastManager().clear();});c.takeScreenshot(TestScreenshotOptions.of("linked_paid_impact_burst").withSize(1280,720).disableCounterPrefix());w.getServer().runCommand("kill @e[type=minecraft:husk]");
  // Impact fixtures exercise the exact admitted segment path; delivery origin must ignore later caster turning.
  for(var shape:List.of(Runes.BURST,Runes.RING,Runes.DOMAIN,Runes.ZONE,Runes.RAIN,Runes.BOLT,Runes.SELF,Runes.ORBIT,Runes.TRAIL)) {
   clear(c);var expected=new AtomicReference<Vec3>();w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var runes=List.of(Runes.BOLT,Runes.EMBER,Runes.ON_HIT,shape,Runes.FIREWARD);var root=SpellCompiler.compile(runes).root();var cast=new Cast(p,1,dev.wildercord.player.Heart.Bonuses.NONE,false,null,new Cast.Info(root,runes.size(),"",runes));
    var trigger=new Cast.Trigger(new Vec3(.5,104,8.5),new Vec3(1,0,0),null,null,null);boolean attached=shape==Runes.SELF || shape==Runes.ORBIT || shape==Runes.TRAIL;
    expected.set(attached?p.position().add(0,.7,0):(shape==Runes.DOMAIN || shape==Runes.ZONE || shape==Runes.RAIN?aim(cast,trigger).add(0,shape==Runes.RAIN?12:.12,0):trigger.pos()));
    segment(cast,root.link.next,trigger);
   });c.waitTicks(2);c.runOnClient(mc->{var packets=events();check(packets.size()==1,"One admitted linked group "+shape.path());var e=packets.getFirst();check(!e.circle(),"No extra glyph "+shape.path());var canvas=new SpellFormations.Canvas(mc.level,mc.player,e,MagicQuality.Level.FULL);check(canvas.focus.distanceTo(expected.get())<.01,"Actual linked origin "+shape.path());if(e.placement()==FormationPayload.FIXED)check(canvas.forward.distanceTo(new Vec3(1,0,0))<.001,"Trigger direction retained");});
   if(shape==Runes.ZONE){c.waitTicks(2);c.takeScreenshot(TestScreenshotOptions.of("linked_impact_zone").withSize(1280,720).disableCounterPrefix());}
  }
  for(boolean allowed:List.of(false,true)) {
   clear(c);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setShiftKeyDown(allowed);var runes=List.of(Runes.IF_SNEAKING,Runes.SELF,Runes.FIREWARD);var root=SpellCompiler.compile(runes).root();CastEngine.cast(new Cast(p,1,dev.wildercord.player.Heart.Bonuses.NONE,false,null,new Cast.Info(root,runes.size(),"",runes)),root);p.setShiftKeyDown(false);});c.waitTicks(2);c.runOnClient(mc->check(events().size()==(allowed?1:0),"Conditional preparation follows actual gate "+allowed));
  }
 }
 private static void segment(Cast cast,SpellPlan.Segment segment,Cast.Trigger trigger){try{var method=CastEngine.class.getDeclaredMethod("runSegment",Cast.class,SpellPlan.Segment.class,Cast.Trigger.class);method.setAccessible(true);method.invoke(null,cast,segment,trigger);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static Vec3 aim(Cast cast,Cast.Trigger trigger){try{var method=CastEngine.class.getDeclaredMethod("aimPoint",Cast.class,Cast.Trigger.class);method.setAccessible(true);return (Vec3)method.invoke(null,cast,trigger);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void send(TestSingleplayerContext w,List<RuneDef> runes){w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var root=SpellCompiler.compile(runes).root();FormationVfx.send(new Cast(p,1,dev.wildercord.player.Heart.Bonuses.NONE,false,null,new Cast.Info(root,runes.size(),"",runes)),runes);});}
 private static Vec3 aim(Cast cast){try{var method=CastEngine.class.getDeclaredMethod("aimPoint",Cast.class,Cast.Trigger.class);method.setAccessible(true);return (Vec3)method.invoke(null,cast,Cast.Trigger.self(cast.caster));}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static List<?> active(){try{var field=SpellFormations.class.getDeclaredField("ACTIVE");field.setAccessible(true);return (List<?>)field.get(null);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static List<FormationPayload> events(){var out=new ArrayList<FormationPayload>();for(Object value:active())try{var field=value.getClass().getDeclaredField("event");field.setAccessible(true);out.add((FormationPayload)field.get(value));}catch(ReflectiveOperationException e){throw new AssertionError(e);}return out;}
 private static void clear(ClientGameTestContext c){c.waitTicks(12);c.runOnClient(mc->{active().clear();mc.particleEngine.clearParticles();mc.gui.hud.getChat().clearMessages(false);mc.gui.toastManager().clear();});}
 private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
}
