package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import java.lang.reflect.Field;
/** Native path movement and deadline observation; no goal clocks or mob resources are fixture-adjusted. */
public final class FungalBrowseBoundsTest implements FabricClientGameTest {
 private SporebackSnail snail;
 public void runTest(ClientGameTestContext c) {
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");
   w.getServer().runOnServer(s -> {
    var l=s.overworld();for(int x=-8;x<=10;x++)for(int z=-8;z<=10;z++) {l.setBlock(new BlockPos(x,29,z),Blocks.MOSS_BLOCK.defaultBlockState(),2);for(int y=30;y<=32;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);l.setBlock(new BlockPos(x,33,z),Blocks.STONE.defaultBlockState(),2);}
    l.setBlock(new BlockPos(4,30,4),Blocks.BROWN_MUSHROOM.defaultBlockState(),2);snail=SporebackContent.SNAIL.create(l,EntitySpawnReason.COMMAND);snail.snapTo(.5,30,.5,0,0);l.addFreshEntity(snail);
   });
   int elapsed=0;for(;elapsed<520;elapsed+=5) {c.waitTicks(5);if(elapsed%50==0)w.getServer().runOnServer(s -> dev.wildercord.Wildercord.LOGGER.info("FUNGAL_PATH tick="+snail.tickCount+" pos="+snail.position()+" dest="+value("destination")+" left="+value("left")+" speed="+snail.getSpeed()+" delta="+snail.getDeltaMovement()+" ground="+snail.onGround()+" path="+(snail.getNavigation().getPath()==null?"none":snail.getNavigation().getPath().getNextNodeIndex()+"/"+snail.getNavigation().getPath().getNodeCount()+" end="+snail.getNavigation().getPath().getEndNode())));if(w.getServer().computeOnServer(s -> snail.dew()))break;}
   int observed=elapsed;w.getServer().runOnServer(s -> dev.wildercord.Wildercord.LOGGER.info("FUNGAL_BOUNDS late elapsed="+observed+" position="+snail.position()+" dew="+snail.dew()+" destination="+value("destination")+" remaining="+value("left")+" searchAt="+value("searchAt")+" tick="+snail.tickCount+" path="+(snail.getNavigation().getPath()==null?"none":snail.getNavigation().getPath().getEndNode())));
   check(elapsed>=190 && elapsed<520,"Late scan corner candidate is reached and browsed within finite search plus journey");
   final SporebackSnail[] occupant=new SporebackSnail[1];
   w.getServer().runOnServer(s -> {s.overworld().setBlock(snail.blockPosition().above(),FungalGarden.NURSERY.defaultBlockState(),2);occupant[0]=SporebackContent.SNAIL.create(s.overworld(),EntitySpawnReason.COMMAND);occupant[0].snapTo(snail.getX()+.2,30,snail.getZ()+.2,0,0);s.overworld().addFreshEntity(occupant[0]);occupant[0].setNoAi(true);check(occupant[0].answerMagic(true),"A real temporary Fire hide occupies the nursery cell");});
   c.waitTicks(50);check(w.getServer().computeOnServer(s -> snail.nurseryReady()==0 && snail.pose()!=2),"Occupied nursery refuses another living visitor rather than tethering it");
   w.getServer().runOnServer(s -> {occupant[0].discard();s.overworld().setBlock(snail.blockPosition().above(),Blocks.AIR.defaultBlockState(),2);});
   w.getServer().runOnServer(s -> {check(snail.getX()>3 && snail.getZ()>3,"Actual native navigation reached the distant mushroom");dev.wildercord.Wildercord.LOGGER.info("FUNGAL_BOUNDS late visit position="+snail.position());snail.discard();s.overworld().setBlock(new BlockPos(4,30,4),Blocks.AIR.defaultBlockState(),2);s.overworld().setBlock(new BlockPos(4,30,0),Blocks.BROWN_MUSHROOM.defaultBlockState(),2);snail=SporebackContent.SNAIL.create(s.overworld(),EntitySpawnReason.COMMAND);snail.snapTo(.5,30,.5,0,0);s.overworld().addFreshEntity(snail);});
   for(int i=0;i<55;i++) {c.waitTicks(5);if(w.getServer().computeOnServer(s -> value("destination")!=null))break;}
   check(w.getServer().computeOnServer(s -> value("destination")!=null),"Obstruction scenario begins with a real valid path");
   int before=w.getServer().computeOnServer(s -> (Integer)value("left"));
   w.getServer().runOnServer(s -> {var p=snail.blockPosition();for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)if(Math.abs(dx)==2 || Math.abs(dz)==2)for(int y=0;y<3;y++)s.overworld().setBlock(p.offset(dx,y,dz),Blocks.STONE.defaultBlockState(),3);snail.getNavigation().stop();});
   c.waitTicks(100);w.getServer().runOnServer(s -> dev.wildercord.Wildercord.LOGGER.info("FUNGAL_BLOCKED before="+before+" left="+value("left")+" retries="+value("retries")+" destination="+value("destination")+" pose="+snail.pose()+" dew="+snail.dew()+" pos="+snail.position()+" tick="+snail.tickCount));check(w.getServer().computeOnServer(s -> value("destination")!=null && (Integer)value("left")<=before-95 && (Integer)value("retries")>0 && !snail.dew()),"Actual blocked path retries consume the original journey deadline");
   c.waitTicks(170);check(w.getServer().computeOnServer(s -> !snail.dew() && value("destination")==null),"Unreachable obstruction ends bounded journey without granting reserve");
  }
 }
 private Object value(String name) {try {Object goal=null;Field selector=net.minecraft.world.entity.Mob.class.getDeclaredField("goalSelector");selector.setAccessible(true);for(var candidate:((net.minecraft.world.entity.ai.goal.GoalSelector)selector.get(snail)).getAvailableGoals())if(candidate.getGoal().getClass().getSimpleName().equals("Visit"))goal=candidate.getGoal();check(goal!=null,"Actual registered Visit goal exists");Field f=goal.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(goal);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void check(boolean b,String why) {if(!b)throw new AssertionError(why);}
}
