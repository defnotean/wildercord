package dev.wildercord.trials;

import dev.wildercord.cast.Archivist;
import dev.wildercord.cast.CinderWarden;
import dev.wildercord.cast.DungeonEntities;
import dev.wildercord.cast.WildercordEntities;
import dev.wildercord.ocean.OceanEntities;
import dev.wildercord.ocean.ThalassorBoss;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/**
 * In-game visual showcase & renderer verification for all 3 BedrockGeoRig overhauled bosses:
 * Archivist (Archive plinth), Thalassor (Prismarine deep basin), and Cinder Warden (Forge chamber).
 */
public final class BossOverhaulShowcaseTest implements FabricClientGameTest {
	private static final String CAM_TAG = "wildercord.overhaul_cam";

	private static void check(boolean ok, String why) {
		if (!ok) throw new AssertionError(why);
	}

	private static void poseMob(Mob m, double x, double y, double z, float yaw) {
		m.snapTo(x, y, z, yaw, 0.0F);
		m.setYRot(yaw);
		m.yRotO = yaw;
		m.setYBodyRot(yaw);
		m.yBodyRotO = yaw;
		m.setYHeadRot(yaw);
		m.yHeadRotO = yaw;
		m.setNoAi(true);
		m.setPersistenceRequired();
	}

	private static float yaw(Vec3 from, Vec3 to) {
		Vec3 d = to.subtract(from);
		return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
	}

