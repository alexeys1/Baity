package com.shyeuar.baity.gui.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class TooltipSizeAnimator {

    private static final float SIZE_TRANSITION_SECONDS = 0.09f;
    private static final float SNAP_EPSILON = 0.5f;
    private static final float MAX_DT_SECONDS = 0.05f;
    private static final long SIZE_MEMORY_MS = 300L;
    /** Adjacent inventory slots are ~18px; only animate Y when the jump is clearly a reposition, not a slot step. */
    private static final float LARGE_Y_JUMP_THRESHOLD = 48.0f;

    private long lastTimeMs;
    private long sizeMemoryExpiryMs;
    private boolean initialized;
    private boolean visibleLastFrame;
    private boolean sizeTransitionActive;
    private boolean positionYTransitionActive;
    private float animatedWidth;
    private float animatedHeight;
    private float animatedX;
    private float animatedY;
    private int lastSignature = Integer.MIN_VALUE;

    public record Frame(
            float animatedWidth,
            float animatedHeight,
            float animatedX,
            float animatedY,
            boolean animateBackground,
            boolean needsTextClip
    ) {
    }

    public Frame update(int signature, float targetWidth, float targetHeight) {
        return update(signature, targetWidth, targetHeight, 0.0f, 0.0f, false);
    }

    public Frame update(int signature, float targetWidth, float targetHeight, float targetX, float targetY) {
        return update(signature, targetWidth, targetHeight, targetX, targetY, false);
    }

    public Frame update(
            int signature,
            float targetWidth,
            float targetHeight,
            float targetX,
            float targetY,
            boolean animatePositionX
    ) {
        return update(signature, targetWidth, targetHeight, targetX, targetY, true, animatePositionX);
    }

    private Frame update(
            int signature,
            float targetWidth,
            float targetHeight,
            float targetX,
            float targetY,
            boolean trackPosition,
            boolean animatePositionX
    ) {
        long now = System.currentTimeMillis();
        boolean memoryActive = initialized && now < sizeMemoryExpiryMs;
        boolean contentChanged = signature != lastSignature;
        boolean freshShow = !initialized || (!memoryActive && !visibleLastFrame);
        boolean switchingWhileVisible = visibleLastFrame && contentChanged;
        boolean resumingWithMemory = memoryActive && !visibleLastFrame;

        if (!initialized || freshShow) {
            animatedWidth = targetWidth;
            animatedHeight = targetHeight;
            if (trackPosition) {
                animatedX = targetX;
                animatedY = targetY;
            }
            sizeTransitionActive = false;
            positionYTransitionActive = false;
            initialized = true;
            lastSignature = signature;
            sizeMemoryExpiryMs = 0L;
        } else if (switchingWhileVisible || (resumingWithMemory && contentChanged)) {
            sizeTransitionActive = true;
            lastSignature = signature;
            if (trackPosition) {
                if (resumingWithMemory) {
                    animatedX = targetX;
                    animatedY = targetY;
                    positionYTransitionActive = false;
                } else {
                    boolean shrinking = targetHeight < animatedHeight - SNAP_EPSILON;
                    positionYTransitionActive = !shrinking
                            && Math.abs(targetY - animatedY) > LARGE_Y_JUMP_THRESHOLD;
                    if (!positionYTransitionActive) {
                        animatedY = targetY;
                    }
                }
            }
        } else if (!sizeTransitionActive) {
            animatedWidth = targetWidth;
            animatedHeight = targetHeight;
            if (trackPosition) {
                animatedY = targetY;
                animatedX = targetX;
            }
            lastSignature = signature;
        }

        if (sizeTransitionActive) {
            float dt = computeDeltaSeconds(now);
            animatedWidth = moveLinear(animatedWidth, targetWidth, dt, SIZE_TRANSITION_SECONDS);
            animatedHeight = moveLinear(animatedHeight, targetHeight, dt, SIZE_TRANSITION_SECONDS);
            if (trackPosition) {
                if (positionYTransitionActive) {
                    animatedY = moveLinear(animatedY, targetY, dt, SIZE_TRANSITION_SECONDS);
                } else {
                    animatedY = targetY;
                }
                if (animatePositionX) {
                    animatedX = moveLinear(animatedX, targetX, dt, SIZE_TRANSITION_SECONDS);
                } else {
                    animatedX = targetX;
                }
            }
            boolean sizeSettled = Math.abs(animatedWidth - targetWidth) <= SNAP_EPSILON
                    && Math.abs(animatedHeight - targetHeight) <= SNAP_EPSILON;
            boolean positionSettled = !trackPosition
                    || (!positionYTransitionActive || Math.abs(animatedY - targetY) <= SNAP_EPSILON);
            if (animatePositionX) {
                positionSettled = positionSettled && Math.abs(animatedX - targetX) <= SNAP_EPSILON;
            }
            if (sizeSettled && positionSettled) {
                animatedWidth = targetWidth;
                animatedHeight = targetHeight;
                if (trackPosition) {
                    animatedX = targetX;
                    animatedY = targetY;
                }
                sizeTransitionActive = false;
                positionYTransitionActive = false;
            }
        }

        boolean animateBackground = sizeTransitionActive || (
                Math.abs(animatedWidth - targetWidth) > SNAP_EPSILON
                        || Math.abs(animatedHeight - targetHeight) > SNAP_EPSILON
        );
        boolean animatePositionY = trackPosition && positionYTransitionActive && (
                sizeTransitionActive || Math.abs(animatedY - targetY) > SNAP_EPSILON
        );
        boolean animatePositionXActive = trackPosition && animatePositionX && (
                sizeTransitionActive || Math.abs(animatedX - targetX) > SNAP_EPSILON
        );
        boolean needsTextClip = animateBackground;

        float frameX = trackPosition ? animatedX : targetX;
        float frameY = trackPosition ? animatedY : targetY;
        return new Frame(
                animatedWidth,
                animatedHeight,
                frameX,
                frameY,
                animateBackground || animatePositionY || animatePositionXActive,
                needsTextClip
        );
    }

    public void endFrame(boolean tooltipVisible) {
        if (tooltipVisible) {
            visibleLastFrame = true;
            return;
        }
        if (visibleLastFrame && initialized) {
            sizeMemoryExpiryMs = System.currentTimeMillis() + SIZE_MEMORY_MS;
        }
        visibleLastFrame = false;
    }

    public void reset() {
        initialized = false;
        visibleLastFrame = false;
        sizeTransitionActive = false;
        positionYTransitionActive = false;
        lastSignature = Integer.MIN_VALUE;
        animatedWidth = 0.0f;
        animatedHeight = 0.0f;
        animatedX = 0.0f;
        animatedY = 0.0f;
        lastTimeMs = 0L;
        sizeMemoryExpiryMs = 0L;
    }

    public void invalidate() {
        initialized = false;
        sizeMemoryExpiryMs = 0L;
    }

    public void offsetAnimatedX(float delta) {
        if (initialized) {
            animatedX += delta;
        }
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

    private static float moveLinear(float current, float target, float dt, float durationSeconds) {
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
}
