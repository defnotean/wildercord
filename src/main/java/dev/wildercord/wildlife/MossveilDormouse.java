package dev.wildercord.wildlife;

import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import java.util.*;

/** Gentle fungal companion; actual feeding and ownership, no combat, breeding or material output. */
public final class MossveilDormouse extends TamableAnimal {
 public static final int FEED_REST=100,CLAIM_LIFE=6000,FILTER_REST=200;
 private static final EntityDataAccessor<Integer> SNIFF=SynchedEntityData.defineId(MossveilDormouse.class,EntityDataSerializers.INT);
 private UUID claimant;private int fed;private long claimUntil,feedReady,filterReady;private boolean operating;private Follow follow;
 public float curl,curlO,sniff,sniffO;
 public MossveilDormouse(EntityType<? extends MossveilDormouse> type,Level l){super(type,l);}
 public static AttributeSupplier.Builder attributes(){return createAnimalAttributes().add(Attributes.MAX_HEALTH,8).add(Attributes.MOVEMENT_SPEED,.23).add(Attributes.FOLLOW_RANGE,16).add(Attributes.STEP_HEIGHT,.6);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(SNIFF,0);}
 @Override protected void registerGoals(){
  goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new SitWhenOrderedToGoal(this));
  follow=new Follow();goalSelector.addGoal(2,follow);goalSelector.addGoal(3,new TemptGoal(this,.8,s->s.is(FungalGarden.GILLS),false){public boolean canUse(){return !isTame()&&super.canUse();}public boolean canContinueToUse(){return !isTame()&&super.canContinueToUse();}});
  goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,.65));goalSelector.addGoal(6,new RandomLookAroundGoal(this));
 }
 @Override public void tick(){super.tick();if(level().isClientSide()){curlO=curl;curl=WildlifeRules.approach(curl,isInSittingPose()?1:0,.12F);sniffO=sniff;sniff=WildlifeRules.approach(sniff,entityData.get(SNIFF)>0?1:0,.2F);}}
 @Override protected void customServerAiStep(ServerLevel l){super.customServerAiStep(l);int sniffLeft=entityData.get(SNIFF);if(sniffLeft>0)entityData.set(SNIFF,sniffLeft-1);else if(tickCount%80==getId()%80){entityData.set(SNIFF,12);voice("sniff");}}
 public static long clock(Level l){return l instanceof ServerLevel s?s.getServer().overworld().getGameTime():l.getGameTime();}
 public int feedings(){return fed;}public UUID claimant(){return claimant;}public long feedReady(){return feedReady;}public long claimUntil(){return claimUntil;}public long filterReady(){return filterReady;}
 public boolean followingOwner(){return follow!=null&&follow.running;}
 void reserveFilter(long until){filterReady=Math.max(filterReady,until);}
 private void voice(String n){if(level() instanceof ServerLevel l)dev.wildercord.cast.feel.Feels.sound(l,position(),"mossveil_"+n,.25F,1);}
 @Override protected void playStepSound(net.minecraft.core.BlockPos at,net.minecraft.world.level.block.state.BlockState state){voice("step");}
 public boolean live(ServerLevel l){return isAlive()&&!isRemoved()&&level()==l&&l.hasChunkAt(blockPosition())&&l.getEntity(getUUID())==this;}
 private boolean actor(ServerPlayer p){return live(p.level())&&p.isAlive()&&!p.isRemoved()&&!p.isCreative()&&!p.isSpectator()&&p.level().getEntity(p.getUUID())==p&&p.distanceToSqr(this)<=9&&p.hasLineOfSight(this);}
 @Override public InteractionResult mobInteract(Player who,InteractionHand hand){
  if(!(who instanceof ServerPlayer p))return who.isSpectator()?InteractionResult.PASS:InteractionResult.SUCCESS;
  if(operating||!actor(p))return InteractionResult.PASS;operating=true;
  try{
   var held=p.getItemInHand(hand);long now=clock(level());
   if(!isTame()){
    if(!held.is(FungalGarden.GILLS)||now<feedReady)return InteractionResult.PASS;
    if(claimant!=null&&MossveilRules.expired(now,claimUntil)){claimant=null;fed=0;claimUntil=0;}
    if(claimant!=null&&!claimant.equals(p.getUUID()))return InteractionResult.PASS;
    var world=p.level();var body=position();var payer=p.position();
    feedReady=now+FEED_REST;held.consume(1,p);
    if(!actor(p)||p.level()!=world||!position().equals(body)||!p.position().equals(payer)||p.getItemInHand(hand)!=held)return InteractionResult.SUCCESS;
    if(claimant==null){claimant=p.getUUID();claimUntil=now+CLAIM_LIFE;}fed++;
    if(fed==3){tame(p);claimant=null;fed=0;claimUntil=0;if(actor(p)&&p.level()==world&&position().equals(body)&&p.position().equals(payer)&&isOwnedBy(p)){setOrderedToSit(true);getNavigation().stop();voice("trust");}}
    else{entityData.set(SNIFF,16);voice("nibble");}
    return InteractionResult.SUCCESS;
   }
   if(!isOwnedBy(p))return InteractionResult.PASS;
   if(held.is(FungalGarden.GILLS)){
    if(now<feedReady||getHealth()>=getMaxHealth())return InteractionResult.PASS;
    var body=position();var payer=p.position();var world=p.level();float before=getHealth();feedReady=now+FEED_REST;held.consume(1,p);
    if(!actor(p)||p.level()!=world||!position().equals(body)||!p.position().equals(payer)||p.getItemInHand(hand)!=held||!isOwnedBy(p)||getHealth()!=before)return InteractionResult.SUCCESS;
    heal(Math.min(2,getMaxHealth()-getHealth()));if(actor(p)&&p.level()==world&&position().equals(body)&&p.position().equals(payer)&&isOwnedBy(p)&&getHealth()>before)voice("nibble");return InteractionResult.SUCCESS;
   }
   if(!held.isEmpty())return InteractionResult.PASS;
   setOrderedToSit(!isOrderedToSit());getNavigation().stop();setTarget(null);setDeltaMovement(0,getDeltaMovement().y,0);voice(isOrderedToSit()?"curl":"idle");return InteractionResult.SUCCESS.withoutItem();
  }finally{operating=false;}
 }
 @Override public boolean isFood(ItemStack s){return false;}
 @Override public boolean canFallInLove(){return false;}
 @Override public AgeableMob getBreedOffspring(ServerLevel l,AgeableMob other){return null;}
 @Override public boolean shouldTryTeleportToOwner(){return false;}
 @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(claimant!=null)out.putString("mossveil_claimant",claimant.toString());out.putInt("mossveil_feedings",fed);out.putLong("mossveil_claim_until",claimUntil);out.putLong("mossveil_feed_ready",feedReady);out.putLong("mossveil_filter_ready",filterReady);}
 @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);claimant=null;try{String id=in.getStringOr("mossveil_claimant","");if(!id.isEmpty())claimant=UUID.fromString(id);}catch(IllegalArgumentException ignored){}fed=Math.clamp(in.getIntOr("mossveil_feedings",0),0,2);claimUntil=in.getLongOr("mossveil_claim_until",0);feedReady=in.getLongOr("mossveil_feed_ready",0);filterReady=in.getLongOr("mossveil_filter_ready",0);if(isTame()||claimant==null){claimant=null;fed=0;claimUntil=0;}operating=false;entityData.set(SNIFF,0);}
 private final class Follow extends Goal{
  private LivingEntity owner;private int repath;private boolean running;
  Follow(){setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
  private boolean eligible(){return isTame()&&owner!=null&&getOwner()==owner&&owner.isAlive()&&!owner.isRemoved()&&owner.level()==level()&&level() instanceof ServerLevel l&&l.getEntity(owner.getUUID())==owner&&!owner.isSpectator()&&!isOrderedToSit()&&distanceToSqr(owner)<=144;}
  public boolean canUse(){owner=getOwner();return isTame()&&eligible()&&distanceToSqr(owner)>9;}
  public boolean canContinueToUse(){return eligible()&&distanceToSqr(owner)>4;}
  public void start(){repath=0;running=true;}
  public void tick(){if(!eligible()){stop();return;}getLookControl().setLookAt(owner,20,20);if(--repath<=0){repath=10;getNavigation().moveTo(owner,.85);}}
  public void stop(){getNavigation().stop();owner=null;running=false;}
 }
}
