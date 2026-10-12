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
 * The Ashen Stag's glade: a clearing burnt out of the forest, laid out in its own coordinates (x across, y up, z inward from the
 * gate). The ground is levelled to one floor (you stand at {@code F+1}), sunk into a low bank of scorched earth that a ring of
 * dead, blackened trees stands on. A burnt path comes in through a gap in the bank, past a fallen gate and what is left of the
 * hunters who came before. In the glade a few charred trunks still stand, and at its heart a great burnt stump smoulders, embers
 * still alive in its hollow top; the stag beds down in the ash before it, facing the gate.
 *
 * <pre>
 *   z 0-6    the path, through the woods to the gate: a cold campfire, a chest, bones
 *   z 7-12   the gate, a cut through the bank, a fallen lintel
 *   z 13-47  the glade, a circle of radius 17 round the stump at (24, 30)
 *   bank     radius 17-23, rising four blocks, its dead trees standing on it
 * </pre>
 */
public final class AshenGladePiece extends DungeonPiece {
	public static final int WIDTH=49,HEIGHT=34,DEPTH=55;
	/** The glade floor's local y: the ground block; you stand one above it. */
	public static final int F=8;
	public static final int CX=24,CZ=30;
	/** The glade's radius, and how far past it the bank runs. */
	public static final double R=17,BANK=6;
	/** Where the stag beds down: in the ash before the stump, facing the gate. */
	public static final int REST_X=CX,REST_Z=CZ-8;
	/** The trunks still standing in the glade: where a charge that misses can end with its antlers in the wood. */
	public static final int[][] TRUNKS={{13,23},{35,23},{11,36},{37,36},{24,43}};

