package dev.wildercord.world.dungeons;

import dev.wildercord.config.Config;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import java.util.*;

/**
 * The Lantern-Bearer's barrow field: an old battlefield where the fallen were buried where they lay, laid out in its own coordinates
 * (x across, y up, z inward from the gate). The ground is levelled to one floor (you stand at {@code F+1}) and ringed by a broken
 * earthwork rampart, faced with fieldstone in places and bristling with sharpened stakes. The way in is a path through a ruined
 * lych-gate cut through the rampart. Inside lie rows of grave mounds, each with its headstone and a candle long gone out; rusted
 * spears stand where they were driven into the ground, tattered banners hang from their poles, and a trench along one side holds
 * the ones nobody buried properly. At the field's heart a tall gibbet hangs a lantern on a chain over an open grave: the Bearer's.
 *
 * <pre>
 *   z 0-6    the path, across the grass to the gate
 *   z 7-11   the lych-gate, a roofed gate through the rampart's cut, a chest under it
 *   z 12-44  the field, a circle of radius 16 round the gibbet's grave at (22, 28)
 *   rampart  radius 16-21, rising three blocks, its stakes along the crest
 * </pre>
 */
public final class BarrowFieldPiece extends DungeonPiece {
	public static final int WIDTH=45,HEIGHT=24,DEPTH=51;
	/** The field floor's local y: the ground block; you stand one above it. */
	public static final int F=6;
	public static final int CX=22,CZ=28;
	/** The field's radius, and how far past it the rampart runs. */
	public static final double R=16,BANK=5;
	/** The open grave the Bearer lies in, its floor one below the field's: the Bearer's feet rest on {@code F}. */
	public static final int REST_X=CX,REST_Y=F,REST_Z=CZ;
	/** The lantern, hanging on its chain from the gibbet's arm right over the grave. */
	public static final int LANTERN_X=CX,LANTERN_Y=F+5,LANTERN_Z=CZ;
	/** The gibbet's post, behind the grave from the gate. */
	public static final int POST_Z=CZ+3,ARM_Y=F+7;
	/** The rows of graves: the z of each row and the x of each grave along it (some are skipped, see {@link #grave}). */
	private static final int[] ROWS={CZ-10,CZ-5,CZ+6,CZ+11};
	/** Every headstone in the field, {x, z}, and the candles on them, {x, y, z}: the Bearer lights them when it rises. */
	public static final int[][] HEADSTONES,CANDLES;
	static{
		List<int[]> stones=new ArrayList<>(),candles=new ArrayList<>();
		for(int z:ROWS)for(int x=CX-12;x<=CX+12;x+=4){
			if(!grave(x,z) || fallen(x,z))continue;
			stones.add(new int[]{x,z+1});
			if(noise(x,z)<70)candles.add(new int[]{x,F+2,z+1});
		}
		HEADSTONES=stones.toArray(int[][]::new);CANDLES=candles.toArray(int[][]::new);
	}

