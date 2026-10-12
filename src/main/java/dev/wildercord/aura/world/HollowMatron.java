package dev.wildercord.aura.world;

import dev.wildercord.cast.feel.Feels;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.InteractionResult;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import java.util.*;

/**
 * The Hollow Matron ({@link HollowMatronRules}). Generation leaves her hanging head-down on her thread under the nest in the Silk
 * Hollow's hall, bound to that thread and to the hall. While she hangs there she cannot be hurt; the first player far enough into the
 * hall (or the first blow) and she lets go, drops down her thread, turns over as she lands and rears up at them hissing, and then the
 * boss bar comes up. The hall going quiet sends her back up her thread, healed. The hall is hers: anything hostile in it she webs and
 * kills.
 * <p>
 * On her feet she bites whoever is in front of her, rears and sweeps her forelegs across through whoever is near, spits web at anyone
 * keeping their distance, slowing them, and pounces on whoever is in reach of a leap, landing on them: after a pounce she is spent a
 * while, the big opening. She skitters aside from some blows, and hurt too fast she backs away across the hall.
 * <p>
 * Wounded, she makes for one of the silk pillars and climbs it. At the top she turns head-down, her brood hatch from the egg sacs
 * round the walls, and dust sifts down onto the spot she has marked under her prey; then she plunges onto it. Strike the pillar's silk
 * while she is on it and she is cut down, falling hard and lying stunned a long while.
 */
public final class HollowMatron extends PathfinderMob implements Enemy {
	private static final EntityDataAccessor<Integer> MOVE=SynchedEntityData.defineId(HollowMatron.class,EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN=SynchedEntityData.defineId(HollowMatron.class,EntityDataSerializers.LONG);
	/** The current move's tell in ticks, so the client knows when the blow lands. */
	private static final EntityDataAccessor<Integer> TELL=SynchedEntityData.defineId(HollowMatron.class,EntityDataSerializers.INT);
	/** Which way a sweep or a skitter goes, for the client: -1 left, 1 right, 0 straight back. */
	private static final EntityDataAccessor<Integer> SIDE=SynchedEntityData.defineId(HollowMatron.class,EntityDataSerializers.INT);
	/** The height her thread hangs from, the underside of the nest, so the client can draw it. */
	private static final EntityDataAccessor<Float> ANCHOR=SynchedEntityData.defineId(HollowMatron.class,EntityDataSerializers.FLOAT);
	private static final EntityDimensions HANGING=EntityDimensions.scalable(2.2F,3.8F).withEyeHeight(.8F),STANDING=EntityDimensions.scalable(3.6F,2.2F).withEyeHeight(1.4F);
	private final ServerBossEvent bar=new ServerBossEvent(getUUID(),getType().getDescription(),BossEvent.BossBarColor.WHITE,BossEvent.BossBarOverlay.NOTCHED_10);
	private boolean bound;
	/** Where her feet are while she hangs, the floor under her, and the hall she keeps. */
	private BlockPos hang=BlockPos.ZERO,hallMin=BlockPos.ZERO,hallMax=BlockPos.ZERO;
	private int floor;
	private float rest;
	private int left,quiet;
	private long lastCut;
	/** The game time she last ran, so she knows how long the hall was left alone. */
	private long lastTick=-1;
	/** The line her blow was locked on, where a skitter carries her, and where a pounce set off from and comes down. */
	private float aim;
	private Vec3 slide=Vec3.ZERO,leapFrom=Vec3.ZERO,leapTo=Vec3.ZERO;
	private int chased;
	private long lastDodge=-999,lastBack=-999,lastSweep=-999,lastSpit=-999,lastPounce=-999;
	/** Hurt taken lately, fading; enough of it sends her backing off. */
	private double stung;
	/** A glob of web in flight: where it is, which way and how fast it goes, and how far it has left to go. */
	private static final class Glob{Vec3 at,step;double left;Glob(Vec3 at,Vec3 step,double left){this.at=at;this.step=step;this.left=left;}}
	private final List<Glob> globs=new ArrayList<>();
	/** The spiderlings that hatch from her sacs carry this, so she spares them while she fights. */
	private static final net.minecraft.world.level.block.state.BlockState SILK=Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState();
	public static final String BROOD="wildercord_hollow_brood";
	/** The feet of the silk pillars she climbs, and the egg sacs her brood hatch from. */
	private List<BlockPos> pillars=List.of(),sacs=List.of();
	/** Through an ambush: the pillar she is on and the top of its silk, where she clings, where she set off from, the spot she
	 *  marked to land on, and the strikes its silk has taken. */
	private BlockPos pillar;
	private int top;
	private Vec3 cling=Vec3.ZERO,climbFrom=Vec3.ZERO,mark=Vec3.ZERO;
	private int strikes;
	private long lastAmbush=-99999,lastStrike=-99;
	private ServerPlayer cutBy;

	public HollowMatron(EntityType<? extends HollowMatron> type,Level level){super(type,level);xpReward=80;setPersistenceRequired();}
	public static AttributeSupplier.Builder createAttributes(){
		return createMobAttributes().add(Attributes.MAX_HEALTH,HollowMatronRules.HEALTH).add(Attributes.ARMOR,HollowMatronRules.ARMOUR).add(Attributes.ARMOR_TOUGHNESS,HollowMatronRules.TIER.toughness)
			.add(Attributes.MOVEMENT_SPEED,HollowMatronRules.SPEED).add(Attributes.ATTACK_DAMAGE,HollowMatronRules.TIER.melee).add(Attributes.FOLLOW_RANGE,40).add(Attributes.KNOCKBACK_RESISTANCE,1)
			.add(Attributes.STEP_HEIGHT,1.1);
	}

	/**
	 * Bind her to her thread: {@code at} where her feet hang, {@code floorY} the floor (the air above it) she drops to, {@code anchor}
	 * the height the thread hangs from, {@code yaw} the way she faces, {@code hall} the hall she keeps.
	 */
	public void bind(BlockPos at,int floorY,float anchor,float yaw,BoundingBox hall){
		bound=true;hang=at.immutable();floor=floorY;rest=yaw;entityData.set(ANCHOR,anchor);
		hallMin=new BlockPos(hall.minX(),hall.minY(),hall.minZ());hallMax=new BlockPos(hall.maxX(),hall.maxY(),hall.maxZ());
		climbed();
	}
	/** The silk pillars she climbs to ambush from (their feet, the air above the floor), and her egg sacs (the same). */
	public void nestle(List<BlockPos> pillars,List<BlockPos> sacs){this.pillars=List.copyOf(pillars);this.sacs=List.copyOf(sacs);}
	/** Client side, for blending one move into the next: the move last drawn, the one before it, when it changed, and how far into it the last one was. */
	public int shownMove=-1,shownTell,prevMove,prevTell;public float shownElapsed,prevElapsed,switchedAt=-100;
	public int move(){return entityData.get(MOVE);}
	public int tell(){return entityData.get(TELL);}
	public int side(){return entityData.get(SIDE);}
	public float anchor(){return entityData.get(ANCHOR);}
	public float elapsed(float partial){return (float)(level().getGameTime()-entityData.get(BEGAN))+partial;}
	private long since(){return level().getGameTime()-entityData.get(BEGAN);}
	/** The floor under her thread. */
	private Vec3 spot(){return new Vec3(hang.getX()+.5,floor,hang.getZ()+.5);}

	@Override protected void defineSynchedData(SynchedEntityData.Builder b){
		super.defineSynchedData(b);b.define(MOVE,HollowMatronRules.DORMANT);b.define(BEGAN,0L);b.define(TELL,0);b.define(SIDE,0);b.define(ANCHOR,0F);
	}
	@Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(MOVE.equals(key))refreshDimensions();}
	@Override protected EntityDimensions getDefaultDimensions(Pose pose){return move()==HollowMatronRules.DORMANT?HANGING:STANDING;}
	@Override protected void registerGoals(){}
	@Override public boolean isPushable(){return false;}
	@Override protected void doPush(Entity entity){}
	@Override public boolean removeWhenFarAway(double distance){return false;}
	@Override public boolean onClimbable(){return false;}

