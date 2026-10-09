package dev.wildercord.cast;

import dev.wildercord.cast.packs.WardState;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static dev.wildercord.cast.NextSignatureNative.check;

/**
 * The support pack (Worst First to Faithful) in a real world: a connected Survival caster and an allied player apply seventeen
 * of its runes through the production effect path, and each outcome is read back from the live entities and blocks.
 */
public final class FxSupportPackTest implements FabricClientGameTest {
	private static final String GUEST = "fxs_guest";

	@Override
	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(30);
			var server = w.getServer();
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule natural_health_regeneration false");
			server.runOnServer(s -> {
				ServerPlayer p = NextSignatureNative.player(s);
				NextSignatureNative.floor(p);
				NextSignatureNative.guest(p, GUEST, true);
			});
			c.waitTicks(2);

			// Stoutheart through the real cast path: the editor accepts it, the cast pays, the caster is guarded.
			server.runOnServer(s -> {
				ServerPlayer p = NextSignatureNative.player(s);
				NextSignatureNative.cast(p, Runes.SELF, Runes.STOUTHEART);
			});
			c.waitTicks(8);
			server.runOnServer(s -> {
				ServerPlayer p = NextSignatureNative.player(s);
				check(p.hasEffect(MobEffects.RESISTANCE), "Stoutheart gives the caster Resistance");
				p.removeEffect(MobEffects.RESISTANCE);
				p.setHealth(20.0F);
			});

			// Grace: a killing blow leaves the ally at 2 with Resistance III, once.
			server.runOnServer(s -> {
				ServerPlayer p = NextSignatureNative.player(s);
				ServerPlayer g = guest(s);
				ServerLevel level = p.level();
				g.setHealth(20.0F);
				g.damageCooldownTime = 0;
				apply(p, Runes.GRACE, List.of(g), g.position());
				check(WardState.has("grace", g), "Grace is on the ally");
				g.hurtServer(level, level.damageSources().generic(), 100.0F);
				check(g.isAlive() && near(g.getHealth(), 2.0F), "Grace leaves the ally at 2 health: " + g.getHealth());
				MobEffectInstance r = g.getEffect(MobEffects.RESISTANCE);
				check(r != null && r.getAmplifier() == 2, "Grace gives Resistance III");
				check(!WardState.has("grace", g), "Grace is spent by the save");
				g.removeEffect(MobEffects.RESISTANCE);
			});

			server.runOnServer(s -> {
				ServerPlayer p = NextSignatureNative.player(s);
				ServerPlayer g = guest(s);
				ServerLevel level = p.level();

				// Worst First heals the most hurt ally near where it lands for 6.
				g.setHealth(10.0F);
				apply(p, Runes.WORST_FIRST, List.of(g), g.position());
				check(between(g.getHealth() - 10.0F, 6.0F, 9.0F), "Worst First heals the hurt ally for 6 (a world quirk may add half): " + g.getHealth());

				// Salve heals 2 and gives Regeneration.
				g.setHealth(10.0F);
				apply(p, Runes.SALVE, List.of(g), g.position());
				check(between(g.getHealth() - 10.0F, 2.0F, 3.0F) && g.hasEffect(MobEffects.REGENERATION), "Salve heals 2 with Regeneration: " + g.getHealth());
				g.removeEffect(MobEffects.REGENERATION);

				// Managift: the giver pays 20, the ally gets three quarters of it.
				Spellbooks.setMana(p, 100.0F);
				Spellbooks.setMana(g, 0.0F);
				float had = Spellbooks.mana(p);
				check(had >= 30.0F, "The giver has mana to give: " + had);
				apply(p, Runes.MANAGIFT, List.of(g), g.position());
				float paid = had - Spellbooks.mana(p);
				check(between(paid, 20.0F, 30.0F), "Managift costs the giver 20: " + paid);
				check(near(Spellbooks.mana(g), paid * 0.75F), "Managift gives the ally three quarters of it: " + Spellbooks.mana(g));

				// Shrug Off lifts the harmful effect with the most time left.
				g.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 400, 0));
				g.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
				apply(p, Runes.SHRUG_OFF, List.of(g), g.position());
				check(!g.hasEffect(MobEffects.SLOWNESS) && g.hasEffect(MobEffects.WEAKNESS), "Shrug Off lifts the longest harmful effect only");
				g.removeEffect(MobEffects.WEAKNESS);

				// Hexguard refuses the next harmful effect, then is spent.
				apply(p, Runes.HEXGUARD, List.of(g), g.position());
				check(WardState.has("hexguard", g), "Hexguard is on the ally");
				g.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
				check(!g.hasEffect(MobEffects.POISON) && !WardState.has("hexguard", g), "Hexguard refuses one poison and is spent");
				g.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
				check(g.hasEffect(MobEffects.POISON), "A second poison lands once Hexguard is spent");
				g.removeEffect(MobEffects.POISON);

