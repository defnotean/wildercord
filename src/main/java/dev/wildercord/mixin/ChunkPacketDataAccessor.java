package dev.wildercord.mixin;

import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.Map;

/** Exposes chunk packet internals so modded sections can be translated per Bedrock recipient without mutating shared packets. */
@Mixin(ClientboundLevelChunkPacketData.class)
public interface ChunkPacketDataAccessor {
	@Accessor("heightmaps")
	Map<Heightmap.Types, long[]> wildercord$heightmaps();

	@Accessor("blockEntitiesData")
	List<?> wildercord$blockEntitiesData();

	@Invoker("<init>")
	static ClientboundLevelChunkPacketData wildercord$create(
		Map<Heightmap.Types, long[]> heightmaps,
		byte[] buffer,
		List<?> blockEntitiesData
	) {
		throw new AssertionError();
	}
}
