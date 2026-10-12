package dev.wildercord.aura.world;

import dev.wildercord.cast.feel.Feels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
 * <p>Awake it fights like a lighter Master: quick bites in flurries, a lunge at anyone who backs off, a grab that holds and
 * squeezes until its prey struggles free, and a slip aside from blows it sees coming. Its scales turn much of a blow from the
 * front; each attack leaves it spent for a moment with its belly bare. At half health it takes to the water ring and bursts out
 * of it three times before it comes back ashore, quicker than before. Anyone keeping their distance gets the tail: it comes over
 * and down in front of it and flings a fan of the island at them, clods of earth that hit through armour.
 */
public final class RootcoilSerpent extends PathfinderMob implements Enemy {
	private static final EntityDataAccessor<Integer> MOVE=SynchedEntityData.defineId(RootcoilSerpent.class,EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN=SynchedEntityData.defineId(RootcoilSerpent.class,EntityDataSerializers.LONG);
	private static final EntityDataAccessor<BlockPos> TREE=SynchedEntityData.defineId(RootcoilSerpent.class,EntityDataSerializers.BLOCK_POS);
	private static final EntityDataAccessor<BlockPos> LANDING=SynchedEntityData.defineId(RootcoilSerpent.class,EntityDataSerializers.BLOCK_POS);
	private static final EntityDataAccessor<Float> REST=SynchedEntityData.defineId(RootcoilSerpent.class,EntityDataSerializers.FLOAT);
	/** The current move's tell in ticks, so the client knows when the blow lands. */
	private static final EntityDataAccessor<Integer> TELL=SynchedEntityData.defineId(RootcoilSerpent.class,EntityDataSerializers.INT);
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
	private int flurry,bites,chased,struggles,bursts,circle;
	private long lastSlip=-999,lastGrab=-999,lastSlam=-999;
	private boolean dived,water,gripping;
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

	public RootcoilSerpent(EntityType<? extends RootcoilSerpent> type,Level level){super(type,level);xpReward=80;setPersistenceRequired();}
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
	public float elapsed(float partial){return (float)(level().getGameTime()-entityData.get(BEGAN))+partial;}
	public BlockPos tree(){return entityData.get(TREE);}
	public BlockPos landing(){return entityData.get(LANDING);}
	public float rest(){return entityData.get(REST);}
	/** The trunk's centre at its foot. */
	public Vec3 trunk(){return Vec3.atBottomCenterOf(tree());}

	@Override protected void defineSynchedData(SynchedEntityData.Builder b){
		super.defineSynchedData(b);b.define(MOVE,SerpentRules.DORMANT);b.define(BEGAN,0L);b.define(TREE,BlockPos.ZERO);b.define(LANDING,BlockPos.ZERO);b.define(REST,0F);b.define(TELL,0);
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
		boolean held=SerpentRules.scripted(move) || move==SerpentRules.SUBMERGED || move==SerpentRules.BURST;noPhysics=held;setNoGravity(held);
		if(was!=move)refreshDimensions();
		if(!SerpentRules.scripted(move))bar.setName(Component.translatable("boss.wildercord.rootcoil_serpent.status",getType().getDescription(),
			Component.translatable("boss.wildercord.rootcoil_serpent.move."+SerpentRules.key(move))));
	}
	/** Ticks since the current move began: 1 on its first tick. */
	private long since(){return level().getGameTime()-entityData.get(BEGAN);}
	/** Back round the trunk, whole again. */
	private void coil(){
		letGo(false);dropClods();
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
		lastCut=level.getGameTime();quiet=0;
		Feels.sound(level,at.add(0,2.5,0),"aura_serpent_challenge",1.6F,1);
	}

	private void awake(ServerLevel level){
		var t=trunk();int move=move();
		boolean placed=move==SerpentRules.SUBMERGED || move==SerpentRules.BURST || move==SerpentRules.DIVE;
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
			case SerpentRules.CONSTRICT->constrict(level);
			case SerpentRules.SQUEEZE->squeeze(level);
			case SerpentRules.SIDESTEP->{face(aim);move(MoverType.SELF,slide);setDeltaMovement(Vec3.ZERO);if(--left<=0)pose(SerpentRules.WATCH,2);}
			case SerpentRules.RECOVER->{getNavigation().stop();if(--left<=0)recovered(level,target);}
			case SerpentRules.DIVE->dive(level);
			case SerpentRules.SUBMERGED->submerged(level,target);
			case SerpentRules.BURST->burst(level);
			case SerpentRules.TAIL_SLAM->slam(level,target);
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
			dived=true;water=true;bursts=0;bar.setColor(BossEvent.BossBarColor.BLUE);diveIn(level);return;
		}
		double d=distanceTo(target);
		if(left>0)left--;
		int pick=-1;
		if(left<=0){
			long now=level.getGameTime();
			pick=SerpentRules.choose(d,random.nextDouble(),now-lastGrab>=SerpentRules.CONSTRICT_REST,now-lastSlam>=SerpentRules.TAIL_REST);
			// A long chase ends in a lunge from further out.
			if(pick<0 && chased>=60 && d<=SerpentRules.LUNGE_REACH+5)pick=SerpentRules.LUNGE;
		}
		if(pick>=0){
			chased=0;aim=yawTo(target.position());
			switch(pick){
				case SerpentRules.STRIKE->{flurry=SerpentRules.flurry(random.nextDouble(),dived);bites=0;strikeAt(SerpentRules.STRIKE_TELL);}
				case SerpentRules.TAIL_SLAM->{lastSlam=level.getGameTime();pose(SerpentRules.TAIL_SLAM,0,SerpentRules.TAIL_TELL);Feels.sound(level,position().add(0,1,0),"aura_serpent_hiss",1.3F,.7F);}
				case SerpentRules.LUNGE->{struck.clear();pose(SerpentRules.LUNGE,0,SerpentRules.LUNGE_TELL);Feels.sound(level,mouth(),"aura_serpent_hiss",1.2F,1.3F);}
				default->{lastGrab=level.getGameTime();pose(SerpentRules.CONSTRICT,0,SerpentRules.CONSTRICT_TELL);Feels.sound(level,position().add(0,1,0),"aura_serpent_constrict",1.2F,1.1F);}
			}
			return;
		}
		if(d>SerpentRules.CLOSE){
			chased++;
			if(!getNavigation().moveTo(target,1))getMoveControl().setWantedPosition(target.getX(),target.getY(),target.getZ(),1);
		}else{chased=0;getNavigation().stop();}
	}

