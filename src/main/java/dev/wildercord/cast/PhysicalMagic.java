package dev.wildercord.cast;

import dev.wildercord.content.PhysicalBlocks;
import dev.wildercord.spell.RuneDef;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Physical, reversible magic. Source water is reserved while it travels, so infinite-water
 * refill cannot duplicate borrowed blocks. Saves restore constructs after a crash or unload. */
public final class PhysicalMagic {
	private PhysicalMagic(){}
	private record Cell(Cast cast,BlockState placed,BlockState replaced,long due){}
	private static final Map<GlobalPos,Cell> CELLS=new HashMap<>();
	private static final int WORLD_CAP=512, OWNER_CAP=64;
	public static void init(){
		PhysicalBlocks.init();
		ServerTickEvents.END_SERVER_TICK.register(server->{
			for(var entry:List.copyOf(CELLS.entrySet())){
				Cell c=entry.getValue();var key=entry.getKey();ServerLevel level=server.getLevel(key.dimension());
				if(level==null){CELLS.remove(key);continue;}
				if(!c.cast.alive()||level.getGameTime()>=c.due){restore(level,key.pos(),c);continue;}
				if(!level.isLoaded(key.pos()))continue;
				if(!level.getBlockState(key.pos()).equals(c.placed)){CELLS.remove(key);continue;}
				if(level.getGameTime()%20==0 && (c.placed.is(PhysicalBlocks.CINDER)||c.placed.is(PhysicalBlocks.ROOT)||c.placed.is(PhysicalBlocks.THUNDER))){
					for(LivingEntity t:level.getEntitiesOfClass(LivingEntity.class,new AABB(key.pos()).inflate(.8))){
						if(c.placed.is(PhysicalBlocks.ROOT)&&Targets.canHelp(c.cast.caster,t)){
							if(!t.hasEffect(MobEffects.REGENERATION))t.addEffect(new MobEffectInstance(MobEffects.REGENERATION,100,0,false,false));
						}
						else if(Targets.canHarm(c.cast.caster,t)&&c.cast.once("terrain-contact:"+t.getUUID()))Effects.hurt(c.cast,t,level.damageSources().indirectMagic(c.cast.caster,c.cast.caster),2*c.cast.power);
					}
				}
				if(PhysicalBlocks.isStep(c.placed)&&c.due-level.getGameTime()<=20){
					for(LivingEntity t:level.getEntitiesOfClass(LivingEntity.class,new AABB(key.pos()).inflate(.1,1,.1)))
						if(Targets.canHelp(c.cast.caster,t))t.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,60,0,false,false));
				}
			}
		});
		PlayerBlockBreakEvents.BEFORE.register((world,player,pos,state,entity)->{
			if(!(world instanceof ServerLevel level)||!PhysicalBlocks.isConstruct(state))return true;
			Cell cell=CELLS.get(GlobalPos.of(level.dimension(),pos));
			if(cell!=null && !state.is(PhysicalBlocks.RESERVATION)&&!state.is(PhysicalBlocks.WATER))restore(level,pos,cell);
			return false;
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server->{for(var e:List.copyOf(CELLS.entrySet())){ServerLevel l=server.getLevel(e.getKey().dimension());if(l!=null)restore(l,e.getKey().pos(),e.getValue());}CELLS.clear();});
	}
	private static boolean place(Cast cast,BlockPos pos,Block block,int ticks,boolean source){
		ServerLevel level=cast.level;
		if(!cast.alive()||!level.isLoaded(pos)||!level.getWorldBorder().isWithinBounds(pos)||level.isOutsideBuildHeight(pos)||level.getBlockEntity(pos)!=null)return false;
		if(CELLS.size()>=WORLD_CAP || CELLS.values().stream().filter(c->c.cast.caster.getUUID().equals(cast.caster.getUUID())).count()>=OWNER_CAP)return false;
		BlockState old=level.getBlockState(pos);
		if(source ? !old.is(Blocks.WATER)||!old.getFluidState().isSource() : !old.isAir())return false;
		if(!source && block!=PhysicalBlocks.WATER && !level.getEntitiesOfClass(LivingEntity.class,new AABB(pos)).isEmpty())return false;
		if(TemporaryBlocks.recorded(level,pos)||!Casters.mayEdit(cast.caster,level,pos)||!cast.takeBlock())return false;
		BlockState placed=block.defaultBlockState();long due=level.getGameTime()+ticks;
		if(!level.setBlockAndUpdate(pos,placed))return false;
		TemporaryBlocks.put(level,pos,placed,old,due);
		CELLS.put(GlobalPos.of(level.dimension(),pos.immutable()),new Cell(cast,placed,old,due));
		return true;
	}
	private static void restore(ServerLevel level,BlockPos pos,Cell cell){
		if(CELLS.get(GlobalPos.of(level.dimension(),pos))!=cell)return;
		CELLS.remove(GlobalPos.of(level.dimension(),pos));
		if(!level.isLoaded(pos))return; // Saved fallback restores when loaded.
		if(level.getBlockState(pos).equals(cell.placed))level.setBlockAndUpdate(pos,cell.replaced);
		TemporaryBlocks.remove(level,pos);
	}
	static int active(){return CELLS.size();}
	/** Discrete terrain reactions do not renew expiry or produce additional material. */
	static boolean interact(Cast incoming,RuneDef rune,Cast.Hit hit){
		if(hit.block()==null||!incoming.alive()||!(incoming.caster instanceof ServerPlayer p)
			||!Casters.mayBuild(p)||!incoming.level.mayInteract(p,hit.block()))return false;
		var key=GlobalPos.of(incoming.level.dimension(),hit.block());Cell cell=CELLS.get(key);
		if(cell==null||!incoming.level.getBlockState(hit.block()).equals(cell.placed))return false;
		boolean allied=Targets.canHelp(incoming.caster,cell.cast.caster);
		if(!allied && !Targets.canHarm(incoming.caster,cell.cast.caster))return false;
		String element=rune.element();Block replacement=null;
		if(cell.placed.is(PhysicalBlocks.WATER) && element.equals("fire")){
			Vfx.emit(incoming.level,SpellMaterials.of("vapour",0xE4EFF5,.4F),hit.point(),12,.5,.03);
			restore(incoming.level,hit.block(),cell);return true;
		}
		if(!allied && (element.equals("wind")&&PhysicalBlocks.isStep(cell.placed)
			||element.equals("fire")&&cell.placed.is(PhysicalBlocks.RIME))){
			restore(incoming.level,hit.block(),cell);return true;
		}
		if(!allied)return false;
		if(cell.placed.is(PhysicalBlocks.WIND))replacement=switch(element){case "frost"->PhysicalBlocks.RIME;case "storm"->PhysicalBlocks.THUNDER;case "earth"->PhysicalBlocks.STRATA;default->null;};
		if(cell.placed.is(PhysicalBlocks.STRATA))replacement=switch(element){case "fire"->PhysicalBlocks.CINDER;case "life"->PhysicalBlocks.ROOT;default->null;};
		if(cell.placed.is(PhysicalBlocks.WATER)&&element.equals("frost"))replacement=PhysicalBlocks.RIME;
		if(replacement==null)return false;
		if(!incoming.level.getEntitiesOfClass(LivingEntity.class,new AABB(hit.block())).isEmpty())return false;
		if(!incoming.takeBlock())return true;
		BlockState state=replacement.defaultBlockState();
		if(!incoming.level.setBlockAndUpdate(hit.block(),state))return true;
		Cell changed=new Cell(cell.cast,state,cell.replaced,cell.due);
		CELLS.put(key,changed);TemporaryBlocks.put(incoming.level,hit.block(),state,cell.replaced,cell.due);
		Vfx.emit(incoming.level,SpellMaterials.of(element,Vfx.theme(element).primary(),.18F),hit.point(),8,.35,.03);
		return true;
	}
	public static boolean apply(Cast cast,RuneDef rune,Cast.Hit hit,double power,double duration){
		String name=rune.path();
		if(!Set.of("strata_rise","cinder_bulwark","root_bulwark","tidal_lift","boiling_surge","thunder_tide","wind_steps","rime_causeway","thunder_walk").contains(name))return false;
		if(!cast.once("physical:"+name))return true;
		if(name.contains("bulwark")||name.equals("strata_rise"))wall(cast,hit,name,(int)Math.min(400,160*duration));
		else if(name.contains("surge")||name.contains("tide")||name.equals("tidal_lift"))water(cast,hit,name,power);
		else steps(cast,name,(int)Math.min(400,160*duration));
		return true;
	}
	private static Vec3 forward(Cast cast){Vec3 d=new Vec3(cast.caster.getLookAngle().x,0,cast.caster.getLookAngle().z);return d.lengthSqr()<1e-5?new Vec3(0,0,1):d.normalize();}
	private static void wall(Cast cast,Cast.Hit hit,String name,int ticks){
		Vec3 base=hit.block()!=null&&hit.face()!=null?Vec3.atBottomCenterOf(hit.block().relative(hit.face())):CastEngine.ground(cast.level,hit.point().add(0,.5,0));
		Vec3 f=forward(cast),side=new Vec3(-f.z,0,f.x);
		Block block=name.equals("cinder_bulwark")?PhysicalBlocks.CINDER:name.equals("root_bulwark")?PhysicalBlocks.ROOT:PhysicalBlocks.STRATA;
		for(int row=0;row<3;row++){int r=row;Scheduler.later(1+row*3,()->{
			if(!cast.alive())return;
			for(int i=-2;i<=2;i++){BlockPos p=BlockPos.containing(base.add(side.scale(i))).above(r);
				if(place(cast,p,block,ticks,false)){
					Vfx.emit(cast.level,SpellMaterials.of("earth",0xBAA17A,.22F),Vec3.atCenterOf(p),4,.35,.05);
					if(block==PhysicalBlocks.CINDER)Light.ray(cast.level,Vec3.atCenterOf(p).add(0,-.5,0),Vec3.atCenterOf(p).add(0,.5,0),0xFF8546,.05,14);
					if(block==PhysicalBlocks.ROOT)Vfx.emit(cast.level,SpellMaterials.of("life",0x81CF65,.18F),Vec3.atCenterOf(p),3,.4,.03);
				}
			}
		});}
	}
	private static void steps(Cast cast,String name,int ticks){
		Vec3 f=forward(cast);Vec3 base=cast.caster.position();Block block=name.equals("rime_causeway")?PhysicalBlocks.RIME:name.equals("thunder_walk")?PhysicalBlocks.THUNDER:PhysicalBlocks.WIND;
		for(int i=0;i<5;i++){int n=i;Scheduler.later(1+i*3,()->{
			if(!cast.alive())return;
			BlockPos p=BlockPos.containing(base.add(f.scale(1.75+n)).add(0,.01,0)).offset(0,n/2,0);
			int width=block==PhysicalBlocks.RIME?1:0;Vec3 side=new Vec3(-f.z,0,f.x);
			for(int j=-width;j<=width;j++){BlockPos q=BlockPos.containing(Vec3.atLowerCornerOf(p).add(side.scale(j)));
				if(place(cast,q,block,ticks,false)){
					Light.groundRing(cast.level,Vec3.atCenterOf(q).add(0,-.3,0),block==PhysicalBlocks.THUNDER?0xFFE270:0xB3EEE3,.05,.65,.035,16);
					Vfx.emit(cast.level,SpellMaterials.of(block==PhysicalBlocks.RIME?"frost":"wind",0xBCEFF4,.2F),Vec3.atCenterOf(q),5,.3,.01);
				}
			}
		});}
	}
	private static void water(Cast cast,Cast.Hit hit,String name,double power){
		Vec3 aim=hit.point();BlockPos centre=BlockPos.containing(cast.caster.position());
		List<BlockPos> sources=new ArrayList<>();
		for(BlockPos p:BlockPos.betweenClosed(centre.offset(-4,-2,-4),centre.offset(4,2,4)))
			if(cast.level.isLoaded(p)&&cast.level.getBlockState(p).is(Blocks.WATER)&&cast.level.getFluidState(p).isSource())sources.add(p.immutable());
		sources.sort(Comparator.comparingDouble(p->p.distToCenterSqr(cast.caster.position())));
		Set<UUID> struck=new HashSet<>();int count=0;
		for(BlockPos source:sources){
			if(count>=3)break;
			if(!place(cast,source,PhysicalBlocks.RESERVATION,100,true))continue;
			int offset=count++*2;Vec3 start=Vec3.atCenterOf(source);Vec3 delta=aim.subtract(start);
			if(delta.length()>10)delta=delta.normalize().scale(10);
			Vec3 end=start.add(delta);BlockPos[] previous={null};Cell[] previousCell={null};boolean[] stopped={false};
			Cell reservedCell=CELLS.get(GlobalPos.of(cast.level.dimension(),source));
			for(int i=0;i<=7;i++){int phase=i;Scheduler.later(8+offset+i*3,Effects.carryContext(()->{
				if(previous[0]!=null){restore(cast.level,previous[0],previousCell[0]);previous[0]=null;}
				if(stopped[0]||!cast.alive())return;
				double t=phase/7.0;Vec3 at=start.lerp(end,t).add(0,Math.sin(t*Math.PI)*2+.8,0);BlockPos p=BlockPos.containing(at);
				if(!place(cast,p,PhysicalBlocks.WATER,6,false)){stopped[0]=true;return;}
				previous[0]=p;
				previousCell[0]=CELLS.get(GlobalPos.of(cast.level.dimension(),p));
				Vfx.emit(cast.level,SpellMaterials.of("water",0x67D5EB,.24F),at,6,.4,.03);
				if(name.equals("boiling_surge"))Vfx.emit(cast.level,SpellMaterials.of("vapour",0xFFD4A0,.35F),at.add(0,.6,0),4,.2,.03);
				if(name.equals("thunder_tide"))Light.ring(cast.level,at,new Vec3(0,1,0),0xFFED86,.2,.75,.03,7);
				for(LivingEntity target:cast.level.getEntitiesOfClass(LivingEntity.class,new AABB(p).inflate(.6))){
					if(!Targets.canHarm(cast.caster,target)||!struck.add(target.getUUID())||cast.takeEntities(1)==0)continue;
					Reactions.mark(target,Reactions.Mark.SOAKED,100);
					Effects.hurt(cast,target,cast.level.damageSources().indirectMagic(cast.caster,cast.caster),(name.equals("tidal_lift")?4:5)*power);
					if(name.equals("boiling_surge"))target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,40,0,false,false));
					if(name.equals("thunder_tide"))target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,20,0,false,false));
				}
			}));}
			Scheduler.later(36+offset,()->{if(previous[0]!=null)restore(cast.level,previous[0],previousCell[0]);restore(cast.level,source,reservedCell);});
		}
		if(count==0)Casters.tell(cast.caster,net.minecraft.network.chat.Component.literal("Tidal magic needs a water source within four blocks."));
	}
}
