package dev.wildercord.mixin;

import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the BlockEntityType on package-private chunk block entity entries. */
@Mixin(targets = "net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData$BlockEntityInfo")
public interface BlockEntityInfoAccessor {
	@Accessor("type")
	BlockEntityType<?> wildercord$type();
}
