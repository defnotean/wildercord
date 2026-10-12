package dev.wildercord.aura.world;

import dev.wildercord.cast.feel.Feels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import java.util.*;

/**
 * The Drowned Bell-Warden ({@link BellWardenRules}). Generation leaves it kneeling before the sunken chapel's fallen bell-frame,
 * bound to that spot and to the chapel's hall, facing the door. While it kneels it cannot be hurt; the first player far enough up
 * the nave (or the first blow) gets it to its feet, its bell tolling as it straightens, and then the boss bar comes up. The
 * chapel going quiet sends it back to its vigil, healed. The hall is its own: anything hostile in it, it kills.
 * <p>
 * On its feet it swings its clapper like a mace (and, half the time, brings it down overhand after), pounds the floor ahead of it
 * with both fists, and charges anyone keeping their distance, bell lowered like a ram. Each blow leaves it spent a moment, bell tipped
 * back and chest bare. It steps back out from under some blows, and hurt too fast it hunches down under its bell until the moment passes.
 */
public final class DrownedBellWarden extends PathfinderMob implements Enemy {
	private static final EntityDataAccessor<Integer> MOVE=SynchedEntityData.defineId(DrownedBellWarden.class,EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN=SynchedEntityData.defineId(DrownedBellWarden.class,EntityDataSerializers.LONG);
	/** The current move's tell in ticks, so the client knows when the blow lands. */
	private static final EntityDataAccessor<Integer> TELL=SynchedEntityData.defineId(DrownedBellWarden.class,EntityDataSerializers.INT);
	private static final EntityDimensions KNEELING=EntityDimensions.scalable(2.4F,3.2F).withEyeHeight(2.6F),STANDING=EntityDimensions.scalable(2.4F,4.6F).withEyeHeight(4.0F);
	private final ServerBossEvent bar=new ServerBossEvent(getUUID(),getType().getDescription(),BossEvent.BossBarColor.BLUE,BossEvent.BossBarOverlay.NOTCHED_10);
	private boolean bound;
	private BlockPos vigil=BlockPos.ZERO,hallMin=BlockPos.ZERO,hallMax=BlockPos.ZERO;
	private float rest;
	private int left,quiet;
	private long lastCut;
	/** The game time it last ran, so it knows how long the chapel was left alone. */
	private long lastTick=-1;
	/** The line its blow was locked on, and where a backstep carries it. */
	private float aim;
	private Vec3 slide=Vec3.ZERO;
	private int chased;
	private long lastDodge=-999,lastSlam=-999,lastCharge=-999,lastShell=-999,lastRing=-999;
	/** Tolls still to come in this tolling, and when it last chose to toll. */
	private int tollsLeft;private long lastToll=-999;
	/** Rings rolling out from where it struck its bell: where, when, and who each has already caught. */
	private record Wave(Vec3 at,long born,Set<UUID> struck){}
	private final List<Wave> waves=new ArrayList<>();
	/** Hurt taken lately, fading; enough of it sends it under its bell. */
	private double stung;
	/** Whoever this charge has already run down. */
	private final Set<UUID> struck=new HashSet<>();

	public DrownedBellWarden(EntityType<? extends DrownedBellWarden> type,Level level){super(type,level);xpReward=80;setPersistenceRequired();}
	public static AttributeSupplier.Builder createAttributes(){
		return createMobAttributes().add(Attributes.MAX_HEALTH,BellWardenRules.HEALTH).add(Attributes.ARMOR,BellWardenRules.ARMOUR).add(Attributes.ARMOR_TOUGHNESS,6)
			.add(Attributes.MOVEMENT_SPEED,BellWardenRules.SPEED).add(Attributes.ATTACK_DAMAGE,12).add(Attributes.FOLLOW_RANGE,40).add(Attributes.KNOCKBACK_RESISTANCE,1)
			.add(Attributes.STEP_HEIGHT,1.1).add(Attributes.WATER_MOVEMENT_EFFICIENCY,1);
	}

	/** Bind it to its vigil: {@code at} the air above the floor where it kneels, {@code yaw} the way it faces, {@code hall} the chapel it keeps. */
	public void bind(BlockPos at,float yaw,BoundingBox hall){
		bound=true;vigil=at.immutable();rest=yaw;
		hallMin=new BlockPos(hall.minX(),hall.minY(),hall.minZ());hallMax=new BlockPos(hall.maxX(),hall.maxY(),hall.maxZ());
		kneelDown();
	}
	public int move(){return entityData.get(MOVE);}
	public int tell(){return entityData.get(TELL);}
	public float elapsed(float partial){return (float)(level().getGameTime()-entityData.get(BEGAN))+partial;}
	private long since(){return level().getGameTime()-entityData.get(BEGAN);}
	private Vec3 spot(){return Vec3.atBottomCenterOf(vigil);}

	@Override protected void defineSynchedData(SynchedEntityData.Builder b){
		super.defineSynchedData(b);b.define(MOVE,BellWardenRules.DORMANT);b.define(BEGAN,0L);b.define(TELL,0);
	}
	@Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(MOVE.equals(key))refreshDimensions();}
	@Override protected EntityDimensions getDefaultDimensions(Pose pose){return move()==BellWardenRules.DORMANT?KNEELING:STANDING;}
	@Override protected void registerGoals(){}
	@Override public boolean isPushable(){return false;}
	@Override protected void doPush(Entity entity){}
	@Override public boolean canBreatheUnderwater(){return true;}
	@Override public boolean removeWhenFarAway(double distance){return false;}

