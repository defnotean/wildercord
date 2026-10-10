package dev.wildercord.town;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/** Sends wandering caravans to travellers out in the overworld. See {@link CaravanRules}. */
public final class Caravans {
	private Caravans() {}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CaravanRules.CHECK_TICKS == 0 && server.getTickCount() > 0) check(server);
		});
	}

	private static void check(MinecraftServer server) {
		ServerLevel level = server.overworld();
		if (!level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SPAWN_WANDERING_TRADERS)) return;
		RandomSource random = level.getRandom();
		if (random.nextDouble() >= CaravanRules.CHANCE) return;
		List<ServerPlayer> players = level.players().stream().filter(p -> p.isAlive() && !p.isSpectator()).toList();
		if (players.isEmpty()) return;
		send(level, players.get(random.nextInt(players.size())));
	}

	/** Makes camp near {@code player}; returns the caravaneer, or null with nowhere to stand or another caravan close by. */
	public static WayfarerKeeper send(ServerLevel level, ServerPlayer player) {
		double apart = CaravanRules.APART * CaravanRules.APART;
		if (!level.getEntitiesOfClass(WayfarerKeeper.class, player.getBoundingBox().inflate(CaravanRules.APART),
			k -> k.role() == WayfarerKeeper.Role.CARAVANEER && k.distanceToSqr(player) < apart).isEmpty()) return null;
		RandomSource random = level.getRandom();
		for (int attempt = 0; attempt < 10; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = CaravanRules.SPAWN_NEAR + random.nextDouble() * (CaravanRules.SPAWN_FAR - CaravanRules.SPAWN_NEAR);
			BlockPos column = BlockPos.containing(player.getX() + Math.cos(angle) * distance, player.getY(), player.getZ() + Math.sin(angle) * distance);
			if (!level.isPositionEntityTicking(column)) continue;
			BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
			if (!level.getFluidState(ground).isEmpty() || !level.getFluidState(ground.below()).isEmpty()) continue;
			WayfarerKeeper keeper = Town.KEEPER.create(level, EntitySpawnReason.EVENT);
			if (keeper == null) return null;
			keeper.snapTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, random.nextFloat() * 360, 0);
			if (!level.noCollision(keeper)) continue;
			keeper.setRole(WayfarerKeeper.Role.CARAVANEER);
			level.addFreshEntity(keeper);
			for (int i = 0; i < CaravanRules.LLAMAS; i++) {
				TraderLlama llama = EntityTypes.TRADER_LLAMA.create(level, EntitySpawnReason.EVENT);
				if (llama == null) continue;
				llama.snapTo(ground.getX() + 0.5 + (i == 0 ? 2 : -2), ground.getY(), ground.getZ() + 0.5, random.nextFloat() * 360, 0);
				if (!level.noCollision(llama)) llama.snapTo(keeper.getX(), keeper.getY(), keeper.getZ(), 0, 0);
				level.addFreshEntity(llama);
				llama.setLeashedTo(keeper, true);
			}
			Direction way = Direction.getApproximateNearest(keeper.getX() - player.getX(), 0, keeper.getZ() - player.getZ());
			player.sendSystemMessage(Component.translatable("message.wildercord.caravan.camp",
				Component.translatable("town.wildercord.way." + way.getSerializedName())).withStyle(ChatFormatting.YELLOW));
			return keeper;
		}
		return null;
	}
}
