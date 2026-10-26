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

/** Three authored outdoor ruins: a broken barricade, a shelter garden, and the cairns of a retreat. */
public final class OldBattlefieldPiece extends DungeonPiece {
	public OldBattlefieldPiece(int x,int y,int z,Direction facing) { super(DungeonWorldgen.BATTLEFIELD_PIECE,x,y,z,31,12,31,facing); }
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
		fill(level,bb,15,3,20,15,9,20,Blocks.POLISHED_ANDESITE.defaultBlockState());
		set(level,bb,Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),15,3,20);
		set(level,bb,Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),15,10,20);
		set(level,bb,Blocks.LANTERN.defaultBlockState(),15,11,20);
		banner(level,bb,Blocks.WALL_BANNER.pick(net.minecraft.world.item.DyeColor.RED),15,7,19,Direction.NORTH);
		weather(level,bb,13,0,18,17,2,22,Blocks.STONE_BRICKS.defaultBlockState(),Blocks.CRACKED_STONE_BRICKS.defaultBlockState(),20);
		// The graves: a low mound and a marker each, some of the markers fallen.
		for (int x:new int[]{3,5}) for (int z:new int[]{14,17,20}) {
			fill(level,bb,x,-3,z,x,-1,z,Blocks.DIRT.defaultBlockState());
			set(level,bb,Blocks.COARSE_DIRT.defaultBlockState(),x,0,z);
			set(level,bb,Blocks.COARSE_DIRT.defaultBlockState(),x,0,z+1);
			int n=noise(x,z);
			set(level,bb,(n<25?Blocks.MOSSY_COBBLESTONE_SLAB:n<60?Blocks.COBBLESTONE_WALL:Blocks.MOSSY_STONE_BRICK_WALL).defaultBlockState(),x,1,z);
		}
	}
	@Override public List<BoundingBox> wardedBoxes() { return List.of(); }
}
