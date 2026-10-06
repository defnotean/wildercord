package dev.wildercord.party;

import com.mojang.authlib.GameProfile;
import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.ArtRules;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.SparRules;
import dev.wildercord.aura.Stance;
import dev.wildercord.aura.arts.ArtFields;
import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.aura.arts.CrimsonArts;
import dev.wildercord.aura.arts.HollowArts;
import dev.wildercord.aura.arts.MethodArts;
import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Reactions;
import dev.wildercord.duel.DuelRules;
import dev.wildercord.duel.Duels;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Native field pulses, real duel completion and synchronous Fabric damage callbacks; no damage-admission bypasses. */
public final class ArtFieldMutationSafetyTest implements FabricClientGameTest {
	private static final Vec3 FEET = new Vec3(.5, 100, .5);
	private static final Vec3 MOTION = new Vec3(.31, .24, -.17);
	private static final String PROBE = "field_mutation_probe";
	private static Consumer<Hit> onDamage;
	private static Runnable beforeTick;
	private static boolean listening;
	private record Hit(LivingEntity target, DamageSource source, float damage) {}
	private static final class Guest extends FakePlayer {
		Guest(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "FieldGuest")); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
	}
	private Guest guest;
	private Mob witness;
	private boolean observed;
	private int mutations;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!listening) {
			listening = true;
			// The permanently registered listener retains no test, world, player or callback after each case.
			ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, base, damage, blocked) -> {
				Consumer<Hit> action = onDamage;
				if (action != null && damage > 0) action.accept(new Hit(target, source, damage));
			});
			ServerTickEvents.START_SERVER_TICK.register(server -> {
				Runnable action = beforeTick;
				if (action != null) action.run();
			});
		}
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
			world.getServer().runCommand("gamerule minecraft:natural_health_regeneration false");
			world.getServer().runCommand("fill -8 99 -8 8 99 12 minecraft:stone");
			world.getServer().runCommand("fill -8 100 -8 8 108 12 minecraft:air");
			try {
				duelEndsDuringRain(context, world);
				for (String transition : List.of("join", "disband", "end", "replace", "disconnect event", "clear")) {
					callbackTransition(context, world, transition);
				}
				collapseAfterJoin(context, world);
				nestedAndThrowingScopes(context, world, false);
				nestedAndThrowingScopes(context, world, true);
			} finally {
				onDamage = null;
				beforeTick = null;
				world.getServer().runOnServer(server -> cleanup(server.getPlayerList().getPlayers().getFirst()));
			}
		}
	}

	private void duelEndsDuringRain(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] admittedTicks = {0};
		String[] admissionFailure = {null};
		world.getServer().runOnServer(server -> {
			ServerPlayer owner = prepare(server, "crimson");
			guest = guest(owner, FEET.add(0, 0, 9));
			join(owner, guest);
			perform(owner, CrimsonArts.arts(), CrimsonArts.RED_RAIN);
			check(ArtFields.count(owner, CrimsonArts.RAIN) == 1, "Actual Red Rain performer opens its ordinary field");
			move(guest, FEET.add(0, 0, ArtRules.RAIN_AHEAD));
			guest.setHealth(SparRules.KNOCKOUT + .01F);
			var duel = Duels.startBout(owner, guest, new DuelRules.Terms(40, 0, 600, SparRules.KNOCKOUT, SparRules.KNOCKOUT, false), FEET, null);
			duel.tick(owner.level().getGameTime());
			check(ArtKit.harmable(owner, guest) && Parties.sameParty(owner, guest), "Fighting duellists are party allies and the pulse may initially hit");
			beforeTick = () -> {
				if (owner.isAlive() && owner.position().distanceToSqr(FEET) < 1 && guest.isAlive()
						&& Duels.inDuel(guest) && duel.fighting() && ArtKit.harmable(owner, guest)
						&& ArtFields.inside(owner, CrimsonArts.RAIN, guest)) {
					admittedTicks[0]++;
				} else {
					admissionFailure[0] = "Native pre-pulse admission changed: owner=" + owner.position() + ", ownerHealth=" + owner.getHealth()
						+ ", guestHealth=" + guest.getHealth() + ", ending=" + duel.ending() + ", harmable=" + ArtKit.harmable(owner, guest)
						+ ", inside=" + ArtFields.inside(owner, CrimsonArts.RAIN, guest);
					beforeTick = null;
				}
			};
			onDamage = hit -> {
				if (hit.target != guest || hit.source.getEntity() != owner) return;
				onDamage = null;
				beforeTick = null;
				observed = true;
				check(hit.damage < SparRules.KNOCKOUT, "Rain ends the duel through nonlethal stop-health AFTER_DAMAGE, not death interception");
				check(!Duels.inDuel(guest) && duel.ending() == DuelRules.Ending.KNOCKOUT,
					"The real duel listener synchronously ends this admitted pulse's duel");
				check(Parties.blocksHarm(owner, guest), "Party protection has resumed before Red Rain's post-hit mark");
			};
		});
		context.waitTicks(ArtRules.BLEED_PERIOD + 2);
		world.getServer().runOnServer(server -> {
			check(admissionFailure[0] == null, admissionFailure[0]);
			check(admittedTicks[0] > 0, "Native ticks actually witnessed owner liveness and opponent admission before the rain hit");
			check(observed, "A real native Red Rain damage callback ran");
			check(!Reactions.has(guest, Reactions.Mark.BLEEDING), "Red Rain cannot add BLEEDING after its damage ends the allied duel");
			check(Effects.applying() == null && Effects.applyingCast() == null, "The ordinary pulse restores ambient source context");
			Wildercord.LOGGER.info("FIELD_MUTATION_PROOF red-rain-stop-health: real damage ended duel; no post-duel bleeding");
		});
	}

	private void callbackTransition(ClientGameTestContext context, TestSingleplayerContext world, String transition) {
		world.getServer().runOnServer(server -> {
			ServerPlayer owner = prepare(server, "hollow");
			guest = guest(owner, FEET.add(0, 0, 2));
			witness = mob(owner.level(), FEET.add(1, 0, 2));
			if (transition.equals("disband")) {
				join(owner, guest);
				var duel = Duels.startBout(owner, guest, new DuelRules.Terms(40, 0, 600, 0, 1, false), FEET, null);
				duel.tick(owner.level().getGameTime());
			}
			ArtKit.Hits hits = ArtKit.hits(owner, AuraFx.art(owner));
			ArtFields.Field[] opened = new ArtFields.Field[1];
			opened[0] = ArtFields.open(owner, PROBE, ArtFields.disc(() -> FEET, 5, 3), 20, 1, (field, player, age) -> {
				check(Effects.applying() == owner && Effects.applyingCast() == null, "A field owns all downstream mutation sinks without a borrowed cast");
				List<LivingEntity> selected = field.foes(player);
				check(selected.contains(guest) && selected.contains(witness), "Both candidates were selected before the synchronous callback");
				Stance.State beforeStance = Stance.state(guest);
				witness.setDeltaMovement(MOTION);
				hits.raw(guest, 1, null);
				check(observed, "The transition happens in the real admitted damage callback");
				boolean retired = !transition.equals("join") && !transition.equals("disband");
				if (retired) check(witness.getDeltaMovement().equals(MOTION), "A retired hit cannot resume Hollow's passive pull on another target");
				guest.setDeltaMovement(MOTION);
				witness.setDeltaMovement(MOTION);
				mutate(guest, owner);
				float before = witness.getHealth();
				hits.raw(witness, 1, null);
				mutate(witness, owner);
				if (retired) check(Stance.state(guest) == beforeStance, "A retired hit cannot resume downstream stance wear");
				if (transition.equals("join") || retired) unchanged(guest, "Post-callback guest");
				else changed(guest, "A disbanded former ally");
				if (retired) {
					check(witness.getHealth() == before, "Retired pulse cannot damage its next already-selected target");
					unchanged(witness, "Retired pulse's next target");
					independentSources(owner, guest, witness);
				} else {
					check(witness.getHealth() < before, "A live pulse still damages an unrelated hostile target");
					changed(witness, "A live pulse's hostile target");
				}
				// Helpful effects and self costs survive retirement; an explicitly different source is independent.
				check(guest.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30), owner), "Beneficial effects are preserved");
				check(owner.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30), owner), "Self-cost debuffs are preserved");
				check(witness.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30), guest), "Explicit unrelated sources remain admitted");
				Effects.withSource(null, () -> {
					witness.igniteForTicks(35);
					check(witness.getRemainingFireTicks() > 0, "Unattributed environmental fire is preserved");
				});
				check(Effects.applying() == owner, "Nested environmental context restores the field owner");
				field.end();
				mutations++;
			});
			if (transition.equals("clear")) {
				ArtFields.open(owner, "silent_clear", ArtFields.disc(() -> FEET, 1, 1), 20, 1,
					(field, player, age) -> { throw new AssertionError("A cleared snapshot field must not pulse"); })
					.onEnd(field -> { throw new AssertionError("Clear must preserve silent cleanup even for later tick snapshot entries"); });
			}
			onDamage = hit -> {
				if (hit.target != guest || hit.source.getEntity() != owner) return;
				onDamage = null;
				observed = true;
				switch (transition) {
					case "join" -> join(owner, guest);
					case "disband" -> {
						check(Parties.session(server).rules.disband(owner.getUUID()) == PartyRules.Result.OK, "Damage callback disbands the real party");
						Duels.callOff(owner);
						check(!Parties.sameParty(owner, guest) && ArtKit.harmable(owner, guest), "Former allied duellists become ordinary hostile targets");
					}
					case "end" -> opened[0].end();
					case "replace" -> {
						for (int i = 0; i < ArtFields.PER_PLAYER; i++) ArtFields.open(owner, "replacement", ArtFields.disc(() -> FEET, 1, 1), 20, 1,
							(field, player, age) -> { check(age > 0, "Callback-opened fields cannot pulse on their creation tick"); field.end(); });
						check(ArtFields.count(owner, PROBE) == 0 && ArtFields.count(owner, "replacement") == ArtFields.PER_PLAYER,
							"Opening from damage evicts the running oldest field and preserves its cap");
					}
					case "disconnect event" -> {
						// Dispatch the native event and its actual Aura -> MethodArts cleanup, without closing the test's TCP connection.
						ServerPlayConnectionEvents.DISCONNECT.invoker().onPlayDisconnect(owner.connection, server);
						check(ArtFields.count(owner, PROBE) == 0, "Fabric disconnect cleanup removes the current field synchronously");
					}
					case "clear" -> MethodArts.clear();
					default -> throw new AssertionError(transition);
				}
			};
		});
		context.waitTicks(3);
		world.getServer().runOnServer(server -> {
			check(observed && mutations == 1, "The real damage transition and post-callback assertions completed once: " + transition);
			check(Effects.applying() == null && Effects.applyingCast() == null, "No field source escapes a callback transition");
			ServerPlayer owner = server.getPlayerList().getPlayers().getFirst();
			float before = witness.getHealth();
			ArtKit.hits(owner, AuraFx.art(owner)).raw(witness, 1, null);
			check(witness.getHealth() < before, "A later non-field art strike still works for this owner");
			Effects.withSource(owner, () -> Reactions.mark(witness, Reactions.Mark.EXPOSED, 20));
			check(Reactions.has(witness, Reactions.Mark.EXPOSED), "A later non-field source scope still mutates hostile marks");
			Wildercord.LOGGER.info("FIELD_MUTATION_PROOF {}: native damage callback and sink assertions passed", transition);
		});
	}

	private void collapseAfterJoin(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer owner = prepare(server, "hollow");
			guest = guest(owner, FEET.add(0, 0, ArtRules.COLLAPSE_AHEAD));
			perform(owner, HollowArts.arts(), HollowArts.COLLAPSE);
			onDamage = hit -> {
				if (hit.target != guest || hit.source.getEntity() != owner) return;
				onDamage = null;
				observed = true;
				join(owner, guest);
			};
		});
		context.waitTicks(ArtRules.COLLAPSE_TICKS + 2);
		world.getServer().runOnServer(server -> {
			check(observed, "The actual Collapse strike reaches its real damage callback");
			check(!Reactions.has(guest, Reactions.Mark.SHADOWED), "Actual Collapse cannot shadow a target that joined during its strike");
			Wildercord.LOGGER.info("FIELD_MUTATION_PROOF collapse-join: no shadow after actual performer damage callback");
		});
	}

	private void nestedAndThrowingScopes(ClientGameTestContext context, TestSingleplayerContext world, boolean sameOwner) {
		world.getServer().runOnServer(server -> {
			ServerPlayer owner = prepare(server, "hollow");
			guest = guest(owner, FEET.add(0, 0, 2));
			witness = mob(owner.level(), FEET.add(1, 0, 2));
			ServerPlayer spellSource = sameOwner ? owner : guest;
			Cast unrelated = new Cast(spellSource);
			var harm = SpellCompiler.compile(List.of(Runes.TOUCH, Runes.HARM)).root().groups.getFirst().effects.getFirst();
			int[] ends = {0};
			ArtFields.open(owner, "outer", ArtFields.disc(() -> FEET, 5, 3), 20, 1, (field, player, age) -> {
				field.end();
				onDamage = hit -> {
					if (hit.target != witness || hit.source.getEntity() != spellSource) return;
					onDamage = null;
					check(Effects.applying() == spellSource && Effects.applyingCast() == unrelated, "Nested real spell establishes its own source and cast");
					tickFields(server);
					check(Effects.applying() == spellSource && Effects.applyingCast() == unrelated, "Nested/throwing field pulses restore the real outer spell and cast");
					observed = true;
				};
				Effects.apply(unrelated, harm, new Cast.Hit(List.of(witness), witness.position(), new Vec3(0, 0, 1), spellSource.position(), null, null, false));
				check(observed && mutations == 1 && ends[0] == 1, "Nested tick ran its throwing pulse and end callback exactly once");
				check(Effects.applying() == owner && Effects.applyingCast() == null, "After the nested spell the outer field still owns source attribution");
				check(Parties.blocksCurrentHarm(witness), "The outer retired-field guard is restored after the inner field returns");
			});
			ArtFields.open(owner, "throwing", ArtFields.disc(() -> FEET, 5, 3), 20, 1, (field, player, age) -> {
				check(Effects.applying() == owner && Effects.applyingCast() == null, "A nested pulse never borrows the unrelated spell's cast");
				check(!Parties.blocksCurrentHarm(witness), "The live inner field does not inherit the retired outer field's veto");
				mutations++;
				throw new IllegalStateException("Intentional native field scope restoration probe");
			}).onEnd(field -> {
				ends[0]++;
				check(Effects.applying() == spellSource && Effects.applyingCast() == unrelated, "End cleanup runs after the pulse's source scope has unwound");
				check(witness.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30), spellSource), "The enclosing spell retains harmful effects even when it shares the retired field's owner");
				witness.setInvulnerableTime(0);
				float before = witness.getHealth();
				check(witness.hurtServer(owner.level(), owner.level().damageSources().playerAttack(spellSource), 1)
					&& witness.getHealth() < before, "The enclosing source retains actual damage admission during field end cleanup");
				ArtFields.open(owner, "after_end", ArtFields.disc(() -> FEET, 1, 1), 20, 1, (next, player, age) -> next.end());
			});
		});
		context.waitTicks(3);
		world.getServer().runOnServer(server -> {
			check(observed && mutations == 1, "The nested native damage/tick/throw path completed");
			check(Effects.applying() == null && Effects.applyingCast() == null, "Neither nested scope retains a source or cast");
			check(!Parties.blocksHarm(server.getPlayerList().getPlayers().getFirst(), witness), "No retired-field guard survives the pulse stack");
			Wildercord.LOGGER.info("FIELD_MUTATION_PROOF nested-throw sameOwner={}: source, cast and retirement context restored", sameOwner);
		});
	}

	private ServerPlayer prepare(MinecraftServer server, String method) {
		ServerPlayer owner = server.getPlayerList().getPlayers().getFirst();
		cleanup(owner);
		observed = false;
		mutations = 0;
		owner.level().getGameRules().set(GameRules.PVP, true, server);
		owner.setGameMode(GameType.SURVIVAL);
		owner.setHealth(owner.getMaxHealth());
		owner.removeAllEffects();
		owner.clearFire();
		// A connected player must receive the teleport packet. Raw snapTo leaves its client at the
		// superflat spawn (y=-60), whose next movement packet looks like a fatal fall from this platform.
		check(owner.teleportTo(owner.level(), FEET.x, FEET.y, FEET.z, java.util.Set.of(), 0, 0, false),
			"The connected owner is teleported through the native player connection");
		owner.setDeltaMovement(Vec3.ZERO);
		owner.resetFallDistance();
		owner.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		owner.setAttached(AuraAttachments.AURA, new AuraAttachments.Data(method, AuraRules.SOVEREIGN, 4500, 160, 0));
		return owner;
	}

	private void cleanup(ServerPlayer owner) {
		onDamage = null;
		beforeTick = null;
		Duels.callOff(owner);
		MethodArts.forget(owner.getUUID());
		Parties.session(owner.level().getServer()).rules.clear();
		if (guest != null) { guest.discard(); guest = null; }
		if (witness != null) { witness.discard(); witness = null; }
	}

	private static void join(ServerPlayer owner, Guest target) {
		PartyRules rules = Parties.session(owner.level().getServer()).rules;
		long now = owner.level().getGameTime();
		check(rules.invite(owner.getUUID(), target.getUUID(), now) == PartyRules.Result.OK, "A real party invitation is created");
		check(rules.accept(target.getUUID(), owner.getUUID(), now) == PartyRules.Result.OK, "The invited UUID accepts real server membership");
	}

	private static Guest guest(ServerPlayer owner, Vec3 at) {
		Guest target = new Guest(owner.level());
		target.setGameMode(GameType.SURVIVAL);
		target.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("starlit", AuraRules.GLOW, 100, 0, 0));
		move(target, at);
		target.setNoGravity(true);
		owner.level().addNewPlayer(target);
		return target;
	}

	private static Mob mob(ServerLevel level, Vec3 at) {
		Mob target = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		check(target != null, "Native hostile fixture exists");
		target.addTag("wildercord.rolled");
		target.setNoAi(true);
		target.setNoGravity(true);
		target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
		target.setHealth(100);
		move(target, at);
		level.addFreshEntity(target);
		return target;
	}

	private static void move(LivingEntity target, Vec3 at) {
		target.snapTo(at.x, at.y, at.z, 0, 0);
		target.setDeltaMovement(Vec3.ZERO);
	}

	private static void perform(ServerPlayer owner, List<AuraApi.StringArt> arts, String id) {
		var art = arts.stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow();
		check(Effects.withSource(owner, () -> art.performer().perform(owner, new AuraApi.StringContext(art, List.of(), null, owner.level().getGameTime()))),
			"Actual art performer accepts " + id);
	}

	private static void mutate(LivingEntity target, ServerPlayer owner) {
		Reactions.mark(target, Reactions.Mark.BLEEDING, 30);
		ArtKit.ignite(target, 30);
		target.setTicksFrozen(30);
		target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30), owner);
		ArtKit.shove(target, new Vec3(.2, .3, .2));
		ArtKit.steady(target);
	}

	private static void unchanged(LivingEntity target, String note) {
		check(!Reactions.has(target, Reactions.Mark.BLEEDING) && !target.isOnFire() && target.getTicksFrozen() == 0
			&& !target.hasEffect(MobEffects.SLOWNESS) && target.getDeltaMovement().equals(MOTION), note + " rejects every harmful mutation at its sink");
	}

	private static void changed(LivingEntity target, String note) {
		check(Reactions.has(target, Reactions.Mark.BLEEDING) && target.isOnFire() && target.getTicksFrozen() > 0
			&& target.hasEffect(MobEffects.SLOWNESS) && !target.getDeltaMovement().equals(MOTION), note + " still receives ordinary admitted mutations");
	}

	private static void independentSources(ServerPlayer owner, Guest other, LivingEntity target) {
		Effects.withSource(owner, () -> {
			check(!Parties.blocksCurrentHarm(target), "A new same-owner non-spell scope is independent of the retired pulse");
			check(target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 30), owner), "An independent same-owner action admits harmful effects");
			float before = target.getHealth();
			check(target.hurtServer(owner.level(), owner.level().damageSources().playerAttack(owner), 1)
				&& target.getHealth() < before, "An independent same-owner action admits actual damage");
			Effects.withSource(other, () -> {
				Reactions.mark(target, Reactions.Mark.SHADOWED, 30);
				check(Reactions.has(target, Reactions.Mark.SHADOWED), "A nested other-owner non-spell scope is also independent");
				Reactions.clear(target, Reactions.Mark.SHADOWED);
			});
			check(Effects.applying() == owner && !Parties.blocksCurrentHarm(target), "Nested source restoration returns to the independent same-owner action");
		});
		try {
			Effects.withSource(owner, () -> {
				check(!Parties.blocksCurrentHarm(target), "Throwing same-owner actions receive their own source scope");
				throw new IllegalStateException("Intentional source scope restoration probe");
			});
			throw new AssertionError("The nested source failure must propagate to its caller");
		} catch (IllegalStateException expected) {
			check(Effects.applying() == owner && Effects.applyingCast() == null && Parties.blocksCurrentHarm(target),
				"Returning and throwing same-owner scopes restore the retired pulse's exact admission scope");
		}
		Reactions.mark(target, Reactions.Mark.SHADOWED, 30);
		check(!Reactions.has(target, Reactions.Mark.SHADOWED), "The resumed retired field still cannot mutate after independent actions");
	}

	private static void tickFields(MinecraftServer server) {
		try {
			var tick = ArtFields.class.getDeclaredMethod("tick", MinecraftServer.class);
			tick.setAccessible(true);
			tick.invoke(null, server);
		} catch (InvocationTargetException failure) {
			if (failure.getCause() instanceof Error error) throw error;
			if (failure.getCause() instanceof RuntimeException error) throw error;
			throw new AssertionError(failure.getCause());
		} catch (ReflectiveOperationException failure) {
			throw new AssertionError("Native field tick entrypoint unavailable", failure);
		}
	}

	private static void check(boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}
}
