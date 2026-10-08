package dev.craftyplayz.wayfinder.mixin;

import dev.craftyplayz.wayfinder.MarkerCameraController;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void alignWithEntity(float partialTicks);

    @Inject(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;alignWithEntity(F)V", shift = At.Shift.AFTER))
    private void wayfinder$snapCamera(DeltaTracker tracker, CallbackInfo ci) {
        Camera camera = (Camera) (Object) this;
        float partialTicks = camera.getCameraEntityPartialTicks(tracker);
        if (MarkerCameraController.update(camera)) {
            // Realign before vanilla builds the frustum, including third-person collision.
            alignWithEntity(partialTicks);
        }
    }
}
