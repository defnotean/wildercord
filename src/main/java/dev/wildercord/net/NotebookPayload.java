package dev.wildercord.net;

import dev.wildercord.Wildercord;
import dev.wildercord.player.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Small board snapshot: rune sequences remain on the server, including very large woven ids. */
public record NotebookPayload(List<Entry> builds,int shapes,int fusions,boolean garden) implements CustomPacketPayload {
 public record Entry(String name,int sockets) {}
 public NotebookPayload {builds=List.copyOf(builds);if(builds.size()>24||shapes<0||shapes>64||fusions<0||fusions>128)throw new IllegalArgumentException("Notebook bounds");}
 public static final Type<NotebookPayload> TYPE=new Type<>(Wildercord.id("notebook"));
 public static final StreamCodec<RegistryFriendlyByteBuf,NotebookPayload> CODEC=StreamCodec.of((b,p)->{
  b.writeVarInt(p.builds.size());for(var entry:p.builds){b.writeUtf(entry.name,32);b.writeVarInt(entry.sockets);}
  b.writeVarInt(p.shapes);b.writeVarInt(p.fusions);b.writeBoolean(p.garden);
 },b->{int size=b.readVarInt();if(size<0||size>24)throw new IllegalArgumentException("Notebook entries");
  var entries=new ArrayList<Entry>();for(int i=0;i<size;i++){String name=b.readUtf(32);int sockets=b.readVarInt();if(sockets<1||sockets>12)throw new IllegalArgumentException("Notebook sockets");entries.add(new Entry(name,sockets));}
  return new NotebookPayload(entries,b.readVarInt(),b.readVarInt(),b.readBoolean());
 });
 public static int open(ServerPlayer p){var n=p.getAttachedOrElse(RuneResearch.NOTES,RuneResearch.Notebook.EMPTY);
  ServerPlayNetworking.send(p,new NotebookPayload(SpellLibrary.list(p).stream().map(b->new Entry(b.name(),b.runes().size())).toList(),n.shapes().size(),n.fusions().size(),n.garden()));return 1;}
 @Override public Type<NotebookPayload> type(){return TYPE;}
}
