package dev.wildercord.content;

import dev.wildercord.player.Mana;
import dev.wildercord.player.WildercordAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** Use it to raise your max mana by 10, forever. Up to 100 crystals count. */
public class ManaCrystalItem extends Item {
	public ManaCrystalItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return Mana.crystals(player) < Mana.MAX_CRYSTALS ? InteractionResult.SUCCESS : InteractionResult.FAIL;
		}
		int crystals = Mana.crystals(serverPlayer);
		if (crystals >= Mana.MAX_CRYSTALS) {
			serverPlayer.sendOverlayMessage(Component.translatable("message.wildercord.crystals_full", Mana.MAX_CRYSTALS).withStyle(ChatFormatting.GRAY));
			return InteractionResult.FAIL;
		}
		serverPlayer.setAttached(WildercordAttachments.CRYSTALS, crystals + 1);
		stack.consume(1, player);
		ServerLevel server = serverPlayer.level();
		server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.2F);
		server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.6F);
		dev.wildercord.cast.Fx.sendParticles(server, ParticleTypes.ENCHANT, player.getX(), player.getY() + 0.2, player.getZ(), 40, 0.6, 0.2, 0.6, 0.6);
		serverPlayer.sendOverlayMessage(Component.translatable("message.wildercord.crystal_used", Mana.CRYSTAL_MANA, crystals + 1, Mana.MAX_CRYSTALS)
			.withColor(0xB8A8FF));
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable("tooltip.wildercord.mana_crystal", Mana.CRYSTAL_MANA, Mana.MAX_CRYSTALS).withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable("tooltip.wildercord.mana_crystal.use").withStyle(ChatFormatting.DARK_AQUA));
	}
}
