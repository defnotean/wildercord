package dev.wildercord.client.fx;
import dev.wildercord.cast.*;
import dev.wildercord.content.*;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.*;
import dev.wildercord.menu.FusionAltarMenu;
import dev.wildercord.net.FormationPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.particle.*;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native paid six-fusion preparation/travel evidence and explicit material identity contracts. */
public final class FieldFusionPresentationTest implements FabricClientGameTest {
 private static final List<RuneDef> NEW=List.of(Runes.SPRINGBED,Runes.CINDER_SIEVE,Runes.ASHEN_MERCY,Runes.CLOCKROOT,Runes.SKYLATCH,Runes.THRESHERWIND);
 public void runTest(ClientGameTestContext c){
  var previous=c.computeOnClient(mc->MagicQuality.own);
  var hidden=c.computeOnClient(mc->mc.gui.hud.isHidden());
  var window=c.computeOnClient(mc->new int[]{mc.getWindow().getWidth(),mc.getWindow().getHeight()});
  try(var w=c.worldBuilder().create()){
   c.waitTicks(30);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");w.getServer().runCommand("fill -16 100 -8 16 100 60 polished_deepslate");
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var b=Spellbooks.get(p).withStarterGiven();for(var r:Runes.all())b=b.learn(r.id());Spellbooks.set(p,b);});c.waitTicks(15);
   c.runOnClient(mc->{mc.getWindow().setWindowed(1280,720);mc.resizeGui();if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();recipes();});
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var at=new BlockPos(0,101,-3);p.level().setBlockAndUpdate(at,WildercordBlocks.FUSION_ALTAR.defaultBlockState());p.setExperienceLevels(100);p.openMenu(p.level().getBlockState(at).getMenuProvider(p.level(),at));});c.waitTicks(5);
   for(var rune:NEW){w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var menu=(FusionAltarMenu)p.containerMenu;var recipe=Fusions.signatureFor(rune).orElseThrow();menu.getSlot(FusionAltarMenu.RESULT).set(ItemStack.EMPTY);menu.getSlot(0).set(RuneItem.stack(recipe.a()));menu.getSlot(1).set(RuneItem.stack(recipe.b()));menu.getSlot(2).set(ItemStack.EMPTY);menu.getSlot(FusionAltarMenu.CATALYST).set(new ItemStack(Items.AMETHYST_SHARD));menu.broadcastChanges();});c.waitTicks(4);
    c.takeScreenshot(TestScreenshotOptions.of("fieldfusion_altar_"+rune.path()));c.runOnClient(mc->mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId,FusionAltarMenu.BUTTON_FUSE));c.waitTicks(4);
    w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var menu=(FusionAltarMenu)p.containerMenu;check(RuneItem.runeOf(menu.getSlot(FusionAltarMenu.RESULT).getItem()).orElseThrow()==rune,"Native client Fuse packet commits exact signature "+rune.path());check(menu.getSlot(0).getItem().isEmpty()&&menu.getSlot(1).getItem().isEmpty()&&menu.getSlot(FusionAltarMenu.CATALYST).getItem().isEmpty(),"Native Fuse packet consumes ingredients "+rune.path());});
   }
   w.getServer().runOnServer(s->s.getPlayerList().getPlayers().getFirst().closeContainer());c.waitTicks(3);
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(int i=0;i<NEW.size();i++)p.getInventory().setItem(i,RuneItem.stack(NEW.get(i)));p.inventoryMenu.broadcastChanges();});c.waitTicks(5);
   c.runOnClient(mc->mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));c.waitTicks(3);c.takeScreenshot(TestScreenshotOptions.of("fieldfusion_inventory"));c.runOnClient(mc->mc.gui.setScreen(null));
   for(var rune:List.of(Runes.CINDER_SIEVE,Runes.SPRINGBED)){
    c.waitTicks(8);c.runOnClient(mc->{mc.particleEngine.clearParticles();MagicQuality.own=MagicQuality.Level.FULL;});
    w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var f=Fusions.signatureFor(rune).orElseThrow();ServerPlayNetworking.send(p,new FormationPayload(p.getId(),"bolt",List.of(Runes.BOLT.id(),rune.id()),List.of(f.first(),f.second()),0xAA9980,1));});c.waitTicks(2);
    c.runOnClient(mc->{check(particles(mc.particleEngine).stream().anyMatch(p->p.isAlive()&&!(p instanceof SpellCircleParticle)),"Pure authored field packet emits materials "+rune.path());check(particles(mc.particleEngine).stream().noneMatch(p->p.isAlive()&&p instanceof LightParticle&&at(p).distanceTo(mc.player.getEyePosition().add(0,-.65,3.2))<1.7),"Pure field preparation suppresses generic luminous body and constituent fallback "+rune.path());});
   }
   for(var quality:List.of(MagicQuality.Level.FULL,MagicQuality.Level.MINIMAL))for(var rune:NEW){
    w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.level().getEntitiesOfClass(RuneBolt.class,p.getBoundingBox().inflate(96)).forEach(net.minecraft.world.entity.Entity::discard);});c.waitTicks(12);
    c.runOnClient(mc->{mc.particleEngine.clearParticles();MagicQuality.own=quality;});
    w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(SpellCaster.edit(p,0,List.of(Runes.BOLT.id(),rune.id()))==null,"Actual fusion spell edits "+rune.path());Spellbooks.setMana(p,100);Spellbooks.setReadyAt(p,0,0);float before=Spellbooks.mana(p);SpellCaster.cast(p,0);check(Spellbooks.mana(p)<before,"Actual fusion preparation paid "+rune.path());});c.waitTicks(4);
    c.runOnClient(mc->{int front=0,rear=0;for(var p:particles(mc.particleEngine))if(p.isAlive()){
     if(p instanceof SpellCircleParticle){rear++;check(at(p).subtract(mc.player.getEyePosition()).dot(mc.player.getLookAngle())<0,"Rear fusion glyph "+rune.path());}
     else if(at(p).distanceTo(mc.player.getEyePosition().add(0,-.65,3.2))<1.7)front++;
    }check(front>0&&rear>0,"Actual authored preparation and rear glyph "+rune.path()+" / "+quality);});
    if(quality==MagicQuality.Level.FULL)c.takeScreenshot(TestScreenshotOptions.of("fieldfusion_prepare_"+rune.path()));
    c.waitTicks(6);c.runOnClient(mc->{RuneBolt bolt=null;for(var e:mc.level.entitiesForRendering())if(e instanceof RuneBolt b)bolt=b;check(bolt!=null&&bolt.getEntityData().get(RuneBolt.DATA_EFFECTS).equals(rune.id()),"Actual projectile retains fused identity "+rune.path());final var live=bolt;double reach=1+2*live.getDeltaMovement().length();check(particles(mc.particleEngine).stream().anyMatch(p->p.isAlive()&&!(p instanceof SpellCircleParticle)&&at(p).distanceTo(live.position())<reach),"Actual custom fused flight "+rune.path()+" / "+quality);});
    if(quality==MagicQuality.Level.FULL)c.takeScreenshot(TestScreenshotOptions.of("fieldfusion_flight_"+rune.path()));
   }
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.level().getEntitiesOfClass(RuneBolt.class,p.getBoundingBox().inflate(96)).forEach(net.minecraft.world.entity.Entity::discard);});c.waitTicks(8);c.runOnClient(mc->{mc.particleEngine.clearParticles();MagicQuality.own=MagicQuality.Level.FULL;});
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(SpellCaster.edit(p,0,List.of(Runes.BOLT.id(),Runes.CINDER_SIEVE.id(),Runes.UMBRA.id()))==null,"Cross-family mixed spell edits");Spellbooks.setMana(p,100);Spellbooks.setReadyAt(p,0,0);float before=Spellbooks.mana(p);SpellCaster.cast(p,0);check(Spellbooks.mana(p)<before,"Actual cross-family mixed preparation is paid");});c.waitTicks(3);
   c.runOnClient(mc->check(particles(mc.particleEngine).stream().anyMatch(p->p.isAlive()&&p instanceof LightParticle&&at(p).distanceTo(mc.player.getEyePosition().add(0,-.65,3.2))<1.7),"Cross-family Umbra retains actual mixed preparation fallback"));c.takeScreenshot(TestScreenshotOptions.of("fieldfusion_mixed_fallback"));
  }finally{c.runOnClient(mc->{MagicQuality.own=previous;if(mc.gui.hud.isHidden()!=hidden)mc.gui.hud.toggle();mc.getWindow().setWindowed(window[0],window[1]);mc.resizeGui();});}
 }
 private static void recipes(){
  ThresherwindRecipeChecks.verify();
  var unique=new HashSet<String>();
  for(var id:FieldFusionForms.RUNES)for(boolean minimal:new boolean[]{false,true}){
   var stages=new ArrayList<String>();var materials=new HashSet<Integer>();
   for(int beat:new int[]{1,2}){var trace=new ArrayList<String>();check(FieldFusionForms.prepare("wildercord:"+id,beat,1,Vec3.ZERO,new Vec3(1,0,0),new Vec3(0,1,0),new Vec3(0,0,1),minimal,(o,p)->{check(Double.isFinite(p.lengthSqr())&&p.length()<1.8,"Bounded field preparation "+id);check(!(o instanceof LightOption),"Material preparation "+id);if(o instanceof MaterialOption m)materials.add(m.style());trace.add(o+"@"+p);}),"Authored preparation dispatch "+id);check(!trace.isEmpty()&&trace.size()<96,"Finite material recipe "+id);stages.add(String.join(";",trace));}
   check(!stages.get(0).equals(stages.get(1)),"Independent preparation order evolves "+id);if(!minimal)check(unique.add(stages.get(0)),"No duplicated field preparation "+id);
   var required=switch(id){case "springbed"->Set.of(MaterialOption.WATER,MaterialOption.PETAL);case "cinder_sieve"->Set.of(MaterialOption.EMBER,MaterialOption.VOID);case "clockroot"->Set.of(MaterialOption.TIME);case "skylatch"->Set.of(MaterialOption.VOID);case "thresherwind"->Set.of(MaterialOption.PETAL);default->Set.<Integer>of();};check(materials.containsAll(required),"Both field ingredients survive minimal "+id);
   var travel=new ArrayList<String>();for(int age:new int[]{4,8}){var trace=new ArrayList<String>();FieldFusionForms.flight("wildercord:"+id,age,1,1,Vec3.ZERO,new Vec3(0,0,1),minimal,(o,p)->{check(Double.isFinite(p.lengthSqr())&&p.length()<1,"Bounded field travel "+id);trace.add(o+"@"+p);});check(!trace.isEmpty()&&trace.size()<40,"Finite travel emissions "+id);travel.add(String.join(";",trace));}check(!travel.get(0).equals(travel.get(1)),"Moving ingredient interaction "+id);
  }
 }
 private static Object field(Object o,Class<?> owner,String name){try{var f=owner.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static Vec3 at(Particle p){return new Vec3(((Number)field(p,Particle.class,"x")).doubleValue(),((Number)field(p,Particle.class,"y")).doubleValue(),((Number)field(p,Particle.class,"z")).doubleValue());}
 private static List<Particle> particles(ParticleEngine engine){var out=new ArrayList<Particle>();for(var g:((Map<?,?>)field(engine,ParticleEngine.class,"particles")).values())for(var p:(Queue<?>)field(g,ParticleGroup.class,"particles"))out.add((Particle)p);for(var p:(Queue<?>)field(engine,ParticleEngine.class,"particlesToAdd"))out.add((Particle)p);return out;}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
