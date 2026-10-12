package dev.wildercord.world.dungeons;

import dev.wildercord.config.Config;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import java.util.*;

/**
 * Mojito's grove: a round meadow of deep grass and flowers, ringed by great old oaks whose crowns lean in over it, laid out in its own
 * coordinates (x across, y up, z inward from the gate). The ground is levelled to one floor (you stand at {@code F+1}), with a low
 * mossy bank round it that the oaks stand on. A worn path comes in through a gap in the bank, between two stones hung with a
 * collar's worth of chain, past a cold fire, a chest and a tipped-over water bowl. At the grove's heart is his bed: a flattened
 * hollow in the grass before an old standing stone grown over with moss, chewed bones about it; he sleeps curled up on it, facing
 * the gate, his sword in the grass before him. Pale mist lies low among the oaks' roots, drifts of white carpet in the shade.
 *
 * <pre>
 *   z 0-6    the path, through the woods to the gate: a cold fire, a chest, the bowl
 *   z 7-11   the gate, two standing stones with chain hung between them
 *   z 15-51  the grove, a circle of radius 18 round the stone at (25, 33)
 *   bank     radius 18-23, rising three blocks, its oaks standing on it
 * </pre>
 */
public final class MojitoGrovePiece extends DungeonPiece {
	public static final int WIDTH=51,HEIGHT=36,DEPTH=59;
	/** The grove floor's local y: the ground block; you stand one above it. */
	public static final int F=8;
	public static final int CX=25,CZ=33;
	/** The grove's radius, and how far past it the bank runs. */
	public static final double R=18,BANK=5;
	/** Where he sleeps: in the hollow before the stone, facing the gate. */
	public static final int REST_X=CX,REST_Z=CZ-7;

