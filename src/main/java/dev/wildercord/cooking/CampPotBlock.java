package dev.wildercord.cooking;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Camp Pot: set it over a lit campfire, fire, magma or lava. Right-click with a bowl to cook from what's in your pack (the
 * meal you picked, else the richest you can make); with an empty hand to read the cookbook; sneak with an empty hand to pick
 * the next meal you can make.
 */
public class CampPotBlock extends Block {
	private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 9, 14), Block.box(1, 9, 1, 15, 11, 15));
	/** The meal each player last picked (forgotten on a restart, which only sends the pot back to the richest). */
	private static final Map<UUID, String> CHOSEN = new HashMap<>();

	public CampPotBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	public static boolean heated(Level level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		return CampfireBlock.isLitCampfire(below) || below.is(BlockTags.FIRE) || below.is(Blocks.MAGMA_BLOCK) || below.is(Blocks.LAVA);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(player instanceof ServerPlayer server)) return InteractionResult.SUCCESS;
		Map<String, Integer> have = pantry(server.getInventory());
		if (player.isShiftKeyDown()) {
			CookingRules.Recipe next = CookingRules.next(have, CHOSEN.get(player.getUUID()));
			if (next == null) {
				server.sendOverlayMessage(Component.translatable("message.wildercord.pot.nothing").withStyle(ChatFormatting.GRAY));
			} else {
				CHOSEN.put(player.getUUID(), next.id());
				server.sendOverlayMessage(Component.translatable("message.wildercord.pot.chosen", name(next)).withStyle(ChatFormatting.GOLD));
			}
			return InteractionResult.SUCCESS;
		}
		server.sendSystemMessage(Component.translatable("message.wildercord.pot.cookbook").withStyle(ChatFormatting.GOLD));
		for (CookingRules.Recipe recipe : CookingRules.RECIPES) {
			server.sendSystemMessage(line(recipe, recipe.affordable(have)));
		}
		if (!heated(level, pos)) server.sendSystemMessage(Component.translatable("message.wildercord.pot.cold").withStyle(ChatFormatting.RED));
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!stack.is(Items.BOWL)) {
			return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
		}
		if (!(player instanceof ServerPlayer server) || !(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
		if (!heated(level, pos)) {
			server.sendOverlayMessage(Component.translatable("message.wildercord.pot.cold").withStyle(ChatFormatting.RED));
			return InteractionResult.SUCCESS;
		}
		Inventory inventory = server.getInventory();
		CookingRules.Recipe recipe = CookingRules.pick(pantry(inventory), CHOSEN.get(player.getUUID()));
		if (recipe == null) {
			server.sendOverlayMessage(Component.translatable("message.wildercord.pot.nothing").withStyle(ChatFormatting.GRAY));
			return InteractionResult.SUCCESS;
		}
		for (CookingRules.Need need : recipe.needs()) {
			take(server, item(need.item()), need.count());
		}
		if (!player.getAbilities().instabuild) stack.shrink(1);
		give(server, new ItemStack(Meals.MEALS.get(recipe.id())));
		level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8F, 1.1F);
		serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 4, 0.15, 0.1, 0.15, 0.01);
		serverLevel.sendParticles(ParticleTypes.BUBBLE_POP, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 8, 0.25, 0.05, 0.25, 0.02);
		server.sendOverlayMessage(Component.translatable("message.wildercord.pot.cooked", name(recipe)).withStyle(ChatFormatting.GOLD));
		return InteractionResult.SUCCESS;
	}

	private static Component name(CookingRules.Recipe recipe) {
		return Component.translatable(Meals.MEALS.get(recipe.id()).getDescriptionId());
	}

	private static Component line(CookingRules.Recipe recipe, boolean affordable) {
		MutableComponent line = Component.literal(affordable ? "✔ " : "• ").append(name(recipe)).append(": ");
		boolean first = true;
		for (CookingRules.Need need : recipe.needs()) {
			if (!first) line.append(", ");
			first = false;
			if (need.count() > 1) line.append(need.count() + "× ");
			line.append(Component.translatable(item(need.item()).getDescriptionId()));
		}
		line.append(" → ");
		first = true;
		for (CookingRules.Buff buff : recipe.buffs()) {
			if (!first) line.append(", ");
			first = false;
			line.append(Meals.describe(buff));
		}
		return line.withStyle(affordable ? ChatFormatting.GREEN : ChatFormatting.GRAY);
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
	}

	static Map<String, Integer> pantry(Inventory inventory) {
		Map<String, Integer> have = new HashMap<>();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty()) have.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), Integer::sum);
		}
		return have;
	}

	private static void give(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack)) player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
	}

	private static void take(ServerPlayer player, Item item, int count) {
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize() && count > 0; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.is(item)) {
				int taken = Math.min(count, stack.getCount());
				stack.shrink(taken);
				count -= taken;
				// A honey bottle gives its bottle back.
				if (item == Items.HONEY_BOTTLE) give(player, new ItemStack(Items.GLASS_BOTTLE, taken));
			}
		}
	}
}
