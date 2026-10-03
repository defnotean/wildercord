package dev.wildercord.aura.world;

import dev.wildercord.cast.SpellDefence;
import dev.wildercord.cast.Spirits;
import dev.wildercord.aura.Aura;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** Real spell resistance, attack openings, acquisition/persistence, tools and rendered custom animals. */
public final class AuraBeastsTest implements FabricClientGameTest {
	@Override public void runTest(ClientGameTestContext c) {
		try(var w=c.worldBuilder().create()) {
			c.waitTicks(40);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule fall_damage false");w.getServer().runCommand("time set 6000");
			var ids=w.getServer().computeOnServer(s->{
				var l=s.overworld();for(int x=-24;x<=24;x++)for(int z=-24;z<=24;z++)l.setBlock(new BlockPos(x,100,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);
				var biomeRegistry=s.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
				for(var habitat:Map.of(AuraBeasts.STONEHORN,List.of("meadow","windswept_hills","windswept_gravelly_hills","stony_peaks"),AuraBeasts.GALECLAW,List.of("windswept_hills","windswept_forest","jagged_peaks","frozen_peaks","snowy_slopes","grove")).entrySet()) {
					for(var name:habitat.getValue()) {
						var biome=biomeRegistry.getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,net.minecraft.resources.Identifier.withDefaultNamespace(name))).value();
						var spawns=biome.getAttributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.NATURAL_MOB_SPAWNS,net.minecraft.world.level.biome.MobSpawnSettings.EMPTY);
						check(spawns.getMobsToSpawn(MobCategory.CREATURE).unwrap().stream().anyMatch(e->e.value().type()==habitat.getKey()),"Natural spawn entry exists in "+name);
					}
				}
				for(var parts:List.of(List.of(AuraBeasts.STONEHORN_PLATE,Items.CLAY_BALL,Items.WHEAT,AuraBeasts.BASTION_POULTICE),List.of(AuraBeasts.GALECLAW_PLUME,Items.COPPER_INGOT,Items.STRING,AuraBeasts.RIDGE_WHISTLE))) {
					var input=net.minecraft.world.item.crafting.CraftingInput.of(3,1,List.of(new ItemStack(parts.get(0)),new ItemStack(parts.get(1)),new ItemStack(parts.get(2))));
					var recipe=s.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,l);
					check(recipe.isPresent() && recipe.get().value().assemble(input).is(parts.get(3)),"Actual loaded recipe crafts the intended field tool");
				}
				check(AuraBeasts.maySpawn(AuraBeasts.STONEHORN,l,EntitySpawnReason.NATURAL,new BlockPos(0,101,0)),"Dry open highland ground permits a natural spawn");
				l.setBlock(new BlockPos(20,101,20),Blocks.STONE.defaultBlockState(),2);check(!AuraBeasts.maySpawn(AuraBeasts.STONEHORN,l,EntitySpawnReason.NATURAL,new BlockPos(20,101,20)),"Occupied habitat is rejected");
				var stone=AuraBeasts.STONEHORN.create(l,EntitySpawnReason.COMMAND);stone.snapTo(-5,101,0,180,0);stone.setNoAi(true);l.addFreshEntity(stone);
				var gale=AuraBeasts.GALECLAW.create(l,EntitySpawnReason.COMMAND);gale.snapTo(5,101,0,180,0);gale.setNoAi(true);l.addFreshEntity(gale);
				var extra=AuraBeasts.STONEHORN.create(l,EntitySpawnReason.COMMAND);extra.snapTo(-15,101,0,0,0);l.addFreshEntity(extra);
				check(!AuraBeasts.maySpawn(AuraBeasts.STONEHORN,l,EntitySpawnReason.NATURAL,new BlockPos(0,101,0)),"Two local animals cap the natural population");extra.discard();
				var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(l,0,101,-9,Set.<Relative>of(),0,0,false);
				for(var beast:List.of(stone,gale)) {
					beast.pose(BeastRules.IDLE,0);float full=beast.getMaxHealth();beast.setHealth(full);beast.damageCooldownTime=0;
					SpellDefence.hurt(l,beast,l.damageSources().source(DamageTypes.FREEZE,p),10);check(close(full-beast.getHealth(),10*BeastRules.spell(beast.gale(),false)),"Elemental spell is resisted through the shared pipeline");
					beast.setHealth(full);beast.damageCooldownTime=0;SpellDefence.hurt(l,beast,l.damageSources().source(DamageTypes.IN_FIRE,p),10);check(close(full-beast.getHealth(),10*BeastRules.spell(beast.gale(),false)),"Fire spell follows the same resistance: "+(full-beast.getHealth())+" expected "+10*BeastRules.spell(beast.gale(),false));
					beast.setHealth(full);beast.damageCooldownTime=0;beast.hurtServer(l,l.damageSources().magic(),10);check(close(full-beast.getHealth(),10*BeastRules.spell(beast.gale(),false)),"Direct magic is resisted once");
					beast.setHealth(full);beast.damageCooldownTime=0;beast.hurtServer(l,l.damageSources().playerAttack(p),10);check(close(full-beast.getHealth(),10),"Physical blades retain their damage");
					beast.setHealth(full);beast.damageCooldownTime=0;beast.hurtServer(l,l.damageSources().source(Aura.DAMAGE,p,p),10);check(close(full-beast.getHealth(),10),"Aura damage retains its identity");
					beast.pose(BeastRules.RECOVER,50);beast.setHealth(full);beast.damageCooldownTime=0;SpellDefence.hurt(l,beast,l.damageSources().magic(),10);check(close(full-beast.getHealth(),10*BeastRules.spell(beast.gale(),true)),"Recovery opens an opportunity for supporting spells");
					beast.pose(BeastRules.IDLE,0);beast.aggression=0;beast.setTarget(null);beast.setHealth(full);
				}
				return new int[]{stone.getId(),gale.getId()};
			});
			c.runOnClient(mc->{mc.getWindow().setWindowed(1280,720);mc.options.guiScale().set(2);mc.resizeGui();});c.waitTicks(25);
			// Actual client feeding packets; the one-per-interval reward survives entity serialization.
			for(int index=0;index<2;index++) {
				int which=index,id=ids[index];w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var beast=(AuraBeast)s.overworld().getEntity(id);p.teleportTo(s.overworld(),beast.getX(),101,beast.getZ()-2.5,Set.<Relative>of(),0,0,false);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(which==0?Items.WHEAT:Items.RABBIT,2));});c.waitTicks(10);
				feed(c,id);c.waitTicks(10);feed(c,id);c.waitTicks(10);
				w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var beast=(AuraBeast)s.overworld().getEntity(id);check(p.getMainHandItem().getCount()==1,"Only successful feeding consumes food");check(drops(s.overworld(),which==0?AuraBeasts.STONEHORN_PLATE:AuraBeasts.GALECLAW_PLUME)==1,"Exactly one material is shed");var saved=TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,s.registryAccess());beast.saveWithoutId(saved);var restored=(AuraBeast)beast.getType().create(s.overworld(),EntitySpawnReason.LOAD);restored.load(TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,s.registryAccess(),saved.buildResult()));check(restored.pose()==BeastRules.RECOVER && restored.calmTicks()>0,"Load resumes safely and preserves calm");restored.mobInteract(p,InteractionHand.MAIN_HAND);check(p.getMainHandItem().getCount()==1 && drops(s.overworld(),which==0?AuraBeasts.STONEHORN_PLATE:AuraBeasts.GALECLAW_PLUME)==1,"Saved shed cooldown prevents repeat material");restored.discard();beast.pose(BeastRules.IDLE,0);});
			}
			// Photograph authored silhouettes without using frozen AI to prove behavior.
			view(w,c,ids[0],"stonehorn_grazer");view(w,c,ids[1],"galeclaw_ridge_runner");
			w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var stone=(Stonehorn)s.overworld().getEntity(ids[0]);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setHealth(20);p.teleportTo(s.overworld(),stone.getX(),101,stone.getZ()+6,Set.<Relative>of(),180,0,false);stone.calm=0;stone.setTarget(p);stone.setNoAi(false);});c.waitTicks(5);
			check(w.getServer().computeOnServer(s->((Stonehorn)s.overworld().getEntity(ids[0])).pose()==BeastRules.WARN),"Charge begins with a full warning");shot(c,"stonehorn_warning");
			w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(s.overworld(),p.getX()+6,101,p.getZ(),Set.<Relative>of(),180,0,false);});c.waitTicks(58);
			w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var stone=(Stonehorn)s.overworld().getEntity(ids[0]);check(p.getHealth()==20,"Sideways evasion avoids the committed charge");check(stone.pose()==BeastRules.RECOVER,"Charge leaves a recovery opening");stone.setNoAi(true);stone.setTarget(null);});
			w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var stone=(Stonehorn)s.overworld().getEntity(ids[0]);stone.pose(BeastRules.IDLE,0);stone.setTarget(p);stone.setNoAi(false);p.setHealth(20);p.damageCooldownTime=0;p.teleportTo(s.overworld(),stone.getX(),101,stone.getZ()+6,Set.<Relative>of(),180,0,false);});c.waitTicks(65);
			w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var stone=(Stonehorn)s.overworld().getEntity(ids[0]);check(p.getHealth()<20,"A player remaining in the charge lane takes actual harm");stone.setNoAi(true);stone.setTarget(null);});
			w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var gale=(Galeclaw)s.overworld().getEntity(ids[1]);gale.pose(BeastRules.IDLE,0);gale.calm=0;gale.aggression=0;gale.setTarget(p);gale.setNoAi(false);p.setHealth(20);p.teleportTo(s.overworld(),gale.getX(),101,gale.getZ()+6,Set.<Relative>of(),180,0,false);});c.waitTicks(5);
			var landing=w.getServer().computeOnServer(s->{var gale=(Galeclaw)s.overworld().getEntity(ids[1]);check(gale.pose()==BeastRules.WARN && !gale.distract(gale.position()),"Whistle cannot cancel committed leap preparation");return gale.landing();});shot(c,"galeclaw_warning");
			w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(s.overworld(),p.getX()+6,101,p.getZ(),Set.<Relative>of(),180,0,false);});c.waitTicks(42);aim(c,ids[1]);shot(c,"galeclaw_leap");c.waitTicks(18);
			w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var gale=(Galeclaw)s.overworld().getEntity(ids[1]);check(gale.landing().equals(landing) && p.getHealth()==20,"Fixed landing lets the player evade the leap");check(gale.pose()==BeastRules.RECOVER,"Leap leaves an exposed recovery");gale.setNoAi(true);gale.pose(BeastRules.IDLE,0);gale.aggression=0;gale.setTarget(p);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AuraBeasts.RIDGE_WHISTLE));});c.waitTicks(10);
			c.runOnClient(mc->mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND));c.waitTicks(10);check(w.getServer().computeOnServer(s->{var g=(Galeclaw)s.overworld().getEntity(ids[1]);return g.calmTicks()>0 && g.getTarget()==null;}),"Actual whistle use distracts stalking Galeclaw");
			w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AuraBeasts.BASTION_POULTICE,2));});c.waitTicks(10);c.runOnClient(mc->mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND));c.waitTicks(10);
			w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.hasEffect(MobEffects.RESISTANCE) && p.hasEffect(MobEffects.SLOWNESS) && p.getMainHandItem().getCount()==1 && p.getCooldowns().isOnCooldown(p.getMainHandItem()),"Poultice applies its useful effect, tradeoff, consumption and cooldown");});
			// Control magic remains a cooperative tool; it thaws instead of leaving the resistant beast frozen forever.
			w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);var stone=(Stonehorn)s.overworld().getEntity(ids[0]);stone.setNoAi(false);stone.setTarget(null);stone.calm=200;Spirits.hold(stone,30);check(stone.isNoAi(),"Support control can hold an Aura-resistant animal");});c.waitTicks(40);
			w.getServer().runOnServer(s->{var stone=(Stonehorn)s.overworld().getEntity(ids[0]);check(!stone.isNoAi(),"Support hold thaws after its actual deadline");stone.setNoAi(true);});
			var scraps=w.getServer().computeOnServer(s->{var gale=(Galeclaw)s.overworld().getEntity(ids[1]);gale.pose(BeastRules.IDLE,0);gale.setTarget(null);gale.setNoAi(false);gale.calm=200;var food=new ItemEntity(s.overworld(),gale.getX()+.5,101,gale.getZ(),new ItemStack(Items.CHICKEN));s.overworld().addFreshEntity(food);return food.getUUID();});c.waitTicks(45);
			check(w.getServer().computeOnServer(s->s.overworld().getEntity(scraps)==null),"Scavenger actually consumes nearby dropped food");
			w.getServer().runOnServer(s->{var gale=(Galeclaw)s.overworld().getEntity(ids[1]);gale.pose(BeastRules.IDLE,0);gale.calm=0;var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.overworld(),gale.getX()+12,101,gale.getZ(),Set.<Relative>of(),0,0,false);dev.wildercord.api.WildercordEvents.AFTER_CAST.invoker().afterCast(p,0,List.of(),10);});c.waitTicks(25);
			check(w.getServer().computeOnServer(s->{var gale=(Galeclaw)s.overworld().getEntity(ids[1]);return gale.getTarget()==s.getPlayerList().getPlayers().getFirst();}),"Recent casting really draws the ridge runner's attention");
			var prey=w.getServer().computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);var gale=(Galeclaw)s.overworld().getEntity(ids[1]);gale.setTarget(null);gale.pose(BeastRules.IDLE,0);gale.calm=0;var hare=dev.wildercord.wildlife.Wildlife.RIMEHARE.create(s.overworld(),EntitySpawnReason.COMMAND);hare.snapTo(gale.getX()+5,101,gale.getZ(),0,0);hare.setNoAi(true);s.overworld().addFreshEntity(hare);return hare.getUUID();});c.waitTicks(130);
			check(w.getServer().computeOnServer(s->{var hare=(LivingEntity)s.overworld().getEntity(prey);return hare==null || !hare.isAlive() || hare.getHealth()<hare.getMaxHealth();}),"Ridge predator actually hunts its highland prey");
			w.getServer().runCommand("difficulty peaceful");c.waitTicks(10);
			w.getServer().runOnServer(s->{var gale=(Galeclaw)s.overworld().getEntity(ids[1]);check(gale.getTarget()==null && gale.pose()==BeastRules.IDLE,"Peaceful cancels aggression and committed attack poses");check(!AuraBeasts.maySpawn(AuraBeasts.GALECLAW,s.overworld(),EntitySpawnReason.NATURAL,new BlockPos(0,101,0)),"Peaceful suppresses new natural beasts");});
		}
	}
	private static void feed(ClientGameTestContext c,int id) { c.runOnClient(mc->{var e=mc.level.getEntity(id);mc.gameMode.interact(mc.player,e,new EntityHitResult(e,e.getBoundingBox().getCenter()),InteractionHand.MAIN_HAND);}); }
	private static int drops(ServerLevel l,Item item) { int n=l.getEntitiesOfClass(ItemEntity.class,new AABB(-30,99,-30,30,110,30),e->e.getItem().is(item)).stream().mapToInt(e->e.getItem().getCount()).sum();for(var p:l.players())for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n; }
	private static void aim(ClientGameTestContext c,int id) { c.runOnClient(mc->{var e=mc.level.getEntity(id);var d=e.getBoundingBox().getCenter().subtract(mc.player.getEyePosition());mc.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())));}); }
	private static void view(TestSingleplayerContext w,ClientGameTestContext c,int id,String name) { w.getServer().runOnServer(s->{var e=(AuraBeast)s.overworld().getEntity(id);var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.teleportTo(s.overworld(),e.getX()+2.5,101,e.getZ()-3,Set.<Relative>of(),0,0,false);e.setYRot((float)Math.toDegrees(Math.atan2(-2.5,-3)));e.yBodyRot=e.yHeadRot=e.getYRot();});c.waitTicks(65);aim(c,id);c.waitTicks(5);shot(c,name);w.getServer().runOnServer(s->s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.SURVIVAL)); }
	private static void shot(ClientGameTestContext c,String name) { c.runOnClient(mc->{mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix()); }
	private static boolean close(double a,double b) { return Math.abs(a-b)<.01; }
	private static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
}
