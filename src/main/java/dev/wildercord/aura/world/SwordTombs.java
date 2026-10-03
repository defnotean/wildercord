package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.List;

/** The tomb's authored blocks and guardian; no natural monster spawn entry. */
public final class SwordTombs {
	private SwordTombs(){}
	private static final ResourceKey<Block> GATE_KEY=ResourceKey.create(Registries.BLOCK,Wildercord.id("intent_gate")),RELIQUARY_KEY=ResourceKey.create(Registries.BLOCK,Wildercord.id("tomb_reliquary"));
	public static final IntentGate GATE=Registry.register(BuiltInRegistries.BLOCK,GATE_KEY,new IntentGate(BlockBehaviour.Properties.of().setId(GATE_KEY).strength(-1,3600000).sound(SoundType.DEEPSLATE).noLootTable()));
	public static final TombReliquary RELIQUARY=Registry.register(BuiltInRegistries.BLOCK,RELIQUARY_KEY,new TombReliquary(BlockBehaviour.Properties.of().setId(RELIQUARY_KEY).strength(-1,3600000).sound(SoundType.STONE).noLootTable().noOcclusion()));
	public static final BlockEntityType<TombReliquaryEntity> RELIQUARY_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Wildercord.id("tomb_reliquary"),FabricBlockEntityTypeBuilder.create(TombReliquaryEntity::new,RELIQUARY).build());
	private static final ResourceKey<EntityType<?>> KEEPER_KEY=ResourceKey.create(Registries.ENTITY_TYPE,Wildercord.id("gravekeeper"));
	public static final EntityType<Gravekeeper> KEEPER=Registry.register(BuiltInRegistries.ENTITY_TYPE,KEEPER_KEY,EntityType.Builder.of(Gravekeeper::new,MobCategory.MONSTER).sized(1.0F,2.9F).eyeHeight(2.35F).clientTrackingRange(10).notInPeaceful().build(KEEPER_KEY));
	public static ItemStack history(){
		var s=new ItemStack(Items.WRITTEN_BOOK);s.set(DataComponents.ITEM_MODEL,Wildercord.id("keeper_testament"));
		s.set(DataComponents.WRITTEN_BOOK_CONTENT,new WrittenBookContent(Filterable.passThrough("The Keeper's Testament"),"The Marchkeepers",0,List.of(
			Filterable.passThrough(Component.translatable("book.wildercord.keeper.1")),Filterable.passThrough(Component.translatable("book.wildercord.keeper.2")),Filterable.passThrough(Component.translatable("book.wildercord.keeper.3"))),true));return s;
	}
	public static void init(){
		FabricDefaultAttributeRegistry.register(KEEPER,Gravekeeper.createAttributes());
		FieldGuide.add(new FieldGuide.Entry("wildercord:gravekeeper",FieldGuide.Group.MONSTER,0xBDA879));
		UseBlockCallback.EVENT.register((player,level,hand,hit)->{
			var state=level.getBlockState(hit.getBlockPos());
			if(!state.is(GATE) && !state.is(RELIQUARY))return InteractionResult.PASS;
			if(player instanceof ServerPlayer p){
				if(state.is(GATE) && dev.wildercord.config.Config.get().auraWorld().swordTombs())IntentGate.use(p,hit.getBlockPos());
				else if(level.getBlockEntity(hit.getBlockPos()) instanceof TombReliquaryEntity t)t.use(p);
			}
			return InteractionResult.SUCCESS;
		});
	}
}
