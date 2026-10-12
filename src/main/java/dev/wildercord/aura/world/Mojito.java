package dev.wildercord.aura.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import java.util.*;

/**
 * Mojito ({@link MojitoRules}): a black standard poodle as tall as three men, white-bibbed and gold-collared, a greatsword held
 * crosswise in his jaws and mint-green magic welling up on both his flanks. Generation leaves him curled asleep before the grave in
 * the misty grove, the sword laid before his paws, bound to that spot and to the grove. While he sleeps he cannot be hurt; the first
 * player far enough in (or the first blow) gets him up: he stretches, takes up the sword in his mouth, shakes out his coat, and the
 * boss bar comes up. The grove going quiet sends him back to sleep, healed. Anything hostile in the grove, he zaps out of the world.
 * <p>
 * On his feet he sweeps the sword across whoever is in front of him, swinging his whole head, one side then the other; spins round
 * on anyone at his back; leaps onto anyone backing off and brings the sword down as he lands; and looses volleys of magic from both
 * flanks at anyone keeping away. He hops back off anyone crowding him. Wounded, he gets the zoomies: a play-bow, then racing round
 * and round the grove with the sword out, leaving a trail of stinging magic, until he flops down flat, tongue lolling.
 */
public final class Mojito extends PathfinderMob implements Enemy {
	private static final EntityDataAccessor<Integer> MOVE=SynchedEntityData.defineId(Mojito.class,EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN=SynchedEntityData.defineId(Mojito.class,EntityDataSerializers.LONG);
	/** The current move's tell in ticks, so the client knows when the blow lands. */
	private static final EntityDataAccessor<Integer> TELL=SynchedEntityData.defineId(Mojito.class,EntityDataSerializers.INT);
	/** Which way the slash sweeps, or a hop carries him, for the client: -1 left, 1 right, 0 straight back. */
	private static final EntityDataAccessor<Integer> SIDE=SynchedEntityData.defineId(Mojito.class,EntityDataSerializers.INT);
	private static final EntityDimensions CURLED=EntityDimensions.scalable(3.6F,2.6F).withEyeHeight(2.0F),STANDING=EntityDimensions.scalable(3.6F,5.0F).withEyeHeight(4.4F);
	/** His magic: mint green. */
	private static final DustParticleOptions MINT=new DustParticleOptions(0x7CFFB2,1.5F),MINT_SMALL=new DustParticleOptions(0x7CFFB2,.8F);
	private final ServerBossEvent bar=new ServerBossEvent(getUUID(),getType().getDescription(),BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.NOTCHED_10);
	private boolean bound;
	private BlockPos bed=BlockPos.ZERO,groveMin=BlockPos.ZERO,groveMax=BlockPos.ZERO;
	private float rest;
	private int left,quiet;
	private long lastCut;
	/** The game time he last ran, so he knows how long the grove was left alone. */
	private long lastTick=-1;
	/** The line his blow was locked on, and where a hop carries him. */
	private float aim;
	private Vec3 slide=Vec3.ZERO;
	private int chased;
	/** Which way the next slash sweeps. */
	private int swing=1;
	private long lastHop=-999,lastSpin=-999,lastLeap=-999,lastBolts=-999,lastZoom=-999;
	/** Whoever this blow has already struck. */
	private final Set<UUID> struck=new HashSet<>();
	/** The leap: how far each tick carries him. */
	private Vec3 leapStep=Vec3.ZERO;
	/** The zoomies: where he runs round, at what radius, which way, how far round he is, and whom he struck when. */
	private Vec3 zoomAt=Vec3.ZERO;
	private double zoomRadius,zoomAngle;
	private int zoomWay=1;
	private final Map<UUID,Long> zoomStruck=new HashMap<>();
	/** A bolt of magic in flight. */
	private static final class Bolt{double x,y,z,dx,dy,dz;int age;Bolt(Vec3 at,Vec3 v){x=at.x;y=at.y;z=at.z;dx=v.x;dy=v.y;dz=v.z;}}
	private final List<Bolt> bolts=new ArrayList<>();
	/** A spot of the trail the zoomies leave, stinging until {@code until}. */
	private record Spot(double x,double y,double z,long until){}
	private final List<Spot> trail=new ArrayList<>();

	public Mojito(EntityType<? extends Mojito> type,Level level){super(type,level);xpReward=80;setPersistenceRequired();}
	public static AttributeSupplier.Builder createAttributes(){
		return createMobAttributes().add(Attributes.MAX_HEALTH,MojitoRules.HEALTH).add(Attributes.ARMOR,MojitoRules.ARMOUR).add(Attributes.ARMOR_TOUGHNESS,MojitoRules.TIER.toughness)
			.add(Attributes.MOVEMENT_SPEED,MojitoRules.SPEED).add(Attributes.ATTACK_DAMAGE,MojitoRules.TIER.melee).add(Attributes.FOLLOW_RANGE,40).add(Attributes.KNOCKBACK_RESISTANCE,1)
			.add(Attributes.STEP_HEIGHT,1.6);
	}

	/** Bind him to his bed: {@code at} the air above the grass where he sleeps, {@code yaw} the way he faces, {@code grove} the grove he keeps. */
	public void bind(BlockPos at,float yaw,BoundingBox grove){
		bound=true;bed=at.immutable();rest=yaw;
		groveMin=new BlockPos(grove.minX(),grove.minY(),grove.minZ());groveMax=new BlockPos(grove.maxX(),grove.maxY(),grove.maxZ());
		bedDown();
	}
	/** Client side, for blending one move into the next: the move last drawn, the one before it, when it changed, and how far into it the last one was. */
	public int shownMove=-1,shownTell,prevMove,prevTell;public float shownElapsed,prevElapsed,switchedAt=-100;
	public int move(){return entityData.get(MOVE);}
	public int tell(){return entityData.get(TELL);}
	public int side(){return entityData.get(SIDE);}
	public float elapsed(float partial){return (float)(level().getGameTime()-entityData.get(BEGAN))+partial;}
	private long since(){return level().getGameTime()-entityData.get(BEGAN);}
	private Vec3 spot(){return Vec3.atBottomCenterOf(bed);}

	@Override protected void defineSynchedData(SynchedEntityData.Builder b){
		super.defineSynchedData(b);b.define(MOVE,MojitoRules.DORMANT);b.define(BEGAN,0L);b.define(TELL,0);b.define(SIDE,0);
	}
	@Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(MOVE.equals(key))refreshDimensions();}
	@Override protected EntityDimensions getDefaultDimensions(Pose pose){return move()==MojitoRules.DORMANT?CURLED:STANDING;}
	@Override protected void registerGoals(){}
	@Override public boolean isPushable(){return false;}
	@Override protected void doPush(Entity entity){}
	@Override public boolean removeWhenFarAway(double distance){return false;}

