package dev.wildercord.wildlife;
import dev.wildercord.Wildercord;
import dev.wildercord.cast.feel.Feels;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;
/** Outcome-only source-identified wire; no refused selection or stale mutation pictures. */
public final class RootCarryFx {
 private RootCarryFx(){}
 public record Event(UUID source,String dimension,BlockPos from,BlockPos to,boolean moved,long tick) implements CustomPacketPayload {
  public Event{if(source==null||dimension==null||dimension.length()>128||!dimension.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")||from==null||to==null||from.distSqr(to)>64||!moved&&!from.equals(to)||moved&&from.equals(to)||Math.abs((long)from.getX())>30_000_000||Math.abs((long)from.getZ())>30_000_000||Math.abs((long)to.getX())>30_000_000||Math.abs((long)to.getZ())>30_000_000||Math.abs((long)from.getY())>4096||Math.abs((long)to.getY())>4096||tick<0)throw new IllegalArgumentException("Invalid root transfer picture");from=from.immutable();to=to.immutable();}
  public static final Type<Event> TYPE=new Type<>(Wildercord.id("root_carry_outcome"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Event> CODEC=StreamCodec.of((b,e)->{b.writeUUID(e.source);b.writeUtf(e.dimension,128);b.writeBlockPos(e.from);b.writeBlockPos(e.to);b.writeBoolean(e.moved);b.writeLong(e.tick);},b->new Event(b.readUUID(),b.readUtf(128),b.readBlockPos(),b.readBlockPos(),b.readBoolean(),b.readLong()));
  @Override public Type<Event> type(){return TYPE;}
 }
 public static void init(){PayloadTypeRegistry.clientboundPlay().register(Event.TYPE,Event.CODEC);}
 static void send(ServerPlayer p,BlockPos from,BlockPos to,boolean moved){if(dev.wildercord.cast.Fx.muted())return;var l=p.level();var event=new Event(p.getUUID(),l.dimension().identifier().toString(),from,to,moved,l.getGameTime());for(var viewer:l.players())if(viewer.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(from))<=48*48&&ServerPlayNetworking.canSend(viewer,Event.TYPE))ServerPlayNetworking.send(viewer,event);Feels.sound(l,net.minecraft.world.phys.Vec3.atCenterOf(moved?to:from),moved?"root_carry_settle":"root_carry_select",.35F,1);}
}
