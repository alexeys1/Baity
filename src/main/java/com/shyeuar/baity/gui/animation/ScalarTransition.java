package com.shyeuar.baity.gui.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ScalarTransition {

    public static final float TRANSITION_SECONDS = 0.09f;
    public static final float SCROLL_TRANSITION_SECONDS = 0.06f;
    public static final float EXPAND_TRANSITION_SECONDS = 0.05f;
    public static final float SNAP_EPSILON = 0.01f;
    public static final float MAX_DT_SECONDS = 0.05f;

    private long lastTimeMs;

    public float beginFrame() {
        long now = System.currentTimeMillis();
        return computeDeltaSeconds(now);
    }

    public float step(float current, float target) {
        return step(current, target, beginFrame());
    }

    public float step(float current, float target, float dt) {
        return moveLinear(current, target, dt, TRANSITION_SECONDS);
    }

    public static float moveLinear(float current, float target, float dt, float durationSeconds) {
        float diff = target - current;
        if (Math.abs(diff) <= SNAP_EPSILON) {
            return target;
        }
        if (durationSeconds <= 0.0f) {
            return target;
        }
        float step = diff * Math.min(1.0f, dt / durationSeconds);
        float next = current + step;
        if (Math.abs(target - next) <= SNAP_EPSILON) {
            return target;
        }
        return next;
    }

    private float computeDeltaSeconds(long now) {
        if (lastTimeMs == 0L) {
            lastTimeMs = now;
            return 1.0f / 60.0f;
        }
        float dt = Math.max(0.0f, Math.min(MAX_DT_SECONDS, (now - lastTimeMs) / 1000.0f));
        lastTimeMs = now;
        return dt;
    }
}
