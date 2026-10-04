package dev.wildercord.wildlife;
import dev.wildercord.cast.feel.Feels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import java.util.*;
/** Territorial cap consumer with a committed physical root rake and a short releasable hold. */
public final class RootmoltStrider extends Monster {
 public static final int IDLE=0,WARNING=1,RAKING=2,HOLDING=3,RECOVERING=4,BROWSING=5;
 private static final EntityDataAccessor<Integer> POSE=SynchedEntityData.defineId(RootmoltStrider.class,EntityDataSerializers.INT);
 private long mealReady,attackReady;private int left;private boolean rakeSpent;private Vec3 line=new Vec3(0,0,1);private LivingEntity held;private BlockPos guardedCap;
 public float warn,warnO,rake,rakeO,feed,feedO;
 public RootmoltStrider(EntityType<? extends RootmoltStrider> t,Level l) {super(t,l);xpReward=0;}
 public static AttributeSupplier.Builder attributes() {return createMonsterAttributes().add(Attributes.MAX_HEALTH,28).add(Attributes.MOVEMENT_SPEED,.19).add(Attributes.FOLLOW_RANGE,10).add(Attributes.ATTACK_DAMAGE,4).add(Attributes.STEP_HEIGHT,.6).add(Attributes.KNOCKBACK_RESISTANCE,.2);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b) {super.defineSynchedData(b);b.define(POSE,IDLE);}
 @Override protected void registerGoals() {goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new Goal() { {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));} public boolean canUse() {return committed();} public boolean canContinueToUse() {return committed();} public void tick() {holdStill();} public boolean requiresUpdateEveryTick() {return true;} });goalSelector.addGoal(2,new RootmoltBrowseGoal(this));goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,.55));goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,6));}
 public int pose() {return entityData.get(POSE);} public long mealReady() {return mealReady;} public long attackReady() {return attackReady;}
 public boolean holding(LivingEntity e) {return pose()==HOLDING && held==e;}
 public boolean canBrowse() {return pose()==IDLE && getTarget()==null && level().getGameTime()>=mealReady;}
 public void beginBrowse(BlockPos p) {entityData.set(POSE,BROWSING);}
 public void stopBrowse() {if(pose()==BROWSING)entityData.set(POSE,IDLE);}
 boolean tryMeal(ServerLevel l,BlockPos p) {
  if(l!=level() || !isAlive() || isRemoved() || pose()!=BROWSING || l.getGameTime()<mealReady || !l.hasChunkAt(p) || distanceToSqr(p.getX()+.5,p.getY(),p.getZ()+.5)>=.85)return false;
  var state=l.getBlockState(p);if(!state.is(FungalGarden.GLOWCAP) || state.getValue(GlowcapBlock.AGE)!=2 || l.clip(new ClipContext(getEyePosition(),Vec3.atCenterOf(p),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()!=HitResult.Type.MISS)return false;
  if(!l.setBlock(p,state.setValue(GlowcapBlock.AGE,0),net.minecraft.world.level.block.Block.UPDATE_CLIENTS))return false;mealReady=l.getGameTime()+RootmoltRules.MEAL_REST;guardedCap=p.immutable();Feels.sound(l,position(),"rootmolt_meal",.45F,1);RootmoltContent.observedMeal(this,p);return true;
 }
 public boolean nearGuardedCap(Vec3 p) {return guardedCap!=null && level().hasChunkAt(guardedCap) && level().getBlockState(guardedCap).is(FungalGarden.GLOWCAP) && p.distanceToSqr(Vec3.atCenterOf(guardedCap))<25;}
 private boolean valid(LivingEntity e) {return e!=null && e.isAlive() && !e.isRemoved() && e.level()==level() && distanceToSqr(e)<36 && !(e instanceof Player p && (p.isSpectator() || p.isCreative()));}
 private boolean committed() {return pose()==WARNING || pose()==RAKING || pose()==HOLDING || pose()==RECOVERING;}
 private void holdStill() {getNavigation().stop();setSpeed(0);setXxa(0);var d=getDeltaMovement();setDeltaMovement(0,d.y,0);}
 private void pose(int n,int ticks) {if(n==WARNING)rakeSpent=false;entityData.set(POSE,n);left=ticks;holdStill();}
 private void release() {if(held!=null && held.getAttachedOrElse(RootmoltContent.GRAB_OWNER,RootmoltContent.NO_OWNER).equals(getUUID())) {held.removeEffect(RootmoltContent.TETHER);held.setAttached(RootmoltContent.GRAB_OWNER,RootmoltContent.NO_OWNER);}held=null;}
 private void recover(ServerLevel l) {release();setTarget(null);attackReady=Math.max(attackReady,l.getGameTime()+RootmoltRules.ATTACK_REST);pose(RECOVERING,RootmoltRules.RECOVERY);}
 /** Authoritative visible bell answer. Range/ownership/rest are checked by the relic caller too. */
 public boolean answerBell(ServerPlayer p) {if(!(level() instanceof ServerLevel l) || !isAlive() || isRemoved() || p.isSpectator() || p.isCreative() || !p.isAlive() || p.level()!=l || distanceToSqr(p)>25 || !hasLineOfSight(p) || (pose()!=WARNING && pose()!=RAKING && pose()!=HOLDING))return false;RootmoltContent.countered(p,this);recover(l);Feels.sound(l,position(),"rootmolt_release",.5F,1);return true;}
 @Override public boolean hurtServer(ServerLevel l,DamageSource source,float amount) {boolean wasCounter=pose()==WARNING || pose()==RAKING || pose()==HOLDING;boolean hit=super.hurtServer(l,source,amount);if(hit) {if(wasCounter && source.getEntity() instanceof ServerPlayer p && source.getDirectEntity()==p && source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK) && !dev.wildercord.cast.Dungeons.spellLanding())RootmoltContent.countered(p,this);recover(l);if(source.getEntity() instanceof LivingEntity e && valid(e))setTarget(e);}return hit;}
 @Override public void die(DamageSource s) {release();super.die(s);}
 @Override public void remove(RemovalReason reason) {release();super.remove(reason);}
 @Override public void tick() {super.tick();if(level().isClientSide()) {warnO=warn;rakeO=rake;feedO=feed;warn=WildlifeRules.approach(warn,pose()==WARNING?1:0,.09F);rake=WildlifeRules.approach(rake,pose()==RAKING || pose()==HOLDING?1:0,.2F);feed=WildlifeRules.approach(feed,pose()==BROWSING?1:0,.08F);}}
 @Override protected void customServerAiStep(ServerLevel l) {
  super.customServerAiStep(l);if(committed())holdStill();
  if(l.getDifficulty()==Difficulty.PEACEFUL) {release();setTarget(null);pose(IDLE,0);return;}
  if(pose()==HOLDING) {if(!valid(held) || !held.hasEffect(RootmoltContent.TETHER) || !held.getAttachedOrElse(RootmoltContent.GRAB_OWNER,RootmoltContent.NO_OWNER).equals(getUUID()) || !hasLineOfSight(held) || distanceToSqr(held)>16 || --left<=0)recover(l);return;}
  if(pose()==RECOVERING) {if(--left<=0)pose(IDLE,0);return;}
  if(pose()==WARNING) {
   if(!valid(getTarget())) {recover(l);return;}
   if(tickCount%4==0)drawLine(l);if(tickCount%20==0)RootmoltContent.alarm(this);
   if(--left<=0) {pose(RAKING,RootmoltRules.RAKE);Feels.sound(l,position(),"rootmolt_rake",.65F,1);}
   return;
  }
  if(pose()==RAKING) {
   // Spend before calling external damage/equipment hooks. A changed target cannot reopen this same rake.
   if(rakeSpent){recover(l);return;}
   var target=getTarget();if(valid(target) && hasLineOfSight(target) && onLine(target.position())) {
    rakeSpent=true;
    boolean hit=target.hurtServer(l,damageSources().mobAttack(this),4);
    if(hit && target.isAlive() && !target.isRemoved() && target.getAttachedOrElse(RootmoltContent.GRAB_OWNER,RootmoltContent.NO_OWNER).equals(RootmoltContent.NO_OWNER)) {if(RootmoltContent.resistRoot(target,this)) {recover(l);return;}
     // A callback may change actors; re-admit the original contact before claiming control.
     if(!isAlive() || isRemoved())return;
     if(level()!=l || pose()!=RAKING || getTarget()!=target)return;
     if(!valid(target) || !hasLineOfSight(target) || !onLine(target.position()) || !target.getAttachedOrElse(RootmoltContent.GRAB_OWNER,RootmoltContent.NO_OWNER).equals(RootmoltContent.NO_OWNER)){recover(l);return;}
     held=target;held.setAttached(RootmoltContent.GRAB_OWNER,getUUID());if(!held.addEffect(new MobEffectInstance(RootmoltContent.TETHER,RootmoltRules.HELD,0,false,false,true),this)) {held.setAttached(RootmoltContent.GRAB_OWNER,RootmoltContent.NO_OWNER);held=null;recover(l);return;}pose(HOLDING,RootmoltRules.HELD);}
    else recover(l);
   }else if(--left<=0) {if(target instanceof ServerPlayer p && valid(p) && hasLineOfSight(p)) {var d=p.position().subtract(position());double along=d.dot(line),side=Math.abs(d.x*line.z-d.z*line.x);if(along>=0 && along<=3.4 && side>=.8 && Math.abs(d.y)<1.3)RootmoltContent.countered(p,this);}recover(l);}
   return;
  }
  if(getTarget()!=null && !valid(getTarget()))setTarget(null);if(pose()==BROWSING || l.getGameTime()<attackReady || tickCount%10!=0)return;
  if(!valid(getTarget()) && guardedCap!=null) {var p=l.getNearestPlayer(getX(),getY(),getZ(),5,e -> e instanceof Player player && valid(player) && nearGuardedCap(player.position()));if(p!=null)setTarget(p);}
  if(valid(getTarget()) && distanceToSqr(getTarget())<16 && hasLineOfSight(getTarget())) {line=getTarget().position().subtract(position()).multiply(1,0,1).normalize();float yaw=(float)Math.toDegrees(Math.atan2(-line.x,line.z));setYRot(yaw);yBodyRot=yaw;yHeadRot=yaw;pose(WARNING,RootmoltRules.WINDUP);attackReady=l.getGameTime()+RootmoltRules.ATTACK_REST;Feels.sound(l,position(),"rootmolt_warn",.55F,1);}
 }
 boolean onLine(Vec3 p) {var d=p.subtract(position());double along=d.dot(line),side=Math.abs(d.x*line.z-d.z*line.x);return along>=0 && along<=3.4 && side<.6 && Math.abs(d.y)<1.3;}
 private void drawLine(ServerLevel l) {for(int i=1;i<=6;i++) {var p=position().add(line.scale(i*.5));var at=BlockPos.containing(p);if(l.hasChunkAt(at) && !l.getBlockState(at.below()).isAir())l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.ROOTED_DIRT.defaultBlockState()),p.x,p.y+.06,p.z,1,.04,.02,.04,0);}}
 @Override protected SoundEvent getAmbientSound() {return dev.wildercord.content.WildercordSounds.kit("rootmolt_call");}
 @Override protected SoundEvent getHurtSound(DamageSource d) {return dev.wildercord.content.WildercordSounds.kit("rootmolt_release");}
 @Override public int getAmbientSoundInterval() {return 360;}
 @Override protected void addAdditionalSaveData(ValueOutput o) {super.addAdditionalSaveData(o);o.putLong("meal_ready",mealReady);o.putLong("attack_ready",attackReady);if(guardedCap!=null)o.store("guarded_cap",BlockPos.CODEC,guardedCap);}
 @Override protected void readAdditionalSaveData(ValueInput i) {super.readAdditionalSaveData(i);mealReady=i.getLongOr("meal_ready",0);attackReady=i.getLongOr("attack_ready",0);held=null;guardedCap=i.read("guarded_cap",BlockPos.CODEC).orElse(null);pose(IDLE,0);setTarget(null);}
}
