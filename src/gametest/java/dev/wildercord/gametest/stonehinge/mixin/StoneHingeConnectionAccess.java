package dev.wildercord.gametest.stonehinge.mixin;
import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Passive endpoint identity only; no channel mutation. */
@Mixin(Connection.class)
public interface StoneHingeConnectionAccess {
    @Accessor("channel") Channel stoneHinge$channel();
}
