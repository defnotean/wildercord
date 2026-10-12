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
 * The Ashen Stag ({@link AshenStagRules}). Generation leaves it bedded in the ash before the Ashen Glade's smouldering stump, bound
 * to that spot and to the glade, facing the gate. While it lies there it cannot be hurt; the first player far enough into the glade
 * (or the first blow) gets it up, hindquarters first, bellowing once its forelegs are under it, and then the boss bar comes up. The
 * glade going quiet sends it back to its bed, healed. The glade is its own: anything hostile in it, it burns out of the world.
 * <p>
 * On its feet it gores whoever is in front of it, kicks out behind at whoever circles to its back, rears and brings both forehooves
 * down on the ground ahead, and charges anyone keeping their distance, antlers lowered. A charge that runs into a trunk or the bank
 * drives its antlers in, and it stands there heaving to pull them free: the big opening. It bounds aside out from under some blows,
 * and hurt too fast it leaps away back across the glade.
 */
public final class AshenStag extends PathfinderMob implements Enemy {
	private static final EntityDataAccessor<Integer> MOVE=SynchedEntityData.defineId(AshenStag.class,EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN=SynchedEntityData.defineId(AshenStag.class,EntityDataSerializers.LONG);
	/** The current move's tell in ticks, so the client knows when the blow lands. */
	private static final EntityDataAccessor<Integer> TELL=SynchedEntityData.defineId(AshenStag.class,EntityDataSerializers.INT);
	/** Which way a bound carries it, for the client: -1 left, 1 right, 0 straight back. */
	private static final EntityDataAccessor<Integer> SIDE=SynchedEntityData.defineId(AshenStag.class,EntityDataSerializers.INT);
	private static final EntityDimensions BEDDED=EntityDimensions.scalable(2.4F,2.0F).withEyeHeight(1.7F),STANDING=EntityDimensions.scalable(2.4F,3.6F).withEyeHeight(3.0F);
	private final ServerBossEvent bar=new ServerBossEvent(getUUID(),getType().getDescription(),BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.NOTCHED_10);
	private boolean bound;
	private BlockPos bed=BlockPos.ZERO,gladeMin=BlockPos.ZERO,gladeMax=BlockPos.ZERO;
	private float rest;
	private int left,quiet;
	private long lastCut;
	/** The game time it last ran, so it knows how long the glade was left alone. */
	private long lastTick=-1;
	/** The line its blow was locked on, and where a bound carries it. */
	private float aim;
	private Vec3 slide=Vec3.ZERO;
	private int chased;
	private long lastDodge=-999,lastLeap=-999,lastStomp=-999,lastCharge=-999,lastKick=-999;
	/** Hurt taken lately, fading; enough of it sends it leaping away. */
	private double stung;
	/** Whoever this charge has already run down. */
	private final Set<UUID> struck=new HashSet<>();

	public AshenStag(EntityType<? extends AshenStag> type,Level level){super(type,level);xpReward=80;setPersistenceRequired();}
	public static AttributeSupplier.Builder createAttributes(){
		return createMobAttributes().add(Attributes.MAX_HEALTH,AshenStagRules.HEALTH).add(Attributes.ARMOR,AshenStagRules.ARMOUR).add(Attributes.ARMOR_TOUGHNESS,4)
			.add(Attributes.MOVEMENT_SPEED,AshenStagRules.SPEED).add(Attributes.ATTACK_DAMAGE,12).add(Attributes.FOLLOW_RANGE,40).add(Attributes.KNOCKBACK_RESISTANCE,1)
			.add(Attributes.STEP_HEIGHT,1.1);
	}

	/** Bind it to its bed: {@code at} the air above the ash where it lies, {@code yaw} the way it faces, {@code glade} the glade it keeps. */
	public void bind(BlockPos at,float yaw,BoundingBox glade){
		bound=true;bed=at.immutable();rest=yaw;
		gladeMin=new BlockPos(glade.minX(),glade.minY(),glade.minZ());gladeMax=new BlockPos(glade.maxX(),glade.maxY(),glade.maxZ());
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
		super.defineSynchedData(b);b.define(MOVE,AshenStagRules.DORMANT);b.define(BEGAN,0L);b.define(TELL,0);b.define(SIDE,0);
	}
	@Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(MOVE.equals(key))refreshDimensions();}
	@Override protected EntityDimensions getDefaultDimensions(Pose pose){return move()==AshenStagRules.DORMANT?BEDDED:STANDING;}
	@Override protected void registerGoals(){}
	@Override public boolean isPushable(){return false;}
	@Override protected void doPush(Entity entity){}
	@Override public boolean removeWhenFarAway(double distance){return false;}