	private void pose(int move,int ticks){pose(move,ticks,0);}
	private void pose(int move,int ticks,int tell){
		int was=move();entityData.set(MOVE,move);entityData.set(BEGAN,level().getGameTime());entityData.set(TELL,tell);left=ticks;getNavigation().stop();
		if(was!=move)refreshDimensions();
		if(!BellWardenRules.scripted(move))bar.setName(Component.translatable("boss.wildercord.drowned_bell_warden.status",getType().getDescription(),
			Component.translatable("boss.wildercord.drowned_bell_warden.move."+BellWardenRules.key(move))));
	}
	/** Back on its knees at its vigil, whole again. */
	private void kneelDown(){
		pose(BellWardenRules.DORMANT,0);hold();setHealth(getMaxHealth());setTarget(null);bar.removeAllPlayers();quiet=0;
	}
	/** Pinned to its vigil while it kneels there (or gets up or down). */
	private void hold(){
		var t=spot();setPos(t.x,t.y,t.z);setDeltaMovement(Vec3.ZERO);face(rest);
	}
	private boolean inHall(Vec3 at,double margin){
		return BellWardenRules.within(at.x,at.y,at.z,hallMin.getX(),hallMin.getY(),hallMin.getZ(),hallMax.getX(),hallMax.getY(),hallMax.getZ(),margin);
	}

	@Override protected void customServerAiStep(ServerLevel level){
		super.customServerAiStep(level);if(!isAlive())return;
		if(!bound){var b=blockPosition();bind(b,getYRot(),new BoundingBox(b.getX()-11,b.getY()-1,b.getZ()-33,b.getX()+11,b.getY()+13,b.getZ()+13));}
		// Left alone long enough (its prey died and went home, say) and it has knelt again in the meantime.
		long now=level.getGameTime();
		if(lastTick>=0 && move()!=BellWardenRules.DORMANT && BellWardenRules.forgets(now-lastTick))kneelDown();
		lastTick=now;
		if(tickCount%BellWardenRules.GUARD_EVERY==0 && Guardians.enabled())guard(level);
		switch(move()){
			case BellWardenRules.DORMANT->dormant(level);
			case BellWardenRules.RISE->{hold();if(--left<=0)stand(level);else if(left==BellWardenRules.RISE_TICKS-BellWardenRules.TOLL_AT)toll(level,1.6F,1);}
			case BellWardenRules.KNEEL->{hold();if(wakeful(level)!=null)wake(level,BellWardenRules.RISE_TICKS-left);else if(--left<=0)kneelDown();}
			default->awake(level);
		}
		if(!waves.isEmpty())waves(level);
	}

