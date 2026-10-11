package dev.wildercord.aura.world;

import dev.wildercord.cast.feel.Feels;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import java.util.*;

/**
 * The Rootcoil Serpent ({@link SerpentRules}). Generation leaves it coiled round the grove's tree, bound to the trunk and facing
 * the herb. While it sleeps it holds still at the trunk's foot and cannot be hurt; the first player onto the island (or the first
 * blow) starts the uncoil, and when it lands the boss bar comes up. The grove going quiet sends it back up the trunk, healed.
 */
public final class RootcoilSerpent extends PathfinderMob implements Enemy {
	private static final EntityDataAccessor<Integer> MOVE=SynchedEntityData.defineId(RootcoilSerpent.class,EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN=SynchedEntityData.defineId(RootcoilSerpent.class,EntityDataSerializers.LONG);
	private static final EntityDataAccessor<BlockPos> TREE=SynchedEntityData.defineId(RootcoilSerpent.class,EntityDataSerializers.BLOCK_POS);
	private static final EntityDataAccessor<BlockPos> LANDING=SynchedEntityData.defineId(RootcoilSerpent.class,EntityDataSerializers.BLOCK_POS);
	private static final EntityDataAccessor<Float> REST=SynchedEntityData.defineId(RootcoilSerpent.class,EntityDataSerializers.FLOAT);
	/** Coiled round the trunk it spans the coils; on the ground, a reared head over a heap of body. */
	private static final EntityDimensions COILED=EntityDimensions.scalable(6.6F,8F),REARED=EntityDimensions.scalable(3F,3F).withEyeHeight(2.6F);
	private final ServerBossEvent bar=new ServerBossEvent(getUUID(),getType().getDescription(),BossEvent.BossBarColor.GREEN,BossEvent.BossBarOverlay.NOTCHED_10);
	private boolean bound;
	private int left,quiet;
	private long lastCut;

	public RootcoilSerpent(EntityType<? extends RootcoilSerpent> type,Level level){super(type,level);xpReward=80;setPersistenceRequired();}
	public static AttributeSupplier.Builder createAttributes(){
		return createMobAttributes().add(Attributes.MAX_HEALTH,SerpentRules.HEALTH).add(Attributes.ARMOR,SerpentRules.ARMOUR).add(Attributes.ARMOR_TOUGHNESS,4)
			.add(Attributes.MOVEMENT_SPEED,SerpentRules.SPEED).add(Attributes.ATTACK_DAMAGE,10).add(Attributes.FOLLOW_RANGE,32).add(Attributes.KNOCKBACK_RESISTANCE,1)
			.add(Attributes.STEP_HEIGHT,1.5).add(Attributes.WATER_MOVEMENT_EFFICIENCY,1);
	}

	/** Bind it to its tree: {@code tree} is the air above the ground at the trunk's foot, {@code rest} the yaw it watches the herb along. */
	public void bind(BlockPos tree,float rest){
		bound=true;entityData.set(TREE,tree.immutable());entityData.set(REST,rest);entityData.set(LANDING,tree.immutable());
		coil();
	}
	public int move(){return entityData.get(MOVE);}
	public float elapsed(float partial){return (float)(level().getGameTime()-entityData.get(BEGAN))+partial;}
	public BlockPos tree(){return entityData.get(TREE);}
	public BlockPos landing(){return entityData.get(LANDING);}
	public float rest(){return entityData.get(REST);}
	/** The trunk's centre at its foot. */
	public Vec3 trunk(){return Vec3.atBottomCenterOf(tree());}

	@Override protected void defineSynchedData(SynchedEntityData.Builder b){
		super.defineSynchedData(b);b.define(MOVE,SerpentRules.DORMANT);b.define(BEGAN,0L);b.define(TREE,BlockPos.ZERO);b.define(LANDING,BlockPos.ZERO);b.define(REST,0F);
	}
	@Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(MOVE.equals(key))refreshDimensions();}
	@Override protected EntityDimensions getDefaultDimensions(Pose pose){return SerpentRules.scripted(move())?COILED:REARED;}
	@Override protected void registerGoals(){}
	@Override public boolean isPushable(){return false;}
	@Override protected void doPush(Entity entity){}
	@Override public boolean canBreatheUnderwater(){return true;}
	@Override public boolean removeWhenFarAway(double distance){return false;}

	private void pose(int move,int ticks){
		int was=move();entityData.set(MOVE,move);entityData.set(BEGAN,level().getGameTime());left=ticks;getNavigation().stop();
		boolean held=SerpentRules.scripted(move);noPhysics=held;setNoGravity(held);
		if(was!=move)refreshDimensions();
	}
	/** Back round the trunk, whole again. */
	private void coil(){
		pose(SerpentRules.DORMANT,0);hold();setHealth(getMaxHealth());setTarget(null);bar.removeAllPlayers();quiet=0;
	}
	/** Pinned to the trunk's foot while it is coiled there (or climbing on or off it). */
	private void hold(){
		var t=trunk();setPos(t.x,t.y,t.z);setDeltaMovement(Vec3.ZERO);setYRot(rest());yBodyRot=rest();yHeadRot=rest();
	}

