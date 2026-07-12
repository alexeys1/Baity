package com.shyeuar.baity.gui.internal;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.value.ModuleCategory;
import com.shyeuar.baity.gui.value.Value;
import com.shyeuar.baity.gui.value.ValueTreeUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public final class ClickGuiSearchUtils {

    private ClickGuiSearchUtils() {
    }

    public static boolean moduleMatchesSearch(Module module, String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return true;
        }
        String query = searchText.toLowerCase();
        if (module.getName().toLowerCase().contains(query)) {
            return true;
        }
        for (ValueTreeUtils.ValueEntry entry : ValueTreeUtils.getSearchableEntries(module)) {
            Value value = entry.value();
            if (value.getDisplayName().toLowerCase().contains(query)) {
                return true;
            }
            if (value.getName().toLowerCase().contains(query)) {
                return true;
            }
        }
        return false;
    }

    public static List<Module> filterModules(String searchText, ModuleCategory category) {
        if (searchText == null || searchText.isEmpty()) {
            return ModuleManager.getModulesByCategory(category);
        }
        String query = searchText.toLowerCase().trim();
        return ModuleManager.getModules().stream()
                .filter(module -> moduleMatchesSearch(module, query))
                .sorted(Comparator.comparing(Module::getName))
                .collect(Collectors.toList());
    }
}