				// Ironhold: no single blow deals more than 4.
				g.setHealth(20.0F);
				g.damageCooldownTime = 0;
				apply(p, Runes.IRONHOLD, List.of(g), g.position());
				g.hurtServer(level, level.damageSources().generic(), 15.0F);
				check(between(20.0F - g.getHealth(), 4.0F, 6.0F), "Ironhold caps a 15 blow at 4: " + g.getHealth());
			});

			// Guardlink: the ally takes 60%, and the guardian takes 40% a tick later.
			server.runOnServer(s -> {
				ServerPlayer p = NextSignatureNative.player(s);
				ServerPlayer g = guest(s);
				ServerLevel level = p.level();
				p.removeEffect(MobEffects.RESISTANCE);
				p.setHealth(20.0F);
				g.setHealth(20.0F);
				g.damageCooldownTime = 0;
				p.damageCooldownTime = 0;
				// Ironhold's 6 seconds may still run, so the blow is one it leaves alone.
				apply(p, Runes.GUARDLINK, List.of(g), g.position());
				check(WardState.has("guardlink", g), "Guardlink binds the ally to the caster");
				g.hurtServer(level, level.damageSources().generic(), 4.0F);
				check(near(g.getHealth(), 17.6F), "The linked ally takes 60% of a 4 blow: " + g.getHealth());
			});
			c.waitTicks(3);
			server.runOnServer(s -> {
				ServerPlayer p = NextSignatureNative.player(s);
				check(near(p.getHealth(), 18.4F), "The guardian takes the other 40%: " + p.getHealth());
			});

			// Crowd control on monsters: Pacify, Taunt, Hobble, Nudge.
			server.runOnServer(s -> {
				ServerPlayer p = NextSignatureNative.player(s);
				Mob calm = NextSignatureNative.foe(p, 4.5, 6.5);
				apply(p, Runes.PACIFY, List.of(calm), calm.position());
				check(WardState.has("calmed", calm), "Pacify calms the husk");
				calm.setTarget(p);
				check(calm.getTarget() == null, "A pacified husk refuses a target");

				Mob angry = NextSignatureNative.foe(p, -4.5, 6.5);
				apply(p, Runes.TAUNT, List.of(angry), angry.position());
				check(angry.getTarget() == p && WardState.has("taunted", angry), "Taunt turns the husk on the caster");
				check(p.hasEffect(MobEffects.RESISTANCE), "Taunt guards the taunter");

				Mob slow = NextSignatureNative.foe(p, 0.5, 9.5);
				apply(p, Runes.HOBBLE, List.of(slow), slow.position());
				MobEffectInstance hobbled = slow.getEffect(MobEffects.SLOWNESS);
				check(hobbled != null && hobbled.getAmplifier() == 2, "Hobble gives Slowness III");
				check(near(slow.getHealth(), slow.getMaxHealth()), "Hobble does no harm");

				Mob pushed = NextSignatureNative.foe(p, 0.5, 4.5);
				pushed.setDeltaMovement(Vec3.ZERO);
				apply(p, Runes.NUDGE, List.of(pushed), pushed.position());
				Vec3 v = pushed.getDeltaMovement();
				check(v.z > 0.3 && near(pushed.getHealth(), pushed.getMaxHealth()), "Nudge pushes the husk away without harm: " + v);
			});

			// Villagers: Hearthguard cuts a monster's blow to 40%, Tend heals 8.
			server.runOnServer(s -> {
				ServerPlayer p = NextSignatureNative.player(s);
				ServerLevel level = p.level();
				var made = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
				check(made != null, "Villager fixture");
				LivingEntity villager = made;
				((Mob) villager).setNoAi(true);
				villager.snapTo(8.5, 101, 0.5, 0, 0);
				level.addFreshEntity(villager);
				Mob husk = NextSignatureNative.foe(p, 10.5, 0.5);
				apply(p, Runes.HEARTHGUARD, List.of(villager), villager.position());
				check(WardState.has("hearthguard", villager), "Hearthguard is on the villager");
				float before = villager.getHealth();
				villager.hurtServer(level, level.damageSources().mobAttack(husk), 10.0F);
				check(near(before - villager.getHealth(), 4.0F), "Hearthguard cuts a husk's 10 blow to 4: " + (before - villager.getHealth()));
				villager.setHealth(5.0F);
				apply(p, Runes.TEND, List.of(villager), villager.position());
				check(between(villager.getHealth() - 5.0F, 8.0F, 12.0F), "Tend heals the villager for 8: " + villager.getHealth());
				husk.discard();
			});

			// Places: Sanctuary forbids spawns inside, Blastward keeps an explosion from breaking the floor.
			server.runOnServer(s -> {
				ServerPlayer p = NextSignatureNative.player(s);
				ServerLevel level = p.level();
				apply(p, Runes.SANCTUARY, List.of(), new Vec3(-6.5, 101, -6.5));
				check(WardState.forbidsSpawn(level, -5.0, 101, -5.0), "No monster spawns inside a Sanctuary");
				check(!WardState.forbidsSpawn(level, 18.0, 101, 18.0), "Sanctuary leaves the world outside alone");

				BlockPos floor = new BlockPos(15, 100, 15);
				apply(p, Runes.BLASTWARD, List.of(), Vec3.atCenterOf(floor.above()));
				check(WardState.sparesBlock(level, floor), "Blastward spares the floor");
				level.explode(null, 15.5, 101.0, 15.5, 3.0F, Level.ExplosionInteraction.TNT);
				check(level.getBlockState(floor).is(Blocks.STONE_BRICKS), "An explosion inside Blastward breaks no block");
			});
			c.waitTicks(2);
		}
	}

	private static ServerPlayer guest(MinecraftServer s) {
		ServerPlayer g = s.getPlayerList().getPlayerByName(GUEST);
		if (g == null) {
			for (var e : NextSignatureNative.player(s).level().players()) {
				if (e.getScoreboardName().equals(GUEST)) {
					return e;
				}
			}
		}
		check(g != null, "The allied guest is present");
		return g;
	}

	private static void apply(ServerPlayer p, RuneDef rune, List<LivingEntity> targets, Vec3 at) {
		NextSignatureNative.apply(new Cast(p), rune, targets, at);
	}

	private static boolean between(float x, float low, float high) {
		return x > low - 0.05F && x < high + 0.05F;
	}

	private static boolean near(float a, float b) {
		return Math.abs(a - b) < 0.05F;
	}
}
