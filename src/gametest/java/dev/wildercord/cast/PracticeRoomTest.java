package dev.wildercord.cast;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.world.phys.AABB;

public final class PracticeRoomTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext context) {
  try(var world=context.worldBuilder().create()) {
   context.waitTicks(35);
   world.getServer().runOnServer(server -> {
    var p=server.getPlayerList().getPlayers().getFirst();
    check(PracticeRoom.enter(p)==1,"practice dimension loads");
    check(p.level().dimension().equals(PracticeRoom.DIMENSION),"practice entry transports player");
    check(p.level().getEntitiesOfClass(TrainingDummy.class,arena()).size()==3,"three practice targets");
    p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setHealth(15);p.getFoodData().setFoodLevel(10);
    var dummy=p.level().getEntitiesOfClass(TrainingDummy.class,arena()).getFirst();
    var thirsty=dev.wildercord.spell.SpellCompiler.compile(java.util.List.of(dev.wildercord.spell.Runes.BEAM,dev.wildercord.spell.Runes.HARM,dev.wildercord.spell.Runes.THIRST)).root().groups.getFirst().effects.getFirst();
    Effects.apply(new Cast(p),thirsty,new Cast.Hit(java.util.List.of(dummy),dummy.position(),new net.minecraft.world.phys.Vec3(0,0,1),p.position(),null,null,false));
    check(dummy.lastDamage()>0 && dummy.getHealth()==dummy.getMaxHealth(),"practice dummy measures native spell damage before healing");
    check(p.getHealth()==15,"native Thirst cannot farm healing in the Practice Room");
    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
    p.setAttached(dev.wildercord.aura.AuraAttachments.AURA,new dev.wildercord.aura.AuraAttachments.Data("gale",dev.wildercord.aura.AuraRules.GLOW,0,100,0));
    check(dev.wildercord.aura.AuraCombat.projected(p,dummy,5,true)>0,"practice dummy measures actual projected Aura damage");
    dummy.setPermanentlyInvulnerable(true);dummy.hurtServer(p.level(),p.level().damageSources().magic(),5);
    check(dummy.lastDamage()==0,"a rejected hurt call cannot reuse a previous dummy damage sample");dummy.setPermanentlyInvulnerable(false);
    PracticeRoom.targets(p.level(),24,true);
    check(p.level().getEntitiesOfClass(TrainingDummy.class,arena()).size()==24,"24-target stress scene");
   });
   context.waitTicks(30);
   context.takeScreenshot(TestScreenshotOptions.of("practice_stress_arena").disableCounterPrefix());
   world.getServer().runOnServer(server -> {
    var p=server.getPlayerList().getPlayers().getFirst();
    check(PracticeRoom.targets(p.level(),3,false)==3,"practice reset");
    check(p.level().getEntitiesOfClass(TrainingDummy.class,arena()).size()==3,"reset removes stress targets");
    check(PracticeRoom.leave(p)==1,"return point survives dimension transfer");
    check(p.level()==server.overworld(),"practice return restores original world");
    check(PracticeRoom.targets(p.level(),24,true)==0,"practice command cannot replace targets in ordinary worlds");
   });
  }
 }
 private static AABB arena(){return new AABB(-12,79,-8,13,85,33);}
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
