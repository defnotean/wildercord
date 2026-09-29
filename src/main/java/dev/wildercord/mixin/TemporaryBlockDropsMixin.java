package dev.wildercord.mixin;

import dev.wildercord.cast.TemporaryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * A spell's block that is only there for a while (a Rampart's wall, a Span's glass, Light's light, frost's
 * crust on lava) drops nothing, however it's taken down: every block's drops are worked out here, whether a
 * player, a piston, an explosion or a Wither breaks it.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class TemporaryBlockDropsMixin {
	@Inject(method = "getDrops", at = @At("HEAD"), cancellable = true)
	private void wildercord$nothingFromSpells(LootParams.Builder params, CallbackInfoReturnable<List<ItemStack>> cir) {
		Vec3 origin = params.getOptionalParameter(LootContextParams.ORIGIN);
		if (origin != null && TemporaryBlocks.holds(params.getLevel(), BlockPos.containing(origin), (BlockState) (Object) this)) {
			cir.setReturnValue(new ArrayList<>());
		}
	}
}
