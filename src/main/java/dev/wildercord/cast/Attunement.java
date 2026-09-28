package dev.wildercord.cast;

import dev.wildercord.content.RuneItem;
import dev.wildercord.content.SigilOption;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Attunements;
import dev.wildercord.spell.RuneColors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Attunement: meditate with a Blank Rune in hand where a biome holds a rune (see {@link Attunements}
 * for which and when) and it drinks the land in over twenty seconds. A circle opens under the
 * caster in the rune's colour, motes rise from the ground into the blank, and it brightens through
 * four stages, each with a chime, until the Blank Rune becomes the biome's rune and the Grimoire
 * records the attunement. Moving, standing up or letting go of the blank breaks it off. Checked
 * every 5 ticks from {@link SpellCaster}, straight after {@link Meditation}.
 */
public final class Attunement {
	private Attunement() {}

	/** Checks (of 5 ticks each) the meditation must hold. */
	public static final int CHECKS = Attunements.SECONDS * 4;
	private static final int STAGES = 4;

	private record State(String rule, int checks, long quietUntil) {}

	private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

	public static void init() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> STATES.clear());
	}

	public static void tick(ServerPlayer player) {
		boolean meditating = player.getAttachedOrElse(WildercordAttachments.MEDITATING, false);
		InteractionHand hand = blankHand(player);
		State state = STATES.get(player.getUUID());
		long now = player.level().getGameTime();
		if (!meditating || hand == null) {
			if (state != null && state.checks() > 0) {
				broken(player);
			}
			if (state != null) {
				STATES.put(player.getUUID(), new State("", 0, state.quietUntil()));
			}
			return;
		}
		Attunements.Place place = placeOf(player);
		Optional<Attunements.Rule> match = Attunements.match(place);
		if (match.isEmpty()) {
			long quiet = state == null ? 0 : state.quietUntil();
			if (now >= quiet) {
				// Say it once a meditation (at most every half minute): a biome that holds a rune says so.
				player.sendOverlayMessage(Component.translatable(Attunements.biomeHolds(place.biome())
					? "message.wildercord.attune_not_now" : "message.wildercord.attune_quiet").withStyle(ChatFormatting.GRAY));
				quiet = now + 600;
			}
			STATES.put(player.getUUID(), new State("", 0, quiet));
			return;
		}
		Attunements.Rule rule = match.get();
		int checks = state != null && state.rule().equals(rule.id()) ? state.checks() + 1 : 1;
		STATES.put(player.getUUID(), new State(rule.id(), checks, state == null ? 0 : state.quietUntil()));
		int color = RuneColors.of(rule.rune());
		show(player, color, checks);
		int perStage = CHECKS / STAGES;
		if (checks == 1) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.attune_begin").withColor(color));
			Fx.sound(player.level(), player.position(), WildercordSounds.CIRCLE_OPEN, 0.8F, 0.8F);
		} else if (checks < CHECKS && checks % perStage == 0) {
			int stage = checks / perStage;
			player.sendOverlayMessage(Component.translatable("message.wildercord.attune_stage." + stage).withColor(color));
			Fx.sound(player.level(), player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.8F + stage * 0.25F);
			Sigils.flash(player.level(), hold(player), color, 0.6F + stage * 0.3F);
		}
		if (checks >= CHECKS) {
			complete(player, hand, rule, color);
			STATES.put(player.getUUID(), new State("", 0, now + 200));
		}
	}

	/** The hand holding a Blank Rune (the main hand first), or null. */
	private static InteractionHand blankHand(ServerPlayer player) {
		if (player.getMainHandItem().is(WildercordItems.BLANK_RUNE)) {
			return InteractionHand.MAIN_HAND;
		}
		if (player.getOffhandItem().is(WildercordItems.BLANK_RUNE)) {
			return InteractionHand.OFF_HAND;
		}
		return null;
	}

	/** Everything an attunement rule can ask about where the player sits. */
	public static Attunements.Place placeOf(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos feet = player.blockPosition();
		BlockPos head = BlockPos.containing(player.getEyePosition());
		String biome = level.getBiome(feet).unwrapKey().map(k -> k.identifier().toString()).orElse("");
		String dimension = level.dimension().identifier().toString();
		long time = Math.floorMod(level.getOverworldClockTime(), 24000L);
		int moon = level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, feet).index();
		Biome.Precipitation falling = level.precipitationAt(head);
		String precipitation = falling == Biome.Precipitation.RAIN ? "rain" : falling == Biome.Precipitation.SNOW ? "snow" : "none";
		boolean sculk = biome.equals("minecraft:deep_dark") && nearSculk(level, feet);
		return new Attunements.Place(biome, dimension, feet.getY(), time, moon, precipitation, level.canSeeSky(head), sculk);
	}

	/** Sculk within 4 blocks. */
	private static boolean nearSculk(ServerLevel level, BlockPos at) {
		for (BlockPos pos : BlockPos.betweenClosed(at.offset(-4, -3, -4), at.offset(4, 3, 4))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(Blocks.SCULK) || state.is(Blocks.SCULK_VEIN) || state.is(Blocks.SCULK_SENSOR) || state.is(Blocks.SCULK_SHRIEKER)
					|| state.is(Blocks.SCULK_CATALYST)) {
				return true;
			}
		}
		return false;
	}

	/** Where the blank is held: in front of the chest, below the eyes. */
	private static Vec3 hold(ServerPlayer player) {
		Vec3 look = player.getLookAngle();
		return player.position().add(look.x * 0.5, 1.0, look.z * 0.5);
	}

	/** The ritual as it goes: the circle under the caster, motes of the land rising into the blank, a growing glow. */
	private static void show(ServerPlayer player, int color, int checks) {
		ServerLevel level = player.level();
		Vec3 feet = player.position();
		if (checks % 6 == 1) {
			Sigils.ground(level, feet, color, 0xFFFFFF, 1.6F + 0.4F * checks / CHECKS, 34);
		}
		double spin = level.getGameTime() * 0.12;
		double progress = checks / (double) CHECKS;
		DustParticleOptions dust = new DustParticleOptions(color, 0.9F);
		Vec3 hand = hold(player);
		for (int i = 0; i < 3; i++) {
			double a = spin + Math.PI * 2 * i / 3;
			Vec3 from = feet.add(Math.cos(a) * 1.6, 0.1, Math.sin(a) * 1.6);
			Fx.send(level, dust, from.x, from.y, from.z, 1, 0, 0, 0, 0);
			Vec3 dir = hand.subtract(from);
			Fx.send(level, ParticleTypes.ENCHANT, from.x, from.y, from.z, 0, dir.x, dir.y, dir.z, 0.8);
		}
		if (checks % 2 == 0) {
			Fx.send(level, SigilOption.glow(color, (float) (0.25 + 0.5 * progress)), hand.x, hand.y, hand.z, 1, 0, 0, 0, 0);
		}
	}

	private static void complete(ServerPlayer player, InteractionHand hand, Attunements.Rule rule, int color) {
		ServerLevel level = player.level();
		ItemStack blank = player.getItemInHand(hand);
		blank.shrink(1);
		ItemStack rune = RuneItem.stack(rule.rune());
		if (blank.isEmpty()) {
			player.setItemInHand(hand, rune);
		} else if (!player.getInventory().add(rune)) {
			level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), rune));
		}
		Vec3 at = hold(player);
		Sigils.flash(level, at, color, 2.2F);
		Light.groundRing(level, player.position(), color, 0.3, 3.0, 0.1, 12);
		Vfx.radial(level, new DustParticleOptions(color, 1.2F), at, 20, 0.2);
		Fx.sound(level, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 1.2F);
		Fx.sound(level, at, WildercordSounds.DISCOVERY, 1.0F, 1.0F);
		player.sendOverlayMessage(Component.translatable("message.wildercord.attuned", RuneItem.runeName(rule.rune()).withColor(color)));
		Grimoire.unlock(player, rule.key());
	}

	private static void broken(ServerPlayer player) {
		player.sendOverlayMessage(Component.translatable("message.wildercord.attune_broken").withStyle(ChatFormatting.GRAY));
		Fx.sound(player.level(), player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5F, 0.5F);
	}

	/** How far along a player's attunement is (0 to 1), for the tests. */
	public static double progress(ServerPlayer player) {
		State state = STATES.get(player.getUUID());
		return state == null ? 0 : Math.min(1, state.checks() / (double) CHECKS);
	}

	public static void forget(UUID player) {
		STATES.remove(player);
	}
}
