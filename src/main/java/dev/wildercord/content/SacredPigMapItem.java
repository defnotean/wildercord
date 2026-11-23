package dev.wildercord.content;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.function.Consumer;

/** A boss-dropped paper pointing to a forest village with a sick sacred pig. */
public class SacredPigMapItem extends Item {
	public static final TagKey<Structure> SACRED_PIG_VILLAGE = TagKey.create(Registries.STRUCTURE,
		dev.wildercord.Wildercord.id("sacred_pig_village"));

	public SacredPigMapItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
		ServerLevel sl = (ServerLevel) sp.level();
		BlockPos here = sp.blockPosition();
		BlockPos found = sl.findNearestMapStructure(SACRED_PIG_VILLAGE, here, 100, false);
		if (found == null) {
			sp.sendSystemMessage(Component.translatable("message.wildercord.sacred_pig_map.lost").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
			return InteractionResult.FAIL;
		}
		SacredPigMap coords = new SacredPigMap(found.getX(), found.getZ());
		stack.set(WildercordComponents.SACRED_PIG_MAP, coords);
		sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.9F);
		sp.sendSystemMessage(Component.translatable("message.wildercord.sacred_pig_map.found", coords.x(), coords.z()).withColor(0xD8C08A).withStyle(ChatFormatting.ITALIC));
		sp.sendSystemMessage(Component.translatable("message.wildercord.sacred_pig_map.lore").withColor(0xA8C49A).withStyle(ChatFormatting.ITALIC));
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		SacredPigMap coords = stack.get(WildercordComponents.SACRED_PIG_MAP);
		if (coords != null) {
			builder.accept(Component.translatable("tooltip.wildercord.sacred_pig_map.coords", coords.x(), coords.z()).withStyle(ChatFormatting.GOLD));
		} else {
			builder.accept(Component.translatable("tooltip.wildercord.sacred_pig_map").withStyle(ChatFormatting.GRAY));
		}
	}
}
