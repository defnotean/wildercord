package dev.wildercord.aura.world;

import com.mojang.authlib.GameProfile;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraGuard;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.AuraStep;
import dev.wildercord.cast.CastEngine;
import dev.wildercord.cast.Charging;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.cast.Statuses;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Native damage-event/charge seam checks, deliberately with the enrolled Master AI paused.
 * These do not prove pursuit movement or timing; MasterPursuitChecks owns that integration.
 * Every interruption-positive probe gets a new target UUID. Production Statuses cooldowns are never cleared.
 */
final class MasterHitReceiptChecks {
	private static final float HIT = 28, HEALTH = 200;
	private static final List<EquipmentSlot> EQUIPMENT = List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
	private enum Defence { BARE, FULL_ABSORPTION, PARTIAL_ABSORPTION, MANA_SKIN, SKIN_AND_ABSORPTION,
		PROTECTION_AND_SKIN, REVERSAL, TOTEM, RESISTANCE_V, FORESIGHT, REJECTED, NONPARTICIPANT, OTHER_SOURCE, OTHER_TARGET }
	private static final class Challenger extends FakePlayer {
		boolean rejectDamage;
		Challenger(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "ReceiptTarget")); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return rejectDamage; }
	}
	private SwordMaster master;
	private Challenger target, outsider;
	private Vec3 origin;

	void run(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			for (String command : List.of("difficulty normal", "gamerule spawn_mobs false", "gamerule natural_health_regeneration false"))
				world.getServer().runCommand(command);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				origin = new Vec3(player.getBlockX() + .5, 181, player.getBlockZ() + .5);
				for (int x = -15; x <= 15; x++) for (int z = -15; z <= 15; z++)
					player.level().setBlockAndUpdate(BlockPos.containing(origin).offset(x, -1, z), Blocks.STONE.defaultBlockState());
				place(player, 8, 3);
			});
			for (Defence defence : Defence.values()) world.getServer().runOnServer(server -> isolated(server.getPlayerList().getPlayers().getFirst(), defence));
			clientDefences(context, world);
		}
	}

	private void isolated(ServerPlayer observer, Defence defence) {
		ServerLevel level = observer.level();
		target = add(level, 0, 3);
		outsider = add(level, 4, 3);
		try {
			start(target);
			ServerPlayer measured = defence == Defence.NONPARTICIPANT || defence == Defence.OTHER_TARGET ? outsider : target;
			boolean skin = defence == Defence.MANA_SKIN || defence == Defence.SKIN_AND_ABSORPTION || defence == Defence.PROTECTION_AND_SKIN;
			if (skin) target.setAttached(WildercordAttachments.CIRCLES, 3);
			float absorption = defence == Defence.FULL_ABSORPTION ? 64 : defence == Defence.PARTIAL_ABSORPTION ? 4
				: defence == Defence.SKIN_AND_ABSORPTION ? 24 : 0;
			target.setAbsorptionAmount(absorption);
			check(close(target.getAbsorptionAmount(), absorption), defence + ": real absorption capacity admits the fixture hearts");
			if (defence == Defence.PROTECTION_AND_SKIN) {
				dress(target);
				// FakePlayer.tick() is intentionally empty. Its inherited native doTick installs equipment modifiers.
				target.doTick();
				check(target.getArmorValue() == 20 && target.getAttributeValue(Attributes.ARMOR_TOUGHNESS) >= 12,
					"Real Protection IV netherite has its native armour/toughness before the probe");
			}
			if (defence == Defence.REVERSAL) {
				target.setHealth(2);
				CastEngine.cast(target, SpellCompiler.compile(List.of(Runes.SELF, Runes.REVERSAL)).root());
			}
			if (defence == Defence.TOTEM) {
				target.setHealth(1); target.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
			}
			if (defence == Defence.RESISTANCE_V) {
				check(target.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 4)), "Native Resistance V is installed");
			}
			if (defence == Defence.FORESIGHT)
				CastEngine.cast(target, SpellCompiler.compile(List.of(Runes.SELF, Runes.FORESIGHT)).root());
			if (defence == Defence.REJECTED) target.rejectDamage = true;
			charge(measured);
			if (skin) check(Heart.active(target) == 3 && Spellbooks.mana(target) == 100, "Circle III Mana Skin starts with exactly 100 mana");
			var held = measured.getAttached(WildercordAttachments.CHARGE);
			float healthBefore = measured.getHealth(), absorptionBefore = measured.getAbsorptionAmount(), manaBefore = Spellbooks.mana(measured);
			DamageSource sourceBefore = measured.getLastDamageSource();
			Vec3 positionBefore = measured.position();
			MasterHitReceipt.Result receipt = MasterHitReceipt.measure(master, measured, () -> {
				if (defence == Defence.OTHER_SOURCE) {
					Effects.readyToHurt(measured);
					measured.hurtServer(level, level.damageSources().generic(), HIT);
					return healthBefore - measured.getHealth();
				}
				return master.projected(defence == Defence.OTHER_TARGET ? target : measured, defence == Defence.FORESIGHT ? 12 : HIT);
			});
			String note = defence + " " + receipt + ", health=" + measured.getHealth() + ", absorption=" + measured.getAbsorptionAmount()
				+ ", mana=" + Spellbooks.mana(measured);
			Wildercord.LOGGER.info("[master-hit-receipt] {}", note);
			check(measured.getAttached(WildercordAttachments.CHARGE) == held, "Measuring native damage alone does not rewrite the held spell: " + note);
			boolean damaging = switch (defence) {
				case BARE, FULL_ABSORPTION, PARTIAL_ABSORPTION, MANA_SKIN, SKIN_AND_ABSORPTION, PROTECTION_AND_SKIN, REVERSAL, TOTEM -> true;
				default -> false;
			};
			check(receipt.damaging() == damaging, "Only resolved matching-source health or absorption damage admits interruption: " + note);
			if (damaging) {
				check(receipt.observed(), "The matching native Aura damage operation was observed: " + note);
				assertMasterSource(measured, note);
				check(close(receipt.absorptionLost(), Math.min(absorption, HIT)), "The receipt records the actual absorption consumed: " + note);
				boolean lifesave = defence == Defence.REVERSAL || defence == Defence.TOTEM;
				if (lifesave) check(close(receipt.healthLost(), healthBefore), "The pre-death-save receipt retains the complete real wound: " + note);
				else if (defence != Defence.PROTECTION_AND_SKIN)
					check(close(receipt.healthLost(), Math.max(0, HIT - absorption)), "The receipt records native pre-heal health damage: " + note);
				else check(receipt.healthLost() > 0 && receipt.healthLost() < HIT * .2F, "Armour leaves a genuine wound smaller than Mana Skin's heal: " + note);
				float expectedNet = defence == Defence.PROTECTION_AND_SKIN || lifesave ? 0 : Math.max(0, HIT - absorption - (skin ? HIT * .2F : 0));
				check(close(receipt.netHealthLost(), expectedNet) && close(Math.max(0, healthBefore - measured.getHealth()), expectedNet),
					"The original projected return remains its unchanged post-heal health delta: " + note);
				check(close(Spellbooks.mana(measured), manaBefore - (skin ? HIT * .4F : 0)), "Only real Mana Skin pays mana at the existing rate: " + note);
				if (defence == Defence.REVERSAL) check(measured.isAlive() && close(measured.getHealth(), HEALTH * .5F),
					"The real self-cast Reversal restores half health after a lethal wound: " + note);
				if (defence == Defence.TOTEM) check(measured.isAlive() && measured.getOffhandItem().isEmpty() && close(measured.getHealth(), 1)
					&& measured.hasEffect(MobEffects.ABSORPTION), "A real offhand totem is consumed and restores life after the wound: " + note);
				// This is the public interruption seam used after receipt admission, not a second simulated attack.
				Effects.withSource(master, () -> check(Statuses.interrupt(measured), "Fresh UUID admits a real Statuses interruption: " + note));
				check(!measured.hasAttached(WildercordAttachments.CHARGE), "Resolved damage breaks precisely the previously held spell: " + note);
			} else {
				check(receipt.healthLost() == 0 && receipt.absorptionLost() == 0, "A rejected or unrelated event cannot manufacture receipt damage: " + note);
				if (defence == Defence.RESISTANCE_V) {
					check(receipt.observed(), "Resistance V reaches the native damage operation but resolves no health or absorption loss: " + note);
					assertMasterSource(measured, note);
				} else check(!receipt.observed(), "A rejected hit or mismatching source/target cannot be observed as this hit: " + note);
				if (defence == Defence.OTHER_SOURCE) check(close(healthBefore - measured.getHealth(), HIT), "An unrelated native source really damaged the target: " + note);
				else check(measured.getHealth() == healthBefore, "The measured target loses no health: " + note);
				check(measured.getAbsorptionAmount() == absorptionBefore && Spellbooks.mana(measured) == manaBefore, "Rejected hits do not spend absorption or mana: " + note);
				if (defence == Defence.FORESIGHT) check(measured.position().distanceToSqr(positionBefore) > .5,
					"The real self-cast Foresight ward executes its native sidestep");
				if (defence == Defence.OTHER_TARGET) check(close(target.getHealth(), HEALTH - HIT), "The unrelated target's native hit really landed");
				if (defence == Defence.NONPARTICIPANT || defence == Defence.REJECTED || defence == Defence.FORESIGHT)
					check(measured.getLastDamageSource() == sourceBefore, "A stopped hit does not replace the last accepted damage source: " + note);
				check(measured.getAttached(WildercordAttachments.CHARGE) == held, "The exact held charge survives the non-damaging receipt: " + note);
			}
		} finally {
			if (master != null) { master.discard(); master = null; }
			Charging.forget(target); target.discard(); target = null;
			Charging.forget(outsider); outsider.discard(); outsider = null;
		}
	}

	private void clientDefences(ClientGameTestContext context, TestSingleplayerContext world) {
		PlayerState[] original = new PlayerState[1];
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			original[0] = new PlayerState(player);
			prepare(player); place(player, 0, 3); start(player);
		});
		try {
			context.getInput().holdKey(options -> options.keyShift);
			world.getServer().waitFor(server -> server.getPlayerList().getPlayers().getFirst().isShiftKeyDown(), 20);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				charge(player); player.setAttached(WildercordAttachments.CIRCLES, 3); player.setAbsorptionAmount(64);
				player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
				player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.FLOW, 0, 40, 0));
				check(player.isShiftKeyDown() && AuraGuard.faces(player, master.position()), "Actual client sneak and facing support a frontal parry");
				check(AuraGuard.raise(player) && AuraGuard.perfectNow(player), "The native Aura Guard entrypoint raises a perfect guard");
				preserves(player, "Perfect Aura parry with full absorption and Mana Skin");
				check(!AuraGuard.perfectNow(player), "The hit consumes the actual perfect-guard window");
				Charging.forget(player);
				player.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE);
			});
			context.getInput().releaseKey(options -> options.keyShift);
			// Let the real player ground naturally; NoAI on the boss does not establish onGround on anything.
			context.waitTicks(5);
			world.getServer().waitFor(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				return player.onGround() && !player.isShiftKeyDown();
			}, 20);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("gale", AuraRules.FORM, 0, 80, 0));
				charge(player);
				float aura = Aura.aura(player);
				check(AuraStep.step(player) && Aura.aura(player) < aura, "A naturally grounded player buys a real Aura Step through its public entrypoint");
				preserves(player, "Real Aura Step untouchable window");
			});
			// Drain only this fixture's scheduled Step movement through real ticks before restoring its position/state.
			context.waitTicks(Math.max(AuraRules.STEP_TICKS, AuraRules.STEP_GUARD_TICKS) + 2);
		} finally {
			context.getInput().releaseKey(options -> options.keyShift);
			world.getServer().runOnServer(server -> {
				if (master != null) { master.discard(); master = null; }
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				Charging.forget(player);
				original[0].restore(player);
			});
		}
	}

	private void preserves(ServerPlayer player, String label) {
		var charge = player.getAttached(WildercordAttachments.CHARGE);
		check(charge != null, label + ": a real held spell is present before impact");
		float health = player.getHealth(), absorption = player.getAbsorptionAmount(), mana = Spellbooks.mana(player);
		DamageSource source = player.getLastDamageSource();
		MasterHitReceipt.Result receipt = MasterHitReceipt.measure(master, player, () -> master.projected(player, HIT));
		check(!receipt.observed() && !receipt.damaging() && receipt.healthLost() == 0 && receipt.absorptionLost() == 0 && receipt.netHealthLost() == 0,
			label + ": the real defence prevents the damage event and all receipt deltas: " + receipt);
		check(player.getHealth() == health && player.getAbsorptionAmount() == absorption && Spellbooks.mana(player) == mana,
			label + ": health, absorption and mana are unchanged");
		check(player.getLastDamageSource() == source && player.getAttached(WildercordAttachments.CHARGE) == charge,
			label + ": the previous damage source and exact charge identity survive");
		Wildercord.LOGGER.info("[master-hit-receipt] {} {}", label, receipt);
	}

	private void start(ServerPlayer participant) {
		master = AuraWorld.SWORD_MASTER.create(participant.level(), EntitySpawnReason.COMMAND);
		check(master != null, "The native receipt Master is constructible");
		master.setDiscipline(MastersRules.GALE); master.snapTo(origin.x, origin.y, origin.z, 0, 0);
		master.setNoAi(true); // Isolated damage probes: no movement or pursuit admission is claimed here.
		participant.level().addFreshEntity(master);
		master.mobInteract(participant, InteractionHand.MAIN_HAND); master.mobInteract(participant, InteractionHand.MAIN_HAND);
		check(SwordMaster.ready(participant) == 1, "The measured challenger explicitly enrolls and closes its trial");
		master.customServerAiStep(participant.level());
		check(master.started() && master.canHarmParticipant(participant) && master.challengerCount() == 1,
			"The isolated native projected pipeline uses a started, locked, consenting trial");
	}

	private Challenger add(ServerLevel level, double x, double z) {
		Challenger player = new Challenger(level); prepare(player);
		player.snapTo(origin.x + x, origin.y, origin.z + z, 180, 0); level.addNewPlayer(player); return player;
	}
	private void prepare(ServerPlayer player) {
		player.setGameMode(GameType.SURVIVAL); player.removeAllEffects();
		player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH); player.setHealth(HEALTH);
		player.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(64); player.setAbsorptionAmount(0);
		for (EquipmentSlot slot : EQUIPMENT) player.setItemSlot(slot, ItemStack.EMPTY);
		player.setAttached(AuraAttachments.AURA, AuraAttachments.Data.NONE); player.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE);
		player.setAttached(WildercordAttachments.CIRCLES, 0); player.setAttached(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
	}
	private void charge(ServerPlayer player) {
		Spellbooks.setCord(player, new ItemStack(WildercordItems.TWINE_CORD));
		List<String> runes = List.of(Runes.BOLT.id(), Runes.HARM.id());
		Spellbooks.set(player, new Spellbook(runes, List.of(runes), 0, true));
		Spellbooks.setReadyAt(player, 0, 0); Spellbooks.setMana(player, 100);
		check(SpellCaster.activeRunes(Spellbooks.get(player), 0, Spellbooks.tier(player)).stream().map(rune -> rune.id()).toList().equals(runes),
			"The native charge fixture resolves both canonical registered rune IDs");
		Charging.request(player, 0, true);
		check(player.hasAttached(WildercordAttachments.CHARGE), "Native Charging.request accepts the fixture's held spell");
	}
	private static void dress(ServerPlayer player) {
		var protection = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
		Item[] pieces = {Items.NETHERITE_BOOTS, Items.NETHERITE_LEGGINGS, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_HELMET};
		EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
		for (int i = 0; i < pieces.length; i++) { ItemStack piece = new ItemStack(pieces[i]); piece.enchant(protection, 4); player.setItemSlot(slots[i], piece); }
	}
	private void place(ServerPlayer player, double x, double z) {
		player.teleportTo(player.level(), origin.x + x, origin.y, origin.z + z, Set.of(), 180, 0, false); player.setDeltaMovement(Vec3.ZERO);
	}
	private void assertMasterSource(ServerPlayer player, String note) {
		DamageSource source = player.getLastDamageSource();
		check(source != null && source.is(Aura.DAMAGE) && source.getEntity() == master && source.getDirectEntity() == master,
			"The real damage retains both Master owner and direct entity: " + note);
	}
	private static boolean close(float a, float b) { return Math.abs(a - b) < .001F; }
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

	/** Restore the actual client's fixture-owned state; fake players and the entire test world are discarded. */
	private static final class PlayerState {
		final double maxHealth, maxAbsorption;
		final float health, absorption, mana, yaw, pitch;
		final Vec3 position, velocity;
		final GameType mode;
		final EnumMap<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
		final List<MobEffectInstance> effects = new ArrayList<>();
		final AuraAttachments.Data aura;
		final AuraAttachments.State state;
		final Spellbook book;
		final ItemStack cord;
		final int circles;
		final WildercordAttachments.Cracks cracks;
		final List<Long> cooldowns;
		PlayerState(ServerPlayer player) {
			maxHealth = player.getAttribute(Attributes.MAX_HEALTH).getBaseValue(); maxAbsorption = player.getAttribute(Attributes.MAX_ABSORPTION).getBaseValue();
			health = player.getHealth(); absorption = player.getAbsorptionAmount(); mana = Spellbooks.mana(player);
			position = player.position(); velocity = player.getDeltaMovement(); yaw = player.getYRot(); pitch = player.getXRot(); mode = player.gameMode.getGameModeForPlayer();
			for (EquipmentSlot slot : EQUIPMENT) equipment.put(slot, player.getItemBySlot(slot).copy());
			for (var effect : player.getActiveEffects()) effects.add(new MobEffectInstance(effect));
			aura = player.getAttachedOrElse(AuraAttachments.AURA, AuraAttachments.Data.NONE); state = player.getAttachedOrElse(AuraAttachments.STATE, AuraAttachments.State.NONE);
			book = Spellbooks.get(player); cord = Spellbooks.cord(player).copy(); circles = player.getAttachedOrElse(WildercordAttachments.CIRCLES, 0);
			cracks = player.getAttachedOrElse(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
			cooldowns = player.getAttachedOrElse(WildercordAttachments.COOLDOWNS, List.of());
		}
		void restore(ServerPlayer player) {
			player.removeAllEffects(); player.setAbsorptionAmount(0);
			player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth); player.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(maxAbsorption);
			for (var entry : equipment.entrySet()) player.setItemSlot(entry.getKey(), entry.getValue());
			for (var effect : effects) player.addEffect(effect);
			player.setHealth(health); player.setAbsorptionAmount(absorption); player.setGameMode(mode);
			player.setAttached(AuraAttachments.AURA, aura); player.setAttached(AuraAttachments.STATE, state);
			player.setAttached(WildercordAttachments.CIRCLES, circles); player.setAttached(WildercordAttachments.CRACKS, cracks);
			Spellbooks.set(player, book); Spellbooks.setCord(player, cord); Spellbooks.setMana(player, mana); player.setAttached(WildercordAttachments.COOLDOWNS, cooldowns);
			player.teleportTo(player.level(), position.x, position.y, position.z, Set.of(), yaw, pitch, false); player.setDeltaMovement(velocity);
		}
	}
}
