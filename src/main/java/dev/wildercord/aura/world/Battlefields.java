package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.config.Config;
import dev.wildercord.player.Heart;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Only requested memories tick: one per online player, no block scans, no forced chunk loads. */
public final class Battlefields {
	private Battlefields() {}
	private static final ResourceKey<net.minecraft.world.level.block.Block> KEY=ResourceKey.create(Registries.BLOCK,Wildercord.id("battlefield_memorial"));
	public static final BattlefieldMemorial MEMORIAL=Registry.register(BuiltInRegistries.BLOCK,KEY,new BattlefieldMemorial(
		BlockBehaviour.Properties.of().setId(KEY).mapColor(MapColor.STONE).sound(SoundType.STONE).strength(3,9).noLootTable().noOcclusion()));
	public static final BlockEntityType<BattlefieldMemoryEntity> MEMORY_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
		Wildercord.id("battlefield_memory"),FabricBlockEntityTypeBuilder.create(BattlefieldMemoryEntity::new,MEMORIAL).build());
	private record Rite(ServerLevel level,BlockPos at,int kind,int held) {}
	private static final Map<UUID,Rite> RITES=new HashMap<>();
	private static final Map<ServerPlayer,Long> READ_AT=new java.util.WeakHashMap<>();

	public static ItemStack book(int kind) {
		var memory=BattlefieldRules.memory(kind);
		var stack=new ItemStack(Items.WRITTEN_BOOK);
		stack.set(DataComponents.WRITTEN_BOOK_CONTENT,new WrittenBookContent(Filterable.passThrough(memory.title()),"The Marchkeepers",0,
			List.of(Filterable.passThrough(Component.translatable("book.wildercord.battlefield."+memory.id()+".1")),
				Filterable.passThrough(Component.translatable("book.wildercord.battlefield."+memory.id()+".2")),
				Filterable.passThrough(Component.translatable("book.wildercord.battlefield."+memory.id()+".3"))),true));
		stack.set(DataComponents.ITEM_MODEL,Wildercord.id("battlefield_"+memory.id()));
		return stack;
	}

	public static void begin(ServerPlayer player,BlockPos pos) {
		if (!Config.get().auraWorld().battlefields() || !player.level().hasChunkAt(pos) || player.isSpectator() || player.distanceToSqr(Vec3.atCenterOf(pos))>BattlefieldRules.REACH*BattlefieldRules.REACH) return;
		if (!(player.level().getBlockEntity(pos) instanceof BattlefieldMemoryEntity marker) || !marker.oldGround()) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.battlefield.empty")); return;
		}
		int kind=marker.getBlockState().getValue(BattlefieldMemorial.KIND);
		if (Heart.discovered(player,BattlefieldRules.memory(kind).key())) {
			long now=player.level().getGameTime();var last=READ_AT.get(player);
			if(last!=null && now>=last && now-last<100) return;
			READ_AT.put(player,now);
			for (int page=1;page<=3;page++) player.sendSystemMessage(Component.translatable("book.wildercord.battlefield."+BattlefieldRules.memory(kind).id()+"."+page)); return;
		}
		if (!holding(player,player.level(),pos)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.battlefield.stance")); return;
		}
		if (RITES.containsKey(player.getUUID())) return;
		RITES.put(player.getUUID(),new Rite(player.level(),pos.immutable(),kind,0));
		Feels.sound(player.level(),Vec3.atCenterOf(pos),"aura_memory_begin",0.6F,1);
	}

	private static boolean holding(ServerPlayer p,ServerLevel level,BlockPos at) {
		return Aura.enabled(p) && !p.isSpectator() && BattlefieldRules.holding(p.isAlive(),Aura.state(p).breathing(),Aura.holdsWeapon(p),p.level()==level,
			p.distanceToSqr(Vec3.atCenterOf(at))) && level.hasChunkAt(at)
			&& level.clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),Vec3.atCenterOf(at),
				net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p)).getBlockPos().equals(at);
	}

	public static void init() {
		net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player,level,hand,hit) -> {
			if (!level.getBlockState(hit.getBlockPos()).is(MEMORIAL)) return net.minecraft.world.InteractionResult.PASS;
			if (player instanceof ServerPlayer p) begin(p,hit.getBlockPos());
			return net.minecraft.world.InteractionResult.SUCCESS;
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {RITES.clear();READ_AT.clear();});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount()%5!=0) return;
			var it=RITES.entrySet().iterator();
			while (it.hasNext()) {
				var entry=it.next();var rite=entry.getValue();var p=server.getPlayerList().getPlayer(entry.getKey());
				if (p==null || !Config.get().auraWorld().battlefields() || !holding(p,rite.level(),rite.at())
					|| !(rite.level().getBlockEntity(rite.at()) instanceof BattlefieldMemoryEntity marker) || !marker.oldGround()
					|| marker.getBlockState().getValue(BattlefieldMemorial.KIND)!=rite.kind()) {
					if (p!=null) p.sendOverlayMessage(Component.translatable("message.wildercord.battlefield.broken"));
					it.remove();continue;
				}
				int held=rite.held()+5;
				if (held<BattlefieldRules.REMEMBER_TICKS) {
					entry.setValue(new Rite(rite.level(),rite.at(),rite.kind(),held));
					if (held%20==0) p.sendOverlayMessage(Component.translatable("message.wildercord.battlefield.listening",held/20,8));
					continue;
				}
				it.remove();var memory=BattlefieldRules.memory(rite.kind());
				if (!Grimoire.unlock(p,memory.key())) continue;
				AuraApi.teachPart(p,memory.part(),"battlefield");
				var book=book(rite.kind());if (!p.getInventory().add(book)) p.drop(book,false,net.minecraft.util.Prediction.SERVER_ONLY);
				AuraFx.groundScar(rite.level(),Vec3.atCenterOf(rite.at()).add(0,-0.5,0),2.8,100,2);
				Feels.sound(rite.level(),Vec3.atCenterOf(rite.at()),switch (rite.kind()) {case 0 -> "aura_memory_broken_line"; case 1 -> "aura_memory_last_shelter"; default -> "aura_memory_returned_step";},0.7F,1);
				p.sendSystemMessage(Component.translatable("message.wildercord.battlefield.remembered",Component.translatable("toast.wildercord.aura.battlefield_"+memory.id())));
			}
		});
	}
}