	private void pose(int move,int ticks){pose(move,ticks,0);}
	private void pose(int move,int ticks,int tell){
		int was=move();entityData.set(MOVE,move);entityData.set(BEGAN,level().getGameTime());entityData.set(TELL,tell);left=ticks;getNavigation().stop();
		if(was!=move)refreshDimensions();
		if(!HollowMatronRules.scripted(move))bar.setName(Component.translatable("boss.wildercord.hollow_matron.status",getType().getDescription(),
			Component.translatable("boss.wildercord.hollow_matron.move."+HollowMatronRules.key(move))));
	}
	/** Back up her thread under the nest, whole again. */
	private void climbed(){
		pose(HollowMatronRules.DORMANT,0);hold();setHealth(getMaxHealth());setTarget(null);bar.removeAllPlayers();quiet=0;globs.clear();pillar=null;
	}
	/** Held on her thread while she hangs, drops or climbs: as far down it as the drop has got. */
	private void hold(){
		var t=spot();
		double d=HollowMatronRules.descent(HollowMatronRules.drop(move(),(float)since()));
		setNoGravity(true);setPos(t.x,hang.getY()+(floor-hang.getY())*d,t.z);setDeltaMovement(Vec3.ZERO);face(rest);
	}
	private boolean inHall(Vec3 at,double margin){
		return HollowMatronRules.within(at.x,at.y,at.z,hallMin.getX(),hallMin.getY(),hallMin.getZ(),hallMax.getX(),hallMax.getY(),hallMax.getZ(),margin);
	}

	@Override protected void customServerAiStep(ServerLevel level){
		super.customServerAiStep(level);if(!isAlive())return;
		if(!bound){var b=blockPosition();bind(b,b.getY(),b.getY()+5,getYRot(),new BoundingBox(b.getX()-20,b.getY()-1,b.getZ()-20,b.getX()+20,b.getY()+18,b.getZ()+20));}
		// Left alone long enough (her prey died and went home, say) and she has climbed back up in the meantime.
		long now=level.getGameTime();
		if(lastTick>=0 && move()!=HollowMatronRules.DORMANT && HollowMatronRules.forgets(now-lastTick))climbed();
		lastTick=now;
		if(tickCount%HollowMatronRules.GUARD_EVERY==0 && Guardians.enabled())guard(level);
		switch(move()){
			case HollowMatronRules.DORMANT->dormant(level);
			case HollowMatronRules.DROP->{hold();dropping(level);if(--left<=0)stand(level);}
			case HollowMatronRules.CLIMB->{hold();if(wakeful(level)!=null)wake(level,HollowMatronRules.DROP_TICKS-left);else if(--left<=0)climbed();else if(left%8==0)creak(level,.8F);}
			default->{setNoGravity(HollowMatronRules.aloft(move()) || move()==HollowMatronRules.POUNCE && since()>tell());awake(level);}
		}
		flight(level);
	}

	/** Her head, more or less: low and forward on her feet, at the bottom of her while she hangs. */
	private Vec3 head(){
		if(move()==HollowMatronRules.DORMANT)return position().add(0,.4,0);
		var f=ahead(getYRot());
		return position().add(f.scale(1.4)).add(0,.9,0);
	}
	/** Her thread creaking as she hangs or climbs. */
	private void creak(ServerLevel level,float volume){Feels.sound(level,position().add(0,3,0),"aura_hollow_matron_creak",volume,.9F+random.nextFloat()*.2F);}
	/** Her hiss: the sound, and her breath stirring the dust and silk in front of her. */
	private void hiss(ServerLevel level,float volume,float pitch){
		var h=head();
		Feels.sound(level,h,"aura_hollow_matron_hiss",volume,pitch);
		var f=ahead(getYRot());level.sendParticles(ParticleTypes.ITEM_COBWEB,h.x+f.x,h.y,h.z+f.z,8,.4,.2,.4,.05);
	}

