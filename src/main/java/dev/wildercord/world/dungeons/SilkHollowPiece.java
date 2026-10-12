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
 * The Hollow Matron's lair: an old mine, long abandoned, that dug into something it shouldn't have. A timbered shaft goes
 * down from a headframe in the woods, a spiral of plank treads round its wall and a flooded sump at its foot; a shored drift
 * runs from there, its rails broken and its cobwebs thickening, into a great hall the miners broke into and never came back
 * from. Four pillars of silk climb from the hall's floor to its ceiling, and between them, under the crown of the dome, hangs
 * the nest: a mass of silk with cocoons dangling from it, and under it, on her thread, the Matron. Egg sacs cluster along the
 * walls. The only light is a little glow lichen on the stone. The hall is warded: the hollow is entered only by the shaft.
 *
 * <pre>
 *   z 1-13   the shaft and headframe, centred at (22, 7)
 *   z 11-28  the drift, three wide, shored every four blocks
 *   z 26-64  the hall, round (22, 45), its wall wandering in and out a little
 * </pre>
 */
public final class SilkHollowPiece extends DungeonPiece {
	public static final int WIDTH=45,HEIGHT=46,DEPTH=68;
	/** The hall floor (you stand one above it), the hall's centre under the nest, its radius and the height of its dome. */
	public static final int FLOOR=3,CX=22,CZ=45,RADIUS=19,DOME=18;
	/** Where the Matron hangs, her feet this high, under the nest on her thread. */
	public static final int REST_Y=FLOOR+6;
	/** The silk pillars, floor to ceiling: what she climbs, and what can be cut out from under her. */
	public static final int[][] PILLARS={{CX-7,CZ-7},{CX+7,CZ-7},{CX-7,CZ+7},{CX+7,CZ+7}};
	/** The egg sacs along the wall, where her brood comes from. */
	public static final int[][] SACS={{CX-15,CZ-4},{CX+15,CZ-4},{CX-12,CZ+10},{CX+12,CZ+10},{CX-4,CZ+15},{CX+5,CZ+15}};
	private static final int SHAFT_X=22,SHAFT_Z=7;
	/** How far below the surface the piece's floor lies. */
	private static final int DEPTH_BELOW=32;
	/** Blocks the treads drop each full turn round the shaft. */
	private static final int RISE=12;
	private static final String TOP="hollow_top";
	private int top;

	private static final BlockState SILK=Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState();
	private static final BlockState WEB=Blocks.COBWEB.defaultBlockState();
	private static final BlockState PLANK=Blocks.SPRUCE_PLANKS.defaultBlockState();
	private static final BlockState POST=Blocks.SPRUCE_FENCE.defaultBlockState();
	private static final BlockState LOG=Blocks.SPRUCE_LOG.defaultBlockState();

