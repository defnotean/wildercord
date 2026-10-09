package dev.wildercord.gametest.perf.mixin;

import dev.wildercord.gametest.perf.PerfCounters;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fabric's fake players drop what they're sent in their own override; count it as a real client would receive it. */
@Mixin(targets = "net.fabricmc.fabric.impl.event.interaction.FakePlayerPacketListener", remap = false)
public abstract class PerfFakeSendMixin {
	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"), remap = false)
	private void perf$sent(Packet<?> packet, ChannelFutureListener listener, CallbackInfo ci) { PerfCounters.sent(packet, true); }
}
