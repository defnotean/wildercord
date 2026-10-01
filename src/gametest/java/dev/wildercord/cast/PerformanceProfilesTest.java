package dev.wildercord.cast;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.*;
import dev.wildercord.spell.*;
import dev.wildercord.client.fx.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.util.*;
/** Reproducible 24-dummy scene. Reports measured intervals rather than claiming a preset guarantees FPS. */
public final class PerformanceProfilesTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext c){var own=MagicQuality.own;var others=MagicQuality.others;boolean flash=MagicQuality.reducedFlash,shake=MagicQuality.cameraShake;
  try(var w=c.worldBuilder().create()) {c.waitTicks(40);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);PracticeRoom.enter(p);PracticeRoom.targets(p.level(),24,true);
   Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var b=Spellbooks.get(p);for(var r:Runes.all())b=b.learn(r.id());Spellbooks.set(p,b.withSpell(0,List.of(Runes.BEAM.id(),Runes.FIRESTORM.id())));
   p.setYRot(0);p.setXRot(0);
  });c.waitTicks(60);
  c.runOnClient(mc->{mc.getWindow().setWindowed(1600,900);if(mc.gui.hud.isHidden())mc.gui.hud.toggle();mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);mc.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);});
  for(String profile:List.of("performance","balanced","cinematic")) {
   c.runOnClient(mc->{MagicQuality.preset(profile);FrameBenchmark.start();});w.getServer().runOnServer(s->VisualMetrics.reset());
   for(int i=0;i<32;i++){w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();Spellbooks.setReadyAt(p,0,0);SpellCaster.cast(p,0);});c.waitTicks(20);}
   c.runOnClient(mc->{if(FrameBenchmark.lastResult().isEmpty())throw new AssertionError("frame benchmark did not complete: "+profile);});
   w.getServer().runOnServer(s->dev.wildercord.Wildercord.LOGGER.info("Profile {}: {}",profile,VisualMetrics.report()));
  }
  } finally {MagicQuality.own=own;MagicQuality.others=others;MagicQuality.reducedFlash=flash;MagicQuality.cameraShake=shake;MagicQuality.save();}
 }
}
