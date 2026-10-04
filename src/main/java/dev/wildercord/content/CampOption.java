package dev.wildercord.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/** Parchment, copper fasteners, braiding teeth and mana fibres have distinct silhouettes and bounded motion. */
public record CampOption(int style,int color,float size,int lifetime,Vec3 drift,float spin) implements ParticleOptions {
 public static final int PAPER=0,STAPLE=1,TEAR=2,FIBER=3,COMB=4,KNOT=5;
 public CampOption {
  if(style<PAPER || style>KNOT || !Float.isFinite(size) || size<.025F || size>.6F
   || lifetime<2 || lifetime>20 || drift==null || !Double.isFinite(drift.lengthSqr()) || drift.lengthSqr()>.25
   || !Float.isFinite(spin) || Math.abs(spin)>.5F)throw new IllegalArgumentException("Invalid camp material");
  color&=0xFFFFFF;
 }
 public static final MapCodec<CampOption> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
  Codec.intRange(PAPER,KNOT).fieldOf("style").forGetter(CampOption::style),
  Codec.INT.fieldOf("color").forGetter(CampOption::color),
  Codec.floatRange(.025F,.6F).fieldOf("size").forGetter(CampOption::size),
  Codec.intRange(2,20).fieldOf("lifetime").forGetter(CampOption::lifetime),
  Vec3.CODEC.fieldOf("drift").forGetter(CampOption::drift),
  Codec.floatRange(-.5F,.5F).fieldOf("spin").forGetter(CampOption::spin)
 ).apply(i,CampOption::new));
 public static final StreamCodec<RegistryFriendlyByteBuf,CampOption> STREAM_CODEC=StreamCodec.of(
  (b,o)->{b.writeVarInt(o.style);b.writeInt(o.color);b.writeFloat(o.size);b.writeVarInt(o.lifetime);
   b.writeDouble(o.drift.x);b.writeDouble(o.drift.y);b.writeDouble(o.drift.z);b.writeFloat(o.spin);},
  b->new CampOption(b.readVarInt(),b.readInt(),b.readFloat(),b.readVarInt(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readFloat()));
 @Override public ParticleType<CampOption> getType(){return WildercordParticles.CAMP;}
}
