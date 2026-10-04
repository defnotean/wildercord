package dev.wildercord.wildlife;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
/** A short ownership-checked physical root restraint; invalid sources release on the next tick. */
public final class RootTetherEffect extends MobEffect {
 public RootTetherEffect() {super(MobEffectCategory.HARMFUL,0x867456);addAttributeModifier(Attributes.MOVEMENT_SPEED,dev.wildercord.Wildercord.id("root_tether"),-.65,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);}
 @Override public boolean shouldApplyEffectTickThisTick(int duration,int amplifier) {return true;}
 @Override public boolean applyEffectTick(ServerLevel l,LivingEntity victim,int amplifier) {
  var owner=victim.getAttachedOrElse(RootmoltContent.GRAB_OWNER,RootmoltContent.NO_OWNER);
  if(!(l.getEntity(owner) instanceof RootmoltStrider source) || !source.isAlive() || source.isRemoved() || !victim.isAlive() || victim.isRemoved() || (victim instanceof net.minecraft.world.entity.player.Player p && (p.isSpectator() || p.isCreative())) || !source.holding(victim) || source.distanceToSqr(victim)>16 || !source.hasLineOfSight(victim)) {victim.setAttached(RootmoltContent.GRAB_OWNER,RootmoltContent.NO_OWNER);return false;}
  return true;
 }
}
