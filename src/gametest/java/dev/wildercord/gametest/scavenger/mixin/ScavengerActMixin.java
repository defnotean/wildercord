package dev.wildercord.gametest.scavenger.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.aura.world.Galeclaw;
import dev.wildercord.aura.world.ScavengerFoodProbe;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value=Galeclaw.class,remap=false)
public abstract class ScavengerActMixin {
 @WrapMethod(method="act(Lnet/minecraft/server/level/ServerLevel;)V",require=1,expect=1,allow=1)
 private void wildercord$scavengerAct(ServerLevel level,Operation<Void> original){ScavengerFoodProbe.act(this,level,original);}
 @WrapOperation(method="act(Lnet/minecraft/server/level/ServerLevel;)V",at=@At(value="INVOKE",target="Ldev/wildercord/aura/world/AuraBeastQueries;complete(Lnet/minecraft/server/level/ServerLevel;Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",ordinal=2),require=1,expect=1,allow=1)
 private <T extends Entity> List<T> wildercord$scavengerQuery(ServerLevel level,Class<T> type,AABB bounds,Predicate<T> eligible,Operation<List<T>> original){return ScavengerFoodProbe.forage(this,level,type,bounds,eligible,original);}
 @WrapOperation(method="act(Lnet/minecraft/server/level/ServerLevel;)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/ai/navigation/PathNavigation;moveTo(Lnet/minecraft/world/entity/Entity;D)Z",ordinal=0),require=1,expect=1,allow=1)
 private boolean wildercord$scavengerMove(PathNavigation navigation,Entity food,double speed,Operation<Boolean> original){return ScavengerFoodProbe.move(this,navigation,food,speed,original);}
 @WrapOperation(method="act(Lnet/minecraft/server/level/ServerLevel;)V",at=@At(value="INVOKE",target="Ldev/wildercord/aura/world/Galeclaw;distanceToSqr(Lnet/minecraft/world/entity/Entity;)D"),require=1,expect=1,allow=1)
 private double wildercord$scavengerDistance(Galeclaw actor,Entity food,Operation<Double> original){return ScavengerFoodProbe.distance(actor,food,original);}
}
