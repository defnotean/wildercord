package dev.wildercord.aura.world;

import dev.wildercord.wildlife.*;
import dev.wildercord.cast.*;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.*;
import java.util.*;

/** Real harvesting, replanting, grazing, tools, native spell impact and saved-world restart. */
public final class HighlandResourcesTest implements FabricClientGameTest {
	private static final BlockPos PATCH=new BlockPos(0,101,3), ROOT=new BlockPos(3,101,3);
	private static boolean refuse;
	private static Stonehorn grazer,protectedGrazer;
	private static Galeclaw runner;
	private static Rimehare hare;
	private static Rimehare feedingHare;
	private static long kiteReady,braidReady;
	@Override public void runTest(ClientGameTestContext c) {
		PlayerBlockBreakEvents.BEFORE.register((l,p,pos,state,be) -> !refuse || !state.is(HighlandContent.REED));
		TestWorldSave save;
		try(var w=c.worldBuilder().create()) {
			c.waitTicks(35);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");w.getServer().runCommand("weather clear");
			w.getServer().runOnServer(s -> {
				var l=s.overworld();for(int x=-24;x<=24;x++)for(int z=-24;z<=24;z++)l.setBlock(new BlockPos(x,100,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);
				var p=p(s);p.setGameMode(GameType.CREATIVE);p.teleportTo(l,.5,101,-.5,Set.<Relative>of(),0,8,false);
				for(int age=0;age<=2;age++)l.setBlock(PATCH.offset((age-2)*2,0,0),HighlandContent.REED.defaultBlockState().setValue(WindreedBlock.AGE,age),2);
				var feature=l.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE,dev.wildercord.Wildercord.id("windreed_patch"))).value();
				var biomes=l.registryAccess().lookupOrThrow(Registries.BIOME);
				for(var b:List.of(Biomes.MEADOW,Biomes.WINDSWEPT_HILLS,Biomes.WINDSWEPT_FOREST,Biomes.WINDSWEPT_GRAVELLY_HILLS))check(biomes.getOrThrow(b).value().getGenerationSettings().hasFeature(feature),"Windreed feature is registered in "+b);
				check(!biomes.getOrThrow(Biomes.PLAINS).value().getGenerationSettings().hasFeature(feature),"Ordinary plains are not saturated with ridge crops");
				for(var recipe:List.of(List.of(HighlandContent.WINDREED,AuraBeasts.GALECLAW_PLUME,Items.LEATHER,Items.STRING,Items.STICK,HighlandContent.DRAFT_KITE),List.of(HighlandContent.WINDREED,Items.STRING,Items.SWEET_BERRIES,HighlandContent.WINDREED_BRAID))) {
					var input=CraftingInput.of(3,2,java.util.stream.IntStream.range(0,6).mapToObj(i -> i<recipe.size()-1?new ItemStack(recipe.get(i)):ItemStack.EMPTY).toList());
					var found=s.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,l);check(found.isPresent() && found.get().value().assemble(input).is(recipe.getLast()),"Loaded recipe crafts the correct tool");
				}
			});c.waitTicks(10);shot(c,"highland_windreed_stages");
			w.getServer().runCommand("place feature wildercord:windreed_patch 14 101 14");c.waitTicks(5);
			check(w.getServer().computeOnServer(s -> {int count=0;for(var at:BlockPos.betweenClosed(9,101,9,19,103,19))if(s.overworld().getBlockState(at).is(HighlandContent.REED))count++;return count>0;}),w.getServer().computeOnServer(s -> {var at=new BlockPos(14,101,14);return "Feature placement: ground="+s.overworld().getBlockState(at.below())+", at="+s.overworld().getBlockState(at)+", fluid="+s.overworld().getFluidState(at)+", survives="+HighlandContent.REED.defaultBlockState().canSurvive(s.overworld(),at)+", feature="+s.registryAccess().lookupOrThrow(Registries.FEATURE).getOrThrow(ResourceKey.create(Registries.FEATURE,dev.wildercord.Wildercord.id("windreed_patch"))).value();}));
			w.getServer().runOnServer(s -> {var p=p(s);p.setGameMode(GameType.SURVIVAL);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);});c.waitTicks(5);
			click(c,PATCH);c.waitTicks(4);
			check(w.getServer().computeOnServer(s -> s.overworld().getBlockState(PATCH).getValue(WindreedBlock.AGE)==0 && drops(s)==2),"Actual harvest gives two tassels and retains a cut root");
			click(c,PATCH);c.waitTicks(3);check(w.getServer().computeOnServer(s -> drops(s)==2),"Repeated use of an immature root gives no extra harvest");
			w.getServer().runOnServer(s -> {var p=p(s);p.teleportTo(s.overworld(),3.5,101,.5,Set.<Relative>of(),0,10,false);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(HighlandContent.WINDREED,2));});c.waitTicks(5);
			click(c,ROOT.below());c.waitTicks(4);
			w.getServer().runOnServer(s -> {
				var l=s.overworld();check(l.getBlockState(ROOT).is(HighlandContent.REED) && l.getBlockState(ROOT).getValue(WindreedBlock.AGE)==0 && p(s).getMainHandItem().getCount()==1,"Actual planting consumes one tassel and starts young");
				l.setBlock(ROOT.below(),Blocks.STONE.defaultBlockState(),2);check(!HighlandContent.REED.defaultBlockState().canSurvive(l,ROOT),"Stone is not a planting surface");l.setBlock(ROOT.below(),Blocks.GRASS_BLOCK.defaultBlockState(),2);l.setBlock(ROOT,HighlandContent.REED.defaultBlockState(),2);
				for(int i=0;i<200 && l.getBlockState(ROOT).getValue(WindreedBlock.AGE)<2;i++)l.getBlockState(ROOT).randomTick(l,ROOT,l.getRandom());
				check(l.getBlockState(ROOT).getValue(WindreedBlock.AGE)==2,"Native random growth reaches mature tassels in open light");
				l.setBlock(ROOT,HighlandContent.REED.defaultBlockState(),2);impact(p(s),ROOT,Runes.GROW);
				check(l.getBlockState(ROOT).getValue(WindreedBlock.AGE)==1,"Native Life impact advances an existing root");
				refuse=true;try {impact(p(s),ROOT,Runes.GROW);}finally {refuse=false;}
				check(l.getBlockState(ROOT).getValue(WindreedBlock.AGE)==1,"Claim refusal prevents Life growth");
				p(s).setGameMode(GameType.ADVENTURE);impact(p(s),ROOT,Runes.GROW);p(s).setGameMode(GameType.CREATIVE);
				check(l.getBlockState(ROOT).getValue(WindreedBlock.AGE)==1,"Adventure cannot bypass planting permissions with Life");
				impact(p(s),ROOT,Runes.PUSH);check(l.getBlockState(ROOT).getValue(WindreedBlock.AGE)==1,"Wind rustles the crop without creating a harvest");
			});c.waitTicks(4);shot(c,"highland_windreed_magic");
			w.getServer().runCommand("time set 12000");
			w.getServer().runOnServer(s -> {
				p(s).teleportTo(s.overworld(),.5,101,-12,Set.<Relative>of(),0,0,false);
				var pos=new BlockPos(-8,101,5);s.overworld().setBlock(pos,HighlandContent.REED.defaultBlockState().setValue(WindreedBlock.AGE,2),2);
				grazer=AuraBeasts.STONEHORN.create(s.overworld(),EntitySpawnReason.COMMAND);grazer.snapTo(-7.5,101,5.5,0,0);s.overworld().addFreshEntity(grazer);
				var at=new BlockPos(0,101,20);s.overworld().setBlock(at,HighlandContent.REED.defaultBlockState().setValue(WindreedBlock.AGE,2),2);
				feedingHare=Wildlife.RIMEHARE.create(s.overworld(),EntitySpawnReason.COMMAND);feedingHare.snapTo(.5,101,20.5,0,0);s.overworld().addFreshEntity(feedingHare);
			});c.waitTicks(20);
			w.getServer().runOnServer(s -> {var p=p(s);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);var at=grazer.position().add(3,0,3);var d=grazer.getBoundingBox().getCenter().subtract(at.add(0,p.getEyeHeight(),0));p.teleportTo(s.overworld(),at.x,at.y,at.z,Set.<Relative>of(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())),false);});c.waitTicks(4);shot(c,"highland_stonehorn_windreed_forage");c.waitTicks(41);
			check(w.getServer().computeOnServer(s -> s.overworld().getBlockState(new BlockPos(-8,101,5)).getValue(WindreedBlock.AGE)==1),w.getServer().computeOnServer(s -> "Stonehorn forage: crop="+s.overworld().getBlockState(new BlockPos(-8,101,5))+", pose="+grazer.pose()+", position="+grazer.position()+", tick="+grazer.tickCount+", difficulty="+s.overworld().getDifficulty()+", clock="+s.overworld().getOverworldClockTime()+", goals="+grazer.getGoalSelector().getAvailableGoals().stream().map(g -> g.getGoal().getClass().getSimpleName()+":"+g.isRunning()).toList()));
			w.getServer().runOnServer(s -> {check(s.overworld().getBlockState(new BlockPos(0,101,20)).getValue(WindreedBlock.AGE)==1,"Actual adult Rimehare forage goal clips a mature crop");feedingHare.discard();});
			w.getServer().runCommand("gamerule mob_griefing false");
			w.getServer().runOnServer(s -> {
				var pos=new BlockPos(8,101,5);s.overworld().setBlock(pos,HighlandContent.REED.defaultBlockState().setValue(WindreedBlock.AGE,2),2);
				protectedGrazer=AuraBeasts.STONEHORN.create(s.overworld(),EntitySpawnReason.COMMAND);protectedGrazer.snapTo(8.5,101,5.5,0,0);s.overworld().addFreshEntity(protectedGrazer);
			});c.waitTicks(65);
			check(w.getServer().computeOnServer(s -> s.overworld().getBlockState(new BlockPos(8,101,5)).getValue(WindreedBlock.AGE)==2),"Mob-griefing refusal preserves the crop");
			w.getServer().runOnServer(s -> {
				grazer.setNoAi(true);protectedGrazer.setNoAi(true);var p=p(s);p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.overworld(),.5,101,-3,Set.<Relative>of(),0,0,false);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(HighlandContent.WINDREED_BRAID,2));
				hare=Wildlife.RIMEHARE.create(s.overworld(),EntitySpawnReason.COMMAND);hare.snapTo(.5,101,1,0,0);hare.setNoAi(true);s.overworld().addFreshEntity(hare);check(hare.boltsFrom(p),"Unprepared nearby player scares the hare");
			});c.waitTicks(5);use(c);c.waitTicks(5);
			w.getServer().runOnServer(s -> {
				var p=p(s);check(p.getMainHandItem().getCount()==1 && p.hasEffect(HighlandContent.DOWNWIND),"Actual braid use consumes one and gives Downwind");check(!hare.boltsFrom(p),"A calm scented approach can observe a hare");
				check(p.getAttributeValue(Attributes.MOVEMENT_SPEED)<.1,"Downwind carries its movement-speed tradeoff");
				runner=AuraBeasts.GALECLAW.create(s.overworld(),EntitySpawnReason.COMMAND);runner.snapTo(.5,101,6,0,0);s.overworld().addFreshEntity(runner);
				dev.wildercord.api.WildercordEvents.AFTER_CAST.invoker().afterCast(p,0,List.of(Runes.PUSH),4);
			});c.waitTicks(25);
			check(w.getServer().computeOnServer(s -> runner.getTarget()==null),"A quiet Downwind caster does not attract an unprovoked Galeclaw");
			check(c.computeOnClient(mc -> mc.player.hasEffect(HighlandContent.DOWNWIND)),"Native effect reaches the owner client");shot(c,"highland_downwind_braid");
			w.getServer().runOnServer(s -> {p(s).setSprinting(true);check(hare.boltsFrom(p(s)),"Sprinting defeats a quiet approach");runner.tickCount=100;runner.act(s.overworld());check(runner.getTarget()==p(s),"Native hunter behavior reveals a sprinting recent caster");});
			w.getServer().runOnServer(s -> {
				var p=p(s);p.setSprinting(false);runner.setNoAi(true);check(runner.getTarget()==p,"Quiet scent does not erase a acquired target");
				grazer.hurtServer(s.overworld(),s.overworld().damageSources().playerAttack(p),1);check(grazer.getTarget()==p,"Downwind cannot suppress defensive retaliation");
				p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(HighlandContent.DRAFT_KITE));p.getInventory().setItem(1,new ItemStack(HighlandContent.WINDREED,2));p.setHealth(20);
				s.overworld().setBlock(new BlockPos(12,119,-8),Blocks.GRASS_BLOCK.defaultBlockState(),2);p.teleportTo(s.overworld(),12.5,120,-7.5,Set.<Relative>of(),0,12,false);
			});c.waitTicks(5);use(c);c.waitTicks(5);shot(c,"highland_draft_kite");
			w.getServer().runOnServer(s -> {
				var p=p(s);check(p.hasEffect(MobEffects.SLOW_FALLING) && p.getInventory().getItem(1).getCount()==1 && p.getMainHandItem().is(HighlandContent.DRAFT_KITE),"Kite spends one fuel and remains reusable");s.overworld().removeBlock(new BlockPos(12,119,-8),false);
			});use(c);c.waitTicks(50);
			check(w.getServer().computeOnServer(s -> p(s).getInventory().getItem(1).getCount()==1 && p(s).getY()>110 && p(s).getHealth()==20),"Native slow descent and cooldown prevent a second fuel payment");
			c.waitTicks(140);w.getServer().runOnServer(s -> {
				check(!p(s).hasEffect(MobEffects.SLOW_FALLING) && p(s).getHealth()==20,"Finite kite lift expires after a safe actual landing");
				p(s).getInventory().setItem(1,ItemStack.EMPTY);
			});c.waitTicks(210);use(c);c.waitTicks(5);check(w.getServer().computeOnServer(s -> !p(s).getCooldowns().isOnCooldown(p(s).getMainHandItem()) && !p(s).hasEffect(MobEffects.SLOW_FALLING)),"Ready kite without fuel cannot give lift");
			w.getServer().runOnServer(s -> {p(s).setGameMode(GameType.CREATIVE);p(s).teleportTo(s.overworld(),3.5,101,.5,Set.<Relative>of(),0,12,false);});c.waitTicks(4);shot(c,"highland_resources_after_use");
			w.getServer().runOnServer(s -> {p(s).setGameMode(GameType.SURVIVAL);p(s).getInventory().setItem(1,new ItemStack(HighlandContent.WINDREED,2));});c.waitTicks(5);use(c);c.waitTicks(3);
			w.getServer().runOnServer(s -> {var p=p(s);check(p.hasEffect(MobEffects.SLOW_FALLING) && p.getInventory().getItem(1).getCount()==1,"Ready kite pays one fuel before saving");kiteReady=p.getAttachedOrElse(HighlandContent.KITE_READY,0L);braidReady=p.getAttachedOrElse(HighlandContent.BRAID_READY,0L);});
			save=w.getWorldSave();
		}
		try(var w=save.open()) {
			c.waitTicks(20);w.getServer().runOnServer(s -> {
				check(s.overworld().getBlockState(ROOT).is(HighlandContent.REED),"Saved-world restart retains the planted root");
				check(p(s).hasEffect(HighlandContent.DOWNWIND),"Saved-world restart retains finite native Downwind duration");check(p(s).getMainHandItem().is(HighlandContent.DRAFT_KITE),"Restart retains the reusable kite");
				check(p(s).getAttachedOrElse(HighlandContent.KITE_READY,0L)==kiteReady && p(s).getAttachedOrElse(HighlandContent.BRAID_READY,0L)==braidReady,"Restart preserves exact shared item-rest deadlines without renewal");
				check(p(s).getCooldowns().isOnCooldown(new ItemStack(HighlandContent.DRAFT_KITE)) && p(s).getCooldowns().isOnCooldown(new ItemStack(HighlandContent.WINDREED_BRAID)),"Join restores native shared cooldown indicators");
			});check(c.computeOnClient(mc -> mc.player.hasEffect(HighlandContent.DOWNWIND)),"Reconnected owner sees persisted Downwind");
			w.getServer().runOnServer(s -> {var p=p(s);p.removeEffect(MobEffects.SLOW_FALLING);p.getCooldowns().removeCooldown(p.getCooldowns().getCooldownGroup(p.getMainHandItem()));p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(HighlandContent.DRAFT_KITE));});c.waitTicks(3);use(c);c.waitTicks(4);
			check(w.getServer().computeOnServer(s -> !p(s).hasEffect(MobEffects.SLOW_FALLING) && p(s).getInventory().getItem(1).getCount()==1),"Persisted server deadline independently refuses a swapped kite even without the native indicator");
		}
	}
	private static int drops(MinecraftServer s) {return s.overworld().getEntitiesOfClass(ItemEntity.class,new AABB(PATCH).inflate(2),e -> e.getItem().is(HighlandContent.WINDREED)).stream().mapToInt(e -> e.getItem().getCount()).sum();}
	private static void impact(ServerPlayer p,BlockPos at,RuneDef effect) {var plan=SpellCompiler.compile(List.of(Runes.TOUCH,effect));CastEngine.onHit(new Cast(p),plan.root().groups.getFirst(),new Cast.Hit(List.of(),Vec3.atCenterOf(at),new Vec3(0,-1,0),p.position(),at,Direction.UP,false),null);}
	private static void click(ClientGameTestContext c,BlockPos at) {c.runOnClient(mc -> mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false)));}
	private static void use(ClientGameTestContext c) {c.runOnClient(mc -> mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND));}
	private static ServerPlayer p(MinecraftServer s) {return s.getPlayerList().getPlayers().getFirst();}
	private static void check(boolean ok,String why) {if(!ok)throw new AssertionError(why);}
	private static void shot(ClientGameTestContext c,String name) {c.runOnClient(mc -> {mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());}
}
