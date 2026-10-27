package dev.wildercord.aura.world;

import com.mojang.math.Transformation;
import dev.wildercord.cast.BlockFx;
import dev.wildercord.cast.MoveDeck;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.feel.Feels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import java.util.*;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The Rootcoil Serpent ({@link SerpentRules}). Generation leaves it coiled round the grove's tree, bound to the trunk and facing
 * the herb. While it sleeps it holds still at the trunk's foot and cannot be hurt; the first player onto the island (or the first
 * blow) starts the uncoil, and when it lands the boss bar comes up. The grove going quiet sends it back up the trunk, healed.
 *
 * <p>Awake it never makes the same move twice running, and picks each from where its prey stands. Close in front it bites
 * straight out, grabs with its coils, or spreads its hood and sweeps a jet of sticky sap across; a little off it throws its whole
 * length in a lunge, slams its tail down to fling a fan of the island, or drives its head into the earth to send a line of roots
 * bursting out along the ground; whoever gets round behind it gets its tail swept round in one full turn at ankle height; and
 * whoever hangs about near it gets ringed, run round once and crushed as the ring draws shut. Each has its own wind-up, and each
 * is set a moment before it comes. A lunge that finds nobody leaves it sprawled, and driving its roots leaves its head stuck fast;
 * its scales turn much of a blow from the front, but spent, sprawled, stuck or reared to spray its belly is bare. It slips aside
 * from blows it sees coming. At half health it rouses: towering, earth shaking off it, it throws back everyone near and roots
 * ripple out across the island to be jumped, then it takes to the water ring and bursts out of it three times before it comes
 * back ashore, quicker, its moves running on in pairs, its roots driven three at a time and its spray swung wider.
 */
public final class RootcoilSerpent extends PathfinderMob implements Enemy {
	private static final EntityDataAccessor<Integer> MOVE=SynchedEntityData.defineId(RootcoilSerpent.class,EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN=SynchedEntityData.defineId(RootcoilSerpent.class,EntityDataSerializers.LONG);
	private static final EntityDataAccessor<BlockPos> TREE=SynchedEntityData.defineId(RootcoilSerpent.class,EntityDataSerializers.BLOCK_POS);
	private static final EntityDataAccessor<BlockPos> LANDING=SynchedEntityData.defineId(RootcoilSerpent.class,EntityDataSerializers.BLOCK_POS);
	private static final EntityDataAccessor<Float> REST=SynchedEntityData.defineId(RootcoilSerpent.class,EntityDataSerializers.FLOAT);
	/** The current move's tell in ticks, so the client knows when the blow lands. */
	private static final EntityDataAccessor<Integer> TELL=SynchedEntityData.defineId(RootcoilSerpent.class,EntityDataSerializers.INT);
	/** Which way the current blow swings: 1 or -1. */
	private static final EntityDataAccessor<Integer> SIDE=SynchedEntityData.defineId(RootcoilSerpent.class,EntityDataSerializers.INT);
	/** Coiled round the trunk it spans the coils; on the ground, a reared head over a heap of body. */
	private static final EntityDimensions COILED=EntityDimensions.scalable(9.6F,8F),REARED=EntityDimensions.scalable(3.4F,4.8F).withEyeHeight(4.3F);
	private final ServerBossEvent bar=new ServerBossEvent(getUUID(),getType().getDescription(),BossEvent.BossBarColor.GREEN,BossEvent.BossBarOverlay.NOTCHED_10);
	private boolean bound;
	private int left,quiet;
	private long lastCut;
	/** The game time it last ran, so it knows how long the grove was left alone (unloaded, or nobody near enough to tick it). */
	private long lastTick=-1;
	// The fight, server side only.
	/** The yaw its current blow is locked along. */
	private float aim;
	private int chased,struggles,bursts,circle,links;
	private long lastSlip=-999;
	/** When it last made each move, by move, and the moves it has made lately. */
	private final long[] lastMade=new long[32];
	private final MoveDeck deck=new MoveDeck(5);
	private boolean dived,water,gripping,landed;
	/** The middle of the ring it is drawing round its prey, how wide the ring is, and where round it it started. */
	private Vec3 middle=Vec3.ZERO;
	private double ringR,ringStart;
	/** How many times its spray has splashed each player this spray. */
	private final Map<UUID,Integer> splashed=new HashMap<>();
	/** Lines of roots running out from where its head went in. */
	private final List<Rootline> roots=new ArrayList<>();
	private static final class Rootline{
		final Vec3 from;final float yaw;final Set<UUID> struck;int age;
		Rootline(Vec3 from,float yaw,Set<UUID> struck){this.from=from;this.yaw=yaw;this.struck=struck;}
	}
	/** Roots standing up out of the ground, to be put away if it goes. */
	private final List<Display.BlockDisplay> spikes=new ArrayList<>();
	/** The ring of roots running out from it as it rouses, and who it has caught. */
	private Vec3 rippleAt=Vec3.ZERO;
	private int rippleAge=-1;
	private final Set<UUID> rippled=new HashSet<>();
	private double heldDamage,ring;
	private ServerPlayer caught;
	private Vec3 from=Vec3.ZERO,to=Vec3.ZERO,slide=Vec3.ZERO;
	private final Set<UUID> struck=new HashSet<>();
	/** Clods of the island in flight from its last slam, and how many have hit each player. */
	private final List<Clod> clods=new ArrayList<>();
	private final Map<UUID,Integer> pelted=new HashMap<>();
	/** A clod: a block display it moves itself, so the island is never actually dug up. */
	private static final class Clod{
		final Display.BlockDisplay shown;final BlockState state;Vec3 at,vel;int age;final Quaternionf spin;
		Clod(Display.BlockDisplay shown,BlockState state,Vec3 at,Vec3 vel,Quaternionf spin){this.shown=shown;this.state=state;this.at=at;this.vel=vel;this.spin=spin;}
	}

	public RootcoilSerpent(EntityType<? extends RootcoilSerpent> type,Level level){super(type,level);xpReward=SerpentRules.TIER.xp;setPersistenceRequired();Arrays.fill(lastMade,-9999);}
	public static AttributeSupplier.Builder createAttributes(){
		return createMobAttributes().add(Attributes.MAX_HEALTH,SerpentRules.HEALTH).add(Attributes.ARMOR,SerpentRules.ARMOUR).add(Attributes.ARMOR_TOUGHNESS,SerpentRules.TIER.toughness)
			.add(Attributes.MOVEMENT_SPEED,SerpentRules.SPEED).add(Attributes.ATTACK_DAMAGE,SerpentRules.TIER.melee).add(Attributes.FOLLOW_RANGE,32).add(Attributes.KNOCKBACK_RESISTANCE,1)
			.add(Attributes.STEP_HEIGHT,1.5).add(Attributes.WATER_MOVEMENT_EFFICIENCY,1);
	}

	/** Bind it to its tree: {@code tree} is the air above the ground at the trunk's foot, {@code rest} the yaw it watches the herb along. */
	public void bind(BlockPos tree,float rest){
		bound=true;entityData.set(TREE,tree.immutable());entityData.set(REST,rest);entityData.set(LANDING,tree.immutable());
		coil();
	}
	public int move(){return entityData.get(MOVE);}
	public int tell(){return entityData.get(TELL);}
	public int side(){return entityData.get(SIDE);}
	public float elapsed(float partial){return (float)(level().getGameTime()-entityData.get(BEGAN))+partial;}
	public BlockPos tree(){return entityData.get(TREE);}
	public BlockPos landing(){return entityData.get(LANDING);}
	public float rest(){return entityData.get(REST);}
	/** The trunk's centre at its foot. */
	public Vec3 trunk(){return Vec3.atBottomCenterOf(tree());}

	@Override protected void defineSynchedData(SynchedEntityData.Builder b){
		super.defineSynchedData(b);b.define(MOVE,SerpentRules.DORMANT);b.define(BEGAN,0L);b.define(TREE,BlockPos.ZERO);b.define(LANDING,BlockPos.ZERO);b.define(REST,0F);b.define(TELL,0);b.define(SIDE,1);
	}
	@Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(MOVE.equals(key))refreshDimensions();}
	@Override protected EntityDimensions getDefaultDimensions(Pose pose){return SerpentRules.scripted(move())?COILED:REARED;}
	@Override protected void registerGoals(){}
	@Override public boolean isPushable(){return false;}
	@Override protected void doPush(Entity entity){}
	@Override public boolean canBreatheUnderwater(){return true;}
	@Override public boolean removeWhenFarAway(double distance){return false;}

