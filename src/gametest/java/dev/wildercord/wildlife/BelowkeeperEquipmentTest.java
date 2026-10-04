package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.BelowkeeperTestSupport.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
/** Combat items are explicitly supplied here; the separate acquisition suite proves their earned path. */
public final class BelowkeeperEquipmentTest implements FabricClientGameTest {
 private RootmoltStrider source;
 public void runTest(ClientGameTestContext c){boolean shift=c.computeOnClient(mc -> mc.options.keyShift.isDown()),use=c.computeOnClient(mc -> mc.options.keyUse.isDown()),jump=c.computeOnClient(mc -> mc.options.keyJump.isDown()),right=c.computeOnClient(mc -> mc.options.keyRight.isDown());long bellRest,bootsRest;TestWorldSave save;
  try{
   try(var w=c.worldBuilder().create()){
    c.waitTicks(25);w.getServer().runCommand("difficulty normal");w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");
    w.getServer().runOnServer(s -> {var l=s.overworld();for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){l.setBlock(new BlockPos(x,29,z),Blocks.STONE.defaultBlockState(),2);for(int y=30;y<=33;y++)l.setBlock(new BlockPos(x,y,z),y==33?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);}p(s).setGameMode(GameType.SURVIVAL);move(p(s),2,30,.5);hand(p(s),new ItemStack(BelowkeeperEquipment.GREAVES));});
    c.waitTicks(5);c.runOnClient(mc -> mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND));c.waitTicks(5);w.getServer().runOnServer(s -> check(p(s).getItemBySlot(EquipmentSlot.FEET).is(BelowkeeperEquipment.GREAVES),"Actual native use equips the armor feet slot"));
    c.runOnClient(mc -> mc.options.keyShift.setDown(true));
    w.getServer().runOnServer(s -> p(s).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS,80,3)));c.waitTicks(4);
    var creepStart=w.getServer().computeOnServer(s -> p(s).position());var creepPrevious=creepStart;
    c.runOnClient(mc -> mc.options.keyRight.setDown(true));
    for(int i=0;i<40;i++){c.waitTicks(1);var current=w.getServer().computeOnServer(s -> p(s).position());check(current.distanceToSqr(creepPrevious)<.0009,"Actual slowed native crouch creep stays below the old per-tick tolerance");creepPrevious=current;}
    c.runOnClient(mc -> mc.options.keyRight.setDown(false));
    w.getServer().runOnServer(s -> {check(p(s).position().distanceToSqr(creepStart)>.01,"Actual native creeping traverses a measurable total distance");check(BelowkeeperEquipment.prepared(p(s))<BelowkeeperEquipment.PREP_TICKS,"Cumulative creeping cannot earn a planted stance");p(s).removeEffect(net.minecraft.world.effect.MobEffects.SLOWNESS);move(p(s),2,30,.5);});c.waitTicks(40);
    w.getServer().runOnServer(s -> check(BelowkeeperEquipment.prepared(p(s))==BelowkeeperEquipment.PREP_TICKS,"First real still stance earns preparation"));
    c.runOnClient(mc -> mc.options.keyJump.setDown(true));c.waitTicks(3);c.runOnClient(mc -> mc.options.keyJump.setDown(jump));
    w.getServer().runOnServer(s -> check(!p(s).onGround() && BelowkeeperEquipment.prepared(p(s))==0,"Actual native jump cancels anchored feet immediately"));
    c.waitTicks(55);w.getServer().runOnServer(s -> {check(BelowkeeperEquipment.prepared(p(s))==BelowkeeperEquipment.PREP_TICKS,"Actual landing and still crouch must earn preparation anew");hand(p(s),new ItemStack(BelowkeeperEquipment.GREAVES));});c.waitTicks(5);
    var previousBoots=w.getServer().computeOnServer(srv -> p(srv).getItemBySlot(EquipmentSlot.FEET));
    c.runOnClient(mc -> mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId,8,mc.player.getInventory().getSelectedSlot(),net.minecraft.world.inventory.ContainerInput.SWAP,mc.player));c.waitTicks(3);
    w.getServer().runOnServer(s -> {dev.wildercord.Wildercord.LOGGER.info("BELOWKEEPER_SWAP prepared={} crouched={} hand={} feet={}",BelowkeeperEquipment.prepared(p(s)),p(s).isShiftKeyDown(),p(s).getMainHandItem(),p(s).getItemBySlot(EquipmentSlot.FEET));check(p(s).getItemBySlot(EquipmentSlot.FEET)!=previousBoots && BelowkeeperEquipment.prepared(p(s))<BelowkeeperEquipment.PREP_TICKS && p(s).getItemBySlot(EquipmentSlot.FEET).is(BelowkeeperEquipment.GREAVES),"Actual native equipment swap cannot transfer prepared stack identity");});
    c.runOnClient(mc -> mc.options.keyShift.setDown(true));c.waitTicks(40);w.getServer().runOnServer(s -> {check(BelowkeeperEquipment.prepared(p(s))==BelowkeeperEquipment.PREP_TICKS,"Actual still crouched physics earns preparation");spawn(s);});
    await(c,w,s -> p(s).getItemBySlot(EquipmentSlot.FEET).getDamageValue()>=8,"Actual physical contact spends finite prepared armor wear");
    w.getServer().runOnServer(s -> {check(p(s).getHealth()<20,"Anchoring does not undo the physical wound");check(!p(s).hasEffect(RootmoltContent.TETHER) && !source.holding(p(s)),"Prepared worn greaves deny only that root control");check(p(s).getAttachedOrElse(BelowkeeperEquipment.GREAVES_READY,0L)>s.overworld().getGameTime() && BelowkeeperEquipment.prepared(p(s))==0,"A real anchor spends shared saved rest and preparation");source.discard();hand(p(s),new ItemStack(BelowkeeperEquipment.BELL));spawn(s);});
    bootsRest=w.getServer().computeOnServer(s -> p(s).getAttachedOrElse(BelowkeeperEquipment.GREAVES_READY,0L));await(c,w,s -> source.pose()==RootmoltStrider.WARNING,"Second fixture source gives a real visible bell warning");
    // A valid start does not authorize a later through-wall finish; no source clocks/poses are forced.
    c.runOnClient(mc -> {mc.options.keyUse.setDown(true);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);});c.waitTicks(6);
    w.getServer().runOnServer(s -> {for(int y=30;y<=32;y++)s.overworld().setBlock(new BlockPos(1,y,0),Blocks.STONE.defaultBlockState(),3);});c.waitTicks(19);c.runOnClient(mc -> mc.options.keyUse.setDown(use));
    w.getServer().runOnServer(s -> {check(p(s).getMainHandItem().getDamageValue()==0 && p(s).getAttachedOrElse(BelowkeeperEquipment.BELL_READY,0L)==0,"Actual obstruction during commitment denies bell wear/reward/rest");for(int y=30;y<=32;y++)s.overworld().setBlock(new BlockPos(1,y,0),Blocks.AIR.defaultBlockState(),3);source.setTarget(p(s));});
    await(c,w,s -> source.pose()==RootmoltStrider.IDLE,"Obstructed fixture source completes its ordinary rake and recovery");
    w.getServer().runOnServer(s -> {dev.wildercord.Wildercord.LOGGER.info("BELOWKEEPER_REARM player={} source={} health={} attackReady={} now={}",p(s).position(),source.position(),p(s).getHealth(),source.attackReady(),s.overworld().getGameTime());source.setTarget(p(s));});
    await(c,w,s -> source.pose()==RootmoltStrider.WARNING,"Unobstructed source earns a subsequent ordinary warning after its actual rest");
    c.runOnClient(mc -> {mc.options.keyUse.setDown(true);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);});c.waitTicks(24);c.runOnClient(mc -> mc.options.keyUse.setDown(use));
    w.getServer().runOnServer(s -> {check(source.pose()==RootmoltStrider.RECOVERING && p(s).getMainHandItem().getDamageValue()==1,"Full native bell commitment interrupts exactly one warning with one wear");check(p(s).hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS) && p(s).hasEffect(net.minecraft.world.effect.MobEffects.MINING_FATIGUE),"The committed bell leaves actual heavy steps/hands");check(!p(s).hasEffect(RootmoltContent.TETHER),"The interrupted warning never applies root control");});
    bellRest=w.getServer().computeOnServer(s -> p(s).getAttachedOrElse(BelowkeeperEquipment.BELL_READY,0L));
    w.getServer().runOnServer(s -> {source.discard();hand(p(s),new ItemStack(BelowkeeperEquipment.BELL));move(p(s),2,30,.5);spawn(s);check(p(s).getAttachedOrElse(BelowkeeperEquipment.BELL_READY,0L)==bellRest&&bellRest>s.overworld().getGameTime(),"Ordinary fresh-copy scene reposition preserves actual still-active saved bell rest");check(source.distanceToSqr(p(s))<9&&source.hasLineOfSight(p(s)),"Fresh actual warning source begins safely inside original four-block reach with real line of sight");});
    try{await(c,w,s -> source.pose()==RootmoltStrider.WARNING,"Another live warning tests the fresh-copy shared rest");}catch(AssertionError failure){throw new AssertionError(w.getServer().computeOnServer(s -> "Fresh-copy actual warning timeout: player="+p(s).position()+" playerAlive="+p(s).isAlive()+" playerGround="+p(s).onGround()+" source="+source.position()+" sourceAlive="+source.isAlive()+" sourceRemoved="+source.isRemoved()+" sourceGround="+source.onGround()+" pose="+source.pose()+" target="+(source.getTarget()==null?null:source.getTarget().getUUID())+" distanceSquared="+source.distanceToSqr(p(s))+" sight="+source.hasLineOfSight(p(s))+" attackReady="+source.attackReady()+" bellRest="+p(s).getAttachedOrElse(BelowkeeperEquipment.BELL_READY,0L)+" now="+s.overworld().getGameTime()),failure);}
    w.getServer().runOnServer(s -> check(source.onGround()&&p(s).onGround()&&source.distanceToSqr(p(s))<16&&source.hasLineOfSight(p(s))&&p(s).getAttachedOrElse(BelowkeeperEquipment.BELL_READY,0L)==bellRest&&bellRest>s.overworld().getGameTime(),"Actual live warning is eligible while the same earned bell deadline remains active"));c.runOnClient(mc -> mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND));c.waitTicks(5);
    w.getServer().runOnServer(s -> {check(p(s).getMainHandItem().getDamageValue()==0 && p(s).getAttachedOrElse(BelowkeeperEquipment.BELL_READY,0L)==bellRest,"A fresh copy cannot evade saved bell rest or renew its deadline");source.discard();});
    c.waitTicks(40);w.getServer().runOnServer(s -> check(BelowkeeperEquipment.prepared(p(s))==BelowkeeperEquipment.PREP_TICKS,"After actual elapsed armor rest, a new still stance is earned before saving"));
    c.takeScreenshot(TestScreenshotOptions.of("belowkeeper_real_anchor_bell_and_copy_rest").disableCounterPrefix());save=w.getWorldSave();
   }
   try(var w=save.open()){c.waitTicks(8);w.getServer().runOnServer(s -> {check(p(s).getAttachedOrElse(BelowkeeperEquipment.BELL_READY,0L)==bellRest && p(s).getAttachedOrElse(BelowkeeperEquipment.GREAVES_READY,0L)==bootsRest,"Full world restart preserves exact independent equipment rests");check(p(s).getCooldowns().isOnCooldown(new ItemStack(BelowkeeperEquipment.BELL)),"Join restores visible cooldown for the fresh bell copy");check(BelowkeeperEquipment.prepared(p(s))<BelowkeeperEquipment.PREP_TICKS,"Rejoining restarts actual preparation instead of restoring a ready stance");});}
  }finally{c.runOnClient(mc -> {mc.options.keyShift.setDown(shift);mc.options.keyUse.setDown(use);mc.options.keyJump.setDown(jump);mc.options.keyRight.setDown(right);});}
 }
 private void spawn(net.minecraft.server.MinecraftServer s){source=RootmoltContent.STRIDER.create(s.overworld(),EntitySpawnReason.COMMAND);source.snapTo(0,30,.5,0,0);s.overworld().addFreshEntity(source);source.setTarget(p(s));}
}
