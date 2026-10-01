package dev.wildercord.aura;

import dev.wildercord.cast.Reactions;
import dev.wildercord.config.Config;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Aura marks: an elemental aura strike (a coated blow, the slash, a chain or a spark) may leave its element's reaction mark on
 * what it struck, so a mage's spell can set it off: a Rime blade leaves a foe frozen for a fire spell's Shatter, a Gale blade
 * windswept for Wildfire, a Crimson blade bleeding for Rupture, a Hollow blade shadowed for Blight, a Starlit blade exposed for
 * Unweave, an Ember blade burning and a Verdant blade a touch of poison for Overload and Elapse. Earth, storm and time set
 * reactions off and leave nothing (storm's own mark is one a Thunder blade's next blow would conduct through itself).
 *
 * <p>Kept modest on purpose: a chance per strike (15% at Glow, 5% more a stage), marks shorter than a spell's, a foe takes
 * another mark from the same striker only after a rest, and another player is never set alight or poisoned by one. A method's own element never sets off the mark it leaves (each leaves
 * what a different element answers; Starlit's exposed only counts toward an Unweave that needs another mark too), so the reward
 * is in fighting beside a mage, or casting as one.</p>
 */
public final class AuraMarks {
	private AuraMarks() {}

	/** When each striker last marked each foe: striker, then foe, to the game time. */
	private static final Map<UUID, Map<UUID, Long>> MARKED = new HashMap<>();

	static void forget(UUID id) {
		MARKED.remove(id);
	}

	static void clear() {
		MARKED.clear();
	}

	/** An elemental aura strike of {@code player}'s landed on {@code target}: it may leave its mark. Returns whether it did. */
	static boolean strike(ServerPlayer player, LivingEntity target) {
		if (!target.isAlive() || target == player) {
			return false;
		}
		String mark = AuraRules.markFor(Aura.element(player));
		if (mark.isEmpty()) {
			return false;
		}
		long now = player.level().getGameTime();
		Map<UUID, Long> marked = MARKED.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
		Long last = marked.get(target.getUUID());
		if (last != null && now - last < AuraRules.MARK_REST) {
			return false;
		}
		double chance = AuraRules.markChance(Aura.stage(player)) * Config.get().aura().heights().markChanceMultiplier();
		if (player.getRandom().nextDouble() >= chance) {
			return false;
		}
		if (marked.size() > 64) {
			marked.values().removeIf(at -> now - at >= AuraRules.MARK_REST);
		}
		marked.put(target.getUUID(), now);
		leave(player, target, mark);
		return true;
	}

	/**
	 * Leaves {@code mark} on {@code target} (the tests call this to skip the chance). On another player only the marks that do
	 * nothing by themselves are left: no fire and no poison, which would be harm outside the PvP caps.
	 */
	public static void leave(ServerPlayer player, LivingEntity target, String mark) {
		int ticks = AuraRules.MARK_TICKS;
		if (target instanceof net.minecraft.world.entity.player.Player && (mark.equals("burning") || mark.equals("poisoned"))) {
			return;
		}
		switch (mark) {
			case "frozen" -> {
				Reactions.mark(target, Reactions.Mark.FROZEN, ticks);
				// A rime of frost to see, never so much that it freezes solid (which hurts).
				int frost = Math.min(target.getTicksRequiredToFreeze() - 1, target.getTicksFrozen() + 40);
				target.setTicksFrozen(Math.max(target.getTicksFrozen(), frost));
			}
			case "windswept" -> Reactions.mark(target, Reactions.Mark.WINDSWEPT, ticks);
			case "shadowed" -> Reactions.mark(target, Reactions.Mark.SHADOWED, ticks);
			case "bleeding" -> Reactions.mark(target, Reactions.Mark.BLEEDING, ticks);
			case "exposed" -> Reactions.mark(target, Reactions.Mark.EXPOSED, ticks);
			case "burning" -> {
				if (!target.fireImmune()) {
					target.igniteForTicks(Math.max(target.getRemainingFireTicks(), ticks));
				}
			}
			case "poisoned" -> target.addEffect(new MobEffectInstance(MobEffects.POISON, ticks, 0, false, true), player);
			default -> {
				return;
			}
		}
		AuraVfx.marked(player.level(), target, Aura.color(player));
	}
}