	private static float pitch(Vec3 from, Vec3 to) {
		Vec3 d = to.subtract(from);
		return (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
	}

	private static void fillBox(ServerLevel l, BlockPos a, BlockPos b, Block block) {
		BlockPos.betweenClosedStream(a, b).forEach(p -> l.setBlock(p, block.defaultBlockState(), 2));
	}

	private static void setupDirector(ClientGameTestContext c, TestSingleplayerContext world, BlockPos anchor, Vec3 eye, Vec3 target) {
		int camId = world.getServer().computeOnServer(server -> {
			ServerLevel l = server.overworld();
			ServerPlayer sp = server.getPlayerList().getPlayers().getFirst();
			sp.teleportTo(l, eye.x, eye.y, eye.z, Set.<Relative>of(), yaw(eye, target), pitch(eye, target), false);
			l.getEntitiesOfClass(Display.TextDisplay.class, new AABB(anchor).inflate(48),
				e -> e.entityTags().contains(CAM_TAG)).forEach(Entity::discard);
			Display.TextDisplay cam = EntityTypes.TEXT_DISPLAY.create(l, EntitySpawnReason.COMMAND);
			cam.snapTo(eye.x, eye.y, eye.z, yaw(eye, target), pitch(eye, target));
			cam.addTag(CAM_TAG);
			l.addFreshEntity(cam);
			return cam.getId();
		});
		c.waitTicks(4);
		c.runOnClient(mc -> {
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
		c.waitTicks(6);
	}

	private static void shot(ClientGameTestContext c, String name) {
		c.runOnClient(mc -> mc.gui.toastManager().clear());
		c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	@Override
	public void runTest(ClientGameTestContext c) {
		try (var world = c.worldBuilder().create()) {
			c.waitTicks(25);
			for (String rule : List.of("advance_time false", "advance_weather false", "spawn_mobs false")) {
				world.getServer().runCommand("gamerule " + rule);
			}
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			c.runOnClient(mc -> {
				mc.getWindow().setWindowed(1280, 720);
				mc.options.renderDistance().set(12);
				mc.options.fov().set(68);
				mc.options.broadcastOptions();
				mc.resizeGui();
			});

			// 1. Archivist showcase stage at (0, 96, 0)
			BlockPos archPos = new BlockPos(0, 96, 0);
			world.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				fillBox(l, archPos.offset(-8, -1, -8), archPos.offset(8, -1, 8), Blocks.DEEPSLATE_TILES);
				fillBox(l, archPos.offset(-8, 0, -8), archPos.offset(8, 8, 8), Blocks.AIR);
				fillBox(l, archPos.offset(-8, 0, -8), archPos.offset(8, 6, -8), Blocks.CHISELED_DEEPSLATE);
				for (int ox = -4; ox <= 4; ox += 4) {
					l.setBlock(archPos.offset(ox, 0, -6), Blocks.SOUL_LANTERN.defaultBlockState(), 2);
					l.setBlock(archPos.offset(ox, 0, 2), Blocks.SEA_LANTERN.defaultBlockState(), 2);
				}
				l.setBlock(archPos.offset(2, 0, 1), Blocks.BOOKSHELF.defaultBlockState(), 2);
				l.setBlock(archPos.offset(-2, 0, 1), Blocks.BOOKSHELF.defaultBlockState(), 2);
				Archivist archivist = WildercordEntities.ARCHIVIST.create(l, EntitySpawnReason.COMMAND);
				check(archivist != null, "Created Archivist");
				poseMob(archivist, archPos.getX() + 0.5, archPos.getY(), archPos.getZ() + 0.5, 0.0F);
				check(l.addFreshEntity(archivist), "Spawned Archivist");
			});
			setupDirector(c, world, archPos,
				new Vec3(archPos.getX() - 2.3, archPos.getY() + 2.0, archPos.getZ() + 4.4),
				new Vec3(archPos.getX() + 0.5, archPos.getY() + 1.35, archPos.getZ() + 0.5));
			shot(c, "overhaul_archivist");

			// 2. Cinder Warden showcase stage at (64, 96, 0)
			BlockPos cinderPos = new BlockPos(64, 96, 0);
			world.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				fillBox(l, cinderPos.offset(-8, -1, -8), cinderPos.offset(8, -1, 8), Blocks.POLISHED_BASALT);
				fillBox(l, cinderPos.offset(-8, 0, -8), cinderPos.offset(8, 9, 8), Blocks.AIR);
				fillBox(l, cinderPos.offset(-8, 0, -8), cinderPos.offset(8, 7, -8), Blocks.BLACKSTONE_BRICKS);
				l.setBlock(cinderPos.offset(-4, 0, -3), Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
				l.setBlock(cinderPos.offset(4, 0, -3), Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
				l.setBlock(cinderPos.offset(-4, 0, 3), Blocks.SHROOMLIGHT.defaultBlockState(), 2);
				l.setBlock(cinderPos.offset(4, 0, 3), Blocks.SHROOMLIGHT.defaultBlockState(), 2);
				CinderWarden warden = DungeonEntities.CINDER_WARDEN.create(l, EntitySpawnReason.COMMAND);
				check(warden != null, "Created Cinder Warden");
				poseMob(warden, cinderPos.getX() + 0.5, cinderPos.getY(), cinderPos.getZ() + 0.5, 0.0F);
				check(l.addFreshEntity(warden), "Spawned Cinder Warden");
			});
			setupDirector(c, world, cinderPos,
				new Vec3(cinderPos.getX() - 2.9, cinderPos.getY() + 2.4, cinderPos.getZ() + 5.2),
				new Vec3(cinderPos.getX() + 0.5, cinderPos.getY() + 1.7, cinderPos.getZ() + 0.5));
			shot(c, "overhaul_cinder_warden");

			// 3. Thalassor deep basin stage at (128, 86, 0)
			BlockPos thalPos = new BlockPos(128, 86, 0);
			world.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				fillBox(l, thalPos.offset(-10, -2, -10), thalPos.offset(10, -2, 10), Blocks.DARK_PRISMARINE);
				fillBox(l, thalPos.offset(-10, -1, -10), thalPos.offset(10, 7, 10), Blocks.WATER);
				for (int ox = -8; ox <= 8; ox += 4) {
					l.setBlock(thalPos.offset(ox, -1, -8), Blocks.SEA_LANTERN.defaultBlockState(), 2);
					l.setBlock(thalPos.offset(ox, -1, 8), Blocks.SEA_LANTERN.defaultBlockState(), 2);
				}
				ServerPlayer sp = s.getPlayerList().getPlayers().getFirst();
				sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
				sp.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 400, 0, false, false));
				ThalassorBoss thal = OceanEntities.THALASSOR.create(l, EntitySpawnReason.COMMAND);
				check(thal != null, "Created Thalassor");
				poseMob(thal, thalPos.getX() + 0.5, thalPos.getY() + 1.4, thalPos.getZ() + 0.5, 0.0F);
				check(l.addFreshEntity(thal), "Spawned Thalassor");
			});
			setupDirector(c, world, thalPos,
				new Vec3(thalPos.getX() - 4.2, thalPos.getY() + 3.0, thalPos.getZ() + 6.4),
				new Vec3(thalPos.getX() + 0.5, thalPos.getY() + 1.7, thalPos.getZ() + 0.5));
			shot(c, "overhaul_thalassor");

			// Restore camera and HUD cleanly
			c.runOnClient(mc -> {
				if (mc.player != null) {
					mc.setCameraEntity(mc.player);
				}
				if (mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
			world.getServer().runOnServer(s -> s.overworld().getEntitiesOfClass(Display.TextDisplay.class,
				new AABB(thalPos).inflate(200), e -> e.entityTags().contains(CAM_TAG)).forEach(Entity::discard));
		}
	}
}
