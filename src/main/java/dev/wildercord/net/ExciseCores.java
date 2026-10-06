package dev.wildercord.net;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.NativeZoneEmitters;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/** Bounded cosmetic snapshot, including non-player hostile casters. Never an authorization token. */
public record ExciseCores(List<Core> cores) implements CustomPacketPayload {
    public ExciseCores { cores = List.copyOf(cores); if (cores.size() > NativeZoneEmitters.MAX_VISIBLE) throw new IllegalArgumentException("Too many cores"); }
    public record Core(long id, Vec3 at, long remaining) {}
    public static final Type<ExciseCores> TYPE = new Type<>(Wildercord.id("excise_cores"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExciseCores> CODEC = StreamCodec.of((b,p) -> {
        b.writeVarInt(p.cores.size());
        for (Core c : p.cores) { b.writeLong(c.id); b.writeDouble(c.at.x); b.writeDouble(c.at.y); b.writeDouble(c.at.z); b.writeLong(c.remaining); }
    }, b -> {
        int count = b.readVarInt();
        if (count < 0 || count > NativeZoneEmitters.MAX_VISIBLE) throw new IllegalArgumentException("Too many cores");
        List<Core> cores = new ArrayList<>();
        for (int i = 0; i < count; i++) cores.add(new Core(b.readLong(), new Vec3(b.readDouble(), b.readDouble(), b.readDouble()), b.readLong()));
        return new ExciseCores(cores);
    });
    @Override public Type<ExciseCores> type() { return TYPE; }
    public static void init() { PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC); }
}
