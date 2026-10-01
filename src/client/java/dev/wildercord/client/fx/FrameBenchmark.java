package dev.wildercord.client.fx;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.*;
/** Frame presentation intervals over a fixed thirty-second scene; bounded samples, not a GPU/CPU profiler. */
public final class FrameBenchmark {
 private FrameBenchmark() {}
 private static final double[] SAMPLES=new double[8192];private static int count,total,throttled;private static long start,last,droppedAtStart;private static Object world;private static String lastResult="";
 public static String lastResult(){return lastResult;}
 public static void init(){HudElementRegistry.addLast(Wildercord.id("frame_benchmark"),(g,delta)->frame());}
 public static void start(){var mc=Minecraft.getInstance();if(mc.level==null)return;count=total=throttled=0;start=System.nanoTime();last=0;lastResult="";droppedAtStart=SpellFormations.dropped();world=mc.level;
  if(mc.player!=null)mc.player.sendSystemMessage(Component.translatable("message.wildercord.frame_start"));}
 private static void frame(){if(start==0)return;var mc=Minecraft.getInstance();if(mc.level!=world||mc.isPaused()){start=0;return;}
  long now=System.nanoTime();if(last==0){last=now;return;}if(mc.getFramerateLimitTracker().getThrottleReason()!=com.mojang.blaze3d.platform.FramerateLimitTracker.FramerateThrottleReason.NONE)throttled++;
  SAMPLES[total++%SAMPLES.length]=(now-last)/1e6;count=Math.min(total,SAMPLES.length);last=now;if(now-start<30_000_000_000L)return;
  start=0;double[] sorted=Arrays.copyOf(SAMPLES,count);Arrays.sort(sorted);if(count==0)return;
  String result=String.format(Locale.ROOT,"own=%s others=%s flash=%s shake=%s frames=%d samples=%d median=%.2fms p95=%.2fms p99=%.2fms formationsLimited=%d",MagicQuality.own,MagicQuality.others,MagicQuality.reducedFlash,MagicQuality.cameraShake,total,count,sorted[count/2],sorted[(int)((count-1)*.95)],sorted[(int)((count-1)*.99)],SpellFormations.dropped()-droppedAtStart);lastResult=result;
  result+=" idle/menu-throttled-frames="+throttled+" current-limit="+mc.getFramerateLimitTracker().getFramerateLimit();lastResult=result;
  Wildercord.LOGGER.info("Wildercord frame benchmark: {}",result);if(mc.player!=null)mc.player.sendSystemMessage(Component.literal(result));
  try{var path=net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("logs/wildercord-frame-benchmarks.txt");java.nio.file.Files.createDirectories(path.getParent());java.nio.file.Files.writeString(path,java.time.Instant.now()+" "+result+System.lineSeparator(),java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);}catch(java.io.IOException e){Wildercord.LOGGER.warn("Cannot write frame benchmark",e);}
 }
}
