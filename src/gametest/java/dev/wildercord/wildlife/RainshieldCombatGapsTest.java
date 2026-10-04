package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;
/** Supplied native combat scenes; actual held item use, vanilla attacks/collisions and real lifecycle. No acquisition/paid spell claim. */
public final class RainshieldCombatGapsTest implements FabricClientGameTest {
 private ServerPlayer watching;private boolean damageObserved,retiredAtDamage;
 private static ServerPlayer player(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
 private static void check(boolean pass,String why){if(!pass)throw new AssertionError(why);}
 private static void setup(MinecraftServer s){var l=s.overworld();for(int x=-7;x<=7;x++)for(int z=-4;z<=4;z++){l.setBlock(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState(),2);for(int y=101;y<=106;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}var p=player(s);p.setGameMode(GameType.SURVIVAL);p.teleportTo(l,.5,101,.5,Set.<Relative>of(),90,0,false);p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(RooksRainshield.ITEM));p.setHealth(20);}
 private static Zombie source(MinecraftServer s,double x){var z=EntityTypes.ZOMBIE.create(s.overworld(),EntitySpawnReason.COMMAND);check(z!=null,"Actual supplied vanilla hostile factory");z.setCustomName(net.minecraft.network.chat.Component.literal("Native Rainshield bypass source"));z.setPersistenceRequired();z.snapTo(x,101,.5,0,0);check(s.overworld().addFreshEntity(z),"Actual unpaused hostile indexes normally");return z;}
 private static UUID armedDraw(){try{var f=dev.wildercord.client.fx.RainshieldClient.class.getDeclaredField("armed");f.setAccessible(true);return (UUID)f.get(null);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void incoming(MinecraftServer s){var a=EntityTypes.ARROW.create(s.overworld(),EntitySpawnReason.COMMAND);check(a!=null,"Actual vanilla arrow factory");a.snapTo(-3.5,102.1,.5,0,0);a.setDeltaMovement(new Vec3(.65,0,0));a.setBaseDamage(2);check(s.overworld().addFreshEntity(a),"Actual vanilla positive arrow indexes");}
 private static void use(ClientGameTestContext c){c.runOnClient(mc->{mc.gui.setScreen(null);mc.options.keyUse.setDown(true);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);});}
 public void runTest(ClientGameTestContext c){net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DAMAGE.register((target,damage,base,taken,blocked)->{if(target==watching&&taken>0){damageObserved=true;retiredAtDamage=!RooksRainshield.active(watching);}});try(var settings=new TidewardNative(c)){
  for(String scenario:List.of("melee","trident","rune_bolt","held_expiry","death","dimension","old_stack_cleanup","release_generation"))try(var w=c.worldBuilder().create()){
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("difficulty normal");w.getServer().runCommand("gamerule natural_health_regeneration false");w.getServer().runCommand("time set midnight");w.getServer().runOnServer(RainshieldCombatGapsTest::setup);c.waitTicks(6);use(c);c.waitTicks(16);
   w.getServer().runOnServer(s->check(RooksRainshield.active(player(s))&&player(s).getTicksUsingItem()>=RooksRainshield.PREP,"Each actual refusal control starts with a prepared fan: "+scenario));w.getServer().runOnServer(s->{watching=player(s);damageObserved=false;retiredAtDamage=false;});
   if(scenario.equals("release_generation")){
    UUID previous=c.computeOnClient(mc -> armedDraw());check(previous!=null,"Actual acknowledged first draw reaches the carrier client");
    c.runOnClient(mc->mc.options.keyUse.setDown(false));c.waitTicks(4);use(c);c.waitTicks(16);
    UUID next=c.computeOnClient(mc -> armedDraw());check(next!=null&&!next.equals(previous),"Same stack's actual second input gesture obtains a fresh server draw nonce");
    c.runOnClient(mc -> net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new RainshieldFx.Release(previous)));c.waitTicks(3);
    w.getServer().runOnServer(s->{check(RooksRainshield.active(player(s)),"Obsolete actual old-draw release packet cannot unlock/clear a newer same-stack fan");incoming(s);});c.waitTicks(12);
    w.getServer().runOnServer(s -> check(player(s).getHealth()==20&&player(s).getMainHandItem().getDamageValue()==4&&player(s).getAttachedOrElse(RooksRainshield.READY,0L)>s.overworld().getGameTime(),"New same-stack draw survives stale packet and pays exactly one real arrow catch"));
   }else if(scenario.equals("old_stack_cleanup")){
    var oldStack=w.getServer().computeOnServer(server->player(server).getMainHandItem());int oldRemaining=w.getServer().computeOnServer(server->player(server).getUseItemRemainingTicks());
    c.runOnClient(mc->mc.options.keyUse.setDown(false));c.waitTicks(4);w.getServer().runOnServer(server->{check(!RooksRainshield.active(player(server)),"Actual original item input release closes its own lease");player(server).setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(RooksRainshield.ITEM));});c.waitTicks(5);use(c);c.waitTicks(16);
    w.getServer().runOnServer(server->{var p=player(server);check(RooksRainshield.active(p)&&p.getUseItem()!=oldStack,"Actual new native stack/use prepares a distinct current fan");
     // Explicit compatibility callback fixture invokes public native APIs, not a claimed stale network packet.
     oldStack.releaseUsing(p.level(),p,oldRemaining);check(RooksRainshield.active(p)&&p.isUsingItem(),"Stale distinct-stack native release callback cannot clear newer real lease");oldStack.finishUsingItem(p.level(),p);check(RooksRainshield.active(p)&&p.isUsingItem(),"Stale distinct-stack native finish callback cannot clear newer real lease");
     var arrow=EntityTypes.ARROW.create(server.overworld(),EntitySpawnReason.COMMAND);check(arrow!=null,"Actual vanilla positive collision control");arrow.snapTo(-3.5,102.1,.5,0,0);arrow.setDeltaMovement(new Vec3(.65,0,0));arrow.setBaseDamage(2);check(server.overworld().addFreshEntity(arrow),"Actual incoming arrow admitted after stale callback controls");});c.waitTicks(12);
    w.getServer().runOnServer(server->{var p=player(server);check(p.getHealth()==20&&p.getMainHandItem().getDamageValue()==4&&oldStack.getDamageValue()==0&&p.getAttachedOrElse(RooksRainshield.READY,0L)>server.overworld().getGameTime(),"New lease retains real collision protection and one paid wear/rest; old stack cannot pay or own it");});
   }else if(scenario.equals("held_expiry")){
    // Keep genuine input held: releasing first would prove cancellation, not deadline expiry/no automatic renewal.
    c.waitTicks(RooksRainshield.WINDOW+8);w.getServer().runOnServer(s->check(!RooksRainshield.active(player(s))&&player(s).getMainHandItem().getDamageValue()==0&&player(s).getAttachedOrElse(RooksRainshield.READY,0L)==0,"Original prepared window expires under held input without renewable cover or invented price"));
    c.runOnClient(mc->mc.options.keyUse.setDown(false));c.waitTicks(4);use(c);c.waitTicks(16);w.getServer().runOnServer(s->{check(RooksRainshield.active(player(s)),"Actual new release/press gesture reopens preparation after unpaid expiry");incoming(s);});c.waitTicks(12);
    w.getServer().runOnServer(s -> check(player(s).getHealth()==20&&player(s).getMainHandItem().getDamageValue()==4&&player(s).getAttachedOrElse(RooksRainshield.READY,0L)>s.overworld().getGameTime(),"Expiry-release positive control retains ordinary one-arrow price/protection"));
   }else if(scenario.equals("death")){
    w.getServer().runOnServer(s->{var p=player(s);check(p.hurtServer(p.level(),p.damageSources().genericKill(),Float.MAX_VALUE)&&!p.isAlive(),"Actual native fatal damage retires living owner");});c.waitTicks(3);w.getServer().runOnServer(s->check(!RooksRainshield.active(player(s))&&player(s).getAttachedOrElse(RooksRainshield.READY,0L)==0,"Actual death clears unpaid fan without rest reward"));
   }else if(scenario.equals("dimension")){
    w.getServer().runOnServer(s->{var n=s.getLevel(Level.NETHER);for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){n.setBlock(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState(),2);for(int y=101;y<=105;y++)n.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}var p=player(s);p.teleportTo(n,.5,101,.5,Set.<Relative>of(),90,0,false);check(p.level()==n,"Actual connected owner changes dimension");});c.waitTicks(5);w.getServer().runOnServer(s->check(!RooksRainshield.active(player(s))&&player(s).getMainHandItem().getDamageValue()==0&&player(s).getAttachedOrElse(RooksRainshield.READY,0L)==0,"Fresh dimension authority clears unpaid old-world fan"));
   }else{
    w.getServer().runOnServer(s->{var p=player(s);if(scenario.equals("melee")){var attacker=source(s,-.5);check(attacker.doHurtTarget(s.overworld(),p)&&p.getHealth()<20,"Actual full-AI vanilla source's native melee attack bypasses prepared relic");}
     else if(scenario.equals("trident")){var trident=new ThrownTrident(s.overworld(),-3.5,102.1,.5,new ItemStack(Items.TRIDENT));trident.setDeltaMovement(new Vec3(.65,0,0));check(s.overworld().addFreshEntity(trident),"Real vanilla trident with physical collision is supplied");}
     else{var attacker=source(s,-5.5);var group=dev.wildercord.spell.SpellCompiler.compile(List.of(dev.wildercord.spell.Runes.BOLT,dev.wildercord.spell.Runes.HARM)).root().groups.getFirst();var bolt=dev.wildercord.cast.RuneBolt.launch(new dev.wildercord.cast.Cast(attacker),group,null,new Vec3(-3.5,102.1,.5),new Vec3(1,0,0),false);check(bolt!=null,"Actual RuneBolt runtime launches supplied hostile payload; no mana-payment claim");}
    });c.waitFor(mc->mc.player.getHealth()<20,100);c.runOnClient(mc->mc.options.keyUse.setDown(false));c.waitTicks(3);w.getServer().runOnServer(s->{var p=player(s);check(damageObserved&&retiredAtDamage,"Actual positive native damage event retires prepared fan immediately, before possible held-input restart");check(p.getHealth()<20,"Actual bypass source delivers real harm: "+scenario);check(p.getMainHandItem().getDamageValue()==0&&p.getAttachedOrElse(RooksRainshield.READY,0L)==0&&!RooksRainshield.active(p),"Non-arrow harm neither catches nor spends/rests; actual released input leaves fan closed");});
   }
   c.runOnClient(mc->mc.options.keyUse.setDown(false));w.getServer().runOnServer(server->watching=null);
  }
 }finally{watching=null;} }
}
