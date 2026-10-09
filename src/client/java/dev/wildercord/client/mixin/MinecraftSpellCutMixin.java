package dev.wildercord.client.mixin;

import dev.wildercord.client.FormDashClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The learned Spell Cut answers a real swing (its packets sent), so it rides the vanilla attack binding. */
@Mixin(Minecraft.class)
public abstract class MinecraftSpellCutMixin {
	@Inject(method = "startAttack", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V", shift = At.Shift.AFTER))
	private void wildercord$spellCut(CallbackInfoReturnable<Boolean> cir) {
		FormDashClient.swing();
	}
}
