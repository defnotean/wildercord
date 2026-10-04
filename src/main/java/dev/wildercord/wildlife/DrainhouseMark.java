package dev.wildercord.wildlife;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
/** Threshold inscription, ribbed garden ledger and a copper bell-restoration desk. */
public final class DrainhouseMark extends Block implements EntityBlock {
 public static final IntegerProperty KIND=IntegerProperty.create("kind",0,2);
 public static final BooleanProperty RESTORED=BooleanProperty.create("restored");
 public DrainhouseMark(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(KIND,0).setValue(RESTORED,false));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(KIND,RESTORED);}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new DrainhouseMarkEntity(p,s);}
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(KIND)==2?Shapes.or(Block.box(1,0,1,15,4,15),Block.box(2,4,3,14,12,13)):Block.box(2,0,2,14,14,14);}
 @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player who,BlockHitResult h){if(who instanceof net.minecraft.server.level.ServerPlayer player)DrainhouseInvestigation.read(player,p);return InteractionResult.SUCCESS;}
 @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player who,InteractionHand hand,BlockHitResult h){if(who instanceof net.minecraft.server.level.ServerPlayer player){if(s.getValue(KIND)==2 && stack.is(BelowkeeperEquipment.BELL))DrainhouseInvestigation.repair(player,p,stack);else if(s.getValue(KIND)==2 && stack.is(SporebackContent.DEW))DrainhouseInvestigation.restore(player,p);else DrainhouseInvestigation.read(player,p);}return InteractionResult.SUCCESS;}
}
