package dev.wildercord.monster;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import java.util.Set;
import java.util.UUID;
/** Books tame a coffer mimic through the real use key; tamed, it follows, opens as a chest for its owner alone, keeps what it holds and spills it when it dies. */
public final class CofferMimicTamingTest implements FabricClientGameTest {
	private static UUID id;
	/** Where it stood when it was killed. */
	private static AABB fell;
	private static void check(boolean x, String why) { if (!x) throw new AssertionError(why); }
	private static ServerPlayer p(MinecraftServer s) { return s.getPlayerList().getPlayers().getFirst(); }
	private static CofferMimic mimic(MinecraftServer s) { return (CofferMimic) s.overworld().getEntity(id); }
	/** Stands the player at (x, z) looking south at a mimic three blocks off. */
	private static void stand(MinecraftServer s, double x, double z) {
		p(s).teleportTo(s.overworld(), x, 101, z, Set.<Relative>of(), 0, 21, false);
		p(s).setDeltaMovement(0, 0, 0);
	}
	private static void hold(MinecraftServer s, ItemStack stack) { p(s).setItemInHand(InteractionHand.MAIN_HAND, stack); }
	private static void use(ClientGameTestContext c) { c.getInput().pressKey(o -> o.keyUse); c.waitTicks(4); }
	private static void shot(ClientGameTestContext c, String name) {
		c.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
		c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}
	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runCommand("time set day");
			w.getServer().runOnServer(s -> {
				var l = s.overworld();
				for (int x = -6; x <= 6; x++) for (int z = -4; z <= 40; z++) {
					l.setBlock(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState(), 2);
					for (int y = 101; y <= 104; y++) l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
				}
				var m = MonsterContent.COFFER_MIMIC.create(l, EntitySpawnReason.COMMAND);
				m.setPos(0.5, 101, 3.5);
				m.settleHere(180);
				l.addFreshEntity(m);
				id = m.getUUID();
				p(s).setGameMode(GameType.SURVIVAL);
				hold(s, new ItemStack(Items.BOOK, 64));
				stand(s, 0.5, 0.5);
			});
			c.waitTicks(10);
			check(c.computeOnClient(mc -> mc.crosshairPickEntity != null && mc.crosshairPickEntity.getUUID().equals(id)), "The crosshair is on the mimic");

			// Books, through the real use key, until one tames it (one in three each).
			int fed = 0;
			while (fed < 40 &&!w.getServer().computeOnServer(s -> mimic(s).tamed())) {
				use(c);
				fed++;
			}
			final int books = fed;
			w.getServer().runOnServer(s -> {
				var m = mimic(s);
				check(m.tamed(), "Books tame it sooner or later");
				check(m.isOwnedBy(p(s)), "It takes to whoever fed it");
				check(m.disguised(), "Fed shut, it stays shut");
				check(m.getTarget() == null, "Tamed, it is after no one");
				check(!m.canAttack(p(s)), "It never bites its owner");
				check(p(s).getMainHandItem().getCount() == 64 - books, "Each book fed is eaten, tamed or not (" + books + " fed)");
				check(m.getHealth() >= m.getMaxHealth(), "Fed whole, it is at full health");
			});
			// At full health its owner's book is kept and the chest opens, so books can be stored in it.
			use(c);
			w.getServer().runOnServer(s -> check(p(s).getMainHandItem().getCount() == 64 - books, "At full health, its owner's book is not eaten"));
			check(c.computeOnClient(mc -> mc.gui.screen() instanceof ContainerScreen), "At full health, a book opens it as a chest");
			c.runOnClient(mc -> mc.player.closeContainer());
			c.waitTicks(3);

			// Hurt, a plain book mends it; an enchanted book is never eaten and opens it instead.
			w.getServer().runOnServer(s -> {
				mimic(s).setHealth(mimic(s).getMaxHealth() - 8.0F);
				hold(s, new ItemStack(Items.ENCHANTED_BOOK));
			});
			c.waitTicks(2);
			use(c);
			w.getServer().runOnServer(s -> {
				check(p(s).getMainHandItem().is(Items.ENCHANTED_BOOK), "An enchanted book is never eaten");
				check(mimic(s).getHealth() <= mimic(s).getMaxHealth() - 8.0F, "Nor does it mend it");
			});
			check(c.computeOnClient(mc -> mc.gui.screen() instanceof ContainerScreen), "An enchanted book opens it as a chest");
			c.runOnClient(mc -> mc.player.closeContainer());
			c.waitTicks(3);
			w.getServer().runOnServer(s -> hold(s, new ItemStack(Items.BOOK, 2)));
			c.waitTicks(2);
			use(c);
			w.getServer().runOnServer(s -> {
				check(p(s).getMainHandItem().getCount() == 1, "Hurt, its owner's plain book is eaten");
				check(mimic(s).getHealth() > mimic(s).getMaxHealth() - 8.0F, "And mends it");
			});
			check(c.computeOnClient(mc -> !(mc.gui.screen() instanceof ContainerScreen)), "Mending does not open the chest");