	private void pose(int move,int ticks){pose(move,ticks,0);}
	private void pose(int move,int ticks,int tell){
		int was=move();entityData.set(MOVE,move);entityData.set(BEGAN,level().getGameTime());entityData.set(TELL,tell);left=ticks;getNavigation().stop();
		if(was!=move)refreshDimensions();
		if(!AshenStagRules.scripted(move))bar.setName(Component.translatable("boss.wildercord.ashen_stag.status",getType().getDescription(),
			Component.translatable("boss.wildercord.ashen_stag.move."+AshenStagRules.key(move))));
	}
	/** Back down in its bed, whole again. */
	private void bedDown(){
		pose(AshenStagRules.DORMANT,0);hold();setHealth(getMaxHealth());setTarget(null);bar.removeAllPlayers();quiet=0;clearFire();
	}
	/** Pinned to its bed while it lies there (or gets up or down). */
	private void hold(){
		var t=spot();setPos(t.x,t.y,t.z);setDeltaMovement(Vec3.ZERO);face(rest);
	}
	private boolean inGlade(Vec3 at,double margin){
		return AshenStagRules.within(at.x,at.y,at.z,gladeMin.getX(),gladeMin.getY(),gladeMin.getZ(),gladeMax.getX(),gladeMax.getY(),gladeMax.getZ(),margin);
	}

	@Override protected void customServerAiStep(ServerLevel level){
		super.customServerAiStep(level);if(!isAlive())return;
		if(!bound){var b=blockPosition();bind(b,getYRot(),new BoundingBox(b.getX()-17,b.getY()-1,b.getZ()-9,b.getX()+17,b.getY()+13,b.getZ()+25));}
		// Left alone long enough (its prey died and went home, say) and it has bedded down again in the meantime.
		long now=level.getGameTime();
		if(lastTick>=0 && move()!=AshenStagRules.DORMANT && AshenStagRules.forgets(now-lastTick))bedDown();
		lastTick=now;
		if(tickCount%AshenStagRules.GUARD_EVERY==0 && Guardians.enabled())guard(level);
		switch(move()){
			case AshenStagRules.DORMANT->dormant(level);
			case AshenStagRules.RISE->{hold();if(--left<=0)stand(level);else if(left==AshenStagRules.RISE_TICKS-AshenStagRules.BELLOW_AT)bellow(level,1.6F,1);}
			case AshenStagRules.BED->{hold();if(wakeful(level)!=null)wake(level,AshenStagRules.RISE_TICKS-left);else if(--left<=0)bedDown();}
			default->awake(level);
		}
		if(tickCount%3==0)embers(level);
	}

	/** Its antlers smoulder whatever it is doing; on its feet, its cracks shed sparks. */
	private void embers(ServerLevel level){
		var h=head();
		level.sendParticles(ParticleTypes.SMOKE,h.x,h.y+.9,h.z,1,.5,.2,.5,.01);
		if(random.nextInt(3)==0)level.sendParticles(ParticleTypes.SMALL_FLAME,h.x,h.y+1,h.z,1,.5,.3,.5,.005);
		if(!AshenStagRules.scripted(move()) && random.nextInt(2)==0)level.sendParticles(ParticleTypes.WHITE_ASH,getX(),getY()+1.8,getZ(),3,.8,.5,.8,0);
	}
	/** Its bellow: the sound, and a breath of sparks and smoke off its antlers. */
	private void bellow(ServerLevel level,float volume,float pitch){
		var h=head();
		Feels.sound(level,h,"aura_ashen_stag_bellow",volume,pitch);
		level.sendParticles(ParticleTypes.LAVA,h.x,h.y+1,h.z,6,.6,.3,.6,0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE,h.x,h.y+.8,h.z,10,.6,.3,.6,.02);
		var f=ahead(getYRot());level.sendParticles(ParticleTypes.FLAME,h.x+f.x,h.y,h.z+f.z,12,.3,.2,.3,.05);
	}
	/** Its head, more or less: up by its antlers on its feet, low over the ash in its bed. */
	private Vec3 head(){
		boolean down=move()==AshenStagRules.DORMANT;var f=ahead(getYRot());
		return position().add(f.scale(down?1.3:1.7)).add(0,down?1.5:3.0,0);
	}

