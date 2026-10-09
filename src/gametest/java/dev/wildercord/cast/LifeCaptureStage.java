package dev.wildercord.cast;

import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.clock.ClockState;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Disposable paired-capture scenery only. No light-engine overrides or combat-clock changes. */
final class LifeCaptureStage {
 static final int FLOOR_Y=220,TOP_Y=FLOOR_Y+7,READY_TICKS=120;
 static final Vec3 OWNER=new Vec3(.5,FLOOR_Y+1,.5),VIEWER=new Vec3(3.5,FLOOR_Y+1,3.5);
 private static final BlockPos MIN=new BlockPos(-6,FLOOR_Y,-6),MAX=new BlockPos(6,TOP_Y,8);
 private static final List<String> WEATHER_FIELDS=List.of("oRainLevel","rainLevel","oThunderLevel","thunderLevel");

 static void build(ServerLevel level){
  check(level.isInsideBuildHeight(MIN)&&level.isInsideBuildHeight(MAX)&&resident(level),
   "Life scenery stays inside already resident native world bounds; no chunk forcing");
  for(var pos:BlockPos.betweenClosed(MIN.above(),MAX))level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
  for(var pos:BlockPos.betweenClosed(MIN,new BlockPos(MAX.getX(),FLOOR_Y,MAX.getZ())))
   level.setBlockAndUpdate(pos,Blocks.POLISHED_DEEPSLATE.defaultBlockState());
 }

 static void rejectPreviousStage(ClientGameTestContext c,UUID source){
  c.runOnClient(mc->{
   var actor=mc.level.getPlayerByUUID(source);
   check(actor!=null&&!at(actor.position(),OWNER)&&problem(mc,source,true)!=null,
    "Native negative control: the actual previous fixture body cannot acknowledge Life capture readiness");
  });
 }

 static void await(ClientGameTestContext c,UUID source,boolean owner){
  // Run before either sampler is armed. Neither the readiness budget nor a stale overlay can
  // consume the original paid material's ten/eight ticks or hide new cast feedback.
  c.waitFor(mc->problem(mc,source,owner)==null,READY_TICKS);
  c.runOnClient(mc->{
   check(problem(mc,source,owner)==null,"Both native bodies, stage geometry and daylight are ready before Life arming");
   check(!daylight(mc.level,new BlockPos(0,FLOOR_Y-1,0)),
    "Native negative control: the resident opaque stage roof refuses daylight readiness below it");
   var actor=mc.level.getPlayerByUUID(source);
   var at=actor.blockPosition().above();
   System.out.println("LIFE_CAPTURE_READY role="+(owner?"owner":"peer")+" source="+source+" floor="+FLOOR_Y
    +" owner="+actor.position()+" local="+mc.player.position()+" sky="+mc.level.getBrightness(LightLayer.SKY,at)
    +" block="+mc.level.getBrightness(LightLayer.BLOCK,at)+" effective="+mc.level.getEffectiveSkyBrightness(at)
    +" raw="+mc.level.getMaxLocalRawBrightness(at));
   mc.gui.hud.getChat().clearMessages(false);mc.gui.toastManager().clear();
  });
 }

 static void verify(Minecraft mc,UUID source,boolean owner){
  String problem=problem(mc,source,owner);check(problem==null,"Life capture readiness: "+problem);
 }

