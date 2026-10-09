package dev.wildercord.cast;

import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.HearthRules;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Set;

/** The hearth pack (cast/HearthEffects) through real paid Self casts: each rune's asserted outcome on a survival player. */
public final class HearthRunesPlayableTest implements FabricClientGameTest {
	private Zombie fresh, named;
	private int pickWear;
	private double step, reach;

	@Override
	public void runTest(ClientGameTestContext c) {
		try (var world = c.worldBuilder().create()) {
			c.waitTicks(40);
			var server = world.getServer();
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("fill -16 100 -16 16 100 16 polished_deepslate");
			server.runCommand("fill -16 101 -16 16 106 16 air");
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				p.setGameMode(GameType.SURVIVAL);
				p.teleportTo(s.overworld(), .5, 101, .5, Set.<Relative>of(), 0, 0, false);
				Spellbooks.setCord(p, new ItemStack(WildercordItems.ECHO_CORD));
				var book = Spellbooks.get(p).withStarterGiven();
				for (RuneDef rune : Runes.all()) {
					book = book.learn(rune.id());
				}
				Spellbooks.set(p, book);
				p.getInventory().clearContent();
				HearthEffects.reset(p);
				step = p.getAttributeValue(Attributes.STEP_HEIGHT);
				reach = p.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
			});
			c.waitTicks(5);

