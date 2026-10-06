package dev.wildercord.wildlife;

import dev.wildercord.cast.feel.Feels;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import java.util.*;

/** A small amphibian that browses water plants, answers magic and offers a finite nonlethal pearl. */
public final class LanternNewt extends PathfinderMob {
	private static final EntityDataAccessor<Boolean> BROWSING=SynchedEntityData.defineId(LanternNewt.class,EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Long> RESPONSE=SynchedEntityData.defineId(LanternNewt.class,EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Boolean> RESTING=SynchedEntityData.defineId(LanternNewt.class,EntityDataSerializers.BOOLEAN);
 private long pearlReady,browseReady,responseReady,frightenedUntil,refugeReady;
 public float rest,restO;
 @Override public void tick() {super.tick();if(level().isClientSide()) {restO=rest;rest=WildlifeRules.approach(rest,resting()?1:0,.08F);}}
 public boolean resting() {return entityData.get(RESTING);}
 public long refugeReady() {return refugeReady;}
	public LanternNewt(EntityType<? extends LanternNewt> type,Level level) {
		super(type,level);moveControl=new SmoothSwimmingMoveControl<>(this,85,10,.7F,.65F,true);
		lookControl=new net.minecraft.world.entity.ai.control.LookControl(this) {
			private boolean swimmingToRefuge() {return getNavigation() instanceof NewtPathNavigation n && n.followingRefuge();}
			// Move control builds its swimming pitch over several ticks. The ordinary
			// look reset would erase it every tick and let buoyancy win the descent.
			@Override protected boolean resetXRotOnTick() {return !swimmingToRefuge();}
			@Override protected Optional<Float> getXRotD() {return swimmingToRefuge()?Optional.empty():super.getXRotD();}
		};
		setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER,0);xpReward=0;
	}
	public static AttributeSupplier.Builder attributes() {return createMobAttributes().add(Attributes.MAX_HEALTH,10).add(Attributes.MOVEMENT_SPEED,.22).add(Attributes.FOLLOW_RANGE,12).add(Attributes.TEMPT_RANGE,10).add(Attributes.STEP_HEIGHT,.6);}
	@Override protected PathNavigation createNavigation(Level level) {return new NewtPathNavigation(this,level);}
	boolean beneathRefuge(BlockPos roof) {return Math.abs(getX()-(roof.getX()+.5))<.28 && Math.abs(getZ()-(roof.getZ()+.5))<.28 && getY()<roof.getY()+.3 && isInWater();}
	@Override public boolean canBreatheUnderwater() {return true;}
	/** Swimming control supplies scaled movement input; use aquatic travel rather than land-mob fluid drag. */
	@Override public void travel(net.minecraft.world.phys.Vec3 input) {
		if (isEffectiveAi() && isInWater()) {
   // A settled visitor must ignore residual swimming input as well as velocity.
   if(resting()) {setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);return;}
			moveRelative(getSpeed(),input);move(MoverType.SELF,getDeltaMovement());
			setDeltaMovement(getDeltaMovement().scale(.9));
			// Counter the swimming controller's idle buoyancy while settled over a plant.
			if(getNavigation().isDone())setDeltaMovement(getDeltaMovement().add(0,-.005,0));
		} else super.travel(input);
	}

