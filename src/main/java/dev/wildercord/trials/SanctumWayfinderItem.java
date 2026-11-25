package dev.wildercord.trials;

import dev.wildercord.Wildercord;
import dev.wildercord.content.WildercordComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * Survival compass guiding parties toward unvisited Co-Op Trial Sanctums.
 * Sneak-right-click cycles target Sanctum (Leyline / Ashen Crucible / Astral Eclipse);
 * right-click pings the nearest matching Sanctum with bearing, distance, and guide particles.
 */
public final class SanctumWayfinderItem extends Item {
	public static final String[] TARGET_IDS = {
		"trial_leyline_sanctum",
		"trial_ashen_crucible",
		"trial_astral_eclipse",
	};

	public SanctumWayfinderItem(Properties properties) {
		super(properties);
	}

	public static int mode(ItemStack stack) {
		int raw = stack.getOrDefault(WildercordComponents.WAYFINDER_MODE, 0);
		return Math.clamp(raw, 0, TARGET_IDS.length - 1);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(player instanceof ServerPlayer sp)) {
			return InteractionResult.SUCCESS;
		}
		if (sp.isShiftKeyDown()) {
			int next = (mode(stack) + 1) % TARGET_IDS.length;
			stack.set(WildercordComponents.WAYFINDER_MODE, next);
			sp.level().playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.2F + 0.1F * next);
			sp.sendOverlayMessage(Component.translatable("message.wildercord.wayfinder.mode",
				Component.translatable("tooltip.wildercord.wayfinder.target." + TARGET_IDS[next])).withStyle(ChatFormatting.AQUA));
			return InteractionResult.SUCCESS;
		}
		if (sp.getCooldowns().isOnCooldown(stack)) {
			return InteractionResult.FAIL;
		}
		sp.getCooldowns().addCooldown(stack, 40);

		int m = mode(stack);
		String targetId = TARGET_IDS[m];
		ServerLevel server = sp.level();
		var structureReg = server.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		var holderOpt = structureReg.get(ResourceKey.create(Registries.STRUCTURE, Wildercord.id(targetId)));
		if (holderOpt.isEmpty()) {
			sp.sendOverlayMessage(Component.translatable("message.wildercord.wayfinder.unavailable").withStyle(ChatFormatting.GRAY));
			return InteractionResult.FAIL;
		}
		BlockPos here = sp.blockPosition();
		var result = server.getChunkSource().getGenerator().findNearestMapStructure(
			server, HolderSet.direct(holderOpt.get()), here, 120, false);
		if (result == null) {
			sp.sendOverlayMessage(Component.translatable("message.wildercord.wayfinder.missing",
				Component.translatable("tooltip.wildercord.wayfinder.target." + targetId)).withStyle(ChatFormatting.GRAY));
			return InteractionResult.FAIL;
		}
		BlockPos found = result.getFirst();
		int dist = (int) Math.round(Math.hypot(found.getX() - here.getX(), found.getZ() - here.getZ()));
		String compass = cardinal(found.getX() - here.getX(), found.getZ() - here.getZ());

		Vec3 dir = new Vec3(found.getX() - here.getX(), 0.0, found.getZ() - here.getZ()).normalize();
		Vec3 eye = sp.getEyePosition();
		for (int i = 1; i <= 8; i++) {
			Vec3 p = eye.add(dir.scale(i * 0.7));
			server.sendParticles(ParticleTypes.END_ROD, p.x, p.y - 0.15, p.z, 2, 0.05, 0.05, 0.05, 0.01);
		}
		server.playSound(null, here, SoundEvents.AMETHYST_CLUSTER_HIT, SoundSource.PLAYERS, 0.9F, 1.3F);
		sp.sendOverlayMessage(Component.translatable("message.wildercord.wayfinder.found",
			Component.translatable("tooltip.wildercord.wayfinder.target." + targetId), dist, compass).withStyle(ChatFormatting.GOLD));
		return InteractionResult.SUCCESS;
	}

	public static String cardinal(int dx, int dz) {
		double angle = Math.toDegrees(Math.atan2(dz, dx));
		if (angle >= -22.5 && angle < 22.5) return "E";
		if (angle >= 22.5 && angle < 67.5) return "SE";
		if (angle >= 67.5 && angle < 112.5) return "S";
		if (angle >= 112.5 && angle < 157.5) return "SW";
		if (angle >= -67.5 && angle < -22.5) return "NE";
		if (angle >= -112.5 && angle < -67.5) return "N";
		if (angle >= -157.5 && angle < -112.5) return "NW";
		return "W";
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		int m = mode(stack);
		builder.accept(Component.translatable("tooltip.wildercord.wayfinder.target",
			Component.translatable("tooltip.wildercord.wayfinder.target." + TARGET_IDS[m])).withStyle(ChatFormatting.AQUA));
		builder.accept(Component.translatable("tooltip.wildercord.wayfinder.hint").withStyle(ChatFormatting.GRAY));
	}
}