			// 1-4: timed self gifts, the attribute ones and the potion ones.
			cast(c, world, Runes.SLOWBURN);
			cast(c, world, Runes.SUREFOOT);
			cast(c, world, Runes.LONG_ARM);
			cast(c, world, Runes.LUCKCHARM);
			cast(c, world, Runes.CURRENTKIN);
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				check(HearthEffects.active(p, "slowburn"), "Slowburn runs after a paid cast");
				check(p.getAttributeValue(Attributes.STEP_HEIGHT) >= step + 0.4, "Surefoot raises step height");
				check(Math.abs(p.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) - reach - 2) < 1e-6, "Long Arm adds exactly 2 reach");
				check(p.hasEffect(MobEffects.LUCK), "Luckcharm gives Luck");
				check(p.hasEffect(MobEffects.DOLPHINS_GRACE), "Currentkin gives Dolphin's Grace");
			});
			// A recast of Long Arm never stacks its reach.
			cast(c, world, Runes.LONG_ARM);
			server.runOnServer(s -> check(Math.abs(player(s).getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) - reach - 2) < 1e-6,
				"Long Arm recast refreshes, never stacks"));

			// 5: Warm Cloak thaws.
			server.runOnServer(s -> player(s).setTicksFrozen(120));
			cast(c, world, Runes.WARM_CLOAK);
			server.runOnServer(s -> check(player(s).getTicksFrozen() == 0, "Warm Cloak clears freezing"));

			// 6: Lantern Soul lights the head's block.
			cast(c, world, Runes.LANTERN_SOUL);
			c.waitTicks(3);
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				check(p.level().getBlockState(BlockPos.containing(p.getEyePosition())).is(Blocks.LIGHT), "Lantern Soul places a light at the head");
			});

			// 7: Waymark keeps at most three.
			for (int i = 0; i < 4; i++) {
				cast(c, world, Runes.WAYMARK);
			}
			server.runOnServer(s -> check(HearthEffects.waymarks(player(s)) == 3, "Waymark keeps the newest three"));

			// 8: Hearthpath points home; Gravefinder has no death to point to.
			cast(c, world, Runes.GRAVEFINDER);
			server.runOnServer(s -> check(!HearthEffects.pointing(player(s)), "Gravefinder without a death points nowhere"));
			cast(c, world, Runes.HEARTHPATH);
			server.runOnServer(s -> check(HearthEffects.pointing(player(s)), "Hearthpath points to spawn"));

			// 9: Tarry slows a good effect's run-down; 10: Clot hastens poison's.
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				p.addEffect(new MobEffectInstance(MobEffects.SPEED, 1200, 0));
				p.addEffect(new MobEffectInstance(MobEffects.POISON, 1200, 0));
			});
			cast(c, world, Runes.TARRY);
			cast(c, world, Runes.CLOT);
			int[] at = new int[2];
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				at[0] = p.getEffect(MobEffects.SPEED).getDuration();
				at[1] = p.getEffect(MobEffects.POISON).getDuration();
			});
			c.waitTicks(100);
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				int speedLost = at[0] - p.getEffect(MobEffects.SPEED).getDuration();
				MobEffectInstance poison = p.getEffect(MobEffects.POISON);
				int poisonLost = poison == null ? at[1] : at[1] - poison.getDuration();
				check(speedLost <= 70, "Tarry slows Speed's run-down (lost " + speedLost + " in 100 ticks)");
				check(poisonLost >= 160, "Clot hastens Poison's run-down (lost " + poisonLost + " in 100 ticks)");
				p.removeEffect(MobEffects.POISON);
				p.setHealth(p.getMaxHealth());
			});

			// 11: Quench puts you out.
			server.runOnServer(s -> player(s).setRemainingFireTicks(200));
			cast(c, world, Runes.QUENCH);
			server.runOnServer(s -> check(player(s).getRemainingFireTicks() <= 0, "Quench puts the fire out"));

			// 12: Tinker's Hum mends a worn tool in hand.
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
				pick.setDamageValue(100);
				p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pick);
				pickWear = 100;
			});
			cast(c, world, Runes.TINKER_HUM);
			c.waitTicks(HearthRules.TINKER_EVERY + 10);
			server.runOnServer(s -> check(player(s).getMainHandItem().getDamageValue() < pickWear, "Tinker's Hum mends the held pickaxe"));

			// 13: Camp Ward takes a fresh monster, never a named one.
			cast(c, world, Runes.CAMP_WARD);
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				check(HearthEffects.anchored(p, "camp_ward"), "Camp Ward stands where cast");
				fresh = EntityTypes.ZOMBIE.create(s.overworld(), EntitySpawnReason.NATURAL);
				named = EntityTypes.ZOMBIE.create(s.overworld(), EntitySpawnReason.NATURAL);
				check(fresh != null && named != null, "Zombie fixtures");
				named.setCustomName(Component.literal("Kept"));
				for (Zombie z : List.of(fresh, named)) {
					z.setNoAi(true);
					z.setPos(4.5, 101, 4.5);
					s.overworld().addFreshEntity(z);
				}
			});
			c.waitTicks(25);
			server.runOnServer(s -> {
				check(fresh.isRemoved(), "Camp Ward fades a fresh spawn inside it");
				check(named.isAlive(), "Camp Ward leaves a named monster");
				named.discard();
			});

			// 14: Lodestar and Homeward.
			cast(c, world, Runes.LODESTAR);
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				GlobalPos home = p.getAttached(HearthEffects.LODESTAR);
				check(home != null && home.pos().equals(BlockPos.containing(.5, 101, .5)), "Lodestar is set at the feet");
				p.teleportTo(s.overworld(), 10.5, 101, .5, Set.<Relative>of(), 0, 0, false);
			});
			c.waitTicks(5);
			cast(c, world, Runes.HOMEWARD);
			server.runOnServer(s -> check(HearthEffects.channelling(player(s)), "Homeward channels"));
			c.waitTicks(HearthRules.HOMEWARD_CHANNEL + 10);
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				check(!HearthEffects.channelling(p), "Homeward's channel ended");
				check(p.blockPosition().equals(BlockPos.containing(.5, 101, .5)), "Homeward returns to the lodestar, at " + p.blockPosition());
			});
			// The rest refuses a second jump right away.
			server.runOnServer(s -> player(s).teleportTo(s.overworld(), 10.5, 101, .5, Set.<Relative>of(), 0, 0, false));
			c.waitTicks(5);
			cast(c, world, Runes.HOMEWARD);
			server.runOnServer(s -> check(!HearthEffects.channelling(player(s)), "Homeward rests 2 minutes"));

			// 15: Hollow Pocket keeps what you put in it.
			cast(c, world, Runes.HOLLOW_POCKET);
			c.waitTicks(5);
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				check(p.containerMenu instanceof ChestMenu menu && menu.getContainer().getContainerSize() == HearthRules.POCKET_SLOTS,
					"Hollow Pocket opens a 9-slot pocket");
				ChestMenu menu = (ChestMenu) p.containerMenu;
				menu.getSlot(0).set(new ItemStack(Items.DIAMOND, 3));
				p.closeContainer();
				List<ItemStack> kept = p.getAttached(HearthEffects.POCKET);
				check(kept != null && kept.size() == HearthRules.POCKET_SLOTS && kept.get(0).is(Items.DIAMOND) && kept.get(0).getCount() == 3,
					"Hollow Pocket saves its items");
			});
			cast(c, world, Runes.HOLLOW_POCKET);
			c.waitTicks(5);
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				check(p.containerMenu instanceof ChestMenu menu && menu.getSlot(0).getItem().is(Items.DIAMOND)
					&& menu.getSlot(0).getItem().getCount() == 3, "Hollow Pocket gives back what was kept");
				p.closeContainer();
			});

			// Clean up: every hearth rune ends, the light comes down and the attributes return.
			server.runOnServer(s -> {
				ServerPlayer p = player(s);
				BlockPos head = BlockPos.containing(p.getEyePosition());
				HearthEffects.reset(p);
				check(!HearthEffects.active(p, "slowburn") && !HearthEffects.anchored(p, "camp_ward"), "Reset ends every hearth rune");
				check(Math.abs(p.getAttributeValue(Attributes.STEP_HEIGHT) - step) < 1e-6, "Surefoot's step height is taken back");
				check(Math.abs(p.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) - reach) < 1e-6, "Long Arm's reach is taken back");
				check(!p.level().getBlockState(head).is(Blocks.LIGHT), "Lantern Soul's light comes down");
			});
		}
	}

	private static ServerPlayer player(net.minecraft.server.MinecraftServer s) {
		return s.getPlayerList().getPlayers().getFirst();
	}

	/** A real paid Self cast of one hearth rune from spell slot 0. */
	private static void cast(ClientGameTestContext c, net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext world, RuneDef rune) {
		world.getServer().runOnServer(s -> {
			ServerPlayer p = player(s);
			Component problem = SpellCaster.edit(p, 0, List.of(Runes.SELF.id(), rune.id()));
			check(problem == null, "Self " + rune.name() + " accepted: " + (problem == null ? "" : problem.getString()));
			Spellbooks.setMana(p, 200);
			Spellbooks.setReadyAt(p, 0, 0);
			float before = Spellbooks.mana(p);
			SpellCaster.cast(p, 0);
			check(Spellbooks.mana(p) < before, rune.name() + " spends mana");
		});
		c.waitTicks(10);
	}

	private static void check(boolean yes, String why) {
		if (!yes) {
			throw new AssertionError(why);
		}
	}
}
