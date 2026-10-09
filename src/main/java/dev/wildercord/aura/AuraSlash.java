package dev.wildercord.aura;

import dev.wildercord.aura.world.ForgedGear;
import dev.wildercord.cast.Targets;
import dev.wildercord.config.Config;
import dev.wildercord.config.WildercordConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
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
        if ((dev.wildercord.cast.ExciseCasting.blocking(player) || dev.wildercord.cast.LessonPackCasting.blocking(player))) return false;
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
		// Awakened on the Way of the Blade at Sovereign, it's free and quick.
		// A bonded blade's Long Crescent: cheaper, and it flies further.
		double price = WayEffects.slashPrice(player, settings.slashCost()) * BladeTraits.slashPrice(player);
		AuraRules.Spend paid = Aura.spend(player, price, "slash");
		BondedBlades.slashed(player);
		Aura.state(player, Aura.state(player).slashReady(now + WayEffects.slashRest(player, settings.slashCooldownTicks())));
		double weapon = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		double damage = AuraRules.slashDamage(weapon, settings.slashDamage(), Math.max(price, 1.0E-6), price <= 0 ? 1 : paid.paid())
			* settings.damageScale();
		if (damage <= 0) {
			// A technique failing at nothing: only the backlash (a spell riding the blade stays on it, for a slash that can carry it).
			return false;
		}
		// An aura-forged glaive's slash: harder (a bonus counted with the element, so held to the cap against a player), further,
		// wider, and through more foes. A spell riding the blade (Spellblade) goes with it.
		fly(player, damage, paid.backlash(), ForgedGear.slashBonus(player), ForgedGear.slashReach(player) * BladeTraits.slashReach(player),
			ForgedGear.slashTargets(player), Spellblade.take(player));
		return true;
	}

	/** The crescent's flight: a step a tick, cutting what it passes, stopped by a solid block. */
	static void fly(ServerPlayer player, double damage, boolean weak) {
		fly(player, damage, weak, 1.0, 1.0, 0, null);
	}

	/**
	 * The crescent's flight (see {@link Crescents}), {@code bonus} times as hard (counted with its element), {@code reach} times
	 * as far and wide, through {@code extraTargets} more foes, carrying {@code spell} (riding the blade, or null). A carried spell
	 * lands on the first few foes it cuts in place of its own shape, or bursts where the slash ends if it cuts none (a wall, a
	 * clash, a guard, the end of its flight), and the rest of the spell follows once it has flown.
	 */
	static void fly(ServerPlayer player, double damage, boolean weak, double bonus, double reach, int extraTargets, Spellblade.Held spell) {
		Vec3 aim = player.getViewVector(1.0F);
		Vec3 flat = new Vec3(aim.x, aim.y * 0.6, aim.z).normalize();
		Vec3 origin = player.getEyePosition().subtract(0, 0.45, 0);
		int color = Aura.color(player);
		boolean carrying = spell != null && dev.wildercord.cast.BladeCasting.begin(spell.cast(), spell.root());
		int[] carried = {0};
		Entity[] first = {null};
		int edge = carrying ? spell.color() : -1;
		player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
		Aura.sound(player, "aura_slash", 1.0F, weak ? 0.8F : 1.0F);
		if (carrying) {
			Aura.sound(player, "aura_spellblade", 1.0F, 1.25F);
		}
		AuraVfx.slashStart(player.level(), origin, flat, color);
		// The blade's draw as the crescent leaves it, and the method's technique sound under the slash's own.
		AuraFx.trail(player, AuraFxRules.Stroke.DRAW, false, weak ? 1.0F : 1.3F);
		AuraFx.sound(player, AuraFx.Sound.ART, 0.6F, weak ? 0.85F : 1.1F);
		Crescents.Cut base = cutter(player);
		Crescents.Cut cut = !carrying ? base : (flight, target) -> {
			float taken = base.cut(flight, target);
			if (target.isAlive()) {
				double power = AuraRules.spellbladePower(carried[0]);
				if (power > 0) {
					if (first[0] == null) {
						first[0] = target;
					}
					carried[0]++;
					dev.wildercord.cast.BladeCasting.cut(spell.cast(), spell.root(), target, origin, flat, power);
					AuraVfx.slashCut(flight.level, target.getBoundingBox().getCenter(), flat, edge);
				}
			}
			return taken;
		};
		// The Way of the Blade's slash pierces: through a held guard, through more foes, and through a crescent it meets.
		boolean pierces = WayEffects.pierces(player);
		Crescents.Flight flight = Crescents.launch(player, origin, flat, color, damage, bonus, AuraRules.SLASH_SPEED, AuraRules.SLASH_RANGE * reach,
			AuraRules.SLASH_WIDTH * reach, AuraRules.SLASH_TARGETS + extraTargets + (pierces ? WayRules.BLADE_SLASH_TARGETS : 0), weak,
			e -> Targets.canHarm(player, e), cut);
		if (pierces) {
			flight.pierce();
		}
		if (carrying) {
			flight.onStep(f -> AuraVfx.slashCarry(f.level, f.front, f.aim, f.side, edge, f.step));
			flight.onEnd((f, at, blocked) -> {
				if (!player.isAlive() || player.level() != f.level) {
					return;
				}
				// The end of its flight: a spell that cut nothing bursts here, and the rest of it follows.
				if (carried[0] == 0) {
					dev.wildercord.cast.BladeCasting.broke(spell.cast(), spell.root(), at, flat, origin, blocked,
						blocked == null ? null : net.minecraft.core.Direction.getApproximateNearest(-flat.x, -flat.y, -flat.z));
				}
				dev.wildercord.cast.BladeCasting.follow(spell.cast(), spell.root(), at, flat, first[0]);
			});
		}
	}

	/**
	 * What a player's crescent does to a creature it reaches: projected aura, through the spell defences against a player, landing
	 * heavily (a flash, and the moment held for the swordsman and a struck player).
	 */
	static Crescents.Cut cutter(ServerPlayer player) {
		return (flight, target) -> {
			float taken = AuraCombat.projected(player, target, flight.damage(), flight.bonus(), true);
			if (taken > 0) {
				AuraFx.impact(player, target, flight.color(), Aura.stage(player), AuraFxRules.Weight.HEAVY);
			}
			return taken;
		};
	}
}
