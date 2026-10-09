package dev.wildercord.gametest.stonehinge.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.gametest.stonehinge.peer.StoneHingePeerProbe;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
@Mixin(ServerEntity.class)
public abstract class StoneHingePeerTrackerMixin {
    @Shadow @Final private Entity entity;
    @WrapMethod(method = "sendChanges")
    private void stoneHinge$tracker(Operation<Void> original) {
        StoneHingePeerProbe.tracker((ServerEntity) (Object) this, entity, () -> original.call());
    }
}
