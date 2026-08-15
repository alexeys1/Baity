package com.shyeuar.baity.gui.value;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.managers.ModuleInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class ValueTreeUtils {
    private ValueTreeUtils() {
    }

    public static List<ValueEntry> getVisibleEntries(Module module) {
        return getVisibleEntries(module, (ignoredModule, groupValue) -> groupValue.isExpanded() ? 1.0f : 0.0f);
    }

    public static List<ValueEntry> getVisibleEntries(
            Module module,
            java.util.function.BiFunction<Module, GroupValue, Float> groupProgress
    ) {
        List<ValueEntry> out = new ArrayList<>();
        for (Value value : module.getValues()) {
            if ("enabled".equals(value.getName())) continue;
            if (isHiddenFromGui(module, value)) continue;
            appendVisible(module, value, 0, null, out, groupProgress);
        }
        return out;
    }

    private static boolean isHiddenFromGui(Module module, Value value) {
        return "FancyDmgSplash".equals(module.getName())
                && ModuleInitializer.isHiddenGuiValue(value.getName());
    }

    public static List<ValueEntry> getAllEntries(Module module) {
        List<ValueEntry> out = new ArrayList<>();
        for (Value value : module.getValues()) {
            if ("enabled".equals(value.getName())) continue;
            appendAll(value, 0, null, out);
        }
        return out;
    }

    public static List<ValueEntry> getSearchableEntries(Module module) {
        List<ValueEntry> out = new ArrayList<>();
        for (Value value : module.getValues()) {
            if ("enabled".equals(value.getName())) continue;
            if (isHiddenFromGui(module, value)) continue;
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

    private static void appendVisible(
            Module module,
            Value value,
            int depth,
            String enclosingGroupName,
            List<ValueEntry> out,
            java.util.function.BiFunction<Module, GroupValue, Float> groupProgress
    ) {
        out.add(new ValueEntry(value, depth, enclosingGroupName));
        if (value instanceof GroupValue groupValue) {
            float progress = groupProgress.apply(module, groupValue);
            if (groupValue.isExpanded() || progress > 0.001f) {
                String parentName = groupValue.getName();
                for (Value child : groupValue.getChildren()) {
                    appendVisible(module, child, depth + 1, parentName, out, groupProgress);
                }
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
