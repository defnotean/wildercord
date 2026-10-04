package dev.wildercord.cast;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.*;
/** Private admitted utility events. No client packet can request a warning or move mana. */
public final class CampConcordFx {
 private CampConcordFx(){}
 public static final int ARM=0,IDLE=1,WARN=2,OBSCURED=3,END=4,OFFER=5,TRANSFER=6,DECLINE=7;
 public record Event(UUID source,UUID target,String world,Vec3 from,Vec3 to,int phase,int gain,long tick) implements CustomPacketPayload {
  public Event{if(source==null||target==null||world==null||world.length()>128||!world.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")||from==null||to==null||!valid(from)||!valid(to)||from.distanceToSqr(to)>64||phase<0||phase>7||gain<0||gain>16||(phase==TRANSFER)!=(gain>0)||phase<=END&&(!source.equals(target)||phase!=WARN&&!from.equals(to)||phase==WARN&&!cardinal(from,to))||phase>=OFFER&&source.equals(target)||tick<0)throw new IllegalArgumentException("Invalid camp-concord event");}
  private static boolean cardinal(Vec3 from,Vec3 to){var d=to.subtract(from);return d.equals(Vec3.ZERO)||d.equals(new Vec3(.5,0,0))||d.equals(new Vec3(-.5,0,0))||d.equals(new Vec3(0,0,.5))||d.equals(new Vec3(0,0,-.5));}
  private static boolean valid(Vec3 p){return Double.isFinite(p.lengthSqr())&&Math.abs(p.x)<=30_000_000&&Math.abs(p.z)<=30_000_000&&Math.abs(p.y)<=4096;}
  public static final Type<Event> TYPE=new Type<>(Wildercord.id("camp_concord"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Event> CODEC=StreamCodec.of((b,e)->{b.writeUUID(e.source);b.writeUUID(e.target);b.writeUtf(e.world,128);vec(b,e.from);vec(b,e.to);b.writeByte(e.phase);b.writeByte(e.gain);b.writeLong(e.tick);},b->new Event(b.readUUID(),b.readUUID(),b.readUtf(128),vec(b),vec(b),b.readUnsignedByte(),b.readUnsignedByte(),b.readLong()));
  private static void vec(RegistryFriendlyByteBuf b,Vec3 p){b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);}private static Vec3 vec(RegistryFriendlyByteBuf b){return new Vec3(b.readDouble(),b.readDouble(),b.readDouble());}
  @Override public Type<Event> type(){return TYPE;}
 }
 public static void init(){PayloadTypeRegistry.clientboundPlay().register(Event.TYPE,Event.CODEC);}
 private static void send(ServerPlayer p,Event e){if(!Fx.muted()&&p.isAlive()&&p.level().dimension().identifier().toString().equals(e.world)&&ServerPlayNetworking.canSend(p,Event.TYPE))ServerPlayNetworking.send(p,e);}
 static void watch(ServerPlayer p,int phase,Vec3 at){var e=new Event(p.getUUID(),p.getUUID(),p.level().dimension().identifier().toString(),at,at,phase,0,p.level().getGameTime());send(p,e);if(phase==WARN||phase==OBSCURED)p.sendOverlayMessage(Component.translatable("message.wildercord."+(phase==WARN?"watchweft.warn":"watchweft.obscured")));}
 /** The sole warning carries only one half-block cardinal tick, never hostile coordinates/identity. */
 static void warning(ServerPlayer p,Vec3 anchor,Vec3 body){var d=body.subtract(anchor);Vec3 tick=d.horizontalDistanceSqr()<.0001?Vec3.ZERO:Math.abs(d.x)>=Math.abs(d.z)?new Vec3(Math.copySign(.5,d.x),0,0):new Vec3(0,0,Math.copySign(.5,d.z));
  send(p,new Event(p.getUUID(),p.getUUID(),p.level().dimension().identifier().toString(),anchor,anchor.add(tick),WARN,0,p.level().getGameTime()));p.sendOverlayMessage(Component.translatable("message.wildercord.watchweft.warn"));}
 static void braid(ServerPlayer p,ServerPlayer t,int phase,int gain){
  // Cancellation may follow a large legitimate departure. Dispose the offer without constructing invalid geometry.
  var from=p.getBoundingBox().getCenter();var to=t.getBoundingBox().getCenter();
  if(p.level()!=t.level()||!Event.valid(from)||!Event.valid(to)||from.distanceToSqr(to)>64)return;
  var e=new Event(p.getUUID(),t.getUUID(),p.level().dimension().identifier().toString(),p.getBoundingBox().getCenter(),t.getBoundingBox().getCenter(),phase,gain,p.level().getGameTime());send(p,e);send(t,e);
  if(phase==OFFER)t.sendOverlayMessage(Component.translatable("message.wildercord.manabraid.offer"));else if(phase==TRANSFER){p.sendOverlayMessage(Component.translatable("message.wildercord.manabraid.given",gain));t.sendOverlayMessage(Component.translatable("message.wildercord.manabraid.received",gain));}
 }
}
