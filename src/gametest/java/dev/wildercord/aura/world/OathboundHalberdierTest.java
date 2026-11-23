package dev.wildercord.aura.world;

import dev.wildercord.compat.McEntities;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * End-to-end verification and 7-panel 1:1 visual studio suite for Knight #3: {@link OathboundHalberdier}.
 */
public final class OathboundHalberdierTest implements FabricClientGameTest {
	private static final Vec3 STAGE = new Vec3(0.5, 121.0, 0.5);

	private static void check(boolean cond, String why) {
		if (!cond) {
			throw new AssertionError(why);
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static ServerLevel level(MinecraftServer server) {
		return player(server).level();
	}

	private static float yaw(Vec3 from, Vec3 to) {
		Vec3 d = to.subtract(from);
		return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
	}

	private static float pitch(Vec3 from, Vec3 to) {
		Vec3 d = to.subtract(from);
		return (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
	}

	private static void director(ClientGameTestContext context, TestSingleplayerContext world, Vec3 eye, Vec3 target) {
		int camId = world.getServer().computeOnServer(server -> {
			ServerLevel l = level(server);
			l.getEntitiesOfClass(Display.TextDisplay.class, new AABB(STAGE, STAGE).inflate(64),
				e -> e.entityTags().contains("wildercord.halberdier_cam")).forEach(Entity::discard);
			Display.TextDisplay cam = EntityType.TEXT_DISPLAY.create(l, EntitySpawnReason.COMMAND);
			cam.snapTo(eye.x, eye.y, eye.z, yaw(eye, target), pitch(eye, target));
			cam.addTag("wildercord.halberdier_cam");
			l.addFreshEntity(cam);
			return cam.getId();
		});
		context.waitTicks(3);
		context.runOnClient(mc -> {
			Entity cam = mc.level.getEntity(camId);
			if (cam != null) {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.setCameraEntity(cam);
				if (!dev.wildercord.gametest.compat.GametestCompat.hudHidden(mc)) {
					dev.wildercord.gametest.compat.GametestCompat.toggleHud(mc);
				}
				dev.wildercord.gametest.compat.GametestCompat.clearToasts(mc);
				dev.wildercord.gametest.compat.GametestCompat.clearChat(mc);
			}
		});
	}

	private static void cut(ClientGameTestContext context) {
		context.runOnClient(mc -> {
			mc.setCameraEntity(mc.player);
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			if (dev.wildercord.gametest.compat.GametestCompat.hudHidden(mc)) {
				dev.wildercord.gametest.compat.GametestCompat.toggleHud(mc);
			}
		});
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static boolean await(ClientGameTestContext context, TestSingleplayerContext world, int ticks, Function<MinecraftServer, Boolean> cond) {
		for (int i = 0; i < ticks; i++) {
			if (world.getServer().computeOnServer(cond::apply)) {
				return true;
			}
			context.waitTicks(1);
		}
		return world.getServer().computeOnServer(cond::apply);
	}

	private static OathboundHalberdier spawnHalberdier(ServerLevel l, Vec3 pos, float yaw, boolean ai) {
		OathboundHalberdier h = AuraWorld.OATHBOUND_HALBERDIER.create(l, EntitySpawnReason.COMMAND);
		h.snapTo(pos.x, pos.y, pos.z, yaw, 0.0F);
		h.setYHeadRot(yaw);
		h.setYBodyRot(yaw);
		h.setPersistenceRequired();
		h.setNoAi(!ai);
		l.addFreshEntity(h);
		return h;
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(30);
			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1600, 900);
				mc.options.guiScale().set(2);
				mc.resizeGui();
			});

			var server = world.getServer();
			server.runCommand("time set 15000");
			server.runCommand("weather clear");
			server.runCommand("difficulty normal");
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule natural_health_regeneration false");

			server.runCommand("fill -10 120 -10 10 120 10 stone_bricks");
			server.runCommand("fill -10 121 -10 10 132 10 air");
			server.runCommand("fill -9 121 7 9 128 7 polished_deepslate");
			server.runCommand("fill -9 121 8 9 128 8 deepslate_bricks");
			server.runCommand("setblock -6 123 -8 light[level=15]");
			server.runCommand("setblock 6 123 -8 light[level=15]");
			server.runCommand("setblock 0 124 -9 light[level=15]");
			server.runCommand("setblock -6 123 6 light[level=15]");
			server.runCommand("setblock 6 123 6 light[level=15]");

			// ------------------------------------------------------------------
			// 1. Visual Studio 7-Panel Verification (1:1 against Concept & Storyboard)
			// ------------------------------------------------------------------
			int studioId = server.computeOnServer(s -> {
				ServerPlayer p = player(s);
				p.setGameMode(GameType.CREATIVE);
				p.teleportTo(level(s), 0.5, 121.0, -9.0, Set.<Relative>of(), 0.0F, 0.0F, false);
				return spawnHalberdier(level(s), STAGE, 180.0F, false).getId();
			});

			record StudioShot(String name, int move, int tell, float elapsed, Vec3 cam, Vec3 look) {}
			List<StudioShot> shots = List.of(
				new StudioShot("halberdier_01_front_34", OathboundHalberdierRules.LOW_GLAIVE_STALK, 0, 0.0F,
					new Vec3(2.4, 122.7, -3.2), STAGE.add(0.0, 1.32, 0.0)),
				new StudioShot("halberdier_02_back_34", OathboundHalberdierRules.LOW_GLAIVE_STALK, 0, 0.0F,
					new Vec3(-2.6, 122.9, 3.4), STAGE.add(0.0, 1.32, 0.0)),
				new StudioShot("halberdier_03_tempest_whirl", OathboundHalberdierRules.TEMPEST_WHIRL,
					OathboundHalberdierRules.WHIRL_TELL, 16.0F,
					new Vec3(2.8, 122.5, -3.2), STAGE.add(0.0, 1.15, 0.0)),
				new StudioShot("halberdier_04_shadowstep_thrust", OathboundHalberdierRules.SHADOWSTEP_THRUST,
					OathboundHalberdierRules.THRUST_TELL, 12.5F,
					new Vec3(3.3, 122.4, -2.5), STAGE.add(0.0, 1.20, 0.0)),
				new StudioShot("halberdier_05_hook_and_reap", OathboundHalberdierRules.HOOK_AND_REAP,
					OathboundHalberdierRules.HOOK_TELL, 14.0F,
					new Vec3(2.6, 122.5, -3.2), STAGE.add(0.0, 1.20, 0.0)),
				new StudioShot("halberdier_06_sky_vault_windup", OathboundHalberdierRules.SKY_VAULT_CRASH,
					OathboundHalberdierRules.CRASH_TELL, 16.0F,
					new Vec3(2.4, 123.0, -3.5), STAGE.add(0.0, 1.50, 0.0)),
				new StudioShot("halberdier_07_thunderbolt_crash", OathboundHalberdierRules.SKY_VAULT_CRASH,
					OathboundHalberdierRules.CRASH_TELL, 21.0F,
					new Vec3(2.8, 122.3, -3.2), STAGE.add(0.0, 1.05, 0.0)),
				new StudioShot("halberdier_08_glaive_sweep", OathboundHalberdierRules.GLAIVE_SWEEP, 8, 11.0F,
					new Vec3(2.8, 122.4, -3.1), STAGE.add(0.0, 1.20, 0.0)),
				new StudioShot("halberdier_09_pole_vault_kick", OathboundHalberdierRules.POLE_VAULT_KICK, 7, 10.0F,
					new Vec3(2.6, 122.6, -3.0), STAGE.add(0.0, 1.30, 0.0)),
				new StudioShot("halberdier_10_storm_jab", OathboundHalberdierRules.STORM_JAB, 5, 8.0F,
					new Vec3(2.8, 122.5, -3.0), STAGE.add(0.0, 1.25, 0.0)),
				new StudioShot("halberdier_11_cyclone_dance", OathboundHalberdierRules.CYCLONE_DANCE, 10, 15.0F,
					new Vec3(2.9, 122.6, -3.2), STAGE.add(0.0, 1.25, 0.0)),
				new StudioShot("halberdier_12_lightning_lunge", OathboundHalberdierRules.LIGHTNING_LUNGE, 7, 11.0F,
					new Vec3(3.2, 122.4, -2.4), STAGE.add(0.0, 1.20, 0.0)),
				new StudioShot("halberdier_13_reaping_circle", OathboundHalberdierRules.REAPING_CIRCLE, 10, 13.0F,
					new Vec3(2.7, 122.6, -3.2), STAGE.add(0.0, 1.25, 0.0)),
				new StudioShot("halberdier_14_glaive_throw", OathboundHalberdierRules.GLAIVE_THROW, 10, 13.0F,
					new Vec3(2.8, 122.6, -3.1), STAGE.add(0.0, 1.30, 0.0)),
				new StudioShot("halberdier_15_thunder_stomp", OathboundHalberdierRules.THUNDER_STOMP, 12, 15.0F,
					new Vec3(2.7, 122.2, -3.2), STAGE.add(0.0, 1.00, 0.0)),
				new StudioShot("halberdier_16_rising_glaive", OathboundHalberdierRules.RISING_GLAIVE, 8, 10.0F,
					new Vec3(2.5, 122.7, -3.1), STAGE.add(0.0, 1.35, 0.0)),
				new StudioShot("halberdier_17_shadow_clone_strike", OathboundHalberdierRules.SHADOW_CLONE_STRIKE, 8, 11.0F,
					new Vec3(2.8, 122.5, -3.1), STAGE.add(0.0, 1.25, 0.0)),
				new StudioShot("halberdier_18_whirlwind_pull", OathboundHalberdierRules.WHIRLWIND_PULL, 10, 14.0F,
					new Vec3(2.9, 122.6, -3.2), STAGE.add(0.0, 1.25, 0.0)),
				new StudioShot("halberdier_19_skyfall_cleave", OathboundHalberdierRules.SKYFALL_CLEAVE, 12, 17.0F,
					new Vec3(2.8, 122.9, -3.2), STAGE.add(0.0, 1.35, 0.0)),
				new StudioShot("halberdier_20_stormbreaker", OathboundHalberdierRules.STORMBREAKER, 16, 20.0F,
					new Vec3(2.7, 122.9, -3.3), STAGE.add(0.0, 1.45, 0.0))
			);

			for (StudioShot sShot : shots) {
				int tellT = sShot.tell() > 0 ? (int) sShot.tell() : OathboundHalberdierRules.tellTicks(sShot.move(), false);
				int dur = Math.max(tellT + 6, OathboundHalberdierRules.moveDuration(sShot.move(), false));
				boolean sequence = sShot.name().contains("_08_") || sShot.name().contains("_09_") || sShot.name().contains("_10_")
					|| sShot.name().contains("_11_") || sShot.name().contains("_12_") || sShot.name().contains("_13_")
					|| sShot.name().contains("_14_") || sShot.name().contains("_15_") || sShot.name().contains("_16_")
					|| sShot.name().contains("_17_") || sShot.name().contains("_18_") || sShot.name().contains("_19_")
					|| sShot.name().contains("_20_");
				float[] frames = sequence
					? new float[]{Math.max(1, tellT * 0.25F), Math.max(2, tellT * 0.75F), tellT + 1.0F,
						tellT + Math.max(2, (dur - tellT) * 0.35F), tellT + Math.max(3, (dur - tellT) * 0.65F), Math.max(tellT + 2, dur - 2.0F)}
					: new float[]{sShot.elapsed()};
				director(context, world, sShot.cam(), sShot.look());
				for (int fi = 0; fi < frames.length; fi++) {
					final float fe = frames[fi];
					final int ft = tellT;
					server.runOnServer(s -> {
						OathboundHalberdier h = (OathboundHalberdier) level(s).getEntity(studioId);
						h.snapTo(STAGE.x, STAGE.y, STAGE.z, 180.0F, 0.0F);
						h.setYBodyRot(180.0F);
						h.setYHeadRot(180.0F);
						h.poseForPreview(sShot.move(), ft, fe);
					});
					context.waitTicks(4);
					String name = sequence ? sShot.name() + "_f" + fi : sShot.name();
					shot(context, name);
				}
			}
			cut(context);

			// ------------------------------------------------------------------
			// 2. Combat Mechanics: Tempest Whirl, Shadowstep Thrust, Hook Pull, Thunder Cross, Axe Break, NBT & Loot
			// ------------------------------------------------------------------
			server.runOnServer(s -> {
				ServerLevel l = level(s);
				OathboundHalberdier studio = (OathboundHalberdier) l.getEntity(studioId);
				if (studio != null) {
					studio.discard();
				}
				ServerPlayer p = player(s);
				p.setGameMode(GameType.SURVIVAL);
				p.setHealth(p.getMaxHealth());
				p.getInventory().clearContent();
				p.teleportTo(l, 0.5, 121.0, -3.2, Set.<Relative>of(), 0.0F, 0.0F, false);
			});

			int halberdierId = server.computeOnServer(s -> {
				OathboundHalberdier h = spawnHalberdier(level(s), STAGE, 180.0F, true);
				check(Math.abs(h.getAttributeValue(Attributes.MAX_HEALTH) - OathboundHalberdierRules.MAX_HEALTH) < 0.01,
					"Halberdier max health mismatch");
				return h.getId();
			});

			// 2a. Tempest Whirl 360-deg AoE hits player behind Halberdier too
			server.runOnServer(s -> {
				ServerLevel l = level(s);
				ServerPlayer p = player(s);
				p.setHealth(p.getMaxHealth());
				McEntities.clearHurtCooldown(p);
				p.teleportTo(l, 0.5, 121.0, 3.8, Set.<Relative>of(), 180.0F, 0.0F, false);
				OathboundHalberdier h = (OathboundHalberdier) l.getEntity(halberdierId);
				h.snapTo(STAGE.x, STAGE.y, STAGE.z, 180.0F, 0.0F);
				h.setTarget(p);
				h.startMove(l, OathboundHalberdierRules.TEMPEST_WHIRL, null);
			});
			check(await(context, world, 24, s -> player(s).getHealth() < 20.0F),
				"Tempest Whirl did not hit target within 360-degree 4.6-block radius");

			// 2b. Hook-and-Reap pulls target inward & applies Slowness
			server.runOnServer(s -> {
				ServerLevel l = level(s);
				ServerPlayer p = player(s);
				p.removeAllEffects();
				p.setHealth(p.getMaxHealth());
				McEntities.clearHurtCooldown(p);
				p.teleportTo(l, 0.5, 121.0, -4.0, Set.<Relative>of(), 0.0F, 0.0F, false);
				OathboundHalberdier h = (OathboundHalberdier) l.getEntity(halberdierId);
				h.snapTo(STAGE.x, STAGE.y, STAGE.z, 180.0F, 0.0F);
				h.startMove(l, OathboundHalberdierRules.HOOK_AND_REAP, p);
			});
			check(await(context, world, 20, s -> {
				ServerPlayer p = player(s);
				return p.hasEffect(MobEffects.SLOWNESS) && p.getHealth() < 20.0F;
			}), "Hook-and-Reap did not damage and slow frontal target");

			// 2c. Thunderbolt Crash 4-way cross hits side arm target
			server.runOnServer(s -> {
				ServerLevel l = level(s);
				ServerPlayer p = player(s);
				p.setHealth(p.getMaxHealth());
				McEntities.clearHurtCooldown(p);
				p.teleportTo(l, 5.2, 121.0, 0.5, Set.<Relative>of(), 90.0F, 0.0F, false);
				OathboundHalberdier h = (OathboundHalberdier) l.getEntity(halberdierId);
				h.snapTo(STAGE.x, STAGE.y, STAGE.z, 180.0F, 0.0F);
				h.startMove(l, OathboundHalberdierRules.SKY_VAULT_CRASH, null);
			});
			check(await(context, world, 30, s -> player(s).getHealth() < 20.0F),
				"Thunderbolt Crash 4-way cross did not hit target on orthogonal arm");

			// 2d. Heavy axe interrupts Sky-Vault windup -> STAGGERED -> amplified damage
			server.runOnServer(s -> {
				ServerLevel l = level(s);
				ServerPlayer p = player(s);
				OathboundHalberdier h = (OathboundHalberdier) l.getEntity(halberdierId);
				h.setNoAi(true);
				h.snapTo(STAGE.x, STAGE.y, STAGE.z, 180.0F, 0.0F);
				h.startMove(l, OathboundHalberdierRules.SKY_VAULT_CRASH, null);
				p.teleportTo(l, 0.5, 121.0, -2.0, Set.<Relative>of(), 0.0F, 0.0F, false);
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_AXE));
				McEntities.clearHurtCooldown(h);
				h.hurtServer(l, l.damageSources().playerAttack(p), 12.0F);
				check(h.move() == OathboundHalberdierRules.STAGGERED,
					"Axe during Sky-Vault windup must stagger Halberdier, got " + h.move());
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
				McEntities.clearHurtCooldown(h);
				float hpBefore = h.getHealth();
				h.hurtServer(l, l.damageSources().playerAttack(p), 10.0F);
				float amplified = hpBefore - h.getHealth();
				check(amplified >= 7.5F, "Staggered follow-up must deal amplified damage, dealt " + amplified);
				h.setNoAi(false);
			});

