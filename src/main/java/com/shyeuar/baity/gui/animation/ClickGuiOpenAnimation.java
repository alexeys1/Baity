package com.shyeuar.baity.gui.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ClickGuiOpenAnimation {

    public static final long PHASE1_MS = 150L;
    public static final long PHASE2_MS = 150L;
    public static final long BACKDROP_FADE_MS = 500L;
    public static final float MIN_OPEN_HEIGHT = 5f;

    private static final float SIGMOID_STR = 8f;
    private static final float SIGMOID_A = -1f / (sigmoid(-0.5f * SIGMOID_STR) - sigmoid(0.5f * SIGMOID_STR));
    private static final float SIGMOID_B = SIGMOID_A * sigmoid(-0.5f * SIGMOID_STR);

    private ClickGuiOpenAnimation() {}

    public record OpeningFrame(float x, float y, float width, float height) {}

    public static OpeningFrame computeFrame(long elapsedMs, float targetWidth, float targetHeight) {
        float openingWidth;
        float openingHeight;

        if (elapsedMs >= PHASE1_MS + PHASE2_MS) {
            openingWidth = targetWidth;
            openingHeight = targetHeight;
        } else if (elapsedMs < PHASE1_MS) {
            openingWidth = elapsedMs * targetWidth / PHASE1_MS;
            openingHeight = MIN_OPEN_HEIGHT;
        } else {
            openingWidth = targetWidth;
            openingHeight = MIN_OPEN_HEIGHT + (elapsedMs - PHASE1_MS) * (targetHeight - MIN_OPEN_HEIGHT) / PHASE2_MS;
        }

        float x = (targetWidth - openingWidth) / 2f;
        float y = (targetHeight - openingHeight) / 2f;
        return new OpeningFrame(x, y, openingWidth, openingHeight);
    }

    public static float backdropOpacity(long elapsedMs) {
        float t = Math.max(0f, Math.min(1f, elapsedMs / (float) BACKDROP_FADE_MS));
        return sigmoidZeroOne(t);
    }

    public static boolean isOpening(long elapsedMs) {
        return elapsedMs < PHASE1_MS + PHASE2_MS;
    }

    public static boolean isPhase1(long elapsedMs) {
        return elapsedMs < PHASE1_MS;
    }

    private static float sigmoidZeroOne(float f) {
        f = Math.max(0f, Math.min(1f, f));
        return SIGMOID_A * sigmoid(SIGMOID_STR * (f - 0.5f)) - SIGMOID_B;
    }

    private static float sigmoid(float val) {
        return (float) (1 / (1 + Math.exp(-val)));
    }
}