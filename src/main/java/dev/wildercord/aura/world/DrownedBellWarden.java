package dev.wildercord.aura.world;

import dev.wildercord.cast.MoveDeck;
import dev.wildercord.cast.feel.Feels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
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
import net.minecraft.world.level.block.state.BlockState;
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
 * On its feet it never makes the same move twice running. Close in it whirls its clapper all the way round, brings it down overhand
 * along a line that splits the floor and sticks there, or lifts the bell off its own head and drops it over a marked circle; further
 * off it lashes its chain out and drags back whoever it catches, punches the floor so that water spouts up under its prey, charges
 * with its bell lowered and rings it on whatever stops it, makes its bell hum in a cone that follows its prey, or pours water from its
 * bell's lip and drags the whole floor in toward its fists. Struck, its bell sends a ring rolling along the floor. Half beaten it tears
 * its bell open and fights on cracked, quicker, in pairs, and harder. It steps back out from under some blows, and hurt too fast it
 * hunches down under its bell until the moment passes.
 */
public final class DrownedBellWarden extends PathfinderMob implements Enemy {
	private static final EntityDataAccessor<Integer> MOVE=SynchedEntityData.defineId(DrownedBellWarden.class,EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN=SynchedEntityData.defineId(DrownedBellWarden.class,EntityDataSerializers.LONG);
	/** The current move's tell in ticks, so the client knows when the blow lands. */
	private static final EntityDataAccessor<Integer> TELL=SynchedEntityData.defineId(DrownedBellWarden.class,EntityDataSerializers.INT);
	/** Whether it has torn its bell open, half beaten. */
	private static final EntityDataAccessor<Boolean> CRACKED=SynchedEntityData.defineId(DrownedBellWarden.class,EntityDataSerializers.BOOLEAN);
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
	private long lastDodge=-999,lastShell=-999,lastRing=-999;
	/** Its last few moves, so it does not fall into a habit, and when it last made each. */
	private final MoveDeck deck=new MoveDeck(BellWardenRules.MEMORY);
	private final long[] lastMade=new long[32];{Arrays.fill(lastMade,-9999);}
	/** Moves run straight on into one another this breath; whether this one is the second go at itself; whether it came off another. */
	private int links,again;private boolean chained;
	/** Rings rolling out along the floor: from where, when, how fast, how far, what they do, and who each has already caught. */
	private static final class Wave{
		final Vec3 at;final long born;final double speed,max,damage,lift;final Set<UUID> struck=new HashSet<>();
		Wave(Vec3 at,long born,double speed,double max,double damage,double lift){this.at=at;this.born=born;this.speed=speed;this.max=max;this.damage=damage;this.lift=lift;}
	}
	private final List<Wave> waves=new ArrayList<>();
	/** Marked floor bubbling up to spout: where, when it bursts, whether it is the second volley, and who it has caught. */
	private static final class Volley{
		final List<Vec3> at;final long bursts;final boolean second;final Set<UUID> struck=new HashSet<>();
		Volley(List<Vec3> at,long bursts,boolean second){this.at=at;this.bursts=bursts;this.second=second;}
	}
	private final List<Volley> volleys=new ArrayList<>();
	/** Where its bell comes down, or is down. */
	private Vec3 drop;
	/** Hurt taken lately, fading; enough of it sends it under its bell. */
	private double stung;
	/** Whoever this move has already caught. */
	private final Set<UUID> struck=new HashSet<>();

	public DrownedBellWarden(EntityType<? extends DrownedBellWarden> type,Level level){super(type,level);xpReward=80;setPersistenceRequired();}
	public static AttributeSupplier.Builder createAttributes(){
		return createMobAttributes().add(Attributes.MAX_HEALTH,BellWardenRules.HEALTH).add(Attributes.ARMOR,BellWardenRules.ARMOUR).add(Attributes.ARMOR_TOUGHNESS,BellWardenRules.TIER.toughness)
			.add(Attributes.MOVEMENT_SPEED,BellWardenRules.SPEED).add(Attributes.ATTACK_DAMAGE,BellWardenRules.TIER.melee).add(Attributes.FOLLOW_RANGE,40).add(Attributes.KNOCKBACK_RESISTANCE,1)
			.add(Attributes.STEP_HEIGHT,1.1).add(Attributes.WATER_MOVEMENT_EFFICIENCY,1);
	}

	/** Bind it to its vigil: {@code at} the air above the floor where it kneels, {@code yaw} the way it faces, {@code hall} the chapel it keeps. */
	public void bind(BlockPos at,float yaw,BoundingBox hall){
		bound=true;vigil=at.immutable();rest=yaw;
		hallMin=new BlockPos(hall.minX(),hall.minY(),hall.minZ());hallMax=new BlockPos(hall.maxX(),hall.maxY(),hall.maxZ());
		kneelDown();
	}
	/** Client side, for blending one move into the next: the move last drawn, the one before it, when it changed, and how far into it the last one was. */
	public int shownMove=-1,shownTell,prevMove,prevTell;public float shownElapsed,prevElapsed,switchedAt=-100;
	public int move(){return entityData.get(MOVE);}
	public int tell(){return entityData.get(TELL);}
	public boolean cracked(){return entityData.get(CRACKED);}
	public float elapsed(float partial){return (float)(level().getGameTime()-entityData.get(BEGAN))+partial;}
	private long since(){return level().getGameTime()-entityData.get(BEGAN);}
	private Vec3 spot(){return Vec3.atBottomCenterOf(vigil);}

	@Override protected void defineSynchedData(SynchedEntityData.Builder b){
		super.defineSynchedData(b);b.define(MOVE,BellWardenRules.DORMANT);b.define(BEGAN,0L);b.define(TELL,0);b.define(CRACKED,false);
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
	/** Back on its knees at its vigil, whole again, its bell mended and the fight forgotten. */
	private void kneelDown(){
		pose(BellWardenRules.DORMANT,0);hold();setHealth(getMaxHealth());setTarget(null);bar.removeAllPlayers();quiet=0;
		entityData.set(CRACKED,false);bar.setColor(BossEvent.BossBarColor.BLUE);forget();
	}
	/** Everything it was in the middle of, dropped. */
	private void forget(){
		deck.clear();Arrays.fill(lastMade,-9999);links=0;again=0;waves.clear();volleys.clear();drop=null;stung=0;struck.clear();
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
		if(!volleys.isEmpty())volleys(level);
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
	private void sound(ServerLevel level,Vec3 at,SoundEvent s,float volume,float pitch){level.playSound(null,at.x,at.y,at.z,s,SoundSource.HOSTILE,volume,pitch);}
	/** A ring of {@code dust} {@code r} out round {@code at}, just off the floor. */
	private void ring(ServerLevel level,Vec3 at,double r,ParticleOptions dust,int n){
		double off=random.nextDouble()*Math.PI*2/n;
		for(int i=0;i<n;i++){double a=off+i*Math.PI*2/n;level.sendParticles(dust,at.x+Math.cos(a)*r,at.y+.15,at.z+Math.sin(a)*r,1,0,0,0,0);}
	}
	/** The floor block under {@code at}, for the dust it kicks up. */
	private BlockState ground(ServerLevel level,Vec3 at){
		var s=level.getBlockState(BlockPos.containing(at.x,at.y-.5,at.z));return s.isAir()?Blocks.DEEPSLATE_TILES.defaultBlockState():s;
	}
	/** The floor under {@code at}, or {@code at} itself when there is none near. */
	private Vec3 floor(ServerLevel level,Vec3 at){
		var hit=level.clip(new ClipContext(at.add(0,3,0),at.add(0,-6,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
		return hit.getType()==HitResult.Type.MISS?at:hit.getLocation();
	}
	/** Floor dust thrown up at {@code at}. */
	private void dust(ServerLevel level,Vec3 at,int n,double spread){
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,at)),at.x,at.y+.1,at.z,n,spread,.1,spread,.15);
	}

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
		deck.clear();links=0;
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
			var at=spot();teleportTo(at.x,at.y,at.z);setDeltaMovement(Vec3.ZERO);getNavigation().stop();drop=null;pose(BellWardenRules.WATCH,BellWardenRules.GAP);
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
			case BellWardenRules.SWING->whirl(level,target);
			case BellWardenRules.HAMMER->hammer(level,target);
			case BellWardenRules.SLAM->slam(level,target);
			case BellWardenRules.CHARGE->charge(level,target);
			case BellWardenRules.TOLL->knell(level,target);
			case BellWardenRules.LASH->lash(level,target);
			case BellWardenRules.COVER->cover(level,target);
			case BellWardenRules.PEAL->peal(level,target);
			case BellWardenRules.UNDERTOW->undertow(level,target);
			case BellWardenRules.CRACK->crack(level,target);
			case BellWardenRules.STUCK->stuck(level);
			case BellWardenRules.DAZED->dazed(level);
			case BellWardenRules.BACKSTEP->{face(aim);move(MoverType.SELF,slide);setDeltaMovement(Vec3.ZERO);if(--left<=0)pose(BellWardenRules.WATCH,2);}
			// It comes up out of its shell striking its bell.
			case BellWardenRules.SHELL->{still();if(--left<=0)begin(level,BellWardenRules.TOLL,target,false);}
			case BellWardenRules.RECOVER->{still();if(--left<=0)pose(BellWardenRules.WATCH,gap());}
			default->watch(level,target);
		}
	}

	/** On its feet and tracking: closing on its prey, turning only so fast so its back can be got at, and picking its next move. */
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
		boolean behind=BellWardenRules.behind(yawTo(target.position())-getYRot());
		if(left>0)left--;
		if(left<=0){
			long now=level.getGameTime();
			// Half beaten for the first time, it cracks its bell before anything else.
			if(BellWardenRules.cracks(getHealth(),getMaxHealth(),cracked())){links=0;begin(level,BellWardenRules.CRACK,target,false);return;}
			long[] since=new long[lastMade.length];for(int i=0;i<since.length;i++)since[i]=now-lastMade[i];
			int pick=BellWardenRules.choose(deck,d,behind,target.getY()-getY(),since,cracked(),random.nextDouble());
			// A long chase ends in a charge from further out.
			if(pick<0 && chased>=60 && d>=5 && d<=BellWardenRules.CHARGE_REACH+4 && deck.last()!=BellWardenRules.CHARGE
				&& since[BellWardenRules.CHARGE]>=BellWardenRules.rest(BellWardenRules.CHARGE,cracked())/2)pick=BellWardenRules.CHARGE;
			if(pick>=0){links=0;begin(level,pick,target,false);return;}
		}
		if(d>BellWardenRules.CLOSE){
			chased++;
			if(!getNavigation().moveTo(target,1))getMoveControl().setWantedPosition(target.getX(),target.getY(),target.getZ(),1);
		}else{chased=0;getNavigation().stop();turn(yawTo(target.position()),10);}
	}

