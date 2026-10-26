package dev.wildercord.world.dungeons;

import dev.wildercord.config.Config;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import java.util.*;

/**
 * The Frost Wyrmling's lair: a den in the glacier under the snowy mountains. A crevasse opens in the snow and goes down,
 * its walls banded white and blue, its floor a drifted slope of snow; where the ice closes over it, it runs on as a tunnel.
 * At its foot, out of the wind, the last party to come this way made camp and never broke it. Beyond, the crevasse opens
 * into a domed hall of ice: four great icicle pillars, thick at the floor and the roof and thin between, a frozen pool with
 * bones under it, the dead of earlier parties frozen into the walls behind clear ice, and at the back, on a mound of
 * packed snow with its hoard round it, the Wyrmling, curled up asleep. The hall's floor is packed ice, bare and slick, with
 * drifts banked against the walls. The hall is warded: the den is entered only by the crevasse.
 *
 * <pre>
 *   z 0-24   the crevasse, sloping down from the snow
 *   z 24-33  its foot, level, with the camp in an alcove off it
 *   z 31-65  the hall, round (20, 48), its wall wandering in and out
 * </pre>
 */
public final class GlacierDenPiece extends DungeonPiece {
	public static final int WIDTH=41,HEIGHT=48,DEPTH=68;
	/** The hall floor (you stand one above it), the hall's centre, its radius and the height of its dome. */
	public static final int FLOOR=3,CX=20,CZ=48,RADIUS=17,DOME=15;
	/** The snow mound at the back of the hall where the Wyrmling sleeps, and the height of its crown above the floor. */
	public static final int NEST_X=CX,NEST_Z=CZ+8,NEST_RISE=2;
	/** Where the Wyrmling rests, its feet this high, on the crown of its mound. */
	public static final int REST_Y=FLOOR+NEST_RISE+1;
	/** The icicle pillars, floor to roof. */
	public static final int[][] PILLARS={{CX-9,CZ-4},{CX+9,CZ-4},{CX-8,CZ+7},{CX+8,CZ+7}};
	/** The frozen pool, its centre and radius. */
	private static final int POOL_X=CX+3,POOL_Z=CZ-8;
	private static final double POOL_R=3.3;
	/** Where the crevasse's slope ends and its level foot begins, and where it meets the hall. */
	private static final int RAMP_START=2,RAMP_END=24,MOUTH=CZ-RADIUS+2;
	/** How much glacier lies over the dome at its thinnest, at least. */
	private static final int COVER=6;
	/** Over this many blocks above its floor, the crevasse is roofed over and runs on as a tunnel this high. */
	private static final int OPEN=14,TUNNEL=5;
	private static final String TOP="den_top";
	private int top;

	private static final BlockState PACKED=Blocks.PACKED_ICE.defaultBlockState();
	private static final BlockState BLUE=Blocks.BLUE_ICE.defaultBlockState();
	private static final BlockState CLEAR=Blocks.ICE.defaultBlockState();
	private static final BlockState SNOW=Blocks.SNOW_BLOCK.defaultBlockState();

