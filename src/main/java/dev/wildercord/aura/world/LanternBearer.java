package dev.wildercord.aura.world;

import dev.wildercord.cast.MoveDeck;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.world.dungeons.BarrowFieldPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import java.util.*;

/**
 * The Lantern-Bearer ({@link LanternBearerRules}). Generation binds it to the open grave under the Barrow Field's gibbet, to the field
 * round it, to the soul lantern hanging over the grave and to the candles on the graves. By day it is not there at all. After dark, the
 * first player in among the graves wakes it: it rises up out of the grave, reaches its crook up and takes the lantern down off the
 * gibbet, its eyes catch, the candles kindle, and it wails; then the boss bar comes up. The field going quiet, or the dawn, sends it
 * back down into the grave, healed, and the lantern back up on its chain. The field is its own: anything hostile in it, it burns away.
 * <p>
 * Up, it picks its next move from what suits where its prey stands, never the one it just made while another will do, and each comes
 * with its own tell. It drags its spade low and reaps at ankle height through everyone in front of it; it thrusts its crook out along
 * a line marked on the ground and drags in whoever it catches, or bites the earth and sticks; it whirls its lantern and looses wisps
 * that follow only whom they can see; it drives its spade in and marks a grave under each of them that bursts; it draws the lantern
 * under its hood and brings it down in a flare all round it; it levers up grave earth and flings it in a fan round where they stand;
 * it sweeps the crook round behind it at whoever is at its back; and it breathes soul fire across its lantern in a slow turning cone.
 * Some blows it goes to smoke and drifts out from under, and hurt too fast it drifts away across the field. Half beaten it holds its
 * rite, once, and fights on roused: quicker, its moves running on in pairs and most of them grown worse, and now and then it puts its
 * lantern out ({@link LanternBearerRules#LIGHTS_OUT}) and comes for them out of the dark.
 */
public final class LanternBearer extends PathfinderMob implements Enemy {
	private static final EntityDataAccessor<Integer> MOVE=SynchedEntityData.defineId(LanternBearer.class,EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN=SynchedEntityData.defineId(LanternBearer.class,EntityDataSerializers.LONG);
	/** The current move's tell in ticks, so the client knows when the blow lands. */
	private static final EntityDataAccessor<Integer> TELL=SynchedEntityData.defineId(LanternBearer.class,EntityDataSerializers.INT);
	/** Which way a fade drifts or the breath turns, for the client: -1 left, 1 right, 0 straight back. */
	private static final EntityDataAccessor<Integer> SIDE=SynchedEntityData.defineId(LanternBearer.class,EntityDataSerializers.INT);
	/** Whether it has held its rite, so the client can show it roused. */
	private static final EntityDataAccessor<Boolean> ROUSED=SynchedEntityData.defineId(LanternBearer.class,EntityDataSerializers.BOOLEAN);
	private static final EntityDimensions LYING=EntityDimensions.scalable(1.4F,0.4F).withEyeHeight(0.2F),STANDING=EntityDimensions.scalable(1.4F,3.4F).withEyeHeight(3.0F);
	private static final DustParticleOptions SOUL_DUST=new DustParticleOptions(0x4FE0E8,1.4F),SOUL_SMALL=new DustParticleOptions(0xA0F0F4,.8F),
		EARTH=new DustParticleOptions(0x4A3828,1.6F);
	private final ServerBossEvent bar=new ServerBossEvent(getUUID(),getType().getDescription(),BossEvent.BossBarColor.BLUE,BossEvent.BossBarOverlay.NOTCHED_10);
	private boolean bound;
	private BlockPos grave=BlockPos.ZERO,hallMin=BlockPos.ZERO,hallMax=BlockPos.ZERO,hook=BlockPos.ZERO;
	private List<BlockPos> candles=new ArrayList<>();
	private float rest;
	private int left,quiet,light;
	private long lastCut;
	/** The game time it last ran, so it knows how long the field was left alone. */
	private long lastTick=-1;
	/** The line its blow was locked on, and where a fade carries it. */
	private float aim;
	private Vec3 slide=Vec3.ZERO;
	private long lastDodge=-999,lastLeap=-999;
	/** Its moves, picked so it never makes the same one twice running, and when it last made each. */
	private final MoveDeck deck=new MoveDeck(4);
	private final long[] lastMade=new long[32];{Arrays.fill(lastMade,-9999);}
	/** How many moves it has run straight on into this time, and whether a move's second part has gone. */
	private int links,again;
	/** Who this swing has struck already, so each is struck once. */
	private final Set<UUID> struck=new HashSet<>();
	/** Which way its prey has been going lately, for laying graves ahead of them. */
	private Vec3 preyVel=Vec3.ZERO,preyLast;
	/** Lights Out: whom it last stood up behind, so each round it comes for the next of them. */
	private int lightsTurn;
	/** Who the crook caught this time, being dragged in. */
	private final Set<UUID> hooked=new HashSet<>();
	/** Soul wisps loosed from its lantern, drifting after their prey; not saved, they burn out soon enough. */
	private final List<Wisp> wisps=new ArrayList<>();
	/** Graves marked, closing until they burst; those dug together share whom they have caught, so each is caught once. */
	private record Mark(Vec3 at,long born,Set<UUID> caught){}
	private final List<Mark> marks=new ArrayList<>();
	/** A ring of soul fire rolling out along the ground. */
	private static final class Wave{final double x,y,z,speed,max,damage;final long born;final Set<UUID> struck=new HashSet<>();
		Wave(Vec3 at,double speed,double max,double damage,long born){x=at.x;y=at.y;z=at.z;this.speed=speed;this.max=max;this.damage=damage;this.born=born;}}
	private final List<Wave> waves=new ArrayList<>();
	/** A clod of grave earth flung off the spade, arcing from {@code from} down onto {@code to}; a fan shares whom it has caught. */
	private record Clod(Vec3 from,Vec3 to,long born,double peak,Set<UUID> caught){}
	private final List<Clod> clods=new ArrayList<>();
	/** Hurt taken lately, fading; enough of it sends it drifting away. */
	private double stung;

	private static final class Wisp{Vec3 at,going;UUID prey;int age,unseen;Wisp(Vec3 at,Vec3 going,UUID prey){this.at=at;this.going=going;this.prey=prey;}}

	public LanternBearer(EntityType<? extends LanternBearer> type,Level level){super(type,level);xpReward=LanternBearerRules.TIER.xp;setPersistenceRequired();}
	public static AttributeSupplier.Builder createAttributes(){
		return createMobAttributes().add(Attributes.MAX_HEALTH,LanternBearerRules.HEALTH).add(Attributes.ARMOR,LanternBearerRules.ARMOUR).add(Attributes.ARMOR_TOUGHNESS,LanternBearerRules.TIER.toughness)
			.add(Attributes.MOVEMENT_SPEED,LanternBearerRules.SPEED).add(Attributes.ATTACK_DAMAGE,LanternBearerRules.TIER.melee).add(Attributes.FOLLOW_RANGE,40).add(Attributes.KNOCKBACK_RESISTANCE,1)
			.add(Attributes.STEP_HEIGHT,1.1);
	}

	/**
	 * Bind it to its grave: {@code at} the air in the grave where it lies, {@code yaw} the way it faces, {@code field} the field it keeps,
	 * {@code lantern} where the soul lantern hangs over the grave, {@code lights} the candles on the graves.
	 */
	public void bind(BlockPos at,float yaw,BoundingBox field,BlockPos lantern,List<BlockPos> lights){
		bound=true;grave=at.immutable();rest=yaw;hook=lantern.immutable();candles=new ArrayList<>(lights);
		hallMin=new BlockPos(field.minX(),field.minY(),field.minZ());hallMax=new BlockPos(field.maxX(),field.maxY(),field.maxZ());
		lieDown();
	}
	/** Client side, for blending one move into the next: the move last drawn, the one before it, when it changed, and how far into it the last one was. */
	public int shownMove=-1,shownTell,prevMove,prevTell;public float shownElapsed,prevElapsed,switchedAt=-100;
	public int move(){return entityData.get(MOVE);}
	public int tell(){return entityData.get(TELL);}
	public int side(){return entityData.get(SIDE);}
	public boolean roused(){return entityData.get(ROUSED);}
	public float elapsed(float partial){return (float)(level().getGameTime()-entityData.get(BEGAN))+partial;}
	private long since(){return level().getGameTime()-entityData.get(BEGAN);}
	private Vec3 spot(){return Vec3.atBottomCenterOf(grave);}

	@Override protected void defineSynchedData(SynchedEntityData.Builder b){
		super.defineSynchedData(b);b.define(MOVE,LanternBearerRules.DORMANT);b.define(BEGAN,0L);b.define(TELL,0);b.define(SIDE,0);b.define(ROUSED,false);
	}
	@Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(MOVE.equals(key))refreshDimensions();}
	@Override protected EntityDimensions getDefaultDimensions(Pose pose){return move()==LanternBearerRules.DORMANT?LYING:STANDING;}
	@Override protected void registerGoals(){}
	@Override public boolean isPushable(){return false;}
	@Override protected void doPush(Entity entity){}
	@Override public boolean removeWhenFarAway(double distance){return false;}
	@Override public boolean canFreeze(){return false;}
	/** Down in the grave there is nothing there to strike or shoot. */
	@Override public boolean isPickable(){return move()!=LanternBearerRules.DORMANT && !unseen() && super.isPickable();}
	@Override public boolean canBeHitByProjectile(){return move()!=LanternBearerRules.DORMANT && move()!=LanternBearerRules.FADE && !unseen() && super.canBeHitByProjectile();}
	/** Whether it is out in the dark, its lantern out, and not there to be seen or struck. */
	public boolean unseen(){return move()==LanternBearerRules.LIGHTS_OUT && !LanternBearerRules.lightsSeen(since());}
	@Override public boolean causeFallDamage(double distance,float multiplier,DamageSource source){return false;}

