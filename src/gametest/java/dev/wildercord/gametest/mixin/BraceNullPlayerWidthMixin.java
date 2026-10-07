package dev.wildercord.gametest.mixin;
import dev.wildercord.gametest.BraceNullPlayerWidth;
import net.minecraft.client.model.player.PlayerModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(PlayerModel.class)
public interface BraceNullPlayerWidthMixin extends BraceNullPlayerWidth {
	@Accessor("slim") boolean wildercord$braceNullSlim();
}
