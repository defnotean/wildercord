package dev.wildercord.aura.world;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Real injury and full native attack; paired complete12/saturated13 pool, no pose/clock fabrication. */
public final class AuraBeastDenseAttackTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext c){for(boolean gale:new boolean[]{false,true})for(int count:new int[]{13,12})exercise(c,gale,count);}
 private void exercise(ClientGameTestContext c,boolean gale,int count){try(var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("difficulty normal");w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule fall_damage false");w.getServer().runCommand("time set 18000");
  var peers=new ArrayList<Zombie>();AuraBeast beast=w.getServer().computeOnServer(s->{var l=s.overworld();for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++){l.setBlock(new BlockPos(x,100,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);for(int y=101;y<=106;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}
   var e=(gale?AuraBeasts.GALECLAW:AuraBeasts.STONEHORN).create(l,EntitySpawnReason.COMMAND);e.snapTo(.5,101,.5,0,0);l.addFreshEntity(e);var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.teleportTo(l,10,101,10,Set.<Relative>of(),0,0,false);return e;
  });c.waitTicks(5);
  w.getServer().runOnServer(s->{var l=s.overworld();
   // Keep the targeted two-block receiver unchanged; supplied non-target rows fit the actual initial recoil-to-landing query.
   for(int n=0;n<count;n++){var e=EntityTypes.ZOMBIE.create(l,EntitySpawnReason.COMMAND);e.setCustomName(net.minecraft.network.chat.Component.literal("Native supplied Aura receiver"));e.setNoAi(true);e.setPersistenceRequired();e.getAttribute(Attributes.ARMOR).setBaseValue(0);e.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0);double dx=n==0?0:((n-1)%4-1.5)*.4;double dz=n==0?2:1.6+((n-1)/4)*.2;e.snapTo(beast.getX()+dx,101,beast.getZ()+dz,0,0);check(l.addFreshEntity(e),"Named native dense receiver enters world");check(e.getHealth()==20&&e.getMaxHealth()==20&&e.getAbsorptionAmount()==0&&e.getAttributeValue(Attributes.ARMOR)==0&&e.getAttributeValue(Attributes.ARMOR_TOUGHNESS)==0,"Exact dense receiver baseline after ordinary admission");peers.add(e);}
   check(beast.hurtServer(l,l.damageSources().mobAttack(peers.getFirst()),1),"Genuine injury admits real dense encounter retaliation");
   if(!gale)for(int x=-3;x<=3;x++)for(int y=101;y<=104;y++)l.setBlock(new BlockPos(x,y,4),Blocks.STONE.defaultBlockState(),2);
  });
  boolean warned=false,attacked=false,recovered=false;for(int n=0;n<100;n++){
   int phase=w.getServer().computeOnServer(s->beast.pose());warned|=phase==BeastRules.WARN;attacked|=phase==(gale?BeastRules.LEAP:BeastRules.CHARGE);if(phase==(gale?BeastRules.LEAP:BeastRules.CHARGE)&&count==13)w.getServer().runOnServer(server->{var raw=new ArrayList<LivingEntity>(13);server.overworld().getEntities(net.minecraft.world.level.entity.EntityTypeTest.<Entity,LivingEntity>forClass(LivingEntity.class),beast.getBoundingBox().inflate(gale?3:2.4),e->e!=beast&&beast.valid(e),raw,13);if(!(raw.size()==13&&raw.stream().allMatch(peers::contains))){
    var level=server.overworld();var bounds=beast.getBoundingBox().inflate(gale?3:2.4);var typed=new ArrayList<LivingEntity>(13);
    level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.<Entity,LivingEntity>forClass(LivingEntity.class),bounds,e->e!=beast,typed,13);
    var production=AuraBeastQueries.complete(level,LivingEntity.class,bounds,beast,e->beast.valid(e)&&!(gale?e instanceof Galeclaw:e instanceof Stonehorn));
    dev.wildercord.Wildercord.LOGGER.info("AURA_DENSE_RECEIPT gale={} supplied={} phase={} sourceBody={} sourceBox={} bounds={} sourceMotion={} sourceCollision={} sourceRegistered={} typedRawCount={} typedComplete={} filteredFixtureCount={} productionEligibleCount={} target={}",gale,count,beast.pose(),beast.position(),beast.getBoundingBox(),bounds,beast.getDeltaMovement(),beast.horizontalCollision,level.getEntity(beast.getUUID())==beast,typed.size(),typed.size()<=BeastRules.VICTIMS,raw.size(),production.size(),beast.getTarget()==null?null:beast.getTarget().getUUID());
    for(var e:typed)dev.wildercord.Wildercord.LOGGER.info("AURA_DENSE_RAW uuid={} type={} supplied={} body={} box={} health={} alive={} registered={} valid={} creative={} spectator={}",e.getUUID(),e.getType(),peers.contains(e),e.position(),e.getBoundingBox(),e.getHealth(),e.isAlive(),level.getEntity(e.getUUID())==e,beast.valid(e),e instanceof net.minecraft.world.entity.player.Player player&&player.isCreative(),e.isSpectator());
    for(int i=0;i<peers.size();i++){var e=peers.get(i);dev.wildercord.Wildercord.LOGGER.info("AURA_DENSE_PEER index={} uuid={} body={} box={} health={} max={} absorption={} registered={} removed={} valid={} intersects={} motion={} pushable={} horizontalCollision={} onGround={}",i,e.getUUID(),e.position(),e.getBoundingBox(),e.getHealth(),e.getMaxHealth(),e.getAbsorptionAmount(),level.getEntity(e.getUUID())==e,e.isRemoved(),beast.valid(e),bounds.intersects(e.getBoundingBox()),e.getDeltaMovement(),e.isPushable(),e.horizontalCollision,e.onGround());}
   }
   check(raw.size()==13&&raw.stream().allMatch(peers::contains),"Actual attack phase remains saturated by precisely supplied peers");});if(attacked&&phase==BeastRules.RECOVER){recovered=true;break;}c.waitTicks(1);
  }
  check(warned&&attacked&&recovered,"Native dense fixture traverses real Warning, original Charge/Leap and finite recovery");
  w.getServer().runOnServer(s->{int damaged=0;for(var e:peers){check(e.getHealth()==20||Math.abs(e.getHealth()-(gale?15:13))<.001,"Each actual supplied peer suffers at most one original native strike");if(e.getHealth()<20)damaged++;}check(count==13?damaged==0:damaged>0,"Saturated thirteen refuses all recipients; complete twelve admits genuine physical damage");});
 }}
 private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
}
