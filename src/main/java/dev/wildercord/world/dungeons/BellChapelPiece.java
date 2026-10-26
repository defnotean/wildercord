package dev.wildercord.world.dungeons;

import dev.wildercord.config.Config;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import java.util.*;

/**
 * The Drowned Bell-Warden's chapel, sunk whole to the floor of the deep ocean, laid out in its own coordinates (x across, y up,
 * z inward from the door). The sea floor at the entrance is at y 2; the chapel stands on foundations over it, dry inside
 * (floor at y 7, you stand at 8), by the same trick as the Scriptorium: the way in is from below, and water never rises
 * through a hole in a floor.
 *
 * <pre>
 *   z 0-4    the approach: a lantern-lit path over the sea floor
 *   z 5-14   the flooded tunnel under the narthex, up through the moon pool
 *   z 9-18   the narthex, dry, a chest by the door; the belfry on its roof, its bell still hung
 *   z 18-54  the nave: a pointed vault on two rows of pillars, blue glass windows, pews, pools on the floor, hanging lanterns
 *   z 54-66  the apse: a dais and an altar under a rose window; the broken bell frame stands where nave meets apse
 * </pre>
 */
public final class BellChapelPiece extends DungeonPiece {
	public static final int WIDTH=31,HEIGHT=24,DEPTH=70;
	private static final int SEABED=2;
	/** The dry floor's local y. */
	public static final int F=7;
	public static final int CX=15;
	/** Where the apse's half circle is centred, and its radius. */
	private static final int APSE_Z=54;
	private static final double APSE_R=11,APSE_WALL=12.6;
	/** Where the Warden keeps its vigil: under the broken bell frame. */
	public static final int REST_X=CX,REST_Z=52;
	/** The pillars' rows, and the slices of nave between them. */
	private static final int[] PILLAR_Z={22,29,36,43,50};

	private static final BlockState DARK=Blocks.DARK_PRISMARINE.defaultBlockState();
	private static final BlockState BRICKS=Blocks.PRISMARINE_BRICKS.defaultBlockState();
	private static final BlockState LIGHT=Blocks.SEA_LANTERN.defaultBlockState();
	private static final BlockState WATER=Blocks.WATER.defaultBlockState();
	private static final BlockState PILLAR=Blocks.DEEPSLATE_BRICKS.defaultBlockState();
	private static final BlockState GLASS=Blocks.STAINED_GLASS.blue().defaultBlockState();

	public BellChapelPiece(int x,int y,int z,Direction facing){super(DungeonWorldgen.BELL_CHAPEL_PIECE,x,y,z,WIDTH,HEIGHT,DEPTH,facing);}
	public BellChapelPiece(CompoundTag tag){super(DungeonWorldgen.BELL_CHAPEL_PIECE,tag);}

