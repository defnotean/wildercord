package dev.wildercord.aura.world;

import dev.wildercord.cast.MoveDeck;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
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
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import java.util.*;

/**
 * Mojito ({@link MojitoRules}): a black standard poodle as tall as three men, white-bibbed and gold-collared, a greatsword held
 * crosswise in his jaws and mint-green magic welling up on both his flanks. Generation leaves him curled asleep before the grave in
 * the misty grove, the sword laid before his paws, bound to that spot and to the grove. While he sleeps he cannot be hurt; the first
 * player far enough in (or the first blow) gets him up: he stretches, takes up the sword in his mouth, shakes out his coat, and the
 * boss bar comes up. The grove going quiet sends him back to sleep, healed. Anything hostile in the grove, he zaps out of the world.
 * <p>
 * On his feet every blow asks something different, and he never throws the same one twice running: a low sweep scraping sparks off
 * the ground (jump it); a rear-up and a chop along a line with a crack of magic running on past it (step off the line); the sword
 * tossed spinning and him bounding after it, pleased as anything if he catches it; dirt dug up and flung back in clods onto marked
 * spots (get round to his face); a pounce onto his own shadow and a ring of magic running out from where he lands (jump it); his own
 * tail chased round and round, then dizzy; and three volleys of magic off his flanks, straight, then led, then fanned. Crowd him or
 * keep hitting him and he shakes himself off. At half health he howls and turns wild: his blows run into one another, and he gets
 * the zoomies, racing round the grove leaving stinging magic and now and then skidding round to cut straight across, until he flops.
 */
