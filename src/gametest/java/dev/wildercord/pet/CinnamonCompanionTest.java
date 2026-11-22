package dev.wildercord.pet;

import com.mojang.authlib.GameProfile;
import dev.wildercord.party.PartyTestHooks;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * End-to-end native verification of Cinnamon's companion upgrade: party low-HP lick healing,
 * universal passive magic, permanent Grand Bow, 4-rider flying harness with 54-slot dual saddlebags,
 * owner flight + dismount Slow Falling, 2-3s hit pause balance, and full world-reopen persistence.
 */
public final class CinnamonCompanionTest implements FabricClientGameTest {
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("wildercord-cinnamon.json");

	private static void check(boolean cond, String why) {
		if (!cond) throw new AssertionError(why);
	}

	private static ServerPlayer owner(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static CinnamonDog findCinnamon(MinecraftServer server) {
		List<CinnamonDog> found = server.overworld().getEntitiesOfClass(
			CinnamonDog.class,
			new AABB(-128, -64, -128, 128, 256, 128));
		check(!found.isEmpty(), "Cinnamon should be present in the overworld");
		return found.getFirst();
	}

	private static final class PartyMate extends FakePlayer {
		PartyMate(ServerLevel level, String name) {
			super(level, new GameProfile(UUID.randomUUID(), name));
		}

		@Override
		public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
			return false;
		}
	}

