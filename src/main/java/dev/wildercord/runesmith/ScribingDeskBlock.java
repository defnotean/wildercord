package dev.wildercord.runesmith;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * The Scribing Desk: a lectern for rune-work. A villager who claims one becomes a Runesmith, and
 * right-clicking it reads your daily contract board (and hands in any that are done).
 */
public class ScribingDeskBlock extends Block {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	private static VoxelShape column(double width, double yBottom, double height) {
		double edge = (16.0 - width) / 32.0;
		return Shapes.box(edge, yBottom / 16.0, edge, 1.0 - edge, (yBottom + height) / 16.0, 1.0 - edge);
	}

	private static VoxelShape boxZ(double width, double zBottom, double zTop, double yBottom, double yTop) {
		double edge = (16.0 - width) / 32.0;
		return Shapes.box(edge, yBottom / 16.0, zBottom / 16.0, 1.0 - edge, yTop / 16.0, zTop / 16.0);
	}

	/** 1.21.4 has no Shapes.rotateHorizontal: the desk only ever turns about Y. */
	private static VoxelShape turn(VoxelShape shape, Direction dir) {
		if (dir == Direction.NORTH) return shape;
		var boxes = new java.util.ArrayList<VoxelShape>();
		for (var box : shape.toAabbs()) {
			double x1 = box.minX, y1 = box.minY, z1 = box.minZ, x2 = box.maxX, y2 = box.maxY, z2 = box.maxZ;
			if (dir == Direction.SOUTH) boxes.add(Shapes.box(1.0 - x2, y1, 1.0 - z2, 1.0 - x1, y2, 1.0 - z1));
			else if (dir == Direction.EAST) boxes.add(Shapes.box(z1, y1, 1.0 - x2, z2, y2, 1.0 - x1));
			else boxes.add(Shapes.box(1.0 - z2, y1, x1, 1.0 - z1, y2, x2));
		}
		return Shapes.or(Shapes.empty(), boxes.toArray(new VoxelShape[0]));
	}

	private static final VoxelShape COLLISION = Shapes.or(column(16.0, 0.0, 2.0), column(8.0, 2.0, 14.0));
	private static final VoxelShape DESK = Shapes.or(
		boxZ(16.0, 10.0, 14.0, 1.0, 5.333333), boxZ(16.0, 12.0, 16.0, 5.333333, 9.666667), boxZ(16.0, 14.0, 18.0, 9.666667, 14.0), COLLISION);
	private static final Map<Direction, VoxelShape> SHAPES = new java.util.EnumMap<>(Map.of(
		Direction.NORTH, DESK, Direction.SOUTH, turn(DESK, Direction.SOUTH),
		Direction.EAST, turn(DESK, Direction.EAST), Direction.WEST, turn(DESK, Direction.WEST)));

	public ScribingDeskBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return COLLISION;
	}

	@Override
	protected VoxelShape getOcclusionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
		return COLLISION;
	}

	@Override
	protected boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer serverPlayer) {
			Contracts.readBoard(serverPlayer, pos);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}
