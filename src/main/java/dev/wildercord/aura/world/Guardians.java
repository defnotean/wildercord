package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.BreathingMethods;
import dev.wildercord.content.RuneItem;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.FieldGuide;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** The guardians of the lairs: bosses bound to the place they were generated in, with no natural spawn entry. */
public final class Guardians {
	private Guardians(){}
	private static final ResourceKey<EntityType<?>> SERPENT_KEY=ResourceKey.create(Registries.ENTITY_TYPE,Wildercord.id("rootcoil_serpent"));
	public static final EntityType<RootcoilSerpent> SERPENT=Registry.register(BuiltInRegistries.ENTITY_TYPE,SERPENT_KEY,
		EntityType.Builder.of(RootcoilSerpent::new,MobCategory.MONSTER).sized(3.0F,3.0F).eyeHeight(2.6F).clientTrackingRange(12).notInPeaceful().build(SERPENT_KEY));
	private static final ResourceKey<EntityType<?>> BELL_WARDEN_KEY=ResourceKey.create(Registries.ENTITY_TYPE,Wildercord.id("drowned_bell_warden"));
	public static final EntityType<DrownedBellWarden> BELL_WARDEN=Registry.register(BuiltInRegistries.ENTITY_TYPE,BELL_WARDEN_KEY,
		EntityType.Builder.of(DrownedBellWarden::new,MobCategory.MONSTER).sized(2.4F,4.6F).eyeHeight(4.0F).clientTrackingRange(12).notInPeaceful().build(BELL_WARDEN_KEY));
	private static final ResourceKey<EntityType<?>> ASHEN_STAG_KEY=ResourceKey.create(Registries.ENTITY_TYPE,Wildercord.id("ashen_stag"));
	public static final EntityType<AshenStag> ASHEN_STAG=Registry.register(BuiltInRegistries.ENTITY_TYPE,ASHEN_STAG_KEY,
		EntityType.Builder.of(AshenStag::new,MobCategory.MONSTER).sized(2.4F,3.6F).eyeHeight(3.0F).clientTrackingRange(12).fireImmune().notInPeaceful().build(ASHEN_STAG_KEY));
	private static final ResourceKey<EntityType<?>> HOLLOW_MATRON_KEY=ResourceKey.create(Registries.ENTITY_TYPE,Wildercord.id("hollow_matron"));
	public static final EntityType<HollowMatron> HOLLOW_MATRON=Registry.register(BuiltInRegistries.ENTITY_TYPE,HOLLOW_MATRON_KEY,
		EntityType.Builder.of(HollowMatron::new,MobCategory.MONSTER).sized(3.6F,2.2F).eyeHeight(1.4F).clientTrackingRange(12).notInPeaceful().build(HOLLOW_MATRON_KEY));
	private static final ResourceKey<EntityType<?>> FROST_WYRMLING_KEY=ResourceKey.create(Registries.ENTITY_TYPE,Wildercord.id("frost_wyrmling"));
	public static final EntityType<FrostWyrmling> FROST_WYRMLING=Registry.register(BuiltInRegistries.ENTITY_TYPE,FROST_WYRMLING_KEY,
		EntityType.Builder.of(FrostWyrmling::new,MobCategory.MONSTER).sized(2.8F,3.0F).eyeHeight(2.6F).clientTrackingRange(12).notInPeaceful().build(FROST_WYRMLING_KEY));
	private static final ResourceKey<EntityType<?>> LANTERN_BEARER_KEY=ResourceKey.create(Registries.ENTITY_TYPE,Wildercord.id("lantern_bearer"));
	public static final EntityType<LanternBearer> LANTERN_BEARER=Registry.register(BuiltInRegistries.ENTITY_TYPE,LANTERN_BEARER_KEY,
		EntityType.Builder.of(LanternBearer::new,MobCategory.MONSTER).sized(1.4F,3.4F).eyeHeight(3.0F).clientTrackingRange(12).notInPeaceful().build(LANTERN_BEARER_KEY));
	private static final ResourceKey<EntityType<?>> MOJITO_KEY=ResourceKey.create(Registries.ENTITY_TYPE,Wildercord.id("mojito"));
	public static final EntityType<Mojito> MOJITO=Registry.register(BuiltInRegistries.ENTITY_TYPE,MOJITO_KEY,
		EntityType.Builder.of(Mojito::new,MobCategory.MONSTER).sized(3.6F,5.0F).eyeHeight(4.4F).clientTrackingRange(12).notInPeaceful().build(MOJITO_KEY));
	/** Whether guardians wake at all (config: aura_world.guardians). */
	public static boolean enabled(){return dev.wildercord.config.Config.get().auraWorld().guardians();}
	public static void init(){
		FabricDefaultAttributeRegistry.register(SERPENT,RootcoilSerpent.createAttributes());
		FieldGuide.add(new FieldGuide.Entry("wildercord:rootcoil_serpent",FieldGuide.Group.MONSTER,0x4E7A3A));
		FabricDefaultAttributeRegistry.register(BELL_WARDEN,DrownedBellWarden.createAttributes());
		FieldGuide.add(new FieldGuide.Entry("wildercord:drowned_bell_warden",FieldGuide.Group.MONSTER,0x3F8C7A));
		FabricDefaultAttributeRegistry.register(ASHEN_STAG,AshenStag.createAttributes());
		FieldGuide.add(new FieldGuide.Entry("wildercord:ashen_stag",FieldGuide.Group.MONSTER,0xC2541E));
		FabricDefaultAttributeRegistry.register(HOLLOW_MATRON,HollowMatron.createAttributes());HollowMatron.listen();
		FieldGuide.add(new FieldGuide.Entry("wildercord:hollow_matron",FieldGuide.Group.MONSTER,0xB8B0A2));
		FabricDefaultAttributeRegistry.register(FROST_WYRMLING,FrostWyrmling.createAttributes());
		FieldGuide.add(new FieldGuide.Entry("wildercord:frost_wyrmling",FieldGuide.Group.MONSTER,0x8FD3F0));
		FabricDefaultAttributeRegistry.register(LANTERN_BEARER,LanternBearer.createAttributes());
		FieldGuide.add(new FieldGuide.Entry("wildercord:lantern_bearer",FieldGuide.Group.MONSTER,0x6FE8E0));
		FabricDefaultAttributeRegistry.register(MOJITO,Mojito.createAttributes());
		FieldGuide.add(new FieldGuide.Entry("wildercord:mojito",FieldGuide.Group.MONSTER,0x7CFFB2));
	}

