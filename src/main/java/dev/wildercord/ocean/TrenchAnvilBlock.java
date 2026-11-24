package dev.wildercord.ocean;

import dev.wildercord.Wildercord;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Heavy basalt-and-brass Hydrothermal Trench Anvil used inside underwater Vent Forges to craft and
 * repair Abyssal Diving gear, Harpoons, Glaives, Lanterns, and Tidecaller Conches.
 */
public final class TrenchAnvilBlock extends Block implements SimpleWaterloggedBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

	public TrenchAnvilBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BlockStateProperties.WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, BlockStateProperties.WATERLOGGED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		boolean wet = context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER);
		return defaultBlockState()
			.setValue(FACING, context.getHorizontalDirection().getClockWise())
			.setValue(BlockStateProperties.WATERLOGGED, wet);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return rotate(state, mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected BlockState updateShape(
		BlockState state,
		Direction dir,
		BlockState neighborState,
		net.minecraft.world.level.LevelAccessor level,
		BlockPos pos,
		BlockPos neighborPos
	) {
		if (state.getValue(BlockStateProperties.WATERLOGGED)) {
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return super.updateShape(state, dir, neighborState, level, pos, neighborPos);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level instanceof ServerLevel && player instanceof ServerPlayer sp) {
			sp.sendSystemMessage(Component.translatable("message.wildercord.trench_anvil.hint")
				.withStyle(ChatFormatting.AQUA));
			return net.minecraft.world.item.ItemInteractionResult.SUCCESS;
		}
		return net.minecraft.world.item.ItemInteractionResult.SUCCESS;
	}

	@Override
	protected net.minecraft.world.item.ItemInteractionResult useItemOn(
		ItemStack stack,
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		InteractionHand hand,
		BlockHitResult hit
	) {
		if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) {
			return net.minecraft.world.item.ItemInteractionResult.SUCCESS;
		}
		if (stack.isEmpty()) {
			return useWithoutItem(state, level, pos, player, hit);
		}
		if (OceanItems.isAbyssalRepairable(stack) && stack.isDamaged()) {
			Item plate = OceanItems.ABYSSAL_IRON_PLATE;
			if (countItem(sp, plate) >= 1) {
				consumeItem(sp, plate, 1);
				stack.setDamageValue(Math.max(0, stack.getDamageValue() - Math.max(1, stack.getMaxDamage() / 2)));
				forgeEffects(server, pos, SoundEvents.ANVIL_USE);
				sp.sendSystemMessage(Component.translatable("message.wildercord.trench_anvil.repaired")
					.withStyle(ChatFormatting.GREEN));
				return net.minecraft.world.item.ItemInteractionResult.SUCCESS;
			}
		}

		String heldId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		for (OceanRules.TrenchForgeRecipe recipe : OceanRules.TRENCH_RECIPES) {
			if (!recipe.baseItemId().equals(heldId)) continue;
			Item mat1 = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(recipe.primaryMaterialId())).orElse(null);
			Item mat2 = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(recipe.secondaryMaterialId())).orElse(null);
			Item outItem = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(recipe.outputItemId())).orElse(null);
			if (mat1 == null || mat2 == null || outItem == null) continue;

			if (countItem(sp, mat1) >= recipe.primaryCount() && countItem(sp, mat2) >= recipe.secondaryCount()) {
				stack.shrink(1);
				consumeItem(sp, mat1, recipe.primaryCount());
				consumeItem(sp, mat2, recipe.secondaryCount());
				ItemStack forged = new ItemStack(outItem);
				if (!sp.getInventory().add(forged)) {
					sp.spawnAtLocation(server, forged);
				}
				forgeEffects(server, pos, SoundEvents.ANVIL_USE);
				sp.sendSystemMessage(Component.translatable("message.wildercord.trench_anvil.forged", outItem.getName(forged))
					.withStyle(ChatFormatting.GOLD));
				Wildercord.LOGGER.info("Player {} forged {} at Trench Anvil", sp.getScoreboardName(), recipe.outputItemId());
				return net.minecraft.world.item.ItemInteractionResult.SUCCESS;
			} else {
				sp.sendSystemMessage(Component.translatable(
					"message.wildercord.trench_anvil.missing",
					recipe.primaryCount(), mat1.getName(new ItemStack(mat1)),
					recipe.secondaryCount(), mat2.getName(new ItemStack(mat2)))
					.withStyle(ChatFormatting.YELLOW));
				return net.minecraft.world.item.ItemInteractionResult.SUCCESS;
			}
		}

		sp.sendSystemMessage(Component.translatable("message.wildercord.trench_anvil.hint")
			.withStyle(ChatFormatting.AQUA));
		return net.minecraft.world.item.ItemInteractionResult.SUCCESS;
	}

	private static int countItem(ServerPlayer player, Item target) {
		int total = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack slot = player.getInventory().getItem(i);
			if (slot.is(target)) total += slot.getCount();
		}
		return total;
	}

	private static void consumeItem(ServerPlayer player, Item target, int count) {
		int left = count;
		for (int i = 0; i < player.getInventory().getContainerSize() && left > 0; i++) {
			ItemStack slot = player.getInventory().getItem(i);
			if (!slot.is(target)) continue;
			int take = Math.min(left, slot.getCount());
			slot.shrink(take);
			left -= take;
		}
	}

	private static void forgeEffects(ServerLevel level, BlockPos pos, net.minecraft.sounds.SoundEvent sound) {
		level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0f, 0.9f);
		level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 10, 0.25, 0.1, 0.25, 0.02);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 12, 0.25, 0.15, 0.25, 0.04);
	}
}
