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
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import java.util.*;
/** Real fungal visits, interaction packets and complete saved-world clocks. */
public final class SporebackSnailTest implements FabricClientGameTest {
 private static SporebackSnail snail;private static UUID saved;private static long gather,response;
 @Override public void runTest(ClientGameTestContext c) {
  TestWorldSave save;
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(30);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");
   w.getServer().runOnServer(s -> {var l=s.overworld();for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++) {l.setBlock(new BlockPos(x,29,z),Blocks.MOSS_BLOCK.defaultBlockState(),2);for(int y=30;y<=32;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);l.setBlock(new BlockPos(x,33,z),Blocks.STONE.defaultBlockState(),2);}l.setBlock(new BlockPos(2,30,0),Blocks.BROWN_MUSHROOM.defaultBlockState(),2);p(s).setGameMode(GameType.SURVIVAL);p(s).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,1200,0));place(s,4.5,-2.5);snail=SporebackContent.SNAIL.create(l,EntitySpawnReason.COMMAND);snail.snapTo(.5,30,.5,0,0);l.addFreshEntity(snail);
    var input=CraftingInput.of(2,2,List.of(new ItemStack(SporebackContent.DEW),new ItemStack(Items.PAPER),new ItemStack(Items.BROWN_MUSHROOM),ItemStack.EMPTY));var recipe=s.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,l);check(recipe.isPresent() && recipe.get().value().assemble(input).is(SporebackContent.POULTICE),"Loaded poultice recipe");});
   c.waitTicks(20);w.getServer().runOnServer(s -> {
    var registry=s.overworld().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
    for(var biome:List.of(net.minecraft.world.level.biome.Biomes.LUSH_CAVES,net.minecraft.world.level.biome.Biomes.DRIPSTONE_CAVES))check(registry.getOrThrow(biome).value().getAttributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.NATURAL_MOB_SPAWNS,net.minecraft.world.level.biome.MobSpawnSettings.EMPTY).getMobsToSpawn(MobCategory.CREATURE).unwrap().stream().anyMatch(e -> e.value().type()==SporebackContent.SNAIL),"Native cave pool");
    var random=net.minecraft.util.RandomSource.create(927);boolean accepted=false;for(int i=0;i<80;i++)accepted|=SpawnPlacements.checkSpawnRules(SporebackContent.SNAIL,s.overworld(),EntitySpawnReason.NATURAL,new BlockPos(-4,30,-4),random);check(accepted,"Dark underground moss habitat admits native attempts");
    s.overworld().setBlock(new BlockPos(-4,29,-4),Blocks.STONE.defaultBlockState(),2);for(int i=0;i<30;i++)check(!SpawnPlacements.checkSpawnRules(SporebackContent.SNAIL,s.overworld(),EntitySpawnReason.NATURAL,new BlockPos(-4,30,-4),random),"Dry stone footing refuses habitat");
   });
   await(c,w,()->snail.dew(),"Living mushroom visit prepares dew");
   w.getServer().runOnServer(s -> {check(s.overworld().getBlockState(new BlockPos(2,30,0)).is(Blocks.BROWN_MUSHROOM),"Browser never destroys mushroom");check(snail.distanceToSqr(2.5,30,.5)<.7,"Actual approach precedes dew");place(s,snail.getX()+1.4,snail.getZ()-1.4);p(s).setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);});c.waitTicks(5);shot(c,"sporeback_ready_dew");
   w.getServer().runOnServer(s -> {long before=snail.gatherReady();p(s).setGameMode(GameType.SPECTATOR);p(s).setShiftKeyDown(true);check(snail.mobInteract(p(s),InteractionHand.MAIN_HAND)==InteractionResult.PASS,"Authoritative gathering refuses spectator even through direct custom call");check(snail.dew() && snail.gatherReady()==before && p(s).getInventory().countItem(SporebackContent.JOURNAL)==0 && !dev.wildercord.player.Heart.grimoire(p(s)).contains("field:sporeback"),"Spectator cannot spend reserve, reset gathering, mint resource or discover journal");p(s).setGameMode(GameType.SURVIVAL);place(s,7.5,7.5);check(snail.mobInteract(p(s),InteractionHand.MAIN_HAND)==InteractionResult.PASS && snail.dew() && snail.gatherReady()==before,"Direct remote gathering is refused without spending reserve");place(s,snail.getX()+1.4,snail.getZ()-1.4);p(s).setShiftKeyDown(false);});c.waitTicks(5);
   interact(c,w);check(w.getServer().computeOnServer(s -> snail.dew()),"Standing interaction keeps dew");c.runOnClient(mc -> mc.options.keyShift.setDown(true));c.waitTicks(5);interact(c,w);c.runOnClient(mc -> mc.options.keyShift.setDown(false));c.waitTicks(5);
   w.getServer().runOnServer(s -> {check(!snail.dew() && snail.gatherReady()>s.overworld().getGameTime(),"Crouched empty-hand gathering uses saved reserve");check(p(s).getInventory().countItem(SporebackContent.JOURNAL)==1,"First observation journal");int dew=s.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,snail.getBoundingBox().inflate(4),e -> e.getItem().is(SporebackContent.DEW)).stream().mapToInt(e -> e.getItem().getCount()).sum()+p(s).getInventory().countItem(SporebackContent.DEW);check(dew==1,"Exactly one actual dew drop");gather=snail.gatherReady();impact(s,Runes.HEAL);response=snail.responseReady();impact(s,Runes.HEAL);check(snail.responseReady()==response && !snail.dew(),"Repeated Life does not renew rest or mint dew");});
   c.waitTicks(205);w.getServer().runOnServer(s -> {snail.answerMagic(true);check(snail.pose()==2 && snail.hiddenUntil()>s.overworld().getGameTime(),"Fire retracts into finite physical shell");if(p(s).getMainHandItem().is(SporebackContent.JOURNAL))p(s).getInventory().setItem(9,p(s).getMainHandItem().copy());p(s).setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(SporebackContent.POULTICE,2));});c.waitTicks(5);shot(c,"sporeback_closed_shell");w.getServer().runOnServer(s -> p(s).removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION));c.waitTicks(5);
   c.runOnClient(mc -> mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND));c.waitTicks(5);w.getServer().runOnServer(s -> {check(p(s).hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION) && p(s).hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS),"Actual item use applies sight with heavy-step tradeoff");check(p(s).getMainHandItem().getCount()==1,"One poultice consumed");});
   c.runOnClient(mc -> mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND));c.waitTicks(5);w.getServer().runOnServer(s -> {check(p(s).getMainHandItem().getCount()==1,"Existing sight refuses consumption/renewal");saved=snail.getUUID();response=snail.responseReady();});save=w.getWorldSave();
  }
  try(var w=save.open()) {c.waitTicks(35);w.getServer().runOnServer(s -> {snail=(SporebackSnail)s.overworld().getEntity(saved);check(snail!=null && snail.gatherReady()==gather && snail.responseReady()==response && !snail.dew(),"Complete restart preserves exact spent reserve and independent clocks");check(p(s).getInventory().countItem(SporebackContent.JOURNAL)==1 && dev.wildercord.player.Heart.grimoire(p(s)).contains("field:sporeback"),"Journal observation survives full restart");var extra=SporebackContent.SNAIL.create(s.overworld(),EntitySpawnReason.COMMAND);extra.snapTo(-3.5,30,-3.5,0,0);extra.setNoAi(true);s.overworld().addFreshEntity(extra);var random=net.minecraft.util.RandomSource.create(951);for(int i=0;i<40;i++)check(!SpawnPlacements.checkSpawnRules(SporebackContent.SNAIL,s.overworld(),EntitySpawnReason.NATURAL,new BlockPos(-5,30,-5),random),"Two-snail local cap");extra.hurtServer(s.overworld(),s.overworld().damageSources().generic(),100);check(s.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,extra.getBoundingBox().inflate(2),e -> e.getItem().is(SporebackContent.DEW)).isEmpty(),"Death cannot mint gathered resource");});c.waitTicks(130);check(w.getServer().computeOnServer(s -> snail.pose()!=2),"Retraction expires naturally");
   w.getServer().runOnServer(s -> {s.overworld().setBlock(new BlockPos(-6,33,-6),Blocks.AIR.defaultBlockState(),2);s.overworld().setBlock(new BlockPos(-5,30,-6),Blocks.TORCH.defaultBlockState(),2);var visitor=SporebackContent.SNAIL.create(s.overworld(),EntitySpawnReason.COMMAND);visitor.snapTo(-5.5,30,-5.5,0,0);s.overworld().addFreshEntity(visitor);snail=visitor;});c.waitTicks(15);
   for(int i=0;i<30;i++) {c.waitTicks(5);w.getServer().runOnServer(s -> {if(snail.pose()==2) {check(s.overworld().getMaxLocalRawBrightness(snail.blockPosition())<=8,"Shelter pose requires actual dark footing");boolean covered=false;for(int y=1;y<=3;y++)covered|=s.overworld().getBlockState(snail.blockPosition().above(y)).isSolidRender();check(covered,"Shelter pose requires cover over current foot, not a distant destination");}});}
}
 }
 private static void impact(MinecraftServer s,RuneDef e) {var plan=SpellCompiler.compile(List.of(Runes.TOUCH,e));CastEngine.onHit(new Cast(p(s)),plan.root().groups.getFirst(),new Cast.Hit(List.of(snail),snail.position(),new Vec3(0,0,1),p(s).position(),snail.blockPosition(),Direction.UP,false),null);}
 private static void interact(ClientGameTestContext c,TestSingleplayerContext w) {int id=w.getServer().computeOnServer(s -> snail.getId());c.runOnClient(mc -> {var e=mc.level.getEntity(id);mc.gameMode.interact(mc.player,e,new EntityHitResult(e,e.getBoundingBox().getCenter()),InteractionHand.MAIN_HAND);});c.waitTicks(5);}
 private static void await(ClientGameTestContext c,TestSingleplayerContext w,java.util.function.BooleanSupplier yes,String why) {for(int i=0;i<120;i++) {c.waitTicks(5);if(w.getServer().computeOnServer(s -> yes.getAsBoolean()))return;}throw new AssertionError(w.getServer().computeOnServer(s -> why+" pose="+snail.pose()+" pos="+snail.position()));}
 private static ServerPlayer p(MinecraftServer s) {return s.getPlayerList().getPlayers().getFirst();}
 private static void place(MinecraftServer s,double x,double z) {var p=p(s);var d=snail==null?new Vec3(.5-x,0,.5-z):snail.position().subtract(x,30,z);p.teleportTo(s.overworld(),x,30,z,Set.<Relative>of(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),35,false);}
 private static void shot(ClientGameTestContext c,String n) {c.runOnClient(mc -> {mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.takeScreenshot(TestScreenshotOptions.of(n).disableCounterPrefix());}
 private static void check(boolean b,String why) {if(!b)throw new AssertionError(why);}
}