	private void pose(int move,int ticks){pose(move,ticks,0);}
	private void pose(int move,int ticks,int tell){
		int was=move();entityData.set(MOVE,move);entityData.set(BEGAN,level().getGameTime());entityData.set(TELL,tell);left=ticks;getNavigation().stop();
		if(was!=move)refreshDimensions();
		if(!LanternBearerRules.scripted(move))bar.setName(Component.translatable("boss.wildercord.lantern_bearer.status",getType().getDescription(),
			Component.translatable("boss.wildercord.lantern_bearer.move."+LanternBearerRules.key(move))));
	}
	/** Down in its grave again, whole and calm, the lantern back on its chain and the candles out. */
	private void goDown(){
		lieDown();
		if(level() instanceof ServerLevel server){lantern(server,false);candles(server,false);}
	}
	/**
	 * Whole and calm in its grave, touching nothing about it: binding runs while the barrow is still being generated, off the server
	 * thread, where reaching into the level for the lantern or the candles would wait on the server for a chunk and hang it.
	 */
	private void lieDown(){
		pose(LanternBearerRules.DORMANT,0);hold();setHealth(getMaxHealth());setTarget(null);bar.removeAllPlayers();quiet=0;light=0;
		entityData.set(ROUSED,false);bar.setColor(BossEvent.BossBarColor.BLUE);forget();
	}
	/** Forgets the fight: what it has made lately, and everything it has loosed. */
	private void forget(){
		deck.clear();Arrays.fill(lastMade,-9999);links=0;again=0;struck.clear();hooked.clear();
		wisps.clear();marks.clear();waves.clear();clods.clear();preyLast=null;preyVel=Vec3.ZERO;
	}
	/** Pinned to its grave while it lies there (or rises, or sinks). */
	private void hold(){
		var t=spot();setPos(t.x,t.y,t.z);setDeltaMovement(Vec3.ZERO);face(rest);
	}
	private boolean inHall(Vec3 at,double margin){
		return LanternBearerRules.within(at.x,at.y,at.z,hallMin.getX(),hallMin.getY(),hallMin.getZ(),hallMax.getX(),hallMax.getY(),hallMax.getZ(),margin);
	}