	private void pose(int move,int ticks){pose(move,ticks,0);}
	private void pose(int move,int ticks,int tell){
		int was=move();entityData.set(MOVE,move);entityData.set(BEGAN,level().getGameTime());entityData.set(TELL,tell);left=ticks;getNavigation().stop();
		// Coiled on the trunk, and moving under the water or through the air, it is placed rather than moved.
		boolean held=SerpentRules.scripted(move) || move==SerpentRules.SUBMERGED || move==SerpentRules.BURST || move==SerpentRules.RING_COIL;noPhysics=held;setNoGravity(held);
		if(was!=move)refreshDimensions();
		if(!SerpentRules.scripted(move))bar.setName(Component.translatable("boss.wildercord.rootcoil_serpent.status",getType().getDescription(),
			Component.translatable("boss.wildercord.rootcoil_serpent.move."+SerpentRules.key(move))));
	}
	/** Ticks since the current move began: 1 on its first tick. */
	private long since(){return level().getGameTime()-entityData.get(BEGAN);}
	/** Back round the trunk, whole again. */
	private void coil(){
		letGo(false);dropClods();dropSpikes();roots.clear();rippleAge=-1;
		deck.clear();Arrays.fill(lastMade,-9999);links=0;
		noPhysics=false;setNoGravity(false);
		pose(SerpentRules.DORMANT,0);hold();setHealth(getMaxHealth());setTarget(null);bar.removeAllPlayers();quiet=0;
		dived=false;water=false;bursts=0;bar.setColor(BossEvent.BossBarColor.GREEN);
	}
	/** Pinned to the trunk's foot while it is coiled there (or climbing on or off it). */
	private void hold(){
		var t=trunk();setPos(t.x,t.y,t.z);setDeltaMovement(Vec3.ZERO);setYRot(rest());yBodyRot=rest();yHeadRot=rest();
	}

	@Override protected void customServerAiStep(ServerLevel level){
		super.customServerAiStep(level);if(!isAlive())return;
		if(!clods.isEmpty())fly(level);
		if(!roots.isEmpty())grow(level);
		if(rippleAge>=0)ripple(level);
		if(!bound)bind(blockPosition(),getYRot());
		// Left alone long enough (its prey died and went home, say) and it has gone back round the trunk in the meantime.
		long now=level.getGameTime();
		if(lastTick>=0 && move()!=SerpentRules.DORMANT && SerpentRules.forgets(now-lastTick))coil();
		lastTick=now;
		if(tickCount%SerpentRules.GUARD_EVERY==0 && Guardians.enabled())guard(level);
		switch(move()){
			case SerpentRules.DORMANT->dormant(level);
			case SerpentRules.UNCOIL->{hold();if(--left<=0)land(level);else if(left==SerpentRules.UNCOIL_TICKS/2)Feels.sound(level,trunk().add(0,3,0),"aura_serpent_uncoil",1,1);}
			case SerpentRules.RECOIL->{hold();var p=wakeful(level);if(p!=null)wake(level,SerpentRules.UNCOIL_TICKS-left);else if(--left<=0)coil();}
			default->awake(level);
		}
	}