	/** Sets off on {@code move} at {@code target}, {@code chained} when run straight on into from the last one (a shorter tell). */
	private void begin(ServerLevel level,int move,LivingEntity target,boolean chained){
		long now=level.getGameTime();lastMade[move]=now;if(move!=BellWardenRules.CRACK)deck.played(move);
		chased=0;struck.clear();this.chained=chained;again=0;
		if(target!=null)aim=yawTo(target.position());
		var b=bell();
		switch(move){
			case BellWardenRules.SWING->{
				pose(move,0,chained?BellWardenRules.SWING_TELL_CHAINED:BellWardenRules.SWING_TELL);
				sound(level,b,SoundEvents.GRINDSTONE_USE,1.3F,.5F);Feels.sound(level,b,"aura_bell_warden_gather",1.1F,.8F);
			}
			case BellWardenRules.HAMMER->{
				pose(move,0,chained?BellWardenRules.HAMMER_TELL_CHAINED:BellWardenRules.HAMMER_TELL);
				sound(level,b,SoundEvents.CHAIN_PLACE,1.3F,.6F);Feels.sound(level,b,"aura_bell_warden_rise",1.2F,1.3F);
			}
			case BellWardenRules.SLAM->{
				pose(move,0,chained?BellWardenRules.SLAM_TELL_CHAINED:BellWardenRules.SLAM_TELL);
				Feels.sound(level,b,"aura_bell_warden_rise",1,1.5F);sound(level,b,SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE,1.2F,.7F);
			}
			case BellWardenRules.CHARGE->{pose(move,0,chained?BellWardenRules.CHARGE_TELL_CHAINED:BellWardenRules.CHARGE_TELL);Feels.sound(level,b,"aura_bell_warden_charge",1.6F,1);}
			case BellWardenRules.TOLL->{pose(move,0,chained?BellWardenRules.TOLL_TELL_CHAINED:BellWardenRules.TOLL_TELL);Feels.sound(level,b,"aura_bell_warden_gather",1.4F,1);}
			case BellWardenRules.LASH->{pose(move,0,chained?BellWardenRules.LASH_TELL_CHAINED:BellWardenRules.LASH_TELL);sound(level,b,SoundEvents.CHAIN_HIT,1.4F,.7F);}
			case BellWardenRules.COVER->{
				drop=null;pose(move,0,chained?BellWardenRules.COVER_TELL_CHAINED:BellWardenRules.COVER_TELL);
				sound(level,b,SoundEvents.BELL_RESONATE,1.4F,.6F);Feels.sound(level,b,"aura_bell_warden_rise",1.3F,.7F);
			}
			case BellWardenRules.PEAL->{pose(move,0,BellWardenRules.PEAL_TELL);sound(level,b,SoundEvents.BELL_RESONATE,1.8F,.8F);}
			case BellWardenRules.UNDERTOW->{
				pose(move,0,BellWardenRules.UNDERTOW_POUR+BellWardenRules.UNDERTOW_PULL);
				sound(level,b,SoundEvents.BUCKET_EMPTY,1.6F,.5F);Feels.sound(level,b,"aura_bell_warden_breath",1.4F,.7F);
			}
			case BellWardenRules.CRACK->{pose(move,0,BellWardenRules.CRACK_TELL);Feels.sound(level,b,"aura_bell_warden_shell",1.6F,.7F);sound(level,b,SoundEvents.GRINDSTONE_USE,1.6F,.4F);}
			default->pose(BellWardenRules.WATCH,gap());
		}
	}
	/** Done with {@code move}: cracked, perhaps straight on into another; otherwise spent {@code recover} ticks. */
	private void after(ServerLevel level,int move,int recover){
		var target=getTarget();
		if(cracked() && target!=null && links+1<BellWardenRules.CHAIN_MOST){
			int next=BellWardenRules.chain(move,distanceTo(target),random.nextDouble());
			if(next>=0 && next!=move){links++;begin(level,next,target,true);return;}
		}
		spent(links>0?recover+BellWardenRules.CHAIN_REST:recover);links=0;
	}