			// Its owner opens it as a chest.
			w.getServer().runOnServer(s -> hold(s, ItemStack.EMPTY));
			c.waitTicks(2);
			use(c);
			check(c.computeOnClient(mc -> mc.gui.screen() instanceof ContainerScreen), "Its owner opens it as a chest");
			w.getServer().runOnServer(s -> {
				check(p(s).containerMenu instanceof ChestMenu chest && chest.getRowCount() == MimicRules.STORAGE_ROWS, "A three-row chest");
				p(s).containerMenu.getSlot(0).set(new ItemStack(Items.DIAMOND, 3));
				p(s).containerMenu.getSlot(26).set(new ItemStack(Items.GOLD_INGOT, 5));
			});
			c.waitTicks(3);
			shot(c, "coffer_mimic_tamed_chest");
			c.runOnClient(mc -> mc.player.closeContainer());
			c.waitTicks(3);
			w.getServer().runOnServer(s -> {
				var m = mimic(s);
				check(m.belly().getItem(0).is(Items.DIAMOND) && m.belly().getItem(0).getCount() == 3, "What is put in it stays in it");
				check(m.belly().getItem(26).is(Items.GOLD_INGOT), "All 27 slots hold");
			});

			// Sneaking: an empty hand pets it, anything else tells it to stay or follow.
			c.getInput().holdKey(o -> o.keyShift);
			c.waitTicks(3);
			use(c);
			check(c.computeOnClient(mc -> mc.gui.screen() == null), "Sneaking with an empty hand pets it rather than opening it");
			w.getServer().runOnServer(s -> {
				check(!mimic(s).staying(), "Petting gives no order");
				hold(s, new ItemStack(Items.STICK));
			});
			c.waitTicks(2);
			use(c);
			w.getServer().runOnServer(s -> check(mimic(s).staying(), "Sneaking with something in hand tells it to stay"));
			use(c);
			w.getServer().runOnServer(s -> check(!mimic(s).staying(), "Again, and it follows"));
			c.getInput().releaseKey(o -> o.keyShift);

			// Someone else gets nothing from it.
			w.getServer().runOnServer(s -> {
				var m = mimic(s);
				var stranger = FakePlayer.get(s.overworld());
				stranger.setPos(0.5, 101, 1.5);
				var book = new ItemStack(Items.BOOK, 4);
				stranger.setItemInHand(InteractionHand.MAIN_HAND, book);
				m.mobInteract(stranger, InteractionHand.MAIN_HAND);
				check(book.getCount() == 4, "It won't eat someone else's book");
				check(!m.belly().stillValid(stranger), "Its chest is shut to anyone but its owner");
				check(m.isOwnedBy(p(s)), "A stranger's book never steals it");
				stranger.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			});

			// It follows its owner, settles beside them, and turns up when left far behind.
			w.getServer().runOnServer(s -> stand(s, 0.5, 14.5));
			c.waitTicks(100);
			w.getServer().runOnServer(s -> {
				var m = mimic(s);
				check(m.distanceTo(p(s)) < MimicRules.FOLLOW_START, "It walks after its owner (" + m.distanceTo(p(s)) + " away)");
			});
			c.waitTicks(MimicRules.SETTLE_BESIDE + 20);
			w.getServer().runOnServer(s -> {
				var m = mimic(s);
				check(m.disguised(), "Left beside its owner, it shuts up as a coffer");
				// It settled behind the player, who faces south; turn round to look at it for the shot.
				float yaw = (float) Math.toDegrees(Math.atan2(m.getZ() - p(s).getZ(), m.getX() - p(s).getX())) - 90F;
				p(s).teleportTo(s.overworld(), p(s).getX(), p(s).getY(), p(s).getZ(), Set.<Relative>of(), yaw, 35, false);
			});
			c.waitTicks(3);
			shot(c, "coffer_mimic_tamed_settled");
			w.getServer().runOnServer(s -> stand(s, 0.5, 38.5));
			c.waitTicks(60);
			w.getServer().runOnServer(s -> check(mimic(s).distanceTo(p(s)) < 5, "Left far behind, it turns up beside its owner (" + mimic(s).distanceTo(p(s)) + " away)"));

			// It keeps its owner, its orders and its belly through a save.
			w.getServer().runOnServer(s -> {
				var m = mimic(s);
				var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, s.registryAccess());
				m.saveWithoutId(saved);
				var restored = MonsterContent.COFFER_MIMIC.create(s.overworld(), EntitySpawnReason.LOAD);
				restored.load(TagValueInput.create(ProblemReporter.DISCARDING, s.registryAccess(), saved.buildResult()));
				check(restored.isOwnedBy(p(s)), "Its owner survives a save");
				check(restored.belly().getItem(0).getCount() == 3 && restored.belly().getItem(26).getCount() == 5, "Its belly survives a save");
				restored.discard();

				// Killed, it spills what it holds (its owner steps away first, so they don't pick it straight up).
				stand(s, 0.5, 18.5);
				fell = m.getBoundingBox();
				m.kill(s.overworld());
			});
			c.waitTicks(5);
			w.getServer().runOnServer(s -> {
				var near = s.overworld().getEntitiesOfClass(ItemEntity.class, fell.inflate(6));
				check(near.stream().anyMatch(e -> e.getItem().is(Items.DIAMOND) && e.getItem().getCount() == 3), "Its diamonds spill when it dies");
				check(near.stream().anyMatch(e -> e.getItem().is(Items.GOLD_INGOT)), "Its gold spills when it dies");
			});
		}
	}
}
