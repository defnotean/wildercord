package dev.wildercord.aura;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.StoneHingeMasterReleaseChecks;
import dev.wildercord.cast.CastEngine;
import dev.wildercord.cast.CastReceiptWardProbe;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Targets;
import dev.wildercord.cast.VoidTime;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.gametest.stonehinge.StoneHingeImpulseProbe;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/**
 * Receipt/source feasibility on the real connected Survival body. No synthetic PlayerList, CHARGE, or form unlock.
 * This suite does NOT certify six-step controlled movement: that gate remains closed in the decision artifact.
 */
public final class StoneHingeFeasibilityTest implements FabricClientGameTest {
	private static final float HEALTH = 200;
	private static boolean registered;
	private static ServerPlayer callbackTarget;
	private static int callbackMode;
	private static boolean inside;
	private static final class ExpectedProbeFailure extends RuntimeException {}
	private ServerPlayer player;
	private Mob attacker;
	private Vec3 origin;

	@Override public void runTest(ClientGameTestContext context) {
		registerCallbacks();
		try (var world = context.worldBuilder().create()) {
			try {
				world.getServer().runOnServer(server -> Wildercord.LOGGER.info(
					"WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.aura.StoneHingeFeasibilityTest\",\"seed\":\"{}\"}", server.overworld().getSeed()));
				context.waitTicks(40);
				for (String command : List.of("difficulty normal", "gamerule spawn_mobs false", "gamerule natural_health_regeneration false"))
					world.getServer().runCommand(command);
				world.getServer().runOnServer(server -> {
					player = server.getPlayerList().getPlayers().getFirst();
					check(player.connection != null && player.connection.player == player && player.connection.hasClientLoaded(), "Recipient is the real loaded connected body");
					origin = new Vec3(player.getBlockX() + .5, 181, player.getBlockZ() + .5);
					for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
						player.level().setBlockAndUpdate(BlockPos.containing(origin).offset(x, -1, z), Blocks.STONE.defaultBlockState());
						for (int y = 0; y < 7; y++) player.level().setBlockAndUpdate(BlockPos.containing(origin).offset(x, y, z), Blocks.AIR.defaultBlockState());
					}
					player.setGameMode(GameType.SURVIVAL);
					player.teleportTo(player.level(), origin.x, origin.y, origin.z, Set.of(), 0, 0, false);
					attacker = EntityTypes.ZOMBIE.create(player.level(), EntitySpawnReason.COMMAND);
					check(attacker != null, "Native melee mob can be created");
					attacker.setNoAi(true); attacker.setNoGravity(true); attacker.setPersistenceRequired(); attacker.setPermanentlyInvulnerable(true);
					attacker.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8);
					if (attacker.getAttribute(Attributes.ATTACK_KNOCKBACK) != null) attacker.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(0);
					attacker.snapTo(origin.x, origin.y, origin.z + 1.5, 180, 0); player.level().addFreshEntity(attacker);
				});
				context.waitTicks(5);
				world.getServer().waitFor(server -> player.onGround(), 40);
				world.getServer().runOnServer(server -> {
					StoneHingeImpulseProbe.assertIdle();
					prepare();
					check(player.onGround(), "Grounded baseline comes from native client movement");
					var baseline = melee(); assertAccepted("bare native mob melee", baseline);
					var nativeImpulse = baseline.impulses().getFirst();
					check(nativeImpulse.grounded() && nativeImpulse.after().y > nativeImpulse.before().y, "Real grounded native knockback retains its own vertical lift");
					geometry(baseline);

					prepare(); callbackTarget = player; callbackMode = 5;
					try {
						melee();
						throw new AssertionError("Native damage callback must throw the scope-cleanup witness");
					} catch (ExpectedProbeFailure expected) {
						StoneHingeImpulseProbe.assertIdle();
					} finally { callbackTarget = null; callbackMode = 0; }
					prepare(); assertAccepted("native melee after exceptional scope cleanup", melee());

					prepare(); player.setAbsorptionAmount(32);
					var absorption = melee(); assertAccepted("absorption-only melee", absorption);
					check(absorption.onlyHit().healthLost() == 0 && absorption.onlyHit().absorptionLost() > 0 && player.getHealth() == HEALTH,
						"Accepted absorption loss is observed despite unchanged health");

					prepare(); Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
					player.setAttached(WildercordAttachments.CIRCLES, 3); Spellbooks.setMana(player, 100);
					var skin = melee(); assertAccepted("Mana Skin native wound", skin);
					check(skin.onlyHit().healthLost() > HEALTH - player.getHealth() && Spellbooks.mana(player) < 100,
						"Pre-rebate health loss survives the real paid Mana Skin restoration");

					prepare(); callbackTarget = player; callbackMode = 1;
					var restored = melee(); callbackTarget = null; assertAccepted("AFTER_DAMAGE restoration", restored);
					check(player.getHealth() == HEALTH && restored.onlyHit().healthLost() > 0, "A healing callback cannot erase the native wound receipt");

					prepare(); player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(.5);
					var half = melee(); assertAccepted("partial knockback resistance", half);
					check(close(half.impulses().getFirst().horizontalMagnitude(), nativeImpulse.horizontalMagnitude() * .5),
						"Receipt contains actual post-resistance impulse, never the pre-resistance requested strength");

					prepare(); player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
					var resistant = melee(); refused("full knockback resistance", resistant);
					check(resistant.onlyHit().healthLost() > 0 && resistant.impulses().isEmpty(), "Damage with no native velocity write cannot buy lateral movement");

					prepare(); player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80, 4));
					var zeroWound = melee(); refused("Resistance V without accepted loss", zeroWound);
					check(zeroWound.onlyHit().healthLost() == 0 && zeroWound.onlyHit().absorptionLost() == 0,
						"hurtServer true or a native impulse alone never authorizes the catch");

					prepare(); player.setPermanentlyInvulnerable(true);
					var invulnerable = melee(); refused("native invulnerability", invulnerable);
					check(invulnerable.impulses().isEmpty(), "Rejected damage produces no captured native knockback");

					prepare(); player.setYRot(180);
					var rear = melee(); refused("rear native melee", rear);
					check(rear.onlyHit().healthLost() > 0 && !rear.impulses().isEmpty(), "Rear damage and knockback remain native");

					prepare();
					var spoof = StoneHingeImpulseProbe.capture(player, () -> player.hurtServer(player.level(), player.damageSources().mobAttack(attacker), 8));
					refused("mob damage source outside a native melee callsite", spoof);
					check(spoof.onlyHit().healthLost() > 0 && !spoof.onlyHit().melee(), "Damage type and close living owner alone cannot claim melee provenance");

					prepare();
					var aura = StoneHingeImpulseProbe.capture(player, () -> player.hurtServer(player.level(), player.level().damageSources().source(Aura.DAMAGE, attacker, attacker), 8));
					refused("close unclassified Aura source", aura);
					check(aura.onlyHit().healthLost() > 0 && !aura.onlyHit().melee(), "Close Aura fields/projected sources stay outside melee provenance");

					for (int mode : List.of(2, 3)) {
						prepare(); callbackTarget = player; callbackMode = mode;
						var nested = melee(); callbackTarget = null; refused(mode == 2 ? "nested other source" : "nested reused exact source", nested);
						check(nested.hits().size() == 2 && nested.hits().stream().allMatch(StoneHingeImpulseProbe.Hit::contaminated),
							"Nested scopes cannot inherit or lend accepted damage/impulse receipts");
					}
					prepare(); callbackTarget = player; callbackMode = 4;
					var extraImpulse = melee(); callbackTarget = null; refused("extra callback impulse", extraImpulse);
					check(extraImpulse.impulses().size() == 2 && extraImpulse.impulses().getLast().hit() == null,
						"The callback write cannot borrow the native default impulse's association, even with the exact same source");

					prepare(); attacker.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4);
					var cooldownSeed = melee(); assertAccepted("native cooldown seed", cooldownSeed);
					float beforeStronger = player.getHealth();
					attacker.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8);
					// Deliberately retain the actual native cooldown from the first hit. No readyToHurt/reset between hits.
					callbackTarget = player; callbackMode = 4;
					StoneHingeImpulseProbe.Trial cooldownCallback;
					try { cooldownCallback = melee(); } finally { callbackTarget = null; callbackMode = 0; }
					refused("stronger cooldown hit with sole same-source callback impulse", cooldownCallback);
					var strongerHit = cooldownCallback.onlyHit(); var callbackImpulse = cooldownCallback.impulses().getFirst();
					check(strongerHit.returned() && strongerHit.observed() && strongerHit.melee() && !strongerHit.contaminated() && !strongerHit.lethal()
						&& close(strongerHit.healthLost(), 4) && close(beforeStronger - player.getHealth(), 4),
						"The stronger actual melee hit accepts only the native cooldown damage difference");
					check(cooldownCallback.impulses().size() == 1 && callbackImpulse.source() == strongerHit.source()
						&& callbackImpulse.horizontalMagnitude() > 0 && player.getDeltaMovement().equals(callbackImpulse.after()) && callbackImpulse.hit() == null,
						"Its only positive native write is the exact-source callback, which is not the missing default melee impulse");

					prepare();
					ItemStack knockbackSword = new ItemStack(Items.DIAMOND_SWORD);
					knockbackSword.enchant(player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.KNOCKBACK), 1);
					attacker.setItemSlot(EquipmentSlot.MAINHAND, knockbackSword);
					var nativeExtra = melee(); refused("native caller-supplied weapon knockback", nativeExtra);
					check(nativeExtra.hits().size() == 1 && nativeExtra.impulses().size() == 2 && nativeExtra.impulses().getLast().hit() == null,
						"Mob.doHurtTarget adds real enchanted knockback after hurtServer returned; no earlier receipt may borrow that later write");
					attacker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

					prepare(); player.setHealth(1); player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
					var totem = melee(); refused("lethal wound saved by native totem", totem);
					check(totem.onlyHit().lethal() && player.isAlive() && player.getOffhandItem().isEmpty(), "A real consumed totem does not turn a lethal wound into a nonlethal catch");
				});
				defences(context, world);
				world.getServer().runOnServer(server -> { prepare(); attacker.discard(); attacker = null; });
				StoneHingeMasterReleaseChecks.run(context, world, player, origin);
				Wildercord.LOGGER.info("STONE_HINGE_FEASIBILITY receipt_gate=observed movement_gate=NOT_PROVEN gameplay_enabled=false");
			} finally {
				world.getServer().runOnServer(server -> {
					callbackTarget = null; callbackMode = 0; inside = false;
					try { StoneHingeImpulseProbe.assertIdle(); }
					finally {
						StoneHingeImpulseProbe.clear();
						if (attacker != null) { attacker.discard(); attacker = null; }
					}
				});
			}
		} finally { player = null; attacker = null; origin = null; }
	}

	private void defences(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			prepare(); player.teleportTo(player.level(), origin.x, origin.y, origin.z, Set.of(), 0, 0, false);
			player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
			player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.FLOW, 0, 40, 0));
		});
		context.getInput().holdKey(options -> options.keyShift);
		try {
			world.getServer().waitFor(server -> player.isShiftKeyDown(), 40);
			world.getServer().runOnServer(server -> {
				check(AuraGuard.raise(player) && AuraGuard.perfectNow(player), "Real connected input raises a real perfect Aura Guard");
				var guard = melee(); refused("full native perfect guard", guard);
				check(player.getHealth() == HEALTH && guard.impulses().isEmpty(), "Full guard neither wounds nor funds an impulse");
				player.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE);
			});
		} finally {
			context.getInput().releaseKey(options -> options.keyShift);
		}
		context.waitTicks(5);
		world.getServer().waitFor(server -> player.onGround() && !player.isShiftKeyDown(), 40);
		world.getServer().runOnServer(server -> {
			prepare(); player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("gale", AuraRules.FORM, 0, 80, 0));
			player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
			check(AuraStep.step(player), "Native grounded Aura Step is paid through its real entrypoint");
			var dodge = melee(); refused("native Aura Step untouchability", dodge);
			check(player.getHealth() == HEALTH && dodge.impulses().isEmpty(), "A genuine dodge cannot earn the catch");
		});
		context.waitTicks(Math.max(AuraRules.STEP_TICKS, AuraRules.STEP_GUARD_TICKS) + 2);
		world.getServer().runOnServer(server -> {
			prepare(); player.teleportTo(player.level(), origin.x, origin.y, origin.z, Set.of(), 0, 0, false);
			CastEngine.cast(player, SpellCompiler.compile(List.of(Runes.SELF, Runes.FORESIGHT)).root());
			Vec3 before = player.position();
			var ward = melee(); refused("actual full Foresight ward", ward);
			check(player.getHealth() == HEALTH && ward.impulses().isEmpty() && player.position().distanceToSqr(before) > .5,
				"The actual ward buys its own dodge; it cannot also fund a Stone Hinge step");
		});
		world.getServer().waitFor(server -> CastReceiptWardProbe.active(player).isEmpty(), 1205);
		world.getServer().runOnServer(server -> {
			prepare(); player.teleportTo(player.level(), origin.x, origin.y, origin.z, Set.of(), 0, 0, false);
			CastEngine.cast(player, SpellCompiler.compile(List.of(Runes.SELF, Runes.FORESIGHT)).root());
			attacker.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(20);
			var partial = melee(); refused("Foresight reentrant same-source remainder", partial);
			check(partial.hits().size() == 2 && partial.hits().stream().anyMatch(hit -> hit.healthLost() > 0),
				"A real partially stopping ward uses a nested exact-source wound and remains conservatively uncatchable");
			attacker.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8);
		});
		world.getServer().waitFor(server -> CastReceiptWardProbe.active(player).isEmpty(), 1205);
		world.getServer().runOnServer(server -> {
			prepare(); player.teleportTo(player.level(), origin.x, origin.y, origin.z, Set.of(), 0, 0, false);
			CastEngine.cast(player, SpellCompiler.compile(List.of(Runes.SELF, Runes.ANCHOR)).root());
			check(VoidTime.anchored(player) && player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 1, "A real self-cast Anchor owns the native resistance attribute");
			var anchored = melee(); refused("actual Anchor", anchored);
			check(anchored.onlyHit().healthLost() > 0 && anchored.impulses().isEmpty(), "Anchor leaves the wound but refuses native impulse and lateral credit");
		});
		world.getServer().waitFor(server -> !VoidTime.anchored(player), 1205);
		world.getServer().runOnServer(server -> {
			prepare(); player.setHealth(2);
			CastEngine.cast(player, SpellCompiler.compile(List.of(Runes.SELF, Runes.REVERSAL)).root());
			var reversal = melee(); refused("lethal wound saved by Reversal", reversal);
			check(reversal.onlyHit().lethal() && player.isAlive() && player.getHealth() > 2, "A real Reversal cannot conceal the lethal native health write");
		});
		world.getServer().runOnServer(server -> {
			prepare(); player.teleportTo(player.level(), origin.x, origin.y + 5, origin.z, Set.of(), 0, 0, false);
		});
		context.waitTicks(3);
		world.getServer().waitFor(server -> !player.onGround() && player.fallDistance > 0, 15);
		world.getServer().runOnServer(server -> {
			Effects.readyToHurt(player); player.setYRot(0); attacker.snapTo(player.getX(), player.getY(), player.getZ() + 1.5, 180, 0);
			double fall = player.fallDistance;
			var falling = melee(); assertAccepted("genuinely falling connected recipient", falling);
			var impulse = falling.impulses().getFirst();
			check(!impulse.grounded() && impulse.after().y == impulse.before().y && player.fallDistance == fall,
				"Observation preserves native falling Y and existing fall distance exactly");
		});
		context.waitTicks(20);
	}

	private void prepare() {
		check(player.level().getServer().getPlayerList().getPlayer(player.getUUID()) == player && player.connection.player == player,
			"Every case still uses the actual connected Survival body");
		player.setGameMode(GameType.SURVIVAL); player.setPermanentlyInvulnerable(false); player.removeAllEffects();
		player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH); player.setHealth(HEALTH);
		player.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(64); player.setAbsorptionAmount(0);
		player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);
		for (EquipmentSlot slot : List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET))
			player.setItemSlot(slot, ItemStack.EMPTY);
		player.setAttached(AuraAttachments.AURA, AuraAttachments.Data.NONE); player.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE);
		player.setAttached(WildercordAttachments.CIRCLES, 0); player.setAttached(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
		Spellbooks.setCord(player, ItemStack.EMPTY); player.setNoGravity(false); player.setYRot(0); player.setXRot(0);
		player.setDeltaMovement(new Vec3(.12, 0, .06)); Effects.readyToHurt(player);
		if (attacker != null) attacker.snapTo(player.getX(), player.getY(), player.getZ() + 1.5, 180, 0);
	}
	private StoneHingeImpulseProbe.Trial melee() {
		check(attacker.isAlive() && !attacker.isRemoved() && Targets.canHarm(attacker, player), "The ordinary hostile melee source is alive and lawful");
		return StoneHingeImpulseProbe.capture(player, () -> attacker.doHurtTarget(player.level(), player));
	}
	private void geometry(StoneHingeImpulseProbe.Trial trial) {
		Vec3 from = player.position(), to = from.add(-.3, 0, 0), velocity = player.getDeltaMovement();
		double fall = player.fallDistance; boolean grounded = player.onGround();
		check(WallTurn.swept(player, from, to, false), "The real whole-body first lateral sweep has room in the open lane");
		BlockPos obstacle = BlockPos.containing(origin).offset(-1, 0, 0);
		for (var state : List.of(Blocks.STONE.defaultBlockState(), Blocks.LAVA.defaultBlockState(), Blocks.POWDER_SNOW.defaultBlockState())) {
			player.level().setBlockAndUpdate(obstacle, state);
			check(!WallTurn.swept(player, from, to, false), "First sweep refuses a real wall or hazard before consuming any native impulse: " + state);
			check(player.getDeltaMovement().equals(velocity) && player.position().equals(from) && player.fallDistance == fall && player.onGround() == grounded,
				"Refused first-step admission leaves all actual native motion/fall/ground state unchanged");
			player.level().setBlockAndUpdate(obstacle, Blocks.AIR.defaultBlockState());
		}
		check(trial.eligibleReceipt(), "Geometry checks neither erase nor consume the observed native impulse");
	}
	private void assertAccepted(String name, StoneHingeImpulseProbe.Trial trial) {
		check(trial.eligibleReceipt(), name + ": exact accepted nonlethal frontal melee and one actual positive impulse: " + trial.summary()); log(name, trial);
	}
	private void refused(String name, StoneHingeImpulseProbe.Trial trial) {
		Vec3 velocity = player.getDeltaMovement(), position = player.position(); double fall = player.fallDistance; boolean grounded = player.onGround();
		check(!trial.eligibleReceipt(), name + ": cannot admit a redirect: " + trial.summary());
		check(player.getDeltaMovement().equals(velocity) && player.position().equals(position) && player.fallDistance == fall && player.onGround() == grounded,
			"Receipt refusal never changes original native motion"); log(name, trial);
	}
	private static void registerCallbacks() {
		if (registered) return; registered = true;
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity != callbackTarget || inside || callbackMode != 2 && callbackMode != 3) return true;
			inside = true;
			try { entity.hurtServer(callbackTarget.level(), callbackMode == 3 ? source : callbackTarget.damageSources().generic(), 2); }
			finally { inside = false; }
			return true;
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (entity != callbackTarget || inside) return;
			if (callbackMode == 1) entity.setHealth(HEALTH);
			if (callbackMode == 4) entity.knockback(.2, 1, 0, source, 0);
			if (callbackMode == 5) throw new ExpectedProbeFailure();
		});
	}
	private static void log(String name, StoneHingeImpulseProbe.Trial trial) { Wildercord.LOGGER.info("STONE_HINGE_NATIVE case={} receiptEligible={} {}", name, trial.eligibleReceipt(), trial.summary()); }
	private static boolean close(double a, double b) { return Math.abs(a - b) < 1.0E-5; }
	private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
}