	private static final BlockState DIRT=Blocks.DIRT.defaultBlockState();
	private static final BlockState POST=Blocks.DARK_OAK_LOG.defaultBlockState();
	private static final BlockState FENCE=Blocks.DARK_OAK_FENCE.defaultBlockState();
	private static final BlockState STAKE=Blocks.SPRUCE_FENCE.defaultBlockState();
	private static final BlockState SPEAR=Blocks.LIGHTNING_ROD.waxed().pick(WeatheringCopper.WeatherState.WEATHERED).defaultBlockState();
	private static final BlockState CANDLE=Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT,false);

	public BarrowFieldPiece(int x,int y,int z,Direction facing){super(DungeonWorldgen.BARROW_FIELD_PIECE,x,y,z,WIDTH,HEIGHT,DEPTH,facing);}
	public BarrowFieldPiece(CompoundTag tag){super(DungeonWorldgen.BARROW_FIELD_PIECE,tag);}

	/** Whether a grave lies at a spot in its row: none too near the gibbet, out by the rampart, in the lane up from the gate or in the trench. */
	private static boolean grave(int x,int z){
		double r=dist(x,z,CX,CZ);
		if(r<7 || r>R-2.5)return false;
		if(Math.abs(x-CX)<=1 && z<CZ)return false;
		return noise(x*3,z*7)>=12;
	}
	/** Whether a grave's headstone has fallen over: it lies as a slab on the mound, and carries no candle. */
	private static boolean fallen(int x,int z){return noise(z,x)>=86;}
	/** Whether a column is in the mass grave: a trench along the field's east side. */
	private static boolean trench(int x,int z){return x>=CX+7 && x<=CX+12 && z>=CZ-2 && z<=CZ+2;}

	/** Needs dry, fairly level open ground across the whole field: no lake, no ravine, no hill through the middle. */
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c){
		if(!Config.get().auraWorld().guardians())return Optional.empty();
		var piece=new BarrowFieldPiece(c.chunkPos().getMinBlockX(),0,c.chunkPos().getMinBlockZ(),Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
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
		lychGate(level,bb);
		stakes(level,bb);
		for(int z:ROWS)for(int x=CX-12;x<=CX+12;x+=4)if(grave(x,z))grave(level,bb,x,z);
		trench(level,bb);
		spears(level,bb);
		banners(level,bb);
		gibbet(level,bb);
	}

	/** How high the rampart stands over a column {@code r} from the grave: nothing in the field, rising three blocks to its crest. */
	private static int bank(double r){
		if(r<=R)return 0;
		double t=r-R;
		return (int)Math.round(t<2.5?t*1.2:Math.max(0,3-(t-2.5)*1.2));
	}
	/** Whether a column is in the cut the path makes through the rampart. */
	private static boolean cut(int x,int z){return Math.abs(x-CX)<=2 && z<CZ;}
	/** Where the rampart has slumped: a few gaps worn down to half its height, as if the field was overrun there. */
	private static boolean slumped(int x,int z){
		double a=Math.atan2(z-CZ,x-CX);
		return Math.abs(Math.IEEEremainder(a-0.9,Math.PI*2))<0.22 || Math.abs(Math.IEEEremainder(a+2.6,Math.PI*2))<0.18;
	}

	private BlockState floor(int x,int z){
		int n=noise(x,z);
		return (n<62?Blocks.GRASS_BLOCK:n<74?Blocks.COARSE_DIRT:n<84?Blocks.PODZOL:n<92?Blocks.ROOTED_DIRT:Blocks.MUD).defaultBlockState();
	}

	/** The levelled field and its rampart: earth packed under it all, the air cleared over it, the grass grown back thin and patchy. */
	private void ground(WorldGenLevel level,BoundingBox bb){
		for(int x=0;x<WIDTH;x++)for(int z=0;z<DEPTH;z++){
			double r=dist(x,z,CX,CZ);if(r>R+BANK+0.5)continue;
			int h=cut(x,z)?0:bank(r),n=noise(x,z);
			if(h>1 && slumped(x,z))h=1;
			fill(level,bb,x,0,z,x,F-3,z,Blocks.STONE.defaultBlockState());
			fill(level,bb,x,F-2,z,x,F-1+h,z,DIRT);
			// The rampart was faced with fieldstone on its inner side; most of it has slid, some still shows.
			if(r>R && r<R+1.6 && h>0 && n<45)for(int y=F;y<F+h;y++)set(level,bb,(noise(x,y,z)<55?Blocks.MOSSY_COBBLESTONE:Blocks.COBBLESTONE).defaultBlockState(),x,y,z);
			BlockState top=r>R?(n<70?Blocks.GRASS_BLOCK:n<85?Blocks.COARSE_DIRT:Blocks.MOSSY_COBBLESTONE).defaultBlockState():floor(x,z);
			set(level,bb,top,x,F+h,z);
			fill(level,bb,x,F+1+h,z,x,HEIGHT-1,z,AIR);
			int at=F+1+h;
			if(r<=R){
				if(n>=96)set(level,bb,Blocks.DEAD_BUSH.defaultBlockState(),x,at,z);
				else if(n<62 && n%7==0)set(level,bb,Blocks.SHORT_GRASS.defaultBlockState(),x,at,z);
			}else if(n<70 && n%4==0 && !cut(x,z))set(level,bb,(n%3==0?Blocks.TALL_GRASS:Blocks.SHORT_GRASS).defaultBlockState(),x,at,z);
			else if(n>=94 && !cut(x,z))set(level,bb,Blocks.DEAD_BUSH.defaultBlockState(),x,at,z);
		}
	}

	/** The path in across the grass, up to the lych-gate and through the rampart's cut. */
	private void path(WorldGenLevel level,BoundingBox bb){
		for(int z=0;z<CZ-R+1;z++)for(int x=CX-2;x<=CX+2;x++){
			if(dist(x,z,CX,CZ)<=R+BANK+0.5 && !cut(x,z))continue;
			fill(level,bb,x,F-2,z,x,F-1,z,DIRT);
			int n=noise(x,z);
			set(level,bb,(Math.abs(x-CX)==2 && n<50?Blocks.GRASS_BLOCK:n<45?Blocks.COARSE_DIRT:n<75?Blocks.GRAVEL:Blocks.DIRT_PATH).defaultBlockState(),x,F,z);
			fill(level,bb,x,F+1,z,x,F+6,z,AIR);
		}
	}

	/**
	 * The lych-gate across the cut: four dark posts and a gabled roof over the path, half its shingles gone, a bench along one side
	 * and the chest of whoever last kept the field under it.
	 */
	private void lychGate(WorldGenLevel level,BoundingBox bb){
		int z0=7,z1=10;
		for(int x:new int[]{CX-3,CX+3})for(int z:new int[]{z0,z1})fill(level,bb,x,F+1,z,x,F+3,z,POST);
		var beamX=POST.setValue(RotatedPillarBlock.AXIS,Direction.Axis.X);
		var beamZ=POST.setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z);
		for(int x:new int[]{CX-3,CX+3})fill(level,bb,x,F+4,z0,x,F+4,z1,beamZ);
		for(int z:new int[]{z0,z1})fill(level,bb,CX-3,F+4,z,CX+3,F+4,z,beamX);
		// The roof: stairs rising from both eaves to a slab ridge, many of them fallen in.
		for(int z=z0-1;z<=z1+1;z++)for(int i=0;i<=3;i++){
			for(int side:new int[]{-1,1}){
				int x=CX+side*(4-i);
				if(i==3 && side==1)continue;
				if(noise(x,z+i)<30)continue;
				BlockState s=i==3?Blocks.DARK_OAK_SLAB.defaultBlockState():
					Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,side<0?Direction.EAST:Direction.WEST);
				set(level,bb,s,x,F+5+i,z);
			}
		}
		// The fallen shingles, lying in the path's edge; the bench; the warden's chest; a cold lantern on a hook.
		set(level,bb,Blocks.DARK_OAK_SLAB.defaultBlockState(),CX+2,F+1,z1+2);
		for(int z=z0+1;z<=z1-1;z++)set(level,bb,Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.WEST),CX-2,F+1,z);
		chest(level,bb,CX+2,F+1,z0+1,DungeonWorldgen.BARROW_FIELD,Direction.WEST,1601);
		set(level,bb,Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true),CX+2,F+3,z1);
		set(level,bb,Blocks.COBWEB.defaultBlockState(),CX-2,F+3,z0);
		set(level,bb,Blocks.COBWEB.defaultBlockState(),CX+3,F+5,z1+1);
	}

	/** The sharpened stakes along the rampart's crest, leaning outward against a charge that came long ago; many are gone. */
	private void stakes(WorldGenLevel level,BoundingBox bb){
		for(int i=0;i<44;i++){
			double a=i*Math.PI*2/44;
			int x=(int)Math.round(CX+Math.cos(a)*(R+2.6)),z=(int)Math.round(CZ+Math.sin(a)*(R+2.6));
			if(cut(x,z) || Math.abs(x-CX)<=3 && z<CZ || slumped(x,z) || noise(x,z,i)<35)continue;
			int foot=F+1+bank(dist(x,z,CX,CZ));
			int tall=1+noise(z,x)%2;
			for(int y=foot;y<foot+tall;y++)set(level,bb,STAKE,x,y,z);
			// The points, outward: a stake's tip is the next block out along the slope, a step down.
			int ox=(int)Math.round(CX+Math.cos(a)*(R+3.6)),oz=(int)Math.round(CZ+Math.sin(a)*(R+3.6));
			if(tall==2 && (ox!=x || oz!=z))set(level,bb,STAKE,ox,foot+1,oz);
		}
	}

	/** A grave in its row: a low mound of turned earth, a headstone at its head, sometimes a candle on it, now and then a skull. */
	private void grave(WorldGenLevel level,BoundingBox bb,int x,int z){
		int n=noise(x,z);
		for(int dz=-1;dz<=0;dz++){
			set(level,bb,(noise(x,z+dz)<55?Blocks.ROOTED_DIRT:Blocks.PODZOL).defaultBlockState(),x,F,z+dz);
			set(level,bb,AIR,x,F+1,z+dz);
		}
		// The headstone: an old wall post, mossy or plain; a few have fallen over and lie as a slab on the mound.
		BlockState stone=(n<35?Blocks.MOSSY_COBBLESTONE_WALL:n<65?Blocks.COBBLESTONE_WALL:n<85?Blocks.STONE_BRICK_WALL:Blocks.MOSSY_STONE_BRICK_WALL).defaultBlockState();
		if(fallen(x,z)){
			set(level,bb,Blocks.COBBLESTONE_SLAB.defaultBlockState(),x,F+1,z);
		}else{
			set(level,bb,stone,x,F+1,z+1);
			if(noise(x,z)<70)set(level,bb,CANDLE.setValue(CandleBlock.CANDLES,1+n%3),x,F+2,z+1);
		}
		if(n%9==4)set(level,bb,Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION,(x*7+z)%16),x,F+1,z-1);
		else if(n%9==7)set(level,bb,Blocks.COBWEB.defaultBlockState(),x,F+1,z-1);
	}

	/** The mass grave: a trench along the east side, half filled in with mud, bones showing through and the bushes dead round it. */
	private void trench(WorldGenLevel level,BoundingBox bb){
		for(int x=CX+6;x<=CX+13;x++)for(int z=CZ-3;z<=CZ+3;z++){
			int n=noise(x,z);
			if(trench(x,z)){
				set(level,bb,n<30?Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS,n%2==0?Direction.Axis.X:Direction.Axis.Z):Blocks.MUD.defaultBlockState(),x,F-1,z);
				set(level,bb,AIR,x,F,z);set(level,bb,AIR,x,F+1,z);
				if(n>=88)set(level,bb,Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION,n%16),x,F,z);
				else if(n>=80)set(level,bb,Blocks.COBWEB.defaultBlockState(),x,F,z);
			}else{
				// The spoil heaped along its lip, never put back.
				set(level,bb,(n<50?Blocks.COARSE_DIRT:Blocks.MUD).defaultBlockState(),x,F,z);
				if(n>=70)set(level,bb,Blocks.DEAD_BUSH.defaultBlockState(),x,F+1,z);
			}
		}
		// The shovel left in the spoil: a fence handle with a slab blade.
		set(level,bb,FENCE,CX+6,F+1,CZ+3);
		set(level,bb,Blocks.IRON_TRAPDOOR.defaultBlockState(),CX+6,F+2,CZ+3);
	}

	/** Rusted spears where they were driven in or fell: standing in the grass in clusters across the west of the field, a few lying flat. */
	private void spears(WorldGenLevel level,BoundingBox bb){
		int[][] at={{CX-11,CZ-1},{CX-10,CZ+1},{CX-12,CZ+2},{CX-8,CZ},{CX-9,CZ-2},{CX-4,CZ-13},{CX+5,CZ+14},{CX+11,CZ-8},{CX-13,CZ+8},{CX+2,CZ-14}};
		for(int[] s:at){
			int n=noise(s[0],s[1]);
			if(n<20)continue;
			BlockState spear=n<75?SPEAR:SPEAR.setValue(BlockStateProperties.FACING,n%2==0?Direction.EAST:Direction.SOUTH);
			set(level,bb,spear,s[0],F+1,s[1]);
		}
		// A broken shield where the spears stand thickest, and the skull of whoever held it.
		set(level,bb,Blocks.SPRUCE_TRAPDOOR.defaultBlockState(),CX-10,F+1,CZ);
		set(level,bb,Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION,5),CX-11,F+1,CZ);
	}

	/** Tattered banners on their poles round the field, grey and black, the colours of whoever fought here long since faded. */
	private void banners(WorldGenLevel level,BoundingBox bb){
		int[][] at={{CX-14,CZ-6},{CX+14,CZ-6},{CX-13,CZ+5},{CX+13,CZ+6},{CX-6,CZ+13},{CX+7,CZ+13}};
		for(int i=0;i<at.length;i++){
			int x=at[i][0],z=at[i][1];
			if(noise(x,z)<15)continue;
			fill(level,bb,x,F+1,z,x,F+2,z,FENCE);
			DyeColor colour=i%3==0?DyeColor.BLACK:i%3==1?DyeColor.GRAY:DyeColor.LIGHT_GRAY;
			set(level,bb,Blocks.BANNER.pick(colour).defaultBlockState().setValue(BannerBlock.ROTATION,(i*5+3)%16),x,F+3,z);
			if(noise(z,x)<30)set(level,bb,Blocks.COBWEB.defaultBlockState(),x,F+1,z+1);
		}
	}

	/**
	 * The gibbet at the field's heart: a tall dark post behind the open grave, its arm reaching out over it, braced, and from the
	 * arm's end a chain and the Bearer's lantern. The grave under it is open and empty-looking, mud at its bottom.
	 */
	private void gibbet(WorldGenLevel level,BoundingBox bb){
		// The grave, three wide and three long, a block deep; trampled bare earth round it.
		for(int x=CX-2;x<=CX+2;x++)for(int z=CZ-2;z<=CZ+2;z++){
			set(level,bb,Blocks.COARSE_DIRT.defaultBlockState(),x,F,z);set(level,bb,AIR,x,F+1,z);
		}
		for(int x=CX-1;x<=CX+1;x++)for(int z=CZ-1;z<=CZ+1;z++){
			set(level,bb,Blocks.MUD.defaultBlockState(),x,F-1,z);set(level,bb,AIR,x,F,z);
		}
		// The heaped earth from it, at its foot, and the post's footing stones.
		set(level,bb,Blocks.ROOTED_DIRT.defaultBlockState(),CX-2,F+1,CZ-2);
		set(level,bb,Blocks.COARSE_DIRT.defaultBlockState(),CX+2,F+1,CZ-2);
		for(int[] d:new int[][]{{-1,0},{1,0},{0,1}})set(level,bb,Blocks.MOSSY_COBBLESTONE.defaultBlockState(),CX+d[0],F+1,POST_Z+d[1]);
		fill(level,bb,CX,F+1,POST_Z,CX,ARM_Y,POST_Z,POST);
		set(level,bb,FENCE,CX,ARM_Y+1,POST_Z);
		var arm=POST.setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z);
		fill(level,bb,CX,ARM_Y,LANTERN_Z,CX,ARM_Y,POST_Z-1,arm);
		// The brace, from the post up under the arm.
		set(level,bb,Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.NORTH).setValue(StairBlock.HALF,net.minecraft.world.level.block.state.properties.Half.TOP),CX,ARM_Y-1,POST_Z-1);
		set(level,bb,Blocks.IRON_CHAIN.defaultBlockState(),LANTERN_X,ARM_Y-1,LANTERN_Z);
		set(level,bb,lantern(),LANTERN_X,LANTERN_Y,LANTERN_Z);
		set(level,bb,Blocks.COBWEB.defaultBlockState(),CX+1,ARM_Y,POST_Z);
	}

	/** The Bearer's lantern as it hangs from the gibbet: a soul lantern, its pale fire never out. */
	public static BlockState lantern(){return Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true);}

	/** The field and its rampart, from below its floor to over the gibbet. */
	@Override public List<BoundingBox> wardedBoxes(){return List.of(worldBox(0,0,0,WIDTH-1,HEIGHT-1,DEPTH-1));}
}
