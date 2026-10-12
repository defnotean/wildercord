package dev.wildercord.world.dungeons;

import dev.wildercord.aura.world.Guardians;
import dev.wildercord.config.Config;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import java.util.*;

/**
 * The Rootcoil Serpent's grove: a sinkhole in the jungle floor, a spiral of mossy treads down its wall to a plunge pool,
 * a low tunnel strewn with bones and shed skin, and a domed cavern. In the cavern a ring of water circles a moss island,
 * and on the island a gnarled tree holds the serpent coiled round its trunk, over the herb at its roots. A shaft of
 * daylight falls through the dome onto the tree. The cavern is warded: the grove is entered only by the sinkhole.
 */
public final class SerpentGrovePiece extends DungeonPiece {
	public static final int WIDTH=49,HEIGHT=44,DEPTH=64;
	/** The cavern floor, the island's centre (the tree) and the cavern's radius, in the piece's own frame. */
	public static final int FLOOR=3,CX=24,CZ=41,RADIUS=21,DOME=20;
	/** The island, the water ring and the shore: radii out from the tree. */
	public static final double ISLAND=8.5,SHORE=14;
	/** Where the herb grows: at the roots, on the side the tunnel opens from. */
	public static final int HERB_X=24,HERB_Z=35;
	private static final int SHAFT_X=24,SHAFT_Z=8,TUNNEL_X=24;
	/** How far below the surface the piece's floor lies (the cavern floor is FLOOR above that). */
	private static final int DEPTH_BELOW=36;
	/** Blocks the spiral drops each full turn round the sinkhole. */
	private static final int RISE=14;
	private static final String TOP="grove_top";
	private int top;

	public SerpentGrovePiece(int x,int y,int z,Direction facing,int top){super(DungeonWorldgen.SERPENT_GROVE_PIECE,x,y,z,WIDTH,HEIGHT,DEPTH,facing);this.top=top;}
	public SerpentGrovePiece(CompoundTag tag){super(DungeonWorldgen.SERPENT_GROVE_PIECE,tag);top=tag.getIntOr(TOP,HEIGHT-6);}
	@Override protected void addAdditionalSaveData(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext context,CompoundTag tag){
		super.addAdditionalSaveData(context,tag);tag.putInt(TOP,top);
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c){
		if(!Config.get().auraWorld().guardians())return Optional.empty();
		var probe=new SerpentGrovePiece(c.chunkPos().getMinBlockX(),0,c.chunkPos().getMinBlockZ(),Direction.Plane.HORIZONTAL.getRandomDirection(c.random()),0);
		var gen=c.chunkGenerator();int sea=gen.getSeaLevel();
		// The sinkhole needs dry, fairly level ground round its lip: no lake, no cliff edge.
		int high=Integer.MIN_VALUE,low=Integer.MAX_VALUE;
		for(int[] at:new int[][]{{SHAFT_X,SHAFT_Z},{SHAFT_X-5,SHAFT_Z},{SHAFT_X+5,SHAFT_Z},{SHAFT_X,SHAFT_Z-5},{SHAFT_X,SHAFT_Z+5}}) {
			var p=probe.localPosition(at[0],0,at[1]);
			int surface=gen.getFirstOccupiedHeight(p.getX(),p.getZ(),Heightmap.Types.WORLD_SURFACE_WG,c.heightAccessor(),c.randomState());
			int floor=gen.getFirstOccupiedHeight(p.getX(),p.getZ(),Heightmap.Types.OCEAN_FLOOR_WG,c.heightAccessor(),c.randomState());
			if(surface<=sea+1 || surface-floor>1)return Optional.empty();
			high=Math.max(high,surface);low=Math.min(low,surface);
		}
		if(high-low>5)return Optional.empty();
		int base=low-DEPTH_BELOW;
		if(base<c.heightAccessor().getMinY()+4)return Optional.empty();
		var piece=new SerpentGrovePiece(c.chunkPos().getMinBlockX(),base,c.chunkPos().getMinBlockZ(),probe.getOrientation(),high-base);
		var entrance=piece.localPosition(SHAFT_X,piece.top,SHAFT_Z);
		return Optional.of(new Structure.GenerationStub(entrance,b->b.addPiece(piece)));
	}