	/**
	 * Its grove is its own: anything hostile that wanders or spawns in, asleep or awake, it snaps up, one every
	 * {@link SerpentRules#GUARD_EVERY} ticks, nearest the trunk first. Only players get a fight.
	 */
	private void guard(ServerLevel level){
		var t=trunk();
		var box=new AABB(t,t).inflate(SerpentRules.ARENA,SerpentRules.LAIR_HEIGHT,SerpentRules.ARENA);
		var m=level.getEntitiesOfClass(Mob.class,box,e->e instanceof Enemy && e!=this && e.isAlive() && !(e instanceof RootcoilSerpent)
			&& SerpentRules.inLair(e.getX()-t.x,e.getY()-t.y,e.getZ()-t.z)).stream().min(Comparator.comparingDouble(e->e.distanceToSqr(t))).orElse(null);
		if(m==null)return;
		var from=move()==SerpentRules.DORMANT || SerpentRules.scripted(move())?t.add(0,4,0):mouth();var to=m.position().add(0,m.getBbHeight()/2,0);
		var step=to.subtract(from);int n=Math.max(2,(int)(step.length()*2));
		for(int i=0;i<=n;i++){var at=from.add(step.scale(i/(double)n));level.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM,at.x,at.y,at.z,1,.1,.1,.1,0);}
		level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,to.x,to.y,to.z,6,.3,.3,.3,.1);
		Feels.sound(level,to,"aura_serpent_strike",1.1F,1.1F+random.nextFloat()*.2F);
		m.hurtServer(level,damageSources().mobAttack(this),Float.MAX_VALUE);
		if(m.isAlive())m.kill(level);
	}

	/** The nearest player close enough to the trunk to wake it, if any. */
	private ServerPlayer wakeful(ServerLevel level){
		var t=trunk();
		return level.players().stream().filter(p->p.isAlive() && !p.isCreative() && !p.isSpectator() && SerpentRules.wakes(p.getX()-t.x,p.getY()-t.y,p.getZ()-t.z))
			.min(Comparator.comparingDouble(p->p.distanceToSqr(t))).orElse(null);
	}

	private void dormant(ServerLevel level){
		hold();if(getHealth()<getMaxHealth())setHealth(getMaxHealth());
		if(tickCount%5==0 && Guardians.enabled() && wakeful(level)!=null)wake(level,0);
		else if(random.nextInt(160)==0)Feels.sound(level,trunk().add(0,5,0),"aura_serpent_hiss",.7F,.85F+random.nextFloat()*.3F);
	}

	/** Begin the uncoil, {@code already} ticks in (when it is woken half way back up the trunk). */
	private void wake(ServerLevel level,int already){
		if(already<=0)entityData.set(LANDING,landingSpot(level));
		pose(SerpentRules.UNCOIL,SerpentRules.UNCOIL_TICKS-already);entityData.set(BEGAN,level.getGameTime()-already);
		Feels.sound(level,trunk().add(0,5,0),"aura_serpent_wake",1.4F,1);
	}

	/**
	 * Where it comes down: on the island between the trunk and the herb, the first spot outward that its reared body fits, turning a
	 * little either side to miss the roots. With no clear spot it lands in front of the herb anyway and shoulders its way out.
	 */
	private BlockPos landingSpot(ServerLevel level){
		var t=trunk();double yaw=Math.toRadians(rest());
		for(double d=SerpentRules.LAND_NEAR;d<=SerpentRules.LAND_FAR;d+=0.5)for(int turn:new int[]{0,-15,15,-30,30,-45,45}){
			double a=yaw+Math.toRadians(turn);
			int x=(int)Math.floor(t.x-Math.sin(a)*d),z=(int)Math.floor(t.z+Math.cos(a)*d);
			for(int y=tree().getY()+1;y>=tree().getY()-3;y--){
				var at=new BlockPos(x,y,z);
				if(level.getBlockState(at.below()).isAir())continue;
				if(level.noCollision(this,REARED.makeBoundingBox(Vec3.atBottomCenterOf(at))))return at;
				break;
			}
		}
		return BlockPos.containing(t.x-Math.sin(yaw)*5,tree().getY(),t.z+Math.cos(yaw)*5);
	}

	/** The uncoil is done: it is on the island, reared, and the fight is on. */
	private void land(ServerLevel level){
		var at=Vec3.atBottomCenterOf(landing());var t=trunk();
		float facing=(float)Math.toDegrees(Math.atan2(-(at.x-t.x),at.z-t.z));
		pose(SerpentRules.WATCH,SerpentRules.GAP);snapTo(at.x,at.y,at.z,facing,0);yBodyRot=facing;yHeadRot=facing;setDeltaMovement(Vec3.ZERO);
		lastCut=level.getGameTime();quiet=0;deck.clear();links=0;
		Feels.sound(level,at.add(0,2.5,0),"aura_serpent_challenge",1.6F,1);
	}

	private void awake(ServerLevel level){
		var t=trunk();int move=move();
		boolean placed=move==SerpentRules.SUBMERGED || move==SerpentRules.BURST || move==SerpentRules.DIVE || move==SerpentRules.RING_COIL;
		if(!placed && SerpentRules.leashed(getX()-t.x,getY()-t.y,getZ()-t.z)){
			letGo(false);
			var at=Vec3.atBottomCenterOf(landing());teleportTo(at.x,at.y,at.z);setDeltaMovement(Vec3.ZERO);getNavigation().stop();pose(SerpentRules.WATCH,gap());
		}
		if(!placed && isInWater() && getFluidHeight(net.minecraft.tags.FluidTags.WATER)>1.2)getJumpControl().jump();
		long now=level.getGameTime();float heal=SerpentRules.regen(getMaxHealth(),now-lastCut);if(heal>0 && getHealth()<getMaxHealth())heal(heal);
		if(tickCount%10==0){
			bar.setProgress(getHealth()/getMaxHealth());
			for(var p:new ArrayList<>(bar.getPlayers()))if(p.level()!=level || !p.isAlive() || p.distanceToSqr(t)>SerpentRules.BAR_DROP*SerpentRules.BAR_DROP)bar.removePlayer(p);
			for(var p:level.players())if(p.isAlive() && !p.isSpectator() && p.distanceToSqr(t)<SerpentRules.BAR_SHOW*SerpentRules.BAR_SHOW)bar.addPlayer(p);
			setTarget(level.players().stream().filter(p->p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(t)<SerpentRules.ARENA*SerpentRules.ARENA && Math.abs(p.getY()-t.y)<12)
				.min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null));
		}
		var target=getTarget();
		switch(move()){
			case SerpentRules.STRIKE->strike(level,target);
			case SerpentRules.LUNGE->lunge(level,target);
			case SerpentRules.CONSTRICT->constrict(level,target);
			case SerpentRules.SQUEEZE->squeeze(level);
			case SerpentRules.SIDESTEP->{face(aim);move(MoverType.SELF,slide);setDeltaMovement(Vec3.ZERO);if(--left<=0)pose(SerpentRules.WATCH,2);}
			case SerpentRules.RECOVER->{getNavigation().stop();if(--left<=0)recovered(level,target);}
			case SerpentRules.DIVE->dive(level);
			case SerpentRules.SUBMERGED->submerged(level,target);
			case SerpentRules.BURST->burst(level);
			case SerpentRules.TAIL_SLAM->slam(level,target);
			case SerpentRules.TAIL_SWEEP->sweep(level);
			case SerpentRules.ROOT_LINE->rootLine(level,target);
			case SerpentRules.HOOD_SPRAY->spray(level,target);
			case SerpentRules.RING_COIL->ringCoil(level);
			case SerpentRules.SPRAWLED,SerpentRules.BURIED->stuck(level);
			default->watch(level,target);
		}
	}

	/** Reared and tracking: closing on its prey, and choosing its next blow once the gap since the last has run out. */
	private void watch(ServerLevel level,LivingEntity target){
		var t=trunk();
		if(target==null){
			quiet++;
			var at=Vec3.atBottomCenterOf(landing());
			if(quiet>=SerpentRules.RETURN_TICKS && distanceToSqr(at)>1.5)getNavigation().moveTo(at.x,at.y,at.z,1);
			if(quiet>=SerpentRules.RESET_TICKS && (distanceToSqr(at)<=2.5 || quiet>=SerpentRules.RESET_TICKS+100)){
				bar.removeAllPlayers();pose(SerpentRules.RECOIL,SerpentRules.UNCOIL_TICKS);hold();Feels.sound(level,t.add(0,3,0),"aura_serpent_uncoil",1,.8F);
			}
			return;
		}
		quiet=0;
		getLookControl().setLookAt(target,30,30);
		if(SerpentRules.dives(getHealth(),getMaxHealth(),dived)){
			dived=true;water=true;bursts=0;links=0;bar.setColor(BossEvent.BossBarColor.BLUE);diveIn(level,true);return;
		}
		double d=distanceTo(target);
		if(left>0)left--;
		int pick=-1;
		if(left<=0){
			pick=SerpentRules.choose(deck,d,behind(target),ages(level.getGameTime()),dived,random.nextDouble());
			// A long chase ends in roots driven at its prey from afar, or a lunge.
			if(pick<0 && chased>=60)pick=d<=SerpentRules.ROOT_REACH && deck.last()!=SerpentRules.ROOT_LINE?SerpentRules.ROOT_LINE:d<=SerpentRules.LUNGE_REACH+5?SerpentRules.LUNGE:-1;
		}
		if(pick>=0){links=0;begin(level,pick,target,false);return;}
		if(d>SerpentRules.CLOSE){
			chased++;
			if(!getNavigation().moveTo(target,1))getMoveControl().setWantedPosition(target.getX(),target.getY(),target.getZ(),1);
		}else{chased=0;getNavigation().stop();}
	}

	/** Ticks since it last made each move, by move. */
	private long[] ages(long now){long[] a=new long[lastMade.length];for(int i=0;i<a.length;i++)a[i]=now-lastMade[i];return a;}
	/** Whether {@code e} stands at its back. */
	private boolean behind(LivingEntity e){return SerpentRules.behind(yawTo(e.position())-getYRot());}

	/** It begins {@code move} at {@code target}; {@code chained} when it runs straight on from the last, with a shorter tell. */
	private void begin(ServerLevel level,int move,LivingEntity target,boolean chained){
		lastMade[move]=level.getGameTime();deck.played(move);struck.clear();landed=false;chased=0;
		aim=move==SerpentRules.TAIL_SWEEP?getYRot():yawTo(target.position());
		entityData.set(SIDE,random.nextBoolean()?1:-1);
		int tell=switch(move){
			case SerpentRules.STRIKE->chained?SerpentRules.STRIKE_CHAIN_TELL:SerpentRules.STRIKE_TELL;
			case SerpentRules.LUNGE->SerpentRules.LUNGE_TELL;
			case SerpentRules.TAIL_SLAM->SerpentRules.TAIL_TELL;
			case SerpentRules.CONSTRICT->SerpentRules.CONSTRICT_TELL;
			case SerpentRules.TAIL_SWEEP->SerpentRules.SWEEP_TELL;
			case SerpentRules.ROOT_LINE->SerpentRules.ROOT_TELL;
			case SerpentRules.HOOD_SPRAY->SerpentRules.SPRAY_TELL;
			default->SerpentRules.RING_SET+SerpentRules.RING_LAP;
		};
		if(chained && move!=SerpentRules.STRIKE && move!=SerpentRules.RING_COIL)tell=tell*3/4;
		var m=mouth();
		switch(move){
			case SerpentRules.STRIKE->Feels.sound(level,m,"aura_serpent_hiss",1.1F,1.35F+random.nextFloat()*.15F);
			case SerpentRules.LUNGE->{Feels.sound(level,m,"aura_serpent_hiss",1.2F,1.2F);sound(level,position(),SoundEvents.SNIFFER_DIGGING,1,.7F);}
			case SerpentRules.TAIL_SLAM->Feels.sound(level,position().add(0,1,0),"aura_serpent_hiss",1.3F,.7F);
			case SerpentRules.CONSTRICT->Feels.sound(level,position().add(0,1,0),"aura_serpent_constrict",1.2F,1.1F);
			case SerpentRules.TAIL_SWEEP->Feels.sound(level,position().add(0,1,0),"aura_serpent_hiss",1.1F,1.8F);
			case SerpentRules.ROOT_LINE->{Feels.sound(level,m,"aura_serpent_hiss",1.4F,.6F);sound(level,position(),SoundEvents.ROOTED_DIRT_HIT,1.2F,.6F);}
			case SerpentRules.HOOD_SPRAY->{splashed.clear();Feels.sound(level,m,"aura_serpent_hiss",1.5F,1);sound(level,m,SoundEvents.HONEY_BLOCK_SLIDE,1.2F,.6F);}
			default->{ringUp(target);Feels.sound(level,position().add(0,1,0),"aura_serpent_uncoil",1.2F,1.3F);}
		}
		pose(move,0,tell);
	}

	/** Done with {@code move}: roused, it may run straight on into another; otherwise it is spent, belly bare, for {@code recover} ticks. */
	private void after(ServerLevel level,int move,int recover){
		var target=getTarget();
		if(dived && !water && target!=null && target.isAlive() && links<SerpentRules.CHAIN_MOST){
			int next=SerpentRules.chain(move,landed,distanceTo(target),behind(target),ages(level.getGameTime()),random.nextDouble());
			if(next>=0){links++;begin(level,next,target,true);return;}
		}
		links=0;spent(recover);
	}

	/**
	 * The bite. It draws its head back over its body, jaws wide, tracking its prey until the last few ticks, when a hiss marks the
	 * aim set; then the head snaps straight out along it. Step aside once it hisses.
	 */
	private void strike(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell-SerpentRules.STRIKE_LOCK && target!=null)aim=yawTo(target.position());
		face(aim);setDeltaMovement(Vec3.ZERO);
		var m=mouth();
		if(el<tell){
			if(el%3==0)level.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM,m.x,m.y,m.z,2,.25,.2,.25,0);
			if(el==tell-SerpentRules.STRIKE_LOCK){Feels.sound(level,m,"aura_serpent_hiss",1.2F,1.7F);level.sendParticles(ParticleTypes.CRIT,m.x,m.y,m.z,6,.3,.3,.3,.05);}
			return;
		}
		if(el==tell){
			Feels.sound(level,m,"aura_serpent_strike",1.3F,.9F+random.nextFloat()*.2F);
			for(int i=1;i<=5;i++){var at=position().add(ahead(aim).scale(i)).add(0,1.6,0);level.sendParticles(ParticleTypes.CRIT,at.x,at.y,at.z,2,.15,.15,.15,.02);}
			for(var p:prey(level))if(reaches(SerpentRules.STRIKE,p) && hit(level,p,SerpentRules.STRIKE_DAMAGE,.5))landed=true;
		}
		if(el>=tell+SerpentRules.STRIKE_HOLD)after(level,SerpentRules.STRIKE,SerpentRules.STRIKE_RECOVER);
	}

	/**
	 * The lunge. It draws its whole body down into tight bends, scuffing the ground, tracking its prey until a hiss sets the line;
	 * then it throws itself {@link SerpentRules#LUNGE_REACH} blocks along it. Step off the line: a lunge that finds nobody leaves it
	 * sprawled along the ground a long while, belly bare.
	 */
	private void lunge(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell-SerpentRules.LUNGE_LOCK && target!=null)aim=yawTo(target.position());
		face(aim);
		if(el<tell){
			setDeltaMovement(Vec3.ZERO);
			if(el%2==0)level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,position())),getX(),getY()+.1,getZ(),5,1.2,.05,1.2,.1);
			if(el==tell-SerpentRules.LUNGE_LOCK){
				Feels.sound(level,mouth(),"aura_serpent_hiss",1.3F,1.5F);
				for(double r=2;r<=SerpentRules.LUNGE_REACH+2;r+=1){var at=position().add(ahead(aim).scale(r));level.sendParticles(new DustParticleOptions(0x6B4A2B,1.6F),at.x,at.y+.15,at.z,2,.3,.02,.3,0);}
			}
			return;
		}
		if(el==tell)Feels.sound(level,position().add(0,1,0),"aura_serpent_lunge",1.4F,1);
		move(MoverType.SELF,ahead(aim).scale(SerpentRules.LUNGE_REACH/SerpentRules.LUNGE_TICKS));setDeltaMovement(Vec3.ZERO);
		level.sendParticles(ParticleTypes.CLOUD,getX(),getY()+.3,getZ(),3,.6,.1,.6,.02);
		for(var p:prey(level))if(!struck.contains(p.getUUID()) && reaches(SerpentRules.LUNGE,p)){struck.add(p.getUUID());if(hit(level,p,SerpentRules.LUNGE_DAMAGE,.9))landed=true;}
		if(el<tell+SerpentRules.LUNGE_TICKS-1)return;
		if(landed){after(level,SerpentRules.LUNGE,SerpentRules.LUNGE_RECOVER);return;}
		// Nobody there: it comes down full length on the ground.
		pose(SerpentRules.SPRAWLED,SerpentRules.SPRAWL_TICKS);
		Feels.sound(level,position(),"aura_serpent_slam",1.1F,1.3F);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,position())),getX(),getY()+.2,getZ(),30,1.5,.1,1.5,.15);
	}

	/**
	 * Sprawled after a lunge that found nothing, or with its head stuck in the earth after driving its roots: it lies there a while,
	 * open, then gathers itself, tearing its head free with a spray of earth.
	 */
	private void stuck(ServerLevel level){
		face(aim);setDeltaMovement(Vec3.ZERO);getNavigation().stop();
		boolean head=move()==SerpentRules.BURIED;var at=head?position().add(ahead(aim).scale(2.6)):position();
		if(tickCount%4==0)level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,at)),at.x,at.y+.2,at.z,4,.6,.1,.6,.05);
		if(head && left==8)sound(level,at,SoundEvents.ROOTED_DIRT_BREAK,1.2F,.8F);
		if(--left>0)return;
		if(head){
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,at)),at.x,at.y+.4,at.z,30,.7,.4,.7,.25);
			Feels.sound(level,at,"aura_serpent_debris",1.2F,.8F);
		}
		links=0;pose(SerpentRules.WATCH,gap());
	}

	/**
	 * The tail slam. Through the tell it tracks its prey, locking its aim for the last three ticks while the tail comes over; then the
	 * tail comes down {@link SerpentRules#SLAM_AHEAD} in front of it, jarring anyone right there, and the ground it struck is flung
	 * along the aim in a fan.
	 */
	private void slam(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell-SerpentRules.TAIL_LOCK && target!=null)aim=yawTo(target.position());
		face(aim);setDeltaMovement(Vec3.ZERO);
		if(el<tell){
			if(el%2==0)level.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM,getX(),getY()+3,getZ(),3,1.5,1,1.5,0);
			// Its aim set, the ground where the tail will come down darkens.
			if(el>=tell-SerpentRules.TAIL_LOCK){var down=position().add(ahead(aim).scale(SerpentRules.SLAM_AHEAD));
				level.sendParticles(new DustParticleOptions(0x6B4A2B,1.6F),down.x,down.y+.15,down.z,10,SerpentRules.SHOCK_RADIUS*.45,.02,SerpentRules.SHOCK_RADIUS*.45,0);}
			return;
		}
		if(el==tell){
			var down=position().add(ahead(aim).scale(SerpentRules.SLAM_AHEAD));
			var ground=ground(level,down);
			Feels.sound(level,down,"aura_serpent_slam",1.8F,.9F+random.nextFloat()*.15F);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground),down.x,down.y+.2,down.z,60,1.4,.2,1.4,.25);
			level.sendParticles(ParticleTypes.EXPLOSION,down.x,down.y+.4,down.z,1,0,0,0,0);
			for(var p:prey(level))if(p.position().distanceToSqr(down)<=SerpentRules.SHOCK_RADIUS*SerpentRules.SHOCK_RADIUS
				&& GuardianBite.land(level,this,p,(float)SerpentRules.SHOCK_DAMAGE,SerpentRules.SLAM_PIERCE)){
				p.push(0,.45,0);p.needsSync=true;landed=true;
			}
			pelted.clear();
			for(int i=0;i<SerpentRules.DEBRIS;i++){
				float yaw=aim+(float)((i-(SerpentRules.DEBRIS-1)/2.0)*SerpentRules.DEBRIS_FAN);
				var from=down.add((random.nextDouble()-.5)*.8,SerpentRules.DEBRIS_FROM,(random.nextDouble()-.5)*.8);
				// Each clod is torn from the ground under where it starts: a slam on moss throws moss, one on stone throws stone.
				throwClod(level,from,ahead(yaw).scale(SerpentRules.DEBRIS_SPEED).add(0,SerpentRules.DEBRIS_LIFT,0),ground(level,from));
			}
		}
		if(el>=tell+3)after(level,SerpentRules.TAIL_SLAM,SerpentRules.TAIL_RECOVER);
	}

	/**
	 * The tail sweep. Through the tell it lifts its tail tip off the ground behind it and rattles it, a ring of earth darkening at
	 * the tail's reach; then it brings the whole tail round in one full turn at ankle height, starting from behind it. Anyone in
	 * reach, wherever they stand, has to jump it as it comes.
	 */
	private void sweep(ServerLevel level){
		face(aim);setDeltaMovement(Vec3.ZERO);
		long el=since();int tell=tell(),side=side();
		if(el<tell){
			var tip=around(SerpentRules.tail(side,0),SerpentRules.SWEEP_REACH-.8,1.6);
			if(el%2==0){
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,position())),tip.x,tip.y,tip.z,4,.2,.2,.2,.1);
				Feels.sound(level,tip,"aura_serpent_hiss",.8F,1.8F+random.nextFloat()*.3F);
			}
			if(el>=tell-4)for(int i=0;i<24;i++){var at=around(i*15,SerpentRules.SWEEP_REACH,.1);level.sendParticles(new DustParticleOptions(0x6B4A2B,1.3F),at.x,at.y,at.z,1,.1,0,.1,0);}
			return;
		}
		int tick=(int)(el-tell)+1;
		if(tick==1){sound(level,position(),SoundEvents.PLAYER_ATTACK_SWEEP,1.5F,.55F);Feels.sound(level,position(),"aura_serpent_slam",.7F,1.5F);}
		double a0=SerpentRules.tail(side,tick-1),a1=SerpentRules.tail(side,tick);
		for(int k=0;k<=2;k++){
			double a=a0+(a1-a0)*k/2;
			var mid=around(a,SerpentRules.SWEEP_REACH*.6,.4);level.sendParticles(ParticleTypes.SWEEP_ATTACK,mid.x,mid.y,mid.z,1,0,0,0,0);
			var tip=around(a,SerpentRules.SWEEP_REACH,.2);level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,tip)),tip.x,tip.y,tip.z,4,.3,.1,.3,.1);
		}
		for(var p:prey(level)){
			if(struck.contains(p.getUUID()))continue;
			double[] b=SerpentSpine.fromWorld(new double[]{p.getX()-getX(),p.getY()-getY(),p.getZ()-getZ()},aim);
			if(!SerpentRules.sweeps(side,tick,Math.toDegrees(Math.atan2(b[0],b[2])),Math.hypot(b[0],b[2]),b[1]))continue;
			struck.add(p.getUUID());
			if(hit(level,p,SerpentRules.SWEEP_DAMAGE,.8,outward(p.position(),position()))){landed=true;p.push(0,.25,0);}
		}
		if(tick>=SerpentRules.SWEEP_TICKS)after(level,SerpentRules.TAIL_SWEEP,SerpentRules.SWEEP_RECOVER);
	}

	/**
	 * Driving roots. It rears tall and looks down along its prey, earth trickling off it; once its aim is set the ground cracks along
	 * the line, and then it drives its head into the earth and a line of roots bursts out along it, faster than a run and too tall to
	 * jump. Step off the line. Roused, it drives three in a fan. Its head stays stuck fast a while after.
	 */
	private void rootLine(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell-SerpentRules.ROOT_LOCK && target!=null)aim=yawTo(target.position());
		face(aim);setDeltaMovement(Vec3.ZERO);
		if(el<tell){
			var m=mouth();
			if(el%3==0)level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,position())),m.x,m.y-.5,m.z,3,.4,.3,.4,0);
			if(el==tell-SerpentRules.ROOT_LOCK)sound(level,position(),SoundEvents.ROOTED_DIRT_HIT,1.4F,.5F);
			if(el>=tell-SerpentRules.ROOT_LOCK)for(float yaw:lines())for(double r=SerpentRules.ROOT_FROM;r<=SerpentRules.ROOT_REACH;r+=1.5){
				var at=position().add(ahead(yaw).scale(r));level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,at)),at.x,at.y+.1,at.z,1,.2,0,.2,.05);
			}
			return;
		}
		var shared=new HashSet<UUID>();
		for(float yaw:lines())roots.add(new Rootline(position(),yaw,shared));
		var at=position().add(ahead(aim).scale(2.6));
		Feels.sound(level,at,"aura_serpent_slam",1.6F,.7F);sound(level,at,SoundEvents.ROOTED_DIRT_BREAK,1.6F,.6F);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,at)),at.x,at.y+.2,at.z,40,1,.2,1,.2);
		pose(SerpentRules.BURIED,SerpentRules.ROOT_STUCK);
	}
	/** The lines its roots run along. */
	private float[] lines(){return dived?new float[]{aim-(float)SerpentRules.ROOT_FAN,aim,aim+(float)SerpentRules.ROOT_FAN}:new float[]{aim};}

	/** Every line of roots runs on, roots standing up out of the ground at its head and throwing up whoever they catch. */
	private void grow(ServerLevel level){
		var players=prey(level);
		for(var it=roots.iterator();it.hasNext();){
			var r=it.next();r.age++;
			double r0=SerpentRules.roots(r.age-1),r1=SerpentRules.roots(r.age);
			var tip=r.from.add(ahead(r.yaw).scale(r1));
			level.sendParticles(new BlockParticleOption(ParticleTypes.DUST_PILLAR,ground(level,tip)),tip.x,tip.y+.1,tip.z,8,.4,.1,.4,.1);
			if(r.age%2==0)spike(level,new Vec3(tip.x,top(level,tip),tip.z),r.yaw,1.8F+random.nextFloat()*.8F,.55F,12);
			if(r.age%3==0)sound(level,tip,SoundEvents.ROOTED_DIRT_BREAK,1.2F,.7F+random.nextFloat()*.3F);
			for(var p:players){
				if(r.struck.contains(p.getUUID()))continue;
				double[] b=SerpentSpine.fromWorld(new double[]{p.getX()-r.from.x,p.getY()-r.from.y,p.getZ()-r.from.z},r.yaw);
				if(!SerpentRules.rooted(b[2],b[0],b[1],r0,r1))continue;
				r.struck.add(p.getUUID());
				if(hit(level,p,SerpentRules.ROOT_DAMAGE,0)){p.push(0,SerpentRules.ROOT_LIFT,0);p.needsSync=true;}
			}
			if(r1>=SerpentRules.ROOT_REACH)it.remove();
		}
	}

	/** A root stands up out of the ground at {@code base}, leaning out along {@code yaw}, holds a moment and sinks back. */
	private void spike(ServerLevel level,Vec3 base,float yaw,float h,float w,int hold){
		var shown=EntityTypes.BLOCK_DISPLAY.create(level,EntitySpawnReason.TRIGGERED);
		if(shown==null)return;
		var turn=new Quaternionf().rotateY((float)Math.toRadians(-yaw)).rotateX(.2F+random.nextFloat()*.35F).rotateY(random.nextFloat()*1.6F);
		shown.snapTo(base.x,base.y-.1,base.z);shown.setBlockState(Blocks.MANGROVE_ROOTS.defaultBlockState());shown.setTransformation(stand(turn,w,.02F));
		BlockFx.fresh(shown);level.addFreshEntity(shown);spikes.add(shown);
		Scheduler.later(1,()->tween(shown,stand(turn,w,h),2));
		Scheduler.later(3+hold,()->tween(shown,stand(turn,w,.02F),6));
		Scheduler.later(10+hold,()->{shown.discard();spikes.remove(shown);});
	}
	/** A {@code w}-wide, {@code h}-tall block turned by {@code turn} about the middle of its foot. */
	private static Transformation stand(Quaternionf turn,float w,float h){
		var shift=new Vector3f(-w/2,0,-w/2).rotate(turn);return new Transformation(shift,new Quaternionf(turn),new Vector3f(w,h,w),new Quaternionf());
	}
	private static void tween(Display.BlockDisplay d,Transformation to,int ticks){
		if(d.isRemoved())return;d.setTransformationInterpolationDelay(0);d.setTransformationInterpolationDuration(ticks);d.setTransformation(to);
	}
	private void dropSpikes(){for(var d:spikes)d.discard();spikes.clear();}

	/**
	 * The hood spray. Its hood spreads wide and it rears back, sap welling and dripping at its jaws; then it swings its head across
	 * in front of it from one side to the other, spitting a jet of sticky sap that stings and slows. Get round behind it, or keep out
	 * of range. Roused, it swings wider. Reared to spray, its belly is open.
	 */
	private void spray(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell(),side=side();
		if(el<=tell-4 && target!=null)aim=yawTo(target.position());
		setDeltaMovement(Vec3.ZERO);
		var m=mouth();
		if(el<tell){
			face(aim);
			if(el%2==0)level.sendParticles(ParticleTypes.DRIPPING_HONEY,m.x,m.y,m.z,3,.3,.2,.3,0);
			if(el==tell-4){Feels.sound(level,m,"aura_serpent_hiss",1.4F,.8F);sound(level,m,SoundEvents.BEEHIVE_DRIP,1.5F,.5F);}
			return;
		}
		int tick=(int)(el-tell);
		double jet=SerpentRules.jet(side,tick,dived);
		var far=around(jet,SerpentRules.SPRAY_REACH,.4);var dir=far.subtract(position());
		face((float)Math.toDegrees(Math.atan2(-dir.x,dir.z)));
		m=mouth();
		for(int i=1;i<=8;i++){var at=m.add(far.subtract(m).scale(i/8.0));level.sendParticles(ParticleTypes.FALLING_HONEY,at.x,at.y,at.z,1,.15,.15,.15,0);if(i%3==0)level.sendParticles(ParticleTypes.SPIT,at.x,at.y,at.z,1,.1,.1,.1,.02);}
		if(tick%4==0)sound(level,m,SoundEvents.LLAMA_SPIT,1.3F,.6F+random.nextFloat()*.2F);
		if(tick%SerpentRules.SPRAY_EVERY==0)for(var p:prey(level)){
			int hits=splashed.getOrDefault(p.getUUID(),0);
			if(hits>=SerpentRules.SPRAY_HITS)continue;
			double[] b=SerpentSpine.fromWorld(new double[]{p.getX()-getX(),p.getY()-getY(),p.getZ()-getZ()},aim);
			if(!SerpentRules.sprays(jet,Math.toDegrees(Math.atan2(b[0],b[2])),Math.hypot(b[0],b[2]),b[1]))continue;
			splashed.put(p.getUUID(),hits+1);
			if(hit(level,p,SerpentRules.SPRAY_DAMAGE,0)){landed=true;p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,SerpentRules.SPRAY_SLOW,1),this);}
		}
		if(tick>=SerpentRules.SPRAY_TICKS)after(level,SerpentRules.HOOD_SPRAY,SerpentRules.SPRAY_RECOVER);
	}

	/**
	 * Where it rings its prey: round where they stand, kept off the trunk, the ring as wide as fits there. It slides out onto the
	 * ring at the nearest point.
	 */
	private void ringUp(LivingEntity target){
		var t=trunk();var p=target.position();var off=p.subtract(t).multiply(1,0,1);double len=off.length();
		double want=Math.max(5.2,Math.min(SerpentRules.RING-1.5,len));
		var flat=len<1e-3?new Vec3(want,0,0):off.scale(want/len);
		middle=new Vec3(t.x+flat.x,p.y,t.z+flat.z);
		ringR=Math.max(3,Math.min(SerpentRules.RING_R,want-2.2));
		ringStart=Math.atan2(getZ()-middle.z,getX()-middle.x);from=position();
	}
	/** Its ring's point {@code a} radians round, {@code r} out, on the ground. */
	private Vec3 onRing(ServerLevel level,double a,double r){var at=new Vec3(middle.x+Math.cos(a)*r,middle.y,middle.z+Math.sin(a)*r);return new Vec3(at.x,top(level,at),at.z);}

	/**
	 * Coiling round. It slides out round its prey and runs once all the way round them, its trail marked on the ground, then draws
	 * the ring shut on whatever is inside: a crushing squeeze, or, if its coils are ready, a hold. Get out of the ring before it
	 * closes.
	 */
	private void ringCoil(ServerLevel level){
		setDeltaMovement(Vec3.ZERO);getNavigation().stop();
		long el=since();int side=side(),set=SerpentRules.RING_SET,lap=SerpentRules.RING_LAP;
		double a;Vec3 at;
		if(el<=set){
			a=ringStart;var goal=onRing(level,a,ringR);at=from.add(goal.subtract(from).scale(SerpentSpine.smooth(el/(double)set)));
		}else if(el<=set+lap){
			a=ringStart+side*2*Math.PI*(el-set)/lap;at=onRing(level,a,ringR);
			if(el%2==0)Feels.sound(level,at,"aura_serpent_hiss",.6F,1.4F);
		}else{
			int tick=(int)(el-set-lap);
			double r0=SerpentRules.ring(ringR,tick-1),r1=SerpentRules.ring(ringR,tick);
			a=ringStart+side*(2*Math.PI+tick*.35);at=onRing(level,a,Math.max(1.5,r1));
			if(tick==1)Feels.sound(level,middle,"aura_serpent_constrict",1.5F,.8F);
			for(var p:prey(level)){
				if(struck.contains(p.getUUID()))continue;
				double dist=Math.hypot(p.getX()-middle.x,p.getZ()-middle.z);
				if(!SerpentRules.closes(dist,r0,r1,p.getY()-middle.y))continue;
				struck.add(p.getUUID());landed=true;
				if(caught==null && level.getGameTime()-lastMade[SerpentRules.CONSTRICT]>=SerpentRules.rest(SerpentRules.CONSTRICT,dived)){
					setPos(at.x,at.y,at.z);aim=yawTo(p.position());face(aim);grab(level,p);return;
				}
				if(hit(level,p,SerpentRules.RING_DAMAGE,.4,outward(middle,p.position())))p.push(0,.35,0);
			}
			if(tick>=SerpentRules.RING_CLOSE){
				setPos(at.x,at.y,at.z);noPhysics=false;setNoGravity(false);
				if(!level.noCollision(this,getBoundingBox())){var land=Vec3.atBottomCenterOf(landing());teleportTo(land.x,land.y,land.z);}
				aim=yawTo(middle);face(aim);
				after(level,SerpentRules.RING_COIL,SerpentRules.RING_RECOVER);return;
			}
		}
		var was=position();setPos(at.x,at.y,at.z);
		var step=at.subtract(was);if(step.horizontalDistanceSqr()>1e-4)face((float)Math.toDegrees(Math.atan2(-step.x,step.z)));
		// The ring it has run so far shows on the ground.
		if(el>set && el<=set+lap)for(int i=0;i<24;i++){
			double b=ringStart+side*2*Math.PI*i/24;if(i*lap>(el-set)*24)break;
			var on=onRing(level,b,ringR);level.sendParticles(new DustParticleOptions(0x6B4A2B,1.4F),on.x,on.y+.1,on.z,1,.1,0,.1,0);
		}
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,at)),at.x,at.y+.1,at.z,3,.5,.05,.5,.05);
	}

	/** The top of the ground at {@code at}, looking a few blocks up and down; its own height if there is none. */
	private double top(ServerLevel level,Vec3 at){
		var pos=BlockPos.containing(at.x,at.y+2,at.z);
		for(int i=0;i<7;i++,pos=pos.below()){
			var state=level.getBlockState(pos);
			if(!state.getFluidState().isEmpty())return pos.getY()+.9;
			var shape=state.getCollisionShape(level,pos);
			if(!shape.isEmpty())return pos.getY()+shape.max(net.minecraft.core.Direction.Axis.Y);
		}
		return at.y;
	}
	/** Which way along the ground {@code to} lies from {@code from}. */
	private Vec3 outward(Vec3 to,Vec3 from){var d=to.subtract(from).multiply(1,0,1);return d.lengthSqr()<1e-4?ahead(aim):d.normalize();}

	/** The solid block under {@code at}, looking a few blocks down; moss if there is none fit to throw. */
	private BlockState ground(ServerLevel level,Vec3 at){
		var pos=BlockPos.containing(at.x,at.y-.6,at.z);
		for(int i=0;i<4;i++,pos=pos.below()){
			var state=level.getBlockState(pos);
			if(state.isAir() || !state.getFluidState().isEmpty())continue;
			if(state.isCollisionShapeFullBlock(level,pos) && !state.hasBlockEntity())return state;
		}
		return Blocks.MOSS_BLOCK.defaultBlockState();
	}

	private static final float CLOD=.8F;
	private void throwClod(ServerLevel level,Vec3 from,Vec3 vel,BlockState state){
		var shown=EntityTypes.BLOCK_DISPLAY.create(level,EntitySpawnReason.TRIGGERED);
		if(shown==null)return;
		var spin=new Quaternionf().rotateXYZ(random.nextFloat()*6.28F,random.nextFloat()*6.28F,random.nextFloat()*6.28F);
		shown.snapTo(from.x,from.y,from.z);shown.setBlockState(state);shown.setTransformation(clod(spin));shown.setPosRotInterpolationDuration(1);
		dev.wildercord.cast.BlockFx.fresh(shown);
		level.addFreshEntity(shown);
		clods.add(new Clod(shown,state,from,vel,spin));
	}
	/** A clod's shape, turned by {@code spin} round its own centre. */
	private static com.mojang.math.Transformation clod(Quaternionf spin){
		var centre=spin.transform(new Vector3f(-CLOD/2,-CLOD/2,-CLOD/2));
		return new com.mojang.math.Transformation(centre,new Quaternionf(spin),new Vector3f(CLOD,CLOD,CLOD),new Quaternionf());
	}

	/** Every clod in flight moves on, tumbling, and breaks on the first player, wall or water in its way. */
	private void fly(ServerLevel level){
		var players=prey(level);
		for(var it=clods.iterator();it.hasNext();){
			var c=it.next();
			c.age++;c.vel=c.vel.add(0,-SerpentRules.DEBRIS_GRAVITY,0);
			var next=c.at.add(c.vel);
			boolean done=c.age>=SerpentRules.DEBRIS_LIFE || c.shown.isRemoved();
			if(!done)for(var p:players){
				var box=p.getBoundingBox().inflate(CLOD/2);
				if(!box.contains(next) && box.clip(c.at,next).isEmpty())continue;
				int hits=pelted.getOrDefault(p.getUUID(),0);
				if(hits>=SerpentRules.DEBRIS_HITS)continue;
				if(GuardianBite.land(level,this,p,(float)SerpentRules.DEBRIS_DAMAGE,SerpentRules.SLAM_PIERCE)){
					pelted.put(p.getUUID(),hits+1);
					var push=c.vel.multiply(1,0,1).normalize();p.knockback(.6,-push.x,-push.z,level.damageSources().mobAttack(this),0);p.needsSync=true;
				}
				next=p.position().add(0,p.getBbHeight()/2,0);done=true;break;
			}
			if(!done){
				var pos=BlockPos.containing(next);var state=level.getBlockState(pos);
				if(!state.getFluidState().isEmpty()){splash(level,next,6);done=true;}
				else if(!state.getCollisionShape(level,pos).isEmpty())done=true;
			}
			c.at=next;
			if(done){shatter(level,c);it.remove();continue;}
			c.shown.setPos(next.x,next.y,next.z);
			if(c.age%3==0){
				c.spin.rotateXYZ(.9F,.5F,.3F);
				c.shown.setTransformationInterpolationDelay(0);c.shown.setTransformationInterpolationDuration(3);c.shown.setTransformation(clod(c.spin));
			}
		}
	}
	private void shatter(ServerLevel level,Clod c){
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,c.state),c.at.x,c.at.y,c.at.z,16,.3,.3,.3,.15);
		Feels.sound(level,c.at,"aura_serpent_debris",1.1F,.85F+random.nextFloat()*.3F);
		c.shown.discard();
	}
	private void dropClods(){for(var c:clods)c.shown.discard();clods.clear();}

	/**
	 * The grab. Its coils swing open in front of it, tracking its prey until the last few ticks, then close on whoever is nearest in
	 * reach. Back off out of reach, or jump clear to the side.
	 */
	private void constrict(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell-SerpentRules.CONSTRICT_LOCK && target!=null)aim=yawTo(target.position());
		face(aim);setDeltaMovement(Vec3.ZERO);
		if(el<tell){
			if(el%2==0){var at=position().add(ahead(aim).scale(2));level.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM,at.x,at.y+1,at.z,4,1.6,.6,1.6,0);}
			return;
		}
		var p=prey(level).stream().filter(q->reaches(SerpentRules.CONSTRICT,q)).min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
		if(p==null){after(level,SerpentRules.CONSTRICT,SerpentRules.CONSTRICT_RECOVER);return;}
		grab(level,p);
	}
	/** Its coils close on {@code p}. */
	private void grab(ServerLevel level,ServerPlayer p){
		caught=p;struggles=0;heldDamage=0;landed=true;lastMade[SerpentRules.CONSTRICT]=level.getGameTime();
		// Whatever the player is already holding down does not count: a struggle is a fresh press.
		var in=p.getLastClientInput();gripping=in.jump() || in.left() || in.right();
		pose(SerpentRules.SQUEEZE,SerpentRules.SQUEEZE_TICKS);
		p.sendOverlayMessage(Component.translatable("message.wildercord.rootcoil_serpent.caught").withColor(0xD8C070));
		Feels.sound(level,p.position(),"aura_serpent_constrict",1.4F,.9F);
	}

	/** Its prey held in the coils: pinned, squeezed now and then, until it struggles free, hurts it enough, or is let go. */
	private void squeeze(ServerLevel level){
		face(aim);setDeltaMovement(Vec3.ZERO);
		var p=caught;
		if(p==null || !p.isAlive() || p.isRemoved() || p.level()!=level || p.isCreative() || p.isSpectator()){letGo(false);spent(SerpentRules.CONSTRICT_RECOVER);return;}
		var grip=position().add(ahead(aim).scale(SerpentSpine.GRIP)).add(0,.6,0);
		if(p.position().distanceTo(grip)>0.6)p.teleportTo(grip.x,grip.y,grip.z);
		p.setDeltaMovement(Vec3.ZERO);p.needsSync=true;
		var in=p.getLastClientInput();boolean pressing=in.jump() || in.left() || in.right();
		if(pressing && !gripping){
			struggles++;
			p.sendOverlayMessage(Component.translatable("message.wildercord.rootcoil_serpent.struggle",struggles,SerpentRules.STRUGGLES).withColor(0xD8C070));
			level.sendParticles(ParticleTypes.CRIT,p.getX(),p.getY()+1,p.getZ(),4,.4,.4,.4,.1);
		}
		gripping=pressing;
		long el=since();
		if(el%SerpentRules.SQUEEZE_EVERY==0){
			hit(level,p,SerpentRules.SQUEEZE_DAMAGE,0);
			Feels.sound(level,p.position(),"aura_serpent_constrict",.8F,1.2F);
		}
		if(struggles>=SerpentRules.STRUGGLES || heldDamage>=SerpentRules.BREAK_FREE){letGo(true);spent(SerpentRules.CONSTRICT_RECOVER+10);}
		else if(el>=SerpentRules.SQUEEZE_TICKS){letGo(false);spent(SerpentRules.CONSTRICT_RECOVER);}
	}

	/** Lets go of whoever it holds, thrown clear; {@code broke} when they fought their way out. */
	private void letGo(boolean broke){
		var p=caught;caught=null;
		if(p==null || !p.isAlive())return;
		var out=ahead(aim);
		p.knockback(broke?.5:.9,-out.x,-out.z,damageSources().mobAttack(this),0);p.needsSync=true;
		if(broke){
			p.sendOverlayMessage(Component.translatable("message.wildercord.rootcoil_serpent.free").withColor(0xA8E070));
			if(level() instanceof ServerLevel server)Feels.sound(server,position().add(0,1.5,0),"aura_serpent_hurt",1.2F,.8F);
		}
	}

	/** Spent after an attack, its belly bare, for {@code ticks}. */
	private void spent(int ticks){pose(SerpentRules.RECOVER,ticks);setDeltaMovement(Vec3.ZERO);}

	private void recovered(ServerLevel level,LivingEntity target){
		if(water){
			if(bursts<SerpentRules.BURSTS && target!=null){diveIn(level,false);return;}
			// Three bursts and it comes ashore for good, wounded and quicker.
			water=false;bar.setColor(BossEvent.BossBarColor.RED);
			if(target!=null)Feels.sound(level,mouth(),"aura_serpent_challenge",1.4F,1.15F);
		}
		pose(SerpentRules.WATCH,gap());
	}

	/** Ticks before its next blow. */
	private int gap(){return (dived?SerpentRules.GAP_WOUNDED:SerpentRules.GAP)+random.nextInt(3);}

	/**
	 * It rears and hisses, then slides off the island into the water ring at the nearest point of its path. The first time, at half
	 * health, it {@code rouse}s first: it towers, shaking, earth pouring off it, then brings its whole length down so hard it throws
	 * back everyone near, and roots ripple out across the island.
	 */
	private void diveIn(ServerLevel level,boolean rouse){
		var t=trunk();ring=Math.atan2(getZ()-t.z,getX()-t.x);circle=random.nextBoolean()?1:-1;from=position();
		aim=yawTo(ringPoint());
		pose(SerpentRules.DIVE,SerpentRules.DIVE_TICKS,rouse?SerpentRules.ROUSE_TELL:SerpentRules.DIVE_TICKS/2);
		if(rouse){Feels.sound(level,mouth(),"aura_serpent_challenge",1.8F,.8F);sound(level,mouth(),SoundEvents.RAVAGER_ROAR,1.6F,.6F);}
		else Feels.sound(level,mouth(),"aura_serpent_dive",1.6F,1);
	}

	private void dive(ServerLevel level){
		setDeltaMovement(Vec3.ZERO);
		long el=since();int tell=tell();boolean rousing=tell==SerpentRules.ROUSE_TELL;
		if(el<tell){
			if(rousing && el%2==0)for(int i=0;i<3;i++){
				var at=position().add((random.nextDouble()-.5)*4,1+random.nextDouble()*3,(random.nextDouble()-.5)*4);
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,position())),at.x,at.y,at.z,3,.3,.3,.3,.1);
			}
			if(rousing && el%6==0)sound(level,position(),SoundEvents.ROOTED_DIRT_HIT,1.4F,.5F);
			return;
		}
		if(rousing && el==tell){
			Feels.sound(level,position(),"aura_serpent_slam",1.8F,.6F);sound(level,position(),SoundEvents.GENERIC_EXPLODE.value(),1.2F,.7F);
			level.sendParticles(ParticleTypes.EXPLOSION,getX(),getY()+.5,getZ(),3,1.5,.2,1.5,0);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,position())),getX(),getY()+.3,getZ(),60,2.5,.3,2.5,.3);
			rippleAt=position();rippleAge=0;rippled.clear();
			for(var p:prey(level)){
				if(p.distanceToSqr(this)>4.5*4.5)continue;
				var out=outward(p.position(),position());
				p.knockback(1.4,-out.x,-out.z,level.damageSources().mobAttack(this),0);p.push(0,.5,0);p.needsSync=true;
			}
		}
		int start=tell+(rousing?4:0),span=SerpentRules.DIVE_TICKS-start;
		face(aim);
		if(el<=start)return;
		noPhysics=true;setNoGravity(true);
		double s=SerpentSpine.smooth(Math.min(1,(el-start)/(double)span));var into=ringPoint();
		setPos(from.x+(into.x-from.x)*s,from.y+(into.y-from.y)*s+Math.sin(Math.PI*s)*1.2,from.z+(into.z-from.z)*s);
		if(el==SerpentRules.DIVE_TICKS-2)splash(level,into,24);
		if(el>=SerpentRules.DIVE_TICKS)pose(SerpentRules.SUBMERGED,SerpentRules.CIRCLE_TICKS+random.nextInt(21)-10);
	}

	/** The roots rippling out across the island from where it came down as it roused, roots standing up along the ring as it goes. */
	private void ripple(ServerLevel level){
		rippleAge++;
		double r0=Math.min(SerpentRules.ROUSE_REACH,(rippleAge-1)*SerpentRules.ROUSE_SPEED),r1=Math.min(SerpentRules.ROUSE_REACH,rippleAge*SerpentRules.ROUSE_SPEED);
		int n=(int)Math.max(8,r1*2.2);
		for(int i=0;i<n;i++){
			double a=2*Math.PI*i/n;var at=new Vec3(rippleAt.x+Math.cos(a)*r1,rippleAt.y,rippleAt.z+Math.sin(a)*r1);
			var on=new Vec3(at.x,top(level,at),at.z);
			if(!level.getBlockState(BlockPos.containing(on.x,on.y-.5,on.z)).getFluidState().isEmpty())continue;
			level.sendParticles(new BlockParticleOption(ParticleTypes.DUST_PILLAR,ground(level,on)),on.x,on.y+.1,on.z,2,.2,.05,.2,.05);
			if(rippleAge%3==0 && i%3==0)spike(level,on,(float)Math.toDegrees(Math.atan2(-Math.cos(a),Math.sin(a))),.9F,.35F,6);
		}
		if(rippleAge%4==0)sound(level,rippleAt,SoundEvents.ROOTED_DIRT_BREAK,1.4F,.6F+random.nextFloat()*.2F);
		for(var p:prey(level)){
			if(rippled.contains(p.getUUID()))continue;
			if(!SerpentRules.crosses(Math.hypot(p.getX()-rippleAt.x,p.getZ()-rippleAt.z),r0,r1,p.getY()-rippleAt.y))continue;
			rippled.add(p.getUUID());
			if(hit(level,p,SerpentRules.ROUSE_DAMAGE,.6,outward(p.position(),rippleAt))){p.push(0,.4,0);p.needsSync=true;}
		}
		if(r1>=SerpentRules.ROUSE_REACH)rippleAge=-1;
	}

	/** Its path's point under the water at the current angle round the island. */
	private Vec3 ringPoint(){
		var t=trunk();return new Vec3(t.x+Math.cos(ring)*SerpentRules.RING,t.y-SerpentRules.DEPTH,t.z+Math.sin(ring)*SerpentRules.RING);
	}

	/** Circling under the water out of reach, a trail of bubbles above it; then it stops, the water boils, and it comes. */
	private void submerged(ServerLevel level,LivingEntity target){
		setDeltaMovement(Vec3.ZERO);
		var surface=trunk().y-1;
		if(left>SerpentRules.BURST_TELL){
			ring+=circle*SerpentRules.CIRCLE_SPEED/SerpentRules.RING;
			var at=ringPoint();setPos(at.x,at.y,at.z);
			face((float)Math.toDegrees(Math.atan2(-(-Math.sin(ring)*circle),Math.cos(ring)*circle)));
			if(tickCount%3==0)level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,at.x,surface-.4,at.z,3,.6,.2,.6,.05);
		}else{
			if(target!=null)aim=yawTo(target.position());
			face(aim);
			if(left==SerpentRules.BURST_TELL)Feels.sound(level,new Vec3(getX(),surface,getZ()),"aura_serpent_burst",1.6F,1);
			level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,getX(),surface-.4,getZ(),6,.9,.2,.9,.1);
			if(left%3==0)splash(level,new Vec3(getX(),surface,getZ()),6);
		}
		if(--left>0)return;
		from=position();
		var goal=target!=null?target.position():Vec3.atBottomCenterOf(landing());
		// It comes up short of where its prey stood, so it lands in front of them rather than inside them.
		var dir=goal.subtract(from).multiply(1,0,1);double len=dir.length();
		to=len>2?goal.subtract(dir.scale(1.5/len)):goal;
		aim=yawTo(to);struck.clear();
		pose(SerpentRules.BURST,0);
		splash(level,new Vec3(getX(),surface,getZ()),30);
	}

	/** Out of the water in an arc to where its prey stood, biting at anything along the way. */
	private void burst(ServerLevel level){
		face(aim);setDeltaMovement(Vec3.ZERO);
		double s=Math.min(1,since()/(double)SerpentRules.BURST_TICKS);
		setPos(from.x+(to.x-from.x)*s,from.y+(to.y-from.y)*s+Math.sin(Math.PI*s)*2.5,from.z+(to.z-from.z)*s);
		for(var p:prey(level))if(!struck.contains(p.getUUID()) && reaches(SerpentRules.BURST,p)){struck.add(p.getUUID());hit(level,p,SerpentRules.BURST_DAMAGE,.9);}
		if(s<1)return;
		bursts++;
		noPhysics=false;setNoGravity(false);
		// Come down inside a wall and it shoulders back out onto the island.
		if(!level.noCollision(this,getBoundingBox())){var at=Vec3.atBottomCenterOf(landing());teleportTo(at.x,at.y,at.z);}
		Feels.sound(level,position(),"aura_serpent_strike",1.4F,.75F);
		spent(SerpentRules.LUNGE_RECOVER);
	}

	private void splash(ServerLevel level,Vec3 at,int count){
		level.sendParticles(ParticleTypes.SPLASH,at.x,at.y+.2,at.z,count,1.2,.3,1.2,.3);
		level.sendParticles(ParticleTypes.BUBBLE,at.x,at.y-.3,at.z,count/2,1,.3,1,.1);
	}

	/** Players it could be fighting right now. */
	private List<ServerPlayer> prey(ServerLevel level){
		var out=new ArrayList<ServerPlayer>();
		for(var p:level.players())if(p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(this)<12*12)out.add(p);
		return out;
	}
	/** Whether {@code move}, along the locked aim, reaches {@code p}. */
	private boolean reaches(int move,LivingEntity p){
		double[] b=SerpentSpine.fromWorld(new double[]{p.getX()-getX(),p.getY()-getY(),p.getZ()-getZ()},aim);
		return SerpentRules.hits(move,b[2],b[0],b[1]);
	}
	/** A blow landing on {@code p}, through part of its armour, knocking it {@code shove} along the aim; whether it hurt. */
	private boolean hit(ServerLevel level,LivingEntity p,double damage,double shove){return hit(level,p,damage,shove,ahead(aim));}
	/** A blow landing on {@code p}, knocking it {@code shove} along {@code dir}; whether it hurt. */
	private boolean hit(ServerLevel level,LivingEntity p,double damage,double shove,Vec3 dir){
		boolean hurt=GuardianBite.land(level,this,p,(float)damage,SerpentRules.PIERCE);
		if(hurt && shove>0){p.knockback(shove,-dir.x,-dir.z,level.damageSources().mobAttack(this),0);p.needsSync=true;}
		return hurt;
	}
	private void sound(ServerLevel level,Vec3 at,SoundEvent s,float volume,float pitch){level.playSound(null,at.x,at.y,at.z,s,SoundSource.HOSTILE,volume,pitch);}
	/** The point {@code angle} degrees round off its aim, {@code reach} out and {@code up} high. */
	private Vec3 around(double angle,double reach,double up){
		double r=Math.toRadians(angle);double[] w=SerpentSpine.toWorld(new double[]{Math.sin(r)*reach,up,Math.cos(r)*reach},aim);
		return position().add(w[0],w[1],w[2]);
	}
	private Vec3 ahead(float yaw){double[] f=SerpentSpine.toWorld(new double[]{0,0,1},yaw);return new Vec3(f[0],0,f[2]);}
	private float yawTo(Vec3 at){return (float)Math.toDegrees(Math.atan2(-(at.x-getX()),at.z-getZ()));}
	private void face(float yaw){setYRot(yaw);yBodyRot=yaw;yHeadRot=yaw;}
	private Vec3 mouth(){return position().add(ahead(getYRot()).scale(2.2)).add(0,4.0,0);}

	@Override public boolean hurtServer(ServerLevel level,DamageSource source,float damage){
		if(source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.CRAMMING))return false;
		int move=move();
		if(SerpentRules.scripted(move) && !source.is(DamageTypes.GENERIC_KILL)){
			// Coiled, its scales turn anything aside; but a blow, or a shot, wakes it.
			if(move==SerpentRules.DORMANT && Guardians.enabled() && source.getEntity() instanceof ServerPlayer p && !p.isCreative())wake(level,0);
			return false;
		}
		if(!source.is(DamageTypes.GENERIC_KILL)){
			var at=source.getSourcePosition();
			boolean frontal=at!=null && SerpentRules.frontal(yawTo(at)-getYRot());
			long now=level.getGameTime();
			if(SerpentRules.slips(move,frontal,now-lastSlip)){
				// It sees the blow coming and slides out from under it.
				lastSlip=now;aim=getYRot();int side=random.nextBoolean()?1:-1;
				var f=ahead(aim);slide=new Vec3(-f.z*side,0,f.x*side).scale(SerpentRules.SIDESTEP_SPEED);
				pose(SerpentRules.SIDESTEP,SerpentRules.SIDESTEP_TICKS);
				Feels.sound(level,position().add(0,1,0),"aura_serpent_sidestep",1.2F,.9F+random.nextFloat()*.2F);
				return false;
			}
			damage*=(float)SerpentRules.scales(move,frontal);
			if(move==SerpentRules.SQUEEZE)heldDamage+=damage;
		}
		boolean hurt=super.hurtServer(level,source,damage);
		if(hurt)lastCut=level.getGameTime();
		return hurt;
	}
	@Override public void die(DamageSource source){
		letGo(false);dropClods();dropSpikes();roots.clear();rippleAge=-1;
		if(level() instanceof ServerLevel server){Feels.sound(server,position().add(0,2,0),"aura_serpent_death",1.6F,1);Guardians.reward(server,this,SerpentRules.TIER,bar,source);bar.removeAllPlayers();}
		super.die(source);
	}
	@Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return null;}
	@Override protected void actuallyHurt(ServerLevel level,DamageSource source,float damage){
		super.actuallyHurt(level,source,damage);
		if(random.nextInt(3)==0)Feels.sound(level,position().add(0,2,0),"aura_serpent_hurt",1,.9F+random.nextFloat()*.2F);
	}
	@Override public void remove(RemovalReason reason){caught=null;dropClods();dropSpikes();roots.clear();bar.removeAllPlayers();super.remove(reason);}
	@Override public void startSeenByPlayer(ServerPlayer player){super.startSeenByPlayer(player);}
	@Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);bar.removePlayer(player);}

	@Override protected void addAdditionalSaveData(ValueOutput out){
		super.addAdditionalSaveData(out);
		out.putBoolean("serpent_bound",bound);out.store("serpent_tree",BlockPos.CODEC,tree());out.store("serpent_landing",BlockPos.CODEC,landing());
		out.putFloat("serpent_rest",rest());out.putBoolean("serpent_awake",!SerpentRules.scripted(move()));out.putBoolean("serpent_dived",dived);out.putLong("serpent_ticked",lastTick);
	}
	@Override protected void readAdditionalSaveData(ValueInput in){
		super.readAdditionalSaveData(in);
		bound=in.getBooleanOr("serpent_bound",false);
		entityData.set(TREE,in.read("serpent_tree",BlockPos.CODEC).orElse(blockPosition()));entityData.set(LANDING,in.read("serpent_landing",BlockPos.CODEC).orElse(tree()));
		entityData.set(REST,in.getFloatOr("serpent_rest",getYRot()));
		dived=in.getBooleanOr("serpent_dived",false);lastTick=in.getLongOr("serpent_ticked",-1L);
		// Mid-fight it comes back ashore and on its feet, whatever it was doing when the chunk unloaded.
		if(in.getBooleanOr("serpent_awake",false)){pose(SerpentRules.WATCH,SerpentRules.GAP);lastCut=0;if(dived)bar.setColor(BossEvent.BossBarColor.RED);}else pose(SerpentRules.DORMANT,0);
	}
}
