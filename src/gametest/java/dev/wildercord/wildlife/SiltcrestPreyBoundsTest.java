package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.SiltcrestNative.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import java.util.*;
/** Native ordinary AI refuses a small pond, protected fish and a saturated raw pool; no fake animal pose/clock. */
public final class SiltcrestPreyBoundsTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext c){for(int mode=0;mode<3;mode++){int scenario=mode;try(var w=c.worldBuilder().create()){c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");w.getServer().runCommand("weather clear");var birds=new SiltcrestBittern[1];var prey=new ArrayList<AbstractFish>();
  w.getServer().runOnServer(s->{var l=s.overworld();floor(l);observer(s.getPlayerList().getPlayers().getFirst(),new net.minecraft.world.phys.Vec3(.5,101.5,-1));int n=scenario==0?2:scenario==1?5:9;for(int i=0;i<n;i++){var f=fish(l,i);if(scenario==1&&i<3){if(i==0)f.setFromBucket(true);else if(i==1)f.setCustomName(Component.literal("Protected pond fish"));else f.setPersistenceRequired();}prey.add(f);}birds[0]=bird(l,.5,.5);check(BitternHabitat.bank(l,birds[0].blockPosition()),"Actual supplied bird begins on an eligible exposed bank");});
  c.waitTicks(2);w.getServer().runOnServer(s->check(birds[0].preyPool(s.overworld()).size()==(scenario==2?0:2),"Raw saturation refuses entire pool; protected fish never supply quorum after actual fluid physics: scenario="+scenario+" pool="+birds[0].preyPool(s.overworld()).size()+" water="+prey.stream().map(f->f.isInWater()).toList()));
  c.waitTicks(220);w.getServer().runOnServer(s->{check(prey.stream().allMatch(f->f.isAlive()&&!f.isRemoved()&&f.getHealth()==f.getMaxHealth()),"Actual AI has not harmed protected/small/dense pond fish");check(birds[0].huntReady()==0,"Refused complete prey pool earns no meal appetite");});
 }}denseNonprey(c);}
 private static void denseNonprey(ClientGameTestContext c){try(var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule max_entity_cramming 0");w.getServer().runCommand("time set 18000");w.getServer().runCommand("weather clear");
  var birds=new SiltcrestBittern[1];var prey=new ArrayList<AbstractFish>();var nonprey=new ArrayList<AbstractFish>();
  w.getServer().runOnServer(s->{var l=s.overworld();floor(l);observer(s.getPlayerList().getPlayers().getFirst(),new net.minecraft.world.phys.Vec3(.5,101.5,-1));
   for(int i=0;i<3;i++)prey.add(fish(l,i));
   for(int i=0;i<256;i++){var f=EntityTypes.TROPICAL_FISH.create(l,EntitySpawnReason.COMMAND);check(f!=null,"Real nonprey fish factory");f.snapTo(1.5+i%4,100.1,.5+(i/4)%4,0,0);check(l.addFreshEntity(f),"Actual nonprey fish admitted to native world");nonprey.add(f);}
   birds[0]=bird(l,.5,.5);check(birds[0].preyPool(l).isEmpty(),"256 actual nonprey fish saturate the raw complete fish scope despite three otherwise eligible cod");
  });c.waitTicks(20);
  w.getServer().runOnServer(s->{var l=s.overworld();check(nonprey.stream().filter(f->f.isAlive()&&!f.isRemoved()&&birds[0].distanceToSqr(f)<36).count()>8,"Actual dense nonprey swimming population remains within raw admission scope");
   check(birds[0].preyPool(l).isEmpty()&&birds[0].huntReady()==0&&prey.stream().allMatch(f->f.isAlive()&&f.getHealth()==f.getMaxHealth()),"Ordinary AI refuses dense nonprey prefix without prey damage or meal credit");
   for(var f:nonprey)f.discard();check(birds[0].preyPool(l).size()==3,"Removing real nonprey overflow reopens complete ordinary three-cod pool without changing clocks or eligibility");
  });
 }}
}
