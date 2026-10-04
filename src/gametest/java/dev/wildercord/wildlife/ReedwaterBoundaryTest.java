package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.TidewardNative.*;
import dev.wildercord.Wildercord;
import dev.wildercord.client.fx.MagicQuality;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Genuine boundary physics and vanilla break callback; predamaged test footwear is explicitly supplied. */
public final class ReedwaterBoundaryTest implements FabricClientGameTest {
 private static boolean aided(net.minecraft.server.level.ServerPlayer p){return p.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY).hasModifier(Wildercord.id("reedwater_walk"));}
 @Override public void runTest(ClientGameTestContext c){
  try(var settings=new TidewardNative(c);var w=c.worldBuilder().create()){
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");
   w.getServer().runOnServer(s -> {var l=s.overworld();floor(l);for(int z=-3;z<=23;z++)for(int x=-2;x<=2;x++){
    boolean wall=x==-2 || x==2 || z==-3 || z==23;l.setBlock(new BlockPos(x,101,z),wall?Blocks.STONE.defaultBlockState():Blocks.WATER.defaultBlockState(),2);
   }for(int x=-1;x<=1;x++)for(int z=10;z<=15;z++)l.setBlock(new BlockPos(x,102,z),Blocks.WATER.defaultBlockState(),2);
    player(s).setGameMode(GameType.SURVIVAL);place(player(s),4.5,101,.5);player(s).setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TidewardEquipment.WADERS));});c.waitTicks(4);
   c.runOnClient(mc -> mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND));c.waitTicks(4);
   w.getServer().runOnServer(s -> check(!aided(player(s)) && !player(s).isInWater(),"Actual retained dry bank refuses water assistance"));
   w.getServer().runOnServer(s -> place(player(s),.5,101,12.5));c.waitTicks(5);
   w.getServer().runOnServer(s -> check(player(s).isInWater() && !aided(player(s)),"Actual two-block-deep water refuses shallow assistance"));
   w.getServer().runOnServer(s -> place(player(s),.5,101,.5));
   await(c,w,s -> TidewardEquipment.shallow(player(s)) && aided(player(s)),35,"Actual native player settles onto genuine supported shallow water");
   c.runOnClient(mc -> mc.options.keyJump.setDown(true));
   await(c,w,s -> !player(s).onGround(),15,"Actual native jump/swim input lifts feet from support");
   c.waitTicks(1);
   w.getServer().runOnServer(s -> check(!aided(player(s)),"Real loss of support removes water movement aid"));c.runOnClient(mc -> mc.options.keyJump.setDown(false));
   await(c,w,s -> TidewardEquipment.shallow(player(s)),40,"The real player lands back on supported shallow footing");
   c.runOnClient(mc -> {mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);MagicQuality.own=MagicQuality.Level.FULL;MagicQuality.others=MagicQuality.Level.MINIMAL;});c.waitTicks(3);shot(c,"tideward_waders_boundary_full_worn");
   c.runOnClient(mc -> {MagicQuality.own=MagicQuality.Level.MINIMAL;MagicQuality.others=MagicQuality.Level.FULL;});c.waitTicks(3);shot(c,"tideward_waders_boundary_minimal_worn");
   w.getServer().runOnServer(s -> {var worn=new ItemStack(TidewardEquipment.WADERS);worn.setDamageValue(worn.getMaxDamage()-1);player(s).setItemSlot(EquipmentSlot.FEET,worn);place(player(s),.5,101,.5);});c.waitTicks(4);
   int start=w.getServer().computeOnServer(s -> player(s).getAttachedOrElse(TidewardEquipment.WALK_PROGRESS,0));c.runOnClient(mc -> mc.options.keyUp.setDown(true));
   await(c,w,s -> player(s).getItemBySlot(EquipmentSlot.FEET).isEmpty(),65,"Real movement spends the last durability through vanilla equipped-item break");c.runOnClient(mc -> mc.options.keyUp.setDown(false));
   w.getServer().runOnServer(s -> {check(!aided(player(s)),"Vanilla break callback leaves no stale assistance on the now-empty feet slot");int expected=((start/TidewardEquipment.WALK_WEAR+1)*TidewardEquipment.WALK_WEAR)%TidewardEquipment.WALK_WINDOW;check(player(s).getAttachedOrElse(TidewardEquipment.WALK_PROGRESS,0)==expected,"Actual breaking movement reaches the exact shared wear boundary, including legitimate window wrap");});
  }
 }
}
