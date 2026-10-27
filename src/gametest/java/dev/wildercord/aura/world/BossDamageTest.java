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
		// The hit cooldown in hurtServer reads damageCooldownTime, which setInvulnerableTime leaves alone.
		p.setInvulnerableTime(0);
		p.damageCooldownTime = 0;
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
					c.runOnClient(mc -> mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND));
					c.waitTicks(3);
					c.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("weapon_" + name + "_first_person_swing").disableCounterPrefix());
					c.waitTicks(10);
					c.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
					c.waitTicks(2);
					c.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("weapon_" + name + "_third_person_idle").disableCounterPrefix());
				}
				c.runOnClient(mc -> {
					mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
					mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
				});
				c.waitTicks(3);
				c.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("weapon_" + name + "_third_person_front").disableCounterPrefix());
				c.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
				c.waitTicks(8);
				c.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("weapon_" + name + "_third_person_back").disableCounterPrefix());
			}
			c.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
		}
	}
}
