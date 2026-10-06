package dev.wildercord.cast;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.api.WildercordEvents;
import dev.wildercord.client.CordScreen;
import dev.wildercord.client.WildercordKeys;
import dev.wildercord.content.CordTier;
import dev.wildercord.content.RelayLessonChecks;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.MasterStudies;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.MasterStudyRules;
import dev.wildercord.spell.RelayInputRules;
import dev.wildercord.spell.RelayRules;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/** Native lesson and real rebound-key loop, followed by production admission/cancellation probes. */
public final class RelayCircleTest implements FabricClientGameTest {
	private static final Vec3 FEET = new Vec3(.5, 150, .5);
	private static boolean listening;
	private static Probe probe;
	private static long nonce = 10_000;
	private static final class Probe {
		ServerPlayer owner;
		TrainingDummy dummy;
		float beforePayment, afterPayment;
		int payments, spent;
	}

	@Override public void runTest(ClientGameTestContext context) {
		RelayLessonChecks.run(context);
		RelayInheritedChecks.run(context);
		RelayMasteryChecks.run(context);
		RelayWorldContextChecks.run(context);
		listen();
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule fall_damage false");
			world.getServer().runCommand("gamerule natural_health_regeneration false");
			world.getServer().runOnServer(server -> {
				probe = new Probe(); probe.owner = player(server);
				prepare(probe.owner, Runes.HARM);
				probe.dummy = WildercordEntities.TRAINING_DUMMY.create(probe.owner.level(), EntitySpawnReason.MOB_SUMMONED);
				check(probe.dummy != null, "Dummy creates");
				probe.dummy.snapTo(.5, 150, 6.5, 180, 0);
				probe.owner.level().addFreshEntity(probe.dummy);
			});
			context.waitTicks(10);
			InputConstants.Key[] original = new InputConstants.Key[1];
			context.runOnClient(mc -> {
				var key = WildercordKeys.castMapping(); original[0] = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(key);
				key.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_F8)); KeyMapping.resetMapping();
				check(WildercordKeys.castKey().getString().contains("F8"), "Lesson and focus help name the rebound cast key");
			});
			try {
				context.getInput().holdKey(WildercordKeys.castMapping());
				context.waitTicks(8);
				world.getServer().runOnServer(server -> {
					check(RelayCircles.pending(probe.owner), "First real key-down places the focus");
					check(probe.payments == 1 && probe.spent > 0, "Placement has exactly one accepted paid transaction");
					check(close(probe.beforePayment - probe.afterPayment, probe.spent), "All accepted mana is paid on placement");
					check(probe.dummy.hitSequence() == 0, "Placement and held repeat make no hit");
					check(!probe.owner.hasAttached(WildercordAttachments.CHARGE), "Relay cannot start the normal charge/overchannel flow");
				});
				context.takeScreenshot(TestScreenshotOptions.of("relay_focus_set_rebound_key").disableCounterPrefix());
				context.getInput().releaseKey(WildercordKeys.castMapping()); context.waitTicks(3);
				world.getServer().runOnServer(server -> {
					probe.owner.teleportTo(probe.owner.level(), -1.5, 150, .5, Set.<Relative>of(), 0, 0, false);
					aim(probe.owner, probe.dummy.getBoundingBox().getCenter());
				});
				context.waitTicks(5);
				context.getInput().holdKey(WildercordKeys.castMapping()); context.waitTicks(2);
				world.getServer().runOnServer(server -> {
					RelayState view = probe.owner.getAttached(RelayState.VIEW);
					check(view != null && view.phase() == RelayState.WARNING, "Fresh second down commits the warned lane");
					check(probe.dummy.hitSequence() == 0, "Warning precedes damage");
				});
				context.takeScreenshot(TestScreenshotOptions.of("relay_committed_lane").disableCounterPrefix());
				context.waitTicks(RelayRules.WARN_TICKS + 2);
				world.getServer().runOnServer(server -> {
					check(probe.dummy.hitSequence() == 1 && probe.dummy.lastDamage() > 0, "The warned ray actually wounds the dummy once");
					check(probe.payments == 1, "Commit and release do not repay or dispatch free copies");
					check(MasterStudies.practicedRelay(probe.owner), "Paid placement, movement and actual wound record optional practice");
					check(RelayCircles.recovering(probe.owner), "Release leaves ten ticks of casting recovery");
				});
				context.getInput().releaseKey(WildercordKeys.castMapping()); context.waitTicks(12);
			} finally {
				context.getInput().releaseKey(WildercordKeys.castMapping());
				context.runOnClient(mc -> { WildercordKeys.castMapping().setKey(original[0]); KeyMapping.resetMapping(); });
			}

			world.getServer().runOnServer(server -> reset(probe.owner, Runes.HARM));
			context.waitTicks(3);
			context.getInput().holdKey(WildercordKeys.castMapping()); context.waitTicks(3);
			context.getInput().releaseKey(WildercordKeys.castMapping()); context.waitTicks(2);
			world.getServer().runOnServer(server -> check(RelayCircles.pending(probe.owner), "Menu fixture accepted through the real cast key"));
			context.setScreen(CordScreen::new); context.waitTicks(3);
			world.getServer().runOnServer(server -> check(!RelayCircles.pending(probe.owner), "Opening a real menu sends cancellation"));
			context.setScreen(() -> null); context.waitTicks(3);

			editor(context, action -> world.getServer().runOnServer(server -> action.accept(server)));
			world.getServer().runOnServer(server -> { reset(probe.owner, Runes.FROST); directDown(probe.owner); });
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				float mana = Spellbooks.mana(probe.owner); long rest = probe.owner.getAttachedOrElse(RelayState.REST, 0L);
				RelayCircles.input(probe.owner, RelayInputRules.DOWN, 0, nonce); // exact duplicate
				RelayCircles.input(probe.owner, RelayInputRules.DOWN, 0, ++nonce); // held repeat
				check(RelayCircles.pending(probe.owner) && close(mana, Spellbooks.mana(probe.owner)), "Duplicate and held-down packets preserve the paid focus");
				SpellCaster.select(probe.owner, 1);
				check(!RelayCircles.pending(probe.owner) && probe.owner.getAttachedOrElse(RelayState.REST, 0L) == rest, "V selection cancels without refund or rest reset");
				Spellbooks.set(probe.owner, Spellbooks.get(probe.owner).withSpell(1, List.of(RelayRules.ID, Runes.FROST.id())));
				check(Spellbooks.readyAt(probe.owner, 1) > probe.owner.level().getGameTime(), "Every Relay slot shows the same shape rest");
			});

			world.getServer().runOnServer(server -> {
				reset(probe.owner, Runes.HARM); directDown(probe.owner);
				long rest = probe.owner.getAttachedOrElse(RelayState.REST, 0L);
				var beforeEditBook = Spellbooks.get(probe.owner);
				Spellbooks.set(probe.owner, beforeEditBook.withSpell(0,List.of(RelayRules.ID,Runes.FROST.id())));
				Spellbooks.set(probe.owner, beforeEditBook);
				check(!RelayCircles.pending(probe.owner), "Same-tick central book/library/loadout round trip permanently retires the paid focus");
				SpellCaster.edit(probe.owner, 0, List.of(RelayRules.ID, Runes.SHOCK.id()));
				check(!RelayCircles.pending(probe.owner) && probe.owner.getAttachedOrElse(RelayState.REST, 0L) == rest, "Editing retires the original paid spell identity");
				admission(probe.owner);
			});
			context.waitTicks(2);
			world.getServer().runOnServer(server -> { downAfterUp(probe.owner); check(!RelayCircles.pending(probe.owner) && Heart.active(probe.owner) == 8, "A fresh insufficient-mana edge is refused"); });
			context.waitTicks(2);
			world.getServer().runOnServer(server -> { downAfterUp(probe.owner); check(!RelayCircles.pending(probe.owner) && Heart.active(probe.owner) == 8, "A later second insufficient-mana edge cannot overcast"); });
			context.waitTicks(2);
			world.getServer().runOnServer(server -> { reset(probe.owner, Runes.HARM); directDown(probe.owner); });
			context.waitTicks(RelayRules.FOCUS_TICKS + 2);
			world.getServer().runOnServer(server -> check(!RelayCircles.pending(probe.owner), "Uncommitted focus expires after four seconds"));
			presentation(context, action -> world.getServer().runOnServer(server -> action.accept(server)));
			world.getServer().runOnServer(server -> {
				reset(probe.owner,Runes.HARM); Spellbooks.learn(probe.owner,Runes.BOLT.id());
				Spellbooks.set(probe.owner,Spellbooks.get(probe.owner).withSpell(0,List.of(Runes.BOLT.id(),Runes.HARM.id())).withSpell(1,List.of(RelayRules.ID,Runes.HARM.id())));
			});context.waitTicks(5);
			context.getInput().holdKey(WildercordKeys.castMapping());context.waitTicks(8);
			world.getServer().runOnServer(server -> {check(probe.owner.hasAttached(WildercordAttachments.CHARGE),"Ordinary held cast starts normally");SpellCaster.select(probe.owner,1);});
			context.waitTicks(3);context.getInput().releaseKey(WildercordKeys.castMapping());context.waitTicks(5);
			world.getServer().runOnServer(server -> check(!probe.owner.hasAttached(WildercordAttachments.CHARGE)&&!RelayCircles.pending(probe.owner),"Selecting Relay while an ordinary cast is held does not strand or convert the captured charge"));
		} finally { probe = null; }
	}

	private static void editor(ClientGameTestContext c, java.util.function.Consumer<java.util.function.Consumer<MinecraftServer>> onServer) {
		onServer.accept(server -> { reset(probe.owner, Runes.HARM); Spellbooks.learn(probe.owner, Runes.AMPLIFY.id()); });
		c.waitTicks(3); c.setScreen(CordScreen::new); c.waitTicks(3);
		click(c, c.computeOnClient(mc -> ((CordScreen)mc.gui.screen()).socketPoint(0, 1)));
		onServer.accept(server -> check(Spellbooks.get(probe.owner).spells().getFirst().equals(List.of(RelayRules.ID)), "Removing a Relay payload saves the noncastable draft instead of leaving the previous spell armed"));
		c.runOnClient(mc -> ((CordScreen)mc.gui.screen()).searchFor("Amplify")); c.waitTicks(2);
		click(c, c.computeOnClient(mc -> ((CordScreen)mc.gui.screen()).codexPoint(Runes.AMPLIFY.id())));
		c.runOnClient(mc -> check(((CordScreen)mc.gui.screen()).rowRunes(0).equals(List.of(RelayRules.ID, Runes.AMPLIFY.id())), "Editor retains the exact unfinished row"));
		onServer.accept(server -> check(Spellbooks.get(probe.owner).spells().getFirst().equals(List.of(RelayRules.ID, Runes.AMPLIFY.id())) && RelayCircles.problem(probe.owner, 0)!=null, "Server and visible invalid draft agree and cannot cast"));
		c.setScreen(() -> null); c.waitTicks(3);
		int payments = probe.payments;
		c.getInput().holdKey(WildercordKeys.castMapping()); c.waitTicks(3); c.getInput().releaseKey(WildercordKeys.castMapping()); c.waitTicks(2);
		onServer.accept(server -> check(probe.payments==payments&&!RelayCircles.pending(probe.owner), "Closing an invalid draft never revives the old accepted Relay"));
		onServer.accept(server -> reset(probe.owner, Runes.HARM)); c.waitTicks(3); c.setScreen(CordScreen::new); c.waitTicks(3);
		onServer.accept(server -> probe.owner.setAttached(WildercordAttachments.CIRCLES, 7)); c.waitTicks(3);
		c.runOnClient(mc -> ((CordScreen)mc.gui.screen()).searchFor("Amplify")); c.waitTicks(2);
		click(c, c.computeOnClient(mc -> ((CordScreen)mc.gui.screen()).codexPoint(Runes.AMPLIFY.id())));
		c.runOnClient(mc -> check(((CordScreen)mc.gui.screen()).rowRunes(0).equals(List.of(RelayRules.ID, Runes.HARM.id())) && ((CordScreen)mc.gui.screen()).lastRefusal()!=null, "Unauthorized edit explicitly restores the accepted row and explains refusal"));
		// Receive an actual rejection while a nested settings child retains the editor.
		CordScreen[] retained = new CordScreen[1];
		c.runOnClient(mc -> {
			retained[0] = (CordScreen) mc.gui.screen();
			retained[0].searchFor("Amplify");
			clickNow(retained[0], retained[0].codexPoint(Runes.AMPLIFY.id()));
			mc.gui.setScreen(new dev.wildercord.client.CombatPresentationScreen(new dev.wildercord.client.MagicSettingsScreen(retained[0])));
		}); c.waitTicks(5);
		c.runOnClient(mc -> {
			check(retained[0].rowRunes(0).equals(List.of(RelayRules.ID, Runes.HARM.id())), "Rejected editor row reconciles through the exact nested retained parent");
			mc.gui.screen().onClose(); mc.gui.screen().onClose();
			check(mc.gui.screen()==retained[0], "Returning through children keeps the reconciled editor instance");
		});
		onServer.accept(server -> probe.owner.setAttached(WildercordAttachments.CIRCLES,8)); c.waitTicks(3);
		dev.wildercord.net.RelayEditorReply[] stale = new dev.wildercord.net.RelayEditorReply[1];
		c.runOnClient(mc -> {
			var editor = retained[0]; clickNow(editor,editor.socketPoint(0,1));
			stale[0]=new dev.wildercord.net.RelayEditorReply(0,dev.wildercord.net.RelayEditorReply.key(editor.rowRunes(0)),List.of(RelayRules.ID,Runes.HARM.id()),"late rejection",editor.editorSession(),editor.editorRevision(0));
			editor.searchFor("Amplify"); clickNow(editor,editor.codexPoint(Runes.AMPLIFY.id())); clickNow(editor,editor.socketPoint(0,1));
		}); c.waitTicks(5);
		onServer.accept(server -> net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(probe.owner,stale[0])); c.waitTicks(3);
		c.runOnClient(mc -> check(retained[0].rowRunes(0).equals(List.of(RelayRules.ID)), "An older reply cannot overwrite a newer edit even when the row returns to the same contents"));
		c.setScreen(CordScreen::new);c.waitTicks(3);
		onServer.accept(server -> net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(probe.owner,stale[0])); c.waitTicks(3);
		c.runOnClient(mc -> check(((CordScreen)mc.gui.screen()).rowRunes(0).equals(List.of(RelayRules.ID)) && retained[0].rowRunes(0).equals(List.of(RelayRules.ID)), "Closed editor replies cannot mutate either the replacement editor or the abandoned parent"));
		onServer.accept(server -> { probe.owner.setAttached(WildercordAttachments.CIRCLES,8); chatCopies(); });
		c.setScreen(() -> null);c.waitTicks(3);
	}
	private static void clickNow(CordScreen editor, double[] at) {
		check(at != null,"Editor control is visible before opening a child");
		var press=new net.minecraft.client.input.MouseButtonEvent(at[0],at[1],new net.minecraft.client.input.MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));
		editor.mouseClicked(press,false);editor.mouseReleased(press);
	}
	private static void click(ClientGameTestContext c, double[] point) {
		check(point!=null,"Editor control is visible"); double scale=c.computeOnClient(mc->mc.getWindow().getGuiScale());
		c.getInput().setCursorPos(point[0]*scale,point[1]*scale);c.waitTicks(1);c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);c.waitTicks(5);
	}
	private static void chatCopies() {
		String knot=dev.wildercord.spell.Knots.id(List.of(Runes.RELAY,Runes.HARM),"");
		for(String code:List.of("wc:"+"harm.".repeat(12)+"relay",dev.wildercord.spell.SpellCodes.encode(List.of(Runes.SELF.id(),Runes.HEAL.id(),knot)))) {
			var card=SpellChat.decorate(net.minecraft.network.chat.Component.literal("Try "+code+" !"));
			var copied=card.getSiblings().stream().map(part->part.getStyle().getClickEvent()).filter(event->event instanceof net.minecraft.network.chat.ClickEvent.CopyToClipboard).map(event->((net.minecraft.network.chat.ClickEvent.CopyToClipboard)event).value()).toList();
			check(copied.equals(List.of(code)),"Actual chat card CopyToClipboard preserves the complete refused token");
			check(RelayRules.containsIds(dev.wildercord.spell.SpellCodes.decode(copied.getFirst())),"Copy/paste cannot erase the forbidden Relay suffix or composite");
		}
	}

	private static void admission(ServerPlayer p) {
		reset(p, Runes.HARM);
		float before = Spellbooks.mana(p);
		SpellCaster.cast(p, 0); Charging.request(p, 0, true);
		check(!RelayCircles.pending(p) && !p.hasAttached(WildercordAttachments.CHARGE) && close(before, Spellbooks.mana(p)), "Legacy cast/charge packets cannot bypass fresh Relay input");
		List<String> pair = List.of(RelayRules.ID, Runes.HARM.id());
		p.setAttached(WildercordAttachments.CIRCLES, 7); check(RelayCircles.problem(p, 0) != null, "Circle gate rejects before edge handling"); downAfterUp(p);
		check(!RelayCircles.pending(p) && close(before, Spellbooks.mana(p)), "Active VII refuses entire row before payment");
		p.setAttached(WildercordAttachments.CIRCLES, 8);
		p.setAttached(WildercordAttachments.GRIMOIRE, List.of("feat:archivist")); check(RelayCircles.problem(p, 0) != null, "Lesson gate rejects before edge handling"); downAfterUp(p);
		check(!RelayCircles.pending(p), "Forged learned rune without lesson cannot cast");
		p.setAttached(WildercordAttachments.GRIMOIRE, List.of("feat:archivist", MasterStudyRules.RELAY));
		Spellbooks.setCord(p, new ItemStack(WildercordItems.COPPER_CORD));
		check(SpellCaster.edit(p, 0, pair) != null && Spellbooks.get(p).spells().getFirst().equals(pair), "Lower Cord edit refuses atomically instead of keeping implicit Self + Harm");
		check(SpellCaster.activeRunes(Spellbooks.get(p), 0, CordTier.COPPER).isEmpty(), "Lower Cord cannot filter Relay out and expose its effect");
		Spellbooks.setCord(p, new ItemStack(WildercordItems.ECHO_CORD));
		for (List<String> invalid : List.of(List.of(RelayRules.ID, Runes.HARM.id(), Runes.AMPLIFY.id()), List.of(Runes.SELF.id(), Runes.HARM.id(), RelayRules.ID), List.of(RelayRules.ID, Runes.HEAL.id()))) {
			Spellbooks.set(p, Spellbooks.get(p).withSpell(0, invalid)); check(RelayCircles.problem(p, 0) != null, "Whole-row grammar rejects before edge handling"); downAfterUp(p);
			check(!RelayCircles.pending(p) && close(before, Spellbooks.mana(p)), "Unsupported whole rows never pay or partly cast: " + invalid);
		}
		Spellbooks.set(p, Spellbooks.get(p).withSpell(0, pair)); Spellbooks.setMana(p, 0);
		check(RelayCircles.problem(p,0)==null, "Insufficient-mana fixture satisfies every non-resource gate");
		check(Heart.cooldownTicks(p, SpellCompiler.compile(List.of(Runes.RELAY, Runes.HARM)), .1) == 160, "The readout never discounts shape rest");
	}

	private static void presentation(ClientGameTestContext c, java.util.function.Consumer<java.util.function.Consumer<MinecraftServer>> onServer) {
		int[][] sizes = {{1920,1080},{1280,720},{854,480}};
		for (int[] size : sizes) for (int scale = 1; scale <= 4; scale++) {
			final int gui = scale;
			onServer.accept(server -> { reset(probe.owner, Runes.HARM); directDown(probe.owner); });
			c.waitTicks(2);
			c.runOnClient(mc -> { mc.getWindow().setWindowed(size[0], size[1]); mc.options.guiScale().set(gui); mc.resizeGui(); });
			c.takeScreenshot(TestScreenshotOptions.of("relay_focus_" + size[0] + "x" + size[1] + "_gui_" + scale).disableCounterPrefix());
			onServer.accept(server -> RelayCircles.cancel(probe.owner));
			c.waitTicks(2);
		}
	}

	public static void prepare(ServerPlayer player, dev.wildercord.spell.RuneDef effect) {
		var level = player.level();
		for (int x = -10; x <= 10; x++) for (int z = -4; z <= 18; z++) {
			level.setBlock(new BlockPos(x, 149, z), Blocks.STONE.defaultBlockState(), 2);
			for (int y = 150; y <= 154; y++) level.setBlock(new BlockPos(x,y,z), Blocks.AIR.defaultBlockState(), 2);
		}
		player.setGameMode(GameType.SURVIVAL); player.setHealth(player.getMaxHealth());
		player.setAttached(WildercordAttachments.CIRCLES, 8); player.setAttached(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
		player.setAttached(WildercordAttachments.GRIMOIRE, List.of("feat:archivist", MasterStudyRules.RELAY));
		Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
		Spellbooks.set(player, new Spellbook(List.of(RelayRules.ID, Runes.HARM.id(), Runes.FROST.id(), Runes.SHOCK.id()), List.of(List.of(RelayRules.ID, effect.id())), 0, true));
		reset(player, effect);
	}
	public static void reset(ServerPlayer player, dev.wildercord.spell.RuneDef effect) {
		RelayCircles.cancel(player);
		player.teleportTo(player.level(), FEET.x, FEET.y, FEET.z, Set.<Relative>of(), 0, 55, false);
		player.setDeltaMovement(Vec3.ZERO); player.setOnGround(true); player.setShiftKeyDown(false);
		player.setAttached(RelayState.REST, 0L);
		for (int i = 0; i < dev.wildercord.gear.SpellSlots.ALL; i++) Spellbooks.setReadyAt(player, i, 0);
		Spellbooks.set(player, Spellbooks.get(player).withSelected(0).withSpell(0, List.of(RelayRules.ID, effect.id())));
		Spellbooks.setMana(player, 200);
	}
	public static void directDown(ServerPlayer player) { RelayCircles.input(player, RelayInputRules.UP, 0, ++nonce); RelayCircles.input(player, RelayInputRules.DOWN, 0, ++nonce); }
	public static void downAfterUp(ServerPlayer player) { RelayCircles.input(player, RelayInputRules.UP, 0, ++nonce); directDown(player); }
	public static void aim(ServerPlayer player, Vec3 target) {
		Vec3 to = target.subtract(player.getEyePosition());
		float yaw = (float)Math.toDegrees(Math.atan2(-to.x, to.z));
		float pitch = (float)-Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x*to.x+to.z*to.z)));
		player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), yaw, pitch, false);
	}
	public static ServerPlayer player(MinecraftServer server) { return server.getPlayerList().getPlayers().getFirst(); }
	public static boolean close(double a, double b) { return Math.abs(a-b) < .01; }
	public static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
	private static void listen() {
		if (listening) return; listening = true;
		WildercordEvents.BEFORE_CAST.register((p, slot, runes, cost) -> { if (probe != null && p == probe.owner && RelayRules.contains(runes)) probe.beforePayment = Spellbooks.mana(p); return true; });
		WildercordEvents.AFTER_CAST.register((p, slot, runes, spent) -> {
			if (probe != null && p == probe.owner && RelayRules.contains(runes)) { probe.payments++; probe.spent = spent; probe.afterPayment = Spellbooks.mana(p); }
		});
	}
}
