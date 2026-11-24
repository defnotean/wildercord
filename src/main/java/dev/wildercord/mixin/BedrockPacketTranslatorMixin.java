package dev.wildercord.mixin;

import dev.wildercord.bedrock.BedrockMappings;
import dev.wildercord.bedrock.BedrockPlayers;
import dev.wildercord.bedrock.BedrockTextTranslator;
import dev.wildercord.bedrock.BedrockVfxBridge;
import dev.wildercord.menu.WildercordMenus;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.datafixers.util.Pair;

/**
 * Translates outbound server -> client packets exclusively for Bedrock/Console players (Geyser/Floodgate)
 * so Geyser's vanilla Java protocol decoder never encounters unknown modded registries or custom codecs.
 * Java players bypass this completely with zero allocations.
 */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class BedrockPacketTranslatorMixin {
	@Unique
	private static final ThreadLocal<Boolean> WILDERCORD_TRANSLATING = ThreadLocal.withInitial(() -> false);

	@Inject(
		method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void wildercord$translateOutboundForBedrock(Packet<?> packet, ChannelFutureListener listener, CallbackInfo ci) {
		if (Boolean.TRUE.equals(WILDERCORD_TRANSLATING.get())) {
			return;
		}
		if (!((Object) this instanceof ServerGamePacketListenerImpl sgp)) {
			return;
		}
		ServerPlayer player = sgp.player;
		if (!BedrockPlayers.isBedrock(player)) {
			return;
		}

		// Handle section batch updates that may expand to block updates
		if (packet instanceof ClientboundSectionBlocksUpdatePacket secUpdate) {
			Packet<?> converted = wildercord$translateSectionUpdate(secUpdate);
			if (converted != packet) {
				ci.cancel();
				wildercord$sendTranslated(converted, listener);
			}
			return;
		}

		Packet<?> translated = wildercord$translatePacket(packet, player);
		if (translated == null) {
			ci.cancel();
		} else if (translated != packet) {
			ci.cancel();
			wildercord$sendTranslated(translated, listener);
		}
	}

	@Unique
	private void wildercord$sendTranslated(Packet<?> rewritten, ChannelFutureListener listener) {
		WILDERCORD_TRANSLATING.set(true);
		try {
			((ServerCommonPacketListenerImpl) (Object) this).send(rewritten, listener);
		} finally {
			WILDERCORD_TRANSLATING.set(false);
		}
	}

	@Unique
	private Packet<?> wildercord$translatePacket(Packet<?> packet, ServerPlayer player) {
		if (packet instanceof ClientboundCustomPayloadPacket payloadPacket) {
			Identifier id = payloadPacket.payload().type().id();
			if ("wildercord".equals(id.getNamespace())) {
				return BedrockVfxBridge.translateCustomPayload(payloadPacket.payload(), player);
			}
			if (id.getNamespace().startsWith("fabric")) {
				return null;
			}
			return packet;
		}

		if (packet instanceof ClientboundBundlePacket bundle) {
			List<Packet<? super ClientGamePacketListener>> rewritten = new ArrayList<>();
			boolean changed = false;
			for (Packet<? super ClientGamePacketListener> sub : bundle.subPackets()) {
				Packet<?> next = wildercord$translatePacket(sub, player);
				if (next == null) {
					changed = true;
				} else if (next instanceof ClientboundBundlePacket inner) {
					changed = true;
					for (Packet<? super ClientGamePacketListener> innerSub : inner.subPackets()) {
						rewritten.add(innerSub);
					}
				} else {
					if (next != sub) {
						changed = true;
					}
					@SuppressWarnings("unchecked")
					Packet<? super ClientGamePacketListener> cast = (Packet<? super ClientGamePacketListener>) next;
					rewritten.add(cast);
				}
			}
			if (!changed) {
				return packet;
			}
			return rewritten.isEmpty() ? null : new ClientboundBundlePacket(rewritten);
		}

		if (packet instanceof ClientboundContainerSetSlotPacket slotPacket) {
			// Do not sync Fabric extra inventory slots >= 46 to Bedrock's 46-slot PlayerInventory
			if (slotPacket.getContainerId() == 0 && slotPacket.getSlot() >= 46) {
				return null;
			}
			ItemStack orig = slotPacket.getItem();
			ItemStack mapped = BedrockMappings.translateItem(orig);
			if (mapped != orig) {
				return new ClientboundContainerSetSlotPacket(slotPacket.getContainerId(), slotPacket.getStateId(), slotPacket.getSlot(), mapped);
			}
			return packet;
		}

		if (packet instanceof ClientboundContainerSetContentPacket contentPacket) {
			List<ItemStack> origItems = contentPacket.items();
			List<ItemStack> mappedItems = new ArrayList<>(origItems.size());
			boolean changed = false;
			for (int i = 0; i < origItems.size(); i++) {
				// Trim extra Fabric inventory slots >= 46 for Player Inventory (containerId 0)
				if (contentPacket.containerId() == 0 && i >= 46) {
					changed = true;
					break;
				}
				ItemStack st = origItems.get(i);
				ItemStack tr = BedrockMappings.translateItem(st);
				if (tr != st) {
					changed = true;
				}
				mappedItems.add(tr);
			}
			ItemStack carried = BedrockMappings.translateItem(contentPacket.carriedItem());
			if (carried != contentPacket.carriedItem()) {
				changed = true;
			}
			return changed ? new ClientboundContainerSetContentPacket(contentPacket.containerId(), contentPacket.stateId(), mappedItems, carried) : packet;
		}

		if (packet instanceof ClientboundSetCursorItemPacket cursorPacket) {
			ItemStack mapped = BedrockMappings.translateItem(cursorPacket.contents());
			return mapped != cursorPacket.contents() ? new ClientboundSetCursorItemPacket(mapped) : packet;
		}

		if (packet instanceof ClientboundSetPlayerInventoryPacket invPacket) {
			if (invPacket.slot() >= 46) {
				return null;
			}
			ItemStack mapped = BedrockMappings.translateItem(invPacket.contents());
			return mapped != invPacket.contents() ? new ClientboundSetPlayerInventoryPacket(invPacket.slot(), mapped) : packet;
		}

		if (packet instanceof ClientboundSetEquipmentPacket eqPacket) {
			List<Pair<EquipmentSlot, ItemStack>> orig = eqPacket.getSlots();
			List<Pair<EquipmentSlot, ItemStack>> mapped = new ArrayList<>(orig.size());
			boolean changed = false;
			for (Pair<EquipmentSlot, ItemStack> p : orig) {
				ItemStack tr = BedrockMappings.translateItem(p.getSecond());
				if (tr != p.getSecond()) {
					changed = true;
				}
				mapped.add(Pair.of(p.getFirst(), tr));
			}
			return changed ? new ClientboundSetEquipmentPacket(eqPacket.getEntity(), mapped) : packet;
		}

		if (packet instanceof ClientboundSetEntityDataPacket dataPacket) {
			return wildercord$translateEntityData(dataPacket, player);
		}

		if (packet instanceof ClientboundAddEntityPacket addPacket) {
			EntityType<?> origType = addPacket.getType();
			EntityType<?> mappedType = BedrockMappings.translateEntityType(origType);
			if (mappedType != origType) {
				return new ClientboundAddEntityPacket(
					addPacket.getId(),
					addPacket.getUUID(),
					addPacket.getX(),
					addPacket.getY(),
					addPacket.getZ(),
					addPacket.getXRot(),
					addPacket.getYRot(),
					mappedType,
					addPacket.getData(),
					addPacket.getMovement(),
					addPacket.getYHeadRot()
				);
			}
			return packet;
		}

		if (packet instanceof ClientboundBlockUpdatePacket blockPacket) {
			BlockState orig = blockPacket.getBlockState();
			if (BedrockMappings.isModdedBlock(orig)) {
				return new ClientboundBlockUpdatePacket(blockPacket.getPos(), BedrockMappings.translateBlockState(orig));
			}
			return packet;
		}

		if (packet instanceof ClientboundBlockEntityDataPacket bePacket) {
			if (BedrockMappings.isModdedBlockEntity(bePacket.getType())) {
				return null;
			}
			return packet;
		}

		if (packet instanceof ClientboundLevelChunkWithLightPacket chunkPacket) {
			return wildercord$translateChunkPacket(chunkPacket, player);
		}

		if (packet instanceof ClientboundLevelParticlesPacket partPacket) {
			return BedrockVfxBridge.translateParticlePacket(partPacket);
		}

		if (packet instanceof ClientboundSoundPacket soundPacket) {
			Holder<SoundEvent> mapped = BedrockMappings.translateSound(soundPacket.getSound());
			if (mapped != soundPacket.getSound()) {
				return new ClientboundSoundPacket(
					mapped,
					soundPacket.getSource(),
					soundPacket.getX(),
					soundPacket.getY(),
					soundPacket.getZ(),
					soundPacket.getVolume(),
					soundPacket.getPitch(),
					soundPacket.getSeed()
				);
			}
			return packet;
		}

		if (packet instanceof ClientboundSoundEntityPacket soundEntity) {
			Holder<SoundEvent> mapped = BedrockMappings.translateSound(soundEntity.getSound());
			if (mapped != soundEntity.getSound()) {
				// Broadcast at listener's position if direct entity lookup isn't needed for modded registry translation
				return new ClientboundSoundPacket(
					mapped,
					soundEntity.getSource(),
					player.getX(),
					player.getY(),
					player.getZ(),
					soundEntity.getVolume(),
					soundEntity.getPitch(),
					soundEntity.getSeed()
				);
			}
			return packet;
		}

		if (packet instanceof ClientboundUpdateMobEffectPacket mobEffect) {
			Holder<MobEffect> mapped = BedrockMappings.translateMobEffect(mobEffect.getEffect());
			if (mapped != mobEffect.getEffect()) {
				return new ClientboundUpdateMobEffectPacket(
					mobEffect.getEntityId(),
					new MobEffectInstance(
						mapped,
						mobEffect.getEffectDurationTicks(),
						mobEffect.getEffectAmplifier(),
						mobEffect.isEffectAmbient(),
						mobEffect.isEffectVisible(),
						mobEffect.effectShowsIcon()
					),
					mobEffect.shouldBlend()
				);
			}
			return packet;
		}

		if (packet instanceof ClientboundRemoveMobEffectPacket removeEffect) {
			Holder<MobEffect> mapped = BedrockMappings.translateMobEffect(removeEffect.effect());
			return mapped != removeEffect.effect() ? new ClientboundRemoveMobEffectPacket(removeEffect.entityId(), mapped) : packet;
		}

		if (packet instanceof ClientboundOpenScreenPacket screenPacket) {
			MenuType<?> type = screenPacket.getType();
			Component origTitle = screenPacket.getTitle();
			Component trTitle = BedrockTextTranslator.translate(origTitle);
			boolean moddedMenu = BedrockMappings.isModdedId(BuiltInRegistries.MENU.getKey(type));
			if (moddedMenu || trTitle != origTitle) {
				MenuType<?> vanillaMenu = type;
				if (moddedMenu) {
					if (type == WildercordMenus.FUSION_ALTAR) {
						vanillaMenu = MenuType.HOPPER;
					} else if (type == WildercordMenus.BACKPACK) {
						int fallbackRows = player.containerMenu != null ? (player.containerMenu.slots.size() - 36) / 9 : 3;
						int rows = BedrockMappings.consumePendingMenuRows(player.getUUID(), fallbackRows);
						vanillaMenu = switch (rows) {
							case 1 -> MenuType.GENERIC_9x1;
							case 2 -> MenuType.GENERIC_9x2;
							case 3 -> MenuType.GENERIC_9x3;
							case 4 -> MenuType.GENERIC_9x4;
							case 5 -> MenuType.GENERIC_9x5;
							default -> MenuType.GENERIC_9x6;
						};
					} else {
						vanillaMenu = MenuType.GENERIC_9x3;
					}
				}
				return new ClientboundOpenScreenPacket(screenPacket.getContainerId(), vanillaMenu, trTitle);
			}
			return packet;
		}

		if (packet instanceof ClientboundMerchantOffersPacket offersPacket) {
			return wildercord$translateMerchantOffers(offersPacket);
		}

		if (packet instanceof ClientboundSystemChatPacket chatPacket) {
			Component tr = BedrockTextTranslator.translate(chatPacket.content());
			return tr != chatPacket.content() ? new ClientboundSystemChatPacket(tr, chatPacket.overlay()) : packet;
		}

		if (packet instanceof ClientboundSetActionBarTextPacket barPacket) {
			Component tr = BedrockTextTranslator.translate(barPacket.text());
			return tr != barPacket.text() ? new ClientboundSetActionBarTextPacket(tr) : packet;
		}

		if (packet instanceof ClientboundSetTitleTextPacket titlePacket) {
			Component tr = BedrockTextTranslator.translate(titlePacket.text());
			return tr != titlePacket.text() ? new ClientboundSetTitleTextPacket(tr) : packet;
		}

		if (packet instanceof ClientboundSetSubtitleTextPacket subPacket) {
			Component tr = BedrockTextTranslator.translate(subPacket.text());
			return tr != subPacket.text() ? new ClientboundSetSubtitleTextPacket(tr) : packet;
		}

		if (packet instanceof ClientboundBossEventPacket bossPacket) {
			return wildercord$translateBossEvent(bossPacket);
		}

		if (packet instanceof ClientboundLevelEventPacket levelEvent && levelEvent.getType() == 2001) {
			BlockState orig = Block.stateById(levelEvent.getData());
			if (BedrockMappings.isModdedBlock(orig)) {
				return new ClientboundLevelEventPacket(
					2001,
					levelEvent.getPos(),
					Block.getId(BedrockMappings.translateBlockState(orig)),
					levelEvent.isGlobalEvent()
				);
			}
			return packet;
		}

		if (packet instanceof ClientboundExplodePacket explodePacket) {
			ParticleOptions ep = BedrockMappings.translateParticle(explodePacket.explosionParticle());
			Holder<SoundEvent> es = BedrockMappings.translateSound(explodePacket.explosionSound());
			if (ep != explodePacket.explosionParticle() || es != explodePacket.explosionSound()) {
				return new ClientboundExplodePacket(
					explodePacket.center(),
					explodePacket.radius(),
					explodePacket.blockCount(),
					explodePacket.playerKnockback(),
					ep,
					es,
					explodePacket.blockParticles(),
					explodePacket.playSound()
				);
			}
			return packet;
		}

		return packet;
	}

	@Unique
	private Packet<?> wildercord$translateBossEvent(ClientboundBossEventPacket packet) {
		ClientboundBossEventPacket[] replaced = new ClientboundBossEventPacket[1];
		packet.dispatch(new ClientboundBossEventPacket.Handler() {
			@Override
			public void add(
				java.util.UUID id,
				Component name,
				float progress,
				BossEvent.BossBarColor color,
				BossEvent.BossBarOverlay overlay,
				boolean darkenScreen,
				boolean playBossMusic,
				boolean createWorldFog
			) {
				Component tr = BedrockTextTranslator.translate(name);
				if (tr != name) {
					BossEvent ev = new BossEvent(id, tr, color, overlay) {};
					ev.setProgress(progress);
					ev.setDarkenScreen(darkenScreen);
					ev.setPlayBossMusic(playBossMusic);
					ev.setCreateWorldFog(createWorldFog);
					replaced[0] = ClientboundBossEventPacket.createAddPacket(ev);
				}
			}

			@Override
			public void updateName(java.util.UUID id, Component name) {
				Component tr = BedrockTextTranslator.translate(name);
				if (tr != name) {
					BossEvent ev = new BossEvent(id, tr, BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS) {};
					replaced[0] = ClientboundBossEventPacket.createUpdateNamePacket(ev);
				}
			}
		});
		return replaced[0] != null ? replaced[0] : packet;
	}

	@Unique
	private Packet<?> wildercord$translateSectionUpdate(ClientboundSectionBlocksUpdatePacket packet) {
		SectionBlocksUpdateAccessor acc = (SectionBlocksUpdateAccessor) packet;
		BlockState[] states = acc.wildercord$states();
		boolean anyModded = false;
		for (BlockState s : states) {
			if (BedrockMappings.isModdedBlock(s)) {
				anyModded = true;
				break;
			}
		}
		if (!anyModded) {
			return packet;
		}
		SectionPos sec = acc.wildercord$sectionPos();
		short[] positions = acc.wildercord$positions();
		List<Packet<? super ClientGamePacketListener>> updates = new ArrayList<>(positions.length);
		for (int i = 0; i < positions.length; i++) {
			BlockPos pos = sec.relativeToBlockPos(positions[i]);
			updates.add(new ClientboundBlockUpdatePacket(pos, BedrockMappings.translateBlockState(states[i])));
		}
		return new ClientboundBundlePacket(updates);
	}

	@Unique
	@SuppressWarnings("unchecked")
	private ClientboundSetEntityDataPacket wildercord$translateEntityData(ClientboundSetEntityDataPacket packet, ServerPlayer player) {
		Entity entity = player.level().getEntity(packet.id());
		boolean stripModdedIds = entity != null && BedrockMappings.isModdedEntity(entity.getType());
		List<SynchedEntityData.DataValue<?>> items = packet.packedItems();
		List<SynchedEntityData.DataValue<?>> out = null;
		for (int i = 0; i < items.size(); i++) {
			SynchedEntityData.DataValue<?> dv = items.get(i);
			if (stripModdedIds && !BedrockMappings.shouldKeepModdedEntityDataId(entity instanceof LivingEntity, dv.id())) {
				if (out == null) {
					out = new ArrayList<>(items.subList(0, i));
				}
				continue;
			}
			SynchedEntityData.DataValue<?> next = dv;
			if (dv.serializer() == EntityDataSerializers.ITEM_STACK && dv.value() instanceof ItemStack st) {
				ItemStack tr = BedrockMappings.translateItem(st);
				if (tr != st) {
					next = new SynchedEntityData.DataValue<>(dv.id(), EntityDataSerializers.ITEM_STACK, tr);
				}
			} else if (dv.serializer() == EntityDataSerializers.BLOCK_STATE && dv.value() instanceof BlockState bs && BedrockMappings.isModdedBlock(bs)) {
				next = new SynchedEntityData.DataValue<>(dv.id(), EntityDataSerializers.BLOCK_STATE, BedrockMappings.translateBlockState(bs));
			} else if (dv.serializer() == EntityDataSerializers.OPTIONAL_BLOCK_STATE && dv.value() instanceof Optional<?> opt && opt.isPresent() && opt.get() instanceof BlockState bs && BedrockMappings.isModdedBlock(bs)) {
				next = new SynchedEntityData.DataValue<>(dv.id(), EntityDataSerializers.OPTIONAL_BLOCK_STATE, Optional.of(BedrockMappings.translateBlockState(bs)));
			} else if (dv.serializer() == EntityDataSerializers.PARTICLE && dv.value() instanceof ParticleOptions po) {
				ParticleOptions tr = BedrockMappings.translateParticle(po);
				if (tr != po) {
					next = new SynchedEntityData.DataValue<>(dv.id(), EntityDataSerializers.PARTICLE, tr);
				}
			} else if (dv.serializer() == EntityDataSerializers.COMPONENT && dv.value() instanceof Component comp) {
				Component tr = BedrockTextTranslator.translate(comp);
				if (tr != comp) {
					next = new SynchedEntityData.DataValue<>(dv.id(), EntityDataSerializers.COMPONENT, tr);
				}
			} else if (dv.serializer() == EntityDataSerializers.OPTIONAL_COMPONENT && dv.value() instanceof Optional<?> opt && opt.isPresent() && opt.get() instanceof Component comp) {
				Component tr = BedrockTextTranslator.translate(comp);
				if (tr != comp) {
					next = new SynchedEntityData.DataValue<>(dv.id(), EntityDataSerializers.OPTIONAL_COMPONENT, Optional.of(tr));
				}
			} else if (dv.serializer() == EntityDataSerializers.PARTICLES && dv.value() instanceof List<?> list) {
				List<ParticleOptions> raw = (List<ParticleOptions>) list;
				List<ParticleOptions> trList = new ArrayList<>(raw.size());
				boolean pChanged = false;
				for (ParticleOptions po : raw) {
					ParticleOptions tr = BedrockMappings.translateParticle(po);
					if (tr != po) pChanged = true;
					trList.add(tr);
				}
				if (pChanged) {
					next = new SynchedEntityData.DataValue<>(dv.id(), EntityDataSerializers.PARTICLES, trList);
				}
			}
			if (out != null) {
				out.add(next);
			} else if (next != dv) {
				out = new ArrayList<>(items.subList(0, i));
				out.add(next);
			}
		}
		if (out == null) {
			return packet;
		}
		return out.isEmpty() ? null : new ClientboundSetEntityDataPacket(packet.id(), out);
	}

	@Unique
	private ClientboundMerchantOffersPacket wildercord$translateMerchantOffers(ClientboundMerchantOffersPacket packet) {
		MerchantOffers orig = packet.getOffers();
		boolean changed = false;
		MerchantOffers out = new MerchantOffers();
		for (MerchantOffer offer : orig) {
			ItemStack res = BedrockMappings.translateItem(offer.getResult());
			ItemStack costAStack = BedrockMappings.translateItem(offer.getBaseCostA());
			ItemStack costBStack = BedrockMappings.translateItem(offer.getCostB());
			if (res != offer.getResult() || costAStack != offer.getBaseCostA() || costBStack != offer.getCostB()) {
				changed = true;
				ItemCost costA = new ItemCost(costAStack.typeHolder(), costAStack.getCount(), DataComponentExactPredicate.EMPTY, costAStack);
				Optional<ItemCost> costB = costBStack.isEmpty()
					? Optional.empty()
					: Optional.of(new ItemCost(costBStack.typeHolder(), costBStack.getCount(), DataComponentExactPredicate.EMPTY, costBStack));
				MerchantOffer rebuilt = new MerchantOffer(
					costA, costB, res,
					offer.getUses(), offer.getMaxUses(), offer.getXp(),
					offer.getPriceMultiplier(), offer.getDemand()
				);
				rebuilt.setSpecialPriceDiff(offer.getSpecialPriceDiff());
				out.add(rebuilt);
			} else {
				out.add(offer);
			}
		}
		return changed
			? new ClientboundMerchantOffersPacket(packet.getContainerId(), out, packet.getVillagerLevel(), packet.getVillagerXp(), packet.showProgress(), packet.canRestock())
			: packet;
	}

	@Unique
	private ClientboundLevelChunkWithLightPacket wildercord$translateChunkPacket(ClientboundLevelChunkWithLightPacket packet, ServerPlayer player) {
		LevelChunk chunk = player.level().getChunkSource().getChunkNow(packet.x(), packet.z());
		if (chunk == null) {
			return packet;
		}

		LevelChunkSection[] sections = chunk.getSections();
		boolean hasModdedSection = false;
		for (LevelChunkSection sec : sections) {
			if (!sec.hasOnlyAir() && sec.maybeHas(BedrockMappings::isModdedBlock)) {
				hasModdedSection = true;
				break;
			}
		}

		ChunkPacketDataAccessor dataAcc = (ChunkPacketDataAccessor) packet.chunkData();
		List<?> origBe = dataAcc.wildercord$blockEntitiesData();
		boolean hasModdedBe = false;
		for (Object entry : origBe) {
			if (BedrockMappings.isModdedBlockEntity(((BlockEntityInfoAccessor) entry).wildercord$type())) {
				hasModdedBe = true;
				break;
			}
		}

		if (!hasModdedSection && !hasModdedBe) {
			return packet;
		}

		// Rebuild section buffer only when a chunk actually holds modded blocks
		FriendlyByteBuf outBuf = new FriendlyByteBuf(Unpooled.buffer());
		try {
			for (LevelChunkSection sec : sections) {
				if (!sec.hasOnlyAir() && sec.maybeHas(BedrockMappings::isModdedBlock)) {
					PalettedContainer<BlockState> copiedStates = sec.getStates().copy();
					for (int y = 0; y < 16; y++) {
						for (int z = 0; z < 16; z++) {
							for (int x = 0; x < 16; x++) {
								BlockState s = copiedStates.get(x, y, z);
								if (BedrockMappings.isModdedBlock(s)) {
									copiedStates.set(x, y, z, BedrockMappings.translateBlockState(s));
								}
							}
						}
					}
					LevelChunkSection translatedSec = new LevelChunkSection(copiedStates, sec.getBiomes());
					translatedSec.recalcBlockCounts();
					translatedSec.write(outBuf);
				} else {
					sec.write(outBuf);
				}
			}
			byte[] bytes = new byte[outBuf.readableBytes()];
			outBuf.readBytes(bytes);

			List<?> beList = origBe;
			if (hasModdedBe) {
				List<Object> filtered = new ArrayList<>(origBe.size());
				for (Object entry : origBe) {
					if (!BedrockMappings.isModdedBlockEntity(((BlockEntityInfoAccessor) entry).wildercord$type())) {
						filtered.add(entry);
					}
				}
				beList = filtered;
			}

			ClientboundLevelChunkPacketData newData = ChunkPacketDataAccessor.wildercord$create(
				dataAcc.wildercord$heightmaps(),
				bytes,
				beList
			);
			return new ClientboundLevelChunkWithLightPacket(packet.x(), packet.z(), newData, packet.lightData());
		} finally {
			outBuf.release();
		}
	}
}
