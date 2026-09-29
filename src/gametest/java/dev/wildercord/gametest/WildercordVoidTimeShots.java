package dev.wildercord.gametest;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.TimeFx;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A screenshot of each void and time rune's presentation, cast for real at a husk on a stone platform high in the sky at noon,
 * one frame a few ticks after the cast (the moment its shape reads best). Look at {@code vt_*.png}; nothing is asserted but
 * that nothing throws. Skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and {@code WILDERCORD_SHOWCASE}.
 */
public class WildercordVoidTimeShots implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 200, 0);
	private static final String TAG = "wildercord.vt_shots";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			stage(world);
			context.waitTicks(20);
			// F1: no toasts, chat or hotbar in the frames.
			context.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.getKey(new net.minecraft.client.input.KeyEvent(
				com.mojang.blaze3d.platform.InputConstants.KEY_F1, 0, 0)));
			context.waitTicks(5);

			shot(context, world, "pull", 3, (s, p) -> cast(p, Runes.PULL, husk(p, 0, 6)));
			shot(context, world, "gravity_well", 14, (s, p) -> cast(p, Runes.GRAVITY_WELL, husk(p, 0, 6)));
			shot(context, world, "riftcall", 20, (s, p) -> cast(p, Runes.RIFTCALL, husk(p, 0, 6)));
			shot(context, world, "hollow", 3, (s, p) -> cast(p, Runes.HOLLOW, husk(p, 0, 6)));
			shot(context, world, "shadowstep", 3, (s, p) -> cast(p, Runes.SHADOWSTEP, husk(p, 0, 7)));
			shot(context, world, "grapple", 3, (s, p) -> cast(p, Runes.GRAPPLE, husk(p, 0, 12)));
			shot(context, world, "warp", 4, (s, p) -> cast(p, Runes.WARP, husk(p, 0, 6)));
			shot(context, world, "sonic_boom", 3, (s, p) -> cast(p, Runes.SONIC_BOOM, husk(p, 0, 6)));
			shot(context, world, "wither", 4, (s, p) -> cast(p, Runes.WITHER, husk(p, 0, 6)));
			shot(context, world, "hex", 5, (s, p) -> cast(p, Runes.HEX, husk(p, 0, 6)));
			shot(context, world, "umbra", 3, (s, p) -> cast(p, Runes.UMBRA, husk(p, 0, 6)));
			shot(context, world, "countdown", 4, (s, p) -> {
				cast(p, Runes.COUNTDOWN, husk(p, -2, 6), husk(p, 0, 6), husk(p, 2, 6));
			});
			shot(context, world, "stasis", 5, (s, p) -> cast(p, Runes.STASIS, husk(p, 0, 7)));
			shot(context, world, "doomclock", 8, (s, p) -> cast(p, Runes.DOOMCLOCK, husk(p, 0, 7)));
			shot(context, world, "prolong", 3, (s, p) -> {
				Mob h = husk(p, 0, 6);
				h.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
				// A helpful rune cannot land on an enemy: the husk puts it on itself.
				Effects.apply(new Cast(h), node(Runes.PROLONG), new Cast.Hit(List.of(h), h.position(), h.getLookAngle(), h.position(), null, null, true));
			});
			shot(context, world, "foresight", 3, (s, p) -> {
				Mob h = husk(p, 0, 6);
				Effects.apply(new Cast(h), node(Runes.FORESIGHT), new Cast.Hit(List.of(h), h.position(), h.getLookAngle(), h.position(), null, null, true));
			});
			shot(context, world, "anchor", 2, (s, p) -> {
				Mob h = husk(p, 0, 6);
				Effects.apply(new Cast(h), node(Runes.ANCHOR), new Cast.Hit(List.of(h), h.position(), h.getLookAngle(), h.position(), null, null, true));
				dev.wildercord.cast.VoidFx.clank((ServerLevel) h.level(), h);
			});
			shot(context, world, "rewind", 8, (s, p) -> TimeFx.rewindPath((ServerLevel) p.level(), p.position().add(0, 0, 10), p.position().add(2, 0, 3)));
			shot(context, world, "time_skip", 3, (s, p) -> TimeFx.skip((ServerLevel) p.level(), p.position().add(0, 0, 6), p.position().add(0, 0, 12)));
			shot(context, world, "eclipse", 14, (s, p) -> cast(p, Runes.ECLIPSE, husk(p, 0, 6)));
			shot(context, world, "dragon_breath", 10, (s, p) -> cast(p, Runes.DRAGON_BREATH, husk(p, 0, 7)));
		}
	}

	private static void shot(ClientGameTestContext context, TestSingleplayerContext world, String name, int ticks, Consumer2 action) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			stand(player);
			action.accept(server, player);
		});
		context.waitTicks(ticks);
		context.takeScreenshot("vt_" + name);
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runCommand("kill @e[type=wolf]");
		world.getServer().runOnServer(server -> stand(server.getPlayerList().getPlayers().getFirst()));
		// Long enough for the longest-lived drawing (a Doomclock face, an Eclipse disc, a Stasis column) to be gone from the next frame.
		context.waitTicks(120);
	}

	@FunctionalInterface
	private interface Consumer2 {
		void accept(MinecraftServer server, ServerPlayer player);
	}

	private static void stage(TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 16) + " " + (y - 1) + " " + (z - 10) + " " + (x + 16) + " " + (y - 1) + " " + (z + 30) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 16) + " " + y + " " + (z - 10) + " " + (x + 16) + " " + (y + 14) + " " + (z + 30) + " minecraft:air");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			player.setGameMode(GameType.CREATIVE);
			Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
			Spellbook book = Spellbooks.get(player).withStarterGiven();
			for (RuneDef rune : Runes.all()) {
				book = book.learn(rune.id());
			}
			Spellbooks.set(player, book);
			player.setAttached(WildercordAttachments.CRYSTALS, Mana.MAX_CRYSTALS);
			stand(player);
		});
	}

	private static void stand(ServerPlayer player) {
		player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.removeAllEffects();
	}

	private static Mob husk(ServerPlayer player, double side, double ahead) {
		ServerLevel level = player.level();
		Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		husk.snapTo(STAGE.getX() + 0.5 + side, STAGE.getY(), STAGE.getZ() + 0.5 + ahead, 180, 0);
		husk.setNoAi(true);
		husk.addTag(TAG);
		husk.addTag("wildercord.rolled");
		level.addFreshEntity(husk);
		husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
		husk.setHealth(100);
		return husk;
	}

	private static SpellPlan.EffectNode node(RuneDef rune) {
		return SpellCompiler.compile(List.of(Runes.TOUCH, rune)).root().groups.getFirst().effects.getFirst();
	}

	private static void cast(ServerPlayer player, RuneDef rune, Entity... targets) {
		Vec3 at = targets.length == 0 ? player.position() : targets[0].position();
		Effects.apply(new Cast(player), node(rune), new Cast.Hit(List.of(targets), at, player.getLookAngle(), player.position(), null, null, false));
	}
}
