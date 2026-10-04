package dev.wildercord.wildlife;

import dev.wildercord.cast.feel.Feels;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import java.util.Comparator;

/** A wet-bank territorial crab: raised claws warn, a fixed sweep commits, then the shell settles. */
public final class ReedbackCrab extends PathfinderMob {
 private static final EntityDataAccessor<Integer> POSE=SynchedEntityData.defineId(ReedbackCrab.class,EntityDataSerializers.INT);
 public static final int IDLE=0,WARNING=1,SWEEP=2,RECOVERY=3,CALM=4;
 private BlockPos home;private int left,epoch;private Vec3 facing=new Vec3(0,0,1);private long calmUntil,responseReady;
 public ReedbackCrab(EntityType<? extends ReedbackCrab> t,Level l) {super(t,l);setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER,0);xpReward=2;}
 public static AttributeSupplier.Builder attributes() {return createMobAttributes().add(Attributes.MAX_HEALTH,24).add(Attributes.MOVEMENT_SPEED,.18).add(Attributes.FOLLOW_RANGE,12).add(Attributes.ATTACK_DAMAGE,5).add(Attributes.STEP_HEIGHT,.6).add(Attributes.KNOCKBACK_RESISTANCE,.3);}
 @Override protected PathNavigation createNavigation(Level l) {return new AmphibiousPathNavigation(this,l);}
 @Override public boolean canBreatheUnderwater() {return true;}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b) {super.defineSynchedData(b);b.define(POSE,IDLE);}
 @Override protected void registerGoals() {goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,.65));goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,6));}
 public float claws,clawsO,strike,strikeO,settle,settleO;
 @Override public void tick() {super.tick();if(level().isClientSide()) {clawsO=claws;strikeO=strike;settleO=settle;claws=WildlifeRules.approach(claws,pose()==WARNING?1:0,.1F);strike=WildlifeRules.approach(strike,pose()==SWEEP?1:0,.25F);settle=WildlifeRules.approach(settle,pose()==CALM || pose()==RECOVERY?1:0,.08F);}}
 public int pose() {return entityData.get(POSE);} public long calmUntil() {return calmUntil;} public long responseReady() {return responseReady;}
 private void pose(int p,int ticks) {epoch++;entityData.set(POSE,p);left=ticks;getNavigation().stop();}
 private boolean valid(LivingEntity e) {return e!=null && e.isAlive() && !e.isRemoved() && e.level()==level() && level() instanceof ServerLevel world && world.getEntity(e.getUUID())==e && !(e instanceof Player p && (p.isCreative() || p.isSpectator())) && home!=null && e.distanceToSqr(Vec3.atBottomCenterOf(home))<144;}
 private void sound(String id) {
  String cue=switch(id) {case "warn" -> "wetland_crab_warn";case "sweep" -> "wetland_crab_sweep";case "calm" -> "wetland_crab_calm";case "stagger" -> "wetland_crab_stagger";default -> throw new IllegalArgumentException("Unknown crab cue: "+id);};
  Feels.sound((ServerLevel)level(),position(),cue,.7F,1);
 }
 /** Paid impact hooks may calm the warning or interrupt its commitment, within a separate saved response rest. */
 public boolean answerMagic(boolean water) {
  if(!(level() instanceof ServerLevel l) || !isAlive() || l.getGameTime()<responseReady)return false;
  responseReady=l.getGameTime()+200;getNavigation().stop();setTarget(null);
  if(water) {calmUntil=l.getGameTime()+200;pose(CALM,0);sound("calm");}
  else {pose(RECOVERY,60);sound("stagger");}
  return true;
 }
 /** A handmade instrument answers only raised warning claws. It cannot interrupt a committed sweep. */
 public boolean answerRattle() {
  if(!(level() instanceof ServerLevel l) || !isAlive() || pose()!=WARNING || l.getGameTime()<responseReady)return false;
  responseReady=l.getGameTime()+200;calmUntil=l.getGameTime()+ReedRattle.CALM_TICKS;
  getNavigation().stop();setTarget(null);pose(CALM,0);sound("calm");return true;
 }
 @Override protected void customServerAiStep(ServerLevel l) {
  super.customServerAiStep(l);if(home==null)home=blockPosition();
  if(l.getDifficulty()==Difficulty.PEACEFUL || !dev.wildercord.config.Config.get().wildlife().enabled()) {setTarget(null);pose(IDLE,0);return;}
  if(l.getGameTime()<calmUntil) {setTarget(null);pose(CALM,0);return;}
  if(pose()==CALM)pose(IDLE,0);
  if(left>0) {
   getNavigation().stop();setYRot((float)Math.toDegrees(Math.atan2(-facing.x,facing.z)));setYBodyRot(getYRot());
   if(--left==0) {
    if(pose()==WARNING) {
     pose(SWEEP,12);sound("sweep");
     sweep(l);
    } else if(pose()==SWEEP)pose(RECOVERY,60);else pose(IDLE,0);
   }
   return;
  }
  if(!valid(getTarget()))setTarget(null);
  if(getTarget()==null && tickCount%10==0) {
   var pool=WetlandQueries.scan(l,Player.class,getBoundingBox().inflate(5),null);var target=pool.saturated()?null:pool.entities().stream().filter(p -> valid(p) && hasLineOfSight(p) && (!p.isShiftKeyDown() || distanceToSqr(p)<4)).min(Comparator.<Player>comparingDouble(this::distanceToSqr).thenComparingInt(Entity::getId)).orElse(null);setTarget(target);
  }
  if(getTarget()!=null) {
   if(distanceToSqr(getTarget())>16) {getNavigation().moveTo(getTarget(),.85);return;}
   var delta=getTarget().position().subtract(position()).multiply(1,0,1);if(delta.lengthSqr()>.01)facing=delta.normalize();pose(WARNING,40);sound("warn");
  } else if(distanceToSqr(Vec3.atBottomCenterOf(home))>100)getNavigation().moveTo(home.getX()+.5,home.getY(),home.getZ()+.5,.7);
 }
 @Override public boolean hurtServer(ServerLevel l,DamageSource s,float amount) {boolean hit=super.hurtServer(l,s,amount);if(hit && s.getEntity() instanceof LivingEntity e) {calmUntil=0;if(pose()==CALM)pose(IDLE,0);if(valid(e))setTarget(e);}return hit;}
 private boolean current(ServerLevel l,int action,Vec3 origin,LivingEntity quarry) {
  return isAlive()&&!isRemoved()&&level()==l&&l.getEntity(getUUID())==this
   &&pose()==SWEEP&&epoch==action&&position().equals(origin)&&getTarget()==quarry
   &&l.getDifficulty()!=Difficulty.PEACEFUL&&dev.wildercord.config.Config.get().wildlife().enabled();
 }
 private void sweep(ServerLevel l) {
  int action=epoch;var origin=position();var quarry=getTarget();var direction=facing;
  var pool=WetlandQueries.scan(l,LivingEntity.class,getBoundingBox().inflate(2.8),this);
  if(pool.saturated())return;
  for(var e:pool.entities()) {
   if(!current(l,action,origin,quarry))return;
   if(!valid(e)||(!(e instanceof Player)&&e!=quarry))continue;
   var victim=e.position();var delta=victim.subtract(origin);double forward=delta.dot(direction),side=Math.abs(delta.dot(new Vec3(-direction.z,0,direction.x)));
   if(forward<=.1||forward>=2.8||side>=1.25||Math.abs(delta.y)>=1.4||!hasLineOfSight(e))continue;
   var source=damageSources().mobAttack(this);boolean admitted=e.hurtServer(l,source,5);
   // Native damage callbacks can remove, move, transfer, calm or replace either actor.
   if(!current(l,action,origin,quarry))return;
   if(admitted&&valid(e)&&e.position().equals(victim))e.knockback(.45,-direction.x,-direction.z,source,0);
  }
 }
 @Override protected SoundEvent getAmbientSound() {return dev.wildercord.content.WildercordSounds.kit("wetland_crab_call");}
 @Override protected SoundEvent getHurtSound(DamageSource s) {return dev.wildercord.content.WildercordSounds.kit("wetland_crab_hurt");}
 @Override protected SoundEvent getDeathSound() {return dev.wildercord.content.WildercordSounds.kit("wetland_crab_death");}
 @Override protected void addAdditionalSaveData(ValueOutput o) {super.addAdditionalSaveData(o);if(home!=null) {o.putInt("bank_x",home.getX());o.putInt("bank_y",home.getY());o.putInt("bank_z",home.getZ());}o.putLong("calm_until",calmUntil);o.putLong("response_ready",responseReady);}
 @Override protected void readAdditionalSaveData(ValueInput i) {super.readAdditionalSaveData(i);home=new BlockPos(i.getIntOr("bank_x",blockPosition().getX()),i.getIntOr("bank_y",blockPosition().getY()),i.getIntOr("bank_z",blockPosition().getZ()));calmUntil=i.getLongOr("calm_until",0);responseReady=i.getLongOr("response_ready",0);pose(RECOVERY,60);}
}