			// 2e. Shadowstep Thrust dash advances Halberdier forward and damages line target
			server.runOnServer(s -> {
				ServerLevel l = level(s);
				ServerPlayer p = player(s);
				p.setHealth(p.getMaxHealth());
				McEntities.clearHurtCooldown(p);
				p.teleportTo(l, 0.5, 121.0, -3.5, Set.<Relative>of(), 0.0F, 0.0F, false);
				OathboundHalberdier h = (OathboundHalberdier) l.getEntity(halberdierId);
				h.snapTo(STAGE.x, STAGE.y, STAGE.z, 180.0F, 0.0F);
				h.startMove(l, OathboundHalberdierRules.SHADOWSTEP_THRUST, p);
			});
			check(await(context, world, 24, s -> {
				OathboundHalberdier h = (OathboundHalberdier) level(s).getEntity(halberdierId);
				return h != null && h.getZ() < -1.0 && player(s).getHealth() < 20.0F;
			}), "Shadowstep Blink-Thrust did not advance Halberdier forward and strike target");

			// 2f. Overdrive (<50% HP), NBT persistence, loot drop, FieldGuide & Spawn Egg
			server.runOnServer(s -> {
				OathboundHalberdier h = (OathboundHalberdier) level(s).getEntity(halberdierId);
				h.setHealth(h.getMaxHealth() * 0.40F);
				h.setTarget(null);
			});
			check(await(context, world, 15, s -> {
				OathboundHalberdier h = (OathboundHalberdier) level(s).getEntity(halberdierId);
				return h != null && h.empowered();
			}), "Halberdier did not enter Overdrive below 50% HP");

