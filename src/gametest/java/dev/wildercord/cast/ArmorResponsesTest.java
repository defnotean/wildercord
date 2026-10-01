package dev.wildercord.cast;
import dev.wildercord.gear.ElementalArmor;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public final class ArmorResponsesTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext context) {
  try(var world=context.worldBuilder().create()) {
   context.waitTicks(35);
   world.getServer().runCommand("gamerule spawn_mobs false");
   world.getServer().runCommand("time set 6000");
   world.getServer().runCommand("fill -6 98 -6 6 98 6 stone");
   world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);
    p.teleportTo(p.level(),0,99,0,java.util.Set.<net.minecraft.world.entity.Relative>of(),0,0,false);
    check(ElementalArmor.ALL.size()==13,"three complete sets and one mantle");equip(p,ElementalArmor.Kind.STONEBOUND);
   });
   context.waitTicks(4);
   world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();
    check(close(ArmorResponses.factor(p),.84),"full stone set adds sixteen percent spell protection");
    p.setDeltaMovement(Vec3.ZERO);Effects.push(p,new Vec3(1,0,0));check(close(p.getDeltaMovement().x,.6),"stone set softens spell knockback");
    check(p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)<.1,"stone movement penalty applies");
    p.setDeltaMovement(Vec3.ZERO);equip(p,ElementalArmor.Kind.RIMEBOUND);p.setShiftKeyDown(true);
   });
   context.waitTicks(2);
   world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();
    check(p.getAttachedOrElse(ArmorResponses.STATE,ArmorResponses.State.EMPTY).rimeWindow()>0,"crouch edge opens rime guard");
    check(close(ArmorResponses.factor(p),.84),"rime full set protects during guard window");
   });
   context.waitTicks(15);
   world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();check(close(ArmorResponses.factor(p),1),"holding crouch cannot maintain rime guard");
    equip(p,ElementalArmor.Kind.MIRROR_THREAD);p.setAttached(ArmorResponses.STATE,new ArmorResponses.State(0,12,0,100,true));
    var attacker=EntityTypes.ZOMBIE.create(p.level(),EntitySpawnReason.COMMAND);attacker.setNoAi(true);attacker.snapTo(0,99,3,0,0);p.level().addFreshEntity(attacker);
    var source=p.damageSources().indirectMagic(attacker,attacker);float before=p.getHealth(),enemyBefore=attacker.getHealth();float left=SpellDefence.reduce(p.level(),p,source,8);
    Effects.readyToHurt(p);check(SpellDefence.hurt(p.level(),p,source,8),"mirror receives actual spell hit");
    check(close(before-p.getHealth(),left*.5),"mirror softens one spell by half");
    check(close(enemyBefore-attacker.getHealth(),Math.min(2,left*.2F)),"mirror returns only its weak fragment: expected="+Math.min(2,left*.2F)+", actual="+(enemyBefore-attacker.getHealth())+", distance="+attacker.distanceTo(p)+", allowed="+Targets.canHarm(p,attacker));
    check(!ArmorResponses.mirrorReady(p),"one hit consumes mirror window");
    p.setAttached(ArmorResponses.STATE,new ArmorResponses.State(12,0,80,50,true));check(!ArmorResponses.mirrorReady(p),"rime guard cannot bypass mantle recharge");
   });
   context.runOnClient(mc->{mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);mc.getWindow().setWindowed(1600,900);});
   for(var kind:ElementalArmor.Kind.values()) {
    world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.setShiftKeyDown(false);equip(p,kind);});
    context.waitTicks(5);context.takeScreenshot(TestScreenshotOptions.of("armor_"+kind.name().toLowerCase(java.util.Locale.ROOT)).disableCounterPrefix());
   }
  }
 }
 private static void equip(net.minecraft.server.level.ServerPlayer p,ElementalArmor.Kind kind) {
  for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))p.setItemSlot(slot,ItemStack.EMPTY);
  for(var item:ElementalArmor.ALL)if(item.kind==kind) {var stack=new ItemStack(item);p.setItemSlot(stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE).slot(),stack);}
 }
 private static boolean close(double a,double b){return Math.abs(a-b)<.025;}
 private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
}
