package dev.wildercord.wildlife;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;
/** Private cosmetic admission; it never authorizes feeding, effect mutation or equipment progress. */
public record MossveilFilterCue(String world,long tick,UUID wearer,int pet,UUID source,int trimmed)implements CustomPacketPayload{
 public MossveilFilterCue{if(world==null||world.length()>128||!world.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")||tick<0||wearer==null||pet<0||source==null||trimmed<1||trimmed>MossveilCowl.MAX_TRIM)throw new IllegalArgumentException("Invalid Mossveil filter cue");}
 public static final Type<MossveilFilterCue> TYPE=new Type<>(Wildercord.id("mossveil_filter"));
 public static final StreamCodec<RegistryFriendlyByteBuf,MossveilFilterCue> CODEC=StreamCodec.of((b,e)->{b.writeUtf(e.world,128);b.writeLong(e.tick);b.writeUUID(e.wearer);b.writeVarInt(e.pet);b.writeUUID(e.source);b.writeVarInt(e.trimmed);},b->new MossveilFilterCue(b.readUtf(128),b.readLong(),b.readUUID(),b.readVarInt(),b.readUUID(),b.readVarInt()));
 public Type<MossveilFilterCue> type(){return TYPE;}
 public static void init(){PayloadTypeRegistry.clientboundPlay().register(TYPE,CODEC);}
 static void send(ServerPlayer p,MossveilDormouse pet,int trimmed){ServerPlayNetworking.send(p,new MossveilFilterCue(p.level().dimension().identifier().toString(),p.level().getGameTime(),p.getUUID(),pet.getId(),pet.getUUID(),trimmed));}
}
