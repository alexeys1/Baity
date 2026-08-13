package com.shyeuar.baity.features.radialmenu;

import com.shyeuar.baity.gui.radial.RadialWheelRenderer;
import com.shyeuar.baity.utils.NickRenderUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class UnicodeIconParser {

    private UnicodeIconParser() {
    }

    public record Parsed(String glyph, int colorArgb) {
        public static final Parsed EMPTY = new Parsed("", RadialWheelRenderer.SYMBOL_ICON_COLOR);
    }

    public static Parsed parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Parsed.EMPTY;
        }
        Integer explicitColor = null;
        for (int i = 0; i < raw.length(); ) {
            int cp = raw.codePointAt(i);
            int cpl = Character.charCount(cp);
            if (cp == '&' && i + cpl < raw.length()) {
                int ncp = raw.codePointAt(i + cpl);
                char ch = Character.toLowerCase((char) ncp);
                Integer mapped = NickRenderUtils.legacyColorRgb(ch);
                if (mapped != null) {
                    explicitColor = mapped;
                    i += cpl + Character.charCount(ncp);
                    continue;
                }
            }
            String glyph = new String(Character.toChars(cp));
            int color = explicitColor == null
                    ? RadialWheelRenderer.SYMBOL_ICON_COLOR
                    : (0xFF000000 | explicitColor);
            return new Parsed(glyph, color);
        }
        return Parsed.EMPTY;
    }
}