	private static void aimAtCinnamon(MinecraftServer server, ServerPlayer player, CinnamonDog dog, double px, double py, double pz) {
		player.teleportTo(server.overworld(), px, py, pz, Set.<Relative>of(), 0, 0, false);
		player.setDeltaMovement(0, 0, 0);
		double dx = dog.getX() - player.getX();
		double dy = (dog.getY() + dog.getBbHeight() * 0.5D) - player.getEyeY();
		double dz = dog.getZ() - player.getZ();
		double horiz = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
		float pitch = (float) (Math.toDegrees(-Math.atan2(dy, horiz)));
		player.teleportTo(server.overworld(), px, py, pz, Set.<Relative>of(), yaw, pitch, false);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		String priorConfig = null;
		boolean hadConfig = Files.exists(CONFIG_PATH);
		try {
			if (hadConfig) priorConfig = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
			Files.createDirectories(CONFIG_PATH.getParent());
			Files.writeString(CONFIG_PATH, "{\"owner\":\"@singleplayer\",\"bell\":true}\n", StandardCharsets.UTF_8);

			var world = context.worldBuilder().create();
			var save = world.getWorldSave();
			try (world) {
				context.waitTicks(35);
				world.getServer().runCommand("gamerule spawn_mobs false");
				world.getServer().runCommand("time set 6000");
				world.getServer().runCommand("weather clear");

				world.getServer().runOnServer(server -> {
					ServerLevel level = server.overworld();
					for (int x = -14; x <= 14; x++) {
						for (int z = -10; z <= 20; z++) {
							level.setBlock(new BlockPos(x, 100, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
							for (int y = 101; y <= 130; y++) {
								level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
							}
						}
					}
					ServerPlayer p = owner(server);
					p.setGameMode(GameType.SURVIVAL);
					p.teleportTo(level, 0.5, 101, 0.5, Set.<Relative>of(), 0, 0, false);
				});
				context.waitTicks(20);

				// Wait until Cinnamon is spawned and bound to owner.
				for (int i = 0; i < 40; i++) {
					boolean ready = world.getServer().computeOnServer(server ->
						!server.overworld().getEntitiesOfClass(CinnamonDog.class, new AABB(-64, 80, -64, 64, 140, 64)).isEmpty());
					if (ready) break;
					context.waitTicks(5);
				}

				// 1. Party low-HP lick healing (party mate healed + Regeneration; stranger untouched).
				PartyMate[] mates = new PartyMate[2];
				world.getServer().runOnServer(server -> {
					ServerLevel level = server.overworld();
					ServerPlayer p = owner(server);
					CinnamonDog dog = findCinnamon(server);
					dog.setOrderedToSit(false);
					dog.teleportTo(0.5, 101, 2.5);
					PartyMate ally = new PartyMate(level, "CinnamonMate1");
					ally.setGameMode(GameType.SURVIVAL);
					ally.snapTo(1.8, 101, 2.5, 0, 0);
					level.addNewPlayer(ally);
					ally.setHealth(6.0F);
					PartyMate stranger = new PartyMate(level, "CinnamonStranger");
					stranger.setGameMode(GameType.SURVIVAL);
					stranger.snapTo(-2.0, 101, 2.5, 0, 0);
					level.addNewPlayer(stranger);
					stranger.setHealth(6.0F);
					long now = level.getGameTime();
					PartyTestHooks.join(server, p.getUUID(), ally.getUUID(), now);
					mates[0] = ally;
					mates[1] = stranger;
				});
				for (int i = 0; i < 20; i++) {
					boolean healed = world.getServer().computeOnServer(server ->
						mates[0].getHealth() > 6.0F && mates[0].hasEffect(MobEffects.REGENERATION));
					if (healed) break;
					context.waitTicks(3);
				}
				world.getServer().runOnServer(server -> {
					check(mates[0].getHealth() > 6.0F, "Cinnamon licks & heals injured same-party teammate");
					check(mates[0].hasEffect(MobEffects.REGENERATION), "Party lick grants Regeneration");
					check(mates[1].getHealth() == 6.0F && !mates[1].hasEffect(MobEffects.REGENERATION),
						"Stranger outside party is not lick-healed");
					mates[0].discard();
					mates[1].discard();
				});

				// 2. Universal passive magic at normal size (against a hostile Husk that won't burn in sunlight).
				Husk[] passiveTarget = new Husk[1];
				world.getServer().runOnServer(server -> {
					ServerLevel level = server.overworld();
					CinnamonDog dog = findCinnamon(server);
					dog.teleportTo(0.5, 101, 2.5);
					Husk z = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
					check(z != null, "Spawn passive magic husk target");
					z.setPos(0.5, 101, 8.5);
					z.setNoAi(true);
					level.addFreshEntity(z);
					passiveTarget[0] = z;
				});
				for (int i = 0; i < 45; i++) {
					boolean hit = world.getServer().computeOnServer(server ->
						!passiveTarget[0].isAlive() || passiveTarget[0].getHealth() < passiveTarget[0].getMaxHealth());
					if (hit) break;
					context.waitTicks(4);
				}
				world.getServer().runOnServer(server -> {
					check(!passiveTarget[0].isAlive() || passiveTarget[0].getHealth() < passiveTarget[0].getMaxHealth(),
						"Normal-size Cinnamon passively casts offensive magic at nearby hostile mobs");
					if (passiveTarget[0].isAlive()) passiveTarget[0].discard();
				});

				// 3. Permanent Grand Bow: equip via real use key, verify stats + permanence after resetGrowth().
				world.getServer().runOnServer(server -> {
					ServerPlayer p = owner(server);
					CinnamonDog dog = findCinnamon(server);
					dog.setOrderedToSit(false);
					dog.teleportTo(0.5, 101, 3.0);
					dog.setYRot(180);
					p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CinnamonContent.GRAND_BOW));
					aimAtCinnamon(server, p, dog, 0.5, 101, 0.8);
				});
				context.waitTicks(3);
				context.getInput().pressKey(o -> o.keyUse);
				context.waitTicks(6);
				world.getServer().runOnServer(server -> {
					CinnamonDog dog = findCinnamon(server);
					check(dog.wearingGrandBow(), "Grand Bow equips cleanly");
					check(dog.getScale() >= 2.99F, "Grand Bow makes Cinnamon 3x big (" + dog.getScale() + ")");
					check(dog.getMaxHealth() >= 80.0F, "Grand Bow boosts max health (" + dog.getMaxHealth() + ")");
					check(dog.getAttributeValue(Attributes.ATTACK_DAMAGE) >= 15.0D, "Grand Bow boosts attack damage");
					check(dog.getAttributeValue(Attributes.ARMOR) >= 10.0D, "Grand Bow boosts armor");
					check(dog.getAttributeValue(Attributes.MOVEMENT_SPEED) >= 0.37D, "Grand Bow boosts speed");
					check(dog.damageBudget() >= 60.0F, "Grand Bow boosts exhaustion damage budget");
					dog.resetGrowth();
					check(dog.getScale() >= 2.99F, "Grand Bow size remains permanent after resetGrowth()");
				});

				// 4. Flying Harness + 4-rider reservation + owner control + 54-slot dual chests.
				world.getServer().runOnServer(server -> {
					ServerPlayer p = owner(server);
					CinnamonDog dog = findCinnamon(server);
					p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CinnamonContent.HARNESS));
					aimAtCinnamon(server, p, dog, 0.5, 101, 0.5);
				});
				context.waitTicks(3);
				context.getInput().pressKey(o -> o.keyUse);
				context.waitTicks(6);

				PartyMate[] partyRiders = new PartyMate[4];
				PartyMate[] nonPartyRider = new PartyMate[1];
				world.getServer().runOnServer(server -> {
					ServerLevel level = server.overworld();
					ServerPlayer p = owner(server);
					CinnamonDog dog = findCinnamon(server);
					check(dog.wearingHarness(), "Flying Harness equips cleanly");

					long now = level.getGameTime();
					for (int i = 0; i < 4; i++) {
						PartyMate mate = new PartyMate(level, "Rider" + i);
						mate.setGameMode(GameType.SURVIVAL);
						mate.snapTo(1.5 + i, 101, 3.0, 0, 0);
						level.addNewPlayer(mate);
						PartyTestHooks.join(server, p.getUUID(), mate.getUUID(), now);
						partyRiders[i] = mate;
					}
					nonPartyRider[0] = new PartyMate(level, "Outsider");
					nonPartyRider[0].setGameMode(GameType.SURVIVAL);
					nonPartyRider[0].snapTo(-2.5, 101, 3.0, 0, 0);
					level.addNewPlayer(nonPartyRider[0]);

					check(!nonPartyRider[0].startRiding(dog), "Non-party outsider cannot ride Cinnamon");
					check(partyRiders[0].startRiding(dog), "Party mate 1 can mount");
					check(partyRiders[1].startRiding(dog), "Party mate 2 can mount");
					check(partyRiders[2].startRiding(dog), "Party mate 3 can mount");
					check(dog.getControllingPassenger() == null, "Non-owner party riders cannot steer Cinnamon");
					check(!partyRiders[3].startRiding(dog), "4th non-owner is rejected so owner always has a seat");

					// Dismount teammates cleanly for close-up screenshot after verifying owner takes the 4th seat.
					p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
					aimAtCinnamon(server, p, dog, 0.5, 101, 0.5);
				});
				context.waitTicks(3);
				context.getInput().pressKey(o -> o.keyUse);
				context.waitTicks(5);
				world.getServer().runOnServer(server -> {
					ServerPlayer p = owner(server);
					CinnamonDog dog = findCinnamon(server);
					check(p.getVehicle() == dog, "Owner mounts Cinnamon as the 4th rider");
					check(dog.getPassengers().size() == 4, "Four riders seated together on Cinnamon");
					check(dog.getControllingPassenger() == p, "Owner takes exclusive flight/steering control");

					// Test 54-slot chest access & persistence contents.
					dog.openCustomInventoryScreen(p);
					check(p.containerMenu instanceof ChestMenu menu && menu.getRowCount() == 6 && menu.getContainer().getContainerSize() == 54,
						"Twin side chest opens 6-row (54-slot) double-chest menu");
					dog.saddlebags().setItem(0, new ItemStack(Items.EMERALD, 3));
					dog.saddlebags().setItem(53, new ItemStack(Items.DIAMOND, 5));
					p.closeContainer();
				});

				// 5. Flight takeoff & climb + airborne screenshot + dismount Slow Falling.
				context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
				context.getInput().holdKey(o -> o.keyJump);
				context.waitTicks(24);
				context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
				context.takeScreenshot(TestScreenshotOptions.of("cinnamon_flight_ride").disableCounterPrefix());
				context.getInput().releaseKey(o -> o.keyJump);
				context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));

				world.getServer().runOnServer(server -> {
					ServerPlayer p = owner(server);
					CinnamonDog dog = findCinnamon(server);
					check(dog.getY() > 103.5D && dog.isFlying(), "Owner can fly Cinnamon high into the sky (" + dog.getY() + ")");
					p.stopRiding();
					check(p.hasEffect(MobEffects.SLOW_FALLING), "Midair dismount grants Slow Falling to riders");
					for (PartyMate mate : partyRiders) {
						mate.stopRiding();
						mate.discard();
					}
					nonPartyRider[0].discard();
					PartyTestHooks.clear(server);
				});

				// Settle back on grass for crisp daylight 3/4 portrait showing Grand Bow + harness + twin side chests.
				context.waitTicks(30);
				world.getServer().runOnServer(server -> {
					ServerPlayer p = owner(server);
					CinnamonDog dog = findCinnamon(server);
					dog.getNavigation().stop();
					dog.setDeltaMovement(0, 0, 0);
					dog.teleportTo(0.5, 101, 2.5);
					dog.setYRot(-140.0F);
					dog.setYBodyRot(-140.0F);
					dog.setYHeadRot(-140.0F);
					dog.setOrderedToSit(false);
					dog.showOff(120);
					check(dog.accumulatedDamage() == 0.0F, "Cinnamon takes zero fall damage");
					aimAtCinnamon(server, p, dog, -3.2, 102.6, -1.8);
				});
				context.waitTicks(6);
				context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
				context.takeScreenshot(TestScreenshotOptions.of("cinnamon_grand_harness_chests").disableCounterPrefix());

				// 6. 2-3 second hit pause balance when attacked by an enemy.
				Husk[] attacker = new Husk[1];
				world.getServer().runOnServer(server -> {
					ServerLevel level = server.overworld();
					CinnamonDog dog = findCinnamon(server);
					Husk z = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
					check(z != null, "Spawn hit-pause attacker");
					z.setPos(2.0, 101, 2.5);
					z.setNoAi(true);
					level.addFreshEntity(z);
					attacker[0] = z;
					dog.hurtServer(level, level.damageSources().mobAttack(z), 4.0F);
					long pauseTicks = dog.hitPauseUntil() - server.overworld().getGameTime();
					check(dog.isHitPaused() && pauseTicks >= 40 && pauseTicks <= 60,
						"Non-ally hit pauses Cinnamon for 2-3 seconds (" + pauseTicks + " ticks)");
					check(!dog.isEffectiveAi(), "Hit-paused Cinnamon pauses attacks/AI");
				});
				context.waitTicks(65);
				world.getServer().runOnServer(server -> {
					CinnamonDog dog = findCinnamon(server);
					check(!dog.isHitPaused() && dog.isEffectiveAi(), "Cinnamon resumes after 2-3 second hit pause");
					attacker[0].discard();
				});
			}

			// 7. Reopen world save and verify Grand Bow, Harness, 3x Scale, and 54-slot dual chest items persist.
			try (var reopened = save.open()) {
				context.waitTicks(30);
				reopened.getServer().runOnServer(server -> {
					CinnamonDog dog = findCinnamon(server);
					check(dog.wearingGrandBow(), "Grand Bow persists across world reopen");
					check(dog.wearingHarness(), "Flying Harness persists across world reopen");
					check(dog.getScale() >= 2.99F, "Grand Bow 3x scale persists across world reopen (" + dog.getScale() + ")");
					ItemStack first = dog.saddlebags().getItem(0);
					ItemStack last = dog.saddlebags().getItem(53);
					check(first.is(Items.EMERALD) && first.getCount() == 3, "Left chest slot 0 item persists across reopen");
					check(last.is(Items.DIAMOND) && last.getCount() == 5, "Right chest slot 53 item persists across reopen");
				});
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (hadConfig && priorConfig != null) {
					Files.writeString(CONFIG_PATH, priorConfig, StandardCharsets.UTF_8);
				} else {
					Files.deleteIfExists(CONFIG_PATH);
				}
			} catch (IOException ignored) {
			}
		}
	}
}
