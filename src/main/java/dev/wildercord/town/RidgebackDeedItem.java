package dev.wildercord.town;

import dev.wildercord.wildlife.MountContent;
import dev.wildercord.wildlife.RidgebackStag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** A stablemaster's deed: use it and a tame, saddled Ridgeback Stag of yours is led up beside you. */
public class RidgebackDeedItem extends Item {
	public RidgebackDeedItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel server) {
			RidgebackStag stag = MountContent.RIDGEBACK_STAG.create(server, EntitySpawnReason.MOB_SUMMONED);
			if (stag == null) return InteractionResult.FAIL;
			stag.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
			stag.finalizeSpawn(server, server.getCurrentDifficultyAt(player.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
			stag.tameWithName(player);
			stag.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
			stag.setPersistenceRequired();
			server.addFreshEntity(stag);
			server.playSound(null, player.blockPosition(), SoundEvents.HORSE_SADDLE.value(), SoundSource.NEUTRAL, 1.0F, 0.9F);
			if (!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		out.accept(Component.translatable("tooltip.wildercord.ridgeback_deed").withStyle(ChatFormatting.GRAY));
	}
}
