package dev.wildercord.gametest.stonehinge.mixin;
import io.netty.channel.ChannelFuture;
import net.minecraft.server.network.ServerConnectionListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;
/** Passive read of the actual retained OS-bound server endpoint when configured with port zero. */
@Mixin(ServerConnectionListener.class)
public interface StoneHingeServerConnectionAccess {
    @Accessor("channels") List<ChannelFuture> stoneHinge$channels();
}
