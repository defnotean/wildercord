package dev.wildercord.cast;
import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.*;
import java.util.*;
/** Finite complete-pool camp observations, not a trap or a damage ward. */
public final class Watchweft {
 private Watchweft(){}
 public static final AttachmentType<Long> READY=AttachmentRegistry.create(Wildercord.id("watchweft_ready"),b->b.initializer(()->0L).persistent(Codec.LONG).copyOnDeath());
 private record Seen(Monster entity,boolean inside){}
 private static final class Watch {
  final UUID owner;final ServerLevel level;final BlockPos floor;final BlockState state;final long until;final Map<UUID,Seen> history=new HashMap<>();long sampled;boolean obscured;
  Watch(ServerPlayer p,BlockPos at,BlockState state,long now){owner=p.getUUID();level=p.level();floor=at.immutable();this.state=state;until=now+CampConcordRules.WATCH;sampled=now-10;}
  Vec3 surface(){return Vec3.atCenterOf(floor).add(0,.52,0);}
 }
 private static final Map<UUID,Watch> ACTIVE=new HashMap<>();
 public static int active(){return ACTIVE.size();}
 public static boolean active(UUID owner){return ACTIVE.containsKey(owner);}
 private static boolean floor(ServerPlayer p,ServerLevel l,BlockPos at,BlockState state,double range){return CampConcordAdmission.cell(p,l,at)&&l.getBlockState(at).equals(state)&&state.isFaceSturdy(l,at,Direction.UP)
  &&l.getFluidState(at).isEmpty()&&CampConcordAdmission.loaded(l,at.above())&&l.getFluidState(at.above()).isEmpty()
  &&l.getBlockState(at.above()).getCollisionShape(l,at.above()).isEmpty()&&p.getEyePosition().distanceToSqr(Vec3.atCenterOf(at))<=range*range;}
 static boolean arm(Cast c,Cast.Hit hit){
  if(!(c.caster instanceof ServerPlayer p)||c.passive||!c.alive()||hit.block()==null||hit.face()!=Direction.UP||!CampConcordAdmission.actor(p,c.level)||ACTIVE.containsKey(p.getUUID())||ACTIVE.size()>=CampConcordRules.CAP)return false;
  var at=hit.block();long now=CampConcordAdmission.now(p),stored=p.getAttachedOrElse(READY,0L),ready=CampConcordRules.ready(stored,now,CampConcordRules.WATCH_REST);
  if(now<0||now>Long.MAX_VALUE-CampConcordRules.WATCH_REST||now<ready||!CampConcordAdmission.loaded(c.level,at))return false;
  var state=c.level.getBlockState(at);var position=p.position();
  if(!floor(p,c.level,at,state,8)||!CampConcordAdmission.ray(p,c.level,p.getEyePosition(),Vec3.atCenterOf(at).add(0,.499,0),9,at))return false;
  try(var lease=CampConcordAdmission.Lease.open(p)){
   if(lease==null||!c.once("watchweft:arm")||!CampConcordAdmission.claim(p,c.level,at)||!p.position().equals(position)
    ||p.getAttachedOrElse(READY,0L)!=stored
    ||!floor(p,c.level,at,state,8)||!CampConcordAdmission.ray(p,c.level,p.getEyePosition(),Vec3.atCenterOf(at).add(0,.499,0),9,at)||ACTIVE.containsKey(p.getUUID())||ACTIVE.size()>=CampConcordRules.CAP)return false;
   var w=new Watch(p,at,state,now);if(!sample(p,w,now,false))return false;
   ACTIVE.put(p.getUUID(),w);p.setAttached(READY,now+CampConcordRules.WATCH_REST);CampConcordFx.watch(p,CampConcordFx.ARM,w.surface());return true;
  }
 }
 private static List<Monster> raw(Watch w){var list=new ArrayList<Monster>(CampConcordRules.RAW+1);var s=w.surface();w.level.getEntities(EntityTypeTest.<Entity,Monster>forClass(Monster.class),new AABB(s.x-6,s.y-1,s.z-6,s.x+6,s.y+3,s.z+6),m->true,list,CampConcordRules.RAW+1);return list;}
 private record Body(Monster entity,Vec3 position,boolean alive,boolean removed,boolean spectator,LivingEntity target){
  Body(Monster m){this(m,m.position(),m.isAlive(),m.isRemoved(),m.isSpectator(),m.getTarget());}
  boolean same(Monster m){return entity==m&&position.equals(m.position())&&alive==m.isAlive()&&removed==m.isRemoved()&&spectator==m.isSpectator()&&target==m.getTarget();}
 }
 private static boolean samePool(ServerPlayer p,Watch w,long now,Map<UUID,Body> snapshot){
  if(ACTIVE.get(w.owner)!=w||!CampConcordAdmission.actor(p,w.level)){w.history.clear();w.sampled=now;return false;}
  var s=w.surface();var box=new AABB(s.x-6,s.y-1,s.z-6,s.x+6,s.y+3,s.z+6);
  boolean loaded=CampConcordAdmission.cells(w.level,box);var fresh=loaded?raw(w):List.<Monster>of();boolean complete=loaded&&fresh.size()<=CampConcordRules.RAW;
  boolean same=complete&&fresh.size()==snapshot.size()&&fresh.stream().allMatch(m->{var before=snapshot.get(m.getUUID());return before!=null&&before.same(m);});
  // Post-callback admission is clear-only. Ordinary next sampling may display obscured after fresh owner authority.
  if(!same){w.history.clear();w.sampled=now;}return same;
 }
 private static boolean sample(ServerPlayer p,Watch w,long now,boolean trigger){
  var s=w.surface();var box=new AABB(s.x-6,s.y-1,s.z-6,s.x+6,s.y+3,s.z+6);
  if(!CampConcordAdmission.cells(w.level,box)){w.history.clear();w.sampled=now;return false;}
  var list=raw(w);if(list.size()>CampConcordRules.RAW){w.history.clear();w.sampled=now;if(trigger&&!w.obscured){w.obscured=true;CampConcordFx.watch(p,CampConcordFx.OBSCURED,s);}return false;}
  if(now-w.sampled!=10)w.history.clear();w.sampled=now;w.obscured=false;
  var snapshot=new HashMap<UUID,Body>();for(var body:list)snapshot.put(body.getUUID(),new Body(body));
  list.sort(Comparator.<Monster>comparingDouble(m->m.distanceToSqr(s)).thenComparingInt(Entity::getId));var next=new HashMap<UUID,Seen>();
  for(var m:list){boolean inside=m.position().subtract(s).horizontalDistanceSqr()<=9;var old=w.history.get(m.getUUID());next.put(m.getUUID(),new Seen(m,inside));
   if(trigger&&old!=null&&old.entity()==m&&!old.inside()&&inside&&m.isAlive()&&!m.isRemoved()&&!m.isSpectator()&&m.level()==w.level&&m.getTarget()==p&&Targets.canHarm(p,m)
    &&CampConcordAdmission.ray(p,w.level,s.add(0,.3,0),m.getBoundingBox().getCenter(),8,null)){
    var original=p.position();var target=m.position();
    try(var lease=CampConcordAdmission.Lease.open(p)){
     if(lease==null)continue;boolean permitted=CampConcordAdmission.claim(p,w.level,w.floor);
     // Re-admit the complete raw pool after external protection callbacks; never reuse its stale prefix.
     if(!samePool(p,w,now,snapshot))return false;
     if(permitted&&ACTIVE.get(p.getUUID())==w&&p.position().equals(original)&&m.position().equals(target)
      &&floor(p,w.level,w.floor,w.state,17)&&m.isAlive()&&!m.isRemoved()&&m.level()==w.level&&m.getTarget()==p&&Targets.canHarm(p,m)
      &&CampConcordAdmission.ray(p,w.level,p.getEyePosition(),s,17,w.floor)&&CampConcordAdmission.ray(p,w.level,s.add(0,.3,0),m.getBoundingBox().getCenter(),8,null)){
      ACTIVE.remove(p.getUUID(),w);w.history.clear();CampConcordFx.warning(p,s,target);return true;
     }
     // One protection admission per sample. A refused or changed candidate cannot retain a stale crossing.
     w.history.clear();w.sampled=now;return false;
    }
   }
  }w.history.clear();w.history.putAll(next);return true;
 }
 static void tick(MinecraftServer server){for(var w:List.copyOf(ACTIVE.values())){var p=server.getPlayerList().getPlayer(w.owner);if(ACTIVE.get(w.owner)!=w)continue;
  if(p==null||!CampConcordAdmission.actor(p,w.level)||CampConcordAdmission.now(p)>=w.until||p.position().distanceToSqr(w.surface())>256||!floor(p,w.level,w.floor,w.state,18)){remove(w.owner,p!=null);continue;}
  long now=CampConcordAdmission.now(p);if(now-w.sampled>=10)sample(p,w,now,true);
  // Protection callbacks may synchronously depart or remove this owner. Never frame old geometry in a new world.
  if(ACTIVE.get(w.owner)!=w)continue;
  if(!CampConcordAdmission.actor(p,w.level)||CampConcordAdmission.now(p)>=w.until||p.position().distanceToSqr(w.surface())>256||!floor(p,w.level,w.floor,w.state,18)){remove(w.owner,false);continue;}
  if(now%40==0)CampConcordFx.watch(p,CampConcordFx.IDLE,w.surface());
 }}
 static void remove(UUID owner,boolean picture){var w=ACTIVE.remove(owner);if(w==null)return;w.history.clear();var p=w.level.getServer().getPlayerList().getPlayer(owner);if(picture&&p!=null&&p.level()==w.level)CampConcordFx.watch(p,CampConcordFx.END,w.surface());}
 static void clear(){ACTIVE.values().forEach(w->w.history.clear());ACTIVE.clear();}
}