	public SilkHollowPiece(int x,int y,int z,Direction facing,int top){super(DungeonWorldgen.SILK_HOLLOW_PIECE,x,y,z,WIDTH,HEIGHT,DEPTH,facing);this.top=top;}
	public SilkHollowPiece(CompoundTag tag){super(DungeonWorldgen.SILK_HOLLOW_PIECE,tag);top=tag.getIntOr(TOP,HEIGHT-8);}
	@Override protected void addAdditionalSaveData(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext context,CompoundTag tag){
		super.addAdditionalSaveData(context,tag);tag.putInt(TOP,top);
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c){
		if(!Config.get().auraWorld().guardians())return Optional.empty();
		var probe=new SilkHollowPiece(c.chunkPos().getMinBlockX(),0,c.chunkPos().getMinBlockZ(),Direction.Plane.HORIZONTAL.getRandomDirection(c.random()),0);
		var gen=c.chunkGenerator();int sea=gen.getSeaLevel();
		// The headframe needs dry, fairly level ground round the shaft: no lake, no cliff edge.
		int high=Integer.MIN_VALUE,low=Integer.MAX_VALUE;
		for(int[] at:new int[][]{{SHAFT_X,SHAFT_Z},{SHAFT_X-5,SHAFT_Z},{SHAFT_X+5,SHAFT_Z},{SHAFT_X,SHAFT_Z-5},{SHAFT_X,SHAFT_Z+5}}){
			var p=probe.localPosition(at[0],0,at[1]);
			int surface=gen.getFirstOccupiedHeight(p.getX(),p.getZ(),Heightmap.Types.WORLD_SURFACE_WG,c.heightAccessor(),c.randomState());
			int floor=gen.getFirstOccupiedHeight(p.getX(),p.getZ(),Heightmap.Types.OCEAN_FLOOR_WG,c.heightAccessor(),c.randomState());
			if(surface<=sea+1 || surface-floor>1)return Optional.empty();
			high=Math.max(high,surface);low=Math.min(low,surface);
		}
		if(high-low>4)return Optional.empty();
		int base=low-DEPTH_BELOW;
		if(base<c.heightAccessor().getMinY()+4)return Optional.empty();
		var piece=new SilkHollowPiece(c.chunkPos().getMinBlockX(),base,c.chunkPos().getMinBlockZ(),probe.getOrientation(),high-base);
		var entrance=piece.localPosition(SHAFT_X,piece.top,SHAFT_Z);
		return Optional.of(new Structure.GenerationStub(entrance,b->b.addPiece(piece)));
	}

