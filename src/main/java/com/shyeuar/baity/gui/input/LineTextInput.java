package com.shyeuar.baity.gui.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashSettings;
import com.shyeuar.baity.gui.value.SliderValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.function.BiPredicate;

public final class LineTextInput {

    private static final int SELECTION_BG = 0xFF3355AA;

    public enum KeyResult {
        NOT_HANDLED,
        HANDLED,
        CANCEL,
        COMMIT
    }

    private Policy policy;
    private String text = "";
    private int cursorCp = 0;
    private int selectionAnchorCp = -1;
    private int selectionEndCp = -1;
    private boolean dragSelecting;

    public LineTextInput(Policy policy) {
        this.policy = policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public String getText() {
        return text;
    }

    public void setText(String value) {
        text = sanitize(value == null ? "" : value);
        clampCursor();
        clearSelection();
    }

    public void setTextAndCursorToEnd(String value) {
        text = sanitize(value == null ? "" : value);
        cursorCp = codePointCount(text);
        clearSelection();
    }

    public int getCursorCp() {
        return cursorCp;
    }

    public void setCursorCp(int cpIndex) {
        cursorCp = Math.max(0, Math.min(cpIndex, codePointCount(text)));
    }

    public boolean hasSelection() {
        return selectionAnchorCp >= 0 && selectionEndCp >= 0 && selectionAnchorCp != selectionEndCp;
    }

    public int getSelectionStartCp() {
        if (!hasSelection()) {
            return cursorCp;
        }
        return Math.min(selectionAnchorCp, selectionEndCp);
    }

    public int getSelectionEndCp() {
        if (!hasSelection()) {
            return cursorCp;
        }
        return Math.max(selectionAnchorCp, selectionEndCp);
    }

    public void clear() {
        text = "";
        cursorCp = 0;
        clearSelection();
    }

    public void clearSelection() {
        selectionAnchorCp = -1;
        selectionEndCp = -1;
        dragSelecting = false;
    }

    public KeyResult handleKey(int keyCode, int modifiers) {
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;

        if (isShortcutModifierDown(modifiers)) {
            if (keyCode == GLFW.GLFW_KEY_V) {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null) {
                    String clip = mc.keyboardHandler.getClipboard();
                    if (clip != null && !clip.isEmpty()) {
                        replaceSelectionWith(clip);
                    }
                }
                return KeyResult.HANDLED;
            }
            if (keyCode == GLFW.GLFW_KEY_C || keyCode == GLFW.GLFW_KEY_INSERT) {
                copyToClipboard();
                return KeyResult.HANDLED;
            }
            if (keyCode == GLFW.GLFW_KEY_A) {
                if (!text.isEmpty()) {
                    selectionAnchorCp = 0;
                    selectionEndCp = codePointCount(text);
                    cursorCp = selectionEndCp;
                }
                return KeyResult.HANDLED;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return KeyResult.CANCEL;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            return KeyResult.COMMIT;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            moveCursor(-1, shift);
            return KeyResult.HANDLED;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            moveCursor(1, shift);
            return KeyResult.HANDLED;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            moveCursorTo(0, shift);
            return KeyResult.HANDLED;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            moveCursorTo(codePointCount(text), shift);
            return KeyResult.HANDLED;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (hasSelection()) {
                deleteSelection();
            } else {
                deleteBeforeCursor();
            }
            return KeyResult.HANDLED;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (hasSelection()) {
                deleteSelection();
            } else {
                deleteAfterCursor();
            }
            return KeyResult.HANDLED;
        }
        return KeyResult.NOT_HANDLED;
    }

    public boolean handleCodePoint(int codePoint) {
        if (Character.isISOControl(codePoint)) {
            return false;
        }
        if (!policy.isAllowed(text, codePoint)) {
            return true;
        }
        replaceSelectionWith(new String(Character.toChars(codePoint)));
        return true;
    }

    public void onMousePressed(Font font, float localX) {
        setCursorFromFontX(font, localX);
        selectionAnchorCp = cursorCp;
        selectionEndCp = cursorCp;
        dragSelecting = true;
    }

    public void onMouseDrag(Font font, float localX) {
        if (!dragSelecting) {
            return;
        }
        setCursorFromFontX(font, localX);
        selectionEndCp = cursorCp;
    }

    public void onMouseReleased() {
        if (selectionAnchorCp == selectionEndCp) {
            clearSelection();
        }
        dragSelecting = false;
    }

    public static boolean tryCommitSlider(SliderValue slider, String inputText) {
        if (inputText == null || inputText.isEmpty() || inputText.equals("-") || inputText.equals(".")) {
            return false;
        }
        if (!isValidSliderInputText(slider, inputText)) {
            return false;
        }
        try {
            return slider.trySetValue(Double.parseDouble(inputText));
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public static int cpIndexToCharIndex(String s, int cpIndex) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        int cpCount = s.codePointCount(0, s.length());
        int target = Math.max(0, Math.min(cpIndex, cpCount));
        int curCp = 0;
        for (int charIdx = 0; charIdx < s.length(); ) {
            if (curCp == target) {
                return charIdx;
            }
            int cp = s.codePointAt(charIdx);
            charIdx += Character.charCount(cp);
            curCp++;
        }
        return s.length();
    }

    public static String limitByCodePoints(String s, int maxCp) {
        if (s == null) {
            return "";
        }
        if (maxCp < 0) {
            return s;
        }
        int cpCount = s.codePointCount(0, s.length());
        if (cpCount <= maxCp) {
            return s;
        }
        int cpSeen = 0;
        int charIdx = 0;
        while (charIdx < s.length()) {
            int cp = s.codePointAt(charIdx);
            if (cpSeen >= maxCp) {
                break;
            }
            charIdx += Character.charCount(cp);
            cpSeen++;
        }
        return s.substring(0, charIdx);
    }

    public static boolean shouldBlinkCursor() {
        return System.currentTimeMillis() % 1000 < 500;
    }

    public static void drawTextWithBlinkCursor(
        GuiGraphics context,
        Font font,
        String text,
        int cursorCp,
        int selectionStartCp,
        int selectionEndCp,
        int x,
        int y,
        int color,
        boolean focused,
        boolean blinkOn
    ) {
        if (text == null) {
            text = "";
        }
        boolean hasSelection = selectionStartCp >= 0 && selectionEndCp >= 0 && selectionStartCp != selectionEndCp;
        if (hasSelection) {
            int selStart = Math.min(selectionStartCp, selectionEndCp);
            int selEnd = Math.max(selectionStartCp, selectionEndCp);
            int x0 = x + font.width(text.substring(0, cpIndexToCharIndex(text, selStart)));
            int x1 = x + font.width(text.substring(0, cpIndexToCharIndex(text, selEnd)));
            context.fill(x0, y, x1, y + 9, SELECTION_BG);
        }
        int charPos = cpIndexToCharIndex(text, cursorCp);
        String before = text.substring(0, charPos);
        String after = text.substring(charPos);
        context.drawString(font, before, x, y, color, false);
        int cursorX = x + font.width(before);
        if (!after.isEmpty()) {
            context.drawString(font, after, cursorX, y, color, false);
        }
        if (focused && blinkOn) {
            context.fill(cursorX, y, cursorX + 1, y + 9, color);
        }
    }

    public static void drawCenteredClippedWithBlinkCursor(
        GuiGraphics context,
        Font font,
        String text,
        int cursorCp,
        int selectionStartCp,
        int selectionEndCp,
        float lineX1,
        float lineY,
        float lineX2,
        int color,
        boolean focused
    ) {
        float lineWidth = lineX2 - lineX1;
        float textAreaTop = lineY - 11f;
        float drawY = textAreaTop + (11f - font.lineHeight) * 0.5f;
        int drawYInt = Math.round(drawY);
        context.enableScissor((int) lineX1, (int) textAreaTop, (int) lineX2, (int) lineY);
        if (text == null) {
            text = "";
        }
        int textWidth = font.width(text);
        int drawX = Math.round(lineX1 + (lineWidth - textWidth) * 0.5f);
        drawTextWithBlinkCursor(
            context,
            font,
            text,
            cursorCp,
            selectionStartCp,
            selectionEndCp,
            drawX,
            drawYInt,
            color,
            focused,
            focused && shouldBlinkCursor()
        );
        context.disableScissor();
    }

    private void copyToClipboard() {
        String toCopy = hasSelection() ? getSelectedText() : text;
        if (toCopy.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        mc.keyboardHandler.setClipboard(toCopy);
    }

    private String getSelectedText() {
        int startCp = getSelectionStartCp();
        int endCp = getSelectionEndCp();
        int startChar = cpIndexToCharIndex(text, startCp);
        int endChar = cpIndexToCharIndex(text, endCp);
        return text.substring(startChar, endChar);
    }

    private void moveCursor(int delta, boolean shift) {
        int next = Math.max(0, Math.min(cursorCp + delta, codePointCount(text)));
        moveCursorTo(next, shift);
    }

    private void moveCursorTo(int cp, boolean shift) {
        if (shift) {
            if (selectionAnchorCp < 0) {
                selectionAnchorCp = cursorCp;
            }
            cursorCp = cp;
            selectionEndCp = cursorCp;
        } else {
            cursorCp = cp;
            clearSelection();
        }
    }

    private void setCursorFromFontX(Font font, float localX) {
        int cpCount = codePointCount(text);
        if (cpCount == 0) {
            cursorCp = 0;
            return;
        }
        int bestCp = 0;
        int bestDist = Integer.MAX_VALUE;
        for (int cp = 0; cp <= cpCount; cp++) {
            int width = font.width(text.substring(0, cpIndexToCharIndex(text, cp)));
            int dist = Math.abs(width - Math.round(localX));
            if (dist < bestDist) {
                bestDist = dist;
                bestCp = cp;
            }
        }
        cursorCp = bestCp;
    }

    private void replaceSelectionWith(String insert) {
        if (insert == null) {
            insert = "";
        }
        if (hasSelection()) {
            int startChar = cpIndexToCharIndex(text, getSelectionStartCp());
            int endChar = cpIndexToCharIndex(text, getSelectionEndCp());
            text = text.substring(0, startChar) + text.substring(endChar);
            cursorCp = getSelectionStartCp();
            clearSelection();
        }
        if (insert.isEmpty()) {
            return;
        }
        StringBuilder filtered = new StringBuilder();
        String base = text;
        for (int i = 0; i < insert.length(); ) {
            int cp = insert.codePointAt(i);
            if (policy.isAllowed(base, cp)) {
                filtered.appendCodePoint(cp);
                base += new String(Character.toChars(cp));
            }
            i += Character.charCount(cp);
        }
        if (filtered.isEmpty()) {
            return;
        }
        String addition = filtered.toString();
        int charPos = cpIndexToCharIndex(text, cursorCp);
        String merged = text.substring(0, charPos) + addition + text.substring(charPos);
        text = sanitize(merged);
        cursorCp = Math.min(cursorCp + addition.codePointCount(0, addition.length()), codePointCount(text));
    }

    private void deleteSelection() {
        if (!hasSelection()) {
            return;
        }
        int startChar = cpIndexToCharIndex(text, getSelectionStartCp());
        int endChar = cpIndexToCharIndex(text, getSelectionEndCp());
        text = text.substring(0, startChar) + text.substring(endChar);
        cursorCp = getSelectionStartCp();
        clearSelection();
    }

    private void deleteBeforeCursor() {
        if (cursorCp <= 0 || text.isEmpty()) {
            return;
        }
        int leftCp = cursorCp - 1;
        int leftChar = cpIndexToCharIndex(text, leftCp);
        int rightChar = cpIndexToCharIndex(text, cursorCp);
        text = text.substring(0, leftChar) + text.substring(rightChar);
        cursorCp = leftCp;
    }

    private void deleteAfterCursor() {
        int cpCount = codePointCount(text);
        if (cursorCp >= cpCount || text.isEmpty()) {
            return;
        }
        int leftChar = cpIndexToCharIndex(text, cursorCp);
        int rightChar = cpIndexToCharIndex(text, cursorCp + 1);
        text = text.substring(0, leftChar) + text.substring(rightChar);
    }

    private String sanitize(String raw) {
        StringBuilder out = new StringBuilder();
        String building = "";
        for (int i = 0; i < raw.length(); ) {
            int cp = raw.codePointAt(i);
            if (policy.isAllowed(building, cp)) {
                out.appendCodePoint(cp);
                building += new String(Character.toChars(cp));
            }
            i += Character.charCount(cp);
        }
        if (policy.maxCodePoints() >= 0) {
            return limitByCodePoints(out.toString(), policy.maxCodePoints());
        }
        return out.toString();
    }

    private void clampCursor() {
        cursorCp = Math.max(0, Math.min(cursorCp, codePointCount(text)));
    }

    private static int codePointCount(String s) {
        return s == null || s.isEmpty() ? 0 : s.codePointCount(0, s.length());
    }

    private static boolean isShortcutModifierDown(int modifiers) {
        if ((modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER)) != 0) {
            return true;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return false;
        }
        return InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
            || InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL)
            || InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_LEFT_SUPER)
            || InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_RIGHT_SUPER);
    }

    private static boolean isSliderCharAllowed(SliderValue slider, String textBefore, int codePoint) {
        if (codePoint > 0xFFFF) {
            return false;
        }
        char ch = (char) codePoint;
        if (ch != '.' && ch != '-' && !Character.isDigit(codePoint)) {
            return false;
        }
        return isValidSliderInputText(slider, textBefore + ch);
    }

    private static boolean isValidSliderInputText(SliderValue slider, String raw) {
        if (raw == null || raw.isEmpty()) {
            return true;
        }
        String s = raw;
        boolean negative = false;
        if (s.startsWith("-")) {
            if (slider.getMinValue() >= 0) {
                return false;
            }
            negative = true;
            s = s.substring(1);
            if (s.isEmpty()) {
                return true;
            }
        }

        int dotCount = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '.') {
                dotCount++;
            } else if (!Character.isDigit(c)) {
                return false;
            }
        }
        if (dotCount > 1) {
            return false;
        }
        if (dotCount > 0 && slider.getDecimalPlaces() <= 0) {
            return false;
        }

        int dotIndex = s.indexOf('.');
        if (dotIndex >= 0) {
            String fractional = s.substring(dotIndex + 1);
            if (fractional.length() > slider.getDecimalPlaces()) {
                return false;
            }
        }

        if (s.endsWith(".")) {
            if (slider.getDecimalPlaces() <= 0) {
                return false;
            }
            s = s.substring(0, s.length() - 1);
            if (s.isEmpty()) {
                return true;
            }
        }

        try {
            double v = Double.parseDouble((negative ? "-" : "") + s);
            if (v > slider.getMaxValue()) {
                return false;
            }
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static final class Policy {
        private final int maxCodePoints;
        private final BiPredicate<String, Integer> allowed;

        private Policy(int maxCodePoints, BiPredicate<String, Integer> allowed) {
            this.maxCodePoints = maxCodePoints;
            this.allowed = allowed;
        }

        public int maxCodePoints() {
            return maxCodePoints;
        }

        public boolean isAllowed(String currentText, int codePoint) {
            return allowed.test(currentText == null ? "" : currentText, codePoint);
        }

        public static Policy search() {
            return new Policy(-1, (text, cp) -> cp >= 32 && cp < 127);
        }

        public static Policy hexColor() {
            return new Policy(6, (text, cp) -> {
                if (cp > 0xFFFF) {
                    return false;
                }
                char ch = (char) (int) cp;
                return (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F');
            });
        }

        public static Policy damageSymbols() {
            return new Policy(
                FancyDmgSplashSettings.MAX_DAMAGE_SYMBOL_CODE_POINTS,
                (text, cp) -> !Character.isISOControl(cp)
            );
        }

        public static Policy freeText(int maxCodePoints) {
            return new Policy(maxCodePoints, (text, cp) -> !Character.isISOControl(cp));
        }

        public static Policy forSlider(SliderValue slider) {
            return new Policy(-1, (text, cp) -> isSliderCharAllowed(slider, text, cp));
        }
    }
}