	@Override protected void customServerAiStep(ServerLevel level){
		super.customServerAiStep(level);if(!isAlive())return;
		if(!bound)bind(blockPosition(),getYRot());
		switch(move()){
			case SerpentRules.DORMANT->dormant(level);
			case SerpentRules.UNCOIL->{hold();if(--left<=0)land(level);else if(left==SerpentRules.UNCOIL_TICKS/2)Feels.sound(level,trunk().add(0,3,0),"aura_serpent_uncoil",1,1);}
			case SerpentRules.RECOIL->{hold();var p=wakeful(level);if(p!=null)wake(level,SerpentRules.UNCOIL_TICKS-left);else if(--left<=0)coil();}
			default->awake(level);
		}
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
		pose(SerpentRules.WATCH,0);snapTo(at.x,at.y,at.z,facing,0);yBodyRot=facing;yHeadRot=facing;setDeltaMovement(Vec3.ZERO);
		lastCut=level.getGameTime();quiet=0;
		Feels.sound(level,at.add(0,2.5,0),"aura_serpent_challenge",1.6F,1);
	}

	private void awake(ServerLevel level){
		var t=trunk();
		if(SerpentRules.leashed(getX()-t.x,getY()-t.y,getZ()-t.z)){
			var at=Vec3.atBottomCenterOf(landing());teleportTo(at.x,at.y,at.z);setDeltaMovement(Vec3.ZERO);getNavigation().stop();
		}
		if(isInWater() && getFluidHeight(net.minecraft.tags.FluidTags.WATER)>1.2)getJumpControl().jump();
		long now=level.getGameTime();float heal=SerpentRules.regen(getMaxHealth(),now-lastCut);if(heal>0 && getHealth()<getMaxHealth())heal(heal);
		if(tickCount%10==0){
			bar.setProgress(getHealth()/getMaxHealth());
			for(var p:new ArrayList<>(bar.getPlayers()))if(p.level()!=level || !p.isAlive() || p.distanceToSqr(t)>SerpentRules.BAR_DROP*SerpentRules.BAR_DROP)bar.removePlayer(p);
			for(var p:level.players())if(p.isAlive() && !p.isSpectator() && p.distanceToSqr(t)<SerpentRules.BAR_SHOW*SerpentRules.BAR_SHOW)bar.addPlayer(p);
			setTarget(level.players().stream().filter(p->p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(t)<SerpentRules.ARENA*SerpentRules.ARENA && Math.abs(p.getY()-t.y)<12)
				.min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null));
		}
		var target=getTarget();
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
		if(distanceTo(target)>SerpentRules.CLOSE){
			if(!getNavigation().moveTo(target,1))getMoveControl().setWantedPosition(target.getX(),target.getY(),target.getZ(),1);
		}else getNavigation().stop();
	}

	@Override public boolean hurtServer(ServerLevel level,DamageSource source,float damage){
		if(source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.CRAMMING))return false;
		if(SerpentRules.scripted(move()) && !source.is(DamageTypes.GENERIC_KILL)){
			// Coiled, its scales turn anything aside; but a blow, or a shot, wakes it.
			if(move()==SerpentRules.DORMANT && Guardians.enabled() && source.getEntity() instanceof ServerPlayer p && !p.isCreative())wake(level,0);
			return false;
		}
		boolean hurt=super.hurtServer(level,source,damage);
		if(hurt)lastCut=level.getGameTime();
		return hurt;
	}
	@Override public void die(DamageSource source){
		if(level() instanceof ServerLevel server){Feels.sound(server,position().add(0,2,0),"aura_serpent_death",1.6F,1);bar.removeAllPlayers();}
		super.die(source);
	}
	@Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return null;}
	@Override protected void actuallyHurt(ServerLevel level,DamageSource source,float damage){
		super.actuallyHurt(level,source,damage);
		if(random.nextInt(3)==0)Feels.sound(level,position().add(0,2,0),"aura_serpent_hurt",1,.9F+random.nextFloat()*.2F);
	}
	@Override public void remove(RemovalReason reason){bar.removeAllPlayers();super.remove(reason);}
	@Override public void startSeenByPlayer(ServerPlayer player){super.startSeenByPlayer(player);}
	@Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);bar.removePlayer(player);}

	@Override protected void addAdditionalSaveData(ValueOutput out){
		super.addAdditionalSaveData(out);
		out.putBoolean("serpent_bound",bound);out.store("serpent_tree",BlockPos.CODEC,tree());out.store("serpent_landing",BlockPos.CODEC,landing());
		out.putFloat("serpent_rest",rest());out.putBoolean("serpent_awake",!SerpentRules.scripted(move()));
	}
	@Override protected void readAdditionalSaveData(ValueInput in){
		super.readAdditionalSaveData(in);
		bound=in.getBooleanOr("serpent_bound",false);
		entityData.set(TREE,in.read("serpent_tree",BlockPos.CODEC).orElse(blockPosition()));entityData.set(LANDING,in.read("serpent_landing",BlockPos.CODEC).orElse(tree()));
		entityData.set(REST,in.getFloatOr("serpent_rest",getYRot()));
		if(in.getBooleanOr("serpent_awake",false)){pose(SerpentRules.WATCH,0);lastCut=0;}else pose(SerpentRules.DORMANT,0);
	}
}