	/** The first block of open air over a column, in the piece's own frame. */
	private int surface(WorldGenLevel level,int x,int z){
		var p=getWorldPos(x,0,z);return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,p.getX(),p.getZ())-getWorldY(0);
	}

	/** Whether a column lies in the chunk being built: only those columns' heights may be read while it generates. */
	private boolean column(BoundingBox bb,int x,int z){
		var p=getWorldPos(x,0,z);return p.getX()>=bb.minX() && p.getX()<=bb.maxX() && p.getZ()>=bb.minZ() && p.getZ()<=bb.maxZ();
	}

	/**
	 * How far into the hall a column is, measured so that the hall's wandering wall is always at {@link #RADIUS}: the wall
	 * bulges out and pulls in a couple of blocks as it goes round, so the hall reads as dug and broken into, not turned.
	 */
	public static double reach(int x,int z){
		double a=Math.atan2(z-CZ,x-CX);
		double wall=RADIUS-1.6+1.6*Math.sin(a*3+0.7)*Math.cos(a*2-0.4);
		return dist(x,z,CX,CZ)*RADIUS/wall;
	}

	/** The ceiling (the topmost open block) over a column at {@code reach} into the hall, or -1 outside it. */
	public static int dome(double reach){return reach>RADIUS?-1:FLOOR+3+(int)Math.round((DOME-3)*Math.sqrt(1-(reach/RADIUS)*(reach/RADIUS)));}

	/** How deep the nest hangs below the ceiling {@code r} blocks from the hall's centre: deepest at its heart, gone by six. */
	public static int nest(double r){return r>=6?0:1+(int)Math.round(5*(1-r/6));}

	@Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox bb,ChunkPos chunk,BlockPos ref){
		DungeonWards.remember(level,this);
		shell(level,bb);
		hall(level,bb);
		for(int[] p:PILLARS)pillar(level,bb,p[0],p[1]);
		nest(level,bb);
		for(int i=0;i<SACS.length;i++)sac(level,bb,SACS[i][0],SACS[i][1],i);
		shaft(level,bb);
		drift(level,bb);
		remains(level,bb);
		// Under the nest, a little behind where she comes down: the Weaver's Silk, a skein of her own silk wound round a bone.
		set(level,bb,dev.wildercord.aura.world.WeaversSilk.BLOCK.defaultBlockState(),CX,FLOOR+1,CZ+3);
		matron(level,bb);
	}

	/** The guardian, hanging head-down on her thread under the nest and facing the drift. Spawned once, by whichever chunk holds her. */
	private void matron(WorldGenLevel level,BoundingBox bb){
		var hang=getWorldPos(CX,REST_Y,CZ);if(!bb.isInside(hang))return;
		var drift=getWorldPos(CX,REST_Y,CZ-10);
		float rest=(float)Math.toDegrees(Math.atan2(-(drift.getX()-hang.getX()),drift.getZ()-hang.getZ()));
		var matron=dev.wildercord.aura.world.Guardians.HOLLOW_MATRON.create(level.getLevel(),net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);if(matron==null)return;
		matron.snapTo(hang.getX()+.5,hang.getY(),hang.getZ()+.5,rest,0);
		matron.finalizeSpawn(level,level.getCurrentDifficultyAt(hang),net.minecraft.world.entity.EntitySpawnReason.STRUCTURE,null);
		// Her thread hangs from the underside of the nest, at its deepest.
		float anchor=getWorldPos(CX,dome(0)-nest(0)+1,CZ).getY();
		matron.bind(hang,getWorldPos(CX,FLOOR+1,CZ).getY(),anchor,rest,worldBox(CX-RADIUS,FLOOR,CZ-RADIUS,CX+RADIUS,FLOOR+DOME,CZ+RADIUS));
		// The pillars she climbs to ambush from, and the sacs her brood hatch from.
		var pillars=new java.util.ArrayList<net.minecraft.core.BlockPos>();for(int[] p:PILLARS)pillars.add(getWorldPos(p[0],FLOOR+1,p[1]));
		var sacs=new java.util.ArrayList<net.minecraft.core.BlockPos>();for(int[] p:SACS)sacs.add(getWorldPos(p[0],FLOOR+1,p[1]));
		matron.nestle(pillars,sacs);
		level.addFreshEntityWithPassengers(matron);
	}

	private BlockState rock(int x,int y,int z){
		int n=noise(x,y,z);
		return n<16?Blocks.TUFF.defaultBlockState():n<26?Blocks.ANDESITE.defaultBlockState():n<31?Blocks.COBBLESTONE.defaultBlockState()
			:n<33?Blocks.COAL_ORE.defaultBlockState():n==33?Blocks.IRON_ORE.defaultBlockState():Blocks.STONE.defaultBlockState();
	}

	/** Solid rock round the hall, the shaft and the drift first, so no cave, aquifer or lava lake breaks into the hollow. */
	private void shell(WorldGenLevel level,BoundingBox bb){
		for(int x=0;x<WIDTH;x++)for(int z=0;z<DEPTH;z++){
			double r=reach(x,z);
			if(r<=RADIUS+2)for(int y=0;y<=dome(Math.min(r,RADIUS))+2;y++)set(level,bb,rock(x,y,z),x,y,z);
			double s=dist(x,z,SHAFT_X,SHAFT_Z);
			if(s<=6.2 && column(bb,x,z)){int lip=Math.min(top,surface(level,x,z))-1;for(int y=0;y<lip;y++)set(level,bb,rock(x,y,z),x,y,z);}
			if(x>=CX-4 && x<=CX+4 && z>=SHAFT_Z+3 && z<=CZ-RADIUS+4)for(int y=0;y<=FLOOR+7;y++)set(level,bb,rock(x,y,z),x,y,z);
		}
	}

	/** The hall: open, rubbly footing, cobweb draped into the corners where the dome comes down, a little lichen for light. */
	private void hall(WorldGenLevel level,BoundingBox bb){
		for(int x=0;x<WIDTH;x++)for(int z=0;z<DEPTH;z++){
			double r=reach(x,z);if(r>RADIUS)continue;
			int ceiling=dome(r),n=noise(x,z);
			fill(level,bb,x,FLOOR+1,z,x,ceiling,z,AIR);
			set(level,bb,(n<40?Blocks.STONE:n<58?Blocks.GRAVEL:n<72?Blocks.TUFF:n<86?Blocks.COBBLESTONE:Blocks.COARSE_DIRT).defaultBlockState(),x,FLOOR,z);
			// Where the dome comes down to the floor, web fills the angle; further in, it hangs from the ceiling in rags.
			boolean mouth=Math.abs(x-CX)<=2 && z<CZ-RADIUS+6;
			// (but not across the drift's mouth: there's no cutting through it, here).
			if(!mouth && r>RADIUS-1.5 && n<55)fill(level,bb,x,FLOOR+1,z,x,FLOOR+1+n%3,z,WEB);
			else if(!mouth && r>RADIUS-3 && n<20)set(level,bb,WEB,x,FLOOR+1,z);
			if(r>8 && noise(x,ceiling,z)<9)fill(level,bb,x,ceiling-1-n%2,z,x,ceiling,z,WEB);
			// Glow lichen on the ceiling, sparse: enough to see shapes, not enough to see her coming.
			else if(r>5 && noise(z,ceiling,x)<4)set(level,bb,Blocks.GLOW_LICHEN.defaultBlockState().setValue(BlockStateProperties.UP,true),x,ceiling,z);
			// Rubble from the breakthrough, kept to the edges.
			if(r>RADIUS-5 && r<=RADIUS-1.5 && n>=94)set(level,bb,Blocks.COBBLESTONE.defaultBlockState(),x,FLOOR+1,z);
		}
	}

	/**
	 * A silk pillar: a rope of white silk as thick as a block from floor to ceiling, webbing clinging round it here and there,
	 * splayed at its foot and fanning out where it meets the stone above.
	 */
	private void pillar(WorldGenLevel level,BoundingBox bb,int x0,int z0){
		int ceiling=dome(reach(x0,z0));
		fill(level,bb,x0,FLOOR+1,z0,x0,ceiling,z0,SILK);
		int[][] sides={{1,0},{-1,0},{0,1},{0,-1}};
		for(int y=FLOOR+1;y<=ceiling;y++)for(int[] s:sides){
			int x=x0+s[0],z=z0+s[1],n=noise(x,y,z);
			if(y==FLOOR+1?n<60:y>ceiling-2?n<75:n<14)set(level,bb,WEB,x,y,z);
		}
		for(int x=x0-3;x<=x0+3;x++)for(int z=z0-3;z<=z0+3;z++){
			double d=dist(x,z,x0,z0);if(d<0.5 || d>3.2)continue;
			int top=dome(reach(x,z));if(top<0)continue;
			if(noise(x,top,z)<(d<2?70:40))set(level,bb,WEB,x,top,z);
			if(d<2 && noise(z,top,x)<35)set(level,bb,SILK,x,top,z);
		}
	}

	/**
	 * The nest under the crown of the dome: silk packed thick and hanging down in a lump, webbing round its edges, and five
	 * cocoons dangling from it on their own threads. The space under its heart is left open, for her.
	 */
	private void nest(WorldGenLevel level,BoundingBox bb){
		for(int x=CX-6;x<=CX+6;x++)for(int z=CZ-6;z<=CZ+6;z++){
			double r=dist(x,z,CX,CZ);int depth=nest(r);if(depth==0)continue;
			int ceiling=dome(reach(x,z));
			for(int y=ceiling-depth+1;y<=ceiling;y++){
				int n=noise(x,y,z);
				set(level,bb,n<58?SILK:n<70?Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.LIGHT_GRAY).defaultBlockState():n<90?WEB:Blocks.MUSHROOM_STEM.defaultBlockState(),x,y,z);
			}
			if(r>3 && noise(x,z)<30)set(level,bb,WEB,x,ceiling-depth,z);
		}
		for(int i=0;i<5;i++){
			double a=i*Math.PI*2/5+0.5;
			int x=(int)Math.round(CX+Math.cos(a)*4.6),z=(int)Math.round(CZ+Math.sin(a)*4.6);
			int hang=dome(reach(x,z))-nest(dist(x,z,CX,CZ)),thread=1+i%3;
			fill(level,bb,x,hang-thread+1,z,x,hang,z,WEB);
			fill(level,bb,x,hang-thread-1,z,x,hang-thread,z,SILK);
		}
	}

	/** An egg sac: a lump of silk against the wall, pale stems of old fungus through it, webbed over. */
	private void sac(WorldGenLevel level,BoundingBox bb,int x0,int z0,int i){
		for(int x=x0-1;x<=x0+1;x++)for(int z=z0-1;z<=z0+1;z++){
			if(reach(x,z)>RADIUS)continue;
			int n=noise(x,z+i),h=x==x0 && z==z0?2:n<50?1:0;
			for(int y=FLOOR+1;y<=FLOOR+h;y++)set(level,bb,noise(x,y,z)<30?Blocks.MUSHROOM_STEM.defaultBlockState():SILK,x,y,z);
			if(n<70)set(level,bb,WEB,x,FLOOR+1+h,z);
		}
	}

	/** The shaft: a headframe over it, posts down its wall, a spiral of plank treads, and a flooded sump at the foot. */
	private void shaft(WorldGenLevel level,BoundingBox bb){
		for(int x=SHAFT_X-6;x<=SHAFT_X+6;x++)for(int z=SHAFT_Z-6;z<=SHAFT_Z+6;z++){
			double r=dist(x,z,SHAFT_X,SHAFT_Z);if(r>6.2 || !column(bb,x,z))continue;
			int ground=Math.min(top,surface(level,x,z));
			if(r>4.4){
				int n=noise(x,z);
				set(level,bb,(n<40?Blocks.GRAVEL:n<75?Blocks.COARSE_DIRT:Blocks.COBBLESTONE).defaultBlockState(),x,ground-1,z);
				continue;
			}
			fill(level,bb,x,FLOOR+1,z,x,HEIGHT-1,z,AIR);
			if(r<1.7){
				set(level,bb,Blocks.GRAVEL.defaultBlockState(),x,FLOOR-3,z);
				fill(level,bb,x,FLOOR-2,z,x,FLOOR,z,Blocks.WATER.defaultBlockState());
				continue;
			}
			set(level,bb,Blocks.COBBLESTONE.defaultBlockState(),x,FLOOR,z);
			// Four posts shore the wall all the way down.
			if(r>3.6 && (Math.abs(x-SHAFT_X)<=0 || Math.abs(z-SHAFT_Z)<=0)){fill(level,bb,x,FLOOR+1,z,x,ground-1,z,LOG);continue;}
			double turn=(Math.atan2(z-SHAFT_Z,x-SHAFT_X)+Math.PI)/(2*Math.PI);
			for(int k=0;;k++){
				int tread=(int)Math.floor(top-1-(turn+k)*RISE);
				if(tread<=FLOOR)break;
				if(tread>=ground)continue;
				set(level,bb,noise(x,tread,z)<20?Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState():PLANK,x,tread,z);
				if(noise(z,tread,x)<6 && r<3)set(level,bb,WEB,x,tread+2,z);
			}
		}
		// The headframe: two posts and a beam over the shaft, a lantern long out of oil hanging from it on a chain.
		for(int x:new int[]{SHAFT_X-5,SHAFT_X+5})if(column(bb,x,SHAFT_Z)){
			int ground=Math.min(top,surface(level,x,SHAFT_Z));fill(level,bb,x,ground,SHAFT_Z,x,top+4,SHAFT_Z,LOG);
		}
		for(int x=SHAFT_X-4;x<=SHAFT_X+4;x++)if(column(bb,x,SHAFT_Z))set(level,bb,LOG.setValue(RotatedPillarBlock.AXIS,Direction.Axis.X),x,top+4,SHAFT_Z);
		if(column(bb,SHAFT_X,SHAFT_Z)){
			set(level,bb,Blocks.IRON_CHAIN.defaultBlockState(),SHAFT_X,top+3,SHAFT_Z);
			set(level,bb,Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true),SHAFT_X,top+2,SHAFT_Z);
		}
	}

	/** The drift from the shaft to the hall: shored every four blocks, rails broken, and the webs thickening as it goes in. */
	private void drift(WorldGenLevel level,BoundingBox bb){
		int from=SHAFT_Z+4,to=CZ-RADIUS+3;
		for(int z=from;z<=to;z++){
			double deep=(z-from)/(double)(to-from);
			for(int x=CX-1;x<=CX+1;x++){
				set(level,bb,(noise(x,z)<60?Blocks.GRAVEL:Blocks.COBBLESTONE).defaultBlockState(),x,FLOOR,z);
				fill(level,bb,x,FLOOR+1,z,x,FLOOR+3,z,AIR);
			}
			if(noise(CX,z)<70)set(level,bb,Blocks.RAIL.defaultBlockState(),CX,FLOOR+1,z);
			if(z%4==0 && noise(z,CX)<85){
				// A timber set: two posts and a cap. Further in, one or two have given way.
				fill(level,bb,CX-1,FLOOR+1,z,CX-1,FLOOR+2,z,POST);
				fill(level,bb,CX+1,FLOOR+1,z,CX+1,FLOOR+2,z,POST);
				fill(level,bb,CX-1,FLOOR+3,z,CX+1,FLOOR+3,z,PLANK);
			}
			for(int x:new int[]{CX-1,CX+1})if(noise(x,FLOOR+3,z)<deep*70)set(level,bb,WEB,x,FLOOR+3,z);
			if(deep>0.6 && noise(z,stray(z))<deep*30)set(level,bb,WEB,stray(z),FLOOR+1,z);
		}
		set(level,bb,Blocks.LANTERN.defaultBlockState(),CX+1,FLOOR+1,from+2);
		chest(level,bb,CX-1,FLOOR+1,from+6,DungeonWorldgen.SILK_HOLLOW,Direction.EAST,1401);
		set(level,bb,Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION,4),CX+1,FLOOR+1,from+10);
	}

	/** The side of the drift a stray web clings to, by row. */
	private static int stray(int z){return CX+(z%2==0?-1:1);}

	/** The last miners: a cocooned body by the breakthrough, bones and a dropped pick in the hall, a cart run off its rails. */
	private void remains(WorldGenLevel level,BoundingBox bb){
		int mouth=CZ-RADIUS+2;
		fill(level,bb,CX+3,FLOOR+1,mouth+3,CX+3,FLOOR+2,mouth+3,SILK);
		set(level,bb,WEB,CX+3,FLOOR+3,mouth+3);
		for(int[] b:new int[][]{{CX-11,CZ-9},{CX+10,CZ+4},{CX-6,CZ+12},{CX+13,CZ-8}}){
			set(level,bb,Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS,(b[0]+b[1])%2==0?Direction.Axis.X:Direction.Axis.Z),b[0],FLOOR+1,b[1]);
			set(level,bb,Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION,(b[0]*3)%16),b[0],FLOOR+2,b[1]);
			set(level,bb,WEB,b[0]+1,FLOOR+1,b[1]);
		}
		// The rails ran a little way into the hall before the miners stopped laying them.
		for(int z=mouth;z<mouth+5;z++)if(noise(CX,z)<60)set(level,bb,Blocks.RAIL.defaultBlockState(),CX,FLOOR+1,z);
		set(level,bb,Blocks.IRON_BARS.defaultBlockState(),CX+10,FLOOR+1,CZ+5);
	}

	/** The hall, from below its floor to over its dome. */
	@Override public List<BoundingBox> wardedBoxes(){return List.of(worldBox(CX-RADIUS-2,0,CZ-RADIUS-2,CX+RADIUS+2,FLOOR+DOME+2,CZ+RADIUS+2));}
}