	/**
	 * The lantern: taken down off the gibbet ({@code held}), or hung back up on its chain. Only a soul lantern is taken, and only into
	 * empty air is one hung, so a player who has meddled with the gibbet keeps what they did.
	 */
	private void lantern(ServerLevel level,boolean held){
		if(!level.isLoaded(hook))return;
		var s=level.getBlockState(hook);
		if(held && s.is(Blocks.SOUL_LANTERN)){
			level.setBlock(hook,Blocks.AIR.defaultBlockState(),3);
			var c=Vec3.atCenterOf(hook);level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,c.x,c.y,c.z,8,.15,.15,.15,.02);
			Feels.sound(level,c,"aura_lantern_bearer_chain",1.2F,1);
		}else if(!held && s.isAir()){
			level.setBlock(hook,BarrowFieldPiece.lantern(),3);
			var c=Vec3.atCenterOf(hook);level.sendParticles(ParticleTypes.SOUL,c.x,c.y,c.z,6,.15,.15,.15,.02);
		}
	}
	/** The candles on the graves, kindled when it rises and snuffed when it goes down. */
	private void candles(ServerLevel level,boolean lit){
		for(var at:candles){
			if(!level.isLoaded(at))continue;
			var s=level.getBlockState(at);
			if(!s.hasProperty(CandleBlock.LIT) || s.getValue(CandleBlock.LIT)==lit)continue;
			level.setBlock(at,s.setValue(CandleBlock.LIT,lit),3);
			var c=Vec3.atCenterOf(at);
			level.sendParticles(lit?ParticleTypes.SOUL_FIRE_FLAME:ParticleTypes.SMOKE,c.x,c.y,c.z,3,.1,.1,.1,.01);
		}
	}
	@Override protected void customServerAiStep(ServerLevel level){
		super.customServerAiStep(level);if(!isAlive())return;
		if(!bound){var b=blockPosition();bind(b,getYRot(),new BoundingBox(b.getX()-16,b.getY()-2,b.getZ()-16,b.getX()+16,b.getY()+14,b.getZ()+16),b.above(5),List.of());}
		// Left alone long enough (its prey died and went home, say) and it has gone back down in the meantime.
		long now=level.getGameTime();
		if(lastTick>=0 && move()!=LanternBearerRules.DORMANT && LanternBearerRules.forgets(now-lastTick))goDown();
		lastTick=now;
		if(tickCount%LanternBearerRules.GUARD_EVERY==0 && Guardians.enabled())guard(level);
		switch(move()){
			case LanternBearerRules.DORMANT->dormant(level);
			case LanternBearerRules.RISE->{
				hold();int el=LanternBearerRules.RISE_TICKS-left;
				if(LanternBearerRules.holdsLantern(LanternBearerRules.rise(LanternBearerRules.RISE,el)))lantern(level,true);
				if(el==LanternBearerRules.ROAR_AT)wail(level);
				if(--left<=0)stand(level);
			}
			case LanternBearerRules.BED->{
				hold();
				if(wakeful(level)!=null){wake(level,LanternBearerRules.RISE_TICKS-left);break;}
				int el=LanternBearerRules.RISE_TICKS-left;
				if(!LanternBearerRules.holdsLantern(LanternBearerRules.rise(LanternBearerRules.BED,el)))lantern(level,false);
				if(--left<=0)goDown();
			}
			default->awake(level);
		}
		drift(level);marks(level);waves(level);clods(level);
		if(tickCount%4==0)smoke(level);
	}

	/** Grave-damp smoke rising off its rags, and its lantern guttering; roused, souls trailing off it; down in the grave, a soul now and then drifting up out of it. */
	private void smoke(ServerLevel level){
		int move=move();
		if(move==LanternBearerRules.DORMANT){if(random.nextInt(12)==0)level.sendParticles(ParticleTypes.SOUL,getX(),getY()+.2,getZ(),1,.4,.1,.6,.01);return;}
		if(random.nextInt(2)==0)level.sendParticles(ParticleTypes.SMOKE,getX(),getY()+.3,getZ(),1,.5,.1,.5,.005);
		if(roused() && !unseen() && random.nextInt(2)==0)level.sendParticles(ParticleTypes.SOUL,getX(),getY()+1.6,getZ(),1,.4,.8,.4,.01);
		if(LanternBearerRules.holdsLantern(LanternBearerRules.rise(move,since())) && !unseen() && random.nextInt(2)==0){var l=lamp();level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,l.x,l.y,l.z,1,.05,.05,.05,.005);}
	}
	/** Its wail: the sound, the candles kindling, its lantern flaring, and a pulse of darkness over everyone on the field. */
	private void wail(ServerLevel level){
		var h=head();
		Feels.sound(level,h,"aura_lantern_bearer_roar",1.8F,1);
		candles(level,true);
		var l=lamp();level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,l.x,l.y,l.z,24,.3,.3,.3,.06);
		level.sendParticles(ParticleTypes.SOUL,getX(),getY()+1.5,getZ(),30,2.5,1,2.5,.03);
		for(var p:prey(level))p.addEffect(new MobEffectInstance(MobEffects.DARKNESS,60,0,false,false));
	}
	/** Its head, more or less: up under its hood standing, down in the grave lying. */
	private Vec3 head(){
		if(move()==LanternBearerRules.DORMANT)return position().add(0,.3,0);
		return position().add(ahead(getYRot()).scale(.3)).add(0,3.0,0);
	}
	/** Its lantern, more or less: hanging off the crook in its left hand, out in front of it. */
	private Vec3 lamp(){
		var f=ahead(getYRot());var r=new Vec3(-f.z,0,f.x);
		return position().add(f.scale(1.1)).add(r.scale(-.9)).add(0,1.7,0);
	}
	/** The blade of its spade, more or less: down at its right side. */
	private Vec3 blade(){
		var f=ahead(getYRot());var r=new Vec3(-f.z,0,f.x);
		return position().add(f.scale(.6)).add(r.scale(1.1)).add(0,.3,0);
	}
	private void sound(ServerLevel level,Vec3 at,SoundEvent s,float volume,float pitch){level.playSound(null,at.x,at.y,at.z,s,SoundSource.HOSTILE,volume,pitch);}
	private void ring(ServerLevel level,Vec3 at,double r,ParticleOptions dust,int n){
		for(int i=0;i<n;i++){double a=i*Math.PI*2/n;level.sendParticles(dust,at.x+Math.cos(a)*r,at.y+.15,at.z+Math.sin(a)*r,1,0,0,0,0);}
	}
	private BlockState ground(ServerLevel level,Vec3 at){
		var s=level.getBlockState(BlockPos.containing(at.x,at.y-.5,at.z));return s.isAir()?Blocks.DIRT.defaultBlockState():s;
	}
	/** The ground under {@code at}, or {@code at} itself when there is none near. */
	private Vec3 floor(ServerLevel level,Vec3 at){
		var hit=level.clip(new ClipContext(at.add(0,3,0),at.add(0,-6,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
		return hit.getType()==HitResult.Type.MISS?at:hit.getLocation();
	}

	/**
	 * The field is its own: anything hostile that wanders or spawns in, down in the grave or up, it burns away with soul fire from its
	 * lantern, one every {@link LanternBearerRules#GUARD_EVERY} ticks, nearest first. Only players get a fight.
	 */
	private void guard(ServerLevel level){
		var box=new AABB(Vec3.atLowerCornerOf(hallMin),Vec3.atLowerCornerOf(hallMax).add(1,1,1));
		var from=LanternBearerRules.holdsLantern(LanternBearerRules.rise(move(),since()))?lamp():Vec3.atCenterOf(hook);
		var m=level.getEntitiesOfClass(Mob.class,box,e->e instanceof Enemy && e!=this && e.isAlive() && !(e instanceof LanternBearer) && inHall(e.position(),0))
			.stream().min(Comparator.comparingDouble(e->e.distanceToSqr(from))).orElse(null);
		if(m==null)return;
		var to=m.position().add(0,m.getBbHeight()/2,0);
		var step=to.subtract(from);int n=Math.max(2,(int)(step.length()*2));
		for(int i=0;i<=n;i++){var at=from.add(step.scale(i/(double)n));level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,at.x,at.y,at.z,1,.03,.03,.03,0);}
		level.sendParticles(ParticleTypes.SOUL,to.x,to.y,to.z,10,.3,.4,.3,.04);
		Feels.sound(level,to,"aura_lantern_bearer_burst",.8F,1.2F+random.nextFloat()*.2F);
		m.hurtServer(level,damageSources().magic(),Float.MAX_VALUE);
		if(m.isAlive())m.kill(level);
	}

	/** The nearest player in the field close enough to its grave to wake it, if it is dark out. */
	private ServerPlayer wakeful(ServerLevel level){
		var t=spot();boolean dark=level.isDarkOutside();
		return level.players().stream().filter(p->p.isAlive() && !p.isCreative() && !p.isSpectator() && inHall(p.position(),0)
			&& LanternBearerRules.wakes(p.getX()-t.x,p.getY()-t.y,p.getZ()-t.z,dark)).min(Comparator.comparingDouble(p->p.distanceToSqr(t))).orElse(null);
	}

	private void dormant(ServerLevel level){
		hold();if(getHealth()<getMaxHealth())setHealth(getMaxHealth());
		if(tickCount%20==0){lantern(level,false);candles(level,false);}
		if(tickCount%5==0 && Guardians.enabled() && wakeful(level)!=null)wake(level,0);
	}

	/** Begin rising, {@code already} ticks in (when it is woken half way back down). */
	private void wake(ServerLevel level,int already){
		pose(LanternBearerRules.RISE,LanternBearerRules.RISE_TICKS-already);entityData.set(BEGAN,level.getGameTime()-already);
		Feels.sound(level,position().add(0,1,0),"aura_lantern_bearer_rise",1.5F,1);
		level.sendParticles(ParticleTypes.SOUL,getX(),getY()+.3,getZ(),24,.6,.2,.9,.04);
		level.sendParticles(ParticleTypes.LARGE_SMOKE,getX(),getY()+.3,getZ(),12,.5,.2,.8,.02);
	}

	/** Up, the lantern on its crook, and the fight is on. */
	private void stand(ServerLevel level){
		pose(LanternBearerRules.WATCH,LanternBearerRules.GAP);hold();lantern(level,true);forget();
		lastCut=level.getGameTime();quiet=0;light=0;
		level.sendParticles(ParticleTypes.SOUL,getX(),getY()+.2,getZ(),16,1,.1,1,.03);
	}
	/** Back down into the grave, for the field going quiet or the dawn. */
	private void sink(ServerLevel level){
		bar.removeAllPlayers();forget();pose(LanternBearerRules.BED,LanternBearerRules.RISE_TICKS);hold();
		Feels.sound(level,head(),"aura_lantern_bearer_rise",1.2F,.8F);candles(level,false);
	}
	private void awake(ServerLevel level){
		if(!inHall(position(),LanternBearerRules.LEASH)){
			var at=spot();teleportTo(at.x,at.y,at.z);setDeltaMovement(Vec3.ZERO);getNavigation().stop();links=0;pose(LanternBearerRules.WATCH,LanternBearerRules.GAP);
		}
		long now=level.getGameTime();float heal=LanternBearerRules.regen(getMaxHealth(),now-lastCut);if(heal>0 && getHealth()<getMaxHealth())heal(heal);
		light=level.isBrightOutside()?light+1:0;
		if(LanternBearerRules.dawn(light)){sink(level);return;}
		if(tickCount%10==0){
			bar.setProgress(getHealth()/getMaxHealth());
			for(var p:new ArrayList<>(bar.getPlayers()))if(p.level()!=level || !p.isAlive() || !inHall(p.position(),LanternBearerRules.BAR_DROP))bar.removePlayer(p);
			for(var p:level.players())if(p.isAlive() && !p.isSpectator() && inHall(p.position(),LanternBearerRules.BAR_SHOW))bar.addPlayer(p);
			setTarget(level.players().stream().filter(p->p.isAlive() && !p.isCreative() && !p.isSpectator() && inHall(p.position(),0))
				.min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null));
		}
		stung*=LanternBearerRules.STING_FADE;
		var target=getTarget();
		// Which way its prey has been going lately, for laying graves ahead of them.
		if(target!=null){var at=target.position();
			if(preyLast!=null){var v=preyVel.scale(.7).add(at.subtract(preyLast).multiply(1,0,1).scale(.3));preyVel=v.length()>1?v.normalize():v;}
			preyLast=at;}else{preyLast=null;preyVel=Vec3.ZERO;}
		switch(move()){
			case LanternBearerRules.REAP->reap(level,target);
			case LanternBearerRules.HOOK->hook(level,target);
			case LanternBearerRules.SNAGGED->snagged(level);
			case LanternBearerRules.WISPS->loose(level,target);
			case LanternBearerRules.GRAVES->dig(level,target);
			case LanternBearerRules.FLARE->flare(level,target);
			case LanternBearerRules.SHOVEL->shovel(level,target);
			case LanternBearerRules.BACKSWEEP->backsweep(level);
			case LanternBearerRules.BREATH->breathe(level,target);
			case LanternBearerRules.RITE->rite(level);
			case LanternBearerRules.LIGHTS_OUT->lights(level,target);
			case LanternBearerRules.FADE->{
				face(aim);move(MoverType.SELF,new Vec3(slide.x,-.3,slide.z));setDeltaMovement(Vec3.ZERO);
				level.sendParticles(ParticleTypes.LARGE_SMOKE,getX(),getY()+1.4,getZ(),2,.4,.8,.4,.01);
				if(--left<=0){pose(LanternBearerRules.WATCH,2);level.sendParticles(ParticleTypes.SOUL,getX(),getY()+1.4,getZ(),8,.4,.8,.4,.02);}
			}
			case LanternBearerRules.RECOVER->{still();if(--left<=0)pose(LanternBearerRules.WATCH,gap());}
			default->watch(level,target);
		}
	}

	/** Up and tracking: drifting after its prey, turning only so fast, so its back can be got at. */
	private void watch(ServerLevel level,LivingEntity target){
		if(getDeltaMovement().horizontalDistanceSqr()>.0004 && tickCount%12==0)Feels.sound(level,position(),"aura_lantern_bearer_step",.8F,.9F+random.nextFloat()*.2F);
		if(target==null){
			quiet++;
			var at=spot();
			if(quiet>=LanternBearerRules.RETURN_TICKS && distanceToSqr(at)>1.5)getNavigation().moveTo(at.x,at.y,at.z,1);
			if(quiet>=LanternBearerRules.RESET_TICKS && (distanceToSqr(at)<=2.5 || quiet>=LanternBearerRules.RESET_TICKS+100))sink(level);
			return;
		}
		quiet=0;
		getLookControl().setLookAt(target,30,30);
		double d=distanceTo(target);
		boolean behind=LanternBearerRules.behind(yawTo(target.position())-getYRot());
		if(left>0)left--;
		if(left<=0){
			long now=level.getGameTime();
			// Half beaten for the first time, it holds its rite before anything else.
			if(LanternBearerRules.rouses(getHealth(),getMaxHealth(),roused())){links=0;begin(level,LanternBearerRules.RITE,target,false);return;}
			long[] since=new long[lastMade.length];for(int i=0;i<since.length;i++)since[i]=now-lastMade[i];
			int pick=LanternBearerRules.choose(deck,d,behind,target.getY()-getY(),since,roused(),random.nextDouble());
			if(pick>=0){links=0;begin(level,pick,target,false);return;}
		}
		if(d>LanternBearerRules.CLOSE){
			if(!getNavigation().moveTo(target,1))getMoveControl().setWantedPosition(target.getX(),target.getY(),target.getZ(),1);
		}else{getNavigation().stop();turn(yawTo(target.position()),9);}
	}

	/** Sets off on {@code move} at {@code target}, {@code chained} when run straight on into from the last one (a shorter tell). */
	private void begin(ServerLevel level,int move,LivingEntity target,boolean chained){
		long now=level.getGameTime();lastMade[move]=now;if(move!=LanternBearerRules.RITE)deck.played(move);
		struck.clear();again=0;
		// The backsweep never turns to face anyone: it is for whoever is at its back.
		if(move==LanternBearerRules.BACKSWEEP)aim=getYRot();else if(target!=null)aim=yawTo(target.position());
		int tell=LanternBearerRules.tell(move,chained);
		var h=head();var l=lamp();
		switch(move){
			case LanternBearerRules.REAP->{pose(move,0,tell);sound(level,blade(),SoundEvents.GRAVEL_BREAK,1.2F,.6F);Feels.sound(level,h,"aura_lantern_bearer_fade",.8F,1.3F);}
			case LanternBearerRules.HOOK->{hooked.clear();pose(move,0,tell);Feels.sound(level,l,"aura_lantern_bearer_chain",1.1F,.9F);sound(level,l,SoundEvents.CHAIN_PLACE,1F,.7F);}
			case LanternBearerRules.WISPS->{pose(move,0,tell);Feels.sound(level,l,"aura_lantern_bearer_wisp",1.2F,.8F);}
			case LanternBearerRules.GRAVES->{pose(move,0,tell);sound(level,h,SoundEvents.EVOKER_PREPARE_ATTACK,1.2F,.6F);Feels.sound(level,h,"aura_lantern_bearer_fade",1,.7F);}
			case LanternBearerRules.FLARE->{pose(move,0,tell);sound(level,l,SoundEvents.FIRECHARGE_USE,1.2F,.5F);Feels.sound(level,l,"aura_lantern_bearer_wisp",1.2F,.6F);}
			case LanternBearerRules.SHOVEL->{pose(move,0,tell);sound(level,blade(),SoundEvents.ROOTED_DIRT_BREAK,1.4F,.6F);sound(level,blade(),SoundEvents.SNIFFER_DIGGING,1.2F,.8F);}
			case LanternBearerRules.BACKSWEEP->{pose(move,0,tell);Feels.sound(level,h,"aura_lantern_bearer_fade",1F,1.1F);sound(level,l,SoundEvents.CHAIN_PLACE,1F,.6F);}
			case LanternBearerRules.BREATH->{entityData.set(SIDE,random.nextBoolean()?1:-1);pose(move,0,tell);sound(level,h,SoundEvents.WARDEN_SONIC_CHARGE,1.2F,1.5F);}
			case LanternBearerRules.LIGHTS_OUT->{lightsTurn=random.nextInt(4);pose(move,0,tell);Feels.sound(level,l,"aura_lantern_bearer_chain",1.2F,.7F);}
			case LanternBearerRules.RITE->{
				pose(move,0,tell);Feels.sound(level,h,"aura_lantern_bearer_roar",1.6F,.7F);sound(level,h,SoundEvents.ILLUSIONER_CAST_SPELL,1.6F,.6F);
				candles(level,true);for(var p:prey(level))p.addEffect(new MobEffectInstance(MobEffects.DARKNESS,50,0,false,false));
			}
			default->pose(LanternBearerRules.WATCH,gap());
		}
	}
	/** Done with {@code move}: roused, perhaps straight on into another; otherwise spent {@code recover} ticks. */
	private void after(ServerLevel level,int move,int recover){
		var target=getTarget();
		if(roused() && target!=null && links<LanternBearerRules.CHAIN_MOST){
			int next=LanternBearerRules.chain(move,distanceTo(target),random.nextDouble());
			if(next>=0){links++;begin(level,next,target,true);return;}
		}
		spent(links>0?recover+LanternBearerRules.CHAIN_REST:recover);links=0;
	}

	/**
	 * The low reap. Through the tell the spade is dragged low behind its right side, scraping up earth and sparks, turning after its prey
	 * until the aim locks and the reach of it is traced on the ground; then swept round at ankle height from its right to its left,
	 * striking each one it passes once. Roused, it sweeps straight back the other way.
	 */
	private void reap(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell-LanternBearerRules.REAP_LOCK && target!=null)aim=turned(aim,yawTo(target.position()),(float)LanternBearerRules.REAP_TURN);
		face(aim);still();
		if(el<tell){
			var b=blade();
			if(el%2==0){
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,b)),b.x,b.y,b.z,3,.2,.05,.2,.1);
				level.sendParticles(ParticleTypes.CRIT,b.x,b.y+.1,b.z,1,.1,.05,.1,.1);
			}
			if(el%4==0)sound(level,b,SoundEvents.GRAVEL_HIT,1F,.6F+el/(float)tell*.4F);
			if(el>=tell-LanternBearerRules.REAP_LOCK)arc(level,-LanternBearerRules.REAP_ARC,LanternBearerRules.REAP_ARC,LanternBearerRules.REAP_REACH,SOUL_SMALL,9);
			return;
		}
		long k=el-tell;
		if(k<LanternBearerRules.REAP_SWEEP)sweep(level,k,false);
		if(!roused()){if(k>=LanternBearerRules.REAP_SWEEP+2)after(level,LanternBearerRules.REAP,LanternBearerRules.REAP_RECOVER);return;}
		long k2=k-LanternBearerRules.REAP_SWEEP-LanternBearerRules.REAP_BACK;
		if(k2<0){
			// The spade dragged round behind its left side for the sweep back.
			var f=ahead(aim);var r=new Vec3(-f.z,0,f.x);var at=position().add(f.scale(.4)).add(r.scale(-1.2));
			if(k2%2==0)level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,at)),at.x,at.y+.2,at.z,3,.2,.05,.2,.1);
			if(k2==-LanternBearerRules.REAP_BACK+1)sound(level,at,SoundEvents.GRAVEL_HIT,1.2F,.8F);
			return;
		}
		if(k2==0)struck.clear();
		if(k2<LanternBearerRules.REAP_SWEEP)sweep(level,k2,true);
		if(k2>=LanternBearerRules.REAP_SWEEP+2)after(level,LanternBearerRules.REAP,LanternBearerRules.REAP_RECOVER);
	}
	/** Tick {@code k} of a low sweep: the slice of the arc the spade passes through this tick, and whoever stands low in it. */
	private void sweep(ServerLevel level,long k,boolean back){
		double a0=LanternBearerRules.reapArc(k,back),a1=LanternBearerRules.reapArc(k+1,back);
		if(k==0){Feels.sound(level,position().add(0,.6,0),"aura_lantern_bearer_reap",1.4F,.8F+random.nextFloat()*.2F);sound(level,position(),SoundEvents.PLAYER_ATTACK_SWEEP,1.4F,.6F);}
		for(int i=0;i<3;i++){
			var f=ahead((float)(aim+a0+(a1-a0)*i/2));
			for(double r=1.6;r<=LanternBearerRules.REAP_REACH;r+=1.4)level.sendParticles(ParticleTypes.SWEEP_ATTACK,getX()+f.x*r,getY()+.3,getZ()+f.z*r,0,0,0,0,0);
			var tip=position().add(f.scale(LanternBearerRules.REAP_REACH));
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,tip)),tip.x,tip.y+.1,tip.z,2,.2,.05,.2,.1);
		}
		var f=ahead(aim);
		for(var p:prey(level)){
			if(struck.contains(p.getUUID()))continue;
			double dx=p.getX()-getX(),dz=p.getZ()-getZ(),fw=dx*f.x+dz*f.z,sd=dx*-f.z+dz*f.x;
			if(LanternBearerRules.hits(LanternBearerRules.REAP,fw,sd,p.getY()-getY()) && LanternBearerRules.sweeps(LanternBearerRules.bearing(fw,sd),a0,a1)){
				struck.add(p.getUUID());hit(level,p,LanternBearerRules.REAP_DAMAGE,.9,position(),.25);
			}
		}
	}
	/** Soul dust traced on the ground in an arc from {@code from} to {@code to} degrees off its aim, {@code r} out. */
	private void arc(ServerLevel level,double from,double to,double r,ParticleOptions dust,int n){
		for(int i=0;i<n;i++){var f=ahead((float)(aim+from+(to-from)*i/(n-1)));level.sendParticles(dust,getX()+f.x*r,getY()+.15,getZ()+f.z*r,1,0,0,0,0);}
	}

	/** Where a missed crook bit into the earth. */
	private Vec3 snagAt=Vec3.ZERO;
	/**
	 * The hook. The crook drawn back with the lantern swinging, turning after its prey while a line of soul fire is laid on the ground
	 * along where it will go, the aim locking a few ticks before; then thrust out along it. Whoever it catches is struck and dragged in
	 * close, the chain of soul light between them, for {@link LanternBearerRules#HOOK_DRAG} ticks. Catching nobody it bites into the
	 * earth and sticks; roused, it is first wrenched back and thrown again.
	 */
	private void hook(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell-LanternBearerRules.HOOK_LOCK && target!=null)aim=turned(aim,yawTo(target.position()),(float)LanternBearerRules.HOOK_TURN);
		face(aim);still();
		var f=ahead(aim);
		if(el<tell){
			if(el>=tell-LanternBearerRules.HOOK_MARK && el%2==0)
				for(double d=1.5;d<=LanternBearerRules.HOOK_REACH;d+=1)level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,getX()+f.x*d,getY()+.1,getZ()+f.z*d,1,.05,0,.05,0);
			if(el%5==0){var l=lamp();sound(level,l,SoundEvents.CHAIN_PLACE,.8F,.8F+el/(float)tell*.4F);level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,l.x,l.y,l.z,2,.1,.1,.1,.01);}
			return;
		}
		if(el==tell){
			Feels.sound(level,lamp(),"aura_lantern_bearer_hook",1.4F,1);sound(level,lamp(),SoundEvents.TRIDENT_THROW.value(),1.2F,.7F);
			double reach=LanternBearerRules.HOOK_REACH;
			for(var p:prey(level)){
				if(struck.contains(p.getUUID()) || !reaches(LanternBearerRules.HOOK,p))continue;
				struck.add(p.getUUID());hooked.add(p.getUUID());hit(level,p,LanternBearerRules.HOOK_DAMAGE,0,position(),0);
				reach=Math.min(reach,distanceTo(p));
			}
			for(double d=1;d<=reach;d+=.5)level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,getX()+f.x*d,getY()+1.6,getZ()+f.z*d,1,.02,.02,.02,0);
			if(hooked.isEmpty()){
				if(roused() && again==0){
					again=1;struck.clear();if(target!=null)aim=yawTo(target.position());pose(LanternBearerRules.HOOK,0,LanternBearerRules.HOOK_RETELL);
					sound(level,lamp(),SoundEvents.CHAIN_PLACE,1.2F,1.2F);
					return;
				}
				double far=target==null?6:Math.max(3,Math.min(LanternBearerRules.HOOK_REACH,distanceTo(target)));
				snagAt=floor(level,position().add(f.scale(far)));
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,snagAt)),snagAt.x,snagAt.y+.1,snagAt.z,16,.3,.1,.3,.15);
				sound(level,snagAt,SoundEvents.ROOTED_DIRT_BREAK,1.4F,.7F);
				links=0;pose(LanternBearerRules.SNAGGED,LanternBearerRules.SNAG_TICKS);still();
				return;
			}
		}
		long drag=el-tell;
		if(drag>0 && drag<=LanternBearerRules.HOOK_DRAG){
			if(drag%3==1)Feels.sound(level,position().add(0,1.5,0),"aura_lantern_bearer_chain",1,1.1F);
			var from=position().add(0,1.6,0);
			for(var p:prey(level)){
				if(!hooked.contains(p.getUUID()))continue;
				var to=new Vec3(getX()-p.getX(),0,getZ()-p.getZ());double d=to.length();
				double speed=LanternBearerRules.pull(d);
				if(speed>0 && d>1e-3)p.setDeltaMovement(to.scale(speed/d).add(0,.12,0));
				p.needsSync=true;
				var step=p.position().add(0,1,0).subtract(from);
				for(int i=1;i<6;i++){var at=from.add(step.scale(i/6.0));level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,at.x,at.y,at.z,1,0,0,0,0);}
			}
		}
		if(drag>=LanternBearerRules.HOOK_DRAG){hooked.clear();after(level,LanternBearerRules.HOOK,LanternBearerRules.HOOK_RECOVER);}
	}
	/** Its crook bitten into the earth, the chain taut from its lantern hand, tugging it free: the opening. */
	private void snagged(ServerLevel level){
		face(aim);still();long el=since();
		if(el%2==0){
			var from=lamp();var step=snagAt.subtract(from);
			for(int i=1;i<6;i++){var at=from.add(step.scale(i/6.0));level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,at.x,at.y,at.z,1,0,0,0,0);}
		}
		if(el%6==0){
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,snagAt)),snagAt.x,snagAt.y+.1,snagAt.z,5,.2,.05,.2,.1);
			sound(level,snagAt,SoundEvents.ROOTED_DIRT_HIT,1.2F,.6F+random.nextFloat()*.2F);sound(level,lamp(),SoundEvents.CHAIN_PLACE,.8F,.6F);
		}
		if(--left<=0){
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,snagAt)),snagAt.x,snagAt.y+.2,snagAt.z,14,.3,.1,.3,.15);
			sound(level,snagAt,SoundEvents.ROOTED_DIRT_BREAK,1.3F,.9F);Feels.sound(level,lamp(),"aura_lantern_bearer_chain",1F,1.2F);
			pose(LanternBearerRules.WATCH,gap());
		}
	}

	/** Where the lantern is, whirled round over its hood, {@code el} ticks in. */
	private Vec3 overhead(long el){double a=el*.9;return position().add(Math.cos(a)*1.1,4.1,Math.sin(a)*1.1);}
	/**
	 * The wisps. The lantern whirled round over its hood, faster and brighter through the tell; then its wisps loosed one after another,
	 * each after the next of its prey in turn.
	 */
	private void loose(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();int n=roused()?LanternBearerRules.WISP_COUNT_ROUSED:LanternBearerRules.WISP_COUNT;
		long last=(long)(n-1)*LanternBearerRules.WISP_EVERY;
		if(target!=null && el<tell+last)aim=turned(aim,yawTo(target.position()),10);
		face(aim);still();
		var top=overhead(el);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,top.x,top.y,top.z,el<tell?1+(int)(el*3/Math.max(1,tell)):2,.05,.05,.05,.005);
		if(el<tell){if(el%4==0)sound(level,top,SoundEvents.SOUL_ESCAPE.value(),1F,.6F+el/(float)tell*.6F);return;}
		long k=el-tell;
		if(k%LanternBearerRules.WISP_EVERY==0 && k<=last){
			int i=(int)(k/LanternBearerRules.WISP_EVERY);
			var targets=prey(level);if(targets.isEmpty() && target instanceof ServerPlayer p)targets.add(p);
			if(!targets.isEmpty()){
				var f=ahead(aim);double spread=(i%2==0?1:-1)*(.3+.25*i);
				var going=new Vec3(f.x-f.z*spread,.5,f.z+f.x*spread).normalize().scale(LanternBearerRules.WISP_SPEED*1.5);
				wisps.add(new Wisp(top,going,targets.get(i%targets.size()).getUUID()));
			}
			Feels.sound(level,top,"aura_lantern_bearer_wisp",1.3F,1+i*.08F);
			level.sendParticles(ParticleTypes.SOUL,top.x,top.y,top.z,6,.2,.2,.2,.04);
		}
		if(k>=last+4)after(level,LanternBearerRules.WISPS,LanternBearerRules.WISP_RECOVER);
	}
	/**
	 * The wisps it has loosed, drifting after their prey while they can see them and straight on while they cannot, guttering out when
	 * they have gone too long without; bursting on their prey, on whatever they fly into, or when they burn out.
	 */
	private void drift(ServerLevel level){
		for(var it=wisps.iterator();it.hasNext();){
			var w=it.next();w.age++;
			var p=level.getPlayerByUUID(w.prey);
			boolean gone=p==null || !p.isAlive() || p.isSpectator() || p.level()!=level;
			if(!gone){
				var eye=p.position().add(0,p.getBbHeight()*.6,0);
				if(w.age%2==0){
					boolean sees=level.clip(new ClipContext(w.at,eye,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()==HitResult.Type.MISS;
					w.unseen=sees?0:w.unseen+2;
				}
				if(w.unseen==0){
					var want=eye.subtract(w.at).normalize().scale(LanternBearerRules.WISP_SPEED);
					w.going=w.going.add(want.subtract(w.going).scale(LanternBearerRules.WISP_TURN));
				}
			}
			w.at=w.at.add(w.going);
			level.sendParticles(w.unseen>0?ParticleTypes.SMOKE:ParticleTypes.SOUL_FIRE_FLAME,w.at.x,w.at.y,w.at.z,2,.05,.05,.05,.005);
			if(w.age%3==0 && w.unseen==0)level.sendParticles(ParticleTypes.SOUL,w.at.x,w.at.y,w.at.z,1,.05,.05,.05,.005);
			if(LanternBearerRules.gutters(w.unseen)){
				it.remove();
				level.sendParticles(ParticleTypes.SMOKE,w.at.x,w.at.y,w.at.z,8,.15,.15,.15,.02);
				sound(level,w.at,SoundEvents.FIRE_EXTINGUISH,.7F,1.4F);
				continue;
			}
			var block=BlockPos.containing(w.at);
			boolean hitBlock=!level.getBlockState(block).getCollisionShape(level,block).isEmpty();
			boolean landed=!gone && LanternBearerRules.bursts(p.position().add(0,p.getBbHeight()*.6,0).distanceTo(w.at));
			if(landed || hitBlock || gone || w.age>=LanternBearerRules.WISP_LIFE){
				it.remove();
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,w.at.x,w.at.y,w.at.z,10,.2,.2,.2,.06);
				Feels.sound(level,w.at,"aura_lantern_bearer_burst",1,1+random.nextFloat()*.2F);
				if(landed && GuardianBite.land(level,this,p,(float)LanternBearerRules.WISP_DAMAGE,LanternBearerRules.PIERCE))
					p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,LanternBearerRules.WISP_SLOW,1,false,true));
			}
		}
	}

	/**
	 * The graves. The spade raised high in both hands over its hood, souls gathering round its feet; then driven down into the earth in
	 * front of it. A grave is marked under each of its prey, a ring of soul dust closing in on it, and bursts up when it has closed;
	 * roused, another is laid ahead of where the nearest is going. The spade stays stuck while they close: the opening.
	 */
	private void dig(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<tell-2 && target!=null)aim=turned(aim,yawTo(target.position()),8);
		face(aim);still();
		var f=ahead(aim);var spade=position().add(f.scale(1.1));
		if(el<tell){
			var h=head().add(0,1.2,0);
			level.sendParticles(ParticleTypes.SOUL,getX(),getY()+.2,getZ(),1,1.4,.05,1.4,.01);
			if(el%3==0)level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,h.x,h.y,h.z,1,.15,.1,.15,.005);
			if(el==tell-6)sound(level,h,SoundEvents.SHOVEL_FLATTEN.value(),1.2F,.5F);
			return;
		}
		if(el==tell){
			long now=level.getGameTime();
			sound(level,spade,SoundEvents.MACE_SMASH_GROUND_HEAVY,1.2F,.7F);sound(level,spade,SoundEvents.ROOTED_DIRT_BREAK,1.4F,.6F);
			Feels.sound(level,spade,"aura_lantern_bearer_slam",1.4F,.8F);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,spade)),spade.x,spade.y+.2,spade.z,24,.5,.2,.5,.15);
			ring(level,spade,1.2,SOUL_DUST,12);
			var caught=new HashSet<UUID>();
			for(var p:prey(level))marks.add(new Mark(floor(level,p.position()),now,caught));
			if(roused() && target!=null && preyVel.lengthSqr()>.002){
				var led=target.position().add(preyVel.scale(LanternBearerRules.GRAVE_LEAD));
				if(inHall(led,0))marks.add(new Mark(floor(level,led),now,caught));
			}
		}
		// The spade stuck fast, earth crumbling off it, souls running out along the ground to the graves.
		if(el%4==0)level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,spade)),spade.x,spade.y+.3,spade.z,3,.2,.1,.2,.05);
		if(el%3==0)level.sendParticles(ParticleTypes.SOUL,spade.x,spade.y+.4,spade.z,1,.3,.2,.3,.02);
		if(el==tell+LanternBearerRules.GRAVE_MARK+LanternBearerRules.GRAVE_HOLD-4)sound(level,spade,SoundEvents.ROOTED_DIRT_HIT,1.2F,.8F);
		if(el>=tell+LanternBearerRules.GRAVE_MARK+LanternBearerRules.GRAVE_HOLD)after(level,LanternBearerRules.GRAVES,LanternBearerRules.GRAVE_RECOVER);
	}
	/** The graves it has marked, closing in and bursting up. */
	private void marks(ServerLevel level){
		if(marks.isEmpty())return;
		long now=level.getGameTime();
		for(var it=marks.iterator();it.hasNext();){
			var m=it.next();long age=now-m.born();var at=m.at();
			if(age<LanternBearerRules.GRAVE_MARK){
				if(age%2==0){
					ring(level,at,LanternBearerRules.graveRing(age),age>=LanternBearerRules.GRAVE_MARK-6?SOUL_DUST:SOUL_SMALL,14);
					level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,at.x,at.y+.1,at.z,1,.2,0,.2,0);
				}
				if(age>=LanternBearerRules.GRAVE_MARK-6)level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,at)),at.x,at.y+.1,at.z,2,.5,.05,.5,.05);
				if(age%6==0)sound(level,at,SoundEvents.ROOTED_DIRT_HIT,.9F,.6F+age/(float)LanternBearerRules.GRAVE_MARK*.6F);
				continue;
			}
			it.remove();
			level.sendParticles(new BlockParticleOption(ParticleTypes.DUST_PILLAR,ground(level,at)),at.x,at.y+.1,at.z,30,.6,.1,.6,.4);
			level.sendParticles(ParticleTypes.SOUL,at.x,at.y+.5,at.z,10,.4,.8,.4,.06);
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,at.x,at.y+.5,at.z,8,.3,.6,.3,.08);
			sound(level,at,SoundEvents.ROOTED_DIRT_BREAK,1.5F,.7F);sound(level,at,SoundEvents.SOUL_ESCAPE.value(),1.2F,.6F);
			for(var p:prey(level))
				if(!m.caught().contains(p.getUUID()) && LanternBearerRules.buries(p.getX()-at.x,p.getY()-at.y,p.getZ()-at.z)){
					m.caught().add(p.getUUID());hit(level,p,LanternBearerRules.GRAVE_DAMAGE,0,at,LanternBearerRules.GRAVE_LIFT);
				}
		}
	}

	/** The lantern drawn in under its hood. */
	private Vec3 hood(){return position().add(ahead(getYRot()).scale(.5)).add(0,2.6,0);}
	/**
	 * The flare. The lantern drawn in under its hood and hugged close, brightening, while souls are drawn in off the ground from all round
	 * it and the edge of the blast is traced in soul dust; then it flares, burning and throwing everyone inside. Roused, the flare rolls
	 * on out across the field as a ring of fire low to the ground.
	 */
	private void flare(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();face(aim);still();
		var c=position();
		if(el<tell){
			var h=hood();
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,h.x,h.y,h.z,1+(int)(el*3/tell),.1,.1,.1,.01);
			for(int i=0;i<2;i++){
				double a=random.nextDouble()*Math.PI*2,r=LanternBearerRules.FLARE_RADIUS;
				level.sendParticles(ParticleTypes.SOUL,c.x+Math.cos(a)*r,c.y+.2,c.z+Math.sin(a)*r,0,-Math.cos(a),.15,-Math.sin(a),.18);
			}
			if(el%2==0)ring(level,c,LanternBearerRules.FLARE_RADIUS,el>=tell-6?SOUL_DUST:SOUL_SMALL,el>=tell-6?30:20);
			if(el%4==0)sound(level,h,SoundEvents.LAVA_POP,1F,.6F+el/(float)tell*.8F);
			return;
		}
		if(el==tell){
			Feels.sound(level,c,"aura_lantern_bearer_burst",1.8F,.7F);sound(level,c,SoundEvents.GENERIC_EXPLODE.value(),1F,1.4F);
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,c.x,c.y+1.5,c.z,50,.4,.4,.4,.25);
			for(int i=0;i<36;i++){double a=i*Math.PI*2/36;level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,c.x,c.y+.3,c.z,0,Math.cos(a),0,Math.sin(a),.4);}
			ring(level,c,LanternBearerRules.FLARE_RADIUS,SOUL_DUST,32);
			for(var p:prey(level))
				if(!struck.contains(p.getUUID()) && reaches(LanternBearerRules.FLARE,p)){
					struck.add(p.getUUID());hit(level,p,LanternBearerRules.FLARE_DAMAGE,LanternBearerRules.FLARE_SHOVE,c,.4);p.igniteForSeconds(LanternBearerRules.FLARE_BURN);
				}
			if(roused()){
				var w=new Wave(c,LanternBearerRules.FLARE_WAVE_SPEED,LanternBearerRules.FLARE_WAVE_REACH,LanternBearerRules.FLARE_WAVE_DAMAGE,
					level.getGameTime()-(long)(LanternBearerRules.FLARE_RADIUS/LanternBearerRules.FLARE_WAVE_SPEED));
				w.struck.addAll(struck);waves.add(w);
			}
		}
		if(el>=tell+3)after(level,LanternBearerRules.FLARE,LanternBearerRules.FLARE_RECOVER);
	}
	/** The rings of fire it has sent rolling out, low along the ground; each catches whoever is not over it, once. */
	private void waves(ServerLevel level){
		if(waves.isEmpty())return;
		long now=level.getGameTime();
		for(var it=waves.iterator();it.hasNext();){
			var w=it.next();double r0=(now-w.born-1)*w.speed,r1=(now-w.born)*w.speed;
			if(r0>w.max){it.remove();continue;}
			int n=Math.min(64,Math.max(12,(int)(r1*4)));
			for(int i=0;i<n;i++){double a=(i+random.nextDouble()*.5)*Math.PI*2/n;level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,w.x+Math.cos(a)*r1,w.y+.15,w.z+Math.sin(a)*r1,1,0,.04,0,.004);}
			var at=new Vec3(w.x,w.y,w.z);
			if((now-w.born)%4==0)sound(level,at,SoundEvents.FIRECHARGE_USE,.6F,1.3F);
			for(var p:prey(level)){
				if(w.struck.contains(p.getUUID()))continue;
				double dx=p.getX()-w.x,dz=p.getZ()-w.z;
				if(LanternBearerRules.rolls(Math.sqrt(dx*dx+dz*dz),r0,r1,p.getY()-w.y)){w.struck.add(p.getUUID());hit(level,p,w.damage,.6,at,.3);p.igniteForSeconds(2);}
			}
		}
	}

	/**
	 * The shovel. The spade dug in deep at its right side, levering, the earth heaving up off it, turning after its prey until the aim
	 * locks; then a fan of grave earth flung off it high round where they stand, each clod's landing marked by a closing ring. Never
	 * nearer than {@link LanternBearerRules#SHOVEL_NEAR}: close in under it. Roused, it digs again and flings a second fan between.
	 */
	private void shovel(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();boolean twice=roused();
		if(target!=null && (el<=tell-LanternBearerRules.SHOVEL_LOCK || twice && el>tell && el<=tell+LanternBearerRules.SHOVEL_AGAIN-LanternBearerRules.SHOVEL_LOCK))
			aim=turned(aim,yawTo(target.position()),(float)LanternBearerRules.SHOVEL_TURN);
		face(aim);still();
		var b=blade();
		boolean digging=el<tell || twice && el>tell && el<tell+LanternBearerRules.SHOVEL_AGAIN;
		if(digging){
			if(el%2==0)level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,b)),b.x,b.y+.1,b.z,4,.25,.1,.25,.12);
			if(el%2==1)level.sendParticles(EARTH,b.x,b.y+.3,b.z,2,.3,.2,.3,0);
			if(el%5==0)sound(level,b,SoundEvents.ROOTED_DIRT_HIT,1.1F,.6F+random.nextFloat()*.2F);
		}
		if(el==tell)fling(level,target,false);
		if(twice && el==tell+LanternBearerRules.SHOVEL_AGAIN)fling(level,target,true);
		if(el>=tell+(twice?LanternBearerRules.SHOVEL_AGAIN:0)+4)after(level,LanternBearerRules.SHOVEL,LanternBearerRules.SHOVEL_RECOVER);
	}
	/** One fan of clods flung off the spade round where {@code target} stands; the {@code second} falls between the first. */
	private void fling(ServerLevel level,LivingEntity target,boolean second){
		double d=target==null?8:Math.sqrt((target.getX()-getX())*(target.getX()-getX())+(target.getZ()-getZ())*(target.getZ()-getZ()));
		var f=ahead(aim);var r=new Vec3(-f.z,0,f.x);var from=blade().add(0,1.4,0);
		long now=level.getGameTime();var caught=new HashSet<UUID>();
		for(int i=0;i<LanternBearerRules.CLODS;i++){
			var s=LanternBearerRules.shovelSpot(i,d,second);
			var to=floor(level,position().add(f.scale(s[0])).add(r.scale(s[1])));
			clods.add(new Clod(from,to,now,3+random.nextDouble()*1.5,caught));
		}
		sound(level,from,SoundEvents.ROOTED_DIRT_BREAK,1.6F,.7F);sound(level,from,SoundEvents.PLAYER_ATTACK_SWEEP,1F,.5F);
		Feels.sound(level,from,"aura_lantern_bearer_reap",1F,.6F);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,blade())),from.x,from.y,from.z,20,.4,.3,.4,.2);
	}
	/** The clods in the air, arcing down onto their marks. */
	private void clods(ServerLevel level){
		if(clods.isEmpty())return;
		long now=level.getGameTime();
		for(var it=clods.iterator();it.hasNext();){
			var c=it.next();long age=now-c.born();double t=age/(double)LanternBearerRules.CLOD_FLIGHT;var to=c.to();
			if(t<1){
				var at=c.from().lerp(to,t).add(0,c.peak()*4*t*(1-t),0);
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.COARSE_DIRT.defaultBlockState()),at.x,at.y,at.z,3,.15,.15,.15,0);
				level.sendParticles(EARTH,at.x,at.y,at.z,1,.1,.1,.1,0);
				if(age%2==0)ring(level,to,LanternBearerRules.CLOD_RADIUS*(1.3-.3*t),t>.6?SOUL_DUST:SOUL_SMALL,10);
				continue;
			}
			it.remove();
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,to)),to.x,to.y+.2,to.z,18,.5,.2,.5,.15);
			level.sendParticles(ParticleTypes.SOUL,to.x,to.y+.3,to.z,3,.3,.2,.3,.02);
			sound(level,to,SoundEvents.GRAVEL_BREAK,1.2F,.7F+random.nextFloat()*.2F);sound(level,to,SoundEvents.ROOTED_DIRT_FALL,1.2F,.6F);
			for(var p:prey(level))
				if(!c.caught().contains(p.getUUID()) && LanternBearerRules.clods(p.getX()-to.x,p.getY()-to.y,p.getZ()-to.z)){
					c.caught().add(p.getUUID());hit(level,p,LanternBearerRules.CLOD_DAMAGE,.4,to,.2);
				}
		}
	}

	/**
	 * The backsweep. Looking back over its shoulder at whoever is at its back, the crook drawn across its body with the lantern swinging,
	 * the reach of it traced in soul dust behind it; then swept round behind it in a burning arc. Its front and flanks are safe.
	 */
	private void backsweep(ServerLevel level){
		long el=since();int tell=tell();face(aim);still();
		double from=LanternBearerRules.BACK_FROM,to=360-LanternBearerRules.BACK_FROM;
		if(el<tell){
			if(el%2==0)arc(level,from,to,LanternBearerRules.BACK_REACH,el>=tell-4?SOUL_DUST:SOUL_SMALL,11);
			if(el%4==0){var l=lamp();sound(level,l,SoundEvents.CHAIN_PLACE,.9F,.7F+el/(float)tell*.5F);level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,l.x,l.y,l.z,2,.1,.1,.1,.01);}
			return;
		}
		if(el==tell){
			Feels.sound(level,position().add(0,1.5,0),"aura_lantern_bearer_reap",1.4F,.65F);sound(level,position(),SoundEvents.PLAYER_ATTACK_SWEEP,1.4F,.5F);
			for(int i=0;i<10;i++){
				var f=ahead((float)(aim+from+(to-from)*i/9));
				for(double r=2;r<=LanternBearerRules.BACK_REACH;r+=1.6)level.sendParticles(ParticleTypes.SWEEP_ATTACK,getX()+f.x*r,getY()+1.2,getZ()+f.z*r,0,0,0,0,0);
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,getX()+f.x*LanternBearerRules.BACK_REACH,getY()+1,getZ()+f.z*LanternBearerRules.BACK_REACH,2,.1,.2,.1,.02);
			}
			for(var p:prey(level))
				if(!struck.contains(p.getUUID()) && reaches(LanternBearerRules.BACKSWEEP,p)){
					struck.add(p.getUUID());hit(level,p,LanternBearerRules.BACK_DAMAGE,1.2,position(),.25);p.igniteForSeconds(LanternBearerRules.BACK_BURN);
				}
		}
		if(el>=tell+3)after(level,LanternBearerRules.BACKSWEEP,LanternBearerRules.BACK_RECOVER);
	}

	/** Its mouth, under its hood, where it breathes across the lantern held up to it. */
	private Vec3 mouth(){return position().add(ahead(getYRot()).scale(.8)).add(0,2.7,0);}
	/**
	 * The breath. The lantern raised to its mouth and its head cocked the way it will turn, soul fire gathering and the way it will turn
	 * traced on the ground; then soul fire breathed across the lantern in a cone that turns slowly round that way, burning whoever it
	 * passes over once. Roused, longer, and swept right back across to the other side. After it, it coughs smoke: the opening, more so
	 * behind it.
	 */
	private void breathe(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();boolean r=roused();int side=side();int len=LanternBearerRules.breathTicks(r);
		if(el<=tell-LanternBearerRules.BREATH_LOCK && target!=null)aim=turned(aim,yawTo(target.position()),(float)LanternBearerRules.BREATH_TURN_TELL);
		still();
		if(el<tell){
			face(aim);var m=mouth();
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,m.x,m.y,m.z,1+(int)(el*2/tell),.08,.08,.08,.005);
			if(el>=tell-8 && el%2==0)arc(level,0,side*LanternBearerRules.BREATH_TURN,3.2,SOUL_SMALL,8);
			if(el%4==0)sound(level,m,SoundEvents.SOUL_ESCAPE.value(),1F,.5F+el/(float)tell*.5F);
			return;
		}
		long k=el-tell;
		if(k<len){
			float yaw=(float)(aim+side*LanternBearerRules.breathTurn(k,r));face(yaw);
			var m=mouth();
			if(k==0){Feels.sound(level,m,"aura_lantern_bearer_wisp",1.5F,.5F);sound(level,m,SoundEvents.BLAZE_SHOOT,1.4F,.6F);}
			if(k%4==0)sound(level,m,SoundEvents.FIRECHARGE_USE,1F,.7F+random.nextFloat()*.2F);
			for(int i=0;i<6;i++){
				var dir=ahead((float)(yaw+(random.nextDouble()*2-1)*LanternBearerRules.BREATH_HALF*.8));
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,m.x,m.y,m.z,0,dir.x,-.18,dir.z,.55+random.nextDouble()*.25);
			}
			for(var p:prey(level))
				if(!struck.contains(p.getUUID()) && reaches(LanternBearerRules.BREATH,p,yaw)){
					struck.add(p.getUUID());hit(level,p,LanternBearerRules.BREATH_DAMAGE,.5,position(),.1);p.igniteForSeconds(LanternBearerRules.BREATH_BURN);
				}
			return;
		}
		face((float)(aim+side*LanternBearerRules.breathTurn(len,r)));
		long c=k-len;
		if(c%4==0){var m=mouth();level.sendParticles(ParticleTypes.SMOKE,m.x,m.y,m.z,5,.15,.1,.15,.02);sound(level,m,SoundEvents.FIRE_EXTINGUISH,.6F,.6F+random.nextFloat()*.3F);}
		if(c>=LanternBearerRules.BREATH_COUGH)after(level,LanternBearerRules.BREATH,LanternBearerRules.BREATH_RECOVER);
	}

	/** The lantern held up high over its hood. */
	private Vec3 raised(){return position().add(ahead(getYRot()).scale(.3)).add(0,4.3,0);}
	/**
	 * The rite, once, half beaten. The lantern held up high over its hood and the souls of the field streaming up out of the graves into
	 * it while the candles flare, the edge of the blast traced in soul dust at the last; then it bursts, throwing everyone near, and a
	 * ring of fire rolls out across the whole field to be jumped. Holding it, it is half there; after, it stands open a while. Then it
	 * fights on roused.
	 */
	private void rite(ServerLevel level){
		long el=since();int tell=tell();face(getYRot());still();
		var c=position();var top=raised();
		if(el<tell){
			for(int i=0;i<2;i++){
				double a=random.nextDouble()*Math.PI*2,r=4+random.nextDouble()*7;
				var from=new Vec3(c.x+Math.cos(a)*r,c.y+.2,c.z+Math.sin(a)*r);var v=top.subtract(from).normalize();
				level.sendParticles(ParticleTypes.SOUL,from.x,from.y,from.z,0,v.x,v.y,v.z,.45);
			}
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,top.x,top.y,top.z,1+(int)(el/8),.15,.15,.15,.01);
			if(el%8==0)for(var at:candles){var p=Vec3.atCenterOf(at);level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,p.x,p.y,p.z,3,.08,.2,.08,.03);}
			if(el%10==0)sound(level,top,SoundEvents.SOUL_ESCAPE.value(),1.6F,.5F+el/(float)tell*.4F);
			if(el==tell-14)sound(level,top,SoundEvents.WARDEN_SONIC_CHARGE,1.6F,.6F);
			if(el>=tell-10 && el%2==0)ring(level,c,LanternBearerRules.RITE_RADIUS,SOUL_DUST,30);
			return;
		}
		if(el==tell){
			entityData.set(ROUSED,true);bar.setColor(BossEvent.BossBarColor.PURPLE);
			Feels.sound(level,head(),"aura_lantern_bearer_roar",2F,.8F);sound(level,c,SoundEvents.GENERIC_EXPLODE.value(),1.4F,.7F);
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,c.x,c.y+2,c.z,70,.5,1,.5,.3);
			level.sendParticles(ParticleTypes.SOUL,c.x,c.y+1,c.z,40,2,1,2,.08);
			for(var p:prey(level))if(reaches(LanternBearerRules.RITE,p)){struck.add(p.getUUID());hit(level,p,LanternBearerRules.RITE_DAMAGE,LanternBearerRules.RITE_SHOVE,c,LanternBearerRules.RITE_LIFT);}
			var w=new Wave(c,LanternBearerRules.RITE_WAVE_SPEED,LanternBearerRules.RITE_WAVE_REACH,LanternBearerRules.RITE_WAVE_DAMAGE,
				level.getGameTime()-(long)(LanternBearerRules.RITE_RADIUS/LanternBearerRules.RITE_WAVE_SPEED));
			w.struck.addAll(struck);waves.add(w);
			candles(level,true);
		}
		if(el>=tell+4){links=0;spent(LanternBearerRules.RITE_RECOVER);}
	}

	/**
	 * Lights Out, roused. Through the tell it raises its lantern to its hood and closes its hand over it; then it breathes it out. The
	 * candles die, everyone on the field is blinded a moment and left in the dark, and it is gone. Each round, after a while unseen, it
	 * stands up behind the next of them with its lantern glimmering low, turning after them until just before it swings, and reaps.
	 */
	private void lights(ServerLevel level,LivingEntity target){
		long el=since();
		still();
		if(el<LanternBearerRules.SNUFF_TELL){
			if(target!=null)aim=turned(aim,yawTo(target.position()),6);
			face(aim);
			if(el%3==0){var l=lamp();level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,l.x,l.y+.3,l.z,2,.08,.08,.08,.005);}
			return;
		}
		if(el==LanternBearerRules.SNUFF_TELL){
			var l=lamp();
			Feels.sound(level,l,"aura_lantern_bearer_fade",1.6F,.6F);
			level.sendParticles(ParticleTypes.SMOKE,l.x,l.y+.3,l.z,16,.12,.2,.12,.02);
			candles(level,false);wisps.clear();
			int dark=LanternBearerRules.LIGHTS_TICKS-LanternBearerRules.SNUFF_TELL+20;
			for(var p:prey(level)){
				p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,LanternBearerRules.LIGHTS_BLIND,0,false,false));
				p.addEffect(new MobEffectInstance(MobEffects.DARKNESS,dark,0,false,false));
			}
		}
		if(el==LanternBearerRules.SNUFF_TELL+LanternBearerRules.LIGHTS_VANISH)
			level.sendParticles(ParticleTypes.LARGE_SMOKE,getX(),getY()+1.4,getZ(),24,.5,1,.5,.02);
		if(el>=LanternBearerRules.LIGHTS_TICKS){relight(level,LanternBearerRules.LIGHTS_RECOVER);return;}
		int stage=LanternBearerRules.lightsStage(el);
		if(LanternBearerRules.lightsAppears(el)){
			var all=prey(level);
			if(all.isEmpty()){relight(level,LanternBearerRules.LIGHTS_RECOVER);return;}
			var p=all.get(lightsTurn++%all.size());
			if(!behind(level,p)){relight(level,LanternBearerRules.LIGHTS_RECOVER);return;}
			setTarget(p);aim=yawTo(p.position());face(aim);
			var l=lamp();
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,l.x,l.y,l.z,4,.05,.05,.05,.005);
			level.sendParticles(ParticleTypes.SMOKE,getX(),getY()+1.2,getZ(),10,.4,.8,.4,.01);
			Feels.sound(level,l,"aura_lantern_bearer_chain",.7F,.6F);
			return;
		}
		if(stage==LanternBearerRules.GLIMMER){
			long to=LanternBearerRules.LIGHTS_GLIMMER-(long)LanternBearerRules.lightsGlimmer(el);
			var prey=getTarget();
			if(prey!=null && to>4)aim=turned(aim,yawTo(prey.position()),12);
			face(aim);
			if(el%4==0){var l=lamp();level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,l.x,l.y,l.z,1,.04,.04,.04,.003);}
			if(to==LanternBearerRules.LIGHTS_GLIMMER/2)Feels.sound(level,head(),"aura_lantern_bearer_fade",.8F,1.3F);
		}else face(aim);
		if(LanternBearerRules.lightsStrikes(el)){
			Feels.sound(level,position().add(0,1.5,0),"aura_lantern_bearer_reap",1.4F,.8F+random.nextFloat()*.15F);
			for(int i=0;i<12;i++){
				double a=Math.toRadians(aim)+Math.toRadians(-70+i*140/11.0),r=3.6;
				level.sendParticles(ParticleTypes.SWEEP_ATTACK,getX()-Math.sin(a)*r,getY()+1.2,getZ()+Math.cos(a)*r,0,0,0,0,0);
			}
			for(var p:prey(level))if(reaches(LanternBearerRules.LIGHTS_OUT,p))hit(level,p,LanternBearerRules.LIGHTS_DAMAGE,.8,position(),.2);
		}
		if(stage==LanternBearerRules.SWING && LanternBearerRules.lightsStage(el+1)==LanternBearerRules.GONE)
			level.sendParticles(ParticleTypes.LARGE_SMOKE,getX(),getY()+1.4,getZ(),16,.4,.9,.4,.02);
	}
	/**
	 * Stand up {@link LanternBearerRules#LIGHTS_BEHIND} behind {@code p}, or failing that somewhere else round them that is open and on
	 * the field; false if there is nowhere.
	 */
	private boolean behind(ServerLevel level,ServerPlayer p){
		float back=p.getYRot()+180;
		for(int i=0;i<8;i++){
			float yaw=back+(i%2==0?1:-1)*((i+1)/2)*45;
			var f=ahead(yaw).scale(LanternBearerRules.LIGHTS_BEHIND);
			for(double up:new double[]{0,1,-1}){
				var at=new Vec3(p.getX()+f.x,Math.floor(p.getY())+up,p.getZ()+f.z);
				if(!inHall(at,0))continue;
				var box=getDimensions(getPose()).makeBoundingBox(at);
				if(!level.noCollision(this,box))continue;
				var below=BlockPos.containing(at.x,at.y-.5,at.z);
				if(level.getBlockState(below).getCollisionShape(level,below).isEmpty())continue;
				teleportTo(at.x,at.y,at.z);setDeltaMovement(Vec3.ZERO);
				return true;
			}
		}
		return false;
	}
	/** Its lantern flares back up and the candles with it, and it stands spent for {@code ticks}. */
	private void relight(ServerLevel level,int ticks){
		var l=lamp();
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,l.x,l.y,l.z,20,.25,.25,.25,.05);
		level.sendParticles(ParticleTypes.SOUL,getX(),getY()+1.4,getZ(),12,.5,.8,.5,.03);
		Feels.sound(level,l,"aura_lantern_bearer_wisp",1.2F,.7F);
		candles(level,true);
		for(var p:prey(level))p.removeEffect(MobEffects.DARKNESS);
		links=0;spent(ticks);
	}

	/** Spent after an attack, leaning on its spade, for {@code ticks}. */
	private void spent(int ticks){pose(LanternBearerRules.RECOVER,ticks);still();}
	/** Gone to smoke from a blow, drifting to one side out from under it. */
	private void fade(ServerLevel level){
		aim=getYRot();int side=random.nextBoolean()?1:-1;var f=ahead(aim);
		slide=new Vec3(-f.z*side,0,f.x*side).scale(LanternBearerRules.FADE_SPEED).add(f.scale(-.2));
		entityData.set(SIDE,side);pose(LanternBearerRules.FADE,LanternBearerRules.FADE_TICKS,LanternBearerRules.FADE_TICKS);
		Feels.sound(level,position().add(0,1.5,0),"aura_lantern_bearer_fade",1.2F,1+random.nextFloat()*.2F);
		level.sendParticles(ParticleTypes.LARGE_SMOKE,getX(),getY()+1.4,getZ(),14,.5,.9,.5,.02);
	}
	/** Hurt too fast: gone to smoke and drifting away from whoever is hurting it, back toward its grave. */
	private void leapAway(ServerLevel level,Vec3 from){
		lastLeap=level.getGameTime();stung=0;links=0;
		var away=from==null?ahead(getYRot()).scale(-1):new Vec3(getX()-from.x,0,getZ()-from.z);
		var home=spot().subtract(position()).multiply(1,0,1);
		if(home.lengthSqr()>16)away=away.normalize().add(home.normalize());
		if(away.lengthSqr()<1e-4)away=ahead(getYRot()).scale(-1);
		aim=getYRot();slide=away.normalize().scale(LanternBearerRules.LEAP_SPEED);
		var f=ahead(aim);double side=slide.x*-f.z+slide.z*f.x;
		entityData.set(SIDE,Math.abs(side)<.3?0:side>0?1:-1);pose(LanternBearerRules.FADE,LanternBearerRules.LEAP_TICKS,LanternBearerRules.LEAP_TICKS);
		Feels.sound(level,position().add(0,1.5,0),"aura_lantern_bearer_fade",1.4F,.8F);
		level.sendParticles(ParticleTypes.LARGE_SMOKE,getX(),getY()+1.4,getZ(),20,.6,1,.6,.03);
	}
	/** Standing its ground: no sliding about, though it still falls. */
	private void still(){getNavigation().stop();var v=getDeltaMovement();setDeltaMovement(0,Math.min(0,v.y),0);}
	/** Ticks before its next move: fewer roused. */
	private int gap(){return (roused()?LanternBearerRules.GAP_ROUSED:LanternBearerRules.GAP)+random.nextInt(3);}

	/** Players it could be fighting right now. */
	private List<ServerPlayer> prey(ServerLevel level){
		var out=new ArrayList<ServerPlayer>();
		for(var p:level.players())if(p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(this)<18*18 && inHall(p.position(),LanternBearerRules.LEASH))out.add(p);
		return out;
	}
	/** Whether {@code move}, along the locked aim, reaches {@code p}. */
	private boolean reaches(int move,LivingEntity p){return reaches(move,p,aim);}
	/** Whether {@code move}, facing {@code yaw}, reaches {@code p}. */
	private boolean reaches(int move,LivingEntity p,float yaw){
		var f=ahead(yaw);double dx=p.getX()-getX(),dz=p.getZ()-getZ();
		return LanternBearerRules.hits(move,dx*f.x+dz*f.z,dx*-f.z+dz*f.x,p.getY()-getY());
	}
	/** A blow landing on {@code p}, through part of its armour, knocking it {@code shove} away from {@code from} and {@code lift} up. */
	private void hit(ServerLevel level,LivingEntity p,double damage,double shove,Vec3 from,double lift){
		if(!GuardianBite.land(level,this,p,(float)damage,LanternBearerRules.PIERCE))return;
		if(shove>0)p.knockback(shove,from.x-p.getX(),from.z-p.getZ(),level.damageSources().mobAttack(this),0);
		if(lift>0)p.setDeltaMovement(p.getDeltaMovement().add(0,lift,0));
		p.needsSync=true;
	}
	private Vec3 ahead(float yaw){double r=Math.toRadians(yaw);return new Vec3(-Math.sin(r),0,Math.cos(r));}

	private float yawTo(Vec3 at){return (float)Math.toDegrees(Math.atan2(-(at.x-getX()),at.z-getZ()));}
	private void face(float yaw){setYRot(yaw);yBodyRot=yaw;yHeadRot=yaw;}
	/** {@code yaw} turned toward {@code toward} by at most {@code most} degrees. */
	private static float turned(float yaw,float toward,float most){
		float d=net.minecraft.util.Mth.wrapDegrees(toward-yaw);return yaw+Math.max(-most,Math.min(most,d));
	}
	/** Turned toward {@code yaw} by at most {@code most} degrees. */
	private void turn(float yaw,float most){face(turned(getYRot(),yaw,most));}

	@Override public boolean hurtServer(ServerLevel level,DamageSource source,float damage){
		if(source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.CRAMMING) || source.is(DamageTypes.FREEZE))return false;
		int move=move();
		if(source.is(DamageTypes.GENERIC_KILL))return super.hurtServer(level,source,damage);
		// Down in the grave, rising or sinking there is nothing yet to strike; gone to smoke, a blow passes through.
		if(LanternBearerRules.scripted(move) || move==LanternBearerRules.FADE)return false;
		// Out in the dark there is nothing there; caught glimmering, its lantern flares and the dark is broken.
		if(move==LanternBearerRules.LIGHTS_OUT){
			if(unseen())return false;
			int stage=LanternBearerRules.lightsStage(since());
			if(stage==LanternBearerRules.GLIMMER || stage==LanternBearerRules.SWING){
				boolean hurt=super.hurtServer(level,source,damage*(float)LanternBearerRules.armour(LanternBearerRules.RECOVER,false,false));
				if(hurt){lastCut=level.getGameTime();relight(level,LanternBearerRules.LIGHTS_BROKEN);}
				return hurt;
			}
		}
		long now=level.getGameTime();
		var at=source.getSourcePosition();
		boolean frontal=at!=null && LanternBearerRules.frontal(yawTo(at)-getYRot());
		// It sees a blow coming and goes to smoke out from under it.
		if(source.getDirectEntity()!=null && source.getDirectEntity()==source.getEntity() && LanternBearerRules.dodges(move,now-lastDodge,random.nextDouble())){
			lastDodge=now;fade(level);
			return false;
		}
		damage*=(float)LanternBearerRules.armour(move,frontal,LanternBearerRules.open(move,since(),tell(),roused()));
		boolean hurt=super.hurtServer(level,source,damage);
		if(hurt){
			lastCut=now;stung+=damage;
			if(LanternBearerRules.leaps(move(),stung,now-lastLeap))leapAway(level,source.getSourcePosition());
		}
		return hurt;
	}
	@Override public void die(DamageSource source){
		if(level() instanceof ServerLevel server){
			Feels.sound(server,head(),"aura_lantern_bearer_death",1.8F,1);Guardians.reward(server,this,LanternBearerRules.TIER,bar,source);bar.removeAllPlayers();wisps.clear();marks.clear();waves.clear();clods.clear();
			server.sendParticles(ParticleTypes.SOUL,getX(),getY()+1.6,getZ(),50,.8,1.2,.8,.05);
			server.sendParticles(ParticleTypes.LARGE_SMOKE,getX(),getY()+1.6,getZ(),30,.6,1.2,.6,.03);
			candles(server,false);lantern(server,false);
		}
		super.die(source);
	}
	@Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return null;}
	@Override protected void actuallyHurt(ServerLevel level,DamageSource source,float damage){
		super.actuallyHurt(level,source,damage);
		if(random.nextInt(3)==0)Feels.sound(level,head(),"aura_lantern_bearer_hurt",1.2F,.9F+random.nextFloat()*.2F);
	}
	@Override public void remove(RemovalReason reason){bar.removeAllPlayers();super.remove(reason);}
	@Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);bar.removePlayer(player);}

	@Override protected void addAdditionalSaveData(ValueOutput out){
		super.addAdditionalSaveData(out);
		out.putBoolean("bearer_bound",bound);out.store("bearer_grave",BlockPos.CODEC,grave);out.putFloat("bearer_rest",rest);
		out.store("bearer_hall_min",BlockPos.CODEC,hallMin);out.store("bearer_hall_max",BlockPos.CODEC,hallMax);
		out.store("bearer_hook",BlockPos.CODEC,hook);out.store("bearer_candles",BlockPos.CODEC.listOf(),candles);
		out.putBoolean("bearer_awake",!LanternBearerRules.scripted(move()));out.putLong("bearer_ticked",lastTick);
		out.putBoolean("bearer_roused",roused());
	}
	@Override protected void readAdditionalSaveData(ValueInput in){
		super.readAdditionalSaveData(in);
		bound=in.getBooleanOr("bearer_bound",false);
		grave=in.read("bearer_grave",BlockPos.CODEC).orElse(blockPosition());rest=in.getFloatOr("bearer_rest",getYRot());
		hallMin=in.read("bearer_hall_min",BlockPos.CODEC).orElse(grave);hallMax=in.read("bearer_hall_max",BlockPos.CODEC).orElse(grave);
		hook=in.read("bearer_hook",BlockPos.CODEC).orElse(grave.above(5));
		candles=new ArrayList<>(in.read("bearer_candles",BlockPos.CODEC.listOf()).orElse(List.of()));
		lastTick=in.getLongOr("bearer_ticked",-1L);
		// Mid-fight it comes back up, whatever it was doing when the chunk unloaded, roused still if it had held its rite.
		boolean awake=in.getBooleanOr("bearer_awake",false),roused=awake && in.getBooleanOr("bearer_roused",false);
		entityData.set(ROUSED,roused);bar.setColor(roused?BossEvent.BossBarColor.PURPLE:BossEvent.BossBarColor.BLUE);
		if(awake){pose(LanternBearerRules.WATCH,LanternBearerRules.GAP);lastCut=0;}else pose(LanternBearerRules.DORMANT,0);
	}
}
