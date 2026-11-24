package dev.wildercord.wildlife;

import dev.wildercord.gametest.compat.GametestCompat;
import dev.wildercord.aura.world.Guardians;
import dev.wildercord.cast.DungeonEntities;
import dev.wildercord.cast.WildercordEntities;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.world.cartographer.CartographerContent;
import dev.wildercord.world.ferryman.FerrymanContent;
import dev.wildercord.world.hollowbell.HollowBellContent;
import dev.wildercord.world.kilnwidow.KilnWidowContent;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * End-to-end native singleplayer verification of the forest White Rabbit and its Hollow Ancient
 * Tree burrow portals: leading the player to the hollow tree, diving inside, and teleporting
 * cleanly to (1) a random cave, (2) a stocked treasure vault, and (3) all 17 Wildercord bosses
 * at once (plus clean return via the Return Burrow Portal).
 */
public final class WhiteRabbitHoleTest implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 100, 0);
	private static final String CAM_TAG = "wildercord.white_rabbit_cam";

	private static void check(boolean cond, String msg) {
		if (!cond) {
			throw new AssertionError(msg);
		}
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
			ServerLevel l = server.overworld();
			l.getEntitiesOfClass(Display.TextDisplay.class, new AABB(STAGE).inflate(260),
				e -> e.getTags().contains(CAM_TAG)).forEach(Entity::discard);
			Display.TextDisplay cam = dev.wildercord.compat.McEntities.create(net.minecraft.world.entity.EntityType.TEXT_DISPLAY, l, MobSpawnType.COMMAND);
			cam.moveTo(eye.x, eye.y, eye.z, yaw(eye, target), pitch(eye, target));
			cam.addTag(CAM_TAG);
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
		context.waitTicks(3);
	}

	private static void cut(ClientGameTestContext context, TestSingleplayerContext world) {
		context.runOnClient(mc -> mc.setCameraEntity(mc.player));
		world.getServer().runOnServer(s -> s.overworld()
			.getEntitiesOfClass(Display.TextDisplay.class, new AABB(STAGE).inflate(260),
				e -> e.getTags().contains(CAM_TAG))
			.forEach(Entity::discard));
		context.waitTicks(2);
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> {
			dev.wildercord.gametest.compat.GametestCompat.clearToasts(mc);
			dev.wildercord.gametest.compat.GametestCompat.clearChat(mc);
		});
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static BlockPos findReturnPortalNear(ServerLevel level, BlockPos center, int radius) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -3; dy <= 5; dy++) {
				for (int dz = -radius; dz <= radius; dz++) {
					BlockPos p = center.offset(dx, dy, dz);
					if (level.getBlockState(p).is(WhiteRabbitContent.RABBIT_HOLE_PORTAL)
						&& level.getBlockEntity(p) instanceof RabbitHolePortalBlockEntity be
						&& be.isReturnPortal()) {
						return p;
					}
				}
			}
		}
		return null;
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder()
				.setUseConsistentSettings(false)
				.adjustSettings(settings -> settings.setSeed("20261123"))
				.create()) {
			context.waitTicks(25);
			for (String rule : List.of("advance_time false", "advance_weather false", "spawn_mobs false")) {
				world.getServer().runCommand("gamerule " + rule);
			}
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("time set 4000");
			world.getServer().runCommand("weather clear");

			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1280, 720);
				mc.options.renderDistance().set(10);
				mc.options.fov().set(74);
				mc.options.broadcastOptions();
				GametestCompat.resizeGui(mc);
				if (!dev.wildercord.gametest.compat.GametestCompat.hudHidden(mc)) dev.wildercord.gametest.compat.GametestCompat.toggleHud(mc);
			});

			// Build a flat oak forest clearing at y=100 for the White Rabbit & Hollow Tree.
			BlockPos treeBase = new BlockPos(18, 101, 0);
			UUID rabbitUuid = world.getServer().computeOnServer(server -> {
				ServerLevel l = server.overworld();
				for (int x = -8; x <= 28; x++) {
					for (int z = -28; z <= 28; z++) {
						l.setBlock(new BlockPos(x, 100, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
						for (int y = 101; y <= 114; y++) {
							l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
						}
					}
				}
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.SURVIVAL);
				p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 12000, 0, false, false));
				p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 12000, 4, false, false));
				p.teleportTo(l, 0.5, 101.0, 0.5, Set.<Relative>of(), -90.0F, 0.0F, false);

				WhiteRabbit rabbit = dev.wildercord.compat.McEntities.create(WhiteRabbitContent.WHITE_RABBIT, l, MobSpawnType.COMMAND);
				check(rabbit != null, "Create WhiteRabbit");
				rabbit.moveTo(3.5, 101.0, 0.5, -90.0F, 0.0F);
				l.addFreshEntity(rabbit);
				BlockPos portal = rabbit.leadToHollowTree(p, treeBase, WhiteRabbitRules.Outcome.RANDOM_CAVE);
				check(l.getBlockState(portal).is(WhiteRabbitContent.RABBIT_HOLE_PORTAL),
					"Hollow Ancient Tree has Rabbit-Hole Portal at trunk center");
				return rabbit.getUUID();
			});

			// Let rabbit bound partway toward the hollow tree while player keeps pace behind it.
			for (int i = 0; i < 20; i++) {
				context.waitTicks(4);
				world.getServer().runOnServer(server -> {
					ServerLevel l = server.overworld();
					if (l.getEntity(rabbitUuid) instanceof WhiteRabbit r && r.isAlive()) {
						ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
						if (r.getX() - p.getX() > 5.0) {
							p.teleportTo(l, r.getX() - 4.0, 101.0, r.getZ(), Set.<Relative>of(), -90.0F, 0.0F, false);
						}
					}
				});
			}

			Vec3[] rabbitAndTree = world.getServer().computeOnServer(server -> {
				ServerLevel l = server.overworld();
				Entity r = l.getEntity(rabbitUuid);
				Vec3 rp = r != null ? r.position() : new Vec3(12.0, 101.0, 0.5);
				return new Vec3[] { rp, Vec3.atCenterOf(treeBase) };
			});
			Vec3 camEye = new Vec3(rabbitAndTree[0].x - 3.6, 103.0, rabbitAndTree[0].z + 4.4);
			director(context, world, camEye, rabbitAndTree[0].lerp(rabbitAndTree[1], 0.55).add(0, 0.8, 0));
			shot(context, "white_rabbit_leading_to_tree");
			cut(context, world);

			// Bring follower right to tree doorway and confirm White Rabbit dives into the hole.
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.teleportTo(l, 14.5, 101.0, 0.5, Set.<Relative>of(), -90.0F, 0.0F, false);
				if (l.getEntity(rabbitUuid) instanceof WhiteRabbit r && r.isAlive()) {
					r.teleportTo(16.2, 101.0, 0.5);
				}
			});
			boolean rabbitDived = false;
			for (int i = 0; i < 25; i++) {
				context.waitTicks(3);
				if (world.getServer().computeOnServer(s -> s.overworld().getEntity(rabbitUuid) == null)) {
					rabbitDived = true;
					break;
				}
			}
			check(rabbitDived, "White Rabbit must dive into the Hollow Tree portal hole when player follows it");

			// 1. Enter RANDOM_CAVE portal -> verify underground cavern + Return Burrow Portal -> return to tree.
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				RabbitHolePortalBlockEntity.clearCooldowns();
				RabbitHolePortalBlockEntity be = (RabbitHolePortalBlockEntity) l.getBlockEntity(treeBase);
				check(be != null && be.outcome() == WhiteRabbitRules.Outcome.RANDOM_CAVE, "Expected RANDOM_CAVE portal");
				be.enter(l, p);
			});
			context.waitTicks(6);
			BlockPos caveSpot = world.getServer().computeOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				check(p.getY() < 40.0, "RANDOM_CAVE portal must teleport player underground, Y=" + p.getY());
				BlockPos ret = findReturnPortalNear(l, p.blockPosition(), 12);
				check(ret != null, "RANDOM_CAVE must contain a Return Burrow Portal");
				return p.blockPosition();
			});
			director(context, world, Vec3.atCenterOf(caveSpot).add(1.8, 1.4, -1.8), Vec3.atCenterOf(caveSpot).add(0, 0.5, 2.0));
			shot(context, "white_rabbit_outcome_cave");
			cut(context, world);

			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				BlockPos ret = findReturnPortalNear(l, p.blockPosition(), 12);
				RabbitHolePortalBlockEntity.clearCooldowns();
				((RabbitHolePortalBlockEntity) l.getBlockEntity(ret)).enter(l, p);
				check(p.getY() >= 99.0, "Return portal returns player cleanly to surface forest");
			});
			context.waitTicks(4);

			// 2. TREASURE_VAULT outcome -> verify Root-Vault, stocked chests, screenshot, and clean return.
			BlockPos vaultTree = new BlockPos(18, 101, -18);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				HollowTreeBuilder.placeHollowTree(l, vaultTree, Direction.WEST, WhiteRabbitRules.Outcome.TREASURE_VAULT);
				RabbitHolePortalBlockEntity.clearCooldowns();
				((RabbitHolePortalBlockEntity) l.getBlockEntity(vaultTree)).enter(l, p);
			});
			context.waitTicks(6);
			BlockPos vaultSpot = world.getServer().computeOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				check(p.getY() < 0.0, "TREASURE_VAULT must teleport player underground, Y=" + p.getY());
				int filledChests = 0;
				boolean hasManaCrystal = false;
				for (int dx = -8; dx <= 8; dx++) {
					for (int dy = -2; dy <= 4; dy++) {
						for (int dz = -8; dz <= 8; dz++) {
							BlockPos cp = p.blockPosition().offset(dx, dy, dz);
							if (l.getBlockEntity(cp) instanceof ChestBlockEntity chest) {
								filledChests++;
								for (int slot = 0; slot < chest.getContainerSize(); slot++) {
									ItemStack stack = chest.getItem(slot);
									if (!stack.isEmpty() && stack.is(WildercordItems.MANA_CRYSTAL)) {
										hasManaCrystal = true;
									}
								}
							}
						}
					}
				}
				check(filledChests >= 3 && hasManaCrystal, "Treasure Vault must hold >=3 chests stocked with Mana Crystals");
				return p.blockPosition();
			});
			director(context, world, Vec3.atCenterOf(vaultSpot).add(3.5, 2.2, 2.5), Vec3.atCenterOf(vaultSpot).add(0, 0.8, -5.0));
			shot(context, "white_rabbit_outcome_treasure");
			cut(context, world);

			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				BlockPos ret = findReturnPortalNear(l, p.blockPosition(), 12);
				check(ret != null, "Treasure Vault must include Return Burrow Portal");
				RabbitHolePortalBlockEntity.clearCooldowns();
				((RabbitHolePortalBlockEntity) l.getBlockEntity(ret)).enter(l, p);
				check(p.getY() >= 99.0, "Return portal returns player cleanly from Treasure Vault");
			});
			context.waitTicks(4);

			// 3. ALL_BOSSES outcome -> verify Grand Colosseum + all 17 distinct Wildercord bosses alive & tagged!
			BlockPos bossTree = new BlockPos(18, 101, 18);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				HollowTreeBuilder.placeHollowTree(l, bossTree, Direction.WEST, WhiteRabbitRules.Outcome.ALL_BOSSES);
				RabbitHolePortalBlockEntity.clearCooldowns();
				((RabbitHolePortalBlockEntity) l.getBlockEntity(bossTree)).enter(l, p);
			});
			context.waitTicks(22);

			List<EntityType<?>> expectedBosses = List.of(
				Guardians.SERPENT, Guardians.LANTERN_BEARER, Guardians.HOLLOW_MATRON, Guardians.ASHEN_STAG,
				Guardians.FROST_WYRMLING, Guardians.MOJITO, Guardians.BELL_WARDEN,
				DungeonEntities.CINDER_WARDEN, DungeonEntities.ROOT_GUARDIAN, DungeonEntities.STAR_EATER,
				DungeonEntities.STORM_CONDUCTOR, DungeonEntities.TIDE_SCRIBE, WildercordEntities.ARCHIVIST,
				FerrymanContent.FERRYMAN, CartographerContent.CARTOGRAPHER, HollowBellContent.BELL, KilnWidowContent.WIDOW
			);
			BlockPos bossSpot = world.getServer().computeOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				check(p.getY() < 0.0, "ALL_BOSSES must teleport player to subterranean Colosseum");
				AABB arenaArea = new AABB(p.blockPosition()).inflate(40.0);
				for (EntityType<?> bossType : expectedBosses) {
					List<Mob> found = l.getEntitiesOfClass(Mob.class, arenaArea,
						m -> m.getType() == bossType && m.isAlive() && m.getTags().contains(WhiteRabbitRules.BOSS_RUSH_TAG));
					check(!found.isEmpty(), "Missing live tagged boss-rush entity: " + bossType.getDescription().getString());
				}
				return p.blockPosition();
			});

			director(context, world,
				Vec3.atCenterOf(bossSpot).add(2.0, 9.5, 4.0),
				Vec3.atCenterOf(bossSpot).add(0.0, 2.0, -16.0));
			shot(context, "white_rabbit_outcome_all_bosses");
			cut(context, world);

			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				BlockPos ret = findReturnPortalNear(l, p.blockPosition(), 16);
				check(ret != null, "Boss-Rush Colosseum must include Return Burrow Portal");
				RabbitHolePortalBlockEntity.clearCooldowns();
				((RabbitHolePortalBlockEntity) l.getBlockEntity(ret)).enter(l, p);
				check(p.getY() >= 99.0, "Return portal returns player cleanly from Colosseum");
			});
			context.waitTicks(4);
		}
	}
}