	/**
	 * The glade is its own: anything hostile that wanders or spawns in, bedded or standing, it burns out of the world, one every
	 * {@link AshenStagRules#GUARD_EVERY} ticks, nearest first. Only players get a fight.
	 */
	private void guard(ServerLevel level){
		var box=new AABB(Vec3.atLowerCornerOf(gladeMin),Vec3.atLowerCornerOf(gladeMax).add(1,1,1));
		var h=head();
		var m=level.getEntitiesOfClass(Mob.class,box,e->e instanceof Enemy && e!=this && e.isAlive() && !(e instanceof AshenStag) && inGlade(e.position(),0))
			.stream().min(Comparator.comparingDouble(e->e.distanceToSqr(h))).orElse(null);
		if(m==null)return;
		var to=m.position().add(0,m.getBbHeight()/2,0);
		var step=to.subtract(h);int n=Math.max(2,(int)(step.length()*2));
		for(int i=0;i<=n;i++){var at=h.add(step.scale(i/(double)n));level.sendParticles(ParticleTypes.FLAME,at.x,at.y,at.z,1,.05,.05,.05,0);}
		level.sendParticles(ParticleTypes.LAVA,to.x,to.y,to.z,4,.3,.3,.3,0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE,to.x,to.y,to.z,8,.3,.4,.3,.03);
		Feels.sound(level,h,"aura_ashen_stag_snort",.8F,1.2F+random.nextFloat()*.2F);
		m.hurtServer(level,damageSources().mobAttack(this),Float.MAX_VALUE);
		if(m.isAlive())m.kill(level);
	}

	/** The nearest player in the glade close enough to its bed to wake it, if any. */
	private ServerPlayer wakeful(ServerLevel level){
		var t=spot();
		return level.players().stream().filter(p->p.isAlive() && !p.isCreative() && !p.isSpectator() && inGlade(p.position(),0)
			&& AshenStagRules.wakes(p.getX()-t.x,p.getY()-t.y,p.getZ()-t.z)).min(Comparator.comparingDouble(p->p.distanceToSqr(t))).orElse(null);
	}

	private void dormant(ServerLevel level){
		hold();if(getHealth()<getMaxHealth())setHealth(getMaxHealth());
		if(tickCount%5==0 && Guardians.enabled() && wakeful(level)!=null)wake(level,0);
		else if(random.nextInt(120)==0){
			var h=head();Feels.sound(level,h,"aura_ashen_stag_breath",.7F,.9F+random.nextFloat()*.2F);
			var f=ahead(rest);level.sendParticles(ParticleTypes.SMOKE,h.x+f.x*.8,h.y-.4,h.z+f.z*.8,6,.15,.05,.15,.02);
		}
	}

	/** Begin getting up, {@code already} ticks in (when it is woken half way back down into its bed). */
	private void wake(ServerLevel level,int already){
		pose(AshenStagRules.RISE,AshenStagRules.RISE_TICKS-already);entityData.set(BEGAN,level.getGameTime()-already);
		Feels.sound(level,head(),"aura_ashen_stag_rise",1.4F,1);
		level.sendParticles(ParticleTypes.WHITE_ASH,getX(),getY()+.3,getZ(),40,1.2,.2,1.6,.05);
	}

	/** On its feet: it stamps, tosses its antlers, and the fight is on. */
	private void stand(ServerLevel level){
		pose(AshenStagRules.WATCH,AshenStagRules.GAP);hold();
		lastCut=level.getGameTime();quiet=0;
		Feels.sound(level,position(),"aura_ashen_stag_stomp",1.2F,1.1F);
		level.sendParticles(ParticleTypes.LARGE_SMOKE,getX(),getY()+.2,getZ(),20,1.2,.1,1.4,.04);
	}

