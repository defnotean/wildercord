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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import java.util.*;

/**
 * The Frost Wyrmling ({@link FrostWyrmlingRules}). Generation leaves it curled up on the snow mound at the back of the Glacier Den's
 * hall, bound to that spot and to the hall, facing the crevasse. Curled up it cannot be hurt; the first player far enough into the
 * hall (or the first blow) wakes it, and it uncurls, gets its legs under it and throws its wings wide with a roar, and then the boss
 * bar comes up. The hall going quiet sends it back to its mound, healed. The hall is its own: anything hostile in it, it freezes
 * where it stands.
 * <p>
 * On its feet it snaps at whoever is in front of it, sweeps its tail round at whoever gets to its flank, beats its wings to throw
 * back anyone crowding it, breathes a cone of frost at anyone a little way off that chills, slows and freezes, and pounces on anyone
 * keeping well away. It hops aside out from under some blows, and hurt too fast it beats away back across the hall. After a pounce
 * it lands heavy and stands blowing: the big opening.
 */
public final class FrostWyrmling extends PathfinderMob implements Enemy {
	private static final EntityDataAccessor<Integer> MOVE=SynchedEntityData.defineId(FrostWyrmling.class,EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN=SynchedEntityData.defineId(FrostWyrmling.class,EntityDataSerializers.LONG);
	/** The current move's tell in ticks, so the client knows when the blow lands. */
	private static final EntityDataAccessor<Integer> TELL=SynchedEntityData.defineId(FrostWyrmling.class,EntityDataSerializers.INT);
	/** Which way a bound or a tail sweep goes, for the client: -1 left, 1 right, 0 straight back. */
	private static final EntityDataAccessor<Integer> SIDE=SynchedEntityData.defineId(FrostWyrmling.class,EntityDataSerializers.INT);
	private static final EntityDimensions CURLED=EntityDimensions.scalable(2.8F,1.6F).withEyeHeight(1.2F),STANDING=EntityDimensions.scalable(2.8F,3.0F).withEyeHeight(2.6F);
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
	/** A pounce: where it left the ground and where it comes down. */
	private Vec3 from=Vec3.ZERO,to=Vec3.ZERO;
	private int chased;
	private long lastDodge=-999,lastLeap=-999,lastTail=-999,lastWing=-999,lastBreath=-999,lastPounce=-999;
	/** Hurt taken lately, fading; enough of it sends it beating away. */
	private double stung;

	public FrostWyrmling(EntityType<? extends FrostWyrmling> type,Level level){super(type,level);xpReward=80;setPersistenceRequired();}
	public static AttributeSupplier.Builder createAttributes(){
		return createMobAttributes().add(Attributes.MAX_HEALTH,FrostWyrmlingRules.HEALTH).add(Attributes.ARMOR,FrostWyrmlingRules.ARMOUR).add(Attributes.ARMOR_TOUGHNESS,4)
			.add(Attributes.MOVEMENT_SPEED,FrostWyrmlingRules.SPEED).add(Attributes.ATTACK_DAMAGE,12).add(Attributes.FOLLOW_RANGE,40).add(Attributes.KNOCKBACK_RESISTANCE,1)
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
		if(!FrostWyrmlingRules.scripted(move))bar.setName(Component.translatable("boss.wildercord.frost_wyrmling.status",getType().getDescription(),
			Component.translatable("boss.wildercord.frost_wyrmling.move."+FrostWyrmlingRules.key(move))));
	}
	/** Curled up again on its mound, whole. */
	private void curlUp(){
		pose(FrostWyrmlingRules.DORMANT,0);hold();setHealth(getMaxHealth());setTarget(null);bar.removeAllPlayers();quiet=0;
	}
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
			case FrostWyrmlingRules.BITE->bite(level);
			case FrostWyrmlingRules.TAIL->tail(level);
			case FrostWyrmlingRules.WING->wing(level);
			case FrostWyrmlingRules.BREATH->breath(level,target);
			case FrostWyrmlingRules.POUNCE->pounce(level,target);
			case FrostWyrmlingRules.BOUND->{face(aim);move(MoverType.SELF,new Vec3(slide.x,-.3,slide.z));setDeltaMovement(Vec3.ZERO);if(--left<=0)pose(FrostWyrmlingRules.WATCH,2);}
			case FrostWyrmlingRules.RECOVER->{still();if(since()%12==6){var h=head();level.sendParticles(ParticleTypes.CLOUD,h.x,h.y-.3,h.z,3,.2,.05,.2,.01);}if(--left<=0)pose(FrostWyrmlingRules.WATCH,gap());}
			default->watch(level,target);
		}
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
			int pick=FrostWyrmlingRules.choose(d,yawTo(target.position())-getYRot(),random.nextDouble(),now-lastTail>=FrostWyrmlingRules.TAIL_REST,
				now-lastWing>=FrostWyrmlingRules.WING_REST,now-lastBreath>=FrostWyrmlingRules.BREATH_REST,now-lastPounce>=FrostWyrmlingRules.POUNCE_REST);
			// A long chase ends in a pounce from wherever it got to.
			if(pick<0 && chased>=60 && d<=FrostWyrmlingRules.POUNCE_REACH+3 && now-lastPounce>=FrostWyrmlingRules.POUNCE_REST/2)pick=FrostWyrmlingRules.POUNCE;
			if(pick>=0){
				chased=0;
				aim=pick==FrostWyrmlingRules.TAIL?getYRot():yawTo(target.position());
				switch(pick){
					case FrostWyrmlingRules.TAIL->{
						lastTail=now;
						// It sweeps toward whichever side its prey is on.
						var f=ahead(aim);double s=(target.getX()-getX())*-f.z+(target.getZ()-getZ())*f.x;
						entityData.set(SIDE,s>=0?1:-1);pose(FrostWyrmlingRules.TAIL,0,FrostWyrmlingRules.TAIL_TELL);
						Feels.sound(level,position(),"aura_frost_wyrmling_snarl",1,1.1F);
					}
					case FrostWyrmlingRules.WING->{lastWing=now;pose(FrostWyrmlingRules.WING,0,FrostWyrmlingRules.WING_TELL);Feels.sound(level,head(),"aura_frost_wyrmling_snarl",1.3F,.85F);}
					case FrostWyrmlingRules.BREATH->{lastBreath=now;pose(FrostWyrmlingRules.BREATH,0,FrostWyrmlingRules.BREATH_TELL);Feels.sound(level,head(),"aura_frost_wyrmling_breath",1.4F,.8F);}
					case FrostWyrmlingRules.POUNCE->{lastPounce=now;pose(FrostWyrmlingRules.POUNCE,0,FrostWyrmlingRules.POUNCE_TELL);Feels.sound(level,head(),"aura_frost_wyrmling_snarl",1.4F,1);}
					default->{pose(FrostWyrmlingRules.BITE,0,FrostWyrmlingRules.BITE_TELL);Feels.sound(level,head(),"aura_frost_wyrmling_snarl",1.1F,1.2F);}
				}
				return;
			}
		}
		if(d>FrostWyrmlingRules.CLOSE){
			chased++;
			if(!getNavigation().moveTo(target,1))getMoveControl().setWantedPosition(target.getX(),target.getY(),target.getZ(),1);
		}else{chased=0;getNavigation().stop();turn(yawTo(target.position()),10);}
	}

	/** Neck drawn back through the tell, then the jaws snapped shut on whoever is in front of it. */
	private void bite(ServerLevel level){
		face(aim);still();
		long el=since();int tell=tell();
		if(el==tell){
			var h=head().add(ahead(aim).scale(1.4));
			Feels.sound(level,h,"aura_frost_wyrmling_bite",1.4F,.9F+random.nextFloat()*.2F);
			level.sendParticles(ParticleTypes.SNOWFLAKE,h.x,h.y-.6,h.z,10,.4,.3,.4,.05);
			for(var p:prey(level))if(reaches(FrostWyrmlingRules.BITE,p))hit(level,p,FrostWyrmlingRules.BITE_DAMAGE,.9,position(),.4);
		}
		if(el>=tell+FrostWyrmlingRules.BITE_HOLD)spent(FrostWyrmlingRules.BITE_RECOVER);
	}
	/** Weight shifted forward through the tell, then its tail swept round behind and beside it. */
	private void tail(ServerLevel level){
		face(aim);still();
		long el=since();int tell=tell();
		if(el==tell){
			var back=position().subtract(ahead(aim).scale(2.6));
			Feels.sound(level,back,"aura_frost_wyrmling_tail",1.4F,.9F+random.nextFloat()*.2F);
			for(int i=0;i<14;i++){double a=Math.toRadians(aim)+Math.PI/2+i*Math.PI/13,r=4;level.sendParticles(ParticleTypes.SNOWFLAKE,getX()-Math.sin(a)*r,getY()+.3,getZ()+Math.cos(a)*r,1,.15,.1,.15,.02);}
			for(var p:prey(level))if(reaches(FrostWyrmlingRules.TAIL,p))hit(level,p,FrostWyrmlingRules.TAIL_DAMAGE,1.4,position(),.3);
		}
		if(el>=tell+2)spent(FrostWyrmlingRules.TAIL_RECOVER);
	}
	/** Wings raised high through the tell, then brought down in one great beat that throws back everyone in front of it. */
	private void wing(ServerLevel level){
		face(aim);still();
		long el=since();int tell=tell();
		if(el==tell){
			var f=ahead(aim);
			Feels.sound(level,position().add(0,2,0),"aura_frost_wyrmling_wing",1.6F,.9F+random.nextFloat()*.15F);
			for(int i=0;i<30;i++){
				double sp=(random.nextDouble()-.5)*1.4;
				level.sendParticles(i%3==0?ParticleTypes.POOF:ParticleTypes.SNOWFLAKE,getX()+f.x*1.5,getY()+.6+random.nextDouble()*1.5,getZ()+f.z*1.5,0,f.x-f.z*sp,.05,f.z+f.x*sp,.6);
			}
			for(var p:prey(level))if(reaches(FrostWyrmlingRules.WING,p))hit(level,p,FrostWyrmlingRules.WING_DAMAGE,FrostWyrmlingRules.WING_SHOVE,position(),.5);
		}
		if(el>=tell+2)spent(FrostWyrmlingRules.WING_RECOVER);
	}
	/**
	 * Chest swelled through the tell while it tracks its prey, then frost breathed out in a cone along the ground, swung slowly after
	 * them; whoever stands in it is bitten by the cold, slowed, and frozen through.
	 */
	private void breath(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(target!=null)aim=turned(aim,yawTo(target.position()),el<tell?12:FrostWyrmlingRules.BREATH_TURN);
		face(aim);still();
		if(el<tell){if(el%4==0){var h=head();level.sendParticles(ParticleTypes.SNOWFLAKE,h.x,h.y,h.z,2,.2,.1,.2,.01);}return;}
		if(el>=tell+FrostWyrmlingRules.BREATH_TICKS){spent(FrostWyrmlingRules.BREATH_RECOVER);return;}
		var h=head();var f=ahead(aim);
		if(el==tell)Feels.sound(level,h,"aura_frost_wyrmling_frost",1.6F,1);
		for(int i=0;i<6;i++){
			double sp=(random.nextDouble()-.5)*.9,up=-.12+random.nextDouble()*.1;
			level.sendParticles(ParticleTypes.SNOWFLAKE,h.x,h.y-.3,h.z,0,f.x-f.z*sp,up,f.z+f.x*sp,.7+random.nextDouble()*.3);
		}
		if(el%3==0)level.sendParticles(ParticleTypes.CLOUD,h.x+f.x*3,h.y-1.2,h.z+f.z*3,3,.6,.2,.6,.02);
		if((el-tell)%FrostWyrmlingRules.BREATH_EVERY!=0)return;
		for(var p:prey(level)){
			if(!reaches(FrostWyrmlingRules.BREATH,p))continue;
			GuardianBite.land(level,this,p,(float)FrostWyrmlingRules.BREATH_DAMAGE,FrostWyrmlingRules.PIERCE);
			p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,FrostWyrmlingRules.BREATH_SLOW,1,false,true));
			if(p.canFreeze())p.setTicksFrozen(Math.min(p.getTicksRequiredToFreeze()+40,p.getTicksFrozen()+FrostWyrmlingRules.BREATH_FROST));
			level.sendParticles(ParticleTypes.SNOWFLAKE,p.getX(),p.getY()+1,p.getZ(),6,.3,.5,.3,.01);
		}
	}
	/**
	 * Crouched through the tell while it tracks its prey, the aim locked for the last three ticks; then a leap at where they stood,
	 * no further than the hall lets it, crushing whoever is there when it comes down; and it stands blowing a long while after.
	 */
	private void pounce(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell-3 && target!=null)aim=yawTo(target.position());
		face(aim);
		if(el<tell){still();if(el%3==0)level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+.1,getZ(),6,.8,0,.8,.04);return;}
		if(el==tell){
			from=position();
			double d=target!=null?Math.sqrt(target.distanceToSqr(this)):FrostWyrmlingRules.POUNCE_REACH*.6;
			to=from.add(ahead(aim).scale(FrostWyrmlingRules.leap(d)));
			for(int i=0;i<8 && !inHall(to,-1.5);i++)to=from.add(to.subtract(from).scale(.75));
			Feels.sound(level,position(),"aura_frost_wyrmling_wing",1.4F,1.2F);
			level.sendParticles(ParticleTypes.POOF,getX(),getY()+.2,getZ(),14,1,.1,1,.05);
		}
		long air=el-tell;
		if(air<FrostWyrmlingRules.POUNCE_TICKS){
			var step=to.subtract(from).scale(1.0/FrostWyrmlingRules.POUNCE_TICKS);
			move(MoverType.SELF,new Vec3(step.x,-.3,step.z));setDeltaMovement(Vec3.ZERO);
			level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+1+FrostWyrmlingRules.arc((air+.5)/FrostWyrmlingRules.POUNCE_TICKS),getZ(),2,.8,.3,.8,.01);
			return;
		}
		if(air==FrostWyrmlingRules.POUNCE_TICKS){
			var at=position();
			Feels.sound(level,at,"aura_frost_wyrmling_land",1.8F,.9F+random.nextFloat()*.15F);
			for(int i=0;i<24;i++){double a=i*Math.PI*2/24,r=FrostWyrmlingRules.POUNCE_RADIUS*.8;level.sendParticles(i%2==0?ParticleTypes.POOF:ParticleTypes.SNOWFLAKE,at.x+Math.cos(a)*r,at.y+.1,at.z+Math.sin(a)*r,1,.1,0,.1,.04);}
			level.sendParticles(ParticleTypes.ITEM_SNOWBALL,at.x,at.y+.2,at.z,30,1.2,.2,1.2,.1);
			for(var p:prey(level))if(FrostWyrmlingRules.crushed(p.getX()-at.x,p.getZ()-at.z,p.getY()-at.y))hit(level,p,FrostWyrmlingRules.POUNCE_DAMAGE,1.2,at,.4);
			spent(FrostWyrmlingRules.POUNCE_RECOVER);
		}
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
	private int gap(){return (wounded()?FrostWyrmlingRules.GAP_WOUNDED:FrostWyrmlingRules.GAP)+random.nextInt(3);}

	/** Players it could be fighting right now. */
	private List<ServerPlayer> prey(ServerLevel level){
		var out=new ArrayList<ServerPlayer>();
		for(var p:level.players())if(p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(this)<16*16 && inHall(p.position(),FrostWyrmlingRules.LEASH))out.add(p);
		return out;
	}
	/** Whether {@code move}, along the locked aim, reaches {@code p}. */
	private boolean reaches(int move,LivingEntity p){
		var f=ahead(aim);double dx=p.getX()-getX(),dz=p.getZ()-getZ();
		return FrostWyrmlingRules.hits(move,dx*f.x+dz*f.z,dx*-f.z+dz*f.x,p.getY()-getY());
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
			Feels.sound(server,head(),"aura_frost_wyrmling_death",1.8F,1);bar.removeAllPlayers();
			server.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY()+1.2,getZ(),60,1.6,.8,1.6,.04);
		}
		super.die(source);
	}
	@Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return null;}
	@Override protected void actuallyHurt(ServerLevel level,DamageSource source,float damage){
		super.actuallyHurt(level,source,damage);
		if(random.nextInt(3)==0)Feels.sound(level,head(),"aura_frost_wyrmling_hurt",1.2F,.9F+random.nextFloat()*.2F);
	}
	@Override public void remove(RemovalReason reason){bar.removeAllPlayers();super.remove(reason);}
	@Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);bar.removePlayer(player);}

	@Override protected void addAdditionalSaveData(ValueOutput out){
		super.addAdditionalSaveData(out);
		out.putBoolean("wyrm_bound",bound);out.store("wyrm_mound",BlockPos.CODEC,mound);out.putFloat("wyrm_rest",rest);
		out.store("wyrm_hall_min",BlockPos.CODEC,hallMin);out.store("wyrm_hall_max",BlockPos.CODEC,hallMax);
		out.putBoolean("wyrm_awake",!FrostWyrmlingRules.scripted(move()));out.putLong("wyrm_ticked",lastTick);
	}
	@Override protected void readAdditionalSaveData(ValueInput in){
		super.readAdditionalSaveData(in);
		bound=in.getBooleanOr("wyrm_bound",false);
		mound=in.read("wyrm_mound",BlockPos.CODEC).orElse(blockPosition());rest=in.getFloatOr("wyrm_rest",getYRot());
		hallMin=in.read("wyrm_hall_min",BlockPos.CODEC).orElse(mound);hallMax=in.read("wyrm_hall_max",BlockPos.CODEC).orElse(mound);
		lastTick=in.getLongOr("wyrm_ticked",-1L);
		// Mid-fight it comes back on its feet, whatever it was doing when the chunk unloaded.
		if(in.getBooleanOr("wyrm_awake",false)){pose(FrostWyrmlingRules.WATCH,FrostWyrmlingRules.GAP);lastCut=0;}else pose(FrostWyrmlingRules.DORMANT,0);
	}
}
