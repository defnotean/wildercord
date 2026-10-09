package dev.wildercord.party;

import com.mojang.authlib.GameProfile;
import dev.wildercord.mixin.ProjectileOwnerAccessor;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;

import java.util.UUID;

/** A real removed shooter leaves only a saved UUID; collision and final damage must both reject its allied arrow. */
public final class PartyOfflineProjectileTest implements FabricClientGameTest {
	private static final UUID SHOOTER = new UUID(0x40A11E, 1);
	private static final UUID OUTSIDER = new UUID(0x40A11E, 2);

	private static final class Shooter extends FakePlayer {
		Shooter(ServerLevel level) {
			super(level, new GameProfile(SHOOTER, "OfflineArcher"));
		}
	}

	private static final class ImpactArrow extends Arrow {
		ImpactArrow(ServerLevel level, LivingEntity shooter) {
			super(level, shooter, new ItemStack(Items.ARROW), null);
		}

		ImpactArrow(ServerLevel level, UUID savedOwner) {
			super(EntityTypes.ARROW, level);
			setOwner(EntityReference.<Entity>of(savedOwner));
		}

		void hit(Entity target) {
			onHit(new EntityHitResult(target));
		}
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(30);
			world.getServer().runOnServer(server -> {
				var target = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = target.level();
				target.setGameMode(GameType.SURVIVAL);
				target.clearFire();
				target.removeEffect(MobEffects.POISON);
				Shooter shooter = new Shooter(level);
				level.addNewPlayer(shooter);
				PartyRules rules = Parties.session(server).rules;
				long now = level.getGameTime();
				check(rules.invite(SHOOTER, target.getUUID(), now) == PartyRules.Result.OK, "Shooter offers membership");
				check(rules.accept(target.getUUID(), SHOOTER, now) == PartyRules.Result.OK, "Target consents to membership");

				ImpactArrow arrow = new ImpactArrow(level, shooter);
				arrow.igniteForTicks(200);
				arrow.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
				check(arrow.getOwner() == shooter, "Arrow initially resolves its actual shooter");
				rules.disconnect(SHOOTER);
				shooter.discard();
				check(arrow.getOwner() == null, "Removed shooter is genuinely unresolved, not a cached live entity");
				var reference = ((ProjectileOwnerAccessor) (Object) arrow).wildercord$ownerReference();
				check(reference != null && reference.getUUID().equals(SHOOTER), "Saved owner reference retains authenticated UUID");
				check(Parties.sameParty(arrow, target) && Parties.blocksHarm(arrow, target), "Offline-owned arrow keeps party protection");

				var damage = level.damageSources().arrow(arrow, arrow.getOwner());
				check(damage.getEntity() == null && damage.getDirectEntity() == arrow, "Damage has only its direct projectile identity");
				check(Parties.blocksDamage(target, damage), "Final damage resolves the saved projectile owner UUID");
				float health = target.getHealth();
				check(!target.hurtServer(level, damage, 4) && target.getHealth() == health, "Offline-owned direct damage is rejected");
				arrow.hit(target);
				check(arrow.isRemoved(), "Blocked collision terminates the arrow rather than repeatedly hitting");
				check(target.getHealth() == health && !target.isOnFire() && !target.hasEffect(MobEffects.POISON),
					"Offline flaming poison arrow causes no health, fire or status mutation");

				ImpactArrow own = new ImpactArrow(level, target);
				check(!Parties.blocksHarm(own, target), "Same-owner self-harm policy is unchanged");
				ImpactArrow stranger = new ImpactArrow(level, OUTSIDER);
				check(stranger.getOwner() == null && !Parties.blocksHarm(stranger, target), "Unrelated offline shooter gains no party exemption");
				ImpactArrow left = new ImpactArrow(level, SHOOTER);
				rules.leave(SHOOTER);
				check(!Parties.blocksHarm(left, target), "A saved arrow UUID does not retain protection after leaving its party");
				own.discard();
				stranger.discard();
				left.discard();
			});
		}
	}

	private static void check(boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}
}
