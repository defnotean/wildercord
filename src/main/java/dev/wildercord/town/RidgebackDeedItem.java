package dev.wildercord.town;

import net.minecraft.world.InteractionResultHolder;
import dev.wildercord.compat.McEntities;

import java.util.List;
import dev.wildercord.wildlife.MountContent;
import dev.wildercord.wildlife.RidgebackStag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** A stablemaster's deed: use it and a tame, saddled Ridgeback Stag of yours is led up beside you. */
public class RidgebackDeedItem extends Item {
	public RidgebackDeedItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) { ItemStack mcUseStack = player.getItemInHand(hand);
		if (level instanceof ServerLevel server) {
			RidgebackStag stag = McEntities.create(MountContent.RIDGEBACK_STAG, server, MobSpawnType.MOB_SUMMONED);
			if (stag == null) return InteractionResultHolder.fail(mcUseStack);
			stag.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
			stag.finalizeSpawn(server, server.getCurrentDifficultyAt(player.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
			stag.tameWithName(player);
			stag.equipSaddle(new ItemStack(Items.SADDLE), null);
			stag.setPersistenceRequired();
			server.addFreshEntity(stag);
			server.playSound(null, player.blockPosition(), SoundEvents.HORSE_SADDLE, SoundSource.NEUTRAL, 1.0F, 0.9F);
			if (!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
		}
		return InteractionResultHolder.success(mcUseStack);
	}

	@Override
	public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> wildercord$lines,TooltipFlag flag) {
		Consumer<Component> out = wildercord$lines::add;
		out.accept(Component.translatable("tooltip.wildercord.ridgeback_deed").withStyle(ChatFormatting.GRAY));
	}
}
