package dev.wildercord.familiar;

import dev.wildercord.cast.LeyWalker;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.world.LeyLines;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Where wild wisps come from: now and then, near a player in the Overworld, one drifts up out of
 * a ley line at night, or out of the mana around an awake Wellstone at any hour. They're rare
 * (a roll every ten seconds, a few near anyone at once) and fade again with the day.
 */
public final class WispSpawner {
	private WispSpawner() {}

	/** Ticks between tries for each player. */
	private static final int EVERY = 200;
	/** Wild wisps allowed within 64 blocks of a player. */
	private static final int CAP = 3;
	/** How strong a ley line must run where a wisp rises. */
	private static final double LEY = 0.35;

	static void tick(MinecraftServer server) {
		if (server.getTickCount() % EVERY != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ServerLevel level = player.level();
			if (player.isSpectator() || level.dimension() != Level.OVERWORLD || !level.getGameRules().get(GameRules.SPAWN_MOBS)) {
				continue;
			}
			trySpawn(level, player);
		}
	}

	/** One try near one player; returns the wisp that rose, or null. */
	public static Wisp trySpawn(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();
		boolean well = player.getAttachedOrElse(WildercordAttachments.WELL_UNTIL, 0L) > level.getGameTime();
		boolean night = level.isDarkOutside();
		if (!well && !night) {
			return null;
		}
		if (level.getEntitiesOfClass(Wisp.class, player.getBoundingBox().inflate(64), Wisp::wild).size() >= CAP) {
			return null;
		}
		long seed = LeyWalker.seed(level);
		for (int attempt = 0; attempt < 6; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = well ? 6 + random.nextDouble() * 10 : 16 + random.nextDouble() * 24;
			double x = player.getX() + Math.cos(angle) * distance;
			double z = player.getZ() + Math.sin(angle) * distance;
			boolean onLey = LeyLines.strength(seed, x, z) >= LEY;
			// A ley line at night rises a wisp one time in four; a Wellstone's mana, one in ten.
			if (!(night && onLey && random.nextInt(4) == 0) && !(well && random.nextInt(10) == 0)) {
				continue;
			}
			int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
			BlockPos at = BlockPos.containing(x, ground + 1.5 + random.nextDouble() * 1.5, z);
			if (!level.getBlockState(at).isAir() || !level.getFluidState(at.below()).isEmpty()) {
				continue;
			}
			return spawn(level, at, WispRules.ELEMENTS.get(random.nextInt(WispRules.ELEMENTS.size())));
		}
		return null;
	}

	/** A wild wisp of {@code element} at {@code at}. */
	public static Wisp spawn(ServerLevel level, BlockPos at, String element) {
		Wisp wisp = FamiliarContent.WISP.create(level, EntitySpawnReason.NATURAL);
		if (wisp == null) {
			return null;
		}
		wisp.setElement(element);
		wisp.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
		level.addFreshEntity(wisp);
		return wisp;
	}
}
