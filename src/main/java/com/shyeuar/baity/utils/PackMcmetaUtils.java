package com.shyeuar.baity.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class PackMcmetaUtils {
    private static final int PACK_FORMAT_1_21_11 = 75;

    private PackMcmetaUtils() {}

    public static void write(File mcmeta, String descriptionJson) {
        mcmeta.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(mcmeta)) {
            out.write(buildContent(descriptionJson).getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
        }
    }

    public static String buildContent(String descriptionJson) {
        return "{\n"
                + "  \"pack\": {\n"
                + "    \"min_format\": " + PACK_FORMAT_1_21_11 + ",\n"
                + "    \"max_format\": " + PACK_FORMAT_1_21_11 + ",\n"
                + "    \"description\": " + descriptionJson + "\n"
                + "  }\n"
                + "}\n";
    }
}