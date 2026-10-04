package dev.wildercord.wildlife;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

/** Complete small typed pools; dense regions refuse rather than select an arbitrary prefix. */
final class WetlandQueries {
 static final int COMPLETE=12;
 record Pool<T extends Entity>(List<T> entities,int enumerated,boolean saturated){}
 static <T extends Entity> Pool<T> scan(ServerLevel world,Class<T> type,AABB box,Entity source) {
  var entries=new ArrayList<T>(COMPLETE+1);
  // Admit every typed peer before semantic eligibility. Only the exact source is excluded.
  world.getEntities(EntityTypeTest.<Entity,T>forClass(type),box,e->e!=source,entries,COMPLETE+1);
  return entries.size()>COMPLETE?new Pool<>(List.of(),entries.size(),true):new Pool<>(List.copyOf(entries),entries.size(),false);
 }
 static <T extends Entity> boolean populationRoom(ServerLevel world,Class<T> type,AABB box,int cap) {
  var pool=scan(world,type,box,null);
  return !pool.saturated()&&pool.entities().stream().filter(Entity::isAlive).count()<cap;
 }
 static boolean refugeFree(ServerLevel world,AABB box,LanternNewt source) {
  var pool=scan(world,LanternNewt.class,box,source);
  return !pool.saturated()&&pool.entities().stream().noneMatch(LanternNewt::resting);
 }
}
