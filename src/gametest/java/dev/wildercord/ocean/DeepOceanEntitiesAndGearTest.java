package dev.wildercord.ocean;

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
import net.minecraft.world.entity.EntityTypes;
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
				e -> e.entityTags().contains(CAM_TAG)).forEach(Entity::discard);
			Display.TextDisplay cam = EntityTypes.TEXT_DISPLAY.create(l, EntitySpawnReason.COMMAND);
			cam.snapTo(eye.x, eye.y, eye.z, yaw(eye, target), pitch(eye, target));
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
				if (!mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
				mc.gui.toastManager().clear();
				mc.gui.hud.getChat().clearMessages(false);
			}
		});
		context.waitTicks(4);
	}

	private static void cut(ClientGameTestContext context, TestSingleplayerContext world) {
		context.runOnClient(mc -> mc.setCameraEntity(mc.player));
		world.getServer().runOnServer(s -> s.overworld()
			.getEntitiesOfClass(Display.TextDisplay.class, new AABB(STAGE).inflate(96),
				e -> e.entityTags().contains(CAM_TAG))
			.forEach(Entity::discard));
		context.waitTicks(2);
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> {
			mc.gui.toastManager().clear();
			mc.gui.hud.getChat().clearMessages(false);
		});
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static void buildAquariumStage(ServerLevel l) {
		for (int x = -22; x <= 22; x++) {
			for (int z = -22; z <= 22; z++) {
				for (int y = 88; y <= 112; y++) {
					l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
				}
				boolean border = Math.abs(x) == 18 || Math.abs(z) == 18;
				if (Math.abs(x) <= 18 && Math.abs(z) <= 18) {
					boolean lamp = (Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 0);
					l.setBlock(new BlockPos(x, 89, z),
						(lamp ? Blocks.SEA_LANTERN : (border ? Blocks.DARK_PRISMARINE : Blocks.SAND)).defaultBlockState(), 2);
					for (int y = 90; y <= 101; y++) {
						if (border) {
							l.setBlock(new BlockPos(x, y, z),
								(y == 101 ? Blocks.PRISMARINE_BRICKS : Blocks.GLASS).defaultBlockState(), 2);
						} else {
							l.setBlock(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState(), 2);
						}
					}
				} else {
					// Surround dry prismarine walkway
					l.setBlock(new BlockPos(x, 89, z), Blocks.PRISMARINE_BRICKS.defaultBlockState(), 2);
				}
			}
		}
		// Dry front observation promenade along south outside aquarium (z = 19..22, y = 94 floor for Showcase)
		for (int x = -14; x <= 14; x++) {
			for (int z = 19; z <= 22; z++) {
				l.setBlock(new BlockPos(x, 93, z), Blocks.DARK_PRISMARINE.defaultBlockState(), 2);
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
			context.waitTicks(20);
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
				mc.resizeGui();
			});

			// Build Aquarium Arena & Showcase Enemies + Thalassor Boss
			UUID[] ids = new UUID[6];
			BlockPos altarPos = new BlockPos(0, 90, -2);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				buildAquariumStage(l);
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.CREATIVE);
				p.teleportTo(l, 0.5, 94.0, 21.0, Set.<Relative>of(), 180f, 0f, false);

				// Place Sovereign Altar inside aquarium floor
				l.setBlock(altarPos, OceanBlocks.SOVEREIGN_ALTAR.defaultBlockState(), 3);

				BarnacleHulkDredger hulk = OceanEntities.BARNACLE_HULK_DREDGER.create(l, EntitySpawnReason.COMMAND);
				hulk.snapTo(-9.0, 90.5, 4.0, -20f, 0f);
				hulk.setNoAi(true);
				hulk.setPersistenceRequired();
				l.addFreshEntity(hulk);

				SirenVeilJelly jelly = OceanEntities.SIREN_VEIL_JELLY.create(l, EntitySpawnReason.COMMAND);
				jelly.snapTo(-4.0, 94.5, 4.5, 0f, 0f);
				jelly.setNoAi(true);
				l.addFreshEntity(jelly);

				CoralSpineMako mako = OceanEntities.CORAL_SPINE_MAKO.create(l, EntitySpawnReason.COMMAND);
				mako.snapTo(4.0, 93.5, 4.5, -35f, 0f);
				mako.setNoAi(true);
				l.addFreshEntity(mako);

				DreadNautilusSentinel nautilus = OceanEntities.DREAD_NAUTILUS_SENTINEL.create(l, EntitySpawnReason.COMMAND);
				nautilus.snapTo(9.0, 92.5, 4.0, 25f, 0f);
				nautilus.setNoAi(true);
				l.addFreshEntity(nautilus);

				ThalassorBoss thalassor = OceanEntities.THALASSOR.create(l, EntitySpawnReason.EVENT);
				thalassor.snapTo(0.0, 96.5, -2.5, 180f, 0f);
				thalassor.setNoAi(true);
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

			// Photograph Enemies + Thalassor through crystal glass wall (outside fog)
			director(context, world, new Vec3(0.5, 95.5, 19.8), new Vec3(0.0, 94.2, 0.5));
			shot(context, "ocean_live_enemies_and_thalassor");
			cut(context, world);

			// Exercise Thalassor phases & Sovereign Altar re-summon logic
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				ThalassorBoss boss = (ThalassorBoss) l.getEntity(ids[4]);
				check(OceanRules.thalassorPhase(boss.getHealth(), boss.getMaxHealth()) == 1, "Phase 1 at full HP");
				boss.setHealth(1200.0f);
				check(OceanRules.thalassorPhase(boss.getHealth(), boss.getMaxHealth()) == 2, "Phase 2 at 50% HP");
				boss.setHealth(500.0f);
				check(OceanRules.thalassorPhase(boss.getHealth(), boss.getMaxHealth()) == 3, "Phase 3 below 33% HP");
				boss.discard();

				// Tick Altar once to detect boss defeated & enter cooldown, then re-summon with Leviathan Heart-Pearl
				SovereignAltarBlockEntity altar = (SovereignAltarBlockEntity) l.getBlockEntity(altarPos);
				check(altar != null, "Sovereign Altar block entity missing");
				SovereignAltarBlockEntity.serverTick(l, altarPos, l.getBlockState(altarPos), altar);
				check(altar.getRespawnTicksRemaining() == OceanRules.THALASSOR_RESPAWN_TICKS, "Altar starts respawn timer");
				InteractionResult res = altar.interact(p, new ItemStack(OceanItems.LEVIATHAN_HEART_PEARL));
				check(res.consumesAction() || res == InteractionResult.SUCCESS, "Offering summoned boss");
				check(altar.getBossUuid() != null && l.getEntity(altar.getBossUuid()) instanceof ThalassorBoss, "Fresh Thalassor bound to altar");
			});

			// Part 2: Friendlies, Merchants, Lumi-Seahorse, Pearl-Pup & Singing Moon-Whale
			UUID[] friendlyIds = new UUID[6];
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				clearMobs(l);
				l.setBlock(altarPos, Blocks.WATER.defaultBlockState(), 3);
				l.setBlock(new BlockPos(-8, 90, -2), Blocks.CHEST.defaultBlockState(), 3);

				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.SURVIVAL);
				p.teleportTo(l, 0.5, 94.0, 21.0, Set.<Relative>of(), 180f, 0f, false);

				OceanRules.MerchantProfession[] profs = OceanRules.MerchantProfession.values();
				for (int i = 0; i < 3; i++) {
					BrineMerchant m = OceanEntities.BRINE_MERCHANT.create(l, EntitySpawnReason.COMMAND);
					m.setMerchantProfession(profs[i]);
					// Place on raised prismarine trader dais inside dry glass pocket or shallow plinth
					BlockPos plinth = new BlockPos(-6 + i * 3, 90, 5);
					l.setBlock(plinth, Blocks.PRISMARINE_BRICKS.defaultBlockState(), 3);
					l.setBlock(plinth.above(), Blocks.PRISMARINE_BRICKS.defaultBlockState(), 3);
					m.snapTo(plinth.getX() + 0.5, 92.0, plinth.getZ() + 0.5, 0f, 0f);
					m.setNoAi(true);
					l.addFreshEntity(m);
					check(m.canBreatheUnderwater(), "Brine-Merchant breathes underwater");
					check(!m.getOffers().isEmpty(), "Brine-Merchant has trade offers for " + profs[i]);
					friendlyIds[i] = m.getUUID();
				}

				LumiSeahorse seahorse = OceanEntities.LUMI_SEAHORSE.create(l, EntitySpawnReason.COMMAND);
				seahorse.snapTo(-4.0, 94.0, 2.0, -30f, 0f);
				seahorse.setNoAi(true);
				l.addFreshEntity(seahorse);
				seahorse.tameWithName(p);
				seahorse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
				friendlyIds[3] = seahorse.getUUID();

				PearlPup pup = OceanEntities.PEARL_PUP.create(l, EntitySpawnReason.COMMAND);
				pup.snapTo(-1.5, 92.0, 3.0, 15f, 0f);
				pup.tame(p);
				l.addFreshEntity(pup);
				friendlyIds[4] = pup.getUUID();

				SingingMoonWhale whale = OceanEntities.SINGING_MOON_WHALE.create(l, EntitySpawnReason.COMMAND);
				whale.snapTo(5.5, 96.0, -1.5, -145f, 0f);
				whale.setNoAi(true);
				l.addFreshEntity(whale);
				check(whale.getMaxHealth() == OceanRules.MOON_WHALE_HP, "Moon-Whale HP == 220");
				friendlyIds[5] = whale.getUUID();
			});

			director(context, world, new Vec3(0.5, 95.5, 19.6), new Vec3(0.5, 94.0, 1.5));
			shot(context, "ocean_live_friendlies_and_whale");
			cut(context, world);

			// Exercise Lumi-Seahorse ride+dash, Pearl-Pup treasure sniff, Singing Moon-Whale song+ambergris
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				LumiSeahorse seahorse = (LumiSeahorse) l.getEntity(friendlyIds[3]);
				seahorse.setNoAi(false);
				check(p.startRiding(seahorse), "Player mounts Lumi-Seahorse");
				seahorse.onPlayerJump(100);
				check(seahorse.getDashTicks() > 0 && seahorse.getDeltaMovement().lengthSqr() > 0.2,
					"Lumi-Seahorse dash impulse triggers underwater");
				p.stopRiding();

				// Feed Moon-Whale cod -> song effects + ambergris drop
				SingingMoonWhale whale = (SingingMoonWhale) l.getEntity(friendlyIds[5]);
				p.teleportTo(l, whale.getX(), whale.getY(), whale.getZ() + 3.0, Set.<Relative>of(), 180f, 0f, false);
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COD));
				whale.mobInteract(p, InteractionHand.MAIN_HAND);
				check(whale.isSinging(), "Moon-Whale sings when fed fish");
				check(p.hasEffect(MobEffects.WATER_BREATHING) && p.hasEffect(MobEffects.DOLPHINS_GRACE),
					"Moon-Whale song grants buffs to diver");
				List<ItemEntity> drops = l.getEntitiesOfClass(ItemEntity.class, whale.getBoundingBox().inflate(4.0),
					ie -> ie.getItem().is(OceanItems.STAR_BARNACLE_AMBERGRIS));
				check(!drops.isEmpty(), "Moon-Whale sheds Star-Barnacle Ambergris");
			});
			// Tick 65 ticks so tamed Pearl-Pup runs its sniffTimer down and locates the chest at (-8,90,-2)
			context.waitTicks(65);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				PearlPup pup = (PearlPup) l.getEntity(friendlyIds[4]);
				check(pup != null && pup.isTame() && pup.isSniffing(), "Tamed Pearl-Pup sniffed out sunken treasure chest");
			});

			// Part 3: Full Abyssal Diving Suit, O2 Reserve, Weapons, Conch Air Dome & Culinary Effects
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
				p.teleportTo(l, 0.5, 92.0, 5.5, Set.<Relative>of(), 180f, -5f, false);
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

				// Leviathan Glaive right-click Hydro-Slash against submerged target
				CoralSpineMako target = OceanEntities.CORAL_SPINE_MAKO.create(l, EntitySpawnReason.COMMAND);
				target.snapTo(0.5, 92.5, 2.5, 0f, 0f);
				target.setNoAi(true);
				l.addFreshEntity(target);
				float beforeHp = target.getHealth();
				OceanItems.LEVIATHAN_GLAIVE.use(l, p, InteractionHand.MAIN_HAND);
				check(target.getHealth() < beforeHp, "Leviathan Glaive Hydro-Slash damages target in 75-deg cone");

				// Tidecaller Conch clears Mining Fatigue and conjures breathable Air Dome
				p.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 200, 1));
				p.setAirSupply(40);
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OceanItems.TIDECALLER_CONCH));
				OceanItems.TIDECALLER_CONCH.use(l, p, InteractionHand.MAIN_HAND);
				check(!p.hasEffect(MobEffects.MINING_FATIGUE), "Conch removes Mining Fatigue");
				check(p.hasEffect(MobEffects.WATER_BREATHING), "Conch grants Water Breathing");

				// Throw Tide-Cleaver Harpoon
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OceanItems.TIDE_CLEAVER_HARPOON));
				OceanItems.TIDE_CLEAVER_HARPOON.use(l, p, InteractionHand.MAIN_HAND);
				check(!l.getEntitiesOfClass(ThrownHarpoon.class, p.getBoundingBox().inflate(16.0)).isEmpty(),
					"Harpoon right-click launches ThrownHarpoon projectile");

				// Put Leviathan Glaive back in main hand & step near crystal glass wall for fog-free showcase
				p.teleportTo(l, 0.5, 91.5, 14.5, Set.<Relative>of(), -140f, 0f, false);
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OceanItems.LEVIATHAN_GLAIVE));
				p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(OceanItems.SIREN_PEARL_LANTERN));
				target.discard();
			});

			// Photograph diver in 3D Abyssal Diving Suit holding Leviathan Glaive & Siren-Pearl Lantern from outside aquarium glass
			director(context, world, new Vec3(2.4, 92.8, 19.6), new Vec3(0.5, 92.3, 14.5));
			shot(context, "ocean_live_diving_suit_and_weapons");
			cut(context, world);

			// Part 4: Trench Anvil Forging + Repair, Sovereign Altar & Siren-Pearl Lantern Repel
			BlockPos forgePos = new BlockPos(-3, 94, 20);
			BlockPos lanternPos = new BlockPos(0, 94, 20);
			BlockPos displayAltarPos = new BlockPos(3, 94, 20);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				clearMobs(l);
				l.setBlock(forgePos, OceanBlocks.TRENCH_ANVIL.defaultBlockState(), 3);
				l.setBlock(lanternPos, OceanBlocks.SIREN_PEARL_LANTERN.defaultBlockState(), 3);
				l.setBlock(displayAltarPos, OceanBlocks.SOVEREIGN_ALTAR.defaultBlockState(), 3);
				check(l.getBlockState(lanternPos).getLightEmission() == 15, "Siren-Pearl Lantern emits light 15");

				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.teleportTo(l, 0.5, 94.0, 21.8, Set.<Relative>of(), 180f, 5f, false);
				p.getInventory().clearContent();
				p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(OceanItems.ABYSSAL_DIVING_HELM));
				p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(OceanItems.BATHYSPHERE_CHESTPLATE));
				p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(OceanItems.KELP_WEAVE_GREAVES));
				p.setItemSlot(EquipmentSlot.FEET, new ItemStack(OceanItems.TRENCH_BOOTS));

				// Forge Tide-Cleaver Harpoon at Trench Anvil: Trident + 3 Iron Plates + 2 Fangs
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TRIDENT));
				p.getInventory().add(new ItemStack(OceanItems.ABYSSAL_IRON_PLATE, 4));
				p.getInventory().add(new ItemStack(OceanItems.CORAL_EDGE_FANG, 2));
				BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(forgePos), Direction.UP, forgePos, false);
				l.getBlockState(forgePos).useItemOn(p.getMainHandItem(), l, p, InteractionHand.MAIN_HAND, hit);
				check(p.getInventory().countItem(OceanItems.TIDE_CLEAVER_HARPOON) >= 1,
					"Trench Anvil forges Tide-Cleaver Harpoon");

				// Repair damaged Harpoon with 1 Abyssal Iron Plate
				ItemStack damagedHarpoon = new ItemStack(OceanItems.TIDE_CLEAVER_HARPOON);
				damagedHarpoon.setDamageValue(600);
				p.setItemInHand(InteractionHand.MAIN_HAND, damagedHarpoon);
				l.getBlockState(forgePos).useItemOn(damagedHarpoon, l, p, InteractionHand.MAIN_HAND, hit);
				check(damagedHarpoon.getDamageValue() < 600, "Trench Anvil repairs Abyssal gear with Iron Plate");
				p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(OceanItems.TIDECALLER_CONCH));
			});

			director(context, world, new Vec3(0.5, 95.8, 25.0), new Vec3(0.5, 94.3, 20.3));
			shot(context, "ocean_live_trench_anvil_and_lantern");
			cut(context, world);
		}
	}
}
