package dev.wildercord.gametest;

import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.MastersArts;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** Actual active-frame harm, server payment, denied packets and interruption after an accepted windup. */
public final class WildercordMastersArtsTest implements FabricClientGameTest {
	private LivingEntity target;

	private static void check(boolean ok, String message) {
		if (!ok) throw new AssertionError(message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
			world.getServer().runCommand("gamerule minecraft:natural_regeneration false");
			world.getServer().runCommand("fill -8 99 -8 8 99 8 minecraft:stone");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(0.5, 100, 0.5);
				player.setYRot(0);
				player.setXRot(0);
				player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
				player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", 4, 1800, 100, 0));
				var mob = EntityTypes.HUSK.create(player.level(), EntitySpawnReason.COMMAND);
				check(mob != null, "Combat target exists");
				mob.setNoAi(true);
				mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
				mob.setHealth(100);
				mob.snapTo(0.5, 100, 2.8, 180, 0);
				player.level().addFreshEntity(mob);
				target = mob;
				check(!MastersArts.activate(player, -1) && !MastersArts.activate(player, 999), "Invalid packet ordinals refuse");
				check(Aura.aura(player) == 100, "Invalid packets charge nothing");
				check(MastersArts.activate(player, 1), "Form unlocks Rising Break");
				check(Aura.aura(player) == 80, "Accepted move pays its exact price before any hit");
				check(target.getHealth() == 100, "Windup deals no immediate damage");
				check(!MastersArts.activate(player, 1) && !MastersArts.activate(player, 2), "Rest cannot be bypassed by resending or switching moves");
			});
			context.waitTicks(12);
			world.getServer().runOnServer(server -> check(target.getHealth() < 100, "Damage lands on the active frame"));
			context.waitTicks(100);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				target.setHealth(100);
				player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", 4, 1800, 100, 0));
				check(MastersArts.activate(player, 1), "Rest ends and a second move can start");
				player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			});
			context.waitTicks(12);
			world.getServer().runOnServer(server -> check(target.getHealth() == 100, "Changing the weapon during windup cancels its pending hit"));
		}
	}
}
