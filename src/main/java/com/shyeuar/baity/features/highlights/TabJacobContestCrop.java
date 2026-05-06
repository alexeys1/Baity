package com.shyeuar.baity.features.highlights;

import com.shyeuar.baity.utils.LocateUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class TabJacobContestCrop {

    private static final String JACOB_CONTEST_ANCHOR = "Jacob's Contest";

    private static final Pattern TAB_CROP_LINE = Pattern.compile(
            "(?<fortune>[☘○]) (?<crop>.+?)(?: ◆ )?(?<percentage>Top [\\d.]+%)?"
    );

    private static final Set<String> TAB_CROP_EXACT_NAMES = Set.of(
            "Wheat", "Carrot", "Potato", "Pumpkin", "Melon Slice", "Mushroom", "Cactus", "Sugar Cane",
            "Nether Wart", "Cocoa Beans", "Sunflower", "Moonflower", "Wild Rose"
    );

    private static final int WIDGET_LINE_SPAN = 14;

    private static final int ACTIVE_MAX_RELATIVE_INDEX = 5;

    private static volatile boolean jacobContestTabBlockFound;
    private static volatile boolean contestCurrentlyActive;
    private static volatile Set<String> parsedContestCrops = Set.of();

    private static int tickCounter;
    private static volatile boolean immediateRescanRequested;

    static {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
    }

    private TabJacobContestCrop() {}

    public static void requestImmediateRescan() {
        immediateRescanRequested = true;
    }

    public static void tick(Minecraft mc) {
        boolean duePeriodic = ++tickCounter % 20 == 0;
        if (!duePeriodic && !immediateRescanRequested) {
            return;
        }
        immediateRescanRequested = false;
        update(mc);
    }

    private static void update(Minecraft mc) {
        if (mc == null || mc.level == null) {
            clearState();
            return;
        }
        List<String> lines = LocateUtils.readTabHudScanPlainLines(mc);
        int anchor = indexOfJacobContestLine(lines);
        if (anchor < 0) {
            clearState();
            return;
        }

        jacobContestTabBlockFound = true;
        int end = Math.min(anchor + WIDGET_LINE_SPAN, lines.size());

        boolean hasStartsIn = false;
        boolean hasActiveBanner = false;
        LinkedHashSet<String> crops = new LinkedHashSet<>();
        boolean cropSectionEnded = false;

        for (int j = anchor; j < end; j++) {
            String line = lines.get(j);
            if (j > anchor && line.contains(JACOB_CONTEST_ANCHOR)) {
                break;
            }
            String tl = line.toLowerCase(Locale.ROOT);
            if (tl.contains("starts in")) {
                hasStartsIn = true;
            }
            String trimmed = line.trim();
            if (j > anchor
                    && j <= anchor + ACTIVE_MAX_RELATIVE_INDEX
                    && "ACTIVE".equalsIgnoreCase(trimmed)) {
                hasActiveBanner = true;
            }
            if (j > anchor && isJacobColumnAfterCropsBoundary(tl)) {
                cropSectionEnded = true;
            }
            if (j > anchor && !cropSectionEnded) {
                Matcher m = TAB_CROP_LINE.matcher(trimmed);
                if (m.matches()) {
                    crops.add(normalizeCropName(m.group("crop")));
                } else if (isExactCatalogCropLine(trimmed)) {
                    crops.add(trimmed);
                }
            }
        }

        contestCurrentlyActive = hasActiveBanner && !hasStartsIn;
        parsedContestCrops = crops.isEmpty() ? Set.of() : Set.copyOf(crops);
    }

    private static int indexOfJacobContestLine(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains(JACOB_CONTEST_ANCHOR)) {
                return i;
            }
        }
        return -1;
    }

    private static String normalizeCropName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim();
    }

    private static boolean isJacobColumnAfterCropsBoundary(String lowerLine) {
        return lowerLine.contains("pests:")
                || lowerLine.contains("spray:")
                || lowerLine.contains("repellent")
                || lowerLine.contains("visitors")
                || lowerLine.contains("pest trap");
    }

    private static boolean isExactCatalogCropLine(String trimmed) {
        if (trimmed.isEmpty() || trimmed.indexOf(':') >= 0) {
            return false;
        }
        for (String name : TAB_CROP_EXACT_NAMES) {
            if (name.equalsIgnoreCase(trimmed)) {
                return true;
            }
        }
        return false;
    }

    private static void clearState() {
        jacobContestTabBlockFound = false;
        contestCurrentlyActive = false;
        parsedContestCrops = Set.of();
    }

    public static boolean isJacobContestTabBlockFound() {
        return jacobContestTabBlockFound;
    }

    public static boolean isContestCurrentlyActive() {
        return contestCurrentlyActive;
    }

    public static Set<String> getParsedContestCrops() {
        return parsedContestCrops;
    }

    public static void clear() {
        clearState();
        tickCounter = 0;
        immediateRescanRequested = false;
    }
}