	/**
	 * Feet planted and twisted far round, the clapper trailing and scraping a ring of sparks on the floor at its reach; then all the way
	 * round in {@link BellWardenRules#SWING_SPIN} ticks, catching each in turn as the clapper comes past. Cracked, a step toward its prey
	 * and round again. Then a dizzy beat.
	 */
	private void whirl(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<tell){
			still();
			if(target!=null && el<=tell-3){turn(yawTo(target.position()),BellWardenRules.TURN);aim=getYRot();}
			face(aim);
			// The second whirl comes stepping in.
			if(again>0){var s=ahead(aim).scale(BellWardenRules.SWING_STEP/BellWardenRules.SWING_RETELL);move(MoverType.SELF,new Vec3(s.x,-.1,s.z));setDeltaMovement(Vec3.ZERO);}
			if(el%3==0)ring(level,position(),BellWardenRules.SWING_REACH,ParticleTypes.SCRAPE,30);
			if(el%4==1){var c=position().add(ahead(aim+150).scale(2.2));level.sendParticles(ParticleTypes.CRIT,c.x,c.y+.2,c.z,3,.2,.05,.2,.1);sound(level,c,SoundEvents.GRINDSTONE_USE,.6F,1.4F);}
			return;
		}
		face(aim);still();
		long k=el-tell;
		if(k==1){Feels.sound(level,bell().add(0,-1.5,0),"aura_bell_warden_swing",1.6F,.85F+random.nextFloat()*.1F);sound(level,position(),SoundEvents.PLAYER_ATTACK_SWEEP,1.4F,.6F);}
		if(k>=1 && k<=BellWardenRules.SWING_SPIN){
			for(int i=0;i<3;i++){
				var c=position().add(ahead(aim+(float)((k-1+i/3.0)*360/BellWardenRules.SWING_SPIN)).scale(3.6));
				level.sendParticles(ParticleTypes.SWEEP_ATTACK,c.x,c.y+1.4,c.z,1,0,0,0,0);level.sendParticles(ParticleTypes.SPLASH,c.x,c.y+.3,c.z,4,.3,.1,.3,.2);
			}
			for(var p:prey(level)){
				if(struck.contains(p.getUUID()))continue;
				double dx=p.getX()-getX(),dz=p.getZ()-getZ();
				if(BellWardenRules.whirls(Math.sqrt(dx*dx+dz*dz),p.getY()-getY()) && BellWardenRules.swept(yawTo(p.position())-aim,k)){
					struck.add(p.getUUID());hit(level,p,BellWardenRules.SWING_DAMAGE,1.2,position(),.25);
				}
			}
			return;
		}
		if(k<=BellWardenRules.SWING_SPIN+3)return;
		if(cracked() && again==0){
			again=1;struck.clear();pose(BellWardenRules.SWING,0,BellWardenRules.SWING_RETELL);
			sound(level,bell(),SoundEvents.GRINDSTONE_USE,1.3F,.65F);return;
		}
		after(level,BellWardenRules.SWING,BellWardenRules.SWING_RECOVER);
	}

	/**
	 * The clapper raised overhead in both hands, back arched, turning after its prey until the aim locks with a glint down the line; then
	 * brought down, and the floor splits on along the line beyond. Cracked, it raises it again and strikes a second time. The last blow
	 * sticks fast in the floor.
	 */
	private void hammer(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell-BellWardenRules.HAMMER_LOCK && target!=null){turn(yawTo(target.position()),BellWardenRules.TURN);aim=getYRot();}
		face(aim);still();
		var f=ahead(aim);
		if(el<tell){
			if(el%3==0){var c=position().add(f.scale(-.6)).add(0,6.2,0);level.sendParticles(ParticleTypes.DRIPPING_WATER,c.x,c.y,c.z,3,.3,.1,.3,0);}
			if(el==tell-BellWardenRules.HAMMER_LOCK)sound(level,bell(),SoundEvents.CHAIN_HIT,1.2F,1.4F);
			if(el>tell-BellWardenRules.HAMMER_LOCK)for(double s=1;s<=BellWardenRules.SPLIT_REACH;s+=1){var c=position().add(f.scale(s));level.sendParticles(ParticleTypes.BUBBLE_POP,c.x,c.y+.15,c.z,1,.05,0,.05,0);}
			return;
		}
		long k=el-tell;
		if(k==0){
			var at=position().add(f.scale(3.4));
			Feels.sound(level,at,"aura_bell_warden_hammer",1.6F,.9F+random.nextFloat()*.2F);sound(level,at,SoundEvents.ANVIL_LAND,1.2F,.5F);
			level.sendParticles(ParticleTypes.SPLASH,at.x,at.y+.1,at.z,24,.5,.1,.5,.25);level.sendParticles(ParticleTypes.CRIT,at.x,at.y+.2,at.z,10,.4,.1,.4,.2);dust(level,at,20,.5);
			for(var p:prey(level))if(reaches(BellWardenRules.HAMMER,p)){struck.add(p.getUUID());hit(level,p,BellWardenRules.HAMMER_DAMAGE,.4,position(),0);}
			return;
		}
		if(k<=BellWardenRules.SPLIT_TICKS){
			double from=BellWardenRules.split(k-1),to=BellWardenRules.split(k);
			for(double s=from;s<=to;s+=.5){var c=floor(level,position().add(f.scale(s)));level.sendParticles(new BlockParticleOption(ParticleTypes.DUST_PILLAR,ground(level,c)),c.x,c.y+.1,c.z,6,.2,.1,.2,.2);}
			sound(level,position().add(f.scale(to)),SoundEvents.DEEPSLATE_BREAK,1.2F,.6F);
			for(var p:prey(level)){
				if(struck.contains(p.getUUID()))continue;
				double dx=p.getX()-getX(),dz=p.getZ()-getZ();
				if(BellWardenRules.splits(dx*f.x+dz*f.z,dx*-f.z+dz*f.x,p.getY()-getY(),from,to)){struck.add(p.getUUID());hit(level,p,BellWardenRules.SPLIT_DAMAGE,.3,position(),.5);}
			}
			return;
		}
		if(k<BellWardenRules.SPLIT_TICKS+2)return;
		if(cracked() && again==0){
			again=1;struck.clear();pose(BellWardenRules.HAMMER,0,BellWardenRules.HAMMER_RETELL);
			sound(level,bell(),SoundEvents.CHAIN_PLACE,1.3F,.75F);Feels.sound(level,bell(),"aura_bell_warden_rise",1.1F,1.45F);return;
		}
		links=0;pose(BellWardenRules.STUCK,BellWardenRules.STUCK_TICKS);still();
		sound(level,position().add(f.scale(3.4)),SoundEvents.ZOMBIE_ATTACK_IRON_DOOR,1F,.5F);
	}
	/** Its clapper stuck fast in the floor, hauling on it; then torn loose. */
	private void stuck(ServerLevel level){
		still();face(aim);
		var at=position().add(ahead(aim).scale(3.4));
		if(left%8==0){sound(level,at,SoundEvents.GRINDSTONE_USE,.9F,.5F+random.nextFloat()*.1F);sound(level,at,SoundEvents.CHAIN_HIT,.8F,.6F);}
		if(left%3==0)level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK_CRUMBLE,ground(level,at)),at.x,at.y+.1,at.z,4,.3,.1,.3,.1);
		if(--left<=0){sound(level,at,SoundEvents.STONE_BREAK,1.4F,.6F);dust(level,at,24,.5);pose(BellWardenRules.WATCH,gap());}
	}

	/**
	 * Both fists raised while it tracks its prey; then down on the floor ahead of it, and marks bubble up under its prey and round
	 * about: each spouts {@link BellWardenRules#SPOUT_DELAY} ticks later. Cracked, a second volley comes up where its prey then stands.
	 */
	private void slam(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell-3 && target!=null){turn(yawTo(target.position()),BellWardenRules.TURN);aim=getYRot();}
		face(aim);still();
		if(el<tell){
			if(el%3==0){var c=position().add(0,5.6,0);level.sendParticles(ParticleTypes.FALLING_WATER,c.x,c.y,c.z,6,1.2,.2,1.2,0);}
			return;
		}
		if(el==tell){
			var at=position().add(ahead(aim).scale(2.2));
			Feels.sound(level,at,"aura_bell_warden_slam",1.6F,.95F+random.nextFloat()*.1F);sound(level,at,SoundEvents.MACE_SMASH_GROUND,1.2F,.8F);
			ring(level,at,1.8,ParticleTypes.SPLASH,20);level.sendParticles(ParticleTypes.CLOUD,at.x,at.y+.2,at.z,8,.8,.1,.8,.05);dust(level,at,16,.8);
			for(var p:prey(level))if(reaches(BellWardenRules.SLAM,p))hit(level,p,BellWardenRules.FIST_DAMAGE,.8,at,.3);
			volley(level,false);
		}
		if(el>=tell+BellWardenRules.SLAM_HOLD)after(level,BellWardenRules.SLAM,BellWardenRules.SLAM_RECOVER);
	}
	/** Marks on the floor under each of its prey, and (the first time) more scattered round them. */
	private void volley(ServerLevel level,boolean second){
		var at=new ArrayList<Vec3>();
		for(var p:prey(level)){
			at.add(floor(level,p.position()));
			if(!second)for(int i=0;i<BellWardenRules.SPOUT_SCATTER;i++){
				double a=(i+random.nextDouble()*.6)*Math.PI*2/BellWardenRules.SPOUT_SCATTER,r=2+random.nextDouble()*(BellWardenRules.SPOUT_SPREAD-2);
				var c=p.position().add(Math.cos(a)*r,0,Math.sin(a)*r);if(inHall(c,0))at.add(floor(level,c));
			}
		}
		if(at.isEmpty())return;
		volleys.add(new Volley(at,level.getGameTime()+BellWardenRules.SPOUT_DELAY,second));
		sound(level,at.get(0),SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE,1.4F,1.2F);
	}
	/** The marked floor bubbling, then spouting, throwing whoever stands on a mark up with it. */
	private void volleys(ServerLevel level){
		long now=level.getGameTime();var prey=prey(level);boolean more=false;
		for(var it=volleys.iterator();it.hasNext();){
			var v=it.next();
			if(!isAlive()){it.remove();continue;}
			if(now<v.bursts){
				if((v.bursts-now)%2==0)for(var at:v.at){ring(level,at,BellWardenRules.SPOUT_RADIUS,ParticleTypes.BUBBLE_POP,10);level.sendParticles(ParticleTypes.FISHING,at.x,at.y+.1,at.z,2,.4,0,.4,0);}
				if(v.bursts-now==5)sound(level,v.at.get(0),SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE,1.4F,1.4F);
				continue;
			}
			int n=0;
			for(var at:v.at){
				level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,at.x,at.y+.5,at.z,30,.4,1.4,.4,.3);level.sendParticles(ParticleTypes.SPLASH,at.x,at.y+3,at.z,30,.6,.6,.6,.4);
				if(n++<3)sound(level,at,SoundEvents.PLAYER_SPLASH_HIGH_SPEED,1.2F,.8F+random.nextFloat()*.3F);
				for(var p:prey)if(!v.struck.contains(p.getUUID()) && BellWardenRules.spouts(p.getX()-at.x,p.getZ()-at.z,p.getY()-at.y)){
					v.struck.add(p.getUUID());hit(level,p,BellWardenRules.SPOUT_DAMAGE,0,at,BellWardenRules.SPOUT_LIFT);
				}
			}
			if(!v.second && cracked())more=true;
			it.remove();
		}
		if(more)volley(level,true);
	}

	/**
	 * Bell lowered, pawing the floor while it tracks its prey, the aim locked for the last few ticks with its bell ringing low; then its
	 * whole weight run along it, running down whoever is in the way. A wall or pillar stops it short, ringing its bell and leaving it
	 * dazed a long while. Out in open floor it stumbles to a stop, and cracked it turns and goes again.
	 */
	private void charge(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell-BellWardenRules.CHARGE_LOCK && target!=null){turn(yawTo(target.position()),BellWardenRules.TURN);aim=getYRot();}
		face(aim);
		if(el<=tell){
			still();
			if(el%4==0){sound(level,position(),SoundEvents.RAVAGER_STEP,1.2F,.7F);dust(level,position().add(ahead(aim).scale(-.8)),8,.4);}
			if(el==tell-BellWardenRules.CHARGE_LOCK)sound(level,bell(),SoundEvents.BELL_BLOCK,1.4F,.5F);
			return;
		}
		var step=ahead(aim).scale(BellWardenRules.CHARGE_REACH/BellWardenRules.CHARGE_TICKS);
		move(MoverType.SELF,new Vec3(step.x,-.2,step.z));setDeltaMovement(Vec3.ZERO);
		if(el%2==0){Feels.sound(level,position(),"aura_bell_warden_step",1.3F,1.1F);level.sendParticles(ParticleTypes.SPLASH,getX(),getY()+.1,getZ(),10,.6,0,.6,.2);}
		for(var p:prey(level))if(!struck.contains(p.getUUID()) && reaches(BellWardenRules.CHARGE,p)){struck.add(p.getUUID());hit(level,p,BellWardenRules.CHARGE_DAMAGE,1.5,position().subtract(ahead(aim)),.35);}
		if(horizontalCollision){
			toll(level,1.8F,1.1F);sound(level,bell(),SoundEvents.ANVIL_LAND,1.4F,.6F);
			level.sendParticles(ParticleTypes.CRIT,bell().x,bell().y,bell().z,16,.8,.6,.8,.3);dust(level,position().add(ahead(aim).scale(1.5)),20,.6);
			links=0;pose(BellWardenRules.DAZED,BellWardenRules.DAZED_TICKS);still();return;
		}
		if(el>=tell+BellWardenRules.CHARGE_TICKS || !inHall(position(),0)){
			sound(level,position(),SoundEvents.RAVAGER_STEP,1.4F,.6F);dust(level,position(),12,.8);
			if(cracked() && again==0 && target!=null){again=1;struck.clear();pose(BellWardenRules.CHARGE,0,BellWardenRules.CHARGE_RETELL);Feels.sound(level,bell(),"aura_bell_warden_charge",1.6F,1.15F);return;}
			after(level,BellWardenRules.CHARGE,BellWardenRules.CHARGE_RECOVER);
		}
	}
	/** Its bell rung on stone, swaying where it stands. */
	private void dazed(ServerLevel level){
		still();
		var b=bell();
		if(left%15==0)toll(level,.7F,1.3F);
		if(left%4==0)for(int i=0;i<3;i++){double a=(tickCount*.25)+i*Math.PI*2/3;level.sendParticles(ParticleTypes.ELECTRIC_SPARK,b.x+Math.cos(a)*1.2,b.y+1.3,b.z+Math.sin(a)*1.2,1,0,0,0,0);}
		if(--left<=0)pose(BellWardenRules.WATCH,gap());
	}

	/** Clapper raised to its own bell while it turns after its prey; then struck, and a ring rolls out from its feet. Cracked, a second, faster. */
	private void knell(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<tell && target!=null){turn(yawTo(target.position()),BellWardenRules.TURN);aim=getYRot();}
		face(aim);still();
		if(el<tell){if(el%3==0){var b=bell();level.sendParticles(ParticleTypes.GLOW,b.x,b.y+1,b.z,3,.6,.4,.6,.02);}return;}
		if(el==tell)strike(level,BellWardenRules.WAVE_SPEED,.95F);
		if(cracked() && el==tell+BellWardenRules.TOLL_AGAIN)strike(level,BellWardenRules.WAVE_FAST,1.15F);
		if(el>=tell+(cracked()?BellWardenRules.TOLL_AGAIN+3:3))after(level,BellWardenRules.TOLL,BellWardenRules.TOLL_RECOVER);
	}
	private void strike(ServerLevel level,double speed,float pitch){
		var b=bell();
		Feels.sound(level,b,"aura_bell_warden_knell",2.2F,pitch+random.nextFloat()*.1F);
		for(int i=0;i<24;i++){double a=i*Math.PI*2/24;level.sendParticles(ParticleTypes.GLOW,b.x+Math.cos(a)*1.6,b.y-.6,b.z+Math.sin(a)*1.6,1,0,0,0,0);}
		level.sendParticles(ParticleTypes.SPLASH,getX(),getY()+.1,getZ(),40,1.4,.1,1.4,.4);
		waves.add(new Wave(position(),level.getGameTime(),speed,BellWardenRules.WAVE_REACH,BellWardenRules.TOLL_DAMAGE,.4));
	}
	/** Its rings rolling out across the floor, each catching whoever has not jumped it as it passes. */
	private void waves(ServerLevel level){
		long now=level.getGameTime();var prey=prey(level);
		for(var it=waves.iterator();it.hasNext();){
			var w=it.next();long age=now-w.born;double r=age*w.speed,was=Math.max(0,(age-1)*w.speed);
			if(r>w.max || !isAlive()){it.remove();continue;}
			if(age<1)continue;
			int n=Math.min(64,Math.max(10,(int)(r*5)));double off=random.nextDouble()*Math.PI*2/n;
			for(int i=0;i<n;i++){double a=off+i*Math.PI*2/n;level.sendParticles(i%3==0?ParticleTypes.GLOW:ParticleTypes.SPLASH,w.at.x+Math.cos(a)*r,w.at.y+.15,w.at.z+Math.sin(a)*r,1,0,.05,0,0);}
			for(var p:prey){
				if(w.struck.contains(p.getUUID()))continue;
				double dx=p.getX()-w.at.x,dz=p.getZ()-w.at.z;
				if(BellWardenRules.crosses(Math.sqrt(dx*dx+dz*dz),was,r,p.getY()-w.at.y)){w.struck.add(p.getUUID());hit(level,p,w.damage,1.0,w.at,w.lift);}
			}
		}
	}

	/**
	 * The chain on its forearm whirled overhead, faster and faster, while it turns after its prey; the aim locks with the chain's rattle
	 * and a line of bubbles along it; then lashed out along the line, dragging whoever it catches back to its feet. Cracked, it lashes
	 * twice.
	 */
	private void lash(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell-BellWardenRules.LASH_LOCK && target!=null){turn(yawTo(target.position()),BellWardenRules.TURN);aim=getYRot();}
		face(aim);still();
		var f=ahead(aim);
		if(el<tell){
			double a=el*(.5+el*.04);var c=position().add(Math.cos(a)*1.8,5.6,Math.sin(a)*1.8);level.sendParticles(ParticleTypes.CRIT,c.x,c.y,c.z,2,.05,.05,.05,0);
			if(el%4==0)sound(level,c,SoundEvents.CHAIN_HIT,.8F,.8F+el*.03F);
			if(el>tell-BellWardenRules.LASH_LOCK)for(double s=BellWardenRules.LASH_NEAR;s<=BellWardenRules.LASH_REACH;s+=1){var d=position().add(f.scale(s));level.sendParticles(ParticleTypes.BUBBLE_POP,d.x,d.y+.15,d.z,1,.05,0,.05,0);}
			return;
		}
		if(el==tell){
			Feels.sound(level,bell(),"aura_bell_warden_swing",1.4F,1.3F);sound(level,position(),SoundEvents.CHAIN_BREAK,1.4F,.6F);
			for(double s=1;s<=BellWardenRules.LASH_REACH;s+=.5){var c=position().add(f.scale(s));level.sendParticles(ParticleTypes.CRIT,c.x,c.y+1.2,c.z,1,.05,.05,.05,0);if(s%2==0)level.sendParticles(ParticleTypes.SPLASH,c.x,c.y+.2,c.z,3,.2,0,.2,.1);}
			for(var p:prey(level))if(reaches(BellWardenRules.LASH,p) && hit(level,p,BellWardenRules.LASH_DAMAGE,0,position(),0)){
				double t=BellWardenRules.tug(distanceTo(p));var n=new Vec3(getX()-p.getX(),0,getZ()-p.getZ()).normalize().scale(t);
				p.setDeltaMovement(p.getKnownMovement().add(n.x,.3,n.z));p.needsSync=true;
				sound(level,p.position(),SoundEvents.CHAIN_HIT,1.2F,.5F);
			}
		}
		if(el<tell+BellWardenRules.LASH_HOLD)return;
		if(cracked() && again==0){again=1;pose(BellWardenRules.LASH,0,BellWardenRules.LASH_RETELL);sound(level,bell(),SoundEvents.CHAIN_HIT,1.4F,.85F);return;}
		after(level,BellWardenRules.LASH,BellWardenRules.LASH_RECOVER);
	}

	/**
	 * Gripping its bell's rim, it lifts it off its own head while it tracks its prey, and a circle bubbles on the floor where it means to
	 * bring it down, glowing once the aim locks; then down mouth first over the circle. It hauls it back up with its head bare a while,
	 * taking blows from anywhere. Cracked, the landing sends a ring out from the bell.
	 */
	private void cover(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell-BellWardenRules.COVER_LOCK && target!=null){
			turn(yawTo(target.position()),BellWardenRules.TURN);aim=getYRot();
			drop=floor(level,position().add(ahead(aim).scale(BellWardenRules.drop(Math.sqrt(distanceToSqr(target.getX(),getY(),target.getZ()))))));
		}
		if(drop==null)drop=floor(level,position().add(ahead(aim).scale(BellWardenRules.COVER_NEAR)));
		face(aim);still();
		if(el<tell){
			boolean locked=el>tell-BellWardenRules.COVER_LOCK;
			if(locked || el%2==0)ring(level,drop,BellWardenRules.COVER_RADIUS,locked?ParticleTypes.GLOW:ParticleTypes.BUBBLE_POP,26);
			if(el%4==0){var b=bell().add(0,2,0);level.sendParticles(ParticleTypes.FALLING_WATER,b.x,b.y,b.z,6,1,.3,1,0);}
			if(el==tell-BellWardenRules.COVER_LOCK)sound(level,drop,SoundEvents.BELL_RESONATE,1.4F,1.2F);
			return;
		}
		if(el==tell){
			sound(level,drop,SoundEvents.ANVIL_LAND,1.6F,.5F);sound(level,drop,SoundEvents.BELL_BLOCK,2F,.6F);Feels.sound(level,drop,"aura_bell_warden_slam",1.6F,.8F);
			ring(level,drop,BellWardenRules.COVER_RADIUS,ParticleTypes.SPLASH,30);level.sendParticles(ParticleTypes.CLOUD,drop.x,drop.y+.3,drop.z,12,1.2,.2,1.2,.05);dust(level,drop,24,1.2);
			for(var p:prey(level)){double dx=p.getX()-drop.x,dz=p.getZ()-drop.z;if(BellWardenRules.covers(dx,dz,p.getY()-drop.y))hit(level,p,BellWardenRules.COVER_DAMAGE,1.0,drop,.3);}
			if(cracked())waves.add(new Wave(drop,level.getGameTime(),BellWardenRules.WAVE_SPEED,BellWardenRules.WAVE_REACH,BellWardenRules.COVER_RING,.3));
			return;
		}
		if((el-tell)%10==0){sound(level,drop,SoundEvents.GRINDSTONE_USE,.9F,.5F);level.sendParticles(ParticleTypes.BUBBLE_POP,drop.x,drop.y+.3,drop.z,8,1,.2,1,.02);}
		if(el>=tell+BellWardenRules.COVER_HOLD){
			sound(level,bell(),SoundEvents.BELL_RESONATE,1.2F,.7F);drop=null;
			after(level,BellWardenRules.COVER,BellWardenRules.COVER_RECOVER);
		}
	}

	/**
	 * Hands on its bell's rim, it makes it hum louder and louder; then the sound goes out in a cone before it, pulsing, while it turns
	 * slowly after its prey. Each pulse stings and slows whoever it reaches in the open. A pillar stops it; its back is bare.
	 */
	private void peal(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();boolean c=cracked();
		var b=bell();
		if(el<tell){
			if(target!=null)turn(yawTo(target.position()),BellWardenRules.TURN);aim=getYRot();face(aim);still();
			if(el%3==0)level.sendParticles(ParticleTypes.NOTE,b.x,b.y+1.2,b.z,2,.8,.3,.8,1);
			if(el==tell-6)sound(level,b,SoundEvents.BELL_RESONATE,2F,1.2F);
			return;
		}
		long k=el-tell;
		if(target!=null)turn(yawTo(target.position()),(float)(c?BellWardenRules.PEAL_TURN_CRACKED:BellWardenRules.PEAL_TURN));
		aim=getYRot();face(aim);still();
		double half=c?BellWardenRules.PEAL_ANGLE_CRACKED:BellWardenRules.PEAL_ANGLE;
		if(k%2==0)for(int i=0;i<6;i++){
			float a=aim+(float)((random.nextDouble()*2-1)*half);double r=1.5+random.nextDouble()*(BellWardenRules.PEAL_REACH-1.5);var at=b.add(ahead(a).scale(r));
			level.sendParticles(ParticleTypes.NOTE,at.x,at.y-1.5+random.nextDouble()*2,at.z,1,0,0,0,random.nextDouble());
		}
		if(k%BellWardenRules.PEAL_EVERY==0){
			Feels.sound(level,b,"aura_bell_warden_ring",1.6F,.8F);sound(level,b,SoundEvents.AMETHYST_BLOCK_RESONATE,1.4F,.6F);
			for(int i=1;i<=3;i++){var at=b.add(ahead(aim).scale(i*3)).add(0,-1.5,0);level.sendParticles(ParticleTypes.SONIC_BOOM,at.x,at.y,at.z,1,0,0,0,0);}
			var f=ahead(aim);
			for(var p:prey(level)){
				double dx=p.getX()-getX(),dz=p.getZ()-getZ();
				if(!BellWardenRules.peals(dx*f.x+dz*f.z,dx*-f.z+dz*f.x,p.getY()-getY(),c))continue;
				// A pillar or a wall between them takes it.
				if(level.clip(new ClipContext(b,p.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()!=HitResult.Type.MISS)continue;
				if(hit(level,p,BellWardenRules.PEAL_DAMAGE,.3,position(),0))p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,BellWardenRules.PEAL_SLOW,1,false,true),this);
			}
		}
		if(k>=BellWardenRules.PEAL_TICKS)after(level,BellWardenRules.PEAL,BellWardenRules.PEAL_RECOVER);
	}

	/**
	 * Arms spread and head back, water pouring from its bell's lip; then the whole floor drags in toward it in shrinking rings while it
	 * gathers its fists overhead, and down they come on whoever it drew in. It is a long while getting its fists back out of the floor.
	 */
	private void undertow(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();boolean c=cracked();
		if(target!=null && el<tell)turn(yawTo(target.position()),10);
		aim=getYRot();face(aim);still();
		if(el<tell){
			var b=bell();
			for(int i=0;i<4;i++){double a=random.nextDouble()*Math.PI*2;level.sendParticles(ParticleTypes.FALLING_WATER,b.x+Math.cos(a)*1.4,b.y-1.2,b.z+Math.sin(a)*1.4,1,0,0,0,0);}
			if(el%3==0)level.sendParticles(ParticleTypes.SPLASH,getX(),getY()+.1,getZ(),8,1.2,0,1.2,.2);
			if(el<BellWardenRules.UNDERTOW_POUR)return;
			if((el-BellWardenRules.UNDERTOW_POUR)%10==0)sound(level,position(),SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE,2F,.6F);
			if(el%2==0){
				double r=BellWardenRules.UNDERTOW_RADIUS*(1-((el-BellWardenRules.UNDERTOW_POUR)%10)/10.0);
				ring(level,position(),r,ParticleTypes.SPLASH,(int)(r*4)+8);ring(level,position(),r*.8,ParticleTypes.FISHING,(int)(r*2)+4);
			}
			for(var p:prey(level)){
				double dx=getX()-p.getX(),dz=getZ()-p.getZ(),d=Math.sqrt(dx*dx+dz*dz),g=BellWardenRules.drag(d,c);
				if(g<=0)continue;
				p.setDeltaMovement(p.getKnownMovement().add(dx/d*g,0,dz/d*g));p.needsSync=true;
			}
			return;
		}
		if(el==tell){
			double r=c?BellWardenRules.CRUSH_RADIUS_CRACKED:BellWardenRules.CRUSH_RADIUS;
			sound(level,position(),SoundEvents.MACE_SMASH_GROUND_HEAVY,2F,.7F);Feels.sound(level,position(),"aura_bell_warden_slam",1.8F,.7F);
			ring(level,position(),r,ParticleTypes.SPLASH,36);ring(level,position(),r*.6,ParticleTypes.CLOUD,18);dust(level,position(),30,r*.5);
			for(var p:prey(level)){double dx=p.getX()-getX(),dz=p.getZ()-getZ();if(BellWardenRules.crushes(Math.sqrt(dx*dx+dz*dz),p.getY()-getY(),c))hit(level,p,BellWardenRules.CRUSH_DAMAGE,1.4,position(),.5);}
		}
		if(el>=tell+2)after(level,BellWardenRules.UNDERTOW,BellWardenRules.UNDERTOW_RECOVER);
	}

	/**
	 * Half beaten: it grips its bell and wrenches at it, light leaking from a widening crack and a glowing ring marking how far the
	 * burst will reach, roaring half way; then the bell tears open in a blast, and a great ring rolls out across the hall. From now on
	 * it fights cracked.
	 */
	private void crack(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		still();if(target!=null)turn(yawTo(target.position()),4);aim=getYRot();
		var b=bell();
		if(el<tell){
			if(el%4==0){ring(level,position(),BellWardenRules.CRACK_RADIUS,ParticleTypes.GLOW,40);sound(level,b,SoundEvents.GRINDSTONE_USE,1.4F,.4F+el*.01F);}
			if(el%6==0)sound(level,b,SoundEvents.BELL_RESONATE,1.6F,.5F+el*.02F);
			if(el==tell/2)sound(level,b,SoundEvents.WARDEN_ROAR,1.6F,.7F);
			level.sendParticles(ParticleTypes.END_ROD,b.x,b.y,b.z,2,.8,.6,.8,.03+el*.002);
			return;
		}
		if(el==tell){
			entityData.set(CRACKED,true);bar.setColor(BossEvent.BossBarColor.PURPLE);
			Feels.sound(level,b,"aura_bell_warden_knell",2.6F,.6F);sound(level,b,SoundEvents.GENERIC_EXPLODE.value(),1.6F,.7F);sound(level,b,SoundEvents.GLASS_BREAK,1.6F,.5F);
			level.sendParticles(ParticleTypes.EXPLOSION,b.x,b.y,b.z,4,1,1,1,0);level.sendParticles(ParticleTypes.GLOW,b.x,b.y,b.z,60,1.5,1,1.5,.4);
			ring(level,position(),BellWardenRules.CRACK_RADIUS*.6,ParticleTypes.SPLASH,30);dust(level,position(),30,2);
			for(var p:prey(level)){double dx=p.getX()-getX(),dz=p.getZ()-getZ();if(BellWardenRules.blasts(Math.sqrt(dx*dx+dz*dz),p.getY()-getY()))hit(level,p,BellWardenRules.CRACK_DAMAGE,2,position(),.6);}
			waves.add(new Wave(position(),level.getGameTime(),BellWardenRules.CRACK_WAVE_SPEED,BellWardenRules.CRACK_WAVE_REACH,BellWardenRules.CRACK_WAVE_DAMAGE,.5));
		}
		if(el>=tell+4){links=0;spent(BellWardenRules.CRACK_RECOVER);}
	}

	/** Spent after an attack, its bell tipped back, for {@code ticks}. */
	private void spent(int ticks){pose(BellWardenRules.RECOVER,ticks);still();}
	/** Hurt too fast: down under its bell for a while. */
	private void shellDown(ServerLevel level){
		lastShell=level.getGameTime();stung=0;links=0;pose(BellWardenRules.SHELL,BellWardenRules.SHELL_TICKS);still();
		Feels.sound(level,bell(),"aura_bell_warden_shell",1.4F,1);
		level.sendParticles(ParticleTypes.SPLASH,getX(),getY()+.1,getZ(),30,1.2,.1,1.2,.3);
	}
	/** Standing its ground: no sliding about, though it still falls. */
	private void still(){getNavigation().stop();var v=getDeltaMovement();setDeltaMovement(0,Math.min(0,v.y),0);}
	/** Ticks before its next move, fewer once cracked. */
	private int gap(){return (cracked()?BellWardenRules.GAP_CRACKED:BellWardenRules.GAP)+random.nextInt(3);}

	/** Players it could be fighting right now. */
	private List<ServerPlayer> prey(ServerLevel level){
		var out=new ArrayList<ServerPlayer>();
		for(var p:level.players())if(p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(this)<24*24 && inHall(p.position(),BellWardenRules.LEASH))out.add(p);
		return out;
	}
	/** Whether {@code move}, along the locked aim, reaches {@code p}. */
	private boolean reaches(int move,LivingEntity p){
		var f=ahead(aim);double dx=p.getX()-getX(),dz=p.getZ()-getZ();
		return BellWardenRules.hits(move,dx*f.x+dz*f.z,dx*-f.z+dz*f.x,p.getY()-getY());
	}
	/** A blow landing on {@code p}, through part of its armour, knocking it {@code shove} away from {@code from} and {@code lift} up; whether it landed. */
	private boolean hit(ServerLevel level,LivingEntity p,double damage,double shove,Vec3 from,double lift){
		if(!GuardianBite.land(level,this,p,(float)damage,BellWardenRules.PIERCE))return false;
		if(shove>0)p.knockback(shove,from.x-p.getX(),from.z-p.getZ(),level.damageSources().mobAttack(this),0);
		if(lift>0)p.setDeltaMovement(p.getDeltaMovement().add(0,lift,0));
		p.needsSync=true;return true;
	}
	private Vec3 ahead(float yaw){double r=Math.toRadians(yaw);return new Vec3(-Math.sin(r),0,Math.cos(r));}

	private float yawTo(Vec3 at){return (float)Math.toDegrees(Math.atan2(-(at.x-getX()),at.z-getZ()));}
	private void face(float yaw){setYRot(yaw);yBodyRot=yaw;yHeadRot=yaw;}
	/** Turning toward {@code yaw}, no more than {@code most} degrees this tick. */
	private void turn(float yaw,float most){face(getYRot()+Mth.clamp(Mth.wrapDegrees(yaw-getYRot()),-most,most));}
	/** Whether {@code move} has it bare to blows from anywhere just now: its bell down over the floor, its head out of it. */
	private boolean open(int move){return move==BellWardenRules.COVER && since()>tell();}

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
			damage*=(float)BellWardenRules.armour(move,frontal,open(move));
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
		out.putBoolean("warden_cracked",cracked());
	}
	@Override protected void readAdditionalSaveData(ValueInput in){
		super.readAdditionalSaveData(in);
		bound=in.getBooleanOr("warden_bound",false);
		vigil=in.read("warden_vigil",BlockPos.CODEC).orElse(blockPosition());rest=in.getFloatOr("warden_rest",getYRot());
		hallMin=in.read("warden_hall_min",BlockPos.CODEC).orElse(vigil);hallMax=in.read("warden_hall_max",BlockPos.CODEC).orElse(vigil);
		lastTick=in.getLongOr("warden_ticked",-1L);
		// Mid-fight it comes back on its feet, whatever it was doing when the chunk unloaded, its bell as cracked as it left it.
		if(in.getBooleanOr("warden_awake",false)){
			pose(BellWardenRules.WATCH,BellWardenRules.GAP);lastCut=0;
			boolean c=in.getBooleanOr("warden_cracked",false);entityData.set(CRACKED,c);if(c)bar.setColor(BossEvent.BossBarColor.PURPLE);
		}else pose(BellWardenRules.DORMANT,0);
	}
}
