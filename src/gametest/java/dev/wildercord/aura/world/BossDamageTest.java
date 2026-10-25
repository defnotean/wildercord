package dev.wildercord.aura.world;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
/** A boss's blow on a player in full diamond with Protection IV lands several times what an ordinary mob's blow of the same strength does. */
public final class BossDamageTest implements FabricClientGameTest {
	private static void check(boolean x, String why) { if (!x) throw new AssertionError(why); }
	private static ServerPlayer p(MinecraftServer s) { return s.getPlayerList().getPlayers().getFirst(); }
	private static Mob mob(MinecraftServer s, String id) {
		var type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(id));
		return (Mob) s.overworld().getEntities(type, e -> true).getFirst();
	}
	/** What a 10-damage blow from {@code id}'s creature takes off the player. */
	private static float taken(MinecraftServer s, String id) {
		var p = p(s);
		p.setHealth(p.getMaxHealth());
		// The hit cooldown in hurtServer reads damageCooldownTime, which setInvulnerableTime leaves alone.
		p.setInvulnerableTime(0);
		p.damageCooldownTime = 0;
		float before = p.getHealth();
		p.hurtServer(s.overworld(), s.overworld().damageSources().mobAttack(mob(s, id)), 10);
		return before - p.getHealth();
	}
	/** What the player's own {@code method} defence leaves of a 10-damage blow. */
	private static float absorbed(ServerPlayer p, net.minecraft.world.damagesource.DamageSource source, String method) {
		try {
			var m = net.minecraft.world.entity.LivingEntity.class.getDeclaredMethod(method, net.minecraft.world.damagesource.DamageSource.class, float.class);
			m.setAccessible(true);
			return (float) m.invoke(p, source, 10f);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("No " + method, e);
		}
	}
	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			var server = w.getServer();
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule natural_health_regeneration false");
			server.runCommand("difficulty normal");
			server.runCommand("fill -8 100 -8 8 100 8 stone");
			server.runCommand("fill -8 101 -8 8 106 8 air");
			server.runOnServer(s -> {
				var p = p(s);
				p.setGameMode(GameType.SURVIVAL);
				p.teleportTo(0.5, 101, 0.5);
			});
			for (String slot : new String[]{"head helmet", "chest chestplate", "legs leggings", "feet boots"}) {
				String[] part = slot.split(" ");
				server.runCommand("item replace entity @a armor." + part[0] + " with diamond_" + part[1] + "[enchantments={protection:4}]");
			}
			server.runCommand("summon zombie 5 101 0 {NoAI:1b,PersistenceRequired:1b}");
			server.runCommand("summon wildercord:cinder_warden -5 101 0 {NoAI:1b,PersistenceRequired:1b}");
			c.waitTicks(5);
			server.runOnServer(s -> {
				check(BossDamage.boss(mob(s, "wildercord:cinder_warden")), "The Cinder Warden counts as a boss");
				check(!BossDamage.boss(mob(s, "minecraft:zombie")), "A zombie does not count as a boss");
				var bossHit = s.overworld().damageSources().mobAttack(mob(s, "wildercord:cinder_warden"));
				float through = BossDamage.pierce(p(s), bossHit, 10, 0);
				check(Math.abs(through - 10 * BossDamageRules.PIERCE) < 0.01f, "A boss's blow goes partly through what a defence stops: " + through);
				float armoured = absorbed(p(s), bossHit, "getDamageAfterArmorAbsorb"), shielded = absorbed(p(s), bossHit, "getDamageAfterMagicAbsorb");
				var plainHit = s.overworld().damageSources().mobAttack(mob(s, "minecraft:zombie"));
				for (var defence : new String[] {"getDamageAfterArmorAbsorb", "getDamageAfterMagicAbsorb"}) {
					float plain = absorbed(p(s), plainHit, defence), boss = absorbed(p(s), bossHit, defence), expected = plain + (10 - plain) * (float) BossDamageRules.PIERCE;
					check(plain < 10 && Math.abs(boss - expected) < 0.01f, defence + " lets a boss's blow partly through: " + boss + " of 10, against " + plain + " for a zombie (expected " + expected + ")");
				}
				float zombie = taken(s, "minecraft:zombie"), warden = taken(s, "wildercord:cinder_warden");
				check(zombie > 0 && zombie < 1.5f, "An ordinary 10-damage blow barely scratches diamond and Protection IV: " + zombie);
				check(warden >= 4f && warden <= 6.5f, "A boss's 10-damage blow lands hard through diamond and Protection IV: " + warden + " (armour leaves " + armoured + ", Protection leaves " + shielded + " of 10)");
				check(warden > zombie * 3, "A boss's blow lands several times an ordinary mob's: " + warden + " against " + zombie);
			});
		}
	}
}