	private void awake(ServerLevel level){
		if(!inGlade(position(),AshenStagRules.LEASH)){
			var at=spot();teleportTo(at.x,at.y,at.z);setDeltaMovement(Vec3.ZERO);getNavigation().stop();pose(AshenStagRules.WATCH,AshenStagRules.GAP);
		}
		long now=level.getGameTime();float heal=AshenStagRules.regen(getMaxHealth(),now-lastCut);if(heal>0 && getHealth()<getMaxHealth())heal(heal);
		if(tickCount%10==0){
			bar.setProgress(getHealth()/getMaxHealth());
			for(var p:new ArrayList<>(bar.getPlayers()))if(p.level()!=level || !p.isAlive() || !inGlade(p.position(),AshenStagRules.BAR_DROP))bar.removePlayer(p);
			for(var p:level.players())if(p.isAlive() && !p.isSpectator() && inGlade(p.position(),AshenStagRules.BAR_SHOW))bar.addPlayer(p);
			setTarget(level.players().stream().filter(p->p.isAlive() && !p.isCreative() && !p.isSpectator() && inGlade(p.position(),0))
				.min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null));
		}
		stung*=AshenStagRules.STING_FADE;
		var target=getTarget();
		switch(move()){
			case AshenStagRules.GORE->gore(level);
			case AshenStagRules.KICK->kick(level);
			case AshenStagRules.STOMP->stomp(level,target);
			case AshenStagRules.CHARGE->charge(level,target);
			case AshenStagRules.BOUND->{face(aim);move(MoverType.SELF,new Vec3(slide.x,-.3,slide.z));setDeltaMovement(Vec3.ZERO);if(--left<=0)pose(AshenStagRules.WATCH,2);}
			case AshenStagRules.STUCK->{still();stuck(level);if(--left<=0)free(level);}
			case AshenStagRules.RECOVER->{still();if(--left<=0)pose(AshenStagRules.WATCH,gap());}
			default->watch(level,target);
		}
	}

	/** On its feet and tracking: closing on its prey, and turning only so fast, so its back can be got at. */
	private void watch(ServerLevel level,LivingEntity target){
		if(getDeltaMovement().horizontalDistanceSqr()>.0004 && tickCount%10==0)Feels.sound(level,position(),"aura_ashen_stag_step",1,.9F+random.nextFloat()*.2F);
		if(target==null){
			quiet++;
			var at=spot();
			if(quiet>=AshenStagRules.RETURN_TICKS && distanceToSqr(at)>1.5)getNavigation().moveTo(at.x,at.y,at.z,1);
			if(quiet>=AshenStagRules.RESET_TICKS && (distanceToSqr(at)<=2.5 || quiet>=AshenStagRules.RESET_TICKS+100)){
				bar.removeAllPlayers();pose(AshenStagRules.BED,AshenStagRules.RISE_TICKS);hold();Feels.sound(level,head(),"aura_ashen_stag_rise",1,.8F);
			}
			return;
		}
		quiet=0;
		getLookControl().setLookAt(target,30,30);
		double d=distanceTo(target);
		boolean behind=AshenStagRules.behind(yawTo(target.position())-getYRot());
		if(left>0)left--;
		if(left<=0){
			long now=level.getGameTime();
			int pick=AshenStagRules.choose(d,behind,random.nextDouble(),now-lastStomp>=AshenStagRules.STOMP_REST,now-lastCharge>=AshenStagRules.CHARGE_REST,now-lastKick>=AshenStagRules.KICK_REST);
			// A long chase ends in a charge from further out.
			if(pick<0 && chased>=60 && d<=AshenStagRules.CHARGE_REACH+3 && now-lastCharge>=AshenStagRules.CHARGE_REST/2)pick=AshenStagRules.CHARGE;
			if(pick>=0){
				chased=0;struck.clear();
				aim=pick==AshenStagRules.KICK?getYRot():yawTo(target.position());
				switch(pick){
					case AshenStagRules.KICK->{lastKick=now;pose(AshenStagRules.KICK,0,AshenStagRules.KICK_TELL);Feels.sound(level,position(),"aura_ashen_stag_snort",1,.9F);}
					case AshenStagRules.STOMP->{lastStomp=now;pose(AshenStagRules.STOMP,0,AshenStagRules.STOMP_TELL);bellow(level,1.2F,1.2F);}
					case AshenStagRules.CHARGE->{lastCharge=now;pose(AshenStagRules.CHARGE,0,AshenStagRules.CHARGE_TELL);Feels.sound(level,head(),"aura_ashen_stag_paw",1.4F,1);}
					default->{pose(AshenStagRules.GORE,0,AshenStagRules.GORE_TELL);Feels.sound(level,head(),"aura_ashen_stag_snort",1.2F,1);}
				}
				return;
			}
		}
		if(d>AshenStagRules.CLOSE){
			chased++;
			if(!getNavigation().moveTo(target,1))getMoveControl().setWantedPosition(target.getX(),target.getY(),target.getZ(),1);
		}else{chased=0;getNavigation().stop();turn(yawTo(target.position()),10);}
	}

	/** Head dropped through the tell, then the antlers swept up through whoever is in front of it, and held up a moment. */
	private void gore(ServerLevel level){
		face(aim);still();
		long el=since();int tell=tell();
		if(el==tell){
			Feels.sound(level,head(),"aura_ashen_stag_gore",1.4F,.9F+random.nextFloat()*.2F);
			var h=head();level.sendParticles(ParticleTypes.FLAME,h.x,h.y,h.z,14,.5,.6,.5,.06);
			for(var p:prey(level))if(reaches(AshenStagRules.GORE,p))hit(level,p,AshenStagRules.GORE_DAMAGE,.9,position(),.55);
		}
		if(el>=tell+AshenStagRules.GORE_HOLD)spent(AshenStagRules.GORE_RECOVER);
	}
	/** Weight shifted onto its forelegs through the tell, then both hind hooves lashed out behind it. */
	private void kick(ServerLevel level){
		face(aim);still();
		long el=since();int tell=tell();
		if(el==tell){
			var back=position().subtract(ahead(aim).scale(2.2));
			Feels.sound(level,back,"aura_ashen_stag_kick",1.4F,.9F+random.nextFloat()*.2F);
			level.sendParticles(ParticleTypes.WHITE_ASH,back.x,back.y+1,back.z,20,.6,.4,.6,.1);
			for(var p:prey(level))if(reaches(AshenStagRules.KICK,p))hit(level,p,AshenStagRules.KICK_DAMAGE,1.6,position(),.3);
		}
		if(el>=tell+2)spent(AshenStagRules.KICK_RECOVER);
	}
	/** Reared up on its hind legs while it tracks its prey, the aim locked for the last three ticks; then both forehooves down ahead of it. */
	private void stomp(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell-3 && target!=null)aim=yawTo(target.position());
		face(aim);still();
		if(el==tell){
			var at=position().add(ahead(aim).scale(AshenStagRules.STOMP_AHEAD));
			Feels.sound(level,at,"aura_ashen_stag_stomp",1.6F,.95F+random.nextFloat()*.1F);
			for(int i=0;i<20;i++){double a=i*Math.PI*2/20,r=AshenStagRules.STOMP_RADIUS*.7;level.sendParticles(i%2==0?ParticleTypes.FLAME:ParticleTypes.LARGE_SMOKE,at.x+Math.cos(a)*r,at.y+.1,at.z+Math.sin(a)*r,1,.1,0,.1,.03);}
			level.sendParticles(ParticleTypes.WHITE_ASH,at.x,at.y+.2,at.z,30,1,.2,1,.1);
			level.sendParticles(ParticleTypes.LAVA,at.x,at.y+.2,at.z,5,.6,.1,.6,0);
			for(var p:prey(level))if(reaches(AshenStagRules.STOMP,p))hit(level,p,AshenStagRules.STOMP_DAMAGE,.8,at,.5);
		}
		if(el>=tell+2)spent(AshenStagRules.STOMP_RECOVER);
	}

	/**
	 * Pawing the ash with its head down through the tell while it tracks its prey, the aim locked for the last four ticks; then its
	 * whole weight run along it, running down whoever is in the way. A trunk or the bank stops it short with its antlers driven in.
	 */
	private void charge(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell-4 && target!=null)aim=yawTo(target.position());
		face(aim);
		if(el<=tell){still();if(el%4==0)level.sendParticles(ParticleTypes.WHITE_ASH,getX(),getY()+.1,getZ(),8,.4,0,.4,.05);return;}
		var step=ahead(aim).scale(AshenStagRules.CHARGE_REACH/AshenStagRules.CHARGE_TICKS);
		move(MoverType.SELF,new Vec3(step.x,-.3,step.z));setDeltaMovement(Vec3.ZERO);
		if(el%2==0)Feels.sound(level,position(),"aura_ashen_stag_step",1.3F,1.2F);
		level.sendParticles(ParticleTypes.LARGE_SMOKE,getX(),getY()+.3,getZ(),3,.5,.1,.5,.01);
		for(var p:prey(level))if(!struck.contains(p.getUUID()) && reaches(AshenStagRules.CHARGE,p)){struck.add(p.getUUID());hit(level,p,AshenStagRules.CHARGE_DAMAGE,1.6,position().subtract(ahead(aim)),.4);}
		if(horizontalCollision)stick(level);
		else if(el>=tell+AshenStagRules.CHARGE_TICKS)spent(AshenStagRules.CHARGE_RECOVER);
	}
	/** Run into something: its antlers driven in, and stuck there. */
	private void stick(ServerLevel level){
		pose(AshenStagRules.STUCK,AshenStagRules.STUCK_TICKS);still();
		var h=head();
		Feels.sound(level,h,"aura_ashen_stag_crash",1.6F,1);
		level.sendParticles(ParticleTypes.CRIT,h.x,h.y,h.z,16,.6,.6,.6,.3);
		level.sendParticles(ParticleTypes.LARGE_SMOKE,h.x,h.y,h.z,14,.6,.6,.6,.03);
	}
	/** Heaving at its antlers now and then while it is stuck. */
	private void stuck(ServerLevel level){
		if(since()%15==8){var h=head();Feels.sound(level,h,"aura_ashen_stag_strain",1.2F,.9F+random.nextFloat()*.2F);level.sendParticles(ParticleTypes.WHITE_ASH,h.x,h.y,h.z,12,.5,.4,.5,.05);}
	}
	/** Antlers wrenched free, and it backs off a step, shaking its head. */
	private void free(ServerLevel level){
		Feels.sound(level,head(),"aura_ashen_stag_snort",1.4F,.8F);
		aim=getYRot();slide=ahead(aim).scale(-.3);entityData.set(SIDE,0);pose(AshenStagRules.BOUND,4,4);
	}

	/** Spent after an attack, head hanging and sides heaving, for {@code ticks}. */
	private void spent(int ticks){pose(AshenStagRules.RECOVER,ticks);still();}
	/** Away from a blow, to one side, quick on its feet. */
	private void boundAside(ServerLevel level){
		aim=getYRot();int side=random.nextBoolean()?1:-1;var f=ahead(aim);
		slide=new Vec3(-f.z*side,0,f.x*side).scale(AshenStagRules.BOUND_SPEED).add(f.scale(-.15));
		entityData.set(SIDE,side);pose(AshenStagRules.BOUND,AshenStagRules.BOUND_TICKS,AshenStagRules.BOUND_TICKS);
		Feels.sound(level,position().add(0,1,0),"aura_ashen_stag_bound",1.2F,.9F+random.nextFloat()*.2F);
		level.sendParticles(ParticleTypes.WHITE_ASH,getX(),getY()+.1,getZ(),16,.6,.1,.6,.08);
	}
	/** Hurt too fast: a leap away from whoever is hurting it, back toward the middle of the glade. */
	private void leapAway(ServerLevel level,Vec3 from){
		lastLeap=level.getGameTime();stung=0;
		var away=from==null?ahead(getYRot()).scale(-1):new Vec3(getX()-from.x,0,getZ()-from.z);
		var home=spot().subtract(position()).multiply(1,0,1);
		if(home.lengthSqr()>16)away=away.normalize().add(home.normalize());
		if(away.lengthSqr()<1e-4)away=ahead(getYRot()).scale(-1);
		aim=getYRot();slide=away.normalize().scale(AshenStagRules.LEAP_SPEED);
		var f=ahead(aim);double side=slide.x*-f.z+slide.z*f.x;
		entityData.set(SIDE,Math.abs(side)<.3?0:side>0?1:-1);pose(AshenStagRules.BOUND,AshenStagRules.LEAP_TICKS,AshenStagRules.LEAP_TICKS);
		bellow(level,1.4F,1.3F);
	}
	/** Standing its ground: no sliding about, though it still falls. */
	private void still(){getNavigation().stop();var v=getDeltaMovement();setDeltaMovement(0,Math.min(0,v.y),0);}
	private boolean wounded(){return AshenStagRules.wounded(getHealth(),getMaxHealth());}
	/** Ticks before its next blow. */
	private int gap(){return (wounded()?AshenStagRules.GAP_WOUNDED:AshenStagRules.GAP)+random.nextInt(3);}

	/** Players it could be fighting right now. */
	private List<ServerPlayer> prey(ServerLevel level){
		var out=new ArrayList<ServerPlayer>();
		for(var p:level.players())if(p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(this)<14*14 && inGlade(p.position(),AshenStagRules.LEASH))out.add(p);
		return out;
	}
	/** Whether {@code move}, along the locked aim, reaches {@code p}. */
	private boolean reaches(int move,LivingEntity p){
		var f=ahead(aim);double dx=p.getX()-getX(),dz=p.getZ()-getZ();
		return AshenStagRules.hits(move,dx*f.x+dz*f.z,dx*-f.z+dz*f.x,p.getY()-getY());
	}
	/** A blow landing on {@code p}, through part of its armour, knocking it {@code shove} away from {@code from} and {@code lift} up. */
	private void hit(ServerLevel level,LivingEntity p,double damage,double shove,Vec3 from,double lift){
		if(!GuardianBite.land(level,this,p,(float)damage,AshenStagRules.PIERCE))return;
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
		if(AshenStagRules.scripted(move) && !source.is(DamageTypes.GENERIC_KILL)){
			// In its bed the ash and bark take the blow; but a blow, or a shot, gets it up.
			if(move==AshenStagRules.DORMANT && Guardians.enabled() && source.getEntity() instanceof ServerPlayer p && !p.isCreative())wake(level,0);
			return false;
		}
		long now=level.getGameTime();
		if(!source.is(DamageTypes.GENERIC_KILL)){
			var at=source.getSourcePosition();
			boolean frontal=at!=null && AshenStagRules.frontal(yawTo(at)-getYRot());
			// It sees a blow coming and bounds out from under it.
			if(source.getDirectEntity()!=null && source.getDirectEntity()==source.getEntity() && AshenStagRules.dodges(move,now-lastDodge,random.nextDouble())){
				lastDodge=now;boundAside(level);
				return false;
			}
			damage*=(float)AshenStagRules.armour(move,frontal);
		}
		boolean hurt=super.hurtServer(level,source,damage);
		if(hurt){
			lastCut=now;stung+=damage;
			if(AshenStagRules.leaps(move(),stung,now-lastLeap))leapAway(level,source.getSourcePosition());
		}
		return hurt;
	}
	@Override public void die(DamageSource source){
		if(level() instanceof ServerLevel server){Feels.sound(server,head(),"aura_ashen_stag_death",1.8F,1);bar.removeAllPlayers();}
		super.die(source);
	}
	@Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return null;}
	@Override protected void actuallyHurt(ServerLevel level,DamageSource source,float damage){
		super.actuallyHurt(level,source,damage);
		if(random.nextInt(3)==0)Feels.sound(level,head(),"aura_ashen_stag_hurt",1.2F,.9F+random.nextFloat()*.2F);
	}
	@Override public void remove(RemovalReason reason){bar.removeAllPlayers();super.remove(reason);}
	@Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);bar.removePlayer(player);}

	@Override protected void addAdditionalSaveData(ValueOutput out){
		super.addAdditionalSaveData(out);
		out.putBoolean("stag_bound",bound);out.store("stag_bed",BlockPos.CODEC,bed);out.putFloat("stag_rest",rest);
		out.store("stag_glade_min",BlockPos.CODEC,gladeMin);out.store("stag_glade_max",BlockPos.CODEC,gladeMax);
		out.putBoolean("stag_awake",!AshenStagRules.scripted(move()));out.putLong("stag_ticked",lastTick);
	}
	@Override protected void readAdditionalSaveData(ValueInput in){
		super.readAdditionalSaveData(in);
		bound=in.getBooleanOr("stag_bound",false);
		bed=in.read("stag_bed",BlockPos.CODEC).orElse(blockPosition());rest=in.getFloatOr("stag_rest",getYRot());
		gladeMin=in.read("stag_glade_min",BlockPos.CODEC).orElse(bed);gladeMax=in.read("stag_glade_max",BlockPos.CODEC).orElse(bed);
		lastTick=in.getLongOr("stag_ticked",-1L);
		// Mid-fight it comes back on its feet, whatever it was doing when the chunk unloaded.
		if(in.getBooleanOr("stag_awake",false)){pose(AshenStagRules.WATCH,AshenStagRules.GAP);lastCut=0;}else pose(AshenStagRules.DORMANT,0);
	}
}
