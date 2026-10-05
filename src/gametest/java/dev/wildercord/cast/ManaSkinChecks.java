package dev.wildercord.cast;

import com.mojang.authlib.GameProfile;
import dev.wildercord.Wildercord;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;

import java.util.List;
import java.util.UUID;

/** Native hurtServer probes: real defence resolution, callback order, payment and scoped reentrancy. */
public final class ManaSkinChecks {
	private ManaSkinChecks() {}
	private static boolean registered;
	private static Target watching, throwing;
	private static DamageSource watchedSource, throwingSource;
	private static Runnable beforeRebate, afterRebate;
	private static final class ProbeFailure extends RuntimeException {}
	private static final class Target extends FakePlayer {
		boolean reject;
		LivingEntity healSource;
		Cast healCast, incomingCast;
		double masteryBeforeHeal, masteryAfterHeal;
		float healthBeforeHeal, healthAfterHeal;
		Target(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "ManaSkinProbe")); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return reject; }
		@Override public void heal(float amount) {
			healSource = Effects.applying(); healCast = Effects.applyingCast(); healthBeforeHeal = getHealth();
			if (incomingCast != null) masteryBeforeHeal = incomingCast.mastery().earned;
			super.heal(amount);
			healthAfterHeal = getHealth();
			if (incomingCast != null) masteryAfterHeal = incomingCast.mastery().earned;
		}
	}

	public static void run(ClientGameTestContext context) {
		register();
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(30);
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule natural_health_regeneration false");
			world.getServer().runOnServer(server -> {
				ServerLevel level = server.overworld();
				basic(level); armour(level); lifesaves(level); callbacks(level); attribution(level);
			});
			Target[] low = new Target[1];
			boolean[] fired = {false};
			world.getServer().runOnServer(server -> {
				low[0] = add(server.overworld()); low[0].setHealth(8);
				Scheduler.onLowHealth(low[0], 20, () -> fired[0] = true);
				hit(low[0], 2.4F);
				check(close(low[0].getHealth(), 6.08F), "The threshold-crossing wound receives its later rebate");
			});
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				check(fired[0], "The real pre-rebate low-health watcher still fires after 8 -> 5.6 -> 6.08");
				low[0].discard();
			});
		} finally { watching = throwing = null; watchedSource = throwingSource = null; beforeRebate = afterRebate = null; }
	}

	private static void register() {
		if (registered) return;
		registered = true;
		var phase = Wildercord.id("mana_skin_native_before_rebate");
		ServerLivingEntityEvents.AFTER_DAMAGE.addPhaseOrdering(phase, Event.DEFAULT_PHASE);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(phase, (entity, source, base, damage, blocked) -> {
			if (entity == watching && source == watchedSource && beforeRebate != null) {
				Runnable action = beforeRebate; beforeRebate = null; action.run();
			}
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, damage) -> {
			if (entity == throwing && source == throwingSource) throw new ProbeFailure();
			return true;
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, damage, blocked) -> {
			if (entity == watching && afterRebate != null) {
				Runnable action = afterRebate; afterRebate = null; action.run();
			}
		});
	}

	private static void attribution(ServerLevel level) {
		Target attacker = add(level), defender = add(level);
		var patient = EntityTypes.WOLF.create(level, EntitySpawnReason.COMMAND);
		check(patient != null, "The helpful-healing control has a real tameable patient");
		try {
			check(Targets.canHarm(attacker, defender), "The real damaging spell may hit this non-allied player");
			List<dev.wildercord.spell.RuneDef> harmful = List.of(Runes.TOUCH, Runes.HARM);
			Cast attack = new Cast(attacker); Mastery.onCast(attacker, 0, harmful, attack, 0);
			check(attack.mastery() != null && attack.mastery().learns, "The incoming spell has an actual learning tally");
			defender.incomingCast = attack;
			double healCredit = healingCredit(attacker);
			watching = defender; watchedSource = null;
			boolean[] restored = {false};
			afterRebate = () -> {
				check(Effects.applying() == attacker && Effects.applyingCast() == attack,
					"The passive restores the exact incoming source and cast before the next damage callback");
				restored[0] = true;
			};
			Effects.withSource(defender, () -> {
				apply(attack, Runes.HARM, defender);
				check(Effects.applying() == defender && Effects.applyingCast() == null,
					"The damaging spell also restores its enclosing non-spell source");
			});
			check(restored[0] && defender.healSource == defender && defender.healCast == null,
				"Mana Skin heals under its owner's non-spell attribution");
			float wound = 20 - defender.healthBeforeHeal, recovered = defender.healthAfterHeal - defender.healthBeforeHeal;
			check(wound > 0 && recovered >= .25F && close(recovered, wound * .2F)
				&& close(100 - Spellbooks.mana(defender), recovered * 2), "Attribution preserves native recovery and exact payment");
			check(healingCredit(attacker) == healCredit && defender.masteryBeforeHeal == defender.masteryAfterHeal,
				"The attacker's Life healing credit and cast tally gain nothing from the defender's passive");
			check(attack.mastery().earned > 0, "The actual harmful spell still earns its normal damage mastery");
			check(defender.getLastDamageSource() != null && defender.getLastDamageSource().getEntity() == attacker,
				"The real wound still retains its attacking owner");

			patient.setTame(true, false); patient.setOwner(attacker); patient.setNoAi(true);
			patient.setPos(attacker.getX() + 2, attacker.getY(), attacker.getZ()); level.addFreshEntity(patient);
			patient.setHealth(patient.getMaxHealth() * .5F);
			check(patient.isAlive() && Targets.canHelp(attacker, patient), "The positive control is a living, genuinely owned ally");
			Cast healing = new Cast(attacker); Mastery.onCast(attacker, 0, List.of(Runes.TOUCH, Runes.HEAL), healing, 0);
			float healthBefore = patient.getHealth();
			apply(healing, Runes.HEAL, patient);
			check(patient.getHealth() > healthBefore && healingCredit(attacker) > healCredit && healing.mastery().earned > 0,
				"A genuine helpful spell still earns Life affinity and healing mastery");
		} finally {
			watching = null; afterRebate = null; attacker.discard(); defender.discard(); patient.discard();
		}
	}

	private static double healingCredit(Target player) {
		var tally = player.getAttachedOrElse(WildercordAttachments.AFFINITY_TALLY, WildercordAttachments.AffinityTally.NONE);
		return tally.earned(dev.wildercord.spell.PlayerAffinity.day(player.level().getGameTime()), "heal");
	}
	private static void apply(Cast cast, dev.wildercord.spell.RuneDef rune, LivingEntity target) {
		var node = SpellCompiler.compile(List.of(Runes.TOUCH, rune)).root().groups.getFirst().effects.getFirst();
		Effects.apply(cast, node, new Cast.Hit(List.of(target), target.position(), cast.caster.getLookAngle(), cast.caster.position(), null, null, false));
	}

	private static void basic(ServerLevel level) {
		probe(level, "bare", p -> {}, 5, 16, 98);
		probe(level, "old wound", p -> p.setHealth(10), 5, 6, 98);
		probe(level, "full absorption with old wound", p -> {p.setHealth(10); p.setAbsorptionAmount(8);}, 5, 10, 100);
		probe(level, "partial absorption", p -> p.setAbsorptionAmount(3), 5, 18.4F, 99.2F);
		probe(level, "resistance", p -> p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 4)), 5, 20, 100);
		probe(level, "limited mana", p -> Spellbooks.setMana(p, 1), 5, 15.5F, 0);
		probe(level, "minimum mana", p -> Spellbooks.setMana(p, .5F), 5, 15.25F, 0);
		probe(level, "below minimum mana", p -> Spellbooks.setMana(p, .49F), 5, 15, .49F);
		probe(level, "below minimum wound", p -> {}, 1, 19, 100);
		probe(level, "Gash", p -> CraftedRunes.noHeal(p, 40), 5, 15, 100);
		probe(level, "rejected", p -> p.reject = true, 5, 20, 100);
		probe(level, "no cord", p -> Spellbooks.setCord(p, ItemStack.EMPTY), 5, 15, 100);
		Target bypass = add(level);
		try {
			bypass.hurtServer(level, level.damageSources().genericKill(), 5);
			check(close(bypass.getHealth(), 15) && close(Spellbooks.mana(bypass), 100), "Bypass-invulnerability sources retain no rebate");
		} finally { bypass.discard(); }
	}

	private static void armour(ServerLevel level) {
		Target attacker = add(level), control = add(level), skin = add(level);
		try {
			control.setAttached(WildercordAttachments.CIRCLES, 0);
			dress(control); dress(skin);
			// generic() bypasses armour in vanilla; this comparison needs an actual physical source.
			DamageSource physical = level.damageSources().playerAttack(attacker);
			check(!physical.is(DamageTypeTags.BYPASSES_ARMOR) && !physical.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)
				&& physical.getEntity() == attacker && physical.getDirectEntity() == attacker,
				"The P4 probe has a physical owner and admits both native armour and enchantments");
			check(dev.wildercord.player.Heart.active(control) == 0 && dev.wildercord.player.Heart.active(skin) == 20
				&& close(control.getHealth(), 20) && close(skin.getHealth(), 20)
				&& control.getAbsorptionAmount() == 0 && skin.getAbsorptionAmount() == 0,
				"The P4 comparison begins with equal health and no absorption, with Skin active only on its recipient");
			boolean controlAccepted = control.hurtServer(level, physical, 28);
			boolean skinAccepted = skin.hurtServer(level, physical, 28);
			float wound = 20 - control.getHealth();
			Wildercord.LOGGER.info("[mana-skin-native] P4 source={} bypassArmour={} accepted={}/{} controlHealth={} skinHealth={} nativeWound={} finalWound={} manaSpent={} armour={}/{} toughness={}/{} circles={}/{}",
				physical.typeHolder().unwrapKey().orElseThrow().identifier(), physical.is(DamageTypeTags.BYPASSES_ARMOR), controlAccepted, skinAccepted,
				control.getHealth(), skin.getHealth(), wound, 20 - skin.getHealth(), 100 - Spellbooks.mana(skin), control.getArmorValue(), skin.getArmorValue(),
				control.getAttributeValue(Attributes.ARMOR_TOUGHNESS), skin.getAttributeValue(Attributes.ARMOR_TOUGHNESS),
				dev.wildercord.player.Heart.active(control), dev.wildercord.player.Heart.active(skin));
			check(controlAccepted && skinAccepted && control.getLastDamageSource() == physical && skin.getLastDamageSource() == physical,
				"Both P4 recipients accept and retain the identical native physical damage source");
			check(wound > 1.25F && wound < 5, "Real Protection IV netherite produces the small native control wound");
			check(close(20 - skin.getHealth(), wound * .8F) && close(100 - Spellbooks.mana(skin), wound * .4F),
				"Protection IV retains eighty percent health loss and pays two mana per restored health");
		} finally { attacker.discard(); control.discard(); skin.discard(); }
	}

	private static void lifesaves(ServerLevel level) {
		Target lethal = add(level), totem = add(level), reversal = add(level);
		try {
			hit(lethal, 28);
			check(lethal.isDeadOrDying() && Spellbooks.mana(lethal) == 100, "Mana Skin never revives an unsaved lethal wound");
			totem.setHealth(1); totem.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING)); hit(totem, 28);
			check(totem.isAlive() && close(totem.getHealth(), 1) && totem.getOffhandItem().isEmpty()
				&& Spellbooks.mana(totem) == 100, "Totem recovery receives no additional Mana Skin rebate or charge");
			reversal.setHealth(2);
			CastEngine.cast(reversal, SpellCompiler.compile(List.of(Runes.SELF, Runes.REVERSAL)).root());
			Spellbooks.setMana(reversal, 100); hit(reversal, 28);
			check(reversal.isAlive() && close(reversal.getHealth(), 10) && DeathsDoor.resting(reversal) > 0
				&& Spellbooks.mana(reversal) == 100, "Real Reversal alone restores half health without a lethal rebate");
		} finally { lethal.discard(); totem.discard(); reversal.discard(); }
	}

	private static void callbacks(ServerLevel level) {
		for (String mode : List.of("same source", "other source", "other target", "rejected nested", "throwing nested", "earlier healing", "partial earlier healing", "earlier Gash")) {
			Target outer = add(level), inner = add(level);
			try {
				watching = outer; watchedSource = level.damageSources().generic();
				if (mode.contains("healing")) outer.setHealth(10);
				beforeRebate = () -> {
					check(close(outer.getHealth(), mode.contains("healing") ? 5 : 15) && Spellbooks.mana(outer) == 100,
						"Earlier callbacks still see the genuine wound before rebate: " + mode);
					switch (mode) {
						case "same source" -> outer.hurtServer(level, watchedSource, 9);
						case "other source" -> outer.hurtServer(level, level.damageSources().magic(), 9);
						case "other target" -> inner.hurtServer(level, watchedSource, 5);
						case "rejected nested" -> { inner.reject = true; inner.hurtServer(level, watchedSource, 5); }
						case "throwing nested" -> {
							throwing = outer; throwingSource = level.damageSources().magic();
							try { outer.hurtServer(level, throwingSource, 9); throw new AssertionError("Expected native callback exception"); }
							catch (ProbeFailure expected) { /* Scope must restore even though this native hit aborts. */ }
							finally { throwing = null; throwingSource = null; }
						}
						case "earlier healing" -> outer.heal(5);
						case "partial earlier healing" -> outer.heal(4.75F);
						case "earlier Gash" -> CraftedRunes.noHeal(outer, 40);
					}
				};
				outer.hurtServer(level, watchedSource, 5);
				float expectedHealth = switch (mode) { case "same source", "other source" -> 12.8F; case "earlier healing", "partial earlier healing" -> 10; case "earlier Gash" -> 15; default -> 16; };
				float expectedMana = switch (mode) { case "same source", "other source" -> 96.4F; case "earlier healing", "earlier Gash" -> 100; case "partial earlier healing" -> 99.5F; default -> 98; };
				check(close(outer.getHealth(), expectedHealth) && close(Spellbooks.mana(outer), expectedMana), "One scoped rebate survives callback mutation: " + mode);
				check(close(inner.getHealth(), mode.equals("other target") ? 16 : 20)
					&& close(Spellbooks.mana(inner), mode.equals("other target") ? 98 : 100), "A nested target cannot borrow the outer wound: " + mode);
				check(dev.wildercord.player.ManaSkinDamage.take(outer, watchedSource) == null, "The hit leaves no stale consumable wound: " + mode);
			} finally { watching = throwing = null; watchedSource = throwingSource = null; beforeRebate = null; outer.discard(); inner.discard(); }
		}
	}

	private static void probe(ServerLevel level, String label, java.util.function.Consumer<Target> setup, float damage, float health, float mana) {
		Target target = add(level);
		try {
			setup.accept(target); hit(target, damage);
			check(close(target.getHealth(), health) && close(Spellbooks.mana(target), mana), label + ": expected health=" + health + ", mana=" + mana + "; actual=" + target.getHealth() + ", " + Spellbooks.mana(target));
			Wildercord.LOGGER.info("[mana-skin-native] {} health={} mana={}", label, target.getHealth(), Spellbooks.mana(target));
		} finally { target.discard(); }
	}
	private static Target add(ServerLevel level) {
		Target target = new Target(level); target.setGameMode(GameType.SURVIVAL);
		target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20); target.setHealth(20);
		target.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(64);
		target.setPos(0.5, 150, 0.5); level.addNewPlayer(target);
		Spellbooks.setCord(target, new ItemStack(WildercordItems.ECHO_CORD));
		Spellbooks.set(target, new Spellbook(List.of(Runes.SELF.id(), Runes.REVERSAL.id()), List.of(), 0, true));
		target.setAttached(WildercordAttachments.CIRCLES, 20); target.setAttached(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
		Spellbooks.setMana(target, 100); return target;
	}
	private static void dress(Target target) {
		var enchantment = target.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
		Item[] items = {Items.NETHERITE_BOOTS, Items.NETHERITE_LEGGINGS, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_HELMET};
		EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
		for (int i = 0; i < slots.length; i++) { var stack = new ItemStack(items[i]); stack.enchant(enchantment, 4); target.setItemSlot(slots[i], stack); }
		target.doTick();
		check(target.getArmorValue() == 20 && target.getAttributeValue(Attributes.ARMOR_TOUGHNESS) >= 12, "Native equipment ticking installs netherite armour");
		for (EquipmentSlot slot : slots) check(EnchantmentHelper.getItemEnchantmentLevel(enchantment, target.getItemBySlot(slot)) == 4,
			"The native " + slot + " equipment really retains Protection IV");
	}
	private static void hit(Target target, float damage) { target.hurtServer(target.level(), target.level().damageSources().generic(), damage); }
	private static boolean close(float actual, float expected) { return Math.abs(actual - expected) < .001F; }
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
