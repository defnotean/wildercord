package dev.wildercord.gametest;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.CastEngine;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Exposed;
import dev.wildercord.cast.Runebound;
import dev.wildercord.cast.SpellDefence;
import dev.wildercord.client.CordScreen;
import dev.wildercord.content.WildercordEffects;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * Standing up to spells: a player at full health meets the worst spell hits the mod has (the bosses' biggest at Hard, and
 * a player's spells in PvP) in four outfits: nothing, full netherite, netherite with Warding IV on every piece, and all that
 * under the Potion of Warding. Each layer must take a measurable share off, Resistance still counts, a hit's bonuses are
 * held to the cap against a player, the spellguard saves a full-health player once and then recharges (a totem answers
 * meanwhile), it doesn't hold below its threshold, a spell's /kill damage can't slip past it, and the real /kill still kills.
 *
 * <p>The numbers are logged ({@code [defence]}) and the guard's moment and the Cord screen's defence readout are
 * photographed. Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordDefenceTest implements FabricClientGameTest {
	/** The stage: a stone platform high in the sky, so terrain and mobs stay out of it. */
	private static final BlockPos STAGE = new BlockPos(0, 180, 0);
	/** Enough health that no measured hit kills (so the guard never has a say in the numbers). */
	private static final double BIG_HEALTH = 200;

	/** A worst case from the measurements: a name, how it's dealt, and at what size before the game's own difficulty. */
	private record Case(String name, String type, double amount, boolean pvp) {}

	/**
	 * The biggest single spell hits a player can take (see the measurements behind SpellDefence): the bosses' at Hard (the
	 * game adds half again on Hard to what a monster deals) and a player's in PvP after the 0.6 PvP scale.
	 */
	private static final List<Case> WORST = List.of(
		new Case("Archivist's Sunfall (blast)", "explosion", 21.66, false),
		new Case("Tide Scribe's lightning on a wet player", "lightning", 19.8, false),
		new Case("Archivist's Beam and Sonic Boom", "sonic_boom", 17.6, false),
		new Case("Star-Eater's Domain of Hex and Harm", "magic", 15.4, false),
		new Case("PvP: Hollow at sustained endgame power", "sonic_boom", 20 * 3.58 * 0.6, true),
		new Case("PvP: Prismatic Burst at sustained power", "magic", 25 * 3.58 * 0.6, true));

	private static final String[] OUTFITS = {"nothing", "netherite", "netherite + Warding IV", "netherite + Warding IV + Warded"};

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			for (String command : List.of("difficulty hard", "time set 6000", "gamerule advance_time false", "gamerule spawn_mobs false",
					"weather clear", "gamerule advance_weather false", "gamerule natural_health_regeneration false")) {
				world.getServer().runCommand(command);
			}
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				ServerLevel level = player.level();
				for (int x = -6; x <= 6; x++) {
					for (int z = -6; z <= 6; z++) {
						level.setBlockAndUpdate(STAGE.offset(x, 0, z), Blocks.STONE.defaultBlockState());
					}
				}
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(level, 0.5, STAGE.getY() + 1, 0.5, java.util.Set.of(), 0, 10, false);
				Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
				// The monster that casts: a husk that stands still, can't be hurt and never becomes a random Runebound.
				Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
				husk.snapTo(0.5, STAGE.getY() + 1, 3.0, 180, 0);
				husk.setNoAi(true);
				husk.setPermanentlyInvulnerable(true);
				husk.setPersistenceRequired();
				husk.addTag("wildercord.rolled");
				husk.addTag("wildercord.defence_caster");
				level.addFreshEntity(husk);
			});
			context.waitTicks(10);

			checkWardingIsAnArmourProtection(world);
			layers(context, world);
			bonusCap(context, world);
			spellguard(context, world);
			readout(context, world);
			theRealKill(context, world);
		}
	}

	// ------------------------------------------------------------------ Warding

	private static void checkWardingIsAnArmourProtection(TestSingleplayerContext world) {
		String problem = world.getServer().computeOnServer(server -> {
			var enchantments = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
			Holder<Enchantment> warding = enchantments.getOrThrow(SpellDefence.WARDING);
			Holder<Enchantment> protection = enchantments.getOrThrow(Enchantments.PROTECTION);
			if (warding.value().getMaxLevel() != 4) {
				return "Warding should go to IV (goes to " + warding.value().getMaxLevel() + ")";
			}
			if (Enchantment.areCompatible(warding, protection)) {
				return "Warding shouldn't sit beside Protection on one piece";
			}
			if (!warding.value().canEnchant(new ItemStack(Items.NETHERITE_CHESTPLATE)) || warding.value().canEnchant(new ItemStack(Items.DIAMOND_SWORD))) {
				return "Warding should go on armour, and only armour";
			}
			if (!warding.is(net.minecraft.tags.EnchantmentTags.IN_ENCHANTING_TABLE)) {
				return "Warding should come out of the enchanting table";
			}
			return null;
		});
		check(problem == null, problem);
	}

	// ------------------------------------------------------------------ the layers

	/** Every worst case in every outfit: each layer takes a measurable share off. Then Resistance on top of it all. */
	private static void layers(ClientGameTestContext context, TestSingleplayerContext world) {
		double[][] taken = new double[WORST.size() + 1][OUTFITS.length];
		for (int outfit = 0; outfit < OUTFITS.length; outfit++) {
			int o = outfit;
			world.getServer().runOnServer(server -> dress(player(server), o >= 1, o >= 2, o >= 3));
			// Armour's worth comes with the next tick's equipment check.
			context.waitTicks(3);
			for (int c = 0; c < WORST.size(); c++) {
				Case hit = WORST.get(c);
				taken[c][o] = world.getServer().computeOnServer(server -> measure(player(server), (level, player) ->
					SpellDefence.hurt(level, player, source(level, hit, husk(level), fake(level)), (float) hit.amount())));
			}
			// A real spell through the whole path: the husk casts Burst and Sonic Boom at a boss's power on Hard.
			taken[WORST.size()][o] = world.getServer().computeOnServer(server -> measure(player(server), (level, player) ->
				Runebound.cast(level, husk(level), List.of(Runes.BURST, Runes.SONIC_BOOM), 1.1)));
		}
		List<String> names = new ArrayList<>(WORST.stream().map(Case::name).toList());
		names.add("A husk's Burst and Sonic Boom, cast for real at boss power");
		for (int c = 0; c < names.size(); c++) {
			double[] row = taken[c];
			Wildercord.LOGGER.info("[defence] {}: {}", names.get(c), String.format(Locale.ROOT, "%.2f (nothing), %.2f (netherite), %.2f (+ Warding IV), %.2f (+ Warded)",
				row[0], row[1], row[2], row[3]));
			check(row[0] > 0, names.get(c) + " should hurt an unarmoured player (took " + row[0] + ")");
			// Armour keeps less of its worth against a huge hit, as it does against a blade (toughness helps), but always some.
			check(row[1] < row[0] * 0.92, names.get(c) + ": full netherite should take a share off (" + row[0] + " to " + row[1] + ")");
			check(row[2] < row[1] * 0.5, names.get(c) + ": Warding IV on every piece should take at least half again off (" + row[1] + " to " + row[2] + ")");
			check(row[3] < row[2] * 0.9, names.get(c) + ": Warded should take a tenth more off at the least (" + row[2] + " to " + row[3] + ")");
			check(row[3] > 0, names.get(c) + ": however well defended, a spell should still hurt (took " + row[3] + ")");
		}
		// Magic armour doesn't stop at all: full netherite takes about a third off.
		double[] magic = taken[3];
		check(magic[1] / magic[0] > 0.55 && magic[1] / magic[0] < 0.72,
			"full netherite should take a third or so off magic, not " + String.format(Locale.ROOT, "%.0f%%", 100 - 100 * magic[1] / magic[0]));
		// Vanilla's Resistance still works against spells, on top of everything else.
		world.getServer().runOnServer(server -> player(server).addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1200, 0)));
		double resisted = world.getServer().computeOnServer(server -> measure(player(server), (level, player) ->
			SpellDefence.hurt(level, player, source(level, WORST.get(3), husk(level), fake(level)), (float) WORST.get(3).amount())));
		Wildercord.LOGGER.info("[defence] {} with Resistance I on top: {}", WORST.get(3).name(), String.format(Locale.ROOT, "%.2f", resisted));
		check(Math.abs(resisted - magic[3] * 0.8) < 0.05, "Resistance I should take its fifth off a spell too (" + magic[3] + " to " + resisted + ")");
		world.getServer().runOnServer(server -> dress(player(server), false, false, false));
		context.waitTicks(3);
	}

	// ------------------------------------------------------------------ the bonus cap

	/** Harm with two Executes on a player under half health: four times over, held to the cap of two and a half. */
	private static void bonusCap(ClientGameTestContext context, TestSingleplayerContext world) {
		double taken = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			ready(player, BIG_HEALTH);
			player.setHealth(90);
			Effects.readyToHurt(player);
			Runebound.cast(level, husk(level), List.of(Runes.BURST, Runes.HARM, Runes.EXECUTE_MOD, Runes.EXECUTE_MOD), 1.0);
			return 90.0 - player.getHealth();
		});
		// Harm's 7, times 2.5 (not 4), and half again for Hard.
		double capped = 7 * 2.5 * 1.5;
		Wildercord.LOGGER.info("[defence] Harm with two Executes under half health: {} (uncapped it would be {})",
			String.format(Locale.ROOT, "%.2f", taken), String.format(Locale.ROOT, "%.2f", 7 * 4 * 1.5));
		check(Math.abs(taken - capped) < 0.5, "a hit's bonuses should be held to 2.5 against a player (took " + taken + ", capped is " + capped + ")");
		context.waitTicks(2);
	}

	// ------------------------------------------------------------------ the spellguard

	private static void spellguard(ClientGameTestContext context, TestSingleplayerContext world) {
		// A full-health player takes a PvP spell of two killing effects: the guard holds on the first, the second can't finish them.
		String saved = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			ready(player, 20);
			pvpSpell(level, player, true);
			if (!player.isAlive() || Math.abs(player.getHealth() - 2.0F) > 0.01F) {
				return "the spellguard should leave a full-health player on one heart (health " + player.getHealth() + ", alive " + player.isAlive() + ")";
			}
			Long held = player.getAttached(WildercordAttachments.SPELLGUARD);
			return held == null || held != level.getGameTime() ? "the spellguard should note when it held (" + held + ")" : null;
		});
		check(saved == null, saved);
		context.waitTicks(2);
		context.takeScreenshot(TestScreenshotOptions.of("defence_spellguard").disableCounterPrefix());
		int left = context.computeOnClient(mc -> SpellDefence.guardSeconds(mc.player));
		check(left > 50 && left <= 60, "the client should see the spellguard recharging for about a minute (" + left + " s)");

		// Recharging: a killing spell a second later finds no guard, and a totem answers instead (one killing blow: a totem
		// answers one).
		context.waitTicks(20);
		String recharging = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ready(player, 20);
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
			pvpSpell(player.level(), player, false);
			if (!player.isAlive() || !player.getOffhandItem().isEmpty()) {
				return "while the spellguard recharges, a killing spell should fall to the totem (alive " + player.isAlive() + ", offhand " + player.getOffhandItem() + ")";
			}
			return null;
		});
		check(recharging == null, recharging);

		// A minute on it's back: it holds again, and the totem stays in hand.
		context.waitTicks(20);
		String recharged = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			player.removeAllEffects();
			player.setAttached(WildercordAttachments.SPELLGUARD, level.getGameTime() - 20L * 60 - 1);
			if (SpellDefence.guardSeconds(player) != 0) {
				return "a minute after it held, the spellguard should be ready again";
			}
			ready(player, 20);
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
			pvpSpell(level, player, false);
			if (Math.abs(player.getHealth() - 2.0F) > 0.01F || player.getOffhandItem().isEmpty()) {
				return "the recharged spellguard should hold again, before any totem (health " + player.getHealth() + ", offhand " + player.getOffhandItem() + ")";
			}
			return null;
		});
		check(recharged == null, recharged);

		// Below its threshold (15 of 20 is 75%) the guard doesn't hold, even charged: the totem goes.
		context.waitTicks(20);
		String low = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			player.removeAllEffects();
			player.setAttached(WildercordAttachments.SPELLGUARD, level.getGameTime() - 20L * 60 - 1);
			ready(player, 20);
			player.setHealth(15);
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
			pvpSpell(level, player, false);
			if (!player.getOffhandItem().isEmpty() || SpellDefence.guardSeconds(player) != 0) {
				return "from 75% health the spellguard shouldn't hold (offhand " + player.getOffhandItem() + ", recharging " + SpellDefence.guardSeconds(player) + ")";
			}
			return null;
		});
		check(low == null, low);

		// A spell that deals /kill damage lands as magic: the guard still holds.
		context.waitTicks(20);
		String killSpell = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			player.removeAllEffects();
			player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
			player.setAttached(WildercordAttachments.SPELLGUARD, level.getGameTime() - 20L * 60 - 1);
			ready(player, 20);
			Effects.readyToHurt(player);
			SpellDefence.hurt(level, player, level.damageSources().source(DamageTypes.GENERIC_KILL, husk(level)), 1000);
			return player.isAlive() && Math.abs(player.getHealth() - 2.0F) < 0.01F ? null
				: "a spell's /kill damage shouldn't get past the defences (health " + player.getHealth() + ", alive " + player.isAlive() + ")";
		});
		check(killSpell == null, killSpell);
		context.waitTicks(20);
	}

	/** The Cord screen's spell-defence badge, in the best outfit, with its tooltip open. */
	private static void readout(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setAttached(WildercordAttachments.SPELLGUARD, player.level().getGameTime() - 20L * 60 - 1);
			dress(player, true, true, true);
			ready(player, 20);
		});
		context.waitTicks(5);
		context.setScreen(CordScreen::new);
		context.waitTicks(5);
		double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		double[] badge = context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).defencePoint());
		context.getInput().setCursorPos(badge[0] * scale, badge[1] * scale);
		context.waitTicks(5);
		List<String> lines = context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).defenceLines());
		Wildercord.LOGGER.info("[defence] Cord screen readout: {}", lines);
		String total = lines.size() > 1 ? lines.get(1) : "";
		int percent = Integer.parseInt(total.replaceAll("[^0-9]", "").isEmpty() ? "0" : total.replaceAll("[^0-9]", ""));
		check(percent >= 85 && percent < 100, "the readout should show the full outfit's share, well over 80% and never all (" + total + ")");
		check(lines.stream().anyMatch(l -> l.contains("Spellguard: ready")), "the readout should say the spellguard is ready (" + lines + ")");
		context.takeScreenshot(TestScreenshotOptions.of("defence_readout").disableCounterPrefix());
		context.runOnClient(mc -> mc.gui.setScreen(null));
		context.waitTicks(3);
	}

	/** However well defended and guarded, /kill kills. */
	private static void theRealKill(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> player(server).addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1200, 1)));
		world.getServer().runCommand("kill @a[type=player]");
		context.waitTicks(5);
		boolean dead = world.getServer().computeOnServer(server -> !player(server).isAlive());
		check(dead, "/kill should kill a player whatever their spell defences and spellguard");
		context.runOnClient(mc -> {
			mc.player.respawn();
			mc.gui.setScreen(null);
		});
		context.waitTicks(10);
	}

	// ------------------------------------------------------------------ helpers

	/** Back in the middle of the stage, standing still, health restored to {@code max}, nothing absorbing, no hurt cooldown. */
	private static void ready(ServerPlayer player, double max) {
		player.teleportTo(player.level(), 0.5, STAGE.getY() + 1, 0.5, java.util.Set.of(), 0, 10, false);
		player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(max);
		player.setHealth((float) max);
		player.setAbsorptionAmount(0);
		player.clearFire();
		Exposed.clear(player);
		Effects.readyToHurt(player);
	}

	/** What one strike takes from a player at full (big) health. */
	private static double measure(ServerPlayer player, BiConsumer<ServerLevel, ServerPlayer> strike) {
		ready(player, BIG_HEALTH);
		float before = player.getHealth();
		strike.accept(player.level(), player);
		return before - player.getHealth();
	}

	/** Another player's spell at a player, at power 6 (an endgame caster's, charged): Burst with Sonic Boom (and Harm, when {@code twice}). */
	private static void pvpSpell(ServerLevel level, ServerPlayer target, boolean twice) {
		FakePlayer caster = fake(level);
		caster.snapTo(target.getX(), target.getY(), target.getZ() - 2);
		List<RuneDef> spell = twice ? List.of(Runes.BURST, Runes.SONIC_BOOM, Runes.HARM) : List.of(Runes.BURST, Runes.SONIC_BOOM);
		CastEngine.cast(caster, SpellCompiler.compile(spell).root(), 1, new Heart.Bonuses(6.0, 1, 1, 1), false, null);
	}

	/** Puts on (or takes off) full netherite, with Warding IV on every piece or not, and the Warded effect or not. */
	private static void dress(ServerPlayer player, boolean netherite, boolean warding, boolean warded) {
		Holder<Enchantment> ward = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(SpellDefence.WARDING);
		Item[] pieces = {Items.NETHERITE_BOOTS, Items.NETHERITE_LEGGINGS, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_HELMET};
		EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
		for (int i = 0; i < slots.length; i++) {
			ItemStack piece = netherite ? new ItemStack(pieces[i]) : ItemStack.EMPTY;
			if (netherite && warding) {
				piece.enchant(ward, 4);
			}
			player.setItemSlot(slots[i], piece);
		}
		player.removeAllEffects();
		if (warded) {
			player.addEffect(new MobEffectInstance(WildercordEffects.WARDED, 1200, 0));
		}
	}

	/** The damage a worst case is dealt with, from the husk (a monster's) or from another player. */
	private static DamageSource source(ServerLevel level, Case hit, Mob husk, FakePlayer rival) {
		net.minecraft.world.entity.LivingEntity from = hit.pvp() ? rival : husk;
		return switch (hit.type()) {
			case "explosion" -> level.damageSources().explosion(from, from);
			case "lightning" -> level.damageSources().source(DamageTypes.LIGHTNING_BOLT, from);
			case "sonic_boom" -> level.damageSources().sonicBoom(from);
			default -> level.damageSources().indirectMagic(from, from);
		};
	}

	private static Mob husk(ServerLevel level) {
		for (net.minecraft.world.entity.Entity e : level.getAllEntities()) {
			if (e instanceof Mob mob && mob.entityTags().contains("wildercord.defence_caster")) {
				return mob;
			}
		}
		throw new AssertionError("the husk that casts is gone");
	}

	private static FakePlayer fake(ServerLevel level) {
		return FakePlayer.get(level);
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void check(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError(what);
		}
	}
}
