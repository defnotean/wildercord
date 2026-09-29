package dev.wildercord.mixin;

import dev.wildercord.cast.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A Span's glass and a Rampart's wall are only there for a while: a piston can't push them
 * somewhere they'd no longer be taken down (and so kept). Nor can one push a warded dungeon wall.
 */
@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlockMixin {
	@Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
	private static void wildercord$keepTemporary(BlockState state, Level level, BlockPos pos, Direction direction, boolean allowDestroy,
			Direction pistonFacing, CallbackInfoReturnable<Boolean> cir) {
		if (level instanceof ServerLevel server && Effects.isTemporary(server, pos)) {
			cir.setReturnValue(false);
			return;
		}
		// Nor push a dungeon's warded walls open (what a player put there moves as usual).
		if (level instanceof ServerLevel server && !state.canBeReplaced() && dev.wildercord.world.dungeons.DungeonWards.warded(server, pos)
				&& !dev.wildercord.world.dungeons.DungeonWards.placedHere(server, pos)) {
			cir.setReturnValue(false);
		}
	}
}
