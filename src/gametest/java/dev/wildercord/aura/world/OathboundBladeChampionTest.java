package dev.wildercord.aura.world;
import dev.wildercord.gametest.compat.GametestCompat;

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
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
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
 * End-to-end verification and 7-panel 1:1 visual studio suite for Knight #2: {@link OathboundBladeChampion}.
 */
public final class OathboundBladeChampionTest implements FabricClientGameTest {
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
				e -> e.entityTags().contains("wildercord.blade_cam")).forEach(Entity::discard);
			Display.TextDisplay cam = EntityType.TEXT_DISPLAY.create(l, EntitySpawnReason.COMMAND);
			cam.snapTo(eye.x, eye.y, eye.z, yaw(eye, target), pitch(eye, target));
			cam.addTag("wildercord.blade_cam");
			l.addFreshEntity(cam);
			return cam.getId();
		});
		context.waitTicks(3);
		context.runOnClient(mc -> {
			Entity cam = mc.level.getEntity(camId);
			if (cam != null) {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.setCameraEntity(cam);
				if (!GametestCompat.hudHidden(mc)) {
					GametestCompat.toggleHud(mc);
				}
				GametestCompat.clearToasts(mc);
				GametestCompat.clearChat(mc);
			}
		});
	}

	private static void cut(ClientGameTestContext context) {
		context.runOnClient(mc -> {
			mc.setCameraEntity(mc.player);
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			if (GametestCompat.hudHidden(mc)) {
				GametestCompat.toggleHud(mc);
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

	private static OathboundBladeChampion spawnChampion(ServerLevel l, Vec3 pos, float yaw, boolean ai) {
		OathboundBladeChampion c = AuraWorld.OATHBOUND_BLADE_CHAMPION.create(l, EntitySpawnReason.COMMAND);
		c.snapTo(pos.x, pos.y, pos.z, yaw, 0.0F);
		c.setYHeadRot(yaw);
		c.setYBodyRot(yaw);
		c.setPersistenceRequired();
		c.setNoAi(!ai);
		l.addFreshEntity(c);
		return c;
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

			// Build museum showcase floor & dark deepslate backdrop behind +Z (lights hidden behind cameras)
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
			// 1. Visual Studio 7-Panel Verification (1:1 against Concept & Anim Sheet)
			// ------------------------------------------------------------------
			int studioId = server.computeOnServer(s -> {
				ServerPlayer p = player(s);
				p.setGameMode(GameType.CREATIVE);
				p.teleportTo(level(s), 0.5, 121.0, -9.0, Set.<Relative>of(), 0.0F, 0.0F, false);
				return spawnChampion(level(s), STAGE, 180.0F, false).getId();
			});

			record StudioShot(String name, int move, int tell, float elapsed, boolean counter, Vec3 cam, Vec3 look) {}
			List<StudioShot> shots = List.of(
				new StudioShot("blade_champion_01_front_34", OathboundBladeChampionRules.IDLE_STALK, 0, 0.0F, false,
					new Vec3(2.3, 122.7, -3.1), STAGE.add(0.0, 1.35, 0.0)),
				new StudioShot("blade_champion_02_back_34", OathboundBladeChampionRules.IDLE_STALK, 0, 0.0F, false,
					new Vec3(-2.6, 122.9, 3.4), STAGE.add(0.0, 1.35, 0.0)),
				new StudioShot("blade_champion_03_riposte_parry", OathboundBladeChampionRules.WRATHFUL_RIPOSTE,
					OathboundBladeChampionRules.RIPOSTE_TELL, 14.0F, false,
					new Vec3(2.3, 122.5, -3.2), STAGE.add(0.0, 1.25, 0.0)),
				new StudioShot("blade_champion_04_crescent_cleave", OathboundBladeChampionRules.CRESCENT_CLEAVE,
					OathboundBladeChampionRules.CRESCENT_TELL, 16.0F, false,
					new Vec3(2.7, 122.3, -3.1), STAGE.add(0.0, 1.15, 0.0)),
				new StudioShot("blade_champion_05_pommel_lunge", OathboundBladeChampionRules.POMMEL_BREAK,
					OathboundBladeChampionRules.POMMEL_TELL, 14.0F, false,
					new Vec3(3.2, 122.4, -2.5), STAGE.add(0.0, 1.20, 0.0)),
				new StudioShot("blade_champion_06_guillotine_windup", OathboundBladeChampionRules.GUILLOTINE_SLAM,
					OathboundBladeChampionRules.GUILLOTINE_TELL, 18.0F, false,
					new Vec3(2.4, 122.9, -3.4), STAGE.add(0.0, 1.55, 0.0)),
				new StudioShot("blade_champion_07_guillotine_cleave", OathboundBladeChampionRules.GUILLOTINE_SLAM,
					OathboundBladeChampionRules.GUILLOTINE_TELL, 23.0F, false,
					new Vec3(2.7, 122.3, -3.2), STAGE.add(0.0, 1.05, 0.0)),
				new StudioShot("blade_champion_08_lunge_thrust", OathboundBladeChampionRules.LUNGE_THRUST, 8, 11.0F, false,
					new Vec3(3.1, 122.4, -2.4), STAGE.add(0.0, 1.20, 0.0)),
				new StudioShot("blade_champion_09_rising_slash", OathboundBladeChampionRules.RISING_SLASH, 7, 9.0F, false,
					new Vec3(2.5, 122.6, -3.0), STAGE.add(0.0, 1.30, 0.0)),
				new StudioShot("blade_champion_10_cross_cut", OathboundBladeChampionRules.CROSS_CUT, 10, 14.0F, false,
					new Vec3(2.6, 122.5, -3.2), STAGE.add(0.0, 1.25, 0.0)),
				new StudioShot("blade_champion_11_blade_flurry", OathboundBladeChampionRules.BLADE_FLURRY, 6, 10.0F, false,
					new Vec3(2.7, 122.5, -3.1), STAGE.add(0.0, 1.25, 0.0)),
				new StudioShot("blade_champion_12_spin_slash", OathboundBladeChampionRules.SPIN_SLASH, 9, 13.0F, false,
					new Vec3(2.8, 122.7, -3.2), STAGE.add(0.0, 1.25, 0.0)),
				new StudioShot("blade_champion_13_backstep_slash", OathboundBladeChampionRules.BACKSTEP_SLASH, 6, 9.0F, false,
					new Vec3(2.8, 122.5, -2.8), STAGE.add(0.0, 1.20, 0.0)),
				new StudioShot("blade_champion_14_mirror_stance", OathboundBladeChampionRules.MIRROR_STANCE, 4, 8.0F, false,
					new Vec3(2.4, 122.5, -3.2), STAGE.add(0.0, 1.25, 0.0)),
				new StudioShot("blade_champion_15_severing_arc", OathboundBladeChampionRules.SEVERING_ARC, 12, 16.0F, false,
					new Vec3(2.9, 122.4, -3.1), STAGE.add(0.0, 1.20, 0.0)),
				new StudioShot("blade_champion_16_piercing_rain", OathboundBladeChampionRules.PIERCING_RAIN, 10, 15.0F, false,
					new Vec3(2.7, 122.9, -3.2), STAGE.add(0.0, 1.35, 0.0)),
				new StudioShot("blade_champion_17_blade_bind", OathboundBladeChampionRules.BLADE_BIND, 8, 11.0F, false,
					new Vec3(2.5, 122.5, -3.1), STAGE.add(0.0, 1.25, 0.0)),
				new StudioShot("blade_champion_18_crimson_tempest", OathboundBladeChampionRules.CRIMSON_TEMPEST, 12, 18.0F, false,
					new Vec3(2.9, 122.7, -3.3), STAGE.add(0.0, 1.30, 0.0)),
				new StudioShot("blade_champion_19_executioner_drop", OathboundBladeChampionRules.EXECUTIONER_DROP, 18, 21.0F, false,
					new Vec3(2.6, 122.9, -3.3), STAGE.add(0.0, 1.45, 0.0)),
				new StudioShot("blade_champion_20_phantom_strike", OathboundBladeChampionRules.PHANTOM_STRIKE, 8, 11.0F, false,
					new Vec3(3.0, 122.5, -2.8), STAGE.add(0.0, 1.25, 0.0))
			);

			for (StudioShot sShot : shots) {
				int tellT = sShot.tell() > 0 ? (int) sShot.tell() : OathboundBladeChampionRules.tellTicks(sShot.move(), false);
				int dur = Math.max(tellT + 6, OathboundBladeChampionRules.moveDuration(sShot.move(), false));
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
						OathboundBladeChampion c = (OathboundBladeChampion) level(s).getEntity(studioId);
						c.snapTo(STAGE.x, STAGE.y, STAGE.z, 180.0F, 0.0F);
						c.poseForPreview(sShot.move(), ft, fe, sShot.counter());
					});
					context.waitTicks(4);
					String name = sequence ? sShot.name() + "_f" + fi : sShot.name();
					shot(context, name);
				}
			}
			cut(context);

			// ------------------------------------------------------------------
			// 2. Combat Mechanics: Parry/Reflect/Counter, Axe Break, Pommel Shatter, Guillotine Line, NBT, Loot
			// ------------------------------------------------------------------
			server.runOnServer(s -> {
				ServerLevel l = level(s);
				OathboundBladeChampion studio = (OathboundBladeChampion) l.getEntity(studioId);
				if (studio != null) {
					studio.discard();
				}
				ServerPlayer p = player(s);
				p.setGameMode(GameType.SURVIVAL);
				p.setHealth(p.getMaxHealth());
				p.getInventory().clearContent();
				p.teleportTo(l, 0.5, 121.0, -2.5, Set.<Relative>of(), 0.0F, 0.0F, false);
			});

			int champId = server.computeOnServer(s -> {
				OathboundBladeChampion c = spawnChampion(level(s), STAGE, 180.0F, true);
				check(Math.abs(c.getAttributeValue(Attributes.MAX_HEALTH) - OathboundBladeChampionRules.MAX_HEALTH) < 0.01,
					"Blade-Champion max health mismatch");
				return c.getId();
			});

			// 2a. Projectile Deflection during Wrathful Riposte
			server.runOnServer(s -> {
				OathboundBladeChampion c = (OathboundBladeChampion) level(s).getEntity(champId);
				c.snapTo(STAGE.x, STAGE.y, STAGE.z, 180.0F, 0.0F);
				c.setTarget(null);
				c.startMove(level(s), OathboundBladeChampionRules.WRATHFUL_RIPOSTE, null);
			});
			context.waitTicks(OathboundBladeChampionRules.RIPOSTE_TELL + 2);
			int arrowId = server.computeOnServer(s -> {
				ServerLevel l = level(s);
				ServerPlayer p = player(s);
				Arrow arrow = new Arrow(l, p, new ItemStack(Items.ARROW), null);
				arrow.setPos(0.5, 122.2, -1.5);
				arrow.setDeltaMovement(new Vec3(0.0, 0.0, 1.8));
				l.addFreshEntity(arrow);
				return arrow.getId();
			});
			boolean reflected = await(context, world, 15, s -> {
				Entity a = level(s).getEntity(arrowId);
				return a != null && a.getDeltaMovement().z < -0.1;
			});
			check(reflected, "Arrow hitting frontal Wrathful Riposte did not deflect back at shooter");
			server.runOnServer(s -> {
				Entity a = level(s).getEntity(arrowId);
				if (a != null) {
					a.discard();
				}
			});

			// 2b. Frontal sword attack gets parried + triggers Riposte Counter Cleave
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				p.setHealth(p.getMaxHealth());
				McEntities.clearHurtCooldown(p);
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
				OathboundBladeChampion c = (OathboundBladeChampion) level(s).getEntity(champId);
				c.setTarget(p);
				McEntities.clearHurtCooldown(c);
				float hpBefore = c.getHealth();
				c.hurtServer(level(s), level(s).damageSources().playerAttack(p), 10.0F);
				float dealt = hpBefore - c.getHealth();
				check(dealt <= 2.5F, "Expected Wrathful Riposte frontal parry <= 2.5 damage, got " + dealt);
				check(c.riposteTriggered(), "Expected Wrathful Riposte counter cleave to trigger on frontal strike");
			});
			boolean counterHitPlayer = await(context, world, 15, s -> player(s).getHealth() < 20.0F);
			check(counterHitPlayer, "Riposte counter cleave did not strike attacker");

			// 2c. Heavy axe smashes Wrathful Riposte -> STAGGERED -> +45% follow-up damage
			server.runOnServer(s -> {
				OathboundBladeChampion c = (OathboundBladeChampion) level(s).getEntity(champId);
				c.setNoAi(true);
				c.snapTo(STAGE.x, STAGE.y, STAGE.z, 180.0F, 0.0F);
				c.startMove(level(s), OathboundBladeChampionRules.WRATHFUL_RIPOSTE, null);
			});
			context.waitTicks(OathboundBladeChampionRules.RIPOSTE_TELL + 2);
			server.runOnServer(s -> {
				ServerLevel l = level(s);
				ServerPlayer p = player(s);
				p.teleportTo(l, 0.5, 121.0, -2.2, Set.<Relative>of(), 0.0F, 0.0F, false);
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_AXE));
				OathboundBladeChampion c = (OathboundBladeChampion) l.getEntity(champId);
				McEntities.clearHurtCooldown(c);
				c.hurtServer(l, l.damageSources().playerAttack(p), 12.0F);
				check(c.move() == OathboundBladeChampionRules.STAGGERED,
					"Diamond Axe hitting active Wrathful Riposte must shatter guard into STAGGERED, was " + c.move());
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
				McEntities.clearHurtCooldown(c);
				float hpBefore = c.getHealth();
				c.hurtServer(l, l.damageSources().playerAttack(p), 10.0F);
				float amplified = hpBefore - c.getHealth();
				check(amplified >= 7.0F, "Staggered follow-up should deal amplified damage, dealt " + amplified);
				c.setNoAi(false);
			});

			// 2d. Pommel-Break Lunge shatters player blocking shield & disables shield item cooldown
			server.runOnServer(s -> {
				ServerLevel l = level(s);
				ServerPlayer p = player(s);
				p.setHealth(p.getMaxHealth());
				McEntities.clearHurtCooldown(p);
				p.teleportTo(l, 0.5, 121.0, -3.5, Set.<Relative>of(), 0.0F, 0.0F, false);
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHIELD));
			});
			context.runOnClient(mc -> {
				mc.gui.setScreen(null);
				mc.options.keyUse.setDown(true);
				if (mc.player != null) {
					mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
				}
			});
			context.waitTicks(7);
			try {
				server.runOnServer(s -> {
					ServerPlayer p = player(s);
					check(p.isBlocking(), "Player shield must be active before Pommel-Break test");
					OathboundBladeChampion c = (OathboundBladeChampion) level(s).getEntity(champId);
					c.snapTo(STAGE.x, STAGE.y, STAGE.z, 180.0F, 0.0F);
					c.startMove(level(s), OathboundBladeChampionRules.POMMEL_BREAK, p);
				});
				boolean shieldShattered = await(context, world, 30, s -> {
					ServerPlayer p = player(s);
					return p.getCooldowns().isOnCooldown(new ItemStack(Items.SHIELD)) && p.getHealth() < 20.0F;
				});
				check(shieldShattered, "Pommel-Break Lunge did not shatter player shield and deal impact damage");
			} finally {
				context.runOnClient(mc -> mc.options.keyUse.setDown(false));
			}

			// 2e. Sky-Rending Guillotine forward line fissure damage
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				p.setHealth(p.getMaxHealth());
				McEntities.clearHurtCooldown(p);
				p.stopUsingItem();
				p.teleportTo(level(s), 0.5, 121.0, -4.2, Set.<Relative>of(), 0.0F, 0.0F, false);
				OathboundBladeChampion c = (OathboundBladeChampion) level(s).getEntity(champId);
				c.snapTo(STAGE.x, STAGE.y, STAGE.z, 180.0F, 0.0F);
				c.startMove(level(s), OathboundBladeChampionRules.GUILLOTINE_SLAM, p);
			});
			boolean guillotineLanded = await(context, world, 36, s -> player(s).getHealth() < 20.0F);
			check(guillotineLanded, "Sky-Rending Guillotine forward crimson fissure did not damage target at 4.7m");

			// 2f. Crimson Overdrive (<50% HP), NBT Save/Load persistence, Loot & Bestiary FieldGuide
			server.runOnServer(s -> {
				OathboundBladeChampion c = (OathboundBladeChampion) level(s).getEntity(champId);
				c.setHealth(c.getMaxHealth() * 0.40F);
				c.setTarget(null);
			});
			boolean becameEmpowered = await(context, world, 15, s -> {
				OathboundBladeChampion c = (OathboundBladeChampion) level(s).getEntity(champId);
				return c != null && c.empowered();
			});
			check(becameEmpowered, "Champion did not enter Crimson Overdrive below 50% HP");

			server.runOnServer(s -> {
				ServerLevel l = level(s);
				ServerPlayer p = player(s);
				OathboundBladeChampion c = (OathboundBladeChampion) l.getEntity(champId);
				TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, l.registryAccess());
				c.saveWithoutId(out);

				OathboundBladeChampion reloaded = AuraWorld.OATHBOUND_BLADE_CHAMPION.create(l, EntitySpawnReason.LOAD);
				reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, l.registryAccess(), out.buildResult()));
				check(reloaded.empowered() && reloaded.getHealth() == c.getHealth(),
					"blade_champion_empowered & health failed to persist across NBT reload");
				check(reloaded.move() == OathboundBladeChampionRules.IDLE_STALK,
					"Reload must reset active move cleanly to IDLE_STALK");
				reloaded.discard();

				p.setGameMode(GameType.CREATIVE);
				McEntities.clearHurtCooldown(c);
				c.hurtServer(l, l.damageSources().playerAttack(p), 5000.0F);
			});
			context.waitTicks(4);
			server.runOnServer(s -> {
				ServerLevel l = level(s);
				List<ItemEntity> drops = l.getEntitiesOfClass(ItemEntity.class, new AABB(STAGE, STAGE).inflate(10));
				boolean hasShard = drops.stream().anyMatch(it -> it.getItem().is(AuraWorld.AURA_SHARD));
				boolean hasPage = drops.stream().anyMatch(it -> it.getItem().is(AuraWorld.MANUAL_PAGE));
				check(hasShard && hasPage, "Defeating Oathbound Blade-Champion must drop Aura Shards and Manual Pages");

				check(FieldGuide.byType("wildercord:oathbound_blade_champion").isPresent(),
					"FieldGuide missing wildercord:oathbound_blade_champion entry");
				check(BuiltInRegistries.ITEM.getKey(AuraWorld.OATHBOUND_BLADE_CHAMPION_SPAWN_EGG).getPath()
						.equals("oathbound_blade_champion_spawn_egg"),
					"Missing oathbound_blade_champion_spawn_egg item registration");
				OathboundBladeChampion raised = KnightSpawner.raiseBladeChampion(l, BlockPos.containing(STAGE), l.getRandom());
				check(raised != null, "KnightSpawner.raiseBladeChampion failed cleanly on open stage");
				raised.discard();
			});
		}
	}
}
