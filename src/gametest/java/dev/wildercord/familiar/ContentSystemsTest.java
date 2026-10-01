package dev.wildercord.familiar;
import dev.wildercord.cast.*;
import dev.wildercord.cast.events.EventAftermath;
import dev.wildercord.content.*;
import dev.wildercord.player.*;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import java.util.*;
/** Exercises utility roles, saved event discoveries, charm tradeoffs and real dummy trial hits. */
public final class ContentSystemsTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext context){try(var world=context.worldBuilder().create()){
  context.waitTicks(35);
  world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);var level=p.level();
   var at=p.blockPosition();EventAftermath.leave(level,at,"star");var aftermath=EventAftermath.of(server);
   check(aftermath.claim(p,0)&&p.hasEffect(MobEffects.NIGHT_VISION),"star echo discoverable");check(!aftermath.claim(p,0),"echo cannot be claimed twice");
   p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(WildercordItems.KEEPERS_HOURGLASS));var hour=Heart.bonuses(p);p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);var base=Heart.bonuses(p);
   check(close(hour.duration(),base.duration()*1.3)&&close(hour.power(),base.power()*.85),"hourglass benefit and drawback");
   p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(WildercordItems.LIVING_SEEDPOD));var seed=Heart.bonuses(p);check(close(seed.cost(),base.cost()*.9)&&close(seed.cooldown(),base.cooldown()*1.1),"seedpod tradeoffs");
   p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(WildercordItems.SKY_FEATHER));var sky=Heart.bonuses(p);check(close(sky.cost(),base.cost()*1.15)&&close(sky.cooldown(),base.cooldown()*.85),"feather tradeoffs");p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
   var wisp=FamiliarContent.WISP.create(level,EntitySpawnReason.COMMAND);wisp.snapTo(p.position());
   var mob=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.COMMAND);mob.setNoAi(true);mob.snapTo(p.getX()+3,p.getY(),p.getZ());level.addFreshEntity(mob);
   check(FamiliarRoles.choose(p,"scout")==1&&FamiliarRoles.help(p,wisp)&&mob.hasEffect(MobEffects.GLOWING),"scout reveals nearest threat");
   p.setLastHurtByMob(mob);FamiliarRoles.choose(p,"guardian");check(FamiliarRoles.help(p,wisp)&&p.hasEffect(MobEffects.ABSORPTION),"guardian gives short emergency shield");
   var crop=at.offset(1,0,0);level.setBlockAndUpdate(crop.below(),Blocks.FARMLAND.defaultBlockState());level.setBlockAndUpdate(crop,((CropBlock)Blocks.WHEAT).getStateForAge(7));FamiliarRoles.choose(p,"gardener");check(FamiliarRoles.help(p,wisp)&&((CropBlock)Blocks.WHEAT).isMaxAge(level.getBlockState(crop)),"gardener marks crop without harvesting");
   FamiliarRoles.choose(p,"companion");check(FamiliarRoles.get(p)==FamiliarRoles.Role.COMPANION,"restore original elemental assistance");
   PracticeRoom.enter(p);check(SpellTrials.start(p,"precision")==1,"optional trial starts in practice survival");
   var dummy=p.level().getEntitiesOfClass(TrainingDummy.class,p.getBoundingBox().inflate(40)).getFirst();
   for(int i=0;i<5;i++){SpellTrials.cast(p,List.of(Runes.BOLT,Runes.FIRE),10);Effects.readyToHurt(dummy);SpellDefence.hurt(p.level(),dummy,p.damageSources().indirectMagic(p,p),2);}
   int pages=p.getInventory().countItem(WildercordItems.TORN_PAGE);check(pages==1,"actual dummy hits complete precision");
   SpellTrials.start(p,"precision");for(int i=0;i<5;i++){SpellTrials.cast(p,List.of(Runes.BOLT,Runes.FIRE),10);Effects.readyToHurt(dummy);SpellDefence.hurt(p.level(),dummy,p.damageSources().indirectMagic(p,p),2);}
   check(p.getInventory().countItem(WildercordItems.TORN_PAGE)==pages,"repeat trial awards no duplicate page");
   check(PracticeRoom.leave(p)==1,"normal player returns from trial arena");
  });
 }}
 private static boolean close(double a,double b){return Math.abs(a-b)<.001;}private static void check(boolean v,String label){if(!v)throw new AssertionError(label);}
}
