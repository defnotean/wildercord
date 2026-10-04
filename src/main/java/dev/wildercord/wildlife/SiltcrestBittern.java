package dev.wildercord.wildlife;
import dev.wildercord.cast.Cast;
import dev.wildercord.cast.feel.Feels;
import net.minecraft.core.*;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.fish.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import java.util.*;
/** Slow wetland fish stalker. No breeding, terrain edits or extra drops; saved appetite also gates offered meals. */
public final class SiltcrestBittern extends PathfinderMob {
 public static final int IDLE=0,STALKING=1,COILING=2,STRIKING=3,PREENING=4,SHELTERING=5,RETREATING=6;
 public static final int APPETITE=6000,PREY_CAP=8,JOURNEY=240,COIL=16,PREEN=60;
 private static final EntityDataAccessor<Integer> POSE=SynchedEntityData.defineId(SiltcrestBittern.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(SiltcrestBittern.class,EntityDataSerializers.INT);
 private long huntReady,controlReady,frightenedUntil,shelterReady;private int epoch,left;private AbstractFish quarry;private Vec3 committed;
 private boolean pendingPreen;private BlockPos shelter;private long shelterUntil;private net.minecraft.resources.ResourceKey<Level> knownWorld;
 public float coil,coilO,strike,strikeO,preen,preenO,rest,restO;
 public SiltcrestBittern(EntityType<? extends SiltcrestBittern> t,Level l){super(t,l);xpReward=0;setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER,4);}
 public static AttributeSupplier.Builder attributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,12).add(Attributes.MOVEMENT_SPEED,.23).add(Attributes.FOLLOW_RANGE,8).add(Attributes.STEP_HEIGHT,.6);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(POSE,IDLE);b.define(PHASE,0);}
 @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new Goal(){{setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}public boolean canUse(){return fixed();}public boolean canContinueToUse(){return fixed();}public boolean requiresUpdateEveryTick(){return true;}public void tick(){still();}});goalSelector.addGoal(2,new BitternRetreatGoal(this));goalSelector.addGoal(3,new BitternShelterGoal(this));goalSelector.addGoal(4,new BitternHuntGoal(this));goalSelector.addGoal(5,new BitternBankReturnGoal(this));goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,.5));goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,5));}
 static long clock(Level l){return l instanceof ServerLevel s?s.getServer().overworld().getGameTime():l.getGameTime();}
 public int pose(){return entityData.get(POSE);}public int phase(){return entityData.get(PHASE);}public long huntReady(){return huntReady;}public long controlReady(){return controlReady;}public BlockPos shelter(){return shelter;}public long shelterUntil(){return shelterUntil;}
 boolean fixed(){return pose()==COILING||pose()==STRIKING||pose()==PREENING||pose()==SHELTERING;}
 boolean wantsShelter(){return (level().isRaining()||!WetlandRules.night(level().getOverworldClockTime()))&&clock(level())>=shelterReady;}
 boolean forageReady(){return isAlive()&&!isRemoved()&&pose()==IDLE&&clock(level())>=huntReady&&clock(level())>=frightenedUntil&&WetlandRules.night(level().getOverworldClockTime())&&!level().isRaining();}
 boolean hungry(){return forageReady()&&onGround()&&BitternHabitat.standingBank(this);}
 void stalking(boolean yes){if(yes&&pose()==IDLE)pose(STALKING,0);else if(!yes&&pose()==STALKING)pose(IDLE,0);}
 private void pose(int n,int ticks){entityData.set(POSE,n);entityData.set(PHASE,0);left=ticks;if(fixed())still();}
 private void still(){getNavigation().stop();setSpeed(0);setXxa(0);setDeltaMovement(0,getDeltaMovement().y,0);}
 private static boolean unownedFish(AbstractFish f){return (f instanceof Cod||f instanceof Salmon)&&!f.fromBucket()&&!f.hasCustomName()&&!f.isPersistenceRequired();}
 static boolean wildFish(AbstractFish f,ServerLevel l){return unownedFish(f)&&f.level()==l&&f.isAlive()&&!f.isRemoved()&&f.isInWater()&&l.hasChunkAt(f.blockPosition());}
 /** Complete raw pool first; a ninth fish refuses rather than biasing a truncated eligible prefix. */
 List<AbstractFish> preyPool(ServerLevel l){var all=new ArrayList<AbstractFish>(PREY_CAP+1);l.getEntities(EntityTypeTest.<Entity,AbstractFish>forClass(AbstractFish.class),getBoundingBox().inflate(6),f->true,all,PREY_CAP+1);if(all.size()>PREY_CAP)return List.of();all.removeIf(f->!wildFish(f,l));all.sort(Comparator.<AbstractFish>comparingDouble(this::distanceToSqr).thenComparing(f->f.getUUID().toString()));return List.copyOf(all);}
 boolean disturbed(ServerLevel l){var players=new ArrayList<Player>(13);l.getEntities(EntityTypeTest.<Entity,Player>forClass(Player.class),getBoundingBox().inflate(4),p->true,players,13);if(players.size()>12)return true;return players.stream().anyMatch(p->p.isAlive()&&!p.isRemoved()&&!p.isCreative()&&!p.isSpectator()&&!p.isShiftKeyDown()&&distanceToSqr(p)<16&&hasLineOfSight(p));}
 boolean beginCoil(ServerLevel l,AbstractFish f){if(level()!=l||pose()!=STALKING||clock(l)<huntReady||!onGround()||disturbed(l)||!wildFish(f,l)||distanceToSqr(f)>3.24||!loadedSight(f)||preyPool(l).size()<3)return false;quarry=f;committed=f.getBoundingBox().getCenter();epoch++;pose(COILING,COIL);getLookControl().setLookAt(f,30,30);return true;}
 boolean loadedSight(Entity e){var a=blockPosition();var b=e.blockPosition();for(int x=Math.min(a.getX(),b.getX())>>4;x<=Math.max(a.getX(),b.getX())>>4;x++)for(int z=Math.min(a.getZ(),b.getZ())>>4;z<=Math.max(a.getZ(),b.getZ())>>4;z++)if(!level().hasChunkAt(new BlockPos(x<<4,a.getY(),z<<4)))return false;return hasLineOfSight(e);}
 private boolean action(ServerLevel l,int token,AbstractFish f){return level()==l&&isAlive()&&!isRemoved()&&epoch==token&&pose()==STRIKING&&quarry==f;}
 /** One committed real hurt; admitted death and current source own appetite/preen, never an arbitrary nearby drop. */
 private void strike(ServerLevel l){var f=quarry;int token=epoch;huntReady=Math.max(huntReady,clock(l)+200);
  if(f==null||!wildFish(f,l)||!onGround()||committed==null||f.getBoundingBox().getCenter().distanceToSqr(committed)>.36||distanceToSqr(f)>3.24||!loadedSight(f)||disturbed(l)||preyPool(l).size()<3){cancel(40);return;}
  var original=position();float prior=f.getHealth();var strikeDamage=damageSources().mobAttack(this);boolean hit=f.hurtServer(l,strikeDamage,4);
  if(!action(l,token,f))return;
  if(!hit||f.getLastDamageSource()!=strikeDamage||!position().equals(original)||f.level()!=l||f.isRemoved()||!unownedFish(f)||!l.hasChunkAt(f.blockPosition())||distanceToSqr(f)>3.24||f.getBoundingBox().getCenter().distanceToSqr(committed)>.36||f.getHealth()>=prior||f.isAlive()||f.getHealth()>0){cancel(40);return;}
  huntReady=clock(l)+APPETITE;quarry=null;committed=null;pendingPreen=true;Feels.sound(l,position(),"bittern_catch",.4F,1);
  l.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH,getX(),getY()+.15,getZ(),3,.13,.02,.13,.01);
 }
 private void cancel(int pause){epoch++;pendingPreen=false;quarry=null;committed=null;getNavigation().stop();huntReady=Math.max(huntReady,clock(level())+pause);if(pose()!=RETREATING)pose(IDLE,0);}
 void retreat(){cancel(200);frightenedUntil=Math.max(frightenedUntil,clock(level())+200);pose(RETREATING,40);Feels.sound((ServerLevel)level(),position(),"bittern_rustle",.35F,1);}
 boolean settle(ServerLevel l,BlockPos at){if(l!=level()||!isAlive()||isRemoved()||pose()!=IDLE||!onGround()||!wantsShelter()||!BitternHabitat.shelter(l,at)||distanceToSqr(at.getX()+.5,at.getY(),at.getZ()+.5)>.36)return false;shelter=at.immutable();shelterUntil=clock(l)+160;shelterReady=clock(l)+1200;epoch++;pose(SHELTERING,160);return true;}
 /** Called only after a real Tidebreath owner changes this actual recipient, not a generic hit appearance. */
 public boolean answerWater(Cast cast){if(!(level() instanceof ServerLevel l)||cast.level!=l||!cast.alive()||cast.passive||!(cast.caster instanceof ServerPlayer p)||p.isCreative()||p.isSpectator()||!isAlive()||isRemoved()||clock(l)<controlReady||distanceToSqr(p)>64||!loadedSight(p)||pose()==SHELTERING||!cast.once("bittern_water_response"))return false;cancel(200);controlReady=clock(l)+200;pose(PREENING,40);Feels.sound(l,position(),"bittern_rustle",.3F,1);return true;}
 @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){var stack=player.getItemInHand(hand);if(!stack.is(net.minecraft.world.item.Items.COD)&&!stack.is(net.minecraft.world.item.Items.SALMON))return super.mobInteract(player,hand);if(level().isClientSide())return InteractionResult.SUCCESS;if(!(player instanceof ServerPlayer p)||p.isCreative()||p.isSpectator()||!p.isAlive()||p.isRemoved()||p.level()!=level()||!isAlive()||isRemoved()||!p.isShiftKeyDown()||distanceToSqr(p)>12.25||!loadedSight(p)||!onGround()||clock(level())<huntReady||clock(level())<frightenedUntil||fixed())return InteractionResult.PASS;
  stack.shrink(1);cancel(0);huntReady=clock(level())+APPETITE;pose(PREENING,PREEN);Feels.sound((ServerLevel)level(),position(),"bittern_rustle",.35F,1);return InteractionResult.SUCCESS;}
 @Override public boolean hurtServer(ServerLevel l,DamageSource source,float amount){float before=getHealth();boolean hit=super.hurtServer(l,source,amount);if(hit&&getHealth()<before&&isAlive()&&!isRemoved()&&level()==l)retreat();return hit;}
 @Override public void tick(){super.tick();if(level().isClientSide()){coilO=coil;strikeO=strike;preenO=preen;restO=rest;coil=WildlifeRules.approach(coil,pose()==COILING?1:0,.12F);strike=WildlifeRules.approach(strike,pose()==STRIKING?1:0,.65F);preen=WildlifeRules.approach(preen,pose()==PREENING?1:0,.1F);rest=WildlifeRules.approach(rest,pose()==SHELTERING?1:0,.1F);}}
 @Override protected void customServerAiStep(ServerLevel l){super.customServerAiStep(l);if(knownWorld==null)knownWorld=l.dimension();else if(!knownWorld.equals(l.dimension())){knownWorld=l.dimension();epoch++;quarry=null;committed=null;pendingPreen=false;shelter=null;shelterUntil=0;pose(IDLE,0);}
  if(pose()==COILING){if(quarry==null||!wildFish(quarry,l)||disturbed(l)||!onGround()||!WetlandRules.night(l.getOverworldClockTime())||l.isRaining()){cancel(200);return;}getLookControl().setLookAt(committed.x,committed.y,committed.z,30,30);entityData.set(PHASE,COIL-left);if(--left<=0){pose(STRIKING,6);strike(l);}return;}
  if(pose()==STRIKING||pose()==PREENING){entityData.set(PHASE,(pose()==PREENING?PREEN:6)-left);if(--left<=0){if(pose()==STRIKING&&pendingPreen){pendingPreen=false;pose(PREENING,PREEN);}else pose(IDLE,0);}return;}
  if(pose()==SHELTERING){if(shelter==null||!BitternHabitat.shelter(l,shelter)||!onGround()||distanceToSqr(shelter.getX()+.5,shelter.getY(),shelter.getZ()+.5)>.64||clock(l)>=shelterUntil){shelter=null;shelterUntil=0;pose(IDLE,0);}return;}
  if(pose()==RETREATING){if(--left<=0)pose(IDLE,0);return;}
 }
 @Override protected SoundEvent getAmbientSound(){return dev.wildercord.content.WildercordSounds.kit("bittern_boom");}
 @Override protected SoundEvent getHurtSound(DamageSource s){return dev.wildercord.content.WildercordSounds.kit("bittern_rustle");}
 @Override public int getAmbientSoundInterval(){return 480;}
 @Override protected float getSoundVolume(){return .4F;}
 @Override protected void addAdditionalSaveData(ValueOutput o){super.addAdditionalSaveData(o);o.putLong("bittern_hunt_ready",huntReady);o.putLong("bittern_control_ready",controlReady);o.putLong("bittern_frightened_until",frightenedUntil);o.putLong("bittern_shelter_ready",shelterReady);o.putLong("bittern_shelter_until",shelterUntil);o.putString("bittern_shelter_world",level().dimension().identifier().toString());if(shelter!=null)o.store("bittern_shelter",BlockPos.CODEC,shelter);}
 @Override protected void readAdditionalSaveData(ValueInput i){super.readAdditionalSaveData(i);huntReady=i.getLongOr("bittern_hunt_ready",0);controlReady=i.getLongOr("bittern_control_ready",0);frightenedUntil=i.getLongOr("bittern_frightened_until",0);shelterReady=i.getLongOr("bittern_shelter_ready",0);shelterUntil=i.getLongOr("bittern_shelter_until",0);shelter=i.read("bittern_shelter",BlockPos.CODEC).orElse(null);if(!i.getStringOr("bittern_shelter_world",level().dimension().identifier().toString()).equals(level().dimension().identifier().toString())){shelter=null;shelterUntil=0;}knownWorld=level().dimension();quarry=null;committed=null;pendingPreen=false;epoch++;pose(shelter!=null&&clock(level())<shelterUntil?SHELTERING:IDLE,0);}
}
