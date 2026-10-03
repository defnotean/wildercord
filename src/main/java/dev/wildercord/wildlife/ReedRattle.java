package dev.wildercord.wildlife;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.cast.feel.Feels;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;

/** A physical warning-window answer; neither a spell nor a tame, with saved player and crab rests. */
public final class ReedRattle {
    private ReedRattle() {}
    public static final int REST_TICKS = 400, CALM_TICKS = 120, USES = 48;
    public static final double REACH = 3;
    private static final ResourceKey<Item> KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("reed_rattle"));
    public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, KEY,
        new AuraWorld.Lore("reed_rattle", new Item.Properties().setId(KEY).durability(USES)));
    public static final AttachmentType<Long> READY = AttachmentRegistry.create(Wildercord.id("reed_rattle_ready"),
        b -> b.initializer(() -> 0L).persistent(Codec.LONG).copyOnDeath());
    private static long clock(ServerPlayer p) { return p.level().getServer().overworld().getGameTime(); }
    private static boolean refuse(ServerPlayer p, String reason, Object... args) {
        p.sendOverlayMessage(Component.translatable("message.wildercord.reed_rattle." + reason, args));
        return false;
    }

    /** The server alone admits use; failure spends neither durability nor either saved clock. */
    public static boolean soothe(ServerPlayer p, InteractionHand hand, ReedbackCrab crab) {
        var stack = p.getItemInHand(hand);
        if (!stack.is(ITEM) || p.isSpectator() || !p.isAlive() || crab.level() != p.level() || !crab.isAlive()) return false;
        if (!p.isShiftKeyDown()) return refuse(p, "crouch");
        if (p.distanceToSqr(crab) > REACH * REACH || !p.hasLineOfSight(crab)) return refuse(p, "reach");
        long now = clock(p), remaining = p.getAttachedOrElse(READY, 0L) - now;
        if (remaining > 0) return refuse(p, "rest", (remaining + 19) / 20);
        if (crab.pose() != ReedbackCrab.WARNING) return refuse(p, "warning");
        if (!crab.answerRattle()) return refuse(p, "crab_rest");
        p.setAttached(READY, now + REST_TICKS);
        p.getCooldowns().addCooldown(stack, REST_TICKS);
        stack.hurtAndBreak(1, p, hand.asEquipmentSlot());
        Feels.sound(p.level(), crab.position(), "wetland_reed_rattle", .6F, 1);
        p.sendOverlayMessage(Component.translatable("message.wildercord.reed_rattle.settle"));
        return true;
    }

    public static void init() {
        CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord")))
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
