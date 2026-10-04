package dev.wildercord.wildlife;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;
/** Private physical fan beats plus own-draw release intent; no target/catch/resource authority on the wire. */
public final class RainshieldFx {
 private RainshieldFx(){}
 public record Event(String world,UUID carrier,UUID recipient,int carrierId,int recipientId,Vec3 normal,int beat,long tick,UUID draw,long generation) implements CustomPacketPayload {
  public Event{if(generation<1||draw==null||world==null||world.length()>128||!world.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")||carrier==null||recipient==null||carrierId<0||recipientId<0||normal==null||!Double.isFinite(normal.x)||!Double.isFinite(normal.z)||normal.y!=0||normal.lengthSqr()<.99||normal.lengthSqr()>1.01||beat<0||beat>3||tick<0)throw new IllegalArgumentException("Invalid rainshield beat");}
  public static final Type<Event> TYPE=new Type<>(Wildercord.id("rainshield_beat"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Event> CODEC=StreamCodec.of((b,e)->{b.writeUtf(e.world,128);b.writeUUID(e.carrier);b.writeUUID(e.recipient);b.writeVarInt(e.carrierId);b.writeVarInt(e.recipientId);b.writeDouble(e.normal.x);b.writeDouble(e.normal.z);b.writeVarInt(e.beat);b.writeLong(e.tick);b.writeUUID(e.draw);b.writeVarLong(e.generation);},b->new Event(b.readUtf(128),b.readUUID(),b.readUUID(),b.readVarInt(),b.readVarInt(),new Vec3(b.readDouble(),0,b.readDouble()),b.readVarInt(),b.readLong(),b.readUUID(),b.readVarLong()));
  @Override public Type<Event> type(){return TYPE;}
 }
 /** No target, item, timing or price comes from the client; nonce acknowledges its own server-issued draw only. */
 public record Release(UUID draw) implements CustomPacketPayload {
  public Release{if(draw==null)throw new IllegalArgumentException("Missing draw");}
  public static final Type<Release> TYPE=new Type<>(Wildercord.id("rainshield_release"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Release> CODEC=StreamCodec.of((b,e)->b.writeUUID(e.draw),b->new Release(b.readUUID()));
  @Override public Type<Release> type(){return TYPE;}
 }
 public static void init(){PayloadTypeRegistry.clientboundPlay().register(Event.TYPE,Event.CODEC);PayloadTypeRegistry.serverboundPlay().register(Release.TYPE,Release.CODEC);ServerPlayNetworking.registerGlobalReceiver(Release.TYPE,(e,context)->RooksRainshield.releaseInput(context.player(),e.draw()));}
 static void send(ServerPlayer p,ServerPlayer r,Vec3 normal,int beat,UUID draw,long generation){if(p.level()!=r.level())return;var e=new Event(p.level().dimension().identifier().toString(),p.getUUID(),r.getUUID(),p.getId(),r.getId(),normal,beat,p.level().getGameTime(),draw,generation);if(ServerPlayNetworking.canSend(p,Event.TYPE))ServerPlayNetworking.send(p,e);if(r!=p&&ServerPlayNetworking.canSend(r,Event.TYPE))ServerPlayNetworking.send(r,e);}
}
