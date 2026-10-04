package dev.wildercord.aura.world;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Original native injury -> Warning -> strike; actual nested nonlethal damage supersedes its followup receipt. */
public final class AuraBeastProvenanceTest implements FabricClientGameTest {
 private static boolean registered,foreign,observed;private static AuraBeast source;private static Probe receiver;private static DamageSource original;
 public void runTest(ClientGameTestContext c){register();try{for(boolean gale:new boolean[]{false,true})for(boolean nested:new boolean[]{false,true})exercise(c,gale,nested);}finally{clear();}}
 private static void register(){if(registered)return;registered=true;ServerLivingEntityEvents.AFTER_DAMAGE.register((target,damage,base,taken,blocked)->{
  if(observed||source==null||receiver==null||target!=receiver||damage.getEntity()!=source||damage.getDirectEntity()!=source||!damage.is(DamageTypes.MOB_ATTACK)||blocked||taken<=0)return;
  observed=true;original=damage;check(source.pose()==(source.gale()?BeastRules.LEAP:BeastRules.CHARGE),"Original actual committed physical strike owns callback");
  check(receiver.getHealth()==(source.gale()?15:13),"Exact genuine five/seven physical health loss precedes foreign mutation");
  if(foreign){var world=(ServerLevel)source.level();var other=world.damageSources().genericKill();
   float beforeHealth=receiver.getHealth(),beforeAbsorption=receiver.getAbsorptionAmount();int beforeInvulnerability=receiver.getInvulnerableTime();var beforeReceipt=receiver.getLastDamageSource();
   // Vanilla cooldown admits only the excess over the preceding physical input; this applies one real health damage.
   boolean admitted=receiver.hurtServer(world,other,source.gale()?6:8);var afterReceipt=receiver.getLastDamageSource();
   dev.wildercord.Wildercord.LOGGER.info("AURA_PROVENANCE_NESTED gale={} admitted={} healthBefore={} healthAfter={} maximum={} absorptionBefore={} absorptionAfter={} invulnerabilityBefore={} invulnerabilityAfter={} alive={} beforeOriginal={} afterOriginal={} afterForeign={} beforeType={} afterType={} foreignType={}",source.gale(),admitted,beforeHealth,receiver.getHealth(),receiver.getMaxHealth(),beforeAbsorption,receiver.getAbsorptionAmount(),beforeInvulnerability,receiver.getInvulnerableTime(),receiver.isAlive(),beforeReceipt==original,afterReceipt==original,afterReceipt==other,beforeReceipt==null?null:beforeReceipt.getMsgId(),afterReceipt==null?null:afterReceipt.getMsgId(),other.getMsgId());
   check(admitted&&receiver.isAlive()&&receiver.getHealth()==(source.gale()?14:12)&&afterReceipt==other,"Actual nested one-health nonlethal foreign damage supersedes final receipt");}
  receiver.after=true;
 });}
 private void exercise(ClientGameTestContext c,boolean gale,boolean nested){try(var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("difficulty normal");w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule fall_damage false");w.getServer().runCommand("time set 18000");
  w.getServer().runOnServer(s->{var l=s.overworld();for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++){l.setBlock(new BlockPos(x,100,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);for(int y=101;y<=106;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}
   var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.teleportTo(l,10,101,10,Set.<Relative>of(),0,0,false);
   source=(gale?AuraBeasts.GALECLAW:AuraBeasts.STONEHORN).create(l,EntitySpawnReason.COMMAND);check(source!=null,"Actual registered original source");source.snapTo(.5,101,.5,0,0);check(l.addFreshEntity(source),"Source enters native world with full AI");
  });c.waitTicks(5);
  w.getServer().runOnServer(s->{var l=s.overworld();receiver=new Probe(l);receiver.setCustomName(Component.literal("Native provenance receiver"));receiver.setNoAi(true);receiver.setPersistenceRequired();receiver.getAttribute(Attributes.ARMOR).setBaseValue(0);receiver.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0);receiver.snapTo(source.getX(),101,source.getZ()+2,0,0);check(l.addFreshEntity(receiver),"Named actual probe enters ordinary Zombie index");check(receiver.getHealth()==20&&receiver.getMaxHealth()==20&&receiver.getAbsorptionAmount()==0&&receiver.getAttributeValue(Attributes.ARMOR)==0,"Exact unmodified twenty-health receiver receipt after admission");foreign=nested;observed=false;original=null;check(source.hurtServer(l,l.damageSources().mobAttack(receiver),1),"Genuine physical injury admits original retaliation");});
  boolean warning=false;for(int n=0;n<100;n++){int[] status=w.getServer().computeOnServer(s->new int[]{source.pose(),observed?1:0});warning|=status[0]==BeastRules.WARN;if(status[1]==1)break;c.waitTicks(1);}check(warning,"Real timed Warning precedes physical strike");c.waitTicks(2);
  w.getServer().runOnServer(s->{check(observed&&receiver.isAlive(),"Actual nonlethal committed attack occurred");check(receiver.getHealth()==(gale?15:13)-(nested?1:0),"Original physical damage remains intact in both controls");check(nested?receiver.postOwnedKnocks==0:receiver.postOwnedKnocks>0,"Final foreign receipt refuses original knock/scar branch; unchanged receipt preserves real authored knockback");check(nested?receiver.getLastDamageSource()!=original:receiver.getLastDamageSource()==original,"Actual final source identity agrees with followup admission");});
 }finally{clear();}}
 private static final class Probe extends Zombie {boolean after;int postOwnedKnocks;Probe(ServerLevel l){super(l);}
  @Override public void knockback(double amount,double x,double z,DamageSource receipt,float resistance){if(after&&receipt==original)postOwnedKnocks++;super.knockback(amount,x,z,receipt,resistance);}
 }
 private static void clear(){source=null;receiver=null;original=null;foreign=false;observed=false;}
 private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
}
