package dev.wildercord.wildlife;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.tags.FluidTags;

/** Actual fluid/collision controls, not manufactured habitat booleans or a manual creature phase. */
public final class SiltcrestBankCollisionTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext c){try(var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");
  w.getServer().runOnServer(s->{var l=s.overworld();SiltcrestNative.floor(l);var at=new BlockPos(2,100,1);
   check(l.getBlockState(at.below()).isSolidRender()&&l.getBlockState(at.above()).getCollisionShape(l,at.above()).isEmpty(),"Real shallow pond has solid footing and clear headroom");
   check(l.getFluidState(at).is(FluidTags.WATER)&&l.getBlockState(at).getCollisionShape(l,at).isEmpty()&&BitternHabitat.bank(l,at),"Ordinary shallow water remains an admitted wading bank");
   l.setBlock(at,Blocks.SEAGRASS.defaultBlockState(),2);check(l.getFluidState(at).is(FluidTags.WATER)&&l.getBlockState(at).getCollisionShape(l,at).isEmpty()&&BitternHabitat.bank(l,at),"Harmless actual water-filled seagrass remains admitted");
   l.setBlock(at,Blocks.OAK_FENCE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,true),2);check(l.getFluidState(at).is(FluidTags.WATER)&&!l.getBlockState(at).getCollisionShape(l,at).isEmpty(),"Actual waterlogged fence is wet but occupies the proposed footing/body");check(!BitternHabitat.bank(l,at),"Collision-bearing wet block refuses the bird bank");
   l.setBlock(at,Blocks.WATER.defaultBlockState(),2);check(BitternHabitat.bank(l,at),"Restoring genuine water reopens ordinary bank admission");
   var dry=new BlockPos(-2,101,-2);var roof=dry.above(2);l.setBlock(dry.east().below(),Blocks.WATER.defaultBlockState(),2);l.setBlock(roof,Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true),2);
   check(l.getBlockState(roof).getValue(LeavesBlock.PERSISTENT)&&BitternHabitat.shelter(l,dry),"Actual player-placed persistent leaf roof supplies dry bank cover");
   l.setBlock(roof,Blocks.AIR.defaultBlockState(),2);check(!BitternHabitat.shelter(l,dry),"Actual roof removal still immediately invalidates physical shelter");
  });
 }}
 private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
}
