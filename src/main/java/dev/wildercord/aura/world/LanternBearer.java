package dev.wildercord.aura.world;

import dev.wildercord.cast.feel.Feels;
import dev.wildercord.world.dungeons.BarrowFieldPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
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
 * Up, it reaps with its spade at whoever is in front of it, drives the spade down among anyone crowding it, hooks anyone keeping away
 * on its crook and drags them in, and looses soul wisps from its lantern after anyone who runs. Some blows it goes to smoke and drifts
 * out from under, and hurt too fast it drifts away across the field. Wounded, now and then it puts its lantern out
 * ({@link LanternBearerRules#LIGHTS_OUT}) and comes for them out of the dark.
 */
public final class LanternBearer extends PathfinderMob implements Enemy {
	private static final EntityDataAccessor<Integer> MOVE=SynchedEntityData.defineId(LanternBearer.class,EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN=SynchedEntityData.defineId(LanternBearer.class,EntityDataSerializers.LONG);
	/** The current move's tell in ticks, so the client knows when the blow lands. */
	private static final EntityDataAccessor<Integer> TELL=SynchedEntityData.defineId(LanternBearer.class,EntityDataSerializers.INT);
	/** Which way a fade drifts, for the client: -1 left, 1 right, 0 straight back. */
	private static final EntityDataAccessor<Integer> SIDE=SynchedEntityData.defineId(LanternBearer.class,EntityDataSerializers.INT);
	private static final EntityDimensions LYING=EntityDimensions.scalable(1.4F,0.4F).withEyeHeight(0.2F),STANDING=EntityDimensions.scalable(1.4F,3.4F).withEyeHeight(3.0F);
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
	private long lastDodge=-999,lastLeap=-999,lastHook=-999,lastWisps=-999,lastSlam=-999,lastLights=-9999;
	/** Lights Out: whom it last stood up behind, so each round it comes for the next of them. */
	private int lightsTurn;
	/** Who the crook caught this time, being dragged in. */
	private final Set<UUID> hooked=new HashSet<>();
	/** Soul wisps loosed from its lantern, drifting after their prey; not saved, they burn out soon enough. */
	private final List<Wisp> wisps=new ArrayList<>();
	/** Hurt taken lately, fading; enough of it sends it drifting away. */
	private double stung;

	private static final class Wisp{Vec3 at,going;UUID prey;int age;Wisp(Vec3 at,Vec3 going,UUID prey){this.at=at;this.going=going;this.prey=prey;}}

	public LanternBearer(EntityType<? extends LanternBearer> type,Level level){super(type,level);xpReward=80;setPersistenceRequired();}
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
		goDown();
	}
	/** Client side, for blending one move into the next: the move last drawn, the one before it, when it changed, and how far into it the last one was. */
	public int shownMove=-1,shownTell,prevMove,prevTell;public float shownElapsed,prevElapsed,switchedAt=-100;
	public int move(){return entityData.get(MOVE);}
	public int tell(){return entityData.get(TELL);}
	public int side(){return entityData.get(SIDE);}
	public float elapsed(float partial){return (float)(level().getGameTime()-entityData.get(BEGAN))+partial;}
	private long since(){return level().getGameTime()-entityData.get(BEGAN);}
	private Vec3 spot(){return Vec3.atBottomCenterOf(grave);}

	@Override protected void defineSynchedData(SynchedEntityData.Builder b){
		super.defineSynchedData(b);b.define(MOVE,LanternBearerRules.DORMANT);b.define(BEGAN,0L);b.define(TELL,0);b.define(SIDE,0);
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
	/** Down in its grave again, whole, the lantern back on its chain and the candles out. */
	private void goDown(){
		pose(LanternBearerRules.DORMANT,0);hold();setHealth(getMaxHealth());setTarget(null);bar.removeAllPlayers();quiet=0;light=0;wisps.clear();hooked.clear();
		if(level() instanceof ServerLevel server){lantern(server,false);candles(server,false);}
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
		drift(level);
		if(tickCount%4==0)smoke(level);
	}

	/** Grave-damp smoke rising off its rags, and its lantern guttering; down in the grave, a soul now and then drifting up out of it. */
	private void smoke(ServerLevel level){
		int move=move();
		if(move==LanternBearerRules.DORMANT){if(random.nextInt(12)==0)level.sendParticles(ParticleTypes.SOUL,getX(),getY()+.2,getZ(),1,.4,.1,.6,.01);return;}
		if(random.nextInt(2)==0)level.sendParticles(ParticleTypes.SMOKE,getX(),getY()+.3,getZ(),1,.5,.1,.5,.005);
		if(LanternBearerRules.holdsLantern(LanternBearerRules.rise(move,since())) && random.nextInt(2)==0){var l=lamp();level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,l.x,l.y,l.z,1,.05,.05,.05,.005);}
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
		return position().add(f.scale(1.1)).add(r.scale(.9)).add(0,1.7,0);
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
		pose(LanternBearerRules.WATCH,LanternBearerRules.GAP);hold();lantern(level,true);
		lastCut=level.getGameTime();quiet=0;light=0;
		level.sendParticles(ParticleTypes.SOUL,getX(),getY()+.2,getZ(),16,1,.1,1,.03);
	}
	/** Back down into the grave, for the field going quiet or the dawn. */
	private void sink(ServerLevel level){
		bar.removeAllPlayers();wisps.clear();hooked.clear();pose(LanternBearerRules.BED,LanternBearerRules.RISE_TICKS);hold();
		Feels.sound(level,head(),"aura_lantern_bearer_rise",1.2F,.8F);candles(level,false);
	}
	private void awake(ServerLevel level){
		if(!inHall(position(),LanternBearerRules.LEASH)){
			var at=spot();teleportTo(at.x,at.y,at.z);setDeltaMovement(Vec3.ZERO);getNavigation().stop();pose(LanternBearerRules.WATCH,LanternBearerRules.GAP);
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
		switch(move()){
			case LanternBearerRules.REAP->reap(level);
			case LanternBearerRules.HOOK->hook(level,target);
			case LanternBearerRules.WISPS->loose(level,target);
			case LanternBearerRules.SLAM->slam(level);
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
		if(left>0)left--;
		if(left<=0){
			long now=level.getGameTime();
			if(LanternBearerRules.lightsOut(wounded(),now-lastLights)){
				lastLights=now;lightsTurn=random.nextInt(4);aim=yawTo(target.position());pose(LanternBearerRules.LIGHTS_OUT,0,LanternBearerRules.SNUFF_TELL);
				Feels.sound(level,lamp(),"aura_lantern_bearer_chain",1.2F,.7F);
				return;
			}
			int pick=LanternBearerRules.choose(d,yawTo(target.position())-getYRot(),random.nextDouble(),now-lastHook>=LanternBearerRules.HOOK_REST,
				now-lastWisps>=LanternBearerRules.WISP_REST,now-lastSlam>=LanternBearerRules.SLAM_REST);
			if(pick>=0){
				aim=yawTo(target.position());
				switch(pick){
					case LanternBearerRules.SLAM->{lastSlam=now;pose(LanternBearerRules.SLAM,0,LanternBearerRules.SLAM_TELL);Feels.sound(level,head(),"aura_lantern_bearer_fade",1,.7F);}
					case LanternBearerRules.HOOK->{lastHook=now;hooked.clear();pose(LanternBearerRules.HOOK,0,LanternBearerRules.HOOK_TELL);Feels.sound(level,lamp(),"aura_lantern_bearer_chain",1.1F,.9F);}
					case LanternBearerRules.WISPS->{lastWisps=now;pose(LanternBearerRules.WISPS,0,LanternBearerRules.WISP_TELL);Feels.sound(level,lamp(),"aura_lantern_bearer_wisp",1.2F,.8F);}
					default->{pose(LanternBearerRules.REAP,0,LanternBearerRules.REAP_TELL);Feels.sound(level,head(),"aura_lantern_bearer_fade",.8F,1.2F);}
				}
				return;
			}
		}
		if(d>LanternBearerRules.CLOSE){
			if(!getNavigation().moveTo(target,1))getMoveControl().setWantedPosition(target.getX(),target.getY(),target.getZ(),1);
		}else{getNavigation().stop();turn(yawTo(target.position()),9);}
	}
	/** The spade drawn back over its right shoulder through the tell, then swung round flat through everyone in front of it. */
	private void reap(ServerLevel level){
		face(aim);still();
		long el=since();int tell=tell();
		if(el==tell){
			Feels.sound(level,position().add(0,1.5,0),"aura_lantern_bearer_reap",1.4F,.9F+random.nextFloat()*.2F);
			for(int i=0;i<12;i++){
				double a=Math.toRadians(aim)+Math.toRadians(-70+i*140/11.0),r=3.6;
				level.sendParticles(ParticleTypes.SWEEP_ATTACK,getX()-Math.sin(a)*r,getY()+1.2,getZ()+Math.cos(a)*r,0,0,0,0,0);
			}
			for(var p:prey(level))if(reaches(LanternBearerRules.REAP,p))hit(level,p,LanternBearerRules.REAP_DAMAGE,.8,position(),.2);
		}
		if(el>=tell+2)spent(LanternBearerRules.REAP_RECOVER);
	}
	/**
	 * The crook drawn back through the tell, its aim locked for the last three ticks; then thrust out along a line. Whoever it catches
	 * is struck and dragged in close, the chain of soul light between them, for {@link LanternBearerRules#HOOK_DRAG} ticks.
	 */
	private void hook(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell-3 && target!=null)aim=yawTo(target.position());
		face(aim);still();
		if(el<tell)return;
		var f=ahead(aim);
		if(el==tell){
			Feels.sound(level,lamp(),"aura_lantern_bearer_hook",1.4F,1);
			double reach=LanternBearerRules.HOOK_REACH;
			for(var p:prey(level)){
				if(!reaches(LanternBearerRules.HOOK,p))continue;
				hooked.add(p.getUUID());hit(level,p,LanternBearerRules.HOOK_DAMAGE,0,position(),0);
				reach=Math.min(reach,distanceTo(p));
			}
			for(double d=1;d<=reach;d+=.5)level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,getX()+f.x*d,getY()+1.6,getZ()+f.z*d,1,.02,.02,.02,0);
		}
		long drag=el-tell;
		if(drag>0 && drag<=LanternBearerRules.HOOK_DRAG){
			if(drag%3==1 && !hooked.isEmpty())Feels.sound(level,position().add(0,1.5,0),"aura_lantern_bearer_chain",1,1.1F);
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
		if(drag>=LanternBearerRules.HOOK_DRAG){hooked.clear();spent(LanternBearerRules.HOOK_RECOVER);}
	}
	/** The lantern raised and swung through the tell, then its wisps loosed, one after each player in turn. */
	private void loose(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(target!=null && el<tell)aim=turned(aim,yawTo(target.position()),10);
		face(aim);still();
		var l=lamp();
		if(el<tell){if(el%3==0)level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,l.x,l.y,l.z,3,.2,.2,.2,.02);return;}
		if(el==tell){
			var targets=prey(level);if(targets.isEmpty() && target instanceof ServerPlayer p)targets.add(p);
			if(!targets.isEmpty()){
				var f=ahead(aim);
				for(int i=0;i<LanternBearerRules.WISP_COUNT;i++){
					double spread=(i-(LanternBearerRules.WISP_COUNT-1)/2.0)*.9;
					var going=new Vec3(f.x-f.z*spread,.5,f.z+f.x*spread).normalize().scale(LanternBearerRules.WISP_SPEED*1.5);
					wisps.add(new Wisp(l.add(0,.6,0),going,targets.get(i%targets.size()).getUUID()));
				}
			}
			Feels.sound(level,l,"aura_lantern_bearer_wisp",1.4F,1.1F);
			level.sendParticles(ParticleTypes.SOUL,l.x,l.y,l.z,10,.3,.3,.3,.05);
		}
		if(el>=tell+4)spent(LanternBearerRules.WISP_RECOVER);
	}
	/** The wisps it has loosed, drifting after their prey and bursting on them, on whatever they fly into, or when they burn out. */
	private void drift(ServerLevel level){
		for(var it=wisps.iterator();it.hasNext();){
			var w=it.next();w.age++;
			var p=level.getPlayerByUUID(w.prey);
			boolean gone=p==null || !p.isAlive() || p.isSpectator() || p.level()!=level;
			if(!gone){
				var to=p.position().add(0,p.getBbHeight()*.6,0).subtract(w.at);
				var want=to.normalize().scale(LanternBearerRules.WISP_SPEED);
				w.going=w.going.add(want.subtract(w.going).scale(LanternBearerRules.WISP_TURN));
			}
			w.at=w.at.add(w.going);
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,w.at.x,w.at.y,w.at.z,2,.05,.05,.05,.005);
			if(w.age%3==0)level.sendParticles(ParticleTypes.SOUL,w.at.x,w.at.y,w.at.z,1,.05,.05,.05,.005);
			var block=BlockPos.containing(w.at);
			boolean struck=!level.getBlockState(block).getCollisionShape(level,block).isEmpty();
			boolean landed=!gone && LanternBearerRules.bursts(p.position().add(0,p.getBbHeight()*.6,0).distanceTo(w.at));
			if(landed || struck || gone || w.age>=LanternBearerRules.WISP_LIFE){
				it.remove();
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,w.at.x,w.at.y,w.at.z,10,.2,.2,.2,.06);
				Feels.sound(level,w.at,"aura_lantern_bearer_burst",1,1+random.nextFloat()*.2F);
				if(landed && GuardianBite.land(level,this,p,(float)LanternBearerRules.WISP_DAMAGE,LanternBearerRules.PIERCE))
					p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,LanternBearerRules.WISP_SLOW,1,false,true));
			}
		}
	}
	/** The spade raised high in both hands through the tell, then driven down into the ground, throwing back everyone round it. */
	private void slam(ServerLevel level){
		face(aim);still();
		long el=since();int tell=tell();
		if(el==tell){
			var at=position().add(ahead(aim).scale(.8));
			Feels.sound(level,at,"aura_lantern_bearer_slam",1.6F,.9F+random.nextFloat()*.15F);
			var dirt=new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,Blocks.COARSE_DIRT.defaultBlockState());
			level.sendParticles(dirt,at.x,at.y+.2,at.z,30,1,.2,1,.1);
			for(int i=0;i<20;i++){double a=i*Math.PI*2/20,r=LanternBearerRules.SLAM_RADIUS*.8;level.sendParticles(ParticleTypes.SOUL,getX()+Math.cos(a)*r,getY()+.2,getZ()+Math.sin(a)*r,1,.1,0,.1,.02);}
			for(var p:prey(level))if(reaches(LanternBearerRules.SLAM,p))hit(level,p,LanternBearerRules.SLAM_DAMAGE,LanternBearerRules.SLAM_SHOVE,position(),.45);
		}
		if(el>=tell+3)spent(LanternBearerRules.SLAM_RECOVER);
	}

	/**
	 * Lights Out. Through the tell it raises its lantern to its hood and closes its hand over it; then it breathes it out. The candles
	 * die, everyone on the field is blinded a moment and left in the dark, and it is gone. Each round, after a while unseen, it stands up
	 * behind the next of them with its lantern glimmering low, turning after them until just before it swings, and reaps.
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
			for(var p:prey(level))if(reaches(LanternBearerRules.REAP,p))hit(level,p,LanternBearerRules.LIGHTS_DAMAGE,.8,position(),.2);
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
		spent(ticks);
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
		lastLeap=level.getGameTime();stung=0;
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
	private boolean wounded(){return LanternBearerRules.wounded(getHealth(),getMaxHealth());}
	/** Ticks before its next blow. */
	private int gap(){return (wounded()?LanternBearerRules.GAP_WOUNDED:LanternBearerRules.GAP)+random.nextInt(3);}

	/** Players it could be fighting right now. */
	private List<ServerPlayer> prey(ServerLevel level){
		var out=new ArrayList<ServerPlayer>();
		for(var p:level.players())if(p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(this)<18*18 && inHall(p.position(),LanternBearerRules.LEASH))out.add(p);
		return out;
	}
	/** Whether {@code move}, along the locked aim, reaches {@code p}. */
	private boolean reaches(int move,LivingEntity p){
		var f=ahead(aim);double dx=p.getX()-getX(),dz=p.getZ()-getZ();
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
				boolean hurt=super.hurtServer(level,source,damage*(float)LanternBearerRules.armour(LanternBearerRules.RECOVER,false));
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
		damage*=(float)LanternBearerRules.armour(move,frontal);
		boolean hurt=super.hurtServer(level,source,damage);
		if(hurt){
			lastCut=now;stung+=damage;
			if(LanternBearerRules.leaps(move(),stung,now-lastLeap))leapAway(level,source.getSourcePosition());
		}
		return hurt;
	}
	@Override public void die(DamageSource source){
		if(level() instanceof ServerLevel server){
			Feels.sound(server,head(),"aura_lantern_bearer_death",1.8F,1);bar.removeAllPlayers();wisps.clear();
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
	}
	@Override protected void readAdditionalSaveData(ValueInput in){
		super.readAdditionalSaveData(in);
		bound=in.getBooleanOr("bearer_bound",false);
		grave=in.read("bearer_grave",BlockPos.CODEC).orElse(blockPosition());rest=in.getFloatOr("bearer_rest",getYRot());
		hallMin=in.read("bearer_hall_min",BlockPos.CODEC).orElse(grave);hallMax=in.read("bearer_hall_max",BlockPos.CODEC).orElse(grave);
		hook=in.read("bearer_hook",BlockPos.CODEC).orElse(grave.above(5));
		candles=new ArrayList<>(in.read("bearer_candles",BlockPos.CODEC.listOf()).orElse(List.of()));
		lastTick=in.getLongOr("bearer_ticked",-1L);
		// Mid-fight it comes back up, whatever it was doing when the chunk unloaded.
		if(in.getBooleanOr("bearer_awake",false)){pose(LanternBearerRules.WATCH,LanternBearerRules.GAP);lastCut=0;}else pose(LanternBearerRules.DORMANT,0);
	}
}
