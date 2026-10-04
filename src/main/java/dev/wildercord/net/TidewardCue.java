package dev.wildercord.net;

import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.ReedbackCrab;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Private finite observations; the packet does not authorise gameplay or quest changes. */
public record TidewardCue(String world,int kind,long nonce,int entity,UUID identity,List<BlockPos> feet) implements CustomPacketPayload {
 public static final int CLEAR=0,WARNING=1,ROUTE=2;
 public TidewardCue {
  if(world==null || world.length()>128 || !world.matches("[a-z0-9_.-]+:[a-z0-9/._-]+") || kind<0 || kind>2 || nonce<0 || feet==null || feet.size()>9
   || kind==WARNING && (entity<0 || identity==null || !feet.isEmpty()) || kind==ROUTE && (feet.size()<2 || identity!=null))throw new IllegalArgumentException("Invalid Tideward cue");
  feet=feet.stream().map(BlockPos::immutable).toList();
  for(var p:feet)if(Math.abs((long)p.getX())>30_000_000 || Math.abs((long)p.getZ())>30_000_000 || Math.abs((long)p.getY())>4096)throw new IllegalArgumentException("Invalid footing coordinate");
  for(int i=1;i<feet.size();i++)if(feet.get(i).distSqr(feet.get(i-1))>3)throw new IllegalArgumentException("Disconnected route");
 }
 public static final Type<TidewardCue> TYPE=new Type<>(Wildercord.id("tideward_cue"));
 public static final StreamCodec<RegistryFriendlyByteBuf,TidewardCue> CODEC=StreamCodec.of((b,p)->{
  b.writeUtf(p.world,128);b.writeVarInt(p.kind);b.writeLong(p.nonce);b.writeVarInt(p.entity);b.writeBoolean(p.identity!=null);if(p.identity!=null)b.writeUUID(p.identity);
  b.writeVarInt(p.feet.size());for(var at:p.feet)b.writeBlockPos(at);
 },b->{
  String world=b.readUtf(128);int kind=b.readVarInt();long nonce=b.readLong();int entity=b.readVarInt();UUID id=b.readBoolean()?b.readUUID():null;int count=b.readVarInt();
  if(count<0 || count>9)throw new IllegalArgumentException("Too many route cells");var points=new ArrayList<BlockPos>(count);for(int i=0;i<count;i++)points.add(b.readBlockPos());
  return new TidewardCue(world,kind,nonce,entity,id,points);
 });
 public static void warning(ServerPlayer p,ReedbackCrab crab){ServerPlayNetworking.send(p,new TidewardCue(p.level().dimension().identifier().toString(),WARNING,p.level().getGameTime(),crab.getId(),crab.getUUID(),List.of()));}
 public static void route(ServerPlayer p,long nonce,List<BlockPos> feet){ServerPlayNetworking.send(p,new TidewardCue(p.level().dimension().identifier().toString(),ROUTE,nonce,0,null,feet));}
 public static void clear(ServerPlayer p,long nonce){ServerPlayNetworking.send(p,new TidewardCue(p.level().dimension().identifier().toString(),CLEAR,nonce,0,null,List.of()));}
 @Override public Type<TidewardCue> type(){return TYPE;}
}
