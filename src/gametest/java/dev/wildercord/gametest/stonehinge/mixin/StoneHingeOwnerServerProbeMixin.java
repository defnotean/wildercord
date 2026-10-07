package dev.wildercord.gametest.stonehinge.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.gametest.stonehinge.peer.StoneHingeNaturalMotion;
import net.minecraft.server.level.ServerPlayer;
import dev.wildercord.gametest.stonehinge.StoneHingeOwnerProbe;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.world.phys.Vec3;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class StoneHingeOwnerServerProbeMixin implements StoneHingeOwnerProbe.ConnectionState {
	@Shadow private Vec3 awaitingPositionFromClient;
	@Override public boolean stoneHinge$awaitingTeleport() { return awaitingPositionFromClient != null; }
    @WrapMethod(method = "tickPlayer()Z", require = 1, expect = 1, allow = 1)
    private boolean stoneHinge$naturalListener(Operation<Boolean> original) {
        return StoneHingeNaturalMotion.listener((ServerGamePacketListenerImpl) (Object) this, () -> original.call());
    }
    @WrapOperation(method = "tickPlayer()Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;doTick()V"), require = 1, expect = 1, allow = 1)
    private void stoneHinge$nativeStepCallsite(ServerPlayer player, Operation<Void> original) {
        StoneHingeNaturalMotion.callSite((ServerGamePacketListenerImpl) (Object) this, player, () -> original.call(player));
    }
	@WrapMethod(method = "handleMovePlayer")
	private void stoneHinge$packet(ServerboundMovePlayerPacket packet, Operation<Void> original) {
		StoneHingeOwnerProbe.ownerMovePacket((ServerGamePacketListenerImpl) (Object) this, packet, () -> original.call(packet));
	}
	@WrapMethod(method = "handlePlayerPositionChange")
	private void stoneHinge$position(double x, double y, double z, float yaw, float pitch, boolean ground, boolean horizontalCollision, Operation<Void> original) {
		StoneHingeOwnerProbe.ownerPosition(((ServerGamePacketListenerImpl) (Object) this).player, x, y, z,
			() -> original.call(x, y, z, yaw, pitch, ground, horizontalCollision));
	}
}
