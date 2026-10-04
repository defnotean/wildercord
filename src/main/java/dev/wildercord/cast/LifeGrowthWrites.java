package dev.wildercord.cast;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Observes only real writes during a synchronous bonemeal operation.
 * No world-volume scan, chunk loads, writes, replacement or admission decisions.
 * The wrapped vanilla method's result and exception propagate unchanged.
 */
public final class LifeGrowthWrites implements AutoCloseable {
 private record Cell(BlockState before,boolean changed){}
 private static final ThreadLocal<LifeGrowthWrites> ACTIVE=new ThreadLocal<>();
 private final Cast cast;private final String rune;private final LifeGrowthWrites previous;
 private final Map<BlockPos,Cell> cells=new LinkedHashMap<>();
 private boolean closed;
 private LifeGrowthWrites(Cast c,String r){cast=c;rune=r;previous=ACTIVE.get();ACTIVE.set(this);}
 static LifeGrowthWrites open(Cast c,String rune){return new LifeGrowthWrites(c,rune);}
 /** A token exists only in the owning loaded server world and under the finite observation cap. */
 public static Object begin(Level l,BlockPos pos){
  var scope=ACTIVE.get();if(scope==null||l!=scope.cast.level||!scope.cast.level.isLoaded(pos))return null;
  var p=pos.immutable();if(!scope.cells.containsKey(p)){
   if(scope.cells.size()>=128)return null;scope.cells.put(p,new Cell(l.getBlockState(p),false));
  }
  return p;
 }
 public static void end(Level l,Object token,boolean succeeded){
  var scope=ACTIVE.get();if(scope==null||l!=scope.cast.level||token==null||!succeeded)return;
  var p=(BlockPos)token;var cell=scope.cells.get(p);if(cell!=null)scope.cells.put(p,new Cell(cell.before(),true));
 }
 @Override public void close(){
  if(closed)return;closed=true;ACTIVE.set(previous);if(previous==null)ACTIVE.remove();
  for(var entry:cells.entrySet())if(entry.getValue().changed()&&cast.level.isLoaded(entry.getKey())){
   var at=entry.getKey();var after=cast.level.getBlockState(at);
   LifeOwnerEvents.cell(cast,rune,at,entry.getValue().before(),after,LifeOwnerEvents.Moment.APPLY);
  }
 }
}
