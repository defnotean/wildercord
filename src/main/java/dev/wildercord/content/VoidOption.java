package dev.wildercord.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/** Bounded opaque void fragments: torn space, shell, sculk pressure and remnants retain material motion. */
public record VoidOption(int style,int color,float size,int lifetime,Vec3 drift,float spin) implements ParticleOptions {
 public static final int FOLD=0,JAW=1,TOOTH=2,CLOTH=3,HAZE=4,SHARD=5,SCULK=6,PRESSURE=7,SHELL=8,REMNANT=9,HOUND=10;
 public VoidOption {
  if(style<FOLD || style>HOUND || !Float.isFinite(size) || size<.025F || size>.6F
   || lifetime<2 || lifetime>20 || drift==null || !Double.isFinite(drift.lengthSqr()) || drift.lengthSqr()>.25
   || !Float.isFinite(spin) || Math.abs(spin)>.5F)throw new IllegalArgumentException("Invalid void material");
  color&=0xFFFFFF;
 }
 public static final MapCodec<VoidOption> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
  Codec.intRange(FOLD,HOUND).fieldOf("style").forGetter(VoidOption::style),
  Codec.INT.fieldOf("color").forGetter(VoidOption::color),
  Codec.floatRange(.025F,.6F).fieldOf("size").forGetter(VoidOption::size),
  Codec.intRange(2,20).fieldOf("lifetime").forGetter(VoidOption::lifetime),
  Vec3.CODEC.fieldOf("drift").forGetter(VoidOption::drift),
  Codec.floatRange(-.5F,.5F).fieldOf("spin").forGetter(VoidOption::spin)
 ).apply(i,VoidOption::new));
 public static final StreamCodec<RegistryFriendlyByteBuf,VoidOption> STREAM_CODEC=StreamCodec.of(
  (b,o)->{b.writeVarInt(o.style);b.writeInt(o.color);b.writeFloat(o.size);b.writeVarInt(o.lifetime);
   b.writeDouble(o.drift.x);b.writeDouble(o.drift.y);b.writeDouble(o.drift.z);b.writeFloat(o.spin);},
  b->new VoidOption(b.readVarInt(),b.readInt(),b.readFloat(),b.readVarInt(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readFloat()));
 @Override public ParticleType<VoidOption> getType(){return WildercordParticles.VOID_MATERIAL;}
}
