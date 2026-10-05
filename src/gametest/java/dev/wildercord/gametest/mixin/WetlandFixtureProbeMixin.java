package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.wildlife.WetlandGenerationProbe;
import dev.wildercord.wildlife.WetlandTerrainTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import org.spongepowered.asm.mixin.Mixin;

/** No changes to the fixture's terrain, search, detector, assertions, or teardown. */
@Mixin(value=WetlandTerrainTest.class,remap=false)
public abstract class WetlandFixtureProbeMixin {
 @WrapMethod(method="runTest(Lnet/fabricmc/fabric/api/client/gametest/v1/context/ClientGameTestContext;)V",require=1,expect=1,allow=1)
 private void wildercord$observeFixture(ClientGameTestContext context,Operation<Void> original) {
  try(var ignored=WetlandGenerationProbe.beginFixture()) {original.call(context);}
 }
}
