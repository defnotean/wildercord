package dev.wildercord.aura.arts;

import dev.wildercord.aura.Aura;
import dev.wildercord.aura.MethodsBArtRules;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Echo, Dawn and Venom's own awakening flourish (they share the plain Dominion ground, but not its rise): rings that ring out
 * one after another, a sun that breaks over the head, a serpent coiling up from the feet.
 */
public final class MethodsBAwakening {
	private MethodsBAwakening() {}

	/** Raises the flourish for {@code player}'s method; false when the method isn't one of the pack's. */
	static boolean flourish(ServerPlayer player, ArtLight show, ArtLight world, Vec3 feet, Vec3 heart) {
		String method = Aura.data(player).method();
		if (method == null) return false;
		ServerLevel level = player.level();
		switch (method) {
			case MethodsBArtRules.ECHO -> {
				int violet = EchoArts.violet(player);
				for (int i = 0; i < 4; i++) {
					double r = 0.6 + i * 0.8;
					show.ring(heart, ArtKit.UP, i % 2 == 0 ? violet : MethodsBKit.SILVER, r * 0.5, r, 0.06, 8 + i * 3);
				}
				show.groundRing(feet, MethodsBKit.SILVER, 0.4, 3.2, 0.1, 14);
				world.ground(feet, SigilOption.RING, violet, 2.6, 50, 0.04);
				Motes.burst(level, heart, 14, MethodsBKit.SILVER, 0.06, 18, 0.18);
				Feels.sound(level, heart, "aura_echo_art", 0.8F, 1.0F);
				return true;
			}
			case MethodsBArtRules.DAWN -> {
				int gold = DawnArts.gold(player);
				Vec3 over = heart.add(0, 1.6, 0);
				show.flash(over, 0xFFFFFF, 2.4F);
				show.orb(over, gold, 0.7, 14);
				for (int i = 0; i < 8; i++) {
					double a = Math.PI * 2 * i / 8;
					show.bare().ray(over, over.add(Math.cos(a) * 2.4, -1.2, Math.sin(a) * 2.4), i % 2 == 0 ? gold : MethodsBKit.ROSE, 0.05, 10);
				}
				world.ground(feet, SigilOption.STAR, gold, 2.6, 50, 0.02);
				Motes.burst(level, over, 14, MethodsBKit.ROSE, 0.07, 18, 0.3);
				Feels.sound(level, heart, "aura_dawn_art", 0.8F, 1.0F);
				return true;
			}
			case MethodsBArtRules.VENOM -> {
				int acid = VenomArts.acid(player);
				for (int i = 0; i < 3; i++) {
					double y = 0.15 + i * 0.6;
					show.ring(feet.add(0, y, 0), ArtKit.UP, i % 2 == 0 ? acid : MethodsBKit.BLACK, 1.8 - i * 0.4, 2.2 - i * 0.4, 0.1, 12 + i * 2);
				}
				show.tongues(feet, 0.8, 2.2, 8, acid, MethodsBKit.BLACK, 14);
				world.ground(feet, SigilOption.CRACKED, acid, 2.6, 50, 0.03);
				Motes.burst(level, heart, 12, acid, 0.07, 18, 0.14);
				Feels.sound(level, heart, "aura_venom_art", 0.8F, 1.0F);
				return true;
			}
			default -> {
				return false;
			}
		}
	}
}
