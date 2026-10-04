package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.MossveilNative.*;
import dev.wildercord.client.fx.MossveilFilterClient;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
/** Supplied worn helmet/materials, actual anvil result pickup and actual last-durability filter wear. */
public final class MossveilRepairBreakTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext c){try(var settings=new TidewardNative(c);var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule natural_health_regeneration false");var paid=new ItemStack[1];var pet=new MossveilDormouse[1];var outputDamage=new int[1];var xp=new int[1];var poisonStart=new long[1];
  w.getServer().runOnServer(s->{floor(s.overworld());var p=player(s);p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();place(p,1.5,-.5);p.giveExperienceLevels(20);xp[0]=p.experienceLevel;
   var at=new BlockPos(2,101,-1);s.overworld().setBlock(at,Blocks.ANVIL.defaultBlockState(),2);p.openMenu(new SimpleMenuProvider((id,inv,who)->new AnvilMenu(id,inv,ContainerLevelAccess.create(s.overworld(),at)),Component.literal("Supplied Mossveil repair")));
   var damaged=new ItemStack(MossveilCowl.ITEM);check(damaged.getMaxDamage()==66,"Actual helmet exposes configured66 durability");damaged.setDamageValue(32);p.containerMenu.getSlot(0).set(damaged);p.containerMenu.getSlot(1).set(new ItemStack(WetlandGarden.FLOSS));p.containerMenu.broadcastChanges();var result=p.containerMenu.getSlot(2).getItem();check(result.is(MossveilCowl.ITEM)&&result.getDamageValue()==16,"One actual repair-tag Floss previews exactly one quarter durability repair");outputDamage[0]=result.getDamageValue();});c.waitTicks(5);
  c.runOnClient(mc->{check(mc.player.containerMenu instanceof AnvilMenu,"Actual connected client received native anvil menu");mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId,2,0,ContainerInput.QUICK_MOVE,mc.player);});c.waitTicks(5);
  w.getServer().runOnServer(s->{var p=player(s);check(p.containerMenu.getSlot(0).getItem().isEmpty()&&p.containerMenu.getSlot(1).getItem().isEmpty()&&p.containerMenu.getSlot(2).getItem().isEmpty(),"Actual result packet consumes damaged input and exactly one repair Floss");check(p.experienceLevel<xp[0]&&p.getInventory().countItem(MossveilCowl.ITEM)==1&&p.getInventory().countItem(WetlandGarden.FLOSS)==0,"Actual vanilla repair pickup pays experience/material and leaves exactly one repaired helmet");for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(MossveilCowl.ITEM)){paid[0]=p.getInventory().getItem(i);p.getInventory().setItem(i,ItemStack.EMPTY);break;}check(paid[0]!=null&&paid[0].getDamageValue()==outputDamage[0],"Received repaired stack retains actual preview damage");p.closeContainer();
   // Explicit supplied near-break wear state tests final-point behavior, not a claim of66 prior filters.
   paid[0].setDamageValue(paid[0].getMaxDamage()-1);p.setItemSlot(EquipmentSlot.HEAD,paid[0]);place(p,1.5,-.5);pet[0]=mouse(s.overworld(),.5,.5);pet[0].tame(p);pet[0].setOrderedToSit(true);p.addEffect(new MobEffectInstance(MobEffects.POISON,600,0));poisonStart[0]=s.overworld().getGameTime();});
  await(c,w,s->pet[0].isInSittingPose(),30,"Actual supplied owner curl settles through normal AI");long cues=c.computeOnClient(mc->MossveilFilterClient.accepted());c.runOnClient(mc->mc.options.keyShift.setDown(true));
  await(c,w,s->player(s).getAttachedOrElse(MossveilCowl.READY,0L)>0,90,"Actual forty-tick planted filter reaches final native helmet wear");c.waitTicks(3);
  w.getServer().runOnServer(s->{var p=player(s);check(p.getItemBySlot(EquipmentSlot.HEAD).isEmpty()&&paid[0].isEmpty(),"Actual native last-point wear breaks and removes the equipped helmet");check(p.getAttachedOrElse(MossveilCowl.READY,0L)==pet[0].filterReady()&&pet[0].filterReady()>MossveilDormouse.clock(s.overworld()),"Broken helmet still pays exact shared finite rest once without refund");check(p.getEffect(MobEffects.POISON)!=null&&Math.abs((600-p.getEffect(MobEffects.POISON).getDuration())-(s.overworld().getGameTime()-poisonStart[0]))<=2&&!p.hasEffect(MobEffects.SLOWNESS),"Postwear absent helmet cannot trim Poison or publish successful slow-breath");});check(c.computeOnClient(mc->MossveilFilterClient.accepted())==cues,"Actual break path produces no false successful authored cue");c.runOnClient(mc->mc.options.keyShift.setDown(false));
 }}
}
