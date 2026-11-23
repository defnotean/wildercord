package dev.wildercord.wildlife;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.cast.feel.Feels;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.*;
import net.minecraft.server.level.ServerLevel;
import dev.wildercord.compat.McAttachments;

/** A physical warning-window answer; neither a spell nor a tame, with saved player and crab rests. */
public final class ReedRattle {
    private ReedRattle() {}
    public static final int REST_TICKS = 400, CALM_TICKS = 120, USES = 48;
    public static final double REACH = 3;
    private static final ResourceKey<Item> KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("reed_rattle"));
    public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, KEY,
        new AuraWorld.Lore("reed_rattle", new Item.Properties());
        Feels.sound(((ServerLevel) p.level()), crab.position(), "wetland_reed_rattle", .6F, 1);
        p.displayClientMessage(Component.translatable("message.wildercord.reed_rattle.settle"), true);
        return true;
    }

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord")))
            .register(output -> output.accept(ITEM));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var p = handler.player;
            long remaining = p.getAttachedOrElse(READY, 0L) - clock(p);
            if (remaining > 0) p.getCooldowns().addCooldown(new ItemStack(ITEM), (int) Math.min(remaining, REST_TICKS));
        });
        UseEntityCallback.EVENT.register((p, level, hand, entity, hit) -> {
            if (!(entity instanceof ReedbackCrab crab) || !p.getItemInHand(hand).is(ITEM) || p.isSpectator()) return InteractionResult.PASS;
            if (p instanceof ServerPlayer serverPlayer) soothe(serverPlayer, hand, crab);
            // A client success forwards the real interaction packet; admission and wear remain on the server.
            return InteractionResult.SUCCESS;
        });
    }
}
