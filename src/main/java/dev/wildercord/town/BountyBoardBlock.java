package dev.wildercord.town;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import dev.wildercord.content.WildercordItems;
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
 * A Wayfarer Inn's bounty board. Click it to read today's bounty and your standing, again to take it; with the work done (or,
 * for a gathering, the goods in your pack), click to turn it in for emeralds and reputation; sneak-click to give a bounty up.
 * Found at inns only (it can't be broken).
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
			if (bounty.type() == BountyRules.Kind.GATHER && !player.isShiftKeyDown()) {
				Item goods = goods(bounty);
				int have = player.getInventory().countItem(goods);
				if (have >= bounty.needed()) {
					take(player, goods, bounty.needed());
					turnIn(player, level, pos, standing, bounty, today);
				} else {
					say(player, "gathering", ChatFormatting.YELLOW, have, bounty.needed(), Town.targetName(bounty));
					say(player, "abandon_hint", ChatFormatting.DARK_GRAY);
				}
			} else if (bounty.done()) {
				turnIn(player, level, pos, standing, bounty, today);
			} else if (player.isShiftKeyDown()) {
				Town.set(player, standing.with(Optional.empty()));
				say(player, "abandoned", ChatFormatting.GRAY, Town.targetName(bounty));
			} else {
				say(player, "hunting", ChatFormatting.YELLOW, bounty.kills(), bounty.needed(), Town.targetName(bounty));
				say(player, "abandon_hint", ChatFormatting.DARK_GRAY);
			}
			return;
		}
		if (!BountyRules.ready(standing.lastDay(), today)) {
			say(player, "tomorrow", ChatFormatting.GRAY);
			standingLine(player, standing);
			return;
		}
		BountyRules.Bounty offer = BountyRules.offer(player.getUUID(), today, pos.asLong(), standing.tier(),
			BountyRules.greatDue(standing.lastGreatWeek(), today));
		long[] shown = SHOWN.get(player.getUUID());
		if (shown == null || shown[0] != pos.asLong() || now > shown[1]) {
			SHOWN.put(player.getUUID(), new long[]{pos.asLong(), now + BountyRules.ACCEPT_TICKS});
			Component target = offer.name().isEmpty() ? Town.targetName(offer.target())
				: Component.translatable("town.wildercord.named_elite", offer.name(), Town.targetName(offer.target()));
			switch (offer.kind()) {
				case GATHER -> say(player, "offer_gather", ChatFormatting.GOLD, offer.needed(), target);
				case ELITE -> say(player, "offer_elite", ChatFormatting.GOLD, target, BountyRules.ELITE_FAR);
				case DUNGEON -> say(player, "offer_dungeon", ChatFormatting.GOLD);
				case GREAT -> say(player, "offer_great", ChatFormatting.LIGHT_PURPLE, offer.needed(), target, (int) BountyRules.RANGE);
				default -> say(player, "offer", ChatFormatting.GOLD, offer.needed(), target, (int) BountyRules.RANGE);
			}
			if (offer.kind() == BountyRules.Kind.GREAT) {
				say(player, "reward_great", ChatFormatting.GREEN, offer.emeralds(), BountyRules.GREAT_CRYSTALS, offer.reputation());
			} else {
				say(player, "reward", ChatFormatting.GREEN, offer.emeralds(), offer.reputation());
			}
			standingLine(player, standing);
			say(player, "accept", ChatFormatting.DARK_GRAY);
			level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.0F);
			return;
		}
		SHOWN.remove(player.getUUID());
		Town.Active taken = new Town.Active(offer.kind().id, offer.target(), offer.needed(), 0, offer.emeralds(), offer.reputation(),
			pos.getX(), pos.getZ(), level.dimension().identifier().toString(), offer.name());
		if (offer.kind() == BountyRules.Kind.ELITE) {
			Direction way = BountyHunts.loose(player, level, pos, taken);
			if (way == null) {
				say(player, "elite_hidden", ChatFormatting.GRAY);
				return;
			}
			Town.set(player, standing.with(Optional.of(taken)));
			say(player, "taken_elite", ChatFormatting.GOLD, Town.targetName(taken), Component.translatable("town.wildercord.way." + way.getSerializedName()));
		} else {
			Town.set(player, standing.with(Optional.of(taken)));
			switch (offer.kind()) {
				case GATHER -> say(player, "taken_gather", ChatFormatting.GOLD, offer.needed(), Town.targetName(taken));
				case DUNGEON -> say(player, "taken_dungeon", ChatFormatting.GOLD);
				default -> say(player, "taken", ChatFormatting.GOLD, offer.needed(), Town.targetName(taken));
			}
		}
		level.playSound(null, pos, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	/** Takes {@code count} of {@code item} from the player's pack. */
	private static void take(ServerPlayer player, Item item, int count) {
		var inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize() && count > 0; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.is(item)) {
				int n = Math.min(count, stack.getCount());
				stack.shrink(n);
				count -= n;
			}
		}
		inventory.setChanged();
	}

	private static Item goods(Town.Active bounty) {
		Identifier id = Identifier.tryParse(bounty.target());
		return id == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(id);
	}

	private static void turnIn(ServerPlayer player, ServerLevel level, BlockPos pos, Town.Standing standing, Town.Active bounty, long today) {
		BountyRules.Tier before = standing.tier();
		boolean great = bounty.type() == BountyRules.Kind.GREAT;
		Town.Standing after = new Town.Standing(standing.reputation() + bounty.reputation(), today, Optional.empty(),
			great ? BountyRules.week(today) : standing.lastGreatWeek(), standing.lastRaidDay());
		Town.set(player, after);
		give(player, new ItemStack(Items.EMERALD, bounty.emeralds()));
		if (great) give(player, new ItemStack(WildercordItems.MANA_CRYSTAL, BountyRules.GREAT_CRYSTALS));
		say(player, "paid", ChatFormatting.GOLD, bounty.emeralds(), bounty.reputation());
		if (after.tier() != before) {
			say(player, "tier_up", ChatFormatting.LIGHT_PURPLE, tierName(after.tier()));
			for (BountyRules.Kind kind : BountyRules.Kind.values()) {
				if (kind.opens == after.tier() && kind != BountyRules.Kind.GREAT) say(player, "opens." + kind.id, ChatFormatting.LIGHT_PURPLE);
			}
		}
		standingLine(player, after);
		level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.2F);
	}

	private static void give(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack)) player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
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
