package dev.wildercord.wildlife;

import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.entity.EntityTypeTest;
import java.util.*;

/** Real native Warning/Fan and real AFTER_DAMAGE callbacks; no pose, epoch or deadline writes. */
public final class CinderBailiffFanSafetyTest implements FabricClientGameTest {
 private static boolean registered;
 private static CinderBailiff source;
 private static int armed;
 private static boolean observed;
 private static UUID callbackVictim;
 private static float callbackHealth;
 private static Entity moved;
 private static final Set<UUID> expectedVictims=new HashSet<>();
 private final List<Zombie> victims=new ArrayList<>();

 @Override public void runTest(ClientGameTestContext c){
  register();
  try{for(int mode=1;mode<=4;mode++)callbacks(c,mode);dense(c);}
  finally{armed=0;expectedVictims.clear();source=null;callbackVictim=null;moved=null;victims.clear();}
 }

 private static void register(){
  if(registered)return;registered=true;
  ServerLivingEntityEvents.AFTER_DAMAGE.register((target,damage,base,taken,blocked)->{
   if(armed==0||source==null||damage.getEntity()!=source||damage.getDirectEntity()!=source
      ||!damage.is(DamageTypes.MOB_ATTACK)||source.pose()!=CinderBailiff.FANNING||blocked||taken<=0)return;
   if(!expectedVictims.contains(target.getUUID())){
    dev.wildercord.Wildercord.LOGGER.info("EMBER_FAN_UNEXPECTED_RECEIVER actualUUID={} type={} health={} expected={}",target.getUUID(),target.getType(),target.getHealth(),expectedVictims);
    check(false,"Actual physical fan callback must belong to the verified supplied receiver pool");
   }
   int mode=armed;armed=0;observed=true;callbackVictim=target.getUUID();callbackHealth=target.getHealth();
   dev.wildercord.Wildercord.LOGGER.info("EMBER_FAN_CALLBACK mode={} target={} health={} base={} taken={} blocked={} armor={} toughness={} world={} fire={} source={}",mode,callbackVictim,callbackHealth,base,taken,blocked,target.getAttributeValue(Attributes.ARMOR),target.getAttributeValue(Attributes.ARMOR_TOUGHNESS),target.level().dimension(),target.isOnFire(),source.getUUID());check(Math.abs(callbackHealth-16)<.001,"Callback follows a real four-health unarmored fan strike");
   if(mode==1)source.discard();
   else if(mode==2)target.discard();
   else{
    var old=(ServerLevel)target.level();var destination=old.getServer().getLevel(Level.NETHER);
    check(destination!=null,"Actual distinct server dimension exists");
    var actor=mode==3?source:target;
    var uuid=actor.getUUID();
    // Exact mapped teleport implementation returns its new clone; UUID lookup indexes accessible sections.
    moved=actor.teleport(new net.minecraft.world.level.portal.TeleportTransition(destination,new net.minecraft.world.phys.Vec3(.5,30,.5),net.minecraft.world.phys.Vec3.ZERO,0,0,Set.<Relative>of(),net.minecraft.world.level.portal.TeleportTransition.PLACE_PORTAL_TICKET));
    dev.wildercord.Wildercord.LOGGER.info("EMBER_FAN_TRANSFER mode={} originalUUID={} originalWorld={} originalRemoved={} originalAlive={} returnedUUID={} returnedWorld={} returnedRemoved={} returnedAlive={} immediateLookup={} destinationTicking={}",mode,uuid,actor.level().dimension(),actor.isRemoved(),actor.isAlive(),moved==null?"none":moved.getUUID(),moved==null?"none":moved.level().dimension(),moved!=null&&moved.isRemoved(),moved!=null&&moved.isAlive(),destination.getEntity(uuid),destination.isPositionEntityTicking(new BlockPos(0,30,0)));
    check(moved!=null&&moved!=actor&&moved.getUUID().equals(uuid)&&moved.level()==destination&&moved.isAlive()&&!moved.isRemoved(),"Actual teleport returns the live same-UUID creature in its distinct destination dimension");
    check(actor.isRemoved()||actor.level()!=old,"Actual original creature departs the old world synchronously");
   }
  });
 }

