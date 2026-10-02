package dev.wildercord.gametest;

import dev.wildercord.cast.SpellCaster;
import dev.wildercord.client.compat.ShaderCompat;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.familiar.Wisp;
import dev.wildercord.familiar.WispSpawner;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.Iris;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

/**
 * Magic under a shader pack: spells whose light glows and whose void darkens, a Shield's circles, a
 * wisp, a Cord just put on, and aura's feel (a Sovereign body aura, an art's trail, impact and banner, an awakened body, the
 * crossroads' standards), each photographed with a
 * pack drawing the world. Only runs with Iris
 * installed ({@code ./gradlew runClientGameTest -Pshaders}).
 *
 * <p>The pack (in this test's resources, {@code shaderpack/}) is a tiny one that works like the big
 * deferred packs in the way that matters here: every surface writes its colour to {@code colortex0} and
 * its light levels to {@code colortex1}, and a composite pass re-lights the scene from {@code colortex1}.
 * Anything drawn with a blend the pack doesn't expect shows up as a wrongly lit patch.</p>
 */
public class WildercordShaderTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!FabricLoader.getInstance().isModLoaded("iris")) {
			dev.wildercord.Wildercord.LOGGER.info("[TEST SKIP] WildercordShaderTest: Iris is not installed in this test profile");
			return;
		}
		context.runOnClient(mc -> TestPack.switchOn(true));
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			check(context.computeOnClient(mc -> ShaderCompat.active()), "the test shader pack should be in use");
			world.getServer().runCommand("time set 18000");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				Spellbook book = Spellbooks.get(player).withStarterGiven();
				for (RuneDef rune : Runes.all()) {
					book = book.learn(rune.id());
				}
				book = book.withSpell(0, ids(Runes.NOVA, Runes.FIRE)).withSpell(1, ids(Runes.BOLT, Runes.HOLLOW))
					.withSpell(2, ids(Runes.SELF, Runes.SHIELD)).withSpell(3, ids(Runes.BURST, Runes.LIGHTNING));
				Spellbooks.set(player, book);
			});
			context.runOnClient(mc -> hideHud(mc, true));
			scenes(context, world, "shader");
			// The same without the pack, to compare.
			context.runOnClient(mc -> TestPack.switchOn(false));
			context.waitTicks(10);
			check(!context.computeOnClient(mc -> ShaderCompat.active()), "the shader pack should be off again");
			scenes(context, world, "plain");
		} finally {
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				hideHud(mc, false);
				TestPack.switchOn(false);
			});
		}
	}

	/** Each spell cast and photographed from behind, then a wisp beside you, then a Cord just put on, from the front. */
	private static void scenes(ClientGameTestContext context, TestSingleplayerContext world, String prefix) {
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		world.getServer().runOnServer(server -> Spellbooks.setCord(player(server), new ItemStack(WildercordItems.ECHO_CORD)));
		context.waitTicks(20);
		String[] names = {"nova_fire", "bolt_hollow", "self_shield", "burst_lightning"};
		for (int spell = 0; spell < 4; spell++) {
			int s = spell;
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				Spellbooks.setMana(player, 300);
				Spellbooks.setReadyAt(player, s, 0);
				SpellCaster.cast(player, s);
			});
			context.waitTicks(8);
			shot(context, prefix + "_" + names[spell]);
			context.waitTicks(30);
		}
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.level().getEntitiesOfClass(Wisp.class, player.getBoundingBox().inflate(32)).forEach(Wisp::discard);
			// Ahead and to the right, so it isn't hidden behind you.
			Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
			Vec3 at = player.position().add(look.scale(3)).add(-look.z * 2, 1.4, look.x * 2);
			check(WispSpawner.spawn(player.level(), BlockPos.containing(at), "frost") != null, "a wisp should spawn");
		});
		context.waitTicks(30);
		shot(context, prefix + "_wisp");
		world.getServer().runOnServer(server -> Spellbooks.setCord(player(server), ItemStack.EMPTY));
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(20);
		world.getServer().runOnServer(server -> Spellbooks.setCord(player(server), new ItemStack(WildercordItems.AMETHYST_CORD)));
		context.waitTicks(20);
		shot(context, prefix + "_cord");
		aura(context, world, prefix);
	}

	/**
	 * Aura's feel under the pack: a Sovereign swordsman's body aura flaring (haze, mantle, corona, burning eyes) from the front, then
	 * from behind an art's trail, its impact and its banner. All of it light, drawn the plain way under a pack.
	 */
	private static void aura(ClientGameTestContext context, TestSingleplayerContext world, String prefix) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setAttached(dev.wildercord.aura.AuraAttachments.AURA, new dev.wildercord.aura.AuraAttachments.Data("ember",
				dev.wildercord.aura.AuraRules.SOVEREIGN, dev.wildercord.aura.AuraRules.threshold(dev.wildercord.aura.AuraRules.SOVEREIGN),
				dev.wildercord.aura.AuraRules.capacity(dev.wildercord.aura.AuraRules.SOVEREIGN), 0));
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
			player.setAttached(dev.wildercord.aura.AuraPresence.LOOK, dev.wildercord.aura.AuraPresence.look(player)
				.fightUntil(player.level().getGameTime() + 600));
		});
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(30);
		shot(context, prefix + "_aura_body");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			net.minecraft.world.entity.monster.zombie.Husk husk = net.minecraft.world.entity.EntityTypes.HUSK.create(player.level(),
				net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			if (husk != null) {
				Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
				husk.snapTo(player.getX() + look.x * 2.4, player.getY(), player.getZ() + look.z * 2.4, 0, 0);
				husk.setNoAi(true);
				husk.addTag("wildercord.shader_aura");
				player.level().addFreshEntity(husk);
			}
		});
		context.waitTicks(10);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.removeAttached(dev.wildercord.aura.SwordStrings.COOLDOWNS);
			dev.wildercord.api.AuraApi.StringArt art = dev.wildercord.api.AuraApi.string(dev.wildercord.aura.arts.EmberArts.KINDLING_DRAW).orElseThrow();
			dev.wildercord.aura.SwordStrings.perform(player, art, art.string().tokens().stream().map(t -> dev.wildercord.aura.SwordString.Token.marks(t)).toList());
		});
		context.waitTicks(3);
		shot(context, prefix + "_aura_art");
		// Its line of fire burning on ahead: light over the ground, under the pack too.
		context.waitTicks(8);
		shot(context, prefix + "_aura_art_line");
		context.waitTicks(30);
		// Awakened (step 6): the awakened form (taller corona, streamers rising, burning eyes) from the front, under the pack too (the
		// art's husk out of the way first, without a death's puff: it stood between the swordsman and the camera).
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.level().getEntitiesOfClass(net.minecraft.world.entity.Entity.class, player.getBoundingBox().inflate(16),
				e -> e.entityTags().contains("wildercord.shader_aura")).forEach(net.minecraft.world.entity.Entity::discard);
		});
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setAttached(dev.wildercord.aura.AuraAttachments.AURA, dev.wildercord.aura.Aura.data(player)
				.withAura(dev.wildercord.aura.AuraRules.capacity(dev.wildercord.aura.AuraRules.SOVEREIGN)));
			player.setAttached(dev.wildercord.aura.Momentum.MOMENTUM, new dev.wildercord.aura.Momentum.State(60, player.level().getGameTime() + 100000, 0, 0, 0));
			check(dev.wildercord.api.AuraApi.awaken(player), "the swordsman should awaken (" + dev.wildercord.aura.Awakening.refusal(player) + ")");
		});
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(40);
		shot(context, prefix + "_aura_awakened");
		world.getServer().runOnServer(server -> dev.wildercord.api.AuraApi.endAwakening(player(server)));
		context.waitTicks(3);
		world.getServer().runCommand("kill @e[tag=wildercord.shader_aura]");
		// The crossroads (step 7): its standards of light from behind, under the pack too; closed again after (its state taken away).
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			dev.wildercord.aura.Ways.set(player, "");
			check(dev.wildercord.api.AuraApi.openCrossroads(player), "the crossroads should rise round a swordsman with no Way");
		});
		context.waitTicks(30);
		shot(context, prefix + "_aura_crossroads");
		// Non-emissive floor damage must keep its normal alpha blending under a deferred pack.
		context.runOnClient(mc -> dev.wildercord.client.fx.AuraGroundScar.clear());
		world.getServer().runCommand("time set 3000");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			dev.wildercord.aura.AuraFx.groundScar(player.level(), player.position(), 3.0, 100, 1);
		});
		context.waitTicks(4);
		check(context.computeOnClient(mc -> dev.wildercord.client.fx.AuraGroundScar.showing() > 0), "physical floor scars should render under the pack");
		shot(context, prefix + "_aura_ground_scar");
		world.getServer().runCommand("time set 18000");
		world.getServer().runOnServer(server -> player(server).removeAttached(dev.wildercord.aura.Crossroads.CROSSROADS));
		context.waitTicks(12);
		// The bonded blade (step 9) under the pack: a Soulforged blade's glow in hand from the front, then lying on the ground.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
			check(dev.wildercord.aura.BondedBlades.bond(player, net.minecraft.world.InteractionHand.MAIN_HAND, "test"),
				"the blade should bond (" + dev.wildercord.aura.BondedBlades.bondRefusal(player) + ")");
			dev.wildercord.aura.BondedBlades.addHistory(player, java.util.Map.of(dev.wildercord.aura.BladeRules.BOSSES, 1), java.util.Map.of());
			dev.wildercord.aura.BondedBlades.setResonance(player, dev.wildercord.aura.BladeRules.threshold(dev.wildercord.aura.BladeRules.SOULFORGED));
		});
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(40);
		shot(context, prefix + "_aura_bonded_blade");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ItemStack blade = player.getMainHandItem().copy();
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			net.minecraft.world.entity.item.ItemEntity e = new net.minecraft.world.entity.item.ItemEntity(player.level(), player.getX(),
				player.getY() + 0.2, player.getZ() + 2.0, blade, 0, 0, 0);
			player.level().addFreshEntity(e);
		});
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		context.waitTicks(20);
		shot(context, prefix + "_aura_bonded_ground");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			dev.wildercord.aura.BondedBlades.release(player);
			player.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, player.getBoundingBox().inflate(8),
				e -> e.getItem().is(net.minecraft.world.item.Items.DIAMOND_SWORD)).forEach(net.minecraft.world.entity.Entity::discard);
		});
		context.waitTicks(5);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.removeAttached(dev.wildercord.aura.Awakening.AWAKENING);
			player.removeAttached(dev.wildercord.aura.Momentum.MOMENTUM);
			player.removeAllEffects();
			player.removeAttached(dev.wildercord.aura.AuraAttachments.AURA);
		});
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		context.waitTicks(5);
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> mc.gui.toastManager().clear());
		context.waitTicks(1);
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static void hideHud(net.minecraft.client.Minecraft mc, boolean hide) {
		if (mc.gui.hud.isHidden() != hide) {
			mc.gui.hud.toggle();
		}
	}

	/** The test pack, put in Iris's shader pack folder and switched on (or off again). Only touched with Iris installed. */
	private static final class TestPack {
		private static final String NAME = "WildercordTest";
		private static final String[] FILES = {"gbuffers_basic.vsh", "gbuffers_basic.fsh", "gbuffers_textured.vsh", "gbuffers_textured.fsh",
			"gbuffers_textured_lit.vsh", "gbuffers_textured_lit.fsh", "composite.vsh", "composite.fsh", "final.vsh", "final.fsh"};

		static void switchOn(boolean on) {
			try {
				if (on) {
					Path shaders = Iris.getShaderpacksDirectory().resolve(NAME).resolve("shaders");
					Files.createDirectories(shaders);
					for (String file : FILES) {
						try (InputStream in = WildercordShaderTest.class.getResourceAsStream("/shaderpack/shaders/" + file)) {
							check(in != null, "the test pack is missing " + file);
							Files.copy(in, shaders.resolve(file), StandardCopyOption.REPLACE_EXISTING);
						}
					}
					Iris.getIrisConfig().setShaderPackName(NAME);
				}
				Iris.getIrisConfig().setShadersEnabled(on);
				Iris.getIrisConfig().save();
				Iris.reload();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void check(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError(what);
		}
	}

	private static List<String> ids(RuneDef... runes) {
		return Arrays.stream(runes).map(RuneDef::id).toList();
	}
}
