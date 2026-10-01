package dev.wildercord.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
public final class RunicHearthBlock extends Block implements EntityBlock {
 public static final IntegerProperty CHARGE=IntegerProperty.create("charge",0,3);
 public RunicHearthBlock(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(CHARGE,0));}
 protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(CHARGE);}
 public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new RunicHearthEntity(p,s);}
 @SuppressWarnings("unchecked") public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide()||t!=WildercordBlocks.HEARTH_ENTITY?null:(BlockEntityTicker<T>)(BlockEntityTicker<RunicHearthEntity>)RunicHearthEntity::tick;}
 protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult h){if(l.getBlockEntity(p) instanceof RunicHearthEntity hearth)hearth.use(player,ItemStack.EMPTY);return InteractionResult.SUCCESS;}
 protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult h){if(l.getBlockEntity(p) instanceof RunicHearthEntity hearth)hearth.use(player,stack);return InteractionResult.SUCCESS;}
}
