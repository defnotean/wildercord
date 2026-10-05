package dev.wildercord.party;

import com.mojang.authlib.GameProfile;
import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.cast.Cast;
import dev.wildercord.cast.CastEngine;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.RuneBolt;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.Spirits;
import dev.wildercord.cast.Statuses;
import dev.wildercord.cast.Targets;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/** Runtime checks for damage, statuses, delayed ownership, pets and beneficial/self exceptions. */
public final class PartyMutationSafetyTest implements FabricClientGameTest {
	private static final class Ally extends FakePlayer {
		Ally(ServerLevel level) {
			super(level, new GameProfile(UUID.nameUUIDFromBytes("party-mutation-ally".getBytes(StandardCharsets.UTF_8)), "PartyAlly"));
		}

		@Override
		public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
			return false;
		}
	}

	private Ally ally;
	private Arrow flamingArrow;
	private float allyHealth;
	private boolean delayedRan;

	private static void check(boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
			world.getServer().runCommand("gamerule minecraft:natural_health_regeneration false");
			world.getServer().runCommand("fill -4 99 -4 4 99 4 minecraft:stone");
			world.getServer().runCommand("fill -4 100 -4 4 106 9 minecraft:air");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = player.level();
				player.setGameMode(GameType.SURVIVAL);
				player.setHealth(player.getMaxHealth());
				ally = new Ally(level);
				ally.setGameMode(GameType.SURVIVAL);
				ally.snapTo(0.5, 100, 0.5, 0, 0);
				level.addNewPlayer(ally);
				PartyRules rules = Parties.session(server).rules;
				long now = level.getGameTime();
				check(rules.invite(player.getUUID(), ally.getUUID(), now) == PartyRules.Result.OK, "Leader can offer an invitation");
				check(!Parties.sameParty(player, ally), "An invitation alone grants no alliance");
				// The relationship changes after this task is queued. Admission must use the current party.
				Effects.withSource(player, () -> Scheduler.later(2, () -> {
					delayedRan = true;
					assertMutationsBlocked(ally);
				}));
				check(rules.accept(ally.getUUID(), player.getUUID(), now) == PartyRules.Result.OK, "Only the recipient accepts membership");
				check(Parties.sameParty(player, ally), "Accepted membership is server-owned");
				check(!Targets.canHarm(player, ally) && Targets.canHelp(player, ally), "Party allies reject harm and admit help");
				float health = ally.getHealth();
				check(!ally.hurtServer(level, level.damageSources().playerAttack(player), 4), "Direct allied damage is rejected");
				check(ally.getHealth() == health, "Rejected damage does not remove health");
				var harmful = SpellCompiler.compile(List.of(Runes.BURST, Runes.FIRE, Runes.VENOM, Runes.CHILL)).root().groups.getFirst();
				CastEngine.onHit(new Cast(player), harmful, new Cast.Hit(List.of(ally), ally.position(), new Vec3(0, 0, 1), player.position(), null, null, false), null);
				check(ally.getHealth() == health && !ally.isOnFire() && !ally.hasEffect(MobEffects.POISON) && !ally.hasEffect(MobEffects.SLOWNESS),
					"A real multi-effect AoE hit preserves an ally's health and status");
				check(!ally.addEffect(new MobEffectInstance(MobEffects.POISON, 40), player), "Explicit allied hostile effect is vetoed");
				check(ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40), player), "Allied regeneration remains helpful");
				var tide = SpellCompiler.compile(List.of(Runes.TOUCH, Runes.TIDEBREATH)).root().groups.getFirst();
				Effects.withSource(player, () -> CastEngine.onHit(new Cast(player), tide,
					new Cast.Hit(List.of(ally), ally.position(), new Vec3(0, 0, 1), player.position(), null, null, false), null));
				check(ally.hasEffect(MobEffects.WATER_BREATHING) && Reactions.has(ally, Reactions.Mark.WET), "Tidebreath keeps its helpful buff and wetness inside a nested source scope");
				Effects.withSource(player, () -> assertMutationsBlocked(ally));
				check(Effects.applying() == null, "Scoped ownership is restored after applying effects");
				ArtKit.hold(player, ally, 30);
				ArtKit.chill(player, ally, 30, 1);
				check(!ally.hasEffect(MobEffects.SLOWNESS) && ally.getTicksFrozen() == 0, "Source-taking Aura controls cannot affect allies");
				var wolf = EntityTypes.WOLF.create(level, EntitySpawnReason.MOB_SUMMONED);
				check(wolf != null, "Summon fixture exists");
				wolf.tame(player);
				check(Parties.blocksHarm(wolf, ally), "Summon ownership resolves to its player party");
				check(!ally.hurtServer(level, level.damageSources().mobAttack(wolf), 4), "Summoned vanilla melee cannot bypass party safety");
				assertSpellCuts(player, ally);
				Effects.withSource(player, () -> player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10), player));
				check(player.hasEffect(MobEffects.WEAKNESS), "Self-cost debuffs are preserved");
				// A world hazard has no allied owner and must not grant blanket fire immunity.
				ally.igniteForTicks(20);
				check(ally.getRemainingFireTicks() > 0, "Unattributed environmental fire remains possible");
				ally.clearFire();
				ally.setTicksFrozen(0);
				allyHealth = ally.getHealth();
				flamingArrow = new Arrow(level, player, new ItemStack(Items.ARROW), null);
				flamingArrow.setPos(ally.getX(), ally.getY() + 1.0, ally.getZ() + 3.0);
				flamingArrow.setDeltaMovement(0, 0, -1.5);
				flamingArrow.setNoGravity(true);
				flamingArrow.igniteForTicks(200);
				level.addFreshEntity(flamingArrow);
			});
			context.waitTicks(4);
			world.getServer().runOnServer(server -> {
				check(delayedRan, "The delayed callback actually ran");
				check(!ally.hasEffect(MobEffects.POISON) && !ally.hasEffect(MobEffects.SLOWNESS), "Queued allied effects are harmless after acceptance");
				check(Effects.applying() == null, "Delayed ownership does not leak into another task");
				check(flamingArrow.isRemoved() || flamingArrow.getDeltaMovement().z >= 0, "Flaming arrow actually reached and bounced from its protected target");
				check(ally.getHealth() == allyHealth && !ally.isOnFire(), "An allied flaming arrow cannot damage or ignite its party member");
				flamingArrow.discard();
				ally.discard();
			});
		}
	}

	private static void assertSpellCuts(ServerPlayer cutter, Ally ally) {
		ServerLevel level = cutter.level();
		cutter.snapTo(2.5, 100, 0.5, 0, 0);
		var enemy = EntityTypes.PILLAGER.create(level, EntitySpawnReason.MOB_SUMMONED);
		check(enemy != null, "Hostile caster fixture exists");
		enemy.snapTo(2.5, 100, 7.5, 180, 0);
		enemy.setNoAi(true);
		var harmful = SpellCompiler.compile(List.of(Runes.BOLT, Runes.HARM)).root().groups.getFirst();
		var helpful = SpellCompiler.compile(List.of(Runes.BOLT, Runes.HEAL)).root().groups.getFirst();
		Vec3 front = cutter.getBoundingBox().getCenter().add(0, 0, 2);
		Vec3 incoming = new Vec3(0, 0, -1);
		Cast shared = new Cast(enemy);
		RuneBolt first = RuneBolt.launch(shared, harmful, null, front, incoming, false);
		RuneBolt sibling = RuneBolt.launch(shared, harmful, null, front.add(0.3, 0, 0), incoming, false);
		check(first != null && sibling != null, "Split bolt fixtures exist");
		check(first.swordCut(cutter) && first.isRemoved(), "A frontal hostile incoming bolt is severed");
		check(!sibling.isRemoved() && shared.alive(), "Cutting one bolt preserves its sibling and shared cast");
		sibling.discard();
		RuneBolt friendly = RuneBolt.launch(new Cast(ally), harmful, null, front, incoming, false);
		check(friendly != null && !friendly.swordCut(cutter) && !friendly.isRemoved(), "A party ally's bolt cannot be cut");
		friendly.discard();
		RuneBolt healing = RuneBolt.launch(new Cast(enemy), helpful, null, front, incoming, false);
		check(healing != null && !healing.swordCut(cutter), "A purely helpful payload cannot be cut even from a hostile caster");
		healing.discard();
		RuneBolt hidden = RuneBolt.launch(new Cast(enemy), harmful, null, front, incoming, false);
		BlockPos wall = BlockPos.containing(cutter.getEyePosition().add(0, 0, 1));
		var old = level.getBlockState(wall);
		level.setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
		try {
			check(hidden != null && !hidden.swordCut(cutter), "Solid cover prevents cutting a bolt through a wall");
		} finally {
			level.setBlock(wall, old, 3);
			if (hidden != null) hidden.discard();
		}
		RuneBolt rear = RuneBolt.launch(new Cast(enemy), harmful, null, cutter.getBoundingBox().getCenter().add(0, 0, -2), new Vec3(0, 0, 1), false);
		check(rear != null && !rear.swordCut(cutter), "Rear projectiles cannot be cut");
		rear.discard();
		RuneBolt leaving = RuneBolt.launch(new Cast(enemy), harmful, null, front, new Vec3(0, 0, 1), false);
		check(leaving != null && !leaving.swordCut(cutter), "Departing projectiles cannot be cut");
		leaving.discard();
		RuneBolt distant = RuneBolt.launch(new Cast(enemy), harmful, null, front.add(0, 0, 4), incoming, false);
		check(distant != null && !distant.swordCut(cutter), "A cut cannot exceed its server reach");
		distant.discard();
		enemy.discard();
	}

	private static void assertMutationsBlocked(Ally target) {
		int fire = target.getRemainingFireTicks();
		int frost = target.getTicksFrozen();
		Vec3 movement = target.getDeltaMovement();
		target.addEffect(new MobEffectInstance(MobEffects.POISON, 40));
		target.igniteForTicks(100);
		target.setTicksFrozen(100);
		Reactions.mark(target, Reactions.Mark.FROZEN, 30);
		Statuses.silence(target, 20);
		Spirits.hold(target, 20);
		ArtKit.lift(target, 1.0, 20);
		ArtKit.ignite(target, 100);
		check(!target.hasEffect(MobEffects.POISON) && !target.hasEffect(MobEffects.SLOWNESS), "Scoped party harm cannot add poison or a hold");
		check(target.getRemainingFireTicks() == fire && target.getTicksFrozen() == frost, "Scoped party harm cannot ignite or freeze");
		check(!Reactions.has(target, Reactions.Mark.FROZEN) && !Statuses.silenced(target), "Scoped party harm cannot add marks or cast locks");
		check(target.getDeltaMovement().equals(movement), "Scoped party harm cannot launch an ally");
	}
}
