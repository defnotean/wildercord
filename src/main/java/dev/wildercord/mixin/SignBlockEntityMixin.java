package dev.wildercord.mixin;

import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * When structure pieces or features configure a sign during world generation, the block entity
 * is in a proto-chunk where {@code level} is null. Mojang calls {@code markUpdated()} unconditionally,
 * which attempts {@code this.level.sendBlockUpdated(...)} and crashes worldgen with an NPE.
 * If {@code level} is null, mark the block entity changed and return safely.
 */
@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin {
	@Inject(method = "markUpdated", at = @At("HEAD"), cancellable = true)
	private void wildercord$guardNullLevelDuringGen(CallbackInfo ci) {
		SignBlockEntity self = (SignBlockEntity) (Object) this;
		if (self.getLevel() == null) {
			self.setChanged();
			ci.cancel();
		}
	}
}
