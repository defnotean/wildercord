package dev.wildercord.cast;
import dev.wildercord.gear.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
/** Cross-check typed deferred damage, armour and control resistance, including deliberate recharge gaps. */
public final class ControlMatchupsTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext c){try(var w=c.worldBuilder().create()){
  c.waitTicks(35);float baseline=w.getServer().computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.getFoodData().setFoodLevel(0);
   p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.IRON_CHESTPLATE));p.setItemSlot(EquipmentSlot.LEGS,new ItemStack(Items.IRON_LEGGINGS));p.setItemSlot(EquipmentSlot.FEET,new ItemStack(Items.IRON_BOOTS));
   p.setHealth(20);Effects.readyToHurt(p);SpellDefence.hurt(p.level(),p,p.damageSources().inFire(),8);float damage=20-p.getHealth();
   p.setHealth(20);GearSlots.set(p,GearSlot.FOCUS,new ItemStack(GearItems.get(GearDef.REPRIEVE)));p.setAttached(DefensiveFoci.STATE,DefensiveFoci.State.EMPTY);Effects.readyToHurt(p);
   SpellDefence.hurt(p.level(),p,p.damageSources().inFire(),8);check(20-p.getHealth()<damage,"reprieve creates an initial response window against typed damage");return damage;
  });c.waitTicks(55);
  w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(20-p.getHealth()<=baseline+.05,"deferred fire damage retains native armour instead of becoming armour-piercing magic");
   check(p.getAttachedOrElse(DefensiveFoci.STATE,DefensiveFoci.State.EMPTY).owed()==0,"typed debt finishes");
   GearSlots.set(p,GearSlot.FOCUS,new ItemStack(GearItems.get(GearDef.GROUNDING)));p.setAttached(DefensiveFoci.STATE,DefensiveFoci.State.EMPTY);
   p.setDeltaMovement(Vec3.ZERO);Effects.push(p,new Vec3(.8,.6,0));check(p.getDeltaMovement().length()<.31,"grounding permits recovery from the first shove");
   GearSlots.clear(p,GearSlot.FOCUS);GearSlots.set(p,GearSlot.FOCUS,new ItemStack(GearItems.get(GearDef.GROUNDING)));p.setDeltaMovement(Vec3.ZERO);Effects.push(p,new Vec3(.8,.6,0));check(p.getDeltaMovement().length()>.99,"swapping focus cannot reset grounding recharge");
   CastLock.lock(p,200);check(CastLock.locked(p),"first silence applies");
  });
  c.waitTicks(20);w.getServer().runOnServer(s->CastLock.lock(s.getPlayerList().getPlayers().getFirst(),200));c.waitTicks(25);
  w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(!CastLock.locked(p),"repeated silence cannot extend the two-second player cap");CastLock.lock(p,200);check(!CastLock.locked(p),"recovery window allows a cast despite another silence hit");});
  c.waitTicks(40);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();CastLock.lock(p,20);check(CastLock.locked(p),"silence can apply again after recovery");});
 }}private static void check(boolean v,String label){if(!v)throw new AssertionError(label);}
}