			server.runOnServer(s -> {
				ServerLevel l = level(s);
				ServerPlayer p = player(s);
				OathboundHalberdier h = (OathboundHalberdier) l.getEntity(halberdierId);
				TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, l.registryAccess());
				h.saveWithoutId(out);

				OathboundHalberdier reloaded = AuraWorld.OATHBOUND_HALBERDIER.create(l, EntitySpawnReason.LOAD);
				reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, l.registryAccess(), out.buildResult()));
				check(reloaded.empowered() && reloaded.getHealth() == h.getHealth(),
					"halberdier_empowered failed to persist across NBT reload");
				check(reloaded.move() == OathboundHalberdierRules.LOW_GLAIVE_STALK,
					"Reload must reset active move cleanly");
				reloaded.discard();

				p.setGameMode(GameType.CREATIVE);
				McEntities.clearHurtCooldown(h);
				h.hurtServer(l, l.damageSources().playerAttack(p), 5000.0F);
			});
			context.waitTicks(4);
			server.runOnServer(s -> {
				ServerLevel l = level(s);
				List<ItemEntity> drops = l.getEntitiesOfClass(ItemEntity.class, new AABB(STAGE, STAGE).inflate(10));
				boolean hasShard = drops.stream().anyMatch(it -> it.getItem().is(AuraWorld.AURA_SHARD));
				boolean hasPage = drops.stream().anyMatch(it -> it.getItem().is(AuraWorld.MANUAL_PAGE));
				check(hasShard && hasPage, "Defeating Oathbound Halberdier must drop Aura Shards and Manual Pages");

				check(FieldGuide.byType("wildercord:oathbound_halberdier").isPresent(),
					"FieldGuide missing wildercord:oathbound_halberdier entry");
				check(BuiltInRegistries.ITEM.getKey(AuraWorld.OATHBOUND_HALBERDIER_SPAWN_EGG).getPath()
						.equals("oathbound_halberdier_spawn_egg"),
					"Missing oathbound_halberdier_spawn_egg registration");
				OathboundHalberdier raised = KnightSpawner.raiseHalberdier(l, BlockPos.containing(STAGE), l.getRandom());
				check(raised != null, "KnightSpawner.raiseHalberdier failed cleanly on open stage");
				raised.discard();
			});
		}
	}
}
