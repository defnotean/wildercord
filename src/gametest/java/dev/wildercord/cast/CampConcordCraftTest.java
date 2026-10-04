package dev.wildercord.cast;
import dev.wildercord.content.*;
import dev.wildercord.player.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;
/** Native table recipes, actual pickup consumption and real learning packets for both ordinary effects. */
public final class CampConcordCraftTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext c){try(var w=c.worldBuilder().create()){c.waitTicks(25);w.getServer().runOnServer(s->{CampConcordNative.arena(s);Spellbooks.set(CampConcordNative.player(s),Spellbook.EMPTY.withStarterGiven());s.overworld().setBlock(new BlockPos(2,101,4),Blocks.CRAFTING_TABLE.defaultBlockState(),2);});
  c.waitFor(mc->mc.level.getBlockState(new BlockPos(2,101,4)).is(Blocks.CRAFTING_TABLE),100);
  for(String path:List.of("watchweft","manabraid")){
   c.runOnClient(mc->mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(2.5,101.5,4.5),Direction.UP,new BlockPos(2,101,4),false)));c.waitTicks(5);
   w.getServer().runOnServer(s->{var p=CampConcordNative.player(s);CampConcordNative.check(p.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu,"Real table use opens native crafting menu");var inputs=path.equals("watchweft")?List.of(WildercordItems.BLANK_RUNE,Items.FEATHER,Items.STRING,Items.COPPER_INGOT,Items.LAPIS_LAZULI,Items.LAPIS_LAZULI,Items.GOLD_INGOT):List.of(WildercordItems.BLANK_RUNE,Items.LAPIS_LAZULI,Items.AMETHYST_SHARD,Items.STRING,Items.LAPIS_LAZULI,Items.LAPIS_LAZULI,Items.GOLD_INGOT);for(int i=0;i<9;i++)p.containerMenu.getSlot(i+1).set(i<inputs.size()?new ItemStack(inputs.get(i)):ItemStack.EMPTY);p.containerMenu.broadcastChanges();CampConcordNative.check(("wildercord:"+path).equals(p.containerMenu.getSlot(0).getItem().get(WildercordComponents.RUNE)),"Loaded native tier-II recipe produces exact new rune");});
   c.waitTicks(5);c.runOnClient(mc->mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId,0,0,ContainerInput.QUICK_MOVE,mc.player));c.waitTicks(5);
   w.getServer().runOnServer(s->{var p=CampConcordNative.player(s);for(int i=1;i<=9;i++)CampConcordNative.check(p.containerMenu.getSlot(i).getItem().isEmpty(),"Native recipe pickup consumes exact ingredients");int found=-1;for(int i=0;i<36;i++)if(("wildercord:"+path).equals(p.getInventory().getItem(i).get(WildercordComponents.RUNE)))found=i;CampConcordNative.check(found>=0,"Crafted actual rune appears in inventory");var item=p.getInventory().getItem(found).copy();p.getInventory().setItem(found,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,item);p.inventoryMenu.broadcastChanges();});c.runOnClient(mc->mc.player.closeContainer());c.waitTicks(5);c.runOnClient(mc->mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND));c.waitTicks(5);w.getServer().runOnServer(s->{var p=CampConcordNative.player(s);CampConcordNative.check(Spellbooks.get(p).knows("wildercord:"+path)&&p.getMainHandItem().isEmpty(),"Actual use consumes newly crafted rune and learns it");});
  }
 }}
}