	private static WolfSoundVariant.WolfSoundSet voice(){return SoundEvents.WOLF_SOUNDS.get(WolfSoundVariants.SoundSet.BIG).adultSounds();}
	private void sound(ServerLevel level,Vec3 at,SoundEvent s,float volume,float pitch){level.playSound(null,at.x,at.y,at.z,s,SoundSource.HOSTILE,volume,pitch);}
	/** His voice, deep for his size. */
	private void growl(ServerLevel level,float volume){sound(level,head(),voice().growlSound().value(),volume,.55F+random.nextFloat()*.1F);}
	private void bark(ServerLevel level,float volume,float pitch){sound(level,head(),voice().ambientSound().value(),volume,pitch);}

	private void pose(int move,int ticks){pose(move,ticks,0);}
	private void pose(int move,int ticks,int tell){
		int was=move();entityData.set(MOVE,move);entityData.set(BEGAN,level().getGameTime());entityData.set(TELL,tell);left=ticks;getNavigation().stop();
		if(was!=move)refreshDimensions();
		if(!MojitoRules.scripted(move))bar.setName(Component.translatable("boss.wildercord.mojito.status",getType().getDescription(),
			Component.translatable("boss.wildercord.mojito.move."+MojitoRules.key(move))));
	}
	/** Back asleep before the grave, whole again. */
	private void bedDown(){
		pose(MojitoRules.DORMANT,0);hold();setHealth(getMaxHealth());setTarget(null);bar.removeAllPlayers();quiet=0;clearFire();bolts.clear();trail.clear();
	}
	/** Pinned to his bed while he sleeps (or gets up or lies down). */
	private void hold(){
		var t=spot();setPos(t.x,t.y,t.z);setDeltaMovement(Vec3.ZERO);face(rest);
	}
	private boolean inGrove(Vec3 at,double margin){
		return MojitoRules.within(at.x,at.y,at.z,groveMin.getX(),groveMin.getY(),groveMin.getZ(),groveMax.getX(),groveMax.getY(),groveMax.getZ(),margin);
	}
	/** His head, more or less: up high on his feet, low on his paws asleep. */
	private Vec3 head(){
		boolean down=move()==MojitoRules.DORMANT;var f=ahead(getYRot());
		return position().add(f.scale(down?1.8:2.4)).add(0,down?1.4:4.3,0);
	}
	/** Where the magic wells up on his flank, {@code side} 1 his right, -1 his left. */
	private Vec3 flank(int side){
		var f=ahead(getYRot());return position().add(-f.z*side*2.4,3.2,f.x*side*2.4);
	}

