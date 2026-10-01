package dev.wildercord.cast;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.WeakHashMap;

/** A per-recipient budget for secondary motes only. Shaped spell light and warning circles bypass it. */
final class DecorationBudget {
 private static final class Window { long tick = Long.MIN_VALUE; int used; }
 private static final Map<ServerPlayer, Window> WINDOWS = new WeakHashMap<>();
 private DecorationBudget() {}
 static boolean accept(ServerPlayer player, int particles) {
  var window = WINDOWS.computeIfAbsent(player, p -> new Window());
  long tick = player.level().getServer().getTickCount();
  if (window.tick != tick) { window.tick = tick; window.used = 0; }
  int weight = Math.clamp(particles, 1, 128);
  if (window.used + weight > 512) { VisualMetrics.dropped(); return false; }
  window.used += weight;
  return true;
 }
}