	private static final BlockState LOG=Blocks.DARK_OAK_LOG.defaultBlockState();
	private static final BlockState WOOD=Blocks.DARK_OAK_WOOD.defaultBlockState();
	private static final BlockState CHAR=Blocks.BASALT.defaultBlockState();
	private static final BlockState ASH=Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.LIGHT_GRAY).defaultBlockState();

	public AshenGladePiece(int x,int y,int z,Direction facing){super(DungeonWorldgen.ASHEN_GLADE_PIECE,x,y,z,WIDTH,HEIGHT,DEPTH,facing);}
	public AshenGladePiece(CompoundTag tag){super(DungeonWorldgen.ASHEN_GLADE_PIECE,tag);}

	/** Needs dry, fairly level forest floor across the whole glade: no lake, no ravine, no hill through the middle. */
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c){
		if(!Config.get().auraWorld().guardians())return Optional.empty();
		var piece=new AshenGladePiece(c.chunkPos().getMinBlockX(),0,c.chunkPos().getMinBlockZ(),Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		var gen=c.chunkGenerator();int sea=gen.getSeaLevel();
		int high=Integer.MIN_VALUE,low=Integer.MAX_VALUE;
		for(int i=0;i<=8;i++){
			double a=i*Math.PI/4;double r=i==8?0:13;
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
		for(int i=0;i<14;i++)deadTree(level,bb,i);
		for(int[] t:TRUNKS)trunk(level,bb,t[0],t[1]);
		stump(level,bb);
		remains(level,bb);
		stag(level,bb);
	}

	/** The guardian, bedded in the ash before the stump and facing the gate. Spawned once, by whichever chunk holds its bed. */
	private void stag(WorldGenLevel level,BoundingBox bb){
		var foot=getWorldPos(REST_X,F+1,REST_Z);if(!bb.isInside(foot))return;
		var gate=getWorldPos(REST_X,F+1,REST_Z-10);
		float rest=(float)Math.toDegrees(Math.atan2(-(gate.getX()-foot.getX()),gate.getZ()-foot.getZ()));
		var stag=dev.wildercord.aura.world.Guardians.ASHEN_STAG.create(level.getLevel(),net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);if(stag==null)return;
		stag.snapTo(foot.getX()+.5,foot.getY(),foot.getZ()+.5,rest,0);
		stag.finalizeSpawn(level,level.getCurrentDifficultyAt(foot),net.minecraft.world.entity.EntitySpawnReason.STRUCTURE,null);
		stag.bind(foot,rest,worldBox(CX-(int)R,F,CZ-(int)R,CX+(int)R,F+14,CZ+(int)R));
		level.addFreshEntityWithPassengers(stag);
	}

	/** How high the bank stands over a column {@code r} from the stump: nothing in the glade, rising four blocks to its outer edge. */
	private static int bank(double r){return r<=R?0:(int)Math.round(Math.min(BANK,r-R)*0.7);}
	/** Whether a column is in the cut the path makes through the bank. */
	private static boolean cut(int x,int z){return Math.abs(x-CX)<=3 && z<CZ;}

	private BlockState floor(int x,int z){
		int n=noise(x,z);
		return (n<34?Blocks.COARSE_DIRT:n<52?Blocks.SOUL_SOIL:n<66?Blocks.PODZOL:n<78?Blocks.GRAVEL:n<90?Blocks.TUFF:Blocks.ROOTED_DIRT).defaultBlockState();
	}

	/** The levelled glade and its bank: earth packed under it all, the air cleared over it, ash drifted at the edges. */
	private void ground(WorldGenLevel level,BoundingBox bb){
		for(int x=0;x<WIDTH;x++)for(int z=0;z<DEPTH;z++){
			double r=dist(x,z,CX,CZ);if(r>R+BANK+0.5)continue;
			int h=cut(x,z)?0:bank(r),n=noise(x,z);
			fill(level,bb,x,0,z,x,F-4,z,Blocks.STONE.defaultBlockState());
			fill(level,bb,x,F-3,z,x,F-1+h,z,Blocks.DIRT.defaultBlockState());
			// The bank's outer slope keeps a little of the forest's own grass; the burn ends somewhere along it.
			BlockState top=r>R+BANK-1.5 && n<60?Blocks.GRASS_BLOCK.defaultBlockState():r>R && n<45?Blocks.COARSE_DIRT.defaultBlockState():floor(x,z);
			set(level,bb,top,x,F+h,z);
			fill(level,bb,x,F+1+h,z,x,HEIGHT-1,z,AIR);
			int at=F+1+h;
			if(r<=R){
				// Ash lies in drifts toward the bank and thins to bare ground where the fight will be.
				if(r>R-3.5 && n<40 || r>R-7 && n<12)set(level,bb,ASH,x,at,z);
				else if(r>R-5 && n>=97)set(level,bb,Blocks.DEAD_BUSH.defaultBlockState(),x,at,z);
			}else if(n>=93 && !cut(x,z))set(level,bb,Blocks.DEAD_BUSH.defaultBlockState(),x,at,z);
			else if(r>R+BANK-1.5 && n<60 && n%5==0)set(level,bb,Blocks.SHORT_GRASS.defaultBlockState(),x,at,z);
		}
	}

	/** The burnt path in from the woods, through the cut in the bank, past a fallen gate. */
	private void path(WorldGenLevel level,BoundingBox bb){
		for(int z=0;z<CZ-R+1;z++)for(int x=CX-3;x<=CX+3;x++){
			if(dist(x,z,CX,CZ)<=R+BANK+0.5)continue;
			fill(level,bb,x,F-3,z,x,F-1,z,Blocks.DIRT.defaultBlockState());
			set(level,bb,(noise(x,z)<55?Blocks.COARSE_DIRT:Blocks.GRAVEL).defaultBlockState(),x,F,z);
			fill(level,bb,x,F+1,z,x,F+6,z,AIR);
		}
		// The gate: two charred posts, the lintel fallen across the path's edge, a lantern dropped beside it.
		for(int x:new int[]{CX-3,CX+3})fill(level,bb,x,F+1,8,x,F+5,8,LOG);
		set(level,bb,Blocks.DARK_OAK_FENCE.defaultBlockState(),CX-3,F+6,8);
		var lying=LOG.setValue(RotatedPillarBlock.AXIS,Direction.Axis.X);
		fill(level,bb,CX+1,F+1,10,CX+3,F+1,10,lying);
		set(level,bb,Blocks.LANTERN.defaultBlockState(),CX+2,F+1,9);
	}

	/**
	 * One of the dead trees on the bank, {@code i} of fourteen round it (the gate's gap left clear): a blackened trunk, charred to
	 * stone in places, and a few bare limbs.
	 */
	private void deadTree(WorldGenLevel level,BoundingBox bb,int i){
		double a=-Math.PI/2+(i+0.5)*Math.PI*2/14;
		if(Math.abs(Math.IEEEremainder(a+Math.PI/2,Math.PI*2))<0.3)return;
		double r=R+2.5+(i%3)*0.9;
		int x=(int)Math.round(CX+Math.cos(a)*r),z=(int)Math.round(CZ+Math.sin(a)*r);
		int foot=F+1+bank(dist(x,z,CX,CZ)),height=7+noise(x,z)%6;
		for(int y=foot;y<foot+height;y++)set(level,bb,noise(x,y,z)<22?CHAR:WOOD,x,y,z);
		// Two or three limbs, reaching out over the bank and up; one broken off and lying at the foot.
		int limbs=2+i%2;
		for(int l=0;l<limbs;l++){
			double b=a+(l-0.5)*1.9+noise(i,l)*0.01;int y=foot+height-2-l*2;
			double lx=x,lz=z;
			for(int s=0;s<3+l%2;s++){
				lx+=Math.cos(b);lz+=Math.sin(b);if(s%2==1)y++;
				set(level,bb,s==2+l%2?Blocks.DARK_OAK_FENCE.defaultBlockState():WOOD,(int)Math.round(lx),y,(int)Math.round(lz));
			}
		}
		set(level,bb,Blocks.DARK_OAK_FENCE.defaultBlockState(),x,foot+height,z);
	}

	/** A charred trunk still standing in the glade, two blocks thick, snapped off at different heights. */
	private void trunk(WorldGenLevel level,BoundingBox bb,int x0,int z0){
		int tallest=0;
		for(int x=x0;x<=x0+1;x++)for(int z=z0;z<=z0+1;z++){
			int top=F+4+(noise(x,z)%4);tallest=Math.max(tallest,top);
			for(int y=F+1;y<=top;y++)set(level,bb,noise(x,y,z)<30?CHAR:WOOD,x,y,z);
			// A splinter of the snapped trunk still pointing up off its tallest corner.
			if(top==tallest && x==x0+1 && z==z0+1)set(level,bb,Blocks.DARK_OAK_FENCE.defaultBlockState(),x,top+1,z);
		}
	}

	/**
	 * The great burnt stump at the glade's heart: roots clawing out across the ash, its sides charred to stone, and in its hollow top
	 * the fire that never quite went out.
	 */
	private void stump(WorldGenLevel level,BoundingBox bb){
		for(int x=CX-4;x<=CX+4;x++)for(int z=CZ-4;z<=CZ+4;z++){
			double r=dist(x,z,CX,CZ);if(r>3.6)continue;
			int top=F+4+(r<2.6?0:noise(x,z)%3)-(r>3?1:0);
			for(int y=F+1;y<=top;y++)set(level,bb,r<2.2 && y>F+2?Blocks.MAGMA_BLOCK.defaultBlockState():noise(x,y,z)<35?CHAR:WOOD,x,y,z);
			if(r<2.2)set(level,bb,AIR,x,top,z);
			if(r<1.2)set(level,bb,Blocks.CAMPFIRE.defaultBlockState(),x,top,z);
		}
		// Roots, eight of them, sunk into the ground as they reach out.
		for(int i=0;i<8;i++){
			double a=i*Math.PI/4+0.3;
			for(double d=3.6;d<=7.5;d+=0.6){
				int x=(int)Math.round(CX+Math.cos(a)*d),z=(int)Math.round(CZ+Math.sin(a)*d);
				if(x==REST_X && z>=REST_Z-3 && z<=REST_Z+2)continue;
				set(level,bb,d<5.4?WOOD:LOG.setValue(RotatedPillarBlock.AXIS,Math.abs(Math.cos(a))>0.7?Direction.Axis.X:Direction.Axis.Z),x,d<5.4?F+1:F,z);
			}
		}
		// The stag's bed: a hollow in the ash before the stump, trampled bare.
		for(int x=REST_X-2;x<=REST_X+2;x++)for(int z=REST_Z-3;z<=REST_Z+2;z++){
			set(level,bb,Blocks.COARSE_DIRT.defaultBlockState(),x,F,z);set(level,bb,AIR,x,F+1,z);
		}
		// Against the stump's foot, between it and the bed: the Cinder Antler, shed and still smouldering.
		set(level,bb,dev.wildercord.aura.world.CinderAntler.BLOCK.defaultBlockState(),CX,F+1,CZ-4);
	}

	/** What is left of the last hunters: a cold fire and their supplies by the path, and further in, bones in the ash. */
	private void remains(WorldGenLevel level,BoundingBox bb){
		set(level,bb,Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT,false),CX-3,F+1,3);
		chest(level,bb,CX+3,F+1,4,DungeonWorldgen.ASHEN_GLADE,Direction.WEST,1301);
		set(level,bb,Blocks.BONE_BLOCK.defaultBlockState(),CX-3,F+1,5);
		for(int[] b:new int[][]{{17,17},{31,42},{9,30},{38,27}}){
			set(level,bb,Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS,(b[0]+b[1])%2==0?Direction.Axis.X:Direction.Axis.Z),b[0],F+1,b[1]);
			set(level,bb,Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION,(b[0]*5)%16),b[0]+1,F+1,b[1]);
		}
		// A snapped bow and a quiver's worth of arrows that never flew: a broken fence rail, and iron bars for the arrowheads.
		set(level,bb,Blocks.DARK_OAK_FENCE.defaultBlockState(),18,F+1,18);
		set(level,bb,Blocks.IRON_BARS.defaultBlockState(),32,F+1,41);
	}

	/** The glade and its bank, from below its floor to over the tallest dead tree. */
	@Override public List<BoundingBox> wardedBoxes(){return List.of(worldBox(0,0,0,WIDTH-1,HEIGHT-1,DEPTH-1));}
}
