package dev.wildercord.wildlife;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** Actual tracked entities exercise the mapped early-abort API, not a synthetic list. */
public final class WetlandQueryBoundsTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext c) {
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");
   w.getServer().runOnServer(s->{
    var l=s.overworld();var player=s.getPlayerList().getPlayers().getFirst();player.setGameMode(GameType.CREATIVE);player.teleportTo(l,20,100,20,Set.<Relative>of(),0,0,false);
    for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){l.setBlock(new BlockPos(x,99,z),Blocks.MUD.defaultBlockState(),2);for(int y=100;y<=103;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}
    var box=new AABB(-4,100,-4,4,104,4);var living=new ArrayList<Zombie>();
    for(int n=0;n<256;n++) {
     var e=EntityTypes.ZOMBIE.create(l,EntitySpawnReason.COMMAND);check(e!=null,"Registered native zombie factory");e.setNoAi(true);e.setPersistenceRequired();e.snapTo(.5,100,.5,0,0);check(l.addFreshEntity(e),"Real bounded-query peer tracked");living.add(e);
     if(n==11||n==12||n==255){var pool=WetlandQueries.scan(l,Zombie.class,box,null);if(n==11)check(!pool.saturated()&&pool.enumerated()==12&&new HashSet<>(pool.entities()).equals(new HashSet<>(living)),"Twelve-peer result is genuinely complete with every actual UUID");else check(pool.saturated()&&pool.enumerated()==13&&pool.entities().isEmpty(),"Thirteen/256 actual peers enumerate only13 and expose no arbitrary prefix");}
    }
    living.forEach(Entity::discard);
    var newts=new ArrayList<LanternNewt>();
    for(int n=0;n<13;n++) {
     if(n<4)check(WetlandQueries.populationRoom(l,LanternNewt.class,box,3)==(n<3),"Actual sparse newt cap remains exactly three");
     var e=WetlandContent.NEWT.create(l,EntitySpawnReason.COMMAND);check(e!=null,"Registered newt factory");e.setNoAi(true);e.snapTo(.5,100,.5,0,0);check(l.addFreshEntity(e),"Native newt tracked");newts.add(e);
    }
    var pool=WetlandQueries.scan(l,LanternNewt.class,box,null);check(pool.saturated()&&pool.enumerated()==13&&!WetlandQueries.populationRoom(l,LanternNewt.class,box,3),"Dense natural cap is conservative and bounded");
    var source=WetlandContent.NEWT.create(l,EntitySpawnReason.COMMAND);source.setNoAi(true);source.snapTo(.5,100,.5,0,0);check(l.addFreshEntity(source),"Actual refuge source");
    check(!WetlandQueries.refugeFree(l,box,source),"Dense refuge refuses even when prefix peers are nonresting");newts.getLast().discard();check(WetlandQueries.refugeFree(l,box,source),"Complete twelve-peer nonresting pool retains ordinary free refuge");
    newts.forEach(Entity::discard);source.discard();
    var crabs=new ArrayList<ReedbackCrab>();
    for(int n=0;n<3;n++){check(WetlandQueries.populationRoom(l,ReedbackCrab.class,box,2)==(n<2),"Actual sparse crab cap remains exactly two");var e=ReedbackContent.CRAB.create(l,EntitySpawnReason.COMMAND);e.setNoAi(true);e.snapTo(.5,100,.5,0,0);check(l.addFreshEntity(e),"Actual local crab");crabs.add(e);}crabs.forEach(Entity::discard);
   });
  }
 }
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
