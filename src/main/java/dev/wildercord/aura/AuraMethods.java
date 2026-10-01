package dev.wildercord.aura;

import dev.wildercord.cast.Grimoire;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.CreativeModeTab;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Learning a breathing method (from its manual, or taught outright through {@code api.AuraApi.grantMethod}). The first method
 * brings Glow. Switching to another keeps the stage but costs the road to the next breakthrough (experience goes back to the
 * start of the stage) and empties the aura held: so one already breathing another way is asked to read again to be sure.
 */
public final class AuraMethods {
	private AuraMethods() {}

	/** How long a switch waits for the second reading (ticks). */
	private static final int CONFIRM_TICKS = 100;

	private record Asked(String method, long at) {}

	private static final Map<UUID, Asked> ASKED = new HashMap<>();

	static void init() {
		BreathingManualItem.init();
		// Every method's manual in the mod's creative tab, after the Torn Page.
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, dev.wildercord.Wildercord.id("wildercord")))
			.register(output -> {
				java.util.List<net.minecraft.world.item.ItemStack> manuals = new java.util.ArrayList<>();
				for (BreathingMethod method : BreathingMethods.all()) {
					manuals.add(BreathingManualItem.of(method.id()));
				}
				output.insertAfter(dev.wildercord.content.WildercordItems.TORN_PAGE, manuals, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
			});
	}

	/**
	 * Learns {@code method}. Returns whether it was learned (a refusal or a request to confirm says why above the hotbar).
	 *
	 * @param source    where it came from ("manual", or a teacher's source id), for the Grimoire's line
	 * @param confirmed whether a switch needs no second asking (a teacher's lesson)
	 */
	public static boolean learn(ServerPlayer player, BreathingMethod method, String source, boolean confirmed) {
		if (!Aura.enabled(player)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.disabled").withColor(0xA89CC8));
			return false;
		}
		AuraAttachments.Data data = Aura.data(player);
		Component name = Component.translatable(method.nameKey()).withColor(0xFF000000 | method.color());
		if (data.learned() && data.method().equals(method.id())) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.already", name).withColor(0xA89CC8));
			return false;
		}
		long now = player.level().getGameTime();
		if (data.learned() && !confirmed) {
			Asked asked = ASKED.get(player.getUUID());
			if (asked == null || !asked.method().equals(method.id()) || now - asked.at() > CONFIRM_TICKS) {
				ASKED.put(player.getUUID(), new Asked(method.id(), now));
				Component current = Aura.method(player).<Component>map(m -> Component.translatable(m.nameKey()).withColor(0xFF000000 | m.color()))
					.orElse(Component.literal(data.method()));
				player.sendSystemMessage(Component.translatable("message.wildercord.aura.switch_confirm", current, name).withColor(0xE8C46A));
				return false;
			}
		}
		ASKED.remove(player.getUUID());
		boolean switching = data.learned();
		int stage = Math.max(AuraRules.GLOW, data.stage());
		double xp = switching ? AuraRules.afterSwitch(stage, AuraStages.threshold(stage)) : data.xp();
		Aura.set(player, new AuraAttachments.Data(method.id(), stage, xp, 0, data.practice()));
		Aura.breakStance(player);
		int color = method.color(stage);
		player.connection.send(new ClientboundSetTitlesAnimationPacket(6, 40, 16));
		player.connection.send(new ClientboundSetTitleTextPacket(name.copy().withColor(0xFF000000 | color)));
		player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(switching
			? "message.wildercord.aura.switched_subtitle" : "message.wildercord.aura.learned_subtitle").withColor(0xE8D8B0)));
		player.sendSystemMessage(Component.translatable(switching ? "message.wildercord.aura.switched" : "message.wildercord.aura.learned", name)
			.withColor(0xE8D8B0));
		player.sendSystemMessage(Component.translatable(method.nameKey() + ".flavour").withColor(0xB8A8D8));
		if (!switching) {
			player.sendSystemMessage(Component.translatable("message.wildercord.aura.learned_how").withColor(0x9A8CD8));
		}
		dev.wildercord.cast.Fx.sound(player.level(), player.position(), SoundEvents.BOOK_PAGE_TURN, 0.8F, 0.9F);
		Aura.sound(player, "aura_breath", 0.9F, 1.0F);
		AuraVfx.learned(player, color);
		Grimoire.unlock(player, "aura:method_" + method.id().replace(':', '.'));
		return true;
	}

	static void forget(UUID id) {
		ASKED.remove(id);
	}
}
