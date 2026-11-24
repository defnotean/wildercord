package dev.wildercord.mixin;

import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes section block update arrays so modded blocks in batch updates can be translated for Bedrock players. */
@Mixin(ClientboundSectionBlocksUpdatePacket.class)
public interface SectionBlocksUpdateAccessor {
	@Accessor("sectionPos")
	SectionPos wildercord$sectionPos();

	@Accessor("positions")
	short[] wildercord$positions();

	@Accessor("states")
	BlockState[] wildercord$states();
}
