package dev.wildercord.wildlife;

import dev.wildercord.cast.*;
import dev.wildercord.spell.*;
import dev.wildercord.player.Heart;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import java.util.*;

/** Actual amphibious travel, live plants, client gathering/placement/reading and saved-world deadlines. */
public final class WetlandNewtTest implements FabricClientGameTest {
	private static LanternNewt newt;
	private static UUID saved;
	private static long pearlReady,responseReady;
	private static final BlockPos PLANT=new BlockPos(2,101,4),LAMP=new BlockPos(-4,101,2),DRY=new BlockPos(-5,102,-1);
	@Override public void runTest(ClientGameTestContext c) {
		TestWorldSave save;
		try(var w=c.worldBuilder().create()) {
			c.waitTicks(35);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule fall_damage false");w.getServer().runCommand("time set 6000");w.getServer().runCommand("weather clear");
			w.getServer().runOnServer(s -> {
				var l=s.overworld();for(int x=-16;x<=16;x++)for(int z=-10;z<=16;z++) {l.setBlock(new BlockPos(x,100,z),Blocks.DIRT.defaultBlockState(),2);l.setBlock(new BlockPos(x,101,z),x>=-8 && x<=8 && z>=0 && z<=12?Blocks.WATER.defaultBlockState():Blocks.GRASS_BLOCK.defaultBlockState(),2);}
				l.setBlock(PLANT,Blocks.SEAGRASS.defaultBlockState(),2);var p=p(s);p.setGameMode(GameType.CREATIVE);p.teleportTo(l,0,102,-2,Set.<Relative>of(),0,30,false);
				newt=WetlandContent.NEWT.create(l,EntitySpawnReason.COMMAND);newt.snapTo(-1.5,101.1,4.5,0,0);newt.getRandom().setSeed(314);l.addFreshEntity(newt);
				var biomes=l.registryAccess().lookupOrThrow(Registries.BIOME);
				for(var b:List.of(Biomes.SWAMP,Biomes.MANGROVE_SWAMP))check(biomes.getOrThrow(b).value().getAttributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.NATURAL_MOB_SPAWNS,MobSpawnSettings.EMPTY).getMobsToSpawn(MobCategory.CREATURE).unwrap().stream().anyMatch(e -> e.value().type()==WetlandContent.NEWT),"Newt appears in "+b);
				check(biomes.getOrThrow(Biomes.PLAINS).value().getAttributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.NATURAL_MOB_SPAWNS,MobSpawnSettings.EMPTY).getMobsToSpawn(MobCategory.CREATURE).unwrap().stream().noneMatch(e -> e.value().type()==WetlandContent.NEWT),"No ordinary-plains spawn entry");
				var r=net.minecraft.util.RandomSource.create(777);boolean accepted=false;for(int i=0;i<100;i++)accepted|=SpawnPlacements.checkSpawnRules(WetlandContent.NEWT,l,EntitySpawnReason.NATURAL,new BlockPos(6,101,6),r);check(accepted,"Native shallow-water rule accepts some natural tries");
				check(!SpawnPlacements.checkSpawnRules(WetlandContent.NEWT,l,EntitySpawnReason.NATURAL,new BlockPos(6,102,-2),r),"Dry ground is refused");
				l.setBlock(new BlockPos(6,102,6),Blocks.WATER.defaultBlockState(),2);check(!SpawnPlacements.checkSpawnRules(WetlandContent.NEWT,l,EntitySpawnReason.NATURAL,new BlockPos(6,101,6),r),"Deep water is refused");l.removeBlock(new BlockPos(6,102,6),false);
				for(var row:List.of(List.of(WetlandContent.DUSK_PEARL,Items.GLASS,Items.STRING,Items.SEAGRASS,WetlandContent.MARSHLIGHT_ITEM),List.of(WetlandContent.DUSK_PEARL,Items.BOOK,WetlandContent.FIELD_NOTES))) {
					var input=CraftingInput.of(2,2,java.util.stream.IntStream.range(0,4).mapToObj(i -> i<row.size()-1?new ItemStack(row.get(i)):ItemStack.EMPTY).toList());var recipe=s.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,l);check(recipe.isPresent() && recipe.get().value().assemble(input).is(row.getLast()),"Native recipe produces "+row.getLast());
				}
			});
			boolean browsed=false;for(int i=0;i<100;i++) {c.waitTicks(5);if(w.getServer().computeOnServer(s -> newt.browsing())) {browsed=true;break;}}
			check(browsed,w.getServer().computeOnServer(s -> "Actual newt never reaches seagrass: "+newt.position()+", water="+newt.isInWater()+", paths="+java.util.stream.IntStream.rangeClosed(-1,1).mapToObj(dy -> {var route=newt.getNavigation().createPath(PLANT.offset(0,dy,0),0);return dy+":"+(route==null?"null":route.canReach()+"/"+route.getEndNode());}).toList()+", goals="+newt.getGoalSelector().getAvailableGoals().stream().map(g -> g.getGoal().getClass().getSimpleName()+":"+g.isRunning()).toList()));
			w.getServer().runOnServer(s -> {check(newt.getX()>1 && s.overworld().getBlockState(PLANT).is(Blocks.SEAGRASS),"Separate food reached and plant retained");aim(s,newt.position(),3);});c.waitTicks(3);shot(c,"wetland_newt_browsing");c.waitTicks(40);
			w.getServer().runOnServer(s -> {check(!newt.browsing() && pearls(s)==0 && s.overworld().getBlockState(PLANT).is(Blocks.SEAGRASS),"Browsing ends without rewards or plant destruction: browsing="+newt.browsing()+", pearls="+pearls(s)+", plant="+s.overworld().getBlockState(PLANT)+", position="+newt.position());newt.setNoAi(true);newt.teleportTo(.5,101.1,1.5);newt.setDeltaMovement(Vec3.ZERO);aim(s,newt.position(),2.5);});c.waitTicks(4);shot(c,"wetland_newt_day");
			check(w.getServer().computeOnServer(s -> Heart.grimoire(p(s)).contains(FieldGuide.key("wildercord:lantern_newt"))),"Actually observed newt unlocks its bestiary entry");
			w.getServer().runOnServer(s -> {p(s).setGameMode(GameType.SURVIVAL);p(s).setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SEAGRASS,3));});c.waitTicks(4);feed(c,w);c.waitTicks(4);
			w.getServer().runOnServer(s -> {check(pearls(s)==1 && p(s).getMainHandItem().getCount()==2,"Actual feeding spends one plant and gives one pearl");pearlReady=newt.pearlReady();check(pearlReady>s.overworld().getGameTime(),"Finite gathering rest begins");});feed(c,w);c.waitTicks(4);
			w.getServer().runOnServer(s -> {check(pearls(s)==1 && p(s).getMainHandItem().getCount()==2 && newt.pearlReady()==pearlReady,"Immediate repeat gives nothing and renews no deadline");});c.waitTicks(65);
			w.getServer().runOnServer(s -> {impact(p(s),Runes.HASTE);check(newt.response()==0,"Unrelated arcane magic does not answer");impact(p(s),Runes.TIDEBREATH);check(newt.response()>0 && newt.responseReady()>s.overworld().getGameTime(),"Native Tidebreath impact wakes lanterns");responseReady=newt.responseReady();impact(p(s),Runes.CLEANSE);check(newt.responseReady()==responseReady && newt.pearlReady()==pearlReady && pearls(s)==1,"Repeated magic cannot renew response or generate another pearl");});
			c.waitTicks(205);w.getServer().runOnServer(s -> {impact(p(s),Runes.CLEANSE);check(newt.response()>0,"Life can answer after the exact response rest");responseReady=newt.responseReady();});
			w.getServer().runOnServer(s -> {var p=p(s);p.setGameMode(GameType.SURVIVAL);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(WetlandContent.MARSHLIGHT_ITEM,2));p.teleportTo(s.overworld(),-4.5,101,1.5,Set.<Relative>of(),0,35,false);});c.waitTicks(4);place(c,LAMP.below());c.waitTicks(5);
			w.getServer().runOnServer(s -> {var state=s.overworld().getBlockState(LAMP);check(state.is(WetlandContent.MARSHLIGHT) && state.getValue(BlockStateProperties.WATERLOGGED) && !s.overworld().getFluidState(LAMP).isEmpty() && p(s).getMainHandItem().getCount()==1,"Actual Marshlight placement retains source water and consumes one");p(s).teleportTo(s.overworld(),-5.5,102,-2.5,Set.<Relative>of(),0,25,false);});c.waitTicks(4);place(c,DRY.below());c.waitTicks(5);
			w.getServer().runOnServer(s -> {check(s.overworld().getBlockState(DRY).is(WetlandContent.MARSHLIGHT) && !s.overworld().getBlockState(DRY).getValue(BlockStateProperties.WATERLOGGED) && p(s).getMainHandItem().isEmpty(),"Dry placement stays dry and spends the second item");check(s.overworld().getBrightness(LightLayer.BLOCK,LAMP.above())>=11,"Native Marshlight illuminates the bank");p(s).setGameMode(GameType.CREATIVE);aim(s,newt.position(),2.5);});
			w.getServer().runCommand("time set 16000");c.waitTicks(8);shot(c,"wetland_marshlight_night");
			w.getServer().runOnServer(s -> {p(s).setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(WetlandContent.FIELD_NOTES));check(p(s).getMainHandItem().get(DataComponents.WRITTEN_BOOK_CONTENT).pages().size()==3,"Crafted lore has three native pages");});c.waitTicks(4);c.runOnClient(mc -> mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND));c.waitTicks(4);
			check(c.computeOnClient(mc -> mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.BookViewScreen),"Actual field notes open for reading");shot(c,"wetland_tideward_field_notes");c.runOnClient(mc -> mc.gui.setScreen(null));
			w.getServer().runOnServer(s -> {
				var l=s.overworld();var dry=WetlandContent.NEWT.create(l,EntitySpawnReason.COMMAND);dry.snapTo(7.5,102,-2.5,0,0);dry.setNoAi(true);l.addFreshEntity(dry);var p=p(s);p.setGameMode(GameType.SURVIVAL);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SEAGRASS,2));
				check(dry.interact(p,InteractionHand.MAIN_HAND,Vec3.ZERO).consumesAction() && p.getMainHandItem().getCount()==2 && dry.pearlReady()==0,"Dry newt refuses reward without consuming food");
				dry.teleportTo(7.5,101,2.5);dry.hurtServer(l,l.damageSources().playerAttack(p),1);dry.interact(p,InteractionHand.MAIN_HAND,Vec3.ZERO);check(p.getMainHandItem().getCount()==2 && dry.pearlReady()==0,"Recently hurt wet newt refuses food");dry.hurtServer(l,l.damageSources().genericKill(),1000);check(pearls(s)==1,"Killing creates no pearl reward");
				for(int i=0;i<2;i++) {var other=WetlandContent.NEWT.create(l,EntitySpawnReason.COMMAND);other.snapTo(6+i,101,5,0,0);other.setNoAi(true);l.addFreshEntity(other);}
				var r=net.minecraft.util.RandomSource.create(777);for(int i=0;i<40;i++)check(!SpawnPlacements.checkSpawnRules(WetlandContent.NEWT,l,EntitySpawnReason.NATURAL,new BlockPos(6,101,6),r),"Native local population cap refuses a fourth newt");
				p.setGameMode(GameType.CREATIVE);aim(s,newt.position(),2.5);saved=newt.getUUID();check(newt.getHealth()==10 && newt.getAirSupply()>0,"Submerged newt survives beyond normal drowning time");
			});save=w.getWorldSave();
		}
		try(var w=save.open()) {
			c.waitTicks(25);w.getServer().runOnServer(s -> {newt=(LanternNewt)s.overworld().getEntity(saved);check(newt!=null && newt.pearlReady()==pearlReady && newt.responseReady()==responseReady,"Same animal retains exact gathering and response deadlines after a full restart");check(s.overworld().getBlockState(LAMP).getValue(BlockStateProperties.WATERLOGGED),"Submerged Marshlight retains waterlogging after restart");p(s).setGameMode(GameType.SURVIVAL);p(s).setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SEAGRASS,2));aim(s,newt.position(),2.5);});c.waitTicks(4);feed(c,w);c.waitTicks(4);
			check(w.getServer().computeOnServer(s -> p(s).getMainHandItem().getCount()==2 && newt.pearlReady()==pearlReady),"Reloading cannot bypass the saved meal rest");
		}
	}
	private static void impact(ServerPlayer p,RuneDef effect) {var plan=SpellCompiler.compile(List.of(Runes.TOUCH,effect));CastEngine.onHit(new Cast(p),plan.root().groups.getFirst(),new Cast.Hit(List.of(newt),newt.position(),new Vec3(0,0,1),p.position(),newt.blockPosition(),Direction.UP,false),null);}
	private static void aim(MinecraftServer s,Vec3 target,double distance) {var p=p(s);var at=target.add(0,.9,-distance);var d=target.add(0,.2,0).subtract(at.add(0,p.getEyeHeight(),0));p.teleportTo(s.overworld(),at.x,at.y,at.z,Set.<Relative>of(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())),false);}
	private static void feed(ClientGameTestContext c,TestSingleplayerContext w) {int id=w.getServer().computeOnServer(s -> newt.getId());c.runOnClient(mc -> {var e=mc.level.getEntity(id);mc.gameMode.interact(mc.player,e,new EntityHitResult(e,e.getBoundingBox().getCenter()),InteractionHand.MAIN_HAND);});}
	private static void place(ClientGameTestContext c,BlockPos ground) {c.runOnClient(mc -> mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(ground),Direction.UP,ground,false)));}
	private static int pearls(MinecraftServer s) {return s.overworld().getEntitiesOfClass(ItemEntity.class,new AABB(-16,100,-10,16,106,16),e -> e.getItem().is(WetlandContent.DUSK_PEARL)).stream().mapToInt(e -> e.getItem().getCount()).sum()+java.util.stream.IntStream.range(0,p(s).getInventory().getContainerSize()).map(i -> p(s).getInventory().getItem(i).is(WetlandContent.DUSK_PEARL)?p(s).getInventory().getItem(i).getCount():0).sum();}
	private static ServerPlayer p(MinecraftServer s) {return s.getPlayerList().getPlayers().getFirst();}
	private static void check(boolean ok,String why) {if(!ok)throw new AssertionError(why);}
	private static void shot(ClientGameTestContext c,String name) {c.runOnClient(mc -> {mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());}
}
