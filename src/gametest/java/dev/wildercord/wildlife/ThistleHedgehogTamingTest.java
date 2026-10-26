package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import java.util.Set;
import java.util.UUID;
/** Sweet berries tame a thistle hedgehog through the real use key; tame, it heals on berries, sits and follows when told, keeps up with its owner, and its hoglets are theirs. */
public final class ThistleHedgehogTamingTest implements FabricClientGameTest {
	private static UUID id;
	private static void check(boolean x, String why) { if (!x) throw new AssertionError(why); }
	private static ServerPlayer p(MinecraftServer s) { return s.getPlayerList().getPlayers().getFirst(); }
	private static ThistleHedgehog hog(MinecraftServer s) { return (ThistleHedgehog) s.overworld().getEntity(id); }
	/** Stands the player at (x, z) looking south and down. */
	private static void stand(MinecraftServer s, double x, double z) {
		p(s).teleportTo(s.overworld(), x, 101, z, Set.<Relative>of(), 0, 35, false);
		p(s).setDeltaMovement(0, 0, 0);
	}
	/** Puts the hedgehog two blocks in front of a player standing at the start, under the crosshair. */
	private static void inFront(MinecraftServer s) {
		var h = hog(s);
		h.getNavigation().stop();
		h.teleportTo(0.5, 101, 2.5);
		h.setYRot(180);
		h.setDeltaMovement(0, 0, 0);
	}
	private static void hold(MinecraftServer s, ItemStack stack) { p(s).setItemInHand(InteractionHand.MAIN_HAND, stack); }
	private static void use(ClientGameTestContext c) { c.getInput().pressKey(o -> o.keyUse); c.waitTicks(4); }
	private static boolean aimed(ClientGameTestContext c) { return c.computeOnClient(mc -> mc.crosshairPickEntity != null && mc.crosshairPickEntity.getUUID().equals(id)); }
	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runCommand("time set day");
			w.getServer().runOnServer(s -> {
				var l = s.overworld();
				for (int x = -6; x <= 6; x++) for (int z = -4; z <= 40; z++) {
					l.setBlock(new BlockPos(x, 100, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
					for (int y = 101; y <= 104; y++) l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
				}
				var h = HedgehogContent.THISTLE_HEDGEHOG.create(l, EntitySpawnReason.COMMAND);
				h.setPos(0.5, 101, 2.5);
				l.addFreshEntity(h);
				id = h.getUUID();
				p(s).setGameMode(GameType.SURVIVAL);
				hold(s, new ItemStack(Items.SWEET_BERRIES, 64));
				stand(s, 0.5, 0.5);
			});
			c.waitTicks(10);
			w.getServer().runOnServer(ThistleHedgehogTamingTest::inFront);
			c.waitTicks(3);
			check(aimed(c), "The crosshair is on the hedgehog");

			// Berries, through the real use key, until a handful tames it (one in three each).
			int fed = 0;
			while (fed < 40 && !w.getServer().computeOnServer(s -> hog(s).isTame())) {
				w.getServer().runOnServer(ThistleHedgehogTamingTest::inFront);
				c.waitTicks(2);
				use(c);
				fed++;
			}
			final int berries = fed;
			w.getServer().runOnServer(s -> {
				var h = hog(s);
				check(h.isTame(), "Berries tame it sooner or later");
				check(h.isOwnedBy(p(s)), "It takes to whoever fed it");
				check(h.isPersistenceRequired(), "Tame, it never despawns");
				check(!h.curled(), "Fed, it uncurls");
				check(p(s).getMainHandItem().getCount() == 64 - berries, "Each handful fed is eaten, tamed or not (" + berries + " fed)");
				h.setHealth(4);
			});

			// Hurt, its owner's berries heal it.
			w.getServer().runOnServer(ThistleHedgehogTamingTest::inFront);
			c.waitTicks(2);
			use(c);
			w.getServer().runOnServer(s -> {
				check(hog(s).getHealth() > 4, "Berries heal a hurt tame hedgehog (" + hog(s).getHealth() + ")");
				check(p(s).getMainHandItem().getCount() == 63 - berries, "Healing eats one handful");
				hog(s).setHealth(hog(s).getMaxHealth());
				hold(s, ItemStack.EMPTY);
			});

			// Its owner tells it to sit, and to follow again.
			w.getServer().runOnServer(ThistleHedgehogTamingTest::inFront);
			c.waitTicks(2);
			use(c);
			c.waitTicks(6);
			w.getServer().runOnServer(s -> {
				check(hog(s).isOrderedToSit(), "Its owner's empty hand tells it to sit");
				check(hog(s).isInSittingPose(), "Told, it sits");
			});
			final int netId = w.getServer().computeOnServer(s -> hog(s).getId());
			check(c.computeOnClient(mc -> mc.level.getEntity(netId) instanceof ThistleHedgehog h && h.isInSittingPose()), "The client sees it sitting");
			c.waitTicks(20);
			c.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
			c.takeScreenshot(TestScreenshotOptions.of("thistle_hedgehog_tame_sitting").disableCounterPrefix());

			// A stranger gives it no orders.
			w.getServer().runOnServer(s -> {
				var stranger = FakePlayer.get(s.overworld());
				stranger.setPos(0.5, 101, 1.5);
				stranger.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				hog(s).mobInteract(stranger, InteractionHand.MAIN_HAND);
				check(hog(s).isOrderedToSit(), "A stranger can't make it get up");
				check(hog(s).isOwnedBy(p(s)), "A stranger never steals it");
			});

			// Sitting, it stays put while its owner walks off.
			w.getServer().runOnServer(s -> stand(s, 0.5, 9.5));
			c.waitTicks(40);
			w.getServer().runOnServer(s -> check(hog(s).distanceTo(p(s)) > 5, "Sitting, it stays where it was told"));

			// Told to follow, it keeps up, and turns up when left far behind.
			w.getServer().runOnServer(s -> {
				stand(s, 0.5, 0.5);
			});
			w.getServer().runOnServer(ThistleHedgehogTamingTest::inFront);
			c.waitTicks(3);
			use(c);
			w.getServer().runOnServer(s -> {
				check(!hog(s).isOrderedToSit(), "Again, and it gets up to follow");
				stand(s, 0.5, 14.5);
			});
			c.waitTicks(120);
			w.getServer().runOnServer(s -> check(hog(s).distanceTo(p(s)) < 6, "It potters after its owner (" + hog(s).distanceTo(p(s)) + " away)"));
			w.getServer().runOnServer(s -> stand(s, 0.5, 38.5));
			c.waitTicks(60);
			w.getServer().runOnServer(s -> check(hog(s).distanceTo(p(s)) < 6, "Left far behind, it turns up beside its owner (" + hog(s).distanceTo(p(s)) + " away)"));

			// Its owner and orders survive a save, and a tame pair's hoglet is the owner's too.
			w.getServer().runOnServer(s -> {
				var h = hog(s);
				h.setOrderedToSit(true);
				var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, s.registryAccess());
				h.saveWithoutId(saved);
				var restored = HedgehogContent.THISTLE_HEDGEHOG.create(s.overworld(), EntitySpawnReason.LOAD);
				restored.load(TagValueInput.create(ProblemReporter.DISCARDING, s.registryAccess(), saved.buildResult()));
				check(restored.isTame() && restored.isOwnedBy(p(s)), "Its owner survives a save");
				check(restored.isOrderedToSit(), "Its order to sit survives a save");
				restored.discard();

				var mate = HedgehogContent.THISTLE_HEDGEHOG.create(s.overworld(), EntitySpawnReason.COMMAND);
				mate.tame(p(s));
				var hoglet = h.getBreedOffspring(s.overworld(), mate);
				check(hoglet instanceof ThistleHedgehog baby && baby.isTame() && baby.isOwnedBy(p(s)), "A tame pair's hoglet belongs to their owner");
				mate.discard();
			});
		}
	}
}
