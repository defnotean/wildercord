package dev.wildercord.gametest.nursery.mixin;

import dev.wildercord.wildlife.FungalNurseryTest;
import dev.wildercord.wildlife.SporebackSnail;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native-test-only read-only receipt trace; never changes an interaction result. */
@Mixin(SporebackSnail.class)
public abstract class SporebackGatherTraceMixin {
 @Inject(method="mobInteract",at=@At("HEAD"))
 private void received(Player p,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){
  var snail=(SporebackSnail)(Object)this;
  if(FungalNurseryTest.tracingGather() && snail.level() instanceof ServerLevel l)
   dev.wildercord.Wildercord.LOGGER.info("FUNGAL_GATHER_MOB_HEAD now={} player={} shift={} hand={} held={} alive={} spectator={} worldSame={} distance={} visible={} snailAlive={} snailRemoved={} pose={} dew={} hidden={} gather={}",l.getGameTime(),p.position(),p.isShiftKeyDown(),hand,p.getItemInHand(hand),p.isAlive(),p.isSpectator(),p.level()==snail.level(),p.distanceToSqr(snail),p.hasLineOfSight(snail),snail.isAlive(),snail.isRemoved(),snail.pose(),snail.dew(),snail.hiddenUntil(),snail.gatherReady());
 }
 @Inject(method="mobInteract",at=@At("RETURN"))
 private void returned(Player p,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){
  var snail=(SporebackSnail)(Object)this;
  if(FungalNurseryTest.tracingGather() && snail.level() instanceof ServerLevel l)
   dev.wildercord.Wildercord.LOGGER.info("FUNGAL_GATHER_MOB_RETURN now={} result={} held={} dew={} gather={}",l.getGameTime(),ci.getReturnValue(),p.getItemInHand(hand),snail.dew(),snail.gatherReady());
 }
}
