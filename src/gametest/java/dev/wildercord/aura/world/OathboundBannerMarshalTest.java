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
import net.minecraft.world.entity.EntityTypes;
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
 * End-to-end verification and 7-panel 1:1 visual studio suite for Knight #4: {@link OathboundBannerMarshal}.
 */
public final class OathboundBannerMarshalTest implements FabricClientGameTest {
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
				e -> e.entityTags().contains("wildercord.marshal_cam")).forEach(Entity::discard);
			Display.TextDisplay cam = EntityTypes.TEXT_DISPLAY.create(l, EntitySpawnReason.COMMAND);
			cam.snapTo(eye.x, eye.y, eye.z, yaw(eye, target), pitch(eye, target));
			cam.addTag("wildercord.marshal_cam");
			l.addFreshEntity(cam);
			return cam.getId();
		});
		context.waitTicks(3);
		context.runOnClient(mc -> {
			Entity cam = mc.level.getEntity(camId);
			if (cam != null) {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.setCameraEntity(cam);
				if (!mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
				mc.gui.toastManager().clear();
				mc.gui.hud.getChat().clearMessages(false);
			}
		});
	}

	private static void cut(ClientGameTestContext context) {
		context.runOnClient(mc -> {
			mc.setCameraEntity(mc.player);
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
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

	private static OathboundBannerMarshal spawnMarshal(ServerLevel l, Vec3 pos, float yaw, boolean ai) {
		OathboundBannerMarshal m = AuraWorld.OATHBOUND_BANNER_MARSHAL.create(l, EntitySpawnReason.COMMAND);
		m.snapTo(pos.x, pos.y, pos.z, yaw, 0.0F);
		m.setYHeadRot(yaw);
		m.setYBodyRot(yaw);
		m.setPersistenceRequired();
		m.setNoAi(!ai);
		l.addFreshEntity(m);
		return m;
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
				return spawnMarshal(level(s), STAGE, 180.0F, false).getId();
			});

			record StudioShot(String name, int move, int tell, float elapsed, boolean counter, Vec3 cam, Vec3 look) {}
			List<StudioShot> shots = List.of(
				new StudioShot("banner_marshal_01_front_34", OathboundBannerMarshalRules.SOVEREIGN_MARCH, 0, 0.0F, false,
					new Vec3(2.6, 123.0, -3.5), STAGE.add(0.0, 1.55, 0.0)),
				new StudioShot("banner_marshal_02_back_34", OathboundBannerMarshalRules.SOVEREIGN_MARCH, 0, 0.0F, false,
					new Vec3(-2.7, 123.2, 3.8), STAGE.add(0.0, 1.60, 0.0)),
				new StudioShot("banner_marshal_03_decree_standard", OathboundBannerMarshalRules.DECREE_OF_THE_STANDARD,
					OathboundBannerMarshalRules.DECREE_TELL, 12.0F, false,
					new Vec3(2.5, 123.0, -3.6), STAGE.add(0.0, 1.55, 0.0)),
				new StudioShot("banner_marshal_04_solar_aegis_guard", OathboundBannerMarshalRules.SOLAR_AEGIS_GUARD,
					OathboundBannerMarshalRules.GUARD_TELL, 14.0F, false,
					new Vec3(2.4, 122.7, -3.4), STAGE.add(0.0, 1.35, 0.0)),
				new StudioShot("banner_marshal_05_sunblade_cleave", OathboundBannerMarshalRules.RADIANT_SUNBLADE_CLEAVE,
					OathboundBannerMarshalRules.CLEAVE_TELL, 15.0F, false,
					new Vec3(2.8, 122.6, -3.3), STAGE.add(0.0, 1.30, 0.0)),
				new StudioShot("banner_marshal_06_heavens_lance_windup", OathboundBannerMarshalRules.SOVEREIGN_JUDGMENT,
					OathboundBannerMarshalRules.JUDGMENT_TELL, 18.0F, false,
					new Vec3(2.6, 123.1, -3.6), STAGE.add(0.0, 1.65, 0.0)),
				new StudioShot("banner_marshal_07_sovereign_judgment", OathboundBannerMarshalRules.SOVEREIGN_JUDGMENT,
					OathboundBannerMarshalRules.JUDGMENT_TELL, 22.5F, false,
					new Vec3(2.8, 122.5, -3.4), STAGE.add(0.0, 1.25, 0.0))
			);

			for (StudioShot sShot : shots) {
				server.runOnServer(s -> {
					OathboundBannerMarshal m = (OathboundBannerMarshal) level(s).getEntity(studioId);
					m.snapTo(STAGE.x, STAGE.y, STAGE.z, 180.0F, 0.0F);
					m.setYBodyRot(180.0F);
					m.setYHeadRot(180.0F);
					m.poseForPreview(sShot.move(), sShot.tell(), sShot.elapsed(), sShot.counter());
				});
				director(context, world, sShot.cam(), sShot.look());
				context.waitTicks(8);
				shot(context, sShot.name());
			}
			cut(context);

			// ------------------------------------------------------------------
			// 2. Combat Mechanics: Decree Ally Buff/Heal, Solar Aegis Block/Reflect/Counter/Axe Break, Judgment, NBT & Loot
			// ------------------------------------------------------------------
			server.runOnServer(s -> {
				ServerLevel l = level(s);
				OathboundBannerMarshal studio = (OathboundBannerMarshal) l.getEntity(studioId);
				if (studio != null) {
					studio.discard();
				}
				ServerPlayer p = player(s);
				p.setGameMode(GameType.SURVIVAL);
				p.setHealth(p.getMaxHealth());
				p.getInventory().clearContent();
				p.teleportTo(l, 0.5, 121.0, -3.0, Set.<Relative>of(), 0.0F, 0.0F, false);
			});

			int marshalId = server.computeOnServer(s -> {
				OathboundBannerMarshal m = spawnMarshal(level(s), STAGE, 180.0F, true);
				check(Math.abs(m.getAttributeValue(Attributes.MAX_HEALTH) - OathboundBannerMarshalRules.MAX_HEALTH) < 0.01,
					"Banner-Marshal max health mismatch");
				return m.getId();
			});

			// 2a. Decree of the Standard heals & buffs nearby allied knight within 12m
			int allyId = server.computeOnServer(s -> {
				ServerLevel l = level(s);
				OathboundHalberdier ally = AuraWorld.OATHBOUND_HALBERDIER.create(l, EntitySpawnReason.COMMAND);
				ally.snapTo(4.5, 121.0, 2.5, 180.0F, 0.0F);
				ally.setNoAi(true);
				ally.setHealth(80.0F);
				l.addFreshEntity(ally);
				OathboundBannerMarshal m = (OathboundBannerMarshal) l.getEntity(marshalId);
				m.startMove(l, OathboundBannerMarshalRules.DECREE_OF_THE_STANDARD, null);
				return ally.getId();
			});
			check(await(context, world, 24, s -> {
				Entity e = level(s).getEntity(allyId);
				return e instanceof OathboundHalberdier ally
					&& ally.getHealth() >= 87.5F
					&& ally.hasEffect(MobEffects.STRENGTH)
					&& ally.hasEffect(MobEffects.RESISTANCE);
			}), "Decree of the Standard did not heal and buff allied knight within 12 blocks");
			server.runOnServer(s -> {
				Entity e = level(s).getEntity(allyId);
				if (e != null) {
					e.discard();
				}
			});

			// 2b. Solar Aegis Guard: arrow deflection + frontal melee block + counter-thrust
			server.runOnServer(s -> {
				OathboundBannerMarshal m = (OathboundBannerMarshal) level(s).getEntity(marshalId);
				m.snapTo(STAGE.x, STAGE.y, STAGE.z, 180.0F, 0.0F);
				m.setTarget(null);
				m.startMove(level(s), OathboundBannerMarshalRules.SOLAR_AEGIS_GUARD, null);
			});
			context.waitTicks(OathboundBannerMarshalRules.GUARD_TELL + 2);
			int arrowId = server.computeOnServer(s -> {
				ServerLevel l = level(s);
				ServerPlayer p = player(s);
				Arrow arrow = new Arrow(l, p, new ItemStack(Items.ARROW), null);
				arrow.setPos(0.5, 122.2, -1.5);
				arrow.setDeltaMovement(new Vec3(0.0, 0.0, 1.8));
				l.addFreshEntity(arrow);
				return arrow.getId();
			});
			check(await(context, world, 15, s -> {
				Entity a = level(s).getEntity(arrowId);
				return a != null && a.getDeltaMovement().z < -0.1;
			}), "Arrow hitting Solar Aegis Guard did not deflect back");
			server.runOnServer(s -> {
				Entity a = level(s).getEntity(arrowId);
				if (a != null) {
					a.discard();
				}
				ServerPlayer p = player(s);
				p.setHealth(p.getMaxHealth());
				p.setInvulnerableTime(0);
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
				OathboundBannerMarshal m = (OathboundBannerMarshal) level(s).getEntity(marshalId);
				m.setTarget(p);
				McEntities.clearHurtCooldown(m);
				float hpBefore = m.getHealth();
				m.hurtServer(level(s), level(s).damageSources().playerAttack(p), 10.0F);
				float dealt = hpBefore - m.getHealth();
				check(dealt <= 2.5F, "Expected Solar Aegis Guard frontal block <= 2.5, got " + dealt);
				check(m.guardCountered(), "Expected Solar Aegis Guard counter-thrust to trigger");
			});
			check(await(context, world, 15, s -> player(s).getHealth() < 20.0F),
				"Solar Aegis Guard counter-thrust did not hit attacker");

			// 2c. Heavy axe shatters Solar Aegis Guard -> STAGGERED -> amplified damage
			server.runOnServer(s -> {
				OathboundBannerMarshal m = (OathboundBannerMarshal) level(s).getEntity(marshalId);
				m.setNoAi(true);
				m.snapTo(STAGE.x, STAGE.y, STAGE.z, 180.0F, 0.0F);
				m.startMove(level(s), OathboundBannerMarshalRules.SOLAR_AEGIS_GUARD, null);
			});
			context.waitTicks(OathboundBannerMarshalRules.GUARD_TELL + 2);
			server.runOnServer(s -> {
				ServerLevel l = level(s);
				ServerPlayer p = player(s);
				p.teleportTo(l, 0.5, 121.0, -2.2, Set.<Relative>of(), 0.0F, 0.0F, false);
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_AXE));
				OathboundBannerMarshal m = (OathboundBannerMarshal) l.getEntity(marshalId);
				McEntities.clearHurtCooldown(m);
				m.hurtServer(l, l.damageSources().playerAttack(p), 12.0F);
				check(m.move() == OathboundBannerMarshalRules.STAGGERED,
					"Diamond Axe hitting Solar Aegis Guard must shatter guard into STAGGERED");
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
				McEntities.clearHurtCooldown(m);
				float hpBefore = m.getHealth();
				m.hurtServer(l, l.damageSources().playerAttack(p), 10.0F);
				float amplified = hpBefore - m.getHealth();
				check(amplified >= 5.0F, "Staggered follow-up should deal amplified damage, dealt " + amplified);
				m.setNoAi(false);
			});

			// 2d. Sovereign Judgment pillar ring damages survival player inside 5.5m radius
			server.runOnServer(s -> {
				ServerLevel l = level(s);
				ServerPlayer p = player(s);
				p.setHealth(p.getMaxHealth());
				p.setInvulnerableTime(0);
				p.teleportTo(l, 0.5, 121.0, -4.0, Set.<Relative>of(), 0.0F, 0.0F, false);
				OathboundBannerMarshal m = (OathboundBannerMarshal) l.getEntity(marshalId);
				m.snapTo(STAGE.x, STAGE.y, STAGE.z, 180.0F, 0.0F);
				m.startMove(l, OathboundBannerMarshalRules.SOVEREIGN_JUDGMENT, p);
			});
			check(await(context, world, 36, s -> player(s).getHealth() < 20.0F),
				"Sovereign Judgment pillars did not damage player inside 5.5m ring");

			// 2e. Empowered (<50% HP), NBT persistence, loot drop, FieldGuide & Spawn Egg
			server.runOnServer(s -> {
				OathboundBannerMarshal m = (OathboundBannerMarshal) level(s).getEntity(marshalId);
				m.setHealth(m.getMaxHealth() * 0.40F);
				m.setTarget(null);
			});
			check(await(context, world, 15, s -> {
				OathboundBannerMarshal m = (OathboundBannerMarshal) level(s).getEntity(marshalId);
				return m != null && m.empowered();
			}), "Banner-Marshal did not enter Solar Sovereignty below 50% HP");

			server.runOnServer(s -> {
				ServerLevel l = level(s);
				ServerPlayer p = player(s);
				OathboundBannerMarshal m = (OathboundBannerMarshal) l.getEntity(marshalId);
				TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, l.registryAccess());
				m.saveWithoutId(out);

				OathboundBannerMarshal reloaded = AuraWorld.OATHBOUND_BANNER_MARSHAL.create(l, EntitySpawnReason.LOAD);
				reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, l.registryAccess(), out.buildResult()));
				check(reloaded.empowered() && reloaded.getHealth() == m.getHealth(),
					"marshal_empowered failed to persist across NBT reload");
				check(reloaded.move() == OathboundBannerMarshalRules.SOVEREIGN_MARCH,
					"Reload must reset active move cleanly");
				reloaded.discard();

				p.setGameMode(GameType.CREATIVE);
				McEntities.clearHurtCooldown(m);
				m.hurtServer(l, l.damageSources().playerAttack(p), 5000.0F);
			});
			context.waitTicks(4);
			server.runOnServer(s -> {
				ServerLevel l = level(s);
				List<ItemEntity> drops = l.getEntitiesOfClass(ItemEntity.class, new AABB(STAGE, STAGE).inflate(10));
				boolean hasShard = drops.stream().anyMatch(it -> it.getItem().is(AuraWorld.AURA_SHARD));
				boolean hasPage = drops.stream().anyMatch(it -> it.getItem().is(AuraWorld.MANUAL_PAGE));
				check(hasShard && hasPage, "Defeating Oathbound Banner-Marshal must drop Aura Shards and Manual Pages");

				check(FieldGuide.byType("wildercord:oathbound_banner_marshal").isPresent(),
					"FieldGuide missing wildercord:oathbound_banner_marshal entry");
				check(BuiltInRegistries.ITEM.getKey(AuraWorld.OATHBOUND_BANNER_MARSHAL_SPAWN_EGG).getPath()
						.equals("oathbound_banner_marshal_spawn_egg"),
					"Missing oathbound_banner_marshal_spawn_egg registration");
				OathboundBannerMarshal raised = KnightSpawner.raiseBannerMarshal(l, BlockPos.containing(STAGE), l.getRandom());
				check(raised != null, "KnightSpawner.raiseBannerMarshal failed cleanly on open stage");
				raised.discard();
			});
		}
	}
}
