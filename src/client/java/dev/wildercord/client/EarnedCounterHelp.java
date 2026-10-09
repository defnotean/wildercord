package dev.wildercord.client;

import dev.wildercord.aura.MastersStyleRules;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Counter controls are always read from the current mappings, including the actual rebound Aura key. */
public final class EarnedCounterHelp {
	private EarnedCounterHelp() {}
	public static Component controls(String art) {
		var style = MastersStyleRules.of(art);
		if (style == null || style.targets() != MastersStyleRules.TargetPolicy.EARNED_COUNTER) return Component.empty();
		var options = Minecraft.getInstance().options;
		return Component.translatable("screen.wildercord.aura.earned_counter_controls", options.keyShift.getTranslatedKeyMessage(),
			WildercordKeys.auraMapping().getTranslatedKeyMessage(), options.keyAttack.getTranslatedKeyMessage());
	}
}