 private void callbacks(ClientGameTestContext c,int mode){
  try(var w=c.worldBuilder().create()){
   c.waitTicks(25);configure(w.getServer());
   w.getServer().runOnServer(s->{arena(s.overworld());arena(s.getLevel(Level.NETHER));spawn(s);});
   c.waitTicks(5);
   w.getServer().runOnServer(s->{
    var l=s.overworld();var aggressor=zombie(l,source.getX(),source.getZ()+2);victims.add(aggressor);
    check(source.hurtServer(l,aggressor.damageSources().mobAttack(aggressor),1),"Genuine attack primes actual native retaliation");
    victims.add(zombie(l,source.getX()-1,source.getZ()+1.3));
    victims.add(zombie(l,source.getX()-1.4,source.getZ()+2.1));
    expectedVictims.clear();for(var target:victims)expectedVictims.add(target.getUUID());
    var raw=new ArrayList<LivingEntity>(13);l.getEntities(EntityTypeTest.<Entity,LivingEntity>forClass(LivingEntity.class),source.getBoundingBox().inflate(4),e->e!=source,raw,13);
    var actual=new HashSet<UUID>();for(var target:raw)actual.add(target.getUUID());
    dev.wildercord.Wildercord.LOGGER.info("EMBER_FAN_RECEIVER_SETUP mode={} rawCount={} actual={} expected={}",mode,raw.size(),actual,expectedVictims);
    check(raw.size()==3&&actual.equals(expectedVictims),"Actual pre-fan raw living pool consists of exactly the three known receiver UUIDs");
    for(var target:raw)check(target.isAlive()&&!target.isRemoved()&&target.level()==l&&Math.abs(target.getHealth()-20)<.001&&target.getAttributeValue(Attributes.ARMOR)==0&&target.getAttributeValue(Attributes.ARMOR_TOUGHNESS)==0&&!target.isOnFire(),"Each actual known receiver starts alive with twenty health, zero armor/toughness and no fire");
    observed=false;callbackVictim=null;callbackHealth=0;moved=null;armed=mode;
   });
   boolean warning=false;for(int n=0;n<20;n++){c.waitTicks(2);if(w.getServer().computeOnServer(s->source.pose()==CinderBailiff.WARNING)){warning=true;break;}}
   check(warning,"Actual close retaliation raises a real fixed Warning before fault injection");
   boolean fired=false;for(int n=0;n<35;n++){c.waitTicks(2);if(w.getServer().computeOnServer(s->observed)){fired=true;break;}}
   check(fired,"Real fan invokes the armed native damage callback");
   c.waitTicks(45);
   w.getServer().runOnServer(s->{
    check(callbackVictim!=null&&Math.abs(callbackHealth-16)<.001,"Fault injection follows admitted health damage rather than a fabricated pose");
    int laterHits=0;
    for(var target:victims){
     if(target.getUUID().equals(callbackVictim))continue;
     if(mode==1||mode==3)check(target.getHealth()==20,"Source removal/transfer aborts remaining batch and all later vent harm");
     else{check(target.getHealth()==20||Math.abs(target.getHealth()-16)<.001,"Victim removal/transfer cannot renew any other victim's once-only strike");if(target.getHealth()<20)laterHits++;}
    }
    if(mode==1)check(source.isRemoved(),"Actual source removal remains authoritative");
    if(mode==3)check(source.isRemoved()||source.level()!=s.overworld(),"Original source cannot continue old-world fan after real dimension transfer");
    if(mode==3||mode==4){
     var destination=s.getLevel(Level.NETHER);var registered=destination.getEntity(moved.getUUID());
     dev.wildercord.Wildercord.LOGGER.info("EMBER_FAN_TRANSFER_REGISTERED mode={} uuid={} actualLookup={} exactClone={} alive={} removed={} world={} entityTicking={}",mode,moved.getUUID(),registered,registered==moved,moved.isAlive(),moved.isRemoved(),moved.level().dimension(),destination.isPositionEntityTicking(moved.blockPosition()));
     check(registered==moved&&registered.isAlive()&&!registered.isRemoved()&&registered.level()==destination,"Actual portal ticket admits the exact live transferred creature into destination UUID lookup by the existing forty-five-tick observation");
    }
    if(mode==2||mode==4)check(laterHits>0,"Removing/transferring one struck victim does not abort unrelated valid fan receivers");
    if(mode==2)check(victims.stream().filter(e->e.getUUID().equals(callbackVictim)).findFirst().orElseThrow().isRemoved(),"Actual struck victim was removed during damage callback");
    if(mode==4)check(moved!=null&&moved.level()==s.getLevel(Level.NETHER)&&Math.abs(((LivingEntity)moved).getHealth()-16)<.001,"Transferred victim retains its one actual strike without old-world repeat damage");
    if(mode==2||mode==4)check(source.pose()==CinderBailiff.RECOVERING||source.pose()==CinderBailiff.IDLE,"Valid source completes finite native fan/recovery after victim departure");
   });
  }finally{armed=0;expectedVictims.clear();source=null;callbackVictim=null;moved=null;victims.clear();}
 }

