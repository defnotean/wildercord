package dev.wildercord.wildlife;
import dev.wildercord.cast.*;
import dev.wildercord.config.*;
import dev.wildercord.content.*;
import dev.wildercord.net.WildercordNetworking;
import dev.wildercord.player.*;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;
/** Actual crafted-rune learning, two paid CastSpell packets, Echo budget and full saved-world reopen. */
public final class RootCarryPaidTest implements FabricClientGameTest {
 private static final BlockPos A=new BlockPos(0,101,0),B=new BlockPos(3,101,0);
 private long selectedUntil,rest;private int price1,price2;private RuneDef rune;
 public void runTest(ClientGameTestContext c){TestWorldSave save;WildercordConfig original;
  try(var w=c.worldBuilder().create()){
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");
   original=w.getServer().computeOnServer(s->Config.get());
   try{
    w.getServer().runOnServer(s->{current(copy(original,Map.of("manaRegenMultiplier",0.0)));rune=Runes.get("wildercord:root_carry").orElseThrow();var l=s.overworld();var p=p(s);p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();
     for(int x=-3;x<=7;x++)for(int z=-4;z<=4;z++){l.setBlock(new BlockPos(x,100,z),Blocks.DIRT.defaultBlockState(),2);for(int y=101;y<=104;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}l.setBlock(A,EmberContent.FERN.defaultBlockState().setValue(CinderFernBlock.COOLED,false),2);aim(p,new Vec3(.5,101.15,.5));
     Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));Spellbooks.set(p,Spellbooks.get(p).withStarterGiven().learn(Runes.TOUCH.id()).learn(Runes.ECHO.id()));
     l.setBlock(new BlockPos(2,101,4),Blocks.CRAFTING_TABLE.defaultBlockState(),2);
    });
    c.waitTicks(3);c.runOnClient(mc->mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(new Vec3(2.5,101.5,4.5),Direction.UP,new BlockPos(2,101,4),false)));c.waitTicks(5);
    w.getServer().runOnServer(s->{var p=p(s);check(p.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu,"Actual crafting-table use packet opens native menu");
     var inputs=List.of(new ItemStack(WildercordItems.BLANK_RUNE),new ItemStack(Items.ROOTED_DIRT),new ItemStack(Items.BONE_MEAL),new ItemStack(Items.STRING),new ItemStack(Items.LAPIS_LAZULI),new ItemStack(Items.LAPIS_LAZULI),new ItemStack(Items.GOLD_INGOT),ItemStack.EMPTY,ItemStack.EMPTY);for(int i=0;i<9;i++)p.containerMenu.getSlot(i+1).set(inputs.get(i));p.containerMenu.broadcastChanges();check(rune.id().equals(p.containerMenu.getSlot(0).getItem().get(WildercordComponents.RUNE)),"Actual rank-II table recipe outputs Root Carry component");
    });
    c.waitTicks(5);c.runOnClient(mc->mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId,0,0,ContainerInput.QUICK_MOVE,mc.player));c.waitTicks(5);
    w.getServer().runOnServer(s->{var p=p(s);for(int i=1;i<=9;i++)check(p.containerMenu.getSlot(i).getItem().isEmpty(),"Actual crafted-rune pickup consumes every ingredient");int found=-1;for(int i=0;i<36;i++)if(rune.id().equals(p.getInventory().getItem(i).get(WildercordComponents.RUNE)))found=i;check(found>=0,"Actual crafted root rune exists");var stack=p.getInventory().getItem(found).copy();p.getInventory().setItem(found,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.inventoryMenu.broadcastChanges();});
    c.runOnClient(mc->mc.player.closeContainer());c.waitTicks(5);c.runOnClient(mc->mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND));c.waitTicks(5);
    w.getServer().runOnServer(s->{var p=p(s);check(Spellbooks.get(p).knows(rune.id())&&p.getMainHandItem().isEmpty(),"Actual use packet learns and consumes crafted rune");check(SpellCaster.edit(p,0,List.of(Runes.TOUCH.id(),rune.id(),Runes.ECHO.id()))==null,"Normal spell editor accepts new rune and Echo");price1=cost(p,List.of(Runes.TOUCH,rune,Runes.ECHO));Spellbooks.setMana(p,200);aim(p,new Vec3(.5,101.15,.5));});
    c.waitTicks(3);cast(c);c.waitTicks(4);
    w.getServer().runOnServer(s->{check(Spellbooks.mana(p(s))==200-price1,"First actual cast pays its exact compiled mana price");var mark=p(s).getAttached(RootCarry.SELECT);check(mark!=null&&mark.at().equals(A)&&s.overworld().getBlockState(A).getValue(CinderFernBlock.AGE)==0&&s.overworld().getBlockState(B).isAir(),"First payment selects an actual root without moving it");selectedUntil=mark.until();});
    c.takeScreenshot(TestScreenshotOptions.of("root_carry_actual_paid_selection").disableCounterPrefix());
    c.waitTicks(15);w.getServer().runOnServer(s->{check(p(s).getAttached(RootCarry.SELECT).until()==selectedUntil&&Spellbooks.mana(p(s))==200-price1,"Actual delayed Echo cannot renew selection or spend a second price");});
    int wait=w.getServer().computeOnServer(s->(int)Math.max(0,Spellbooks.readyAt(p(s),0)-s.overworld().getGameTime()+1));c.waitTicks(Math.max(1,wait));
    w.getServer().runOnServer(s->{var p=p(s);check(SpellCaster.edit(p,0,List.of(Runes.TOUCH.id(),rune.id()))==null,"Separate ordinary destination cast edits after actual cooldown");price2=cost(p,List.of(Runes.TOUCH,rune));aim(p,new Vec3(3.5,100.99,.5));});
    c.waitTicks(3);cast(c);c.waitTicks(4);
    w.getServer().runOnServer(s->{var p=p(s);check(Spellbooks.mana(p)==200-price1-price2,"Two distinct actual payments, no refunded or free transfer");check(s.overworld().getBlockState(A).isAir()&&s.overworld().getBlockState(B).is(EmberContent.FERN)&&s.overworld().getBlockState(B).getValue(CinderFernBlock.AGE)==0&&!s.overworld().getBlockState(B).getValue(CinderFernBlock.COOLED),"Exactly one retained young root preserves actual hot state");check(p.getAttached(RootCarry.SELECT)==null,"Successful move consumes selection");rest=p.getAttachedOrElse(RootCarry.READY,0L);check(rest>s.overworld().getGameTime(),"Successful two-write transfer earns finite rest");});
    c.takeScreenshot(TestScreenshotOptions.of("root_carry_actual_paid_transfer").disableCounterPrefix());
    w.getServer().runOnServer(s->{for(int slot=0;slot<36;slot++)check(p(s).getInventory().getItem(slot).isEmpty(),"Relocation returns no extra plant or crafting items");check(s.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(-4,100,-5,8,106,5)).isEmpty(),"Actual two-cell move emits no root drops");});
   }finally{w.getServer().runOnServer(s->current(original));}
   save=w.getWorldSave();
  }
  try(var w=save.open()){
   c.waitTicks(25);w.getServer().runOnServer(s->{check(p(s).getAttachedOrElse(RootCarry.READY,0L)==rest&&p(s).getAttached(RootCarry.SELECT)==null,"Exact saved rest survives fresh server; no selection resurrects");check(s.overworld().getBlockState(A).isAir()&&s.overworld().getBlockState(B).is(EmberContent.FERN)&&!s.overworld().getBlockState(B).getValue(CinderFernBlock.COOLED),"Actual moved-root state survives full reopen");});
   int wait=w.getServer().computeOnServer(s->(int)Math.max(1,rest-s.overworld().getGameTime()+1));c.waitTicks(wait);
   w.getServer().runOnServer(s->{aim(p(s),new Vec3(3.5,101.15,.5));Spellbooks.setMana(p(s),200);});c.waitTicks(3);cast(c);c.waitTicks(4);check(w.getServer().computeOnServer(s->p(s).getAttached(RootCarry.SELECT)!=null),"Finite rest expires and actual client cast creates another selection");save=w.getWorldSave();
  }
  try(var w=save.open()){c.waitTicks(25);w.getServer().runOnServer(s->check(p(s).getAttached(RootCarry.SELECT)==null&&p(s).getAttachedOrElse(RootCarry.READY,0L)==rest,"Active marker is cleared by real restart without renewing the original rest"));}
 }
 private static int cost(ServerPlayer p,List<RuneDef> rs){return Heart.manaCost(p,SpellCompiler.compile(rs),Heart.secretCost(p,rs)*Mastery.costFactor(p,rs));}
 private static void cast(ClientGameTestContext c){c.runOnClient(mc->ClientPlayNetworking.send(new WildercordNetworking.CastSpell(0)));}
 private static void aim(ServerPlayer p,Vec3 target){var eye=new Vec3(2,101+p.getEyeHeight(),2.5);var d=target.subtract(eye);p.teleportTo(p.level(),2,101,2.5,Set.<Relative>of(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())),false);}
 private static ServerPlayer p(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
 @SuppressWarnings("unchecked") private static <T>T copy(T record,Map<String,Object> changes){try{var ps=record.getClass().getRecordComponents();var types=new Class<?>[ps.length];var args=new Object[ps.length];for(int i=0;i<ps.length;i++){types[i]=ps[i].getType();args[i]=changes.getOrDefault(ps[i].getName(),ps[i].getAccessor().invoke(record));}return (T)record.getClass().getDeclaredConstructor(types).newInstance(args);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void current(WildercordConfig config){try{var f=Config.class.getDeclaredField("current");f.setAccessible(true);f.set(null,config);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
}
