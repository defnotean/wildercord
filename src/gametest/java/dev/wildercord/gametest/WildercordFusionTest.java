package dev.wildercord.gametest;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.client.FusionAltarScreen;
import dev.wildercord.content.CordTier;
import dev.wildercord.content.RuneItem;
import dev.wildercord.content.WildercordBlocks;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.menu.FusionAltarMenu;
import dev.wildercord.player.Heart;
import dev.wildercord.player.RuneRanks;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.Fusions;
import dev.wildercord.spell.Knots;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Fusion Altar, server side: ranking a rune up (and that rank reaching the damage it does),
 * combining two effects (and the Grimoire recording it), tying a spell into a Knot (its cost, its
 * discount, the Cord's tier limit on what's inside, and anyone learning it), refusals when XP is
 * short, and every fused effect cast at a husk. Ends with a screenshot of the altar's screen.
 *
 * <p>Runs in the full suite; skipped with {@code WILDERCORD_TOUR_ONLY} or {@code WILDERCORD_CORDS_ONLY}.</p>
 */
public class WildercordFusionTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			List<String> failures = new ArrayList<>();
			for (String failure : world.getServer().computeOnServer(WildercordFusionTest::checks)) {
				failures.add(failure);
			}
			context.waitTicks(40);
			// The screen: open the altar with runes on it and look.
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				BlockPos pos = altar(player);
				player.openMenu(player.level().getBlockState(pos).getMenuProvider(player.level(), pos));
				if (player.containerMenu instanceof FusionAltarMenu menu) {
					for (int i = 0; i < FusionAltarMenu.RUNE_SLOTS; i++) {
						menu.getSlot(i).set(RuneItem.stack(Runes.FIRE));
					}
					menu.broadcastChanges();
				}
			});
			context.waitTicks(20);
			boolean open = context.computeOnClient(mc -> mc.gui.screen() instanceof FusionAltarScreen);
			if (!open) {
				failures.add("right-clicking the altar should open its screen");
			}
			context.takeScreenshot(TestScreenshotOptions.of("fusion_altar").disableCounterPrefix());
			context.runOnClient(mc -> mc.player.closeContainer());
			context.waitTicks(5);
			if (!failures.isEmpty()) {
				throw new AssertionError("The Fusion Altar went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	/** An altar two blocks in front of the player. */
	private static BlockPos altar(ServerPlayer player) {
		BlockPos pos = player.blockPosition().relative(player.getDirection(), 2);
		if (!player.level().getBlockState(pos).is(WildercordBlocks.FUSION_ALTAR)) {
			player.level().setBlockAndUpdate(pos, WildercordBlocks.FUSION_ALTAR.defaultBlockState());
		}
		return pos;
	}

	private static FusionAltarMenu menu(ServerPlayer player) {
		BlockPos pos = altar(player);
		return new FusionAltarMenu(0, player.getInventory(), ContainerLevelAccess.create(player.level(), pos));
	}

	/** Puts runes (or nulls) in the three rune slots and a catalyst in the fourth. */
	private static void load(FusionAltarMenu menu, ItemStack catalyst, ItemStack... runes) {
		for (int i = 0; i < FusionAltarMenu.RUNE_SLOTS; i++) {
			menu.getSlot(i).set(i < runes.length && runes[i] != null ? runes[i].copy() : ItemStack.EMPTY);
		}
		menu.getSlot(FusionAltarMenu.CATALYST).set(catalyst.copy());
	}

	private static ItemStack result(FusionAltarMenu menu) {
		return menu.getSlot(FusionAltarMenu.RESULT).getItem();
	}

	private static Mob husk(ServerPlayer player, double ahead) {
		ServerLevel level = player.level();
		Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		Vec3 at = player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(ahead));
		husk.snapTo(at.x, at.y, at.z, 0, 0);
		husk.setNoAi(true);
		husk.addTag("wildercord.fusion");
		// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
		husk.addTag("wildercord.rolled");
		level.addFreshEntity(husk);
		return husk;
	}

	/** Health {@code rune} takes off a fresh husk when {@code player} casts it at Touch. */
	private static float damage(ServerPlayer player, RuneDef rune) {
		Mob husk = husk(player, 3);
		float before = husk.getHealth();
		SpellCompiler.Compiled compiled = SpellCompiler.compile(List.of(Runes.TOUCH, rune));
		SpellPlan.EffectNode node = compiled.root().groups.getFirst().effects.getFirst();
		Effects.apply(new Cast(player), node, new Cast.Hit(List.of(husk), husk.position(), player.getLookAngle(), player.position(), null, null, false));
		float taken = before - husk.getHealth();
		husk.discard();
		return taken;
	}

	/** Uses a rune item from the main hand, as a right-click would. */
	private static void learn(ServerPlayer player, ItemStack stack) {
		player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
		stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
	}

	private static List<String> checks(MinecraftServer server) {
		List<String> failures = new ArrayList<>();
		ServerPlayer player = player(server);
		player.setGameMode(GameType.SURVIVAL);
		Spellbooks.setCord(player, new ItemStack(WildercordItems.AMETHYST_CORD));
		Spellbook book = Spellbooks.get(player).withStarterGiven();
		for (RuneDef rune : List.of(Runes.BOLT, Runes.TOUCH, Runes.FIRE, Runes.PUSH, Runes.HARM, Runes.AMPLIFY, Runes.SELF, Runes.HEAL)) {
			book = book.learn(rune.id());
		}
		Spellbooks.set(player, book.withSpell(0, List.of(Runes.BOLT.id(), Runes.FIRE.id(), Runes.AMPLIFY.id())));
		player.setExperienceLevels(30);

		// ---- Upgrade: three Harm runes make Harm II for 2 levels, and learning it makes Harm hit 25% harder.
		float before = damage(player, Runes.HARM);
		FusionAltarMenu menu = menu(player);
		ItemStack harm = RuneItem.stack(Runes.HARM);
		load(menu, ItemStack.EMPTY, harm, harm, harm);
		Fusions.Plan plan = menu.plan();
		if (plan.kind() != Fusions.Kind.UPGRADE || !plan.ready()) {
			failures.add("three Harm runes should plan an upgrade (" + plan.kind() + ", " + plan.problem() + ")");
		}
		if (!menu.clickMenuButton(player, FusionAltarMenu.BUTTON_FUSE)) {
			failures.add("the upgrade should go through");
		}
		ItemStack harm2 = result(menu).copy();
		if (!RuneItem.runeOf(harm2).map(r -> r.is(Runes.HARM.id())).orElse(false) || RuneItem.rankOf(harm2) != 2) {
			failures.add("the upgrade should make a rank II Harm rune (made " + harm2 + ")");
		}
		if (player.experienceLevel != 28) {
			failures.add("rank II should cost 2 levels (have " + player.experienceLevel + " of 30)");
		}
		if (!menu.getSlot(0).getItem().isEmpty()) {
			failures.add("the upgrade should use up the three runes");
		}
		if (!Heart.discovered(player, "feat:" + Feats.UPGRADE)) {
			failures.add("the first upgrade should earn its feat");
		}
		learn(player, harm2);
		if (RuneRanks.rank(player, Runes.HARM.id()) != 2) {
			failures.add("learning Harm II should rank Harm up (rank " + RuneRanks.rank(player, Runes.HARM.id()) + ")");
		}
		float after = damage(player, Runes.HARM);
		if (before <= 0 || Math.abs(after / before - 1.25F) > 0.05F) {
			failures.add("Harm II should hit 25% harder (" + before + " then " + after + ")");
		}
		String line = SpellCompiler.compile(List.of(Runes.BOLT, Runes.HARM), RuneRanks.lookup(player)).lines().getFirst();
		if (!line.contains("Harm II")) {
			failures.add("the readout should say Harm II (says \"" + line + "\")");
		}

		// ---- A full result slot blocks the next fusion rather than losing anything.
		load(menu, ItemStack.EMPTY, harm, harm, harm);
		if (menu.clickMenuButton(player, FusionAltarMenu.BUTTON_FUSE) || RuneItem.rankOf(result(menu)) != 2) {
			failures.add("fusing onto an untaken result should be refused");
		}
		menu.getSlot(FusionAltarMenu.RESULT).set(ItemStack.EMPTY);

		// ---- Combine: Fire and Push with a shard make Firestorm for 3 levels, and the Grimoire records it.
		load(menu, new ItemStack(Items.AMETHYST_SHARD), RuneItem.stack(Runes.FIRE), null, RuneItem.stack(Runes.PUSH));
		int levels = player.experienceLevel;
		if (!menu.clickMenuButton(player, FusionAltarMenu.BUTTON_FUSE)) {
			failures.add("Fire and Push with a shard should combine (" + menu.plan().problem() + ")");
		}
		if (!RuneItem.runeOf(result(menu)).map(r -> r.is(Runes.FIRESTORM.id())).orElse(false)) {
			failures.add("Fire and Push should make Firestorm (made " + result(menu) + ")");
		}
		// (Its first time also earns an advancement, whose experience can give a level back.)
		if (player.experienceLevel < levels - 3 || player.experienceLevel > levels - 2) {
			failures.add("combining should cost 3 levels");
		}
		if (!menu.getSlot(FusionAltarMenu.CATALYST).getItem().isEmpty()) {
			failures.add("combining should use up the shard");
		}
		if (!Heart.discovered(player, Fusions.recipeFor(Runes.FIRESTORM).orElseThrow().key()) || !Heart.discovered(player, "feat:" + Feats.COMBINE)) {
			failures.add("the first Firestorm should be recorded in the Grimoire, with its feat");
		}
		menu.getSlot(FusionAltarMenu.RESULT).set(ItemStack.EMPTY);
		// Two effects of one element fuse too: that element at its purest.
		load(menu, new ItemStack(Items.AMETHYST_SHARD), RuneItem.stack(Runes.FIRE), RuneItem.stack(Runes.EMBER));
		if (!menu.clickMenuButton(player, FusionAltarMenu.BUTTON_FUSE)
				|| !RuneItem.runeOf(result(menu)).map(r -> r.is(Runes.CONFLAGRATION.id())).orElse(false)) {
			failures.add("Fire and Ember with a shard should make Conflagration (made " + result(menu) + ", " + menu.plan().problem() + ")");
		}
		menu.getSlot(FusionAltarMenu.RESULT).set(ItemStack.EMPTY);
		// A shape isn't an effect: nothing fuses with it.
		load(menu, new ItemStack(Items.AMETHYST_SHARD), RuneItem.stack(Runes.FIRE), RuneItem.stack(Runes.BOLT));
		if (menu.clickMenuButton(player, FusionAltarMenu.BUTTON_FUSE) || !result(menu).isEmpty()) {
			failures.add("a fire rune and a shape shouldn't fuse");
		}

		// ---- Too little XP: refused, nothing used up.
		player.setExperienceLevels(1);
		load(menu, new ItemStack(Items.AMETHYST_SHARD), RuneItem.stack(Runes.FIRE), RuneItem.stack(Runes.PUSH));
		if (menu.clickMenuButton(player, FusionAltarMenu.BUTTON_FUSE) || menu.getSlot(0).getItem().isEmpty()) {
			failures.add("a fusion without enough XP should be refused and keep the runes");
		}
		player.setExperienceLevels(30);

		// ---- Tie a Knot: spell 1 (Bolt · Fire · Amplify) into one rune for 3 levels.
		load(menu, new ItemStack(Items.STRING), null, new ItemStack(WildercordItems.BLANK_RUNE));
		levels = player.experienceLevel;
		if (!menu.clickMenuButton(player, FusionAltarMenu.BUTTON_KNOT)) {
			failures.add("a Blank Rune, string and spell 1 should tie a Knot (" + menu.plan().problem() + ")");
		}
		ItemStack knotItem = result(menu).copy();
		Optional<RuneDef> knot = RuneItem.runeOf(knotItem);
		if (knot.isEmpty() || !Knots.isKnot(knot.get()) || !knotItem.is(WildercordItems.KNOT)) {
			failures.add("tying should make a Knot (made " + knotItem + ")");
			return failures;
		}
		if (!Knots.contents(knot.get()).equals(List.of(Runes.BOLT, Runes.FIRE, Runes.AMPLIFY))) {
			failures.add("the Knot should hold Bolt, Fire and Amplify (holds " + Knots.contents(knot.get()) + ")");
		}
		// The first Knot also earns its advancement, whose experience can lift the player back up a level.
		if (Knots.xpCost(Knots.contents(knot.get())) != 3 || player.experienceLevel < levels - 3 || player.experienceLevel > levels - 2) {
			failures.add("a Knot of 3 runes should cost 3 levels (paid " + (levels - player.experienceLevel) + ")");
		}
		if (!Heart.discovered(player, "feat:" + Feats.KNOT)) {
			failures.add("the first Knot should earn its feat");
		}
		// A Knot on the altar with an effect and a shard is turned down for what it is, not taken for a silent rune.
		menu.getSlot(FusionAltarMenu.RESULT).set(ItemStack.EMPTY);
		load(menu, new ItemStack(Items.AMETHYST_SHARD), knotItem, RuneItem.stack(Runes.FIRE));
		String refused = menu.plan().problem();
		if (refused == null || refused.contains("add-on")) {
			failures.add("a Knot and an effect shouldn't fuse, and not because of a missing add-on (says \"" + refused + "\")");
		}
		load(menu, ItemStack.EMPTY);
		double loose = SpellCompiler.compile(List.of(Runes.BOLT, Runes.FIRE, Runes.AMPLIFY)).cost();
		double tied = SpellCompiler.compile(List.of(knot.get())).cost();
		if (Math.abs(tied - loose * 0.9) > 1e-6) {
			failures.add("a Knot should cost 10% less than its runes (" + tied + " vs " + loose + ")");
		}

		// ---- Anyone can learn it, even without the runes inside; it fires from one socket; the Cord's tier still counts.
		Spellbooks.set(player, Spellbooks.get(player).withSpell(1, List.of(knot.get().id())));
		if (!SpellCaster.activeRunes(Spellbooks.get(player), 1, CordTier.AMETHYST).isEmpty()) {
			failures.add("a Knot not learned yet shouldn't fire");
		}
		Spellbook stripped = new Spellbook(List.of(), Spellbooks.get(player).spells(), 0, true);
		Spellbooks.set(player, stripped);
		learn(player, knotItem);
		if (!Spellbooks.knows(player, knot.get().id())) {
			failures.add("anyone should be able to learn a Knot, even knowing none of its runes");
		}
		if (SpellCaster.activeRunes(Spellbooks.get(player), 1, CordTier.AMETHYST).size() != 1) {
			failures.add("a learned Knot should fire from its one socket on a Cord that holds its runes");
		}
		Spellbook first = Spellbooks.get(player).withSpell(0, List.of(knot.get().id()));
		if (!SpellCaster.activeRunes(first, 0, CordTier.TWINE).isEmpty()) {
			failures.add("a Twine Cord (Tier I) shouldn't fire a Knot holding Tier II Fire");
		}
		if (SpellCaster.activeRunes(first, 0, CordTier.COPPER).size() != 1) {
			failures.add("a Copper Cord (Tier II) should fire a Knot of Tier I and II runes");
		}
		long now = player.level().getGameTime();
		Spellbooks.setCord(player, new ItemStack(WildercordItems.AMETHYST_CORD));
		Spellbooks.setReadyAt(player, 1, 0);
		Spellbooks.setMana(player, 500);
		SpellCaster.cast(player, 1);
		if (Spellbooks.readyAt(player, 1) <= now) {
			failures.add("a spell of one Knot should cast");
		}

		// ---- Every fused effect at a husk: nothing may throw.
		net.minecraft.world.phys.Vec3 home = player.position();
		for (RuneDef fused : Runes.FUSED) {
			try {
				Mob husk = husk(player, 3);
				SpellCompiler.Compiled compiled = SpellCompiler.compile(List.of(Runes.TOUCH, fused));
				Effects.apply(new Cast(player), compiled.root().groups.getFirst().effects.getFirst(),
					new Cast.Hit(List.<Entity>of(husk), husk.position(), player.getLookAngle(), player.position(), null, null, false));
				// On Self too, bar the ones written on the ground (a Skyglyph under the player would launch them away).
				if (fused.kind() != dev.wildercord.spell.EffectKind.WORLD) {
					SpellCompiler.Compiled self = SpellCompiler.compile(List.of(Runes.SELF, fused));
					Effects.apply(new Cast(player), self.root().groups.getFirst().effects.getFirst(),
						new Cast.Hit(List.<Entity>of(player), player.position(), player.getLookAngle(), player.position(), null, null, true));
				}
			} catch (RuntimeException e) {
				failures.add(fused.name() + " threw " + e);
			}
		}
		player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(20), m -> m.entityTags().contains("wildercord.fusion"))
			.forEach(Mob::discard);
		// Put the player back as they were: the fused effects on Self may have moved, slowed or sealed them.
		player.removeAllEffects();
		player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		player.teleportTo(home.x, home.y, home.z);
		player.setHealth(player.getMaxHealth());
		return failures;
	}
}
