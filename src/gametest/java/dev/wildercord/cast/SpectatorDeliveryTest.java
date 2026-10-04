package dev.wildercord.cast;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import dev.wildercord.spell.Runes;
import java.util.*;
/** Actual near spectator, fresh hostile victim, paid public casts. No direct spell impact or collision calls. */
public final class SpectatorDeliveryTest implements FabricClientGameTest {
 private static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 @Override public void runTest(ClientGameTestContext c){ServerPlayer[] actor={null};try(var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");w.getServer().runCommand("fill -4 100 -4 4 100 12 polished_deepslate");
  w.getServer().runOnServer(s -> {var spectator=s.getPlayerList().getPlayers().getFirst();spectator.setGameMode(GameType.SPECTATOR);actor[0]=NextSignatureNative.guest(spectator,"CollisionCaster",false);actor[0].snapTo(.5,101,.5,0,0);NextSignatureNative.ground(actor[0]);});
  for(var shape:List.of(Runes.BOLT,Runes.SPARK,Runes.RAY,Runes.TOUCH)){
   w.getServer().runCommand("summon pillager 0.5 101 3.3 {NoAI:1b,Silent:1b}");c.waitTicks(3);
   UUID[] victim={null};float[] spectatorHealth={0};
   w.getServer().runOnServer(s -> {var l=s.overworld();var observer=s.getPlayerList().getPlayers().getFirst();var p=actor[0];p.snapTo(.5,101,.5,0,0);NextSignatureNative.ground(p);observer.teleportTo(l,.5,101,1.5,Set.<Relative>of(),180,0,false);check(observer.isSpectator(),"Actual spectator keeps its mode while occupying the near collision line");spectatorHealth[0]=observer.getHealth();var targets=l.getEntitiesOfClass(net.minecraft.world.entity.monster.illager.Pillager.class,new net.minecraft.world.phys.AABB(-1,100,2,2,105,5));check(targets.size()==1,"Fresh exact hostile fixture");var t=targets.getFirst();victim[0]=t.getUUID();check(observer.getBoundingBox().clip(p.getEyePosition(),p.getEyePosition().add(0,0,3)).isPresent(),"Actual observer bounds intersect the paid cast segment before its victim");NextSignatureNative.cast(p,shape,Runes.VENOM);});
   c.waitTicks(20);
   w.getServer().runOnServer(s -> {var l=s.overworld();var t=l.getEntity(victim[0]);check(t instanceof net.minecraft.world.entity.LivingEntity living && living.getHealth()<living.getMaxHealth() && living.hasEffect(MobEffects.POISON),"Paid "+shape.path()+" passes the actual spectator and damages/marks its exact hostile victim");var observer=s.getPlayerList().getPlayers().getFirst();check(observer.isSpectator() && observer.getHealth()==spectatorHealth[0] && !observer.hasEffect(MobEffects.POISON),"Spectator remains unharmed and unmarked");t.discard();});
  }
 }finally{actor[0]=null;}}
}
