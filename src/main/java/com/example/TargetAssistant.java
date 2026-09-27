package com.example.mymod.modules; 

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW; 

public class TargetAssistant { 

private static final String CATEGORY = "Accessibility";
private static final String TRANSLATION_KEY = "key.target_assistant.toggle";

// Sozlamalar (Masofa va burchak)
private static final double SCAN_RANGE = 3.5;
private static final double FOV_THRESHOLD = 3.0; // Tor ko'rish burchagi
private static final int ATTACK_COOLDOWN = 10;   // Urishlar orasidagi vaqt (ticks)

private KeyBinding keyBinding;
private boolean isEnabled = false;
private boolean lastEnabledState = false;
private int cooldownRemaining = 0;

public TargetAssistant() {
registerKeyBinding();
registerTickListener();
}

private void registerKeyBinding() {
keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
TRANSLATION_KEY,
InputUtil.Type.KEYSYM,
GLFW.GLFW_KEY_UNKNOWN, // O'yinchining o'zi sozlamalardan tugma belgilaydi
CATEGORY
));
}

private void registerTickListener() {
ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);
}

private void onEndClientTick(MinecraftClient client) {
while (keyBinding.wasPressed()) {
toggle();
}
if (isEnabled != lastEnabledState) {
    sendStatusMessage(client);
    lastEnabledState = isEnabled;
}

if (isEnabled) {
    tickAutoAttackAndAim(client);
}

}

private void toggle() {
isEnabled = !isEnabled;
}

private void tickAutoAttackAndAim(MinecraftClient client) {
if (cooldownRemaining > 0) {
cooldownRemaining--;
}
if (client.player == null || client.interactionManager == null) {
    return;
}

// Eng yaqin dushmanni qidirish
LivingEntity target = findEntityInCrosshair(client);

if (target != null) {
    // 1. KAMERANI DUSHMANGA BURISH LOGIKASI
    double diffX = target.getX() - client.player.getX();
    double diffY = target.getEyeY() - client.player.getEyeY();
    double diffZ = target.getZ() - client.player.getZ();
    double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

    float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F;
    float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));

    // Kamerani dushman markaziga to'g'rilash
    client.player.setYaw(yaw);
    client.player.setPitch(pitch);

    // 2. AVTOMATIK URISH (ATTACK) LOGIKASI
    if (cooldownRemaining <= 0) {
        // To'g'ridan-to'g'ri dushmanga qarab turganini qayta tekshirish
        HitResult hitResult = client.crosshairTarget;
        if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;
            if (entityHitResult.getEntity() == target) {
                // Hujum qilish va qo'lni siltash
                client.interactionManager.attackEntity(client.player, target);
                client.player.swingHand(client.player.getActiveHand());
                cooldownRemaining = ATTACK_COOLDOWN;
            }
        }
    }
}

}

private LivingEntity findEntityInCrosshair(MinecraftClient client) {
Vec3d eyePos = client.player.getEyePos();
Vec3d lookVec = client.player.getRotationVec(1.0F);
Box scanBox = client.player.getBoundingBox().expand(SCAN_RANGE);
LivingEntity closestMatch = null;
double closestAngle = FOV_THRESHOLD;

if (client.world != null) {
    for (Entity entity : client.world.getOtherEntities(client.player, scanBox)) {
        if (!(entity instanceof LivingEntity)) {
            continue;
        }
        LivingEntity living = (LivingEntity) entity;
        if (!living.isAlive()) {
            continue;
        }

        double distance = client.player.distanceTo(living);
        if (distance > SCAN_RANGE) {
            continue;
        }

        Vec3d toEntity = living.getEyePos().subtract(eyePos);
        Vec3d toEntityNormalized = toEntity.normalize();

        double dot = lookVec.dotProduct(toEntityNormalized);
        dot = Math.max(-1.0, Math.min(1.0, dot));
        double angleDegrees = Math.toDegrees(Math.acos(dot));

        if (angleDegrees < closestAngle) {
            closestAngle = angleDegrees;
            closestMatch = living;
        }
    }
}
return closestMatch;

}

private void sendStatusMessage(MinecraftClient client) {
if (client.player == null) {
return;
}
String statusText = isEnabled ? "Target Assistant: ENABLED" : "Target Assistant: DISABLED";
client.player.sendMessage(Text.literal(statusText), false);
}

public boolean isEnabled() {
return isEnabled;
}

  }
