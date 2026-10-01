package dev.wildercord.cast;
import dev.wildercord.gear.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Verify actual deferred harm, item removal and the control recharge against a real player. */
public final class DefensiveFociTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext context) {
  try(var world=context.worldBuilder().create()) {
   context.waitTicks(35);
   world.getServer().runOnServer(server -> {
    var p=server.getPlayerList().getPlayers().getFirst();
    p.setGameMode(GameType.SURVIVAL); p.setHealth(20); p.getFoodData().setFoodLevel(0);
    GearSlots.set(p,GearSlot.FOCUS,new ItemStack(GearItems.get(GearDef.REPRIEVE)));
    Effects.readyToHurt(p);
    check(SpellDefence.hurt(p.level(),p,p.damageSources().magic(),8),"reprieve hit lands");
    close(p.getHealth(),14.8,"reprieve immediate damage");
    close(p.getAttachedOrElse(DefensiveFoci.STATE,DefensiveFoci.State.EMPTY).owed(),2.8,"reprieve exact debt");
    // The serialized state must retain the debt; removing the item cannot cancel payment.
    var state=p.getAttachedOrElse(DefensiveFoci.STATE,DefensiveFoci.State.EMPTY);
    var json=DefensiveFoci.State.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,state).getOrThrow();
    check(state.equals(DefensiveFoci.State.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,json).getOrThrow()),"debt persists in save data");
    GearSlots.clear(p,GearSlot.FOCUS);
   });
   context.waitTicks(55);
   world.getServer().runOnServer(server -> {
    var p=server.getPlayerList().getPlayers().getFirst();
    close(p.getHealth(),12,"all delayed damage is paid after focus removal");
    close(p.getAttachedOrElse(DefensiveFoci.STATE,DefensiveFoci.State.EMPTY).owed(),0,"debt clears");
    GearSlots.set(p,GearSlot.FOCUS,new ItemStack(GearItems.get(GearDef.REPRIEVE)));
    Effects.readyToHurt(p); SpellDefence.hurt(p.level(),p,p.damageSources().magic(),6);
    close(p.getHealth(),6,"reprieve does not retrigger during recharge");
    GearSlots.set(p,GearSlot.FOCUS,new ItemStack(GearItems.get(GearDef.GROUNDING)));
    p.setDeltaMovement(Vec3.ZERO); Effects.push(p,new Vec3(0,1,0));
    close(p.getDeltaMovement().y,.3,"grounding resists the first launch");
    p.setDeltaMovement(Vec3.ZERO); Effects.push(p,new Vec3(0,1,0));
    close(p.getDeltaMovement().y,1,"grounding recharge prevents repeated immunity");
   });
  }
 }
 private static void check(boolean v,String message) { if(!v)throw new AssertionError(message); }
 private static void close(double actual,double expected,String message) { check(Math.abs(actual-expected)<.03,message+": "+actual+" != "+expected); }
}
