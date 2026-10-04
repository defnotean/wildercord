package dev.wildercord.cast;
import dev.wildercord.world.dungeons.DungeonWards;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.*;
/** Read-only conservative break-claim compatibility, protected by whole-player operation leases. */
final class CampConcordAdmission {
 private CampConcordAdmission(){}
 private static final ThreadLocal<Set<ServerPlayer>> OPERATING=ThreadLocal.withInitial(()->Collections.newSetFromMap(new IdentityHashMap<>()));
 static final class Lease implements AutoCloseable {
  private final List<ServerPlayer> players;private Lease(List<ServerPlayer> p){players=p;}
  static Lease open(ServerPlayer... p){var active=OPERATING.get();var list=List.of(p);if(new HashSet<>(list).size()!=list.size()||active.size()+list.size()>8||list.stream().anyMatch(active::contains))return null;active.addAll(list);return new Lease(list);}
  @Override public void close(){var active=OPERATING.get();active.removeAll(players);if(active.isEmpty())OPERATING.remove();}
 }
 static long now(ServerPlayer p){return p.level().getServer().overworld().getGameTime();}
 static boolean actor(ServerPlayer p,ServerLevel l){return p.level()==l&&p.isAlive()&&!p.isRemoved()&&!p.isSpectator()&&!p.isCreative()
  &&l.getServer().getPlayerList().getPlayer(p.getUUID())==p&&!dev.wildercord.duel.Duels.inDuel(p)&&!dev.wildercord.aura.Spars.sparring(p)
  &&!dev.wildercord.aura.world.DuelistDuels.inDuel(p)&&!DungeonWards.warded(l,p.blockPosition());}
 static boolean loaded(ServerLevel l,BlockPos at){return l.hasChunkAt(at)&&!l.isOutsideBuildHeight(at)&&l.getWorldBorder().isWithinBounds(at);}
 static boolean cells(ServerLevel l,AABB box){
  if(!Double.isFinite(box.getSize())||box.maxX-box.minX>12.01||box.maxY-box.minY>4.01||box.maxZ-box.minZ>12.01)return false;
  // Rectangle contains at most four chunks. Eight corner guards plus each actual chunk replace 845 cell reads.
  for(double x:new double[]{box.minX,box.maxX})for(double y:new double[]{box.minY,box.maxY})for(double z:new double[]{box.minZ,box.maxZ})if(!loaded(l,BlockPos.containing(x,y,z)))return false;
  int xa=BlockPos.containing(box.minX,box.minY,box.minZ).getX()>>4,xb=BlockPos.containing(box.maxX,box.minY,box.minZ).getX()>>4;
  int za=BlockPos.containing(box.minX,box.minY,box.minZ).getZ()>>4,zb=BlockPos.containing(box.minX,box.minY,box.maxZ).getZ()>>4;
  for(int x=xa;x<=xb;x++)for(int z=za;z<=zb;z++)if(!l.hasChunk(x,z))return false;return true;
 }
 static boolean ray(ServerPlayer p,ServerLevel l,Vec3 from,Vec3 to,double maximum,BlockPos last){
  var delta=to.subtract(from);double length=delta.length();if(!Double.isFinite(length)||length>maximum)return false;
  int n=Math.max(1,(int)Math.ceil(length*2));for(int i=0;i<=n;i++)if(!loaded(l,BlockPos.containing(from.add(delta.scale(i/(double)n)))))return false;
  var h=l.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));return h.getType()==HitResult.Type.MISS||last!=null&&h.getBlockPos().equals(last);
 }
 static boolean cell(ServerPlayer p,ServerLevel l,BlockPos at){return actor(p,l)&&loaded(l,at)&&l.mayInteract(p,at)&&!DungeonWards.warded(l,at)&&!Effects.isTemporary(l,at)&&l.getBlockEntity(at)==null;}
 static boolean claim(ServerPlayer p,ServerLevel l,BlockPos at){return cell(p,l,at)&&Casters.probeBreak(l,p,at,l.getBlockState(at),null);}
}