	public GlacierDenPiece(int x,int y,int z,Direction facing,int top){super(DungeonWorldgen.GLACIER_DEN_PIECE,x,y,z,WIDTH,HEIGHT,DEPTH,facing);this.top=top;}
	public GlacierDenPiece(CompoundTag tag){super(DungeonWorldgen.GLACIER_DEN_PIECE,tag);top=tag.getIntOr(TOP,FLOOR+12);}
	@Override protected void addAdditionalSaveData(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext context,CompoundTag tag){
		super.addAdditionalSaveData(context,tag);tag.putInt(TOP,top);
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c){
		if(!Config.get().auraWorld().guardians())return Optional.empty();
		var probe=new GlacierDenPiece(c.chunkPos().getMinBlockX(),0,c.chunkPos().getMinBlockZ(),Direction.Plane.HORIZONTAL.getRandomDirection(c.random()),0);
		var gen=c.chunkGenerator();int sea=gen.getSeaLevel();
		// Dry land all over: no lake over the hall, no frozen sea at the crevasse's head.
		int hallLow=Integer.MAX_VALUE,head=Integer.MAX_VALUE;
		int[][] hall={{CX,CZ},{CX-12,CZ},{CX+12,CZ},{CX,CZ-12},{CX,CZ+12}};
		int[][] crevasse={{CX,1},{CX-3,1},{CX+3,1},{CX,(RAMP_START+RAMP_END)/2}};
		for(int[] at:hall){int s=surface(c,gen,probe,at);if(s==Integer.MIN_VALUE || s<=sea+1)return Optional.empty();hallLow=Math.min(hallLow,s);}
		for(int i=0;i<crevasse.length;i++){
			int s=surface(c,gen,probe,crevasse[i]);if(s==Integer.MIN_VALUE || s<=sea+1)return Optional.empty();
			if(i<3)head=Math.min(head,s);
		}
		// The hall goes in under the glacier deep enough that its dome is covered everywhere; the crevasse comes down to it.
		int base=hallLow-(FLOOR+DOME+COVER);
		if(base<c.heightAccessor().getMinY()+4)return Optional.empty();
		int top=head-base;
		// Too shallow, and there is no crevasse; too deep, and its slope is a cliff.
		if(top-1<FLOOR+4 || top-1-FLOOR>RAMP_END-RAMP_START || top>HEIGHT-OPEN-2)return Optional.empty();
		var piece=new GlacierDenPiece(c.chunkPos().getMinBlockX(),base,c.chunkPos().getMinBlockZ(),probe.getOrientation(),top);
		var entrance=piece.localPosition(CX,top,1);
		return Optional.of(new Structure.GenerationStub(entrance,b->b.addPiece(piece)));
	}

	/** The surface over a spot while locating, or {@link Integer#MIN_VALUE} if it lies under water. */
	private static int surface(Structure.GenerationContext c,ChunkGenerator gen,GlacierDenPiece probe,int[] at){
		var p=probe.localPosition(at[0],0,at[1]);
		int surface=gen.getFirstOccupiedHeight(p.getX(),p.getZ(),Heightmap.Types.WORLD_SURFACE_WG,c.heightAccessor(),c.randomState());
		int floor=gen.getFirstOccupiedHeight(p.getX(),p.getZ(),Heightmap.Types.OCEAN_FLOOR_WG,c.heightAccessor(),c.randomState());
		return surface-floor>1?Integer.MIN_VALUE:surface;
	}

