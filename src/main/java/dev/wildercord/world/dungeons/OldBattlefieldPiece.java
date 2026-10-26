package dev.wildercord.world.dungeons;

import dev.wildercord.aura.world.BattlefieldMemoryEntity;
import dev.wildercord.aura.world.BattlefieldMemorial;
import dev.wildercord.aura.world.BattlefieldRules;
import dev.wildercord.aura.world.Battlefields;
import dev.wildercord.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.StructureManager;
import java.util.List;
import java.util.Optional;

/**
 * Three authored outdoor ruins: a broken barricade, a shelter garden, and the cairns of a retreat. Every field shares the
 * same frame: a breached ring wall with a gatehouse on the track, a wrecked siege engine and shell craters outside it,
 * the wreckage of the fight across the ground, and a tall obelisk with a fire kept burning at its top.
 */
public final class OldBattlefieldPiece extends DungeonPiece {
	public OldBattlefieldPiece(int x,int y,int z,Direction facing) { super(DungeonWorldgen.BATTLEFIELD_PIECE,x,y,z,31,24,31,facing); }
	public OldBattlefieldPiece(CompoundTag tag) { super(DungeonWorldgen.BATTLEFIELD_PIECE,tag); }
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		if (!Config.get().auraWorld().battlefields()) return Optional.empty();
		var piece=new OldBattlefieldPiece(c.chunkPos().getMinBlockX(),0,c.chunkPos().getMinBlockZ(),Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		var centre=piece.localPosition(15,0,15);
		int surface=height(c,centre,Heightmap.Types.WORLD_SURFACE_WG);
		int floor=height(c,centre,Heightmap.Types.OCEAN_FLOOR_WG);
		int[] neighbours=new int[4];int i=0;
		for (int[] d:new int[][]{{-12,0},{12,0},{0,-12},{0,12}}) neighbours[i++]=height(c,centre.offset(d[0],0,d[1]),Heightmap.Types.WORLD_SURFACE_WG);
		if (!BattlefieldRules.footing(surface,floor,neighbours,c.chunkGenerator().getSeaLevel())) return Optional.empty();
		piece.move(0,surface,0);
		return Optional.of(new Structure.GenerationStub(centre.atY(surface),b->b.addPiece(piece)));
	}
	private static int height(Structure.GenerationContext c,BlockPos p,Heightmap.Types map) {
		return c.chunkGenerator().getFirstOccupiedHeight(p.getX(),p.getZ(),map,c.heightAccessor(),c.randomState());
	}
	@Override public void postProcess(WorldGenLevel level,StructureManager structures,ChunkGenerator generator,RandomSource random,
		BoundingBox bb,ChunkPos chunk,BlockPos reference) {
		var stone=Blocks.MOSSY_COBBLESTONE.defaultBlockState();var earth=Blocks.COARSE_DIRT.defaultBlockState();
		// Small central footing, with gravel tracks and scattered fragments instead of a square floor.
		for (int x=4;x<=26;x++) for (int z=4;z<=26;z++) {
			int dx=x-15,dz=z-15;
			if (dx*dx+dz*dz<28 || (Math.abs(dx)<2 && z>6 && z<25)) {
				fill(level,bb,x,-6,z,x,-1,z,Blocks.DIRT.defaultBlockState());
				set(level,bb,noise(x,z)<35?Blocks.GRAVEL.defaultBlockState():earth,x,0,z);
			}
			if (noise(x,z)<8 && dx*dx+dz*dz>30) {
				fill(level,bb,x,-6,z,x,0,z,Blocks.DIRT.defaultBlockState());
				set(level,bb,Blocks.MOSSY_COBBLESTONE_SLAB.defaultBlockState(),x,1,z);
			}
		}
		rampart(level,bb);
		siegeEngine(level,bb);
		for (int[] c:new int[][]{{27,27},{4,27},{27,3}}) crater(level,bb,c[0],c[1]);
		switch (variant()) {
			case 0 -> {
				// Staggered barricades flank a deliberate breach. Charred uprights lean around its empty centre.
				for (int x:new int[]{6,9,12,18,21,24}) {
					fill(level,bb,x,-6,10,x,3+(x%2),10,Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState());
					set(level,bb,Blocks.COBBLESTONE_WALL.defaultBlockState(),x,1,11);
				}
				fill(level,bb,6,1,10,12,1,10,Blocks.DARK_OAK_FENCE.defaultBlockState());
				fill(level,bb,18,1,10,24,1,10,Blocks.DARK_OAK_FENCE.defaultBlockState());
				for (int z=13;z<24;z++) set(level,bb,Blocks.PACKED_MUD.defaultBlockState(),10+z%3,0,z);
				fill(level,bb,20,0,20,24,0,23,Blocks.BLACKSTONE.defaultBlockState());
				// A line of stakes before the barricade, a few knocked flat in the charge.
				for (int x=6;x<=24;x+=2) {
					if (x>12 && x<18) continue;
					if (noise(x,8)<30) set(level,bb,Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,Direction.Axis.Z),x,1,7);
					else fill(level,bb,x,1,7,x,1+noise(x,7)%2,7,Blocks.DARK_OAK_FENCE.defaultBlockState());
				}
				banner(level,bb,Blocks.WALL_BANNER.pick(net.minecraft.world.item.DyeColor.RED),9,3,9,Direction.SOUTH);
				banner(level,bb,Blocks.WALL_BANNER.pick(net.minecraft.world.item.DyeColor.RED),21,3,9,Direction.SOUTH);
			}
			case 1 -> {
				// The shelter's wall has an open doorway and a roof that has fallen into the garden.
				fill(level,bb,7,-6,11,7,3,23,stone);fill(level,bb,8,-6,23,23,2,23,stone);
				fill(level,bb,23,-6,15,23,4,23,stone);
				for (int x=9;x<=21;x+=3) {
					set(level,bb,Blocks.MOSS_BLOCK.defaultBlockState(),x,0,20);
					set(level,bb,Blocks.AZALEA.defaultBlockState(),x,1,20);
				}
				fill(level,bb,8,0,12,10,0,15,Blocks.OAK_PLANKS.defaultBlockState());
				set(level,bb,Blocks.COMPOSTER.defaultBlockState(),21,1,18);
				// What is left of the roof: a broken sheet of it slid off into the garden, the wall heads capped and crumbling.
				for (int x=8;x<=10;x++) {
					set(level,bb,stair(Blocks.SPRUCE_STAIRS,Direction.NORTH,false),x,1,13);
					if (noise(x,14)<70) set(level,bb,Blocks.SPRUCE_SLAB.defaultBlockState(),x,1,14);
				}
				for (int z=11;z<=23;z++) if (noise(7,z)<55) set(level,bb,Blocks.MOSSY_COBBLESTONE_SLAB.defaultBlockState(),7,4,z);
				for (int x=8;x<=22;x++) if (noise(x,23)<55) set(level,bb,Blocks.MOSSY_COBBLESTONE_SLAB.defaultBlockState(),x,3,23);
				for (int z=15;z<=23;z++) if (noise(23,z)<55) set(level,bb,Blocks.MOSSY_COBBLESTONE_SLAB.defaultBlockState(),23,5,z);
			}
			default -> {
				// Paired cairns preserve a route; the last watchtower is broken, never a copy of the other ruins.
				for (int z:new int[]{7,12,20,25}) for (int x:new int[]{10,20}) {
					fill(level,bb,x,-6,z,x,1+(z%2),z,stone);
					set(level,bb,Blocks.STONE_BRICK_SLAB.defaultBlockState(),x,2+(z%2),z);
				}
				for (int x:new int[]{23,26}) for (int z:new int[]{6,9}) fill(level,bb,x,-6,z,x,3+(z%2),z,Blocks.SPRUCE_LOG.defaultBlockState());
				fill(level,bb,23,3,6,26,3,7,Blocks.SPRUCE_PLANKS.defaultBlockState());
				set(level,bb,Blocks.LANTERN.defaultBlockState(),25,4,7);
				// A rail round the watch platform, and one of the tower's legs lying where it fell.
				for (int x=23;x<=26;x++) if (x!=25) set(level,bb,Blocks.SPRUCE_FENCE.defaultBlockState(),x,4,6);
				fill(level,bb,22,1,10,22,1,13,Blocks.SPRUCE_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,Direction.Axis.Z));
			}
		}
		remembrance(level,bb);
		wreckage(level,bb);
		BlockPos marker=localPosition(15,1,15);
		if (bb.isInside(marker)) {
			level.setBlock(marker,Battlefields.MEMORIAL.defaultBlockState().setValue(BattlefieldMemorial.KIND,variant()),2);
			if (level.getBlockEntity(marker) instanceof BattlefieldMemoryEntity memory) memory.awaken();
		}
		chest(level,bb,18,1,18,DungeonWorldgen.BATTLEFIELD_SUPPLIES,Direction.NORTH,821);
	}
	/**
	 * What every field keeps: a worn ring of paving round the memorial, two banner poles where the track comes up to it, and
	 * a row of graves off to one side. The ground round the memorial itself is left open, so it can be stood at.
	 */
	private void remembrance(WorldGenLevel level,BoundingBox bb) {
		for (int x=10;x<=20;x++) for (int z=10;z<=20;z++) {
			double d=dist(x,z,15,15);if (d<2.6 || d>4.6) continue;
			int n=noise(x,z);
			if (n<18) continue;
			set(level,bb,(n<55?Blocks.STONE_BRICKS:n<75?Blocks.MOSSY_STONE_BRICKS:n<90?Blocks.CRACKED_STONE_BRICKS:Blocks.MOSSY_COBBLESTONE).defaultBlockState(),x,0,z);
		}
		for (int x:new int[]{13,17}) {
			fill(level,bb,x,1,12,x,3,12,Blocks.DARK_OAK_FENCE.defaultBlockState());
			set(level,bb,Blocks.STONE_BRICK_SLAB.defaultBlockState(),x,4,12);
			banner(level,bb,Blocks.WALL_BANNER.pick(x<15?net.minecraft.world.item.DyeColor.RED:net.minecraft.world.item.DyeColor.GRAY),x,3,11,Direction.SOUTH);
		}
		// A tall obelisk behind the memorial on a stepped plinth, the fallen's banner hung on its face, a lamp kept at its top.
		fill(level,bb,13,-6,18,17,-1,22,Blocks.DIRT.defaultBlockState());
		fill(level,bb,13,0,18,17,0,22,Blocks.STONE_BRICKS.defaultBlockState());
		for (int x=13;x<=17;x++) {
			set(level,bb,stair(Blocks.STONE_BRICK_STAIRS,Direction.SOUTH,false),x,1,18);
			set(level,bb,stair(Blocks.STONE_BRICK_STAIRS,Direction.NORTH,false),x,1,22);
		}
		for (int z=19;z<=21;z++) {
			set(level,bb,stair(Blocks.STONE_BRICK_STAIRS,Direction.EAST,false),13,1,z);
			set(level,bb,stair(Blocks.STONE_BRICK_STAIRS,Direction.WEST,false),17,1,z);
		}
		fill(level,bb,14,1,19,16,1,21,Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
		fill(level,bb,14,2,19,16,2,21,Blocks.POLISHED_ANDESITE.defaultBlockState());
		for (int x:new int[]{14,16}) for (int z:new int[]{19,21}) set(level,bb,Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),x,2,z);
		// A three-block die, then a fluted shaft (a cross of four ribs round the core) that narrows to a fire bowl at the top.
		fill(level,bb,14,3,19,16,5,21,Blocks.STONE_BRICKS.defaultBlockState());
		for (int x:new int[]{14,16}) for (int z:new int[]{19,21}) {
			set(level,bb,Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),x,5,z);
			set(level,bb,Blocks.LANTERN.defaultBlockState(),x,6,z);
		}
		fill(level,bb,15,6,20,15,19,20,Blocks.POLISHED_ANDESITE.defaultBlockState());
		for (int[] r:new int[][]{{14,20},{16,20},{15,19},{15,21}}) {
			fill(level,bb,r[0],6,r[1],r[0],13,r[1],Blocks.STONE_BRICKS.defaultBlockState());
			Direction in=r[0]<15?Direction.EAST:r[0]>15?Direction.WEST:r[1]<20?Direction.NORTH:Direction.SOUTH;
			set(level,bb,stair(Blocks.STONE_BRICK_STAIRS,in,false),r[0],14,r[1]);
		}
		for (int y:new int[]{9,16}) set(level,bb,Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),15,y,20);
		set(level,bb,Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),15,20,20);
		set(level,bb,Blocks.CAMPFIRE.defaultBlockState(),15,21,20);
		banner(level,bb,Blocks.WALL_BANNER.pick(net.minecraft.world.item.DyeColor.RED),15,11,18,Direction.SOUTH);
		banner(level,bb,Blocks.WALL_BANNER.pick(net.minecraft.world.item.DyeColor.GRAY),13,11,20,Direction.WEST);
		banner(level,bb,Blocks.WALL_BANNER.pick(net.minecraft.world.item.DyeColor.GRAY),17,11,20,Direction.EAST);
		weather(level,bb,13,0,18,17,13,22,Blocks.STONE_BRICKS.defaultBlockState(),Blocks.CRACKED_STONE_BRICKS.defaultBlockState(),20);
		weather(level,bb,13,0,18,17,8,22,Blocks.STONE_BRICKS.defaultBlockState(),Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),18);
		// The graves: a low mound and a marker each, some of the markers fallen.
		for (int x:new int[]{3,5}) for (int z:new int[]{14,17,20}) {
			fill(level,bb,x,-3,z,x,-1,z,Blocks.DIRT.defaultBlockState());
			set(level,bb,Blocks.COARSE_DIRT.defaultBlockState(),x,0,z);
			set(level,bb,Blocks.COARSE_DIRT.defaultBlockState(),x,0,z+1);
			int n=noise(x,z);
			set(level,bb,(n<25?Blocks.MOSSY_COBBLESTONE_SLAB:n<60?Blocks.COBBLESTONE_WALL:Blocks.MOSSY_STONE_BRICK_WALL).defaultBlockState(),x,1,z);
		}
	}
	/**
	 * The ring wall the field was fought over: a low, much-breached circuit of rubble and dressed stone round the whole
	 * field, open where the track runs through, with a gatehouse of two towers (one fallen to a stump) over the way in.
	 */
	private void rampart(WorldGenLevel level,BoundingBox bb) {
		for (int x=0;x<=30;x++) for (int z=0;z<=30;z++) {
			double d=dist(x,z,15,15);
			// Skipped at the front-left, where the trebuchet stands in the breach it battered.
			if (d<13.5 || d>14.6 || Math.abs(x-15)<=2 || x<=8 && z<=7) continue;
			int n=noise(x,7,z);
			if (n<22) continue;
			int top=n<45?1:n<80?2:3;
			fill(level,bb,x,-4,z,x,-1,z,Blocks.DIRT.defaultBlockState());
			for (int y=0;y<top;y++) {
				int m=noise(x,y,z);
				set(level,bb,(m<35?Blocks.MOSSY_COBBLESTONE:m<60?Blocks.COBBLESTONE:m<85?Blocks.STONE_BRICKS:Blocks.CRACKED_STONE_BRICKS).defaultBlockState(),x,y,z);
			}
			if (noise(x,9,z)<40) set(level,bb,Blocks.MOSSY_COBBLESTONE_SLAB.defaultBlockState(),x,top,z);
		}
		// The gatehouse: two towers flanking the track, the left one standing to its crenels, the right one a broken stump.
		for (int t=0;t<2;t++) {
			int x0=t==0?10:18,top=t==0?8:4;
			fill(level,bb,x0,-4,1,x0+2,-1,3,Blocks.DIRT.defaultBlockState());
			fill(level,bb,x0,0,1,x0+2,top,3,Blocks.STONE_BRICKS.defaultBlockState());
			for (int x=x0;x<=x0+2;x+=2) for (int z=1;z<=3;z+=2) fill(level,bb,x,0,z,x,top,z,Blocks.POLISHED_ANDESITE.defaultBlockState());
			if (t==0) {
				for (int x=x0;x<=x0+2;x++) for (int z=1;z<=3;z++) if ((x+z)%2==0) set(level,bb,Blocks.STONE_BRICK_WALL.defaultBlockState(),x,top+1,z);
				set(level,bb,Blocks.AIR.defaultBlockState(),x0+1,5,1);
				banner(level,bb,Blocks.WALL_BANNER.pick(net.minecraft.world.item.DyeColor.RED),x0+1,6,0,Direction.SOUTH);
				// What is left of the arch: a corbelled spring off the standing tower, reaching half across the gate.
				set(level,bb,stair(Blocks.STONE_BRICK_STAIRS,Direction.WEST,true),x0+3,6,2);
				set(level,bb,Blocks.STONE_BRICKS.defaultBlockState(),x0+3,7,2);
				set(level,bb,Blocks.STONE_BRICK_SLAB.defaultBlockState(),x0+4,7,2);
			} else {
				for (int x=x0;x<=x0+2;x++) for (int z=1;z<=3;z++) if (noise(x,top,z)<55) set(level,bb,Blocks.AIR.defaultBlockState(),x,top,z);
				// Its upper courses lie tumbled across the ground outside.
				for (int[] r:new int[][]{{21,1},{22,2},{22,0},{23,1},{24,3}})
					set(level,bb,(noise(r[0],r[1])<50?Blocks.CRACKED_STONE_BRICKS:Blocks.MOSSY_STONE_BRICKS).defaultBlockState(),r[0],0,r[1]);
				set(level,bb,Blocks.STONE_BRICK_SLAB.defaultBlockState(),22,1,1);
			}
			weather(level,bb,x0,0,1,x0+2,top,3,Blocks.STONE_BRICKS.defaultBlockState(),Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),25);
			weather(level,bb,x0,0,1,x0+2,top,3,Blocks.STONE_BRICKS.defaultBlockState(),Blocks.CRACKED_STONE_BRICKS.defaultBlockState(),20);
		}
		// The track runs out through the gate.
		for (int z=0;z<=6;z++) for (int x=14;x<=16;x++) {
			fill(level,bb,x,-4,z,x,-1,z,Blocks.DIRT.defaultBlockState());
			set(level,bb,noise(x,z)<45?Blocks.GRAVEL.defaultBlockState():Blocks.COARSE_DIRT.defaultBlockState(),x,0,z);
		}
	}

	/** Outside the wall at the front: the wreck of a trebuchet, its arm snapped, its counterweight still hung and a pile of shot beside it. */
	private void siegeEngine(WorldGenLevel level,BoundingBox bb) {
		var beam=Blocks.DARK_OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,Direction.Axis.X);
		var cross=Blocks.DARK_OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,Direction.Axis.Z);
		fill(level,bb,1,-4,1,7,-1,6,Blocks.DIRT.defaultBlockState());
		fill(level,bb,1,0,1,7,0,6,Blocks.COARSE_DIRT.defaultBlockState());
		for (int z:new int[]{2,5}) {
			fill(level,bb,1,1,z,7,1,z,beam);
			fill(level,bb,4,2,z,4,6,z,Blocks.DARK_OAK_LOG.defaultBlockState());
			set(level,bb,stair(Blocks.DARK_OAK_STAIRS,Direction.EAST,false),3,2,z);
			set(level,bb,stair(Blocks.DARK_OAK_STAIRS,Direction.WEST,false),5,2,z);
		}
		for (int x:new int[]{1,7}) fill(level,bb,x,1,3,x,1,4,cross);
		fill(level,bb,4,7,2,4,7,5,cross);
		// The counterweight box hangs on chains below the axle; the arm's stub points up and its broken length lies in the grass.
		fill(level,bb,3,6,3,3,6,4,Blocks.IRON_CHAIN.defaultBlockState());
		fill(level,bb,2,4,3,3,5,4,Blocks.COBBLESTONE.defaultBlockState());
		set(level,bb,Blocks.SPRUCE_PLANKS.defaultBlockState(),2,5,3);
		set(level,bb,Blocks.SPRUCE_PLANKS.defaultBlockState(),3,5,4);
		fill(level,bb,5,8,3,5,9,3,Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState());
		set(level,bb,Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,Direction.Axis.X),6,8,3);
		fill(level,bb,6,1,3,8,1,3,Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,Direction.Axis.X));
		set(level,bb,Blocks.IRON_CHAIN.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,Direction.Axis.X),9,1,3);
		for (int[] r:new int[][]{{1,7,0},{2,7,0},{1,8,0},{2,8,0},{1,7,1},{3,8,0}})
			set(level,bb,(noise(r[0],r[2],r[1])<50?Blocks.COBBLESTONE:Blocks.ANDESITE).defaultBlockState(),r[0],r[2],r[1]);
	}

	/** A shell crater: a scorched bowl with a lip of thrown earth. */
	private void crater(WorldGenLevel level,BoundingBox bb,int cx,int cz) {
		for (int x=cx-3;x<=cx+3;x++) for (int z=cz-3;z<=cz+3;z++) {
			double d=dist(x,z,cx,cz);
			if (d>3.2) continue;
			fill(level,bb,x,-4,z,x,-2,z,Blocks.DIRT.defaultBlockState());
			if (d<1.6) {
				set(level,bb,noise(x,z)<40?Blocks.BLACKSTONE.defaultBlockState():Blocks.COARSE_DIRT.defaultBlockState(),x,-1,z);
				fill(level,bb,x,0,z,x,2,z,Blocks.AIR.defaultBlockState());
			} else if (d<2.5) {
				set(level,bb,noise(x,z)<45?Blocks.GRAVEL.defaultBlockState():Blocks.COARSE_DIRT.defaultBlockState(),x,0,z);
				set(level,bb,Blocks.DIRT.defaultBlockState(),x,-1,z);
				fill(level,bb,x,1,z,x,2,z,Blocks.AIR.defaultBlockState());
			} else {
				set(level,bb,Blocks.DIRT.defaultBlockState(),x,-1,z);
				set(level,bb,Blocks.DIRT.defaultBlockState(),x,0,z);
				if (noise(x,3,z)<55) set(level,bb,Blocks.COARSE_DIRT.defaultBlockState(),x,1,z);
			}
		}
	}

	/**
	 * The wreckage of the fight strewn over the field between the memorial ring and the wall: fallen stakes, the poles of
	 * lost standards with their rags still on them, broken shields of planks, a few skulls and the dead brush grown up since.
	 */
	private void wreckage(WorldGenLevel level,BoundingBox bb) {
		for (int x=1;x<=29;x++) for (int z=1;z<=29;z++) {
			double d=dist(x,z,15,15);
			if (d<5.5 || d>13.2 || Math.abs(x-15)<=1) continue;
			if (!getBlock(level,x,1,z,bb).isAir() || !getBlock(level,x,0,z,bb).isSolid()) continue;
			int n=noise(x,11,z);
			if (n<3) {
				Direction.Axis axis=noise(x,z)<50?Direction.Axis.X:Direction.Axis.Z;
				int dx=axis==Direction.Axis.X?1:0,dz=1-dx;
				if (getBlock(level,x+dx,1,z+dz,bb).isAir()) {
					var log=Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,axis);
					set(level,bb,log,x,1,z);set(level,bb,log,x+dx,1,z+dz);
				}
			} else if (n<5) {
				if (getBlock(level,x,2,z,bb).isAir() && getBlock(level,x,3,z,bb).isAir() && getBlock(level,x,2,z-1,bb).isAir()) {
					fill(level,bb,x,1,z,x,3,z,Blocks.DARK_OAK_FENCE.defaultBlockState());
					banner(level,bb,Blocks.WALL_BANNER.pick(noise(x,z)<50?net.minecraft.world.item.DyeColor.RED:net.minecraft.world.item.DyeColor.BLACK),x,2,z-1,Direction.SOUTH);
				}
			} else if (n<7) {
				set(level,bb,Blocks.SPRUCE_TRAPDOOR.defaultBlockState(),x,1,z);
			} else if (n<8) {
				set(level,bb,Blocks.SKELETON_SKULL.defaultBlockState().setValue(net.minecraft.world.level.block.SkullBlock.ROTATION,noise(x,z)%16),x,1,z);
			} else if (n<12) {
				set(level,bb,Blocks.DEAD_BUSH.defaultBlockState(),x,1,z);
			} else if (n<14) {
				set(level,bb,Blocks.COBBLESTONE_WALL.defaultBlockState(),x,1,z);
			}
		}
	}
	@Override public List<BoundingBox> wardedBoxes() { return List.of(); }
}
