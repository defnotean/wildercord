package dev.wildercord.gametest;

import dev.wildercord.pet.CinnamonContent;
import dev.wildercord.pet.CinnamonDog;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Focused in-world suite for Cinnamon: configuration, direct summon, ownership, sitting, immortality, rendering, her bow,
 * her tongue and bell, and defending her owner.
 */
public final class WildercordCinnamonTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		Path config = FabricLoader.getInstance().getConfigDir().resolve("wildercord-cinnamon.json");
		String previous;
		try {
			previous = Files.exists(config) ? Files.readString(config, StandardCharsets.UTF_8) : null;
		} catch (Exception e) { throw new AssertionError("Cannot read Cinnamon config", e); }
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(35);
			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1600, 900);
				mc.options.setCameraType(CameraType.FIRST_PERSON);
			});
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("fill -7 98 -7 7 98 7 minecraft:stone");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.CREATIVE);
				player.teleportTo(player.level(), 0.5, 99, 0.5, Set.<Relative>of(), 0, 18, false);
				try {
					Files.createDirectories(config.getParent());
					Files.writeString(config, "{\"owner\":\"" + player.getGameProfile().name() + "\"}\n", StandardCharsets.UTF_8);
				} catch (Exception e) { throw new AssertionError("Cannot configure Cinnamon", e); }
			});
			context.waitTicks(120);
			int initial = world.getServer().computeOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				var dogs = player.level().getEntitiesOfClass(CinnamonDog.class, player.getBoundingBox().inflate(48));
				check(dogs.size() == 1, "configured owner should have one Cinnamon, found " + dogs.size());
				CinnamonDog dog = dogs.getFirst();
				check(dog.isTame() && dog.isOwnedBy(player), "Cinnamon must arrive pre-tamed to the configured player");
				check("Cinnamon".equals(dog.getCustomName().getString()), "Cinnamon must show her name");
				check(!dog.hurtServer(player.level(), player.damageSources().generic(), 1000) && dog.isAlive(), "damage must not kill Cinnamon");
				check(dog.mobInteract(player, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS && dog.isOrderedToSit(), "owner click should make her sit");
				check(dog.mobInteract(player, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS && !dog.isOrderedToSit(), "second click should make her follow");
				dog.setNoAi(true);
				dog.teleportTo(0.5, 99, 3.5);
				dog.setYRot(180); dog.setYBodyRot(180); dog.setYHeadRot(180);
				dog.setCustomNameVisible(false);
				return dog.getId();
			});
			context.waitTicks(10);
			context.takeScreenshot(TestScreenshotOptions.of("cinnamon_front").disableCounterPrefix());
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				CinnamonDog dog = player.level().getEntity(initial) instanceof CinnamonDog found ? found : null;
				if (dog != null) dog.setCustomNameVisible(true);
				try { Files.writeString(config, "{\"owner\":\"@singleplayer\"}\n", StandardCharsets.UTF_8); }
				catch (Exception e) { throw new AssertionError("Cannot switch Cinnamon to singleplayer binding", e); }
			});
			context.waitTicks(110);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				CinnamonDog dog = player.level().getEntitiesOfClass(CinnamonDog.class, player.getBoundingBox().inflate(48)).getFirst();
				dog.mobInteract(player, InteractionHand.MAIN_HAND);
				check(dog.isOrderedToSit(), "prepare persistent sitting preference");
			});
			world.getServer().runCommand("summon wildercord:cinnamon 2 99 2");
			context.waitTicks(50);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				var dogs = player.level().getEntitiesOfClass(CinnamonDog.class, player.getBoundingBox().inflate(48));
				check(dogs.size() == 1, "direct summon should replace the old body, found " + dogs.size());
				CinnamonDog dog = dogs.getFirst();
				check(dog.getId() != initial && dog.isTame() && dog.isOwnedBy(player), "/summon must produce an owned, tame Cinnamon");
				check(dog.isOrderedToSit(), "direct summon must restore the owner's sitting choice");
				check(dog.mobInteract(player, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS && !dog.isOrderedToSit(), "directly summoned Cinnamon must follow");
				check(dog.mobInteract(player, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS && dog.isOrderedToSit(), "directly summoned Cinnamon must sit");
				dog.setNoAi(true);
				dog.teleportTo(0.5, 99, 3.5);
				dog.setYRot(180); dog.setYBodyRot(180); dog.setYHeadRot(180);
				dog.setCustomNameVisible(false);
			});
			context.takeScreenshot(TestScreenshotOptions.of("cinnamon_summoned").disableCounterPrefix());
			context.waitTicks(225);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				CinnamonDog dog = player.level().getEntitiesOfClass(CinnamonDog.class, player.getBoundingBox().inflate(48)).getFirst();
				check((dog.mood() & 1) != 0, "sitting quietly should let Cinnamon curl up");
			});
			context.takeScreenshot(TestScreenshotOptions.of("cinnamon_resting").disableCounterPrefix());
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				CinnamonDog dog = player.level().getEntitiesOfClass(CinnamonDog.class, player.getBoundingBox().inflate(48)).getFirst();
				player.setItemInHand(InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(CinnamonContent.TOY));
				dog.mobInteract(player, InteractionHand.MAIN_HAND);
				check(dog.isOrderedToSit(), "playing with her toy must preserve her sitting preference");
				player.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
			});
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				CinnamonDog dog = player.level().getEntitiesOfClass(CinnamonDog.class, player.getBoundingBox().inflate(48)).getFirst();
				check((dog.mood() & 2) != 0 && (dog.mood() & 1) == 0, "her favourite toy must wake her and start a playful wag");
			});
			context.takeScreenshot(TestScreenshotOptions.of("cinnamon_toy_play").disableCounterPrefix());

			// Her bow: put on with the item, kept when her body is replaced, and untied with shears.
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				CinnamonDog dog = player.level().getEntitiesOfClass(CinnamonDog.class, player.getBoundingBox().inflate(48)).getFirst();
				player.setItemInHand(InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(CinnamonContent.BOW));
				check(dog.mobInteract(player, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS, "her bow should go on");
				player.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
				check(player.getAttachedOrElse(dev.wildercord.pet.CinnamonState.BOW, false), "her owner should remember the bow");
				dog.showOff(200);
				dog.setCustomNameVisible(false);
			});
			context.waitTicks(3);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				CinnamonDog dog = player.level().getEntitiesOfClass(CinnamonDog.class, player.getBoundingBox().inflate(48)).getFirst();
				int mood = dog.mood();
				check((mood & CinnamonDog.BOW) != 0, "she should be wearing her bow (mood " + mood + ")");
				check((mood & CinnamonDog.TONGUE) != 0 && (mood & CinnamonDog.RINGING) != 0, "her tongue and bell should show (mood " + mood + ")");
			});
			context.waitTicks(60);
			context.takeScreenshot(TestScreenshotOptions.of("cinnamon_bow_tongue").disableCounterPrefix());
			world.getServer().runCommand("summon wildercord:cinnamon 1 99 2");
			context.waitTicks(50);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				var dogs = player.level().getEntitiesOfClass(CinnamonDog.class, player.getBoundingBox().inflate(48));
				check(dogs.size() == 1 && (dogs.getFirst().mood() & CinnamonDog.BOW) != 0, "a new body should still wear her bow");
				CinnamonDog dog = dogs.getFirst();
				player.getInventory().clearContent();
				player.setItemInHand(InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHEARS));
				check(dog.mobInteract(player, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS, "shears should untie her bow");
				check(!player.getAttachedOrElse(dev.wildercord.pet.CinnamonState.BOW, true), "untying should be remembered");
				check(player.getInventory().countItem(CinnamonContent.BOW) == 1, "her bow should come back to her owner");
				player.getInventory().clearContent();
			});
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				CinnamonDog dog = player.level().getEntitiesOfClass(CinnamonDog.class, player.getBoundingBox().inflate(48)).getFirst();
				check((dog.mood() & CinnamonDog.BOW) == 0, "the bow should be off");
				dog.setNoAi(true);
				dog.teleportTo(0.5, 99, 3.5);
				dog.setYRot(180); dog.setYBodyRot(180); dog.setYHeadRot(180);
			});
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.level().getEntitiesOfClass(CinnamonDog.class, player.getBoundingBox().inflate(48)).getFirst()
					.teleportTo(player.getX(), player.level().getMinY() - 10, player.getZ());
			});
			context.waitTicks(45);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				var dogs = player.level().getEntitiesOfClass(CinnamonDog.class, player.getBoundingBox().inflate(48));
				check(dogs.size() == 1 && dogs.getFirst().isAlive(), "Cinnamon must be rescued from the void");
				check(dogs.getFirst().isOrderedToSit(), "rescue must preserve sitting");
			});

			// Anyone who hurts her owner gets bitten: a husk (it won't burn at noon) strikes the owner, and she goes for it.
			world.getServer().runCommand("difficulty normal");
			int husk = world.getServer().computeOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.SURVIVAL);
				player.setHealth(player.getMaxHealth());
				CinnamonDog dog = player.level().getEntitiesOfClass(CinnamonDog.class, player.getBoundingBox().inflate(48)).getFirst();
				dog.setNoAi(false);
				dog.setOrderedToSit(false);
				var attacker = net.minecraft.world.entity.EntityTypes.HUSK.create(player.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				attacker.snapTo(player.getX() + 3, player.getY(), player.getZ() + 1, 0, 0);
				attacker.setNoAi(true);
				player.level().addFreshEntity(attacker);
				player.hurtServer(player.level(), player.damageSources().mobAttack(attacker), 1);
				return attacker.getId();
			});
			context.waitTicks(80);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				CinnamonDog dog = player.level().getEntitiesOfClass(CinnamonDog.class, player.getBoundingBox().inflate(48)).getFirst();
				var attacker = player.level().getEntity(husk) instanceof net.minecraft.world.entity.LivingEntity living ? living : null;
				check(attacker == null || !attacker.isAlive() || attacker.getHealth() < attacker.getMaxHealth(),
					"Cinnamon should bite whoever hurt her owner (target " + dog.getTarget() + ", husk health "
						+ (attacker == null ? "gone" : attacker.getHealth()) + ")");
				if (attacker != null) attacker.discard();
				player.setGameMode(GameType.CREATIVE);
			});
		} finally {
			try {
				if (previous == null) Files.deleteIfExists(config);
				else Files.writeString(config, previous, StandardCharsets.UTF_8);
			} catch (Exception e) { throw new AssertionError("Cannot restore Cinnamon config", e); }
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
