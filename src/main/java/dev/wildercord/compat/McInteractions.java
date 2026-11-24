package dev.wildercord.compat;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;

/** 1.21.1 expects ItemInteractionResult from block item interactions; the shared helpers still return InteractionResult. */
public final class McInteractions {
	private McInteractions() {}

	public static ItemInteractionResult itemResult(InteractionResult result) {
		return switch (result) {
			case SUCCESS, SUCCESS_NO_ITEM_USED -> ItemInteractionResult.SUCCESS;
			case CONSUME -> ItemInteractionResult.CONSUME;
			case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
			case FAIL -> ItemInteractionResult.FAIL;
			default -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		};
	}
}