	/** A bite drawn back for {@code tell} ticks along the locked aim. */
	private void strikeAt(int tell){pose(SerpentRules.STRIKE,0,tell);}

	private void strike(ServerLevel level,LivingEntity target){
		face(aim);long el=since();int tell=tell();
		if(el==tell){
			Feels.sound(level,mouth(),"aura_serpent_strike",1.3F,.9F+random.nextFloat()*.2F);
			for(var p:prey(level))if(reaches(SerpentRules.STRIKE,p))hit(level,p,SerpentRules.STRIKE_DAMAGE,.5);
		}
		if(el<tell+SerpentRules.STRIKE_HOLD)return;
		bites++;
		// The flurry goes on while its prey is still in reach, each bite re-aimed.
		if(bites<flurry && target!=null && distanceTo(target)<=SerpentRules.CLOSE+1){aim=yawTo(target.position());strikeAt(SerpentRules.STRIKE_CHAIN_TELL);}
		else spent(SerpentRules.STRIKE_RECOVER);
	}

	/** It tracks its prey through the start of the tell, locks its aim for the last four ticks, then throws itself along it. */
	private void lunge(ServerLevel level,LivingEntity target){
		long el=since();
		if(el<=SerpentRules.LUNGE_TELL-4 && target!=null)aim=yawTo(target.position());
		face(aim);
		if(el<=SerpentRules.LUNGE_TELL){setDeltaMovement(Vec3.ZERO);return;}
		if(el==SerpentRules.LUNGE_TELL+1)Feels.sound(level,position().add(0,1,0),"aura_serpent_lunge",1.4F,1);
		move(MoverType.SELF,ahead(aim).scale(SerpentRules.LUNGE_REACH/SerpentRules.LUNGE_TICKS));setDeltaMovement(Vec3.ZERO);
		for(var p:prey(level))if(!struck.contains(p.getUUID()) && reaches(SerpentRules.LUNGE,p)){struck.add(p.getUUID());hit(level,p,SerpentRules.LUNGE_DAMAGE,.9);}
		if(el>=SerpentRules.LUNGE_TELL+SerpentRules.LUNGE_TICKS)spent(SerpentRules.LUNGE_RECOVER);
	}

