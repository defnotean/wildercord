package dev.wildercord.player;

import dev.wildercord.Wildercord;
import dev.wildercord.spell.TribulationRules;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The Tribulation's signature (0.12 "Tempering"): every tribulation circle a heart holds (the 5th, 10th, 15th and 20th) leaves
 * a scar, one more heart of health for good. Worked out again from the circles whenever it might change (joining,
 * respawning, a circle forming or being given back), so it never drifts from the heart.
 */
public final class TribulationScars {
	private TribulationScars() {}

	private static final Identifier HEALTH = Wildercord.id("tribulation_scars");

	public static void init() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> apply(handler.player)));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> apply(newPlayer));
	}

	public static void apply(ServerPlayer player) {
		AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
		if (health == null || player.isRemoved()) return;
		int scars = TribulationRules.scars(Heart.circles(player));
		if (scars <= 0) {
			health.removeModifier(HEALTH);
		} else {
			health.addOrReplacePermanentModifier(new AttributeModifier(HEALTH, scars * TribulationRules.SCAR_HEALTH, AttributeModifier.Operation.ADD_VALUE));
		}
		if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
	}
}
