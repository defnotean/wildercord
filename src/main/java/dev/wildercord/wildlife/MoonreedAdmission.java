package dev.wildercord.wildlife;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
/** Finite synchronous cell ownership; no world references survive scope exit. */
final class MoonreedAdmission implements AutoCloseable {
 @FunctionalInterface interface Writer {boolean set(ServerLevel l,BlockPos p,BlockState next);}
 static final Writer WORLD=(l,p,s)->l.setBlock(p,s,net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
 private record Key(ServerLevel level,BlockPos pos) {}
 private static final ThreadLocal<Set<Key>> ACTIVE=ThreadLocal.withInitial(HashSet::new);
 private final Key key;private boolean closed;
 private MoonreedAdmission(Key key){this.key=key;}
 static MoonreedAdmission open(ServerLevel l,BlockPos p){var cells=ACTIVE.get();var key=new Key(l,p.immutable());if(cells.size()>=8 || !cells.add(key))return null;return new MoonreedAdmission(key);}
 public void close(){if(closed)return;closed=true;var cells=ACTIVE.get();cells.remove(key);if(cells.isEmpty())ACTIVE.remove();}
}
