package dev.wildercord.aura.world;

import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.cast.Vfx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** A blindfolded stone effigy. Physical, locked-direction attacks and a breakable forward guard. */
public final class Gravekeeper extends PathfinderMob implements Enemy {
	private static final EntityDataAccessor<Integer> MOVE=SynchedEntityData.defineId(Gravekeeper.class,EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN=SynchedEntityData.defineId(Gravekeeper.class,EntityDataSerializers.LONG);
	private final ServerBossEvent bar=new ServerBossEvent(getUUID(),getType().getDescription(),BossEvent.BossBarColor.WHITE,BossEvent.BossBarOverlay.NOTCHED_10);
	private BlockPos reliquary,home;
	private int left,sequence,quiet;
	private Vec3 direction=new Vec3(0,0,1);
	private boolean fought;
	public Gravekeeper(EntityType<? extends Gravekeeper> type,Level level){super(type,level);xpReward=35;setPersistenceRequired();}
	public static AttributeSupplier.Builder createAttributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,160).add(Attributes.ARMOR,8).add(Attributes.MOVEMENT_SPEED,.23).add(Attributes.ATTACK_DAMAGE,6).add(Attributes.FOLLOW_RANGE,28).add(Attributes.KNOCKBACK_RESISTANCE,.8);}
	public void bind(BlockPos altar,BlockPos arena){reliquary=altar.immutable();home=arena.immutable();}
	public BlockPos home(){return home;}
	public Vec3 attackDirection(){return direction;}
	public int move(){return entityData.get(MOVE);}
	public float elapsed(float partial){return (float)(level().getGameTime()-entityData.get(BEGAN))+partial;}
	@Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(MOVE,SwordTombRules.IDLE);b.define(BEGAN,0L);}
	@Override protected void registerGoals(){}
	private void pose(int move,int ticks){entityData.set(MOVE,move);entityData.set(BEGAN,level().getGameTime());left=ticks;getNavigation().stop();}
	private TombReliquaryEntity tomb(ServerLevel level){return reliquary!=null && level.hasChunkAt(reliquary) && level.getBlockEntity(reliquary) instanceof TombReliquaryEntity t && Objects.equals(t.guardian(),getUUID())?t:null;}
	@Override protected void customServerAiStep(ServerLevel level){
		super.customServerAiStep(level);if(!isAlive())return;
		if(home==null)home=blockPosition();
		if(distanceToSqr(Vec3.atBottomCenterOf(home))>SwordTombRules.ARENA_RADIUS*SwordTombRules.ARENA_RADIUS || getY()<home.getY()-3 || getY()>home.getY()+7){
			teleportTo(home.getX()+.5,home.getY(),home.getZ()+.5);setDeltaMovement(Vec3.ZERO);pose(SwordTombRules.RECOVER,40);
		}
		if(tickCount%10==0){
			var center=Vec3.atCenterOf(home);
			bar.setProgress(getHealth()/getMaxHealth());bar.setName(Component.translatable("boss.wildercord.gravekeeper.status",getType().getDescription(),Component.translatable("boss.wildercord.gravekeeper.move."+move())));
			for(var p:new ArrayList<>(bar.getPlayers()))if(p.level()!=level || !p.isAlive() || p.distanceToSqr(center)>28*28)bar.removePlayer(p);
			for(var p:level.players())if(p.isAlive() && !p.isSpectator() && p.distanceToSqr(center)<24*24)bar.addPlayer(p);
			var target=level.players().stream().filter(p->p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(center)<20*20 && hasLineOfSight(p)).min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
			setTarget(target);
			if(fought){var t=tomb(level);if(t!=null)for(var p:level.players())t.participant(p);}
		}
		var target=getTarget();
		if(target==null){getNavigation().stop();quiet++;if(quiet==201){setHealth(getMaxHealth());pose(SwordTombRules.IDLE,0);sequence=0;}return;}
		quiet=0;
		if(left>0){
			getNavigation().stop();setYRot((float)(Math.atan2(-direction.x,direction.z)*180/Math.PI));yBodyRot=getYRot();yHeadRot=getYRot();
			if(--left==0){
				int move=move();if(move==SwordTombRules.SWEEP || move==SwordTombRules.THRUST){strike(level,move);pose(SwordTombRules.RECOVER,SwordTombRules.RECOVER_TICKS);}
				else pose(SwordTombRules.IDLE,0);
			}
			return;
		}
		getLookControl().setLookAt(target,30,30);
		if(distanceTo(target)>6){getNavigation().moveTo(target,1);return;}
		direction=target.position().subtract(position()).multiply(1,0,1).normalize();if(direction.lengthSqr()<.5)direction=new Vec3(0,0,1);
		if(sequence++%3==2){pose(SwordTombRules.GUARD,SwordTombRules.GUARD_TICKS);Feels.sound(level,position(),"aura_tomb_brace",.8F,1);return;}
		int attack=sequence%3==1?SwordTombRules.SWEEP:SwordTombRules.THRUST;
		pose(attack,SwordTombRules.WINDUP);Feels.sound(level,position(),attack==SwordTombRules.SWEEP?"aura_tomb_sweep_ready":"aura_tomb_thrust_ready",1,1);
		// Scuff the threatened lanes with stone dust. These are physical lines, never spell seals.
		Vec3 side=new Vec3(-direction.z,0,direction.x);
		for(int i=1;i<=7;i++) {
			if(attack==SwordTombRules.SWEEP && i>5)break;
			double width=attack==SwordTombRules.THRUST?.75:Math.min(3,i*.8);
			for(int sign:new int[]{-1,1})Vfx.emit(level,new BlockParticleOption(ParticleTypes.BLOCK,Blocks.STONE.defaultBlockState()),position().add(direction.scale(i)).add(side.scale(sign*width)).add(0,.12,0),2,.04,.02);
		}
	}
	private void strike(ServerLevel level,int attack){
		Vec3 side=new Vec3(-direction.z,0,direction.x);
		AuraFx.trail(this,attack==SwordTombRules.SWEEP?AuraFxRules.Stroke.SWEEP:AuraFxRules.Stroke.THRUST,false,0xBDA879,4,1.1F);
		Feels.sound(level,position(),attack==SwordTombRules.SWEEP?"aura_tomb_sweep":"aura_tomb_thrust",1,1);
		for(var p:level.getEntitiesOfClass(ServerPlayer.class,getBoundingBox().inflate(8),e->e.isAlive() && !e.isCreative() && !e.isSpectator())){
			Vec3 delta=p.position().subtract(position());
			if(!SwordTombRules.hit(attack,delta.dot(direction),delta.dot(side),delta.y) || !hasLineOfSight(p))continue;
			if(p.hurtServer(level,level.damageSources().mobAttack(this),(float)SwordTombRules.damage(attack))){
				p.knockback(.35,-direction.x,-direction.z,level.damageSources().mobAttack(this),0);AuraFx.impact(this,p,0xBDA879,4,AuraFxRules.Weight.FULL);
			}
		}
		AuraFx.groundScar(level,position().add(direction.scale(attack==SwordTombRules.SWEEP?2:4)),attack==SwordTombRules.SWEEP?2.4:1,70,2);
	}
	@Override public boolean hurtServer(ServerLevel level,DamageSource source,float damage){
		if(source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_WALL))return false;
		boolean front=false,axe=false;
		var from=source.getDirectEntity();var origin=source instanceof dev.wildercord.cast.RelayDamageSource?source.getSourcePosition():from==null?null:from.position();if(origin!=null){var delta=origin.subtract(position()).multiply(1,0,1).normalize();front=delta.dot(direction)>.3;}
		if(source.getEntity() instanceof ServerPlayer p){
			axe=source.is(DamageTypes.PLAYER_ATTACK) && p.getMainHandItem().is(ItemTags.AXES);
			if(move()==SwordTombRules.GUARD && front && axe){pose(SwordTombRules.BROKEN,SwordTombRules.BROKEN_TICKS);Feels.sound(level,position(),"aura_tomb_break",1,1);}
		}
		float through=(float)(damage*SwordTombRules.multiplier(move(),front,axe));
		boolean hurt=super.hurtServer(level,source,through);
		if(hurt && source.getEntity() instanceof ServerPlayer p){fought=true;var t=tomb(level);if(t!=null)t.participant(p);}
		return hurt;
	}
	@Override public void die(DamageSource source){
		if(level() instanceof ServerLevel server){var t=tomb(server);if(t!=null){if(source.getEntity() instanceof ServerPlayer killer && !killer.isCreative() && !killer.isSpectator())fought=true;if(fought)for(var p:server.players())t.participant(p);t.defeated(getUUID());}Feels.sound(server,position(),"aura_tomb_fall",1,1);bar.removeAllPlayers();}
		super.die(source);
	}
	@Override public void remove(RemovalReason reason){bar.removeAllPlayers();super.remove(reason);}
	@Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(home!=null)out.store("tomb_home",BlockPos.CODEC,home);if(reliquary!=null)out.store("reliquary",BlockPos.CODEC,reliquary);out.putBoolean("fought",fought);}
	@Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);home=in.read("tomb_home",BlockPos.CODEC).orElse(null);reliquary=in.read("reliquary",BlockPos.CODEC).orElse(null);fought=in.getBooleanOr("fought",false);pose(SwordTombRules.RECOVER,40);}
}
