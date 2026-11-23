package dev.wildercord.ocean;

import dev.wildercord.gametest.compat.GametestCompat;
import dev.wildercord.aura.world.BossDamage;
import dev.wildercord.client.fx.CustomMobFireFx;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Native client + server verification of all 9 Deep Ocean entities, 3 Brine-Merchant professions,
 * Lumi-Seahorse riding & burst dash, Pearl-Pup taming & treasure sniffing, Singing Moon-Whale song
 * & ambergris, Thalassor phases & Sovereign Altar respawn, Abyssal Diving Suit O2 & Conduit Power,
 * Harpoon/Glaive/Conch/Siren-Pearl Lantern mechanics, Trench Anvil forging & repair, and 1:1 visuals.
 */
public final class DeepOceanEntitiesAndGearTest implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 90, 0);
	private static final String CAM_TAG = "wildercord.ocean_cam";

	private static void check(boolean cond, String msg) {
		if (!cond) throw new AssertionError(msg);
	}

	private static void poseMob(net.minecraft.world.entity.Mob m, double x, double y, double z, float yaw) {
		m.snapTo(x, y, z, yaw, 0f);
		m.setYRot(yaw);
		m.yRotO = yaw;
		m.setYBodyRot(yaw);
		m.yBodyRotO = yaw;
		m.setYHeadRot(yaw);
		m.yHeadRotO = yaw;
		m.setNoAi(true);
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
			l.getEntitiesOfClass(Display.TextDisplay.class, new AABB(STAGE).inflate(96),
				e -> e.getTags().contains(CAM_TAG)).forEach(Entity::discard);
			Display.TextDisplay cam = EntityType.TEXT_DISPLAY.create(l, EntitySpawnReason.COMMAND);
			cam.snapTo(eye.x, eye.y, eye.z, yaw(eye, target), pitch(eye, target));
			cam.addTag(CAM_TAG);
			l.addFreshEntity(cam);
			return cam.getId();
		});
		context.waitTicks(4);
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
		context.waitTicks(6);
	}

	private static void directorUnderwater(ClientGameTestContext context, TestSingleplayerContext world, Vec3 eye, Vec3 target) {
		world.getServer().runOnServer(server -> {
			ServerLevel l = server.overworld();
			ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
			p.setGameMode(GameType.SPECTATOR);
			p.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 400, 0, false, false, false));
			p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false, false));
			p.teleportTo(l, eye.x, eye.y, eye.z, Set.<Relative>of(), yaw(eye, target), pitch(eye, target), false);
		});
		context.waitTicks(4);
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			mc.setCameraEntity(mc.player);
			if (!dev.wildercord.gametest.compat.GametestCompat.hudHidden(mc)) {
				dev.wildercord.gametest.compat.GametestCompat.toggleHud(mc);
			}
			dev.wildercord.gametest.compat.GametestCompat.clearToasts(mc);
			dev.wildercord.gametest.compat.GametestCompat.clearChat(mc);
		});
		context.waitTicks(8);
	}

	private static void cut(ClientGameTestContext context, TestSingleplayerContext world) {
		context.runOnClient(mc -> mc.setCameraEntity(mc.player));
		world.getServer().runOnServer(s -> {
			s.overworld().getEntitiesOfClass(Display.TextDisplay.class, new AABB(STAGE).inflate(96),
				e -> e.getTags().contains(CAM_TAG)).forEach(Entity::discard);
			ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
			p.removeAllEffects();
		});
		context.waitTicks(2);
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> {
			dev.wildercord.gametest.compat.GametestCompat.clearToasts(mc);
			dev.wildercord.gametest.compat.GametestCompat.clearChat(mc);
		});
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static void buildAquariumStage(ServerLevel l) {
		// Solid non-falling base at y=88..89 so water never leaks; barrier front wall at z=9 eliminates glass streaks.
		for (int x = -16; x <= 16; x++) {
			for (int z = -9; z <= 20; z++) {
				l.setBlock(new BlockPos(x, 88, z), Blocks.STONE.defaultBlockState(), 2);
				for (int y = 90; y <= 108; y++) {
					l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
				}
				boolean inTank = Math.abs(x) <= 13 && z >= -7 && z <= 9;
				if (inTank) {
					boolean sideWall = Math.abs(x) == 13 || z == -7;
					boolean frontBarrier = z == 9 && Math.abs(x) < 13;
					boolean lamp = Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 0;
					l.setBlock(new BlockPos(x, 89, z),
						(lamp ? Blocks.SEA_LANTERN : (sideWall ? Blocks.DARK_PRISMARINE : Blocks.SMOOTH_SANDSTONE)).defaultBlockState(), 2);
					for (int y = 90; y <= 98; y++) {
						if (sideWall) {
							boolean band = (y == 92 || y == 96);
							l.setBlock(new BlockPos(x, y, z),
								(band ? Blocks.SEA_LANTERN : Blocks.PRISMARINE_BRICKS).defaultBlockState(), 2);
						} else if (frontBarrier) {
							l.setBlock(new BlockPos(x, y, z), Blocks.BARRIER.defaultBlockState(), 2);
						} else {
							l.setBlock(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState(), 2);
						}
					}
				} else {
					boolean trim = Math.floorMod(x + z, 5) == 0;
					l.setBlock(new BlockPos(x, 89, z),
						(trim ? Blocks.PRISMARINE_BRICKS : Blocks.DARK_PRISMARINE).defaultBlockState(), 2);
				}
			}
		}
	}

	private static void clearMobs(ServerLevel l) {
		l.getEntitiesOfClass(Entity.class, new AABB(STAGE).inflate(40), e -> !(e instanceof ServerPlayer))
			.forEach(Entity::discard);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder()
				.setUseConsistentSettings(false)
				.adjustSettings(s -> s.setSeed("20261124"))
				.create()) {
			context.waitTicks(25);
			for (String rule : List.of("advance_time false", "advance_weather false", "spawn_mobs false")) {
				world.getServer().runCommand("gamerule " + rule);
			}
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");

			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1280, 720);
				mc.options.renderDistance().set(10);
				mc.options.fov().set(72);
				mc.options.broadcastOptions();
				GametestCompat.resizeGui(mc);
			});

			// Build Aquarium Stage first and wait for client chunk meshing before spawning & shooting
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				buildAquariumStage(l);
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.CREATIVE);
				p.teleportTo(l, 0.5, 90.0, 14.0, Set.<Relative>of(), 180f, 0f, false);
			});
			context.waitTicks(35);

			// Part 1: Enemies + Thalassor Boss + Sovereign Altar
			UUID[] ids = new UUID[5];
			BlockPos altarPos = new BlockPos(0, 90, -1);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				l.setBlock(altarPos, OceanBlocks.SOVEREIGN_ALTAR.defaultBlockState(), 3);

				BarnacleHulkDredger hulk = OceanEntities.BARNACLE_HULK_DREDGER.create(l, EntitySpawnReason.COMMAND);
				poseMob(hulk, -5.8, 90.0, 1.4, -22f);
				hulk.setPersistenceRequired();
				l.addFreshEntity(hulk);

				SirenVeilJelly jelly = OceanEntities.SIREN_VEIL_JELLY.create(l, EntitySpawnReason.COMMAND);
				poseMob(jelly, -2.8, 93.0, 1.6, -12f);
				l.addFreshEntity(jelly);

				CoralSpineMako mako = OceanEntities.CORAL_SPINE_MAKO.create(l, EntitySpawnReason.COMMAND);
				poseMob(mako, 2.7, 92.2, 1.5, 58f);
				l.addFreshEntity(mako);

				DreadNautilusSentinel nautilus = OceanEntities.DREAD_NAUTILUS_SENTINEL.create(l, EntitySpawnReason.COMMAND);
				poseMob(nautilus, 5.8, 92.2, 1.4, 38f);
				l.addFreshEntity(nautilus);

				ThalassorBoss thalassor = OceanEntities.THALASSOR.create(l, EntitySpawnReason.EVENT);
				poseMob(thalassor, -0.4, 94.3, -2.2, 70f);
				thalassor.setArenaCenter(altarPos);
				l.addFreshEntity(thalassor);
				if (l.getBlockEntity(altarPos) instanceof SovereignAltarBlockEntity altar) {
					altar.bindExistingBoss(thalassor);
				}

				check(hulk.getMaxHealth() == OceanRules.BARNACLE_HULK_HP, "Barnacle Hulk HP");
				check(jelly.getMaxHealth() == OceanRules.SIREN_JELLY_HP, "Siren Jelly HP");
				check(mako.getMaxHealth() == OceanRules.CORAL_MAKO_HP, "Coral Mako HP");
				check(nautilus.getMaxHealth() == OceanRules.DREAD_NAUTILUS_HP, "Dread Nautilus HP");
				check(thalassor.getMaxHealth() == OceanRules.THALASSOR_MAX_HP, "Thalassor HP == 2400");
				check(BossDamage.boss(thalassor), "Thalassor is recognized by BossDamage");
				check(thalassor.fireImmune(), "Thalassor is fireImmune");
				check(CustomMobFireFx.isCustomMob(hulk), "Custom ocean mobs use perimeter embers instead of vanilla fire overlay");

				ids[0] = hulk.getUUID();
				ids[1] = jelly.getUUID();
				ids[2] = mako.getUUID();
				ids[3] = nautilus.getUUID();
				ids[4] = thalassor.getUUID();
			});

			directorUnderwater(context, world, new Vec3(0.0, 92.6, 8.4), new Vec3(0.0, 92.2, 0.0));
			shot(context, "ocean_live_enemies_and_thalassor");
			cut(context, world);

			// Exercise Thalassor phases & Sovereign Altar re-summon logic
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.CREATIVE);
				ThalassorBoss boss = (ThalassorBoss) l.getEntity(ids[4]);
				check(OceanRules.thalassorPhase(boss.getHealth(), boss.getMaxHealth()) == 1, "Phase 1 at full HP");
				boss.setHealth(1200.0f);
				check(OceanRules.thalassorPhase(boss.getHealth(), boss.getMaxHealth()) == 2, "Phase 2 at 50% HP");
				boss.setHealth(500.0f);
				check(OceanRules.thalassorPhase(boss.getHealth(), boss.getMaxHealth()) == 3, "Phase 3 below 33% HP");
				boss.discard();

				SovereignAltarBlockEntity altar = (SovereignAltarBlockEntity) l.getBlockEntity(altarPos);
				check(altar != null, "Sovereign Altar block entity missing");
				SovereignAltarBlockEntity.serverTick(l, altarPos, l.getBlockState(altarPos), altar);
				check(altar.getRespawnTicksRemaining() == OceanRules.THALASSOR_RESPAWN_TICKS, "Altar starts respawn timer");
				InteractionResult res = altar.interact(p, new ItemStack(OceanItems.LEVIATHAN_HEART_PEARL));
				check(res.consumesAction() || res == InteractionResult.SUCCESS, "Offering summoned boss");
				check(altar.getBossUuid() != null && l.getEntity(altar.getBossUuid()) instanceof ThalassorBoss, "Fresh Thalassor bound to altar");
			});

			// Part 2: Friendlies, 3 Brine-Merchants, Lumi-Seahorse, Pearl-Pup & Singing Moon-Whale
			UUID[] friendlyIds = new UUID[6];
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				clearMobs(l);
				l.setBlock(altarPos, Blocks.WATER.defaultBlockState(), 3);
				l.setBlock(new BlockPos(-4, 90, 0), Blocks.CHEST.defaultBlockState(), 3);

				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.SURVIVAL);
				p.teleportTo(l, 0.5, 90.0, 14.0, Set.<Relative>of(), 180f, 0f, false);

				OceanRules.MerchantProfession[] profs = OceanRules.MerchantProfession.values();
				for (int i = 0; i < 3; i++) {
					BrineMerchant m = OceanEntities.BRINE_MERCHANT.create(l, EntitySpawnReason.COMMAND);
					m.setMerchantProfession(profs[i]);
					poseMob(m, -5.2 + i * 1.9, 90.0, 1.8, (i - 1) * -14f);
					l.addFreshEntity(m);
					check(m.canBreatheUnderwater(), "Brine-Merchant breathes underwater");
					check(!m.getOffers().isEmpty(), "Brine-Merchant has trade offers for " + profs[i]);
					friendlyIds[i] = m.getUUID();
				}

				PearlPup pup = OceanEntities.PEARL_PUP.create(l, EntitySpawnReason.COMMAND);
				poseMob(pup, 0.3, 90.0, 1.8, -14f);
				pup.tame(p);
				l.addFreshEntity(pup);
				friendlyIds[4] = pup.getUUID();

				LumiSeahorse seahorse = OceanEntities.LUMI_SEAHORSE.create(l, EntitySpawnReason.COMMAND);
				poseMob(seahorse, 2.5, 91.6, 2.0, 80f);
				l.addFreshEntity(seahorse);
				seahorse.tameWithName(p);
				seahorse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
				friendlyIds[3] = seahorse.getUUID();

				SingingMoonWhale whale = OceanEntities.SINGING_MOON_WHALE.create(l, EntitySpawnReason.COMMAND);
				poseMob(whale, 2.8, 94.0, -2.0, 64f);
				whale.setSinging(true);
				l.addFreshEntity(whale);
				check(whale.getMaxHealth() == OceanRules.MOON_WHALE_HP, "Moon-Whale HP == 220");
				friendlyIds[5] = whale.getUUID();
			});

			// Let taming heart particles fade cleanly before hero screenshot
			context.waitTicks(45);
			directorUnderwater(context, world, new Vec3(0.0, 92.4, 8.4), new Vec3(0.0, 91.6, 0.5));
			shot(context, "ocean_live_friendlies_and_whale");
			cut(context, world);

			// Exercise Lumi-Seahorse ride+dash, Pearl-Pup treasure sniff, Singing Moon-Whale song+ambergris
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.SURVIVAL);
				p.teleportTo(l, 2.0, 91.0, 3.0, Set.<Relative>of(), 180f, 0f, false);
				PearlPup pup = (PearlPup) l.getEntity(friendlyIds[4]);
				if (pup != null) pup.setNoAi(false);
				LumiSeahorse seahorse = (LumiSeahorse) l.getEntity(friendlyIds[3]);
				seahorse.setNoAi(false);
				check(p.startRiding(seahorse), "Player mounts Lumi-Seahorse");
			});
			context.waitTicks(3);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				LumiSeahorse seahorse = (LumiSeahorse) l.getEntity(friendlyIds[3]);
				check(seahorse.isInWater(), "Lumi-Seahorse is submerged in water");
				seahorse.onPlayerJump(100);
				check(seahorse.getDashTicks() > 0 && seahorse.getDeltaMovement().lengthSqr() > 0.2,
					"Lumi-Seahorse dash impulse triggers underwater");
				p.stopRiding();

				SingingMoonWhale whale = (SingingMoonWhale) l.getEntity(friendlyIds[5]);
				p.teleportTo(l, whale.getX(), whale.getY(), whale.getZ() + 2.5, Set.<Relative>of(), 180f, 0f, false);
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COD));
				whale.mobInteract(p, InteractionHand.MAIN_HAND);
				check(whale.isSinging(), "Moon-Whale sings when fed fish");
				check(p.hasEffect(MobEffects.WATER_BREATHING) && p.hasEffect(MobEffects.DOLPHINS_GRACE),
					"Moon-Whale song grants buffs to diver");
				List<ItemEntity> drops = l.getEntitiesOfClass(ItemEntity.class, whale.getBoundingBox().inflate(4.0),
					ie -> ie.getItem().is(OceanItems.STAR_BARNACLE_AMBERGRIS));
				check(!drops.isEmpty(), "Moon-Whale sheds Star-Barnacle Ambergris");
			});
			context.waitTicks(65);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				PearlPup pup = (PearlPup) l.getEntity(friendlyIds[4]);
				check(pup != null && pup.isTame() && pup.isSniffing(), "Tamed Pearl-Pup sniffed out sunken treasure chest");
			});

			// Part 3: Full Abyssal Diving Suit, O2 Reserve, Weapons, Conch Air Dome & Visual Hero Shot
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				clearMobs(l);
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.removeAllEffects();
				p.setGameMode(GameType.SURVIVAL);
				p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(OceanItems.ABYSSAL_DIVING_HELM));
				p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(OceanItems.BATHYSPHERE_CHESTPLATE));
				p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(OceanItems.KELP_WEAVE_GREAVES));
				p.setItemSlot(EquipmentSlot.FEET, new ItemStack(OceanItems.TRENCH_BOOTS));
				p.setAttached(OceanItems.TANK_OXYGEN, OceanRules.CHEST_OXYGEN_MAX);
				p.teleportTo(l, 0.5, 92.0, 2.0, Set.<Relative>of(), 180f, -5f, false);
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OceanItems.LEVIATHAN_GLAIVE));
				p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(OceanItems.SIREN_PEARL_LANTERN));

				check(OceanItems.wornSuitPieces(p) == 4, "All 4 Abyssal Diving Suit pieces equipped");
			});
			context.waitTicks(20);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				check(p.hasEffect(MobEffects.CONDUIT_POWER), "Full suit grants Conduit Power");
				check(p.hasEffect(MobEffects.NIGHT_VISION), "Abyssal Helm grants submerged Night Vision");
				check(p.getAirSupply() == p.getMaxAirSupply(), "Pressurized tank preserves air underwater");

				CoralSpineMako target = OceanEntities.CORAL_SPINE_MAKO.create(l, EntitySpawnReason.COMMAND);
				target.snapTo(0.5, 92.0, -1.0, 0f, 0f);
				target.setNoAi(true);
				l.addFreshEntity(target);
				float beforeHp = target.getHealth();
				OceanItems.LEVIATHAN_GLAIVE.use(l, p, InteractionHand.MAIN_HAND);
				check(target.getHealth() < beforeHp, "Leviathan Glaive Hydro-Slash damages target in 75-deg cone");

				p.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 200, 1));
				p.setAirSupply(40);
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OceanItems.TIDECALLER_CONCH));
				OceanItems.TIDECALLER_CONCH.use(l, p, InteractionHand.MAIN_HAND);
				check(!p.hasEffect(MobEffects.MINING_FATIGUE), "Conch removes Mining Fatigue");
				check(p.hasEffect(MobEffects.WATER_BREATHING), "Conch grants Water Breathing");

				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OceanItems.TIDE_CLEAVER_HARPOON));
				OceanItems.TIDE_CLEAVER_HARPOON.use(l, p, InteractionHand.MAIN_HAND);
				check(!l.getEntitiesOfClass(ThrownHarpoon.class, p.getBoundingBox().inflate(16.0)).isEmpty(),
					"Harpoon right-click launches ThrownHarpoon projectile");

				// Step onto sunlit dry prismarine promenade beside tank for crystal-clear 3D Suit + Glaive + Lantern photo
				p.teleportTo(l, 0.5, 90.0, 12.5, Set.<Relative>of(), 0f, 0f, false);
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OceanItems.LEVIATHAN_GLAIVE));
				p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(OceanItems.SIREN_PEARL_LANTERN));
				target.discard();
			});

			context.runOnClient(mc -> {
				mc.particleEngine.clearParticles();
				if (!dev.wildercord.gametest.compat.GametestCompat.hudHidden(mc)) dev.wildercord.gametest.compat.GametestCompat.toggleHud(mc);
				mc.setCameraEntity(mc.player);
				mc.options.fov().set(55);
				mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
				mc.options.broadcastOptions();
			});
			context.waitTicks(8);
			shot(context, "ocean_live_diving_suit_and_weapons");
			context.runOnClient(mc -> {
				mc.options.fov().set(72);
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.options.broadcastOptions();
			});
			cut(context, world);

			// Part 4: Trench Anvil Forging + Repair, Sovereign Altar & Siren-Pearl Lantern
			BlockPos forgePos = new BlockPos(-2, 90, 12);
			BlockPos lanternPos = new BlockPos(0, 90, 12);
			BlockPos displayAltarPos = new BlockPos(2, 90, 12);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				clearMobs(l);
				l.setBlock(forgePos, OceanBlocks.TRENCH_ANVIL.defaultBlockState(), 3);
				l.setBlock(lanternPos, OceanBlocks.SIREN_PEARL_LANTERN.defaultBlockState(), 3);
				l.setBlock(displayAltarPos, OceanBlocks.SOVEREIGN_ALTAR.defaultBlockState(), 3);
				check(l.getBlockState(lanternPos).getLightEmission() == 15, "Siren-Pearl Lantern emits light 15");

				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.teleportTo(l, -3.5, 90.0, 13.8, Set.<Relative>of(), -142f, 6f, false);
				p.getInventory().clearContent();
				p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(OceanItems.ABYSSAL_DIVING_HELM));
				p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(OceanItems.BATHYSPHERE_CHESTPLATE));
				p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(OceanItems.KELP_WEAVE_GREAVES));
				p.setItemSlot(EquipmentSlot.FEET, new ItemStack(OceanItems.TRENCH_BOOTS));

				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TRIDENT));
				p.getInventory().add(new ItemStack(OceanItems.ABYSSAL_IRON_PLATE, 4));
				p.getInventory().add(new ItemStack(OceanItems.CORAL_EDGE_FANG, 2));
				BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(forgePos), Direction.UP, forgePos, false);
				l.getBlockState(forgePos).useItemOn(p.getMainHandItem(), l, p, InteractionHand.MAIN_HAND, hit);
				check(p.getInventory().countItem(OceanItems.TIDE_CLEAVER_HARPOON) >= 1,
					"Trench Anvil forges Tide-Cleaver Harpoon");

				ItemStack damagedHarpoon = new ItemStack(OceanItems.TIDE_CLEAVER_HARPOON);
				damagedHarpoon.setDamageValue(600);
				p.setItemInHand(InteractionHand.MAIN_HAND, damagedHarpoon);
				l.getBlockState(forgePos).useItemOn(damagedHarpoon, l, p, InteractionHand.MAIN_HAND, hit);
				check(damagedHarpoon.getDamageValue() < 600, "Trench Anvil repairs Abyssal gear with Iron Plate");
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OceanItems.TIDE_CLEAVER_HARPOON));
				p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(OceanItems.TIDECALLER_CONCH));
			});
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				// Discard auto-spawned Thalassor above displayAltarPos (puts altar into 20-min cooldown)
				l.getEntitiesOfClass(ThalassorBoss.class, new AABB(displayAltarPos).inflate(40)).forEach(Entity::discard);
			});

			context.runOnClient(mc -> {
				mc.particleEngine.clearParticles();
				if (!dev.wildercord.gametest.compat.GametestCompat.hudHidden(mc)) dev.wildercord.gametest.compat.GametestCompat.toggleHud(mc);
				mc.setCameraEntity(mc.player);
				mc.options.fov().set(68);
				mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
				mc.options.broadcastOptions();
			});
			context.waitTicks(8);
			shot(context, "ocean_live_trench_anvil_and_lantern");
			context.runOnClient(mc -> {
				mc.options.fov().set(72);
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.options.broadcastOptions();
			});
			cut(context, world);
		}
	}
}
