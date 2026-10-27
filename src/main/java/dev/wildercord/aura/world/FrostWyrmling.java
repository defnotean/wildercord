package dev.wildercord.aura.world;

import dev.wildercord.aura.arts.ArtBlocks;
import dev.wildercord.cast.BlockFx;
import dev.wildercord.cast.MoveDeck;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.feel.Feels;
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
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;

/**
 * The Frost Wyrmling ({@link FrostWyrmlingRules}). Generation leaves it curled up on the snow mound at the back of the Glacier Den's
 * hall, bound to that spot and to the hall, facing the crevasse. Curled up it cannot be hurt; the first player far enough into the
 * hall (or the first blow) wakes it, and it uncurls, gets its legs under it and throws its wings wide with a roar, and then the boss
 * bar comes up. The hall going quiet sends it back to its mound, healed. The hall is its own: anything hostile in it, it freezes
 * where it stands.
 * <p>
 * On its feet it never makes the same move twice running, and each has its own tell. Neck coiled back in an S, it lunges and snaps.
 * Looking back over its shoulder with its tail raised, it whips the tail round low through everything but its front. Wings mantled
 * forward, it beats up gale after gale of snow that shoves everyone before it. Head drawn high and chest aglow, it raises pillars of
 * ice by its prey and sweeps its breath across the hall. Rump wiggling in a crouch, it bounds after its prey in a string of hops.
 * It beats up under the dome, draws a frost line on the floor and dives down it, skidding into a sprawl. It breathes a glazed strip
 * along the floor, turns its back and toboggans tail first down it. It roars at the dome and the icicles come down on frost marks.
 * It hops aside out from under some blows, and hurt too fast it beats away back across the hall.
 * <p>
 * Half beaten it wraps itself in its wings in a shell of ice that closes over it, bursts out in a ring of frost, and fights on
 * fierce: quicker, its moves running into one another, each a little worse, and its lances unlocked.
 */
public final class FrostWyrmling extends PathfinderMob implements Enemy {
	private static final EntityDataAccessor<Integer> MOVE=SynchedEntityData.defineId(FrostWyrmling.class,EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN=SynchedEntityData.defineId(FrostWyrmling.class,EntityDataSerializers.LONG);
	/** The current move's tell in ticks, so the client knows when the blow lands. */
	private static final EntityDataAccessor<Integer> TELL=SynchedEntityData.defineId(FrostWyrmling.class,EntityDataSerializers.INT);
	/** Which way a bound, a lash, a breath or a sprawl goes, for the client: -1 left, 1 right, 0 straight. */
	private static final EntityDataAccessor<Integer> SIDE=SynchedEntityData.defineId(FrostWyrmling.class,EntityDataSerializers.INT);
	private static final EntityDimensions CURLED=EntityDimensions.scalable(2.8F,1.6F).withEyeHeight(1.2F),STANDING=EntityDimensions.scalable(2.8F,3.0F).withEyeHeight(2.6F);
	private static final DustParticleOptions FROST=new DustParticleOptions(0x9FDFFF,1.2F),WHITE=new DustParticleOptions(0xFFFFFF,1.5F),PALE=new DustParticleOptions(0xCFEFFF,.9F);
	private final ServerBossEvent bar=new ServerBossEvent(getUUID(),getType().getDescription(),BossEvent.BossBarColor.BLUE,BossEvent.BossBarOverlay.NOTCHED_10);
	private boolean bound;
	private BlockPos mound=BlockPos.ZERO,hallMin=BlockPos.ZERO,hallMax=BlockPos.ZERO;
	private float rest;
	private int left,quiet;
	private long lastCut;
	/** The game time it last ran, so it knows how long the hall was left alone. */
	private long lastTick=-1;
	/** The line its blow was locked on, and where a bound carries it. */
	private float aim;
	private Vec3 slide=Vec3.ZERO;
	/** A hop or a dive: where it left from and where it comes down; and the line it skids or toboggans along. */
	private Vec3 from=Vec3.ZERO,to=Vec3.ZERO,glide=Vec3.ZERO;
	private int chased;
	private long lastDodge=-999,lastLeap=-999;
	/** When it last made each move, the moves it has made lately, and how many it has run together. */
	private final long[] lastMade=new long[32];
	{Arrays.fill(lastMade,-9999);}
	private final MoveDeck deck=new MoveDeck(FrostWyrmlingRules.MEMORY);
	private int links;
	/** Half beaten and out of its shell: quicker, and its moves run into one another. */
	private boolean fierce;
	/** Whoever this blow has already struck. */
	private final Set<UUID> struck=new HashSet<>();
	/** The ice pillars it raised for its breath. */
	private final List<Vec3> pillars=new ArrayList<>();
	/** Rime Lances: where its forefeet came down, the yaws of the lines it sent out, and who a lance has already flung this time. */
	private Vec3 slam=Vec3.ZERO;
	private float[] lanes=new float[0];
	private final Set<UUID> lanced=new HashSet<>();
	/** Hurt taken lately, fading; enough of it sends it beating away. */
	private double stung;
	/** A ring of frost running out along the floor from where it burst its shell. */
	private static final class Wave{final double x,y,z,speed,max,damage,lift;final long born;final Set<UUID> struck=new HashSet<>();
		Wave(Vec3 at,double speed,double max,double damage,double lift,long born){x=at.x;y=at.y;z=at.z;this.speed=speed;this.max=max;this.damage=damage;this.lift=lift;this.born=born;}}
	private final List<Wave> waves=new ArrayList<>();
	/** An icicle shaken loose from the dome: the frost mark it falls on, when it was marked and when it lands, the dome over it, and the ice itself once it falls. */
	private static final class Icicle{final Vec3 at;final long marked,drop;final double top;Display.BlockDisplay ice;
		Icicle(Vec3 at,long marked,long drop,double top){this.at=at;this.marked=marked;this.drop=drop;this.top=top;}}
	private final List<Icicle> icicles=new ArrayList<>();

	public FrostWyrmling(EntityType<? extends FrostWyrmling> type,Level level){super(type,level);xpReward=FrostWyrmlingRules.TIER.xp;setPersistenceRequired();}
	public static AttributeSupplier.Builder createAttributes(){
		return createMobAttributes().add(Attributes.MAX_HEALTH,FrostWyrmlingRules.HEALTH).add(Attributes.ARMOR,FrostWyrmlingRules.ARMOUR).add(Attributes.ARMOR_TOUGHNESS,FrostWyrmlingRules.TIER.toughness)
			.add(Attributes.MOVEMENT_SPEED,FrostWyrmlingRules.SPEED).add(Attributes.ATTACK_DAMAGE,FrostWyrmlingRules.TIER.melee).add(Attributes.FOLLOW_RANGE,40).add(Attributes.KNOCKBACK_RESISTANCE,1)
			.add(Attributes.STEP_HEIGHT,1.1);
	}

	/** Bind it to its mound: {@code at} the air above the snow where it lies, {@code yaw} the way it faces, {@code hall} the hall it keeps. */
	public void bind(BlockPos at,float yaw,BoundingBox hall){
		bound=true;mound=at.immutable();rest=yaw;
		hallMin=new BlockPos(hall.minX(),hall.minY(),hall.minZ());hallMax=new BlockPos(hall.maxX(),hall.maxY(),hall.maxZ());
		curlUp();
	}
	/** Client side, for blending one move into the next: the move last drawn, the one before it, when it changed, and how far into it the last one was. */
	public int shownMove=-1,shownTell,prevMove,prevTell;public float shownElapsed,prevElapsed,switchedAt=-100;
	public int move(){return entityData.get(MOVE);}
	public int tell(){return entityData.get(TELL);}
	public int side(){return entityData.get(SIDE);}
	public float elapsed(float partial){return (float)(level().getGameTime()-entityData.get(BEGAN))+partial;}
	private long since(){return level().getGameTime()-entityData.get(BEGAN);}
	private Vec3 spot(){return Vec3.atBottomCenterOf(mound);}

	@Override protected void defineSynchedData(SynchedEntityData.Builder b){
		super.defineSynchedData(b);b.define(MOVE,FrostWyrmlingRules.DORMANT);b.define(BEGAN,0L);b.define(TELL,0);b.define(SIDE,0);
	}
	@Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(MOVE.equals(key))refreshDimensions();}
	@Override protected EntityDimensions getDefaultDimensions(Pose pose){return move()==FrostWyrmlingRules.DORMANT?CURLED:STANDING;}
	@Override protected void registerGoals(){}
	@Override public boolean isPushable(){return false;}
	@Override protected void doPush(Entity entity){}
	@Override public boolean removeWhenFarAway(double distance){return false;}
	@Override public boolean canFreeze(){return false;}

	private void pose(int move,int ticks){pose(move,ticks,0);}
	private void pose(int move,int ticks,int tell){
		int was=move();entityData.set(MOVE,move);entityData.set(BEGAN,level().getGameTime());entityData.set(TELL,tell);left=ticks;getNavigation().stop();
		if(was!=move)refreshDimensions();
		// Only a dive keeps it up on its wings.
		if(move!=FrostWyrmlingRules.DIVE && isNoGravity())setNoGravity(false);
		if(!FrostWyrmlingRules.scripted(move))bar.setName(Component.translatable("boss.wildercord.frost_wyrmling.status",getType().getDescription(),
			Component.translatable("boss.wildercord.frost_wyrmling.move."+FrostWyrmlingRules.key(move))));
	}
	/** Curled up again on its mound, whole, and calm again. */
	private void curlUp(){
		pose(FrostWyrmlingRules.DORMANT,0);hold();setHealth(getMaxHealth());setTarget(null);bar.removeAllPlayers();quiet=0;
		fierce=false;bar.setColor(BossEvent.BossBarColor.BLUE);deck.clear();Arrays.fill(lastMade,-9999);links=0;waves.clear();thaw();pillars.clear();
	}
	/** The icicles still hanging or falling, gone. */
	private void thaw(){for(var i:icicles)if(i.ice!=null && !i.ice.isRemoved())i.ice.discard();icicles.clear();}
	private void sound(ServerLevel level,Vec3 at,SoundEvent s,float volume,float pitch){level.playSound(null,at.x,at.y,at.z,s,SoundSource.HOSTILE,volume,pitch);}
	/** Pinned to its mound while it lies there (or uncurls, or curls up). */
	private void hold(){
		var t=spot();setPos(t.x,t.y,t.z);setDeltaMovement(Vec3.ZERO);face(rest);
	}
	private boolean inHall(Vec3 at,double margin){
		return FrostWyrmlingRules.within(at.x,at.y,at.z,hallMin.getX(),hallMin.getY(),hallMin.getZ(),hallMax.getX(),hallMax.getY(),hallMax.getZ(),margin);
	}

