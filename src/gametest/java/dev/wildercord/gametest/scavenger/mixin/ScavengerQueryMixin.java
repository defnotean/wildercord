package dev.wildercord.gametest.scavenger.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.aura.world.ScavengerFoodProbe;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(targets="dev.wildercord.aura.world.AuraBeastQueries",remap=false)
public abstract class ScavengerQueryMixin {
 @WrapMethod(method="complete(Lnet/minecraft/server/level/ServerLevel;Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/entity/Entity;Ljava/util/function/Predicate;)Ljava/util/List;",require=1,expect=1,allow=1)
 private static <T extends Entity> List<T> wildercord$scavengerComplete(ServerLevel level,Class<T> type,AABB bounds,Entity source,Predicate<T> eligible,Operation<List<T>> original){return ScavengerFoodProbe.complete(level,type,bounds,source,eligible,original);}
 @WrapOperation(method="complete(Lnet/minecraft/server/level/ServerLevel;Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/entity/Entity;Ljava/util/function/Predicate;)Ljava/util/List;",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;Ljava/util/List;I)V"),require=1,expect=1,allow=1)
 private static <T extends Entity> void wildercord$scavengerRaw(ServerLevel level,EntityTypeTest<Entity,T> type,AABB bounds,Predicate<? super T> eligible,List<? super T> found,int limit,Operation<Void> original){ScavengerFoodProbe.raw(level,type,bounds,eligible,found,limit,original);}
}