	/** Its bell, ringing: the sound, and a ring of drowned light rolling off it. */
	private void toll(ServerLevel level,float volume,float pitch){
		var b=bell();
		Feels.sound(level,b,"aura_bell_warden_toll",volume,pitch);
		for(int i=0;i<24;i++){double a=i*Math.PI*2/24;level.sendParticles(ParticleTypes.GLOW,b.x+Math.cos(a)*1.6,b.y-.6,b.z+Math.sin(a)*1.6,1,0,0,0,0);}
		level.sendParticles(ParticleTypes.BUBBLE,b.x,b.y-1,b.z,12,.8,.4,.8,.05);
	}
	/** The middle of the bell it wears over its head and shoulders. */
	private Vec3 bell(){return position().add(0,move()==BellWardenRules.DORMANT?2.6:3.8,0);}

	/**
	 * The chapel is its own: anything hostile that wanders or spawns in, kneeling or standing, it rings out of the world, one every
	 * {@link BellWardenRules#GUARD_EVERY} ticks, nearest first. Only players get a fight.
	 */
	private void guard(ServerLevel level){
		var box=new AABB(Vec3.atLowerCornerOf(hallMin),Vec3.atLowerCornerOf(hallMax).add(1,1,1));
		var b=bell();
		var m=level.getEntitiesOfClass(Mob.class,box,e->e instanceof Enemy && e!=this && e.isAlive() && !(e instanceof DrownedBellWarden) && inHall(e.position(),0))
			.stream().min(Comparator.comparingDouble(e->e.distanceToSqr(b))).orElse(null);
		if(m==null)return;
		var to=m.position().add(0,m.getBbHeight()/2,0);
		var step=to.subtract(b);int n=Math.max(2,(int)(step.length()*2));
		for(int i=0;i<=n;i++){var at=b.add(step.scale(i/(double)n));level.sendParticles(ParticleTypes.GLOW,at.x,at.y,at.z,1,.05,.05,.05,0);}
		level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,to.x,to.y,to.z,6,.3,.3,.3,.1);
		Feels.sound(level,b,"aura_bell_warden_toll",.6F,1.5F+random.nextFloat()*.2F);
		m.hurtServer(level,damageSources().mobAttack(this),Float.MAX_VALUE);
		if(m.isAlive())m.kill(level);
	}

	/** The nearest player in the hall close enough to its vigil to wake it, if any. */
	private ServerPlayer wakeful(ServerLevel level){
		var t=spot();
		return level.players().stream().filter(p->p.isAlive() && !p.isCreative() && !p.isSpectator() && inHall(p.position(),0)
			&& BellWardenRules.wakes(p.getX()-t.x,p.getY()-t.y,p.getZ()-t.z)).min(Comparator.comparingDouble(p->p.distanceToSqr(t))).orElse(null);
	}

	private void dormant(ServerLevel level){
		hold();if(getHealth()<getMaxHealth())setHealth(getMaxHealth());
		if(tickCount%5==0 && Guardians.enabled() && wakeful(level)!=null)wake(level,0);
		else if(random.nextInt(140)==0){
			var b=bell();Feels.sound(level,b,"aura_bell_warden_breath",.8F,.9F+random.nextFloat()*.2F);
			level.sendParticles(ParticleTypes.BUBBLE_POP,b.x,b.y-1.2,b.z,8,.4,.2,.4,.02);
		}
	}

	/** Begin getting up, {@code already} ticks in (when it is woken half way back down to its knees). */
	private void wake(ServerLevel level,int already){
		pose(BellWardenRules.RISE,BellWardenRules.RISE_TICKS-already);entityData.set(BEGAN,level.getGameTime()-already);
		Feels.sound(level,bell(),"aura_bell_warden_rise",1.4F,1);
	}

	/** On its feet: it strikes its own bell, and the fight is on. */
	private void stand(ServerLevel level){
		pose(BellWardenRules.WATCH,BellWardenRules.GAP);hold();
		lastCut=level.getGameTime();quiet=0;
		Feels.sound(level,bell(),"aura_bell_warden_challenge",1.8F,1);
		level.sendParticles(ParticleTypes.SPLASH,getX(),getY()+.2,getZ(),30,1.2,.2,1.2,.3);
	}

	private void awake(ServerLevel level){
		if(!inHall(position(),BellWardenRules.LEASH)){
			var at=spot();teleportTo(at.x,at.y,at.z);setDeltaMovement(Vec3.ZERO);getNavigation().stop();pose(BellWardenRules.WATCH,BellWardenRules.GAP);
		}
		long now=level.getGameTime();float heal=BellWardenRules.regen(getMaxHealth(),now-lastCut);if(heal>0 && getHealth()<getMaxHealth())heal(heal);
		if(tickCount%10==0){
			bar.setProgress(getHealth()/getMaxHealth());
			for(var p:new ArrayList<>(bar.getPlayers()))if(p.level()!=level || !p.isAlive() || !inHall(p.position(),BellWardenRules.BAR_DROP))bar.removePlayer(p);
			for(var p:level.players())if(p.isAlive() && !p.isSpectator() && inHall(p.position(),BellWardenRules.BAR_SHOW))bar.addPlayer(p);
			setTarget(level.players().stream().filter(p->p.isAlive() && !p.isCreative() && !p.isSpectator() && inHall(p.position(),0))
				.min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null));
		}
		stung*=BellWardenRules.STING_FADE;
		var target=getTarget();
		switch(move()){
			case BellWardenRules.SWING,BellWardenRules.HAMMER->swing(level,target);
			case BellWardenRules.SLAM->slam(level,target);
			case BellWardenRules.CHARGE->charge(level,target);
			case BellWardenRules.BACKSTEP->{face(aim);move(MoverType.SELF,slide);setDeltaMovement(Vec3.ZERO);if(--left<=0)pose(BellWardenRules.WATCH,2);}
			case BellWardenRules.TOLL->knell(level,target);
			// It comes up out of its shell striking its bell.
			case BellWardenRules.SHELL->{still();if(--left<=0)beginToll(level);}
			case BellWardenRules.RECOVER->{still();if(--left<=0)pose(BellWardenRules.WATCH,gap());}
			default->watch(level,target);
		}
	}

	/** On its feet and tracking: closing on its prey. */
	private void watch(ServerLevel level,LivingEntity target){
		if(getDeltaMovement().horizontalDistanceSqr()>.0004 && tickCount%16==0)Feels.sound(level,position(),"aura_bell_warden_step",1,.9F+random.nextFloat()*.2F);
		if(target==null){
			quiet++;
			var at=spot();
			if(quiet>=BellWardenRules.RETURN_TICKS && distanceToSqr(at)>1.5)getNavigation().moveTo(at.x,at.y,at.z,1);
			if(quiet>=BellWardenRules.RESET_TICKS && (distanceToSqr(at)<=2.5 || quiet>=BellWardenRules.RESET_TICKS+100)){
				bar.removeAllPlayers();pose(BellWardenRules.KNEEL,BellWardenRules.RISE_TICKS);hold();Feels.sound(level,bell(),"aura_bell_warden_rise",1,.8F);
			}
			return;
		}
		quiet=0;
		getLookControl().setLookAt(target,30,30);
		double d=distanceTo(target);
		if(left>0)left--;
		if(left<=0){
			long now=level.getGameTime();
			int pick=BellWardenRules.choose(d,random.nextDouble(),now-lastSlam>=BellWardenRules.SLAM_REST,now-lastCharge>=BellWardenRules.CHARGE_REST,now-lastToll>=BellWardenRules.TOLL_REST);
			// A long chase ends in a charge from further out.
			if(pick<0 && chased>=80 && d<=BellWardenRules.CHARGE_REACH+3 && now-lastCharge>=BellWardenRules.CHARGE_REST/2)pick=BellWardenRules.CHARGE;
			if(pick>=0){
				chased=0;aim=yawTo(target.position());struck.clear();
				switch(pick){
					case BellWardenRules.SLAM->{lastSlam=now;pose(BellWardenRules.SLAM,0,BellWardenRules.SLAM_TELL);Feels.sound(level,bell(),"aura_bell_warden_rise",1,1.5F);}
					case BellWardenRules.TOLL->beginToll(level);
					case BellWardenRules.CHARGE->{lastCharge=now;pose(BellWardenRules.CHARGE,0,BellWardenRules.CHARGE_TELL);Feels.sound(level,bell(),"aura_bell_warden_charge",1.6F,1);}
					default->pose(BellWardenRules.SWING,0,BellWardenRules.SWING_TELL);
				}
				return;
			}
		}
		if(d>BellWardenRules.CLOSE){
			chased++;
			if(!getNavigation().moveTo(target,1))getMoveControl().setWantedPosition(target.getX(),target.getY(),target.getZ(),1);
		}else{chased=0;getNavigation().stop();face(yawTo(target.position()));}
	}

	/**
	 * The clapper, swung across its front once the tell runs out and held out through {@link BellWardenRules#SWING_HOLD}; then, if its
	 * prey is still in reach and it has the mind to, brought down overhand along a fresh aim.
	 */
	private void swing(ServerLevel level,LivingEntity target){
		face(aim);still();
		int move=move(),tell=tell();long el=since();boolean hammer=move==BellWardenRules.HAMMER;
		if(el==tell){
			Feels.sound(level,bell().add(0,-1.5,0),hammer?"aura_bell_warden_hammer":"aura_bell_warden_swing",1.4F,.9F+random.nextFloat()*.2F);
			for(var p:prey(level))if(reaches(move,p))hit(level,p,hammer?BellWardenRules.HAMMER_DAMAGE:BellWardenRules.SWING_DAMAGE,hammer?.4:1.1,position(),hammer?0:.2);
			if(hammer){var at=position().add(ahead(aim).scale(3.4));level.sendParticles(ParticleTypes.SPLASH,at.x,at.y+.1,at.z,24,.5,.1,.5,.25);level.sendParticles(ParticleTypes.CRIT,at.x,at.y+.2,at.z,10,.4,.1,.4,.2);}
		}
		if(el<tell+BellWardenRules.SWING_HOLD)return;
		if(!hammer && target!=null && distanceTo(target)<=BellWardenRules.CLOSE+1.5 && BellWardenRules.hammers(random.nextDouble(),wounded())){
			aim=yawTo(target.position());pose(BellWardenRules.HAMMER,0,BellWardenRules.HAMMER_TELL);
		}else spent(hammer?BellWardenRules.HAMMER_RECOVER:BellWardenRules.SWING_RECOVER);
	}

	/** Both fists raised while it tracks its prey, the aim locked for the last three ticks; then down on the floor ahead of it. */
	private void slam(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell-3 && target!=null)aim=yawTo(target.position());
		face(aim);still();
		if(el==tell){
			var at=position().add(ahead(aim).scale(BellWardenRules.SLAM_AHEAD));
			Feels.sound(level,at,"aura_bell_warden_slam",1.6F,.95F+random.nextFloat()*.1F);
			for(int i=0;i<20;i++){double a=i*Math.PI*2/20;level.sendParticles(ParticleTypes.SPLASH,at.x+Math.cos(a)*BellWardenRules.SLAM_RADIUS*.7,at.y+.1,at.z+Math.sin(a)*BellWardenRules.SLAM_RADIUS*.7,3,.1,0,.1,.2);}
			level.sendParticles(ParticleTypes.CLOUD,at.x,at.y+.2,at.z,8,.8,.1,.8,.05);
			for(var p:prey(level))if(reaches(BellWardenRules.SLAM,p))hit(level,p,BellWardenRules.SLAM_DAMAGE,.8,at,.45);
		}
		if(el>=tell+2)spent(BellWardenRules.SLAM_RECOVER);
	}

	/**
	 * Head down through the tell while it tracks its prey, the aim locked for the last four ticks; then its whole weight run along it,
	 * running down whoever is in the way. A wall or pillar stops it short, ringing its bell and leaving it dazed a while longer.
	 */
	private void charge(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell-4 && target!=null)aim=yawTo(target.position());
		face(aim);
		if(el<=tell){still();return;}
		var step=ahead(aim).scale(BellWardenRules.CHARGE_REACH/BellWardenRules.CHARGE_TICKS);
		move(MoverType.SELF,new Vec3(step.x,-.2,step.z));setDeltaMovement(Vec3.ZERO);
		if(el%2==0){Feels.sound(level,position(),"aura_bell_warden_step",1.3F,1.1F);level.sendParticles(ParticleTypes.SPLASH,getX(),getY()+.1,getZ(),10,.6,0,.6,.2);}
		for(var p:prey(level))if(!struck.contains(p.getUUID()) && reaches(BellWardenRules.CHARGE,p)){struck.add(p.getUUID());hit(level,p,BellWardenRules.CHARGE_DAMAGE,1.5,position().subtract(ahead(aim)),.35);}
		if(horizontalCollision){
			toll(level,1.4F,1.2F);level.sendParticles(ParticleTypes.CRIT,bell().x,bell().y,bell().z,16,.8,.6,.8,.3);
			spent(BellWardenRules.CHARGE_RECOVER+16);
		}else if(el>=tell+BellWardenRules.CHARGE_TICKS)spent(BellWardenRules.CHARGE_RECOVER);
	}

	/** Clapper raised to its own bell: once, or more as it weakens. */
	private void beginToll(ServerLevel level){
		lastToll=level.getGameTime();tollsLeft=BellWardenRules.tolls(getHealth(),getMaxHealth());
		pose(BellWardenRules.TOLL,0,BellWardenRules.TOLL_TELL);still();
		Feels.sound(level,bell(),"aura_bell_warden_gather",1.4F,1);
	}
	/**
	 * Turning to face its prey with the clapper raised; then struck, and a ring rolls out from its feet. Again, quicker, while it has
	 * tolls left; then it is spent.
	 */
	private void knell(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(target!=null && el<tell)aim=yawTo(target.position());
		face(aim);still();
		if(el==tell){
			var b=bell();
			Feels.sound(level,b,"aura_bell_warden_knell",2.2F,.95F+random.nextFloat()*.1F);
			for(int i=0;i<24;i++){double a=i*Math.PI*2/24;level.sendParticles(ParticleTypes.GLOW,b.x+Math.cos(a)*1.6,b.y-.6,b.z+Math.sin(a)*1.6,1,0,0,0,0);}
			level.sendParticles(ParticleTypes.SPLASH,getX(),getY()+.1,getZ(),40,1.4,.1,1.4,.4);
			waves.add(new Wave(position(),level.getGameTime(),new HashSet<>()));
		}
		if(el<tell+3)return;
		if(--tollsLeft>0){pose(BellWardenRules.TOLL,0,BellWardenRules.TOLL_AGAIN);Feels.sound(level,bell(),"aura_bell_warden_gather",1.4F,1.15F);}
		else spent(BellWardenRules.TOLL_RECOVER);
	}
	/** Its rings rolling out across the floor, each catching whoever has not jumped it as it passes. */
	private void waves(ServerLevel level){
		long now=level.getGameTime();var prey=prey(level);
		for(var it=waves.iterator();it.hasNext();){
			var w=it.next();long age=now-w.born();
			if(BellWardenRules.waveDone(age) || !isAlive()){it.remove();continue;}
			double r=BellWardenRules.waveRadius(age);
			int n=Math.min(64,Math.max(10,(int)(r*5)));double off=random.nextDouble()*Math.PI*2/n;
			for(int i=0;i<n;i++){double a=off+i*Math.PI*2/n;level.sendParticles(i%3==0?ParticleTypes.GLOW:ParticleTypes.SPLASH,w.at().x+Math.cos(a)*r,w.at().y+.15,w.at().z+Math.sin(a)*r,1,0,.05,0,0);}
			for(var p:prey){
				if(w.struck().contains(p.getUUID()))continue;
				double dx=p.getX()-w.at().x,dz=p.getZ()-w.at().z;
				if(BellWardenRules.waveHits(Math.sqrt(dx*dx+dz*dz),r,p.getY()-w.at().y)){w.struck().add(p.getUUID());hit(level,p,BellWardenRules.TOLL_DAMAGE,1.0,w.at(),.4);}
			}
		}
	}
	/** Spent after an attack, its bell tipped back, for {@code ticks}. */
	private void spent(int ticks){pose(BellWardenRules.RECOVER,ticks);still();}
	/** Hurt too fast: down under its bell for a while. */
	private void shellDown(ServerLevel level){
		lastShell=level.getGameTime();stung=0;pose(BellWardenRules.SHELL,BellWardenRules.SHELL_TICKS);still();
		Feels.sound(level,bell(),"aura_bell_warden_shell",1.4F,1);
		level.sendParticles(ParticleTypes.SPLASH,getX(),getY()+.1,getZ(),30,1.2,.1,1.2,.3);
	}
	/** Standing its ground: no sliding about, though it still falls. */
	private void still(){getNavigation().stop();var v=getDeltaMovement();setDeltaMovement(0,Math.min(0,v.y),0);}
	private boolean wounded(){return BellWardenRules.wounded(getHealth(),getMaxHealth());}
	/** Ticks before its next blow. */
	private int gap(){return (wounded()?BellWardenRules.GAP_WOUNDED:BellWardenRules.GAP)+random.nextInt(3);}

	/** Players it could be fighting right now. */
	private List<ServerPlayer> prey(ServerLevel level){
		var out=new ArrayList<ServerPlayer>();
		for(var p:level.players())if(p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(this)<14*14 && inHall(p.position(),BellWardenRules.LEASH))out.add(p);
		return out;
	}
	/** Whether {@code move}, along the locked aim, reaches {@code p}. */
	private boolean reaches(int move,LivingEntity p){
		var f=ahead(aim);double dx=p.getX()-getX(),dz=p.getZ()-getZ();
		return BellWardenRules.hits(move,dx*f.x+dz*f.z,dx*-f.z+dz*f.x,p.getY()-getY());
	}
	/** A blow landing on {@code p}, through part of its armour, knocking it {@code shove} away from {@code from} and {@code lift} up. */
	private void hit(ServerLevel level,LivingEntity p,double damage,double shove,Vec3 from,double lift){
		if(!GuardianBite.land(level,this,p,(float)damage,BellWardenRules.PIERCE))return;
		if(shove>0)p.knockback(shove,from.x-p.getX(),from.z-p.getZ(),level.damageSources().mobAttack(this),0);
		if(lift>0)p.setDeltaMovement(p.getDeltaMovement().add(0,lift,0));
		p.needsSync=true;
	}
	private Vec3 ahead(float yaw){double r=Math.toRadians(yaw);return new Vec3(-Math.sin(r),0,Math.cos(r));}

	private float yawTo(Vec3 at){return (float)Math.toDegrees(Math.atan2(-(at.x-getX()),at.z-getZ()));}
	private void face(float yaw){setYRot(yaw);yBodyRot=yaw;yHeadRot=yaw;}

	@Override public boolean hurtServer(ServerLevel level,DamageSource source,float damage){
		if(source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.CRAMMING))return false;
		int move=move();
		if(BellWardenRules.scripted(move) && !source.is(DamageTypes.GENERIC_KILL)){
			// At its vigil the bell takes the blow; but a blow, or a shot, gets it up.
			if(move==BellWardenRules.DORMANT && Guardians.enabled() && source.getEntity() instanceof ServerPlayer p && !p.isCreative())wake(level,0);
			return false;
		}
		long now=level.getGameTime();
		if(!source.is(DamageTypes.GENERIC_KILL)){
			var at=source.getSourcePosition();
			boolean frontal=at!=null && BellWardenRules.frontal(yawTo(at)-getYRot());
			// It sees a blow coming and heaves itself back out from under it.
			if(source.getDirectEntity()!=null && source.getDirectEntity()==source.getEntity() && BellWardenRules.dodges(move,frontal,now-lastDodge,random.nextDouble())){
				lastDodge=now;aim=getYRot();slide=ahead(aim).scale(-BellWardenRules.BACKSTEP_SPEED);
				pose(BellWardenRules.BACKSTEP,BellWardenRules.BACKSTEP_TICKS);
				Feels.sound(level,position().add(0,1,0),"aura_bell_warden_backstep",1.2F,.9F+random.nextFloat()*.2F);
				return false;
			}
			damage*=(float)BellWardenRules.armour(move,frontal);
			if(move==BellWardenRules.SHELL)ring(level,source);
		}
		boolean hurt=super.hurtServer(level,source,damage);
		if(hurt){
			lastCut=now;stung+=damage;
			if(BellWardenRules.shells(move(),stung,now-lastShell))shellDown(level);
		}
		return hurt;
	}
	/** A blow on its shelled bell: it rings out, and whoever struck it up close is thrown back by the ring. */
	private void ring(ServerLevel level,DamageSource source){
		long now=level.getGameTime();if(now-lastRing<4)return;lastRing=now;
		var b=bell();Feels.sound(level,b,"aura_bell_warden_ring",1.4F,.95F+random.nextFloat()*.1F);
		level.sendParticles(ParticleTypes.GLOW,b.x,b.y-.5,b.z,10,1,.6,1,.05);
		if(source.getEntity() instanceof LivingEntity e && source.getDirectEntity()==e && e.distanceToSqr(this)<5*5){e.knockback(.8,getX()-e.getX(),getZ()-e.getZ(),level.damageSources().mobAttack(this),0);e.needsSync=true;}
	}
	@Override public void die(DamageSource source){
		if(level() instanceof ServerLevel server){Feels.sound(server,bell(),"aura_bell_warden_death",1.8F,1);bar.removeAllPlayers();}
		super.die(source);
	}
	@Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return null;}
	@Override protected void actuallyHurt(ServerLevel level,DamageSource source,float damage){
		super.actuallyHurt(level,source,damage);
		if(random.nextInt(3)==0)Feels.sound(level,bell(),"aura_bell_warden_hurt",1.2F,.9F+random.nextFloat()*.2F);
	}
	@Override public void remove(RemovalReason reason){bar.removeAllPlayers();super.remove(reason);}
	@Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);bar.removePlayer(player);}

	@Override protected void addAdditionalSaveData(ValueOutput out){
		super.addAdditionalSaveData(out);
		out.putBoolean("warden_bound",bound);out.store("warden_vigil",BlockPos.CODEC,vigil);out.putFloat("warden_rest",rest);
		out.store("warden_hall_min",BlockPos.CODEC,hallMin);out.store("warden_hall_max",BlockPos.CODEC,hallMax);
		out.putBoolean("warden_awake",!BellWardenRules.scripted(move()));out.putLong("warden_ticked",lastTick);
	}
	@Override protected void readAdditionalSaveData(ValueInput in){
		super.readAdditionalSaveData(in);
		bound=in.getBooleanOr("warden_bound",false);
		vigil=in.read("warden_vigil",BlockPos.CODEC).orElse(blockPosition());rest=in.getFloatOr("warden_rest",getYRot());
		hallMin=in.read("warden_hall_min",BlockPos.CODEC).orElse(vigil);hallMax=in.read("warden_hall_max",BlockPos.CODEC).orElse(vigil);
		lastTick=in.getLongOr("warden_ticked",-1L);
		// Mid-fight it comes back on its feet, whatever it was doing when the chunk unloaded.
		if(in.getBooleanOr("warden_awake",false)){pose(BellWardenRules.WATCH,BellWardenRules.GAP);lastCut=0;}else pose(BellWardenRules.DORMANT,0);
	}
}
