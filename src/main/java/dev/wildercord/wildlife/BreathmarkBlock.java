package dev.wildercord.wildlife;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
/** Two Belowkeeper field marks: one damp root, one ventilated spiral. */
public final class BreathmarkBlock extends Block implements EntityBlock {
 public static final IntegerProperty KIND=IntegerProperty.create("kind",0,1);
 public BreathmarkBlock(Properties p) {super(p);registerDefaultState(defaultBlockState().setValue(KIND,0));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) {b.add(KIND);}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s) {return new BreathmarkEntity(p,s);}
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) {return Block.box(2,0,2,14,13,14);}
 @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player who,BlockHitResult h) {if(who instanceof net.minecraft.server.level.ServerPlayer player)FungalInvestigation.read(player,p);return InteractionResult.SUCCESS;}
 @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player who,InteractionHand hand,BlockHitResult h) {return useWithoutItem(s,l,p,who,h);}
}
