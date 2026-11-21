package dev.wildercord.aura.world;

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
 * End-to-end verification and 7-panel 1:1 visual studio suite for Knight #1: {@link OathboundBastion}.
 */
public final class OathboundBastionTest implements FabricClientGameTest {
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
				e -> e.entityTags().contains("wildercord.bastion_cam")).forEach(Entity::discard);
			Display.TextDisplay cam = EntityTypes.TEXT_DISPLAY.create(l, EntitySpawnReason.COMMAND);
			cam.snapTo(eye.x, eye.y, eye.z, yaw(eye, target), pitch(eye, target));
			cam.addTag("wildercord.bastion_cam");
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

	private static OathboundBastion spawnBastion(ServerLevel l, Vec3 pos, float yaw, boolean ai) {
		OathboundBastion b = AuraWorld.OATHBOUND_BASTION.create(l, EntitySpawnReason.COMMAND);
		b.snapTo(pos.x, pos.y, pos.z, yaw, 0.0F);
		b.setYHeadRot(yaw);
		b.setYBodyRot(yaw);
		b.setPersistenceRequired();
		b.setNoAi(!ai);
		l.addFreshEntity(b);
		return b;
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
			server.runCommand("setblock -6 121 -8 sea_lantern");
			server.runCommand("setblock 6 121 -8 sea_lantern");
			server.runCommand("setblock 0 121 -9 sea_lantern");

			// ------------------------------------------------------------------
			// 1. Visual Studio 7-Panel Verification (1:1 against Concept & Anim Sheet)
			// ------------------------------------------------------------------
			int studioId = server.computeOnServer(s -> {
				ServerPlayer p = player(s);
				p.setGameMode(GameType.CREATIVE);
				p.teleportTo(level(s), 0.5, 121.0, -9.0, Set.<Relative>of(), 0.0F, 0.0F, false);
				return spawnBastion(level(s), STAGE, 180.0F, false).getId();
			});

			record StudioShot(String name, int move, int tell, float elapsed, float barrier, Vec3 cam, Vec3 look) {}
			List<StudioShot> shots = List.of(
				new StudioShot("bastion_01_front_34", OathboundBastionRules.IDLE_MARCH, 0, 0.0F, 0.25F,
					new Vec3(2.4, 122.7, -3.1), STAGE.add(0.0, 1.35, 0.0)),
				new StudioShot("bastion_02_back_34", OathboundBastionRules.IDLE_MARCH, 0, 0.0F, 0.25F,
					new Vec3(-2.6, 122.9, 3.4), STAGE.add(0.0, 1.35, 0.0)),
				new StudioShot("bastion_03_phalanx_brace", OathboundBastionRules.PHALANX_BRACE, OathboundBastionRules.PHALANX_TELL, 14.0F, 1.0F,
					new Vec3(2.3, 122.4, -3.2), STAGE.add(0.0, 1.15, 0.0)),
				new StudioShot("bastion_04_shield_ram", OathboundBastionRules.SHIELD_RAM, OathboundBastionRules.RAM_TELL, 20.0F, 0.85F,
					new Vec3(3.3, 122.5, -2.5), STAGE.add(0.0, 1.2, 0.0)),
				new StudioShot("bastion_05_anvil_windup", OathboundBastionRules.ANVIL_SLAM, OathboundBastionRules.ANVIL_TELL, 18.0F, 0.35F,
					new Vec3(2.4, 122.8, -3.4), STAGE.add(0.0, 1.55, 0.0)),
				new StudioShot("bastion_06_anvil_impact", OathboundBastionRules.ANVIL_SLAM, OathboundBastionRules.ANVIL_TELL, 24.0F, 0.35F,
					new Vec3(2.6, 122.3, -3.1), STAGE.add(0.0, 1.05, 0.0)),
				new StudioShot("bastion_07_shield_bash", OathboundBastionRules.SHIELD_BASH, OathboundBastionRules.BASH_TELL, 15.0F, 0.75F,
					new Vec3(2.3, 122.6, -3.3), STAGE.add(0.0, 1.25, 0.0))
			);

			for (StudioShot sShot : shots) {
				server.runOnServer(s -> {
					OathboundBastion b = (OathboundBastion) level(s).getEntity(studioId);
					b.snapTo(STAGE.x, STAGE.y, STAGE.z, 180.0F, 0.0F);
					b.poseForPreview(sShot.move(), sShot.tell(), sShot.elapsed(), sShot.barrier());
				});
				director(context, world, sShot.cam(), sShot.look());
				context.waitTicks(8);
				shot(context, sShot.name());
			}
			cut(context);

			// ------------------------------------------------------------------
			// 2. Combat Mechanics, Deflection, Axe Break, Wall Stagger, Persistence & Loot
			// ------------------------------------------------------------------
			server.runOnServer(s -> {
				ServerLevel l = level(s);
				OathboundBastion b = (OathboundBastion) l.getEntity(studioId);
				check(b.getAttributeValue(Attributes.MAX_HEALTH) == OathboundBastionRules.MAX_HEALTH,
					"Bastion max health must be " + OathboundBastionRules.MAX_HEALTH);
				check(b.getAttributeValue(Attributes.ARMOR) == OathboundBastionRules.ARMOR, "Bastion armor must be 18");
				check(b.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == OathboundBastionRules.ARMOR_TOUGHNESS, "Bastion toughness must be 8");
				check(b.getBbHeight() >= 2.4F, "Bastion hitbox height must match 1.25x scale");
				check(FieldGuide.byType("wildercord:oathbound_bastion").isPresent(), "Bastion must be in Field Guide");
				check(BuiltInRegistries.ITEM.getKey(AuraWorld.OATHBOUND_BASTION_SPAWN_EGG).getPath().equals("oathbound_bastion_spawn_egg"),
					"Bastion spawn egg registered");
				b.discard();
			});

			// A) Phalanx Brace: arrow deflection + non-axe reduction + axe shatter -> Staggered
			int combatId = server.computeOnServer(s -> {
				ServerLevel l = level(s);
				ServerPlayer p = player(s);
				p.setGameMode(GameType.SURVIVAL);
				p.setHealth(p.getMaxHealth());
				p.teleportTo(l, 0.5, 121.0, -4.5, Set.<Relative>of(), 0.0F, 0.0F, false);
				OathboundBastion b = spawnBastion(l, STAGE, 180.0F, true);
				b.startMove(l, OathboundBastionRules.PHALANX_BRACE, p);
				return b.getId();
			});

			int arrowId = server.computeOnServer(s -> {
				ServerLevel l = level(s);
				ServerPlayer p = player(s);
				OathboundBastion b = (OathboundBastion) l.getEntity(combatId);
				Arrow arrow = new Arrow(l, p, new ItemStack(Items.ARROW), null);
				arrow.setPos(0.5, 122.2, -1.5);
				arrow.setDeltaMovement(new Vec3(0.0, 0.0, 1.8));
				l.addFreshEntity(arrow);
				b.deflection(arrow);
				check(b.getHealth() == b.getMaxHealth(), "Bastion unhurt by deflected frontal arrow");
				return arrow.getId();
			});
			context.waitTicks(2);
			server.runOnServer(s -> {
				Arrow arrow = (Arrow) level(s).getEntity(arrowId);
				if (arrow != null) {
					check(arrow.getDeltaMovement().z < 0.0 || arrow.getOwner() == level(s).getEntity(combatId),
						"Deflected arrow should reverse away from Bastion");
					arrow.discard();
				}
			});

			server.runOnServer(s -> {
				ServerLevel l = level(s);
				ServerPlayer p = player(s);
				OathboundBastion b = (OathboundBastion) l.getEntity(combatId);
				b.setNoAi(true);
				b.startMove(l, OathboundBastionRules.PHALANX_BRACE, p);
				b.hurtServer(l, l.damageSources().playerAttack(p), 20.0F);
				float nonAxeLost = b.getMaxHealth() - b.getHealth();
				check(nonAxeLost < 6.0F, "Frontal non-axe strike heavily blocked by Phalanx Brace (lost " + nonAxeLost + ")");

				b.setHealth(b.getMaxHealth());
				b.setInvulnerableTime(0);
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_AXE));
				b.hurtServer(l, l.damageSources().playerAttack(p), 12.0F);
				p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				check(b.move() == OathboundBastionRules.STAGGERED, "Frontal axe blow must shatter Phalanx Brace into STAGGERED");
			});

			// B) Shield-Ram into stone wall -> STAGGERED (place player in Creative so AI doesn't turn back during tell)
			server.runCommand("fill -2 121 4 2 124 4 stone");
			server.runOnServer(s -> {
				ServerLevel l = level(s);
				ServerPlayer p = player(s);
				p.setGameMode(GameType.CREATIVE);
				OathboundBastion b = (OathboundBastion) l.getEntity(combatId);
				b.setNoAi(false);
				b.snapTo(0.5, 121.0, 1.5, 0.0F, 0.0F);
				b.setTarget(null);
				b.startMove(l, OathboundBastionRules.SHIELD_RAM, null);
			});
			check(await(context, world, 45, s -> level(s).getEntity(combatId) instanceof OathboundBastion b && b.move() == OathboundBastionRules.STAGGERED),
				"Shield-Ram crashing into a stone wall must trigger STAGGERED");
			server.runCommand("fill -2 121 4 2 124 4 air");

			// C) Anvil Slam shockwave damages & lifts survival player within 5.8 blocks
			server.runOnServer(s -> {
				ServerLevel l = level(s);
				ServerPlayer p = player(s);
				p.setGameMode(GameType.SURVIVAL);
				p.setHealth(p.getMaxHealth());
				p.teleportTo(l, 0.5, 121.0, -3.5, Set.<Relative>of(), 0.0F, 0.0F, false);
				OathboundBastion b = (OathboundBastion) l.getEntity(combatId);
				b.snapTo(STAGE.x, STAGE.y, STAGE.z, 180.0F, 0.0F);
				b.setTarget(p);
				b.startMove(l, OathboundBastionRules.ANVIL_SLAM, p);
			});
			check(await(context, world, 35, s -> player(s).getHealth() < player(s).getMaxHealth()),
				"Anvil Slam shockwave must damage survival player inside radius");

			// D) Shield-Bash damages and slows frontal target
			server.runOnServer(s -> {
				ServerLevel l = level(s);
				ServerPlayer p = player(s);
				p.removeAllEffects();
				p.setHealth(p.getMaxHealth());
				p.setInvulnerableTime(0);
				p.teleportTo(l, 0.5, 121.0, -2.0, Set.<Relative>of(), 0.0F, 0.0F, false);
				OathboundBastion b = (OathboundBastion) l.getEntity(combatId);
				b.snapTo(STAGE.x, STAGE.y, STAGE.z, 180.0F, 0.0F);
				b.setTarget(p);
				b.startMove(l, OathboundBastionRules.SHIELD_BASH, p);
			});
			check(await(context, world, 25, s -> player(s).hasEffect(MobEffects.SLOWNESS) && player(s).getHealth() < player(s).getMaxHealth()),
				"Shield-Bash sweep must damage and slow a frontal target");

			// E) Empowered phase below half health, NBT save/load round-trip, and entity loot table drops
			server.runOnServer(s -> {
				ServerLevel l = level(s);
				ServerPlayer p = player(s);
				OathboundBastion b = (OathboundBastion) l.getEntity(combatId);
				b.setHealth(60.0F);
				b.setTarget(null);
			});
			check(await(context, world, 10, s -> ((OathboundBastion) level(s).getEntity(combatId)).empowered()),
				"Bastion must empower below half health");

			server.runOnServer(s -> {
				ServerLevel l = level(s);
				ServerPlayer p = player(s);
				OathboundBastion b = (OathboundBastion) l.getEntity(combatId);

				var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, s.registryAccess());
				b.saveWithoutId(saved);
				OathboundBastion restored = AuraWorld.OATHBOUND_BASTION.create(l, EntitySpawnReason.LOAD);
				restored.load(TagValueInput.create(ProblemReporter.DISCARDING, s.registryAccess(), saved.buildResult()));
				check(restored.empowered() && restored.getHealth() == b.getHealth(), "Empowered state & health survive save/load");
				check(restored.move() == OathboundBastionRules.IDLE_MARCH, "Reload resets active swing cleanly to IDLE_MARCH");
				restored.discard();

				p.setGameMode(GameType.CREATIVE);
				b.setInvulnerableTime(0);
				b.hurtServer(l, l.damageSources().playerAttack(p), 5000.0F);
			});
			context.waitTicks(4);
			server.runOnServer(s -> {
				ServerLevel l = level(s);
				List<ItemEntity> drops = l.getEntitiesOfClass(ItemEntity.class, new AABB(STAGE, STAGE).inflate(10));
				boolean hasShard = drops.stream().anyMatch(it -> it.getItem().is(AuraWorld.AURA_SHARD));
				boolean hasPage = drops.stream().anyMatch(it -> it.getItem().is(AuraWorld.MANUAL_PAGE));
				check(hasShard && hasPage, "Defeating Oathbound Bastion must drop Aura Shards and Manual Pages");
			});
		}
	}
}
