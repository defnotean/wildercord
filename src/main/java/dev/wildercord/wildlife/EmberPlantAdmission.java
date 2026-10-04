package dev.wildercord.wildlife;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
/** Bounded synchronous plant mutation leases; removed at scope exit, no world/entity retention. */
final class EmberPlantAdmission implements AutoCloseable {
 @FunctionalInterface interface Writer {boolean set(ServerLevel level,BlockPos at,BlockState next);}
 static final Writer WORLD=(l,p,s)->l.setBlock(p,s,net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
 private static final ThreadLocal<Set<GlobalPos>> ACTIVE=ThreadLocal.withInitial(HashSet::new);
 private final GlobalPos key;private boolean closed;
 private EmberPlantAdmission(GlobalPos key){this.key=key;}
 static EmberPlantAdmission open(ServerLevel l,BlockPos p){var cells=ACTIVE.get();var key=GlobalPos.of(l.dimension(),p.immutable());if(cells.size()>=8||!cells.add(key))return null;return new EmberPlantAdmission(key);}
 public void close(){if(closed)return;closed=true;var cells=ACTIVE.get();cells.remove(key);if(cells.isEmpty())ACTIVE.remove();}
}
