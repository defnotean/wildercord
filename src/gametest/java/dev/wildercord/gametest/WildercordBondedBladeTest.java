package dev.wildercord.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraPresence;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.Awakening;
import dev.wildercord.aura.BladeBond;
import dev.wildercord.aura.BladeCeremony;
import dev.wildercord.aura.BladeRegistry;
import dev.wildercord.aura.BladeRules;
import dev.wildercord.aura.BladeTraits;
import dev.wildercord.aura.BondedBlades;
import dev.wildercord.aura.Momentum;
import dev.wildercord.aura.Stance;
import dev.wildercord.aura.StanceRules;
import dev.wildercord.aura.StringRules;
import dev.wildercord.aura.SwordStrings;
import dev.wildercord.aura.Ways;
import dev.wildercord.cast.LeyWalker;
import dev.wildercord.cast.PowerPlaces;
import dev.wildercord.cast.WildercordEntities;
import dev.wildercord.client.AuraScreen;
import dev.wildercord.client.WildercordKeys;
import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.world.LeyLines;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * The bonded blade, step 9 of the aura overhaul, at a real ley crossing and on a stone platform in the sky:
 * <ul>
 *   <li><b>The ceremony</b>: nothing away from a ley crossing; at one, the breathing stance held with the real keys and a diamond sword in hand
 *       runs ten seconds (kindling, joining, sealing, filmed from in front and through the swordsman's eyes) and bonds it; moving breaks it; a
 *       second blade won't bond while one is;</li>
 *   <li><b>The look</b>: each tier's glow in hand from in front and in first person, by day and by night, and cold in another's hands;</li>
 *   <li><b>Resonance</b>: an art landing and a foe felled with real swings grow it (a training dummy gives nothing), and the Named moment;</li>
 *   <li><b>The page</b>: the Blade tab before a bond and with one, a name typed and given, a suggestion asked for, a trait chosen from its three
 *       cards, released with two clicks;</li>
 *   <li><b>Traits</b>: Well-Worn Verse's price and rest (the client agreeing), Riposte, Gravewarden, Sundering Steel (half on a player), Long
 *       Crescent, and Soulforged half again;</li>
 *   <li><b>Dropped</b>: lying on the ground it glows, nobody else picks it up, lava, a blast and a hopper leave it, its swordsman takes it back;
 *       out of the world it comes home;</li>
 *   <li><b>Death</b>: kept through a death (Curse of Vanishing too, and from the cursor), never dropped, never twice; and under keepInventory;</li>
 *   <li><b>Theft and duplication</b>: not taken from a chest by anyone else, not pulled by a hopper, home from another's hands and from inside a
 *       box they carry, kept for a swordsman who isn't here, never used up at an anvil, a grindstone or the repair recipe, never broken; and at
 *       every step exactly one of it in the world;</li>
 *   <li><b>The passing</b>: a master in the stance, a disciple kneeling before them, the rite, the blade in the disciple's hands with its story,
 *       sleeping until they grow into it; and back;</li>
 *   <li><b>The tooltip</b>: its lines, and its story with Shift held.</li>
 * </ul>
 * Screenshots: {@code blade_*}.
 *
 * <p>{@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and {@code WILDERCORD_SHOWCASE} skip it; {@code WILDERCORD_BLADE=a,b} plays only
 * the scenes named (ceremony, glow, resonance, page, traits, dropped, death, theft, pass, tooltip).</p>
 */
public class WildercordBondedBladeTest implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 190, 0);
	private static final String TAG = "wildercord.blade_test";
	private static final UUID DISCIPLE_ID = UUID.nameUUIDFromBytes("wildercord-blade-disciple".getBytes(java.nio.charset.StandardCharsets.UTF_8));
	private static final int FULL = 14;
	private static boolean hooked;
	private static final List<String> HOOKED = java.util.Collections.synchronizedList(new ArrayList<>());

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		if (!hooked) {
			hooked = true;
			// Step 10's masters and disciples will say who may receive a blade; here, the test's own disciple.
			AuraApi.allowBladePassing((master, disciple) -> disciple.getUUID().equals(DISCIPLE_ID));
			AuraApi.onBlade(new AuraApi.BladeHook() {
				@Override
				public void bonded(ServerPlayer player, ItemStack blade) {
					HOOKED.add("bonded");
				}

				@Override
				public void tiered(ServerPlayer player, ItemStack blade, int tier) {
					HOOKED.add("tiered:" + tier);
				}

				@Override
				public void passed(ServerPlayer from, ServerPlayer to, ItemStack blade) {
					HOOKED.add("passed:" + to.getGameProfile().name());
				}

				@Override
				public void released(ServerPlayer player, UUID bond) {
					HOOKED.add("released");
				}
			});
		}
		String only = System.getenv("WILDERCORD_BLADE");
		Set<String> scenes = only == null || only.isBlank() ? null : Set.of(only.split(","));
		context.runOnClient(mc -> {
			mc.getWindow().setWindowed(1920, 1080);
			mc.options.guiScale().set(2);
			mc.resizeGui();
			mc.options.toggleCrouch().set(false);
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			MagicQuality.stringIndicator = MagicQuality.StringIndicator.CROSSHAIR;
		});
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule natural_regeneration false");
			world.getServer().runCommand("gamerule keep_inventory false");
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("time set 3000");
			world.getServer().runCommand("weather clear");
			stage(world);
			context.waitTicks(10);
			List<String> failures = new ArrayList<>();
			scene(scenes, "ceremony", failures, "the ceremony", () -> ceremony(context, world));
			scene(scenes, "glow", failures, "the look", () -> glow(context, world));
			scene(scenes, "resonance", failures, "resonance", () -> resonance(context, world));
			scene(scenes, "page", failures, "the page", () -> page(context, world));
			scene(scenes, "traits", failures, "traits", () -> traits(context, world));
			scene(scenes, "dropped", failures, "dropped", () -> dropped(context, world));
			scene(scenes, "death", failures, "death", () -> death(context, world));
			scene(scenes, "theft", failures, "theft and duplication", () -> theft(context, world));
			scene(scenes, "pass", failures, "the passing", () -> pass(context, world));
			scene(scenes, "tooltip", failures, "the tooltip", () -> tooltip(context, world));
			if (!failures.isEmpty()) {
				throw new AssertionError("The bonded blade went wrong:\n  " + String.join("\n  ", failures));
			}
		} finally {
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				if (mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
			context.getInput().releaseKey(o -> o.keyShift);
			context.getInput().releaseShift();
			AuraScreen.listArts(false);
		}
	}

	private static void scene(Set<String> scenes, String id, List<String> failures, String what, Runnable test) {
		if (scenes == null || scenes.contains(id)) {
			try {
				test.run();
			} catch (AssertionError | RuntimeException e) {
				failures.add(what + ": " + e);
				System.out.println("[bonded blade test] " + what + " went wrong: " + e);
			}
		}
	}

	private static void check(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError(what);
		}
	}

	// ------------------------------------------------------------------ the ceremony

	private static void ceremony(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.EDGE);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
			return null;
		});
		context.waitTicks(5);
		// Away from a ley crossing the stance is only the stance.
		check(!on(world, player -> PowerPlaces.isPlaceOfPower(player.level(), player.blockPosition())), "the stage isn't a place of power");
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(70);
		check(!on(world, BladeCeremony::underWay), "no ceremony away from a ley crossing");
		context.getInput().releaseKey(o -> o.keyShift);
		check(on(world, player -> !BondedBlades.bonded(player.getMainHandItem())), "nothing bonded away from a ley crossing");
		// At the heart of a ley crossing.
		goToCrossing(context, world);
		int before = BondedBlades.bondsMade();
		HOOKED.clear();
		frontView(context, world, 205, -26, 4.8);
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(45);
		check(on(world, BladeCeremony::underWay), "the stance at a ley crossing with a blade in hand begins the ceremony");
		check(context.computeOnClient(mc -> mc.player.getAttached(BondedBlades.RITE) != null), "the client hears the ceremony began (its HUD, the kindling)");
		shot(context, "blade_ceremony_kindling_tp");
		context.waitTicks(55);
		shot(context, "blade_ceremony_joining_tp");
		context.waitTicks(70);
		shot(context, "blade_ceremony_sealing_tp");
		waitFor(context, () -> BondedBlades.bondsMade() > before, 60, "the ceremony should seal the bond in ten seconds");
		context.waitTicks(3);
		shot(context, "blade_ceremony_sealed_tp");
		context.getInput().releaseKey(o -> o.keyShift);
		String bonded = on(world, player -> {
			ItemStack hand = player.getMainHandItem();
			BladeBond b = BondedBlades.bond(hand);
			if (b == null) {
				return "the sword in hand should carry the bond";
			}
			if (!b.ownedBy(player.getUUID()) || b.tier() != BladeRules.BONDED) {
				return "bonded to its swordsman, Bonded (" + b + ")";
			}
			if (!BladeRegistry.of(player.level().getServer()).activeFor(b.id(), player.getUUID())) {
				return "the world's registry holds the bond";
			}
			if (!BondedBlades.state(player).bond().equals(b.id().toString())) {
				return "the swordsman's own record holds the bond";
			}
			if (!b.origin().how().equals("ceremony") || b.origin().biome().isEmpty() || b.origin().day() < 1) {
				return "its story begins where and when it was bonded (" + b.origin() + ")";
			}
			if (b.color() != Aura.color(player)) {
				return "it takes on its swordsman's colour";
			}
			if (player.hasAttached(BondedBlades.RITE)) {
				return "the ceremony is over";
			}
			return dev.wildercord.player.Heart.grimoire(player).contains("aura:bond") ? null : "the first bond goes into the Grimoire";
		});
		check(bonded == null, bonded);
		check(HOOKED.contains("bonded"), "the blade hooks hear of it (" + HOOKED + ")");
		check(context.computeOnClient(mc -> BondedBlades.bonded(mc.player.getMainHandItem())), "the client sees the bond on the blade");
		context.waitTicks(60);
		shot(context, "blade_bonded_tp");
		// A second blade won't bond while one is: the stance at the crossing with another sword does nothing.
		on(world, player -> {
			player.getInventory().setItem(1, player.getMainHandItem().copy());
			player.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
			player.getInventory().setSelectedSlot(0);
			return null;
		});
		context.waitTicks(5);
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(70);
		check(!on(world, BladeCeremony::underWay), "a second blade won't begin a ceremony while one is bonded");
		context.getInput().releaseKey(o -> o.keyShift);
		// Released, the stance with a new blade begins again; moving breaks it.
		on(world, player -> {
			BondedBlades.release(player);
			player.getInventory().setItem(1, ItemStack.EMPTY);
			player.getInventory().setItem(0, new ItemStack(Items.NETHERITE_SWORD));
			return null;
		});
		context.waitTicks(5);
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(60);
		check(on(world, BladeCeremony::underWay), "after a release a new blade can be bonded");
		on(world, player -> {
			player.teleportTo(player.level(), player.getX() + 1.2, player.getY(), player.getZ(), Set.<Relative>of(), player.getYRot(), player.getXRot(), false);
			return null;
		});
		context.waitTicks(4);
		check(!on(world, BladeCeremony::underWay), "moving breaks the ceremony");
		check(on(world, player -> !BondedBlades.bonded(player.getMainHandItem())), "a broken ceremony bonds nothing");
		context.getInput().releaseKey(o -> o.keyShift);
		// Through the swordsman's own eyes: the same rite, low and thin.
		on(world, player -> {
			double[] heart = crossingHeart(player);
			player.teleportTo(player.level(), heart[0], player.getY(), heart[1], Set.<Relative>of(), 20.0F, 26.0F, false);
			player.setDeltaMovement(Vec3.ZERO);
			return null;
		});
		firstPerson(context, world, 20.0F, 26.0F);
		context.waitTicks(10);
		int again = BondedBlades.bondsMade();
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(45);
		shot(context, "blade_ceremony_kindling_fp");
		context.waitTicks(55);
		shot(context, "blade_ceremony_joining_fp");
		context.waitTicks(70);
		shot(context, "blade_ceremony_sealing_fp");
		waitFor(context, () -> BondedBlades.bondsMade() > again, 60, "the ceremony in first person bonds too");
		context.waitTicks(3);
		shot(context, "blade_ceremony_sealed_fp");
		context.getInput().releaseKey(o -> o.keyShift);
		context.waitTicks(40);
		leaveCrossing(context, world);
	}

	// ------------------------------------------------------------------ the look

	private static void glow(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		bondNew(world, "ember", AuraRules.EDGE, new ItemStack(Items.DIAMOND_SWORD));
		context.waitTicks(10);
		int tierUps = BondedBlades.tierUps();
		for (int tier = BladeRules.BONDED; tier <= BladeRules.MAX_TIER; tier++) {
			int t = tier;
			if (t == BladeRules.SOULFORGED) {
				// The moment itself, from behind (in third person before it comes, so the swordsman sees its column).
				thirdPerson(context, world, 0, 14, 4.5, false);
			}
			on(world, player -> {
				// Each tier shown at the stage that first lets it show (Bonded and Named at Edge, Awakened at Form, Soulforged at Sovereign).
				setAura(player, "ember", Math.max(AuraRules.EDGE, BladeRules.gate(t)), false);
				if (t >= BladeRules.SOULFORGED) {
					BondedBlades.addHistory(player, Map.of(BladeRules.BOSSES, 1), Map.of());
				}
				BondedBlades.setResonance(player, BladeRules.threshold(t));
				return null;
			});
			context.waitTicks(4);
			String grew = on(world, player -> {
				BladeBond b = BondedBlades.bond(player.getMainHandItem());
				if (b == null || b.tier() != t) {
					return "the blade should reach tier " + t + " (" + (b == null ? "no bond" : b.tier()) + ")";
				}
				if (t >= BladeRules.NAMED && (b.shownName().isEmpty() || !player.getMainHandItem().getHoverName().getString().equals(b.shownName()))) {
					return "at Named it takes a name of its own and goes by it (" + b.growth().name() + ")";
				}
				if (t >= BladeRules.AWAKENED && b.growth().offer().isEmpty()) {
					return "at Awakened it offers its traits";
				}
				if (t > BladeRules.BONDED && !dev.wildercord.player.Heart.grimoire(player).contains("aura:blade_" + BladeRules.tierId(t))) {
					return "each tier goes into the Grimoire";
				}
				return null;
			});
			check(grew == null, grew);
			if (t == BladeRules.SOULFORGED) {
				waitFor(context, () -> BondedBlades.tierUps() >= tierUps + 3, 30, "it reaches Soulforged");
				context.waitTicks(3);
				shot(context, "blade_tier_soulforged_moment_tp");
			}
			context.waitTicks(70);
			frontView(context, world, 150, 10, 2.6);
			context.waitTicks(4);
			shot(context, "blade_glow_" + BladeRules.tierId(t) + "_tp");
			firstPerson(context, world, 0.0F, 8.0F);
			context.waitTicks(4);
			shot(context, "blade_glow_" + BladeRules.tierId(t) + "_fp");
		}
		check(BondedBlades.tierUps() >= tierUps + 3, "three tiers reached (" + (BondedBlades.tierUps() - tierUps) + ")");
		// By night: the Awakened and Soulforged glow.
		world.getServer().runCommand("time set 18000");
		context.waitTicks(5);
		frontView(context, world, 150, 10, 2.6);
		context.waitTicks(4);
		shot(context, "blade_glow_soulforged_night_tp");
		firstPerson(context, world, 0.0F, 8.0F);
		context.waitTicks(4);
		shot(context, "blade_glow_soulforged_night_fp");
		world.getServer().runCommand("time set 3000");
		// In another's hands it's cold, and only steel to them; then it goes home.
		on(world, player -> {
			Rival r = rival(player);
			r.setItemInHand(InteractionHand.MAIN_HAND, player.getMainHandItem().copy());
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			// A fake player isn't ticked, so what it holds is told to the client by hand.
			player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket(r.getId(),
				List.of(com.mojang.datafixers.util.Pair.of(net.minecraft.world.entity.EquipmentSlot.MAINHAND, r.getMainHandItem().copy()))));
			return null;
		});
		// Face to face with whoever holds it: in their hand it's cold, a grey vein and nothing more.
		on(world, player -> {
			Rival r = rival(player);
			r.snapTo(r.getX(), r.getY(), r.getZ(), 180, 0);
			r.setYHeadRot(180);
			r.setYBodyRot(180);
			player.teleportTo(player.level(), r.getX() + 0.35, player.getY(), r.getZ() - 2.1, Set.<Relative>of(), 0.0F, 14.0F, false);
			return null;
		});
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		context.waitTicks(8);
		shot(context, "blade_cold_in_another_hand_tp");
		String cold = on(world, player -> {
			Rival r = rival(player);
			if (Aura.holdsWeapon(r)) {
				return "someone else's bonded blade is no aura weapon in a stranger's hands";
			}
			int returns = BondedBlades.returns();
			BondedBlades.scan(r, BladeRegistry.of(player.level().getServer()), true);
			if (BondedBlades.returns() != returns + 1 || BondedBlades.bonded(r.getMainHandItem())) {
				return "it slips out of the stranger's hands";
			}
			return copies(player) == 1 && BondedBlades.slotOf(player) >= 0 ? null : "and home to its swordsman, the only one of it (" + copies(player) + ")";
		});
		check(cold == null, cold);
		dropRival(world);
		firstPerson(context, world, 0.0F, 8.0F);
	}

	// ------------------------------------------------------------------ resonance

	private static void resonance(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		bondNew(world, "ember", AuraRules.EDGE, new ItemStack(Items.DIAMOND_SWORD));
		on(world, player -> {
			ServerLevel level = player.level();
			for (int i = 0; i < 3; i++) {
				spawn(level, EntityTypes.HUSK, at(-0.8 + 0.8 * i, 2.4), 60, 180);
			}
			return null;
		});
		context.waitTicks(20);
		float before = on(world, player -> BondedBlades.bond(player.getMainHandItem()).growth().resonance());
		// The First Art with the real keys (two swings and a low one), landing on the husks.
		for (int t = 0; t < 30 && context.computeOnClient(mc -> mc.player.getAttackStrengthScale(0.5F) < 0.99F); t++) {
			context.waitTicks(1);
		}
		swing(context);
		context.waitTicks(FULL);
		swing(context);
		context.waitTicks(FULL);
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(2);
		swing(context);
		context.waitTicks(2);
		context.getInput().releaseKey(o -> o.keyShift);
		context.waitTicks(10);
		// And a foe felled with a blow.
		// Away from the art's fire (it fells what stands in it before the blade can), a fresh foe squarely before the blade.
		on(world, player -> {
			player.teleportTo(player.level(), STAGE.getX() + 6.5, STAGE.getY(), STAGE.getZ() - 6.5, Set.<Relative>of(), 0.0F, 8.0F, false);
			return null;
		});
		context.waitTicks(FULL);
		on(world, player -> {
			spawn(player.level(), EntityTypes.HUSK, new Vec3(STAGE.getX() + 6.5, STAGE.getY(), STAGE.getZ() - 4.6), 1, 180);
			return null;
		});
		context.waitTicks(4);
		swing(context);
		context.waitTicks(30);
		String grew = on(world, player -> {
			BladeBond b = BondedBlades.bond(player.getMainHandItem());
			if (b == null) {
				return "the blade is gone";
			}
			if (b.growth().resonance() <= before) {
				return "an art landing and a foe felled grow its resonance (" + before + " to " + b.growth().resonance() + ")";
			}
			if (b.history().count(BladeRules.KILLS) < 1) {
				StringBuilder seen = new StringBuilder();
				for (Mob foe : foes(player)) {
					seen.append(String.format(java.util.Locale.ROOT, " %.1f at %.1f,%.1f;", foe.getHealth(), foe.getX() - player.getX(), foe.getZ() - player.getZ()));
				}
				return "it counts the foe felled (" + b.history().counts() + "; foes:" + seen + " facing " + player.getYRot() + ")";
			}
			if (b.history().count(BladeRules.ARTS) < 1 || !b.history().arts().containsKey("kindling_draw")) {
				return "it counts the art and remembers which (" + b.history().arts() + ")";
			}
			return null;
		});
		check(grew == null, grew);
		// A training dummy gives it nothing.
		kill(world);
		on(world, player -> {
			stand(player);
			ServerLevel level = player.level();
			dev.wildercord.cast.TrainingDummy dummy = WildercordEntities.TRAINING_DUMMY.create(level, EntitySpawnReason.COMMAND);
			Vec3 p = at(0, 2.4);
			dummy.snapTo(p.x, p.y, p.z, 180, 0);
			dummy.addTag(TAG);
			level.addFreshEntity(dummy);
			return null;
		});
		context.waitTicks(10);
		float beforeDummy = on(world, player -> BondedBlades.bond(player.getMainHandItem()).growth().resonance());
		for (int i = 0; i < 4; i++) {
			context.waitTicks(FULL);
			swing(context);
		}
		context.waitTicks(30);
		float afterDummy = on(world, player -> BondedBlades.bond(player.getMainHandItem()).growth().resonance());
		check(Math.abs(afterDummy - beforeDummy) < 1.0E-3, "a training dummy gives a blade nothing (" + beforeDummy + " to " + afterDummy + ")");
		kill(world);
		// The Named moment: just short of it, a foe felled carries it over.
		on(world, player -> {
			BondedBlades.setResonance(player, BladeRules.threshold(BladeRules.NAMED) - 0.2);
			spawn(player.level(), EntityTypes.HUSK, at(0, 2.4), 1, 180);
			return null;
		});
		context.waitTicks(15);
		int tierUps = BondedBlades.tierUps();
		thirdPerson(context, world, 0, 14, 4.5, false);
		context.waitTicks(FULL);
		swing(context);
		waitFor(context, () -> BondedBlades.tierUps() > tierUps, 40, "a foe felled carries it to Named");
		context.waitTicks(2);
		shot(context, "blade_named_moment_tp");
		String named = on(world, player -> {
			BladeBond b = BondedBlades.bond(player.getMainHandItem());
			return b != null && b.tier() == BladeRules.NAMED && !b.shownName().isEmpty() ? null : "Named, with a name of its own (" + b + ")";
		});
		check(named == null, named);
		firstPerson(context, world, 0.0F, 8.0F);
	}

	// ------------------------------------------------------------------ the page

	private static void page(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.EDGE);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
			return null;
		});
		context.waitTicks(5);
		// Before a bond: how it's made.
		context.runOnClient(mc -> AuraScreen.listArts(false));
		context.setScreen(() -> new AuraScreen(null));
		context.waitTicks(3);
		click(context, AuraScreen::bladeTabPoint);
		context.waitTicks(3);
		check(context.computeOnClient(mc -> AuraScreen.showingBlade()), "clicking the tab should open the Blade page");
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(2);
		shot(context, "blade_page_unbonded");
		context.setScreen(() -> null);
		// Bonded and Named.
		on(world, player -> {
			BondedBlades.bond(player, InteractionHand.MAIN_HAND, "test");
			BondedBlades.setResonance(player, BladeRules.threshold(BladeRules.NAMED) + 40);
			BondedBlades.addHistory(player, Map.of(BladeRules.KILLS, 85, BladeRules.ARTS, 64, BladeRules.FINISHERS, 9, BladeRules.GUARDS, 21,
				BladeRules.UNDEAD, 40), Map.of("kindling_draw", 30, "rising_cinders", 18, "backdraft", 16));
			return null;
		});
		context.waitTicks(25);
		context.runOnClient(mc -> AuraScreen.showBlade(true));
		context.setScreen(() -> new AuraScreen(null));
		context.waitTicks(4);
		String suggested = context.computeOnClient(mc -> BondedBlades.bond(mc.player.getMainHandItem()).shownName());
		check(!suggested.isEmpty(), "Named, it has a name of its own");
		clickBlade(context, "name");
		// Clear the name and type another (an invisible mark in it never gets in).
		for (int i = 0; i < 30; i++) {
			context.getInput().pressKey(InputConstants.KEY_BACKSPACE);
		}
		context.getInput().typeChars("Ashen Vow​");
		context.getInput().pressKey(InputConstants.KEY_RETURN);
		context.waitTicks(2);
		clickBlade(context, "name_it");
		context.waitTicks(6);
		String typed = on(world, player -> BondedBlades.bond(player.getMainHandItem()).growth().name());
		check(typed.equals("Ashen Vow"), "the name typed is given, made safe (" + typed + ")");
		// It suggests another.
		clickBlade(context, "suggest");
		context.waitTicks(2);
		String draft = context.computeOnClient(mc -> dev.wildercord.client.AuraScreen.showingBlade() ? bladeDraft() : "");
		check(!draft.isEmpty() && !draft.equals("Ashen Vow"), "Suggest offers a name of its own (" + draft + ")");
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(2);
		shot(context, "blade_page_named");
		context.setScreen(() -> null);
		// Awakened: three cards; the one its habits show most is offered, chosen with a click.
		on(world, player -> {
			setAura(player, "ember", AuraRules.FORM, false);
			BondedBlades.addHistory(player, Map.of(BladeRules.GUARDS, 70, BladeRules.BROKEN_STANCES, 40, BladeRules.KILLS, 300), Map.of("kindling_draw", 60));
			BondedBlades.setResonance(player, BladeRules.threshold(BladeRules.AWAKENED) + 30);
			return null;
		});
		context.waitTicks(25);
		List<String> offer = on(world, player -> BondedBlades.bond(player.getMainHandItem()).growth().offer());
		check(offer.size() == BladeRules.OFFER && offer.contains(BladeRules.RIPOSTE), "it offers three, Riposte among them (" + offer + ")");
		context.runOnClient(mc -> AuraScreen.showBlade(true));
		context.setScreen(() -> new AuraScreen(null));
		context.waitTicks(4);
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(2);
		shot(context, "blade_page_offer");
		clickBlade(context, "trait:" + BladeRules.RIPOSTE);
		context.waitTicks(6);
		String chosen = on(world, player -> BondedBlades.bond(player.getMainHandItem()).growth().trait());
		check(chosen.equals(BladeRules.RIPOSTE), "a card clicked is the trait it takes (" + chosen + ")");
		hoverBlade(context, "trait:" + BladeRules.RIPOSTE);
		context.waitTicks(2);
		shot(context, "blade_page_trait");
		// Released with two clicks: only steel again, remembering whose it was.
		context.getInput().setCursorPos(4, 4);
		clickBlade(context, "release");
		check(on(world, player -> BondedBlades.state(player).bonded()), "one click only asks");
		clickBlade(context, "release");
		context.waitTicks(6);
		String released = on(world, player -> {
			ItemStack hand = player.getMainHandItem();
			if (BondedBlades.bonded(hand) || BondedBlades.state(player).bonded()) {
				return "two clicks release the bond";
			}
			return hand.has(BondedBlades.FORMER) ? null : "it remembers whose it was";
		});
		check(released == null, released);
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(2);
		shot(context, "blade_page_released");
		context.setScreen(() -> null);
		// Away: bonded, but lying in a chest.
		on(world, player -> {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
			BondedBlades.bond(player, InteractionHand.MAIN_HAND, "test");
			BondedBlades.setResonance(player, BladeRules.threshold(BladeRules.NAMED));
			return null;
		});
		context.waitTicks(25);
		on(world, player -> {
			BlockPos chest = STAGE.offset(3, 0, 3);
			player.level().setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
			((ChestBlockEntity) player.level().getBlockEntity(chest)).setItem(0, player.getMainHandItem().copy());
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			return null;
		});
		context.waitTicks(30);
		check(on(world, player -> BondedBlades.state(player).away()), "its swordsman's record knows it's away");
		context.runOnClient(mc -> AuraScreen.showBlade(true));
		context.setScreen(() -> new AuraScreen(null));
		context.waitTicks(4);
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(2);
		shot(context, "blade_page_away");
		context.setScreen(() -> null);
		context.runOnClient(mc -> AuraScreen.listArts(false));
	}

	private static String bladeDraft() {
		try {
			java.lang.reflect.Method m = Class.forName("dev.wildercord.client.BladePage").getDeclaredMethod("draftName");
			m.setAccessible(true);
			return (String) m.invoke(null);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("couldn't read the page's name: " + e);
		}
	}

	// ------------------------------------------------------------------ traits

	private static void traits(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		bondNew(world, "ember", AuraRules.FORM, new ItemStack(Items.DIAMOND_SWORD));
		on(world, player -> {
			BondedBlades.setResonance(player, BladeRules.threshold(BladeRules.AWAKENED));
			BondedBlades.addHistory(player, Map.of(), Map.of("kindling_draw", 40));
			BondedBlades.forceTrait(player, BladeRules.WELL_WORN);
			spawn(player.level(), EntityTypes.HUSK, at(0, 2.4), 40, 180);
			spawn(player.level(), EntityTypes.PIG, at(1.5, 2.4), 10, 180);
			return null;
		});
		context.waitTicks(25);
		// Well-Worn Verse: its favourite art cheaper and back sooner, on the server and as the client predicts.
		String worn = on(world, player -> {
			AuraApi.StringArt art = AuraApi.artOf(player, "kindling_draw").orElse(null);
			if (art == null) {
				return "the First Art should be the player's";
			}
			player.removeAttached(Momentum.MOMENTUM);
			double price = SwordStrings.price(player, art);
			double expected = art.cost() * BladeRules.WELL_WORN_PRICE;
			if (Math.abs(price - expected) > 1.0E-6) {
				return "its favourite art costs " + BladeRules.WELL_WORN_PRICE + " of its price (" + price + " of " + art.cost() + ")";
			}
			if (SwordStrings.rest(player, art) != Math.round(art.cooldownTicks() * BladeRules.WELL_WORN_REST)) {
				return "and rests less (" + SwordStrings.rest(player, art) + " of " + art.cooldownTicks() + ")";
			}
			AuraApi.StringArt other = AuraApi.artOf(player, "rising_cinders").orElse(null);
			return other == null || Math.abs(SwordStrings.price(player, other) - other.cost()) < 1.0E-6 ? null : "only its favourite";
		});
		check(worn == null, worn);
		double clientPrice = context.computeOnClient(mc -> {
			AuraApi.StringArt art = AuraApi.artOf(mc.player, "kindling_draw").orElseThrow();
			return SwordStrings.price(mc.player, art) / art.cost();
		});
		check(Math.abs(clientPrice - BladeRules.WELL_WORN_PRICE) < 1.0E-6, "the client predicts the same price (" + clientPrice + ")");
		// Riposte: the blow after a perfect guard; half on a player.
		String riposte = on(world, player -> {
			BondedBlades.forceTrait(player, BladeRules.RIPOSTE);
			Mob husk = foes(player).getFirst();
			if (BladeTraits.coat(player, husk) != 1.0) {
				return "no Riposte without a perfect guard";
			}
			try {
				java.lang.reflect.Method guarded = BladeTraits.class.getDeclaredMethod("guarded", ServerPlayer.class);
				guarded.setAccessible(true);
				guarded.invoke(null, player);
			} catch (ReflectiveOperationException e) {
				return "couldn't guard: " + e;
			}
			if (!BladeTraits.riposteReady(player)) {
				return "a perfect guard readies it";
			}
			double f = BladeTraits.coat(player, husk);
			if (Math.abs(f - BladeRules.RIPOSTE_DAMAGE) > 1.0E-9) {
				return "the next coated blow lands harder (" + f + ")";
			}
			if (BladeTraits.riposteReady(player)) {
				return "spent by the blow it answers";
			}
			try {
				java.lang.reflect.Method guarded = BladeTraits.class.getDeclaredMethod("guarded", ServerPlayer.class);
				guarded.setAccessible(true);
				guarded.invoke(null, player);
			} catch (ReflectiveOperationException e) {
				return "couldn't guard: " + e;
			}
			double pvp = BladeTraits.coat(player, rival(player));
			return Math.abs(pvp - BladeRules.scaled(BladeRules.RIPOSTE_DAMAGE, 0.5)) < 1.0E-9 ? null : "half that against a player (" + pvp + ")";
		});
		check(riposte == null, riposte);
		// Gravewarden: the undead only, never a player.
		String grave = on(world, player -> {
			BondedBlades.forceTrait(player, BladeRules.GRAVEWARDEN);
			Mob husk = foes(player).getFirst();
			Mob pig = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16), m -> m.getType() == EntityTypes.PIG).getFirst();
			double undead = BladeTraits.coat(player, husk);
			double other = BladeTraits.coat(player, pig);
			double pvp = BladeTraits.coat(player, rival(player));
			return Math.abs(undead - BladeRules.GRAVEWARDEN_DAMAGE) < 1.0E-9 && other == 1.0 && pvp == 1.0 ? null
				: "harder on the undead only (" + undead + ", " + other + ", " + pvp + ")";
		});
		check(grave == null, grave);
		// Sundering Steel: stance worn harder, half on a player; a guard's own wear untouched.
		String sunder = on(world, player -> {
			BondedBlades.forceTrait(player, BladeRules.SUNDERING_STEEL);
			Mob husk = foes(player).getFirst();
			double foe = invokeStance(player, husk, 10, StanceRules.Source.ART);
			double pvp = invokeStance(player, rival(player), 10, StanceRules.Source.ART);
			double guard = invokeStance(player, husk, 10, StanceRules.Source.GUARD);
			return Math.abs(foe - 11.2) < 1.0E-6 && Math.abs(pvp - 10.6) < 1.0E-6 && Math.abs(guard - 10) < 1.0E-6 ? null
				: "stance worn a ninth harder, half that on a player, never a guard's (" + foe + ", " + pvp + ", " + guard + ")";
		});
		check(sunder == null, sunder);
		// Long Crescent, and half again at Soulforged; nothing at all with another blade in hand.
		String crescent = on(world, player -> {
			BondedBlades.forceTrait(player, BladeRules.LONG_CRESCENT);
			if (Math.abs(BladeTraits.slashPrice(player) - BladeRules.LONG_CRESCENT_PRICE) > 1.0E-9
					|| Math.abs(BladeTraits.slashReach(player) - BladeRules.LONG_CRESCENT_REACH) > 1.0E-9) {
				return "Aura Slash cheaper and further (" + BladeTraits.slashPrice(player) + ", " + BladeTraits.slashReach(player) + ")";
			}
			setAura(player, "ember", AuraRules.SOVEREIGN, false);
			BondedBlades.addHistory(player, Map.of(BladeRules.BOSSES, 1), Map.of());
			BondedBlades.setResonance(player, BladeRules.threshold(BladeRules.SOULFORGED));
			if (Math.abs(BladeTraits.slashPrice(player) - BladeRules.scaled(BladeRules.LONG_CRESCENT_PRICE, BladeRules.SOULFORGED_TRAIT)) > 1.0E-9) {
				return "Soulforged, half again as strong (" + BladeTraits.slashPrice(player) + ")";
			}
			ItemStack blade = player.getMainHandItem();
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			player.getInventory().setItem(5, blade);
			String held = BondedBlades.heldTrait(player);
			player.getInventory().setItem(5, ItemStack.EMPTY);
			player.setItemInHand(InteractionHand.MAIN_HAND, blade);
			return held.isEmpty() ? null : "a trait only works with its blade in hand (" + held + ")";
		});
		check(crescent == null, crescent);
		dropRival(world);
	}

	private static double invokeStance(ServerPlayer player, net.minecraft.world.entity.LivingEntity target, double wear, StanceRules.Source source) {
		try {
			java.lang.reflect.Method m = BladeTraits.class.getDeclaredMethod("stance", ServerPlayer.class, net.minecraft.world.entity.LivingEntity.class, double.class,
				StanceRules.Source.class);
			m.setAccessible(true);
			return (double) m.invoke(null, player, target, wear, source);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("couldn't weigh the stance: " + e);
		}
	}

	// ------------------------------------------------------------------ dropped

	private static void dropped(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		bondNew(world, "rime", AuraRules.EDGE, new ItemStack(Items.DIAMOND_SWORD));
		on(world, player -> {
			BondedBlades.setResonance(player, BladeRules.threshold(BladeRules.NAMED));
			return null;
		});
		context.waitTicks(30);
		// Thrown down with the real key.
		context.getInput().pressKey(o -> o.keyDrop);
		context.waitTicks(20);
		String lying = on(world, player -> {
			List<ItemEntity> blades = bladesOnGround(player);
			if (blades.size() != 1) {
				return "the blade should lie on the ground (" + blades.size() + ")";
			}
			ItemEntity e = blades.getFirst();
			if (!e.hasPickUpDelay() || !e.fireImmune() || e.getAge() != -32768) {
				return "kept for its swordsman: nobody else's to pick up, fireproof, never rotting (" + e.hasPickUpDelay() + ", " + e.fireImmune() + ", " + e.getAge() + ")";
			}
			// Someone else walks over it.
			Rival r = rival(player);
			r.snapTo(e.getX(), e.getY(), e.getZ(), 0, 0);
			e.playerTouch(r);
			if (!e.isAlive() || BondedBlades.bonded(r.getMainHandItem()) || r.getInventory().countItem(Items.DIAMOND_SWORD) > 0) {
				return "nobody else can pick it up";
			}
			return null;
		});
		check(lying == null, lying);
		dropRival(world);
		// Its glow lying there, by day.
		on(world, player -> {
			// Beside it and a little back, so it lies clear of the swordsman in the view.
			ItemEntity e = bladesOnGround(player).getFirst();
			player.teleportTo(player.level(), e.getX() + 1.2, player.getY(), e.getZ() - 1.6, Set.<Relative>of(), 0.0F, 38.0F, false);
			return null;
		});
		thirdPerson(context, world, 0, 38, 2.8, false);
		context.waitTicks(6);
		shot(context, "blade_dropped_tp");
		// Lava, a blast and a hopper leave it be (well away from its swordsman: a pit of lava walled in stone, a blast, a hopper under it).
		BlockPos hazard = STAGE.offset(9, 0, 9);
		on(world, player -> {
			ServerLevel level = player.level();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					level.setBlockAndUpdate(hazard.offset(dx, 0, dz), Blocks.STONE.defaultBlockState());
				}
			}
			level.setBlockAndUpdate(hazard, Blocks.LAVA.defaultBlockState());
			ItemEntity e = bladesOnGround(player).getFirst();
			e.teleportTo(hazard.getX() + 0.5, hazard.getY() + 0.3, hazard.getZ() + 0.5);
			e.setDeltaMovement(Vec3.ZERO);
			return null;
		});
		context.waitTicks(80);
		String inLava = on(world, player -> {
			List<ItemEntity> blades = bladesOnGround(player);
			return blades.size() == 1 ? null : "lava leaves it be (" + blades.size() + ")";
		});
		check(inLava == null, inLava);
		on(world, player -> {
			ServerLevel level = player.level();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					level.setBlockAndUpdate(hazard.offset(dx, 0, dz), Blocks.AIR.defaultBlockState());
				}
			}
			ItemEntity e = bladesOnGround(player).getFirst();
			e.teleportTo(hazard.getX() + 0.5, hazard.getY(), hazard.getZ() + 0.5);
			e.setDeltaMovement(Vec3.ZERO);
			return null;
		});
		context.waitTicks(10);
		on(world, player -> {
			ItemEntity e = bladesOnGround(player).getFirst();
			player.level().explode(null, e.getX() + 0.5, e.getY(), e.getZ(), 2.5F, Level.ExplosionInteraction.NONE);
			return null;
		});
		context.waitTicks(10);
		check(on(world, player -> bladesOnGround(player).size() == 1), "a blast leaves it be");
		on(world, player -> {
			ServerLevel level = player.level();
			BlockPos under = hazard.below();
			level.setBlockAndUpdate(under, Blocks.HOPPER.defaultBlockState());
			ItemEntity e = bladesOnGround(player).getFirst();
			e.teleportTo(under.getX() + 0.5, under.getY() + 1.0, under.getZ() + 0.5);
			e.setDeltaMovement(Vec3.ZERO);
			return null;
		});
		context.waitTicks(60);
		String hopper = on(world, player -> {
			ServerLevel level = player.level();
			List<ItemEntity> blades = bladesOnGround(player);
			if (blades.size() != 1) {
				return "a hopper leaves it lying (" + blades.size() + " on the ground)";
			}
			if (level.getBlockEntity(hazard.below()) instanceof HopperBlockEntity h && !h.isEmpty()) {
				return "the hopper took it";
			}
			level.setBlockAndUpdate(hazard.below(), Blocks.STONE.defaultBlockState());
			return null;
		});
		check(hopper == null, hopper);
		// Its swordsman takes it back.
		on(world, player -> {
			ItemEntity e = bladesOnGround(player).getFirst();
			e.teleportTo(e.getX(), STAGE.getY(), e.getZ());
			player.teleportTo(player.level(), e.getX(), STAGE.getY(), e.getZ(), Set.<Relative>of(), 0.0F, 30.0F, false);
			player.resetFallDistance();
			return null;
		});
		context.waitTicks(20);
		String back = on(world, player -> bladesOnGround(player).isEmpty() && BondedBlades.slotOf(player) >= 0 && copies(player) == 1 ? null
			: "its swordsman picks it up (" + bladesOnGround(player).size() + " on the ground, slot " + BondedBlades.slotOf(player) + ")");
		check(back == null, back);
		// Soulforged, by night, lying in the dark: its column and its pool.
		on(world, player -> {
			stand(player);
			setAura(player, "rime", AuraRules.SOVEREIGN, false);
			BondedBlades.addHistory(player, Map.of(BladeRules.BOSSES, 1), Map.of());
			BondedBlades.setResonance(player, BladeRules.threshold(BladeRules.SOULFORGED));
			return null;
		});
		context.waitTicks(80);
		String alive = on(world, player -> player.isAlive() ? null
			: "the swordsman should still be standing (" + player.getHealth() + " health at " + player.position() + ")");
		check(alive == null, alive);
		world.getServer().runCommand("time set 18000");
		on(world, player -> {
			int slot = BondedBlades.slotOf(player);
			ItemStack blade = player.getInventory().getItem(slot).copy();
			player.getInventory().setItem(slot, ItemStack.EMPTY);
			ItemEntity e = new ItemEntity(player.level(), STAGE.getX() + 0.5, STAGE.getY() + 0.2, STAGE.getZ() + 3.5, blade, 0, 0, 0);
			player.level().addFreshEntity(e);
			// The swordsman a little to one side, so the blade lies clear of them in the view.
			player.teleportTo(player.level(), STAGE.getX() + 2.2, STAGE.getY(), STAGE.getZ() - 1.0, Set.<Relative>of(), 0.0F, 24.0F, false);
			player.resetFallDistance();
			return null;
		});
		thirdPerson(context, world, 0, 24, 4.0, false);
		context.waitTicks(30);
		shot(context, "blade_dropped_soulforged_night_tp");
		world.getServer().runCommand("time set 3000");
		// Out of the world, it comes home.
		int returns = BondedBlades.returns();
		on(world, player -> {
			ItemEntity e = bladesOnGround(player).getFirst();
			e.teleportTo(e.getX(), player.level().getMinY() - 80, e.getZ());
			return null;
		});
		context.waitTicks(10);
		String home = on(world, player -> BondedBlades.returns() > returns && BondedBlades.slotOf(player) >= 0 && copies(player) == 1 ? null
			: "out of the world it comes home, the only one of it (slot " + BondedBlades.slotOf(player) + ", " + copies(player) + ")");
		check(home == null, home);
		firstPerson(context, world, 0.0F, 8.0F);
	}

	// ------------------------------------------------------------------ death

	private static void death(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		bondNew(world, "crimson", AuraRules.EDGE, new ItemStack(Items.DIAMOND_SWORD));
		context.waitTicks(10);
		// Without keepInventory (the blade cursed with vanishing too), with it, and with the blade on the cursor as death comes.
		for (int round = 0; round < 3; round++) {
			int r = round;
			world.getServer().runCommand("gamerule keep_inventory " + (r == 1));
			on(world, player -> {
				int slot = BondedBlades.slotOf(player);
				ItemStack blade = player.getInventory().getItem(slot).copy();
				player.getInventory().setItem(slot, ItemStack.EMPTY);
				if (r == 0) {
					blade.enchant(player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.VANISHING_CURSE), 1);
				}
				if (r == 2) {
					player.containerMenu.setCarried(blade);
				} else {
					player.getInventory().setItem(3, blade);
				}
				player.getInventory().setItem(7, new ItemStack(Items.STONE));
				player.kill(player.level());
				return null;
			});
			context.waitTicks(5);
			String died = on(world, player -> {
				long stones = player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(16), e -> e.getItem().is(Items.STONE)).size();
				if (!bladesOnGround(player).isEmpty()) {
					return "a death never drops the blade";
				}
				return r == 1 ? (stones == 0 ? null : "keepInventory keeps everything") : stones == 1 ? null : "the rest drops as ever (" + stones + " stones)";
			});
			check(died == null, died + " [round " + r + "]");
			context.runOnClient(mc -> {
				mc.player.respawn();
				mc.gui.setScreen(null);
			});
			context.waitTicks(15);
			String alive = on(world, player -> {
				if (BondedBlades.slotOf(player) < 0) {
					return "the new body has its blade";
				}
				if (r == 0 && BondedBlades.slotOf(player) != 3) {
					return "back in the slot it was in (" + BondedBlades.slotOf(player) + ")";
				}
				if (copies(player) != 1) {
					return "exactly one of it (" + copies(player) + ")";
				}
				if (player.hasAttached(BondedBlades.KEPT)) {
					return "nothing left kept once it's given back";
				}
				return BondedBlades.state(player).bonded() ? null : "the bond outlives the body";
			});
			check(alive == null, alive + " [round " + r + "]");
			check(context.computeOnClient(mc -> BondedBlades.carried(mc.player).isEmpty()) == false, "the client sees it back [round " + r + "]");
			world.getServer().runCommand("kill @e[type=item]");
			on(world, player -> {
				stand(player);
				return null;
			});
			context.waitTicks(5);
		}
		world.getServer().runCommand("gamerule keep_inventory false");
		check(BondedBlades.deathsKept() >= 2, "deaths kept the blade (" + BondedBlades.deathsKept() + ")");
	}

	// ------------------------------------------------------------------ theft and duplication

	private static void theft(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		bondNew(world, "hollow", AuraRules.EDGE, new ItemStack(Items.NETHERITE_SWORD));
		context.waitTicks(10);
		String chest = on(world, player -> {
			ItemStack blade = BondedBlades.carried(player);
			Rival r = rival(player);
			SimpleContainer box = new SimpleContainer(blade.copy());
			Slot slot = new Slot(box, 0, 0, 0);
			if (slot.mayPickup(r)) {
				return "nobody else can take it out of a chest";
			}
			return slot.mayPickup(player) ? null : "its swordsman can";
		});
		check(chest == null, chest);
		// A hopper under a chest leaves it in the chest.
		on(world, player -> {
			ServerLevel level = player.level();
			BlockPos at = STAGE.offset(4, 1, 4);
			level.setBlockAndUpdate(at.below(), Blocks.HOPPER.defaultBlockState());
			level.setBlockAndUpdate(at, Blocks.CHEST.defaultBlockState());
			int slot = BondedBlades.slotOf(player);
			((ChestBlockEntity) level.getBlockEntity(at)).setItem(0, player.getInventory().getItem(slot).copy());
			player.getInventory().setItem(slot, ItemStack.EMPTY);
			return null;
		});
		context.waitTicks(40);
		String pulled = on(world, player -> {
			ServerLevel level = player.level();
			BlockPos at = STAGE.offset(4, 1, 4);
			if (!BondedBlades.bonded(((ChestBlockEntity) level.getBlockEntity(at)).getItem(0))) {
				return "a hopper never pulls it out of a chest";
			}
			if (level.getBlockEntity(at.below()) instanceof HopperBlockEntity h && !h.isEmpty()) {
				return "the hopper has it";
			}
			// Back to its swordsman, the chest broken away.
			ItemStack blade = ((ChestBlockEntity) level.getBlockEntity(at)).getItem(0).copy();
			((ChestBlockEntity) level.getBlockEntity(at)).setItem(0, ItemStack.EMPTY);
			level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(at.below(), Blocks.AIR.defaultBlockState());
			player.getInventory().setItem(2, blade);
			return copies(player) == 1 ? null : "one of it (" + copies(player) + ")";
		});
		check(pulled == null, pulled);
		// Put into someone else's hands, and inside a box they carry: home.
		String stolen = on(world, player -> {
			MinecraftServer server = player.level().getServer();
			Rival r = rival(player);
			int slot = BondedBlades.slotOf(player);
			ItemStack blade = player.getInventory().getItem(slot).copy();
			player.getInventory().setItem(slot, ItemStack.EMPTY);
			r.getInventory().setItem(4, blade);
			BondedBlades.scan(r, BladeRegistry.of(server), true);
			if (BondedBlades.bonded(r.getInventory().getItem(4)) || BondedBlades.slotOf(player) < 0 || copies(player) != 1) {
				return "from someone else's inventory it goes home, the only one of it";
			}
			slot = BondedBlades.slotOf(player);
			blade = player.getInventory().getItem(slot).copy();
			player.getInventory().setItem(slot, ItemStack.EMPTY);
			ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
			shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.STONE), blade)));
			r.getInventory().setItem(5, shulker);
			BondedBlades.scan(r, BladeRegistry.of(server), true);
			ItemContainerContents left = r.getInventory().getItem(5).get(DataComponents.CONTAINER);
			boolean inside = left != null && left.nonEmptyItemCopyStream().anyMatch(BondedBlades::bonded);
			boolean stone = left != null && left.nonEmptyItemCopyStream().anyMatch(s -> s.is(Items.STONE));
			if (inside || !stone || BondedBlades.slotOf(player) < 0 || copies(player) != 1) {
				return "from inside a box they carry it goes home too, the box keeping the rest (" + inside + ", " + stone + ")";
			}
			return null;
		});
		check(stolen == null, stolen);
		dropRival(world);
		// A swordsman who isn't here: kept for them, given when they come.
		String ghost = on(world, player -> {
			MinecraftServer server = player.level().getServer();
			Ghost g = new Ghost(player.level());
			g.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("stone", AuraRules.EDGE, AuraRules.threshold(AuraRules.EDGE), 20, 0));
			g.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			if (!BondedBlades.bond(g, InteractionHand.MAIN_HAND, "test")) {
				return "a fake swordsman's blade should bond";
			}
			ItemStack theirs = g.getMainHandItem().copy();
			g.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			player.getInventory().setItem(8, theirs);
			BondedBlades.scan(player, BladeRegistry.of(server), true);
			if (BondedBlades.bonded(player.getInventory().getItem(8))) {
				return "someone else's blade never stays in my inventory";
			}
			return BladeRegistry.of(server).waitingFor(g.getUUID()) == 1 ? null : "it waits for its swordsman in the world's keeping";
		});
		check(ghost == null, ghost);
		// Never used up at an anvil, a grindstone or the repair recipe; never broken.
		String smith = on(world, player -> {
			ItemStack blade = BondedBlades.carried(player);
			AnvilMenu anvil = new AnvilMenu(91, player.getInventory(), ContainerLevelAccess.NULL);
			anvil.getSlot(0).set(new ItemStack(Items.NETHERITE_SWORD));
			anvil.getSlot(1).set(blade.copy());
			anvil.createResult();
			if (!anvil.getSlot(2).getItem().isEmpty()) {
				return "an anvil never uses a bonded blade up";
			}
			GrindstoneMenu grindstone = new GrindstoneMenu(92, player.getInventory(), ContainerLevelAccess.NULL);
			grindstone.getSlot(0).set(new ItemStack(Items.NETHERITE_SWORD));
			grindstone.getSlot(1).set(blade.copy());
			if (!grindstone.getSlot(2).getItem().isEmpty()) {
				return "a grindstone never melts it into another";
			}
			grindstone.getSlot(0).set(ItemStack.EMPTY);
			grindstone.getSlot(1).set(ItemStack.EMPTY);
			if (RepairItemRecipe.INSTANCE.matches(CraftingInput.of(2, 1, List.of(blade.copy(), new ItemStack(Items.NETHERITE_SWORD))), player.level())) {
				return "the repair recipe never takes it";
			}
			// Worn down: it stops at its last point.
			int slot = BondedBlades.slotOf(player);
			ItemStack real = player.getInventory().getItem(slot);
			real.setDamageValue(real.getMaxDamage() - 2);
			real.hurtAndBreak(50, player.level(), player, broken -> {});
			ItemStack after = player.getInventory().getItem(slot);
			if (after.isEmpty() || !BondedBlades.bonded(after) || after.getDamageValue() != after.getMaxDamage() - 1) {
				return "it never breaks, only notches (" + after + ", " + after.getDamageValue() + "/" + after.getMaxDamage() + ")";
			}
			after.setDamageValue(0);
			return copies(player) == 1 ? null : "and there's still one of it (" + copies(player) + ")";
		});
		check(smith == null, smith);
		// Upgraded at a smithing table (which carries every component of the old blade onto the new, as here), it's the same blade.
		String upgraded = on(world, player -> {
			int slot = BondedBlades.slotOf(player);
			ItemStack old = player.getInventory().getItem(slot);
			BladeBond before = BondedBlades.bond(old);
			ItemStack up = new ItemStack(Items.NETHERITE_SWORD.builtInRegistryHolder(), 1, old.getComponentsPatch());
			player.getInventory().setItem(slot, up);
			BondedBlades.scan(player, BladeRegistry.of(player.level().getServer()), true);
			BladeBond after = BondedBlades.bond(player.getInventory().getItem(slot));
			return after != null && before != null && after.id().equals(before.id()) && copies(player) == 1 ? null
				: "a blade upgraded at the smithing table keeps its bond (" + after + ")";
		});
		check(upgraded == null, upgraded);
		// Never an ingredient: a rune's recipe that asks for an axe passes a bonded one by, and a furnace won't melt one into nuggets.
		String eaten = on(world, player -> {
			ServerLevel level = player.level();
			ItemStack blade = BondedBlades.carried(player);
			ItemStack bondedAxe = new ItemStack(Items.DIAMOND_AXE.builtInRegistryHolder(), 1, blade.getComponentsPatch());
			ItemStack bondedIron = new ItemStack(Items.IRON_SWORD.builtInRegistryHolder(), 1, blade.getComponentsPatch());
			var rune = level.getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(Registries.RECIPE,
				dev.wildercord.Wildercord.id("rune_cleave")));
			if (rune.isEmpty() || !(rune.get().value() instanceof net.minecraft.world.item.crafting.CraftingRecipe recipe)) {
				return "the rune recipe the check uses is there";
			}
			List<ItemStack> grid = new ArrayList<>(List.of(new ItemStack(dev.wildercord.content.WildercordItems.BLANK_RUNE),
				new ItemStack(Items.DIAMOND_AXE), new ItemStack(dev.wildercord.content.WildercordItems.MANA_CRYSTAL), new ItemStack(Items.DIAMOND)));
			if (!recipe.matches(CraftingInput.of(2, 2, grid), level)) {
				return "an ordinary axe makes the rune (the check's own sense)";
			}
			grid.set(1, bondedAxe);
			if (recipe.matches(CraftingInput.of(2, 2, grid), level)) {
				return "a bonded axe is never eaten by a recipe";
			}
			var furnace = net.minecraft.world.item.crafting.RecipeType.SMELTING;
			if (level.getServer().getRecipeManager().getRecipeFor(furnace, new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(Items.IRON_SWORD)), level).isEmpty()) {
				return "an ordinary iron sword melts (the check's own sense)";
			}
			return level.getServer().getRecipeManager().getRecipeFor(furnace, new net.minecraft.world.item.crafting.SingleRecipeInput(bondedIron), level).isEmpty()
				? null : "a furnace never melts a bonded blade";
		});
		check(eaten == null, eaten);
	}

	// ------------------------------------------------------------------ the passing

	private static void pass(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		bondNew(world, "starlit", AuraRules.SOVEREIGN, new ItemStack(Items.DIAMOND_SWORD));
		on(world, player -> {
			BondedBlades.addHistory(player, Map.of(BladeRules.BOSSES, 1, BladeRules.KILLS, 400, BladeRules.FINISHERS, 70), Map.of());
			BondedBlades.setResonance(player, BladeRules.threshold(BladeRules.SOULFORGED));
			BondedBlades.forceTrait(player, BladeRules.CLOSING_STROKE);
			player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 8.0F, false);
			Disciple d = disciple(player);
			// The server sees it kneel (sneaking); a fake player is never ticked, so the client still draws it standing.
			d.setShiftKeyDown(true);
			return null;
		});
		context.waitTicks(80);
		int passes = BondedBlades.passes();
		HOOKED.clear();
		// From in front and to one side of the master (turned a little from the disciple, as the facing allows): the master's face
		// and blade, the disciple before them.
		frontView(context, world, 40, -20, 5.0);
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(45);
		check(on(world, BladeCeremony::underWay), "a master in the stance with a disciple kneeling before them begins the passing");
		context.waitTicks(100);
		shot(context, "blade_passing_tp");
		waitFor(context, () -> BondedBlades.passes() > passes, 120, "the passing should hand the blade over in eight seconds");
		on(world, player -> {
			// A fake player isn't ticked, so what it now holds is told to the client by hand.
			Disciple d = disciple(player);
			player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket(d.getId(),
				List.of(com.mojang.datafixers.util.Pair.of(net.minecraft.world.entity.EquipmentSlot.MAINHAND, d.getMainHandItem().copy()))));
			return null;
		});
		context.waitTicks(3);
		shot(context, "blade_passed_tp");
		context.getInput().releaseKey(o -> o.keyShift);
		String passed = on(world, player -> {
			Disciple d = disciple(player);
			ItemStack hand = d.getMainHandItem();
			BladeBond b = BondedBlades.bond(hand);
			if (b == null || !b.ownedBy(d.getUUID())) {
				return "the blade is in the disciple's hands, bonded to them";
			}
			if (BondedBlades.state(player).bonded() || !BondedBlades.state(d).bond().equals(b.id().toString())) {
				return "the bond moved from master to disciple";
			}
			if (!BladeRegistry.of(player.level().getServer()).activeFor(b.id(), d.getUUID())) {
				return "the world's registry follows";
			}
			if (!b.who().lineage().contains(player.getGameProfile().name()) || b.history().deeds().stream().noneMatch(x -> x.kind().equals(BladeRules.DEED_PASSED))) {
				return "its lineage and story keep the master (" + b.who() + ")";
			}
			if (b.tier() != BladeRules.SOULFORGED || !b.growth().trait().equals(BladeRules.CLOSING_STROKE)) {
				return "its tier and trait go with it";
			}
			if (BladeRules.effective(b.tier(), Aura.stage(d)) != 0) {
				return "a Flow disciple's blade sleeps until they grow into it";
			}
			if (copies(player) != 0 || BondedBlades.slotOf(player) >= 0) {
				return "the master has no copy";
			}
			return null;
		});
		check(passed == null, passed);
		check(HOOKED.contains("passed:Disciple"), "the blade hooks hear of it (" + HOOKED + ")");
		// A disciple with a blade of their own can't be given another; and back to the master (an operator's way).
		String back = on(world, player -> {
			Disciple d = disciple(player);
			if (AuraApi.bladePassingRefusal(player, d) == null) {
				return "nothing to pass without a blade";
			}
			if (!BondedBlades.pass(d, player, false)) {
				return "an operator can pass it back";
			}
			BladeBond b = BondedBlades.bond(BondedBlades.carried(player));
			return b != null && b.ownedBy(player.getUUID()) && b.who().lineage().contains("Disciple") ? null : "back with its master, its lineage longer";
		});
		check(back == null, back);
		dropDisciple(world);
		firstPerson(context, world, 0.0F, 8.0F);
	}

	// ------------------------------------------------------------------ the tooltip

	private static void tooltip(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		bondNew(world, "verdant", AuraRules.FORM, new ItemStack(Items.DIAMOND_SWORD));
		on(world, player -> {
			BondedBlades.addHistory(player, Map.of(BladeRules.KILLS, 312, BladeRules.ARTS, 140, BladeRules.FINISHERS, 41, BladeRules.GUARDS, 37,
				BladeRules.TECHNIQUES, 22, BladeRules.BOSSES, 1, BladeRules.DUELS, 2, BladeRules.LOW, 30), Map.of("thorn_lash", 70, "blossom_fall", 40));
			BondedBlades.setResonance(player, BladeRules.threshold(BladeRules.AWAKENED) + 260);
			BondedBlades.forceTrait(player, BladeRules.LAST_LIGHT);
			player.getInventory().setSelectedSlot(0);
			return null;
		});
		context.waitTicks(25);
		List<String> lines = context.computeOnClient(mc -> dev.wildercord.client.BladeTooltip.lines(mc.player.getMainHandItem(), mc.player, true).stream()
			.map(c -> c.getString()).toList());
		check(lines.stream().anyMatch(l -> l.contains("Last Light")), "the tooltip names its trait (" + lines + ")");
		check(lines.stream().anyMatch(l -> l.startsWith("Bonded on day")), "and tells where and when it was bonded");
		check(lines.stream().anyMatch(l -> l.contains("312")), "and its deeds");
		context.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
		context.waitTicks(6);
		double[] at = context.computeOnClient(mc -> {
			if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
				return null;
			}
			for (Slot slot : screen.getMenu().slots) {
				if (slot.container == mc.player.getInventory() && slot.getContainerSlot() == 0) {
					double scale = mc.getWindow().getGuiScale();
					return new double[] {(intField(screen, "leftPos") + slot.x + 8) * scale, (intField(screen, "topPos") + slot.y + 8) * scale};
				}
			}
			return null;
		});
		check(at != null, "the inventory shows the blade's slot");
		context.getInput().setCursorPos(at[0], at[1]);
		context.waitTicks(3);
		shot(context, "blade_tooltip");
		context.getInput().holdShift();
		context.waitTicks(3);
		shot(context, "blade_tooltip_story");
		context.getInput().releaseShift();
		context.setScreen(() -> null);
	}

	private static int intField(Object screen, String name) {
		try {
			java.lang.reflect.Field field = AbstractContainerScreen.class.getDeclaredField(name);
			field.setAccessible(true);
			return field.getInt(screen);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("couldn't read " + name + " off the screen: " + e);
		}
	}

	// ------------------------------------------------------------------ counting it

	/** Every bonded blade of {@code player}'s anywhere near: in their inventory, on the cursor, on the ground, in a fake player's hands. */
	private static int copies(ServerPlayer player) {
		UUID me = player.getUUID();
		int n = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			BladeBond b = BondedBlades.bond(player.getInventory().getItem(i));
			if (b != null && b.ownedBy(me)) {
				n++;
			}
		}
		BladeBond carried = BondedBlades.bond(player.containerMenu.getCarried());
		if (carried != null && carried.ownedBy(me)) {
			n++;
		}
		for (ItemEntity e : player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(64))) {
			BladeBond b = BondedBlades.bond(e.getItem());
			if (b != null && b.ownedBy(me)) {
				n++;
			}
		}
		for (ServerPlayer other : player.level().getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(64), p -> p != player)) {
			for (int i = 0; i < other.getInventory().getContainerSize(); i++) {
				BladeBond b = BondedBlades.bond(other.getInventory().getItem(i));
				if (b != null && b.ownedBy(me)) {
					n++;
				}
			}
		}
		return n;
	}

	private static List<ItemEntity> bladesOnGround(ServerPlayer player) {
		return player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(64), e -> BondedBlades.bonded(e.getItem()));
	}

	// ------------------------------------------------------------------ the crossing

	private static double[] crossingHeart(ServerPlayer player) {
		long seed = LeyWalker.seed(player.level());
		for (int ring = 0; ring <= 200; ring++) {
			for (int ix = -ring; ix <= ring; ix++) {
				for (int iz = -ring; iz <= ring; iz++) {
					if (Math.max(Math.abs(ix), Math.abs(iz)) != ring) {
						continue;
					}
					double[] heart = LeyLines.crossingIn(seed, ix, iz);
					if (heart != null && heart[2] >= 0.6) {
						return heart;
					}
				}
			}
		}
		throw new AssertionError("there should be a ley crossing near spawn");
	}

	private static void goToCrossing(ClientGameTestContext context, TestSingleplayerContext world) {
		double[] heart = on(world, WildercordBondedBladeTest::crossingHeart);
		int hx = (int) Math.floor(heart[0]);
		int hz = (int) Math.floor(heart[1]);
		int y = 200;
		world.getServer().runCommand("forceload add " + (hx - 10) + " " + (hz - 10) + " " + (hx + 10) + " " + (hz + 10));
		world.getServer().runCommand("fill " + (hx - 9) + " " + (y - 1) + " " + (hz - 9) + " " + (hx + 9) + " " + (y - 1) + " " + (hz + 9) + " minecraft:grass_block");
		world.getServer().runCommand("fill " + (hx - 9) + " " + y + " " + (hz - 9) + " " + (hx + 9) + " " + (y + 8) + " " + (hz + 9) + " minecraft:air");
		on(world, player -> {
			player.teleportTo(player.level(), heart[0], y, heart[1], Set.<Relative>of(), 20.0F, 10.0F, false);
			player.setDeltaMovement(Vec3.ZERO);
			return null;
		});
		context.waitTicks(30);
		check(on(world, player -> PowerPlaces.isPlaceOfPower(player.level(), player.blockPosition())), "the crossing's heart is a place of power");
	}

	private static void leaveCrossing(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("forceload remove all");
		on(world, player -> {
			stand(player);
			return null;
		});
		context.waitTicks(10);
		firstPerson(context, world, 0.0F, 8.0F);
	}

	// ------------------------------------------------------------------ playing

	private static void swing(ClientGameTestContext context) {
		context.getInput().pressKey(o -> o.keyAttack);
	}

	private static void waitFor(ClientGameTestContext context, java.util.function.BooleanSupplier done, int most, String what) {
		for (int t = 0; t < most && !done.getAsBoolean(); t++) {
			context.waitTicks(1);
		}
		check(done.getAsBoolean(), what);
	}

	private static void click(ClientGameTestContext context, Function<AuraScreen, double[]> where) {
		double[] p = context.computeOnClient(mc -> mc.gui.screen() instanceof AuraScreen s ? where.apply(s) : null);
		check(p != null, "nothing to click there");
		double guiScale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(p[0] * guiScale, p[1] * guiScale);
		context.waitTicks(1);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(2);
	}

	private static void clickBlade(ClientGameTestContext context, String key) {
		context.waitTicks(1);
		double[] p = context.computeOnClient(mc -> mc.gui.screen() instanceof AuraScreen s ? s.bladePoint(key) : null);
		check(p != null, "the Blade page should draw " + key);
		double guiScale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(p[0] * guiScale, p[1] * guiScale);
		context.waitTicks(1);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(2);
	}

	private static void hoverBlade(ClientGameTestContext context, String key) {
		double[] p = context.computeOnClient(mc -> mc.gui.screen() instanceof AuraScreen s ? s.bladePoint(key) : null);
		check(p != null, "the Blade page should draw " + key);
		double guiScale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(p[0] * guiScale, p[1] * guiScale);
		context.waitTicks(2);
	}

	// ------------------------------------------------------------------ views

	private static void thirdPerson(ClientGameTestContext context, TestSingleplayerContext world, float yaw, float pitch, double distance, boolean hud) {
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			if (mc.gui.hud.isHidden() == hud) {
				mc.gui.hud.toggle();
			}
		});
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(distance);
			player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), yaw, pitch, false);
			return null;
		});
		context.waitTicks(2);
	}

	/** From in front: the camera faces the swordsman (and the blade in their hand) as they look toward {@code yaw}. */
	private static void frontView(ClientGameTestContext context, TestSingleplayerContext world, float yaw, float pitch, double distance) {
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(distance);
			player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), yaw, pitch, false);
			return null;
		});
		context.waitTicks(2);
	}

	private static void firstPerson(ClientGameTestContext context, TestSingleplayerContext world, float yaw, float pitch) {
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(4.0);
			player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), yaw, pitch, false);
			return null;
		});
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
	}

	// ------------------------------------------------------------------ the stage

	private static void stage(TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 14) + " " + (y - 1) + " " + (z - 14) + " " + (x + 14) + " " + (y - 1) + " " + (z + 24) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 14) + " " + y + " " + (z - 14) + " " + (x + 14) + " " + (y + 10) + " " + (z + 24) + " minecraft:air");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
			stand(player);
		});
	}

	private static void stand(ServerPlayer player) {
		player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 8.0F, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.removeAllEffects();
		player.setHealth(player.getMaxHealth());
		player.getFoodData().setFoodLevel(20);
		player.resetFallDistance();
		player.clearFire();
	}

	private static void reset(ClientGameTestContext context, TestSingleplayerContext world) {
		context.getInput().releaseKey(o -> o.keyShift);
		kill(world);
		dropRival(world);
		dropDisciple(world);
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		world.getServer().runCommand("time set 3000");
		world.getServer().runCommand("fill " + (STAGE.getX() - 14) + " " + STAGE.getY() + " " + (STAGE.getZ() - 14) + " " + (STAGE.getX() + 14) + " "
			+ (STAGE.getY() + 10) + " " + (STAGE.getZ() + 24) + " minecraft:air");
		world.getServer().runCommand("fill " + (STAGE.getX() - 14) + " " + (STAGE.getY() - 1) + " " + (STAGE.getZ() - 14) + " " + (STAGE.getX() + 14) + " "
			+ (STAGE.getY() - 1) + " " + (STAGE.getZ() + 24) + " minecraft:stone");
		context.waitTicks(6);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			BondedBlades.release(player);
			player.getInventory().clearContent();
			player.containerMenu.setCarried(ItemStack.EMPTY);
			stand(player);
			if (player.level().getScoreboard().getPlayersTeam(player.getScoreboardName()) != null) {
				player.level().getScoreboard().removePlayerFromTeam(player.getScoreboardName());
			}
			player.removeAttached(AuraAttachments.STATE);
			player.removeAttached(SwordStrings.COOLDOWNS);
			player.removeAttached(Momentum.MOMENTUM);
			player.removeAttached(Stance.STANCE);
			player.removeAttached(Awakening.AWAKENING);
			player.removeAttached(BondedBlades.KEPT);
			player.setAttached(AuraPresence.TIMERS, AuraPresence.Timers.NONE);
			Ways.set(player, "");
		});
		firstPerson(context, world, 0.0F, 8.0F);
		context.runOnClient(mc -> {
			mc.player.setXRot(8);
			mc.player.xRotO = 8;
		});
		context.waitTicks(StringRules.ENGAGED_TICKS + 10);
	}

	private static void kill(TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			for (Entity e : player.level().getEntitiesOfClass(Entity.class, player.getBoundingBox().inflate(64), e -> e.entityTags().contains(TAG))) {
				e.discard();
			}
		});
	}

	private static void setAura(ServerPlayer player, String method, int stage) {
		setAura(player, method, stage, true);
	}

	private static void setAura(ServerPlayer player, String method, int stage, boolean fresh) {
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data(method, stage, AuraRules.threshold(stage), AuraRules.capacity(stage), 0));
		if (fresh) {
			player.removeAttached(AuraAttachments.STATE);
			player.removeAttached(Awakening.AWAKENING);
		}
	}

	/** {@code player} of {@code method} at {@code stage}, with {@code blade} in hand bonded at once (no ceremony). */
	private static void bondNew(TestSingleplayerContext world, String method, int stage, ItemStack blade) {
		String why = on(world, player -> {
			setAura(player, method, stage);
			player.setItemInHand(InteractionHand.MAIN_HAND, blade);
			return BondedBlades.bond(player, InteractionHand.MAIN_HAND, "test") ? null : "the blade should bond: " + BondedBlades.bondRefusal(player);
		});
		check(why == null, why);
	}

	private static Vec3 at(double side, double ahead) {
		return new Vec3(STAGE.getX() + 0.5 + side, STAGE.getY(), STAGE.getZ() + 0.5 + ahead);
	}

	/** A foe that stands its ground (no speed, no sight, its AI left on), facing {@code yaw}. */
	private static <T extends Mob> T spawn(ServerLevel level, EntityType<T> type, Vec3 at, double health, float yaw) {
		T mob = type.create(level, EntitySpawnReason.COMMAND);
		if (mob == null) {
			throw new AssertionError("couldn't make a " + type);
		}
		mob.snapTo(at.x, at.y, at.z, yaw, 0);
		mob.setYHeadRot(yaw);
		mob.setYBodyRot(yaw);
		mob.addTag(TAG);
		mob.addTag("wildercord.rolled");
		mob.setPersistenceRequired();
		AttributeInstance max = mob.getAttribute(Attributes.MAX_HEALTH);
		if (max != null) {
			max.setBaseValue(Math.max(health, 1));
		}
		AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.setBaseValue(0.0);
		}
		AttributeInstance sight = mob.getAttribute(Attributes.FOLLOW_RANGE);
		if (sight != null) {
			sight.setBaseValue(0.0);
		}
		AttributeInstance steady = mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (steady != null) {
			steady.setBaseValue(1.0);
		}
		mob.setHealth((float) health);
		// A test about the blade, not about openings: their stance stands steady.
		mob.setAttached(Stance.STANCE, new Stance.State(0, 0, 40, 0, -1, Long.MAX_VALUE / 4, 0));
		level.addFreshEntity(mob);
		return mob;
	}

	private static List<Mob> foes(ServerPlayer player) {
		List<Mob> found = new ArrayList<>(player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48),
			m -> m.entityTags().contains(TAG) && m.getType() == EntityTypes.HUSK && m.isAlive()));
		found.sort(java.util.Comparator.comparingInt(Mob::getId));
		if (found.isEmpty()) {
			throw new AssertionError("the foes are gone");
		}
		return found;
	}

	// ------------------------------------------------------------------ other players

	/** Someone else (a fake player that can be hurt). */
	private static final class Rival extends FakePlayer {
		Rival(ServerLevel level) {
			super(level, new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("wildercord-blade-rival".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
				"Rival"));
		}

		@Override
		public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
			return false;
		}

		@Override
		public PlayerTeam getTeam() {
			return level().getScoreboard().getPlayersTeam(getScoreboardName());
		}
	}

	/** A disciple, kneeling before their master. */
	private static final class Disciple extends FakePlayer {
		Disciple(ServerLevel level) {
			super(level, new com.mojang.authlib.GameProfile(DISCIPLE_ID, "Disciple"));
		}
	}

	/** A swordsman who isn't here (never in the world). */
	private static final class Ghost extends FakePlayer {
		Ghost(ServerLevel level) {
			super(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "Ghost"));
		}
	}

	private static Rival rival;
	private static Disciple disciple;

	private static Rival rival(ServerPlayer player) {
		if (rival == null || rival.isRemoved() || rival.level() != player.level()) {
			rival = new Rival(player.level());
			Vec3 p = at(1.6, 1.6);
			rival.snapTo(p.x, p.y, p.z, 200, 0);
			rival.setYHeadRot(200);
			player.connection.send(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(rival)));
			player.level().addNewPlayer(rival);
		}
		return rival;
	}

	private static Disciple disciple(ServerPlayer player) {
		if (disciple == null || disciple.isRemoved() || disciple.level() != player.level()) {
			disciple = new Disciple(player.level());
			Vec3 p = at(0, 2.0);
			disciple.snapTo(p.x, p.y, p.z, 180, 0);
			disciple.setYHeadRot(180);
			disciple.setYBodyRot(180);
			disciple.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("rime", AuraRules.FLOW, AuraRules.threshold(AuraRules.FLOW), 10, 0));
			disciple.setAttached(AuraAttachments.LOOK, new AuraAttachments.Look(0x8CDCFF, AuraRules.FLOW, true, false, false));
			player.connection.send(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(disciple)));
			player.level().addNewPlayer(disciple);
		}
		return disciple;
	}

	private static void dropRival(TestSingleplayerContext world) {
		on(world, player -> {
			if (rival != null) {
				rival.discard();
				player.connection.send(new net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket(List.of(rival.getUUID())));
				rival = null;
			}
			return null;
		});
	}

	private static void dropDisciple(TestSingleplayerContext world) {
		on(world, player -> {
			if (disciple != null) {
				BondedBlades.release(disciple);
				disciple.discard();
				player.connection.send(new net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket(List.of(disciple.getUUID())));
				disciple = null;
			}
			return null;
		});
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> mc.gui.toastManager().clear());
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static <T> T on(TestSingleplayerContext world, Function<ServerPlayer, T> step) {
		return world.getServer().computeOnServer(server -> step.apply(player(server)));
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}
}
