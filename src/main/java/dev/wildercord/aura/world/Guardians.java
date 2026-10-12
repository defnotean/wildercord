package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;

/** The guardians of the lairs: bosses bound to the place they were generated in, with no natural spawn entry. */
public final class Guardians {
	private Guardians(){}
	private static final ResourceKey<EntityType<?>> SERPENT_KEY=ResourceKey.create(Registries.ENTITY_TYPE,Wildercord.id("rootcoil_serpent"));
	public static final EntityType<RootcoilSerpent> SERPENT=Registry.register(BuiltInRegistries.ENTITY_TYPE,SERPENT_KEY,
		EntityType.Builder.of(RootcoilSerpent::new,MobCategory.MONSTER).sized(3.0F,3.0F).eyeHeight(2.6F).clientTrackingRange(12).notInPeaceful().build(SERPENT_KEY));
	private static final ResourceKey<EntityType<?>> BELL_WARDEN_KEY=ResourceKey.create(Registries.ENTITY_TYPE,Wildercord.id("drowned_bell_warden"));
	public static final EntityType<DrownedBellWarden> BELL_WARDEN=Registry.register(BuiltInRegistries.ENTITY_TYPE,BELL_WARDEN_KEY,
		EntityType.Builder.of(DrownedBellWarden::new,MobCategory.MONSTER).sized(2.4F,4.6F).eyeHeight(4.0F).clientTrackingRange(12).notInPeaceful().build(BELL_WARDEN_KEY));
	/** Whether guardians wake at all (config: aura_world.guardians). */
	public static boolean enabled(){return dev.wildercord.config.Config.get().auraWorld().guardians();}
	public static void init(){
		FabricDefaultAttributeRegistry.register(SERPENT,RootcoilSerpent.createAttributes());
		FieldGuide.add(new FieldGuide.Entry("wildercord:rootcoil_serpent",FieldGuide.Group.MONSTER,0x4E7A3A));
		FabricDefaultAttributeRegistry.register(BELL_WARDEN,DrownedBellWarden.createAttributes());
		FieldGuide.add(new FieldGuide.Entry("wildercord:drowned_bell_warden",FieldGuide.Group.MONSTER,0x3F8C7A));
	}
}
