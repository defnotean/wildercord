package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.SiltcrestNative.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.world.phys.Vec3;
/** Supplied pond; real injury/backstep/rest expiry followed by bounded native bank-return navigation. */
public final class SiltcrestBankReturnTest implements FabricClientGameTest {
 private SiltcrestBittern witness;
 public void runTest(ClientGameTestContext c){EcologyReturnProbeChecks.verify();try(var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");w.getServer().runCommand("weather clear");
  w.getServer().runOnServer(s->{dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.wildlife.SiltcrestBankReturnTest\",\"seed\":\"{}\"}",s.overworld().getSeed());floor(s.overworld());observer(s.getPlayerList().getPlayers().getFirst(),new Vec3(.5,101.7,.5));witness=bird(s.overworld(),.5,.5);});
  var probe=w.getServer().computeOnServer(s->EcologyReturnProbe.begin(EcologyReturnProbe.BANK,s.overworld(),witness));
  try{
  await(c,w,60,s->witness.onGround(),"Actual bank witness settles on physical floor");
  float before=w.getServer().computeOnServer(s->{check(BitternHabitat.standingBank(witness),"Initial supplied bank is genuine dry habitat");float hp=witness.getHealth();boolean hurt=witness.hurtServer(s.overworld(),s.overworld().damageSources().playerAttack(s.getPlayerList().getPlayers().getFirst()),1);check(hurt&&witness.getHealth()<hp&&witness.pose()==SiltcrestBittern.RETREATING,"Actual physical injury owns finite retreat and rest");return witness.getHealth();});
  await(c,w,100,s->witness.onGround()&&!BitternHabitat.standingBank(witness)&&witness.position().distanceToSqr(new Vec3(.5,101,.5))>1,"Ordinary native backstep really leaves the bank; no position/pose injection");
  long rest=w.getServer().computeOnServer(s->witness.huntReady());
  int allowance=w.getServer().computeOnServer(s->(int)Math.max(0,rest-s.overworld().getGameTime())+SiltcrestBittern.JOURNEY+20);
  await(c,w,allowance,s->s.overworld().getGameTime()>=rest&&witness.onGround()&&BitternHabitat.standingBank(witness),"Real rest expires and native finite navigation regains a dry local bank");
  w.getServer().runOnServer(s->{check(witness.huntReady()==rest&&witness.getHealth()==before,"Return manufactures neither meal, restored health nor renewed rest");check(witness.pose()==SiltcrestBittern.IDLE||witness.pose()==SiltcrestBittern.STALKING,"Return leaves ordinary forage AI eligible, no forced animation phase");});
  }finally{probe.close();}
 }}
}
