package dev.wildercord.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/** A curved air current: relative quadratic control/end, bounded width and lifetime. */
public record AirflowOption(int color, Vec3 control, Vec3 end, float width, int lifetime, boolean minimal) implements ParticleOptions {
    public AirflowOption {
        if (control == null || end == null || !Double.isFinite(control.lengthSqr())
            || !Double.isFinite(end.lengthSqr()) || control.lengthSqr() > 36 || end.lengthSqr() > 36
            || !Float.isFinite(width) || width < .015F || width > .3F || lifetime < 2 || lifetime > 40)
            throw new IllegalArgumentException("Invalid airflow curve");
        color &= 0xFFFFFF;
    }
    public static final MapCodec<AirflowOption> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.INT.fieldOf("color").forGetter(AirflowOption::color),
        Vec3.CODEC.fieldOf("control").forGetter(AirflowOption::control),
        Vec3.CODEC.fieldOf("end").forGetter(AirflowOption::end),
        Codec.floatRange(.015F,.3F).fieldOf("width").forGetter(AirflowOption::width),
        Codec.intRange(2,40).fieldOf("lifetime").forGetter(AirflowOption::lifetime),
        Codec.BOOL.optionalFieldOf("minimal",false).forGetter(AirflowOption::minimal)
    ).apply(i,AirflowOption::new));
    public static final StreamCodec<RegistryFriendlyByteBuf,AirflowOption> STREAM_CODEC = StreamCodec.of(
        (b,o) -> {b.writeInt(o.color);b.writeDouble(o.control.x);b.writeDouble(o.control.y);b.writeDouble(o.control.z);
            b.writeDouble(o.end.x);b.writeDouble(o.end.y);b.writeDouble(o.end.z);b.writeFloat(o.width);b.writeVarInt(o.lifetime);b.writeBoolean(o.minimal);},
        b -> new AirflowOption(b.readInt(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),
            new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readFloat(),b.readVarInt(),b.readBoolean()));
    @Override public ParticleType<AirflowOption> getType(){return WildercordParticles.AIRFLOW;}
}
