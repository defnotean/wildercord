package dev.wildercord.aura.world;

import dev.wildercord.cast.MoveDeck;
import dev.wildercord.cast.feel.Feels;
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
 * The Ashen Stag ({@link AshenStagRules}). Generation leaves it bedded in the ash before the Ashen Glade's smouldering stump, bound
 * to that spot and to the glade, facing the gate. While it lies there it cannot be hurt; the first player far enough into the glade
 * (or the first blow) gets it up, hindquarters first, bellowing once its forelegs are under it, and then the boss bar comes up. The
 * glade going quiet sends it back to its bed, healed. The glade is its own: anything hostile in it, it burns out of the world.
 * <p>
 * On its feet it picks its next move from what suits where its prey stands, never the one it just made while another will do, and
 * each comes with its own tell. Head low and twisted, tines scraping sparks, it tosses whoever is square in front of it; tail
 * flagged, it bucks at whoever is at its back and kicks embers out behind; reared up with its belly bare, it stamps a ring of fire
 * out across the ash; it charges anyone keeping away, leaving a burning wake, and skids round at the end with its flank turned (a
 * charge into a trunk or the bank sticks its antlers fast); it rakes cinders down on where its prey is going; it calls a herd of
 * ember deer across the glade in lanes; it shakes a pall of blinding smoke off its antlers and stalks out of it to lunge; and it
 * locks its antlers on whoever it catches and grinds them, until it throws them or two blows to its throat wrench it free. It bounds
 * aside out from under some blows, and hurt too fast it leaps away back across the glade. Half beaten it kindles, once: fire climbs
 * its antlers for good, and it comes on quicker, runs its moves on in pairs, and most of them grow worse.
 */
