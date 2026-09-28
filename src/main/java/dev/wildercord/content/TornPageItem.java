package dev.wildercord.content;

import dev.wildercord.cast.Grimoire;
import dev.wildercord.spell.Secrets;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

/**
 * A Torn Page from an old grimoire: read it to learn the riddle of a secret spell you haven't
 * found yet. The riddle goes into your Grimoire, where it waits until you solve it.
 */
public class TornPageItem extends Item {
	public TornPageItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player instanceof ServerPlayer serverPlayer) {
			Secrets.Secret secret = Grimoire.hint(serverPlayer);
			if (secret == null) {
				serverPlayer.sendOverlayMessage(Component.translatable("message.wildercord.page_nothing").withStyle(ChatFormatting.GRAY));
				return InteractionResult.FAIL;
			}
			stack.consume(1, player);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.8F);
			serverPlayer.sendSystemMessage(Component.translatable("message.wildercord.page_read").withColor(0xC8B89A).withStyle(ChatFormatting.ITALIC));
			serverPlayer.sendSystemMessage(Component.literal("  “" + secret.riddle() + "”").withColor(0xE8D8B0).withStyle(ChatFormatting.ITALIC));
			// The margin: a sketch of the way to the nearest Archive, in the overworld.
			if (serverPlayer.level().dimension() == Level.OVERWORLD) {
				net.minecraft.core.BlockPos archive = serverPlayer.level().findNearestMapStructure(dev.wildercord.world.WildercordWorldgen.ARCHIVES,
					serverPlayer.blockPosition(), 48, false);
				if (archive != null) {
					double dx = archive.getX() - serverPlayer.getX();
					double dz = archive.getZ() - serverPlayer.getZ();
					int blocks = (int) Math.round(Math.sqrt(dx * dx + dz * dz) / 50.0) * 50;
					serverPlayer.sendSystemMessage(Component.translatable("message.wildercord.page_map", Math.max(50, blocks),
						Component.translatable("direction.wildercord." + compass(dx, dz))).withColor(0x9A8CD8).withStyle(ChatFormatting.ITALIC));
				}
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** The eight compass points, for the page's map sketch. */
	private static String compass(double dx, double dz) {
		String[] points = {"south", "southwest", "west", "northwest", "north", "northeast", "east", "southeast"};
		double angle = Math.toDegrees(Math.atan2(-dx, dz));
		return points[Math.floorMod((int) Math.round(angle / 45.0), 8)];
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable("tooltip.wildercord.torn_page").withStyle(ChatFormatting.GRAY));
	}
}
