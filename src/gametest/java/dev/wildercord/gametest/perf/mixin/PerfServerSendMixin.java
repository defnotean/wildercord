package dev.wildercord.gametest.perf.mixin;

import dev.wildercord.gametest.perf.PerfCounters;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts every packet the server hands a real connection (the perf harness reads it). */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class PerfServerSendMixin {
	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V", at = @At("HEAD"))
	private void perf$sent(Packet<?> packet, PacketSendListener listener, CallbackInfo ci) { PerfCounters.sent(packet, false); }
}
