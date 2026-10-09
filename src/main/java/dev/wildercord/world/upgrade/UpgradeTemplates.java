package dev.wildercord.world.upgrade;

import dev.wildercord.aura.world.BattlefieldMemorial;
import dev.wildercord.aura.world.Battlefields;
import dev.wildercord.aura.world.SleepingBlades;
import dev.wildercord.aura.world.SwordTombs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Reviewed bounded blueprints only. Dungeon post-processing, entities, loot and terrain clearing are intentionally absent. */
public final class UpgradeTemplates {
	private UpgradeTemplates() {}
	public static final String FAMILY="wildercord:wayfarer_training_pavilion";
	public static final int VERSION=1, PAVILION_WRITES=123, GUARD_CELLS=729;
	private static final Set<Block> NATURAL_FLOOR=Set.of(Blocks.GRASS_BLOCK,Blocks.DIRT,Blocks.COARSE_DIRT,Blocks.PODZOL,
		Blocks.STONE,Blocks.GRANITE,Blocks.ANDESITE,Blocks.DIORITE,Blocks.DEEPSLATE,Blocks.MOSS_BLOCK);

	public static UpgradePlan preview(ServerLevel level,int chunkX,int y,int chunkZ){return preview(level,FAMILY,chunkX,y,chunkZ);}
	/** One bounded resident-chunk snapshot; historical provenance is UNKNOWN even on this narrow allowlist. */
	public static UpgradePlan preview(ServerLevel level,String family,int chunkX,int y,int chunkZ) {
		var blueprint=UpgradeBlueprints.of(family).orElseThrow(()->new IllegalArgumentException("No individually reviewed adapter for "+family));
		var chunk=level.getChunkSource().getChunkNow(chunkX,chunkZ);
		if(chunk==null)throw new IllegalStateException("Chunk is not resident; it will not be loaded for this command");
		int cx=Math.addExact(Math.multiplyExact(chunkX,16),8),cz=Math.addExact(Math.multiplyExact(chunkZ,16),8),r=blueprint.radius();
		var cells=new ArrayList<UpgradePlan.Cell>();
		for(int dy=-1;dy<=blueprint.top();dy++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++) {
			var pos=new BlockPos(cx+dx,y+dy,cz+dz);
			if(level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos))throw new IllegalStateException("Outside build height or world border");
			BlockState old=chunk.getBlockState(pos);
			if(old.hasBlockEntity() || !old.getFluidState().isEmpty())throw new IllegalStateException("Guard contains block entity or fluid");
			if(dy==-1?!NATURAL_FLOOR.contains(old.getBlock()):!old.is(Blocks.AIR))
				throw new IllegalStateException("Requires flat natural foundation and completely empty headroom; custom blocks/redstone/builds are excluded");
			BlockState after=state(family,blueprint.at(dx,dy,dz),chunkX,chunkZ);
			cells.add(new UpgradePlan.Cell(new UpgradePlan.Point(pos.getX(),pos.getY(),pos.getZ()),
				BlockStateParser.serialize(old),BlockStateParser.serialize(after==null?old:after)));
		}
		var plan=new UpgradePlan(level.getSeed(),level.dimension().identifier().toString(),family,blueprint.version(),chunkX,chunkZ,cells);
		if(plan.writes().size()!=blueprint.writes() || plan.cells().size()!=blueprint.cells())throw new IllegalStateException("Unexpected template budget");
		return plan;
	}
	/** World position of the family anchor for a plan, or null for inert families. Cells start at the foundation layer. */
	static BlockPos anchor(UpgradePlan plan) {
		var b=UpgradeBlueprints.of(plan.family()).orElse(null);if(b==null || b.anchor()==null)return null;
		int floor=plan.cells().getFirst().point().y()+1;
		return new BlockPos(plan.chunkX()*16+8+b.anchor().x(),floor+b.anchor().y(),plan.chunkZ()*16+8+b.anchor().z());
	}
	static BlockState state(String family,char code,int chunkX,int chunkZ) {
		return switch(code) {
			case 'M'->Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
			case 'L'->Blocks.STRIPPED_OAK_LOG.defaultBlockState();
			case 'B'->Blocks.STONE_BRICKS.defaultBlockState();
			case 'S'->Blocks.SMOOTH_STONE.defaultBlockState();
			case 'C'->Blocks.MOSSY_COBBLESTONE.defaultBlockState();
			case 'K'->Blocks.COBBLESTONE.defaultBlockState();
			case 'D'->Blocks.COARSE_DIRT.defaultBlockState();
			case 'A'->switch(family) {
				case UpgradeCatalog.SLEEPING_BLADE->SleepingBlades.STONE.defaultBlockState();
				case UpgradeCatalog.BATTLEFIELD->Battlefields.MEMORIAL.defaultBlockState().setValue(BattlefieldMemorial.KIND,Math.floorMod(chunkX*31+chunkZ,3));
				case UpgradeCatalog.SWORD_TOMB->SwordTombs.RELIQUARY.defaultBlockState();
				default->throw new IllegalArgumentException("Inert family has no anchor");
			};
			default->null;
		};
	}
}
