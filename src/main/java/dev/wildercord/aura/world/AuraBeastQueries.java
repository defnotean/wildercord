package dev.wildercord.aura.world;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import java.util.*;
import java.util.function.Predicate;

/** Complete candidate pool or refusal: never selects a hidden prefix in a crowded native query. */
final class AuraBeastQueries {
 private AuraBeastQueries(){}
 static <T extends Entity> List<T> complete(ServerLevel level,Class<T> type,AABB bounds,Predicate<T> eligible){return complete(level,type,bounds,null,eligible);}
 /** Exclude only this one known source identity before counting; semantic exclusions occur after raw admission. */
 static <T extends Entity> List<T> complete(ServerLevel level,Class<T> type,AABB bounds,Entity source,Predicate<T> eligible){
  var found=new ArrayList<T>(BeastRules.CANDIDATES);
  level.getEntities(EntityTypeTest.<Entity,T>forClass(type),bounds,e->e!=source,found,BeastRules.CANDIDATES);
  if(found.size()>BeastRules.VICTIMS)return List.of();
  found.removeIf(e->!eligible.test(e));return found;
 }
}
