package dev.wildercord.client.familiar;

import dev.wildercord.familiar.Bonds;
import dev.wildercord.familiar.FamiliarContent;
import dev.wildercord.familiar.Familiars;
import dev.wildercord.familiar.WispRules;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Familiars on the client: the wisp's renderer, and the Wisp Lantern's list of your familiars. */
public final class FamiliarClient {
	private FamiliarClient() {}

	public static void init() {
		ModelLayerRegistry.registerModelLayer(WispRenderer.LAYER, WispModel::createLayer);
		EntityRendererRegistry.register(FamiliarContent.WISP, WispRenderer::new);
		// Only on the game's thread: creative search builds its index from tooltips in the background, and the
		// player's familiars aren't safe to read from there.
		Thread game = Thread.currentThread();
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (!stack.is(FamiliarContent.WISP_LANTERN) || Thread.currentThread() != game || Minecraft.getInstance().player == null) {
				return;
			}
			Bonds bonds = Familiars.get(Minecraft.getInstance().player);
			int at = Math.min(1, lines.size());
			if (bonds.bonds().isEmpty()) {
				lines.add(at, Component.translatable("tooltip.wildercord.wisp_lantern.empty").withStyle(ChatFormatting.DARK_GRAY));
				return;
			}
			for (Bonds.Bond bond : bonds.bonds()) {
				String where = bond.id().equals(bonds.out()) ? "out" : bond.waiting() ? "waiting" : "resting";
				int next = WispRules.nextLevelAt(bond.xp());
				Component level = next < 0 ? Component.translatable("tooltip.wildercord.wisp_lantern.level_max", bond.level())
					: Component.translatable("tooltip.wildercord.wisp_lantern.level", bond.level(), bond.xp(), next);
				lines.add(at++, Component.translatable("tooltip.wildercord.wisp_lantern.bond", Familiars.displayName(bond).copy().withColor(Familiars.color(bond)),
					Familiars.element(bond.element()), level, Component.translatable("tooltip.wildercord.wisp_lantern." + where)).withStyle(ChatFormatting.GRAY));
			}
		});
	}
}
