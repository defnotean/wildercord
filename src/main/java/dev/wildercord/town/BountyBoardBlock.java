package dev.wildercord.town;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A Wayfarer Inn's bounty board. Click it to read today's bounty and your standing, again to take it; with the hunt done,
 * click to turn it in for emeralds and reputation; sneak-click to give a bounty up. Found at inns only (it can't be broken).
 */
public class BountyBoardBlock extends Block {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final VoxelShape NORTH_SOUTH = Block.box(0, 0, 6, 16, 16, 10);
	private static final VoxelShape EAST_WEST = Block.box(6, 0, 0, 10, 16, 16);
	/** Who was last shown an offer at which board, and until when it can be taken. */
	private static final Map<UUID, long[]> SHOWN = new HashMap<>();

	public BountyBoardBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(FACING).getAxis() == Direction.Axis.Z ? NORTH_SOUTH : EAST_WEST;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer server && level instanceof ServerLevel serverLevel) {
			use(server, serverLevel, pos);
		}
		return InteractionResult.SUCCESS;
	}

	public static void use(ServerPlayer player, ServerLevel level, BlockPos pos) {
		Town.Standing standing = Town.standing(player);
		long now = level.getGameTime();
		long today = now / BountyRules.DAY;
		if (standing.bounty().isPresent()) {
			Town.Active bounty = standing.bounty().get();
			if (bounty.done()) {
				turnIn(player, level, pos, standing, bounty, today);
			} else if (player.isShiftKeyDown()) {
				Town.set(player, standing.with(Optional.empty()));
				say(player, "abandoned", ChatFormatting.GRAY, Town.targetName(bounty.target()));
			} else {
				say(player, "hunting", ChatFormatting.YELLOW, bounty.kills(), bounty.needed(), Town.targetName(bounty.target()));
				say(player, "abandon_hint", ChatFormatting.DARK_GRAY);
			}
			return;
		}
		if (!BountyRules.ready(standing.lastDay(), today)) {
			say(player, "tomorrow", ChatFormatting.GRAY);
			standingLine(player, standing);
			return;
		}
		BountyRules.Bounty offer = BountyRules.offer(player.getUUID(), today, pos.asLong());
		long[] shown = SHOWN.get(player.getUUID());
		if (shown == null || shown[0] != pos.asLong() || now > shown[1]) {
			SHOWN.put(player.getUUID(), new long[]{pos.asLong(), now + BountyRules.ACCEPT_TICKS});
			say(player, "offer", ChatFormatting.GOLD, offer.needed(), Town.targetName(offer.target()), (int) BountyRules.RANGE);
			say(player, "reward", ChatFormatting.GREEN, offer.emeralds(), offer.reputation());
			standingLine(player, standing);
			say(player, "accept", ChatFormatting.DARK_GRAY);
			level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.0F);
			return;
		}
		SHOWN.remove(player.getUUID());
		Town.set(player, standing.with(Optional.of(new Town.Active(offer.target(), offer.needed(), 0, offer.emeralds(), offer.reputation(),
			pos.getX(), pos.getZ(), level.dimension().identifier().toString()))));
		say(player, "taken", ChatFormatting.GOLD, offer.needed(), Town.targetName(offer.target()));
		level.playSound(null, pos, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	private static void turnIn(ServerPlayer player, ServerLevel level, BlockPos pos, Town.Standing standing, Town.Active bounty, long today) {
		BountyRules.Tier before = standing.tier();
		Town.Standing after = new Town.Standing(standing.reputation() + bounty.reputation(), today, Optional.empty());
		Town.set(player, after);
		ItemStack pay = new ItemStack(Items.EMERALD, bounty.emeralds());
		if (!player.getInventory().add(pay)) player.drop(pay, false, net.minecraft.util.Prediction.SERVER_ONLY);
		say(player, "paid", ChatFormatting.GOLD, bounty.emeralds(), bounty.reputation());
		if (after.tier() != before) {
			say(player, "tier_up", ChatFormatting.LIGHT_PURPLE, tierName(after.tier()));
		}
		standingLine(player, after);
		level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.2F);
	}

	static void standingLine(ServerPlayer player, Town.Standing standing) {
		BountyRules.Tier next = standing.tier().next();
		if (next == null) {
			say(player, "standing_top", ChatFormatting.AQUA, tierName(standing.tier()), standing.reputation());
		} else {
			say(player, "standing", ChatFormatting.AQUA, tierName(standing.tier()), standing.reputation(), next.needs, tierName(next));
		}
	}

	public static Component tierName(BountyRules.Tier tier) {
		return Component.translatable("town.wildercord.tier." + tier.id);
	}

	private static void say(ServerPlayer player, String key, ChatFormatting colour, Object... args) {
		player.sendSystemMessage(Component.translatable("message.wildercord.bounty." + key, args).withStyle(colour));
	}
}
