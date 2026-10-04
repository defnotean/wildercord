package dev.wildercord.wildlife;
import dev.wildercord.cast.feel.Feels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.entity.EntityTypeTest;
import java.util.*;
/** Territorial woodland ceramic breather: fixed three-lane physical fan, no terrain fire. */
public final class CinderBailiff extends PathfinderMob {
 public static final int IDLE=0,WARNING=1,FANNING=2,RECOVERING=3,BROWSING=4,RESTING=5;
 private static final EntityDataAccessor<Integer> POSE=SynchedEntityData.defineId(CinderBailiff.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Integer> VENT=SynchedEntityData.defineId(CinderBailiff.class,EntityDataSerializers.INT);
 private net.minecraft.resources.ResourceKey<Level> knownDimension;private long mealReady,attackReady,restUntil;private int left,epoch;private BlockPos home;private Vec3 forward=new Vec3(0,0,1);private final Set<UUID> spent=new HashSet<>();
 public float warning,warningO,fan,fanO,rest,restO;
 public CinderBailiff(EntityType<? extends CinderBailiff> t,Level l){super(t,l);xpReward=0;}
 public static AttributeSupplier.Builder attributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.MOVEMENT_SPEED,.21).add(Attributes.FOLLOW_RANGE,10).add(Attributes.ATTACK_DAMAGE,4).add(Attributes.STEP_HEIGHT,.6).add(Attributes.KNOCKBACK_RESISTANCE,.25);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(POSE,IDLE);b.define(VENT,-1);}
 @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new Goal(){{setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}public boolean canUse(){return committed();}public boolean canContinueToUse(){return committed();}public void tick(){still();}public boolean requiresUpdateEveryTick(){return true;}});goalSelector.addGoal(2,new EmberHabitatGoal(this));goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,.5));goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,6));}
 private static long clock(Level l){return l instanceof ServerLevel server?server.getServer().overworld().getGameTime():l.getGameTime();}
 public int pose(){return entityData.get(POSE);}public int vent(){return entityData.get(VENT);}public long mealReady(){return mealReady;}public long attackReady(){return attackReady;}public long restUntil(){return restUntil;}public BlockPos home(){return home;}
 public boolean mayBrowse(){return pose()==IDLE && getTarget()==null && clock(level())>=mealReady;}
 private boolean committed(){return pose()==WARNING||pose()==FANNING||pose()==RECOVERING||pose()==RESTING;}
 private void still(){getNavigation().stop();setSpeed(0);setXxa(0);setDeltaMovement(0,getDeltaMovement().y,0);}
 private void pose(int n,int ticks){entityData.set(POSE,n);left=ticks;if(committed())still();}
 void browsing(boolean yes){if(yes && pose()==IDLE)pose(BROWSING,0);else if(!yes && pose()==BROWSING)pose(IDLE,0);}
 static boolean mature(Level l,BlockPos p){return l.hasChunkAt(p)&&l.getBlockState(p).is(EmberContent.FERN)&&l.getBlockState(p).getValue(CinderFernBlock.AGE)==2&&l.getBlockState(p).getValue(CinderFernBlock.COOLED);}
 boolean meal(ServerLevel l,BlockPos p){return meal(l,p,EmberPlantAdmission.WORLD);}
 private boolean mealActor(ServerLevel l,BlockPos p){return l==level()&&isAlive()&&!isRemoved()&&pose()==BROWSING&&getTarget()==null&&clock(l)>=mealReady&&l.hasChunkAt(p)&&distanceToSqr(p.getX()+.5,p.getY(),p.getZ()+.5)<1.3&&l.clip(new ClipContext(getEyePosition(),Vec3.atCenterOf(p),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()==HitResult.Type.MISS;}
 /** Genuine reached browsing context is re-admitted after actual age consumption, before home/rest claims. */
 boolean meal(ServerLevel l,BlockPos p,EmberPlantAdmission.Writer writer){
  if(!mealActor(l,p)||!mature(l,p))return false;var old=l.getBlockState(p);var next=old.setValue(CinderFernBlock.AGE,1);long ready=mealReady;int action=epoch;
  try(var lease=EmberPlantAdmission.open(l,p)){
   if(lease==null||!writer.set(l,p,next)||!mealActor(l,p)||epoch!=action||mealReady!=ready||!l.getBlockState(p).equals(next)||!next.canSurvive(l,p))return false;
   home=p.immutable();mealReady=clock(l)+EmberRules.MEAL_REST;setTarget(null);restUntil=clock(l)+160;pose(RESTING,160);Feels.sound(l,position(),"ember_bailiff_browse",.5F,1);return true;
  }
 }

 private boolean valid(LivingEntity e){return e!=null&&e.isAlive()&&!e.isRemoved()&&e.level()==level()&&distanceToSqr(e)<64&&!(e instanceof Player p&&(p.isCreative()||p.isSpectator()));}
 private void recover(ServerLevel l){epoch++;setTarget(null);entityData.set(VENT,-1);attackReady=Math.max(attackReady,clock(l)+EmberRules.ATTACK_REST);pose(RECOVERING,EmberRules.RECOVERY);}
 /** Only call from admitted real Water/Frost outcome on this actual target. */
 public boolean cool(ServerPlayer p){if(!(level() instanceof ServerLevel l)||!isAlive()||isRemoved()||!valid(p)||!hasLineOfSight(p)||pose()!=WARNING)return false;recover(l);Feels.sound(l,position(),"ember_bailiff_cool",.5F,1);return true;}
 @Override public boolean hurtServer(ServerLevel l,DamageSource s,float amount){float before=getHealth();boolean hit=super.hurtServer(l,s,amount);if(hit && before>getHealth() && isAlive()&&!isRemoved()&&level()==l){if(pose()==RESTING||pose()==BROWSING||pose()==WARNING&&s.getEntity() instanceof ServerPlayer p&&s.getDirectEntity()==p&&s.is(DamageTypes.PLAYER_ATTACK)&&!dev.wildercord.cast.Dungeons.spellLanding())recover(l);if(s.getEntity() instanceof LivingEntity e&&valid(e)&&pose()!=FANNING)setTarget(e);}return hit;}
 @Override public void tick(){super.tick();if(level().isClientSide()){warningO=warning;fanO=fan;restO=rest;warning=WildlifeRules.approach(warning,pose()==WARNING?1:0,.1F);fan=WildlifeRules.approach(fan,pose()==FANNING?1:0,.2F);rest=WildlifeRules.approach(rest,pose()==RESTING?1:0,.06F);}}
 @Override protected void customServerAiStep(ServerLevel l){super.customServerAiStep(l);if(knownDimension==null)knownDimension=l.dimension();else if(!knownDimension.equals(l.dimension())){knownDimension=l.dimension();epoch++;home=null;restUntil=0;setTarget(null);entityData.set(VENT,-1);attackReady=Math.max(attackReady,clock(l)+EmberRules.ATTACK_REST);pose(IDLE,0);}if(committed())still();boolean peaceful=l.getDifficulty()==Difficulty.PEACEFUL;if(peaceful){setTarget(null);if(pose()==WARNING||pose()==FANNING||pose()==RECOVERING){epoch++;entityData.set(VENT,-1);pose(IDLE,0);}}
  if(pose()==RESTING){if(home==null||!l.hasChunkAt(home)||!l.getBlockState(home).is(EmberContent.FERN)||!l.getBlockState(home).getValue(CinderFernBlock.COOLED)||distanceToSqr(home.getX()+.5,home.getY(),home.getZ()+.5)>2||clock(l)>=restUntil)pose(IDLE,0);return;}
  if(pose()==RECOVERING){if(--left<=0)pose(IDLE,0);return;}
  if(pose()==WARNING){if(!valid(getTarget())){recover(l);return;}if(left%6==0)preview(l);if(--left<=0){entityData.set(VENT,-1);pose(FANNING,EmberRules.FAN);Feels.sound(l,position(),"ember_bailiff_fan",.6F,1);}return;}
  if(pose()==FANNING){int elapsed=EmberRules.FAN-left;if(elapsed==0||elapsed==6||elapsed==12){int stage=elapsed/6;entityData.set(VENT,stage);pulse(l,stage);}if(!isAlive()||isRemoved()||level()!=l||pose()!=FANNING)return;if(--left<=0){recover(l);Feels.sound(l,position(),"ember_bailiff_recover",.5F,1);}return;}
  if(peaceful)return;if(getTarget()!=null&&!valid(getTarget()))setTarget(null);if(pose()==IDLE&&tickCount%10==0&&getTarget()!=null&&(distanceToSqr(getTarget())>=16||!hasLineOfSight(getTarget())))setTarget(null);if(pose()==BROWSING||clock(l)<attackReady||tickCount%10!=0)return;
  if(getTarget()==null&&home!=null&&l.hasChunkAt(home)&&l.getBlockState(home).is(EmberContent.FERN)){var guests=new ArrayList<Player>(13);l.getEntities(EntityTypeTest.<Entity,Player>forClass(Player.class),getBoundingBox().inflate(5),e->true,guests,13);if(guests.size()<=12){guests.removeIf(e->!valid(e)||distanceToSqr(e)>=16||!hasLineOfSight(e)||e.position().distanceToSqr(Vec3.atCenterOf(home))>16);guests.sort(Comparator.comparingDouble(this::distanceToSqr));if(!guests.isEmpty())setTarget(guests.getFirst());}}
  if(valid(getTarget())&&distanceToSqr(getTarget())<16&&hasLineOfSight(getTarget())){forward=getTarget().position().subtract(position()).multiply(1,0,1).normalize();if(forward.lengthSqr()<.5)return;float yaw=(float)Math.toDegrees(Math.atan2(-forward.x,forward.z));setYRot(yaw);yBodyRot=yaw;yHeadRot=yaw;epoch++;spent.clear();attackReady=clock(l)+EmberRules.ATTACK_REST;pose(WARNING,EmberRules.WARNING);Feels.sound(l,position(),"ember_bailiff_warn",.6F,1);}
 }
 public boolean inLane(Vec3 p,int lane){var d=p.subtract(position());return EmberRules.lane(d.x,d.y,d.z,forward.x,forward.z,lane);}
 private void pulse(ServerLevel l,int lane){int token=epoch;var victims=new ArrayList<LivingEntity>(13);l.getEntities(EntityTypeTest.<Entity,LivingEntity>forClass(LivingEntity.class),getBoundingBox().inflate(4),e->e!=this,victims,13);draw(l,lane);if(victims.size()>12)return;victims.sort(Comparator.comparingDouble(this::distanceToSqr));for(var e:victims){if(!isAlive()||isRemoved()||level()!=l||pose()!=FANNING||epoch!=token)return;if(spent.size()>=EmberRules.VICTIMS)return;if(valid(e)&&inLane(e.position(),lane)&&hasLineOfSight(e)&&spent.add(e.getUUID()))e.hurtServer(l,damageSources().mobAttack(this),4);}}
 private void preview(ServerLevel l){for(int k=0;k<3;k++)for(int r=1;r<=3;r++){double a=(k-1)*Math.PI/6;var d=new Vec3(forward.x*Math.cos(a)+forward.z*Math.sin(a),0,forward.z*Math.cos(a)-forward.x*Math.sin(a));var p=position().add(d.scale(r));var at=BlockPos.containing(p);if(l.hasChunkAt(at)&&!l.getBlockState(at.below()).isAir())l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.TERRACOTTA.defaultBlockState()),p.x,p.y+.06,p.z,1,.035,.01,.035,0);}}
 private void draw(ServerLevel l,int lane){double a=(lane-1)*Math.PI/6;for(int r=1;r<=7;r++){var d=new Vec3(forward.x*Math.cos(a)+forward.z*Math.sin(a),0,forward.z*Math.cos(a)-forward.x*Math.sin(a));var p=position().add(d.scale(r*.5));if(l.hasChunkAt(BlockPos.containing(p)))l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.DYED_TERRACOTTA.gray().defaultBlockState()),p.x,p.y+.7,p.z,2,.08,.1,.08,.03);}}
 @Override protected SoundEvent getAmbientSound(){return dev.wildercord.content.WildercordSounds.kit("ember_bailiff_browse");}
 @Override protected SoundEvent getHurtSound(DamageSource d){return dev.wildercord.content.WildercordSounds.kit("ember_bailiff_recover");}
 @Override public int getAmbientSoundInterval(){return 400;}
 @Override protected void addAdditionalSaveData(ValueOutput o){super.addAdditionalSaveData(o);o.putLong("ember_meal_ready",mealReady);o.putLong("ember_attack_ready",attackReady);o.putLong("ember_rest_until",restUntil);o.putString("ember_home_dimension",level().dimension().identifier().toString());if(home!=null)o.store("ember_home",BlockPos.CODEC,home);}
 @Override protected void readAdditionalSaveData(ValueInput i){super.readAdditionalSaveData(i);mealReady=i.getLongOr("ember_meal_ready",0);attackReady=i.getLongOr("ember_attack_ready",0);restUntil=i.getLongOr("ember_rest_until",0);home=i.read("ember_home",BlockPos.CODEC).orElse(null);if(!i.getStringOr("ember_home_dimension",level().dimension().identifier().toString()).equals(level().dimension().identifier().toString())){home=null;restUntil=0;}knownDimension=level().dimension();spent.clear();epoch++;setTarget(null);entityData.set(VENT,-1);pose(home!=null&&clock(level())<restUntil?RESTING:IDLE,0);}
}
