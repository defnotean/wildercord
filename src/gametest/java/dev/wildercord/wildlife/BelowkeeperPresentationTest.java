package dev.wildercord.wildlife;

import static dev.wildercord.wildlife.BelowkeeperTestSupport.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Registered feature and native item/model presentation. Equipment/books are supplied, not earned here. */
public final class BelowkeeperPresentationTest implements FabricClientGameTest {
 private BlockPos[] markers;
 @Override public void runTest(ClientGameTestContext c){
  var camera=c.computeOnClient(mc -> mc.options.getCameraType());
  var screen=c.computeOnClient(mc -> mc.gui.screen());
  boolean hidden=c.computeOnClient(mc -> mc.gui.hud.isHidden());
  int[] size=c.computeOnClient(mc -> new int[]{mc.getWindow().getWidth(),mc.getWindow().getHeight()});
  int scale=c.computeOnClient(mc -> mc.options.guiScale().get());
  boolean[] keys=c.computeOnClient(mc -> new boolean[]{mc.options.keyUp.isDown(),mc.options.keyDown.isDown(),mc.options.keyLeft.isDown(),mc.options.keyRight.isDown(),mc.options.keyShift.isDown(),mc.options.keyJump.isDown(),mc.options.keyUse.isDown(),mc.options.keyAttack.isDown()});
  try(var w=c.worldBuilder().create()){
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");w.getServer().runCommand("time set 6000");
   c.runOnClient(mc -> {var k=new net.minecraft.client.KeyMapping[]{mc.options.keyUp,mc.options.keyDown,mc.options.keyLeft,mc.options.keyRight,mc.options.keyShift,mc.options.keyJump,mc.options.keyUse,mc.options.keyAttack};for(var key:k)key.setDown(false);mc.getWindow().setWindowed(1024,640);mc.options.guiScale().set(2);mc.resizeGui();mc.gui.setScreen(null);mc.options.setCameraType(CameraType.FIRST_PERSON);if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();});
   w.getServer().runOnServer(s -> {
    var l=s.overworld();for(int x=-18;x<=18;x++)for(int z=-14;z<=14;z++)for(int y=29;y<=34;y++)l.setBlock(new BlockPos(x,y,z),y==29?Blocks.MOSS_BLOCK.defaultBlockState():y==34?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
    p(s).setGameMode(GameType.SPECTATOR);p(s).addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,2400,0,false,false,false));move(p(s),0,31,0);
   });
   w.getServer().runCommand("place feature wildercord:belowkeeper_drainhouse 0 30 0");
   w.getServer().runOnServer(s -> {markers=new BlockPos[3];for(int x=-18;x<=18;x++)for(int z=-14;z<=14;z++)for(int y=30;y<=31;y++){var at=new BlockPos(x,y,z);var state=s.overworld().getBlockState(at);if(state.is(DrainhouseContent.MARK)){int kind=state.getValue(DrainhouseMark.KIND);check(markers[kind]==null,"Registered feature emits exactly one actual marker per room");check(s.overworld().getBlockEntity(at) instanceof DrainhouseMarkEntity e && e.authentic(),"Actual registered feature commits authentic room marker");markers[kind]=at;}}for(var at:markers)check(at!=null,"Registered feature supplies all three room models");});
   c.waitTicks(30);
   String[] rooms={"threshold","garden","alcove"};
   for(int i=0;i<3;i++){
    final var at=markers[i];w.getServer().runOnServer(s -> {var focus=Vec3.atCenterOf(at);look(p(s),focus.add(-1.15,.68,-1.8),focus.add(0,.10,0));});c.waitTicks(18);
    check(c.computeOnClient(mc -> mc.level.getBlockState(at).is(DrainhouseContent.MARK)),"Actual room marker reached client before screenshot");shot(c,"belowkeeper_room_"+rooms[i]);
   }
   // Broader views supplement the close marker evidence; no authored blocks are moved.
   var center=markers[1].offset(0,0,-1);
   w.getServer().runOnServer(srv -> {var focus=Vec3.atLowerCornerOf(center).add(.5,.20,-2.15);var eye=Vec3.atLowerCornerOf(center).add(1.8,2.4,2.4);look(p(srv),eye,focus);
    for(int dx:new int[]{-1,1})check(srv.overworld().getBlockState(center.offset(dx,0,-2)).is(FungalGarden.GLOWCAP),"Both actual garden buds remain in functional room");
    for(int dx=-2;dx<=2;dx++)check(!srv.overworld().getFluidState(center.offset(dx,-1,-3)).isEmpty(),"Actual five-cell functional water gutter remains present");
   });c.waitTicks(18);shot(c,"belowkeeper_garden_gutter_and_two_buds");
   w.getServer().runOnServer(srv -> {var eye=Vec3.atLowerCornerOf(center).add(-9.3,1.65,.5);var focus=Vec3.atLowerCornerOf(center).add(-4.8,.80,1.15);look(p(srv),eye,focus);
    var hit=srv.overworld().clip(new net.minecraft.world.level.ClipContext(eye,focus,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p(srv)));
    check(hit.getType()==net.minecraft.world.phys.HitResult.Type.MISS,"Actual loaded entrance aperture provides an unobstructed threshold overview");
   });c.waitTicks(18);shot(c,"belowkeeper_threshold_entrance_overview");
   // Open equipment review pad, separate from the shaded architecture. No earned-item assertion.
   w.getServer().runOnServer(s -> {var l=s.overworld();for(int x=22;x<=32;x++)for(int z=-5;z<=5;z++){l.setBlock(new BlockPos(x,29,z),Blocks.STONE_BRICKS.defaultBlockState(),2);for(int y=30;y<=35;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}var player=p(s);player.setGameMode(GameType.SURVIVAL);player.setItemSlot(EquipmentSlot.FEET,new ItemStack(BelowkeeperEquipment.GREAVES));player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(BelowkeeperEquipment.BELL));player.getInventory().setItem(9,new ItemStack(DrainhouseContent.ENTRANCE_NOTES));player.getInventory().setItem(10,new ItemStack(DrainhouseContent.GARDEN_NOTES));player.getInventory().setItem(11,new ItemStack(DrainhouseContent.ALCOVE_NOTES));player.getInventory().setItem(12,new ItemStack(BelowkeeperEquipment.GREAVES));player.getInventory().setItem(13,new ItemStack(BelowkeeperEquipment.BELL));player.getInventory().setItem(14,new ItemStack(RootmoltContent.EGG));player.teleportTo(l,27.5,30,.5,Set.<Relative>of(),0,12,false);player.inventoryMenu.broadcastChanges();});
   c.waitTicks(25);check(c.computeOnClient(mc -> mc.player.getMainHandItem().is(BelowkeeperEquipment.BELL) && mc.player.getItemBySlot(EquipmentSlot.FEET).is(BelowkeeperEquipment.GREAVES)),"Actual supplied equipment reaches client hand/feet");c.runOnClient(mc -> {if(mc.gui.hud.isHidden())mc.gui.hud.toggle();});c.waitTicks(5);shot(c,"belowkeeper_bell_held_first_person");
   c.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));c.waitTicks(18);
   check(c.computeOnClient(mc -> mc.gameRenderer.mainCamera().entity()==mc.player && mc.gameRenderer.mainCamera().isDetached()),"Native actual-player third-person camera renders real worn equipment");shot(c,"belowkeeper_greaves_worn_third_person");
   c.runOnClient(mc -> {if(mc.gui.hud.isHidden())mc.gui.hud.toggle();mc.gui.setScreen(new InventoryScreen(mc.player));});c.waitTicks(12);
   check(c.computeOnClient(mc -> mc.gui.screen() instanceof InventoryScreen),"Actual inventory screen shows supplied original icons");shot(c,"belowkeeper_equipment_and_book_icons");
  }finally{c.runOnClient(mc -> {mc.gui.setScreen(screen);mc.options.setCameraType(camera);if(mc.player!=null)mc.setCameraEntity(mc.player);if(mc.gui.hud.isHidden()!=hidden)mc.gui.hud.toggle();mc.options.guiScale().set(scale);mc.getWindow().setWindowed(size[0],size[1]);mc.resizeGui();var k=new net.minecraft.client.KeyMapping[]{mc.options.keyUp,mc.options.keyDown,mc.options.keyLeft,mc.options.keyRight,mc.options.keyShift,mc.options.keyJump,mc.options.keyUse,mc.options.keyAttack};for(int i=0;i<k.length;i++)k[i].setDown(keys[i]);});}
 }
 private static void look(net.minecraft.server.level.ServerPlayer player,Vec3 eye,Vec3 focus){var d=focus.subtract(eye);float yaw=(float)Math.toDegrees(Math.atan2(-d.x,d.z));float pitch=(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z)));player.teleportTo(player.level(),eye.x,eye.y-player.getEyeHeight(),eye.z,Set.<Relative>of(),yaw,pitch,false);}
 private static void shot(ClientGameTestContext c,String name){c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());}
}