	/**
	 * The tail slam. Through the tell it tracks its prey, locking its aim for the last three ticks while the tail comes over; then the
	 * tail comes down {@link SerpentRules#SLAM_AHEAD} in front of it, jarring anyone right there, and the ground it struck is flung
	 * along the aim in a fan.
	 */
	private void slam(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell-3 && target!=null)aim=yawTo(target.position());
		face(aim);setDeltaMovement(Vec3.ZERO);
		if(el<tell){
			if(el%2==0)level.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM,getX(),getY()+3,getZ(),3,1.5,1,1.5,0);
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
				p.push(0,.45,0);p.needsSync=true;
			}
			pelted.clear();
			for(int i=0;i<SerpentRules.DEBRIS;i++){
				float yaw=aim+(float)((i-(SerpentRules.DEBRIS-1)/2.0)*SerpentRules.DEBRIS_FAN);
				var from=down.add((random.nextDouble()-.5)*.8,SerpentRules.DEBRIS_FROM,(random.nextDouble()-.5)*.8);
				// Each clod is torn from the ground under where it starts: a slam on moss throws moss, one on stone throws stone.
				throwClod(level,from,ahead(yaw).scale(SerpentRules.DEBRIS_SPEED).add(0,SerpentRules.DEBRIS_LIFT,0),ground(level,from));
			}
		}
		if(el>=tell+3)spent(SerpentRules.TAIL_RECOVER);
	}

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

	/** Its coils swing open, and close on whoever is nearest in front of it. */
	private void constrict(ServerLevel level){
		face(aim);
		if(since()<SerpentRules.CONSTRICT_TELL)return;
		var p=prey(level).stream().filter(q->reaches(SerpentRules.CONSTRICT,q)).min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
		if(p==null){spent(SerpentRules.CONSTRICT_RECOVER);return;}
		caught=p;struggles=0;heldDamage=0;
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
			if(bursts<SerpentRules.BURSTS && target!=null){diveIn(level);return;}
			// Three bursts and it comes ashore for good, wounded and quicker.
			water=false;bar.setColor(BossEvent.BossBarColor.RED);
			if(target!=null)Feels.sound(level,mouth(),"aura_serpent_challenge",1.4F,1.15F);
		}
		pose(SerpentRules.WATCH,gap());
	}

	/** Ticks before its next blow. */
	private int gap(){return (dived?SerpentRules.GAP_WOUNDED:SerpentRules.GAP)+random.nextInt(3);}

	/** It rears and hisses, then slides off the island into the water ring at the nearest point of its path. */
	private void diveIn(ServerLevel level){
		var t=trunk();ring=Math.atan2(getZ()-t.z,getX()-t.x);circle=random.nextBoolean()?1:-1;from=position();
		aim=yawTo(ringPoint());
		pose(SerpentRules.DIVE,SerpentRules.DIVE_TICKS);
		Feels.sound(level,mouth(),"aura_serpent_dive",1.6F,1);
	}

	private void dive(ServerLevel level){
		face(aim);setDeltaMovement(Vec3.ZERO);
		long el=since();int half=SerpentRules.DIVE_TICKS/2;
		if(el<=half)return;
		noPhysics=true;setNoGravity(true);
		double s=SerpentSpine.smooth((el-half)/(double)half);var into=ringPoint();
		setPos(from.x+(into.x-from.x)*s,from.y+(into.y-from.y)*s+Math.sin(Math.PI*s)*1.2,from.z+(into.z-from.z)*s);
		if(el==SerpentRules.DIVE_TICKS-2)splash(level,into,24);
		if(el>=SerpentRules.DIVE_TICKS)pose(SerpentRules.SUBMERGED,SerpentRules.CIRCLE_TICKS+random.nextInt(21)-10);
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
	/** A blow landing on {@code p}, through part of its armour, knocking it {@code shove} along the aim. */
	private void hit(ServerLevel level,LivingEntity p,double damage,double shove){
		if(GuardianBite.land(level,this,p,(float)damage,SerpentRules.PIERCE) && shove>0){
			var f=ahead(aim);p.knockback(shove,-f.x,-f.z,level.damageSources().mobAttack(this),0);p.needsSync=true;
		}
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
		letGo(false);dropClods();
		if(level() instanceof ServerLevel server){Feels.sound(server,position().add(0,2,0),"aura_serpent_death",1.6F,1);bar.removeAllPlayers();}
		super.die(source);
	}
	@Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return null;}
	@Override protected void actuallyHurt(ServerLevel level,DamageSource source,float damage){
		super.actuallyHurt(level,source,damage);
		if(random.nextInt(3)==0)Feels.sound(level,position().add(0,2,0),"aura_serpent_hurt",1,.9F+random.nextFloat()*.2F);
	}
	@Override public void remove(RemovalReason reason){caught=null;dropClods();bar.removeAllPlayers();super.remove(reason);}
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
