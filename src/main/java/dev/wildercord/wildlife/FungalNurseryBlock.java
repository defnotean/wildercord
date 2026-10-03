package dev.wildercord.wildlife;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
/** Open ventilated canopy. No ticker, extra yield or accelerated clocks. */
public final class FungalNurseryBlock extends Block {
 private static final VoxelShape SHAPE=Shapes.or(Block.box(0,12,0,16,16,16),Block.box(0,-16,0,2,12,2),Block.box(14,-16,0,16,12,2),Block.box(0,-16,14,2,12,16),Block.box(14,-16,14,16,12,16));
 public FungalNurseryBlock(Properties p) {super(p);}
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) {return SHAPE;}
 @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player who,BlockHitResult h) {if(who instanceof net.minecraft.server.level.ServerPlayer player)FungalInvestigation.hint(player);return InteractionResult.SUCCESS;}
 @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player who,InteractionHand hand,BlockHitResult h) {
  if(who instanceof net.minecraft.server.level.ServerPlayer player) {if(stack.is(FungalGarden.BREATHER))FungalInvestigation.finish(player,p);else FungalInvestigation.hint(player);}return InteractionResult.SUCCESS;
 }
}
