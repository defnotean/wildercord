package dev.wildercord.gametest.stonehinge.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.gametest.stonehinge.StoneHingeOwnerProbe;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientPacketListener.class)
public abstract class StoneHingeOwnerClientProbeMixin {
	@WrapMethod(method = "handleSetEntityMotion")
	private void stoneHinge$motion(ClientboundSetEntityMotionPacket packet, Operation<Void> original) {
		StoneHingeOwnerProbe.clientMotion(packet, () -> original.call(packet));
	}
	@WrapMethod(method = "handleMovePlayer")
	private void stoneHinge$correction(ClientboundPlayerPositionPacket packet, Operation<Void> original) {
		StoneHingeOwnerProbe.clientCorrection(packet, () -> original.call(packet));
	}
	@WrapMethod(method = "handleTeleportEntity")
	private void stoneHinge$entityCorrection(ClientboundTeleportEntityPacket packet, Operation<Void> original) {
		StoneHingeOwnerProbe.clientCorrection(packet, () -> original.call(packet));
	}
}
