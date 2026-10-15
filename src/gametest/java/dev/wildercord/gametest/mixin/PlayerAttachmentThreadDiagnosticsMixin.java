package dev.wildercord.gametest.mixin;

import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Test-only diagnosis of attachment writes racing the server's player save. */
@Mixin(value = Entity.class, priority = 900)
public abstract class PlayerAttachmentThreadDiagnosticsMixin {
	@Inject(method = "setAttached", at = @At("HEAD"), remap = false, require = 1, allow = 1)
	private void wildercord$attachmentThread(AttachmentType<?> type, Object value, CallbackInfoReturnable<Object> cir) {
		if ((Object) this instanceof ServerPlayer player && !player.level().getServer().isSameThread()) {
			throw new AssertionError("Off-thread server-player attachment write: " + type.identifier() + " on " + Thread.currentThread().getName());
		}
	}
}
