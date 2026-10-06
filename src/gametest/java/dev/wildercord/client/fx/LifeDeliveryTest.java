package dev.wildercord.client.fx;
import dev.wildercord.cast.*;
import dev.wildercord.content.*;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Paid native Self, world, hostile and movement delivery acceptance, plus renderer accessibility. */
public final class LifeDeliveryTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext c) {
  var previous=c.computeOnClient(mc->MagicQuality.own);
  try(var world=c.worldBuilder().create()) {
   c.waitTicks(40);var server=world.getServer();
   server.runCommand("gamerule spawn_mobs false");server.runCommand("time set 6000");server.runCommand("weather clear");
   server.runCommand("fill -16 100 -12 16 100 40 polished_deepslate");server.runCommand("fill -12 101 24 12 109 24 gray_concrete");
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var b=Spellbooks.get(p).withStarterGiven();for(var r:Runes.all())b=b.learn(r.id());Spellbooks.set(p,b);});
   c.waitTicks(15);c.runOnClient(mc->{mc.getWindow().setWindowed(1280,720);mc.resizeGui();if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();MagicQuality.own=MagicQuality.Level.FULL;
    var option=new LifeOption(LifeOption.LEAF,0xC7D89B,.2F,8,new Vec3(.01,-.01,0),.15F);
    var leaf=new LifeParticle(mc.level,0,80,0,option);check(leaf.getLightCoords(0)!=net.minecraft.util.LightCoordsUtil.FULL_BRIGHT,"Living material samples world light");
    boolean flash=MagicQuality.reducedFlash;try{MagicQuality.reducedFlash=false;leaf.tick();float normal=((Number)LifeFlightTest.field(leaf,net.minecraft.client.particle.SingleQuadParticle.class,"alpha")).floatValue();var reduced=new LifeParticle(mc.level,0,80,0,option);MagicQuality.reducedFlash=true;reduced.tick();check(((Number)LifeFlightTest.field(reduced,net.minecraft.client.particle.SingleQuadParticle.class,"alpha")).floatValue()<normal,"Reduced Flash softens living material");}finally{MagicQuality.reducedFlash=flash;}
   });
   // Establish screenshot baseline using the exact production frame helper.
   var base=c.computeOnClient(mc->LifeFlightTest.snapshot(mc,"life_delivery_background"));c.waitFor(mc->base.isDone());base.join();
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setHealth(10);paid(p,"self","heal");});
   c.waitTicks(4);c.runOnClient(mc->{check(LifeFlightTest.particles(mc.particleEngine).stream().anyMatch(p->p instanceof LifeParticle && LifeFlightTest.at(p).distanceTo(mc.player.position().add(0,.7,0))<1.65),"Healing preparation stays at caster");check(noBolts(mc),"Self Heal creates no projectile");});
   c.runOnClient(mc->mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));shot(c,"life_delivery_self_heal");
   c.waitTicks(8);server.runOnServer(s->check(s.getPlayerList().getPlayers().getFirst().getHealth()>10,"Paid Self Heal actually restores health"));
   c.waitTicks(12);server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();paid(p,"self","bramble");});c.waitTicks(12);c.runOnClient(mc->check(noBolts(mc),"Self Bramble creates no projectile"));shot(c,"life_delivery_self_bramble");
   // NoAI still permits the Runebound load roll; this reflection fixture needs an ordinary 20-health Husk.
   server.runCommand("summon husk 0.5 101 2.5 {NoAI:1b,Silent:1b,Tags:[\"wildercord.rolled\"]}");c.waitTicks(4);
   server.runOnServer(s->{
    var p=s.getPlayerList().getPlayers().getFirst();var attacker=s.overworld().getEntitiesOfClass(Husk.class,p.getBoundingBox().inflate(4)).getFirst();
    check(attacker.getHealth()==20 && attacker.getMaxHealth()==20 && Runebound.spellOf(attacker).isEmpty(),"Bramble attacker is an ordinary full-health Husk");
    p.setInvulnerableTime(0);check(p.hurtServer(s.overworld(),s.overworld().damageSources().mobAttack(attacker),2),"Bramble receives a real incoming attack");
   });
   c.waitTicks(4);server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var attackers=s.overworld().getEntitiesOfClass(Husk.class,p.getBoundingBox().inflate(5));check(!attackers.isEmpty() && attackers.getFirst().getHealth()<20,"Paid Bramble reflects the real incoming attack");attackers.forEach(net.minecraft.world.entity.Entity::discard);});
   // World effect is delivered by a paid Self cast over adjacent farmland.
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var farmland=new BlockPos(1,100,0);s.overworld().setBlock(farmland,Blocks.FARMLAND.defaultBlockState(),3);s.overworld().setBlock(farmland.above(),Blocks.WHEAT.defaultBlockState(),3);paid(p,"self","grow");});
   c.waitTicks(12);server.runOnServer(s->check(s.overworld().getBlockState(new BlockPos(1,101,0)).getValue(CropBlock.AGE)>0,"Paid Grow changes actual crops"));shot(c,"life_delivery_world_grow");
   // Poison an actual hostile target along the paid projectile's route.
   server.runCommand("summon pillager 0.5 101 10.5 {NoAI:1b,Silent:1b}");c.waitTicks(8);
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);p.setDeltaMovement(Vec3.ZERO);});c.waitTicks(6);
   server.runOnServer(s->paid(s.getPlayerList().getPlayers().getFirst(),"bolt","venom"));c.waitTicks(20);
   server.runOnServer(s->{var targets=s.overworld().getEntitiesOfClass(net.minecraft.world.entity.monster.illager.Pillager.class,new net.minecraft.world.phys.AABB(-2,100,7,3,106,14));var p=s.getPlayerList().getPlayers().getFirst();check(!targets.isEmpty() && targets.getFirst().getHealth()<targets.getFirst().getMaxHealth() && targets.getFirst().hasEffect(net.minecraft.world.effect.MobEffects.POISON),"Paid Venom hits hostile entity and applies poison: player="+p.position()+" targets="+targets.stream().map(t->t.position()+" health="+t.getHealth()+" poison="+t.hasEffect(net.minecraft.world.effect.MobEffects.POISON)).toList()+" bolts="+s.overworld().getEntitiesOfClass(RuneBolt.class,p.getBoundingBox().inflate(40)).stream().map(b->b.position().toString()).toList());targets.forEach(net.minecraft.world.entity.Entity::discard);});shot(c,"life_delivery_hostile_venom");
   c.waitTicks(15);server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);paid(p,"bolt","bloomstep");});
   c.waitTicks(40);server.runOnServer(s->check(s.getPlayerList().getPlayers().getFirst().position().z>15,"Paid Bloomstep reaches safe destination from actual Bolt impact"));shot(c,"life_delivery_movement_bloomstep");
  }finally{c.runOnClient(mc->MagicQuality.own=previous);}
 }
 private static void paid(net.minecraft.server.level.ServerPlayer p,String...paths){check(SpellCaster.edit(p,0,Arrays.stream(paths).map(s->"wildercord:"+s).toList())==null,"Accepted delivery");Spellbooks.setReadyAt(p,0,0);Spellbooks.setMana(p,100);float before=Spellbooks.mana(p);SpellCaster.cast(p,0);check(Spellbooks.mana(p)<before,"Survival delivery spends mana");}
 private static boolean noBolts(net.minecraft.client.Minecraft mc){for(var e:mc.level.entitiesForRendering())if(e instanceof RuneBolt)return false;return true;}
 private static void shot(ClientGameTestContext c,String name){c.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of(name).withSize(1280,720).disableCounterPrefix());}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
