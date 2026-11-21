package dev.wildercord.gametest.stonehinge.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.wildercord.gametest.stonehinge.peer.StoneHingeNaturalMotion;
import net.minecraft.server.level.ServerPlayer;
import dev.wildercord.gametest.stonehinge.StoneHingeOwnerProbe;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.phys.Vec3;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class StoneHingeOwnerServerProbeMixin implements StoneHingeOwnerProbe.ConnectionState {
	@Shadow private Vec3 awaitingPositionFromClient;
	@Override public boolean stoneHinge$awaitingTeleport() { return awaitingPositionFromClient != null; }
    @WrapMethod(method = "tick()V", require = 1, expect = 1, allow = 1)
    private void stoneHinge$naturalListener(Operation<Void> original) {
        StoneHingeNaturalMotion.listener((ServerGamePacketListenerImpl) (Object) this, () -> {
            original.call();
            return true;
        });
    }
    @WrapOperation(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;doTick()V"), require = 1, expect = 1, allow = 1)
    private void stoneHinge$nativeStepCallsite(ServerPlayer player, Operation<Void> original) {
        StoneHingeNaturalMotion.callSite((ServerGamePacketListenerImpl) (Object) this, player, () -> original.call(player));
    }
	@WrapMethod(method = "handleMovePlayer")
	private void stoneHinge$packet(ServerboundMovePlayerPacket packet, Operation<Void> original) {
		StoneHingeOwnerProbe.ownerMovePacket((ServerGamePacketListenerImpl) (Object) this, packet, () -> original.call(packet));
	}
	@Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isPassenger()Z", shift = At.Shift.AFTER))
	private void stoneHinge$beginPosition(ServerboundMovePlayerPacket packet, CallbackInfo ci,
			@Local(ordinal = 0) double x, @Local(ordinal = 1) double y, @Local(ordinal = 2) double z,
			@Share("positionFinish") LocalRef<Runnable> finish) {
		ServerPlayer player = ((ServerGamePacketListenerImpl) (Object) this).player;
		if (!player.isPassenger()) {
			finish.set(StoneHingeOwnerProbe.beginOwnerPosition(player, x, y, z));
		}
	}
	@Inject(method = "handleMovePlayer", at = @At("RETURN"))
	private void stoneHinge$endPosition(ServerboundMovePlayerPacket packet, CallbackInfo ci,
			@Share("positionFinish") LocalRef<Runnable> finish) {
		Runnable done = finish.get();
		if (done != null) {
			finish.set(null);
			done.run();
		}
	}
}
