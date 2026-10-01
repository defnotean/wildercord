package dev.wildercord.cast.events;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.saveddata.*;
import java.util.*;

/** Ten-minute echoes invite players back after an event. Saved data; no altered terrain or chunk tickets. */
public final class EventAftermath extends SavedData {
 public record Echo(String dimension,BlockPos pos,String kind,long until,List<String> claimed) {
  public Echo{pos=pos.immutable();claimed=List.copyOf(claimed);}
  static final Codec<Echo> CODEC=RecordCodecBuilder.create(i->i.group(Codec.STRING.fieldOf("dimension").forGetter(Echo::dimension),BlockPos.CODEC.fieldOf("pos").forGetter(Echo::pos),Codec.STRING.fieldOf("kind").forGetter(Echo::kind),Codec.LONG.fieldOf("until").forGetter(Echo::until),Codec.STRING.listOf(0,128).fieldOf("claimed").forGetter(Echo::claimed)).apply(i,Echo::new));
 }
 private final List<Echo> echoes=new ArrayList<>();
 static final Codec<EventAftermath> CODEC=Echo.CODEC.listOf(0,32).xmap(EventAftermath::new,a->List.copyOf(a.echoes));
 static final SavedDataType<EventAftermath> TYPE=new SavedDataType<>(Wildercord.id("event_aftermath"),EventAftermath::new,CODEC,null);
 public EventAftermath(){}private EventAftermath(List<Echo> list){echoes.addAll(list);}
 public static EventAftermath of(MinecraftServer s){return s.overworld().getDataStorage().computeIfAbsent(TYPE);}
 public List<Echo> echoes(){return List.copyOf(echoes);}
 public static void leave(ServerLevel level,BlockPos pos,String kind){
  var a=of(level.getServer());a.echoes.removeIf(e->e.until<=level.getGameTime());
  if(a.echoes.size()>=32)a.echoes.removeFirst();
  a.echoes.add(new Echo(level.dimension().identifier().toString(),pos,kind,level.getGameTime()+12000,List.of()));a.setDirty();
 }
 public static void init(){ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%40!=0)return;var a=of(server);long now=server.overworld().getGameTime();
  if(a.echoes.removeIf(e->e.until<=now))a.setDirty();
  for(var level:server.getAllLevels())for(int i=0;i<a.echoes.size();i++) {
   var e=a.echoes.get(i);if(!e.dimension.equals(level.dimension().identifier().toString())||!level.hasChunkAt(e.pos))continue;
   var nearby=level.players().stream().filter(p->!p.isSpectator()&&p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(e.pos))<24*24).toList();if(nearby.isEmpty())continue;
   dev.wildercord.cast.Fx.sendParticles(level, e.kind.equals("star")?ParticleTypes.END_ROD:e.kind.equals("rift")?ParticleTypes.PORTAL:ParticleTypes.ENCHANT,e.pos.getX()+.5,e.pos.getY()+1,e.pos.getZ()+.5,6,.7,.5,.7,.01);
   for(var p:nearby)if(p.isShiftKeyDown()&&p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(e.pos))<16)a.claim(p,i);
  }
 });}
 /** Crouching near an echo discovers one temporary boon per player, with no item farming. */
 public boolean claim(ServerPlayer p,int index) {
  if(index<0||index>=echoes.size())return false;var e=echoes.get(index);
  if(e.until<=p.level().getGameTime()||!e.dimension.equals(p.level().dimension().identifier().toString())||p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(e.pos))>=16||e.claimed.contains(p.getStringUUID())||e.claimed.size()>=128)return false;
  var ids=new ArrayList<>(e.claimed);ids.add(p.getStringUUID());echoes.set(index,new Echo(e.dimension,e.pos,e.kind,e.until,ids));setDirty();
  if(e.kind.equals("star")){p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,1200,0));p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,200,0));}
  else if(e.kind.equals("rift"))p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,400,0));
  else p.addEffect(new MobEffectInstance(MobEffects.REGENERATION,200,0));
  p.sendSystemMessage(Component.translatable("message.wildercord.aftermath",Component.translatable("aftermath.wildercord."+e.kind)));return true;
 }
}
