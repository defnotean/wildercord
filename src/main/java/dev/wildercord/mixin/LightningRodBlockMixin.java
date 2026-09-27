package dev.wildercord.mixin;

import dev.wildercord.content.RuneItem;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Drop a Blank Rune next to a lightning rod in a storm: when the rod is struck, the blank becomes a Lightning rune. */
@Mixin(LightningRodBlock.class)
public abstract class LightningRodBlockMixin {
	@Inject(method = "onLightningStrike", at = @At("TAIL"))
	private void wildercord$chargeBlanks(BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		for (ItemEntity item : server.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2.5), e -> e.getItem().is(WildercordItems.BLANK_RUNE))) {
			ItemStack charged = RuneItem.stack(Runes.LIGHTNING);
			charged.setCount(item.getItem().getCount());
			item.setItem(charged);
			item.setGlowingTag(true);
			server.sendParticles(ParticleTypes.ELECTRIC_SPARK, item.getX(), item.getY() + 0.3, item.getZ(), 30, 0.3, 0.3, 0.3, 0.2);
			server.playSound(null, item.getX(), item.getY(), item.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.6F);
		}
	}
}
