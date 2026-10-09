package dev.wildercord.aura;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.cast.Charging;
import dev.wildercord.cast.Effects;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** The selected-style entry keeps payment, active frames, source context and cancellation in one authoritative lifecycle. */
public final class MastersStyleTimelineTest implements FabricClientGameTest {
	private int impacts, completions, followUps;
	private boolean cancelAfterFirst, interruptAccepted, repeatedInterrupt;
	private Vec3 activeAim;
	private int activeTargets;
	private AuraApi.StringArt fixture;

	private static void check(boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}

	private static void prepare(ServerPlayer player) {
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.SOVEREIGN, 4500, 160, 0));
		player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		player.setYRot(0);
		player.setXRot(0);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
			world.getServer().runCommand("gamerule minecraft:natural_health_regeneration false");
			world.getServer().runCommand("fill -8 99 -8 8 99 8 minecraft:stone");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(.5, 100, .5);
				prepare(player);
				var foe = EntityTypes.HUSK.create(player.level(), EntitySpawnReason.COMMAND);
				check(foe != null, "An active-frame target exists");
				foe.setNoAi(true);
				foe.snapTo(.5, 100, 2.5, 180, 0);
				player.level().addFreshEntity(foe);
				fixture = AuraApi.StringArt.of("kindling_draw", "swing swing low", AuraRules.GLOW, 6, 40, (owner, move) -> {
					check(SwordStrings.performing() == fixture, "Active frames restore the art identity");
					check(Effects.applying() == owner && Effects.applyingCast() == null, "Aura has a source without becoming a spell");
					check(move.struck() == null, "A pre-windup victim cannot bypass the active cone");
					activeAim = ArtKit.flat(owner);
					activeTargets = ArtKit.arc(owner, move.struck(), 4, 75, 3).size();
					impacts++;
					var continuation = MastersArts.continuation(owner);
					dev.wildercord.cast.Scheduler.later(2, () -> { if (continuation.getAsBoolean()) followUps++; });
					if (cancelAfterFirst) {
						dev.wildercord.cast.Scheduler.later(1, () -> interruptAccepted = dev.wildercord.cast.Statuses.interrupt(owner));
						dev.wildercord.cast.Scheduler.later(3, () -> repeatedInterrupt = dev.wildercord.cast.Statuses.interrupt(owner));
					}
					return true;
				});
				AuraApi.onString((owner, art, move) -> { if (art == fixture) completions++; });
				long now = player.level().getGameTime();
				double cost = SwordStrings.price(player, fixture);
				check(SwordStrings.perform(player, fixture, List.of(1, 1, SwordString.Token.LOW.bit())), "A checked style enters its windup");
				check(Math.abs(Aura.aura(player) - (160 - cost)) < .001, "The accepted windup pays once, immediately");
				check(SwordStrings.readyAt(player, fixture.id()) == now + SwordStrings.rest(player, fixture), "Individual rest starts at acceptance");
				check(impacts == 0 && completions == 0, "Neither effects nor success hooks run before the active frame");
				float after = Aura.aura(player);
				check(!SwordStrings.perform(player, fixture, List.of(1, 1, SwordString.Token.LOW.bit())), "A direct repeat cannot bypass commitment");
				check(Aura.aura(player) == after, "A rejected repeat cannot charge again");
				int selected = player.getInventory().getSelectedSlot();
				var selectedStack = player.getMainHandItem();
				player.getInventory().setSelectedSlot(selected);
				player.getInventory().setSelectedItem(selectedStack);
				player.getInventory().setItem((selected + 2) % 9, new ItemStack(Items.STICK));
				player.setYRot(180);
			});
			context.waitTicks(10);
			world.getServer().runOnServer(server -> {
				check(followUps == 1, "An uninterrupted physical continuation can land");
				check(impacts == 1 && completions == 1, "Performer and hooks run once; same selection/stack and unrelated inventory writes never cancel");
				check(activeAim.z > .9 && activeTargets == 1, "Turning during windup does not move the committed cone");
				check(Effects.applying() == null, "The active-frame source is restored afterward");
			});
			context.waitTicks(40);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				prepare(player);
				check(SwordStrings.perform(player, fixture, List.of(1, 1, SwordString.Token.LOW.bit())), "Another rested form starts");
				Charging.interrupt(player);
				check(MastersArts.committed(player), "Interruption retains the committed recovery");
			});
			context.waitTicks(8);
			world.getServer().runOnServer(server -> check(impacts == 1 && completions == 1, "An interrupted windup never runs its performer or completion hooks"));
			context.waitTicks(40);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				prepare(player);
				check(SwordStrings.perform(player, fixture, List.of(1, 1, SwordString.Token.LOW.bit())), "A final form starts");
				player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			});
			context.waitTicks(8);
			world.getServer().runOnServer(server -> check(impacts == 1 && completions == 1, "Weapon changes cancel the pending active frame too"));
			context.waitTicks(40);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				prepare(player);
				check(SwordStrings.perform(player, fixture, List.of(1, 1, SwordString.Token.LOW.bit())), "A round-trip selection test starts its windup");
				int slot = player.getInventory().getSelectedSlot();
				var original = player.getMainHandItem();
				player.getInventory().setSelectedSlot((slot + 1) % 9);
				player.getInventory().setSelectedSlot(slot);
				check(player.getMainHandItem() == original, "The exact original stack is back before the same server call ends");
				check(MastersArts.committed(player), "The cancelled windup still owns its paid recovery");
			});
			context.waitTicks(10);
			world.getServer().runOnServer(server -> check(impacts == 1 && completions == 1,
				"Switching away and back is latched and cannot restore a pending hit"));
			context.waitTicks(40);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				prepare(player);
				cancelAfterFirst = true;
				check(SwordStrings.perform(player, fixture, List.of(1, 1, SwordString.Token.LOW.bit())), "A multi-cut form starts");
			});
			context.waitTicks(12);
			world.getServer().runOnServer(server -> {
				check(impacts == 2 && completions == 2, "Its first active frame completed normally");
				check(followUps == 1, "Interruption after release stops the remaining physical cut");
				check(interruptAccepted && !repeatedInterrupt, "Aura interruption records the shared anti-chain-lock immunity");
			});
		}
	}
}
