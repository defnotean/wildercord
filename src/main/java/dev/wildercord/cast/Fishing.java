package dev.wildercord.cast;

import dev.wildercord.cast.events.WorldEvents;
import dev.wildercord.content.FishingRules;
import dev.wildercord.content.SigilOption;
import dev.wildercord.spell.Feats;
import dev.wildercord.world.LeyLines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Runes on a fishing line. What the magic waters loot condition reads at the bobber (a mana storm over it, a ley line
 * under it, a thunderstorm raining on it, and whether it fished open water), and what happens when a rune comes up:
 * the angler's Grimoire earns Reeled In, and a rune that magic waters tangled in the line shows itself at the bobber.
 * The odds are in {@link FishingRules}; {@code content.WildercordLoot} builds the loot.
 */
public final class Fishing {
	private Fishing() {}

	/** The light a rune tangled in the line shows at the bobber: pale water-violet, between the sea's blue and a ley line's. */
	private static final int TANGLED = 0xA8B4FF;

	/** Whether a mana storm rages over this point. */
	public static boolean underStorm(ServerLevel level, Vec3 at) {
		return WorldEvents.stormAt(level, at) != null;
	}

	/** Whether this point is on or near a ley line (ley lines run in the Overworld only). */
	public static boolean nearLey(ServerLevel level, Vec3 at) {
		return level.dimension() == Level.OVERWORLD && LeyLines.strength(LeyWalker.seed(level), at.x, at.z) >= FishingRules.NEAR_LEY;
	}

	/**
	 * Whether a thunderstorm reaches this point: its rain (or its snow, high up or in the cold) falls on the surface
	 * above it, where a bobber floats. Not under a roof, and not where no weather falls (a desert).
	 */
	public static boolean inThunder(ServerLevel level, Vec3 at) {
		return level.isThundering()
			&& level.precipitationAt(BlockPos.containing(at).above()) != net.minecraft.world.level.biome.Biome.Precipitation.NONE;
	}

	/**
	 * The chance (0 to 1) that a catch brought up at {@code origin} on {@code bobber}'s line also brings up a rune
	 * tangled in it. None without a bobber (loot rolled some other way) or unless it fished open water, as treasure.
	 */
	public static double magicWatersChance(ServerLevel level, Vec3 origin, Entity bobber, double multiplier) {
		if (!(bobber instanceof FishingHook hook) || !hook.isOpenWaterFishing()) {
			return 0;
		}
		return FishingRules.magicWatersChance(underStorm(level, origin), nearLey(level, origin), inThunder(level, origin), multiplier);
	}

	/**
	 * A rune came up on {@code bobber}'s line. Its angler earns Reeled In the first time, and one that magic waters
	 * tangled in the line shows itself: a glint and a ring of light on the water, and a line above the hotbar.
	 */
	public static void caught(Entity bobber, boolean magic) {
		if (!(bobber instanceof FishingHook hook) || !(hook.getPlayerOwner() instanceof ServerPlayer angler)
				|| !(hook.level() instanceof ServerLevel level)) {
			return;
		}
		Grimoire.feat(angler, Feats.REELED_IN);
		if (!magic) {
			return;
		}
		angler.sendOverlayMessage(Component.translatable("message.wildercord.fishing_magic").withColor(TANGLED));
		Vec3 at = hook.position();
		Vfx.emit(level, SigilOption.glow(TANGLED, 0.9F), at.add(0, 0.3, 0), 1, 0.0, 0.0);
		ElementFx.groundRing(level, at.add(0, 0.05, 0), TANGLED, 0.2, 1.8, 0.06, 14);
		Vfx.emit(level, ParticleTypes.END_ROD, at.add(0, 0.3, 0), 8, 0.3, 0.04);
		Vfx.emit(level, ParticleTypes.SPLASH, at, 12, 0.3, 0.1);
		Fx.sound(level, at, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.4F);
	}
}
