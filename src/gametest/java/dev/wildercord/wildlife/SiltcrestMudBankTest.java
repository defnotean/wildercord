package dev.wildercord.wildlife;

import static dev.wildercord.wildlife.SiltcrestNative.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/** Existing APIs only: actual partial-Mud grounding must admit a genuine ordinary hunt; raised waterlogged fence stays refused. */
public final class SiltcrestMudBankTest implements FabricClientGameTest {
 private SiltcrestBittern actor;
 public void runTest(ClientGameTestContext c){
  try(var w=c.worldBuilder().create()){
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");w.getServer().runCommand("weather clear");
   w.getServer().runOnServer(s->{var l=s.overworld();floor(l);for(int x=-8;x<=10;x++)for(int z=-8;z<=8;z++){var p=new BlockPos(x,100,z);if(!p.equals(new BlockPos(1,100,0)))l.setBlock(p,Blocks.MUD.defaultBlockState(),2);}observer(s.getPlayerList().getPlayers().getFirst(),new Vec3(.5,101.5,.5));for(int i=0;i<3;i++)fish(l,0);actor=bird(l,.5,.5);});
   await(c,w,60,s->actor.onGround()&&Math.abs(actor.getY()-100.875)<.03&&s.overworld().getBlockState(actor.blockPosition()).is(Blocks.MUD),"Ordinary physics grounds the original1.6-high creature on actual0.875 Mud collision");
   w.getServer().runOnServer(s->{var l=s.overworld();var body=actor.blockPosition();var state=l.getBlockState(body);double top=state.getCollisionShape(l,body).max(net.minecraft.core.Direction.Axis.Y);check(top==.875&&l.getFluidState(body).isEmpty()&&l.noCollision(actor,actor.getBoundingBox()),"Real partial Mud plane is dry, unpenetrated and matches the grounded body");check(BitternHabitat.bank(l,body.above())&&actor.preyPool(l).size()==3,"Canonical footspace above the actual Mud shore has supported water-adjacent bank and three real wild prey");System.out.println("SILTCREST_MUD actualBody="+actor.position()+" ground="+actor.onGround()+" colliderTop="+top+" blockBank="+BitternHabitat.bank(l,body)+" canonicalBank="+BitternHabitat.bank(l,body.above())+" hungry="+actor.hungry()+" pose="+actor.pose()+" pool="+actor.preyPool(l).size());check(actor.hungry()||actor.pose()==SiltcrestBittern.STALKING||actor.pose()==SiltcrestBittern.COILING,"Actual grounded partial Mud bank permits ordinary hungry/started hunt admission without a full-block fallback");});
   await(c,w,500,s->actor.pose()==SiltcrestBittern.COILING&&actor.onGround()&&Math.abs(actor.getY()-100.875)<.03,"Real Mud-bank hunting reaches original committed coiling");
   await(c,w,120,s->actor.pose()==SiltcrestBittern.PREENING&&actor.huntReady()>SiltcrestBittern.clock(s.overworld())+SiltcrestBittern.APPETITE-100,"Original real wild-fish strike earns actual finite appetite and preening on Mud");
   w.getServer().runOnServer(s->{check(actor.preyPool(s.overworld()).size()==2&&actor.getHealth()==12,"One actual admitted catch leaves two live wild fish and does not heal/damage the source");check(s.overworld().getBlockState(actor.blockPosition()).is(Blocks.MUD),"Actual successful visitor still rests on partial Mud, not a full-block fallback");});
  }
  try(var w=c.worldBuilder().create()){
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");w.getServer().runCommand("weather clear");
   w.getServer().runOnServer(s->{var l=s.overworld();floor(l);observer(s.getPlayerList().getPlayers().getFirst(),new Vec3(.5,102,.5));var at=new BlockPos(0,100,0);l.setBlock(at,Blocks.OAK_FENCE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,true),2);actor=SiltcrestContent.BITTERN.create(l,net.minecraft.world.entity.EntitySpawnReason.COMMAND);check(actor!=null,"Original ordinary fence-control factory");actor.snapTo(.5,102,.5,0,0);actor.setPersistenceRequired();check(l.addFreshEntity(actor),"Actual raised waterlogged-fence creature is tracked with ordinary AI");});
   await(c,w,60,s->actor.onGround()&&Math.abs(actor.getY()-101.5)<.03,"Actual waterlogged fence grounds the original creature on its real1.5-high top");
   w.getServer().runOnServer(s->{var l=s.overworld();var support=new BlockPos(0,100,0);check(l.getBlockState(support).is(Blocks.OAK_FENCE)&&!l.getFluidState(support).isEmpty()&&l.getBlockState(support).getCollisionShape(l,support).max(net.minecraft.core.Direction.Axis.Y)==1.5,"Negative raised-fence control retains actual fluid and tall collision");check(!BitternHabitat.bank(l,actor.blockPosition())&&!BitternHabitat.bank(l,support)&&!actor.hungry(),"Partial-foot normalization cannot promote an actual raised waterlogged fence into a safe hunting bank");});
  }
 }
}