	@Override protected void defineSynchedData(SynchedEntityData.Builder b) {super.defineSynchedData(b);b.define(BROWSING,false);b.define(RESPONSE,0L);b.define(RESTING,false);}
	public boolean browsing() {return entityData.get(BROWSING);}
	public int response() {return (int)Math.clamp(entityData.get(RESPONSE)-level().getGameTime(),0,80);}
	public boolean frightened() {return level().getGameTime()<frightenedUntil;}
	public long pearlReady() {return pearlReady;}
	public long responseReady() {return responseReady;}
	@Override protected void registerGoals() {
		goalSelector.addGoal(1,new PanicGoal(this,1.5));
		goalSelector.addGoal(3,new TemptGoal(this,.8,s -> s.is(Items.SEAGRASS),false));
		goalSelector.addGoal(4,new Shelter());
		goalSelector.addGoal(5,new Browse());
		goalSelector.addGoal(6,new RandomSwimmingGoal(this,.7,80) {@Override public boolean canUse() {return isInWater() && super.canUse();}});
		goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,.55) {@Override public boolean canUse() {return !isInWater() && super.canUse();}});
		goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,6));
		goalSelector.addGoal(8,new RandomLookAroundGoal(this));
	}
	/** A visible response is finite and cannot generate pearls or renew their gathering deadline. */
	public boolean answerMagic() {
		if(!(level() instanceof ServerLevel l) || !isAlive() || !isInWaterOrRain() || l.getGameTime()<responseReady || l.getGameTime()<frightenedUntil)return false;
		responseReady=l.getGameTime()+WetlandRules.RESPONSE_REST;entityData.set(RESPONSE,l.getGameTime()+80);Feels.sound(l,position(),"wetland_newt_answer",.65F,1);return true;
	}
	@Override protected void customServerAiStep(ServerLevel l) {
		super.customServerAiStep(l);
		if(isInWaterOrRain() && (response()>0 || WetlandRules.night(l.getOverworldClockTime())) && tickCount%20==0)
			l.sendParticles(net.minecraft.core.particles.ParticleTypes.GLOW,getX(),getY()+.3,getZ(),2,.16,.05,.16,0);
	}
	@Override public boolean hurtServer(ServerLevel l,DamageSource source,float amount) {boolean hit=super.hurtServer(l,source,amount);if(hit) {frightenedUntil=l.getGameTime()+200;entityData.set(RESPONSE,0L);entityData.set(RESTING,false);}return hit;}
	@Override protected InteractionResult mobInteract(Player p,InteractionHand hand) {
		var stack=p.getItemInHand(hand);if(!stack.is(Items.SEAGRASS))return super.mobInteract(p,hand);
		if(level() instanceof ServerLevel l) {
			if(!isAlive() || l.getGameTime()<frightenedUntil)return InteractionResult.PASS;
			if(!isInWaterOrRain()) {p.sendOverlayMessage(Component.translatable("message.wildercord.lantern_newt.water"));return InteractionResult.SUCCESS;}
			if(!WetlandRules.ready(l.getGameTime(),pearlReady)) {p.sendOverlayMessage(Component.translatable("message.wildercord.lantern_newt.rest"));return InteractionResult.SUCCESS;}
			if(!p.getAbilities().instabuild)stack.shrink(1);
			pearlReady=l.getGameTime()+WetlandRules.PEARL_REST;browseReady=l.getGameTime()+WetlandRules.BROWSE_REST;
			spawnAtLocation(l,new ItemStack(WetlandContent.DUSK_PEARL));entityData.set(RESPONSE,l.getGameTime()+60);Feels.sound(l,position(),"wetland_newt_pearl",.7F,1);
		}
		return InteractionResult.SUCCESS;
	}
	@Override protected SoundEvent getAmbientSound() {return dev.wildercord.content.WildercordSounds.kit("wetland_newt_call");}
	@Override protected SoundEvent getHurtSound(DamageSource s) {return dev.wildercord.content.WildercordSounds.kit("wetland_newt_hurt");}
	@Override protected SoundEvent getDeathSound() {return dev.wildercord.content.WildercordSounds.kit("wetland_newt_death");}
	@Override public int getAmbientSoundInterval() {return 240;}
	@Override protected void addAdditionalSaveData(ValueOutput out) {super.addAdditionalSaveData(out);out.putLong("refuge_ready",refugeReady);out.putLong("pearl_ready",pearlReady);out.putLong("browse_ready",browseReady);out.putLong("response_ready",responseReady);out.putLong("frightened_until",frightenedUntil);}
	@Override protected void readAdditionalSaveData(ValueInput in) {super.readAdditionalSaveData(in);refugeReady=in.getLongOr("refuge_ready",0);pearlReady=in.getLongOr("pearl_ready",0);browseReady=in.getLongOr("browse_ready",0);responseReady=in.getLongOr("response_ready",0);frightenedUntil=in.getLongOr("frightened_until",0);entityData.set(BROWSING,false);entityData.set(RESPONSE,0L);entityData.set(RESTING,false);}
 /** Seeks one waterlogged roof, takes a short rest and leaves its gathering clocks alone. */
 private final class Shelter extends Goal {
  private HabitatSweep sweep;private BlockPos roof;private net.minecraft.world.level.pathfinder.Path route;
  private int scanAt,travelLeft,settled,retries;private boolean arrived;
  Shelter() {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
  private boolean weather() {return isInWaterOrRain() && (!WetlandRules.night(level().getOverworldClockTime()) || level().isRaining()) && !frightened() && response()==0;}
  private boolean available(BlockPos at) {
   if(!level().hasChunkAt(at))return false;var b=level().getBlockState(at);
   return b.is(WetlandShelters.REFUGE) && b.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)
    && WetlandQueries.refugeFree((ServerLevel)level(),new net.minecraft.world.phys.AABB(at).inflate(.25),LanternNewt.this);
  }
  @Override public boolean canUse() {
   if(!weather() || level().getGameTime()<refugeReady) {sweep=null;return false;}
   if(tickCount<scanAt)return false;scanAt=tickCount+20;
   if(sweep==null) {sweep=new HabitatSweep(6);var here=blockPosition();sweep.anchor(here.getX(),here.getY(),here.getZ());}
   int paths=0;
   for(int i=0;i<8;i++) {
    var o=sweep.next();if(o==null) {sweep=null;scanAt=tickCount+200;return false;}
    for(int h=0;h<3;h++) {
     int dy=h==0?0:h==1?1:-1;var at=new BlockPos(sweep.x()+o.x(),sweep.y()+dy,sweep.z()+o.z());
     if(!available(at))continue;var path=getNavigation().createPath(at,0);paths++;
     if(path!=null && path.canReach()) {roof=at;route=path;return true;}
     if(paths==2)return false;
    }
   }
   return false;
  }
  @Override public void start() {travelLeft=240;settled=0;retries=0;arrived=false;sweep=null;getNavigation().moveTo(route,.7);}
  @Override public boolean canContinueToUse() {return roof!=null && travelLeft>0 && settled<WetlandRules.REFUGE_PAUSE && weather() && available(roof);}
  @Override public void tick() {
   travelLeft--;
   boolean near=beneathRefuge(roof);
   entityData.set(RESTING,near);
   if(!near && distanceToSqr(roof.getX()+.5,roof.getY()+.12,roof.getZ()+.5)<1)getMoveControl().setWantedPosition(roof.getX()+.5,roof.getY()+.1,roof.getZ()+.5,.7);
   if(near) {
    getNavigation().stop();setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);entityData.set(RESTING,true);
    if(!arrived) {arrived=true;refugeReady=level().getGameTime()+WetlandRules.REFUGE_REST;Feels.sound((ServerLevel)level(),position(),"wetland_refuge_settle",.3F,1);}
    settled++;
   } else if(getNavigation().isDone() && tickCount%20==0 && retries++<4) {var path=getNavigation().createPath(roof,0);if(path!=null && path.canReach())getNavigation().moveTo(path,.7);}
  }
  @Override public boolean requiresUpdateEveryTick() {return true;}
  @Override public void stop() {if(arrived && isAlive())Feels.sound((ServerLevel)level(),position(),"wetland_refuge_wake",.3F,1);entityData.set(RESTING,false);getNavigation().stop();roof=null;route=null;sweep=null;scanAt=tickCount+80;}
 }

	/** Visits live seagrass without consuming the plant, minting resources or spawning offspring. */
	private final class Browse extends Goal {
		private HabitatSweep sweep;
		private BlockPos food;
		private int searchAt,left,chew,scanAt,retries;
		Browse() {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
		private boolean plant(BlockPos at) {return level().hasChunkAt(at) && (level().getBlockState(at).is(Blocks.SEAGRASS) || level().getBlockState(at).is(Blocks.TALL_SEAGRASS));}
		@Override public boolean canUse() {
			return isInWater() && tickCount>=searchAt && level().getGameTime()>=browseReady && level().getGameTime()>=frightenedUntil;
		}
		@Override public void start() {
			// Hold a stable search origin and MOVE while prospecting, so wandering cannot steal the approach.
			getNavigation().stop();sweep=new HabitatSweep(4);var here=blockPosition();sweep.anchor(here.getX(),here.getY(),here.getZ());
			food=null;left=220;chew=0;scanAt=tickCount;retries=0;
		}
		@Override public boolean canContinueToUse() {
			return left>0 && chew<30 && (food==null || plant(food)) && level().getGameTime()>=browseReady && level().getGameTime()>=frightenedUntil;
		}
		private void seek() {
			if(tickCount<scanAt)return;scanAt=tickCount+20;int paths=0;
			for(int i=0;i<8;i++) {
				var o=sweep.next();if(o==null) {left=0;return;}
				for(int h=0;h<3;h++) {
					int dy=h==0?0:h==1?1:-1;var at=new BlockPos(sweep.x()+o.x(),sweep.y()+dy,sweep.z()+o.z());
					if(!plant(at))continue;var path=getNavigation().createPath(at,0);paths++;
					if(path!=null && path.canReach()) {food=at;left=200;getNavigation().moveTo(path,.7);return;}
					if(paths==2)return;
				}
			}
		}
		@Override public void tick() {
			left--;if(food==null) {seek();return;}
			boolean near=distanceToSqr(food.getX()+.5,food.getY()+.1,food.getZ()+.5)<1 && isInWater();entityData.set(BROWSING,near);
			if(near) {
				getNavigation().stop();setDeltaMovement(getDeltaMovement().scale(.5));
				getLookControl().setLookAt(food.getX()+.5,food.getY()+.1,food.getZ()+.5);
				if(++chew==30)browseReady=level().getGameTime()+WetlandRules.BROWSE_REST;
			} else {
				chew=0;
				if(getNavigation().isDone() && tickCount%20==0 && retries++<4) {
					var path=getNavigation().createPath(food,0);if(path!=null && path.canReach())getNavigation().moveTo(path,.7);
				}
			}
		}
		@Override public boolean requiresUpdateEveryTick() {return true;}
		@Override public void stop() {searchAt=tickCount+(food==null?160:40);food=null;sweep=null;getNavigation().stop();entityData.set(BROWSING,false);}
	}
}
