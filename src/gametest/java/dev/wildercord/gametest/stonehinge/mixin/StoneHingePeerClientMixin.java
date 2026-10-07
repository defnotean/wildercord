package dev.wildercord.gametest.stonehinge.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.gametest.stonehinge.peer.StoneHingePeerProbe;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.*;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(ClientPacketListener.class)
public abstract class StoneHingePeerClientMixin {
    @WrapMethod(method = "handleMoveEntity")
    private void stoneHinge$peerMove(ClientboundMoveEntityPacket packet, Operation<Void> original) { StoneHingePeerProbe.peerPacket(packet, () -> original.call(packet)); }
    @WrapMethod(method = "handleEntityPositionSync")
    private void stoneHinge$peerSync(ClientboundEntityPositionSyncPacket packet, Operation<Void> original) { StoneHingePeerProbe.peerPacket(packet, () -> original.call(packet)); }
    @WrapMethod(method = "handleTeleportEntity")
    private void stoneHinge$peerTeleport(ClientboundTeleportEntityPacket packet, Operation<Void> original) { StoneHingePeerProbe.peerPacket(packet, () -> original.call(packet)); }
}
