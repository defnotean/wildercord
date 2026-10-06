package dev.wildercord.gametest.stonehinge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.gametest.stonehinge.StoneHingeOwnerProbe;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class StoneHingeOwnerPositionSendProbeMixin {
	@WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"))
	private void stoneHinge$ownerSend(ClientPacketListener connection, Packet<?> packet, Operation<Void> original) {
		StoneHingeOwnerProbe.clientPositionSend((LocalPlayer) (Object) this, packet, () -> original.call(connection, packet));
	}
}
