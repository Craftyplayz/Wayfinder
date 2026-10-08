package dev.craftyplayz.wayfinder.mixin;

import dev.craftyplayz.wayfinder.MarkerCameraController;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private float eyeHeight;
    @Shadow private float eyeHeightOld;

    @Shadow protected abstract void alignWithEntity(float partialTicks);

    @Inject(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;alignWithEntity(F)V", shift = At.Shift.AFTER))
    private void wayfinder$snapCamera(DeltaTracker tracker, CallbackInfo ci) {
        Camera camera = (Camera) (Object) this;
        Entity entity = camera.entity();
        if (entity == null) return;
        float partialTicks = camera.getCameraEntityPartialTicks(tracker);
        Vec3 eyePosition = new Vec3(Mth.lerp(partialTicks, entity.xo, entity.getX()),
                Mth.lerp(partialTicks, entity.yo, entity.getY())
                        + Mth.lerp(partialTicks, eyeHeightOld, eyeHeight),
                Mth.lerp(partialTicks, entity.zo, entity.getZ()));
        if (MarkerCameraController.update(camera, eyePosition)) {
            // Realign before vanilla builds the frustum, including third-person collision.
            alignWithEntity(partialTicks);
        }
    }
}
