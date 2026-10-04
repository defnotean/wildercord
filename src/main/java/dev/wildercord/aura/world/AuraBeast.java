package dev.wildercord.aura.world;

import dev.wildercord.aura.AuraFx;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.cast.Vfx;
import dev.wildercord.config.Config;
import dev.wildercord.wildlife.Wildlife;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** Shared physiology and persistence; Stonehorn and Galeclaw author their own attack and ecological behavior. */
public abstract class AuraBeast extends PathfinderMob {
	private static final EntityDataAccessor<Integer> POSE=SynchedEntityData.defineId(AuraBeast.class,EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN=SynchedEntityData.defineId(AuraBeast.class,EntityDataSerializers.LONG);
	protected BlockPos home;
	protected Vec3 direction=new Vec3(0,0,1), landing=Vec3.ZERO;
	protected int left, calm, aggression;
	private long nextShed;
	private boolean spellHit;
	private int attackEpoch;
	private dev.wildercord.wildlife.HighlandShelterGoal shelter;
	private dev.wildercord.wildlife.WindreedForageGoal reedForage;
	private final Set<UUID> struck=new HashSet<>();
	protected AuraBeast(EntityType<? extends AuraBeast> type,Level level) { super(type,level); xpReward=9; }
	public abstract boolean gale();
	public int pose() { return entityData.get(POSE); }
	public float elapsed(float partial) { return level().getGameTime()-entityData.get(BEGAN)+partial; }
	public BlockPos home() { return home; }
	public Vec3 landing() { return landing; }
	public int calmTicks() { return calm; }
	@Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return dev.wildercord.content.WildercordSounds.kit(gale()?"aura_galeclaw_call":"aura_stonehorn_grumble"); }
	@Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) { return dev.wildercord.content.WildercordSounds.kit(gale()?"aura_galeclaw_hurt":"aura_stonehorn_hurt"); }
	@Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return dev.wildercord.content.WildercordSounds.kit(gale()?"aura_galeclaw_death":"aura_stonehorn_death"); }
	@Override public int getAmbientSoundInterval() { return gale()?220:300; }
	public static AttributeSupplier.Builder attributes(boolean gale) {
		return createMobAttributes().add(Attributes.MAX_HEALTH,gale?36:56).add(Attributes.MOVEMENT_SPEED,gale?.30:.20)
			.add(Attributes.ATTACK_DAMAGE,gale?5:7).add(Attributes.FOLLOW_RANGE,24).add(Attributes.KNOCKBACK_RESISTANCE,gale?.15:.65).add(Attributes.STEP_HEIGHT,1);
	}
	@Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(POSE,0); b.define(BEGAN,0L); }
	@Override protected void registerGoals() {
		goalSelector.addGoal(0,new FloatGoal(this));
		shelter=new dev.wildercord.wildlife.HighlandShelterGoal(this,
			() -> Config.get().auraWorld().auraBeasts() && level().getDifficulty()!=Difficulty.PEACEFUL && getTarget()==null && left==0 && (pose()==BeastRules.IDLE || pose()==BeastRules.REST),
			() -> dev.wildercord.wildlife.HighlandRules.wantsCover(gale(),level().getOverworldClockTime(),level().isRaining()),
			settled -> {if(settled && pose()!=BeastRules.REST)pose(BeastRules.REST,0);else if(!settled && pose()==BeastRules.REST)pose(BeastRules.IDLE,0);});
		goalSelector.addGoal(2,shelter);
		if(!gale()) {
			reedForage=new dev.wildercord.wildlife.WindreedForageGoal(this,
				() -> Config.get().auraWorld().auraBeasts() && level().getDifficulty()!=Difficulty.PEACEFUL && getTarget()==null && left==0 && (pose()==BeastRules.IDLE || pose()==BeastRules.FORAGE),
				feeding -> {if(left==0) {if(feeding && pose()!=BeastRules.FORAGE)pose(BeastRules.FORAGE,0);else if(!feeding && pose()==BeastRules.FORAGE)pose(BeastRules.IDLE,0);}});
			goalSelector.addGoal(3,reedForage);
		}
		goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,.7) {
			@Override public boolean canUse() { return pose()==BeastRules.IDLE && getTarget()==null && super.canUse(); }
			@Override public boolean canContinueToUse() { return pose()==BeastRules.IDLE && getTarget()==null && super.canContinueToUse(); }
		});
		goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8));
		goalSelector.addGoal(7,new RandomLookAroundGoal(this));
	}
	protected void pose(int id,int ticks) { attackEpoch++; entityData.set(POSE,id); entityData.set(BEGAN,level().getGameTime()); left=ticks; getNavigation().stop(); }
	protected void face(Vec3 delta) { direction=delta.multiply(1,0,1).normalize(); if(direction.lengthSqr()<.5) direction=new Vec3(0,0,1); lockFacing(); }
	protected void lockFacing() { setYRot((float)(Math.atan2(-direction.x,direction.z)*180/Math.PI)); yBodyRot=getYRot(); yHeadRot=getYRot(); }
	protected boolean valid(LivingEntity p) { return p!=null && p.isAlive() && !p.isRemoved() && p.level()==level() && (!(p instanceof Player pl) || !pl.isCreative() && !pl.isSpectator()); }
	protected void sound(String action) {
		var level=(ServerLevel)level();
		if(gale()) switch(action) {
			case "warn" -> Feels.sound(level,position(),"aura_galeclaw_warn",.8F,1);
			case "leap" -> Feels.sound(level,position(),"aura_galeclaw_leap",.8F,1);
			case "land" -> Feels.sound(level,position(),"aura_galeclaw_land",.8F,1);
			default -> throw new IllegalArgumentException("Unknown Galeclaw action: "+action);
		} else switch(action) {
			case "warn" -> Feels.sound(level,position(),"aura_stonehorn_warn",.8F,1);
			case "charge" -> Feels.sound(level,position(),"aura_stonehorn_charge",.8F,1);
			case "impact" -> Feels.sound(level,position(),"aura_stonehorn_impact",.8F,1);
			case "forage" -> Feels.sound(level,position(),"aura_stonehorn_forage",.8F,1);
			default -> throw new IllegalArgumentException("Unknown Stonehorn action: "+action);
		}
	}
	protected void dust(Vec3 at,int count) { Vfx.emit((ServerLevel)level(),new BlockParticleOption(ParticleTypes.BLOCK,Blocks.STONE.defaultBlockState()),at,count,.22,.05); }
	/** A whistle interrupts stalking, but cannot cancel an already committed attack or revenge after damage. */
	public boolean distract(Vec3 at) {
		if (!gale() || aggression>0 || pose()==BeastRules.WARN || pose()==BeastRules.LEAP) return false;
		calm=200; setTarget(null); pose(BeastRules.IDLE,0); getNavigation().moveTo(at.x,at.y,at.z,1); return true;
	}
	public boolean hurtBySpell(ServerLevel level,DamageSource source,float amount) {
		boolean previous=spellHit; spellHit=true;
		try { return hurtServer(level,source,amount); } finally { spellHit=previous; }
	}
	@Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount) {
		boolean magic=spellHit || source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC);
		if(magic) amount*=(float)BeastRules.spell(gale(),pose()==BeastRules.RECOVER);
		float before=getHealth();boolean hit=super.hurtServer(level,source,amount);
		if(hit && before>getHealth() && isAlive() && !isRemoved() && level()==level && source.getEntity() instanceof LivingEntity attacker && valid(attacker)) { aggression=200; calm=0; setTarget(attacker); }
		if(hit && isAlive() && !isRemoved() && level()==level && magic && tickCount%5==0) { dust(getBoundingBox().getCenter(),5); }
		return hit;
	}
	@Override protected InteractionResult mobInteract(Player p,InteractionHand hand) {
		if(!p.isAlive() || p.isRemoved() || p.isSpectator() || p.level()!=level())return InteractionResult.PASS;
		var stack=p.getItemInHand(hand);
		if(gale() ? !stack.is(Items.RABBIT) && !stack.is(Items.CHICKEN) : !stack.is(Items.WHEAT)) return super.mobInteract(p,hand);
		if(!isAlive() || isRemoved() || pose()==BeastRules.WARN || pose()==BeastRules.CHARGE || pose()==BeastRules.LEAP || aggression>0) return InteractionResult.PASS;
		if(level() instanceof ServerLevel server) {
			if(server.getGameTime()<nextShed) { p.sendOverlayMessage(Component.translatable("message.wildercord.aura_beast.rest")); return InteractionResult.SUCCESS; }
			if(!p.getAbilities().instabuild) stack.shrink(1);
			if(this instanceof Galeclaw runner) runner.ate();
			nextShed=server.getGameTime()+BeastRules.SHED_INTERVAL; calm=200; setTarget(null); pose(BeastRules.FORAGE,60);
			spawnAtLocation(server,new ItemStack(gale()?AuraBeasts.GALECLAW_PLUME:AuraBeasts.STONEHORN_PLATE)); sound(gale()?"land":"forage");
		}
		return InteractionResult.SUCCESS;
	}
	protected int attackEpoch() {return attackEpoch;}
	protected boolean attackActive(ServerLevel level,int epoch,int phase) {
		return isAlive() && !isRemoved() && level()==level && attackEpoch==epoch && pose()==phase
			&& (phase==BeastRules.CHARGE || phase==BeastRules.LEAP) && Config.get().auraWorld().auraBeasts()
			&& level.getDifficulty()!=Difficulty.PEACEFUL;
	}
	protected void hit(ServerLevel level,LivingEntity target,float damage,double knock) {hitOwned(level,target,damage,knock);}
	/** Return the exact admitted damage identity; no shared receipt or callback-mutable cached source. */
	protected DamageSource hitOwned(ServerLevel level,LivingEntity target,float damage,double knock) {
		int epoch=attackEpoch,phase=pose();
		if(!attackActive(level,epoch,phase) || !valid(target) || struck.size()>=BeastRules.VICTIMS || !struck.add(target.getUUID()))return null;
		var source=level.damageSources().mobAttack(this);float health=target.getHealth();boolean admitted=target.hurtServer(level,source,damage);
		if(admitted && target.getLastDamageSource()==source && attackActive(level,epoch,phase) && valid(target)) {
			target.knockback(knock,-direction.x,-direction.z,source,0);
			if(target.getLastDamageSource()==source && attackActive(level,epoch,phase) && valid(target))AuraFx.groundScar(level,target.position(),1.5,35,2);
		}
		return admitted && attackActive(level,epoch,phase) && target.level()==level && !target.isRemoved() && target.getHealth()<health ? source : null;
	}
	protected void beginAttack() { struck.clear();attackEpoch++; }
	@Override protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if(home==null) home=blockPosition();
		if(calm>0) calm--; if(aggression>0) aggression--;
		if(!Config.get().auraWorld().auraBeasts() || level.getDifficulty()==Difficulty.PEACEFUL) {
			setTarget(null); if(pose()!=BeastRules.IDLE) { setDeltaMovement(Vec3.ZERO); pose(BeastRules.IDLE,0); } return;
		}
		if(!valid(getTarget()) || getTarget()!=null && (getTarget().distanceToSqr(Vec3.atBottomCenterOf(home))>32*32 || distanceToSqr(getTarget())>28*28)) setTarget(null);
		if(getTarget()==null && shelter.running())return;
		if(getTarget()==null && reedForage!=null && reedForage.running()) {
			if(this instanceof Stonehorn grazer)grazer.noticeThreat(level);
			if(getTarget()==null)return;
		}
		if(getTarget()==null && pose()==BeastRules.IDLE && distanceToSqr(Vec3.atBottomCenterOf(home))>16*16 && tickCount%40==0)
			getNavigation().moveTo(home.getX()+.5,home.getY(),home.getZ()+.5,.8);
		act(level);
	}
	protected abstract void act(ServerLevel level);
	@Override protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); if(home!=null) out.store("beast_home",BlockPos.CODEC,home); out.putLong("next_shed",nextShed); out.putInt("calm",calm); }
	@Override protected void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); home=in.read("beast_home",BlockPos.CODEC).orElse(null); nextShed=in.getLongOr("next_shed",0); calm=Math.clamp(in.getIntOr("calm",0),0,200); aggression=0; pose(BeastRules.RECOVER,BeastRules.RECOVERY); }
}
