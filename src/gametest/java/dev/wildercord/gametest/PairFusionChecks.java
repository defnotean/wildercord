package dev.wildercord.gametest;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Effects;
import dev.wildercord.pairs.PairSpecs;
import dev.wildercord.spell.EffectKind;
import dev.wildercord.spell.Fusions;
import dev.wildercord.spell.PairRunes;
import dev.wildercord.spell.PairSpec;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Every written pair fusion, cast for real: the altar fuses its two runes into it, and its effect runs on a husk (or
 * on the caster, for a helpful one) through its whole animation without throwing, and a harmful one leaves its mark.
 * Run from {@link WildercordFusionTest}, in a world of its own.
 */
public final class PairFusionChecks {
	private PairFusionChecks() {}

	private static final int BATCH = 6;
	private static final int SPACING = 16;

	private record Written(RuneDef a, RuneDef b, RuneDef pair, PairSpec spec) {}

	private static void check(boolean ok, String why) {
		if (!ok) throw new AssertionError(why);
	}

	public static void run(ClientGameTestContext c) {
		try (var world = c.worldBuilder().create()) {
			c.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("time set noon");
			List<Written> written = world.getServer().computeOnServer(server -> {
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.SURVIVAL);
				p.teleportTo(p.level(), 0.5, 101, 0.5, java.util.Set.of(), 0, 0, false);
				for (BlockPos q : BlockPos.betweenClosed(new BlockPos(-56, 100, -8), new BlockPos(56, 100, 24))) {
					p.level().setBlock(q, Blocks.STONE_BRICKS.defaultBlockState(), 2);
				}
				return roster();
			});
			check(written.size() == PairSpecs.COUNT, "every generated pair resolves to two pairable runes: " + written.size() + " of " + PairSpecs.COUNT);
			for (int from = 0; from < written.size(); from += BATCH) {
				List<Written> batch = written.subList(from, Math.min(written.size(), from + BATCH));
				List<Integer> marks = new ArrayList<>();
				world.getServer().runOnServer(server -> {
					ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
					p.setHealth(p.getMaxHealth());
					p.removeAllEffects();
					p.teleportTo(p.level(), 0.5, 101, 0.5, java.util.Set.of(), 0, 0, false);
					for (int i = 0; i < batch.size(); i++) {
						Written w = batch.get(i);
						check(Fusions.recipe(w.a(), w.b()).filter(f -> f instanceof Fusions.Pair pair && pair.result() == w.pair()).isPresent()
							|| Fusions.recipe(w.a(), w.b()).filter(f -> f instanceof Fusions.Signature).isPresent(),
							"the altar fuses " + w.a().path() + " and " + w.b().path() + " into " + w.spec().name());
						var husk = EntityTypes.HUSK.create(p.level(), EntitySpawnReason.COMMAND);
						check(husk != null, "husk created");
						husk.setNoAi(true);
						husk.setPos((i - BATCH / 2) * SPACING + 0.5, 101, 10.5);
						husk.setOnGround(true);
						p.level().addFreshEntity(husk);
						marks.add(husk.getId());
						boolean helpful = w.spec().kind() == EffectKind.HELPFUL;
						LivingEntity target = helpful ? p : husk;
						Vec3 at = target.position();
						var node = SpellCompiler.compile(List.of(Runes.SELF, w.pair())).root().groups.getFirst().effects.getFirst();
						List<Entity> hit = List.of(target);
						try {
							Effects.apply(new Cast(p), node, new Cast.Hit(hit, at, new Vec3(0, 0, 1), p.position(), null, Direction.UP, helpful));
						} catch (RuntimeException e) {
							throw new AssertionError(w.spec().name() + " (" + w.a().path() + " + " + w.b().path() + ") threw on cast", e);
						}
					}
				});
				// The longest animation runs its course.
				c.waitTicks(100);
				world.getServer().runOnServer(server -> {
					ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
					for (int i = 0; i < batch.size(); i++) {
						Written w = batch.get(i);
						Entity e = p.level().getEntity(marks.get(i));
						if (w.spec().kind() == EffectKind.HARMFUL) {
							boolean touched = e == null || !e.isAlive() || (e instanceof LivingEntity l
								&& (l.getHealth() < l.getMaxHealth() || !l.getActiveEffects().isEmpty() || l.isOnFire() || l.getTicksFrozen() > 0
								|| l.position().distanceTo(new Vec3((i - BATCH / 2) * SPACING + 0.5, 101, 10.5)) > 0.5));
							check(touched, w.spec().name() + " (" + w.a().path() + " + " + w.b().path() + ") did nothing to its target");
						}
						if (e != null) {
							e.discard();
						}
					}
					p.clearFire();
				});
			}
		}
	}

	/** Every written pair, by its two runes. */
	private static List<Written> roster() {
		List<RuneDef> runes = Runes.all().stream().filter(PairRunes::pairable).sorted(Comparator.comparing(RuneDef::path)).toList();
		List<Written> out = new ArrayList<>();
		for (int i = 0; i < runes.size(); i++) {
			for (int j = i + 1; j < runes.size(); j++) {
				RuneDef a = runes.get(i);
				RuneDef b = runes.get(j);
				PairSpec spec = PairSpecs.spec(a.path(), b.path());
				if (spec != null) {
					RuneDef pair = PairRunes.of(a, b).orElseThrow(() -> new AssertionError("no pair rune for " + a.path() + " + " + b.path()));
					out.add(new Written(a, b, pair, spec));
				}
			}
		}
		return out;
	}
}