 private void dense(ClientGameTestContext c){
  try(var w=c.worldBuilder().create()){
   c.waitTicks(25);configure(w.getServer());w.getServer().runOnServer(s->{arena(s.overworld());spawn(s);});c.waitTicks(5);
   w.getServer().runOnServer(s->{
    var l=s.overworld();var aggressor=zombie(l,source.getX(),source.getZ()+2);victims.add(aggressor);
    check(source.hurtServer(l,aggressor.damageSources().mobAttack(aggressor),1),"Dense-pool source receives actual ordinary injury");
    for(int n=0;n<12;n++){double angle=(n+.5)*Math.PI/6;victims.add(zombie(l,source.getX()+2*Math.cos(angle),source.getZ()+2*Math.sin(angle)));}
    var pool=new ArrayList<LivingEntity>(13);l.getEntities(EntityTypeTest.<Entity,LivingEntity>forClass(LivingEntity.class),source.getBoundingBox().inflate(4),e->e!=source,pool,13);
    check(pool.size()==13&&pool.stream().allMatch(e->victims.contains(e)),"Actual bounded native query is saturated by thirteen known living peers");
    for(var target:pool)check(target.getHealth()==20&&target.getMaxHealth()==20&&!target.isOnFire(),"Each dense receiver starts with exactly twenty health and no fire: "+receiverState(target));
   });
   boolean warning=false;for(int n=0;n<20;n++){c.waitTicks(2);if(w.getServer().computeOnServer(s->source.pose()==CinderBailiff.WARNING)){warning=true;break;}}check(warning,"Dense source still authors its real readable warning");
   boolean recovered=false;for(int n=0;n<40;n++){c.waitTicks(2);if(w.getServer().computeOnServer(s->source.pose()==CinderBailiff.RECOVERING)){recovered=true;break;}}check(recovered,"Actual saturated fan completes all finite vents and recovery");
   w.getServer().runOnServer(s->{for(var target:victims)check(target.getHealth()==20,"Thirteen-peer refusal harms no arbitrary hidden prefix: "+receiverState(target));check(source.attackReady()>s.overworld().getGameTime(),"Dense refusal still pays original finite attack rest");});
  }finally{armed=0;expectedVictims.clear();source=null;victims.clear();}
 }

 private Zombie zombie(ServerLevel l,double x,double z){var e=EntityTypes.ZOMBIE.create(l,EntitySpawnReason.COMMAND);check(e!=null,"Actual native peer factory");e.setNoAi(true);e.setPersistenceRequired();e.getAttribute(Attributes.ARMOR).setBaseValue(0);e.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0);
  // Supplied health witnesses must not receive the unrelated random Runebound load-time health bonus.
  e.setCustomName(net.minecraft.network.chat.Component.literal("Cinder Bailiff fan witness"));
  e.snapTo(x,30,z,0,0);l.addFreshEntity(e);check(e.getHealth()==20&&e.getMaxHealth()==20,"Actual admitted fan witness has exactly twenty health: "+receiverState(e));return e;}
 private static String receiverState(LivingEntity e){return "uuid="+e.getUUID()+", health="+e.getHealth()+", max="+e.getMaxHealth()+", fire="+e.isOnFire()+", alive="+e.isAlive()+", removed="+e.isRemoved()+", position="+e.position()+", sourceDistance="+(source==null?"none":source.distanceTo(e));}
 private void spawn(MinecraftServer s){source=EmberContent.BAILIFF.create(s.overworld(),EntitySpawnReason.COMMAND);check(source!=null,"Actual registered source factory");source.snapTo(.5,30,.5,0,0);s.overworld().addFreshEntity(source);var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.teleportTo(s.overworld(),7,30,7,Set.<Relative>of(),0,0,false);}
 private void arena(ServerLevel l){check(l!=null,"Native test level exists");for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){l.setBlock(new BlockPos(x,29,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);for(int y=30;y<=34;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}}
 private void configure(TestServerContext s){s.runCommand("difficulty normal");s.runCommand("gamerule spawn_mobs false");s.runCommand("gamerule natural_health_regeneration false");s.runCommand("time set 18000");}
 private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
}