	@Override protected void customServerAiStep(ServerLevel level){
		super.customServerAiStep(level);if(!isAlive())return;
		if(!bound){var b=blockPosition();bind(b,getYRot(),new BoundingBox(b.getX()-17,b.getY()-3,b.getZ()-17,b.getX()+17,b.getY()+13,b.getZ()+17));}
		// Left alone long enough (its prey died and went home, say) and it has curled up again in the meantime.
		long now=level.getGameTime();
		if(lastTick>=0 && move()!=FrostWyrmlingRules.DORMANT && FrostWyrmlingRules.forgets(now-lastTick))curlUp();
		lastTick=now;
		if(tickCount%FrostWyrmlingRules.GUARD_EVERY==0 && Guardians.enabled())guard(level);
		switch(move()){
			case FrostWyrmlingRules.DORMANT->dormant(level);
			case FrostWyrmlingRules.RISE->{hold();if(--left<=0)stand(level);else if(left==FrostWyrmlingRules.RISE_TICKS-FrostWyrmlingRules.ROAR_AT)roar(level,1.6F,1);}
			case FrostWyrmlingRules.BED->{hold();if(wakeful(level)!=null)wake(level,FrostWyrmlingRules.RISE_TICKS-left);else if(--left<=0)curlUp();}
			default->awake(level);
		}
		if(tickCount%4==0)rime(level);
	}

