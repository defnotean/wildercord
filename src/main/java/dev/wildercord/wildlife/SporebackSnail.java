package dev.wildercord.wildlife;
import dev.wildercord.cast.feel.Feels;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import java.util.*;
/** A quiet fungal browser: the living mushroom remains, and one saved dew reserve needs a real visit. */
public final class SporebackSnail extends PathfinderMob {
 private static final EntityDataAccessor<Integer> POSE=SynchedEntityData.defineId(SporebackSnail.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Boolean> DEW=SynchedEntityData.defineId(SporebackSnail.class,EntityDataSerializers.BOOLEAN);
 private boolean shelterRest;
 private long gatherReady,forageReady,responseReady,hiddenUntil,answerUntil;
 public float hide,hideO;
 public SporebackSnail(EntityType<? extends SporebackSnail> type,Level l) {super(type,l);xpReward=0;}
 public static AttributeSupplier.Builder attributes() {return createMobAttributes().add(Attributes.MAX_HEALTH,12).add(Attributes.MOVEMENT_SPEED,.13).add(Attributes.FOLLOW_RANGE,8).add(Attributes.STEP_HEIGHT,.6);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b) {super.defineSynchedData(b);b.define(POSE,0);b.define(DEW,false);}
 public int pose() {return entityData.get(POSE);} public boolean dew() {return entityData.get(DEW);}
 public long gatherReady() {return gatherReady;} public long responseReady() {return responseReady;} public long hiddenUntil() {return hiddenUntil;}
 @Override public void tick() {super.tick();if(level().isClientSide()) {hideO=hide;hide=WildlifeRules.approach(hide,pose()==2?1:0,.08F);}}
 @Override protected void registerGoals() {goalSelector.addGoal(1,new Visit());goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,.5,120));goalSelector.addGoal(8,new RandomLookAroundGoal(this));}
 public static boolean fungus(Level l,BlockPos p) {return l.hasChunkAt(p) && (l.getBlockState(p).is(Blocks.BROWN_MUSHROOM) || l.getBlockState(p).is(Blocks.RED_MUSHROOM));}
 public boolean answerMagic(boolean fire) {
  if(!(level() instanceof ServerLevel l) || !isAlive() || l.getGameTime()<responseReady)return false;
  responseReady=l.getGameTime()+SporebackRules.RESPONSE_REST;
  if(fire) {hiddenUntil=l.getGameTime()+SporebackRules.HIDE_TICKS;answerUntil=0;entityData.set(POSE,2);getNavigation().stop();Feels.sound(l,position(),"sporeback_hide",.45F,1);}
  else if(l.getGameTime()>=hiddenUntil) {answerUntil=l.getGameTime()+60;Feels.sound(l,position(),"sporeback_answer",.4F,1);}
  return true;
 }
 @Override protected void customServerAiStep(ServerLevel l) {super.customServerAiStep(l);if(l.getGameTime()<hiddenUntil) {entityData.set(POSE,2);getNavigation().stop();setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);}else if(pose()==2 && !shelterRest)entityData.set(POSE,0);if(answerUntil>l.getGameTime() && tickCount%12==0)l.sendParticles(net.minecraft.core.particles.ParticleTypes.SPORE_BLOSSOM_AIR,getX(),getY()+.5,getZ(),2,.12,.05,.12,0);}
 @Override public boolean hurtServer(ServerLevel l,DamageSource s,float amount) {boolean hit=super.hurtServer(l,s,amount);if(hit) {hiddenUntil=l.getGameTime()+SporebackRules.HIDE_TICKS;answerUntil=0;entityData.set(POSE,2);getNavigation().stop();}return hit;}
 @Override protected InteractionResult mobInteract(Player player,InteractionHand hand) {
  if(player.isSpectator() || !player.isAlive() || player.level()!=level() || distanceToSqr(player)>9 || !player.hasLineOfSight(this))return InteractionResult.PASS;
  if(!player.getItemInHand(hand).isEmpty())return super.mobInteract(player,hand);
  if(level() instanceof ServerLevel l) {
   if(!player.isShiftKeyDown()) {player.sendOverlayMessage(Component.translatable("message.wildercord.sporeback.quiet"));return InteractionResult.SUCCESS;}
   if(!isAlive() || pose()==2 || l.getGameTime()<hiddenUntil || !dew() || l.getGameTime()<gatherReady) {player.sendOverlayMessage(Component.translatable("message.wildercord.sporeback.wait"));return InteractionResult.SUCCESS;}
   entityData.set(DEW,false);gatherReady=l.getGameTime()+SporebackRules.GATHER_REST;
   spawnAtLocation(l,new ItemStack(SporebackContent.DEW));Feels.sound(l,position(),"sporeback_gather",.5F,1);
   var notes=new java.util.ArrayList<>(dev.wildercord.player.Heart.grimoire(player));
   if(!notes.contains("field:sporeback")) {notes.add("field:sporeback");player.setAttached(dev.wildercord.player.WildercordAttachments.GRIMOIRE,List.copyOf(notes));player.getInventory().placeItemBackInInventory(new ItemStack(SporebackContent.JOURNAL),net.minecraft.util.Prediction.SERVER_ONLY);}
  }return InteractionResult.SUCCESS;
 }
 @Override protected SoundEvent getAmbientSound() {return dev.wildercord.content.WildercordSounds.kit("sporeback_call");}
 @Override protected SoundEvent getHurtSound(DamageSource s) {return dev.wildercord.content.WildercordSounds.kit("sporeback_hide");}
 @Override public int getAmbientSoundInterval() {return 320;}
 @Override protected void addAdditionalSaveData(ValueOutput o) {super.addAdditionalSaveData(o);o.putLong("gather_ready",gatherReady);o.putLong("forage_ready",forageReady);o.putLong("response_ready",responseReady);o.putLong("hidden_until",hiddenUntil);o.putBoolean("dew",dew());}
 @Override protected void readAdditionalSaveData(ValueInput i) {super.readAdditionalSaveData(i);gatherReady=i.getLongOr("gather_ready",0);forageReady=i.getLongOr("forage_ready",0);responseReady=i.getLongOr("response_ready",0);hiddenUntil=i.getLongOr("hidden_until",0);entityData.set(DEW,i.getBooleanOr("dew",false));entityData.set(POSE,0);answerUntil=0;}
 private final class Visit extends Goal {
  private HabitatSweep sweep;private BlockPos destination;private int scanAt,left,chew,searchAt,retries;private boolean shelter;
  Visit() {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
  private boolean wantsShelter() {return level().getMaxLocalRawBrightness(blockPosition())>8;}
  private boolean cover(BlockPos p) {if(!level().hasChunkAt(p) || level().getMaxLocalRawBrightness(p)>8)return false;for(int y=1;y<=3;y++)if(level().hasChunkAt(p.above(y)) && level().getBlockState(p.above(y)).isSolidRender())return true;return false;}
  private boolean habitat(BlockPos p) {return shelter?level().hasChunkAt(p) && level().getBlockState(p).isAir() && cover(p):fungus(level(),p);}
  @Override public boolean canUse() {return tickCount>=searchAt && level().getGameTime()>=hiddenUntil && (wantsShelter() || (!dew() && level().getGameTime()>=forageReady));}
  @Override public void start() {shelter=wantsShelter();sweep=new HabitatSweep(4);var p=blockPosition();sweep.anchor(p.getX(),p.getY(),p.getZ());destination=null;left=260;chew=0;scanAt=tickCount;retries=0;getNavigation().stop();}
  @Override public boolean canContinueToUse() {return left>0 && chew<40 && level().getGameTime()>=hiddenUntil && (destination==null || habitat(destination));}
  private void seek() {if(tickCount<scanAt)return;scanAt=tickCount+20;int paths=0;for(int k=0;k<8;k++) {var offset=sweep.next();if(offset==null) {left=0;return;}for(int dy=-1;dy<=1;dy++) {var p=new BlockPos(sweep.x()+offset.x(),sweep.y()+dy,sweep.z()+offset.z());if(!habitat(p))continue;if(paths>=2)return;paths++;var path=getNavigation().createPath(p,0);if(path!=null && path.canReach()) {destination=p;getNavigation().moveTo(path,.6);return;}}}}
  @Override public void tick() {left--;if(destination==null) {seek();return;}boolean near=(!shelter || cover(blockPosition())) && distanceToSqr(destination.getX()+.5,destination.getY(),destination.getZ()+.5)<.7 && level().clip(new net.minecraft.world.level.ClipContext(getEyePosition(),net.minecraft.world.phys.Vec3.atCenterOf(destination),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,SporebackSnail.this)).getType()==net.minecraft.world.phys.HitResult.Type.MISS;if(near) {getNavigation().stop();shelterRest=shelter;entityData.set(POSE,shelter?2:1);if(++chew==40 && !shelter) {entityData.set(DEW,true);forageReady=level().getGameTime()+SporebackRules.FORAGE_REST;Feels.sound((ServerLevel)level(),position(),"sporeback_browse",.35F,1);}}else {chew=0;entityData.set(POSE,0);if(getNavigation().isDone() && tickCount%20==0 && retries++<3) {var path=getNavigation().createPath(destination,0);if(path!=null && path.canReach())getNavigation().moveTo(path,.6);}}}
  @Override public boolean requiresUpdateEveryTick() {return true;}
  @Override public void stop() {shelterRest=false;destination=null;sweep=null;getNavigation().stop();entityData.set(POSE,level().getGameTime()<hiddenUntil?2:0);searchAt=tickCount+120;}
 }
}
