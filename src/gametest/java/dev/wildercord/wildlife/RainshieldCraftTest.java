package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.BelowkeeperTestSupport.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;
/** Supplied mature plants/pantry: real harvest pickup -> intermediate craft -> relic craft -> actual arrow wear. No natural growth/distribution credit. */
public final class RainshieldCraftTest implements FabricClientGameTest {
 private static final BlockPos WIND=new BlockPos(-1,101,0),MOON=new BlockPos(1,101,0);private ItemStack crafted;
 public void runTest(ClientGameTestContext c){try(var settings=new TidewardNative(c);var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");w.getServer().runCommand("difficulty normal");
  w.getServer().runOnServer(s->{var l=s.overworld();for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){l.setBlock(new BlockPos(x,100,z),Blocks.DIRT.defaultBlockState(),2);for(int y=101;y<=105;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}l.setBlock(new BlockPos(2,100,0),Blocks.WATER.defaultBlockState(),3);l.setBlock(WIND,HighlandContent.REED.defaultBlockState().setValue(WindreedBlock.AGE,2),3);l.setBlock(MOON,WetlandGarden.REED.defaultBlockState().setValue(MoonreedBlock.AGE,2),3);check(l.getBlockState(WIND).canSurvive(l,WIND)&&l.getBlockState(MOON).canSurvive(l,MOON),"Actual supplied plants have valid soil/moisture");var p=p(s);p.setGameMode(GameType.SURVIVAL);dev.wildercord.player.Spellbooks.set(p,dev.wildercord.player.Spellbooks.get(p).withStarterGiven());p.getInventory().clearContent();move(p,.5,101,3.5);p.getInventory().setItem(9,new ItemStack(Items.STRING));p.getInventory().setItem(10,new ItemStack(Items.SWEET_BERRIES));p.getInventory().setItem(11,new ItemStack(Items.COPPER_INGOT));p.getInventory().setItem(12,new ItemStack(Items.LEATHER));p.setHealth(20);});c.waitTicks(6);
  harvest(c,w,WIND,HighlandContent.WINDREED);harvest(c,w,MOON,WetlandGarden.FLOSS);
  w.getServer().runOnServer(s->check(p(s).getInventory().countItem(HighlandContent.WINDREED)==2&&p(s).getInventory().countItem(WetlandGarden.FLOSS)==1,"Real picked-up harvest materials are present before crafting"));
  var braid=w.getServer().computeOnServer(s->List.of(take(p(s),HighlandContent.WINDREED,1),take(p(s),Items.STRING,1),take(p(s),Items.SWEET_BERRIES,1),ItemStack.EMPTY));craft(c,w,braid,HighlandContent.WINDREED_BRAID);
  var inputs=w.getServer().computeOnServer(s->List.of(take(p(s),HighlandContent.WINDREED_BRAID,1),take(p(s),WetlandGarden.FLOSS,1),take(p(s),Items.COPPER_INGOT,1),take(p(s),Items.LEATHER,1)));craft(c,w,inputs,RooksRainshield.ITEM);
  w.getServer().runOnServer(s->{check(p(s).getInventory().countItem(HighlandContent.WINDREED)==1&&p(s).getInventory().countItem(WetlandGarden.FLOSS)==0&&p(s).getInventory().countItem(HighlandContent.WINDREED_BRAID)==0,"Actual earned harvest ingredients, not substituted stacks, are consumed once");crafted=take(p(s),RooksRainshield.ITEM,1);hand(p(s),crafted);move(p(s),.5,101,3.5);});c.waitTicks(6);c.runOnClient(mc->{mc.options.keyUse.setDown(true);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);});c.waitTicks(16);
  w.getServer().runOnServer(s->{check(p(s).getMainHandItem()==crafted&&RooksRainshield.active(p(s)),"Same actual crafted stack prepares the fan natively");var a=EntityTypes.ARROW.create(s.overworld(),EntitySpawnReason.COMMAND);check(a!=null,"Actual vanilla arrow");a.snapTo(-3.5,102.1,3.5,0,0);a.setDeltaMovement(new Vec3(.65,0,0));a.setBaseDamage(2);s.overworld().addFreshEntity(a);});c.waitTicks(12);
  w.getServer().runOnServer(s->check(p(s).getHealth()==20&&p(s).getMainHandItem()==crafted&&crafted.getDamageValue()==4&&p(s).getAttachedOrElse(RooksRainshield.READY,0L)>s.overworld().getGameTime(),"Real acquired/crafted relic pays one actual arrow catch with same-stack wear/rest"));c.runOnClient(mc->mc.options.keyUse.setDown(false));
 }}
 private static void harvest(ClientGameTestContext c,TestSingleplayerContext w,BlockPos at,Item material){int before=w.getServer().computeOnServer(s->p(s).getInventory().countItem(material));click(c,w,at,ItemStack.EMPTY);UUID drop=w.getServer().computeOnServer(s->{check(s.overworld().getBlockState(at).getValue(at.equals(WIND)?WindreedBlock.AGE:MoonreedBlock.AGE)==0,"Real native harvest resets supplied mature plant");if(p(s).getInventory().countItem(material)>before)return null;var drops=s.overworld().getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(3),e->e.getItem().is(material));check(drops.size()==1,"One actual harvest drop UUID is tracked");return drops.getFirst().getUUID();});if(drop!=null)collect(c,w,material,before,drop);}
}