	/** Frost smokes off it whatever it is doing; on its feet, snow sifts off its back and wings. */
	private void rime(ServerLevel level){
		if(FrostWyrmlingRules.scripted(move())){if(random.nextInt(3)==0)level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+1.2,getZ(),1,.8,.3,.8,.005);return;}
		if(random.nextInt(2)==0)level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+2.2,getZ(),2,.9,.4,1.2,.01);
		if(wounded() && random.nextInt(2)==0){var h=head();level.sendParticles(ParticleTypes.CLOUD,h.x,h.y,h.z,1,.15,.05,.15,.005);}
	}
	/** Its roar: the sound, frost breathed out ahead, and snow shaken off its wings. */
	private void roar(ServerLevel level,float volume,float pitch){
		var h=head();
		Feels.sound(level,h,"aura_frost_wyrmling_roar",volume,pitch);
		var f=ahead(getYRot());
		for(int i=0;i<16;i++)level.sendParticles(ParticleTypes.SNOWFLAKE,h.x,h.y,h.z,0,f.x+(random.nextDouble()-.5)*.6,.1+random.nextDouble()*.2,f.z+(random.nextDouble()-.5)*.6,.35);
		level.sendParticles(ParticleTypes.CLOUD,h.x+f.x,h.y,h.z+f.z,10,.4,.2,.4,.03);
		level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+2.4,getZ(),40,2.4,.6,1.2,.05);
	}
	/** Its head, more or less: up on its neck on its feet, tucked low against its tail when curled. */
	private Vec3 head(){
		boolean down=move()==FrostWyrmlingRules.DORMANT;var f=ahead(getYRot());
		return position().add(f.scale(down?1.2:2.6)).add(0,down?.9:2.6,0);
	}

	/**
	 * The hall is its own: anything hostile that wanders or spawns in, curled up or standing, it freezes solid where it stands, one
	 * every {@link FrostWyrmlingRules#GUARD_EVERY} ticks, nearest first. Only players get a fight.
	 */
	private void guard(ServerLevel level){
		var box=new AABB(Vec3.atLowerCornerOf(hallMin),Vec3.atLowerCornerOf(hallMax).add(1,1,1));
		var h=head();
		var m=level.getEntitiesOfClass(Mob.class,box,e->e instanceof Enemy && e!=this && e.isAlive() && !(e instanceof FrostWyrmling) && inHall(e.position(),0))
			.stream().min(Comparator.comparingDouble(e->e.distanceToSqr(h))).orElse(null);
		if(m==null)return;
		var to=m.position().add(0,m.getBbHeight()/2,0);
		var step=to.subtract(h);int n=Math.max(2,(int)(step.length()*2));
		for(int i=0;i<=n;i++){var at=h.add(step.scale(i/(double)n));level.sendParticles(ParticleTypes.SNOWFLAKE,at.x,at.y,at.z,1,.05,.05,.05,0);}
		level.sendParticles(ParticleTypes.ITEM_SNOWBALL,to.x,to.y,to.z,12,.3,.4,.3,.05);
		level.sendParticles(ParticleTypes.CLOUD,to.x,to.y,to.z,8,.3,.4,.3,.02);
		Feels.sound(level,to,"aura_frost_wyrmling_frost",.8F,1.2F+random.nextFloat()*.2F);
		m.hurtServer(level,damageSources().freeze(),Float.MAX_VALUE);
		if(m.isAlive())m.kill(level);
	}

	/** The nearest player in the hall close enough to its mound to wake it, if any. */
	private ServerPlayer wakeful(ServerLevel level){
		var t=spot();
		return level.players().stream().filter(p->p.isAlive() && !p.isCreative() && !p.isSpectator() && inHall(p.position(),0)
			&& FrostWyrmlingRules.wakes(p.getX()-t.x,p.getY()-t.y,p.getZ()-t.z)).min(Comparator.comparingDouble(p->p.distanceToSqr(t))).orElse(null);
	}

	private void dormant(ServerLevel level){
		hold();if(getHealth()<getMaxHealth())setHealth(getMaxHealth());
		if(tickCount%5==0 && Guardians.enabled() && wakeful(level)!=null)wake(level,0);
		else if(random.nextInt(100)==0){
			// A sleeping breath, a little cloud of frost puffed out over the snow.
			var h=head();Feels.sound(level,h,"aura_frost_wyrmling_breath",.6F,1.1F+random.nextFloat()*.2F);
			var f=ahead(rest).scale(-1);var r=new Vec3(-f.z,0,f.x);
			var at=h.add(r.scale(.8));
			level.sendParticles(ParticleTypes.CLOUD,at.x,at.y,at.z,5,.15,.05,.15,.01);
			level.sendParticles(ParticleTypes.SNOWFLAKE,at.x,at.y,at.z,4,.2,.05,.2,.01);
		}
	}

	/** Begin uncurling, {@code already} ticks in (when it is woken half way back to curling up). */
	private void wake(ServerLevel level,int already){
		pose(FrostWyrmlingRules.RISE,FrostWyrmlingRules.RISE_TICKS-already);entityData.set(BEGAN,level.getGameTime()-already);
		Feels.sound(level,head(),"aura_frost_wyrmling_rise",1.4F,1);
		level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+.4,getZ(),40,1.4,.3,1.4,.05);
		level.sendParticles(ParticleTypes.ITEM_SNOWBALL,getX(),getY()+.3,getZ(),20,1.2,.2,1.2,.08);
	}

	/** On its feet: wings folded with a snap, a step forward, and the fight is on. */
	private void stand(ServerLevel level){
		pose(FrostWyrmlingRules.WATCH,FrostWyrmlingRules.GAP);hold();
		lastCut=level.getGameTime();quiet=0;
		Feels.sound(level,position(),"aura_frost_wyrmling_step",1.3F,.9F);
		level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+.2,getZ(),24,1.4,.1,1.4,.04);
	}
	private void awake(ServerLevel level){
		if(!inHall(position(),FrostWyrmlingRules.LEASH)){
			var at=spot();teleportTo(at.x,at.y,at.z);setDeltaMovement(Vec3.ZERO);getNavigation().stop();pose(FrostWyrmlingRules.WATCH,FrostWyrmlingRules.GAP);
		}
		long now=level.getGameTime();float heal=FrostWyrmlingRules.regen(getMaxHealth(),now-lastCut);if(heal>0 && getHealth()<getMaxHealth())heal(heal);
		if(tickCount%10==0){
			bar.setProgress(getHealth()/getMaxHealth());
			for(var p:new ArrayList<>(bar.getPlayers()))if(p.level()!=level || !p.isAlive() || !inHall(p.position(),FrostWyrmlingRules.BAR_DROP))bar.removePlayer(p);
			for(var p:level.players())if(p.isAlive() && !p.isSpectator() && inHall(p.position(),FrostWyrmlingRules.BAR_SHOW))bar.addPlayer(p);
			setTarget(level.players().stream().filter(p->p.isAlive() && !p.isCreative() && !p.isSpectator() && inHall(p.position(),0))
				.min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null));
		}
		stung*=FrostWyrmlingRules.STING_FADE;
		var target=getTarget();
		switch(move()){
			case FrostWyrmlingRules.BITE->bite(level,target);
			case FrostWyrmlingRules.TAIL->tail(level);
			case FrostWyrmlingRules.GALE->gale(level,target);
			case FrostWyrmlingRules.BREATH->breath(level,target);
			case FrostWyrmlingRules.POUNCE->pounce(level,target);
			case FrostWyrmlingRules.DIVE->dive(level,target);
			case FrostWyrmlingRules.SLIDE->toboggan(level);
			case FrostWyrmlingRules.ICEFALL->icefall(level);
			case FrostWyrmlingRules.LANCES->lances(level,target);
			case FrostWyrmlingRules.SHELL->shell(level);
			case FrostWyrmlingRules.BOUND->{face(aim);move(MoverType.SELF,new Vec3(slide.x,-.3,slide.z));setDeltaMovement(Vec3.ZERO);if(--left<=0)pose(FrostWyrmlingRules.WATCH,2);}
			case FrostWyrmlingRules.RECOVER->{still();if(since()%12==6){var h=head();level.sendParticles(ParticleTypes.CLOUD,h.x,h.y-.3,h.z,3,.2,.05,.2,.01);}if(--left<=0)pose(FrostWyrmlingRules.WATCH,gap());}
			case FrostWyrmlingRules.SPRAWL->sprawl(level);
			case FrostWyrmlingRules.DAZED->dazed(level);
			default->watch(level,target);
		}
		waves(level);icicles(level);
	}

	/** On its feet and tracking: closing on its prey, and turning only so fast, so its flank can be got at. */
	private void watch(ServerLevel level,LivingEntity target){
		if(getDeltaMovement().horizontalDistanceSqr()>.0004 && tickCount%9==0)Feels.sound(level,position(),"aura_frost_wyrmling_step",1,.9F+random.nextFloat()*.2F);
		if(target==null){
			quiet++;
			var at=spot();
			if(quiet>=FrostWyrmlingRules.RETURN_TICKS && distanceToSqr(at)>1.5)getNavigation().moveTo(at.x,at.y,at.z,1);
			if(quiet>=FrostWyrmlingRules.RESET_TICKS && (distanceToSqr(at)<=2.5 || quiet>=FrostWyrmlingRules.RESET_TICKS+100)){
				bar.removeAllPlayers();pose(FrostWyrmlingRules.BED,FrostWyrmlingRules.RISE_TICKS);hold();Feels.sound(level,head(),"aura_frost_wyrmling_rise",1,.8F);
			}
			return;
		}
		quiet=0;
		getLookControl().setLookAt(target,30,30);
		double d=distanceTo(target);
		if(left>0)left--;
		if(left<=0){
			long now=level.getGameTime();
			int pick;
			if(FrostWyrmlingRules.shells(getHealth(),getMaxHealth(),fierce))pick=FrostWyrmlingRules.SHELL;
			else{
				long[] since=new long[lastMade.length];
				for(int i=0;i<since.length;i++)since[i]=now-lastMade[i];
				pick=FrostWyrmlingRules.choose(deck,d,yawTo(target.position())-getYRot(),target.getY()-getY(),since,fierce,random.nextDouble());
				// A long chase ends in bounds, or the icicles, from wherever it got to.
				if(pick<0 && chased>=60 && d>=5){
					if(since[FrostWyrmlingRules.POUNCE]>=FrostWyrmlingRules.rest(FrostWyrmlingRules.POUNCE,fierce)/2)pick=FrostWyrmlingRules.POUNCE;
					else if(since[FrostWyrmlingRules.ICEFALL]>=FrostWyrmlingRules.rest(FrostWyrmlingRules.ICEFALL,fierce)/2)pick=FrostWyrmlingRules.ICEFALL;
				}
			}
			if(pick>=0){links=0;begin(level,pick,target,false);return;}
		}
		if(d>FrostWyrmlingRules.CLOSE){
			chased++;
			if(!getNavigation().moveTo(target,1))getMoveControl().setWantedPosition(target.getX(),target.getY(),target.getZ(),1);
		}else{chased=0;getNavigation().stop();turn(yawTo(target.position()),10);}
	}

	/** Start {@code move} at {@code target}; {@code chained} when it runs straight on into it from another, fierce. */
	private void begin(ServerLevel level,int move,LivingEntity target,boolean chained){
		lastMade[move]=level.getGameTime();deck.played(move);chased=0;struck.clear();
		if(target!=null)aim=yawTo(target.position());
		switch(move){
			case FrostWyrmlingRules.BITE->{pose(move,0,FrostWyrmlingRules.BITE_TELL);Feels.sound(level,head(),"aura_frost_wyrmling_snarl",1.1F,1.2F);}
			case FrostWyrmlingRules.TAIL->{
				aim=getYRot();
				// It sweeps round from the far side, so the tail comes at its prey round behind it.
				double s=target==null?1:FrostWyrmlingRules.wrap(yawTo(target.position())-aim);
				entityData.set(SIDE,s>=0?-1:1);pose(move,0,fierce?FrostWyrmlingRules.TAIL_TELL_FIERCE:FrostWyrmlingRules.TAIL_TELL);
				Feels.sound(level,position(),"aura_frost_wyrmling_snarl",1,1.1F);
			}
			case FrostWyrmlingRules.GALE->{pose(move,0,FrostWyrmlingRules.GALE_TELL);Feels.sound(level,head(),"aura_frost_wyrmling_snarl",1.3F,.85F);Feels.sound(level,position().add(0,2,0),"aura_frost_wyrmling_wing",1.2F,.7F);}
			case FrostWyrmlingRules.BREATH->{entityData.set(SIDE,random.nextBoolean()?1:-1);pillars.clear();pose(move,0,FrostWyrmlingRules.BREATH_TELL);Feels.sound(level,head(),"aura_frost_wyrmling_breath",1.4F,.7F);}
			case FrostWyrmlingRules.POUNCE->{pose(move,0,FrostWyrmlingRules.POUNCE_TELL);Feels.sound(level,head(),"aura_frost_wyrmling_snarl",1.4F,1);}
			case FrostWyrmlingRules.DIVE->{from=position();to=position();pose(move,0,FrostWyrmlingRules.DIVE_TELL);Feels.sound(level,head(),"aura_frost_wyrmling_snarl",1.5F,.9F);}
			case FrostWyrmlingRules.SLIDE->{glide=ahead(aim);pose(move,0,FrostWyrmlingRules.SLIDE_TELL);Feels.sound(level,head(),"aura_frost_wyrmling_breath",1.4F,1.1F);}
			case FrostWyrmlingRules.ICEFALL->{pose(move,0,FrostWyrmlingRules.ICEFALL_TELL);roar(level,1.9F,.7F);sound(level,position(),SoundEvents.LIGHTNING_BOLT_THUNDER,.7F,.5F);}
			case FrostWyrmlingRules.LANCES->{pose(move,0,FrostWyrmlingRules.LANCE_TELL);roar(level,1.8F,.85F);}
			case FrostWyrmlingRules.SHELL->{
				fierce=true;bar.setColor(BossEvent.BossBarColor.WHITE);pose(move,0,FrostWyrmlingRules.SHELL_TELL);roar(level,2F,.7F);
				Feels.sound(level,position().add(0,2,0),"aura_frost_wyrmling_wing",1.6F,.6F);
			}
			default->{}
		}
	}
	/** Done with {@code move}: fierce, maybe straight on into another; otherwise spent for {@code recover} ticks, longer after a run. */
	private void after(ServerLevel level,int move,int recover){
		var target=getTarget();
		if(fierce && target!=null && links<FrostWyrmlingRules.CHAIN_MOST){
			int next=FrostWyrmlingRules.chain(move,distanceTo(target),random.nextDouble());
			if(next>=0 && level.getGameTime()-lastMade[next]>=FrostWyrmlingRules.rest(next,true)/2){links++;begin(level,next,target,true);return;}
		}
		spent(links>0?recover+FrostWyrmlingRules.CHAIN_REST:recover);links=0;
	}

	/**
	 * Neck coiled back low in an S through the tell, jaws parting with frost steaming off them, turning after its prey until just
	 * before; then a lunge and its jaws snapped shut on whatever is in front of it. Fierce, it gathers itself and snaps again, aimed
	 * afresh: a sidestep takes it out of the line, and it has to be taken twice.
	 */
	private void bite(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell(),again=tell+FrostWyrmlingRules.BITE_HOLD+FrostWyrmlingRules.BITE_AGAIN;
		boolean second=fierce && el>tell+FrostWyrmlingRules.BITE_HOLD;
		long snap=second?again:tell;
		if(target!=null && el<snap-3 && (el<tell || second))aim=turned(aim,yawTo(target.position()),8);
		face(aim);still();
		if(el<snap && el%3==0){var h=head();level.sendParticles(ParticleTypes.CLOUD,h.x,h.y-.4,h.z,1,.1,.05,.1,.005);level.sendParticles(ParticleTypes.SNOWFLAKE,h.x,h.y-.4,h.z,2,.15,.1,.15,.01);}
		if(el==snap-4 && second)Feels.sound(level,head(),"aura_frost_wyrmling_snarl",1F,1.35F);
		if(el==snap || el==snap+1){
			if(el==snap)struck.clear();
			var f=ahead(aim);move(MoverType.SELF,new Vec3(f.x*FrostWyrmlingRules.BITE_LUNGE/2,-.3,f.z*FrostWyrmlingRules.BITE_LUNGE/2));setDeltaMovement(Vec3.ZERO);
		}
		if(el==snap+1){
			var h=head().add(ahead(aim).scale(1.2));
			Feels.sound(level,h,"aura_frost_wyrmling_bite",1.4F,.9F+random.nextFloat()*.2F);
			level.sendParticles(ParticleTypes.SNOWFLAKE,h.x,h.y-.6,h.z,12,.4,.3,.4,.05);
			level.sendParticles(ParticleTypes.ITEM_SNOWBALL,h.x,h.y-.6,h.z,6,.3,.2,.3,.08);
			var f=ahead(aim);
			for(var p:prey(level)){
				double dx=p.getX()-getX(),dz=p.getZ()-getZ();
				if(!struck.contains(p.getUUID()) && FrostWyrmlingRules.bitten(dx*f.x+dz*f.z,dx*-f.z+dz*f.x,p.getY()-getY())){struck.add(p.getUUID());hit(level,p,FrostWyrmlingRules.BITE_DAMAGE,.9,position(),.4);}
			}
		}
		if(el>=(fierce?again:tell)+FrostWyrmlingRules.BITE_HOLD)after(level,FrostWyrmlingRules.BITE,FrostWyrmlingRules.BITE_RECOVER);
	}
	/**
	 * Crouched and looking back over its shoulder through the tell, tail raised and curled, frost gathering on the fin at its tip;
	 * then whipped round low from the far side through everything but its front. Jump it.
	 */
	private void tail(ServerLevel level){
		face(aim);still();
		long el=since();int tell=tell(),side=side();
		var f=ahead(aim);
		if(el<tell){
			if(el%2==0){var tip=position().subtract(f.scale(3.2)).add(0,1.8,0);level.sendParticles(ParticleTypes.SNOWFLAKE,tip.x,tip.y,tip.z,2,.2,.2,.2,.01);level.sendParticles(PALE,tip.x,tip.y,tip.z,1,.15,.15,.15,0);}
			return;
		}
		int k=(int)(el-tell)+1;
		if(k==1)Feels.sound(level,position().subtract(f.scale(2.6)),"aura_frost_wyrmling_tail",1.5F,.85F+random.nextFloat()*.15F);
		if(k<=FrostWyrmlingRules.TAIL_TICKS){
			double was=FrostWyrmlingRules.tail(side,k-1),now=FrostWyrmlingRules.tail(side,k);
			for(int j=0;j<=4;j++){
				var dir=ahead((float)(aim+was+(now-was)*j/4));
				for(double r=1.5;r<=FrostWyrmlingRules.TAIL_REACH;r+=1.3)level.sendParticles(j%2==0?ParticleTypes.SNOWFLAKE:PALE,getX()+dir.x*r,getY()+.35,getZ()+dir.z*r,1,.08,.05,.08,.01);
			}
			for(var p:prey(level)){
				if(struck.contains(p.getUUID()))continue;
				double dx=p.getX()-getX(),dz=p.getZ()-getZ();
				if(!FrostWyrmlingRules.lashed(side,k,yawTo(p.position())-aim,Math.sqrt(dx*dx+dz*dz),p.getY()-getY()))continue;
				struck.add(p.getUUID());
				hit(level,p,FrostWyrmlingRules.TAIL_DAMAGE,1.4,position(),.3);
			}
		}
		if(k>=FrostWyrmlingRules.TAIL_TICKS+2)after(level,FrostWyrmlingRules.TAIL,FrostWyrmlingRules.TAIL_RECOVER);
	}
	/**
	 * Reared a little through the tell with its wings mantled forward and cupped, snow swirling up round it; then beaten down again
	 * and again, each beat a gale of snow along its aim that stings and shoves everyone before it (fierce, one beat more). Get round
	 * to its side, or crouch into the wind.
	 */
	private void gale(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell(),beats=fierce?FrostWyrmlingRules.GALE_BEATS_FIERCE:FrostWyrmlingRules.GALE_BEATS;
		if(target!=null && (el<tell-3 || el>tell))aim=turned(aim,yawTo(target.position()),el<tell?8:3);
		face(aim);still();
		if(el<tell){if(el%2==0)level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+1.5,getZ(),6,2,1,2,.04);return;}
		long k=el-tell;
		if(k%FrostWyrmlingRules.GALE_EVERY==0 && k/FrostWyrmlingRules.GALE_EVERY<beats){
			struck.clear();
			var f=ahead(aim);
			Feels.sound(level,position().add(0,2,0),"aura_frost_wyrmling_wing",1.7F,.8F+random.nextFloat()*.15F);
			sound(level,position().add(f.scale(2)),SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),1.2F,.7F);
			for(int i=0;i<36;i++){
				double sp=(random.nextDouble()-.5)*1.6;
				level.sendParticles(i%3==0?ParticleTypes.POOF:ParticleTypes.SNOWFLAKE,getX()+f.x*1.5,getY()+.4+random.nextDouble()*2,getZ()+f.z*1.5,0,f.x-f.z*sp,.02,f.z+f.x*sp,.9);
			}
			for(var p:prey(level)){
				double dx=p.getX()-getX(),dz=p.getZ()-getZ();
				if(struck.contains(p.getUUID()) || !FrostWyrmlingRules.gusted(dx*f.x+dz*f.z,dx*-f.z+dz*f.x,p.getY()-getY()))continue;
				struck.add(p.getUUID());
				hit(level,p,FrostWyrmlingRules.GALE_DAMAGE,0,position(),0);
				double shove=FrostWyrmlingRules.GALE_SHOVE*(p.isShiftKeyDown()?FrostWyrmlingRules.GALE_CROUCH:1);
				p.knockback(shove,-f.x,-f.z,level.damageSources().mobAttack(this),0);p.needsSync=true;
			}
		}
		if(k>=(beats-1)*FrostWyrmlingRules.GALE_EVERY+4)after(level,FrostWyrmlingRules.GALE,FrostWyrmlingRules.GALE_RECOVER);
	}
	/**
	 * Head drawn back high through the tell, chest swelling and aglow, turning after its prey until just before; as it draws breath
	 * two pillars of ice burst up either side of its prey (fierce, a third off to one side). Then a beam of frost swept from one side
	 * of the hall to the other (fierce, and back), chilling, slowing and freezing whoever it crosses. Get behind a pillar, or behind it.
	 */
	private void breath(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell(),dir=side(),passes=fierce?2:1;
		if(target!=null && el<tell-4)aim=turned(aim,yawTo(target.position()),12);
		face(aim);still();
		if(el==2 && target!=null){
			var f=ahead(yawTo(target.position()));var r=new Vec3(-f.z,0,f.x);
			int hold=tell-2+passes*FrostWyrmlingRules.BREATH_TICKS+4;
			pillar(level,target.position().add(r.scale(FrostWyrmlingRules.PILLAR_SIDE)),hold);
			pillar(level,target.position().subtract(r.scale(FrostWyrmlingRules.PILLAR_SIDE)),hold);
			if(fierce)pillar(level,position().add(ahead((float)(aim+(random.nextBoolean()?1:-1)*(30+random.nextDouble()*20))).scale(6)),hold);
		}
		var h=head();
		if(el<tell){
			if(el%2==0){var c=position().add(ahead(aim).scale(1.4)).add(0,1.6,0);level.sendParticles(FROST,c.x,c.y,c.z,3,.3,.3,.3,0);level.sendParticles(ParticleTypes.SNOWFLAKE,h.x,h.y,h.z,2,.2,.1,.2,.01);}
			return;
		}
		long k=el-tell;
		if(k>=passes*FrostWyrmlingRules.BREATH_TICKS){after(level,FrostWyrmlingRules.BREATH,FrostWyrmlingRules.BREATH_RECOVER);return;}
		if(k==FrostWyrmlingRules.BREATH_TICKS)struck.clear();
		if(k==0 || k==FrostWyrmlingRules.BREATH_TICKS)Feels.sound(level,h,"aura_frost_wyrmling_frost",1.7F,.9F);
		if(k%6==0)Feels.sound(level,h,"aura_frost_wyrmling_breath",1F,1.3F);
		float beam=(float)(aim+FrostWyrmlingRules.beam(dir,k));
		var f=ahead(beam);
		// The beam, stopped short by any pillar in its way.
		double ground=getY()+.4;
		for(double r=1;r<=FrostWyrmlingRules.BREATH_REACH;r+=.8){
			double x=h.x+f.x*r,z=h.z+f.z*r,y=h.y-.3+(ground-(h.y-.3))*Math.min(1,r/5);
			if(blocked(h,x,z))break;
			level.sendParticles(ParticleTypes.SNOWFLAKE,x,y,z,1,.15,.12,.15,.02);
			if(((int)(r*1.25))%3==0)level.sendParticles(PALE,x,y,z,1,.1,.1,.1,0);
		}
		for(var p:prey(level)){
			if(struck.contains(p.getUUID()))continue;
			double dx=p.getX()-getX(),dz=p.getZ()-getZ();
			if(!FrostWyrmlingRules.breathed(yawTo(p.position())-beam,Math.sqrt(dx*dx+dz*dz),p.getY()-getY()))continue;
			if(blocked(h,p.getX(),p.getZ()) || covered(level,h,p))continue;
			struck.add(p.getUUID());
			hit(level,p,FrostWyrmlingRules.BREATH_DAMAGE,.3,position(),0);
			p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,FrostWyrmlingRules.BREATH_SLOW,1,false,true));
			if(p.canFreeze())p.setTicksFrozen(Math.min(p.getTicksRequiredToFreeze()+40,p.getTicksFrozen()+FrostWyrmlingRules.BREATH_FROST));
			level.sendParticles(ParticleTypes.SNOWFLAKE,p.getX(),p.getY()+1,p.getZ(),8,.3,.5,.3,.01);
		}
	}
	/** A pillar of ice bursting up out of the floor at {@code at}, if it is in the hall, standing {@code hold} ticks. */
	private void pillar(ServerLevel level,Vec3 at,int hold){
		if(!inHall(at,-1))return;
		double y=floor(level,at.x,at.y+.5,at.z);var base=new Vec3(at.x,y-.3,at.z);
		BlockFx.shard(level,base,FrostWyrmlingRules.PILLAR_HEIGHT,FrostWyrmlingRules.PILLAR_WIDTH,0,1,0,hold);
		pillars.add(base);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.PACKED_ICE.defaultBlockState()),at.x,y+.3,at.z,16,.4,.2,.4,.12);
		Feels.sound(level,base,"aura_frost_wyrmling_lance",1.2F,1.3F);
	}
	/** Whether one of its pillars stands between its jaws at {@code h} and ({@code x}, {@code z}). */
	private boolean blocked(Vec3 h,double x,double z){
		for(var c:pillars)if(FrostWyrmlingRules.sheltered(x-h.x,z-h.z,c.x-h.x,c.z-h.z))return true;
		return false;
	}
	/** Whether the hall itself (an icicle pillar, a wall) stands between its jaws at {@code h} and {@code p}. */
	private boolean covered(ServerLevel level,Vec3 h,LivingEntity p){
		var eye=p.getEyePosition();
		return level.clip(new ClipContext(h,eye,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()!=HitResult.Type.MISS;
	}
	/**
	 * Crouched low through the tell with its rump wiggling, tracking its prey; then a string of bounding hops (fierce, one more),
	 * each aimed afresh where its prey stands as it gathers itself, the spot marked in frost on the floor, and each landing claws
	 * first; the last lands hardest and widest. Keep moving sideways.
	 */
	private void pounce(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell(),hops=FrostWyrmlingRules.hops(fierce);
		if(el<tell){
			if(el<=tell-3 && target!=null)aim=turned(aim,yawTo(target.position()),10);
			face(aim);still();
			if(el%3==0)level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+.1,getZ(),6,.8,0,.8,.04);
			return;
		}
		long s=el-tell;int i=(int)(s/FrostWyrmlingRules.HOP_CYCLE);long c=s%FrostWyrmlingRules.HOP_CYCLE;
		boolean last=i==hops-1;double radius=last?FrostWyrmlingRules.LAST_RADIUS:FrostWyrmlingRules.HOP_RADIUS;
		if(c==0){
			// Gathered for the next hop: where it will land, marked.
			struck.clear();
			if(target!=null)aim=yawTo(target.position());
			from=position();
			double d=target!=null?Math.sqrt(target.distanceToSqr(getX(),target.getY(),getZ())):FrostWyrmlingRules.HOP_REACH*.6;
			to=from.add(ahead(aim).scale(Math.min(FrostWyrmlingRules.HOP_REACH,d)));
			for(int n=0;n<8 && !inHall(to,-1.5);n++)to=from.add(to.subtract(from).scale(.75));
			to=new Vec3(to.x,floor(level,to.x,from.y+.5,to.z),to.z);
			Feels.sound(level,head(),"aura_frost_wyrmling_snarl",.9F,1.2F+i*.08F);
		}
		face(aim);
		if(c%2==0)ring(level,to,radius,last?WHITE:FROST,last?20:14);
		if(c<FrostWyrmlingRules.HOP_GAP){still();return;}
		if(c==FrostWyrmlingRules.HOP_GAP){
			Feels.sound(level,position(),"aura_frost_wyrmling_bound",1.3F,.9F+random.nextFloat()*.2F);
			level.sendParticles(ParticleTypes.POOF,getX(),getY()+.2,getZ(),10,.8,.1,.8,.04);
		}
		var step=to.subtract(from).scale(1.0/FrostWyrmlingRules.HOP_TICKS);
		move(MoverType.SELF,new Vec3(step.x,-.3,step.z));setDeltaMovement(Vec3.ZERO);
		level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+1+FrostWyrmlingRules.hop(s+.5),getZ(),2,.8,.3,.8,.01);
		if(c<FrostWyrmlingRules.HOP_CYCLE-1)return;
		var at=position();
		Feels.sound(level,at,"aura_frost_wyrmling_land",last?2F:1.5F,(last?.8F:1F)+random.nextFloat()*.15F);
		for(int n=0;n<24;n++){double a=n*Math.PI*2/24,r=radius*.8;level.sendParticles(n%2==0?ParticleTypes.POOF:ParticleTypes.SNOWFLAKE,at.x+Math.cos(a)*r,at.y+.1,at.z+Math.sin(a)*r,1,.1,0,.1,.04);}
		level.sendParticles(ParticleTypes.ITEM_SNOWBALL,at.x,at.y+.2,at.z,last?36:18,radius*.5,.2,radius*.5,.1);
		for(var p:prey(level))if(!struck.contains(p.getUUID()) && FrostWyrmlingRules.lands(p.getX()-at.x,p.getZ()-at.z,p.getY()-at.y,radius)){
			struck.add(p.getUUID());hit(level,p,last?FrostWyrmlingRules.LAST_DAMAGE:FrostWyrmlingRules.HOP_DAMAGE,last?1.3:.8,at,last?.45:.25);
		}
		if(last)after(level,FrostWyrmlingRules.POUNCE,FrostWyrmlingRules.POUNCE_RECOVER);
	}
	/** A ring of {@code n} motes of {@code dust} at radius {@code r} round {@code at}, just off the floor. */
	private void ring(ServerLevel level,Vec3 at,double r,ParticleOptions dust,int n){
		for(int i=0;i<n;i++){double a=i*Math.PI*2/n;level.sendParticles(dust,at.x+Math.cos(a)*r,at.y+.15,at.z+Math.sin(a)*r,1,0,0,0,0);}
	}
	/**
	 * Crouched, then up off the floor on great beats of its wings to hang under the dome, a pale frost line drawn across the floor from
	 * under it to its prey, following them; the line flares white and holds as its aim fixes, it tucks its wings, and it dives down the
	 * line and skids on along the ice, sprawling at the end of it. Step off the line once it flares, then go for its belly.
	 */
	private void dive(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell(),up=FrostWyrmlingRules.DIVE_CROUCH+FrostWyrmlingRules.DIVE_RISE,lock=tell-FrostWyrmlingRules.DIVE_LOCK;
		if(el<FrostWyrmlingRules.DIVE_CROUCH){
			if(target!=null)aim=turned(aim,yawTo(target.position()),10);
			face(aim);still();
			if(el==1)level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+.1,getZ(),12,1,0,1,.05);
			return;
		}
		if(el<up){
			// Up on its wings.
			setNoGravity(true);face(aim);getNavigation().stop();
			move(MoverType.SELF,new Vec3(0,FrostWyrmlingRules.DIVE_HEIGHT/FrostWyrmlingRules.DIVE_RISE,0));setDeltaMovement(Vec3.ZERO);
			if((el-FrostWyrmlingRules.DIVE_CROUCH)%4==0){
				Feels.sound(level,position().add(0,2,0),"aura_frost_wyrmling_wing",1.5F,.9F);
				level.sendParticles(ParticleTypes.POOF,getX(),getY()-.2,getZ(),10,1.4,.1,1.4,.06);
			}
			level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+1,getZ(),3,1.6,.4,1.6,.02);
			return;
		}
		if(el<tell){
			// Hanging under the dome, the line drawn under it to its prey; then fixed.
			setNoGravity(true);setDeltaMovement(Vec3.ZERO);
			if(el<lock && target!=null){
				aim=turned(aim,yawTo(target.position()),15);
				double d=Math.sqrt(target.distanceToSqr(getX(),target.getY(),getZ()));
				var ground=new Vec3(getX(),to.y,getZ());
				to=ground.add(ahead(aim).scale(FrostWyrmlingRules.dive(d)));
				for(int n=0;n<8 && !inHall(to,-1.5);n++)to=ground.add(to.subtract(ground).scale(.75));
				to=new Vec3(to.x,floor(level,to.x,ground.y+.5,to.z),to.z);
			}
			if(el==lock){from=position();Feels.sound(level,head(),"aura_frost_wyrmling_snarl",1.6F,1.2F);sound(level,to,SoundEvents.AMETHYST_BLOCK_RESONATE,1.4F,.6F);}
			face(aim);
			if(el%5==0)Feels.sound(level,position().add(0,2,0),"aura_frost_wyrmling_wing",1.2F,1.05F);
			if(el%2==0 || el>=lock){
				var start=new Vec3(getX(),to.y,getZ());var run=to.subtract(start);int n=Math.max(3,(int)(run.length()*1.6));
				for(int i=0;i<=n;i++){var at=start.add(run.scale(i/(double)n));level.sendParticles(el>=lock?WHITE:PALE,at.x,at.y+.12,at.z,1,0,0,0,0);}
				ring(level,to,FrostWyrmlingRules.DIVE_RADIUS,el>=lock?WHITE:FROST,12);
			}
			return;
		}
		long k=el-tell;
		face(aim);
		if(k<FrostWyrmlingRules.DIVE_TICKS){
			// Down the line.
			setNoGravity(true);
			double t=(k+1.0)/FrostWyrmlingRules.DIVE_TICKS;var want=from.add(to.subtract(from).scale(t*t));
			move(MoverType.SELF,want.subtract(position()));setDeltaMovement(Vec3.ZERO);
			level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+1.2,getZ(),6,.8,.6,.8,.04);
			if(k==0)Feels.sound(level,head(),"aura_frost_wyrmling_roar",1.5F,1.3F);
			strike(level,FrostWyrmlingRules.DIVE_RADIUS,FrostWyrmlingRules.DIVE_DAMAGE,1.2,.5);
			if(k<FrostWyrmlingRules.DIVE_TICKS-1)return;
			// Down: the floor shaken, and on along the ice.
			setNoGravity(false);glide=ahead(aim);
			var at=position();
			Feels.sound(level,at,"aura_frost_wyrmling_land",2.2F,.7F);sound(level,at,SoundEvents.MACE_SMASH_GROUND_HEAVY,1.2F,.8F);
			level.sendParticles(ParticleTypes.ITEM_SNOWBALL,at.x,at.y+.2,at.z,40,1.6,.2,1.6,.15);
			level.sendParticles(ParticleTypes.POOF,at.x,at.y+.2,at.z,20,1.4,.1,1.4,.08);
			strike(level,FrostWyrmlingRules.DIVE_RADIUS+1,FrostWyrmlingRules.DIVE_DAMAGE,1.4,.5);
			return;
		}
		long s=k-FrostWyrmlingRules.DIVE_TICKS;
		if(s<FrostWyrmlingRules.DIVE_SKID && !horizontalCollision){
			double speed=FrostWyrmlingRules.DIVE_SKID_REACH/FrostWyrmlingRules.DIVE_SKID*2*(1-(s+.5)/FrostWyrmlingRules.DIVE_SKID);
			var step=glide.scale(speed);
			if(inHall(position().add(step),-1.5)){move(MoverType.SELF,new Vec3(step.x,-.3,step.z));setDeltaMovement(Vec3.ZERO);}
			level.sendParticles(ParticleTypes.SNOWFLAKE,getX()+glide.x*1.6,getY()+.2,getZ()+glide.z*1.6,6,.7,.1,.7,.06);
			if(s%3==0)sound(level,position(),SoundEvents.GLASS_BREAK,.5F,1.6F);
			strike(level,FrostWyrmlingRules.DIVE_RADIUS,FrostWyrmlingRules.DIVE_DAMAGE,1.2,.4);
			return;
		}
		entityData.set(SIDE,0);pose(FrostWyrmlingRules.SPRAWL,FrostWyrmlingRules.DIVE_SPRAWL);still();
		Feels.sound(level,position(),"aura_frost_wyrmling_hurt",1.2F,.8F);
	}
	/** Whoever is within {@code radius} of it and not yet struck this move, struck, knocked {@code shove} away and {@code lift} up. */
	private void strike(ServerLevel level,double radius,double damage,double shove,double lift){
		var at=position();
		for(var p:prey(level))if(!struck.contains(p.getUUID()) && FrostWyrmlingRules.lands(p.getX()-at.x,p.getZ()-at.z,p.getY()-at.y,radius)){
			struck.add(p.getUUID());hit(level,p,damage,shove,at,lift);
		}
	}
	/**
	 * Head down and breathing a glazed strip of ice along the floor at its prey, the strip laid out ahead of it a sheet at a time;
	 * then it turns its back, drops to its belly and toboggans tail first down the strip, its tail lashing, and ends sprawled. Get
	 * off the strip, then go for its belly.
	 */
	private void toboggan(ServerLevel level){
		long el=since();int tell=tell();
		if(el<tell){
			still();
			if(el<6){
				// The strip, a sheet at a time.
				face(aim);
				double d=1.5+el*2.3;var c=position().add(glide.scale(d));
				float s=(float)FrostWyrmlingRules.SLIDE_WIDTH,th=(float)-Math.toRadians(aim);
				if(inHall(c,-.5)){
					double y=floor(level,c.x,getY()+.5,c.z);
					var base=new Vec3(c.x-s/2*(Math.cos(th)+Math.sin(th)-1),y,c.z-s/2*(Math.cos(th)-Math.sin(th)-1));
					ArtBlocks.sheet(level,base,(el%2==0?Blocks.ICE:Blocks.PACKED_ICE).defaultBlockState(),s,th,1,(int)(tell-el)+FrostWyrmlingRules.SLIDE_TICKS+10);
					level.sendParticles(ParticleTypes.SNOWFLAKE,c.x,y+.2,c.z,8,s*.35,.05,s*.35,.03);
				}
				var h=head();
				for(int i=0;i<6;i++){var at=h.add(c.subtract(h).scale(i/6.0));level.sendParticles(i%2==0?ParticleTypes.SNOWFLAKE:PALE,at.x,at.y,at.z,1,.1,.1,.1,.01);}
				if(el%2==0)Feels.sound(level,c,"aura_frost_wyrmling_breath",.9F,1.4F);
				return;
			}
			// Turning its back to the strip, and down on its belly.
			float turn=(float)Math.min(1,Math.max(0,(el-FrostWyrmlingRules.SLIDE_TURN)/(double)(tell-FrostWyrmlingRules.SLIDE_TURN)));
			face(aim+180*turn);
			if(el==FrostWyrmlingRules.SLIDE_TURN)Feels.sound(level,position(),"aura_frost_wyrmling_step",1.3F,.8F);
			if(el==tell-2){Feels.sound(level,position(),"aura_frost_wyrmling_land",1.2F,1.2F);level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+.2,getZ(),16,1.2,.1,1.2,.06);}
			return;
		}
		long k=el-tell;
		face(aim+180);
		var step=glide.scale(FrostWyrmlingRules.SLIDE_REACH/FrostWyrmlingRules.SLIDE_TICKS);
		boolean on=k<FrostWyrmlingRules.SLIDE_TICKS && !horizontalCollision && inHall(position().add(step),-1.5);
		if(on){
			move(MoverType.SELF,new Vec3(step.x,-.3,step.z));setDeltaMovement(Vec3.ZERO);
			if(k==0)Feels.sound(level,position(),"aura_frost_wyrmling_snarl",1.4F,1.4F);
			if(k%2==0)sound(level,position(),SoundEvents.GLASS_HIT,.8F,1.5F+random.nextFloat()*.3F);
			level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+.3,getZ(),6,1,.1,1,.08);
			level.sendParticles(ParticleTypes.ITEM_SNOWBALL,getX()+glide.x*1.8,getY()+.2,getZ()+glide.z*1.8,3,.6,.1,.6,.1);
			var at=position();var r=new Vec3(-glide.z,0,glide.x);
			for(var p:prey(level)){
				if(struck.contains(p.getUUID()) || !FrostWyrmlingRules.lands(p.getX()-at.x,p.getZ()-at.z,p.getY()-at.y,FrostWyrmlingRules.SLIDE_RADIUS))continue;
				struck.add(p.getUUID());
				// Flung off to whichever side of the strip they were on.
				double side=(p.getX()-at.x)*r.x+(p.getZ()-at.z)*r.z;var away=r.scale(side>=0?1:-1);
				hit(level,p,FrostWyrmlingRules.SLIDE_DAMAGE,0,at,.4);
				p.knockback(1.3,-away.x,-away.z,level.damageSources().mobAttack(this),0);p.needsSync=true;
			}
			return;
		}
		entityData.set(SIDE,random.nextBoolean()?1:-1);pose(FrostWyrmlingRules.SPRAWL,FrostWyrmlingRules.SLIDE_SPRAWL);still();
		Feels.sound(level,position(),"aura_frost_wyrmling_land",1.4F,1.1F);
		level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+.3,getZ(),20,1.4,.2,1.4,.06);
	}
	/** Sprawled at the end of a dive or a slide, legs paddling at the ice, wide open; then scrambling up again, ready. */
	private void sprawl(ServerLevel level){
		still();
		if(since()%8==0){sound(level,position(),SoundEvents.GLASS_HIT,.6F,1.8F);level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+.2,getZ(),6,1.2,.1,1.2,.03);}
		if(left==8)Feels.sound(level,position(),"aura_frost_wyrmling_step",1.3F,1.1F);
		if(--left<=0){links=0;pose(FrostWyrmlingRules.WATCH,gap());Feels.sound(level,head(),"aura_frost_wyrmling_snarl",1.2F,1F);}
	}
	/**
	 * Head reared back to the dome with a roar and a rumble, wings half open, snow sifting down all over the hall; then a frost mark
	 * appears under each of its prey every few ticks, an icicle shaking loose over each, and down they come one after another
	 * (fierce, more of them). Keep moving and they fall behind you; its pale belly is bared all the while.
	 */
	private void icefall(ServerLevel level){
		long el=since();int tell=tell(),marks=fierce?FrostWyrmlingRules.MARKS_FIERCE:FrostWyrmlingRules.MARKS;
		face(aim);still();
		if(el%2==0)for(int i=0;i<3;i++){
			double x=getX()+(random.nextDouble()-.5)*24,z=getZ()+(random.nextDouble()-.5)*24;
			if(inHall(new Vec3(x,getY(),z),0))level.sendParticles(ParticleTypes.SNOWFLAKE,x,getY()+7+random.nextDouble()*3,z,1,.3,.1,.3,.01);
		}
		if(el%6==0)sound(level,position(),SoundEvents.AMETHYST_BLOCK_RESONATE,1.1F,.5F+random.nextFloat()*.1F);
		long j=(el-FrostWyrmlingRules.MARK_AT)/FrostWyrmlingRules.ICEFALL_EVERY;
		if(el>=FrostWyrmlingRules.MARK_AT && (el-FrostWyrmlingRules.MARK_AT)%FrostWyrmlingRules.ICEFALL_EVERY==0 && j<marks){
			long now=level.getGameTime();
			for(var p:prey(level)){
				// Every other one right under them; the rest a little way off, where they might be stepping.
				var at=p.position();
				if(j%2==1){double a=random.nextDouble()*Math.PI*2,r=1.5+random.nextDouble()*1.5;at=at.add(Math.cos(a)*r,0,Math.sin(a)*r);}
				if(!inHall(at,-1))continue;
				double y=floor(level,at.x,p.getY()+.5,at.z);at=new Vec3(at.x,y,at.z);
				var ice=new Icicle(at,now,now+tell-FrostWyrmlingRules.MARK_AT,ceiling(level,at));
				ice.ice=hang(level,at,ice.top);
				icicles.add(ice);
			}
		}
		if(el>=tell+(marks-1L)*FrostWyrmlingRules.ICEFALL_EVERY+FrostWyrmlingRules.ICICLE_FALL+2)after(level,FrostWyrmlingRules.ICEFALL,FrostWyrmlingRules.ICEFALL_RECOVER);
	}
	/** The underside of the dome over {@code at}, or as high as it looks if there is none near. */
	private double ceiling(ServerLevel level,Vec3 at){
		var pos=BlockPos.containing(at.x,at.y+.5,at.z).mutable();
		for(int i=0;i<FrostWyrmlingRules.ICEFALL_CEILING;i++){
			pos.move(0,1,0);
			if(!level.getBlockState(pos).getCollisionShape(level,pos).isEmpty())return pos.getY();
		}
		return at.y+FrostWyrmlingRules.ICEFALL_CEILING;
	}
	private static final float ICICLE_W=.55F,ICICLE_H=1.8F;
	/** An icicle of {@code w} across and {@code h} long hanging point down from its display, dropped {@code drop} below it. */
	private static com.mojang.math.Transformation spike(float w,float h,float drop){
		var turn=new Quaternionf().rotateY((float)Math.PI/4);
		var shift=new Vector3f(-w/2,0,-w/2).rotate(turn).add(0,-h-drop,0);
		return new com.mojang.math.Transformation(shift,new Quaternionf(turn),new Vector3f(w,h,w),new Quaternionf());
	}
	private static void tween(Display.BlockDisplay d,com.mojang.math.Transformation to,int ticks){
		if(d==null || d.isRemoved())return;
		d.setTransformationInterpolationDelay(0);d.setTransformationInterpolationDuration(ticks);d.setTransformation(to);
	}
	/** An icicle growing down out of the dome at {@code top} over {@code at}. */
	private Display.BlockDisplay hang(ServerLevel level,Vec3 at,double top){
		var d=EntityTypes.BLOCK_DISPLAY.create(level,EntitySpawnReason.TRIGGERED);
		if(d==null)return null;
		d.snapTo(at.x,top,at.z);d.setBlockState(Blocks.BLUE_ICE.defaultBlockState());d.setTransformation(spike(.05F,.05F,0));
		BlockFx.fresh(d);level.addFreshEntity(d);
		Scheduler.later(1,()->tween(d,spike(ICICLE_W,ICICLE_H,0),4));
		return d;
	}
	/** The icicles: marked, shaking, falling and shattering where they land, striking whoever still stands on the mark. */
	private void icicles(ServerLevel level){
		if(icicles.isEmpty())return;
		long now=level.getGameTime();
		for(var it=icicles.iterator();it.hasNext();){
			var i=it.next();long land=i.drop+FrostWyrmlingRules.ICICLE_FALL;
			if(now<i.drop){
				if((now-i.marked)%2==0)ring(level,i.at,FrostWyrmlingRules.ICICLE_RADIUS,FROST,12);
				if((now-i.marked)%3==0)level.sendParticles(ParticleTypes.SNOWFLAKE,i.at.x,i.top-ICICLE_H,i.at.z,2,.15,.1,.15,.01);
				if(now==i.drop-4)sound(level,new Vec3(i.at.x,i.top,i.at.z),SoundEvents.GLASS_HIT,1F,.6F);
				continue;
			}
			if(now==i.drop){
				tween(i.ice,spike(ICICLE_W,ICICLE_H,(float)Math.max(0,i.top-i.at.y-ICICLE_H*.6)),FrostWyrmlingRules.ICICLE_FALL);
				sound(level,new Vec3(i.at.x,i.top,i.at.z),SoundEvents.AMETHYST_BLOCK_CHIME,1.2F,.7F);
			}
			if(now<land){ring(level,i.at,FrostWyrmlingRules.ICICLE_RADIUS,WHITE,14);continue;}
			// Down: shattered on the floor.
			it.remove();
			if(i.ice!=null && !i.ice.isRemoved())i.ice.discard();
			sound(level,i.at,SoundEvents.GLASS_BREAK,1.3F,.7F+random.nextFloat()*.2F);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.BLUE_ICE.defaultBlockState()),i.at.x,i.at.y+.4,i.at.z,24,.5,.3,.5,.15);
			level.sendParticles(ParticleTypes.SNOWFLAKE,i.at.x,i.at.y+.3,i.at.z,10,.6,.2,.6,.05);
			BlockFx.shard(level,i.at.add(0,-.3,0),1.1F,.5F,random.nextDouble()-.5,random.nextDouble()-.5,.25F,10);
			for(var p:prey(level))if(!struck.contains(p.getUUID()) && FrostWyrmlingRules.lands(p.getX()-i.at.x,p.getZ()-i.at.z,p.getY()-i.at.y,FrostWyrmlingRules.ICICLE_RADIUS)){
				struck.add(p.getUUID());hit(level,p,FrostWyrmlingRules.ICICLE_DAMAGE,.4,i.at,0);
				p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,30,1,false,true));
			}
		}
	}
	/**
	 * Half beaten, once: it throws its head back, roars, and wraps its wings about itself as spears of ice close over it one after
	 * another into a shell; the shell glows, cracks and a white ring marks how far it will burst; then it bursts out, flinging whoever
	 * is close, and a ring of frost runs out along the floor across the hall. Get clear of the white ring, jump the frost ring, then
	 * strike it while it stands dazed.
	 */
	private void shell(ServerLevel level){
		long el=since();int tell=tell(),warn=tell-FrostWyrmlingRules.SHELL_WARN;
		face(aim);still();
		if(el>=2 && el<2+FrostWyrmlingRules.SHELL_SHARDS*2 && el%2==0){
			int i=(int)(el-2)/2;double a=getYRot()+i*360.0/FrostWyrmlingRules.SHELL_SHARDS;var f=ahead((float)a);
			var base=position().add(f.scale(FrostWyrmlingRules.SHELL_RADIUS));double y=floor(level,base.x,getY()+.5,base.z);
			BlockFx.shard(level,new Vec3(base.x,y-.3,base.z),3.6F,1F,-f.x,-f.z,.42F,(int)(tell-el)-2);
			Feels.sound(level,base,"aura_frost_wyrmling_lance",1.2F,.9F+i*.06F);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.PACKED_ICE.defaultBlockState()),base.x,y+.3,base.z,10,.3,.2,.3,.1);
		}
		if(el<warn){
			if(el%4==0)level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+1.5,getZ(),8,1.2,1,1.2,.02);
			if(el==2+FrostWyrmlingRules.SHELL_SHARDS*2)sound(level,position(),SoundEvents.AMETHYST_BLOCK_RESONATE,1.6F,.5F);
			return;
		}
		if(el<tell){
			// About to burst: glowing, cracking, the reach marked.
			level.sendParticles(WHITE,getX(),getY()+1.5,getZ(),4,1.2,1.2,1.2,0);
			if(el%2==0)ring(level,position(),FrostWyrmlingRules.BURST_RADIUS,WHITE,22);
			if(el%3==0)sound(level,position(),SoundEvents.GLASS_HIT,1.4F,.5F+(el-warn)*.05F);
			return;
		}
		var at=position();
		roar(level,2.2F,.8F);
		sound(level,at,SoundEvents.GLASS_BREAK,2F,.5F);sound(level,at,SoundEvents.MACE_SMASH_GROUND_HEAVY,1.4F,.7F);
		var ice=new BlockParticleOption(ParticleTypes.BLOCK,Blocks.BLUE_ICE.defaultBlockState());
		for(int i=0;i<40;i++){double a=random.nextDouble()*Math.PI*2;level.sendParticles(ice,at.x+Math.cos(a)*1.6,at.y+.5+random.nextDouble()*2.5,at.z+Math.sin(a)*1.6,0,Math.cos(a),.25,Math.sin(a),.6);}
		level.sendParticles(ParticleTypes.POOF,at.x,at.y+1,at.z,30,1.6,1,1.6,.1);
		strike(level,FrostWyrmlingRules.BURST_RADIUS,FrostWyrmlingRules.BURST_DAMAGE,FrostWyrmlingRules.BURST_SHOVE,.5);
		waves.add(new Wave(at,FrostWyrmlingRules.RING_SPEED,FrostWyrmlingRules.RING_REACH,FrostWyrmlingRules.RING_DAMAGE,FrostWyrmlingRules.RING_LIFT,level.getGameTime()));
		pose(FrostWyrmlingRules.DAZED,FrostWyrmlingRules.DAZED_TICKS);
	}
	/** Shaken after bursting out of its shell, swaying where it stands with its head low, wide open; then fierce. */
	private void dazed(ServerLevel level){
		still();
		if(since()%5==0){var h=head();level.sendParticles(ParticleTypes.CLOUD,h.x,h.y-.4,h.z,2,.2,.1,.2,.01);level.sendParticles(PALE,h.x,h.y+.4,h.z,3,.4,.1,.4,0);}
		if(--left<=0){pose(FrostWyrmlingRules.WATCH,gap());roar(level,1.8F,1.1F);}
	}
	/** The rings of frost running out along the floor, catching whoever is standing as they pass, until they run out. */
	private void waves(ServerLevel level){
		if(waves.isEmpty())return;
		long now=level.getGameTime();
		var prey=prey(level);
		for(var it=waves.iterator();it.hasNext();){
			var w=it.next();
			double r0=(now-w.born-1)*w.speed,r1=(now-w.born)*w.speed;
			if(r0>w.max){it.remove();continue;}
			var at=new Vec3(w.x,w.y,w.z);
			int n=Math.max(12,(int)(r1*5));
			for(int i=0;i<n;i++){
				double a=i*Math.PI*2/n,x=w.x+Math.cos(a)*r1,z=w.z+Math.sin(a)*r1;
				level.sendParticles(i%3==0?ParticleTypes.SNOWFLAKE:FROST,x,w.y+.25,z,1,.05,.05,.05,0);
			}
			if((now-w.born)%3==0)sound(level,at,SoundEvents.AMETHYST_BLOCK_CHIME,.9F,.6F+(float)(r1/w.max)*.6F);
			for(var p:prey){
				if(w.struck.contains(p.getUUID()))continue;
				double d=Math.sqrt(p.distanceToSqr(w.x,p.getY(),w.z));
				if(FrostWyrmlingRules.crosses(d,r0,r1,p.getY()-w.y)){w.struck.add(p.getUUID());hit(level,p,w.damage,.8,at,w.lift);}
			}
		}
	}

	/**
	 * Rime Lances. Reared up on its hind legs through the tell, wings wide, turning a little after its prey; then down on its
	 * forefeet. Frost cracks race out along the floor from where they land, one line at each player and the rest fanned evenly round
	 * it, and close behind the crack lances of ice burst up one after another out to the hall's edge, flinging whoever stands on one;
	 * they stand a moment and sink, and it stands spent.
	 */
	private void lances(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<tell-4 && target!=null)aim=turned(aim,yawTo(target.position()),6);
		face(aim);still();
		if(el<tell){
			if(el%3==0)level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+3,getZ(),6,1.8,.5,.8,.02);
			return;
		}
		long s=el-tell;
		if(s==0){
			slam=position().add(ahead(aim).scale(1.2));lanced.clear();
			var yaws=new ArrayList<Float>();yaws.add(aim);
			for(var p:prey(level))if(p!=target)yaws.add(yawTo(p.position()));
			float[] arr=new float[yaws.size()];for(int i=0;i<arr.length;i++)arr[i]=yaws.get(i);
			lanes=FrostWyrmlingRules.lines(arr);
			Feels.sound(level,slam,"aura_frost_wyrmling_land",2F,.75F);
			Feels.sound(level,slam,"aura_frost_wyrmling_lance",2F,1);
			level.sendParticles(ParticleTypes.ITEM_SNOWBALL,slam.x,slam.y+.2,slam.z,30,1.2,.1,1.2,.12);
			level.sendParticles(ParticleTypes.POOF,slam.x,slam.y+.2,slam.z,16,1,.1,1,.06);
		}
		var crack=new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,net.minecraft.world.level.block.Blocks.PACKED_ICE.defaultBlockState());
		double run=FrostWyrmlingRules.LANCE_REACH-FrostWyrmlingRules.LANCE_START;
		double crackWas=s==0?FrostWyrmlingRules.LANCE_START:Math.min(FrostWyrmlingRules.LANCE_REACH,FrostWyrmlingRules.LANCE_START+s*run/FrostWyrmlingRules.LANCE_WARN);
		double crackNow=Math.min(FrostWyrmlingRules.LANCE_REACH,FrostWyrmlingRules.LANCE_START+(s+1)*run/FrostWyrmlingRules.LANCE_WARN);
		double front=FrostWyrmlingRules.lanceFront(s),was=FrostWyrmlingRules.lanceFront(s-1);
		for(float lane:lanes){
			var f=ahead(lane);
			// The crack, racing out ahead: the warning.
			for(double d=crackWas;d<crackNow;d+=.7){
				double y=floor(level,slam.x+f.x*d,slam.y,slam.z+f.z*d);
				level.sendParticles(crack,slam.x+f.x*d,y+.1,slam.z+f.z*d,2,.15,0,.15,0);
				level.sendParticles(ParticleTypes.SNOWFLAKE,slam.x+f.x*d,y+.15,slam.z+f.z*d,1,.1,.05,.1,.01);
			}
			// The lances, bursting up behind it.
			if(front>was){
				double x=slam.x+f.x*front,z=slam.z+f.z*front,y=floor(level,x,slam.y,z);
				float h=(float)(1.5+front*.09+random.nextDouble()*.5),w=(float)(.7+random.nextDouble()*.25);
				dev.wildercord.cast.BlockFx.shard(level,new Vec3(x,y-.35,z),h,w,f.x,f.z,.18F+random.nextFloat()*.12F,FrostWyrmlingRules.LANCE_HOLD);
				level.sendParticles(crack,x,y+.3,z,6,.25,.2,.25,.1);
				if(((int)front)%3==0)Feels.sound(level,new Vec3(x,y,z),"aura_frost_wyrmling_frost",.9F,1.3F+random.nextFloat()*.3F);
			}
		}
		if(front>0 && s<FrostWyrmlingRules.LANCE_WARN+FrostWyrmlingRules.LANCE_RUN+3){
			for(var p:prey(level)){
				if(lanced.contains(p.getUUID()))continue;
				for(float lane:lanes){
					var f=ahead(lane);double dx=p.getX()-slam.x,dz=p.getZ()-slam.z;
					if(!FrostWyrmlingRules.lanced(dx*f.x+dz*f.z,dx*-f.z+dz*f.x,p.getY()-slam.y,front))continue;
					lanced.add(p.getUUID());
					hit(level,p,FrostWyrmlingRules.LANCE_DAMAGE,.3,slam,FrostWyrmlingRules.LANCE_LIFT);
					p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,FrostWyrmlingRules.BREATH_SLOW,1,false,true));
					level.sendParticles(crack,p.getX(),p.getY()+.5,p.getZ(),14,.3,.4,.3,.15);
					break;
				}
			}
		}
		if(s>=FrostWyrmlingRules.LANCE_WARN+FrostWyrmlingRules.LANCE_RUN+6)after(level,FrostWyrmlingRules.LANCES,FrostWyrmlingRules.LANCE_RECOVER);
	}
	/** The top of the floor near ({@code x}, {@code y}, {@code z}): up out of a drift, or down off a ledge, a few blocks at most. */
	private static double floor(ServerLevel level,double x,double y,double z){
		var at=BlockPos.containing(x,y,z).mutable();
		for(int i=0;i<3 && !level.getBlockState(at).getCollisionShape(level,at).isEmpty();i++)at.move(0,1,0);
		for(int i=0;i<4 && level.getBlockState(at.below()).getCollisionShape(level,at.below()).isEmpty();i++)at.move(0,-1,0);
		var shape=level.getBlockState(at).getCollisionShape(level,at);
		return at.getY()+(shape.isEmpty()?0:shape.max(net.minecraft.core.Direction.Axis.Y));
	}

	/** Spent after an attack, head low and wings drooping, breath steaming, for {@code ticks}. */
	private void spent(int ticks){pose(FrostWyrmlingRules.RECOVER,ticks);still();}
	/** Away from a blow, to one side, with a flick of its wings. */
	private void boundAside(ServerLevel level){
		aim=getYRot();int side=random.nextBoolean()?1:-1;var f=ahead(aim);
		slide=new Vec3(-f.z*side,0,f.x*side).scale(FrostWyrmlingRules.BOUND_SPEED).add(f.scale(-.15));
		entityData.set(SIDE,side);pose(FrostWyrmlingRules.BOUND,FrostWyrmlingRules.BOUND_TICKS,FrostWyrmlingRules.BOUND_TICKS);
		Feels.sound(level,position().add(0,1,0),"aura_frost_wyrmling_bound",1.2F,.9F+random.nextFloat()*.2F);
		level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+.1,getZ(),16,.6,.1,.6,.08);
	}
	/** Hurt too fast: a beat of its wings away from whoever is hurting it, back toward the middle of the hall. */
	private void leapAway(ServerLevel level,Vec3 from){
		lastLeap=level.getGameTime();stung=0;
		var away=from==null?ahead(getYRot()).scale(-1):new Vec3(getX()-from.x,0,getZ()-from.z);
		var home=spot().subtract(position()).multiply(1,0,1);
		if(home.lengthSqr()>16)away=away.normalize().add(home.normalize());
		if(away.lengthSqr()<1e-4)away=ahead(getYRot()).scale(-1);
		aim=getYRot();slide=away.normalize().scale(FrostWyrmlingRules.LEAP_SPEED);
		var f=ahead(aim);double side=slide.x*-f.z+slide.z*f.x;
		entityData.set(SIDE,Math.abs(side)<.3?0:side>0?1:-1);pose(FrostWyrmlingRules.BOUND,FrostWyrmlingRules.LEAP_TICKS,FrostWyrmlingRules.LEAP_TICKS);
		Feels.sound(level,position().add(0,2,0),"aura_frost_wyrmling_wing",1.4F,1.3F);
		level.sendParticles(ParticleTypes.POOF,getX(),getY()+.3,getZ(),16,1.2,.1,1.2,.05);
	}
	/** Standing its ground: no sliding about, though it still falls. */
	private void still(){getNavigation().stop();var v=getDeltaMovement();setDeltaMovement(0,Math.min(0,v.y),0);}
	private boolean wounded(){return FrostWyrmlingRules.wounded(getHealth(),getMaxHealth());}
	/** Ticks before its next blow. */
	private int gap(){return (fierce?FrostWyrmlingRules.GAP_FIERCE:FrostWyrmlingRules.GAP)+random.nextInt(3);}

	/** Players it could be fighting right now. */
	private List<ServerPlayer> prey(ServerLevel level){
		var out=new ArrayList<ServerPlayer>();
		for(var p:level.players())if(p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(this)<16*16 && inHall(p.position(),FrostWyrmlingRules.LEASH))out.add(p);
		return out;
	}
	/** A blow landing on {@code p}, through part of its armour, knocking it {@code shove} away from {@code from} and {@code lift} up. */
	private void hit(ServerLevel level,LivingEntity p,double damage,double shove,Vec3 from,double lift){
		if(!GuardianBite.land(level,this,p,(float)damage,FrostWyrmlingRules.PIERCE))return;
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
		if(FrostWyrmlingRules.scripted(move) && !source.is(DamageTypes.GENERIC_KILL)){
			// Curled up, its wings and the frost on its back take the blow; but a blow, or a shot, wakes it.
			if(move==FrostWyrmlingRules.DORMANT && Guardians.enabled() && source.getEntity() instanceof ServerPlayer p && !p.isCreative())wake(level,0);
			return false;
		}
		long now=level.getGameTime();
		if(!source.is(DamageTypes.GENERIC_KILL)){
			var at=source.getSourcePosition();
			boolean frontal=at!=null && FrostWyrmlingRules.frontal(yawTo(at)-getYRot());
			// It sees a blow coming and hops out from under it.
			if(source.getDirectEntity()!=null && source.getDirectEntity()==source.getEntity() && FrostWyrmlingRules.dodges(move,now-lastDodge,random.nextDouble())){
				lastDodge=now;boundAside(level);
				return false;
			}
			damage*=(float)FrostWyrmlingRules.armour(move,frontal);
		}
		boolean hurt=super.hurtServer(level,source,damage);
		if(hurt){
			lastCut=now;stung+=damage;
			if(FrostWyrmlingRules.leaps(move(),stung,now-lastLeap))leapAway(level,source.getSourcePosition());
		}
		return hurt;
	}
	@Override public void die(DamageSource source){
		if(level() instanceof ServerLevel server){
			Feels.sound(server,head(),"aura_frost_wyrmling_death",1.8F,1);Guardians.reward(server,this,FrostWyrmlingRules.TIER,bar,source);bar.removeAllPlayers();
			server.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+1.2,getZ(),60,1.6,.8,1.6,.04);
		}
		super.die(source);
	}
	@Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return null;}
	@Override protected void actuallyHurt(ServerLevel level,DamageSource source,float damage){
		super.actuallyHurt(level,source,damage);
		if(random.nextInt(3)==0)Feels.sound(level,head(),"aura_frost_wyrmling_hurt",1.2F,.9F+random.nextFloat()*.2F);
	}
	@Override public void remove(RemovalReason reason){bar.removeAllPlayers();thaw();super.remove(reason);}
	@Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);bar.removePlayer(player);}

	@Override protected void addAdditionalSaveData(ValueOutput out){
		super.addAdditionalSaveData(out);
		out.putBoolean("wyrm_bound",bound);out.store("wyrm_mound",BlockPos.CODEC,mound);out.putFloat("wyrm_rest",rest);
		out.store("wyrm_hall_min",BlockPos.CODEC,hallMin);out.store("wyrm_hall_max",BlockPos.CODEC,hallMax);
		out.putBoolean("wyrm_awake",!FrostWyrmlingRules.scripted(move()));out.putLong("wyrm_ticked",lastTick);out.putBoolean("wyrm_fierce",fierce);
	}
	@Override protected void readAdditionalSaveData(ValueInput in){
		super.readAdditionalSaveData(in);
		bound=in.getBooleanOr("wyrm_bound",false);
		mound=in.read("wyrm_mound",BlockPos.CODEC).orElse(blockPosition());rest=in.getFloatOr("wyrm_rest",getYRot());
		hallMin=in.read("wyrm_hall_min",BlockPos.CODEC).orElse(mound);hallMax=in.read("wyrm_hall_max",BlockPos.CODEC).orElse(mound);
		lastTick=in.getLongOr("wyrm_ticked",-1L);
		// Once out of its shell it stays fierce, unloaded or not.
		fierce=in.getBooleanOr("wyrm_fierce",false);bar.setColor(fierce?BossEvent.BossBarColor.WHITE:BossEvent.BossBarColor.BLUE);
		// Mid-fight it comes back on its feet, whatever it was doing when the chunk unloaded.
		if(in.getBooleanOr("wyrm_awake",false)){pose(FrostWyrmlingRules.WATCH,FrostWyrmlingRules.GAP);lastCut=0;}else pose(FrostWyrmlingRules.DORMANT,0);
	}
}