	private static final BlockState LOG=Blocks.OAK_LOG.defaultBlockState();
	private static final BlockState WOOD=Blocks.OAK_WOOD.defaultBlockState();
	private static final BlockState LEAVES=Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true);
	private static final BlockState MIST=Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState();
	private static final BlockState[] FLOWERS={Blocks.OXEYE_DAISY.defaultBlockState(),Blocks.AZURE_BLUET.defaultBlockState(),
		Blocks.LILY_OF_THE_VALLEY.defaultBlockState(),Blocks.CORNFLOWER.defaultBlockState(),Blocks.WHITE_TULIP.defaultBlockState()};

	public MojitoGrovePiece(int x,int y,int z,Direction facing){super(DungeonWorldgen.MOJITO_GROVE_PIECE,x,y,z,WIDTH,HEIGHT,DEPTH,facing);}
	public MojitoGrovePiece(CompoundTag tag){super(DungeonWorldgen.MOJITO_GROVE_PIECE,tag);}

	/** Needs dry, fairly level ground across the whole grove: no lake, no ravine, no hill through the middle. */
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c){
		if(!Config.get().auraWorld().guardians())return Optional.empty();
		var piece=new MojitoGrovePiece(c.chunkPos().getMinBlockX(),0,c.chunkPos().getMinBlockZ(),Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		var gen=c.chunkGenerator();int sea=gen.getSeaLevel();
		int high=Integer.MIN_VALUE,low=Integer.MAX_VALUE;
		for(int i=0;i<=8;i++){
			double a=i*Math.PI/4;double r=i==8?0:14;
			var p=piece.getWorldPos(CX+(int)Math.round(Math.cos(a)*r),0,CZ+(int)Math.round(Math.sin(a)*r));
			int surface=gen.getFirstOccupiedHeight(p.getX(),p.getZ(),Heightmap.Types.WORLD_SURFACE_WG,c.heightAccessor(),c.randomState());
			int floor=gen.getFirstOccupiedHeight(p.getX(),p.getZ(),Heightmap.Types.OCEAN_FLOOR_WG,c.heightAccessor(),c.randomState());
			if(surface<=sea+1 || surface-floor>1)return Optional.empty();
			high=Math.max(high,floor);low=Math.min(low,floor);
		}
		if(high-low>5)return Optional.empty();
		int ground=(high+low)/2;
		piece.move(0,ground-1-F,0);
		var gate=piece.getWorldPos(CX,F+1,2);
		return Optional.of(new Structure.GenerationStub(new BlockPos(gate.getX(),ground,gate.getZ()),b->b.addPiece(piece)));
	}

	@Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox bb,ChunkPos chunk,BlockPos ref){
		DungeonWards.remember(level,this);
		ground(level,bb);
		path(level,bb);
		for(int i=0;i<12;i++)oak(level,bb,i);
		heart(level,bb);
		mojito(level,bb);
	}

	/** The guardian, curled up in his hollow and facing the gate. Spawned once, by whichever chunk holds his bed. */
	private void mojito(WorldGenLevel level,BoundingBox bb){
		var foot=getWorldPos(REST_X,F+1,REST_Z);if(!bb.isInside(foot))return;
		var gate=getWorldPos(REST_X,F+1,REST_Z-10);
		float rest=(float)Math.toDegrees(Math.atan2(-(gate.getX()-foot.getX()),gate.getZ()-foot.getZ()));
		var dog=dev.wildercord.aura.world.Guardians.MOJITO.create(level.getLevel(),net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);if(dog==null)return;
		dog.snapTo(foot.getX()+.5,foot.getY(),foot.getZ()+.5,rest,0);
		dog.finalizeSpawn(level,level.getCurrentDifficultyAt(foot),net.minecraft.world.entity.EntitySpawnReason.STRUCTURE,null);
		dog.bind(foot,rest,worldBox(CX-(int)R,F,CZ-(int)R,CX+(int)R,F+16,CZ+(int)R));
		level.addFreshEntityWithPassengers(dog);
	}

	/** How high the bank stands over a column {@code r} from the stone: nothing in the grove, rising three blocks to its outer edge. */
	private static int bank(double r){return r<=R?0:(int)Math.round(Math.min(BANK,r-R)*0.6);}
	/** Whether a column is in the cut the path makes through the bank. */
	private static boolean cut(int x,int z){return Math.abs(x-CX)<=3 && z<CZ;}

	/** The levelled meadow and its bank: earth under it all, the air cleared over it, grass and flowers, mist lying at the edges. */
	private void ground(WorldGenLevel level,BoundingBox bb){
		for(int x=0;x<WIDTH;x++)for(int z=0;z<DEPTH;z++){
			double r=dist(x,z,CX,CZ);if(r>R+BANK+0.5)continue;
			int h=cut(x,z)?0:bank(r),n=noise(x,z);
			fill(level,bb,x,0,z,x,F-4,z,Blocks.STONE.defaultBlockState());
			fill(level,bb,x,F-3,z,x,F-1+h,z,Blocks.DIRT.defaultBlockState());
			BlockState top=r>R && n<40?Blocks.MOSS_BLOCK.defaultBlockState():r>R-2 && n<10?Blocks.PODZOL.defaultBlockState():Blocks.GRASS_BLOCK.defaultBlockState();
			set(level,bb,top,x,F+h,z);
			fill(level,bb,x,F+1+h,z,x,HEIGHT-1,z,AIR);
			int at=F+1+h;
			if(r<=R){
				// Mist lies thick under the oaks and thins out toward the middle, where the grass is short and the fight will be.
				if(r>R-3 && n<45 || r>R-6 && n<14)set(level,bb,MIST,x,at,z);
				else if(r>R-6 && n>=88)set(level,bb,FLOWERS[n%FLOWERS.length],x,at,z);
				else if(r>R-8 && n%7==0)set(level,bb,Blocks.SHORT_GRASS.defaultBlockState(),x,at,z);
			}else if(n>=90 && !cut(x,z))set(level,bb,Blocks.FERN.defaultBlockState(),x,at,z);
			else if(n<30 && !cut(x,z))set(level,bb,MIST,x,at,z);
			else if(n%4==0 && !cut(x,z))set(level,bb,Blocks.SHORT_GRASS.defaultBlockState(),x,at,z);
		}
	}

	/** The worn path in from the woods, through the cut in the bank, between the chained gate stones. */
	private void path(WorldGenLevel level,BoundingBox bb){
		for(int z=0;z<CZ-R+1;z++)for(int x=CX-3;x<=CX+3;x++){
			if(dist(x,z,CX,CZ)<=R+BANK+0.5)continue;
			fill(level,bb,x,F-3,z,x,F-1,z,Blocks.DIRT.defaultBlockState());
			set(level,bb,(Math.abs(x-CX)<=1?Blocks.DIRT_PATH:noise(x,z)<50?Blocks.GRASS_BLOCK:Blocks.COARSE_DIRT).defaultBlockState(),x,F,z);
			fill(level,bb,x,F+1,z,x,F+6,z,AIR);
		}
		// The gate: two mossy standing stones, a gold chain sagging between them.
		for(int x:new int[]{CX-3,CX+3}){
			fill(level,bb,x,F+1,9,x,F+4,9,Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
			set(level,bb,Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState(),x,F+5,9);
		}
		for(int x=CX-2;x<=CX+2;x++)set(level,bb,Blocks.IRON_CHAIN.defaultBlockState().setValue(ChainBlock.AXIS,Direction.Axis.X),x,Math.abs(x-CX)<=1?F+3:F+4,9);
		set(level,bb,Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true),CX,F+2,9);
		// By the path: a cold fire, the last visitors' chest, and a tipped water bowl.
		set(level,bb,Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT,false),CX-3,F+1,3);
		chest(level,bb,CX+3,F+1,4,DungeonWorldgen.MOJITO_GROVE,Direction.WEST,1401);
		set(level,bb,Blocks.CAULDRON.defaultBlockState(),CX-3,F+1,6);
	}

	/**
	 * One of the great oaks on the bank, {@code i} of twelve round it (the gate's gap left clear): a thick trunk, roots spreading
	 * at its foot, and a broad crown leaning in over the grove.
	 */
	private void oak(WorldGenLevel level,BoundingBox bb,int i){
		double a=-Math.PI/2+(i+0.5)*Math.PI*2/12;
		if(Math.abs(Math.IEEEremainder(a+Math.PI/2,Math.PI*2))<0.35)return;
		double r=R+2.5+(i%2)*0.8;
		int x=(int)Math.round(CX+Math.cos(a)*r),z=(int)Math.round(CZ+Math.sin(a)*r);
		int foot=F+1+bank(dist(x,z,CX,CZ)),height=8+noise(x,z)%5;
		for(int y=foot;y<foot+height;y++)for(int dx=0;dx<=1;dx++)for(int dz=0;dz<=1;dz++)set(level,bb,y==foot?WOOD:LOG,x+dx,y,z+dz);
		for(int d=0;d<4;d++){
			var dir=Direction.from2DDataValue(d);
			int rx=x+(dir.getStepX()>0?2:dir.getStepX()<0?-1:noise(i,d)%2),rz=z+(dir.getStepZ()>0?2:dir.getStepZ()<0?-1:noise(d,i)%2);
			set(level,bb,WOOD,rx,foot,rz);
		}
		// The crown, leaning in: centred a little toward the grove's middle, a squashed ball of leaves.
		double cx=x+.5-Math.cos(a)*2.5,cz=z+.5-Math.sin(a)*2.5;int cy=foot+height;
		for(int dx=-5;dx<=5;dx++)for(int dy=-2;dy<=3;dy++)for(int dz=-5;dz<=5;dz++){
			double e=(dx*dx+dz*dz)/25.0+(dy*dy)/(dy<0?6.0:9.0);
			int lx=(int)Math.floor(cx)+dx,ly=cy+dy,lz=(int)Math.floor(cz)+dz;
			if(e>1 || e>0.75 && noise(lx,ly,lz)<45)continue;
			set(level,bb,LEAVES,lx,ly,lz);
		}
		// A branch from the trunk's top out into the crown.
		for(int s=1;s<=3;s++)set(level,bb,WOOD,(int)Math.round(x+.5-Math.cos(a)*s),cy-1+s/2,(int)Math.round(z+.5-Math.sin(a)*s));
	}

	/**
	 * The grove's heart: an old standing stone grown over with moss, a hollow trampled in the grass before it where he sleeps, chewed
	 * bones about, a ball he left behind.
	 */
	private void heart(WorldGenLevel level,BoundingBox bb){
		for(int x=CX-1;x<=CX+1;x++)for(int z=CZ-1;z<=CZ;z++){
			int top=F+3+(x==CX?2:noise(x,z)%2);
			for(int y=F+1;y<=top;y++)set(level,bb,noise(x,y,z)<40?Blocks.MOSSY_COBBLESTONE.defaultBlockState():Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),x,y,z);
		}
		set(level,bb,Blocks.MOSS_CARPET.defaultBlockState(),CX,F+6,CZ);
		set(level,bb,Blocks.MOSS_CARPET.defaultBlockState(),CX-1,F+5,CZ-1);
		// His bed: a hollow of flattened grass before the stone, nothing growing in it.
		for(int x=REST_X-3;x<=REST_X+3;x++)for(int z=REST_Z-4;z<=REST_Z+3;z++){
			if(dist(x,z,REST_X,REST_Z)>3.6)continue;
			set(level,bb,(noise(x,z)<30?Blocks.COARSE_DIRT:Blocks.GRASS_BLOCK).defaultBlockState(),x,F,z);set(level,bb,AIR,x,F+1,z);
		}
		// Chewed bones about the bed and the stone, and a ball: a slime block, a little the worse for him.
		for(int[] b:new int[][]{{CX-5,CZ-3},{CX+5,CZ-2},{CX+3,CZ+3},{CX-4,CZ+4},{CX+7,CZ-9}})
			set(level,bb,Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS,(b[0]+b[1])%2==0?Direction.Axis.X:Direction.Axis.Z),b[0],F+1,b[1]);
		set(level,bb,Blocks.SLIME_BLOCK.defaultBlockState(),CX-8,F+1,CZ-11);
		// A ring of white flowers round the stone's foot.
		for(int k=0;k<10;k++){
			double t=k*Math.PI/5;int x=(int)Math.round(CX+Math.cos(t)*2.6),z=(int)Math.round(CZ-.5+Math.sin(t)*2.6);
			if(dist(x,z,REST_X,REST_Z)<=3.6)continue;
			set(level,bb,FLOWERS[k%FLOWERS.length],x,F+1,z);
		}
	}

	/** The grove and its bank, from below its floor to over the tallest oak's crown. */
	@Override public List<BoundingBox> wardedBoxes(){return List.of(worldBox(0,0,0,WIDTH-1,HEIGHT-1,DEPTH-1));}
}