 private static String problem(Minecraft mc,UUID source,boolean owner){
  if(mc.level==null||mc.player==null||mc.getConnection()==null||owner!=mc.player.getUUID().equals(source))return "native role/world";
  var actor=mc.level.getPlayerByUUID(source);
  var others=mc.level.players().stream().filter(p->!p.getUUID().equals(source)).toList();
  if(actor==null||others.size()!=1)return "two tracked bodies";
  var viewer=others.getFirst();
  if(!actor.isAlive()||!viewer.isAlive()||!at(actor.position(),OWNER)||!at(viewer.position(),VIEWER)
    ||!facing(mc,actor,0,0)||!facing(mc,viewer,135,10))return "native stage poses";
  for(var body:List.of(actor,viewer)){
   var info=mc.getConnection().getPlayerInfo(body.getUUID());
   if(info==null||info.getGameMode()!=GameType.SURVIVAL)return "both Survival";
  }
  var level=mc.level;
  if(!level.isInsideBuildHeight(MIN)||!level.isInsideBuildHeight(MAX)||!resident(level))return "resident stage bounds";
  for(int x=MIN.getX();x<=MAX.getX();x++)for(int z=MIN.getZ();z<=MAX.getZ();z++){
   var floor=new BlockPos(x,FLOOR_Y,z);
   if(!level.getBlockState(floor).is(Blocks.POLISHED_DEEPSLATE))return "native stage floor";
   // canSeeSky is just SKY==15 in this version; WORLD_SURFACE independently rejects
   // any real overhead block, including transparent roofs, without loading a column.
   if(level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z)!=FLOOR_Y+1)return "open native column";
   for(int y=FLOOR_Y+1;y<=TOP_Y;y++)if(!daylight(level,new BlockPos(x,y,z)))return "body/material daylight volume";
  }
  return null;
 }

 private static boolean resident(Level level){
  for(int x=MIN.getX()>>4;x<=MAX.getX()>>4;x++)for(int z=MIN.getZ()>>4;z<=MAX.getZ()>>4;z++)
   if(!level.hasChunk(x,z))return false;
  return true;
 }
 private static boolean facing(Minecraft mc,Player body,float yaw,float pitch){
  // Tracked remote look packets quantize angles to 360/256 degrees.
  double tolerance=body==mc.player?.1:360.0/256+.001;
  return Math.abs(Mth.wrapDegrees(body.getYRot()-yaw))<=tolerance&&Math.abs(body.getXRot()-pitch)<=tolerance;
 }
 private static boolean at(Vec3 actual,Vec3 expected){return actual.distanceToSqr(expected)<.0004;}
 private static boolean daylight(Level level,BlockPos at){
  if(!level.isInsideBuildHeight(at)||!level.hasChunk(at.getX()>>4,at.getZ()>>4))return false;
  return level.getBlockState(at).isAir()&&level.canSeeSky(at)
   &&level.getBrightness(LightLayer.SKY,at)==15&&level.getBrightness(LightLayer.BLOCK,at)==0
   &&level.getEffectiveSkyBrightness(at)>=14&&level.getMaxLocalRawBrightness(at)>=14;
 }

 static final class Lighting implements AutoCloseable {
  private final MinecraftServer server;
  private final Holder<WorldClock> clock;
  private final ClockState clockState;
  private final int clear,rain,thunder;
  private final boolean raining,thundering;
  private final List<WeatherLevels> levels;
  Lighting(MinecraftServer server){
   this.server=server;clock=server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);
   clockState=server.clockManager().getInstance(clock).packState();
   var weather=server.getWeatherData();clear=weather.getClearWeatherTime();rain=weather.getRainTime();thunder=weather.getThunderTime();
   raining=weather.isRaining();thundering=weather.isThundering();
   var saved=new java.util.ArrayList<WeatherLevels>();for(var level:server.getAllLevels())saved.add(new WeatherLevels(level));levels=List.copyOf(saved);
  }
  void noon(){
   long gameTime=server.overworld().getGameTime();
   server.clockManager().setTotalTicks(clock,6000);server.setWeatherParameters(24000,0,false,false);
   check(server.overworld().getGameTime()==gameTime,"Disposable daylight setup never advances combat gameTime");
  }
  @Override public void close(){
   long gameTime=server.overworld().getGameTime();
   server.clockManager().getInstance(clock).loadFrom(clockState);
   // The native manager path syncs this restored state and invalidates each level's
   // environment-attribute cache, even when the paused value itself is unchanged.
   server.clockManager().setPaused(clock,clockState.paused());
   var weather=server.getWeatherData();weather.setClearWeatherTime(clear);weather.setRainTime(rain);weather.setThunderTime(thunder);
   weather.setRaining(raining);weather.setThundering(thundering);for(var level:levels)level.restore(server);
   check(server.clockManager().getInstance(clock).packState().equals(clockState)
    &&weather.getClearWeatherTime()==clear&&weather.getRainTime()==rain&&weather.getThunderTime()==thunder
    &&weather.isRaining()==raining&&weather.isThundering()==thundering&&server.overworld().getGameTime()==gameTime,
    "Success or failure restores exact pre-fixture clock/weather state without rewinding combat gameTime");
  }
 }
 private static final class WeatherLevels {
  final ServerLevel level;final float[] values=new float[WEATHER_FIELDS.size()];
  WeatherLevels(ServerLevel level){this.level=level;for(int i=0;i<values.length;i++)values[i]=weatherField(level,WEATHER_FIELDS.get(i),null);}
  void restore(MinecraftServer server){for(int i=0;i<values.length;i++){
   weatherField(level,WEATHER_FIELDS.get(i),values[i]);
   check(weatherField(level,WEATHER_FIELDS.get(i),null)==values[i],"Native weather interpolation state restored");
  }level.updateSkyBrightness();
   // A restored saturated rain level may not change on the next tick, so sync
   // native weather packets now instead of leaving either client at clear.
   server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(level.isRaining()?ClientboundGameEventPacket.START_RAINING:ClientboundGameEventPacket.STOP_RAINING,0),level.dimension());
   server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,values[1]),level.dimension());
   server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE,values[3]),level.dimension());
  }
 }
 private static float weatherField(Level level,String name,Float value){
  try{var field=Level.class.getDeclaredField(name);field.setAccessible(true);if(value!=null)field.setFloat(level,value);return field.getFloat(level);}
  catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
 }
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
