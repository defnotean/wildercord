package dev.wildercord.gametest.stonehinge.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.gametest.stonehinge.peer.StoneHingePeerProbe;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class StoneHingePeerSendMixin {
    @WrapMethod(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V")
    private void stoneHinge$peerSend(Packet<?> packet, PacketSendListener listener, Operation<Void> original) {
        if ((Object) this instanceof ServerGamePacketListenerImpl handler)
            StoneHingePeerProbe.serverSend(handler.player, packet, () -> original.call(packet, listener));
        else original.call(packet, listener);
    }
}
