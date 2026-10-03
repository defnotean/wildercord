package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.config.Config;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** Two rare highland animals with grounded habitats, local crowd caps and useful acquisition paths. */
public final class AuraBeasts {
	private AuraBeasts() {}
	public static final EntityType<Stonehorn> STONEHORN=entity("stonehorn",EntityType.Builder.of(Stonehorn::new,MobCategory.CREATURE).sized(1.35F,1.45F).eyeHeight(1.2F).clientTrackingRange(10));
	public static final EntityType<Galeclaw> GALECLAW=entity("galeclaw",EntityType.Builder.of(Galeclaw::new,MobCategory.CREATURE).sized(.95F,1.35F).eyeHeight(1.1F).clientTrackingRange(10));
	public static final Item STONEHORN_PLATE=item("stonehorn_plate",false);
	public static final Item GALECLAW_PLUME=item("galeclaw_plume",false);
	public static final Item BASTION_POULTICE=item("bastion_poultice",true);
	public static final Item RIDGE_WHISTLE=item("ridge_whistle",true);
	private static final Item STONE_EGG=egg("stonehorn",STONEHORN), GALE_EGG=egg("galeclaw",GALECLAW);
	private static <T extends Entity> EntityType<T> entity(String id,EntityType.Builder<T> builder) {
		var key=ResourceKey.create(Registries.ENTITY_TYPE,Wildercord.id(id)); return Registry.register(BuiltInRegistries.ENTITY_TYPE,key,builder.build(key));
	}
	private static Item egg(String id,EntityType<?> type) {
		var key=ResourceKey.create(Registries.ITEM,Wildercord.id(id+"_spawn_egg")); return Registry.register(BuiltInRegistries.ITEM,key,new SpawnEggItem(new Item.Properties().setId(key).spawnEgg(type)));
	}
	private static Item item(String id,boolean usable) {
		var key=ResourceKey.create(Registries.ITEM,Wildercord.id(id));
		return Registry.register(BuiltInRegistries.ITEM,key,new AuraWorld.Lore(id,new Item.Properties().setId(key).stacksTo(id.equals("ridge_whistle")?1:64)) {
			@Override public InteractionResult use(Level level,Player p,InteractionHand hand) {
				if(!usable) return InteractionResult.PASS;
				var stack=p.getItemInHand(hand); if(p.getCooldowns().isOnCooldown(stack)) return InteractionResult.PASS;
				if(level instanceof ServerLevel server) {
					if(id.equals("bastion_poultice")) {
						p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,200,0)); p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,200,0));
						p.getCooldowns().addCooldown(stack,600); if(!p.getAbilities().instabuild) stack.shrink(1);
						Feels.sound(server,p.position(),"aura_stonehorn_forage",.6F,.8F);
					} else {
						for(var beast:server.getEntitiesOfClass(Galeclaw.class,p.getBoundingBox().inflate(16),b->b.isAlive() && p.hasLineOfSight(b))) beast.distract(p.position());
						p.getCooldowns().addCooldown(stack,200); Feels.sound(server,p.position(),"aura_galeclaw_whistle",.8F,1);
					}
				}
				return InteractionResult.SUCCESS;
			}
		});
	}
	public static boolean maySpawn(EntityType<?> type,ServerLevelAccessor level,EntitySpawnReason reason,BlockPos at) {
		if(reason!=EntitySpawnReason.NATURAL && reason!=EntitySpawnReason.CHUNK_GENERATION) return true;
		var ground=TagKey.create(Registries.BLOCK,Wildercord.id("aura_beast_ground"));
		int near=level.getEntities(type,new AABB(at).inflate(96),e->e.isAlive()).size();
		return BeastRules.habitat(Config.get().auraWorld().auraBeasts(),level.getDifficulty()==Difficulty.PEACEFUL,
			level.getBlockState(at.below()).is(ground),level.canSeeSky(at),at.getY(),level.getSeaLevel(),near)
			&& level.getFluidState(at).isEmpty() && level.getBlockState(at).isAir() && level.getBlockState(at.above()).isAir();
	}
	private static <T extends AuraBeast> void spawn(EntityType<T> type,String id,int weight,String... ids) {
		SpawnPlacements.register(type,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(t,l,r,p,random)->maySpawn(t,l,r,p));
		var biomes=Arrays.stream(ids).map(s->ResourceKey.create(Registries.BIOME,Identifier.withDefaultNamespace(s))).collect(java.util.stream.Collectors.toSet());
		BiomeModifications.create(Wildercord.id("aura_beasts/"+id)).add(ModificationPhase.ADDITIONS,BiomeSelectors.includeByKey(biomes),c->{
			if(Config.get().auraWorld().auraBeasts()) c.getMobSpawnSettings().addSpawn(MobCategory.CREATURE,new MobSpawnSettings.SpawnerData(type,UniformInt.of(1,1)),weight);
		});
	}
	public static void init() {
		FabricDefaultAttributeRegistry.register(STONEHORN,AuraBeast.attributes(false)); FabricDefaultAttributeRegistry.register(GALECLAW,AuraBeast.attributes(true));
		FieldGuide.add(new FieldGuide.Entry("wildercord:stonehorn",FieldGuide.Group.WILDLIFE,0xA6AE8B));
		FieldGuide.add(new FieldGuide.Entry("wildercord:galeclaw",FieldGuide.Group.MONSTER,0x8FAAB8));
		spawn(STONEHORN,"stonehorn",3,"meadow","windswept_hills","windswept_gravelly_hills","stony_peaks");
		spawn(GALECLAW,"galeclaw",2,"windswept_hills","windswept_forest","jagged_peaks","frozen_peaks","snowy_slopes","grove");
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o->List.of(STONEHORN_PLATE,GALECLAW_PLUME,BASTION_POULTICE,RIDGE_WHISTLE,STONE_EGG,GALE_EGG).forEach(o::accept));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(o->{o.accept(STONE_EGG);o.accept(GALE_EGG);});
	}
}