	@Override protected void customServerAiStep(ServerLevel level){
		super.customServerAiStep(level);if(!isAlive())return;
		if(!bound){var b=blockPosition();bind(b,getYRot(),new BoundingBox(b.getX()-20,b.getY()-1,b.getZ()-20,b.getX()+20,b.getY()+16,b.getZ()+20));}
		// Left alone long enough (his prey died and went home, say) and he has curled up again in the meantime.
		long now=level.getGameTime();
		if(lastTick>=0 && move()!=MojitoRules.DORMANT && MojitoRules.forgets(now-lastTick))bedDown();
		lastTick=now;
		if(tickCount%MojitoRules.GUARD_EVERY==0 && Guardians.enabled())guard(level);
		switch(move()){
			case MojitoRules.DORMANT->dormant(level);
			case MojitoRules.RISE->{hold();rising(level,MojitoRules.RISE_TICKS-left);if(--left<=0)stand(level);}
			case MojitoRules.BED->{hold();if(wakeful(level)!=null)wake(level,MojitoRules.RISE_TICKS-left);else if(--left<=0)bedDown();}
			default->awake(level);
		}
		fly(level);linger(level);
		if(tickCount%4==0)glow(level);
	}

	/** The magic about him: a wisp now and then off the sword as he sleeps, wisps on both flanks on his feet. */
	private void glow(ServerLevel level){
		if(MojitoRules.scripted(move())){
			if(random.nextInt(3)==0){var h=head();level.sendParticles(MINT_SMALL,h.x,h.y-.8,h.z,1,1.2,.2,1.2,0);}
			return;
		}
		for(int s=-1;s<=1;s+=2){var at=flank(s);level.sendParticles(MINT_SMALL,at.x,at.y,at.z,1,.3,.3,.3,0);}
	}

