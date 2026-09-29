package dev.wildercord.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.status.ClientStatusPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.net.InetSocketAddress;

/**
 * The server list asks a server for its status with the address as typed, but joining it greets the server with the
 * address its DNS (SRV) record points at. Tunnels that sort traffic by that greeting (playit.gg's, for one) only
 * know the address they gave out, so a server reached through its own domain showed "Can't connect to server" in the
 * list while joining it worked. The list now greets a redirected server the way joining does. An address that no SRV
 * record redirects (an IP, or a name with no record) goes as typed, as before.
 */
@Mixin(ServerStatusPinger.class)
public abstract class ServerStatusPingerMixin {
	@WrapOperation(method = "pingServer", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/network/Connection;initiateServerboundStatusConnection(Ljava/lang/String;ILnet/minecraft/network/protocol/status/ClientStatusPacketListener;)V"))
	private void wildercord$greetLikeJoining(Connection connection, String host, int port, ClientStatusPacketListener listener,
			Operation<Void> original, @Local InetSocketAddress address) {
		// The name the address was resolved from (never a reverse lookup): the SRV target when a record redirected it.
		String resolved = address.getHostString();
		if (resolved != null && !resolved.isEmpty() && !resolved.equalsIgnoreCase(host) && !isIpLiteral(resolved)) {
			original.call(connection, resolved, address.getPort(), listener);
		} else {
			original.call(connection, host, port, listener);
		}
	}

	private static boolean isIpLiteral(String host) {
		return host.indexOf(':') >= 0 || host.chars().allMatch(c -> c == '.' || Character.isDigit(c));
	}
}
