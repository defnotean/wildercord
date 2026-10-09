package dev.wildercord.wildlife;
import dev.wildercord.cast.*;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.phys.*;
import java.util.*;

/** Native pollinator flight, ecological boundaries, inspection packets and saved root/cooldown. */
public final class WetlandGardenTest implements FabricClientGameTest {
 private static final BlockPos ROOT=new BlockPos(0,102,0);
 private static Glimmerwing moth;private static LanternNewt newt;private static long savedReady;
 @Override public void runTest(ClientGameTestContext c) {
  TestWorldSave save;
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(30);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");w.getServer().runCommand("time set 18000");w.getServer().runCommand("weather clear");
   w.getServer().runOnServer(s -> {
    dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.wildlife.WetlandGardenTest\",\"seed\":\""+s.overworld().getSeed()+"\"}");
    var l=s.overworld();for(int x=-20;x<=20;x++)for(int z=-15;z<=15;z++) {l.setBlock(new BlockPos(x,100,z),Blocks.DIRT.defaultBlockState(),2);l.setBlock(new BlockPos(x,101,z),x>=1&&x<=6?Blocks.WATER.defaultBlockState():Blocks.GRASS_BLOCK.defaultBlockState(),2);l.setBlock(new BlockPos(x,102,z),Blocks.AIR.defaultBlockState(),2);}
    p(s).setGameMode(GameType.CREATIVE);aim(s,new Vec3(.5,102.7,.5),3);
    l.setBlock(ROOT,WetlandGarden.REED.defaultBlockState(),2);
    var grow=RandomSource.create(119);for(int i=0;i<100 && age(s)==0;i++)WetlandGarden.REED.randomTick(l.getBlockState(ROOT),l,ROOT,grow);
    check(age(s)==1,"Native night growth prepares a bud without spawning moths");
    check(MoonreedBlock.moist(l,ROOT) && !MoonreedBlock.moist(l,new BlockPos(-5,102,0)),"Wet bank and dry root differ");
    check(!MoonreedBlock.pollinate(l,ROOT,new Vec3(-5,103,0)),"Distant moth cannot pollinate");
    l.setBlock(ROOT.above(),Blocks.STONE.defaultBlockState(),2);check(!MoonreedBlock.pollinate(l,ROOT,Vec3.atCenterOf(ROOT)),"Covered flower refuses");l.removeBlock(ROOT.above(),false);
    var biomes=l.registryAccess().lookupOrThrow(Registries.BIOME);
    for(var key:List.of(Biomes.SWAMP,Biomes.MANGROVE_SWAMP)) {
     var b=biomes.getOrThrow(key).value();check(b.getGenerationSettings().features().get(GenerationStep.Decoration.VEGETAL_DECORATION.ordinal()).stream().anyMatch(f -> f.unwrapKey().orElseThrow().identifier().equals(dev.wildercord.Wildercord.id("moonreed_patch"))),"Natural patch registered in "+key);
     check(b.getAttributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.NATURAL_MOB_SPAWNS,MobSpawnSettings.EMPTY).getMobsToSpawn(MobCategory.AMBIENT).unwrap().stream().anyMatch(e -> e.value().type()==Wildlife.GLIMMERWING),"Pollinator shares wetland biome");
    }
    moth=Wildlife.GLIMMERWING.create(l,EntitySpawnReason.COMMAND);moth.snapTo(-2,102.9,.5,0,0);moth.setPersistenceRequired();moth.getRandom().setSeed(77);l.addFreshEntity(moth);
   });
   boolean bloomed=false;for(int i=0;i<80;i++) {c.waitTicks(5);if(w.getServer().computeOnServer(s -> age(s)==2)) {bloomed=true;break;}}
   check(bloomed,"Actual flying Glimmerwing must approach and pollinate the separate flower");
   w.getServer().runOnServer(s -> {check(moth.distanceToSqr(Vec3.atCenterOf(ROOT).add(0,.4,0))<2,"Moth reached flower");moth.setNoAi(true);moth.setDeltaMovement(Vec3.ZERO);aim(s,new Vec3(.5,102.7,.5),2.2);});c.waitTicks(4);shot(c,"wetland_moonreed_pollinated_night");
   w.getServer().runCommand("time set 6000");c.waitTicks(5);shot(c,"wetland_moonreed_pollinated");
   w.getServer().runOnServer(s -> p(s).setGameMode(GameType.SURVIVAL));c.waitTicks(3);useRoot(c);c.waitTicks(5);
   w.getServer().runOnServer(s -> {check(age(s)==0 && floss(s)==1,"Actual client harvest gives one floss and retains root");});useRoot(c);c.waitTicks(4);check(w.getServer().computeOnServer(s -> floss(s)==1),"Repeat gives no extra floss");
   w.getServer().runCommand("time set 6000");w.getServer().runOnServer(s -> {s.overworld().setBlock(ROOT,WetlandGarden.REED.defaultBlockState(),2);var grow=RandomSource.create(119);for(int i=0;i<100;i++)WetlandGarden.REED.randomTick(s.overworld().getBlockState(ROOT),s.overworld(),ROOT,grow);check(age(s)==0,"Daytime random growth refuses");impact(p(s),Runes.GROW);check(age(s)==1,"Life prepares bud");impact(p(s),Runes.GROW);check(age(s)==1,"Life cannot substitute for pollination");check(!MoonreedBlock.pollinate(s.overworld(),ROOT,Vec3.atCenterOf(ROOT)),"Daytime moth cannot bloom");p(s).setGameMode(GameType.ADVENTURE);s.overworld().setBlock(ROOT,WetlandGarden.REED.defaultBlockState(),2);impact(p(s),Runes.GROW);check(age(s)==0,"Adventure Life cannot edit roots");p(s).setGameMode(GameType.CREATIVE);impact(p(s),Runes.GROW);});
   w.getServer().runOnServer(s -> {
    var ripe=new BlockPos(-2,102,2);s.overworld().setBlock(ripe,WetlandGarden.REED.defaultBlockState().setValue(MoonreedBlock.AGE,2),2);s.overworld().destroyBlock(ripe,true,p(s));check(floss(s)==2,"Native ripe break drops exactly one extra floss");s.overworld().setBlock(ripe,WetlandGarden.REED.defaultBlockState(),2);s.overworld().destroyBlock(ripe,true,p(s));check(floss(s)==2,"Native immature break yields no floss");
    var input=CraftingInput.of(2,2,List.of(new ItemStack(WetlandGarden.FLOSS),new ItemStack(WetlandContent.DUSK_PEARL),new ItemStack(Items.COPPER_INGOT),new ItemStack(Items.GLASS)));var recipe=s.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,s.overworld());check(recipe.isPresent() && recipe.get().value().assemble(input).is(WetlandGarden.LENS),"Native lens recipe");
    p(s).setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(WetlandGarden.LENS));p(s).setGameMode(GameType.SURVIVAL);
   });c.waitTicks(4);useRoot(c);c.waitTicks(4);
   w.getServer().runOnServer(s -> {check(p(s).getMainHandItem().getDamageValue()==1 && age(s)==1,"Lens inspection costs one use, not a harvest");savedReady=p(s).getAttachedOrElse(WetlandGarden.LENS_READY,0L);});useRoot(c);c.waitTicks(3);check(w.getServer().computeOnServer(s -> p(s).getMainHandItem().getDamageValue()==1),"Lens rests against repeated packets");shot(c,"wetland_dewglass_bud_reading");
   c.waitTicks(105);c.runOnClient(mc -> mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND));c.waitTicks(5);
   w.getServer().runOnServer(s -> {check(p(s).getMainHandItem().getDamageValue()==2,"Actual air-use scans visible reeds");});shot(c,"wetland_dewglass_bank_search");
   c.waitTicks(105);w.getServer().runOnServer(s -> {newt=WetlandContent.NEWT.create(s.overworld(),EntitySpawnReason.COMMAND);newt.snapTo(2.5,101.1,.5,0,0);newt.setNoAi(true);s.overworld().addFreshEntity(newt);aim(s,newt.position(),2);});c.waitTicks(5);int id=w.getServer().computeOnServer(s -> newt.getId());
   c.runOnClient(mc -> {var e=mc.level.getEntity(id);mc.gameMode.interact(mc.player,e,new EntityHitResult(e,e.getBoundingBox().getCenter()),InteractionHand.MAIN_HAND);});c.waitTicks(5);
   w.getServer().runOnServer(s -> {check(p(s).getMainHandItem().getDamageValue()==3 && newt.pearlReady()==0 && newt.getHealth()==10,"Inspecting newt does not gather, heal or change its rest");savedReady=p(s).getAttachedOrElse(WetlandGarden.LENS_READY,0L);});shot(c,"wetland_dewglass_newt_reading");save=w.getWorldSave();
  }
  try(var w=save.open()) {c.waitTicks(25);w.getServer().runOnServer(s -> {check(age(s)==1 && p(s).getMainHandItem().is(WetlandGarden.LENS) && p(s).getMainHandItem().getDamageValue()==3,"World restart preserves bud and lens wear");check(p(s).getAttachedOrElse(WetlandGarden.LENS_READY,0L)==savedReady,"Exact inspection rest saved");check(p(s).getCooldowns().isOnCooldown(p(s).getMainHandItem()),"Client-visible native cooldown restored at join");});useRoot(c);c.waitTicks(5);check(w.getServer().computeOnServer(s -> p(s).getMainHandItem().getDamageValue()==3),"Reloading cannot bypass inspection rest");
   w.getServer().runOnServer(s -> {
    var l=s.overworld();var feature=l.registryAccess().lookupOrThrow(Registries.FEATURE).getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.FEATURE,dev.wildercord.Wildercord.id("moonreed_patch"))).value();
    check(feature.place(l,l.getChunkSource().getGenerator(),RandomSource.create(1337),new BlockPos(1,102,8)),"Registered feature places real bank roots");
    check(!feature.place(l,l.getChunkSource().getGenerator(),RandomSource.create(1337),new BlockPos(-14,102,8)),"Registered feature refuses dry bank");
    long plants=java.util.stream.StreamSupport.stream(BlockPos.betweenClosed(new BlockPos(-4,100,4),new BlockPos(6,104,12)).spliterator(),false).filter(at -> l.getBlockState(at).is(WetlandGarden.REED)).count();check(plants>0 && plants<=24,"Feature uses at most twenty-four native placement attempts");
   });
  }
 }
 private static void impact(ServerPlayer p,RuneDef effect) {var plan=SpellCompiler.compile(List.of(Runes.TOUCH,effect));CastEngine.onHit(new Cast(p),plan.root().groups.getFirst(),new Cast.Hit(List.of(),Vec3.atCenterOf(ROOT),new Vec3(0,0,1),p.position(),ROOT,Direction.UP,false),null);}
 private static int age(MinecraftServer s) {return s.overworld().getBlockState(ROOT).getValue(MoonreedBlock.AGE);}
 private static int floss(MinecraftServer s) {return s.overworld().getEntitiesOfClass(ItemEntity.class,new AABB(-4,100,-4,4,106,4),e -> e.getItem().is(WetlandGarden.FLOSS)).stream().mapToInt(e -> e.getItem().getCount()).sum()+java.util.stream.IntStream.range(0,p(s).getInventory().getContainerSize()).map(i -> p(s).getInventory().getItem(i).is(WetlandGarden.FLOSS)?p(s).getInventory().getItem(i).getCount():0).sum();}
 private static void useRoot(ClientGameTestContext c) {c.runOnClient(mc -> mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(ROOT),Direction.UP,ROOT,false)));}
 private static ServerPlayer p(MinecraftServer s) {return s.getPlayerList().getPlayers().getFirst();}
 private static void aim(MinecraftServer s,Vec3 target,double distance) {var p=p(s);var at=target.add(0,.4,-distance);var d=target.subtract(at.add(0,p.getEyeHeight(),0));p.teleportTo(s.overworld(),at.x,at.y,at.z,Set.<Relative>of(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())),false);}
 private static void shot(ClientGameTestContext c,String name) {c.runOnClient(mc -> {mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());}
 private static void check(boolean b,String why) {if(!b)throw new AssertionError(why);}
}
