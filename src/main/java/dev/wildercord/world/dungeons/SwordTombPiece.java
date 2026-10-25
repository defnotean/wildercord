package dev.wildercord.world.dungeons;

import dev.wildercord.aura.world.*;
import dev.wildercord.config.Config;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import java.util.*;

/** A stair into a buried blade gallery, two intent thresholds and a spacious physical duel chamber. */
public final class SwordTombPiece extends DungeonPiece {
	public SwordTombPiece(int x,int y,int z,Direction facing){super(DungeonWorldgen.SWORD_TOMB_PIECE,x,y,z,41,21,55,facing);}
	public SwordTombPiece(CompoundTag tag){super(DungeonWorldgen.SWORD_TOMB_PIECE,tag);}
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c){
		if(!Config.get().auraWorld().swordTombs())return Optional.empty();
		var piece=new SwordTombPiece(c.chunkPos().getMinBlockX(),0,c.chunkPos().getMinBlockZ(),Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		var entrance=piece.localPosition(20,0,2);
		int surface=c.chunkGenerator().getFirstOccupiedHeight(entrance.getX(),entrance.getZ(),Heightmap.Types.WORLD_SURFACE_WG,c.heightAccessor(),c.randomState());
		int floor=c.chunkGenerator().getFirstOccupiedHeight(entrance.getX(),entrance.getZ(),Heightmap.Types.OCEAN_FLOOR_WG,c.heightAccessor(),c.randomState());
		if(surface<c.chunkGenerator().getSeaLevel() || surface-floor>1)return Optional.empty();
		piece.move(0,surface-12,0);return Optional.of(new Structure.GenerationStub(entrance.atY(surface),b->b.addPiece(piece)));
	}
	@Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox bb,ChunkPos chunk,BlockPos ref){
		DungeonWards.remember(level,this);var wall=Blocks.DEEPSLATE_TILES.defaultBlockState();var carved=Blocks.CHISELED_STONE_BRICKS.defaultBlockState();var trim=Blocks.POLISHED_ANDESITE.defaultBlockState();
		room(level,bb,10,0,0,30,12,16,wall);room(level,bb,6,0,15,34,7,25,wall);
		room(level,bb,4,0,24,36,10,47,wall);room(level,bb,10,0,46,30,7,54,wall);
		fill(level,bb,19,1,15,21,3,25,AIR);fill(level,bb,19,1,46,21,3,48,AIR);
		// Twelve grounded steps join the open entrance to the gallery; every step has walking headroom.
		for(int step=0;step<=12;step++){
			int z=2+step,y=12-step;fill(level,bb,18,0,z,22,y,z,trim);fill(level,bb,18,y+1,z,22,17,z,AIR);
		}
		// The roofed vestibule is marked at the surface by a broken arch and two carved blade stones.
		for(int x:new int[]{16,24}){fill(level,bb,x,12,1,x,15,1,carved);set(level,bb,Blocks.STONE_BRICK_SLAB.defaultBlockState(),x,16,1);}
		fill(level,bb,17,15,1,23,15,1,carved);fill(level,bb,18,13,0,22,14,1,AIR);
		// A grass barrow covers the vestibule; keep the descending stair open to the sky.
		for(int x=10;x<=30;x++)for(int z=0;z<=16;z++){
			if(x>=18 && x<=22 && z<=14)continue;
			double slope=(x-20)*(x-20)/100.0+(z-8)*(z-8)/64.0;
			int top=13+(int)Math.max(0,3*(1-slope));
			fill(level,bb,x,12,z,x,top-1,z,Blocks.DIRT.defaultBlockState());set(level,bb,Blocks.GRASS_BLOCK.defaultBlockState(),x,top,z);
		}
		// Each gallery bay has a grave plinth, steel blade and modest lamp, arranged away from the central route.
		for(int x:new int[]{9,31})for(int z:new int[]{18,22}){
			fill(level,bb,x-1,1,z-1,x+1,1,z+1,Blocks.POLISHED_DEEPSLATE.defaultBlockState());
			set(level,bb,Blocks.IRON_BARS.defaultBlockState(),x,2,z);set(level,bb,Blocks.LANTERN.defaultBlockState(),x,3,z);
		}
		for(int z:new int[]{27,34,43})for(int x:new int[]{6,34}){
			fill(level,bb,x,1,z,x,7,z,carved);set(level,bb,Blocks.LANTERN.defaultBlockState(),x,8,z);
		}
		for(int x:new int[]{14,26})for(int z:new int[]{30,40}){
			fill(level,bb,x,7,z,x,9,z,Blocks.IRON_CHAIN.defaultBlockState());
			set(level,bb,Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING,true),x,6,z);
		}
		// Low pendants light the actual duel and the keeper's mask, rather than only the perimeter.
		for(int x:new int[]{18,22}){
			fill(level,bb,x,5,36,x,9,36,Blocks.IRON_CHAIN.defaultBlockState());
			set(level,bb,Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING,true),x,4,36);
		}
		// Broken burial terraces provide flanking cover without choking the sweep/thrust lanes.
		for(int x:new int[]{8,29}){fill(level,bb,x,0,29,x+3,0,42,trim);for(int z:new int[]{30,40})fill(level,bb,x,1,z,x+2,1,z+1,Blocks.STONE_BRICK_SLAB.defaultBlockState());}
		fill(level,bb,14,0,28,26,0,44,Blocks.SMOOTH_STONE.defaultBlockState());
		if(variant()==1){fill(level,bb,7,1,18,12,2,20,AIR);fill(level,bb,7,0,18,12,0,20,Blocks.MOSS_BLOCK.defaultBlockState());}
		if(variant()==2){fill(level,bb,28,1,48,32,3,50,AIR);fill(level,bb,28,0,48,32,0,50,trim);}
		dress(level,bb);
		gate(level,bb,15,2);gate(level,bb,24,3);
		chest(level,bb,7,1,23,DungeonWorldgen.TOMB_HALL,Direction.EAST,931);
		chest(level,bb,29,1,51,DungeonWorldgen.TOMB_HALL,Direction.WEST,932);
		var altar=localPosition(20,1,44);
		if(bb.isInside(altar)){
			level.setBlock(altar,SwordTombs.RELIQUARY.defaultBlockState().setValue(TombReliquary.FACING,inward(20,44)),2);
			if(level.getBlockEntity(altar) instanceof TombReliquaryEntity entity)entity.awaken();
		}
		fill(level,bb,17,1,50,23,1,52,carved);set(level,bb,Blocks.IRON_BARS.defaultBlockState(),20,2,51);
	}
	/** Architecture over the plan: a pedimented gate on the barrow, a lined cut, vaulted halls and a framed keeper's niche. */
	private void dress(WorldGenLevel level,BoundingBox bb){
		var carved=Blocks.CHISELED_STONE_BRICKS.defaultBlockState();var dark=Blocks.POLISHED_DEEPSLATE.defaultBlockState();var tiles=Blocks.DEEPSLATE_TILES.defaultBlockState();
		var cap=Blocks.POLISHED_DEEPSLATE_WALL.defaultBlockState();
		// The gate: carved piers rise through the barrow again, flanked by dark pilasters and soul lights, under a stepped pediment.
		for(int x:new int[]{16,24}){fill(level,bb,x,12,1,x,15,1,carved);sconce(level,bb,x,14,0,Direction.SOUTH,true);}
		for(int x:new int[]{15,25})fill(level,bb,x,12,1,x,14,1,dark);
		for(int i=0;i<4;i++){
			int y=16+i;
			set(level,bb,stair(Blocks.POLISHED_DEEPSLATE_STAIRS,Direction.EAST,false),16+i,y,1);
			set(level,bb,stair(Blocks.POLISHED_DEEPSLATE_STAIRS,Direction.WEST,false),24-i,y,1);
			if(17+i<=23-i)fill(level,bb,17+i,y,1,23-i,y,1,dark);
		}
		set(level,bb,carved,20,18,1);set(level,bb,carved,20,19,1);set(level,bb,cap,20,20,1);
		// A dark threshold where the barrow opens.
		for(int x=18;x<=22;x++)for(int z=0;z<=1;z++)set(level,bb,x==18 || x==22?carved:dark,x,12,z);
		// The cut is faced with tiles instead of bare earth and railed along its lip.
		for(int x:new int[]{17,23})for(int z=0;z<=14;z++){
			int top=barrow(x,z);
			for(int y=12;y<=top;y++)set(level,bb,noise(x,y,z)<20?Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState():tiles,x,y,z);
			if(z>=2)set(level,bb,cap,x,top+1,z);
		}
		// Grave stelae stand in the turf of the barrow, either side of the cut.
		for(int x:new int[]{12,14,26,28})for(int z:new int[]{6,10}){
			int top=barrow(x,z),n=noise(x,z);
			set(level,bb,(n<50?Blocks.STONE_BRICK_WALL:Blocks.MOSSY_STONE_BRICK_WALL).defaultBlockState(),x,top+1,z);
			if(n<70)set(level,bb,Blocks.STONE_BRICK_SLAB.defaultBlockState(),x,top+2,z);
		}
		// Soul lamps light the vestibule hall beneath the stair.
		hangingLantern(level,bb,14,11,8,2,true);hangingLantern(level,bb,26,11,8,2,true);
		// Gallery: corbelled cornices, a rib across the bays, mourning banners and a dark runner down the route.
		for(int z=16;z<=23;z++){
			set(level,bb,stair(Blocks.DEEPSLATE_TILE_STAIRS,Direction.WEST,true),7,6,z);
			set(level,bb,stair(Blocks.DEEPSLATE_TILE_STAIRS,Direction.EAST,true),33,6,z);
			for(int x=18;x<=22;x++)set(level,bb,x==18 || x==22?Blocks.POLISHED_ANDESITE.defaultBlockState():dark,x,0,z);
		}
		fill(level,bb,7,6,20,33,6,20,dark);
		for(int z:new int[]{17,21}){
			var colour=z==17?net.minecraft.world.item.DyeColor.BLACK:net.minecraft.world.item.DyeColor.GRAY;
			banner(level,bb,Blocks.WALL_BANNER.pick(colour),7,4,z,Direction.EAST);
			banner(level,bb,Blocks.WALL_BANNER.pick(colour),33,4,z,Direction.WEST);
		}
		weather(level,bb,6,1,15,34,7,25,tiles,Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState(),12);
		// Duel chamber: a cornice along both walls, bases and capitals on the piers, and transverse ribs on corbels.
		for(int z=25;z<=46;z++){
			set(level,bb,stair(Blocks.DEEPSLATE_TILE_STAIRS,Direction.WEST,true),5,5,z);
			set(level,bb,stair(Blocks.DEEPSLATE_TILE_STAIRS,Direction.EAST,true),35,5,z);
		}
		for(int z:new int[]{27,34,43}){
			for(int x:new int[]{6,34}){
				for(int y:new int[]{1,7}){
					boolean top=y==7;
					set(level,bb,stair(Blocks.POLISHED_DEEPSLATE_STAIRS,Direction.WEST,top),x+1,y,z);
					set(level,bb,stair(Blocks.POLISHED_DEEPSLATE_STAIRS,Direction.EAST,top),x-1,y,z);
					set(level,bb,stair(Blocks.POLISHED_DEEPSLATE_STAIRS,Direction.NORTH,top),x,y,z-1);
					set(level,bb,stair(Blocks.POLISHED_DEEPSLATE_STAIRS,Direction.SOUTH,top),x,y,z+1);
				}
			}
			fill(level,bb,5,9,z,35,9,z,dark);
			set(level,bb,stair(Blocks.POLISHED_DEEPSLATE_STAIRS,Direction.WEST,true),5,8,z);
			set(level,bb,stair(Blocks.POLISHED_DEEPSLATE_STAIRS,Direction.EAST,true),35,8,z);
		}
		// A dark ring is set flush into the duelling floor round the keeper's ground.
		for(int x=14;x<=26;x++)for(int z=29;z<=43;z++){double d=dist(x,z,20,36);if(d>=5.5 && d<6.5)set(level,bb,dark,x,0,z);}
		// The keeper's niche: carved jambs and a lintel frame the crypt door, banners hung either side.
		for(int x:new int[]{17,23})fill(level,bb,x,1,45,x,5,45,carved);
		fill(level,bb,17,6,45,23,6,45,dark);
		set(level,bb,stair(Blocks.POLISHED_DEEPSLATE_STAIRS,Direction.WEST,true),18,5,45);
		set(level,bb,stair(Blocks.POLISHED_DEEPSLATE_STAIRS,Direction.EAST,true),22,5,45);
		banner(level,bb,Blocks.WALL_BANNER.pick(net.minecraft.world.item.DyeColor.BLACK),18,3,45,Direction.SOUTH);
		banner(level,bb,Blocks.WALL_BANNER.pick(net.minecraft.world.item.DyeColor.BLACK),22,3,45,Direction.SOUTH);
		weather(level,bb,4,1,24,36,10,47,tiles,Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState(),12);
		// Crypt: soul lamps over the bier and candles left burning on it.
		for(int x:new int[]{14,20,26})hangingLantern(level,bb,x,6,50,1,true);
		for(int x:new int[]{18,22})set(level,bb,Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.CANDLES,x==18?3:2).setValue(net.minecraft.world.level.block.CandleBlock.LIT,true),x,2,51);
	}
	/** The barrow's turf height at a column, matching the mound built in postProcess. */
	private static int barrow(int x,int z){double slope=(x-20)*(x-20)/100.0+(z-8)*(z-8)/64.0;return 13+(int)Math.max(0,3*(1-slope));}
	private void gate(WorldGenLevel level,BoundingBox bb,int z,int stage){for(int x=19;x<=21;x++)for(int y=1;y<=3;y++){
		var p=localPosition(x,y,z);if(bb.isInside(p))level.setBlock(p,SwordTombs.GATE.defaultBlockState().setValue(IntentGate.STAGE,stage).setValue(IntentGate.FACING,inward(x,z)),2);
	}}
	@Override public List<BoundingBox> wardedBoxes(){return List.of(worldBox(6,0,15,34,7,25),worldBox(4,0,24,36,10,47),worldBox(10,0,46,30,7,54));}
}
