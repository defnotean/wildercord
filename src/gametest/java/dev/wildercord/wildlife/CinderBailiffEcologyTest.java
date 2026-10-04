package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.UUID;
/** Actual contested navigation/browsing and closed-save restart; never sets pose or deadline. */
public final class CinderBailiffEcologyTest implements FabricClientGameTest {
 private CinderBailiff a,b;private final BlockPos fern=new BlockPos(2,30,0);
 public void runTest(ClientGameTestContext c){UUID owner;long meal;TestWorldSave save;
  try(var w=c.worldBuilder().create()){c.waitTicks(25);w.getServer().runCommand("difficulty normal");w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");
   w.getServer().runOnServer(s->{var l=s.overworld();for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){l.setBlock(new BlockPos(x,29,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);for(int y=30;y<=33;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}l.setBlock(fern,EmberContent.FERN.defaultBlockState().setValue(CinderFernBlock.AGE,2),2);var player=s.getPlayerList().getPlayers().getFirst();player.setGameMode(GameType.CREATIVE);player.teleportTo(l,0,30,5,java.util.Set.<net.minecraft.world.entity.Relative>of(),0,0,false);a=EmberContent.BAILIFF.create(l,EntitySpawnReason.COMMAND);b=EmberContent.BAILIFF.create(l,EntitySpawnReason.COMMAND);a.snapTo(-.5,30,.5,0,0);b.snapTo(5.5,30,.5,0,0);l.addFreshEntity(a);l.addFreshEntity(b);});
   boolean arrived=false;for(int n=0;n<150;n++){c.waitTicks(5);if(w.getServer().computeOnServer(s->s.overworld().getBlockState(fern).getValue(CinderFernBlock.AGE)==1)){arrived=true;break;}}check(arrived,"A real competitor reaches and completes40ticks of fern browse");check(w.getServer().computeOnServer(s->(a.mealReady()>0)!=(b.mealReady()>0)),"Exactly one creature consumes mature growth without deleting root");var winner=w.getServer().computeOnServer(s->a.mealReady()>0?a:b);owner=winner.getUUID();meal=winner.mealReady();
   w.getServer().runOnServer(s->{check(winner.pose()==CinderBailiff.RESTING&&winner.restUntil()>s.overworld().getGameTime(),"Actual reached shelter owns finite resting state");(winner==a?b:a).discard();s.overworld().setBlock(fern,EmberContent.FERN.defaultBlockState().setValue(CinderFernBlock.AGE,2),2);});
   c.waitTicks(60);check(w.getServer().computeOnServer(s->winner.mealReady()==meal&&s.overworld().getBlockState(fern).getValue(CinderFernBlock.AGE)==2),"Another ripe fern cannot renew or shorten meal rest");save=w.getWorldSave();
  }
  try(var w=save.open()){c.waitTicks(25);w.getServer().runOnServer(s->{var e=(CinderBailiff)s.overworld().getEntity(owner);check(e!=null&&e.mealReady()==meal,"Ordinary full reopen preserves exact meal deadline");check(s.overworld().getBlockState(fern).getValue(CinderFernBlock.AGE)==2,"No restart yields early second consumption");});}
 }
 private static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
}