	/**
	 * The hall is hers: anything hostile that wanders or spawns in, hanging or on her feet, she webs and kills, one every
	 * {@link HollowMatronRules#GUARD_EVERY} ticks, nearest first. Only players get a fight.
	 */
	private void guard(ServerLevel level){
		var box=new AABB(Vec3.atLowerCornerOf(hallMin),Vec3.atLowerCornerOf(hallMax).add(1,1,1));
		var h=head();
		// Her brood she spares while she fights; once she is back on her thread, they go too.
		boolean fighting=move()!=HollowMatronRules.DORMANT;
		var m=level.getEntitiesOfClass(Mob.class,box,e->e instanceof Enemy && e!=this && e.isAlive() && !(e instanceof HollowMatron) && inHall(e.position(),0)
				&& !(fighting && e.entityTags().contains(BROOD)))
			.stream().min(Comparator.comparingDouble(e->e.distanceToSqr(h))).orElse(null);
		if(m==null)return;
		var to=m.position().add(0,m.getBbHeight()/2,0);
		var step=to.subtract(h);int n=Math.max(2,(int)(step.length()*2));
		for(int i=0;i<=n;i++){var at=h.add(step.scale(i/(double)n));level.sendParticles(ParticleTypes.ITEM_COBWEB,at.x,at.y,at.z,1,.05,.05,.05,0);}
		level.sendParticles(ParticleTypes.ITEM_COBWEB,to.x,to.y,to.z,16,.4,.5,.4,.05);
		Feels.sound(level,to,"aura_hollow_matron_splat",.8F,1.1F+random.nextFloat()*.2F);
		m.hurtServer(level,damageSources().mobAttack(this),Float.MAX_VALUE);
		if(m.isAlive())m.kill(level);
	}

	/** The nearest player in the hall close enough to the floor under her to wake her, if any. */
	private ServerPlayer wakeful(ServerLevel level){
		var t=spot();
		return level.players().stream().filter(p->p.isAlive() && !p.isCreative() && !p.isSpectator() && inHall(p.position(),0)
			&& HollowMatronRules.wakes(p.getX()-t.x,p.getY()-t.y,p.getZ()-t.z)).min(Comparator.comparingDouble(p->p.distanceToSqr(t))).orElse(null);
	}

	private void dormant(ServerLevel level){
		hold();if(getHealth()<getMaxHealth())setHealth(getMaxHealth());
		if(tickCount%5==0 && Guardians.enabled() && wakeful(level)!=null)wake(level,0);
		else if(random.nextInt(140)==0)creak(level,.6F);
		else if(random.nextInt(200)==0)Feels.sound(level,head(),"aura_hollow_matron_chitter",.6F,.9F+random.nextFloat()*.2F);
	}

	/** She lets go and drops, {@code already} ticks in (when she is woken half way back up her thread). */
	private void wake(ServerLevel level,int already){
		pose(HollowMatronRules.DROP,HollowMatronRules.DROP_TICKS-already);entityData.set(BEGAN,level.getGameTime()-already);
		Feels.sound(level,position().add(0,2,0),"aura_hollow_matron_drop",1.4F,1);
		level.sendParticles(ParticleTypes.ITEM_COBWEB,getX(),getY()+3,getZ(),20,.6,.6,.6,.05);
	}
	/** The drop: landing as she reaches the floor, turning over, then the hiss as she rears up. */
	private void dropping(ServerLevel level){
		long el=since();
		if(el==Math.round(HollowMatronRules.DROP_TICKS*HollowMatronRules.FALL_END)){
			Feels.sound(level,position(),"aura_hollow_matron_land",1.6F,1);
			level.sendParticles(ParticleTypes.POOF,getX(),getY()+.2,getZ(),20,1.4,.1,1.4,.03);
			level.sendParticles(ParticleTypes.ITEM_COBWEB,getX(),getY()+.5,getZ(),24,1.2,.4,1.2,.05);
		}
		if(el==HollowMatronRules.HISS_AT)hiss(level,1.8F,.9F);
	}

	/** On her feet: the fight is on. */
	private void stand(ServerLevel level){
		pose(HollowMatronRules.WATCH,HollowMatronRules.GAP);hold();setNoGravity(false);
		lastCut=level.getGameTime();quiet=0;
		Feels.sound(level,position(),"aura_hollow_matron_step",1.2F,.9F);
	}

