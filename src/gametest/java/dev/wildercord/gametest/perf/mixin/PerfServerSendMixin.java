package dev.wildercord.gametest.perf.mixin;

import dev.wildercord.gametest.perf.PerfCounters;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts every packet the server hands a real connection (the perf harness reads it). */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class PerfServerSendMixin {
	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"))
	private void perf$sent(Packet<?> packet, ChannelFutureListener listener, CallbackInfo ci) { PerfCounters.sent(packet, false); }
}
