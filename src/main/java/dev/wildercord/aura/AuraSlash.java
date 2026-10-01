package dev.wildercord.aura;

import dev.wildercord.aura.world.ForgedGear;
import dev.wildercord.cast.Targets;
import dev.wildercord.config.Config;
import dev.wildercord.config.WildercordConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Aura Slash (Edge): tap the Aura key and the blade looses a crescent of aura that flies ahead at chest height, cutting each
 * foe in its path once (six at most) for the weapon's damage times the slash's factor, in the method's element. It's the Crescent
 * shape's flight and look in the aura's colour, without a spell: it costs aura and has a short cooldown, and spent past empty it
 * goes out weakened, with backlash. Against players it meets the spell defences ({@link AuraCombat#projected}).
 */
public final class AuraSlash {
	private AuraSlash() {}

	/** The technique: loose a slash. */
	public static boolean loose(ServerPlayer player) {
		long now = player.level().getGameTime();
		if (!Aura.holdsWeapon(player)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.no_weapon").withColor(0xA89CC8));
			return false;
		}
		AuraAttachments.State state = Aura.state(player);
		if (now < state.slashReadyAt()) {
			return false;
		}
		WildercordConfig.AuraSettings settings = Config.get().aura();
		double price = settings.slashCost();
		AuraRules.Spend paid = Aura.spend(player, price, "slash");
		Aura.state(player, Aura.state(player).slashReady(now + settings.slashCooldownTicks()));
		double weapon = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		double damage = AuraRules.slashDamage(weapon, settings.slashDamage(), Math.max(price, 1.0E-6), price <= 0 ? 1 : paid.paid())
			* settings.damageScale();
		if (damage <= 0) {
			// A technique failing at nothing: only the backlash.
			return false;
		}
		// An aura-forged glaive's slash: harder (a bonus counted with the element, so held to the cap against a player), further,
		// wider, and through more foes.
		fly(player, damage, paid.backlash(), ForgedGear.slashBonus(player), ForgedGear.slashReach(player), ForgedGear.slashTargets(player));
		return true;
	}

	/** The crescent's flight: a step a tick, cutting what it passes, stopped by a solid block. */
	static void fly(ServerPlayer player, double damage, boolean weak) {
		fly(player, damage, weak, 1.0, 1.0, 0);
	}

	/**
	 * The crescent's flight (see {@link Crescents}), {@code bonus} times as hard (counted with its element), {@code reach} times
	 * as far and wide, through {@code extraTargets} more foes.
	 */
	static void fly(ServerPlayer player, double damage, boolean weak, double bonus, double reach, int extraTargets) {
		Vec3 aim = player.getViewVector(1.0F);
		Vec3 flat = new Vec3(aim.x, aim.y * 0.6, aim.z).normalize();
		Vec3 origin = player.getEyePosition().subtract(0, 0.45, 0);
		int color = Aura.color(player);
		player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
		Aura.sound(player, "aura_slash", 1.0F, weak ? 0.8F : 1.0F);
		AuraVfx.slashStart(player.level(), origin, flat, color);
		Crescents.launch(player, origin, flat, color, damage, bonus, AuraRules.SLASH_SPEED, AuraRules.SLASH_RANGE * reach, AuraRules.SLASH_WIDTH * reach,
			AuraRules.SLASH_TARGETS + extraTargets, weak, e -> Targets.canHarm(player, e), cutter(player));
	}

	/** What a player's crescent does to a creature it reaches: projected aura, through the spell defences against a player. */
	static Crescents.Cut cutter(ServerPlayer player) {
		return (flight, target) -> AuraCombat.projected(player, target, flight.damage(), flight.bonus(), true);
	}
}
