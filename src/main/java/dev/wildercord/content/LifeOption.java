package dev.wildercord.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/** Living materials: leaf flutter, seeds, petals, sap and fibres retain their own silhouettes. */
public record LifeOption(int style,int color,float size,int lifetime,Vec3 drift,float spin) implements ParticleOptions {
 public static final int LEAF=0,SEED=1,PETAL=2,SPORE=3,VINE=4,SAP=5,TISSUE=6,THORN=7;
 public LifeOption {
  if(style<LEAF || style>THORN || !Float.isFinite(size) || size<.025F || size>.6F
   || lifetime<2 || lifetime>20 || drift==null || !Double.isFinite(drift.lengthSqr()) || drift.lengthSqr()>.25
   || !Float.isFinite(spin) || Math.abs(spin)>.5F)throw new IllegalArgumentException("Invalid life material");
  color&=0xFFFFFF;
 }
 public static final MapCodec<LifeOption> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
  Codec.intRange(LEAF,THORN).fieldOf("style").forGetter(LifeOption::style),
  Codec.INT.fieldOf("color").forGetter(LifeOption::color),
  Codec.floatRange(.025F,.6F).fieldOf("size").forGetter(LifeOption::size),
  Codec.intRange(2,20).fieldOf("lifetime").forGetter(LifeOption::lifetime),
  Vec3.CODEC.fieldOf("drift").forGetter(LifeOption::drift),
  Codec.floatRange(-.5F,.5F).fieldOf("spin").forGetter(LifeOption::spin)
 ).apply(i,LifeOption::new));
 public static final StreamCodec<RegistryFriendlyByteBuf,LifeOption> STREAM_CODEC=StreamCodec.of(
  (b,o)->{b.writeVarInt(o.style);b.writeInt(o.color);b.writeFloat(o.size);b.writeVarInt(o.lifetime);
   b.writeDouble(o.drift.x);b.writeDouble(o.drift.y);b.writeDouble(o.drift.z);b.writeFloat(o.spin);},
  b->new LifeOption(b.readVarInt(),b.readInt(),b.readFloat(),b.readVarInt(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readFloat()));
 @Override public ParticleType<LifeOption> getType(){return WildercordParticles.LIFE;}
}