	/**
	 * The grove is his own: anything hostile that wanders or spawns in, asleep or awake, he zaps out of the world with a bolt off
	 * his flank, one every {@link MojitoRules#GUARD_EVERY} ticks, nearest first. Only players get a fight.
	 */
	private void guard(ServerLevel level){
		var box=new AABB(Vec3.atLowerCornerOf(groveMin),Vec3.atLowerCornerOf(groveMax).add(1,1,1));
		var h=head();
		var m=level.getEntitiesOfClass(Mob.class,box,e->e instanceof Enemy && e!=this && e.isAlive() && !(e instanceof Mojito) && inGrove(e.position(),0))
			.stream().min(Comparator.comparingDouble(e->e.distanceToSqr(h))).orElse(null);
		if(m==null)return;
		var from=flank(random.nextBoolean()?1:-1);
		var to=m.position().add(0,m.getBbHeight()/2,0);
		var step=to.subtract(from);int n=Math.max(2,(int)(step.length()*2));
		for(int i=0;i<=n;i++){var at=from.add(step.scale(i/(double)n));level.sendParticles(MINT,at.x,at.y,at.z,1,.05,.05,.05,0);}
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER,to.x,to.y,to.z,8,.3,.4,.3,0);
		sound(level,to,SoundEvents.AMETHYST_BLOCK_CHIME,1.2F,.8F+random.nextFloat()*.2F);
		m.hurtServer(level,damageSources().mobAttack(this),Float.MAX_VALUE);
		if(m.isAlive())m.kill(level);
	}

	/** The nearest player in the grove close enough to his bed to wake him, if any. */
	private ServerPlayer wakeful(ServerLevel level){
		var t=spot();
		return level.players().stream().filter(p->p.isAlive() && !p.isCreative() && !p.isSpectator() && inGrove(p.position(),0)
			&& MojitoRules.wakes(p.getX()-t.x,p.getY()-t.y,p.getZ()-t.z)).min(Comparator.comparingDouble(p->p.distanceToSqr(t))).orElse(null);
	}

	private void dormant(ServerLevel level){
		hold();if(getHealth()<getMaxHealth())setHealth(getMaxHealth());
		if(tickCount%5==0 && Guardians.enabled() && wakeful(level)!=null)wake(level,0);
		// Snoring softly, now and then a sleepy whine.
		else if(random.nextInt(140)==0)sound(level,head(),random.nextInt(4)==0?voice().whineSound().value():voice().pantSound().value(),.5F,.45F+random.nextFloat()*.1F);
	}

	/** Begin getting up, {@code already} ticks in (when he is woken half way back down). */
	private void wake(ServerLevel level,int already){
		pose(MojitoRules.RISE,MojitoRules.RISE_TICKS-already);entityData.set(BEGAN,level.getGameTime()-already);
		growl(level,1.4F);
	}
	/** Getting up: on his feet, the sword taken up in his jaws, his coat shaken out. */
	private void rising(ServerLevel level,int el){
		if(el==MojitoRules.GRAB_AT){
			var h=head();sound(level,h,SoundEvents.ARMOR_EQUIP_NETHERITE.value(),1.6F,.7F);
			level.sendParticles(MINT,h.x,h.y,h.z,20,1.6,.3,1.6,0);
		}else if(el==MojitoRules.SHAKE_AT){
			sound(level,position(),SoundEvents.WOLF_SHAKE,2F,.55F);
			level.sendParticles(ParticleTypes.POOF,getX(),getY()+2.5,getZ(),30,1.6,1.2,1.6,.05);
			level.sendParticles(MINT,getX(),getY()+3,getZ(),30,2,1.2,2,0);
		}
	}
	/** On his feet: a bark, and the fight is on. */
	private void stand(ServerLevel level){
		pose(MojitoRules.WATCH,MojitoRules.GAP);hold();
		lastCut=level.getGameTime();quiet=0;
		bark(level,2F,.6F);
	}

	private void awake(ServerLevel level){
		if(!inGrove(position(),MojitoRules.LEASH)){
			var at=spot();teleportTo(at.x,at.y,at.z);setDeltaMovement(Vec3.ZERO);getNavigation().stop();pose(MojitoRules.WATCH,MojitoRules.GAP);
		}
		long now=level.getGameTime();float heal=MojitoRules.regen(getMaxHealth(),now-lastCut);if(heal>0 && getHealth()<getMaxHealth())heal(heal);
		if(tickCount%10==0){
			bar.setProgress(getHealth()/getMaxHealth());
			for(var p:new ArrayList<>(bar.getPlayers()))if(p.level()!=level || !p.isAlive() || !inGrove(p.position(),MojitoRules.BAR_DROP))bar.removePlayer(p);
			for(var p:level.players())if(p.isAlive() && !p.isSpectator() && inGrove(p.position(),MojitoRules.BAR_SHOW))bar.addPlayer(p);
			setTarget(level.players().stream().filter(p->p.isAlive() && !p.isCreative() && !p.isSpectator() && inGrove(p.position(),0))
				.min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null));
		}
		var target=getTarget();
		switch(move()){
			case MojitoRules.SLASH->slash(level);
			case MojitoRules.SPIN->spin(level);
			case MojitoRules.LEAP->leap(level,target);
			case MojitoRules.BOLTS->bolts(level,target);
			case MojitoRules.ZOOMIES->zoomies(level,target);
			case MojitoRules.HOP->{face(aim);move(MoverType.SELF,new Vec3(slide.x,-.3,slide.z));setDeltaMovement(Vec3.ZERO);if(--left<=0)pose(MojitoRules.WATCH,2);}
			case MojitoRules.RECOVER->{still();pant(level);if(--left<=0)pose(MojitoRules.WATCH,gap());}
			case MojitoRules.FLOP->{still();pant(level);if(--left<=0){pose(MojitoRules.WATCH,gap());growl(level,1.2F);}}
			default->watch(level,target);
		}
	}
	/** On his feet and tracking: closing on his prey, and turning only so fast, so his back can be got at. */
	private void watch(ServerLevel level,LivingEntity target){
		if(getDeltaMovement().horizontalDistanceSqr()>.0004 && tickCount%8==0){
			sound(level,position(),SoundEvents.WOLF_STEP.value(),1.4F,.5F+random.nextFloat()*.1F);
			// The gold chain at his throat, jingling.
			if(random.nextInt(3)==0)sound(level,head().add(0,-1,0),SoundEvents.CHAIN_PLACE,.6F,1.4F+random.nextFloat()*.3F);
		}
		if(target==null){
			quiet++;
			var at=spot();
			if(quiet>=MojitoRules.RETURN_TICKS && distanceToSqr(at)>1.5)getNavigation().moveTo(at.x,at.y,at.z,1);
			if(quiet>=MojitoRules.RESET_TICKS && (distanceToSqr(at)<=2.5 || quiet>=MojitoRules.RESET_TICKS+100)){
				bar.removeAllPlayers();pose(MojitoRules.BED,MojitoRules.RISE_TICKS);hold();sound(level,head(),voice().whineSound().value(),1,.5F);
			}
			return;
		}
		quiet=0;
		getLookControl().setLookAt(target,30,30);
		double d=distanceTo(target);
		boolean behind=MojitoRules.behind(yawTo(target.position())-getYRot());
		if(left>0)left--;
		if(left<=0){
			long now=level.getGameTime();
			int pick=MojitoRules.choose(d,behind,random.nextDouble(),now-lastSpin>=MojitoRules.SPIN_REST,now-lastLeap>=MojitoRules.LEAP_REST,
				now-lastBolts>=MojitoRules.BOLT_REST,now-lastHop>=MojitoRules.HOP_REST);
			if(MojitoRules.zooms(wounded(),now-lastZoom,d))pick=MojitoRules.ZOOMIES;
			// A long chase ends in a leap from further in.
			if(pick<0 && chased>=60 && d>=5 && now-lastLeap>=MojitoRules.LEAP_REST/2)pick=MojitoRules.LEAP;
			if(pick>=0){
				chased=0;struck.clear();
				aim=yawTo(target.position());
				switch(pick){
					case MojitoRules.SLASH->{swing=-swing;entityData.set(SIDE,swing);pose(MojitoRules.SLASH,0,MojitoRules.SLASH_TELL);growl(level,1.2F);}
					case MojitoRules.SPIN->{lastSpin=now;pose(MojitoRules.SPIN,0,MojitoRules.SPIN_TELL);growl(level,1.4F);}
					case MojitoRules.LEAP->{lastLeap=now;pose(MojitoRules.LEAP,0,MojitoRules.LEAP_TELL);bark(level,1.8F,.55F);}
					case MojitoRules.BOLTS->{lastBolts=now;pose(MojitoRules.BOLTS,0,MojitoRules.BOLT_TELL);sound(level,position().add(0,3,0),SoundEvents.AMETHYST_BLOCK_RESONATE,2F,.7F);}
					case MojitoRules.HOP->{lastHop=now;hopFrom(level,target.position());}
					case MojitoRules.ZOOMIES->{lastZoom=now;zoomStruck.clear();pose(MojitoRules.ZOOMIES,0,MojitoRules.ZOOM_TELL);bark(level,2F,.75F);}
					default->{}
				}
				return;
			}
		}
		if(d>MojitoRules.CLOSE){
			chased++;
			if(!getNavigation().moveTo(target,1))getMoveControl().setWantedPosition(target.getX(),target.getY(),target.getZ(),1);
		}else{chased=0;getNavigation().stop();turn(yawTo(target.position()),10);}
	}

	/** Head drawn back to one side through the tell, then the sword swept in a wide arc across the ground in front of him. */
	private void slash(ServerLevel level){
		face(aim);still();
		long el=since();int tell=tell();
		if(el==tell){
			sound(level,head(),SoundEvents.PLAYER_ATTACK_SWEEP,2F,.6F+random.nextFloat()*.1F);
			int s=side();
			for(int i=0;i<=12;i++){
				double a=Math.toRadians(aim+s*(-80+i*160/12.0)),r=MojitoRules.SLASH_REACH*.75;
				double x=getX()-Math.sin(a)*r,z=getZ()+Math.cos(a)*r;
				level.sendParticles(ParticleTypes.SWEEP_ATTACK,x,getY()+1.4,z,1,0,0,0,0);
				level.sendParticles(MINT,x,getY()+1.4,z,2,.2,.2,.2,0);
			}
			for(var p:prey(level))if(reaches(MojitoRules.SLASH,p))hit(level,p,MojitoRules.SLASH_DAMAGE,1.2,position(),.3);
		}
		if(el>=tell+3)spent(MojitoRules.SLASH_RECOVER);
	}
	/** Crouched low through the tell, then a full turn on the spot, the blade sweeping round through everyone near him. */
	private void spin(ServerLevel level){
		still();
		long el=since();int tell=tell();
		if(el<=tell){face(aim);return;}
		long k=el-tell;
		if(k==1)sound(level,position().add(0,2,0),SoundEvents.PLAYER_ATTACK_SWEEP,2.2F,.5F);
		float yaw=aim+360F*k/MojitoRules.SPIN_TICKS;face(yaw);
		var f=ahead(yaw);
		for(int i=2;i<=6;i+=2){double x=getX()-f.z*i,z=getZ()+f.x*i;level.sendParticles(ParticleTypes.SWEEP_ATTACK,x,getY()+1.4,z,1,0,0,0,0);level.sendParticles(MINT,x,getY()+1.4,z,1,.2,.1,.2,0);}
		for(var p:prey(level))if(!struck.contains(p.getUUID()) && reaches(MojitoRules.SPIN,p)){struck.add(p.getUUID());hit(level,p,MojitoRules.SPIN_DAMAGE,1.6,position(),.35);}
		if(k>=MojitoRules.SPIN_TICKS){face(aim);spent(MojitoRules.SPIN_RECOVER);}
	}
	/**
	 * Crouched with his hindquarters up through the tell, tracking his prey, the aim locked for the last three ticks; then a great
	 * leap onto where they stood, and the sword brought down as he lands.
	 */
	private void leap(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell-3 && target!=null)aim=yawTo(target.position());
		face(aim);
		if(el<tell){still();if(el%4==0)level.sendParticles(ParticleTypes.POOF,getX(),getY()+.2,getZ(),6,1,.1,1,.03);return;}
		int n=MojitoRules.LEAP_TICKS;
		if(el==tell){
			double d=target==null?8:Math.sqrt(target.distanceToSqr(getX(),target.getY(),getZ()));
			leapStep=ahead(aim).scale(MojitoRules.leapLength(d)/n);
			sound(level,position(),SoundEvents.RAVAGER_STEP,2F,.6F);growl(level,1.6F);
			return;
		}
		long k=el-tell-1;
		if(k<n){
			double vy=1.0*(1-2.0*k/(n-1));
			move(MoverType.SELF,new Vec3(leapStep.x,vy,leapStep.z));setDeltaMovement(Vec3.ZERO);
			level.sendParticles(MINT,getX(),getY()+2.5,getZ(),3,1,1,1,0);
			return;
		}
		move(MoverType.SELF,new Vec3(0,-3,0));setDeltaMovement(Vec3.ZERO);
		var at=position().add(ahead(aim).scale(1.5));
		sound(level,at,SoundEvents.MACE_SMASH_GROUND_HEAVY,2.2F,.7F);
		for(int i=0;i<24;i++){double a=i*Math.PI*2/24,r=MojitoRules.LEAP_RADIUS*.8;level.sendParticles(i%2==0?MINT:ParticleTypes.POOF,at.x+Math.cos(a)*r,at.y+.2,at.z+Math.sin(a)*r,1,.1,0,.1,.02);}
		level.sendParticles(ParticleTypes.END_ROD,at.x,at.y+.3,at.z,16,1.2,.2,1.2,.08);
		for(var p:prey(level))if(MojitoRules.lands(p.getX()-at.x,p.getY()-at.y,p.getZ()-at.z))hit(level,p,MojitoRules.LEAP_DAMAGE,1.4,at,.5);
		spent(MojitoRules.LEAP_RECOVER);
	}
	/** Magic welling up on both flanks through the tell as he turns to face his prey; then volley after volley loosed at them. */
	private void bolts(ServerLevel level,LivingEntity target){
		still();
		if(target!=null)turn(yawTo(target.position()),8);
		long el=since();int tell=tell();
		if(el<tell){
			if(el%2==0)for(int s=-1;s<=1;s+=2){var at=flank(s);level.sendParticles(MINT,at.x,at.y,at.z,1+(int)(el/4),.2+el*.02,.2+el*.02,.2+el*.02,0);}
			return;
		}
		long k=el-tell;
		if(k%MojitoRules.BOLT_EVERY==0 && k/MojitoRules.BOLT_EVERY<MojitoRules.BOLT_VOLLEYS){
			sound(level,position().add(0,3,0),SoundEvents.EVOKER_CAST_SPELL,1.8F,1.1F+random.nextFloat()*.2F);
			sound(level,position().add(0,3,0),SoundEvents.BREEZE_SHOOT,1.4F,.8F);
			for(int s=-1;s<=1;s+=2){
				var from=flank(s);
				var to=target==null?from.add(ahead(getYRot()).scale(10)):target.position().add(0,target.getBbHeight()*.5,0);
				var v=to.subtract(from);
				if(v.lengthSqr()<1e-4)v=ahead(getYRot());
				bolts.add(new Bolt(from,v.normalize().scale(MojitoRules.BOLT_SPEED)));
				level.sendParticles(ParticleTypes.END_ROD,from.x,from.y,from.z,6,.2,.2,.2,.05);
			}
		}
		if(k>=(long)(MojitoRules.BOLT_VOLLEYS-1)*MojitoRules.BOLT_EVERY+4)spent(MojitoRules.BOLT_RECOVER);
	}
	/** The bolts in flight: on until they hit someone, hit something solid, or fade. */
	private void fly(ServerLevel level){
		if(bolts.isEmpty())return;
		var prey=prey(level);
		for(var it=bolts.iterator();it.hasNext();){
			var b=it.next();
			var from=new Vec3(b.x,b.y,b.z);var to=from.add(b.dx,b.dy,b.dz);
			boolean gone=++b.age>MojitoRules.BOLT_LIFE;
			if(!gone && level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()!=HitResult.Type.MISS)gone=true;
			b.x=to.x;b.y=to.y;b.z=to.z;
			if(!gone)for(var p:prey)if(MojitoRules.boltHits(b.x,b.y,b.z,p.getX(),p.getY(),p.getZ(),p.getBbHeight())){
				hit(level,p,MojitoRules.BOLT_DAMAGE,.4,from.subtract(b.dx*3,0,b.dz*3),.1);gone=true;break;
			}
			if(gone){
				level.sendParticles(MINT,b.x,b.y,b.z,10,.3,.3,.3,0);
				level.sendParticles(ParticleTypes.HAPPY_VILLAGER,b.x,b.y,b.z,4,.3,.3,.3,0);
				sound(level,to,SoundEvents.AMETHYST_BLOCK_CHIME,1F,1.2F+random.nextFloat()*.3F);
				it.remove();continue;
			}
			level.sendParticles(MINT,b.x,b.y,b.z,3,.1,.1,.1,0);
			level.sendParticles(ParticleTypes.END_ROD,b.x,b.y,b.z,1,0,0,0,0);
		}
	}

	/**
	 * The zoomies. A play-bow through the tell, tail going; then round and round the grove at full tilt, sword out, running down
	 * whoever is in his path and leaving a trail of magic, the circles widening or tightening after his prey; then a flop.
	 */
	private void zoomies(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<tell){still();face(aim);if(el%6==0)sound(level,head(),voice().pantSound().value(),1F,.7F);return;}
		long now=level.getGameTime();
		double want=target==null?MojitoRules.ZOOM_MIN_RADIUS:MojitoRules.zoomRadius(Math.sqrt(target.distanceToSqr(zoomAt.x,target.getY(),zoomAt.z)));
		if(el==tell){
			zoomAt=spot();zoomRadius=want;zoomWay=random.nextBoolean()?1:-1;
			zoomAngle=Math.atan2(getZ()-zoomAt.z,getX()-zoomAt.x);
			if(target!=null)want=MojitoRules.zoomRadius(Math.sqrt(target.distanceToSqr(zoomAt.x,target.getY(),zoomAt.z)));
		}
		zoomRadius+=(want-zoomRadius)*.05;
		zoomAngle+=zoomWay*MojitoRules.ZOOM_SPEED/zoomRadius;
		var goal=zoomAt.add(Math.cos(zoomAngle)*zoomRadius,0,Math.sin(zoomAngle)*zoomRadius);
		var step=goal.subtract(position()).multiply(1,0,1);
		double most=MojitoRules.ZOOM_SPEED*1.3;
		if(step.length()>most)step=step.normalize().scale(most);
		if(step.lengthSqr()>1e-4){aim=(float)Math.toDegrees(Math.atan2(-step.x,step.z));face(aim);}
		move(MoverType.SELF,new Vec3(step.x,-.3,step.z));setDeltaMovement(Vec3.ZERO);
		if(el%3==0){
			sound(level,position(),SoundEvents.WOLF_STEP.value(),1.6F,.6F+random.nextFloat()*.1F);
			trail.add(new Spot(getX(),getY(),getZ(),now+MojitoRules.TRAIL_TICKS));
		}
		if(random.nextInt(25)==0)bark(level,1.6F,.8F+random.nextFloat()*.1F);
		level.sendParticles(MINT,getX(),getY()+2,getZ(),3,1.2,.8,1.2,0);
		for(var p:prey(level)){
			long last=zoomStruck.getOrDefault(p.getUUID(),-999L);
			if(now-last>=MojitoRules.ZOOM_REHIT && reaches(MojitoRules.ZOOMIES,p)){zoomStruck.put(p.getUUID(),now);hit(level,p,MojitoRules.ZOOM_DAMAGE,1.8,position().subtract(ahead(aim)),.45);}
		}
		if(el>=tell+MojitoRules.ZOOM_TICKS){
			pose(MojitoRules.FLOP,MojitoRules.ZOOM_FLOP);still();
			sound(level,position(),SoundEvents.RAVAGER_STEP,1.6F,.5F);
			level.sendParticles(ParticleTypes.POOF,getX(),getY()+.5,getZ(),20,1.6,.2,1.6,.03);
		}
	}
	/** The trail the zoomies left, glittering and stinging whoever stands in it; gone when it fades. */
	private void linger(ServerLevel level){
		if(trail.isEmpty())return;
		long now=level.getGameTime();
		trail.removeIf(s->now>=s.until());
		if(now%3==0)for(var s:trail){
			level.sendParticles(MINT_SMALL,s.x(),s.y()+.2,s.z(),2,.6,.1,.6,0);
			if(random.nextInt(6)==0)level.sendParticles(ParticleTypes.HAPPY_VILLAGER,s.x(),s.y()+.3,s.z(),1,.5,.1,.5,0);
		}
		if(now%MojitoRules.TRAIL_EVERY!=0)return;
		var hurt=new HashSet<UUID>();double r=MojitoRules.TRAIL_RADIUS*MojitoRules.TRAIL_RADIUS;
		for(var p:prey(level))for(var s:trail){
			double dx=p.getX()-s.x(),dz=p.getZ()-s.z();
			if(dx*dx+dz*dz<=r && Math.abs(p.getY()-s.y())<1.5 && hurt.add(p.getUUID())){
				GuardianBite.land(level,this,p,(float)MojitoRules.TRAIL_DAMAGE,MojitoRules.PIERCE);
				level.sendParticles(MINT,p.getX(),p.getY()+.4,p.getZ(),6,.25,.2,.25,0);
				break;
			}
		}
	}

	/** Panting, tongue out, while he catches his breath. */
	private void pant(ServerLevel level){if(since()%12==0)sound(level,head(),voice().pantSound().value(),1.2F,.55F+random.nextFloat()*.1F);}
	/** Spent after an attack for {@code ticks}. */
	private void spent(int ticks){pose(MojitoRules.RECOVER,ticks);still();}
	/** A quick hop back away from {@code from}. */
	private void hopFrom(ServerLevel level,Vec3 from){
		aim=getYRot();
		var away=new Vec3(getX()-from.x,0,getZ()-from.z);
		if(away.lengthSqr()<1e-4)away=ahead(aim).scale(-1);
		slide=away.normalize().scale(MojitoRules.HOP_SPEED);
		var f=ahead(aim);double side=slide.x*-f.z+slide.z*f.x;
		entityData.set(SIDE,Math.abs(side)<.3?0:side>0?1:-1);pose(MojitoRules.HOP,MojitoRules.HOP_TICKS,MojitoRules.HOP_TICKS);
		sound(level,position(),SoundEvents.WOLF_STEP.value(),1.6F,.6F);
		level.sendParticles(ParticleTypes.POOF,getX(),getY()+.1,getZ(),10,1,.1,1,.04);
	}
	/** Standing his ground: no sliding about, though he still falls. */
	private void still(){getNavigation().stop();var v=getDeltaMovement();setDeltaMovement(0,Math.min(0,v.y),0);}
	private boolean wounded(){return MojitoRules.wounded(getHealth(),getMaxHealth());}
	/** Ticks before his next blow. */
	private int gap(){return (wounded()?MojitoRules.GAP_WOUNDED:MojitoRules.GAP)+random.nextInt(3);}

	/** Players he could be fighting right now. */
	private List<ServerPlayer> prey(ServerLevel level){
		var out=new ArrayList<ServerPlayer>();
		for(var p:level.players())if(p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(this)<24*24 && inGrove(p.position(),MojitoRules.LEASH))out.add(p);
		return out;
	}
	/** Whether {@code move}, along the locked aim, reaches {@code p}. */
	private boolean reaches(int move,LivingEntity p){
		var f=ahead(aim);double dx=p.getX()-getX(),dz=p.getZ()-getZ();
		return MojitoRules.hits(move,dx*f.x+dz*f.z,dx*-f.z+dz*f.x,p.getY()-getY());
	}
	/** A blow landing on {@code p}, through part of its armour, knocking it {@code shove} away from {@code from} and {@code lift} up. */
	private void hit(ServerLevel level,LivingEntity p,double damage,double shove,Vec3 from,double lift){
		if(!GuardianBite.land(level,this,p,(float)damage,MojitoRules.PIERCE))return;
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
		if(MojitoRules.scripted(move) && !source.is(DamageTypes.GENERIC_KILL)){
			// Asleep his thick coat takes the blow; but a blow, or a shot, wakes him.
			if(move==MojitoRules.DORMANT && Guardians.enabled() && source.getEntity() instanceof ServerPlayer p && !p.isCreative())wake(level,0);
			return false;
		}
		long now=level.getGameTime();
		if(!source.is(DamageTypes.GENERIC_KILL)){
			var at=source.getSourcePosition();
			boolean frontal=at!=null && MojitoRules.frontal(yawTo(at)-getYRot());
			// He sees a blow coming and hops back out from under it.
			if(source.getDirectEntity()!=null && source.getDirectEntity()==source.getEntity() && MojitoRules.dodges(move,now-lastHop,random.nextDouble())){
				lastHop=now;hopFrom(level,at==null?position().add(ahead(getYRot())):at);
				return false;
			}
			damage*=(float)MojitoRules.armour(move,frontal);
		}
		boolean hurt=super.hurtServer(level,source,damage);
		if(hurt)lastCut=now;
		return hurt;
	}
	@Override public void die(DamageSource source){
		if(level() instanceof ServerLevel server){sound(server,head(),voice().deathSound().value(),2F,.5F);bar.removeAllPlayers();bolts.clear();trail.clear();}
		super.die(source);
	}
	@Override protected SoundEvent getHurtSound(DamageSource source){return null;}
	@Override protected void actuallyHurt(ServerLevel level,DamageSource source,float damage){
		super.actuallyHurt(level,source,damage);
		if(random.nextInt(3)==0)sound(level,head(),voice().hurtSound().value(),1.4F,.55F+random.nextFloat()*.1F);
	}
	@Override public void remove(RemovalReason reason){bar.removeAllPlayers();super.remove(reason);}
	@Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);bar.removePlayer(player);}

	@Override protected void addAdditionalSaveData(ValueOutput out){
		super.addAdditionalSaveData(out);
		out.putBoolean("mojito_bound",bound);out.store("mojito_bed",BlockPos.CODEC,bed);out.putFloat("mojito_rest",rest);
		out.store("mojito_grove_min",BlockPos.CODEC,groveMin);out.store("mojito_grove_max",BlockPos.CODEC,groveMax);
		out.putBoolean("mojito_awake",!MojitoRules.scripted(move()));out.putLong("mojito_ticked",lastTick);
	}
	@Override protected void readAdditionalSaveData(ValueInput in){
		super.readAdditionalSaveData(in);
		bound=in.getBooleanOr("mojito_bound",false);
		bed=in.read("mojito_bed",BlockPos.CODEC).orElse(blockPosition());rest=in.getFloatOr("mojito_rest",getYRot());
		groveMin=in.read("mojito_grove_min",BlockPos.CODEC).orElse(bed);groveMax=in.read("mojito_grove_max",BlockPos.CODEC).orElse(bed);
		lastTick=in.getLongOr("mojito_ticked",-1L);
		// Mid-fight he comes back on his feet, whatever he was doing when the chunk unloaded.
		if(in.getBooleanOr("mojito_awake",false)){pose(MojitoRules.WATCH,MojitoRules.GAP);lastCut=0;}else pose(MojitoRules.DORMANT,0);
	}
}