	/** The first block of open air over a column, in the piece's own frame: where the jungle floor is. */
	private int surface(WorldGenLevel level,int x,int z){
		var p=getWorldPos(x,0,z);return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,p.getX(),p.getZ())-getWorldY(0);
	}

	/** Whether a column lies in the chunk being built: only those columns' heights may be read while it generates. */
	private boolean column(BoundingBox bb,int x,int z){
		var p=getWorldPos(x,0,z);return p.getX()>=bb.minX() && p.getX()<=bb.maxX() && p.getZ()>=bb.minZ() && p.getZ()<=bb.maxZ();
	}

	/** The dome's ceiling over a column {@code r} blocks from the tree, or -1 outside the cavern. */
	public static int dome(double r){return r>RADIUS?-1:FLOOR+3+(int)Math.round((DOME-3)*Math.sqrt(1-(r/RADIUS)*(r/RADIUS)));}

	@Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox bb,ChunkPos chunk,BlockPos ref){
		DungeonWards.remember(level,this);
		shell(level,bb);
		cavern(level,bb);
		tree(level,bb);
		sinkhole(level,bb);
		tunnel(level,bb);
		ceiling(level,bb);
		serpent(level,bb);
	}

	/** The guardian, coiled round the trunk at its foot and watching the herb. Spawned once, by whichever chunk holds the trunk. */
	private void serpent(WorldGenLevel level,BoundingBox bb){
		var foot=getWorldPos(CX,FLOOR+2,CZ);if(!bb.isInside(foot))return;
		var herb=getWorldPos(HERB_X,FLOOR+2,HERB_Z);
		float rest=(float)Math.toDegrees(Math.atan2(-(herb.getX()-foot.getX()),herb.getZ()-foot.getZ()));
		var serpent=Guardians.SERPENT.create(level.getLevel(),net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);if(serpent==null)return;
		serpent.snapTo(foot.getX()+.5,foot.getY(),foot.getZ()+.5,rest,0);
		serpent.finalizeSpawn(level,level.getCurrentDifficultyAt(foot),net.minecraft.world.entity.EntitySpawnReason.STRUCTURE,null);
		serpent.bind(foot,rest);
		level.addFreshEntityWithPassengers(serpent);
	}

	private BlockState rock(int x,int y,int z){
		int n=noise(x,y,z);
		return n<14?Blocks.MOSSY_COBBLESTONE.defaultBlockState():n<24?Blocks.TUFF.defaultBlockState():n<32?Blocks.ANDESITE.defaultBlockState()
			:n<38?Blocks.MOSS_BLOCK.defaultBlockState():Blocks.STONE.defaultBlockState();
	}

	/** Solid rock round the cavern, the sinkhole and the tunnel first, so no cave, aquifer or lava lake breaks into the grove. */
	private void shell(WorldGenLevel level,BoundingBox bb){
		for(int x=0;x<WIDTH;x++)for(int z=0;z<DEPTH;z++){
			double r=dist(x,z,CX,CZ);
			int ceiling=r<=RADIUS+2?dome(Math.min(r,RADIUS))+2:-1;
			if(r<=RADIUS+2)for(int y=0;y<=ceiling;y++)set(level,bb,rock(x,y,z),x,y,z);
			double s=dist(x,z,SHAFT_X,SHAFT_Z);
			if(s<=6.2 && column(bb,x,z)){int lip=Math.min(top,surface(level,x,z))-1;for(int y=0;y<lip;y++)set(level,bb,rock(x,y,z),x,y,z);}
			if(x>=TUNNEL_X-4 && x<=TUNNEL_X+4 && z>=SHAFT_Z+3 && z<=CZ-RADIUS+3)for(int y=0;y<=FLOOR+7;y++)set(level,bb,rock(x,y,z),x,y,z);
		}
	}

	private void cavern(WorldGenLevel level,BoundingBox bb){
		BlockState water=Blocks.WATER.defaultBlockState();
		for(int x=0;x<WIDTH;x++)for(int z=0;z<DEPTH;z++){
			double r=dist(x,z,CX,CZ);if(r>RADIUS)continue;
			int ceiling=dome(r),n=noise(x,z);
			fill(level,bb,x,FLOOR+1,z,x,ceiling,z,AIR);
			if(r>=ISLAND && r<SHORE){
				// The ring: three blocks of water over clay and gravel, deep enough for the serpent to vanish into.
				set(level,bb,n<55?Blocks.CLAY.defaultBlockState():n<85?Blocks.GRAVEL.defaultBlockState():Blocks.MUD.defaultBlockState(),x,0,z);
				fill(level,bb,x,1,z,x,FLOOR,z,water);
				if(n%17==0 && r>ISLAND+0.8 && r<SHORE-0.8)set(level,bb,Blocks.LILY_PAD.defaultBlockState(),x,FLOOR+1,z);
				continue;
			}
			boolean island=r<ISLAND;
			int ground=island && r<5?FLOOR+1:FLOOR;
			BlockState soil=island?(n<62?Blocks.MOSS_BLOCK:n<84?Blocks.ROOTED_DIRT:Blocks.COARSE_DIRT).defaultBlockState()
				:r<SHORE+1.5?(n<50?Blocks.CLAY:Blocks.MUD).defaultBlockState()
				:(n<58?Blocks.MOSS_BLOCK:n<78?Blocks.COARSE_DIRT:n<90?Blocks.ROOTED_DIRT:Blocks.MOSSY_COBBLESTONE).defaultBlockState();
			fill(level,bb,x,1,z,x,ground-1,z,Blocks.DIRT.defaultBlockState());set(level,bb,soil,x,ground,z);
			if(island && r<2.6)continue;
			if(x==HERB_X && z==HERB_Z)continue;
			// Ground cover keeps to the edges, so the fight has open footing.
			int at=ground+1;
			if(!island && r>SHORE+1.5 && r<RADIUS-1.5){
				if(n<12)set(level,bb,Blocks.MOSS_CARPET.defaultBlockState(),x,at,z);
				else if(n>=95 && r>RADIUS-4)set(level,bb,Blocks.AZALEA.defaultBlockState(),x,at,z);
				else if(n>=92 && r>RADIUS-4)set(level,bb,Blocks.FERN.defaultBlockState(),x,at,z);
			}
			else if(!island && r<SHORE+1.5 && n<7)set(level,bb,Blocks.BIG_DRIPLEAF.defaultBlockState(),x,at,z);
			else if(island && r>5.5 && n<16)set(level,bb,Blocks.MOSS_CARPET.defaultBlockState(),x,at,z);
		}
		// What the serpent left of the last ones who came: bones, a skull, a rusted blade, and its own skin, shed in a long curl.
		for(int[] b:new int[][]{{9,30},{38,47},{14,55},{35,27}}){
			set(level,bb,Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS,(b[0]+b[1])%2==0?Direction.Axis.X:Direction.Axis.Z),b[0],FLOOR+1,b[1]);
			set(level,bb,Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION,(b[0]*7)%16),b[0]+1,FLOOR+1,b[1]);
		}
		set(level,bb,Blocks.IRON_BARS.defaultBlockState(),39,FLOOR+1,46);
		skin(level,bb,10,49,8,0.0,FLOOR+1);
		skin(level,bb,33,24,7,2.2,FLOOR+1);
		// A shaft of daylight falls through the dome onto the tree.
		for(int x=CX-1;x<=CX+1;x++)for(int z=CZ-1;z<=CZ+1;z++)if(dist(x,z,CX,CZ)<1.5 && column(bb,x,z))
			fill(level,bb,x,dome(0)-1,z,x,Math.min(HEIGHT-1,surface(level,x,z)),z,AIR);
	}

	/** A shed skin: a curl of pale moss along the floor, {@code length} blocks long, starting at a heading of {@code heading}. */
	private void skin(WorldGenLevel level,BoundingBox bb,int x0,int z0,int length,double heading,int y){
		double x=x0,z=z0,a=heading;
		for(int i=0;i<length*2;i++){
			set(level,bb,Blocks.PALE_MOSS_CARPET.defaultBlockState(),(int)Math.round(x),y,(int)Math.round(z));
			a+=Math.sin(i*0.7)*0.45;x+=Math.cos(a)*0.5;z+=Math.sin(a)*0.5;
		}
	}

	/** The gnarled tree: a thick trunk, roots spread through the island, four crooked limbs and a canopy the serpent coils into. */
	private void tree(WorldGenLevel level,BoundingBox bb){
		BlockState log=Blocks.JUNGLE_LOG.defaultBlockState(),wood=Blocks.JUNGLE_WOOD.defaultBlockState();
		int ground=FLOOR+2,crown=ground+11;
		for(int x=CX-2;x<=CX+2;x++)for(int z=CZ-2;z<=CZ+2;z++){
			double r=dist(x,z,CX,CZ);
			int height=r<1.2?crown:r<1.9?crown-2-noise(x,z)%3:r<2.5?ground+2+noise(x,z)%3:-1;
			if(height>0)fill(level,bb,x,ground,z,x,height,z,r<1.2?log:wood);
		}
		// Six roots run out across the island and down into the water's edge.
		for(int i=0;i<6;i++){
			double a=i*Math.PI/3+0.4;
			for(double d=2.5;d<=ISLAND+0.6;d+=0.5){
				int x=(int)Math.round(CX+Math.cos(a+d*0.08)*d),z=(int)Math.round(CZ+Math.sin(a+d*0.08)*d);
				if(x==HERB_X && z==HERB_Z)continue;
				int y=d<4.5?ground:d<6?FLOOR+1:FLOOR;
				set(level,bb,d<5?wood:Blocks.MANGROVE_ROOTS.defaultBlockState(),x,y,z);
			}
		}
		// Limbs bend outward and up; each ends in a mass of leaves lit from within.
		for(int i=0;i<4;i++){
			double a=i*Math.PI/2+0.8;int y=ground+8+i%2;
			double x=CX,z=CZ;
			for(int step=0;step<7;step++){
				x+=Math.cos(a)*0.9;z+=Math.sin(a)*0.9;if(step%2==1)y++;
				set(level,bb,wood,(int)Math.round(x),y,(int)Math.round(z));
			}
			canopy(level,bb,(int)Math.round(x),y+1,(int)Math.round(z),3);
		}
		canopy(level,bb,CX,crown+1,CZ,3);
		// The herb's bed: a hollow between two roots, bare earth under the trunk's shadow, and the Coilroot growing in it.
		set(level,bb,Blocks.ROOTED_DIRT.defaultBlockState(),HERB_X,FLOOR,HERB_Z);
		set(level,bb,dev.wildercord.aura.world.Coilroot.BLOCK.defaultBlockState(),HERB_X,FLOOR+1,HERB_Z);set(level,bb,AIR,HERB_X,FLOOR+2,HERB_Z);
	}

	private void canopy(WorldGenLevel level,BoundingBox bb,int cx,int cy,int cz,int radius){
		for(int x=cx-radius;x<=cx+radius;x++)for(int y=cy-1;y<=cy+radius-1;y++)for(int z=cz-radius;z<=cz+radius;z++){
			double d=Math.sqrt((x-cx)*(x-cx)+(y-cy)*(y-cy)*1.6+(z-cz)*(z-cz));int n=noise(x,y,z);
			if(d>radius+0.2 || n<10 || dist(x,z,CX,CZ)>RADIUS-2)continue;
			Block leaf=n<40?Blocks.FLOWERING_AZALEA_LEAVES:n<75?Blocks.AZALEA_LEAVES:Blocks.JUNGLE_LEAVES;
			set(level,bb,d<1.2 && n%5==0?Blocks.SHROOMLIGHT.defaultBlockState():leaf.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true),x,y,z);
		}
	}

	/** The sinkhole: open to the sky, a spiral of treads down its wall, and a plunge pool for anyone who misses a step. */
	private void sinkhole(WorldGenLevel level,BoundingBox bb){
		for(int x=SHAFT_X-6;x<=SHAFT_X+6;x++)for(int z=SHAFT_Z-6;z<=SHAFT_Z+6;z++){
			double r=dist(x,z,SHAFT_X,SHAFT_Z);if(r>6.2 || !column(bb,x,z))continue;
			if(r>4.4){
				// The lip: coarse earth and moss, so the hole reads from a distance.
				var lip=noise(x,z)<45?Blocks.COARSE_DIRT:noise(x,z)<75?Blocks.MOSS_BLOCK:Blocks.MOSSY_COBBLESTONE;
				set(level,bb,lip.defaultBlockState(),x,Math.min(top,surface(level,x,z))-1,z);
				continue;
			}
			fill(level,bb,x,FLOOR+1,z,x,HEIGHT-1,z,AIR);
			if(r<1.7){
				set(level,bb,Blocks.CLAY.defaultBlockState(),x,FLOOR-3,z);
				fill(level,bb,x,FLOOR-2,z,x,FLOOR,z,Blocks.WATER.defaultBlockState());
				continue;
			}
			set(level,bb,Blocks.MOSSY_COBBLESTONE.defaultBlockState(),x,FLOOR,z);
			double turn=(Math.atan2(z-SHAFT_Z,x-SHAFT_X)+Math.PI)/(2*Math.PI);
			int ground=Math.min(top,surface(level,x,z));
			for(int k=0;;k++){
				int tread=(int)Math.floor(top-1-(turn+k)*RISE);
				if(tread<=FLOOR)break;
				if(tread>=ground)continue;
				set(level,bb,(noise(x,tread,z)<30?Blocks.MOSSY_COBBLESTONE:Blocks.MOSSY_STONE_BRICKS).defaultBlockState(),x,tread,z);
				set(level,bb,Blocks.COBBLESTONE.defaultBlockState(),x,tread-1,z);
			}
		}
	}

	/** The tunnel from the sinkhole's foot to the cavern: low, winding a little, and full of warnings. */
	private void tunnel(WorldGenLevel level,BoundingBox bb){
		int from=SHAFT_Z+4,to=CZ-RADIUS+2;
		for(int z=from;z<=to;z++){
			int bend=(int)Math.round(Math.sin(z*0.45)*1.2);
			for(int x=TUNNEL_X-2+bend;x<=TUNNEL_X+2+bend;x++){
				boolean edge=x==TUNNEL_X-2+bend || x==TUNNEL_X+2+bend;
				set(level,bb,(noise(x,z)<55?Blocks.MOSS_BLOCK:Blocks.COARSE_DIRT).defaultBlockState(),x,FLOOR,z);
				fill(level,bb,x,FLOOR+1,z,x,FLOOR+(edge?3:4),z,AIR);
			}
			if(z%3==0)set(level,bb,Blocks.HANGING_ROOTS.defaultBlockState(),TUNNEL_X+bend+(z%2==0?1:-1),FLOOR+4,z);
		}
		set(level,bb,Blocks.COBWEB.defaultBlockState(),TUNNEL_X-2+(int)Math.round(Math.sin(15*0.45)*1.2),FLOOR+3,15);
		set(level,bb,Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION,8),TUNNEL_X+1+(int)Math.round(Math.sin(13*0.45)*1.2),FLOOR+1,13);
		set(level,bb,Blocks.BONE_BLOCK.defaultBlockState(),TUNNEL_X-1+(int)Math.round(Math.sin(17*0.45)*1.2),FLOOR+1,17);
		skin(level,bb,TUNNEL_X,from+1,5,Math.PI/2,FLOOR+1);
		chest(level,bb,TUNNEL_X+2+(int)Math.round(Math.sin(19*0.45)*1.2)-1,FLOOR+1,19,DungeonWorldgen.GROVE_SUPPLIES,Direction.WEST,1201);
	}

	/** Glow berries hang from the dome: the cavern's only light besides the shaft and the canopy. */
	private void ceiling(WorldGenLevel level,BoundingBox bb){
		for(int x=0;x<WIDTH;x++)for(int z=0;z<DEPTH;z++){
			double r=dist(x,z,CX,CZ);int n=noise(x,z);
			if(r>RADIUS-1 || r<9.5 || n>=7)continue;
			int ceiling=dome(r),length=2+n%4;
			for(int i=0;i<length;i++){
				int y=ceiling-i;if(y<=FLOOR+6)break;
				boolean end=i==length-1;
				var vine=(end?Blocks.CAVE_VINES:Blocks.CAVE_VINES_PLANT).defaultBlockState().setValue(CaveVines.BERRIES,(n+i)%2==0);
				set(level,bb,vine,x,y,z);
			}
		}
	}

	@Override public List<BoundingBox> wardedBoxes(){return List.of(worldBox(CX-RADIUS-2,0,CZ-RADIUS-2,CX+RADIUS+2,dome(0)+2,CZ+RADIUS+2));}
}
