package dev.wildercord.wildlife;
import dev.wildercord.cast.*;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.*;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import java.util.*;
public final class ReedbackCrabTest implements FabricClientGameTest {
 private static ReedbackCrab crab;private static UUID saved;private static long response,calm;
 @Override public void runTest(ClientGameTestContext c) {
  TestWorldSave save;
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(30);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");w.getServer().runCommand("difficulty normal");
   w.getServer().runOnServer(s -> {var l=s.overworld();for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++) {l.setBlock(new BlockPos(x,100,z),Blocks.MUD.defaultBlockState(),2);l.setBlock(new BlockPos(x,101,z),x>4?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState(),2);l.setBlock(new BlockPos(x,102,z),Blocks.AIR.defaultBlockState(),2);}
    var biomes=l.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
    for(var biome:List.of(net.minecraft.world.level.biome.Biomes.SWAMP,net.minecraft.world.level.biome.Biomes.MANGROVE_SWAMP))check(biomes.getOrThrow(biome).value().getAttributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.NATURAL_MOB_SPAWNS,net.minecraft.world.level.biome.MobSpawnSettings.EMPTY).getMobsToSpawn(MobCategory.CREATURE).unwrap().stream().anyMatch(e -> e.value().type()==ReedbackContent.CRAB),"Registered wetland pool");
    check(biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS).value().getAttributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.NATURAL_MOB_SPAWNS,net.minecraft.world.level.biome.MobSpawnSettings.EMPTY).getMobsToSpawn(MobCategory.CREATURE).unwrap().stream().noneMatch(e -> e.value().type()==ReedbackContent.CRAB),"No plains pool entry");
    var random=net.minecraft.util.RandomSource.create(872);var wet=new BlockPos(6,101,6);boolean accepted=false;for(int i=0;i<100;i++)accepted|=SpawnPlacements.checkSpawnRules(ReedbackContent.CRAB,l,EntitySpawnReason.NATURAL,wet,random);check(accepted,"Native shallow bank admits natural tries");
    for(int i=0;i<40;i++)check(!SpawnPlacements.checkSpawnRules(ReedbackContent.CRAB,l,EntitySpawnReason.NATURAL,new BlockPos(-8,101,-8),random),"Dry inland placement refused");
    l.setBlock(wet.above(),Blocks.WATER.defaultBlockState(),2);for(int i=0;i<40;i++)check(!SpawnPlacements.checkSpawnRules(ReedbackContent.CRAB,l,EntitySpawnReason.NATURAL,wet,random),"Deep water refused");l.setBlock(wet.above(),Blocks.AIR.defaultBlockState(),2);
    p(s).setGameMode(GameType.SURVIVAL);placePlayer(s,.5,-2.5);crab=ReedbackContent.CRAB.create(l,EntitySpawnReason.COMMAND);crab.snapTo(.5,101,.5,0,0);l.addFreshEntity(crab);});
   await(c,w,()->crab.pose()==ReedbackCrab.WARNING,"Unsafe approach must produce warning");c.waitTicks(12);shot(c,"reedback_warning");
   w.getServer().runOnServer(s -> {check(crab.pose()==ReedbackCrab.WARNING && p(s).getHealth()==20,"Warning does not deal damage");placePlayer(s,3.5,.5);});c.waitTicks(42);
   check(w.getServer().computeOnServer(s -> p(s).getHealth()==20 && crab.pose()==ReedbackCrab.RECOVERY),"Sidestep evades fixed sweep and exposes recovery");
   w.getServer().runOnServer(s -> {placePlayer(s,.5,-1.5);});await(c,w,()->crab.pose()==ReedbackCrab.WARNING,"Crab prepares another sweep after recovery");c.waitTicks(45);
   check(w.getServer().computeOnServer(s -> p(s).getHealth()<20),"Remaining in committed sweep takes actual damage");
   w.getServer().runOnServer(s -> {impact(s,Runes.TIDEBREATH);response=crab.responseReady();calm=crab.calmUntil();});c.waitTicks(5);
   check(w.getServer().computeOnServer(s -> crab.pose()==ReedbackCrab.CALM && crab.getTarget()==null && calm>s.overworld().getGameTime()),"Native water impact calms aggression");shot(c,"reedback_water_calm");
   w.getServer().runOnServer(s -> {impact(s,Runes.TIDEBREATH);check(crab.responseReady()==response && crab.calmUntil()==calm,"Repeated impact cannot renew either clock");p(s).setGameMode(GameType.CREATIVE);});c.waitTicks(205);
   w.getServer().runOnServer(s -> {p(s).setGameMode(GameType.SURVIVAL);p(s).setHealth(20);placePlayer(s,.5,-2.5);});await(c,w,()->crab.pose()==ReedbackCrab.WARNING,"Calm expires naturally");
   w.getServer().runOnServer(s -> {impact(s,Runes.WINDCUT);});c.waitTicks(5);check(w.getServer().computeOnServer(s -> crab.pose()==ReedbackCrab.RECOVERY && crab.responseReady()>s.overworld().getGameTime()),"Wind impact interrupts actual warning");shot(c,"reedback_wind_recovery");
   w.getServer().runCommand("difficulty peaceful");c.waitTicks(10);check(w.getServer().computeOnServer(s -> crab.getTarget()==null && crab.pose()==ReedbackCrab.IDLE),"Peaceful cancels aggression");
   w.getServer().runCommand("difficulty normal");w.getServer().runOnServer(s -> {crab.discard();crab=ReedbackContent.CRAB.create(s.overworld(),EntitySpawnReason.COMMAND);crab.snapTo(.5,101,.5,0,0);s.overworld().addFreshEntity(crab);});
   // Actual client sneak input, outside the crab's two-block personal space.
   c.runOnClient(mc -> mc.options.keyShift.setDown(true));c.waitTicks(40);check(w.getServer().computeOnServer(s -> crab.getTarget()==null && crab.pose()!=ReedbackCrab.WARNING),"Quiet approach avoids territorial acquisition");c.runOnClient(mc -> mc.options.keyShift.setDown(false));
   w.getServer().runOnServer(s -> {impact(s,Runes.TIDEBREATH);response=crab.responseReady();calm=crab.calmUntil();saved=crab.getUUID();});c.waitTicks(5);save=w.getWorldSave();
  }
  try(var w=save.open()) {c.waitTicks(35);w.getServer().runOnServer(s -> {crab=(ReedbackCrab)s.overworld().getEntity(saved);check(crab!=null && crab.responseReady()==response && crab.calmUntil()==calm,"Full reload preserves exact independent magic clocks");check(crab.pose()==ReedbackCrab.CALM && crab.getTarget()==null,"Reload does not release a stale swipe");
    var extra=ReedbackContent.CRAB.create(s.overworld(),EntitySpawnReason.COMMAND);extra.snapTo(9.5,101,9.5,0,0);extra.setNoAi(true);s.overworld().addFreshEntity(extra);var random=net.minecraft.util.RandomSource.create(884);for(int i=0;i<60;i++)check(!SpawnPlacements.checkSpawnRules(ReedbackContent.CRAB,s.overworld(),EntitySpawnReason.NATURAL,new BlockPos(6,101,6),random),"Two-crab local cap refuses another spawn");
    extra.hurtServer(s.overworld(),s.overworld().damageSources().generic(),100);int clay=s.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(extra.blockPosition()).inflate(3),e -> e.getItem().is(net.minecraft.world.item.Items.CLAY_BALL)).stream().mapToInt(e -> e.getItem().getCount()).sum();check(clay>=1 && clay<=3,"Native death loot yields the documented clay");});}
 }
 private static void impact(MinecraftServer s,RuneDef effect) {var plan=SpellCompiler.compile(List.of(Runes.TOUCH,effect));CastEngine.onHit(new Cast(p(s)),plan.root().groups.getFirst(),new Cast.Hit(List.of(crab),crab.position(),new Vec3(0,0,1),p(s).position(),crab.blockPosition(),Direction.UP,false),null);}
 private static void await(ClientGameTestContext c,TestSingleplayerContext w,java.util.function.BooleanSupplier yes,String why) {for(int i=0;i<50;i++) {c.waitTicks(5);if(w.getServer().computeOnServer(s -> yes.getAsBoolean()))return;}throw new AssertionError(w.getServer().computeOnServer(s -> why+": pose="+crab.pose()+", target="+crab.getTarget()+", crab="+crab.position()+", player="+p(s).position()));}
 private static ServerPlayer p(MinecraftServer s) {return s.getPlayerList().getPlayers().getFirst();}
 private static void placePlayer(MinecraftServer s,double x,double z) {var p=p(s);var d=crab==null?new Vec3(.5-x,0,.5-z):crab.position().subtract(x,101,z);p.teleportTo(s.overworld(),x,101,z,Set.<Relative>of(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),20,false);}
 private static void shot(ClientGameTestContext c,String name) {c.runOnClient(mc -> {mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());}
 private static void check(boolean yes,String why) {if(!yes)throw new AssertionError(why);}
}