public final class Mojito extends PathfinderMob implements Enemy {
	private static final EntityDataAccessor<Integer> MOVE=SynchedEntityData.defineId(Mojito.class,EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN=SynchedEntityData.defineId(Mojito.class,EntityDataSerializers.LONG);
	/** The current move's tell in ticks, so the client knows when the blow lands. */
	private static final EntityDataAccessor<Integer> TELL=SynchedEntityData.defineId(Mojito.class,EntityDataSerializers.INT);
	/** Which way the slash sweeps, or a hop carries him, for the client: -1 left, 1 right, 0 straight back. */
	private static final EntityDataAccessor<Integer> SIDE=SynchedEntityData.defineId(Mojito.class,EntityDataSerializers.INT);
	/** Where the thrown sword is, and whether it is out of his mouth, for the client. */
	private static final EntityDataAccessor<Vector3fc> SWORD=SynchedEntityData.defineId(Mojito.class,EntityDataSerializers.VECTOR3);
	private static final EntityDataAccessor<Boolean> THROWN=SynchedEntityData.defineId(Mojito.class,EntityDataSerializers.BOOLEAN);
	/** Dirt, and the stars round his head when he is dizzy. */
	private static final DustParticleOptions EARTH=new DustParticleOptions(0x6B4A2B,1.6F),STAR=new DustParticleOptions(0xFFE066,1.1F),SHADOW=new DustParticleOptions(0x1A1A22,1.8F);
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
	/** Which way the next sweep goes. */
	private int swing=1;
	/** Howled at half health, and wild from then on. */
	private boolean wild;
	/** What he did lately, so he mixes his blows up, and when he last made each move. */
	private final MoveDeck deck=new MoveDeck(4);
	private final long[] lastMade=new long[32];
	{Arrays.fill(lastMade,-9999);}
	/** How many blows this run has run into one another, wild. */
	private int links;
	/** Whoever this blow has already struck. */
	private final Set<UUID> struck=new HashSet<>();
	/** Whoever his bounding run after the sword has already run down. */
	private final Set<UUID> trampled=new HashSet<>();
	/** How his prey has been moving lately, to lead them; how long someone has stood crowding him, and when he took his last few blows. */
	private Vec3 preyVel=Vec3.ZERO,preyLast;
	private int crowd;
	private final ArrayDeque<Long> blows=new ArrayDeque<>();
	/** A ring of magic running out along the ground from ({@code x},{@code y},{@code z}). */
	private static final class Wave{final double x,y,z,speed,max,damage,lift;final long born;final Set<UUID> struck=new HashSet<>();
		Wave(Vec3 at,double speed,double max,double damage,double lift,long born){x=at.x;y=at.y;z=at.z;this.speed=speed;this.max=max;this.damage=damage;this.lift=lift;this.born=born;}}
	private final List<Wave> waves=new ArrayList<>();
	/** A clod of earth flung from {@code from} onto {@code to}, peaking {@code peak} above the line. */
	private record Clod(Vec3 from,Vec3 to,long born,double peak){}
	private final List<Clod> clods=new ArrayList<>();
	/** The fetch: where the sword is, how it moves each tick, and how far along it is (1 out, 2 lying there, 3 coming back). */
	private Vec3 sword=Vec3.ZERO,swordStep=Vec3.ZERO;
	private int fetchStage;
	/** The pounce: where he will come down. */
	private Vec3 landing=Vec3.ZERO;
	/** The zoomies: when the current dash began (-1 for none) and its step. */
	private long dashAt=-1;
	private Vec3 dashStep=Vec3.ZERO;
	/** Client side: the sword where it was last tick and this one. */
	private Vec3 swordOld=Vec3.ZERO,swordCur=Vec3.ZERO;
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

	public Mojito(EntityType<? extends Mojito> type,Level level){super(type,level);xpReward=MojitoRules.TIER.xp;setPersistenceRequired();}
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
	/** Whether the sword is out of his mouth, and (client side) where it is, between last tick and this. */
	public boolean thrown(){return entityData.get(THROWN);}
	public Vec3 swordAt(float partial){return swordOld.lerp(swordCur,partial);}

	@Override protected void defineSynchedData(SynchedEntityData.Builder b){
		super.defineSynchedData(b);b.define(MOVE,MojitoRules.DORMANT);b.define(BEGAN,0L);b.define(TELL,0);b.define(SIDE,0);
		b.define(SWORD,new Vector3f());b.define(THROWN,false);
	}
	@Override public void tick(){
		super.tick();
		if(level().isClientSide()){
			var v=entityData.get(SWORD);var now=new Vec3(v.x(),v.y(),v.z());
			swordOld=thrown() && swordCur.distanceToSqr(now)<64?swordCur:now;swordCur=now;
		}
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
		if(move!=MojitoRules.FETCH && entityData.get(THROWN)){entityData.set(THROWN,false);fetchStage=0;}
		if(!MojitoRules.scripted(move))bar.setName(Component.translatable("boss.wildercord.mojito.status",getType().getDescription(),
			Component.translatable("boss.wildercord.mojito.move."+MojitoRules.key(move))));
	}
	/** Back asleep before the grave, whole again. */
	private void bedDown(){
		pose(MojitoRules.DORMANT,0);hold();setHealth(getMaxHealth());setTarget(null);bar.removeAllPlayers();quiet=0;clearFire();bolts.clear();trail.clear();
		wild=false;bar.setColor(BossEvent.BossBarColor.RED);deck.clear();Arrays.fill(lastMade,-9999);links=0;
		waves.clear();clods.clear();blows.clear();crowd=0;fetchStage=0;entityData.set(THROWN,false);
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
		fly(level);linger(level);waves(level);clods(level);
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
		if(target!=null){
			var at=target.position();
			if(preyLast!=null){var v=preyVel.scale(.7).add(at.subtract(preyLast).multiply(1,0,1).scale(.3));preyVel=v.length()>1?v.normalize():v;}
			preyLast=at;
			crowd=distanceTo(target)<=MojitoRules.SHAKE_NEAR?crowd+1:Math.max(0,crowd-2);
		}else{preyLast=null;preyVel=Vec3.ZERO;crowd=0;}
		switch(move()){
			case MojitoRules.SWEEP->sweep(level,target);
			case MojitoRules.CHOP->chop(level,target);
			case MojitoRules.CHASE->chase(level,target);
			case MojitoRules.DIZZY->dizzy(level);
			case MojitoRules.POUNCE->pounce(level,target);
			case MojitoRules.FETCH->fetch(level,target);
			case MojitoRules.PRANCE->prance(level);
			case MojitoRules.DIG->dig(level,target);
			case MojitoRules.SHAKE->shake(level);
			case MojitoRules.HOWL->howl(level);
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
			int pick;
			if(MojitoRules.howls(getHealth(),getMaxHealth(),wild))pick=MojitoRules.HOWL;
			else if(MojitoRules.shakes(blows(now),crowd,now-lastMade[MojitoRules.SHAKE]))pick=MojitoRules.SHAKE;
			else if(MojitoRules.zooms(wild,now-lastMade[MojitoRules.ZOOMIES],d))pick=MojitoRules.ZOOMIES;
			else{
				long[] since=new long[lastMade.length];
				for(int i=0;i<since.length;i++)since[i]=now-lastMade[i];
				pick=MojitoRules.choose(deck,d,behind,since,random.nextDouble());
				// A long chase ends in a pounce from further in.
				if(pick<0 && chased>=60 && d>=5 && since[MojitoRules.POUNCE]>=MojitoRules.rest(MojitoRules.POUNCE)/2)pick=MojitoRules.POUNCE;
			}
			if(pick>=0){links=0;begin(level,pick,target,false);return;}
		}
		if(d>MojitoRules.CLOSE){
			chased++;
			if(!getNavigation().moveTo(target,1))getMoveControl().setWantedPosition(target.getX(),target.getY(),target.getZ(),1);
		}else{chased=0;getNavigation().stop();turn(yawTo(target.position()),10);}
	}

	/** Start {@code move} at {@code target}; {@code chained} when he runs straight on into it from another blow, wild. */
	private void begin(ServerLevel level,int move,LivingEntity target,boolean chained){
		lastMade[move]=level.getGameTime();deck.played(move);chased=0;struck.clear();
		if(target!=null)aim=yawTo(target.position());
		switch(move){
			case MojitoRules.SWEEP->{swing=-swing;entityData.set(SIDE,swing);pose(move,0,chained?MojitoRules.SWEEP_TELL_CHAINED:MojitoRules.SWEEP_TELL);growl(level,1.2F);}
			case MojitoRules.CHOP->{pose(move,0,chained?MojitoRules.CHOP_TELL_CHAINED:MojitoRules.CHOP_TELL);growl(level,1.5F);sound(level,head(),SoundEvents.ARMOR_EQUIP_NETHERITE.value(),1.4F,.6F);}
			case MojitoRules.CHASE->{entityData.set(SIDE,random.nextBoolean()?1:-1);pose(move,0,MojitoRules.CHASE_TELL);bark(level,1.6F,.95F);}
			case MojitoRules.POUNCE->{landing=position();pose(move,0,MojitoRules.POUNCE_TELL);bark(level,1.8F,.55F);}
			case MojitoRules.BOLTS->{pose(move,0,MojitoRules.BOLT_TELL);sound(level,position().add(0,3,0),SoundEvents.AMETHYST_BLOCK_RESONATE,2F,.7F);}
			case MojitoRules.FETCH->{fetchStage=0;trampled.clear();pose(move,0,MojitoRules.FETCH_TELL);bark(level,1.8F,.85F);}
			case MojitoRules.DIG->{pose(move,0,MojitoRules.DIG_TELL);sound(level,position(),SoundEvents.SNIFFER_DIGGING,1.8F,.6F);}
			case MojitoRules.SHAKE->{pose(move,0,MojitoRules.SHAKE_TELL);sound(level,head(),voice().whineSound().value(),1.4F,.6F);}
			case MojitoRules.HOWL->{wild=true;bar.setColor(BossEvent.BossBarColor.GREEN);pose(move,0,MojitoRules.HOWL_TELL);sound(level,head(),SoundEvents.WARDEN_SONIC_CHARGE,1.6F,1.3F);}
			case MojitoRules.HOP->hopFrom(level,target==null?position().add(ahead(getYRot())):target.position());
			case MojitoRules.ZOOMIES->{zoomStruck.clear();dashAt=-1;pose(move,0,MojitoRules.ZOOM_TELL);bark(level,2F,.75F);}
			default->{}
		}
	}
	/** Done with {@code move}: wild, maybe straight on into another blow; otherwise spent for {@code recover} ticks, longer after a run. */
	private void after(ServerLevel level,int move,int recover){
		var target=getTarget();
		if(wild && target!=null && links<MojitoRules.CHAIN_MOST){
			int next=MojitoRules.chain(move,distanceTo(target),random.nextDouble());
			if(next>=0){links++;begin(level,next,target,true);return;}
		}
		spent(links>0?recover+MojitoRules.CHAIN_REST:recover);links=0;
	}
	/** A point {@code r} out along the ground on a line {@code angle} degrees off his aim, a hand above the grass. */
	private Vec3 bladeAt(double angle,double r){return position().add(ahead((float)(aim+angle)).scale(r)).add(0,.35,0);}

	/**
	 * Head drawn back to one side through the tell, the blade's tip scraping sparks off the ground; then swept low across his front
	 * from that side right round to the other, catching whoever it passes with their feet still down.
	 */
	private void sweep(ServerLevel level,LivingEntity target){
		face(aim);still();
		long el=since();int tell=tell(),s=side();
		if(el<tell){
			var tip=bladeAt(s*90,6);
			if(el%2==0)level.sendParticles(ParticleTypes.ELECTRIC_SPARK,tip.x,tip.y,tip.z,3,.3,.1,.3,.05);
			if(el%4==0)sound(level,tip,SoundEvents.GRINDSTONE_USE,.8F,.8F+el*.03F);
			return;
		}
		long k=el-tell;
		if(k>=1 && k<=MojitoRules.SWEEP_TICKS){
			if(k==1)sound(level,head(),SoundEvents.PLAYER_ATTACK_SWEEP,2.2F,.55F+random.nextFloat()*.1F);
			double a=MojitoRules.blade(s,k);
			for(double r=2;r<=MojitoRules.SWEEP_REACH;r+=1.5){
				var at=bladeAt(a,r);
				level.sendParticles(MINT,at.x,at.y,at.z,1,.15,.05,.15,0);
				if(r>5)level.sendParticles(ParticleTypes.CRIT,at.x,at.y,at.z,2,.2,.05,.2,.1);
			}
			var tip=bladeAt(a,MojitoRules.SWEEP_REACH*.8);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK,tip.x,tip.y+.2,tip.z,1,0,0,0,0);
			for(var p:prey(level)){
				if(struck.contains(p.getUUID()))continue;
				double flat=Math.sqrt(p.distanceToSqr(getX(),p.getY(),getZ()));
				if(MojitoRules.sweeps(s,(int)k,yawTo(p.position())-aim,flat,p.getY()-getY())){
					// Knocked along the way the blade is going.
					struck.add(p.getUUID());hit(level,p,MojitoRules.SWEEP_DAMAGE,1.4,p.position().subtract(ahead(aim-s*90)),.25);
				}
			}
		}
		if(k>=MojitoRules.SWEEP_TICKS+2)after(level,MojitoRules.SWEEP,MojitoRules.SWEEP_RECOVER);
	}
	/** The ground under {@code at}, dirt if there is none, for the dust a blow throws up. */
	private BlockState ground(ServerLevel level,Vec3 at){
		var s=level.getBlockState(BlockPos.containing(at.x,at.y-.5,at.z));
		return s.isAir()?Blocks.DIRT.defaultBlockState():s;
	}
	/**
	 * Reared up on his hind legs through the tell, belly bare, following his prey until a beat before; then the sword brought straight
	 * down along a line. It sticks in the ground a moment, and a crack of magic creeps on past where it landed and bursts up.
	 */
	private void chop(ServerLevel level,LivingEntity target){
		still();
		long el=since();int tell=tell();
		if(el<tell-3 && target!=null){turn(yawTo(target.position()),6);aim=getYRot();}
		face(aim);
		if(el<tell){
			if(el>=tell-8 && el%2==0)for(double r=1;r<=MojitoRules.CHOP_LENGTH;r+=1){var at=bladeAt(0,r);level.sendParticles(MINT_SMALL,at.x,at.y,at.z,1,.1,0,.1,0);}
			return;
		}
		long k=el-tell;
		if(k==0){
			var at=bladeAt(0,MojitoRules.CHOP_LENGTH*.6);
			sound(level,at,SoundEvents.ANVIL_LAND,1.4F,.5F);sound(level,at,SoundEvents.MACE_SMASH_GROUND_HEAVY,2F,.7F);
			for(double r=1;r<=MojitoRules.CHOP_LENGTH;r+=.75){
				var p=bladeAt(0,r);
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,p)),p.x,p.y,p.z,4,.3,.1,.3,.1);
				level.sendParticles(ParticleTypes.CRIT,p.x,p.y,p.z,1,.2,.1,.2,.1);
			}
			for(var p:prey(level))if(reaches(MojitoRules.CHOP,p))hit(level,p,MojitoRules.CHOP_DAMAGE,.8,position(),.2);
		}else if(k<MojitoRules.CHOP_CRACK){
			// The crack creeping on past the blade, the ground splitting along it.
			double r=MojitoRules.CHOP_LENGTH+(MojitoRules.CRACK_LENGTH-MojitoRules.CHOP_LENGTH)*k/(double)MojitoRules.CHOP_CRACK;
			for(double x=MojitoRules.CHOP_LENGTH;x<=r;x+=1){var p=bladeAt(0,x);level.sendParticles(MINT_SMALL,p.x,p.y-.2,p.z,1,.2,0,.2,0);}
			var tip=bladeAt(0,r);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK,tip.x,tip.y,tip.z,4,.3,.1,.3,.05);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,tip)),tip.x,tip.y,tip.z,3,.3,.05,.3,.05);
			if(k%2==1)sound(level,tip,SoundEvents.ROOTED_DIRT_BREAK,1.2F,.6F+k*.05F);
		}else if(k==MojitoRules.CHOP_CRACK){
			var mid=bladeAt(0,(MojitoRules.CHOP_LENGTH+MojitoRules.CRACK_LENGTH)/2);
			sound(level,mid,SoundEvents.GRAVEL_BREAK,2F,.5F);sound(level,mid,SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),2F,.7F);
			for(double x=MojitoRules.CHOP_LENGTH;x<=MojitoRules.CRACK_LENGTH;x+=1){
				var p=bladeAt(0,x);
				level.sendParticles(new BlockParticleOption(ParticleTypes.DUST_PILLAR,ground(level,p)),p.x,p.y,p.z,6,.3,.4,.3,.3);
				level.sendParticles(MINT,p.x,p.y+.8,p.z,3,.3,.6,.3,0);
				level.sendParticles(ParticleTypes.END_ROD,p.x,p.y+.4,p.z,1,.2,.3,.2,.08);
			}
			var f=ahead(aim);
			for(var p:prey(level)){
				double dx=p.getX()-getX(),dz=p.getZ()-getZ();
				if(MojitoRules.cracks(dx*f.x+dz*f.z,dx*-f.z+dz*f.x,p.getY()-getY()))hit(level,p,MojitoRules.CRACK_DAMAGE,0,position(),MojitoRules.CRACK_LIFT);
			}
		}
		if(k>=MojitoRules.CHOP_STUCK){
			// Wrenched back out of the ground.
			var at=bladeAt(0,MojitoRules.CHOP_LENGTH*.6);
			sound(level,at,SoundEvents.SHOVEL_FLATTEN.value(),1.6F,.5F);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,at)),at.x,at.y,at.z,12,.6,.2,.6,.1);
			after(level,MojitoRules.CHOP,MojitoRules.CHOP_RECOVER);
		}
	}
	/**
	 * He spots his tail through the tell, head looking back, tail going; then round and round after it three times with the blade
	 * sticking out, drifting after his prey as he goes, striking whoever it passes; and then he is dizzy.
	 */
	private void chase(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell(),s=side();
		if(el<tell){still();face(aim);if(el%4==0)sound(level,head(),voice().pantSound().value(),1F,1F);return;}
		long k=el-tell;
		float yaw=aim+s*(float)(360.0*MojitoRules.CHASE_TURNS*k/MojitoRules.CHASE_TICKS);face(yaw);
		var drift=target==null?Vec3.ZERO:target.position().subtract(position()).multiply(1,0,1);
		drift=drift.length()>3?drift.normalize().scale(MojitoRules.CHASE_DRIFT):Vec3.ZERO;
		move(MoverType.SELF,new Vec3(drift.x,-.3,drift.z));setDeltaMovement(Vec3.ZERO);
		// Round often enough to come by everyone again.
		if(k%10==1)struck.clear();
		if(k%5==1)sound(level,position(),SoundEvents.PLAYER_ATTACK_SWEEP,1.6F,.7F+random.nextFloat()*.2F);
		if(k%4==0)sound(level,position(),SoundEvents.WOLF_STEP.value(),1.4F,.7F);
		var f=ahead(yaw);
		for(int side=-1;side<=1;side+=2){
			double x=getX()-f.z*side*5,z=getZ()+f.x*side*5;
			level.sendParticles(ParticleTypes.SWEEP_ATTACK,x,getY()+2.5,z,1,0,0,0,0);
			level.sendParticles(MINT,x,getY()+2.5,z,2,.3,.2,.3,0);
		}
		for(var p:prey(level))if(!struck.contains(p.getUUID()) && reaches(MojitoRules.CHASE,p)){struck.add(p.getUUID());hit(level,p,MojitoRules.CHASE_DAMAGE,1.6,position(),.3);}
		if(k>=MojitoRules.CHASE_TICKS){pose(MojitoRules.DIZZY,MojitoRules.DIZZY_TICKS);still();sound(level,head(),voice().whineSound().value(),1.4F,.7F);}
	}
	/** Dizzy after his tail: swaying where he stands, stars wheeling round his head. Open. */
	private void dizzy(ServerLevel level){
		still();long el=since();
		face(getYRot()+(float)Math.sin(el*.35)*3);
		var h=head();
		for(int i=0;i<3;i++){double a=el*.3+i*Math.PI*2/3;level.sendParticles(STAR,h.x+Math.cos(a)*1.1,h.y+.9,h.z+Math.sin(a)*1.1,1,0,0,0,0);}
		if(el%14==0)sound(level,h,voice().whineSound().value(),1F,.6F+random.nextFloat()*.1F);
		if(el%10==5)sound(level,position(),SoundEvents.WOLF_STEP.value(),1F,.5F);
		if(--left<=0){pose(MojitoRules.WATCH,gap());growl(level,1.2F);}
	}

	/**
	 * Crouched, hindquarters up through the tell, his shadow marking where he will land and following his prey until a beat before;
	 * then a great leap onto it, the sword brought down as he lands, and a ring of magic running out along the ground from there.
	 */
	private void pounce(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell(),n=MojitoRules.POUNCE_TICKS;
		if(el<=tell-MojitoRules.POUNCE_LOCK && target!=null){
			aim=yawTo(target.position());
			double d=Math.sqrt(target.distanceToSqr(getX(),target.getY(),getZ()));
			landing=position().add(ahead(aim).scale(MojitoRules.leapLength(d)));
		}
		face(aim);
		if(el<tell){
			still();
			if(el%4==0)level.sendParticles(ParticleTypes.POOF,getX(),getY()+.2,getZ(),6,1,.1,1,.03);
			if(el%2==0)ring(level,landing,MojitoRules.POUNCE_RADIUS,SHADOW,20);
			else ring(level,landing,MojitoRules.POUNCE_RADIUS*(1-el/(double)tell),MINT_SMALL,12);
			return;
		}
		if(el==tell){
			leapStep=landing.subtract(position()).multiply(1,0,1).scale(1.0/n);
			sound(level,position(),SoundEvents.RAVAGER_STEP,2F,.6F);growl(level,1.6F);
			return;
		}
		long k=el-tell-1;
		if(k<n){
			double vy=1.2*(1-2.0*k/(n-1));
			move(MoverType.SELF,new Vec3(leapStep.x,vy,leapStep.z));setDeltaMovement(Vec3.ZERO);
			level.sendParticles(MINT,getX(),getY()+2.5,getZ(),3,1,1,1,0);
			ring(level,landing,MojitoRules.POUNCE_RADIUS,SHADOW,16);
			return;
		}
		move(MoverType.SELF,new Vec3(0,-3,0));setDeltaMovement(Vec3.ZERO);
		var at=position();
		sound(level,at,SoundEvents.MACE_SMASH_GROUND_HEAVY,2.2F,.7F);sound(level,at,SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),1.6F,.6F);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,at)),at.x,at.y+.2,at.z,40,2,.2,2,.15);
		level.sendParticles(ParticleTypes.END_ROD,at.x,at.y+.3,at.z,16,1.2,.2,1.2,.08);
		for(var p:prey(level))if(MojitoRules.lands(p.getX()-at.x,p.getY()-at.y,p.getZ()-at.z))hit(level,p,MojitoRules.POUNCE_DAMAGE,1.4,at,.5);
		waves.add(new Wave(at,MojitoRules.WAVE_SPEED,MojitoRules.WAVE_REACH,MojitoRules.WAVE_DAMAGE,MojitoRules.WAVE_LIFT,level.getGameTime()));
		after(level,MojitoRules.POUNCE,MojitoRules.POUNCE_RECOVER);
	}
	/** A ring of {@code n} motes of {@code dust} at radius {@code r} round {@code at}, just off the ground. */
	private void ring(ServerLevel level,Vec3 at,double r,ParticleOptions dust,int n){
		for(int i=0;i<n;i++){double a=i*Math.PI*2/n;level.sendParticles(dust,at.x+Math.cos(a)*r,at.y+.15,at.z+Math.sin(a)*r,1,0,0,0,0);}
	}
	/** The rings of magic running out along the ground, catching whoever is standing as they pass, until they run out. */
	private void waves(ServerLevel level){
		if(waves.isEmpty())return;
		long now=level.getGameTime();
		var prey=prey(level);
		for(var it=waves.iterator();it.hasNext();){
			var w=it.next();
			double r0=(now-w.born-1)*w.speed,r1=(now-w.born)*w.speed;
			if(r0>w.max){it.remove();continue;}
			var at=new Vec3(w.x,w.y,w.z);
			int n=Math.max(12,(int)(r1*5));
			for(int i=0;i<n;i++){
				double a=i*Math.PI*2/n,x=w.x+Math.cos(a)*r1,z=w.z+Math.sin(a)*r1;
				level.sendParticles(i%3==0?ParticleTypes.ELECTRIC_SPARK:MINT,x,w.y+.25,z,1,.05,.05,.05,0);
			}
			if((now-w.born)%3==0)sound(level,at,SoundEvents.AMETHYST_BLOCK_CHIME,.9F,.6F+(float)(r1/w.max)*.6F);
			for(var p:prey){
				if(w.struck.contains(p.getUUID()))continue;
				double d=Math.sqrt(p.distanceToSqr(w.x,p.getY(),w.z));
				if(MojitoRules.crosses(d,r0,r1,p.getY()-w.y)){w.struck.add(p.getUUID());hit(level,p,w.damage,.8,at,w.lift);}
			}
		}
	}
	/**
	 * Eyes on his prey and tail going through the tell, the sword shifted in his jaws; then tossed spinning along the ground at them
	 * and him bounding after it, the blade and he both striking whoever is in the way. Caught, he prances about with it, pleased as
	 * anything and wide open; if he can't get to it, it comes back to him on its own, striking on the way.
	 */
	private void fetch(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<tell){
			still();
			if(el<=tell-MojitoRules.FETCH_LOCK && target!=null)turn(yawTo(target.position()),10);
			aim=getYRot();face(aim);
			if(el%4==0){var h=head();level.sendParticles(MINT,h.x,h.y-.6,h.z,3,.8,.2,.8,0);}
			if(el%5==0)sound(level,head(),voice().pantSound().value(),1.2F,1.1F);
			return;
		}
		long k=el-tell;
		if(k==0){
			sword=position().add(ahead(aim).scale(2.5)).add(0,MojitoRules.FETCH_HEIGHT,0);
			swordStep=ahead(aim).scale(MojitoRules.FETCH_THROW);
			fetchStage=1;entityData.set(SIDE,1);entityData.set(THROWN,true);
			sound(level,head(),SoundEvents.TRIDENT_THROW.value(),2F,.6F);bark(level,1.8F,1F);
		}
		var prey=prey(level);
		if(fetchStage==1){
			var next=sword.add(swordStep);
			var hit=level.clip(new ClipContext(sword,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
			if(hit.getType()!=HitResult.Type.MISS){next=hit.getLocation().subtract(swordStep.normalize().scale(.3));}
			sword=next;
			if((k+1)%4==0)sound(level,sword,SoundEvents.PLAYER_ATTACK_SWEEP,1.2F,1.3F);
			level.sendParticles(ParticleTypes.CRIT,sword.x,sword.y,sword.z,3,.4,.2,.4,.05);
			level.sendParticles(MINT,sword.x,sword.y,sword.z,2,.3,.2,.3,0);
			if(hit.getType()!=HitResult.Type.MISS || k*MojitoRules.FETCH_THROW>=MojitoRules.FETCH_RANGE){
				fetchStage=2;entityData.set(SIDE,2);
				sound(level,sword,SoundEvents.TRIDENT_HIT_GROUND,1.8F,.6F);sound(level,sword,SoundEvents.AMETHYST_BLOCK_CHIME,1.4F,.7F);
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,sword)),sword.x,sword.y,sword.z,14,.4,.2,.4,.1);
			}
		}else if(fetchStage==2){
			// Dropping to the ground where it ran out of throw.
			var down=sword.add(0,-.6,0);
			var hit=level.clip(new ClipContext(sword,down,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
			if(hit.getType()==HitResult.Type.MISS)sword=down;
			if(k%6==0)level.sendParticles(MINT_SMALL,sword.x,sword.y+.5,sword.z,4,.3,.4,.3,0);
		}
		if(fetchStage==1 || fetchStage==2){
			for(var p:prey)if(fetchStage==1 && !struck.contains(p.getUUID()) && MojitoRules.swordHits(sword.x,sword.y,sword.z,p.getX(),p.getY(),p.getZ(),p.getBbHeight())){
				struck.add(p.getUUID());hit(level,p,MojitoRules.SWORD_DAMAGE,1.2,sword.subtract(swordStep),.3);
			}
			// Bounding after it, running down whoever is in his way.
			var way=sword.subtract(position()).multiply(1,0,1);
			double far=way.length();
			if(fetchStage==2 && far<=MojitoRules.FETCH_CATCH){
				entityData.set(THROWN,false);fetchStage=0;still();
				pose(MojitoRules.PRANCE,MojitoRules.FETCH_PRANCE);
				sound(level,head(),SoundEvents.ARMOR_EQUIP_NETHERITE.value(),1.4F,1.1F);bark(level,1.8F,1.25F);
				var h=head();level.sendParticles(ParticleTypes.HAPPY_VILLAGER,h.x,h.y,h.z,14,1,.6,1,0);
				return;
			}
			if(k>=2 && far>.1){
				var step=way.scale(Math.min(MojitoRules.FETCH_RUN,far)/far);
				aim=(float)Math.toDegrees(Math.atan2(-step.x,step.z));face(aim);
				move(MoverType.SELF,new Vec3(step.x,-.3,step.z));setDeltaMovement(Vec3.ZERO);
				if(k%3==0)sound(level,position(),SoundEvents.WOLF_STEP.value(),1.6F,.65F+random.nextFloat()*.1F);
				if(k%2==0)level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,position())),getX(),getY()+.2,getZ(),4,1,.1,1,.1);
				for(var p:prey)if(!trampled.contains(p.getUUID()) && reaches(MojitoRules.FETCH,p)){trampled.add(p.getUUID());hit(level,p,MojitoRules.BODY_DAMAGE,1.6,position().subtract(ahead(aim)),.35);}
			}
			// Can't get to it: a wall in the way, or too long at it.
			if((k>6 && horizontalCollision) || k>=MojitoRules.FETCH_RUN_TICKS){
				fetchStage=3;entityData.set(SIDE,3);struck.clear();still();
				sound(level,sword,SoundEvents.TRIDENT_RETURN,2F,.6F);sound(level,head(),voice().whineSound().value(),1.4F,.8F);
			}
		}else if(fetchStage==3){
			// Back to him on its own, spinning, striking whoever it passes.
			still();
			var h=head();var way=h.subtract(sword);double far=way.length();
			face(yawTo(sword));
			if(far<=1.5 || k>=MojitoRules.FETCH_RUN_TICKS+60){
				entityData.set(THROWN,false);fetchStage=0;
				sound(level,h,SoundEvents.ARMOR_EQUIP_NETHERITE.value(),1.4F,.7F);
				after(level,MojitoRules.FETCH,MojitoRules.FETCH_RECOVER);
				return;
			}
			var step=way.scale(Math.min(MojitoRules.FETCH_RETURN,far)/far);
			sword=sword.add(step);
			if(k%4==0)sound(level,sword,SoundEvents.PLAYER_ATTACK_SWEEP,1.2F,1.3F);
			level.sendParticles(MINT,sword.x,sword.y,sword.z,3,.3,.2,.3,0);
			level.sendParticles(ParticleTypes.END_ROD,sword.x,sword.y,sword.z,1,.1,.1,.1,0);
			for(var p:prey)if(!struck.contains(p.getUUID()) && MojitoRules.swordHits(sword.x,sword.y,sword.z,p.getX(),p.getY(),p.getZ(),p.getBbHeight())){
				struck.add(p.getUUID());hit(level,p,MojitoRules.SWORD_DAMAGE,1.2,sword.subtract(step),.3);
			}
		}
		entityData.set(SWORD,new Vector3f((float)sword.x,(float)sword.y,(float)sword.z));
	}
	/** Pleased as anything with the sword caught: prancing on the spot, head high and tail going. Wide open. */
	private void prance(ServerLevel level){
		still();long el=since();
		face(getYRot()+(float)Math.sin(el*.25)*4);
		var h=head();
		if(el%5==0)level.sendParticles(ParticleTypes.NOTE,h.x,h.y+1,h.z,1,.6,.3,.6,1);
		if(el%7==0)level.sendParticles(ParticleTypes.HAPPY_VILLAGER,h.x,h.y,h.z,3,.8,.4,.8,0);
		if(el%4==0)sound(level,position(),SoundEvents.WOLF_STEP.value(),1F,1.1F);
		if(el%12==6)bark(level,1.2F,1.15F+random.nextFloat()*.1F);
		if(--left<=0)pose(MojitoRules.WATCH,gap());
	}

	/**
	 * Rump swung round to his prey through the tell, forepaws scrabbling; then digging away like mad, flinging clods of earth back
	 * between his legs onto where they are headed, each spot marked before it lands. His face, not his rump, is the place to be.
	 */
	private void dig(ServerLevel level,LivingEntity target){
		still();long el=since();int tell=tell();
		if(target!=null)turn(yawTo(target.position())+180,el<tell?15:(float)MojitoRules.DIG_TURN);
		aim=getYRot();
		var paws=position().add(ahead(aim).scale(1.6));
		if(el%2==0)level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,paws)),paws.x,paws.y+.3,paws.z,5,.6,.2,.6,.15);
		if(el%3==0)sound(level,paws,SoundEvents.ROOTED_DIRT_HIT,1.4F,.6F+random.nextFloat()*.2F);
		if(el<tell)return;
		long k=el-tell;
		if(k%MojitoRules.DIG_EVERY==0 && target!=null){
			var from=position().subtract(ahead(aim).scale(2.5)).add(0,1.5,0);
			var to=target.position().add(preyVel.scale(6)).add((random.nextDouble()*2-1)*1.5,0,(random.nextDouble()*2-1)*1.5);
			var flat=to.subtract(from).multiply(1,0,1);
			if(flat.length()>MojitoRules.DIG_RANGE){flat=flat.normalize().scale(MojitoRules.DIG_RANGE);to=new Vec3(from.x+flat.x,to.y,from.z+flat.z);}
			to=floor(level,to);
			clods.add(new Clod(from,to,level.getGameTime(),3+.25*flat.length()));
			sound(level,from,SoundEvents.ROOTED_DIRT_BREAK,1.6F,.7F+random.nextFloat()*.2F);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.DIRT.defaultBlockState()),from.x,from.y,from.z,10,.4,.3,.4,.15);
		}
		if(k>=MojitoRules.DIG_TICKS){sound(level,paws,SoundEvents.SNIFFER_DIGGING_STOP,1.6F,.6F);after(level,MojitoRules.DIG,MojitoRules.DIG_RECOVER);}
	}
	/** The ground under {@code at}, looking a little up and down from it. */
	private Vec3 floor(ServerLevel level,Vec3 at){
		var hit=level.clip(new ClipContext(at.add(0,3,0),at.add(0,-6,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
		return hit.getType()==HitResult.Type.MISS?at:hit.getLocation();
	}
	/** The clods in the air, arcing down onto their marks, a ring of earth closing on each mark; bursting where they land. */
	private void clods(ServerLevel level){
		if(clods.isEmpty())return;
		long now=level.getGameTime();
		var prey=prey(level);
		var dirt=new BlockParticleOption(ParticleTypes.BLOCK,Blocks.DIRT.defaultBlockState());
		for(var it=clods.iterator();it.hasNext();){
			var c=it.next();
			double t=(now-c.born())/(double)MojitoRules.DIG_FLIGHT;
			var to=c.to();
			if(t<1){
				var at=c.from().lerp(to,t).add(0,c.peak()*4*t*(1-t),0);
				level.sendParticles(dirt,at.x,at.y,at.z,3,.2,.2,.2,0);
				level.sendParticles(EARTH,at.x,at.y,at.z,1,.15,.15,.15,0);
				ring(level,to,MojitoRules.DIG_RADIUS*(1+(1-t)*.8),(now-c.born())%2==0?EARTH:MINT_SMALL,10);
				continue;
			}
			sound(level,to,SoundEvents.GRAVEL_BREAK,1.6F,.6F+random.nextFloat()*.2F);sound(level,to,SoundEvents.ROOTED_DIRT_FALL,1.4F,.6F);
			level.sendParticles(dirt,to.x,to.y+.3,to.z,24,.6,.3,.6,.15);
			level.sendParticles(ParticleTypes.POOF,to.x,to.y+.3,to.z,6,.5,.2,.5,.02);
			for(var p:prey)if(MojitoRules.clods(to.x-p.getX(),to.y-p.getY(),to.z-p.getZ())){
				hit(level,p,MojitoRules.DIG_DAMAGE,.3,to,.15);
				p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,MojitoRules.DIG_SLOW,1,false,true),this);
			}
			it.remove();
		}
	}
	/**
	 * Magic welling up on both flanks through the tell as he turns to face his prey; then three volleys, each its own: a pair loosed
	 * straight at them, a pair loosed at where they are running to, and a fan of them spread wide off his head.
	 */
	private void bolts(ServerLevel level,LivingEntity target){
		still();
		if(target!=null)turn(yawTo(target.position()),8);
		long el=since();int tell=tell();
		if(el<tell){
			if(el%2==0)for(int s=-1;s<=1;s+=2){var at=flank(s);level.sendParticles(MINT,at.x,at.y,at.z,1+(int)(el/4),.2+el*.02,.2+el*.02,.2+el*.02,0);}
			return;
		}
		long k=el-tell;int every=MojitoRules.BOLT_EVERY;
		int volley=(int)(k/every);
		// Each volley gathering where it will come from: both flanks, then both flanks brighter, then his head.
		if(volley<MojitoRules.BOLT_VOLLEYS && k%every>every-5){
			if(volley<2)for(int s=-1;s<=1;s+=2){var at=flank(s);level.sendParticles(volley==0?MINT:ParticleTypes.ELECTRIC_SPARK,at.x,at.y,at.z,3,.3,.3,.3,0);}
			else{var h=head();level.sendParticles(MINT,h.x,h.y+.4,h.z,4,.8,.3,.8,0);}
		}
		if(k%every==0 && volley<MojitoRules.BOLT_VOLLEYS){
			var up=position().add(0,3,0);
			var to=target==null?position().add(ahead(getYRot()).scale(10)).add(0,1,0):target.position().add(0,target.getBbHeight()*.5,0);
			switch(volley){
				case 0->{
					sound(level,up,SoundEvents.EVOKER_CAST_SPELL,1.8F,1.1F);sound(level,up,SoundEvents.BREEZE_SHOOT,1.4F,.8F);
					for(int s=-1;s<=1;s+=2)loose(level,flank(s),to);
				}
				case 1->{
					sound(level,up,SoundEvents.ILLUSIONER_CAST_SPELL,1.8F,1.2F);sound(level,up,SoundEvents.BREEZE_SHOOT,1.4F,1F);
					double d=target==null?10:Math.sqrt(target.distanceToSqr(getX(),target.getEyeY(),getZ()));
					var led=to.add(preyVel.scale(Math.min(d/MojitoRules.BOLT_SPEED,30)));
					for(int s=-1;s<=1;s+=2)loose(level,flank(s),led);
				}
				default->{
					sound(level,up,SoundEvents.AMETHYST_CLUSTER_BREAK,2F,.6F);sound(level,up,SoundEvents.BREEZE_SHOOT,1.6F,.6F);
					var from=head();var v=to.subtract(from);
					double flat=Math.sqrt(v.x*v.x+v.z*v.z),yaw=Math.atan2(v.z,v.x);
					for(int i=0;i<MojitoRules.FAN;i++){
						double a=yaw+Math.toRadians(MojitoRules.FAN_SPREAD*(2.0*i/(MojitoRules.FAN-1)-1));
						var dir=new Vec3(Math.cos(a)*flat,v.y,Math.sin(a)*flat);
						loose(level,from,from.add(dir));
					}
				}
			}
		}
		if(k>=(long)(MojitoRules.BOLT_VOLLEYS-1)*every+6)after(level,MojitoRules.BOLTS,MojitoRules.BOLT_RECOVER);
	}
	/** A bolt loosed from {@code from} toward {@code to}. */
	private void loose(ServerLevel level,Vec3 from,Vec3 to){
		var v=to.subtract(from);
		if(v.lengthSqr()<1e-4)v=ahead(getYRot());
		bolts.add(new Bolt(from,v.normalize().scale(MojitoRules.BOLT_SPEED)));
		level.sendParticles(ParticleTypes.END_ROD,from.x,from.y,from.z,6,.2,.2,.2,.05);
	}
	/**
	 * Crowded, or hit once too often: a shiver running through his coat for the tell, then a great shake from nose to tail that
	 * flings water and magic all about and knocks back whoever is close, before or behind.
	 */
	private void shake(ServerLevel level){
		still();long el=since();int tell=tell();
		if(el<tell){
			face(getYRot()+(el%2==0?3:-3));
			if(el%2==0)level.sendParticles(ParticleTypes.SPLASH,getX(),getY()+3,getZ(),8,1.4,1,1.4,0);
			if(el%3==0)level.sendParticles(MINT_SMALL,getX(),getY()+2.5,getZ(),6,1.6,1.2,1.6,0);
			if(el%4==0)sound(level,position(),SoundEvents.WOLF_STEP.value(),1F,.5F);
			return;
		}
		if(el==tell){
			sound(level,position(),SoundEvents.WOLF_SHAKE,2.4F,.5F);sound(level,position(),SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),1.8F,.7F);
			level.sendParticles(ParticleTypes.SPLASH,getX(),getY()+3,getZ(),80,2.5,1.5,2.5,.4);
			level.sendParticles(ParticleTypes.POOF,getX(),getY()+2,getZ(),30,2,1,2,.15);
			ring(level,position(),MojitoRules.SHAKE_RADIUS,MINT,32);
			double r=MojitoRules.SHAKE_RADIUS;
			for(var p:prey(level))if(p.distanceToSqr(getX(),p.getY(),getZ())<=r*r && Math.abs(p.getY()-getY())<5)
				hit(level,p,MojitoRules.SHAKE_DAMAGE,MojitoRules.SHAKE_PUSH,position(),MojitoRules.SHAKE_LIFT);
		}
		if(el>=tell+6){blows.clear();crowd=0;spent(MojitoRules.SHAKE_RECOVER);}
	}
	/**
	 * Half beaten, he sits back and howls: magic rising round him through the tell, then ring after ring of it running out along
	 * the ground (jump them). From then on he is wild.
	 */
	private void howl(ServerLevel level){
		still();long el=since();int tell=tell();
		var h=head();
		if(el<tell){
			if(el%2==0)level.sendParticles(MINT,getX(),getY()+.5+el*.25,getZ(),6,1.5,.3,1.5,0);
			level.sendParticles(MINT_SMALL,h.x,h.y,h.z,2,.6,.6,.6,0);
			return;
		}
		long k=el-tell;int every=MojitoRules.HOWL_EVERY;
		if(k%every==0 && k/every<MojitoRules.HOWL_RINGS){
			int i=(int)(k/every);
			sound(level,h,voice().whineSound().value(),3F,.45F+i*.08F);
			sound(level,h,i==0?SoundEvents.EVOKER_PREPARE_ATTACK:SoundEvents.AMETHYST_BLOCK_RESONATE,2F,.5F+i*.1F);
			level.sendParticles(ParticleTypes.SONIC_BOOM,h.x,h.y+.5,h.z,1,0,0,0,0);
			level.sendParticles(ParticleTypes.END_ROD,h.x,h.y,h.z,12,.5,.5,.5,.1);
			waves.add(new Wave(position(),MojitoRules.WAVE_SPEED,MojitoRules.HOWL_REACH,MojitoRules.WAVE_DAMAGE,MojitoRules.WAVE_LIFT,level.getGameTime()));
		}
		if(k>=(long)(MojitoRules.HOWL_RINGS-1)*every+6){pose(MojitoRules.WATCH,4);growl(level,1.6F);}
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
	 * whoever is in his path and leaving a trail of magic, the circles widening or tightening after his prey. Now and then he skids
	 * round to face them, a bark and the line he will take marked out, and cuts straight across; then back to his circles, and at
	 * last a flop.
	 */
	private void zoomies(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<tell){still();face(aim);if(el%6==0)sound(level,head(),voice().pantSound().value(),1F,.7F);return;}
		long now=level.getGameTime(),z=el-tell;
		if(z==0){
			zoomAt=spot();zoomWay=random.nextBoolean()?1:-1;
			zoomRadius=target==null?MojitoRules.ZOOM_MIN_RADIUS:MojitoRules.zoomRadius(Math.sqrt(target.distanceToSqr(zoomAt.x,target.getY(),zoomAt.z)));
			zoomAngle=Math.atan2(getZ()-zoomAt.z,getX()-zoomAt.x);
		}
		int skid=MojitoRules.DASH_SKID,run=MojitoRules.DASH_TICKS;
		if(dashAt<0 && target!=null && z>=25 && (z-25)%MojitoRules.DASH_EVERY==0 && z+skid+run<MojitoRules.ZOOM_TICKS){dashAt=el;bark(level,2F,1F);}
		if(dashAt>=0){
			long d=el-dashAt;
			if(d<skid){
				// Skidding round to face them, turf flying, the line he will take marked out.
				if(target!=null)turn(yawTo(target.position()),30);
				aim=getYRot();
				var drift=getDeltaMovement().multiply(.5,0,.5);
				move(MoverType.SELF,new Vec3(drift.x,-.3,drift.z));setDeltaMovement(Vec3.ZERO);
				level.sendParticles(ParticleTypes.POOF,getX(),getY()+.2,getZ(),4,1,.1,1,.04);
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,position())),getX(),getY()+.2,getZ(),6,1,.1,1,.15);
				if(d%2==0)sound(level,position(),SoundEvents.GRAVEL_HIT,1.6F,.6F);
				var f=ahead(aim);
				for(double r=2;r<=MojitoRules.DASH_SPEED*run;r+=1.5){var at=position().add(f.scale(r));level.sendParticles(MINT_SMALL,at.x,at.y+.2,at.z,1,.1,0,.1,0);}
				if(d==skid-1)dashStep=f.scale(MojitoRules.DASH_SPEED);
			}else if(d<skid+run && !(d>skid && horizontalCollision)){
				face(aim);
				move(MoverType.SELF,new Vec3(dashStep.x,-.3,dashStep.z));setDeltaMovement(Vec3.ZERO);
				if(d==skid)sound(level,position(),SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),1.6F,.8F);
				if(d%2==0){sound(level,position(),SoundEvents.WOLF_STEP.value(),1.8F,.7F);trail.add(new Spot(getX(),getY(),getZ(),now+MojitoRules.TRAIL_TICKS));}
				level.sendParticles(ParticleTypes.CLOUD,getX(),getY()+.5,getZ(),3,.8,.2,.8,.02);
			}else{
				// Back round onto a circle from wherever the dash left him.
				dashAt=-1;
				zoomAngle=Math.atan2(getZ()-zoomAt.z,getX()-zoomAt.x);
				zoomRadius=MojitoRules.zoomRadius(Math.sqrt(distanceToSqr(zoomAt.x,getY(),zoomAt.z)));
			}
		}else{
			double want=target==null?MojitoRules.ZOOM_MIN_RADIUS:MojitoRules.zoomRadius(Math.sqrt(target.distanceToSqr(zoomAt.x,target.getY(),zoomAt.z)));
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
		}
		level.sendParticles(MINT,getX(),getY()+2,getZ(),3,1.2,.8,1.2,0);
		boolean skidding=dashAt>=0 && el-dashAt<skid;
		if(!skidding)for(var p:prey(level)){
			long last=zoomStruck.getOrDefault(p.getUUID(),-999L);
			if(now-last>=MojitoRules.ZOOM_REHIT && reaches(MojitoRules.ZOOMIES,p)){zoomStruck.put(p.getUUID(),now);hit(level,p,MojitoRules.ZOOM_DAMAGE,1.8,position().subtract(ahead(aim)),.45);}
		}
		if(z>=MojitoRules.ZOOM_TICKS){
			dashAt=-1;pose(MojitoRules.FLOP,MojitoRules.ZOOM_FLOP);still();
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
	/** Ticks before his next blow; fewer once he is wild. */
	private int gap(){return (wild?MojitoRules.GAP_WILD:MojitoRules.GAP)+random.nextInt(3);}
	/** How many blows players have landed on him within the shake window up to {@code now}. */
	private int blows(long now){
		while(!blows.isEmpty() && now-blows.peekFirst()>MojitoRules.SHAKE_WINDOW)blows.pollFirst();
		return blows.size();
	}

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
			if(source.getDirectEntity()!=null && source.getDirectEntity()==source.getEntity() && MojitoRules.dodges(move,now-lastMade[MojitoRules.HOP],random.nextDouble())){
				lastMade[MojitoRules.HOP]=now;hopFrom(level,at==null?position().add(ahead(getYRot())):at);
				return false;
			}
			damage*=(float)MojitoRules.armour(move,frontal,move==MojitoRules.CHOP && since()<tell());
		}
		boolean hurt=super.hurtServer(level,source,damage);
		if(hurt){lastCut=now;if(source.getEntity() instanceof ServerPlayer)blows.addLast(now);}
		return hurt;
	}
	@Override public void die(DamageSource source){
		if(level() instanceof ServerLevel server){
			sound(server,head(),voice().deathSound().value(),2F,.5F);
			Guardians.reward(server,this,MojitoRules.TIER,bar,source);
			bar.removeAllPlayers();bolts.clear();trail.clear();waves.clear();clods.clear();entityData.set(THROWN,false);
		}
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
		out.putBoolean("mojito_howled",wild);
	}
	@Override protected void readAdditionalSaveData(ValueInput in){
		super.readAdditionalSaveData(in);
		bound=in.getBooleanOr("mojito_bound",false);
		bed=in.read("mojito_bed",BlockPos.CODEC).orElse(blockPosition());rest=in.getFloatOr("mojito_rest",getYRot());
		groveMin=in.read("mojito_grove_min",BlockPos.CODEC).orElse(bed);groveMax=in.read("mojito_grove_max",BlockPos.CODEC).orElse(bed);
		lastTick=in.getLongOr("mojito_ticked",-1L);
		wild=in.getBooleanOr("mojito_howled",false);
		if(wild)bar.setColor(BossEvent.BossBarColor.GREEN);
		// Mid-fight he comes back on his feet, whatever he was doing when the chunk unloaded.
		if(in.getBooleanOr("mojito_awake",false)){pose(MojitoRules.WATCH,MojitoRules.GAP);lastCut=0;}else pose(MojitoRules.DORMANT,0);
	}
}
