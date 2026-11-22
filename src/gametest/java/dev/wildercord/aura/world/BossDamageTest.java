package dev.wildercord.aura.world;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
/** A boss's blow on a player in full diamond with Protection IV lands several times what an ordinary mob's blow of the same strength does. */
public final class BossDamageTest implements FabricClientGameTest {
	private static void check(boolean x, String why) { if (!x) throw new AssertionError(why); }
	private static ServerPlayer p(MinecraftServer s) { return s.getPlayerList().getPlayers().getFirst(); }
	private static Mob mob(MinecraftServer s, String id) {
		var type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(id));
		return (Mob) s.overworld().getEntities(type, e -> true).getFirst();
	}
	/** What a 10-damage blow from {@code id}'s creature takes off the player. */
	private static float taken(MinecraftServer s, String id) {
		var p = p(s);
		p.setHealth(p.getMaxHealth());
		dev.wildercord.compat.McEntities.clearHurtCooldown(p);
		float before = p.getHealth();
		p.hurtServer(s.overworld(), s.overworld().damageSources().mobAttack(mob(s, id)), 10);
		return before - p.getHealth();
	}
	/** What the player's own {@code method} defence leaves of a 10-damage blow. */
	private static float absorbed(ServerPlayer p, net.minecraft.world.damagesource.DamageSource source, String method) {
		try {
			var m = net.minecraft.world.entity.LivingEntity.class.getDeclaredMethod(method, net.minecraft.world.damagesource.DamageSource.class, float.class);
			m.setAccessible(true);
			return (float) m.invoke(p, source, 10f);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("No " + method, e);
		}
	}
	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			var server = w.getServer();
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule natural_health_regeneration false");
			server.runCommand("difficulty normal");
			server.runCommand("fill -8 100 -8 8 100 8 stone");
			server.runCommand("fill -8 101 -8 8 106 8 air");
			server.runOnServer(s -> {
				var p = p(s);
				p.setGameMode(GameType.SURVIVAL);
				p.teleportTo(0.5, 101, 0.5);
			});
			for (String slot : new String[]{"head helmet", "chest chestplate", "legs leggings", "feet boots"}) {
				String[] part = slot.split(" ");
				server.runCommand("item replace entity @a armor." + part[0] + " with diamond_" + part[1] + "[enchantments={protection:4}]");
			}
			server.runCommand("summon zombie 5 101 0 {NoAI:1b,PersistenceRequired:1b}");
			server.runCommand("summon wildercord:cinder_warden -5 101 0 {NoAI:1b,PersistenceRequired:1b}");
			c.waitTicks(5);
			server.runOnServer(s -> {
				check(BossDamage.boss(mob(s, "wildercord:cinder_warden")), "The Cinder Warden counts as a boss");
				check(!BossDamage.boss(mob(s, "minecraft:zombie")), "A zombie does not count as a boss");
				var bossHit = s.overworld().damageSources().mobAttack(mob(s, "wildercord:cinder_warden"));
				float through = BossDamage.pierce(p(s), bossHit, 10, 0);
				check(Math.abs(through - 10 * BossDamageRules.PIERCE) < 0.01f, "A boss's blow goes partly through what a defence stops: " + through);
				float armoured = absorbed(p(s), bossHit, "getDamageAfterArmorAbsorb"), shielded = absorbed(p(s), bossHit, "getDamageAfterMagicAbsorb");
				var plainHit = s.overworld().damageSources().mobAttack(mob(s, "minecraft:zombie"));
				for (var defence : new String[] {"getDamageAfterArmorAbsorb", "getDamageAfterMagicAbsorb"}) {
					float plain = absorbed(p(s), plainHit, defence), boss = absorbed(p(s), bossHit, defence), expected = plain + (10 - plain) * (float) BossDamageRules.PIERCE;
					check(plain < 10 && Math.abs(boss - expected) < 0.01f, defence + " lets a boss's blow partly through: " + boss + " of 10, against " + plain + " for a zombie (expected " + expected + ")");
				}
				float zombie = taken(s, "minecraft:zombie"), warden = taken(s, "wildercord:cinder_warden");
				check(zombie > 0 && zombie < 1.5f, "An ordinary 10-damage blow barely scratches diamond and Protection IV: " + zombie);
				check(warden >= 4f && warden <= 6.5f, "A boss's 10-damage blow lands hard through diamond and Protection IV: " + warden + " (armour leaves " + armoured + ", Protection leaves " + shielded + " of 10)");
				check(warden > zombie * 3, "A boss's blow lands several times an ordinary mob's: " + warden + " against " + zombie);

				// Mojito's reward & Fetchblade forging: defeating Mojito rewards shards, crystals, pages, a rune, and an Epic Fetchblade that bounds on use.
				var player = p(s);
				player.getInventory().clearContent();
				var mojito = new Mojito(Guardians.MOJITO, s.overworld());
				mojito.setPos(0.5, 101, 3.5);
				s.overworld().addFreshEntity(mojito);
				mojito.die(s.overworld().damageSources().playerAttack(player));
				int shards = player.getInventory().countItem(AuraWorld.AURA_SHARD);
				int crystals = player.getInventory().countItem(dev.wildercord.content.WildercordItems.MANA_CRYSTAL);
				int tornPages = player.getInventory().countItem(dev.wildercord.content.WildercordItems.TORN_PAGE);
				int manualPages = player.getInventory().countItem(AuraWorld.MANUAL_PAGE);
				int runes = player.getInventory().countItem(dev.wildercord.content.WildercordItems.RUNE);
				check(shards >= MojitoRules.TIER.shards, "Mojito awards at least " + MojitoRules.TIER.shards + " Aura Shards, got " + shards);
				check(crystals >= MojitoRules.TIER.crystals, "Mojito awards at least " + MojitoRules.TIER.crystals + " Mana Crystals, got " + crystals);
				check(tornPages >= 2 && manualPages >= 2 && runes >= 1, "Mojito awards Torn Pages, Manual Pages, and a rune");

				var fetchblade = AuraWorld.forged(AuraWorldRules.Forged.FETCHBLADE, true);
				check(ForgedGear.of(fetchblade) == AuraWorldRules.Forged.FETCHBLADE, "Fetchblade carries the fetchblade forging");
				player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, fetchblade);
				check(ForgedGear.slashBonus(player) == AuraWorldRules.FETCHBLADE_SLASH, "Fetchblade boosts Aura Slash damage");
				check(ForgedGear.slashReach(player) == AuraWorldRules.FETCHBLADE_REACH, "Fetchblade boosts Aura Slash reach");
				check(ForgedGear.slashTargets(player) == AuraWorldRules.FETCHBLADE_TARGETS, "Fetchblade adds an extra Aura Slash target");
				var res = net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(player, s.overworld(), net.minecraft.world.InteractionHand.MAIN_HAND);
				check(res.consumesAction(), "Using Mojito's Fetchblade triggers its Fetch bound");
				check(player.getCooldowns().isOnCooldown(fetchblade), "Fetchblade enters cooldown after bounding");
				check(player.hasEffect(net.minecraft.world.effect.MobEffects.SPEED), "Fetchblade bound grants Speed II");

				// Upgraded Dungeon Boss Trophy: Cinder Heart grants Fire Resistance, Strength, and Regeneration on a 90s rest.
				var heart = new net.minecraft.world.item.ItemStack(dev.wildercord.content.dungeons.DungeonItems.CINDER_HEART);
				player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, heart);
				heart.use(s.overworld(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
				check(player.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE)
					&& player.hasEffect(net.minecraft.world.effect.MobEffects.STRENGTH)
					&& player.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),
					"Cinder Heart grants Fire Resistance, Strength, and Regeneration");
				check(dev.wildercord.content.dungeons.TrophyItem.REST == 20 * 90, "Trophy cooldown is 90 seconds");

				// Upgraded Dungeon Relics (Rootbound Relic & Stormglass Relic) restore 25 mana and 20 aura with a 20s cooldown.
				player.setAttached(dev.wildercord.player.WildercordAttachments.MANA, 0.0F);
				player.setAttached(dev.wildercord.aura.AuraAttachments.AURA, new dev.wildercord.aura.AuraAttachments.Data("stone", dev.wildercord.aura.AuraRules.FLOW, 0, 0, 0));
				var relic = new net.minecraft.world.item.ItemStack(dev.wildercord.content.WildercordItems.ROOTBOUND_RELIC);
				player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, relic);
				relic.use(s.overworld(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
				check(player.getAttachedOrElse(dev.wildercord.player.WildercordAttachments.MANA, 0.0F) >= 25f, "Rootbound Relic restores 25 mana");
				check(dev.wildercord.aura.Aura.aura(player) >= 20.0f, "Rootbound Relic restores 20 aura");
				check(player.getCooldowns().isOnCooldown(relic), "Rootbound Relic enters cooldown");
			});

			// Verify all Tripo voxel weapon models (oathkeeper, lumenedge, bulwark_maul, skyrend_glaive, fetchblade, and staffs)
			// in first-person, third-person front, third-person back, and mid-attack swing.
			server.runOnServer(s -> {
				var player = p(s);
				player.removeAllEffects();
				for (var e : s.overworld().getEntities(player, player.getBoundingBox().inflate(32))) e.discard();
				player.teleportTo(0.5, 101, 0.5);
			});
			c.runOnClient(mc -> {
				mc.getWindow().setWindowed(1280, 720);
				mc.options.guiScale().set(2);
				mc.resizeGui();
			});
			java.util.List<java.util.Map.Entry<String, net.minecraft.world.item.ItemStack>> showcase = java.util.List.of(
				java.util.Map.entry("oathkeeper", new net.minecraft.world.item.ItemStack(SleepingBlades.BLADE)),
				java.util.Map.entry("lumenedge", AuraWorld.forged(AuraWorldRules.Forged.LUMENEDGE, true)),
				java.util.Map.entry("bulwark_maul", AuraWorld.forged(AuraWorldRules.Forged.BULWARK_MAUL, true)),
				java.util.Map.entry("skyrend_glaive", AuraWorld.forged(AuraWorldRules.Forged.SKYREND_GLAIVE, true)),
				java.util.Map.entry("fetchblade", AuraWorld.forged(AuraWorldRules.Forged.FETCHBLADE, true)),
				java.util.Map.entry("fire_staff", new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("wildercord:fire_staff")))),
				java.util.Map.entry("greater_arcane_staff", new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("wildercord:greater_arcane_staff"))))
			);
			for (var entry : showcase) {
				String name = entry.getKey();
				var stack = entry.getValue();
				server.runOnServer(s -> {
					var player = p(s);
					player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack.copy());
					player.setYRot(0);
					player.setXRot(10);
				});
				c.waitTicks(22);
				c.runOnClient(mc -> {
					for (var ctx : new net.minecraft.world.item.ItemDisplayContext[]{
						net.minecraft.world.item.ItemDisplayContext.GUI,
						net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
						net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
						net.minecraft.world.item.ItemDisplayContext.GROUND
					}) {
						var state = new net.minecraft.client.renderer.item.ItemStackRenderState();
						mc.getItemModelResolver().updateForLiving(state, mc.player.getMainHandItem(), ctx, mc.player);
						check(!state.isEmpty(), name + " resolves a non-empty model in " + ctx);
					}
					mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
					mc.gui.toastManager().clear();
					mc.gui.hud.getChat().clearMessages(false);
				});
				c.waitTicks(2);
				c.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("weapon_" + name + "_first_person").disableCounterPrefix());
				if ("skyrend_glaive".equals(name)) {
					c.runOnClient(mc -> dev.wildercord.gametest.compat.GametestCompat.swingMainHandWithItem(mc.player));
					c.waitTicks(6);
					c.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("weapon_" + name + "_first_person_swing").disableCounterPrefix());
					c.waitTicks(10);
					c.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
					c.waitTicks(2);
					c.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("weapon_" + name + "_third_person_idle").disableCounterPrefix());
				}
				c.runOnClient(mc -> {
					dev.wildercord.gametest.compat.GametestCompat.swingMainHandWithItem(mc.player);
					mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
				});
				c.waitTicks(6);
				c.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("weapon_" + name + "_third_person_front").disableCounterPrefix());
				c.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
				c.waitTicks(10);
				c.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("weapon_" + name + "_third_person_back").disableCounterPrefix());
			}
			c.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));

			// Verify Spirits.isBoss classification, 1-2s capped holds, breakout + block shatter, 5s immunity, and pursuit resumption.
			net.minecraft.core.BlockPos[] holdBlockPos = new net.minecraft.core.BlockPos[1];
			java.util.UUID[] liveBossId = new java.util.UUID[1];
			double[] distAtBreakout = new double[1];
			server.runOnServer(s -> {
				var player = p(s);
				var level = s.overworld();
				for (var e : level.getEntities(player, player.getBoundingBox().inflate(48))) e.discard();
				for (int dx = -12; dx <= 12; dx++) {
					for (int dz = -12; dz <= 12; dz++) {
						level.setBlockAndUpdate(new net.minecraft.core.BlockPos(dx, 100, dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
						boolean wall = Math.abs(dx) == 12 || Math.abs(dz) == 12;
						for (int dy = 101; dy <= 104; dy++) {
							var state = (wall && dy <= 103)
								? net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()
								: net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
							level.setBlockAndUpdate(new net.minecraft.core.BlockPos(dx, dy, dz), state);
						}
					}
				}
				player.setGameMode(GameType.SURVIVAL);
				player.setHealth(player.getMaxHealth());
				player.teleportTo(0.5, 101, 7.5);
				player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.RESISTANCE, 600, 3, false, false));
				player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 600, 3, false, false));

				var wardenType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("wildercord:cinder_warden"));
				var serpentType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("wildercord:rootcoil_serpent"));
				var warden = wardenType.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				var serpent = serpentType.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				var guardianCat = dev.wildercord.wildlife.ShrineCatContent.SHRINE_CAT.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				var villageCat = dev.wildercord.wildlife.ShrineCatContent.SHRINE_CAT.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				var zombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				check(warden != null && dev.wildercord.cast.Spirits.isBoss(warden), "cinder_warden recognized as boss");
				check(serpent != null && dev.wildercord.cast.Spirits.isBoss(serpent), "rootcoil_serpent guardian recognized as boss");
				if (guardianCat != null) {
					guardianCat.setRole(dev.wildercord.wildlife.ShrineCatRules.ROLE_GUARDIAN);
					check(dev.wildercord.cast.Spirits.isBoss(guardianCat), "untamed sanctuary guardian ShrineCat recognized as boss");
				}
				if (villageCat != null) {
					villageCat.setRole(dev.wildercord.wildlife.ShrineCatRules.ROLE_VILLAGE);
					check(!dev.wildercord.cast.Spirits.isBoss(villageCat), "peaceful village ShrineCat not treated as boss");
				}
				check(zombie != null && !dev.wildercord.cast.Spirits.isBoss(zombie), "ordinary zombie not treated as boss");
				if (zombie != null) {
					zombie.setPos(0.5, 101, -6.5);
					level.addFreshEntity(zombie);
					dev.wildercord.cast.Spirits.hold(zombie, 40, player);
					check(zombie.isNoAi(), "non-boss mob still gets full NoAi hold");
					zombie.discard();
				}

				var bastion = dev.wildercord.aura.world.AuraWorld.OATHBOUND_BASTION.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
				check(bastion != null && dev.wildercord.cast.Spirits.isBoss(bastion), "oathbound_bastion recognized as boss");
				bastion.setPos(0.5, 101, -9.5);
				bastion.setPersistenceRequired();
				level.addFreshEntity(bastion);
				liveBossId[0] = bastion.getUUID();
				bastion.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
				bastion.setTarget(player);

				holdBlockPos[0] = bastion.blockPosition().south();
				dev.wildercord.cast.TemporaryBlocks.put(level, holdBlockPos[0],
					net.minecraft.world.level.block.Blocks.PACKED_MUD.defaultBlockState(),
					net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
					level.getGameTime() + 200);
				level.setBlockAndUpdate(holdBlockPos[0], net.minecraft.world.level.block.Blocks.PACKED_MUD.defaultBlockState());

				dev.wildercord.cast.Spirits.freeze(bastion, 200);
				boolean stacked = dev.wildercord.cast.Spirits.tryClaimBossHold(bastion, 200, player);
				check(!stacked, "cannot extend or stack an already active boss hold");
				check(dev.wildercord.cast.Spirits.bossHeld(bastion), "boss enters brief temporary hold");
				check(!bastion.isNoAi(), "boss AI is never set to NoAI");
				check(bastion.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS), "boss receives temporary slowness while held");
			});
			c.waitTicks(38);
			server.runOnServer(s -> {
				var player = p(s);
				var level = s.overworld();
				var boss = (Mob) level.getEntity(liveBossId[0]);
				check(boss != null && boss.isAlive(), "live boss remains alive after breakout");
				check(!dev.wildercord.cast.Spirits.bossHeld(boss), "boss breaks out of 200-tick hold within 38 ticks");
				check(dev.wildercord.cast.Spirits.bossHoldImmune(boss), "boss gains 5s unstoppable immunity after breakout");
				check(!boss.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS), "breakout clears slowness");
				check(level.getBlockState(holdBlockPos[0]).isAir() && !dev.wildercord.cast.TemporaryBlocks.recorded(level, holdBlockPos[0]),
					"breakout shatters surrounding temporary hold blocks");
				check(!dev.wildercord.cast.Spirits.tryClaimBossHold(boss, 100, player), "follow-up hold claim rejected during 5s immunity");
				dev.wildercord.cast.Spirits.freeze(boss, 100);
				check(!dev.wildercord.cast.Spirits.bossHeld(boss) && !boss.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS),
					"follow-up freeze cannot re-hold boss during 5s immunity");
				check(boss.getTarget() == player, "boss retains target on breakout");
				distAtBreakout[0] = boss.distanceTo(player);
			});
			c.waitTicks(20);
			server.runOnServer(s -> {
				var player = p(s);
				var level = s.overworld();
				var boss = (Mob) level.getEntity(liveBossId[0]);
				check(boss != null && boss.isAlive(), "boss alive during counterattack pursuit");
				check(boss.getTarget() == player, "boss continues targeting player after breakout");
				check(boss.distanceTo(player) < distAtBreakout[0] - 0.5, "boss closes distance to resume attacking after breakout");
			});
			c.waitTicks(90);
			server.runOnServer(s -> {
				var player = p(s);
				var level = s.overworld();
				var boss = (Mob) level.getEntity(liveBossId[0]);
				check(boss != null && !dev.wildercord.cast.Spirits.bossHoldImmune(boss), "5s hold immunity expires cleanly");
				check(dev.wildercord.cast.Spirits.tryClaimBossHold(boss, 40, player), "boss can be briefly held again after 5s immunity expires");
			});
			c.waitTicks(38);
			server.runOnServer(s -> {
				var player = p(s);
				var level = s.overworld();
				var boss = (Mob) level.getEntity(liveBossId[0]);
				check(boss != null && !dev.wildercord.cast.Spirits.bossHeld(boss), "second temporary hold also breaks out within 38 ticks");
				if (boss != null) boss.discard();
				player.removeAllEffects();
				player.setGameMode(GameType.CREATIVE);
				player.setHealth(player.getMaxHealth());
			});

			// Verify raised boss health pools, dungeon altar respawn, lair guardian respawn, and clean custom-mob fire rendering.
			net.minecraft.core.BlockPos altarPos = new net.minecraft.core.BlockPos(0, 101, 0);
			long[] savedTime = new long[1];
			server.runOnServer(s -> {
				var player = p(s);
				var level = s.overworld();
				savedTime[0] = level.getGameTime();
				for (var e : level.getEntities(player, player.getBoundingBox().inflate(48))) e.discard();

				var wardenType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("wildercord:cinder_warden"));
				var serpentType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("wildercord:rootcoil_serpent"));
				var archivistType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("wildercord:archivist"));
				var maxHealthAttr = (net.minecraft.world.entity.ai.attributes.RangedAttribute) net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH.value();
				check(maxHealthAttr.getMaxValue() >= dev.wildercord.cast.BossRules.MAX_HEALTH_CAP, "max_health attribute cap raised above vanilla 1024");
				var warden = (Mob) wardenType.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				var serpent = (Mob) serpentType.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				var archivist = (Mob) archivistType.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				var bastion = dev.wildercord.aura.world.AuraWorld.OATHBOUND_BASTION.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				var keeper = dev.wildercord.aura.world.SwordTombs.KEEPER.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				var ferryman = dev.wildercord.world.ferryman.FerrymanContent.FERRYMAN.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				check(warden != null && warden.getMaxHealth() == 1200.0F, "cinder_warden max health raised to 1200");
				check(serpent != null && serpent.getMaxHealth() == 750.0F, "rootcoil_serpent max health raised to 750");
				check(archivist != null && archivist.getMaxHealth() == 1650.0F, "archivist max health raised to 1650");
				check(bastion != null && bastion.getMaxHealth() == 650.0F, "oathbound_bastion max health raised to 650");
				check(keeper != null && keeper.getMaxHealth() == 700.0F, "gravekeeper max health raised to 700");
				check(ferryman != null && ferryman.getMaxHealth() == 720.0F, "reed_crowned_ferryman max health raised to 720");

				// Test DungeonAltarBlockEntity respawn after BossRules.BOSS_RESPAWN_TICKS.
				player.teleportTo(0.5, 101, 4.5);
				level.setBlockAndUpdate(altarPos, dev.wildercord.content.dungeons.DungeonBlocks.ALTAR.defaultBlockState()
					.setValue(dev.wildercord.content.dungeons.DungeonAltarBlock.KIND, dev.wildercord.content.dungeons.DungeonAltarBlock.Kind.CINDER)
					.setValue(dev.wildercord.content.dungeons.DungeonAltarBlock.AWAKE, true));
				var altarBe = (dev.wildercord.content.dungeons.DungeonAltarBlockEntity) level.getBlockEntity(altarPos);
				altarBe.slain();
				check(altarBe.isSlain(), "dungeon altar marks slain after defeat");

				long dueTime = level.getGameTime() + dev.wildercord.cast.BossRules.BOSS_RESPAWN_TICKS;
				long aligned = dueTime + Math.floorMod(-(dueTime + altarPos.asLong()), 100L);
				((net.minecraft.world.level.storage.ServerLevelData) level.getLevelData()).setGameTime(aligned);
				dev.wildercord.content.dungeons.DungeonAltarBlockEntity.serverTick(level, altarPos, level.getBlockState(altarPos), altarBe);
				check(!altarBe.isSlain() && !level.getBlockState(altarPos).getValue(dev.wildercord.content.dungeons.DungeonAltarBlock.AWAKE),
					"dungeon altar re-arms after respawn delay");
				level.setBlockAndUpdate(altarPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
				((net.minecraft.world.level.storage.ServerLevelData) level.getLevelData()).setGameTime(savedTime[0]);

				// Test LairRespawns scheduling on bound guardian death and respawn at home.
				var boundSerpent = (dev.wildercord.aura.world.RootcoilSerpent) serpentType.create(level, net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
				boundSerpent.snapTo(0.5, 101, 0.5, 0.0F, 0.0F);
				boundSerpent.bind(altarPos, 0.0F);
				level.addFreshEntity(boundSerpent);
				boundSerpent.setHealth(1.0F);
				boundSerpent.hurtServer(level, level.damageSources().genericKill(), 50.0F);
				check(dev.wildercord.aura.world.LairRespawns.pendingCount(level) >= 1, "bound lair guardian schedules respawn on death");
				long afterLairRespawn = level.getGameTime() + dev.wildercord.cast.BossRules.BOSS_RESPAWN_TICKS + 1L;
				((net.minecraft.world.level.storage.ServerLevelData) level.getLevelData()).setGameTime(afterLairRespawn);
				dev.wildercord.aura.world.LairRespawns.tickLevel(level);
				var respawned = level.getEntitiesOfClass(dev.wildercord.aura.world.RootcoilSerpent.class,
					new net.minecraft.world.phys.AABB(altarPos).inflate(16.0), dev.wildercord.aura.world.RootcoilSerpent::isAlive);
				check(respawned.size() == 1
					&& respawned.getFirst().getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) == 750.0
					&& respawned.getFirst().getMaxHealth() >= 750.0F,
					"lair guardian respawns in its arena with boosted base max health 750 (size=" + respawned.size()
					+ (respawned.isEmpty() ? "" : ", base=" + respawned.getFirst().getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
						+ ", max=" + respawned.getFirst().getMaxHealth()) + ")");
				check(dev.wildercord.aura.world.LairRespawns.pendingCount(level) == 0, "lair respawn entry clears once spawned");
				for (var r : respawned) r.discard();
				((net.minecraft.world.level.storage.ServerLevelData) level.getLevelData()).setGameTime(savedTime[0]);
			});

			// Test custom mob fire rendering: burning custom mob suppresses vanilla flame billboard while burning vanilla mob keeps it.
			int[] fireIds = new int[2];
			server.runCommand("weather clear");
			server.runCommand("time set 6000");
			server.runOnServer(s -> {
				var player = p(s);
				var level = s.overworld();
				for (int dx = -12; dx <= 12; dx++) {
					for (int dz = -12; dz <= 12; dz++) {
						for (int dy = 101; dy <= 103; dy++) {
							level.setBlockAndUpdate(new net.minecraft.core.BlockPos(dx, dy, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
						}
					}
				}
				player.setGameMode(GameType.CREATIVE);
				player.teleportTo(0.5, 101, 5.5);
				player.setYRot(180.0F);
				player.setXRot(8.0F);

				var bastion = dev.wildercord.aura.world.AuraWorld.OATHBOUND_BASTION.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				bastion.setNoAi(true);
				bastion.setPos(0.5, 101, 0.5);
				bastion.setYRot(0.0F);
				bastion.setYHeadRot(0.0F);
				bastion.setYBodyRot(0.0F);
				bastion.setRemainingFireTicks(200);
				level.addFreshEntity(bastion);
				fireIds[0] = bastion.getId();

				var zombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				zombie.setNoAi(true);
				zombie.setPos(3.5, 101, 0.5);
				zombie.setRemainingFireTicks(200);
				level.addFreshEntity(zombie);
				fireIds[1] = zombie.getId();
			});
			c.waitTicks(6);
			c.runOnClient(mc -> {
				var custom = mc.level.getEntity(fireIds[0]);
				var vanilla = mc.level.getEntity(fireIds[1]);
				check(custom != null && custom.displayFireAnimation(), "burning custom mob logically displays fire");
				check(vanilla != null && vanilla.displayFireAnimation(), "burning vanilla mob logically displays fire");
				var customState = mc.getEntityRenderDispatcher().extractEntity(custom, 0.0F);
				var vanillaState = mc.getEntityRenderDispatcher().extractEntity(vanilla, 0.0F);
				check(!customState.displayFireAnimation, "custom mob suppresses obscuring vanilla fire billboard");
				check(vanillaState.displayFireAnimation, "vanilla mob preserves vanilla fire billboard");
				mc.gui.toastManager().clear();
				mc.gui.hud.getChat().clearMessages(false);
			});
			c.waitTicks(4);
			c.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("custom_mob_fire_clean").disableCounterPrefix());
			server.runOnServer(s -> {
				var level = s.overworld();
				for (int id : fireIds) {
					var e = level.getEntity(id);
					if (e != null) e.discard();
				}
			});
		}
	}
}