public final class AshenStag extends PathfinderMob implements Enemy {
	private static final EntityDataAccessor<Integer> MOVE=SynchedEntityData.defineId(AshenStag.class,EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN=SynchedEntityData.defineId(AshenStag.class,EntityDataSerializers.LONG);
	/** The current move's tell in ticks, so the client knows when the blow lands. */
	private static final EntityDataAccessor<Integer> TELL=SynchedEntityData.defineId(AshenStag.class,EntityDataSerializers.INT);
	/** Which way a bound carries it, or a toss twists, for the client: -1 left, 1 right, 0 straight back. */
	private static final EntityDataAccessor<Integer> SIDE=SynchedEntityData.defineId(AshenStag.class,EntityDataSerializers.INT);
	/** Whether it has kindled, so the client can set its antlers alight. */
	private static final EntityDataAccessor<Boolean> KINDLED=SynchedEntityData.defineId(AshenStag.class,EntityDataSerializers.BOOLEAN);
	private static final EntityDimensions BEDDED=EntityDimensions.scalable(2.4F,2.0F).withEyeHeight(1.7F),STANDING=EntityDimensions.scalable(2.4F,3.6F).withEyeHeight(3.0F);
	private static final DustParticleOptions EMBER=new DustParticleOptions(0xFF6A1A,1.4F),EMBER_SMALL=new DustParticleOptions(0xFFA040,.8F),SOOT=new DustParticleOptions(0x2A2420,1.6F);
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
	private long lastDodge=-999,lastLeap=-999;
	/** Hurt taken lately, fading; enough of it sends it leaping away. */
	private double stung;
	/** Whoever this blow has already caught. */
	private final Set<UUID> struck=new HashSet<>();
	/** Its memory of its last few moves, when it last made each, how many it has run on into one after another, whether this one was
	 *  run on into, and how many times it has gone again within this one. */
	private final MoveDeck deck=new MoveDeck(5);
	private final long[] lastMade=new long[32];{Arrays.fill(lastMade,-9999);}
	private int links,again;
	private boolean chained;
	/** Which way its prey has been moving lately, and where they stood last tick. */
	private Vec3 preyVel=Vec3.ZERO,preyLast;
	/** The Cinder Rush: runs made so far, and where this run set off from. */
	private int runs;
	private Vec3 runFrom=Vec3.ZERO;
	/** A strip of embers left along a charge's track, or a patch where a cinder came down, smouldering until {@code until}. */
	private record Strip(double ax,double az,double bx,double bz,double y,long until){}
	private final List<Strip> strips=new ArrayList<>();
	/** A ring of fire running out along the ground from a stamp. */
	private static final class Wave{final double x,y,z,speed,max,damage,lift;final long born;final Set<UUID> struck=new HashSet<>();
		Wave(Vec3 at,double speed,double max,double damage,double lift,long born){x=at.x;y=at.y;z=at.z;this.speed=speed;this.max=max;this.damage=damage;this.lift=lift;this.born=born;}}
	private final List<Wave> waves=new ArrayList<>();
	/** A cinder raked up, arcing from {@code from} down onto {@code to}. */
	private record Cinder(Vec3 from,Vec3 to,long born,double peak){}
	private final List<Cinder> cinders=new ArrayList<>();
	/** An ember deer of the herd, running {@code dir} across the lane at {@code centre} along the herd's facing {@code f} from {@code o},
	 *  {@code r} its right; setting off at {@code born}, its lane marked before; {@code lead} the first down its lane. */
	private record Deer(Vec3 o,Vec3 f,Vec3 r,double centre,int dir,long born,boolean lead,Set<UUID> struck){}
	private final List<Deer> herd=new ArrayList<>();
	/** The pall of smoke, where it hangs and until when. */
	private Vec3 smokeAt;
	private long smokeUntil;
	/** Whoever its antlers are locked on, when they were caught, and the blows to its front since. */
	private UUID locked;
	private long lockedAt;
	private int lockBlows;

	public AshenStag(EntityType<? extends AshenStag> type,Level level){super(type,level);xpReward=AshenStagRules.TIER.xp;setPersistenceRequired();}
	public static AttributeSupplier.Builder createAttributes(){
		return createMobAttributes().add(Attributes.MAX_HEALTH,AshenStagRules.HEALTH).add(Attributes.ARMOR,AshenStagRules.ARMOUR).add(Attributes.ARMOR_TOUGHNESS,AshenStagRules.TIER.toughness)
			.add(Attributes.MOVEMENT_SPEED,AshenStagRules.SPEED).add(Attributes.ATTACK_DAMAGE,AshenStagRules.TIER.melee).add(Attributes.FOLLOW_RANGE,40).add(Attributes.KNOCKBACK_RESISTANCE,1)
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
	public boolean kindled(){return entityData.get(KINDLED);}
	public float elapsed(float partial){return (float)(level().getGameTime()-entityData.get(BEGAN))+partial;}
	private long since(){return level().getGameTime()-entityData.get(BEGAN);}
	private Vec3 spot(){return Vec3.atBottomCenterOf(bed);}

	@Override protected void defineSynchedData(SynchedEntityData.Builder b){
		super.defineSynchedData(b);b.define(MOVE,AshenStagRules.DORMANT);b.define(BEGAN,0L);b.define(TELL,0);b.define(SIDE,0);b.define(KINDLED,false);
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
	/** Back down in its bed, whole again, its fire banked and the fight forgotten. */
	private void bedDown(){
		pose(AshenStagRules.DORMANT,0);hold();setHealth(getMaxHealth());setTarget(null);bar.removeAllPlayers();quiet=0;clearFire();
		entityData.set(KINDLED,false);bar.setColor(BossEvent.BossBarColor.RED);forget();
	}
	/** Every move and everything it left about the glade, forgotten. */
	private void forget(){
		deck.clear();Arrays.fill(lastMade,-9999);links=0;again=0;strips.clear();waves.clear();cinders.clear();herd.clear();smokeAt=null;locked=null;
		preyLast=null;preyVel=Vec3.ZERO;
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

	/** Its antlers smoulder whatever it is doing; on its feet, its cracks shed sparks. Stalking, its glow is banked; kindled, its antlers burn. */
	private void embers(ServerLevel level){
		var h=head();int move=move();
		level.sendParticles(ParticleTypes.SMOKE,h.x,h.y+.9,h.z,1,.5,.2,.5,.01);
		if(move==AshenStagRules.STALK)return;
		if(random.nextInt(3)==0)level.sendParticles(ParticleTypes.SMALL_FLAME,h.x,h.y+1,h.z,1,.5,.3,.5,.005);
		if(!AshenStagRules.scripted(move) && random.nextInt(2)==0)level.sendParticles(ParticleTypes.WHITE_ASH,getX(),getY()+1.8,getZ(),3,.8,.5,.8,0);
		if(kindled()){
			level.sendParticles(ParticleTypes.FLAME,h.x,h.y+1.1,h.z,3,.6,.4,.6,.01);
			if(random.nextInt(3)==0)level.sendParticles(ParticleTypes.LAVA,h.x,h.y+1,h.z,1,.4,.2,.4,0);
		}
		// Rushing, its cracks flare and it trails fire.
		if(move==AshenStagRules.RUSH){
			level.sendParticles(ParticleTypes.FLAME,getX(),getY()+1.6,getZ(),4,.7,.5,1.0,.01);
			level.sendParticles(ParticleTypes.LAVA,h.x,h.y+1,h.z,1,.4,.2,.4,0);
		}
	}
	/** Its bellow: the sound, and a breath of sparks and smoke off its antlers. */
	private void bellow(ServerLevel level,float volume,float pitch){
		var h=head();
		Feels.sound(level,h,"aura_ashen_stag_bellow",volume,pitch);
		level.sendParticles(ParticleTypes.LAVA,h.x,h.y+1,h.z,6,.6,.3,.6,0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE,h.x,h.y+.8,h.z,10,.6,.3,.6,.02);
		var f=ahead(getYRot());level.sendParticles(ParticleTypes.FLAME,h.x+f.x,h.y,h.z+f.z,12,.3,.2,.3,.05);
	}
	/** Its head, more or less: low over the ash in its bed, high reared up, low with its antlers levelled, up by its shoulders otherwise. */
	private Vec3 head(){
		int move=move();var f=ahead(getYRot());
		if(move==AshenStagRules.DORMANT)return position().add(f.scale(1.3)).add(0,1.5,0);
		boolean high=move==AshenStagRules.REAR || move==AshenStagRules.KINDLE;
		boolean low=move==AshenStagRules.TOSS || move==AshenStagRules.LOCK || move==AshenStagRules.CHARGE || move==AshenStagRules.RUSH
			|| move==AshenStagRules.STALK || move==AshenStagRules.RAKE;
		return position().add(f.scale(high?1.0:low?2.2:1.7)).add(0,high?4.0:low?2.0:3.0,0);
	}
	private void sound(ServerLevel level,Vec3 at,SoundEvent s,float volume,float pitch){level.playSound(null,at.x,at.y,at.z,s,SoundSource.HOSTILE,volume,pitch);}
	private void ring(ServerLevel level,Vec3 at,double r,ParticleOptions dust,int n){
		for(int i=0;i<n;i++){double a=i*Math.PI*2/n;level.sendParticles(dust,at.x+Math.cos(a)*r,at.y+.15,at.z+Math.sin(a)*r,1,0,0,0,0);}
	}
	private BlockState ground(ServerLevel level,Vec3 at){
		var s=level.getBlockState(BlockPos.containing(at.x,at.y-.5,at.z));return s.isAir()?Blocks.DIRT.defaultBlockState():s;
	}
	/** The ground under {@code at}, or {@code at} itself when there is none near. */
	private Vec3 floor(ServerLevel level,Vec3 at){
		var hit=level.clip(new ClipContext(at.add(0,3,0),at.add(0,-6,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
		return hit.getType()==HitResult.Type.MISS?at:hit.getLocation();
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
		deck.clear();links=0;
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
			var at=spot();teleportTo(at.x,at.y,at.z);setDeltaMovement(Vec3.ZERO);getNavigation().stop();locked=null;pose(AshenStagRules.WATCH,AshenStagRules.GAP);
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
		// Which way its prey has been going lately, for leading its cinders.
		if(target!=null){var at=target.position();
			if(preyLast!=null){var v=preyVel.scale(.7).add(at.subtract(preyLast).multiply(1,0,1).scale(.3));preyVel=v.length()>1?v.normalize():v;}
			preyLast=at;}else{preyLast=null;preyVel=Vec3.ZERO;}
		smoulder(level);waves(level);cinders(level);herd(level);pall(level);
		switch(move()){
			case AshenStagRules.TOSS->toss(level,target);
			case AshenStagRules.BUCK->buck(level,target);
			case AshenStagRules.REAR->rear(level,target);
			case AshenStagRules.CHARGE->charge(level,target);
			case AshenStagRules.SKID->skidding(level,target);
			case AshenStagRules.RUSH->rush(level,target);
			case AshenStagRules.RAKE->rake(level,target);
			case AshenStagRules.STAMPEDE->stampede(level);
			case AshenStagRules.SMOKE->smoke(level);
			case AshenStagRules.STALK->stalk(level,target);
			case AshenStagRules.LUNGE->lunge(level,target);
			case AshenStagRules.LOCK->lock(level,target);
			case AshenStagRules.STAGGER->reel(level);
			case AshenStagRules.KINDLE->kindle(level);
			case AshenStagRules.BOUND->{face(aim);move(MoverType.SELF,new Vec3(slide.x,-.3,slide.z));setDeltaMovement(Vec3.ZERO);if(--left<=0)pose(AshenStagRules.WATCH,2);}
			case AshenStagRules.STUCK->{still();stuck(level);if(--left<=0)free(level);}
			case AshenStagRules.RECOVER->{still();if(--left<=0)pose(AshenStagRules.WATCH,gap());}
			default->watch(level,target);
		}
	}

	/** On its feet and tracking: closing on its prey, turning only so fast, so its back can be got at, and picking its next move. */
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
			// Half beaten for the first time, it kindles before anything else.
			if(AshenStagRules.kindles(getHealth(),getMaxHealth(),kindled())){links=0;begin(level,AshenStagRules.KINDLE,target,false);return;}
			long[] since=new long[lastMade.length];for(int i=0;i<since.length;i++)since[i]=now-lastMade[i];
			int pick=AshenStagRules.choose(deck,d,behind,target.getY()-getY(),since,kindled(),random.nextDouble());
			// A long chase ends in a charge from further out.
			if(pick<0 && chased>=60 && d>=5 && d<=AshenStagRules.CHARGE_REACH+3 && since[AshenStagRules.CHARGE]>=AshenStagRules.rest(AshenStagRules.CHARGE,kindled())/2)pick=AshenStagRules.CHARGE;
			if(pick>=0){links=0;begin(level,pick,target,false);return;}
		}
		if(d>AshenStagRules.CLOSE){
			chased++;
			if(!getNavigation().moveTo(target,1))getMoveControl().setWantedPosition(target.getX(),target.getY(),target.getZ(),1);
		}else{chased=0;getNavigation().stop();turn(yawTo(target.position()),10);}
	}

	/** Sets off on {@code move} at {@code target}, {@code chained} when run straight on into from the last one (a shorter tell). */
	private void begin(ServerLevel level,int move,LivingEntity target,boolean chained){
		long now=level.getGameTime();lastMade[move]=now;if(move!=AshenStagRules.KINDLE)deck.played(move);
		chased=0;struck.clear();this.chained=chained;again=0;
		if(target!=null)aim=yawTo(target.position());
		var h=head();
		switch(move){
			case AshenStagRules.TOSS->{
				entityData.set(SIDE,random.nextBoolean()?1:-1);pose(move,0,chained?AshenStagRules.TOSS_TELL_CHAINED:AshenStagRules.TOSS_TELL);
				sound(level,h,SoundEvents.GRINDSTONE_USE,1.2F,.6F);Feels.sound(level,h,"aura_ashen_stag_snort",1.2F,1.1F);
			}
			case AshenStagRules.BUCK->{
				aim=getYRot();entityData.set(SIDE,0);pose(move,0,chained?AshenStagRules.BUCK_TELL_CHAINED:AshenStagRules.BUCK_TELL);
				Feels.sound(level,position(),"aura_ashen_stag_snort",1.2F,.85F);
			}
			case AshenStagRules.REAR->{pose(move,0,chained?AshenStagRules.REAR_TELL_CHAINED:AshenStagRules.REAR_TELL);bellow(level,1.2F,1.2F);}
			case AshenStagRules.CHARGE->{
				if(AshenStagRules.rushes(kindled(),chained)){
					runs=0;pose(AshenStagRules.RUSH,0,AshenStagRules.RUSH_TELL);
					bellow(level,1.8F,.85F);Feels.sound(level,position(),"aura_ashen_stag_paw",1.4F,.9F);
				}else{pose(move,0,chained?AshenStagRules.CHARGE_TELL_CHAINED:AshenStagRules.CHARGE_TELL);Feels.sound(level,h,"aura_ashen_stag_paw",1.4F,1);}
			}
			case AshenStagRules.RAKE->{pose(move,0,AshenStagRules.RAKE_TELL);sound(level,h,SoundEvents.GRAVEL_BREAK,1.4F,.7F);Feels.sound(level,h,"aura_ashen_stag_snort",1.2F,1);}
			case AshenStagRules.STAMPEDE->{pose(move,0,AshenStagRules.HERD_TELL);call(level,target);bellow(level,2F,.7F);}
			case AshenStagRules.SMOKE->{pose(move,0,AshenStagRules.SMOKE_TELL);sound(level,h,SoundEvents.WOLF_SHAKE,1.6F,.5F);Feels.sound(level,h,"aura_ashen_stag_snort",1.2F,.8F);}
			case AshenStagRules.LOCK->{
				locked=null;lockBlows=0;pose(move,0,AshenStagRules.LOCK_TELL);
				sound(level,h,SoundEvents.GOAT_PREPARE_RAM,1.6F,.6F);Feels.sound(level,h,"aura_ashen_stag_snort",1.3F,.9F);
			}
			case AshenStagRules.KINDLE->{
				entityData.set(KINDLED,true);bar.setColor(BossEvent.BossBarColor.YELLOW);pose(move,0,AshenStagRules.KINDLE_TELL);
				locked=null;bellow(level,2F,.8F);
			}
			default->pose(AshenStagRules.WATCH,gap());
		}
	}
	/** Done with {@code move}: kindled, perhaps straight on into another; otherwise spent {@code recover} ticks. */
	private void after(ServerLevel level,int move,int recover){
		var target=getTarget();
		if(kindled() && target!=null && links<AshenStagRules.CHAIN_MOST){
			int next=AshenStagRules.chain(move,distanceTo(target),random.nextDouble());
			if(next>=0){links++;begin(level,next,target,true);return;}
		}
		spent(links>0?recover+AshenStagRules.CHAIN_REST:recover);links=0;
	}

	/**
	 * The toss. Head dropped low and twisted to one side, the low tine scraping sparks off the ash, turning hard after its prey until
	 * the aim locks; then wrenched up through whoever is square in front of it, and held up a moment, its chest bare.
	 */
	private void toss(ServerLevel level,LivingEntity target){
		still();long el=since();int tell=tell(),s=side();
		if(el<=tell-AshenStagRules.TOSS_LOCK && target!=null){turn(yawTo(target.position()),(float)AshenStagRules.TOSS_TURN);aim=getYRot();}else face(aim);
		var f=ahead(aim);
		if(el<tell){
			var tine=position().add(f.scale(2.4)).add(-f.z*s*.9,.2,f.x*s*.9);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK,tine.x,tine.y,tine.z,3,.2,.05,.2,.08);
			if(el%2==0)level.sendParticles(ParticleTypes.SMALL_FLAME,tine.x,tine.y,tine.z,1,.15,.02,.15,.01);
			if(el%4==0)sound(level,tine,SoundEvents.GRINDSTONE_USE,.9F,.6F+el/(float)tell*.6F);
		}
		if(el==tell){
			var h=head();
			Feels.sound(level,h,"aura_ashen_stag_gore",1.5F,.9F+random.nextFloat()*.2F);sound(level,h,SoundEvents.PLAYER_ATTACK_SWEEP,1.4F,.7F);
			var at=position().add(f.scale(2.2)).add(0,1.4,0);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK,at.x,at.y,at.z,1,0,0,0,0);
			level.sendParticles(ParticleTypes.FLAME,at.x,at.y,at.z,16,.5,.8,.5,.06);
			for(var p:prey(level))if(reaches(AshenStagRules.TOSS,p) && struck.add(p.getUUID()))hit(level,p,AshenStagRules.TOSS_DAMAGE,.3,position(),.95);
		}
		if(el>=tell+AshenStagRules.TOSS_HOLD)after(level,AshenStagRules.TOSS,AshenStagRules.TOSS_RECOVER);
	}

	/**
	 * The buck. Tail flagged and head down to its knees, its weight thrown onto its forelegs; then both hind hooves lashed out behind it,
	 * kicking a fan of embers after them. Kindled, it wheels round on its forelegs and bucks again at whoever ran in behind it.
	 */
	private void buck(ServerLevel level,LivingEntity target){
		still();face(aim);long el=since();int tell=tell();
		var f=ahead(aim);var back=position().subtract(f.scale(2.2));
		if(el<tell){
			if(el%2==0)level.sendParticles(ParticleTypes.WHITE_ASH,back.x,back.y+1.4,back.z,4,.5,.3,.5,.02);
			if(el==tell/2)Feels.sound(level,position(),"aura_ashen_stag_paw",1.2F,1.2F);
			if(el>=tell-4)level.sendParticles(ParticleTypes.SMALL_FLAME,back.x,back.y+.6,back.z,2,.4,.3,.4,.01);
		}
		if(el==tell){
			Feels.sound(level,back,"aura_ashen_stag_kick",1.5F,.9F+random.nextFloat()*.2F);sound(level,back,SoundEvents.FIRECHARGE_USE,1.2F,.8F);
			level.sendParticles(ParticleTypes.WHITE_ASH,back.x,back.y+1,back.z,20,.6,.4,.6,.1);
			for(int i=0;i<18;i++){
				double a=Math.toRadians((random.nextDouble()*2-1)*AshenStagRules.FAN_ANGLE),r=1+random.nextDouble()*(AshenStagRules.FAN_REACH-1);
				double bx=-f.x*Math.cos(a)-f.z*Math.sin(a),bz=-f.z*Math.cos(a)+f.x*Math.sin(a);
				level.sendParticles(ParticleTypes.FLAME,getX()+bx*r,getY()+.6,getZ()+bz*r,1,.1,.1,.1,.02);
			}
			for(var p:prey(level)){
				if(!struck.add(p.getUUID()))continue;
				double dx=p.getX()-getX(),dz=p.getZ()-getZ(),fw=dx*f.x+dz*f.z,sd=dx*-f.z+dz*f.x,ht=p.getY()-getY();
				if(reaches(AshenStagRules.BUCK,p))hit(level,p,AshenStagRules.BUCK_DAMAGE,1.8,position(),.35);
				else if(AshenStagRules.fans(fw,sd,ht)){hit(level,p,AshenStagRules.FAN_DAMAGE,.5,back,.2);p.igniteForSeconds(2);}
			}
		}
		if(el>=tell+3){
			if(kindled() && again==0 && target!=null){
				again=1;struck.clear();turn(yawTo(target.position())+180,(float)AshenStagRules.BUCK_PIVOT);aim=getYRot();
				pose(AshenStagRules.BUCK,0,AshenStagRules.BUCK_RETELL);Feels.sound(level,position(),"aura_ashen_stag_snort",1.3F,1.1F);
				level.sendParticles(ParticleTypes.WHITE_ASH,getX(),getY()+.1,getZ(),14,.8,.1,.8,.06);
			}else after(level,AshenStagRules.BUCK,AshenStagRules.BUCK_RECOVER);
		}
	}

	/**
	 * The rear. Up on its hind legs, belly bare, tracking its prey until the aim locks, embers gathering at its forehooves and the
	 * ground where they will land ringed with flame; then both forehooves stamped down, crushing whoever is under them and sending a
	 * ring of fire running out across the ash. Kindled, it rears again and stamps a second ring.
	 */
	private void rear(ServerLevel level,LivingEntity target){
		still();long el=since();int tell=tell();boolean twice=kindled();
		int second=tell+AshenStagRules.REAR_AGAIN;
		boolean tracking=el<=tell-AshenStagRules.REAR_LOCK || (twice && el>tell+2 && el<=second-AshenStagRules.REAR_LOCK);
		if(tracking && target!=null){turn(yawTo(target.position()),12);aim=getYRot();}else face(aim);
		long to=el<=tell?tell:second;
		if(el<to && (el<=tell || twice)){
			var f=ahead(aim);var hooves=position().add(f.scale(1.2)).add(0,2.6,0);
			level.sendParticles(ParticleTypes.SMALL_FLAME,hooves.x,hooves.y,hooves.z,2,.3,.3,.3,.01);
			if(to-el<=8)ring(level,floor(level,position().add(f.scale(AshenStagRules.REAR_AHEAD))),AshenStagRules.REAR_RADIUS,EMBER,16);
		}
		if(el==tell || (twice && el==second))stamp(level);
		if(el>=tell+(twice?AshenStagRules.REAR_AGAIN:0)+4)after(level,AshenStagRules.REAR,AshenStagRules.REAR_RECOVER);
	}
	/** Both forehooves down ahead of it: whoever is under them crushed, and a ring of fire sent running out. */
	private void stamp(ServerLevel level){
		var f=ahead(aim);var at=floor(level,position().add(f.scale(AshenStagRules.REAR_AHEAD)));long now=level.getGameTime();
		Feels.sound(level,at,"aura_ashen_stag_stomp",1.7F,.9F+random.nextFloat()*.1F);sound(level,at,SoundEvents.MACE_SMASH_GROUND_HEAVY,1.8F,.8F);
		level.sendParticles(new BlockParticleOption(ParticleTypes.DUST_PILLAR,ground(level,at)),at.x,at.y+.1,at.z,30,1.2,.1,1.2,.2);
		ring(level,at,AshenStagRules.REAR_RADIUS*.7,EMBER,20);
		level.sendParticles(ParticleTypes.LAVA,at.x,at.y+.2,at.z,6,.6,.1,.6,0);
		level.sendParticles(ParticleTypes.WHITE_ASH,at.x,at.y+.2,at.z,30,1,.2,1,.1);
		var w=new Wave(at,AshenStagRules.WAVE_SPEED,AshenStagRules.WAVE_REACH,AshenStagRules.WAVE_DAMAGE,.45,now);
		for(var p:prey(level)){
			double dx=p.getX()-getX(),dz=p.getZ()-getZ();
			if(AshenStagRules.crushes(dx*f.x+dz*f.z,dx*-f.z+dz*f.x,p.getY()-getY())){hit(level,p,AshenStagRules.REAR_DAMAGE,.8,at,.4);w.struck.add(p.getUUID());}
		}
		waves.add(w);
	}
	/** The rings of fire running out across the ash; jump them. */
	private void waves(ServerLevel level){
		if(waves.isEmpty())return;
		long now=level.getGameTime();
		for(var it=waves.iterator();it.hasNext();){
			var w=it.next();double r0=(now-w.born-1)*w.speed,r1=(now-w.born)*w.speed;
			if(r0>w.max){it.remove();continue;}
			if(r1<=0)continue;
			var at=new Vec3(w.x,w.y,w.z);int n=Math.max(12,(int)(r1*5));
			for(int i=0;i<n;i++){double a=i*Math.PI*2/n+random.nextDouble()*.1;
				level.sendParticles(i%2==0?ParticleTypes.FLAME:EMBER,w.x+Math.cos(a)*r1,w.y+.2,w.z+Math.sin(a)*r1,1,.05,.1,.05,.01);}
			if((now-w.born)%3==0)sound(level,at,SoundEvents.FIRECHARGE_USE,.8F,.6F+(float)(r1/w.max)*.6F);
			for(var p:prey(level)){
				double d=Math.sqrt(p.distanceToSqr(w.x,p.getY(),w.z));
				if(!w.struck.contains(p.getUUID()) && AshenStagRules.crosses(d,r0,r1,p.getY()-w.y)){
					w.struck.add(p.getUUID());hit(level,p,w.damage,.8,at,w.lift);p.igniteForSeconds(2);
				}
			}
		}
	}

	/**
	 * The charge. Pawing the ash with its head down, smoke snorted from its nostrils, tracking its prey until the aim locks; then its
	 * whole weight run along that line, running down whoever is in the way and leaving a burning wake. Out of room it skids round with
	 * its flank turned; a trunk or the bank stops it short with its antlers driven in.
	 */
	private void charge(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell){
			still();
			if(el<=tell-AshenStagRules.CHARGE_LOCK && target!=null)aim=yawTo(target.position());
			face(aim);var h=head();
			if(el%5==0)Feels.sound(level,position(),"aura_ashen_stag_paw",1.2F,.9F+random.nextFloat()*.2F);
			if(el%4==0)level.sendParticles(ParticleTypes.WHITE_ASH,getX(),getY()+.1,getZ(),8,.4,0,.4,.05);
			if(el%3==0)level.sendParticles(ParticleTypes.LARGE_SMOKE,h.x,h.y-.3,h.z,2,.1,.05,.1,.03);
			if(el==tell)runFrom=position();
			return;
		}
		face(aim);
		var f=ahead(aim);var step=f.scale(AshenStagRules.CHARGE_REACH/AshenStagRules.CHARGE_TICKS);
		move(MoverType.SELF,new Vec3(step.x,-.3,step.z));setDeltaMovement(Vec3.ZERO);
		if(el%2==0)Feels.sound(level,position(),"aura_ashen_stag_step",1.3F,1.2F);
		level.sendParticles(ParticleTypes.FLAME,getX(),getY()+.2,getZ(),3,.5,.1,.5,.02);
		level.sendParticles(ParticleTypes.LARGE_SMOKE,getX(),getY()+.3,getZ(),2,.5,.1,.5,.01);
		for(var p:prey(level))if(!struck.contains(p.getUUID()) && reaches(AshenStagRules.CHARGE,p)){struck.add(p.getUUID());hit(level,p,AshenStagRules.CHARGE_DAMAGE,1.6,position().subtract(f),.4);}
		if(horizontalCollision){lay(level,AshenStagRules.EMBER_TICKS);stick(level);return;}
		if(el>=tell+AshenStagRules.CHARGE_TICKS || !inGlade(position().add(f.scale(3)),0)){lay(level,AshenStagRules.EMBER_TICKS);skid(level);}
	}
	/** The end of a run: hooves dug in and sliding on, then wheeling round, its flank turned the while. */
	private void skid(ServerLevel level){
		slide=ahead(aim).scale(AshenStagRules.CHARGE_REACH/AshenStagRules.CHARGE_TICKS*.8);
		pose(AshenStagRules.SKID,AshenStagRules.SKID_TICKS);
		sound(level,position(),SoundEvents.GRAVEL_BREAK,1.6F,.6F);Feels.sound(level,head(),"aura_ashen_stag_snort",1.3F,.8F);
	}
	private void skidding(ServerLevel level,LivingEntity target){
		long el=since();
		if(el<AshenStagRules.SKID_SLIDE){
			double k=1-el/(double)AshenStagRules.SKID_SLIDE;
			move(MoverType.SELF,new Vec3(slide.x*k,-.3,slide.z*k));setDeltaMovement(Vec3.ZERO);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,position())),getX(),getY()+.1,getZ(),6,.6,.05,.6,.1);
			level.sendParticles(ParticleTypes.SMALL_FLAME,getX(),getY()+.1,getZ(),2,.5,.05,.5,.01);
			if(el%2==0)sound(level,position(),SoundEvents.GRAVEL_HIT,1.2F,.6F);
		}else still();
		if(target!=null)turn(yawTo(target.position()),9);
		if(--left<=0)after(level,AshenStagRules.CHARGE,4);
	}
	/**
	 * The Cinder Rush. Each run: a tell spent wheeling round toward its prey and pawing (a long one before the first, a short one
	 * after), then its whole weight run along the way it ended up facing, embers left smouldering behind it. The last run ends in a
	 * skid; a run into a trunk sticks it fast and ends the rush.
	 */
	private void rush(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell){
			still();
			if(el<=tell-3 && target!=null)aim=yawTo(target.position());
			turn(aim,24);var h=head();
			if(el%4==0)level.sendParticles(ParticleTypes.WHITE_ASH,getX(),getY()+.1,getZ(),8,.4,0,.4,.05);
			level.sendParticles(ParticleTypes.FLAME,h.x,h.y+.4,h.z,2,.3,.2,.3,.02);
			if(el==tell){aim=getYRot();runFrom=position();}
			return;
		}
		face(aim);
		var f=ahead(aim);var step=f.scale(AshenStagRules.RUSH_REACH/AshenStagRules.RUSH_TICKS);
		move(MoverType.SELF,new Vec3(step.x,-.3,step.z));setDeltaMovement(Vec3.ZERO);
		if(el%2==0)Feels.sound(level,position(),"aura_ashen_stag_step",1.4F,1.25F);
		level.sendParticles(ParticleTypes.FLAME,getX(),getY()+.2,getZ(),4,.5,.1,.5,.02);
		for(var p:prey(level))if(!struck.contains(p.getUUID()) && reaches(AshenStagRules.RUSH,p)){struck.add(p.getUUID());hit(level,p,AshenStagRules.RUSH_DAMAGE,1.4,position().subtract(f),.4);}
		if(horizontalCollision){lay(level,AshenStagRules.RUSH_TRAIL);stick(level);return;}
		if(el<tell+AshenStagRules.RUSH_TICKS && inGlade(position().add(f.scale(3)),0))return;
		lay(level,AshenStagRules.RUSH_TRAIL);
		if(++runs>=AshenStagRules.RUSH_RUNS){skid(level);return;}
		struck.clear();pose(AshenStagRules.RUSH,0,AshenStagRules.RUSH_TURN);
		Feels.sound(level,position(),"aura_ashen_stag_paw",1.3F,1.15F);
	}
	/** The embers this run left along its track, from where it set off to where it is now, smouldering {@code ticks}. */
	private void lay(ServerLevel level,int ticks){
		var at=position();
		if(at.subtract(runFrom).horizontalDistanceSqr()<1)return;
		strips.add(new Strip(runFrom.x,runFrom.z,at.x,at.z,at.y,level.getGameTime()+ticks));
		var mid=runFrom.add(at).scale(.5);
		Feels.sound(level,mid,"aura_ashen_stag_smoulder",1.2F,.9F+random.nextFloat()*.2F);
	}
	/** The ember strips and cinder patches smouldering, and burning whoever stands in them; gone when they go out. */
	private void smoulder(ServerLevel level){
		if(strips.isEmpty())return;
		long now=level.getGameTime();
		strips.removeIf(st->now>=st.until());
		boolean burn=now%AshenStagRules.EMBER_EVERY==0;
		var hurt=new HashSet<UUID>();
		for(var st:strips){
			double dx=st.bx()-st.ax(),dz=st.bz()-st.az();boolean patch=dx*dx+dz*dz<.25;int n=Math.max(2,(int)Math.sqrt(dx*dx+dz*dz));
			// Dying down toward the end: smoke instead of flame.
			boolean low=st.until()-now<25;
			if(now%3==0){
				if(patch)level.sendParticles(low?ParticleTypes.SMOKE:ParticleTypes.FLAME,st.ax(),st.y()+.1,st.az(),3,.5,.02,.5,.005);
				else for(int i=0;i<n;i+=2){
					double t=(i+random.nextDouble())/n,x=st.ax()+dx*t,z=st.az()+dz*t;
					level.sendParticles(low?ParticleTypes.SMOKE:ParticleTypes.FLAME,x,st.y()+.1,z,1,.35,.02,.35,.005);
					if(!low && random.nextInt(4)==0)level.sendParticles(ParticleTypes.LAVA,x,st.y()+.1,z,1,.3,0,.3,0);
				}
			}
			if(!burn)continue;
			for(var p:prey(level))if(Math.abs(p.getY()-st.y())<1.5 && AshenStagRules.onStrip(p.getX(),p.getZ(),st.ax(),st.az(),st.bx(),st.bz()) && hurt.add(p.getUUID())){
				GuardianBite.land(level,this,p,(float)AshenStagRules.EMBER_DAMAGE,AshenStagRules.PIERCE);
				level.sendParticles(ParticleTypes.FLAME,p.getX(),p.getY()+.3,p.getZ(),6,.25,.2,.25,.02);
			}
		}
	}
	/** Run into something: its antlers driven in, and stuck there. */
	private void stick(ServerLevel level){
		links=0;pose(AshenStagRules.STUCK,AshenStagRules.STUCK_TICKS);still();
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

	/**
	 * The rake. Brow down in the ash, antlers dragged side to side, sparks and grit thrown up; then cinders flung one after another,
	 * each arcing down burning on where its prey is going, a closing ring of embers marking the spot. Kindled, a second cinder comes
	 * down with each, on where they stand.
	 */
	private void rake(ServerLevel level,LivingEntity target){
		still();long el=since();int tell=tell();
		if(target!=null)turn(yawTo(target.position()),6);
		aim=getYRot();var f=ahead(aim);var tips=position().add(f.scale(2.4)).add(0,.3,0);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,tips)),tips.x,tips.y,tips.z,3,.6,.1,.6,.1);
		if(el%2==0)level.sendParticles(ParticleTypes.ELECTRIC_SPARK,tips.x,tips.y,tips.z,2,.5,.1,.5,.06);
		if(el%4==0)sound(level,tips,SoundEvents.GRAVEL_HIT,1.1F,.6F+random.nextFloat()*.2F);
		if(el>=tell && el<tell+AshenStagRules.RAKE_TICKS && (el-tell)%AshenStagRules.RAKE_EVERY==0 && target!=null){
			fling(level,target,tips,true);if(kindled())fling(level,target,tips,false);
			sound(level,tips,SoundEvents.BLAZE_SHOOT,1.2F,.7F+random.nextFloat()*.2F);
		}
		if(el>=tell+AshenStagRules.RAKE_TICKS)after(level,AshenStagRules.RAKE,AshenStagRules.RAKE_RECOVER);
	}
	/** A cinder flung from the antler tips at {@code target}: on where they are going if {@code lead}, on where they stand otherwise. */
	private void fling(ServerLevel level,LivingEntity target,Vec3 from,boolean lead){
		var aimAt=target.position().add(lead?preyVel.scale(12):Vec3.ZERO);
		double j=lead?1.5:.5;aimAt=aimAt.add((random.nextDouble()*2-1)*j,0,(random.nextDouble()*2-1)*j);
		var flat=aimAt.subtract(position()).multiply(1,0,1);
		if(flat.length()>AshenStagRules.CINDER_RANGE)aimAt=position().add(flat.normalize().scale(AshenStagRules.CINDER_RANGE)).add(0,aimAt.y-getY(),0);
		var to=floor(level,aimAt);double far=to.subtract(from).horizontalDistance();
		cinders.add(new Cinder(from,to,level.getGameTime(),3+.25*far));
	}
	/** The cinders in the air, and bursting where they come down. */
	private void cinders(ServerLevel level){
		if(cinders.isEmpty())return;
		long now=level.getGameTime();
		for(var it=cinders.iterator();it.hasNext();){
			var c=it.next();double t=(now-c.born())/(double)AshenStagRules.CINDER_FLIGHT;
			if(t<1){
				var at=c.from().lerp(c.to(),t).add(0,c.peak()*4*t*(1-t),0);
				level.sendParticles(ParticleTypes.FLAME,at.x,at.y,at.z,2,.1,.1,.1,.01);
				level.sendParticles(ParticleTypes.SMOKE,at.x,at.y,at.z,1,.1,.1,.1,.01);
				if((now-c.born())%2==0)ring(level,c.to(),AshenStagRules.CINDER_RADIUS*(1+(1-t)*.8),t>.6?EMBER:EMBER_SMALL,14);
				continue;
			}
			it.remove();var to=c.to();
			sound(level,to,SoundEvents.LAVA_POP,1.4F,.8F);sound(level,to,SoundEvents.FIRECHARGE_USE,1F,1.1F);
			level.sendParticles(ParticleTypes.LAVA,to.x,to.y+.2,to.z,5,.5,.1,.5,0);
			level.sendParticles(ParticleTypes.FLAME,to.x,to.y+.2,to.z,16,.6,.2,.6,.05);
			level.sendParticles(ParticleTypes.LARGE_SMOKE,to.x,to.y+.3,to.z,6,.5,.2,.5,.02);
			for(var p:prey(level))if(AshenStagRules.cinders(p.getX()-to.x,p.getY()-to.y,p.getZ()-to.z)){hit(level,p,AshenStagRules.CINDER_DAMAGE,.3,to,.2);p.igniteForSeconds(2);}
			strips.add(new Strip(to.x,to.z,to.x,to.z,to.y,now+AshenStagRules.PATCH_TICKS));
		}
	}

	/**
	 * Calls the herd: lanes across the glade, one through where its prey stands and the others every few strides either side, each
	 * marked with embers until two ember deer run down it one behind the other. Kindled, a second herd follows half a lane over, the
	 * other way.
	 */
	private void call(ServerLevel level,LivingEntity target){
		herd.clear();
		var o=position();var f=ahead(aim);var r=new Vec3(-f.z,0,f.x);long now=level.getGameTime();
		double preyF=target==null?0:target.position().subtract(o).dot(f);int dir=random.nextBoolean()?1:-1;
		int waves=kindled()?2:1;
		for(int w=0;w<waves;w++){
			double[] lanes=AshenStagRules.lanes(preyF+w*AshenStagRules.LANE_SPACING/2);int d=w==0?dir:-dir;
			for(int i=0;i<lanes.length;i++)for(int k=0;k<2;k++){
				long born=now+AshenStagRules.HERD_TELL+k*AshenStagRules.HERD_STAGGER+(i%2)*3+w*AshenStagRules.HERD_AGAIN;
				herd.add(new Deer(o,f,r,lanes[i],d,born,k==0,new HashSet<>()));
			}
		}
	}
	/** The herd: lanes marked before the deer come, then the deer running down them, trampling whoever stands in the lane. */
	private void herd(ServerLevel level){
		if(herd.isEmpty())return;
		long now=level.getGameTime();
		for(var it=herd.iterator();it.hasNext();){
			var e=it.next();long wait=e.born()-now;
			if(wait>0){
				if(e.lead() && wait%4==0 && wait<=AshenStagRules.HERD_TELL)for(double x=-AshenStagRules.HERD_WIDTH;x<=AshenStagRules.HERD_WIDTH;x+=2)for(int s=-1;s<=1;s+=2){
					var at=e.o().add(e.f().scale(e.centre()+s*AshenStagRules.LANE_HALF)).add(e.r().scale(x));
					level.sendParticles(EMBER,at.x,e.o().y+.15,at.z,1,0,0,0,0);
				}
				continue;
			}
			long t=-wait;
			if(AshenStagRules.through(t)){it.remove();continue;}
			double from=AshenStagRules.lateral(e.dir(),t-1),to=AshenStagRules.lateral(e.dir(),t);
			var pos=e.o().add(e.f().scale(e.centre())).add(e.r().scale(to));
			level.sendParticles(ParticleTypes.FLAME,pos.x,pos.y+1.0,pos.z,5,.4,.5,.4,.02);
			level.sendParticles(ParticleTypes.SMALL_FLAME,pos.x,pos.y+1.8,pos.z,2,.2,.2,.2,.01);
			level.sendParticles(ParticleTypes.WHITE_ASH,pos.x,pos.y+.2,pos.z,3,.4,.1,.4,.02);
			if(e.lead() && t%4==0)sound(level,pos,SoundEvents.HORSE_GALLOP,.9F,1.2F+random.nextFloat()*.2F);
			for(var p:prey(level)){
				var v=p.position().subtract(e.o());
				if(AshenStagRules.trampled(v.dot(e.r()),from,to,v.dot(e.f()),e.centre(),p.getY()-e.o().y) && e.struck().add(p.getUUID()))
					hit(level,p,AshenStagRules.DEER_DAMAGE,1.2,p.position().subtract(e.r().scale(e.dir())),.35);
			}
		}
	}
	/** Head thrown back bellowing the herd on, its throat bare, until they are well under way. */
	private void stampede(ServerLevel level){
		still();face(aim);long el=since();int tell=tell();
		var h=head();
		if(el%3==0)level.sendParticles(ParticleTypes.LARGE_SMOKE,h.x,h.y+.5,h.z,2,.3,.3,.3,.03);
		if(el==tell){bellow(level,1.8F,.9F);sound(level,position(),SoundEvents.HORSE_GALLOP,1.6F,.8F);}
		if(el>=tell+AshenStagRules.HERD_HOLD)after(level,AshenStagRules.STAMPEDE,AshenStagRules.HERD_RECOVER);
	}

	/** The smoke. Antlers shaken until smoke pours off them; then a pall thrown out where it stands, and it slips away into it. */
	private void smoke(ServerLevel level){
		still();face(aim);long el=since();int tell=tell();
		var h=head();
		level.sendParticles(ParticleTypes.LARGE_SMOKE,h.x,h.y+.6,h.z,3,.6,.4,.6,.02);
		if(el%4==0)sound(level,h,SoundEvents.WOLF_SHAKE,1.2F,.5F+el/(float)tell*.3F);
		if(el==tell){
			smokeAt=position();smokeUntil=level.getGameTime()+AshenStagRules.SMOKE_TICKS;
			level.sendParticles(ParticleTypes.LARGE_SMOKE,getX(),getY()+1.5,getZ(),60,3,1.2,3,.04);
			level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,getX(),getY()+1,getZ(),20,2.5,.6,2.5,.02);
			sound(level,position(),SoundEvents.FIRE_EXTINGUISH,1.6F,.6F);
			struck.clear();pose(AshenStagRules.STALK,AshenStagRules.STALK_TICKS);
		}
	}
	/** The pall hanging where it was thrown, blinding whoever stands in it. */
	private void pall(ServerLevel level){
		if(smokeAt==null)return;
		long now=level.getGameTime();
		if(now>=smokeUntil){smokeAt=null;return;}
		double r=AshenStagRules.SMOKE_RADIUS;
		if(now%2==0)for(int i=0;i<6;i++){
			double a=random.nextDouble()*Math.PI*2,d=Math.sqrt(random.nextDouble())*r;
			level.sendParticles(ParticleTypes.LARGE_SMOKE,smokeAt.x+Math.cos(a)*d,smokeAt.y+.4+random.nextDouble()*2.5,smokeAt.z+Math.sin(a)*d,1,.3,.3,.3,.005);
		}
		if(now%AshenStagRules.SMOKE_EVERY==0)for(var p:prey(level))if(AshenStagRules.smothered(p.getX()-smokeAt.x,p.getY()-smokeAt.y,p.getZ()-smokeAt.z))
			p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,AshenStagRules.SMOKE_BLIND,0,false,true),this);
	}
	/** Prowling low out of its smoke, its glow banked, soft snorts its only give-away, until it is near enough to lunge. */
	private void stalk(ServerLevel level,LivingEntity target){
		if(target==null){pose(AshenStagRules.WATCH,gap());return;}
		getNavigation().moveTo(target,.7);
		if(since()%12==0)Feels.sound(level,head(),"aura_ashen_stag_snort",.5F,1.3F);
		if(distanceTo(target)<=AshenStagRules.STALK_NEAR || --left<=0){
			getNavigation().stop();aim=yawTo(target.position());struck.clear();pose(AshenStagRules.LUNGE,0,AshenStagRules.LUNGE_TELL);
			var h=head();Feels.sound(level,h,"aura_ashen_stag_snort",1.5F,1.2F);
			level.sendParticles(ParticleTypes.FLAME,h.x,h.y+.5,h.z,12,.5,.3,.5,.04);
		}
	}
	/** The lunge: antlers flared on a snort, then a short hard spring along the locked line. Kindled, it springs again. */
	private void lunge(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();
		if(el<=tell){
			still();
			if(el<=tell-AshenStagRules.LUNGE_LOCK && target!=null){turn(yawTo(target.position()),20);aim=getYRot();}else face(aim);
			if(el%3==0){var h=head();level.sendParticles(ParticleTypes.SMALL_FLAME,h.x,h.y+.4,h.z,3,.6,.3,.6,.01);}
			return;
		}
		face(aim);var f=ahead(aim);
		if(el<=tell+AshenStagRules.LUNGE_TICKS){
			var step=f.scale(AshenStagRules.LUNGE_REACH/AshenStagRules.LUNGE_TICKS);
			move(MoverType.SELF,new Vec3(step.x,-.3,step.z));setDeltaMovement(Vec3.ZERO);
			if(el==tell+1){Feels.sound(level,head(),"aura_ashen_stag_gore",1.4F,1.1F);sound(level,position(),SoundEvents.PLAYER_ATTACK_SWEEP,1.2F,.8F);}
			level.sendParticles(ParticleTypes.FLAME,getX(),getY()+.4,getZ(),3,.4,.2,.4,.02);
			for(var p:prey(level))if(reaches(AshenStagRules.LUNGE,p) && struck.add(p.getUUID()))hit(level,p,AshenStagRules.LUNGE_DAMAGE,1.2,position().subtract(f),.3);
			return;
		}
		if(kindled() && again==0){
			again=1;struck.clear();pose(AshenStagRules.LUNGE,0,AshenStagRules.LUNGE_RETELL);Feels.sound(level,head(),"aura_ashen_stag_snort",1.4F,1.3F);
		}else after(level,AshenStagRules.SMOKE,AshenStagRules.LUNGE_RECOVER);
	}

	/**
	 * The lock. Antlers levelled and a forehoof scraping, turning after its prey until the aim locks; then a short lunge, and whoever
	 * it catches is held on its antlers and ground, until it throws them, or two blows to its throat wrench it free.
	 */
	private void lock(ServerLevel level,LivingEntity target){
		long el=since();int tell=tell();long now=level.getGameTime();
		if(locked!=null){holding(level,now);return;}
		if(el<=tell){
			still();
			if(el<=tell-AshenStagRules.LOCK_LOCK && target!=null){turn(yawTo(target.position()),15);aim=getYRot();}else face(aim);
			if(el%5==0)Feels.sound(level,position(),"aura_ashen_stag_paw",1.1F,1.1F);
			var f=ahead(aim);var hoof=position().add(f.scale(1.2));
			if(el%2==0)level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,ground(level,hoof)),hoof.x,hoof.y+.1,hoof.z,4,.2,.05,.2,.1);
			if(el%4==0){var h=head();level.sendParticles(ParticleTypes.LARGE_SMOKE,h.x,h.y-.2,h.z,2,.1,.05,.1,.03);}
			return;
		}
		face(aim);var f=ahead(aim);
		if(el<=tell+AshenStagRules.LOCK_TICKS){
			var step=f.scale(AshenStagRules.LOCK_REACH/AshenStagRules.LOCK_TICKS);
			move(MoverType.SELF,new Vec3(step.x,-.3,step.z));setDeltaMovement(Vec3.ZERO);
			for(var p:prey(level))if(reaches(AshenStagRules.LOCK,p)){
				locked=p.getUUID();lockedAt=now;lockBlows=0;still();
				hit(level,p,AshenStagRules.LOCK_DAMAGE,0,position(),0);
				sound(level,p.position(),SoundEvents.GOAT_RAM_IMPACT,1.6F,.7F);Feels.sound(level,head(),"aura_ashen_stag_gore",1.4F,.8F);
				return;
			}
			return;
		}
		// Caught nothing: it stumbles on, head down, a while.
		links=0;spent(AshenStagRules.LOCK_MISS);
	}
	/** Holding its prey on its antlers: dragged in before it, ground now and then, and thrown at the last. */
	private void holding(ServerLevel level,long now){
		still();face(aim);
		var f=ahead(aim);
		var e=level.getEntity(locked);
		if(!(e instanceof LivingEntity p) || !p.isAlive() || p.distanceTo(this)>AshenStagRules.LOCK_SLIP){locked=null;after(level,AshenStagRules.LOCK,AshenStagRules.LOCK_RECOVER);return;}
		var hold=position().add(f.scale(AshenStagRules.LOCK_AHEAD));var pull=hold.subtract(p.position()).multiply(1,0,1);
		if(pull.lengthSqr()>.01){p.setDeltaMovement(p.getDeltaMovement().scale(.5).add(pull.normalize().scale(Math.min(.12,pull.length()*.2))));p.needsSync=true;}
		long held=now-lockedAt;var h=head();
		if(held>0 && held%AshenStagRules.LOCK_GRIND==0 && held<AshenStagRules.LOCK_HOLD-10){
			hit(level,p,AshenStagRules.GRIND_DAMAGE,0,position(),0);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK,h.x,h.y-.5,h.z,8,.4,.3,.4,.1);
			sound(level,h,SoundEvents.GRINDSTONE_USE,1.3F,.5F);Feels.sound(level,h,"aura_ashen_stag_strain",1.1F,1.1F);
		}
		if(held>=AshenStagRules.LOCK_HOLD){
			sound(level,h,SoundEvents.PLAYER_ATTACK_SWEEP,1.5F,.6F);Feels.sound(level,h,"aura_ashen_stag_gore",1.6F,.8F);
			level.sendParticles(ParticleTypes.FLAME,p.getX(),p.getY()+1,p.getZ(),16,.5,.5,.5,.06);
			hit(level,p,AshenStagRules.THROW_DAMAGE,1.0,position(),1.0);
			locked=null;after(level,AshenStagRules.LOCK,AshenStagRules.LOCK_RECOVER);
		}
	}
	/** Its antlers wrenched free of a lock by force: it staggers back, head swinging. */
	private void wrench(ServerLevel level){
		locked=null;links=0;pose(AshenStagRules.STAGGER,AshenStagRules.STAGGER_TICKS);still();
		var h=head();
		Feels.sound(level,h,"aura_ashen_stag_crash",1.5F,1.2F);sound(level,h,SoundEvents.PLAYER_ATTACK_CRIT,1.4F,.7F);
		level.sendParticles(ParticleTypes.CRIT,h.x,h.y,h.z,20,.6,.6,.6,.3);
	}
	private void reel(ServerLevel level){
		still();
		if(since()%10==5){var h=head();Feels.sound(level,h,"aura_ashen_stag_snort",1.2F,.7F);level.sendParticles(ParticleTypes.WHITE_ASH,h.x,h.y,h.z,10,.5,.4,.5,.04);}
		if(--left<=0)pose(AshenStagRules.WATCH,gap());
	}

	/**
	 * Half beaten, once: reared and bellowing to the sky while fire climbs its antlers and embers rain about it, a ring marking how far
	 * the blast will reach; then down in a blast that throws back everyone near and sends a great ring of fire out across the glade.
	 */
	private void kindle(ServerLevel level){
		still();face(aim);long el=since();int tell=tell();long now=level.getGameTime();
		var h=head();
		if(el<tell){
			level.sendParticles(ParticleTypes.FLAME,h.x,h.y+(el/(double)tell)*1.5,h.z,4,.6,.4,.6,.02);
			if(el%3==0){level.sendParticles(ParticleTypes.LAVA,h.x,h.y+1,h.z,2,.5,.3,.5,0);sound(level,h,SoundEvents.LAVA_POP,1F,.8F);}
			for(int i=0;i<3;i++){double a=random.nextDouble()*Math.PI*2,d=random.nextDouble()*8;
				level.sendParticles(ParticleTypes.SMALL_FLAME,getX()+Math.cos(a)*d,getY()+4+random.nextDouble()*2,getZ()+Math.sin(a)*d,0,0,-.2,0,1);}
			if(el%4==0)ring(level,position(),AshenStagRules.KINDLE_RADIUS,EMBER,40);
			if(el%8==0)sound(level,position(),SoundEvents.FIRECHARGE_USE,1.2F,.6F);
			if(el==tell/2)bellow(level,2F,.6F);
		}
		if(el==tell){
			var at=position();
			sound(level,at,SoundEvents.MACE_SMASH_GROUND_HEAVY,2.4F,.6F);Feels.sound(level,at,"aura_ashen_stag_stomp",2F,.8F);sound(level,at,SoundEvents.BLAZE_SHOOT,2F,.6F);
			level.sendParticles(ParticleTypes.EXPLOSION,at.x,at.y+1,at.z,3,1,.4,1,0);
			level.sendParticles(ParticleTypes.FLAME,at.x,at.y+.5,at.z,60,3,.5,3,.1);
			var w=new Wave(at,AshenStagRules.KINDLE_WAVE_SPEED,AshenStagRules.KINDLE_WAVE_REACH,AshenStagRules.WAVE_DAMAGE,.5,now);
			for(var p:prey(level))if(AshenStagRules.blasts(p.getX()-at.x,p.getY()-at.y,p.getZ()-at.z)){
				hit(level,p,AshenStagRules.KINDLE_DAMAGE,1.5,at,.7);p.igniteForSeconds(3);w.struck.add(p.getUUID());
			}
			waves.add(w);
		}
		if(el>=tell+4){links=0;spent(AshenStagRules.KINDLE_RECOVER);}
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
		lastLeap=level.getGameTime();stung=0;links=0;
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
	/** Ticks before its next move, fewer once it has kindled. */
	private int gap(){return (kindled()?AshenStagRules.GAP_KINDLED:AshenStagRules.GAP)+random.nextInt(3);}

	/** Players it could be fighting right now. */
	private List<ServerPlayer> prey(ServerLevel level){
		var out=new ArrayList<ServerPlayer>();
		for(var p:level.players())if(p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(this)<24*24 && inGlade(p.position(),AshenStagRules.LEASH))out.add(p);
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
	/** Whether {@code move} has its front open just now: its throat while it holds someone, its belly reared up, its chest after a toss. */
	private boolean open(int move){
		long el=since();int tell=tell();
		return switch(move){
			case AshenStagRules.LOCK->locked!=null;
			case AshenStagRules.REAR->el<tell || (kindled() && el>tell+2 && el<tell+AshenStagRules.REAR_AGAIN);
			case AshenStagRules.TOSS->el>tell;
			default->false;
		};
	}

	@Override public boolean hurtServer(ServerLevel level,DamageSource source,float damage){
		if(source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.CRAMMING))return false;
		int move=move();
		if(AshenStagRules.scripted(move) && !source.is(DamageTypes.GENERIC_KILL)){
			// In its bed the ash and bark take the blow; but a blow, or a shot, gets it up.
			if(move==AshenStagRules.DORMANT && Guardians.enabled() && source.getEntity() instanceof ServerPlayer p && !p.isCreative())wake(level,0);
			return false;
		}
		long now=level.getGameTime();boolean frontal=false;
		if(!source.is(DamageTypes.GENERIC_KILL)){
			var at=source.getSourcePosition();
			frontal=at!=null && AshenStagRules.frontal(yawTo(at)-getYRot());
			// It sees a blow coming and bounds out from under it.
			if(source.getDirectEntity()!=null && source.getDirectEntity()==source.getEntity() && AshenStagRules.dodges(move,now-lastDodge,random.nextDouble())){
				lastDodge=now;boundAside(level);
				return false;
			}
			damage*=(float)AshenStagRules.armour(move,frontal,open(move));
		}
		boolean hurt=super.hurtServer(level,source,damage);
		if(hurt){
			lastCut=now;stung+=damage;
			// Two blows to its throat while it holds someone wrench its antlers free.
			if(move==AshenStagRules.LOCK && locked!=null && frontal && source.getEntity() instanceof ServerPlayer && AshenStagRules.breaks(++lockBlows))wrench(level);
			else if(AshenStagRules.leaps(move(),stung,now-lastLeap))leapAway(level,source.getSourcePosition());
		}
		return hurt;
	}
	@Override public void die(DamageSource source){
		if(level() instanceof ServerLevel server){Feels.sound(server,head(),"aura_ashen_stag_death",1.8F,1);Guardians.reward(server,this,AshenStagRules.TIER,bar,source);bar.removeAllPlayers();}
		super.die(source);
	}
	@Override protected SoundEvent getHurtSound(DamageSource source){return null;}
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
		out.putBoolean("stag_kindled",kindled());
	}
	@Override protected void readAdditionalSaveData(ValueInput in){
		super.readAdditionalSaveData(in);
		bound=in.getBooleanOr("stag_bound",false);
		bed=in.read("stag_bed",BlockPos.CODEC).orElse(blockPosition());rest=in.getFloatOr("stag_rest",getYRot());
		gladeMin=in.read("stag_glade_min",BlockPos.CODEC).orElse(bed);gladeMax=in.read("stag_glade_max",BlockPos.CODEC).orElse(bed);
		lastTick=in.getLongOr("stag_ticked",-1L);
		// Mid-fight it comes back on its feet, whatever it was doing when the chunk unloaded, and still kindled if it had.
		if(in.getBooleanOr("stag_awake",false)){
			pose(AshenStagRules.WATCH,AshenStagRules.GAP);lastCut=0;
			boolean k=in.getBooleanOr("stag_kindled",false);entityData.set(KINDLED,k);if(k)bar.setColor(BossEvent.BossBarColor.YELLOW);
		}else pose(AshenStagRules.DORMANT,0);
	}
}
