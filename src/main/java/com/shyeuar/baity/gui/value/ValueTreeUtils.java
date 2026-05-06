package com.shyeuar.baity.gui.value;

import com.shyeuar.baity.gui.module.Module;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class ValueTreeUtils {
    private ValueTreeUtils() {
    }

    public static List<ValueEntry> getVisibleEntries(Module module) {
        List<ValueEntry> out = new ArrayList<>();
        for (Value value : module.getValues()) {
            if ("enabled".equals(value.getName())) continue;
            appendVisible(value, 0, null, out);
        }
        return out;
    }

    public static List<ValueEntry> getAllEntries(Module module) {
        List<ValueEntry> out = new ArrayList<>();
        for (Value value : module.getValues()) {
            if ("enabled".equals(value.getName())) continue;
            appendAll(value, 0, null, out);
        }
        return out;
    }

    public static Value findByName(Module module, String valueName) {
        if (valueName == null) return null;
        for (ValueEntry entry : getAllEntries(module)) {
            if (valueName.equals(entry.value().getName())) {
                return entry.value();
            }
        }
        return null;
    }

    private static void appendVisible(Value value, int depth, String enclosingGroupName, List<ValueEntry> out) {
        out.add(new ValueEntry(value, depth, enclosingGroupName));
        if (value instanceof GroupValue groupValue && groupValue.isExpanded()) {
            String parentName = groupValue.getName();
            for (Value child : groupValue.getChildren()) {
                appendVisible(child, depth + 1, parentName, out);
            }
        }
    }

    private static void appendAll(Value value, int depth, String enclosingGroupName, List<ValueEntry> out) {
        out.add(new ValueEntry(value, depth, enclosingGroupName));
        if (value instanceof GroupValue groupValue) {
            String parentName = groupValue.getName();
            for (Value child : groupValue.getChildren()) {
                appendAll(child, depth + 1, parentName, out);
            }
        }
    }

    public record ValueEntry(Value value, int depth, String enclosingGroupName) {
    }
}
