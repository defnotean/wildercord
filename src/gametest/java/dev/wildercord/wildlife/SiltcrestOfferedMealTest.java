package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.SiltcrestNative.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import java.util.UUID;
/** Real crouch/entity-use packets feed one fish, never breed/drop rewards, and preserve the shared appetite after full reopen. */
public final class SiltcrestOfferedMealTest implements FabricClientGameTest {
 private SiltcrestBittern bird;private UUID id;private long ready;
 public void runTest(ClientGameTestContext c){boolean old=c.computeOnClient(mc->mc.options.keyShift.isDown());TestWorldSave save;try{
  try(var w=c.worldBuilder().create()){c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");w.getServer().runOnServer(s->{floor(s.overworld());bird=bird(s.overworld(),.5,-1.5);id=bird.getUUID();var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);place(p,.5,101,-3.5,bird.getBoundingBox().getCenter());p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COD,2));});
   c.runOnClient(mc->mc.options.keyShift.setDown(true));c.waitTicks(6);await(c,w,50,s->bird.onGround()&&s.getPlayerList().getPlayers().getFirst().isShiftKeyDown(),"Genuine settled bird and actual server-observed client crouch");use(c,w);c.waitTicks(4);
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.getMainHandItem().getCount()==1&&bird.pose()==SiltcrestBittern.PREENING,"Actual entity-use packet consumes exactly one raw fish and starts visible preen");ready=bird.huntReady();check(ready>s.overworld().getGameTime(),"Offered meal owns same finite appetite as natural prey");});use(c,w);c.waitTicks(3);w.getServer().runOnServer(s->check(s.getPlayerList().getPlayers().getFirst().getMainHandItem().getCount()==1&&bird.huntReady()==ready,"Repeated real packet consumes no fish and renews no deadline"));save=w.getWorldSave();
  }
  try(var w=save.open()){c.waitTicks(25);w.getServer().runOnServer(s->{bird=(SiltcrestBittern)s.overworld().getEntity(id);check(bird!=null&&bird.huntReady()==ready,"Exact offered-meal appetite persists across full restart");place(s.getPlayerList().getPlayers().getFirst(),bird.getX(),101,bird.getZ()-2,bird.getBoundingBox().getCenter());});c.runOnClient(mc->mc.options.keyShift.setDown(true));c.waitTicks(5);use(c,w);c.waitTicks(4);w.getServer().runOnServer(s->check(s.getPlayerList().getPlayers().getFirst().getMainHandItem().getCount()==1&&bird.huntReady()==ready,"Restart cannot bypass shared meal rest"));}
 }finally{c.runOnClient(mc->mc.options.keyShift.setDown(old));}}
 private static void use(ClientGameTestContext c,TestSingleplayerContext w){int id=w.getServer().computeOnServer(s->s.overworld().getEntitiesOfClass(SiltcrestBittern.class,new AABB(-8,100,-8,10,106,8)).getFirst().getId());c.runOnClient(mc->{var bird=mc.level.getEntity(id);check(bird!=null,"Actual live bird is tracked on client");mc.gameMode.interact(mc.player,bird,new EntityHitResult(bird,bird.getBoundingBox().getCenter()),InteractionHand.MAIN_HAND);});}
}
