package dev.wildercord.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.client.wildlife.RootmoltModel;
import dev.wildercord.client.wildlife.RootmoltRenderer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Actual tracked custom rig/material and natural browse/warning/contact/miss phases. No pose or timer writes. */
public final class RootmoltPresentationTest implements FabricClientGameTest {
 private RootmoltStrider source;private Zombie victim;private float contactHealth,missHealth;
 private record Pose(float watch,float strike,float graze,float left,float right,float head,float gill){}
 @Override public void runTest(ClientGameTestContext c){
  var previousCamera=c.computeOnClient(mc->mc.options.getCameraType());
  boolean hidden=c.computeOnClient(mc->mc.gui.hud.isHidden());
  int[] window=c.computeOnClient(mc->new int[]{mc.getWindow().getWidth(),mc.getWindow().getHeight()});
  try(var w=c.worldBuilder().create()){
   c.waitTicks(25);w.getServer().runCommand("difficulty normal");w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");
   w.getServer().runOnServer(s->{var l=s.overworld();for(int x=-9;x<=9;x++)for(int z=-9;z<=9;z++){
    l.setBlock(new BlockPos(x,29,z),Blocks.MOSS_BLOCK.defaultBlockState(),2);for(int y=30;y<=32;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);l.setBlock(new BlockPos(x,33,z),Blocks.STONE.defaultBlockState(),2);}
    var cap=new BlockPos(0,30,0);l.setBlock(cap.below().east(),Blocks.WATER.defaultBlockState(),2);l.setBlock(cap,FungalGarden.GLOWCAP.defaultBlockState().setValue(GlowcapBlock.AGE,2),2);
    source=RootmoltContent.STRIDER.create(l,EntitySpawnReason.COMMAND);check(source!=null,"Actual Rootmolt fixture");source.setPersistenceRequired();source.snapTo(-1.5,30,.5,0,0);l.addFreshEntity(source);
    var viewer=s.getPlayerList().getPlayers().getFirst();viewer.setGameMode(GameType.SPECTATOR);viewer.setNoGravity(true);viewer.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,1200,0,false,false,false));camera(viewer,source);
   });
   c.runOnClient(mc->{mc.options.setCameraType(CameraType.FIRST_PERSON);mc.getWindow().setWindowed(1280,720);mc.resizeGui();if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();});
   int id=source.getId();awaitClient(c,id,e->true,"Actual entity reaches client tracking");
   Pose before=inspect(c,id,true);shot(c,"rootmolt_material_before_browse");
   awaitClient(c,id,e->e.pose()==RootmoltStrider.BROWSING && e.feed>=.55F,"Real close uninterrupted browse reaches its actual client blend");
   Pose browse=inspect(c,id,false);check(browse.graze()>.5F,"Registered renderer reads actual browse blend");
   check(Math.abs(browse.head()-before.head())>.06F,"Actual browse changes the head/feeding posture");shot(c,"rootmolt_material_actual_browse");
   c.waitTicks(4);Pose breathing=inspect(c,id,false);check(Math.abs(breathing.gill()-browse.gill())>.0001F,"Actual elapsed client age moves the exposed gills");
   awaitServer(c,w,s->source.mealReady()>0,"Real browse consumes the actual mature cap");
   w.getServer().runOnServer(s->{check(s.overworld().getBlockState(new BlockPos(0,30,0)).getValue(GlowcapBlock.AGE)==0,"Actual feeding reset remains authoritative");
    victim=new Zombie(EntityTypes.ZOMBIE,s.overworld());victim.setNoAi(true);victim.setCustomName(net.minecraft.network.chat.Component.literal("Rootmolt presentation target"));victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS).setBaseValue(0);check(victim.getHealth()==20 && victim.getArmorValue()==0,"Actual unarmored full-health physical target baseline");victim.snapTo(source.getX()+2,30,source.getZ(),0,0);s.overworld().addFreshEntity(victim);contactHealth=victim.getHealth();check(contactHealth==victim.getMaxHealth() && victim.getArmorValue()==0,"Admitted target has actual full post-spawn health and no armor");source.setTarget(victim);camera(s.getPlayerList().getPlayers().getFirst(),source);});
   awaitClient(c,id,e->e.pose()==RootmoltStrider.WARNING && e.warn>=.8F,"Actual warning reaches raised asymmetric shovel pose");
   Pose warning=inspect(c,id,false);check(warning.watch()>.75F && warning.left()<before.left()-.5F,"Renderer/model raise the real warning arms");
   check(Math.abs(warning.left()-warning.right())>.12F,"Original unequal shovel warning is retained");shot(c,"rootmolt_material_actual_warning");
   awaitClient(c,id,e->e.pose()==RootmoltStrider.HOLDING && e.rakeO+(e.rake-e.rakeO)*.5F>=.8F,"Real physical contact reaches its actual committed strike blend");
   Pose contact=inspect(c,id,false);check(contact.strike()>.75F && contact.left()>warning.left()+.8F,"Actual contact sweeps the shovel instead of retaining warning posture");
   w.getServer().runOnServer(s->{Wildercord.LOGGER.info("ROOTMOLT_CONTACT baseline={} health={} armor={} sourcePose={} held={} effect={} owner={} source={}",contactHealth,victim.getHealth(),victim.getArmorValue(),source.pose(),source.holding(victim),victim.hasEffect(RootmoltContent.TETHER),victim.getAttachedOrElse(RootmoltContent.GRAB_OWNER,RootmoltContent.NO_OWNER),source.getUUID());check(victim.getHealth()==contactHealth-4 && source.holding(victim),"Actual four-damage contact owns the captured hold");});shot(c,"rootmolt_material_actual_rake_contact");
   awaitClient(c,id,e->e.pose()==RootmoltStrider.RECOVERING && e.rake<.15F && e.warn<.15F,"Natural finite hold drops back into recovery");
   Pose recovery=inspect(c,id,false);check(Math.abs(recovery.left()-before.left())<.4F,"Actual recovery returns the arms toward rest");shot(c,"rootmolt_material_after_recovery");
   // A second real AI encounter misses after ordinary collision-respecting target movement.
   w.getServer().runOnServer(s->{Vec3 place=source.position();source.discard();victim.discard();source=RootmoltContent.STRIDER.create(s.overworld(),EntitySpawnReason.COMMAND);source.setPersistenceRequired();source.snapTo(place.x,place.y,place.z,0,0);source.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0);s.overworld().addFreshEntity(source);
    victim=new Zombie(EntityTypes.ZOMBIE,s.overworld());victim.setNoAi(true);victim.setCustomName(net.minecraft.network.chat.Component.literal("Rootmolt presentation target"));victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS).setBaseValue(0);check(victim.getHealth()==20 && victim.getArmorValue()==0,"Actual unarmored full-health physical target baseline");victim.snapTo(source.getX()+2,30,source.getZ(),0,0);s.overworld().addFreshEntity(victim);missHealth=victim.getHealth();check(missHealth==victim.getMaxHealth() && victim.getArmorValue()==0,"Admitted miss target has actual full post-spawn health and no armor");source.setTarget(victim);camera(s.getPlayerList().getPlayers().getFirst(),source);});
   int missId=source.getId();awaitClient(c,missId,e->e.pose()==RootmoltStrider.WARNING && e.warn>.5F,"Second actual warning commits its own line");
   w.getServer().runOnServer(s->{double z=victim.getZ();victim.move(MoverType.SELF,new Vec3(0,0,1.5));check(victim.getZ()-z>1.4,"Ordinary target movement really clears the committed line");});
   awaitClient(c,missId,e->e.pose()==RootmoltStrider.RAKING && e.rake>=.2F,"An actual missed rake exposes the live RAKING state");
   inspect(c,missId,false);shot(c,"rootmolt_material_actual_missed_rake");
   awaitServer(c,w,s->source.pose()==RootmoltStrider.RECOVERING,"Missed rake ends through its ordinary finite clock");
   w.getServer().runOnServer(s->check(victim.getHealth()==missHealth && !victim.hasEffect(RootmoltContent.TETHER),"Miss captured no wound or control"));
  }finally{
   source=null;victim=null;c.runOnClient(mc->{mc.options.setCameraType(previousCamera);if(mc.gui.hud.isHidden()!=hidden)mc.gui.hud.toggle();mc.getWindow().setWindowed(window[0],window[1]);mc.resizeGui();});
  }
 }
 private static void camera(net.minecraft.server.level.ServerPlayer viewer,RootmoltStrider source){
  Vec3 focus=source.getBoundingBox().getCenter().add(0,.08,0),feet=source.position().add(3.4,.6,-3.2),d=focus.subtract(feet.add(0,viewer.getEyeHeight(),0));
  viewer.teleportTo(viewer.level(),feet.x,feet.y,feet.z,Set.<Relative>of(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())),false);
 }
 private static Pose inspect(ClientGameTestContext c,int id,boolean texture){return c.computeOnClient(mc->{
  check(mc.level.getEntity(id) instanceof RootmoltStrider,"Actual tracked Rootmolt type");var e=(RootmoltStrider)mc.level.getEntity(id);
  var raw=mc.getEntityRenderDispatcher().getRenderer(e);check(raw instanceof RootmoltRenderer,"Actual entity type selects the registered original renderer");var renderer=(RootmoltRenderer)raw;
  var state=renderer.createRenderState(e,.5F);RootmoltModel model=renderer.getModel();model.setupAnim(state);
  ModelPart[] legs=field(model,"legs",ModelPart[].class);check(legs.length==6,"Actual custom rig has six legs");var unique=Collections.newSetFromMap(new IdentityHashMap<ModelPart,Boolean>());
  for(var leg:legs){check(unique.add(leg),"Every leg is an independent model part");check(!leg.getChild("tibia").isEmpty(),"Each actual leg has its lower articulated segment");}
  var abdomen=field(model,"abdomen",ModelPart.class);for(int side:new int[]{-1,1})for(int k=0;k<3;k++)check(!abdomen.getChild("gill_"+side+"_"+k).isEmpty(),"Actual rig carries all six exposed material gills");
  ModelPart left=field(model,"leftShovel",ModelPart.class),right=field(model,"rightShovel",ModelPart.class),head=field(model,"head",ModelPart.class);
  check(!left.getChild("edge").isEmpty() && !right.getChild("edge").isEmpty(),"Both actual shovels retain their carved edges");
  check(renderer.getTextureLocation(state).equals(Wildercord.id("textures/entity/rootmolt_strider.png")),"Renderer binds the authored Rootmolt material");
  if(texture)try(var stream=mc.getResourceManager().getResource(renderer.getTextureLocation(state)).orElseThrow().open()){
   var image=javax.imageio.ImageIO.read(stream);check(image!=null && image.getWidth()==128 && image.getHeight()==128,"Actual material resource loads at authored atlas size");var colors=new HashSet<Integer>();int opaque=0;
   for(int y=0;y<128;y++)for(int x=0;x<128;x++){int argb=image.getRGB(x,y);if((argb>>>24)>0){opaque++;colors.add(argb);}}
   check(opaque>500 && colors.size()>24,"Loaded material has opaque authored detail beyond a missing/two-color texture");
  }catch(java.io.IOException failure){throw new AssertionError(failure);}
  Wildercord.LOGGER.info("ROOTMOLT_PRESENTATION actual={} clientPose={} watch={} strike={} graze={} left={} right={}",id,e.pose(),state.watch,state.strike,state.graze,left.xRot,right.xRot);
  return new Pose(state.watch,state.strike,state.graze,left.xRot,right.xRot,head.xRot,abdomen.getChild("gill_1_0").zRot);
 });}
 private static <T>T field(Object owner,String name,Class<T> type){try{var f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return type.cast(f.get(owner));}catch(ReflectiveOperationException failure){throw new AssertionError(failure);}}
 private static void awaitClient(ClientGameTestContext c,int id,java.util.function.Predicate<RootmoltStrider> yes,String why){for(int i=0;i<600;i++){c.waitTicks(1);if(c.computeOnClient(mc->mc.level!=null && mc.level.getEntity(id) instanceof RootmoltStrider e && yes.test(e)))return;}throw new AssertionError(why);}
 private static void awaitServer(ClientGameTestContext c,TestSingleplayerContext w,java.util.function.Predicate<net.minecraft.server.MinecraftServer> yes,String why){for(int i=0;i<600;i++){c.waitTicks(1);if(w.getServer().computeOnServer(yes::test))return;}throw new AssertionError(why);}
 private static void shot(ClientGameTestContext c,String name){c.runOnClient(mc->{mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());}
 private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
}
