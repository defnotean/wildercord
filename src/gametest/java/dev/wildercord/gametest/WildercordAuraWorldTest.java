package dev.wildercord.gametest;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraGuard;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.AuraSlash;
import dev.wildercord.aura.BreathingManualItem;
import dev.wildercord.aura.BreathingMethods;
import dev.wildercord.aura.Crescents;
import dev.wildercord.aura.world.AuraFighter;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.aura.world.AuraWorldRules;
import dev.wildercord.aura.world.Duelist;
import dev.wildercord.aura.world.DuelistDuels;
import dev.wildercord.aura.world.DuelistSpawner;
import dev.wildercord.aura.world.FallenKnight;
import dev.wildercord.aura.world.ForgedGear;
import dev.wildercord.aura.world.KnightSpawner;
import dev.wildercord.aura.world.ManualPageItem;
import dev.wildercord.cast.TemporaryBlocks;
import dev.wildercord.client.WildercordKeys;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.duel.DuelRules;
import dev.wildercord.gear.GearDef;
import dev.wildercord.gear.GearItems;
import dev.wildercord.gear.GearSlot;
import dev.wildercord.gear.GearSlots;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * The world of aura in a real world, on a stone platform in the sky:
 * <ul>
 *   <li>duelists come to a camp (lighting a borrowed fire that goes with them), to a road and to a village's bell, never two
 *       near one another; seen up close, one goes into the field guide;</li>
 *   <li>a duel: the real use key offers it, a second use accepts, the countdown, the fight at the challenger's own stage; the
 *       duelist's guard halves a blow and its perfect moment staggers a rushed one; at Edge it slashes, cutting only its
 *       challenger; brought low it yields, and a challenger without a method learns its own, with a little experience and the
 *       Grimoire; one already breathing another way is handed the manual instead; knocked out, a challenger is put back as they
 *       began; the duel trial is registered for Form and Sovereign;</li>
 *   <li>the forged gear: each smithing recipe makes its forging (keeping the weapon's enchantments), pages of one method bind into
 *       its manual (mixed pages don't), and each piece does its one thing: Lumenedge's aura, Skyrend Glaive's slash (harder and
 *       further), Bulwark Maul's cheaper guard, the Breath Sash's capacity and quicker breath;</li>
 *   <li>a fallen knight: rising in a dark spawner room at rank 1; its slash, telegraphed by a raised blade, lands through the
 *       spell defences, and stepping aside dodges it; a perfect guard sends its crescent back; its own guard staggers a rushed blow,
 *       halves a held one, and breaks to an axe; its loot always has a page of its method and sometimes an Aura Shard;</li>
 *   <li>two crescents meeting head on clash, harming nothing behind either;</li>
 *   <li>the slash by day, against the sky.</li>
 * </ul>
 * Screenshots: {@code aura_world_*}: the duelists by method, each pose, the camp, the knight and its poses, the items, the forged
 * weapons in hand, a clash and the slash by day.
 *
 * <p>Runs in the full suite; {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and {@code WILDERCORD_SHOWCASE} skip it.</p>
 */
public class WildercordAuraWorldTest implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 180, 0);
	private static final String TAG = "wildercord.aura_world_test";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		context.runOnClient(mc -> {
			mc.getWindow().setWindowed(1920, 1080);
			mc.options.guiScale().set(2);
			mc.resizeGui();
		});
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule natural_regeneration false");
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			stage(world);
			context.waitTicks(10);
			List<String> failures = new ArrayList<>();
			run(failures, "duelists come", () -> spawning(context, world));
			reset(context, world);
			run(failures, "a duel", () -> duel(context, world));
			reset(context, world);
			run(failures, "a duel at Edge", () -> edgeDuel(context, world));
			reset(context, world);
			run(failures, "losing a duel", () -> losing(context, world));
			reset(context, world);
			run(failures, "the forged gear", () -> forged(context, world));
			reset(context, world);
			run(failures, "a fallen knight", () -> knight(context, world));
			reset(context, world);
			run(failures, "a clash", () -> clash(context, world));
			reset(context, world);
			run(failures, "the slash by day", () -> daylight(context, world));
			reset(context, world);
			run(failures, "pictures", () -> pictures(context, world));
			if (!failures.isEmpty()) {
				throw new AssertionError("The world of aura went wrong:\n  " + String.join("\n  ", failures));
			}
		} finally {
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				if (mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
			context.getInput().releaseKey(o -> o.keyShift);
		}
	}

	private static void run(List<String> failures, String what, Runnable test) {
		try {
			test.run();
		} catch (AssertionError e) {
			failures.add(what + ": " + e.getMessage());
		}
	}

	private static void check(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError(what);
		}
	}

	// ------------------------------------------------------------------ duelists come

	private static void spawning(ClientGameTestContext context, TestSingleplayerContext world) {
		int lawn = STAGE.getY() - 20;
		world.getServer().runCommand("fill -48 " + lawn + " -48 48 " + lawn + " 48 minecraft:grass_block");
		context.waitTicks(5);
		String camp = on(world, player -> {
			ServerLevel level = player.level();
			Duelist duelist = DuelistSpawner.spawnNear(level, player, true, level.getRandom());
			if (duelist == null) {
				return "a duelist should find a camp on the open grass below";
			}
			duelist.addTag(TAG);
			BlockPos fire = duelist.camp();
			if (fire == null || !level.getBlockState(fire).is(Blocks.CAMPFIRE) || !TemporaryBlocks.recorded(level, fire)) {
				return "a duelist at a camp should light a campfire, written down to go (" + fire + ")";
			}
			if (DuelistSpawner.loaded(level.getServer()) != 1) {
				return "one duelist should be about (" + DuelistSpawner.loaded(level.getServer()) + ")";
			}
			if (DuelistSpawner.spawnNear(level, player, true, level.getRandom()) != null) {
				return "no second duelist should come near the first";
			}
			duelist.vanish(level);
			return level.getBlockState(fire).is(Blocks.CAMPFIRE) ? "its fire should go with it" : null;
		});
		check(camp == null, camp);
		String road = on(world, player -> {
			ServerLevel level = player.level();
			int ground = STAGE.getY() - 20;
			player.level().getServer().getCommands().performPrefixedCommand(player.level().getServer().createCommandSourceStack(),
				"fill -48 " + ground + " -48 48 " + ground + " 48 minecraft:dirt_path");
			DuelistSpawner.Spot spot = DuelistSpawner.road(level, player.blockPosition(), level.getRandom());
			player.level().getServer().getCommands().performPrefixedCommand(player.level().getServer().createCommandSourceStack(),
				"fill -48 " + ground + " -48 48 " + ground + " 48 minecraft:grass_block");
			return spot != null && spot.place() == DuelistSpawner.Place.ROAD ? null : "a dirt path should be a road for a duelist (" + spot + ")";
		});
		check(road == null, road);
		String village = on(world, player -> {
			ServerLevel level = player.level();
			BlockPos column = player.blockPosition().offset(30, 0, 10);
			BlockPos top = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
			level.setBlockAndUpdate(top, Blocks.BELL.defaultBlockState());
			DuelistSpawner.Spot spot = DuelistSpawner.village(level, player.blockPosition(), level.getRandom());
			level.setBlockAndUpdate(top, Blocks.AIR.defaultBlockState());
			if (spot == null || spot.place() != DuelistSpawner.Place.VILLAGE) {
				return "a village's bell should draw a duelist near it (" + spot + ")";
			}
			return spot.pos().distSqr(top) <= 12 * 12 ? null : "it should come within a few blocks of the bell (" + spot.pos() + " from " + top + ")";
		});
		check(village == null, village);
		// Seen up close, a duelist goes into the field guide.
		on(world, player -> {
			duelist(player, "verdant", 0, 6);
			return null;
		});
		context.waitTicks(45);
		boolean met = on(world, player -> Heart.grimoire(player).contains(FieldGuide.key("wildercord:duelist")));
		world.getServer().runCommand("fill -48 " + lawn + " -48 48 " + lawn + " 48 minecraft:air");
		check(met, "a duelist seen up close should go into the field guide");
	}

	// ------------------------------------------------------------------ a duel

	private static void duel(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			player.setAttached(AuraAttachments.AURA, AuraAttachments.Data.NONE);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			duelist(player, "hollow", 0, 2.6).addTag("wildercord.duelist_a");
			return null;
		});
		context.waitTicks(10);
		check(on(world, player -> AuraApi.trials(AuraRules.FORM).contains(DuelistDuels.TRIAL) && AuraApi.trials(AuraRules.SOVEREIGN).contains(DuelistDuels.TRIAL)),
			"the duel trial should be registered for Form and Sovereign");
		// The real use key, as a player would: the first offers, the second accepts.
		context.getInput().pressKey(o -> o.keyUse);
		context.waitTicks(3);
		check(on(world, player -> DuelistDuels.phase(player) == null), "the first use should only offer the duel");
		context.getInput().pressKey(o -> o.keyUse);
		context.waitTicks(3);
		String begun = on(world, player -> {
			Duelist duelist = (Duelist) tagged(player, "wildercord.duelist_a");
			if (DuelistDuels.phase(player) != DuelRules.Phase.COUNTDOWN) {
				return "the second use should start the countdown (" + DuelistDuels.phase(player) + ")";
			}
			if (duelist.stage() != AuraRules.GLOW || !duelist.getMainHandItem().is(Items.IRON_SWORD) || !duelist.state(AuraFighter.DRAWN)) {
				return "a challenger without a method meets a Glow duelist with its blade drawn (stage " + duelist.stage() + ", " + duelist.getMainHandItem() + ")";
			}
			return Math.abs(duelist.getMaxHealth() - AuraWorldRules.duelistHealth(AuraRules.GLOW)) < 1e-3 ? null : "its health should be its stage's";
		});
		check(begun == null, begun);
		context.waitTicks(DuelRules.COUNTDOWN_TICKS + 2);
		check(on(world, player -> DuelistDuels.phase(player) == DuelRules.Phase.FIGHTING), "the countdown should give way to the fight");
		// A Glow duelist has no guard; a Flow one does. Its guard is checked with its own will set aside, so nothing it decides
		// moves the numbers.
		float[] open = on(world, player -> {
			Duelist duelist = (Duelist) tagged(player, "wildercord.duelist_a");
			duelist.setNoAi(true);
			duelist.setStage(AuraRules.FLOW);
			float before = duelist.getHealth();
			player.attack(duelist);
			return new float[] {before - duelist.getHealth()};
		});
		check(open[0] > 0, "a blow should land on the duelist once the fight is on (" + open[0] + ")");
		context.waitTicks(25);
		String guards = on(world, player -> {
			Duelist duelist = (Duelist) tagged(player, "wildercord.duelist_a");
			duelist.setHealth(duelist.getMaxHealth());
			if (!duelist.raiseGuard()) {
				return "the duelist should raise its guard";
			}
			float before = duelist.getHealth();
			player.attack(duelist);
			if (duelist.getHealth() < before) {
				return "a blow in its perfect moment should be turned whole (took " + (before - duelist.getHealth()) + ")";
			}
			MobEffectInstance slow = player.getEffect(MobEffects.SLOWNESS);
			return slow != null && player.hasEffect(MobEffects.WEAKNESS) ? null : "a rushed blow into its perfect guard should stagger the player";
		});
		check(guards == null, guards);
		context.waitTicks(16);
		float held = on(world, player -> {
			player.removeAllEffects();
			Duelist duelist = (Duelist) tagged(player, "wildercord.duelist_a");
			float before = duelist.getHealth();
			player.attack(duelist);
			return before - duelist.getHealth();
		});
		check(held > 0 && Math.abs(held - open[0] * (1 - AuraWorldRules.MOB_GUARD_SHARE)) < 0.6,
			"its held guard should halve a blow (" + held + " against " + open[0] + ")");
		context.waitTicks(25);
		// Brought low, it yields: the challenger learns its method, a little experience, and the Grimoire's entry.
		String won = on(world, player -> {
			Duelist duelist = (Duelist) tagged(player, "wildercord.duelist_a");
			duelist.dropGuard();
			duelist.setHealth(9);
			player.attack(duelist);
			if (DuelistDuels.phase(player) != null) {
				return "a blow leaving it on its last health should make it yield and end the duel (" + DuelistDuels.phase(player) + ")";
			}
			if (!duelist.isAlive() || !duelist.state(AuraFighter.BOW) || !duelist.leaving()) {
				return "it should yield, alive, and bow to go";
			}
			if (Math.abs(duelist.getHealth() - duelist.getMaxHealth()) > 1e-3) {
				return "it should mend once the duel is over";
			}
			AuraAttachments.Data data = Aura.data(player);
			if (!data.method().equals("hollow") || data.stage() != AuraRules.GLOW) {
				return "a challenger without a method should learn the duelist's (" + data + ")";
			}
			if (Math.abs(data.xp() - AuraWorldRules.duelXp(AuraRules.GLOW)) > 1e-3) {
				return "the lesson should give a little experience (" + data.xp() + ")";
			}
			return Heart.grimoire(player).contains("aura:duelist") ? null : "the lesson should go into the Grimoire";
		});
		check(won == null, won);
		on(world, player -> {
			tagged(player, "wildercord.duelist_a").setNoAi(false);
			return null;
		});
		context.waitTicks(AuraWorldRules.BOW_TICKS + 75);
		boolean gone = on(world, player -> player.level().getEntitiesOfClass(Duelist.class, player.getBoundingBox().inflate(48),
			d -> d.entityTags().contains("wildercord.duelist_a")).isEmpty());
		check(gone, "a beaten duelist should bow and go");

		// One already breathing another way is handed the manual, to choose.
		on(world, player -> {
			setAura(player, "ember", AuraRules.GLOW, 10, 0);
			player.getInventory().clearContent();
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			Duelist duelist = duelist(player, "rime", 0, 2.6);
			duelist.addTag("wildercord.duelist_b");
			DuelistDuels.start(player, duelist);
			return null;
		});
		context.waitTicks(DuelRules.COUNTDOWN_TICKS + 3);
		String manual = on(world, player -> {
			Duelist duelist = (Duelist) tagged(player, "wildercord.duelist_b");
			duelist.setNoAi(true);
			duelist.setHealth(3);
			player.attack(duelist);
			if (!Aura.data(player).method().equals("ember")) {
				return "a challenger who breathes another way should keep their method (" + Aura.data(player) + ")";
			}
			for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
				ItemStack stack = player.getInventory().getItem(i);
				if (stack.is(BreathingManualItem.MANUAL) && "rime".equals(stack.get(BreathingManualItem.METHOD))) {
					return null;
				}
			}
			return "they should be handed the duelist's manual instead";
		});
		check(manual == null, manual);
	}

	// ------------------------------------------------------------------ at Edge: the slash

	private static void edgeDuel(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "ember", AuraRules.EDGE, AuraRules.capacity(AuraRules.EDGE), AuraRules.threshold(AuraRules.EDGE));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			Duelist duelist = duelist(player, "thunder", 0, 2.6);
			duelist.addTag("wildercord.duelist_edge");
			DuelistDuels.start(player, duelist);
			return null;
		});
		check(on(world, player -> DuelistDuels.stage(player) == AuraRules.EDGE), "an Edge challenger should meet an Edge duelist");
		context.waitTicks(DuelRules.COUNTDOWN_TICKS + 2);
		// Out of reach of its blade: it slashes. A husk stands beside the challenger, in the crescent's path.
		on(world, player -> {
			player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 10.5, Set.<Relative>of(), 180.0F, 0.0F, false);
			player.setHealth(player.getMaxHealth());
			player.removeAttached(WildercordAttachments.SPELLGUARD);
			spawn(player.level(), EntityTypes.HUSK, new Vec3(STAGE.getX() + 1.2, STAGE.getY(), STAGE.getZ() + 10.5), 200).addTag("wildercord.bystander");
			return null;
		});
		boolean wound = false;
		boolean flew = false;
		for (int t = 0; t < 140 && !flew; t++) {
			context.waitTicks(1);
			boolean[] seen = on(world, player -> {
				Duelist duelist = (Duelist) tagged(player, "wildercord.duelist_edge");
				return new boolean[] {duelist.state(AuraFighter.WINDUP), Crescents.inFlight().stream().anyMatch(f -> f.caster() == duelist)};
			});
			wound |= seen[0];
			flew = seen[1];
		}
		check(wound, "the duelist should raise its blade before it slashes (the tell)");
		check(flew, "out of its blade's reach, an Edge duelist should slash");
		context.waitTicks(12);
		String cut = on(world, player -> {
			Mob husk = tagged(player, "wildercord.bystander");
			if (husk.getHealth() < husk.getMaxHealth()) {
				return "a duelist's crescent should cut nobody but its challenger (the husk took " + (husk.getMaxHealth() - husk.getHealth()) + ")";
			}
			return player.getHealth() < player.getMaxHealth() ? null : "the crescent should reach its challenger (health " + player.getHealth() + ")";
		});
		check(cut == null, cut);
		on(world, player -> {
			((Duelist) tagged(player, "wildercord.duelist_edge")).vanish(player.level());
			return null;
		});
		context.waitTicks(2);
		check(on(world, player -> DuelistDuels.phase(player) == null), "a duelist gone ends its duel");
	}

	// ------------------------------------------------------------------ losing

	private static void losing(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "ember", AuraRules.GLOW, 10, 0);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			player.setHealth(player.getMaxHealth());
			Duelist duelist = duelist(player, "stone", 0, 2.6);
			duelist.addTag("wildercord.duelist_c");
			DuelistDuels.start(player, duelist);
			return null;
		});
		context.waitTicks(DuelRules.COUNTDOWN_TICKS + 3);
		String lost = on(world, player -> {
			Duelist duelist = (Duelist) tagged(player, "wildercord.duelist_c");
			ServerLevel level = player.level();
			dev.wildercord.cast.Effects.readyToHurt(player);
			player.hurtServer(level, level.damageSources().mobAttack(duelist), 5);
			dev.wildercord.cast.Effects.readyToHurt(player);
			player.hurtServer(level, level.damageSources().mobAttack(duelist), 100);
			if (!player.isAlive()) {
				return "a challenger brought down by the duelist should be knocked out, not killed";
			}
			if (DuelistDuels.phase(player) != null) {
				return "the knockout should end the duel";
			}
			if (Math.abs(player.getHealth() - player.getMaxHealth()) > 0.01) {
				return "losing should cost nothing but pride: the health the duelist took given back (" + player.getHealth() + ")";
			}
			if (duelist.leaving() || duelist.restingUntil() <= level.getGameTime()) {
				return "a duelist that won should stay, and rest a while before the next challenge";
			}
			return null;
		});
		check(lost == null, lost);
	}

	// ------------------------------------------------------------------ the forged gear

	@SuppressWarnings("unchecked")
	private static void forged(ClientGameTestContext context, TestSingleplayerContext world) {
		String recipes = on(world, player -> {
			ServerLevel level = player.level();
			for (AuraWorldRules.Forged forged : AuraWorldRules.Forged.values()) {
				for (boolean netherite : new boolean[] {false, true}) {
					ItemStack base = AuraWorld.forged(forged, netherite).getItem().getDefaultInstance();
					base.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING), 2);
					ItemStack reagent = new ItemStack(switch (forged) {
						case LUMENEDGE -> dev.wildercord.wildlife.Wildlife.LUMEN_ANTLER;
						case SKYREND_GLAIVE -> dev.wildercord.content.Reagents.FULGURITE_SHARD;
						case BULWARK_MAUL -> dev.wildercord.content.Reagents.GEODE_GRIT;
					});
					String id = forged.id + "_from_" + (netherite ? "netherite" : "diamond");
					RecipeHolder<?> holder = level.getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, dev.wildercord.Wildercord.id(id))).orElse(null);
					if (holder == null) {
						return "the recipe " + id + " should exist";
					}
					Recipe<SmithingRecipeInput> recipe = (Recipe<SmithingRecipeInput>) holder.value();
					SmithingRecipeInput input = new SmithingRecipeInput(new ItemStack(AuraWorld.AURA_SHARD), base, reagent);
					if (!recipe.matches(input, level)) {
						return id + " should take an Aura Shard, the weapon and its reagent";
					}
					ItemStack out = recipe.assemble(input);
					if (ForgedGear.of(out) != forged || !out.is(base.getItem()) || out.getEnchantments().isEmpty()) {
						return id + " should forge the weapon itself, keeping its enchantments (" + out + ")";
					}
				}
			}
			RecipeHolder<?> pages = level.getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,
				dev.wildercord.Wildercord.id("breathing_manual_from_pages"))).orElse(null);
			if (pages == null) {
				return "the pages' recipe should exist";
			}
			Recipe<CraftingInput> bind = (Recipe<CraftingInput>) pages.value();
			List<ItemStack> grid = new ArrayList<>();
			for (int i = 0; i < 4; i++) {
				grid.add(ManualPageItem.of(AuraWorld.MANUAL_PAGE, "gale"));
			}
			grid.add(new ItemStack(Items.BOOK));
			CraftingInput same = CraftingInput.of(3, 2, List.of(grid.get(0), grid.get(1), grid.get(2), grid.get(3), grid.get(4), ItemStack.EMPTY));
			if (!bind.matches(same, level) || !"gale".equals(bind.assemble(same).get(BreathingManualItem.METHOD))) {
				return "four Gale pages and a book should bind into Gale Breath's manual";
			}
			grid.set(2, ManualPageItem.of(AuraWorld.MANUAL_PAGE, "rime"));
			CraftingInput mixed = CraftingInput.of(3, 2, List.of(grid.get(0), grid.get(1), grid.get(2), grid.get(3), grid.get(4), ItemStack.EMPTY));
			if (bind.matches(mixed, level)) {
				return "pages of two methods shouldn't bind";
			}
			return level.getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, dev.wildercord.Wildercord.id("breath_sash"))).isPresent()
				? null : "the Breath Sash's recipe should exist";
		});
		check(recipes == null, recipes);

		// Lumenedge: a blow's aura, half again.
		double[] gains = on(world, player -> {
			setAura(player, "ember", AuraRules.EDGE, 0, AuraRules.threshold(AuraRules.EDGE));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
			double plain = AuraApi.gain(player, 2.0, "hit");
			setAura(player, "ember", AuraRules.EDGE, 0, AuraRules.threshold(AuraRules.EDGE));
			player.setItemInHand(InteractionHand.MAIN_HAND, AuraWorld.forged(AuraWorldRules.Forged.LUMENEDGE, false));
			double forged = AuraApi.gain(player, 2.0, "hit");
			double stance = AuraApi.gain(player, 2.0, "stance");
			return new double[] {plain, forged, stance};
		});
		check(Math.abs(gains[1] - gains[0] * AuraWorldRules.LUMENEDGE_GAIN) < 1e-3, "Lumenedge should give half again a blow's aura (" + gains[1] + " against " + gains[0] + ")");
		check(Math.abs(gains[2] - 2.0) < 1e-3, "Lumenedge gives nothing extra to the stance (" + gains[2] + ")");

		// Skyrend Glaive: a slash 1.6 times as hard, flying further.
		double plain = slashOn(context, world, new ItemStack(Items.NETHERITE_SPEAR), 4);
		double glaive = slashOn(context, world, AuraWorld.forged(AuraWorldRules.Forged.SKYREND_GLAIVE, true), 4);
		check(plain > 0 && Math.abs(glaive / plain - AuraWorldRules.SKYREND_SLASH) < 0.05, "Skyrend Glaive's slash should be 1.6 times as hard (" + glaive + " against " + plain + ")");
		double far = slashOn(context, world, new ItemStack(Items.NETHERITE_SPEAR), 17.5);
		double farGlaive = slashOn(context, world, AuraWorld.forged(AuraWorldRules.Forged.SKYREND_GLAIVE, true), 17.5);
		check(far <= 1e-6 && farGlaive > 0, "only the glaive's crescent should reach 17 blocks (" + far + ", " + farGlaive + ")");

		// Bulwark Maul: the guard costs less.
		double[] costs = on(world, player -> {
			setAura(player, "stone", AuraRules.FLOW, 40, AuraRules.threshold(AuraRules.FLOW));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_AXE));
			AuraGuard.raise(player);
			double plainCost = 40 - Aura.data(player).aura();
			setAura(player, "stone", AuraRules.FLOW, 40, AuraRules.threshold(AuraRules.FLOW));
			player.setItemInHand(InteractionHand.MAIN_HAND, AuraWorld.forged(AuraWorldRules.Forged.BULWARK_MAUL, false));
			AuraGuard.raise(player);
			return new double[] {plainCost, 40 - Aura.data(player).aura()};
		});
		check(Math.abs(costs[1] - costs[0] * AuraWorldRules.BULWARK_GUARD_COST) < 1e-3, "Bulwark Maul's guard should cost less (" + costs[1] + " against " + costs[0] + ")");

		// The Breath Sash: more aura held (the client sees it too), and the stance settles sooner.
		int[] capacity = on(world, player -> {
			setAura(player, "rime", AuraRules.EDGE, 0, AuraRules.threshold(AuraRules.EDGE));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			int without = Aura.capacity(player);
			GearSlots.set(player, GearSlot.TOME, new ItemStack(GearItems.get(GearDef.BREATH_SASH)));
			return new int[] {without, Aura.capacity(player)};
		});
		check(capacity[0] == AuraRules.capacity(AuraRules.EDGE) && capacity[1] == AuraWorldRules.sashCapacity(capacity[0], AuraWorldRules.SASH_CAPACITY),
			"the Breath Sash should raise aura capacity (" + capacity[0] + " to " + capacity[1] + ")");
		context.waitTicks(4);
		int seen = context.computeOnClient(mc -> Aura.capacity(mc.player));
		check(seen == capacity[1], "the client should see the sash's capacity too (" + seen + ")");
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(AuraWorldRules.SASH_SETTLE + 4);
		boolean settled = on(world, player -> Aura.state(player).breathing());
		context.getInput().releaseKey(o -> o.keyShift);
		check(settled, "with the sash the stance should settle in half the time");
		on(world, player -> {
			GearSlots.clear(player, GearSlot.TOME);
			return null;
		});
	}

	/** A slash from the player with {@code weapon} at a fresh husk {@code ahead} blocks off: what it took. */
	private static double slashOn(ClientGameTestContext context, TestSingleplayerContext world, ItemStack weapon, double ahead) {
		on(world, player -> {
			kill(player, TAG);
			stand(player);
			player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), 0.0F, 0.0F, false);
			setAura(player, "starlit", AuraRules.EDGE, AuraRules.capacity(AuraRules.EDGE), AuraRules.threshold(AuraRules.EDGE));
			player.setItemInHand(InteractionHand.MAIN_HAND, weapon.copy());
			spawn(player.level(), EntityTypes.HUSK, at(0, ahead), 400).addTag("wildercord.slashed");
			return null;
		});
		context.waitTicks(6);
		on(world, player -> AuraSlash.loose(player));
		context.waitTicks(16);
		return on(world, player -> {
			Mob husk = tagged(player, "wildercord.slashed");
			return (double) (husk.getMaxHealth() - husk.getHealth());
		});
	}

	// ------------------------------------------------------------------ a fallen knight

	private static void knight(ClientGameTestContext context, TestSingleplayerContext world) {
		// A dark room with a spawner on cobblestone: a dungeon, where a rank 1 knight rises.
		int y = STAGE.getY() - 30;
		world.getServer().runCommand("fill -10 " + (y - 1) + " -10 10 " + (y + 5) + " 10 minecraft:stone hollow");
		world.getServer().runCommand("setblock 0 " + (y - 1) + " 0 minecraft:cobblestone");
		world.getServer().runCommand("setblock 0 " + y + " 0 minecraft:spawner");
		on(world, player -> {
			player.teleportTo(player.level(), 0.5, y, 4.5, Set.<Relative>of(), 180.0F, 0.0F, false);
			return null;
		});
		context.waitTicks(30);
		String risen = on(world, player -> {
			ServerLevel level = player.level();
			KnightSpawner.Haunt haunt = KnightSpawner.haunt(level, player.blockPosition());
			if (haunt == null || haunt.rank() != 1) {
				return "a spawner room should be a haunt of rank 1 (" + haunt + ")";
			}
			FallenKnight knight = KnightSpawner.spawnIn(level, player.blockPosition(), haunt, level.getRandom());
			if (knight == null) {
				return "a knight should rise in the dark room";
			}
			knight.addTag(TAG);
			if (knight.rank() != 1 || Math.abs(knight.getMaxHealth() - AuraWorldRules.knightHealth(1)) > 1e-3 || Math.abs(knight.getY() - y) > 1) {
				return "it should be rank 1, inside the room (" + knight.rank() + ", y " + knight.getY() + ")";
			}
			var tag = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getTagOrEmpty(KnightSpawner.HAUNTS);
			List<String> haunts = new ArrayList<>();
			tag.forEach(h -> haunts.add(h.unwrapKey().map(k -> k.identifier().toString()).orElse("?")));
			return haunts.contains("minecraft:stronghold") && haunts.contains("minecraft:ancient_city") && haunts.contains("wildercord:ember_sanctum")
				? null : "knights should haunt strongholds, ancient cities and the expeditions (" + haunts + ")";
		});
		check(risen == null, risen);
		on(world, player -> {
			kill(player, TAG);
			stand(player);
			return null;
		});
		world.getServer().runCommand("fill -10 " + (y - 1) + " -10 10 " + (y + 5) + " 10 minecraft:air");

		// Its slash: the blade raised high (the tell), then a crescent through the spell defences.
		on(world, player -> {
			player.setHealth(player.getMaxHealth());
			player.removeAttached(WildercordAttachments.SPELLGUARD);
			setAura(player, "stone", AuraRules.FLOW, AuraRules.capacity(AuraRules.FLOW), AuraRules.threshold(AuraRules.FLOW));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			knight(player, "thunder", 2, 0, 9).addTag("wildercord.knight");
			return null;
		});
		boolean telegraphed = false;
		for (int t = 0; t < 80 && !telegraphed; t++) {
			context.waitTicks(1);
			telegraphed = on(world, player -> ((FallenKnight) tagged(player, "wildercord.knight")).state(AuraFighter.WINDUP));
		}
		check(telegraphed, "a knight out of reach should raise its blade to slash");
		context.waitTicks(AuraWorldRules.knightSlashWindup(2) + 14);
		float struck = on(world, player -> player.getMaxHealth() - player.getHealth());
		check(struck > 2 && struck < AuraWorldRules.knightSlash(2) + 1, "its slash should land through the spell defences (" + struck + ")");
		// Stepping off its line dodges it.
		on(world, player -> {
			player.setHealth(player.getMaxHealth());
			FallenKnight knight = (FallenKnight) tagged(player, "wildercord.knight");
			knight.setHealth(knight.getMaxHealth());
			return null;
		});
		on(world, player -> {
			stand(player);
			FallenKnight knight = (FallenKnight) tagged(player, "wildercord.knight");
			knight.snapTo(STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 9.5, 180, 0);
			knight.windUp(player, AuraWorldRules.knightSlashWindup(2), 400);
			return null;
		});
		context.waitTicks(AuraWorldRules.knightSlashWindup(2) - 2);
		on(world, player -> {
			player.teleportTo(player.level(), player.getX() + 4.5, player.getY(), player.getZ(), Set.<Relative>of(), 0.0F, 8.0F, false);
			return null;
		});
		context.waitTicks(16);
		float dodged = on(world, player -> player.getMaxHealth() - player.getHealth());
		check(dodged <= 1e-3, "stepping off the line after its aim fixed should dodge the slash (took " + dodged + ")");

		// A perfect guard sends its crescent back at it.
		on(world, player -> {
			stand(player);
			FallenKnight knight = (FallenKnight) tagged(player, "wildercord.knight");
			knight.setNoAi(true);
			knight.snapTo(STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 5.5, 180, 0);
			knight.setHealth(knight.getMaxHealth());
			return null;
		});
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(3);
		String turned = on(world, player -> {
			FallenKnight knight = (FallenKnight) tagged(player, "wildercord.knight");
			if (!AuraGuard.raise(player)) {
				return "the guard should rise";
			}
			Vec3 from = knight.getEyePosition().subtract(0, 0.45, 0);
			Vec3 aim = player.getBoundingBox().getCenter().subtract(from).normalize();
			Crescents.launch(knight, from, aim, knight.auraColor(), 6, 1, AuraWorldRules.KNIGHT_SLASH_SPEED * 2, 14, 3, 6, false,
				e -> e instanceof ServerPlayer, (flight, target) -> knight.projected(target, flight.damage()));
			return null;
		});
		check(turned == null, turned);
		context.waitTicks(12);
		String reflected = on(world, player -> {
			FallenKnight knight = (FallenKnight) tagged(player, "wildercord.knight");
			if (player.getHealth() < player.getMaxHealth()) {
				return "a perfect guard should turn the crescent whole (took " + (player.getMaxHealth() - player.getHealth()) + ")";
			}
			return knight.getHealth() < knight.getMaxHealth() ? null : "the crescent should fly back and cut the knight";
		});
		context.getInput().releaseKey(o -> o.keyShift);
		check(reflected == null, reflected);

		// Its own guard: a rushed blow staggers you, a held one is halved, and an axe breaks it.
		context.waitTicks(25);
		float open = on(world, player -> {
			player.removeAllEffects();
			FallenKnight knight = (FallenKnight) tagged(player, "wildercord.knight");
			knight.snapTo(STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 2.3, 180, 0);
			knight.setHealth(knight.getMaxHealth());
			setAura(player, "stone", AuraRules.GLOW, 0, 0);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			float before = knight.getHealth();
			player.attack(knight);
			return before - knight.getHealth();
		});
		context.waitTicks(25);
		String rushed = on(world, player -> {
			FallenKnight knight = (FallenKnight) tagged(player, "wildercord.knight");
			knight.setHealth(knight.getMaxHealth());
			knight.raiseGuard();
			player.attack(knight);
			return knight.getHealth() >= knight.getMaxHealth() && player.hasEffect(MobEffects.SLOWNESS) ? null
				: "a blow into its perfect guard should be turned and stagger the player";
		});
		check(rushed == null, rushed);
		context.waitTicks(16);
		float held = on(world, player -> {
			player.removeAllEffects();
			FallenKnight knight = (FallenKnight) tagged(player, "wildercord.knight");
			float before = knight.getHealth();
			player.attack(knight);
			return before - knight.getHealth();
		});
		check(open > 0 && Math.abs(held - open * 0.5) < 0.6, "its held guard should halve a blow (" + held + " against " + open + ")");
		context.waitTicks(AuraWorldRules.MOB_GUARD_REST + 25);
		String broken = on(world, player -> {
			FallenKnight knight = (FallenKnight) tagged(player, "wildercord.knight");
			knight.raiseGuard();
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_AXE));
			return null;
		});
		check(broken == null, broken);
		context.waitTicks(14);
		String axed = on(world, player -> {
			FallenKnight knight = (FallenKnight) tagged(player, "wildercord.knight");
			float before = knight.getHealth();
			player.attack(knight);
			if (knight.guarding() || !knight.state(AuraFighter.STAGGER)) {
				return "an axe should break its guard and leave it reeling";
			}
			return knight.getHealth() < before ? null : "the axe's blow should land in full";
		});
		check(axed == null, axed);

		// Its loot: always a page of its method, an Aura Shard about a time in four.
		String loot = on(world, player -> {
			ServerLevel level = player.level();
			FallenKnight knight = (FallenKnight) tagged(player, "wildercord.knight");
			LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
				dev.wildercord.Wildercord.id("entities/fallen_knight")));
			int shards = 0;
			for (int i = 0; i < 400; i++) {
				LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, knight)
					.withParameter(LootContextParams.ORIGIN, knight.position())
					.withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(player))
					.withOptionalParameter(LootContextParams.ATTACKING_ENTITY, player)
					.withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, player)
					.withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
					.create(LootContextParamSets.ENTITY);
				boolean page = false;
				for (ItemStack stack : table.getRandomItems(params)) {
					if (stack.is(AuraWorld.MANUAL_PAGE)) {
						if (!"thunder".equals(stack.get(BreathingManualItem.METHOD))) {
							return "a Thunder knight's pages should be Thunder Breath's (" + stack.get(BreathingManualItem.METHOD) + ")";
						}
						page = true;
					}
					if (stack.is(AuraWorld.AURA_SHARD)) {
						shards++;
					}
				}
				if (!page) {
					return "a knight should always drop a manual page";
				}
			}
			return shards > 60 && shards < 140 ? null : "about one knight in four should drop an Aura Shard (" + shards + " of 400)";
		});
		check(loot == null, loot);
	}

	// ------------------------------------------------------------------ a clash

	private static void clash(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "ember", AuraRules.EDGE, AuraRules.capacity(AuraRules.EDGE), AuraRules.threshold(AuraRules.EDGE));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			FallenKnight knight = knight(player, "hollow", 3, 0, 14);
			knight.setNoAi(true);
			knight.addTag("wildercord.clash_knight");
			// A husk behind each of them, each in the other's crescent's line.
			spawn(player.level(), EntityTypes.HUSK, at(0, -2), 200).addTag("wildercord.behind_player");
			spawn(player.level(), EntityTypes.HUSK, at(0, 16.5), 200).addTag("wildercord.behind_knight");
			player.setHealth(player.getMaxHealth());
			return null;
		});
		context.waitTicks(5);
		on(world, player -> {
			FallenKnight knight = (FallenKnight) tagged(player, "wildercord.clash_knight");
			Vec3 from = knight.getEyePosition().subtract(0, 0.45, 0);
			Crescents.launch(knight, from, new Vec3(0, 0, -1), knight.auraColor(), 6, 1, AuraWorldRules.KNIGHT_SLASH_SPEED, 16, 3, 6, false,
				e -> e instanceof LivingEntity && !(e instanceof FallenKnight), (flight, target) -> knight.projected(target, flight.damage()));
			AuraSlash.loose(player);
			return null;
		});
		boolean met = false;
		for (int t = 0; t < 14 && !met; t++) {
			context.waitTicks(1);
			met = on(world, player -> Crescents.inFlight().isEmpty());
		}
		context.waitTicks(20);
		String clashed = on(world, player -> {
			Mob a = tagged(player, "wildercord.behind_player");
			Mob b = tagged(player, "wildercord.behind_knight");
			FallenKnight knight = (FallenKnight) tagged(player, "wildercord.clash_knight");
			if (a.getHealth() < a.getMaxHealth() || b.getHealth() < b.getMaxHealth() || player.getHealth() < player.getMaxHealth()
					|| knight.getHealth() < knight.getMaxHealth()) {
				return "two crescents meeting head on should both break, harming nobody (player " + player.getHealth() + ", knight " + knight.getHealth() + ")";
			}
			return Heart.grimoire(player).contains("aura:clash") ? null : "a first clash should go into the Grimoire";
		});
		check(met, "both crescents should be gone once they meet");
		check(clashed == null, clashed);
	}

	// ------------------------------------------------------------------ the slash by day

	private static void daylight(ClientGameTestContext context, TestSingleplayerContext world) {
		for (String time : List.of("day", "night")) {
			world.getServer().runCommand(time.equals("day") ? "time set 6000" : "time set 18000");
			for (String method : List.of("gale", "thunder", "ember")) {
				on(world, player -> {
					stand(player);
					setAura(player, method, AuraRules.EDGE, AuraRules.capacity(AuraRules.EDGE), AuraRules.threshold(AuraRules.EDGE));
					player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
					player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), 0.0F, -8.0F, false);
					return null;
				});
				// Off to the side and low, looking up across the crescent's path: the sky behind it.
				director(context, world, at(5.5, 3.2).add(0, 0.4, 0), at(0, 6.6).add(0, 1.9, 0));
				context.waitTicks(4);
				boolean loosed = on(world, player -> AuraSlash.loose(player));
				context.waitTicks(3);
				boolean flying = on(world, player -> !Crescents.inFlight().isEmpty());
				shot(context, "aura_world_slash_" + time + "_" + method);
				cut(context);
				check(loosed && flying, "the slash should be loosed and in flight for its picture (" + method + " by " + time + ")");
				context.waitTicks(20);
			}
		}
		world.getServer().runCommand("time set 6000");
		// A clash, seen from the side: two knights' crescents meeting.
		on(world, player -> {
			stand(player);
			FallenKnight a = knight(player, "ember", 3, -6, 8, 270);
			FallenKnight b = knight(player, "rime", 3, 6, 8, 90);
			a.setNoAi(true);
			b.setNoAi(true);
			a.addTag("wildercord.clash_a");
			b.addTag("wildercord.clash_b");
			return null;
		});
		director(context, world, at(0, -1.5).add(0, 2.2, 0), at(0, 8).add(0, 1.3, 0));
		context.waitTicks(4);
		on(world, player -> {
			for (String tag : List.of("wildercord.clash_a", "wildercord.clash_b")) {
				FallenKnight k = (FallenKnight) tagged(player, tag);
				Vec3 from = k.getEyePosition().subtract(0, 0.45, 0);
				Vec3 aim = new Vec3(tag.endsWith("a") ? 1 : -1, 0, 0);
				Crescents.launch(k, from, aim, k.auraColor(), 6, 1, AuraWorldRules.KNIGHT_SLASH_SPEED * 1.3, 14, 3, 6, false,
					e -> e instanceof ServerPlayer, (flight, target) -> k.projected(target, flight.damage()));
			}
			return null;
		});
		boolean met = false;
		for (int t = 0; t < 12 && !met; t++) {
			context.waitTicks(1);
			met = on(world, player -> Crescents.inFlight().isEmpty());
		}
		shot(context, "aura_world_clash");
		cut(context);
		check(met, "the two knights' crescents should meet and break");
	}

	/** Watches from a camera of its own at {@code eye}, looking at {@code target} (the HUD hidden). */
	private static void director(ClientGameTestContext context, TestSingleplayerContext world, Vec3 eye, Vec3 target) {
		int id = on(world, player -> {
			ServerLevel level = player.level();
			level.getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class, player.getBoundingBox().inflate(128),
				e -> e.entityTags().contains("wildercord.camera")).forEach(Entity::discard);
			net.minecraft.world.entity.Display.TextDisplay camera = EntityTypes.TEXT_DISPLAY.create(level, EntitySpawnReason.COMMAND);
			Vec3 d = target.subtract(eye);
			float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
			float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
			camera.snapTo(eye.x, eye.y, eye.z, yaw, pitch);
			camera.addTag("wildercord.camera");
			camera.addTag(TAG);
			level.addFreshEntity(camera);
			return camera.getId();
		});
		context.waitTicks(3);
		context.runOnClient(mc -> {
			Entity camera = mc.level.getEntity(id);
			if (camera != null) {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.setCameraEntity(camera);
				if (!mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
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

	// ------------------------------------------------------------------ pictures

	private static void pictures(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("time set 6000");
		// Every duelist, side by side.
		on(world, player -> {
			stand(player);
			int i = 0;
			for (var method : BreathingMethods.BUILT_IN) {
				Duelist d = duelist(player, method.id(), -6.75 + 1.5 * i, 7.5);
				d.setNoAi(true);
				d.holdPose(0);
				i++;
			}
			return null;
		});
		frame(context, world, 0, 9, "aura_world_duelists");
		on(world, player -> {
			kill(player, TAG);
			return null;
		});
		// Close up, a few methods.
		for (String method : List.of("ember", "rime", "verdant", "hollow")) {
			on(world, player -> {
				Duelist d = duelist(player, method, 0, 3, 180 + 25);
				d.setNoAi(true);
				d.holdPose(0);
				return null;
			});
			frame(context, world, 0, 12, "aura_world_duelist_" + method);
			on(world, player -> {
				kill(player, TAG);
				return null;
			});
		}
		// Its poses: drawn in guard, the slash's tell, the bow, yielding, by its fire.
		record Pose(String name, int flags, boolean sword, int yaw) {}
		for (Pose pose : List.of(new Pose("guard", AuraFighter.DRAWN | AuraFighter.GUARD, true, 160), new Pose("windup", AuraFighter.DRAWN | AuraFighter.WINDUP, true, 160),
				new Pose("bow", AuraFighter.BOW, false, 200), new Pose("yield", AuraFighter.DRAWN | AuraFighter.YIELD, true, 150),
				new Pose("sit", AuraFighter.SIT, false, 215))) {
			on(world, player -> {
				Duelist d = duelist(player, "ember", 0, 3, pose.yaw());
				d.setNoAi(true);
				d.setStage(AuraRules.EDGE);
				if (pose.sword()) {
					d.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
				}
				d.holdPose(pose.flags());
				if (pose.name().equals("sit")) {
					player.level().setBlockAndUpdate(BlockPos.containing(at(1.7, 3.6)), Blocks.CAMPFIRE.defaultBlockState());
				}
				return null;
			});
			frame(context, world, 0, 12, "aura_world_duelist_" + pose.name());
			on(world, player -> {
				kill(player, TAG);
				player.level().setBlockAndUpdate(BlockPos.containing(at(1.7, 3.6)), Blocks.AIR.defaultBlockState());
				return null;
			});
		}
		// The knight: standing, its tell, its guard; and at night, its glow.
		record KnightPose(String name, int flags, int yaw) {}
		for (KnightPose pose : List.of(new KnightPose("", AuraFighter.DRAWN, 205), new KnightPose("_windup", AuraFighter.DRAWN | AuraFighter.WINDUP, 160),
				new KnightPose("_guard", AuraFighter.DRAWN | AuraFighter.GUARD, 160))) {
			on(world, player -> {
				FallenKnight k = knight(player, "hollow", 3, 0, 3, pose.yaw());
				k.setNoAi(true);
				k.holdPose(pose.flags());
				return null;
			});
			frame(context, world, 0, 12, "aura_world_knight" + pose.name());
			on(world, player -> {
				kill(player, TAG);
				return null;
			});
		}
		world.getServer().runCommand("time set 18000");
		on(world, player -> {
			FallenKnight k = knight(player, "crimson", 3, 0, 3, 200);
			k.setNoAi(true);
			k.holdPose(AuraFighter.DRAWN);
			return null;
		});
		frame(context, world, 0, 12, "aura_world_knight_night");
		on(world, player -> {
			kill(player, TAG);
			return null;
		});
		world.getServer().runCommand("time set 6000");

		// The items, in frames on a wall.
		List<ItemStack> items = new ArrayList<>();
		for (String method : List.of("ember", "rime", "thunder", "verdant", "hollow")) {
			items.add(ManualPageItem.of(AuraWorld.MANUAL_PAGE, method));
		}
		items.add(new ItemStack(AuraWorld.AURA_SHARD));
		items.add(new ItemStack(GearItems.get(GearDef.BREATH_SASH)));
		items.add(AuraWorld.forged(AuraWorldRules.Forged.LUMENEDGE, false));
		items.add(AuraWorld.forged(AuraWorldRules.Forged.SKYREND_GLAIVE, false));
		items.add(AuraWorld.forged(AuraWorldRules.Forged.BULWARK_MAUL, false));
		items.add(new ItemStack(AuraWorld.DUELIST_SPAWN_EGG));
		items.add(new ItemStack(AuraWorld.FALLEN_KNIGHT_SPAWN_EGG));
		int wallZ = STAGE.getZ() + 4;
		world.getServer().runCommand("fill -4 " + STAGE.getY() + " " + wallZ + " 3 " + (STAGE.getY() + 2) + " " + wallZ + " minecraft:spruce_planks");
		on(world, player -> {
			for (int i = 0; i < items.size(); i++) {
				int x = -3 + i % 6;
				int y = STAGE.getY() + 2 - i / 6;
				ItemFrame frame = new ItemFrame(player.level(), new BlockPos(x, y, wallZ - 1), Direction.NORTH);
				frame.setItem(items.get(i), false);
				frame.addTag(TAG);
				player.level().addFreshEntity(frame);
			}
			player.teleportTo(player.level(), 0.0, STAGE.getY() + 0.2, wallZ - 3.6, Set.<Relative>of(), 0.0F, 10.0F, false);
			return null;
		});
		context.runOnClient(mc -> {
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		context.waitTicks(10);
		shot(context, "aura_world_items");
		context.runOnClient(mc -> {
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		on(world, player -> {
			for (Entity e : player.level().getEntitiesOfClass(ItemFrame.class, player.getBoundingBox().inflate(16))) {
				e.discard();
			}
			return null;
		});
		world.getServer().runCommand("fill -4 " + STAGE.getY() + " " + wallZ + " 3 " + (STAGE.getY() + 2) + " " + wallZ + " minecraft:air");
		world.getServer().runCommand("kill @e[type=item]");

		// The forged weapons in hand, their aura round them.
		for (AuraWorldRules.Forged forged : AuraWorldRules.Forged.values()) {
			on(world, player -> {
				stand(player);
				setAura(player, forged == AuraWorldRules.Forged.LUMENEDGE ? "starlit" : forged == AuraWorldRules.Forged.SKYREND_GLAIVE ? "thunder" : "stone",
					AuraRules.EDGE, AuraRules.capacity(AuraRules.EDGE), AuraRules.threshold(AuraRules.EDGE));
				player.setItemInHand(InteractionHand.MAIN_HAND, AuraWorld.forged(forged, true));
				player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), 0.0F, -22.0F, false);
				player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(2.3);
				return null;
			});
			context.waitTicks(8);
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
				if (!mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
			context.waitTicks(3);
			if (forged == AuraWorldRules.Forged.SKYREND_GLAIVE) {
				// A glaive is held upright with its blade's flat to the sides, so from the front it's only a line, and a camera
				// of its own doesn't draw the player: a duelist holds it side on instead.
				on(world, player -> {
					player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
					Duelist d = duelist(player, "thunder", 0, 3.4, 115);
					d.setNoAi(true);
					d.setStage(AuraRules.EDGE);
					d.setItemSlot(EquipmentSlot.MAINHAND, AuraWorld.forged(forged, true));
					d.holdPose(AuraFighter.DRAWN);
					return null;
				});
				frame(context, world, 0, 14, "aura_world_" + forged.id + "_in_hand");
				on(world, player -> {
					kill(player, TAG);
					return null;
				});
			} else {
				context.runOnClient(mc -> {
					mc.player.setYBodyRot(48);
					mc.player.yBodyRotO = 48;
				});
				context.waitTicks(2);
				shot(context, "aura_world_" + forged.id + "_in_hand");
			}
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				if (mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
			on(world, player -> {
				player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(4.0);
				return null;
			});
		}
	}

	/** Frames what stands in front: the player at {@code standZ} blocks along the platform, looking down it, the HUD hidden. */
	private static void frame(ClientGameTestContext context, TestSingleplayerContext world, double standZ, float pitch, String name) {
		on(world, player -> {
			player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5 + standZ, Set.<Relative>of(), 0.0F, pitch, false);
			return null;
		});
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		context.waitTicks(12);
		shot(context, name);
		context.runOnClient(mc -> {
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
	}

	private static void face(LivingEntity e, float yaw) {
		e.setYRot(yaw);
		e.setYHeadRot(yaw);
		e.setYBodyRot(yaw);
		e.yBodyRotO = yaw;
		e.yHeadRotO = yaw;
	}

	// ------------------------------------------------------------------ the stage

	private static void stage(TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 12) + " " + (y - 1) + " " + (z - 8) + " " + (x + 12) + " " + (y - 1) + " " + (z + 26) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 12) + " " + y + " " + (z - 8) + " " + (x + 12) + " " + (y + 6) + " " + (z + 26) + " minecraft:air");
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
	}

	private static void reset(ClientGameTestContext context, TestSingleplayerContext world) {
		context.getInput().releaseKey(o -> o.keyShift);
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runCommand("kill @e[type=wildercord:duelist]");
		world.getServer().runCommand("kill @e[type=wildercord:fallen_knight]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		context.waitTicks(10);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			stand(player);
			player.removeAttached(AuraAttachments.STATE);
		});
		context.waitTicks(5);
	}

	private static void setAura(ServerPlayer player, String method, int stage, float aura, double xp) {
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data(method, stage, xp, aura, 0));
		player.removeAttached(AuraAttachments.STATE);
	}

	private static Vec3 at(double side, double ahead) {
		return new Vec3(STAGE.getX() + 0.5 + side, STAGE.getY(), STAGE.getZ() + 0.5 + ahead);
	}

	/** A duelist of {@code method} on the platform, facing the player. */
	private static Duelist duelist(ServerPlayer player, String method, double side, double ahead) {
		return duelist(player, method, side, ahead, 180);
	}

	/** A duelist of {@code method} on the platform, facing {@code yaw}. */
	private static Duelist duelist(ServerPlayer player, String method, double side, double ahead, float yaw) {
		ServerLevel level = player.level();
		Duelist duelist = AuraWorld.DUELIST.create(level, EntitySpawnReason.COMMAND);
		Vec3 p = at(side, ahead);
		duelist.snapTo(p.x, p.y, p.z, yaw, 0);
		duelist.setYHeadRot(yaw);
		duelist.setYBodyRot(yaw);
		duelist.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(p)), EntitySpawnReason.COMMAND, null);
		duelist.setMethod(BreathingMethods.byId(method).orElseThrow());
		duelist.addTag(TAG);
		level.addFreshEntity(duelist);
		return duelist;
	}

	/** A fallen knight of {@code method} and {@code rank} on the platform, hunting the player. */
	private static FallenKnight knight(ServerPlayer player, String method, int rank, double side, double ahead) {
		return knight(player, method, rank, side, ahead, 180);
	}

	private static FallenKnight knight(ServerPlayer player, String method, int rank, double side, double ahead, float yaw) {
		ServerLevel level = player.level();
		FallenKnight knight = AuraWorld.FALLEN_KNIGHT.create(level, EntitySpawnReason.COMMAND);
		Vec3 p = at(side, ahead);
		knight.snapTo(p.x, p.y, p.z, yaw, 0);
		knight.setYHeadRot(yaw);
		knight.setYBodyRot(yaw);
		knight.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(p)), EntitySpawnReason.COMMAND, null);
		knight.setMethod(BreathingMethods.byId(method).orElseThrow());
		knight.setRank(rank);
		knight.addTag(TAG);
		knight.setTarget(player);
		level.addFreshEntity(knight);
		return knight;
	}

	private static <T extends Mob> T spawn(ServerLevel level, EntityType<T> type, Vec3 at, double health) {
		T mob = type.create(level, EntitySpawnReason.COMMAND);
		mob.snapTo(at.x, at.y, at.z, 180, 0);
		mob.setNoAi(true);
		mob.addTag(TAG);
		mob.addTag("wildercord.rolled");
		AttributeInstance max = mob.getAttribute(Attributes.MAX_HEALTH);
		if (max != null) {
			max.setBaseValue(health);
		}
		mob.setHealth((float) health);
		level.addFreshEntity(mob);
		return mob;
	}

	private static Mob tagged(ServerPlayer player, String tag) {
		List<Mob> found = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(64), m -> m.entityTags().contains(tag) && m.isAlive());
		if (found.isEmpty()) {
			throw new AssertionError("the mob tagged " + tag + " is gone");
		}
		return found.getFirst();
	}

	private static void kill(ServerPlayer player, String tag) {
		for (Entity e : player.level().getEntitiesOfClass(Entity.class, player.getBoundingBox().inflate(80), e -> e.entityTags().contains(tag))) {
			e.discard();
		}
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> mc.gui.toastManager().clear());
		context.waitTicks(1);
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static <T> T on(TestSingleplayerContext world, Function<ServerPlayer, T> step) {
		return world.getServer().computeOnServer(server -> step.apply(player(server)));
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}
}
