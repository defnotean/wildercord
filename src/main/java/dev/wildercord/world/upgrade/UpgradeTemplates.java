package dev.wildercord.world.upgrade;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** One inert building only. Dungeon post-processing, entities, loot and terrain clearing are intentionally absent. */
public final class UpgradeTemplates {
	private UpgradeTemplates() {}
	public static final String FAMILY="wildercord:wayfarer_training_pavilion";
	public static final int VERSION=1, PAVILION_WRITES=123, GUARD_CELLS=729;
	private static final Set<Block> NATURAL_FLOOR=Set.of(Blocks.GRASS_BLOCK,Blocks.DIRT,Blocks.COARSE_DIRT,Blocks.PODZOL,
		Blocks.STONE,Blocks.GRANITE,Blocks.ANDESITE,Blocks.DIORITE,Blocks.DEEPSLATE,Blocks.MOSS_BLOCK);

	/** One bounded resident-chunk snapshot; historical provenance is UNKNOWN even on this narrow allowlist. */
	public static UpgradePlan preview(ServerLevel level,int chunkX,int y,int chunkZ) {
		var chunk=level.getChunkSource().getChunkNow(chunkX,chunkZ);
		if(chunk==null)throw new IllegalStateException("Chunk is not resident; it will not be loaded for this command");
		int cx=Math.addExact(Math.multiplyExact(chunkX,16),8),cz=Math.addExact(Math.multiplyExact(chunkZ,16),8);
		var cells=new ArrayList<UpgradePlan.Cell>();
		for(int dy=-1;dy<=7;dy++)for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++) {
			var pos=new BlockPos(cx+dx,y+dy,cz+dz);
			if(level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos))throw new IllegalStateException("Outside build height or world border");
			BlockState old=chunk.getBlockState(pos);
			if(old.hasBlockEntity() || !old.getFluidState().isEmpty())throw new IllegalStateException("Guard contains block entity or fluid");
			if(dy==-1?!NATURAL_FLOOR.contains(old.getBlock()):!old.is(Blocks.AIR))
				throw new IllegalStateException("Requires flat natural foundation and completely empty 9×9×8 headroom; custom blocks/redstone/builds are excluded");
			BlockState after=desired(dx,dy,dz);
			cells.add(new UpgradePlan.Cell(new UpgradePlan.Point(pos.getX(),pos.getY(),pos.getZ()),
				BlockStateParser.serialize(old),BlockStateParser.serialize(after==null?old:after)));
		}
		var plan=new UpgradePlan(level.getSeed(),level.dimension().identifier().toString(),FAMILY,VERSION,chunkX,chunkZ,cells);
		if(plan.writes().size()!=PAVILION_WRITES || plan.cells().size()!=GUARD_CELLS)throw new IllegalStateException("Unexpected template budget");
		return plan;
	}
	static BlockState desired(int x,int y,int z) {
		if(y==0 && Math.abs(x)<=3 && Math.abs(z)<=3)return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
		if(y>=1 && y<=4 && Math.abs(x)==2 && Math.abs(z)==2)return Blocks.STRIPPED_OAK_LOG.defaultBlockState();
		if(y==5 && Math.abs(x)<=3 && Math.abs(z)<=3)return (Math.abs(x)==3 || Math.abs(z)==3?Blocks.STONE_BRICKS:Blocks.SMOOTH_STONE).defaultBlockState();
		if(y==6 && Math.abs(x)<=1 && Math.abs(z)<=1)return Blocks.SMOOTH_STONE.defaultBlockState();
		return null;
	}
}