	/** The first block of open air over a column, in the piece's own frame. */
	private int surface(WorldGenLevel level,int x,int z){
		var p=getWorldPos(x,0,z);return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,p.getX(),p.getZ())-getWorldY(0);
	}

	/** Whether a column lies in the chunk being built: only those columns' heights may be read while it generates. */
	private boolean column(BoundingBox bb,int x,int z){
		var p=getWorldPos(x,0,z);return p.getX()>=bb.minX() && p.getX()<=bb.maxX() && p.getZ()>=bb.minZ() && p.getZ()<=bb.maxZ();
	}

	/** How far into the hall a column is, measured so that its wandering wall is always at {@link #RADIUS}. */
	public static double reach(int x,int z){
		double a=Math.atan2(z-CZ,x-CX);
		double wall=RADIUS-1.4+1.4*Math.sin(a*2+1.1)*Math.cos(a*3-0.2);
		return dist(x,z,CX,CZ)*RADIUS/wall;
	}

	/** The roof (the topmost open block) over a column at {@code reach} into the hall, or -1 outside it. */
	public static int dome(double reach){return reach>RADIUS?-1:FLOOR+3+(int)Math.round((DOME-3)*Math.sqrt(1-(reach/RADIUS)*(reach/RADIUS)));}

	/** The height of the crevasse's floor {@code z} blocks along it, in blocks above the piece's base, with a fraction. */
	double ramp(int z){
		if(z<=RAMP_START)return top-1;
		if(z>=RAMP_END)return FLOOR;
		return top-1+(FLOOR-(top-1))*(z-RAMP_START)/(double)(RAMP_END-RAMP_START);
	}

	/** How wide an icicle pillar is (its radius) a fraction {@code t} of the way from floor to roof: thick at the ends, thin between. */
	public static double pillarRadius(double t){double e=Math.abs(2*t-1);return 0.7+1.8*e*e*e;}

	/** How high the nest mound rises {@code d} blocks from its crown, or -1 past its foot. */
	public static int mound(double d){return d>4.6?-1:(int)Math.round(NEST_RISE*Math.min(1,(4.6-d)/2.6));}

	@Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox bb,ChunkPos chunk,BlockPos ref){
		DungeonWards.remember(level,this);
		shell(level,bb);
		hall(level,bb);
		for(int[] p:PILLARS)pillar(level,bb,p[0],p[1]);
		pool(level,bb);
		nest(level,bb);
		frozenDead(level,bb);
		crevasse(level,bb);
		camp(level,bb);
		dress(level,bb);
		// Stuck upright in the snow behind the crown of its mound, where it lies: the Rimescale, a scale it shed.
		set(level,bb,dev.wildercord.aura.world.Rimescale.BLOCK.defaultBlockState(),NEST_X+2,FLOOR+1+mound(dist(NEST_X+2,NEST_Z+3,NEST_X,NEST_Z)),NEST_Z+3);
		wyrmling(level,bb);
	}

	/** The guardian, curled asleep on its mound over the hoard, facing the way in. Spawned once, by whichever chunk holds it. */
	private void wyrmling(WorldGenLevel level,BoundingBox bb){
		var at=getWorldPos(NEST_X,REST_Y,NEST_Z);if(!bb.isInside(at))return;
		var mouth=getWorldPos(NEST_X,REST_Y,NEST_Z-10);
		float rest=(float)Math.toDegrees(Math.atan2(-(mouth.getX()-at.getX()),mouth.getZ()-at.getZ()));
		var wyrmling=dev.wildercord.aura.world.Guardians.FROST_WYRMLING.create(level.getLevel(),net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);if(wyrmling==null)return;
		wyrmling.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,rest,0);
		wyrmling.finalizeSpawn(level,level.getCurrentDifficultyAt(at),net.minecraft.world.entity.EntitySpawnReason.STRUCTURE,null);
		wyrmling.bind(at,rest,worldBox(CX-RADIUS,FLOOR,CZ-RADIUS,CX+RADIUS,FLOOR+DOME,CZ+RADIUS));
		level.addFreshEntityWithPassengers(wyrmling);
	}

	/** The glacier: packed ice banded with snow and old blue ice, rock showing through low down. */
	private BlockState ice(int x,int y,int z){
		int n=noise(x,y,z),band=Math.floorMod(y*3+noise(x/3,z/3)/25,7);
		if(y<FLOOR-1 && n<45)return n<30?Blocks.STONE.defaultBlockState():Blocks.ANDESITE.defaultBlockState();
		return band==0?SNOW:band==3 && n<70?BLUE:n<8?BLUE:n<12?Blocks.CALCITE.defaultBlockState():PACKED;
	}

	/** Solid glacier round the hall and the crevasse first, so no cave, aquifer or lava breaks into the den. */
	private void shell(WorldGenLevel level,BoundingBox bb){
		for(int x=0;x<WIDTH;x++)for(int z=0;z<DEPTH;z++){
			double r=reach(x,z);
			if(r<=RADIUS+2)for(int y=0;y<=dome(Math.min(r,RADIUS))+2;y++)set(level,bb,ice(x,y,z),x,y,z);
			if(Math.abs(x-CX)<=6 && z<=MOUTH+2 && column(bb,x,z)){
				int floor=(int)Math.floor(ramp(z));
				int lip=Math.min(surface(level,x,z)-2,floor+OPEN+2);
				for(int y=0;y<=Math.max(lip,floor);y++)set(level,bb,ice(x,y,z),x,y,z);
			}
		}
	}

	/** The hall: a bare floor of packed ice, drifts banked against the wall, icicles hanging from the dome, a little lichen. */
	private void hall(WorldGenLevel level,BoundingBox bb){
		for(int x=0;x<WIDTH;x++)for(int z=0;z<DEPTH;z++){
			double r=reach(x,z);if(r>RADIUS)continue;
			int roof=dome(r),n=noise(x,z);
			fill(level,bb,x,FLOOR+1,z,x,roof,z,AIR);
			// Drifts: deep against the wall, thinning inward. Snow won't lie on ice, so the drift's foot is snow.
			double drift=(r-(RADIUS-4))/4.0;
			boolean mouth=Math.abs(x-CX)<=2 && z<CZ-RADIUS+5;
			if(!mouth && drift>0 && n<drift*120){
				int layers=Math.max(1,Math.min(8,(int)Math.round(drift*9-n%3)));
				set(level,bb,SNOW,x,FLOOR,z);
				if(layers==8 && drift>0.8){set(level,bb,SNOW,x,FLOOR+1,z);layers=1+n%4;set(level,bb,snow(layers),x,FLOOR+2,z);}
				else set(level,bb,snow(layers),x,FLOOR+1,z);
			}
			else set(level,bb,n<78?PACKED:n<90?BLUE:CLEAR,x,FLOOR,z);
			// Icicles: blue ice hanging one to four blocks from the dome, longest toward the middle.
			int hang=noise(x,roof,z);
			if(r>3 && hang<14)fill(level,bb,x,roof-(r<10?hang%4:hang%2),z,x,roof,z,BLUE);
			// Glow lichen, sparse, in the dome's cracks: enough to see by, barely.
			else if(r>4 && noise(z,roof,x)<5)set(level,bb,Blocks.GLOW_LICHEN.defaultBlockState().setValue(BlockStateProperties.UP,true),x,roof,z);
		}
	}

	private static BlockState snow(int layers){return Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS,Math.max(1,Math.min(8,layers)));}

	/** An icicle pillar: blue ice, packed ice round its flare at the floor and the roof, thinnest halfway up. */
	private void pillar(WorldGenLevel level,BoundingBox bb,int x0,int z0){
		int roof=dome(reach(x0,z0));
		for(int y=FLOOR+1;y<=roof;y++){
			double t=(y-FLOOR-1)/(double)Math.max(1,roof-FLOOR-1),rad=pillarRadius(t);
			for(int x=x0-3;x<=x0+3;x++)for(int z=z0-3;z<=z0+3;z++){
				double d=dist(x,z,x0,z0);if(d>rad || reach(x,z)>RADIUS)continue;
				set(level,bb,d<rad-1 || noise(x,y,z)<55?BLUE:PACKED,x,y,z);
			}
		}
	}

	/** The frozen pool: clear ice flush with the floor over a shallow basin, bones lying on its bottom. */
	private void pool(WorldGenLevel level,BoundingBox bb){
		for(int x=POOL_X-4;x<=POOL_X+4;x++)for(int z=POOL_Z-4;z<=POOL_Z+4;z++){
			double d=dist(x,z,POOL_X,POOL_Z);if(d>POOL_R+noise(x,z)/120.0)continue;
			set(level,bb,Blocks.GRAVEL.defaultBlockState(),x,FLOOR-2,z);
			set(level,bb,CLEAR,x,FLOOR,z);
			set(level,bb,CLEAR,x,FLOOR-1,z);
		}
		set(level,bb,Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X),POOL_X-1,FLOOR-1,POOL_Z);
		set(level,bb,Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X),POOL_X,FLOOR-1,POOL_Z);
		set(level,bb,Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION,6),POOL_X+1,FLOOR-1,POOL_Z+1);
	}

	/**
	 * The nest: a mound of packed snow at the back of the hall, scraped flat on its crown where the Wyrmling lies, with
	 * its hoard round its foot: bones gnawed and frozen, raw gold and a chest it dragged in from somewhere.
	 */
	private void nest(WorldGenLevel level,BoundingBox bb){
		for(int x=NEST_X-5;x<=NEST_X+5;x++)for(int z=NEST_Z-5;z<=NEST_Z+5;z++){
			double d=dist(x,z,NEST_X,NEST_Z);int h=mound(d);if(h<0 || reach(x,z)>RADIUS)continue;
			fill(level,bb,x,FLOOR,z,x,FLOOR+h,z,SNOW);
			if(d>3.4 && noise(x,z)<60)set(level,bb,snow(2+noise(z,x)%4),x,FLOOR+h+1,z);
		}
		for(int[] b:new int[][]{{NEST_X-4,NEST_Z-2},{NEST_X+4,NEST_Z+1},{NEST_X-3,NEST_Z+4},{NEST_X+2,NEST_Z-4}}){
			set(level,bb,Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS,(b[0]+b[1])%2==0?Direction.Axis.X:Direction.Axis.Z),b[0],FLOOR+1,b[1]);
		}
		set(level,bb,Blocks.RAW_GOLD_BLOCK.defaultBlockState(),NEST_X+5,FLOOR+1,NEST_Z-1);
		set(level,bb,Blocks.RAW_GOLD_BLOCK.defaultBlockState(),NEST_X-5,FLOOR+1,NEST_Z+2);
		set(level,bb,Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION,9),NEST_X-4,FLOOR+2,NEST_Z-2);
		chest(level,bb,NEST_X+4,FLOOR+1,NEST_Z+4,DungeonWorldgen.GLACIER_DEN,Direction.NORTH,1502);
	}

	/** The dead of earlier parties, frozen into the hall's wall: bones and a skull, seen through a sheet of clear ice. */
	private void frozenDead(WorldGenLevel level,BoundingBox bb){
		for(double a:new double[]{-0.5,0.9,2.3,3.6}){
			double dx=Math.cos(a),dz=Math.sin(a);
			// Walk out from the centre to the last open column, then the ice sheet is there and the body behind it.
			int k=0;while(k<RADIUS+3 && reach((int)Math.round(CX+dx*(k+1)),(int)Math.round(CZ+dz*(k+1)))<=RADIUS)k++;
			int x=(int)Math.round(CX+dx*k),z=(int)Math.round(CZ+dz*k);
			int bx=(int)Math.round(CX+dx*(k+1)),bz=(int)Math.round(CZ+dz*(k+1));
			fill(level,bb,x,FLOOR+1,z,x,FLOOR+2,z,CLEAR);
			set(level,bb,Blocks.BONE_BLOCK.defaultBlockState(),bx,FLOOR+1,bz);
			set(level,bb,Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION,Math.floorMod((int)Math.round(a*16/(Math.PI*2))+12,16)),bx,FLOOR+2,bz);
		}
	}

	/**
	 * The crevasse: cut down through the glacier from the snow, a V that narrows to three blocks at its foot, its floor a
	 * slope of drifted snow; where the ice over it is too thick, it runs on as a tunnel. Its foot is level into the hall.
	 */
	private void crevasse(WorldGenLevel level,BoundingBox bb){
		for(int z=0;z<=MOUTH+1;z++)for(int x=CX-6;x<=CX+6;x++){
			if(!column(bb,x,z))continue;
			double fy=ramp(z);int floor=(int)Math.floor(fy),sky=surface(level,x,z);
			boolean open=sky-floor<=OPEN;
			int ceiling=open?Math.max(sky,floor+TUNNEL):floor+TUNNEL;
			int off=Math.abs(x-CX),n=noise(x,z);
			// Its sides lean out as they go up; a tunnel stays three wide.
			int lowest=-1;
			for(int y=floor+1;y<=ceiling;y++){
				double half=open?1.2+(y-floor-1)/2.6+(n%3)*0.25:1.2;
				if(off<=half){if(lowest<0)lowest=y;set(level,bb,AIR,x,y,z);}
			}
			if(lowest<0)continue;
			// The floor: snow, levelled out by layers so the slope can be walked.
			if(lowest==floor+1){
				set(level,bb,SNOW,x,floor,z);
				int layers=(int)Math.round((fy-floor)*8);
				if(layers>0 && z<RAMP_END)set(level,bb,snow(layers),x,floor+1,z);
			}
			// Old blue ice along the foot of its walls.
			else if(noise(x,lowest,z)<35)set(level,bb,BLUE,x,lowest-1,z);
		}
	}

	/**
	 * The last party's camp, in an alcove off the foot of the crevasse: a cold fire, bedrolls, a chest, a soul lantern long
	 * since gone dim, and one of them still sitting there.
	 */
	private void camp(WorldGenLevel level,BoundingBox bb){
		int x0=CX+2,x1=CX+5,z0=RAMP_END+1,z1=RAMP_END+5;
		fill(level,bb,x0,FLOOR,z0,x1,FLOOR,z1,SNOW);
		fill(level,bb,x0,FLOOR+1,z0,x1,FLOOR+3,z1,AIR);
		set(level,bb,Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT,false),x0+1,FLOOR+1,z0+2);
		set(level,bb,Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.BROWN).defaultBlockState(),x1,FLOOR+1,z0);
		set(level,bb,Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.BROWN).defaultBlockState(),x1,FLOOR+1,z0+1);
		set(level,bb,Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.GRAY).defaultBlockState(),x1,FLOOR+1,z1-1);
		chest(level,bb,x1,FLOOR+1,z1,DungeonWorldgen.GLACIER_DEN,Direction.WEST,1501);
		set(level,bb,Blocks.SOUL_LANTERN.defaultBlockState(),x0,FLOOR+1,z1);
		set(level,bb,Blocks.BONE_BLOCK.defaultBlockState(),x0+2,FLOOR+1,z0+3);
		set(level,bb,Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION,12),x0+2,FLOOR+2,z0+3);
		set(level,bb,Blocks.POWDER_SNOW.defaultBlockState(),x0,FLOOR+1,z0);
	}

	/**
	 * The den's dressing: cairns the last party raised at the crevasse's head, ribs of old blue ice along the tunnel and a
	 * portal of it where the tunnel opens into the hall, spikes of ice standing round the Wyrmling's mound, and the camp's
	 * firewood and a barrel of stores.
	 */
	private void dress(WorldGenLevel level,BoundingBox bb){
		gate(level,bb);
		// Cairns either side of the gate, one with a skull on it, one with a lantern.
		for(int x:new int[]{CX-9,CX+9}){
			if(!column(bb,x,1))continue;
			int y=surface(level,x,1);
			fill(level,bb,x,y,1,x,y+2,1,Blocks.COBBLESTONE.defaultBlockState());
			set(level,bb,Blocks.COBBLESTONE_WALL.defaultBlockState(),x,y+3,1);
			set(level,bb,x<CX?Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION,8):Blocks.LANTERN.defaultBlockState(),x,y+4,1);
		}
		// Ribs of blue ice along the level foot, clear of the camp's alcove; over the tunnel only where it is roofed.
		for(int z:new int[]{RAMP_END+1,RAMP_END+4,RAMP_END+7}){
			for(int y=FLOOR+1;y<=FLOOR+TUNNEL;y++){
				set(level,bb,BLUE,CX-2,y,z);
				if(y>FLOOR+3 || z>RAMP_END+5)set(level,bb,BLUE,CX+2,y,z);
			}
			if(column(bb,CX,z) && surface(level,CX,z)-FLOOR>OPEN)fill(level,bb,CX-2,FLOOR+TUNNEL+1,z,CX+2,FLOOR+TUNNEL+1,z,BLUE);
		}
		// The portal into the hall: posts and a lintel of blue ice, two icicles hanging inside it.
		int gate=MOUTH-1;
		fill(level,bb,CX-2,FLOOR+1,gate,CX-2,FLOOR+TUNNEL,gate,BLUE);
		fill(level,bb,CX+2,FLOOR+1,gate,CX+2,FLOOR+TUNNEL,gate,BLUE);
		fill(level,bb,CX-3,FLOOR+TUNNEL+1,gate,CX+3,FLOOR+TUNNEL+1,gate,BLUE);
		set(level,bb,Blocks.CALCITE.defaultBlockState(),CX,FLOOR+TUNNEL+1,gate);
		set(level,bb,BLUE,CX-1,FLOOR+TUNNEL,gate);
		set(level,bb,BLUE,CX+1,FLOOR+TUNNEL,gate);
		// Spikes of ice round the mound, open at the front where the Wyrmling looks out.
		for(int k=0;k<8;k++){
			if(k==5 || k==6)continue;
			double a=Math.PI/8+k*Math.PI/4;
			int x=(int)Math.round(NEST_X+Math.cos(a)*6.8),z=(int)Math.round(NEST_Z+Math.sin(a)*6.8);
			if(reach(x,z)>RADIUS-1)continue;
			int h=2+noise(x,z)%3;
			fill(level,bb,x,FLOOR+1,z,x,FLOOR+h,z,BLUE);
			set(level,bb,PACKED,x,FLOOR+h+1,z);
			set(level,bb,snow(3+noise(z,x)%3),x,FLOOR+1,z+(z<NEST_Z?-1:1));
		}
		// The camp's stores: firewood stacked by the chest and a barrel by the bedroll.
		set(level,bb,Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X),CX+3,FLOOR+1,RAMP_END+5);
		set(level,bb,Blocks.BARREL.defaultBlockState(),CX+4,FLOOR+1,RAMP_END+1);
	}

	/**
	 * The gate at the crevasse's head, seen from far across the snow: two spires of old blue ice either side of the way down,
	 * an arch of it between them hung with icicles and soul lanterns, frozen banners, and a field of ice spikes round about.
	 * Each column reads its own surface, so the spires stand on the ground whichever chunk builds them.
	 */
	private void gate(WorldGenLevel level,BoundingBox bb){
		int t=top,roof=HEIGHT-1;
		for(int side=-1;side<=1;side+=2){
			int sx=CX+side*5,sz=2,h=Math.min(roof,t+(side<0?13:11));
			for(int x=sx-1;x<=sx+1;x++)for(int z=sz-1;z<=sz+1;z++){
				if(!column(bb,x,z))continue;
				int ground=Math.min(surface(level,x,z),t);
				fill(level,bb,x,ground-3,z,x,t-1,z,PACKED);
				boolean corner=x!=sx && z!=sz;
				int up=corner?t+1:x==sx && z==sz?h:t+5-(noise(x,z)%2);
				for(int y=t;y<=Math.min(up,roof);y++)set(level,bb,y%4==0?PACKED:noise(x,y,z)<20?CLEAR:BLUE,x,y,z);
				if(corner)set(level,bb,snow(2+noise(z,x)%4),x,t+2,z);
			}
			if(column(bb,sx,sz))set(level,bb,Blocks.SOUL_LANTERN.defaultBlockState(),sx,Math.min(roof,h+1),sz);
			banner(level,bb,Blocks.WALL_BANNER.pick(net.minecraft.world.item.DyeColor.LIGHT_BLUE),sx,t+4,sz-2,Direction.SOUTH);
		}
		// The arch: rising from the spires to a crown over the way down, two blocks deep, icicles under it.
		for(int x=CX-4;x<=CX+4;x++){
			int off=Math.abs(x-CX),y=Math.min(roof-1,t+10-off*off/6);
			for(int z=1;z<=3;z++){
				set(level,bb,z==2?BLUE:PACKED,x,y,z);
				set(level,bb,z==2?PACKED:SNOW,x,y+1,z);
			}
			if(off==0)set(level,bb,Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState(),x,y+1,2);
			if(off==1 || off==3)set(level,bb,CLEAR,x,y-1,2);
			if(off==2)set(level,bb,Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true),x,y-1,2);
		}
		// A field of spikes round the head, kept off the crevasse and the gate.
		for(int x=CX-14;x<=CX+14;x++)for(int z=0;z<=12;z++){
			int off=Math.abs(x-CX),n=noise(x,z);
			if(off<8 || n>=9 || off==9 && z<=2 || !column(bb,x,z))continue;
			int ground=surface(level,x,z),h=2+noise(z,x)%(off>11?6:4);
			fill(level,bb,x,ground-1,z,x,Math.min(roof,ground+h),z,n<4?BLUE:PACKED);
			if(n<3)set(level,bb,PACKED,x,Math.min(roof,ground+h+1),z);
		}
	}

	/** The hall, from below its floor to over its dome. */
	@Override public List<BoundingBox> wardedBoxes(){return List.of(worldBox(CX-RADIUS-2,0,CZ-RADIUS-2,CX+RADIUS+2,FLOOR+DOME+2,CZ+RADIUS+2));}
}