	private void awake(ServerLevel level){
		if(!inHall(position(),HollowMatronRules.LEASH)){
			var at=spot();teleportTo(at.x,at.y,at.z);setDeltaMovement(Vec3.ZERO);getNavigation().stop();setNoGravity(false);pose(HollowMatronRules.WATCH,HollowMatronRules.GAP);
		}
		long now=level.getGameTime();float heal=HollowMatronRules.regen(getMaxHealth(),now-lastCut);if(heal>0 && getHealth()<getMaxHealth())heal(heal);
		if(tickCount%10==0){
			bar.setProgress(getHealth()/getMaxHealth());
			for(var p:new ArrayList<>(bar.getPlayers()))if(p.level()!=level || !p.isAlive() || !inHall(p.position(),HollowMatronRules.BAR_DROP))bar.removePlayer(p);
			for(var p:level.players())if(p.isAlive() && !p.isSpectator() && inHall(p.position(),HollowMatronRules.BAR_SHOW))bar.addPlayer(p);
			setTarget(level.players().stream().filter(p->p.isAlive() && !p.isCreative() && !p.isSpectator() && inHall(p.position(),0))
				.min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null));
		}
		stung*=HollowMatronRules.STING_FADE;
		var target=getTarget();
		switch(move()){
			case HollowMatronRules.BITE->bite(level);
			case HollowMatronRules.SWEEP->sweep(level);
			case HollowMatronRules.SPIT->spit(level,target);
			case HollowMatronRules.POUNCE->pounce(level,target);
			case HollowMatronRules.SKITTER->{face(aim);move(MoverType.SELF,new Vec3(slide.x,-.3,slide.z));setDeltaMovement(Vec3.ZERO);if(--left<=0)pose(HollowMatronRules.WATCH,2);}
			case HollowMatronRules.HASTE->making(level);
			case HollowMatronRules.SCALE->scaling(level);
			case HollowMatronRules.LURK->lurking(level,target);
			case HollowMatronRules.PLUNGE->plunging(level);
			case HollowMatronRules.TUMBLE->tumbling(level);
			case HollowMatronRules.RECOVER->{still();if(--left<=0)pose(HollowMatronRules.WATCH,gap());else if(left%14==0)Feels.sound(level,head(),"aura_hollow_matron_chitter",.8F,.8F);}
			default->watch(level,target);
		}
	}

	/** On her feet and tracking: closing on her prey, wheeling quickly, but not so quickly her abdomen can't be got at. */
	private void watch(ServerLevel level,LivingEntity target){
		if(getDeltaMovement().horizontalDistanceSqr()>.0004 && tickCount%5==0)Feels.sound(level,position(),"aura_hollow_matron_step",.9F,.9F+random.nextFloat()*.3F);
		if(target==null){
			quiet++;
			var at=spot();
			if(quiet>=HollowMatronRules.RETURN_TICKS && distanceToSqr(at)>1.5)getNavigation().moveTo(at.x,at.y,at.z,1);
			if(quiet>=HollowMatronRules.RESET_TICKS && (distanceToSqr(at)<=2.5 || quiet>=HollowMatronRules.RESET_TICKS+100)){
				bar.removeAllPlayers();pose(HollowMatronRules.CLIMB,HollowMatronRules.DROP_TICKS);hold();creak(level,1);
			}
			return;
		}
		quiet=0;
		getLookControl().setLookAt(target,30,30);
		double d=distanceTo(target);
		if(left>0)left--;
		if(left<=0){
			long now=level.getGameTime();
			if(HollowMatronRules.ambushes(getHealth(),getMaxHealth(),now-lastAmbush) && haste(level))return;
			int pick=HollowMatronRules.choose(d,random.nextDouble(),now-lastSweep>=HollowMatronRules.SWEEP_REST,now-lastPounce>=HollowMatronRules.POUNCE_REST,now-lastSpit>=HollowMatronRules.SPIT_REST);
			// A long chase ends in web spat after whoever is running.
			if(pick<0 && chased>=60 && d<=HollowMatronRules.GLOB_RANGE-2 && now-lastSpit>=HollowMatronRules.SPIT_REST/2)pick=HollowMatronRules.SPIT;
			if(pick>=0){
				chased=0;aim=yawTo(target.position());
				switch(pick){
					case HollowMatronRules.SWEEP->{
						lastSweep=now;entityData.set(SIDE,random.nextBoolean()?1:-1);pose(HollowMatronRules.SWEEP,0,HollowMatronRules.SWEEP_TELL);
						hiss(level,1.2F,1.15F);
					}
					case HollowMatronRules.SPIT->{lastSpit=now;pose(HollowMatronRules.SPIT,0,HollowMatronRules.SPIT_TELL);Feels.sound(level,head(),"aura_hollow_matron_chitter",1.2F,1.1F);}
					case HollowMatronRules.POUNCE->{lastPounce=now;pose(HollowMatronRules.POUNCE,0,HollowMatronRules.POUNCE_TELL);Feels.sound(level,head(),"aura_hollow_matron_chitter",1.3F,.8F);}
					default->{pose(HollowMatronRules.BITE,0,HollowMatronRules.BITE_TELL);hiss(level,1.1F,1.3F);}
				}
				return;
			}
		}
		if(d>HollowMatronRules.CLOSE){
			chased++;
			if(!getNavigation().moveTo(target,1.1))getMoveControl().setWantedPosition(target.getX(),target.getY(),target.getZ(),1.1);
		}else{chased=0;getNavigation().stop();turn(yawTo(target.position()),HollowMatronRules.TURN);}
	}

	/** Reared back with her fangs spread through the tell, then a lunge forward and a bite at whoever is in front of her. */
	private void bite(ServerLevel level){
		face(aim);
		long el=since();int tell=tell();
		if(el<=tell){still();return;}
		if(el<=tell+HollowMatronRules.LUNGE_TICKS){
			var step=ahead(aim).scale(HollowMatronRules.LUNGE/HollowMatronRules.LUNGE_TICKS);
			move(MoverType.SELF,new Vec3(step.x,-.3,step.z));setDeltaMovement(Vec3.ZERO);
		}else still();
		if(el==tell+HollowMatronRules.LUNGE_TICKS){
			Feels.sound(level,head(),"aura_hollow_matron_bite",1.4F,.9F+random.nextFloat()*.2F);
			var h=head();level.sendParticles(ParticleTypes.CRIT,h.x,h.y,h.z,10,.4,.3,.4,.2);
			for(var p:prey(level))if(reaches(HollowMatronRules.BITE,p))hit(level,p,HollowMatronRules.BITE_DAMAGE,.6,position(),.2);
		}
		if(el>=tell+HollowMatronRules.LUNGE_TICKS+2)spent(HollowMatronRules.BITE_RECOVER);
	}
	/** Her forelegs raised and drawn to one side through the tell, then swept across in front of her through whoever is near. */
	private void sweep(ServerLevel level){
		face(aim);still();
		long el=since();int tell=tell();
		if(el==tell)Feels.sound(level,head(),"aura_hollow_matron_sweep",1.4F,.9F+random.nextFloat()*.2F);
		if(el==tell+HollowMatronRules.SWEEP_TICKS/2){
			var f=ahead(aim);
			for(int i=-4;i<=4;i++){
				double a=Math.toRadians(aim)+i*.3,r=HollowMatronRules.SWEEP_REACH*.8;
				level.sendParticles(ParticleTypes.SWEEP_ATTACK,getX()-Math.sin(a)*r,getY()+1,getZ()+Math.cos(a)*r,1,0,0,0,0);
			}
			int side=side();var across=new Vec3(-f.z*side,0,f.x*side);
			for(var p:prey(level))if(reaches(HollowMatronRules.SWEEP,p))hit(level,p,HollowMatronRules.SWEEP_DAMAGE,1.1,p.position().subtract(across),.3);
		}
		if(el>=tell+HollowMatronRules.SWEEP_TICKS)spent(HollowMatronRules.SWEEP_RECOVER);
	}
	/** Reared up tracking her prey through the tell, the aim locked for the last three ticks; then a glob of web spat along it. */
	private void spit(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell-3 && target!=null)aim=yawTo(target.position());
		face(aim);still();
		if(el==tell){
			var h=head().add(0,.4,0);
			Vec3 to=target!=null?target.position().add(0,target.getBbHeight()*.5,0):h.add(ahead(aim).scale(10));
			// Along the locked line, but at her prey's height.
			var flat=ahead(aim);double reach=Math.max(2,Math.sqrt(to.subtract(h).horizontalDistanceSqr()));
			var dir=new Vec3(flat.x*reach,to.y-h.y,flat.z*reach).normalize();
			globs.add(new Glob(h,dir.scale(HollowMatronRules.GLOB_SPEED),HollowMatronRules.GLOB_RANGE));
			Feels.sound(level,h,"aura_hollow_matron_spit",1.4F,.9F+random.nextFloat()*.2F);
			level.sendParticles(ParticleTypes.ITEM_COBWEB,h.x,h.y,h.z,10,.2,.2,.2,.1);
		}
		if(el>=tell+2)spent(HollowMatronRules.SPIT_RECOVER);
	}
	/** Her globs of web flying, and splatting on whoever (or whatever) they hit. */
	private void flight(ServerLevel level){
		if(globs.isEmpty())return;
		for(var it=globs.iterator();it.hasNext();){
			var g=it.next();
			var next=g.at.add(g.step);
			var hitBlock=level.clip(new ClipContext(g.at,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
			ServerPlayer struck=null;
			for(var p:prey(level)){
				var c=p.position().add(0,p.getBbHeight()*.5,0);
				if(nearest(g.at,next,c).distanceToSqr(c)<=HollowMatronRules.GLOB_RADIUS*HollowMatronRules.GLOB_RADIUS){struck=p;break;}
			}
			for(int i=0;i<3;i++){var at=g.at.add(g.step.scale(i/3.0));level.sendParticles(ParticleTypes.ITEM_COBWEB,at.x,at.y,at.z,2,.08,.08,.08,0);}
			if(struck!=null){
				splat(level,struck.position().add(0,struck.getBbHeight()*.5,0));
				if(GuardianBite.land(level,this,struck,(float)HollowMatronRules.SPIT_DAMAGE,HollowMatronRules.PIERCE))
					struck.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,HollowMatronRules.WEB_SLOW_TICKS,2,false,true),this);
				it.remove();continue;
			}
			if(hitBlock.getType()!=HitResult.Type.MISS){splat(level,hitBlock.getLocation());it.remove();continue;}
			g.at=next;g.left-=g.step.length();
			if(g.left<=0){splat(level,g.at);it.remove();}
		}
	}
	private void splat(ServerLevel level,Vec3 at){
		Feels.sound(level,at,"aura_hollow_matron_splat",1.2F,.9F+random.nextFloat()*.2F);
		level.sendParticles(ParticleTypes.ITEM_COBWEB,at.x,at.y,at.z,24,.5,.5,.5,.08);
	}
	/** The point on the line from {@code a} to {@code b} nearest {@code c}. */
	private static Vec3 nearest(Vec3 a,Vec3 b,Vec3 c){
		var ab=b.subtract(a);double len=ab.lengthSqr();if(len<1e-6)return a;
		double t=Math.max(0,Math.min(1,c.subtract(a).dot(ab)/len));return a.add(ab.scale(t));
	}

	/**
	 * Crouched low on bent legs through the tell while she tracks her prey, the spot locked for the last three ticks; then a leap at
	 * it in an arc, landing hard on whoever is under her. After it she is spent a long while.
	 */
	private void pounce(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell){
			still();
			if(el<=tell-3 && target!=null){
				aim=yawTo(target.position());
				var to=target.position();var off=to.subtract(position()).multiply(1,0,1);
				if(off.length()>HollowMatronRules.POUNCE_REACH)to=position().add(off.normalize().scale(HollowMatronRules.POUNCE_REACH));
				leapTo=new Vec3(to.x,getY(),to.z);
			}
			face(aim);
			if(el==tell){
				leapFrom=position();if(!inHall(leapTo,0))leapTo=leapFrom;
				Feels.sound(level,position(),"aura_hollow_matron_pounce",1.5F,.9F+random.nextFloat()*.2F);
				level.sendParticles(ParticleTypes.POOF,getX(),getY()+.1,getZ(),12,1,.1,1,.03);
			}
			return;
		}
		face(aim);
		int tick=(int)(el-tell);
		double k=Math.min(1,tick/(double)HollowMatronRules.POUNCE_TICKS);
		var want=leapFrom.add(leapTo.subtract(leapFrom).scale(k)).add(0,HollowMatronRules.arc(tick,HollowMatronRules.POUNCE_TICKS),0);
		move(MoverType.SELF,want.subtract(position()));setDeltaMovement(Vec3.ZERO);
		if(tick<HollowMatronRules.POUNCE_TICKS)return;
		setNoGravity(false);
		Feels.sound(level,position(),"aura_hollow_matron_land",1.6F,1.1F);
		level.sendParticles(ParticleTypes.POOF,getX(),getY()+.2,getZ(),24,1.4,.1,1.4,.04);
		for(var p:prey(level))if(reaches(HollowMatronRules.POUNCE,p))hit(level,p,HollowMatronRules.POUNCE_DAMAGE,1.2,position(),.4);
		spent(HollowMatronRules.POUNCE_RECOVER);
	}

	// ---- the ambush.
	/** The top of the silk pillar standing on {@code foot}, or -1 if it has been cut through near the floor or is too short to climb. */
	private int topOf(Level level,BlockPos foot){
		if(!level.getBlockState(foot).is(SILK.getBlock()))return -1;
		var m=foot.mutable();
		while(m.getY()<hallMax.getY()+2 && level.getBlockState(m.above()).is(SILK.getBlock()))m.move(0,1,0);
		return m.getY()-foot.getY()>=8?m.getY():-1;
	}
	/** Whether her pillar still stands as high as she climbed it. */
	private boolean intact(){return pillar!=null && topOf(level(),pillar)>=top;}
	private Vec3 pillarAt(){return new Vec3(pillar.getX()+.5,pillar.getY(),pillar.getZ()+.5);}
	/** Off to the nearest standing pillar; false if none stands. */
	private boolean haste(ServerLevel level){
		lastAmbush=level.getGameTime();
		BlockPos best=null;int bestTop=-1;double far=Double.MAX_VALUE;
		for(var foot:pillars){
			int t=topOf(level,foot);if(t<0)continue;
			double d=position().distanceToSqr(foot.getX()+.5,foot.getY(),foot.getZ()+.5);
			if(d<far){far=d;best=foot;bestTop=t;}
		}
		if(best==null)return false;
		pillar=best;top=bestTop;strikes=0;
		// She clings to the side of the silk facing into the hall.
		var c=pillarAt();var in=spot().subtract(c).multiply(1,0,1);
		in=in.lengthSqr()<1e-4?new Vec3(1,0,0):in.normalize();
		cling=c.add(in.scale(HollowMatronRules.CLING));
		pose(HollowMatronRules.HASTE,HollowMatronRules.HASTE_TICKS);
		hiss(level,1.5F,.8F);
		return true;
	}
	/** Running for her pillar; climbing from wherever she has got to once she is there, or once she has run long enough. */
	private void making(ServerLevel level){
		if(--left<=0 || position().subtract(cling.x,getY(),cling.z).horizontalDistanceSqr()<=3*3){
			climbFrom=position();pose(HollowMatronRules.SCALE,HollowMatronRules.SCALE_TICKS);setNoGravity(true);
			Feels.sound(level,position().add(0,1,0),"aura_hollow_matron_climb",1.4F,1);
			return;
		}
		if(tickCount%3==0)getNavigation().moveTo(cling.x,floor,cling.z,1.5);
		if(tickCount%4==0)Feels.sound(level,position(),"aura_hollow_matron_step",1,1.1F+random.nextFloat()*.2F);
	}
	/** Up the pillar: onto the silk in the first third, and on up it, quick. */
	private void scaling(ServerLevel level){
		long el=since();
		if(el%5==0 && !intact()){tumble(level,null);return;}
		double k=Math.min(1,el/(double)HollowMatronRules.SCALE_TICKS),on=Math.min(1,k*3),up=k*k*(3-2*k);
		double y=climbFrom.y+(top-HollowMatronRules.CLING_DROP-climbFrom.y)*up;
		setPos(climbFrom.x+(cling.x-climbFrom.x)*on,y,climbFrom.z+(cling.z-climbFrom.z)*on);setDeltaMovement(Vec3.ZERO);
		face(yawTo(pillarAt()));
		if(el%6==3){
			Feels.sound(level,position().add(0,2,0),"aura_hollow_matron_climb",1.1F,.9F+random.nextFloat()*.2F);
			level.sendParticles(ParticleTypes.ITEM_COBWEB,cling.x,getY()+2,cling.z,6,.4,.6,.4,.03);
		}
		if(el<HollowMatronRules.SCALE_TICKS)return;
		var target=getTarget();
		mark=target!=null?new Vec3(target.getX(),floor,target.getZ()):spot();
		pose(HollowMatronRules.LURK,HollowMatronRules.LURK_TICKS,HollowMatronRules.LURK_TICKS-HollowMatronRules.LURK_TELL);
		hiss(level,1.6F,.85F);
		hatch(level,target);
	}
	/**
	 * Head-down at the top of her pillar, turning to watch her prey and marking the spot under them; for the last of it the spot is
	 * fixed and dust sifts down onto it from where she will come. Then she lets go.
	 */
	private void lurking(ServerLevel level,LivingEntity target){
		long el=since();int coil=tell();
		setDeltaMovement(Vec3.ZERO);
		if(el%5==0 && !intact()){tumble(level,null);return;}
		if(el<coil && target!=null)mark=new Vec3(target.getX(),floor,target.getZ());
		if(!inHall(mark,0))mark=spot();
		turn(yawTo(mark),24);
		if(el==coil)Feels.sound(level,head(),"aura_hollow_matron_chitter",1.6F,.75F);
		if(el>=coil){
			var dust=new BlockParticleOption(ParticleTypes.FALLING_DUST,SILK);
			level.sendParticles(dust,mark.x,getY()+1,mark.z,5,1.2,.4,1.2,0);
			level.sendParticles(ParticleTypes.ITEM_COBWEB,mark.x,mark.y+.1,mark.z,2,1.4,0,1.4,0);
			if((el-coil)%7==0)creak(level,1.1F);
		}else if(el%12==0)Feels.sound(level,head(),"aura_hollow_matron_chitter",.9F,.9F+random.nextFloat()*.2F);
		if(el<HollowMatronRules.LURK_TICKS)return;
		climbFrom=position();aim=yawTo(mark);
		pose(HollowMatronRules.PLUNGE,HollowMatronRules.PLUNGE_TICKS);
		Feels.sound(level,position().add(0,1,0),"aura_hollow_matron_drop",1.6F,1.1F);
	}
	/** Off the pillar and down onto the marked spot, landing on whoever is there. */
	private void plunging(ServerLevel level){
		long el=since();face(aim);
		double k=Math.min(1,el/(double)HollowMatronRules.PLUNGE_TICKS),down=HollowMatronRules.plunge(k);
		setPos(climbFrom.x+(mark.x-climbFrom.x)*k,climbFrom.y+(mark.y-climbFrom.y)*down,climbFrom.z+(mark.z-climbFrom.z)*k);setDeltaMovement(Vec3.ZERO);
		if(el<HollowMatronRules.PLUNGE_TICKS)return;
		setNoGravity(false);pillar=null;
		Feels.sound(level,position(),"aura_hollow_matron_land",2F,.9F);
		level.sendParticles(ParticleTypes.POOF,getX(),getY()+.2,getZ(),30,1.8,.1,1.8,.05);
		level.sendParticles(ParticleTypes.ITEM_COBWEB,getX(),getY()+.5,getZ(),24,1.6,.4,1.6,.05);
		for(var p:prey(level))if(reaches(HollowMatronRules.PLUNGE,p))hit(level,p,HollowMatronRules.PLUNGE_DAMAGE,1.4,position(),.5);
		spent(HollowMatronRules.PLUNGE_RECOVER);
	}
	/** Her brood: spiderlings out of the egg sacs, a few strides in from each toward the middle of the hall, after her prey. */
	private void hatch(ServerLevel level,LivingEntity target){
		if(sacs.isEmpty())return;
		var box=new AABB(Vec3.atLowerCornerOf(hallMin),Vec3.atLowerCornerOf(hallMax).add(1,1,1));
		int alive=level.getEntitiesOfClass(Mob.class,box,e->e.isAlive() && e.entityTags().contains(BROOD)).size();
		int n=HollowMatronRules.brood(prey(level).size())-alive;
		var order=new ArrayList<>(sacs);Collections.shuffle(order,new Random(random.nextLong()));
		for(int i=0;i<n;i++){
			var sac=order.get(i%order.size());
			var c=spot();var in=new Vec3(c.x-(sac.getX()+.5),0,c.z-(sac.getZ()+.5));
			in=in.lengthSqr()<1e-4?Vec3.ZERO:in.normalize();
			var at=new Vec3(sac.getX()+.5+in.x*2,sac.getY(),sac.getZ()+.5+in.z*2);
			var spider=EntityTypes.CAVE_SPIDER.create(level,EntitySpawnReason.MOB_SUMMONED);if(spider==null)continue;
			spider.snapTo(at.x,at.y,at.z,random.nextFloat()*360,0);
			spider.finalizeSpawn(level,level.getCurrentDifficultyAt(BlockPos.containing(at)),EntitySpawnReason.MOB_SUMMONED,null);
			spider.addTag(BROOD);
			if(target!=null)spider.setTarget(target);
			level.addFreshEntity(spider);
			level.sendParticles(ParticleTypes.ITEM_COBWEB,sac.getX()+.5,sac.getY()+1,sac.getZ()+.5,24,.7,.6,.7,.08);
			Feels.sound(level,Vec3.atCenterOf(sac),"aura_hollow_matron_hatch",1.3F,.9F+random.nextFloat()*.3F);
		}
	}
	/** Someone striking silk near her: if it is her pillar's, while she is on it, enough strikes cut her down. */
	private void struck(ServerLevel level,ServerPlayer by,BlockPos at){
		int move=move();
		if(pillar==null || (move!=HollowMatronRules.SCALE && move!=HollowMatronRules.LURK) || at.getX()!=pillar.getX() || at.getZ()!=pillar.getZ())return;
		if(at.getY()<pillar.getY() || at.getY()>top)return;
		long now=level.getGameTime();if(now-lastStrike<HollowMatronRules.STRIKE_GAP)return;
		lastStrike=now;strikes++;
		Feels.sound(level,Vec3.atCenterOf(at),"aura_hollow_matron_tear",1.3F,.9F+strikes*.1F);
		level.sendParticles(ParticleTypes.ITEM_COBWEB,at.getX()+.5,at.getY()+.5,at.getZ()+.5,14,.5,.5,.5,.08);
		// The whole pillar shudders up to her.
		var c=pillarAt();for(int y=at.getY();y<=top;y+=2)level.sendParticles(ParticleTypes.ITEM_COBWEB,c.x,y+.5,c.z,2,.4,.2,.4,.02);
		if(HollowMatronRules.cut(strikes))tumble(level,by);
		else hiss(level,1.2F,1.2F+strikes*.1F);
	}
	/** Cut down (or her pillar gone from under her): off it, falling, legs flailing. */
	private void tumble(ServerLevel level,ServerPlayer by){
		cutBy=by;
		var away=pillar==null?Vec3.ZERO:position().subtract(pillarAt()).multiply(1,0,1);
		away=away.lengthSqr()<1e-4?Vec3.ZERO:away.normalize().scale(.25);
		pillar=null;pose(HollowMatronRules.TUMBLE,HollowMatronRules.TUMBLE_MAX);setNoGravity(false);setDeltaMovement(away.x,.1,away.z);
		Feels.sound(level,head(),"aura_hollow_matron_hurt",1.8F,.7F);
		Feels.sound(level,position(),"aura_hollow_matron_tear",1.6F,.7F);
		level.sendParticles(ParticleTypes.ITEM_COBWEB,getX(),getY()+1,getZ(),30,1,1,1,.1);
	}
	/** Falling till she hits the floor; then she lies stunned on her splayed legs, hurt by the fall, a long while. */
	private void tumbling(ServerLevel level){
		if(!(onGround() && since()>2) && --left>0)return;
		Feels.sound(level,position(),"aura_hollow_matron_land",2F,.75F);
		level.sendParticles(ParticleTypes.POOF,getX(),getY()+.2,getZ(),30,1.6,.1,1.6,.05);
		spent(HollowMatronRules.CUT_RECOVER);
		stung=0;lastBack=level.getGameTime();
		var by=cutBy;cutBy=null;
		hurtServer(level,by!=null && by.isAlive()?damageSources().playerAttack(by):damageSources().generic(),(float)HollowMatronRules.CUT_DAMAGE);
	}
	/** Listening for players striking silk, for whoever is up a pillar. */
	public static void listen(){
		AttackBlockCallback.EVENT.register((player,level,hand,pos,dir)->{
			if(level instanceof ServerLevel server && player instanceof ServerPlayer p && !p.isSpectator() && level.getBlockState(pos).is(SILK.getBlock()))
				for(var m:server.getEntitiesOfClass(HollowMatron.class,new AABB(pos).inflate(24)))m.struck(server,p,pos);
			return InteractionResult.PASS;
		});
	}

	/** Spent after an attack, low on splayed legs, for {@code ticks}. */
	private void spent(int ticks){pose(HollowMatronRules.RECOVER,ticks);still();}
	/** Out from under a blow, to one side, quick on her many feet. */
	private void skitter(ServerLevel level){
		aim=getYRot();int side=random.nextBoolean()?1:-1;var f=ahead(aim);
		slide=new Vec3(-f.z*side,0,f.x*side).scale(HollowMatronRules.SKITTER_SPEED).add(f.scale(-.1));
		entityData.set(SIDE,side);pose(HollowMatronRules.SKITTER,HollowMatronRules.SKITTER_TICKS,HollowMatronRules.SKITTER_TICKS);
		Feels.sound(level,position(),"aura_hollow_matron_skitter",1.2F,.9F+random.nextFloat()*.2F);
	}
	/** Hurt too fast: she backs away from whoever is hurting her, toward the middle of the hall, hissing. */
	private void backOff(ServerLevel level,Vec3 from){
		lastBack=level.getGameTime();stung=0;
		var away=from==null?ahead(getYRot()).scale(-1):new Vec3(getX()-from.x,0,getZ()-from.z);
		var home=spot().subtract(position()).multiply(1,0,1);
		if(home.lengthSqr()>16)away=away.normalize().add(home.normalize());
		if(away.lengthSqr()<1e-4)away=ahead(getYRot()).scale(-1);
		aim=getYRot();slide=away.normalize().scale(HollowMatronRules.BACK_SPEED);
		var f=ahead(aim);double side=slide.x*-f.z+slide.z*f.x;
		entityData.set(SIDE,Math.abs(side)<.3?0:side>0?1:-1);pose(HollowMatronRules.SKITTER,HollowMatronRules.BACK_TICKS,HollowMatronRules.BACK_TICKS);
		Feels.sound(level,position(),"aura_hollow_matron_skitter",1.3F,.85F);
		hiss(level,1.3F,1.1F);
	}
	/** Standing her ground: no sliding about, though she still falls. */
	private void still(){getNavigation().stop();var v=getDeltaMovement();setDeltaMovement(0,Math.min(0,v.y),0);}
	private boolean wounded(){return HollowMatronRules.wounded(getHealth(),getMaxHealth());}
	/** Ticks before her next blow. */
	private int gap(){return (wounded()?HollowMatronRules.GAP_WOUNDED:HollowMatronRules.GAP)+random.nextInt(3);}

	/** Players she could be fighting right now. */
	private List<ServerPlayer> prey(ServerLevel level){
		var out=new ArrayList<ServerPlayer>();
		for(var p:level.players())if(p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(this)<24*24 && inHall(p.position(),HollowMatronRules.LEASH))out.add(p);
		return out;
	}
	/** Whether {@code move}, along the locked aim, reaches {@code p}. */
	private boolean reaches(int move,LivingEntity p){
		var f=ahead(aim);double dx=p.getX()-getX(),dz=p.getZ()-getZ();
		return HollowMatronRules.hits(move,dx*f.x+dz*f.z,dx*-f.z+dz*f.x,p.getY()-getY());
	}
	/** A blow landing on {@code p}, through part of its armour, knocking it {@code shove} away from {@code from} and {@code lift} up. */
	private void hit(ServerLevel level,LivingEntity p,double damage,double shove,Vec3 from,double lift){
		if(!GuardianBite.land(level,this,p,(float)damage,HollowMatronRules.PIERCE))return;
		if(shove>0)p.knockback(shove,from.x-p.getX(),from.z-p.getZ(),level.damageSources().mobAttack(this),0);
		if(lift>0)p.setDeltaMovement(p.getDeltaMovement().add(0,lift,0));
		p.needsSync=true;
	}
	private Vec3 ahead(float yaw){double r=Math.toRadians(yaw);return new Vec3(-Math.sin(r),0,Math.cos(r));}

	private float yawTo(Vec3 at){return (float)Math.toDegrees(Math.atan2(-(at.x-getX()),at.z-getZ()));}
	private void face(float yaw){setYRot(yaw);yBodyRot=yaw;yHeadRot=yaw;}
	/** Turned toward {@code yaw} by at most {@code most} degrees. */
	private void turn(float yaw,float most){
		float d=net.minecraft.util.Mth.wrapDegrees(yaw-getYRot());face(getYRot()+Math.max(-most,Math.min(most,d)));
	}

	@Override public boolean hurtServer(ServerLevel level,DamageSource source,float damage){
		if(source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.CRAMMING))return false;
		int move=move();
		if(HollowMatronRules.scripted(move) && !source.is(DamageTypes.GENERIC_KILL)){
			// Hanging, she draws up out of reach; but a blow, or a shot, brings her down.
			if(move==HollowMatronRules.DORMANT && Guardians.enabled() && source.getEntity() instanceof ServerPlayer p && !p.isCreative())wake(level,0);
			return false;
		}
		long now=level.getGameTime();
		if(!source.is(DamageTypes.GENERIC_KILL)){
			var at=source.getSourcePosition();
			double angle=at==null?0:yawTo(at)-getYRot();
			// She feels a blow coming through her legs and skitters out from under it.
			if(source.getDirectEntity()!=null && source.getDirectEntity()==source.getEntity() && HollowMatronRules.dodges(move,now-lastDodge,random.nextDouble())){
				lastDodge=now;skitter(level);
				return false;
			}
			damage*=(float)HollowMatronRules.armour(move,at!=null && HollowMatronRules.frontal(angle),at!=null && HollowMatronRules.rear(angle));
		}
		boolean hurt=super.hurtServer(level,source,damage);
		if(hurt){
			lastCut=now;stung+=damage;
			if(HollowMatronRules.backs(move(),stung,now-lastBack))backOff(level,source.getSourcePosition());
		}
		return hurt;
	}
	@Override public void die(DamageSource source){
		if(level() instanceof ServerLevel server){Feels.sound(server,head(),"aura_hollow_matron_death",1.8F,1);bar.removeAllPlayers();setNoGravity(false);}
		super.die(source);
	}
	@Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return null;}
	@Override protected void actuallyHurt(ServerLevel level,DamageSource source,float damage){
		super.actuallyHurt(level,source,damage);
		if(random.nextInt(3)==0)Feels.sound(level,head(),"aura_hollow_matron_hurt",1.2F,.9F+random.nextFloat()*.2F);
	}
	@Override public void remove(RemovalReason reason){bar.removeAllPlayers();super.remove(reason);}
	@Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);bar.removePlayer(player);}

	@Override protected void addAdditionalSaveData(ValueOutput out){
		super.addAdditionalSaveData(out);
		out.putBoolean("matron_bound",bound);out.store("matron_hang",BlockPos.CODEC,hang);out.putInt("matron_floor",floor);out.putFloat("matron_anchor",anchor());
		out.putFloat("matron_rest",rest);
		out.store("matron_hall_min",BlockPos.CODEC,hallMin);out.store("matron_hall_max",BlockPos.CODEC,hallMax);
		out.store("matron_pillars",BlockPos.CODEC.listOf(),pillars);out.store("matron_sacs",BlockPos.CODEC.listOf(),sacs);
		out.putBoolean("matron_awake",!HollowMatronRules.scripted(move()));out.putLong("matron_ticked",lastTick);
	}
	@Override protected void readAdditionalSaveData(ValueInput in){
		super.readAdditionalSaveData(in);
		bound=in.getBooleanOr("matron_bound",false);
		hang=in.read("matron_hang",BlockPos.CODEC).orElse(blockPosition());floor=in.getIntOr("matron_floor",hang.getY());
		entityData.set(ANCHOR,in.getFloatOr("matron_anchor",hang.getY()+5));rest=in.getFloatOr("matron_rest",getYRot());
		hallMin=in.read("matron_hall_min",BlockPos.CODEC).orElse(hang);hallMax=in.read("matron_hall_max",BlockPos.CODEC).orElse(hang);
		lastTick=in.getLongOr("matron_ticked",-1L);
		pillars=in.read("matron_pillars",BlockPos.CODEC.listOf()).orElse(List.of());sacs=in.read("matron_sacs",BlockPos.CODEC.listOf()).orElse(List.of());
		// Mid-fight she comes back on her feet, whatever she was doing when the chunk unloaded.
		if(in.getBooleanOr("matron_awake",false)){pose(HollowMatronRules.WATCH,HollowMatronRules.GAP);setNoGravity(false);lastCut=0;}else{pose(HollowMatronRules.DORMANT,0);setNoGravity(true);}
	}
}