	/** Finds its footing on the sea floor at the door, where the sea is deep enough to cover the belfry, and the apse's end too. */
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c){
		if(!Config.get().auraWorld().guardians())return Optional.empty();
		var piece=new BellChapelPiece(c.chunkPos().getMinBlockX(),0,c.chunkPos().getMinBlockZ(),Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		var gen=c.chunkGenerator();int sea=gen.getSeaLevel();
		var door=piece.getWorldPos(CX,0,2);var back=piece.getWorldPos(CX,0,60);
		int ground=gen.getFirstOccupiedHeight(door.getX(),door.getZ(),Heightmap.Types.OCEAN_FLOOR_WG,c.heightAccessor(),c.randomState());
		int far=gen.getFirstOccupiedHeight(back.getX(),back.getZ(),Heightmap.Types.OCEAN_FLOOR_WG,c.heightAccessor(),c.randomState());
		if(sea-ground<HEIGHT || sea-far<HEIGHT-4)return Optional.empty();
		piece.move(0,ground-1-SEABED,0);
		return Optional.of(new Structure.GenerationStub(new BlockPos(door.getX(),ground,door.getZ()),b->b.addPiece(piece)));
	}

	@Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox bb,ChunkPos chunk,BlockPos ref){
		DungeonWards.remember(level,this);
		approach(level,bb);
		narthex(level,bb);
		tunnel(level,bb);
		nave(level,bb);
		apse(level,bb);
		frame(level,bb);
		belfry(level,bb);
		warden(level,bb);
	}

	/** The chapel, from the tunnel's mouth to the back of the apse, floor to belfry. */
	@Override public List<BoundingBox> wardedBoxes(){return List.of(worldBox(2,0,5,WIDTH-3,HEIGHT-1,67));}

	/** Old stone, weathered: bricks, mossy and cracked. */
	private BlockState stone(int x,int y,int z){
		int n=noise(x,y,z);
		return (n<22?Blocks.MOSSY_STONE_BRICKS:n<34?Blocks.CRACKED_STONE_BRICKS:Blocks.STONE_BRICKS).defaultBlockState();
	}

	/** The vault's ceiling (its highest air) over a column {@code r} blocks from the nave's centre line or the apse's centre. */
	static int ceiling(double r){return F+8+(int)Math.round(5*Math.max(0,1-r/12));}

	/** Fills a column under the floor down to the sea floor, so the chapel stands on something. */
	private void foundation(WorldGenLevel level,BoundingBox bb,int x,int z){
		for(int y=F-1;y>=0;y--){
			var there=getBlock(level,x,y,z,bb);
			if(!there.isAir() && there.getFluidState().isEmpty())break;
			set(level,bb,y==F-1?DARK:Blocks.PRISMARINE.defaultBlockState(),x,y,z);
		}
	}

	/** The highest solid block of the sea floor in a column, looking down from {@code from}. */
	private int seabed(WorldGenLevel level,BoundingBox bb,int x,int z,int from){
		for(int y=from;y>0;y--){var there=getBlock(level,x,y,z,bb);if(!there.isAir() && there.getFluidState().isEmpty())return y;}
		return SEABED;
	}

	// ------------------------------------------------------------------ the approach and the tunnel

	private void approach(WorldGenLevel level,BoundingBox bb){
		for(int z=0;z<=4;z++){
			for(int x=12;x<=18;x++){
				int floor=seabed(level,bb,x,z,6);
				set(level,bb,x==CX?DARK:BRICKS,x,floor,z);
			}
			if(z%2==1)for(int x:new int[]{11,19}){
				int floor=seabed(level,bb,x,z,6);
				for(int y=floor+1;y<=floor+3;y++)set(level,bb,stone(x,y,z),x,y,z);
				set(level,bb,LIGHT,x,floor+4,z);
			}
			fill(level,bb,13,3,z,17,5,z,WATER);
		}
		// A dark portal over the tunnel's mouth: two jambs and a lintel, a sea lantern at its keystone.
		for(int x:new int[]{12,18}){
			int floor=seabed(level,bb,x,4,6);
			fill(level,bb,x,floor+1,4,x,7,4,DARK);
		}
		fill(level,bb,13,6,4,17,7,4,DARK);
		set(level,bb,LIGHT,CX,7,4);
		set(level,bb,stair(Blocks.DARK_PRISMARINE_STAIRS,Direction.WEST,true),13,5,4);
		set(level,bb,stair(Blocks.DARK_PRISMARINE_STAIRS,Direction.EAST,true),17,5,4);
	}

	/** Under the narthex: a flooded tunnel, rising through the moon pool. */
	private void tunnel(WorldGenLevel level,BoundingBox bb){
		fill(level,bb,11,2,5,19,F-1,15,BRICKS);
		fill(level,bb,12,2,5,18,2,14,DARK);
		fill(level,bb,12,3,5,18,5,14,WATER);
		for(int z:new int[]{7,10,13}){set(level,bb,LIGHT,11,4,z);set(level,bb,LIGHT,19,4,z);}
		// The moon pool: up through the tunnel's roof and the narthex's floor. The water stops at the floor.
		fill(level,bb,14,F-1,12,16,F,14,WATER);
	}

	// ------------------------------------------------------------------ the narthex and its belfry

	private void narthex(WorldGenLevel level,BoundingBox bb){
		for(int x=9;x<=21;x++)for(int z=9;z<=18;z++)foundation(level,bb,x,z);
		for(int x=9;x<=21;x++)for(int z=9;z<=18;z++)for(int y=F;y<=F+7;y++){
			boolean edge=x==9 || x==21 || z==9 || z==18 || y==F || y==F+7;
			set(level,bb,edge?(y==F?Blocks.DEEPSLATE_TILES.defaultBlockState():stone(x,y,z)):AIR,x,y,z);
		}
		// Sea lanterns ring the moon pool; the front wall's glass looks out on the sea.
		fill(level,bb,13,F,11,17,F,15,LIGHT);
		fill(level,bb,13,F+3,9,13,F+5,9,GLASS);fill(level,bb,17,F+3,9,17,F+5,9,GLASS);
		for(int z:new int[]{12,16}){set(level,bb,LIGHT,9,F+3,z);set(level,bb,LIGHT,21,F+3,z);}
		chest(level,bb,20,F+1,17,DungeonWorldgen.BELL_CHAPEL,Direction.WEST,31);
		set(level,bb,Blocks.BARREL.defaultBlockState(),20,F+1,16);
		set(level,bb,Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES,3).setValue(CandleBlock.LIT,true),10,F+1,17);
		// A corbelled cornice round the room, dark at the corners, a lantern over the pool and the Warden's blue by the nave door.
		for(int x=11;x<=19;x++){
			set(level,bb,stair(Blocks.STONE_BRICK_STAIRS,Direction.SOUTH,true),x,F+6,10);
			set(level,bb,stair(Blocks.STONE_BRICK_STAIRS,Direction.NORTH,true),x,F+6,17);
		}
		for(int z=11;z<=16;z++){
			set(level,bb,stair(Blocks.STONE_BRICK_STAIRS,Direction.WEST,true),10,F+6,z);
			set(level,bb,stair(Blocks.STONE_BRICK_STAIRS,Direction.EAST,true),20,F+6,z);
		}
		for(int x:new int[]{10,20})for(int z:new int[]{10,17})set(level,bb,DARK,x,F+6,z);
		hangingLantern(level,bb,CX,F+6,13,1,false);
		for(int x:new int[]{11,19})banner(level,bb,Blocks.WALL_BANNER.pick(DyeColor.BLUE),x,F+4,17,Direction.SOUTH);
		// Outside: buttresses either side of the front, a parapet round the roof and a stepped gable over the front wall.
		for(int x:new int[]{10,20}){
			for(int z=6;z<=8;z++)foundation(level,bb,x,z);
			buttress(level,bb,x,8,F,8,Direction.SOUTH,PILLAR,Blocks.DEEPSLATE_BRICK_STAIRS);
		}
		crenellate(level,bb,9,9,21,18,F+8,PILLAR);
		for(int k=0;k<3;k++){
			int lo=10+2*k,hi=20-2*k;
			for(int x=lo;x<=hi;x++)set(level,bb,k==2?DARK:stone(x,F+8+k,9),x,F+8+k,9);
			set(level,bb,stair(Blocks.DEEPSLATE_BRICK_STAIRS,Direction.EAST,false),lo-1,F+8+k,9);
			set(level,bb,stair(Blocks.DEEPSLATE_BRICK_STAIRS,Direction.WEST,false),hi+1,F+8+k,9);
		}
		set(level,bb,LIGHT,CX,F+9,9);
		set(level,bb,Blocks.CHISELED_DEEPSLATE.defaultBlockState(),CX,F+11,9);
	}

	/** The bell tower on the narthex's roof: four posts, a cap, and its bell, still hung, out in the sea. */
	/** The guardian, kneeling before the bell-frame and facing down the nave toward the door. Spawned once, by whichever chunk holds its knees. */
	private void warden(WorldGenLevel level,BoundingBox bb){
		var foot=getWorldPos(REST_X,F+1,REST_Z);if(!bb.isInside(foot))return;
		var door=getWorldPos(REST_X,F+1,REST_Z-10);
		float rest=(float)Math.toDegrees(Math.atan2(-(door.getX()-foot.getX()),door.getZ()-foot.getZ()));
		var warden=dev.wildercord.aura.world.Guardians.BELL_WARDEN.create(level.getLevel(),net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);if(warden==null)return;
		warden.snapTo(foot.getX()+.5,foot.getY(),foot.getZ()+.5,rest,0);
		warden.finalizeSpawn(level,level.getCurrentDifficultyAt(foot),net.minecraft.world.entity.EntitySpawnReason.STRUCTURE,null);
		warden.bind(foot,rest,worldBox(4,F,19,26,F+14,65));
		level.addFreshEntityWithPassengers(warden);
	}

	private void belfry(WorldGenLevel level,BoundingBox bb){
		for(int x:new int[]{13,17})for(int z:new int[]{11,15})fill(level,bb,x,F+8,z,x,F+14,z,PILLAR);
		fill(level,bb,13,F+8,11,17,F+8,15,DARK);
		fill(level,bb,13,F+15,11,17,F+15,15,DARK);
		fill(level,bb,14,F+16,12,16,F+16,14,Blocks.PRISMARINE.defaultBlockState());
		fill(level,bb,13,F+14,13,17,F+14,13,Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X));
		set(level,bb,Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT,BellAttachType.CEILING).setValue(BellBlock.FACING,Direction.NORTH),CX,F+13,13);
	}

	// ------------------------------------------------------------------ the nave

	private void nave(WorldGenLevel level,BoundingBox bb){
		for(int x=3;x<=27;x++)for(int z=18;z<=APSE_Z;z++){
			foundation(level,bb,x,z);
			int top=ceiling(Math.abs(x-CX));
			if(x==3 || x==27 || z==18){
				for(int y=F;y<=top+2;y++)set(level,bb,y==F?DARK:stone(x,y,z),x,y,z);
				continue;
			}
			set(level,bb,floor(x,z),x,F,z);
			fill(level,bb,x,F+1,z,x,top,z,AIR);
			set(level,bb,DARK,x,top+1,z);set(level,bb,noise(x,z)<30?Blocks.PRISMARINE.defaultBlockState():DARK,x,top+2,z);
		}
		// The door from the narthex.
		fill(level,bb,13,F+1,18,17,F+4,18,AIR);
		fill(level,bb,12,F+5,18,18,F+5,18,DARK);
		// Dark jambs and a lit keystone frame the door; over it a small rose window looks back at the belfry.
		fill(level,bb,12,F+1,18,12,F+4,18,DARK);
		fill(level,bb,18,F+1,18,18,F+4,18,DARK);
		set(level,bb,LIGHT,CX,F+6,18);
		for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)set(level,bb,dx==0 && dy==0?LIGHT:GLASS,CX+dx,F+10+dy,18);
		for(int x:new int[]{11,19})banner(level,bb,Blocks.WALL_BANNER.pick(DyeColor.BLUE),x,F+5,19,Direction.NORTH);
		// Ribs across the vault over each pair of pillars, and the pillars, a lantern set in each, on stepped plinths
		// under corbelled capitals. Dark shafts on the walls carry the ribs' ends.
		for(int z:PILLAR_Z){
			for(int x=4;x<=26;x++)set(level,bb,DARK,x,ceiling(Math.abs(x-CX)),z);
			for(int x:new int[]{8,22}){
				int top=ceiling(Math.abs(x-CX));
				fill(level,bb,x,F+1,z,x,top,z,PILLAR);
				set(level,bb,Blocks.CHISELED_DEEPSLATE.defaultBlockState(),x,F+1,z);
				set(level,bb,LIGHT,x,F+6,z);
				for(int y:new int[]{F+1,F+7}){
					boolean cap=y==F+7;
					set(level,bb,stair(Blocks.DEEPSLATE_BRICK_STAIRS,Direction.EAST,cap),x-1,y,z);
					set(level,bb,stair(Blocks.DEEPSLATE_BRICK_STAIRS,Direction.WEST,cap),x+1,y,z);
					set(level,bb,stair(Blocks.DEEPSLATE_BRICK_STAIRS,Direction.NORTH,cap),x,y,z-1);
					set(level,bb,stair(Blocks.DEEPSLATE_BRICK_STAIRS,Direction.SOUTH,cap),x,y,z+1);
				}
			}
			for(int x:new int[]{4,26}){
				fill(level,bb,x,F+1,z,x,F+7,z,DARK);
				set(level,bb,LIGHT,x,F+4,z);
			}
			// Outside, a stepped buttress against each wall opposite the shafts.
			for(int x=0;x<=2;x++){foundation(level,bb,x,z);foundation(level,bb,30-x,z);}
			buttress(level,bb,2,z,F,10,Direction.WEST,PILLAR,Blocks.DEEPSLATE_BRICK_STAIRS);
			buttress(level,bb,28,z,F,10,Direction.EAST,PILLAR,Blocks.DEEPSLATE_BRICK_STAIRS);
			for(int x:new int[]{2,28})pinnacle(level,bb,x,F+10,z);
		}
		// A dark string course inside the long walls under the windows, an eave course along their tops outside, and a
		// ridge of dark prismarine down the roof, lit over each rib.
		for(int z=19;z<APSE_Z;z++){
			set(level,bb,DARK,3,F+2,z);set(level,bb,DARK,27,F+2,z);
			set(level,bb,stair(Blocks.STONE_BRICK_STAIRS,Direction.EAST,true),2,F+10,z);
			set(level,bb,stair(Blocks.STONE_BRICK_STAIRS,Direction.WEST,true),28,F+10,z);
			set(level,bb,DARK,CX,ceiling(0)+3,z);
		}
		for(int z:PILLAR_Z)set(level,bb,LIGHT,CX,ceiling(0)+3,z);
		// Tall windows of blue glass in each bay.
		for(int i=0;i<PILLAR_Z.length-1;i++){
			int z=PILLAR_Z[i]+3;
			for(int x:new int[]{3,27}){fill(level,bb,x,F+3,z,x,F+6,z+1,GLASS);set(level,bb,GLASS,x,F+7,z);}
		}
		pews(level,bb);
		pools(level,bb);
		// Lanterns down the middle and the aisles.
		for(int z=26;z<=47;z+=7){
			hang(level,bb,CX,z,ceiling(0),F+9);
			hang(level,bb,5,z,ceiling(10),F+8);
			hang(level,bb,25,z,ceiling(10),F+8);
		}
	}

	/** A runner of prismarine down the middle; elsewhere old tiles, some of them cracked. */
	private BlockState floor(int x,int z){
		if(Math.abs(x-CX)<=1)return BRICKS;
		int n=noise(x,z);
		return (n<18?Blocks.CRACKED_DEEPSLATE_TILES:n<30?Blocks.POLISHED_DEEPSLATE:Blocks.DEEPSLATE_TILES).defaultBlockState();
	}

	/** A lantern hung on a chain from the vault at {@code top} down to {@code low}. */
	private void hang(WorldGenLevel level,BoundingBox bb,int x,int z,int top,int low){
		fill(level,bb,x,low+1,z,x,top,z,Blocks.IRON_CHAIN.defaultBlockState());
		set(level,bb,Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true),x,low,z);
	}

	/** Pews in the front half of the nave, facing the apse, either side of the runner; some long since floated off. */
	private void pews(WorldGenLevel level,BoundingBox bb){
		var seat=Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.NORTH);
		for(int z=23;z<=39;z+=2)for(int x=9;x<=21;x++){
			if(Math.abs(x-CX)<=2 || noise(x,z+7)<14)continue;
			set(level,bb,seat,x,F+1,z);
		}
	}

	/** Shallow pools in the back half of the nave, where the floor has sunk: water flush with it. */
	private void pools(WorldGenLevel level,BoundingBox bb){
		for(int[] p:new int[][]{{11,44},{12,44},{11,45},{19,46},{20,46},{19,47},{20,47},{13,49},{14,49},{17,41},{10,40}})set(level,bb,WATER,p[0],F,p[1]);
	}

	// ------------------------------------------------------------------ the apse

	private void apse(WorldGenLevel level,BoundingBox bb){
		for(int x=2;x<=28;x++)for(int z=APSE_Z+1;z<=67;z++){
			double r=dist(x,z,CX,APSE_Z);
			if(r>APSE_WALL)continue;
			foundation(level,bb,x,z);
			int top=ceiling(r);
			if(r>APSE_R){
				for(int y=F;y<=F+10;y++)set(level,bb,y==F || y==F+10?DARK:rose(x,y,z),x,y,z);
				continue;
			}
			boolean dais=z>=58;
			set(level,bb,floor(x,z),x,F,z);
			if(dais)set(level,bb,Blocks.POLISHED_DEEPSLATE.defaultBlockState(),x,F+1,z);
			fill(level,bb,x,dais?F+2:F+1,z,x,top,z,AIR);
			// Dark ribs fan out over the half dome from where it meets the nave.
			if(r>2 && onSpoke(x,z,CX,APSE_Z,12,0.55))set(level,bb,DARK,x,top,z);
			set(level,bb,DARK,x,top+1,z);set(level,bb,noise(x,z)<30?Blocks.PRISMARINE.defaultBlockState():DARK,x,top+2,z);
		}
		// A step up onto the dais across the apse's width.
		for(int x=4;x<=26;x++){
			if(dist(x,57,CX,APSE_Z)<=APSE_R && dist(x,58,CX,APSE_Z)<=APSE_R)set(level,bb,stair(Blocks.POLISHED_DEEPSLATE_STAIRS,Direction.NORTH,false),x,F+1,57);
		}
		// Four columns stand on the ribs round the dais, each with a lantern set in it, and two soul lanterns hang over the
		// way up to the altar.
		for(int k:new int[]{1,2,4,5}){
			double a=Math.PI/6*k;
			int px=CX+(int)Math.round(Math.cos(a)*10),pz=APSE_Z+(int)Math.round(Math.sin(a)*10);
			pillar(level,bb,px,pz,F+2,ceiling(dist(px,pz,CX,APSE_Z)),Blocks.CHISELED_DEEPSLATE.defaultBlockState(),PILLAR,DARK);
			set(level,bb,LIGHT,px,F+6,pz);
		}
		for(int x:new int[]{CX-4,CX+4}){
			int top=ceiling(dist(x,58,CX,APSE_Z));
			hangingLantern(level,bb,x,top,58,top-(F+7),true);
		}
		// The altar on the dais: the Bell Shard on it, the broken lip of the Warden's bell, and candles either side.
		var candles=Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES,4).setValue(CandleBlock.LIT,true);
		set(level,bb,Blocks.CHISELED_DEEPSLATE.defaultBlockState(),CX,F+2,63);
		set(level,bb,Blocks.POLISHED_DEEPSLATE_SLAB.defaultBlockState(),CX-1,F+2,63);set(level,bb,Blocks.POLISHED_DEEPSLATE_SLAB.defaultBlockState(),CX+1,F+2,63);
		set(level,bb,dev.wildercord.aura.world.BellShard.BLOCK.defaultBlockState(),CX,F+3,63);
		set(level,bb,candles.setValue(CandleBlock.CANDLES,2),CX-3,F+2,62);set(level,bb,candles.setValue(CandleBlock.CANDLES,2),CX+3,F+2,62);
		for(int x:new int[]{CX-6,CX+6}){set(level,bb,LIGHT,x,F+2,60);}
		// Outside, buttresses fan round the apse's wall like the nave's, each crowned by a lit pinnacle; none stands behind the rose window.
		for(int k:new int[]{1,2,4,5}){
			double a=Math.PI/6*k;
			int bx=CX+(int)Math.round(Math.cos(a)*13),bz=APSE_Z+(int)Math.round(Math.sin(a)*13);
			foundation(level,bb,bx,bz);
			fill(level,bb,bx,F,bz,bx,F+9,bz,PILLAR);
			pinnacle(level,bb,bx,F+10,bz);
		}
	}

	/** A pinnacle on a buttress's head: a carved block, a slender shaft, a sea lantern and a dark cap, so the sunken walls show their line in the gloom. */
	private void pinnacle(WorldGenLevel level,BoundingBox bb,int x,int y,int z){
		set(level,bb,Blocks.CHISELED_DEEPSLATE.defaultBlockState(),x,y,z);
		set(level,bb,Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState(),x,y+1,z);
		set(level,bb,LIGHT,x,y+2,z);
		set(level,bb,Blocks.DARK_PRISMARINE_SLAB.defaultBlockState(),x,y+3,z);
	}

	/** The apse's wall: plain stone, but at its back a rose window, a ring of blue round spokes of cyan and a yellow heart. */
	private BlockState rose(int x,int y,int z){
		int dx=x-CX,dy=y-(F+5);
		if(z<62 || Math.abs(dx)>3)return stone(x,y,z);
		double d=Math.sqrt(dx*dx+dy*dy);
		if(d>3.6)return stone(x,y,z);
		if(d<0.8)return Blocks.STAINED_GLASS.yellow().defaultBlockState();
		if(Math.abs(d-3)<0.6)return GLASS;
		if(dx==0 || dy==0 || Math.abs(dx)==Math.abs(dy))return Blocks.STAINED_GLASS.cyan().defaultBlockState();
		return Blocks.STAINED_GLASS.lightBlue().defaultBlockState();
	}

	/** The broken bell frame where nave meets apse: two posts, a beam snapped short, the chains that held the bell hanging empty. */
	private void frame(WorldGenLevel level,BoundingBox bb){
		var post=Blocks.DARK_OAK_LOG.defaultBlockState();
		var beam=post.setValue(RotatedPillarBlock.AXIS,Direction.Axis.X);
		for(int x:new int[]{REST_X-4,REST_X+4})fill(level,bb,x,F+1,REST_Z,x,F+9,REST_Z,post);
		fill(level,bb,REST_X-4,F+10,REST_Z,REST_X+1,F+10,REST_Z,beam);
		set(level,bb,beam,REST_X+4,F+10,REST_Z);
		fill(level,bb,REST_X-1,F+7,REST_Z,REST_X-1,F+9,REST_Z,Blocks.IRON_CHAIN.defaultBlockState());
		set(level,bb,Blocks.IRON_CHAIN.defaultBlockState(),REST_X+1,F+9,REST_Z);
		// The rest of the beam, where it fell.
		set(level,bb,Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z),REST_X+3,F+1,REST_Z+2);
		set(level,bb,Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z),REST_X+3,F+1,REST_Z+3);
	}
}