	/**
	 * Award every fighter who faced {@code boss} (those on its boss bar, nearby in the lair, or the killer) with
	 * tier-scaled Aura Shards, Mana Crystals, Torn Pages, Manual Pages, and an unlearned Tier III/IV rune.
	 */
	public static void reward(ServerLevel server, Mob boss, BossTier tier, ServerBossEvent bar, DamageSource source) {
		Set<ServerPlayer> winners = new LinkedHashSet<>(bar.getPlayers());
		for (ServerPlayer p : server.players()) {
			if (p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(boss) <= 32 * 32) {
				winners.add(p);
			}
		}
		if (source.getEntity() instanceof ServerPlayer killer && killer.isAlive() && !killer.isSpectator()) {
			winners.add(killer);
		}
		var methods = BreathingMethods.BUILT_IN;
		int minRuneTier = tier.ordinal() >= BossTier.ELDER.ordinal() ? 4 : 3;
		int pages = tier.ordinal() >= BossTier.HEROIC.ordinal() ? 2 : 1;
		for (ServerPlayer p : winners) {
			giveOrDrop(p, new ItemStack(AuraWorld.AURA_SHARD, tier.shards));
			giveOrDrop(p, new ItemStack(WildercordItems.MANA_CRYSTAL, tier.crystals));
			giveOrDrop(p, new ItemStack(WildercordItems.TORN_PAGE, pages));
			for (int i = 0; i < pages && !methods.isEmpty(); i++) {
				var method = methods.get(server.getRandom().nextInt(methods.size()));
				giveOrDrop(p, ManualPageItem.of(AuraWorld.MANUAL_PAGE, method.id()));
			}
			giveOrDrop(p, RuneItem.stack(pickRune(server, p, minRuneTier)));
		}
	}

	private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
		}
	}

	private static RuneDef pickRune(ServerLevel level, ServerPlayer player, int minTier) {
		List<RuneDef> pool = new ArrayList<>();
		for (RuneDef rune : Runes.all()) {
			if (rune.tier() >= minTier && Runes.common(rune) && (player == null || !Spellbooks.knows(player, rune.id()))) {
				pool.add(rune);
			}
		}
		if (pool.isEmpty()) {
			for (RuneDef rune : Runes.all()) {
				if (rune.tier() >= minTier && Runes.common(rune)) {
					pool.add(rune);
				}
			}
		}
		return pool.isEmpty() ? Runes.BOLT : pool.get(level.getRandom().nextInt(pool.size()));
	}
}
