package dev.wildercord.client.fx;
import dev.wildercord.cast.*;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Actual safe teleport, swap, delayed return/crouch choice and wall permissions. */
public final class VoidMovementDeliveryTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext c){
  var deny=new AtomicBoolean(false);var crouch=c.computeOnClient(mc->mc.options.toggleCrouch().get());
  var shift=c.computeOnClient(mc->mc.options.keyShift.isDown());
  PlayerBlockBreakEvents.BEFORE.register((l,p,pos,state,e)->!deny.get() || pos.getZ()!=2 || pos.getY()<101);
  try(var world=c.worldBuilder().create()){
   c.waitTicks(40);var server=world.getServer();server.runCommand("gamerule spawn_mobs false");server.runCommand("fill -16 100 -12 16 100 30 polished_deepslate");server.runCommand("fill -12 101 18 12 109 18 gray_concrete");
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);reset(p);Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var b=Spellbooks.get(p).withStarterGiven();for(var r:Runes.all())b=b.learn(r.id());Spellbooks.set(p,b);});c.waitTicks(15);
   server.runOnServer(s->paid(s.getPlayerList().getPlayers().getFirst(),"bolt","blink"));c.waitTicks(32);
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.getZ()>12 && p.level().noCollision(p,p.getBoundingBox()),"Paid Blink reaches safe real impact destination");});shot(c,"void_delivery_blink");
   server.runOnServer(s->reset(s.getPlayerList().getPlayers().getFirst()));c.waitTicks(8);server.runCommand("summon husk 0.5 101 10.5 {NoAI:1b,Silent:1b}");c.waitTicks(6);
   server.runOnServer(s->paid(s.getPlayerList().getPlayers().getFirst(),"bolt","warp"));c.waitTicks(20);
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var foes=s.overworld().getEntitiesOfClass(Husk.class,new net.minecraft.world.phys.AABB(-4,100,-4,4,105,15));check(p.getZ()>8 && !foes.isEmpty() && foes.getFirst().getZ()<3,"Paid Warp actually exchanges caster and enemy positions");check(p.level().noCollision(p,p.getBoundingBox()),"Warp arrival is safe");foes.forEach(net.minecraft.world.entity.Entity::discard);});shot(c,"void_delivery_warp");
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();reset(p);paid(p,"bolt","warp_step");});c.waitTicks(32);
   server.runOnServer(s->check(s.getPlayerList().getPlayers().getFirst().getZ()>12,"Paid Warp Step reaches outbound impact"));shot(c,"void_delivery_warp_step_out");c.waitTicks(65);
   server.runOnServer(s->check(s.getPlayerList().getPlayers().getFirst().position().distanceTo(new Vec3(.5,101,.5))<.6,"Warp Step naturally returns after delay"));shot(c,"void_delivery_warp_step_return");
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();reset(p);paid(p,"bolt","warp_step");});c.waitTicks(32);
   c.runOnClient(mc->{mc.options.toggleCrouch().set(false);mc.options.keyShift.setDown(true);});c.waitTicks(65);
   server.runOnServer(s->check(s.getPlayerList().getPlayers().getFirst().getZ()>12,"Actual crouch input keeps Warp Step arrival"));c.runOnClient(mc->mc.options.keyShift.setDown(false));shot(c,"void_delivery_warp_step_stay");
   server.runCommand("fill -4 101 2 4 105 2 stone");server.runOnServer(s->reset(s.getPlayerList().getPlayers().getFirst()));c.waitTicks(8);deny.set(true);
   server.runOnServer(s->paid(s.getPlayerList().getPlayers().getFirst(),"self","zipper"));c.waitTicks(12);
   server.runOnServer(s->check(s.getPlayerList().getPlayers().getFirst().getZ()<1,"Paid Zipper respects actual wall claim veto"));deny.set(false);
   server.runOnServer(s->paid(s.getPlayerList().getPlayers().getFirst(),"self","zipper"));c.waitTicks(12);
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.getZ()>2.5 && p.level().noCollision(p,p.getBoundingBox()),"Paid Zipper passes allowed thin wall safely");});shot(c,"void_delivery_zipper");
  }finally{deny.set(false);c.runOnClient(mc->{mc.options.keyShift.setDown(shift);mc.options.toggleCrouch().set(crouch);});}
 }
 private static void reset(net.minecraft.server.level.ServerPlayer p){p.teleportTo(p.level(),.5,101,.5,Set.<Relative>of(),0,0,false);p.setDeltaMovement(Vec3.ZERO);}
 private static void paid(net.minecraft.server.level.ServerPlayer p,String shape,String rune){check(SpellCaster.edit(p,0,List.of("wildercord:"+shape,"wildercord:"+rune))==null,"Accepted delivery "+rune);Spellbooks.setMana(p,100);Spellbooks.setReadyAt(p,0,0);float before=Spellbooks.mana(p);SpellCaster.cast(p,0);check(Spellbooks.mana(p)<before,"Paid delivery "+rune);}
 private static void shot(ClientGameTestContext c,String name){c.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of(name).withSize(1280,720).disableCounterPrefix());}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
