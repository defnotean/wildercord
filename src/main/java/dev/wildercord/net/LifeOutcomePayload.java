package dev.wildercord.net;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.LifeOutcomes;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/** Presentation only: source is the actual cast owner, never inferred from the recipient. */
public record LifeOutcomePayload(UUID source,String dimension,String rune,LifeOutcomes.Moment moment,
                                 Vec3 anchor,Vec3 secondary,int units,double delta,long tick,Vec3 normal,double standoff) implements CustomPacketPayload {
 public static final int MAX_PIECES=128;
 public LifeOutcomePayload(UUID source,String dimension,String rune,LifeOutcomes.Moment moment,Vec3 anchor,Vec3 secondary,int units,double delta,long tick){this(source,dimension,rune,moment,anchor,secondary,units,delta,tick,new Vec3(0,0,1),0);}
 public LifeOutcomePayload {
  if(dimension==null||dimension.isEmpty()||dimension.length()>128||!dimension.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")
    ||rune==null||!LifeOutcomes.RUNES.contains(rune)||moment==null||moment==LifeOutcomes.Moment.REFUSED
    ||!coordinate(anchor)||secondary!=null&&(!coordinate(secondary)||secondary.distanceToSqr(anchor)>64*64)
    ||normal==null||!Double.isFinite(normal.lengthSqr())||Math.abs(normal.lengthSqr()-1)>1e-5||!Double.isFinite(standoff)||standoff<0||standoff>4
    ||units<0||units>6||!Double.isFinite(delta)||Math.abs(delta)>1_000_000||tick<0)
   throw new IllegalArgumentException("Invalid Life outcome payload");
 }
 public static boolean coordinate(Vec3 at){return at!=null&&Double.isFinite(at.x)&&Double.isFinite(at.y)&&Double.isFinite(at.z)
  &&Math.abs(at.x)<=30_000_000&&Math.abs(at.z)<=30_000_000&&Math.abs(at.y)<=4096;}
 public static boolean own(UUID source,UUID local){return source!=null&&source.equals(local);}
 public LifeOutcomes.Observation observation(){return new LifeOutcomes.Observation(rune,moment,anchor,secondary,units,delta,0,normal,standoff);}
 public static final Type<LifeOutcomePayload> TYPE=new Type<>(Wildercord.id("life_outcome"));
 public static final StreamCodec<RegistryFriendlyByteBuf,LifeOutcomePayload> CODEC=StreamCodec.of((b,p)->{
  b.writeBoolean(p.source!=null);if(p.source!=null){b.writeLong(p.source.getMostSignificantBits());b.writeLong(p.source.getLeastSignificantBits());}
  b.writeUtf(p.dimension,128);b.writeUtf(p.rune,64);b.writeVarInt(p.moment.ordinal());write(b,p.anchor);
  b.writeBoolean(p.secondary!=null);if(p.secondary!=null)write(b,p.secondary);
  b.writeVarInt(p.units);b.writeDouble(p.delta);b.writeLong(p.tick);write(b,p.normal);b.writeDouble(p.standoff);
 },b->{
  UUID source=b.readBoolean()?new UUID(b.readLong(),b.readLong()):null;
  String dimension=b.readUtf(128),rune=b.readUtf(64);int ordinal=b.readVarInt();
  if(ordinal<0||ordinal>=LifeOutcomes.Moment.values().length)throw new IllegalArgumentException("Invalid Life moment");
  Vec3 anchor=read(b),secondary=b.readBoolean()?read(b):null;
  return new LifeOutcomePayload(source,dimension,rune,LifeOutcomes.Moment.values()[ordinal],anchor,secondary,b.readVarInt(),b.readDouble(),b.readLong(),read(b),b.readDouble());
 });
 private static void write(RegistryFriendlyByteBuf b,Vec3 v){b.writeDouble(v.x);b.writeDouble(v.y);b.writeDouble(v.z);}
 private static Vec3 read(RegistryFriendlyByteBuf b){return new Vec3(b.readDouble(),b.readDouble(),b.readDouble());}
 @Override public Type<LifeOutcomePayload> type(){return TYPE;}
}
