package dev.wildercord.client.fx;
import dev.wildercord.cast.*;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.util.*;

/** Actual paid ward/innate outcomes and caster-bound production material. */
public final class VoidWardDeliveryTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext c){
  var old=c.computeOnClient(mc->MagicQuality.own);
  var camera=c.computeOnClient(mc->mc.options.getCameraType());
  try(var world=c.worldBuilder().create()){
   c.waitTicks(40);var server=world.getServer();server.runCommand("gamerule spawn_mobs false");server.runCommand("fill -16 100 -12 16 100 30 polished_deepslate");
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var book=Spellbooks.get(p).withStarterGiven();for(var r:Runes.all())book=book.learn(r.id());Spellbooks.set(p,book);});c.waitTicks(15);
   c.runOnClient(mc->{MagicQuality.own=MagicQuality.Level.FULL;mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);});
   for(String rune:List.of("veil","anchor","infinity","shulkershell","phantom")){
    c.waitTicks(12);c.runOnClient(mc->mc.particleEngine.clearParticles());
    server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(rune.equals("phantom"))p.setAttached(dev.wildercord.player.WildercordAttachments.INNATE,"wildercord:phantom");paid(p,rune);});c.waitTicks(4);
    c.runOnClient(mc->{check(VoidFlightTest.particles(mc.particleEngine).stream().anyMatch(p->p instanceof VoidParticle && VoidFlightTest.at(p).distanceTo(mc.player.position().add(0,.7,0))<1.65),"Actual Self material stays at caster "+rune);for(var e:mc.level.entitiesForRendering())check(!(e instanceof RuneBolt),"Self ward never becomes a projectile "+rune);});
    c.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("void_self_"+rune).withSize(1280,720).disableCounterPrefix());c.waitTicks(8);
    server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();switch(rune){
     case "veil"->check(p.hasEffect(MobEffects.INVISIBILITY),"Actual invisibility ward");
     case "anchor"->check(VoidTime.anchored(p),"Actual Anchor state");
     case "shulkershell"->check(p.hasEffect(MobEffects.RESISTANCE) && p.getEffect(MobEffects.RESISTANCE).getAmplifier()==3 && p.hasEffect(MobEffects.SLOWNESS),"Actual shell mitigation and immobility");
     case "phantom"->check(s.overworld().getEntities((net.minecraft.world.entity.Entity)null,p.getBoundingBox().inflate(4),e->e.entityTags().contains("wildercord.afterimage")).size()==1,"Owned Phantom creates exactly one real afterimage");
     default->{}
    }});
    if(rune.equals("infinity")){
     server.runCommand("summon husk 0.5 101 3.5 {NoAI:1b,Silent:1b}");c.waitTicks(8);
     server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var enemies=s.overworld().getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Husk.class,p.getBoundingBox().inflate(5));check(!enemies.isEmpty() && enemies.getFirst().hasEffect(MobEffects.SLOWNESS),"Actual Infinity slows nearby enemy");enemies.forEach(net.minecraft.world.entity.Entity::discard);});
    }
   }
  }finally{c.runOnClient(mc->{MagicQuality.own=old;mc.options.setCameraType(camera);});}
 }
 private static void paid(net.minecraft.server.level.ServerPlayer p,String rune){check(SpellCaster.edit(p,0,List.of(Runes.SELF.id(),"wildercord:"+rune))==null,"Accepted paid Self "+rune);Spellbooks.setReadyAt(p,0,0);Spellbooks.setMana(p,100);float before=Spellbooks.mana(p);SpellCaster.cast(p,0);check(Spellbooks.mana(p)<before,"Paid Self "+rune);}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
