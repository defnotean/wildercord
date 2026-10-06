package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.wildercord.gametest.ArchivePlacementProbe;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.commands.PlaceCommand;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Original methods execute once, using identical arguments and results, including failures. */
@Mixin(PlaceCommand.class)
public abstract class ArchivePlacementProbeMixin {
	@WrapMethod(method = "placeStructure", require = 1, expect = 1, allow = 1)
	private static int wildercord$place(CommandSourceStack source, Holder.Reference<Structure> structure, BlockPos origin, Operation<Integer> original) throws CommandSyntaxException {
		var scope = ArchivePlacementProbe.placement(source.getLevel(), structure.key().identifier().toString(), origin);
		Integer result = null;
		try { result = original.call(source, structure, origin); return result; }
		finally { scope.finish(result, source.getLevel()); }
	}

	@WrapOperation(method = "placeStructure", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/StructureStart;isValid()Z"), require = 1, expect = 1, allow = 1)
	private static boolean wildercord$start(StructureStart start, Operation<Boolean> original) {
		boolean valid = original.call(start);
		ArchivePlacementProbe.generated(start, valid);
		return valid;
	}
}
