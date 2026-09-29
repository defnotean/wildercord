package dev.wildercord.gametest;

import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Sigils;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.client.FusionAltarScreen;
import dev.wildercord.content.RuneItem;
import dev.wildercord.content.WildercordBlocks;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.menu.FusionAltarMenu;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.Fusions;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * In game, the signature fusions: the Fusion Altar making a signature rune from its pair (and the same elements with
 * other runes still making their element fusion, ranks kept, the Grimoire counting it apart), then each of the
 * sixteen cast for real (threaded on a Cord and cast with {@link SpellCaster#cast}) at monsters standing on a walled
 * stone platform high in the air, or on the caster, and checked for its core effect: its damage, its heal, the mark
 * it leaves, where it moves the caster. Ends with screenshots of the altar showing a signature result
 * ({@code signature_altar}) and of a few signature runes' magic circles ({@code signature_circles}).
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordSignatureFusionTest implements FabricClientGameTest {
	/** The platform: high enough that no terrain gets in the way. */
	private static final BlockPos STAGE = new BlockPos(0, 180, 0);
	private static final String TAG = "wildercord.signature";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule natural_regeneration false");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			stage(world);
			context.waitTicks(10);
			List<String> failures = new ArrayList<>();
			check(failures, "The altar", world.getServer().computeOnServer(server -> altar(player(server))));
			altarScreen(context, world, failures);
			cleanup(context, world);
			check(failures, "Frostwire", frostwire(context, world));
			cleanup(context, world);
			check(failures, "Seethe", seethe(context, world));
			cleanup(context, world);
			check(failures, "Bloomstep", bloomstep(context, world));
			cleanup(context, world);
			check(failures, "Skyburst", skyburst(context, world));
			cleanup(context, world);
			check(failures, "Stitchtime", stitchtime(context, world));
			cleanup(context, world);
			check(failures, "Parasite", parasite(context, world));
			cleanup(context, world);
			check(failures, "Razorgale", razorgale(context, world));
			cleanup(context, world);
			check(failures, "Doomclock", doomclock(context, world));
			cleanup(context, world);
			check(failures, "Thunderstep", thunderstep(context, world));
			cleanup(context, world);
			check(failures, "Halo", halo(context, world));
			cleanup(context, world);
			check(failures, "Thunderquake", thunderquake(context, world));
			cleanup(context, world);
			check(failures, "Cometfall", cometfall(context, world));
			cleanup(context, world);
			check(failures, "Riposte", riposte(context, world));
			cleanup(context, world);
			check(failures, "Dust Devil", dustDevil(context, world));
			cleanup(context, world);
			check(failures, "Malison", malison(context, world));
			cleanup(context, world);
			check(failures, "Avalanche", avalanche(context, world));
			cleanup(context, world);
			circles(context, world);
			if (!failures.isEmpty()) {
				throw new AssertionError("The signature fusions went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	private static void check(List<String> failures, String name, List<String> found) {
		found.forEach(f -> failures.add(name + ": " + f));
	}

	// ------------------------------------------------------------------ the altar

	/**
	 * Chill and Shock with a shard make Frostwire (a signature, recorded apart from the element fusions, with its
	 * advancement); Chill and Jolt still make Hail; a rank III Shock and a rank II Chill make a rank II Frostwire.
	 */
	private static List<String> altar(ServerPlayer player) {
		List<String> out = new ArrayList<>();
		player.setExperienceLevels(30);
		FusionAltarMenu menu = menu(player);
		load(menu, new ItemStack(Items.AMETHYST_SHARD), RuneItem.stack(Runes.CHILL), null, RuneItem.stack(Runes.SHOCK));
		Fusions.Plan plan = menu.plan();
		if (!plan.ready() || !plan.signature() || plan.result() != Runes.FROSTWIRE) {
			out.add("Chill and Shock should plan the signature Frostwire (" + plan.kind() + ", " + plan.result() + ", " + plan.problem() + ")");
		}
		if (!menu.clickMenuButton(player, FusionAltarMenu.BUTTON_FUSE) || !RuneItem.runeOf(result(menu)).map(r -> r.is(Runes.FROSTWIRE.id())).orElse(false)) {
			out.add("Chill and Shock should fuse into Frostwire (made " + result(menu) + ")");
		}
		List<String> grimoire = Heart.grimoire(player);
		if (!Heart.discovered(player, "fusion:frostwire") || Fusions.signaturesFound(grimoire) != 1 || Fusions.elementFusionsFound(grimoire) != 0) {
			out.add("the first Frostwire should be recorded as a signature fusion, apart from the element fusions (" + grimoire + ")");
		}
		if (!Heart.discovered(player, "feat:" + Feats.COMBINE)) {
			out.add("a signature fusion is a combine: it should earn the Fusion feat");
		}
		var advancement = player.level().getServer().getAdvancements().get(dev.wildercord.Wildercord.id("altar/signature"));
		if (advancement == null || !player.getAdvancements().getOrStartProgress(advancement).isDone()) {
			out.add("the first signature fusion should grant its advancement");
		}
		menu.getSlot(FusionAltarMenu.RESULT).set(ItemStack.EMPTY);
		// The same elements with other runes: their element fusion.
		load(menu, new ItemStack(Items.AMETHYST_SHARD), RuneItem.stack(Runes.CHILL), RuneItem.stack(Runes.JOLT));
		plan = menu.plan();
		if (plan.signature() || plan.result() != Runes.HAIL || !menu.clickMenuButton(player, FusionAltarMenu.BUTTON_FUSE)) {
			out.add("Chill and Jolt should still make Hail (" + plan.result() + ", " + plan.problem() + ")");
		}
		menu.getSlot(FusionAltarMenu.RESULT).set(ItemStack.EMPTY);
		// Ranks: the lower of the two.
		load(menu, new ItemStack(Items.AMETHYST_SHARD), RuneItem.stack(Runes.SHOCK, 3), RuneItem.stack(Runes.CHILL, 2));
		if (!menu.clickMenuButton(player, FusionAltarMenu.BUTTON_FUSE) || RuneItem.rankOf(result(menu)) != 2
				|| !RuneItem.runeOf(result(menu)).map(r -> r.is(Runes.FROSTWIRE.id())).orElse(false)) {
			out.add("a rank III Shock and a rank II Chill should make a rank II Frostwire (made " + result(menu) + ")");
		}
		menu.getSlot(FusionAltarMenu.RESULT).set(ItemStack.EMPTY);
		load(menu, ItemStack.EMPTY);
		return out;
	}

	/** The altar's screen with Chill, Shock and a shard on it: the panel names the signature fusion. Screenshot {@code signature_altar}. */
	private static void altarScreen(ClientGameTestContext context, TestSingleplayerContext world, List<String> failures) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			BlockPos pos = altarPos(player);
			player.openMenu(player.level().getBlockState(pos).getMenuProvider(player.level(), pos));
			if (player.containerMenu instanceof FusionAltarMenu menu) {
				menu.getSlot(0).set(RuneItem.stack(Runes.CHILL));
				menu.getSlot(2).set(RuneItem.stack(Runes.SHOCK));
				menu.getSlot(FusionAltarMenu.CATALYST).set(new ItemStack(Items.AMETHYST_SHARD));
				menu.broadcastChanges();
			}
		});
		context.waitTicks(30);
		boolean open = context.computeOnClient(mc -> mc.gui.screen() instanceof FusionAltarScreen screen && screen.getMenu().plan().signature());
		if (!open) {
			failures.add("The altar: its screen should open, and show Chill and Shock as a signature fusion");
		}
		context.takeScreenshot(TestScreenshotOptions.of("signature_altar").disableCounterPrefix());
		context.runOnClient(mc -> mc.player.closeContainer());
		context.waitTicks(5);
		world.getServer().runOnServer(server -> player(server).level().setBlockAndUpdate(altarPos(player(server)), Blocks.AIR.defaultBlockState()));
	}

	/** An altar two blocks behind the player (out of the way of the husks in front). */
	private static BlockPos altarPos(ServerPlayer player) {
		return new BlockPos(STAGE.getX(), STAGE.getY(), STAGE.getZ() - 2);
	}

	private static FusionAltarMenu menu(ServerPlayer player) {
		BlockPos pos = altarPos(player);
		if (!player.level().getBlockState(pos).is(WildercordBlocks.FUSION_ALTAR)) {
			player.level().setBlockAndUpdate(pos, WildercordBlocks.FUSION_ALTAR.defaultBlockState());
		}
		return new FusionAltarMenu(0, player.getInventory(), ContainerLevelAccess.create(player.level(), pos));
	}

	private static void load(FusionAltarMenu menu, ItemStack catalyst, ItemStack... runes) {
		for (int i = 0; i < FusionAltarMenu.RUNE_SLOTS; i++) {
			menu.getSlot(i).set(i < runes.length && runes[i] != null ? runes[i].copy() : ItemStack.EMPTY);
		}
		menu.getSlot(FusionAltarMenu.CATALYST).set(catalyst.copy());
	}

	private static ItemStack result(FusionAltarMenu menu) {
		return menu.getSlot(FusionAltarMenu.RESULT).getItem();
	}

	// ------------------------------------------------------------------ the runes

	/** Frostwire at a husk: it's chilled and shocked, a slowed husk near it is shocked too, one that isn't cold is left alone. */
	private static List<String> frostwire(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = on(world, player -> {
			int cold = husk(player, 2.5, 5);
			mob(player, cold).addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 400, 0));
			return new int[] {husk(player, 0, 5), cold, husk(player, -3, 5)};
		});
		String cast = on(world, player -> cast(player, Runes.BEAM, Runes.FROSTWIRE));
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(14);
		String result = on(world, player -> {
			Mob target = mob(player, ids[0]);
			Mob cold = mob(player, ids[1]);
			Mob warm = mob(player, ids[2]);
			if (target == null || cold == null || warm == null) {
				return "the husks should still be there";
			}
			if (!has(target, MobEffects.SLOWNESS, 1)) {
				return "the husk hit should be chilled (Slowness II)";
			}
			if (!between(taken(target), 3, 6.5)) {
				return "the husk hit should be shocked for about 4 (took " + taken(target) + ")";
			}
			if (!between(taken(cold), 3, 6.5)) {
				return "a slowed husk near it should be shocked by the current for about 4 (took " + taken(cold) + ")";
			}
			return taken(warm) == 0 ? null : "a husk that isn't cold should be left alone (took " + taken(warm) + ")";
		});
		return found(result);
	}

	/** Seethe at a husk: held in its bubble, scalded, then the steam bursts on it and on its neighbour (blinded, soaked); one further off is spared. */
	private static List<String> seethe(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = on(world, player -> new int[] {husk(player, 0, 5), husk(player, 1.5, 5.5), husk(player, -5, 5)});
		String cast = on(world, player -> cast(player, Runes.BEAM, Runes.SEETHE));
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(5);
		String held = on(world, player -> {
			Mob target = mob(player, ids[0]);
			return target != null && has(target, MobEffects.SLOWNESS, 6) ? null : "the husk hit should be held in the bubble";
		});
		context.waitTicks(42);
		String burst = on(world, player -> {
			Mob target = mob(player, ids[0]);
			Mob near = mob(player, ids[1]);
			Mob far = mob(player, ids[2]);
			if (target == null || near == null || far == null) {
				return "the husks should still be there";
			}
			if (!between(taken(target), 5, 11)) {
				return "the husk in the bubble should be scalded 4 times and caught in the burst, about 8 (took " + taken(target) + ")";
			}
			if (!between(taken(near), 2.5, 6) || !near.hasEffect(MobEffects.BLINDNESS) || !Reactions.has(near, Reactions.Mark.SOAKED)) {
				return "a husk beside the bubble should be scalded by its steam for about 4, blinded and soaked (took " + taken(near) + ")";
			}
			return taken(far) == 0 ? null : "a husk 5 blocks off should be left alone (took " + taken(far) + ")";
		});
		return found(held, burst);
	}

	/** Bloomstep at the grass ahead: the caster steps onto it, grass and flowers grow there, and they get Regeneration. */
	private static List<String> bloomstep(ClientGameTestContext context, TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 3) + " " + (y - 1) + " " + (z + 2) + " " + (x + 3) + " " + (y - 1) + " " + (z + 8) + " minecraft:grass_block");
		context.waitTicks(2);
		String cast = on(world, player -> {
			// Looking down at the grass, 4 or 5 blocks ahead.
			player.teleportTo(player.level(), x + 0.5, y, z + 0.5, Set.<Relative>of(), 0.0F, 20.0F, false);
			return cast(player, Runes.BEAM, Runes.BLOOMSTEP);
		});
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(4);
		String result = on(world, player -> {
			double moved = player.position().subtract(x + 0.5, y, z + 0.5).horizontalDistance();
			if (moved < 3) {
				return "the caster should step to where the spell landed (moved " + moved + ")";
			}
			if (!player.hasEffect(MobEffects.REGENERATION)) {
				return "the caster should arrive with Regeneration";
			}
			int grown = 0;
			for (BlockPos p : BlockPos.betweenClosed(player.blockPosition().offset(-3, 0, -3), player.blockPosition().offset(3, 1, 3))) {
				if (!player.level().getBlockState(p).isAir()) {
					grown++;
				}
			}
			return grown > 0 ? null : "grass and flowers should spring up where the caster arrives";
		});
		world.getServer().runCommand("fill " + (x - 3) + " " + (y - 1) + " " + (z + 2) + " " + (x + 3) + " " + (y - 1) + " " + (z + 8) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 3) + " " + y + " " + (z + 2) + " " + (x + 3) + " " + (y + 1) + " " + (z + 8) + " minecraft:air");
		return found(result);
	}

	/** Skyburst at a husk: flung up, then its blast rains fire on it and the husk beneath beside it. */
	private static List<String> skyburst(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = on(world, player -> new int[] {husk(player, 0, 5), husk(player, 1.5, 5)});
		String cast = on(world, player -> cast(player, Runes.BEAM, Runes.SKYBURST));
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(4);
		String flung = on(world, player -> {
			Mob target = mob(player, ids[0]);
			return target != null && target.getY() > STAGE.getY() + 0.5 ? null : "the husk hit should be flung up";
		});
		context.waitTicks(22);
		String blast = on(world, player -> {
			Mob target = mob(player, ids[0]);
			Mob beneath = mob(player, ids[1]);
			if (target == null || beneath == null) {
				return "the husks should still be there";
			}
			if (taken(target) < 4) {
				return "the flung husk should be caught in its own blast (took " + taken(target) + ")";
			}
			return taken(beneath) >= 2 ? null : "the husk beneath it should be caught in the fire it rains down (took " + taken(beneath) + ")";
		});
		return found(flung, blast);
	}

	/** Stitchtime on yourself: 4 back at once, then every wound of the next 4 seconds healed back when they're up. */
	private static List<String> stitchtime(ClientGameTestContext context, TestSingleplayerContext world) {
		int husk = on(world, player -> husk(player, 3, 3));
		float[] healed = new float[1];
		String cast = on(world, player -> {
			player.setHealth(8.0F);
			String c = cast(player, Runes.SELF, Runes.STITCHTIME);
			healed[0] = player.getHealth();
			return c;
		});
		if (cast != null) {
			return List.of(cast);
		}
		if (healed[0] < 11.5F) {
			return List.of("it should heal 4 at once (at " + healed[0] + " from 8)");
		}
		for (int i = 0; i < 3; i++) {
			on(world, player -> {
				Effects.readyToHurt(player);
				player.hurtServer(player.level(), player.level().damageSources().mobAttack(mob(player, husk)), 2.0F);
				return null;
			});
			context.waitTicks(5);
		}
		String wounded = on(world, player -> player.getHealth() < healed[0] - 2 ? null : "the blows should land meanwhile (at " + player.getHealth() + ")");
		context.waitTicks(80);
		String back = on(world, player -> Math.abs(player.getHealth() - healed[0]) < 0.6F ? null
			: "when its time is up it should heal back every wound it counted (at " + player.getHealth() + ", " + healed[0] + " before the blows)");
		return found(wounded, back);
	}

	/** Parasite at a vindicator: poisoned and drained into the caster; when it dies, it leaps to the vindicator beside it. */
	private static List<String> parasite(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = on(world, player -> new int[] {mob(player, EntityTypes.VINDICATOR, 0, 5), mob(player, EntityTypes.VINDICATOR, 3, 5)});
		String cast = on(world, player -> {
			player.setHealth(12.0F);
			return cast(player, Runes.BEAM, Runes.PARASITE);
		});
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(45);
		String drained = on(world, player -> {
			Mob host = mob(player, ids[0]);
			if (host == null || !host.hasEffect(MobEffects.POISON)) {
				return "its host should be poisoned";
			}
			if (taken(host) < 2) {
				return "its host should be drained each second (took " + taken(host) + ")";
			}
			return player.getHealth() > 12.5F ? null : "the caster should be fed what it drains (at " + player.getHealth() + " from 12)";
		});
		on(world, player -> {
			Mob host = mob(player, ids[0]);
			if (host != null) {
				host.kill(player.level());
			}
			return null;
		});
		context.waitTicks(30);
		String leapt = on(world, player -> {
			Mob next = mob(player, ids[1]);
			return next != null && next.hasEffect(MobEffects.POISON) ? null : "when its host dies, it should leap to the enemy beside it";
		});
		return found(drained, leapt);
	}

	/** Razorgale at a husk: cut and left bleeding, then the gale tears the wound (Rupture); its neighbour too, one further off spared. */
	private static List<String> razorgale(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = on(world, player -> new int[] {husk(player, 0, 5), husk(player, 2, 5.5), husk(player, -5, 5)});
		String cast = on(world, player -> cast(player, Runes.BEAM, Runes.RAZORGALE));
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(3);
		String cut = on(world, player -> {
			Mob target = mob(player, ids[0]);
			return target != null && Reactions.has(target, Reactions.Mark.BLEEDING) && between(taken(target), 1.5, 3)
				? null : "the first pass should cut for 2 and leave it bleeding (took " + (target == null ? -1 : taken(target)) + ")";
		});
		context.waitTicks(15);
		String torn = on(world, player -> {
			Mob target = mob(player, ids[0]);
			Mob near = mob(player, ids[1]);
			Mob far = mob(player, ids[2]);
			if (target == null || near == null || far == null) {
				return "the husks should still be there";
			}
			if (!between(taken(target), 7, 12.5)) {
				return "the gale coming back round should tear the wound open: about 2, then 3 and 4 (took " + taken(target) + ")";
			}
			if (Reactions.has(target, Reactions.Mark.BLEEDING)) {
				return "tearing the wound open uses the bleeding up";
			}
			if (taken(near) < 7) {
				return "a husk 2 blocks off should be cut and torn too (took " + taken(near) + ")";
			}
			return taken(far) == 0 ? null : "a husk 5 blocks off should be left alone (took " + taken(far) + ")";
		});
		return found(cut, torn);
	}

	/** Doomclock at a husk: two blows wind it 4 tighter, and at zero it bursts for 12 on it and the husk beside it. */
	private static List<String> doomclock(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = on(world, player -> new int[] {husk(player, 0, 5), husk(player, 2, 5)});
		String cast = on(world, player -> cast(player, Runes.BEAM, Runes.DOOMCLOCK));
		if (cast != null) {
			return List.of(cast);
		}
		for (int i = 0; i < 2; i++) {
			context.waitTicks(8);
			on(world, player -> {
				Mob target = mob(player, ids[0]);
				if (target != null) {
					Effects.readyToHurt(target);
					target.hurtServer(player.level(), player.level().damageSources().generic(), 1.0F);
				}
				return null;
			});
		}
		context.waitTicks(20);
		String early = on(world, player -> {
			Mob near = mob(player, ids[1]);
			return near != null && taken(near) == 0 ? null : "nothing should burst before the clock runs out";
		});
		context.waitTicks(30);
		String burst = on(world, player -> {
			Mob target = mob(player, ids[0]);
			Mob near = mob(player, ids[1]);
			if (target == null || near == null) {
				return "the husks should still be there";
			}
			if (!between(taken(near), 10, 14.5)) {
				return "the burst should be 8 and the 4 two blows wound (the husk beside it took " + taken(near) + ")";
			}
			return taken(target) >= 12 ? null : "its bearer should take the burst too (took " + taken(target) + ")";
		});
		return found(early, burst);
	}

	/** Thunderstep at a husk: the caster comes down right behind it, and the strike hurts and stuns it and the husk beside. */
	private static List<String> thunderstep(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = on(world, player -> new int[] {husk(player, 0, 6), husk(player, 1.4, 7.2)});
		String cast = on(world, player -> cast(player, Runes.BEAM, Runes.THUNDERSTEP));
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(3);
		String result = on(world, player -> {
			Mob target = mob(player, ids[0]);
			Mob near = mob(player, ids[1]);
			if (target == null || near == null) {
				return "the husks should still be there";
			}
			if (player.position().subtract(STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5).horizontalDistance() < 4 || player.distanceTo(target) > 2.6) {
				return "the caster should come down right beside the husk hit (" + player.distanceTo(target) + " from it)";
			}
			if (player.getZ() < target.getZ()) {
				return "the caster should come down behind the husk, not in front of it";
			}
			if (!between(taken(target), 6, 10.5) || !has(target, MobEffects.SLOWNESS, 6)) {
				return "the husk hit should be struck for about 8 and stunned (took " + taken(target) + ")";
			}
			return taken(near) >= 6 ? null : "the husk beside it should be struck too (took " + taken(near) + ")";
		});
		return found(result);
	}

	/** Halo on yourself: it smites the husk nearby (tripled: undead), healing you 1 each time. */
	private static List<String> halo(ClientGameTestContext context, TestSingleplayerContext world) {
		int husk = on(world, player -> husk(player, 0, 4));
		String cast = on(world, player -> {
			player.setHealth(10.0F);
			return cast(player, Runes.SELF, Runes.HALO);
		});
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(18);
		String first = on(world, player -> {
			Mob target = mob(player, husk);
			if (target == null || !between(taken(target), 7, 11)) {
				return "the halo should smite the husk for 3, tripled against the undead (took " + (target == null ? -1 : taken(target)) + ")";
			}
			return player.getHealth() >= 10.9F ? null : "the ally should heal 1 with each smite (at " + player.getHealth() + ")";
		});
		context.waitTicks(40);
		String second = on(world, player -> {
			Mob target = mob(player, husk);
			return target == null || taken(target) >= 16 ? null : "it should smite again 2 seconds later (took " + taken(target) + ")";
		});
		return found(first, second);
	}

	/** Thunderquake at a husk: all three waves reach it, two reach a husk 3.5 off, none one 7 off. */
	private static List<String> thunderquake(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = on(world, player -> new int[] {husk(player, 0, 5), husk(player, 3.5, 5), husk(player, -7, 5)});
		String cast = on(world, player -> cast(player, Runes.BEAM, Runes.THUNDERQUAKE));
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(30);
		String result = on(world, player -> {
			Mob heart = mob(player, ids[0]);
			Mob mid = mob(player, ids[1]);
			Mob far = mob(player, ids[2]);
			if (heart == null || mid == null || far == null) {
				return "the husks should still be there";
			}
			if (!between(taken(heart), 10, 14.5)) {
				return "every wave should reach the husk at the heart: 12 (took " + taken(heart) + ")";
			}
			if (!between(taken(mid), 6, 10)) {
				return "two waves should reach a husk 3.5 blocks off: 8 (took " + taken(mid) + ")";
			}
			return taken(far) == 0 ? null : "no wave should reach a husk 7 blocks off (took " + taken(far) + ")";
		});
		return found(result);
	}

	/** Cometfall at a husk: the blast burns it and its neighbour; shards strike two husks further off. */
	private static List<String> cometfall(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = on(world, player -> new int[] {husk(player, 0, 5), husk(player, 2.5, 5), husk(player, -6.5, 5), husk(player, 6.5, 5)});
		String cast = on(world, player -> cast(player, Runes.BEAM, Runes.COMETFALL));
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(10);
		String waits = on(world, player -> {
			Mob target = mob(player, ids[0]);
			return target != null && taken(target) == 0 ? null : "the comet should take a second to fall";
		});
		context.waitTicks(25);
		String landed = on(world, player -> {
			List<Mob> husks = new ArrayList<>();
			for (int id : ids) {
				Mob m = mob(player, id);
				if (m == null) {
					return "the husks should still be there";
				}
				husks.add(m);
			}
			if (!between(taken(husks.get(0)), 11, 20)) {
				return "the husk it falls on should take about 16 (took " + taken(husks.get(0)) + ")";
			}
			if (taken(husks.get(1)) < 8 || !husks.get(1).isOnFire() && taken(husks.get(1)) < 9) {
				return "a husk 2.5 blocks off should be caught and set alight (took " + taken(husks.get(1)) + ")";
			}
			if (taken(husks.get(2)) < 3 || taken(husks.get(3)) < 3) {
				return "its shards should strike the husks further off for 4 (took " + taken(husks.get(2)) + " and " + taken(husks.get(3)) + ")";
			}
			return null;
		});
		return found(waits, landed);
	}

	/** Riposte on yourself: the husk's next two blows are sidestepped and answered for 6; the third lands. */
	private static List<String> riposte(ClientGameTestContext context, TestSingleplayerContext world) {
		int husk = on(world, player -> husk(player, 0, 2));
		String cast = on(world, player -> cast(player, Runes.SELF, Runes.RIPOSTE));
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(3);
		List<String> out = new ArrayList<>();
		for (int blow = 1; blow <= 3; blow++) {
			int b = blow;
			String result = on(world, player -> {
				Mob striker = mob(player, husk);
				if (striker == null) {
					return "the husk should still be there";
				}
				float before = player.getHealth();
				float struck = taken(striker);
				Effects.readyToHurt(player);
				player.hurtServer(player.level(), player.level().damageSources().mobAttack(striker), 3.0F);
				boolean dodged = player.getHealth() >= before;
				float answered = taken(striker) - struck;
				if (b <= 2) {
					if (!dodged) {
						return "blow " + b + " should be sidestepped (" + before + " to " + player.getHealth() + ")";
					}
					return between(answered, 4.5, 7.5) ? null : "blow " + b + " should be answered for 6 (it took " + answered + ")";
				}
				return dodged ? "the third blow should land: only two are seen coming" : null;
			});
			if (result != null) {
				out.add(result);
			}
			context.waitTicks(6);
		}
		return out;
	}

	/** Dust Devil at a husk: it's caught up, blinded and scoured each second, and flung high when the devil blows out. */
	private static List<String> dustDevil(ClientGameTestContext context, TestSingleplayerContext world) {
		int husk = on(world, player -> husk(player, 0, 5));
		String cast = on(world, player -> cast(player, Runes.BEAM, Runes.DUST_DEVIL));
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(10);
		String caught = on(world, player -> {
			Mob target = mob(player, husk);
			return target != null && target.hasEffect(MobEffects.BLINDNESS) && taken(target) >= 2 ? null
				: "the husk should be caught up: blinded and scoured (took " + (target == null ? -1 : taken(target)) + ")";
		});
		context.waitTicks(85);
		String scoured = on(world, player -> {
			Mob target = mob(player, husk);
			return target != null && between(taken(target), 10, 19) ? null
				: "the husk should be scoured for 3 a second while it's caught: about 15 (took " + (target == null ? -1 : taken(target)) + ")";
		});
		double highest = 0;
		for (int i = 0; i < 20; i++) {
			context.waitTicks(1);
			double up = on(world, player -> {
				Mob target = mob(player, husk);
				return target == null ? 0.0 : target.getY() - STAGE.getY();
			});
			highest = Math.max(highest, up);
		}
		String flung = highest > 1.5 ? null : "when the devil blows out it should fling the husk high (it rose " + highest + ")";
		return found(caught, scoured, flung);
	}

	/** Malison at a husk: it's hurt and shadowed; when it dies, the curse passes on to the two husks near it. */
	private static List<String> malison(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = on(world, player -> new int[] {husk(player, 0, 5), husk(player, 2, 5), husk(player, -2, 5)});
		String cast = on(world, player -> cast(player, Runes.BEAM, Runes.MALISON));
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(3);
		String cursed = on(world, player -> {
			Mob target = mob(player, ids[0]);
			if (target == null || !between(taken(target), 2, 4.5)) {
				return "the husk hit should take 3 (took " + (target == null ? -1 : taken(target)) + ")";
			}
			if (!Reactions.has(target, Reactions.Mark.SHADOWED)) {
				return "the husk hit should be cursed (shadowed)";
			}
			Mob neighbour = mob(player, ids[1]);
			if (neighbour == null || Reactions.has(neighbour, Reactions.Mark.SHADOWED)) {
				return "its neighbours should be there, and not cursed yet";
			}
			target.kill(player.level());
			return null;
		});
		context.waitTicks(5);
		String passed = on(world, player -> {
			Mob a = mob(player, ids[1]);
			Mob b = mob(player, ids[2]);
			return a != null && b != null && Reactions.has(a, Reactions.Mark.SHADOWED) && Reactions.has(b, Reactions.Mark.SHADOWED) ? null
				: "when the cursed husk dies, the curse should pass to the husks near it";
		});
		return found(cursed, passed);
	}

	/** Avalanche at a husk: 9 on its bare head, 6 through the helmet of the one beside it, both buried; its drifts melt after 10 seconds. */
	private static List<String> avalanche(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = on(world, player -> {
			int bare = husk(player, 0, 5);
			int helmed = husk(player, 2, 5);
			mob(player, helmed).setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
			return new int[] {bare, helmed};
		});
		String cast = on(world, player -> cast(player, Runes.BEAM, Runes.AVALANCHE));
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(4);
		String fell = on(world, player -> {
			Mob bare = mob(player, ids[0]);
			Mob helmed = mob(player, ids[1]);
			if (bare == null || helmed == null) {
				return "the husks should still be there";
			}
			if (!between(taken(bare), 7.5, 11) || !has(bare, MobEffects.SLOWNESS, 2)) {
				return "the bare-headed husk should take half again, 9, and be buried (Slowness III) (took " + taken(bare) + ")";
			}
			if (taken(helmed) <= 0 || taken(helmed) >= taken(bare)) {
				return "the husk with a helmet should take less (took " + taken(helmed) + " against " + taken(bare) + ")";
			}
			return snow(player) > 0 ? null : "drifts of snow should lie where it fell";
		});
		context.waitTicks(210);
		String melted = on(world, player -> snow(player) == 0 ? null : "its drifts should melt after 10 seconds (" + snow(player) + " left)");
		return found(fell, melted);
	}

	private static int snow(ServerPlayer player) {
		int n = 0;
		BlockPos c = new BlockPos(STAGE.getX(), STAGE.getY(), STAGE.getZ() + 5);
		for (BlockPos p : BlockPos.betweenClosed(c.offset(-5, -1, -5), c.offset(5, 2, 5))) {
			if (player.level().getBlockState(p).is(Blocks.SNOW)) {
				n++;
			}
		}
		return n;
	}

	// ------------------------------------------------------------------ the circles

	/**
	 * A few signature runes' magic circles hung in the night air in front of the caster, each its runes' two elements
	 * braided with a star: Frostwire, Seethe, Halo and Cometfall alone, and Bolt with Doomclock. Screenshot
	 * {@code signature_circles}.
	 */
	private static void circles(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("time set 18000");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, -4.0F, false);
		});
		context.runOnClient(mc -> {
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
			mc.gui.toastManager().clear();
		});
		context.waitTicks(10);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			Vec3 facing = new Vec3(0, 0, -1);
			List<List<RuneDef>> spells = List.of(List.of(Runes.FROSTWIRE), List.of(Runes.SEETHE), List.of(Runes.BOLT, Runes.DOOMCLOCK), List.of(Runes.HALO),
				List.of(Runes.COMETFALL));
			for (int i = 0; i < spells.size(); i++) {
				List<RuneDef> runes = spells.get(i);
				RuneDef colour = runes.getLast();
				double x = STAGE.getX() + 0.5 + (i - 2) * 2.6;
				double y = STAGE.getY() + 1.8 + (i % 2 == 0 ? 0.6 : -0.3);
				Sigils.spell(level, new Vec3(x, y, STAGE.getZ() + 6.5), facing, runes, RuneColors.of(colour), 1.2F, 160);
			}
		});
		context.waitTicks(40);
		context.takeScreenshot(TestScreenshotOptions.of("signature_circles").disableCounterPrefix());
		context.runOnClient(mc -> {
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		world.getServer().runCommand("time set 6000");
	}

	// ------------------------------------------------------------------ helpers

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static <T> T on(TestSingleplayerContext world, Function<ServerPlayer, T> step) {
		return world.getServer().computeOnServer(server -> step.apply(player(server)));
	}

	private static List<String> found(String... results) {
		List<String> out = new ArrayList<>();
		for (String result : results) {
			if (result != null) {
				out.add(result);
			}
		}
		return out;
	}

	private static boolean between(double value, double low, double high) {
		return value >= low && value <= high;
	}

	private static float taken(LivingEntity e) {
		return e.getMaxHealth() - e.getHealth();
	}

	private static boolean has(LivingEntity e, Holder<MobEffect> effect, int amplifier) {
		MobEffectInstance instance = e == null ? null : e.getEffect(effect);
		return instance != null && instance.getAmplifier() >= amplifier;
	}

	/** A 21x21 stone floor, walled with glass so nothing is thrown off it, open air above; an Echo Cord, every rune known. */
	private static void stage(TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 11) + " " + (y - 1) + " " + (z - 11) + " " + (x + 11) + " " + (y + 3) + " " + (z + 11) + " minecraft:glass");
		world.getServer().runCommand("fill " + (x - 10) + " " + (y - 1) + " " + (z - 10) + " " + (x + 10) + " " + (y - 1) + " " + (z + 10) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 10) + " " + y + " " + (z - 10) + " " + (x + 10) + " " + (y + 8) + " " + (z + 10) + " minecraft:air");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
			Spellbook book = Spellbooks.get(player).withStarterGiven();
			for (RuneDef rune : Runes.all()) {
				book = book.learn(rune.id());
			}
			Spellbooks.set(player, book);
			player.setAttached(WildercordAttachments.CRYSTALS, Mana.MAX_CRYSTALS);
			stand(player);
		});
	}

	/** The caster at the middle of the platform, facing south (+Z) and a little down, toward the monsters. */
	private static void stand(ServerPlayer player) {
		player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 8.0F, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.setHealth(player.getMaxHealth());
		player.removeAllEffects();
		player.clearFire();
		player.setTicksFrozen(0);
	}

	/** A husk (no AI, so it stands still) {@code dx}, {@code dz} from the middle of the platform, facing the caster. */
	private static int husk(ServerPlayer player, double dx, double dz) {
		return mob(player, EntityTypes.HUSK, dx, dz);
	}

	private static int mob(ServerPlayer player, EntityType<? extends Mob> type, double dx, double dz) {
		ServerLevel level = player.level();
		Mob mob = type.create(level, EntitySpawnReason.COMMAND);
		mob.snapTo(STAGE.getX() + 0.5 + dx, STAGE.getY(), STAGE.getZ() + 0.5 + dz, 180, 0);
		mob.setNoAi(true);
		mob.addTag(TAG);
		// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
		mob.addTag("wildercord.rolled");
		level.addFreshEntity(mob);
		return mob.getId();
	}

	private static Mob mob(ServerPlayer player, int id) {
		return player.level().getEntity(id) instanceof Mob mob && mob.isAlive() ? mob : null;
	}

	/** Threads {@code shape} and {@code effect} into spell 1 and casts it at once, full mana and no cooldown. Returns what went wrong, or null. */
	private static String cast(ServerPlayer player, RuneDef shape, RuneDef effect) {
		List<String> ids = List.of(shape.id(), effect.id());
		SpellCaster.edit(player, 0, List.of());
		SpellCaster.edit(player, 0, ids);
		if (!Spellbooks.get(player).spells().getFirst().equals(ids)) {
			return "the spell didn't thread (has " + Spellbooks.get(player).spells().getFirst() + ")";
		}
		Spellbooks.setReadyAt(player, 0, 0);
		Spellbooks.setMana(player, Mana.max(player));
		player.removeAttached(WildercordAttachments.RHYTHM);
		long now = player.level().getGameTime();
		SpellCaster.cast(player, 0);
		return Spellbooks.readyAt(player, 0) > now ? null : "it didn't cast";
	}

	private static void cleanup(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		world.getServer().runOnServer(server -> stand(player(server)));
		context.waitTicks(20);
	}
}
