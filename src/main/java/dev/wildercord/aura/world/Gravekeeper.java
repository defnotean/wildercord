package dev.wildercord.aura.world;

import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.cast.MoveDeck;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.cast.Vfx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.ItemTags;
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

/**
 * A blindfolded stone effigy keeping the Marchkeepers' buried blades. It opens with the drill it was taught, sweep, thrust and
 * guard, then fights as it likes and never throws the same blow twice running: the blade brought round low at whoever gets
 * behind it, a stillness while it listens and a leap on whatever it hears, the buried hands called up beneath anyone who stands
 * about, and a charge with the blade held crosswise. Half broken, it kneels for its rite and fights on roused.
 */
public final class Gravekeeper extends PathfinderMob implements Enemy {
	private static final EntityDataAccessor<Integer> MOVE=SynchedEntityData.defineId(Gravekeeper.class,EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> TELL=SynchedEntityData.defineId(Gravekeeper.class,EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN=SynchedEntityData.defineId(Gravekeeper.class,EntityDataSerializers.LONG);
	private static final DustParticleOptions BONE=new DustParticleOptions(0xBDA879,1.4F),GRAVE=new DustParticleOptions(0x5E5A52,1.6F),SOUL=new DustParticleOptions(0x7FD8E0,1.2F);
	private final ServerBossEvent bar=new ServerBossEvent(getUUID(),getType().getDescription(),BossEvent.BossBarColor.WHITE,BossEvent.BossBarOverlay.NOTCHED_10);
	private final MoveDeck deck=new MoveDeck(SwordTombRules.MEMORY);
	private final Map<UUID,Vec3> steps=new HashMap<>();
	private final Map<UUID,Long> grasped=new HashMap<>();
	private final List<Grasp> grasps=new ArrayList<>();
	private final Set<UUID> struck=new HashSet<>();
	/** A buried hand about to come up through the ground at a spot, and which wave of them it rose with. */
	private record Grasp(Vec3 at,long when,long wave){}
	private BlockPos reliquary,home;
	private int left,sequence,quiet,rest;
	private long waves;
	private Vec3 direction=new Vec3(0,0,1),leapFrom=Vec3.ZERO,leapTo=Vec3.ZERO,charge=Vec3.ZERO;
	private UUID quarry;
	private boolean fought,roused;
	public Gravekeeper(EntityType<? extends Gravekeeper> type,Level level){super(type,level);xpReward=35;setPersistenceRequired();}
	public static AttributeSupplier.Builder createAttributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,160).add(Attributes.ARMOR,8).add(Attributes.MOVEMENT_SPEED,.23).add(Attributes.ATTACK_DAMAGE,6).add(Attributes.FOLLOW_RANGE,28).add(Attributes.KNOCKBACK_RESISTANCE,.8);}
	public void bind(BlockPos altar,BlockPos arena){reliquary=altar.immutable();home=arena.immutable();}
	public BlockPos home(){return home;}
	public Vec3 attackDirection(){return direction;}
	public int move(){return entityData.get(MOVE);}
	/** How long the windup of the sweep or thrust under way was given. */
	public int tell(){return entityData.get(TELL);}
	public boolean roused(){return roused;}
	public float elapsed(float partial){return (float)(level().getGameTime()-entityData.get(BEGAN))+partial;}
	@Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(MOVE,SwordTombRules.IDLE);b.define(TELL,SwordTombRules.WINDUP);b.define(BEGAN,0L);}
	@Override protected void registerGoals(){}
	private void pose(int move,int ticks){entityData.set(MOVE,move);entityData.set(BEGAN,level().getGameTime());left=ticks;getNavigation().stop();}
	private long since(){return level().getGameTime()-entityData.get(BEGAN);}
	private void settle(){pose(SwordTombRules.IDLE,0);rest=roused?SwordTombRules.ROUSED_REST:SwordTombRules.REST;}
	private TombReliquaryEntity tomb(ServerLevel level){return reliquary!=null && level.hasChunkAt(reliquary) && level.getBlockEntity(reliquary) instanceof TombReliquaryEntity t && Objects.equals(t.guardian(),getUUID())?t:null;}
	private void reset(){
		setHealth(getMaxHealth());pose(SwordTombRules.IDLE,0);sequence=0;rest=0;roused=false;deck.clear();grasps.clear();grasped.clear();steps.clear();
		bar.setColor(BossEvent.BossBarColor.WHITE);
	}
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
		grasp(level);
		var target=getTarget();
		if(target==null){quiet++;if(quiet==201)reset();}else quiet=0;
		if(left>0)left--;
		switch(move()){
			case SwordTombRules.SWEEP,SwordTombRules.THRUST->blade(level,target);
			case SwordTombRules.HEEL->heel(level);
			case SwordTombRules.LISTEN->listen(level,target);
			case SwordTombRules.VERDICT->verdict(level);
			case SwordTombRules.HANDS->hands(level);
			case SwordTombRules.RAMPART->rampart(level,target);
			case SwordTombRules.RITE->rite(level);
			case SwordTombRules.IDLE->idle(level,target);
			default->{getNavigation().stop();face(direction);if(left==0)settle();}
		}
	}
	/** Between blows: a moment's pause, closing in, then the next blow, chosen. */
	private void idle(ServerLevel level,LivingEntity target){
		if(target==null){getNavigation().stop();return;}
		getLookControl().setLookAt(target,30,30);
		double d=distanceTo(target);
		if(rest>0){rest--;if(d>3)getNavigation().moveTo(target,1);else getNavigation().stop();return;}
		if(!roused && getHealth()<=getMaxHealth()*.5F){begin(level,SwordTombRules.RITE,target);return;}
		if(sequence<SwordTombRules.DRILL.length){if(d>6){getNavigation().moveTo(target,1);return;}begin(level,SwordTombRules.DRILL[sequence++],target);return;}
		var flat=target.position().subtract(position()).multiply(1,0,1);
		boolean behind=flat.lengthSqr()>1e-4 && flat.normalize().dot(ahead(getYRot()))<-.3;
		int move=SwordTombRules.choose(deck,d,behind,random.nextDouble());
		if(move<0){getNavigation().moveTo(target,1);return;}
		begin(level,move,target);
	}
	private void begin(ServerLevel level,int move,LivingEntity target){
		if(move!=SwordTombRules.RITE)deck.played(move);
		struck.clear();
		var flat=target.position().subtract(position()).multiply(1,0,1);
		if(move==SwordTombRules.HEEL)direction=ahead(getYRot());
		else if(flat.lengthSqr()>1e-4)direction=flat.normalize();
		face(direction);
		switch(move){
			case SwordTombRules.SWEEP,SwordTombRules.THRUST->windup(level,move,roused?SwordTombRules.ROUSED_WINDUP:SwordTombRules.WINDUP);
			case SwordTombRules.GUARD->{pose(move,SwordTombRules.GUARD_TICKS);Feels.sound(level,position(),"aura_tomb_brace",.8F,1);}
			case SwordTombRules.HEEL->{
				pose(move,SwordTombRules.HEEL_TELL+SwordTombRules.HEEL_SPIN);
				sound(level,position(),SoundEvents.GRINDSTONE_USE,1.4F,.6F);
			}
			case SwordTombRules.LISTEN->{
				pose(move,SwordTombRules.LISTEN_TELL+SwordTombRules.LISTEN_TICKS);steps.clear();
				sound(level,eyes(),SoundEvents.SCULK_CLICKING,1.6F,.6F);
			}
			case SwordTombRules.HANDS->{
				pose(move,SwordTombRules.HANDS_TELL+(waves()-1)*SwordTombRules.HANDS_EVERY+SwordTombRules.HANDS_RISE+4);
				sound(level,position(),SoundEvents.SOUL_ESCAPE.value(),2F,.5F);
			}
			case SwordTombRules.RAMPART->{
				pose(move,SwordTombRules.RAMPART_TELL+SwordTombRules.RAMPART_TICKS);
				sound(level,position(),SoundEvents.ARMOR_EQUIP_NETHERITE.value(),1.6F,.6F);
			}
			case SwordTombRules.RITE->{
				pose(move,SwordTombRules.RITE_TICKS);
				Feels.sound(level,position(),"aura_tomb_awake",1.4F,.8F);
			}
			default->pose(SwordTombRules.IDLE,0);
		}
	}
	/** The old sweep or thrust: the blade drawn back and the lane it will take scuffed in stone dust. */
	private void windup(ServerLevel level,int attack,int ticks){
		pose(attack,ticks);entityData.set(TELL,ticks);
		Feels.sound(level,position(),attack==SwordTombRules.SWEEP?"aura_tomb_sweep_ready":"aura_tomb_thrust_ready",1,1);
		// Scuff the threatened lanes with stone dust. These are physical lines, never spell seals.
		Vec3 side=new Vec3(-direction.z,0,direction.x);
		for(int i=1;i<=7;i++) {
			if(attack==SwordTombRules.SWEEP && i>5)break;
			double width=attack==SwordTombRules.THRUST?.75:Math.min(3,i*.8);
			for(int sign:new int[]{-1,1})Vfx.emit(level,new BlockParticleOption(ParticleTypes.BLOCK,Blocks.STONE.defaultBlockState()),position().add(direction.scale(i)).add(side.scale(sign*width)).add(0,.12,0),2,.04,.02);
		}
	}
	/** The sweep or thrust drawn back and then let go; roused, a sweep runs straight on into a thrust. */
	private void blade(ServerLevel level,LivingEntity target){
		getNavigation().stop();face(direction);
		if(left>0){
			if(left%8==0)level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.STONE.defaultBlockState()),getX(),getY()+2.4,getZ(),3,.4,.4,.4,.02);
			return;
		}
		int move=move();strike(level,move);
		int next=roused && target!=null?SwordTombRules.chain(move,distanceTo(target)):-1;
		if(next<0){pose(SwordTombRules.RECOVER,SwordTombRules.RECOVER_TICKS);return;}
		var flat=target.position().subtract(position()).multiply(1,0,1);if(flat.lengthSqr()>1e-4)direction=flat.normalize();
		face(direction);deck.played(next);windup(level,next,SwordTombRules.CHAIN_WINDUP);
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
	/**
	 * The heel-turn: the blade trailed low behind it, grinding sparks off the stone and its reach marked round it; then brought round
	 * in one full turn at ankle height. Whoever is off the ground as it passes goes clear.
	 */
	private void heel(ServerLevel level){
		getNavigation().stop();face(direction);
		long s=since();
		if(s<=SwordTombRules.HEEL_TELL){
			var back=position().subtract(direction.scale(2.2));
			level.sendParticles(ParticleTypes.CRIT,back.x,back.y+.2,back.z,3,.3,.05,.3,.15);
			if(s%3==0)sound(level,back,SoundEvents.GRINDSTONE_USE,1.2F,.5F+s*.03F);
			if(s%4==0)ring(level,position(),SwordTombRules.HEEL_RADIUS,BONE,28);
			return;
		}
		long t=s-SwordTombRules.HEEL_TELL;
		double swept=Math.min(360,360.0*t/SwordTombRules.HEEL_SPIN);
		if(t==1){sound(level,position(),SoundEvents.PLAYER_ATTACK_SWEEP,2F,.5F);Feels.sound(level,position(),"aura_tomb_sweep",1,.8F);}
		for(double r=1.5;r<=SwordTombRules.HEEL_RADIUS;r+=.9){
			var at=position().add(ahead((float)(getYRot()+180+swept)).scale(r));
			level.sendParticles(ParticleTypes.CRIT,at.x,at.y+.3,at.z,1,0,0,0,0);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,at)),at.x,at.y+.15,at.z,1,.1,0,.1,.05);
		}
		for(var p:prey(level)){
			if(struck.contains(p.getUUID()) || !SwordTombRules.passed(yawTo(p.position())-getYRot(),swept))continue;
			struck.add(p.getUUID());
			var d=p.position().subtract(position());
			if(SwordTombRules.heel(d.x,d.z,d.y))hit(level,p,SwordTombRules.damage(SwordTombRules.HEEL),.9,position(),.15);
		}
		if(left==0){pose(SwordTombRules.RECOVER,SwordTombRules.HEEL_RECOVER);AuraFx.groundScar(level,position(),SwordTombRules.HEEL_RADIUS-.5,70,2);}
	}
	/**
	 * Stood stock still with the blade point lowered, head turning, a hand raised to the blindfold, a soft clicking going out from it
	 * in rings: listening. Any footfall not crept, any hop, any blow is heard; nothing heard, and it is left at a loss.
	 */
	private void listen(ServerLevel level,LivingEntity target){
		getNavigation().stop();face(direction);
		long s=since();
		if(s%2==0){double r=1+(s%16)*.6;ring(level,position(),r,ParticleTypes.SCULK_CHARGE_POP,(int)(8+r*3));}
		if(s%8==0)sound(level,eyes(),SoundEvents.SCULK_CLICKING,1.4F,.6F+random.nextFloat()*.2F);
		for(var p:prey(level)){
			var at=p.position();var was=steps.put(p.getUUID(),at);
			if(was==null || s<SwordTombRules.LISTEN_TELL+SwordTombRules.LISTEN_GRACE)continue;
			var step=at.subtract(was);
			if(SwordTombRules.heard(Math.hypot(step.x,step.z),step.y,p.isShiftKeyDown())){hear(level,p);return;}
		}
		if(left==0){pose(SwordTombRules.LOST,SwordTombRules.LOST_TICKS);sound(level,eyes(),SoundEvents.SCULK_CLICKING_STOP,1.6F,.5F);}
	}
	/** It heard someone: its head snaps round to them and it drops into a crouch to leap. */
	private void hear(ServerLevel level,LivingEntity who){
		quarry=who.getUUID();leapFrom=position();leapTo=clamp(who.position());
		var flat=leapTo.subtract(position()).multiply(1,0,1);if(flat.lengthSqr()>1e-4)direction=flat.normalize();face(direction);
		struck.clear();pose(SwordTombRules.VERDICT,SwordTombRules.VERDICT_CROUCH+SwordTombRules.VERDICT_LEAP+SwordTombRules.VERDICT_STUCK);
		sound(level,eyes(),SoundEvents.SCULK_CLICKING_STOP,2F,1.4F);Feels.sound(level,position(),"aura_tomb_brace",1.2F,1.3F);
		level.sendParticles(ParticleTypes.SCULK_CHARGE_POP,who.getX(),who.getY()+1,who.getZ(),12,.4,.6,.4,.05);
	}
	/**
	 * The verdict: crouched with the blade raised two-handed overhead and the spot it will come down on marked, following its quarry
	 * until just before it goes; a leap, the chop, and the blade stuck fast in the ground while it tugs at it.
	 */
	private void verdict(ServerLevel level){
		getNavigation().stop();
		long s=since();int crouch=SwordTombRules.VERDICT_CROUCH,leap=SwordTombRules.VERDICT_LEAP;
		if(s<crouch){
			if(s<crouch-3 && quarry!=null && level.getEntity(quarry) instanceof LivingEntity who && who.isAlive())leapTo=clamp(who.position());
			var flat=leapTo.subtract(position()).multiply(1,0,1);if(flat.lengthSqr()>1e-4)direction=flat.normalize();face(direction);
			ring(level,leapTo,SwordTombRules.VERDICT_RADIUS,BONE,18);
			level.sendParticles(ParticleTypes.SCULK_CHARGE_POP,leapTo.x,leapTo.y+.2,leapTo.z,2,.3,0,.3,0);
			leapFrom=position();
			return;
		}
		face(direction);
		if(s<crouch+leap){
			double t=(s-crouch+1)/(double)leap;
			var goal=leapFrom.lerp(leapTo,t).add(0,Math.sin(t*Math.PI)*3,0);
			move(MoverType.SELF,goal.subtract(position()));setDeltaMovement(Vec3.ZERO);
			if(s==crouch)sound(level,position(),SoundEvents.RAVAGER_STEP,1.6F,.6F);
			ring(level,leapTo,SwordTombRules.VERDICT_RADIUS,BONE,18);
			if(s==crouch+leap-1)land(level);
			return;
		}
		if(s%10==0){var at=position().add(direction.scale(1.6));sound(level,at,SoundEvents.GRAVEL_HIT,1.2F,.5F);level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,at)),at.x,at.y+.2,at.z,6,.3,.1,.3,.1);}
		if(left==0){
			var t=getTarget();
			if(roused && t!=null && SwordTombRules.chain(SwordTombRules.VERDICT,distanceTo(t))==SwordTombRules.HEEL)begin(level,SwordTombRules.HEEL,t);
			else settle();
		}
	}
	private void land(ServerLevel level){
		sound(level,position(),SoundEvents.ANVIL_LAND,1.6F,.5F);Feels.sound(level,position(),"aura_tomb_thrust",1.2F,.7F);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,position())),getX(),getY()+.2,getZ(),40,1.2,.2,1.2,.2);
		level.sendParticles(ParticleTypes.POOF,getX(),getY()+.3,getZ(),12,1,.1,1,.04);
		AuraFx.groundScar(level,position().add(direction.scale(1.4)),SwordTombRules.VERDICT_RADIUS,70,2);
		double r=SwordTombRules.VERDICT_RADIUS;
		for(var p:prey(level)){var d=p.position().subtract(position());if(d.x*d.x+d.z*d.z<=r*r && Math.abs(d.y)<2)hit(level,p,SwordTombRules.damage(SwordTombRules.VERDICT),.6,position(),.3);}
	}
	/**
	 * Hands of the buried: the blade raised point down and plunged into the ground, and it kneels over it; then wave on wave, a ring
	 * of grave dust and a wisp of soul-fire under each of its prey, and a moment later a hand of stone bursts up there.
	 */
	private void hands(ServerLevel level){
		getNavigation().stop();face(direction);
		long s=since(),now=level.getGameTime();
		if(s<SwordTombRules.HANDS_TELL){
			level.sendParticles(ParticleTypes.SOUL,getX(),getY()+.2,getZ(),2,.8,.1,.8,.02);
			if(s==SwordTombRules.HANDS_TELL-1){
				var at=position().add(direction.scale(1.2));
				sound(level,at,SoundEvents.ANVIL_LAND,1.4F,.4F);AuraFx.groundScar(level,at,1.2,70,2);
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,at)),at.x,at.y+.2,at.z,20,.4,.1,.4,.15);
			}
			return;
		}
		long w=s-SwordTombRules.HANDS_TELL;
		if(w%SwordTombRules.HANDS_EVERY==0 && w/SwordTombRules.HANDS_EVERY<waves()){
			waves++;
			for(var p:prey(level))grasps.add(new Grasp(p.position(),now+SwordTombRules.HANDS_RISE,waves));
			sound(level,position(),SoundEvents.SOUL_ESCAPE.value(),1.8F,.6F);
			ring(level,position(),2,SOUL,16);
		}
		if(s%4==0)level.sendParticles(ParticleTypes.SOUL,getX(),getY()+.3,getZ(),3,1.2,.1,1.2,.02);
		if(left==0)settle();
	}
	private int waves(){return SwordTombRules.HANDS_WAVES+(roused?1:0);}
	/** The buried hands, marked where they will come up and bursting up when their time comes; one to a wave for each of its prey. */
	private void grasp(ServerLevel level){
		if(grasps.isEmpty())return;
		long now=level.getGameTime();double r=SwordTombRules.GRASP_RADIUS;
		for(var it=grasps.iterator();it.hasNext();){
			var g=it.next();var at=g.at();long to=g.when()-now;
			if(to>0){
				if(to%3==0)ring(level,at,r,GRAVE,12);
				level.sendParticles(ParticleTypes.SOUL,at.x,at.y+.1,at.z,1,.3,0,.3,.01);
				if(to==6)sound(level,at,SoundEvents.GRAVEL_BREAK,1F,.5F);
				continue;
			}
			it.remove();
			level.sendParticles(new BlockParticleOption(ParticleTypes.DUST_PILLAR,Blocks.STONE.defaultBlockState()),at.x,at.y+.1,at.z,30,.4,.1,.4,.3);
			level.sendParticles(ParticleTypes.SOUL,at.x,at.y+.8,at.z,6,.3,.6,.3,.04);
			sound(level,at,SoundEvents.DEEPSLATE_BREAK,1.4F,.5F);
			for(var p:prey(level)){
				var d=p.position().subtract(at);
				if(d.x*d.x+d.z*d.z>r*r || d.y<-1 || d.y>1.5 || grasped.getOrDefault(p.getUUID(),-1L)==g.wave())continue;
				grasped.put(p.getUUID(),g.wave());
				if(hit(level,p,SwordTombRules.damage(SwordTombRules.HANDS),0,at,.7))p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,40,1,false,true),this);
			}
		}
	}
	/**
	 * The rampart: the blade brought crosswise before its chest, the back foot stamping, the line it will take scuffed out ahead; then
	 * a charge straight down that line. Step aside; or stand before a wall and let it run into it, and it stands there shaking, stuck.
	 */
	private void rampart(ServerLevel level,LivingEntity target){
		getNavigation().stop();
		long s=since();int tell=SwordTombRules.RAMPART_TELL;
		Vec3 side=new Vec3(-direction.z,0,direction.x);
		if(s<tell){
			if(s<tell-SwordTombRules.RAMPART_LOCK && target!=null){var flat=target.position().subtract(position()).multiply(1,0,1);if(flat.lengthSqr()>1e-4)direction=flat.normalize();}
			face(direction);
			if(s%6==0){sound(level,position(),SoundEvents.RAVAGER_STEP,1.4F,.5F);level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,position())),getX(),getY()+.1,getZ(),8,.6,.05,.6,.1);}
			if(s%2==0)for(double r=2;r<=SwordTombRules.RAMPART_SPEED*SwordTombRules.RAMPART_TICKS;r+=1.5)for(int sign:new int[]{-1,1}){
				var at=position().add(direction.scale(r)).add(new Vec3(-direction.z,0,direction.x).scale(sign*SwordTombRules.RAMPART_WIDTH));
				level.sendParticles(GRAVE,at.x,at.y+.15,at.z,1,0,0,0,0);
			}
			if(s==tell-SwordTombRules.RAMPART_LOCK)sound(level,position(),SoundEvents.ARMOR_EQUIP_NETHERITE.value(),2F,.4F);
			return;
		}
		face(direction);
		long c=s-tell;
		if(c==0){charge=direction.scale(SwordTombRules.RAMPART_SPEED);Feels.sound(level,position(),"aura_tomb_thrust",1.4F,.6F);}
		var before=position();
		move(MoverType.SELF,new Vec3(charge.x,-.3,charge.z));setDeltaMovement(Vec3.ZERO);
		double went=position().subtract(before).multiply(1,0,1).length();
		if(c%2==0)sound(level,position(),SoundEvents.RAVAGER_STEP,1.6F,.7F);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,position())),getX(),getY()+.2,getZ(),4,.6,.1,.6,.15);
		level.sendParticles(ParticleTypes.CLOUD,getX(),getY()+.4,getZ(),2,.5,.2,.5,.02);
		for(var p:prey(level)){
			if(struck.contains(p.getUUID()))continue;
			var d=p.position().subtract(position());
			if(!SwordTombRules.rampart(d.dot(direction),d.dot(side),d.y))continue;
			struck.add(p.getUUID());hit(level,p,SwordTombRules.damage(SwordTombRules.RAMPART),1.4,position().subtract(direction),.35);
		}
		if(c>1 && (horizontalCollision || went<SwordTombRules.RAMPART_SPEED*.3)){
			pose(SwordTombRules.WALLED,SwordTombRules.WALLED_TICKS);
			sound(level,position(),SoundEvents.ANVIL_LAND,2F,.4F);Feels.sound(level,position(),"aura_tomb_break",1.2F,.8F);
			var at=position().add(direction.scale(1.2));var wall=level.getBlockState(BlockPos.containing(at.x,getY()+1,at.z));if(wall.isAir())wall=Blocks.STONE.defaultBlockState();
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,wall),at.x,getY()+1.5,at.z,30,.6,.8,.6,.2);
			return;
		}
		if(left==0 || position().add(charge).subtract(Vec3.atBottomCenterOf(home)).multiply(1,0,1).length()>SwordTombRules.ARENA_RADIUS-1){
			// Pulled up short at the edge, or run out: a skid, turf flying.
			sound(level,position(),SoundEvents.GRAVEL_HIT,1.6F,.5F);
			level.sendParticles(ParticleTypes.POOF,getX(),getY()+.3,getZ(),10,.8,.1,.8,.03);
			var t=getTarget();
			if(!roused || t==null){pose(SwordTombRules.RECOVER,SwordTombRules.RAMPART_RECOVER);return;}
			var flat=t.position().subtract(position()).multiply(1,0,1);if(flat.lengthSqr()>1e-4)direction=flat.normalize();
			face(direction);deck.played(SwordTombRules.SWEEP);struck.clear();windup(level,SwordTombRules.SWEEP,SwordTombRules.CHAIN_WINDUP);
		}
	}
	/**
	 * The rite, once, when it is half broken: the blade planted, both hands on the pommel and head bowed, grave ash rising all over
	 * the arena and a ring of soul-fire drawing in on it; then it rises, throws its arms wide and everyone near is flung back.
	 */
	private void rite(ServerLevel level){
		getNavigation().stop();face(direction);
		long s=since();var c=Vec3.atCenterOf(home);
		level.sendParticles(ParticleTypes.WHITE_ASH,c.x,c.y,c.z,20,9,1.5,9,0);
		level.sendParticles(ParticleTypes.ASH,getX(),getY()+1,getZ(),6,1,1,1,0);
		if(s%3==0)ring(level,position(),Math.max(.6,SwordTombRules.RITE_RADIUS*(1-s/(double)SwordTombRules.RITE_TICKS)),SOUL,24);
		if(s%9==0)sound(level,position(),SoundEvents.SOUL_ESCAPE.value(),1.6F,.4F+s*.01F);
		if(left>0)return;
		roused=true;bar.setColor(BossEvent.BossBarColor.RED);
		Feels.sound(level,position(),"aura_tomb_awake",2F,1.2F);sound(level,position(),SoundEvents.GENERIC_EXPLODE.value(),1.4F,.6F);
		for(int i=1;i<=3;i++)ring(level,position(),SwordTombRules.RITE_RADIUS*i/3.0,ParticleTypes.CLOUD,24*i);
		level.sendParticles(ParticleTypes.SOUL,getX(),getY()+1,getZ(),30,1.5,1,1.5,.1);
		double r=SwordTombRules.RITE_RADIUS;
		for(var p:prey(level))if(p.distanceToSqr(this)<=r*r)hit(level,p,SwordTombRules.damage(SwordTombRules.RITE),1.6,position(),.5);
		deck.clear();settle();
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
		if(hurt && source.getEntity() instanceof ServerPlayer p){
			fought=true;var t=tomb(level);if(t!=null)t.participant(p);
			// Struck while it listens, it knows exactly where its striker stands.
			if(move()==SwordTombRules.LISTEN && isAlive())hear(level,p);
		}
		return hurt;
	}
	@Override public void die(DamageSource source){
		if(level() instanceof ServerLevel server){var t=tomb(server);if(t!=null){if(source.getEntity() instanceof ServerPlayer killer && !killer.isCreative() && !killer.isSpectator())fought=true;if(fought)for(var p:server.players())t.participant(p);t.defeated(getUUID());}Feels.sound(server,position(),"aura_tomb_fall",1,1);bar.removeAllPlayers();}
		super.die(source);
	}
	@Override public void remove(RemovalReason reason){bar.removeAllPlayers();super.remove(reason);}
	@Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(home!=null)out.store("tomb_home",BlockPos.CODEC,home);if(reliquary!=null)out.store("reliquary",BlockPos.CODEC,reliquary);out.putBoolean("fought",fought);out.putBoolean("roused",roused);out.putInt("drill",sequence);}
	@Override protected void readAdditionalSaveData(ValueInput in){
		super.readAdditionalSaveData(in);home=in.read("tomb_home",BlockPos.CODEC).orElse(null);reliquary=in.read("reliquary",BlockPos.CODEC).orElse(null);fought=in.getBooleanOr("fought",false);
		roused=in.getBooleanOr("roused",false);sequence=Math.min(SwordTombRules.DRILL.length,Math.max(0,in.getIntOr("drill",0)));if(roused)bar.setColor(BossEvent.BossBarColor.RED);
		pose(SwordTombRules.RECOVER,40);
	}
	private List<ServerPlayer> prey(ServerLevel level){var c=home==null?position():Vec3.atCenterOf(home);return level.getEntitiesOfClass(ServerPlayer.class,new AABB(c,c).inflate(20,8,20),p->p.isAlive() && !p.isCreative() && !p.isSpectator());}
	private boolean hit(ServerLevel level,ServerPlayer p,double damage,double shove,Vec3 from,double lift){
		if(!p.hurtServer(level,level.damageSources().mobAttack(this),(float)damage))return false;
		if(shove>0)p.knockback(shove,from.x-p.getX(),from.z-p.getZ(),level.damageSources().mobAttack(this),0);
		if(lift>0)p.setDeltaMovement(p.getDeltaMovement().add(0,lift,0));
		p.needsSync=true;AuraFx.impact(this,p,0xBDA879,4,AuraFxRules.Weight.FULL);
		return true;
	}
	/** A spot pulled back inside the arena, if it lies too near the edge. */
	private Vec3 clamp(Vec3 at){
		var c=Vec3.atBottomCenterOf(home);var d=at.subtract(c).multiply(1,0,1);double most=SwordTombRules.ARENA_RADIUS-2;
		return d.length()>most?c.add(d.normalize().scale(most)).add(0,at.y-c.y,0):at;
	}
	private Vec3 eyes(){return position().add(0,getEyeHeight(),0);}
	private void sound(ServerLevel level,Vec3 at,SoundEvent s,float volume,float pitch){level.playSound(null,at.x,at.y,at.z,s,SoundSource.HOSTILE,volume,pitch);}
	private BlockState ground(ServerLevel level,Vec3 at){var s=level.getBlockState(BlockPos.containing(at.x,at.y-.5,at.z));return s.isAir()?Blocks.STONE.defaultBlockState():s;}
	private void ring(ServerLevel level,Vec3 at,double r,ParticleOptions dust,int n){for(int i=0;i<n;i++){double a=i*Math.PI*2/n;level.sendParticles(dust,at.x+Math.cos(a)*r,at.y+.15,at.z+Math.sin(a)*r,1,0,0,0,0);}}
	private Vec3 ahead(float yaw){double r=Math.toRadians(yaw);return new Vec3(-Math.sin(r),0,Math.cos(r));}
	private float yawTo(Vec3 at){return (float)Math.toDegrees(Math.atan2(-(at.x-getX()),at.z-getZ()));}
	private void face(Vec3 way){float yaw=(float)Math.toDegrees(Math.atan2(-way.x,way.z));setYRot(yaw);yBodyRot=yaw;yHeadRot=yaw;}
}
