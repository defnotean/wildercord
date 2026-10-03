package dev.wildercord.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/** Physical earth fragments: material silhouette, drift and angular momentum travel together. */
public record EarthOption(int style,int color,float size,int lifetime,Vec3 drift,float spin) implements ParticleOptions {
 public static final int ROCK=0,SLAB=1,GRIT=2,DUST=3,FRACTURE=4,BONE=5,ROOT=6,CRYSTAL=7;
 public EarthOption {
  if(style<ROCK || style>CRYSTAL || !Float.isFinite(size) || size<.025F || size>.6F
   || lifetime<2 || lifetime>20 || drift==null || !Double.isFinite(drift.lengthSqr()) || drift.lengthSqr()>.25
   || !Float.isFinite(spin) || Math.abs(spin)>.5F)throw new IllegalArgumentException("Invalid earth material");
  color&=0xFFFFFF;
 }
 public static final MapCodec<EarthOption> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
  Codec.intRange(ROCK,CRYSTAL).fieldOf("style").forGetter(EarthOption::style),
  Codec.INT.fieldOf("color").forGetter(EarthOption::color),
  Codec.floatRange(.025F,.6F).fieldOf("size").forGetter(EarthOption::size),
  Codec.intRange(2,20).fieldOf("lifetime").forGetter(EarthOption::lifetime),
  Vec3.CODEC.fieldOf("drift").forGetter(EarthOption::drift),
  Codec.floatRange(-.5F,.5F).fieldOf("spin").forGetter(EarthOption::spin)
 ).apply(i,EarthOption::new));
 public static final StreamCodec<RegistryFriendlyByteBuf,EarthOption> STREAM_CODEC=StreamCodec.of(
  (b,o)->{b.writeVarInt(o.style);b.writeInt(o.color);b.writeFloat(o.size);b.writeVarInt(o.lifetime);
   b.writeDouble(o.drift.x);b.writeDouble(o.drift.y);b.writeDouble(o.drift.z);b.writeFloat(o.spin);},
  b->new EarthOption(b.readVarInt(),b.readInt(),b.readFloat(),b.readVarInt(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readFloat()));
 @Override public ParticleType<EarthOption> getType(){return WildercordParticles.EARTH;}
}
